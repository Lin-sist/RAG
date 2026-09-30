import type { Citation } from '@/types/qa'

interface SSEFrame {
  name: string
  data: string
}

function createFrameParser(onEvent: (event: SSEFrame) => void) {
  let lineBuffer = ''
  let eventData: string[] = []
  let eventName = ''

  function dispatchEvent() {
    if (eventData.length > 0) onEvent({ name: eventName, data: eventData.join('\n') })
    eventData = []
    eventName = ''
  }

  function processLine(raw: string) {
    const line = raw.endsWith('\r') ? raw.slice(0, -1) : raw
    if (!line) {
      dispatchEvent()
    } else if (line.startsWith('data:')) {
      // Spring's SSE writer emits data:chunk. Preserve the text after the colon.
      eventData.push(line.slice(5))
    } else if (line.startsWith('event:')) {
      eventName = line.slice(6).trim()
    }
  }

  return {
    push(chunk: string) {
      lineBuffer += chunk
      let newline = lineBuffer.indexOf('\n')
      while (newline !== -1) {
        processLine(lineBuffer.slice(0, newline))
        lineBuffer = lineBuffer.slice(newline + 1)
        newline = lineBuffer.indexOf('\n')
      }
    },
    finish(flushTail: boolean) {
      const incompleteTail = Boolean(lineBuffer || eventData.length || eventName)
      if (flushTail) {
        if (lineBuffer) processLine(lineBuffer)
        dispatchEvent()
      }
      lineBuffer = ''
      eventData = []
      eventName = ''
      return incompleteTail
    },
  }
}

export interface TextSSEParser {
  push(chunk: string): void
  finish(): void
  readonly receivedChunks: boolean
  readonly doneMarker: boolean
  readonly streamError: string | null
}

/** Legacy text wire; it carries no business final state. */
export function createTextSSEParser(onChunk: (chunk: string) => void): TextSSEParser {
  let receivedChunks = false
  let doneMarker = false
  let streamError: string | null = null
  const frames = createFrameParser(({ data: payload }) => {
    if (payload.trim() === '[DONE]') {
      doneMarker = true
    } else if (payload.startsWith('[ERROR]')) {
      streamError = payload.slice('[ERROR]'.length).trim() || '流式问答失败'
    } else if (payload && !streamError && !doneMarker) {
      receivedChunks = true
      onChunk(payload)
    }
  })
  return {
    push: frames.push,
    finish() { frames.finish(true) },
    get receivedChunks() { return receivedChunks },
    get doneMarker() { return doneMarker },
    get streamError() { return streamError },
  }
}

export type StreamFinalState = 'ANSWER' | 'NO_ANSWER' | 'UNSUPPORTED' | 'INVALID' | 'ERROR' | 'CANCELLED'

export interface StructuredTerminal {
  schemaVersion: 'structured-v1'
  finalState: StreamFinalState
  reason: string
  citations: Citation[]
  metadata: Record<string, unknown>
  classifierVersion: string | null
  effectiveStrategy: string | null
  policyVersion: string | null
  routeReason: string | null
  budgetOutcome: string | null
  usage: Record<string, unknown> | null
}

const FINAL_STATES = new Set<StreamFinalState>([
  'ANSWER', 'NO_ANSWER', 'UNSUPPORTED', 'INVALID', 'ERROR', 'CANCELLED',
])
const TERMINAL_KEYS = [
  'schemaVersion', 'finalState', 'reason', 'citations', 'metadata',
  'classifierVersion', 'effectiveStrategy', 'policyVersion', 'routeReason',
  'budgetOutcome', 'usage',
] as const

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function isCitation(value: unknown): value is Citation {
  return isRecord(value)
    && typeof value.source === 'string'
    && typeof value.snippet === 'string'
    && typeof value.startIndex === 'number'
    && typeof value.endIndex === 'number'
}

function parseTerminal(data: string): StructuredTerminal | null {
  let value: unknown
  try { value = JSON.parse(data) } catch { return null }
  if (!isRecord(value) || !TERMINAL_KEYS.every(key => Object.prototype.hasOwnProperty.call(value, key))) return null
  if (value.schemaVersion !== 'structured-v1'
    || typeof value.finalState !== 'string'
    || !FINAL_STATES.has(value.finalState as StreamFinalState)
    || typeof value.reason !== 'string'
    || !Array.isArray(value.citations)
    || !value.citations.every(isCitation)
    || !isRecord(value.metadata)
    || (!isRecord(value.usage) && value.usage !== null)) return null
  for (const key of ['classifierVersion', 'effectiveStrategy', 'policyVersion', 'routeReason', 'budgetOutcome']) {
    if (value[key] !== null && typeof value[key] !== 'string') return null
  }
  if (value.finalState !== 'ANSWER' && value.citations.length > 0) return null
  return value as unknown as StructuredTerminal
}

export interface StructuredSSEParser {
  push(chunk: string): void
  finish(): void
  readonly terminal: StructuredTerminal | null
  readonly protocolError: string | null
  readonly receivedChunks: boolean
}

export function createStructuredSSEParser(onChunk: (chunk: string) => void): StructuredSSEParser {
  let terminal: StructuredTerminal | null = null
  let protocolError: string | null = null
  let receivedChunks = false
  const frames = createFrameParser(({ name, data }) => {
    if (protocolError) return
    if (name === 'text') {
      if (terminal) {
        protocolError = '终态后仍收到文本事件'
      } else {
        receivedChunks = true
        onChunk(data)
      }
    } else if (name === 'terminal') {
      if (terminal) {
        protocolError = '收到重复终态事件'
      } else {
        terminal = parseTerminal(data)
        if (!terminal) protocolError = '终态事件格式无效'
      }
    } else {
      protocolError = '收到未知流事件'
    }
  })
  return {
    push: frames.push,
    // A structured event must end with an SSE blank line; a truncated tail is incomplete.
    finish() {
      if (frames.finish(false)) protocolError = '流事件未完整结束'
    },
    get terminal() { return protocolError ? null : terminal },
    get protocolError() { return protocolError },
    get receivedChunks() { return receivedChunks },
  }
}
