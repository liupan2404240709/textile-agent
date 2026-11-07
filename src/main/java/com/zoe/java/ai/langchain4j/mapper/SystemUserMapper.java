package com.zoe.java.ai.langchain4j.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zoe.java.ai.langchain4j.entity.SystemUser;
import org.apache.ibatis.annotations.Param;

/**
* @author liubo
* @description 针对表【system_user(员工信息表)】的数据库操作Mapper
* @createDate 2023-07-24 15:48:00
* @Entity com.atguigu.lease.model.SystemUser
*/
public interface SystemUserMapper extends BaseMapper<SystemUser> {

    SystemUser selectOneByUsername(@Param("username") String username);

}




