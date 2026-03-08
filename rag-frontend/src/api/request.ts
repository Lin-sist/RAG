import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse, type AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, setToken, clearTokens } from '@/utils/storage'
import router from '@/router'

const request: AxiosInstance = axios.create({
    baseURL: '',
    timeout: 30000,
    headers: { 'Content-Type': 'application/json' },
})

// ---------- 请求拦截器：自动带上 Token ----------
request.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        const token = getToken('accessToken')
        if (token && config.headers) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    (error) => Promise.reject(error)
)

// ---------- Token 刷新相关状态 ----------
let isRefreshing = false                         // 是否正在刷新 Token
let pendingRequests: Array<(token: string) => void> = []  // 等待刷新完成的请求队列

/**
 * 将请求加入等待队列，等 Token 刷新完成后自动重发
 */
function addPendingRequest(config: InternalAxiosRequestConfig): Promise<AxiosResponse> {
    return new Promise((resolve) => {
        pendingRequests.push((newToken: string) => {
            config.headers.Authorization = `Bearer ${newToken}`
            resolve(request(config))
        })
    })
}

/**
 * 用 refreshToken 获取新的 accessToken
 * 注意：这里用独立的 axios 实例发请求，避免被拦截器循环拦截
 */
async function refreshAccessToken(): Promise<string | null> {
    const refreshTokenStr = getToken('refreshToken')
    if (!refreshTokenStr) return null

    try {
        const res = await axios.post('/auth/refresh', { refreshToken: refreshTokenStr })
        const data = res.data?.data
        if (data?.accessToken) {
            setToken('accessToken', data.accessToken)
            if (data.refreshToken) setToken('refreshToken', data.refreshToken)
            return data.accessToken
        }
        return null
    } catch {
        return null
    }
}

/**
 * 处理 Token 过期：尝试刷新，成功则重发失败请求，失败则跳登录页
 */
async function handleTokenExpired(error: AxiosError): Promise<AxiosResponse> {
    const originalConfig = error.config as InternalAxiosRequestConfig

    // 如果是 refresh 接口本身返回 401，直接跳登录
    if (originalConfig.url?.includes('/auth/refresh')) {
        forceLogout()
        return Promise.reject(error)
    }

    // 如果已经在刷新中，把当前请求加入等待队列
    if (isRefreshing) {
        return addPendingRequest(originalConfig)
    }

    isRefreshing = true
    const newToken = await refreshAccessToken()
    isRefreshing = false

    if (newToken) {
        // 刷新成功：释放所有排队的请求
        pendingRequests.forEach((cb) => cb(newToken))
        pendingRequests = []

        // 重发当前失败的请求
        originalConfig.headers.Authorization = `Bearer ${newToken}`
        return request(originalConfig)
    } else {
        // 刷新失败：全部放弃，跳登录
        pendingRequests = []
        forceLogout()
        return Promise.reject(error)
    }
}

/**
 * 清除登录态并跳转到登录页
 */
function forceLogout() {
    clearTokens()
    ElMessage.error('登录已过期，请重新登录')
    router.push('/login')
}

// ---------- 响应拦截器 ----------
request.interceptors.response.use(
    (response: AxiosResponse) => {
        const data = response.data
        // 后端业务层面的错误（code !== 200）
        if (data.code !== undefined && data.code !== 200) {
            ElMessage.error(data.message || '请求失败')
            return Promise.reject(new Error(data.message || '请求失败'))
        }
        return response
    },
    async (error: AxiosError) => {
        if (error.response) {
            const { status, data } = error.response as { status: number; data: any }
            switch (status) {
                case 401:
                    // 尝试用 refreshToken 自动续期
                    return handleTokenExpired(error)
                case 403:
                    ElMessage.error('没有权限访问')
                    break
                case 404:
                    ElMessage.error(data?.message || '请求的资源不存在')
                    break
                default:
                    ElMessage.error(data?.message || `请求错误 (${status})`)
            }
        } else if (error.request) {
            ElMessage.error('网络错误，请检查网络连接')
        } else {
            ElMessage.error('请求配置错误')
        }
        return Promise.reject(error)
    }
)

export default request
