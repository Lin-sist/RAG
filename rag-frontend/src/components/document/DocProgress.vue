<!-- 文档处理进度条 — 阶段 6 -->
<template>
  <div class="doc-progress" v-if="props.tasks.length > 0">
    <div class="progress-title">
      <el-icon class="rotating"><Loading /></el-icon>
      <span>正在处理（{{ activeTasks }} / {{ props.tasks.length }}）</span>
    </div>

    <div class="progress-list">
      <div v-for="task in props.tasks" :key="task.taskId" class="progress-item">
        <div class="progress-info">
          <span class="file-name">{{ task.fileName }}</span>
          <span class="progress-message">{{ task.status?.message || '等待中...' }}</span>
        </div>
        <el-progress
          :percentage="task.status?.progress ?? 0"
          :status="progressStatus(task)"
          :stroke-width="14"
          striped
          :striped-flow="isActive(task)"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import type { TaskStatusResponse } from '@/types/task'

export interface ProgressTask {
  taskId: string
  fileName: string
  status: TaskStatusResponse | null
}

const props = defineProps<{
  tasks: ProgressTask[]
}>()

/** 进度条的 Element Plus status */
function progressStatus(task: ProgressTask): '' | 'success' | 'exception' | 'warning' {
  if (!task.status) return ''
  const state = task.status.state
  if (state === 'COMPLETED') return 'success'
  if (state === 'FAILED' || state === 'CANCELLED') return 'exception'
  return ''
}

/** 任务是否还在活跃执行 */
function isActive(task: ProgressTask): boolean {
  if (!task.status) return true
  return task.status.state === 'PENDING' || task.status.state === 'RUNNING'
}

/** 还在运行的任务数 */
const activeTasks = computed(() => {
  return props.tasks.filter(t => isActive(t)).length
})
</script>

<style scoped>
.doc-progress {
  background: var(--el-fill-color-lighter, #fafafa);
  border-radius: 8px;
  padding: 16px;
}
.progress-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--el-color-primary);
}
.progress-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.progress-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}
.file-name {
  font-weight: 500;
  color: var(--el-text-color-primary);
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.progress-message {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.rotating {
  animation: spin 1.2s linear infinite;
}
</style>
