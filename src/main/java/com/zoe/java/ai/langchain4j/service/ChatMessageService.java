
package com.zoe.java.ai.langchain4j.service;

import com.zoe.java.ai.langchain4j.vo.ChatHistoryVo;

import java.util.List;

public interface ChatMessageService {

    List<ChatHistoryVo> getHistoryList();

    String getChatContent(String memoryId);

    void deleteHistory(String memoryId);
}