package com.zoe.java.ai.langchain4j.service;

import com.zoe.java.ai.langchain4j.dto.RagLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * RAG 日志服务
 */
@Service
@Slf4j
public class RagLogService {
    
    // 使用线程安全的列表存储日志
    private final CopyOnWriteArrayList<RagLog> logs = new CopyOnWriteArrayList<>();
    
    // 最大保留日志数量
    private static final int MAX_LOGS = 100;
    
    /**
     * 添加日志
     */
    public void addLog(RagLog ragLog) {
        if (ragLog.getId() == null) {
            ragLog.setId(UUID.randomUUID().toString());
        }
        if (ragLog.getCreateTime() == null) {
            ragLog.setCreateTime(LocalDateTime.now());
        }
        
        logs.add(0, ragLog); // 最新的日志在前面
        
        // 限制日志数量，避免内存溢出
        if (logs.size() > MAX_LOGS) {
            logs.remove(logs.size() - 1);
        }
        
        log.info("[RAG日志] {}: {} - {}", ragLog.getOperation(), ragLog.getStatus(), ragLog.getMessage());
    }
    
    /**
     * 获取所有日志
     */
    public List<RagLog> getAllLogs() {
        return new ArrayList<>(logs);
    }
    
    /**
     * 获取最近的日志
     */
    public List<RagLog> getRecentLogs(int count) {
        int size = Math.min(count, logs.size());
        return new ArrayList<>(logs.subList(0, size));
    }
    
    /**
     * 清空日志
     */
    public void clearLogs() {
        logs.clear();
        log.info("RAG日志已清空");
    }
    
    /**
     * 创建处理中日志
     */
    public RagLog createProcessingLog(String fileName, String operation) {
        return RagLog.builder()
                .fileName(fileName)
                .operation(operation)
                .status("PROCESSING")
                .message("开始处理...")
                .createTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建成功日志
     */
    public RagLog createSuccessLog(String fileName, String operation, String message, Integer segmentCount, Long duration) {
        return RagLog.builder()
                .fileName(fileName)
                .operation(operation)
                .status("SUCCESS")
                .message(message)
                .segmentCount(segmentCount)
                .duration(duration)
                .createTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建失败日志
     */
    public RagLog createErrorLog(String fileName, String operation, String errorMessage, Long duration) {
        return RagLog.builder()
                .fileName(fileName)
                .operation(operation)
                .status("ERROR")
                .message(errorMessage)
                .duration(duration)
                .createTime(LocalDateTime.now())
                .build();
    }
}
