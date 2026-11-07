package com.zoe.java.ai.langchain4j;

import com.zoe.java.ai.langchain4j.annotation.EnableLlmConversationLogging;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@MapperScan("com.zoe.java.ai.langchain4j.mapper")
@EnableLlmConversationLogging
public class PapaApplication {
    public static void main(String[] args) {
        SpringApplication.run(PapaApplication.class, args);
    }
}
