// Frozen shader geometry from the approved standalone prototype; native WebGL1 only.
  const VERTEX = `
    attribute vec2 a_position;
    void main() { gl_Position = vec4(a_position, 0.0, 1.0); }
  `;
  const FRAGMENT = `
    precision highp float;
    uniform vec2 u_resolution;
    uniform float u_dpr;
    uniform float u_time;
    uniform float u_mode;
    uniform float u_trail;
    uniform float u_opacity;
    uniform vec3 u_ink;
    uniform vec3 u_accent;
    uniform sampler2D u_atlas;

    float sail(vec2 p, float side, float breathing) {
      float u = (p.x + 0.93) / 1.94;
      float bounds = smoothstep(0.0, 0.035, u) * (1.0 - smoothstep(0.97, 1.015, u));
      float x = clamp(u, 0.0, 1.0);
      float taper = 1.0 - x;
      float wave = sin(x * 3.141593);
      float bend = sin(u_time * 0.44 + x * 2.6) * 0.06 * wave;
      float y = p.y * side + bend;
      float outer = (0.88 + 0.11 * breathing) * pow(taper, 1.18);
      float inner = 0.88 * pow(taper, 3.25) - (0.15 + 0.20 * breathing) * wave;
      float edge = 0.018 + 0.035 * taper;
      float face = smoothstep(inner - edge, inner + edge, y)
                 * (1.0 - smoothstep(outer - edge, outer + edge, y)) * bounds;
      float v = clamp((y - inner) / max(outer - inner, 0.001), 0.0, 1.0);
      float ridge = pow(max(0.0, sin(v * 3.141593)), 0.8);
      float light = 0.43 + 0.46 * ridge + 0.11 * sin(x * 3.8 + u_time * 0.32 + side);
      return face * clamp(light, 0.0, 1.0);
    }
    vec2 pose(vec2 p) {
      // A shallow yaw and asymmetric bend suggest a folded surface without a 3D mesh.
      p.x /= 0.86 + 0.12 * sin(u_time * 0.30 + 0.8);
      p.y -= 0.05 * sin(u_time * 0.36) * p.x;
      p.y /= 0.84 + 0.13 * sin(u_time * 0.60);
      return p;
    }
    float field(vec2 p) {
      float breath = 0.5 + 0.5 * sin(u_time * 0.60);
      float core = max(sail(p, 1.0, breath), sail(p, -1.0, breath));
      float veil = max(sail(p + vec2(0.10, 0.035), 1.0, breath),
                       sail(p + vec2(0.07, -0.03), -1.0, breath)) * 0.19;
      float value = max(core, veil);
      // A curved seam follows the folded face and exits through both wing edges.
      // There is no vertical closing edge or separate triangular cutout.
      float roundedY = max(0.0, sqrt(p.y * p.y + 0.0009) - 0.03);
      float seamX = 0.60 - 1.45 * pow(roundedY, 1.18);
      seamX += 0.025 * sin(u_time * 0.44) * p.y;
      float seamWidth = (0.020 + 0.008 * breath) * (1.0 + 0.55 * abs(p.y));
      float seam = 1.0 - smoothstep(seamWidth, seamWidth + 0.022, abs(p.x - seamX));
      // The outer fold is a lighter veil, not a heavy closed arrowhead.
      float frontFold = smoothstep(seamX + seamWidth + 0.022, seamX + seamWidth + 0.10, p.x);
      value *= mix(1.0, 0.48 + 0.10 * breath, frontFold);
      return value * (1.0 - seam);
    }
    float convergence(vec2 p) {
      // Analytic lanes: scattered packets enter a broad tail and narrow toward the tip.
      // A single local lane is evaluated per cell; no particle loop or extra draw pass.
      float t = (p.x + 1.70) / 2.70;
      float x = clamp(t, 0.0, 1.0);
      float envelope = 1.00 * pow(1.0 - x, 1.32) + 0.024;
      float sway = 0.060 * sin(p.x * 2.0 + u_time * 0.35) * (1.0 - x);
      float row = (p.y - sway) / envelope * 7.0;
      float lane = floor(row + 0.5);
      float seed = fract(sin(lane * 47.13 + 3.7) * 4375.85);
      float center = lane / 7.0 * envelope + sway;
      float width = 0.006 + 0.007 * seed + 0.004 * (1.0 - x);
      float line = 1.0 - smoothstep(width, width + 0.009, abs(p.y - center));
      float phase = fract(t * (7.0 + seed * 3.0) - u_time * (0.38 + seed * 0.14) + seed);
      float packet = smoothstep(0.48, 0.86, phase) * (1.0 - smoothstep(0.86, 0.99, phase));
      // Most fragments fade into the wings; only selected streams reach the output.
      float selected = step(0.72, seed);
      float filtering = mix(1.0 - smoothstep(0.48, 0.75, x), 1.0, selected);
      float bounds = smoothstep(0.0, 0.10, t) * (1.0 - smoothstep(0.96, 1.01, t));
      bounds *= 1.0 - smoothstep(6.5, 7.5, abs(lane));
      return line * (0.06 + 0.94 * packet) * filtering * bounds * (0.44 - 0.12 * x);
    }
    void main() {
      // Integral device-pixel cells avoid beat patterns when a fine grid is sampled.
      float spacing = floor((u_mode > 1.5 ? 7.0 : 4.0) * u_dpr + 0.5);
      vec2 grid = floor(gl_FragCoord.xy / spacing);
      vec2 cell = (grid + 0.5) * spacing;
      vec2 p = (cell - 0.5 * u_resolution) / (0.5 * min(u_resolution.x, u_resolution.y));
      p *= 1.10;
      // Give incoming data space on the left, without changing the approved fold.
      vec2 logo = pose((p - vec2(0.21 * u_trail, 0.0)) / mix(1.0, 0.82, u_trail));
      float strength = max(field(logo), convergence(logo) * u_trail);
      vec2 local = fract(gl_FragCoord.xy / spacing) - 0.5;
      float coverage = 0.0;
      float alpha = 0.0;
      if (u_mode < 0.5) {
        float radius = 0.06 + 0.34 * sqrt(strength);
        coverage = 1.0 - smoothstep(radius - 0.06, radius + 0.035, length(local));
        alpha = coverage * smoothstep(0.015, 0.30, strength) * (0.30 + 0.70 * strength);
      } else if (u_mode < 1.5) {
        vec2 diagonal = vec2(abs(local.x + local.y), abs(local.x - local.y));
        float thickness = 0.03 + 0.12 * strength;
        float arm = 0.12 + 0.29 * sqrt(strength);
        coverage = (1.0 - smoothstep(thickness, thickness + 0.07, min(diagonal.x, diagonal.y)))
                   * (1.0 - smoothstep(arm - 0.04, arm + 0.04, max(abs(local.x), abs(local.y))));
        alpha = coverage * smoothstep(0.015, 0.14, strength) * (0.25 + 0.75 * strength);
      } else {
        float glyph = floor(clamp(strength, 0.0, 0.999) * 8.0);
        vec2 uv = vec2((glyph + local.x + 0.5) / 8.0, 0.5 - local.y);
        coverage = texture2D(u_atlas, uv).a;
        alpha = coverage * smoothstep(0.02, 0.16, strength) * (0.38 + 0.62 * strength);
      }
      alpha *= u_opacity;
      float tip = smoothstep(0.78, 0.96, logo.x) * (1.0 - smoothstep(0.025, 0.12, abs(logo.y)));
      vec3 color = mix(u_ink, u_accent, tip * 0.80);
      gl_FragColor = vec4(color * alpha, alpha);
    }
  `;


export type LoginMotionTexture = 'dots' | 'cross' | 'ascii'
export interface LoginMotionState {
  renderer: 'webgl' | 'static'
  reducedMotion: boolean
  visible: boolean
  texture: LoginMotionTexture
}
export interface LoginMotionOptions { onState?: (state: LoginMotionState) => void }
export interface LoginMotionController { refresh(): void; destroy(): void }

export function mount(canvas: HTMLCanvasElement, options: LoginMotionOptions = {}): LoginMotionController {
  const stage = canvas.parentElement
  if (!stage) return { refresh() {}, destroy() {} }
  let gl: WebGLRenderingContext | null = null
  let program: WebGLProgram | null = null
  let buffer: WebGLBuffer | null = null
  let atlas: WebGLTexture | null = null
  let uniforms: Record<string, WebGLUniformLocation | null> = {}
  let media: MediaQueryList | undefined
  let resizeObserver: ResizeObserver | undefined
  let intersectionObserver: IntersectionObserver | undefined
  let raf = 0, elapsed = 3, cycleElapsed = 0, lastTick = 0, lastDraw = 0
  let visible = true, disposed = false, lost = false, listening = false
  let texture: LoginMotionTexture = 'dots', dpr = 1
  const textures: LoginMotionTexture[] = ['dots', 'cross', 'ascii']
  const attempt = (action: () => void) => { try { action() } catch { /* decoration never blocks the form */ } }
  function report() {
    if (disposed) return
    attempt(() => options.onState?.({
      renderer: program && !lost ? 'webgl' : 'static',
      reducedMotion: media?.matches ?? false,
      visible: visible && !document.hidden,
      texture,
    }))
  }
  function stop() {
    if (raf) attempt(() => cancelAnimationFrame(raf))
    raf = lastTick = lastDraw = 0
  }
  function release() {
    const context = gl
    if (context && !lost) {
      if (buffer) attempt(() => context.deleteBuffer(buffer))
      if (atlas) attempt(() => context.deleteTexture(atlas))
      if (program) attempt(() => context.deleteProgram(program))
    }
    buffer = null; atlas = null; program = null; uniforms = {}
  }
  function fallback() {
    stop(); release(); stage!.dataset.renderer = 'static'; report()
  }
  function compile(context: WebGLRenderingContext, type: number, source: string) {
    const shader = context.createShader(type)
    if (!shader) throw Error('Logo shader unavailable')
    try {
      context.shaderSource(shader, source); context.compileShader(shader)
      if (!context.getShaderParameter(shader, context.COMPILE_STATUS)) throw Error('Logo shader compilation failed')
      return shader
    } catch (error) { attempt(() => context.deleteShader(shader)); throw error }
  }
  function initialize() {
    let vertex: WebGLShader | null = null, fragment: WebGLShader | null = null
    try {
      const context = canvas.getContext('webgl', { alpha: true, antialias: false, premultipliedAlpha: true, powerPreference: 'low-power' })
      gl = context
      if (!context || !context.getShaderPrecisionFormat(context.FRAGMENT_SHADER, context.HIGH_FLOAT)?.precision) throw Error('WebGL unavailable')
      vertex = compile(context, context.VERTEX_SHADER, VERTEX)
      fragment = compile(context, context.FRAGMENT_SHADER, FRAGMENT)
      program = context.createProgram()
      if (!program) throw Error('Logo program unavailable')
      context.attachShader(program, vertex); context.attachShader(program, fragment); context.linkProgram(program)
      if (!context.getProgramParameter(program, context.LINK_STATUS)) throw Error('Logo shader link failed')
      context.useProgram(program)
      buffer = context.createBuffer()
      if (!buffer) throw Error('Logo buffer unavailable')
      context.bindBuffer(context.ARRAY_BUFFER, buffer)
      context.bufferData(context.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, -1, 1, 1, -1, 1, 1]), context.STATIC_DRAW)
      const position = context.getAttribLocation(program, 'a_position')
      if (position < 0) throw Error('Logo position unavailable')
      context.enableVertexAttribArray(position); context.vertexAttribPointer(position, 2, context.FLOAT, false, 0, 0)
      uniforms = Object.fromEntries(['resolution', 'dpr', 'time', 'mode', 'trail', 'opacity', 'ink', 'accent', 'atlas'].map(name => [name, context.getUniformLocation(program!, 'u_' + name)]))
      const glyphCanvas = document.createElement('canvas')
      glyphCanvas.width = 8 * 32; glyphCanvas.height = 32
      const ctx = glyphCanvas.getContext('2d')
      if (!ctx) throw Error('Logo atlas unavailable')
      ctx.fillStyle = '#fff'; ctx.font = '24px monospace'; ctx.textAlign = 'center'; ctx.textBaseline = 'middle'
      const chars = ' .:+*#%@'
      for (let i = 0; i < chars.length; i++) ctx.fillText(chars[i], i * 32 + 16, 16)
      atlas = context.createTexture()
      if (!atlas) throw Error('Logo texture unavailable')
      context.activeTexture(context.TEXTURE0); context.bindTexture(context.TEXTURE_2D, atlas)
      context.texParameteri(context.TEXTURE_2D, context.TEXTURE_MIN_FILTER, context.LINEAR)
      context.texParameteri(context.TEXTURE_2D, context.TEXTURE_MAG_FILTER, context.LINEAR)
      context.texParameteri(context.TEXTURE_2D, context.TEXTURE_WRAP_S, context.CLAMP_TO_EDGE)
      context.texParameteri(context.TEXTURE_2D, context.TEXTURE_WRAP_T, context.CLAMP_TO_EDGE)
      context.texImage2D(context.TEXTURE_2D, 0, context.RGBA, context.RGBA, context.UNSIGNED_BYTE, glyphCanvas)
      context.uniform1i(uniforms.atlas ?? null, 0)
      stage!.dataset.renderer = 'webgl'
    } catch { fallback() }
    finally {
      const context = gl
      if (context && vertex) attempt(() => context.deleteShader(vertex))
      if (context && fragment) attempt(() => context.deleteShader(fragment))
    }
    report()
  }
  function hex(value: string, defaultColor: string) {
    const color = /^#[0-9a-f]{6}$/i.test(value.trim()) ? value.trim() : defaultColor
    return [1, 3, 5].map(i => parseInt(color.slice(i, i + 2), 16) / 255)
  }
  function draw() {
    const context = gl
    if (disposed || lost || !program || !context) return
    try {
      texture = textures[Math.floor(cycleElapsed / 6) % textures.length]!
      const phase = cycleElapsed % 6
      const ease = (value: number) => { const x = Math.max(0, Math.min(1, value)); return x * x * (3 - 2 * x) }
      const opacity = media?.matches ? 1 : ease(phase - 0.15) * ease(5.85 - phase)
      context.useProgram(program); context.viewport(0, 0, canvas.width, canvas.height)
      const style = getComputedStyle(stage!)
      context.uniform2f(uniforms.resolution ?? null, canvas.width, canvas.height)
      context.uniform1f(uniforms.dpr ?? null, dpr); context.uniform1f(uniforms.time ?? null, elapsed)
      context.uniform1f(uniforms.mode ?? null, { dots: 0, cross: 1, ascii: 2 }[texture])
      context.uniform1f(uniforms.trail ?? null, 1); context.uniform1f(uniforms.opacity ?? null, opacity)
      context.uniform3fv(uniforms.ink ?? null, hex(style.getPropertyValue('--art-ink'), '#eeeeea'))
      context.uniform3fv(uniforms.accent ?? null, hex(style.getPropertyValue('--art-accent'), '#c9a15a'))
      context.drawArrays(context.TRIANGLES, 0, 6)
      canvas.dataset.frame = String(Number(canvas.dataset.frame || 0) + 1)
      canvas.dataset.phase = elapsed.toFixed(3); canvas.dataset.texture = texture; canvas.dataset.opacity = opacity.toFixed(3)
    } catch { fallback() }
  }
  function resize() {
    if (disposed) return
    try {
      const size = Math.max(1, Math.round(canvas.getBoundingClientRect().width))
      dpr = Math.min(window.devicePixelRatio || 1, 1.5, 1200 / size)
      const pixels = Math.max(1, Math.round(size * dpr))
      if (canvas.width !== pixels || canvas.height !== pixels) canvas.width = canvas.height = pixels
      draw()
    } catch { fallback() }
  }
  const running = () => !disposed && !lost && !!program && !media?.matches && visible && !document.hidden
  function frame(now: number) {
    raf = 0
    if (!running()) { if (!disposed) { if (media?.matches) draw(); report() }; return }
    if (lastTick) {
      const delta = Math.min((now - lastTick) / 1000, 0.1)
      elapsed += delta; cycleElapsed += delta
    }
    lastTick = now
    if (!lastDraw || now - lastDraw >= 1000 / 30) { draw(); lastDraw = now }
    if (running()) {
      try { raf = requestAnimationFrame(frame) } catch { fallback() }
    }
  }
  function synchronize() {
    if (disposed) return
    stop()
    if (running()) { try { raf = requestAnimationFrame(frame) } catch { fallback() } }
    report()
  }
  function reducedChange() { if (!disposed) { draw(); synchronize() } }
  function contextLost(event: Event) {
    if (disposed) return
    event.preventDefault(); lost = true; stage!.dataset.renderer = 'static'; synchronize()
  }
  function contextRestored() {
    if (disposed) return
    lost = false; buffer = null; atlas = null; program = null
    initialize(); resize(); synchronize()
  }
  function disconnect() {
    attempt(() => resizeObserver?.disconnect()); attempt(() => intersectionObserver?.disconnect())
    if (listening) {
      attempt(() => media?.removeEventListener('change', reducedChange))
      attempt(() => document.removeEventListener('visibilitychange', synchronize))
      attempt(() => canvas.removeEventListener('webglcontextlost', contextLost))
      attempt(() => canvas.removeEventListener('webglcontextrestored', contextRestored))
      listening = false
    }
  }
  try {
    stage.dataset.renderer = 'static'
    if (typeof ResizeObserver === 'undefined' || typeof IntersectionObserver === 'undefined'
      || typeof requestAnimationFrame !== 'function' || typeof cancelAnimationFrame !== 'function') throw Error('Motion APIs unavailable')
    media = window.matchMedia('(prefers-reduced-motion: reduce)')
    initialize(); resize()
    resizeObserver = new ResizeObserver(resize); resizeObserver.observe(canvas)
    intersectionObserver = new IntersectionObserver(entries => {
      if (disposed) return
      visible = entries[0]?.isIntersecting ?? false; synchronize()
    }, { threshold: 0.01 }); intersectionObserver.observe(canvas)
    // Mark before adding listeners so a partially failed setup is still cleaned up.
    listening = true
    media.addEventListener('change', reducedChange); document.addEventListener('visibilitychange', synchronize)
    canvas.addEventListener('webglcontextlost', contextLost); canvas.addEventListener('webglcontextrestored', contextRestored)
    synchronize()
  } catch { disconnect(); fallback() }
  return {
    refresh: draw,
    destroy() {
      if (disposed) return
      disposed = true; stop(); disconnect(); release()
    },
  }
}
