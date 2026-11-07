package com.zoe.java.ai.langchain4j.store;

import com.zoe.java.ai.langchain4j.entity.LoginUser;
import com.zoe.java.ai.langchain4j.entity.LoginUserHolder;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

@Component
public class MySqlChatMemoryStore implements ChatMemoryStore {

    private static final Logger log = LoggerFactory.getLogger(MySqlChatMemoryStore.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        // 获取当前用户ID，确保只能读取自己的消息
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        Long userId = loginUser != null ? loginUser.getUserId() : null;
        
        String sql;
        Object[] params;
        
        if (userId != null) {
            sql = "SELECT content FROM chat_messages WHERE memory_id = ? AND user_id = ? ORDER BY create_time ASC";
            params = new Object[]{memoryId.toString(), userId};
        } else {
            sql = "SELECT content FROM chat_messages WHERE memory_id = ? ORDER BY create_time ASC";
            params = new Object[]{memoryId.toString()};
        }
        
        try {
            List<String> contents = jdbcTemplate.queryForList(sql, String.class, params);
            if (contents == null || contents.isEmpty()) {
                log.debug("未找到 memoryId={} 的记录", memoryId);
                return new LinkedList<>();
            }
            
            // 合并多条记录的内容（取最新的一条或合并所有）
            String mergedContent = contents.get(contents.size() - 1); // 取最新的记录
            if (contents.size() > 1) {
                log.warn("发现 memoryId={} 存在 {} 条重复记录，取最新一条", memoryId, contents.size());
            }
            
            log.debug("成功读取 memoryId={} 的消息，共 {} 字符", memoryId, mergedContent.length());
            return ChatMessageDeserializer.messagesFromJson(mergedContent);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // 新对话首次查询，数据库没有记录，这是正常的
            log.debug("首次对话，memoryId={} 没有历史记录", memoryId);
            return new LinkedList<>();
        } catch (Exception e) {
            log.error("读取 memoryId={} 的消息失败", memoryId, e);
            return new LinkedList<>();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String memoryIdStr = memoryId.toString();
        String content = ChatMessageSerializer.messagesToJson(messages);
        LocalDateTime now = LocalDateTime.now();

        // 获取当前用户ID
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        Long userId = loginUser != null ? loginUser.getUserId() : null;
        
        if (userId == null) {
            log.warn("更新消息时未获取到用户ID, memoryId={}, 尝试从数据库现有记录中获取", memoryId);
            // 尝试从现有记录中找回 userId，防止上下文丢失导致的数据污染
            try {
                String getUserIdSql = "SELECT user_id FROM chat_messages WHERE memory_id = ? LIMIT 1";
                userId = jdbcTemplate.queryForObject(getUserIdSql, Long.class, memoryIdStr);
                if (userId != null) {
                    log.info("成功从数据库恢复 userId={} for memoryId={}", userId, memoryId);
                }
            } catch (Exception e) {
                log.debug("未在数据库找到 memoryId={} 的现有记录，将使用 NULL user_id", memoryId);
            }
        }

        try {
            if (userId != null) {
                // 优先使用带用户ID的逻辑
                String checkSql = "SELECT COUNT(*) FROM chat_messages WHERE memory_id = ? AND user_id = ?";
                Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, memoryIdStr, userId);

                if (count != null && count > 0) {
                    String updateSql = "UPDATE chat_messages SET content = ?, update_time = ? WHERE memory_id = ? AND user_id = ?";
                    int rows = jdbcTemplate.update(updateSql, content, now, memoryIdStr, userId);
                    log.info("更新 memoryId={} 的消息，userId={}, 影响行数={}, 消息数量={}",
                            memoryId, userId, rows, messages.size());
                } else {
                    String insertSql = "INSERT INTO chat_messages (memory_id, user_id, content, create_time, update_time) VALUES (?, ?, ?, ?, ?)";
                    jdbcTemplate.update(insertSql, memoryIdStr, userId, content, now, now);
                    log.info("插入新的 memoryId={} 的消息，userId={}, 消息数量={}",
                            memoryId, userId, messages.size());
                }
            } else {
                // 兜底逻辑：如果没有 userId，确保只处理一条记录，避免批量更新多条脏数据
                log.warn("最终仍无 userId，使用兼容模式处理 memoryId={}", memoryId);
                String checkSql = "SELECT COUNT(*) FROM chat_messages WHERE memory_id = ?";
                Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, memoryIdStr);

                if (count != null && count > 0) {
                    // 限制只更新最早或最晚的一条，防止同时更新多条重复记录
                    String updateSql = "UPDATE chat_messages SET content = ?, update_time = ? WHERE memory_id = ? AND id = (SELECT id FROM (SELECT MIN(id) as id FROM chat_messages WHERE memory_id = ?) as tmp)";
                    int rows = jdbcTemplate.update(updateSql, content, now, memoryIdStr, memoryIdStr);
                    log.info("兼容模式更新 memoryId={}，影响行数={}, 消息数量={}",
                            memoryId, rows, messages.size());
                } else {
                    String insertSql = "INSERT INTO chat_messages (memory_id, user_id, content, create_time, update_time) VALUES (?, ?, ?, ?, ?)";
                    jdbcTemplate.update(insertSql, memoryIdStr, userId, content, now, now);
                    log.info("兼容模式插入 memoryId={}，消息数量={}",
                            memoryId, messages.size());
                }
            }
        } catch (Exception e) {
            log.error("保存 memoryId={} 的消息失败", memoryId, e);
            throw new RuntimeException("保存聊天消息失败", e);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        // 获取当前用户ID，确保只能删除自己的消息
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        Long userId = loginUser != null ? loginUser.getUserId() : null;
        
        String sql;
        Object[] params;
        
        if (userId != null) {
            sql = "DELETE FROM chat_messages WHERE memory_id = ? AND user_id = ?";
            params = new Object[]{memoryId.toString(), userId};
        } else {
            sql = "DELETE FROM chat_messages WHERE memory_id = ?";
            params = new Object[]{memoryId.toString()};
        }
        
        try {
            jdbcTemplate.update(sql, params);
            log.info("删除 memoryId={} 的消息{}", memoryId, userId != null ? ", userId=" + userId : "");
        } catch (Exception e) {
            log.error("删除 memoryId={} 的消息失败", memoryId, e);
        }
    }
}
