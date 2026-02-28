<!-- 消息组件 — Design V2 (ChatGPT 风格全宽行布局) -->
<template>
  <div :class="['chat-message', msg.role]">
    <div class="message-row">
      <!-- 头像 -->
      <div class="msg-avatar">
        <div v-if="msg.role === 'assistant'" class="avatar-circle ai">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 2L2 7l10 5 10-5-10-5z"/>
            <path d="M2 17l10 5 10-5"/>
            <path d="M2 12l10 5 10-5"/>
          </svg>
        </div>
        <div v-else class="avatar-circle user">
          {{ (username ?? '用').charAt(0).toUpperCase() }}
        </div>
      </div>

      <!-- 内容区 -->
      <div class="msg-body">
        <!-- 加载骨架 -->
        <div v-if="msg.loading && !msg.content" class="loading-skeleton">
          <div class="skeleton-line" />
          <div class="skeleton-line short" />
        </div>

        <!-- 错误状态 -->
        <div v-else-if="msg.error" class="error-content">
          <el-icon><WarningFilled /></el-icon>
          <span>{{ msg.content }}</span>
        </div>

        <!-- Markdown 内容 -->
        <div v-else class="message-content" v-html="renderedContent" @click="handleContentClick" />

        <!-- 流式光标 -->
        <span v-if="msg.loading && msg.content" class="stream-cursor" />

        <!-- 操作栏 (hover 显示) -->
        <div v-if="!msg.loading && msg.content && !msg.error" class="message-actions">
          <button class="action-btn" title="复制" @click="copyContent">
            <el-icon :size="14"><CopyDocument /></el-icon>
          </button>
        </div>

        <!-- 引用来源 -->
        <CitationList
          v-if="msg.role === 'assistant' && msg.citations && msg.citations.length > 0 && !msg.loading"
          :citations="msg.citations"
        />

        <!-- 时间 -->
        <div class="msg-time">{{ formatTime(msg.timestamp) }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { WarningFilled, CopyDocument } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import python from 'highlight.js/lib/languages/python'
import java from 'highlight.js/lib/languages/java'
import sql from 'highlight.js/lib/languages/sql'
import json from 'highlight.js/lib/languages/json'
import xml from 'highlight.js/lib/languages/xml'
import bash from 'highlight.js/lib/languages/bash'
import cssLang from 'highlight.js/lib/languages/css'
import 'highlight.js/styles/atom-one-dark.css'
import CitationList from './CitationList.vue'
import type { ChatMessage } from '@/stores/chat'

hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('js', javascript)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('ts', typescript)
hljs.registerLanguage('python', python)
hljs.registerLanguage('java', java)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('json', json)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('html', xml)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('sh', bash)
hljs.registerLanguage('css', cssLang)

const props = defineProps<{
  msg: ChatMessage
  username?: string
}>()

const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  breaks: true,
  highlight(str: string, lang: string) {
    const langName = lang || 'text'
    let highlighted: string
    if (lang && hljs.getLanguage(lang)) {
      try {
        highlighted = hljs.highlight(str, { language: lang, ignoreIllegals: true }).value
      } catch { highlighted = md.utils.escapeHtml(str) }
    } else {
      highlighted = md.utils.escapeHtml(str)
    }
    return `<div class="code-block-wrapper"><div class="code-block-header"><span class="code-lang">${langName}</span><button class="code-copy-btn">复制</button></div><pre class="hljs"><code>${highlighted}</code></pre></div>`
  },
})

const renderedContent = computed(() => {
  if (!props.msg.content) return ''
  return md.render(props.msg.content)
})

function formatTime(ts: number): string {
  return new Date(ts).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function copyContent() {
  navigator.clipboard.writeText(props.msg.content || '').then(() => {
    ElMessage.success({ message: '已复制', duration: 1500 })
  })
}

function handleContentClick(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (target.classList.contains('code-copy-btn')) {
    const wrapper = target.closest('.code-block-wrapper')
    const code = wrapper?.querySelector('code')
    if (code) {
      navigator.clipboard.writeText(code.textContent || '')
      target.textContent = '已复制!'
      setTimeout(() => { target.textContent = '复制' }, 2000)
    }
  }
}
</script>

<style scoped>
.chat-message {
  transition: background-color 0.2s ease;
}

.chat-message.assistant {
  background: var(--rag-bg-ai-msg);
}

.chat-message.user {
  background: var(--rag-bg-user-msg);
}

.message-row {
  display: flex;
  gap: var(--rag-space-4);
  max-width: 768px;
  margin: 0 auto;
  padding: var(--rag-space-6) var(--rag-space-4);
}

.chat-message.user .message-row {
  flex-direction: row-reverse;
}

.msg-avatar {
  flex-shrink: 0;
}

.avatar-circle {
  width: 32px;
  height: 32px;
  border-radius: var(--rag-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
}

.avatar-circle.ai {
  background: var(--rag-primary);
  color: #fff;
}

.avatar-circle.user {
  background: var(--rag-gradient);
  color: #fff;
}

.msg-body {
  flex: 1;
  min-width: 0;
  position: relative;
}

/* Loading skeleton */
.loading-skeleton {
  display: flex;
  flex-direction: column;
  gap: var(--rag-space-2);
  padding: var(--rag-space-2) 0;
}

.skeleton-line {
  height: 14px;
  background: linear-gradient(90deg, var(--rag-border-light) 25%, var(--rag-border) 50%, var(--rag-border-light) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
  width: 80%;
}

.skeleton-line.short {
  width: 50%;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* Error */
.error-content {
  display: flex;
  align-items: center;
  gap: var(--rag-space-2);
  color: var(--rag-danger);
  font-size: var(--rag-font-body);
}

/* Markdown content */
.message-content {
  font-size: var(--rag-font-body);
  line-height: var(--rag-line-height);
  color: var(--rag-text-primary);
  word-break: break-word;
}

.message-content :deep(p) {
  margin: 0 0 var(--rag-space-2);
}

.message-content :deep(p:last-child) {
  margin-bottom: 0;
}

.message-content :deep(.code-block-wrapper) {
  margin: var(--rag-space-3) 0;
  border-radius: var(--rag-radius-sm);
  overflow: hidden;
}

.message-content :deep(.code-block-header) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--rag-space-2) var(--rag-space-3);
  background: #2d2d2d;
  font-size: var(--rag-font-small);
  color: #abb2bf;
}

.message-content :deep(.code-lang) {
  text-transform: uppercase;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.message-content :deep(.code-copy-btn) {
  background: none;
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #abb2bf;
  font-size: var(--rag-font-small);
  cursor: pointer;
  padding: 2px 8px;
  border-radius: 4px;
  transition: var(--rag-transition);
}

.message-content :deep(.code-copy-btn:hover) {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.message-content :deep(pre.hljs) {
  margin: 0;
  padding: var(--rag-space-4);
  font-size: 13px;
  line-height: 1.5;
  overflow-x: auto;
}

.message-content :deep(code) {
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
}

.message-content :deep(:not(pre) > code) {
  background: rgba(79, 70, 229, 0.08);
  padding: 2px 6px;
  border-radius: 4px;
  color: var(--rag-primary);
  font-size: 13px;
}

.message-content :deep(ul),
.message-content :deep(ol) {
  padding-left: 20px;
  margin: var(--rag-space-1) 0;
}

.message-content :deep(blockquote) {
  border-left: 3px solid var(--rag-primary-light);
  margin: var(--rag-space-2) 0;
  padding: var(--rag-space-1) var(--rag-space-3);
  color: var(--rag-text-secondary);
  background: var(--rag-bg-hover);
  border-radius: 0 var(--rag-radius-sm) var(--rag-radius-sm) 0;
}

.message-content :deep(table) {
  border-collapse: collapse;
  margin: var(--rag-space-2) 0;
  width: 100%;
  font-size: 13px;
}

.message-content :deep(th),
.message-content :deep(td) {
  border: 1px solid var(--rag-border);
  padding: var(--rag-space-2) var(--rag-space-3);
  text-align: left;
}

.message-content :deep(th) {
  background: var(--rag-bg-hover);
  font-weight: 600;
}

.message-content :deep(a) {
  color: var(--rag-primary);
  text-decoration: none;
}

.message-content :deep(a:hover) {
  text-decoration: underline;
}

/* Stream cursor */
.stream-cursor {
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

/* Message actions (hover visible) */
.message-actions {
  display: flex;
  gap: var(--rag-space-1);
  margin-top: var(--rag-space-2);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.chat-message:hover .message-actions {
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
  transition: var(--rag-transition);
}

.action-btn:hover {
  border-color: var(--rag-primary-light);
  color: var(--rag-primary);
  background: var(--rag-bg-hover);
}

/* Time */
.msg-time {
  font-size: 11px;
  color: var(--rag-text-placeholder);
  margin-top: var(--rag-space-2);
}

.chat-message.user .msg-time {
  text-align: right;
}
</style>
