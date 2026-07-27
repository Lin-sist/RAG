# Design: C13b Tenant Data-Plane Enforcement

## 1. Current State

C13a 的身份链已经成立：数据库 user/tenant → `UserPrincipal` → access/refresh JWT → `RequestIdentity(userId, tenantId)`。但 `RequestIdentity` 目前只用于少数 KB 创建路径，其他路径仍以裸 ID 工作：

- `AuthorizationService` 先按裸 `kbId/historyId` 读取，再只判断 owner/user permission。
- `KnowledgeBaseService`、`DocumentService`、`QAHistoryService`、`QAFeedbackService` 与 task API 大量公开不含 tenant 的读写签名。
- `document`、`document_chunk`、`kb_permission`、`qa_history`、`qa_feedback`、`async_task` 没有 tenant column；恢复协调器只有 owner/document/task 信息。
- `QARequest`、`RetrieveOptions`、`SearchOptions` 与 `VectorStore` 接受裸 `collectionName`/filter；Milvus/Qdrant/Elasticsearch 的 get/delete/count 没有 tenant 条件。
- BM25 以内存 collection name 分桶；QA/embedding/session/idempotency/task Redis key 未统一包含 tenant；`clearAllCache` 会跨所有 key 扫描。
- C5 durable index input 与任务恢复没有 tenant namespace。C5 清理成功后原始输入可能已删除，因此不能假定可用原文重新 embedding 旧向量。

这些事实意味着认证 tenant claim 本身不能阻止数据面串租户。

## 2. Target Invariants

1. **身份不变量**：用户请求只能从 `RequestIdentity` 取得 tenantId；后台任务只能从 tenant-scoped durable record 取得 tenantId。
2. **SQL 不变量**：任何业务读写必须包含 tenant predicate；child row 的 tenantId 必须与 KB/user/parent row 一致。
3. **权限不变量**：tenant match 先于 owner/public/permission；`is_public` 只在同 tenant 生效。
4. **向量不变量**：业务代码不能把客户端 collection/filter 直接交给 adapter；运行时 vector scope 必须由 tenant-scoped KB 解析。
5. **缓存不变量**：承载 tenant 数据的 key、payload、evict/clear 均 tenant-local；不存在全局业务 cache flush API。
6. **异步不变量**：tenantId 与 task/document 一起持久化和传播，线程切换、SSE、恢复与重试不依赖 ThreadLocal。
7. **失败不变量**：缺 tenant、mismatch、reserved filter、legacy vector not ready、unsupported adapter 均 fail closed；不回退 magic tenant 或无过滤路径。

## 3. Persistence And Migration

### 3.1 V11 Columns

新增 `V11__tenant_data_plane_enforcement.sql`，为以下表增加 `tenant_id BIGINT`：

| 表 | 回填事实源 | 主要索引/约束用途 |
|---|---|---|
| `document` | `knowledge_base.tenant_id` via `kb_id` | tenant + kb、tenant + id、tenant + content hash |
| `document_chunk` | `document.tenant_id` via `document_id` | tenant + document、tenant + vector_id |
| `kb_permission` | `knowledge_base.tenant_id`，并校验 user tenant 相同 | tenant + kb + user unique |
| `qa_history` | 非空 kb 时取 KB tenant；否则取 user tenant；两者同时存在时必须一致 | tenant + user + created、tenant + kb |
| `qa_feedback` | `qa_history.tenant_id`，并校验 feedback user tenant 相同 | tenant + qa + user unique |
| `async_task` | document→KB tenant；无 document 的 legacy row 使用 owner user tenant；二者并存时必须一致 | tenant + task unique、tenant + recovery/lease |

迁移顺序固定为：增加可空列 → 回填 → 检查 null/orphan/mismatch → 调整相关 unique/index → 改为 `NOT NULL`。V1-V10 不修改。当前所有 legacy 数据应落入 C13a 的唯一 legacy tenant，但测试必须主动构造不一致 fixture，证明迁移不会把冲突悄悄归到默认 tenant。

`role`、`permission`、`role_permission` 不在 C13b 增加 tenantId；它们是当前全局认证角色目录。`user_role` 通过全局唯一 userId 归属，C13b 不把它扩展为 tenant RBAC。未来 tenant-scoped role/membership 另立 Type C change。

### 3.2 Entity And Mapper Rules

- 上述实体显式增加 tenantId；DTO 默认不向客户端暴露 tenantId。
- 用户路径的 service 方法接收 `RequestIdentity` 或只含 `tenantId + resource identity` 的不可变 scope，禁止仅以 `Long userId` 作为授权上下文。
- 所有 `selectById/deleteById/update ... id=` 的业务调用改为 `tenant_id + id` 条件；受控 migration/test 代码例外必须显式标注。
- list/count/update/finalize 同样带 tenant predicate；不能以“之前 controller 已授权”为由省略下层过滤。
- 通过聚焦测试和 main-source 扫描锁定新增裸 ID 旁路。C13b 不使用 MyBatis tenant interceptor，因为它通常依赖 ThreadLocal，也无法覆盖 Redis/vector/file/async 数据面。

## 4. API, Authorization And Permission Flow

### 4.1 Request Flow

controller 首行取得 `RequestIdentity`，随后所有 service/authorization 调用传播完整 identity：

```text
UserPrincipal
  -> RequestIdentity(userId, tenantId)
  -> tenant-scoped KB lookup
  -> owner/public/permission decision within same tenant
  -> document/history/task/vector/cache operation with same tenantId
```

REST 请求形状保持不变，不增加 tenant header/query/body/cookie。客户端若在 QA metadata filter 中提交 `tenantId`、`tenant_id`、`kbId`、`kb_id`、`collectionName` 等 reserved scope key，服务端返回稳定 400；不能由客户端覆盖，也不能简单让客户端条件与服务端条件做 OR。

### 4.2 Resource Disclosure

- 跨 tenant KB/document/history/feedback/task：按 not found 处理，不区分真实不存在与其他 tenant 存在。
- 同 tenant 但 owner/permission 不足：保留稳定 forbidden。
- `is_public=true`：只允许同 tenant 用户读；跨 tenant 不因 public 放宽。
- `kb_permission` grant：目标 user 与 KB 必须同 tenant；否则按目标不可用/不一致失败，不创建关系。
- history/feedback：history、feedback、request identity 的 tenant 必须一致；裸 userId 相等不是充分条件。

## 5. RAG And Vector Scope

### 5.1 Scope Object

在 core 边界新增不含认证实现依赖的 immutable `TenantVectorScope(tenantId, knowledgeBaseId, collectionName)`（最终命名实现时可微调）。admin 层只能从 `tenant_id + kb_id` 查询成功的 KB 构造它；客户端不能直接构造或提交 collectionName。

`QARequest/RetrieveOptions/SearchOptions` 将 tenant scope 与普通 metadata filter 分开。服务端 reserved filter 始终由 scope 生成，普通 filter 只能进一步收窄结果。RAG cache key 使用 tenantId、KB id、query/model/options hash，不依赖客户端 collectionName 作为隔离键。

### 5.2 Milvus Contract

Milvus 是 C13b 的最小已支持 adapter。目标 contract：

- collection lifecycle 接收 tenant vector scope；新 KB 使用 canonical tenant-aware collection name，legacy KB 仍以 SQL 映射的 collection 为物理 namespace，不能从客户端猜测或覆盖。
- upsert 强制写入服务端 `tenantId` 与 `kbId` marker；如果 document metadata 已含冲突值则拒绝。
- search 表达式强制 `tenantId AND kbId`，再 AND 普通 filter；字符串构造必须使用受控字段和值，不接受任意客户端 key 拼接。
- get/getByIds/delete/count 同样含 tenantId/kbId，不允许只按 vector ID 或 collection count。
- 任何返回记录缺 marker 或 marker mismatch 时丢弃并产生稳定隔离错误/诊断；不能把它当正常空结果静默吞掉。
- drop collection 只在 tenant-scoped KB admin/delete 流程执行，并在 SQL mapping 与 scope 一致时允许。

### 5.3 Adapter Capability Guard

`rag.vectorstore.type=qdrant|elasticsearch` 在 tenant enforcement 启用时，只有通过与 Milvus 同等的 adapter capability contract 才能创建 bean。若本 change 未完成对应实现和 contract tests，启动明确失败并指出 unsupported tenant enforcement；不得自动回退 Milvus、NoOp 或无过滤实现。

该边界允许后续独立 change 为 Qdrant/Elasticsearch 补齐 tenant contract，而不在 C13b 为追求“接口看起来兼容”写未验证过滤。

### 5.4 Legacy Vector Audit And Shadow Migration

旧向量没有可信 tenant marker，且原始 durable input 可能已清理。决策 16 已选择 tenant-aware shadow collection，不修改既有 collection schema：

1. 默认关闭的 maintenance command 从 tenant-scoped SQL 读取 KB、当前 collection mapping、document_chunk.vector_id 与 tenantId。
2. 通过 adapter 的受控 legacy maintenance capability 按 ID 只读既有 collection 的 vector/content/metadata；该 capability 不由 runtime `VectorStore` 接口或业务 API 暴露。
3. 校验 source collection、vector ID、documentId、kbId 与 SQL 关系；冲突、缺失、重复或读取失败记为稳定错误，整个 KB 不置 READY，也不切换 mapping。
4. 把已验证的原 vector/content 复制到具备独立 tenantId/kbId 标量字段的 canonical shadow collection；不重新生成 vector，不调用 embedding/rerank/LLM，不修改或删除 source collection。
5. 对 shadow collection 复核 expected/observed/migrated/missing/mismatch 数量；只有全量一致且无错误时，才在同一 SQL 状态转换中切换 active collection mapping 并写入 READY。
6. 部分失败保持原 mapping 与非 READY，shadow collection 只保留为不可服务的维护产物，供显式重试或清理；runtime 不得把它当作 active collection。
7. runtime 对非 READY KB fail closed；新建空 KB 可在 canonical tenant-aware collection 初始化成功后原子写入 mapping/READY。

真实 Milvus 目前只授权只读盘点。任何真实 shadow collection 创建、vector 复制、mapping/readiness 切换、重试或清理都属于写入/迁移，执行前必须披露 collection/record 数、读写范围、数据出站、容量、超时/重试和回滚风险，并另行取得用户授权。

### 5.4 实现期 Legacy Schema 闸门

实现审计确认当前 `pom.xml` 固定 `milvus-sdk-java 2.3.4`，而既有 `MilvusVectorStore` 将 `metadata` 建为 `VarChar`，collection 中没有可用于所有操作的独立 `tenant_id` / `kb_id` 标量字段。当前依赖暴露的 collection alter 能力不能证明可给既有 schema 原位增加这两个字段，因此决策 8 的“原位补 marker”不能按已批准文字直接落地。

用户已确认下方决策 16 选择方案 A：tenant-aware shadow collection + 全量复制审计 + SQL mapping/readiness 原子切换。实现与合成 Testcontainers 验证可继续；真实 collection 仍只允许只读盘点，未获单独写授权时 maintenance write/switch 必须 `SKIPPED`。不得把 JSON 字符串拼接、仅 search 过滤或 mock/unit 结果当作真实迁移证据。

## 6. Keyword Index And Query Fallback

- `KeywordIndex` 使用与 vector 相同的 tenant vector scope；in-memory map key 至少包含 tenantId + KB id，不能只使用裸 collectionName。
- `KeywordIndexBootstrap` 从 tenant-scoped SQL 加载 document/chunk，并把 tenant marker 写入内部 document metadata。
- vector 故障时 keyword-only fallback 仍必须保持同一 tenant scope；不允许因为 Milvus fail open 而退回全局 BM25 collection。
- C13b 不修改 dense/BM25/RRF/rerank 指标口径，双 tenant tests 只验证集合隔离，不比较质量收益。

## 7. Cache And Redis Namespaces

### 7.1 Tenant-Scoped Data

- auth session：`session:v2:{tenantId}:{userId}`，session payload 同时存 tenantId；refresh/logout 对 key 与 fresh principal/token tenant 做一致性校验。升级后旧 session 不复用，用户重新登录。
- QA cache：`qa:cache:v2:{tenantId}:{kbId}:...`；evict/clear 只能 tenant/KB-local。删除全局 `qa:cache:*` 业务清理路径，保留受控维护操作时也必须显式 scope。
- embedding cache：`embedding:v2:{tenantId}:{provider/model}:{contentHash}`；避免跨 tenant 内容派生缓存命中和统计混用。全局 clear 改为 tenant-local 或 maintenance-only。
- idempotency：业务 key 由 `tenantId + userId + endpoint/key` 构成；aspect 从已认证 principal 取得 identity，未认证且 required 的 tenant 业务写请求 fail closed。
- task projection：`task:status:v2:{tenantId}:{taskId}`，JSON payload 含 tenantId；durable fallback 查询同样要求 tenantId。
- index input/lock：storage key/path 与业务 lock 加 tenant namespace，防止相同 task/document key 串用。

### 7.2 Explicit Global Controls

- token blacklist 继续使用 token hash 全局 key；token 自身不可预测且 blacklist 必须在完整 principal 构造前可查询。
- IP rate limit 保持 global；USER 维度因 userId 全局唯一可保持现状，但若 key 承载业务结果或 tenant quota，必须另行 tenant scope。
- 上述 global key 在代码和测试中显式命名为 global security scope，不能被 C13b 完成声明当作 tenant data cache。

## 8. Async Task And Recovery

- `async_task`、`IndexTaskRecord`、`TaskStatus`、`DocumentIndexMessage` 与 durable input 均携带 tenantId。
- submit 从 controller `RequestIdentity` 捕获 tenantId + ownerId；禁止 metadata 中的 tenant 字段决定任务归属。
- recovery coordinator 可以 system-wide 扫描 claimable rows，但每条 row 必须有合法 tenantId。executor 以 `TenantExecutionScope(tenantId, ownerId, taskId)` 重新加载 document/KB，并验证所有父子关系。
- ledger claim/heartbeat/phase/finalize/update 均包含 tenant predicate。`IndexTaskSqlFinalizer` 锁定的 document、chunks 和 task 必须同 tenant，否则事务回滚。
- Redis projection miss 时只从同 tenant durable row 重建；不能按裸 taskId 从另一个 tenant 恢复。
- task API 的 status/result/cancel/exists/completed 全部先按 tenantId + taskId 查询，再验证 ownerId。
- 不使用 `SecurityContextHolder`/ThreadLocal 在后台推断 tenant；SSE subscription 只传播 immutable scope。

## 9. Failure Semantics And Observability

- 跨 tenant user-facing read：稳定 not found；同 tenant forbidden 保持区分。
- migration mismatch/null/orphan：Flyway/startup fail closed。
- reserved scope filter：400，不记录原始敏感 filter value。
- vector scope 缺失、legacy KB not READY、marker mismatch、unsupported adapter：稳定 dependency/configuration error，不能降级到无 tenant filter。
- Redis tenant key/payload mismatch：关键 session/task/idempotency fail closed；QA/embedding cache 可 fail open 到未命中/重算，但不能读另一个 tenant 的 value。
- telemetry/log 只记录低基数 operation、adapter、error category、tenant-scope-present boolean；不把 tenantId、query、content、token、Redis key 或 collection 原值作为常规高基数属性。

## 10. Verification Strategy

### 10.1 RED Fixtures

- MySQL fixture 直接创建 tenant A/B、各自 user/KB/document/chunk/history/feedback/task，并构造跨 tenant permission/parent mismatch。
- API/service tests 对同一资源 ID 模式执行 A success、B not-found、同 tenant无权限 forbidden。
- client filter tests 覆盖 tenantId/kbId 的 camel/snake/case variants，证明不能覆盖服务端 scope。
- async tests 覆盖 fresh submit、Redis projection miss、lease recovery、finalize 与 cancel。

### 10.2 Vector Contract

- 使用既有 Testcontainers Milvus 入口建立两个 tenant scope；验证相同/不同 vector IDs 下的 upsert/search/get/delete/count/drop 隔离。
- legacy fixture 缺 marker 时 runtime fail closed；maintenance backfill 后只有正确 tenant 可见。
- Qdrant/Elasticsearch 在无 contract 支持时配置启动失败；若实现者选择同 change 补齐，必须复用同一 adapter contract suite 后才能改为 supported。

### 10.3 Full Gates

- 聚焦模块测试后运行 `mvn -q test`。
- 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`，确认评测工具未被 C13b 破坏。
- 前端无改动则正式 build `SKIPPED`；若 REST DTO 形状意外变化则必须运行含 `vue-tsc` 的正式 build。
- 扫描 protected paths、credentials/sensitive logs、ThreadLocal、裸 mapper ID 操作、无 tenant VectorStore 调用、旧 Redis key、Markdown links 与 `git diff --check`。
- 真实 adapter maintenance/provider 调用另立执行门禁，记录 adapter、记录量、模型调用=0、数据出站、超时、重试和结果。

## 11. Rollout And Rollback

1. 先提交并审阅 OpenSpec 规划，不写实现。
2. 批准后按 migration/SQL → permission/API → task/cache → vector/keyword → legacy maintenance/readiness → full integration 顺序做 TDD 小切片。
3. V11 是 forward-only；不修改 V1-V10，不提供自动 destructive down migration。
4. Redis v2 key 通过版本前缀自然隔离；旧 cache/session 不迁移，用户重新登录，task projection从 durable ledger 重建。
5. legacy vector readiness 未完成时相应 KB 不提供 vector 读写；不允许通过关闭 tenant enforcement 恢复业务。
6. 若应用版本回退，旧版本不得在已允许第二 tenant 的数据上运行。C13b/C14 完成前仍不开放第二业务 tenant，因此当前 rollback 只针对 legacy tenant 维护窗口。

## 12. C14 Handoff

C13b 验收后仍只可描述为 server-side data-plane enforcement 已实现并通过双 tenant integration tests。C14 必须新增隔离/恶意样本评测，覆盖 ID guessing、reserved filter、public/permission、cache/task/vector/stream/recovery、错误与 timing disclosure。C14 通过前不开放 tenant CRUD/switch、MCP 或 Router，也不宣称生产租户隔离成立。

## 决策记录

### 决策 1：SQL 隔离使用显式 scope 还是全局拦截器
- **面临的选择**：MyBatis tenant interceptor + ThreadLocal；每个 service/mapper 显式传 tenantId；只在 controller 做一次授权。
- **选了哪个 + 为什么**：选择显式 `RequestIdentity`/scope 传播和 tenant predicate，因为它能覆盖 SQL、Redis、vector、file 与 async，并与 C13a 禁止全局 ThreadLocal 的契约一致。
- **放弃的代价**：interceptor 在异步/Reactor 边界易丢失且覆盖不了其他数据面；只在 controller 授权会让内部调用、恢复任务和后续维护代码形成旁路。

### 决策 2：child table 是否冗余持久化 tenantId
- **面临的选择**：所有 child 只 join KB/user 推导；为关键业务表冗余 tenantId；只给 history/task 增加 tenantId。
- **选了哪个 + 为什么**：选择为 document/chunk/permission/history/feedback/task 都持久化 tenantId，使直接 ID 查询、异步恢复和索引可独立 fail closed，并能建立可扫描的完整性门禁。
- **放弃的代价**：每次 join 推导容易在 mapper 优化或删除父记录后失去边界；只覆盖部分表会保留同类裸 ID 漏路。

### 决策 3：跨 tenant 资源返回 403 还是 404
- **面临的选择**：一律 403；跨 tenant 404、同 tenant无权限403；沿用现有异常原样返回。
- **选了哪个 + 为什么**：选择跨 tenant 404、同 tenant无权限403，避免 ID guessing 暴露其他 tenant 资源存在，同时保留同 tenant 权限诊断。
- **放弃的代价**：一律 403 会确认目标存在；沿用现状会因不同 service 先查后验的顺序泄露不一致信息。

### 决策 4：`is_public` 的范围
- **面临的选择**：全系统公开；tenant 内公开；C13b 禁用 public。
- **选了哪个 + 为什么**：选择 tenant 内公开，兼容现有 public 功能，同时保证 tenant 根边界先于 public 权限。
- **放弃的代价**：全系统公开直接破坏租户隔离；完全禁用会删除已有用户可见能力并扩大变更范围。

### 决策 5：向量隔离只靠 collection 还是增加 tenant marker
- **面临的选择**：只信任每 KB 独立 collection；所有 tenant 共 collection 只靠 metadata filter；SQL-mapped KB namespace + 服务端 tenant/kb marker 双重约束。
- **选了哪个 + 为什么**：选择 namespace 与 marker 双重约束，现有每 KB collection 可继续使用，同时 search/get/delete/count 都能验证 tenant/kb 一致性。
- **放弃的代价**：只靠 collection 无法证明裸 collectionName 没被误用；只靠共享 collection filter 会扩大 blast radius，且 collection lifecycle 与旧数据迁移更复杂。

### 决策 6：C13b 是否一次支持三个 vector adapter
- **面临的选择**：Milvus/Qdrant/Elasticsearch 全部一次完成；先验证默认 Milvus，其他 adapter fail startup；只改接口并声称三者兼容。
- **选了哪个 + 为什么**：选择 Milvus-first，其他 adapter 只有通过同一完整 contract 才能启用；这与当前默认 adapter 和 baseline 的 fail-closed 边界一致。
- **放弃的代价**：三者一次完成会显著扩大高风险集成范围；只改接口会把未验证 metadata/filter 行为伪装成隔离能力。

### 决策 7：客户端普通 metadata filter 如何与 tenant scope 合并
- **面临的选择**：信任客户端 tenant/kb filter；服务端覆盖冲突字段；reserved scope key 一律拒绝，普通字段与服务端 scope 做 AND。
- **选了哪个 + 为什么**：选择拒绝 reserved key、普通 filter 只做 AND，使攻击尝试可观察且不会因大小写/别名覆盖产生歧义。
- **放弃的代价**：直接信任会越权；静默覆盖虽可安全执行，但会掩盖调用方错误并留下别名漏检风险。

### 决策 8：旧向量如何获得 tenant marker
- **面临的选择**：重新读取原文并调用 embedding；把无 marker 向量当 legacy tenant 隐式可见；维护态读取现有向量并复制到 tenant-aware shadow collection。
- **选了哪个 + 为什么**：选择维护态复用现有 vector/content 并写入 shadow collection，因为 C5 已可能清理原始输入，复制不产生模型调用；只有全量审计通过并原子切换 SQL mapping/readiness 后才可服务。
- **放弃的代价**：重新 embedding 可能无原文、产生费用且改变 baseline；隐式 legacy fallback 会成为永久跨租户后门；直接改旧 collection 又没有当前 SDK/schema 能力证据。

### 决策 9：legacy vector maintenance 是否属于 runtime API
- **面临的选择**：管理员 REST API；应用启动自动迁移；默认关闭的 maintenance command/profile。
- **选了哪个 + 为什么**：选择维护态命令/profile，允许事前披露数量、窗口与风险，并避免业务请求触发批量外部数据读写。
- **放弃的代价**：REST API 增加高危远程操作面；启动自动迁移会在部署时产生不可控延迟、失败和数据变更。

### 决策 10：embedding cache 是否跨 tenant 共享
- **面临的选择**：按内容+模型全局共享；按 tenant+模型+内容隔离；完全禁用 cache。
- **选了哪个 + 为什么**：选择 tenant-scoped cache，避免内容派生命中与统计跨 tenant 混用，同时保留现有性能收益。
- **放弃的代价**：全局共享可能通过 timing/usage 形成侧信道；完全禁用会无必要地增加 provider 调用和延迟。

### 决策 11：token blacklist 与 IP rate limit 是否加 tenant 前缀
- **面临的选择**：所有 Redis key 一律 tenant 前缀；安全控制保持 global、业务数据 tenant-scoped；完全重做认证/限流模型。
- **选了哪个 + 为什么**：选择业务数据 tenant-scoped、安全控制显式 global；blacklist 需在 principal 完整构造前按 token hash 查询，IP limit 本来就是跨 tenant 防滥用边界。
- **放弃的代价**：强行 tenant 化 blacklist 会产生解析顺序与撤销漏路；重做认证/限流超出 C13b；所有 key 保持 global 则业务缓存仍不隔离。

### 决策 12：后台恢复怎样取得 tenant
- **面临的选择**：读取当前 SecurityContext/ThreadLocal；通过 document/owner 每次临时推导；在 ledger/message/status 持久化 tenantId 并校验父关系。
- **选了哪个 + 为什么**：选择持久化 tenantId 并在执行时校验，因为恢复发生在无用户请求的线程池中，需要稳定、可审计的身份事实。
- **放弃的代价**：SecurityContext/ThreadLocal 在后台不存在或泄漏；临时推导会在父记录变化、缺失或跨表不一致时产生不确定归属。

### 决策 13：Redis v1 key 如何升级
- **面临的选择**：扫描复制所有旧 key；v2 tenant key 自然冷启动并只从 durable source 重建必要状态；同时读写 v1/v2 双轨。
- **选了哪个 + 为什么**：选择 v2 冷启动，session 重新登录、cache 重算、task projection 从 MySQL ledger 重建，避免扫描未知 Redis 数据。
- **放弃的代价**：扫描复制可能误归属或泄露 key/value；双轨读取会保留无 tenant fallback 并难以退场。

### 决策 14：C13b 是否开放第二业务 tenant
- **面临的选择**：C13b 直接开放 tenant CRUD；C13b 仅完成 enforcement 并用 SQL fixture 测第二 tenant；继续只测 legacy tenant。
- **选了哪个 + 为什么**：选择只用受控 fixture 验证两个 tenant，真实第二 tenant 与管理 API等 C14 后再开放，避免在恶意评测前暴露业务数据。
- **放弃的代价**：立即开放会把 C14 风险交给真实用户；只测 legacy tenant无法证明条件真正区分 tenant。

### 决策 15：何时可以宣称租户隔离成立
- **面临的选择**：C13a identity 就绪；C13b 实现与集成测试通过；C13b 加 C14 隔离/恶意样本评测通过。
- **选了哪个 + 为什么**：选择 C13b+C14 都通过后才宣称成立；C13b 完成时只报告 data-plane enforcement evidence。
- **放弃的代价**：前两种都会把代码存在或可控 fixture 当成对漏路与恶意输入的完整证明，造成过度承诺。

### 决策 16：既有 Milvus 2.3 collection 无法原位增加标量字段时怎样迁移
- **面临的选择**：建立 tenant-aware shadow collection、复制现有 vector/content 并在全量审计后切换 SQL mapping；另立依赖升级闸门并先用真实 contract 证明新版可安全演进既有 schema；继续把 marker 写进当前 `VarChar metadata` 并只在 search 拼表达式。
- **选了哪个 + 为什么**：选择 tenant-aware shadow collection，复制既有 vector/content，经全量审计后原子切换 SQL mapping/readiness；这不改依赖基线、不依赖未证明的 schema evolution，也能让新 collection 对全部 adapter 操作使用独立 tenant/kb 标量字段。
- **放弃的代价**：shadow copy 需要额外 collection 容量、切换与回滚设计；依赖升级扩大兼容验证范围且不保证旧 schema 可原地改变；VarChar workaround 会留下未过滤操作与伪完成证据。

### 决策 17：是否把 OTel Collector 时序波动纳入 C13b
- **面临的选择**：在 C13b 内修改 collector/exporter 时序实现或测试；只记录已知波动并独立复跑；另立小范围维护任务处理稳定性。
- **选了哪个 + 为什么**：选择不扩入 C13b；完整门禁若再次只命中该既有时序失败，记录全仓非 GREEN 与独立复跑证据，后续必要时另立维护任务，因为它不属于 tenant data-plane contract。
- **放弃的代价**：在 C13b 顺手修复会混入无关观测实现并扩大回归面；只忽略失败会伪报完整门禁通过；独立维护会增加一次后续流程但保留范围清晰。
