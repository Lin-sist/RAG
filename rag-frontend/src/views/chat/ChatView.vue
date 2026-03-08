<template>
  <div class="chat-view">
    <!-- ========== 左侧：知识库选择面板 ========== -->
    <aside class="kb-panel">
      <div class="kb-panel-header">
        <el-icon><Collection /></el-icon>
        <span>选择知识库</span>
      </div>

      <div v-loading="kbLoading" class="kb-list">
        <div
          v-for="kb in kbList"
          :key="kb.id"
          :class="['kb-item', { active: chatStore.currentKbId === kb.id }]"
          @click="selectKb(kb)"
        >
          <div class="kb-item-name">{{ kb.name }}</div>
          <div class="kb-item-meta">
            <el-tag size="small" :type="kb.isPublic ? 'success' : 'info'" effect="plain">
              {{ kb.isPublic ? '公开' : '私有' }}
            </el-tag>
            <span class="doc-count">{{ kb.documentCount }} 篇</span>
          </div>
        </div>

        <el-empty
          v-if="!kbLoading && kbList.length === 0"
          description="暂无知识库"
          :image-size="60"
        >
          <el-button size="small" type="primary" @click="$router.push('/knowledge-base')">
            去创建
          </el-button>
        </el-empty>
      </div>

      <!-- 清空对话按钮 -->
      <div class="kb-panel-footer">
        <el-button
          text
          type="danger"
          size="small"
          :icon="Delete"
          :disabled="chatStore.messages.length === 0"
          @click="handleClearChat"
        >
          清空对话
        </el-button>
      </div>
    </aside>

    <!-- ========== 右侧：对话区域 ========== -->
    <main class="chat-main">
      <!-- 顶部状态栏 -->
      <div class="chat-header">
        <div class="chat-header-info">
          <template v-if="chatStore.currentKbId">
            <el-icon color="#409eff"><ChatDotRound /></el-icon>
            <span class="chat-kb-name">{{ chatStore.currentKbName }}</span>
          </template>
          <template v-else>
            <el-icon color="#c0c4cc"><ChatDotRound /></el-icon>
            <span class="chat-hint">请先在左侧选择一个知识库</span>
          </template>
        </div>
        <el-tag v-if="chatStore.isStreaming" type="success" effect="dark" size="small" round>
          <el-icon class="streaming-icon"><Loading /></el-icon>
          生成中...
        </el-tag>
      </div>

      <!-- 消息列表 -->
      <div ref="messagesContainer" class="messages-container">
        <!-- 欢迎提示 -->
        <div v-if="chatStore.messages.length === 0" class="welcome-area">
          <div class="welcome-brand">
            <div class="brand-icon">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 2L2 7l10 5 10-5-10-5z"/>
                <path d="M2 17l10 5 10-5"/>
                <path d="M2 12l10 5 10-5"/>
              </svg>
            </div>
            <h2 class="welcome-title">
              {{ chatStore.currentKbId ? '有什么可以帮你的？' : '选择知识库开始对话' }}
            </h2>
            <p class="welcome-subtitle" v-if="chatStore.currentKbId">
              已连接「{{ chatStore.currentKbName }}」
            </p>
          </div>

          <div v-if="chatStore.currentKbId" class="example-grid">
            <div
              v-for="(ex, i) in exampleQuestions"
              :key="i"
              class="example-card"
              @click="sendQuestion(ex, true)"
            >
              <span class="example-emoji">{{ exampleEmojis[i] }}</span>
              <span class="example-text">{{ ex }}</span>
            </div>
          </div>

          <p v-if="!chatStore.currentKbId" class="welcome-hint">
            👈 请先在左侧选择一个知识库，再开始提问
          </p>
        </div>

        <!-- 消息列表 -->
        <ChatMessage
          v-for="msg in chatStore.messages"
          :key="msg.id"
          :msg="msg"
          :username="authStore.userInfo?.username || '用户'"
        />

        <!-- 滚动锚点 -->
        <div ref="scrollAnchor" />
      </div>

      <!-- 输入区域 -->
      <ChatInput
        ref="chatInputRef"
        :disabled="chatStore.isStreaming || !chatStore.currentKbId"
        :top-k="chatStore.topK"
        @send="sendQuestion"
        @update:top-k="chatStore.setTopK"
      />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Collection, ChatDotRound, Delete, Loading } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { useSSE } from '@/composables/useSSE'
import { ask as askApi } from '@/api/qa'
import { listKB } from '@/api/knowledgeBase'
import type { KnowledgeBaseDTO } from '@/types/knowledgeBase'
import ChatMessage from '@/components/chat/ChatMessage.vue'
import ChatInput from '@/components/chat/ChatInput.vue'

const authStore = useAuthStore()
const chatStore = useChatStore()
const sse = useSSE()

const messagesContainer = ref<HTMLElement>()
const scrollAnchor = ref<HTMLElement>()
const chatInputRef = ref<InstanceType<typeof ChatInput>>()

// ---- 知识库列表 ----
const kbList = ref<KnowledgeBaseDTO[]>([])
const kbLoading = ref(false)

const exampleEmojis = ['📖', '✨', '💡', '🔍']
const exampleQuestions = [
  '这个知识库包含哪些主要内容？',
  '请总结一下最重要的要点',
  '帮我解释一下核心概念',
  '有哪些实际应用场景？',
]

onMounted(async () => {
  kbLoading.value = true
  try {
    const res = await listKB()
    kbList.value = res.data.data
  } catch {
    ElMessage.error('获取知识库列表失败')
  } finally {
    kbLoading.value = false
  }
})

// ---- 选择知识库 ----
function selectKb(kb: KnowledgeBaseDTO) {
  if (chatStore.currentKbId === kb.id) return
  // 如果有消息，提示是否清空
  if (chatStore.messages.length > 0) {
    ElMessageBox.confirm('切换知识库将清空当前对话，是否继续？', '切换知识库', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(() => {
      chatStore.clearMessages()
      chatStore.setCurrentKb(kb.id, kb.name)
    }).catch(() => { /* 取消 */ })
  } else {
    chatStore.setCurrentKb(kb.id, kb.name)
  }
}

// ---- 发送问题 ----
async function sendQuestion(question: string, useStream: boolean) {
  if (!chatStore.currentKbId) {
    ElMessage.warning('请先选择一个知识库')
    return
  }

  // 添加用户消息
  chatStore.addMessage({
    role: 'user',
    content: question,
    timestamp: Date.now(),
  })

  // 添加 assistant 占位消息
  chatStore.addMessage({
    role: 'assistant',
    content: '',
    loading: true,
    timestamp: Date.now(),
  })

  scrollToBottom()

  if (useStream) {
    await doStreamAsk(question)
  } else {
    await doSyncAsk(question)
  }

  scrollToBottom()
}

// ---- 同步问答 ----
async function doSyncAsk(question: string) {
  try {
    const res = await askApi({
      kbId: chatStore.currentKbId!,
      question,
      topK: chatStore.topK,
    })
    const qaResp = res.data.data
    chatStore.updateLastAssistantMessage(qaResp.answer)
    chatStore.setLastAssistantMeta(qaResp.citations, qaResp.contexts)
    chatStore.setLastAssistantLoading(false)
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '请求失败，请稍后重试'
    chatStore.setLastAssistantError(msg)
  }
}

// ---- 流式问答 ----
async function doStreamAsk(question: string) {
  chatStore.setStreaming(true)

  const body = {
    kbId: chatStore.currentKbId!,
    question,
    topK: chatStore.topK,
  }

  await sse.connect('/api/qa/ask/stream', body, (chunk: string) => {
    chatStore.appendToLastAssistant(chunk)
    scrollToBottom()
  })

  // 流结束
  chatStore.setLastAssistantLoading(false)
  chatStore.setStreaming(false)

  if (sse.error.value) {
    // 如果流整体失败且没有收到任何内容
    const last = chatStore.messages[chatStore.messages.length - 1]
    if (last && last.role === 'assistant' && !last.content) {
      chatStore.setLastAssistantError(sse.error.value)
    }
  }
}

// ---- 清空对话 ----
function handleClearChat() {
  ElMessageBox.confirm('确定清空所有对话记录？', '清空对话', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    chatStore.clearMessages()
  }).catch(() => { /* 取消 */ })
}

// ---- 自动滚动到底部 ----
function scrollToBottom() {
  nextTick(() => {
    scrollAnchor.value?.scrollIntoView({ behavior: 'smooth' })
  })
}

// 监听消息变化，自动滚动
watch(() => chatStore.messages.length, () => {
  scrollToBottom()
})
</script>

<style scoped>
.chat-view {
  display: flex;
  height: calc(100vh - 100px);
  background: var(--rag-bg-surface);
  border-radius: var(--rag-radius-md);
  overflow: hidden;
  box-shadow: var(--rag-shadow-sm);
}

/* ========== 左侧知识库面板 ========== */
.kb-panel {
  width: 240px;
  border-right: 1px solid var(--rag-border);
  display: flex;
  flex-direction: column;
  background: var(--rag-bg-page);
  flex-shrink: 0;
}

.kb-panel-header {
  display: flex;
  align-items: center;
  gap: var(--rag-space-2);
  padding: var(--rag-space-4);
  font-size: var(--rag-font-body);
  font-weight: 600;
  color: var(--rag-text-primary);
  border-bottom: 1px solid var(--rag-border);
}

.kb-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--rag-space-2);
}

.kb-item {
  padding: var(--rag-space-3);
  border-radius: var(--rag-radius-sm);
  cursor: pointer;
  transition: var(--rag-transition);
  margin-bottom: var(--rag-space-1);
}

.kb-item:hover {
  background: var(--rag-bg-hover);
}

.kb-item.active {
  background: var(--rag-primary);
  color: #fff;
}

.kb-item-name {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: var(--rag-space-1);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-item-meta {
  display: flex;
  align-items: center;
  gap: var(--rag-space-2);
  font-size: 11px;
}

.kb-item.active .kb-item-meta {
  color: rgba(255, 255, 255, 0.85);
}

.kb-item.active .doc-count {
  color: rgba(255, 255, 255, 0.85);
}

.doc-count {
  color: var(--rag-text-secondary);
  font-size: 11px;
}

.kb-panel-footer {
  padding: var(--rag-space-2) var(--rag-space-3);
  border-top: 1px solid var(--rag-border);
}

/* ========== 右侧对话区域 ========== */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--rag-bg-page);
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--rag-space-3) var(--rag-space-6);
  border-bottom: 1px solid var(--rag-border);
  flex-shrink: 0;
  background: var(--rag-bg-surface);
}

.chat-header-info {
  display: flex;
  align-items: center;
  gap: var(--rag-space-2);
  font-size: var(--rag-font-body);
}

.chat-kb-name {
  font-weight: 600;
  color: var(--rag-text-primary);
}

.chat-hint {
  color: var(--rag-text-placeholder);
}

.streaming-icon {
  animation: spin 1.5s linear infinite;
  margin-right: var(--rag-space-1);
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ========== 消息列表 ========== */
.messages-container {
  flex: 1;
  overflow-y: auto;
  scroll-behavior: smooth;
}

/* ========== 欢迎区域 ========== */
.welcome-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: var(--rag-space-12) var(--rag-space-6);
  text-align: center;
}

.welcome-brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: var(--rag-space-8);
}

.brand-icon {
  width: 72px;
  height: 72px;
  border-radius: var(--rag-radius-lg);
  background: var(--rag-gradient);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--rag-space-4);
  box-shadow: 0 8px 32px rgba(79, 70, 229, 0.3);
}

.welcome-title {
  font-size: var(--rag-font-h1);
  font-weight: 600;
  color: var(--rag-text-primary);
  margin: 0 0 var(--rag-space-2);
}

.welcome-subtitle {
  font-size: var(--rag-font-body);
  color: var(--rag-text-secondary);
  margin: 0;
}

.welcome-hint {
  font-size: var(--rag-font-body);
  color: var(--rag-text-placeholder);
  max-width: 400px;
  line-height: var(--rag-line-height);
}

/* 示例卡片网格 */
.example-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--rag-space-3);
  max-width: 560px;
  width: 100%;
}

.example-card {
  display: flex;
  align-items: center;
  gap: var(--rag-space-3);
  padding: var(--rag-space-4);
  background: var(--rag-bg-surface);
  border: 1px solid var(--rag-border);
  border-radius: var(--rag-radius-md);
  cursor: pointer;
  transition: var(--rag-transition);
  text-align: left;
}

.example-card:hover {
  border-color: var(--rag-primary-light);
  box-shadow: var(--rag-shadow-sm);
  transform: translateY(-1px);
}

.example-emoji {
  font-size: 20px;
  flex-shrink: 0;
}

.example-text {
  font-size: 13px;
  color: var(--rag-text-regular);
  line-height: 1.4;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .kb-panel {
    display: none;
  }

  .example-grid {
    grid-template-columns: 1fr;
  }
}
</style>
