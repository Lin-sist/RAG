# Tasks: C13b Tenant Data-Plane Enforcement

## 0. 事前闸门（实现前必须完成）

- [x] 用户审阅并批准 proposal 的范围、非目标与完成口径。
- [x] 用户确认 design 的 15 条决策，重点确认 child table tenantId、跨 tenant 404、tenant-local public、Milvus-first、unsupported adapter fail startup、legacy vector maintenance/readiness 与 Redis v2 key 冷启动。
- [x] 用户审阅并批准 `rag-system` spec delta；确认 C13b 完成仍不能代替 C14。
- [x] 明确提交责任；2026-07-27 用户授权 Agent 对当前阶段 C13b 文件分段提交，不包含 push、PR 或部署。
- [x] 实现开始前复查 `git status --short --branch`，补录上一规划提交 hash（若用户已提交），保护用户改动。

## 1. V11 Migration And SQL Tenant Roots

- [x] RED：新增 V10→V11 有数据 fixture，覆盖 tenant A/B、正常/逻辑删除 rows、nullable KB history、durable/legacy task 与刻意 parent/user mismatch。
- [x] 新增 `V11__tenant_data_plane_enforcement.sql`：为 document/chunk/kb_permission/history/feedback/async_task 增加 tenantId，按父事实回填并在 mismatch/null/orphan 时 fail closed。
- [x] 调整 tenant-aware unique/index，最后改 `NOT NULL`；不修改 V1-V10，不清空/重建业务表。
- [x] 更新 entity/mapper 与 migration assertions；验证 fresh install、V10→V11、Flyway validate、业务 ID/owner/public/status/task phase 保持。
- [x] 运行 migration 聚焦测试并把命令、用例数和结果追加 `.ai/AGENT_LOG.md`。

## 2. SQL/API/Permission Enforcement

- [x] RED：双 tenant service/controller tests 覆盖 KB detail/list/update/delete/statistics、document list/delete、history/feedback 与 permission grant/read/write/admin。
- [x] controller 用户路径统一取得 `RequestIdentity`；移除以裸 userId 作为完整授权上下文的调用。
- [x] 重构 `AuthorizationService` 与 KB/document/permission service，使每次 lookup/list/count/update/delete 都包含 tenant predicate。
- [x] 将 `is_public` 锁定为 tenant-local；跨 tenant owner/public/permission 返回 not-found，同 tenant无权限返回 forbidden。
- [x] 禁止跨 tenant `kb_permission` 写入，并验证 permission target user 与 KB tenant 一致。
- [x] history/feedback save/read/list/delete/duplicate check 全部 tenant-aware，不只依赖 userId/qaId。
- [x] 更新 DTO/mapper tests，确认 REST 请求形状不增加 tenant selector、响应默认不暴露 tenantId。
- [x] 运行 SQL/API/permission 聚焦测试与裸 mapper main-source 扫描，追加执行证据。

## 3. Task, Recovery And Durable Input Enforcement

- [x] RED：双 tenant task tests 覆盖 status/result/cancel/exists/completed、Redis projection miss、durable fallback、lease recovery 与 SQL finalize。
- [x] 为 `IndexTaskRecord`、ledger、`TaskStatus`、message/projection 增加 tenantId；submit 只从 `RequestIdentity` 捕获 tenant scope。
- [x] ledger create/find/claim/heartbeat/phase/finalize/update 全部带 tenant predicate；system-wide scan 返回的每条 record 必须有合法 tenantId。
- [x] recovery executor 从 durable record 构造 immutable execution scope，并校验 task/document/KB/owner tenant 一致；禁止 SecurityContext/ThreadLocal 推导。
- [x] `IndexTaskSqlFinalizer` 在同一事务中锁定并验证 tenant-scoped task/document/chunks。
- [x] `IndexInputStore` storage key/path/cleanup 与业务 lock 加 tenant namespace；验证跨 tenant 相同资源名不冲突。
- [x] 运行 task/recovery/input 聚焦测试与 ThreadLocal 扫描，追加执行证据。

## 4. Redis And Cache Namespace V2

- [x] RED：key contract tests 覆盖 session、QA、embedding、idempotency、task projection 的 tenant namespace 与 payload mismatch。
- [x] auth session 升级为 v2 tenant key，refresh/logout 校验 token/fresh principal/session tenant；旧 session 不双读并要求重新登录。
- [x] QA cache key 使用 tenantId+KB id，evict/clear 只能 tenant-local；删除业务路径的全局 `qa:cache:*` 清理。
- [x] embedding cache 使用 tenantId+effective provider/model+content hash；tenant-local evict/clear，Redis fail-open 只能变成 miss/重算。
- [x] idempotency key 绑定 tenantId+userId+endpoint/key，关键写请求缺 identity 时 fail closed。
- [x] task projection 使用 v2 tenant key/payload，并只从同 tenant durable record 重建。
- [x] 保留 token blacklist/global IP rate limit 的显式 global security scope并补边界测试，不把它们包装成 tenant cache。
- [x] 运行 Redis/cache 聚焦测试与旧业务 key 扫描，追加执行证据。

## 5. RAG, Reserved Filter And Keyword Scope

- [x] RED：QA sync/SSE/debug tests 覆盖 tenant A/B、tenant-local KB、client reserved filter 与 vector failure→keyword fallback。
- [x] 新增 core immutable tenant vector/query scope，admin 只能从 tenant-scoped KB 解析；业务 API 不接受裸 collectionName 作为 scope。
- [x] 调整 `QARequest/RetrieveOptions/SearchOptions/RAGService/QueryEngine` 数据流，分离服务端 scope 与普通 metadata filter。
- [x] 拒绝 tenant/kb/collection reserved filter 的 camel/snake/case aliases；普通 filter 只与服务端 scope 做 AND。
- [x] `KeywordIndex`/bootstrap/map key 绑定 tenantId+KB id；vector 故障降级仍保持相同 tenant scope。
- [x] QA history 保存、query count、source title enrich 与 citation context lookup 保持同一 RequestIdentity。
- [x] 运行 sync/SSE/debug/keyword 聚焦测试，确认 retrieval/rerank/citation/no-answer 指标口径未变。

## 6. Milvus Tenant Adapter Contract

- [x] RED：建立可复用 adapter contract suite，覆盖 create/has/upsert/search/get/getByIds/delete/count/drop 的双 tenant隔离与 marker mismatch。
- [x] Milvus upsert 强制服务端 tenantId/kbId marker，拒绝冲突 metadata。
- [x] Milvus search 将 tenantId+kbId 与普通 filter 安全 AND；不接受任意 reserved field/expression 注入。
- [x] Milvus get/getByIds/delete/count/drop 全部 tenant-scoped；缺 marker/mismatch fail closed，不以空结果掩盖隔离错误。
- [x] 新 KB 使用 canonical tenant-aware collection namespace；legacy collection 只能经 SQL scope mapping 使用。
- [x] 增加 adapter capability/startup guard：未通过 contract 的 Qdrant/Elasticsearch 在 enforcement 模式下拒绝启动，不 fallback。
- [x] 使用既有 Testcontainers Milvus 运行 adapter contract；记录容器版本、操作、错误分类与真实模型调用=0。

## 7. Legacy Vector Maintenance And Readiness

- [x] 实现期事前闸门：用户确认决策 16=A，采用 tenant-aware shadow collection，复制既有 vector/content，全量审计后原子切换 SQL mapping/readiness；不升级依赖，不实现 VarChar/search-only workaround。
- [x] RED：构造 missing/mismatch/duplicate/partial legacy vector fixtures，验证 runtime 非 READY 时 fail closed。
- [x] 实现默认关闭、非 REST 的 maintenance audit/backfill 入口；runtime `VectorStore` 不暴露 unscoped legacy read。
- [x] 从 tenant-scoped SQL 读取 KB/current mapping/document/chunk/vector identity，只读 source collection 并把既有 vector/content/metadata 复制到 tenant-aware shadow collection；不得调用 embedding/rerank/LLM，不修改或删除 source collection。
- [x] 记录 expected/observed/migrated/missing/mismatch 和稳定错误类别；仅 shadow 全量一致且无错误时，在同一 SQL 状态转换中切换 active collection mapping 并写 KB readiness；部分失败保持原 mapping + 非 READY。
- [x] 空 KB/new KB 的 READY 规则可重复执行；部分失败不得留下可服务的 false READY。
- [x] 在任何真实 Milvus audit/backfill 前，向用户披露 collection/record 数、读写范围、数据出站、超时/重试与回滚风险并取得单独授权。
- [x] 真实 maintenance 未获授权时明确 `SKIPPED`，不能用 mock/unit 结果宣称现有数据已迁移。
- [x] 当前真实 Milvus 只授权只读盘点；任何真实 collection 创建、复制、mapping/readiness 切换、重试或清理必须另行授权。

## 8. Full Gates And Closeout

- [x] 运行各模块聚焦测试后执行 `mvn -q test`，记录 Surefire/Failsafe tests/failures/errors/skips。
- [x] OTel collector 既有时序波动不扩入 C13b；若完整门禁再次仅命中该项，记录非 GREEN 与独立复跑证据，必要时另立维护任务。
- [x] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`；确认 evaluation contract 未变化。
- [x] 前端无改动时正式 build 记为 `SKIPPED`；若有前端/DTO 联动则运行包含 `vue-tsc` 的正式 build。
- [x] 运行 SensitiveLogs、protected paths、credential、ThreadLocal、裸 tenant-bypass mapper/vector/cache key、Markdown links 与 `git diff --check` 门禁。
- [x] 将每个 requirement/scenario 对应到测试或真实 evidence；`RETRIEVAL_ONLY/PARTIAL/mock` 不得替代隔离结论。
- [x] 更新 tasks、`.ai/AGENT_LOG.md`、`openspec/project.md` 与相关 architecture/roadmap/optimization 文档；保持 C14/C15/C16 边界。
- [x] 用户验收后才把 delta 原文接受进 baseline、归档 change 并将 `.ai/ACTIVE_TASK.md` 置为 `IDLE`。
- [x] C13b 收口措辞只写“data-plane enforcement 已实现并通过指定测试”；C14 前不宣称租户隔离成立，不开放第二业务 tenant/MCP/Router。

### 实现证据映射（2026-07-27）

- SQL/API/permission scenarios：`TenantDataPlaneMigrationMySqlTest`、`C5RecoveryMySqlTest`、tenant enforcement service/controller suites 与裸 mapper 扫描。
- task/cache scenarios：task/recovery/input/projection 聚焦 suites、QA/embedding cache v2 key/payload mismatch tests 与旧 key runtime 扫描。
- RAG/keyword scenarios：`QAControllerTest`、`ReservedScopeFilterTest`、`QueryEngineImplTest`、`InMemoryBm25KeywordIndexTest` 及 telemetry regression。
- Milvus scenarios：`MilvusVectorStoreFailureSemanticsTest` 与本地 `milvusdb/milvus:v2.3.4` Testcontainers `MilvusFailureSemanticsIT`；全部为合成数据，真实模型调用 0。
- legacy/readiness scenarios：V12 migration tests、`KnowledgeBaseServiceImplTest`、`DocumentIndexingServiceImplTest`、`VectorShadowMigrationServiceTest` 的 missing/mismatch/duplicate/partial/empty/success fixtures。
- full-gate scenarios：Python 159 tests、SensitiveLogs、protected path/credential/ThreadLocal/bare vector/cache/Markdown/diff scans；全仓 Maven 因唯一 OTel 时序断言保持非 GREEN，该用例独立复跑通过且不扩入 C13b。
