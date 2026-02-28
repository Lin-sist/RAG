import request from './request'
import type { ApiResponse, PageResult } from '@/types/api'
import type { QAHistoryDTO, FeedbackRequest } from '@/types/history'

export function getHistoryPage(page: number, size: number) {
    return request.get<ApiResponse<PageResult<QAHistoryDTO>>>('/api/qa/history', { params: { page, size } })
}

export function getHistoryById(id: number) {
    return request.get<ApiResponse<QAHistoryDTO>>(`/api/qa/history/${id}`)
}

export function deleteHistory(id: number) {
    return request.delete<ApiResponse<void>>(`/api/qa/history/${id}`)
}

export function submitFeedback(historyId: number, data: FeedbackRequest) {
    return request.post<ApiResponse<void>>(`/api/qa/history/${historyId}/feedback`, data)
}
