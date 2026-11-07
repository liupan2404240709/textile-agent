package com.zoe.java.ai.langchain4j.config;

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.data.document.Document;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * 统一 RAG 配置
 * 确保文档上传和对话检索使用同一个 EmbeddingStore 实例
 */
@Slf4j
@Configuration
public class UnifiedRagConfig {

    @Value("${langchain4j.community.dashscope.embedding-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.community.dashscope.embedding-model.model-name:qwen3.7-text-embedding}")
    private String embeddingModelName;

    @Value("${langchain4j.community.dashscope.streaming-chat-model.model-name:qwen-plus}")
    private String chatModelName;

    /**
     * 创建统一的 Embedding Model Bean
     */
    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        log.info("初始化统一的 Qwen Embedding Model: {}", embeddingModelName);
        return QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(embeddingModelName)
                .build();
    }

    /**
     * 创建统一的 Chat Model Bean
     */
    @Bean("qwenChatModel")
    @Primary
    public ChatModel qwenChatModel() {
        log.info("初始化统一的 Qwen Chat Model: {}", chatModelName);
        return QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName(chatModelName)
                // Qwen3.8-27B 属于视觉语言模型，DashScope 原生 text-generation 端点不接受它
                // （会返回 InvalidParameter: url error）。显式声明多模态，走 multimodal-generation 端点。
                .isMultimodalModel(true)
                .build();
    }

    /**
     * 创建统一的 Streaming Chat Model Bean
     */
    @Bean("qwenStreamingChatModel")
    @Primary
    public StreamingChatModel qwenStreamingChatModel() {
        log.info("初始化统一的 Qwen Streaming Chat Model: {}", chatModelName);
        return QwenStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(chatModelName)
                // 同上：Qwen3.8-27B 需走多模态端点，否则流式对话会因 text-generation 端点被拒而卡住
                .isMultimodalModel(true)
                .build();
    }

    /**
     * 提供获取预加载文档的方法（可选）
     */
    public List<Document> loadPreloadedDocuments() {
        // 这里可以添加启动时自动加载文档的逻辑
        return List.of();
    }
}
