interface ResponseMetadata {
    code: number
    message: string
    traceId: string
    timestamp: string
}

// 无载荷成功响应（删除、退出）允许省略 data。
export type ApiResponse<T> = ResponseMetadata & ([T] extends [void] ? { data?: T } : { data: T })

export interface PageResult<T> {
    records: T[]
    total: number
    page: number
    size: number
    totalPages: number
}
