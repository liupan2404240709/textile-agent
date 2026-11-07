package com.zoe.java.ai.langchain4j.controller;

import com.zoe.java.ai.langchain4j.dto.ChatForm;
import com.zoe.java.ai.langchain4j.dto.ImageGenerateForm;
import com.zoe.java.ai.langchain4j.vo.ChatHistoryVo;
import com.zoe.java.ai.langchain4j.result.Result;
import com.zoe.java.ai.langchain4j.service.ChatMessageService;
import com.zoe.java.ai.langchain4j.service.PapaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/papa")
@Tag(name = "papa")
public class PapaController {

    @Autowired
    private PapaService papaService;

    @Autowired
    private ChatMessageService chatMessageService;

    @PostMapping(value = "/chat", produces = "text/stream;charset=utf-8")
    @Operation(summary = "对话")
    public Flux<String> chat(@RequestBody ChatForm chatForm) {
        log.info("收到聊天请求: memoryId={}, message={}, webSearchEnabled={}",
                chatForm.getMemoryId(),
                chatForm.getMessage() != null && chatForm.getMessage().length() > 50 ?
                        chatForm.getMessage().substring(0, 50) + "..." : chatForm.getMessage(),
                chatForm.getWebSearchEnabled());
        return papaService.chat(chatForm);
    }

    @PostMapping("/chat/stop/{memoryId}")
    @Operation(summary = "停止当前对话流")
    public Result<Void> stopChat(@PathVariable String memoryId) {
        papaService.stopChat(memoryId);
        return Result.ok();
    }

    @PostMapping("/generateImage")
    @Operation(summary = "文生图")
    public Result<Map<String, String>> generateImage(@RequestBody ImageGenerateForm imageGenerateForm) {
        return papaService.generateImage(imageGenerateForm);
    }

    @GetMapping("/history")
    @Operation(summary = "获取历史对话列表")
    public Result<List<ChatHistoryVo>> getHistory() {
        List<ChatHistoryVo> historyList = chatMessageService.getHistoryList();
        return Result.ok(historyList);
    }

    @DeleteMapping("/history/{memoryId}")
    @Operation(summary = "删除历史对话")
    public Result<Void> deleteHistory(@PathVariable String memoryId) {
        chatMessageService.deleteHistory(memoryId);
        return Result.ok();
    }

    @GetMapping("/history/{memoryId}/content")
    @Operation(summary = "获取历史对话内容")
    public Result<String> getHistoryContent(@PathVariable("memoryId") String memoryId) {
        String content = chatMessageService.getChatContent(memoryId);
        if (content == null) {
            return Result.fail("未找到该对话记录");
        }
        return Result.ok(content);
    }

    @PostMapping(value = "/readImageContent")
    @Operation(summary = "图片解析")
    public String readImageContent(@RequestParam("image") MultipartFile image,
                                   @RequestParam(value = "message", required = false) String message) throws IOException {
        return papaService.readImageContent(image, message);
    }

    @PostMapping(value = "/uploadFile", produces = "text/stream;charset=utf-8")
    @Operation(summary = "上传文件并提问（支持Word、Excel、PDF、TXT等）")
    public Flux<String> uploadFileAndChat(@RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "message", required = false, defaultValue = "请分析这个文件的内容") String message,
                                          @RequestParam(value = "memoryId", required = false) String memoryId,
                                          @RequestParam(value = "webSearchEnabled", defaultValue = "false") Boolean webSearchEnabled) {
        log.info("收到文件上传请求: fileName={}, fileSize={}, message={}",
                file.getOriginalFilename(), file.getSize(), message);
        return papaService.uploadFileAndChat(file, message, memoryId, webSearchEnabled);
    }

    @GetMapping("/downloadImage")
    @Operation(summary = "下载AI生成图片（代理）")
    public void downloadImage(@RequestParam("url") String imageUrl,
                              @RequestParam(value = "filename", required = false, defaultValue = "ai-generated.png") String filename,
                              HttpServletResponse response) {
        papaService.downloadImage(imageUrl, filename, response);
    }
}