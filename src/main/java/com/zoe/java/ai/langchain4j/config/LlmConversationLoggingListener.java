package com.zoe.java.ai.langchain4j.config;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
class LlmConversationLoggingListener implements ChatModelListener {

    private final LlmConversationLoggingProperties properties;

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        if (!properties.isEnabled()) {
            return;
        }
        ChatRequest req = requestContext.chatRequest();
        String modelName = req.modelName();
        log.info(
                "[LLM] >>> request | provider={} modelName={} messageCount={}",
                requestContext.modelProvider(),
                modelName != null ? modelName : "(default)",
                req.messages().size()
        );
        List<ChatMessage> messages = req.messages();
        
        // 统计工具调用结果消息数量
        long toolResultCount = messages.stream()
                .filter(m -> m instanceof ToolExecutionResultMessage)
                .count();
        if (toolResultCount > 0) {
            log.info("🔧 [MCP] 本次请求包含 {} 个工具执行结果", toolResultCount);
        }
        
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage m = messages.get(i);
            // 工具执行结果单独高亮显示
            if (m instanceof ToolExecutionResultMessage tr) {
                log.info("\n" + "=".repeat(80));
                log.info(" [MCP 工具结果] msg[{}] tool={}", i, tr.toolName());
                String resultText = "";
                try {
                    resultText = tr.text();
                } catch (Exception e) {
                    resultText = tr.contents().toString();
                }
                log.info("📄 结果内容: {}", ellipsize(resultText));
                log.info("=".repeat(80));
            } else {
                log.info("[LLM]     msg[{}] {} {}", i, m.type(), ellipsize(formatMessageBody(m)));
            }
        }
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        if (!properties.isEnabled()) {
            return;
        }
        var resp = responseContext.chatResponse();
        AiMessage ai = resp.aiMessage();
        String assistantText = ai.text();
        String thinking = ai.thinking();
        String tools = ai.hasToolExecutionRequests()
                ? ai.toolExecutionRequests().stream()
                .map(r -> r.name() + "(" + r.id() + ")")
                .collect(Collectors.joining(", "))
                : "";

        TokenUsage tu = resp.tokenUsage();
        String tokenStr = tu == null
                ? "n/a"
                : "in=%s out=%s total=%s".formatted(
                nullSafe(tu.inputTokenCount()),
                nullSafe(tu.outputTokenCount()),
                nullSafe(tu.totalTokenCount())
        );

        FinishReason fr = resp.finishReason();
        log.info(
                "[LLM] <<< response | modelName={} finishReason={} tokens=[{}]",
                resp.modelName(),
                fr != null ? fr : "n/a",
                tokenStr
        );
        if (thinking != null && !thinking.isEmpty()) {
            log.info("[LLM]     thinking: {}", ellipsize(thinking));
        }
        if (assistantText != null && !assistantText.isEmpty()) {
            log.info("[LLM]     assistant: {}", ellipsize(assistantText));
        }
        if (!tools.isEmpty()) {
            log.info("[LLM]     toolExecutionRequests: {}", tools);
        }
        
        // MCP 工具调用详细日志
        if (ai.hasToolExecutionRequests()) {
            log.info("\n" + "=".repeat(80));
            log.info("🔧 [MCP 工具调用] 检测到 {} 个工具请求", ai.toolExecutionRequests().size());
            log.info("=".repeat(80));
            for (int i = 0; i < ai.toolExecutionRequests().size(); i++) {
                var toolReq = ai.toolExecutionRequests().get(i);
                log.info("\n--- MCP 工具 #{} ---", i + 1);
                log.info("️ 工具名称: {}", toolReq.name());
                log.info("📌 工具ID: {}", toolReq.id());
                log.info("📋 工具参数: {}", toolReq.arguments());
                log.info("=".repeat(80));
            }
        }
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        if (!properties.isEnabled()) {
            return;
        }
        log.warn(
                "[LLM] !!! error | provider={}",
                errorContext.modelProvider(),
                errorContext.error()
        );
    }

    private static String nullSafe(Integer n) {
        return n == null ? "?" : String.valueOf(n);
    }

    private String ellipsize(String text) {
        if (text == null) {
            return "";
        }
        int max = Math.max(200, properties.getMaxMessageChars());
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "... [truncated " + (text.length() - max) + " chars]";
    }

    private static String formatMessageBody(ChatMessage m) {
        if (m instanceof SystemMessage sm) {
            return sm.text();
        }
        if (m instanceof UserMessage um) {
            if (um.hasSingleText()) {
                return um.singleText();
            }
            return "[user multimodal contents=" + um.contents().size() + "]";
        }
        if (m instanceof AiMessage am) {
            StringBuilder sb = new StringBuilder();
            if (am.text() != null) {
                sb.append(am.text());
            }
            if (am.hasToolExecutionRequests()) {
                if (!sb.isEmpty()) {
                    sb.append(" | ");
                }
                sb.append("tools=").append(
                        am.toolExecutionRequests().stream()
                                .map(r -> r.name() + "(" + r.id() + ")")
                                .collect(Collectors.joining(", "))
                );
            }
            return sb.isEmpty() ? am.toString() : sb.toString();
        }
        if (m instanceof ToolExecutionResultMessage tr) {
            try {
                return "tool=" + tr.toolName() + " result=" + tr.text();
            } catch (IllegalStateException e) {
                return "tool=" + tr.toolName() + " result=" + tr.contents();
            }
        }
        return m.toString();
    }
}
