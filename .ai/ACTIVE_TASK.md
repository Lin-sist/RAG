# Active Task

## Status

`ACTIVE`

## Active Change

- Change：`bounded-query-router`
- 阶段：C16 实现与 deterministic evidence 已完成，等待用户最终验收
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

1. 用户已于 2026-07-30 批准统一规划、18 条设计决策与双 spec delta，并要求开始 C16 实现。
2. vertical TDD 实现已覆盖 default-off Router/classifier/registry、budgeted fact、no-answer、SSE/MCP 与 versioned evaluation；当前不再扩展功能范围。
3. 提交责任保持 `用户手动提交`；Agent 不暂存、不提交。accepted baseline、归档与 `IDLE` 仍需用户最终验收。
4. 真实 embedding/rerank/ask/generation/judge/LLM/provider 调用未授权；实现与主证据必须保持 zero-call/zero-egress。
5. 已完成 default-off Router、closed-world `fact-v1` executor、确定性 fact/unsupported/invalid 分类、跨 stage budget/usage、validated-citation no-answer、版本化 cache identity、sync/SSE/MCP attribution 与低基数 telemetry。
6. `bounded-query-router-eval-v1` 已固定 20 条 ID-only sidecar、budget profile、本地 validator、七通道 evaluator 与 traceability；plan-only=`VALID`，真实 provider/model calls=0、business data outbound=false、live=`SKIPPED`。
7. Java C16 focused 与全部 MCP 相邻回归通过，Python 211/211 通过；Maven full 616 tests 中 1 个既有 tracing 日志捕获时序失败、2 个 Redis 环境用例跳过，该 tracing 类隔离重跑 9/9 通过，因此全仓不声明 GREEN。

## Readiness Basis

- 启动 HEAD：`3b2f06e`；分支 `main...origin/main [ahead 14]`，工作区与暂存区在规划前均干净。
- `.ai/ACTIVE_TASK.md` 在启动时为 `IDLE`，未归档 active change=0；C15 archive 存在且 tasks unchecked=0。
- C14 的 26/26 required cases 与四通道/global `PASS` 已解除 Router 前置隔离门禁；C15 也已独立归档，MCP 与 Router 没有依赖捆绑。
- 当前代码 confirmed 为 query normalization/variants、hybrid/RRF/rerank attribution、context token budget、sync/SSE/MCP ask 与 no-answer metadata；显式 Router/classifier/strategy registry/跨阶段 budget 均不存在。

## Emergency Rule

如果本文件指向的 change 不存在、用户未批准却进入实现、实现需要新增/升级依赖或修改公开 API/SSE/MCP/schema/migration/frontend/生产默认，或与用户当前请求冲突，停止写操作并先修正活动任务指针/事前闸门。
