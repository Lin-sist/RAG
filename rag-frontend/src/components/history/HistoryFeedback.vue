<template>
  <section class="history-feedback" aria-label="这条问答的反馈">
    <div class="feedback-heading">问答反馈</div>

    <p v-if="loading" class="feedback-hint">正在读取反馈...</p>
    <div v-else-if="loadError" class="feedback-status" role="alert">
      <span>{{ loadError }}</span>
      <button type="button" @click="loadFeedback">重试读取</button>
    </div>
    <template v-else-if="feedback">
      <p class="feedback-hint">已提交评分：{{ feedback.rating }} / 5</p>
      <p v-if="feedback.comment" class="feedback-comment">{{ feedback.comment }}</p>
    </template>
    <div v-else-if="submissionLocked" class="feedback-status" role="status">
      <span>该问答已提交反馈，暂时未能读回内容。</span>
      <button type="button" @click="loadFeedback">重试读取</button>
    </div>
    <template v-else>
      <label class="feedback-label" for="history-feedback-rating">评分</label>
      <select id="history-feedback-rating" v-model.number="rating" :disabled="submitting">
        <option :value="0">请选择</option>
        <option v-for="score in 5" :key="score" :value="score">{{ score }} 分</option>
      </select>
      <label class="feedback-label" for="history-feedback-comment">评论（可选）</label>
      <textarea id="history-feedback-comment" v-model="comment" maxlength="500" :disabled="submitting" rows="3" />
      <button type="button" class="submit-feedback" :disabled="rating < 1 || submitting" @click="submit">
        {{ submitting ? '提交中...' : '提交反馈' }}
      </button>
      <p v-if="submitError" class="feedback-status" role="alert">{{ submitError }}</p>
    </template>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getFeedback, submitFeedback } from '@/api/history'
import { normalizeError } from '@/api/errors'
import type { QAFeedbackDTO } from '@/types/history'

const props = defineProps<{ historyId: number }>()
const loading = ref(true)
const loadError = ref('')
const submitError = ref('')
const submitting = ref(false)
const rating = ref(0)
const comment = ref('')
const feedback = ref<QAFeedbackDTO | null>(null)
const submissionLocked = ref(false)

async function loadFeedback() {
  loading.value = true
  loadError.value = ''
  try {
    const response = await getFeedback(props.historyId)
    const saved = response.data.data[0]
    if (saved) {
      feedback.value = saved
      submissionLocked.value = true
    }
  } catch {
    loadError.value = '反馈读取失败，请重试。'
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (submitting.value || submissionLocked.value || rating.value < 1 || rating.value > 5) return
  submitting.value = true
  submitError.value = ''
  try {
    const response = await submitFeedback(props.historyId, {
      rating: rating.value,
      comment: comment.value.trim() || undefined,
    })
    // POST 已成功时先保存结果。后续 GET 失败也不能再次提交。
    feedback.value = response.data.data
    submissionLocked.value = true
    await loadFeedback()
  } catch (error) {
    const apiError = normalizeError(error)
    if (apiError.errorCode === 'FEEDBACK_001' || apiError.errorCode === 'FEEDBACK_002') {
      submissionLocked.value = true
      await loadFeedback()
    } else {
      submitError.value = apiError.message
    }
  } finally {
    submitting.value = false
  }
}

onMounted(loadFeedback)
</script>

<style scoped>
.history-feedback { margin-top: 20px; padding: 16px; border: 1px solid var(--rag-border); border-radius: 10px; color: var(--rag-text-primary); }
.feedback-heading { font-size: 13px; font-weight: 600; margin-bottom: 10px; }
.feedback-hint, .feedback-comment { margin: 8px 0; font-size: 13px; line-height: 1.5; }
.feedback-label { display: block; margin: 12px 0 6px; font-size: 12px; color: var(--rag-text-secondary); }
select, textarea { display: block; width: 100%; padding: 8px; border: 1px solid var(--rag-border); border-radius: 6px; background: var(--rag-bg-surface); color: var(--rag-text-primary); font: inherit; }
textarea { resize: vertical; }
.submit-feedback, .feedback-status button { margin-top: 12px; padding: 7px 12px; border: 1px solid var(--rag-primary); border-radius: 6px; background: var(--rag-primary); color: white; cursor: pointer; }
.submit-feedback:disabled { opacity: .55; cursor: not-allowed; }
.feedback-status { margin: 8px 0; font-size: 13px; color: var(--rag-text-secondary); }
.feedback-status button { margin: 0 0 0 8px; }
</style>
