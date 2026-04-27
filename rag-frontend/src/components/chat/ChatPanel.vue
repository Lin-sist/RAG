<template>
  <main class="chat-panel">
    <div class="messages-container">
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

      <template v-else>
        <div
          v-for="msg in messages"
          :key="msg.id"
          :class="['message-wrapper', msg.role]"
        >
          <div v-if="msg.role === 'user'" class="user-message">
            <div class="user-content">{{ msg.content }}</div>
          </div>

          <div v-else class="ai-message">
            <div class="ai-avatar">
              <span>AI</span>
            </div>
            <div class="ai-body">
              <div v-if="msg.loading && !msg.content" class="loading-dots">
                <span></span>
                <span></span>
                <span></span>
              </div>

              <div v-else class="ai-content" v-html="renderMarkdown(msg.content)"></div>

              <span v-if="msg.loading && msg.content" class="typing-cursor"></span>

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
                      <span v-if="cite.snippet" class="cite-snippet">{{ cite.snippet }}</span>
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

    <div class="input-wrapper">
      <div
        v-if="selectedKb"
        style="max-width: 720px; width: 100%; margin-bottom: 8px; display: flex; justify-content: flex-start;"
      >
        <div
          style="display: inline-flex; align-items: center; gap: 8px; padding: 4px 10px; border: 1px solid var(--rag-border); border-radius: 999px; background: var(--rag-bg-surface); color: var(--rag-text-secondary); font-size: 12px;"
        >
          <span>知识库：{{ selectedKb.name }}</span>
          <button
            type="button"
            title="取消选择"
            @click="clearSelectedKb"
            style="border: none; background: transparent; color: var(--rag-text-secondary); cursor: pointer; font-size: 14px; line-height: 1; padding: 0;"
          >
            x
          </button>
        </div>
      </div>

      <div class="input-container">
        <div ref="kbDropdownRoot" style="position: relative; display: flex;">
          <button class="attach-btn" title="选择知识库" @click.stop="toggleKbDropdown">
            <Plus :size="16" />
          </button>
          <div
            v-if="kbDropdownOpen"
            @click.stop
            style="position: absolute; left: 0; bottom: calc(100% + 8px); z-index: 30; min-width: 280px; max-height: 280px; overflow-y: auto; border: 1px solid var(--rag-border); border-radius: 12px; background: var(--rag-bg-surface); box-shadow: var(--rag-shadow-md); padding: 8px;"
          >
            <div
              v-if="kbList.length === 0"
              style="padding: 10px 12px; color: var(--rag-text-secondary); font-size: 13px;"
            >
              暂无知识库
            </div>
            <button
              v-for="kb in kbList"
              :key="kb.id"
              type="button"
              @click="selectKbFromDropdown(kb)"
              :style="{
                width: '100%',
                textAlign: 'left',
                border: 'none',
                borderRadius: '8px',
                padding: '8px 10px',
                background: selectedKbId === kb.id ? 'var(--rag-bg-user-msg)' : 'transparent',
                cursor: 'pointer',
                marginBottom: '4px',
              }"
            >
              <div
                :style="{
                  fontSize: '13px',
                  color: 'var(--rag-text-primary)',
                  fontWeight: selectedKbId === kb.id ? '600' : '500',
                  lineHeight: '1.4',
                }"
              >
                {{ kb.name }}
              </div>
              <div style="font-size: 12px; color: var(--rag-text-secondary); margin-top: 2px;">
                {{ kb.documentCount }} 篇文档
              </div>
            </button>
          </div>
        </div>

        <input
          v-model="inputText"
          type="text"
          class="pill-input"
          placeholder="Ask about company knowledge..."
          @keydown="handleKeydown"
        />

        <button class="mic-btn" title="语音输入">
          <Mic :size="18" />
        </button>
        <button
          :class="['send-btn-pill', { active: canSend }]"
          :disabled="!canSend"
          @click="handleSend"
        >
          <ArrowUp :size="18" />
        </button>
      </div>

      <p class="disclaimer">AI 可能产生不准确内容，请核对引用来源</p>
    </div>
  </main>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Sparkles,
  ArrowRight,
  Copy,
  RefreshCw,
  ThumbsUp,
  ThumbsDown,
  FileText,
  Mic,
  ArrowUp,
  Plus,
} from 'lucide-vue-next'
import MarkdownIt from 'markdown-it'
import { getHistoryById } from '@/api/history'
import { useSSE } from '@/composables/useSSE'
import { useChatStore } from '@/stores/chat'
import { useKnowledgeBaseStore } from '@/stores/knowledgeBase'
import type { QAHistoryDTO } from '@/types/history'
import type { KnowledgeBaseDTO } from '@/types/knowledgeBase'

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

const messages = ref<Message[]>([])
const inputText = ref('')
const isStreaming = ref(false)
const scrollAnchor = ref<HTMLElement>()
const kbDropdownRoot = ref<HTMLElement>()
const kbDropdownOpen = ref(false)
const selectedKbId = ref<number | null>(null)
const route = useRoute()
const historyLoading = ref(false)
const historyLoadError = ref('')
let historyLoadSeq = 0
const sse = useSSE()
const chatStore = useChatStore()
const kbStore = useKnowledgeBaseStore()

const exampleQuestions = [
  '这个知识库包含哪些主要内容？',
  '请总结一下最重要的要点',
  '帮我解释一下核心概念',
  '有哪些实际应用场景？',
]

const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  breaks: true,
})

const canSend = computed(() => inputText.value.trim().length > 0 && !isStreaming.value)
const kbList = computed(() => kbStore.list)
const selectedKb = computed(() => kbList.value.find(kb => kb.id === selectedKbId.value) ?? null)

async function loadKbListIfNeeded() {
  if (kbStore.list.length > 0) return
  try {
    await kbStore.fetchList()
  } catch {
    // keep panel usable even if dropdown list loading fails
  }
}

function parseRouteHistoryId(): number | null {
  const raw = route.params.id

  if (Array.isArray(raw)) {
    return null
  }

  const id = Number(raw)
  return Number.isInteger(id) && id > 0 ? id : null
}

function normalizeCitations(citations: QAHistoryDTO['citations'] | null | undefined = []): Citation[] {
  return (citations ?? []).map((cite) => {
    const citeWithScore = cite as unknown as { score?: number; relevanceScore?: number }
    const rawScore = citeWithScore.score ?? citeWithScore.relevanceScore

    return {
      source: cite.source || '未知来源',
      snippet: cite.snippet || '',
      score: typeof rawScore === 'number' && Number.isFinite(rawScore) ? rawScore : 1,
    }
  })
}

function buildMessagesFromHistory(record: QAHistoryDTO): Message[] {
  return [
    {
      id: `history_${record.id}_user`,
      role: 'user',
      content: record.question || '历史问题为空',
    },
    {
      id: `history_${record.id}_assistant`,
      role: 'assistant',
      content: record.answer || '历史回答为空',
      loading: false,
      citations: normalizeCitations(record.citations),
    },
  ]
}

async function loadHistorySession(id: number) {
  const seq = ++historyLoadSeq

  historyLoading.value = true
  historyLoadError.value = ''

  messages.value = [
    {
      id: `history_${id}_loading`,
      role: 'assistant',
      content: '',
      loading: true,
    },
  ]

  try {
    const res = await getHistoryById(id)
    const record = res.data.data

    if (seq !== historyLoadSeq) return

    messages.value = buildMessagesFromHistory(record)
    selectedKbId.value = record.kbId ?? null

    await loadKbListIfNeeded()
    scrollToBottom()
  } catch {
    if (seq !== historyLoadSeq) return

    historyLoadError.value = '历史记录加载失败'
    messages.value = [
      {
        id: `history_${id}_error`,
        role: 'assistant',
        content: '历史记录加载失败，请返回历史记录页重试。',
        loading: false,
      },
    ]
    ElMessage.error('历史记录加载失败')
  } finally {
    if (seq === historyLoadSeq) {
      historyLoading.value = false
    }
  }
}

function resetNewChat() {
  historyLoadSeq += 1
  historyLoading.value = false
  historyLoadError.value = ''
  messages.value = []
  selectedKbId.value = null
}

function toggleKbDropdown() {
  kbDropdownOpen.value = !kbDropdownOpen.value
}

function selectKbFromDropdown(kb: KnowledgeBaseDTO) {
  selectedKbId.value = kb.id
  kbDropdownOpen.value = false
}

function clearSelectedKb() {
  selectedKbId.value = null
}

function handleOutsideClick(event: MouseEvent) {
  if (!kbDropdownOpen.value) return
  const target = event.target as Node | null
  if (!target) return
  if (kbDropdownRoot.value?.contains(target)) return
  kbDropdownOpen.value = false
}

function renderMarkdown(content: string): string {
  if (!content) return ''
  return md.render(content)
}

function scrollToBottom() {
  nextTick(() => {
    scrollAnchor.value?.scrollIntoView({ behavior: 'smooth' })
  })
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

function handleSend() {
  if (historyLoading.value) return
  if (historyLoadError.value) {
    historyLoadError.value = ''
  }
  if (!canSend.value) return
  sendMessage(inputText.value.trim())
  inputText.value = ''
}

function sendMessage(text: string) {
  if (!text) return
  const effectiveKbId = selectedKbId.value ?? chatStore.currentKbId
  if (!effectiveKbId) {
    ElMessage.warning('请先选择一个知识库')
    return
  }

  const userMsg: Message = {
    id: `msg_${Date.now()}_user`,
    role: 'user',
    content: text,
  }
  messages.value.push(userMsg)

  const aiMsg: Message = {
    id: `msg_${Date.now()}_ai`,
    role: 'assistant',
    content: '',
    loading: true,
  }
  messages.value.push(aiMsg)

  scrollToBottom()
  simulateStreamResponse(aiMsg.id, text, effectiveKbId)
}

async function simulateStreamResponse(msgId: string, question: string, kbId: number) {
  isStreaming.value = true
  const msg = messages.value.find(m => m.id === msgId)
  if (!msg) {
    isStreaming.value = false
    return
  }

  try {
    await sse.connect(
      '/api/qa/ask/stream',
      {
        kbId,
        question,
        topK: chatStore.topK,
      },
      (chunk: string) => {
        msg.content += chunk
        scrollToBottom()
      },
    )
  } finally {
    msg.loading = false
    if (sse.error.value && !msg.content) {
      msg.content = sse.error.value
    }
    isStreaming.value = false
    scrollToBottom()
  }
}

function copyToClipboard(text: string) {
  navigator.clipboard.writeText(text).then(() => {
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

watch(
  () => route.fullPath,
  async () => {
    const historyId = parseRouteHistoryId()

    if (route.name === 'ChatSession' && historyId) {
      await loadHistorySession(historyId)
      return
    }

    if (route.name === 'ChatSession') {
      resetNewChat()
      return
    }

    if (route.name === 'Chat' || route.name === 'ChatV2') {
      resetNewChat()
    }
  },
  { immediate: true },
)

onMounted(async () => {
  await loadKbListIfNeeded()
  document.addEventListener('click', handleOutsideClick)
})

onUnmounted(() => {
  document.removeEventListener('click', handleOutsideClick)
})
</script>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0;
  background: var(--rag-bg-surface);
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  scroll-behavior: smooth;
}

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

.message-wrapper {
  padding: 24px 0;
}

.message-wrapper.user {
  background: var(--rag-bg-user-msg);
}

.message-wrapper.assistant {
  background: var(--rag-bg-ai-msg);
}

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

.cite-snippet {
  font-size: 12px;
  color: var(--rag-text-secondary);
  line-height: 1.4;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.cite-score {
  font-size: 11px;
  color: var(--rag-primary);
  background: var(--rag-success-light);
  padding: 2px 6px;
  border-radius: 4px;
  width: fit-content;
}

.input-wrapper {
  position: sticky;
  bottom: 0;
  padding: 16px 24px 24px;
  background: linear-gradient(to top, var(--rag-bg-surface) 80%, transparent);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.input-container {
  display: flex;
  align-items: center;
  gap: 12px;
  max-width: 720px;
  width: 100%;
  background: var(--rag-bg-input);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid var(--rag-border);
  border-radius: 9999px;
  padding: 12px 16px;
  box-shadow: var(--rag-shadow-sm);
}

.attach-btn {
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 50%;
  background: var(--rag-border);
  color: var(--rag-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.attach-btn:hover {
  background: var(--rag-bg-hover);
  color: var(--rag-text-regular);
}

.pill-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 15px;
  line-height: 1.4;
  font-family: inherit;
  color: var(--rag-text-primary);
  background: transparent;
}

.pill-input::placeholder {
  color: var(--rag-text-placeholder);
}

.mic-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--rag-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.mic-btn:hover {
  color: var(--rag-text-regular);
  background: rgba(0, 0, 0, 0.05);
}

.send-btn-pill {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: var(--rag-border);
  color: var(--rag-text-placeholder);
  cursor: not-allowed;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.send-btn-pill.active {
  background: var(--rag-text-primary);
  color: var(--rag-bg-primary);
  cursor: pointer;
}

.send-btn-pill.active:hover {
  background: var(--rag-bg-hover);
}

.disclaimer {
  text-align: center;
  font-size: 11px;
  color: var(--rag-text-placeholder);
  margin-top: 10px;
}

@media (max-width: 768px) {
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
