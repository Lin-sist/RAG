<template>
  <el-aside :width="collapsed ? '64px' : '220px'" class="app-sidebar">
    <!-- Logo 区域 -->
    <div class="logo-section" @click="$router.push('/')">
      <span v-if="!collapsed" class="logo-text">🤖 RAG 系统</span>
      <span v-else class="logo-icon">🤖</span>
    </div>

    <!-- 导航菜单 -->
    <el-menu
      :default-active="activeRoute"
      :collapse="collapsed"
      router
      class="side-menu"
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409eff"
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

    <!-- 底部版本信息（展开时显示） -->
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

/**
 * 计算当前激活的菜单项
 * 如果在 /knowledge-base/:id 详情页，也要高亮"知识库管理"
 */
const activeRoute = computed(() => {
  const path = route.path
  if (path.startsWith('/knowledge-base')) return '/knowledge-base'
  return path
})
</script>

<style scoped>
.app-sidebar {
  background-color: #304156;
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
  background-color: #263445;
  color: #fff;
  cursor: pointer;
  transition: background-color 0.2s;
  flex-shrink: 0;
}
.logo-section:hover {
  background-color: #1f2d3d;
}
.logo-text {
  font-size: 18px;
  font-weight: 700;
  white-space: nowrap;
}
.logo-icon {
  font-size: 24px;
}
.side-menu {
  border-right: none;
  flex: 1;
}
/* 去掉折叠菜单的右边框 */
.side-menu:not(.el-menu--collapse) {
  width: 220px;
}
.sidebar-footer {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}
.version-text {
  font-size: 12px;
  color: rgba(191, 203, 217, 0.5);
}
</style>
