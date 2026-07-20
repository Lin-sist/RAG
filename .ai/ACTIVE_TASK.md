# Active Task

## Status

`IDLE`

## Active Change

- 当前无 active change。

## Last Completed

- Change：`2026-07-20-reranker-ab-evaluation`
- 位置：`openspec/changes/archive/2026-07-20-reranker-ab-evaluation/`
- 结果：完成严格 identity、provider coverage、sample pairing 与 latency 分口径的 retrieval-only reranker A/B；full `R=3,W=3` 六个 measured runs comparison=`COMPARABLE`。
- 验收：用户已验收 C7 full evidence 与结论边界；4 个 requirements / 11 个 scenarios 已接受进 `evaluation` baseline，change 已归档。当前默认 provider 继续保持 heuristic；如需切换必须另立 Type C change。

## Start A New Material Change

1. 在 `openspec/changes/<change-id>/` 创建 `proposal.md`、`design.md`、`tasks.md` 和需要的 spec delta。
2. 将本文件 `Status` 改为 `ACTIVE`，并填写 change id、目标、范围、非目标和验收入口。
3. 按 `AGENTS.md` 执行并持续更新 tasks 与 `.ai/AGENT_LOG.md`。
4. 完成且验证后，将本文件恢复为 `IDLE`；经用户确认后把 change 移入 `openspec/changes/archive/`。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
