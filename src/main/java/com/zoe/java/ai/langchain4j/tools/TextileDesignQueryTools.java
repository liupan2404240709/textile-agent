package com.zoe.java.ai.langchain4j.tools;

import com.zoe.java.ai.langchain4j.entity.TextileDesignProject;
import com.zoe.java.ai.langchain4j.entity.TextileDesignReview;
import com.zoe.java.ai.langchain4j.entity.TextileDesignerProfile;
import com.zoe.java.ai.langchain4j.service.TextileDesignerService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 纺织设计师与纺织设计项目信息查询工具
 * 为AI智能体提供查询纺织设计师档案、纺织设计项目、纺织设计评审等功能
 */
@Slf4j
@Component
public class TextileDesignQueryTools {

    @Autowired
    private TextileDesignerService textileDesignerService;

    /**
     * 查询纺织设计师基本信息
     */
    @Tool(name = "查询纺织设计师档案", value = "根据纺织设计师姓名或编号查询设计师的详细信息，包括基本信息、专业方向（面料/服装/家纺/花型/印染等）、资质证书、设计成果等。" +
            "当用户询问某位纺织设计师的信息时使用此工具。参数可以是姓名或设计师编号。")
    public String queryDesignerProfile(
            @P(value = "纺织设计师姓名或编号，例如：张伟 或 TEX001") String nameOrNo) {

        log.info("🔍 查询纺织设计师档案: {}", nameOrNo);

        TextileDesignerProfile designer = null;

        // 尝试按姓名查询
        if (!nameOrNo.matches("TEX\\d+")) {
            designer = textileDesignerService.getDesignerByName(nameOrNo);
        }

        // 如果姓名查询失败，尝试按编号查询
        if (designer == null) {
            designer = textileDesignerService.getDesignerByNo(nameOrNo);
        }

        if (designer == null) {
            return "未找到名为\"" + nameOrNo + "\"的纺织设计师信息。请检查姓名或编号是否正确。";
        }

        // 格式化返回结果
        StringBuilder result = new StringBuilder();
        result.append("📋 纺织设计师档案信息\n");
        result.append("━━━━━━━━━━━━━━━━━━━━\n");
        result.append("👤 姓名：").append(designer.getName()).append("\n");
        result.append("🔖 编号：").append(designer.getDesignerNo()).append("\n");
        result.append(" 性别：").append(designer.getGender() == 1 ? "男" : "女").append("\n");
        result.append("🎓 学历：").append(designer.getEducation()).append("\n");
        result.append("📚 专业：").append(designer.getMajor()).append("\n");
        result.append("🧵 专业方向：").append(designer.getSpecialty()).append("\n");
        result.append("⏱️ 从业年限：").append(designer.getExperienceYears()).append("年\n");
        result.append("📜 资质证书：").append(designer.getCertificate()).append("\n");
        result.append("⭐ 综合评分：").append(designer.getRating()).append("/5.0\n");
        result.append("👍 好评率：").append(designer.getGoodRate()).append("%\n");
        result.append("📊 项目总数：").append(designer.getTotalProjects()).append("个\n");
        result.append("🏆 获奖数量：").append(designer.getTotalAwards()).append("项\n");
        result.append("\n📝 个人简介：\n").append(designer.getIntroduction()).append("\n");
        result.append("\n 工作经历：\n").append(designer.getWorkExperience()).append("\n");
        result.append("\n🏆 设计成果：\n").append(designer.getAchievements()).append("\n");
        result.append("\n🏷️ 技能标签：").append(designer.getSkillTags()).append("\n");
        result.append("━━━━━━━━━━━━━━━━━━━━\n");

        return result.toString();
    }

    /**
     * 查询纺织设计师的设计项目
     */
    @Tool(name = "查询纺织设计项目", value = "根据纺织设计师姓名或编号查询该设计师的纺织设计项目，包括项目名称、设计目标、进度安排、评审状态等。" +
            "当用户询问纺织设计师的项目安排或面料/花型/服装设计方案时使用此工具。")
    public String queryDesignProjects(
            @P(value = "纺织设计师姓名或编号") String nameOrNo) {

        log.info("📅 查询纺织设计项目: {}", nameOrNo);

        // 先获取设计师ID
        TextileDesignerProfile designer = getDesigner(nameOrNo);
        if (designer == null) {
            return "未找到纺织设计师\"" + nameOrNo + "\"，无法查询设计项目。";
        }

        List<TextileDesignProject> projects = textileDesignerService.getDesignProjectsByDesignerId(designer.getId());

        if (projects.isEmpty()) {
            return "纺织设计师\"" + designer.getName() + "\"暂无设计项目。";
        }

        StringBuilder result = new StringBuilder();
        result.append("📅 纺织设计项目列表（共").append(projects.size()).append("个）\n");
        result.append("━━━━━━━━━━━━━━━━━━━━\n");

        for (int i = 0; i < projects.size(); i++) {
            TextileDesignProject project = projects.get(i);
            result.append("\n【项目").append(i + 1).append("】\n");
            result.append(" 项目名称：").append(project.getProjectName()).append("\n");
            result.append(" 项目编号：").append(project.getProjectCode()).append("\n");
            result.append("📆 项目阶段：").append(project.getStage()).append("\n");
            result.append(" 设计周期：").append(project.getDesignCycle()).append("天（里程碑").append(project.getMilestoneCount()).append("个）\n");
            result.append("📅 时间：").append(project.getStartDate()).append(" 至 ").append(project.getEndDate()).append("\n");
            result.append(" 实际进度：").append(project.getActualProgress()).append("%\n");
            result.append("📝 设计目标：\n").append(project.getDesignObjectives()).append("\n");
            result.append("📚 设计内容：\n").append(project.getDesignContent()).append("\n");
            result.append("🎯 设计方法：").append(project.getDesignMethods()).append("\n");
            result.append("📋 验收标准：").append(project.getAcceptanceCriteria()).append("\n");

            // 项目状态
            String statusText = switch (project.getProjectStatus()) {
                case 0 -> "草稿";
                case 1 -> "待评审";
                case 2 -> "已通过";
                case 3 -> "已驳回";
                case 4 -> "进行中";
                case 5 -> "已完成";
                default -> "未知";
            };
            result.append("✅ 状态：").append(statusText).append("\n");

            if (project.getReviewComment() != null && !project.getReviewComment().isEmpty()) {
                result.append("💬 评审意见：").append(project.getReviewComment()).append("\n");
            }

            if (project.getDesignEffect() != null && !project.getDesignEffect().isEmpty()) {
                result.append(" 设计成效：").append(project.getDesignEffect()).append("\n");
            }

            result.append("────────────────────\n");
        }

        return result.toString();
    }

    /**
     * 查询纺织设计师的设计评审信息
     */
    @Tool(name = "查询纺织设计评审", value = "根据纺织设计师姓名或编号查询该设计师获得的纺织设计评审记录，包括评分、评审内容、情感分析结果等。" +
            "当用户询问纺织设计师的设计能力、评审反馈时使用此工具。")
    public String queryDesignReviews(
            @P(value = "纺织设计师姓名或编号") String nameOrNo) {

        log.info("⭐ 查询纺织设计评审: {}", nameOrNo);

        // 先获取设计师ID
        TextileDesignerProfile designer = getDesigner(nameOrNo);
        if (designer == null) {
            return "未找到纺织设计师\"" + nameOrNo + "\"，无法查询评审信息。";
        }

        List<TextileDesignReview> reviews = textileDesignerService.getReviewsByDesignerId(designer.getId());

        if (reviews.isEmpty()) {
            return "纺织设计师\"" + designer.getName() + "\"暂无设计评审记录。";
        }

        // 计算统计数据
        double avgScore = reviews.stream()
                .mapToDouble(e -> e.getOverallScore().doubleValue())
                .average()
                .orElse(0.0);

        long positiveCount = reviews.stream()
                .filter(e -> "positive".equals(e.getSentimentType()))
                .count();

        double positiveRate = (double) positiveCount / reviews.size() * 100;

        StringBuilder result = new StringBuilder();
        result.append("⭐ 纺织设计评审报告\n");
        result.append("━━━━━━━━━━━━━━━━━━━━\n");
        result.append("👤 纺织设计师：").append(designer.getName()).append("\n");
        result.append(" 评审总数：").append(reviews.size()).append("条\n");
        result.append("⭐ 平均评分：").append(String.format("%.2f", avgScore)).append("/5.0\n");
        result.append("😊 好评率：").append(String.format("%.1f", positiveRate)).append("%\n");
        result.append("\n📝 详细评审列表：\n");
        result.append("────────────────────\n");

        // 显示最新5条评审
        List<TextileDesignReview> latestReviews = reviews.stream()
                .limit(5)
                .collect(Collectors.toList());

        for (int i = 0; i < latestReviews.size(); i++) {
            TextileDesignReview review = latestReviews.get(i);
            result.append("\n【评审").append(i + 1).append("】\n");
            result.append("⭐ 总体评分：").append(review.getOverallScore()).append("/5.0\n");

            if (review.getInnovationScore() != null) {
                result.append("💡 创新性：").append(review.getInnovationScore()).append("/5.0\n");
            }
            if (review.getUsabilityScore() != null) {
                result.append("🎯 实用性：").append(review.getUsabilityScore()).append("/5.0\n");
            }
            if (review.getProfessionalScore() != null) {
                result.append("💼 专业水平：").append(review.getProfessionalScore()).append("/5.0\n");
            }
            if (review.getInteractionScore() != null) {
                result.append("🤝 协作沟通：").append(review.getInteractionScore()).append("/5.0\n");
            }
            if (review.getProcessabilityScore() != null) {
                result.append("🏭 工艺可行性：").append(review.getProcessabilityScore()).append("/5.0\n");
            }

            result.append("💬 评审内容：\n").append(review.getReviewContent()).append("\n");

            // 情感分析结果
            if (review.getSentimentType() != null) {
                String sentimentEmoji = switch (review.getSentimentType()) {
                    case "positive" -> "😊";
                    case "negative" -> "😔";
                    default -> "😐";
                };
                result.append("🎭 情感分析：").append(sentimentEmoji).append(" ")
                      .append(review.getSentimentType()).append(" (得分: ").append(review.getSentimentScore()).append(")\n");
            }

            result.append("────────────────────\n");
        }

        if (reviews.size() > 5) {
            result.append("\n... 还有").append(reviews.size() - 5).append("条评审未显示\n");
        }

        return result.toString();
    }

    /**
     * 查询纺织设计师的完整信息（档案+设计项目+设计评审）
     */
    @Tool(name = "查询纺织设计师完整信息", value = "根据纺织设计师姓名或编号查询该设计师的完整信息，包括档案信息、设计项目、设计评审等全部内容。" +
            "当用户要求全面了解某位纺织设计师时使用此工具。")
    public String queryDesignerFullInfo(
            @P(value = "纺织设计师姓名或编号") String nameOrNo) {

        log.info("📋 查询纺织设计师完整信息: {}", nameOrNo);

        TextileDesignerProfile designer = getDesigner(nameOrNo);
        if (designer == null) {
            return "未找到纺织设计师\"" + nameOrNo + "\"的信息。";
        }

        // 组合三个查询的结果
        String profile = queryDesignerProfile(nameOrNo);
        String projects = queryDesignProjects(nameOrNo);
        String reviews = queryDesignReviews(nameOrNo);

        return profile + "\n\n" + projects + "\n\n" + reviews;
    }

    /**
     * 根据专业方向查询纺织设计师
     */
    @Tool(name = "按专业方向查询纺织设计师", value = "根据专业方向查询该方向下的所有纺织设计师列表。" +
            "当用户询问某个专业方向有哪些纺织设计师时使用此工具，例如：面料设计师有哪些？")
    public String queryDesignersBySpecialty(
            @P(value = "专业方向，例如：面料设计、服装设计、家纺设计、花型设计、印染工艺等") String specialty) {

        log.info("🎯 按专业方向查询纺织设计师: {}", specialty);

        List<TextileDesignerProfile> designers = textileDesignerService.getDesignersBySpecialty(specialty);

        if (designers.isEmpty()) {
            return "暂未找到从事\"" + specialty + "\"方向的纺织设计师。";
        }

        StringBuilder result = new StringBuilder();
        result.append(" ").append(specialty).append("方向纺织设计师列表（共").append(designers.size()).append("位）\n");
        result.append("━━━━━━━━━━━━━━━━━━━━\n");

        for (int i = 0; i < designers.size(); i++) {
            TextileDesignerProfile designer = designers.get(i);
            result.append("\n【").append(i + 1).append("】\n");
            result.append(" 姓名：").append(designer.getName()).append("\n");
            result.append("🔖 编号：").append(designer.getDesignerNo()).append("\n");
            result.append("⏱️ 从业年限：").append(designer.getExperienceYears()).append("年\n");
            result.append(" 学历：").append(designer.getEducation()).append("\n");
            result.append("⭐ 评分：").append(designer.getRating()).append("/5.0\n");
            result.append("👍 好评率：").append(designer.getGoodRate()).append("%\n");
            result.append(" 项目总数：").append(designer.getTotalProjects()).append("个\n");
            result.append(" 简介：").append(designer.getIntroduction()).append("\n");
            result.append("────────────────────\n");
        }

        return result.toString();
    }

    /**
     * 辅助方法：根据姓名或编号获取纺织设计师对象
     */
    private TextileDesignerProfile getDesigner(String nameOrNo) {
        TextileDesignerProfile designer = null;

        // 尝试按姓名查询
        if (!nameOrNo.matches("TEX\\d+")) {
            designer = textileDesignerService.getDesignerByName(nameOrNo);
        }

        // 如果姓名查询失败，尝试按编号查询
        if (designer == null) {
            designer = textileDesignerService.getDesignerByNo(nameOrNo);
        }

        return designer;
    }
}
