import { createServer as createHttpServer } from 'node:http'
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createServer } from 'vite'
import { createPinia, setActivePinia } from 'pinia'
import { fileURLToPath } from 'node:url'
const server = await createServer({ configFile: false, optimizeDeps: { noDiscovery: true, include: [] }, server: { middlewareMode: true, hmr: { server: createHttpServer() } },
  resolve: { alias: { '@': fileURLToPath(new URL('../src', import.meta.url)) } },
  plugins: [{ name: 'store-fixtures', enforce: 'pre', resolveId(id) {
    if (id.endsWith('/api/knowledgeBase')) return '\0kb-fixture'
    if (id === 'element-plus') return '\0message-fixture'
  }, load(id) {
    if (id === '\0message-fixture') return 'export const ElMessage = { success() {}, error() {} }'
    if (id === '\0kb-fixture') return `export let handler; export function setHandler(fn) { handler=fn }
      export const listKB = () => handler('list'); export const getKBById = id => handler('detail',id);
      export const getKBStatistics = id => handler('stats',id);`
  } }], appType: 'custom' })
try {
  const { useKnowledgeBaseStore } = await server.ssrLoadModule('/src/stores/knowledgeBase.ts')
  const api = await server.ssrLoadModule('\0kb-fixture')
  await test('list failure is distinct from empty success and is recoverable', async () => {
    setActivePinia(createPinia()); const store = useKnowledgeBaseStore()
    api.setHandler(async () => { throw new Error('fixture') })
    await assert.rejects(store.fetchList()); assert.ok(store.listError); assert.equal(store.loading,false)
    api.setHandler(async () => ({data:{data:[]}})); await store.fetchList()
    assert.equal(store.listError,''); assert.deepEqual(store.list,[])
  })
  await test('out-of-order detail responses cannot overwrite current KB', async () => {
    setActivePinia(createPinia()); const store = useKnowledgeBaseStore(); const pending = new Map()
    api.setHandler((_,id) => new Promise(resolve => pending.set(id,resolve)))
    const first = store.fetchById(1), second = store.fetchById(2)
    pending.get(2)({data:{data:{id:2}}}); await second
    pending.get(1)({data:{data:{id:1}}}); await first
    assert.equal(store.current.id,2); assert.equal(store.loading,false)
  })
  await test('statistics failure does not erase detail', async () => {
    setActivePinia(createPinia()); const store = useKnowledgeBaseStore()
    store.setCurrent({id:1}); api.setHandler(async () => {throw new Error('fixture')})
    await store.fetchStatistics(1)
    assert.equal(store.current.id,1); assert.equal(store.statistics,null); assert.ok(store.statisticsError)
  })
} finally { await server.close() }
