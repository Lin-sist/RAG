<template>
  <div class="login-logo-motion" :data-renderer="state.renderer" aria-hidden="true">
    <canvas ref="canvas" />
    <img class="static-logo" :src="staticLogo" alt="" />
  </div>
</template>
<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useThemeStore } from '@/stores/theme'
import { mount, type LoginMotionController, type LoginMotionState } from '@/utils/loginLogoMotion'
import staticLogo from '@/assets/login-logo.svg'

const canvas = ref<HTMLCanvasElement>()
const theme = useThemeStore()
const state = ref<LoginMotionState>({ renderer: 'static', reducedMotion: false, visible: true, texture: 'dots' })
let controller: LoginMotionController | undefined, mounted = false
onMounted(() => {
  mounted = true
  if (!canvas.value) return
  try {
    controller = mount(canvas.value, { onState(value) { if (mounted) state.value = value } })
  } catch { state.value = { ...state.value, renderer: 'static' } }
})
watch(() => theme.effectiveTheme, async () => {
  await nextTick()
  if (mounted) controller?.refresh()
})
onUnmounted(() => { mounted = false; controller?.destroy(); controller = undefined })
</script>
<style scoped>
.login-logo-motion { position: relative; aspect-ratio: 1; }
canvas, .static-logo { position: absolute; inset: 0; display: block; width: 100%; height: 100%; }
.static-logo { object-fit: contain; }
.login-logo-motion[data-renderer="webgl"] .static-logo { display: none; }
.login-logo-motion[data-renderer="static"] canvas { visibility: hidden; }
</style>
