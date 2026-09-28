import type { QAHistoryDTO } from '@/types/history'
import type { Citation, QAResponse, RetrievedContext } from '@/types/qa'

export interface ChatPresentationMessage {
    id: string
    role: 'user' | 'assistant'
    content: string
    loading?: boolean
    error?: boolean
    citations?: Citation[]
    contexts?: RetrievedContext[]
    sourceHint?: string
}

export function normalizeCitations(citations: Citation[] | null | undefined): Citation[] {
    return Array.isArray(citations) ? citations.filter(Boolean) : []
}

export function normalizeContexts(contexts: RetrievedContext[] | null | undefined): RetrievedContext[] {
    return Array.isArray(contexts) ? contexts.filter(Boolean) : []
}

export function citationLabel(citation: Citation): string {
    return citation.documentTitle?.trim()
        || citation.sourceFileName?.trim()
        || citation.source?.trim()
        || '未知来源'
}

export function formatRelevanceScore(score: number | null | undefined): string | null {
    if (typeof score !== 'number' || !Number.isFinite(score)) return null
    return score >= 0 && score <= 1 ? `${Math.round(score * 100)}%` : score.toFixed(3)
}

export function applySyncResponse(message: ChatPresentationMessage, response: QAResponse): void {
    const responseStatus = typeof response.metadata?.status === 'string'
        ? response.metadata.status
        : undefined

    if (responseStatus === 'error') {
        message.content = response.answer || '问答生成失败，请稍后重试'
        message.loading = false
        message.error = true
        message.citations = []
        message.contexts = []
        message.sourceHint = undefined
        return
    }

    const citations = normalizeCitations(response.citations)
    message.content = response.answer || '未收到有效回答'
    message.loading = false
    message.error = false
    message.citations = citations
    message.contexts = normalizeContexts(response.contexts)
    message.sourceHint = citations.length > 0 ? undefined : '本回答未返回可展示来源'
}

export function buildHistoryMessages(record: QAHistoryDTO): ChatPresentationMessage[] {
    const citations = normalizeCitations(record.citations)
    return [
        {
            id: `history_${record.id}_user`,
            role: 'user',
            content: record.question || '历史问题为空',
        },
        {
            id: `history_${record.id}_assistant`,
            role: 'assistant',
            content: record.answer || '历史回答为空',
            loading: false,
            error: false,
            citations,
            sourceHint: citations.length > 0
                ? undefined
                : '该历史回答未保存可展示来源',
        },
    ]
}
