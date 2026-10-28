<template>
  <div class="app-shell">
    <AppSidebar v-if="auth.isLoggedIn" :key="auth.sessionRevision" @open-settings="openSettings" />

    <section class="shell-content">
      <router-view v-if="auth.isLoggedIn" v-slot="{ Component }">
        <transition name="fade-slide" mode="out-in">
          <component :is="Component" :key="auth.sessionRevision" class="route-page" />
        </transition>
      </router-view>
    </section>

    <SettingsModal
      v-model="settingsOpen"
      :initial-tab="settingsInitialTab"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, provide, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useProfileAvatarStore } from '@/stores/profileAvatar'
import { useKnowledgeBaseStore } from '@/stores/knowledgeBase'
import { useChatStore } from '@/stores/chat'
import { getSessionContext } from '@/api/sessionContext'
import { createSessionContextLoader, retrySessionContextKey } from '@/utils/sessionContext'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import SettingsModal from '@/components/settings/SettingsModal.vue'

const settingsOpen = ref(false)
const auth = useAuthStore(), knowledgeBases = useKnowledgeBaseStore(), chat = useChatStore(), router = useRouter()
const profileAvatar = useProfileAvatarStore()
const context = createSessionContextLoader(auth, getSessionContext, () => {
  auth.clearAuth()
  ElMessage.warning('账号归属已变化，请重新登录')
  void router.replace('/login')
})
provide(retrySessionContextKey, context.load)
watch(() => [auth.sessionRevision, auth.tokenRevision, auth.isLoggedIn], () => {
  if (auth.isLoggedIn) void context.load()
}, { immediate: true })
watch(() => auth.sessionRevision, () => {
  settingsOpen.value = false
  knowledgeBases.clearAll()
  chat.clearMessages()
  chat.setCurrentKb(null)
  chat.setStreaming(false)
}, { flush: 'sync', immediate: true })
onUnmounted(context.dispose)
onUnmounted(profileAvatar.clear)
const settingsInitialTab = ref<'general' | 'profile'>('general')

function openSettings(tab: 'general' | 'profile') {
  settingsInitialTab.value = tab
  settingsOpen.value = true
}
</script>

<style scoped>
.app-shell {
  display: flex;
  flex-direction: row;
  height: 100dvh;
  width: 100%;
  background: var(--rag-bg-surface);
  overflow: hidden;
}

.shell-content {
  flex: 1;
  min-width: 0;
  height: 100dvh;
  background: var(--rag-bg-page);
  overflow-y: auto;
  overflow-x: hidden;
}

.route-page {
  width: 100%;
  height: 100%;
}

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
