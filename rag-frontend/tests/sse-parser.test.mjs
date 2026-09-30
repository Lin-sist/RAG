import { createServer as createHttpServer } from 'node:http'
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createServer } from 'vite'
import { fileURLToPath } from 'node:url'

const server = await createServer({
  configFile: false,
  optimizeDeps: { noDiscovery: true, include: [] },
  server: { middlewareMode: true, hmr: { server: createHttpServer() } },
  resolve: { alias: { '@': fileURLToPath(new URL('../src', import.meta.url)) } },
  appType: 'custom',
})

try {
  const { createTextSSEParser, createStructuredSSEParser } = await server.ssrLoadModule('/src/composables/sseParser.ts')
  const terminal = (finalState = 'ANSWER', citations = []) => ({
    schemaVersion: 'structured-v1', finalState, reason: 'NONE', citations,
    metadata: {}, classifierVersion: 'legacy', effectiveStrategy: 'legacy',
    policyVersion: 'legacy', routeReason: 'LEGACY', budgetOutcome: null, usage: null,
  })
  const event = (name, data) => `event:${name}\ndata:${data}\n\n`

  await test('split frames retain UTF-8 text, leading spaces and tail event', () => {
    const chunks = []
    const parser = createTextSSEParser(chunk => chunks.push(chunk))
    parser.push('data:  北')
    parser.push('星\r\n\r\ndata:令牌')
    parser.finish()
    assert.deepEqual(chunks, ['  北星', '令牌'])
    assert.equal(parser.receivedChunks, true)
    assert.equal(parser.doneMarker, false)
  })

  await test('error followed by DONE remains an error and never becomes answer text', () => {
    const chunks = []
    const parser = createTextSSEParser(chunk => chunks.push(chunk))
    parser.push('data:部分\n\ndata:[ERROR] 暂不可用\n\ndata:[DONE]\n\n')
    parser.finish()
    assert.deepEqual(chunks, ['部分'])
    assert.equal(parser.streamError, '暂不可用')
    assert.equal(parser.doneMarker, true)
  })

  await test('multiple data lines belong to one event', () => {
    const chunks = []
    const parser = createTextSSEParser(chunk => chunks.push(chunk))
    parser.push('data:第一行\ndata:第二行\n\ndata:[DONE]\n\n')
    parser.finish()
    assert.deepEqual(chunks, ['第一行\n第二行'])
    assert.equal(parser.doneMarker, true)
  })

  await test('structured parser keeps split UTF-8 text and accepts one ANSWER terminal with citations', () => {
    const chunks = []
    const parser = createStructuredSSEParser(chunk => chunks.push(chunk))
    const citation = { source: 'doc-1', snippet: '证据', startIndex: 0, endIndex: 2 }
    const wire = event('text', '  北星') + event('terminal', JSON.stringify(terminal('ANSWER', [citation])))
    for (const part of [wire.slice(0, 9), wire.slice(9, 16), wire.slice(16, 31), wire.slice(31)]) parser.push(part)
    parser.finish()
    assert.deepEqual(chunks, ['  北星'])
    assert.deepEqual(parser.terminal.citations, [citation])
    assert.equal(parser.protocolError, null)
  })

  await test('structured parser rejects missing, malformed or duplicate terminal and post-terminal text', () => {
    const cases = [
      event('text', '部分'),
      event('terminal', '{bad json}'),
      event('terminal', JSON.stringify(terminal())) + event('terminal', JSON.stringify(terminal())),
      event('terminal', JSON.stringify(terminal())) + event('text', 'late'),
      event('terminal', JSON.stringify(terminal('NO_ANSWER', [{ source: 'x', snippet: 'x', startIndex: 0, endIndex: 1 }]))),
      event('unknown', 'x'),
      `event:terminal\ndata:${JSON.stringify(terminal())}`,
    ]
    for (const wire of cases) {
      const parser = createStructuredSSEParser(() => {})
      parser.push(wire)
      parser.finish()
      assert.equal(parser.terminal, null)
    }
  })

  const { useSSE } = await server.ssrLoadModule('/src/composables/useSSE.ts')
  const originalFetch = globalThis.fetch
  const originalStorage = globalThis.localStorage
  globalThis.localStorage = { getItem: () => null }
  try {
    await test('client abort is distinct from a completed stream', async () => {
      globalThis.fetch = (_url, { signal }) => new Promise((_resolve, reject) => {
        signal.addEventListener('abort', () => reject(Object.assign(new Error('aborted'), { name: 'AbortError' })))
      })
      const sse = useSSE()
      const pending = sse.connect('/api/qa/ask/stream', { kbId: 17, question: 'synthetic' }, () => {})
      sse.disconnect()
      const result = await pending
      assert.equal(result.status, 'CLIENT_ABORTED')
      assert.equal(result.completed, false)
      assert.equal(result.receivedChunks, false)
      assert.equal(sse.error.value, null)
    })

    await test('wire ERROR wins even when DONE follows', async () => {
      const encoded = new TextEncoder().encode('data:部分\n\ndata:[ERROR] 暂不可用\n\ndata:[DONE]\n\n')
      globalThis.fetch = async () => new Response(new ReadableStream({
        start(controller) { controller.enqueue(encoded); controller.close() },
      }), { status: 200 })
      const received = []
      const result = await useSSE().connect('/api/qa/ask/stream', { kbId: 17, question: 'synthetic' }, chunk => received.push(chunk))
      assert.deepEqual(received, ['部分'])
      assert.equal(result.status, 'STREAM_ERROR')
      assert.equal(result.doneMarker, true)
    })

    await test('structured request negotiates v1 and returns terminal without a second ask', async () => {
      let calls = 0
      globalThis.fetch = async (_url, options) => {
        calls++
        assert.equal(options.headers['X-RAG-Stream-Contract'], 'structured-v1')
        const wire = event('text', '回答') + event('terminal', JSON.stringify(terminal()))
        return new Response(new ReadableStream({
          start(controller) { controller.enqueue(new TextEncoder().encode(wire)); controller.close() },
        }), { status: 200 })
      }
      const received = []
      const result = await useSSE().connectStructured('/api/qa/ask/stream', { kbId: 17, question: 'synthetic' }, chunk => received.push(chunk))
      assert.equal(calls, 1)
      assert.deepEqual(received, ['回答'])
      assert.equal(result.status, 'TERMINAL')
      assert.equal(result.terminal.finalState, 'ANSWER')
    })

    await test('structured stream without terminal is incomplete; abort is only client-local', async () => {
      globalThis.fetch = async () => new Response(new ReadableStream({
        start(controller) { controller.enqueue(new TextEncoder().encode(event('text', '部分'))); controller.close() },
      }), { status: 200 })
      const incomplete = await useSSE().connectStructured('/api/qa/ask/stream', { kbId: 17 }, () => {})
      assert.equal(incomplete.status, 'INCOMPLETE')
      assert.equal(incomplete.terminal, null)
      globalThis.fetch = (_url, { signal }) => new Promise((_resolve, reject) => {
        signal.addEventListener('abort', () => reject(Object.assign(new Error('aborted'), { name: 'AbortError' })))
      })
      const sse = useSSE()
      const pending = sse.connectStructured('/api/qa/ask/stream', { kbId: 17 }, () => {})
      sse.disconnect()
      const aborted = await pending
      assert.equal(aborted.status, 'CLIENT_ABORTED')
      assert.equal(aborted.terminal, null)
    })
  } finally {
    globalThis.fetch = originalFetch
    globalThis.localStorage = originalStorage
  }
} finally {
  await server.close()
}
