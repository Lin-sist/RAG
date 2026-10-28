export type McpClient = 'codex' | 'claude-code' | 'cursor'
export const MCP_CREDENTIAL_ENV = 'RAG_MCP_ACCESS_TOKEN'
export const MCP_DEFAULT_ENDPOINT = 'http://127.0.0.1:8080/mcp'

/** This address is only exported as configuration, never passed to a network client. */
export function normalizeMcpEndpoint(input: string): string | null {
  if (input.length > 512 || /[\x00-\x1f\x7f`$'"<>|&;()\\?#@]/.test(input)) return null
  try {
    const url = new URL(input.trim())
    if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.search || url.hash) return null
    if (!/^(?:[a-z\d.-]+|\[[a-f\d:]+\])$/i.test(url.hostname) || !/^\/(?:[A-Za-z\d._~-]+\/)*mcp$/.test(url.pathname)) return null
    return url.href
  } catch { return null }
}

export function buildMcpClientSetup(client: McpClient, endpoint: string) {
  const url = normalizeMcpEndpoint(endpoint)
  if (!url) throw new Error('MCP_ENDPOINT_INVALID')
  const codexConfig = `[mcp_servers.enterprise-rag]\nurl = ${JSON.stringify(url)}\nbearer_token_env_var = "${MCP_CREDENTIAL_ENV}"`
  const codexCommand = `codex mcp add enterprise-rag --url "${url}" --bearer-token-env-var ${MCP_CREDENTIAL_ENV}`
  const httpConfig = { url, headers: { Authorization: `Bearer \${env:${MCP_CREDENTIAL_ENV}}` } }
  const claudeConfig = { mcpServers: { 'enterprise-rag': { type: 'http', url, headers: { Authorization: `Bearer \${${MCP_CREDENTIAL_ENV}}` } } } }
  const cursorConfig = JSON.stringify({ mcpServers: { 'enterprise-rag': httpConfig } }, null, 2)
  const utf8 = new TextEncoder().encode(JSON.stringify(httpConfig))
  const encoded = btoa(Array.from(utf8, byte => String.fromCharCode(byte)).join(''))
  const installUrl = 'cursor://anysphere.cursor-deeplink/mcp/install?' + new URLSearchParams({ name: 'enterprise-rag', config: encoded })
  return {
    client, url, command: client === 'codex' ? codexCommand : null,
    config: client === 'codex' ? codexConfig : client === 'claude-code' ? JSON.stringify(claudeConfig, null, 2) : cursorConfig,
    configLocation: client === 'codex' ? '~/.codex/config.toml' : client === 'claude-code' ? '项目根目录 .mcp.json' : '.cursor/mcp.json 或 ~/.cursor/mcp.json',
    installUrl: client === 'cursor' ? installUrl : null,
  }
}

export const MCP_DISCOVERY_PROMPT = '请确认 enterprise-rag MCP 服务是否已连接，并列出你能发现的工具名称。暂时不要调用检索或问答工具。'
export const MCP_QUERY_PROMPT = '请使用 enterprise-rag 的检索工具，在我指定的知识库中查找【问题】，根据返回资料回答，并列出引用出处。若工具不可用，请直接说明。'
