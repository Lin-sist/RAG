import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { useAuthStore } from './auth'
import { getAvatar } from '@/api/profile'

/** Private images stay in memory. Keys include login and profile revisions. */
export const useProfileAvatarStore = defineStore('profile-avatar', () => {
    const auth = useAuthStore(), url = ref(''), unavailable = ref(false)
    let generation = 0, loaded = '', flight: Promise<void> | undefined
    const key = () => `${auth.sessionRevision}:${auth.userInfo?.id}:${auth.userInfo?.profileRevision}`
    function clear() {
        generation++; loaded = ''; flight = undefined; unavailable.value = false
        if (url.value) URL.revokeObjectURL(url.value)
        url.value = ''
    }
    function publish(blob: Blob, revision: number) {
        if (!auth.isLoggedIn || !auth.userInfo?.hasAvatar || auth.userInfo.profileRevision !== revision) return
        if (blob.type !== 'image/png' || blob.size > 512 * 1024) return
        const next = URL.createObjectURL(blob), old = url.value
        generation++; loaded = key(); flight = undefined; unavailable.value = false
        url.value = next
        if (old) URL.revokeObjectURL(old)
    }
    function load(force = false): Promise<void> {
        if (!auth.isLoggedIn || !auth.userInfo?.hasAvatar) { clear(); return Promise.resolve() }
        if (!force && loaded === key()) return flight ?? Promise.resolve()
        const owner = key(), request = ++generation
        loaded = owner; unavailable.value = false
        flight = getAvatar().then(async blob => {
            const next = URL.createObjectURL(blob)
            try {
                const image = new Image(); image.src = next; await image.decode()
                if (generation !== request || key() !== owner) { URL.revokeObjectURL(next); return }
                const old = url.value; url.value = next
                if (old) URL.revokeObjectURL(old)
            } catch { URL.revokeObjectURL(next); throw new Error('Invalid avatar') }
        }).catch(() => { if (generation === request && key() === owner) { unavailable.value = true; loaded = '' } })
            .finally(() => { if (generation === request) flight = undefined })
        return flight
    }
    watch(() => auth.sessionRevision, clear, { flush: 'sync' })
    watch(() => [auth.userInfo?.hasAvatar, auth.userInfo?.profileRevision], () => { void load() })
    return { url, unavailable, clear, load, publish }
})
