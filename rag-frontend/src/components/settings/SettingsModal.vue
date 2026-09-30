<template>
  <Teleport to="body">
    <div v-if="modelValue" class="settings-backdrop" @click.self="close">
      <section ref="dialog" class="settings-modal" role="dialog" aria-modal="true" aria-labelledby="settings-title" @keydown="handleKeys" tabindex="-1">
        <header><h1 id="settings-title">设置</h1><button ref="closeButton" class="close-button" aria-label="关闭设置" @click="close"><X :size="20" /></button></header>
        <div class="settings-body">
          <nav aria-label="设置分类">
            <button :class="{ active: activeTab === 'general' }" :aria-current="activeTab === 'general' ? 'page' : undefined" @click="activeTab = 'general'"><Settings2 :size="18" />常规</button>
            <button :class="{ active: activeTab === 'profile' }" :aria-current="activeTab === 'profile' ? 'page' : undefined" @click="activeTab = 'profile'"><User :size="18" />个人资料</button>
          </nav>
          <div class="settings-content">
            <template v-if="activeTab === 'general'">
              <h2>常规</h2>
              <div class="appearance-row"><label for="theme-preference">外观</label>
                <select id="theme-preference" :value="theme.preference" @change="chooseTheme">
                  <option value="system">跟随系统</option><option value="light">浅色</option><option value="dark">深色</option>
                </select>
              </div>
              <p class="settings-hint">外观偏好仅保存在当前浏览器。</p>
              <p v-if="!theme.persistenceAvailable" class="settings-hint" role="status">当前外观已生效，浏览器暂无法保存偏好。</p>
            </template>
            <template v-else>
              <h2>个人资料</h2>
              <div class="profile-summary"><span class="profile-avatar">{{ username.charAt(0).toUpperCase() }}</span><div><strong>{{ username }}</strong><p>当前登录资料</p></div></div>
              <p v-if="!auth.userInfo" class="settings-hint" role="status">资料暂不可用</p>
              <dl v-else><div><dt>用户名</dt><dd>{{ auth.userInfo.username || '未提供' }}</dd></div><div><dt>邮箱</dt><dd>{{ auth.userInfo.email || '未提供' }}</dd></div></dl>
              <p class="settings-hint">此处仅展示当前登录资料。</p>
            </template>
          </div>
        </div>
      </section>
    </div>
  </Teleport>
</template>
<script setup lang="ts">
import { ref, computed, watch, nextTick, onUnmounted } from 'vue'
import { X, User, Settings2 } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore, type ThemePreference } from '@/stores/theme'
// Existing alternate shell still passes legacy tabs; unsupported tabs open general appearance.
const props = withDefaults(defineProps<{ modelValue: boolean; initialTab?: 'general' | 'profile' | 'security' | 'apiKey' }>(), { initialTab: 'general' })
const emit = defineEmits<{ (event: 'update:modelValue', value: boolean): void }>()
const auth = useAuthStore(), theme = useThemeStore()
const username = computed(() => auth.userInfo?.username || '已登录用户')
const activeTab = ref<'general' | 'profile'>(props.initialTab === 'profile' ? 'profile' : 'general')
const dialog = ref<HTMLElement>(), closeButton = ref<HTMLButtonElement>()
let previousFocus: HTMLElement | null = null
let previousOverflow = ''
let background: { element: HTMLElement; inert: boolean }[] = []
let lockHeld = false
let generation = 0
function release() {
  if (!lockHeld) return
  for (const item of background) item.element.inert = item.inert
  background = []
  lockHeld = false
  document.body.style.overflow = previousOverflow
  if (previousFocus?.isConnected) previousFocus.focus()
}
watch(() => props.modelValue, async open => {
  const current = ++generation
  if (!open) { release(); return }
  activeTab.value = props.initialTab === 'profile' ? 'profile' : 'general'
  previousFocus = document.activeElement as HTMLElement
  previousOverflow = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  lockHeld = true
  await nextTick()
  if (current !== generation) return
  const app = document.getElementById('app')
  if (app) { background = [{ element: app, inert: app.inert }]; app.inert = true }
  closeButton.value?.focus()
})
watch(() => props.initialTab, tab => { activeTab.value = tab === 'profile' ? 'profile' : 'general' })
function close() { emit('update:modelValue', false) }
function chooseTheme(event: Event) { theme.setPreference((event.target as HTMLSelectElement).value as ThemePreference) }
function handleKeys(event: KeyboardEvent) {
  if (event.key === 'Escape') { event.preventDefault(); close(); return }
  if (event.key !== 'Tab') return
  const controls = Array.from(dialog.value?.querySelectorAll<HTMLElement>('button:not([disabled]), select:not([disabled]), [tabindex="0"]') ?? [])
  const first = controls[0], last = controls[controls.length - 1]
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
}
onUnmounted(() => { generation++; release() })
</script>
<style scoped>
.settings-backdrop { position: fixed; inset: 0; z-index: 100; display: grid; place-items: center; padding: 16px; background: rgba(0,0,0,.5); color: var(--rag-text-primary); }
.settings-modal { width: min(760px, 100%); max-height: calc(100dvh - 32px); overflow: auto; border: 1px solid var(--rag-border); border-radius: 18px; background: var(--rag-bg-overlay); box-shadow: var(--rag-shadow-lg); }
header { display: flex; align-items: center; justify-content: space-between; padding: 22px 24px; border-bottom: 1px solid var(--rag-border); }
h1 { font-size: 20px; font-weight: 600; } h2 { font-size: 18px; font-weight: 600; margin-bottom: 28px; }
button, select { font: inherit; color: inherit; cursor: pointer; }
button { border: none; background: transparent; }
.close-button { display: grid; place-items: center; width: 32px; height: 32px; border-radius: 8px; color: var(--rag-text-secondary); }
button:hover { background: var(--rag-bg-hover); }
.settings-body { display: grid; grid-template-columns: 180px minmax(0, 1fr); min-height: 310px; }
nav { padding: 16px 12px; border-right: 1px solid var(--rag-border); }
nav button { display: flex; align-items: center; gap: 10px; padding: 11px 12px; border-radius: 8px; width: 100%; text-align: left; font-size: 14px; margin-bottom: 4px; }
nav button.active { background: var(--rag-bg-hover); }
.settings-content { padding: 28px; min-width: 0; }
.appearance-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding-bottom: 18px; border-bottom: 1px solid var(--rag-border); font-size: 14px; }
select { min-width: 120px; max-width: 100%; border: 1px solid var(--rag-border); border-radius: 8px; background: var(--rag-bg-overlay); padding: 7px 10px; font-size: 13px; }
.settings-hint { color: var(--rag-text-secondary); font-size: 12px; line-height: 1.7; margin-top: 14px; }
.profile-summary { display: flex; gap: 14px; align-items: center; }
.profile-avatar { display: grid; place-items: center; flex: none; width: 44px; height: 44px; border: 1px solid var(--rag-border); border-radius: 50%; background: var(--rag-bg-hover); }
.profile-summary strong { font-size: 14px; overflow-wrap: anywhere; }
.profile-summary p { font-size: 12px; color: var(--rag-text-secondary); }
dl { font-size: 13px; margin-top: 20px; }
dl div { display: grid; grid-template-columns: 64px minmax(0,1fr); gap: 14px; padding: 12px 0; border-bottom: 1px solid var(--rag-border); }
dt { color: var(--rag-text-secondary); } dd { overflow-wrap: anywhere; }
@media (max-width: 600px) {
  .settings-body { grid-template-columns: 1fr; min-height: 320px; }
  nav { display: flex; gap: 4px; padding: 12px; border-right: 0; border-bottom: 1px solid var(--rag-border); }
  nav button { width: auto; margin: 0; }
  header { padding: 16px 20px; } .settings-content { padding: 22px 20px; }
}
</style>
