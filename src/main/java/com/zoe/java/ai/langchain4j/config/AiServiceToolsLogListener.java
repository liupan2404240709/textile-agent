package com.zoe.java.ai.langchain4j.config;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.spring.event.AiServiceRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AiServiceToolsLogListener implements ApplicationListener<AiServiceRegisteredEvent> {

    /**
     * {@link AiServiceRegisteredEvent} 只在 Spring 注册 AI Service 时触发一次，
     * {@link AiServiceRegisteredEvent#toolSpecifications()} 是<strong>当时</strong>可见的静态工具规格列表。
     * <p>
     * 它<strong>不会</strong>在运行时监听模型是否 “激活了某个 Skill”。
     * 官方 Skills（Tool Mode）里的 {@code activate_skill} 若存在，也属于工具调用，需通过
     * 工具执行回调 / 日志才能观察每次调用；本事件无法代替。
     * <p>
     * 当前工程已移除人物 Skill 注入，{@code PapaAgent} 的系统提示由
     * {@link PapaSystemPromptComposer} 从提示词模板组装，仅依赖 RAG 与工具，
     * 因此控制台不会出现 “skill active” 类日志——这是架构差异，不是监听器坏了。
     */
    @Override
    public void onApplicationEvent(AiServiceRegisteredEvent event) {
        String aiServiceName = event.aiServiceClass().getSimpleName();
        List<ToolSpecification> tools = event.toolSpecifications();
        String toolNames = tools.isEmpty()
                ? "(无)"
                : tools.stream()
                .map(ToolSpecification::name)
                .collect(Collectors.joining(", "));

        log.info(
                "[AiService 启动快照] {} 注册时的静态工具列表（非运行时、不表示 Skill 已激活）: {}",
                aiServiceName,
                toolNames
        );
    }
}
