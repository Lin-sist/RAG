<template>
  <section :class="['workspace-context', { compact: collapsed, profile: mode === 'profile' }]" aria-label="工作空间归属">
    <button v-if="collapsed" class="compact-entry" :aria-label="detailsLabel" :title="detailsLabel" @click="emit('open-profile')"><Building2 :size="18" /></button>
    <template v-else>
      <p v-if="mode === 'sidebar'" class="workspace-label">当前工作空间</p>
      <div v-if="auth.contextStatus === 'ready' && auth.workspace" class="workspace-ready">
        <div class="workspace-heading"><Building2 v-if="mode === 'sidebar'" :size="18" aria-hidden="true" /><p class="workspace-name">{{ auth.workspace.displayName }}</p></div>
        <button v-if="mode === 'sidebar'" class="workspace-link" :aria-label="detailsLabel" @click="emit('open-profile')">查看归属</button>
      </div>
      <div v-else-if="auth.contextStatus === 'loading' || auth.contextStatus === 'idle'" class="workspace-loading" role="status" aria-busy="true">
        <span class="skeleton" aria-hidden="true"></span><span class="loading-label">正在读取工作空间</span>
      </div>
      <div v-else class="workspace-error" role="status"><p>工作空间资料暂不可用</p><button class="workspace-link" @click="retry">重试工作空间资料</button></div>
    </template>
  </section>
</template>
<script setup lang="ts">
import { computed, inject } from 'vue'
import { Building2 } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { retrySessionContextKey } from '@/utils/sessionContext'
withDefaults(defineProps<{ mode?: 'sidebar' | 'profile'; collapsed?: boolean }>(), { mode: 'sidebar', collapsed: false })
const emit = defineEmits<{ (event: 'open-profile'): void }>()
const auth = useAuthStore()
const retry = inject(retrySessionContextKey, async () => {})
const detailsLabel = computed(() => auth.workspace ? `查看工作空间归属：${auth.workspace.displayName}` : '查看工作空间归属')
</script>
<style scoped>
.workspace-context { margin: 0 0 16px; padding: 12px; border: 1px solid var(--rag-border); border-radius: 12px; min-width: 0; }
.workspace-context:not(.compact):not(.profile) { min-height: 120px; }
p { margin: 0; }
.workspace-label { font-size: 12px; line-height: 16px; color: var(--rag-text-secondary); margin-bottom: 8px; }
.workspace-heading { display: flex; gap: 8px; align-items: flex-start; }
.workspace-heading svg { flex: none; margin-top: 1px; }
.workspace-name { font-size: 14px; line-height: 20px; font-weight: 500; overflow-wrap: anywhere; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.workspace-link { display: block; margin-top: 8px; padding: 4px 0; font: inherit; font-size: 12px; line-height: 16px; text-align: left; color: var(--rag-text-secondary); border: 0; background: transparent; text-decoration: underline; text-underline-offset: 3px; cursor: pointer; }
.workspace-link:hover { color: var(--rag-text-primary); }
button:active { transform: translateY(1px); }
button:focus-visible { outline: 2px solid var(--rag-text-secondary); outline-offset: 3px; }
.workspace-error { font-size: 14px; line-height: 20px; color: var(--rag-text-secondary); }
.skeleton { display: block; height: 20px; width: 80%; border-radius: 4px; background: var(--rag-border); }
.loading-label { display: block; margin-top: 8px; font-size: 12px; line-height: 16px; color: var(--rag-text-secondary); }
.compact { padding: 0; border: 0; display: grid; place-items: center; }
.compact-entry { width: 40px; height: 40px; display: grid; place-items: center; border: 0; border-radius: 8px; color: var(--rag-text-secondary); background: transparent; cursor: pointer; }
.compact-entry:hover { background: var(--rag-bg-hover); }
.profile { padding: 0; margin: 0; border: 0; }
.profile .workspace-name { display: block; overflow: visible; font-weight: 400; }
@media (prefers-reduced-motion: reduce) { button:active { transform: none; } }
</style>
