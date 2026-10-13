(() => {
  'use strict';
  const root = document.documentElement;
  const themeButton = document.getElementById('themeToggle');
  const motionButton = document.getElementById('motionToggle');
  const motionStatus = document.getElementById('motionStatus');
  const textureButtons = [...document.querySelectorAll('[data-texture]')];
  let paused = false;
  const motion = window.RagLogoMotion.mount(document.getElementById('logoCanvas'), {
    onState(state) {
      const fallback = state.renderer !== 'webgl';
      motionButton.disabled = fallback || state.reducedMotion;
      textureButtons.forEach(button => { button.disabled = fallback; });
      motionButton.textContent = fallback ? '静态展示' : state.reducedMotion ? '静态展示' : state.paused ? '继续动效' : '暂停动效';
      motionButton.setAttribute('aria-pressed', String(state.paused));
      motionStatus.textContent = fallback ? '当前浏览器使用静态 logo。' : state.reducedMotion ? '已遵循系统的降低动态效果设置。' : '';
    },
  });
  // Keep prototype preferences in memory. No localStorage or credential persistence.
  themeButton.addEventListener('click', () => {
    const light = root.dataset.theme !== 'light';
    root.dataset.theme = light ? 'light' : 'dark';
    themeButton.textContent = light ? '黑白版 →' : '米白版 →';
    motion.refresh();
  });
  textureButtons.forEach(button => button.addEventListener('click', () => {
    textureButtons.forEach(item => item.setAttribute('aria-pressed', String(item === button)));
    motion.setTexture(button.dataset.texture);
  }));
  motionButton.addEventListener('click', () => { paused = !paused; motion.setPaused(paused); });

  const form = document.getElementById('loginForm');
  const username = document.getElementById('username');
  const password = document.getElementById('password');
  const passwordToggle = document.getElementById('passwordToggle');
  const loginButton = document.getElementById('loginButton');
  const status = document.getElementById('formStatus');
  let submitting = false;
  let timer = 0;
  const fields = [username, password];
  const idle = '独立设计演示，不连接真实账号。请使用演示信息。';
  function error(input, invalid) {
    input.setAttribute('aria-invalid', String(invalid));
    document.getElementById(`${input.id}Error`).hidden = !invalid;
  }
  fields.forEach(input => input.addEventListener('input', () => {
    error(input, false);
    if (!submitting) status.textContent = idle;
  }));
  passwordToggle.addEventListener('click', () => {
    const show = password.type === 'password';
    password.type = show ? 'text' : 'password';
    passwordToggle.textContent = show ? '隐藏' : '显示';
    passwordToggle.setAttribute('aria-label', show ? '隐藏密码' : '显示密码');
    passwordToggle.setAttribute('aria-pressed', String(show));
  });
  form.addEventListener('submit', event => {
    event.preventDefault();
    if (submitting) return;
    const invalid = fields.filter(input => input.id === 'username' ? !input.value.trim() : !input.value);
    fields.forEach(input => error(input, invalid.includes(input)));
    if (invalid.length) {
      status.textContent = '请补全演示信息。';
      invalid[0].focus();
      return;
    }
    submitting = true;
    loginButton.disabled = true;
    loginButton.textContent = '演示中…';
    status.textContent = '正在演示表单状态，不发送登录请求。';
    timer = window.setTimeout(() => {
      // Nothing authenticates: explicitly show local validation rather than a fake login success.
      fields.forEach(input => { input.value = ''; input.readOnly = false; });
      password.type = 'password';
      passwordToggle.textContent = '显示';
      passwordToggle.setAttribute('aria-label', '显示密码');
      passwordToggle.setAttribute('aria-pressed', 'false');
      submitting = false;
      loginButton.disabled = false;
      loginButton.textContent = '登 录';
      status.textContent = '演示校验完成。未进行身份认证，输入信息已清空。';
    }, 700);
    fields.forEach(input => { input.readOnly = true; });
  });
  function clearInputs() {
    fields.forEach(input => { input.value = ''; input.readOnly = false; });
    password.type = 'password';
    passwordToggle.textContent = '显示';
    passwordToggle.setAttribute('aria-label', '显示密码');
    passwordToggle.setAttribute('aria-pressed', 'false');
  }
  // pagehide can enter the back-forward cache; remount the renderer on return.
  window.addEventListener('pagehide', () => {
    window.clearTimeout(timer);
    clearInputs();
    motion.destroy();
  });
  window.addEventListener('pageshow', event => {
    clearInputs();
    if (event.persisted) window.location.reload();
  });
  // Without scripts the form stays disabled, so native submission cannot expose inputs.
  fields.forEach(input => { input.disabled = false; });
  passwordToggle.disabled = false;
  loginButton.disabled = false;
})();
