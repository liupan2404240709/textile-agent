-- 用户聊天历史表 (chat_messages)
-- 用于存储用户的聊天对话历史记录

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    memory_id VARCHAR(100) NOT NULL COMMENT '会话记忆ID，标识一次完整的对话会话',
    user_id BIGINT NOT NULL COMMENT '用户ID，关联system_user表',
    content TEXT NOT NULL COMMENT '消息内容（JSON格式，包含角色、内容等信息）',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    
    INDEX idx_memory_id (memory_id),
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time),
    FOREIGN KEY (user_id) REFERENCES system_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户聊天历史表';

-- 可选：添加一些示例数据用于测试
-- INSERT INTO chat_messages (memory_id, user_id, content) VALUES 
-- ('session_001', 1, '{"role": "user", "content": "你好，我想咨询一下健康问题"}'),
-- ('session_001', 1, '{"role": "assistant", "content": "您好！我是医疗助手，请问您有什么健康问题需要咨询？"}');