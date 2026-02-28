import request from './request'
import type { ApiResponse } from '@/types/api'
import type { TaskStatusResponse } from '@/types/task'

export function getTaskStatus(taskId: string) {
    return request.get<ApiResponse<TaskStatusResponse>>(`/api/tasks/${taskId}`)
}

export function getTaskResult(taskId: string) {
    return request.get<ApiResponse<TaskStatusResponse>>(`/api/tasks/${taskId}/result`)
}

export function cancelTask(taskId: string) {
    return request.post<ApiResponse<void>>(`/api/tasks/${taskId}/cancel`)
}
