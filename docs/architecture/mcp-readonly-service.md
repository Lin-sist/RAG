# C15 只读 MCP 服务：开发期使用边界

> 状态日期：2026-07-28  
> 当前状态：实现中。本文只说明已落地的 transport、认证和暴露边界，不代表 C15 已通过 Resources/Tools、双租户、只读副作用、官方 conformance 或独立 client 验收。

## 当前入口与默认值

- endpoint：`http://127.0.0.1:8080/mcp`
- transport：sessionless Streamable HTTP，目标协议版本 `2025-11-25`
- `rag.mcp.enabled=false`：默认不注册 MCP transport
- `rag.mcp.local-only=true`：默认拒绝非 loopback peer，且不信任 `X-Forwarded-For`
- `rag.mcp.allowed-origins=[]`：请求没有 `Origin` 时可供非浏览器 client 使用；存在 `Origin` 时必须 exact-match，禁止 `*`
- `rag.mcp.max-request-bytes=131072`：在 JSON-RPC 解析前限制 request body；可配置范围为 1 至 1048576 bytes

本机开发时可显式设置：

```powershell
$env:RAG_MCP_ENABLED="true"
$env:RAG_MCP_LOCAL_ONLY="true"
$env:RAG_MCP_ALLOWED_ORIGINS=""
$env:RAG_MCP_MAX_REQUEST_BYTES="131072"
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

## 当前错误边界

| 条件 | 当前结果 |
|---|---|
| 缺失、过期或非 Bearer access token | HTTP `401`，`AUTH_001` |
| Origin 不在 exact allowlist | HTTP `403`，`MCP_FORBIDDEN` |
| `local-only=true` 且 peer 非 loopback | HTTP `403`，`MCP_FORBIDDEN` |
| request body 超过配置上限 | HTTP `413`，`MCP_REQUEST_TOO_LARGE` |
| POST `Content-Type` 不是显式 `application/json` | HTTP `415`，`MCP_UNSUPPORTED_MEDIA_TYPE` |
| POST `Accept` 未同时显式包含 JSON 与 SSE | HTTP `406`，`MCP_NOT_ACCEPTABLE` |

上述响应使用固定、脱敏类别，不回显 token、tenant/user identity 或下游异常。Resource not-found、Tool execution error 和 provider failure 的最终 MCP 映射仍属于后续实现切片。
