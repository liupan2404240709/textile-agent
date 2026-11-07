package com.zoe.java.ai.langchain4j.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 查询重写服务
 * 通过同义词扩展、意图识别等策略优化用户查询，提升检索效果
 */
@Slf4j
@Service
public class QueryRewriteService {

    /**
     * 纺织行业专业术语同义词映射表
     */
    private static final Map<String, List<String>> DESIGN_SYNONYMS = new HashMap<>();

    /**
     * 纺织材料、染整工艺与检测标准同义词映射表
     */
    private static final Map<String, List<String>> ERGONOMIC_SYNONYMS = new HashMap<>();

    static {
        // 纺织行业核心术语
        DESIGN_SYNONYMS.put("纺织", Arrays.asList("纺织品", "纺织工业", "Textile", "面料"));
        DESIGN_SYNONYMS.put("面料", Arrays.asList("织物", "布料", "Fabric", "面布"));
        DESIGN_SYNONYMS.put("面料设计", Arrays.asList("织物设计", "Fabric Design", "织品设计", "纺织材料设计"));
        DESIGN_SYNONYMS.put("服装设计", Arrays.asList("服装开发", "Fashion Design", "款式设计", "成衣设计"));
        DESIGN_SYNONYMS.put("家纺设计", Arrays.asList("家用纺织品设计", "Home Textile", "床品设计", "窗帘设计"));
        DESIGN_SYNONYMS.put("花型设计", Arrays.asList("图案设计", "印花图案", "Pattern Design", "花样设计"));
        DESIGN_SYNONYMS.put("印染", Arrays.asList("染整", "染色印花", "Dyeing and Printing", "印染工艺"));
        DESIGN_SYNONYMS.put("针织", Arrays.asList("针织工艺", "Knitting", "经编", "纬编"));
        DESIGN_SYNONYMS.put("设计评审", Arrays.asList("Design Review", "面料评审", "方案评审", "大货评审"));
        DESIGN_SYNONYMS.put("打样", Arrays.asList("试样", "Sampling", "Lab Dip", "Strike Off", "手样"));

        // 纺织材料、染整工艺与检测标准
        ERGONOMIC_SYNONYMS.put("纱支", Arrays.asList("纱线支数", "Yarn Count", "支数", "S/Ne"));
        ERGONOMIC_SYNONYMS.put("克重", Arrays.asList("平方米克重", "Fabric Weight", "GSM", "面密度"));
        ERGONOMIC_SYNONYMS.put("色牢度", Arrays.asList("Color Fastness", "掉色程度", "沾色等级", "耐洗色牢度"));
        ERGONOMIC_SYNONYMS.put("缩水率", Arrays.asList("尺寸变化率", "Shrinkage", "水洗缩水", "Dimensional Change"));
        ERGONOMIC_SYNONYMS.put("起球", Arrays.asList("起毛起球", "Pilling", "抗起球性", "表面起球"));
        ERGONOMIC_SYNONYMS.put("纤维", Arrays.asList("纺织纤维", "Fiber", "天然纤维", "化学纤维"));
        ERGONOMIC_SYNONYMS.put("后整理", Arrays.asList("Finishing", "功能整理", "定型整理", "柔软整理"));
        ERGONOMIC_SYNONYMS.put("缸差", Arrays.asList("批差", "Batch Difference", "色差", "Color Difference"));
        ERGONOMIC_SYNONYMS.put("检测标准", Arrays.asList("检测依据", "Testing Standard", "GB/T", "FZ/T"));
        ERGONOMIC_SYNONYMS.put("可持续纺织", Arrays.asList("环保纺织", "Sustainable Textile", "再生纤维", "GRS"));
    }

    /**
     * 重写用户查询，增强检索效果
     *
     * @param originalQuery 原始查询
     * @return 优化后的查询
     */
    public String rewriteQuery(String originalQuery) {
        if (originalQuery == null || originalQuery.trim().isEmpty()) {
            return originalQuery;
        }

        log.info("🔄 开始查询重写: '{}'", originalQuery);

        String rewritten = originalQuery.trim();

        // 策略1：同义词扩展
        rewritten = expandSynonyms(rewritten);

        // 策略2：专业术语标准化
        rewritten = normalizeTechnicalTerms(rewritten);

        // 策略3：意图增强
        rewritten = enhanceIntent(rewritten);

        log.info("✅ 查询重写完成: '{}' -> '{}'", originalQuery, rewritten);

        return rewritten;
    }

    /**
     * 策略1：同义词扩展
     * 为查询中的关键词添加同义词，使用 OR 逻辑扩展检索范围
     */
    private String expandSynonyms(String query) {
        String result = query;

        // 合并所有同义词表
        Map<String, List<String>> allSynonyms = new HashMap<>();
        allSynonyms.putAll(DESIGN_SYNONYMS);
        allSynonyms.putAll(ERGONOMIC_SYNONYMS);

        // 查找查询中的关键词并扩展
        for (Map.Entry<String, List<String>> entry : allSynonyms.entrySet()) {
            String keyword = entry.getKey();
            List<String> synonyms = entry.getValue();

            // 使用不区分大小写的匹配
            Pattern pattern = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(result).find()) {
                // 构建扩展查询：原始词 OR 同义词
                String expanded = "(" + keyword + " OR " + String.join(" OR ", synonyms) + ")";
                result = pattern.matcher(result).replaceAll(expanded);
                log.debug("  📝 同义词扩展: '{}' -> '{}'", keyword, expanded);
            }
        }

        return result;
    }

    /**
     * 策略2：专业术语标准化
     * 将口语化表达转换为专业术语
     */
    private String normalizeTechnicalTerms(String query) {
        String result = query;

        // 口语化 -> 专业化映射
        Map<String, String> normalizationMap = new HashMap<>();
        normalizationMap.put("好不好看", "花型美观度 图案评价");
        normalizationMap.put("好看吗", "花型美观度 图案评价");
        normalizationMap.put("掉色吗", "色牢度 沾色 褪色");
        normalizationMap.put("会缩水吗", "缩水率 尺寸变化率");
        normalizationMap.put("起球吗", "起毛起球 抗起球性");
        normalizationMap.put("舒服吗", "亲肤性 柔软度 服用性能");
        normalizationMap.put("做得出来吗", "工艺可行性 试纺试织可行性");
        normalizationMap.put("好不好做", "工艺可行性 染整难度");
        normalizationMap.put("成本高吗", "成本核算 用纱量 染费");
        normalizationMap.put("多少钱", "成本 报价");
        normalizationMap.put("怎么选", "选型建议 面料选择指南");
        normalizationMap.put("有什么区别", "区别 差异 对比");
        normalizationMap.put("哪个更好", "优缺点对比 推荐");

        for (Map.Entry<String, String> entry : normalizationMap.entrySet()) {
            if (result.contains(entry.getKey())) {
                result = result.replace(entry.getKey(), entry.getValue());
                log.debug("  📝 术语标准化: '{}' -> '{}'", entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * 策略3：意图增强
     * 根据查询意图添加相关关键词
     */
    private String enhanceIntent(String query) {
        String result = query;

        // 对比类查询增强
        if (isComparisonQuery(query)) {
            result += " 区别 差异 对比 比较 不同";
            log.debug("  📝 意图增强: 对比类查询");
        }

        // 推荐类查询增强
        if (isRecommendationQuery(query)) {
            result += " 推荐 选购 选择 建议 指南";
            log.debug("  📝 意图增强: 推荐类查询");
        }

        // 技术参数类查询增强
        if (isTechnicalQuery(query)) {
            result += " 技术参数 性能指标 规格 标准";
            log.debug("  📝 意图增强: 技术参数类查询");
        }

        return result;
    }

    /**
     * 判断是否为对比类查询
     */
    private boolean isComparisonQuery(String query) {
        String[] keywords = {"对比", "区别", "差异", "比较", "不同", "vs", "versus"};
        return Arrays.stream(keywords)
                .anyMatch(query::contains);
    }

    /**
     * 判断是否为推荐类查询
     */
    private boolean isRecommendationQuery(String query) {
        String[] keywords = {"推荐", "选购", "选择", "哪个", "怎么挑", "如何选"};
        return Arrays.stream(keywords)
                .anyMatch(query::contains);
    }

    /**
     * 判断是否为技术参数类查询
     */
    private boolean isTechnicalQuery(String query) {
        String[] keywords = {"参数", "指标", "规格", "性能", "指数", "标准"};
        return Arrays.stream(keywords)
                .anyMatch(query::contains);
    }

    /**
     * 批量重写查询（用于处理多个查询）
     */
    public List<String> rewriteQueries(List<String> queries) {
        return queries.stream()
                .map(this::rewriteQuery)
                .collect(Collectors.toList());
    }
}
