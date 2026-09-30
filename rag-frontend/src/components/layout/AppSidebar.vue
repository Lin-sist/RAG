<template>
  <button v-if="mobile" v-show="!drawerOpen" class="mobile-menu" aria-label="打开侧栏" @click="openDrawer"><PanelLeftOpen :size="20" /></button>
  <div v-if="mobile && drawerOpen" class="drawer-backdrop" @click="closeDrawer"></div>
  <aside v-show="!mobile || drawerOpen" ref="sidebar" :class="['chat-sidebar', { collapsed: !mobile && collapsed, 'mobile-drawer': mobile }]"
    :role="mobile ? 'dialog' : undefined" :aria-modal="mobile ? true : undefined" aria-label="主导航" @keydown="handleKeys">
    <div class="sidebar-header"><BrandMark :compact="!mobile && collapsed" /><button class="icon-button" :aria-label="mobile ? '关闭侧栏' : collapsed ? '展开侧栏' : '收起侧栏'" @click="mobile ? closeDrawer() : collapsed = !collapsed"><PanelLeftOpen v-if="!mobile && collapsed" :size="18" /><PanelLeftClose v-else :size="18" /></button></div>
    <nav class="fixed-nav" aria-label="主要入口">
      <button class="nav-item" :class="{ active: route.path === '/chat' }" title="新问答" @click="navigate('/chat')"><SquarePen :size="19" /><span>新问答</span></button>
      <button class="nav-item" :class="{ active: route.path.startsWith('/kb') }" title="知识库" @click="navigate('/kb')"><FolderOpen :size="19" /><span>知识库</span></button>
      <button class="nav-item" :class="{ active: route.path === '/history' }" title="历史记录" @click="navigate('/history')"><History :size="19" /><span>历史记录</span></button>
    </nav>
    <div v-if="mobile || !collapsed" class="sidebar-content">
      <h2>最近问答</h2>
      <p v-if="historyState === 'loading'" class="sidebar-status" role="status">正在加载最近记录…</p>
      <div v-else-if="historyState === 'error'" class="sidebar-status" role="status">最近记录加载失败<button class="retry-button" @click="loadHistory">重试最近记录</button></div>
      <p v-else-if="historyState === 'empty'" class="sidebar-status">暂无历史记录</p>
      <template v-else><button v-for="item in historyList" :key="item.id" class="history-item" :class="{ active: route.path === '/chat/' + item.id }" :title="item.title" @click="navigate('/chat/' + item.id)">{{ item.title }}</button></template>
    </div>
    <div class="user-bar">
      <button ref="menuTrigger" class="user-trigger" :aria-expanded="userMenuOpen" aria-controls="user-menu" :aria-label="username + '，打开用户菜单'" @click="toggleUserMenu"><span class="user-avatar">{{ userInitial }}</span><span class="user-name">{{ username }}</span><ChevronDown :size="16" /></button>
      <div v-if="userMenuOpen" id="user-menu" ref="userMenu" class="user-menu" @keydown="handleMenuKeys">
        <button @click="openSettings('general')"><Settings :size="16" />设置</button>
        <button @click="openSettings('profile')"><User :size="16" />个人资料</button>
        <button @click="theme.setPreference(theme.effectiveTheme === 'dark' ? 'light' : 'dark')"><Sun v-if="theme.effectiveTheme === 'dark'" :size="16" /><Moon v-else :size="16" />{{ theme.effectiveTheme === 'dark' ? '切换浅色' : '切换深色' }}</button>
        <div class="menu-divider"></div><button @click="handleLogout"><LogOut :size="16" />退出登录</button>
      </div>
    </div>
  </aside>
</template>
<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { SquarePen, FolderOpen, History, ChevronDown, PanelLeftOpen, PanelLeftClose, Settings, User, Sun, Moon, LogOut } from 'lucide-vue-next'
import BrandMark from '@/components/common/BrandMark.vue'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { getHistoryPage } from '@/api/history'
const emit = defineEmits<{ (e: 'open-settings', tab: 'general' | 'profile'): void }>()
const route = useRoute(), router = useRouter(), auth = useAuthStore(), theme = useThemeStore()
const collapsed = ref(false), mobile = ref(false), drawerOpen = ref(false), userMenuOpen = ref(false)
const sidebar = ref<HTMLElement>(), menuTrigger = ref<HTMLButtonElement>(), userMenu = ref<HTMLElement>()
const historyList = ref<{ id: string; title: string }[]>([])
const historyState = ref<'loading' | 'error' | 'empty' | 'ready'>('loading')
const username = computed(() => auth.userInfo?.username || '已登录用户')
const userInitial = computed(() => username.value.charAt(0).toUpperCase())
let sequence = 0, mounted = false
let media: MediaQueryList
let returnFocus: HTMLElement | null = null
let previousOverflow = ''
let background: HTMLElement | null = null
let previousInert = false
async function loadHistory() {
  const request = ++sequence, revision = auth.sessionRevision
  historyState.value = 'loading'
  historyList.value = []
  try {
    const res = await getHistoryPage(1, 20)
    if (!mounted || request !== sequence || revision !== auth.sessionRevision) return
    historyList.value = res.data.data.records.slice(0, 20).map(item => ({ id: String(item.id), title: item.question }))
    historyState.value = historyList.value.length ? 'ready' : 'empty'
  } catch {
    if (mounted && request === sequence && revision === auth.sessionRevision) historyState.value = 'error'
  }
}
watch(() => auth.sessionRevision, () => { sequence++; historyList.value = []; if (auth.isLoggedIn) void loadHistory() })
async function openDrawer() {
  returnFocus = document.activeElement as HTMLElement
  previousOverflow = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  background = document.querySelector('.shell-content')
  if (background) { previousInert = background.inert; background.inert = true }
  drawerOpen.value = true
  await nextTick()
  sidebar.value?.querySelector<HTMLButtonElement>('button')?.focus()
}
function closeDrawer() {
  if (!drawerOpen.value) return
  drawerOpen.value = false
  userMenuOpen.value = false
  document.body.style.overflow = previousOverflow
  if (background) background.inert = previousInert
  background = null
  const target = returnFocus
  void nextTick(() => { if (target?.isConnected) target.focus() })
}
function handleKeys(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    if (userMenuOpen.value) { userMenuOpen.value = false; menuTrigger.value?.focus() }
    else closeDrawer()
  }
  if (event.key !== 'Tab' || !mobile.value || !drawerOpen.value) return
  const buttons = Array.from(sidebar.value?.querySelectorAll<HTMLButtonElement>('button:not([disabled])') ?? [])
  const first = buttons[0], last = buttons[buttons.length - 1]
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
}
async function toggleUserMenu() {
  userMenuOpen.value = !userMenuOpen.value
  if (userMenuOpen.value) { await nextTick(); userMenu.value?.querySelector<HTMLButtonElement>('button')?.focus() }
}
function handleMenuKeys(event: KeyboardEvent) {
  if (event.key === 'Escape') { event.stopPropagation(); userMenuOpen.value = false; menuTrigger.value?.focus() }
  if (event.key === 'Tab') { event.preventDefault(); userMenuOpen.value = false; menuTrigger.value?.focus() }
  if (!['ArrowDown', 'ArrowUp'].includes(event.key)) return
  event.preventDefault()
  const buttons = Array.from(userMenu.value?.querySelectorAll<HTMLButtonElement>('button') ?? [])
  const current = buttons.indexOf(document.activeElement as HTMLButtonElement)
  buttons[(current + (event.key === 'ArrowDown' ? 1 : -1) + buttons.length) % buttons.length]?.focus()
}
async function openSettings(tab: 'general' | 'profile') {
  userMenuOpen.value = false
  if (mobile.value) { closeDrawer(); await nextTick() }
  else menuTrigger.value?.focus()
  emit('open-settings', tab)
}
function navigate(path: string) { userMenuOpen.value = false; closeDrawer(); void router.push(path) }
function handleLogout() { auth.clearAuth(); navigate('/login') }
function outsideClick(event: MouseEvent) { if (!(event.target as HTMLElement).closest('.user-bar')) userMenuOpen.value = false }
function resize() { mobile.value = media.matches; if (!mobile.value) closeDrawer() }
onMounted(() => {
  mounted = true
  media = window.matchMedia('(max-width: 768px)')
  resize()
  media.addEventListener('change', resize)
  document.addEventListener('click', outsideClick)
  window.addEventListener('rag-history-updated', loadHistory)
  void loadHistory()
})
onUnmounted(() => {
  mounted = false; sequence++; closeDrawer()
  media?.removeEventListener('change', resize)
  document.removeEventListener('click', outsideClick)
  window.removeEventListener('rag-history-updated', loadHistory)
})
</script>
<style scoped>
.chat-sidebar { width: 300px; flex: 0 0 300px; height: 100dvh; display: flex; flex-direction: column; background: var(--rag-bg-sidebar); border-right: 1px solid var(--rag-border); color: var(--rag-text-primary); padding: 12px; }
.sidebar-header { display: flex; align-items: center; justify-content: space-between; min-height: 44px; padding: 0 4px; margin-bottom: 20px; }
button { font-family: inherit; cursor: pointer; color: inherit; border: 0; background: transparent; }
.icon-button { width: 32px; height: 32px; display: grid; place-items: center; border-radius: 8px; flex: none; color: var(--rag-text-secondary); }
.icon-button:hover, .nav-item:hover, .history-item:hover, .user-trigger:hover, .user-menu button:hover { background: var(--rag-bg-hover); }
.fixed-nav { display: grid; gap: 3px; }
.nav-item { width: 100%; display: flex; gap: 12px; align-items: center; padding: 11px 12px; border-radius: 9px; text-align: left; font-size: 14px; }
.active { background: var(--rag-bg-hover); }
.sidebar-content { flex: 1; min-height: 0; overflow-y: auto; margin-top: 26px; }
h2 { font-size: 12px; font-weight: 500; padding: 0 12px 8px; color: var(--rag-text-placeholder); }
.history-item { display: block; width: 100%; padding: 10px 12px; text-align: left; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; border-radius: 8px; font-size: 13px; }
.sidebar-status { padding: 12px; color: var(--rag-text-secondary); font-size: 13px; }
.retry-button { display: block; padding-top: 8px; text-decoration: underline; }
.user-bar { position: relative; margin-top: auto; padding-top: 12px; }
.user-trigger { display: flex; align-items: center; gap: 10px; width: 100%; padding: 8px; border-radius: 10px; }
.user-avatar { width: 32px; height: 32px; flex: none; display: grid; place-items: center; border-radius: 50%; background: var(--rag-bg-hover); border: 1px solid var(--rag-border); font-size: 13px; }
.user-name { flex: 1; text-align: left; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; }
.user-menu { position: absolute; bottom: calc(100% + 8px); left: 0; width: 250px; padding: 6px; background: var(--rag-bg-overlay); border: 1px solid var(--rag-border); border-radius: 13px; box-shadow: var(--rag-shadow-lg); z-index: 55; }
.user-menu button { display: flex; align-items: center; gap: 12px; width: 100%; padding: 11px 12px; border-radius: 8px; font-size: 13px; text-align: left; }
.menu-divider { height: 1px; background: var(--rag-border); margin: 5px; }
.collapsed { width: 76px; flex-basis: 76px; }
.collapsed .sidebar-header { flex-direction: column; gap: 6px; }
.collapsed .nav-item { justify-content: center; padding: 12px; }
.collapsed .nav-item span, .collapsed .user-name, .collapsed .user-trigger > svg { display: none; }
.mobile-menu { position: fixed; top: 12px; left: 12px; z-index: 40; width: 36px; height: 36px; display: grid; place-items: center; border-radius: 9px; border: 1px solid var(--rag-border); background: var(--rag-bg-sidebar); }
.drawer-backdrop { position: fixed; inset: 0; background: rgba(0,0,0,.45); z-index: 60; }
.mobile-drawer { position: fixed; left: 0; top: 0; width: min(300px, calc(100vw - 32px)); z-index: 61; }
</style>
