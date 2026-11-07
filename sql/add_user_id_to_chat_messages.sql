-- 为 chat_messages 表添加 user_id 字段
-- 执行此脚本前请备份数据库

-- 添加 user_id 字段
ALTER TABLE chat_messages 
ADD COLUMN user_id BIGINT COMMENT '用户ID' AFTER memory_id;

-- 为 user_id 字段添加索引，提升查询性能
CREATE INDEX idx_user_id ON chat_messages(user_id);

-- 如果需要为现有数据设置默认用户ID（可选）
-- UPDATE chat_messages SET user_id = 1 WHERE user_id IS NULL;
