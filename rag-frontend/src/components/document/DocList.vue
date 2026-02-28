<!-- 文档列表组件 — 阶段 6 -->
<template>
  <div class="doc-list">
    <div class="doc-list-header">
      <span class="doc-count">共 {{ documents.length }} 篇文档</span>
      <el-button :icon="Refresh" circle size="small" @click="emit('refresh')" :loading="loading" />
    </div>

    <el-table
      v-if="documents.length > 0"
      :data="documents"
      stripe
      style="width: 100%"
      v-loading="loading"
    >
      <el-table-column prop="title" label="文档标题" min-width="200" show-overflow-tooltip />

      <el-table-column prop="fileType" label="类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.fileType }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="status" label="状态" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small" effect="dark">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="chunkCount" label="分块数" width="90" align="center">
        <template #default="{ row }">
          <span v-if="row.status === 'COMPLETED'">{{ row.chunkCount }}</span>
          <span v-else class="text-muted">—</span>
        </template>
      </el-table-column>

      <el-table-column prop="createdAt" label="上传时间" width="170" align="center">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>

      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <el-popconfirm
            title="确定删除该文档？"
            confirm-button-text="删除"
            cancel-button-text="取消"
            @confirm="handleDelete(row)"
          >
            <template #reference>
              <el-button type="danger" :icon="Delete" size="small" link :loading="row._deleting" />
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-else-if="!loading" description="暂无文档，请上传文件" :image-size="60" />
  </div>
</template>

<script setup lang="ts">
import { Refresh, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { formatDate } from '@/utils/format'
import { deleteDocument } from '@/api/knowledgeBase'
import type { DocumentInfo, DocumentStatus } from '@/types/document'

const props = defineProps<{
  kbId: number
  documents: DocumentInfo[]
  loading: boolean
}>()

const emit = defineEmits<{
  (e: 'refresh'): void
}>()

/** 状态 → Element Plus tag type 映射 */
function statusTagType(status: DocumentStatus): 'warning' | 'primary' | 'success' | 'danger' | 'info' {
  const map: Record<DocumentStatus, 'warning' | 'primary' | 'success' | 'danger'> = {
    PENDING: 'warning',
    PROCESSING: 'primary',
    COMPLETED: 'success',
    FAILED: 'danger',
  }
  return map[status] ?? 'info'
}

/** 状态 → 中文标签 */
function statusLabel(status: DocumentStatus): string {
  const map: Record<DocumentStatus, string> = {
    PENDING: '等待处理',
    PROCESSING: '处理中',
    COMPLETED: '已完成',
    FAILED: '处理失败',
  }
  return map[status] ?? status
}

/** 删除文档 */
async function handleDelete(doc: DocumentInfo & { _deleting?: boolean }) {
  doc._deleting = true
  try {
    await deleteDocument(props.kbId, doc.id)
    ElMessage.success('文档已删除')
    emit('refresh')
  } catch {
    ElMessage.error('删除失败')
  } finally {
    doc._deleting = false
  }
}
</script>

<style scoped>
.doc-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.doc-count {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.text-muted {
  color: var(--el-text-color-placeholder);
}
</style>
