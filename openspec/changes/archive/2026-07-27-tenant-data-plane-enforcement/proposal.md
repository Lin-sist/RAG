# Proposal: C13b Tenant Data-Plane Enforcement

## Why

C13a 已建立唯一 legacy tenant、数据库认证 tenant identity、access/refresh JWT tenant claim 与 immutable `RequestIdentity(userId, tenantId)`，但当前业务数据面仍大量只按裸 `userId`、`kbId`、`documentId`、`taskId` 或 `collectionName` 读写。`is_public`、owner 与 `kb_permission` 仍没有 tenant 根边界；历史、反馈、异步任务、Redis key、文件型 durable input、BM25 与三个 vector adapter 也没有统一的 tenant 强制约束。

因此当前状态只能证明 tenant model/context readiness，不能证明租户隔离。若直接开放第二业务 tenant，攻击者即使不能伪造 JWT tenant claim，仍可能通过猜测资源 ID、任务 ID、向量 collection、缓存键或异步恢复路径触达其他 tenant 的数据。

C13b 将服务端 `RequestIdentity` 贯穿同步、流式和异步主链路，对 SQL/API/permission、Milvus、Redis/cache、task、history/feedback 与本地索引输入实施 fail-closed tenant enforcement。Qdrant、Elasticsearch 若未在本 change 内通过同一 adapter contract，将在 tenant enforcement 模式下拒绝启动；不得静默省略过滤或退回无隔离 adapter。

## 用户故事：改前坏事 → 改后不同

- 改前坏事：tenant A 可以提交 tenant B 的 `kbId/documentId/historyId/taskId`；controller 虽有 owner/permission 检查，但底层常先按裸 ID 读出资源，且 public/permission 语义没有 tenant 限制。
- 改后不同：所有用户请求先取得服务端 `RequestIdentity`，资源查询把 `tenantId` 作为不可省略条件；跨 tenant 资源按不存在处理，owner/public/permission 只在同一 tenant 内生效。
- 改前坏事：向量接口接受裸 `collectionName`，search 虽支持客户端 metadata filter，但 get/delete/count 和 collection lifecycle 没有 tenant 约束，旧向量也没有可信 tenant marker。
- 改后不同：运行时只能使用由 tenant-scoped KB 解析出的 vector scope；Milvus 的 upsert/search/get/delete/count 全部强制 tenant marker，旧向量完成显式审计迁移前保持不可服务状态。
- 改前坏事：QA、embedding、session、idempotency、task Redis key 以及 durable task/input 只依赖全局前缀或裸 ID，跨 tenant 缓存命中、状态读取和恢复归属无法被系统性证明安全。
- 改后不同：所有承载 tenant 业务数据的 key、状态与输入路径都包含服务端 tenant namespace；token blacklist 和全局 IP rate limit 作为明确的全局安全控制保留全局语义。
- 改前坏事：后台恢复协调器没有请求 principal，只能按 task/document ID 恢复，容易在异步边界丢失 tenant。
- 改后不同：tenantId 与 ownerId 一起持久化在 durable ledger/message/status 中，恢复执行从持久化事实重建 immutable execution scope，不读取 ThreadLocal 或客户端 metadata。

## Goals

1. 新增前向 Flyway migration，为 `document`、`document_chunk`、`kb_permission`、`qa_history`、`qa_feedback` 与 `async_task` 建立非空 tenant 归属、索引和 legacy backfill，并验证父子 tenant 一致性。
2. 将 controller、authorization 与业务 service 的用户路径统一改为显式接收 `RequestIdentity`；SQL 读写必须包含 tenant predicate，跨 tenant ID 不泄露资源是否存在。
3. 把 `is_public`、owner 与 KB permission 约束为 tenant-local 语义；禁止跨 tenant grant、read、write、admin 或 public access。
4. 为 RAG/query/vector/keyword 数据流建立服务端 tenant scope；保留客户端可选的普通 metadata filter，但拒绝或覆盖任何 reserved tenant/KB scope 字段。
5. 在 Milvus adapter 上验证 create/upsert/search/get/delete/count/drop 的 tenant enforcement；未通过同一 contract 的 Qdrant/Elasticsearch 配置 fail closed。
6. 提供默认关闭、维护态专用的 legacy vector audit/backfill 路径，复用已有向量并写入 tenant marker；不重新调用 embedding provider，不在 readiness 未确认时静默放宽读取。
7. 对 QA/embedding/session/idempotency/task cache、Redis task projection、durable task ledger/message 与 `IndexInputStore` 路径实施 tenant namespace。
8. 用双 tenant fixture、迁移测试、SQL/API/permission 单测、异步恢复测试、Redis key 测试和 Milvus integration contract 证明 C13b enforcement；真实外部 provider 调用与 C14 恶意评测仍单独授权。

## Non-Goals

- 不提供 tenant CRUD、邀请、membership、多 tenant switch、tenant selector、租户级角色管理、跨租户管理员、SSO/provisioning 或计费。
- 不修改登录 DTO、username/email 全局唯一性、JWT tenant identity 来源或 C13a 的单用户单 tenant 模型。
- 不在本 change 宣称生产级多租户、合规隔离或恶意输入安全；C14 通过前只能描述为代码与集成层 enforcement 已就绪。
- 不为了租户测试定制 retrieval、chunking、rerank、prompt、citation、no-answer 或 generation 指标口径。
- 不执行 C14 隔离与恶意样本评测，不开放 C15 MCP，不实现 C16 Router。
- 不新增或升级依赖，不修改 `.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`。
- 不在规划阶段执行 Milvus/Qdrant/Elasticsearch、embedding、rerank、ask、generation、judge、LLM 或其他业务外部调用。
- token blacklist 继续按不可预测 token hash 全局隔离，IP rate limit 继续是全局安全控制；二者不伪装成 tenant cache。

## Capability Classification

- `confirmed`：C13a tenant/user/KB schema、数据库 principal、JWT tenant claim、refresh reload、immutable `RequestIdentity`、单 KB 独立 vector collection、MySQL/Redis/Milvus Testcontainers 入口、durable index task ledger 与可重建 Redis projection。
- `partial`：controller 已在少数创建路径取得 `RequestIdentity`，但大多数授权与 service 只传 userId；KB 已有 tenantId，但 document/chunk/permission/history/feedback/task 没有；vector search 支持普通 filter，但其他操作没有 tenant scope；Redis key 只有资源 ID/hash。
- `planned`：V11 child-row tenant backfill、一致性门禁、显式 identity service contract、tenant-local public/permission、tenant vector scope、Milvus contract、unsupported adapter startup guard、legacy vector audit/backfill、tenant-scoped Redis/task/input/keyword 数据流与双 tenant integration evidence。
- `out_of_scope`：tenant 管理/切换/membership、tenant-scoped username、租户级 RBAC、生产 SSO/provisioning、C14/C15/C16、Qdrant/Elasticsearch 完整 tenant adapter（除非同一 change 内通过完整 contract）。
- `unknown`：真实组织层级、生产 tenant 数量与容量、跨租户运维角色、数据驻留/retention、现有真实向量集合的规模与完整性、生产 Qdrant/Elasticsearch 是否实际使用；实现前不得臆造。

## Proposed Contract

1. 任何用户可见 SQL/API 资源访问都必须从 `RequestIdentity.tenantId` 推导；客户端 tenant/KB/vector metadata 不能扩大 scope。
2. `knowledge_base` 是业务资源 tenant root；child rows 同时持久化 tenantId 作为直接查询与异步恢复的防御性边界，写入时必须与父资源 tenant 一致。
3. `is_public` 只表示 tenant 内公开。owner、KB permission、history ownership 与 feedback ownership 均必须先满足同 tenant，再判断现有用户级权限。
4. 跨 tenant 的 KB/document/history/feedback/task ID 访问返回稳定的 not-found 边界，不回显目标 tenant、owner 或资源存在性；同 tenant 内权限不足仍使用现有 forbidden 语义。
5. runtime vector/keyword 操作必须携带由 tenant-scoped KB 解析的 immutable scope。Milvus 文档必须有服务端 tenantId/kbId marker；search/get/delete/count 必须强制同一 scope，不能接受客户端覆盖。
6. legacy vector 未通过显式 audit/backfill 和 readiness 标记时，该 KB 的 vector query/mutation fail closed；维护路径默认关闭、不可从业务 API 调用，且不得触发 embedding 或 LLM。
7. Qdrant/Elasticsearch 只有在通过与 Milvus 等价的 create/upsert/search/get/delete/count/drop contract 后才能在 enforcement 模式启用；否则应用启动拒绝该 adapter。
8. tenant 业务 cache、session、idempotency、task projection/ledger/message/input path 必须含 tenant namespace；clear/evict 操作不得跨 tenant 扫描。token blacklist 与全局 IP rate limit 保留明示的 global scope。
9. 后台恢复从 durable ledger 中重建 tenant execution scope，并在每次 document/KB/finalize/vector 操作重新带入 tenant 条件；不使用全局 ThreadLocal tenant selector。
10. C13b 完成只证明实现与集成测试层 enforcement。只有 C14 隔离和恶意样本评测通过后，项目才可以宣称租户隔离成立或开放第二业务 tenant、C15/C16。

## Impact

- 预计新增 `V11__tenant_data_plane_enforcement.sql`，扩展六类 child/business table entity、mapper、migration fixture 与一致性测试。
- controller、authorization、KB/document/history/feedback/task/indexing/RAG/query/vector/keyword/cache 接口会发生显式 tenant scope 变更；外部 REST 请求形状保持不变。
- Redis key 与 task status payload 会升级版本并包含 tenantId；旧业务 cache/projection 不复用，durable MySQL ledger 仍是任务恢复事实源。
- Milvus 旧向量需要维护态 audit/backfill；当前规划不执行真实迁移。没有 READY 证据的 KB 将 fail closed，而不是读取无 marker 记录。
- 规划阶段只新增 OpenSpec artifacts、更新 `.ai/ACTIVE_TASK.md` 并追加 `.ai/AGENT_LOG.md`。

## Risks And Mitigations

- 风险：遗漏一个裸 ID mapper 调用形成旁路。缓解：service contract 强制 identity/scope、聚焦双 tenant 测试，并增加针对业务 main source 的静态扫描门禁。
- 风险：child table backfill 与父 KB/user 不一致。缓解：V11 先可空回填、执行 mismatch/null 检查，再改 NOT NULL/索引；不修改 V1-V10。
- 风险：旧向量没有 tenant marker，直接启用 filter 会造成空检索。缓解：独立 audit/backfill 与 readiness 状态；迁移复用已有向量，不触发 embedding，未就绪时明确 fail closed。
- 风险：仅 search 有 filter，但 get/delete/count 或 collection lifecycle 仍越界。缓解：adapter contract 覆盖全部接口，不以 search-only 测试作为完成证据。
- 风险：Qdrant/Elasticsearch 的 metadata 表达与 Milvus 不同。缓解：C13b 先锁定已验证 Milvus；其他 adapter 未通过等价 contract 时启动失败，不写伪兼容分支。
- 风险：Redis key 升级导致旧 session/cache/task projection 失效。缓解：要求重新登录、业务 cache 自然重建、task projection 从 tenant-scoped durable ledger 重建；不扫描或复制未知旧 key。
- 风险：system recovery 需要跨 tenant 扫描。缓解：协调器可扫描 claimable ledger，但每条记录必须携带 tenantId，并以 per-record execution scope 执行，不把 system scan 暴露给用户 API。

## Acceptance Evidence

- OpenSpec 事前闸门批准 proposal、design 决策、tasks 与 spec delta。
- MySQL fresh install 与 V10→V11 fixture 验证非空 backfill、父子一致性、ID/业务关系保持和 mismatch fail closed。
- 双 tenant SQL/API/permission tests 覆盖 KB/document/history/feedback/task 的同 tenant success、跨 tenant not-found、tenant-local public/permission 与 reserved filter rejection。
- Redis tests 验证 session/QA/embedding/idempotency/task key 和 payload tenant namespace，以及 tenant-local clear/evict；token blacklist/global IP rate limit 边界不被误改。
- durable task/indexing/recovery tests 验证 tenantId 在 submit、ledger、projection、recovery、finalize、input path 与 vector metadata 中保持一致。
- Milvus adapter contract 覆盖 tenant-scoped create/upsert/search/get/delete/count/drop，并证明 tenant A 的任何操作看不到或修改 tenant B；Qdrant/Elasticsearch 配置在无合格 contract 时 fail startup。
- 旧向量维护工具只在显式 maintenance mode 运行，审计数量/错误类别/readiness，不触发 embedding/rerank/LLM；真实运行仍需另行披露调用量、数据出站与风险并获授权。
- `mvn -q test`、Python unit tests、SensitiveLogs、protected paths、Markdown links、旧裸路径扫描与 `git diff --check` 通过；前端无改动时正式 build 记为 `SKIPPED`。

## Approval Gate

当前批准仅覆盖 C13b 规划，不代表批准 schema/Java/Redis/Milvus 实现或任何真实 provider/adapter 调用。用户需审阅并确认：child table 冗余 tenantId、tenant-local public/permission、跨 tenant not-found、Milvus-first 与 unsupported adapter fail startup、legacy vector maintenance/readiness、Redis key 失效边界，以及 design 中的决策记录。提交责任保持 `用户手动提交`。
