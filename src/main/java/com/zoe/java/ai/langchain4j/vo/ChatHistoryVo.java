
package com.zoe.java.ai.langchain4j.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatHistoryVo {

    private String memoryId;

    private String firstMessage;

    private LocalDateTime lastUpdateTime;
}