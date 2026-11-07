-- 用户信息表 (system_user)
-- 用于存储系统用户的基本信息

CREATE TABLE IF NOT EXISTS system_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    name VARCHAR(100) COMMENT '真实姓名',
    type TINYINT DEFAULT 1 COMMENT '用户类型：0-管理员，1-普通用户',
    phone VARCHAR(20) COMMENT '手机号码',
    avatar_url VARCHAR(500) COMMENT '头像地址URL',
    additional_info TEXT COMMENT '备注信息',
    post_id BIGINT COMMENT '岗位ID',
    status TINYINT DEFAULT 1 COMMENT '账号状态：0-禁用，1-启用',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';


INSERT INTO system_user (username, password, name, type, phone, status) 
VALUES ('admin', 'e10adc3949ba59abbe56e057f20f883e', '系统管理员', 0, '13800138000', 1)
ON DUPLICATE KEY UPDATE username=username;

INSERT INTO system_user (username, password, name, type, phone, status) 
VALUES ('user', 'e10adc3949ba59abbe56e057f20f883e', '普通用户', 1, '13800138001', 1)
ON DUPLICATE KEY UPDATE username=username;