export interface TextSSEParser {
  push(chunk: string): void
  finish(): void
  readonly receivedChunks: boolean
  readonly doneMarker: boolean
  readonly streamError: string | null
}

/** Only the current text wire format is interpreted here; no business final state is inferred. */
export function createTextSSEParser(onChunk: (chunk: string) => void): TextSSEParser {
  let lineBuffer = ''
  let eventData: string[] = []
  let receivedChunks = false
  let doneMarker = false
  let streamError: string | null = null

  function dispatchEvent() {
    if (eventData.length === 0) return
    const payload = eventData.join('\n')
    eventData = []

    if (payload.trim() === '[DONE]') {
      doneMarker = true
      return
    }
    if (payload.startsWith('[ERROR]')) {
      streamError = payload.slice('[ERROR]'.length).trim() || '流式问答失败'
      return
    }
    if (payload && !streamError && !doneMarker) {
      receivedChunks = true
      onChunk(payload)
    }
  }

  function processLine(line: string) {
    if (line.endsWith('\r')) line = line.slice(0, -1)
    if (!line) {
      dispatchEvent()
      return
    }
    if (line.startsWith('data:')) {
      // The backend uses data:chunk. Keep every character after the colon.
      eventData.push(line.slice(5))
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
    finish() {
      if (lineBuffer) processLine(lineBuffer)
      lineBuffer = ''
      dispatchEvent()
    },
    get receivedChunks() { return receivedChunks },
    get doneMarker() { return doneMarker },
    get streamError() { return streamError },
  }
}
