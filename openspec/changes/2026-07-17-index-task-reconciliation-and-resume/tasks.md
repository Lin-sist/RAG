# Tasks: C5b Index Task Reconciliation And Safe Resume

## 0. Approval Gate

- [ ] 用户审阅并批准 proposal、design、13 条决策记录、tasks 与 `rag-system` spec delta。
- [ ] 用户确认 MySQL `async_task` 为 durable ledger，Redis 为可重建投影；不引入 RabbitMQ/Kafka。
- [ ] 用户确认 ledger 只保存 typed IDs/phase/lease/contract/safe result，不保存 input key、正文、chunks、文件名或 raw provider message。
- [ ] 用户确认 acceptance 顺序为 durable input/document → DB ledger → Redis projection → schedule → response。
- [ ] 用户确认使用 DB CAS lease/heartbeat，数据库时间决定过期；建议默认 batch=20、concurrency=1、lease=5m、heartbeat=60s、maxAttempts=3。
- [ ] 用户确认只自动恢复 SAFE_PRE_VECTOR 与 VECTOR_CONFIRMED 收尾；VECTOR_IN_FLIGHT/outcome unknown 进入 `RECONCILIATION_REQUIRED`。
- [ ] 用户确认新 C5b 任务采用 contract-versioned deterministic chunk/vector ID，旧 IDs 不迁移、不重写。
- [ ] 用户确认 Redis 正常但 miss/TTL 时回源 DB 并重建投影；Redis outage 继续遵守 C4c HTTP 503。
- [ ] 用户确认 reconciliation 与 resume 分开：自动 resume 默认关闭，启用真实 provider 前另行审批调用量/模型/出站/费用。
- [ ] 用户确认 legacy 无 ledger 的 PENDING/FAILED 不自动采用，统一进入人工协调状态。
- [ ] 用户确认 C5b 有界重试 CLEANUP_PENDING input delete，但不因此触发索引重放。
- [ ] 用户确认首版不新增 force-resume API/UI、不恢复非索引 task、不启用现有本地 MQ 模板。
- [ ] 用户确认提交责任为 `用户手动提交`，或另行明确授权 `Agent 提交`。

> 当前仅为规划草案。所有 approval gate 未通过前，不修改生产 Java、migration、配置或测试。

## 1. Inventory And RED Tests

- [ ] 固化真实 upload → Redis submit → JVM future/closure → status TTL 的 consumer/lifecycle inventory。
- [ ] 固化 `async_task` 表存在但生产 Java 零读写、MQ 模板不承载真实索引的事实。
- [ ] 固化 document/input/task 状态组合及 C4c/C4d/C5a 禁止伪成功/未知重放边界。
- [ ] 为 ledger acceptance ordering、Redis initial projection failure 与假 task prevention 添加 RED 测试。
- [ ] 为 DB lease CAS、heartbeat、expiry、两个 coordinator 竞争和数据库时间边界添加 RED 测试。
- [ ] 为 SAFE_PRE_VECTOR、VECTOR_IN_FLIGHT、VECTOR_CONFIRMED、contract mismatch 添加恢复 RED 测试。
- [ ] 为 Redis miss/TTL DB fallback、owner preservation 与 projection rebuild 添加 RED 测试。
- [ ] 为 legacy task、CLEANUP_PENDING、attempt exhausted 与 sensitive payload/log 添加 RED 测试。

## 2. Durable Task Ledger

- [ ] 新增前向 migration，兼容既有 nullable async_task 行并添加 document/owner/phase/lease/attempt/contract/mutation 字段与索引。
- [ ] 新增 ledger entity/mapper/repository 与稳定 state/phase/failure types，不把 DB raw message 暴露客户端。
- [ ] 建立每个新 document index 的稳定 taskId 与单一 durable ledger 关联；resume attempt 不更换 taskId。
- [ ] 上传 acceptance 改为 durable input/document → ledger → Redis projection → schedule → response。
- [ ] ledger 或 Redis initial write 已知失败时 operation=0，不返回假 task，并保留/清理明确事实。
- [ ] task durable result 使用 sanitized summary，不持久化 `ProcessResult.rawContent`、chunks 或 storage key。

## 3. Claim, Checkpoint And Safe Resume

- [ ] 实现基于 DB 条件 UPDATE 的 claim/lease/heartbeat/release，使用数据库时间和 optimistic version。
- [ ] 实现 bounded scan、batch/concurrency/backoff/max-attempt 配置，避免无界恢复风暴。
- [ ] 实现 durable phase checkpoints：ACCEPTED、SAFE_PRE_VECTOR、VECTOR_IN_FLIGHT、VECTOR_CONFIRMED、FINALIZING 与终态。
- [ ] 为新任务生成 contract-versioned deterministic chunk/vector IDs，持久化 content/config/chunk count facts。
- [ ] SAFE_PRE_VECTOR lease 过期后可按相同 input/contract 重新 parse/embed，事实不一致 fail closed。
- [ ] VECTOR_IN_FLIGHT/outcome unknown 永不自动 vector replay，转 `RECONCILIATION_REQUIRED` 且 vector calls=0。
- [ ] VECTOR_CONFIRMED 恢复只重建/校验 deterministic facts并完成 DB/keyword/input 收尾，vector upsert calls=0。
- [ ] DB finalize 对 chunks/contentHash/document status/document count 保持事务性或可证明幂等，避免重复计数。

## 4. Reconciliation Lifecycle

- [ ] MySQL ledger 成为 document index task 权威；Redis 正常 miss/TTL 时回源并重建安全投影。
- [ ] Redis outage 继续返回 C4c stable 503，不伪装成 DB fallback 成功或 Redis 健康。
- [ ] 保持 task ownerId 与现有 TaskController 授权语义，禁止跨用户查询恢复任务。
- [ ] legacy 无 ledger/phase 的 PENDING/FAILED + AVAILABLE 标记 `RECONCILIATION_REQUIRED`，不自动采用。
- [ ] CLEANUP_PENDING 有界重试 delete；成功转 CLEANED，失败保留 pending，parser/embedding/vector calls=0。
- [ ] deterministic failure、cancel、attempt exhausted 进入稳定终态，不无限循环。
- [ ] reconciliation 可运行但 `resume-enabled=false` 为默认；真实 provider resume 启用前完成单独预算授权。
- [ ] 不新增 force-resume endpoint/UI、RabbitMQ/Kafka、非 document task recovery 或 exactly-once 声明。

## 5. Verification And Closeout

- [ ] 运行 ledger/repository/service/coordinator 聚焦测试与双 coordinator 并发测试。
- [ ] 运行真实 MySQL 8.0.36 V1→新版本、V7→新版本、legacy async_task migration 与 lease CAS 验证。
- [ ] 运行 Redis projection miss/TTL、outage fail-closed 与 restart recovery 集成。
- [ ] 运行 crash-window integration：safe pre-vector resume、vector in-flight quarantine、vector-confirmed finalize。
- [ ] 运行现有 C3 happy path、C4c Redis fault、C4d Milvus fault 回归。
- [ ] 运行 `mvn -q test`。
- [ ] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`。
- [ ] 运行 SensitiveLogs 与 `git diff --check`。
- [ ] 记录真实 provider/embedding/rerank/judge/ask 调用量；计划实现验证目标为全部 0。
- [ ] 扫描公开 DTO、前端、retrieval/generation/eval、对象存储、新依赖与受保护路径无越界改动。
- [ ] 更新 tasks、`.ai/ACTIVE_TASK.md`、project/roadmap/architecture 与追加式 `.ai/AGENT_LOG.md`。
- [ ] 用户完成实现验收后，才接受 spec delta、恢复 `IDLE` 并归档 change。

## Commit Responsibility

当前为 `用户手动提交`。Agent 不执行 `git add`、`git commit`、push、PR、部署或发布。
