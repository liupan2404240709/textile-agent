package com.zoe.java.ai.langchain4j.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoe.java.ai.langchain4j.vo.ChatHistoryVo;
import com.zoe.java.ai.langchain4j.entity.ChatMessageEntity;
import com.zoe.java.ai.langchain4j.entity.LoginUser;
import com.zoe.java.ai.langchain4j.entity.LoginUserHolder;
import com.zoe.java.ai.langchain4j.mapper.ChatMessageMapper;
import com.zoe.java.ai.langchain4j.service.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatMessageServiceImpl implements ChatMessageService {
    private static final int HISTORY_TITLE_MAX_LENGTH = 26;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<ChatHistoryVo> getHistoryList() {
        // 获取当前用户ID
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        Long userId = loginUser != null ? loginUser.getUserId() : null;
        
        if (userId == null) {
            return List.of();
        }
        
        List<ChatMessageEntity> entities = chatMessageMapper.selectHistoryByUserId(userId);

        return entities.stream().map(entity -> {
            String memoryId = entity.getMemoryId();
            String content = entity.getContent();
            LocalDateTime updateTime = entity.getUpdateTime();

            String firstMessage = "新对话";
            try {
                JsonNode messages = objectMapper.readTree(content);
                String title = buildHistoryTitle(messages);
                if (title != null && !title.isBlank()) {
                    firstMessage = title;
                }
            } catch (Exception e) {
            }

            return new ChatHistoryVo(memoryId, firstMessage, updateTime);
        }).collect(Collectors.toList());
    }

    private String normalizeRole(JsonNode message) {
        String rawRole = "";
        if (message.hasNonNull("type")) {
            rawRole = message.get("type").asText("");
        } else if (message.hasNonNull("role")) {
            rawRole = message.get("role").asText("");
        } else if (message.hasNonNull("messageType")) {
            rawRole = message.get("messageType").asText("");
        } else if (message.hasNonNull("kind")) {
            rawRole = message.get("kind").asText("");
        }
        rawRole = rawRole.toUpperCase();

        if (rawRole.contains("USER") || rawRole.contains("HUMAN")) {
            return "USER";
        }
        if (rawRole.contains("SYSTEM")) {
            return "SYSTEM";
        }
        if (rawRole.contains("AI") || rawRole.contains("ASSISTANT")) {
            return "ASSISTANT";
        }
        return "UNKNOWN";
    }

    private String extractText(JsonNode message) {
        if (message.hasNonNull("singleText")) {
            return message.get("singleText").asText("");
        }
        if (message.hasNonNull("text")) {
            return message.get("text").asText("");
        }

        if (message.hasNonNull("contents") && message.get("contents").isArray()) {
            return extractTextFromArray(message.get("contents"));
        }

        JsonNode contentNode = message.get("content");
        if (contentNode == null || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }
        if (contentNode.isArray()) {
            return extractTextFromArray(contentNode);
        }
        return "";
    }

    private String extractTextFromArray(JsonNode arrayNode) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode item : arrayNode) {
            String text = "";
            if (item.isTextual()) {
                text = item.asText("");
            } else if (item.hasNonNull("text")) {
                text = item.get("text").asText("");
            } else if (item.hasNonNull("content")) {
                text = item.get("content").asText("");
            } else if (item.hasNonNull("value")) {
                text = item.get("value").asText("");
            }
            if (!text.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append('\n');
                }
                sb.append(text);
            }
        }
        return sb.toString();
    }

    private String buildHistoryTitle(JsonNode messages) {
        if (messages == null || !messages.isArray() || messages.isEmpty()) {
            return "新对话";
        }

        String fallbackAssistantText = "";
        for (JsonNode message : messages) {
            String role = normalizeRole(message);
            String text = cleanHistoryText(extractText(message));
            if (text.isBlank()) {
                continue;
            }
            if ("USER".equals(role)) {
                return shortenForTitle(text);
            }
            if ("ASSISTANT".equals(role) && fallbackAssistantText.isBlank()) {
                fallbackAssistantText = text;
            }
        }
        if (!fallbackAssistantText.isBlank()) {
            return shortenForTitle(fallbackAssistantText);
        }
        return "新对话";
    }

    private String cleanHistoryText(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.trim();
        if (cleaned.isBlank()) {
            return "";
        }

        String[] ragMarkers = new String[]{
                "Answer using the following information:",
                "Use the following information to answer:",
                "请基于以下信息回答",
                "请根据以下信息回答"
        };

        int cutIndex = -1;
        for (String marker : ragMarkers) {
            int idx = indexOfIgnoreCase(cleaned, marker);
            if (idx >= 0 && (cutIndex == -1 || idx < cutIndex)) {
                cutIndex = idx;
            }
        }
        if (cutIndex > 0) {
            cleaned = cleaned.substring(0, cutIndex).trim();
        }
        return cleaned;
    }

    private int indexOfIgnoreCase(String source, String target) {
        return source.toLowerCase().indexOf(target.toLowerCase());
    }

    private String shortenForTitle(String text) {
        String compact = text.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        if (compact.length() <= HISTORY_TITLE_MAX_LENGTH) {
            return compact;
        }
        return compact.substring(0, HISTORY_TITLE_MAX_LENGTH) + "...";
    }

    @Override
    public void deleteHistory(String memoryId) {
        // 获取当前用户ID，确保只能删除自己的历史记录
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        Long userId = loginUser != null ? loginUser.getUserId() : null;
        
        if (userId == null) {
            return;
        }
        
        LambdaQueryWrapper<ChatMessageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessageEntity::getMemoryId, memoryId)
               .eq(ChatMessageEntity::getUserId, userId);
        chatMessageMapper.delete(wrapper);
    }

    @Override
    public String getChatContent(String memoryId) {
        // 获取当前用户ID，确保只能查看自己的历史内容
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        Long userId = loginUser != null ? loginUser.getUserId() : null;
        
        if (userId == null) {
            return null;
        }
        
        LambdaQueryWrapper<ChatMessageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessageEntity::getMemoryId, memoryId)
               .eq(ChatMessageEntity::getUserId, userId);
        ChatMessageEntity entity = chatMessageMapper.selectOne(wrapper);

        if (entity == null) {
            return null;
        }
        return entity.getContent();
    }
}
