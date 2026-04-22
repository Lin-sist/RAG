import { ref, onUnmounted } from 'vue'
import { getToken } from '@/utils/storage'

interface StreamResult {
    completed: boolean
    interrupted: boolean
    receivedChunks: boolean
}

export function useSSE() {
    const data = ref('')
    const isConnected = ref(false)
    const error = ref<string | null>(null)
    let abortController: AbortController | null = null

    async function connect(
        url: string,
        body: Record<string, unknown>,
        onChunk: (chunk: string) => void,
    ): Promise<StreamResult> {
        abortController = new AbortController()
        isConnected.value = true
        error.value = null
        data.value = ''
        let buffer = ''
        let receivedChunks = false
        let interrupted = false

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
                buffer += decoder.decode(value, { stream: true })
                const lines = buffer.split(/\r?\n/)
                buffer = lines.pop() ?? ''
                receivedChunks = processLines(lines, onChunk) || receivedChunks
            }

            const finalChunk = decoder.decode()
            if (finalChunk) {
                buffer += finalChunk
            }
            if (buffer) {
                receivedChunks = processLines([buffer], onChunk) || receivedChunks
            }
        } catch (e) {
            if (e instanceof Error && e.name !== 'AbortError') {
                error.value = e.message
                interrupted = true
            }
        } finally {
            isConnected.value = false
        }

        return {
            completed: !interrupted,
            interrupted,
            receivedChunks,
        }
    }

    function disconnect() {
        abortController?.abort()
        isConnected.value = false
    }

    onUnmounted(() => disconnect())

    return { data, isConnected, error, connect, disconnect }

    function processLines(lines: string[], onChunk: (chunk: string) => void): boolean {
        let received = false

        for (const line of lines) {
            if (!line.startsWith('data:')) {
                continue
            }

            // Spring SseEmitter 输出格式为 "data:chunk"（冒号后无空格）
            // 必须只截掉 "data:" 这 5 个字符，否则 token 自带的前导空格会被误吞。
            const chunk = line.slice(5)
            if (!chunk || chunk.trim() === '[DONE]') {
                continue
            }

            data.value += chunk
            onChunk(chunk)
            received = true
        }

        return received
    }
}
