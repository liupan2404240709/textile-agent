package com.zoe.java.ai.langchain4j.service;

import com.zoe.java.ai.langchain4j.entity.RerankResult;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * 语义缓存服务
 * 对相同或相似的查询进行缓存，避免重复检索
 */
@Slf4j
@Service
public class SemanticCacheService {

    private final EmbeddingModel embeddingModel;

    /**
     * 缓存的向量存储（内存中，重启后清空）
     */
    private InMemoryEmbeddingStore<TextSegment> cacheStore;

    /**
     * 语义相似度阈值（超过此阈值认为是相同查询）
     */
    private static final double SEMANTIC_SIMILARITY_THRESHOLD = 0.95;

    /**
     * 缓存过期时间（秒），默认 1 小时
     */
    private static final long CACHE_TTL_SECONDS = 3600;

    /**
     * 最大缓存数量
     */
    private static final int MAX_CACHE_SIZE = 1000;

    public SemanticCacheService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @PostConstruct
    public void init() {
        cacheStore = new InMemoryEmbeddingStore<>();
        log.info("✅ 语义缓存初始化完成: maxCacheSize={}, ttl={}s", MAX_CACHE_SIZE, CACHE_TTL_SECONDS);
    }

    /**
     * 尝试从缓存中获取检索结果
     *
     * @param query 用户查询
     * @return 缓存的结果，如果未命中则返回 null
     */
    public List<Content> getFromCache(String query) {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }

        try {
            // 将查询向量化
            Embedding queryEmbedding = embeddingModel.embed(query).content();

            // 在缓存中搜索相似查询（使用 search 方法）
            dev.langchain4j.store.embedding.EmbeddingSearchRequest searchRequest = 
                dev.langchain4j.store.embedding.EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .minScore(SEMANTIC_SIMILARITY_THRESHOLD)
                    .maxResults(1)
                    .build();
            
            dev.langchain4j.store.embedding.EmbeddingSearchResult<TextSegment> searchResult = 
                cacheStore.search(searchRequest);
            
            List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

            if (!matches.isEmpty()) {
                EmbeddingMatch<TextSegment> match = matches.get(0);
                String cachedQuery = match.embedded().text();

                log.info("✅ 命中语义缓存: query='{}', similarTo='{}', similarity={:.4f}",
                        query, cachedQuery, match.score());

                // 从元数据中获取缓存的结果
                String cachedResultsJson = match.embedded().metadata().getString("cached_results");
                if (cachedResultsJson != null) {
                    // 这里简化处理，实际应该反序列化
                    // 为演示目的，返回 null 让外部重新检索
                    // TODO: 实现完整的缓存结果序列化/反序列化
                }

                return null;  // 暂时返回 null，需要完整实现序列化
            }

            log.debug("❌ 未命中语义缓存: query='{}'", query);
            return null;

        } catch (Exception e) {
            log.warn("⚠️ 缓存查询失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将检索结果存入缓存
     *
     * @param query   用户查询
     * @param results 检索结果
     */
    public void putToCache(String query, List<Content> results) {
        if (query == null || results == null || results.isEmpty()) {
            return;
        }

        try {
            // 检查缓存大小
            // TODO: 实现 LRU 淘汰策略

            // 将结果序列化为 JSON
            String resultsJson = serializeResults(results);

            // 构建缓存条目（使用正确的 Metadata 类）
            dev.langchain4j.data.document.Metadata metadata = 
                new dev.langchain4j.data.document.Metadata();
            metadata.put("cached_results", resultsJson);
            
            TextSegment cacheEntry = TextSegment.from(query, metadata);

            // 向量化并存入缓存
            Embedding embedding = embeddingModel.embed(query).content();
            cacheStore.add(embedding, cacheEntry);

            log.info("💾 缓存检索结果: query='{}', resultsCount={}", query, results.size());

        } catch (Exception e) {
            log.warn("⚠️ 缓存存储失败: {}", e.getMessage());
        }
    }

    /**
     * 序列化检索结果
     */
    private String serializeResults(List<Content> results) {
        // TODO: 实现完整的序列化逻辑
        // 简化处理：只存储文本内容
        return results.stream()
                .map(content -> content.textSegment().text())
                .reduce((a, b) -> a + "|||" + b)
                .orElse("");
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        cacheStore = new InMemoryEmbeddingStore<>();
        log.info("🗑️ 语义缓存已清空");
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStats getStats() {
        // TODO: 实现缓存统计
        return new CacheStats(0, 0, 0);
    }

    /**
     * 缓存统计信息
     */
    public record CacheStats(long hits, long misses, double hitRate) {
    }
}
