package com.zoe.java.ai.langchain4j.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Rerank 重排序结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RerankResult {
    
    /** 原始索引（在候选列表中的位置） */
    private int index;
    
    /** 相关性得分（0-1之间，越高越相关） */
    private double score;
    
    /** 文档内容 */
    private String document;
    
    /** 文档标题（如果有） */
    private String title;
    
    /** 元数据（可选） */
    private String metadata;
}
