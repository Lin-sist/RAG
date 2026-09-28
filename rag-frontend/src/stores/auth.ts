import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getToken, setToken, removeToken } from '@/utils/storage'

export const useAuthStore = defineStore('auth', () => {
    const accessToken = ref<string>(getToken('accessToken') || '')
    const refreshToken = ref<string>(getToken('refreshToken') || '')
    const userInfo = ref<{ id: number; username: string; email: string } | null>(null)

    const sessionRevision = ref(0)
    const isLoggedIn = computed(() => !!accessToken.value)

    function setAuthTokens(access: string, refresh: string, rotating = false) {
        if (!rotating) sessionRevision.value++
        accessToken.value = access
        refreshToken.value = refresh
        setToken('accessToken', access)
        setToken('refreshToken', refresh)
    }

    function setUserInfo(info: { id: number; username: string; email: string }) {
        userInfo.value = info
    }

    function clearAuth() {
        sessionRevision.value++
        accessToken.value = ''
        refreshToken.value = ''
        userInfo.value = null
        removeToken('accessToken')
        removeToken('refreshToken')
    }

    return { accessToken, refreshToken, userInfo, sessionRevision, isLoggedIn, setAuthTokens, setUserInfo, clearAuth }
})
