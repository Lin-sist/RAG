# Active Task

## Status

`ACTIVE`

## Active Change

- Change：`2026-07-23-judge-calibration-and-status-semantics`
- 位置：`openspec/changes/2026-07-23-judge-calibration-and-status-semantics/`
- 目标：为可选 LLM judge 建立版本化 contract、独立人工 gold calibration corpus/evidence，并分离 objective、judge 与全局 Report status/comparison safety。
- 范围：24 条 faithful×relevant 四象限静态校准集、manifest/validator/calibration runner、strict score parser、tracked judge identity、canary/full agreement 与 repeat stability、normal eval channel status 和兼容性；当前仅完成规划，不修改实现或 baseline。
- 非目标：不做 C10 quality threshold/exit gate，不默认开启 judge，不改 v1/v2 dataset、C9a objective formulas、Java/API、生产 prompt/citation/retrieval/rerank/no-answer、前端或 SSE，不运行真实 judge calibration。
- 验收入口：`proposal.md` 的 planning/offline/live/closeout gates、`design.md` 的 15 条决策记录、`tasks.md` 与 `specs/evaluation/spec.md` 的 4 requirements / 12 scenarios。
- 提交责任：`用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- 外部调用：planning 与 offline implementation 默认均为 0；live canary 最多 4 judge calls、full 最多 72、合计最多 76，必须另行固定 provider/model/出站/费用/限流并授权。

## Last Completed

- Change：`2026-07-23-claim-evidence-objective-metrics`
- 位置：`openspec/changes/archive/2026-07-23-claim-evidence-objective-metrics/`
- 结果：完成固定 `claim-lexical-v1` 的确定性 claim splitter、provenance-valid returned citation evidence、exact / `0.70` claim-token alignment、逐 claim attribution、aggregate support rate、局部完整性状态与报告身份。
- 验收：用户已验收 claim contract、evidence attribution、aggregate/status、兼容性与结论边界；4 个 requirements / 12 个 scenarios 已接受进 `evaluation` baseline，change 已归档。C9a 不代表 C9b judge calibration、semantic faithfulness、C10 quality gate 或真实 150 条 generation evidence 已完成。

## Start A New Material Change

1. 在 `openspec/changes/<change-id>/` 创建 `proposal.md`、`design.md`、`tasks.md` 和需要的 spec delta。
2. 将本文件 `Status` 改为 `ACTIVE`，并填写 change id、目标、范围、非目标和验收入口。
3. 按 `AGENTS.md` 执行并持续更新 tasks 与 `.ai/AGENT_LOG.md`。
4. 完成且验证后，将本文件恢复为 `IDLE`；经用户确认后把 change 移入 `openspec/changes/archive/`。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
