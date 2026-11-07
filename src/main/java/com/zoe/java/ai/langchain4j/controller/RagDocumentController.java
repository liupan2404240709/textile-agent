package com.zoe.java.ai.langchain4j.controller;

import com.zoe.java.ai.langchain4j.dto.RagLog;
import com.zoe.java.ai.langchain4j.result.Result;
import com.zoe.java.ai.langchain4j.service.RagDocumentService;
import com.zoe.java.ai.langchain4j.service.RagLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * RAG 文档管理控制器
 */
@RestController
@RequestMapping("/rag")
@Tag(name = "RAG 文档管理")
@Slf4j
public class RagDocumentController {

    @Autowired
    private RagDocumentService ragDocumentService;

    @Autowired
    private RagLogService ragLogService;

    /**
     * 上传单个文档
     */
    @PostMapping("/upload")
    @Operation(summary = "上传单个文档到 RAG 系统")
    public Result<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            String result = ragDocumentService.uploadAndProcessDocument(file);
            return Result.ok(result);
        } catch (IOException e) {
            log.error("上传文档失败", e);
            return Result.fail("上传文档失败: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 批量上传文档
     */
    @PostMapping("/upload-batch")
    @Operation(summary = "批量上传文档到 RAG 系统")
    public Result<String> uploadBatchDocuments(@RequestParam("files") MultipartFile[] files) {
        try {
            String result = ragDocumentService.uploadMultipleDocuments(files);
            return Result.ok(result);
        } catch (IOException e) {
            log.error("批量上传文档失败", e);
            return Result.fail("批量上传文档失败: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 获取已上传文档列表
     */
    @GetMapping("/documents")
    @Operation(summary = "获取已上传的文档列表")
    public Result<List<String>> getDocuments() {
        try {
            List<String> documents = ragDocumentService.getUploadedDocuments();
            return Result.ok(documents);
        } catch (Exception e) {
            log.error("获取文档列表失败", e);
            return Result.fail("获取文档列表失败: " + e.getMessage());
        }
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/document/{fileName}")
    @Operation(summary = "删除指定文档")
    public Result<Boolean> deleteDocument(@PathVariable String fileName) {
        try {
            boolean success = ragDocumentService.deleteDocument(fileName);
            if (success) {
                return Result.ok(true);
            } else {
                return Result.fail("删除文档失败");
            }
        } catch (Exception e) {
            log.error("删除文档失败: {}", fileName, e);
            return Result.fail("删除文档失败: " + e.getMessage());
        }
    }

    /**
     * 获取处理日志
     */
    @GetMapping("/logs")
    @Operation(summary = "获取 RAG 处理日志")
    public Result<List<RagLog>> getLogs(
            @RequestParam(value = "count", defaultValue = "50") Integer count) {
        try {
            List<RagLog> logs = ragLogService.getRecentLogs(count);
            return Result.ok(logs);
        } catch (Exception e) {
            log.error("获取日志失败", e);
            return Result.fail("获取日志失败: " + e.getMessage());
        }
    }

    /**
     * 清空日志
     */
    @DeleteMapping("/logs")
    @Operation(summary = "清空 RAG 处理日志")
    public Result<String> clearLogs() {
        try {
            ragLogService.clearLogs();
            return Result.ok("日志已清空");
        } catch (Exception e) {
            log.error("清空日志失败", e);
            return Result.fail("清空日志失败: " + e.getMessage());
        }
    }
}
