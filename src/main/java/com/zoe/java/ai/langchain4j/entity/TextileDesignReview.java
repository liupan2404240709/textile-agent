package com.zoe.java.ai.langchain4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 纺织设计评审记录
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("textile_design_review")
public class TextileDesignReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long designerId;

    private Long reviewerId;

    private Long projectId;

    private String projectName;

    private BigDecimal overallScore;

    /** 创新性评分 */
    private BigDecimal innovationScore;

    /** 实用性评分 */
    private BigDecimal usabilityScore;

    /** 专业水平评分 */
    private BigDecimal professionalScore;

    /** 协作沟通评分 */
    private BigDecimal interactionScore;

    /** 工艺可行性评分（试纺/试织/印染打样可行性） */
    private BigDecimal processabilityScore;

    private String reviewContent;

    private String sentimentType;

    private BigDecimal sentimentScore;

    private Byte isAnonymous;

    /** 评审状态：0-待评审 1-已通过 2-已驳回 */
    private Byte reviewStatus;

    private String replyContent;

    private LocalDateTime replyTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Byte isDeleted;
}
