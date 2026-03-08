<template>
  <el-aside :width="collapsed ? '64px' : '220px'" class="app-sidebar">
    <!-- Logo 区域 -->
    <div class="logo-section" @click="$router.push('/')">
      <div class="logo-icon-wrapper">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 2L2 7l10 5 10-5-10-5z"/>
          <path d="M2 17l10 5 10-5"/>
          <path d="M2 12l10 5 10-5"/>
        </svg>
      </div>
      <span v-if="!collapsed" class="logo-text">RAG 系统</span>
    </div>

    <!-- 导航菜单 -->
    <el-menu
      :default-active="activeRoute"
      :collapse="collapsed"
      router
      class="side-menu"
      background-color="#0F172A"
      text-color="#94A3B8"
      active-text-color="#FFFFFF"
    >
      <el-menu-item index="/knowledge-base">
        <el-icon><Collection /></el-icon>
        <template #title>知识库管理</template>
      </el-menu-item>
      <el-menu-item index="/chat">
        <el-icon><ChatDotRound /></el-icon>
        <template #title>智能问答</template>
      </el-menu-item>
      <el-menu-item index="/history">
        <el-icon><Clock /></el-icon>
        <template #title>问答历史</template>
      </el-menu-item>
    </el-menu>

    <!-- 底部版本信息 -->
    <div v-if="!collapsed" class="sidebar-footer">
      <span class="version-text">v1.0.0</span>
    </div>
  </el-aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Collection, ChatDotRound, Clock } from '@element-plus/icons-vue'

defineProps<{
  collapsed: boolean
}>()

const route = useRoute()

const activeRoute = computed(() => {
  const path = route.path
  if (path.startsWith('/knowledge-base')) return '/knowledge-base'
  return path
})
</script>

<style scoped>
.app-sidebar {
  background-color: #0F172A;
  transition: width 0.3s ease;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.logo-section {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--rag-space-3);
  background: rgba(255, 255, 255, 0.03);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  color: #fff;
  cursor: pointer;
  transition: background 0.2s ease;
  flex-shrink: 0;
  padding: 0 var(--rag-space-4);
}

.logo-section:hover {
  background: rgba(255, 255, 255, 0.06);
}

.logo-icon-wrapper {
  width: 32px;
  height: 32px;
  background: var(--rag-gradient);
  border-radius: var(--rag-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
  letter-spacing: 0.5px;
}

.side-menu {
  border-right: none;
  flex: 1;
}

.side-menu:not(.el-menu--collapse) {
  width: 220px;
}

/* 激活菜单项左侧指示条 */
.side-menu :deep(.el-menu-item.is-active) {
  background: rgba(79, 70, 229, 0.15) !important;
  border-left: 3px solid var(--rag-primary);
  font-weight: 500;
}

.side-menu :deep(.el-menu-item) {
  border-left: 3px solid transparent;
  transition: var(--rag-transition);
}

.side-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06) !important;
}

.sidebar-footer {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}

.version-text {
  font-size: var(--rag-font-small);
  color: rgba(148, 163, 184, 0.4);
}
</style>
