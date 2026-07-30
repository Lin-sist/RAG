# Tasks: C15 MCP Read-Only Service

## 0. 事前闸门（实现前必须完成）

- [x] 用户审阅并批准 proposal 的范围、非目标、部署型 JWT 限制与 C15 完成口径。
- [x] 用户确认 design 的 18 条决策，重点确认 sessionless Streamable HTTP、official SDK compatibility hard-stop、default-off/local-only、JWT 非 OAuth profile、deterministic compare-sources 与技术写入边界。
- [x] 用户审阅并批准 `rag-system` spec delta 的 7 requirements / 26 scenarios。
- [x] 用户明确授权加入/下载固定版本官方 MCP Java SDK 与固定 conformance 工具；该授权不包含 Spring 基线升级、真实 provider 调用、push、PR 或部署。
- [x] 明确提交责任；当前保持 `用户手动提交`，Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- [x] 实现开始前复查 `git status --short --branch`：HEAD=`c864e1c`，工作区与暂存区干净，规划提交 hash 已补录。
- [x] 用户复核 implementation-discovered 决策 19：SDK 2.0.0 高层 Resource registry 为进程级静态状态，C15 只在官方 stateless handler 公共边界窄装饰 `resources/list`；用户于 2026-07-29 验收 discovery checkpoint 并要求进入下一阶段实现。
- [x] 用户复核 implementation-discovered 决策 20：用户于 2026-07-30 验收上一切片并允许完成剩余 C15；SDK 2.0.0 会忽略 `resources/read` extra 参数，C15 在同一公开 handler 边界只做 `uri` / `_meta` exact allowlist，合法 read 继续委托 SDK。

## 1. SDK Compatibility And Protocol Foundation

- [x] RED：`McpProtocolMvcTest` 首次 testCompile 仅因 `McpServerConfiguration` 不存在而失败，锁定 initialize/version/server identity/sessionless transport public behavior。
- [x] 固定官方 MCP Java SDK/core/Jackson 2 binding=`2.0.0`、目标 spec=`2025-11-25`、conformance suite=`0.1.15`；官方 release commit=`f56d038`、license=`MIT`，本地 artifact SHA-256 已记录到 AGENT_LOG。
- [x] 运行 dependency tree/convergence 审计：Boot 实际解析 Spring `6.1.2`、Reactor `3.6.1`、Jackson `2.15.3`、Tomcat `10.1.17`/Servlet `6.0`、SLF4J `2.0.9`；SDK transport 与 schema runtime 通过。全树 Enforcer 仍只命中既有 Milvus/Qdrant/PDF/Flexmark 冲突，未冒充全仓 convergence GREEN。
- [x] 只在 `rag-admin` 引入官方 core/Servlet + Jackson 2 binding；未引入 Spring AI starter，未升级 Spring Boot/Spring Framework/Jackson/Reactor。
- [x] 实现 `McpProperties` 与 conditional configuration；`rag.mcp.enabled=false` 时不注册 `/mcp` transport。
- [x] 实现 sessionless Streamable HTTP `/mcp`：POST JSON、GET 405、不返回 `MCP-Session-Id`，initialize 只声明 Resources/Tools。
- [x] reactor compile、transport focused tests 与现有 JWT/RequestIdentity/QA controller smoke 通过；当前无需 `mcp-runtime-foundation` 前置 change。
- [x] compatibility evidence、跳过项与 hard-stop 结论已追加到 `.ai/AGENT_LOG.md`。

## 2. Authentication, Origin And Request Identity

- [x] 建立 missing/expired token、cookie/query token、invalid Origin、non-loopback request 和 principal 缺 tenant identity 的前置失败测试；新增 Origin/local-only/identity/context 边界均先 RED，既有 JWT header-only 行为以回归测试锁定。
- [x] 新增 `/mcp` 专用 Origin/local-only/request-size filter；`Origin` exact-match、`*` fail startup，不信任 forwarded header；无 `Content-Length` 的超限 body、非 JSON 与不完整 Accept 均在 transport 解析前稳定拒绝。
- [x] 让现有 JWT filter 保护 initialize、resources/templates/list、resources/list/read、tools/list/call；missing token 组合测试证明所有方法逐请求重新认证，sessionless transport 不保存 identity。
- [x] 实现唯一 `McpRequestIdentityResolver` 并接入 SDK transport context，只从当次 authenticated `UserPrincipal` 构造 `RequestIdentity`；handler 只读取 server context key，不接受其他 tenant/user map。
- [x] 对 unauthorized/forbidden/not-found 建立稳定且脱敏的 HTTP/JSON-RPC/tool error 边界；access/refresh token、tenant/user facts 不进入响应或日志。
- [x] README 与 `docs/architecture/mcp-readonly-service.md` 明确手工 Bearer header、default-off/local-only、query/Cookie token 禁止，以及不提供 MCP OAuth metadata/discovery/audience/scopes、不宣称 authorization profile 兼容。

## 3. Read-Only Resources

- [x] RED：URI query/fragment/userinfo/port、percent-encoded slash、`..`、非法/负 ID、extra segment、foreign ID 与 document/KB mismatch 均 fail closed。
- [x] 实现 `McpResourceUri` 的三种 exact canonical template 与 round-trip tests：KB、document、chunk；KB/document 为正 long，chunkIndex 为非负 int，解析错误固定脱敏且不回显原 URI。
- [x] 实现 authenticated `resources/list`：只列当前 identity 可访问 KB，kbId 稳定排序、50 默认/100 最大、opaque cursor，每页重新授权；同一 cursor 跨 tenant 测试证明只返回当次身份结果。
- [x] 实现固定 `resources/templates/list`；恰好三条 canonical template，不声明 subscriptions/listChanged，不提供 document version template。
- [x] 实现 KB Resource JSON whitelist；每次 read 从 transport context 取身份并复用现有 KB read authorization，只返回 id/name/description/documentCount/isPublic/createdAt/updatedAt，排除 tenantId、ownerId、vectorCollection、storage/vector/internal fields，provider calls=0。
- [x] 实现 document Resource JSON whitelist，并用 tenantId + documentId + kbId 双重一致性检查。
- [x] 实现 chunk Resource text read，严格按 tenant/document/chunkIndex 查询，UTF-8 bytes 上限与 `truncated` evidence 可验证。
- [x] 对 foreign/nonexistent 使用 matched controls，证明 error fingerprint 与可观察 timing 不泄露另一个 tenant 的资源事实。
- [x] 运行 Resources focused tests，并把 requirement/scenario→test mapping 追加到 C15 traceability artifact。

## 4. Fixed Tool Schemas And Result Mapping

- [x] RED：unknown/extra fields、tenant/collection/filter/provider aliases、空/超长 query、topK/minScore 越界、非法 citation identity 与 output schema mismatch 均稳定失败。
- [x] 固定四个 tool names、JSON Schema 2020-12 input/output、`additionalProperties=false`、read-only annotations 与 `taskSupport=forbidden`。
- [x] 实现 `McpResultMapper`，保证 structuredContent 与兼容 TextContent 语义一致，字段白名单、单项/总 bytes 上限和 ResourceLink 可验证。
- [x] 实现 `rag.search`：KB read authorization + ready vector scope + `QueryEngine.retrieveWithDiagnostics`，不接受任意 filter/provider/model。
- [x] 实现 `rag.ask`：read-only facade + `RAGService.ask` + shared source enrichment；不调用 REST controller。
- [x] 实现 `McpCitationReader` 与 `rag.get-citation`，按 tenant + KB + document + chunk identity exact match，provider calls=0。
- [x] 实现 `rag.compare-sources`，对两个 source 分别授权并返回 deterministic side-by-side；固定 `semanticComparisonStatus=NOT_PERFORMED`，provider calls=0。
- [x] 对 success/no-result/error 运行 output schema validation；raw context/metadata/provider body/exception 不进入 tool result。
- [x] 运行 Tools focused tests，记录 schema hash、tool list identity 与结果上限。

## 5. Authoritative Side-Effect Boundary

- [x] RED：直接复用 REST ask 会增加 query count/history；保留该边界事实后，让 MCP path 改走 read-only facade。
- [x] 对 `rag.ask` success/no-result/generation error 分别断言 QA history 与 query count 前后不变。
- [x] 对全部 resources/tools 断言 KB/document/index/user/tenant/permission/history/feedback/task rows、逻辑删除/version/count 前后不变。
- [x] `rag.mcp.cache-enabled=false` 时断言 MCP ask 不写 QA cache；显式启用时只允许当前 tenant/KB 的 TTL cache，payload scope mismatch fail closed。
- [x] 允许的 rate-limit/concurrency/metrics/traces 写入只包含 bounded facts，不在普通日志/metric labels 写 user/tenant/query/content/token。
- [x] 禁止注册任何 create/update/delete/cancel/feedback/history tool；tools/list 与 direct call 两侧都验证 unknown/prohibited tool fail closed。
- [x] 把 read-only invariant 与允许技术写入逐项映射到 spec delta 和 integration evidence。

## 6. External Calls, Limits And Failure Semantics

- [x] RED：`external-tools-enabled=false` 时 `rag.search`/`rag.ask` 不出现在 tools/list，按名字直接调用也不执行 provider。
- [x] 实现 external tool visibility/call 双重 guard；Resources/get-citation/compare-sources 保持 provider calls=0。
- [x] deterministic fake 下验证 search/ask 的 requested/effective provider、model call count、fallback、timeout/retry diagnostics 白名单，不改变现有 provider contract。
- [x] 客户端传 provider/model/retry/timeout/filter/cache 等 extra property 时 schema fail closed，不能覆盖 server config。
- [x] 实现 user+tenant+tool rate-limit 与 search+ask concurrency=2；Redis security-state failure按 critical dependency fail closed。
- [x] 实现 resources/get/compare 5s、search 30s、ask 120s 和 result-too-large 终态；timeout/cancel 不重放 provider 或伪造成功。
- [x] 用户未授权 live smoke，已记录 `SKIPPED`；未调用 hosted provider，也未用 synthetic fixture 声称 hosted endpoint/auth/真实生成可用。

## 7. Dual-Tenant Integration And Protocol Conformance

- [x] RED：双 tenant fixture 中 foreign KB/document/chunk/citation 经 MCP Resources/Tools 可见或可推断时测试稳定失败。
- [x] 新增隔离 `c15-mcp-readonly` Failsafe profile，复用已声明 MySQL/Redis/Milvus Testcontainers 与 deterministic embedding/generation，不连接常驻服务。
- [x] `C15McpReadOnlyIT` 覆盖 owner/reader/public/permission、resources pagination/read、四 tools、foreign/nonexistent control、cache off、side-effect snapshots 与 failure paths；cache on 由独立 MVC test 锁定 server-owned scope。
- [x] harness 记录 image/version/health/spec/SDK/tool schema/Git HEAD/provider-call facts，只操作自有 container/network/temp path。
- [x] 经用户授权，固定 conformance `0.1.15` 与 Node `22.17.0` 运行五个适用于 C15 capability 的 generic server scenarios；suite-specific fixture 场景按 design 决策 22 记录为不适用。
- [x] 独立官方 Java SDK client 完成 initialize、resources/templates/list、resources/list/read、tools/list、四 tools 与错误路径 smoke。
- [x] conformance/client smoke 全部使用 synthetic fixture 与 deterministic provider；business data outbound=false，真实 provider/model calls=0。
- [x] 生成 C15 traceability，将每个 requirement/scenario 映射到 test/evidence；unit/MockMvc 不替代 wire conformance。

## 8. Full Gates And Closeout

- [x] 运行 MCP focused unit/MVC tests 与 `c15-mcp-readonly` Failsafe，记录 tests/failures/errors/skips、SDK/spec/client identity。
- [x] 运行 `mvn -q test`；仍只命中既有 OTel collector 时序债务，独立复跑通过并如实保持全仓非 GREEN，未在 C15 修改观测实现。
- [x] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`，190 tests / OK，evaluation contract/report status 未漂移。
- [x] 前端无改动，包含 `vue-tsc` 的正式 build 按规则记为 `SKIPPED`。
- [x] 运行 SensitiveLogs、credentials、absolute path、raw content/metadata、protected paths、dependency convergence、Markdown links 与 `git diff --check`；除已记录的既有依赖收敛债务外通过。
- [x] 更新 tasks、`.ai/AGENT_LOG.md`、`openspec/project.md`、architecture/roadmap/optimization 与 C15 usage/traceability 文档。
- [x] 收口结论只声明“在固定 SDK/spec/client 与 synthetic Milvus profile 下，只读 MCP Resources/Tools 互操作及 tenant/side-effect evidence 通过”。
- [x] 用户于 2026-07-30 明确要求归档；delta 已 exact-copy 接受进 `rag-system` baseline，change 已归档并将 `.ai/ACTIVE_TASK.md` 置为 `IDLE`。
- [x] 归档后仍不宣称 MCP OAuth profile、远程生产部署、Qdrant/Elasticsearch、真实 provider、第二业务 tenant、Router 或 Agentic RAG 已完成。
