import { useAuthStore } from '@/stores/auth'
import { login as loginApi, logout as logoutApi } from '@/api/auth'
import type { LoginRequest } from '@/types/auth'
import router from '@/router'
import { refreshSession } from '@/api/request'
import { ApiError } from '@/api/errors'

export function useAuth() {
    const authStore = useAuthStore()

    async function login(data: LoginRequest) {
        const res = await loginApi(data)
        const authData = res.data.data
        if (!authData?.accessToken || !authData.refreshToken || !authData.userInfo) throw new ApiError('认证响应不完整，请重新登录')
        authStore.setAuthTokens(authData.accessToken, authData.refreshToken)
        authStore.setUserInfo(authData.userInfo)
        return authData
    }

    async function logout() {
        try { await logoutApi() } finally {
            authStore.clearAuth()
            router.push('/login')
        }
    }

    async function refresh() {
        return refreshSession()
    }

    return { login, logout, refresh }
}
