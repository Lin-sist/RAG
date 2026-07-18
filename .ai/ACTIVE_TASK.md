# Active Task

## Status

`ACTIVE`

## Active Change

- Change：`2026-07-18-nvidia-reranker-adapter-and-attribution`
- 位置：`openspec/changes/2026-07-18-nvidia-reranker-adapter-and-attribution/`
- 阶段：规划草案，等待用户事前闸门批准；尚未进入实现。

## Goal

在不改变默认 heuristic、不过早进行 C7 收益 A/B 的前提下，新增 NVIDIA `/v1/ranking` 协议适配，并让每次检索能够结构化说明 requested/effective reranker、fallback 原因与次数、真实 model 调用数、候选覆盖和延迟，供同步问答、debug retrieval 与评测 runner 逐样本归因。

## Scope

- 新增显式 `nvidia` reranker provider 与 typed NVIDIA ranking 请求/响应契约。
- 建立 rerank outcome/diagnostics，合并进既有 `RetrievalResult.diagnostics`。
- 在同步 QA metadata、debug retrieval 和评测报告中暴露脱敏的逐样本归因。
- 使用本地合成 HTTP server、Java/Python 单测和完整质量门禁验证协议、fallback 与报告聚合。
- 真实 provider smoke 仅在用户单独批准明确调用预算后执行。

## Non-goals

- 不修改默认 `retrieval.rerank.provider=heuristic`。
- 不在 C6 下真实收益、Recall/MRR/Top1 改善或默认 provider 切换结论；A/B 留给 C7。
- 不修改 embedding、分块、hybrid/RRF、prompt、citation、no-answer、judge 指标口径。
- 不修改数据库、索引状态机、前端或 SSE 结构化完成事件。
- 不进行批量 rerank、ask、judge、embedding 或其他业务外部调用。

## Approval And Commit Responsibility

- 用户已授权启动 C6 规划，但尚未批准 proposal/design/tasks/spec delta 或生产实现。
- 提交责任：`用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- 真实 NVIDIA 调用：未授权；当前允许调用量为 0。

## Acceptance Entry

1. 用户批准 proposal、design 决策记录、tasks 与 `rag-system` spec delta 后，才进入实现。
2. 实现按 TDD 小切片推进，先验证本地 NVIDIA 协议与 attribution，再改生产代码。
3. closeout 前必须明确真实 smoke 是“授权执行一次”还是“明确延期并保留 protocol-tested 边界”。
4. 用户验收后才能接受 delta、恢复 `IDLE` 并归档 change。

## Last Completed

- Change：`2026-07-18-c5-recovery-debt-closeout`
- 结果：C5 已登记恢复实现债务清零，delta 已接受并归档。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
