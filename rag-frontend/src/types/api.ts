export interface ApiResponse<T> {
    code: number
    message: string
    data: T
    traceId: string
    timestamp: string
}

export interface PageResult<T> {
    records: T[]
    total: number
    page: number
    size: number
    totalPages: number
}
