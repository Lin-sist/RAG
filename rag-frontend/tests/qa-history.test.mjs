import { createServer as createHttpServer } from 'node:http'
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createServer } from 'vite'
import { fileURLToPath } from 'node:url'

const calls = []
const server = await createServer({
  configFile: false,
  optimizeDeps: { noDiscovery: true, include: [] },
  server: { middlewareMode: true, hmr: { server: createHttpServer() } },
  resolve: { alias: { '@': fileURLToPath(new URL('../src', import.meta.url)) } },
  plugins: [{
    name: 'qa-history-request-fixture',
    enforce: 'pre',
    resolveId(id, importer) {
      if (id === './request' && importer?.replaceAll('\\', '/').includes('/src/api/')) {
        return '\0request-fixture'
      }
    },
    load(id) {
      if (id !== '\0request-fixture') return
      return `
        const calls = globalThis.__qaHistoryFixtureCalls
        export default {
          post(url, data) { calls.push({ method: 'post', url, data }); return Promise.resolve({ data: { data: {} } }) },
          get(url, config) { calls.push({ method: 'get', url, config }); return Promise.resolve({ data: { data: {} } }) },
          delete(url) { calls.push({ method: 'delete', url }); return Promise.resolve({ data: {} }) }
        }
      `
    },
  }],
  appType: 'custom',
})

globalThis.__qaHistoryFixtureCalls = calls

try {
  const qaApi = await server.ssrLoadModule('/src/api/qa.ts')
  const historyApi = await server.ssrLoadModule('/src/api/history.ts')
  const qaPresentation = await server.ssrLoadModule('/src/utils/qaPresentation.ts')
  const historyPresentation = await server.ssrLoadModule('/src/utils/historyPresentation.ts')

  await test('sync ask uses POST /api/qa/ask and never calls stream endpoint', async () => {
    calls.length = 0
    await qaApi.ask({ kbId: 7, question: 'fixture question', topK: 4 })
    assert.deepEqual(calls, [{
      method: 'post',
      url: '/api/qa/ask',
      data: { kbId: 7, question: 'fixture question', topK: 4 },
    }])
  })

  await test('citation presentation prefers verified document labels and does not invent scores', () => {
    const citation = {
      source: 'chunk-source',
      sourceFileName: 'source.pdf',
      documentTitle: 'Document title',
      score: 0.812,
      snippet: 'fixture',
      startIndex: 0,
      endIndex: 7,
    }
    assert.equal(qaPresentation.citationLabel(citation), 'Document title')
    assert.equal(qaPresentation.formatRelevanceScore(citation.score), '81%')
    assert.equal(qaPresentation.formatRelevanceScore(undefined), null)

    const message = { id: 'a', role: 'assistant', content: '', loading: true }
    qaPresentation.applySyncResponse(message, {
      question: 'q', answer: 'answer', citations: null, contexts: null, metadata: null,
    })
    assert.equal(message.loading, false)
    assert.deepEqual(message.citations, [])
    assert.equal(message.sourceHint, '本回答未返回可展示来源')
  })

  await test('business error response is rendered as an error without sources', () => {
    const message = { id: 'error', role: 'assistant', content: '', loading: true }
    qaPresentation.applySyncResponse(message, {
      question: 'q',
      answer: '模型服务请求未被接受，请检查配置后重试',
      citations: [{ source: 'must-not-render', snippet: 'fixture', startIndex: 0, endIndex: 7 }],
      contexts: [{ content: 'must-not-render', source: 'fixture', relevanceScore: 1, metadata: {} }],
      metadata: { status: 'error' },
    })

    assert.equal(message.loading, false)
    assert.equal(message.error, true)
    assert.equal(message.content, '模型服务请求未被接受，请检查配置后重试')
    assert.deepEqual(message.citations, [])
    assert.deepEqual(message.contexts, [])
    assert.equal(message.sourceHint, undefined)
  })

  await test('history detail is adapted as one question and one answer, not a conversation', () => {
    const record = {
      id: 12,
      userId: 3,
      kbId: 7,
      question: '历史问题',
      answer: '历史回答',
      citations: null,
      traceId: 'fixture',
      latencyMs: 15,
      createdAt: '2026-09-21T10:00:00',
    }
    const messages = qaPresentation.buildHistoryMessages(record)
    assert.equal(messages.length, 2)
    assert.deepEqual(messages.map(message => message.role), ['user', 'assistant'])
    assert.ok(messages.every(message => !Object.hasOwn(message, 'conversationId')))
    assert.equal(messages[1].sourceHint, '该历史回答未保存可展示来源')
  })

  await test('history API keeps one-based paging and record-id detail routes', async () => {
    calls.length = 0
    await historyApi.getHistoryPage(1, 100, 7)
    await historyApi.getHistoryById(12)
    assert.deepEqual(calls, [
      { method: 'get', url: '/api/history', config: { params: { page: 1, size: 100, kbId: 7 } } },
      { method: 'get', url: '/api/history/12', config: undefined },
    ])
  })

  await test('history grouping is newest-first and local filtering does not change records', () => {
    const records = [
      { id: 1, kbId: 7, question: 'older', createdAt: '2026-09-19T12:00:00' },
      { id: 2, kbId: 8, question: 'newer', createdAt: '2026-09-21T09:00:00' },
    ]
    const groups = historyPresentation.groupHistoryRecords(records, new Date('2026-09-21T12:00:00'))
    assert.equal(groups[0].items[0].id, 2)
    assert.equal(groups[2].items[0].id, 1)
    const filtered = historyPresentation.filterHistoryGroups(groups, '知识库 #7')
    assert.equal(filtered.flatMap(group => group.items).length, 1)
    assert.equal(records.length, 2)
  })
} finally {
  delete globalThis.__qaHistoryFixtureCalls
  await server.close()
}
