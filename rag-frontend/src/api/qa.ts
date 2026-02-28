import request from './request'
import type { ApiResponse } from '@/types/api'
import type { QAResponse, AskRequest } from '@/types/qa'

export function ask(data: AskRequest) {
    return request.post<ApiResponse<QAResponse>>('/api/qa/ask', data)
}

export function askSimple(data: AskRequest) {
    return request.post<ApiResponse<QAResponse>>('/api/qa/ask/simple', data)
}

// 流式问答通过 composables/useSSE.ts 实现
