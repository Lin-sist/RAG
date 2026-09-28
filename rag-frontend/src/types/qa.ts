export interface AskRequest {
    kbId: number
    question: string
    topK?: number
    minScore?: number
    filter?: Record<string, unknown>
    enableCache?: boolean
}

export interface Citation {
    source: string
    sourceFileName?: string | null
    documentTitle?: string | null
    documentId?: number | null
    chunkId?: string | null
    score?: number | null
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
    citations: Citation[] | null
    contexts: RetrievedContext[] | null
    metadata: Record<string, unknown> | null
}
