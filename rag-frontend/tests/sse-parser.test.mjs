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
  const { createTextSSEParser } = await server.ssrLoadModule('/src/composables/sseParser.ts')

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
  } finally {
    globalThis.fetch = originalFetch
    globalThis.localStorage = originalStorage
  }
} finally {
  await server.close()
}
