<template>
  <div class="auth-layout">
    <header class="auth-topbar">
      <div class="auth-wordmark" aria-label="RAG 智能问答">
        <svg viewBox="125 120 355 275" width="26" height="20" aria-hidden="true">
          <path d="M141 133 C252 170 372 222 465 268 C372 236 250 184 141 133 Z" fill="currentColor" />
          <path d="M226 380 C310 372 396 326 465 268 C398 340 302 388 226 380 Z" fill="currentColor" />
          <path d="M412 238 Q447 252 465 268 Q447 284 412 298 Q427 283 427 268 Q427 253 412 238 Z" fill="#d97757" />
        </svg>
        <span>RAG<span class="wordmark-accent">\</span>智能问答</span>
      </div>
      <span class="auth-topbar-note">知识库问答</span>
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
import LoginPixelArt from '@/components/common/LoginPixelArt.vue'
</script>

<style scoped>
.auth-layout {
  --auth-ink: #1a1915;
  --auth-muted: #75705f;
  min-height: 100vh;
  color: var(--auth-ink);
  background-color: #f2efe7;
  background-image: radial-gradient(rgba(26, 25, 21, .13) 1.1px, transparent 1.5px);
  background-size: 32px 32px;
  font-family: -apple-system, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
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
.auth-topbar-note { color: var(--auth-muted); font-size: 13px; }
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
