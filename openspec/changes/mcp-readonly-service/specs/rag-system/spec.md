## ADDED Requirements

### Requirement: Default-Off MCP Protocol And Compatibility Gate

系统 SHALL 提供一个默认关闭的只读 MCP server capability。`rag.mcp.enabled=false` 时，应用 MUST NOT 注册可调用的 MCP endpoint、Resources 或 Tools，也 MUST NOT 把代码存在描述为 MCP 已启用。

C15 首版启用时 SHALL 使用单一 `/mcp` 的 sessionless Streamable HTTP，目标协议版本为 `2025-11-25`。server MUST NOT 分配 `MCP-Session-Id`，MUST NOT 声明或模拟 SSE server push、session resume、resource subscription/list-changed、tool list-changed、prompts、sampling、elicitation、completion 或 MCP tasks；不提供 server stream 的 GET MAY 返回 405。

系统只有在固定版本官方 MCP SDK 与当前 Java/Spring/Jackson/Reactor/Servlet 基线兼容、协议协商和固定 conformance evidence 通过后，才 MAY 声明支持目标 MCP 版本。若兼容性需要升级 Spring 基线、压制关键 dependency conflict、手写协议或静默退回未批准的旧 spec，C15 implementation MUST fail closed 并返回独立 runtime foundation 事前闸门。

#### Scenario: 默认关闭

- GIVEN 应用使用默认配置启动
- WHEN 客户端请求 `/mcp` 或尝试发现 MCP capabilities
- THEN MCP transport 不注册或返回稳定 disabled/not-found 结果
- AND 不产生 provider 调用、session、Resource/Tool 列表或业务副作用

#### Scenario: 显式启用 Sessionless Streamable HTTP

- GIVEN `rag.mcp.enabled=true` 且 compatibility gate 已通过
- WHEN 已认证 client 完成 initialize 并请求 server capabilities
- THEN server 只声明 Resources 与当前配置允许的 Tools
- AND POST 使用标准 JSON-RPC/Streamable HTTP，GET 无 server stream 时返回 405
- AND 不返回 `MCP-Session-Id`，不声明未实现能力

#### Scenario: SDK 与当前基线不兼容

- GIVEN 固定官方 SDK 导致 dependency convergence、compile、transport smoke 或 runtime linkage/serialization 失败
- WHEN C15 尝试进入业务 Resource/Tool 实现
- THEN 实现 hard-stop，记录实际依赖与失败类别
- AND 不升级 Spring/Jackson/Reactor、不手写协议、不把旧 spec 或 unit-only 结果包装成通过

### Requirement: Authenticated Tenant-Scoped MCP Boundary

每个 MCP HTTP request SHALL 由现有 Spring Security access JWT 重新认证。服务端 MUST 只从 authenticated `UserPrincipal` 或等价服务端 principal 构造 immutable `RequestIdentity(userId, tenantId)`；token、tenantId、collectionName、ownerId、provider/model 或 reserved scope MUST NOT 从 query、cookie、tool arguments、Resource URI、cursor 或客户端 metadata 取得。

initialize、resources/templates/list、resources/list/read、tools/list/call 都 MUST 要求认证并执行当前 user/tenant 的权限检查。foreign resource 与 matched nonexistent control SHALL 使用相同 not-found 边界，不得泄露 foreign tenant、owner/public/permission、资源存在性、内容或状态，也不得修改 foreign state。

C15 首版复用现有 access JWT 只构成部署型认证；系统 MUST NOT 宣称已经实现 MCP OAuth authorization profile、Protected Resource Metadata、authorization server discovery、resource indicator/audience binding、dynamic client registration 或 scope step-up。

#### Scenario: 同 Tenant 已认证访问

- GIVEN access JWT 对应 tenant A 的有效用户，且用户对 tenant A 的 KB 有 read 权限
- WHEN client 列出/读取 Resources 或调用任一允许 Tool
- THEN server 从 principal 构造 tenant A 的 `RequestIdentity`
- AND 每层使用 tenant A + 当前用户权限执行 lookup/retrieval
- AND client 不需要也不能提交 tenant selector

#### Scenario: 缺失或无效认证

- GIVEN request 缺少 access token，或 token 过期、签名无效、缺 userId/tenantId
- WHEN client 请求 initialize、Resources 或 Tools
- THEN request 在业务 handler 前返回稳定 unauthorized
- AND 不返回 capability/resource/tool 内容，不回显 token/claim，不产生 provider 或业务调用

#### Scenario: 客户端伪造 Tenant 或猜测 Foreign ID

- GIVEN tenant A 用户知道 tenant B 的 kbId、documentId、chunk identity 或 citation reference
- WHEN client 在 header/query/body/URI/extra tool property 中伪造 tenant B 或直接使用 foreign ID
- THEN server identity 仍只能是 tenant A，unknown/reserved property fail closed
- AND foreign 与 matched nonexistent 具有相同稳定 not-found 边界
- AND tenant B 内容、metadata、存在性与状态均不可见且不变

#### Scenario: Authorization Profile 声明边界

- GIVEN `/mcp` 使用现有项目 JWT，但没有 OAuth Protected Resource Metadata、MCP audience/resource indicator 或 scopes
- WHEN 文档、报告或 server metadata 描述认证能力
- THEN 只声明 deployment-specific Bearer authentication
- AND 明确通用 client 需要手工 header 支持
- AND 不使用 OAuth-compliant、production authorization 或完整 MCP auth 等表述

### Requirement: Tenant-Filtered Read-Only Knowledge Resources

server SHALL 暴露 authenticated、tenant/user permission-filtered 的知识库 Resources。`resources/list` SHALL 只列当前 identity 可访问的 KB，并以稳定顺序、默认 50/最大 100 的分页返回；每页与每次 cursor 使用 MUST 重新执行当前身份的授权，不得缓存或信任先前 user/tenant scope。

首版 SHALL 只支持以下 custom URI：`rag://knowledge-bases/{kbId}`、`rag://knowledge-bases/{kbId}/documents/{documentId}`、`rag://knowledge-bases/{kbId}/documents/{documentId}/chunks/{chunkIndex}`。URI MUST 严格验证 scheme/authority/path/ID，并拒绝 query、fragment、userinfo、port、空或额外 segment、percent-encoded slash、`..`、负数和非法数字。

KB/document JSON 与 chunk text SHALL 使用字段白名单和 UTF-8 byte 上限。Resource MUST NOT 暴露 tenantId、owner/uploader ID、vector collection、storage key/path、input/content hash、deleted/version、raw metadata、Redis/Milvus/SQL facts、token 或 credential。当前乐观锁 version MUST NOT 被包装为 document-version Resource。

#### Scenario: 分页列出可访问知识库

- GIVEN 当前用户可访问多个同 tenant KB，且另一个 tenant 有名称/ID 相似 KB
- WHEN client 调用 `resources/list` 并使用 cursor 翻页
- THEN 只返回当前 identity 可访问 KB 的 `rag://knowledge-bases/{kbId}` Resources
- AND 顺序、page size、next cursor 稳定且 bounded
- AND cursor 在另一个 user/tenant 下复用时重新授权，不泄露原列表

#### Scenario: 读取 KB、Document 与 Chunk

- GIVEN 当前用户对 KB 有 read 权限，document 属于该 KB 且 chunk identity 有效
- WHEN client 依次读取 KB、document 与 chunk Resource
- THEN 每层执行 tenant + KB + document 一致性检查
- AND KB/document 只返回白名单 JSON，chunk 只返回 bounded UTF-8 text
- AND 超长 content 明确标记 truncated，不静默超过结果上限

#### Scenario: 非法 URI 或 Foreign Resource

- GIVEN URI 含非法 encoding/path/query/fragment，或 ID 属于 foreign tenant/mismatched KB
- WHEN client 调用 `resources/read`
- THEN server 返回稳定 resource-not-found 或 invalid-request
- AND foreign 与不存在 control 不泄露身份、内容、metadata 或存在性
- AND 不读取 original durable input、vector collection 或无 tenant fallback

#### Scenario: 请求 Document Version Resource

- GIVEN 当前只有乐观锁 version，没有文档版本历史领域模型
- WHEN client 请求 `/versions/...`、version query 或未声明 template
- THEN server 返回稳定 unsupported/not-found
- AND 不把当前 row、durable input 或缓存内容伪装成历史版本

### Requirement: Fixed Bounded Read-Only MCP Tools

server SHALL 只定义 `rag.search`、`rag.ask`、`rag.get-citation` 与 `rag.compare-sources` 四个 C15 Tool。每个 input/output MUST 使用固定 JSON Schema 2020-12、`additionalProperties=false`、明确 required/bounds 和可验证 structured content；兼容 TextContent MUST 与 structured result 语义一致。

所有 Tool SHALL 标注 read-only/non-destructive/idempotent hints，但真实只读性 MUST 由服务端权限、service boundary 与 side-effect tests 保证。C15 MUST NOT 注册 create/update/delete/upload/index/cancel/permission/history/feedback/tenant 等写 Tool，也不得通过 unknown tool name 或 SDK callback 绕过固定 registry。

`rag.search`/`rag.ask` 只接受 kbId、query/question、topK `1..20` 与 minScore `0..1`；MUST NOT 接受任意 filter、tenant/collection、provider/model、retry/timeout/cache selector。`rag.get-citation` 与 `rag.compare-sources` SHALL 对每个 document/chunk reference 独立执行 tenant/KB/read/citation identity 校验。

#### Scenario: Tool Discovery 与 Schema

- GIVEN MCP 与对应 tool group 已显式启用
- WHEN authenticated client 调用 `tools/list`
- THEN 只返回当前配置允许的固定 Tool names、input/output schema 与 read-only annotations
- AND schema hash/identity 稳定，unknown/extra fields fail closed
- AND 不声明写 Tool、MCP tasks 或 server-controlled provider 参数

#### Scenario: Search 与 Ask

- GIVEN 用户对 READY KB 有 read 权限，输入满足固定 bounds，external tools 已显式启用
- WHEN 调用 `rag.search` 或 `rag.ask`
- THEN search 只返回 bounded ranked excerpts/ResourceLinks 与 diagnostics whitelist
- AND ask 只返回现有 no-answer/generation/citation contract 的 answer、bounded citations/ResourceLinks 与 metadata whitelist
- AND 不返回 raw contexts/metadata、collection、prompt、provider body 或异常内容

#### Scenario: Get Citation

- GIVEN kbId、documentId 与 chunkId 属于当前用户可读的同一 tenant/KB/document
- WHEN 调用 `rag.get-citation`
- THEN server 返回 exact matched chunk 的 bounded content、positions、title 与 ResourceLink
- AND provider/model calls=0
- AND mismatch、foreign 或不存在 identity 使用稳定 not-found

#### Scenario: Compare Sources

- GIVEN 两个 source reference 分别通过当前用户的 read authorization 与 citation identity 校验
- WHEN 调用 `rag.compare-sources`
- THEN server 并排返回两个 bounded source/excerpt/ResourceLink
- AND 返回 sameKnowledgeBase/sameDocument/sameChunk 与 `semanticComparisonStatus=NOT_PERFORMED`
- AND 不调用 LLM/rerank/embedding，不输出可信度、矛盾、蕴含或等价判断

### Requirement: Authoritative Read-Only Side-Effect Boundary

MCP Resources/Tools MUST NOT 创建、更新、删除或计数 knowledge base、document、chunk、index、user、tenant、permission、QA history、query count、feedback 或 task 等权威业务事实。`rag.ask` success、no-result、generation error、timeout 或 cancel 都 MUST NOT 调用 REST controller 的 query-count/history side effects，也不得把失败/部分输出保存为正常历史。

C15 MAY 允许 tenant-scoped TTL QA/embedding cache、rate-limit/concurrency counters、已有隐私白名单内的 metrics/traces 与安全日志等非权威技术写入。上述写入 MUST 保持 tenant-local、TTL/bounded、可关闭且不含用户私密内容；cache disabled 时不得写 QA cache，rate-limit security state 不可用时 provider-capable Tool MUST fail closed。

Tool annotations、HTTP method 或“read-only”命名不得作为完成证据；acceptance SHALL 用前后状态快照证明禁止的业务事实不变，并单独列出实际发生的允许技术写入。

#### Scenario: MCP Ask 不写 History 或 Query Count

- GIVEN KB、QA history 与 query count 有可比的请求前快照
- WHEN `rag.ask` 分别得到 success、no-result 或 generation error
- THEN answer/citation contract 正常返回
- AND QA history 行数/内容与 query count 前后不变
- AND 不因 retry、cache hit/miss 或 error 重复计数/保存

#### Scenario: 允许的 Tenant-Scoped 技术写入

- GIVEN cache/metrics/rate-limit 按配置启用
- WHEN MCP Resource/Tool 执行
- THEN 只允许当前 tenant/KB 的 TTL cache、security counter 与 bounded telemetry facts 写入
- AND 不命中、覆盖、清除或泄露另一个 tenant 的技术状态
- AND logs/metrics 不含 user/tenant ID、query、answer、content、snippet、token 或 raw error

#### Scenario: 尝试调用写能力

- GIVEN client 猜测 create/update/delete/upload/index/cancel/feedback/history/tenant tool name 或构造写参数
- WHEN 调用 `tools/list` 或 `tools/call`
- THEN 写 Tool 不存在且 direct call 稳定失败
- AND KB/document/index/user/tenant/permission/history/feedback/task 权威状态前后不变

### Requirement: External Tool Enablement And Provider Budget Boundary

可能触发 embedding/rerank/generation 的 `rag.search` 与 `rag.ask` SHALL 由独立 `rag.mcp.external-tools-enabled` 开关控制，默认 MUST 为 `false`。关闭时二者 MUST NOT 出现在 `tools/list`，按名字直接调用也 MUST fail closed，且 provider/model calls 为 0。

显式启用后，search/ask SHALL 沿用现有 server-owned provider、model、timeout、retry、fallback、citation/no-answer 与 tenant cache contract。client MUST NOT 通过 arguments/metadata/header 改变 provider、model、retry、timeout、filter、cache 或数据出站策略。每次执行 SHALL 输出 sanitized requested/effective provider、model call count、fallback 与 retry/timeout facts；unknown/missing attribution MUST NOT 被写成没有外调。

开发/验收主证据 SHALL 使用 deterministic provider 和 synthetic content，真实 provider/model calls=0、business data outbound=false。任何 live MCP search/ask smoke 前 MUST 披露 tool/样本数、provider/model、预计最大调用/尝试、query/context 数据出站、费用/零费用依据与限流风险并取得用户授权；未授权时 SHALL 记录 `SKIPPED`。

#### Scenario: External Tools 默认关闭

- GIVEN MCP transport 启用但 `external-tools-enabled=false`
- WHEN client 调用 `tools/list` 或直接调用 `rag.search`/`rag.ask`
- THEN list 不包含二者，direct call 返回 `MCP_EXTERNAL_TOOLS_DISABLED` 或 unknown tool
- AND embedding/rerank/generation/provider calls=0，业务数据出站=false

#### Scenario: 显式启用后的 Deterministic 验证

- GIVEN external tools 显式启用且使用 deterministic embedding/generation fixture
- WHEN 调用 search/ask success、no-result 与 failure paths
- THEN tool 输出实际 requested/effective provider、call count、fallback/retry 的白名单 facts
- AND 现有 retrieval/generation/citation/no-answer 语义不被 C15 改写
- AND 真实 provider/model calls=0，business data outbound=false

#### Scenario: 未授权 Live Smoke

- GIVEN 当前 provider 配置可能把 query/context 发送到外部 embedding/rerank/LLM
- WHEN 尚未披露 tool、调用量、模型、最大尝试、数据出站、费用与限流并取得授权
- THEN live MCP smoke 状态为 `SKIPPED`
- AND fake/mock/Testcontainers 结果不得描述为 hosted endpoint/auth/真实生成已验证

#### Scenario: Provider Failure 与 Attribution

- GIVEN 获授权的 search/ask 执行发生 timeout、429/5xx、invalid response 或 fallback
- WHEN Tool 构造结果与 diagnostics
- THEN 沿用 accepted bounded retry/failure/fallback contract，不无限重试或重放
- AND 记录实际 attempt/model-call/fallback category，不把 fallback 写成 model success
- AND 不回显 endpoint credential、Authorization、provider body、query/context 或异常 message/stack

### Requirement: MCP Security Limits Error Semantics And Conformance Evidence

MCP endpoint MUST 在 JSON-RPC 业务处理前校验 Origin、local-only exposure、authentication、request content type/Accept/version 与 request size。`Origin` 存在时 SHALL exact-match 显式 allowlist，`*` MUST 被拒绝；默认 local-only SHALL 拒绝 non-loopback peer，通用 CORS 配置不得替代 MCP Origin evidence。

Resources/Tools SHALL 有固定 input、page/topK/content/result bytes、timeout、rate 与 expensive concurrency 上限。rate key MAY 使用 tenant/user/tool 形成非权威 security namespace，但 MUST NOT 进入普通日志或 metrics label；Redis rate-limit state 不可用时 provider-capable Tool MUST fail closed，不能无限制外调。

server SHALL 区分 JSON-RPC/protocol error 与 `isError=true` tool execution error，并使用稳定、脱敏的 bounded category。foreign/not-found、invalid input、rate limit、timeout、dependency unavailable、result too large、external disabled 与 internal error MUST 机器可判定；任何错误输出/日志不得包含 token、tenant/user ID、SQL/Redis/Milvus facts、query/content、provider body、异常 message/stack 或绝对路径。

C15 只有在固定 SDK/spec/schema/Git HEAD 下通过 focused tests、双 tenant synthetic integration、官方 conformance suite 与独立 MCP client smoke，且 requirement/scenario 有 traceability 时才 MAY 验收。unit/MockMvc、单 client、mock-only、未固定版本或跳过 conformance 不能单独证明标准 MCP 互操作完成。

#### Scenario: Origin 与 Local-Only 防护

- GIVEN `/mcp` 启用且 request 来自 non-loopback peer 或携带不在 allowlist 的 Origin
- WHEN request 到达 transport
- THEN 在解析/执行 Resource/Tool 前返回稳定 forbidden
- AND 不产生 capability 内容、provider 调用、业务读取/写入或敏感日志

#### Scenario: Rate Timeout 与 Result Bounds

- GIVEN input/page/topK/content/result、tool rate、并发或 timeout 超过固定上限
- WHEN Resource/Tool 执行或准备返回结果
- THEN server 返回稳定 invalid/rate-limited/timeout/result-too-large 状态
- AND 不通过截断 schema、重复 provider call、无限 queue/retry 或不受限内容绕过门禁
- AND foreign/用户私密内容不进入错误或 diagnostics

#### Scenario: Protocol 与 Tool Error 分层脱敏

- GIVEN request 存在 malformed JSON-RPC、unknown tool、schema-valid 但业务参数非法、dependency failure 或 internal exception
- WHEN server 构造错误
- THEN 结构/方法错误使用 protocol error，业务执行错误使用 `isError=true` tool result
- AND category 稳定、可机器判定且不含 token、tenant/user、query/content、provider raw facts、异常 message/stack

#### Scenario: 固定 Conformance 与 Client Evidence

- GIVEN SDK/spec/schema/Git HEAD 与 synthetic fixture identity 均固定
- WHEN 运行官方 conformance suite、独立 MCP client smoke 与双 tenant side-effect integration
- THEN initialize、Resources、Tools、errors、auth/tenant/read-only invariants 全部有可追溯 evidence
- AND provider/model calls=0、business data outbound=false、真实 Milvus maintenance=`SKIPPED`
- AND 任一 conformance/client/required scenario 缺失或 skipped 时 C15 不得验收归档
