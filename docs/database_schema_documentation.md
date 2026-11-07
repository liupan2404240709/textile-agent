-- 数据库表结构说明文档

# 纺织行业智能体项目数据库表结构

## 1. 用户信息表 (system_user)

### 表结构
| 字段名 | 类型 | 约束 | 描述 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 主键ID |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 用户名 |
| password | VARCHAR(255) | NOT NULL | 密码（加密存储） |
| name | VARCHAR(100) | - | 真实姓名 |
| type | TINYINT | DEFAULT 1 | 用户类型：0-管理员，1-普通用户 |
| phone | VARCHAR(20) | - | 手机号码 |
| avatar_url | VARCHAR(500) | - | 头像地址URL |
| additional_info | TEXT | - | 备注信息 |
| post_id | BIGINT | - | 岗位ID |
| status | TINYINT | DEFAULT 1 | 账号状态：0-禁用，1-启用 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| is_deleted | TINYINT | DEFAULT 0 | 逻辑删除：0-未删除，1-已删除 |

### 索引
- `idx_username`: 用户名索引
- `idx_phone`: 手机号索引  
- `idx_status`: 状态索引

### 默认账户
- 管理员账户: admin / admin123
- 普通用户: user / user123

## 2. 用户聊天历史表 (chat_messages)

### 表结构
| 字段名 | 类型 | 约束 | 描述 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 主键ID |
| memory_id | VARCHAR(100) | NOT NULL | 会话记忆ID，标识一次完整的对话会话 |
| user_id | BIGINT | NOT NULL, FOREIGN KEY | 用户ID，关联system_user表 |
| content | TEXT | NOT NULL | 消息内容（JSON格式，包含角色、内容等信息） |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

### 索引
- `idx_memory_id`: 会话ID索引
- `idx_user_id`: 用户ID索引
- `idx_create_time`: 创建时间索引

### 外键约束
- `user_id` 关联 `system_user(id)`，级联删除

## 3. 纺织设计域业务表

纺织行业智能体的业务数据由以下四张表承载，字段级说明见 `create_textile_design_tables.sql`：

| 表名 | 说明 | 关键字段 |
|---|---|---|
| `textile_designer_profile` | 纺织设计师档案 | designer_no(`TEX001`)、specialty（面料/服装/家纺/花型/印染等）、experience_years、skill_tags |
| `textile_design_project` | 纺织设计项目 | designer_id、project_name、stage（概念提案/面料开发/花型设计/印染打样/大货生产）、acceptance_criteria |
| `textile_design_review` | 纺织设计评审 | designer_id、project_id、overall_score、innovation_score、usability_score、processability_score |
| `textile_review_appointment` | 纺织设计评审会议预约 | username、project_name、date、time、reviewer_name |

## 使用说明

1. **执行顺序**: 先执行 `create_user_tables.sql`、`create_chat_history_table.sql`，再执行 `create_textile_design_tables.sql`
2. **字符集**: 使用 utf8mb4 支持中文和特殊字符
3. **引擎**: 使用 InnoDB 支持事务和外键
4. **安全提示**: 默认账户密码仅为测试用，生产环境请修改为强密码

## 对应的 Java 实体类

- `SystemUser.java` - 对应用户信息表
- `ChatMessageEntity.java` - 对应聊天历史表
- `BaseEntity.java` - 基础实体类，包含通用字段
- `TextileDesignerProfile.java` - 对应纺织设计师档案表 `textile_designer_profile`
- `TextileDesignProject.java` - 对应纺织设计项目表 `textile_design_project`
- `TextileDesignReview.java` - 对应纺织设计评审表 `textile_design_review`
- `TextileReviewAppointment.java` - 对应纺织设计评审会议预约表 `textile_review_appointment`

> 上述纺织设计域表结构详见 `create_textile_design_tables.sql`，测试数据详见 `insert_textile_designer_test_data.sql`。
> 从旧版本（工业设计域）升级请先执行 `migrate_to_textile_design.sql`。
