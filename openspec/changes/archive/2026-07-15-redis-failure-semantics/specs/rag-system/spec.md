## ADDED Requirements

### Requirement: Redis 依赖分级与稳定故障结果

系统 SHALL 在 Redis consumer boundary 按业务关键性处理依赖故障，不得在共享 Redis utility 层把所有异常统一吞掉。QA cache、embedding cache、query count increment 与非关键清理属于 optional/best-effort：Redis read/write/delete 失败 MUST NOT 中断 canonical 问答、embedding 或知识库删除。登录 session、refresh session、token blacklist、rate limit、带 key 的幂等保护与异步任务状态属于 critical state：状态不可读取或持久化时 MUST fail-closed，并以 HTTP 503 和稳定错误码表达依赖不可用。

系统 MUST 区分“成功读取后不存在”和“Redis 状态未知”。task status unknown MUST NOT 表达为 404，blacklist unknown MUST NOT 表达为 not-blacklisted，statistics query-count unknown MUST NOT 表达为零。系统不得为 security-critical 或 state-source consumer 引入仅单实例有效的内存 fallback。

#### Scenario: QA cache read 不可用

- GIVEN QA cache Redis read 抛出连接或命令异常
- WHEN 用户发起可正常完成的问答
- THEN 系统把该次 cache read 视为 miss 并继续 canonical retrieval/generation
- AND 返回正常问答结果而不是 Redis 业务错误
- AND 不产生假 cache hit

#### Scenario: Embedding cache write 不可用

- GIVEN embedding provider 已成功返回向量
- WHEN Redis cache write 失败
- THEN 系统仍返回该向量
- AND 不把缓存失败转换为 embedding failure

#### Scenario: 查询计数写入失败

- GIVEN 问答主操作已形成可返回结果
- WHEN query count increment 发生 Redis 故障
- THEN 问答结果仍按主操作结果返回
- AND 系统记录安全的 best-effort 降级诊断

#### Scenario: 统计计数读取失败

- GIVEN statistics endpoint 无法从 Redis 读取 query count
- WHEN 客户端请求知识库统计
- THEN 系统返回 HTTP 503 与稳定依赖不可用错误
- AND 不返回伪造的 `queryCount=0`

#### Scenario: 登录 session 写入失败

- GIVEN 用户凭据有效但 Redis session hash 或 TTL 无法持久化
- WHEN 用户登录
- THEN 系统返回 HTTP 503
- AND 不向客户端返回 access token 或 refresh token

#### Scenario: 黑名单状态未知

- GIVEN access token 的 Redis blacklist lookup 失败
- WHEN 请求进入认证过滤器
- THEN 系统不得建立该 token 的认证上下文
- AND 返回稳定的 HTTP 503，而不是把 token 当作未撤销

#### Scenario: 限流依赖不可用

- GIVEN 带 `@RateLimit` 的请求无法执行 Redis 限流命令或命令结果为空
- WHEN 拦截器处理该请求
- THEN 系统返回 HTTP 503 与稳定依赖不可用错误
- AND 目标 handler 不执行
- AND 正常超出配额的 429 语义保持不变

### Requirement: Redis 幂等保护与不确定结果

客户端提供幂等 key 时，系统 MUST 在业务 operation 开始前成功读取并获取 Redis 幂等状态；pre-operation Redis 故障 MUST 返回 HTTP 503 且 operation MUST NOT 执行。正常已存在的 PROCESSING 状态 SHALL 继续表达为 409 conflict。

业务 operation 可能完成后若 completed result 无法持久化，系统 MUST 返回稳定的 `IDEMPOTENCY_OUTCOME_UNKNOWN`，MUST NOT 声称 operation 已回滚或可以安全自动重试。系统 MUST NOT 在客户端响应或普通日志中记录原始幂等 key、Redis key/value 或业务 result。

#### Scenario: 幂等锁获取前 Redis 不可用

- GIVEN 请求携带 `X-Idempotency-Key`
- WHEN Redis read 或 SETNX 在 operation 开始前失败
- THEN 系统返回 HTTP 503 与依赖不可用错误
- AND operation 调用次数为 0

#### Scenario: 幂等结果写入后的结果未知

- GIVEN 请求已获取幂等锁且业务 operation 已返回
- WHEN completed result 无法写入 Redis
- THEN 系统返回 `IDEMPOTENCY_OUTCOME_UNKNOWN`
- AND 不声称业务 operation 已回滚
- AND 客户端提示要求查询当前资源状态，不建议直接自动重试

#### Scenario: 未提供可选幂等 key

- GIVEN endpoint 的幂等 key 为 optional 且请求未提供该 header
- WHEN 业务 operation 执行
- THEN 系统保持当前不进入 Redis 幂等检查的兼容行为

### Requirement: Redis 异步任务状态事实源

Redis 作为当前异步任务状态事实源时，系统 MUST 在初始 PENDING 状态成功持久化后才启动 task operation。初始状态写入失败 MUST 返回 HTTP 503 且 task operation MUST NOT 启动。task status/result 读取发生 Redis 故障时 MUST 返回 HTTP 503，不得表达为 task 不存在；只有 Redis 成功读取且 key 不存在时 MAY 返回 404。

任务执行中 progress、COMPLETED、FAILED 或 CANCELLED 状态无法持久化时，系统 MUST NOT 对外报告相应状态写入成功。C4c 不承诺恢复、重放或协调已产生部分副作用的任务；这些能力属于后续索引恢复 change。

#### Scenario: 初始任务状态写入失败

- GIVEN Redis 无法持久化新任务的 PENDING 状态
- WHEN 客户端提交异步索引任务
- THEN 系统返回 HTTP 503
- AND task operation 调用次数为 0
- AND 不返回可轮询的假 taskId

#### Scenario: 任务状态读取失败

- GIVEN taskId 已由系统接受但 Redis status read 失败
- WHEN 客户端查询任务状态
- THEN 系统返回 HTTP 503
- AND 不返回 404、COMPLETED 或空状态

#### Scenario: 取消状态未持久化

- GIVEN 客户端请求取消任务
- WHEN Redis 无法持久化 CANCELLED 状态
- THEN 系统不得报告取消成功
- AND 返回稳定的依赖不可用或状态不确定结果

### Requirement: Redis 故障安全诊断

Redis 故障响应与普通日志 MAY 记录 dependency、固定 subsystem、固定 operation、稳定 errorCategory、failMode、traceId 与安全 exception type。系统 MUST NOT 记录 Redis key/value、token 或 token hash、session 内容、幂等 key、task result、question、prompt、context、文件名、连接凭据或异常原始 message。

#### Scenario: 故障内容包含敏感 marker

- GIVEN 合成 Redis key/value、token、session、幂等 key 或异常 message 包含敏感 marker
- WHEN 系统生成客户端错误、diagnostics 与普通日志
- THEN 输出只包含允许的固定字段和稳定错误类别
- AND 不包含上述 marker 或底层连接凭据
