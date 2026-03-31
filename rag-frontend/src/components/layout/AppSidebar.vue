<template>
  <aside :class="['chat-sidebar', { collapsed: sidebarCollapsed }]">
    <div class="sidebar-header">
      <div v-if="sidebarCollapsed" class="logo-toggle-container" @click="toggleSidebar">
        <div class="logo-wrapper">
          <Layers :size="20" />
        </div>
        <button class="collapse-btn-overlay" title="展开侧边栏">
          <PanelLeftOpen :size="18" />
        </button>
      </div>

      <template v-else>
        <div class="logo-wrapper">
          <Layers :size="20" />
        </div>
        <span class="logo-text">RAG 智能问答</span>
        <button class="collapse-btn" @click="toggleSidebar" title="收起侧边栏">
          <PanelLeftClose :size="18" />
        </button>
      </template>
    </div>

    <button
      :class="['new-chat-btn', { active: isNewChatActive }]"
      @click="goNewChat"
      :title="sidebarCollapsed ? 'New Chat' : ''"
    >
      <Plus :size="18" />
      <span v-if="!sidebarCollapsed">New Chat</span>
    </button>

    <div class="sidebar-content">
      <div class="collapsible-section">
        <div v-if="!sidebarCollapsed" class="section-header" @click="toggleKbSection">
          <ChevronDown :size="16" :class="['chevron-icon', { collapsed: !kbSectionExpanded }]" />
          <span class="section-title">知识库</span>
        </div>

        <div :class="['section-body', { expanded: kbSectionExpanded || sidebarCollapsed }]">
          <div class="kb-list">
            <div
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :class="['kb-item', { active: isKbActive(kb.id) }]"
              @click="goKbDetail(kb.id)"
              :title="sidebarCollapsed ? kb.name : ''"
            >
              <FolderOpen :size="16" class="kb-icon" />
              <span v-if="!sidebarCollapsed" class="kb-name">{{ kb.name }}</span>
              <span v-if="!sidebarCollapsed" class="kb-doc-count">{{ kb.documentCount }}篇</span>
            </div>
          </div>
        </div>
      </div>

      <div class="collapsible-section">
        <div v-if="!sidebarCollapsed" class="section-header" @click="toggleHistorySection">
          <ChevronDown :size="16" :class="['chevron-icon', { collapsed: !historySectionExpanded }]" />
          <span class="section-title" @click.stop="goHistoryList">历史对话</span>
        </div>

        <div :class="['section-body', { expanded: historySectionExpanded || sidebarCollapsed }]">
          <div class="history-list">
            <div
              v-for="item in historyList"
              :key="item.id"
              :class="['history-item', { active: isHistoryActive(item.id) }]"
              @click="goHistoryChat(item.id)"
              :title="sidebarCollapsed ? item.title : ''"
            >
              <MessageSquare :size="16" class="history-icon" />
              <span v-if="!sidebarCollapsed" class="history-title">{{ item.title }}</span>
            </div>

            <div v-if="historyList.length === 0 && !sidebarCollapsed" class="empty-history">
              暂无历史记录
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="user-bar">
      <div class="user-bar-left">
        <div
          class="user-avatar-initial"
          @click="toggleUserMenu"
          :title="sidebarCollapsed ? username : ''"
        >
          {{ userInitial }}
        </div>

        <span v-if="!sidebarCollapsed" class="user-bar-name">{{ username }}</span>

        <div v-if="userMenuOpen" class="user-menu" @click.stop>
          <div class="user-menu-item" @click="handleProfile">
            <User :size="16" />
            <span>个人资料</span>
          </div>
          <div class="user-menu-item" @click="handleSettings">
            <Settings :size="16" />
            <span>设置</span>
          </div>
          <div class="user-menu-divider"></div>
          <div class="user-menu-item logout" @click="handleLogout">
            <LogOut :size="16" />
            <span>退出登录</span>
          </div>
        </div>
      </div>

      <div v-if="!sidebarCollapsed" class="user-bar-right">
        <button
          class="mode-toggle"
          :class="{ active: !isDark }"
          @click="toggleDarkMode"
          role="switch"
          :aria-checked="!isDark"
          :title="isDark ? '切换到亮色模式' : '切换到暗色模式'"
        >
          <span class="toggle-track">
            <span class="toggle-thumb">
              <Moon v-if="!isDark" :size="10" class="thumb-icon" />
              <Sun v-else :size="10" class="thumb-icon" />
            </span>
          </span>
        </button>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Layers,
  Plus,
  FolderOpen,
  MessageSquare,
  ChevronDown,
  Moon,
  Sun,
  PanelLeftClose,
  PanelLeftOpen,
  User,
  Settings,
  LogOut,
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useKnowledgeBaseStore } from '@/stores/knowledgeBase'
import { getHistoryPage } from '@/api/history'

interface HistoryItem {
  id: string
  title: string
}

const emit = defineEmits<{
  (e: 'open-settings', tab: 'profile' | 'security' | 'apiKey'): void
}>()

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const kbStore = useKnowledgeBaseStore()

const sidebarCollapsed = ref(false)
const kbSectionExpanded = ref(true)
const historySectionExpanded = ref(true)
const isDark = ref(false)
const userMenuOpen = ref(false)
const historyList = ref<HistoryItem[]>([])
const HISTORY_UPDATED_EVENT = 'rag-history-updated'

const username = computed(() => authStore.userInfo?.username || 'Linsist')
const userInitial = computed(() => username.value.charAt(0).toUpperCase())
const knowledgeBases = computed(() => kbStore.list)
const isNewChatActive = computed(() => route.path === '/chat' || route.path === '/chat-v2')

function isKbActive(id: number): boolean {
  return route.path === `/kb/${id}`
}

function isHistoryActive(id: string): boolean {
  return route.path === `/chat/${id}`
}

function goNewChat() {
  router.push('/chat')
}

function goKbDetail(id: number) {
  router.push(`/kb/${id}`)
}

function goHistoryChat(id: string) {
  router.push(`/chat/${id}`)
}

function goHistoryList() {
  router.push('/history')
}

function toggleKbSection() {
  kbSectionExpanded.value = !kbSectionExpanded.value
}

function toggleHistorySection() {
  historySectionExpanded.value = !historySectionExpanded.value
}

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

function toggleUserMenu() {
  userMenuOpen.value = !userMenuOpen.value
}

function closeUserMenu(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (!target.closest('.user-bar-left')) {
    userMenuOpen.value = false
  }
}

function handleProfile() {
  emit('open-settings', 'profile')
  userMenuOpen.value = false
}

function handleSettings() {
  emit('open-settings', 'security')
  userMenuOpen.value = false
}

function handleLogout() {
  authStore.clearAuth()
  userMenuOpen.value = false
  router.push('/login')
}

function applyTheme(dark: boolean) {
  document.documentElement.classList.toggle('dark', dark)
  document.documentElement.classList.toggle('light', !dark)
}

function toggleDarkMode() {
  isDark.value = !isDark.value
  applyTheme(isDark.value)
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}

async function loadSidebarData() {
  try {
    if (kbStore.list.length === 0) {
      await kbStore.fetchList()
    }
  } catch {
    // ignore sidebar data failures
  }

  try {
    const res = await getHistoryPage(1, 20)
    const records = res.data.data.records
    historyList.value = records.map((item) => ({
      id: String(item.id),
      title: item.question,
    }))
  } catch {
    historyList.value = []
  }
}

function handleHistoryUpdated() {
  loadSidebarData()
}

onMounted(() => {
  const savedTheme = localStorage.getItem('theme')
  if (savedTheme === 'dark') {
    isDark.value = true
  } else if (savedTheme === 'light') {
    isDark.value = false
  } else {
    isDark.value = window.matchMedia('(prefers-color-scheme: dark)').matches
  }
  applyTheme(isDark.value)

  loadSidebarData()
  document.addEventListener('click', closeUserMenu)
  window.addEventListener(HISTORY_UPDATED_EVENT, handleHistoryUpdated)
})

onUnmounted(() => {
  document.removeEventListener('click', closeUserMenu)
  window.removeEventListener(HISTORY_UPDATED_EVENT, handleHistoryUpdated)
})
</script>

<style scoped>
.chat-sidebar {
  flex: 0 0 240px;
  width: 240px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--rag-bg-sidebar);
  border-right: 1px solid var(--rag-border);
  transition: width 0.2s ease;
}

.chat-sidebar.collapsed {
  flex-basis: 64px;
  width: 64px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid var(--rag-border);
  position: relative;
}

.logo-wrapper {
  width: 32px;
  height: 32px;
  background: var(--rag-primary);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.logo-text {
  font-size: 14px;
  font-weight: 600;
  color: var(--rag-text-primary);
  white-space: nowrap;
  overflow: hidden;
}

.collapse-btn {
  margin-left: auto;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--rag-text-secondary);
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.collapse-btn:hover {
  background: var(--rag-bg-hover);
  color: var(--rag-text-primary);
}

.chat-sidebar.collapsed .sidebar-header {
  justify-content: center;
  padding: 16px 8px;
}

.logo-toggle-container {
  position: relative;
  width: 32px;
  height: 32px;
  cursor: pointer;
}

.logo-toggle-container .logo-wrapper {
  position: absolute;
  top: 0;
  left: 0;
  opacity: 1;
  transition: opacity 0.15s ease;
}

.logo-toggle-container .collapse-btn-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 32px;
  height: 32px;
  border: none;
  background: var(--rag-bg-hover);
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--rag-text-secondary);
  opacity: 0;
  transition: opacity 0.15s ease;
}

.logo-toggle-container:hover .logo-wrapper {
  opacity: 0;
}

.logo-toggle-container:hover .collapse-btn-overlay {
  opacity: 1;
  color: var(--rag-text-primary);
}

.new-chat-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin: 12px 16px;
  padding: 10px 16px;
  background: var(--rag-bg-surface);
  border: 1px solid var(--rag-border);
  border-radius: 8px;
  font-size: 14px;
  color: var(--rag-text-primary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.new-chat-btn:hover {
  background: var(--rag-bg-hover);
  border-color: var(--rag-primary);
}

.new-chat-btn.active {
  background: var(--rag-success-light);
  border-color: var(--rag-primary);
  color: var(--rag-primary-dark);
}

.chat-sidebar.collapsed .new-chat-btn {
  margin: 12px 8px;
  padding: 10px;
  justify-content: center;
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.collapsible-section {
  margin-bottom: 4px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s ease;
}

.section-header:hover {
  background: var(--rag-bg-hover);
}

.chevron-icon {
  color: var(--rag-text-secondary);
  transition: transform 0.2s ease;
  flex-shrink: 0;
}

.chevron-icon.collapsed {
  transform: rotate(-90deg);
}

.section-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--rag-text-primary);
}

.section-body {
  height: 0;
  overflow: hidden;
  transition: height 0.2s ease;
}

.section-body.expanded {
  height: auto;
}

.kb-list {
  padding: 0 8px 8px;
}

.kb-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--rag-text-secondary);
}

.kb-item:hover {
  background: var(--rag-bg-hover);
}

.kb-item.active {
  background: var(--rag-success-light);
  color: var(--rag-primary-dark);
}

.kb-icon {
  color: var(--rag-primary);
  flex-shrink: 0;
}

.kb-item.active .kb-icon {
  color: var(--rag-primary-dark);
}

.kb-name {
  flex: 1;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-doc-count {
  font-size: 11px;
  color: var(--rag-text-placeholder);
}

.kb-item.active .kb-doc-count {
  color: var(--rag-primary-dark);
  opacity: 0.7;
}

.chat-sidebar.collapsed .kb-list {
  padding: 0 8px 8px;
}

.chat-sidebar.collapsed .kb-item {
  justify-content: center;
  padding: 10px;
}

.history-list {
  padding: 0 8px 8px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--rag-text-secondary);
}

.history-item:hover {
  background: var(--rag-bg-hover);
}

.history-item.active {
  background: var(--rag-bg-user-msg);
}

.history-icon {
  color: var(--rag-text-placeholder);
  flex-shrink: 0;
}

.history-title {
  flex: 1;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 160px;
}

.empty-history {
  padding: 16px;
  text-align: center;
  font-size: 12px;
  color: var(--rag-text-secondary);
}

.chat-sidebar.collapsed .history-list {
  padding: 0 8px 8px;
}

.chat-sidebar.collapsed .history-item {
  justify-content: center;
  padding: 10px;
}

.user-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  margin-top: auto;
  border-top: 1px solid var(--rag-border);
}

.user-bar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
}

.user-menu {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 0;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  min-width: 160px;
  z-index: 100;
  overflow: hidden;
}

.user-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  font-size: 14px;
  color: #0d0d0d;
  cursor: pointer;
  transition: background 0.2s ease;
}

.user-menu-item:hover {
  background: #f4f4f4;
}

.user-menu-item.logout {
  color: #ef4444;
}

.user-menu-item.logout:hover {
  background: #fef2f2;
}

.user-menu-divider {
  height: 1px;
  background: #e5e5e5;
  margin: 4px 0;
}

.chat-sidebar.collapsed .user-bar {
  justify-content: center;
  padding: 12px 8px;
}

.chat-sidebar.collapsed .user-bar-left {
  gap: 0;
}

.user-avatar-initial {
  width: 32px;
  height: 32px;
  background: var(--rag-text-primary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--rag-bg-surface);
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.user-avatar-initial:hover {
  transform: scale(1.05);
}

.user-bar-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--rag-text-primary);
}

.user-bar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mode-toggle {
  width: 36px;
  height: 20px;
  padding: 0;
  background: transparent;
  border: none;
  cursor: pointer;
}

.toggle-track {
  display: block;
  width: 36px;
  height: 20px;
  background: var(--rag-border);
  border-radius: 10px;
  position: relative;
  transition: background 0.2s ease;
}

.mode-toggle.active .toggle-track {
  background: var(--rag-primary);
}

.toggle-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 16px;
  height: 16px;
  background: var(--rag-bg-surface);
  border-radius: 50%;
  transition: transform 0.2s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumb-icon {
  color: var(--rag-text-secondary);
}

.mode-toggle.active .toggle-thumb {
  transform: translateX(16px);
}

@media (max-width: 768px) {
  .chat-sidebar {
    display: none;
  }
}
</style>
