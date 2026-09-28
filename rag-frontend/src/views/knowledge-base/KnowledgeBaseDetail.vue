<template>
  <div class="kb-detail-view" v-loading="detailLoading">
    <div class="detail-header">
      <button class="back-btn" @click="router.push('/kb')">
        <ArrowLeft :size="18" />
      </button>
      <div class="detail-title">
        <h1 class="title-text">{{ kbStore.current?.name || '知识库详情' }}</h1>
        <span
          v-if="kbStore.current"
          :class="['visibility-pill', kbStore.current.isPublic ? 'public' : 'private']"
        >
          {{ kbStore.current.isPublic ? '公开' : '私有' }}
        </span>
      </div>
      <div v-if="kbStore.current && !detailError" class="header-actions">
        <button class="action-btn primary" @click="editDialogVisible = true">
          <Edit :size="16" />
          <span>编辑</span>
        </button>
        <button class="action-btn danger" @click="confirmDelete">
          <Delete :size="16" />
          <span>删除</span>
        </button>
      </div>
    </div>

    <el-alert v-if="detailError" :title="detailError" type="error" :closable="false" show-icon />
    <el-button v-if="detailError" @click="loadDetail">重新加载</el-button>
    <div v-if="kbStore.current && !detailError && !detailLoading" class="detail-content">
      <div class="info-card-v2">
        <div class="card-header-v2">
          <InfoFilled class="header-icon" />
          <span class="card-section-title">基本信息</span>
        </div>
        <div class="info-grid">
          <div class="info-row">
            <div class="info-item">
              <span class="info-label">知识库 ID</span>
              <span class="info-value">{{ kbStore.current.id }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">名称</span>
              <span class="info-value">{{ kbStore.current.name }}</span>
            </div>
          </div>
          <div class="info-divider"></div>
          <div class="info-row full">
            <div class="info-item">
              <span class="info-label">描述</span>
              <span class="info-value desc">{{ kbStore.current.description || '暂无描述' }}</span>
            </div>
          </div>
          <div class="info-divider"></div>
          <div class="info-row">
            <div class="info-item">
              <span class="info-label">向量集合</span>
              <span class="info-value mono">{{ kbStore.current.vectorCollection }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">文档数量</span>
              <span class="info-value highlight">{{ kbStore.current.documentCount }} 篇</span>
            </div>
          </div>
          <div class="info-divider"></div>
          <div class="info-row">
            <div class="info-item">
              <span class="info-label">创建时间</span>
              <span class="info-value">{{ formatDate(kbStore.current.createdAt) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">更新时间</span>
              <span class="info-value">{{ formatDate(kbStore.current.updatedAt) }}</span>
            </div>
          </div>
        </div>
      </div>

      <el-alert v-if="kbStore.statisticsError" :title="'统计暂不可用：' + kbStore.statisticsError" type="warning" :closable="false" />
      <KBStatsPanel v-else :stats="kbStore.statistics" :loading="statsLoading" />
      <details class="vector-identity">
        <summary>向量索引信息</summary>
        <dl><template v-for="(value, label) in vectorIdentity" :key="label"><dt>{{ label }}</dt><dd>{{ value ?? '未返回' }}</dd></template></dl>
      </details>

      <div class="info-card-v2">
        <div class="card-header-v2 doc-header">
          <div class="header-left">
            <FileText :size="18" class="header-icon" />
            <span class="card-section-title">文档管理</span>
          </div>
          <button class="action-btn primary small" @click="uploadVisible = !uploadVisible">
            <Upload :size="14" />
            <span>{{ uploadVisible ? '收起上传' : '上传文档' }}</span>
          </button>
        </div>

        <div v-show="uploadVisible" class="upload-section">
          <DocUploader
            :kb-id="getKbId()" :key="getKbId()"
            @uploaded="handleDocUploaded"
            @all-done="handleAllUploadDone"
          />
          <div class="section-divider"></div>
        </div>

        <DocProgress :tasks="progressTasks" @retry="retryTask" />
        <el-alert v-if="docsError" :title="docsError" type="error" :closable="false" />
        <el-button v-if="docsError" @click="loadDocuments">重试文档加载</el-button>

        <DocList v-if="!docsError"
          :kb-id="getKbId()"
          :documents="documents"
          :loading="docsLoading"
          @refresh="loadDocuments"
        />
      </div>
    </div>

    <KBCreateDialog
      v-model:visible="editDialogVisible"
      :edit-data="kbStore.current"
      @success="handleEditSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Edit, Delete, Upload, ArrowLeft, InfoFilled } from '@element-plus/icons-vue'
import { FileText } from 'lucide-vue-next'
import { useKnowledgeBaseStore } from '@/stores/knowledgeBase'
import { useTaskPolling } from '@/composables/useTaskPolling'
import { normalizeError } from '@/api/errors'
import { formatDate } from '@/utils/format'
import { listDocuments } from '@/api/knowledgeBase'
import KBStatsPanel from '@/components/knowledge-base/KBStatsPanel.vue'
import KBCreateDialog from '@/components/knowledge-base/KBCreateDialog.vue'
import DocUploader from '@/components/document/DocUploader.vue'
import DocList from '@/components/document/DocList.vue'
import DocProgress from '@/components/document/DocProgress.vue'
import type { ProgressTask } from '@/components/document/DocProgress.vue'
import type { DocumentInfo, DocumentUploadResponse } from '@/types/document'

const router = useRouter(), route = useRoute(), kbStore = useKnowledgeBaseStore()
const polling = useTaskPolling()
const editDialogVisible = ref(false), statsLoading = ref(false), uploadVisible = ref(false)
const detailLoading = ref(false), detailError = ref(''), docsError = ref('')
const documents = ref<DocumentInfo[]>([]), docsLoading = ref(false)
const progressTasks = reactive<ProgressTask[]>([])
let pageSequence = 0, docsSequence = 0
const vectorIdentity = computed(() => ({
  '提供方': kbStore.current?.vectorProviderFamily, '模型': kbStore.current?.vectorModel,
  '维度': kbStore.current?.vectorDimension, '端点身份': kbStore.current?.vectorEndpointIdentity,
  '请求契约': kbStore.current?.vectorRequestContract, '代次': kbStore.current?.vectorGeneration,
  '身份指纹': kbStore.current?.vectorIdentityFingerprint,
}))
function getKbId() { return Number(route.params.id) }
async function loadDocuments() {
  const seq = ++docsSequence, id = getKbId()
  docsLoading.value = true; docsError.value = ''
  try {
    const res = await listDocuments(id)
    if (seq === docsSequence && id === getKbId()) documents.value = res.data.data
  } catch (error) {
    if (seq === docsSequence && id === getKbId()) docsError.value = normalizeError(error).message
  } finally { if (seq === docsSequence) docsLoading.value = false }
}
function startTaskPolling(task: ProgressTask) {
  const seq = pageSequence
  task.pollError = ''
  polling.start(task.taskId, {
    update: status => { task.status = status },
    error: error => { task.pollError = normalizeError(error).message },
    terminal: () => {
      if (seq !== pageSequence) return
      void loadDocuments()
      const id = getKbId()
      void kbStore.fetchStatistics(id).then(stats => {
        if (seq === pageSequence && stats && kbStore.current?.id === id) kbStore.current.documentCount = stats.documentCount
      })
    },
  })
}
function retryTask(id: string) {
  const task = progressTasks.find(item => item.taskId === id)
  if (task) startTaskPolling(task)
}
function handleDocUploaded(data: DocumentUploadResponse) {
  if (progressTasks.some(task => task.taskId === data.taskId)) return
  const task = reactive<ProgressTask>({ taskId: data.taskId, fileName: data.fileName, status: null })
  progressTasks.push(task); startTaskPolling(task)
}
function handleAllUploadDone() { void loadDocuments() }
async function loadDetail() {
  const seq = ++pageSequence, id = getKbId()
  polling.stopAll(); progressTasks.splice(0); docsSequence++
  documents.value = []; detailError.value = ''; docsError.value = ''; detailLoading.value = true
  if (!Number.isInteger(id) || id <= 0) { detailError.value = '无效的知识库 ID'; detailLoading.value = false; return }
  try {
    await kbStore.fetchById(id)
    if (seq !== pageSequence) return
    statsLoading.value = true
    await Promise.all([kbStore.fetchStatistics(id), loadDocuments()])
  } catch (error) {
    if (seq === pageSequence) detailError.value = normalizeError(error).message
  } finally {
    if (seq === pageSequence) { detailLoading.value = false; statsLoading.value = false }
  }
}
watch(() => route.params.id, loadDetail, { immediate: true })
onUnmounted(() => { pageSequence++; docsSequence++; polling.stopAll() })
function handleEditSuccess() { void loadDetail() }
async function confirmDelete() {
  if (!kbStore.current) return
  const id = kbStore.current.id
  try { await ElMessageBox.confirm(`确定要删除知识库「${kbStore.current.name}」及其文档吗？`, '删除确认', { type: 'warning' }) }
  catch { return }
  try { await kbStore.remove(id); await router.push('/kb') }
  catch (error) { ElMessage.error(normalizeError(error).message) }
}
</script>

<style scoped>
.kb-detail-view {
  width: 100%;
  max-width: 900px;
  margin: 0 auto;
  box-sizing: border-box;
  min-width: 0;
  padding: 16px 24px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--rag-space-3);
  margin-bottom: var(--rag-space-4);
  padding: 8px 0;
  max-height: 180px;
  width: 100%;
}

.back-btn {
  width: 32px;
  height: 32px;
  border: 1px solid var(--rag-border);
  background: var(--rag-bg-card);
  border-radius: var(--rag-radius-sm);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--rag-text-secondary);
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.back-btn:hover {
  background: var(--rag-bg-hover);
  color: var(--rag-text-primary);
  border-color: var(--rag-primary);
}

.detail-title {
  display: flex;
  align-items: center;
  gap: var(--rag-space-3);
  flex: 1;
  min-width: 0;
}

.title-text {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--rag-text-primary);
  letter-spacing: -0.01em;
}

.visibility-pill {
  font-size: 11px;
  font-weight: 500;
  padding: 3px 10px;
  border-radius: 9999px;
}

.visibility-pill.private {
  background: var(--rag-bg-hover);
  color: var(--rag-text-secondary);
}

.visibility-pill.public {
  background: var(--rag-success-light);
  color: var(--rag-primary);
}

.header-actions {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: var(--rag-space-2);
  flex-shrink: 0;
}

.action-btn {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: nowrap;
  gap: 6px;
  width: auto;
  padding: 8px 16px;
  border: none;
  border-radius: var(--rag-radius-sm);
  font-size: 14px;
  font-weight: 500;
  line-height: 1.2;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn :deep(svg) {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.action-btn > span {
  white-space: nowrap;
}

.action-btn.small {
  width: auto;
  padding: 6px 12px;
  font-size: 13px;
}

.action-btn.primary {
  background: var(--rag-primary);
  color: #fff;
}

.action-btn.primary:hover {
  background: var(--rag-primary-hover);
}

.action-btn.danger {
  background: #ef4444;
  color: #fff;
}

.action-btn.danger:hover {
  background: #dc2626;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: var(--rag-space-4);
}

.info-card-v2 {
  background: var(--rag-bg-card);
  border: 1px solid var(--rag-border);
  border-radius: var(--rag-radius-md);
  padding: var(--rag-space-5);
}

.card-header-v2 {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  margin-bottom: var(--rag-space-4);
}

.card-header-v2.doc-header {
  justify-content: space-between;
  padding-left: 8px;
  padding-right: 8px;
}

.card-header-v2.doc-header .header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  color: var(--rag-primary);
}

.card-section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--rag-text-primary);
}

.info-grid {
  display: flex;
  flex-direction: column;
  padding: 0 8px;
}

.info-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--rag-space-4);
  padding: var(--rag-space-2) 8px;
}

.info-row.full {
  grid-template-columns: 1fr;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-left: 8px;
}

.info-label {
  font-size: 12px;
  color: var(--rag-text-secondary);
}

.info-value {
  font-size: 14px;
  color: var(--rag-text-primary);
}

.info-value.mono {
  font-family: 'SF Mono', Consolas, monospace;
}

.info-value.highlight {
  color: var(--rag-primary);
  font-weight: 600;
}

.info-value.desc {
  line-height: 1.7;
}

.info-divider {
  height: 1px;
  background: var(--rag-border);
  margin: var(--rag-space-4) 0;
}

.upload-section {
  padding-left: 8px;
  padding-right: 8px;
  margin-bottom: var(--rag-space-4);
}

:deep(.doc-progress),
:deep(.doc-list) {
  padding-left: 8px;
  padding-right: 8px;
}

.section-divider {
  height: 1px;
  background: var(--rag-border);
  margin-left: 8px;
  margin-right: 8px;
  margin-top: var(--rag-space-4);
}

.vector-identity { padding: 16px 24px; border: 1px solid var(--rag-border); border-radius: 12px; }
.vector-identity summary { cursor: pointer; font-weight: 500; }
.vector-identity dl { display: grid; grid-template-columns: 100px minmax(0, 1fr); gap: 12px 24px; }
.vector-identity dt { color: var(--rag-text-secondary); }
.vector-identity dd { margin: 0; overflow-wrap: anywhere; }
.info-value, .title-text { overflow-wrap: anywhere; }
@media (max-width: 640px) {
  .kb-detail-view { padding: 16px; }
  .detail-header { flex-wrap: wrap; max-height: none; }
  .header-actions { width: 100%; }
  .info-row { grid-template-columns: minmax(0, 1fr); }
  .info-card-v2 { padding: 16px; }
}
</style>
