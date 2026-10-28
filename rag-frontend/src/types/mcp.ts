export interface McpTool {
  name: string
  title: string
  description: string
  inputSchema: Record<string, unknown>
  outputSchema: Record<string, unknown>
  readOnly: boolean
  externalCallPossible: boolean
}
export interface McpResourceTemplate {
  name: string
  title: string
  description: string
  uriTemplate: string
  mimeType: string
}
export interface McpMetadata {
  enabled: boolean
  externalToolsConfigured: boolean
  externalToolsEffective: boolean
  localOnly: boolean
  endpointPath: '/mcp'
  transport: 'streamable-http'
  sessionMode: 'stateless'
  authMode: 'project-access-jwt'
  oauthSupported: false
  targetProtocolVersion: '2025-11-25'
  server: { name: string; version: string }
  originPolicy: 'exact-allowlist'
  implementedTools: McpTool[]
  configuredToolNames: string[]
  implementedResourceTemplates: McpResourceTemplate[]
  configuredResourceTemplateNames: string[]
}
export type CheckStepKey = 'initialize' | 'ping' | 'tools' | 'resources'
export type CheckStepStatus = 'idle' | 'running' | 'passed' | 'failed' | 'skipped'
export interface CheckStep { key: CheckStepKey; label: string; status: CheckStepStatus }
export interface McpCheckResult {
  status: 'passed' | 'failed' | 'cancelled'
  category: string
  steps: CheckStep[]
  completedAt: string
  toolNames: string[]
  resourceTemplateNames: string[]
}
