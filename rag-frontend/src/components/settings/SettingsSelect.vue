<template>
  <div ref="root" class="settings-select" :class="{ 'keyboard-focus': keyboardFocus }" :data-setting-id="id" :data-value="modelValue" @pointerdown.capture="pointerFocus = true; keyboardFocus = false" @focusin="keyboardFocus = !pointerFocus" @focusout="focusOut" @keydown.capture="handleKeydown">
    <ElSelect :id="id" :model-value="modelValue" :aria-label="label" :append-to="container" :teleported="!!container"
      :persistent="false" :show-arrow="false" :suffix-icon="ChevronDown" :offset="6" :fit-input-width="true"
      placement="bottom-end" :fallback-placements="['top-end', 'bottom-start', 'top-start']" :popper-options="popperOptions"
      :popper-style="{ '--settings-menu-max-height': `${menuHeight}px` }" popper-class="settings-select-popper"
      @update:model-value="value => emit('update:modelValue', String(value))" @visible-change="visibilityChanged">
      <ElOption v-for="option in options" :key="option.value" :value="option.value" :label="option.label" :data-value="option.value">
        <span class="settings-option-label">{{ option.label }}</span><Check v-if="modelValue === option.value" class="settings-option-check" :size="16" aria-hidden="true" />
      </ElOption>
    </ElSelect>
  </div>
</template>
<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElSelect, ElOption } from 'element-plus'
import { Check, ChevronDown } from 'lucide-vue-next'

const props = defineProps<{
  id: string; label: string; modelValue: string; describedBy?: string; container?: HTMLElement
  options: readonly { value: string; label: string }[]
}>()
const emit = defineEmits<{ (event: 'update:modelValue', value: string): void; (event: 'escape'): void }>()
const root = ref<HTMLElement>(), expanded = ref(false), menuHeight = ref(260)
const keyboardFocus = ref(false)
const pointerFocus = ref(false)
function focusOut(event: FocusEvent) {
  if (!root.value?.contains(event.relatedTarget as Node | null)) pointerFocus.value = false
}
let observer: ResizeObserver | undefined
const popperOptions = computed(() => ({
  strategy: 'fixed' as const,
  modifiers: [
    { name: 'flip', options: { boundary: props.container, padding: 8 } },
    { name: 'preventOverflow', options: { boundary: props.container, padding: 8, altAxis: true } },
  ],
}))
function fitMenu() {
  if (!expanded.value || !root.value || !props.container) return
  const anchor = root.value.getBoundingClientRect(), bounds = props.container.getBoundingClientRect()
  const top = Math.max(0, bounds.top), bottom = Math.min(window.innerHeight, bounds.bottom)
  menuHeight.value = Math.max(40, Math.min(260, Math.max(anchor.top - top, bottom - anchor.bottom) - 24))
}
function cleanup() {
  observer?.disconnect(); observer = undefined
  window.removeEventListener('resize', fitMenu)
  window.visualViewport?.removeEventListener('resize', fitMenu)
  document.removeEventListener('scroll', fitMenu, true)
}
function visibilityChanged(open: boolean) {
  expanded.value = open
  cleanup()
  if (!open) return
  fitMenu()
  window.addEventListener('resize', fitMenu)
  window.visualViewport?.addEventListener('resize', fitMenu)
  document.addEventListener('scroll', fitMenu, true)
  observer = new ResizeObserver(fitMenu)
  if (props.container) observer.observe(props.container)
}
function handleKeydown(event: KeyboardEvent) {
  keyboardFocus.value = true
  // ElSelect owns Escape while open; its handler also stops Escape when closed.
  if (event.key === 'Escape' && !expanded.value) { event.preventDefault(); event.stopPropagation(); emit('escape') }
}
onMounted(async () => {
  await nextTick()
  if (props.describedBy) root.value?.querySelector('input[role="combobox"]')?.setAttribute('aria-describedby', props.describedBy)
})
onUnmounted(cleanup)
</script>
<style src="./settingsSelect.css"></style>
