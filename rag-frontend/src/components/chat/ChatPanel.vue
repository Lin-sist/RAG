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
      <div class="input-container">
        <button class="attach-btn" title="添加附件">
          <Plus :size="16" />
        </button>

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
import { ref, computed, nextTick } from 'vue'
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
  if (!canSend.value) return
  sendMessage(inputText.value.trim())
  inputText.value = ''
}

function sendMessage(text: string) {
  if (!text) return

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
