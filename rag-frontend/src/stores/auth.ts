import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getToken, setToken, removeToken } from '@/utils/storage'
import type { ContextStatus, SessionContext } from '@/types/sessionContext'

export const useAuthStore = defineStore('auth', () => {
    const accessToken = ref<string>(getToken('accessToken') || '')
    const refreshToken = ref<string>(getToken('refreshToken') || '')
    const userInfo = ref<{ id: number; username: string; email: string } | null>(null)

    const sessionRevision = ref(0)
    const tokenRevision = ref(0)
    const workspace = ref<SessionContext['workspace'] | null>(null)
    const contextStatus = ref<ContextStatus>('idle')
    const isLoggedIn = computed(() => !!accessToken.value)

    function setAuthTokens(access: string, refresh: string, rotating = false) {
        workspace.value = null
        contextStatus.value = 'idle'
        if (!rotating) { sessionRevision.value++; userInfo.value = null }
        accessToken.value = access
        refreshToken.value = refresh
        setToken('accessToken', access)
        setToken('refreshToken', refresh)
        tokenRevision.value++
    }

    function setUserInfo(info: { id: number; username: string; email: string }) {
        userInfo.value = info
    }

    function clearAuth() {
        sessionRevision.value++
        accessToken.value = ''
        refreshToken.value = ''
        userInfo.value = null
        workspace.value = null
        contextStatus.value = 'idle'
        removeToken('accessToken')
        removeToken('refreshToken')
    }

    function beginContextLoad() { workspace.value = null; contextStatus.value = 'loading' }
    function acceptContext(context: SessionContext) {
        userInfo.value = { ...context.userInfo, email: context.userInfo.email ?? '' }
        workspace.value = context.workspace
        contextStatus.value = 'ready'
    }
    function failContextLoad() { workspace.value = null; contextStatus.value = 'error' }

    return { accessToken, refreshToken, userInfo, sessionRevision, tokenRevision, workspace, contextStatus,
        isLoggedIn, setAuthTokens, setUserInfo, clearAuth, beginContextLoad, acceptContext, failContextLoad }
})
