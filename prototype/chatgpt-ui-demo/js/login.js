/* =========================================================
   登录页设计演示脚本
   1) 登录表单：纯前端演示，不发起任何网络请求
   2) 像素无限循环动画：原生 canvas，复刻 Anthropic RSI
      页"方块像素在多个图形间无限变形"的 hero 视觉
   零依赖 · 离线可用
   ========================================================= */
(() => {
  'use strict';

  /* ---------------- 1. 登录表单（演示） ---------------- */

  const form = document.getElementById('loginForm');
  const userInput = document.getElementById('username');
  const pwdInput = document.getElementById('password');
  const userErr = document.getElementById('usernameErr');
  const pwdErr = document.getElementById('passwordErr');
  const fieldUser = document.getElementById('fieldUsername');
  const fieldPwd = document.getElementById('fieldPassword');
  const loginBtn = document.getElementById('loginBtn');
  const formHint = document.getElementById('formHint');
  const pwdToggle = document.getElementById('pwdToggle');

  function setErr(field, errEl, input, show) {
    field.classList.toggle('is-error', show);
    errEl.hidden = !show;
    input.setAttribute('aria-invalid', show ? 'true' : 'false');
  }

  // 输入即清除错误态
  userInput.addEventListener('input', () => setErr(fieldUser, userErr, userInput, false));
  pwdInput.addEventListener('input', () => setErr(fieldPwd, pwdErr, pwdInput, false));

  pwdToggle.addEventListener('click', () => {
    const show = pwdInput.type === 'password';
    pwdInput.type = show ? 'text' : 'password';
    pwdToggle.textContent = show ? '隐藏' : '显示';
    pwdInput.focus();
  });

  form.addEventListener('submit', (e) => {
    e.preventDefault();
    const badUser = userInput.value.trim() === '';
    const badPwd = pwdInput.value === '';
    setErr(fieldUser, userErr, userInput, badUser);
    setErr(fieldPwd, pwdErr, pwdInput, badPwd);
    if (badUser || badPwd) return;

    // 演示态：模拟校验耗时，不触碰任何后端
    loginBtn.disabled = true;
    loginBtn.textContent = '登录中…';
    setTimeout(() => {
      loginBtn.classList.add('is-done');
      loginBtn.textContent = '✓ 已进入演示';
      formHint.textContent = '设计原型演示成功：真实实现请接入 rag-auth 登录接口';
      setTimeout(() => {
        loginBtn.classList.remove('is-done');
        loginBtn.disabled = false;
        loginBtn.textContent = '登\u00a0\u00a0录';
        formHint.textContent = '设计原型 · 不发起任何网络请求 · 全部为演示数据';
      }, 2600);
    }, 900);
  });

  /* ---------------- 2. 像素无限循环动画 ---------------- */

  const GRID = 16;              // 16×16 像素格
  const HOLD_MS = 1700;         // 每个图形停留时长
  const MORPH_MS = 460;         // 单格变形时长
  const STAGGER_MS = 38;        // 由中心向外的错峰

  const canvas = document.getElementById('pixelCanvas');
  const ctx = canvas.getContext('2d');
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  // 调色板与格形由 canvas 的 data-palette / data-shape 选择：
  // clay = 陶土方块（login.html 米白版）；mono = 白灰圆点（login-dark.html 黑白版）
  const PALETTES = {
    clay: ['#d97757', '#d97757', '#c15f3c', '#b4552d', '#e5a184', '#edcdb9'],
    mono: ['#ffffff', '#ffffff', '#e8e8e8', '#c9c9c9', '#9a9a9a', '#5c5c5c'],
  };
  const SHAPE = canvas.dataset.shape === 'dot' ? 'dot' : 'square';
  const PALETTE = PALETTES[canvas.dataset.palette] || PALETTES.clay;

  function mk(fn) {
    const set = new Set();
    for (let y = 0; y < GRID; y++) for (let x = 0; x < GRID; x++) if (fn(x, y)) set.add(y * GRID + x);
    return set;
  }

  // 六个图形：RAG logo → 星芒 → 文档 → 放大镜 → 对话 → 无限循环，依次变形、无限往复
  // logo 格点版：收敛方块（大→中→小）+ 双刃箭头，来自定稿 mark（prototype/logo-designer/logos/export/）
  const logoCells = [
    [1, 4], [2, 4], [1, 5], [2, 5],          // 大方块（上/下）
    [1, 10], [2, 10], [1, 11], [2, 11],
    [4, 6], [4, 9],                           // 中方块
    [6, 7], [6, 8],                           // 小方块（收束列）
    [7, 2], [8, 4], [9, 5], [10, 6], [11, 6], [12, 7], [13, 7],   // 上刃
    [7, 13], [8, 11], [9, 10], [10, 9], [11, 9], [12, 8], [13, 8], // 下刃
  ];
  const logoSet = new Set(logoCells.map(([x, y]) => y * GRID + x));
  const GLYPHS = [
    logoSet,
    // 星芒（8 向辐射，致敬参考页的星形像素）
    mk((x, y) => {
      const u = x + 0.5 - 8, v = y + 0.5 - 8;
      const axis = (Math.abs(u) < 0.75 && Math.abs(v) < 7.6) || (Math.abs(v) < 0.75 && Math.abs(u) < 7.6);
      const diag = Math.abs(Math.abs(u) - Math.abs(v)) < 0.75 && Math.max(Math.abs(u), Math.abs(v)) < 6.4;
      return axis || diag;
    }),
    // 文档（右上折角 + 内容行）
    mk((x, y) => {
      if (x < 4 || x > 11 || y < 1 || y > 14) return false;
      if ((11 - x) + (y - 1) < 3) return false;
      if ((y === 5 || y === 8 || y === 11) && x >= 6 && x <= 10) return false;
      return true;
    }),
    // 放大镜（圆环 + 粗柄）
    mk((x, y) => {
      const d = Math.hypot(x + 0.5 - 7.2, y + 0.5 - 7.2);
      const ring = d > 3.1 && d < 4.9;
      const handle = x >= 10 && x <= 14 && y >= 10 && y <= 14 && Math.abs(x - y) <= 1;
      return ring || handle;
    }),
    // 对话气泡（圆角矩形 + 左下尾巴 + 内容行）
    mk((x, y) => {
      if (x >= 3 && x <= 12 && y >= 2 && y <= 9) {
        return !((y === 4 || y === 6) && x >= 5 && x <= 10);
      }
      if (y === 10 && x >= 4 && x <= 6) return true;
      if (y === 11 && (x === 4 || x === 5)) return true;
      return false;
    }),
    // 无限循环 ∞（双环）
    mk((x, y) => {
      const d1 = Math.hypot(x + 0.5 - 5, y + 0.5 - 8);
      const d2 = Math.hypot(x + 0.5 - 11, y + 0.5 - 8);
      const band = d => d > 2.0 && d < 3.9;
      return band(d1) || band(d2);
    }),
  ];

  const N = GRID * GRID;
  const scale = new Float32Array(N);   // 每格当前显示比例 0..1
  const targetOn = new Uint8Array(N);  // 每格目标亮灭
  const colorIdx = new Uint8Array(N);  // 每格颜色
  const anims = [];                    // 进行中的变形 {i, from, to, t0, dur}
  let frameNo = 0;
  let glyphIdx = 0;
  let lastMorphAt = 0;

  const rnd = () => Math.random();
  const palettePick = () => {
    const r = rnd();
    // 中间调权重更高
    if (r < 0.34) return 0; if (r < 0.52) return 2; if (r < 0.68) return 3;
    if (r < 0.86) return 4; return 5;
  };
  const easeInOut = t => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);

  // 初始化：点亮第一个图形
  for (const i of GLYPHS[0]) {
    targetOn[i] = 1;
    scale[i] = 1;
    colorIdx[i] = palettePick();
  }

  function morphTo(glyph) {
    const now = performance.now();
    for (let i = 0; i < N; i++) {
      const on = glyph.has(i) ? 1 : 0;
      if (on === targetOn[i]) {
        if (rnd() < 0.14) colorIdx[i] = palettePick(); // 静止格偶发换色
        continue;
      }
      targetOn[i] = on;
      if (rnd() < 0.55) colorIdx[i] = palettePick();
      const x = i % GRID, y = (i / GRID) | 0;
      const dist = Math.hypot(x + 0.5 - 8, y + 0.5 - 8);
      anims.push({
        i, from: scale[i], to: on,
        t0: now + dist * STAGGER_MS + rnd() * 90,
        dur: MORPH_MS + rnd() * 120,
      });
    }
    frameNo++;
  }

  function draw(now) {
    // 画布尺寸（含 devicePixelRatio）
    const cssSize = canvas.clientWidth || 560;
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    if (canvas.width !== Math.round(cssSize * dpr)) {
      canvas.width = Math.round(cssSize * dpr);
      canvas.height = Math.round(cssSize * dpr);
    }
    const size = canvas.width;
    ctx.clearRect(0, 0, size, size);
    const cell = size / GRID;

    // 推进变形
    for (let a = anims.length - 1; a >= 0; a--) {
      const an = anims[a];
      if (now <= an.t0) continue;
      const p = Math.min(1, (now - an.t0) / an.dur);
      scale[an.i] = an.from + (an.to - an.from) * easeInOut(p);
      if (p >= 1) anims.splice(a, 1);
    }

    // 外圈光点：目标图形边缘外的散点（复刻参考页图案外围的碎点花）
    for (let i = 0; i < N; i++) {
      if (targetOn[i]) continue;
      const x = i % GRID, y = (i / GRID) | 0;
      let near = false;
      for (let dy = -1; dy <= 1 && !near; dy++) {
        for (let dx = -1; dx <= 1 && !near; dx++) {
          const nx = x + dx, ny = y + dy;
          if (nx >= 0 && nx < GRID && ny >= 0 && ny < GRID && targetOn[ny * GRID + nx]) near = true;
        }
      }
      if (!near) continue;
      const h = (x * 73856093 ^ y * 19349663 ^ frameNo * 83492791) >>> 0;
      if (h % 100 >= 15) continue;
      const px = (x + 0.5) * cell, py = (y + 0.5) * cell;
      ctx.globalAlpha = 0.28 + (h % 30) / 100;
      ctx.fillStyle = PALETTE[h % 4];
      ctx.beginPath();
      ctx.arc(px, py, cell * 0.09, 0, Math.PI * 2);
      ctx.fill();
    }

    // 亮格：圆角方块，轻微呼吸感
    for (let i = 0; i < N; i++) {
      const s = scale[i];
      if (s <= 0.01) continue;
      const x = i % GRID, y = (i / GRID) | 0;
      const wob = anims.length
        ? 1
        : 1 + 0.035 * Math.sin(now / 650 + (x + y) * 0.8); // 静止期呼吸
      const w = cell * 0.86 * s * wob;
      const px = (x + 0.5) * cell - w / 2, py = (y + 0.5) * cell - w / 2;
      ctx.globalAlpha = Math.min(1, 0.4 + s * 0.6);
      ctx.fillStyle = PALETTE[colorIdx[i]];
      const r = Math.max(1, w * 0.26);
      ctx.beginPath();
      if (SHAPE === 'dot') ctx.arc(px + w / 2, py + w / 2, Math.max(0.5, w / 2 * 0.92), 0, Math.PI * 2);
      else if (ctx.roundRect) ctx.roundRect(px, py, w, w, r);
      else ctx.rect(px, py, w, w);
      ctx.fill();
    }
    ctx.globalAlpha = 1;
  }

  function loop(now) {
    if (!lastMorphAt) lastMorphAt = now;
    if (now - lastMorphAt >= HOLD_MS) {
      glyphIdx = (glyphIdx + 1) % GLYPHS.length;
      morphTo(GLYPHS[glyphIdx]);
      lastMorphAt = now;
    }
    draw(now);
    requestAnimationFrame(loop);
  }

  if (reduceMotion) {
    // 降低动效偏好：静止展示文档图形，不进入循环
    draw(performance.now());
  } else {
    requestAnimationFrame(loop);
  }
})();
