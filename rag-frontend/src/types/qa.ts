export interface AskRequest {
    kbId: number
    question: string
    topK?: number
    filter?: Record<string, unknown>
    enableCache?: boolean
}

export interface Citation {
    source: string
    snippet: string
    startIndex: number
    endIndex: number
}

export interface RetrievedContext {
    content: string
    source: string
    relevanceScore: number
    metadata: Record<string, unknown>
}

export interface QAResponse {
    question: string
    answer: string
    citations: Citation[]
    contexts: RetrievedContext[]
    metadata: Record<string, unknown>
}
