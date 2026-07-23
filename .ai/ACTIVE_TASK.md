# Active Task

## Status

`IDLE`

当前没有 active OpenSpec change。

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
