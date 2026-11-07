package com.zoe.java.ai.langchain4j.service;

import com.zoe.java.ai.langchain4j.dto.ChatForm;
import com.zoe.java.ai.langchain4j.dto.ImageGenerateForm;
import com.zoe.java.ai.langchain4j.result.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;

public interface PapaService {

    Flux<String> chat(ChatForm chatForm);

    void stopChat(String memoryId);

    String readImageContent(MultipartFile image, String message) throws IOException;

    Result<Map<String, String>> generateImage(ImageGenerateForm imageGenerateForm);

    void downloadImage(String imageUrl, String filename, HttpServletResponse response);

    /**
     * 上传文件（Word、Excel、PDF、TXT等）并交给大模型处理
     *
     * @param file 上传的文件
     * @param message 用户提问
     * @param memoryId 对话ID（可选）
     * @param webSearchEnabled 是否启用联网搜索
     * @return 流式响应
     */
    Flux<String> uploadFileAndChat(MultipartFile file, String message, String memoryId, Boolean webSearchEnabled);
}
