<template>
  <el-card v-loading="loading" class="stats-panel" shadow="never">
    <template #header>
      <div class="panel-header">
        <el-icon><DataAnalysis /></el-icon>
        <span>统计概览</span>
      </div>
    </template>
    <el-row :gutter="20" v-if="stats">
      <el-col :span="8">
        <el-statistic title="文档数量" :value="stats.documentCount">
          <template #prefix><el-icon style="color: #409eff"><Document /></el-icon></template>
        </el-statistic>
      </el-col>
      <el-col :span="8">
        <el-statistic title="向量数量" :value="stats.vectorCount">
          <template #prefix><el-icon style="color: #67c23a"><Cpu /></el-icon></template>
        </el-statistic>
      </el-col>
      <el-col :span="8">
        <el-statistic title="查询次数" :value="stats.queryCount">
          <template #prefix><el-icon style="color: #e6a23c"><Search /></el-icon></template>
        </el-statistic>
      </el-col>
    </el-row>
    <el-empty v-else description="暂无统计数据" :image-size="60" />
  </el-card>
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
  margin-bottom: 20px;
}
.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
}
</style>
