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
          <div class="welcome-icon">🤖</div>
          <h3 class="welcome-title">RAG 智能问答</h3>
          <p class="welcome-desc">
            {{ chatStore.currentKbId
              ? `已连接「${chatStore.currentKbName}」，请输入你的问题`
              : '👈 请先在左侧选择一个知识库，再开始提问'
            }}
          </p>
          <div v-if="chatStore.currentKbId" class="welcome-examples">
            <p class="example-label">💡 你可以尝试问：</p>
            <el-button
              v-for="(ex, i) in exampleQuestions"
              :key="i"
              text
              type="primary"
              class="example-btn"
              @click="sendQuestion(ex, true)"
            >
              {{ ex }}
            </el-button>
          </div>
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

const exampleQuestions = [
  '这个知识库包含哪些主要内容？',
  '请总结一下最重要的要点',
  '帮我解释一下核心概念',
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
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

/* ========== 左侧知识库面板 ========== */
.kb-panel {
  width: 240px;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  background: #fafbfc;
  flex-shrink: 0;
}

.kb-panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #ebeef5;
}

.kb-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.kb-item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
}

.kb-item:hover {
  background: #ecf5ff;
}

.kb-item.active {
  background: #409eff;
  color: #fff;
}

.kb-item-name {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 4px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-item-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
}

.kb-item.active .kb-item-meta {
  color: rgba(255, 255, 255, 0.85);
}

.kb-item.active .doc-count {
  color: rgba(255, 255, 255, 0.85);
}

.doc-count {
  color: #909399;
  font-size: 11px;
}

.kb-panel-footer {
  padding: 8px 12px;
  border-top: 1px solid #ebeef5;
}

/* ========== 右侧对话区域 ========== */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border-bottom: 1px solid #ebeef5;
  flex-shrink: 0;
}

.chat-header-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.chat-kb-name {
  font-weight: 600;
  color: #303133;
}

.chat-hint {
  color: #c0c4cc;
}

.streaming-icon {
  animation: spin 1.5s linear infinite;
  margin-right: 4px;
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
  padding: 40px 20px;
  text-align: center;
}

.welcome-icon {
  font-size: 56px;
  margin-bottom: 16px;
}

.welcome-title {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px;
}

.welcome-desc {
  font-size: 14px;
  color: #909399;
  max-width: 400px;
  line-height: 1.6;
}

.welcome-examples {
  margin-top: 24px;
}

.example-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 12px;
}

.example-btn {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
}
</style>
