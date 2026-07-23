# Active Task

## Status

`ACTIVE`

## Change

- ID：`2026-07-23-eval-quality-threshold-gates`
- 位置：`openspec/changes/2026-07-23-eval-quality-threshold-gates/`
- 阶段：C10 proposal/design/tasks/spec delta 已建立，等待用户审阅与 offline TDD implementation 授权。

## Goal

建立版本化 eval quality gate profile、按指标通道与 `all/type/difficulty/answerability` 切片的确定性阈值/容差判定、缺失与错误 fail-closed 语义，以及 `PASS/FAIL/NOT_EVALUABLE/INVALID` 的稳定退出码。

## Scope

- tracked profile schema/identity 与 DRAFT/ACTIVE 生命周期；
- 独立 offline evaluator，消费 local details JSON，不触发 backend/provider；
- hard threshold、reference regression tolerance、minimum denominator 与 per-channel completeness；
- 脱敏 gate JSON/Markdown/console output；
- 首个 v2 retrieval profile 的 shape，以及后续 reference evidence/阈值激活闸门。

## Non-Goals

- planning/offline implementation 阶段不执行 embedding、rerank、debug retrieval、ask、generation、judge 或其他 provider 调用；
- 不追认 C7 历史 30 条报告为 v2 gate evidence，不把 C9a/C9b 离线结果冒充真实 generation/judge baseline；
- 不修改 dataset、metric formulas、Java/API、数据库、前端、依赖、production prompt/citation/no-answer、默认 judge/reranker/provider；
- 不自动调参、不修改 CI 平台配置、不进入 C11+ 或 C14。

## Current Gate

1. 用户审阅并批准 proposal 的两道闸、profile/status/exit-code 语义、initial retrieval-only 边界与 non-goals。
2. 用户审阅并批准 design 的 15 条决策记录和 `evaluation` delta 的 4 requirements / 12 scenarios。
3. 用户明确授权后才进入 offline TDD implementation；该授权默认不包含任何 live/reference provider 调用。
4. 首个 profile 只有在 v2 reference evidence 单独披露/授权并由用户确认具体阈值后才可从 `DRAFT` 切为 `ACTIVE`。

## Submission Responsibility

`用户手动提交`。Agent 不暂存、不提交、不 push、不创建 PR、不部署。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
