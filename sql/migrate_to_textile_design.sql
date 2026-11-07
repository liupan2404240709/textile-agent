-- ============================================================
-- 存量库迁移脚本：工业设计域 → 纺织行业域
-- 适用对象：已经执行过工业设计域建表脚本（designer_profile /
--           design_project / design_review /
--           design_review_appointment）的存量数据库
--
-- 【重要】执行前务必先备份数据库！
--   mysqldump -u root -p llm > llm_backup_$(date +%Y%m%d).sql
--
-- 全新部署无需执行本脚本，直接执行 create_textile_design_tables.sql 即可。
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. 设计师档案表 → 纺织设计师档案表
-- ------------------------------------------------------------
RENAME TABLE designer_profile TO textile_designer_profile;

ALTER TABLE textile_designer_profile
    CHANGE COLUMN designer_no designer_no VARCHAR(50) NOT NULL COMMENT '纺织设计师编号，例如 TEX001',
    CHANGE COLUMN specialty specialty VARCHAR(100) COMMENT '专业方向：面料设计、服装设计、家纺设计、花型设计、印染工艺、针织工艺、纺织材料、产业用纺织品等',
    CHANGE COLUMN experience_years experience_years INT DEFAULT 0 COMMENT '从业年限',
    CHANGE COLUMN work_experience work_experience TEXT COMMENT '工作经历',
    CHANGE COLUMN achievements achievements TEXT COMMENT '设计成果与获奖情况',
    CHANGE COLUMN total_projects total_projects INT DEFAULT 0 COMMENT '项目总数',
    CHANGE COLUMN total_awards total_awards INT DEFAULT 0 COMMENT '获奖数量';

-- 旧索引改名以匹配新字段语义（保留索引结构）
ALTER TABLE textile_designer_profile DROP INDEX idx_specialty;
ALTER TABLE textile_designer_profile ADD INDEX idx_specialty (specialty);

-- 编号前缀迁移：DES001 → TEX001（保证与 TextileDesignQueryTools 的 TEX\d+ 匹配规则一致）
UPDATE textile_designer_profile
SET designer_no = CONCAT('TEX', SUBSTRING(designer_no, 4))
WHERE designer_no LIKE 'DES%';

-- ------------------------------------------------------------
-- 2. 设计项目表 → 纺织设计项目表
-- ------------------------------------------------------------
RENAME TABLE design_project TO textile_design_project;

ALTER TABLE textile_design_project
    CHANGE COLUMN designer_id designer_id BIGINT NOT NULL COMMENT '纺织设计师ID，关联textile_designer_profile表',
    CHANGE COLUMN project_name project_name VARCHAR(200) NOT NULL COMMENT '项目名称',
    CHANGE COLUMN project_code project_code VARCHAR(50) COMMENT '项目编号',
    CHANGE COLUMN stage stage VARCHAR(50) COMMENT '项目阶段：概念提案、面料开发、花型设计、印染打样、大货生产',
    CHANGE COLUMN design_cycle design_cycle INT COMMENT '设计周期（天）',
    CHANGE COLUMN milestone_count milestone_count INT COMMENT '里程碑数量',
    CHANGE COLUMN design_objectives design_objectives TEXT COMMENT '设计目标',
    CHANGE COLUMN design_content design_content TEXT COMMENT '设计内容大纲',
    CHANGE COLUMN design_methods design_methods VARCHAR(200) COMMENT '设计方法与工具',
    CHANGE COLUMN acceptance_criteria acceptance_criteria VARCHAR(200) COMMENT '验收标准（克重、色牢度、缩水率、缸差ΔE等）',
    CHANGE COLUMN project_status project_status TINYINT DEFAULT 0 COMMENT '项目状态：0-草稿，1-待评审，2-已通过，3-已驳回，4-进行中，5-已完成',
    CHANGE COLUMN review_comment review_comment TEXT COMMENT '评审意见',
    CHANGE COLUMN reviewer_id reviewer_id BIGINT COMMENT '评审人ID',
    CHANGE COLUMN review_time review_time DATETIME COMMENT '评审时间',
    CHANGE COLUMN design_effect design_effect VARCHAR(100) COMMENT '设计成效评估';

ALTER TABLE textile_design_project DROP INDEX idx_stage;
ALTER TABLE textile_design_project ADD INDEX idx_stage (stage);

-- ------------------------------------------------------------
-- 3. 设计评审表 → 纺织设计评审表
-- ------------------------------------------------------------
RENAME TABLE design_review TO textile_design_review;

ALTER TABLE textile_design_review
    CHANGE COLUMN designer_id designer_id BIGINT NOT NULL COMMENT '纺织设计师ID，关联textile_designer_profile表',
    CHANGE COLUMN reviewer_id reviewer_id BIGINT NOT NULL COMMENT '评审人ID，关联system_user表',
    CHANGE COLUMN project_id project_id BIGINT COMMENT '项目ID，关联textile_design_project表',
    CHANGE COLUMN project_name project_name VARCHAR(200) COMMENT '项目名称',
    CHANGE COLUMN innovation_score innovation_score DECIMAL(3,1) COMMENT '创新性评分',
    CHANGE COLUMN usability_score usability_score DECIMAL(3,1) COMMENT '实用性评分',
    CHANGE COLUMN manufacturability_score processability_score DECIMAL(3,1) COMMENT '工艺可行性（试纺/试织/印染打样可行性）评分',
    CHANGE COLUMN review_content review_content TEXT COMMENT '评审内容',
    CHANGE COLUMN review_status review_status TINYINT DEFAULT 1 COMMENT '评审状态：0-待评审，1-已通过，2-已驳回',
    CHANGE COLUMN reply_content reply_content TEXT COMMENT '设计师回复内容';

-- ------------------------------------------------------------
-- 4. 设计评审会议预约表 → 纺织设计评审会议预约表
-- ------------------------------------------------------------
RENAME TABLE design_review_appointment TO textile_review_appointment;

ALTER TABLE textile_review_appointment
    CHANGE COLUMN project_name project_name VARCHAR(200) COMMENT '评审项目名称',
    CHANGE COLUMN reviewer_name reviewer_name VARCHAR(100) COMMENT '评审专家姓名';

-- ============================================================
-- 回滚说明
-- 若需要回滚，按相反顺序执行 RENAME TABLE + CHANGE COLUMN 即可，
-- 例如：RENAME TABLE textile_designer_profile TO designer_profile;
--       ALTER TABLE designer_profile CHANGE COLUMN designer_no designer_no VARCHAR(50) NOT NULL COMMENT '设计师编号';
-- 建议优先使用执行前备份的 dump 文件恢复，避免字段语义被二次改写。
-- ============================================================
