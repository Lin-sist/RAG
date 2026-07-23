# Active Task

## Status

`ACTIVE`

## Active Change

- Change：`2026-07-23-claim-evidence-objective-metrics`
- 位置：`openspec/changes/2026-07-23-claim-evidence-objective-metrics/`
- 目标：为成功的 answerable generation 输出增加确定性 claim 拆分，并只使用通过既有 citation-context provenance 校验的 returned citation evidence 计算客观词法对齐、逐 claim attribution 与聚合完整性状态。
- 范围：Python eval runner 的离线 claim contract、eligible evidence、exact/token lexical alignment、per-sample/aggregate report schema、算法 identity、测试与评测指南；离线 TDD 实现已完成，当前等待用户验收，不接受 baseline、不归档。
- 非目标：不做 C9b judge 校准或 objective/judge 全局状态拆分，不做 C10 threshold/exit gate，不改 dataset release、Java/API、prompt、生产 citation、retrieval/chunking/rerank/no-answer、前端或 SSE，不运行真实 generation baseline。
- 验收入口：`proposal.md` 的 planning/implementation/closeout gate、`design.md` 的 12 条决策记录、`tasks.md` 与 `specs/evaluation/spec.md` 的 4 requirements / 12 scenarios。
- 提交责任：`用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- 外部调用：离线实现阶段 embedding/rerank/ask/judge/LLM/provider 调用量与数据出站均为 0；后续任何真实 150 条 generation evidence run 必须单独披露并授权，本次实现授权不包含该运行。

## Last Completed

- Change：`2026-07-22-eval-dataset-expansion-and-annotation`
- 位置：`openspec/changes/archive/2026-07-22-eval-dataset-expansion-and-annotation/`
- 结果：完成 `rag-eval-dev-v2` 的 150 条 expanded dataset、exact quota、immutable v1 seed、fixture grounding/coverage、duplicate/near-duplicate、150 条 review sidecar、manifest schema v2 与 v1/v2 并存验证；默认 manifest 已切换到 v2。
- 验收：用户已验收 expanded data、review evidence、v1/v2 identity 与结论边界；4 个 requirements / 12 个 scenarios 已接受进 `evaluation` baseline，change 已归档。C8b 不代表 C9/C10/C14 或质量收益结论已完成。

## Start A New Material Change

1. 在 `openspec/changes/<change-id>/` 创建 `proposal.md`、`design.md`、`tasks.md` 和需要的 spec delta。
2. 将本文件 `Status` 改为 `ACTIVE`，并填写 change id、目标、范围、非目标和验收入口。
3. 按 `AGENTS.md` 执行并持续更新 tasks 与 `.ai/AGENT_LOG.md`。
4. 完成且验证后，将本文件恢复为 `IDLE`；经用户确认后把 change 移入 `openspec/changes/archive/`。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
