import type { Citation } from './qa'

export interface QAHistoryDTO {
    id: number
    userId: number
    kbId: number
    question: string
    answer: string
    citations: Citation[]
    traceId: string
    latencyMs: number
    createdAt: string
}

export interface QAFeedbackDTO {
    id: number
    qaId: number
    userId: number
    rating: number
    comment: string
    createdAt: string
}

export interface FeedbackRequest {
    rating: number
    comment?: string
}
