<!-- 聊天输入框 — 阶段 7 -->
<template>
  <div class="chat-input">
    <!-- topK 设置 -->
    <div class="input-toolbar">
      <div class="toolbar-left">
        <el-tooltip content="检索返回的最相关文档片段数量" placement="top">
          <div class="topk-control">
            <span class="topk-label">TopK:</span>
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

    <!-- 输入区域 -->
    <div class="input-area">
      <el-input
        ref="inputRef"
        v-model="question"
        type="textarea"
        :rows="2"
        :autosize="{ minRows: 1, maxRows: 4 }"
        placeholder="输入你的问题... (Enter 发送, Shift+Enter 换行)"
        resize="none"
        :disabled="disabled"
        @keydown="handleKeydown"
      />
      <el-button
        type="primary"
        :icon="Promotion"
        :loading="disabled"
        :disabled="!canSend"
        circle
        class="send-btn"
        @click="handleSend"
      />
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
.chat-input {
  border-top: 1px solid var(--el-border-color-lighter);
  padding: 12px 16px;
  background: #fff;
}

.input-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.topk-control {
  display: flex;
  align-items: center;
  gap: 6px;
}

.topk-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.topk-input {
  width: 100px;
}

.input-area {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.input-area :deep(.el-textarea__inner) {
  border-radius: 12px;
  padding: 8px 14px;
  line-height: 1.6;
}

.send-btn {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
}
</style>
