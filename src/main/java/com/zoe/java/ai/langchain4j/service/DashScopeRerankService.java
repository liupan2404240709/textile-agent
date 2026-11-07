package com.zoe.java.ai.langchain4j.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoe.java.ai.langchain4j.entity.RerankResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DashScope Rerank 服务
 * 使用阿里云百炼的重排序 API 对检索结果进行精排
 */
@Slf4j
@Service
public class DashScopeRerankService {

    @Value("${dashscope.api-key:}")
    private String apiKey;

    @Value("${rag.rerank.enabled:true}")
    private boolean rerankEnabled;

    @Value("${rag.rerank.model:gpt-4o-rerank}")
    private String rerankModel;

    @Value("${rag.rerank.threshold:0.7}")
    private double rerankThreshold;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public DashScopeRerankService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .baseUrl("https://dashscope.aliyuncs.com")
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * 对候选文档进行重排序
     *
     * @param query     用户查询
     * @param documents 候选文档列表
     * @param topK      返回前K个结果
     * @return 重排序后的结果（按相关性降序排列）
     */
    public List<RerankResult> rerank(String query, List<String> documents, int topK) {
        if (!rerankEnabled) {
            log.info("⚠️ Rerank 功能未启用，跳过重排序");
            return createFallbackResults(documents, topK);
        }

        if (documents == null || documents.isEmpty()) {
            log.warn("⚠️ 候选文档列表为空，跳过 Rerank");
            return Collections.emptyList();
        }

        log.info("🔄 开始 Rerank 重排序: query='{}', candidates={}, topK={}, model={}",
                query, documents.size(), topK, rerankModel);

        long startTime = System.currentTimeMillis();

        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", rerankModel);
            requestBody.put("query", query);
            requestBody.put("documents", documents);
            requestBody.put("top_n", Math.min(topK, documents.size()));
            requestBody.put("return_documents", false);  // 不返回文档内容，减少响应大小

            // 调用 DashScope Rerank API
            String responseJson = webClient.post()
                    .uri("/api/v1/services/rerank/text-rerank/text-rerank")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("X-DashScope-SSE", "disable")  // 禁用 SSE
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (responseJson == null || responseJson.isEmpty()) {
                log.warn("⚠️ Rerank API 返回为空");
                return createFallbackResults(documents, topK);
            }

            // 解析响应
            List<RerankResult> results = parseRerankResponse(responseJson, documents);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Rerank 完成: 耗时 {}ms, 返回 {} 条结果", duration, results.size());

            // 打印重排序前后对比（仅日志级别为 DEBUG 时）
            if (log.isDebugEnabled()) {
                logRerankComparison(query, documents, results);
            }

            return results;

        } catch (Exception e) {
            log.error("❌ Rerank 失败: {}", e.getMessage(), e);
            // 打印请求详情以便调试
            log.error("🔍 请求详情: model={}, query={}, documents={}, topK={}", 
                    rerankModel, query, documents.size(), topK);
            return createFallbackResults(documents, topK);
        }
    }

    /**
     * 解析 Rerank API 响应
     */
    private List<RerankResult> parseRerankResponse(String responseJson, List<String> documents) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode outputNode = root.path("output");

            if (outputNode.isMissingNode() || outputNode.isNull()) {
                log.warn("⚠️ Rerank 响应中缺少 output 字段");
                return createFallbackResults(documents, documents.size());
            }

            JsonNode resultsNode = outputNode.path("results");
            if (!resultsNode.isArray() || resultsNode.isEmpty()) {
                log.warn("⚠️ Rerank 响应中 results 为空");
                return createFallbackResults(documents, documents.size());
            }

            List<RerankResult> results = new ArrayList<>();
            for (JsonNode item : resultsNode) {
                int index = item.path("index").asInt(-1);
                double score = item.path("relevance_score").asDouble(0.0);

                if (index >= 0 && index < documents.size()) {
                    RerankResult result = RerankResult.builder()
                            .index(index)
                            .score(score)
                            .document(documents.get(index))
                            .build();
                    results.add(result);
                }
            }

            // 按得分降序排序
            results.sort(Comparator.comparingDouble(RerankResult::getScore).reversed());

            return results;

        } catch (Exception e) {
            log.error("❌ 解析 Rerank 响应失败: {}", e.getMessage(), e);
            return createFallbackResults(documents, documents.size());
        }
    }

    /**
     * 降级策略：如果 Rerank API 失败，按原始顺序返回
     */
    private List<RerankResult> createFallbackResults(List<String> documents, int topK) {
        log.warn("⚠️ 使用降级策略：按原始顺序返回 Top-{}", Math.min(documents.size(), topK));

        List<RerankResult> fallback = new ArrayList<>();
        for (int i = 0; i < Math.min(documents.size(), topK); i++) {
            RerankResult result = RerankResult.builder()
                    .index(i)
                    .score(0.5)  // 默认分数
                    .document(documents.get(i))
                    .build();
            fallback.add(result);
        }
        return fallback;
    }

    /**
     * 打印重排序前后对比（便于调试）
     */
    private void logRerankComparison(String query, List<String> originalDocs,
                                     List<RerankResult> rerankedResults) {
        log.debug("\n========== 📊 Rerank 重排序对比 ==========");
        log.debug("📝 查询: {}", query);

        log.debug("\n【重排序前 - 向量检索结果】");
        for (int i = 0; i < Math.min(originalDocs.size(), 5); i++) {
            String preview = originalDocs.get(i).length() > 60
                    ? originalDocs.get(i).substring(0, 60) + "..."
                    : originalDocs.get(i);
            log.debug("  #{}: {}", i + 1, preview);
        }

        log.debug("\n【重排序后 - Rerank 精排结果】");
        for (int i = 0; i < rerankedResults.size(); i++) {
            RerankResult result = rerankedResults.get(i);
            String preview = result.getDocument().length() > 60
                    ? result.getDocument().substring(0, 60) + "..."
                    : result.getDocument();
            log.debug("  #{}: [得分: {:.4f}] {}",
                    i + 1, result.getScore(), preview);
        }

        log.debug("===========================================\n");
    }

    /**
     * 检查 Rerank 功能是否启用
     */
    public boolean isEnabled() {
        return rerankEnabled;
    }

    /**
     * 获取 Rerank 阈值
     */
    public double getThreshold() {
        return rerankThreshold;
    }
}
