package com.zoe.java.ai.langchain4j.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zoe.java.ai.langchain4j.entity.TextileReviewAppointment;

/**
 * 纺织设计评审会议预约服务
 */
public interface TextileReviewAppointmentService extends IService<TextileReviewAppointment> {
    //IService中有一些预定义的方法可以直接使用
    TextileReviewAppointment getOne(TextileReviewAppointment appointment);
}
