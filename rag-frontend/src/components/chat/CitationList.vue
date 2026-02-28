<!-- 引用来源展示 — 阶段 7 -->
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
            <el-icon :size="12" color="#909399"><Paperclip /></el-icon>
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
  margin-top: 8px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.citation-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  cursor: pointer;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  transition: background 0.2s;
  user-select: none;
}

.citation-header:hover {
  background: #f5f7fa;
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
  border-top: 1px solid #ebeef5;
}

.citation-item {
  padding: 10px 14px;
  border-bottom: 1px solid #f0f0f0;
}

.citation-item:last-child {
  border-bottom: none;
}

.cite-source {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 4px;
}

.source-name {
  font-size: 12px;
  font-weight: 500;
  color: #606266;
}

.cite-snippet {
  margin: 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
