# Active Task

## Status

`ACTIVE`

## Active Change

- Change：`2026-07-18-c5-recovery-debt-closeout`
- 位置：`openspec/changes/2026-07-18-c5-recovery-debt-closeout/`
- 目标：闭环 C5 legacy 隔离、持续 lease heartbeat、有界 backoff/attempt 终态、SQL finalize 严格幂等与真实依赖验证，为 C6 规划清空 P0 恢复债务。
- 范围：只修复 C5 已登记债务；真实 provider 调用为 0，auto resume 默认关闭。
- 非目标：C6 reranker、retrieval/generation/evaluation 改动、force-resume API/UI、MQ、对象存储或 exactly-once。
- 验收入口：`openspec/changes/2026-07-18-c5-recovery-debt-closeout/tasks.md`。
- 提交责任：`用户手动提交`；Agent 不暂存、不提交、不 push。

## Last Completed

- Change：`2026-07-17-index-task-reconciliation-and-resume`
- 位置：`openspec/changes/archive/2026-07-17-index-task-reconciliation-and-resume/`
- 结果：建立 document index durable ledger、稳定 taskId、数据库 claim 边界、phase checkpoint、保守 safe resume、Redis durable fallback 与 cleanup reconciliation。
- 验收：用户已确认提交并要求归档；已实现范围的 delta 接受进 `rag-system` baseline，未完成保证转入技术债。

## Start A New Material Change

1. 在 `openspec/changes/<change-id>/` 创建 `proposal.md`、`design.md`、`tasks.md` 和需要的 spec delta。
2. 将本文件 `Status` 改为 `ACTIVE`，并填写 change id、目标、范围、非目标和验收入口。
3. 按 `AGENTS.md` 执行并持续更新 tasks 与 `.ai/AGENT_LOG.md`。
4. 完成且验证后，将本文件恢复为 `IDLE`；经用户确认后把 change 移入 `openspec/changes/archive/`。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
