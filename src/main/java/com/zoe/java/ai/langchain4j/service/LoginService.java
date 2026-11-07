package com.zoe.java.ai.langchain4j.service;

import com.zoe.java.ai.langchain4j.vo.CaptchaVo;
import com.zoe.java.ai.langchain4j.vo.LoginVo;
import org.springframework.stereotype.Service;


@Service
public interface LoginService {

    CaptchaVo getCaptcha();

    String login(LoginVo loginVo);
}
