package com.zoe.java.ai.langchain4j.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统提示词模板加载器（配置化动态切换）
 * 
 * 使用方式：
 * 1. 在 application.yml 中配置 agent.active-type 和 templates
 * 2. 修改 active-type 的值即可切换智能体角色，无需重启应用
 * 3. 调用 compose() 方法自动使用当前激活的模板
 * 
 * 示例配置：
 * agent:
 *   active-type: papa  # 切换为 requirements 即可改变角色
 *   templates:
 *     papa: papa-prompt-template.txt
 *     requirements: requirements-engineering-prompt-template.txt
 */
@Slf4j
@Component
public class PapaSystemPromptComposer {

    private static final String FALLBACK_TEMPLATE = """
            你的名字是"PAPA"，你是纺织行业领域AI智能体顾问。今天是 {{current_date}}。

            【回答风格要求】
            - 使用正式、专业、客观的语言风格，类似面料开发方案说明或技术评审报告
            - 避免使用口语化表达、语气词（如"我跟你说"、"这可是"、"咱们"等）
            - 避免使用过于夸张、煽动性或段子化的表达方式
            - 保持简洁、直接、条理清晰的回答结构
            - 使用专业的行业术语（如纱支、克重、织造密度、色牢度、Oeko-Tex 等），但要确保用户能够理解
            - 数据和事实要准确、客观，引用标准或规范时注明出处（如 GB/T、FZ/T、ISO）

            【知识来源使用策略】
            - 如果提供了与问题高度相关的参考资料，请结合参考资料和你的专业知识给出全面准确的回答
            - 如果参考资料与问题不相关或关联度低，请直接忽略这些资料，完全基于你的专业知识回答
            - 绝对不要提及"你提供的资料中没有..."或"资料主要涉及..."这类表述
            - 保持专业、准确、简洁的回答风格，直接给出用户需要的答案

            以下为附加 Skill 说明：
            {{skillContent}}
            """;

    @Autowired
    private AgentProperties agentProperties;

    /**
     * 缓存已加载的模板（key: 模板路径, value: 模板内容）
     */
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("✅ 智能体配置初始化完成: activeType={}, templates={}", 
                agentProperties.getActiveType(), 
                agentProperties.getTemplates().keySet());
        
        // 打印当前使用的模板路径和内容预览
        String templatePath = agentProperties.getActiveTemplatePath();
        String template = getActiveTemplate();
        log.info("📝 当前激活的模板路径: {}", templatePath);
        log.info("📝 模板内容预览 (前100字符): {}", 
                template.length() > 100 ? template.substring(0, 100) + "..." : template);
        
        // 预加载所有配置的模板
        preloadAllTemplates();
    }

    /**
     * 预加载所有配置的模板到缓存
     */
    private void preloadAllTemplates() {
        agentProperties.getTemplates().forEach((agentType, templatePath) -> {
            try {
                loadTemplateToCache(agentType, templatePath);
            } catch (Exception e) {
                log.error("❌ 预加载模板失败: agentType={}, path={}", agentType, templatePath, e);
            }
        });
    }

    /**
     * 加载单个模板到缓存
     */
    private void loadTemplateToCache(String agentType, String templatePath) {
        ClassPathResource resource = new ClassPathResource(templatePath);
        if (!resource.exists()) {
            log.warn("⚠️ 模板文件不存在: {}，将使用内置简短模板", templatePath);
            templateCache.put(agentType, FALLBACK_TEMPLATE);
            return;
        }
        
        try {
            String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            templateCache.put(agentType, template);
            log.info("✅ 已加载模板 [{}]: {}, 长度 {} 字符", agentType, templatePath, template.length());
        } catch (IOException e) {
            log.error("❌ 读取模板文件失败: {}, 使用内置简短模板", templatePath, e);
            templateCache.put(agentType, FALLBACK_TEMPLATE);
        }
    }

    /**
     * 获取当前激活智能体的模板内容
     */
    private String getActiveTemplate() {
        String activeType = agentProperties.getActiveType();
        
        // 先从缓存获取
        String cached = templateCache.get(activeType);
        if (cached != null) {
            return cached;
        }
        
        // 缓存未命中，加载模板
        String templatePath = agentProperties.getActiveTemplatePath();
        log.info("🔄 动态加载模板: agentType={}, path={}", activeType, templatePath);
        loadTemplateToCache(activeType, templatePath);
        
        // 返回加载后的模板（如果加载失败会使用 FALLBACK_TEMPLATE）
        return templateCache.getOrDefault(activeType, FALLBACK_TEMPLATE);
    }

    /**
     * 组装系统提示词（使用当前激活的智能体模板）
     *
     * @param skillContent 附加 Skill 内容
     * @param currentDate  当前日期
     * @return 完整的系统提示词
     */
    public String compose(String skillContent, String currentDate) {
        String template = getActiveTemplate();
        String skill = skillContent == null ? "" : skillContent;
        String date = currentDate == null ? "" : currentDate;
        
        return template
                .replace("{{current_date}}", date)
                .replace("{{skillContent}}", skill);
    }

    /**
     * 组装系统提示词（不指定日期）
     *
     * @param skillContent 附加 Skill 内容
     * @return 完整的系统提示词
     */
    public String compose(String skillContent) {
        return compose(skillContent, null);
    }

    /**
     * 重新加载指定智能体的模板（用于热更新）
     *
     * @param agentType 智能体类型
     */
    public void reloadTemplate(String agentType) {
        String templatePath = agentProperties.getTemplates().get(agentType);
        if (templatePath != null) {
            templateCache.remove(agentType);  // 清除缓存
            loadTemplateToCache(agentType, templatePath);  // 重新加载
            log.info("🔄 模板已重新加载: agentType={}", agentType);
        }
    }

    /**
     * 获取当前激活的智能体类型
     */
    public String getActiveAgentType() {
        return agentProperties.getActiveType();
    }
}
