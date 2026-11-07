package com.zoe.java.ai.langchain4j.service;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * OCR 服务 - 用于处理扫描件PDF和图片型文档
 */
@Slf4j
@Service
public class OcrService {

    @Value("${rag.ocr.enabled:true}")
    private boolean ocrEnabled;

    @Value("${rag.ocr.language:chi_sim+eng}")
    private String ocrLanguage;

    @Value("${rag.ocr.data-path:./tessdata}")
    private String tessDataPath;

    @Value("${rag.ocr.dpi:300}")
    private int dpi;

    /**
     * 使用 OCR 识别 PDF 中的文本（处理扫描件）
     * 
     * @param pdfFile PDF文件路径
     * @return 识别出的文本内容
     */
    public String recognizePdfWithOcr(Path pdfFile) throws IOException {
        if (!ocrEnabled) {
            log.warn("OCR 功能未启用，请在配置中设置 rag.ocr.enabled=true");
            throw new IllegalStateException("OCR 功能未启用");
        }

        log.info("开始 OCR 识别: {}, DPI: {}, 语言: {}", pdfFile.getFileName(), dpi, ocrLanguage);
        long startTime = System.currentTimeMillis();

        StringBuilder fullText = new StringBuilder();
        int pageCount = 0;
        
        try (PDDocument document = PDDocument.load(pdfFile.toFile())) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            pageCount = document.getNumberOfPages();
            
            log.info("PDF 共 {} 页，开始逐页 OCR 识别...", pageCount);

            for (int page = 0; page < pageCount; page++) {
                try {
                    // 将PDF页面渲染为图片
                    BufferedImage image = pdfRenderer.renderImageWithDPI(page, dpi);
                    
                    // 执行 OCR 识别
                    String pageText = performOcr(image);
                    
                    if (pageText != null && !pageText.trim().isEmpty()) {
                        fullText.append(pageText);
                        fullText.append("\n\n"); // 页之间添加分隔
                        log.info("第 {}/{} 页 OCR 完成，识别 {} 字符", page + 1, pageCount, pageText.length());
                    } else {
                        log.warn("第 {}/{} 页 OCR 结果为空", page + 1, pageCount);
                    }
                    
                    // 释放图片资源
                    image.flush();
                    
                } catch (Exception e) {
                    log.error("第 {} 页 OCR 识别失败", page + 1, e);
                    // 继续处理下一页，不中断整个流程
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        String result = fullText.toString().trim();
        
        log.info("OCR 识别完成: 总字符数={}, 耗时={}ms, 平均每页={}ms", 
                result.length(), duration, pageCount > 0 ? duration / pageCount : 0);
        
        if (result.isEmpty()) {
            throw new IOException("OCR 识别结果为空，可能原因：1)PDF完全空白；2)图片质量太差；3)缺少OCR语言包");
        }
        
        return result;
    }

    /**
     * 对单个图片执行 OCR 识别
     */
    private String performOcr(BufferedImage image) throws TesseractException {
        Tesseract tesseract = new Tesseract();
        
        // 设置 Tess4J 数据路径
        tesseract.setDatapath(tessDataPath);
        
        // 设置识别语言（中文简体+英文）
        tesseract.setLanguage(ocrLanguage);
        
        // 设置 OCR 引擎模式（3 = 默认 + LSTM）
        tesseract.setOcrEngineMode(1);
        
        // 设置页面分割模式（6 = 假设统一文本块）
        tesseract.setPageSegMode(6);
        
        // 执行识别
        return tesseract.doOCR(image);
    }

    /**
     * 检查 OCR 环境是否就绪
     */
    public boolean isOcrReady() {
        if (!ocrEnabled) {
            return false;
        }
        
        try {
            Path tessDataDir = Paths.get(tessDataPath);
            if (!Files.exists(tessDataDir)) {
                log.warn("TessData 目录不存在: {}", tessDataPath);
                return false;
            }
            
            // 检查语言包文件是否存在
            String[] languages = ocrLanguage.split("\\+");
            for (String lang : languages) {
                Path langFile = tessDataDir.resolve(lang + ".traineddata");
                if (!Files.exists(langFile)) {
                    log.warn("缺少 OCR 语言包: {}", langFile);
                    return false;
                }
            }
            
            log.info("OCR 环境检查通过");
            return true;
            
        } catch (Exception e) {
            log.error("OCR 环境检查失败", e);
            return false;
        }
    }

    /**
     * 获取 OCR 配置信息
     */
    public String getOcrInfo() {
        return String.format("OCR状态: %s | 语言: %s | DPI: %d | 数据路径: %s",
                ocrEnabled ? "已启用" : "未启用",
                ocrLanguage,
                dpi,
                tessDataPath);
    }
}
