package com.zoe.java.ai.langchain4j.dto;

import lombok.Data;

@Data
public class ChatForm {
    private String memoryId;
    private String message;
    private Boolean webSearchEnabled;
}
