<template>
  <div class="kb-card" @click="emit('click')">
    <!-- 顶部渐变装饰条 -->
    <div class="card-gradient-bar" />

    <!-- 卡片头部 -->
    <div class="card-header">
      <div class="card-icon-circle">
        <el-icon :size="24"><Collection /></el-icon>
      </div>
      <div class="card-header-right">
        <el-tag :type="kb.isPublic ? 'success' : 'info'" size="small" effect="plain" round>
          {{ kb.isPublic ? '公开' : '私有' }}
        </el-tag>
        <el-dropdown trigger="click" @command="handleCommand" @click.stop>
          <el-icon class="more-btn"><MoreFilled /></el-icon>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="edit"><el-icon><Edit /></el-icon>编辑</el-dropdown-item>
              <el-dropdown-item command="stats"><el-icon><DataAnalysis /></el-icon>统计</el-dropdown-item>
              <el-dropdown-item command="delete" divided>
                <span style="color: var(--rag-danger);"><el-icon><Delete /></el-icon>删除</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 名称 -->
    <h3 class="card-title">{{ kb.name }}</h3>

    <!-- 描述 -->
    <p class="card-desc">{{ kb.description || '暂无描述' }}</p>

    <!-- 底部信息 -->
    <div class="card-footer">
      <div class="footer-badge">
        <el-icon :size="13"><Document /></el-icon>
        <span>{{ kb.documentCount }} 篇文档</span>
      </div>
      <span class="footer-time">
        <el-icon :size="13"><Clock /></el-icon>
        {{ formatDate(kb.createdAt) }}
      </span>
    </div>
  </div>
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
  background: var(--rag-bg-surface);
  border: 1px solid var(--rag-border);
  border-radius: var(--rag-radius-md);
  padding: 0;
  overflow: hidden;
  transition: var(--rag-transition);
  height: 100%;
  display: flex;
  flex-direction: column;
}

.kb-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--rag-shadow-md);
  border-color: var(--rag-primary-light);
}

/* 顶部渐变装饰条 */
.card-gradient-bar {
  height: 4px;
  background: var(--rag-gradient);
  flex-shrink: 0;
}

.kb-card:hover .card-gradient-bar {
  height: 5px;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: var(--rag-space-4) var(--rag-space-4) 0;
}

.card-icon-circle {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.1), rgba(14, 165, 233, 0.1));
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--rag-primary);
  flex-shrink: 0;
}

.card-header-right {
  display: flex;
  align-items: center;
  gap: var(--rag-space-2);
}

.more-btn {
  font-size: 18px;
  color: var(--rag-text-placeholder);
  cursor: pointer;
  padding: var(--rag-space-1);
  border-radius: var(--rag-space-1);
  transition: var(--rag-transition);
}

.more-btn:hover {
  color: var(--rag-primary);
  background: var(--rag-bg-hover);
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--rag-text-primary);
  margin: var(--rag-space-3) var(--rag-space-4) 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-desc {
  color: var(--rag-text-secondary);
  font-size: 13px;
  line-height: var(--rag-line-height);
  margin: var(--rag-space-2) var(--rag-space-4) 0;
  min-height: 42px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--rag-space-3) var(--rag-space-4);
  margin-top: auto;
  border-top: 1px solid var(--rag-border-light);
  font-size: var(--rag-font-small);
  color: var(--rag-text-secondary);
}

.footer-badge {
  display: flex;
  align-items: center;
  gap: var(--rag-space-1);
  background: rgba(79, 70, 229, 0.08);
  color: var(--rag-primary);
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.footer-time {
  display: flex;
  align-items: center;
  gap: var(--rag-space-1);
}
</style>
