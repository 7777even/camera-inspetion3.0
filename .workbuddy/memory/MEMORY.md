# Project Conventions

## 开发方法论
- **必须遵循 Superpowers 开发流程**。任何功能开发、Bug 修复、代码修改，在写代码之前必须走：brainstorming → writing-plans → subagent-driven-development（或 executing-plans）→ TDD → code-review → verification-before-completion 的完整流程。
- 禁止跳过设计阶段直接写代码。即使是"简单"的改动也要先出设计再动手。

## 用户协作偏好
- 即使用户说"直接执行 / 直接做"，**也必须保留 brainstorming 流程里的 spec 用户审阅环节**（设计文档写好后停下来请用户审阅确认，再进 writing-plans）。用户纠正过一次：不要跳过该审阅环节。

## 技术栈踩坑（已沉淀，避免再犯）
### MCP Java SDK 0.10.0 + Spring AI 1.0.0
- webmvc starter 仅支持 **SSE server transport**（`WebMvcSseServerTransportProvider`），不支持 streamable-http。
- `transport` 合法值：`WEBMVC` / `WEBFLUX`（非 `streamable-http`）。
- SSE 端点以 functional `RouterFunction` 注册，路由路径 = `sse-endpoint` / `sse-message-endpoint` 本身，**`base-url` 不前缀到路由**。
- **`base-url` 会拼到 endpoint 事件下发的 POST 路径**（公式 `baseUrl + sseMessageEndpoint + "?sessionId=" + sessionId`），因此 `base-url` **必须为空字符串** `""`，否则客户端 POST 落到 404，10s 等不到 message endpoint，抛 `McpError: Failed to wait for the message endpoint`。详细见 `docs/Phase4-streamable-http升级评估.md`。
- `MethodToolCallbackProvider` 会**静默丢弃返回 `Object`/`Map` 的 `@Tool` 方法**（不报错）。`@Tool` 方法必须返回**具体 DTO**（如 `OperationResultView`）。
- 线缆级 SSE 集成测试正确构造：`HttpClientSseClientTransport.builder(baseUri).sseEndpoint("/mcp/sse").build()`，其中 `baseUri` 不含 sse 路径（SDK 内部 `URI.resolve` 拼出 GET URL）。默认 `sseEndpoint = "/sse"`，不可省略 `.sseEndpoint(...)`。
