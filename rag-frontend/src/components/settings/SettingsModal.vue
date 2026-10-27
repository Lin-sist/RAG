<template>
  <Teleport to="body">
    <div v-if="modelValue" class="settings-backdrop" @click.self="close">
      <section ref="dialog" class="settings-modal" role="dialog" aria-modal="true" aria-labelledby="settings-title" @keydown="handleKeys" tabindex="-1">
        <header class="settings-header"><h1 id="settings-title">设置</h1><button ref="closeButton" type="button" class="close-button" aria-label="关闭设置" @click="close"><X :size="20" /></button></header>
        <div class="settings-body">
          <nav class="settings-nav" aria-label="设置分类">
            <button v-for="category in categories" :key="category.id" type="button" :class="{ active: activeTab === category.id }" :aria-current="activeTab === category.id ? 'page' : undefined" @click="chooseTab(category.id)"><component :is="category.icon" :size="18" aria-hidden="true" /><span>{{ category.label }}</span></button>
          </nav>
          <div class="settings-mobile-nav"><label for="settings-category">设置分类</label><SettingsSelect id="settings-category" label="设置分类" :model-value="activeTab" :options="categoryOptions" :container="dialog" @update:model-value="chooseTab($event as Tab)" @escape="dismissLayer" /></div>
          <div ref="content" class="settings-content">
            <template v-if="activeTab === 'appearance'">
              <h2>外观</h2><p class="section-description">调整阅读体验，让界面适合你的使用习惯。</p>
              <fieldset class="theme-group"><legend>颜色模式</legend><div class="theme-options">
                <label v-for="mode in themeModes" :key="mode.value" class="theme-option" :class="{ selected: theme.preference === mode.value }">
                  <input type="radio" name="settings-theme" :value="mode.value" :checked="theme.preference === mode.value" @change="theme.setPreference(mode.value)" />
                  <span class="theme-thumbnail" :data-preview="mode.value" aria-hidden="true"><span class="thumbnail-sidebar"></span><span class="thumbnail-content"><i></i><i></i><i></i><b></b></span></span>
                  <span class="theme-label">{{ mode.label }}<Check v-if="theme.preference === mode.value" :size="16" aria-hidden="true" /></span>
                </label>
              </div></fieldset>
              <div class="setting-row"><label for="answer-text-size">回答字号</label><SettingsSelect id="answer-text-size" label="回答字号" :model-value="preferences.values.answerTextSize" :options="textSizeOptions" :container="dialog" @update:model-value="changePreference('answerTextSize', $event)" @escape="dismissLayer" /></div>
              <div class="reading-preview"><p class="preview-caption">预览示例</p><div class="preview-answer">清晰的回答，需要能核对的依据。</div><p class="preview-source"><FileText :size="14" aria-hidden="true" />引用来源 · 示例资料</p></div>
              <div class="setting-row motion-row"><div><label for="motion-preference">减少动态效果</label><p id="motion-description" class="row-description">遵循系统偏好，或始终减少运动与自动播放。</p></div><SettingsSelect id="motion-preference" label="减少动态效果" :model-value="preferences.values.motion" described-by="motion-description" :options="motionOptions" :container="dialog" @update:model-value="changePreference('motion', $event)" @escape="dismissLayer" /></div>
            </template>
            <template v-else-if="activeTab === 'conversation'">
              <h2>问答偏好</h2><p class="section-description">选择发送方式，以及来源片段的初始展示。</p>
              <div class="setting-row"><div><label for="send-key">发送快捷键</label><p id="send-key-description" class="row-description">{{ preferences.values.sendKey === 'enter' ? 'Shift + Enter 换行，发送按钮始终可用。' : 'Enter 换行，Ctrl / ⌘ + Enter 发送。' }}</p></div><SettingsSelect id="send-key" label="发送快捷键" :model-value="preferences.values.sendKey" described-by="send-key-description" :options="sendKeyOptions" :container="dialog" @update:model-value="changePreference('sendKey', $event)" @escape="dismissLayer" /></div>
              <div class="setting-row"><div><label for="source-expansion">来源片段</label><p id="source-description" class="row-description">只影响新显示的来源卡，手动展开状态会保留。</p></div><SettingsSelect id="source-expansion" label="来源片段" :model-value="preferences.values.sources" described-by="source-description" :options="sourceOptions" :container="dialog" @update:model-value="changePreference('sources', $event)" @escape="dismissLayer" /></div>
              <div class="settings-note"><h3>回答方式</h3><p>同步／流式可在输入框高级选项中选择。</p><p>来源是否可见，仍取决于本次回答的完整终态。</p></div>
              <p class="settings-hint">输入法正在选词时，Enter 用于确认文字。</p>
            </template>
            <template v-else-if="activeTab === 'account'">
              <h2>账户与工作空间</h2><p class="section-description">查看当前登录身份和资料归属。</p>
              <div class="profile-summary"><span class="profile-avatar">{{ username.charAt(0).toUpperCase() }}</span><div><strong>{{ username }}</strong><p>当前登录资料</p></div></div>
              <p v-if="!auth.userInfo" class="settings-hint" role="status">资料暂不可用</p>
              <dl v-else class="profile-details"><div><dt>用户名</dt><dd>{{ auth.userInfo.username || '未提供' }}</dd></div><div><dt>邮箱</dt><dd>{{ auth.userInfo.email || '未提供' }}</dd></div></dl>
              <dl class="profile-details"><div><dt>所属工作空间</dt><dd><WorkspaceContext mode="profile" /></dd></div></dl>
              <p class="settings-hint">资料可见范围以当前工作空间及知识库授权为准。</p>
              <div class="setting-row logout-row"><span>当前登录</span><button type="button" class="outlined-button" :disabled="loggingOut || logoutPending" :aria-busy="loggingOut || logoutPending" @click="handleLogout"><LogOut :size="16" aria-hidden="true" />{{ loggingOut || logoutPending ? '正在退出…' : '退出登录' }}</button></div>
            </template>
            <template v-else-if="activeTab === 'data'">
              <h2>数据管理</h2><p class="section-description">区分浏览器偏好与服务端历史记录。</p>
              <section class="settings-group"><h3>当前浏览器</h3><p>外观和操作偏好保存在当前浏览器。同一浏览器中的其他账号也会沿用，不会同步到其他设备。</p>
                <button ref="resetTrigger" type="button" class="outlined-button" :aria-expanded="resetConfirm" aria-controls="settings-reset-confirm" @click="beginReset">重置本地偏好</button>
                <section v-if="resetConfirm" id="settings-reset-confirm" class="reset-confirm" aria-labelledby="reset-title"><h3 id="reset-title">恢复这些默认设置？</h3><ul><li>颜色模式：跟随系统</li><li>回答字号：标准 · 15 px</li><li>减少动态效果：跟随系统</li><li>发送快捷键：Enter 发送</li><li>来源片段：默认收起</li></ul><p>只重置以上偏好，历史记录、知识库和登录信息保持原样。</p><div class="confirm-actions"><button ref="cancelResetButton" type="button" class="outlined-button" @click="cancelReset">取消</button><button type="button" class="solid-button" @click="confirmReset">确认重置</button></div></section>
                <p class="reset-result" role="status">{{ resetResult }}</p>
              </section>
              <section class="settings-group history-group"><h3>历史记录</h3><p>在历史记录页面查看和管理已经保存的问答。</p><RouterLink class="outlined-button history-link" to="/history" @click="openHistory">打开历史记录<ArrowUpRight :size="16" aria-hidden="true" /></RouterLink></section>
            </template>
            <template v-else>
              <h2>关于与帮助</h2><p class="section-description">了解现有问答功能及常见状态。</p>
              <section class="settings-group"><h3>开始一次问答</h3><p>先选择可访问的知识库，再输入问题。发送方式可在“问答偏好”中调整；同步／流式在输入框高级选项中选择。</p></section>
              <section class="settings-group"><h3>核对回答依据</h3><p>完整回答下方可展开引用来源，查看实际返回的资料片段。没有足够依据或回答未完整结束时，不应把部分文本当作已核实结论。</p></section>
              <section class="settings-group"><h3>查看执行过程</h3><p>在问答上方切换到“轨迹”，查看本次请求已有的阶段信息。旧历史记录不一定保存执行轨迹。</p></section>
              <section class="settings-group"><h3>停止接收与历史</h3><p>“停止接收”只表示浏览器停止接收，不代表服务端已取消。历史详情展示单条问答，后续提问请回到新问答。</p></section>
              <section class="settings-group"><h3>偏好保存</h3><p>设置只在当前浏览器保存。其他标签页刷新后读取最新设置，不会同步到其他设备。</p></section>
              <div class="version-row"><span>前端版本</span><span>{{ FRONTEND_VERSION }}</span></div>
            </template>
          </div>
        </div>
        <footer class="settings-footer"><p>更改即时生效，仅保存在当前浏览器。</p><p class="persistence-status" role="status">{{ persistenceMessage }}</p></footer>
      </section>
    </div>
  </Teleport>
</template>
<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { RouterLink } from 'vue-router'
import { ElMessage } from 'element-plus'
import { X, Sun, SlidersHorizontal, UserRound, Database, CircleHelp, Check, FileText, LogOut, ArrowUpRight } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useAuth } from '@/composables/useAuth'
import { useThemeStore } from '@/stores/theme'
import { usePreferencesStore, type Preferences } from '@/stores/preferences'
import { FRONTEND_VERSION } from '@/utils/buildInfo'
import WorkspaceContext from '@/components/common/WorkspaceContext.vue'
import SettingsSelect from './SettingsSelect.vue'

type Tab = 'appearance' | 'conversation' | 'account' | 'data' | 'about'
const props = withDefaults(defineProps<{ modelValue: boolean; initialTab?: Tab | 'general' | 'profile' | 'security' | 'apiKey' }>(), { initialTab: 'general' })
const emit = defineEmits<{ (event: 'update:modelValue', value: boolean): void }>()
const categories = [
  { id: 'appearance', label: '外观', icon: Sun }, { id: 'conversation', label: '问答偏好', icon: SlidersHorizontal },
  { id: 'account', label: '账户与工作空间', icon: UserRound }, { id: 'data', label: '数据管理', icon: Database },
  { id: 'about', label: '关于与帮助', icon: CircleHelp },
] as const
const themeModes = [{ value: 'system', label: '跟随系统' }, { value: 'light', label: '浅色' }, { value: 'dark', label: '深色' }] as const
const categoryOptions = categories.map(category => ({ value: category.id, label: category.label }))
const textSizeOptions = [{ value: 'small', label: '小 · 14 px' }, { value: 'standard', label: '标准 · 15 px' }, { value: 'large', label: '大 · 17 px' }]
const motionOptions = [{ value: 'system', label: '跟随系统' }, { value: 'reduce', label: '始终减少' }]
const sendKeyOptions = [{ value: 'enter', label: 'Enter 发送' }, { value: 'modifier-enter', label: 'Ctrl / ⌘ + Enter 发送' }]
const sourceOptions = [{ value: 'collapsed', label: '默认收起' }, { value: 'expanded', label: '默认展开' }]
const auth = useAuthStore(), theme = useThemeStore(), preferences = usePreferencesStore()
const { logout, logoutPending } = useAuth()
const loggingOut = ref(false)
const username = computed(() => auth.userInfo?.username || '已登录用户')
function mapTab(tab: typeof props.initialTab): Tab {
  if (tab === 'profile') return 'account'
  return categories.some(category => category.id === tab) ? tab as Tab : 'appearance'
}
const activeTab = ref<Tab>(mapTab(props.initialTab))
const dialog = ref<HTMLElement>(), content = ref<HTMLElement>(), closeButton = ref<HTMLButtonElement>()
const resetTrigger = ref<HTMLButtonElement>(), cancelResetButton = ref<HTMLButtonElement>()
const resetConfirm = ref(false), resetResult = ref('')
const persistenceMessage = computed(() => !theme.persistenceAvailable || !preferences.persistenceAvailable
  ? '本次更改已生效，浏览器暂无法保存，刷新后可能丢失。' : '')
let previousFocus: HTMLElement | null = null
let previousOverflow = '', lockHeld = false, generation = 0
let background: { element: HTMLElement; inert: boolean }[] = []
function release() {
  if (!lockHeld) return
  for (const item of background) item.element.inert = item.inert
  background = []; lockHeld = false
  document.body.style.overflow = previousOverflow
  if (previousFocus?.isConnected && (!previousFocus.getClientRects || previousFocus.getClientRects().length)) previousFocus.focus()
  else document.querySelector<HTMLElement>('.mobile-menu, .user-trigger')?.focus()
}
async function syncOpen(open: boolean) {
  const current = ++generation
  if (!open) { release(); return }
  activeTab.value = mapTab(props.initialTab); resetConfirm.value = false; resetResult.value = ''
  previousFocus = document.activeElement as HTMLElement
  previousOverflow = document.body.style.overflow
  document.body.style.overflow = 'hidden'; lockHeld = true
  await nextTick()
  if (current !== generation) return
  const app = document.getElementById('app')
  if (app) { background = [{ element: app, inert: app.inert }]; app.inert = true }
  closeButton.value?.focus()
}
watch(() => props.modelValue, syncOpen)
onMounted(() => { if (props.modelValue) void syncOpen(true) })
watch(() => props.initialTab, tab => chooseTab(mapTab(tab)))
function chooseTab(tab: Tab) {
  activeTab.value = tab; resetConfirm.value = false; resetResult.value = ''
  if (content.value) content.value.scrollTop = 0
}
function close() { emit('update:modelValue', false) }
function changePreference<K extends keyof Preferences>(key: K, value: string) { preferences.setPreference(key, value as Preferences[K]) }
function dismissLayer() { if (resetConfirm.value) void cancelReset(); else close() }
async function beginReset() { resetResult.value = ''; resetConfirm.value = true; await nextTick(); cancelResetButton.value?.focus() }
async function cancelReset() { resetConfirm.value = false; await nextTick(); resetTrigger.value?.focus() }
async function confirmReset() {
  theme.setPreference('system'); preferences.reset()
  await cancelReset()
  resetResult.value = theme.persistenceAvailable && preferences.persistenceAvailable ? '已恢复默认设置。' : '当前已恢复默认，但浏览器暂无法保存，刷新后可能恢复旧设置。'
}
function openHistory(event: MouseEvent) { if (!event.ctrlKey && !event.metaKey && !event.shiftKey && !event.altKey && event.button === 0) close() }
async function handleLogout() {
  if (loggingOut.value || logoutPending?.value) return
  loggingOut.value = true
  try { const result = await logout(); if (result === 'unconfirmed') ElMessage.warning('本地登录信息已清除，服务端退出未确认。') }
  catch { ElMessage.warning('本地登录信息已清除，登录页跳转失败，请刷新页面。') }
  finally { loggingOut.value = false }
}
function handleKeys(event: KeyboardEvent) {
  if (event.key === 'Escape') { event.preventDefault(); event.stopPropagation(); dismissLayer(); return }
  if (event.key !== 'Tab') return
  const controls = Array.from(dialog.value?.querySelectorAll<HTMLElement>('button:not([disabled]), a[href], input:not([disabled]), select:not([disabled]), [tabindex="0"]') ?? []).filter(element => {
    if (!element.getClientRects().length) return false
    if (element instanceof HTMLInputElement && element.type === 'radio' && !element.checked) return false
    return true
  })
  const first = controls[0], last = controls[controls.length - 1]
  if (!first) { event.preventDefault(); dialog.value?.focus(); return }
  if (event.shiftKey && (document.activeElement === first || document.activeElement === dialog.value)) { event.preventDefault(); last?.focus() }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus() }
}
onUnmounted(() => { generation++; release() })
</script>
<style scoped src="./settings.css"></style>
