package com.zoe.java.ai.langchain4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 纺织设计评审会议预约
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("textile_review_appointment")
public class TextileReviewAppointment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String idCard;

    /** 评审项目名称 */
    private String projectName;

    private String date;

    private String time;

    /** 评审专家姓名 */
    private String reviewerName;
}
