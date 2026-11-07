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
 * 纺织设计师档案
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("textile_designer_profile")
public class TextileDesignerProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 纺织设计师编号，例如 TEX001 */
    private String designerNo;

    private String name;

    private Integer gender;

    private LocalDate birthDate;

    private String idCard;

    private String phone;

    private String email;

    private String education;

    private String major;

    private String certificate;

    /** 专业方向，例如：面料设计、服装设计、家纺设计、花型设计、印染工艺 */
    private String specialty;

    private Integer experienceYears;

    private String introduction;

    private String workExperience;

    private String achievements;

    private String skillTags;

    private BigDecimal rating;

    private BigDecimal goodRate;

    private Integer totalProjects;

    private Integer totalAwards;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Byte isDeleted;
}
