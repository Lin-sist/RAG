<template>
  <div class="loading-spinner">
    <el-icon class="is-loading" :size="size" :color="color"><Loading /></el-icon>
    <span v-if="text" class="loading-text">{{ text }}</span>
  </div>
</template>
<script setup lang="ts">
import { Loading } from '@element-plus/icons-vue'
withDefaults(defineProps<{ size?: number; color?: string; text?: string }>(), { size: 24, color: '#409eff', text: '' })
</script>
<style scoped>
.loading-spinner { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.loading-text { font-size: 14px; color: #909399; }
</style>
