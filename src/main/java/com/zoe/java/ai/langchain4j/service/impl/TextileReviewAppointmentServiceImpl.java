package com.zoe.java.ai.langchain4j.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zoe.java.ai.langchain4j.entity.TextileReviewAppointment;
import com.zoe.java.ai.langchain4j.mapper.TextileReviewAppointmentMapper;
import com.zoe.java.ai.langchain4j.service.TextileReviewAppointmentService;
import org.springframework.stereotype.Service;

@Service
public class TextileReviewAppointmentServiceImpl
        extends ServiceImpl<TextileReviewAppointmentMapper, TextileReviewAppointment>
        implements TextileReviewAppointmentService {

    /**
     * 查询评审预约是否已存在
     *
     * @param appointment 预约信息
     * @return 已存在的预约记录，不存在返回 null
     */
    @Override
    public TextileReviewAppointment getOne(TextileReviewAppointment appointment) {
        LambdaQueryWrapper<TextileReviewAppointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TextileReviewAppointment::getUsername, appointment.getUsername());
        queryWrapper.eq(TextileReviewAppointment::getIdCard, appointment.getIdCard());
        queryWrapper.eq(TextileReviewAppointment::getProjectName, appointment.getProjectName());
        queryWrapper.eq(TextileReviewAppointment::getDate, appointment.getDate());
        queryWrapper.eq(TextileReviewAppointment::getTime, appointment.getTime());

        return baseMapper.selectOne(queryWrapper);
    }
}
