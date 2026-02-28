<template>
  <div class="auth-layout">
    <div class="auth-container">
      <div class="auth-card">
        <div class="auth-header">
          <h1 class="auth-title">🤖 RAG 知识库问答系统</h1>
          <p class="auth-subtitle">智能文档检索与问答平台</p>
        </div>
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.auth-layout {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.auth-container { width: 100%; max-width: 420px; padding: 20px; }
.auth-card {
  background: #fff;
  border-radius: 12px;
  padding: 40px 32px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}
.auth-header { text-align: center; margin-bottom: 32px; }
.auth-title { font-size: 24px; font-weight: 700; color: #303133; margin: 0 0 8px 0; }
.auth-subtitle { font-size: 14px; color: #909399; margin: 0; }
</style>
