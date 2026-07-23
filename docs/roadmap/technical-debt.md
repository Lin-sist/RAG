# RAG 项目技术债清单

> 状态日期：2026-07-23
> 本文是从旧维护计划和交接材料中提炼、并按当前代码复核后的待办库存。它不是活动任务计划；每次重大改动应进入独立 OpenSpec change，再从本文移除或标记完成。

## P0：进入下一轮功能迭代前

### 1. 前端正式构建（已完成：2026-07-14）

- 结果：`ignoreDeprecations` 已调整为与 TypeScript 5.7.3 兼容的 `"5.0"`，不再触发 `TS5103`。
- 验证：标准 build 中的 `vue-tsc -b` 与 `vite build` 均通过，并生成正式 `dist/`。
- 证据：`rag-frontend/package.json`、`rag-frontend/tsconfig.json`。

### 2. 认证用户与默认凭据治理（已完成：2026-07-15）

- 已实现：`UserDetailsServiceImpl` 改为数据库用户与角色查询，历史固定管理员种子由前向 migration 精确隔离，并提供默认关闭、外部凭据驱动的一次性管理员 bootstrap。
- 验证：MySQL 8.0.36 Testcontainers 覆盖全新 V1→V6、V5 exact seed、changed-admin、重复 migrate 与 Flyway validate；完整 Maven、Python、前端和敏感日志门禁通过。
- 独立剩余债务：`application.yml` 的开发态 JWT fallback 不属于 C2 登录/refresh 契约，后续需单独治理。
- 证据：`openspec/changes/archive/2026-07-14-database-backed-authentication/`、`rag-auth/.../UserDetailsServiceImpl.java`、`rag-admin/src/main/resources/db/migration/V6__quarantine_known_admin_seed.sql`。

### 3. 补真实依赖集成测试（主链路已完成：2026-07-15）

- 已实现：独立 `c3-integration` Maven/Failsafe 入口使用隔离 MySQL、Redis、etcd、MinIO、Milvus 和 test-scope 确定性 embedding，覆盖登录、上传、异步索引、retrieval、删除与资源清理。
- 验证：主链路重复运行通过；完整 Maven 203 tests、默认 Maven 202 tests、Python 33 tests 与 SensitiveLogs 门禁通过，均为 0 failures/errors/skipped。
- 后续进展：LLM、Redis、Milvus 故障语义、C5a durable input 与 C5b 已实现范围均已按独立 OpenSpec change 接受进 baseline。
- 证据：`openspec/changes/archive/2026-07-15-integration-test-happy-path/`、`openspec/changes/archive/2026-07-15-llm-provider-resilience/`、`openspec/changes/archive/2026-07-15-redis-failure-semantics/`、`openspec/changes/archive/2026-07-15-milvus-failure-semantics/`、`openspec/changes/archive/2026-07-16-durable-index-inputs/`。

### 4. C5 恢复债务（已完成：2026-07-18）

- 已实现：legacy 无 ledger 文档有界隔离；固定有界 coordinator、持续 heartbeat、DB-time 指数 backoff、attempt exhausted `FAILED/TERMINAL`；lease 丢失在 embedding/vector mutation 前 fail closed。
- 已实现：独立 `@Transactional` SQL finalizer 使用 document row lock，原子覆盖 chunks、document 状态/哈希/计数、knowledge-base document count 与 durable task completion；V9 增加 `(document_id, chunk_index)` 唯一约束并兼容清理历史重复行。
- 真实验证：MySQL 8.0.36 覆盖 fresh/V1/V7→V9、legacy 数据、双 claimant、owner/expiry heartbeat、backoff、attempt 终态、finalize 幂等与 rollback；Redis 7 stop/start 覆盖 outage 503 与 durable owner 投影重建。
- 收口状态：`2026-07-18-c5-recovery-debt-closeout` 已通过用户验收，delta 已接受进 baseline，change 已归档且活动任务已恢复 `IDLE`；当前无 C5 已登记实现债务残余。

## P1：下一轮 RAG 质量工程

### 1. 真实 reranker A/B（已完成：2026-07-20）

- C7 已固定 KB、fixture、配置、eval-set 与 Git HEAD，按 `R=3,W=3` 完成 heuristic/NVIDIA 六个 measured runs；comparison=`COMPARABLE`，model 90/90 effective nvidia、fallback=0。
- NVIDIA 相对 heuristic 的 Recall@5/MRR/Top1 观察提升为 +7.84pp/+0.0895/+3.70pp；server-side rerank P50/P95 为 363/688ms，overall P50 增加 188ms。H1 冷启动污染 aggregate P95，不能据此宣称 model 尾延迟更快。
- 30 条开发样本不能外推生产收益；用户已验收 C7 evidence 与结论边界，delta 已接受进 `evaluation` baseline，change 已归档。默认仍保持 heuristic；若未来切换默认 provider，须另立 Type C change。

### 2. 评测数据版本治理与扩充（已完成：2026-07-23）

- C8a 已新增 `rag-eval-dev-v1` manifest、sample schema contract、共享 validator 与 direct/reproducible runner 前置校验；当前 30 条 JSONL 和 3 份 fixture bytes 保持不变。
- custom eval-set 仅能显式降级为 `UNVERSIONED`，不得形成正式 baseline、可比较结论或质量门禁输入；C7 历史报告不追认回写新 version。
- C8a 已由用户验收；4 requirements / 13 scenarios 已接受进 `evaluation` baseline 并归档。
- C8b 已由用户验收并归档：150 条 v2（保留 30 条 seed、新增 120 条）包含 exact quota、fixture grounding/coverage、重复检测、150 条 review sidecar 和 v1/v2 共存；默认 manifest 已切换到 v2，4 requirements / 12 scenarios 已接受进 `evaluation` baseline。

### 3. 分块结构专项

- 来源：承接已关闭 v4 计划中未执行的 Stage 3；后续须重新分级并独立立项，不从旧 v4 计划续跑。
- 验证标题感知、长代码块、长段落和父子块策略。
- 保持 `420/80` 为稳定基线，只做可回滚的单变量实验。

### 4. Claim-level 引用质量

- C9a 已验收归档 objective lexical claim support：确定性句子/列表拆分，只接受 provenance-valid returned citation snippets，按 exact / `0.70` claim-token coverage 输出逐 claim attribution、全 claim 分母与局部完整性状态；4 个 requirements / 12 个 scenarios 已接受进 `evaluation` baseline。
- 该结果只能说明固定算法下的词法证据对齐，不能证明语义蕴含或完整事实正确性；真实 150 条 generation evidence 尚未授权和执行。
- C9b 已验收归档：judge contract/corpus/validator/runner 与 objective/judge/global 状态分离均已落地，4 requirements / 12 scenarios 已接受进 baseline；live canary/full 未授权并按 `SKIPPED` 收口，未来真实校准仍需单独披露调用量、模型、数据出站、费用/限流并授权。
- C10 已验收归档：版本化 profile contract、离线 evaluator、固定类别切片、阈值/容差语义、fail-closed 状态和稳定退出码已落地，4 requirements / 12 scenarios 已接受进 baseline。Reference calls 未授权并按 `SKIPPED` 收口，首个 retrieval profile 保持 DRAFT；正式 reference evidence、具体阈值和 ACTIVE quality gate 仍是后续独立 evidence/activation 工作，不得把本次归档解释为质量达标。

### 4. SSE 结构化结果

- 当前流式路径只输出文本 chunk，历史保存 citations 为空。
- 需要设计兼容的结构化完成事件，明确 citations、contexts、metadata 和中断语义。

### 5. 可观测性与恢复演练（C11 tracing core 已完成：2026-07-23）

- C11 已建立默认关闭、fail-open 的 OTel 1.31 进程内 tracing core：分离 ingest/ask trace、固定实际执行阶段、稳定 task/document/chunk lineage、W3C/custom context、MDC bridge、同步/流式终态与隐私白名单；4 requirements / 12 scenarios 已接受进 `rag-system` baseline 并归档。
- C12 仍需独立立项决定 network exporter、metrics、alerts/dashboard、production sampling、retention、权限、部署和真实 backend 容量/费用；C11 未发送外部 telemetry，不能解释为生产观测栈完成。
- LLM 429/503/timeout、Redis/Milvus 不可用语义已完成；继续演练索引输入丢失、进程中断与恢复。

## P2：基线稳定后

- 组织/租户模型与强制 tenant filter。
- 前端统一设计 token、空态/错态/处理中态和可访问性。
- 生产数据评测集扩充与反馈闭环。
- 有界 Query Router：按 fact、multi-hop、global、no-answer 选择策略。
- MCP 只读知识资源和搜索/问答工具。
- Agentic RAG 仅在前述能力有评测门禁后进入。

## 不应重复立项

以下能力已经存在，不应继续以“从零接入”方式创建任务：

- BM25 + dense vector + RRF hybrid retrieval。
- Reranker 接口、heuristic 实现、HTTP model adapter 和失败降级。
- 固定评测 KB、`--preflight-only`、只读 `--keep-existing`。
- citation validation/fallback 与 no-answer 引用抑制。
- generation/citation/no-answer 客观指标通道。

## 已完成的治理基础

- 根目录 `AGENTS.md` 已建立统一协作规则。
- `.ai/ACTIVE_TASK.md` 已作为唯一活动任务指针。
- `.ai/AGENT_LOG.md` 已用于追加执行证据。
- `openspec/` 已包含 project context、baseline specs 和 change 生命周期。
