<template><span class="account-avatar" aria-hidden="true"><img v-if="image" :src="image" alt="" @error="failed = true" /><span v-else>{{ initial }}</span></span></template>
<script setup lang="ts">
import { computed, ref, watch, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useProfileAvatarStore } from '@/stores/profileAvatar'
const props = defineProps<{ preview?: string; removed?: boolean; name?: string }>()
const auth = useAuthStore(), avatar = useProfileAvatarStore(), failed = ref(false)
const source = computed(() => props.removed ? '' : props.preview ?? avatar.url)
const image = computed(() => failed.value ? '' : source.value)
const initial = computed(() => Array.from(props.name || auth.userInfo?.displayName || auth.userInfo?.username || 'A')[0]?.toUpperCase())
watch(source, () => { failed.value = false })
onMounted(() => { void avatar.load() })
</script>
<style scoped>
.account-avatar { display: inline-flex; flex: 0 0 auto; width: 40px; height: 40px; align-items: center; justify-content: center; border-radius: 50%; background: var(--rag-bg-hover); color: var(--rag-text-primary); overflow: hidden; outline: 1px solid var(--rag-border); }
img { width: 100%; height: 100%; object-fit: cover; }
</style>
