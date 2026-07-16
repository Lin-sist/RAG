# Active Task

## Status

`ACTIVE`

## Active Change

- Change：`2026-07-15-milvus-failure-semantics`
- 位置：`openspec/changes/2026-07-15-milvus-failure-semantics/`
- 目标：锁定默认 Milvus 在检索、索引 mutation、删除、统计和重启窗口中的失败、部分降级与 outcome unknown 语义。
- 范围：已批准的 proposal、design、tasks、决策记录与 `rag-system` spec delta；生产实现与验证已完成，当前等待用户验收，验收前不接受进 baseline。
- 非目标：Qdrant/Elasticsearch 故障契约、Milvus HA/容量调优、索引恢复/重放/孤儿协调、公开 DTO/schema/前端重做。
- 验收入口：`openspec/changes/2026-07-15-milvus-failure-semantics/tasks.md`。
- 提交责任：`用户手动提交`；Agent 不暂存、不提交、不 push。

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
