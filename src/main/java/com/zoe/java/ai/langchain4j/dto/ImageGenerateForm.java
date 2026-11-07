package com.zoe.java.ai.langchain4j.dto;

import lombok.Data;

@Data
public class ImageGenerateForm {
    private String prompt;
    private String size;
}
