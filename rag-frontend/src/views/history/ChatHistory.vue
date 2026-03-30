<!--
  ChatHistory.vue - 历史记录页面
  复用现有主题系统 (html.dark class toggle)
-->
<template>
  <div class="chat-history-page">
    <!-- Header -->
    <header class="history-header">
      <div class="header-left">
        <Clock :size="28" class="header-icon" />
        <h1 class="header-title">历史记录</h1>
      </div>
      <div class="header-right">
        <!-- 主题切换按钮 -->
        <div class="theme-toggle-group">
          <button
            :class="['theme-btn', { active: !isDark }]"
            @click="setTheme(false)"
            title="亮色模式"
          >
            <Sun :size="16" />
          </button>
          <button
            :class="['theme-btn', { active: isDark }]"
            @click="setTheme(true)"
            title="暗色模式"
          >
            <Moon :size="16" />
          </button>
        </div>
        <button class="clear-all-btn" @click="handleClearAll">
          清空全部
        </button>
      </div>
    </header>

    <!-- Search Bar -->
    <div class="search-bar">
      <Search :size="18" class="search-icon" />
      <input
        v-model="searchQuery"
        type="text"
        class="search-input"
        placeholder="搜索对话..."
      />
    </div>

    <!-- History List -->
    <div class="history-list">
      <template v-for="group in filteredGroups" :key="group.label">
        <div v-if="group.items.length > 0" class="history-group">
          <div class="group-label">{{ group.label }}</div>
          <div
            v-for="item in group.items"
            :key="item.id"
            class="history-item"
            @mouseenter="hoveredItemId = item.id"
            @mouseleave="hoveredItemId = null"
          >
            <MessageSquareText :size="18" class="item-icon" />
            <span class="item-title">{{ item.title }}</span>
            <span class="item-kb-tag">
              <Folder :size="12" />
              {{ item.knowledgeBase }}
            </span>
            <span class="item-time-or-delete">
              <span v-if="hoveredItemId !== item.id" class="item-time">
                {{ item.time }}
              </span>
              <button
                v-else
                class="delete-btn"
                @click.stop="handleDelete(item)"
                title="删除"
              >
                <Trash2 :size="18" />
              </button>
            </span>
          </div>
        </div>
      </template>

      <!-- Empty State -->
      <div v-if="filteredGroups.every(g => g.items.length === 0)" class="empty-state">
        <MessageSquareText :size="48" class="empty-icon" />
        <p>暂无历史记录</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  Clock,
  Sun,
  Moon,
  Search,
  MessageSquareText,
  Folder,
  Trash2
} from 'lucide-vue-next'

interface HistoryItem {
  id: number
  title: string
  knowledgeBase: string
  time: string
}

interface HistoryGroup {
  label: string
  items: HistoryItem[]
}

// Theme state
const isDark = ref(false)

// Search state
const searchQuery = ref('')
const hoveredItemId = ref<number | null>(null)

// Mock data
const historyGroups = ref<HistoryGroup[]>([
  {
    label: '今天',
    items: [
      { id: 1, title: '新功能发布讨论', knowledgeBase: '产品文档库', time: '10分钟前' },
      { id: 2, title: 'API集成问题排查', knowledgeBase: '技术支持', time: '2小时前' }
    ]
  },
  {
    label: '昨天',
    items: [
      { id: 3, title: 'Q3营销策略复盘', knowledgeBase: '市场运营', time: '昨天 15:30' },
      { id: 4, title: '团队协作工具对比', knowledgeBase: '内部资料', time: '昨天 11:45' }
    ]
  },
  {
    label: '过去 7 天',
    items: [
      { id: 5, title: '用户反馈分析报告', knowledgeBase: '客户洞察', time: '5天前' },
      { id: 6, title: '数据安全合规指南', knowledgeBase: '法律法务', time: '6天前' }
    ]
  },
  {
    label: '更早',
    items: [
      { id: 7, title: '年度计划草案', knowledgeBase: '战略规划', time: '2023年12月15日' },
      { id: 8, title: '竞品分析报告V2', knowledgeBase: '市场研究', time: '2023年11月28日' }
    ]
  }
])

// Filtered groups based on search query
const filteredGroups = computed(() => {
  if (!searchQuery.value.trim()) {
    return historyGroups.value
  }
  const query = searchQuery.value.toLowerCase()
  return historyGroups.value.map(group => ({
    ...group,
    items: group.items.filter(item =>
      item.title.toLowerCase().includes(query) ||
      item.knowledgeBase.toLowerCase().includes(query)
    )
  }))
})

// Theme functions
function applyTheme(dark: boolean) {
  document.documentElement.classList.toggle('dark', dark)
  document.documentElement.classList.toggle('light', !dark)
}

function setTheme(dark: boolean) {
  isDark.value = dark
  applyTheme(dark)
  localStorage.setItem('theme', dark ? 'dark' : 'light')
}

// Action handlers
function handleDelete(item: HistoryItem) {
  console.log('delete', item.id)
  // Remove item from mock data
  for (const group of historyGroups.value) {
    const index = group.items.findIndex(i => i.id === item.id)
    if (index !== -1) {
      group.items.splice(index, 1)
      break
    }
  }
}

function handleClearAll() {
  console.log('clear all')
  // Clear all mock data
  for (const group of historyGroups.value) {
    group.items = []
  }
}

onMounted(() => {
  // Restore theme from localStorage
  const savedTheme = localStorage.getItem('theme')
  if (savedTheme === 'dark') {
    isDark.value = true
  } else if (savedTheme === 'light') {
    isDark.value = false
  } else {
    isDark.value = window.matchMedia('(prefers-color-scheme: dark)').matches
  }
  applyTheme(isDark.value)
})
</script>

<style scoped>
.chat-history-page {
  min-height: 100vh;
  background: var(--rag-bg-page);
  padding: 32px 48px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* ========== Header ========== */
.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  color: var(--rag-text-primary);
}

.header-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--rag-text-primary);
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* Theme Toggle Group */
.theme-toggle-group {
  display: flex;
  align-items: center;
  background: var(--rag-bg-hover);
  border-radius: 8px;
  padding: 4px;
}

.theme-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--rag-text-secondary);
  transition: all 0.2s ease;
}

.theme-btn:hover {
  color: var(--rag-text-primary);
}

.theme-btn.active {
  background: var(--rag-bg-surface);
  color: var(--rag-text-primary);
  box-shadow: var(--rag-shadow-sm);
}

/* Clear All Button */
.clear-all-btn {
  padding: 8px 16px;
  background: #EF4444;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.clear-all-btn:hover {
  background: #DC2626;
}

/* ========== Search Bar ========== */
.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--rag-bg-surface);
  border: 1px solid var(--rag-border);
  border-radius: 12px;
  padding: 12px 16px;
  margin-bottom: 24px;
}

.search-icon {
  color: var(--rag-text-placeholder);
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 15px;
  color: var(--rag-text-primary);
  outline: none;
}

.search-input::placeholder {
  color: var(--rag-text-placeholder);
}

/* ========== History List ========== */
.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-group {
  margin-bottom: 8px;
}

.group-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--rag-text-secondary);
  padding: 8px 0;
}

/* History Item */
.history-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: transparent;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.history-item:hover {
  background: var(--rag-bg-hover);
}

.item-icon {
  color: var(--rag-text-secondary);
  flex-shrink: 0;
}

.item-title {
  flex: 1;
  font-size: 15px;
  font-weight: 500;
  color: var(--rag-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Knowledge Base Tag */
.item-kb-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: var(--rag-bg-hover);
  border-radius: 6px;
  font-size: 12px;
  color: var(--rag-text-secondary);
  flex-shrink: 0;
}

/* Time or Delete */
.item-time-or-delete {
  width: 100px;
  display: flex;
  justify-content: flex-end;
  flex-shrink: 0;
}

.item-time {
  font-size: 13px;
  color: var(--rag-text-secondary);
}

.delete-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #EF4444;
  transition: all 0.15s ease;
}

.delete-btn:hover {
  background: rgba(239, 68, 68, 0.1);
}

/* ========== Empty State ========== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  color: var(--rag-text-secondary);
}

.empty-icon {
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state p {
  font-size: 15px;
}

/* ========== Responsive ========== */
@media (max-width: 768px) {
  .chat-history-page {
    padding: 24px 16px;
  }

  .history-item {
    flex-wrap: wrap;
  }

  .item-kb-tag {
    order: 3;
    margin-top: 8px;
  }

  .item-time-or-delete {
    order: 2;
  }
}
</style>
