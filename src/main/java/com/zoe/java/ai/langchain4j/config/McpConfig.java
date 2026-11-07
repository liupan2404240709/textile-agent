package com.zoe.java.ai.langchain4j.config;


import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport; // 智谱 MCP 仍使用旧版 SSE 协议
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.mcp.McpToolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class McpConfig {

    private static final Logger log = LoggerFactory.getLogger(McpConfig.class);

    @Value("${mcp.zhipu.api-key:}")
    private String zhipuApiKey;

    @Value("${mcp.baidu.api-key:}")
    private String baiduMapApiKey;

    @Value("${mcp.antv-chart.enabled:true}")
    private boolean antvChartEnabled;

    @Bean
    public McpTransport zhipuWebSearchTransport() {
        String sseUrl = "https://open.bigmodel.cn/api/mcp/web_search/sse?Authorization=" + zhipuApiKey;
        
        log.info("🔧 [MCP配置] 初始化智谱 Web Search MCP 传输层（SSE 协议）");
        log.info("📡 SSE URL: {}", sseUrl.replaceAll("Authorization=.*", "Authorization=***"));

        // 注意：智谱 MCP 服务端仍使用旧版 SSE 协议，需使用 HttpMcpTransport
        // 虽然该类已标记为 @Deprecated，但在服务端升级前仍需使用
        return new HttpMcpTransport.Builder()
                .sseUrl(sseUrl)
                .logRequests(true)  // 记录所有请求
                .logResponses(true) // 记录所有响应
                .build();
    }

    @Bean
    public McpTransport baiduMapTransport() {
        if (baiduMapApiKey == null || baiduMapApiKey.trim().isEmpty()) {
            log.warn("百度地图 API Key 未配置，跳过百度地图 MCP 客户端初始化");
            return null;
        }

        List<String> command = List.of(
                "cmd.exe", "/c", "npx", "-y", "@baidumap/mcp-server-baidu-map"
        );

        Map<String, String> environment = Map.of(
                "BAIDU_MAP_API_KEY", baiduMapApiKey
        );

        try {
            log.info("正在启动百度地图 MCP 服务: {}", String.join(" ", command));
            return new StdioMcpTransport.Builder()
                    .command(command)
                    .environment(environment)
                    .logEvents(true)
                    .build();
        } catch (Exception e) {
            log.error("启动百度地图 MCP 服务失败，请确认已安装 Node.js 和 npx 命令", e);
            return null;
        }
    }

    @Bean
    public McpClient zhipuWebSearchClient(@Qualifier("zhipuWebSearchTransport") McpTransport transport) {
        return new DefaultMcpClient.Builder()
                .transport(transport)
                .build();
    }

    @Bean
    public McpClient baiduMapClient(@Qualifier("baiduMapTransport") McpTransport transport) {
        if (transport == null) {
            log.warn("百度地图传输层未初始化，跳过百度地图 MCP 客户端创建");
            return null;
        }

        try {
            return new DefaultMcpClient.Builder()
                    .transport(transport)
                    .build();
        } catch (Exception e) {
            log.error("创建百度地图 MCP 客户端失败", e);
            return null;
        }
    }

    @Bean
    public McpTransport antvChartTransport() {
        if (!antvChartEnabled) {
            log.info("AntV Chart MCP 服务已禁用，跳过初始化");
            return null;
        }

        List<String> command = List.of(
                "cmd.exe", "/c", "npx", "-y", "@antv/mcp-server-chart"
        );

        try {
            log.info("📊 [MCP配置] 正在启动 AntV Chart MCP 服务: {}", String.join(" ", command));
            log.info("ℹ️  提示：首次启动可能需要下载 npm 包，请耐心等待...");
            return new StdioMcpTransport.Builder()
                    .command(command)
                    .logEvents(true)
                    .build();
        } catch (Exception e) {
            log.error("❌ 启动 AntV Chart MCP 服务失败，请确认已安装 Node.js 和 npx 命令", e);
            return null;
        }
    }

    @Bean
    public McpClient antvChartClient(@Qualifier("antvChartTransport") McpTransport transport) {
        if (transport == null) {
            log.warn("AntV Chart 传输层未初始化，跳过 AntV Chart MCP 客户端创建");
            return null;
        }

        try {
            log.info("✅ 正在创建 AntV Chart MCP 客户端");
            return new DefaultMcpClient.Builder()
                    .transport(transport)
                    .build();
        } catch (Exception e) {
            log.error("❌ 创建 AntV Chart MCP 客户端失败", e);
            return null;
        }
    }

    @Bean
    public ToolProvider mcpToolProvider(List<McpClient> clients) {
        List<McpClient> validClients = clients.stream()
                .filter(client -> client != null)
                .toList();

        if (validClients.isEmpty()) {
            log.warn("没有可用的 MCP 客户端，将返回空的 ToolProvider");
        } else {
            log.info("✅ MCP ToolProvider 初始化完成，共加载 {} 个 MCP 客户端", validClients.size());
            for (McpClient client : validClients) {
                log.info("  - MCP Client: {}", client.getClass().getSimpleName());
            }
        }

        return McpToolProvider.builder()
                .mcpClients(validClients)
                .build();
    }
}
