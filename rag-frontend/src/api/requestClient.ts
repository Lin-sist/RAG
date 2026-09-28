import axios, { type AxiosAdapter, type InternalAxiosRequestConfig } from 'axios'
import type { AuthResponse } from '../types/auth'
import { ApiError, normalizeError } from './errors'

interface Session {
    revision(): number
    access(): string
    refresh(): string
    save(data: AuthResponse): void
    clear(): void
    expired(): void
}
type RetryConfig = InternalAxiosRequestConfig & { authRetried?: boolean; authRevision?: number }

// 唯一 HTTP client；Session 注入仅用于隔离 Pinia/router 和离线适配器测试。
export function createRequestClient(session: Session, adapter?: AxiosAdapter) {
    const request = axios.create({ baseURL: '', timeout: 30000,
        headers: { 'Content-Type': 'application/json' }, ...(adapter ? { adapter } : {}) })
    let refreshing: Promise<AuthResponse> | null = null
    function expire() {
        const hadSession = !!(session.access() || session.refresh())
        session.clear()
        if (hadSession) session.expired()
    }
    function refreshSession(): Promise<AuthResponse> {
        if (refreshing) return refreshing
        const token = session.refresh()
        const revision = session.revision()
        refreshing = (async () => {
            try {
                if (!token) throw new ApiError('登录已过期，请重新登录', 401)
                const response = await request.post('/auth/refresh', { refreshToken: token })
                const data = response.data?.data as AuthResponse | undefined
                if (!data?.accessToken || !data.refreshToken || !data.userInfo) {
                    throw new ApiError('认证响应不完整，请重新登录', response.status)
                }
                // 退出或重新登录后，不允许旧 refresh 覆盖新会话。
                if (session.refresh() !== token || session.revision() !== revision) throw new ApiError('登录状态已变更', 401)
                session.save(data)
                return data
            } catch (error) {
                if (session.refresh() === token && session.revision() === revision) expire()
                throw normalizeError(error)
            } finally {
                // 无 token 的同步失败也必须释放 single-flight。
                queueMicrotask(() => { refreshing = null })
            }
        })()
        return refreshing
    }
    request.interceptors.request.use(config => {
        const retryConfig = config as RetryConfig
        retryConfig.authRevision ??= session.revision()
        if (retryConfig.authRevision !== session.revision()) throw new ApiError('登录状态已变更', 401)
        const token = session.access()
        if (token) config.headers.Authorization = `Bearer ${token}`
        else delete config.headers.Authorization
        return config
    })
    request.interceptors.response.use(response => {
        const body = response.data
        if (typeof body?.code === 'number' && (body.code < 200 || body.code >= 300)) {
            throw new ApiError(body.message || '请求失败', response.status, String(body.code), body.traceId)
        }
        return response
    }, async error => {
        const normalized = normalizeError(error)
        const config = error.config as RetryConfig | undefined
        // 认证端点失败直接返回调用方；不刷新登录或退出请求。
        if (normalized.status !== 401 || !config || config.url?.startsWith('/auth/')) throw normalized
        if (config.authRevision !== session.revision()) throw normalized
        const sentToken = config.headers.Authorization
        const currentToken = session.access()
        if (config.authRetried) {
            if (sentToken === `Bearer ${currentToken}`) expire()
            throw normalized
        }
        config.authRetried = true
        if (!currentToken) throw normalized
        if (sentToken === `Bearer ${currentToken}`) await refreshSession()
        return request(config)
    })
    return { request, refreshSession }
}
