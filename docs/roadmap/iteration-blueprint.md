# Enterprise RAG QA System｜迭代蓝图（Iteration Blueprint）· v5(已冻结)

> 文档性质：长期迭代的"母文档 / 宪章"，不是可执行的 OpenSpec change。
> 状态日期：2026-07-14
> 本版变更（经四轮复核冻结正式通过）：
> - §0 第 4/5 条明确"归档/proposal.md"要求仅约束 Type C，消除对 Type B（B0/B1）的字面覆盖（gap #1）。
> - B0 隐私清扫范围扩为普通日志各级别中的全部用户私密内容（含文件名、文档标题、query variants、prompt/context 片段、可能携带用户内容的异常消息），对齐 rag-system spec 第 60–68 行（gap #2）。
> 经四轮 codex 代码复核，蓝图正式冻结通过，作为后续所有 change 的方向基线。

## 0. 给 codex 的阅读与执行约定

1. 本文件是方向蓝图，不覆盖 `AGENTS.md` 与 `openspec/project.md`。冲突以 `AGENTS.md` 为准。
2. **分级适用范围**：本节第 3 条"创建正式 change 目录并激活 ACTIVE_TASK"**仅适用于 Type C 重大变更**；Type A（只读）与 Type B（小范围维护）一律按 `AGENTS.md` 分级执行，不创建 change 目录，但 Type B 必须追加 `AGENT_LOG`。
3. （仅 Type C）正式启动时，在 `openspec/changes/<change-id>/` 生成 proposal/design/tasks/spec delta，并把 `.ai/ACTIVE_TASK.md` 置为 ACTIVE。
4. **（仅 Type C）** 一次只激活一个 change，串行推进，完成并经用户确认后归档。（Type B 不进入此 change 目录/归档流程，仅追加 `AGENT_LOG`。）
5. **每个 Type C change** 的 proposal.md 必须含 **「用户故事（大白话）」**：以"改之前会发生什么糟糕的事 → 改之后实际使用有什么不同"的具体场景表述，面向非专家可读。硬性要求。（Type B 无需 proposal.md，但仍建议在 AGENT_LOG 中用一句大白话说明改前/改后差异。）
6. 外部调用：NVIDIA NIM 对本项目模型免费一事，是**作者提供的外部假设，非可由代码/spec 证实的事实**；即便成本为零，任何批量 rerank/ask/judge/embedding 前仍须按 accepted spec 说明**调用量、数据出站、模型、限流，以及费用及零费用依据**并取得授权，并对批量调用做节流/重试。该披露与授权要求**适用于所有实际产生批量外调的 change**，包括但不限于 C3 的真实 embedding provider 路径、C6/C7 的 rerank、C9b 的 judge 校准；对齐 evaluation spec 第 47–54 行。

## 1. 迭代总方向

把项目从"可运行的模块化 RAG 工程原型"推进为
**"能独立支撑真实业务、每一步都有数据说话的可信企业知识服务，并可增量扩展为受约束的 Agent 知识底座"**。

主干依赖链（严格串行）：

- 既存债务清扫（Type B）
- → 数据库认证与凭据治理
- → 真实 happy-path 集成测试
- → 故障契约与索引恢复
- → provider 归因
- → reranker A/B 与评测门禁
- → 可观测性（trace 核心 → 导出/指标）
- → 租户隔离及隔离评测
- → MCP / Router（隔离评测通过后，二者独立、不捆绑）

## 2. 作者已确认的决策

- **决策 A（B1 分级）**：`frontend-build-baseline-fix` 按 **Type B** 执行，措辞冲突已在第 0 节第 2 条消除；该项在早期 Type B 中编号为 **B1**。**冻结后首个开工项为 B0 `sensitive-log-redaction`（先于 B1）**，避免启动顺序歧义。
- **决策 B（用户创建能力）**：数据库认证首版**只做管理员预置 / 一次性 bootstrap**，不含注册/用户管理 API。用户创建 API 为**旁支可选 change（旁支 Type C，不占主线编号、不阻塞主线）**。
- **决策 C（prod secret 守卫，精确化）**：JJWT 已在所有 profile 拒绝过短 HMAC key；本项目自定义 prod guard **只补"已知默认值 / 空值 / 误配置"的启动校验**，长度以 **UTF-8 字节** 定义，命中即 fail-fast。
- **决策 D（故障降级语义）**：不在蓝图层预定义，放到故障契约相关 change 展开。
- **决策 E（旁支定位）**：`user-provisioning` 从主线序号移出，标为**旁支 Type C**；**跳过它可直接进入 C3（happy-path 测试）**，不占主线、不阻塞。

## 3. change 序列总览（拆分并新增后）

> change-id 为语义占位，启动时补日期前缀。B=小范围维护（Type B），C=重大变更（Type C）。

### 早期 Type B（既存债务清扫，可尽快开工）

| id | 一句话目标 | 分级 |
|---|---|---|
| `sensitive-log-redaction`（B0） | 清除普通日志各级别中的全部用户私密内容（修复既存 spec 违规） | Type B |
| `frontend-build-baseline-fix`（B1） | 修 TS5103，让含 vue-tsc 的正式 build 通过 | Type B |

### 主线 Type C

| 顺序 | 建议 change-id 语义部分 | 一句话目标 |
|---|---|---|
| C1 | `jwt-secret-production-guard` | prod 下已知默认/空/误配置 secret 启动 fail-fast |
| C2 | `database-backed-authentication` | 认证改数据库用户 + 一次性 bootstrap + 清理默认 admin 种子 |
| C3 | `integration-test-happy-path` | Testcontainers 覆盖主链路，含确定性 embedding 边界 |
| C4b | `llm-provider-resilience` | LLM 429/503/timeout 降级（含公共契约矩阵模板） |
| C4c | `redis-failure-semantics` | Redis 重启/不可用语义（缓存/登录/任务状态） |
| C4d | `milvus-failure-semantics` | Milvus 不可用（失败 vs 部分降级） |
| C5a | `durable-index-inputs` | 索引输入持久化 |
| C5b | `index-task-reconciliation-and-resume` | 孤儿任务协调与中断续跑 |
| C6 | `nvidia-reranker-adapter-and-attribution` | NVIDIA ranking 协议适配 + 逐样本 effective provider/fallback 归因 |
| C7 | `reranker-ab-evaluation` | heuristic vs model 可复现 A/B（逐样本延迟 + 覆盖阈值） |
| C8a | `eval-dataset-schema-and-versioning` | 固定 question set/fixture corpus/schema/标注版本/内容哈希 |
| C8b | `eval-dataset-expansion-and-annotation` | 扩充至 100~300 条并标注 |
| C9a | `claim-evidence-objective-metrics` | claim 拆分与 claim-evidence 客观对齐 |
| C9b | `judge-calibration-and-status-semantics` | judge 校准 + objective/judge 状态分离 |
| C10 | `eval-quality-threshold-gates` | 按 profile/类别/指标通道的阈值门禁与退出码 |
| C11 | `genai-tracing-core` | OTel API/SDK + ingest/ask 双 trace 模型 + 进程内验证 |
| C12 | `otel-export-and-metrics` | exporter、阶段 metrics、部署配置（隐私/高基数防护） |
| C13a | `tenant-model-context-and-migration` | 租户模型、上下文与迁移 |
| C13b~ | `tenant-enforcement-across-data-planes` | 跨数据面服务端强制隔离（精确子拆分见 §4，设计期确定） |
| C14 | `tenant-isolation-evaluation` | 隔离与恶意文档评测（开放外部接口前门禁） |
| C15 | `mcp-readonly-service` | 知识库暴露为 MCP 只读 Resources/Tools |
| C16 | `bounded-query-router` | 受约束 Router，首版做 fact 策略 + 统一 no-answer policy |

> **C4 契约矩阵说明**：不再设独立的 `failure-contract-matrix`（C4a）作为单独 change。原因：若单独归档并把 delta 接受进 baseline，代码尚未实现 C4b/c/d，baseline 会立即领先代码；若不归档又会占死唯一 active change。改为把**公共故障契约矩阵作为 C4b/C4c/C4d 各自 design 的统一模板**，每个 change 只接受并实现自身依赖的契约。

### 旁支 / 后续

| id | 定位 |
|---|---|
| `user-provisioning`（旁支 Type C） | 旁支可选：确需自助创建用户时才启动；因其修改 API/权限/持久化，按 `AGENTS.md` 必须走 Type C 与 OpenSpec，**不得按 Type B 跳过** |
| `router-multihop` / `router-global` / `router-high-risk` | Router 高级策略逐项独立推进（含人工确认设计） |

关键顺序纪律：

- 先修既存债务（B0 → B1 两个 Type B）→ 再进 C1。
- 先有数据库用户（C2）→ 再写登录联合测试（C3）。
- 先建 happy-path（C3）→ 再定义故障语义（C4x）；索引恢复（C5x）独立。
- 先接通并可归因 provider（C6）→ 再做证明收益的 A/B（C7）。
- 租户隔离（C13x）通过隔离评测（C14）后，才开放 MCP（C15）或 Router（C16）；MCP 不依赖 Router，二者不捆绑。

## 4. 每个 change 的意图卡片（已按 codex 四轮复核校正）

### B0. sensitive-log-redaction（Type B，冻结后首个开工项）
- 现状事实：accepted `rag-system` spec（第 60–68 行）禁止**全部用户私密内容**进入普通日志，但当前 `QAController`、`RAGServiceImpl`、`PromptBuilder` 记录问题文本或截断内容，`KnowledgeBaseController` 还在 INFO 日志记录上传文件名，均属既存违规。
- 目标：**清除普通日志各级别中的全部用户私密内容，包括但不限于 question/query、query variants、prompt/context/snippet、文件名与文档标题，以及可能携带用户内容的异常消息**；确保上述内容不进入普通日志。
- 用户故事：改之前用户提问原文、上传的文件名/标题等会明文落到普通日志里，存在隐私泄露风险；改之后日志不再包含任何用户私密内容。
- 分级说明：属 accepted spec 一致性修复，**无需新增 spec delta**，按 Type B 执行并追加 AGENT_LOG。

### B1. frontend-build-baseline-fix（Type B）
- 目标：修复 TS5103（ignoreDeprecations:"6.0" 与 TS 5.7.3 不兼容），标准 build 通过。
- 用户故事：改之前 `npm run build` 报错、产不出可部署包；改之后能打出正式 `dist/`。
- 非目标：不重做 UI、不升级无关依赖；**部署与演示链接属 out_of_scope**。
- 验收：含 vue-tsc 的正式 build 通过，禁止用单独 vite build 冒充。

### C1. jwt-secret-production-guard
- 现状校正：JJWT 已在所有 profile 拒绝过短 HMAC key。
- 目标：prod profile 下补"已知默认值 / 空值 / 误配置"启动校验，长度按 UTF-8 字节定义，命中 fail-fast。
- 用户故事：改之前生产可能带着已知默认/空密钥启动、有伪造令牌风险；改之后误配置直接起不来。
- 非目标：不改认证数据源（留 C2）；不重复实现 JJWT 已有的长度校验。

### C2. database-backed-authentication
- 现状校正：数据库已有用户/角色/权限表与默认 admin 种子，但认证仍走内存用户；"账号非数据库持久化管理"（并非简单"重启即重置"）。
- 目标：新增 entity/mapper/repository，`UserDetailsService` 改数据库实现，一次性 bootstrap，**清理迁移中的已知默认 admin 种子**，首次密码来自外部注入或一次性 bootstrap。
- 用户故事：改之前只有写死的假账号且带已知默认凭据；改之后账号在库、密码加密、无固定可登录凭据。
- 非目标（决策 B）：不含注册/用户管理 API。

### C3. integration-test-happy-path
- 现状校正：仅 Testcontainers 拉起 MySQL/Redis/Milvus 不足以消除 embedding provider 依赖。
- 目标：覆盖登录→上传→索引→检索→删除主链路，并**定义本地确定性 embedding 边界**（固定假向量或本地小模型）以保证无外部依赖、可重复；如确需真实 embedding provider，须显式标注并走授权（含费用及零费用依据，见第 0 节第 6 条）。
- 用户故事：改之前依赖缺失时测试自跳过、全绿不可信；改之后一条命令确定性验证主链路。
- 非目标：不含故障注入（留 C4x）、不追求高覆盖率。

### C4b~C4d. 故障契约（拆分）
- 公共故障契约矩阵作为 C4b/c/d 各自 design 的统一模板（不设独立 C4a，见 §3 说明）；C4b/C4c/C4d 分别落 LLM、Redis、Milvus 的降级语义（决策 D 在此展开），每个 change 只接受并实现自身依赖的契约。
- 用户故事：改之前依赖抖动行为不明、可能整体崩；改之后每种故障有明确、被测试锁定的降级行为。

### C5a~C5b. 索引恢复（拆分）
- 当前进展：C5a 与 C5b 已实现范围均已接受进 baseline；上传输入使用 durable filesystem，新任务使用 MySQL ledger、稳定 taskId、phase checkpoint 与 Redis 可重建投影。
- C5b 已锁定并实现保守边界：SAFE_PRE_VECTOR/VECTOR_CONFIRMED 才可在显式开关下恢复，VECTOR_IN_FLIGHT/outcome unknown 只进入协调状态，不自动重放。
- C5 遗留的 legacy 隔离、持续 heartbeat/backoff、attempt exhausted、finalize 严格幂等与真实 MySQL/Redis/crash-window 验证已在 `2026-07-18-c5-recovery-debt-closeout` 完成；用户验收通过后，delta 已接受并归档，上述 C5 实现债务已清零。

### C6. nvidia-reranker-adapter-and-attribution
- 现状校正：当前 adapter 为通用 documents/results/relevance_score，registry 失败会 fallback 到 heuristic；现有单测只验证自建 mock 协议。
- 目标：适配 NVIDIA ranking 真实协议；**逐样本**记录 requested/effective provider、fallback count/reason、model 调用数与实际覆盖样本数。
- 用户故事：改之前无法证明重排是否真用了模型（失败静默 fallback）；改之后每次可看清用了哪种重排、fallback 几次。
- 非目标：本 change 不下收益结论（留 C7）。
- 当前进展：已实现独立 NVIDIA ranking adapter、typed outcome、整样本 fallback、同步 QA/debug/runner attribution，并通过本地合成协议测试与完整回归；用户已验收，delta 已接受并归档。默认仍为 heuristic，真实 NVIDIA endpoint smoke 未执行，边界为 protocol-tested/real-endpoint-unverified。

### C7. reranker-ab-evaluation
- 目标：固定 KB/fixture/配置/Git HEAD，对比 heuristic 与 model 的 Recall@5、MRR、Top1、**逐样本延迟 P50/P95** 与降级行为。
- 报告有效性硬规则（强化）：仅排除"全程 fallback"不够，**部分 fallback 同样污染 A/B**；干净 model 组须 **100% effective-model 覆盖**，或明确覆盖阈值并单独报告；记录逐样本 effective provider 并定义可比较性条件。

### C8a~C8b. 评测集（拆分）
- 现状校正：当前 30 条为 fact/definition/reasoning/multi_hop/no_answer 五类。
- C8a：版本化须同时固定 question set、fixture corpus、KB 身份、schema、标注版本、内容哈希与配置/Git HEAD（对齐 evaluation spec 第 5–14 行）。
- C8b：扩充至 100~300 条并标注，明确分类配额。
- 非目标：**暂不构造"权限隔离/恶意文档"样本**（依赖租户模型，留 C14）。

### C9a~C9b. claim 与 judge（拆分）
- 现状校正：当前已有 citation snippet、unsupported citation、no-answer 及可选 judge 的 faithfulness/relevance；**并非只有关键词命中**。
- C9a：claim 拆分与 claim-evidence 客观对齐。
- C9b：judge 校准；修复"judge 全失败仍 CLEAN"缺口——**judge 全失败可令全局状态 PARTIAL，但不得把干净的 retrieval/objective citation 指标也标成不可比较**，须分别表达 objective status 与 judge status（对齐 evaluation spec 第 36–45 行）。judge 校准若产生批量调用，须按第 0 节第 6 条披露调用量/出站/模型/限流/费用及零费用依据。

### C10. eval-quality-threshold-gates
- 目标：定义按 profile/类别/指标通道的阈值、容差、缺失值与错误处理规则，并给非零退出码形成门禁。

### C11. genai-tracing-core
- 现状校正：当前只有 MDC traceId 与请求耗时日志，无 OTel span/阶段 metrics。
- 目标：只建立 OTel API/SDK、**分离的 ingest trace 与 ask trace 模型**及进程内验证；ask 通过 document/chunk lineage 链回 ingest 产物，而非把 ingest 当问答子 span。**明确与 C12 的依赖边界**（本 change 不含 exporter/metrics/部署配置）。

### C12. otel-export-and-metrics
- 目标：接入 exporter、阶段 metrics 与部署配置。
- 隐私/高基数硬规则：候选文档 ID/分数**不得直接作 metrics 标签**，应作为受采样、受权限控制的 span event/debug artifact；原始问题、正文、snippet 不入普通日志（注意：既存违规已由 B0 提前修复，本 change 只保证新增遥测不再引入）。

### C13a~C13b. 租户隔离（拆分）
- 现状校正：当前已有 owner/public/用户级 KB 授权，**并非"所有用户共享无隔离"**；缺租户级模型。
- C13a：租户模型、上下文与迁移（可先做兼容性暗铺设）。
- C13b~：**强制 tenant filter 必须由服务端身份推导，不得信任客户端 metadata filter**，并覆盖 SQL、Milvus、cache、task、history 等数据面。
- **Vector adapter 覆盖范围**：代码中除 Milvus 外仍存在 `QdrantVectorStore`、`ElasticsearchVectorStore`。若首版只支持 Milvus tenant mode，**必须把 Qdrant/Elasticsearch 明确列为 `out_of_scope`，并在 tenant mode 下拒绝启用，而不是静默缺少隔离**。
- **精确子拆分留待 change 设计期**：C13b 若过大，建议在正式 design 中进一步拆为「SQL/API/权限强制隔离」「索引与全部启用的 vector adapters」「cache/task/history 命名空间」等切片；蓝图层不锁死具体切分，只固定上述 out_of_scope 与"服务端身份推导"两条边界事实。

### C14. tenant-isolation-evaluation
- 目标：补隔离与恶意文档评测样本与断言，作为**开放任何外部接口（C15/C16）前的门禁**。

### C15. mcp-readonly-service
- 现状校正：当前已有 REST + Swagger，**并非"只能通过前端访问"**；准确缺口是"无标准 MCP 接口"。
- 目标：把知识库暴露为 MCP 只读 Resources 与 Tools（search/ask/get-citation/compare-sources），含认证与 tenant 传播。
- **只读语义冻结**：当前 `ask` 确实会增查询计数、写 QA history、可能写 cache（`QAController`、`RAGServiceImpl`）。首版只读语义冻结为——**禁止修改 KB/文档/索引/用户/租户，且不写 QA history 与查询计数；仅允许受租户隔离、TTL 与隐私规则约束的 cache / metrics / audit 等非权威技术写入**。为此须提供无 history/count 的只读问答路径。
- **版本 Resource 边界**：当前只有乐观锁 `version` 字段、无文档版本历史领域模型。**首版从目标中移除"文档版本 Resource"，并明确列为非目标（out_of_scope）**，待未来单独增加文档版本能力后再纳入。
- **外部调用**：`ask`/`compare-sources` 可能触发 embedding/rerank/LLM，只读不等于无数据出站/无限流，须沿用第 0 节第 6 条的外部调用授权与预算限制（含费用及零费用依据）。
- 非目标：不依赖、不捆绑 Router。

### C16. bounded-query-router
- 现状校正：当前已有 query normalization、query variants 与解释型问题 fallback；准确缺口是"没有显式、可观测、预算受限的策略分类与有效策略归因"。
- 目标：受约束 Router，**首版只做 fact 策略**，每条策略有最大步数、token、超时预算与有效策略归因。
- **no-answer 定位**：拒答是检索后依据证据充分性得出的结果，**不作为纯前置路由类别**；首版路由到 fact 策略后执行**统一 evidence/no-answer policy**。
- **人工确认 spec 映射**：high-risk 人工确认的状态/交互当前为 unknown，其 spec delta **移至未来 `router-high-risk`**，不进首版 delta。
- 后续：multi-hop、global summary、high-risk 逐项独立推进。

## 5. spec delta 落点

| Change | 建议 baseline spec |
|---|---|
| B0 log redaction | 无 delta（accepted spec 一致性修复）。 |
| B1 frontend build | 无 delta（AGENTS.md 已有正式 build 规则）。 |
| C1 secret guard | `rag-system`：生产 secret 启动拒绝语义。 |
| C2 db auth | `rag-system`：用户持久化、密码状态、bootstrap 边界、刷新时重载用户。 |
| C3 happy-path | 测试容器无 delta；**确定性 embedding 边界仅在修改生产 provider 接口或正式运行语义时才落 `rag-system`；若只是 test-profile 下的 fake/local adapter，则不建长期 spec delta**。 |
| C4b~d / C5a~b | 故障降级与任务恢复行为落 `rag-system`（公共契约矩阵作为各 change design 模板）。 |
| C6 adapter/attribution | `rag-system`：NVIDIA provider 契约、逐样本 effective provider、fallback。 |
| C7 A/B | `evaluation`：A/B 身份、延迟、provider 覆盖与报告有效性。 |
| C8a~b / C9a~b / C10 | `evaluation`：数据集版本与 fixture 固定、分类配额、claim 指标、objective/judge 状态分离、阈值与退出码。 |
| C11 / C12 | `rag-system`：ingest/ask trace、阶段 span、隐私、高基数、provider/token/fallback/cache 语义。 |
| C13a~b / C14 | `rag-system`：tenant 模型、服务端强制过滤、跨数据面隔离、Qdrant/Elasticsearch 的 out_of_scope 与 tenant mode 拒绝启用；`evaluation`：隔离与恶意样本。 |
| C15 MCP | `rag-system`：Resources/Tools、只读边界（禁改业务资源、不写 history/count、仅允许受约束非权威技术写入）、文档版本 Resource 列为 out_of_scope、外部调用约束、认证与 tenant 传播。 |
| C16 Router | `rag-system`：策略分类、预算、有效策略归因、统一 no-answer policy；`evaluation`：按策略独立指标与门禁。**人工确认状态不进本 delta。** |
| user-provisioning（旁支 Type C） | `rag-system`：用户创建/管理 API、权限与持久化行为（走完整 Type C + OpenSpec 流程）。 |

> 注意：`agent-collaboration` spec 是 Codex/OpenSpec 协作治理，**不是运行时 Agentic RAG**。MCP、Router、人工确认不要因名称含 "Agent" 就误放进去。
