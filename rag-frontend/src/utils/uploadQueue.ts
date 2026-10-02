import { normalizeError } from '../api/errors'
import type { DocumentUploadResponse } from '../types/document'

export interface UploadOutcome {
    file: File
    state: 'accepted' | 'failed' | 'unconfirmed'
    message: string
    data?: DocumentUploadResponse
}

// 一次用户操作一份快照；只把具有有效202/taskId的文件移出下一轮队列。
export async function submitUploadBatch(files: readonly File[], send: (file: File) => Promise<{
    status: number; data: { data: DocumentUploadResponse }
}>, onResult: (result: UploadOutcome) => void, active = () => true) {
    const accepted = new Set<File>()
    for (const file of [...files]) {
        if (!active()) break
        let result: UploadOutcome
        try {
            const response = await send(file)
            if (!active()) break
            const data = response.data?.data
            if (response.status === 202 && typeof data?.taskId === 'string' && data.taskId.trim()) {
                accepted.add(file)
                result = { file, state: 'accepted', data, message: `「${file.name}」已接受，等待索引处理` }
            } else {
                result = { file, state: 'unconfirmed', message: `「${file.name}」接受状态未确认；请先核查文档列表，再决定是否重新上传` }
            }
        } catch (error) {
            if (!active()) break
            const normalized = normalizeError(error)
            const rejected = normalized.status !== undefined && normalized.status >= 400 && normalized.status < 500 && normalized.status !== 408
            result = { file, state: rejected ? 'failed' : 'unconfirmed', message: rejected
                ? `「${file.name}」上传失败：${normalized.message}`
                : `「${file.name}」接受状态未确认：${normalized.message}；请先核查文档列表，再决定是否重新上传` }
        }
        onResult(result)
    }
    return files.filter(file => !accepted.has(file))
}
