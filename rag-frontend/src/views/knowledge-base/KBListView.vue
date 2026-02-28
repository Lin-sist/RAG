<template>
  <div class="kb-list-view">
    <div class="page-header">
      <h2>知识库管理</h2>
      <el-button type="primary" icon="Plus">创建知识库</el-button>
    </div>
    <el-empty description="暂无知识库，请先创建一个">
      <el-button type="primary">创建知识库</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
// 阶段 5 实现
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; color: #303133; }
</style>
