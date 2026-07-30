# Active Task

## Status

`ACTIVE`

## Active Change

- Change：`bounded-query-router`
- 阶段：C16 规划待审
- 位置：`openspec/changes/bounded-query-router/`
- 类型：Type C（新增用户可见 Router 策略、跨阶段预算、no-answer policy 与评测契约）
- 提交责任：`用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。

## Scope

1. 建立 default-off、确定性的 `fact-intent-v1` 与唯一 `fact-v1` strategy；enabled 模式的非 fact/ambiguous 输入稳定 `UNSUPPORTED`，不回退 legacy。
2. 通过 immutable route plan/usage ledger 限制 query variants、一次 retrieval orchestration、rerank/generation calls、token ceiling 与 stage 间 deadline。
3. 建立 `evidence-no-answer-v1`，分离 insufficient evidence、model refusal、citation不足与 dependency error；no-answer 不作为前置 route。
4. 对齐 sync/SSE/C15 read-only ask、cache 与低基数 telemetry attribution，不修改公开 request DTO、SSE wire 或 MCP Tool schema。
5. 建立独立 versioned router evaluation release/status，按 classification、strategy、budget、retrieval、generation/citation、no-answer 与 error 通道验收。

## Current Gate

1. proposal、design 的 18 条决策、tasks 与 `rag-system` / `evaluation` 双 spec delta 已形成规划草案，等待用户审阅批准。
2. 当前授权只覆盖规划文件；批准前不得修改 Java/Python/runtime config、运行 Docker/Testcontainers、启用 Router 或执行 provider-capable evaluation。
3. 用户需重点确认 default-off、enabled 仅 `fact-v1`、unsupported 不走 legacy、deterministic classifier、deadline 真实保证、统一 no-answer 与 deterministic 主证据。
4. 任何真实 embedding/rerank/ask/generation/judge/LLM/provider 调用仍需单独披露调用量、模型、数据出站、费用/限流并取得授权。

## Readiness Basis

- 启动 HEAD：`3b2f06e`；分支 `main...origin/main [ahead 14]`，工作区与暂存区在规划前均干净。
- `.ai/ACTIVE_TASK.md` 在启动时为 `IDLE`，未归档 active change=0；C15 archive 存在且 tasks unchecked=0。
- C14 的 26/26 required cases 与四通道/global `PASS` 已解除 Router 前置隔离门禁；C15 也已独立归档，MCP 与 Router 没有依赖捆绑。
- 当前代码 confirmed 为 query normalization/variants、hybrid/RRF/rerank attribution、context token budget、sync/SSE/MCP ask 与 no-answer metadata；显式 Router/classifier/strategy registry/跨阶段 budget 均不存在。

## Emergency Rule

如果本文件指向的 change 不存在、用户未批准却进入实现、实现需要新增/升级依赖或修改公开 API/SSE/MCP/schema/migration/frontend/生产默认，或与用户当前请求冲突，停止写操作并先修正活动任务指针/事前闸门。
