<template>
  <div class="auth-layout">
    <div class="auth-bg-orb orb-1" />
    <div class="auth-bg-orb orb-2" />
    <div class="auth-bg-orb orb-3" />
    <div class="auth-container">
      <div class="auth-card">
        <div class="auth-header">
          <div class="auth-logo">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2L2 7l10 5 10-5-10-5z"/>
              <path d="M2 17l10 5 10-5"/>
              <path d="M2 12l10 5 10-5"/>
            </svg>
          </div>
          <h1 class="auth-title">RAG 知识库问答系统</h1>
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
  background: #0F172A;
  position: relative;
  overflow: hidden;
}

/* 抽象光斑背景 */
.auth-bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  animation: float 8s ease-in-out infinite;
}

.orb-1 {
  width: 400px;
  height: 400px;
  background: rgba(79, 70, 229, 0.4);
  top: -10%;
  left: 20%;
  animation-delay: 0s;
}

.orb-2 {
  width: 300px;
  height: 300px;
  background: rgba(14, 165, 233, 0.3);
  bottom: -5%;
  right: 15%;
  animation-delay: -3s;
}

.orb-3 {
  width: 200px;
  height: 200px;
  background: rgba(129, 140, 248, 0.3);
  top: 40%;
  right: 35%;
  animation-delay: -5s;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -20px) scale(1.05); }
  66% { transform: translate(-20px, 15px) scale(0.95); }
}

.auth-container {
  width: 100%;
  max-width: 420px;
  padding: var(--rag-space-6);
  position: relative;
  z-index: 1;
}

.auth-card {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: var(--rag-radius-lg);
  padding: var(--rag-space-12) var(--rag-space-8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.3);
}

.auth-header {
  text-align: center;
  margin-bottom: var(--rag-space-8);
}

.auth-logo {
  width: 56px;
  height: 56px;
  margin: 0 auto var(--rag-space-4);
  background: var(--rag-gradient);
  border-radius: var(--rag-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 8px 32px rgba(79, 70, 229, 0.4);
}

.auth-title {
  font-size: 22px;
  font-weight: 700;
  color: #FFFFFF;
  margin: 0 0 var(--rag-space-2);
}

.auth-subtitle {
  font-size: var(--rag-font-body);
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

/* 覆盖 Element Plus 输入框在暗色背景下的样式 */
.auth-card :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  box-shadow: none !important;
}

.auth-card :deep(.el-input__inner) {
  color: #FFFFFF;
}

.auth-card :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.4);
}

.auth-card :deep(.el-input__prefix .el-icon) {
  color: rgba(255, 255, 255, 0.4);
}

.auth-card :deep(.el-button--primary) {
  background: var(--rag-gradient);
  border: none;
  font-weight: 600;
}

.auth-card :deep(.el-button--primary:hover) {
  opacity: 0.9;
}
</style>
