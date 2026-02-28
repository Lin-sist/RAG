export interface ApiResponse<T> {
    code: number
    message: string
    data: T
    traceId: string
    timestamp: string
}

export interface PageResult<T> {
    content: T[]
    totalElements: number
    totalPages: number
    size: number
    number: number
}
