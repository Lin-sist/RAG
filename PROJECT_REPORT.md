# Enterprise RAG QA System｜项目事实报告（PROJECT_REPORT.md）

> 报告生成时间：2026-10-11  
> 事实依据：基于代码仓库（Git HEAD `31ad3f8`）、配置文件、Flyway 数据库迁移脚本、OpenSpec 迭代归档与实测报告。  
> 编写原则：只陈述代码、配置、测试与文档中客观存在的事实，标明具体文件路径与类/方法名；未完成或未验证内容明确标注，不推测、不美化。

---

## 执行摘要（Executive Summary）

1. **项目定位**：企业级证据型知识库问答工程原型（Java 17 + Spring Boot 3.2.1 + Milvus + Vue 3），主打可信生成、严格引用校验与离线质量门禁。
2. **核心链路**：认证与租户注入 → 异步文档 Durable 索引入库 → 向量/BM25 混合召回 + RRF 融合 → Heuristic/NVIDIA 重排 → LLM 生成（Token 预算控制） → 引用来源校验 → SSE 结构化终端流与历史持久化。
3. **AI 核心参数**：切分 `420/80`（语义分块）；Dense+BM25 双路召回（`rrf-k=60`）；重排 `top-n=20`、`top-k=5`（默认 Heuristic）；引用校验重叠阈值 `0.58`；Prompt 上下文 Token 预算 `1200`。
4. **Agent 现状**：**无自主 Agent 循环，无 LLM Tool Calling**；仅实现了外部只读 MCP Server 接口（C15）与单轮闭域事实路由器（C16 `fact-v1`，默认关闭）。
5. **工程治理**：完备的数据库驱动认证、滑动窗口限流、Redis 幂等防重、MySQL Durable Task 状态机账本、Flyway 数据面租户隔离。
6. **实测基线**：在 150 条开发评测集（`rag-eval-dev-v2`）上，Recall@5 为 `47.44%`，Top1 命中率 `92.31%`，关键词命中率 `79.74%`，准确拒答率 `95.0%`，检索 P50 延迟 `2219ms`。
7. **已知风险**：Milvus 字符串主键与 MySQL 自增 Long 主键存在双体系映射；C18 评测审计因后续 C21 代码修改出现 `budget_source_drift` 离线校验失败；客观词法 Claim 支持率仅 `5.22%`；Live Judge 未完成线上校准（处于 SKIPPED）。
8. **代码体量**：Java 59,294 行（主工程 32,526 行 + 测试 26,768 行），Python 评测 16,331 行，前端 9,799 行，总计约 8.5 万行；27 个 REST 端点与 1 个 MCP 端点。

---

## 1. 项目概况与功能矩阵

### 1.1 一句话定位与目标用户
- **定位**：企业证据型知识运行时原型（Enterprise Evidence & Knowledge Runtime 原型），通过混合检索、引用校验与防幻觉拒答提供高可信问答服务。
- **目标用户**：企业内部需精确查询研发规范、系统架构与技术文档的工程师；以及需要以安全、带权限形式调用知识库的业务系统。

### 1.2 核心功能矩阵与完成度分类

| 功能分类 | 功能名称 | 实现状态 | 代码/配置位置 | 事实说明 |
|---|---|---|---|---|
| **认证与租户** | 数据库认证与 JWT 签发 | 已完整实现 | `rag-auth/.../UserDetailsServiceImpl.java`<br>`rag-auth/.../JwtTokenProvider.java` | 区分 Access Token (1h) 与 Refresh Token (7d)，签发与刷新均绑定数据库租户事实 |
| | 多租户数据面隔离 | 已完整实现 | `rag-admin/.../db/migration/V10~V11.sql`<br>`rag-core/.../VectorStoreConfig.java` | 数据表强制 `tenant_id`；Milvus 适配器做租户过滤；Qdrant/ES 在隔离模式下启动阻断 |
| **文档处理** | 多格式文件解析与白名单 | 已完整实现 | `rag-document/.../DocumentParserFactory.java`<br>`rag-admin/.../DocumentIndexingServiceImpl.java` | 支持 TXT、MD、PDF、DOCX、代码文件，限制 50MB 上传，白名单过滤 |
| | 保守 Token 估算与分块 | 已完整实现 | `rag-document/.../DocumentChunker.java`<br>`rag-document/.../ConservativeTokenChunker.java` | 运行时加载 `420` 块大小、`80` 重叠大小，支持 Markdown 标题元数据继承 |
| | Durable 异步索引状态机 | 已完整实现 | `rag-admin/.../task/IndexTaskLedger.java`<br>`rag-admin/.../task/IndexTaskCoordinator.java` | 基于 MySQL 的分布式租约、心跳续期、指数退避重试与幂等 finalize |
| **检索与重排** | Dense 向量检索 | 已完整实现 | `rag-core/.../milvus/MilvusVectorStore.java` | Milvus 2.3.4，IVF_FLAT 索引，COSINE 距离度量 |
| | BM25 关键词检索与启动重建 | 已完整实现 | `rag-core/.../keyword/InMemoryBm25KeywordIndex.java`<br>`rag-admin/.../KeywordIndexBootstrap.java` | 内存化 BM25 索引，启动时从 `document_chunk` 表自动重建 |
| | RRF 双路检索融合 | 已完整实现 | `rag-core/.../query/QueryEngineImpl.java#fuseByRrf` | 倒数排名融合算法，参数 `rrf-k=60`，关键词 TopK 放大 2 倍召回 |
| | Reranker 抽象与 NVIDIA 适配 | 已完整实现 | `rag-core/.../rerank/HeuristicReranker.java`<br>`rag-core/.../rerank/NvidiaReranker.java` | 默认 Heuristic；支持 NVIDIA `/v1/ranking` 模型重排及自动降级兜底 |
| **问答与生成** | 上下文去重与 Token 预算裁剪 | 已完整实现 | `rag-core/.../prompt/PromptBuilder.java` | 默认上下文预算 1200 Token，超额自动按优先级丢弃后置块 |
| | LLM 同步与流式调用 | 已完整实现 | `rag-core/.../generator/AnswerGeneratorImpl.java` | 支持 OpenAI 兼容协议与通义千问协议，支持 Reactor Flux 流式输出 |
| | 引用溯源提取与重叠度校验 | 已完整实现 | `rag-core/.../citation/CitationValidator.java` | 校验来源元数据，要求片段精确匹配或 Token 重叠度 `>= 0.58` |
| | 显式拒答（No-Answer）机制 | 已完整实现 | `rag-core/.../prompt/PromptBuilder.java`<br>`rag-core/.../citation/CitationValidator.java` | 知识不足时强制输出固定拒答语，拒答时严格清空所有引用 |
| | SSE 终端结构化事件契约 | 已完整实现 | `rag-admin/.../QAController.java#askStream`<br>`rag-core/.../model/StreamTerminalEvent.java` | 流式结束推送 `terminal` 事件，携带终态（ANSWER/NO_ANSWER）、Citations 与 Metadata |
| **扩展与控制** | 只读 MCP Server 适配器 | 已完整实现 | `rag-admin/.../mcp/McpServletConfig.java`<br>`rag-admin/.../mcp/McpProtocolHandler.java` | 实现 MCP 协议，暴露 4 个只读 Tool 与 3 个 Resource，默认关闭（local-only） |
| | 有界意图路由器（Router） | 部分实现 | `rag-core/.../router/BoundedQueryRouter.java`<br>`rag-core/.../router/FactQueryStrategyExecutor.java` | 实现了单轮事实型查询 `fact-v1`；multi-hop、compare、temporal 等策略仅占位未实现 |
| | 离线质量门禁 Evaluator | 已完整实现 | `scripts/evaluate_quality_gate.py`<br>`docs/eval/schema/rag-quality-gate-profile-v1.json` | 具备 12 项指标的 hard floor 与 tolerance 组合门禁校验能力 |
| | LLM 自动化 Judge 评测 | 仅占位与离线工具就绪 | `scripts/rag_judge_contract.py`<br>`scripts/run_judge_calibration.py` | 静态 24-case 校准合约与严格 JSON 解析器就绪；线上调用为 `SKIPPED`，无真实打分数据 |
| | Agentic RAG / 工具循环 / 智能体 | **无** | 长期禁区与 W4 规划 | 当前代码库无自主 Agent 循环，无模型主动 Function Calling |

### 1.3 OpenSpec 完整演进历程、当前阶段与后续规划

根据项目总规划书（`docs/roadmap/iteration-blueprint.md`）与 OpenSpec 变更归档（`openspec/changes/archive/`），系统严格遵循规范驱动开发（Spec-Driven Development）原则，完整迭代演进历程如下：

#### 1. 基础工程与可靠性治理阶段（C1 ~ C7，2026-07）
- **C1** (`repository-governance-bootstrap`)：建立 OpenSpec 治理规范、Agent 协作边界与质量门禁契约。
- **C2** (`database-backed-authentication`)：从静态内存认证迁移至数据库支撑的 JWT 认证，集成生产秘钥安全守卫。
- **C3** (`real-integration-pipeline`)：引入 Testcontainers 容器化测试流水线，打通 MySQL/Redis/Milvus 真实集成验证。
- **C4** (`dependency-failure-semantics`)：定义并实现 LLM、Redis、Milvus 在离线/超时时的显式故障语义与平滑降级（如向量库异常降级为 BM25 纯词法检索）。
- **C5/C5b** (`durable-indexing-and-recovery`)：实现基于 MySQL 的 Durable Index Task Ledger 状态机、租约心跳续期与崩溃恢复机制，采用确定性 SHA-256 算法生成幂等分块哈希。
- **C6/C7** (`nvidia-reranker-adapter-ab`)：抽象 Reranker 接口，接入 NVIDIA `/v1/ranking` 模型端点，并在 30 题开发集上完成 clean A/B 对比评测（NVIDIA vs Heuristic）。

#### 2. 评测工程化与多租户隔离阶段（C8 ~ C16，2026-07 ~ 2026-08）
- **C8/C9/C9b** (`eval-dataset-v2-and-metrics`)：将评测集由 30 题扩充至 150 题（`rag-eval-dev-v2`），制定样本 Schema 强校验规范，引入严格词法 Claim 支撑率与 Judge 校准合约。
- **C10** (`eval-quality-threshold-gates`)：实现 `evaluate_quality_gate.py` 离线质量门禁评估器，确立 12 项指标的 hard floor 与 regression tolerance 组合门禁契约。
- **C11/C12** (`genai-tracing-and-otel`)：装配 OpenTelemetry 1.31 SDK，实现 RAG 检索-生成全链路分布式追踪（Tracing Core 与 Prometheus 指标导出）。
- **C13/C14** (`tenant-data-plane-enforcement`)：Flyway 迁移强制全表 `tenant_id`，Milvus 检索追加标量表达式下推隔离；构建包含 26 个用例的跨租户对抗评测矩阵。
- **C15** (`mcp-readonly-service`)：基于 Model Context Protocol SDK 2.0 实现只读 MCP Server 端点（`/mcp`），向外部宿主暴露 4 个只读 Tool 与 3 个 Resource。
- **C16** (`bounded-query-router`)：引入有界意图路由器，落地单轮闭域事实检索策略 `fact-v1`，确立防越权与防循环调用原则。

#### 3. 真实质量基线与流式终端契约阶段（C17 ~ C21，2026-09）
- **C17** (`retrieval-quality-gate-activation`，2026-09-10 验收归档)：在 150 条开发集上完成 3 次固定身份实测，正式激活首个检索质量门禁 Profile，12/12 规则通过。
- **C18** (`generation-objective-evidence-baseline`，2026-09-22 验收归档)：完成首个端到端客观生成基线评测，真实记录 Recall@5 (47.44%)、关键词命中率 (79.74%)、引用准确率 (91.91%)、拒答率 (95.0%) 与客观 Claim 覆盖率 (5.22%)。
- **C21** (`structured-sse-terminal-contract`，2026-09-29 验收归档)：确立前后端 SSE 流式终端契约（`structured-v1`），在流结束时以 `event: terminal` 确定性推送终态枚举、结构化引用与错误原因。

#### 4. 前端交付与体验对齐阶段（F1 ~ F5，2026-09 ~ 2026-10）
- **F1 / F2**：实现文本流式与结构化流式终端事件消费渲染。
- **F3** (`frontend-chat-source-alignment`)：对齐侧边栏 Citation 来源定位与片段折叠交互。
- **F4** (`frontend-kb-upload-alignment`，2026-10-02 验收归档)：对齐知识库管理面板、文档多格式上传与异步进度轮询展示。
- **F5** (`frontend-history-feedback-alignment`，**当前 ACTIVE 阶段**)：对齐历史会话记录、用户点赞/点踩反馈与检索高亮。

#### 5. 后续演进路线图（Wave 2 ~ Wave 6 规划）
- **近期（2026 Q4 / W1 收尾与 W2）**：
  - **C19**：推动 Live LLM Judge 真实调用校准，完善 Faithfulness / Relevance 线上评分。
  - **C20**：激活生成质量门禁（Objective Generation Quality Gate）。
  - **W2（有界策略扩展）**：在 Router 中逐步推进 multi-hop 多跳推理、compare 对比检索、temporal 时序检索等复合策略。
- **中期（2027 H1 / W3 ~ W4）**：
  - **W3（权限感知知识源联邦）**：接入更多异构数据源（Confluence, Notion, GitLab），实现多源统一权限下推。
  - **W4（可恢复深度研究）**：实现长耗时、支持暂停/恢复与人工干预（Human-in-the-loop）的深度报告生成。
- **远期（2027 H2 / W5 ~ W6）**：
  - **W5（受控行动与 Agent 互操作）**：引入 Agent-to-Agent (A2A) 协议与带审查的工具执行。
  - **W6（企业级生产化）**：全链路金丝雀发布、多机房高可用与大规模租户 SLA 验证。

---

## 2. 技术栈与实际版本配置

### 2.1 依赖与中间件版本（pom.xml / docker-compose.yml）
- **核心框架**：Java 17, Spring Boot `3.2.1`（Maven 多模块：`rag-common`, `rag-auth`, `rag-document`, `rag-core`, `rag-admin`）。
- **持久化层**：MySQL `8.0`（InnoDB, `mysql-connector-j`, HikariCP）, MyBatis-Plus `3.5.5`, Flyway `9.22.3`。
- **缓存与状态**：Redis `7-alpine`（Spring Data Redis, Lettuce 连接池）。
- **向量数据库与对象存储**：
  - Milvus `2.3.4`（`milvus-sdk-java:2.3.4`，搭配 etcd `v3.5.5` 与 MinIO `RELEASE.2023-03-20T20-16-18Z`）。
  - Qdrant Client `1.7.0`、Elasticsearch Java Client `8.11.3`（代码中存在适配器，但隔离模式下阻断）。
- **安全与工具**：
  - JJWT `0.12.3`（`jjwt-api`, `jjwt-impl`, `jjwt-jackson`）。
  - Model Context Protocol SDK `2.0.0`（`mcp-core`, `mcp-json-jackson2`）。
  - OpenTelemetry SDK `1.31.0`（`opentelemetry-sdk`, `opentelemetry-exporter-otlp`）。
  - Springdoc OpenAPI UI `2.3.0`。
- **前端栈**：Vue `3.4+`, TypeScript `5+`, Vite `5+`, Element Plus `2.5+`, Pinia, Axios。

### 2.2 运行环境与模型实际配置（rag-admin/.../application.yml）
- **LLM 生成配置**：
  - `rag.llm.provider`: `openai`
  - `rag.llm.openai.base-url`: `${NVIDIA_BASE_URL:https://integrate.api.nvidia.com/v1}`
  - `rag.llm.openai.model`: `${NVIDIA_CHAT_MODEL:qwen/qwen3.5-122b-a10b}`
  - `rag.llm.openai.temperature`: `0.2`
  - `rag.llm.openai.max-tokens`: `2048`
  - `rag.llm.timeout`: `120` 秒
  - `rag.llm.max-retries`: `0`（代码底层针对 429/5xx 支持 backoff 800ms 重试）
- **Embedding 配置**：
  - `rag.embedding.default-provider`: `openai`
  - `rag.embedding.openai.base-url`: `https://integrate.api.nvidia.com/v1`
  - `rag.embedding.openai.model`: `${NVIDIA_EMBED_MODEL:nvidia/nemotron-3-embed-1b}`
  - `rag.embedding.openai.dimension`: `2048`
  - `rag.embedding.openai.timeout-ms`: `60000`
- **向量库 Milvus 配置**：
  - `rag.vectorstore.type`: `milvus`
  - `index-type`: `IVF_FLAT`
  - `metric-type`: `COSINE`
  - `nlist`: `1024`
  - `nprobe`: `16`
- **检索与重排配置**：
  - `retrieval.hybrid.enabled`: `true`
  - `retrieval.hybrid.rrf-k`: `60`
  - `retrieval.hybrid.keyword-top-k-multiplier`: `2`
  - `retrieval.rerank.provider`: `heuristic`（默认启用启发式重排）
  - `retrieval.rerank.top-n`: `20`
  - `retrieval.rerank.top-k`: `5`
  - `retrieval.rerank.nvidia.enabled`: `false`（可选开启，模型专属端点 `/v1/ranking`，超时 3000ms）
- **分块配置**：
  - `document.chunking.chunk-size`: `420`
  - `document.chunking.chunk-overlap`: `80`
  - `document.chunking.strategy`: `semantic`

---

## 3. 架构设计与核心调用链路

### 3.1 模块分层与工程职责

```
                ┌──────────────────────────────────────────────┐
                │        rag-frontend (Vue 3 + TS + Vite)      │
                └──────────────────────┬───────────────────────┘
                                       │ HTTP / REST / SSE
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ rag-admin                                                                   │
│ - QAController, KnowledgeBaseController, TaskController, HistoryController   │
│ - McpServletConfig / McpProtocolHandler (/mcp)                              │
│ - DocumentIndexingServiceImpl (编排), IndexTaskCoordinator (状态恢复)        │
│ - GenAiTracingConfiguration (OTel SDK 装配)                                 │
└──────┬───────────────────────┬───────────────────────────────┬──────────────┘
       │                       │                               │
       ▼                       ▼                               ▼
┌──────────────┐       ┌──────────────┐                ┌──────────────┐
│ rag-auth     │       │ rag-document │                │ rag-core     │
│ - Security   │       │ - Parsers    │                │ - QueryEngine│
│ - JWT / Black│       │ - Chunker    │                │ - VectorStore│
│ - UserDetails│       │ - TokenCount │                │ - Generator  │
└──────┬───────┘       └──────┬───────┘                └──────┬───────┘
       │                      │                               │
       └──────────────────────┴───────────────┬───────────────┘
                                              ▼
                               ┌──────────────────────────────┐
                               │ rag-common                   │
                               │ - RedisAsyncTaskManager      │
                               │ - SlidingWindowRateLimiter   │
                               │ - IdempotencyAspect          │
                               │ - GenAiTelemetry Facade      │
                               └──────────────────────────────┘
```

### 3.2 核心调用链路（文字时序描述）

#### 链路 1：同步问答链路（提问 → 检索 → 生成 → 返回）
1. **请求接入与身份判定**：客户端调用 `POST /api/qa/ask`。`QAController.ask` 拦截请求，经 `@RateLimit`（30次/60s）检查，从 SecurityContext 提取不可篡改的 `RequestIdentity(userId, tenantId)`，通过 `AuthorizationService` 鉴权知识库权限并获取当前可用的 `TenantVectorScope`。
2. **检索阶段（QueryEngineImpl）**：
   - 生成 Query 变体（口语化剥离、英文缩写展开，最多 8 个变体）。
   - Dense 向量检索：调用 `EmbeddingService` 批量向量化变体，进入 `MilvusVectorStore.search`，传入 COSINE 度量和租户表达式 `tenant_id == X and kb_id == Y`，召回 Top 5 候选（过滤 `minScore < 0.3`）。
   - BM25 关键词检索：`InMemoryBm25KeywordIndex.search` 按 `keyword-top-k-multiplier=2` 召回 Top 10 候选。
   - RRF 排序融合：`fuseByRrf` 按 `1 / (60 + rank + 1)` 将两路候选合并并降序重排。
   - 重排器执行：`RerankerRegistry` 提取前 20（`top-n`）个候选，执行 `HeuristicReranker`（或 NVIDIA ranking），输出最终 Top 5（`top-k`）上下文。
3. **生成阶段（AnswerGeneratorImpl）**：
   - Prompt 构建：`PromptBuilder` 进行上下文去重，并按照 `1200` Token 预算裁剪上下文，渲染 `STRUCTURED_TEMPLATE`（包含严格防幻觉与中英文一字不差拒答指令）。
   - LLM 调用：WebClient 发送 POST 请求至 OpenAI 兼容接口，设置超时 120s；遇到 429/5xx 触发指数退避重试。
4. **后置校验与响应（CitationValidator）**：
   - 答案文本清理；
   - 引用验证：`CitationValidator.validate` 校验生成的引用，核验来源文档 ID，并要求引用片段与检索上下文的 Token 重叠度 `>= 0.58`，剔除悬空引用；若判定为拒答文本则强行清空引用。
   - 持久化历史：异步写入 `qa_history` 表；调用 `enrichResponseSources` 补全文档标题后返回响应 DTO。

#### 链路 2：文档索引入库链路（上传 → 异步解析 → 向量化与关键词入库）
1. **上传接入**：调用 `POST /api/kb/{kbId}/documents`。`DocumentIndexingServiceImpl.submitIndexing` 校验文件名与白名单类型，将原始二进制流写入 `IndexInputStore`，在 MySQL `document` 表生成 `PENDING` 记录。
2. **Ledger 任务登记**：在 MySQL `async_task` 表创建 durable ledger 记录，状态置为 `ACCEPTED`，分配唯一 `taskId`，同步在 Redis 建立 24 小时状态投影缓存。
3. **异步调度执行**：
   - 解析与分块：打开已校验文件流，`DocumentProcessor` 调用 Tika/Markdown 解析器，`DocumentChunker` 按照 `chunk-size=420`、`chunk-overlap=80` 进行分块。
   - 内容去重检查：计算文档 SHA-256 `contentHash`，若库内已有同内容就绪文档，直接复用已有索引并完成。
   - 稳定 Chunk ID 生成：`DeterministicChunkIdentity.remap` 基于 `c5b-v1:documentId:contentHash:index:contentDigest` 生成 64 位十六进制 SHA-256 字符串，作为 Chunk 的全局稳定身份。
4. **向量生成与写入**：
   - 状态标记：Ledger 标记 `SAFE_PRE_VECTOR`。
   - 嵌入计算：`EmbeddingService.embedBatch` 批量计算 2048 维向量。
   - 写入 Milvus：Ledger 标记 `VECTOR_IN_FLIGHT`；`MilvusVectorStore.upsert` 写入向量，成功后 Ledger 立即打点 `VECTOR_CONFIRMED`。
5. **事务化 Finalize**：
   - 写入 BM25 关键词索引：更新 `InMemoryBm25KeywordIndex`。
   - SQL Finalize：`IndexTaskSqlFinalizer` 在单个数据库事务中，行级锁锁定 Document 记录，批量插入 `document_chunk` 表，递增知识库文档总数，将文档状态置为 `COMPLETED`，更新 Ledger 状态为 `COMPLETED`。

#### 链路 3：SSE 终端流式问答链路
1. **客户端接入**：客户端调用 `POST /api/qa/ask/stream`（请求体携带 JSON `AskRequest(kbId, question, topK...)`），通过请求头声明流式契约版本 `X-RAG-Stream-Contract: structured-v1`。
2. **连接初始化**：`QAController.askStream` 完成鉴权与向量范围检查，创建并返回 120 秒超时的 `SseEmitter`；绑定 `onCompletion`、`onTimeout` 与 `onError` 处理生命周期与连接关闭标记。
3. **后台检索与 Flux 订阅**：后台线程执行双路检索与重排，获取上下文；`ragService.askStream` 启动 WebClient 的 Reactor `Flux<String>` 数据流，并通过 Reactor Context 注入 `StreamTerminalSignal`。
4. **增量文本下发**：模型产生 token 增量时，服务端下发 `event: text`（内容为原始增量纯文本 chunk）；控制器在内存缓存中累加生成内容，流式诊断器记录 chunk 耗时与空隙。
5. **终态收敛与终端事件（Terminal Event）**：
   - 模型正常结束时，执行 `AnswerGeneratorImpl.finalizeStream` 完成全量文本的 Citation 校验；服务端向客户端发送终端事件 `event: terminal`，携带符合 `structured-v1` 的 JSON 负载（含终态枚举 `ANSWER`、校验后的 `citations` 列表与生成元数据）；
   - 若流式中途出错或超时，则发送带 `finalState: ERROR` 及错误原因的 `terminal` 事件；
   - 只有当终态为完整的 `ANSWER` 且连接未异常中断时，服务端才异步调用 `saveStreamHistory` 将问题、答案、有效引用及耗时持久化进 `qa_history` 表。最后由服务端调用 `emitter.complete()` 优雅关闭连接。

### 3.3 数据库表与向量集合结构

#### MySQL 核心数据表（Flyway V1 ~ V13）
- `tenant`：租户表（`id`, `code`, `name`, `enabled`）。
- `user`：用户表（`id`, `tenant_id`, `username`, `password_hash`, `email`, `enabled`）。
- `knowledge_base`：知识库表（`id`, `tenant_id`, `name`, `owner_id`, `vector_collection`, `vector_readiness`, `vector_generation`, `document_count`）。
- `document`：文档表（`id`, `tenant_id`, `kb_id`, `uploader_id`, `title`, `file_path`, `file_type`, `content_hash`, `status`, `chunk_count`）。
- `document_chunk`：分块表（`id` [自增PK], `tenant_id`, `document_id`, `vector_id` [VARCHAR(100)], `content`, `chunk_index`, `start_pos`, `end_pos`, `metadata` [JSON]）。
- `async_task`：异步任务与 Ledger 状态机（`id`, `tenant_id`, `task_id`, `document_id`, `status`, `execution_phase`, `attempt_count`, `lease_owner`, `lease_until`, `heartbeat_at`, `prepared_content_hash`）。
- `qa_history`：问答历史表（`id`, `tenant_id`, `user_id`, `kb_id`, `question`, `answer`, `citations` [JSON], `trace_id`, `latency_ms`）。
- `qa_feedback`：问答反馈表（`id`, `tenant_id`, `qa_id`, `user_id`, `rating`, `comment`）。
- `kb_permission`：知识库权限表（`id`, `tenant_id`, `kb_id`, `user_id`, `permission_type`）。

#### Milvus 向量集合 Schema（MilvusVectorStore.java L140-179）
- 集合命名规则：`kb_{kbId}` 或影子集合 `kb_{hash}`。
- 字段构成：
  - `id` (VarChar 256, PrimaryKey = true)：存放 Chunk 唯一哈希。
  - `vector` (FloatVector, Dimension = 2048)：密集向量数据。
  - `content` (VarChar 65535)：分块原文正文。
  - `metadata` (VarChar 65535)：序列化的 JSON 元数据字符串。
  - `tenant_id` (Int64)：服务端强制填入的租户主键（用于标量过滤）。
  - `kb_id` (Int64)：知识库 ID（用于标量过滤）。

---

## 4. AI 相关核心实现详情

### 4.1 Prompt 模板与组装逻辑
- **代码位置**：`rag-core/src/main/java/com/enterprise/rag/core/rag/prompt/PromptBuilder.java`。
- **默认模板（STRUCTURED_TEMPLATE）**：
  ```text
  You are a helpful assistant that answers questions based on the provided context.

  ## Instructions
  - Answer the question based ONLY on the provided context
  - If the context doesn't contain enough information, reply exactly "知识库中没有足够信息回答该问题。" for a Chinese question; for other languages, use the equivalent sentence in the user's language
  - When refusing because context is insufficient, do not include citations
  - Do not add information from external knowledge
  - Do not include raw source headers, chunk numbers, relevance scores, or any internal source markers in the final answer
  - Citations are handled separately by the system, so keep the answer body clean and user-facing
  - Use complete and fluent sentences; avoid broken words or truncated phrases
  - Preserve original terms, numbers, and definitions from the context whenever possible
  - Do not invent facts or mix in outside knowledge
  - Be concise and accurate
  - You MUST reply in the SAME language as the user's question (e.g. Chinese question → Chinese answer)
  ...
  ## Context
  %s

  ## Question
  %s

  ## Answer
  ```
- **组装与优化算法（PromptBuilder.buildOptimized）**：
  1. 上下文去重（L216-227）：`deduplicateContexts` 计算 `source::content` 摘要，过滤重复 chunk。
  2. 预算动态裁剪（L229-246）：设定 Token 预算（默认 `DEFAULT_CONTEXT_TOKEN_BUDGET = 1200`），`applyTokenBudget` 按照 CJK 单字符 / 西文单词粗略估算 Token 消耗，一旦加入当前 chunk 导致超出 1200 Token，立即截断舍弃后续块。
  3. 上下文拼接格式化（L187-198）：每条上下文输出为 `Context {i}\nSource: {sourceHint}\nContent:\n{content}`。

### 4.2 检索、切分与校验具体参数

| 环节 | 关键参数项 | 代码定义位置 | 实际运行值 | 逻辑说明 |
|---|---|---|---|---|
| **分块** | `chunkSize` | `application.yml`<br>`DocumentChunkingProperties.java` | **420** | 基于 TokenCounter 的估算预算值 |
| | `chunkOverlap` | `application.yml`<br>`DocumentChunkingProperties.java` | **80** | 重叠 Token 预算，保护跨边界上下文 |
| | `strategy` | `ChunkStrategy.java` | **SEMANTIC** | 段落（`\n\s*\n`）优先切分，长段落递归句子切分 |
| **Embedding** | 模型与维度 | `application.yml` | `nvidia/nemotron-3-embed-1b` / **2048** 维 | 基于 OpenAI 兼容客户端调用 |
| **检索** | Query 变体上限 | `QARequest.java` | **8** | 口语化剥离与同义词扩展 |
| | Dense 召回 topK | `QARequest.java` | **5** | Milvus 单路检索候选数 |
| | Dense 最低阈值 | `QARequest.java` | **0.3** | 过滤余弦相似度低于 0.3 的噪音片段 |
| | BM25 topK 乘数 | `application.yml` | **2** | 关键词检索取 `topK * 2 = 10` 个候选 |
| | RRF 常数 k | `application.yml`<br>`QueryEngineImpl.java` | **60** | 倒数排名融合平滑因子 |
| **重排** | 候选输入 topN | `application.yml` | **20** | 送入 Reranker 的最大候选数 |
| | 最终输出 topK | `application.yml` | **5** | 重排后保留给 Prompt 的最大上下文数 |
| | 默认 Provider | `application.yml` | **heuristic** | 启发式规则（命中关键词词频与位置打分） |
| **引用校验** | 词法覆盖率阈值 | `CitationValidator.java` L21 | **0.58** (`MIN_TOKEN_OVERLAP`) | 引用文本片段与检索上下文的 Token 交集比例 |
| | 拒答引用清空 | `CitationValidator.java` L36 | **true** | 拒答时强制将 citations 列表置空 |

### 4.3 流式、降级、重试与超时
- **流式输出**：`AnswerGeneratorImpl.callLLMStream` 返回 Reactor `Flux<String>`，控制器使用 Spring `SseEmitter` 包装。
- **故障降级（Fallback）**：
  - 向量服务离线降级：`QueryEngineImpl` 捕获 `VectorDependencyException`；若关键词索引（BM25）有结果，系统平滑降级为纯关键词召回，在元数据中标记 `keywordOnly`；若关键词也无结果，拒绝假成功并向用户抛出明确异常。
  - 重排器降级：当配置了 `nvidia` 但网络异常或未开通时，`RerankerRegistry` 自动整次回退到 `HeuristicReranker`，记录 `rerankFallbackCount` 并在日志报警。
  - 引用兜底机制：若 LLM 未显式输出 `[Source X]` 标记，`AnswerGeneratorImpl.resolveCitations` 会基于句子重叠度自动提取最多 3 条相关上下文作为兜底引用。
- **重试策略**：`AnswerGeneratorImpl.buildRetrySpec` 配置 `Retry.backoff(maxRetries, Duration.ofMillis(800))`。默认 `maxRetries=0`；在启用重试时，仅对 HTTP 429、5xx、TimeoutException、IOException 实施重试。流式模式下，一旦首字符已发往客户端（`visibleContentEmitted=true`），立即禁止重试，避免向客户端输出重复污染文本。
- **超时控制**：LLM HTTP 连接超时 120 秒，Embedding 请求超时 60 秒，MCP 问答超时 120 秒。

### 4.4 Agent 与工具调用判定
- **是否存在 Agent / 工具调用 / 多步流程？**
  - **明确结论：无**。
  - **证据**：
    1. 没有 LangChain / LlamaIndex / AutoGen 等 Agent 框架依赖；
    2. LLM 生成调用均为单次同步或单次流式请求，未配置 OpenAI Tools / Functions 参数，模型无法自主发起 Tool Call；
    3. C15 的 MCP 模块是本系统作为 **MCP Server** 被 Cursor、Claude Desktop 等外部宿主只读调用，本系统内部并不作为 Client 循环调用其他外部工具；
    4. C16 路由器目前仅支持单步单事实路由 `fact-v1`，multi-hop 多跳推理处于规划阶段（无代码循环）。

---

## 5. 非 AI 工程实现细节

1. **认证与安全（rag-auth）**：
   - 入口：`rag-auth/.../config/SecurityConfig.java`、`rag-auth/.../jwt/JwtTokenProvider.java`。
   - 采用 Spring Security 无状态过滤器链；JWT 包含 `userId` 与 `tenantId` Claims；支持开发态秘钥保护守卫（生产必须 `>= 32` 字节）；支持基于 Redis 的登出黑名单机制（`TokenBlacklistServiceImpl.java`）。
2. **限流保护（rag-common）**：
   - 入口：`rag-common/.../ratelimit/SlidingWindowRateLimiter.java`、`RateLimitInterceptor.java`。
   - 基于 Redis ZSET 时间戳实现精确滑动窗口；通过 `@RateLimit` 注解拦截，问答接口配置为 `30` 次 / `60` 秒 / 用户维度。
3. **接口幂等（rag-common）**：
   - 入口：`rag-common/.../idempotency/IdempotencyAspect.java`、`RedisIdempotencyHandler.java`。
   - `@Idempotent` 切面结合 Redis `SET NX EX` 分布式锁，防止文档重复并发上传与问答重复提交。
4. **异步任务与 Durable Ledger（rag-common & rag-admin）**：
   - 入口：`rag-common/.../async/RedisAsyncTaskManager.java`、`rag-admin/.../task/impl/IndexTaskLedgerImpl.java`。
   - 索引任务双写：Redis 充当 24h 读投影，MySQL `async_task` 充当 Durable Ledger，记录 `lease_until`、`heartbeat_at`、`attempt_count`（最大3次），由 `IndexTaskCoordinator` 定时扫描崩溃未完成任务并恢复。
5. **数据面权限与隔离（rag-admin）**：
   - 入口：`rag-admin/.../kb/service/impl/KBPermissionServiceImpl.java`、`AuthorizationServiceImpl.java`。
   - 细粒度 RBAC 校验当前用户对知识库的读/写/管理权限；数据面所有查询严格拼接 `tenant_id` 条件。

---

## 6. 测试与评测事实

### 6.1 现有测试类型与覆盖内容
- **Java 单元与 Mock 测试**：覆盖各层业务逻辑（Controller MVC 模拟、Service 业务流、Prompt 构建、分词切分器等）。
- **Property-Based Testing（jqwik）**：
  - `VectorStorePropertyTest.java`：通过属性化随机向量与元数据验证向量库 CRUD 的代数一致性。
  - `KnowledgeBasePropertyTest.java`、`RateLimiterPropertyTest.java`、`AsyncTaskManagerPropertyTest.java`：随机并发参数下验证状态机与限流计数器。
- **Testcontainers 容器化真实集成测试（c3-integration Profile）**：
  - `FullFlowIT.java`：在真实隔离 MySQL、Redis、etcd、MinIO、Milvus 容器中，验证完整“注册→创建知识库→上传MD文档→异步索引完成→向量检索→删除”端到端主链路。
  - `C14 TenantIsolationAdversarialIntegrationTest.java`：双租户对抗矩阵测试，验证跨租户越权查询阻断。
- **Python 评测工具集**：
  - 19 个独立测试文件（`test_*.py`），验证评测集 schema 校验、离线门禁 evaluator 逻辑、NVIDIA A/B 对比算法等。

### 6.2 评测集规模与指标定义
- **评测集版本**：`rag-eval-dev-v2`（位于 `docs/eval/releases/rag-eval-dev-v2.jsonl`）。
- **规模与分类**：共 **150 条样本**（30 条 v1 种子 + 120 条新增扩展）。
  - 按类别：`fact` (39 条), `definition` (23 条), `reasoning` (44 条), `multi_hop` (44 条)。
  - 按难度：`easy` (39 条), `medium` (68 条), `hard` (43 条)。
  - 按可答性：`answerable` (**130 条**，期望有答案并带准确引用), `no_answer` (**20 条**，期望模型准确拒答)。
- **指标定义与计算公式**：
  - `Recall@5`：$\frac{\text{检索出的 Top5 片段中包含的预期 Context 数}}{\text{全部预期 Context 总数 (150题共293个)}}$。
  - `MRR`（Mean Reciprocal Rank）：$\frac{1}{130} \sum \frac{1}{\text{首个命中预期 Context 的排名}}$。
  - `Top1 Source Accuracy`：首个召回块所在源文档与预期源文档一致的题目比例。
  - `Answer Keyword Hit Rate`：回答中包含的标注关键词命中数 / 标注关键词总数（469个）。
  - `Citation Source Hit Rate`：回答给出的 Citation 来源文档命中预期文档的比例。
  - `Objective Claim Support Rate`：将回答按句子拆为独立 Claim，要求由验证通过的 Citation Snippet 提供 Exact 或 Token 重叠度 `>= 0.70` 的词法支撑；支持的 Claim 数 / 总 Claim 数。
  - `No-Answer Accuracy`：20 个拒答题目中，成功输出拒答语且不含虚假引用的题目比例。

### 6.3 最近一次实测基线数据（2026-09-22 C18 基线报告）
数据来源：`docs/eval/reports/c18-generation-objective-review-v1.json`（Git HEAD `bbab9280`，测试运行者实际测得）：

| 指标通道 | 指标名称 | 实测值 | 样本分母与说明 |
|---|---|---:|---|
| **检索指标** | Recall@3 | **44.37%** | 130/293 |
| | Recall@5 | **47.44%** | 139/293 |
| | MRR | **0.5262** | 130 条可答样本 |
| | Top1 Source Accuracy | **92.31%** | 120/130 |
| **生成指标** | Answer Keyword Hit Rate | **79.74%** | 374 / 469 个预期关键词 |
| | Answerable Ask Success Rate | **100.0%** | 130 / 130 全部回答完成 |
| **引用指标** | Citation Source Hit Rate | **91.91%** | 125 / 136 |
| | Citation Snippet Hit Rate | **100.0%** | 336 / 336（凡有引用皆有片段） |
| | Unsupported Citation Count | **0** | 零无依据悬空引用 |
| | No-Answer Citation Violation | **1** | 20 道拒答题中仅 1 次违规输出引用 |
| **客观 Claim** | Objective Claim Support Rate | **5.22%** | 23 / 441（严格词法字面覆盖，真实基线，未美化） |
| **拒答指标** | No-Answer Accuracy | **95.0%** | 19 / 20 道题目正确拒答 |
| **延迟表现** | Retrieval Latency P50 / P95 | **2219ms / 5797ms** | 包含 1.2s 检索节奏限制与网络耗时 |
| **稳定性事实** | 外部 HTTP 请求情况 | 181 次请求 | 遇到 31 次 NVIDIA 503 异常，全部自动重试恢复 |

---

## 7. 已知问题与技术风险

### 7.1 Milvus 主键映射问题（深度根因分析）
- **现象描述**：系统在持久化分块与检索溯源时，MySQL 的分块 ID 与 Milvus 内部的主键 ID 存在双轨割裂。
- **根因代码文件**：
  1. `rag-admin/src/main/resources/db/migration/V2__document_tables.sql`（L28-43）：MySQL `document_chunk` 表的主键 `id` 采用 `BIGINT NOT NULL AUTO_INCREMENT`，而 `vector_id` 列为 `VARCHAR(100)` 普通字段。
  2. `rag-core/src/main/java/com/enterprise/rag/core/vectorstore/milvus/MilvusVectorStore.java`（L85-90）：Milvus Collection 的 Primary Key 字段名定义为 `FIELD_ID = "id"`，数据类型强制为 `DataType.VarChar`（长度 256）。
  3. `rag-admin/src/main/java/com/enterprise/rag/admin/kb/service/impl/DocumentIndexingServiceImpl.java`（L530-545）：入库时，`DeterministicChunkIdentity` 生成的 SHA-256 字符串（`chunk.id()`）作为 `VectorDocument.id` 存入 Milvus 的主键字段；而在 MySQL 中，该哈希存入 `entityChunk.setVectorId(...)`，MySQL 的主键 `id` 由自增生成。
  4. `rag-core/src/main/java/com/enterprise/rag/core/rag/query/QueryEngineImpl.java`（L257-263）：检索返回时，Milvus 的 `idScore.getStrID()` 直接被作为 `RetrievedContext.source`。
  5. `rag-core/src/main/java/com/enterprise/rag/core/rag/citation/CitationValidator.java`（L152-154）：`chunkId(context)` 方法直接返回 `context.source()`（即 Milvus 的 String 哈希）。
- **风险与影响**：
  - 系统中存在两个“Chunk ID”：MySQL 数据库主键 `document_chunk.id`（数字 Long）与向量库主键 `vector_id`（64位哈希 String）。
  - 若外部接口或 MCP 客户端混淆了这两个 ID，通过 MySQL 主键查询将无法定位 Milvus 向量；删除文档时，`DocumentServiceImpl.java` 必须先从 MySQL `SELECT vector_id FROM document_chunk` 得到字符串列表，再调用 Milvus 删除。若 MySQL 记录丢失或 `vector_id` 为空，Milvus 孤儿向量将无法清理。

### 7.2 其他代码与工程风险
1. **C18 预算审计源文件哈希漂移（budget_source_drift）**：
   - 现场验证证据：执行 `python -B -m unittest discover -s scripts -p "test_*.py"` 时，`test_c18_generation_contract.py` 等 4 个测试报错 `c18_budget_contract.BudgetContractError: budget_source_drift`。
   - 根因：`scripts/c18_budget_contract.py` 在 C18 归档时锁定了相关源码的 SHA-256 哈希。随后 C21（`structured-sse-terminal-contract`）修改了 `QAController.java`、`AnswerGeneratorImpl.java` 和 `RAGServiceImpl.java`，导致离线防御校验按设计要求 Fail-closed 报警。需要更新 C18 审计记录或解除已归档版本的源码哈希强绑定。
2. **客观词法 Claim 覆盖率极低（5.22%）**：
   - 5.22% 的数值反映出基于字面词法（Jaccard / Token overlap）校验自然语言长答案的局限性；模型回答常使用同义词替换或抽象概括，词法校验无法建立语义等价，亟需未来引入 NLI（自然语言推理）模型或校准后的 LLM Judge。
3. **Live Judge 处于 SKIPPED 状态**：
   - C9b 仅完成了离线校准工具和 24 条静态 Gold 数据集；由于未获真实 API 调用授权，线上 Judge 的 Faithfulness / Relevance 评分未完成校准，不可作为发布门禁。
4. **Qdrant 与 Elasticsearch 多租户未闭环**：
   - `rag-core/src/main/java/com/enterprise/rag/core/vectorstore/config/VectorStoreConfig.java`（L142）：当 `rag.tenant.enforcement.enabled=true` 时，若配置为非 Milvus 向量库，应用启动直接抛异常退出。
5. **SSE 流式历史在 Legacy 模式下不持久化引用**：
   - `QAController.java` 中，当客户端未携带 `X-RAG-Stream-Contract: structured-v1` 请求头时，服务端退化为 legacy 格式下发纯文本流，无法在流式结束时携带结构化引用，历史表 `qa_history.citations` 保存为 null。只有在 `structured-v1` 契约且终态为 `ANSWER` 时，Citations 才会随流结束完成校验并持久化。

---

## 8. 可量化素材与最小成本测试指南

### 8.1 代码与接口量化数据
- **代码规模统计（真实物理行统计）**：
  - **Java 主工程**：304 个文件，**32,526** 行代码。
  - **Java 测试代码**：138 个文件，**26,768** 行代码（含 jqwik 随机测试与 IT 测试）。
  - **Python 评测脚本**：13 个工具脚本，**9,411** 行。
  - **Python 评测测试**：19 个测试文件，**6,920** 行。
  - **前端界面（Vue 3 / TS）**：74 个源文件，**9,799** 行。
  - **总代码规模**：约 **85,424** 行代码。
- **接口统计**：5 个 REST 控制器，共 **27 个端点**：
  - `AuthController`：3 个（`POST /api/auth/login`, `POST /api/auth/refresh`, `POST /api/auth/logout`）
  - `KnowledgeBaseController`：9 个（`POST /api/kb`, `GET /api/kb/{id}`, `GET /api/kb`, `PUT /api/kb/{id}`, `DELETE /api/kb/{id}`, `GET /api/kb/{id}/statistics`, `POST /api/kb/{id}/documents`, `GET /api/kb/{id}/documents`, `DELETE /api/kb/{kbId}/documents/{docId}`）
  - `QAController`：4 个（`POST /api/qa/ask`, `GET /api/qa/ask`, `POST /api/qa/ask/stream`, `POST /api/qa/debug/retrieve`）
  - `TaskController`：5 个（`GET /api/tasks/{taskId}`, `GET /api/tasks/{taskId}/result`, `POST /api/tasks/{taskId}/cancel`, `GET /api/tasks/{taskId}/exists`, `GET /api/tasks/{taskId}/completed`）
  - `HistoryController`：6 个（`GET /api/history`, `GET /api/history/{id}`, `DELETE /api/history/{id}`, `POST /api/history/{id}/feedback`, `GET /api/history/{id}/feedback`, `GET /api/history/feedback/my`）
  - 协议端点：1 个 MCP Streamable HTTP 协议端点（`POST /mcp`）。

### 8.2 最小成本（低费/零费）实测步骤建议

```text
[环境拉起] -> [预检验证] -> [离线门禁运行 (0费用)] -> [检索基线抽测 (极低费用)]
```

1. **第一步：基础组件拉起（本地无外部调用费用）**
   ```powershell
   docker compose -f docker-compose.yml up -d
   ```
   启动本地 MySQL 8.0、Redis 7、Milvus 2.3.4、etcd 与 MinIO，确保容器状态健康。
2. **第二步：后端服务启动与迁移**
   ```powershell
   .\start-backend.ps1
   ```
   检查控制台日志，确认 Flyway 顺利执行完成 V1 ~ V13 迁移，Tomcat 启动于 8080 端口。
3. **第三步：离线质量门禁重放（0 API 费用）**
   ```powershell
   python -B scripts/evaluate_quality_gate.py --profile docs/eval/schema/rag-quality-gate-profile-v1.json --report docs/eval/reports/c17-retrieval-reference-review-v1.json
   ```
   无需调用任何 LLM 或 Embedding，直接使用归档的 450 条脱敏检索快照，复现 12/12 规则 PASS。
4. **第四步：微量采样抽测检索链路（仅 5 次 Embedding 调用，费用 < 0.001 元）**
   ```powershell
   python -B scripts/run_rag_eval.py --base-url http://localhost:8080 --sample-limit 5 --skip-ask --allow-unversioned-eval-set
   ```
   只对 5 个样本执行向量化与检索（跳过 LLM 生成），实测验证 Milvus 连通性、BM25 重建与 RRF 融合执行，可测得真实的毫秒级检索延迟（P50/P95）。

---

## 9. 技术面试高频追问清单（15 题）

### Q1: 你们的混合检索（Hybrid Search）是怎么设计的？为什么选择 RRF 而不是线性加权？
- **答案要点**：
  1. 架构采用 Dense 向量（Milvus）+ 稀疏关键词（内存 BM25）双路召回。
  2. 向量检索侧重语义相似度，但在专有名词、错误码、方法名上容易漏召回；BM25 侧重字面精确命中，二者互补。
  3. 为什么用 RRF：向量相似度（余弦值 0~1）与 BM25 得分（0 到几十的无界值）量纲和分布完全不同，做线性加权（$\alpha \cdot S_{dense} + \beta \cdot S_{bm25}$）需要对分数做复杂的归一化，且超参数 $\alpha, \beta$ 极难维护；RRF 只关注相对排名（Rank），公式为 $1 / (k + rank)$，对打分漂移鲁棒。
- **代码位置**：`rag-core/src/main/java/com/enterprise/rag/core/rag/query/QueryEngineImpl.java`（方法 `fuseByRrf`，L339-377）。

### Q2: 文档切分为什么选 420/80？分块大小对召回有什么影响？
- **答案要点**：
  1. 420/80 是在 30 题开发集上对 `500/50`、`420/80`、`640/96`、`700/100` 等矩阵实测对比后的工程平衡点。
  2. 块太小（如 < 200）：语义破碎，上下文不足，且丢失标题层级；
  3. 块太大（如 > 800）：单块噪音多，Dense 向量表示被稀释，且挤占 LLM 上下文预算；
  4. 重叠 80 Token 是为了防止关键实体和句子边界被硬切断导致语义中断。
- **代码位置**：`rag-admin/src/main/resources/application.yml`（L236-237），`rag-document/.../chunker/DocumentChunker.java`。

### Q3: 为什么 Rerank 默认使用 Heuristic 启发式而不是模型重排？真实效果如何？
- **答案要点**：
  1. 系统设计了 `Reranker` 接口，默认 Heuristic（基于关键词在文本中的密度、跨距和首部位置加权），保证开箱即用、无外部依赖、零调用延迟。
  2. C7 阶段在 30 个固定样本上完成了 clean A/B 测试：NVIDIA 模型重排相比 Heuristic，Recall@5 提升 7.84pp（76.47% vs 68.63%），MRR 提升 0.0895；但模型重排引入了单次 363ms（P50）/ 688ms（P95）的额外耗时。
  3. 为了系统稳定与成本可控，遵循“不随评测自动变更生产默认配置”的工程原则，保持 Heuristic 默认，NVIDIA 作为可插拔配置。
- **代码位置**：`rag-core/.../rerank/HeuristicReranker.java`，`rag-core/.../rerank/NvidiaReranker.java`，`docs/eval/reports/c7-reranker-ab-full-2026-07-20.md`。

### Q4: 如何防止 LLM 产生幻觉？你们的引用校验机制是怎么实现的？
- **答案要点**：
  1. **Prompt 强约束**：系统指令强制限定“仅依据所给上下文回答，不足时明确输出固定拒答语，拒答时不带引用”。
  2. **事后引用严格校验（CitationValidator）**：LLM 生成答案后，提取其 Claim 与引用；要求 Citation 绑定的 documentId 必须属于当次检索结果，且文本片段（Snippet）在原文中必须存在完全匹配（exact）或 Token 重叠度 `>= 0.58`（token_overlap）；不合规引用全部剥离。
  3. 若判定为拒答，强制清空全部 Citation，防止“模型说不知道却挂了个参考文档”的虚假引用漏洞。
- **代码位置**：`rag-core/.../citation/CitationValidator.java`（L21 `MIN_TOKEN_OVERLAP = 0.58d`，L29-72 `validate`）。

### Q5: 异步文档索引如果处理到一半系统宕机，如何保证数据一致性与幂等？
- **答案要点**：
  1. **Durable Ledger 状态机**：MySQL `async_task` 记录执行阶段（`ACCEPTED` → `SAFE_PRE_VECTOR` → `VECTOR_IN_FLIGHT` → `VECTOR_CONFIRMED` → `FINALIZING` → `COMPLETED`）。
  2. **租约与心跳**：执行节点持有带超时的租约（`lease_until`）并定期心跳；超时未完成的任务由 `IndexTaskCoordinator` 扫描认领。
  3. **确定性 Chunk ID**：Chunk ID 基于内容与索引计算 SHA-256，重试时重新生成相同的 Vector ID，向向量库与数据库写入均为幂等 upsert。
  4. **事务化 SQL Finalize**：Milvus 写入成功后（`VECTOR_CONFIRMED`），DB 状态更新、分块写入与统计采用单事务行锁提交。
- **代码位置**：`rag-admin/.../task/impl/IndexTaskLedgerImpl.java`，`rag-admin/.../task/IndexTaskSqlFinalizer.java`，`rag-admin/.../task/DeterministicChunkIdentity.java`。

### Q6: 你们的多租户数据隔离是怎么实现的？在向量数据库层面怎么防越权？
- **答案要点**：
  1. **服务端身份锁定**：客户端不能传 `tenantId`，必须由服务端拦截器从经过校验的 JWT 解析出 `RequestIdentity(userId, tenantId)`。
  2. **关系数据面**：SQL 表强制包含 `tenant_id`，MyBatis-Plus 查询全局拼接租户条件。
  3. **向量数据面**：Milvus Collection 字段内置 `tenant_id` 标量字段；查询时在底层强制追加标量过滤表达式 `tenant_id == X and kb_id == Y`，跨租户无法被检索召回；不支持租户隔离的向量库（如未适配的 Qdrant/ES）在系统启动时被硬阻断（Fail-closed）。
- **代码位置**：`rag-admin/.../db/migration/V11__tenant_data_plane_enforcement.sql`，`rag-core/.../milvus/MilvusVectorStore.java`（L491 `withExpr(scopeExpression(scope))`）。

### Q7: 针对口语化问题或缩写，你们在检索前做了哪些查询改写（Query Rewriting）？
- **答案要点**：
  1. **标点与多余空白归一化**。
  2. **口语化前缀剥离**：使用正则剥离“请问”、“想问一下”、“分析一下”、“什么是”等客套噪音词，提取核心提问主干。
  3. **专业缩写同义词展开**：内置术语字典（如 `jwt` 扩展为 `json web token`、`token认证`；`oauth` 扩展为 `oauth 2.0`），生成多个检索变体分别检索并加权合并。
- **代码位置**：`rag-core/.../query/QueryEngineImpl.java`（方法 `buildQueryVariants`，L382-411）。

### Q8: 为什么要在 Prompt 构建时做 Token 预算（Token Budget）？怎么实现的？
- **答案要点**：
  1. 检索召回的 TopK 上下文拼接起来往往字数极大，若无脑灌入 Prompt 会导致：API 费用暴增、LLM 推理延迟显著拉长、且容易触发模型长上下文“迷失在中间”（Lost in the Middle）现象。
  2. 系统设定 `DEFAULT_CONTEXT_TOKEN_BUDGET = 1200`，由 `ConservativeTokenCounter` 在拼接前逐一检查候选块；一旦累计 Token 超过 1200，后续排名靠后的 chunk 被阻断丢弃，确保上下文聚焦。
- **代码位置**：`rag-core/.../prompt/PromptBuilder.java`（L229-246 `applyTokenBudget`）。

### Q9: SSE 流式传输中如果后端或大模型发生异常，前端怎么感知？
- **答案要点**：
  1. 流式阶段如果直接抛异常断开连接，前端常表现为“光标一直闪烁或回答戛然而止”。
  2. 系统设计了终端事件规范（`structured-v1`）：流式过程中正常输出 `data: {...}` 文本 chunk；在结束、异常或拒答时，发送专有事件 `event: terminal`，携带明确的 `finalState`（`ANSWER`, `NO_ANSWER`, `ERROR`, `CANCELLED`）与错误原因，让前端确定性渲染错态或拒答态。
- **代码位置**：`rag-admin/.../controller/QAController.java`（方法 `askStream`），`docs/roadmap/technical-debt.md`。

### Q10: 你们的评测体系有哪些指标？为什么说检索评测通过不能代表回答质量好？
- **答案要点**：
  1. 严格拆分 5 个独立通道：`Retrieval`（召回率/MRR）、`Generation`（关键词命中）、`Citation`（引用准确率）、`Claim Support`（客观词法主张支撑率）、`No-Answer`（拒答准确率）。
  2. 检索指标好（例如 Recall@5 高）仅代表“正确的证据文本出现在了召回候选列表中”，但并不保证：① Prompt 预算裁剪没有把它丢掉；② LLM 正确理解了这段证据；③ LLM 没有掺杂固有幻觉；④ 答案正确打上了引用标记。因此必须多通道独立断言。
- **代码位置**：`docs/eval/schema/rag-quality-gate-profile-v1.json`，`openspec/specs/evaluation/spec.md`。

### Q11: 你们的评测集是怎么构建和版本化治理的？
- **答案要点**：
  1. 采用版本化发布：当前为 `rag-eval-dev-v2`，由 150 条样本构成，受 `docs/eval/releases/rag-eval-dev-v2-manifest.json` 强校验。
  2. 包含每个样本的 SHA-256、期望关键词（keywords）、期望引用来源（sources）和期望上下文片段（contains）。
  3. 评测执行前先跑本地 Schema Validator，检查样本顺序、ID 合法性；任何对题目、正文或标注的篡改都会触发哈希不一致而终止评测，禁止“为了通过门禁而修改测试题”。
- **代码位置**：`scripts/eval_dataset_contract.py`，`scripts/build_eval_dataset_v2.py`。

### Q12: 为什么你们的 Objective Claim Support Rate 只有 5.22%？这是 Bug 吗？
- **答案要点**：
  1. 不是代码 Bug，而是真实严苛的词法基线。
  2. 当前计算方法是将自然语言答案切句后，与引用的原始 Context 片段做严格的 Token 级重叠（`>= 0.70`）。由于模型生成时采用总结、意译或语法重构，字面分词重叠率天然偏低。
  3. 项目坚持“不美化数据、不人为放宽阈值”，真实记录该工程指标，并明确将其作为引入 NLI（自然语言推理）模型和 LLM Judge 的技术演进依据。
- **代码位置**：`scripts/compile_generation_objective_baseline.py`，`docs/eval/reports/c18-generation-objective-review-v1.json`。

### Q13: 系统引入了 OpenTelemetry，具体的 Tracing 链路拓扑是怎样的？
- **答案要点**：
  1. 基于 OTel 1.31 API/SDK，系统运行默认开启进程内 Tracing 追踪（fail-open，不影响主业务）。
  2. 问答主链路追踪拓扑：`rag.ask` (Root Span) → `retrieval.query_variants` → `retrieval.vector_search` → `retrieval.keyword_search` → `retrieval.fusion` → `retrieval.rerank` → `prompt.build` → `llm.request` → `citation.validate`。
  3. 整个链路贯穿统一的 `traceId`，并记录各阶段耗时、Token 估算量及降级事件（如 `FALLBACK_SUCCESS`）。
- **代码位置**：`rag-common/.../trace/GenAiTelemetry.java`，`rag-admin/.../config/GenAiTracingConfiguration.java`。

### Q14: 如果知识库被删除了，相关的 Milvus 向量和 MinIO 文件怎么清理？
- **答案要点**：
  1. `DocumentServiceImpl.delete` 采用级联清理逻辑：
     - 首先从 MySQL `document_chunk` 读取该文档所有的 `vector_id`；
     - 校验知识库向量状态为 `READY` 后，调用 `MilvusVectorStore.delete` 批量删除 Milvus 集合中的向量行，并清理内存 BM25 索引；
     - 调用 `IndexInputStore.delete` 删除本地/MinIO 原始文件；
     - 最后在 MySQL 事务中逻辑删除 `document_chunk` 与 `document` 记录。
- **代码位置**：`rag-admin/.../kb/service/impl/DocumentServiceImpl.java`（L178-216）。

### Q15: 如果高并发下很多用户同时提问，系统怎么扛住压力？
- **答案要点**：
  1. **接入层保护**：基于 Redis 滑动窗口对每个用户限流（30次/分），阻断单用户刷接口。
  2. **问答缓存**：支持可选的问答结果缓存（`enableCache=true`），相同问题的请求直接命中缓存返回。
  3. **并发线程池隔离**：文档解析与入库使用有界的 `asyncTaskExecutor` 线程池；WebClient 对外调用使用非阻塞响应式 I/O，不占用 Tomcat 业务线程。
  4. **下游熔断与重试**：外部大模型调用配置超时（120s）与重试退避，防止外部服务抖动导致线程池被耗尽。
- **代码位置**：`rag-common/.../ratelimit/SlidingWindowRateLimiter.java`，`rag-common/.../async/RedisAsyncTaskManager.java`。

---

## 10. 候选人优先搞懂的核心代码点与设计决策（Top 10）

### 1. `QueryEngineImpl.java` 的双路检索与 RRF 融合实现
- **位置**：`rag-core/src/main/java/com/enterprise/rag/core/rag/query/QueryEngineImpl.java#fuseByRrf`
- **为什么重要**：这是整个 RAG 系统的检索核心。面试必问“如何解决专有名词召回不准”以及“为什么不用线性加权”。掌握 RRF 的公式、k=60 的作用以及双路归一化难点，能体现对 RAG 召回痛点的深度理解。

### 2. `CitationValidator.java` 的引用校验与防幻觉机制
- **位置**：`rag-core/src/main/java/com/enterprise/rag/core/rag/citation/CitationValidator.java`
- **为什么重要**：这是项目区别于一般 Toy RAG 项目的关键特色。面试官最爱抓着“RAG 如何保证可信度、怎么防止幻觉”提问。需要讲清楚如何通过元数据溯源校验 + Token 重叠度（0.58）过滤悬空引用，以及拒答时清空引用的设计。

### 3. `PromptBuilder.java` 的 Token 预算裁剪与严格拒答指令
- **位置**：`rag-core/src/main/java/com/enterprise/rag/core/rag/prompt/PromptBuilder.java`
- **为什么重要**：体现工程落地细节。没有 Token 预算的 RAG 系统在工业界根本无法上线（不仅费钱而且延迟高、容易迷失）。理解动态 Token 估算、按优先级丢弃后置块，以及中英文严格拒答指令对评测的重要影响。

### 4. `DocumentIndexingServiceImpl.java` 的 Durable Ledger 状态机与幂等
- **位置**：`rag-admin/src/main/java/com/enterprise/rag/admin/kb/service/impl/DocumentIndexingServiceImpl.java`
- **为什么重要**：体现扎实的后端分布式工程功底。回答“大文件解析入库做到一半挂了怎么恢复”时，必须拿出基于 MySQL Durable Ledger、租约（Lease）、心跳（Heartbeat）与 SHA-256 确定性 Chunk ID 的全套恢复方案。

### 5. `MilvusVectorStore.java` 的主键映射与多租户标量过滤
- **位置**：`rag-core/src/main/java/com/enterprise/rag/core/vectorstore/milvus/MilvusVectorStore.java`
- **为什么重要**：这是系统的底层存储核心。不仅要清楚 Milvus 的 IVF_FLAT 与 COSINE 配置，还要直面已知技术债（MySQL Long 自增主键与 Milvus VarChar 哈希主键的割裂），以及租户过滤表达式是如何下推到 Milvus 引擎执行的。

### 6. `AnswerGeneratorImpl.java` 的流式处理、重试策略与可见字符防重
- **位置**：`rag-core/src/main/java/com/enterprise/rag/core/rag/generator/AnswerGeneratorImpl.java`
- **为什么重要**：考察 Reactive 编程与网络弹性。重点看 `applyStreamRetryPolicy` 中的细节——**一旦向用户输出了首批字符（visibleContentEmitted），就坚决不再重试**，否则客户端会收到重复甚至撕裂的文本流。

### 7. `QAController.java` 的 SSE 结构化终端事件契约（`structured-v1`）
- **位置**：`rag-admin/src/main/java/com/enterprise/rag/admin/controller/QAController.java#askStream`
- **为什么重要**：前后端协议设计的最佳实践。传统 SSE 只推文本流，前端拿不到终态元数据。理解为什么要在末尾推 `terminal` 事件携带 Citations、Final State 与 Diagnostics，以及如何避免流式失败保存为虚假历史。

### 8. `RerankerRegistry.java` 与 `HeuristicReranker.java` / `NvidiaReranker.java`
- **位置**：`rag-core/src/main/java/com/enterprise/rag/core/rag/rerank/`
- **为什么重要**：体现高内聚低耦合的架构设计。当昂贵的外部大模型重排不可用时，系统如何优雅降级到本地 Heuristic 启发式重排而不导致请求崩溃，并且把 fallback 事件记入链路追踪。

### 9. 评测体系与离线门禁设计（`evaluate_quality_gate.py` & `c18_generation_objective_review-v1.json`）
- **位置**：`scripts/evaluate_quality_gate.py`，`docs/eval/schema/rag-quality-gate-profile-v1.json`
- **为什么重要**：衡量技术人是否具备数据驱动（Data-driven）的研发思维。能够清晰阐明 150 条数据集的构成、Recall@5 为何只有 47.44%（真实的冷启动开发基线）、以及 Hard Floor 与 Regression Tolerance 组合门禁的设计哲学。

### 10. `SecurityConfig.java` & `JwtTokenProvider.java` 租户事实绑定
- **位置**：`rag-auth/src/main/java/com/enterprise/rag/auth/jwt/JwtTokenProvider.java`
- **为什么重要**：B 端系统最看重的安全红线。要理解为什么租户信息绝不能允许客户端通过 Header 或 Query 传参指定，必须由服务端的认证事实生成并固化进 JWT，并在每次请求解析为不可变的 `RequestIdentity`。

---

## 附录：剩余问题与证据缺口（Remaining Questions & Gaps）

1. **未完全回答的问题**：
   - Docker socket 在高并发或重启时的失效根因仍未证实（文档标记为环境偶发，无代码级定位）。
   - 生产级多租户与多向量库（Qdrant/ES）对等多租户尚未实现，当前在隔离模式下被直接阻断。
2. **证据较弱的领域**：
   - 生产真实业务数据下的生成质量：目前所有的实测数据均来自 `rag-eval-dev-v2` 150 条开发集（3 份 Markdown 种子），缺乏在真实企业多格式（超长 PDF、扫描件表格）复杂知识库下的压力表现。
   - LLM 自动化 Judge 目前缺乏真实 Provider 的校准数据，一致性证据为空白。
3. **后续建议优先级**：
   - **P0**：修复 C18 预算审计源文件哈希（因 C21 变更引起的 `budget_source_drift`），恢复全套 Python 评测脚本 GREEN。
   - **P1**：统一 Milvus 向量主键与 MySQL `document_chunk.id` 的映射规范，消除双 ID 隐患。
   - **P2**：推动 C19 Live Judge 校准，获取真实 LLM 打分一致性证据，完善回答忠实度（Faithfulness）评测。
