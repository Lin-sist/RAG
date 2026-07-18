# Design: C5b Index Task Reconciliation And Safe Resume

## Context

C5a 只持久化了“任务要处理的输入”，没有持久化“任务如何继续执行”。当前 upload 在 document 建立后调用 `RedisAsyncTaskManager.submit`，Redis 先写 PENDING，再把 closure 放进 JVM executor；任务状态 24 小时后过期，future/closure 随进程消失。`document.status` 直到最终成功/失败才更新，不能表达 vector mutation 是否已发送。

V2 的 `async_task` 表目前是未使用 schema；`rag-common.async.mq` 是未接入真实上传的模板且默认 consumer 不做索引。C5b 不能在这些名字之上假设 durability，而要把真实生产编排迁入可验证的 durable ledger。

## Goals

- 一个已接受的 document index task 拥有稳定 taskId 与数据库 ledger。
- Redis 是低延迟投影，不是唯一事实源；TTL/miss 不造成已接受任务误报不存在。
- 多实例通过 DB lease 至多一个 active owner，lease 可过期、续租和审计。
- 只恢复可证明安全的 phase；vector mutation 不确定时 fail closed。
- C5a durable input 与 CLEANUP_PENDING 被纳入协调，但不扩大为对象存储或分布式事务。
- provider 调用批次、并发与启用开关明确，测试不产生真实模型调用。

## Non-goals

- exactly-once、全自动外部副作用对账或强制重放。
- RabbitMQ/Kafka/Redis Streams、新基础设施或新增依赖。
- 非文档索引 task、恢复 UI、公开 force-resume endpoint。
- 修改 retrieval/generation/citation/no-answer/judge 指标或生产算法。

## Confirmed Data Flow Today

```text
durable input + document(PENDING)
  -> Redis PENDING (24h TTL)
  -> JVM CompletableFuture + captured typed identifiers
  -> parse/embed/vector/db finalize
  -> Redis terminal projection
```

进程退出会丢失 executor 与 closure；Redis PENDING/RUNNING 不能证明任务仍在执行，Redis miss 也不能证明任务从未被接受。

## Proposed Architecture

```text
Upload acceptance
  -> durable input/document
  -> MySQL async_task ledger (ACCEPTED, stable taskId)
  -> Redis status projection
  -> local executor claim

Coordinator (startup + bounded schedule)
  -> scan expired/non-terminal ledger + CLEANUP_PENDING
  -> DB compare-and-set lease
  -> classify phase
       SAFE_PRE_VECTOR     -> optional safe resume
       VECTOR_CONFIRMED    -> finalize without vector upsert
       VECTOR_IN_FLIGHT    -> RECONCILIATION_REQUIRED
       legacy/mismatch     -> RECONCILIATION_REQUIRED
  -> update DB ledger first, then Redis projection
```

## Durable Ledger

建议在既有 `async_task` 上做前向 migration，而不是创建第二套任务表。字段草案：

- identity：`task_id`、`task_type=DOCUMENT_INDEX`、`document_id`、`owner_id`；
- lifecycle：`status`、`execution_phase`、`attempt_count`、`failure_code`；
- claim：`lease_owner`、`lease_until`、`heartbeat_at`、`next_attempt_at`；
- resume contract：`index_contract_version`、`chunk_size`、`chunk_overlap`、`prepared_content_hash`、`prepared_chunk_count`；
- mutation facts：`vector_started_at`、`vector_confirmed_at`；
- auditing：既有 created/updated/version，必要索引覆盖 status/phase/lease/next attempt。

`payload/result/error_message` 不保存输入 bytes、storage key、正文、chunks、文件名、标题或 raw provider message。taskId 在同一逻辑任务的 resume attempt 间保持不变；attempt 递增而不是创建客户端不可追踪的新 taskId。

## State And Phase Model

业务 status 与执行 phase 分开：

| Status | Phase | 含义 | 自动动作 |
|---|---|---|---|
| PENDING | ACCEPTED | ledger 已建立，尚未开始副作用 | 可 claim |
| RUNNING | SAFE_PRE_VECTOR | parse/embed 可重做，尚未发送 vector mutation | lease 过期后可安全 resume |
| RUNNING | VECTOR_IN_FLIGHT | mutation 已开始但未确认 | 禁止自动 replay，转协调状态 |
| RUNNING | VECTOR_CONFIRMED | vector 已明确成功，DB 收尾未确认 | 只做可验证 finalize，不再 upsert |
| RUNNING | FINALIZING | DB transaction/keyword/input cleanup 收尾 | 按事实继续或协调 |
| COMPLETED | COMPLETED | document/index/ledger 完成 | 只处理 cleanup pending |
| FAILED | FAILED | 确定性失败或预算耗尽 | 不自动无限重试 |
| CANCELLED | CANCELLED | 用户取消且未进入不确定 mutation | 不 resume |
| RECONCILIATION_REQUIRED | AMBIGUOUS | outcome unknown、legacy 或事实冲突 | 只报告，不自动 mutation |

phase 更新必须在对应副作用之前/之后写入 durable ledger。`VECTOR_IN_FLIGHT -> VECTOR_CONFIRMED` 之间仍存在无法原子消除的窗口；该窗口的进程中断按 ambiguous 处理，不声称安全重放。

## Reproducible Index Identity

为让 SAFE_PRE_VECTOR 重做和 VECTOR_CONFIRMED 收尾重建相同内部 IDs，建议只对新 C5b 任务采用 deterministic vector ID：

```text
sha256(indexContractVersion + documentId + preparedContentHash + chunkIndex + chunkContentHash)
```

ledger 同时持久化 `index_contract_version`、chunk size/overlap、prepared content hash 与 chunk count。恢复时重新打开 C5a input、按固定 contract 解析，并验证全部事实；任何版本、hash、chunk count 或 ID 不一致都进入 `RECONCILIATION_REQUIRED`。旧 document/vector IDs 不迁移、不重写。

## Claim And Lease

- coordinator 以小批次查询 `next_attempt_at <= now` 且 lease 为空/过期的非终态记录；
- 使用单条条件 UPDATE/CAS 设置 lease owner、lease until 并增加 attempt；
- active worker 周期 heartbeat，只能更新自己持有且未过期的 lease；
- 完成、失败、取消或协调状态清空 lease；
- 多实例测试必须证明同一 task 同一时刻只有一个 owner 进入 task operation；
- 时间比较以数据库时间为准，避免应用节点时钟漂移决定归属。

用户已批准初始默认：scan batch 20、concurrency 1、lease 5 分钟、heartbeat 60 秒、max attempts 3、指数 backoff；这些仍保持可配置，不写死为不可变契约。

## Redis Projection And Public Polling

MySQL ledger 是 document index task durable source of truth；Redis 保持现有低延迟投影和 outage fail-closed 行为。查询流程建议：

1. Redis 正常且命中：返回投影；
2. Redis 正常但 miss：按 taskId 查询 DB ledger；存在则返回 sanitized durable status 并 best-effort 重建 Redis；
3. Redis 依赖故障：继续遵守 C4c HTTP 503，不把 DB fallback 伪装成 Redis 健康；
4. DB 与 Redis 冲突：DB ledger 决定 durable phase，记录安全 drift diagnostics，刷新投影；
5. ownerId 从 ledger 恢复，保持现有 task ownership 检查。

DB ledger 结果只保存/返回安全 summary，不持久化 `ProcessResult.rawContent`。

## Reconciliation And Resume Policy

reconciliation scan 与 resume execution 分开配置：

- reconciliation 可扫描/分类/续租过期/CLEANUP_PENDING，不产生 provider 调用；
- `resume-enabled=false` 为建议默认，开启后才允许 SAFE_PRE_VECTOR 重新 parse/embed；
- 每轮候选、并发、attempt 与 backoff 有界；
- 启用真实 embedding provider 前必须完成任务数/预计 chunks/模型/出站/费用授权；
- VECTOR_CONFIRMED finalize 不再调用 vector upsert，且应避免不必要的 embedding；
- deterministic parser/embedding/vector failure 不自动循环，按 stable code 与预算进入 FAILED。

## Legacy And Existing Placeholder Components

- C5b 上线前创建、但没有 ledger/phase 的 PENDING/FAILED document，即使 input AVAILABLE 也不能证明 mutation 未发生；首版标记 legacy reconciliation required，不自动采用。
- 旧 COMPLETED document 继续按 C5a 兼容读取，不创建恢复任务。
- 现有 `async_task` 旧行若 task type/owner/document facts 不完整，migration 保持 nullable，不自动解释为 document recovery ledger。
- `rag-common.async.mq` 模板继续不参与真实恢复；若未来接 RabbitMQ/Kafka，应另立 change，不能在 C5b 顺手启用。

## Cleanup Reconciliation

document COMPLETED 且 input state 为 CLEANUP_PENDING 时，coordinator 可执行幂等 `IndexInputStore.delete`：

- DELETED/ALREADY_MISSING -> CLEANED；
- FAILED/异常 -> 保持 CLEANUP_PENDING 并按 cleanup backoff 重试；
- 不调用 parser、embedding、vector 或 generation；
- document delete 已有 fail-closed 语义保持不变。

## Failure Matrix

| Window | Durable fact | C5b action |
|---|---|---|
| ledger 创建前失败 | 无 task acceptance | 不返回 taskId，按 C5a 清理 |
| ledger 已建、Redis PENDING 失败 | ledger REJECTED/FAILED | 不启动 operation，不返回假 task |
| ACCEPTED 后进程退出 | safe phase | lease 到期后可 resume |
| parse/embed 时退出 | SAFE_PRE_VECTOR | 可按固定 contract 重做 |
| vector 调用已开始、未确认 | VECTOR_IN_FLIGHT | RECONCILIATION_REQUIRED，vector calls=0 |
| vector 明确成功、DB 前退出 | VECTOR_CONFIRMED | 重建 deterministic facts，只 finalize |
| DB finalize transaction 失败 | VECTOR_CONFIRMED/FINALIZING | 重试幂等 DB finalize，不 upsert vector |
| Redis 投影丢失/TTL | DB ledger 存在 | DB 返回状态并重建投影 |
| lease owner 消失 | lease expired | 其他实例按 CAS claim |
| contract/input mismatch | unsafe | RECONCILIATION_REQUIRED |

## Security And Observability

允许 diagnostics：taskId 的安全短标识、documentId、phase、attempt、lease category、stable failure code、dependency、operation、errorType、traceId。禁止记录 storage root/key、原文件名、标题、正文、chunks、embedding input、vector payload、provider raw message、credential、Redis value 或 DB payload。

建议 metrics：candidate count、claimed count、resumed count、reconciliation-required count、lease contention、lease expiry、cleanup retry、attempt exhausted、Redis projection rebuild；不把业务内容作为 label。

## Verification Strategy

- repository tests：ledger CRUD、status/phase transition、CAS lease、heartbeat、attempt/backoff、sanitized payload；
- service tests：acceptance ordering、Redis initial write failure、safe pre-vector resume、vector in-flight quarantine、vector-confirmed finalize、cleanup pending；
- concurrency tests：两个 coordinator 竞争同一 task，operation budget=1；
- restart-style integration：实例 A 建 ledger/进入指定 phase后停止，实例 B 使用同 MySQL/Redis/root 恢复；
- Redis tests：projection miss/TTL 从 DB 重建，Redis outage 仍遵守 C4c 503；
- migration：真实 MySQL 8.0.36 V1→新版本、V7→新版本、旧 async_task nullable compatibility 与索引；
- C3/C4c/C4d 回归：主链路、Redis stop/start、Milvus outcome unknown 不重放；
- complete gates：`mvn -q test`、Python 33 tests、SensitiveLogs、`git diff --check`；真实 provider 0 调用。

## Rollout And Rollback

- migration 只前向添加 nullable 字段/索引并按需扩大 status 长度；旧应用可忽略。
- reconciliation scan 与 auto resume 分开开关；先 observe/classify，再在隔离环境开启 resume。
- rollback 到旧应用时 ledger 保留但不被消费；不得由旧版本删除 C5a input 或重放 ledger。
- 不自动迁移 legacy documents 为 safe-resumable；需要后续显式人工策略。

## 决策记录

### 决策 1：Durable task 的权威事实源

1. **面临的选择**：A. 继续只用 Redis；B. 复用 MySQL `async_task` 为 durable ledger、Redis 为投影；C. 新建 RabbitMQ/Kafka。
2. **选了哪个 + 为什么**：选 B；用户已批准。既有 MySQL schema 可前向扩展，能支持 phase/lease/CAS，且 Redis outage 正是需要协调的场景。
3. **放弃的代价**：A 会继续受 TTL 和执行 closure 丢失影响；C 会新增基础设施、凭据与运维故障面，超出 C5b。

### 决策 2：Ledger 保存什么

1. **面临的选择**：A. 把 MultipartFile/正文/chunks 放入 payload；B. 只存 typed IDs、phase、lease、contract 与安全结果；C. 仅在 document 表加一个 taskId。
2. **选了哪个 + 为什么**：选 B；用户已批准。恢复需要可查询 phase/lease，但输入 bytes 已由 C5a 管理，不应复制敏感内容。
3. **放弃的代价**：A 会放大数据库和隐私风险；C 无法表达 attempt、lease、mutation checkpoint 与 task polling 连续性。

### 决策 3：Acceptance 顺序

1. **面临的选择**：A. 先启动 executor 再写 ledger；B. durable input/document → DB ledger → Redis projection → schedule → response；C. 只写 DB 不写 Redis。
2. **选了哪个 + 为什么**：选 B；用户已批准。客户端拿到 taskId 前必须同时具备 durable command facts 和现有轮询投影，已知失败不能启动 operation。
3. **放弃的代价**：A 可能产生无账本执行或假 acceptance；C 会无故破坏现有 TaskController/Redis 契约。

### 决策 4：多实例 claim

1. **面临的选择**：A. JVM synchronized/in-memory set；B. Redis lock；C. MySQL 条件 UPDATE lease + heartbeat。
2. **选了哪个 + 为什么**：选 C；用户已批准。DB 已是 durable ledger，可用同一事实源原子 claim，并避免 Redis outage 时失去协调依据。
3. **放弃的代价**：A 跨实例无效且重启丢失；B 把恢复能力再次绑定到故障中的 Redis，锁/ledger 还会出现双事实源。

### 决策 5：哪些 phase 可以自动续跑

1. **面临的选择**：A. 所有非终态都重跑；B. 只恢复 SAFE_PRE_VECTOR 与 VECTOR_CONFIRMED 收尾；C. 所有中断只报错不恢复。
2. **选了哪个 + 为什么**：选 B；用户已批准。它能恢复可证明安全的窗口，同时遵守 C4d outcome unknown 不自动 replay。
3. **放弃的代价**：A 可能重复未知 vector mutation；C 会让 C5b 只剩监控，无法实现有意义的安全续跑。

### 决策 6：Chunk/vector identity

1. **面临的选择**：A. 保持每次随机 UUID；B. 新任务使用 contract-versioned deterministic ID；C. 在 task payload 保存全部 chunk 内容与向量。
2. **选了哪个 + 为什么**：选 B；用户已批准。相同 document/content/config 可重建相同 IDs，且无需在 ledger 复制正文或向量。
3. **放弃的代价**：A 无法可靠完成重启后的 post-vector 收尾；C 会造成敏感数据复制、体积膨胀和凭空新增备份责任。

### 决策 7：Vector in-flight 中断

1. **面临的选择**：A. lease 过期后直接重放 upsert；B. 当作未执行并失败；C. 标记 `RECONCILIATION_REQUIRED` 且 vector calls=0。
2. **选了哪个 + 为什么**：选 C；用户已批准。外部 mutation 与 DB checkpoint 不能原子提交，in-flight 只能诚实表达结果不确定。
3. **放弃的代价**：A 违反 C4d 并可能重复副作用；B 会把可能已成功的 mutation 伪装成确定失败。

### 决策 8：Redis 与 DB 查询关系

1. **面临的选择**：A. Redis miss 永远 404；B. DB ledger 权威、Redis 正常 miss 时回源并重建；C. 每次查询都只读 DB 并删除 Redis。
2. **选了哪个 + 为什么**：选 B；用户已批准。保留低延迟投影，同时让 TTL 不再抹掉已接受任务身份。
3. **放弃的代价**：A 会继续误报任务不存在；C 会扩大数据库读负载并无必要地推翻 C4c 已有投影路径。

### 决策 9：Reconciliation 与 auto resume 默认开关

1. **面临的选择**：A. 两者默认全开；B. reconciliation 可开、会触发 embedding 的 resume 默认关闭；C. 两者默认永久关闭。
2. **选了哪个 + 为什么**：选 B；用户已批准。分类/cleanup 不产生模型费用，而自动 resume 可能导致数据出站和批量 embedding，必须显式授权。
3. **放弃的代价**：A 可能在上线后产生未预算调用与限流；C 无法形成可操作的 C5b 能力。

### 决策 10：Legacy 无 ledger 任务

1. **面临的选择**：A. 所有 PENDING/FAILED + AVAILABLE 自动采用；B. 首版统一标记 legacy reconciliation required；C. 直接删除旧输入和 document。
2. **选了哪个 + 为什么**：选 B；用户已批准。旧状态不能证明 vector mutation 是否发生，保守隔离符合 C4d/C5a 边界。
3. **放弃的代价**：A 可能重放未知 mutation或重复计数；C 会丢失可人工判断的证据和用户文档事实。

### 决策 11：CLEANUP_PENDING 的处理

1. **面临的选择**：A. C5b 不处理；B. coordinator 有界重试幂等 input delete；C. cleanup pending 时重新执行整个索引。
2. **选了哪个 + 为什么**：选 B；用户已批准。这是 C5a 明确留给后续协调的安全副作用，且不需要 provider/vector 调用。
3. **放弃的代价**：A 会让磁盘与隐私债永久积累；C 把纯清理错误扩大成高风险业务重放。

### 决策 12：现有本地 MQ 模板

1. **面临的选择**：A. 直接把模板 producer/consumer 当 durable queue；B. C5b 不依赖它，真实索引由 ledger coordinator 调度；C. 顺手接 RabbitMQ。
2. **选了哪个 + 为什么**：选 B；用户已批准。模板当前无 durable broker 且默认 consumer 不执行真实索引，复用会制造错误能力声明。
3. **放弃的代价**：A 仍会随 JVM 丢失并可能只跑日志模拟；C 新增基础设施和部署面，超出一个 change。

### 决策 13：公开恢复入口

1. **面临的选择**：A. 新增用户 force-resume API；B. 首版只做自动安全 phase 与稳定协调状态，沿用现有 task query DTO；C. 前端直接改 document 状态触发重跑。
2. **选了哪个 + 为什么**：选 B；用户已批准。避免用户绕过 mutation 安全边界，也不扩大到前端重做。
3. **放弃的代价**：A 容易被误用为强制重放且需新增权限语义；C 会把内部状态机暴露为不安全写接口。
