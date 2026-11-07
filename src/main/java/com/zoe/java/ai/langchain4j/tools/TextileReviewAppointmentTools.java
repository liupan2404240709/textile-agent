package com.zoe.java.ai.langchain4j.tools;

import com.zoe.java.ai.langchain4j.entity.TextileReviewAppointment;
import com.zoe.java.ai.langchain4j.service.TextileReviewAppointmentService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 纺织设计评审会议预约工具
 */
@Component
public class TextileReviewAppointmentTools {

    @Autowired
    private TextileReviewAppointmentService textileReviewAppointmentService;

    @Tool(name = "预约纺织设计评审", value = "根据参数，先执行工具方法queryReviewSchedule查询该评审会议是否可预约，并直接给用户回答是否可预约，" +
            "并让用户确认所有预约信息，用户确认后再进行预约。如果用户没有提供具体的评审专家姓名，请从向量存储中找到一位评审专家。")
    public String bookReviewAppointment(TextileReviewAppointment appointment) {

        //查找数据库中是否包含对应的预约记录
        TextileReviewAppointment appointmentDB = textileReviewAppointmentService.getOne(appointment);

        if (appointmentDB == null) {
            appointment.setId(null);//防止大模型幻觉设置了id
            if (textileReviewAppointmentService.save(appointment)) {
                return "评审预约成功，并返回预约详情";
            } else {
                return "评审预约失败";
            }
        }

        return "您在相同的项目和时间已有评审预约";
    }

    @Tool(name = "取消纺织设计评审预约", value = "根据参数，查询预约是否存在，如果存在则删除预约记录并返回取消预约成功，否则返回取消预约失败")
    public String cancelReviewAppointment(TextileReviewAppointment appointment) {

        TextileReviewAppointment appointmentDB = textileReviewAppointmentService.getOne(appointment);
        if (appointmentDB != null) {
            //删除预约记录
            if (textileReviewAppointmentService.removeById(appointmentDB.getId())) {
                return "取消评审预约成功";
            } else {
                return "取消评审预约失败";
            }
        }

        //取消失败
        return "您没有评审预约记录，请核对评审项目和时间";
    }

    @Tool(name = "查询评审排期", value = "根据项目名称，日期，时间和评审专家查询是否还有评审排期，并返回给用户")
    public boolean queryReviewSchedule(
            @P(value = "评审项目名称") String name,
            @P(value = "日期") String date,
            @P(value = "时间，可选值：上午、下午") String time,
            @P(value = "评审专家姓名", required = false) String reviewerName
    ) {

        System.out.println("查询评审排期");
        System.out.println("项目名称：" + name);
        System.out.println("日期：" + date);
        System.out.println("时间：" + time);
        System.out.println("评审专家：" + reviewerName);

        //TODO 维护评审专家的排期信息：
        //如果没有指定评审专家名字，则根据其他条件查询是否有可以预约的评审专家（有返回true，否则返回false）；

        //如果指定了评审专家名字，则判断该专家是否有排班（没有排班返回false）
        //如果有排班，则判断该专家排班时间段是否已约满（约满返回false，有空闲时间返回true）

        return true;
    }

}
