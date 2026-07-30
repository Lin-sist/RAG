# Active Task

## Status

`ACTIVE`

## Active Change

- Change：`mcp-readonly-service`
- 阶段：C15 实现与 synthetic evidence 已闭环，待用户最终验收、手动提交后的 clean-HEAD 复跑与归档
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

1. 用户已于 2026-07-30 验收上一切片并明确允许按实际情况完成剩余 C15；implementation-discovered 决策 20 视为复核通过，document/chunk Resources、四个固定只读 Tools 与验证切片已继续完成。
2. SDK 2.0.0 compatibility/transport 闸门、独立官方 Java SDK client、双 tenant Testcontainers integration，以及 conformance 0.1.15 中适用于固定 C15 capability 的五个 generic server scenarios 均已通过；选择性范围和 Node 22.17.0 Windows runtime 见 design 决策 22。
3. 当前 evidence 生成于 HEAD `e812e3e` 的 dirty working tree：这是实现期可复现证据，不是可归档 clean-HEAD 正式报告。用户手动提交后必须在新 HEAD 重跑 `c15-mcp-readonly` profile，再由用户最终验收后接受 delta、归档 change 并置 `IDLE`。
4. 任何真实 embedding/rerank/ask/generation/provider smoke 仍需单独披露调用量、模型、数据出站、费用/限流并取得授权；当前没有该授权，本轮只使用 synthetic deterministic fixture，live smoke 明确 `SKIPPED`。

## Readiness Basis

- 启动 HEAD：`cab9939`；分支 `main...origin/main [ahead 7]`，工作区与暂存区在规划前均干净。
- C14 archive files=5、unchecked tasks=0、未归档 active change=0，`.ai/ACTIVE_TASK.md` 在启动时为 `IDLE`。
- C14 正式 evidence 为 26/26 required cases，functional/content/error/timing 与 global `PASS`；它解除 C15 前置阻塞，但不自动启用 MCP。
- 当前 C15 已实现 default-off/local-only/sessionless `/mcp`、现有 JWT 逐请求认证、三种 bounded tenant-scoped Resources、四个固定只读 Tools、严格原始参数校验、ResourceLink、rate/concurrency/timeout/result bounds 与脱敏错误；`rag.search`/`rag.ask` 另有 default-off 开关，`rag.ask` 不经过 REST controller，默认不读写 QA cache，也不写 history/query count。
- `C15McpReadOnlyIT` 的自有 MySQL 8.0.36、Redis 7-alpine digest、etcd 3.5.5、MinIO 固定 release 与 Milvus 2.3.4 synthetic fixture 已证明权威状态摘要前后一致、real provider/model calls=0、business data outbound=false；独立 Java SDK client 与适用 conformance scenarios 也已通过。

## Emergency Rule

如果本文件指向的 change 不存在、用户未批准却进入实现、SDK compatibility 需要框架升级、或与用户当前请求冲突，停止写操作并先修正活动任务指针/事前闸门。
