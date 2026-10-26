import { clamp, makeModels, morphPoint, projectionScale, spring, wheelTarget } from './loginParticleGeometry'

const VERTEX = `
  attribute vec4 a_particle;
  uniform float u_time, u_aspect, u_size, u_scale;
  varying float v_tone, v_alpha;
  void main() {
    vec3 p = a_particle.xyz;
    float yaw = .19 + sin(u_time * .22) * .10;
    float c = cos(yaw), s = sin(yaw);
    p = vec3(p.x*c+p.z*s, p.y, p.z*c-p.x*s);
    p.y += sin(u_time*.6) * .016;
    float depth = 3.8 / (3.8-p.z*.45);
    gl_Position = vec4(p.x * u_scale * depth / u_aspect, p.y * u_scale * depth, -p.z*.15, 1.0);
    gl_PointSize = u_size * depth;
    v_tone = a_particle.w;
    v_alpha = .66 + .30 * (p.z + .6) / 1.2;
  }
`
const FRAGMENT = `
  precision mediump float;
  uniform vec3 u_ink, u_accent;
  varying float v_tone, v_alpha;
  void main() {
    float coverage = 1.0 - smoothstep(.25, .5, length(gl_PointCoord - vec2(.5)));
    gl_FragColor = vec4(mix(u_ink, u_accent, v_tone * .85), coverage * clamp(v_alpha, .3, .95));
  }
`
export interface ParticleState { renderer: 'static' | 'webgl'; scene: number; progress: number; paused: boolean; reduced: boolean }
export interface ParticleController { select(index: number): void; pause(): void; refresh(): void; destroy(): void }

export function mountParticles(canvas: HTMLCanvasElement, onState: (state: ParticleState) => void = () => {}): ParticleController {
  let gl: WebGLRenderingContext | null = null, program: WebGLProgram | null = null, buffer: WebGLBuffer | null = null
  let uniforms: Record<string, WebGLUniformLocation | null> = {}, count = 0, models: Float32Array[] = []
  let points = new Float32Array(), offsets = new Float32Array()
  let disposed = false, lost = false, visible = true, paused = false, position = 0, target = 0, velocity = 0, time = 0
  let raf = 0, last = 0, reported = '', frameCount = 0
  let media: MediaQueryList | undefined, resizeObserver: ResizeObserver | undefined, intersection: IntersectionObserver | undefined
  const cleanups: Array<() => void> = []
  const pointer = { active: false, x: 0, y: 0, px: 0, py: 0 }
  const safe = (fn: () => void) => { try { fn() } catch { /* decoration never blocks authentication */ } }
  const available = () => !!program && !lost && !disposed
  const drawable = () => available() && visible && !document.hidden
  const moving = () => drawable() && !paused && !media?.matches
  function report() {
    if (disposed) return
    const progress = available() ? Number(position.toFixed(4)) : 0
    const state: ParticleState = { renderer: available() ? 'webgl' : 'static', scene: Math.round(progress), progress, paused, reduced: media?.matches ?? false }
    const key = JSON.stringify(state)
    if (key !== reported) { reported = key; safe(() => onState(state)) }
  }
  function stop() { if (raf) safe(() => cancelAnimationFrame(raf)); raf = last = 0 }
  function release() {
    if (gl) { if (buffer) safe(() => gl!.deleteBuffer(buffer)); if (program) safe(() => gl!.deleteProgram(program)) }
    program = buffer = null; uniforms = {}
  }
  function fail() {
    stop(); pointer.active = false; release()
    // A lost context keeps its restoration listener; other failures fully detach.
    if (!lost) cleanup()
    canvas.dataset.renderer = 'static'; report()
  }
  function listen(host: EventTarget, name: string, fn: EventListener, options?: AddEventListenerOptions) {
    cleanups.push(() => host.removeEventListener(name, fn, options))
    host.addEventListener(name, fn, options)
  }
  function cleanup() {
    safe(() => resizeObserver?.disconnect()); safe(() => intersection?.disconnect())
    for (const remove of cleanups.splice(0)) safe(remove)
  }
  function compile(type: number, source: string) {
    const shader = gl!.createShader(type)
    if (!shader) throw Error('Particle shader unavailable')
    try {
      gl!.shaderSource(shader, source); gl!.compileShader(shader)
      if (!gl!.getShaderParameter(shader, gl!.COMPILE_STATUS)) throw Error('Particle shader failed')
      return shader
    } catch (error) { safe(() => gl!.deleteShader(shader)); throw error }
  }
  function initialize() {
    let vertex: WebGLShader | undefined, fragment: WebGLShader | undefined
    try {
      gl = canvas.getContext('webgl', { alpha: true, antialias: false, premultipliedAlpha: false, powerPreference: 'low-power' })
      if (!gl) throw Error('Particle context unavailable')
      vertex = compile(gl.VERTEX_SHADER, VERTEX); fragment = compile(gl.FRAGMENT_SHADER, FRAGMENT)
      program = gl.createProgram()
      if (!program) throw Error('Particle program unavailable')
      gl.attachShader(program, vertex); gl.attachShader(program, fragment); gl.linkProgram(program)
      if (!gl.getProgramParameter(program, gl.LINK_STATUS)) throw Error('Particle link failed')
      buffer = gl.createBuffer()
      if (!buffer) throw Error('Particle buffer unavailable')
      gl.useProgram(program); gl.bindBuffer(gl.ARRAY_BUFFER, buffer)
      const attribute = gl.getAttribLocation(program, 'a_particle')
      if (attribute < 0) throw Error('Particle attribute unavailable')
      gl.enableVertexAttribArray(attribute); gl.vertexAttribPointer(attribute, 4, gl.FLOAT, false, 0, 0)
      uniforms = Object.fromEntries(['time', 'aspect', 'size', 'scale', 'ink', 'accent'].map(name => [name, gl!.getUniformLocation(program!, 'u_' + name)]))
      gl.enable(gl.BLEND); gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA); gl.clearColor(0, 0, 0, 0)
      canvas.dataset.renderer = 'webgl'
    } catch { fail() }
    finally { if (gl && vertex) safe(() => gl!.deleteShader(vertex!)); if (gl && fragment) safe(() => gl!.deleteShader(fragment!)) }
  }
  function color(style: CSSStyleDeclaration, name: string, fallback: string) {
    const raw = style.getPropertyValue(name).trim(), hex = /^#[a-f0-9]{6}$/i.test(raw) ? raw : fallback
    return [1, 3, 5].map(start => parseInt(hex.slice(start, start + 2), 16) / 255)
  }
  function draw(dt = 0) {
    if (!drawable() || !gl || !program) return
    try {
      const rect = canvas.getBoundingClientRect(), width = Math.max(1, rect.width), height = Math.max(1, rect.height)
      const budget = width < 360 ? 2000 : 4800
      if (count !== budget) { count = budget; models = makeModels(count); points = new Float32Array(count * 4); offsets = new Float32Array(count * 4) }
      const dpr = Math.min(window.devicePixelRatio || 1, 1.5, 1200 / Math.max(width, height))
      const w = Math.max(1, Math.round(width * dpr)), h = Math.max(1, Math.round(height * dpr))
      if (canvas.width !== w || canvas.height !== h) { canvas.width = w; canvas.height = h }
      if (moving()) {
        time += dt
        ;[position, velocity] = spring(position, velocity, target, dt)
        position = clamp(position)
        if (Math.abs(target - position) < .00001 && Math.abs(velocity) < .0001) { position = target; velocity = 0 }
      }
      const seconds = Math.max(0, dt), decay = Math.exp(-18 * seconds)
      let maxOffset = 0
      for (let i = 0; i < count; i++) {
        const at = i * 4
        morphPoint(models, i, position, points, at)
        let fx = 0, fy = 0
        if (pointer.active && moving()) {
          const sx = pointer.x - pointer.px, sy = pointer.y - pointer.py, length = sx * sx + sy * sy
          const along = length ? clamp(((points[at] - pointer.px) * sx + (points[at + 1] - pointer.py) * sy) / length, 0, 1) : 1
          const dx = points[at] - pointer.px - sx * along, dy = points[at + 1] - pointer.py - sy * along
          const strength = Math.max(0, 1 - Math.hypot(dx, dy) / .4) ** 2
          fx = clamp((dx * .9 + sx * 1.5) * strength, -.2, .2)
          fy = clamp((dy * .9 + sy * 1.5) * strength, -.2, .2)
        }
        for (let axis = 0; axis < 2; axis++) {
          const force = axis ? fy : fx, slot = at + axis, gap = offsets[slot] - force
          const impulse = offsets[slot + 2] + 18 * gap
          offsets[slot] = force + (gap + impulse * seconds) * decay
          offsets[slot + 2] = (offsets[slot + 2] - 18 * impulse * seconds) * decay
          points[slot] += offsets[slot]
        }
        maxOffset = Math.max(maxOffset, Math.hypot(offsets[at], offsets[at + 1]))
      }
      pointer.px = pointer.x; pointer.py = pointer.y
      const style = getComputedStyle(canvas)
      gl.useProgram(program); gl.viewport(0, 0, w, h); gl.clear(gl.COLOR_BUFFER_BIT)
      gl.uniform1f(uniforms.time, time); gl.uniform1f(uniforms.aspect, width / height)
      gl.uniform1f(uniforms.scale, projectionScale(width / height))
      gl.uniform1f(uniforms.size, (width < 360 ? 1.9 : 2.3) * dpr)
      gl.uniform3fv(uniforms.ink, color(style, '--art-ink', '#34593d'))
      gl.uniform3fv(uniforms.accent, color(style, '--art-accent', '#9b7b40'))
      gl.bindBuffer(gl.ARRAY_BUFFER, buffer); gl.bufferData(gl.ARRAY_BUFFER, points, gl.DYNAMIC_DRAW); gl.drawArrays(gl.POINTS, 0, count)
      canvas.dataset.frame = String(++frameCount); canvas.dataset.progress = position.toFixed(4)
      canvas.dataset.points = String(count); canvas.dataset.offset = maxOffset.toFixed(4); canvas.dataset.time = time.toFixed(3)
      report()
    } catch { fail() }
  }
  function frame(now: number) {
    raf = 0
    if (!moving()) return
    if (!last || now - last >= 1000 / 30) {
      const dt = last ? Math.min((now - last) / 1000, .08) : 0
      last = now; draw(dt)
    }
    if (moving()) { try { raf = requestAnimationFrame(frame) } catch { fail() } }
  }
  function refresh() {
    if (disposed) return
    stop(); draw(); report()
    if (moving()) { try { raf = requestAnimationFrame(frame) } catch { fail() } }
  }
  function leave() { pointer.active = false }
  function still() { position = target = Math.round(position); velocity = 0; offsets.fill(0); leave() }
  function modeChange() { if (disposed) return; if (media?.matches) still(); refresh() }
  function move(event: PointerEvent) {
    if (!moving() || event.pointerType === 'touch') return
    const r = canvas.getBoundingClientRect()
    const unit = r.height * .5 * projectionScale(r.width / r.height)
    const x = (event.clientX - r.left - r.width / 2) / unit
    const y = -(event.clientY - r.top - r.height / 2) / unit
    if (!pointer.active) { pointer.px = x; pointer.py = y }
    pointer.x = x; pointer.y = y; pointer.active = true
  }
  function wheel(event: WheelEvent) {
    if (!moving()) return
    const next = wheelTarget(target, event, canvas.getBoundingClientRect().height)
    if (next !== target) { event.preventDefault(); target = next }
  }
  function visibilityChange() { if (disposed) return; leave(); refresh() }
  function contextLost(event: Event) { if (disposed) return; event.preventDefault(); lost = true; fail() }
  function contextRestored() { if (disposed) return; lost = false; initialize(); refresh() }
  try {
    if (typeof ResizeObserver === 'undefined' || typeof IntersectionObserver === 'undefined'
      || typeof requestAnimationFrame !== 'function' || typeof cancelAnimationFrame !== 'function') throw Error('Particle browser APIs unavailable')
    media = window.matchMedia('(prefers-reduced-motion: reduce)'); initialize()
    if (program) {
      resizeObserver = new ResizeObserver(refresh); resizeObserver.observe(canvas)
      intersection = new IntersectionObserver(entries => { if (!disposed) { visible = entries[entries.length - 1]?.isIntersecting ?? false; visibilityChange() } }); intersection.observe(canvas)
      listen(canvas, 'wheel', wheel as EventListener, { passive: false })
      listen(canvas, 'pointermove', move as EventListener, { passive: true })
      for (const name of ['pointerleave', 'pointercancel', 'pointerup']) listen(canvas, name, leave)
      listen(window, 'blur', leave); listen(document, 'visibilitychange', visibilityChange); listen(media, 'change', modeChange)
      listen(canvas, 'webglcontextlost', contextLost); listen(canvas, 'webglcontextrestored', contextRestored); refresh()
    }
  } catch { cleanup(); fail() }
  report()
  return {
    select(index) {
      if (!available() || !Number.isFinite(index)) return
      target = Math.round(clamp(index)); leave()
      if (paused || media?.matches) { position = target; velocity = 0; offsets.fill(0) }
      refresh()
    },
    pause() { if (!available() || media?.matches) return; paused = !paused; if (paused) still(); refresh() },
    refresh,
    destroy() {
      if (disposed) return
      disposed = true; stop(); leave(); cleanup(); release(); canvas.width = canvas.height = 0
      models = []; points = offsets = new Float32Array()
    },
  }
}
