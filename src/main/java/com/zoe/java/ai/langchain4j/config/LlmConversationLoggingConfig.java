package com.zoe.java.ai.langchain4j.config;

import dev.langchain4j.model.chat.listener.ChatModelListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * 注册 {@link ChatModelListener}，由 LangChain4j Spring Boot 自动挂到 {@code ChatModel} /
 * {@code StreamingChatModel}（含百炼 DashScope 自动配置的 bean）。
 */
@Configuration
@EnableConfigurationProperties(LlmConversationLoggingProperties.class)
@ConditionalOnProperty(prefix = "app.llm.conversation-log", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LlmConversationLoggingConfig {

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    ChatModelListener llmConversationLoggingListener(LlmConversationLoggingProperties properties) {
        return new LlmConversationLoggingListener(properties);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    ChatModelListener mcpToolExecutionLogger() {
        return new McpToolExecutionLogger();
    }
}
