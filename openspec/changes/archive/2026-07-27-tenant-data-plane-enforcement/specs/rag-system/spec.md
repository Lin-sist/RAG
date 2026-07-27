# RAG System Delta: C13b Tenant Data-Plane Enforcement

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
