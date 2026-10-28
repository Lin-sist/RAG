export function mcpMetadata(enabled = true, external = false) {
  const names = ['rag.search', 'rag.ask', 'rag.get-citation', 'rag.compare-sources']
  const titles = ['检索知识库', '基于知识库回答', '读取指定引用', '并排查看两份来源']
  const templates = [
    ['knowledge-base', 'rag://knowledge-bases/{kbId}', 'application/json'],
    ['knowledge-base-document', 'rag://knowledge-bases/{kbId}/documents/{documentId}', 'application/json'],
    ['knowledge-base-document-chunk', 'rag://knowledge-bases/{kbId}/documents/{documentId}/chunks/{chunkIndex}', 'text/plain; charset=utf-8'],
  ]
  return { enabled, externalToolsConfigured: external, externalToolsEffective: enabled && external, localOnly: true,
    endpointPath: '/mcp', transport: 'streamable-http', sessionMode: 'stateless', authMode: 'project-access-jwt', oauthSupported: false,
    targetProtocolVersion: '2025-11-25', server: { name: 'enterprise-rag-readonly', version: 'c15-v1' }, originPolicy: 'exact-allowlist',
    implementedTools: names.map((name, i) => ({ name, title: titles[i], description: 'Synthetic description', inputSchema: { type: 'object', additionalProperties: false, properties: {} }, outputSchema: { type: 'object' }, readOnly: true, externalCallPossible: i < 2 })),
    configuredToolNames: enabled ? external ? names : names.slice(2) : [],
    implementedResourceTemplates: templates.map(([name, uriTemplate, mimeType]) => ({ name, title: name, description: 'Synthetic template', uriTemplate, mimeType })),
    configuredResourceTemplateNames: enabled ? templates.map(t => t[0]) : [] }
}

export function mcpReply(request, metadata = mcpMetadata()) {
  const result = {
    initialize: { protocolVersion: metadata.targetProtocolVersion, serverInfo: metadata.server, capabilities: { resources: { subscribe: false, listChanged: false }, tools: { listChanged: false } } },
    ping: {},
    'tools/list': { tools: metadata.implementedTools.filter(t => metadata.configuredToolNames.includes(t.name)) },
    'resources/templates/list': { resourceTemplates: metadata.implementedResourceTemplates },
  }[request.method]
  return { jsonrpc: '2.0', id: request.id, result }
}
