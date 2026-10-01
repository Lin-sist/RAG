<template>
  <div class="answer-content" :class="{ 'is-error': error }">
    <div ref="body" class="answer-markdown" @click="copyCode" v-html="renderAnswerMarkdown(content)"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { renderAnswerMarkdown } from '@/utils/answerMarkdown'
import { copyText, clipboardMessage } from '@/utils/clipboard'

const props = defineProps<{ content: string; error?: boolean }>()
const body = ref<HTMLElement>()
let copying = false
let revision = 0
watch(() => props.content, () => { revision += 1 })

async function copyCode(event: MouseEvent) {
  const target = event.target instanceof Element ? event.target.closest('button.code-copy') : null
  if (!target || !body.value?.contains(target) || copying) return
  const code = target.closest('.answer-code')?.querySelector('pre code')
  if (!code) return
  const currentRevision = revision
  const resultLabel = target.closest('.answer-code')?.querySelector('.code-copy-result')
  if (!resultLabel) return
  copying = true
  resultLabel.textContent = '正在复制…'
  const result = await copyText(code.textContent || '')
  copying = false
  if (currentRevision === revision) resultLabel.textContent = clipboardMessage(result)
}
</script>

<style scoped>
.answer-content { min-width: 0; color: var(--rag-text-primary); font-size: 15px; line-height: 1.85; overflow-wrap: anywhere; }
.is-error { color: var(--rag-danger); }
.answer-markdown > :deep(:first-child) { margin-top: 0; }
.answer-markdown > :deep(:last-child) { margin-bottom: 0; }
.answer-markdown :deep(p) { margin: 0 0 14px; }
.answer-markdown :deep(h1), .answer-markdown :deep(h2), .answer-markdown :deep(h3) { margin: 24px 0 12px; font-weight: 600; line-height: 1.5; }
.answer-markdown :deep(h1) { font-size: 24px; }
.answer-markdown :deep(h2) { font-size: 20px; }
.answer-markdown :deep(h3) { font-size: 17px; }
.answer-markdown :deep(ul), .answer-markdown :deep(ol) { margin: 12px 0; padding-left: 24px; }
.answer-markdown :deep(li) { margin: 5px 0; }
.answer-markdown :deep(blockquote) { margin: 16px 0; padding: 4px 16px; border-left: 3px solid var(--rag-border); color: var(--rag-text-secondary); }
.answer-markdown :deep(blockquote p:last-child) { margin-bottom: 0; }
.answer-markdown :deep(hr) { border: 0; border-top: 1px solid var(--rag-border); margin: 24px 0; }
.answer-markdown :deep(a) { color: var(--rag-primary); text-decoration: underline; text-underline-offset: 3px; }
.answer-markdown :deep(img) { max-width: 100%; height: auto; }
.answer-markdown :deep(code) { font-family: Consolas, 'SF Mono', monospace; font-size: 13px; border-radius: 4px; background: var(--rag-bg-surface); padding: 2px 5px; }
.answer-markdown :deep(.answer-code) { margin: 16px 0; border: 1px solid var(--rag-border); border-radius: 12px; overflow: hidden; background: var(--rag-bg-surface); }
.answer-markdown :deep(.code-toolbar) { display: flex; gap: 12px; align-items: center; justify-content: space-between; padding: 8px 14px; border-bottom: 1px solid var(--rag-border); color: var(--rag-text-secondary); font-size: 12px; }
.answer-markdown :deep(.code-toolbar span) { min-width: 0; overflow-wrap: anywhere; }
.answer-markdown :deep(.code-copy) { flex: none; border: 0; border-radius: 6px; background: transparent; color: inherit; font: inherit; padding: 6px 8px; cursor: pointer; }
.answer-markdown :deep(.code-copy:hover) { background: var(--rag-bg-hover); color: var(--rag-text-primary); }
.answer-markdown :deep(pre) { overflow-x: auto; margin: 0; padding: 16px; line-height: 1.65; white-space: pre; overflow-wrap: normal; }
.answer-markdown :deep(pre code) { padding: 0; background: transparent; border-radius: 0; }
.answer-markdown :deep(.answer-table) { max-width: 100%; overflow-x: auto; margin: 16px 0; }
.answer-markdown :deep(table) { border-collapse: collapse; min-width: 100%; font-size: 14px; }
.answer-markdown :deep(th), .answer-markdown :deep(td) { padding: 10px 14px; border: 1px solid var(--rag-border); text-align: left; min-width: 120px; }
.answer-markdown :deep(th) { background: var(--rag-bg-surface); font-weight: 600; }
.answer-markdown :deep(button:focus-visible), .answer-markdown :deep([tabindex]:focus-visible), .answer-markdown :deep(a:focus-visible) { outline: 2px solid var(--rag-primary); outline-offset: -2px; }
.answer-markdown :deep(.code-copy-result) { flex: 1; text-align: right; font-size: 11px; }
</style>
