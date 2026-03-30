<template>
  <div class="kb-detail-view" v-loading="kbStore.loading">
    <!-- 顶部返回导航 -->
    <div class="detail-header">
      <button class="back-btn" @click="router.push('/knowledge-base')">
        <ArrowLeft :size="18" />
      </button>
      <div class="detail-title">
        <span class="title-text">{{ kbStore.current?.name || '知识库详情' }}</span>
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
      <!-- 基础信息卡片 - 改为卡片式信息组 -->
      <div class="info-card-v2">
        <div class="card-header-v2">
          <Info :size="18" class="header-icon" />
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

      <!-- 统计面板 -->
      <KBStatsPanel :stats="kbStore.statistics" :loading="statsLoading" />

      <!-- 文档管理区域 -->
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

        <!-- 上传区域（可折叠） -->
        <div v-show="uploadVisible" class="upload-section">
          <DocUploader
            :kb-id="getKbId()"
            @uploaded="handleDocUploaded"
            @all-done="handleAllUploadDone"
          />
          <div class="section-divider"></div>
        </div>

        <!-- 进度面板（有任务时显示） -->
        <DocProgress :tasks="progressTasks" />

        <!-- 文档列表 -->
        <DocList
          :kb-id="getKbId()"
          :documents="documents"
          :loading="docsLoading"
          @refresh="loadDocuments"
        />
      </div>
    </div>

    <!-- 编辑弹窗 -->
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
import { Edit, Delete, Upload, ArrowLeft, Info } from '@element-plus/icons-vue'
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

// ========== 文档列表状态 ==========
const documents = ref<DocumentInfo[]>([])
const docsLoading = ref(false)

// ========== 进度任务状态 ==========
const progressTasks = reactive<ProgressTask[]>([])
const pollingTimers = new Map<string, ReturnType<typeof setInterval>>()

// 获取路由参数中的 id
function getKbId(): number {
  return Number(route.params.id)
}

// ---------- 加载文档列表 ----------
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

// ---------- 文件上传成功回调 ----------
function handleDocUploaded(data: DocumentUploadResponse) {
  // 添加到进度追踪列表
  const task: ProgressTask = {
    taskId: data.taskId,
    fileName: data.fileName,
    status: null,
  }
  progressTasks.push(task)
  // 开始轮询该任务
  startTaskPolling(task)
}

// ---------- 全部上传完成回调 ----------
function handleAllUploadDone() {
  // 刷新文档列表以显示新上传的文档（即使它们还在 PROCESSING 中）
  loadDocuments()
}

// ---------- 轮询单个任务的进度 ----------
function startTaskPolling(task: ProgressTask) {
  // 立即查询一次
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
      // 终态：停止轮询
      stopTaskPolling(task.taskId)

      if (state === 'COMPLETED') {
        ElMessage.success(`「${task.fileName}」处理完成`)
      } else {
        const reason = task.status?.error || task.status?.message || '处理失败'
        ElMessage.error(`「${task.fileName}」处理失败：${reason}`)
      }

      // 刷新文档列表和统计
      loadDocuments()
      kbStore.fetchStatistics(getKbId())
      kbStore.fetchById(getKbId()) // 刷新 documentCount

      // 3 秒后从进度列表中移除已完成的任务
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

// ---------- 加载知识库详情 + 统计 ----------
async function loadDetail() {
  const id = getKbId()
  if (!id || isNaN(id)) {
    ElMessage.error('无效的知识库 ID')
    router.push('/knowledge-base')
    return
  }
  try {
    await kbStore.fetchById(id)
    statsLoading.value = true
    await kbStore.fetchStatistics(id)
    statsLoading.value = false
    // 同时加载文档列表
    await loadDocuments()
  } catch {
    router.push('/knowledge-base')
  }
}

// 页面初始化
onMounted(loadDetail)

// 路由参数变化时重新加载
watch(() => route.params.id, () => {
  if (route.params.id) {
    stopAllPolling()
    progressTasks.splice(0)
    loadDetail()
  }
})

// 组件销毁时清理所有轮询
onUnmounted(() => {
  stopAllPolling()
})

// 编辑成功后刷新
function handleEditSuccess() {
  loadDetail()
}

// 确认删除
async function confirmDelete() {
  if (!kbStore.current) return
  try {
    await ElMessageBox.confirm(
      `确定要删除知识库「${kbStore.current.name}」吗？\n该操作不可恢复！`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning', confirmButtonClass: 'el-button--danger' }
    )
    await kbStore.remove(kbStore.current.id)
    router.push('/knowledge-base')
  } catch {
    // 用户取消
  }
}
</script>

<style scoped>
.kb-detail-view {
  max-width: 960px;
  padding: var(--rag-space-6);
}

/* 顶部导航头 */
.detail-header {
  display: flex;
  align-items: center;
  gap: var(--rag-space-4);
  margin-bottom: var(--rag-space-6);
}

.back-btn {
  width: 36px;
  height: 36px;
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
}

.title-text {
  font-size: 20px;
  font-weight: 600;
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
  gap: var(--rag-space-2);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: var(--rag-radius-sm);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn.small {
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
  background: transparent;
  color: var(--rag-danger);
  border: 1px solid var(--rag-danger);
}

.action-btn.danger:hover {
  background: rgba(239, 68, 68, 0.1);
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: var(--rag-space-6);
}

/* 信息卡片 V2 */
.info-card-v2 {
  background: var(--rag-bg-card);
  border: 1px solid var(--rag-border);
  border-radius: var(--rag-radius-lg);
  padding: var(--rag-space-6);
}

.card-header-v2 {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: var(--rag-space-4);
}

.card-header-v2 .header-icon {
  color: var(--rag-primary);
}

.card-header-v2.doc-header {
  justify-content: space-between;
}

.card-header-v2.doc-header .header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-section-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--rag-text-primary);
}

/* 信息网格 */
.info-grid {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.info-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--rag-space-6);
  padding: var(--rag-space-3) 0;
}

.info-row.full {
  grid-template-columns: 1fr;
}

.info-divider {
  height: 1px;
  background: var(--rag-border-light);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: var(--rag-text-placeholder);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.info-value {
  font-size: 14px;
  color: var(--rag-text-primary);
  font-weight: 500;
}

.info-value.desc {
  color: var(--rag-text-secondary);
  font-weight: 400;
  line-height: 1.5;
}

.info-value.mono {
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 13px;
  color: var(--rag-text-secondary);
}

.info-value.highlight {
  color: var(--rag-primary);
}

.upload-section {
  margin-bottom: var(--rag-space-4);
}

.section-divider {
  height: 1px;
  background: var(--rag-border-light);
  margin: var(--rag-space-4) 0;
}
</style>
