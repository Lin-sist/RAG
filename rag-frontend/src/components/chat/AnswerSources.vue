<template>
  <section class="answer-sources" aria-label="引用来源">
    <h3><FileText :size="14" aria-hidden="true" />引用来源</h3>
    <div v-if="citations?.length" class="source-list">
      <details v-for="(citation, index) in citations" :key="`${resetKey}:${index}`" class="source-card" :open="initiallyExpanded">
        <summary :aria-label="`来源片段：${citationLabel(citation)}`">
          <FileText :size="17" class="source-icon" aria-hidden="true" />
          <span class="source-heading">
            <span class="source-title">{{ citationLabel(citation) }}</span>
            <span class="source-preview">{{ citation.snippet || '此来源未返回片段' }}</span>
            <span v-if="formatRelevanceScore(citation.score)" class="source-score">检索相关度 {{ formatRelevanceScore(citation.score) }}</span>
          </span>
          <ChevronDown :size="15" class="source-chevron" aria-hidden="true" />
          <span class="source-toggle"><span class="when-closed">展开片段</span><span class="when-open">收起片段</span></span>
        </summary>
        <p class="source-snippet">{{ citation.snippet || '此来源未返回片段' }}</p>
      </details>
    </div>
    <p v-else class="source-empty">{{ hint || '本回答未返回可展示来源' }}</p>
  </section>
</template>

<script setup lang="ts">
import { FileText, ChevronDown } from 'lucide-vue-next'
import type { Citation } from '@/types/qa'
import { citationLabel, formatRelevanceScore } from '@/utils/qaPresentation'
import { usePreferencesStore } from '@/stores/preferences'
defineProps<{ citations?: Citation[]; hint?: string; resetKey: string | number }>()
// Snapshot once; a settings change must not override a reader's toggle.
const initiallyExpanded = usePreferencesStore().values.sources === 'expanded'
</script>

<style scoped>
.answer-sources { margin-top: 22px; }
h3 { margin: 0 0 10px; display: flex; align-items: center; gap: 6px; font-size: 12px; font-weight: 500; color: var(--rag-text-secondary); }
.source-list { display: grid; gap: 8px; }
.source-card { border: 1px solid var(--rag-border); border-radius: 12px; background: var(--rag-bg-surface); min-width: 0; padding: 0 14px 12px; }
summary { display: flex; align-items: center; gap: 9px; padding: 12px 0 8px; list-style: none; cursor: pointer; color: var(--rag-text-primary); }
summary::-webkit-details-marker { display: none; }
summary:focus-visible { outline: 2px solid var(--rag-primary); outline-offset: 3px; border-radius: 4px; }
.source-icon, .source-chevron { flex: none; color: var(--rag-text-secondary); }
.source-heading { flex: 1; min-width: 0; }
.source-title { display: block; font-size: 13px; font-weight: 500; overflow-wrap: anywhere; }
.source-toggle { font-size: 11px; flex: none; color: var(--rag-text-secondary); }
.when-open { display: none; }
details[open] .when-open { display: inline; }
details[open] .when-closed { display: none; }
details[open] .source-chevron { transform: rotate(180deg); }
.source-snippet { margin: 0; color: var(--rag-text-secondary); font-size: 12px; line-height: 1.75; white-space: pre-wrap; overflow-wrap: anywhere; }
.source-preview { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; margin-top: 5px; color: var(--rag-text-secondary); font-size: 12px; line-height: 1.75; overflow-wrap: anywhere; }
details[open] .source-preview { display: none; }
.source-score { display: block; margin: 6px 0 0; font-size: 11px; color: var(--rag-text-secondary); }
.source-empty { margin: 0; padding: 12px 14px; border: 1px dashed var(--rag-border); border-radius: 10px; color: var(--rag-text-secondary); font-size: 12px; line-height: 1.6; }
@media (max-width: 480px) { .source-toggle { display: none; } }
</style>
