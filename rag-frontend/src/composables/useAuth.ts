import { useAuthStore } from '@/stores/auth'
import { login as loginApi, logout as logoutApi, refreshToken as refreshApi } from '@/api/auth'
import type { LoginRequest } from '@/types/auth'
import router from '@/router'

export function useAuth() {
    const authStore = useAuthStore()

    async function login(data: LoginRequest) {
        const res = await loginApi(data)
        const authData = res.data.data
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
        if (!authStore.refreshToken) throw new Error('没有 refreshToken')
        const res = await refreshApi(authStore.refreshToken)
        const authData = res.data.data
        authStore.setAuthTokens(authData.accessToken, authData.refreshToken)
        return authData
    }

    return { login, logout, refresh }
}
