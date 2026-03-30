<template>
  <div class="kb-list-view">
    <div class="page-header">
      <h1>知识库管理</h1>
      <button class="create-btn" @click="dialogVisible = true">
        <Plus :size="18" />
        <span>创建知识库</span>
      </button>
    </div>

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

    <KBCreateDialog
      v-model:visible="dialogVisible"
      :edit-data="editTarget"
      @success="handleDialogSuccess"
    />

    <el-dialog v-model="statsDialogVisible" title="知识库统计" width="520px" destroy-on-close>
      <KBStatsPanel :stats="kbStore.statistics" :loading="statsLoading" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Plus } from 'lucide-vue-next'
import { useKnowledgeBaseStore } from '@/stores/knowledgeBase'
import type { KnowledgeBaseDTO } from '@/types/knowledgeBase'
import KBCard from '@/components/knowledge-base/KBCard.vue'
import KBCreateDialog from '@/components/knowledge-base/KBCreateDialog.vue'
import KBStatsPanel from '@/components/knowledge-base/KBStatsPanel.vue'

const router = useRouter()
const kbStore = useKnowledgeBaseStore()

const dialogVisible = ref(false)
const editTarget = ref<KnowledgeBaseDTO | null>(null)
const statsDialogVisible = ref(false)
const statsLoading = ref(false)

onMounted(() => {
  kbStore.fetchList()
})

function goDetail(id: number) {
  router.push(`/kb/${id}`)
}

function openEdit(kb: KnowledgeBaseDTO) {
  editTarget.value = kb
  dialogVisible.value = true
}

function handleDialogSuccess() {
  editTarget.value = null
  kbStore.fetchList()
}

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

async function openStats(kb: KnowledgeBaseDTO) {
  statsDialogVisible.value = true
  statsLoading.value = true
  await kbStore.fetchStatistics(kb.id)
  statsLoading.value = false
}
</script>

<style scoped>
.kb-list-view {
  height: 100%;
  overflow-y: auto;
  padding: var(--rag-space-6);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--rag-space-8);
}

.page-header h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: var(--rag-text-primary);
  letter-spacing: -0.02em;
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: var(--rag-primary);
  color: #fff;
  border: none;
  border-radius: var(--rag-radius-sm);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.create-btn:hover {
  background: var(--rag-primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--rag-shadow-sm);
}

:deep(.el-empty) {
  padding: var(--rag-space-12);
}

:deep(.el-empty__description p) {
  color: var(--rag-text-secondary);
  font-size: 14px;
}
</style>
