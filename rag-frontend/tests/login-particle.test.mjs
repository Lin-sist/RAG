import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createServer as createHttpServer } from 'node:http'
import { createServer } from 'vite'
import { fileURLToPath } from 'node:url'
import { readFile } from 'node:fs/promises'
import vue from '@vitejs/plugin-vue'
import { createRenderer, getCurrentInstance, h, nextTick, ssrContextKey } from 'vue'
import { createPinia } from 'pinia'

const server = await createServer({ configFile: false, optimizeDeps: { noDiscovery: true, include: [] },
  server: { middlewareMode: true, hmr: { server: createHttpServer() } }, appType: 'custom',
  resolve: { alias: { '@': fileURLToPath(new URL('../src', import.meta.url)) } }, plugins: [vue(), {
    name: 'particle-component-fixture', enforce: 'pre',
    transform(source, id) { if (id.endsWith('LoginParticleScene.vue')) return source.replace("from '@/utils/loginParticleMotion'", "from 'particle-component-fixture'") },
    resolveId(id) { if (id === 'particle-component-fixture') return '\0particle-component-fixture' },
    load(id) { if (id === '\0particle-component-fixture') return 'export const mountParticles=(canvas,cb)=>globalThis.__particleComponent(canvas,cb)' },
  }] })
const saved = new Map()
function install(name, value) {
  if (!saved.has(name)) saved.set(name, Object.getOwnPropertyDescriptor(globalThis, name))
  Object.defineProperty(globalThis, name, { configurable: true, writable: true, value })
}
function fixture(failure = '', reduced = false, width = 560) {
  const callbacks = new Map(), resources = new Set(), removed = new Set(), observers = [], uniforms = {}
  let clock = 100, sequence = 0, draws = 0, data
  const host = props => Object.assign({ listeners: new Map(),
    addEventListener(name, fn, options) { this.listeners.set(name, { fn, options }) },
    removeEventListener(name) { this.listeners.delete(name) },
    emit(name, fields = {}) { let prevented = false; this.listeners.get(name)?.fn({ preventDefault() { prevented = true }, ...fields }); return prevented },
  }, props)
  const resource = type => { if (failure === type) return null; const item = { type, id: ++sequence }; resources.add(item); return item }
  const noop = () => {}
  const gl = {
    createShader: () => resource('shader'), deleteShader: item => removed.add(item), shaderSource: noop, compileShader: noop,
    getShaderParameter: () => failure !== 'compile', createProgram: () => resource('program'), deleteProgram: item => removed.add(item),
    attachShader: noop, linkProgram: noop, getProgramParameter: () => failure !== 'link',
    createBuffer: () => resource('buffer'), deleteBuffer: item => removed.add(item), getAttribLocation: () => failure === 'attribute' ? -1 : 0,
    useProgram: noop, bindBuffer: noop, enableVertexAttribArray: noop, vertexAttribPointer: noop,
    getUniformLocation: (_, name) => name, enable: noop, blendFunc: noop, clearColor: noop, clear: noop, viewport: noop,
    uniform1f: (name, value) => { uniforms[name] = value }, uniform3fv: (name, value) => { uniforms[name] = [...value] },
    bufferData: (_, value) => { data = value.slice() }, drawArrays() { if (failure === 'draw') throw Error('synthetic draw'); draws++ },
  }
  const canvas = host({ dataset: {}, width: 0, height: 0, getContext: () => failure === 'webgl' ? null : gl,
    getBoundingClientRect: () => ({ left: 10, top: 20, width, height: width / 1.12 }) })
  const document = host({ hidden: false }), media = host({ matches: reduced }), window = host({ devicePixelRatio: 4, matchMedia: () => media })
  install('document', document); install('window', window)
  install('getComputedStyle', () => ({ getPropertyValue: name => name === '--art-ink' ? '#34593d' : '#9b7b40' }))
  install('requestAnimationFrame', fn => { const id = ++sequence; callbacks.set(id, fn); return id })
  install('cancelAnimationFrame', id => callbacks.delete(id))
  install('ResizeObserver', class {
    constructor(fn) { this.fn = fn; this.disconnected = false; observers.push(this) }
    observe() {} disconnect() { this.disconnected = true }
  })
  install('IntersectionObserver', class extends globalThis.ResizeObserver {})
  const tick = (ms = 40) => { clock += ms; const work = [...callbacks.values()]; callbacks.clear(); for (const fn of work) fn(clock) }
  return { canvas, document, window, media, callbacks, resources, removed, observers, uniforms,
    tick, advance(n = 60) { for (let i = 0; i < n; i++) tick() },
    get draws() { return draws }, get data() { return data },
    wheel(fields = {}) { return canvas.emit('wheel', { deltaY: 120, deltaX: 0, deltaMode: 0, cancelable: true, ctrlKey: false, metaKey: false, ...fields }) },
  }
}

try {
  const { makeModels, morphPoint, spring, wheelTarget } = await server.ssrLoadModule('/src/utils/loginParticleGeometry.ts')
  const { mountParticles } = await server.ssrLoadModule('/src/utils/loginParticleMotion.ts')
  await test('three deterministic point clouds have equal budgets, meaningful depth and finite bounded coordinates', () => {
    for (const count of [2000, 4800]) {
      const models = makeModels(count)
      assert.equal(models.length, 3); assert.deepEqual(models, makeModels(count))
      for (const points of models) {
        assert.equal(points.length, count * 4)
        assert.ok(points.every(Number.isFinite))
        assert.ok(points.every(value => Math.abs(value) < 1.5))
        const depths = [...points].filter((_, index) => index % 4 === 2)
        assert.ok(Math.max(...depths) - Math.min(...depths) > .2)
      }
      assert.notDeepEqual(models[0], models[1]); assert.notDeepEqual(models[1], models[2])
    }
    for (const count of [0, -1, 4801, NaN, 1.5]) assert.throws(() => makeModels(count))
  })
  await test('morph endpoints match source models, intermediate scatter is deterministic and models stay immutable', () => {
    const models = makeModels(100), before = models.map(value => value.slice()), out = new Float32Array(4), again = new Float32Array(4)
    for (let scene = 0; scene < 3; scene++) for (let i = 0; i < 100; i++) {
      morphPoint(models, i, scene, out)
      assert.ok(out.every((value, axis) => Math.abs(value - models[scene][i * 4 + axis]) < 1e-6))
    }
    morphPoint(models, 31, .5, out); morphPoint(models, 31, .5, again); assert.deepEqual(out, again)
    assert.ok(out.some((value, axis) => axis < 3 && Math.abs(value - (models[0][124 + axis] + models[1][124 + axis]) / 2) > .01))
    const near = new Float32Array(4); morphPoint(models, 31, .5001, near)
    assert.ok(near.every((value, axis) => Math.abs(value - out[axis]) < .002))
    assert.deepEqual(models, before)
  })
  await test('spring retargeting is continuous, settles and has comparable timing at 30 and 60 fps', () => {
    const run = (step, count, target, start = [0, 0]) => { let state = start; for (let i = 0; i < count; i++) state = spring(...state, target, step); return state }
    assert.ok(Math.abs(run(1 / 30, 30, 1)[0] - run(1 / 60, 60, 1)[0]) < 1e-10)
    const mid = run(1 / 30, 4, 1), reverse = spring(...mid, 0, 1 / 60)
    assert.ok(Math.abs(reverse[0] - mid[0]) < .1)
    assert.ok(Math.abs(run(1 / 30, 90, 0, mid)[0]) < 1e-8)
  })
  await test('wheel modes are normalized and bounded; zoom, horizontal, uncancelable and outward endpoints pass through', () => {
    const event = { deltaY: 16, deltaX: 0, deltaMode: 0, ctrlKey: false, metaKey: false, cancelable: true }
    const px = wheelTarget(1, event, 500)
    assert.equal(px, wheelTarget(1, { ...event, deltaY: 1, deltaMode: 1 }, 500))
    assert.equal(px, wheelTarget(1, { ...event, deltaY: 16 / 500, deltaMode: 2 }, 500))
    assert.equal(wheelTarget(1, { ...event, deltaY: 10000 }, 500), 1.18)
    for (const patch of [{ ctrlKey: true }, { metaKey: true }, { cancelable: false }, { deltaX: 100 }, { deltaY: NaN }]) assert.equal(wheelTarget(1, { ...event, ...patch }, 500), 1)
    assert.equal(wheelTarget(2, event, 500), 2); assert.equal(wheelTarget(0, { ...event, deltaY: -16 }, 500), 0)
  })
  await test('new controller draws point clouds within pixel budgets and owns one frame chain', () => {
    for (const width of [300, 560, 1800]) {
      const f = fixture('', false, width), motion = mountParticles(f.canvas)
      assert.equal(f.canvas.dataset.renderer, 'webgl'); assert.equal(f.callbacks.size, 1)
      assert.ok(f.canvas.width <= 1200 && f.canvas.height <= 1200)
      assert.equal(Number(f.canvas.dataset.points), width < 360 ? 2000 : 4800)
      motion.refresh(); motion.refresh(); assert.equal(f.callbacks.size, 1)
      f.advance(5); assert.ok(f.data.every(Number.isFinite)); motion.destroy()
    }
  })
  await test('scroll can stop mid-morph and reverse; only a consumed canvas event is cancelled', () => {
    const f = fixture(), motion = mountParticles(f.canvas)
    assert.equal(f.canvas.listeners.get('wheel').options.passive, false)
    assert.equal(f.document.listeners.has('wheel'), false); assert.equal(f.window.listeners.has('wheel'), false)
    assert.equal(f.wheel({ deltaY: -120 }), false)
    for (let i = 0; i < 4; i++) assert.equal(f.wheel(), true)
    f.advance(); const before = Number(f.canvas.dataset.progress); assert.ok(before > .4 && before < .5)
    assert.equal(f.wheel({ ctrlKey: true }), false); assert.equal(f.wheel({ cancelable: false }), false)
    assert.equal(f.wheel({ deltaY: -120 }), true); f.advance()
    assert.ok(Number(f.canvas.dataset.progress) < before)
    motion.select(2); f.advance(); assert.equal(f.wheel(), false); motion.destroy()
  })
  await test('pause/reduce snap to a complete scene; manual selection paints once without an active clock', () => {
    const f = fixture(), states = [], motion = mountParticles(f.canvas, value => states.push(value))
    motion.select(1); f.advance(5); motion.pause()
    assert.ok(Number.isInteger(Number(f.canvas.dataset.progress))); assert.equal(f.callbacks.size, 0)
    motion.select(2); assert.equal(f.canvas.dataset.progress, '2.0000'); assert.equal(f.callbacks.size, 0); assert.equal(f.wheel(), false)
    motion.pause(); assert.equal(f.callbacks.size, 1)
    f.media.matches = true; f.media.emit('change'); assert.equal(f.callbacks.size, 0)
    motion.select(0); assert.equal(f.canvas.dataset.progress, '0.0000'); assert.equal(states.at(-1).reduced, true)
    const time = f.canvas.dataset.time; f.tick(50000); motion.refresh(); assert.equal(f.canvas.dataset.time, time)
    f.media.matches = false; f.media.emit('change'); assert.equal(f.callbacks.size, 1); motion.destroy()
  })
  await test('pointer creates bounded local offsets then recovers; touch and window blur do not leave a stuck brush', () => {
    const f = fixture(), motion = mountParticles(f.canvas); motion.select(1); f.advance()
    const r = f.canvas.getBoundingClientRect(), center = { clientX: r.left + r.width / 2, clientY: r.top + r.height / 2, pointerType: 'mouse' }
    f.canvas.emit('pointermove', center); f.advance(4)
    f.canvas.emit('pointermove', { ...center, clientX: center.clientX + 35 }); f.advance(4)
    assert.ok(Number(f.canvas.dataset.offset) > .001 && Number(f.canvas.dataset.offset) < .4)
    f.window.emit('blur'); f.advance(); assert.ok(Number(f.canvas.dataset.offset) < .0001)
    f.canvas.emit('pointermove', { ...center, pointerType: 'touch' }); f.advance(5); assert.equal(Number(f.canvas.dataset.offset), 0)
    assert.equal(f.canvas.listeners.has('touchmove'), false); motion.destroy()
  })
  await test('hidden/offscreen views stop drawing and time; theme refresh is deferred until visibility returns', () => {
    const f = fixture(), motion = mountParticles(f.canvas); f.advance(4)
    const time = f.canvas.dataset.time, draws = f.draws
    f.document.hidden = true; f.document.emit('visibilitychange'); assert.equal(f.callbacks.size, 0)
    motion.refresh(); f.tick(50000); assert.equal(f.draws, draws)
    f.document.hidden = false; f.document.emit('visibilitychange'); assert.equal(f.canvas.dataset.time, time)
    f.observers[1].fn([{ isIntersecting: false }]); const offscreen = f.draws; motion.refresh(); assert.equal(f.draws, offscreen)
    f.observers[1].fn([{ isIntersecting: true }]); assert.equal(f.callbacks.size, 1); motion.destroy()
  })
  await test('unsupported and partial initialization failures release every acquired GPU resource', () => {
    for (const failure of ['webgl', 'shader', 'compile', 'program', 'link', 'buffer', 'attribute', 'draw']) {
      const f = fixture(failure), state = [], motion = mountParticles(f.canvas, value => state.push(value))
      assert.equal(state.at(-1).renderer, 'static', failure); assert.equal(f.callbacks.size, 0)
      motion.select(2); motion.pause(); assert.equal(f.callbacks.size, 0)
      motion.destroy(); motion.destroy()
      assert.ok([...f.resources].every(item => f.removed.has(item)), failure)
      assert.equal(f.canvas.listeners.size + f.document.listeners.size + f.window.listeners.size + f.media.listeners.size, 0)
    }
    const f = fixture(); install('IntersectionObserver', class { constructor() { throw Error('synthetic observer failure') } })
    const motion = mountParticles(f.canvas); assert.equal(f.canvas.dataset.renderer, 'static'); assert.ok(f.observers.every(item => item.disconnected)); motion.destroy()
  })
  await test('context restores once; disposal blocks all late callbacks and clears listeners/observers', () => {
    const f = fixture(), motion = mountParticles(f.canvas)
    assert.equal(f.canvas.emit('webglcontextlost'), true); assert.equal(f.canvas.dataset.renderer, 'static'); assert.equal(f.callbacks.size, 0)
    f.canvas.emit('webglcontextrestored'); assert.equal(f.canvas.dataset.renderer, 'webgl'); assert.equal(f.callbacks.size, 1)
    motion.destroy(); const draws = f.draws
    for (const observer of f.observers) observer.fn([{ isIntersecting: true }])
    motion.refresh(); f.canvas.emit('webglcontextrestored'); assert.equal(f.draws, draws); assert.equal(f.callbacks.size, 0)
    assert.ok(f.observers.every(item => item.disconnected)); assert.ok([...f.resources].every(item => f.removed.has(item)))
    assert.equal(f.canvas.width, 0); assert.equal(f.canvas.height, 0)
  })
  await test('Vue integration refreshes one controller, disposes once and ignores late state after unmount', async () => {
    const calls = []
    install('__particleComponent', (canvas, cb) => {
      const entry = { canvas, cb, refreshes: 0, destroyed: 0 }; calls.push(entry)
      return { select() {}, pause() {}, refresh() { entry.refreshes++ }, destroy() { entry.destroyed++ } }
    })
    const { useThemeStore } = await server.ssrLoadModule('/src/stores/theme.ts')
    const Component = (await server.ssrLoadModule('/src/components/common/LoginParticleScene.vue')).default
    const renderer = createRenderer({ createElement: type => ({ type, children: [] }), createText: text => ({ text }), createComment: text => ({ text }),
      setText(n, t) { n.text = t }, setElementText(n, t) { n.text = t }, insert(n, p) { n.parent = p; p.children.push(n) },
      remove(n) { n.parent.children = n.parent.children.filter(x => x !== n) }, parentNode: n => n.parent, nextSibling: () => null, patchProp() {} })
    const pinia = createPinia(), theme = useThemeStore(pinia)
    const app = renderer.createApp({ ...Component, render() { const instance = getCurrentInstance(); return h('canvas', { ref: el => { instance.setupState.canvas = el } }) } })
    app.use(pinia); app.provide(ssrContextKey, {}); app.mount({ children: [] })
    theme.setPreference('dark'); await nextTick(); await nextTick(); assert.equal(calls[0].refreshes, 1)
    const state = app._instance.setupState; theme.setPreference('light'); app.unmount(); await nextTick(); await nextTick()
    calls[0].cb({ renderer: 'webgl', scene: 2, paused: false, reduced: false })
    assert.equal(state.state.renderer, 'static'); assert.equal(calls[0].destroyed, 1); assert.equal(calls[0].refreshes, 1)
    const template = await readFile(new URL('../src/components/common/LoginParticleScene.vue', import.meta.url), 'utf8')
    assert.match(template, /type="button"/); assert.match(template, /aria-pressed/); assert.match(template, /选择粒子图样/)
  })
} finally {
  await server.close()
  for (const [name, descriptor] of saved) { if (descriptor) Object.defineProperty(globalThis, name, descriptor); else delete globalThis[name] }
}
