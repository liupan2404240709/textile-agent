package com.zoe.java.ai.langchain4j.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoe.java.ai.langchain4j.assistant.PapaAgent;
import com.zoe.java.ai.langchain4j.assistant.PapaAgentNoTools;
import com.zoe.java.ai.langchain4j.dto.ChatForm;
import com.zoe.java.ai.langchain4j.dto.ImageGenerateForm;
import com.zoe.java.ai.langchain4j.config.PapaSystemPromptComposer;
import com.zoe.java.ai.langchain4j.entity.LoginUser;
import com.zoe.java.ai.langchain4j.entity.LoginUserHolder;
import com.zoe.java.ai.langchain4j.result.Result;
import com.zoe.java.ai.langchain4j.service.PapaService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.Disposable;
import reactor.util.context.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class PapaServiceImpl implements PapaService {

    private static final String DEFAULT_IMAGE_ANALYSIS_PROMPT = "请从纺织专业角度解析这张图片，重点识别面料材质与组织结构（机织/针织/非织造）、图案花型与色彩（潘通色号、色差）、印染与后整理工艺、款式与版型特征、应用场景（服装/家纺/产业用纺织品），并给出简要结论。";
    private static final String DEFAULT_IMAGE_GENERATE_SIZE = "1024*1024";

    private final PapaAgent papaAgent;
    private final PapaAgentNoTools papaAgentNoTools;
    private final PapaSystemPromptComposer papaSystemPromptComposer;

    @Value("${langchain4j.community.dashscope.image-model.api-key}")
    private String dashscopeApiKey;

    @Value("${langchain4j.community.dashscope.image-model.model-name}")
    private String imageModelName;

    @Value("${langchain4j.community.dashscope.image-generation-model.model-name:wanx-v1}")
    private String imageGenerationModelName;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://dashscope.aliyuncs.com")
            .build();

    // 存储活跃的流式对话，key为memoryId
    private final Map<String, Disposable> activeChatStreams = new ConcurrentHashMap<>();

    public PapaServiceImpl(PapaAgent papaAgent,
                           PapaAgentNoTools papaAgentNoTools,
                           PapaSystemPromptComposer papaSystemPromptComposer) {
        this.papaAgent = papaAgent;
        this.papaAgentNoTools = papaAgentNoTools;
        this.papaSystemPromptComposer = papaSystemPromptComposer;
    }

    @Override
    public Flux<String> chat(ChatForm chatForm) {
        String memoryId = chatForm.getMemoryId();
            
        // 如果该memoryId已有活跃流，先停止
        stopChat(memoryId);
            
        // 在当前线程捕获用户信息（拦截器已设置）
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        Long userId = loginUser != null ? loginUser.getUserId() : null;
        String username = loginUser != null ? loginUser.getUsername() : null;
            
        boolean webSearchEnabled = Boolean.TRUE.equals(chatForm.getWebSearchEnabled());
        log.info("开始对话, memoryId={}, userId={}, username={}, webSearchEnabled={}", 
                memoryId, userId, username, webSearchEnabled);
        log.info("原始 webSearchEnabled 值: {} (type: {})", chatForm.getWebSearchEnabled(), 
                chatForm.getWebSearchEnabled() != null ? chatForm.getWebSearchEnabled().getClass().getName() : "null");
            
        Flux<String> chatFlux;
            
        // 修改：始终使用 PapaAgent（带工具），但根据开关决定是否启用MCP搜索
        // 这样纺织设计师查询工具始终可用
        log.info("🔧 使用 PapaAgent (带纺织设计师查询工具)");
        if (webSearchEnabled) {
            log.info("✅ 同时启用 MCP 联网搜索");
        } else {
            log.info("⛔ 禁用 MCP 联网搜索，但保留纺织设计师查询工具");
        }
        chatFlux = chatWithTools(chatForm);
            
        // 包装Flux以跟踪订阅和取消，并传递用户上下文
        return Flux.create(sink -> {
            // 在订阅时恢复用户上下文到当前线程
            if (userId != null) {
                LoginUserHolder.setLoginUser(new LoginUser(userId, username));
                log.debug("在线程 {} 中恢复用户上下文, userId={}", Thread.currentThread().getName(), userId);
            }
            
            Disposable disposable = chatFlux
                    .doOnCancel(() -> {
                        log.info("流式对话已取消, memoryId: {}", memoryId);
                        activeChatStreams.remove(memoryId);
                    })
                    .doOnComplete(() -> {
                        log.info("流式对话已完成, memoryId: {}", memoryId);
                        activeChatStreams.remove(memoryId);
                    })
                    .doOnError(error -> {
                        log.error("流式对话出错, memoryId: {}, error: {}", memoryId, error.getMessage());
                        activeChatStreams.remove(memoryId);
                    })
                    .doFinally(signalType -> {
                        // 清理线程上下文
                        LoginUserHolder.clear();
                    })
                    .subscribe(
                            sink::next,
                            sink::error,
                            sink::complete
                    );
            
            // 保存disposable以便后续取消
            activeChatStreams.put(memoryId, disposable);
            sink.onCancel(() -> {
                disposable.dispose();
                activeChatStreams.remove(memoryId);
                LoginUserHolder.clear();
            });
        });
    }

    @Override
    public String readImageContent(MultipartFile image, String message) throws IOException {
        if (image == null || image.isEmpty()) {
            return "图片不能为空，请重新上传";
        }

        String userMessage = (message == null || message.trim().isEmpty())
                ? DEFAULT_IMAGE_ANALYSIS_PROMPT
                : message.trim();

        String contentType = image.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "image/jpeg";
        }
        String imageBase64 = Base64.getEncoder().encodeToString(image.getBytes());
        String imageDataUrl = "data:" + contentType + ";base64," + imageBase64;

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", userMessage);

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("type", "image_url");
        imagePart.put("image_url", Map.of("url", imageDataUrl));

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", List.of(textPart, imagePart));

        Map<String, Object> body = new HashMap<>();
        body.put("model", imageModelName);
        body.put("messages", List.of(userMsg));

        String response = webClient.post()
                .uri("/compatible-mode/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + dashscopeApiKey)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return extractModelContent(response == null ? "" : response);
    }

// ... existing code ...

    @Override
    public Result<Map<String, String>> generateImage(ImageGenerateForm imageGenerateForm) {
        if (imageGenerateForm == null || imageGenerateForm.getPrompt() == null || imageGenerateForm.getPrompt().trim().isEmpty()) {
            return Result.fail("提示词不能为空");
        }

        String prompt = imageGenerateForm.getPrompt().trim();

        if (containsSensitiveWords(prompt)) {
            log.warn("提示词包含敏感内容，已拦截: {}", prompt);
            return Result.fail("提示词可能包含敏感内容（如国旗、政治等），请修改后重试");
        }

        String size = normalizeImageSize(imageGenerateForm.getSize());

        String[] sizeParts = size.split("[*x×]");
        int width = 1024;
        int height = 1024;
        if (sizeParts.length == 2) {
            try {
                width = Integer.parseInt(sizeParts[0].trim());
                height = Integer.parseInt(sizeParts[1].trim());
            } catch (NumberFormatException e) {
                log.warn("解析尺寸失败，使用默认值 1024*1024");
            }
        }

        Map<String, Object> input = new HashMap<>();
        input.put("prompt", prompt);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("size", width + "*" + height);
        parameters.put("n", 1);

        Map<String, Object> body = new HashMap<>();
        body.put("model", imageGenerationModelName);
        body.put("input", input);
        body.put("parameters", parameters);

        try {
            log.info("开始生成图片: model={}, prompt={}, size={}x{}", imageGenerationModelName, prompt, width, height);

            String response = webClient.post()
                    .uri("/api/v1/services/aigc/text2image/image-synthesis")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + dashscopeApiKey)
                    .header("X-DashScope-Async", "enable")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            String taskId = root.path("output").path("task_id").asText("");
            String taskStatus = root.path("output").path("task_status").asText("");

            log.info("任务已提交, taskId: {}, status: {}", taskId, taskStatus);

            if ("SUCCEEDED".equals(taskStatus)) {
                return Result.ok(extractImageGenerateResult(response, prompt, size));
            } else if ("PENDING".equals(taskStatus) || "RUNNING".equals(taskStatus)) {
                return pollTaskStatus(taskId, prompt, size);
            } else {
                String errorMsg = root.path("output").path("message").asText("未知错误");
                log.error("任务失败, taskId: {}, error: {}", taskId, errorMsg);
                return Result.fail("图片生成失败: " + errorMsg);
            }
        } catch (Exception e) {
            log.error("图片生成异常, prompt: {}, error: {}", prompt, e.getMessage(), e);
            return Result.fail("图片生成失败：" + e.getMessage());
        }
    }

    private Result<Map<String, String>> pollTaskStatus(String taskId, String prompt, String size) {
        int maxRetries = 30;
        int retryInterval = 2000;

        for (int i = 0; i < maxRetries; i++) {
            try {
                Thread.sleep(retryInterval);

                String response = webClient.get()
                        .uri("/api/v1/tasks/{taskId}", taskId)
                        .header("Authorization", "Bearer " + dashscopeApiKey)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                JsonNode root = objectMapper.readTree(response);
                String taskStatus = root.path("output").path("task_status").asText("");

                log.debug("轮询任务状态 [{}/{}], taskId: {}, status: {}", i + 1, maxRetries, taskId, taskStatus);

                if ("SUCCEEDED".equals(taskStatus)) {
                    log.info("图片生成成功, taskId: {}", taskId);
                    return Result.ok(extractImageGenerateResult(response, prompt, size));
                } else if ("FAILED".equals(taskStatus)) {
                    String errorMsg = root.path("output").path("message").asText("任务失败");
                    if (errorMsg.contains("inappropriate content") || errorMsg.contains("DataInspectionFailed")) {
                        return Result.fail("内容安全拦截：提示词包含敏感内容，请修改后重试");
                    }
                    log.error("任务失败, taskId: {}, error: {}", taskId, errorMsg);
                    return Result.fail("图片生成失败: " + errorMsg);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Result.fail("图片生成被中断");
            } catch (Exception e) {
                log.warn("轮询异常, taskId: {}, error: {}", taskId, e.getMessage());
            }
        }

        log.error("任务超时, taskId: {}", taskId);
        return Result.fail("图片生成超时，请稍后重试");
    }

    private boolean containsSensitiveWords(String prompt) {
        String[] sensitiveWords = {
                "国旗", "党旗", "军旗", "政治", "领导人",
                "涉政", "色情", "暴力", "恐怖", "反动",
                "敏感", "禁忌"
        };
        return java.util.Arrays.stream(sensitiveWords)
                .anyMatch(word -> prompt.contains(word));
    }

// ... existing code ...


    @Override
    public void stopChat(String memoryId) {
        if (memoryId == null || memoryId.isBlank()) {
            log.warn("stopChat called with null or blank memoryId");
            return;
        }
        
        Disposable disposable = activeChatStreams.remove(memoryId);
        if (disposable != null && !disposable.isDisposed()) {
            log.info("正在停止流式对话, memoryId: {}", memoryId);
            disposable.dispose();
        } else {
            log.debug("没有找到活跃的流式对话或已停止, memoryId: {}", memoryId);
        }
    }

    /**
     * 使用带工具的Agent进行对话（纺织设计师查询工具始终可用）
     * 根据webSearchEnabled决定是否额外启用MCP联网搜索
     */
    private Flux<String> chatWithTools(ChatForm chatForm) {
        String systemPrompt = buildSystemPrompt(chatForm.getWebSearchEnabled());
        return papaAgent.chat(chatForm.getMemoryId(), chatForm.getMessage(), systemPrompt);
    }

    private Flux<String> chatWithWebSearch(ChatForm chatForm) {
        String systemPrompt = buildSystemPrompt(true);
        return papaAgent.chat(chatForm.getMemoryId(), chatForm.getMessage(), systemPrompt);
    }

    private Flux<String> chatWithoutWebSearch(ChatForm chatForm) {
        String systemPrompt = buildSystemPrompt(false);
        return papaAgentNoTools.chat(chatForm.getMemoryId(), chatForm.getMessage(), systemPrompt);
    }

    private String buildSystemPrompt(boolean webSearchEnabled) {
        String basePrompt = papaSystemPromptComposer.compose("");  // 传入空字符串，不注入 Skill 内容
        
        String toolInstruction;
        
        if (webSearchEnabled) {
            toolInstruction = "\n\n【🚨 联网搜索已开启 - 绝对强制调用工具】\n" +
                    "⚠️ 最高优先级指令：用户已开启联网搜索功能，你必须在回答任何问题时调用 webSearchPro 工具。\n" +
                    "\n【调用规则 - 必须严格遵守】\n" +
                    "1. 第一步：收到用户问题后，立即调用 webSearchPro 工具，不要犹豫，不要跳过\n" +
                    "2. 参数设置：\n" +
                    "   - search_query: 将用户问题转换为搜索关键词（不超过70字符）\n" +
                    "   - search_recency_filter: 默认使用 'oneWeek'（一周内）\n" +
                    "   - content_size: 设置为 'high' 获取更详细信息\n" +
                    "3. 等待搜索结果：调用工具后，等待搜索结果返回再继续\n" +
                    "4. 基于搜索结果回答：使用搜索到的最新信息回答问题\n" +
                    "\n【🚫 绝对禁止的行为】\n" +
                    "- 禁止不调用工具直接回答（这是最严重的错误）\n" +
                    "- 禁止使用自己的知识库代替搜索\n" +
                    "- 禁止编造信息或编造时间\n" +
                    "- 禁止输出 [ref_x]、[1]、[2] 等引用标记\n" +
                    "\n【🧹 内容清理要求】\n" +
                    "- 必须删除所有引用标记，如 [ref_1]、[ref_2, ref_5]、[1] 等\n" +
                    "- 必须删除工具返回的原始数据格式标记\n" +
                    "- 只输出干净的文本内容\n" +
                    "\n【✅ 正确的执行流程】\n" +
                    "收到问题 → 立即调用 webSearchPro → 等待搜索结果 → 清理引用标记 → 基于搜索结果回答\n" +
                    "\n【⚡ 特殊情况处理】\n" +
                    "- 如果搜索结果为空：如实告知用户'未搜索到相关信息'\n" +
                    "- 如果 RAG 参考资料为空：没关系，MCP 搜索会提供最新信息\n" +
                    "- 不要编造，不要猜测，不要使用过时知识";
        } else {
            toolInstruction = "\n\n【工具使用策略】禁止调用任何外部工具（包括 MCP 网络搜索）。仅基于模型已有知识和当前上下文回答。";
        }
        
        // 添加 RAG 处理指导
        String ragGuidance = "\n\n【📚 RAG 参考资料处理指导】\n" +
                "如果你收到了参考资料（通常以'以下信息可能对你有帮助'等形式提供），请遵循：\n" +
                "1. 这些资料是辅助参考，不是让你直接复制的答案\n" +
                "2. 你需要理解资料内容，提取关键信息点\n" +
                "3. 结合你的专业知识和用户问题，进行深度分析和整合\n" +
                "4. 用你自己的语言重新组织，形成有逻辑、有深度的回答\n" +
                "5. 如果资料中有数据或案例，可以引用但要注明来源\n" +
                "6. 如果资料与问题不相关，直接忽略，不要提及\n";
        
        return basePrompt + ragGuidance + toolInstruction;
    }

    private String normalizeImageSize(String size) {
        if (size == null || size.isBlank()) {
            return DEFAULT_IMAGE_GENERATE_SIZE;
        }
        return size.trim();
    }

    private Map<String, String> extractImageGenerateResult(String responseJson, String prompt, String size) throws IOException {
        JsonNode root = objectMapper.readTree(responseJson);

        String taskId = root.path("output").path("task_id").asText("");
        String taskStatus = root.path("output").path("task_status").asText("");
        String imageUrl = extractImageUrl(root);

        Map<String, String> result = new LinkedHashMap<>();
        result.put("prompt", prompt);
        result.put("size", size);
        result.put("taskId", taskId);
        result.put("taskStatus", taskStatus);
        result.put("imageUrl", imageUrl);
        return result;
    }

    private String extractImageUrl(JsonNode root) {
        JsonNode resultsNode = root.path("output").path("results");
        if (resultsNode.isArray() && !resultsNode.isEmpty()) {
            JsonNode first = resultsNode.get(0);
            String url = first.path("url").asText("");
            if (!url.isBlank()) {
                return url;
            }
        }

        String directUrl = root.path("output").path("url").asText("");
        if (!directUrl.isBlank()) {
            return directUrl;
        }

        return "";
    }

    private String extractModelContent(String responseJson) throws IOException {
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.isNull()) {
            return "图片解析失败，请稍后重试";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode item : contentNode) {
                String text = item.path("text").asText("");
                if (!text.isBlank()) {
                    if (!sb.isEmpty()) {
                        sb.append("\n");
                    }
                    sb.append(text);
                }
            }
            if (!sb.isEmpty()) {
                return sb.toString();
            }
        }
        return "图片解析完成，但未返回可展示内容";
    }

    @Override
    public void downloadImage(String imageUrl, String filename, HttpServletResponse response) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        
        try {
            log.info("开始下载图片: url={}, filename={}", imageUrl, filename);
            
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestMethod("GET");
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.error("图片下载失败，响应码: {}", responseCode);
                response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                response.getWriter().write("图片下载失败");
                return;
            }
            
            // 设置响应头
            String contentType = connection.getContentType();
            if (contentType != null && !contentType.isEmpty()) {
                response.setContentType(contentType);
            } else {
                response.setContentType("image/png");
            }
            
            // 设置文件名
            if (filename == null || filename.isEmpty()) {
                filename = "ai-generated.png";
            }
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            
            // 获取内容长度
            long contentLength = connection.getContentLengthLong();
            if (contentLength > 0) {
                response.setContentLengthLong(contentLength);
            }
            
            // 流式传输
            inputStream = connection.getInputStream();
            outputStream = response.getOutputStream();
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            outputStream.flush();
            log.info("图片下载完成: {}", filename);
            
        } catch (IOException e) {
            log.error("图片下载异常: url={}, error={}", imageUrl, e.getMessage(), e);
            try {
                if (!response.isCommitted()) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().write("图片下载失败: " + e.getMessage());
                }
            } catch (IOException ex) {
                log.error("写入错误响应失败", ex);
            }
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
                if (connection != null) connection.disconnect();
            } catch (IOException e) {
                log.error("关闭资源失败", e);
            }
        }
    }

    /**
     * 上传文件（Word、Excel、PDF、TXT等）并交给大模型处理
     */
    @Override
    public Flux<String> uploadFileAndChat(MultipartFile file, String message, String memoryId, Boolean webSearchEnabled) {
        if (file == null || file.isEmpty()) {
            return Flux.just("文件不能为空");
        }

        String fileName = file.getOriginalFilename();
        String fileType = fileName != null ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() : "";
        
        log.info("开始处理文件上传: fileName={}, fileType={}, fileSize={}", fileName, fileType, file.getSize());

        try {
            // 1. 提取文件内容
            String fileContent = extractFileContent(file, fileType);
            
            if (fileContent == null || fileContent.trim().isEmpty()) {
                return Flux.just("⚠️ 文件内容为空或无法解析，请检查文件格式");
            }
            
            log.info("✅ 文件内容提取成功，长度: {} 字符", fileContent.length());
            
            // 2. 构建组合提示词
            String combinedMessage = buildFileChatMessage(message, fileName, fileContent);
            
            log.info("📝 组合消息: {}", combinedMessage.length() > 100 ? combinedMessage.substring(0, 100) + "..." : combinedMessage);
            
            // 3. 生成 memoryId（如果未提供）
            String finalMemoryId = memoryId != null && !memoryId.isEmpty() ? memoryId : UUID.randomUUID().toString();
            
            // 4. 构建 ChatForm
            ChatForm chatForm = new ChatForm();
            chatForm.setMemoryId(finalMemoryId);
            chatForm.setMessage(combinedMessage);
            chatForm.setWebSearchEnabled(webSearchEnabled != null ? webSearchEnabled : false);
            
            log.info("🚀 开始调用大模型处理文件+提问");
            
            // 5. 调用现有的 chat 方法（支持流式输出）
            return chat(chatForm);
            
        } catch (Exception e) {
            log.error("❌ 文件处理失败: {}", e.getMessage(), e);
            return Flux.just("❌ 文件处理失败: " + e.getMessage());
        }
    }

    /**
     * 提取文件内容（支持 Word、Excel、PDF、TXT 等）
     */
    private String extractFileContent(MultipartFile file, String fileType) throws IOException {
        log.info("🔍 开始提取文件内容: fileType={}", fileType);
        
        try (InputStream inputStream = file.getInputStream()) {
            // 使用 Apache Tika 自动检测并提取文件内容
            Tika tika = new Tika();
            String content = tika.parseToString(inputStream);
            
            log.info("✅ Tika 解析完成，提取到 {} 字符", content.length());
            return content;
            
        } catch (Exception e) {
            log.error("❌ 文件解析失败: {}", e.getMessage(), e);
            throw new IOException("文件解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建文件对话的组合消息
     */
    private String buildFileChatMessage(String userQuestion, String fileName, String fileContent) {
        StringBuilder message = new StringBuilder();
        
        message.append("📄 用户上传了一个文件，请分析并回答用户的问题：\n\n");
        message.append("【文件信息】\n");
        message.append("- 文件名: ").append(fileName).append("\n");
        message.append("- 文件大小: 约").append(fileContent.length()).append(" 字符\n\n");
        
        message.append("【文件内容】\n");
        message.append("---\n");
        
        // 如果文件内容太长，截取前 50000 字符（约 25 页 A4 纸）
        if (fileContent.length() > 50000) {
            message.append(fileContent.substring(0, 50000));
            message.append("\n...（文件内容过长，已截取前50000字符）...");
        } else {
            message.append(fileContent);
        }
        
        message.append("\n---\n\n");
        
        message.append("【用户问题】\n");
        message.append(userQuestion != null && !userQuestion.trim().isEmpty() ? userQuestion : "请总结这个文件的主要内容");
        
        message.append("\n\n【回答要求】\n");
        message.append("1. 仔细阅读文件内容，理解核心信息\n");
        message.append("2. 针对用户问题进行详细、准确的回答\n");
        message.append("3. 如果用户要求总结，请提供结构化的要点总结\n");
        message.append("4. 如果文件内容与问题不相关，请如实告知用户\n");
        message.append("5. 回答时使用清晰的格式（标题、列表、表格等）\n");
        
        return message.toString();
    }
}
