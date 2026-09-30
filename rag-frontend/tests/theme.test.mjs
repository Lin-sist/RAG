import { createServer as createHttpServer } from 'node:http'
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createServer } from 'vite'
import { createPinia } from 'pinia'
const server = await createServer({ configFile: false, optimizeDeps: { noDiscovery: true, include: [] },
  server: { middlewareMode: true, hmr: { server: createHttpServer() } }, appType: 'custom' })
try {
  const { useThemeStore } = await server.ssrLoadModule('/src/stores/theme.ts')
  function fixture(saved, dark = false, fail = false) {
    const classes = new Set(), listeners = new Set(), writes = []
    const env = { root: { classList: { toggle(name, on) { on ? classes.add(name) : classes.delete(name) } }, style: {} },
      media: { matches: dark, addEventListener(_, fn) { listeners.add(fn) }, removeEventListener(_, fn) { listeners.delete(fn) } },
      read() { if (fail) throw Error('storage unavailable'); return saved },
      write(value) { if (fail) throw Error('storage unavailable'); writes.push(value) } }
    const store = useThemeStore(createPinia())
    store.initialize(env)
    return { store, classes, listeners, writes, env, change(matches) { env.media.matches = matches; for (const fn of listeners) fn({ matches }) } }
  }
  await test('legacy dark/light values apply before mounting and override system changes', () => {
    for (const value of ['dark', 'light']) {
      const f = fixture(value, value === 'light')
      assert.equal(f.store.preference, value); assert.equal(f.store.effectiveTheme, value)
      assert.equal(f.classes.has(value), true); assert.equal(f.env.root.style.colorScheme, value)
      f.change(value === 'light'); assert.equal(f.store.effectiveTheme, value)
      f.store.dispose(); assert.equal(f.listeners.size, 0)
    }
  })
  await test('missing, invalid and system values follow OS; manual override then system works', () => {
    for (const value of [null, 'invalid', 'system']) {
      const f = fixture(value)
      assert.equal(f.store.preference, 'system')
      f.change(true); assert.equal(f.store.effectiveTheme, 'dark')
      f.store.setPreference('light'); f.change(true); assert.equal(f.store.effectiveTheme, 'light')
      f.store.setPreference('system'); assert.equal(f.store.effectiveTheme, 'dark')
      assert.deepEqual(f.writes, ['light', 'system'])
      f.store.dispose()
    }
  })
  await test('initialization is idempotent and dispose permits clean reinitialization', () => {
    const f = fixture('dark')
    f.store.initialize(f.env); assert.equal(f.listeners.size, 1)
    f.store.dispose(); assert.equal(f.listeners.size, 0)
    f.store.initialize(f.env); assert.equal(f.listeners.size, 1)
    f.store.dispose()
  })
  await test('blocked storage still permits an in-memory selection without claiming persistence', () => {
    const f = fixture(null, false, true)
    assert.equal(f.store.preference, 'system'); assert.equal(f.store.persistenceAvailable, false)
    f.store.setPreference('dark'); assert.equal(f.store.effectiveTheme, 'dark')
    assert.equal(f.classes.has('dark'), true); assert.equal(f.store.persistenceAvailable, false)
    f.store.dispose()
  })
} finally { await server.close() }
