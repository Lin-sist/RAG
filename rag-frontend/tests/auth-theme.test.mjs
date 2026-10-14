import { createServer as createHttpServer } from 'node:http'
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createServer } from 'vite'
import vue from '@vitejs/plugin-vue'
import { createRenderer, h, ssrContextKey } from 'vue'
import { createPinia } from 'pinia'
import { fileURLToPath } from 'node:url'

const server = await createServer({ configFile: false, optimizeDeps: { noDiscovery: true, include: [] },
  server: { middlewareMode: true, hmr: { server: createHttpServer() } }, appType: 'custom',
  resolve: { alias: { '@': fileURLToPath(new URL('../src', import.meta.url)) } }, plugins: [vue()] })
const renderer = createRenderer({ createElement: type => ({ type, children: [] }), createText: text => ({ text }), createComment: text => ({ text }),
  setText(n, t) { n.text = t }, setElementText(n, t) { n.text = t }, insert(n, p) { n.parent = p; p.children.push(n) },
  remove(n) { n.parent.children = n.parent.children.filter(x => x !== n) }, parentNode: n => n.parent, nextSibling: () => null, patchProp() {} })
try {
  const Layout = (await server.ssrLoadModule('/src/layouts/AuthLayout.vue')).default
  const { useThemeStore } = await server.ssrLoadModule('/src/stores/theme.ts')
  function fixture(saved = 'system', blocked = false) {
    const pinia = createPinia(), theme = useThemeStore(pinia), classes = new Set(), listeners = new Set(), writes = []
    theme.initialize({ root: { classList: { toggle: (k, on) => on ? classes.add(k) : classes.delete(k) }, style: {} },
      media: { matches: false, addEventListener: (_, fn) => listeners.add(fn), removeEventListener: (_, fn) => listeners.delete(fn) },
      read() { if (blocked) throw Error('synthetic blocked storage'); return saved },
      write(value) { if (blocked) throw Error('synthetic blocked storage'); writes.push(value) } })
    function mount() {
      const app = renderer.createApp({ ...Layout, render: () => h('div') }); app.use(pinia); app.provide(ssrContextKey, {}); app.mount({ children: [] })
      return { app, state: app._instance.setupState }
    }
    return { theme, classes, writes, mount, change: matches => { for (const fn of listeners) fn({ matches }) } }
  }
  await test('login follows shared system theme live and manual choice wins over later OS changes', () => {
    const f = fixture(), m = f.mount(); assert.equal(m.state.isDark, false)
    f.change(true); assert.equal(m.state.isDark, true); assert.equal(f.theme.preference, 'system')
    m.state.toggleTheme(); assert.equal(f.theme.preference, 'light'); assert.equal(f.classes.has('light'), true)
    f.change(true); assert.equal(m.state.isDark, false); assert.deepEqual(f.writes, ['light']); m.app.unmount(); f.theme.dispose()
  })
  await test('login uses existing shared choice and preserves it across layout unmount/remount', () => {
    const f = fixture('dark'), first = f.mount(); assert.equal(first.state.isDark, true)
    first.state.toggleTheme(); first.app.unmount(); const second = f.mount()
    assert.equal(second.state.isDark, false); assert.equal(f.theme.effectiveTheme, 'light'); assert.deepEqual(f.writes, ['light'])
    f.theme.setPreference('dark'); assert.equal(second.state.isDark, true); second.app.unmount(); f.theme.dispose()
  })
  await test('blocked storage does not prevent login rendering or in-memory theme switching', () => {
    const f = fixture('system', true), m = f.mount(); assert.equal(f.theme.persistenceAvailable, false)
    assert.doesNotThrow(() => m.state.toggleTheme()); assert.equal(m.state.isDark, true); assert.equal(f.classes.has('dark'), true)
    assert.equal(f.theme.persistenceAvailable, false); m.app.unmount(); f.theme.dispose()
  })
} finally { await server.close() }
