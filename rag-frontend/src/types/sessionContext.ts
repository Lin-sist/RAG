export interface SessionContext {
    userInfo: { id: number; username: string; email: string | null; displayName?: string | null; hasAvatar?: boolean; profileRevision?: number }
    workspace: { id: string; displayName: string }
}

export type ContextStatus = 'idle' | 'loading' | 'ready' | 'error'
