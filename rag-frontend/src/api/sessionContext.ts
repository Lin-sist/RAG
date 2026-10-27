import request from './request'
import type { ApiResponse } from '@/types/api'
import type { SessionContext } from '@/types/sessionContext'

export async function getSessionContext(): Promise<SessionContext> {
    return (await request.get<ApiResponse<SessionContext>>('/api/session/context')).data.data
}
