# chatgpt-ui-demo 对接真实后端调研报告

> 文档性质：只读调研报告（Type A 产物），不改变任何能力声明，不构成实现授权。
> 状态日期：2026-08-30 · Git HEAD：`46bd90a`（main，ahead 15）。
> 调研方法：静态阅读后端 Controller/DTO/Security 配置、现有 `rag-frontend` API 层、OpenSpec spec 与 demo 全部源码；未启动后端、未发起任何 provider/embedding/LLM 外部调用。所有契约字段均标注来源文件，行号以该 HEAD 为准；运行时行为（尤其异常分支）需联调实测复核。

---

## 1. 结论摘要（TL;DR）

1. **后端可对接面是完整且成熟的**：登录/JWT、知识库 CRUD、文档异步上传 + 任务轮询、同步问答（含 citations）、SSE 流式问答、历史与反馈均有 confirmed 级实现，共 5 个 Controller、约 25 个端点，全部挂在 `http://localhost:8080`（无 context-path）。
2. **demo 与后端的最大差距不是"缺接口"，而是三个模型级错位**：
   - **会话模型**：demo 以"对话（含多轮 turns）"为组织单位；后端 QA 历史是**扁平的单轮问答记录**（无 conversation 实体、无多轮上下文），`/api/qa/ask` 每次提问相互独立。
   - **引用模型**：demo 在答案文本里内嵌 `{{cite:N}}` 标记渲染行内引用角标；后端答案文本**不含可解析的引用标记**，引用是事后归因出的独立数组（且流式路径 citations 恒为空）。
   - **检索管道视图**：demo 的分步管道（路由/改写/召回/重排/组装 + 每步毫秒数）在同步 `/ask` 响应里**没有对应数据**；最接近的真实数据源是 `POST /api/qa/debug/retrieve`（仅检索、有界字段），step 级耗时只能保留为演示。
3. **demo 9 个视图中有 5 个无后端支撑**（评测 / MCP / 可观测 / 知识源 / 研究任务），对接时应保持静态演示或裁剪，不要为其发明接口。
4. **对接本身不需要新基础设施**：后端 CORS 允许任意来源（`allowedOriginPatterns("*")`），demo 的零依赖静态服务形态可以直接跨域调 8080；也可以走同源反代贴近生产形态。
5. **治理路径**：本调研是 Type A 只读产物；真正动手对接属于 Type C（新增用户可见能力、改变 demo"不发起网络请求"的边界声明），必须先立 OpenSpec change 并更新 `.ai/ACTIVE_TASK.md`。

---

## 2. 真实后端契约盘点

### 2.1 部署形态与入口

| 项 | 值 | 来源 |
| --- | --- | --- |
| 服务端口 | `8080`，无 context-path，路径直接挂根 | `rag-admin/src/main/resources/application.yml` |
| REST 前缀约定 | 认证 `/auth/**`；业务 `/api/**` | 各 Controller 类级 `@RequestMapping` |
| Swagger | `/swagger-ui.html`、`/v3/api-docs`（联调时的实时契约源） | `application.yml:91-96` |
| multipart 上限 | 单文件 50MB / 请求 55MB | `application.yml:11-15` |
| 基础设施依赖 | MySQL、Redis（token 黑名单/会话/限流/任务投影）、Milvus | `docker-compose.yml`（仅基础设施在 compose 中，后应用本地启动） |
| 现有前端 dev 代理 | Vite 5173 → 8080，代理 `/api`、`/auth` 两个前缀 | `rag-frontend/vite.config.ts:15-24` |

### 2.2 认证契约（`AuthController`，前缀 `/auth`）

| 端点 | 方法 | 入参 | 出参 `data` | 备注 |
| --- | --- | --- | --- | --- |
| `/auth/login` | POST | `{username, password}` | `AuthResponse` | 限流 20 次/60s/IP；失败 401 `[AUTH_001]` |
| `/auth/refresh` | POST | `{refreshToken}` | 新 `AuthResponse` | 限流 60 次/60s/IP；**rotate：旧 refresh token 立即作废** |
| `/auth/logout` | POST | header Bearer | `null` | access token 进 Redis 黑名单 |

`AuthResponse`（`rag-auth/.../dto/AuthResponse.java:15`）：`accessToken` / `refreshToken` / `expiresIn`（access 有效秒数）/ `tokenType: "Bearer"` / `userInfo{id, username, email}`。

关键约束（前端必须处理）：

- **无注册 API**。首个账号依赖 `AUTH_BOOTSTRAP_*` 环境变量引导（`application.yml:70-75`，默认关闭）。联调前需确认账号来源。
- access token 有效期 **3600s**、refresh **604800s**（7 天）；JWT claim 含 `userId`、`tenantId`（无 tenantId 或 ≤0 的 token 直接无效，`JwtTokenProvider.java:187-201`）。
- **refresh 一次一换 + Redis 会话一致性校验**（`AuthServiceImpl.java:255`）：同一会话并发 refresh 会互相踢掉 → 前端必须做**单飞（single-flight）刷新**（`rag-frontend/src/api/request.ts:24-97` 已有可抄实现：`isRefreshing` 标志 + 等待队列 + 独立 axios 实例）。
- 401 判定请以 **HTTP 状态码**为准（见 2.3 双信封问题）。

### 2.3 统一响应与错误结构（⚠️ 双信封，对接最大坑点之一）

**成功/业务错误信封** `ApiResponse`（`rag-common/.../ApiResponse.java:22`，`ApiResponseAdvice` 自动包裹一切返回值）：

```json
{ "code": 200, "message": "success", "data": <T>, "traceId": "...", "timestamp": "...Z" }
```

`data` 为 null 时该字段整个省略（如 DELETE 返回无 `data` 键）。

**校验/系统错误信封** `ErrorResponse`（`ErrorResponse.java:21`）——**与成功信封不同构**：

```json
{ "status": 400, "errorCode": "VALIDATION_001", "message": "...", "traceId": "...", "timestamp": "...Z", "path": "...", "fieldErrors": [...] }
```

**安全层 401/403**（`JwtAuthenticationEntryPoint.java:35`）：HTTP 状态码正确（401/403），但 body 是 `ApiResponse` 形态且 `code` 恒为 500：`{"code":500,"message":"[AUTH_001] 未认证，请先登录"}`。

**限流 429**（`RateLimitExceptionHandler.java:27`）：响应头带 `X-RateLimit-Remaining` / `X-RateLimit-Reset` / `Retry-After`，body 是 ApiResponse 形态（`code:500`）。

**Redis 依赖不可用**：认证过滤器直接写 503 `REDIS_DEPENDENCY_UNAVAILABLE`（`JwtAuthenticationFilter.java:99-106`）。

→ **前端判定成败一律用 HTTP status；错误文案需同时兼容 `body.errorCode` 与 `body.message` 内 `[CODE] msg` 两种形态。**

### 2.4 REST 端点清单

#### 知识库（`KnowledgeBaseController`，`/api/knowledge-bases`）

| 端点 | 方法 | 入参 | 出参 `data` | 状态码/权限/限流 |
| --- | --- | --- | --- | --- |
| `/` | POST | `{name(≤100必填), description?(≤500), isPublic?=false}` | `KnowledgeBaseDTO` | 201；ADMIN 语义由 owner 即得；20/60s |
| `/` | GET | — | `KnowledgeBaseDTO[]`（当前用户可访问全部） | 登录 |
| `/{id}` | GET/PUT/DELETE | path id | `KnowledgeBaseDTO` / null | 读写权限分层；PUT 30/60s、DELETE 10/60s |
| `/{id}/statistics` | GET | — | `KnowledgeBaseStatistics` | READ |
| `/{id}/documents` | POST | **multipart：`file` 必填、`title` 可选** | `DocumentUploadResponse` | **202**（异步）；WRITE；15/60s；支持 `X-Idempotency-Key` 幂等 |
| `/{id}/documents` | GET | — | `Document[]` | READ |
| `/{kbId}/documents/{docId}` | DELETE | — | null | WRITE；20/60s |

`KnowledgeBaseDTO`：`id, name, description, ownerId, vectorCollection, documentCount, isPublic, createdAt, updatedAt`。
`KnowledgeBaseStatistics`：`kbId, documentCount, vectorCount, queryCount`。
`Document`：`id, kbId, uploaderId, title, fileType, contentHash, status, chunkCount, createdAt, updatedAt`（`status ∈ PENDING/PROCESSING/COMPLETED/FAILED/RECONCILIATION_REQUIRED`；**无文件大小字段**，`inputSizeBytes` 被 `@JsonIgnore`）。
`DocumentUploadResponse`：`documentId, taskId(string), fileName, fileType, status="PENDING"`。

上传错误码（`KnowledgeBaseController.java:195-201`、`DocumentIndexingServiceImpl.java:140`）：`DOC_001` 类型不支持（白名单：pdf / md / txt / docx / 代码文件）、`DOC_002` 空文件、`DOC_003` 空文件名、`DOC_004` 文档不存在(404)、`DOC_005` 文档不属于该库、`DOC_006` 删除失败。

#### 任务（`TaskController`，`/api/tasks`）

| 端点 | 方法 | 出参 `data` |
| --- | --- | --- |
| `/{taskId}` | GET | `TaskStatusResponse` |
| `/{taskId}/result` | GET | 同上（含 result；未完成 → 400 `TASK_002`） |
| `/{taskId}/cancel` | POST | `{taskId, cancelled}`；已完成 → 400 `TASK_003`；20/60s |
| `/{taskId}/exists`、`/{taskId}/completed` | GET | 轻量探测 |

`TaskStatusResponse`：`taskId, taskType, state, progress(0-100), message, result, error, createdAt, updatedAt`；`state ∈ PENDING/RUNNING/COMPLETED/FAILED/CANCELLED`。任务归属校验 tenant+user（`TaskController.java:182-193`）。上传后的进度消息序列为 10/30/50/70/85/100 六段（demo 的 `TASK_STAGES` 已与之对齐）。

#### 问答（`QAController`，`/api/qa`）

| 端点 | 方法 | 入参 | 出参 | 限流 |
| --- | --- | --- | --- | --- |
| `/ask` | POST | `AskRequest{kbId, question, topK?, minScore?, filter?, enableCache?}` | `QAResponse` | 30/60s/用户 |
| `/ask` | GET | query `kbId, question, topK(默认5)` | `QAResponse` | 未挂限流注解 |
| `/ask/stream` | POST | 同 `AskRequest` | **SSE**（见 2.5） | 10/60s/用户 |
| `/debug/retrieve` | POST | 同上 + `enableRerank?` | `RetrievalDebugResponse` | 60/60s/用户 |

`QAResponse`（`rag-core/.../model/QAResponse.java:20`）：`question, answer, citations[], contexts[], metadata{}`。
`Citation`：`source, sourceFileName, documentTitle, documentId, chunkId, score, snippet, startIndex, endIndex`。**注意：Citation 不含 kbId/kbName**——"来自哪个知识库"只能从本次提问上下文得知。
`RetrievedContext`：`content, source, relevanceScore, metadata{chunkId, chunkIndex, documentTitle, sourceFileName, ...}`。

`metadata` 实际会出现的键（已逐一 grep 核实）：`cached`、`contextCount`、`contextTokenBudget`、`estimatedContextTokens`、`estimatedOutputTokens`、`citationValidation`、`citationFallbackUsed`、`citationFallbackCount`、`droppedCitations`、`validCitations`、`retrievedContextCount`、`retrievedTopScore`、`retrievedAvgScore`、`noAnswerReason`、`vectorCollection`、router 开启时的 `route*` 系列、异常时的 `status: "error"/"no_result"` + `error*`/`llmDiagnostics`/`vectorDiagnostics`。**没有总耗时字段，也没有分步耗时**；`latencyMs` 只写入历史记录（`QAController.java:135-149`）。

`RetrievalDebugResponse`（`QAController.java:1093-1131`）：`queryVariants[{query,weight}], topK, enableRerank, contextCount, topScore, avgScore, status("ok"/"retrieve_failed"), message, warnings[], diagnostics{rerank*}, contexts[RetrievedContextDebugItem{rank, source, sourceFileName, documentTitle, displaySource, chunkId, documentId, chunkIndex, startIndex, endIndex, score, contentPreview, snippet, contentLength, metadata}]`。**检索失败时也是 HTTP 200**，靠 `status` 字段区分。

权限：所有问答端点要求登录且对 `kbId` 有 READ 权限（owner/公开/被授权）。

#### 历史与反馈（`HistoryController`，`/api/history`）

| 端点 | 方法 | 入参 | 出参 |
| --- | --- | --- | --- |
| `/` | GET | query `kbId?`、`page`(默认1)、`size`(默认20，≤100) | `PageResult<QAHistoryDTO>` |
| `/{id}` | GET / DELETE | — | `QAHistoryDTO` / null |
| `/{id}/feedback` | POST | `{rating(1-5必填), comment?}` | 201 `QAFeedbackDTO`；重复 → 400 `FEEDBACK_001`；20/60s |
| `/{id}/feedback` | GET | — | `QAFeedbackDTO[]` |
| `/feedback/my` | GET | — | 当前用户全部反馈 |

`QAHistoryDTO`：`id, userId, kbId, question, answer, citations[], traceId, latencyMs, createdAt`（按 createdAt 倒序；只能看自己的）。
`QAFeedbackDTO`：`id, qaId, userId, rating, comment, createdAt`。**反馈列表不含问题文本与 kb**——展示"我的反馈"需逐条回查 `GET /api/history/{qaId}`。

### 2.5 SSE 流式契约（重点，demo 接入时最易踩坑）

端点 `POST /api/qa/ask/stream`，`Content-Type: text/event-stream`（`QAController.java:162`）：

- **不能用 `EventSource`**：需要 POST JSON body + `Authorization` 头，必须用 `fetch` + `response.body.getReader()` 手工解析（`rag-frontend/src/composables/useSSE.ts` 已有完整可抄实现）。
- **无自定义 `event:` 名**，每条消息只有 `data:` 行；data 是**纯文本 delta**（LLM token 片段），不是 JSON。
- Spring SseEmitter 输出为 `data:chunk`（**冒号后无空格**）→ 解析时必须精确 `line.slice(5)`，多切一个字符会吞掉 token 的前导空格（`useSSE.ts:96-99` 注释明确记录过这个坑）。
- 正常结束：`data:[DONE]`（`QAController.java:236-251`）。
- **错误也是 200 流内文本**：先 `data:[ERROR] <中文错误>`，再 `data:[DONE]` 后 complete（`QAController.java:234-237`）——不能依赖 HTTP 错误码。
- **流式路径没有 citations/引用事件**；正常完成时服务端落历史但 citations 存空列表。这是 `docs/architecture/overview.md` 明示的已确认边界，也呼应 `docs/roadmap/technical-debt.md` P1.4（SSE 结构化 terminal result 未做）。
- SseEmitter 超时 120s；客户端主动断开用 `AbortController`（中断后是否落历史未验证，联调时实测）。
- 无 WebSocket 实现（全仓 grep 确认）。

### 2.6 CORS 与限流

- CORS（`SecurityConfig.java:129-163`）：允许来源 `allowedOriginPatterns("*")` + `allowCredentials(true)` → **任何 http(s) 源都能直连 8080**；方法 GET/POST/PUT/DELETE/PATCH/OPTIONS；允许头 `Authorization, Content-Type, X-Requested-With, X-Trace-Id, X-Idempotency-Key`；暴露 `X-Trace-Id, X-RateLimit-*`。
- 限流汇总（超出即 429）：login 20/60s/IP、refresh 60/60s/IP、ask 30/60s/user、stream **10/60s/user**、debug 60/60s/user、KB 增 20 / 改 30 / 删 10/60s、上传 15/60s、文档删 20/60s、历史删 30/60s、反馈 20/60s、任务取消 20/60s。

### 2.7 多租户与安全边界

- 租户事实在 JWT claim `tenantId` 中，由服务端签发与校验；**前端不能也不需要传任何租户 header/参数**（demo 高级菜单里的"模拟越权 filter"对应真实服务端行为：客户端 filter 携带保留字段 `tenant` 会被 `RAG_SCOPE_FILTER_RESERVED` 拒绝——这是真实实现，不是演示虚构）。
- 换租户 = 换账号登录。实体响应中租户字段均被 `@JsonIgnore`。

---

## 3. demo 现状盘点

demo 形态：零依赖纯静态（HTML/CSS/原生 JS），`js/app.js` 约 2900 行 + `js/login.js`（登录页演示动画），文件头与服务提示均声明"不发起任何网络请求"。

| 视图/功能 | 实现方式 | 后端支撑 |
| --- | --- | --- |
| 首页问候 + 建议 | 写死建议文案，随"聊天/工作"模式轮换 | 建议→提问可直接用 |
| 会话视图（多轮 + 流式） | `pickQA()` 按关键词匹配预置答案，`setInterval` 逐字输出；`{{cite:N}}` 内嵌引用角标；终态条（ANSWER/NO_ANSWER/ERROR/CANCELLED/UNSUPPORTED） | 同步+流式问答 API（模型错位见 §5.1） |
| 检索管道可视化 | `mkPipe()` 本地生成 5 步管道 + 每步毫秒数 + 查询变体 | `debug/retrieve` 可支撑大部分字段（无分步耗时） |
| 知识库列表 | mock `KBS` 数组（含 size/状态/近7天热度 spark） | KB 列表 API（无 size、无热度时序） |
| 知识库详情 | mock 文件列表 + 向量集合/租户/分块配置/spark 图 | 详情 + statistics + 文档列表 API（spark 无时序数据源） |
| 上传任务面板 | 本地定时器模拟 6 段进度（消息已对齐真实回调序列） | 上传 + 任务轮询 API 可完整替代 |
| 我的反馈 | mock 列表（含 rating 1-5 + comment） | 反馈 API（缺问题文本，需回查历史） |
| 搜索弹窗 Ctrl+K | 本地搜 CONVS + KBS | KB 名可本地过滤；对话搜索无后端 API |
| 设置弹窗 | 外观/强调色真实生效（localStorage） | 纯客户端，无需后端 |
| 评测 / MCP / 可观测 / 知识源 / 研究任务 | 5 个静态视图，口径对齐 evaluation spec / C15 / C11-C12 / 蓝图 W3/W4 | **无任何 REST 支撑**（评测是 Python 工具链、MCP 默认关、后三者为规划原型） |
| 登录页（login.html / login-dark.html） | 纯演示校验，明确提示"接入 rag-auth 登录接口" | `/auth/login` 可直接接入 |
| 分享/导出 Markdown/复制 | 纯客户端 | 无需后端 |

demo 内部已有不少"口径对齐"注释（TaskState 五态、FeedbackRequest、RetrievalDebugResponse 字段、C13 越权拦截、C21 终态），说明原作者已按真实契约预演过 UI——这是对接的有利条件。

---

## 4. 差距对照矩阵（按功能域）

图例：✅ 可直连 / 🔧 需适配（有接口但形态有差） / ❌ 无后端支撑（保持演示或裁剪）

| # | demo 功能 | 判定 | 差距与对接要点 |
| --- | --- | --- | --- |
| 1 | 登录页 | 🔧 | 表单字段与后端一致；需接 `/auth/login`、双 token 存储（建议沿用 `rag_accessToken`/`rag_refreshToken` key）、userInfo 渲染（demo 硬编码 `admin`）、限流 429 文案、错误信封双形态兼容 |
| 2 | token 续期/登出 | 🔧 | 新增：单飞 refresh + 401 队列重放 + refresh 失败强制登出；`/auth/logout` 接入（现有"退出登录"是占位 toast） |
| 3 | 知识库范围选择 | 🔧 | demo 按**名称**选库（`pick-kb`）；真实需先 `GET /api/knowledge-bases` 拿 **kbId(number)**，以 id 为唯一标识、名称仅展示 |
| 4 | 同步问答 | 🔧 | `AskRequest` 直连；demo 的"检索深度 低/中/高"需映射为 `topK`（demo 虚构的 8/16/24 vs 后端默认 5，映射需产品确认）；`enableCache`/`minScore`/`filter` 真实存在，可直接映射高级菜单 |
| 5 | 流式问答 | 🔧 | 新增 fetch-SSE 客户端（抄 `useSSE.ts`：`slice(5)`、`[DONE]`、`[ERROR] ` 前缀）；发送/停止按钮改 AbortController；注意流式 10/60s 限流与"重新生成"按钮的叠加触发 |
| 6 | 引用角标 + 浮层 | 🔧 | `{{cite:N}}` 内嵌标记无后端支撑 → 改为答案下方**引用列表**（Citation[] 的 documentTitle/sourceFileName/chunkId/snippet/score），或后处理匹配；浮层字段映射见附录 B |
| 7 | 检索管道视图 | 🔧 | 真实数据源是 `debug/retrieve`（额外一次请求，60/60s）：queryVariants/topScore/avgScore/contextCount 可真；分步耗时/tenant 过滤步骤/Milvus 降级演示保留为演示或删除 |
| 8 | 终态条（五态） | 🔧 | 同步路径从 `metadata.status`/`noAnswerReason` 推导；**流式路径后端无结构化终态**（只有文本流 + `[ERROR]`），需本地推断或维持演示（依赖 P1.4 技术债） |
| 9 | 会话列表（侧栏"最近"） | 🔧 | **会话实体不存在**：`GET /api/history` 返回扁平问答记录。可选方案：a) 侧栏直接列历史问答（改信息架构）；b) 前端把同一会话的记录本地聚合（但换设备/清缓存即失联）；c) 推动后端加 conversation 概念（Type C，超出本次对接） |
| 10 | 对话删除/重命名 | 🔧 | `DELETE /api/history/{id}` 只删单条问答记录，不是"删会话"；重命名完全无支撑（会话标题本就是前端生成的） |
| 11 | 点赞/点踩 | 🔧 | 需拿到本次问答的 **historyId**：同步 `/ask` 的响应体不返回历史 id（历史在服务端自动保存），需在提问后 `GET /api/history` 反查（按 question+kbId+时间匹配，不可靠）→ 建议联调确认是否可推动后端在 QAResponse 中回传 historyId（小契约增量） |
| 12 | 我的反馈 | 🔧 | `GET /api/history/feedback/my` 直连；问题文本需逐条 `GET /api/history/{qaId}` 回查（N+1，注意量） |
| 13 | 知识库列表/搜索 | 🔧 | 直连后本地过滤；demo 的 size 列、评测库"状态"徽标、"近7天热度"spark 无数据源（queryCount 是累计值）→ 列或演示或删 |
| 14 | 知识库新建/重命名/删除 | 🔧 | POST/PUT/DELETE 直连（demo 现为占位 toast）；删除需确认弹窗（真实不可逆） |
| 15 | 文档列表/删除 | 🔧 | 直连；文档"大小"无字段、"文档预览浮层"无内容端点（REST 无 chunk 读取；MCP 默认关）→ 预览保持演示或去掉 |
| 16 | 上传 + 任务面板 | ✅ | `POST .../documents`(multipart) → `taskId` → 2s 轮询 `GET /api/tasks/{id}` → `COMPLETED/FAILED` 终止 + `POST .../cancel`；demo 的六段进度消息已与真实回调序列一致，改动量最小 |
| 17 | 上传幂等 | 🔧 | 可选带 `X-Idempotency-Key`（CORS 已放行该头）；失败重试场景建议带上 |
| 18 | Ctrl+K 搜索对话 | ❌ | 无对话搜索 API；只能对已加载的历史页做客户端过滤，或裁剪 |
| 19 | 评测/MCP/可观测/知识源/研究任务 | ❌ | 无 REST 支撑；保持静态演示页（demo 页脚已声明"演示数据"）或在本期从导航中隐藏 |
| 20 | 设置（外观/强调色） | ✅ | 纯客户端已实现 |
| 21 | 分享/复制链接/导出 MD/听写/语音 | ❌/✅ | 均纯客户端：复制/导出可用；听写/语音模式保持占位 |
| 22 | 响应缓存/路由高级选项 | 🔧 | `enableCache` 真实；`rag.router.enabled` 生产默认关闭——demo 的 fact/compare/high-risk 路由演示全部是规划原型（C16 默认关、W2/W4 未实现），UI 可保留演示标注 |

---

## 5. 三个架构性差距（需要产品/架构决策，不是写代码能顺带解决的）

### 5.1 会话模型错位

后端问答是**无状态单轮**：`AskRequest` 不携带会话 id，也没有"把上一轮答案拼进上下文"的参数；历史表每条记录独立。demo 的整个信息架构（侧栏会话列表、会话内多轮、会话级分享/导出/重命名）建立在不存在的实体上。

- **若保持 demo 现有交互**：会话只能作为**纯前端本地概念**（localStorage 聚合 historyId 列表），需接受换设备/清缓存丢失、且"追问"实际是新问题（RAG 不会理解指代）。
- **若以历史为主信息架构**：侧栏改为"最近问答"，更贴近后端事实，但偏离 ChatGPT 范式。
- **若要真多轮**：需要后端增加会话与上下文拼接能力 → 独立的 Type C change，建议不与本次对接捆绑。

### 5.2 引用呈现模型错位

demo 的行内 `{{cite:N}}` 角标依赖预置答案里的手工标记；真实答案文本由 LLM 生成、引用由 `CitationValidator` 事后归因（`AnswerGeneratorImpl.java:104-128`），**答案字符串里没有可解析的锚点**。可行的降级方案（按成本排序）：

1. 引用列表挂在消息底部（rag-frontend 的现行做法），放弃行内锚点；
2. 保留行内角标但由前端做"句子级模糊匹配"推断锚点（启发式，会有错标）；
3. 推动后端在生成时输出结构化行内标记（改 prompt/生成契约 → Type C + 评测口径影响，成本高且违反"不为前端定制 prompt"的约束）。

另注意：**流式路径 citations 恒为空**（服务端落历史也是空列表），流式消息要么不显示引用、要么流结束后用同步 `/ask` 重新问一次换取 citations（翻倍消耗 LLM 调用，不建议默认开启）。

### 5.3 流式终态缺失

demo 的五态终态条（含 NO_ANSWER 拒答解释、ERROR 分类）在流式路径没有结构化数据源，只有 `data:[ERROR] <msg>` 文本。短期方案是本地推断（收到 `[ERROR]` → ERROR 态、正常 `[DONE]` 但全文为空 → 按 no-answer 处理）；根本解决依赖 `docs/roadmap/technical-debt.md` P1.4（SSE 结构化 terminal result）。

---

## 6. 错误与边界语义注意事项（对接必读清单）

1. **判定成败用 HTTP status**，不读 `body.code`（Security 层 401/403 的 body `code` 恒为 500）。
2. 错误文案兼容两种形态：`ErrorResponse.errorCode` 与 `ApiResponse.message` 内的 `[AUTH_001] xxx` 前缀。
3. **时间格式混用**：历史/知识库/文档的 `createdAt/updatedAt` 是无时区 LocalDateTime（DB 时区 Asia/Shanghai）；`ApiResponse.timestamp` 与任务的是 UTC Instant（带 Z）。分别格式化。
4. ID 类型：`kbId/documentId/userId` 是 JSON number（Long）；`taskId`、demo 会话 id 是 string。
5. refresh 必须单飞；两个标签页并发刷新会互踢（rotate + 会话一致性校验）。
6. 流式限流 10 次/分钟：建议问题、自动重试、"重新生成"都要考虑配额；429 时读 `Retry-After`。
7. 上传 202 ≠ 完成：必须轮询任务或文档状态；`RECONCILIATION_REQUIRED` 表示需人工干预，不会自动恢复。
8. 无结果/错误在同步路径也是 200：靠 `metadata.status`（`no_result`/`error`）区分，不要只看 HTTP。
9. `debug/retrieve` 检索失败也是 200 + `status:"retrieve_failed"`。
10. Redis 未启动时所有需认证请求 503（本地联调常见故障，先查 compose 栈）。
11. 联调会产生**真实的 embedding/rerank/LLM provider 调用与费用**：按仓库规则需在事前说明调用量并取得授权；当前 active change（C17）阶段向量索引处于未就绪状态，问答可能以 `VECTOR_INDEX_NOT_READY`/503 失败——联调前先确认有一个已完成索引的知识库。
12. demo 的 `mdRender()` 是迷你渲染器（粗体/行内代码/标题/列表/引用块/代码块），真实 LLM 回答可能含表格、链接、嵌套结构 → 要么扩渲染器，要么换 markdown-it（引入依赖，破坏"零依赖"边界，需决策）；现有"先 esc 后渲染"的顺序必须保留（防 XSS）。

---

## 7. 迁移策略建议

### 7.1 部署形态（二选一，建议 B）

- **A. 直接跨域**：保持 python `http.server`（如 :8765），前端 `fetch` 直接指向 `http://localhost:8080`。CORS 已全开，零新增设施；`file://` 打开的 Origin 为 `null` 不保证可用，务必经 http 访问。
- **B.（建议）同源反代**：给 demo 配一个最小反代（Vite dev server / nginx / caddy 均可），把 `/api`、`/auth` 转发 8080——与 `rag-frontend` 的 dev 代理和未来生产同源部署形态一致，避免 cookie/origin 类隐性差异。

### 7.2 分期切片（每期独立可验证，符合"一次一个切片"约束）

| 期 | 内容 | 验收 |
| --- | --- | --- |
| P0 认证 | 登录页接 `/auth/login`；token 存储 + axios 式请求封装（原生 fetch 即可）+ 单飞 refresh + 401 处理 + logout；顶栏/菜单显示真实 userInfo | 登录→刷新页面保持登录→过期自动续期→登出黑名单生效 |
| P1 问答主链路 | KB 列表接真实数据（kbId 选择器）；同步 `/ask` + 流式 `/ask/stream`；引用列表（降级方案 1）；[ERROR]/[DONE]/停止按钮；错误/空态（no_result）UI | 真实知识库上问答全链路可用，拒答与错误可见 |
| P2 知识库与文档 | KB 新建/删除；文档列表/删除；上传 + 任务轮询面板（mock 定时器替换为真实轮询） | 上传→进度→完成→可检索 闭环 |
| P3 历史与反馈 | 历史列表接 `/api/history`（信息架构按 §5.1 决策落地）；点赞/点踩接 feedback（需先解决 historyId 获取，见矩阵 #11）；我的反馈页 | 反馈可写可读 |
| P4 检索可视化 | 高级菜单参数真实映射（topK/minScore/enableCache）；管道视图改吃 `debug/retrieve`；演示成分显式标注 | 管道数据与真实检索一致（除分步耗时） |
| 持续 | 评测/MCP/可观测/知识源/研究任务 5 页保持静态演示并保留"演示数据"角标；语音/听写/对话搜索等占位维持 | 不为演示页发明接口 |

### 7.3 治理路径

- 本对接是 **Type C**：改变用户可见能力（演示页变真实数据）、触碰鉴权与外部调用，必须先立 OpenSpec change（proposal/design/tasks + spec delta），更新 `.ai/ACTIVE_TASK.md`，且 design.md 需含决策记录（§5 的三个模型错位正是天然的三条决策项）。
- 不修改 `rag-frontend` 任何文件；代码复用仅限"抄实现模式"（SSE 解析、单飞刷新、轮询参数），不是引入依赖。
- 与 C17（retrieval-quality-gate-activation）无契约交集，但**联调依赖其前置条件**（向量索引就绪）；不要并行推进。

### 7.4 联调前置 checklist

- [ ] MySQL/Redis/Milvus compose 栈可用；后端 8080 启动（`start-backend.ps1`）
- [ ] 管理员账号就绪（`AUTH_BOOTSTRAP_*` 引导或已有账号）
- [ ] 至少一个知识库已完成索引（`GET /statistics` 的 vectorCount > 0）
- [ ] LLM/embedding/rerank provider 配置确认，**外部调用量与费用已获用户授权**
- [ ] Swagger（`/swagger-ui.html`）可访问，作为联调期契约仲裁

---

## 8. 附录

### A. 端点速查（前端视角）

```
POST /auth/login            {username,password} → AuthResponse          (20/60s/IP)
POST /auth/refresh          {refreshToken}      → AuthResponse          (60/60s/IP, rotate)
POST /auth/logout           Bearer              → void

GET  /api/knowledge-bases                       → KnowledgeBaseDTO[]
POST /api/knowledge-bases   {name,description?,isPublic?} → KB        (201)
GET/PUT/DELETE /api/knowledge-bases/{id}
GET  /api/knowledge-bases/{id}/statistics       → {documentCount,vectorCount,queryCount}
POST /api/knowledge-bases/{id}/documents    multipart file[,title] → {documentId,taskId,...} (202)
GET  /api/knowledge-bases/{id}/documents        → Document[]
DELETE /api/knowledge-bases/{kbId}/documents/{docId}

GET  /api/tasks/{taskId}                        → {state,progress,message,...}
POST /api/tasks/{taskId}/cancel

POST /api/qa/ask             {kbId,question,topK?,minScore?,filter?,enableCache?} → QAResponse (30/60s)
POST /api/qa/ask/stream      同上 → SSE text delta + [DONE]/[ERROR]        (10/60s)
POST /api/qa/debug/retrieve  同上+enableRerank? → RetrievalDebugResponse   (60/60s)

GET  /api/history?page&size&kbId?               → PageResult<QAHistoryDTO>
GET/DELETE /api/history/{id}
POST /api/history/{id}/feedback {rating,comment?} → QAFeedbackDTO (201)
GET  /api/history/{id}/feedback · GET /api/history/feedback/my
```

### B. 关键字段映射（demo mock → 真实 DTO）

| demo mock | 真实来源 | 映射说明 |
| --- | --- | --- |
| `cites[]{file, kb, score, chunk, snippet}` | `Citation` | `file`→`sourceFileName`（兜底 `source`）；`kb` 无字段，用本次提问的库名；`score` 为 number 无需字符串化；`chunk`→`chunkId`；浮层可补 `documentTitle/documentId/startIndex/endIndex` |
| `retrieval.chunks[]{file, chunk, score}` | `RetrievedContext` | `file`→`source` 或 `metadata.sourceFileName`；`chunk`→`metadata.chunkIndex`；`score`→`relevanceScore`；展开可看 `content` 全文 |
| `retrieval.pipe{variants, topK, steps, topScore, avgScore, tokens, finalN}` | `RetrievalDebugResponse` | `variants/topScore/avgScore/contextCount` 真实；`steps` 分步耗时与 `tokens` 无数据源，保留演示标注 |
| `TASK_STAGES[10/30/50/70/85/100]` | `TaskStatusResponse.progress/message` | 已对齐，改为真实轮询驱动即可 |
| `FEEDBACKS[]{qaTitle, kb, rating, comment, time}` | `QAFeedbackDTO` + `QAHistoryDTO` | `rating/comment/createdAt` 直连；`qaTitle` 需 `GET /api/history/{qaId}` 回查；`kb` 无字段（历史记录有 `kbId`，可换算名称） |
| 会话 `CONVS[]{id, title, tag, turns[]}` | 无对应实体 | 纯前端聚合（见 §5.1） |
| `KBS[]{size, hits, activity[], visibility}` | `KnowledgeBaseDTO/Statistics` | size/hits/activity 无数据源；`isPublic` 可替代 visibility |
| 用户名 `admin` 硬编码 | `AuthResponse.userInfo` | `username`/`email` 渲染 |

### C. 可直接抄写的 rag-frontend 实现

| 模式 | 位置 |
| --- | --- |
| SSE fetch 解析（`slice(5)`、`[DONE]`、AbortController） | `rag-frontend/src/composables/useSSE.ts:88-109` |
| 401 单飞刷新 + 请求队列重放 | `rag-frontend/src/api/request.ts:24-97` |
| 任务轮询（2s、终态终止、异常即停） | `rag-frontend/src/composables/useTaskPolling.ts` |
| token 存储约定（`rag_` 前缀 key） | `rag-frontend/src/utils/storage.ts` |
