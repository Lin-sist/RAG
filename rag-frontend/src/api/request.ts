import { useAuthStore } from '@/stores/auth'
import router from '@/router'
import { createRequestClient } from './requestClient'

const client = createRequestClient({
    revision: () => useAuthStore().sessionRevision,
    access: () => useAuthStore().accessToken,
    refresh: () => useAuthStore().refreshToken,
    save: data => {
        const store = useAuthStore()
        store.setAuthTokens(data.accessToken, data.refreshToken, true)
        store.setUserInfo(data.userInfo)
    },
    clear: () => useAuthStore().clearAuth(),
    expired: () => { void router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } }) },
})

export const refreshSession = client.refreshSession
export default client.request
