package com.zoe.java.ai.langchain4j.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RAG 文档处理日志
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagLog {
    
    /**
     * 日志ID
     */
    private String id;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 操作类型：UPLOAD, DELETE, BATCH_UPLOAD
     */
    private String operation;
    
    /**
     * 状态：SUCCESS, ERROR, PROCESSING
     */
    private String status;
    
    /**
     * 消息内容
     */
    private String message;
    
    /**
     * 文本片段数量
     */
    private Integer segmentCount;
    
    /**
     * 耗时（毫秒）
     */
    private Long duration;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
