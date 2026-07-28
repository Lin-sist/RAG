# Tasks: C15 MCP Read-Only Service

## 0. 事前闸门（实现前必须完成）

- [ ] 用户审阅并批准 proposal 的范围、非目标、部署型 JWT 限制与 C15 完成口径。
- [ ] 用户确认 design 的 18 条决策，重点确认 sessionless Streamable HTTP、official SDK compatibility hard-stop、default-off/local-only、JWT 非 OAuth profile、deterministic compare-sources 与技术写入边界。
- [ ] 用户审阅并批准 `rag-system` spec delta 的 7 requirements / 26 scenarios。
- [ ] 用户明确是否授权加入/下载固定版本官方 MCP Java SDK 与固定 conformance 工具；该授权不包含 Spring 基线升级、真实 provider 调用、push、PR 或部署。
- [ ] 明确提交责任；当前默认 `用户手动提交`，Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- [ ] 实现开始前复查 `git status --short --branch`，保护用户改动并补录上一规划提交 hash（若用户已提交）。

## 1. SDK Compatibility And Protocol Foundation

- [ ] RED：最小 transport tests 证明 disabled state、initialize/version negotiation、capability declaration、GET 405 与 sessionless header 边界尚未满足。
- [ ] 固定 MCP Java SDK、Jackson 2 binding、目标 spec 与 conformance suite 版本；记录来源、hash/version 与 license，不使用 floating/latest。
- [ ] 运行 dependency tree/convergence 审计，记录 Java/Spring Framework/Reactor/Jackson/Servlet/SLF4J/OTel 实际解析版本及冲突。
- [ ] 只在 `rag-admin` 引入官方 core/Servlet + Jackson 2 binding；不引入 Spring AI starter，不升级 Spring Boot/Spring Framework/Jackson/Reactor。
- [ ] 实现 `McpProperties` 与 conditional configuration；`rag.mcp.enabled=false` 时不注册 `/mcp` transport。
- [ ] 实现 sessionless Streamable HTTP `/mcp`：POST JSON、GET 405、不返回 `MCP-Session-Id`、不声明 SSE/session/notification/subscription/prompts/tasks。
- [ ] 运行 reactor compile、transport focused tests 与现有 auth/controller smoke；若需要框架升级、出现 linkage/serialization conflict 或不能对齐目标 spec，停止实现并报告 `mcp-runtime-foundation` 前置需求。
- [ ] 将 compatibility evidence、跳过项与 hard-stop 结论追加到 `.ai/AGENT_LOG.md`。

## 2. Authentication, Origin And Request Identity

- [ ] RED：missing/expired token、cookie/query token、invalid Origin、non-loopback request 和 principal 缺 tenant identity 均在业务 handler 前失败。
- [ ] 新增 `/mcp` 专用 Origin/local-only/request-size filter；`Origin` 存在时 exact-match allowlist，禁止 `*`，不把通用 CORS 当作通过证据。
- [ ] 让现有 JWT filter 保护 initialize、resources/templates/list、resources/list/read、tools/list/call；所有请求都重新认证，不使用 session identity。
- [ ] 实现唯一 `McpRequestIdentityResolver`，只从 `UserPrincipal` 构造 `RequestIdentity`，不读取 tool args、URI、cursor、header/query/body tenant selector。
- [ ] 对 unauthorized/forbidden/not-found 建立稳定且脱敏的 HTTP/JSON-RPC/tool error 边界；access/refresh token、tenant/user facts 不进入响应或日志。
- [ ] 明确 README/usage evidence：首版需要手工 Bearer header 配置，不提供 MCP OAuth metadata/discovery/audience/scopes，不宣称 authorization profile 兼容。

## 3. Read-Only Resources

- [ ] RED：URI query/fragment/userinfo/port、percent-encoded slash、`..`、非法/负 ID、extra segment、foreign ID 与 document/KB mismatch 均 fail closed。
- [ ] 实现 `McpResourceUri` 的三种 exact template 与 round-trip tests：KB、document、chunk。
- [ ] 实现 authenticated `resources/list`：只列当前 identity 可访问 KB，kbId 稳定排序、50 默认/100 最大、opaque cursor，每页重新授权。
- [ ] 实现固定 `resources/templates/list`；不声明 subscriptions/listChanged，不提供 document version template。
- [ ] 实现 KB Resource JSON whitelist，排除 tenantId、ownerId、vectorCollection、storage/vector/internal fields。
- [ ] 实现 document Resource JSON whitelist，并用 tenantId + documentId + kbId 双重一致性检查。
- [ ] 实现 chunk Resource text read，严格按 tenant/document/chunkIndex 查询，UTF-8 bytes 上限与 `truncated` evidence 可验证。
- [ ] 对 foreign/nonexistent 使用 matched controls，证明 error fingerprint 与可观察 timing 不泄露另一个 tenant 的资源事实。
- [ ] 运行 Resources focused tests，并把 requirement/scenario→test mapping 追加到 C15 traceability artifact。

## 4. Fixed Tool Schemas And Result Mapping

- [ ] RED：unknown/extra fields、tenant/collection/filter/provider aliases、空/超长 query、topK/minScore 越界、非法 citation identity 与 output schema mismatch 均稳定失败。
- [ ] 固定四个 tool names、JSON Schema 2020-12 input/output、`additionalProperties=false`、read-only annotations 与 `taskSupport=forbidden`。
- [ ] 实现 `McpResultMapper`，保证 structuredContent 与兼容 TextContent 语义一致，字段白名单、单项/总 bytes 上限和 ResourceLink 可验证。
- [ ] 实现 `rag.search`：KB read authorization + ready vector scope + `QueryEngine.retrieveWithDiagnostics`，不接受任意 filter/provider/model。
- [ ] 实现 `rag.ask`：read-only facade + `RAGService.ask` + shared source enrichment；不调用 REST controller。
- [ ] 实现 `McpCitationReader` 与 `rag.get-citation`，按 tenant + KB + document + chunk identity exact match，provider calls=0。
- [ ] 实现 `rag.compare-sources`，对两个 source 分别授权并返回 deterministic side-by-side；固定 `semanticComparisonStatus=NOT_PERFORMED`，provider calls=0。
- [ ] 对 success/no-result/error 运行 output schema validation；raw context/metadata/provider body/exception 不进入 tool result。
- [ ] 运行 Tools focused tests，记录 schema hash、tool list identity 与结果上限。

## 5. Authoritative Side-Effect Boundary

- [ ] RED：直接复用 REST ask 会增加 query count/history；保留该证据后，让 MCP path 改走 read-only facade。
- [ ] 对 `rag.ask` success/no-result/generation error 分别断言 QA history 与 query count 前后不变。
- [ ] 对全部 resources/tools 断言 KB/document/index/user/tenant/permission/history/feedback/task rows、逻辑删除/version/count 前后不变。
- [ ] `rag.mcp.cache-enabled=false` 时断言 MCP ask 不写 QA cache；显式启用时只允许当前 tenant/KB 的 TTL cache，payload scope mismatch fail closed。
- [ ] 允许的 rate-limit/concurrency/metrics/traces 写入只包含 bounded facts，不在普通日志/metric labels 写 user/tenant/query/content/token。
- [ ] 禁止注册任何 create/update/delete/cancel/feedback/history tool；tools/list 与 direct call 两侧都验证 unknown/prohibited tool fail closed。
- [ ] 把 read-only invariant 与允许技术写入逐项映射到 spec delta 和 integration evidence。

## 6. External Calls, Limits And Failure Semantics

- [ ] RED：`external-tools-enabled=false` 时 `rag.search`/`rag.ask` 不出现在 tools/list，按名字直接调用也不执行 provider。
- [ ] 实现 external tool visibility/call 双重 guard；Resources/get-citation/compare-sources 保持 provider calls=0。
- [ ] deterministic fake 下验证 search/ask 的 requested/effective provider、model call count、fallback、timeout/retry diagnostics 白名单，不改变现有 provider contract。
- [ ] 客户端传 provider/model/retry/timeout/filter/cache 等 extra property 时 schema fail closed，不能覆盖 server config。
- [ ] 实现 user+tenant+tool rate-limit 与 search+ask concurrency=2；Redis security-state failure按 critical dependency fail closed。
- [ ] 实现 resources/get/compare 5s、search 30s、ask 120s 和 result-too-large 终态；timeout/cancel 不重放 provider 或伪造成功。
- [ ] 若用户另行授权 live smoke，先披露 tool、样本数、provider/model、预计最大尝试、数据出站、费用/限流；未授权时记录 `SKIPPED`，不能用 fake 声称 hosted provider 可用。

## 7. Dual-Tenant Integration And Protocol Conformance

- [ ] RED：双 tenant fixture 中 foreign KB/document/chunk/citation 经 MCP Resources/Tools 可见或可推断时测试稳定失败。
- [ ] 新增隔离 `c15-mcp-readonly` Failsafe profile，复用已声明 MySQL/Redis/Milvus Testcontainers 与 deterministic embedding/generation，不连接常驻服务。
- [ ] `C15McpReadOnlyIT` 覆盖 owner/reader/public/permission、resources pagination/read、四 tools、foreign/nonexistent control、cache on/off、side-effect snapshots 与 failure paths。
- [ ] harness 记录 image/version/health/spec/SDK/tool schema/Git HEAD/provider-call facts，只操作自有 container/network/temp path。
- [ ] 固定版本运行官方 MCP conformance suite；如需 `npx` 下载，先取得用户批准并记录版本/调用/网络事实。
- [ ] 至少一个独立 MCP client 完成 initialize、resources/templates/list、resources/list/read、tools/list、四 tools 与错误路径 smoke。
- [ ] conformance/client smoke 全部使用 synthetic fixture 与 deterministic provider；business data outbound=false，真实 provider/model calls=0。
- [ ] 生成 C15 traceability，将每个 requirement/scenario 映射到 test/evidence；unit/MockMvc 不替代 wire conformance。

## 8. Full Gates And Closeout

- [ ] 运行 MCP focused unit/MVC tests 与 `c15-mcp-readonly` Failsafe，记录 tests/failures/errors/skips、SDK/spec/client identity。
- [ ] 运行 `mvn -q test`；若仍只命中既有 OTel collector 时序债务，独立复跑并如实保持全仓非 GREEN，不在 C15 顺手修改观测实现。
- [ ] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`，确认 evaluation contract/report status 未漂移。
- [ ] 前端无改动则正式 build 记为 `SKIPPED`；若任何前端/共享 DTO 发生变化，运行包含 `vue-tsc` 的正式 build。
- [ ] 运行 SensitiveLogs、credentials、absolute path、raw content/metadata、protected paths、dependency convergence、Markdown links 与 `git diff --check`。
- [ ] 更新 tasks、`.ai/AGENT_LOG.md`、`openspec/project.md`、architecture/roadmap/optimization 与 C15 usage/traceability 文档。
- [ ] 收口结论只声明“在固定 SDK/spec/client 与 synthetic Milvus profile 下，只读 MCP Resources/Tools 互操作及 tenant/side-effect evidence 通过”。
- [ ] 用户最终验收后才把 delta 原文接受进 `rag-system` baseline、归档 change 并将 `.ai/ACTIVE_TASK.md` 置为 `IDLE`。
- [ ] 归档后仍不宣称 MCP OAuth profile、远程生产部署、Qdrant/Elasticsearch、真实 provider、第二业务 tenant、Router 或 Agentic RAG 已完成。
