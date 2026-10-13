/* Original procedural sail geometry, inspired by the visual reference, not its source.
 * One WebGL1 fragment pass: continuous shape/lighting field -> fixed glyph grid.
 * Native browser APIs only. Mount returns an explicit destroy hook for later Vue use.
 */
(() => {
  'use strict';
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
      float tip = smoothstep(0.78, 0.96, logo.x) * (1.0 - smoothstep(0.025, 0.12, abs(logo.y)));
      vec3 color = mix(u_ink, u_accent, tip * 0.80);
      gl_FragColor = vec4(color * alpha, alpha);
    }
  `;

  function mount(canvas, options = {}) {
    const stage = canvas.parentElement;
    const media = window.matchMedia('(prefers-reduced-motion: reduce)');
    let gl = null;
    let program = null;
    let buffer = null;
    let atlas = null;
    let uniforms = {};
    let raf = 0;
    let elapsed = 3.0;
    let lastTick = 0;
    let lastDraw = 0;
    let manualPaused = false;
    let visible = true;
    let disposed = false;
    let lost = false;
    let texture = 'cross';
    let trailEnabled = true;
    let dpr = 1;

    const report = () => options.onState?.({
      renderer: program && !lost ? 'webgl' : 'static',
      paused: manualPaused || media.matches,
      reducedMotion: media.matches,
      visible: visible && !document.hidden,
      texture,
      trailEnabled,
    });
    function hex(value) {
      const s = value.trim().replace('#', '');
      return [0, 2, 4].map(i => parseInt(s.slice(i, i + 2), 16) / 255);
    }
    function compile(type, source) {
      const shader = gl.createShader(type);
      gl.shaderSource(shader, source);
      gl.compileShader(shader);
      if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
        gl.deleteShader(shader);
        throw new Error('Logo shader compilation failed');
      }
      return shader;
    }
    function release() {
      if (!gl || lost) return;
      if (buffer) gl.deleteBuffer(buffer);
      if (atlas) gl.deleteTexture(atlas);
      if (program) gl.deleteProgram(program);
      buffer = atlas = program = null;
    }
    function initialize() {
      let vertex = null;
      let fragment = null;
      try {
        gl = canvas.getContext('webgl', { alpha: true, antialias: false, premultipliedAlpha: true, powerPreference: 'low-power' });
        if (!gl) throw new Error('WebGL unavailable');
        vertex = compile(gl.VERTEX_SHADER, VERTEX);
        fragment = compile(gl.FRAGMENT_SHADER, FRAGMENT);
        program = gl.createProgram();
        gl.attachShader(program, vertex);
        gl.attachShader(program, fragment);
        gl.linkProgram(program);
        if (!gl.getProgramParameter(program, gl.LINK_STATUS)) throw new Error('Logo shader link failed');
        gl.useProgram(program);
        buffer = gl.createBuffer();
        gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
        gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, -1, 1, 1, -1, 1, 1]), gl.STATIC_DRAW);
        const position = gl.getAttribLocation(program, 'a_position');
        gl.enableVertexAttribArray(position);
        gl.vertexAttribPointer(position, 2, gl.FLOAT, false, 0, 0);
        uniforms = Object.fromEntries(['resolution', 'dpr', 'time', 'mode', 'trail', 'ink', 'accent', 'atlas'].map(name => [name, gl.getUniformLocation(program, `u_${name}`)]));
        const chars = ' .:+*#%@';
        const glyphCanvas = document.createElement('canvas');
        glyphCanvas.width = 8 * 32;
        glyphCanvas.height = 32;
        const ctx = glyphCanvas.getContext('2d');
        ctx.fillStyle = '#fff';
        ctx.font = '24px monospace';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        for (let i = 0; i < chars.length; i++) ctx.fillText(chars[i], i * 32 + 16, 16);
        atlas = gl.createTexture();
        gl.activeTexture(gl.TEXTURE0);
        gl.bindTexture(gl.TEXTURE_2D, atlas);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
        gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, glyphCanvas);
        gl.uniform1i(uniforms.atlas, 0);
        stage.dataset.renderer = 'webgl';
      } catch {
        release();
        program = null;
        stage.dataset.renderer = 'static';
      } finally {
        if (vertex) gl.deleteShader(vertex);
        if (fragment) gl.deleteShader(fragment);
      }
      report();
    }
    function draw() {
      if (disposed || lost || !program) return;
      gl.useProgram(program);
      gl.viewport(0, 0, canvas.width, canvas.height);
      const style = getComputedStyle(stage);
      gl.uniform2f(uniforms.resolution, canvas.width, canvas.height);
      gl.uniform1f(uniforms.dpr, dpr);
      gl.uniform1f(uniforms.time, elapsed);
      gl.uniform1f(uniforms.mode, { dots: 0, cross: 1, ascii: 2 }[texture]);
      gl.uniform1f(uniforms.trail, trailEnabled ? 1 : 0);
      gl.uniform3fv(uniforms.ink, hex(style.getPropertyValue('--art-ink')));
      gl.uniform3fv(uniforms.accent, hex(style.getPropertyValue('--art-accent')));
      gl.drawArrays(gl.TRIANGLES, 0, 6);
      canvas.dataset.frame = String(Number(canvas.dataset.frame || 0) + 1);
      canvas.dataset.phase = elapsed.toFixed(3);
    }
    function resize() {
      if (disposed) return;
      const size = Math.max(1, Math.round(canvas.getBoundingClientRect().width));
      dpr = Math.min(window.devicePixelRatio || 1, 1.5, 1200 / size);
      const pixels = Math.max(1, Math.round(size * dpr));
      if (canvas.width !== pixels || canvas.height !== pixels) canvas.width = canvas.height = pixels;
      draw();
    }
    const running = () => !disposed && !lost && !!program && !manualPaused && !media.matches && visible && !document.hidden;
    function frame(now) {
      raf = 0;
      if (!running()) { if (!disposed) report(); return; }
      if (lastTick) elapsed += Math.min((now - lastTick) / 1000, 0.1);
      lastTick = now;
      // Cap expensive draws at 30fps. Time is continuous across skipped display frames.
      if (!lastDraw || now - lastDraw >= 1000 / 30) { draw(); lastDraw = now; }
      raf = requestAnimationFrame(frame);
    }
    function synchronize() {
      if (raf) cancelAnimationFrame(raf);
      raf = lastTick = lastDraw = 0;
      if (running()) raf = requestAnimationFrame(frame);
      report();
    }
    function visibilityChange() { synchronize(); }
    function reducedChange() { draw(); synchronize(); }
    function contextLost(event) {
      event.preventDefault();
      lost = true;
      stage.dataset.renderer = 'static';
      synchronize();
    }
    function contextRestored() {
      lost = false;
      buffer = atlas = program = null;
      initialize();
      resize();
      synchronize();
    }
    initialize();
    resize();
    const resizeObserver = new ResizeObserver(resize);
    resizeObserver.observe(canvas);
    const intersectionObserver = new IntersectionObserver(entries => {
      visible = entries[0]?.isIntersecting ?? false;
      synchronize();
    }, { threshold: 0.01 });
    intersectionObserver.observe(canvas);
    media.addEventListener('change', reducedChange);
    document.addEventListener('visibilitychange', visibilityChange);
    canvas.addEventListener('webglcontextlost', contextLost);
    canvas.addEventListener('webglcontextrestored', contextRestored);
    synchronize();
    return {
      setTexture(next) { if (!['dots', 'cross', 'ascii'].includes(next)) return; texture = next; draw(); report(); },
      setPaused(paused) { manualPaused = !!paused; synchronize(); if (manualPaused) draw(); },
      setTrailEnabled(enabled) { trailEnabled = !!enabled; draw(); report(); },
      refresh: draw,
      destroy() {
        if (disposed) return;
        disposed = true;
        if (raf) cancelAnimationFrame(raf);
        resizeObserver.disconnect();
        intersectionObserver.disconnect();
        media.removeEventListener('change', reducedChange);
        document.removeEventListener('visibilitychange', visibilityChange);
        canvas.removeEventListener('webglcontextlost', contextLost);
        canvas.removeEventListener('webglcontextrestored', contextRestored);
        release();
      },
    };
  }
  window.RagLogoMotion = Object.freeze({ mount });
})();
