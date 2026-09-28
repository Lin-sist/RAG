import { ref, onUnmounted, getCurrentInstance } from 'vue'
import { getToken } from '@/utils/storage'
import { createTextSSEParser } from './sseParser'

export type StreamTransportStatus = 'DONE_TEXT_ONLY' | 'STREAM_ERROR' | 'CLIENT_ABORTED'

export interface StreamResult {
  status: StreamTransportStatus
  completed: boolean
  interrupted: boolean
  receivedChunks: boolean
  doneMarker: boolean
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
    if (isConnected.value) throw new Error('已有流式请求正在进行')
    const controller = new AbortController()
    abortController = controller
    isConnected.value = true
    error.value = null
    data.value = ''
    const parser = createTextSSEParser((chunk) => {
      data.value += chunk
      onChunk(chunk)
    })
    let status: StreamTransportStatus = 'DONE_TEXT_ONLY'

    try {
      const token = getToken('accessToken')
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify(body),
        signal: controller.signal,
      })
      if (!response.ok) throw new Error(`HTTP ${response.status}: ${response.statusText}`)

      const reader = response.body?.getReader()
      if (!reader) throw new Error('响应体不可读')
      const decoder = new TextDecoder()
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        parser.push(decoder.decode(value, { stream: true }))
      }
      parser.push(decoder.decode())
      parser.finish()
      if (parser.streamError) {
        error.value = parser.streamError
        status = 'STREAM_ERROR'
      }
    } catch (cause) {
      if (controller.signal.aborted) {
        status = 'CLIENT_ABORTED'
      } else {
        status = 'STREAM_ERROR'
        error.value = cause instanceof Error ? cause.message : '流式连接失败'
      }
    } finally {
      if (abortController === controller) abortController = null
      isConnected.value = false
    }

    return {
      status,
      completed: status === 'DONE_TEXT_ONLY',
      interrupted: status !== 'DONE_TEXT_ONLY',
      receivedChunks: parser.receivedChunks,
      doneMarker: parser.doneMarker,
    }
  }

  function disconnect() {
    abortController?.abort()
  }

  if (getCurrentInstance()) onUnmounted(disconnect)
  return { data, isConnected, error, connect, disconnect }
}
