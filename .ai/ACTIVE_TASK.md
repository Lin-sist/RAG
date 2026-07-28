# Active Task

## Status

`ACTIVE`

## Active Change

- Change：`mcp-readonly-service`
- 阶段：C15 规划待审
- 位置：`openspec/changes/mcp-readonly-service/`
- 类型：Type C（新增用户可见 MCP protocol surface、认证/tenant 传播、只读副作用与外部调用契约）
- 提交责任：`用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。

## Scope

1. 建立 default-off、local-only、sessionless Streamable HTTP `/mcp`，只声明 Resources 与固定 read-only Tools。
2. 复用现有 access JWT 与 server-derived `RequestIdentity`；客户端不得提交 tenant/collection/filter/provider selector，且首版不宣称 MCP OAuth authorization profile 兼容。
3. 规划 KB/document/chunk Resources，以及 `rag.search`、`rag.ask`、`rag.get-citation`、`rag.compare-sources` 四个 bounded Tool。
4. `rag.ask` 不写 QA history/query count；只允许受约束 tenant-scoped cache、rate-limit 与隐私白名单 telemetry 等非权威技术写入。
5. 官方 MCP Java SDK 与 Spring Boot 3.2.1 的兼容性是实现前 hard gate；不兼容时停止并另立 runtime foundation change，不在 C15 升级框架或手写协议。

## Current Gate

1. proposal、design 的 18 条决策、tasks 与 `rag-system` 7 requirements / 26 scenarios spec delta 已形成规划草案，等待用户审阅批准。
2. 当前授权只覆盖规划文件；批准前不得加入/下载 MCP SDK、修改 POM/Java/runtime config、启动 Testcontainers/conformance 或开放 `/mcp`。
3. 用户需重点确认 sessionless/default-off/local-only、现有 JWT 非 OAuth profile、deterministic compare-sources、external tools 独立开关与 SDK compatibility hard-stop。
4. 任何真实 embedding/rerank/ask/generation/provider smoke 仍需单独披露调用量、模型、数据出站、费用/限流并取得授权。

## Readiness Basis

- 启动 HEAD：`cab9939`；分支 `main...origin/main [ahead 7]`，工作区与暂存区在规划前均干净。
- C14 archive files=5、unchecked tasks=0、未归档 active change=0，`.ai/ACTIVE_TASK.md` 在启动时为 `IDLE`。
- C14 正式 evidence 为 26/26 required cases，functional/content/error/timing 与 global `PASS`；它解除 C15 前置阻塞，但不自动启用 MCP。
- 当前代码 confirmed 为 Boot 3.2.1 WebMVC、JWT `UserPrincipal`/`RequestIdentity`、tenant-aware KB/document/RAG/citation/cache；MCP dependency/endpoint/schema 均不存在。

## Emergency Rule

如果本文件指向的 change 不存在、用户未批准却进入实现、SDK compatibility 需要框架升级、或与用户当前请求冲突，停止写操作并先修正活动任务指针/事前闸门。
