export interface QAHistoryDTO {
    id: number
    userId: number
    kbId: number
    kbName: string
    question: string
    answer: string
    citations: string
    latencyMs: number
    feedback?: QAFeedbackDTO
    createdAt: string
}

export interface QAFeedbackDTO {
    id: number
    historyId: number
    rating: number
    comment: string
    createdAt: string
}

export interface FeedbackRequest {
    rating: number
    comment?: string
}
