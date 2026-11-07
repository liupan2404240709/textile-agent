package com.zoe.java.ai.langchain4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
        wiringMode = EXPLICIT,
        streamingChatModel = "qwenStreamingChatModel",
        chatMemoryProvider = "chatMemoryProviderPapa",
        contentRetriever = "contentRetrieverPapa",
        toolProvider = "mcpToolProvider",
        tools = {"textileDesignQueryTools", "textileReviewAppointmentTools"}
)
public interface PapaAgent {

    @SystemMessage("{{systemPrompt}}")
    Flux<String> chat(
            @MemoryId String memoryId,
            @UserMessage String message,
            @V("systemPrompt") String systemPrompt
    );
}