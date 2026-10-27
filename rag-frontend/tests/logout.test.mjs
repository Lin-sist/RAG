import { createServer as createHttpServer } from 'node:http'
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createServer } from 'vite'
import vue from '@vitejs/plugin-vue'
import axios from 'axios'
import { createRenderer, h, nextTick, ssrContextKey } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { routerKey, routeLocationKey } from 'vue-router'
import { fileURLToPath } from 'node:url'

const stored = new Map()
globalThis.localStorage = { getItem: k => stored.get(k) ?? null, setItem: (k, v) => stored.set(k, v), removeItem: k => stored.delete(k) }
globalThis.document = { activeElement: null, body: { style: { overflow: '' } }, addEventListener() {}, removeEventListener() {}, querySelector() { return null } }
globalThis.window = { matchMedia() { return { matches: false, addEventListener() {}, removeEventListener() {} } }, addEventListener() {}, removeEventListener() {} }
globalThis.__logoutFixture = { routes: [], warnings: [] }
const server = await createServer({ configFile: false, optimizeDeps: { noDiscovery: true, include: [] },
  server: { middlewareMode: true, hmr: { server: createHttpServer() } }, appType: 'custom',
  resolve: { alias: { '@': fileURLToPath(new URL('../src', import.meta.url)) } },
  plugins: [vue(), { name: 'logout-offline-fixtures', enforce: 'pre',
    resolveId(id) { if (id === 'logout-router-fixture') return '\0logout-router' },
    load(id) { if (id === '\0logout-router') return `export default { replace: async path => { globalThis.__logoutFixture.routes.push(path) }, push() {}, currentRoute: { value: { fullPath: '/chat' } } }` },
    transform(code, id) {
      if (/useAuth\.ts|\/api\/request\.ts/.test(id.replaceAll('\\', '/'))) return code.replace("import router from '@/router'", "import router from 'logout-router-fixture'")
      if (id.endsWith('AppSidebar.vue')) return code.replace("import { ElMessage } from 'element-plus'", 'const ElMessage = { warning: text => globalThis.__logoutFixture.warnings.push(text) }')
    },
  }] })
const renderer = createRenderer({ createElement: type => ({ type, children: [] }), createText: text => ({ text }), createComment: text => ({ text }),
  setText(n, t) { n.text = t }, setElementText(n, t) { n.text = t }, insert(n, p) { n.parent = p; p.children.push(n) },
  remove(n) { n.parent.children = n.parent.children.filter(x => x !== n) }, parentNode: n => n.parent, nextSibling: () => null, patchProp() {} })
const deferred = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b }); return { promise, resolve, reject } }
const flush = async () => { for (let i = 0; i < 12; i++) { await Promise.resolve(); await nextTick() } }
try {
  const { useAuthStore } = await server.ssrLoadModule('/src/stores/auth.ts')
  const { useAuth } = await server.ssrLoadModule('/src/composables/useAuth.ts')
  const { default: request, refreshSession } = await server.ssrLoadModule('/src/api/request.ts')
  const Sidebar = (await server.ssrLoadModule('/src/components/layout/AppSidebar.vue')).default
  function fixture(handler) {
    stored.clear(); const pinia = createPinia(); setActivePinia(pinia)
    const auth = useAuthStore(pinia); auth.setAuthTokens('fixture-old', 'fixture-refresh'); auth.setUserInfo({ id: 901, username: 'fixture', email: '' })
    const calls = []; globalThis.__logoutFixture.routes = []; globalThis.__logoutFixture.warnings = []
    request.defaults.adapter = async config => {
      calls.push({ url: config.url, method: config.method, authorization: config.headers.Authorization })
      const [status, data = { code: 200 }] = await handler(config)
      const response = { status, data, headers: {}, statusText: '', config }
      if (status >= 400) throw new axios.AxiosError('synthetic failure', 'ERR_BAD_RESPONSE', config, {}, response)
      return response
    }
    return { pinia, auth, calls, ...useAuth() }
  }
  await test('logout sends one authenticated POST before clearing both stored tokens and replacing login route', async () => {
    const f = fixture(async () => [200]); assert.equal(await f.logout(), 'confirmed')
    assert.deepEqual(f.calls, [{ url: '/auth/logout', method: 'post', authorization: 'Bearer fixture-old' }])
    assert.equal(f.auth.isLoggedIn, false); assert.equal(f.auth.refreshToken, ''); assert.equal(f.auth.userInfo, null); assert.equal(stored.size, 0)
    assert.deepEqual(globalThis.__logoutFixture.routes, ['/login'])
  })
  await test('network, 401, 429 and 503 failures clear local identity with no refresh or replay', async () => {
    for (const status of [0, 401, 429, 503]) {
      const f = fixture(async () => { if (!status) throw new axios.AxiosError('synthetic timeout', 'ECONNABORTED'); return [status] })
      assert.equal(await f.logout(), 'unconfirmed'); assert.equal(f.calls.length, 1); assert.equal(f.auth.isLoggedIn, false); assert.equal(stored.size, 0)
      assert.deepEqual(globalThis.__logoutFixture.routes, ['/login'])
    }
  })
  await test('logical failure inside HTTP 200 is not reported as confirmed logout', async () => {
    const f = fixture(async () => [200, { code: 500, message: 'synthetic rejection' }])
    assert.equal(await f.logout(), 'unconfirmed'); assert.equal(f.auth.isLoggedIn, false); assert.equal(f.calls.length, 1)
  })
  await test('concurrent composable callers share one logout flight', async () => {
    const d = deferred(), f = fixture(() => d.promise), other = useAuth()
    const first = f.logout(), second = other.logout(); assert.equal(first, second); await flush(); assert.equal(f.calls.length, 1)
    assert.equal(f.logoutPending.value, true); assert.equal(other.logoutPending.value, true)
    d.resolve([200]); assert.deepEqual(await Promise.all([first, second]), ['confirmed', 'confirmed']); assert.equal(globalThis.__logoutFixture.routes.length, 1)
    assert.equal(other.logoutPending.value, false)
  })
  await test('late success or failure cannot clear or navigate a new login', async () => {
    for (const status of [200, 503]) {
      const d = deferred(), f = fixture(() => d.promise), pending = f.logout(); await flush()
      f.auth.setAuthTokens('fixture-new', 'fixture-new-refresh'); f.auth.setUserInfo({ id: 902, username: 'new', email: '' })
      d.resolve([status]); assert.equal(await pending, 'superseded'); assert.equal(f.auth.accessToken, 'fixture-new'); assert.equal(f.auth.userInfo.id, 902)
      assert.deepEqual(globalThis.__logoutFixture.routes, [])
    }
  })
  await test('new session logout has its own flight while old logout is pending', async () => {
    const old = deferred(), newer = deferred(), f = fixture(c => c.headers.Authorization === 'Bearer fixture-old' ? old.promise : newer.promise)
    const first = f.logout(); await flush(); f.auth.setAuthTokens('fixture-new', 'fixture-new-refresh')
    const second = f.logout(); await flush(); old.resolve([200]); assert.equal(await first, 'superseded'); assert.equal(f.auth.isLoggedIn, true)
    newer.resolve([200]); assert.equal(await second, 'confirmed'); assert.equal(f.calls.length, 2); assert.equal(globalThis.__logoutFixture.routes.length, 1)
  })
  await test('late refresh cannot resurrect a cleared logout session', async () => {
    const d = deferred(), f = fixture(c => c.url === '/auth/refresh' ? d.promise : Promise.resolve([200]))
    const refresh = refreshSession(); const rejected = assert.rejects(refresh); await flush(); assert.equal(await f.logout(), 'confirmed')
    d.resolve([200, { code: 200, data: { accessToken: 'fixture-rotated', refreshToken: 'fixture-rotated-refresh', userInfo: { id: 901 } } }]); await rejected
    assert.equal(f.auth.isLoggedIn, false); assert.equal(stored.size, 0)
  })
  await test('sidebar locks repeat clicks, then shows honest failure warning and closes drawer', async () => {
    const d = deferred(), f = fixture(c => c.url === '/auth/logout' ? d.promise : Promise.resolve([200, { code: 200, data: { records: [] } }]))
    const app = renderer.createApp({ ...Sidebar, render: () => h('div') }); app.use(f.pinia); app.provide(ssrContextKey, {})
    app.provide(routerKey, { push() {} }); app.provide(routeLocationKey, { path: '/chat' }); app.mount({ children: [] }); await flush()
    const state = app._instance.setupState; state.userMenuOpen = true; state.mobile = true; await state.openDrawer()
    const first = state.handleLogout(); await state.handleLogout(); await flush(); assert.equal(state.loggingOut, true)
    assert.equal(f.calls.filter(c => c.url === '/auth/logout').length, 1)
    d.resolve([503]); await first; assert.equal(state.loggingOut, false); assert.equal(state.userMenuOpen, false); assert.equal(state.drawerOpen, false)
    assert.equal(document.body.style.overflow, ''); assert.deepEqual(globalThis.__logoutFixture.warnings, ['本地登录信息已清除，服务端退出未确认。'])
    app.unmount()
  })
} finally {
  await server.close(); delete globalThis.localStorage; delete globalThis.document; delete globalThis.window; delete globalThis.__logoutFixture
}
