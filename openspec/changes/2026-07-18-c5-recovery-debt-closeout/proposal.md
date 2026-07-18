# Proposal: C5 Recovery Debt Closeout

## Why

C5a/C5b 已建立 durable input、durable task ledger 与保守 resume，但归档时明确留下四类 P0 债务：legacy 无 ledger 任务未隔离、coordinator 缺持续 heartbeat/backoff/attempt exhausted、DB finalize/document count 未证明严格幂等，以及真实 MySQL/Redis/双 coordinator/crash-window 验证缺失。这些缺口会让 C6 reranker 工作建立在不完整的索引恢复基础上。

## User Story

作为项目维护者，我希望 C5 索引恢复链路在进入 C6 前具备稳定的 legacy 隔离、有界重试、lease 保活、幂等 SQL 收尾和真实依赖验证，从而不再把这些基础设施风险带入质量评测阶段。

## Scope

- legacy `PENDING/FAILED + AVAILABLE` 且没有 durable ledger 的 document 转为稳定 `RECONCILIATION_REQUIRED`，保留 input，parser/embedding/vector 调用为 0。
- coordinator 使用配置的 concurrency，并在恢复期间持续 heartbeat；lease 丢失后在任何新的 vector mutation 前停止。
- 恢复失败按数据库时间设置指数 backoff；达到 max attempts 后进入稳定 `FAILED/TERMINAL`，不再扫描。
- VECTOR_CONFIRMED/FINALIZING 的 SQL chunks/contentHash/document status/document count/task completion 在单一事务中收尾；document count 至多增加一次。
- 补真实 MySQL 8.0.36 migration/lease 竞争、Redis restart fallback 与 phase crash-window 验证。
- 同步 baseline、project/architecture/roadmap、tasks 与追加式 AGENT_LOG，完成后归档并恢复 `IDLE`。

## Out Of Scope

- 不自动重放 `VECTOR_IN_FLIGHT` 或 `VECTOR_OPERATION_OUTCOME_UNKNOWN`。
- 不默认开启会调用 embedding/provider 的 auto resume；不进行真实 provider 调用。
- 不新增 force-resume API/UI、RabbitMQ/Kafka、对象存储、分布式事务或 exactly-once 声明。
- 不修改 retrieval、generation、citation、no-answer、rerank、prompt 或 evaluation 指标。
- 不进入 C6 NVIDIA reranker adapter 或 A/B 评测。

## Verification

- 每个行为按 TDD 单个 RED→GREEN 推进。
- 聚焦 ledger/coordinator/finalizer/service 测试。
- MySQL 8.0.36 验证 V1→最新、V7→最新、legacy async_task 与两个 claimant。
- Redis container restart 验证 outage fail-closed 与 restart 后 durable fallback。
- `mvn -q test`、Python 33 项、SensitiveLogs、`git diff --check`。
- 真实 embedding/rerank/judge/ask/LLM/provider 调用量为 0。

## Commit Responsibility

`用户手动提交`。Agent 不执行 `git add`、`git commit`、push、PR、部署或发布。
