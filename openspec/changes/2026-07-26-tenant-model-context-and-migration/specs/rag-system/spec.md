# RAG System Delta: C13a Tenant Model, Context And Migration

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
