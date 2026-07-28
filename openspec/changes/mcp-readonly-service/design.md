# Design: C15 MCP Read-Only Service

## 1. 设计目标与约束

C15 在现有 `rag-admin` WebMVC 进程中增加一个默认关闭的只读 MCP server adapter。它只负责协议、认证身份桥接、Resource/Tool schema、结果映射与运行边界；知识库权限、tenant scope、retrieval、generation、citation、cache 和 provider failure semantics 继续由已接受的现有 service contract 负责。

本 change 的“只读”定义是：不改变知识库、文档、索引、用户、tenant、permission、QA history、query count、feedback 或 task 等业务/权威事实。tenant-scoped TTL cache、rate-limit/concurrency counter、已有隐私白名单内的 metrics/traces 与安全日志属于非权威技术写入，可以在明确配置和测试约束下发生。

规划必须同时守住四个事实：

1. C14 PASS 只解除 C15 的前置证据门禁，不等于 MCP 已可安全启用。
2. 当前 REST ask controller 有 query count/history 副作用，不能直接当只读 tool handler。
3. 当前 access JWT 含 userId/tenantId，但没有 MCP OAuth Protected Resource Metadata、resource audience 与 scope flow；首版只能声明部署型认证。
4. 当前 Spring Boot 3.2.1 与最新 MCP/Spring AI 依赖基线存在兼容性风险，必须先 spike，不能靠假设推进。

## 2. 当前事实与外部规范基线

### 2.1 仓库事实

- `rag-admin` 是 Spring Boot 3.2.1 WebMVC 启动与 REST 编排模块；`rag-auth` 已通过 Spring Security + JWT 建立无状态认证。
- `CurrentUserService.requireIdentity` 只从 `UserPrincipal` 构造正数 userId/tenantId 的 immutable `RequestIdentity`。
- `AuthorizationService`、`KnowledgeBaseService.requireReadyVectorScope`、tenant-aware `DocumentService` 与 C13b contract 已覆盖 KB/document/retrieval scope。
- `RAGService.ask(QARequest)` 本身负责 cache、retrieval、generation、citation 与 sanitized metadata，不写 query count/history；这些副作用目前位于 `QAController`。
- `QueryEngine.retrieveWithDiagnostics` 可提供 retrieval-only 内容与 reranker attribution；当前 debug DTO enrichment 仍在 controller 内。
- `DocumentChunk` 已持久化 tenantId、documentId、vectorId、content、chunkIndex 与 position，可支撑 citation/chunk Resource；original durable input storage key 不可暴露。
- 当前通用 `RateLimitInterceptor` 按 REST handler annotation 工作，不能直接区分同一 `/mcp` endpoint 内的不同 tool call。
- 代码库当前没有 MCP dependency、transport、endpoint、resource URI 或 tool schema。

### 2.2 规范与 SDK 调研基线

- MCP `2025-11-25` [Streamable HTTP transport](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports) 使用单一 HTTP endpoint；POST 可返回 JSON 或 SSE，GET 在不提供 server stream 时可以返回 405；session 是可选能力。规范还要求校验 `Origin`，本地 server 应优先只绑定 loopback，并应对每条连接做认证。
- MCP [Resources](https://modelcontextprotocol.io/specification/2025-11-25/server/resources) 支持 list/read/templates、custom URI 与分页；Resource URI 必须验证并在读取前重新做权限检查。
- MCP [Tools](https://modelcontextprotocol.io/specification/2025-11-25/server/tools) 使用 JSON Schema 2020-12 input/output、structured content 与 tool execution error；server 必须校验输入、权限、限流并清理输出。
- MCP [Authorization](https://modelcontextprotocol.io/specification/2025-11-25/basic/authorization) 对 HTTP authorization profile 定义 OAuth 2.1 resource server、Protected Resource Metadata、resource indicator/audience 与 token handling；本项目现有 JWT 不满足这套完整互操作 profile。
- 官方 MCP Java SDK [`v2.0.0`](https://github.com/modelcontextprotocol/java-sdk/releases/tag/v2.0.0) 对齐 `2025-11-25`，提供 Servlet/core、Jackson 2/3 与 Streamable HTTP；但其构建基线包含比本项目更新的 Reactor/Jackson/Servlet 版本，实际兼容性未知。
- 当前 [Spring AI 2.0](https://docs.spring.io/spring-ai/reference/getting-started.html) 面向 Spring Boot 4.0/4.1；Spring AI 1.1.x 面向 Boot 3.5.x。两者都不能在没有框架升级验证时直接作为本项目 Boot 3.2.1 starter。

这些网页事实只用于规划技术闸门，不等于本仓库已下载、编译或验证任何 MCP library。

## 3. 总体架构

```text
MCP Host
  -> POST /mcp
  -> local-only / Origin / request-size guard
  -> existing JwtAuthenticationFilter
  -> MCP Servlet transport (sessionless Streamable HTTP)
  -> McpRequestIdentityResolver
  -> McpResourceService / McpReadOnlyToolService
  -> existing AuthorizationService + tenant-aware services
  -> McpResultMapper (whitelist + size bounds)
  -> JSON-RPC result / tool execution error
```

规划中的 production package 位于 `rag-admin/src/main/java/com/enterprise/rag/admin/mcp/`，职责建议拆为：

| 组件 | 职责 |
|---|---|
| `McpProperties` | default-off、local-only、Origin、external tools、cache、limits/timeouts/rate config |
| `McpServerConfiguration` | 条件注册 transport、server capability、固定 resource/tool schema |
| `McpOriginAndExposureFilter` | `/mcp` 专用 Origin、local-only、request-size 守卫 |
| `McpRequestIdentityResolver` | 从当前 authenticated principal 构造 `RequestIdentity`，不读取 tool args |
| `McpResourceUri` | 严格解析/格式化 custom URI，拒绝 query/fragment/path trick |
| `McpKnowledgeResourceService` | resources list/read/templates、KB/document/chunk 权限与字段白名单 |
| `McpReadOnlyToolService` | 四个 tools 的输入归一、权限、只读编排和稳定 result/error |
| `McpCitationReader` | 按 tenant + KB + document + chunk identity 读取并校验 citation |
| `McpToolRateLimiter` | user + tenant + tool 维度速率/并发限制，复用现有 rate limiter |
| `McpResultMapper` | structured/TextContent 一致性、metadata whitelist、truncation、resource links |

不得让 SDK transport callback 直接调用 controller。controller 是 REST adapter，并包含 C15 禁止的副作用和 REST-specific response shape。

## 4. Transport 与依赖兼容闸门

### 4.1 首版 transport

- endpoint 固定 `/mcp`，只有 `rag.mcp.enabled=true` 时注册。
- 使用 sessionless Streamable HTTP：不返回 `MCP-Session-Id`，不维护 session store，不支持 DELETE session。
- POST 接受 MCP JSON-RPC 并返回 `application/json`；不主动建立 SSE stream。
- GET 返回 405；server 不声明 resource subscription/listChanged、tool listChanged、prompts、completion、sampling、elicitation 或 tasks。
- 每个 request 都重新通过 JWT filter；transport/session 标识不参与认证。
- server name 固定 `enterprise-rag-readonly`，capability version 固定 `c15-v1`，实际 negotiated protocol version进入安全 diagnostics，但不作为自由文本 metric label。

### 4.2 SDK compatibility spike

实现第一切片只允许做 dependency/transport spike：

1. 固定官方 Java SDK、JSON binding、spec 和 conformance suite 版本；不使用 floating/latest。
2. 先检查 dependency tree 中 Java、Spring Framework、Reactor、Jackson、Servlet、SLF4J 与现有 OTel/Milvus 依赖的实际解析版本。
3. 只使用官方 core/Servlet + Jackson 2 binding；不引入 Spring AI starter。
4. 运行 reactor compile、最小 initialize/ping/resources/list/tools/list transport tests 与现有核心 smoke。
5. 如果需要升级 Spring Boot/Spring Framework/Jackson/Reactor、排除关键 transitive 后仍有 LinkageError/NoSuchMethodError，或只能退回未批准的旧协议版本，C15 implementation hard-stop。

hard-stop 后应保持 change 规划材料，向用户报告 compatibility evidence，并另立 `mcp-runtime-foundation` 或等价 Type C change。不得在 C15 内手写 JSON-RPC/Streamable HTTP 代替官方 SDK。

## 5. 配置与默认值

规划默认值如下，用户批准后在 implementation tests 中冻结：

| 配置 | 默认 | 含义 |
|---|---:|---|
| `rag.mcp.enabled` | `false` | 不注册 MCP transport |
| `rag.mcp.external-tools-enabled` | `false` | 不暴露/执行可能调用 embedding/rerank/LLM 的 search/ask |
| `rag.mcp.cache-enabled` | `false` | MCP ask 默认不读写 QA cache；显式开启后仍只允许 tenant-scoped TTL cache |
| `rag.mcp.local-only` | `true` | 非 loopback request 在 transport 前拒绝 |
| `rag.mcp.allowed-origins` | 空 | `Origin` 缺失可用于非浏览器客户端；存在时必须 exact-match，禁止 `*` |
| `rag.mcp.resource-page-size` | `50` | 最大 `100`，超出拒绝或归一到上限 |
| `rag.mcp.max-query-chars` | `2000` | query/question 输入字符上限 |
| `rag.mcp.max-chunk-bytes` | `65536` | 单 Resource/chunk 最大 UTF-8 bytes |
| `rag.mcp.max-result-bytes` | `131072` | 单 tool result structured+text 的总上限 |
| `rag.mcp.search-timeout` | `30s` | retrieval tool 上限 |
| `rag.mcp.ask-timeout` | `120s` | generation tool 上限，仍受 provider 自身更小 timeout 约束 |
| `rag.mcp.read-timeout` | `5s` | resource/get-citation/compare 上限 |
| `rag.mcp.expensive-concurrency-per-user` | `2` | search+ask 同一 user 的同时执行上限 |

默认值不写入 `.env.local` 或 `application-dev.yml`；tracked `application.yml` 只能使用安全默认/环境占位，不包含 token、issuer、secret 或业务 endpoint 凭据。

## 6. Authentication、Authorization 与 Tenant 传播

### 6.1 首版部署型认证

- `/mcp` 继续由现有 `SecurityFilterChain.anyRequest().authenticated()` 保护。
- access token 只允许位于 `Authorization: Bearer` header；query、cookie、tool args、resource URI 不接受 token。
- `initialize`、resources/templates/list、resources/list/read、tools/list/call 全部需要认证，不提供匿名 capability discovery。
- token refresh 仍通过现有 REST auth endpoint 完成；MCP server 不处理 refresh token。
- C15 不新增 OAuth metadata/discovery/authorization server/scope；文档必须写明通用 MCP host 需要支持手工 Bearer header 配置。

### 6.2 Identity 与资源授权

每个 handler 的第一项业务动作是：

1. 从 Spring Security `Authentication` 取得 `UserPrincipal`。
2. 调用与 `CurrentUserService.requireIdentity` 等价的单一 resolver，得到 immutable `RequestIdentity`。
3. 对目标 KB 调用现有 read authorization，并解析 tenant-ready vector scope（仅 search/ask 需要 vector scope）。
4. 对 document/chunk 再以 `tenantId + documentId` 查询，并验证 document.kbId 等于已授权 KB。
5. foreign resource 与不存在资源使用同一 `MCP_RESOURCE_NOT_FOUND`，不得暴露 tenant、owner/public/permission 或存在性。

Tool input schema、Resource URI 和 cursor 都不含 tenantId。客户端传入任何 tenant/collection/provider/filter 等 unknown property，由 JSON Schema 或 URI parser 拒绝，不能“忽略后继续”。

## 7. Resources 设计

### 7.1 Capability

server 只声明基础 `resources` capability，不声明 `subscribe` 或 `listChanged`。

`resources/list` 只列出当前 identity 可访问的 KB 级 Resource，避免把所有 document/chunk 扁平展开：

```text
rag://knowledge-bases/{kbId}
```

结果按 `kbId` 稳定升序、默认每页 50、最大 100。cursor 是 opaque base64url 编码的 lastSeenKbId/version，不含 tenant/user 或资源名称；每次使用 cursor 都重新执行当前 identity 的 accessible query，跨用户复用 cursor 不产生越权结果。

### 7.2 Resource templates

固定暴露三种 template：

```text
rag://knowledge-bases/{kbId}
rag://knowledge-bases/{kbId}/documents/{documentId}
rag://knowledge-bases/{kbId}/documents/{documentId}/chunks/{chunkIndex}
```

URI parser 只接受小写 scheme/authority、十进制正数 kbId/documentId、非负 chunkIndex；拒绝 query、fragment、userinfo、port、空 segment、前导 `+/-`、percent-encoded slash、`..` 和多余路径。

### 7.3 Resource content

| Resource | MIME | 白名单内容 |
|---|---|---|
| KB | `application/json` | id、name、description、documentCount、isPublic、createdAt、updatedAt |
| Document | `application/json` | id、kbId、title、fileType、status、chunkCount、createdAt、updatedAt |
| Chunk | `text/plain; charset=utf-8` | content；超过上限返回 bounded prefix + `truncated=true` 的伴随 metadata |

禁止输出 tenantId、ownerId/uploaderId、vectorCollection/vectorId（除 tool citation identity 的 bounded `chunkId`）、filePath/storage key、input hash/size/state、contentHash、deleted/version、raw metadata 或内部 SQL/Redis/Milvus facts。

文档版本 URI（如 `/versions/{version}`）必须返回 not found/unsupported；乐观锁 version 不能冒充历史版本资源。

## 8. Tools 设计

### 8.1 共同规则

- 固定 tool names：`rag.search`、`rag.ask`、`rag.get-citation`、`rag.compare-sources`。
- input/output schema 使用 JSON Schema 2020-12，object 均 `additionalProperties=false`。
- tool annotation：`readOnlyHint=true`、`destructiveHint=false`、`idempotentHint=true`、`openWorldHint=false`；annotations 只是提示，不能替代 server enforcement。
- 不声明 task support；请求不会转为 MCP task 或 durable async job。
- success 同时返回 `structuredContent` 与相同 JSON 的 bounded TextContent；schema validation 必须在发送前完成。
- tool output 中的 source 尽量使用 ResourceLink，客户端无需猜私有 REST URL。

### 8.2 `rag.search`

输入：

```text
kbId: positive integer, required
query: non-blank string <= 2000 chars, required
topK: integer 1..20, default 5
minScore: number 0..1, default existing QA minimum
```

不接受 filter、tenantId、collectionName、enableRerank、provider、model、timeout、retry 或 cache 开关。handler 通过 KB read authorization + ready vector scope 构造 `RetrieveOptions`，调用 `QueryEngine.retrieveWithDiagnostics`。

输出：status、resultCount、items[rank, score, documentId, chunkId, documentTitle, sourceFileName, boundedExcerpt, resourceUri] 以及 rerank/retrieval diagnostics whitelist。不得输出 raw metadata、query variants、完整 context、provider raw body 或内部 collection。

### 8.3 `rag.ask`

输入与 search 相同，但字段为 `question`。handler 构造固定 non-streaming `QARequest`，cache 由 server config 决定，调用 `RAGService.ask`，再由共享 source enrichment service 映射 citations。

输出：status、answer、citations[documentId, chunkId, title, boundedSnippet, score, resourceUri]、no-answer/error category 与必要的 cache/retrieval/rerank/provider-call diagnostics whitelist。默认不返回完整 contexts。

handler 不调用 `KnowledgeBaseService.incrementQueryCount`、`QAHistoryService.save` 或 REST controller。现有 RAG failure/no-answer/citation semantics 不变。

### 8.4 `rag.get-citation`

输入：`kbId`、`documentId`、`chunkId`。其中 chunkId 对应持久化 chunk/vector identity，均有长度/字符上限。handler 先授权 KB，再 tenant-scoped 读取 document/chunks，要求 document.kbId 和 chunk identity exact match。

输出：document/chunk identity、title、positions、bounded content/snippet 与 exact chunk ResourceLink。该 tool 不执行 embedding、rerank 或 generation。

### 8.5 `rag.compare-sources`

输入为恰好两个 source reference，每个包含 `kbId`、`documentId`、`chunkId`。两个 source 可属于同 tenant 内不同、但当前用户分别有读权限的 KB；每个 reference 独立执行完整授权和 citation identity 校验。

输出：left/right 的 bounded metadata、excerpt、ResourceLink，以及 `sameKnowledgeBase`、`sameDocument`、`sameChunk` 和 `semanticComparisonStatus=NOT_PERFORMED`。不计算“谁更可信”、矛盾、蕴含、相似度或生成摘要，不调用 provider。

## 9. 只读副作用与外部调用矩阵

| Capability | 权威业务写入 | 允许技术写入 | 可能外调 |
|---|---|---|---|
| resources list/read | 无 | metrics/rate limit | 无 |
| `rag.get-citation` | 无 | metrics/rate limit | 无 |
| `rag.compare-sources` | 无 | metrics/rate limit | 无 |
| `rag.search` | 无 | embedding/rerank cache、metrics/rate limit | 取决于现有 embedding/rerank config |
| `rag.ask` | 无 history/count | tenant QA/embedding cache（仅显式启用）、metrics/rate limit | retrieval + generation，取决于现有 config |

`rag.mcp.external-tools-enabled=false` 时，`tools/list` 不暴露 `rag.search`/`rag.ask`，对应 call 也 fail closed 为 `MCP_EXTERNAL_TOOLS_DISABLED`；不能只从列表隐藏但仍可按名字调用。

启用 external tools 后，客户端仍不能指定 provider/model/retry。调用沿用 accepted provider contract；result diagnostics 只报告 requested/effective provider、model call count、fallback、timeout/retry 的安全白名单。开发验收使用 deterministic provider 且 calls=0；任何 live smoke 前必须另行披露预计样本/调用量、模型、query/context 数据出站、最大尝试、费用与限流风险并取得授权。

## 10. Security、限流与错误语义

### 10.1 Transport security

- `McpOriginAndExposureFilter` 在解析 JSON-RPC 前执行；`Origin` 存在时必须 exact-match allowlist，`*` 非法。
- `local-only=true` 时只接受 loopback peer；不信任未配置的 `X-Forwarded-For` 来绕过该检查。
- request body、header size 和 content type 使用现有 server 上限及 MCP-specific 更小上限；非 JSON/错误 Accept/MCP version 直接稳定拒绝。
- C15 不改变全局 CORS `*`，但明确它不是 `/mcp` Origin 安全证据。

### 10.2 Tool rate/concurrency

使用现有 `RateLimiter`，key 固定为不可逆的 `tenantId:userId:toolName` 组合（只进入 Redis key，不进入普通日志/metrics label）：

| Tool | 默认速率 |
|---|---:|
| `rag.search` | 60/60s |
| `rag.ask` | 30/60s |
| `rag.get-citation` | 120/60s |
| `rag.compare-sources` | 60/60s |

search+ask 共享 per-user concurrency=2。Redis rate-limit state 属 critical security control，读取/写入失败按现有 contract fail closed；不能因 Redis outage 无限制执行 provider calls。

### 10.3 Error mapping

transport/JSON-RPC 结构错误使用 protocol errors；通过 schema 但业务参数/权限/依赖失败使用 `isError=true` tool execution result。稳定 category 至少包括：

```text
MCP_AUTH_REQUIRED
MCP_INVALID_ARGUMENT
MCP_RESOURCE_NOT_FOUND
MCP_FORBIDDEN
MCP_RATE_LIMITED
MCP_RESULT_TOO_LARGE
MCP_TIMEOUT
MCP_DEPENDENCY_UNAVAILABLE
MCP_EXTERNAL_TOOLS_DISABLED
MCP_INTERNAL_ERROR
```

foreign resource 与不存在资源统一 `MCP_RESOURCE_NOT_FOUND`。error TextContent 只含稳定 category 和可操作的安全提示；不得包含 token、tenant/user ID、URI 内部解析值、SQL、Redis key/value、collection、query/question/content、provider response、异常 message/stack 或绝对路径。

## 11. 测试与验收设计

### 11.1 TDD 顺序

1. SDK compatibility/transport：先 RED 于 dependency/initialize/version/capability，再完成最小 server。
2. identity/Origin：missing/expired JWT、invalid Origin、client tenant selectors 先 RED。
3. Resources：URI parser、pagination、whitelist、foreign/nonexistent、version boundary 先 RED。
4. Tools：四个 schema/result/error、external visibility 与 input bounds 先 RED。
5. Side effects：history/count/KB/document/task/feedback 前后快照先 RED，再接 read-only facade。
6. Dual-tenant/infrastructure：使用 C14 可复用 fixture pattern 证明所有 read/call scope。
7. Conformance/client smoke：固定 tool 版本和 client identity，生成可重复互操作 evidence。

### 11.2 聚焦测试

建议新增：

- `McpServerConfigurationTest`
- `McpOriginAndExposureFilterTest`
- `McpResourceUriTest`
- `McpKnowledgeResourceServiceTest`
- `McpReadOnlyToolServiceTest`
- `McpToolRateLimiterTest`
- `McpProtocolMvcTest`
- `C15McpReadOnlyIT`（MySQL/Redis/Milvus + deterministic provider）

`C15McpReadOnlyIT` 必须至少证明：

- A/B tenant 同 ID pattern 下 resources/tools 不泄漏 foreign content/metadata/存在性。
- 同 tenant owner/reader/public/permission 与现有 REST read semantics 一致。
- `rag.ask` success/no-result/error 前后 QA history、query count、KB/document/task/feedback 权威状态不变。
- cache disabled 时 QA cache 无写入；显式 enabled 时只有当前 tenant scoped TTL key 可写。
- get-citation/compare provider calls=0；deterministic search/ask evidence 不产生真实 provider/data egress。
- rate-limit、timeout、result-too-large、dependency failure 均 fail closed 且错误脱敏。

### 11.3 协议互操作

- 固定 SDK 与 MCP spec identity。
- 固定版本运行官方 conformance suite；该工具可能通过 `npx` 下载，执行前需用户批准网络/工具下载。
- 至少一个独立 MCP client 完成 initialize、resources/templates/list、resources/list/read、tools/list、四个 tools 和错误路径 smoke。
- conformance/client smoke 使用 synthetic data 与 deterministic provider，不连接真实业务 DB/Redis/Milvus/provider。
- 若 conformance 工具不可用或未授权，change 保持未完成，不能用 Mockito/MockMvc 冒充标准互操作通过。

## 12. Rollout 与回滚

- 默认配置关闭，合入代码不改变现有 REST surface 或 provider 调用。
- 首次只允许本机 synthetic profile 启用；远程暴露、TLS/proxy、OAuth profile、registry 发布另立 change。
- 回滚首选把 `rag.mcp.enabled=false`，使 transport bean 不注册；不需要数据库 rollback。
- 若 SDK 造成 runtime regression，可回退 C15 dependency/adapter 文件；现有 core/auth/REST schema 不应依赖 MCP types。
- implementation 不得让 `rag-core` 或 `rag-auth` 反向依赖 `rag-admin.mcp`，避免删除 adapter 时破坏核心。

## 13. 决策记录

### 决策 1：C15 与 Router 是否合并
- **面临的选择**：把 MCP 与 C16 Router 一次实现、只做 MCP change、先 Router 再 MCP。
- **选了哪个 + 为什么**：选择只做 MCP change，因为蓝图明确两者独立，MCP 的协议/认证/只读风险不依赖路由策略。
- **放弃的代价**：合并会把协议与策略评测绑成大爆炸变更；先 Router 会无端阻塞已满足前置门禁的 MCP。

### 决策 2：使用官方 SDK、Spring AI starter 还是手写协议
- **面临的选择**：官方 Java SDK core/Servlet、Spring AI MCP starter、手写 JSON-RPC/Streamable HTTP。
- **选了哪个 + 为什么**：选择官方 Java SDK direct integration，并先做兼容 spike；它最接近规范且不要求立即采用完整 Spring AI 栈。
- **放弃的代价**：Spring AI starter 当前要求更高 Boot 基线；手写协议容易漏 lifecycle、version、error、Origin 与 conformance 细节。

### 决策 3：SDK 不兼容时怎么处理
- **面临的选择**：在 C15 升级 Spring 基线、排除/强压冲突继续、停止并另立 runtime foundation change。
- **选了哪个 + 为什么**：选择 hard-stop 后另立 change，因为框架升级是独立高风险 Type C，不能藏在 MCP feature 内。
- **放弃的代价**：直接升级扩大回归面；强压版本可能编译通过却在运行时产生 linkage/serialization 错误。

### 决策 4：首版使用哪种 transport
- **面临的选择**：stdio、旧 HTTP+SSE、stateful Streamable HTTP、sessionless Streamable HTTP。
- **选了哪个 + 为什么**：选择 sessionless Streamable HTTP，因为现有应用已是认证 WebMVC 服务，且首版固定 Resources/Tools 不需要 server push/session。
- **放弃的代价**：stdio 难复用现有 HTTP JWT；旧 transport 已淘汰；stateful transport增加 session hijack、身份绑定和恢复状态。

### 决策 5：MCP 放在现有进程还是独立 sidecar
- **面临的选择**：`rag-admin` 内嵌 adapter、独立 Java sidecar、Node/TypeScript sidecar。
- **选了哪个 + 为什么**：选择 `rag-admin` 内嵌 adapter，因为可以直接复用服务端 identity、权限与 tenant-aware services，避免 token/数据再次跨进程传递。
- **放弃的代价**：独立 sidecar 需要新增部署和内部 API；Node sidecar 还会引入第二运行时与重复契约。

### 决策 6：首版默认如何暴露
- **面临的选择**：默认公网可用、默认同网段可用、default-off 且 local-only。
- **选了哪个 + 为什么**：选择 default-off + local-only，因项目仍是工程原型，远程 TLS/proxy/OAuth/容量事实均未知。
- **放弃的代价**：默认远程暴露会在认证互操作、Origin、TLS 和限流未验证时增加攻击面。

### 决策 7：认证使用现有 JWT 还是完整 MCP OAuth
- **面临的选择**：沿用现有 access JWT、在 C15 内实现完整 MCP OAuth profile、暂时匿名本机使用。
- **选了哪个 + 为什么**：选择现有 JWT 的部署型认证并明确非 OAuth-conformant，因为它已有 server-issued tenant identity，完整 OAuth 需要新的 issuer/audience/scope 契约。
- **放弃的代价**：完整 OAuth 会把 authorization server 和 token lifecycle 扩入 C15；匿名模式无法满足企业知识资源边界。

### 决策 8：tenant scope 从哪里来
- **面临的选择**：tool args/resource URI 携带 tenantId、MCP session 保存 tenant、每个 request 从 principal 构造 `RequestIdentity`。
- **选了哪个 + 为什么**：选择每个 request 从 principal 构造 identity，和 C13 accepted contract 一致且 sessionless 不会残留跨请求状态。
- **放弃的代价**：客户端 tenant selector 可被伪造；session tenant 可能与后续 token/user 漂移并形成越权。

### 决策 9：Resource 列表的粒度
- **面临的选择**：扁平列出所有 chunks、列出 KB 并用 document/chunk templates、只提供一个静态 catalog。
- **选了哪个 + 为什么**：选择 KB list + document/chunk templates，在可发现性和列表规模之间平衡，并让 search/citation 返回精确 ResourceLink。
- **放弃的代价**：扁平 chunks 会爆炸且泄露过量 metadata；单一 catalog 让标准 resource navigation 过弱。

### 决策 10：是否提供文档版本 Resource
- **面临的选择**：把乐观锁 version 当历史版本、临时从 durable input 生成版本、明确 out_of_scope。
- **选了哪个 + 为什么**：选择 out_of_scope，因为当前没有 document-version 领域模型或不可变历史内容。
- **放弃的代价**：伪装 version 会返回不可复现内容；读取 durable input 会暴露 storage/lifecycle 并扩大解析成本。

### 决策 11：Tool input 是否接受任意 filter/provider 参数
- **面临的选择**：透传 REST filter/provider、只过滤 reserved keys、首版完全不接收 filter/provider。
- **选了哪个 + 为什么**：选择首版不接收，避免新的 scope injection 与客户端控制外调/成本，先固定最小可审计 schema。
- **放弃的代价**：直接透传会绕过 server-owned scope；只过滤已知 key 容易漏 alias/嵌套变体。

### 决策 12：MCP ask 如何复用现有能力
- **面临的选择**：调用 `QAController.ask`、复制一套 RAG、建立 read-only facade 调用现有 `RAGService`。
- **选了哪个 + 为什么**：选择 read-only facade，因为它复用现有 RAG contract，同时隔离 controller 的 query count/history 副作用。
- **放弃的代价**：调用 controller 会违反只读语义；复制 RAG 会造成 prompt/citation/failure contract 漂移。

### 决策 13：`compare-sources` 是否使用 LLM
- **面临的选择**：LLM 生成语义比较、启发式相似度/矛盾分数、确定性并排返回两条已验证来源。
- **选了哪个 + 为什么**：选择确定性并排返回，因当前没有 comparison prompt/evaluation/no-answer 契约，C15 不应制造未经评测的新结论。
- **放弃的代价**：LLM 比较扩大 provider/质量风险；启发式分数容易被误读为语义真值。

### 决策 14：只读是否允许 cache/metrics/rate-limit 写入
- **面临的选择**：绝对零写入、允许所有现有副作用、只允许受约束非权威技术写入。
- **选了哪个 + 为什么**：选择第三项，和冻结蓝图一致，并保持 cache/observability/security controls 可用而不改变业务事实。
- **放弃的代价**：绝对零写入会禁用安全限流与可观测性；允许全部副作用会把 history/count 偷渡进只读接口。

### 决策 15：external tools 如何启用
- **面临的选择**：MCP 开启即启用 search/ask、由客户端参数选择、独立 default-off server config。
- **选了哪个 + 为什么**：选择独立 default-off config，使协议暴露与 provider/data-egress 授权分离，客户端不能扩大预算。
- **放弃的代价**：自动启用会产生意外调用；客户端选择会把费用、模型与出站控制交给不可信输入。

### 决策 16：Tool result 返回完整上下文还是 bounded schema
- **面临的选择**：原样返回 QAResponse/metadata、只返回文本、structured schema + bounded text/resource links。
- **选了哪个 + 为什么**：选择 structured schema + bounded text/resource links，兼顾客户端兼容、可验证字段与最小披露。
- **放弃的代价**：原样返回会泄露内部 metadata/超大 contexts；纯文本难校验 citation identity 和自动化消费。

### 决策 17：错误如何表达
- **面临的选择**：所有失败都用 JSON-RPC protocol error、所有失败都 HTTP 200 文本、结构错误与业务执行错误分层。
- **选了哪个 + 为什么**：选择分层，符合 MCP 当前 tool error 语义，也让模型能修正参数而不把服务故障伪装成成功。
- **放弃的代价**：全 protocol error 难区分可修正业务参数；全成功文本会破坏机器可判定状态与监控。

### 决策 18：什么证据才算 MCP 互操作完成
- **面临的选择**：只跑 unit/MockMvc、只用一个手工 client、固定 conformance suite + 独立 client + 双 tenant side-effect evidence。
- **选了哪个 + 为什么**：选择第三项，因为“标准 MCP”必须同时证明 wire interop、身份隔离和真实只读副作用。
- **放弃的代价**：unit tests 不能证明协议；单一 client smoke 可能只验证该客户端的宽松兼容行为。
