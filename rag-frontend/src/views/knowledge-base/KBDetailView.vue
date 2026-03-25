<template>
  <div class="kb-detail-view" v-loading="kbStore.loading">
    <!-- 顶部返回导航 -->
    <el-page-header @back="router.push('/knowledge-base')">
      <template #content>
        <div class="detail-title">
          <span>{{ kbStore.current?.name || '知识库详情' }}</span>
          <el-tag v-if="kbStore.current" :type="kbStore.current.isPublic ? 'success' : 'info'" size="small">
            {{ kbStore.current.isPublic ? '公开' : '私有' }}
          </el-tag>
        </div>
      </template>
      <template #extra>
        <el-button-group>
          <el-button type="primary" :icon="Edit" @click="editDialogVisible = true">编辑</el-button>
          <el-button type="danger" :icon="Delete" @click="confirmDelete">删除</el-button>
        </el-button-group>
      </template>
    </el-page-header>

    <div v-if="kbStore.current" class="detail-content">
      <!-- 基础信息卡片 -->
      <el-card shadow="never" class="info-card">
        <template #header>
          <span class="card-section-title">基本信息</span>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="知识库 ID">{{ kbStore.current.id }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ kbStore.current.name }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">
            {{ kbStore.current.description || '暂无描述' }}
          </el-descriptions-item>
          <el-descriptions-item label="向量集合">
            <el-tag type="info" size="small">{{ kbStore.current.vectorCollection }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="文档数量">
            <el-tag type="primary" size="small">{{ kbStore.current.documentCount }} 篇</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(kbStore.current.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDate(kbStore.current.updatedAt) }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 统计面板 -->
      <KBStatsPanel :stats="kbStore.statistics" :loading="statsLoading" />

      <!-- 文档管理区域 -->
      <el-card shadow="never" class="info-card">
        <template #header>
          <div class="doc-header">
            <span class="card-section-title">文档管理</span>
            <el-button type="primary" size="small" @click="uploadVisible = !uploadVisible">
              <el-icon><Upload /></el-icon>
              {{ uploadVisible ? '收起上传' : '上传文档' }}
            </el-button>
          </div>
        </template>

        <!-- 上传区域（可折叠） -->
        <div v-show="uploadVisible" class="upload-section">
          <DocUploader
            :kb-id="getKbId()"
            @uploaded="handleDocUploaded"
            @all-done="handleAllUploadDone"
          />
          <el-divider />
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
      </el-card>
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
import { Edit, Delete, Upload } from '@element-plus/icons-vue'
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
}
.detail-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
}
.detail-content {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.info-card {
  /* 无额外样式，使用默认 el-card */
}
.card-section-title {
  font-weight: 600;
  font-size: 15px;
}
.doc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.upload-section {
  margin-bottom: 8px;
}
</style>
