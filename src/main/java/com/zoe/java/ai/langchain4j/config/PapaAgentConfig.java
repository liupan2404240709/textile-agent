package com.zoe.java.ai.langchain4j.config;

import com.zoe.java.ai.langchain4j.entity.RerankResult;
import com.zoe.java.ai.langchain4j.service.DashScopeRerankService;
import com.zoe.java.ai.langchain4j.store.MySqlChatMemoryStore;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class PapaAgentConfig {

    private final MySqlChatMemoryStore mySqlChatMemoryStore;
    private final DashScopeRerankService rerankService;

    @Autowired
    public PapaAgentConfig(MySqlChatMemoryStore mySqlChatMemoryStore,
                          DashScopeRerankService rerankService) {
        this.mySqlChatMemoryStore = mySqlChatMemoryStore;
        this.rerankService = rerankService;
    }

    @Bean
    public ChatMemoryProvider chatMemoryProviderPapa() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(1000)
                .chatMemoryStore(mySqlChatMemoryStore)
                .build();
    }

    /**
     * 启用 RAG 检索，使用 Milvus Embedding Store + Rerank 重排序
     * 两阶段检索架构：
     *   阶段1：向量检索快速召回 Top-20 候选
     *   阶段2：Rerank 精排选出 Top-5 高质量结果
     */
    @Bean
    @Primary
    public ContentRetriever contentRetrieverPapa(
            @Qualifier("milvusEmbeddingStore") EmbeddingStore embeddingStore,
            EmbeddingModel embeddingModel) {

        return new ContentRetriever() {
            @Override
            public List<Content> retrieve(Query query) {
                long startTime = System.currentTimeMillis();
                String queryText = query.text();

                log.info("\n========== 🔍 RAG 检索开始（含 Rerank） ==========");
                log.info("📝 用户查询: {}", queryText);

                try {
                    // ================= 阶段1：向量召回 =================
                    log.info("\n【阶段1】向量检索 - 快速召回候选集");
                    long embedStartTime = System.currentTimeMillis();
                    Embedding queryEmbedding = embeddingModel.embed(queryText).content();
                    long embedDuration = System.currentTimeMillis() - embedStartTime;
                    log.info("✅ 查询向量化完成，耗时: {}ms", embedDuration);

                    // 先召回更多候选（Top-20），供 Rerank 精排
                    long searchStartTime = System.currentTimeMillis();
                    EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                            .queryEmbedding(queryEmbedding)
                            .minScore(0.5)
                            .maxResults(20)  // 召回 Top-20 候选集
                            .build();

                    EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
                    long searchDuration = System.currentTimeMillis() - searchStartTime;

                    List<EmbeddingMatch<TextSegment>> candidates = searchResult.matches();

                    log.info("✅ 向量召回完成，耗时: {}ms", searchDuration);
                    log.info("📊 召回候选数量: {}", candidates.size());

                    if (candidates.isEmpty()) {
                        log.warn("⚠️ 向量检索无结果，直接返回");
                        log.info("\n========== ✅ RAG 检索完成（无结果） ==========");
                        return List.of();
                    }

                    // ================= 阶段2：Rerank 重排序 =================
                    log.info("\n【阶段2】Rerank 重排序 - 精准精排");
                    long rerankStartTime = System.currentTimeMillis();

                    // 提取候选文档内容
                    List<String> candidateDocuments = candidates.stream()
                        .map(match -> match.embedded().text())
                        .collect(Collectors.toList());

                    // 调用 Rerank API 进行精排
                    List<RerankResult> rerankedResults = rerankService.rerank(
                        queryText,
                        candidateDocuments,
                        10  // Rerank 返回 Top-10
                    );

                    long rerankDuration = System.currentTimeMillis() - rerankStartTime;
                    log.info("✅ Rerank 精排完成，耗时: {}ms", rerankDuration);

                    // ================= 阶段3：质量过滤 =================
                    log.info("\n【阶段3】质量过滤 - 最终筛选");
                    double RERANK_THRESHOLD = rerankService.getThreshold();  // 从配置读取阈值

                    List<Content> finalResults = rerankedResults.stream()
                        .filter(result -> {
                            boolean pass = result.getScore() >= RERANK_THRESHOLD;
                            if (!pass) {
                                log.debug("🔽 过滤低分结果: score={:.4f} < threshold={:.4f}",
                                        result.getScore(), RERANK_THRESHOLD);
                            }
                            return pass;
                        })
                        .limit(5)  // 最终返回 Top-5
                        .map(result -> {
                            // 从原始候选中获取对应的 TextSegment
                            TextSegment segment = candidates.get(result.getIndex()).embedded();
                            return Content.from(segment);
                        })
                        .collect(Collectors.toList());

                    // ================= 汇总统计 =================
                    long totalDuration = System.currentTimeMillis() - startTime;

                    log.info("\n========== ✅ RAG 检索完成 ==========");
                    log.info("📊 最终结果: {} 条", finalResults.size());
                    log.info("⏱️ 总耗时: {}ms", totalDuration);
                    log.info("   - 向量检索: {}ms (召回 {} 条)", searchDuration, candidates.size());
                    log.info("   - Rerank 精排: {}ms (过滤到 {} 条)", rerankDuration, finalResults.size());

                    // 打印最终结果预览
                    if (!finalResults.isEmpty()) {
                        log.info("\n📄 最终返回内容预览:");
                        for (int i = 0; i < finalResults.size(); i++) {
                            Content content = finalResults.get(i);
                            String preview = content.textSegment().text().length() > 100
                                    ? content.textSegment().text().substring(0, 100) + "..."
                                    : content.textSegment().text();
                            log.info("  #{}: {}", i + 1, preview);
                        }
                    }

                    log.info("=====================================\n");

                    return finalResults;

                } catch (Exception e) {
                    long totalDuration = System.currentTimeMillis() - startTime;
                    log.error("\n========== ❌ RAG 检索失败，耗时: {}ms，降级为无检索结果（聊天仍可进行） ==========",
                            totalDuration, e);
                    // 检索失败（如嵌入/Rerank 模型配额耗尽、网络异常）时降级为空结果，
                    // 避免整条对话请求因 RAG 不可用而 500。知识库检索增强暂不可用。
                    return List.of();
                }
            }
        };
    }

}
