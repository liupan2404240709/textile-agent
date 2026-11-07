-- 纺织行业领域相关表
-- 用于纺织行业智能体系统的纺织设计师、纺织设计项目与纺织设计评审管理功能

-- 1. 纺织设计师档案表 (textile_designer_profile)
CREATE TABLE IF NOT EXISTS textile_designer_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '关联系统用户ID',
    designer_no VARCHAR(50) NOT NULL UNIQUE COMMENT '纺织设计师编号，例如 TEX001',
    name VARCHAR(100) NOT NULL COMMENT '纺织设计师姓名',
    gender TINYINT COMMENT '性别：0-女，1-男',
    birth_date DATE COMMENT '出生日期',
    id_card VARCHAR(18) COMMENT '身份证号',
    phone VARCHAR(20) COMMENT '联系电话',
    email VARCHAR(100) COMMENT '邮箱',
    education VARCHAR(50) COMMENT '学历：大专，本科，硕士，博士',
    major VARCHAR(100) COMMENT '所学专业：纺织工程、服装设计与工程、纺织材料与纺织品设计、染整工程等',
    certificate VARCHAR(200) COMMENT '资质证书（多个证书用逗号分隔）',
    specialty VARCHAR(100) COMMENT '专业方向：面料设计、服装设计、家纺设计、花型设计、印染工艺、针织工艺、纺织材料、产业用纺织品等',
    experience_years INT DEFAULT 0 COMMENT '从业年限',
    introduction TEXT COMMENT '个人简介',
    work_experience TEXT COMMENT '工作经历',
    achievements TEXT COMMENT '设计成果与获奖情况',
    skill_tags VARCHAR(500) COMMENT '技能标签（逗号分隔，用于知识图谱）',
    rating DECIMAL(3,1) DEFAULT 0.0 COMMENT '综合评分（满分5分）',
    good_rate DECIMAL(5,2) DEFAULT 0.0 COMMENT '好评率（百分比）',
    total_projects INT DEFAULT 0 COMMENT '项目总数',
    total_awards INT DEFAULT 0 COMMENT '获奖数量',
    status TINYINT DEFAULT 1 COMMENT '状态：0-离职，1-在职，2-休假',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',

    INDEX idx_user_id (user_id),
    INDEX idx_designer_no (designer_no),
    INDEX idx_name (name),
    INDEX idx_specialty (specialty),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='纺织设计师档案表';

-- 2. 纺织设计项目表 (textile_design_project)
CREATE TABLE IF NOT EXISTS textile_design_project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    designer_id BIGINT NOT NULL COMMENT '纺织设计师ID，关联textile_designer_profile表',
    project_name VARCHAR(200) NOT NULL COMMENT '项目名称',
    project_code VARCHAR(50) COMMENT '项目编号',
    stage VARCHAR(50) COMMENT '项目阶段：概念提案、面料开发、花型设计、印染打样、大货生产',
    design_cycle INT COMMENT '设计周期（天）',
    milestone_count INT COMMENT '里程碑数量',
    start_date DATE COMMENT '开始日期',
    end_date DATE COMMENT '结束日期',
    design_objectives TEXT COMMENT '设计目标',
    design_content TEXT COMMENT '设计内容大纲',
    design_methods VARCHAR(200) COMMENT '设计方法与工具',
    acceptance_criteria VARCHAR(200) COMMENT '验收标准（克重、色牢度、缩水率、缸差ΔE等）',
    schedule_details TEXT COMMENT '详细进度安排（JSON格式）',
    project_status TINYINT DEFAULT 0 COMMENT '项目状态：0-草稿，1-待评审，2-已通过，3-已驳回，4-进行中，5-已完成',
    review_comment TEXT COMMENT '评审意见',
    reviewer_id BIGINT COMMENT '评审人ID',
    review_time DATETIME COMMENT '评审时间',
    reminder_enabled TINYINT DEFAULT 1 COMMENT '是否启用提醒：0-否，1-是',
    last_reminder_time DATETIME COMMENT '最后提醒时间',
    actual_progress DECIMAL(5,2) DEFAULT 0.0 COMMENT '实际进度（百分比）',
    design_effect VARCHAR(100) COMMENT '设计成效评估',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',

    INDEX idx_designer_id (designer_id),
    INDEX idx_project_code (project_code),
    INDEX idx_project_status (project_status),
    INDEX idx_start_date (start_date),
    INDEX idx_stage (stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='纺织设计项目表';

-- 3. 纺织设计评审表 (textile_design_review)
CREATE TABLE IF NOT EXISTS textile_design_review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    designer_id BIGINT NOT NULL COMMENT '纺织设计师ID，关联textile_designer_profile表',
    reviewer_id BIGINT NOT NULL COMMENT '评审人ID，关联system_user表',
    project_id BIGINT COMMENT '项目ID，关联textile_design_project表',
    project_name VARCHAR(200) COMMENT '项目名称',
    overall_score DECIMAL(3,1) NOT NULL COMMENT '总体评分（1-5分）',
    innovation_score DECIMAL(3,1) COMMENT '创新性评分',
    usability_score DECIMAL(3,1) COMMENT '实用性评分',
    professional_score DECIMAL(3,1) COMMENT '专业水平评分',
    interaction_score DECIMAL(3,1) COMMENT '协作沟通评分',
    processability_score DECIMAL(3,1) COMMENT '工艺可行性（试纺/试织/印染打样可行性）评分',
    review_content TEXT COMMENT '评审内容',
    sentiment_type VARCHAR(20) COMMENT '情感类型：positive-正面，neutral-中性，negative-负面',
    sentiment_score DECIMAL(5,2) COMMENT '情感分析得分（-1到1）',
    is_anonymous TINYINT DEFAULT 0 COMMENT '是否匿名：0-否，1-是',
    review_status TINYINT DEFAULT 1 COMMENT '评审状态：0-待评审，1-已通过，2-已驳回',
    reply_content TEXT COMMENT '设计师回复内容',
    reply_time DATETIME COMMENT '回复时间',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',

    INDEX idx_designer_id (designer_id),
    INDEX idx_reviewer_id (reviewer_id),
    INDEX idx_project_id (project_id),
    INDEX idx_overall_score (overall_score),
    INDEX idx_sentiment_type (sentiment_type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='纺织设计评审表';

-- 4. 纺织设计评审会议预约表 (textile_review_appointment)
CREATE TABLE IF NOT EXISTS textile_review_appointment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(100) COMMENT '预约人姓名',
    id_card VARCHAR(30) COMMENT '预约人身份证号',
    project_name VARCHAR(200) COMMENT '评审项目名称',
    date VARCHAR(20) COMMENT '评审日期',
    time VARCHAR(20) COMMENT '评审时间段，例如：上午、下午',
    reviewer_name VARCHAR(100) COMMENT '评审专家姓名',

    INDEX idx_username (username),
    INDEX idx_project_name (project_name),
    INDEX idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='纺织设计评审会议预约表';
