<template>
  <div class="chat-view">
    <el-empty description="阶段 7 实现：智能问答对话界面">
      <template #image>
        <el-icon :size="64" color="#409eff"><ChatDotRound /></el-icon>
      </template>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { ChatDotRound } from '@element-plus/icons-vue'
</script>

<style scoped>
.chat-view { height: calc(100vh - 140px); display: flex; align-items: center; justify-content: center; }
</style>
