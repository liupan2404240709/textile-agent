package com.zoe.java.ai.langchain4j.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 纺织设计师测试数据初始化器
 * 用于在应用启动时插入正确的UTF-8编码测试数据
 */
@Component
public class TextileDesignerDataInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        // 检查是否已有数据
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM textile_designer_profile", Integer.class);

        if (count != null && count > 0) {
            System.out.println("纺织设计师数据已存在，跳过初始化");
            return;
        }

        System.out.println("开始插入纺织设计师测试数据...");

        // 插入纺织设计师档案
        insertDesignerProfiles();

        // 插入纺织设计项目
        insertDesignProjects();

        // 插入纺织设计评审
        insertDesignReviews();

        System.out.println("纺织设计师测试数据初始化完成！");
    }

    private void insertDesignerProfiles() {
        String sql = "INSERT INTO textile_designer_profile (user_id, designer_no, name, gender, birth_date, phone, email, education, major, certificate, specialty, experience_years, introduction, work_experience, achievements, skill_tags, rating, good_rate, total_projects, total_awards, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // 张伟 - 面料设计师
        jdbcTemplate.update(sql, 1001, "TEX001", "张伟", 1, "1985-03-15",
            "13800138001", "zhangwei@example.com", "硕士", "纺织材料与纺织品设计",
            "高级纺织面料设计师职业资格证,中国国际面料设计大赛获奖者", "面料设计", 12,
            "资深面料设计师，专注功能性与生态面料开发 12 年，主导多款量产面料的成分设计与织造工艺落地。",
            "2014-2018年 在某纺织研究院担任主任面料设计师\n2018-至今 在本机构担任面料设计总监",
            "2020年获得中国国际面料设计大赛金奖\n2022年主导的凉感功能面料实现量产",
            "面料设计,织造工艺,功能整理,纱线选型,试纺打样", 4.8, 96.50, 25, 6, 1);

        // 李芳 - 家纺设计师
        jdbcTemplate.update(sql, 1002, "TEX002", "李芳", 0, "1990-07-22",
            "13800138002", "lifang@example.com", "本科", "家用纺织品设计",
            "中级家用纺织品设计师职业资格证,中国国际家用纺织品设计大赛获奖者", "家纺设计", 8,
            "家纺设计师，擅长床品与窗帘的图案配套设计与材质工艺（印染/提花/绣花）落地，熟悉棉麻、天丝与遮光涂层工艺。",
            "2016-2019年 在某家纺品牌担任产品设计师\n2019-至今 在本机构担任资深设计师",
            "2021年设计的模块化床品套件获中国国际家用纺织品设计大赛金奖\n2023年主导的适老化家纺系列实现量产",
            "家纺设计,图案配套,提花工艺,遮光涂层,材质工艺", 4.9, 98.20, 30, 8, 1);

        System.out.println("已插入纺织设计师档案数据");
    }

    private void insertDesignProjects() {
        String sql = "INSERT INTO textile_design_project (designer_id, project_name, project_code, stage, design_cycle, milestone_count, start_date, end_date, design_objectives, design_content, design_methods, acceptance_criteria, schedule_details, project_status, review_comment, actual_progress, design_effect) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // 张伟的设计项目
        jdbcTemplate.update(sql, 1, "凉感功能面料开发", "TEX-FABRIC-001", "面料开发",
            60, 5, "2024-03-01", "2024-04-30",
            "开发适用于夏季服装的凉感功能面料，满足接触凉感与耐洗性要求",
            "纤维选型（凉感涤纶/锦纶）与纱线结构设计\n织物组织设计（平纹/提花）与密度优化\n功能整理（凉感助剂/吸湿速干）工艺验证",
            "纱线选型,小样试织,功能整理,物性测试",
            "接触凉感系数 Q-max 不低于 0.15；20 次水洗后凉感保持率不低于 80%；面料克重 150g/m²±5%",
            "{\"week1-2\": \"纤维选型与纱线设计\", \"week3-5\": \"小样试织与组织优化\", \"week6-8\": \"功能整理与大货验证\"}",
            4, "凉感性能达标，耐洗稳定性需进一步验证", 75.00, "良好");

        System.out.println("已插入纺织设计项目数据");
    }

    private void insertDesignReviews() {
        String sql = "INSERT INTO textile_design_review (designer_id, reviewer_id, project_id, project_name, overall_score, innovation_score, usability_score, professional_score, interaction_score, processability_score, review_content, sentiment_type, sentiment_score, is_anonymous) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // 张伟的项目评审
        jdbcTemplate.update(sql, 1, 2001, 1, "凉感功能面料开发",
            5.0, 5.0, 5.0, 5.0, 5.0, 5.0,
            "纤维选型与凉感助剂方案搭配合理，试纺数据充分，物性指标全部达标，评审一次通过。",
            "positive", 0.95, 0);

        jdbcTemplate.update(sql, 1, 2002, 1, "凉感功能面料开发",
            4.5, 5.0, 4.5, 5.0, 4.0, 4.5,
            "创新性突出，但20次水洗后的凉感保持率建议补充更严格的验证。",
            "positive", 0.82, 0);

        System.out.println("已插入纺织设计评审数据");
    }
}
