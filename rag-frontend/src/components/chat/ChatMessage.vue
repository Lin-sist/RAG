<!-- 消息气泡 — 阶段 7 -->
<template>
  <div :class="['chat-message', msg.role]">
    <!-- 头像 -->
    <div class="avatar">
      <el-avatar v-if="msg.role === 'user'" :size="36" class="user-avatar">
        {{ (username ?? '用').charAt(0).toUpperCase() }}
      </el-avatar>
      <el-avatar v-else :size="36" class="ai-avatar">
        AI
      </el-avatar>
    </div>

    <!-- 消息体 -->
    <div class="bubble-wrapper">
      <div :class="['bubble', msg.role]">
        <!-- 加载状态 -->
        <div v-if="msg.loading && !msg.content" class="loading-dots">
          <span /><span /><span />
        </div>

        <!-- 错误状态 -->
        <div v-else-if="msg.error" class="error-content">
          <el-icon color="#f56c6c"><WarningFilled /></el-icon>
          <span>{{ msg.content }}</span>
        </div>

        <!-- 正常内容：Markdown 渲染 -->
        <div v-else class="message-content" v-html="renderedContent" />

        <!-- 流式光标 -->
        <span v-if="msg.loading && msg.content" class="stream-cursor">▊</span>
      </div>

      <!-- 引用来源 -->
      <CitationList
        v-if="msg.role === 'assistant' && msg.citations && msg.citations.length > 0 && !msg.loading"
        :citations="msg.citations"
      />

      <!-- 消息时间 -->
      <div class="msg-time">{{ formatTime(msg.timestamp) }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { WarningFilled } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import CitationList from './CitationList.vue'
import type { ChatMessage } from '@/stores/chat'

const props = defineProps<{
  msg: ChatMessage
  username?: string
}>()

const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  breaks: true,
})

const renderedContent = computed(() => {
  if (!props.msg.content) return ''
  return md.render(props.msg.content)
})

function formatTime(ts: number): string {
  const d = new Date(ts)
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.chat-message {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  transition: background-color 0.2s;
}

.chat-message:hover {
  background-color: #fafafa;
}

.chat-message.user {
  flex-direction: row-reverse;
}

.avatar {
  flex-shrink: 0;
}

.user-avatar {
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  font-weight: 600;
}

.ai-avatar {
  background: linear-gradient(135deg, #67c23a, #529b2e);
  color: #fff;
  font-weight: 700;
  font-size: 13px;
}

.bubble-wrapper {
  max-width: 75%;
  min-width: 60px;
}

.bubble {
  padding: 10px 16px;
  border-radius: 12px;
  line-height: 1.6;
  word-break: break-word;
  position: relative;
}

.bubble.user {
  background: #409eff;
  color: #fff;
  border-top-right-radius: 4px;
}

.bubble.assistant {
  background: #f4f4f5;
  color: #303133;
  border-top-left-radius: 4px;
}

/* Markdown 内容样式 */
.message-content :deep(p) {
  margin: 0 0 8px;
}

.message-content :deep(p:last-child) {
  margin-bottom: 0;
}

.message-content :deep(pre) {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
  font-size: 13px;
  line-height: 1.5;
}

.message-content :deep(code) {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
}

.message-content :deep(:not(pre) > code) {
  background: rgba(0, 0, 0, 0.06);
  padding: 2px 6px;
  border-radius: 4px;
  color: #c7254e;
}

.bubble.user .message-content :deep(:not(pre) > code) {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.message-content :deep(ul),
.message-content :deep(ol) {
  padding-left: 20px;
  margin: 4px 0;
}

.message-content :deep(blockquote) {
  border-left: 3px solid #dcdfe6;
  margin: 8px 0;
  padding: 4px 12px;
  color: #909399;
}

.message-content :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
  width: 100%;
}

.message-content :deep(th),
.message-content :deep(td) {
  border: 1px solid #dcdfe6;
  padding: 6px 12px;
  text-align: left;
}

.message-content :deep(th) {
  background: #f5f7fa;
  font-weight: 600;
}

.message-content :deep(a) {
  color: #409eff;
  text-decoration: none;
}

.message-content :deep(a:hover) {
  text-decoration: underline;
}

/* 加载动画 */
.loading-dots {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #909399;
  animation: dot-bounce 1.4s ease-in-out infinite;
}

.loading-dots span:nth-child(2) {
  animation-delay: 0.2s;
}

.loading-dots span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes dot-bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

/* 流式光标 */
.stream-cursor {
  animation: blink 0.8s step-end infinite;
  color: #409eff;
  font-weight: bold;
}

@keyframes blink {
  50% { opacity: 0; }
}

/* 错误状态 */
.error-content {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #f56c6c;
}

/* 消息时间 */
.msg-time {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 4px;
  padding: 0 4px;
}

.chat-message.user .msg-time {
  text-align: right;
}
</style>
