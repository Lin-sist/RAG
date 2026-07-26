# Design: C13a Tenant Model, Context And Migration

## 1. Current State

当前认证链路由数据库加载 `AuthUserAccount`，构建 `UserPrincipal`，再把 `userId/username/roles/tokenType` 签入 JWT。`CurrentUserService` 只从认证 principal 读取 `userId`。数据库 V1-V9 没有 tenant 表或 tenant column；`knowledge_base` 使用 `owner_id` 与 `is_public`，`kb_permission` 使用 `kb_id/user_id`。

因此 owner/public/用户级权限是已确认能力，但它不是租户隔离。C13a 必须先建立唯一可信的 tenant identity，同时保持旧数据与现有单租户主链路可升级。

## 2. Target Shape

### 2.1 Persistence

- 新增 `tenant` 表：稳定主键、唯一 `code`、显示 `name`、enabled/deleted/version 与审计时间字段。
- `user` 新增非空 `tenant_id` 与索引；C13a 固定一名用户只有一个 active tenant。
- `knowledge_base` 新增非空 `tenant_id` 与索引，使资源归属不依赖 owner 的可变关系。
- 新的 Flyway V10 先创建/取得唯一 `legacy-default` tenant，再回填旧 user/KB，最后加非空与索引约束；不得修改 V1-V9。
- C13a 不给 document/chunk/task/history/cache/vector metadata 批量增加 tenant 字段；这些属于 C13b 的逐数据面设计。

### 2.2 Authentication And Context

- auth repository 从数据库同时加载 `tenantId`，`UserPrincipal` 持有 `userId + tenantId + roles`。
- access/refresh token 都由服务端写入 `tenantId` claim；客户端不能传入该 claim 的替代来源。
- JWT 解析对缺失、非正整数或类型错误的 tenant claim fail closed。部署前签发的无 tenant claim token 需要重新登录。
- refresh 沿用 baseline 的数据库重载规则；新 token 使用 fresh principal 的 tenantId，而不是旧 token 的 tenantId。
- admin 层提供 immutable `RequestIdentity(userId, tenantId)` 或等价值对象，由统一 service 从 `UserPrincipal` 构造。后续 C13b 通过显式参数传播，不使用可泄漏到线程池的全局 `ThreadLocal`。

### 2.3 Dark Rollout Boundary

- C13a 只创建一个 legacy tenant，不提供 tenant CRUD、membership 或 switch API。
- 现有 KB/文档/问答行为在同一 legacy tenant 内保持兼容；C13a 不修改 SQL/API/vector/cache/task/history 过滤。
- 在 C13b 和 C14 完成前，不允许把系统描述为多租户隔离已完成，也不允许创建第二个可业务使用的 tenant。

## 3. Migration Sequence

1. 从 V9 schema 建立 tenant table，并插入唯一 `legacy-default` row。
2. 给 `user`、`knowledge_base` 增加临时可空 `tenant_id`。
3. 将所有现有非删除和已逻辑删除 rows 都回填到 legacy tenant，避免保留无法归属的历史记录。
4. 验证没有 null/invalid tenant reference 后，将两列改为 `NOT NULL` 并创建索引。
5. fresh install 与 V9→V10 都通过 Flyway `migrate + validate`；验证 user/KB 主键、owner、public 与 `kb_permission` 关联不变。

实现时若 MySQL/H2 兼容语法无法在同一 migration 中安全表达，以 MySQL 8.0.36 为生产事实源，并为纯 SQL compatibility test 使用与现有测试一致的受控写法；不得修改历史 migration 来迁就测试。

## 4. Verification Strategy

- Migration RED：V9 fixture 中包含正常/禁用/逻辑删除用户、public/private KB 与 KB permission；升级后 tenant row 唯一、所有 user/KB 非空归属、ID/关系不变。
- Auth RED：数据库账号必须加载 tenantId；login/refresh 签发 fresh tenant claim；缺失/非法 claim 认证失败；旧 refresh token 不能覆盖数据库 tenant。
- Context RED：统一入口同时要求 userId/tenantId，非 `UserPrincipal`、null 或非法值返回稳定 401/403 边界；任何伪造 client tenant 值都不改变 context。
- Compatibility：既有数据库认证、bootstrap、JWT property、知识库主链路测试继续通过；C13a 不新增第二租户隔离成功断言。
- Full gates：`mvn -q test`、Python unit tests、SensitiveLogs、protected paths、migration scan、Markdown links、`git diff --check`。

## 5. Failure Semantics

- migration 无法建立完整 tenant 归属时 fail closed，应用不得带 null tenant context 启动到可服务状态。
- 登录时数据库用户缺 tenant、tenant 不存在或不可用时拒绝签发 token，响应不泄露 tenant code/name 或内部 schema。
- access/refresh token tenant claim 缺失或非法时拒绝认证；不回退到请求 header、query、body、metadata 或固定 magic ID。
- context 构造失败使用稳定认证/授权错误，不把 token、tenant 原始输入或数据库异常写入普通日志。

## 6. Rollout And Rollback

- Rollout：先合并规划；用户批准后以 migration/auth/context 小切片 TDD 实现；完成后仍只运行 legacy single-tenant 模式。
- Compatibility：新部署会使无 tenant claim 的既有 token 失效，用户需重新登录；数据库业务 ID 与 API 请求形状保持不变。
- Rollback：数据库 forward migration 不通过回改 V1-V9 或自动删 tenant 数据；如需回退应用，应使用兼容已新增列的前一版本或单独制定恢复方案。当前规划不执行 destructive down migration。

## 7. C13b Handoff

C13b 必须从 `RequestIdentity.tenantId` 或等价服务端身份出发，逐项覆盖 SQL/API/permission、所有启用 vector adapters、cache/task/history 命名空间。若首版只支持 Milvus tenant mode，Qdrant/Elasticsearch 必须在 tenant mode 下拒绝启用，不能静默运行无隔离路径。C14 隔离与恶意样本评测通过前不得开放 MCP 或 Router。

## 决策记录

### 决策 1：C13 是否一次完成全部租户隔离
- **面临的选择**：一个 change 同时改身份与全部数据面；先做 C13a 模型/上下文/迁移再拆 C13b；只在查询层临时加 filter。
- **选了哪个 + 为什么**：选择先做 C13a，再按数据面拆 C13b；身份与迁移先稳定，后续过滤才能共享同一可信输入并逐片验收。
- **放弃的代价**：一次全改会让范围和故障定位失控；查询层临时 filter 会漏掉写入、异步、缓存和向量路径。

### 决策 2：用户与租户是单归属还是多 membership
- **面临的选择**：`user.tenant_id` 单归属；tenant membership 多对多表；继续没有持久化租户归属。
- **选了哪个 + 为什么**：选择 C13a 使用 `user.tenant_id` 单归属，与当前一个 username 对应一个认证主体和无 tenant selector 的登录契约一致，能用最小模型建立可信上下文。
- **放弃的代价**：多 membership 需要 tenant selector、session 切换和租户级角色语义，超出本 change；继续无归属则 C13b 没有可信过滤键。

### 决策 3：知识库 tenant 是否只从 owner 间接推导
- **面临的选择**：只通过 owner 的 tenant 推导；在 `knowledge_base` 冗余持久化 `tenant_id`；立刻给所有业务表增加 tenant_id。
- **选了哪个 + 为什么**：选择给 `knowledge_base` 持久化 tenant_id，它是后续文档、向量和问答数据面的稳定根边界，不会因 owner 状态或未来转移而失去资源归属。
- **放弃的代价**：仅靠 owner 会让资源租户随用户关系漂移；一次给所有表加列会提前进入 C13b 并扩大迁移风险。

### 决策 4：旧数据如何迁移
- **面临的选择**：要求人工为每条数据指定 tenant；自动创建一个稳定 legacy tenant 并原地回填；清空旧数据后重新初始化。
- **选了哪个 + 为什么**：选择稳定 legacy tenant 原地回填，当前仓库没有真实组织映射可供推断，这能保留所有业务 ID、owner/public 与权限关系。
- **放弃的代价**：人工映射无法自动部署且容易不一致；清空数据是破坏性操作，也违背兼容迁移目标。

### 决策 5：tenant identity 从哪里进入请求上下文
- **面临的选择**：信任客户端 header/body/metadata；从服务端数据库 principal 签入 JWT 并构造 context；每个 controller 各自查库猜测。
- **选了哪个 + 为什么**：选择数据库 principal → 服务端签发 JWT → 统一 immutable context，身份链可验证且为 C13b 提供一致输入。
- **放弃的代价**：客户端字段可伪造越权；controller 各自查库会重复逻辑并产生不一致的失败语义。

### 决策 6：部署前旧 token 缺少 tenant claim 怎么处理
- **面临的选择**：静默映射到固定 tenant；每次请求查库补齐；fail closed 并要求重新登录。
- **选了哪个 + 为什么**：选择 fail closed 并要求重新登录，避免在未来出现多个 tenant 后仍保留可被错误归属的兼容后门。
- **放弃的代价**：固定回退会把缺失身份当有效身份；逐请求查库增加隐式兼容分支和运行负担，且难规定何时移除。

### 决策 7：refresh 使用旧 token 还是数据库 tenant
- **面临的选择**：复制旧 refresh token 的 tenant；refresh 时重载数据库用户与 tenant；允许客户端选择新 tenant。
- **选了哪个 + 为什么**：选择重载数据库事实，与已接受的 refresh 用户状态/角色语义一致，新 token 不继承可能过期的 tenant claim。
- **放弃的代价**：复制旧 claim 会延续陈旧归属；客户端选择会破坏服务端身份推导边界。

### 决策 8：tenant context 是否使用 ThreadLocal
- **面临的选择**：全局 ThreadLocal；只把 tenantId 当散落的 Long；使用 immutable identity 并显式传递。
- **选了哪个 + 为什么**：选择 immutable identity 显式传递，能够同时携带 userId/tenantId，并避免异步索引、Reactor/SSE 或线程池复用造成上下文串租户。
- **放弃的代价**：ThreadLocal 在异步边界容易丢失或泄漏；散落 Long 无法证明 user 与 tenant 属于同一认证事实。

### 决策 9：C13a 是否开放 tenant CRUD 与第二租户
- **面临的选择**：立即开放管理 API；只做内部 legacy tenant 暗铺设；完全不建立 tenant row。
- **选了哪个 + 为什么**：选择只做 legacy tenant 暗铺设，因为 C13b 过滤和 C14 隔离评测尚未完成，开放第二租户会制造真实泄露风险。
- **放弃的代价**：立即开放会让未隔离数据面暴露给真实租户；不建 tenant row 则无法验证迁移和上下文。

### 决策 10：是否现在修改 username/email 唯一性和登录 DTO
- **面临的选择**：改为 tenant-scoped username/email 并要求 tenant selector；保持全局唯一与当前登录 DTO；新增并行登录端点。
- **选了哪个 + 为什么**：选择保持现状，C13a 的目标是可信 tenant identity，不是账号 provisioning 或多 membership 登录体验。
- **放弃的代价**：立即改唯一性会引入 API、索引、歧义解析和前端联动；并行端点会形成两套认证契约。

### 决策 11：是否给 tenant 关联增加数据库外键
- **面临的选择**：立即增加物理外键；只用非空列、索引和应用/迁移验证；维持可空弱关联。
- **选了哪个 + 为什么**：选择 C13a 采用非空列、索引与显式验证，保持仓库现有无业务外键、软删除与 Flyway 兼容风格；是否引入物理外键留待数据生命周期设计统一决定。
- **放弃的代价**：立即加外键可能改变现有删除/恢复顺序并放大升级失败；可空弱关联会允许无 tenant 身份进入运行时。

### 决策 12：C13a 完成后能否宣称租户隔离
- **面临的选择**：字段和 JWT 就绪即宣称完成；C13b 强制过滤后宣称完成；C13b 加 C14 隔离评测通过后才宣称完成。
- **选了哪个 + 为什么**：选择 C13b 实现且 C14 评测通过后才宣称隔离成立，字段存在不能证明各数据面没有漏路。
- **放弃的代价**：过早宣称会把 schema readiness 当成安全保证；只看实现不做恶意隔离评测仍难发现组合路径泄露。
