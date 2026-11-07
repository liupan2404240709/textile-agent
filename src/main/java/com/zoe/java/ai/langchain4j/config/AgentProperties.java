package com.zoe.java.ai.langchain4j.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 智能体配置属性
 * 通过修改 application.yml 中的 agent.active-type 即可切换智能体角色
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    /**
     * 当前激活的智能体类型（如：papa, requirements）
     * YAML 中使用 active-type，Spring Boot 会自动转换为 activeType
     */
    private String activeType = "papa";

    /**
     * 各智能体的提示词模板路径映射
     * key: 智能体类型代码
     * value: 模板文件路径（classpath）
     */
    private Map<String, String> templates = new HashMap<>();

    /**
     * 获取当前激活智能体的模板路径
     */
    public String getActiveTemplatePath() {
        String path = templates.getOrDefault(activeType, "papa-prompt-template.txt");
        // 调试日志：打印当前使用的模板
        System.out.println("[DEBUG] AgentProperties.getActiveTemplatePath() -> activeType=" + activeType + ", path=" + path);
        return path;
    }

    /**
     * 检查智能体类型是否存在
     */
    public boolean hasTemplate(String agentType) {
        return templates.containsKey(agentType);
    }
}
