package com.zoe.java.ai.langchain4j.config;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MCP 工具调用详细日志监听器
 * 用于记录 MCP Web Search 等工具的完整请求和响应信息
 */
@Slf4j
@Component
public class McpToolExecutionLogger implements ChatModelListener {

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        // 记录每次请求中的工具执行结果
        List<ChatMessage> messages = requestContext.chatRequest().messages();
        
        long toolResultCount = messages.stream()
                .filter(m -> m instanceof ToolExecutionResultMessage)
                .count();
        
        if (toolResultCount > 0) {
            log.info("\n" + "=".repeat(100));
            log.info("🔧 [MCP 工具执行结果] 本次请求包含 {} 个工具返回结果", toolResultCount);
            log.info("=".repeat(100));
            
            int index = 0;
            for (ChatMessage message : messages) {
                if (message instanceof ToolExecutionResultMessage toolResult) {
                    index++;
                    log.info("\n┌─────────────────────────────────────────────────────────────────");
                    log.info("│ 📥 MCP 工具结果 #{}", index);
                    log.info("├─────────────────────────────────────────────────────────────────");
                    log.info("│ 工具名称: {}", toolResult.toolName());
                    log.info("│ 工具 ID: {}", toolResult.id());
                    
                    String resultText = "";
                    try {
                        resultText = toolResult.text();
                    } catch (Exception e) {
                        log.error("│ 获取工具结果文本失败", e);
                    }
                    
                    if (resultText != null && !resultText.isEmpty()) {
                        log.info("│ 结果长度: {} 字符", resultText.length());
                        log.info("├─────────────────────────────────────────────────────────────────");
                        log.info("│ 📄 完整返回内容:");
                        log.info("│ " + "─".repeat(96));
                        
                        // 分行显示，避免日志过长
                        String[] lines = resultText.split("\n");
                        for (String line : lines) {
                            log.info("│ {}", line);
                        }
                        
                        log.info("│ " + "─".repeat(96));
                    } else {
                        log.info("│ ⚠️  结果为空");
                    }
                    log.info("└─────────────────────────────────────────────────────────────────\n");
                }
            }
            log.info("=".repeat(100) + "\n");
        }
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        // 记录模型的工具调用请求
        var resp = responseContext.chatResponse();
        if (resp == null) {
            return;
        }
        
        var aiMessage = resp.aiMessage();
        if (aiMessage != null && aiMessage.hasToolExecutionRequests()) {
            log.info("\n" + "=".repeat(100));
            log.info("🔧 [MCP 工具调用请求] 模型请求调用 {} 个工具", aiMessage.toolExecutionRequests().size());
            log.info("=".repeat(100));
            
            for (int i = 0; i < aiMessage.toolExecutionRequests().size(); i++) {
                var toolReq = aiMessage.toolExecutionRequests().get(i);
                log.info("\n┌─────────────────────────────────────────────────────────────────");
                log.info("│ 📤 MCP 工具调用 #{}", i + 1);
                log.info("├─────────────────────────────────────────────────────────────────");
                log.info("│ 工具名称: {}", toolReq.name());
                log.info("│ 工具 ID: {}", toolReq.id());
                log.info("│ 工具参数: {}", toolReq.arguments());
                log.info("└─────────────────────────────────────────────────────────────────\n");
            }
            log.info("=".repeat(100) + "\n");
        }
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        log.error("❌ [MCP 工具执行] 发生错误: {}", errorContext.error().getMessage(), errorContext.error());
    }
}
