-- 插入用户信息到 system_user 表
-- 账号: user, 密码: 123456 (MD5加密)

USE llm;

-- 插入用户记录
INSERT INTO system_user (username, password, name, type, phone, status, create_time, update_time, is_deleted) 
VALUES (
    'user', 
    'e10adc3949ba59abbe56e057f20f883e',  -- MD5加密后的123456
    '普通用户', 
    1,  -- 普通用户类型
    '13800138001', 
    1,  -- 启用状态
    NOW(), 
    NOW(), 
    0   -- 未删除
)
ON DUPLICATE KEY UPDATE 
    password = VALUES(password),
    name = VALUES(name),
    update_time = NOW();

-- 查询验证插入结果
SELECT id, username, name, type, phone, status, create_time 
FROM system_user 
WHERE username = 'user';
