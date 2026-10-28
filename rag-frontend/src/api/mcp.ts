import request from './request'
import type { ApiResponse } from '@/types/api'
import type { McpMetadata } from '@/types/mcp'
import { parseMcpMetadata } from '@/utils/mcpCheck'

export async function getMcpMetadata(signal?: AbortSignal): Promise<McpMetadata> {
  const response = await request.get<ApiResponse<unknown>>('/api/integrations/mcp', { signal })
  return parseMcpMetadata(response.data.data)
}
