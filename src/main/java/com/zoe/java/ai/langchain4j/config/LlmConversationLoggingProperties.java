package com.zoe.java.ai.langchain4j.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.llm.conversation-log")
public class LlmConversationLoggingProperties {

    /**
     * 是否注册 {@link dev.langchain4j.model.chat.listener.ChatModelListener}。
     */
    private boolean enabled = true;

    /**
     * 单条消息写入日志的最大字符数，超出部分省略（避免超大 system 刷满控制台）。
     */
    private int maxMessageChars = 20000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxMessageChars() {
        return maxMessageChars;
    }

    public void setMaxMessageChars(int maxMessageChars) {
        this.maxMessageChars = maxMessageChars;
    }
}
