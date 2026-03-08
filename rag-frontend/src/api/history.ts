import request from './request'
import type { ApiResponse, PageResult } from '@/types/api'
import type { QAHistoryDTO, QAFeedbackDTO, FeedbackRequest } from '@/types/history'

export function getHistoryPage(page: number, size: number, kbId?: number) {
    return request.get<ApiResponse<PageResult<QAHistoryDTO>>>('/api/history', {
        params: { page, size, ...(kbId ? { kbId } : {}) },
    })
}

export function getHistoryById(id: number) {
    return request.get<ApiResponse<QAHistoryDTO>>(`/api/history/${id}`)
}

export function deleteHistory(id: number) {
    return request.delete<ApiResponse<void>>(`/api/history/${id}`)
}

export function submitFeedback(historyId: number, data: FeedbackRequest) {
    return request.post<ApiResponse<QAFeedbackDTO>>(`/api/history/${historyId}/feedback`, data)
}

export function getFeedback(historyId: number) {
    return request.get<ApiResponse<QAFeedbackDTO[]>>(`/api/history/${historyId}/feedback`)
}

export function getMyFeedbacks() {
    return request.get<ApiResponse<QAFeedbackDTO[]>>('/api/history/feedback/my')
}
