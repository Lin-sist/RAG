# Active Task

## Status

`IDLE`

## Active Change

- 当前无 active change。

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
