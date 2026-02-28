<!-- 历史条目 — 阶段 8 -->
<template>
  <div class="history-item" @click="$emit('click')">
    <!-- 左侧：问答信息 -->
    <div class="item-body">
      <div class="item-question">
        <el-icon class="q-icon"><ChatDotRound /></el-icon>
        <span class="q-text">{{ item.question }}</span>
      </div>

      <div class="item-answer">
        {{ truncateText(item.answer, 120) }}
      </div>

      <div class="item-meta">
        <el-tag size="small" effect="plain" type="info">
          <el-icon style="vertical-align: -2px;"><Collection /></el-icon>
          KB #{{ item.kbId }}
        </el-tag>
        <span class="meta-item">
          <el-icon><Timer /></el-icon>
          {{ item.latencyMs }}ms
        </span>
        <span class="meta-item">
          <el-icon><Clock /></el-icon>
          {{ formatDate(item.createdAt) }}
        </span>
        <el-tag
          v-if="item.citations && item.citations.length > 0"
          size="small"
          type="success"
          effect="plain"
        >
          {{ item.citations.length }} 条引用
        </el-tag>
      </div>
    </div>

    <!-- 右侧：操作按钮 -->
    <div class="item-actions" @click.stop>
      <el-tooltip content="提交反馈" placement="top">
        <el-button text type="warning" :icon="Star" size="small" @click="$emit('feedback')" />
      </el-tooltip>
      <el-tooltip content="删除" placement="top">
        <el-button text type="danger" :icon="Delete" size="small" @click="$emit('delete')" />
      </el-tooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ChatDotRound, Collection, Timer, Clock, Star, Delete } from '@element-plus/icons-vue'
import type { QAHistoryDTO } from '@/types/history'
import { formatDate, truncateText } from '@/utils/format'

defineProps<{
  item: QAHistoryDTO
}>()

defineEmits<{
  (e: 'click'): void
  (e: 'feedback'): void
  (e: 'delete'): void
}>()
</script>

<style scoped>
.history-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background-color 0.2s;
}

.history-item:hover {
  background-color: #f5f7fa;
}

.history-item:last-child {
  border-bottom: none;
}

.item-body {
  flex: 1;
  min-width: 0;
}

.item-question {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
}

.q-icon {
  flex-shrink: 0;
  color: #409eff;
  font-size: 18px;
  margin-top: 2px;
}

.q-text {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  line-height: 1.5;
  word-break: break-word;
}

.item-answer {
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
  margin-bottom: 10px;
  padding-left: 26px;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-left: 26px;
  flex-wrap: wrap;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.item-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
  margin-left: 12px;
}
</style>
