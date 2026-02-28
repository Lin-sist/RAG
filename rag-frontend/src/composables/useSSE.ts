import { ref, onUnmounted } from 'vue'
import { getToken } from '@/utils/storage'

export function useSSE() {
    const data = ref('')
    const isConnected = ref(false)
    const error = ref<string | null>(null)
    let abortController: AbortController | null = null

    async function connect(url: string, body: Record<string, unknown>, onChunk: (chunk: string) => void) {
        abortController = new AbortController()
        isConnected.value = true
        error.value = null
        data.value = ''

        try {
            const token = getToken('accessToken')
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...(token ? { Authorization: `Bearer ${token}` } : {}),
                },
                body: JSON.stringify(body),
                signal: abortController.signal,
            })

            if (!response.ok) throw new Error(`HTTP ${response.status}: ${response.statusText}`)

            const reader = response.body?.getReader()
            const decoder = new TextDecoder()
            if (!reader) throw new Error('响应体不可读')

            while (true) {
                const { done, value } = await reader.read()
                if (done) break
                const text = decoder.decode(value, { stream: true })
                const lines = text.split('\n')
                for (const line of lines) {
                    // Spring SseEmitter 输出格式为 "data:chunk"（冒号后无空格）
                    // 必须只截掉 "data:" 这5个字符，否则 token 自带的前导空格会被误吞
                    // 导致英文单词之间没有空格（如 "Accordingtothe..."）
                    if (line.startsWith('data:')) {
                        const chunk = line.slice(5)
                        if (chunk === '[DONE]' || chunk.trim() === '[DONE]') continue
                        if (!chunk) continue
                        data.value += chunk
                        onChunk(chunk)
                    }
                }
            }
        } catch (e) {
            if (e instanceof Error && e.name !== 'AbortError') error.value = e.message
        } finally {
            isConnected.value = false
        }
    }

    function disconnect() {
        abortController?.abort()
        isConnected.value = false
    }

    onUnmounted(() => disconnect())

    return { data, isConnected, error, connect, disconnect }
}
