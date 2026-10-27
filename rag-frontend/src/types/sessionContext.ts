export interface SessionContext {
    userInfo: { id: number; username: string; email: string | null }
    workspace: { id: string; displayName: string }
}

export type ContextStatus = 'idle' | 'loading' | 'ready' | 'error'
