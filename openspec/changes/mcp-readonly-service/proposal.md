# Proposal: C15 MCP Read-Only Service

## Why

项目已经有受 Spring Security 保护的 REST + Swagger 接口，也有 tenant-aware 的知识库、文档、检索、生成与 citation 链路；准确缺口不是“只能从前端访问”，而是没有标准 MCP server capability，外部 MCP host 无法通过统一的 Resources/Tools 协议发现和调用这些只读知识能力。

C13a/C13b 已把服务端 tenant identity 与数据面 enforcement 接通，C14 的固定双 tenant adversarial release 也已形成 26/26、functional/content/error/timing 四通道与 global `PASS` 的正式 evidence。因此 C14 前置门禁已经解除，项目可以建立独立 C15 Type C change；但 C14 PASS 不会自动开放 MCP，C15 仍需自己的协议、认证、只读副作用、限流、外调和互操作契约。

现有 `/api/qa/ask` 不能直接包装成“只读 MCP tool”：controller 会增加 query count，并在成功时写 QA history；流式入口也会计数和保存历史。C15 必须复用 tenant-aware RAG 内核，同时建立一条明确不写 history/count 的只读编排路径。另一方面，当前项目是 Spring Boot 3.2.1，而当前 Spring AI MCP starter 与官方 Java SDK 的版本基线已经前移；在没有真实依赖收敛、编译和协议测试前，不能假定任一 starter 可以直接接入。

## 用户故事：改前坏事 → 改后不同

- 改前坏事：MCP host 只能自行拼装项目私有 REST 调用，无法标准化发现可访问知识库、读取引用或调用检索/问答工具。
- 改后不同：启用 C15 后，认证用户可通过单一 MCP endpoint 发现 tenant 内有权限的 Resources，并调用固定、带 JSON Schema 的 `rag.search`、`rag.ask`、`rag.get-citation`、`rag.compare-sources`。
- 改前坏事：若直接复用 REST ask controller，每次 MCP 问答都会悄悄增加查询计数并写入 QA history，“只读”只停留在工具名字上。
- 改后不同：MCP ask 复用现有检索/生成/citation contract，但不会写 QA history 或 query count；只允许 tenant-scoped cache、rate-limit、metrics/traces 等受约束的非权威技术写入。
- 改前坏事：客户端可能把 tenantId、collectionName 或任意 metadata filter 塞进 tool arguments，尝试覆盖服务端 scope。
- 改后不同：MCP schema 不接收 tenant selector、collection selector 或任意 filter；每次 resource/tool 调用都从已认证 principal 构造 immutable `RequestIdentity` 并重新做 KB/document 权限检查。
- 改前坏事：为了“快点接 MCP”可能升级整个 Spring 基线、手写不完整 JSON-RPC，或把旧协议版本包装成当前互操作能力。
- 改后不同：实现先过官方 Java SDK 与当前 Spring Boot 3.2.1 的兼容性闸门；若不能在不升级框架的前提下通过，则停止 C15 实现并另立 runtime foundation change，不静默降级或自制协议。

## Goals

1. 建立默认关闭的 MCP server capability，目标协议为 MCP `2025-11-25`，首版使用 sessionless Streamable HTTP、单一 `/mcp` endpoint、同步 JSON 响应，不声明 SSE、session、notification 或 subscription。
2. 优先使用官方 MCP Java SDK 的 Servlet/core 能力与 Jackson 2 binding；在任何业务实现前完成 Maven dependency convergence、compile、transport smoke 与协议版本协商闸门。
3. 复用现有 Spring Security access JWT，在每个 HTTP request 上重新认证；从 `UserPrincipal` 构造 `RequestIdentity(userId, tenantId)`，客户端不能提交或覆盖 tenant scope。
4. 暴露 tenant/user 权限过滤后的知识库、文档 metadata 与 chunk text Resources；使用稳定 custom URI、分页、字段白名单和长度上限。
5. 暴露四个固定 Tools：`rag.search`、`rag.ask`、`rag.get-citation`、`rag.compare-sources`；input/output schema 使用 JSON Schema 2020-12、`additionalProperties=false` 和明确上限。
6. 为 MCP ask 建立无 QA history、无 query count 的只读编排；禁止创建、更新、删除 KB/document/index/user/tenant/permission/history/feedback/task。
7. 首版 `rag.compare-sources` 只验证并并排返回两条授权 citation/chunk，不调用 LLM、不输出服务端语义裁决；语义综合另立后续 change。
8. 对可能触发 embedding/rerank/generation 的 `rag.search` 与 `rag.ask` 使用独立默认关闭的 external-tools 开关、现有 provider retry/fallback contract 和实际调用归因。
9. 为 MCP endpoint/tool 增加 Origin 校验、本机优先暴露、用户+工具限流、超时、结果大小、输出字段白名单与稳定错误分类。
10. 通过 deterministic provider、双 tenant Testcontainers、HTTP/protocol integration 与固定版本 conformance suite 形成可重复证据；真实 provider smoke 继续单独披露与授权。

## Non-Goals

- 不实现 C16 Router、multi-hop/global/high-risk 策略、client-side agent loop 或 Agentic RAG 平台。
- 不提供写工具，不创建/更新/删除知识库、文档、索引、用户、tenant、permission、history、feedback 或 task。
- 不提供 prompts、sampling、elicitation、completion、roots、MCP tasks、resource subscription、list-changed notification、SSE server push 或 session resume。
- 不提供 stdio transport、旧 HTTP+SSE transport、公共 MCP registry 发布、互联网部署、反向代理/TLS/HA/SLA 或生产容量证明。
- 不新增 tenant CRUD/switch/membership、第二业务 tenant、跨租户管理员或新的 RBAC/scope 模型。
- 不为 Qdrant/Elasticsearch 补 tenant adapter；它们在 enforcement mode 下继续 fail startup，Milvus/C14 evidence 不外推。
- 不执行真实 Milvus shadow copy/mapping/readiness switch，不修改既有 migration 或持久化 schema。
- 不提供“文档版本” Resource；当前只有乐观锁 version，没有文档版本历史领域模型。
- 不把 original durable input bytes、storage key、vector collection、tenantId、ownerId、token、prompt、raw provider body 或内部异常暴露为 Resource/Tool output。
- 不在 C15 内升级 Spring Boot/Spring Framework/Jackson/Reactor，不引入 Spring AI starter；如官方 SDK 不能兼容当前基线，另立前置 Type C change。
- 不在首版实现 MCP OAuth 2.1 authorization server、Protected Resource Metadata、dynamic client registration、resource indicator/audience token 或 scope step-up；现有 JWT 路线只作为部署型认证，不能宣称完整 MCP authorization profile 兼容。
- 不改变默认 embedding/rerank/LLM provider、prompt、citation/no-answer、评测指标或 active quality gate。
- 不修改 `.env.local`、`application-dev.yml`、`.agents/` 或 `docs/学习文档/`。

## Capability Classification

- `confirmed`：C14 已验收归档且 global `PASS`；当前为 Java 17 + Spring Boot 3.2.1 WebMVC；现有 JWT principal 含 server-issued userId/tenantId；`RequestIdentity`、KB/document authorization、tenant-ready vector scope、QueryEngine/RAGService、citation、tenant-scoped cache 与 rate limiter 均已有可复用入口。
- `partial`：REST 已有 KB/document/read、debug retrieval、sync/stream ask，但 presentation/orchestration 聚集在 controller；source enrichment 仍是 controller 私有逻辑，REST ask 会写 query count/history，现有限流 interceptor 也不能按 MCP tool 名单独计数。
- `planned`：default-off sessionless Streamable HTTP、官方 SDK compatibility gate、MCP identity bridge、Resources/Tools schema、read-only ask facade、citation reader、deterministic source pair、tool-level limits、sanitized errors、dual-tenant integration 与 conformance evidence。
- `out_of_scope`：写工具、Router、MCP OAuth profile、Spring 基线升级、stdio/SSE/session/notification、文档版本、真实 tenant rollout、真实 Milvus maintenance、未验证 vector adapters、生产部署与 registry 发布。
- `unknown`：官方 Java SDK 2.x 与当前 Boot 3.2.1/Jackson/Reactor/Servlet 的实际可共存性、目标 MCP host 的固定产品矩阵、未来 authorization server/issuer、真实反向代理与 Origin policy、生产并发/结果大小、live provider 调用量与费用。

## Proposed Contract

1. `rag.mcp.enabled` 默认 MUST 为 `false`；关闭时不得注册可调用 `/mcp` transport 或对外宣称 MCP 已启用。
2. 启用时首版只声明 sessionless Streamable HTTP、Resources 与 Tools；GET MAY 返回 405，server 不分配 `MCP-Session-Id`，不声明未实现 capability。
3. 服务端只有在官方 SDK 与当前依赖基线兼容、协议协商和固定 conformance evidence 通过后，才可声明支持目标 MCP 版本。dependency conflict 不得通过升级 Spring 基线、手写协议或静默降级掩盖。
4. 每个 MCP HTTP request 必须携带现有项目 access JWT；Spring Security 验证后从 principal 构造 `RequestIdentity`。tenantId、collectionName、ownerId、provider/model 或任意 reserved scope 字段不得进入 tool input/resource URI。
5. Resources 只返回当前 identity 可访问的 KB、document metadata 与 chunk content；所有 URI 先严格解析，再按 tenant + KB + document 权限查询。foreign resource 与 matched nonexistent 使用同一 not-found 边界。
6. 首版 Resource URI 固定为 `rag://knowledge-bases/{kbId}`、`rag://knowledge-bases/{kbId}/documents/{documentId}`、`rag://knowledge-bases/{kbId}/documents/{documentId}/chunks/{chunkIndex}`；资源列表分页，字段与内容长度有白名单/上限。
7. `rag.search` 与 `rag.ask` 只接受 kbId、query/question 和受限 topK/minScore；不接受任意 metadata filter。`rag.get-citation` 与 `rag.compare-sources` 只接受可验证的 document/chunk identity。
8. Tools 同时提供 structured content、output schema 和兼容 TextContent；标记 `readOnlyHint=true` 只作为提示，真实只读性由 service boundary、权限检查和 side-effect tests 强制保证。
9. MCP ask 不写 QA history、不增加 query count；所有 MCP tool 不写业务/领域事实。允许的非权威写入仅限 tenant-scoped TTL cache、rate-limit counters、现有隐私白名单内的 metrics/traces 与安全日志。
10. `rag.compare-sources` 首版只返回两条分别通过授权和 citation identity 校验的 bounded excerpts/metadata/resource links；不调用 LLM、不输出 faithful/contradiction/equivalence 等语义判断。
11. `rag.search`/`rag.ask` 的外部能力使用独立默认关闭开关。真实执行必须沿用当前 provider timeout/retry/fallback，返回实际 provider/model-call attribution，客户端不得覆盖 provider、模型、重试或数据出站策略。
12. MCP endpoint 必须校验 Origin、默认限制本机调用，并对 tool 进行 user+tenant+tool 维度限流；input、output、timeout 和并发均有固定上限，错误和日志不得回显内容、token、tenant、内部 ID 映射或 provider raw body。
13. C15 acceptance 必须使用双 tenant synthetic fixture、deterministic embedding/generation、provider calls=0 的主证据，验证 resources/tools、foreign/nonexistent、history/count 不变、允许技术写入和所有禁止 side effects。
14. 现有 JWT 认证不等于 MCP OAuth authorization profile；C15 文档、报告和对外描述必须明确该互操作限制。未来 OAuth discovery/audience/scope 需要独立 Type C change。

## Impact

- 规划中的 production 改动主要位于 `rag-admin`：MCP configuration/transport/security bridge、resource/tool adapter、read-only facade、result mapper 与 focused tests。
- `rag-core` 预计不改现有 RAG 语义；如需复用 source enrichment 或只读 orchestration，只抽取 contract-preserving service，不改 prompt/citation/no-answer。
- `rag-common` 可能只复用或小幅扩展现有限流 abstraction；不得改变 REST 现有限流语义。
- `pom.xml` / `rag-admin/pom.xml` 只有在事前批准且 compatibility spike 通过后才加入固定版本官方 MCP SDK；不引入 Spring AI starter 或框架升级。
- 无数据库 migration、前端或现有 REST DTO 变更；若实现证明这些假设不成立，暂停并回到事前闸门。
- 规划阶段只新增 OpenSpec artifacts、更新 `.ai/ACTIVE_TASK.md` 并追加 `.ai/AGENT_LOG.md`。

## Risks And Mitigations

- 风险：官方 MCP Java SDK 当前依赖基线与 Boot 3.2.1 不兼容。缓解：第一实现切片只做 pinned dependency/Servlet/Jackson2/conformance smoke；不通过即停止，不在 C15 偷渡框架升级或手写协议。
- 风险：沿用项目 JWT 被误写成标准 MCP OAuth。缓解：将其定义为部署型认证；所有文档/证据明确 OAuth discovery、audience/resource indicator 与 scopes 尚未实现。
- 风险：直接复用 controller 导致 history/count 或其他副作用。缓解：MCP adapter 只调用独立 read-only facade 和底层 tenant-aware services，并用前后 SQL/Redis 快照锁定权威副作用为 0。
- 风险：Resource/tool 参数成为新的 tenant/filter 注入面。缓解：schema `additionalProperties=false`，不接收 tenant/collection/provider/filter；URI parser 使用有限 grammar，并在每次 read/call 重新授权。
- 风险：`compare-sources` 变成未经评测的新 prompt。缓解：首版只做 deterministic pair fetch，不进行 LLM synthesis；语义比较另立 change。
- 风险：MCP host 自动调用 search/ask 造成数据出站或费用。缓解：external tools 独立默认关闭；真实 smoke 前披露 tool、样本数、provider/model、最大尝试、数据内容、费用/限流并取得授权。
- 风险：返回完整 chunks/contexts 造成过量暴露。缓解：字段白名单、topK、单项/总字节上限、truncated 标志与 resource links；禁止 raw metadata、storage/vector/internal fields。
- 风险：session 或 SSE 引入身份绑定/恢复复杂度。缓解：首版 sessionless、无 server push，GET 返回 405；以后需要 session/notification 时另立 change。
- 风险：现有通用 CORS `*` 被误当作 MCP Origin 防护。缓解：为 `/mcp` 建立独立 Origin allowlist/local-only guard，不复用宽松 CORS 作为安全证据。

## Acceptance Evidence

- proposal、design 的 18 条决策、tasks 与 `rag-system` spec delta 先经用户批准。
- compatibility spike 固定 SDK/spec/conformance 版本，记录 dependency tree、Java/Spring/Jackson/Reactor/Servlet identity；不升级 Spring 基线且 compile/tests 通过。
- disabled/enabled transport tests 证明默认无 `/mcp`、启用后 initialize/version negotiation、Resources/Tools capability 与 GET 405/sessionless 边界正确。
- security tests 覆盖 missing/expired token、Origin、malformed URI/schema、unknown tool、client tenant/filter/provider injection、foreign/nonexistent fingerprint 与日志/响应脱敏。
- resource tests 覆盖 accessible KB pagination、KB/document/chunk read、字段白名单、content limit、version Resource 不存在与双 tenant隔离。
- tool tests 覆盖四个固定 schema、read-only annotations、structured/text result consistency、topK/minScore/size/timeout/rate-limit 和稳定 error taxonomy。
- MySQL/Redis/Milvus synthetic integration 验证 `rag.ask` 前后 QA history/query count/KB/document/task/feedback 不变；tenant-scoped cache/rate-limit/metrics 等允许技术写入不扩大 tenant scope。
- deterministic provider 路径验证 search/ask 的 provider-call attribution 与失败/fallback；正式主证据 provider/model calls=0、business data outbound=false。
- 固定版本 MCP conformance suite 与至少一个独立 client smoke 通过；若工具不可用或下载未授权，C15 不得以 unit tests 代替互操作结论。
- 运行 focused tests、`mvn -q test`、Python unit tests、SensitiveLogs、Markdown links、protected paths、dependency drift 与 `git diff --check`；前端无改动时正式 build 记为 `SKIPPED`。

## Approval Gate

本轮只批准启动 C15 规划，不代表批准加入/下载 MCP SDK、升级任何依赖、编写 Java 实现、启动 Testcontainers/conformance、开放 `/mcp`、执行真实 provider 调用或部署。用户需重点确认：

1. 首版是 sessionless Streamable HTTP、default-off/local-only，不做 stdio/SSE/session/notification。
2. 首版复用现有 access JWT，但明确不宣称 MCP OAuth authorization profile 兼容。
3. 首版 `rag.compare-sources` 是 deterministic side-by-side，不新增 LLM synthesis。
4. SDK 与 Boot 3.2.1 不兼容时停止 C15，另立 runtime foundation change。
5. external tools 与真实 provider smoke 继续单独披露和授权。

提交责任保持 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
