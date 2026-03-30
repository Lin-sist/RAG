<template>
  <div v-loading="loading" class="stats-panel">
    <div class="panel-header">
      <DataAnalysis class="header-icon" />
      <span>统计概览</span>
    </div>
    <div class="stats-grid" v-if="stats">
      <div class="stat-card">
        <div class="stat-icon doc-icon">
          <Document />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.documentCount }}</div>
          <div class="stat-label">文档数量</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon vector-icon">
          <Cpu />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.vectorCount }}</div>
          <div class="stat-label">向量数量</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon query-icon">
          <Search />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.queryCount }}</div>
          <div class="stat-label">查询次数</div>
        </div>
      </div>
    </div>
    <div v-else class="stats-empty">
      <span>暂无统计数据</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { DataAnalysis, Document, Cpu, Search } from '@element-plus/icons-vue'
import type { KnowledgeBaseStatistics } from '@/types/knowledgeBase'

defineProps<{
  stats: KnowledgeBaseStatistics | null
  loading?: boolean
}>()
</script>

<style scoped>
.stats-panel {
  background: var(--rag-bg-card);
  border: 1px solid var(--rag-border);
  border-radius: var(--rag-radius-lg);
  padding: var(--rag-space-6);
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  font-size: 15px;
  color: var(--rag-text-primary);
  margin-bottom: var(--rag-space-6);
}

.header-icon {
  width: 20px;
  height: 20px;
  color: var(--rag-primary);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--rag-space-4);
}

.stat-card {
  display: flex;
  align-items: center;
  gap: var(--rag-space-4);
  padding: var(--rag-space-4);
  background: var(--rag-bg-hover);
  border-radius: var(--rag-radius-md);
  transition: all 0.2s ease;
}

.stat-card:hover {
  background: var(--rag-bg-surface);
  box-shadow: var(--rag-shadow-sm);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--rag-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon svg {
  width: 24px;
  height: 24px;
}

.doc-icon {
  background: rgba(16, 163, 127, 0.1);
  color: var(--rag-primary);
}

.vector-icon {
  background: rgba(103, 194, 58, 0.1);
  color: #67c23a;
}

.query-icon {
  background: rgba(230, 162, 60, 0.1);
  color: #e6a23c;
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--rag-text-primary);
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.stat-label {
  font-size: 13px;
  color: var(--rag-text-secondary);
}

.stats-empty {
  text-align: center;
  padding: var(--rag-space-8);
  color: var(--rag-text-placeholder);
  font-size: 14px;
}
</style>
