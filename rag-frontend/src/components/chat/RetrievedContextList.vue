<template>
  <div class="context-list">
    <div class="context-header" @click="expanded = !expanded">
      <el-icon :size="14"><Collection /></el-icon>
      <span class="context-title">检索上下文 ({{ contexts.length }})</span>
      <el-icon :size="12" :class="['expand-icon', { expanded }]">
        <ArrowRight />
      </el-icon>
    </div>

    <el-collapse-transition>
      <div v-show="expanded" class="context-items">
        <div
          v-for="(context, idx) in contexts"
          :key="idx"
          class="context-item"
        >
          <div class="context-topline">
            <span class="context-source">{{ context.source || '未知来源' }}</span>
            <span class="context-score">相关度 {{ formatScore(context.relevanceScore) }}</span>
          </div>
          <p class="context-content">{{ context.content }}</p>
        </div>
      </div>
    </el-collapse-transition>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ArrowRight, Collection } from '@element-plus/icons-vue'
import type { RetrievedContext } from '@/types/qa'

defineProps<{
  contexts: RetrievedContext[]
}>()

const expanded = ref(false)

function formatScore(score: number) {
  if (Number.isNaN(score)) {
    return '-'
  }
  return score.toFixed(3)
}
</script>

<style scoped>
.context-list {
  margin-top: var(--rag-space-3);
  border: 1px solid var(--rag-border);
  border-radius: var(--rag-radius-sm);
  overflow: hidden;
  background: var(--rag-bg-surface);
}

.context-header {
  display: flex;
  align-items: center;
  gap: var(--rag-space-2);
  padding: var(--rag-space-2) var(--rag-space-3);
  cursor: pointer;
  font-size: var(--rag-font-small);
  color: var(--rag-text-secondary);
  transition: background 0.2s ease;
  user-select: none;
}

.context-header:hover {
  background: var(--rag-bg-hover);
}

.context-title {
  flex: 1;
  font-weight: 500;
}

.expand-icon {
  transition: transform 0.3s;
}

.expand-icon.expanded {
  transform: rotate(90deg);
}

.context-items {
  border-top: 1px solid var(--rag-border-light);
}

.context-item {
  padding: var(--rag-space-3) var(--rag-space-4);
  border-bottom: 1px solid var(--rag-border-light);
}

.context-item:last-child {
  border-bottom: none;
}

.context-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--rag-space-2);
  margin-bottom: var(--rag-space-1);
}

.context-source {
  font-size: var(--rag-font-small);
  font-weight: 500;
  color: var(--rag-text-regular);
}

.context-score {
  font-size: 11px;
  color: var(--rag-text-placeholder);
}

.context-content {
  margin: 0;
  font-size: var(--rag-font-small);
  color: var(--rag-text-secondary);
  line-height: var(--rag-line-height);
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
