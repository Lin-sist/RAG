<template>
  <div class="kb-detail-view" v-loading="kbStore.loading">
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
      <div class="header-actions">
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

    <div v-if="kbStore.current" class="detail-content">
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

      <KBStatsPanel :stats="kbStore.statistics" :loading="statsLoading" />

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
            :kb-id="getKbId()"
            @uploaded="handleDocUploaded"
            @all-done="handleAllUploadDone"
          />
          <div class="section-divider"></div>
        </div>

        <DocProgress :tasks="progressTasks" />

        <DocList
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
import { ref, reactive, onMounted, watch, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Edit, Delete, Upload, ArrowLeft, InfoFilled } from '@element-plus/icons-vue'
import { FileText } from 'lucide-vue-next'
import { useKnowledgeBaseStore } from '@/stores/knowledgeBase'
import { formatDate } from '@/utils/format'
import { listDocuments } from '@/api/knowledgeBase'
import { getTaskStatus } from '@/api/task'
import KBStatsPanel from '@/components/knowledge-base/KBStatsPanel.vue'
import KBCreateDialog from '@/components/knowledge-base/KBCreateDialog.vue'
import DocUploader from '@/components/document/DocUploader.vue'
import DocList from '@/components/document/DocList.vue'
import DocProgress from '@/components/document/DocProgress.vue'
import type { DocumentInfo } from '@/types/document'
import type { DocumentUploadResponse } from '@/types/document'
import type { TaskStatusResponse } from '@/types/task'

interface ProgressTask {
  taskId: string
  fileName: string
  status: TaskStatusResponse | null
}

const router = useRouter()
const route = useRoute()
const kbStore = useKnowledgeBaseStore()

const editDialogVisible = ref(false)
const statsLoading = ref(false)
const uploadVisible = ref(false)

const documents = ref<DocumentInfo[]>([])
const docsLoading = ref(false)

const progressTasks = reactive<ProgressTask[]>([])
const pollingTimers = new Map<string, ReturnType<typeof setInterval>>()

function getKbId(): number {
  return Number(route.params.id)
}

async function loadDocuments() {
  const id = getKbId()
  if (!id || isNaN(id)) return
  docsLoading.value = true
  try {
    const res = await listDocuments(id)
    documents.value = res.data.data
  } catch {
    ElMessage.error('获取文档列表失败')
  } finally {
    docsLoading.value = false
  }
}

function handleDocUploaded(data: DocumentUploadResponse) {
  const task: ProgressTask = {
    taskId: data.taskId,
    fileName: data.fileName,
    status: null,
  }
  progressTasks.push(task)
  startTaskPolling(task)
}

function handleAllUploadDone() {
  loadDocuments()
}

function startTaskPolling(task: ProgressTask) {
  pollTaskOnce(task)

  const timer = setInterval(() => {
    pollTaskOnce(task)
  }, 2000)

  pollingTimers.set(task.taskId, timer)
}

async function pollTaskOnce(task: ProgressTask) {
  try {
    const res = await getTaskStatus(task.taskId)
    task.status = res.data.data
    const state = res.data.data.state

    if (state === 'COMPLETED' || state === 'FAILED' || state === 'CANCELLED') {
      stopTaskPolling(task.taskId)

      if (state === 'COMPLETED') {
        ElMessage.success(`「${task.fileName}」处理完成`)
      } else {
        const reason = task.status?.error || task.status?.message || '处理失败'
        ElMessage.error(`「${task.fileName}」处理失败：${reason}`)
      }

      loadDocuments()
      kbStore.fetchStatistics(getKbId())
      kbStore.fetchById(getKbId())

      setTimeout(() => {
        const idx = progressTasks.findIndex(t => t.taskId === task.taskId)
        if (idx !== -1) progressTasks.splice(idx, 1)
      }, 3000)
    }
  } catch {
    // 轮询出错时不立即停止，等下次重试
  }
}

function stopTaskPolling(taskId: string) {
  const timer = pollingTimers.get(taskId)
  if (timer) {
    clearInterval(timer)
    pollingTimers.delete(taskId)
  }
}

function stopAllPolling() {
  pollingTimers.forEach((timer) => clearInterval(timer))
  pollingTimers.clear()
}

async function loadDetail() {
  const id = getKbId()
  if (!id || isNaN(id)) {
    ElMessage.error('无效的知识库 ID')
    router.push('/kb')
    return
  }
  try {
    await kbStore.fetchById(id)
    statsLoading.value = true
    await kbStore.fetchStatistics(id)
    statsLoading.value = false
    await loadDocuments()
  } catch {
    router.push('/kb')
  }
}

onMounted(loadDetail)

watch(() => route.params.id, () => {
  if (route.params.id) {
    stopAllPolling()
    progressTasks.splice(0)
    loadDetail()
  }
})

onUnmounted(() => {
  stopAllPolling()
})

function handleEditSuccess() {
  loadDetail()
}

async function confirmDelete() {
  if (!kbStore.current) return
  try {
    await ElMessageBox.confirm(
      `确定要删除知识库「${kbStore.current.name}」吗？\n该操作不可恢复！`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning', confirmButtonClass: 'el-button--danger' }
    )
    await kbStore.remove(kbStore.current.id)
    router.push('/kb')
  } catch {
    // 用户取消
  }
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
</style>
