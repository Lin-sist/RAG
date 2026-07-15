# Design: C4c Redis Failure Semantics

## 1. Context

C4c 是 Redis 依赖故障的独立 Type C change，只接受 Redis 相关契约。C4b 已归档；Milvus 与索引恢复继续由 C4d、C5a/C5b 处理。

Redis 当前不是单一“缓存”，而是多种语义的共同基础设施：

```text
optional performance data
  -> QA cache / embedding cache / query counter cleanup

security-critical state
  -> login session / refresh session / token blacklist / rate limiter

side-effect guard
  -> RedisIdempotencyHandler

state source
  -> RedisAsyncTaskManager / task status API
```

因此故障处理必须位于 consumer boundary，而不是在 `RedisUtil` 中全局吞异常。

## 2. Goals

- 为 Redis connection refused、command timeout、mid-request disconnect 与重启窗口建立稳定分类。
- 保证可选缓存失败不拖垮 canonical RAG/embedding 路径。
- 保证鉴权、撤销、限流和幂等保护在状态不可验证时不静默放行。
- 保证 task 状态不可用与 task 不存在是两个不同结果。
- 锁定安全诊断和无假成功副作用。

## 3. Non-goals

- 不做 Redis 高可用拓扑、连接池调优、熔断、全局 retry 或新依赖。
- 不做任务恢复、重放、孤儿协调、持久输入或 exactly-once。
- 不改 Redis 数据结构、key schema、TTL 或序列化版本。
- 不处理 Milvus/LLM/provider 故障。

## 4. 公共故障契约矩阵模板

| 维度 | 含义 |
|---|---|
| dependency / operation | 失败依赖与调用动作 |
| failure category | 稳定错误类别，不依赖原始 message |
| retry eligibility | 是否允许应用级重试 |
| attempt budget | 单个业务动作的硬上限 |
| client outcome | HTTP/服务层可观察结果 |
| side effects | canonical operation、token、cache、counter、task state |
| diagnostics | 允许记录的安全字段 |
| verification | 确定性故障注入和断言 |

### 4.1 C4c Redis 矩阵

| operation / failure | criticality | retry | client outcome | side effects |
|---|---|---|---|---|
| QA/embedding cache read | optional | 无应用级 retry | 视为 cache miss，继续 canonical operation | 不写假缓存 |
| QA/embedding cache write/evict | optional | 无应用级 retry | 保留 canonical 成功结果 | 仅缓存缺失，记录安全诊断 |
| query count increment/delete | optional metric | 无应用级 retry | ask/delete 继续 | 计数允许少记，不伪称成功写入 |
| statistics query-count read | response field source | 无应用级 retry | HTTP 503，不返回假零值 | 文档/向量事实不被覆盖 |
| login session write | security critical | 无应用级 retry | HTTP 503，不返回 token | 不建立半会话 |
| refresh session read/write | security critical | 无应用级 retry | HTTP 503，不签发新 token | 旧 token 状态不擅自改变 |
| blacklist lookup/write | security critical | 无应用级 retry | 请求拒绝或 logout 503 | 不把 unknown 当作 not-blacklisted |
| rate-limit Lua/check | security critical | 无应用级 retry | HTTP 503 | 目标 handler 不执行 |
| idempotency pre-operation read/lock | side-effect guard | 无应用级 retry | HTTP 503 `dependency_unavailable` | operation 调用次数 0 |
| idempotency post-operation result write | side-effect guard | 无应用级 retry | `outcome_unknown`，不得自动重试 | operation 可能已完成，不宣称回滚 |
| task initial PENDING write | state source | 无应用级 retry | HTTP 503 | task operation 不启动 |
| task status read | state source | 无应用级 retry | HTTP 503，不是 404 | 不伪造状态 |
| task progress/terminal write | state source | 无应用级 retry | 状态写失败可观察，禁止报告完成/取消成功 | operation 可能有部分副作用，交 C5 协调 |

所有 C4c 业务动作默认只执行一次 Redis command sequence，不在应用层自动重放。Lettuce 自身连接恢复不被描述为业务 retry；测试断言业务 operation 的调用次数和对外结果。

## 5. Contract Details

### 5.1 Optional cache 与计数

QA cache 和 embedding cache 只影响性能，不是答案或向量的事实源。read 异常等同 miss；write/evict 异常只记录降级，canonical result 保留。不能用全局 `RedisUtil` catch 实现，因为同一工具也服务 auth。

query count 是 best-effort 运行计数：increment 和知识库删除时的清理失败不阻塞 ask/delete。统计 API 若无法读取该字段则返回 503，不把 outage 表达成真实零次。C4c 不新增 nullable DTO 或 partial statistics schema。

### 5.2 Auth session 与 token blacklist

login 只有在 session hash 与 TTL 均成功写入后才返回 access/refresh token。refresh 在 session 读取、数据库用户重载、token 生成和新 session 状态写入的完整关键步骤中遇到 Redis 故障时返回 503，不混淆为凭据错误。

access token 请求的 blacklist lookup 失败时不得继续建立认证上下文。logout 只有 access token blacklist、关联 refresh token blacklist 与 session 删除达到既定撤销结果时才返回成功；Redis 故障返回 503。禁止记录 token、hash、session value 或 username。

### 5.3 Rate limit

所有当前 `@RateLimit` 都保护认证、问答或写操作。Redis Lua 返回 null/empty 或抛出连接/命令异常时统一 fail-closed 503；正常窗口超限继续现有 429。interceptor 必须区分 `rate_limited` 和 `redis_unavailable`，目标 controller 不执行。

### 5.4 Idempotency

仅当客户端实际提供 `X-Idempotency-Key` 时进入 Redis 幂等契约；`required=false` 且未提供 key 时继续当前直执行业务的兼容行为。

pre-operation Redis read/SETNX 失败必须在 supplier 执行前返回 503。正常已存在 PROCESSING 仍是 409。operation 成功后若 completed result 无法持久化，系统不能判断事务是否提交，因此返回稳定 `IDEMPOTENCY_OUTCOME_UNKNOWN`，响应不得建议自动重试，并记录安全 operation category，不记录原始 key 或 result。

实现前必须用 Spring AOP + transaction 集成测试确认切面顺序。若能证明业务事务尚未提交且可可靠回滚，仍要保留保守契约的测试；不得把偶然排序包装成跨模块 exactly-once 保证。

### 5.5 Async task state

`RedisAsyncTaskManager` 以 Redis 为状态事实源。submit 顺序固定为：生成 taskId → 持久化 PENDING → 注册/启动 future。初始写失败时 future 和 task operation 都不得启动。

status/result/exists 在 Redis 不可用时抛出稳定 dependency exception，由 TaskController 映射 503；只有成功读取且 key 不存在时才是 404。cancel 只有终止动作和 CANCELLED 状态均满足可观察契约时才成功。

任务执行中 Redis 中断后，业务 operation 可能已经执行。C4c 负责阻止假 `COMPLETED` 和假取消成功，不提供恢复、重放或孤儿协调；这些进入 C5。

### 5.6 Stable diagnostics

允许字段：

- `dependency=redis`；
- `subsystem`：`qa_cache`、`embedding_cache`、`query_counter`、`auth_session`、`token_blacklist`、`rate_limit`、`idempotency`、`task_status`；
- `operation`：固定枚举式动作名；
- `errorCategory`：`connection`、`timeout`、`command`、`serialization`、`unknown`；
- `failMode`：`open`、`closed`、`outcome_unknown`；
- traceId 与安全 exception type。

禁止字段：Redis key/value、token/hash、session、幂等 key、task result/error message、question/prompt/context、文件名、异常原始 message 与连接凭据。

## 6. HTTP And Error Mapping

| condition | HTTP | stable code |
|---|---:|---|
| Redis critical dependency unavailable | 503 | `REDIS_DEPENDENCY_UNAVAILABLE` |
| Idempotency operation outcome unknown | 503 | `IDEMPOTENCY_OUTCOME_UNKNOWN` |
| Existing idempotency request processing | 409 | existing `IDEMPOTENCY_001` |
| Normal rate limit exceeded | 429 | existing rate-limit code |
| Task key successfully read as absent | 404 | existing task-not-found code |

响应 message 为稳定中文提示，不包含 key、token、taskId 之外的用户内容或底层异常 message。对于 `IDEMPOTENCY_OUTCOME_UNKNOWN`，提示应要求查询当前资源状态或使用业务查询接口确认，不能简单写“请重试”。

## 7. Implementation Shape

预计实现边界：

- `rag-common`：新增统一安全的 Redis dependency exception/category；修正 rate limiter/interceptor、idempotency handler、async task manager 及异常映射；
- `rag-auth`：session 与 blacklist fail-closed；filter 对 Redis dependency failure 返回 503；
- `rag-core`：QA/embedding cache read/write/evict fail-open；
- `rag-admin`：query counter/statistics、knowledge-base cleanup、auth/task controller fault mapping 与相应测试；
- `c3-integration` 或独立 `c4c-redis-fault` Failsafe 入口：隔离 Redis stop/start 验证，不接触常驻容器；
- change artifacts、`.ai/ACTIVE_TASK.md` 与追加式 `.ai/AGENT_LOG.md`。

不计划修改前端、Flyway migration、API DTO shape、依赖版本、生产 Redis 地址、`.env.local`、`application-dev.yml`、评测脚本或 baseline spec。若实现需要 DTO/schema/新依赖，先停下补设计并重新审批。

## 8. TDD And Verification

按 RED → GREEN → REFACTOR 推进：

1. mock fault 测试锁定 optional cache/counter fail-open；
2. auth blacklist/session 与 rate-limit fail-closed；
3. idempotency pre-operation 与 post-operation unknown；
4. async task initial/status/terminal failure；
5. 隔离 Redis container stop/start 的公开 HTTP 验证；
6. 完整门禁与敏感信息扫描。

计划命令：

```powershell
mvn -q -pl rag-common -am test
mvn -q -pl rag-auth -am test
mvn -q -pl rag-core -am test
mvn -q -pl rag-admin -am test
mvn -q -pl rag-admin -am -Pc4c-redis-fault verify
mvn -q test
python -B -m unittest discover -s scripts -p 'test_*.py'
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run_local_quality_gates.ps1 -Mode SensitiveLogs
git diff --check
```

profile 名是规划值；实现前先复用 C3 Failsafe 结构，若无需新增 profile 可在 tasks 中记录实际入口。无前端改动时不运行前端 build；若意外触及前端，必须运行包含 `vue-tsc` 的正式 build。

## 9. External Calls And Cost

真实 embedding/rerank/judge/ask/LLM 调用量为 0。故障测试只在本机 Docker/Testcontainers network 操作合成 Redis 数据；可能下载仓库已声明的 Redis/Testcontainers 镜像，无业务数据出站、provider 限流或模型费用。

## 10. Alternatives

### A. 在 `RedisUtil` 统一 catch 并返回 null/false

拒绝。它会把 blacklist unknown 变成 not-blacklisted，把 task status unknown 变成 not-found，并让安全关键路径静默放行。

### B. 所有 Redis 故障统一 fail-closed

拒绝。QA/embedding cache 和查询计数是可选性能/统计能力，统一失败会无必要地拖垮 canonical 问答和索引路径。

### C. Redis 故障时使用单机内存 fallback

拒绝。多实例下 session、限流、幂等和 task state 会分叉；C4c 不制造只能在单节点成立的“高可用”。

### D. 对 Redis command 自动重试

拒绝作为 C4c 默认。写操作的透明重试可能重复副作用，且连接驱动已有恢复行为；本 change 先锁定单次业务动作和稳定失败，重试策略需独立容量与幂等设计。

## 决策记录

### DR-1：按 consumer criticality 分级还是统一处理

- **面临的选择**：在 `RedisUtil` 全局吞异常、所有入口统一 fail-closed，或由每个 consumer 按 optional/security/side-effect/state-source 分级。
- **选了哪个 + 为什么**：选择 consumer 分级，因为同一 Redis 同时承载可丢缓存和安全关键状态，统一语义必然产生可用性或安全错误；用户已在事前闸门确认。
- **放弃的代价**：实现和测试会跨 `rag-common`、`rag-auth`、`rag-core`、`rag-admin`，需要维护一张完整矩阵。

### DR-2：缓存故障 fail-open 还是 fail-closed

- **面临的选择**：Redis cache 失败时中断问答/embedding，或绕过 cache 继续 canonical operation。
- **选了哪个 + 为什么**：选择 fail-open，因为缓存不是答案或 embedding 的事实源，失败只应损失性能；用户已在事前闸门确认。
- **放弃的代价**：outage 期间 provider/检索调用量和延迟会上升，需要安全降级日志识别。

### DR-3：鉴权与黑名单故障放行还是拒绝

- **面临的选择**：blacklist/session unknown 时按有效状态继续，或 fail-closed 返回 503。
- **选了哪个 + 为什么**：选择 fail-closed，因为把 unknown 当作未撤销会让 logout 与 token revocation 失效；用户已在事前闸门确认。
- **放弃的代价**：Redis outage 期间已有 token 请求、login、refresh 和 logout 的可用性下降。

### DR-4：限流依赖故障 fail-open 还是 fail-closed

- **面临的选择**：Redis Lua 失败时继续放行、按 endpoint 混合，或所有当前 `@RateLimit` 入口返回 503。
- **选了哪个 + 为什么**：选择所有当前受限入口 fail-closed，因为它们包含 login、问答和写操作，静默放行会在依赖故障时移除唯一的共享限流边界；用户已在事前闸门确认。
- **放弃的代价**：Redis outage 会扩大受影响 API 面，连只读检索调试也不可用；未来可另设按风险分级的 policy。

### DR-5：幂等 post-operation 写失败如何表达

- **面临的选择**：假定业务回滚并提示重试、返回成功但不存结果，或明确返回 outcome unknown。
- **选了哪个 + 为什么**：选择 `IDEMPOTENCY_OUTCOME_UNKNOWN`，因为仅凭 Redis 写失败无法跨事务与外部副作用证明 operation 未提交；用户已在事前闸门确认。
- **放弃的代价**：客户端必须查询资源状态或人工协调，体验不如简单自动重试。

### DR-6：任务状态不可用是否等同任务不存在

- **面临的选择**：Redis read 异常返回 empty/404，使用内存 fallback，或返回 503 并保留 Redis 为唯一状态事实源。
- **选了哪个 + 为什么**：选择 503 且无内存 fallback，因为 unknown 不能伪装成 not-found，多实例内存状态也不一致；用户已在事前闸门确认。
- **放弃的代价**：outage 期间无法查询任务状态；恢复、孤儿协调与续跑仍需 C5。

### DR-7：统计读取故障返回零还是失败

- **面临的选择**：返回 `queryCount=0`、修改 DTO 表达 partial/unknown，或保持 DTO 并让 statistics endpoint 返回 503。
- **选了哪个 + 为什么**：选择 503 且不改 DTO，因为零是业务事实而不是故障占位，同时避免 C4c 扩成统计 API schema 重设计；用户已在事前闸门确认。
- **放弃的代价**：Redis outage 时整个 statistics 响应不可用，即使文档数可能仍可计算。

### DR-8：故障验证使用共享 Redis 还是隔离容器

- **面临的选择**：mock-only、停止本地共享 Redis，或 mock 加 Testcontainers stop/start。
- **选了哪个 + 为什么**：选择 mock 加隔离 Testcontainers，以同时锁定分支逻辑和真实连接中断语义且不破坏用户环境；用户已在事前闸门确认。
- **放弃的代价**：验证耗时更长并依赖 Docker；首次运行可能需要下载既有基础设施镜像。

### DR-9：C4c 是否包含任务恢复

- **面临的选择**：在 Redis 故障语义中同时实现任务重放/协调，或只禁止假成功并把恢复留给 C5。
- **选了哪个 + 为什么**：选择只锁定故障结果，因为持久输入、孤儿协调与续跑已有 C5a/C5b 顺序，提前混入会越过依赖边界；用户已在事前闸门确认。
- **放弃的代价**：C4c 完成后仍不能自动恢复 outage 中断的任务，只能准确暴露不确定状态。

### DR-10：提交责任

- **面临的选择**：沿用 C4b 的 Agent 提交授权，或把 C4c 作为新 change 重新确认。
- **选了哪个 + 为什么**：选择用户手动提交，因为仓库规则要求每个任务事前明确，C4b 授权不自动扩展到 C4c。
- **放弃的代价**：规格批准与实现提交需要用户显式操作或重新授权 Agent。

## 11. Consumer Inventory

| consumer | 分类 | 已锁定语义 |
|---|---|---|
| `RAGServiceImpl` QA cache | optional | read/write/evict/clear fail-open |
| `EmbeddingServiceImpl` cache | optional | read/write/evict/clear fail-open |
| `KnowledgeBaseServiceImpl` query counter | optional metric / response source | increment/delete fail-open；statistics read/deserialize 503 |
| `AuthServiceImpl` session | security-critical | login/refresh/logout Redis 故障 503，不返回无法验证的 token/成功 |
| `TokenBlacklistService` | security-critical | lookup/write/delete fail-closed |
| `SlidingWindowRateLimiter` | security-critical | exception/null/empty 503；正常超限仍 429 |
| `RedisIdempotencyHandler` | side-effect-guard | pre-operation 503/0 次调用；post-operation `outcome_unknown` |
| `RedisAsyncTaskManager` | state-source | initial/read/progress/terminal/cancel 故障可观察，不伪造 absent/completed/cancelled |
