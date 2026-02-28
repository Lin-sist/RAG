<template>
  <el-card class="kb-card" shadow="hover" @click="emit('click')">
    <!-- 卡片头部：名称 + 操作 -->
    <template #header>
      <div class="card-header">
        <div class="card-title-row">
          <el-icon class="card-icon" :size="20"><Collection /></el-icon>
          <span class="card-title">{{ kb.name }}</span>
          <el-tag :type="kb.isPublic ? 'success' : 'info'" size="small" class="visibility-tag">
            {{ kb.isPublic ? '公开' : '私有' }}
          </el-tag>
        </div>
        <el-dropdown trigger="click" @command="handleCommand" @click.stop>
          <el-icon class="more-btn"><MoreFilled /></el-icon>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="edit"><el-icon><Edit /></el-icon>编辑</el-dropdown-item>
              <el-dropdown-item command="stats"><el-icon><DataAnalysis /></el-icon>统计</el-dropdown-item>
              <el-dropdown-item command="delete" divided>
                <span style="color: #f56c6c;"><el-icon><Delete /></el-icon>删除</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </template>

    <!-- 卡片内容：描述 + 底部信息 -->
    <p class="card-desc">{{ kb.description || '暂无描述' }}</p>
    <div class="card-footer">
      <span class="footer-item">
        <el-icon><Document /></el-icon>
        {{ kb.documentCount }} 篇文档
      </span>
      <span class="footer-item">
        <el-icon><Clock /></el-icon>
        {{ formatDate(kb.createdAt) }}
      </span>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { Collection, MoreFilled, Edit, Delete, DataAnalysis, Document, Clock } from '@element-plus/icons-vue'
import type { KnowledgeBaseDTO } from '@/types/knowledgeBase'
import { formatDate } from '@/utils/format'

defineProps<{ kb: KnowledgeBaseDTO }>()

const emit = defineEmits<{
  click: []
  edit: []
  delete: []
  stats: []
}>()

function handleCommand(cmd: string) {
  if (cmd === 'edit') emit('edit')
  else if (cmd === 'delete') emit('delete')
  else if (cmd === 'stats') emit('stats')
}
</script>

<style scoped>
.kb-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  height: 100%;
}
.kb-card:hover {
  transform: translateY(-2px);
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
}
.card-icon {
  color: var(--rag-primary, #409eff);
  flex-shrink: 0;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.visibility-tag {
  flex-shrink: 0;
}
.more-btn {
  font-size: 18px;
  color: #909399;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
}
.more-btn:hover {
  color: var(--rag-primary, #409eff);
  background: #f0f2f5;
}
.card-desc {
  color: #909399;
  font-size: 13px;
  line-height: 1.6;
  margin: 0 0 16px;
  min-height: 42px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-footer {
  display: flex;
  align-items: center;
  gap: 20px;
  color: #909399;
  font-size: 12px;
  border-top: 1px solid #f0f2f5;
  padding-top: 12px;
}
.footer-item {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
