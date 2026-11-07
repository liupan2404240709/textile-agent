-- 创建 llm 数据库
CREATE DATABASE IF NOT EXISTS llm 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;

-- 使用 llm 数据库
USE llm;

-- 显示创建成功的消息
SELECT 'Database llm created successfully!' AS message;
