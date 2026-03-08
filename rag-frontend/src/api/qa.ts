import request from './request'
import type { ApiResponse } from '@/types/api'
import type { QAResponse, AskRequest } from '@/types/qa'

export function ask(data: AskRequest) {
    return request.post<ApiResponse<QAResponse>>('/api/qa/ask', data)
}

export function askSimple(kbId: number, question: string, topK?: number) {
    return request.get<ApiResponse<QAResponse>>('/api/qa/ask', {
        params: { kbId, question, ...(topK ? { topK } : {}) },
    })
}

// 流式问答通过 composables/useSSE.ts 实现，POST /api/qa/ask/stream
