<template>
  <main class="history-page">
    <header class="page-heading"><div><span class="eyebrow">你的问答与反馈</span><h1>历史记录</h1><p>每条记录保存一次提问和回答，可随时回看。</p></div><button class="refresh" :disabled="loading || busy" @click="refresh">刷新</button></header>
    <nav class="history-tabs" aria-label="历史分类"><button :aria-pressed="tab === 'records'" @click="tab = 'records'">问答记录</button><button :aria-pressed="tab === 'feedback'" @click="tab = 'feedback'">我的反馈</button></nav>
    <MyFeedbackList v-if="tab === 'feedback'" :key="auth.sessionRevision" :titles="titles" />
    <section v-else aria-label="问答记录">
      <div class="toolbar">
        <label class="find"><Search :size="17" /><input v-model="searchQuery" aria-label="筛选本页问题或知识库 ID" placeholder="筛选本页问题或知识库 ID" /><button v-if="searchQuery" aria-label="清除筛选" @click="searchQuery = ''">×</button></label>
        <label class="scope">知识库 ID<input :value="filterKbId" type="number" min="1" placeholder="全部" :disabled="busy" @change="changeScope(($event.target as HTMLInputElement).value)" /></label>
        <button class="batch-button" :disabled="loading || busy" @click="handleClearAll">批量删除历史</button>
      </div>
      <p class="scope-note">仅筛选本页已加载记录 · {{ filteredCount }} / {{ records.length }} 条<span v-if="kbError"> · 知识库名称暂不可用，按 ID 展示</span></p>
      <p v-if="actionMessage" class="action-message" role="status">{{ actionMessage }}</p>
      <div v-if="preparing" class="action-message" role="status">正在读取全部知识库的删除目标快照… <button @click="cancelPreparation">停止读取</button></div>
      <div class="history-list" :aria-busy="loading">
        <div v-if="loading" class="state-panel" role="status">正在加载历史记录…</div>
        <div v-else-if="loadError" class="state-panel" role="alert"><p>{{ loadError }}</p><button @click="loadHistoryGroups()">重试读取</button></div>
        <template v-else>
          <section v-for="group in filteredGroups.filter(group => group.items.length)" :key="group.label" class="history-group">
            <h2>{{ group.label }}</h2>
            <div v-for="item in group.items" :key="item.id" class="history-row">
              <RouterLink :to="'/chat/' + item.id" class="record-link"><MessageSquareText :size="18" /><span class="record-title">{{ item.title }}</span><span class="record-meta">{{ kbLabel(item.id) }}<time>{{ item.time }}</time></span></RouterLink>
              <button class="delete-button" :disabled="busy" :aria-label="'删除历史记录 #' + item.id" @click="handleDelete(item, $event)"><Trash2 :size="17" /></button>
            </div>
          </section>
          <div v-if="!filteredCount" class="state-panel"><MessageSquareText :size="32" /><p>{{ searchQuery.trim() ? '本页没有匹配的记录，其他页可继续查看。' : '此范围暂无历史问答记录。' }}</p><button v-if="searchQuery" @click="searchQuery = ''">清除筛选</button></div>
        </template>
      </div>
      <footer class="pagination"><span>共 {{ total }} 条 · 第 {{ page }} / {{ Math.max(1, totalPages) }} 页</span><div><button :disabled="loading || busy || page <= 1" @click="goPage(page - 1)">上一页</button><button :disabled="loading || busy || page >= totalPages" @click="goPage(page + 1)">下一页</button></div></footer>
    </section>
  </main>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { RouterLink } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Search, MessageSquareText, Trash2 } from 'lucide-vue-next'
import { getHistoryPage, deleteHistory } from '@/api/history'
import { listKB } from '@/api/knowledgeBase'
import { normalizeError } from '@/api/errors'
import { useAuthStore } from '@/stores/auth'
import type { QAHistoryDTO } from '@/types/history'
import { groupHistoryRecords, filterHistoryGroups, type HistoryPresentationItem } from '@/utils/historyPresentation'
import { createReadGate, collectHistorySnapshot, deleteHistorySnapshot } from '@/utils/historyActions'
import MyFeedbackList from '@/components/history/MyFeedbackList.vue'

const auth = useAuthStore(), gate = createReadGate(() => auth.sessionRevision), actionGate = createReadGate(() => auth.sessionRevision)
const tab = ref<'records' | 'feedback'>('records'), searchQuery = ref(''), filterKbId = ref('')
const records = ref<QAHistoryDTO[]>([]), page = ref(1), total = ref(0), totalPages = ref(1)
const loading = ref(false), loadError = ref(''), actionMessage = ref(''), busy = ref(false), preparing = ref(false)
const kbNames = ref<Record<number, string>>({}), kbError = ref(false), removed = new Set<number>()
const filteredGroups = computed(() => filterHistoryGroups(groupHistoryRecords(records.value), searchQuery.value))
const filteredCount = computed(() => filteredGroups.value.reduce((n, group) => n + group.items.length, 0))
const titles = computed(() => Object.fromEntries(records.value.map(record => [record.id, record.question])))

function kbLabel(id: number) {
  const kbId = records.value.find(record => record.id === id)?.kbId
  return kbId ? kbNames.value[kbId] || `知识库 #${kbId}` : '知识库信息未返回'
}
async function loadHistoryGroups(correctLastPage = true) {
  const token = gate.begin(), target = page.value, kbId = Number(filterKbId.value) || undefined
  loading.value = true; loadError.value = ''
  try {
    const result = (await getHistoryPage(target, 20, kbId)).data.data
    if (!gate.current(token)) return
    if (correctLastPage && target > Math.max(1, result.totalPages)) {
      page.value = Math.max(1, result.totalPages); await loadHistoryGroups(false); return
    }
    records.value = result.records.filter(record => !removed.has(record.id))
    total.value = result.total; totalPages.value = result.totalPages
  } catch (error) {
    if (gate.current(token)) { records.value = []; loadError.value = normalizeError(error).message }
  } finally { if (gate.current(token)) loading.value = false }
}
async function loadNames() {
  const revision = auth.sessionRevision
  try { const result = (await listKB()).data.data; if (revision === auth.sessionRevision && alive) { kbNames.value = Object.fromEntries(result.map(kb => [kb.id, kb.name])); kbError.value = false } }
  catch { if (revision === auth.sessionRevision && alive) kbError.value = true }
}
let alive = true
function changeScope(value: string) {
  if (busy.value) return
  if (value && (!/^\d+$/.test(value) || !Number.isSafeInteger(Number(value)) || Number(value) <= 0)) { actionMessage.value = '请输入正整数知识库 ID。'; return }
  filterKbId.value = value; page.value = 1; void loadHistoryGroups()
}
function goPage(value: number) { if (!busy.value) { page.value = value; void loadHistoryGroups() } }
function confirmRemoved(id: number) { gate.invalidate(); removed.add(id); records.value = records.value.filter(item => item.id !== id) }
async function runDelete(ids: number[], token: ReturnType<typeof actionGate.begin>) {
  gate.invalidate(); loading.value = false
  const result = await deleteHistorySnapshot(ids, deleteHistory, () => actionGate.current(token), confirmRemoved)
  if (!actionGate.current(token)) return
  actionMessage.value = `已确认删除 ${result.succeeded} 条，失败 ${result.failed} 条，结果待确认 ${result.uncertain} 条，未执行 ${result.remaining} 条。${result.stopped ? '已停止后续删除，请核查后重新确认。' : ''}`
  if (result.succeeded) window.dispatchEvent(new Event('rag-history-updated'))
  await loadHistoryGroups()
  if (loadError.value) actionMessage.value += ' 删除结果已保留，列表刷新失败，可只重试读取。'
}
async function handleDelete(item: HistoryPresentationItem, event?: Event) {
  if (busy.value) return
  const trigger = event?.currentTarget as HTMLButtonElement | undefined
  busy.value = true; const token = actionGate.begin()
  try {
    await ElMessageBox.confirm(`删除历史记录 #${item.id}：${item.title.slice(0, 100)}？`, '删除确认', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
    if (actionGate.current(token)) await runDelete([item.id], token)
  } catch (error) { if (actionGate.current(token) && error !== 'cancel' && error !== 'close') actionMessage.value = normalizeError(error).message }
  finally { if (actionGate.current(token)) { busy.value = false; await nextTick(); if (trigger?.isConnected) trigger.focus() } }
}
function cancelPreparation() { actionGate.invalidate(); preparing.value = false; busy.value = false; actionMessage.value = '已停止快照读取，未执行删除。' }
async function handleClearAll(event?: Event) {
  if (busy.value) return
  const trigger = event?.currentTarget as HTMLButtonElement | undefined
  busy.value = true; preparing.value = true; actionMessage.value = ''; const token = actionGate.begin()
  try {
    const ids = await collectHistorySnapshot(async (p, size) => (await getHistoryPage(p, size)).data.data, () => actionGate.current(token))
    if (!actionGate.current(token)) return
    preparing.value = false
    if (!ids.length) { actionMessage.value = '读取快照中没有可删除的记录。'; return }
    await ElMessageBox.confirm(`将逐条删除全部知识库中本次读取的 ${ids.length} 条历史记录，不受本页筛选影响。快照读取期间新增的记录可能不包含；删除无法撤销，也不保证关联反馈被清理。`, '确认批量删除', { confirmButtonText: `删除 ${ids.length} 条`, cancelButtonText: '取消', type: 'warning' })
    if (actionGate.current(token)) await runDelete(ids, token)
  } catch (error) { if (actionGate.current(token) && error !== 'cancel' && error !== 'close') actionMessage.value = '快照准备失败，未执行删除：' + normalizeError(error).message }
  finally { if (actionGate.current(token)) { preparing.value = false; busy.value = false; await nextTick(); if (trigger?.isConnected) trigger.focus() } }
}
function refresh() { if (!busy.value) { void loadNames(); if (tab.value === 'records') void loadHistoryGroups(); else window.dispatchEvent(new Event('rag-feedback-updated')) } }
function handleHistoryUpdated() { if (!busy.value && tab.value === 'records') void loadHistoryGroups() }
watch(() => auth.sessionRevision, () => { gate.invalidate(); actionGate.invalidate(); records.value = []; removed.clear(); kbNames.value = {}; searchQuery.value = ''; filterKbId.value = ''; actionMessage.value = ''; busy.value = false; preparing.value = false; loading.value = false; loadError.value = ''; total.value = 0; totalPages.value = 1; page.value = 1; const revision = auth.sessionRevision; queueMicrotask(() => { if (alive && revision === auth.sessionRevision && auth.isLoggedIn) { void loadHistoryGroups(); void loadNames() } }) }, { flush: 'sync' })
watch(tab, value => { if (value === 'records' && !busy.value) void loadHistoryGroups(); else { gate.invalidate(); loading.value = false } })
onMounted(() => { window.addEventListener('rag-history-updated', handleHistoryUpdated); void loadHistoryGroups(); void loadNames() })
onUnmounted(() => { alive = false; gate.close(); actionGate.close(); window.removeEventListener('rag-history-updated', handleHistoryUpdated) })
</script>

<style scoped>
.history-page { width: min(100%, 1100px); margin: 0 auto; padding: 42px 40px; color: var(--rag-text-primary); }
.page-heading { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.eyebrow { font-size: 12px; color: var(--rag-text-secondary); } h1 { font-size: 28px; margin: 8px 0 10px; letter-spacing: -.6px; } .page-heading p { color: var(--rag-text-secondary); font-size: 13px; margin: 0; line-height: 1.6; }
button, input { font: inherit; } button { cursor: pointer; color: inherit; background: var(--rag-bg-surface); border: 1px solid var(--rag-border); border-radius: 8px; padding: 8px 12px; font-size: 13px; } button:disabled { opacity: .45; cursor: not-allowed; } button:hover:not(:disabled) { background: var(--rag-bg-hover); } button:focus-visible, input:focus-visible, a:focus-visible { outline: 2px solid var(--rag-primary); outline-offset: 3px; }
.history-tabs { display: flex; gap: 8px; padding: 26px 0 20px; border-bottom: 1px solid var(--rag-border); margin-bottom: 22px; } .history-tabs button { border: 0; background: transparent; color: var(--rag-text-secondary); } .history-tabs button[aria-pressed=true] { background: var(--rag-bg-hover); color: var(--rag-text-primary); }
.toolbar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; } .find { flex: 1; min-width: 180px; display: flex; gap: 10px; align-items: center; padding: 9px 12px; border: 1px solid var(--rag-border); border-radius: 10px; background: var(--rag-bg-surface); color: var(--rag-text-secondary); } .find input { flex: 1; min-width: 0; width: 100%; background: transparent; border: 0; color: var(--rag-text-primary); font-size: 13px; } .find button { padding: 0 5px; border: 0; } .scope { display: flex; gap: 7px; align-items: center; font-size: 12px; color: var(--rag-text-secondary); } .scope input { width: 78px; padding: 9px 7px; border-radius: 8px; border: 1px solid var(--rag-border); background: var(--rag-bg-surface); color: var(--rag-text-primary); } .batch-button, .delete-button { color: var(--rag-danger); }
.scope-note, .action-message { color: var(--rag-text-secondary); font-size: 12px; line-height: 1.7; overflow-wrap: anywhere; } .action-message { padding: 12px; background: var(--rag-bg-hover); border-radius: 8px; }
.history-group h2 { font-size: 12px; font-weight: 500; color: var(--rag-text-secondary); margin: 22px 0 9px; } .history-row { display: flex; align-items: center; border-radius: 10px; padding: 0 6px; } .history-row:hover { background: var(--rag-bg-hover); } .record-link { flex: 1; min-width: 0; display: flex; align-items: center; gap: 12px; padding: 14px 8px; color: inherit; text-decoration: none; font-size: 14px; } .record-link > svg { flex: none; color: var(--rag-text-secondary); } .record-title { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; } .record-meta { display: flex; gap: 12px; font-size: 12px; color: var(--rag-text-secondary); } .record-meta time { min-width: 70px; text-align: right; } .delete-button { display: grid; place-items: center; border: 0; background: transparent; padding: 9px; flex: none; }
.state-panel { min-height: 210px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; color: var(--rag-text-secondary); font-size: 14px; text-align: center; } .pagination { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; border-top: 1px solid var(--rag-border); margin-top: 24px; padding-top: 20px; color: var(--rag-text-secondary); font-size: 12px; } .pagination div { display: flex; gap: 8px; }
@media(max-width:768px) { .history-page { padding: 60px 18px 24px; } h1 { font-size: 25px; } .find { flex-basis: 100%; } .record-link { flex-wrap: wrap; gap: 8px; } .record-title { flex-basis: calc(100% - 40px); } .record-meta { margin-left: 26px; flex: 1; flex-wrap: wrap; } .record-meta time { text-align: left; } .record-link { overflow-wrap: anywhere; } }
</style>
