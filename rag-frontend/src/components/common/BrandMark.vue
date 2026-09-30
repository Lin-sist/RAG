<template>
  <span class="brand-mark">
    <svg aria-hidden="true" viewBox="0 0 512 512">
      <g transform="translate(-116.7 -59.5) scale(1.23)">
        <path d="M141 133 C252 170 372 222 465 268 C372 236 250 184 141 133 Z" fill="currentColor" />
        <path d="M226 380 C310 372 396 326 465 268 C398 340 302 388 226 380 Z" fill="currentColor" />
        <path d="M412 238 Q447 252 465 268 Q447 284 412 298 Q427 283 427 268 Q427 253 412 238 Z" fill="#C9A15A" />
      </g>
    </svg>
    <span v-if="!compact" class="brand-name">RAG <span>智能问答</span></span>
  </span>
</template>
<script setup lang="ts">
defineProps<{ compact?: boolean }>()
</script>
<style scoped>
.brand-mark { display: inline-flex; align-items: center; gap: 10px; color: var(--rag-text-primary); min-width: 0; }
svg { width: 32px; height: 32px; flex: none; }
.brand-name { font-size: 16.5px; font-weight: 600; white-space: nowrap; letter-spacing: .2px; }
.brand-name span { font: inherit; }
</style>
