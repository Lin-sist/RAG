import axios from 'axios'

export class ApiError extends Error {
    constructor(message: string, public readonly status?: number,
        public readonly errorCode?: string, public readonly traceId?: string,
        public readonly retryAfter?: string) {
        super(message)
        this.name = 'ApiError'
    }
}

export function normalizeError(error: unknown): ApiError {
    if (error instanceof ApiError) return error
    if (axios.isAxiosError(error)) {
        const status = error.response?.status
        const body = error.response?.data
        const defaults: Record<number, string> = { 401: '登录凭据无效或已过期', 403: '没有权限访问', 404: '请求的资源不存在', 429: '请求过于频繁，请稍后重试', 503: '服务暂不可用，请稍后重试' }
        return new ApiError(typeof body?.message === 'string' ? body.message :
            (status ? defaults[status] || `请求错误 (${status})` : '网络错误，请检查网络连接'),
            status, typeof body?.errorCode === 'string' ? body.errorCode : error.code,
            typeof body?.traceId === 'string' ? body.traceId : undefined,
            error.response?.headers?.['retry-after']?.toString())
    }
    return new ApiError('请求处理失败，请重试')
}
