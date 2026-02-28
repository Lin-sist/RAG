<!-- 引用来源展示 — Design V2 -->
<template>
  <div class="citation-list">
    <div class="citation-header" @click="expanded = !expanded">
      <el-icon :size="14"><Document /></el-icon>
      <span class="citation-title">引用来源 ({{ citations.length }})</span>
      <el-icon :size="12" :class="['expand-icon', { expanded }]">
        <ArrowRight />
      </el-icon>
    </div>

    <el-collapse-transition>
      <div v-show="expanded" class="citation-items">
        <div
          v-for="(cite, idx) in citations"
          :key="idx"
          class="citation-item"
        >
          <div class="cite-source">
            <el-icon :size="12"><Paperclip /></el-icon>
            <span class="source-name">{{ cite.source || '未知来源' }}</span>
          </div>
          <p class="cite-snippet">{{ cite.snippet }}</p>
        </div>
      </div>
    </el-collapse-transition>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Document, ArrowRight, Paperclip } from '@element-plus/icons-vue'
import type { Citation } from '@/types/qa'

defineProps<{
  citations: Citation[]
}>()

const expanded = ref(false)
</script>

<style scoped>
.citation-list {
  margin-top: var(--rag-space-3);
  border: 1px solid var(--rag-border);
  border-radius: var(--rag-radius-sm);
  overflow: hidden;
  background: var(--rag-bg-surface);
}

.citation-header {
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

.citation-header:hover {
  background: var(--rag-bg-hover);
}

.citation-title {
  flex: 1;
  font-weight: 500;
}

.expand-icon {
  transition: transform 0.3s;
}

.expand-icon.expanded {
  transform: rotate(90deg);
}

.citation-items {
  border-top: 1px solid var(--rag-border-light);
}

.citation-item {
  padding: var(--rag-space-3) var(--rag-space-4);
  border-bottom: 1px solid var(--rag-border-light);
  transition: background 0.2s ease;
}

.citation-item:last-child {
  border-bottom: none;
}

.citation-item:hover {
  background: var(--rag-bg-hover);
}

.cite-source {
  display: flex;
  align-items: center;
  gap: var(--rag-space-1);
  margin-bottom: var(--rag-space-1);
}

.source-name {
  font-size: var(--rag-font-small);
  font-weight: 500;
  color: var(--rag-text-regular);
}

.cite-snippet {
  margin: 0;
  font-size: var(--rag-font-small);
  color: var(--rag-text-secondary);
  line-height: var(--rag-line-height);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
