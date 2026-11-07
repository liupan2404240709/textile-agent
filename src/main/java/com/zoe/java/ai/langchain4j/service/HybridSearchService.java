package com.zoe.java.ai.langchain4j.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合检索服务
 * 结合向量检索和 BM25 关键词检索，使用 RRF（Reciprocal Rank Fusion）算法融合结果
 */
@Slf4j
@Service
public class HybridSearchService {

    /**
     * RRF 融合参数 k（经验值通常为 60）
     */
    private static final int RRF_K = 60;

    /**
     * 使用 RRF（Reciprocal Rank Fusion）算法融合向量检索和关键词检索结果
     *
     * @param vectorResults  向量检索结果（按相似度降序）
     * @param keywordResults 关键词检索结果（按 BM25 分数降序）
     * @param topK           返回前 K 个结果
     * @return 融合后的结果列表
     */
    public List<EmbeddingMatch<TextSegment>> reciprocalRankFusion(
            List<EmbeddingMatch<TextSegment>> vectorResults,
            List<EmbeddingMatch<TextSegment>> keywordResults,
            int topK) {

        log.info("🔄 开始 RRF 融合: 向量={} 条, 关键词={} 条, topK={}",
                vectorResults.size(), keywordResults.size(), topK);

        // 使用 Map 存储每个文档的 RRF 分数
        Map<String, RRFScore> scoreMap = new LinkedHashMap<>();

        // 处理向量检索结果
        for (int i = 0; i < vectorResults.size(); i++) {
            EmbeddingMatch<TextSegment> match = vectorResults.get(i);
            String docId = getDocumentId(match);
            int rank = i + 1;
            double rrfScore = 1.0 / (RRF_K + rank);

            scoreMap.computeIfAbsent(docId, k -> new RRFScore(match))
                    .addVectorScore(rrfScore, rank);
        }

        // 处理关键词检索结果
        for (int i = 0; i < keywordResults.size(); i++) {
            EmbeddingMatch<TextSegment> match = keywordResults.get(i);
            String docId = getDocumentId(match);
            int rank = i + 1;
            double rrfScore = 1.0 / (RRF_K + rank);

            scoreMap.computeIfAbsent(docId, k -> new RRFScore(match))
                    .addKeywordScore(rrfScore, rank);
        }

        // 按 RRF 分数降序排序
        List<RRFScore> sortedScores = scoreMap.values().stream()
                .sorted(Comparator.comparingDouble(RRFScore::getTotalScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());

        // 转换为 EmbeddingMatch 列表
        List<EmbeddingMatch<TextSegment>> fusedResults = sortedScores.stream()
                .map(RRFScore::getMatch)
                .collect(Collectors.toList());

        log.info("✅ RRF 融合完成: 返回 {} 条结果", fusedResults.size());

        // 打印融合详情（DEBUG 级别）
        if (log.isDebugEnabled()) {
            log.debug("\n========== 📊 RRF 融合详情 ==========");
            for (int i = 0; i < Math.min(fusedResults.size(), 10); i++) {
                RRFScore score = sortedScores.get(i);
                String preview = score.getMatch().embedded().text().length() > 50
                        ? score.getMatch().embedded().text().substring(0, 50) + "..."
                        : score.getMatch().embedded().text();
                log.debug("  #{}: [向量排名:{}, 关键词排名:{}, RRF分数:{:.4f}] {}",
                        i + 1,
                        score.getVectorRank() != null ? score.getVectorRank() : "-",
                        score.getKeywordRank() != null ? score.getKeywordRank() : "-",
                        score.getTotalScore(),
                        preview);
            }
            log.debug("=====================================\n");
        }

        return fusedResults;
    }

    /**
     * 获取文档唯一标识（用于去重）
     */
    private String getDocumentId(EmbeddingMatch<TextSegment> match) {
        // 优先使用元数据中的 document_id
        if (match.embedded().metadata() != null) {
            String docId = match.embedded().metadata().getString("document_id");
            if (docId != null && !docId.isEmpty()) {
                return docId;
            }
        }
        // 降级：使用文本内容的哈希值
        return String.valueOf(match.embedded().text().hashCode());
    }

    /**
     * RRF 分数计算辅助类
     */
    private static class RRFScore {
        private final EmbeddingMatch<TextSegment> match;
        private double vectorRRFScore = 0.0;
        private double keywordRRFScore = 0.0;
        private Integer vectorRank = null;
        private Integer keywordRank = null;

        public RRFScore(EmbeddingMatch<TextSegment> match) {
            this.match = match;
        }

        public void addVectorScore(double score, int rank) {
            this.vectorRRFScore = score;
            this.vectorRank = rank;
        }

        public void addKeywordScore(double score, int rank) {
            this.keywordRRFScore = score;
            this.keywordRank = rank;
        }

        public double getTotalScore() {
            return vectorRRFScore + keywordRRFScore;
        }

        public EmbeddingMatch<TextSegment> getMatch() {
            return match;
        }

        public Integer getVectorRank() {
            return vectorRank;
        }

        public Integer getKeywordRank() {
            return keywordRank;
        }
    }
}
