# Active Task

## Status

`IDLE`

当前没有活动 change。新重大变更必须重新完成 readiness 与 OpenSpec 事前闸门。

## Last Completed

- Change：`2026-07-23-judge-calibration-and-status-semantics`
- 位置：`openspec/changes/archive/2026-07-23-judge-calibration-and-status-semantics/`
- 结果：完成共享 `rag-judge-v1` contract、strict parser、24 条四象限静态 calibration corpus/validator/runner，以及 objective/judge/global status 与 per-channel comparison safety。
- 验收：用户已验收 offline corpus/contract/status/comparison 实现和结论边界；4 requirements / 12 scenarios 已接受进 `evaluation` baseline，change 已归档。Live calibration 未授权并按 `SKIPPED` 收口，因此 C9b 不确认真实 judge agreement、production faithfulness、通用可靠性、默认开启 judge 或 C10 quality gate。

## Start A New Material Change

1. 在 `openspec/changes/<change-id>/` 创建 `proposal.md`、`design.md`、`tasks.md` 和需要的 spec delta。
2. 将本文件 `Status` 改为 `ACTIVE`，并填写 change id、目标、范围、非目标和验收入口。
3. 按 `AGENTS.md` 执行并持续更新 tasks 与 `.ai/AGENT_LOG.md`。
4. 完成且验证后，将本文件恢复为 `IDLE`；经用户确认后把 change 移入 `openspec/changes/archive/`。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
