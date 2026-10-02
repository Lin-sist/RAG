<template>
  <div class="doc-list">
    <div class="doc-toolbar"><el-input v-model="query" placeholder="筛选已加载的文档" clearable aria-label="筛选已加载的文档" /><el-button :loading="loading" aria-label="刷新文档列表" @click="emit('refresh')"><RefreshCw :size="15" /></el-button></div>
    <p class="doc-count">已加载 {{ documents.length }} 篇 · 当前 {{ filtered.length }} 篇</p>
    <el-skeleton v-if="loading" :rows="4" animated />
    <div v-else-if="filtered.length" class="table-scroll" tabindex="0" role="region" aria-label="文档表格，可横向滚动">
      <table><thead><tr><th>文档</th><th>状态</th><th>分块</th><th>上传时间</th><th>操作</th></tr></thead><tbody>
        <tr v-for="doc in filtered" :key="doc.id"><td class="doc-title"><strong :title="doc.title">{{ doc.title }}</strong><small>{{ doc.fileType }}</small></td><td><span :class="['doc-state', doc.status.toLowerCase()]">{{ statusLabel(doc.status) }}</span></td><td>{{ doc.status === 'COMPLETED' ? doc.chunkCount : '—' }}</td><td class="doc-date">{{ formatDate(doc.createdAt) }}</td><td><el-button type="danger" link :loading="deleting.has(doc.id)" :aria-label="'删除文档 ' + doc.title" @click="handleDelete(doc)"><Trash2 :size="15" /></el-button></td></tr>
      </tbody></table>
    </div>
    <div v-else class="empty-state"><FileText :size="30" /><p>{{ query.trim() ? '没有匹配的已加载文档' : '暂无文档，点击上传开始使用' }}</p><el-button v-if="query.trim()" @click="query = ''">清除文档筛选</el-button></div>
    <el-alert v-if="deleteError" class="delete-error" :title="deleteError" type="error" :closable="false" show-icon />
  </div>
</template>
<script setup lang="ts">
import { computed, reactive, ref, watch, onUnmounted } from 'vue'
import { RefreshCw, Trash2, FileText } from 'lucide-vue-next'
import { ElMessageBox, ElMessage } from 'element-plus'
import { formatDate } from '@/utils/format'
import { normalizeError } from '@/api/errors'
import { deleteDocument } from '@/api/knowledgeBase'
import type { DocumentInfo } from '@/types/document'
const props = defineProps<{ kbId: number; documents: DocumentInfo[]; loading: boolean }>()
const emit = defineEmits<{ refresh: []; deleted: [] }>()
const query = ref(''), deleteError = ref(''), deleting = reactive(new Set<number>())
let disposed = false
onUnmounted(() => { disposed = true })
watch(() => props.kbId, () => { query.value = ''; deleteError.value = '' })
const filtered = computed(() => {
  const term = query.value.trim().toLocaleLowerCase()
  return props.documents.filter(doc => `${doc.title} ${doc.fileType}`.toLocaleLowerCase().includes(term))
})
function statusLabel(status: string) {
  return ({ PENDING: '等待处理', PROCESSING: '处理中', COMPLETED: '已完成', FAILED: '处理失败', RECONCILIATION_REQUIRED: '需要核对' } as Record<string, string>)[status] || '未识别状态'
}
async function handleDelete(doc: DocumentInfo) {
  if (deleting.has(doc.id)) return
  const kbId = props.kbId
  deleting.add(doc.id); deleteError.value = ''
  try {
    try { await ElMessageBox.confirm(`确定删除文档「${doc.title}」及其向量数据吗？`, '删除文档确认', { type: 'warning' }) }
    catch { return }
    if (disposed || props.kbId !== kbId) return
    try {
      await deleteDocument(kbId, doc.id)
      if (disposed || props.kbId !== kbId) return
      ElMessage.success('文档已删除'); emit('deleted')
    } catch (error) { if (!disposed && props.kbId === kbId) deleteError.value = normalizeError(error).message }
  } finally { deleting.delete(doc.id) }
}
</script>
<style scoped>
.doc-toolbar { display: flex; gap: 10px; }
.doc-count { color: var(--rag-text-secondary); font-size: 12px; margin-block: 14px; }
.table-scroll { overflow-x: auto; border-top: 1px solid var(--rag-border); }
table { width: 100%; border-collapse: collapse; text-align: left; font-size: 12px; }
th { color: var(--rag-text-secondary); font-weight: 500; background: var(--rag-bg-hover); padding: 12px 8px; white-space: nowrap; }
td { padding: 16px 8px; border-bottom: 1px solid var(--rag-border); color: var(--rag-text-primary); }
.doc-title { min-width: 140px; max-width: 240px; overflow-wrap: anywhere; }
.doc-title strong { font-weight: 500; line-height: 1.6; font-size: 13px; }
.doc-title small { display: block; color: var(--rag-text-secondary); margin-top: 5px; }
.doc-date { white-space: nowrap; color: var(--rag-text-secondary); }
.doc-state { display: inline-block; padding: 3px 7px; border-radius: 5px; background: var(--rag-bg-hover); white-space: nowrap; color: var(--rag-text-secondary); }
.completed { color: var(--rag-success); }.failed { color: var(--rag-danger); }.pending, .reconciliation_required { color: var(--rag-warning); }
.empty-state { text-align: center; color: var(--rag-text-secondary); padding-block: 40px; font-size: 13px; }
.delete-error { margin-top: 14px; }.table-scroll:focus-visible { outline: 2px solid var(--rag-primary); outline-offset: 3px; }
</style>
