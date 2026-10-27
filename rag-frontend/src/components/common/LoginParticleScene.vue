<template>
  <section class="particle-scene" :data-renderer="state.renderer" aria-label="从知识到回答">
    <button class="particle-stage" type="button" :disabled="state.renderer !== 'webgl'"
      :aria-label="'图样：' + PARTICLE_SCENES[state.scene] + '。点击切换下一幕，左右方向键选择图样。'"
      aria-keyshortcuts="ArrowLeft ArrowRight Home End" @click="nextScene" @keydown="navigate">
      <canvas ref="canvas" aria-hidden="true" />
      <img v-if="state.renderer === 'static'" class="particle-fallback" :src="staticLogo" alt="" />
    </button>
    <div class="particle-controls">
      <button class="particle-pause" type="button" :disabled="state.renderer !== 'webgl' || state.reduced"
        :aria-label="state.reduced ? '系统已减少动态效果' : state.paused ? '继续粒子动画' : '暂停粒子动画'"
        :aria-pressed="state.paused" @click="controller?.pause()">
        <svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true">
          <path v-if="state.paused" d="M5 3 L12 8 L5 13 Z" fill="currentColor" />
          <path v-else d="M5 3 V13 M11 3 V13" fill="none" stroke="currentColor" stroke-width="1.5" />
        </svg>
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useThemeStore } from '@/stores/theme'
import { usePreferencesStore } from '@/stores/preferences'
import { PARTICLE_SCENES } from '@/utils/loginParticleGeometry'
import { mountParticles, type ParticleController, type ParticleState } from '@/utils/loginParticleMotion'
import staticLogo from '@/assets/login-logo.svg'
const canvas = ref<HTMLCanvasElement>()
const emit = defineEmits<{ change: [state: ParticleState] }>()
const state = ref<ParticleState>({ renderer: 'static', scene: 0, progress: 0, paused: false, reduced: false })
const theme = useThemeStore()
const preferences = usePreferencesStore()
let controller: ParticleController | undefined, mounted = false
function nextScene() {
  controller?.select((state.value.scene + 1) % PARTICLE_SCENES.length)
}
function navigate(event: KeyboardEvent) {
  if (event.ctrlKey || event.metaKey || event.altKey) return
  const choices: Record<string, number> = { ArrowLeft: state.value.scene - 1, ArrowRight: state.value.scene + 1, Home: 0, End: PARTICLE_SCENES.length - 1 }
  const index = choices[event.key]
  if (index === undefined) return
  event.preventDefault()
  controller?.select(Math.max(0, Math.min(PARTICLE_SCENES.length - 1, index)))
}
onMounted(() => {
  mounted = true
  if (canvas.value) controller = mountParticles(canvas.value, value => {
    if (mounted) { state.value = value; emit('change', value) }
  }, { reduced: () => preferences.reducedMotion, subscribe: listener => watch(() => preferences.reducedMotion, listener, { flush: 'sync' }) })
})
watch(() => theme.effectiveTheme, async () => { await nextTick(); if (mounted) controller?.refresh() })
onUnmounted(() => { mounted = false; controller?.destroy(); controller = undefined })
</script>

<style scoped>
.particle-scene { min-width: 0; color: var(--auth-muted); }
.particle-stage { position: relative; display: block; width: 100%; aspect-ratio: 1; border: 0; padding: 0; background: transparent; color: inherit; cursor: pointer; }
canvas, .particle-fallback { position: absolute; inset: 0; width: 100%; height: 100%; display: block; }
canvas { touch-action: pan-y pinch-zoom; }
.particle-fallback { object-fit: contain; }
[data-renderer="static"] canvas { visibility: hidden; }
.particle-controls { display: flex; justify-content: flex-end; }
.particle-pause { display: grid; place-items: center; width: 44px; height: 44px; padding: 0; border: 0; border-radius: 50%; background: transparent; color: inherit; cursor: pointer; }
.particle-pause:hover:enabled { color: var(--auth-ink); }
button:focus-visible { outline: 2px solid var(--auth-focus); outline-offset: 3px; }
button:disabled { cursor: default; }
.particle-pause:disabled { opacity: .55; }
</style>
