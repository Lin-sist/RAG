<template>
  <main class="kb-detail-view">
    <div class="detail-top">
      <button class="back" @click="router.push('/kb')"><ArrowLeft :size="16" />知识库</button>
      <div v-if="current && !detailError" class="actions">
        <el-button type="primary" @click="router.push({ path: '/chat', query: { kbId: String(current.id) } })"><Sparkles :size="15" />开始提问</el-button>
        <el-button @click="editDialogVisible = true">编辑</el-button>
        <el-button :loading="deleting" type="danger" plain @click="confirmDelete">删除</el-button>
      </div>
    </div>
    <el-skeleton v-if="detailLoading" :rows="6" animated />
    <div v-else-if="detailError" role="status"><el-alert :title="detailError" type="error" :closable="false" /><el-button class="retry" @click="loadDetail">重试详情加载</el-button></div>
    <template v-else-if="current">
      <header class="detail-hero">
        <div class="hero-icon"><Database :size="25" /></div>
        <div><div class="title-line"><h1>{{ current.name }}</h1><span class="visibility">{{ current.isPublic ? '组织内公开' : '非公开' }}</span></div><p>{{ current.description || '暂无描述' }}</p><small>更新于 {{ formatDate(current.updatedAt) }}</small></div>
      </header>
      <div v-if="statsError" class="local-error" role="status"><el-alert :title="confirmedAction + '统计刷新失败：' + statsError" type="warning" :closable="false" /><el-button class="retry" :loading="kbStore.statisticsLoading" @click="loadStatistics">重试统计加载</el-button></div>
      <KBStatsPanel v-else :stats="statistics" :loading="kbStore.statisticsKbId === kbId && kbStore.statisticsLoading" />
      <div v-if="listRefreshError" class="local-error" role="status"><el-alert :title="confirmedAction + '知识库列表刷新失败：' + listRefreshError" type="warning" :closable="false" /><el-button class="retry" @click="loadList">重试知识库列表</el-button></div>
      <div class="detail-grid">
        <section class="detail-card documents">
          <div class="card-heading"><h2>文档</h2><el-button @click="uploadVisible = !uploadVisible" :aria-expanded="uploadVisible"><Upload :size="15" />{{ uploadVisible ? '收起上传' : '上传文档' }}</el-button></div>
          <div v-show="uploadVisible" class="upload-section"><DocUploader :kb-id="kbId!" :key="kbId!" @uploaded="handleDocUploaded" @all-done="loadDocuments" /></div>
          <DocProgress :tasks="progressTasks" @retry="retryTask" />
          <div v-if="docsError" class="local-error" role="status"><el-alert :title="confirmedAction + '文档刷新失败：' + docsError" type="error" :closable="false" /><el-button class="retry" @click="loadDocuments">重试文档加载</el-button></div>
          <DocList v-else :kb-id="kbId!" :documents="documents" :loading="docsLoading" @refresh="loadDocuments" @deleted="handleDocumentDeleted" />
        </section>
        <aside class="detail-card metadata">
          <h2>基本信息</h2>
          <dl><dt>知识库 ID</dt><dd>{{ current.id }}</dd><dt>可见范围</dt><dd>{{ current.isPublic ? '组织内公开：本工作空间内的用户可以读取，其他工作空间不可见。' : '非公开：创建者及已获授权的本工作空间用户可以访问。' }}</dd><dt>创建时间</dt><dd>{{ formatDate(current.createdAt) }}</dd><dt>更新时间</dt><dd>{{ formatDate(current.updatedAt) }}</dd></dl>
          <details class="vector-identity"><summary>向量索引信息</summary><dl><template v-for="(value, label) in vectorIdentity" :key="label"><dt>{{ label }}</dt><dd>{{ value ?? '未返回' }}</dd></template></dl></details>
          <p class="read-note">文档完成索引后可参与检索。“需要核对”的文档请先核查，不能视为已完成。</p>
        </aside>
      </div>
    </template>
    <KBCreateDialog v-model:visible="editDialogVisible" :edit-data="current" @success="handleEditSuccess" />
  </main>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { ArrowLeft, Database, Upload, Sparkles } from 'lucide-vue-next'
import { useKnowledgeBaseStore } from '@/stores/knowledgeBase'
import { useTaskPolling } from '@/composables/useTaskPolling'
import { normalizeError } from '@/api/errors'
import { formatDate } from '@/utils/format'
import { parseKbId } from '@/utils/kbPresentation'
import { listDocuments } from '@/api/knowledgeBase'
import KBStatsPanel from '@/components/knowledge-base/KBStatsPanel.vue'
import KBCreateDialog from '@/components/knowledge-base/KBCreateDialog.vue'
import DocUploader from '@/components/document/DocUploader.vue'
import DocList from '@/components/document/DocList.vue'
import DocProgress from '@/components/document/DocProgress.vue'
import type { ProgressTask } from '@/components/document/DocProgress.vue'
import type { DocumentInfo, DocumentUploadResponse } from '@/types/document'

const router = useRouter(), route = useRoute(), kbStore = useKnowledgeBaseStore(), polling = useTaskPolling()
const kbId = computed(() => parseKbId(route.params.id))
const current = computed(() => kbStore.current?.id === kbId.value ? kbStore.current : null)
const statistics = computed(() => kbStore.statistics?.kbId === kbId.value ? kbStore.statistics : null)
const statsError = computed(() => kbStore.statisticsKbId === kbId.value ? kbStore.statisticsError : '')
const editDialogVisible = ref(false), uploadVisible = ref(false), deleting = ref(false)
const detailLoading = ref(false), detailError = ref(''), docsError = ref(''), listRefreshError = ref(''), confirmedAction = ref('')
const documents = ref<DocumentInfo[]>([]), docsLoading = ref(false), progressTasks = reactive<ProgressTask[]>([])
let pageSequence = 0, docsSequence = 0
const vectorIdentity = computed(() => ({
  '向量集合': current.value?.vectorCollection, '提供方': current.value?.vectorProviderFamily,
  '模型': current.value?.vectorModel, '维度': current.value?.vectorDimension,
  '端点身份': current.value?.vectorEndpointIdentity, '请求契约': current.value?.vectorRequestContract,
  '代次': current.value?.vectorGeneration, '身份指纹': current.value?.vectorIdentityFingerprint,
}))
async function loadDocuments() {
  const seq = ++docsSequence, id = kbId.value
  if (!id) return
  docsLoading.value = true; docsError.value = ''
  try {
    const response = await listDocuments(id)
    if (seq === docsSequence && id === kbId.value) documents.value = response.data.data
  } catch (error) {
    if (seq === docsSequence && id === kbId.value) docsError.value = normalizeError(error).message
  } finally { if (seq === docsSequence) docsLoading.value = false }
}
async function loadStatistics() {
  const id = kbId.value, seq = pageSequence
  if (!id) return
  const stats = await kbStore.fetchStatistics(id)
  if (seq === pageSequence && stats?.kbId === id && current.value) current.value.documentCount = stats.documentCount
}
async function loadList() {
  const seq = pageSequence
  listRefreshError.value = ''
  try { await kbStore.fetchList() }
  catch (error) { if (seq === pageSequence) listRefreshError.value = normalizeError(error).message }
}
function refreshRelated() { return Promise.all([loadDocuments(), loadStatistics(), loadList()]) }
function startTaskPolling(task: ProgressTask) {
  const seq = pageSequence
  task.pollError = ''
  polling.start(task.taskId, {
    update: status => { if (seq === pageSequence) task.status = status },
    error: error => { if (seq === pageSequence) task.pollError = normalizeError(error).message },
    terminal: () => { if (seq === pageSequence) { confirmedAction.value = '任务已结束；'; void refreshRelated() } },
  })
}
function retryTask(id: string) { const task = progressTasks.find(item => item.taskId === id); if (task) startTaskPolling(task) }
function handleDocUploaded(data: DocumentUploadResponse) {
  if (progressTasks.some(task => task.taskId === data.taskId)) return
  const task = reactive<ProgressTask>({ taskId: data.taskId, fileName: data.fileName, status: null })
  progressTasks.push(task); startTaskPolling(task)
}
async function loadDetail() {
  const seq = ++pageSequence, id = kbId.value
  polling.stopAll(); progressTasks.splice(0); docsSequence++; kbStore.clearDetail()
  documents.value = []; docsLoading.value = false; detailError.value = ''; docsError.value = ''; listRefreshError.value = ''; confirmedAction.value = ''; uploadVisible.value = false; editDialogVisible.value = false
  if (!id) { detailError.value = '无效的知识库 ID'; detailLoading.value = false; return }
  detailLoading.value = true
  try {
    await kbStore.fetchById(id)
    if (seq !== pageSequence) return
    detailLoading.value = false
    void loadStatistics(); void loadDocuments()
  } catch (error) { if (seq === pageSequence) detailError.value = normalizeError(error).message }
  finally { if (seq === pageSequence) detailLoading.value = false }
}
watch(() => route.params.id, loadDetail, { immediate: true })
onUnmounted(() => { pageSequence++; docsSequence++; polling.stopAll(); kbStore.clearDetail() })
function handleEditSuccess() { void loadList() }
function handleDocumentDeleted() { confirmedAction.value = '删除已完成；'; void refreshRelated() }
async function confirmDelete() {
  if (!current.value || deleting.value) return
  const id = current.value.id, name = current.value.name
  deleting.value = true
  try {
    try { await ElMessageBox.confirm(`确定删除「${name}」及其文档和向量数据吗？`, '删除确认', { type: 'warning' }) }
    catch { return }
    try { await kbStore.remove(id); if (kbId.value === id) await router.push('/kb') }
    catch (error) { ElMessage.error(normalizeError(error).message) }
  } finally { deleting.value = false }
}
</script>

<style scoped>
.kb-detail-view { max-width: 1160px; margin-inline: auto; padding: 32px; box-sizing: border-box; width: 100%; min-width: 0; overflow-y: auto; color: var(--rag-text-primary); }
.detail-top, .actions, .title-line, .card-heading { display: flex; align-items: center; gap: 12px; }
.detail-top { justify-content: space-between; margin-bottom: 32px; flex-wrap: wrap; }
.back { display: inline-flex; align-items: center; gap: 8px; background: transparent; color: var(--rag-text-secondary); border: 0; padding: 8px 0; cursor: pointer; }
.actions { flex-wrap: wrap; gap: 8px; }
.actions .el-button + .el-button { margin-left: 0; }
.actions svg, .card-heading svg { margin-right: 6px; }
.detail-hero { display: flex; gap: 18px; margin-bottom: 28px; align-items: flex-start; }
.detail-hero > div:last-child { min-width: 0; }
.hero-icon { width: 52px; height: 52px; flex-shrink: 0; display: grid; place-items: center; background: var(--rag-bg-hover); border: 1px solid var(--rag-border); border-radius: 14px; }
.title-line { flex-wrap: wrap; }
h1 { font-size: 28px; line-height: 1.3; margin: 0; overflow-wrap: anywhere; }
.detail-hero p { margin: 10px 0; color: var(--rag-text-secondary); overflow-wrap: anywhere; white-space: pre-wrap; line-height: 1.65; }
.detail-hero small { color: var(--rag-text-secondary); }
.visibility { color: var(--rag-text-secondary); background: var(--rag-bg-hover); border-radius: 6px; padding: 4px 8px; font-size: 12px; }
.detail-grid { display: grid; grid-template-columns: minmax(0, 1fr) 280px; align-items: start; gap: 20px; margin-top: 24px; }
.detail-card { min-width: 0; padding: 22px; border: 1px solid var(--rag-border); background: var(--rag-bg-card); border-radius: 16px; }
h2 { font-size: 15px; font-weight: 600; margin: 0; }
.card-heading { justify-content: space-between; flex-wrap: wrap; margin-bottom: 18px; }
.upload-section { padding-bottom: 22px; margin-bottom: 18px; border-bottom: 1px solid var(--rag-border); }
dl { margin: 20px 0 0; }
dt { color: var(--rag-text-secondary); font-size: 12px; margin-top: 16px; }
dd { margin: 6px 0 0; font-size: 13px; overflow-wrap: anywhere; line-height: 1.6; }
.vector-identity { margin-top: 24px; border-top: 1px solid var(--rag-border); padding-top: 16px; }
summary { cursor: pointer; font-size: 13px; }
.read-note { font-size: 12px; line-height: 1.7; color: var(--rag-text-secondary); margin-top: 24px; }
.retry { margin-top: 10px; }
.local-error { margin-block: 16px; }
button:focus-visible, summary:focus-visible { outline: 2px solid var(--rag-primary); outline-offset: 4px; }
@media (max-width: 1100px) { .detail-grid { grid-template-columns: 1fr; } }
@media (max-width: 720px) { .kb-detail-view { padding: 22px 16px; } .detail-top { margin-bottom: 24px; } .detail-card { padding: 16px; } h1 { font-size: 23px; } .detail-hero { gap: 12px; } .hero-icon { width: 42px; height: 42px; } }
@media (max-width: 768px) { .kb-detail-view { padding-top: 60px; } }
</style>
