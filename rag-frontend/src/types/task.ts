export interface TaskStatusResponse {
    taskId: string
    taskType: string
    state: TaskState
    progress: number
    message: string
    result: unknown
    error: string | null
    createdAt: string
    updatedAt: string
}

export type TaskState = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
