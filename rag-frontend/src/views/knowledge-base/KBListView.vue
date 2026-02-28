<template>
  <div class="kb-list-view">
    <!-- 页面标题 + 创建按钮 -->
    <div class="page-header">
      <h2>知识库管理</h2>
      <el-button type="primary" :icon="Plus" @click="dialogVisible = true">创建知识库</el-button>
    </div>

    <!-- 知识库卡片网格 -->
    <div v-loading="kbStore.loading">
      <el-row v-if="kbStore.list.length > 0" :gutter="20">
        <el-col
          v-for="kb in kbStore.list"
          :key="kb.id"
          :xs="24" :sm="12" :md="8" :lg="6"
          style="margin-bottom: 20px;"
        >
          <KBCard
            :kb="kb"
            @click="goDetail(kb.id)"
            @edit="openEdit(kb)"
            @delete="confirmDelete(kb)"
            @stats="openStats(kb)"
          />
        </el-col>
      </el-row>
      <el-empty v-else description="暂无知识库，请先创建一个">
        <el-button type="primary" @click="dialogVisible = true">创建知识库</el-button>
      </el-empty>
    </div>

    <!-- 创建/编辑弹窗 -->
    <KBCreateDialog
      v-model:visible="dialogVisible"
      :edit-data="editTarget"
      @success="handleDialogSuccess"
    />

    <!-- 统计面板弹窗 -->
    <el-dialog v-model="statsDialogVisible" title="知识库统计" width="520px" destroy-on-close>
      <KBStatsPanel :stats="kbStore.statistics" :loading="statsLoading" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useKnowledgeBaseStore } from '@/stores/knowledgeBase'
import type { KnowledgeBaseDTO } from '@/types/knowledgeBase'
import KBCard from '@/components/knowledge-base/KBCard.vue'
import KBCreateDialog from '@/components/knowledge-base/KBCreateDialog.vue'
import KBStatsPanel from '@/components/knowledge-base/KBStatsPanel.vue'

const router = useRouter()
const kbStore = useKnowledgeBaseStore()

// ----- 弹窗控制 -----
const dialogVisible = ref(false)
const editTarget = ref<KnowledgeBaseDTO | null>(null)
const statsDialogVisible = ref(false)
const statsLoading = ref(false)

// ----- 页面初始化：加载知识库列表 -----
onMounted(() => {
  kbStore.fetchList()
})

// ----- 跳转详情页 -----
function goDetail(id: number) {
  router.push(`/knowledge-base/${id}`)
}

// ----- 打开编辑弹窗 -----
function openEdit(kb: KnowledgeBaseDTO) {
  editTarget.value = kb
  dialogVisible.value = true
}

// ----- 弹窗关闭后重置编辑对象 -----
function handleDialogSuccess() {
  editTarget.value = null
  kbStore.fetchList() // 刷新列表
}

// ----- 确认删除 -----
async function confirmDelete(kb: KnowledgeBaseDTO) {
  try {
    await ElMessageBox.confirm(
      `确定要删除知识库「${kb.name}」吗？\n该操作会同时删除知识库下所有文档和向量数据，不可恢复！`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning', confirmButtonClass: 'el-button--danger' }
    )
    await kbStore.remove(kb.id)
  } catch {
    // 用户取消
  }
}

// ----- 打开统计面板 -----
async function openStats(kb: KnowledgeBaseDTO) {
  statsDialogVisible.value = true
  statsLoading.value = true
  await kbStore.fetchStatistics(kb.id)
  statsLoading.value = false
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
</style>
