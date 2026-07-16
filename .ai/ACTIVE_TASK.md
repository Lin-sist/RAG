# Active Task

## Status

`ACTIVE`

## Active Change

- Change：`2026-07-16-durable-index-inputs`
- 位置：`openspec/changes/2026-07-16-durable-index-inputs/`
- 目标：让已接受的文档索引任务使用应用管理、可校验且跨进程存活的输入，不再依赖任务闭包捕获的系统临时文件。
- 范围：proposal、design、tasks、决策记录与 `rag-system` spec delta 规划草案；用户批准前不修改生产代码、数据库 migration、配置或测试。
- 非目标：孤儿任务扫描、自动重放/恢复、跨存储补偿、对象存储接入、公开 DTO/前端重做。
- 验收入口：`openspec/changes/2026-07-16-durable-index-inputs/tasks.md`。
- 提交责任：`用户手动提交`；Agent 不暂存、不提交、不 push。

## Last Completed

- Change：`2026-07-15-milvus-failure-semantics`
- 位置：`openspec/changes/archive/2026-07-15-milvus-failure-semantics/`
- 结果：锁定默认 Milvus 的稳定故障分类、keyword-only 降级、mutation outcome unknown、生命周期 fail-closed 与安全诊断。
- 实现提交：`545c8e7`。
- 验收：用户已确认通过；delta 已接受进 `rag-system` baseline，change 已归档。

## Start A New Material Change

1. 在 `openspec/changes/<change-id>/` 创建 `proposal.md`、`design.md`、`tasks.md` 和需要的 spec delta。
2. 将本文件 `Status` 改为 `ACTIVE`，并填写 change id、目标、范围、非目标和验收入口。
3. 按 `AGENTS.md` 执行并持续更新 tasks 与 `.ai/AGENT_LOG.md`。
4. 完成且验证后，将本文件恢复为 `IDLE`；经用户确认后把 change 移入 `openspec/changes/archive/`。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
