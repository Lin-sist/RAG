import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Citation, RetrievedContext } from '@/types/qa'

export type ResponseMode = 'sync' | 'stream'
export type SourceStatus = 'complete' | 'unavailable' | 'interrupted'

export interface ChatMessage {
    id: string
    role: 'user' | 'assistant'
    content: string
    citations?: Citation[]
    contexts?: RetrievedContext[]
    responseMode?: ResponseMode
    sourceStatus?: SourceStatus
    sourceHint?: string
    loading?: boolean
    error?: boolean
    timestamp: number
}

let _msgId = 0
function genMsgId(): string {
    return `msg_${Date.now()}_${++_msgId}`
}

export const useChatStore = defineStore('chat', () => {
    const messages = ref<ChatMessage[]>([])
    const currentKbId = ref<number | null>(null)
    const currentKbName = ref<string>('')
    const isStreaming = ref(false)
    const topK = ref(5)

    /** 添加一条消息 */
    function addMessage(msg: Omit<ChatMessage, 'id'>) {
        messages.value.push({ ...msg, id: genMsgId() })
    }

    function patchLastAssistant(patch: Partial<ChatMessage>) {
        const last = messages.value[messages.value.length - 1]
        if (last && last.role === 'assistant') {
            Object.assign(last, patch)
        }
    }

    /** 追加文本到最后一条 assistant 消息 */
    function appendToLastAssistant(chunk: string) {
        const last = messages.value[messages.value.length - 1]
        if (last && last.role === 'assistant') {
            last.content += chunk
            last.error = false
        }
    }

    /** 完整替换最后一条 assistant 消息的内容 */
    function updateLastAssistantMessage(content: string) {
        const last = messages.value[messages.value.length - 1]
        if (last && last.role === 'assistant') last.content = content
    }

    /** 设置最后一条 assistant 消息的引用和上下文 */
    function setLastAssistantMeta(citations?: Citation[], contexts?: RetrievedContext[]) {
        patchLastAssistant({
            ...(citations ? { citations } : {}),
            ...(contexts ? { contexts } : {}),
        })
    }

    /** 设置最后一条 assistant 消息的 loading 状态 */
    function setLastAssistantLoading(loading: boolean) {
        patchLastAssistant({ loading })
    }

    /** 标记最后一条 assistant 消息为错误 */
    function setLastAssistantError(errorMsg: string) {
        patchLastAssistant({
            loading: false,
            error: true,
            content: errorMsg,
            sourceStatus: 'interrupted',
        })
    }

    function clearMessages() { messages.value = [] }
    function setCurrentKb(id: number | null, name?: string) {
        currentKbId.value = id
        currentKbName.value = name || ''
    }
    function setStreaming(val: boolean) { isStreaming.value = val }
    function setTopK(val: number) { topK.value = val }

    return {
        messages, currentKbId, currentKbName, isStreaming, topK,
        patchLastAssistant,
        addMessage, appendToLastAssistant, updateLastAssistantMessage,
        setLastAssistantMeta, setLastAssistantLoading, setLastAssistantError,
        clearMessages, setCurrentKb, setStreaming, setTopK,
    }
})
