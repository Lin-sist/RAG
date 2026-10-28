import type { CheckStep, CheckStepKey, McpCheckResult, McpMetadata } from '@/types/mcp'

export const MCP_MAX_BYTES = 131072
export const MCP_REQUEST_TIMEOUT = 5000
export const MCP_TOTAL_TIMEOUT = 15000
const toolNames = ['rag.search', 'rag.ask', 'rag.get-citation', 'rag.compare-sources']
const resourceNames = ['knowledge-base', 'knowledge-base-document', 'knowledge-base-document-chunk']
const object = (value: unknown): value is Record<string, unknown> => !!value && typeof value === 'object' && !Array.isArray(value)
const same = (left: string[], right: string[]) => left.length === right.length && [...left].sort().every((v, i) => v === [...right].sort()[i])
function canonical(value: unknown): string {
  if (Array.isArray(value)) return '[' + value.map(canonical).join(',') + ']'
  if (object(value)) return '{' + Object.keys(value).sort().map(key => JSON.stringify(key) + ':' + canonical(value[key])).join(',') + '}'
  return JSON.stringify(value) ?? 'null'
}

export class McpCheckError extends Error {
  constructor(public readonly category: string) { super(category) }
}

export function parseMcpMetadata(value: unknown): McpMetadata {
  const fail = () => { throw new McpCheckError('METADATA_INVALID') }
  if (!object(value)) return fail()
  if (['enabled', 'externalToolsConfigured', 'externalToolsEffective', 'localOnly'].some(key => typeof value[key] !== 'boolean')) return fail()
  if (value.endpointPath !== '/mcp' || value.transport !== 'streamable-http' || value.sessionMode !== 'stateless'
    || value.authMode !== 'project-access-jwt' || value.oauthSupported !== false || value.originPolicy !== 'exact-allowlist'
    || value.targetProtocolVersion !== '2025-11-25' || !object(value.server)
    || value.server.name !== 'enterprise-rag-readonly' || value.server.version !== 'c15-v1') return fail()
  if (!Array.isArray(value.implementedTools) || value.implementedTools.length !== 4 || !same(value.implementedTools.map(t => t?.name), toolNames)) return fail()
  if (!Array.isArray(value.implementedResourceTemplates) || value.implementedResourceTemplates.length !== 3
    || !same(value.implementedResourceTemplates.map(t => t?.name), resourceNames)) return fail()
  for (const t of value.implementedTools) {
    if (!object(t) || typeof t.title !== 'string' || typeof t.description !== 'string' || !object(t.inputSchema)
      || !object(t.outputSchema) || t.readOnly !== true || typeof t.externalCallPossible !== 'boolean') return fail()
    if (t.externalCallPossible !== ['rag.search', 'rag.ask'].includes(String(t.name))) return fail()
  }
  for (const t of value.implementedResourceTemplates) {
    if (!object(t) || ['title', 'description', 'uriTemplate', 'mimeType'].some(k => typeof t[k] !== 'string')) return fail()
  }
  const expected = value.enabled ? toolNames.filter(name => value.externalToolsConfigured || !['rag.search', 'rag.ask'].includes(name)) : []
  if (!Array.isArray(value.configuredToolNames) || !same(value.configuredToolNames, expected)
    || !Array.isArray(value.configuredResourceTemplateNames) || !same(value.configuredResourceTemplateNames, value.enabled ? resourceNames : [])
    || value.externalToolsEffective !== (value.enabled && value.externalToolsConfigured)) return fail()
  // The API is authenticated; render descriptive strings as text only, never HTML.
  return value as unknown as McpMetadata
}

export function initialMcpSteps(): CheckStep[] {
  return [
    { key: 'initialize', label: '协议握手', status: 'idle' },
    { key: 'ping', label: 'Ping', status: 'idle' },
    { key: 'tools', label: '工具发现', status: 'idle' },
    { key: 'resources', label: '资源模板发现', status: 'idle' },
  ]
}

function abortable<T>(promise: Promise<T>, signal: AbortSignal): Promise<T> {
  return new Promise((resolve, reject) => {
    const abort = () => reject(new McpCheckError(signal.reason === 'timeout' ? 'TIMEOUT' : 'CANCELLED'))
    if (signal.aborted) { abort(); return }
    signal.addEventListener('abort', abort, { once: true })
    promise.then(resolve, reject).finally(() => signal.removeEventListener('abort', abort))
  })
}

async function boundedText(response: Response, signal: AbortSignal): Promise<string> {
  const size = response.headers.get('Content-Length')
  if (size !== null && Number(size) > MCP_MAX_BYTES) { void response.body?.cancel().catch(() => {}); throw new McpCheckError('RESPONSE_TOO_LARGE') }
  if (!response.body) return ''
  const reader = response.body.getReader(), decoder = new TextDecoder('utf-8', { fatal: true })
  let bytes = 0, text = '', completed = false
  try {
    while (true) {
      const { done, value } = await abortable(reader.read(), signal)
      if (done) { completed = true; return text + decoder.decode() }
      bytes += value.byteLength
      if (bytes > MCP_MAX_BYTES) throw new McpCheckError('RESPONSE_TOO_LARGE')
      text += decoder.decode(value, { stream: true })
    }
  } finally {
    if (!completed) void reader.cancel().catch(() => {})
    reader.releaseLock()
  }
}

export async function runMcpCheck(options: {
  metadata: McpMetadata; token: string; signal: AbortSignal
  onStep?: (steps: CheckStep[]) => void
  fetch?: typeof fetch; now?: () => number
}): Promise<McpCheckResult> {
  const metadata = parseMcpMetadata(options.metadata), steps = initialMcpSteps()
  const now = options.now ?? Date.now, deadline = now() + MCP_TOTAL_TIMEOUT
  const transport = options.fetch ?? globalThis.fetch
  let current: CheckStepKey = 'initialize', discoveredTools: string[] = [], discoveredResources: string[] = []
  const emit = () => options.onStep?.(steps.map(s => ({ ...s })))
  const mark = (key: CheckStepKey, status: CheckStep['status']) => { steps.find(s => s.key === key)!.status = status; emit() }
  async function call(method: string, id: number | null, params?: Record<string, unknown>) {
    if (options.signal.aborted) throw new McpCheckError('CANCELLED')
    const remaining = deadline - now()
    if (remaining <= 0) throw new McpCheckError('TIMEOUT')
    const controller = new AbortController()
    const cancel = () => controller.abort('cancelled')
    options.signal.addEventListener('abort', cancel, { once: true })
    const timer = setTimeout(() => controller.abort('timeout'), Math.min(MCP_REQUEST_TIMEOUT, remaining))
    try {
      const headers: Record<string, string> = { 'Content-Type': 'application/json', Accept: 'application/json, text/event-stream', Authorization: `Bearer ${options.token}` }
      if (method !== 'initialize') headers['MCP-Protocol-Version'] = metadata.targetProtocolVersion
      const response = await abortable(transport('/mcp', { method: 'POST', headers, credentials: 'omit', cache: 'no-store', redirect: 'error',
        signal: controller.signal, body: JSON.stringify({ jsonrpc: '2.0', ...(id !== null ? { id } : {}), method, ...(params ? { params } : {}) }) }), controller.signal)
      if (response.redirected || (response.status >= 300 && response.status < 400)) throw new McpCheckError('REDIRECT_REJECTED')
      if (!response.ok) throw new McpCheckError(({ 401: 'AUTH_REQUIRED', 403: 'ACCESS_REJECTED', 404: 'ENDPOINT_UNAVAILABLE', 405: 'METHOD_REJECTED', 406: 'ACCEPT_REJECTED', 415: 'CONTENT_TYPE_REJECTED', 429: 'RATE_LIMITED' } as Record<number, string>)[response.status] ?? 'SERVER_ERROR')
      if (response.headers.has('MCP-Session-Id')) throw new McpCheckError('PROTOCOL_INCOMPATIBLE')
      const text = await boundedText(response, controller.signal)
      if (now() > deadline) throw new McpCheckError('TIMEOUT')
      if (id === null) {
        if (response.status !== 202 || text.trim()) throw new McpCheckError('PROTOCOL_INCOMPATIBLE')
        return {}
      }
      if (response.headers.get('Content-Type')?.split(';')[0]?.trim().toLowerCase() !== 'application/json') throw new McpCheckError('PROTOCOL_INCOMPATIBLE')
      let body: unknown
      try { body = JSON.parse(text) } catch { throw new McpCheckError('PROTOCOL_INCOMPATIBLE') }
      if (!object(body) || body.jsonrpc !== '2.0' || body.id !== id || Object.prototype.hasOwnProperty.call(body, 'error') || !object(body.result)) throw new McpCheckError('PROTOCOL_INCOMPATIBLE')
      return body.result
    } finally { clearTimeout(timer); options.signal.removeEventListener('abort', cancel); controller.abort('finished') }
  }
  try {
    if (!metadata.enabled || !options.token) throw new McpCheckError(metadata.enabled ? 'AUTH_REQUIRED' : 'MCP_DISABLED')
    mark('initialize', 'running')
    const initialized = await call('initialize', 1, { protocolVersion: metadata.targetProtocolVersion, capabilities: {}, clientInfo: { name: 'rag-browser-check', version: '1.0' } })
    if (initialized.protocolVersion !== metadata.targetProtocolVersion || !object(initialized.serverInfo)
      || initialized.serverInfo.name !== metadata.server.name || initialized.serverInfo.version !== metadata.server.version
      || !object(initialized.capabilities) || !object(initialized.capabilities.resources) || !object(initialized.capabilities.tools)
      || Object.keys(initialized.capabilities).some(key => !['resources', 'tools'].includes(key))
      || initialized.capabilities.resources.subscribe || initialized.capabilities.resources.listChanged || initialized.capabilities.tools.listChanged) throw new McpCheckError('PROTOCOL_INCOMPATIBLE')
    await call('notifications/initialized', null)
    mark('initialize', 'passed')
    current = 'ping'; mark(current, 'running'); await call('ping', 2, {}); mark(current, 'passed')
    current = 'tools'; mark(current, 'running')
    const tools = await call('tools/list', 3, {})
    if (tools.nextCursor) throw new McpCheckError('DISCOVERY_INCOMPLETE')
    if (!Array.isArray(tools.tools) || tools.tools.length > 4 || tools.tools.some(t => !object(t) || typeof t.name !== 'string' || !object(t.inputSchema))) throw new McpCheckError('PROTOCOL_INCOMPATIBLE')
    discoveredTools = tools.tools.map(t => t.name as string)
    if (!same(discoveredTools, metadata.configuredToolNames)) throw new McpCheckError('CATALOG_CHANGED')
    for (const tool of tools.tools) {
      const expected = metadata.implementedTools.find(t => t.name === tool.name)!
      if (canonical(tool.inputSchema) !== canonical(expected.inputSchema) || canonical(tool.outputSchema) !== canonical(expected.outputSchema)) throw new McpCheckError('CATALOG_CHANGED')
    }
    mark(current, 'passed')
    current = 'resources'; mark(current, 'running')
    const resources = await call('resources/templates/list', 4, {})
    if (resources.nextCursor) throw new McpCheckError('DISCOVERY_INCOMPLETE')
    if (!Array.isArray(resources.resourceTemplates) || resources.resourceTemplates.length !== 3
      || resources.resourceTemplates.some(t => !object(t) || typeof t.name !== 'string' || !metadata.implementedResourceTemplates.some(expected => expected.name === t.name && expected.uriTemplate === t.uriTemplate && expected.mimeType === t.mimeType))) throw new McpCheckError('CATALOG_CHANGED')
    discoveredResources = resources.resourceTemplates.map(t => t.name as string)
    if (!same(discoveredResources, metadata.configuredResourceTemplateNames)) throw new McpCheckError('CATALOG_CHANGED')
    mark(current, 'passed')
    return { status: 'passed', category: 'CHECK_PASSED', steps, completedAt: new Date().toISOString(), toolNames: discoveredTools, resourceTemplateNames: discoveredResources }
  } catch (error) {
    const category = options.signal.aborted ? 'CANCELLED' : error instanceof McpCheckError ? error.category : 'NETWORK_ERROR'
    mark(current, category === 'CANCELLED' ? 'skipped' : 'failed')
    for (const step of steps) if (step.status === 'idle' || step.status === 'running') step.status = 'skipped'
    emit()
    return { status: category === 'CANCELLED' ? 'cancelled' : 'failed', category, steps, completedAt: new Date().toISOString(), toolNames: discoveredTools, resourceTemplateNames: discoveredResources }
  }
}

export function mcpCheckMessage(category: string): string {
  return ({ CHECK_PASSED: '本浏览器路径本次协议检查通过。', CANCELLED: '已取消本次检查。', MCP_DISABLED: '服务端未启用 MCP。',
    AUTH_REQUIRED: '登录状态失效，请重新登录后检查。', ACCESS_REJECTED: '请求被访问策略拒绝，请核对 Origin 和本机限制。',
    ENDPOINT_UNAVAILABLE: '协议入口不可用，请核对服务配置和 /mcp 代理路径。', METHOD_REJECTED: '请求方法不兼容，请核对客户端与代理。',
    ACCEPT_REJECTED: '响应格式协商失败，请核对客户端与代理。', CONTENT_TYPE_REJECTED: '请求内容类型不兼容，请核对客户端与代理。',
    RATE_LIMITED: '请求过于频繁，请稍后手动检查。', TIMEOUT: '检查超时，请核对服务后手动重试。', RESPONSE_TOO_LARGE: '响应超过大小限制，已停止检查。',
    REDIRECT_REJECTED: '协议入口发生重定向，已停止发送凭据，请核对代理路径。', PROTOCOL_INCOMPATIBLE: '响应不符合当前协议约定，请核对服务版本与代理。',
    DISCOVERY_INCOMPLETE: '能力发现未完成，本次不自动翻页，请核对服务配置。', CATALOG_CHANGED: '实际目录与配置快照不同，请刷新接入信息后重新检查。',
    SERVER_ERROR: '服务暂不可用，请核对服务后手动重试。', METADATA_INVALID: '接入信息格式不兼容，请核对后端版本。',
  } as Record<string, string>)[category] ?? '网络连接异常，请核对服务后手动重试。'
}
