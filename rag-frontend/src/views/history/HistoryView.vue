<template>
  <div class="history-view">
    <!-- 页面标题 + 筛选 -->
    <div class="page-header">
      <h2>问答历史</h2>
      <div class="header-actions">
        <el-select
          v-model="filterKbId"
          placeholder="全部知识库"
          clearable
          style="width: 200px;"
          @change="handleFilterChange"
        >
          <el-option
            v-for="kb in kbList"
            :key="kb.id"
            :label="kb.name"
            :value="kb.id"
          />
        </el-select>
      </div>
    </div>

    <!-- 历史列表 -->
    <el-card shadow="never" class="history-card" v-loading="loading">
      <template v-if="historyList.length > 0">
        <HistoryItem
          v-for="item in historyList"
          :key="item.id"
          :item="item"
          @click="openDetail(item)"
          @feedback="openFeedback(item)"
          @delete="confirmDelete(item)"
        />
      </template>
      <el-empty v-else description="暂无问答历史记录" />

      <!-- 分页 -->
      <div v-if="total > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="fetchPage"
          @current-change="fetchPage"
        />
      </div>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="问答详情" size="560px" destroy-on-close>
      <template v-if="detailItem">
        <div class="detail-section">
          <h4 class="detail-label">提问</h4>
          <p class="detail-text question-text">{{ detailItem.question }}</p>
        </div>

        <el-divider />

        <div class="detail-section">
          <h4 class="detail-label">回答</h4>
          <div class="detail-answer" v-html="renderMarkdown(detailItem.answer)" />
        </div>

        <template v-if="detailItem.citations && detailItem.citations.length > 0">
          <el-divider />
          <div class="detail-section">
            <h4 class="detail-label">引用来源 ({{ detailItem.citations.length }})</h4>
            <div
              v-for="(cite, idx) in detailItem.citations"
              :key="idx"
              class="citation-card"
            >
              <div class="citation-source">
                <el-icon><Document /></el-icon>
                {{ cite.source }}
              </div>
              <div class="citation-snippet">{{ cite.snippet }}</div>
            </div>
          </div>
        </template>

        <el-divider />

        <div class="detail-section">
          <h4 class="detail-label">元信息</h4>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="记录 ID">{{ detailItem.id }}</el-descriptions-item>
            <el-descriptions-item label="知识库 ID">{{ detailItem.kbId }}</el-descriptions-item>
            <el-descriptions-item label="响应延迟">{{ detailItem.latencyMs }} ms</el-descriptions-item>
            <el-descriptions-item label="Trace ID">
              <el-text size="small" truncated>{{ detailItem.traceId }}</el-text>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDate(detailItem.createdAt) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 反馈列表 -->
        <template v-if="detailFeedbacks.length > 0">
          <el-divider />
          <div class="detail-section">
            <h4 class="detail-label">已提交反馈</h4>
            <div v-for="fb in detailFeedbacks" :key="fb.id" class="feedback-card">
              <el-rate :model-value="fb.rating" disabled />
              <p v-if="fb.comment" class="feedback-comment">{{ fb.comment }}</p>
              <span class="feedback-time">{{ formatDate(fb.createdAt) }}</span>
            </div>
          </div>
        </template>

        <div class="detail-footer">
          <el-button type="primary" :icon="Star" @click="openFeedback(detailItem)">
            提交反馈
          </el-button>
          <el-button type="danger" plain :icon="Delete" @click="confirmDelete(detailItem)">
            删除记录
          </el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 反馈弹窗 -->
    <FeedbackDialog
      v-if="feedbackTarget"
      v-model:visible="feedbackDialogVisible"
      :history-id="feedbackTarget.id"
      :question-text="feedbackTarget.question"
      @success="handleFeedbackSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Star, Delete } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import { getHistoryPage, deleteHistory, getFeedback } from '@/api/history'
import { listKB } from '@/api/knowledgeBase'
import type { QAHistoryDTO, QAFeedbackDTO } from '@/types/history'
import type { KnowledgeBaseDTO } from '@/types/knowledgeBase'
import { formatDate } from '@/utils/format'
import HistoryItem from '@/components/history/HistoryItem.vue'
import FeedbackDialog from '@/components/history/FeedbackDialog.vue'

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })
function renderMarkdown(text: string): string {
  return md.render(text || '')
}

// ---------- 列表状态 ----------
const historyList = ref<QAHistoryDTO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const loading = ref(false)

// ---------- 筛选 ----------
const filterKbId = ref<number | undefined>(undefined)
const kbList = ref<KnowledgeBaseDTO[]>([])

// ---------- 详情抽屉 ----------
const drawerVisible = ref(false)
const detailItem = ref<QAHistoryDTO | null>(null)
const detailFeedbacks = ref<QAFeedbackDTO[]>([])

// ---------- 反馈弹窗 ----------
const feedbackDialogVisible = ref(false)
const feedbackTarget = ref<QAHistoryDTO | null>(null)

// ---------- 初始化 ----------
onMounted(async () => {
  await fetchPage()
  // 加载知识库列表用于筛选下拉
  try {
    const res = await listKB()
    kbList.value = res.data.data
  } catch {
    // 不阻塞主流程
  }
})

// ---------- 拉取分页数据 ----------
async function fetchPage() {
  loading.value = true
  try {
    const res = await getHistoryPage(currentPage.value, pageSize.value, filterKbId.value)
    const page = res.data.data
    historyList.value = page.records
    total.value = page.total
  } catch {
    ElMessage.error('获取问答历史失败')
  } finally {
    loading.value = false
  }
}

function handleFilterChange() {
  currentPage.value = 1
  fetchPage()
}

// ---------- 详情抽屉 ----------
async function openDetail(item: QAHistoryDTO) {
  detailItem.value = item
  drawerVisible.value = true
  // 同时加载反馈列表
  detailFeedbacks.value = []
  try {
    const res = await getFeedback(item.id)
    detailFeedbacks.value = res.data.data
  } catch {
    // 无反馈也不报错
  }
}

// ---------- 反馈 ----------
function openFeedback(item: QAHistoryDTO) {
  feedbackTarget.value = item
  feedbackDialogVisible.value = true
}

function handleFeedbackSuccess() {
  // 刷新详情抽屉里的反馈列表
  if (detailItem.value) {
    getFeedback(detailItem.value.id).then(res => {
      detailFeedbacks.value = res.data.data
    }).catch(() => {})
  }
}

// ---------- 删除 ----------
async function confirmDelete(item: QAHistoryDTO) {
  try {
    await ElMessageBox.confirm(
      `确定删除这条问答记录吗？\n「${item.question.substring(0, 40)}...」`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning', confirmButtonClass: 'el-button--danger' }
    )
    await deleteHistory(item.id)
    ElMessage.success('删除成功')
    // 如果是在抽屉里删的，关闭抽屉
    if (drawerVisible.value && detailItem.value?.id === item.id) {
      drawerVisible.value = false
      detailItem.value = null
    }
    await fetchPage()
  } catch {
    // 用户取消
  }
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.history-card {
  border-radius: 8px;
}

.history-card :deep(.el-card__body) {
  padding: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 16px 20px;
  border-top: 1px solid #ebeef5;
}

/* ========== 详情抽屉 ========== */
.detail-section {
  margin-bottom: 8px;
}

.detail-label {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 10px;
}

.detail-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.7;
  margin: 0;
}

.question-text {
  background: #f0f7ff;
  padding: 10px 14px;
  border-radius: 6px;
  border-left: 3px solid #409eff;
}

.detail-answer {
  font-size: 14px;
  color: #303133;
  line-height: 1.7;
}

.detail-answer :deep(p) {
  margin: 0 0 8px;
}

.detail-answer :deep(pre) {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  font-size: 13px;
}

.detail-answer :deep(code) {
  font-family: 'Consolas', 'Monaco', monospace;
}

.detail-answer :deep(:not(pre) > code) {
  background: rgba(0, 0, 0, 0.06);
  padding: 2px 6px;
  border-radius: 4px;
}

/* 引用来源卡片 */
.citation-card {
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 10px 14px;
  margin-bottom: 8px;
}

.citation-source {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #409eff;
  margin-bottom: 6px;
}

.citation-snippet {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

/* 反馈卡片 */
.feedback-card {
  background: #fdf6ec;
  border-radius: 6px;
  padding: 10px 14px;
  margin-bottom: 8px;
}

.feedback-comment {
  font-size: 13px;
  color: #606266;
  margin: 6px 0 4px;
}

.feedback-time {
  font-size: 12px;
  color: #c0c4cc;
}

.detail-footer {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}
</style>
