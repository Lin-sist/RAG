# Proposal: C5b Index Task Reconciliation And Safe Resume

## Why

C5a 已让被接受的文档输入跨进程存活，但任务本身仍由当前 JVM 的 `CompletableFuture` 与闭包执行，Redis 只保存 24 小时状态投影。进程在 PENDING、解析、embedding、vector mutation 或 DB 收尾阶段退出后，新实例没有 durable execution phase、claim/lease、attempt 或安全续跑依据；仅凭 `document.status`、Redis 状态或输入仍为 AVAILABLE，无法判断 vector mutation 是否已发生。

仓库虽然已有 `async_task` 表和 `rag-common.async.mq` 模板，但当前 Java 生产链路不读写该表，真实上传也不经过 `DocumentIndexProducer`；默认 consumer 只记录日志，不能作为 durable queue 或恢复能力。C5b 必须先建立事实账本和安全边界，再谈自动续跑，不能把已有占位结构包装成已实现。

## Readiness Verdict

`GO`，允许进入规划阶段：

- C5a 已由提交 `b144bbb` 完成，delta 已接受进 baseline，change 已归档。
- 工作区在扫描开始时干净，`.ai/ACTIVE_TASK.md` 为 `IDLE`，没有其他 active change。
- durable input、完整性校验、FAILED/outcome unknown 保留和 CLEANUP_PENDING 已具备，满足 C5b 前置。
- C4c/C4d 已锁定 Redis fail-closed 与 vector outcome unknown 不自动重放，C5b 可在既有契约上增量规划。
- 当前阻断项属于待设计能力，不阻止创建 proposal；未获批准前不得修改实现。

## 用户故事（大白话：改前坏事 → 改后不同）

改之前，上传已经成功、文件也还在，但服务一重启，真正干活的内存任务就没了：Redis 可能还显示 PENDING/RUNNING，也可能 24 小时后消失，文档会永久卡住；如果系统贸然重跑，又可能把已经发给 Milvus 但回执未知的写入再做一次。改之后，每个新索引任务都有数据库执行账本、明确 phase 和有期限的 claim；新实例只续跑能证明安全的阶段，多个实例不会同时抢同一任务，碰到 vector in-flight 或旧任务证据不足时明确进入 `RECONCILIATION_REQUIRED`，而不是假装恢复成功。

## Current Status

- `confirmed`：C5a durable input 可按 document 的 opaque key/size/SHA-256 重新打开，输入状态覆盖 AVAILABLE/CLEANUP_PENDING/CLEANED/MISSING/CORRUPT。
- `confirmed`：真实上传路径直接调用 `RedisAsyncTaskManager.submit`；任务 closure 和 `CompletableFuture` 只在本 JVM 内存，进程退出即丢失执行体。
- `confirmed`：Redis task status TTL 为 86400 秒；Redis 保存状态/结果投影，不保存可由新实例执行的 typed command。
- `confirmed`：V2 已建 `async_task` 表，但当前生产 Java 无 entity/mapper/repository，也没有任何读写该表的索引链路。
- `confirmed`：`rag-common.async.mq` 仅为未接入真实上传的本地模板；默认 consumer 不执行真实解析、embedding 或 vector mutation。
- `confirmed`：`DocumentProcessor` 与 `DocumentChunker` 当前生成随机 UUID，重复解析不能天然重建相同 vector IDs。
- `confirmed`：C4d 明确禁止对 `VECTOR_OPERATION_OUTCOME_UNKNOWN` 自动重放；进程在 mutation 发出后中断同样不能被推断为“未执行”。
- `partial`：document/input 状态可筛选候选任务，但没有 taskId 关联、durable phase、attempt、lease owner/expiry 或 execution contract version。
- `partial`：vector adapter 提供 `getById/getByIds`，但当前没有持久化的 prepared vector ID manifest，无法据此无歧义恢复所有中断窗口。
- `planned`：使用既有 `async_task` 表建立文档索引 durable ledger，以 MySQL phase/lease 为权威，Redis 退为可重建的查询投影。
- `planned`：只对可证明未进入 vector mutation 或已明确 VECTOR_CONFIRMED 的任务执行安全续跑；in-flight/证据不一致进入 `RECONCILIATION_REQUIRED`。
- `out_of_scope`：强制重放 outcome unknown、RabbitMQ/Kafka、exactly-once、对象存储、非索引任务恢复、恢复 UI、跨 provider failover。
- `unknown`：自动 resume 默认是否开启、lease/scan/attempt 默认值、确定性 chunk ID 方案和 legacy task 处置方式，均需用户在事前闸门确认。

## Scope

- 把既有 `async_task` 表接入真实文档索引链路，新增 typed document/task association、owner、execution phase、attempt、lease、next-attempt、stable failure code 与 index contract facts。
- 保持 taskId 在重启前后稳定；客户端查询 Redis miss/TTL 后可从 durable ledger 获取安全状态并按需重建投影，不把已接受任务误报 404。
- 将执行拆成 durable checkpoints：ACCEPTED、SAFE_PRE_VECTOR、VECTOR_IN_FLIGHT、VECTOR_CONFIRMED、FINALIZING、COMPLETED/FAILED/CANCELLED/RECONCILIATION_REQUIRED。
- 使用数据库 compare-and-set lease/heartbeat 协调多实例；只有 lease 过期且 phase 可安全恢复的任务可被重新 claim。
- 为新任务建立可重现的 chunk/vector identity 与 index contract version；恢复时配置或内容不一致则 fail closed。
- 对 VECTOR_IN_FLIGHT、`VECTOR_OPERATION_OUTCOME_UNKNOWN`、legacy 无 ledger 或证据冲突任务禁止自动 mutation replay，明确进入人工协调状态。
- 有界扫描 CLEANUP_PENDING 输入并执行幂等清理；不得因 cleanup retry 触发索引重放。
- 提供配置化 scan batch/concurrency/lease/backoff/max-attempt；reconciliation 可观测，但会触发 embedding 的自动 resume 默认关闭，需显式启用。
- 使用 Testcontainers MySQL/Redis、确定性 embedding 与合成数据验证 crash-window、lease 竞争、Redis 重建和安全续跑。

## Non-goals

- 不承诺 exactly-once；目标是 at-most-one active lease、stable task identity、safe replay boundary 与可协调结果。
- 不自动重放 VECTOR_IN_FLIGHT、`VECTOR_OPERATION_OUTCOME_UNKNOWN` 或无法证明 mutation 未发生的旧任务。
- 不新增“强制重试/忽略风险”公开 endpoint、后台管理 UI 或前端页面。
- 不把 RabbitMQ/Kafka/Redis Streams 引入本 change，也不把现有本地 MQ 模板宣称为 durable queue。
- 不恢复 QA、导出或其他非文档索引异步任务。
- 不改变 retrieval、rerank、prompt、citation、no-answer、judge 或评测指标。
- 不接入 MinIO/S3，不实现 filesystem/DB/Redis/Milvus 分布式事务。
- 不把输入 bytes、正文、chunk 内容、storage key、文件名、标题、prompt/context 写入 durable task payload、普通日志或客户端错误。

## Spec Delta Decision

C5b 修改任务被接受后的持久性保证、task polling 事实源、跨实例 claim、恢复状态机、失败结果与自动副作用边界，属于长期用户可观察能力，必须提供 `rag-system` spec delta。规划批准不等于实现完成；只有通过真实 MySQL/Redis crash-window 验证并由用户验收后才能接受进 baseline。

## External Calls And Authorization

| 调用类型 | 规划阶段调用量 | 实现测试调用量 | 生产恢复潜在调用 | 数据出站 | 费用/限流 | 授权状态 |
|---|---:|---:|---:|---|---|---|
| embedding | 0 | 0 真实调用；确定性 test stub | 每个安全续跑任务可能重新 embedding 1 次 | 启用真实 provider 时会发送恢复文档 chunks | 取决于 provider；需批次/并发上限 | 自动 resume 默认关闭，启用前需明确授权 |
| rerank | 0 | 0 | 0 | 无 | 0 | 不适用 |
| judge | 0 | 0 | 0 | 无 | 0 | 不适用 |
| ask/LLM | 0 | 0 | 0 | 无 | 0 | 不适用 |

规划与实现测试不得调用真实 provider。隔离验证只使用固定基础设施镜像、确定性 embedding 和合成文档；若后续要在真实 provider 配置下批量 resume，必须先说明候选任务数、预计 chunks/调用量、模型、数据出站、费用与限流风险并取得用户授权。

## Acceptance Evidence

- 新上传在返回 taskId 前已建立 durable task ledger，且 DB/Redis/task scheduling 任一已知失败不返回假 acceptance。
- 杀死实例后，ACCEPTED/SAFE_PRE_VECTOR 任务在 lease 过期后由新实例以同 taskId 安全续跑并完成。
- 两个 coordinator 并发扫描时只有一个 lease owner 执行，attempt 与 heartbeat 可观察。
- VECTOR_IN_FLIGHT、outcome unknown、contract/content mismatch 与 legacy 无 ledger 不调用 vector mutation，稳定进入 `RECONCILIATION_REQUIRED`。
- VECTOR_CONFIRMED 后中断可只完成 DB/keyword/input cleanup 收尾，不再次调用 vector upsert。
- Redis status 丢失或 TTL 到期时，已接受任务不误报 404；DB ledger 返回安全状态并重建投影。
- CLEANUP_PENDING 重试只执行幂等 input delete，不触发 parser/embedding/vector。
- task/ledger/log/client 不包含 input key、文件名、标题、正文、chunk、provider raw message 或凭据。
- 前向 migration 在真实 MySQL 8.0.36 上覆盖 V1→新版本、已有 async_task 兼容、lease CAS 与索引使用。
- 聚焦 Java、完整 Maven、C3/C4c/C4d 回归、Python、SensitiveLogs 与 `git diff --check` 通过；真实 provider 调用量为 0。

## Risks

- DB ledger 与外部 vector mutation 仍无原子事务；VECTOR_IN_FLIGHT 到 VECTOR_CONFIRMED 的 crash window只能隔离为 reconciliation required，不能消除。
- 确定性 chunk/vector identity 会改变新索引的内部 ID 生成规则；必须保证内容/排序不变、旧 IDs 可读且不重写既有索引。
- 自动 resume 可能重新触发 embedding 成本和数据出站；默认关闭、批次有界和显式授权是安全前提。
- lease 太短会误抢仍在运行的任务，太长会拖慢恢复；必须通过 heartbeat、时钟边界与真实并发测试锁定。
- legacy PENDING/FAILED 没有 durable phase，自动采用可能重放未知 vector mutation；首版应保守隔离。
- `async_task.payload/result` 若直接复用容易持久化正文或 ProcessResult.rawContent；C5b 必须只保存 typed 安全事实和 sanitized result。

## Commit Responsibility

当前为 `用户手动提交`。本轮只进入 C5b 规划；Agent 不执行 `git add`、`git commit`、push、PR、部署或发布。
