export interface AskRequest {
    kbId: number
    question: string
    topK?: number
    filter?: Record<string, unknown>
    enableCache?: boolean
}

export interface Citation {
    source: string
    content: string
    score?: number
}

export interface RetrievedContext {
    content: string
    source: string
    score: number
    metadata: Record<string, unknown>
}

export interface QAResponse {
    question: string
    answer: string
    citations: Citation[]
    contexts: RetrievedContext[]
    metadata: Record<string, unknown>
}
