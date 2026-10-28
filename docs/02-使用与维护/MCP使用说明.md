# C15 只读 MCP 服务：使用与证据边界

> 整理日期：2026-10-27。状态：维护说明；目录整理不等于重新完成运行或真实业务验证。

> 状态日期：2026-07-30
>
> 当前状态：已于 2026-07-30 验收归档；7 requirements / 26 scenarios 已接受进 `rag-system` baseline。服务仍默认关闭，不代表远程生产部署、MCP OAuth Authorization Profile 或真实 provider 可用性已经验收。

## 当前入口与默认值

已登录网页的侧栏提供「MCP 接入」，路径 `/integrations/mcp`。点击「接入 AI 助手」后选择 Codex、Claude Code 或 Cursor；按专属步骤确认地址，在客户端本地设置 `RAG_MCP_ACCESS_TOKEN`，再复制命令/配置或打开 Cursor 添加确认页。Codex 提供命令/TOML，Claude Code 使用 `.mcp.json`，Cursor 可用官方安装链接或 JSON。合并配置时保留已有项；页面不会修改本机文件或导出真实登录 Token。具体实现与未验证项见[客户端接入记录](../../openspec/changes/mcp-access-console/client-onboarding-review.md)。

默认地址只是本机开发示例。弹层允许修改用于生成客户端配置的地址，但不会探测它，也不会改变本页诊断的固定同源 `/mcp` 目标。安装链接只包含地址与环境变量引用，点击后需在客户端确认；复制/打开只表示准备步骤，不能证明客户端已连接。先在客户端确认 `enterprise-rag` 工具可见，再按权限与外调工具开关使用知识库；JWT 过期需在客户端本地更新，尚未实现 MCP 独立凭据或 OAuth。

原配置、工具模板和浏览器协议检查放在默认折叠的「服务状态与高级诊断」。读取配置需后端运行包含 `GET /api/integrations/mcp` 的版本；旧后端缺少接口时如实显示不可用，仍可查看接入模板。后端与协议证据见[首版实施记录](../../openspec/changes/mcp-access-console/implementation-review.md)。

页面「检查连接」仅在配置启用且用户明确点击后，向当前站点同源 `/mcp` 依次发送 initialize、initialized 通知、ping、tools/list、resources/templates/list；最多五个请求、最长 15 秒，不执行工具、不读取资源实例、不调用模型。请求带当前登录身份，但复制模板永远只有 `<access-token>`，不导出登录 Token。检查通过不代表工具执行、知识库权限、生成质量或外部客户端互操作已验证。

本地 Vite 已增加固定 `/mcp` 代理，但浏览器 Origin 仍受下文精确 allowlist 约束；默认空列表会拒绝浏览器请求。部署时还需自行配置同源 `/mcp` 的正确路由与明确允许的页面 Origin。页面不会放宽 local-only/Origin，也不会替你启用服务；`http://127.0.0.1:8080/mcp` 始终只是本机开发地址示例，不是自动推断的对外地址。

- endpoint：`http://127.0.0.1:8080/mcp`
- transport：官方 MCP Java SDK `2.0.0` 的 sessionless Streamable HTTP，目标协议版本 `2025-11-25`
- POST 返回 JSON；GET 在不提供 server stream 时返回 `405`
- server 只声明 Resources 与当前开关允许的 Tools；不声明 session、SSE server push、subscription/listChanged、Prompt、Sampling、Elicitation、Completion 或 Task
- `rag.mcp.enabled=false`：默认不注册 MCP transport
- `rag.mcp.external-tools-enabled=false`：默认不列出也不执行 `rag.search` / `rag.ask`
- `rag.mcp.cache-enabled=false`：默认不读写 MCP QA cache
- `rag.mcp.local-only=true`：默认拒绝非 loopback peer，且不信任 `X-Forwarded-For`
- `rag.mcp.allowed-origins=[]`：请求没有 `Origin` 时可供非浏览器 client 使用；存在 `Origin` 时必须 exact-match，禁止 `*`
- `rag.mcp.max-request-bytes=131072`：在 JSON-RPC 解析前限制 request body；可配置范围为 1 至 1048576 bytes
- `rag.mcp.resource-page-size=50`：`resources/list` 服务端页大小；可配置范围为 1 至 100，client 不能提交 limit 覆盖
- `rag.mcp.max-chunk-bytes=65536`、`max-result-bytes=131072`、`max-query-chars=2000`：分别约束 UTF-8 chunk、Tool 总结果和 query/question；硬上限依次为 1048576、1048576、10000
- `rag.mcp.read-timeout=5s`、`search-timeout=30s`、`ask-timeout=120s`：配置上限依次为 30s、2m、10m
- `rag.mcp.expensive-concurrency-per-user=2`：同一 tenant/user 的 search + ask 共享并发上限；配置范围 1..16

本机开发时可显式设置：

```powershell
$env:RAG_MCP_ENABLED="true"
$env:RAG_MCP_EXTERNAL_TOOLS_ENABLED="false"
$env:RAG_MCP_CACHE_ENABLED="false"
$env:RAG_MCP_LOCAL_ONLY="true"
$env:RAG_MCP_ALLOWED_ORIGINS=""
$env:RAG_MCP_MAX_REQUEST_BYTES="131072"
$env:RAG_MCP_RESOURCE_PAGE_SIZE="50"
```

远程暴露、TLS/proxy trust、registry 发布和 production rollout 不属于 C15 当前范围。不要通过关闭 `local-only` 把开发期 endpoint 直接暴露到公网。

## 认证方式

首版复用本系统签发的 access JWT。MCP client 必须支持为 HTTP transport 手工配置 header：

```json
{
  "url": "http://127.0.0.1:8080/mcp",
  "headers": {
    "Authorization": "Bearer <access-token>"
  }
}
```

- token 只能来自 `Authorization: Bearer ...`；query parameter 和 Cookie 不会被当作认证来源。
- 每个 initialize、Resource 和 Tool 请求都重新经过现有无状态 JWT filter。
- tenant/user identity 只从服务器认证后的 `UserPrincipal` 构造；Tool 参数、Resource URI、cursor、header、query 或 body 都不能选择 tenant。
- 示例中的占位符不能替换成 tracked 文件中的真实 token；access/refresh token、tenant/user facts 也不得写入普通日志或错误响应。

这只是项目部署内的 JWT 认证，不实现 MCP Authorization Profile。当前不提供 OAuth 2.1 Protected Resource Metadata、authorization server discovery、resource audience/indicator 或 scopes，也不应描述为 OAuth-compliant、完整 MCP authorization 或 production authorization。

## Resources

`resources/templates/list` 恰好返回三条 canonical template：

```text
rag://knowledge-bases/{kbId}
rag://knowledge-bases/{kbId}/documents/{documentId}
rag://knowledge-bases/{kbId}/documents/{documentId}/chunks/{chunkIndex}
```

URI 只接受小写 scheme/authority、canonical 正十进制 KB/document ID 和非负 chunk index；query、fragment、userinfo、port、前导零、encoded slash、`..` 与额外 segment 全部 fail closed。当前乐观锁 `version` 不是文档版本历史，因而不提供 version Resource。

| Resource | 输出白名单 |
|---|---|
| KB | `id/name/description/documentCount/isPublic/createdAt/updatedAt` |
| Document | `id/kbId/title/fileType/status/chunkCount/createdAt/updatedAt` |
| Chunk | `text/plain; charset=UTF-8` 的 bounded content；截断只发生在完整 UTF-8 code point，并以 metadata 标记 `truncated` |

`resources/list` 只列当次身份可访问的 KB，按 `kbId` 升序分页。opaque base64url cursor 不含 tenant/user/name；每次翻页都重新授权。document 必须满足 `tenantId + documentId + kbId`，chunk 必须满足 `tenantId + documentId + chunkIndex`，没有无 tenant fallback，也不读取 original durable input。

SDK 2.0.0 的高层 Resource registry 是进程级静态列表；动态 KB list 在官方 stateless handler 公共边界窄装饰 `resources/list`。同一边界对 `resources/read` 原始参数执行 `uri` / `_meta` exact allowlist，再把合法 read 完整委托 SDK；禁止按请求全局增删 registry。

## 固定只读 Tools

所有 Tool 使用 JSON Schema 2020-12、`additionalProperties=false`，并声明 read-only、non-destructive、idempotent、closed-world hints。SDK 2.0.0 不会替 server 强制所有 raw input，因此 `/mcp` 公开 handler 在业务执行前统一拒绝 unknown/reserved field、空文本、越界数字与非法 citation identity。

| Tool | 输入 | 行为与输出边界 |
|---|---|---|
| `rag.search` | `kbId/query`，可选 `topK=1..20`、`minScore=0..1` | KB 授权 + READY tenant vector scope + `QueryEngine`；返回 bounded ranked excerpt、ResourceLink 和 diagnostics 白名单 |
| `rag.ask` | `kbId/question`，可选 `topK=1..20`、`minScore=0..1` | 直接调用 read-only RAG service，不经过 REST controller；保留 answer/no-result/citation 语义，不写 history/query count |
| `rag.get-citation` | `kbId/documentId/chunkId` | exact tenant/KB/document/chunk identity；返回 bounded content、position、title 与 ResourceLink；provider calls=0 |
| `rag.compare-sources` | `left/right` 两个 citation reference | 两侧独立授权并确定性并排返回；固定 `semanticComparisonStatus=NOT_PERFORMED`；不调用 embedding/rerank/LLM |

Tool 输出同时提供 schema-validated `structuredContent` 和语义一致的 TextContent；canonical chunk URI 另作为标准 ResourceLink 返回。ID schema 明确限制在 Java 正 `long` 范围内，固定四 Tool schema 的 canonical SHA-256 为 `44ce4cc851551057bbabfbdf3735b964de300ff691b2e73a612856e9fa64213f`。

当 `external-tools-enabled=false` 时，search/ask 既不出现在 `tools/list`，按名字直调也返回 `MCP_EXTERNAL_TOOLS_DISABLED`，不会进入 retrieval/generation。client 不能提交 provider、model、filter、tenant、collection、retry、timeout 或 cache selector。

## 只读副作用与运行保护

- MCP path 不创建、更新、删除或计数 KB、document、chunk、index、user、tenant、permission、QA history、query count、feedback 或 task。
- `cache-enabled=false` 时不写 QA cache；开启后也只能把 server-owned tenant/KB scope 传给现有 TTL cache contract。
- rate-limit/concurrency counter 与隐私白名单 telemetry 是允许的非权威技术写入。Redis security state 不可用时 provider-capable Tool fail closed。
- deadline 会 cancel 当前 worker，不排队无限等待、不重放 provider；超大结果 fail closed，不靠破坏 schema 的静默截断通过。

## 当前错误边界

| 条件 | 当前结果 |
|---|---|
| 缺失、过期或非 Bearer access token | HTTP `401`，`AUTH_001` |
| Origin 不在 exact allowlist | HTTP `403`，`MCP_FORBIDDEN` |
| `local-only=true` 且 peer 非 loopback | HTTP `403`，`MCP_FORBIDDEN` |
| request body 超过配置上限 | HTTP `413`，`MCP_REQUEST_TOO_LARGE` |
| POST `Content-Type` 不是显式 `application/json` | HTTP `415`，`MCP_UNSUPPORTED_MEDIA_TYPE` |
| POST `Accept` 未同时显式包含 JSON 与 SSE | HTTP `406`，`MCP_NOT_ACCEPTABLE` |
| `resources/list` cursor 非 canonical 或包含 tenant/limit 等额外字段 | JSON-RPC `-32602`，`MCP_INVALID_ARGUMENT` |
| `resources/read` 包含 `tenantId` 等 extra 参数 | JSON-RPC `-32602`，`MCP_INVALID_ARGUMENT` |
| foreign/nonexistent/mismatched Resource | JSON-RPC `-32603`，`MCP_RESOURCE_NOT_FOUND` |
| 同 tenant 私有 KB 无读权限 | JSON-RPC `-32603`，`MCP_FORBIDDEN` |
| unknown Tool 或 raw Tool 参数结构非法 | JSON-RPC protocol error，`MCP_INVALID_ARGUMENT` |
| Tool 的 forbidden/not-found/rate/timeout/dependency/result/internal failure | HTTP 200 的 `isError=true` Tool result，使用 bounded `MCP_*` 类别 |

上述响应使用固定、脱敏类别，不回显 token、tenant/user identity、query/content、raw Resource URI、provider body、绝对路径或下游异常。

## 当前验证与可复现入口

实现期证据使用 synthetic content、deterministic embedding/generation 和测试自有 MySQL 8.0.36、Redis 7-alpine digest、etcd 3.5.5、MinIO 固定 release、Milvus 2.3.4；真实 provider/model calls=0、business data outbound=false、真实 Milvus maintenance=`SKIPPED`。

```powershell
mvn -q -pl rag-admin -am -P c15-mcp-readonly "-Dtest=C15McpProfileContractTest" "-Dit.test=C15McpReadOnlyIT,C15McpConformanceIT" "-Dsurefire.failIfNoSpecifiedTests=false" verify
```

- `C15McpReadOnlyIT`：双 tenant/owner/reader/private/public、三类 Resource、四个 Tool、matched foreign controls、success/no-result/generation error、权威状态摘要、QA cache/count/history 和粗粒度 timing。
- `McpIndependentClientMvcTest`：官方 Java SDK 独立 client 覆盖 initialize、分页 Resources、三 template、三类 read、四 Tool 与 unknown write Tool。
- `C15McpConformanceIT`：固定 `@modelcontextprotocol/conformance@0.1.15`，在 Node `22.17.0` 上运行适用于 C15 capability 的 `server-initialize`、`ping`、`tools-list`、`resources-list`、`dns-rebinding-protection`。套件其余场景要求专用 echo/add/long-running/Prompt/Task fixture，与 C15 固定只读 registry 冲突，明确为不适用，不能描述为整套全部场景通过。

详细 requirement/scenario 映射见 [`../eval/mcp/c15-mcp-readonly-traceability.md`](../eval/mcp/c15-mcp-readonly-traceability.md)。正式归档证据生成于 clean HEAD `45959672ec64ec72c05bcbe17fe52204555f1098`，`workingTreeDirty=false`；该结论只适用于固定 SDK/spec/schema 与本机 synthetic profile，不能声称 production-ready。
