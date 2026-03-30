<template>
  <el-container class="default-layout">
    <!-- 左侧导航栏 -->
    <AppSidebar :collapsed="isCollapse" />

    <!-- 右侧内容区 -->
    <el-container>
      <!-- 顶部栏 -->
      <AppHeader
        :collapsed="isCollapse"
        :title="currentTitle"
        @toggle-collapse="isCollapse = !isCollapse"
      />

      <!-- 主内容区 -->
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/common/AppHeader.vue'
import AppSidebar from '@/components/common/AppSidebar.vue'

const route = useRoute()
const isCollapse = ref(false)
const currentTitle = computed(() => (route.meta.title as string) || 'RAG 系统')
</script>

<style scoped>
.default-layout {
  height: 100vh;
}
.layout-main {
  background-color: var(--rag-bg-page);
  padding: var(--rag-space-6);
  overflow-y: auto;
}

/* 路由切换过渡动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.2s ease;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
