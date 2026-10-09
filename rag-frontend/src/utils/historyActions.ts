import type { PageResult } from '@/types/api'
import type { QAHistoryDTO, QAFeedbackDTO } from '@/types/history'
import { normalizeError } from '@/api/errors'

export function createReadGate(revision: () => number) {
    let sequence = 0, alive = true
    return {
        begin() { return { sequence: ++sequence, revision: revision() } },
        current(token: { sequence: number; revision: number }) {
            return alive && sequence === token.sequence && revision() === token.revision
        },
        invalidate() { sequence++ },
        close() { alive = false; sequence++ },
    }
}

export async function collectHistorySnapshot(
    read: (page: number, size: number) => Promise<PageResult<QAHistoryDTO>>,
    current: () => boolean,
): Promise<number[]> {
    const ids = new Set<number>()
    let pages = 1
    for (let page = 1; page <= pages; page++) {
        if (!current()) throw new Error('SNAPSHOT_CANCELLED')
        const result = await read(page, 100)
        if (!current()) throw new Error('SNAPSHOT_CANCELLED')
        if (page === 1) pages = Math.max(1, result.totalPages)
        if (!Number.isSafeInteger(pages) || pages < 1 || !Array.isArray(result.records)) throw new Error('INVALID_HISTORY_PAGE')
        for (const record of result.records) {
            if (!Number.isSafeInteger(record.id) || record.id <= 0) throw new Error('INVALID_HISTORY_ID')
            ids.add(record.id)
        }
    }
    return [...ids]
}

export function isWriteUncertain(error: unknown) {
    const { status } = normalizeError(error)
    return status === undefined || status >= 500 || status === 408
}

export async function deleteHistorySnapshot(
    ids: number[], remove: (id: number) => Promise<unknown>, current: () => boolean,
    confirmed: (id: number) => void,
) {
    const targets = [...new Set(ids)]
    const result = { succeeded: 0, failed: 0, uncertain: 0, remaining: targets.length, stopped: false }
    for (const id of targets) {
        if (!current()) { result.stopped = true; break }
        try {
            await remove(id)
            result.succeeded++
            if (current()) confirmed(id)
        } catch (error) {
            if (isWriteUncertain(error)) result.uncertain++
            else result.failed++
            if (normalizeError(error).status === 429 || isWriteUncertain(error)) result.stopped = true
        }
        result.remaining--
        if (result.stopped) break
    }
    return result
}

export function matchingFeedback(items: QAFeedbackDTO[], historyId: number, userId?: number) {
    return items.find(item => item.qaId === historyId && (userId === undefined || item.userId === userId)) ?? null
}
