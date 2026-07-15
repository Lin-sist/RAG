# Proposal: C4c Redis Failure Semantics

## Why

C3 已证明隔离 Redis 正常时登录、上传、异步索引、retrieval 与删除主链路可运行；C4b 已接受 LLM provider 故障契约。下一步需要锁定 Redis 重启、连接拒绝与命令超时时各业务入口的真实语义。

当前 Redis 同时承载可选缓存、查询计数、限流、登录会话、token 黑名单、幂等状态和异步任务状态。各消费者目前自行处理异常，结果互相矛盾：embedding cache 读失败会降级，但写失败可能中断生成；QA cache 失败可能把整次问答变成错误；限流和黑名单查询在 Redis 异常时放行；登录与任务状态又直接传播底层异常；知识库统计可能把依赖故障误报成零。若只在 `RedisUtil` 全局吞异常，会把安全关键状态和可选性能缓存混为一谈。

## 用户故事（大白话：改前坏事 → 改后不同）

改之前，Redis 短暂重启时，同一个故障可能让缓存拖垮问答、让已注销 token 被当作有效、让限流失效，或者把“任务状态读不到”误报成“任务不存在”；改之后，缓存和统计清理等非关键能力可以安全绕过，登录、黑名单、限流、幂等与任务状态等关键能力会明确拒绝并返回稳定的暂不可用结果，系统不会伪造成功、零值或不存在。

## Current Status

- `confirmed`：C4b 已由用户验收，delta 已接受进 `rag-system` baseline，change 已归档，启动 C4c 前工作区干净。
- `confirmed`：Redis 直接消费者包括 QA/embedding cache、查询计数与统计、登录 session、token blacklist、滑动窗口限流、Redis 幂等和异步任务状态。
- `partial`：embedding cache read、查询计数 increment、rate limiter、blacklist add/check/remove 已各自吞掉部分异常，但 fail-open 是否安全没有统一契约。
- `partial`：login/refresh、task submit/status 和 idempotency 会传播部分 Redis 异常，但客户端状态、错误码、operation 是否已执行和重试建议没有被测试锁定。
- `planned`：按数据关键性建立 fail-open/fail-closed 矩阵，并用 mock fault seam 与隔离 Testcontainers Redis stop/start 验证。
- `out_of_scope`：Milvus 故障（C4d）、任务输入持久化与孤儿协调/续跑（C5a/C5b）、Redis 集群/哨兵部署、跨进程恢复、生产监控平台与容量优化。
- `unknown`：Redis 在业务操作完成后、幂等结果落盘前断开时，底层事务与 AOP 顺序能否保证回滚；实现前必须用测试确认，并且不得宣称 exactly-once。

## Scope

- 复用 C4 公共故障契约矩阵，按 `optional / security_critical / side_effect_guard / state_source` 四类定义 Redis 失败行为。
- QA 与 embedding cache 的 read/write/evict 失败采用 fail-open：继续 canonical retrieval/generation/embedding，不把缓存异常伪装成业务失败。
- 查询计数 increment 与删除时计数清理采用 best-effort；统计 read 失败不得返回伪造的 `queryCount=0`，而应返回稳定的依赖暂不可用结果。
- login session 写入、refresh session 读取、token blacklist 检查/写入、带 `X-Idempotency-Key` 的副作用保护、rate-limit 检查和 task 状态读写采用 fail-closed。
- Redis 故障在到达 controller/filter/interceptor 时映射为稳定 HTTP 503 与安全错误码；已知并发幂等冲突继续使用现有 409，不混淆为依赖故障。
- task submit 只有在初始 `PENDING` 状态成功持久化后才允许启动执行；状态存储不可用时不得把任务误报为不存在、完成或取消成功。
- 幂等保护在业务操作开始前失去 Redis 时不得执行操作；若操作可能已完成但结果状态无法持久化，返回明确的 `outcome_unknown`，禁止提示客户端自动重试。
- 使用稳定诊断字段记录 subsystem、operation、failure category 与 fail mode，不记录 Redis key/value、token、session、幂等 key、任务结果、question 或异常原始消息。
- 不新增生产依赖，不调用任何真实模型/provider。

## Non-goals

- 不修改 Milvus、LLM、retrieval、rerank、prompt、citation、no-answer 或 judge 指标口径。
- 不实现 Redis Cluster、Sentinel、replica failover、应用级熔断器或跨请求 retry policy。
- 不引入内存 session、内存 blacklist、内存 rate-limit、内存 task state 或本地幂等 fallback；这些 fallback 在多实例下会产生不一致。
- 不承诺 Redis 故障期间 exactly-once；幂等 post-operation 写失败只表达 outcome unknown。
- 不做任务重放、孤儿任务扫描、进程重启恢复或持久输入设计；这些属于 C5a/C5b。
- 不优化 `KEYS`、TTL、序列化格式、key 命名、连接池、容量或性能。
- 不修改前端交互；只锁定后端可观察错误契约，前端专项展示另行处理。
- 本规格阶段不修改生产 Java、测试、配置、依赖或 baseline spec。

## Spec Delta Decision

本 change 新增 Redis 依赖分级、关键状态 fail-closed、幂等不确定结果、任务状态事实源和安全诊断契约。它会改变认证、限流、API 错误与副作用语义，因此必须提供 `rag-system` spec delta；用户验收前不得接受进 baseline。

## External Calls And Authorization

| 调用类型 | 规格/实现验证调用量 | 数据出站 | 模型 | 限流风险 | 费用 | 授权状态 |
|---|---:|---|---|---|---|---|
| embedding | 0 | 无 | 无 | 无 | 0 | 不适用 |
| rerank | 0 | 无 | 无 | 无 | 0 | 不适用 |
| judge | 0 | 无 | 无 | 无 | 0 | 不适用 |
| ask/LLM | 0 | 无 | 无 | 无 | 0 | 不适用 |

故障集成测试只操作 C3/Failsafe 启动的隔离 Redis Testcontainer，并使用合成 key/value；不得停止、重启或清理用户常驻 Redis。若本地缺少已声明镜像，Testcontainers 可能从镜像仓库下载基础设施镜像，但不上传业务数据。

## Acceptance Evidence

- 用户先审阅并批准 proposal、design、决策记录与 spec delta，再允许修改业务代码。
- fault matrix 覆盖所有 tracked Redis 直接消费者，并能说明每个操作的 fail mode、客户端结果与副作用。
- QA/embedding cache read/write 故障不改变 canonical 成功结果；失败缓存不产生假命中或部分写成功声明。
- login/refresh/blacklist/rate-limit 的 Redis 故障以安全的 503 fail-closed 结束，不签发或接受无法验证状态的 token，不绕过限流。
- 带幂等 key 的请求在 pre-operation Redis 故障时 operation 调用次数为 0；post-operation 状态写失败返回 `outcome_unknown`，不得建议自动重试。
- task 初始状态写失败时 operation 调用次数为 0；status read 故障返回 503，不冒充 404；取消状态未持久化时不返回成功。
- 查询计数写入/清理失败不拖垮问答或知识库删除；统计读取失败不伪造零值。
- mock 单元故障测试与隔离 Redis stop/start 集成测试通过；完整 Maven、Python、SensitiveLogs 与 `git diff --check` 通过。
- 真实 embedding/rerank/judge/ask/LLM 业务调用量均为 0。

## Risks

- fail-closed 会降低 Redis outage 期间的认证和受限接口可用性，但可避免撤销绕过、无限流与无法证明的幂等副作用。
- 幂等结果在 operation 后写失败时无法仅靠 Redis 判断业务是否提交；本 change 必须诚实返回 outcome unknown，长期根治可能需要数据库幂等记录或 outbox，另起 change。
- 异步任务在执行中 Redis 中断后可能已经产生部分外部副作用，但状态不可见；C4c 只禁止假成功，恢复与协调留给 C5。
- HTTP 503 是现有 API 的新故障语义，需要 controller/filter/interceptor 级测试避免被全局异常处理误映射为 500/401/404。
- 隔离 stop/start 测试若复用共享 Redis 会破坏本地环境，因此必须绑定 Testcontainers 生命周期并断言未触碰常驻容器。

## Commit Responsibility

`用户手动提交`。当前仅授权创建 C4c 规格草案；Agent 不暂存、不提交、不 push。若用户批准实现并授权 Agent 提交，需要另行明确。
