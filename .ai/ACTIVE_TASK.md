# Active Task

## Status

`ACTIVE`

## Active Change

- Change：`2026-07-15-redis-failure-semantics`
- 位置：`openspec/changes/2026-07-15-redis-failure-semantics/`
- 目标：锁定 Redis 重启/不可用时缓存、认证、限流、幂等、查询计数与任务状态的分级故障语义。
- 范围：仅 proposal、design、tasks、`rag-system` spec delta 的规划与后续经批准实现；当前实现审批闸门未通过。
- 非目标：Milvus 故障、索引恢复/重放、Redis 高可用部署、内存 fallback、生产 retry policy 与前端重做。
- 验收入口：`openspec/changes/2026-07-15-redis-failure-semantics/tasks.md`。
- 提交责任：`用户手动提交`；Agent 不暂存、不提交、不 push。

## Last Completed

- Change：`2026-07-15-llm-provider-resilience`
- 位置：`openspec/changes/archive/2026-07-15-llm-provider-resilience/`
- 结果：锁定 LLM provider 有界重试、安全诊断、SSE 首内容边界及失败 cache/history/query count 副作用。
- 实现提交：`db8898a`；治理收口提交：`df2f75b`；提交补录：`d3e86d8`。
- 验收：用户已确认通过；delta 已接受进 `rag-system` baseline，change 已归档。

## Start A New Material Change

1. 在 `openspec/changes/<change-id>/` 创建 `proposal.md`、`design.md`、`tasks.md` 和需要的 spec delta。
2. 将本文件 `Status` 改为 `ACTIVE`，并填写 change id、目标、范围、非目标和验收入口。
3. 按 `AGENTS.md` 执行并持续更新 tasks 与 `.ai/AGENT_LOG.md`。
4. 完成且验证后，将本文件恢复为 `IDLE`；经用户确认后把 change 移入 `openspec/changes/archive/`。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
