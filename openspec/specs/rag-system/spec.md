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
