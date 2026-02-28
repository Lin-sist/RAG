import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface ChatMessage {
    role: 'user' | 'assistant'
    content: string
    citations?: Array<{ source: string; content: string }>
    loading?: boolean
    timestamp: number
}

export const useChatStore = defineStore('chat', () => {
    const messages = ref<ChatMessage[]>([])
    const currentKbId = ref<number | null>(null)
    const isStreaming = ref(false)

    function addMessage(msg: ChatMessage) { messages.value.push(msg) }
    function updateLastAssistantMessage(content: string) {
        const last = messages.value[messages.value.length - 1]
        if (last && last.role === 'assistant') last.content = content
    }
    function clearMessages() { messages.value = [] }
    function setCurrentKbId(id: number | null) { currentKbId.value = id }
    function setStreaming(val: boolean) { isStreaming.value = val }

    return { messages, currentKbId, isStreaming, addMessage, updateLastAssistantMessage, clearMessages, setCurrentKbId, setStreaming }
})
