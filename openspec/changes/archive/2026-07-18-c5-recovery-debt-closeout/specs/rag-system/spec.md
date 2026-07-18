# RAG System Spec Delta: C5 Recovery Debt Closeout

## ADDED Requirements

### Requirement: Legacy 索引任务隔离

系统 SHALL 有界扫描升级前 `PENDING/FAILED + AVAILABLE` 且没有 durable ledger 的 document，并将其表达为 `RECONCILIATION_REQUIRED`。系统 MUST NOT 为该记录合成可恢复 phase，MUST NOT 自动 parse/embed/vector replay，MUST NOT 删除 input。

#### Scenario: Legacy document 没有 ledger

- GIVEN document 为 PENDING 或 FAILED、input AVAILABLE 且没有 document-index ledger
- WHEN reconciliation 扫描该记录
- THEN document status 变为 RECONCILIATION_REQUIRED
- AND 不创建合成 task
- AND parser、embedding、vector mutation 与 input delete 调用次数均为 0

### Requirement: 有界 lease、backoff 与终态

系统 SHALL 使用配置的 concurrency 上限执行 claimed task，并在恢复期间按 heartbeat interval 续租。只有当前未过期 lease owner MAY heartbeat；lease 丢失后系统 MUST 在任何新的 vector mutation 前停止。

恢复失败 SHALL 使用数据库时间持久化有界指数 backoff。attempt 达到 max attempts 后 task SHALL 进入稳定 FAILED/TERMINAL，释放 lease 且不再被协调扫描。failure facts MUST 使用稳定 code，不保存 raw exception message。

#### Scenario: 两个 coordinator 竞争

- GIVEN 两个 coordinator 同时看到同一 task
- WHEN 两者竞争 DB lease
- THEN 至多一个 worker 执行恢复
- AND operation 总调用次数为 1

#### Scenario: 长任务持续 heartbeat

- GIVEN worker 持有 lease 且恢复仍在执行
- WHEN heartbeat interval 到达
- THEN 当前 owner 使用数据库时间延长 lease
- AND 其他 worker 不能接管未过期任务

#### Scenario: 恢复失败进入 backoff

- GIVEN 可重试恢复在 attempt 上限前失败
- WHEN coordinator 记录失败
- THEN next_attempt_at 按数据库时间设置指数 backoff
- AND backoff 到期前任务不能被 claim

#### Scenario: Attempt exhausted

- GIVEN task 的当前 attempt 已达到 max attempts
- WHEN 本次恢复失败
- THEN task 进入 FAILED/TERMINAL
- AND lease 被释放
- AND 后续扫描不再返回该 task

### Requirement: 索引 SQL 收尾严格幂等

系统 SHALL 在单一数据库事务内完成 chunks-if-absent、contentHash、chunkCount、document COMPLETED、knowledge-base document count 与 durable task completion。重复执行同一 VECTOR_CONFIRMED/FINALIZING 收尾 MUST NOT 重复 chunks 或 document count。

keyword index upsert 与 durable input delete 不属于该 SQL 事务；系统 MUST NOT 把 SQL rollback 描述为这些外部副作用已回滚。

#### Scenario: 重复 VECTOR_CONFIRMED 收尾

- GIVEN 同一 task 已完成一次 SQL finalize，但调用方因中断再次执行收尾
- WHEN transactional finalizer 再次运行
- THEN document chunks 只存在一份
- AND knowledge-base document count 只增加一次
- AND task 保持 COMPLETED/TERMINAL

#### Scenario: SQL finalize 中途失败

- GIVEN finalizer 在 document、count 或 task completion 任一步发生数据库异常
- WHEN 事务回滚
- THEN 本次 SQL 变更不部分提交
- AND 后续可按相同 deterministic facts 重试
