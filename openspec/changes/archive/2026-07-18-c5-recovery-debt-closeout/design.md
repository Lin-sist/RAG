# Design: C5 Recovery Debt Closeout

## Current Gaps

1. `IndexTaskReconciliationCoordinator` 同步循环，`concurrency` 与 `heartbeatSeconds` 没有形成持续执行语义。
2. claim 会增加 attempt，但恢复失败只立即 release；`next_attempt_at` 未写，达到上限后没有稳定终态。
3. legacy document 没有 ledger 时不会被现有 task scan 发现。
4. `DocumentIndexingServiceImpl.finalizeIndex` 跨多个 service 调用更新 chunks、document、knowledge base count 与 task，重试窗口可能重复计数。
5. C5b 验证主要是 mock/H2 单元路径，缺真实 MySQL/Redis 竞争与重启证据。

## Target Flow

```text
bounded scan
  -> legacy scan: quarantine document only, no task adoption
  -> durable task CAS claim (DB time)
  -> VECTOR_IN_FLIGHT: RECONCILIATION_REQUIRED, operation=0
  -> resume disabled: release without attempt storm
  -> resume enabled:
       start periodic heartbeat
       assert lease before new vector mutation
       success -> transactional DB finalize -> terminal
       retryable failure -> DB-time exponential backoff
       max attempt -> FAILED/TERMINAL
```

## Transaction Boundary

新增独立 finalization service，使用 Spring `@Transactional` 包住 document row lock、chunks-if-absent、contentHash/chunkCount/status、knowledge-base document count 与 ledger completion。keyword upsert 和 durable input delete 保持事务外幂等副作用；失败可重试，但 SQL 事实不部分提交。

## Safety

- legacy input 不删除、不解析、不 embedding、不 vector replay。
- heartbeat 失败或 lease 丢失后，恢复路径在进入 vector mutation 前 fail closed。
- backoff/attempt 使用稳定 failure code，不持久化 raw exception message。
- auto resume 默认继续关闭；测试使用 deterministic stub 和合成数据。

## 决策记录

### 决策 1：legacy 任务如何表达
- **面临的选择**：为 legacy document 合成 ledger；只更新 document 为 `RECONCILIATION_REQUIRED`；继续保持 PENDING/FAILED 不处理。
- **选了哪个 + 为什么**：选择只更新 document 状态；它能稳定暴露人工协调事实，又不会伪造缺失的 task phase 或 mutation 历史。
- **放弃的代价**：合成 ledger 会制造错误恢复依据；不处理会让旧任务永久悬空且无法区分普通失败。

### 决策 2：coordinator 并发模型
- **面临的选择**：继续同步单线程；每轮临时创建线程；使用固定有界 executor。
- **选了哪个 + 为什么**：选择固定有界 executor，容量直接来自 `concurrency`，避免无界线程和配置无效。
- **放弃的代价**：同步模式无法兑现 concurrency；临时线程难以关闭、观测和限制资源。

### 决策 3：lease 如何持续保活
- **面临的选择**：只在恢复前 heartbeat 一次；单纯加长 lease；恢复期间周期 heartbeat 并在副作用前检查 lease。
- **选了哪个 + 为什么**：选择周期 heartbeat + 副作用前检查；它同时覆盖长解析/embedding 和 lease owner 丢失窗口。
- **放弃的代价**：单次 heartbeat 会在长任务中失效；长 lease 只扩大故障检测时间，不能证明 owner 仍存活。

### 决策 4：失败重试如何退避
- **面临的选择**：立即 release；应用内 sleep；把 `next_attempt_at` 持久化为 DB 时间指数 backoff。
- **选了哪个 + 为什么**：选择持久化 DB-time backoff；跨实例一致、重启后仍有效，并与 claim 查询共用事实源。
- **放弃的代价**：立即 release 会形成重试风暴；应用 sleep 在进程退出后丢失且占用 worker。

### 决策 5：attempt exhausted 如何收敛
- **面临的选择**：保持 RUNNING 但永远 claim 失败；进入 RECONCILIATION_REQUIRED；进入稳定 FAILED/TERMINAL。
- **选了哪个 + 为什么**：确定性/可重试失败达到上限后进入 FAILED/TERMINAL；它明确表示自动恢复预算耗尽，停止无限扫描。
- **放弃的代价**：保持 RUNNING 会永久污染扫描；RECONCILIATION_REQUIRED 应保留给 mutation 不确定或事实不一致，不应混淆普通预算耗尽。

### 决策 6：DB finalize 的事务边界
- **面临的选择**：保留多个 service 调用并依赖重试；只给现有 private 方法加注解；抽取独立 Spring transactional finalizer。
- **选了哪个 + 为什么**：选择独立 transactional finalizer；private self-invocation 无法获得 Spring 事务代理，独立 bean 能原子覆盖全部 SQL 事实。
- **放弃的代价**：裸重试可能重复 document count；private `@Transactional` 看似存在但实际不生效。

### 决策 7：document count 如何防重复
- **面临的选择**：每次 finalize 都 `+1`；以 task completed 判断；锁定 document 行并只在非 COMPLETED→COMPLETED 时 `+1`。
- **选了哪个 + 为什么**：选择 document row lock + 状态跃迁；document 是计数归属事实，事务回滚可同时撤销状态与计数。
- **放弃的代价**：无条件 `+1` 会重试重复计数；仅看 task 状态在 task/document 不一致窗口不可靠。

### 决策 8：真实验证边界
- **面临的选择**：继续只用 mock/H2；复用用户常驻服务；使用隔离 Testcontainers MySQL/Redis 与 deterministic stub。
- **选了哪个 + 为什么**：选择隔离 Testcontainers；能验证 Flyway、数据库时间/CAS 与 Redis restart，且不碰用户数据、不产生 provider 费用。
- **放弃的代价**：mock/H2 不能证明 MySQL 方言与竞争；复用常驻服务会污染用户环境并难以复现。

### 决策 9：是否顺带进入 C6
- **面临的选择**：同时改 reranker adapter；只修 C5；把 C5 债务留在 C6 中处理。
- **选了哪个 + 为什么**：选择只修 C5；恢复基础与 RAG 质量实验是不同契约，分开才能保持基线可比和提交清晰。
- **放弃的代价**：混做会让故障恢复改动污染 C6 A/B 归因；留到 C6 会让质量阶段背负 P0 基础债务。

### 决策 10：如何建立 chunk 的数据库唯一事实
- **面临的选择**：只依赖应用层先查后插；以 vector_id 唯一；以 document_id + chunk_index 唯一并在迁移中保留最早记录、清理重复行。
- **选了哪个 + 为什么**：选择 document_id + chunk_index 唯一；它直接表达一个文档位置只能有一个有效分块，并为事务重试提供数据库兜底。
- **放弃的代价**：应用层检查存在并发窗口；vector_id 会随内容契约变化，不能稳定表达同一文档位置的重复事实。迁移不清理历史重复则无法安全建立唯一约束。
