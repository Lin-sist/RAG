import { createServer as createHttpServer } from 'node:http'
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { createServer } from 'vite'
import vue from '@vitejs/plugin-vue'
import { createRenderer, getCurrentInstance, h, nextTick, ssrContextKey } from 'vue'
import { createPinia } from 'pinia'
import { fileURLToPath } from 'node:url'

const server = await createServer({ configFile: false, optimizeDeps: { noDiscovery: true, include: [] },
  server: { middlewareMode: true, hmr: { server: createHttpServer() } }, appType: 'custom',
  resolve: { alias: { '@': fileURLToPath(new URL('../src', import.meta.url)) } }, plugins: [vue(), {
    name: 'component-motion-fixture', enforce: 'pre',
    transform(code, id) { if (id.endsWith('LoginLogoMotion.vue')) return code.replace("from '@/utils/loginLogoMotion'", "from 'component-motion-fixture'") },
    resolveId(id) { if (id === 'component-motion-fixture') return '\0component-motion-fixture' },
    load(id) { if (id === '\0component-motion-fixture') return `export const mount=(canvas,options)=>globalThis.__motionComponent.mount(canvas,options)` },
  }] })

function environment(failure = '', reduced = false) {
  const callbacks = new Map(), canvasListeners = new Map(), documentListeners = new Map(), mediaListeners = new Set()
  const stats = { created: new Map(), deleted: new Map(), uniforms: {}, draws: 0 }, observers = []
  let now = 100, sequence = 0
  const resource = type => { if (failure === type) return null; const value = { type, id: ++sequence }; stats.created.set(type, (stats.created.get(type) || 0) + 1); return value }
  const release = value => { if (value) stats.deleted.set(value.type, (stats.deleted.get(value.type) || 0) + 1) }
  const noop = () => {}
  const gl = {
    getShaderPrecisionFormat: () => ({ precision: failure === 'precision' ? 0 : 23 }), createShader: () => resource('shader'), deleteShader: release,
    shaderSource: noop, compileShader: noop, getShaderParameter: () => failure !== 'compile', createProgram: () => resource('program'),
    deleteProgram: release, attachShader: noop, linkProgram: noop, getProgramParameter: () => failure !== 'link', useProgram: noop,
    createBuffer: () => resource('buffer'), deleteBuffer: release, bindBuffer: noop, bufferData: noop, getAttribLocation: () => 0,
    enableVertexAttribArray: noop, vertexAttribPointer: noop, getUniformLocation: (_, name) => name,
    createTexture: () => resource('texture'), deleteTexture: release, activeTexture: noop, bindTexture: noop, texParameteri: noop, texImage2D: noop,
    viewport: noop, uniform1i: noop, uniform2f: noop,
    uniform1f: (name, value) => { stats.uniforms[name] = value }, uniform3fv: (name, value) => { stats.uniforms[name] = [...value] },
    drawArrays() { if (failure === 'draw') throw Error('synthetic draw'); stats.draws++ },
  }
  const stage = { dataset: {} }, canvas = { parentElement: stage, dataset: {}, width: 0, height: 0,
    getContext: () => failure === 'webgl' ? null : gl, getBoundingClientRect: () => ({ width: 560 }),
    addEventListener: (name, fn) => canvasListeners.set(name, fn), removeEventListener: name => canvasListeners.delete(name) }
  const media = { matches: reduced, addEventListener: (_, fn) => mediaListeners.add(fn), removeEventListener: (_, fn) => mediaListeners.delete(fn) }
  const doc = { hidden: false, addEventListener: (name, fn) => documentListeners.set(name, fn), removeEventListener: name => documentListeners.delete(name),
    createElement: () => ({ getContext: () => failure === 'atlas' ? null : { fillText() {} } }) }
  globalThis.document = doc; globalThis.window = { devicePixelRatio: 2, matchMedia: () => media }
  globalThis.getComputedStyle = () => ({ getPropertyValue: name => name === '--art-ink' ? '#34593d' : '#9b7b40' })
  globalThis.requestAnimationFrame = fn => { const id = ++sequence; callbacks.set(id, fn); return id }
  globalThis.cancelAnimationFrame = id => callbacks.delete(id)
  globalThis.ResizeObserver = class { constructor(fn) { this.fn = fn; this.disconnected = false; observers.push(this) } observe() {} disconnect() { this.disconnected = true } }
  globalThis.IntersectionObserver = class extends globalThis.ResizeObserver {}
  const emit = (name, event = { preventDefault() {} }) => canvasListeners.get(name)?.(event)
  return { canvas, stage, stats, callbacks, observers, gl, doc, documentListeners, canvasListeners, mediaListeners,
    tick(ms = 100) { now += ms; const queued = [...callbacks.values()]; callbacks.clear(); queued.forEach(fn => fn(now)) },
    reduce(value) { media.matches = value; [...mediaListeners].forEach(fn => fn({ matches: value })) },
    hide(value) { doc.hidden = value; documentListeners.get('visibilitychange')?.() },
    intersect(value) { observers[1].fn([{ isIntersecting: value }]) }, emit,
  }
}
const renderer = createRenderer({ createElement: type => ({ type, children: [] }), createText: text => ({ text }), createComment: text => ({ text }),
  setText(n, t) { n.text = t }, setElementText(n, t) { n.text = t }, insert(n, p) { n.parent = p; p.children.push(n) },
  remove(n) { n.parent.children = n.parent.children.filter(x => x !== n) }, parentNode: n => n.parent, nextSibling: () => null, patchProp() {} })
try {
  const { mount } = await server.ssrLoadModule('/src/utils/loginLogoMotion.ts')
  await test('approved vertex/fragment strings and static SVG are unchanged', async () => {
    const [source, reference, svg, originalSvg] = await Promise.all([
      readFile(new URL('../src/utils/loginLogoMotion.ts', import.meta.url), 'utf8'), readFile(new URL('../../prototype/rag-login-motion-demo/js/logo-motion.js', import.meta.url), 'utf8'),
      readFile(new URL('../src/assets/login-logo.svg', import.meta.url), 'utf8'), readFile(new URL('../../prototype/rag-login-motion-demo/assets/logo.svg', import.meta.url), 'utf8'),
    ])
    for (const name of ['VERTEX', 'FRAGMENT']) {
      const expression = new RegExp('const ' + name + ' = `([\\s\\S]*?)`;')
      assert.equal(source.match(expression)[1], reference.match(expression)[1])
    }
    assert.equal(svg.replaceAll('\r\n', '\n'), originalSvg.replaceAll('\r\n', '\n'))
  })
  await test('unsupported WebGL, shader, program, buffer, atlas or texture fails static and releases resources', () => {
    for (const failure of ['webgl', 'precision', 'shader', 'compile', 'program', 'link', 'buffer', 'atlas', 'texture', 'draw']) {
      const f = environment(failure), motion = mount(f.canvas)
      assert.equal(f.stage.dataset.renderer, 'static', failure); assert.equal(f.callbacks.size, 0, failure)
      motion.destroy(); motion.destroy()
      for (const [kind, count] of f.stats.created) assert.equal(f.stats.deleted.get(kind) || 0, count, failure + ':' + kind)
      assert.equal(f.canvasListeners.size, 0); assert.equal(f.mediaListeners.size, 0); assert.equal(f.documentListeners.size, 0)
    }
  })
  await test('missing observer APIs or partially failed observer setup degrades without a running animation', () => {
    for (const missing of ['ResizeObserver', 'IntersectionObserver']) {
      const f = environment(); delete globalThis[missing]; const motion = mount(f.canvas)
      assert.equal(f.stage.dataset.renderer, 'static'); assert.equal(f.callbacks.size, 0); motion.destroy()
    }
    const f = environment(); globalThis.IntersectionObserver = class { constructor() { throw Error('synthetic setup') } }
    const motion = mount(f.canvas); assert.equal(f.stage.dataset.renderer, 'static'); assert.ok(f.observers.every(x => x.disconnected)); motion.destroy()
  })
  await test('single active clock cycles dots/cross/ascii with a transparent switch and bounded canvas size', () => {
    const f = environment(), motion = mount(f.canvas)
    assert.equal(f.stage.dataset.renderer, 'webgl'); assert.equal(f.canvas.width, 840); assert.equal(f.callbacks.size, 1)
    const seen = new Set(), switches = []; let old = f.canvas.dataset.texture
    for (let i = 0; i < 192; i++) {
      f.tick(); seen.add(f.canvas.dataset.texture)
      if (f.canvas.dataset.texture !== old) { switches.push(Number(f.canvas.dataset.opacity)); old = f.canvas.dataset.texture }
      assert.equal(f.callbacks.size, 1)
    }
    assert.deepEqual([...seen], ['dots', 'cross', 'ascii']); assert.ok(switches.length >= 3); assert.ok(switches.every(x => x === 0))
    motion.destroy()
  })
  await test('reduce during a fade shows a full static frame and freezes active time; returning resumes once', () => {
    const f = environment(), motion = mount(f.canvas); for (let i = 0; i < 7; i++) f.tick()
    assert.ok(Number(f.canvas.dataset.opacity) < 1); f.reduce(true)
    const phase = f.canvas.dataset.phase; assert.equal(f.canvas.dataset.opacity, '1.000'); assert.equal(f.callbacks.size, 0)
    f.tick(9000); motion.refresh(); assert.equal(f.canvas.dataset.phase, phase)
    f.reduce(false); assert.equal(f.callbacks.size, 1); f.tick(); assert.equal(f.canvas.dataset.phase, phase); motion.destroy()
  })
  await test('hidden/offscreen views freeze the animation clock and ignore late observer callbacks after destroy', () => {
    const f = environment(), motion = mount(f.canvas); f.tick(); f.tick(); const phase = f.canvas.dataset.phase
    for (const change of [value => f.hide(value), value => f.intersect(!value)]) {
      change(true); assert.equal(f.callbacks.size, 0); f.tick(15000); assert.equal(f.canvas.dataset.phase, phase)
      change(false); assert.equal(f.callbacks.size, 1); f.tick(); assert.equal(f.canvas.dataset.phase, phase)
    }
    motion.destroy(); const frames = f.stats.draws; f.observers[0].fn(); f.observers[1].fn([{ isIntersecting: true }]); motion.refresh()
    assert.equal(f.callbacks.size, 0); assert.equal(f.stats.draws, frames)
  })
  await test('context loss falls back; restore rebuilds resources; disposal prevents subsequent restore', () => {
    const states = [], f = environment(), motion = mount(f.canvas, { onState: s => states.push(s.renderer) })
    f.emit('webglcontextlost'); assert.equal(f.stage.dataset.renderer, 'static'); assert.equal(f.callbacks.size, 0)
    f.emit('webglcontextrestored'); assert.equal(f.stage.dataset.renderer, 'webgl'); assert.equal(f.callbacks.size, 1)
    motion.destroy(); const count = f.stats.created.get('program'); f.emit('webglcontextrestored'); assert.equal(f.stats.created.get('program'), count)
    assert.ok(states.includes('static')); assert.equal(f.canvasListeners.size, 0)
  })
  await test('theme refresh redraws colors without creating another clock; missing parent is inert', () => {
    const f = environment('', true), motion = mount(f.canvas)
    globalThis.getComputedStyle = () => ({ getPropertyValue: name => name === '--art-ink' ? '#eeeeea' : '#c9a15a' })
    motion.refresh(); assert.equal(f.stats.uniforms.u_ink[0], 238 / 255); assert.equal(f.callbacks.size, 0); motion.destroy()
    assert.doesNotThrow(() => mount({ parentElement: null }).destroy())
  })
  await test('Vue mount/theme/unmount/remount uses explicit controller; late state cannot update an unmounted component', async () => {
    const calls = []
    globalThis.__motionComponent = { mount(canvas, options) { const entry = { canvas, options, refreshes: 0, destroyed: 0 }; calls.push(entry); return { refresh() { entry.refreshes++ }, destroy() { entry.destroyed++ } } } }
    const { useThemeStore } = await server.ssrLoadModule('/src/stores/theme.ts'), Component = (await server.ssrLoadModule('/src/components/common/LoginLogoMotion.vue')).default
    const pinia = createPinia(), theme = useThemeStore(pinia)
    function create() {
      const app = renderer.createApp({ ...Component, render() { const instance = getCurrentInstance(); return h('canvas', { ref: el => { instance.setupState.canvas = el } }) } })
      app.use(pinia); app.provide(ssrContextKey, {}); app.mount({ children: [] }); return app
    }
    const first = create(); assert.equal(calls.length, 1)
    theme.setPreference('dark'); await nextTick(); await nextTick(); assert.equal(calls[0].refreshes, 1)
    theme.setPreference('light'); const state = first._instance.setupState; first.unmount(); await nextTick(); await nextTick()
    assert.equal(calls[0].destroyed, 1); assert.equal(calls[0].refreshes, 1)
    calls[0].options.onState({ renderer: 'webgl' }); assert.equal(state.state.renderer, 'static')
    const second = create(); assert.equal(calls.length, 2); second.unmount(); assert.equal(calls[1].destroyed, 1)
  })
} finally {
  await server.close()
  for (const key of ['window', 'document', 'getComputedStyle', 'requestAnimationFrame', 'cancelAnimationFrame', 'ResizeObserver', 'IntersectionObserver', '__motionComponent']) delete globalThis[key]
}
