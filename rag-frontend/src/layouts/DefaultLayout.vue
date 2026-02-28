<template>
  <el-container class="default-layout">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="logo-section">
        <span v-if="!isCollapse" class="logo-text">🤖 RAG 系统</span>
        <span v-else class="logo-icon">🤖</span>
      </div>
      <el-menu
        :default-active="currentRoute"
        :collapse="isCollapse"
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
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <span class="username">{{ authStore.userInfo?.username || '用户' }}</span>
          <el-button type="danger" text @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            登出
          </el-button>
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Collection, ChatDotRound, Clock, Fold, Expand, SwitchButton } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useAuth } from '@/composables/useAuth'

const route = useRoute()
const authStore = useAuthStore()
const { logout } = useAuth()
const isCollapse = ref(false)
const currentRoute = computed(() => route.path)
const currentTitle = computed(() => (route.meta.title as string) || 'RAG 系统')

async function handleLogout() { await logout() }
</script>

<style scoped>
.default-layout { height: 100vh; }
.layout-aside { background-color: #304156; transition: width 0.3s; overflow: hidden; }
.logo-section { height: 60px; display: flex; align-items: center; justify-content: center; background-color: #263445; color: #fff; }
.logo-text { font-size: 18px; font-weight: 700; white-space: nowrap; }
.logo-icon { font-size: 24px; }
.side-menu { border-right: none; }
.layout-header {
  background: #fff; display: flex; align-items: center; justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08); padding: 0 20px; height: 60px;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.collapse-btn { font-size: 20px; cursor: pointer; color: #606266; }
.collapse-btn:hover { color: #409eff; }
.header-right { display: flex; align-items: center; gap: 12px; }
.username { color: #606266; font-size: 14px; }
.layout-main { background-color: #f0f2f5; padding: 20px; }
</style>
