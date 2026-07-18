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
