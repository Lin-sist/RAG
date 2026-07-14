# Active Task

## Status

`IDLE`

## Active Change

无。

## Last Completed

- Change：`2026-07-14-database-backed-authentication`
- 位置：`openspec/changes/archive/2026-07-14-database-backed-authentication/`
- 结果：登录与 refresh 改以数据库用户和角色为事实源；新增默认关闭的一次性管理员 bootstrap，并通过 V6 前向迁移精确隔离历史 known seed。
- 实现提交：`9c63051d8863786f04d8c0ccdb9fd34743d6311e`。
- 验收：用户已确认通过；delta 已接受进 `openspec/specs/rag-system/spec.md`。

## Start A New Material Change

1. 在 `openspec/changes/<change-id>/` 创建 `proposal.md`、`design.md`、`tasks.md` 和需要的 spec delta。
2. 将本文件 `Status` 改为 `ACTIVE`，并填写 change id、目标、范围、非目标和验收入口。
3. 按 `AGENTS.md` 执行并持续更新 tasks 与 `.ai/AGENT_LOG.md`。
4. 完成且验证后，将本文件恢复为 `IDLE`；经用户确认后把 change 移入 `openspec/changes/archive/`。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
