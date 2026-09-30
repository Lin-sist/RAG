import { ref, onUnmounted, getCurrentInstance } from 'vue'
import { getToken } from '@/utils/storage'
import {
  createTextSSEParser,
  createStructuredSSEParser,
  type StructuredTerminal,
} from './sseParser'

export type StreamTransportStatus = 'DONE_TEXT_ONLY' | 'STREAM_ERROR' | 'CLIENT_ABORTED'

export interface StreamResult {
  status: StreamTransportStatus
  completed: boolean
  interrupted: boolean
  receivedChunks: boolean
  doneMarker: boolean
}

export interface StructuredStreamResult {
  status: 'TERMINAL' | 'INCOMPLETE' | 'CLIENT_ABORTED'
  terminal: StructuredTerminal | null
  receivedChunks: boolean
}

export function useSSE() {
  const data = ref('')
  const isConnected = ref(false)
  const error = ref<string | null>(null)
  let abortController: AbortController | null = null

  async function consume(
    url: string,
    body: Record<string, unknown>,
    parser: { push(chunk: string): void; finish(): void },
    structured: boolean,
  ): Promise<'OK' | 'FAILED' | 'ABORTED'> {
    if (isConnected.value) throw new Error('已有流式请求正在进行')
    const controller = new AbortController()
    abortController = controller
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
          ...(structured ? { 'X-RAG-Stream-Contract': 'structured-v1' } : {}),
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
      return controller.signal.aborted ? 'ABORTED' : 'OK'
    } catch (cause) {
      if (controller.signal.aborted) return 'ABORTED'
      error.value = cause instanceof Error ? cause.message : '流式连接失败'
      return 'FAILED'
    } finally {
      if (abortController === controller) abortController = null
      isConnected.value = false
    }
  }

  async function connect(
    url: string,
    body: Record<string, unknown>,
    onChunk: (chunk: string) => void,
  ): Promise<StreamResult> {
    const parser = createTextSSEParser((chunk) => {
      data.value += chunk
      onChunk(chunk)
    })
    const outcome = await consume(url, body, parser, false)
    let status: StreamTransportStatus = 'DONE_TEXT_ONLY'
    if (outcome === 'ABORTED') status = 'CLIENT_ABORTED'
    else if (outcome === 'FAILED') status = 'STREAM_ERROR'
    else if (parser.streamError) {
      error.value = parser.streamError
      status = 'STREAM_ERROR'
    }
    return {
      status,
      completed: status === 'DONE_TEXT_ONLY',
      interrupted: status !== 'DONE_TEXT_ONLY',
      receivedChunks: parser.receivedChunks,
      doneMarker: parser.doneMarker,
    }
  }

  async function connectStructured(
    url: string,
    body: Record<string, unknown>,
    onChunk: (chunk: string) => void,
  ): Promise<StructuredStreamResult> {
    const parser = createStructuredSSEParser((chunk) => {
      data.value += chunk
      onChunk(chunk)
    })
    const outcome = await consume(url, body, parser, true)
    if (outcome === 'ABORTED') {
      return { status: 'CLIENT_ABORTED', terminal: null, receivedChunks: parser.receivedChunks }
    }
    if (outcome === 'FAILED' || parser.protocolError || !parser.terminal) {
      error.value ||= parser.protocolError || '连接结束但未收到有效终态'
      return { status: 'INCOMPLETE', terminal: null, receivedChunks: parser.receivedChunks }
    }
    return { status: 'TERMINAL', terminal: parser.terminal, receivedChunks: parser.receivedChunks }
  }

  function disconnect() {
    abortController?.abort()
  }

  if (getCurrentInstance()) onUnmounted(disconnect)
  return { data, isConnected, error, connect, connectStructured, disconnect }
}
