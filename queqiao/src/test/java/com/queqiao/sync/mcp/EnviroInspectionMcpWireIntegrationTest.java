package com.queqiao.sync.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 线缆级（真实 SSE 连接）MCP 集成测试：用 MCP Java SDK 的 SSE client transport
 * 对随机端口起的 queqiao 服务做 initialize → listTools → callTool，验证端点真实可用。
 *
 * <p>背景：Spring AI 1.0.0 + MCP Java SDK 0.10.0 的 webmvc 仅提供 SSE server transport
 * （{@code WebMvcSseServerTransportProvider}）。客户端用 SDK 的
 * {@code HttpClientSseClientTransport}（同样基于 SSE 协议），按 0.10.0 的协议约定：
 * <ol>
 *   <li>GET baseUri + sseEndpoint 建立 SSE 长连接；</li>
 *   <li>服务端首条事件为 {@code event: endpoint}，data 为
 *       {@code baseUrl + sseMessageEndpoint + "?sessionId=" + sessionId}；</li>
 *   <li>客户端解析该 URL 后 POST JSON-RPC 消息到该地址（含 sessionId 查询参数）。</li>
 * </ol>
 *
 * <p>关键约束：服务端 {@code base-url} 必须与 {@code sse-message-endpoint} 配合，使拼接结果
 * 等于 RouterFunction 实际注册的 POST 路径。当前 application.yml 中
 * {@code base-url=""} + {@code sse-message-endpoint="/mcp/message"}，
 * 拼接后为 {@code /mcp/message?sessionId=xxx}，与 RouterFunction 注册的
 * {@code POST /mcp/message} 完全一致。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EnviroInspectionMcpWireIntegrationTest {

    private static final Logger log =
            LoggerFactory.getLogger(EnviroInspectionMcpWireIntegrationTest.class);

    @LocalServerPort
    int port;

    @Test
    void wireLevel_initializeListToolsCallTool() {
        // 正确构造方式：baseUri 为服务根，sseEndpoint 为 SSE 接入路径。
        // SDK 内部通过 URI.resolve(baseUri, sseEndpoint) 拼出 GET URL。
        HttpClientSseClientTransport transport = HttpClientSseClientTransport
                .builder("http://localhost:" + port)
                .sseEndpoint("/mcp/sse")
                .build();
        McpSyncClient client = McpClient.sync(transport).build();
        try {
            log.info("[C9] SSE initialize -> baseUri=http://localhost:{}, sseEndpoint=/mcp/sse", port);
            client.initialize();
            log.info("[C9] SSE initialize 成功，准备 listTools");

            ListToolsResult tools = client.listTools();
            List<String> names = tools.tools().stream().map(Tool::name).toList();
            log.info("[C9] listTools -> {}", names);
            assertThat(names).containsExactlyInAnyOrder(
                    "getInspectionLedger", "getCameraStatus", "getInspectionSummary",
                    "triggerInspection", "downloadLedgerDocx");

            CallToolResult result = client.callTool(new CallToolRequest("getCameraStatus", Map.of()));
            log.info("[C9] callTool(getCameraStatus) -> content.size={}",
                    result == null || result.content() == null ? 0 : result.content().size());
            assertThat(result).isNotNull();
            assertThat(result.content()).isNotEmpty();
        } finally {
            client.close();
        }
    }
}
