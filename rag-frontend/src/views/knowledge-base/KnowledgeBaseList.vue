<template>
  <div class="kb-list-view">
    <header class="page-header">
      <div><p class="eyebrow">知识空间</p><h1>知识库</h1><p class="subtitle">管理文档，让回答有据可查。</p></div>
      <el-button type="primary" @click="openCreate"><Plus :size="16" />创建知识库</el-button>
    </header>
    <div class="list-toolbar">
      <el-input v-model="query" placeholder="筛选已加载的知识库" clearable aria-label="筛选知识库" />
      <span>已加载 {{ kbStore.list.length }} 个 · 当前 {{ filtered.length }} 个</span>
      <div class="view-switch" role="group" aria-label="知识库视图">
        <button :aria-pressed="mode === 'list'" aria-label="列表视图" @click="mode = 'list'"><List :size="17" /></button>
        <button :aria-pressed="mode === 'grid'" aria-label="网格视图" @click="mode = 'grid'"><LayoutGrid :size="17" /></button>
      </div>
      <el-button :loading="kbStore.listLoading" @click="loadList">刷新</el-button>
    </div>
    <el-skeleton v-if="kbStore.listLoading" :rows="5" animated />
    <div v-else-if="kbStore.listError" role="status"><el-alert :title="kbStore.listError" type="error" :closable="false" show-icon /><el-button class="retry" @click="loadList">重试知识库加载</el-button></div>
    <section v-else-if="filtered.length" :class="['kb-table', { grid: mode === 'grid' }]" aria-label="知识库列表">
      <div class="table-heading"><span>名称</span><span>文档</span><span>更新时间</span><span>操作</span></div>
      <article v-for="kb in filtered" :key="kb.id" class="kb-row">
        <button class="kb-name" @click="router.push(`/kb/${kb.id}`)">
          <Database :size="20" /><span><strong :title="kb.name">{{ kb.name }}</strong><small>{{ kb.description || '暂无描述' }}</small></span>
        </button>
        <span class="doc-count">{{ kb.documentCount }} 篇</span>
        <time>{{ formatDate(kb.updatedAt) }}</time>
        <div class="row-actions">
          <el-tag size="small" type="info">{{ kb.isPublic ? '公开' : '私有' }}</el-tag>
          <el-dropdown trigger="click" :disabled="deleting.has(kb.id)" @command="(command: string) => act(command, kb)">
            <button class="more-button" :aria-label="'管理 ' + kb.name">•••</button>
            <template #dropdown><el-dropdown-menu>
              <el-dropdown-item command="edit">编辑</el-dropdown-item>
              <el-dropdown-item command="stats">查看统计</el-dropdown-item>
              <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
            </el-dropdown-menu></template>
          </el-dropdown>
        </div>
      </article>
    </section>
    <el-empty v-else :description="query.trim() ? '没有匹配的知识库' : '还没有知识库'">
      <el-button v-if="query.trim()" @click="query = ''">清除筛选</el-button>
      <el-button v-else type="primary" @click="openCreate">创建知识库</el-button>
    </el-empty>
    <KBCreateDialog v-model:visible="dialogVisible" :edit-data="editTarget" @success="loadList" />
    <el-dialog v-model="statsDialogVisible" :title="'知识库统计 · ' + (statsTarget?.name || '')" width="min(520px, 92vw)" destroy-on-close @closed="kbStore.clearStatistics()">
      <div v-if="kbStore.statisticsKbId === statsTarget?.id && kbStore.statisticsError" role="status"><el-alert :title="kbStore.statisticsError" type="error" :closable="false" /><el-button class="retry" @click="loadStats">重试统计加载</el-button></div>
      <KBStatsPanel v-else :stats="kbStore.statistics?.kbId === statsTarget?.id ? kbStore.statistics : null" :loading="kbStore.statisticsLoading" />
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Plus, Database, List, LayoutGrid } from 'lucide-vue-next'
import { useKnowledgeBaseStore } from '@/stores/knowledgeBase'
import { normalizeError } from '@/api/errors'
import { formatDate } from '@/utils/format'
import type { KnowledgeBaseDTO } from '@/types/knowledgeBase'
import KBCreateDialog from '@/components/knowledge-base/KBCreateDialog.vue'
import KBStatsPanel from '@/components/knowledge-base/KBStatsPanel.vue'
const router = useRouter(), kbStore = useKnowledgeBaseStore()
const query = ref(''), mode = ref('list'), dialogVisible = ref(false), statsDialogVisible = ref(false)
const statsTarget = ref<KnowledgeBaseDTO | null>(null)
const deleting = reactive(new Set<number>())
const editTarget = ref<KnowledgeBaseDTO | null>(null)
const filtered = computed(() => {
  const term = query.value.trim().toLocaleLowerCase()
  return kbStore.list.filter(kb => `${kb.name} ${kb.description || ''}`.toLocaleLowerCase().includes(term))
})
async function loadList() { try { await kbStore.fetchList() } catch { /* store exposes the error state */ } }
onMounted(loadList)
onUnmounted(() => kbStore.clearStatistics())
function openCreate() { editTarget.value = null; dialogVisible.value = true }
async function loadStats() { if (statsTarget.value) await kbStore.fetchStatistics(statsTarget.value.id) }
async function act(command: string, kb: KnowledgeBaseDTO) {
  if (command === 'edit') { editTarget.value = kb; dialogVisible.value = true }
  if (command === 'stats') {
    statsTarget.value = kb; statsDialogVisible.value = true
    await loadStats()
  }
  if (command === 'delete') {
    if (deleting.has(kb.id)) return
    deleting.add(kb.id)
    try { await ElMessageBox.confirm(`确定删除「${kb.name}」及其文档和向量数据吗？`, '删除确认', { type: 'warning' }) }
    catch { deleting.delete(kb.id); return }
    try { await kbStore.remove(kb.id) } catch (error) { ElMessage.error(normalizeError(error).message) }
    finally { deleting.delete(kb.id) }
  }
}
</script>
<style scoped>
.kb-list-view { max-width: 1120px; margin-inline: auto; padding: 36px 32px; width: 100%; box-sizing: border-box; overflow-y: auto; }
.page-header, .list-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.page-header { margin-bottom: 32px; }
.eyebrow { color: var(--rag-text-secondary); font-size: 12px; margin: 0 0 8px; }
h1 { font-size: 28px; margin: 0; color: var(--rag-text-primary); }
.subtitle { color: var(--rag-text-secondary); font-size: 14px; margin-bottom: 0; }
.list-toolbar { margin-bottom: 24px; color: var(--rag-text-secondary); font-size: 13px; }
.list-toolbar .el-input { max-width: 360px; }
.list-toolbar > span { margin-inline-start: auto; white-space: nowrap; }
.kb-table { border: 1px solid var(--rag-border); border-radius: 14px; overflow: hidden; }
.kb-row, .table-heading { display: grid; grid-template-columns: minmax(0, 1fr) 70px 160px 90px; gap: 16px; align-items: center; padding: 18px 20px; }
.table-heading { background: var(--rag-bg-hover); color: var(--rag-text-secondary); font-size: 12px; }
.kb-row + .kb-row { border-top: 1px solid var(--rag-border); }
.kb-row { background: var(--rag-bg-card); font-size: 13px; }
.kb-name { display: flex; gap: 14px; align-items: center; min-width: 0; border: 0; background: transparent; color: var(--rag-text-primary); text-align: start; cursor: pointer; padding: 4px 0; }
.kb-name svg { flex-shrink: 0; color: var(--rag-text-secondary); }
.kb-name > span { min-width: 0; }
.kb-name strong, .kb-name small { display: block; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.kb-name strong { font-size: 14px; font-weight: 550; }
.kb-name small { margin-top: 6px; color: var(--rag-text-secondary); }
.row-actions { display: flex; align-items: center; justify-content: flex-end; gap: 8px; }
.more-button { cursor: pointer; border: 1px solid var(--rag-border); border-radius: 6px; background: var(--rag-bg-card); color: var(--rag-text-primary); padding: 6px; }
.retry { margin-top: 12px; }
.view-switch { display: flex; border: 1px solid var(--rag-border); border-radius: 8px; padding: 3px; }
.view-switch button { display: flex; align-items: center; padding: 7px; border: 0; border-radius: 5px; color: var(--rag-text-secondary); background: transparent; cursor: pointer; }
.view-switch button[aria-pressed="true"] { background: var(--rag-bg-hover); color: var(--rag-text-primary); }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px; border: 0; overflow: visible; }
.grid .table-heading { display: none; }
.grid .kb-row { border: 1px solid var(--rag-border); border-radius: 14px; grid-template-columns: 1fr auto; }
.grid .kb-name { grid-column: 1 / -1; }
.grid time { grid-column: 1 / -1; }
.grid .row-actions { grid-column: 2; grid-row: 2; }
button:focus-visible { outline: 2px solid var(--rag-primary); outline-offset: 3px; }
time, .doc-count { color: var(--rag-text-secondary); }
@media (max-width: 720px) {
  .kb-list-view { padding: 24px 16px; }
  .page-header, .list-toolbar { flex-wrap: wrap; }
  .list-toolbar .el-input { max-width: none; width: 100%; }
  .table-heading { display: none; }
  .kb-row { grid-template-columns: minmax(0, 1fr) auto; gap: 12px; padding: 16px; }
  .kb-name { grid-column: 1; } .row-actions { grid-column: 2; grid-row: 1; }
  time { text-align: end; } .doc-count { grid-column: 1; grid-row: 2; }
}
@media (max-width: 768px) { .kb-list-view { padding-top: 60px; } }
</style>
