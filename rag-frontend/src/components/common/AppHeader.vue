<template>
  <el-header class="app-header">
    <div class="header-left"><slot name="left" /></div>
    <div class="header-right"><slot name="right" /></div>
  </el-header>
</template>
<script setup lang="ts"></script>
<style scoped>
.app-header { background: #fff; display: flex; align-items: center; justify-content: space-between; box-shadow: 0 1px 4px rgba(0,0,0,0.08); padding: 0 20px; }
.header-left, .header-right { display: flex; align-items: center; gap: 12px; }
</style>
