import type { InjectionKey } from 'vue'
import type { ContextStatus, SessionContext } from '../types/sessionContext'

export const retrySessionContextKey: InjectionKey<() => Promise<void>> = Symbol('retry-session-context')

interface Owner {
    sessionRevision: number
    tokenRevision: number
    isLoggedIn: boolean
    userInfo: { id: number } | null
    contextStatus: ContextStatus
    beginContextLoad(): void
    acceptContext(context: SessionContext): void
    failContextLoad(): void
}

function validContext(value: SessionContext): boolean {
    return !!value?.userInfo && Number.isSafeInteger(value.userInfo.id) && value.userInfo.id > 0
        && typeof value.userInfo.username === 'string' && !!value.userInfo.username.trim()
        && (value.userInfo.email === null || typeof value.userInfo.email === 'string')
        && (value.userInfo.displayName == null || typeof value.userInfo.displayName === 'string')
        && (value.userInfo.hasAvatar === undefined || typeof value.userInfo.hasAvatar === 'boolean')
        && (value.userInfo.profileRevision === undefined || (Number.isSafeInteger(value.userInfo.profileRevision) && value.userInfo.profileRevision >= 0))
        && typeof value.workspace?.id === 'string' && /^[1-9]\d*$/.test(value.workspace.id)
        && typeof value.workspace.displayName === 'string' && !!value.workspace.displayName.trim()
}

/** A generation belongs to both the login and the current token rotation. */
export function createSessionContextLoader(owner: Owner, read: () => Promise<SessionContext>, identityChanged: () => void) {
    let generation = 0
    let flight: { key: string; promise: Promise<void> } | undefined
    let confirmed: { revision: number; userId: number; workspaceId: string } | undefined
    let completedKey: string | undefined

    function load(): Promise<void> {
        if (!owner.isLoggedIn) return Promise.resolve()
        const revision = owner.sessionRevision, rotation = owner.tokenRevision
        const key = `${revision}:${rotation}`
        if (flight?.key === key) return flight.promise
        if (completedKey === key && owner.contextStatus === 'ready') return Promise.resolve()
        const request = ++generation, expectedUser = owner.userInfo?.id
        const current = () => owner.isLoggedIn && owner.sessionRevision === revision
            && owner.tokenRevision === rotation && generation === request
        owner.beginContextLoad()
        // Defer the read so synchronous failures use the same completion path.
        const promise = Promise.resolve().then(read).then(value => {
            if (!current()) return
            if (!validContext(value)) throw new Error('Invalid session context')
            const prior = confirmed?.revision === revision ? confirmed : undefined
            if ((expectedUser !== undefined && expectedUser !== value.userInfo.id)
                || (prior && (prior.userId !== value.userInfo.id || prior.workspaceId !== value.workspace.id))) {
                identityChanged()
                return
            }
            confirmed = { revision, userId: value.userInfo.id, workspaceId: value.workspace.id }
            completedKey = key
            owner.acceptContext(value)
        }).catch(() => {
            if (current()) owner.failContextLoad()
        }).finally(() => {
            if (flight?.promise === promise) flight = undefined
        })
        flight = { key, promise }
        return promise
    }

    function dispose() { generation++; flight = undefined; completedKey = undefined; confirmed = undefined }
    return { load, dispose }
}
