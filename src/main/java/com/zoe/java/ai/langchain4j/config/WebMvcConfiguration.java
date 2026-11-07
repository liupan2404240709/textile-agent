package com.zoe.java.ai.langchain4j.config;


import com.zoe.java.ai.langchain4j.converter.StringToBaseEnumConverterFactory;
import com.zoe.java.ai.langchain4j.interceptor.AuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {


    @Autowired
    private StringToBaseEnumConverterFactory stringToBaseEnumConverterFactory;

    @Autowired
    private AuthenticationInterceptor authenticationInterceptor;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(this.stringToBaseEnumConverterFactory);
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this.authenticationInterceptor)
                .addPathPatterns("/admin/**", "/papa/**")
                .excludePathPatterns("/admin/login", "/admin/login/**");
                // 如果 papa 接口需要匿名访问，取消下面注释
                // .excludePathPatterns("/papa/chat", "/papa/generateImage");
    }
}
