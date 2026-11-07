package com.zoe.java.ai.langchain4j.service;

import com.zoe.java.ai.langchain4j.dto.RagLog;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RAG 文档处理服务
 */
@Service
@Slf4j
public class RagDocumentService {

    @Autowired
    private EmbeddingStore embeddingStore;  // 使用 Milvus 向量数据库

    @Autowired
    private RagLogService ragLogService;

    @Value("${rag.local.storage.path:./documents/rag_uploads}")
    private String localStoragePath;

    /**
     * 注入配置的 EmbeddingModel（使用 application.yml 中的 qwen embedding-model 配置）
     */
    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private OcrService ocrService;

    @Value("${rag.ocr.auto-fallback:true}")
    private boolean autoFallbackToOcr;

    /**
     * 上传并处理单个文档
     */
    public String uploadAndProcessDocument(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        long startTime = System.currentTimeMillis();
        String fileName = file.getOriginalFilename();
        
        log.info("📄 ========== 开始处理文档: {} ==========", fileName);
        
        // 创建处理中日志
        RagLog processingLog = ragLogService.createProcessingLog(fileName, "UPLOAD");
        ragLogService.addLog(processingLog);

        try {
            // 1. 保存文件到本地存储
            log.info("📁 步骤 1/4: 保存文件到本地...");
            String savedFileName = saveFileToLocal(file);
            
            // 2. 解析文档
            log.info("📖 步骤 2/4: 解析文档内容...");
            Document document = parseDocument(file);
            log.info("✅ 文档解析完成，文本长度: {} 字符", document.text().length());
            
            // 3. 切分文档
            log.info("✂️ 步骤 3/4: 切分文档为文本片段...");
            List<TextSegment> segments = splitDocument(document);
            log.info("✅ 文档切分完成，生成 {} 个片段", segments.size());
            
            // 4. 向量化并存储到 Milvus
            log.info("🔢 步骤 4/4: 向量化并存入 Milvus...");
            ingestToMilvus(segments);
            
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("🎉 ========== 文档处理完成: {} ==========", fileName);
            
            // 创建成功日志
            RagLog successLog = ragLogService.createSuccessLog(
                fileName, 
                "UPLOAD", 
                String.format("文档上传成功，生成 %d 个文本片段", segments.size()),
                segments.size(),
                duration
            );
            ragLogService.addLog(successLog);
            
            log.info("文档处理完成: {}, 生成 {} 个文本片段, 耗时: {}ms", fileName, segments.size(), duration);
            
            return "文档上传成功: " + fileName + ", 生成 " + segments.size() + " 个文本片段";
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            log.error("❌ ========== 文档处理失败: {}, 耗时: {}ms ==========", fileName, duration, e);
            
            // 创建失败日志
            RagLog errorLog = ragLogService.createErrorLog(
                fileName, 
                "UPLOAD", 
                e.getMessage(),
                duration
            );
            ragLogService.addLog(errorLog);
            
            log.error("文档处理失败: {}, 耗时: {}ms", fileName, duration, e);
            throw e;
        }
    }

    /**
     * 保存文件到本地存储
     */
    private String saveFileToLocal(MultipartFile file) throws IOException {
        String storagePath = localStoragePath;
        File directory = new File(storagePath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 生成唯一文件名，保留原始扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uniqueFileName = "doc_" + timestamp + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        
        Path filePath = Paths.get(storagePath, uniqueFileName);
        Files.write(filePath, file.getBytes());
        
        log.info("文件已保存到: {}", filePath.toAbsolutePath());
        
        return uniqueFileName;
    }

    /**
     * 解析文档（使用 Tika 自动检测并解析所有格式，支持 OCR 回退）
     */
    private Document parseDocument(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 预先检查文件大小
        long fileSize = file.getSize();
        if (fileSize == 0) {
            throw new IllegalArgumentException("文件为空: " + originalFilename);
        }
        log.info("开始解析文档: {}, 大小: {} bytes", originalFilename, fileSize);

        try {
            // 1. 首先尝试使用 Apache Tika 解析
            ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();
            Document document = parser.parse(file.getInputStream());
            
            // 2. 验证解析结果是否为空
            if (document != null && document.text() != null && !document.text().trim().isEmpty()) {
                int textLength = document.text().length();
                log.info("文档解析成功: {}, 提取文本长度: {} 字符", originalFilename, textLength);
                return document;
            }
            
            // 3. 如果 Tika 解析结果为空，且启用了 OCR 自动回退，则尝试 OCR
            if (autoFallbackToOcr && isPdfFile(originalFilename)) {
                log.warn("Tika 解析结果为空，尝试使用 OCR 识别: {}", originalFilename);
                return parseWithOcrFallback(file, originalFilename);
            }
            
            // 4. 如果未启用 OCR 或不是 PDF，抛出错误
            String errorMsg = String.format(
                "文档解析后内容为空: %s。可能原因：1)PDF是扫描件/图片类型，需要OCR支持；2)文件已损坏；3)文件被加密。建议：使用文本型PDF或启用OCR功能",
                originalFilename
            );
            log.warn(errorMsg);
            throw new IOException(errorMsg);
            
        } catch (dev.langchain4j.data.document.BlankDocumentException e) {
            // 5. BlankDocumentException 时尝试 OCR 回退
            if (autoFallbackToOcr && isPdfFile(originalFilename)) {
                log.warn("捕获到 BlankDocumentException，尝试使用 OCR 识别: {}", originalFilename);
                return parseWithOcrFallback(file, originalFilename);
            }
            
            String errorMsg = String.format(
                "文档解析失败（内容为空）: %s。该文件可能是扫描件、图片或加密PDF。请尝试：1)使用可复制文本的PDF；2)启用OCR功能；3)检查文件是否损坏",
                originalFilename
            );
            log.error(errorMsg, e);
            throw new IOException(errorMsg, e);
            
        } catch (Exception e) {
            log.error("Tika 解析失败: {}", originalFilename, e);
            throw new IOException("文档解析失败: " + originalFilename + ", 错误: " + e.getMessage(), e);
        }
    }

    /**
     * 使用 OCR 回退解析 PDF
     */
    private Document parseWithOcrFallback(MultipartFile file, String originalFilename) throws IOException {
        log.info("🔍 开始 OCR 处理流程: {}", originalFilename);
        
        // 检查 OCR 环境
        if (!ocrService.isOcrReady()) {
            String errorMsg = String.format(
                "OCR 环境未就绪，无法处理扫描件PDF: %s。请安装 Tesseract OCR 并下载语言包。详情: %s",
                originalFilename,
                ocrService.getOcrInfo()
            );
            log.error(errorMsg);
            throw new IOException(errorMsg);
        }
        
        log.info("✅ OCR 环境检查通过，开始识别...");
        
        try {
            // 保存临时文件用于 OCR
            Path tempFile = saveTempFileForOcr(file);
            log.info("📁 临时文件已创建: {}", tempFile.toAbsolutePath());
            
            // 执行 OCR 识别
            log.info("⏳ 正在执行 OCR 识别，这可能需要几分钟...");
            String ocrText = ocrService.recognizePdfWithOcr(tempFile);
            
            // 清理临时文件
            Files.deleteIfExists(tempFile);
            
            // 验证 OCR 结果
            if (ocrText == null || ocrText.trim().isEmpty()) {
                String errorMsg = String.format("OCR 识别结果为空: %s", originalFilename);
                log.error(errorMsg);
                throw new IOException(errorMsg);
            }
            
            // 创建 Document 对象
            Document document = Document.from(ocrText);
            
            log.info("✅ OCR 识别成功: {}, 提取文本长度: {} 字符", originalFilename, ocrText.length());
            return document;
            
        } catch (Exception e) {
            String errorMsg = String.format(
                "OCR 识别失败: %s。错误: %s。建议：1)检查PDF质量；2)确认OCR语言包已安装；3)尝试其他PDF文件",
                originalFilename,
                e.getMessage()
            );
            log.error(errorMsg, e);
            throw new IOException(errorMsg, e);
        }
    }

    /**
     * 保存临时文件用于 OCR 处理
     */
    private Path saveTempFileForOcr(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String tempFileName = "ocr_temp_" + timestamp + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        
        Path tempPath = Paths.get(localStoragePath, tempFileName);
        Files.write(tempPath, file.getBytes());
        
        log.debug("临时文件已创建: {}", tempPath.toAbsolutePath());
        return tempPath;
    }

    /**
     * 判断是否为 PDF 文件
     */
    private boolean isPdfFile(String filename) {
        if (filename == null) {
            return false;
        }
        String lowerName = filename.toLowerCase();
        return lowerName.endsWith(".pdf");
    }

    /**
     * 切分文档
     */
    private List<TextSegment> splitDocument(Document document) {
        // 使用递归字符切分器
        return DocumentSplitters.recursive(
                500,  // 每个片段最大字符数
                50    // 重叠字符数
        ).split(document);
    }

    /**
     * 将文本片段向量化并存入 Milvus
     */
    private void ingestToMilvus(List<TextSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            log.warn("⚠️ 文本片段列表为空，跳过向量化");
            return;
        }

        log.info("🔢 开始向量化 {} 个文本片段...", segments.size());
        
        // 使用配置的 EmbeddingModel（Qwen text-embedding-v3）
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        
        log.info("✅ 向量化完成，生成 {} 个向量，准备存入 Milvus...", embeddings.size());
        
        // 使用批量添加
        embeddingStore.addAll(embeddings, segments);
        
        log.info("✅ 已向量化并批量存储 {} 个文本片段到 Milvus", segments.size());
    }

    /**
     * 批量上传文档
     */
    public String uploadMultipleDocuments(MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("文件列表不能为空");
        }

        long startTime = System.currentTimeMillis();
        int successCount = 0;
        int totalSegments = 0;
        List<String> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                if (!file.isEmpty()) {
                    String fileName = file.getOriginalFilename();
                    long fileStartTime = System.currentTimeMillis();
                    
                    // 创建处理中日志
                    RagLog processingLog = ragLogService.createProcessingLog(fileName, "BATCH_UPLOAD");
                    ragLogService.addLog(processingLog);
                    
                    String savedFileName = saveFileToLocal(file);
                    Document document = parseDocument(file);
                    List<TextSegment> segments = splitDocument(document);
                    ingestToMilvus(segments);
                    
                    long duration = System.currentTimeMillis() - fileStartTime;
                    successCount++;
                    totalSegments += segments.size();
                    
                    // 创建成功日志
                    RagLog successLog = ragLogService.createSuccessLog(
                        fileName,
                        "BATCH_UPLOAD",
                        String.format("批量处理成功，生成 %d 个文本片段", segments.size()),
                        segments.size(),
                        duration
                    );
                    ragLogService.addLog(successLog);
                    
                    log.info("处理成功: {}, 片段数: {}, 耗时: {}ms", fileName, segments.size(), duration);
                }
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("处理文件失败: {}", file.getOriginalFilename(), e);
                errors.add(file.getOriginalFilename() + ": " + e.getMessage());
                
                // 创建失败日志
                RagLog errorLog = ragLogService.createErrorLog(
                    file.getOriginalFilename(),
                    "BATCH_UPLOAD",
                    e.getMessage(),
                    duration
                );
                ragLogService.addLog(errorLog);
            }
        }

        long totalDuration = System.currentTimeMillis() - startTime;
        
        StringBuilder result = new StringBuilder();
        result.append(String.format("批量处理完成: 成功 %d/%d 个文件, 共生成 %d 个文本片段, 总耗时: %dms", 
                successCount, files.length, totalSegments, totalDuration));
        
        if (!errors.isEmpty()) {
            result.append("\n失败的文件: ").append(String.join(", ", errors));
        }
        
        return result.toString();
    }

    /**
     * 获取已上传文档列表
     */
    public List<String> getUploadedDocuments() {
        File directory = new File(localStoragePath);
        if (!directory.exists()) {
            return List.of();
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return List.of();
        }

        List<String> fileNames = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) {
                fileNames.add(file.getName());
            }
        }
        
        return fileNames;
    }

    /**
     * 删除文档
     */
    public boolean deleteDocument(String fileName) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 创建处理中日志
            RagLog processingLog = ragLogService.createProcessingLog(fileName, "DELETE");
            ragLogService.addLog(processingLog);
            
            Path filePath = Paths.get(localStoragePath, fileName);
            Files.deleteIfExists(filePath);
            
            long duration = System.currentTimeMillis() - startTime;
            
            // 创建成功日志
            RagLog successLog = ragLogService.createSuccessLog(
                fileName,
                "DELETE",
                "文档已删除",
                null,
                duration
            );
            ragLogService.addLog(successLog);
            
            log.info("文档已删除: {}, 耗时: {}ms", fileName, duration);
            return true;
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            
            // 创建失败日志
            RagLog errorLog = ragLogService.createErrorLog(
                fileName,
                "DELETE",
                e.getMessage(),
                duration
            );
            ragLogService.addLog(errorLog);
            
            log.error("删除文档失败: {}, 耗时: {}ms", fileName, duration, e);
            return false;
        }
    }
}

