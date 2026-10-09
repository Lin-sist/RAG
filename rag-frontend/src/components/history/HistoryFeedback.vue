<template>
  <section class="history-feedback" aria-label="这条问答的反馈" :aria-busy="loading || submitting">
    <div class="feedback-heading">这条回答对你有帮助吗？</div>
    <p v-if="feedback" class="feedback-hint">已提交评分：{{ feedback.rating }} / 5</p>
    <p v-if="feedback?.comment" class="feedback-comment">{{ feedback.comment }}</p>
    <p v-if="loading" class="feedback-hint" role="status">正在读取反馈…</p>
    <div v-else-if="loadError" class="feedback-status" role="alert"><span>{{ loadError }}</span><button :disabled="submitting" @click="loadFeedback">重试读取</button></div>
    <div v-else-if="uncertain" class="feedback-status" role="status">
      <p>提交结果待确认。{{ checkedEmpty ? '本次核查未读到反馈，原提交仍可能迟到成功。' : '请先读取原记录反馈，核查期间不会自动重新提交。' }}</p>
      <button :disabled="submitting" @click="loadFeedback">核查提交结果</button>
      <button v-if="checkedEmpty" :disabled="submitting" @click="confirmResubmit">确认再次提交</button>
    </div>
    <div v-else-if="submissionLocked && !feedback" class="feedback-status" role="status"><span>该问答已提交反馈，暂时未读到内容。</span><button @click="loadFeedback">重试读取</button></div>
    <template v-else-if="!feedback && !loading">
      <label class="feedback-label" :for="'feedback-rating-' + historyId">评分</label>
      <select :id="'feedback-rating-' + historyId" v-model.number="rating" :disabled="submitting"><option :value="0">请选择评分</option><option v-for="score in 5" :key="score" :value="score">{{ score }} 分</option></select>
      <label class="feedback-label" :for="'feedback-comment-' + historyId">评论（可选）</label>
      <textarea :id="'feedback-comment-' + historyId" v-model="comment" maxlength="500" :disabled="submitting" rows="3" placeholder="说说哪些地方有帮助，或还需要改进" />
      <button class="submit-feedback" :disabled="rating < 1 || submitting" @click="submit()">{{ submitting ? '提交中…' : '提交反馈' }}</button>
    </template>
    <p v-if="submitError" class="feedback-status" role="alert">{{ submitError }}</p>
  </section>
</template>
<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessageBox } from 'element-plus'
import { getFeedback, submitFeedback } from '@/api/history'
import { normalizeError } from '@/api/errors'
import { useAuthStore } from '@/stores/auth'
import { createReadGate, matchingFeedback, isWriteUncertain } from '@/utils/historyActions'
import type { QAFeedbackDTO } from '@/types/history'
const props = defineProps<{ historyId: number }>()
const auth = useAuthStore(), gate = createReadGate(() => auth.sessionRevision), submitGate = createReadGate(() => auth.sessionRevision)
const loading = ref(true), loadError = ref(''), submitError = ref(''), submitting = ref(false), rating = ref(0), comment = ref('')
const feedback = ref<QAFeedbackDTO | null>(null), submissionLocked = ref(false), uncertain = ref(false), checkedEmpty = ref(false)
async function loadFeedback() {
  const token = gate.begin(), id = props.historyId, userId = auth.userInfo?.id
  loading.value = true; loadError.value = ''
  try {
    const response = await getFeedback(id)
    if (!gate.current(token) || id !== props.historyId) return
    const saved = matchingFeedback(response.data.data, id, userId)
    if (saved) { feedback.value = saved; submissionLocked.value = true; uncertain.value = false; checkedEmpty.value = false }
    else if (uncertain.value) checkedEmpty.value = true
  } catch (error) { if (gate.current(token)) loadError.value = normalizeError(error).message }
  finally { if (gate.current(token)) loading.value = false }
}
async function submit(confirmedRetry = false) {
  if (submitting.value || submissionLocked.value || loading.value || loadError.value || (uncertain.value && !confirmedRetry) || !Number.isInteger(rating.value) || rating.value < 1 || rating.value > 5) return
  submitting.value = true; submitError.value = ''; checkedEmpty.value = false
  const token = submitGate.begin(), id = props.historyId, userId = auth.userInfo?.id
  gate.invalidate()
  try {
    const result = (await submitFeedback(id, { rating: rating.value, comment: comment.value.trim() || undefined })).data.data
    if (!submitGate.current(token) || id !== props.historyId) return
    const saved = matchingFeedback([result], id, userId)
    if (!saved) { uncertain.value = true; submitError.value = '提交响应与当前记录不符，请核查结果。'; await loadFeedback(); return }
    feedback.value = saved; submissionLocked.value = true; uncertain.value = false
    window.dispatchEvent(new Event('rag-feedback-updated'))
    await loadFeedback()
  } catch (error) {
    if (!submitGate.current(token)) return
    const apiError = normalizeError(error)
    if (apiError.errorCode === 'FEEDBACK_001' || apiError.errorCode === 'FEEDBACK_002') { submissionLocked.value = true; uncertain.value = false; await loadFeedback() }
    else if (isWriteUncertain(error)) { uncertain.value = true; submitError.value = ''; await loadFeedback() }
    else { uncertain.value = false; submitError.value = apiError.message }
  } finally { if (submitGate.current(token)) submitting.value = false }
}
async function confirmResubmit(event?: Event) {
  if (submitting.value || !uncertain.value || !checkedEmpty.value) return
  const trigger = event?.currentTarget as HTMLButtonElement | undefined
  submitting.value = true; const token = submitGate.begin(), id = props.historyId
  try {
    await ElMessageBox.confirm('本次核查未读到反馈，但原提交可能仍在处理。确定再次提交同一评分和评论？', '确认再次提交', { confirmButtonText: '再次提交', cancelButtonText: '取消', type: 'warning' })
    if (submitGate.current(token) && id === props.historyId) { submitting.value = false; await submit(true) }
  } catch { /* 取消不发送 POST */ }
  finally { if (submitGate.current(token)) { submitting.value = false; await nextTick(); if (trigger?.isConnected) trigger.focus() } }
}
function reset() {
  gate.invalidate(); submitGate.invalidate(); feedback.value = null; submissionLocked.value = false; uncertain.value = false; checkedEmpty.value = false; submitting.value = false; rating.value = 0; comment.value = ''; submitError.value = ''; loadError.value = ''; loading.value = false
  const token = gate.begin()
  queueMicrotask(() => { if (gate.current(token) && auth.isLoggedIn) void loadFeedback() })
}
watch(() => [props.historyId, auth.sessionRevision], reset, { flush: 'sync' })
onMounted(loadFeedback)
onUnmounted(() => { gate.close(); submitGate.close() })
</script>
<style scoped>
.history-feedback { margin-top: 24px; padding: 20px; border: 1px solid var(--rag-border); border-radius: 12px; color: var(--rag-text-primary); background: var(--rag-bg-surface); } .feedback-heading { font-size: 14px; font-weight: 600; margin-bottom: 12px; } .feedback-hint, .feedback-comment, .feedback-status { margin: 10px 0; font-size: 13px; line-height: 1.7; overflow-wrap: anywhere; } .feedback-comment { white-space: pre-wrap; } .feedback-label { display: block; margin: 14px 0 6px; font-size: 12px; color: var(--rag-text-secondary); } select, textarea { display: block; width: 100%; padding: 10px; border: 1px solid var(--rag-border); border-radius: 8px; background: var(--rag-bg-page); color: var(--rag-text-primary); font: inherit; font-size: 13px; } textarea { resize: vertical; } button { margin-top: 12px; padding: 8px 12px; border: 1px solid var(--rag-border); border-radius: 8px; background: var(--rag-bg-hover); color: var(--rag-text-primary); font: inherit; font-size: 13px; cursor: pointer; } button + button { margin-left: 8px; } .submit-feedback { background: var(--rag-primary); color: white; border-color: var(--rag-primary); } button:disabled { opacity: .5; cursor: not-allowed; } .feedback-status { color: var(--rag-text-secondary); } button:focus-visible, select:focus-visible, textarea:focus-visible { outline: 2px solid var(--rag-primary); outline-offset: 3px; }
</style>
