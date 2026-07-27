# Active Task

## Status

`ACTIVE`

## Change

- Change：`tenant-data-plane-enforcement`
- 阶段：C13b data-plane enforcement 实现与指定测试已收口，等待用户验收；不接受 baseline、不归档、不置 `IDLE`
- 位置：`openspec/changes/tenant-data-plane-enforcement/`
- 类型：Type C（SQL/API/permission、vector、cache/task/history 跨数据面 tenant enforcement）
- 提交责任：用户已于 2026-07-27 授权 `Agent 提交`；本轮只提交计划内 C13b 文件，不 push、不创建 PR、不部署。

## Planning Artifacts

- `proposal.md`：问题、范围、非目标、能力分类、契约、风险与验收证据。
- `design.md`：V11、显式 identity、权限、Milvus-first、legacy vector maintenance/readiness、Redis/task/recovery 与 17 条决策记录。
- `tasks.md`：事前闸门与 migration→SQL/API→task/cache→RAG/vector→maintenance→full gates 的可验证切片。
- `specs/rag-system/spec.md`：6 requirements / 18 scenarios 草案。

## Current Gate

1. 用户已批准 proposal、15 条 decisions、6/18 spec delta 与 tasks；2026-07-27 又授权 Agent 对当前阶段实现分段提交。
2. 已形成三段实现提交：V11 migration/entity、Redis identity/query scope 基础、SQL/API/permission 与 task/recovery/input 数据面；治理检查点单独提交。
3. Docker Desktop 恢复后，MySQL 8.0.36 下 `TenantDataPlaneMigrationMySqlTest` 为 8/0/0/0，`C5RecoveryMySqlTest` 为 5/0/0/0，均不再 skip；task/recovery/input/projection 既有组合证据仍为 68/0/0/0。
4. QA/embedding cache v2、RAG/keyword scope、Milvus adapter contract、V12 readiness 与默认关闭的 shadow maintenance 已实现；runtime `VectorStore` 不再暴露无租户 legacy read，旧 service lookup 兼容入口 fail closed。
5. 用户已确认 design 决策 16=A：采用 tenant-aware shadow collection，复制既有 vector/content，全量审计后原子切换 SQL mapping/readiness；真实 Milvus 当前只授权只读盘点，任何真实写入、迁移或切换仍需另行授权。
6. 最终 `mvn -q test` 仍仅命中既有 `GenAiTracingConfigurationTest` 的不可用 collector 时序断言（rag-admin 214 tests / 1 failure / 0 errors / 2 skipped），该单项独立复跑通过；全仓仍记为非 GREEN。Milvus 2.3.4 合成 contract 2/0/0/0、Python 159 tests、SensitiveLogs 与静态范围门禁通过。
7. 下一闸门只有用户验收；验收前不接受 delta、不归档、不把 `.ai/ACTIVE_TASK.md` 置为 `IDLE`。真实 Milvus shadow copy/readiness switch 仍需单独授权，当前为 `SKIPPED`。

## Execution Boundaries

- 不修改 V1-V10、accepted baseline、依赖或受保护本地配置；生产代码与测试只按已批准 C13b tasks 小步修改。
- 不执行真实 Milvus/Qdrant/Elasticsearch maintenance，不调用 embedding/rerank/ask/generation/judge/LLM/provider。
- 不修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`。
- 不宣称租户隔离成立；C14 通过前不开放第二业务 tenant、MCP 或 Router。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
