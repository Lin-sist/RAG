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

      <!-- 文档管理区域（阶段6实现） -->
      <el-card shadow="never" class="info-card">
        <template #header>
          <div class="doc-header">
            <span class="card-section-title">文档管理</span>
            <el-button type="primary" size="small" disabled>
              <el-icon><Upload /></el-icon>上传文档（阶段6）
            </el-button>
          </div>
        </template>
        <el-empty description="文档管理功能将在阶段 6 实现" :image-size="80" />
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
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Edit, Delete, Upload } from '@element-plus/icons-vue'
import { useKnowledgeBaseStore } from '@/stores/knowledgeBase'
import { formatDate } from '@/utils/format'
import KBStatsPanel from '@/components/knowledge-base/KBStatsPanel.vue'
import KBCreateDialog from '@/components/knowledge-base/KBCreateDialog.vue'

const router = useRouter()
const route = useRoute()
const kbStore = useKnowledgeBaseStore()

const editDialogVisible = ref(false)
const statsLoading = ref(false)

// 获取路由参数中的 id
function getKbId(): number {
  return Number(route.params.id)
}

// 加载知识库详情 + 统计
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
  } catch {
    router.push('/knowledge-base')
  }
}

// 页面初始化
onMounted(loadDetail)

// 路由参数变化时重新加载
watch(() => route.params.id, () => {
  if (route.params.id) loadDetail()
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
</style>
