# Active Task

## Status

`IDLE`

## Active Change

无。

## Last Completed

- Change：`2026-07-15-redis-failure-semantics`
- 位置：`openspec/changes/archive/2026-07-15-redis-failure-semantics/`
- 结果：锁定 Redis optional fail-open、关键状态 fail-closed、幂等 outcome unknown、异步任务状态事实源及安全诊断。
- 实现提交：`ae0fbd9`。
- 验收：用户已确认通过；delta 已接受进 `rag-system` baseline，change 已归档。

## Start A New Material Change

1. 在 `openspec/changes/<change-id>/` 创建 `proposal.md`、`design.md`、`tasks.md` 和需要的 spec delta。
2. 将本文件 `Status` 改为 `ACTIVE`，并填写 change id、目标、范围、非目标和验收入口。
3. 按 `AGENTS.md` 执行并持续更新 tasks 与 `.ai/AGENT_LOG.md`。
4. 完成且验证后，将本文件恢复为 `IDLE`；经用户确认后把 change 移入 `openspec/changes/archive/`。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
