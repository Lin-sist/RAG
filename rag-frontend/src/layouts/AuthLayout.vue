<template>
  <div class="auth-layout" :class="{ 'auth-layout--dark': isDark }">
    <header class="auth-topbar">
      <div class="auth-wordmark" aria-label="RAG 智能问答">
        <svg viewBox="125 120 355 275" width="26" height="20" aria-hidden="true">
          <path d="M141 133 C252 170 372 222 465 268 C372 236 250 184 141 133 Z" fill="currentColor" />
          <path d="M226 380 C310 372 396 326 465 268 C398 340 302 388 226 380 Z" fill="currentColor" />
          <path d="M412 238 Q447 252 465 268 Q447 284 412 298 Q427 283 427 268 Q427 253 412 238 Z" fill="#d97757" />
        </svg>
        <span>RAG<span class="wordmark-accent">\</span>智能问答</span>
      </div>
      <div class="auth-topbar-actions">
        <span class="auth-topbar-note">知识库问答</span>
        <button class="auth-theme-toggle" type="button" @click="toggleTheme">
          {{ isDark ? '米白版 →' : '暗黑版 →' }}
        </button>
      </div>
    </header>

    <main class="auth-hero">
      <section class="auth-intro">
        <h1>让每个回答<br />都有出处</h1>
        <p class="auth-subtitle">基于知识库的检索与问答</p>
        <router-view />
      </section>
      <LoginPixelArt class="auth-art" />
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import LoginPixelArt from '@/components/common/LoginPixelArt.vue'

const isDark = ref(false)

function applyTheme(dark: boolean) {
  isDark.value = dark
  document.documentElement.classList.toggle('dark', dark)
  document.documentElement.classList.toggle('light', !dark)
}

function toggleTheme() {
  applyTheme(!isDark.value)
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}

onMounted(() => {
  const savedTheme = localStorage.getItem('theme')
  applyTheme(savedTheme === 'dark' || (savedTheme !== 'light' && window.matchMedia('(prefers-color-scheme: dark)').matches))
})
</script>

<style scoped>
.auth-layout {
  --auth-ink: #1a1915;
  --auth-ink-soft: #3e3b33;
  --auth-muted: #75705f;
  --auth-paper: #faf8f2;
  --auth-line: #d9d3c3;
  --auth-placeholder: #a49e8d;
  --auth-focus: #1a1915;
  --auth-focus-ring: rgba(217, 119, 87, .16);
  --auth-button: #1a1915;
  --auth-button-ink: #f6f3ea;
  --auth-button-hover: #b4552d;
  --auth-error: #b23a16;
  --auth-pixel-0: #d97757;
  --auth-pixel-1: #c15f3c;
  --auth-pixel-2: #b4552d;
  --auth-pixel-3: #e5a184;
  --auth-pixel-4: #edcdb9;
  min-height: 100vh;
  color: var(--auth-ink);
  background-color: #f2efe7;
  background-image: radial-gradient(rgba(26, 25, 21, .13) 1.1px, transparent 1.5px);
  background-size: 32px 32px;
  font-family: -apple-system, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
.auth-layout--dark {
  --auth-ink: #f5f5f5;
  --auth-ink-soft: #d9d9d9;
  --auth-muted: #8f8f8f;
  --auth-paper: #121212;
  --auth-line: #3a3a3a;
  --auth-placeholder: #6b6b6b;
  --auth-focus: #e3e3e3;
  --auth-focus-ring: rgba(255, 255, 255, .09);
  --auth-button: #fff;
  --auth-button-ink: #0a0a0a;
  --auth-button-hover: #e2e2e2;
  --auth-error: #e5484d;
  --auth-pixel-0: #f5f5f5;
  --auth-pixel-1: #d9d9d9;
  --auth-pixel-2: #b8b8b8;
  --auth-pixel-3: #8f8f8f;
  --auth-pixel-4: #666;
  color-scheme: dark;
  background-color: #0a0a0a;
  background-image: radial-gradient(rgba(255, 255, 255, .09) 1.1px, transparent 1.5px);
}
.auth-topbar {
  height: 68px;
  max-width: 1368px;
  margin: 0 auto;
  padding: 0 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.auth-wordmark { display: flex; align-items: center; gap: 6px; font-size: 17px; font-weight: 700; letter-spacing: .02em; }
.auth-wordmark svg { flex: none; }
.wordmark-accent { color: #d97757; padding: 0 2px; }
.auth-layout--dark .wordmark-accent { color: #cfcfcf; }
.auth-layout--dark .auth-wordmark svg path:last-child { fill: #c9a15a; }
.auth-topbar-actions { display: flex; align-items: center; gap: 22px; }
.auth-topbar-note { color: var(--auth-muted); font-size: 13px; }
.auth-theme-toggle {
  padding: 7px 0;
  border: 0;
  background: transparent;
  color: var(--auth-muted);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
  transition: color 150ms;
}
.auth-theme-toggle:hover, .auth-theme-toggle:focus-visible { color: var(--auth-ink); }
.auth-theme-toggle:focus-visible { outline: 2px solid var(--auth-focus); outline-offset: 4px; border-radius: 2px; }
.auth-hero {
  max-width: 1280px;
  min-height: calc(100vh - 68px);
  margin: 0 auto;
  padding: 40px 44px 64px;
  display: grid;
  grid-template-columns: 1.04fr .96fr;
  gap: 56px;
  align-items: center;
}
.auth-intro h1 {
  margin: 0 0 20px;
  font-family: Georgia, 'Times New Roman', 'Songti SC', 'STSong', 'SimSun', serif;
  font-size: clamp(46px, 5.4vw, 82px);
  line-height: 1.1;
  font-weight: 600;
  letter-spacing: .015em;
}
.auth-subtitle { color: var(--auth-muted); font-size: 15.5px; letter-spacing: .02em; }
.auth-art { width: min(560px, 100%); justify-self: center; }
@media (max-width: 980px) {
  .auth-topbar { padding: 0 24px; }
  .auth-hero { grid-template-columns: 1fr; gap: 24px; padding: 32px 24px 56px; min-height: 0; }
  .auth-art { width: min(300px, 72vw); }
}
@media (max-width: 520px) {
  .auth-topbar { height: 58px; padding: 0 20px; }
  .auth-topbar-note { display: none; }
  .auth-hero { padding: 42px 20px 48px; }
  .auth-intro h1 { font-size: clamp(42px, 11vw, 56px); }
  .auth-art { margin-top: 8px; width: min(230px, 60vw); }
}
</style>
