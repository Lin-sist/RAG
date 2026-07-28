# Active Task

## Status

`ACTIVE`

## Active Change

- Change：`mcp-readonly-service`
- 阶段：C15 实现中（Read-Only Resources / URI parser checkpoint）
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

1. 用户已于 2026-07-28 要求启动 C15 规划实现，视为批准 proposal、18 条 design decisions、tasks、7 requirements / 26 scenarios delta，以及规划内固定版本官方 SDK / conformance 工具引入与下载。
2. SDK 2.0.0 compatibility/transport 闸门已通过当前 compile、initialize、schema runtime 与相邻回归；仍须在后续 Resources/Tools/conformance 路径持续检查 LinkageError/serialization drift。
3. authentication/Origin/local-only/request-size/request identity checkpoint 已由用户验收并提交为 `e17adca`；三种 Resource URI canonical parser 与恶意语法矩阵已完成，下一纵向切片进入 authenticated resources/templates/list 与分页授权，不横向预写 Tools。
4. 任何真实 embedding/rerank/ask/generation/provider smoke 仍需单独披露调用量、模型、数据出站、费用/限流并取得授权；当前没有该授权。

## Readiness Basis

- 启动 HEAD：`cab9939`；分支 `main...origin/main [ahead 7]`，工作区与暂存区在规划前均干净。
- C14 archive files=5、unchecked tasks=0、未归档 active change=0，`.ai/ACTIVE_TASK.md` 在启动时为 `IDLE`。
- C14 正式 evidence 为 26/26 required cases，functional/content/error/timing 与 global `PASS`；它解除 C15 前置阻塞，但不自动启用 MCP。
- 当前 C15 已加入固定官方 SDK 2.0.0、default-off `/mcp`、sessionless initialize、现有 JWT 逐请求保护、exact Origin/local-only/request-size guard、server-derived transport identity 与严格 KB/document/chunk Resource URI parser；Resource handlers/list/read、Tools、side-effect、双 tenant、conformance/client evidence 尚未实现。

## Emergency Rule

如果本文件指向的 change 不存在、用户未批准却进入实现、SDK compatibility 需要框架升级、或与用户当前请求冲突，停止写操作并先修正活动任务指针/事前闸门。
