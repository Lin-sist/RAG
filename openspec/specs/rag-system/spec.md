# RAG System Specification

## Requirements

### Requirement: 文档索引链路

系统 SHALL 将上传文档解析、分块、向量化并写入配置的向量库，同时持久化文档/chunk 状态；失败 SHALL 以可观察的失败状态结束，不得伪装成功。

#### Scenario: 成功索引

- GIVEN 用户对知识库具有写权限
- WHEN 上传受支持的文档并完成异步任务
- THEN 文档状态为 `COMPLETED`
- AND chunk 可被该知识库检索

#### Scenario: 外部依赖失败

- WHEN Embedding 或向量库调用失败
- THEN 任务或文档状态明确失败
- AND 错误不包含 secret

### Requirement: 混合检索

系统 SHALL 支持 dense vector 与 BM25 keyword 双路召回并通过 RRF 融合；任一非关键路线不可用时 SHALL 以明确降级方式维持主链路或返回可诊断失败。

#### Scenario: 默认查询

- WHEN 用户在已完成索引的知识库中提问
- THEN 系统使用当前启用的 hybrid 配置检索
- AND 返回结果携带可用于诊断的来源与分数信息

### Requirement: Reranker 边界

系统 MUST 默认使用已验证可用的 reranker。真实 model provider 未配置、不可用或失败时 MUST 降级到 heuristic，且不得宣称 model rerank 收益已经验证。

#### Scenario: Model provider 不可用

- GIVEN 配置请求 model reranker
- WHEN provider 健康检查失败或调用异常
- THEN 查询使用 heuristic fallback 或返回明确失败
- AND 记录 provider 与降级原因

### Requirement: 生成与引用

系统 SHALL 只基于检索上下文生成知识库回答。Citation MUST 回连到本轮 returned contexts；无法验证的 citation SHALL 被丢弃或标记为 unsupported。

#### Scenario: 有足够上下文

- WHEN 检索上下文足以回答问题
- THEN 响应包含答案及可验证 citations
- AND citation snippet 能回连到 returned contexts

#### Scenario: 无足够上下文

- WHEN 知识库没有足够信息
- THEN 系统明确拒答
- AND `metadata.status=no_result`
- AND citations 为空

### Requirement: Secret 安全

API key、JWT secret、数据库密码和用户私密内容 MUST NOT 写入 tracked files、诊断报告或普通日志。

#### Scenario: Provider 失败诊断

- WHEN 外部 provider 调用失败
- THEN 可以记录 provider、endpoint、model、timeout、retry 和错误类别
- BUT MUST NOT 记录 API key 或认证 header

### Requirement: 生产 JWT Secret 启动守卫

系统在精确 active profile `prod` 下 MUST 在构造 JWT signing key 前校验 `jwt.secret`。当值为空白、等于仓库已知默认值、包含首尾空白或整个值仍为未解析占位符时，应用 MUST 拒绝启动；失败信息 MUST NOT 回显该值、长度、hash 或内容片段。

通过自定义 production guard 的候选值 SHALL 保持原样并以 UTF-8 bytes 交给 JJWT。自定义 guard MUST NOT 复制 JJWT 的算法最小 key-length 规则；过短 key SHALL 继续由 JJWT 在所有 profile 下拒绝。

#### Scenario: 生产环境使用仓库已知默认值

- GIVEN active profiles 包含精确的 `prod`
- AND `jwt.secret` 等于 tracked config 的已知默认值
- WHEN Spring 创建 JWT signing provider
- THEN 应用启动失败
- AND 错误指出 `jwt.secret` 使用了 `known-default`
- AND 错误与普通日志不包含 secret 原文、长度或 hash

#### Scenario: 生产环境 secret 为空白或带首尾空白

- GIVEN active profiles 包含精确的 `prod`
- WHEN `jwt.secret` 为 `null`、空串、纯空白或首尾包含空白
- THEN 应用启动失败
- AND 系统不通过自动 trim 改变输入后继续启动

#### Scenario: 生产环境仍收到未解析占位符

- GIVEN active profiles 包含精确的 `prod`
- WHEN 整个 `jwt.secret` 值符合未解析占位符结构 `${...}`
- THEN 应用启动失败
- AND 错误只报告 `unresolved-placeholder` 类别与修复指引

#### Scenario: 生产环境使用合法候选值

- GIVEN active profiles 包含精确的 `prod`
- AND `jwt.secret` 不命中 production guard 的精确拒绝规则
- WHEN 系统构造 JWT signing provider
- THEN 候选值以原始 UTF-8 bytes 交给 JJWT
- AND 是否满足算法 key-strength 由 JJWT 决定

#### Scenario: 非生产环境保留本地兼容性

- GIVEN active profiles 不包含精确的 `prod`
- WHEN 系统使用当前 dev/test 配置启动
- THEN production guard 不因仓库 fallback 值阻断启动
- BUT JJWT 仍按 UTF-8 bytes 拒绝过短 signing key

### Requirement: 数据库用户认证

系统 SHALL 以数据库中持久化的用户、密码 hash、enabled/deleted 状态和角色作为登录认证事实源，MUST NOT 在运行时创建或依赖进程内固定默认账号。密码 MUST 以 BCrypt hash 验证，普通日志、错误响应和 tracked files MUST NOT 包含明文密码或 bootstrap 凭据。

#### Scenario: 持久化用户成功登录

- GIVEN 数据库存在未删除、已启用且密码 hash 有效的用户
- AND 用户关联一个或多个未删除角色
- WHEN 用户提交正确的 username 与 password
- THEN 系统从数据库加载用户与角色并完成认证
- AND 签发的 token 使用该数据库用户 ID 与当前角色
- AND 应用重启后仍可使用同一持久化用户登录

#### Scenario: 无效用户不能登录

- GIVEN username 对应的用户不存在、已逻辑删除、已禁用或密码不匹配
- WHEN 发起登录
- THEN 系统拒绝认证
- AND 响应不暴露用户是否存在、password hash 或凭据内容

#### Scenario: 运行时不存在固定默认账号

- GIVEN bootstrap 未显式启用
- WHEN 应用完成启动
- THEN 系统不创建进程内或数据库默认用户
- AND `admin123`、`user123` 等仓库已知固定凭据不能用于登录

### Requirement: Refresh 时重载用户状态

系统在使用 refresh token 签发新 token 前 SHALL 从数据库重新加载用户与角色。用户不存在、已逻辑删除或已禁用时 MUST 拒绝 refresh；角色变化 SHALL 反映到新 token。C2 不要求每次 access token 请求都查询数据库，存量 access token 继续受现有过期与黑名单规则约束。

#### Scenario: 用户被禁用后不能刷新

- GIVEN refresh token 的签发用户已在数据库中被禁用、逻辑删除或移除
- WHEN 客户端请求 refresh
- THEN 系统拒绝签发新的 access token 与 refresh token
- AND 不使用 refresh token 内的旧用户状态绕过数据库状态

#### Scenario: 角色变化进入新 token

- GIVEN 用户的数据库角色在原 token 签发后发生变化
- AND 用户仍存在且已启用
- WHEN refresh 会话与 token 校验通过
- THEN 系统使用数据库中的最新角色签发新 token
- AND 不继续复制旧 token 中的角色快照

### Requirement: 显式一次性管理员 Bootstrap

系统 SHALL 提供默认关闭的一次性管理员 bootstrap。bootstrap 仅在显式启用且用户名、密码由外部配置完整提供时执行；tracked config MUST NOT 提供可登录密码 fallback。用户写入、BCrypt 编码后的密码持久化与 ADMIN 角色关联 MUST 在同一事务中完成。

bootstrap MUST 幂等，MUST NOT 覆盖正常同名用户的密码、enabled 状态、email 或角色。数据库已有其他非删除用户而目标 username 不存在、ADMIN 角色缺失或配置无效时，应用 MUST fail-fast 且不得留下部分数据。

#### Scenario: Bootstrap 默认关闭

- GIVEN bootstrap 配置未设置或 `enabled=false`
- WHEN 应用启动
- THEN 系统不创建或修改任何用户与角色关联
- AND 缺少 bootstrap 凭据不会产生默认账号

#### Scenario: 空用户库创建首个管理员

- GIVEN bootstrap 显式启用
- AND 外部提供合法 username 与 password
- AND 数据库没有非删除用户且存在 ADMIN 角色
- WHEN 应用完成数据库初始化
- THEN 系统创建一个已启用的持久化用户
- AND 只持久化 password 的 BCrypt hash
- AND 在同一事务中建立 ADMIN 角色关联
- AND 应用在 bootstrap 完成前不被视为 ready

#### Scenario: 重复启动不覆盖正常管理员

- GIVEN bootstrap 目标 username 已对应正常数据库用户
- AND 该用户已有 ADMIN 角色
- WHEN 使用相同或不同的外部 bootstrap 配置再次启动
- THEN bootstrap 幂等结束
- AND 不修改该用户的密码、状态、email 或角色

#### Scenario: 同名非管理员不能被静默提权

- GIVEN bootstrap 目标 username 已对应正常数据库用户
- AND 该用户没有 ADMIN 角色
- WHEN 应用启动
- THEN 应用 fail-fast
- AND 不修改该用户的密码、状态、email 或角色

#### Scenario: 已有其他用户时拒绝注入管理员

- GIVEN bootstrap 显式启用
- AND 目标 username 不存在
- AND 数据库已存在其他非删除用户
- WHEN 应用启动
- THEN 应用 fail-fast
- AND 不创建新用户或部分角色关联
- AND 错误不回显 username、password、长度、hash 或内容片段

### Requirement: 已知默认管理员种子隔离

系统 SHALL 通过新的前向 Flyway migration 精确识别历史 migration 中 username 与 password hash 均匹配的已知默认管理员种子，并使该固定凭据不可认证。系统 MUST NOT 修改已执行的历史 migration，MUST NOT 仅按 username 删除用户，且 SHALL 保留可能被业务数据引用的用户 ID。

显式 bootstrap MAY 接管带精确隔离标记的历史种子，使用外部密码的 BCrypt hash 重新启用同一用户 ID 并确保 ADMIN 关联；它 MUST NOT 把相同逻辑应用到已修改密码的正常同名用户。

#### Scenario: 精确 known seed 被隔离

- GIVEN 数据库仍包含 username 与 password hash 均匹配历史 known seed 的用户
- WHEN 执行 C2 前向 migration
- THEN 该用户被禁用并写入不可认证的隔离标记
- AND 用户 ID 与业务数据引用保持不变
- AND 已知固定密码不能登录

#### Scenario: 已修改密码的同名用户不受影响

- GIVEN 数据库存在相同 username 但 password hash 已不同于历史 known seed 的用户
- WHEN 执行 C2 前向 migration
- THEN migration 不修改该用户的 password hash、enabled 状态、角色或 ID

#### Scenario: Bootstrap 接管隔离种子

- GIVEN bootstrap 显式启用
- AND 目标 username 对应精确隔离标记的历史种子
- AND 外部凭据合法且 ADMIN 角色存在
- WHEN bootstrap 执行
- THEN 系统保留原用户 ID
- AND 用外部密码的 BCrypt hash 替换隔离标记并启用用户
- AND ADMIN 角色关联存在且无重复记录

### Requirement: LLM Provider 有界重试与故障分类

系统 SHALL 将 LLM provider 的 429、5xx、请求 timeout、I/O 与连接失败分类为 transient failure，并且 MAY 仅在显式配置的非负 `max-retries` 预算内重试。`max-retries=N` 时单次同步问答或流式首个用户可见内容之前的 provider 总尝试数 MUST NOT 超过 `1+N`；`max-retries=0` MUST 只执行一次 provider 尝试。系统 MUST NOT 无限重试。

非重试型 4xx、配置错误以及 2xx malformed/empty response MUST NOT 自动重试。流式响应一旦已经输出至少一个用户可见内容 chunk，后续任何 provider failure MUST NOT 触发重新订阅或从头重放。

#### Scenario: 429 在显式预算内恢复

- GIVEN `max-retries=1`
- AND provider 第一次返回 429、第二次返回合法响应
- WHEN 发起同步问答或流式请求尚未输出用户可见内容
- THEN 系统总共执行 2 次 provider 尝试
- AND 返回第二次尝试的成功结果
- AND query count 只增加一次

#### Scenario: 503 重试耗尽

- GIVEN `max-retries=2`
- AND provider 每次均返回 503
- WHEN 发起问答
- THEN provider 总尝试数为 3
- AND 最终错误类别为 `provider_5xx`
- AND diagnostics 表明 `retryCount=2` 与 `retryExhausted=true`
- AND 系统不再追加第 4 次尝试

#### Scenario: 默认零重试

- GIVEN `max-retries=0`
- WHEN provider 返回 429、5xx、timeout 或 network failure
- THEN 系统只执行 1 次 provider 尝试
- AND 直接进入稳定 generation failure 语义

#### Scenario: 非重试型响应

- WHEN provider 返回 400、401、403、404 或合法 HTTP 2xx 但 body malformed/empty
- THEN 系统不自动重试
- AND 4xx 分类为 `provider_http_error`
- AND malformed/empty response 分类为 `invalid_response`

#### Scenario: 流式首个内容后失败

- GIVEN 流式 provider 已输出至少一个非空内容 chunk
- WHEN 后续发生 429、5xx、timeout、network 或解析失败
- THEN 系统不得重新订阅 provider
- AND 已输出内容不得从头重复
- AND 当前流进入稳定失败终态

### Requirement: LLM Generation 失败响应与副作用

LLM generation 最终失败时，系统 SHALL 返回稳定、机器可判定且不泄露 provider 或用户私密内容的失败结果。同步 `/api/qa/ask` SHALL 保持现有 HTTP 200 外层兼容，并在 `QAResponse.metadata.status` 中返回 `error`；answer MUST 仅包含稳定用户提示，citations 与 contexts MUST 为空。

失败 diagnostics MAY 包含 provider、固定 endpoint path、model、timeout、maxRetries、attemptCount、retryCount、retryExhausted、稳定 errorCategory 与可选 HTTP status，但 MUST NOT 包含 API key、Authorization header、provider response body、原始异常 message、question、prompt、context 或 snippet。

一次被接受的问答请求 SHALL 只增加一次 query count，不因 provider retry 重复计数。同步 generation failure、流式 failure 或部分输出后 failure MUST NOT 写入成功 QA cache，也 MUST NOT 保存为正常 QA history。

#### Scenario: 同步 generation 失败

- GIVEN retrieval 已返回 contexts
- WHEN LLM transient failure 在配置预算内仍未恢复
- THEN `/api/qa/ask` 返回 HTTP 200 外层兼容响应
- AND `metadata.status=error`
- AND answer 为稳定的暂不可用提示
- AND citations 与 contexts 为空
- AND QA cache 与 QA history 均不写入
- AND query count 只增加一次

#### Scenario: 流式首个内容前失败

- GIVEN 流式请求尚未输出用户可见内容
- WHEN provider failure 最终无法恢复
- THEN SSE 输出稳定 `[ERROR]` 提示后输出 `[DONE]` 并结束
- AND 不保存 QA history
- AND 错误 chunk 不包含 provider body、原始异常 message、prompt 或 context

#### Scenario: 流式部分输出后失败

- GIVEN 流式请求已输出部分答案
- WHEN provider 随后失败
- THEN 系统保留已发送内容但不得重放
- AND 追加稳定 `[ERROR]` 与 `[DONE]` 后结束
- AND 部分答案不得保存为正常 QA history

#### Scenario: 安全故障诊断

- GIVEN provider 错误 body 或异常链包含合成 secret、认证 header、prompt 或 context marker
- WHEN 系统构造客户端响应、diagnostics 和普通日志
- THEN 可观察输出只包含允许的稳定字段与错误类别
- AND 不包含上述敏感 marker 或原始 provider 内容

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

### Requirement: Milvus 依赖故障分类与稳定结果

系统 SHALL 在 Milvus adapter boundary 把连接拒绝、RPC timeout、SDK non-success status、collection/index 缺失、序列化失败和未知异常转换为稳定、安全且机器可判定的结果。系统 MUST NOT 依赖 SDK 原始 message 作为客户端契约，也 MUST NOT 把 dependency unknown 表达为 empty result、success 或零值。

已知在 operation 前失败的请求 SHALL 使用 `VECTOR_STORE_UNAVAILABLE`；确认 collection/index 缺失 SHALL 使用 `VECTOR_INDEX_UNAVAILABLE`；mutation 可能已被服务端接受但回执无法确认时 SHALL 使用 `VECTOR_OPERATION_OUTCOME_UNKNOWN`。系统 MUST NOT 声称 outcome unknown 已回滚或可以安全自动重试。

#### Scenario: SDK 在 operation 前连接失败

- GIVEN Milvus SDK 在 search/create/upsert/delete/drop/count 的 operation 前抛出连接异常
- WHEN consumer 处理该调用
- THEN 系统产生稳定 `VECTOR_STORE_UNAVAILABLE`
- AND 不把调用结果表达为成功、empty 或 zero
- AND 不向客户端或普通日志输出 SDK 原始 message

#### Scenario: Collection 已确认缺失

- GIVEN 数据库中的知识库指向一个已确认不存在的 Milvus collection
- WHEN 系统执行 search/read
- THEN 系统返回 `VECTOR_INDEX_UNAVAILABLE`
- AND 不把结果表达为 `no_result`
- AND 不在 read/search 路径自动创建空 collection

#### Scenario: Mutation 回执未知

- GIVEN Milvus mutation 已发送且服务端可能已经接受
- WHEN response timeout、disconnect 或非结构化失败使执行结果无法确认
- THEN 系统返回 `VECTOR_OPERATION_OUTCOME_UNKNOWN`
- AND 不声称 mutation 未执行或已回滚
- AND 不自动重放该 mutation

### Requirement: Milvus 检索部分降级

hybrid retrieval 中 Milvus dense route 不可用时，系统 MAY 仅在 keyword route 健康且返回非空 contexts 时继续生成，并 MUST 明确标记 `retrievalMode=keyword_only`、`retrievalDegraded=true` 和 `degradedDependency=milvus`。该响应 MUST NOT 被写入普通成功 QA cache，且 MUST NOT 被描述为完整 hybrid retrieval。

当 keyword route 被禁用、调用失败或返回空 contexts 时，系统 MUST 返回稳定 retrieval error，MUST NOT 表达 `metadata.status=no_result`，也 MUST NOT 调用 LLM generation。一次 vector dependency failure 后系统 MUST 停止剩余 query-variant vector calls，不得把 query variants 当作隐式 retry budget。

#### Scenario: Dense 失败但关键词证据可用

- GIVEN hybrid retrieval 已启用
- AND Milvus dense search 发生依赖故障
- AND keyword route 成功返回非空 contexts
- WHEN 用户发起问答
- THEN 系统使用 keyword contexts 继续既有 rerank/generation
- AND QA metadata 明确标记 keyword-only degradation
- AND 不写普通成功 QA cache
- AND query count 只增加一次

#### Scenario: Dense 失败且关键词路线被禁用

- GIVEN Milvus dense search 发生依赖故障
- AND keyword route 未启用
- WHEN 用户发起问答
- THEN 系统返回稳定 `VECTOR_STORE_UNAVAILABLE`
- AND 不返回 `no_result`
- AND LLM 调用次数为 0
- AND 不写成功 cache/history

#### Scenario: Dense 失败且关键词结果为空

- GIVEN Milvus dense search 发生依赖故障
- AND keyword route 成功但返回空 contexts
- WHEN 用户发起问答
- THEN 系统返回稳定 retrieval error
- AND 不把 dense unknown 解释为知识库没有答案
- AND LLM 调用次数为 0

#### Scenario: 健康态 hybrid 行为保持不变

- GIVEN Milvus 与 keyword route 均健康
- WHEN 用户发起 hybrid retrieval
- THEN 系统继续使用现有 query variants、BM25、RRF、rerank 和 final topK
- AND 不写入 degradation 标记

### Requirement: Milvus 索引写入与生命周期一致性

知识库 collection create 未明确成功前，系统 MUST NOT 返回知识库创建成功。文档 vector upsert 未明确成功时，document/task MUST NOT 进入 `COMPLETED`，系统 MUST NOT 持久化新的成功 chunks、contentHash 或 document count。Milvus vector mutation failure MUST NOT 进入无差别应用级自动重放。

文档 vector delete 或知识库 collection drop 未确认成功时，系统 MUST NOT 报告 canonical delete 成功，也 MUST NOT 仅记录 warning 后继续删除 SQL 事实。SQL rollback MUST NOT 被描述为外部 mutation 回滚。durable compensation、orphan reconciliation、rebuild 和 replay 属于后续 C5 change。

#### Scenario: Knowledge-base collection 创建失败

- GIVEN 数据库知识库创建流程尚未向客户端返回
- WHEN Milvus collection create 在 operation 前失败
- THEN 系统返回 HTTP 503 与 `VECTOR_STORE_UNAVAILABLE`
- AND 不返回可用知识库
- AND 不把 collection 初始化失败表达为创建成功

#### Scenario: Document upsert 在 mutation 前失败

- GIVEN 文档已进入异步索引
- WHEN Milvus upsert 在 mutation 前发生依赖故障
- THEN document 与 task 进入安全 `FAILED`
- AND task error 只包含稳定 code/message
- AND 不写新的 chunks、contentHash、`COMPLETED` 或 document count
- AND vector operation 不被自动重放

#### Scenario: Document upsert 结果未知

- GIVEN 文档 vector mutation 已发送
- WHEN 系统无法确认服务端是否完成写入
- THEN document 与 task 进入 `FAILED`
- AND error code 为 `VECTOR_OPERATION_OUTCOME_UNKNOWN`
- AND 响应不声称 vector 未写或建议安全自动重试

#### Scenario: Document vector 删除未确认

- GIVEN 文档存在 SQL chunk 和 Milvus vector
- WHEN vector delete 未明确成功
- THEN 系统不得报告文档删除成功
- AND 不把异常吞掉后继续提交 canonical SQL delete
- AND outcome unknown 时明确可能存在部分外部副作用

#### Scenario: Knowledge-base collection drop 未确认

- GIVEN 知识库删除流程正在执行
- WHEN Milvus collection drop 未明确成功
- THEN 系统不得报告知识库删除成功
- AND 不把 drop failure 仅记录为 warning 后继续提交 canonical SQL delete

### Requirement: Milvus 统计与安全诊断

Milvus vector count 是 statistics 响应的事实字段。count 读取失败时系统 MUST 返回 HTTP 503 与稳定 dependency code，MUST NOT 返回伪造的 `vectorCount=0`。本 change 不要求修改 statistics DTO shape。

Milvus failure 响应、异步 task error 和普通日志 MAY 记录 dependency、固定 subsystem、固定 operation、稳定 errorCategory、failMode、traceId、安全 SDK status code 和 exception type。系统 MUST NOT 记录 SDK raw message、host/port/endpoint/credential、collection、document/vector ID、query、content、metadata/filter、文件名、标题、prompt/context/snippet 或 mutation body。

#### Scenario: Vector count 读取失败

- GIVEN statistics endpoint 无法从 Milvus 读取 vector count
- WHEN 客户端请求知识库统计
- THEN 系统返回 HTTP 503 与 `VECTOR_STORE_UNAVAILABLE`
- AND 不返回 `vectorCount=0`

#### Scenario: 故障内容包含敏感 marker

- GIVEN SDK message、collection、query、content、metadata 或 endpoint 包含合成敏感 marker
- WHEN 系统生成客户端错误、task error、diagnostics 与普通日志
- THEN 输出只包含允许的固定安全字段和稳定类别
- AND 不包含上述 marker 或原始 SDK 内容

#### Scenario: 隔离 Milvus 重启恢复

- GIVEN 测试自有 Milvus container 已完成健康 search
- WHEN 仅该 Milvus container 被 stop 后再 start
- THEN outage 期间公开入口符合 keyword-only 或 stable failure 契约
- AND restart 后应用级 search 在有界等待内恢复
- AND 测试不枚举或操作用户常驻容器、volume、etcd 或 MinIO

### Requirement: 已接受索引任务的输入持久性

系统 SHALL 在返回已接受的 documentId/taskId 前，把上传输入完整写入应用管理的 durable storage，并持久化可由新进程解析的 opaque storage key、byte size、SHA-256 与 input state。系统 MUST NOT 把请求生命周期 stream、内存闭包或 system temp absolute path 作为已接受任务的唯一输入事实。

durable input 发布 SHALL 使用同一存储根内的 staging + atomic publish。发布、数据库关联或初始 task PENDING 状态任一在 acceptance 前发生已知失败时，系统 MUST NOT 返回可轮询的假任务，并 SHALL 清理可确定未被接受的 staging/object；清理失败 MUST 留下可协调事实，不得伪装成功。

#### Scenario: 输入持久化后任务被接受

- GIVEN 用户上传受支持的文档
- WHEN 系统返回 documentId 与 taskId
- THEN 对应输入已通过 atomic publish 完整存在于 durable storage
- AND 数据库保存 opaque key、byte size、SHA-256 与可用状态
- AND 新应用进程可不依赖原请求或原进程 temp 重新打开同一输入

#### Scenario: Atomic publish 失败

- GIVEN 上传流只写入 staging 或 atomic publish 失败
- WHEN 系统处理该上传
- THEN 不返回已接受 documentId/taskId
- AND 不让任务读取部分最终文件
- AND 清理可确定的 staging 数据或记录 cleanup pending

#### Scenario: 初始任务状态写入失败

- GIVEN durable input 与 document 关联已建立
- WHEN Redis 无法持久化初始 task PENDING 状态
- THEN 系统不返回假 taskId 且 task operation 不启动
- AND 对未接受的 document/input 执行确定性清理或保留明确可协调状态

### Requirement: 索引输入身份与完整性

系统 SHALL 使用与客户端文件名、绝对路径和解析后 `content_hash` 分离的 opaque storage key。任务读取输入时 MUST 验证 regular-file/root confinement、byte size 与 SHA-256；missing、路径逃逸、非普通文件或校验不一致 MUST 在 parser、embedding 和 vector mutation 前失败。

missing input SHALL 表达为稳定 `INDEX_INPUT_UNAVAILABLE`，完整性不一致 SHALL 表达为稳定 `INDEX_INPUT_CORRUPT`。客户端、task error 与普通日志 MUST NOT 暴露 storage root、storage key、绝对路径、原始文件名、标题、正文或底层异常原始 message。

#### Scenario: 新进程重新打开输入

- GIVEN 实例 A 已原子发布输入并持久化 key 与完整性事实
- AND 实例 A 已停止
- WHEN 实例 B 使用相同 durable root 处理该 document
- THEN 实例 B 按 key 打开与校验相同 bytes
- AND 不依赖实例 A 的闭包、stream 或 temp path

#### Scenario: Durable input 缺失

- GIVEN document 记录声明输入 AVAILABLE
- WHEN store 无法找到对应 regular file
- THEN document/task 返回 `INDEX_INPUT_UNAVAILABLE`
- AND parser、embedding 与 vector mutation 调用次数为 0
- AND 不把缺失输入表达为文档无内容或索引完成

#### Scenario: Durable input 被截断或替换

- GIVEN store 中 bytes 的 size 或 SHA-256 与持久化事实不一致
- WHEN task 尝试读取输入
- THEN document/task 返回 `INDEX_INPUT_CORRUPT`
- AND 不继续解析或写入任何索引

#### Scenario: Storage key 尝试逃逸 root

- GIVEN storage key 是绝对路径、包含 traversal 或解析到 root 外的符号链接
- WHEN store 解析该 key
- THEN 操作 fail closed
- AND 不读取、覆盖或删除 configured root 外文件

### Requirement: 索引输入生命周期与清理事实

系统 SHALL 明确区分 `AVAILABLE`、`CLEANUP_PENDING`、`CLEANED`、`MISSING` 与 `CORRUPT` 输入状态。健康索引完成后系统 SHALL 尝试最小化保留原始输入；清理成功后标记 CLEANED，清理失败时保持业务索引结果并标记 CLEANUP_PENDING，不得报告输入已删除。

索引 FAILED、进程中断或 vector mutation outcome unknown 时，系统 MUST NOT 无条件删除仍可用于后续协调的 AVAILABLE 输入。C5a MUST NOT 因输入可用而自动重放任务；orphan detection、lease/claim、replay 与 resume 属于 C5b。

#### Scenario: 健康索引完成并清理

- GIVEN 输入 AVAILABLE 且文档索引明确 COMPLETED
- WHEN durable input 删除成功
- THEN input state 变为 CLEANED
- AND 后续不把该输入表达为可恢复

#### Scenario: 完成后的清理失败

- GIVEN 文档索引已明确 COMPLETED
- WHEN durable input 删除失败
- THEN 文档索引结果保持 COMPLETED
- AND input state 为 CLEANUP_PENDING
- AND 系统不声称原始输入已删除

#### Scenario: 索引失败或 outcome unknown

- GIVEN task FAILED、进程中断或 vector mutation outcome unknown
- WHEN C5a 处理任务终态或中断窗口
- THEN 仍可校验的输入保持 AVAILABLE
- AND 系统不自动 replay vector mutation
- AND 后续协调由 C5b 决定

#### Scenario: Canonical document delete

- GIVEN document 仍关联 AVAILABLE 或 CLEANUP_PENDING 输入
- WHEN canonical document delete 执行
- THEN 系统尝试清理 durable input
- AND 清理失败不得被记录为输入已删除

### Requirement: 旧记录与部署边界

新增输入字段 SHALL 对旧 document 行保持 nullable compatibility。旧 COMPLETED document 没有 durable input 时系统 MAY 继续提供既有已索引结果；旧 PENDING/FAILED document 没有 durable input 时系统 MUST 返回稳定 unavailable，MUST NOT 猜测 system temp 文件、伪造可恢复性或自动重跑。

production profile SHALL 显式配置可写、非 system temp 的 durable root，并在启动时验证；root 缺失、不可写或不满足持久化约束时系统 MUST fail fast，不得静默退回 system temp 或 memory。

#### Scenario: 旧已完成文档没有输入

- GIVEN 升级前 document 已 COMPLETED 且没有 storage key
- WHEN 新版本读取该文档或执行检索
- THEN 已有索引结果保持可用
- AND 系统不把该行标记为可恢复输入

#### Scenario: 旧未完成文档没有输入

- GIVEN 升级前 document 为 PENDING/FAILED 且没有 storage key
- WHEN 系统评估其输入
- THEN 返回 `INDEX_INPUT_UNAVAILABLE`
- AND 不搜索或猜测 system temp 文件
- AND 不自动启动索引任务

#### Scenario: Production durable root 无效

- GIVEN production profile 的 durable root 缺失、不可写或位于 system temp
- WHEN 应用启动
- THEN 启动 fail fast 并给出不含绝对路径的稳定配置错误
- AND 不退回 system temp 或 memory storage
+

### Requirement: 文档索引任务的 durable ledger

系统 SHALL 为每个新接受的文档索引任务持久化稳定 taskId、document/owner 关联、执行 status/phase、attempt、lease 与安全 failure facts。MySQL ledger SHALL 是 document index task 的 durable source of truth；Redis MAY 作为低延迟状态投影，但 TTL、miss 或进程退出 MUST NOT 抹掉已接受任务身份。

客户端获得 taskId 前，系统 SHALL 按 durable input/document、DB ledger、Redis initial projection、local scheduling 的顺序建立 acceptance。任一已知失败 MUST NOT 返回可轮询的假任务，task operation 调用次数 MUST 为 0。

durable task payload/result MUST NOT 保存 input bytes、storage key、绝对路径、原文件名、标题、正文、chunks、embedding/vector payload、provider raw message 或凭据。

#### Scenario: 新任务完成 durable acceptance

- GIVEN 用户上传受支持文档且 C5a input 已原子发布
- WHEN 系统返回 documentId 与 taskId
- THEN DB ledger 已保存稳定 task identity、owner 与 ACCEPTED phase
- AND Redis 已保存可查询的 initial projection
- AND taskId 在后续 resume attempts 中保持不变

#### Scenario: Ledger 或 initial projection 失败

- GIVEN durable input/document 已建立
- WHEN DB ledger 或 Redis initial PENDING projection 写入失败
- THEN 系统不返回假 taskId
- AND task operation 调用次数为 0
- AND document/input/ledger 留下可清理或可协调的明确事实

#### Scenario: Redis projection TTL 或 miss

- GIVEN taskId 已由系统接受且 DB ledger 仍存在
- AND Redis 正常但 projection 已过期或缺失
- WHEN owner 查询 task status
- THEN 系统从 DB ledger 返回 sanitized durable status
- AND best-effort 重建 Redis projection
- AND 不把任务误报为 404

### Requirement: 数据库任务 claim 边界

系统 SHALL 通过数据库条件更新竞争 document index task lease，并使用数据库时间判断 lease 与 next-attempt 是否到期。只有条件更新成功的 worker MAY 执行后续分类或恢复；失败竞争者 MUST 跳过该任务。

lease 过期只表示任务可被重新 claim 和分类，不表示任何外部 mutation 未发生。VECTOR_IN_FLIGHT 任务即使 lease 过期也 MUST NOT 因此自动重放 vector mutation。

#### Scenario: 两个 worker 竞争同一任务

- GIVEN 两个 worker 尝试 claim 同一个可协调 task
- WHEN 两者执行 durable compare-and-set
- THEN 至多一个条件更新成功
- AND 失败竞争者不执行恢复或外部副作用

#### Scenario: Lease 过期后的重新分类

- GIVEN task 非终态且 lease 已按数据库时间过期
- WHEN 新 worker 成功 claim
- THEN 系统仍按 durable phase 决定后续动作
- AND 不把 lease 过期解释为 vector mutation 未执行

### Requirement: 保守的 phase-aware resume

系统 SHALL 在文档索引副作用边界持久化 ACCEPTED、SAFE_PRE_VECTOR、VECTOR_IN_FLIGHT、VECTOR_CONFIRMED、FINALIZING 与终态 phase。新 C5b task SHALL 使用 versioned、可重现的 chunk/vector identity，并持久化 content、chunk config 与 chunk count 的安全 contract facts。

只有 SAFE_PRE_VECTOR，或已明确 VECTOR_CONFIRMED 且不再执行 vector upsert 的收尾路径，MAY 在显式启用 resume 后执行。VECTOR_IN_FLIGHT 或 `VECTOR_OPERATION_OUTCOME_UNKNOWN` MUST 转为 `RECONCILIATION_REQUIRED`，MUST NOT 自动 vector replay。恢复时 contract 或重新解析 facts 不一致 MUST fail closed。

#### Scenario: Pre-vector 中断后安全续跑

- GIVEN task phase 为 SAFE_PRE_VECTOR、input AVAILABLE 且恢复开关已显式启用
- AND persisted contract 与当前 runtime 一致
- WHEN worker 恢复该 task
- THEN 系统沿用相同 taskId 与 deterministic vector IDs
- AND 可重新 parse/embed 并继续索引

#### Scenario: Vector in-flight 中断

- GIVEN task phase 为 VECTOR_IN_FLIGHT 或 failure 为 `VECTOR_OPERATION_OUTCOME_UNKNOWN`
- WHEN worker 协调该 task
- THEN task 进入 `RECONCILIATION_REQUIRED`
- AND vector mutation 调用次数为 0
- AND 系统不声称原 mutation 未执行、已回滚或可以安全自动重试

#### Scenario: Vector confirmed 后收尾

- GIVEN ledger 明确记录 VECTOR_CONFIRMED
- WHEN worker 按相同 contract 恢复
- THEN 系统重新解析并校验 content hash 与 chunk count
- AND 只执行 DB、keyword 与 input cleanup 收尾
- AND vector upsert 调用次数为 0

#### Scenario: Resume contract 不一致

- GIVEN chunk config/version、content hash 或 chunk count 与 ledger 不一致
- WHEN worker评估恢复
- THEN 恢复 fail closed
- AND embedding/vector mutation 调用次数为 0
- AND 客户端与普通日志不暴露不一致的原始内容

### Requirement: Cleanup reconciliation 与恢复开关

document 已 COMPLETED 且 input state 为 CLEANUP_PENDING 时，系统 MAY 有界扫描并重试幂等 input delete；该路径 MUST NOT 调用 parser、embedding、vector mutation、rerank 或 generation。delete 返回 DELETED 或 ALREADY_MISSING 时系统 SHALL 标记 CLEANED；失败时 SHALL 保持 CLEANUP_PENDING。

reconciliation classification 与会触发 provider 的 auto resume SHALL 使用独立开关。会重新 embedding 的 auto resume 默认 MUST 为关闭；只有显式启用且调用预算获授权后才能运行。

恢复 diagnostics MUST NOT 记录 storage root/key、文件名、标题、正文、chunks、embedding input、vector payload、provider raw message、credential、Redis value 或 task DB payload。

#### Scenario: Cleanup pending 重试成功

- GIVEN document 已 COMPLETED 且 input state 为 CLEANUP_PENDING
- WHEN coordinator 幂等删除 input 得到 DELETED 或 ALREADY_MISSING
- THEN input state 变为 CLEANED
- AND parser、embedding 与 vector mutation 调用次数为 0

#### Scenario: Cleanup pending 重试失败

- GIVEN input delete 返回 FAILED 或稳定 cleanup error
- WHEN coordinator 处理该结果
- THEN input state 保持 CLEANUP_PENDING
- AND 不把失败伪装成 CLEANED

#### Scenario: Auto resume 未启用

- GIVEN reconciliation scan 已启用但 resume 开关关闭
- WHEN 发现 SAFE_PRE_VECTOR task
- THEN embedding/vector mutation 调用次数为 0
- AND task 不被伪装成已恢复完成

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

### Requirement: NVIDIA Ranking 协议适配

系统 SHALL 在显式选择且完整配置 `nvidia` reranker 时，使用 typed NVIDIA ranking contract 调用配置的 HTTP endpoint。请求 MUST 包含 model、`query.text`、按候选顺序构造的 `passages[].text` 与显式 truncate policy；响应 MUST 按 `rankings[].index/logit` 解释。系统 MUST NOT 把 NVIDIA raw logit 表述为已校准概率或跨样本可直接比较的 relevance probability。

NVIDIA adapter MUST 验证 rankings 非空、index 唯一且不越界、完整覆盖本次候选并且 logit 为有限数值。候选数量 MUST NOT 超过配置与 provider contract 的上限。首版仅支持 text query 与 text passages。

#### Scenario: 合法 NVIDIA rankings

- GIVEN `provider=nvidia` 且配置完整
- AND 本次候选均为非空 text passages
- WHEN provider 返回完整唯一的 `rankings[].index/logit`
- THEN 系统按 logit 降序决定 final rerank order
- AND 每个 index 精确映射回原候选
- AND 原 retrieval score 保留，raw logit 与 rerank rank 作为独立 metadata
- AND 系统不把 logit 命名或展示为概率

#### Scenario: 非法或不完整 rankings

- WHEN provider 返回空 rankings、重复/越界/缺失 index、非法 logit 或未完整覆盖候选
- THEN 本次 NVIDIA 结果不得部分生效
- AND 系统整次使用 heuristic fallback 或返回稳定失败
- AND diagnostics 使用稳定 `invalid_response` 或 `incomplete_rankings`，不包含 raw body

#### Scenario: 外调前输入不满足协议

- WHEN query/passages 为空、候选超过 provider 上限或 NVIDIA 配置不完整
- THEN 系统不发送 ranking HTTP 请求
- AND model call count 为 0
- AND 使用 heuristic fallback 或稳定返回无候选结果

### Requirement: Reranker Requested 与 Effective Provider 归因

每次启用 rerank 的 retrieval SHALL 产生结构化归因，至少包含 requested provider、effective provider、fallback count/reason、实际 model call count、candidate count、scored count、coverage、rerank latency、model 与 protocol。requested provider 表示配置意图，effective provider 表示实际决定 final order 的唯一 provider；两者 MUST NOT 因 fallback 被混为同一事实。

归因 MUST 使用稳定字段与枚举，MUST NOT 包含 API key、Authorization、query、passages/context、raw provider response、原始异常 message 或 stack trace。单次样本的 fallback count SHALL 为 0 或 1；C6 首版对 NVIDIA 的 model call count SHALL 为 0 或 1，不自动 retry。

#### Scenario: NVIDIA 成功生效

- GIVEN requested provider 为 `nvidia`
- WHEN 一个合法 ranking 请求成功且完整覆盖候选
- THEN effective provider 为 `nvidia`
- AND fallback count 为 0
- AND model call count 为 1
- AND candidate/scored coverage 为 100%
- AND protocol 标识为稳定 NVIDIA ranking 版本

#### Scenario: 调用前不可用

- GIVEN requested provider 为 `nvidia`
- WHEN 配置不完整或显式 health check 失败
- THEN effective provider 为 `heuristic`
- AND fallback count 为 1
- AND fallback reason 为 `not_configured` 或 `health_check_failed`
- AND model call count 为 0

#### Scenario: 调用后失败

- GIVEN requested provider 为 `nvidia`
- WHEN ranking request timeout、网络失败、HTTP 4xx/5xx 或响应无效
- THEN effective provider 为 `heuristic`
- AND fallback count 为 1
- AND model call count 为 1
- AND fallback reason 使用稳定分类而非原始异常内容

#### Scenario: 默认 heuristic

- GIVEN tracked 默认配置未被覆盖
- WHEN 系统执行 rerank
- THEN requested 与 effective provider 均为 `heuristic`
- AND model call count 与 fallback count 均为 0
- AND 系统不访问 NVIDIA endpoint

### Requirement: Rerank Fallback 与 Retrieval Diagnostics 合并

Rerank outcome SHALL 通过显式返回值进入 `RetrievalResult.diagnostics`，不得只依赖普通日志、ThreadLocal 或单个 context metadata。rerank diagnostics SHALL 与 vector/keyword route diagnostics 合并；当 Milvus degradation 与 rerank fallback 同时发生时，两类事实 MUST 同时保留且不得互相覆盖。

Rerank fallback 本身 MUST NOT 改写既有 retrieval degradation/cache 语义，除非后续独立 change 明确修改。provider 返回部分 rankings 时首版 MUST 整次 fallback，不得形成无法单值归因的 model/heuristic 混合排序。

#### Scenario: Milvus 降级且 NVIDIA fallback

- GIVEN dense route 不可用但 keyword evidence 可用
- AND requested reranker 为 `nvidia`
- WHEN NVIDIA ranking 同时失败
- THEN returned contexts 使用 keyword route 与 heuristic rerank
- AND diagnostics 同时包含 `retrievalMode=keyword_only`、Milvus degradation 与 NVIDIA fallback facts
- AND effective rerank provider 仅为 `heuristic`

#### Scenario: Rerank 未启用

- GIVEN request 明确 `enableRerank=false`
- WHEN retrieval 返回 contexts
- THEN diagnostics 标明 rerank disabled 或不产生 provider 调用
- AND model call count 为 0
- AND contexts 保持既有 retrieval order

### Requirement: Debug、同步问答与评测逐样本归因

同步 QA response metadata 与 debug retrieval response SHALL 暴露同一套 sanitized reranker attribution。debug retrieval MUST 使用包含 diagnostics 的检索入口，使 retrieval-only 运行也能证明 requested/effective provider 与 fallback；同步 QA MUST 报告实际用于生成 contexts 的 attribution。

评测 runner SHALL 逐样本保存 reranker attribution，并聚合 effective provider sample count、model coverage、fallback count/reason、model call count 与 candidate coverage。C6 attribution SHALL NOT 改变既有 Report status、Recall@3/5、MRR、Top1、generation、citation、no-answer 或 judge 指标语义，也 SHALL NOT 自行宣称 NVIDIA 相对 heuristic 的业务收益。

#### Scenario: Retrieval-only 报告包含 provider coverage

- GIVEN 评测以 `skipAsk=true` 运行多个样本
- WHEN 部分样本 NVIDIA 成功、部分样本 fallback
- THEN 每个样本记录 requested/effective provider 与调用/fallback facts
- AND 报告分别聚合 effective NVIDIA 与 heuristic 样本数
- AND 部分 fallback 不得被描述为 100% model coverage
- AND retrieval metrics 仍按既有顺序与公式计算

#### Scenario: 同步问答透传安全归因

- GIVEN 同步问答完成 retrieval 并生成答案
- WHEN 构造 `QAResponse.metadata`
- THEN metadata 包含本次实际 contexts 的 sanitized rerank attribution
- AND 不包含 query、passages、provider raw body、异常 message、API key 或 Authorization

## ADDED Requirements

### Requirement: 分离的 Ingest 与 Ask Trace 身份

启用 GenAI tracing 时，系统 SHALL 使用 OpenTelemetry API/SDK 为 document ingest 与 ask 建立彼此分离的 trace。durable ingest task MUST 使用独立 root span；upload/request context 只可作为可选 span link，不得成为跨异步执行或重启恢复的强制 parent。ask trace MUST NOT 把历史 ingest span 当作 parent/child。

tracing 关闭或 telemetry 内部失败时，系统 MUST 保持既有 QA/indexing 业务行为；telemetry MUST fail open，但不得吞掉或改写业务异常、retry、fallback、持久化与返回语义。

#### Scenario: 上传接受后异步索引

- GIVEN 文档上传请求已创建稳定 taskId 并返回 202
- WHEN durable task 在线程池中开始执行
- THEN 系统创建新的 `rag.ingest` root span
- AND 若 submission context 仍可用则以 span link 关联
- AND ingest span 不作为 upload request span 的 child

#### Scenario: 重启后恢复索引任务

- GIVEN durable task 在进程重启后恢复且原 submission context 不可用
- WHEN reconciliation/resume 开始执行
- THEN 系统仍创建合法独立 `rag.ingest` root
- AND 使用稳定 task/phase/resume 属性表达恢复事实
- AND 不因缺少历史 trace context 拒绝恢复

#### Scenario: Ask 使用历史索引产物

- GIVEN ask 检索到此前 ingest 产生的 chunks
- WHEN 系统建立 `rag.ask` trace
- THEN ask 与 ingest 保持不同 trace identity
- AND 通过稳定 task/document/chunk lineage 关联产物
- AND 不持久化或依赖 ingest trace/span id

### Requirement: GenAI 阶段 Span 拓扑与生命周期

系统 SHALL 使用固定、非动态 span names 表达实际执行的 ask 与 ingest stages。未执行的 cache、retrieval route、rerank、generation、citation、keyword write 或 finalize 阶段 MUST NOT 产生伪 span。business no-result MUST NOT 被标为 telemetry error。

同步 ask SHALL 在返回或抛错时结束；流式 ask SHALL 在 complete、error、cancel 或 timeout 的真实终态结束且只结束一次。异步线程、Reactor/SSE 回调完成后 context 与 MDC MUST 被恢复或清理，不得泄漏到后续请求/任务。

#### Scenario: Cache Hit 跳过下游阶段

- GIVEN ask 启用 cache 且命中有效响应
- WHEN `rag.ask` 完成
- THEN trace 包含 cache lookup 与 `CACHE_HIT` outcome
- AND 不包含 retrieval、rerank、LLM 或 citation spans

#### Scenario: 流式问答被客户端取消

- GIVEN streaming ask 已开始并产生部分 chunks
- WHEN client disconnect 或 subscription cancel
- THEN `rag.ask` 以 `CANCELLED` 终态结束一次
- AND span duration 覆盖到取消时刻
- AND 不逐 token 创建 span/event

#### Scenario: Provider Retry 后成功降级

- GIVEN LLM 或 reranker 按既有契约发生 retry/fallback
- WHEN 最终业务请求成功
- THEN stage span 记录 bounded retry events、attempt/retry counts、requested/effective provider 与安全 fallback reason
- AND ask outcome 反映最终业务结果
- AND 不记录 raw provider response 或异常 message/stack

### Requirement: 稳定的 Ingest-to-Ask Lineage

ingest 产物 SHALL 使用稳定 `ingestTaskId`、`documentId` 与确定性 `chunkId` 建立 lineage。ask SHALL 只对最终选择的有界 contexts 记录 lineage events，其数量 MUST NOT 超过最终 selected topK。lineage id/rank/score MUST NOT 用于动态 span name、普通日志或 metrics labels。

旧索引缺少部分或全部 lineage 字段时，系统 SHALL 分别表达 `PARTIAL` 或 `MISSING`，但 MUST NOT 因 telemetry lineage 不完整改变检索、生成或响应结果。

#### Scenario: 完整 Lineage Round Trip

- GIVEN ingest 将 task/document/chunk identity 写入 vector/keyword metadata
- WHEN ask 最终选择对应 retrieved context
- THEN ask trace 产生有界 `rag.lineage.context` event
- AND event 可关联 ingest task、document 与 chunk
- AND lineage status 为 `COMPLETE`

#### Scenario: 旧索引缺少 Ingest Task Id

- GIVEN retrieved context 仍有 document/chunk identity 但没有 `ingestTaskId`
- WHEN ask 记录 lineage
- THEN lineage status 为 `PARTIAL`
- AND 系统保留可用 identity
- AND QA 结果与不启用 tracing 时一致

#### Scenario: 候选多于最终 TopK

- GIVEN retrieval/rerank 产生的候选数大于最终 selected topK
- WHEN ask 记录 lineage events
- THEN 只记录最终 selected contexts
- AND event 数量不超过 topK
- AND 未选择候选的 id/score 不进入普通 telemetry

### Requirement: OTel Context 兼容、隐私与进程内验证

启用 tracing 时，系统 SHALL 优先提取合法 W3C `traceparent`，仅在其不存在时兼容严格合法的 `X-Trace-Id/X-Span-Id` pair，并将当前 OTel trace/span id 同步到既有 MDC 与响应头。非法或不完整 header MUST NOT 成为 remote parent。

普通 telemetry MUST NOT 包含 raw question、prompt、answer、context、citation snippet、document content、file name/title、collection name、username/user id、credential、Authorization、provider raw body、异常 message 或 stack trace。系统 MUST NOT 使用默认 exception recording 绕过该限制。C11 SHALL 使用 in-memory exporter/fake dependencies 验证 topology、context、lineage、status 与隐私，不配置网络 exporter、metrics、告警或部署采样。

#### Scenario: W3C 与 Custom Header 同时存在

- GIVEN request 同时携带合法 W3C `traceparent` 与合法 custom trace/span headers
- WHEN tracing filter 提取 remote parent
- THEN W3C context 优先
- AND MDC/响应头使用当前 OTel context identity
- AND custom headers 不创建第二套 active context

#### Scenario: 敏感 Sentinel 贯穿失败路径

- GIVEN question/prompt/context/file/provider error 各包含唯一敏感 sentinel
- WHEN ask 或 ingest 失败并导出到 in-memory exporter
- THEN span name、attributes、events 与 status 中均不包含 sentinel
- AND 只保留 allowlisted error type/category/code
- AND 不记录异常 message 或 stack trace

#### Scenario: C11 无外部 Telemetry 传输

- GIVEN C11 runtime 或测试启用 tracing
- WHEN ask/ingest spans 被创建与验证
- THEN runtime 不注册 OTLP/Zipkin/Jaeger 等 network exporter
- AND metrics、alerts、dashboard、production sampling 与 deployment config 保持 out of scope
- AND 真实 provider/exporter 调用与数据出站为 0

## ADDED Requirements

### Requirement: Default-Off OTLP Signal Export And Fail-Open Lifecycle

系统 SHALL 对 tracing、metrics 与 network export 提供相互独立的显式开关，且 tracked 默认值全部为关闭。默认启动 MUST NOT 创建 OTLP network connection 或新增公共 management endpoint。显式启用 export 时，系统 SHALL 使用 OTLP gRPC 将已启用 signals 发送到配置的本机 Collector。

exporter SHALL 使用 bounded queue、batch、interval 与 timeout。初始化失败、backend unavailable、queue pressure、export timeout 或 shutdown flush failure MUST NOT 改变 ask/indexing 的成功、失败、retry、fallback、持久化或响应语义。export diagnostics MUST NOT 包含 endpoint、headers、credential、异常 message 或 stack trace。

#### Scenario: 默认启动不外发遥测

- GIVEN tracked 默认配置未被覆盖
- WHEN 应用启动并执行 ask 或 ingest
- THEN 不注册 network exporter 或 periodic metric reader
- AND 不连接 OTLP endpoint
- AND C11 disabled/legacy 行为保持不变

#### Scenario: 仅启用 Metrics Export

- GIVEN metrics 与 export 显式开启而 tracing 保持关闭
- WHEN ask 或 ingest 执行
- THEN metrics 通过 OTLP 发送到配置的本机 Collector
- AND 不创建或导出 C11 spans
- AND metrics completeness 不依赖 trace context

#### Scenario: Collector 不可用

- GIVEN export 已启用且 Collector unreachable、queue full 或 export timeout
- WHEN 业务请求或索引任务完成
- THEN 业务结果与 exporter 可用时一致
- AND telemetry failure 只形成 bounded safe diagnostics/drop facts
- AND 不记录 endpoint、credential 或 raw exception content

### Requirement: Low-Cardinality GenAI Stage Metrics

系统 SHALL 为 ask/ingest operation count、duration、in-flight、固定 stage duration、actual provider calls、fallback、actual token usage 与 token-usage coverage 提供固定 instrument name、type、unit 和 histogram boundaries。metrics SHALL 在真实 lifecycle 上记录且独立于 trace sampling；未执行阶段、retry 或 fallback MUST NOT 造成 operation 分母重复计数。

metric labels MUST 限于 bounded operation、stage、outcome、provider、retrieval route、fallback reason 与 token direction taxonomy；未知值 SHALL 归一为 `unknown/other`。task/document/chunk/user/KB/QA/trace/span id、rank、score、topK、question、prompt、answer、context、snippet、file/title/collection、model 自由文本、endpoint、credential 与 error detail MUST NOT 成为 metric label。

#### Scenario: 普通成功 Trace 被采样丢弃

- GIVEN 一次成功 ask trace 未被 Collector 保留
- WHEN metric points 被聚合
- THEN operation count/duration、实际 stage、provider call 与 applicable token coverage 仍各自正确记录
- AND 分母不因 trace sampling 缩小
- AND metric 不伪造到不存在的 trace 关联

#### Scenario: 高基数输入不能扩张 Series

- GIVEN 多次请求携带不同 document/chunk/task/user id、model 文本、score 与 error code
- WHEN 系统记录同一 bounded operation/stage/outcome
- THEN metric label set 不包含这些输入
- AND series 数量只由批准的 bounded taxonomy 决定
- AND 非法或未知枚举归一为 `unknown/other`

#### Scenario: Token Usage 缺失

- GIVEN provider 未返回 actual token usage
- WHEN generation stage 完成
- THEN `rag.token.usage` 不以 0 或估算值填充 actual usage
- AND coverage 记录 usage missing
- AND prompt estimated token 如保留仍不冒充 provider actual usage

### Requirement: Local Reference Backend Sampling Retention And Access Boundary

C12 SHALL 提供独立、可选的本机 reference deployment，以 Collector 接收 OTLP、Tempo 保存 trace、Prometheus 保存 metrics、Grafana 查询两类信号。reference deployment MUST NOT 隐式加入默认 compose 启动；只有 OTLP 与 Grafana 可绑定宿主 localhost，Tempo、Prometheus 与 Collector internal endpoints MUST 保持内部可见。

Collector SHALL 全量保留 error、timeout、cancel 与 fallback traces，并按固定可配置比例保留其他成功 traces；metrics MUST NOT 被采样。reference trace retention SHALL 为 72 小时、metric retention SHALL 为 7 天。Grafana anonymous access MUST 关闭，credential MUST 由未跟踪 secret/env 注入；这些值 SHALL 被描述为本机开发默认而非生产 SLA、合规或容量承诺。

#### Scenario: Reference Stack 未启动

- GIVEN 默认 compose 或应用单独启动
- WHEN C12 reference profile 未被显式选择
- THEN Collector、Tempo、Prometheus 与 Grafana 不启动
- AND 应用不因 reference stack 缺失而失败
- AND 不产生公网或 SaaS telemetry 出站

#### Scenario: 本机 Trace Sampling

- GIVEN synthetic traces 同时包含 error/fallback 与普通 success outcomes
- WHEN Collector tail sampling 完成
- THEN error、timeout、cancel 与 fallback traces 全部进入 Tempo
- AND 普通 success 只按配置比例进入 Tempo
- AND 对应 metrics 全部进入 Prometheus

#### Scenario: 未授权访问 Backend

- GIVEN 用户未提供 Grafana credential 或尝试直接访问 backend internal port
- WHEN 请求 dashboard、Tempo 或 Prometheus
- THEN Grafana 不允许 anonymous access
- AND Tempo、Prometheus 与 Collector internal endpoints 不通过宿主端口暴露
- AND tracked files 不包含可用 credential

### Requirement: Queryable Dashboard Local Rules And Synthetic Verification

reference deployment SHALL provision 可查询的 Tempo/Prometheus datasources、RAG overview dashboard 与本地 Prometheus alert rules。dashboard SHALL 覆盖 ask/ingest traffic、outcome、in-flight、stage latency、provider/fallback、actual token coverage 和 Collector/export state。alert rules SHALL 至少覆盖带最小流量门槛的 ask error ratio、provider fallback ratio 与 Collector receive/export/drop failure，并标明其为 non-SLA reference defaults。

C12 acceptance SHALL 使用 synthetic spans/metrics 验证 export、query、sampling、retention、access、privacy 与 fail-open，不得为了观测验收调用真实 embedding、rerank、ask、generation、judge、LLM 或其他业务 provider。notification delivery、production latency SLA、SaaS backend 与跨主机 telemetry SHALL 保持 out of scope。

#### Scenario: Dashboard 和 Rules 可加载

- GIVEN reference stack 使用固定版本配置启动
- WHEN Grafana provisioning 与 Prometheus rules 被加载
- THEN dashboard 可查询规定的 trace/metric panels
- AND rule expressions 可解析并展示 pending/firing 状态
- AND variables、legend、annotations 与 alert labels 不含 lineage 或用户高基数内容

#### Scenario: 敏感 Sentinel 进入导出路径

- GIVEN synthetic question、prompt、context、file、user、credential 与 error 各含唯一 sentinel
- WHEN trace 与 metrics 经 Collector 导出并被查询
- THEN span allowlist 继续阻止 C11 禁止内容
- AND metric names、labels、resource、dashboard 与 alert output 均不含 sentinel
- AND Collector 不用 logging/debug exporter 把业务 telemetry 复制到普通日志

#### Scenario: Synthetic 验收不触发业务外调

- GIVEN C12 integration verification 被执行
- WHEN 生成测试 traces、metrics、failure 与 recovery evidence
- THEN 不调用真实 embedding、rerank、debug retrieval、ask、generation、judge、LLM 或 provider endpoint
- AND 不发送知识库、用户内容或 telemetry 到公网/SaaS
- AND 结果只证明本机 reference observability 闭环，不证明 production readiness 或 SLA

## ADDED Requirements

### Requirement: Durable Tenant Model And Legacy Data Migration

系统 SHALL 建立持久化 tenant 模型，并使每个数据库用户与知识库关联一个非空 tenant identity。C13a SHALL 固定一名用户只有一个 active tenant；`knowledge_base` 的 tenant identity SHALL 独立持久化，不得仅在运行时通过 owner 间接猜测。

新的前向 Flyway migration SHALL 创建唯一、稳定的 legacy tenant，并将所有既有用户与知识库原地回填到该 tenant。migration MUST 保留 user/knowledge-base 主键、owner/public 字段、逻辑删除状态与既有 `kb_permission` 关系，MUST NOT 修改已执行的 V1-V9 migration 或清空旧数据。

#### Scenario: V9 旧库升级

- GIVEN V9 数据库包含正常、禁用或逻辑删除用户，以及 public/private 知识库和既有 KB permission
- WHEN Flyway 升级到 C13a migration
- THEN 唯一 legacy tenant 被创建
- AND 每个既有 user 与 knowledge_base 都获得非空 tenant identity
- AND 原 user/KB ID、owner/public、删除状态与 permission 关系保持不变

#### Scenario: Fresh Install

- GIVEN 一个空数据库从 V1 执行到 latest
- WHEN C13a migration 完成并通过 Flyway validate
- THEN tenant schema、legacy tenant、user tenant column 与 knowledge_base tenant column 均存在
- AND 后续认证或 KB 创建不能写入 null tenant identity

#### Scenario: 不完整回填

- GIVEN migration 无法为任意 user 或 knowledge_base 建立有效 tenant identity
- WHEN 应用尝试完成 schema 升级或启动
- THEN migration 或启动 fail closed
- AND 系统不得以 null/unknown tenant context 进入可服务状态

### Requirement: Server-Issued Tenant Identity In Authentication

数据库用户记录 SHALL 是 tenant identity 的认证事实源。登录时系统 SHALL 从数据库加载 userId、tenantId、状态与角色，并由服务端把 tenantId 写入 access token 与 refresh token；客户端 header、query、body、cookie 或 metadata MUST NOT 创建、选择或覆盖 tenant identity。

tenant claim 缺失、类型错误、非正数或无法形成有效认证 principal 时，系统 MUST 拒绝认证且不得回退到固定 tenant、客户端输入或未验证默认值。错误响应与普通日志 MUST NOT 回显 token、tenant 原始输入、数据库内部值或 credential。

#### Scenario: 数据库用户登录

- GIVEN 数据库中存在已启用且 tenant identity 有效的用户
- WHEN 用户使用现有登录 DTO 成功认证
- THEN access token 与 refresh token 都由服务端携带该数据库 tenantId
- AND authenticated principal 同时包含一致的 userId、tenantId 与当前角色
- AND 客户端不需要也不能提交 tenant selector

#### Scenario: 旧 Token 缺少 Tenant Claim

- GIVEN token 签名与过期时间有效但没有 tenant claim
- WHEN token 用于 access、validate 或 refresh
- THEN 系统 fail closed 并要求重新登录
- AND 不把该 token 静默映射到 legacy tenant

#### Scenario: 客户端伪造 Tenant

- GIVEN 已认证 token 属于 tenant A
- WHEN 请求 header、query、body 或 metadata 声称 tenant B
- THEN authenticated tenant context 仍只能来自服务端签发的 tenant A identity
- AND 客户端值不能改变资源归属或后续过滤输入

### Requirement: Refresh Reload And Immutable Request Identity

refresh token 换发新 token 前，系统 SHALL 按已接受的用户状态规则重新加载数据库用户、角色与 tenant identity。新 token MUST 使用 fresh 数据库 tenantId；旧 refresh token 内的 tenant claim MUST NOT 覆盖数据库事实。

系统 SHALL 提供统一、immutable 的 request identity，同时携带 authenticated userId 与 tenantId。该 identity SHALL 从 `UserPrincipal` 或等价服务端认证主体构造并显式传递；C13a MUST NOT 依赖可跨请求或异步线程泄漏的全局 ThreadLocal tenant selector。

#### Scenario: Refresh 使用最新 Tenant Identity

- GIVEN refresh token 有效且对应用户仍可认证
- AND 数据库认证记录是当前 tenant identity 的事实源
- WHEN 系统换发 access/refresh token
- THEN 系统重新加载用户、角色与 tenantId
- AND 新 token 使用 fresh tenantId
- AND 旧 token claim 或客户端输入不能覆盖它

#### Scenario: Principal 缺少 Tenant Identity

- GIVEN 请求没有 `UserPrincipal` 或 principal 缺少有效 userId/tenantId
- WHEN controller 或 service 请求统一 request identity
- THEN 系统返回稳定未认证结果
- AND 不构造 partial identity 或使用 magic tenant ID

#### Scenario: 异步边界不使用全局 Tenant Selector

- GIVEN 后续业务需要把 request identity 传入 SSE、Reactor、索引任务或线程池
- WHEN C13a identity abstraction 被使用
- THEN userId 与 tenantId 作为同一个 immutable identity 显式传播
- AND C13a 不引入依赖线程复用状态的全局 tenant ThreadLocal

### Requirement: C13a Dark Rollout And Isolation Claim Boundary

C13a SHALL 只建立 legacy single-tenant 模型、认证 identity 与迁移兼容性，不得开放 tenant CRUD、membership、tenant switch 或第二个业务 tenant。C13a MUST NOT 修改或宣称已经覆盖 SQL/API、Milvus、Qdrant、Elasticsearch、cache、task、history、feedback 或其他数据面的 tenant enforcement。

只有 C13b 从服务端 request identity 对全部启用数据面实施强制 tenant filter/namespace，且 C14 隔离与恶意样本评测通过后，系统才 MAY 宣称租户隔离成立。若未来 tenant mode 只支持 Milvus，Qdrant/Elasticsearch MUST 在该 mode 下拒绝启用，不得静默运行缺少隔离的 adapter。

#### Scenario: C13a 完成后保持单租户兼容

- GIVEN C13a migration、认证与 context tests 已通过
- WHEN 用户在 legacy tenant 中执行现有登录与知识库主链路
- THEN 现有单租户行为与 API 请求形状保持兼容
- AND 系统不创建第二个业务 tenant 或提供 tenant switch
- AND 验收结论只描述 tenant model/context readiness，不描述跨租户隔离已完成

#### Scenario: 客户端尝试启用第二租户

- GIVEN C13b 强制过滤与 C14 隔离评测尚未完成
- WHEN 客户端尝试通过 API、header、query、body 或 metadata 创建、选择或模拟第二 tenant
- THEN 系统不提供该能力
- AND 现有 owner/public/KB permission 语义不被包装成 tenant isolation

#### Scenario: 后续 Tenant Mode 的 Vector Adapter 边界

- GIVEN 后续 C13b 启用 tenant mode
- WHEN 某个已配置 vector adapter 没有已验证的 tenant enforcement
- THEN 系统拒绝启用该 adapter
- AND 不以 silently omitted filter、client metadata 或 fallback adapter 继续提供服务

## ADDED Requirements

### Requirement: Tenant-Scoped SQL, API And Permission Enforcement

系统 SHALL 从服务端认证得到的 immutable request identity 对所有用户可见 SQL/API 资源实施 tenant enforcement。knowledge base SHALL 是业务资源 tenant root；document、document chunk、KB permission、QA history、QA feedback 与 async task SHALL 持久化非空 tenant identity，且 child tenant MUST 与其 KB、user 或 parent row 的 tenant 一致。

任何 lookup/list/count/insert/update/delete/finalize SHALL 包含服务端 tenant predicate，不得仅依赖裸 `userId`、资源 ID 或 controller 已经做过一次授权。owner、`is_public` 与 KB permission 只有在 request tenant 与 resource tenant 相同后才 MAY 生效；C13b 的 `is_public` 只表示 tenant 内公开。

#### Scenario: 同 Tenant 主链路保持兼容

- GIVEN 已认证用户、KB、document、history、feedback 与 task 都属于 tenant A
- WHEN 用户通过现有 REST 请求形状执行 KB/文档/问答/历史/反馈/任务主链路
- THEN 服务端从 `RequestIdentity` 取得 tenant A 并在每层显式传播
- AND 现有 owner/public/permission 与用户级行为在 tenant A 内保持兼容
- AND 客户端不需要也不能提交 tenant selector

#### Scenario: 跨 Tenant 猜测资源 ID

- GIVEN tenant A 用户知道 tenant B 的 kbId、documentId、historyId、feedbackId 或 taskId
- WHEN tenant A 用户调用对应 detail/list/update/delete/status/result/cancel API
- THEN 所有 SQL 与 service lookup 都以 tenant A 作为不可省略条件
- AND 响应按资源不存在处理，不泄露 tenant B、owner、public、permission、状态或资源存在性
- AND tenant B 的数据不被读取、修改、删除或计数

#### Scenario: Public 与 Permission 不跨 Tenant

- GIVEN tenant B 的 KB 为 public 或存在 owner/KB permission 关系
- WHEN tenant A 用户尝试读取、写入、管理或被授予该 KB 权限
- THEN public、owner 与 permission 均不能越过 tenant 根边界
- AND 系统拒绝创建跨 tenant `kb_permission`
- AND 同 tenant 无权限与跨 tenant 不可见使用各自稳定的 forbidden/not-found 语义

### Requirement: Forward Data-Plane Tenant Migration And Integrity

新的前向 Flyway migration SHALL 从 C13a 已持久化的 user/knowledge-base tenant facts 回填 document、document chunk、KB permission、QA history、QA feedback 与 async task tenant identity。migration SHALL 按可空列、确定性回填、null/orphan/mismatch 检查、索引/unique 调整、`NOT NULL` 的顺序执行，MUST NOT 修改 V1-V10、清空数据或把冲突行静默归入 magic/default tenant。

数据库与应用写入路径 SHALL 防止新 child row 的 tenant 与父资源 tenant 不一致。全局 role/permission 目录不在 C13b 被包装为 tenant-scoped RBAC。

#### Scenario: V10 旧库升级

- GIVEN V10 数据库包含 legacy tenant 的正常和逻辑删除 KB、document/chunk、permission、history/feedback 与 durable/legacy task
- WHEN Flyway 执行 C13b migration 并 validate
- THEN 每个目标 row 获得非空、与父事实一致的 tenant identity
- AND 原业务 ID、owner/public、document status、history/feedback 关系与 task phase/lease facts 保持不变
- AND tenant-aware index/unique 可用于直接 lookup 与 recovery

#### Scenario: 不一致父子关系 Fail Closed

- GIVEN fixture 中 child 的 KB、user 或 parent row 指向不同 tenant，或无法找到有效 tenant 事实
- WHEN migration 或应用写入尝试完成该关系
- THEN migration/transaction fail closed
- AND 系统不使用 legacy-default、客户端 metadata 或任意一侧 tenant 猜测归属
- AND 冲突行不进入可服务状态

#### Scenario: Fresh Install 与新写入

- GIVEN 空数据库从 V1 执行到 latest
- WHEN 系统创建 tenant-scoped KB、document、chunk、permission、history、feedback 或 task
- THEN 所有目标表 tenant column 均为非空
- AND child tenant 来自已验证的服务端 parent/request scope
- AND null 或 mismatch tenant 写入失败

### Requirement: Tenant-Scoped Retrieval And Vector Adapter Contract

RAG、query、keyword 与 vector 数据流 SHALL 使用由 tenant-scoped knowledge base 解析的 immutable scope，同时携带 tenantId、knowledgeBaseId 与受控 collection namespace。客户端 metadata filter MUST NOT 创建或覆盖 tenant、KB 或 collection scope；reserved scope fields SHALL 被拒绝，普通 filter 只能与服务端 scope 做 AND。

任何在 tenant enforcement 模式启用的 vector adapter SHALL 对 create/has/upsert/search/get/getByIds/delete/count/drop 实施等价 tenant enforcement。C13b SHALL 至少验证默认 Milvus adapter；Qdrant 或 Elasticsearch 未通过同一 contract 时 MUST 拒绝启动，不得 fallback 到无 tenant filter、NoOp 或其他 adapter。

#### Scenario: 同 Collection 中的双 Tenant Vector 隔离

- GIVEN tenant A/B 的受控 vector scopes 与含 tenant/kb marker 的 records
- WHEN 执行 upsert、search、get/getByIds、delete、count 或 drop
- THEN 每个操作都强制当前服务端 tenantId 与 knowledgeBaseId
- AND tenant A 看不到、统计不到、修改不了、删除不了 tenant B record 或 namespace
- AND search-only 过滤不能作为其他操作已隔离的替代证据

#### Scenario: 客户端伪造 Retrieval Scope

- GIVEN 已认证请求属于 tenant A 且选择了 tenant A 的 kbId
- WHEN 客户端 filter/metadata 使用 camel、snake 或大小写变体声称 tenant B、其他 kbId 或 collectionName
- THEN 系统返回稳定 invalid-request 结果
- AND 伪造字段不进入 vector、keyword、QA cache 或 history scope
- AND 普通非 reserved filter 仍只能进一步收窄 tenant A 结果

#### Scenario: Unsupported Adapter Fail Startup

- GIVEN tenant enforcement 已启用且配置的 Qdrant/Elasticsearch adapter 没有通过完整 tenant contract
- WHEN 应用创建 vector adapter 或启动服务
- THEN 启动 fail closed 并报告稳定 unsupported tenant-enforcement category
- AND 不静默省略 filter、不切换 adapter、不以接口编译通过宣称 adapter 已隔离

### Requirement: Legacy Vector Readiness Without Unscoped Fallback

既有 vector record 在没有可信 tenant/kb marker 时 MUST NOT 被 tenant runtime 隐式读取。系统 SHALL 提供默认关闭、不可从业务 REST API 调用的 maintenance audit/backfill capability，依据 tenant-scoped SQL 的 KB/document/chunk/vector identity 复用现有 vector/content 并补写 marker；该流程 MUST NOT 调用 embedding、rerank、generation、judge 或 LLM。

每个 KB SHALL 只有在 expected、observed、migrated、missing 与 mismatch evidence 完整且无错误后才进入 READY。存在 vector rows 而未 READY 的 KB SHALL fail closed，不得以 legacy-default fallback、关闭 tenant filter 或 keyword/global cache 伪装成功。

#### Scenario: Legacy Vector 全量迁移

- GIVEN legacy KB 的 SQL chunk/vector IDs 与外部 vector records 完整对应但缺少 tenant marker
- WHEN 获授权的 maintenance mode 执行 audit/backfill
- THEN 保留原 vector/content 与业务 identity，只补写服务端 tenantId/kbId marker
- AND embedding/rerank/LLM/model calls 为 0
- AND 全量复核一致后该 KB 才标记 READY

#### Scenario: Legacy Vector 缺失或冲突

- GIVEN legacy vector 存在 missing、duplicate、wrong KB、wrong document 或 marker mismatch
- WHEN maintenance audit/backfill 执行
- THEN 记录稳定数量与错误类别，并保持该 KB 非 READY
- AND runtime query/mutation fail closed
- AND 不自动重算 embedding、不删除未知 record、不把部分成功当作完整迁移

#### Scenario: 未经授权的真实 Maintenance

- GIVEN maintenance 可能读取或修改真实 Milvus collection 与业务内容
- WHEN 尚未披露 collection/record 数、adapter、数据路径、超时/重试和风险并取得用户授权
- THEN 真实 audit/backfill SHALL 为 `SKIPPED`
- AND unit/mock/Testcontainers 证据不能被描述为现有真实 vector 数据已完成迁移

### Requirement: Tenant-Scoped Cache, Task And Async Execution

承载 tenant 业务数据的 auth session、QA cache、embedding cache、idempotency、task projection、durable task ledger/message、index input path 与业务 lock SHALL 使用服务端 tenant namespace。evict/clear/read/write/rebuild SHALL 保持 tenant-local；旧无 tenant key MUST NOT 被运行时双读或回退。

后台恢复 MAY system-wide 扫描 claimable ledger，但每条 task SHALL 持久化合法 tenantId，并从该 durable fact 构造 immutable execution scope。claim、heartbeat、phase、document/KB load、vector mutation、SQL finalize、projection rebuild 与 task API SHALL 使用同一 tenantId，不得从 ThreadLocal、SecurityContext 或客户端 metadata 推断。

token blacklist 与 global IP rate limit MAY 保持明确的 global security scope；它们不得被当作 tenant 业务 cache 的完成证据。

#### Scenario: Redis 业务数据不跨 Tenant 命中

- GIVEN tenant A/B 具有相同 query/content/idempotency key pattern 或 taskId fixture
- WHEN 读取、写入、evict 或 clear session/QA/embedding/idempotency/task 数据
- THEN key 与必要 payload 都包含并校验服务端 tenant identity
- AND tenant A 不命中、覆盖、清除或重建 tenant B 数据
- AND QA/embedding cache failure 只能退化为同 tenant miss/recompute，不能读无 tenant或其他 tenant value

#### Scenario: Durable Task 恢复保持 Tenant

- GIVEN Redis projection 丢失且数据库中 tenant A/B 都有 claimable task
- WHEN 协调器扫描并恢复任务
- THEN 每条 task 从 durable ledger 取得 tenantId 与 owner/document identity
- AND document、KB、input、vector、finalize 与新 projection 都使用相同 tenant execution scope
- AND tenant mismatch、null tenant 或 cross-tenant parent 使该任务 fail closed

#### Scenario: 旧 Redis Key 与全局安全控制边界

- GIVEN 部署前存在无 tenant 的 session/cache/task key，并存在 token blacklist 与 IP rate-limit key
- WHEN C13b 版本启动
- THEN 旧业务 key 不双读，session 需重新登录、cache 重算、task projection 从 tenant-scoped durable ledger 重建
- AND token blacklist 与 global IP rate limit 保持其安全语义
- AND 系统不通过全局业务 key scan/copy 猜测 tenant 归属

### Requirement: C13b Evidence And Isolation Claim Boundary

C13b SHALL 使用双 tenant migration、SQL/API/permission、cache/task/recovery 与 vector adapter contract tests 证明 data-plane enforcement。测试 SHALL 区分真实 adapter integration、unit/mock、跳过的真实 maintenance 与 C14 恶意评测；mock、`RETRIEVAL_ONLY`、`PARTIAL` 或单 tenant happy path MUST NOT 被当作完整隔离证据。

C13b 完成 MAY 描述为 server-side data-plane enforcement 已实现并通过指定测试，但 MUST NOT 宣称生产租户隔离成立。只有 C14 隔离与恶意样本评测通过后，系统才 MAY 开放第二业务 tenant、tenant CRUD/switch、C15 MCP 或 C16 Router，或对外宣称租户隔离成立。

#### Scenario: C13b 验收证据完整

- GIVEN migration、SQL/API/permission、Redis/task/recovery 与 Milvus contract 已实现
- WHEN 执行聚焦测试、完整 Maven/Python gates、静态 bypass 扫描与范围检查
- THEN evidence 标明测试类型、adapter、tests/failures/errors/skips、真实模型调用与真实 maintenance 状态
- AND 每个 requirement/scenario 可映射到测试或明确跳过原因
- AND unsupported adapter 与 legacy not-ready 边界有 fail-closed 证据

#### Scenario: 只完成 Unit 或单 Tenant Happy Path

- GIVEN 只有 mock/unit、legacy tenant happy path、search-only filter 或部分数据面结果
- WHEN 形成阶段结论
- THEN 结论标记为 partial/incomplete
- AND 不得声称 C13b 已覆盖全部启用数据面
- AND 不得提前接受 delta、归档 change 或进入 C14/C15/C16

#### Scenario: C13b 完成但 C14 未通过

- GIVEN C13b 的实现与集成门禁已通过并经用户验收
- WHEN 项目描述当前能力或决定是否开放第二业务 tenant、MCP 或 Router
- THEN 只描述 data-plane enforcement evidence，不描述生产租户隔离已成立
- AND 第二业务 tenant、tenant management、C15 与 C16 继续关闭
- AND 必须等待 C14 隔离与恶意样本评测通过

### Requirement: C14 Isolation Evidence Gate For Tenant Capability Claims

系统在 C13b data-plane enforcement 之后，仍 SHALL 通过独立 C14 adversarial evaluation 才能形成租户隔离完成结论。C14 evidence MUST 使用两个 synthetic tenant，覆盖 identity override、ID guessing、public/permission、reserved filter、cache/task/recovery、vector/keyword、sync/SSE/history/feedback、error 与 coarse timing disclosure，并同时验证 foreign content 不可见和 foreign state 不可修改。

在 C14 global `Report status` 不是完整 `PASS` 时，系统 MUST NOT 开放第二业务 tenant、tenant CRUD/switch、C15 MCP 或 C16 Router，也 MUST NOT 把 C13a identity、C13b unit/integration tests、mock、single-tenant happy path、`RETRIEVAL_ONLY` 或 `PARTIAL` evidence 描述为租户隔离成立。

#### Scenario: C13b 完成但 C14 非 PASS

- GIVEN C13b 已验收但 C14 为 `FAIL/NOT_EVALUABLE/INVALID` 或尚未执行
- WHEN 项目描述当前能力或决定是否开放后续 tenant/MCP/Router 能力
- THEN 只可声明 data-plane enforcement 已实现并通过指定测试
- AND 第二业务 tenant、tenant management、C15 与 C16 继续关闭

#### Scenario: C14 固定攻击矩阵通过

- GIVEN C14 versioned release、双 tenant synthetic fixture、全部 required cases 与 functional/content/error/timing channels 完整 PASS
- WHEN 形成阶段验收结论
- THEN 系统 MAY 声明 Milvus 支持配置和受测攻击矩阵下的租户隔离 evidence 通过
- AND 结论必须附带 Git/profile/release identity 与未覆盖边界

#### Scenario: C14 PASS 后请求自动开放能力

- GIVEN C14 evidence 已通过
- WHEN 请求直接启用 production tenant CRUD/switch、C15 MCP 或 C16 Router
- THEN 仍需独立 Type C change、权限契约、迁移/运行验证与明确授权
- AND C14 不自动改变生产默认开关、API surface 或部署状态

### Requirement: Supported Profile And Production Boundary After C14

C14 的隔离结论 SHALL 限定于当前已通过 tenant adapter contract 的 Milvus 配置、当前 SQL/Redis/task/RAG 路径和固定 synthetic attack matrix。Qdrant/Elasticsearch 在未通过等价 contract 与 C14 matrix 前 MUST 继续在 enforcement mode 下 fail startup；Milvus evidence MUST NOT 外推到未验证 adapter。

C14 PASS MUST NOT 被描述为真实 Milvus shadow migration 已执行、生产数据已验证、生产级多租户/合规认证、容量/DoS 安全、真实网关/跨区域 timing 安全或所有 side-channel 已消除。任何真实 collection copy/mapping/readiness switch、真实业务 tenant 启用或生产安全测试仍需单独披露范围、数据出站、超时/重试、回滚与风险并取得授权。

#### Scenario: 未验证 Adapter

- GIVEN C14 在 Milvus 支持配置下 PASS，但 Qdrant/Elasticsearch 没有等价 tenant contract/evidence
- WHEN 配置 enforcement mode 使用未验证 adapter
- THEN 应用继续 fail startup
- AND 不以 C14 Milvus report、接口兼容或空结果声称该 adapter 已隔离

#### Scenario: 真实 Shadow Migration 未授权

- GIVEN C14 使用 synthetic collection 完成 evaluation，但真实 Milvus collection 尚未 audit/copy/switch
- WHEN 形成 C14 结论或计划生产启用
- THEN 真实 maintenance 状态保持 `SKIPPED`，现有业务数据迁移不得写成已完成
- AND 任何真实写入、切换、重试或清理仍需单独授权

#### Scenario: 生产级安全声明

- GIVEN C14 本机 synthetic functional/error/timing evidence PASS
- WHEN 对外描述系统成熟度
- THEN 仍按工程原型表述，并列出真实拓扑、容量、合规、全 adapter、运维权限与网络侧信道未知项
- AND 不得使用“生产级多租户”“已通过渗透测试”或“无 timing side-channel”等超范围结论
