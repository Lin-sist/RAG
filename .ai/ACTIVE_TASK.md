# Active Task

## Status

`ACTIVE`

## Active Change

- Change：`2026-07-17-index-task-reconciliation-and-resume`
- 位置：`openspec/changes/2026-07-17-index-task-reconciliation-and-resume/`
- 目标：为文档索引建立 durable task ledger、孤儿任务协调、跨实例 lease 与安全续跑边界，使进程中断不再留下永久悬空任务。
- 范围：用户已批准 proposal、design、13 条决策记录、tasks 与 `rag-system` spec delta；现按 TDD 进入 durable ledger、lease/checkpoint、安全 resume 与 reconciliation 实现，验收前不接受进 baseline。
- 非目标：自动重放 vector outcome unknown、强制 resume API/UI、RabbitMQ/Kafka、对象存储、非文档索引任务恢复、分布式事务或 exactly-once 承诺。
- 验收入口：`openspec/changes/2026-07-17-index-task-reconciliation-and-resume/tasks.md`。
- 提交责任：`用户手动提交`；Agent 不暂存、不提交、不 push。

## Last Completed

- Change：`2026-07-16-durable-index-inputs`
- 位置：`openspec/changes/archive/2026-07-16-durable-index-inputs/`
- 结果：让已接受的文档索引任务使用应用管理、原子发布、可校验且跨进程存活的 durable input，并锁定显式输入生命周期与部署边界。
- 验收：用户已确认通过；delta 已接受进 `rag-system` baseline，change 已归档。

## Start A New Material Change

1. 在 `openspec/changes/<change-id>/` 创建 `proposal.md`、`design.md`、`tasks.md` 和需要的 spec delta。
2. 将本文件 `Status` 改为 `ACTIVE`，并填写 change id、目标、范围、非目标和验收入口。
3. 按 `AGENTS.md` 执行并持续更新 tasks 与 `.ai/AGENT_LOG.md`。
4. 完成且验证后，将本文件恢复为 `IDLE`；经用户确认后把 change 移入 `openspec/changes/archive/`。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
