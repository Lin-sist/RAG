<template>
  <section class="my-feedback" aria-label="我的反馈" :aria-busy="loading">
    <div v-if="loading" class="state" role="status">正在读取我的反馈…</div>
    <div v-else-if="error" class="state" role="alert"><p>{{ error }}</p><button @click="load">重试读取反馈</button></div>
    <p v-else-if="!items.length" class="state">你还没有提交过反馈。</p>
    <template v-else>
      <p class="hint">{{ items.length }} 条反馈 · 评分反映个人使用感受</p>
      <article v-for="item in items" :key="item.id" class="feedback-card">
        <div class="meta"><span class="rating" :aria-label="'评分 ' + item.rating + ' 分'">{{ '★'.repeat(Math.max(0, Math.min(5, item.rating))) }} <span>{{ item.rating }} / 5</span></span><time>{{ item.createdAt }}</time></div>
        <RouterLink :to="'/chat/' + item.qaId">{{ titles[item.qaId] || '历史记录 #' + item.qaId }}</RouterLink>
        <p v-if="item.comment" class="comment">{{ item.comment }}</p>
      </article>
    </template>
  </section>
</template>
<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { RouterLink } from 'vue-router'
import { getMyFeedbacks } from '@/api/history'
import { normalizeError } from '@/api/errors'
import { useAuthStore } from '@/stores/auth'
import { createReadGate } from '@/utils/historyActions'
import type { QAFeedbackDTO } from '@/types/history'
defineProps<{ titles: Record<number, string> }>()
const auth = useAuthStore(), gate = createReadGate(() => auth.sessionRevision)
const items = ref<QAFeedbackDTO[]>([]), loading = ref(false), error = ref('')
async function load() {
  const token = gate.begin(); loading.value = true; error.value = ''
  try {
    const result = (await getMyFeedbacks()).data.data
    if (gate.current(token)) items.value = result.filter(item => !auth.userInfo?.id || item.userId === auth.userInfo.id).sort((a, b) => b.createdAt.localeCompare(a.createdAt))
  } catch (cause) { if (gate.current(token)) { items.value = []; error.value = normalizeError(cause).message } }
  finally { if (gate.current(token)) loading.value = false }
}
watch(() => auth.sessionRevision, () => { gate.invalidate(); items.value = []; error.value = ''; loading.value = false; const token = gate.begin(); queueMicrotask(() => { if (gate.current(token) && auth.isLoggedIn) void load() }) }, { flush: 'sync' })
onMounted(() => { window.addEventListener('rag-feedback-updated', load); void load() })
onUnmounted(() => { gate.close(); window.removeEventListener('rag-feedback-updated', load) })
</script>
<style scoped>
.hint, .meta { color: var(--rag-text-secondary); font-size: 12px; line-height: 1.6; } .feedback-card { padding: 20px 0; border-bottom: 1px solid var(--rag-border); } .meta { display: flex; gap: 12px; justify-content: space-between; flex-wrap: wrap; margin-bottom: 10px; } .rating { color: var(--rag-text-primary); letter-spacing: 2px; } .rating span { color: var(--rag-text-secondary); letter-spacing: normal; margin-left: 6px; } a { display: block; color: var(--rag-text-primary); font-size: 14px; line-height: 1.7; text-decoration: none; overflow-wrap: anywhere; } a:hover { text-decoration: underline; } .comment { color: var(--rag-text-secondary); white-space: pre-wrap; overflow-wrap: anywhere; font-size: 13px; line-height: 1.8; margin-bottom: 0; } .state { min-height: 220px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; color: var(--rag-text-secondary); font-size: 14px; } button { border: 1px solid var(--rag-border); background: var(--rag-bg-surface); color: var(--rag-text-primary); padding: 8px 12px; border-radius: 8px; cursor: pointer; } a:focus-visible, button:focus-visible { outline: 2px solid var(--rag-primary); outline-offset: 3px; }
</style>
