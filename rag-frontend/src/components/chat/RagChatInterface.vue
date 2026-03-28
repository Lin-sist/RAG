<!-- 
  RAG Chat Interface - 现代企业级 RAG 聊天界面
  Vue 3 SFC + TypeScript + Lucide Icons
  Primary Accent: #10A37F
-->
<template>
  <div class="rag-chat-interface">
    <!-- ========== 左侧边栏 (240px) ========== -->
    <aside class="chat-sidebar">
      <div class="sidebar-header">
        <div class="logo-wrapper">
          <Layers :size="20" />
        </div>
        <span class="logo-text">RAG 智能问答</span>
      </div>

      <!-- 新建对话按钮 -->
      <button class="new-chat-btn" @click="handleNewChat">
        <Plus :size="18" />
        <span>New Chat</span>
      </button>

      <!-- 侧边栏内容区 -->
      <div class="sidebar-content">
        <!-- 知识库（可折叠） -->
        <div class="collapsible-section">
          <div class="section-header" @click="toggleKbSection">
            <ChevronDown :size="16" :class="['chevron-icon', { collapsed: !kbSectionExpanded }]" />
            <span class="section-title">知识库</span>
          </div>
          <div :class="['section-body', { expanded: kbSectionExpanded }]">
            <div class="kb-list">
              <div
                v-for="kb in knowledgeBases"
                :key="kb.id"
                :class="['kb-item', { active: currentKbId === kb.id }]"
                @click="selectKnowledgeBase(kb.id)"
              >
                <FolderOpen :size="16" class="kb-icon" />
                <span class="kb-name">{{ kb.name }}</span>
                <span class="kb-doc-count">{{ kb.docCount }}篇</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 历史对话（可折叠） -->
        <div class="collapsible-section">
          <div class="section-header" @click="toggleHistorySection">
            <ChevronDown :size="16" :class="['chevron-icon', { collapsed: !historySectionExpanded }]" />
            <span class="section-title">历史对话</span>
          </div>
          <div :class="['section-body', { expanded: historySectionExpanded }]">
            <div class="history-list">
              <div
                v-for="item in historyList"
                :key="item.id"
                :class="['history-item', { active: currentHistoryId === item.id }]"
                @click="loadHistory(item.id)"
              >
                <MessageSquare :size="16" class="history-icon" />
                <span class="history-title">{{ item.title }}</span>
              </div>
              <div v-if="historyList.length === 0" class="empty-history">
                暂无历史记录
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部用户信息栏 -->
      <div class="user-bar">
        <div class="user-bar-left">
          <div class="user-avatar-initial">
            {{ userInitial }}
          </div>
          <span class="user-bar-name">{{ props.username }}</span>
        </div>
        <div class="user-bar-right">
          <span class="mode-label">Light Mode</span>
          <button 
            class="mode-toggle" 
            :class="{ active: !isDarkMode }"
            @click="toggleDarkMode"
            role="switch"
            :aria-checked="!isDarkMode"
          >
            <span class="toggle-track">
              <span class="toggle-thumb"></span>
            </span>
          </button>
        </div>
      </div>
    </aside>

    <!-- ========== 主聊天区域 ========== -->
    <main class="chat-main">
      <!-- 消息列表区域 -->
      <div ref="messagesContainer" class="messages-container">
        <!-- 欢迎页面 -->
        <div v-if="messages.length === 0" class="welcome-screen">
          <div class="welcome-icon">
            <Sparkles :size="48" />
          </div>
          <h1 class="welcome-title">有什么可以帮助你的？</h1>
          <p class="welcome-subtitle">基于你的知识库，为你提供精准的智能问答</p>
          
          <div class="example-questions">
            <div
              v-for="(question, idx) in exampleQuestions"
              :key="idx"
              class="example-card"
              @click="sendMessage(question)"
            >
              <span class="example-text">{{ question }}</span>
              <ArrowRight :size="16" />
            </div>
          </div>
        </div>

        <!-- 消息列表 -->
        <template v-else>
          <div
            v-for="msg in messages"
            :key="msg.id"
            :class="['message-wrapper', msg.role]"
          >
            <!-- 用户消息 -->
            <div v-if="msg.role === 'user'" class="user-message">
              <div class="user-content">{{ msg.content }}</div>
            </div>

            <!-- AI 消息 -->
            <div v-else class="ai-message">
              <div class="ai-avatar">
                <span>AI</span>
              </div>
              <div class="ai-body">
                <!-- 加载状态 -->
                <div v-if="msg.loading && !msg.content" class="loading-dots">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>

                <!-- 消息内容 -->
                <div v-else class="ai-content" v-html="renderMarkdown(msg.content)"></div>

                <!-- 流式光标 -->
                <span v-if="msg.loading && msg.content" class="typing-cursor"></span>

                <!-- 操作栏 (hover 显示) -->
                <div v-if="!msg.loading && msg.content" class="message-actions">
                  <button class="action-btn" title="复制" @click="copyToClipboard(msg.content)">
                    <Copy :size="14" />
                  </button>
                  <button class="action-btn" title="重新生成" @click="regenerateMessage(msg.id)">
                    <RefreshCw :size="14" />
                  </button>
                  <button class="action-btn" title="有帮助" @click="thumbUp(msg.id)">
                    <ThumbsUp :size="14" />
                  </button>
                  <button class="action-btn" title="无帮助" @click="thumbDown(msg.id)">
                    <ThumbsDown :size="14" />
                  </button>
                </div>

                <!-- 引用来源卡片 -->
                <div v-if="msg.citations && msg.citations.length > 0 && !msg.loading" class="citations-section">
                  <div class="citations-label">
                    <FileText :size="14" />
                    <span>Sources</span>
                  </div>
                  <div class="citations-grid">
                    <div
                      v-for="(cite, cidx) in msg.citations"
                      :key="cidx"
                      class="citation-card"
                    >
                      <div class="cite-icon">
                        <FileText :size="16" />
                      </div>
                      <div class="cite-info">
                        <span class="cite-filename">{{ cite.source }}</span>
                        <span class="cite-score">{{ Math.round(cite.score * 100) }}% Match</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <div ref="scrollAnchor"></div>
      </div>

      <!-- ========== 悬浮输入框 ========== -->
      <div class="input-wrapper">
        <div class="input-container">
          <!-- 工具栏 -->
          <div class="input-toolbar">
            <div class="toolbar-item">
              <Database :size="14" />
              <select v-model="currentKbId" class="kb-selector">
                <option :value="null" disabled>选择知识库</option>
                <option v-for="kb in knowledgeBases" :key="kb.id" :value="kb.id">
                  {{ kb.name }}
                </option>
              </select>
            </div>
            <div class="toolbar-item">
              <span class="topk-label">Top-K</span>
              <input
                v-model.number="topK"
                type="number"
                min="1"
                max="20"
                class="topk-input"
              />
            </div>
          </div>

          <!-- 输入框 -->
          <div class="input-box">
            <textarea
              ref="inputRef"
              v-model="inputText"
              placeholder="输入你的问题..."
              rows="1"
              @keydown="handleKeydown"
              @input="autoResize"
            ></textarea>
            <button
              :class="['send-btn', { active: canSend }]"
              :disabled="!canSend"
              @click="handleSend"
            >
              <Send :size="18" />
            </button>
          </div>

          <!-- 免责声明 -->
          <p class="disclaimer">AI 可能产生不准确内容，请核对引用来源</p>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, withDefaults, defineProps } from 'vue'

// Props
interface Props {
  username?: string
}

const props = withDefaults(defineProps<Props>(), {
  username: 'Linsist'
})

// Computed
const userInitial = computed(() => {
  return props.username.charAt(0).toUpperCase()
})
import {
  Layers,
  Plus,
  FolderOpen,
  MessageSquare,
  Sparkles,
  ArrowRight,
  Copy,
  RefreshCw,
  ThumbsUp,
  ThumbsDown,
  FileText,
  Send,
  ChevronDown,
  Database,
} from 'lucide-vue-next'
import MarkdownIt from 'markdown-it'

// Types
interface KnowledgeBase {
  id: number
  name: string
  docCount: number
}

interface HistoryItem {
  id: string
  title: string
}

interface Citation {
  source: string
  snippet: string
  score: number
}

interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  loading?: boolean
  citations?: Citation[]
}

// State
const messages = ref<Message[]>([])
const inputText = ref('')
const currentKbId = ref<number | null>(null)
const topK = ref(5)
const isStreaming = ref(false)

// Sidebar collapse state
const kbSectionExpanded = ref(true)
const historySectionExpanded = ref(true)
const currentHistoryId = ref<string | null>(null)
const isDarkMode = ref(false)

const inputRef = ref<HTMLTextAreaElement>()
const messagesContainer = ref<HTMLElement>()
const scrollAnchor = ref<HTMLElement>()

// Mock Data
const knowledgeBases = ref<KnowledgeBase[]>([
  { id: 1, name: '产品文档', docCount: 128 },
  { id: 2, name: '技术手册', docCount: 56 },
  { id: 3, name: '常见问题', docCount: 234 },
])

const historyList = ref<HistoryItem[]>([
  { id: '1', title: '如何配置系统参数？' },
  { id: '2', title: 'API 接口文档查询' },
  { id: '3', title: '故障排查流程' },
])

const exampleQuestions = [
  '这个知识库包含哪些主要内容？',
  '请总结一下最重要的要点',
  '帮我解释一下核心概念',
  '有哪些实际应用场景？',
]

// Markdown renderer
const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  breaks: true,
})

// Computed
const canSend = computed(() => inputText.value.trim().length > 0 && !isStreaming.value && currentKbId.value !== null)

// Methods
function renderMarkdown(content: string): string {
  if (!content) return ''
  return md.render(content)
}

function handleNewChat() {
  messages.value = []
}

function selectKnowledgeBase(id: number) {
  currentKbId.value = id
}

function loadHistory(id: string) {
  currentHistoryId.value = id
  // Load history implementation
  console.log('Load history:', id)
}

function toggleKbSection() {
  kbSectionExpanded.value = !kbSectionExpanded.value
}

function toggleHistorySection() {
  historySectionExpanded.value = !historySectionExpanded.value
}

function toggleDarkMode() {
  isDarkMode.value = !isDarkMode.value
  // Dark mode implementation can be added here
}

function scrollToBottom() {
  nextTick(() => {
    scrollAnchor.value?.scrollIntoView({ behavior: 'smooth' })
  })
}

function autoResize() {
  if (inputRef.value) {
    inputRef.value.style.height = 'auto'
    inputRef.value.style.height = Math.min(inputRef.value.scrollHeight, 150) + 'px'
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

function handleSend() {
  if (!canSend.value) return
  sendMessage(inputText.value.trim())
  inputText.value = ''
  if (inputRef.value) {
    inputRef.value.style.height = 'auto'
  }
}

function sendMessage(text: string) {
  if (!text || !currentKbId.value) return

  // Add user message
  const userMsg: Message = {
    id: `msg_${Date.now()}_user`,
    role: 'user',
    content: text,
  }
  messages.value.push(userMsg)

  // Add AI placeholder
  const aiMsg: Message = {
    id: `msg_${Date.now()}_ai`,
    role: 'assistant',
    content: '',
    loading: true,
  }
  messages.value.push(aiMsg)

  scrollToBottom()

  // Simulate streaming response
  simulateStreamResponse(aiMsg.id)
}

function simulateStreamResponse(msgId: string) {
  isStreaming.value = true
  const responseText = '根据知识库中的相关文档，我为您找到了以下信息：\n\n这是一个模拟的 AI 回复内容。在实际应用中，这里会显示来自 RAG 系统的真实回答，包括从知识库中检索到的相关信息和 AI 的总结分析。\n\n**关键要点：**\n\n1. 第一个要点内容\n2. 第二个要点内容\n3. 第三个要点内容\n\n如需了解更多详情，请参考下方的引用来源。'
  
  let index = 0
  const msg = messages.value.find(m => m.id === msgId)
  if (!msg) return

  const interval = setInterval(() => {
    if (index < responseText.length) {
      msg.content += responseText[index]
      index++
      scrollToBottom()
    } else {
      clearInterval(interval)
      msg.loading = false
      msg.citations = [
        { source: 'product-guide.pdf', snippet: '相关内容片段...', score: 0.98 },
        { source: 'faq-collection.docx', snippet: '相关内容片段...', score: 0.92 },
        { source: 'tech-manual.md', snippet: '相关内容片段...', score: 0.87 },
      ]
      isStreaming.value = false
      scrollToBottom()
    }
  }, 20)
}

function copyToClipboard(text: string) {
  navigator.clipboard.writeText(text).then(() => {
    // Show toast
    console.log('Copied!')
  })
}

function regenerateMessage(id: string) {
  console.log('Regenerate:', id)
}

function thumbUp(id: string) {
  console.log('Thumb up:', id)
}

function thumbDown(id: string) {
  console.log('Thumb down:', id)
}

onMounted(() => {
  // Default select first KB
  if (knowledgeBases.value.length > 0) {
    currentKbId.value = knowledgeBases.value[0].id
  }
})
</script>

<style scoped>
/* ========== CSS Variables ========== */
.rag-chat-interface {
  --rag-primary: #10A37F;
  --rag-bg-ai-msg: transparent;
  --rag-bg-user-msg: #F4F4F4;
  --rag-border: #E5E5E5;
  --rag-text-primary: #0D0D0D;
  --rag-text-secondary: #676767;
  --rag-shadow-md: 0 8px 24px rgba(0, 0, 0, 0.08);

  /* Extended palette */
  --rag-bg-surface: #FFFFFF;
  --rag-bg-sidebar: #F9F9F9;
  --rag-bg-hover: #ECECEC;
  --rag-success-light: rgba(16, 163, 127, 0.1);
}

/* ========== Layout ========== */
.rag-chat-interface {
  display: flex;
  height: 100vh;
  width: 100%;
  background: var(--rag-bg-surface);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  color: var(--rag-text-primary);
}

/* ========== Sidebar (240px) ========== */
.chat-sidebar {
  width: 240px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--rag-bg-sidebar);
  border-right: 1px solid var(--rag-border);
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid var(--rag-border);
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

/* ========== Sidebar Content ========== */
.sidebar-content {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

/* Collapsible Section */
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

/* Knowledge Base List */
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
  background: #f9f9f9;
}

.kb-item.active {
  background: #e6f6f3;
  color: #0d7a60;
}

.kb-icon {
  color: var(--rag-primary);
  flex-shrink: 0;
}

.kb-item.active .kb-icon {
  color: #0d7a60;
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
  color: #b4b4b4;
}

.kb-item.active .kb-doc-count {
  color: #0d7a60;
  opacity: 0.7;
}

/* History List */
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
  background: #f9f9f9;
}

.history-item.active {
  background: #f4f4f4;
}

.history-icon {
  color: #b4b4b4;
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

/* ========== User Bar (Bottom) ========== */
.user-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  margin-top: auto;
  border-top: 1px solid #e5e5e5;
}

.user-bar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar-initial {
  width: 32px;
  height: 32px;
  background: #1a1a1a;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.user-bar-name {
  font-size: 14px;
  font-weight: 500;
  color: #0d0d0d;
}

.user-bar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mode-label {
  font-size: 12px;
  color: var(--rag-text-secondary);
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
  background: #e5e5e5;
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
  background: #ffffff;
  border-radius: 50%;
  transition: transform 0.2s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
}

.mode-toggle.active .toggle-thumb {
  transform: translateX(16px);
}

/* ========== Main Chat Area ========== */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--rag-bg-surface);
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  scroll-behavior: smooth;
}

/* ========== Welcome Screen ========== */
.welcome-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 48px 24px;
  text-align: center;
}

.welcome-icon {
  width: 80px;
  height: 80px;
  background: var(--rag-success-light);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--rag-primary);
  margin-bottom: 24px;
}

.welcome-title {
  font-size: 28px;
  font-weight: 600;
  color: var(--rag-text-primary);
  margin: 0 0 8px;
}

.welcome-subtitle {
  font-size: 14px;
  color: var(--rag-text-secondary);
  margin: 0 0 32px;
  max-width: 400px;
}

.example-questions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  max-width: 480px;
}

.example-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  background: var(--rag-bg-surface);
  border: 1px solid var(--rag-border);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.example-card:hover {
  border-color: var(--rag-primary);
  transform: translateY(-2px);
  box-shadow: var(--rag-shadow-md);
}

.example-text {
  font-size: 14px;
  color: var(--rag-text-primary);
}

.example-card svg {
  color: var(--rag-text-secondary);
  transition: transform 0.2s ease;
}

.example-card:hover svg {
  transform: translateX(4px);
  color: var(--rag-primary);
}

/* ========== Messages ========== */
.message-wrapper {
  padding: 24px 0;
}

.message-wrapper.user {
  background: var(--rag-bg-user-msg);
}

.message-wrapper.assistant {
  background: var(--rag-bg-ai-msg);
}

/* User Message */
.user-message {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  justify-content: flex-end;
}

.user-content {
  background: var(--rag-bg-surface);
  border: 1px solid var(--rag-border);
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.6;
  max-width: 70%;
  word-break: break-word;
}

/* AI Message */
.ai-message {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  gap: 16px;
}

.ai-avatar {
  width: 32px;
  height: 32px;
  background: var(--rag-primary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ai-avatar span {
  color: white;
  font-size: 12px;
  font-weight: 600;
}

.ai-body {
  flex: 1;
  min-width: 0;
  position: relative;
}

.ai-content {
  font-size: 14px;
  line-height: 1.7;
  color: var(--rag-text-primary);
  word-break: break-word;
}

.ai-content :deep(p) {
  margin: 0 0 12px;
}

.ai-content :deep(p:last-child) {
  margin-bottom: 0;
}

.ai-content :deep(strong) {
  font-weight: 600;
}

.ai-content :deep(ul),
.ai-content :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.ai-content :deep(li) {
  margin: 4px 0;
}

.ai-content :deep(code) {
  background: var(--rag-bg-user-msg);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'SF Mono', Consolas, monospace;
  font-size: 13px;
}

/* Loading Dots */
.loading-dots {
  display: flex;
  gap: 4px;
  padding: 8px 0;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  background: var(--rag-text-secondary);
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) { animation-delay: -0.32s; }
.loading-dots span:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* Typing Cursor */
.typing-cursor {
  display: inline-block;
  width: 2px;
  height: 16px;
  background: var(--rag-primary);
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: blink 0.8s step-end infinite;
}

@keyframes blink {
  50% { opacity: 0; }
}

/* Message Actions */
.message-actions {
  display: flex;
  gap: 4px;
  margin-top: 12px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.message-wrapper:hover .message-actions {
  opacity: 1;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px solid var(--rag-border);
  background: var(--rag-bg-surface);
  border-radius: 6px;
  cursor: pointer;
  color: var(--rag-text-secondary);
  transition: all 0.2s ease;
}

.action-btn:hover {
  border-color: var(--rag-primary);
  color: var(--rag-primary);
}

/* ========== Citations ========== */
.citations-section {
  margin-top: 16px;
}

.citations-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--rag-text-secondary);
  margin-bottom: 10px;
}

.citations-grid {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.citation-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: var(--rag-bg-surface);
  border: 1px solid var(--rag-border);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 180px;
}

.citation-card:hover {
  border-color: var(--rag-primary);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.cite-icon {
  width: 32px;
  height: 32px;
  background: var(--rag-bg-user-msg);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--rag-text-secondary);
  flex-shrink: 0;
}

.cite-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.cite-filename {
  font-size: 13px;
  font-weight: 500;
  color: var(--rag-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cite-score {
  font-size: 11px;
  color: var(--rag-primary);
  background: var(--rag-success-light);
  padding: 2px 6px;
  border-radius: 4px;
  width: fit-content;
}

/* ========== Floating Input ========== */
.input-wrapper {
  position: sticky;
  bottom: 0;
  padding: 16px 24px 24px;
  background: linear-gradient(to top, var(--rag-bg-surface) 80%, transparent);
}

.input-container {
  max-width: 800px;
  margin: 0 auto;
  background: var(--rag-bg-surface);
  border: 1px solid var(--rag-border);
  border-radius: 16px;
  padding: 12px 16px;
  box-shadow: var(--rag-shadow-md);
}

/* Toolbar */
.input-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--rag-border);
}

.toolbar-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--rag-text-secondary);
}

.kb-selector {
  border: none;
  background: transparent;
  font-size: 13px;
  color: var(--rag-text-primary);
  cursor: pointer;
  outline: none;
  padding: 4px 8px;
  border-radius: 6px;
}

.kb-selector:hover {
  background: var(--rag-bg-user-msg);
}

.topk-label {
  font-size: 12px;
  font-weight: 500;
}

.topk-input {
  width: 50px;
  padding: 4px 8px;
  border: 1px solid var(--rag-border);
  border-radius: 6px;
  font-size: 13px;
  text-align: center;
  outline: none;
}

.topk-input:focus {
  border-color: var(--rag-primary);
}

/* Input Box */
.input-box {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.input-box textarea {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  font-size: 14px;
  line-height: 1.5;
  font-family: inherit;
  color: var(--rag-text-primary);
  background: transparent;
  min-height: 24px;
  max-height: 150px;
}

.input-box textarea::placeholder {
  color: var(--rag-text-secondary);
}

.send-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 10px;
  background: var(--rag-border);
  color: var(--rag-text-secondary);
  cursor: not-allowed;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.send-btn.active {
  background: var(--rag-primary);
  color: white;
  cursor: pointer;
}

.send-btn.active:hover {
  opacity: 0.9;
}

/* Disclaimer */
.disclaimer {
  text-align: center;
  font-size: 11px;
  color: var(--rag-text-secondary);
  margin: 10px 0 0;
}

/* ========== Responsive ========== */
@media (max-width: 768px) {
  .chat-sidebar {
    display: none;
  }

  .user-content {
    max-width: 85%;
  }

  .citations-grid {
    flex-direction: column;
  }

  .citation-card {
    min-width: auto;
    width: 100%;
  }
}
</style>
