<template>
  <div class="app-shell">
    <AppSidebar @open-settings="openSettings" />

    <section class="shell-content">
      <router-view v-slot="{ Component }">
        <transition name="fade-slide" mode="out-in">
          <component :is="Component" class="route-page" />
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
import { ref } from 'vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import SettingsModal from '@/components/settings/SettingsModal.vue'

const settingsOpen = ref(false)
const settingsInitialTab = ref<'profile' | 'security' | 'apiKey'>('profile')

function openSettings(tab: 'profile' | 'security' | 'apiKey') {
  settingsInitialTab.value = tab
  settingsOpen.value = true
}
</script>

<style scoped>
.app-shell {
  display: flex;
  height: 100vh;
  background: var(--rag-bg-surface);
}

.shell-content {
  flex: 1;
  min-width: 0;
  background: var(--rag-bg-page);
  overflow: hidden;
}

.route-page {
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
