package com.zoe.java.ai.langchain4j.config;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.List;

/**
 * Milvus 向量数据库配置
 */
@Configuration
@Slf4j
public class MilvusConfig {

    @Value("${rag.milvus.host:localhost}")
    private String milvusHost;

    @Value("${rag.milvus.port:19530}")
    private int milvusPort;

    @Value("${rag.milvus.collectionName:textile_knowledge}")
    private String collectionName;

    @Value("${rag.local.storage.path:./documents/rag_uploads}")
    private String localStoragePath;

    @Value("${rag.milvus.enabled:true}")
    private boolean milvusEnabled;

    /**
     * 创建 Embedding Store
     * 如果 Milvus 不可用，则使用内存存储作为降级方案
     */
    @Bean
    public EmbeddingStore milvusEmbeddingStore() {
        if (!milvusEnabled) {
            log.warn("Milvus 已禁用，使用内存 Embedding Store（重启后数据会丢失）");
            return new InMemoryEmbeddingStore<>();
        }
        
        try {
            log.info("正在连接 Milvus: {}:{}", milvusHost, milvusPort);
            return dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore.builder()
                    .host(milvusHost)
                    .port(milvusPort)
                    .collectionName(collectionName)
                    .dimension(1024) // text-embedding-v3 实际输出1024维
                    .autoFlushOnInsert(false) // 关闭自动刷新，避免触发Milvus速率限制
                    .build();
        } catch (Exception e) {
            log.error("Milvus 连接失败，降级使用内存 Embedding Store。错误: {}", e.getMessage());
            log.warn("提示：如果需要持久化存储，请确保 Milvus 服务已启动并正确配置");
            return new InMemoryEmbeddingStore<>();
        }
    }

    /**
     * 获取本地存储路径
     */
    public String getLocalStoragePath() {
        File directory = new File(localStoragePath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return localStoragePath;
    }

    /**
     * 加载本地文件夹中的所有文档
     */
    public List<Document> loadDocumentsFromLocalFolder() {
        String folderPath = getLocalStoragePath();
        File folder = new File(folderPath);
        
        if (!folder.exists() || !folder.isDirectory()) {
            return List.of();
        }

        // 加载所有支持的文档格式
        return FileSystemDocumentLoader.loadDocuments(folderPath);
    }
}
