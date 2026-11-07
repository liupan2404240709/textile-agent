package com.zoe.java.ai.langchain4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 纺织设计项目
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("textile_design_project")
public class TextileDesignProject {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long designerId;

    private String projectName;

    private String projectCode;

    /** 项目阶段，例如：概念提案、面料开发、花型设计、印染打样、大货生产 */
    private String stage;

    /** 设计周期（天） */
    private Integer designCycle;

    /** 里程碑数量 */
    private Integer milestoneCount;

    private LocalDate startDate;

    private LocalDate endDate;

    private String designObjectives;

    private String designContent;

    private String designMethods;

    /** 验收标准 */
    private String acceptanceCriteria;

    private String scheduleDetails;

    /** 项目状态：0-草稿 1-待评审 2-已通过 3-已驳回 4-进行中 5-已完成 */
    private Integer projectStatus;

    private String reviewComment;

    private Long reviewerId;

    private LocalDateTime reviewTime;

    private Byte reminderEnabled;

    private LocalDateTime lastReminderTime;

    private BigDecimal actualProgress;

    private String designEffect;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Byte isDeleted;
}
