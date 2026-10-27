import { useAuthStore } from '@/stores/auth'
import { login as loginApi, logout as logoutApi } from '@/api/auth'
import type { LoginRequest } from '@/types/auth'
import router from '@/router'
import { refreshSession } from '@/api/request'
import { ApiError } from '@/api/errors'
import { computed, ref, type Ref } from 'vue'

type LogoutResult = 'confirmed' | 'unconfirmed' | 'superseded'
const logoutFlights = new WeakMap<object, { revision: number; promise: Promise<LogoutResult> }>()
const logoutRevisions = new WeakMap<object, Ref<number | null>>()

export function useAuth() {
    const authStore = useAuthStore()
    let logoutRevision = logoutRevisions.get(authStore)
    if (!logoutRevision) { logoutRevision = ref(null); logoutRevisions.set(authStore, logoutRevision) }
    const pendingRevision = logoutRevision
    const logoutPending = computed(() => pendingRevision.value === authStore.sessionRevision)

    async function login(data: LoginRequest) {
        const res = await loginApi(data)
        const authData = res.data.data
        if (!authData?.accessToken || !authData.refreshToken || !authData.userInfo) throw new ApiError('认证响应不完整，请重新登录')
        authStore.setAuthTokens(authData.accessToken, authData.refreshToken)
        authStore.setUserInfo(authData.userInfo)
        return authData
    }

    function logout(): Promise<LogoutResult> {
        const revision = authStore.sessionRevision
        const existing = logoutFlights.get(authStore)
        if (existing?.revision === revision) return existing.promise
        pendingRevision.value = revision
        const promise = (async (): Promise<LogoutResult> => {
            let result: LogoutResult = 'confirmed'
            try { await logoutApi() } catch { result = 'unconfirmed' }
            // 请求属于发起退出的会话；迟到结果不能清除后来登录的身份。
            if (authStore.sessionRevision !== revision) return 'superseded'
            authStore.clearAuth()
            await router.replace('/login')
            return result
        })()
        logoutFlights.set(authStore, { revision, promise })
        const release = () => {
            if (logoutFlights.get(authStore)?.promise === promise) { logoutFlights.delete(authStore); pendingRevision.value = null }
        }
        void promise.then(release, release)
        return promise
    }

    async function refresh() {
        return refreshSession()
    }

    return { login, logout, logoutPending, refresh }
}
