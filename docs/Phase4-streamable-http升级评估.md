# Phase 4 后续行动：streamable-http 升级可行性评估（A1）

> 评估日期：2026-07-07
> 评估人：codebuddy（Phase 4 收尾阶段）
> 关联交付：Phase 4 鹊桥 MCP 服务（Spring AI 1.0.0 + MCP Java SDK 0.10.0，SSE 传输）

## 1. 背景

当前 Phase 4 交付的鹊桥 MCP 服务使用 **SSE 传输**（`WebMvcSseServerTransportProvider`），
端点 `GET /mcp/sse` + `POST /mcp/message`，对应 MCP 协议 2024-11-05 版的
"legacy SSE transport"（`transport: "sse"`）。

MCP 协议本身已演进到支持 **streamable-http**（`transport: "streamable-http"`），
提供更现代的 HTTP 流式通信模型（无长连接、HTTP 友好的会话恢复等）。MCP 官方
[Roadmap](https://modelcontextprotocol.io/development/roadmap) 也在推进
streamable-http 成为推荐传输。

## 2. 升级路径与依赖关系

| 组件 | 当前版本 | 支持 streamable-http 的版本 | 升级代价 |
|------|---------|---------------------------|---------|
| Spring Boot | 3.3.5 | 3.4.x / 3.5.x（被 1.1.0+ 强约束） | 中等 |
| Spring AI | 1.0.0 | **1.1.0-M1+**（新增 `WebMvcStreamableHttpServerTransport`） | 中等（API 变化、starter 名变更） |
| MCP Java SDK | 0.10.0 | 0.10.0+ 已支持 streamable-http client；server 端 `mcp-spring-webmvc` 需后续版本同步 | 需协同升级 |
| Tomcat | 10.1.x（Boot 3.3.5 带入） | 跟随 Boot 升级即可 | 无需手动处理 |
| JDK | 17 | 不变 | — |

**关键事实**（WebSearch + Maven 仓库核验，2026-07-07）：
- Spring AI 1.0.0 仅 SSE 传输（`WebMvcSseServerTransportProvider`）。
- Spring AI 1.1.0-M1+ 起新增 `WebMvcStreamableHttpServerTransport`（类名以 `Streamable` 关键字出现），并把 `mcp-spring-webmvc` 升级到 0.11.x+ 同步支持。
- Spring AI 1.0.0 → 1.1.0 是 minor 升级，**API 存在破坏性变化**（`ToolCallbackProvider` 构造、`@Tool` 注解默认值、Starter artifactId 调整），需要回归测试全部 `@Tool` 方法。

## 3. 升级后预期收益

| 维度 | SSE（当前） | streamable-http（升级后） |
|------|------------|-------------------------|
| 客户端实现成熟度 | 高（Spring AI、MCP 官方 SDK、桌面端、Inspector 均支持） | 中（0.10.0+ 客户端 SDK 支持，但 桌面端 MCP 实现仍以 SSE 为主流） |
| 代理/网关友好度 | 低（长连接、代理超时需调整） | 高（无状态 HTTP，可走标准反向代理） |
| 会话恢复 | 弱（依赖 SSE 事件 + sessionId） | 强（Last-Event-ID 续传） |
| 鉴权/限流友好度 | 中 | 高（普通 HTTP 中间件可直接用） |
| 部署可观测性 | 中（需要 SSE 解码） | 高（普通 HTTP 监控） |

## 4. 升级风险

1. **Spring AI 1.0.0 → 1.1.0 minor 升级**：破坏性 API 变化，@Tool 方法的返回类型校验、ToolCallbackProvider 注册流程可能需要改动。当前 Phase 4 交付的 5 个 `@Tool` 方法均返回 `OperationResultView`（具体 DTO，已规避了"返回 Object/Map 被丢弃"的 1.0.0 bug），但 1.1.0 的语义可能变化。
2. **依赖协同**：Spring Boot 3.3.5 ↔ Spring AI 1.1.0 兼容性需在 writing-plans 阶段实机验证（已知 Spring AI 1.1.x 系列强约束 Boot ≥ 3.4，可能需要一并升 Boot）。
3. **桌面端兼容性**：脑机桌面端 MCP client 的实现（自研/开源）若仅支持 SSE transport，升级后无法回连；需要先确认桌面端 SDK 是否支持 streamable-http。
4. **测试覆盖重置**：现有 `EnviroInspectionMcpWireIntegrationTest`（线缆级 SSE 集成测试，C9）针对 SSE 协议；升级到 streamable-http 后该测试需要重写（HTTP POST + 流式响应解析）。
5. **回归窗口**：升级期间所有 5 个 MCP 工具的真实调用路径都需要真机联调验证（Phase 4 已规划"真机联调留 Phase 5"，升级期建议**先于 Phase 5**做）。

## 5. 建议

**当前 Phase 4 不做升级**，原因：
- Phase 4 目标是"打通 MCP 集成链路 + 文档/测试完整性"，已通过 SSE 路径完整达成。
- 升级属于"协议层现代化"，是独立可切分的 Phase 6+ 工作。
- SSE 是 MCP 当前桌面端/官方 SDK 的**主流通用传输**，脑机桌面端生产对接零障碍。

**建议在 Phase 5 末或 Phase 6 评估升级窗口**，前置条件：
1. 脑机桌面端 MCP client SDK 确认支持 streamable-http；
2. Spring AI 1.1.x 升级到 GA（不是 M 里程碑）；
3. 配套 MCP Java SDK 升级到同步 GA；
4. 在 writing-plans 阶段先做小型 Spike 验证（4-8h 范围）再做完整迁移。

## 6. 本评估的关联交付

- 当前 SSE 端点：`GET /mcp/sse` + `POST /mcp/message`（见 `queqiao/src/main/resources/application.yml`）
- 升级留口子：所有 MCP 工具以 `@Tool` 注解封装在 `EnviroInspectionMcpTools` 中，
  不直接依赖传输层；未来升级只需替换 starter + transport 配置，业务代码零改动。
- 回归兜底：若升级中出现阻塞，可回退到 SSE 方案（保留 `spring-ai-starter-mcp-server-webmvc`
  + `transport: WEBMVC`），不影响业务功能。
