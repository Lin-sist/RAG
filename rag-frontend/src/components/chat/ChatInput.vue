<!-- 聊天输入框 — Design V2 (居中悬浮风格) -->
<template>
  <div class="chat-input-wrapper">
    <div class="chat-input-container">
      <!-- 工具条 -->
      <div class="input-toolbar">
        <div class="toolbar-left">
          <el-tooltip content="检索返回的最相关文档片段数量" placement="top">
            <div class="topk-control">
              <span class="topk-label">TopK</span>
              <el-input-number
                v-model="localTopK"
                :min="1"
                :max="20"
                :step="1"
                size="small"
                controls-position="right"
                class="topk-input"
                @change="emit('update:topK', localTopK)"
              />
            </div>
          </el-tooltip>
        </div>
        <div class="toolbar-right">
          <el-switch
            v-model="useStream"
            active-text="流式"
            inactive-text="同步"
            inline-prompt
            size="small"
          />
        </div>
      </div>

      <!-- 输入框 -->
      <div class="input-box">
        <el-input
          ref="inputRef"
          v-model="question"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 6 }"
          placeholder="输入你的问题... (Enter 发送, Shift+Enter 换行)"
          resize="none"
          :disabled="disabled"
          @keydown="handleKeydown"
        />
        <button
          class="send-btn"
          :class="{ active: canSend }"
          :disabled="!canSend"
          @click="handleSend"
        >
          <el-icon :size="18"><Promotion /></el-icon>
        </button>
      </div>

      <!-- 免责声明 -->
      <p class="disclaimer">AI 可能产生不准确内容，请核对引用来源。</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { Promotion } from '@element-plus/icons-vue'
import type { InputInstance } from 'element-plus'

const props = defineProps<{
  disabled?: boolean
  topK?: number
}>()

const emit = defineEmits<{
  send: [question: string, stream: boolean]
  'update:topK': [value: number]
}>()

const inputRef = ref<InputInstance>()
const question = ref('')
const localTopK = ref(props.topK ?? 5)
const useStream = ref(true)

const canSend = computed(() => question.value.trim().length > 0 && !props.disabled)

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

function handleSend() {
  const q = question.value.trim()
  if (!q || props.disabled) return
  emit('send', q, useStream.value)
  question.value = ''
  nextTick(() => inputRef.value?.focus())
}

/** 外部可调用：聚焦输入框 */
function focus() {
  inputRef.value?.focus()
}

defineExpose({ focus })
</script>

<style scoped>
.chat-input-wrapper {
  border-top: 1px solid var(--rag-border-light);
  padding: var(--rag-space-4) var(--rag-space-4) var(--rag-space-2);
  background: var(--rag-bg-surface);
}

.chat-input-container {
  max-width: 768px;
  margin: 0 auto;
}

.input-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--rag-space-2);
  padding: 0 var(--rag-space-1);
}

.topk-control {
  display: flex;
  align-items: center;
  gap: var(--rag-space-2);
}

.topk-label {
  font-size: var(--rag-font-small);
  color: var(--rag-text-secondary);
  white-space: nowrap;
  font-weight: 500;
}

.topk-input {
  width: 100px;
}

.input-box {
  position: relative;
  border: 1.5px solid var(--rag-border);
  border-radius: var(--rag-radius-input);
  background: var(--rag-bg-surface);
  padding: var(--rag-space-2) var(--rag-space-3);
  transition: var(--rag-transition);
}

.input-box:focus-within {
  border-color: var(--rag-primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.input-box :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none !important;
  padding: var(--rag-space-1) 44px var(--rag-space-1) 0;
  line-height: var(--rag-line-height);
  font-size: var(--rag-font-body);
  background: transparent;
  resize: none;
}

.send-btn {
  position: absolute;
  right: var(--rag-space-2);
  bottom: var(--rag-space-2);
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: var(--rag-border);
  color: var(--rag-text-placeholder);
  cursor: not-allowed;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--rag-transition);
}

.send-btn.active {
  background: var(--rag-primary);
  color: #fff;
  cursor: pointer;
}

.send-btn.active:hover {
  background: var(--rag-primary-hover);
}

.disclaimer {
  text-align: center;
  font-size: 11px;
  color: var(--rag-text-placeholder);
  margin: var(--rag-space-2) 0 0;
}
</style>
