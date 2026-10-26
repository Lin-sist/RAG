<template>
  <section class="particle-scene" :data-renderer="state.renderer" aria-label="从知识到回答">
    <div class="particle-stage" aria-hidden="true">
      <canvas ref="canvas" />
      <img v-if="state.renderer === 'static'" class="particle-fallback" :src="staticLogo" alt="" />
    </div>
    <div class="particle-caption">
      <span class="particle-eyebrow">从知识到回答</span>
      <span class="particle-count">0{{ state.scene + 1 }} <span>/ 03</span></span>
    </div>
    <div class="particle-controls" role="group" aria-label="选择粒子图样">
      <button v-for="(label, index) in PARTICLE_SCENES" :key="label" type="button"
        :aria-pressed="state.scene === index" :disabled="state.renderer !== 'webgl'" @click="controller?.select(index)">
        {{ label }}
      </button>
      <button class="particle-pause" type="button" :disabled="state.renderer !== 'webgl' || state.reduced"
        :aria-label="state.reduced ? '系统已减少动态效果' : state.paused ? '继续粒子动画' : '暂停粒子动画'"
        :aria-pressed="state.paused" @click="controller?.pause()">
        <svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true">
          <path v-if="state.paused" d="M5 3 L12 8 L5 13 Z" fill="currentColor" />
          <path v-else d="M5 3 V13 M11 3 V13" fill="none" stroke="currentColor" stroke-width="1.5" />
        </svg>
      </button>
    </div>
    <p class="particle-hint">{{ state.renderer === 'static' ? '让知识彼此关联，让回答有据可循' : state.reduced || state.paused ? '静态画面 · 点击名称切换图样' : '滚动探索图样 · 移动鼠标，拨动粒子' }}</p>
  </section>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useThemeStore } from '@/stores/theme'
import { PARTICLE_SCENES } from '@/utils/loginParticleGeometry'
import { mountParticles, type ParticleController, type ParticleState } from '@/utils/loginParticleMotion'
import staticLogo from '@/assets/login-logo.svg'
const canvas = ref<HTMLCanvasElement>()
const state = ref<ParticleState>({ renderer: 'static', scene: 0, paused: false, reduced: false })
const theme = useThemeStore()
let controller: ParticleController | undefined, mounted = false
onMounted(() => {
  mounted = true
  if (canvas.value) controller = mountParticles(canvas.value, value => { if (mounted) state.value = value })
})
watch(() => theme.effectiveTheme, async () => { await nextTick(); if (mounted) controller?.refresh() })
onUnmounted(() => { mounted = false; controller?.destroy(); controller = undefined })
</script>

<style scoped>
.particle-scene { min-width: 0; color: var(--auth-muted); }
.particle-stage { position: relative; width: 100%; aspect-ratio: 1.12; }
canvas, .particle-fallback { position: absolute; inset: 0; width: 100%; height: 100%; display: block; }
canvas { touch-action: pan-y pinch-zoom; }
.particle-fallback { object-fit: contain; }
[data-renderer="static"] canvas { visibility: hidden; }
.particle-caption { display: flex; align-items: baseline; justify-content: space-between; padding-bottom: 15px; border-bottom: 1px solid var(--auth-line); }
.particle-eyebrow { font-size: 12px; letter-spacing: .12em; }
.particle-count { color: var(--auth-ink); font-size: 13px; font-variant-numeric: tabular-nums; }
.particle-count span { color: var(--auth-muted); margin-left: 5px; }
.particle-controls { display: flex; align-items: center; gap: 6px; padding-top: 10px; }
.particle-controls button { min-height: 44px; padding: 8px 13px; border: 1px solid transparent; border-radius: 24px; background: transparent; color: inherit; font: inherit; font-size: 12px; cursor: pointer; }
.particle-controls button[aria-pressed="true"] { color: var(--auth-ink); border-color: var(--auth-line); background: var(--auth-paper); }
.particle-controls button:hover:enabled { color: var(--auth-ink); }
.particle-controls button:focus-visible { outline: 2px solid var(--auth-focus); outline-offset: 2px; }
.particle-controls button:disabled { cursor: default; opacity: .55; }
.particle-controls .particle-pause { display: grid; place-items: center; flex: none; width: 44px; padding: 0; margin-left: auto; }
.particle-hint { margin: 6px 0 0; font-size: 11px; line-height: 1.6; letter-spacing: .02em; }
@media (max-width: 520px) {
  .particle-controls { gap: 0; }
  .particle-controls button { padding-inline: 9px; font-size: 11px; }
  .particle-caption { padding-bottom: 10px; }
  .particle-hint { font-size: 10px; }
}
</style>
