package com.zoe.java.ai.langchain4j.annotation;

import com.zoe.java.ai.langchain4j.config.LlmConversationLoggingConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用基于 {@link dev.langchain4j.model.chat.listener.ChatModelListener} 的对话日志：
 * 每次发往模型的请求（含 system / user / 历史等）与完整响应（assistant、token、finishReason）会输出到控制台。
 * <p>
 * 关闭：{@code app.llm.conversation-log.enabled=false}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(LlmConversationLoggingConfig.class)
public @interface EnableLlmConversationLogging {
}
