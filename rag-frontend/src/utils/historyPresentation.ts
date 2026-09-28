import type { QAHistoryDTO } from '@/types/history'

export type DateGroupLabel = '今天' | '昨天' | '更早'

export interface HistoryPresentationItem {
    id: number
    title: string
    knowledgeBase: string
    time: string
}

export interface HistoryPresentationGroup {
    label: DateGroupLabel
    items: HistoryPresentationItem[]
}

const GROUP_LABELS: DateGroupLabel[] = ['今天', '昨天', '更早']

export function createEmptyHistoryGroups(): HistoryPresentationGroup[] {
    return GROUP_LABELS.map(label => ({ label, items: [] }))
}

function getDateBucket(date: Date, now: Date): DateGroupLabel {
    const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    const yesterdayStart = new Date(todayStart)
    yesterdayStart.setDate(yesterdayStart.getDate() - 1)

    if (date >= todayStart) return '今天'
    if (date >= yesterdayStart && date < todayStart) return '昨天'
    return '更早'
}

function formatTwoDigits(value: number): string {
    return value.toString().padStart(2, '0')
}

function formatItemTime(date: Date, bucket: DateGroupLabel, fallback: string): string {
    if (Number.isNaN(date.getTime())) return fallback
    const hhmm = `${formatTwoDigits(date.getHours())}:${formatTwoDigits(date.getMinutes())}`
    if (bucket === '今天') return hhmm
    if (bucket === '昨天') return `昨天 ${hhmm}`
    return `${date.getFullYear()}-${formatTwoDigits(date.getMonth() + 1)}-${formatTwoDigits(date.getDate())}`
}

export function groupHistoryRecords(
    records: QAHistoryDTO[],
    now = new Date(),
): HistoryPresentationGroup[] {
    const grouped: Record<DateGroupLabel, HistoryPresentationItem[]> = {
        今天: [],
        昨天: [],
        更早: [],
    }

    const sorted = [...records].sort((a, b) => (
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    ))

    for (const record of sorted) {
        const createdAt = new Date(record.createdAt)
        const bucket = Number.isNaN(createdAt.getTime()) ? '更早' : getDateBucket(createdAt, now)
        grouped[bucket].push({
            id: record.id,
            title: record.question || `历史记录 #${record.id}`,
            knowledgeBase: `知识库 #${record.kbId}`,
            time: formatItemTime(createdAt, bucket, record.createdAt),
        })
    }

    return GROUP_LABELS.map(label => ({ label, items: grouped[label] }))
}

export function filterHistoryGroups(
    groups: HistoryPresentationGroup[],
    searchQuery: string,
): HistoryPresentationGroup[] {
    const query = searchQuery.trim().toLocaleLowerCase()
    if (!query) return groups

    return groups.map(group => ({
        ...group,
        items: group.items.filter(item => (
            item.title.toLocaleLowerCase().includes(query)
            || item.knowledgeBase.toLocaleLowerCase().includes(query)
        )),
    }))
}
