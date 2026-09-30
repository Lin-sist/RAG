import { createServer as createHttpServer } from 'node:http'
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createServer } from 'vite'
import vue from '@vitejs/plugin-vue'
import { createRenderer, h, nextTick, ssrContextKey } from 'vue'
import { createPinia } from 'pinia'
import { routerKey, routeLocationKey } from 'vue-router'
import { renderToString } from '@vue/server-renderer'
import { fileURLToPath } from 'node:url'

globalThis.localStorage = { getItem() { return null }, setItem() {}, removeItem() {} }
globalThis.document = { activeElement: null, body: { style: { overflow: '' } }, addEventListener() {}, removeEventListener() {}, querySelector() { return null } }
globalThis.window = { matchMedia() { return { matches: false, addEventListener() {}, removeEventListener() {} } }, addEventListener() {}, removeEventListener() {} }
const server = await createServer({ configFile: false, optimizeDeps: { noDiscovery: true, include: [] },
  server: { middlewareMode: true, hmr: { server: createHttpServer() } },
  resolve: { alias: { '@': fileURLToPath(new URL('../src', import.meta.url)) } },
  plugins: [vue(), { name: 'shell-request-fixtures', enforce: 'pre', resolveId(id) {
    if (id.endsWith('/api/history')) return '\0history-fixture'
  }, load(id) {
    if (id === '\0history-fixture') return `export let handler; export function setHandler(fn) { handler = fn }
      export const getHistoryPage = (page, size) => handler(page, size)`
  } }], appType: 'custom' })
const renderer = createRenderer({
  createElement: type => ({ type, children: [] }), createText: text => ({ text }), createComment: text => ({ text }),
  setText(node, text) { node.text = text }, setElementText(node, text) { node.text = text },
  insert(node, parent) { node.parent = parent; parent.children.push(node) },
  remove(node) { node.parent.children = node.parent.children.filter(child => child !== node) },
  parentNode: node => node.parent, nextSibling: () => null, patchProp() {},
})
try {
  const { default: Sidebar } = await server.ssrLoadModule('/src/components/layout/AppSidebar.vue')
  const { default: Settings } = await server.ssrLoadModule('/src/components/settings/SettingsModal.vue')
  const { useAuthStore } = await server.ssrLoadModule('/src/stores/auth.ts')
  const api = await server.ssrLoadModule('\0history-fixture')
  function mountSidebar() {
    const root = { children: [] }, pinia = createPinia()
    const app = renderer.createApp({ ...Sidebar, render: () => h('div') })
    app.use(pinia); app.provide(ssrContextKey, {})
    app.provide(routerKey, { push() {} }); app.provide(routeLocationKey, { path: '/chat' }); app.mount(root)
    return { state: app._instance.setupState, auth: useAuthStore(pinia), unmount: () => app.unmount() }
  }
  const result = records => ({ data: { data: { records } } })
  await test('sidebar distinguishes error, manual retry, empty and at most 20 recent records', async () => {
    const calls = []
    api.setHandler(async (page, size) => { calls.push([page, size]); throw Error('fixture') })
    const f = mountSidebar(); await nextTick(); await nextTick()
    assert.equal(f.state.historyState, 'error')
    api.setHandler(async () => result([])); await f.state.loadHistory()
    assert.equal(f.state.historyState, 'empty')
    api.setHandler(async () => result(Array.from({ length: 25 }, (_, i) => ({ id: i + 1, question: 'q' + i }))))
    await f.state.loadHistory()
    assert.equal(f.state.historyState, 'ready'); assert.equal(f.state.historyList.length, 20)
    assert.deepEqual(calls[0], [1, 20]); f.unmount()
  })
  await test('stale responses cannot refill history after a newer request, auth revision or unmount', async () => {
    const pending = []
    api.setHandler(() => new Promise(resolve => pending.push(resolve)))
    const f = mountSidebar()
    const newer = f.state.loadHistory()
    pending[1](result([{ id: 2, question: 'new' }])); await newer
    pending[0](result([{ id: 1, question: 'old' }])); await nextTick()
    assert.equal(f.state.historyList[0].id, '2')
    const stale = f.state.loadHistory()
    f.auth.clearAuth(); await nextTick()
    pending[2](result([{ id: 3, question: 'previous session' }])); await stale
    assert.equal(f.state.historyList.length, 0)
    const unmounted = f.state.loadHistory(); f.unmount()
    pending[3](result([{ id: 4, question: 'after unmount' }])); await unmounted
    assert.equal(f.state.historyList.length, 0)
  })
  await test('settings renders only general appearance and truthful read-only identity', async () => {
    const pinia = createPinia()
    const { createSSRApp } = await import('vue')
    async function html(tab = 'profile') {
      const app = createSSRApp(Settings, { modelValue: true, initialTab: tab })
      app.use(pinia); const context = {}; await renderToString(app, context)
      return context.teleports.body
    }
    const missing = await html()
    assert.match(missing, /已登录用户/); assert.match(missing, /资料暂不可用/)
    assert.doesNotMatch(missing, /lin@example|sk-rag|更新密码|重新生成|保存更改|换头像/)
    useAuthStore(pinia).setUserInfo({ id: 901, username: 'fixture user', email: '' })
    const known = await html()
    assert.match(known, /fixture user/); assert.match(known, /未提供/)
    const general = await html('security')
    assert.match(general, /跟随系统/); assert.match(general, /浅色/); assert.match(general, /深色/)
  })
  await test('settings releases scroll and background locks on close or unmount', async () => {
    const background = { inert: false }
    let returned = 0
    document.getElementById = () => background
    document.activeElement = { isConnected: true, focus() { returned++ } }
    for (const action of ['close', 'unmount']) {
      document.body.style.overflow = 'auto'
      const app = renderer.createApp({ ...Settings, render: () => h('div') }, { modelValue: false })
      app.use(createPinia()); app.provide(ssrContextKey, {}); app.mount({ children: [] })
      app._instance.props.modelValue = true
      await nextTick(); await nextTick()
      assert.equal(document.body.style.overflow, 'hidden'); assert.equal(background.inert, true)
      if (action === 'close') { app._instance.props.modelValue = false; await nextTick() }
      else app.unmount()
      assert.equal(background.inert, false); assert.equal(document.body.style.overflow, 'auto')
      if (action === 'close') app.unmount()
    }
    assert.equal(returned, 2)
  })
} finally {
  await server.close()
  delete globalThis.localStorage; delete globalThis.document; delete globalThis.window
}
