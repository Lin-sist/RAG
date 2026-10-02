import { createServer as createHttpServer } from 'node:http'
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createServer } from 'vite'
import { createPinia, setActivePinia } from 'pinia'
import { fileURLToPath } from 'node:url'
const server = await createServer({ configFile: false, optimizeDeps: { noDiscovery: true, include: [] }, server: { middlewareMode: true, hmr: { server: createHttpServer() } },
  resolve: { alias: { '@': fileURLToPath(new URL('../src', import.meta.url)) } },
  plugins: [{ name: 'store-fixtures', enforce: 'pre', transform(code,id) {
    if (id.endsWith('/stores/knowledgeBase.ts')) return code.replace("import { ElMessage } from 'element-plus'", 'const ElMessage = { success() {}, error() {} }')
  }, resolveId(id) {
    if (id.endsWith('/api/knowledgeBase')) return '\0kb-fixture'
    if (id === 'element-plus') return '\0message-fixture'
  }, load(id) {
    if (id === '\0message-fixture') return 'export const ElMessage = { success() {}, error() {} }'
    if (id === '\0kb-fixture') return `export let handler; export function setHandler(fn) { handler=fn }
      export const listKB = () => handler('list'); export const getKBById = id => handler('detail',id);
      export const getKBStatistics = id => handler('stats',id);
      export const updateKB = (id,data) => handler('update',id,data); export const deleteKB = id => handler('delete',id);
      export const createKB = data => handler('create',null,data);`
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
  await test('switching detail invalidates in-flight statistics, including loading and errors', async () => {
    setActivePinia(createPinia()); const store = useKnowledgeBaseStore(); let resolveStats
    api.setHandler((op,id) => op === 'stats' ? new Promise(r => resolveStats=r) : Promise.resolve({data:{data:{id}}}))
    const old = store.fetchStatistics(1); assert.equal(store.statisticsLoading,true)
    await store.fetchById(2)
    resolveStats({data:{data:{kbId:1,documentCount:17}}}); await old
    assert.equal(store.current.id,2); assert.equal(store.statistics,null); assert.equal(store.statisticsKbId,null); assert.equal(store.statisticsLoading,false)
  })
  await test('statistics mismatch is not displayed, and explicit retry recovers', async () => {
    setActivePinia(createPinia()); const store = useKnowledgeBaseStore()
    api.setHandler(async () => ({data:{data:{kbId:2}}})); await store.fetchStatistics(1)
    assert.equal(store.statistics,null); assert.ok(store.statisticsError)
    api.setHandler(async () => ({data:{data:{kbId:1,documentCount:3}}})); await store.fetchStatistics(1)
    assert.equal(store.statistics.documentCount,3); assert.equal(store.statisticsError,'')
  })
  await test('confirmed update prevents stale list overwrite and does not change another detail', async () => {
    setActivePinia(createPinia()); const store = useKnowledgeBaseStore(); let resolveList
    store.list=[{id:1,name:'before'}]; store.setCurrent({id:2,name:'other'})
    api.setHandler((op,id,data) => op === 'list' ? new Promise(r => resolveList=r) : Promise.resolve({data:{data:{id,...data}}}))
    const stale=store.fetchList(); await store.update(1,{name:'after',description:''})
    resolveList({data:{data:[{id:1,name:'stale'}]}}); await stale
    assert.equal(store.list[0].name,'after'); assert.equal(store.list[0].description,''); assert.equal(store.current.id,2)
  })
  await test('rejected deletion preserves facts; confirmed deletion drops only its target', async () => {
    setActivePinia(createPinia()); const store=useKnowledgeBaseStore(); store.list=[{id:1},{id:2}]; store.setCurrent({id:2})
    api.setHandler(async () => {throw new Error('denied')}); await assert.rejects(store.remove(1))
    assert.deepEqual(store.list.map(x=>x.id),[1,2])
    api.setHandler(async () => ({})); await store.remove(1)
    assert.deepEqual(store.list.map(x=>x.id),[2]); assert.equal(store.current.id,2)
  })
} finally { await server.close() }
