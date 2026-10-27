<template>
  <section ref="root" class="trace-shell" aria-label="本次问答轨迹" @keydown="handleKey">
    <header class="trace-heading">
      <div><p class="trace-eyebrow">本次问答</p><h2>{{ question || '问答执行过程' }}</h2><p class="trace-summary"><span :class="{ 'trace-danger': businessState === 'ERROR' }">{{ outcome }}</span><span v-if="trace">{{ operations.length }} 项操作</span><span v-if="fallbacks" class="trace-warning">{{ fallbacks }} 项降级</span></p></div>
      <div v-if="trace" class="trace-elapsed"><span>服务端执行</span><strong>{{ formatTraceTime(trace.elapsedMs) }}</strong></div>
    </header>
    <div v-if="!trace" class="trace-empty" role="status">{{ pending ? '等待本次执行记录…' : released ? '本页记录已释放' : historical ? '该历史记录未保存轨迹' : invalid ? '本次轨迹不可用，回答仍按原结果展示' : '本次未提供轨迹' }}</div>
    <template v-else>
      <p v-if="invalid || trace.captureStatus === 'PARTIAL'" class="trace-capture" role="status">轨迹不完整；已收到的阶段仍可查看。</p>
      <p v-if="transport === 'CLIENT_ABORTED' || transport === 'INCOMPLETE'" class="trace-capture">{{ transport === 'CLIENT_ABORTED' ? '已停止接收' : '连接未交付有效终态' }}，服务端最终状态未确认。</p>
      <section class="trace-overview" aria-label="可拖选的轨迹总览">
        <div class="trace-overview-head"><strong>执行分布</strong><button v-if="range" type="button" @click="clearRange">全部时间<span v-if="newActivity"> · 有新操作</span></button><button v-else type="button" @click="createRange">选择时间范围</button></div>
        <div class="trace-plot-grid"><div class="trace-lane-labels"><span v-for="kind in laneTypes" :key="kind" :class="`trace-type-${kind}`">{{ typeNames[kind] }}</span></div>
          <div ref="plot" class="trace-timeline" :style="{ height: `${laneTypes.length * 28 + 28}px` }" tabindex="0" aria-label="时间总览，可横向拖动选择范围" @pointerdown="pointerDown" @pointermove="pointerMove" @pointerup="pointerUp" @pointercancel="cancelDrag">
            <div v-for="fraction in [0, .25, .5, .75, 1]" :key="fraction" class="trace-tick" :style="{ left: `${fraction * 100}%` }"><span>{{ fraction ? formatTraceTime(domain * fraction) : '0' }}</span></div>
            <button v-for="op in operations.filter(o => o.startOffsetMs <= domain)" :key="op.stageId" type="button" :class="['trace-segment', `trace-type-${op.type}`, { 'trace-dimmed': !matches(op), 'trace-failed': op.status === 'FAILED' }]" :style="segmentStyle(op)" :aria-label="`${typeNames[op.type]}：${op.title}，${duration(op)}`" :aria-pressed="selectedId === op.stageId" :data-trace-segment="op.stageId" @click="segmentClick($event, op.stageId)"></button>
            <div v-if="range" class="trace-selection" :style="{ left: `${range[0] / domain * 100}%`, width: `${(range[1] - range[0]) / domain * 100}%` }"></div>
            <button v-for="(value, index) in range ?? []" :key="index" type="button" role="slider" class="trace-handle" :data-trace-handle="index" :aria-label="index === 0 ? '选区起点' : '选区终点'" :aria-valuemin="0" :aria-valuemax="domain" :aria-valuenow="Math.round(value)" :aria-valuetext="formatTraceTime(value)" :style="{ left: `${value / domain * 100}%` }" @keydown="rangeKey($event, index)"></button>
          </div>
        </div><div class="trace-range-caption"><span>{{ rangeLabel }}</span><span class="trace-brush-hint">横向拖选范围 · 点选操作查看详情</span></div>
      </section>
      <div class="trace-body-grid" :class="{ 'trace-has-detail': selected }">
        <section class="trace-operations" aria-label="操作列表">
          <div class="trace-list-toolbar"><div role="group" aria-label="按操作类型筛选" class="trace-filters"><button v-for="kind in ['all', ...laneTypes]" :key="kind" type="button" :aria-pressed="typeFilter === kind" :class="kind === 'all' ? '' : `trace-type-${kind}`" @click="typeFilter = kind">{{ kind === 'all' ? '全部' : typeNames[kind as TraceType] }}</button></div><select v-model="phaseFilter" aria-label="按阶段筛选"><option value="all">全部阶段</option><option value="prepare">准备</option><option value="retrieve">检索</option><option value="generate">生成</option><option value="validate">校验</option><option v-if="laneTypes.includes('other')" value="other">其他</option></select></div>
          <button v-for="op in filtered" :key="op.stageId" type="button" :class="['trace-operation', `trace-type-${op.type}`]" :data-trace-id="op.stageId" :aria-label="`查看${op.title}详情`" :aria-pressed="selectedId === op.stageId" @click="selectOperation(op.stageId)"><span class="trace-type-tag">{{ typeNames[op.type] }}</span><span class="trace-operation-body"><strong>{{ op.title }}</strong><span>{{ op.description }}</span></span><span class="trace-operation-time">{{ duration(op) }}<small :class="{ 'trace-danger': op.status === 'FAILED' }">{{ statusNames[op.status] }}</small></span></button>
          <p v-if="!filtered.length" class="trace-empty">{{ operations.length ? '这个范围内没有匹配的操作。' : pending ? '等待实际阶段记录…' : '本次没有可展示的阶段记录' }}</p>
        </section>
        <Teleport to="body" :disabled="!mobile"><div v-if="selected && mobile && active" class="trace-sheet-backdrop" aria-hidden="true" @click="closeDetail()"></div>
        <aside v-if="selected && active" ref="inspector" :class="['trace-inspector', `trace-type-${selected.type}`, { 'trace-mobile-sheet': mobile }]" :role="mobile ? 'dialog' : 'complementary'" :aria-modal="mobile ? true : undefined" aria-label="操作详情" @keydown="mobile && handleKey($event)">
          <div class="trace-detail-head"><span class="trace-type-tag">{{ typeNames[selected.type] }}</span><span>操作 {{ operations.findIndex(o => o.stageId === selectedId) + 1 }} / {{ operations.length }}</span><button type="button" aria-label="关闭详情" @click="closeDetail()">×</button></div>
          <h3 ref="detailTitle" tabindex="-1">{{ selected.title }}</h3><p class="trace-detail-description">{{ selected.description }}</p>
          <div class="trace-detail-tabs" role="tablist" aria-label="操作详情页签"><button v-for="tab in detailTabs" :key="tab" type="button" role="tab" :aria-selected="detailTab === tab" @click="detailTab = tab" @keydown="detailTabKey($event, tab)">{{ tab }}</button></div>
          <section role="tabpanel" :aria-label="detailTab">
            <template v-if="detailTab === '回答'"><p v-if="!answerAvailable" class="trace-note">{{ pending ? '仅展示本次已收到文本，尚未确认完整回答。' : '本次没有已确认的完整回答。' }}</p><AnswerContent v-if="answer" :content="answer" /><p v-else class="trace-note">本次未提供回答文本。</p></template>
            <template v-else-if="detailTab === '来源'"><AnswerSources v-if="answerAvailable" :citations="citations" hint="本次未返回可展示来源" :reset-key="trace.runId" /><p v-else class="trace-note">来源将在本次有效完整回答后提供。</p></template>
            <dl v-else class="trace-facts"><template v-for="fact in detailFacts" :key="fact[0]"><dt>{{ fact[0] }}</dt><dd>{{ fact[1] }}</dd></template></dl>
          </section>
          <details class="trace-technical"><summary>技术信息</summary><p>执行标识：{{ trace.runId }}</p><p>操作标识：{{ selected.stageId }}</p><p v-if="selected.type !== 'other'">阶段：{{ selected.stage }}</p><p v-if="trace.traceId">Trace ID：{{ trace.traceId }}</p></details>
        </aside></Teleport>
      </div>
    </template><span class="trace-sr-only" role="status" aria-live="polite">{{ announcement }}</span>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import AnswerContent from './AnswerContent.vue'
import AnswerSources from './AnswerSources.vue'
import type { Citation } from '@/types/qa'
import { clampTraceRange, formatTraceTime, intersectTrace, traceOperations, type ExecutionTrace, type TraceOperation, type TraceType } from '@/utils/executionTrace'

const props = withDefaults(defineProps<{ trace?: ExecutionTrace; question?: string; answer?: string; citations?: Citation[]; pending?: boolean; historical?: boolean; invalid?: boolean; released?: boolean; businessState?: string; transport?: string; answerAvailable?: boolean; active?: boolean }>(), { question: '', answer: '', citations: () => [], active: true })
const root = ref<HTMLElement>(), plot = ref<HTMLElement>(), inspector = ref<HTMLElement>(), detailTitle = ref<HTMLElement>()
const selectedId = ref(''), detailTab = ref('概览'), typeFilter = ref('all'), phaseFilter = ref('all'), announcement = ref('')
const range = ref<[number, number] | null>(null), frozenDomain = ref<number | null>(null), width = ref(1000)
const mobile = computed(() => width.value < 720)
const operations = computed(() => props.trace ? traceOperations(props.trace) : [])
const laneTypes = computed<TraceType[]>(() => operations.value.some(o => o.type === 'other') ? ['assistant', 'tool', 'context', 'other'] : ['assistant', 'tool', 'context'])
const typeNames: Record<TraceType, string> = { tool: '工具', assistant: '助手', context: '上下文', other: '其他' }
const statusNames = { RUNNING: '执行中', SUCCEEDED: '✓ 完成', FAILED: '! 失败', CANCELLED: '已取消', SKIPPED: '未执行' }
const domain = computed(() => Math.max(1, frozenDomain.value ?? props.trace?.elapsedMs ?? 1))
const newActivity = computed(() => !!range.value && (props.trace?.elapsedMs ?? 0) > domain.value)
const matches = (op: TraceOperation) => intersectTrace(op, range.value, props.trace?.elapsedMs ?? domain.value)
  && (typeFilter.value === 'all' || op.type === typeFilter.value) && (phaseFilter.value === 'all' || op.phase === phaseFilter.value)
const filtered = computed(() => operations.value.filter(matches))
const selected = computed(() => operations.value.find(o => o.stageId === selectedId.value))
const duration = (o: TraceOperation) => o.endOffsetMs === null ? '执行中' : formatTraceTime(o.endOffsetMs - o.startOffsetMs)
const fallbacks = computed(() => operations.value.reduce((n, o) => n + Number(o.facts.fallbackCount ?? 0), 0))
const stateLabels: Record<string, string> = { ANSWER: '已完成', NO_ANSWER: '未获得可用答案', UNSUPPORTED: '当前问题不受支持', INVALID: '输入无效', ERROR: '本次问答失败', CANCELLED: '服务端已取消' }
const outcome = computed(() => props.pending ? '执行中' : props.transport === 'CLIENT_ABORTED' || props.transport === 'INCOMPLETE' ? '结果未确认' : stateLabels[props.businessState ?? ''] ?? '本次执行已结束')
const rangeLabel = computed(() => (range.value ? `${formatTraceTime(range.value[0])} — ${formatTraceTime(range.value[1])}（跨度 ${formatTraceTime(range.value[1] - range.value[0])}）` : '全部时间') + ` · ${filtered.value.length} 项操作`)
const segmentStyle = (op: TraceOperation) => ({ left: `${Math.min(1, op.startOffsetMs / domain.value) * 100}%`, width: `${Math.max(0, Math.min(domain.value, op.endOffsetMs ?? props.trace?.elapsedMs ?? op.startOffsetMs) - op.startOffsetMs) / domain.value * 100}%`, top: `${26 + laneTypes.value.indexOf(op.type) * 28}px` })
const detailTabs = computed(() => selected.value?.type === 'assistant' ? ['概览', '回答', '计时'] : selected.value?.type === 'context' ? ['概览', '来源'] : ['概览', '参数', '结果'])
const labels: Record<string, string> = { attemptCount: '实际尝试', retryCount: '重试', modelCallCount: '模型调用', fallbackCount: '降级次数', selectedContextCount: '选中片段', candidateCount: '候选数', validCitations: '有效引用', droppedCitations: '丢弃引用', inputTokens: '输入 Token', outputTokens: '输出 Token', totalTokens: '合计 Token', requestedProvider: '请求方式', effectiveProvider: '生效方式', fallbackReason: '降级原因', errorCategory: '故障分类', retrievalMode: '实际路线', topK: '候选上限', minScore: '分数下限', enabled: '是否启用' }
const enumLabels: Record<string, string> = { heuristic: '规则重排', keyword_only: '仅关键词检索', dense_only: '仅向量检索', hybrid: '混合检索', disabled: '未启用', timeout: '超时', network: '网络故障', dependency_unavailable: '依赖暂不可用', unavailable: '暂不可用', provider_unavailable: '模型服务暂不可用', retry_exhausted: '重试已耗尽', unknown: '未提供', none: '无', not_configured: '未配置', invalid_response: '响应格式异常' }
const factValue = (v: string | number | boolean) => typeof v === 'boolean' ? v ? '是' : '否' : typeof v === 'string' ? enumLabels[v] ?? v : String(v)
const detailFacts = computed(() => {
  const s = selected.value
  if (!s) return []
  if (detailTab.value === '计时') return [['开始偏移', formatTraceTime(s.startOffsetMs)], ['结束偏移', s.endOffsetMs === null ? '未结束' : formatTraceTime(s.endOffsetMs)], ['调用耗时', duration(s)], ['首 Token 延迟', '未提供']]
  const parameters = new Set(['topK', 'minScore', 'enabled', 'requestedProvider'])
  const facts = Object.entries(s.facts).filter(([k]) => detailTab.value === '参数' ? parameters.has(k) : !parameters.has(k)).map(([k, v]) => [labels[k] ?? '事实', factValue(v)])
  return detailTab.value === '参数' ? facts.length ? facts : [['参数', '本次未提供可展示参数']] : [['状态', statusNames[s.status]], ['耗时', duration(s)], ...facts]
})
async function selectOperation(id: string) { selectedId.value = id; detailTab.value = '概览'; announcement.value = '已选中' + (selected.value?.title ?? '操作'); await nextTick(); if (!mobile.value && width.value < 1080) inspector.value?.scrollIntoView({ block: 'nearest' }); detailTitle.value?.focus({ preventScroll: true }) }
function segmentClick(e: MouseEvent, id: string) { if (e.detail === 0) void selectOperation(id) }
async function closeDetail(restore = true) { const id = selectedId.value; selectedId.value = ''; await nextTick(); if (restore) (root.value?.querySelector<HTMLElement>(`[data-trace-id="${id}"]`) ?? plot.value)?.focus({ preventScroll: true }) }
function clearRange() { range.value = null; frozenDomain.value = null; announcement.value = '已恢复全部时间'; plot.value?.focus({ preventScroll: true }) }
function createRange() { frozenDomain.value = domain.value; range.value = clampTraceRange(domain.value * .25, domain.value * .75, domain.value); commitRange(); nextTick(() => root.value?.querySelector<HTMLElement>('[data-trace-handle="0"]')?.focus()) }
function commitRange() { if (selected.value && !matches(selected.value)) { const inside = inspector.value?.contains(document.activeElement); void closeDetail(false); if (inside) plot.value?.focus({ preventScroll: true }) } announcement.value = rangeLabel.value }
watch([typeFilter, phaseFilter], commitRange)
watch(() => props.trace?.runId, () => { selectedId.value = ''; range.value = null; frozenDomain.value = null; typeFilter.value = 'all'; phaseFilter.value = 'all'; drag = null })
function detailTabKey(e: KeyboardEvent, tab: string) { if (!['ArrowLeft', 'ArrowRight'].includes(e.key)) return; e.preventDefault(); const tabs = detailTabs.value; detailTab.value = tabs[(tabs.indexOf(tab) + (e.key === 'ArrowRight' ? 1 : tabs.length - 1)) % tabs.length]!; nextTick(() => inspector.value?.querySelector<HTMLElement>('[aria-selected="true"]')?.focus()) }
type Drag = { pointer: number; x: number; y: number; anchor: number; before: [number, number] | null; mode: 'new' | 'pan' | '0' | '1'; box: DOMRect; total: number; op?: string; active: boolean }
let drag: Drag | null = null
function point(e: PointerEvent, d?: Drag) { const box = d?.box ?? plot.value!.getBoundingClientRect(); return Math.max(0, Math.min(d?.total ?? domain.value, (e.clientX - box.left) / box.width * (d?.total ?? domain.value))) }
function pointerDown(e: PointerEvent) {
  if (e.button !== 0 || !plot.value) return
  const target = e.target as HTMLElement, r = range.value, value = point(e), handle = target.closest<HTMLElement>('[data-trace-handle]')?.dataset.traceHandle
  drag = { pointer: e.pointerId, x: e.clientX, y: e.clientY, anchor: value, before: r ? [...r] : null,
    mode: handle === '0' || handle === '1' ? handle : r && value > r[0] && value < r[1] && !target.closest('[data-trace-segment]') ? 'pan' : 'new',
    box: plot.value.getBoundingClientRect(), total: domain.value, op: target.closest<HTMLElement>('[data-trace-segment]')?.dataset.traceSegment, active: false }
  plot.value.setPointerCapture(e.pointerId)
}
function pointerMove(e: PointerEvent) {
  const d = drag
  if (!d || d.pointer !== e.pointerId || !d.active && Math.abs(e.clientX - d.x) < 6) return
  if (!d.active && e.pointerType === 'touch' && Math.abs(e.clientY - d.y) > Math.abs(e.clientX - d.x)) return
  d.active = true; frozenDomain.value = d.total
  const p = point(e, d), old = d.before
  if (d.mode === '0' && old) range.value = [Math.max(0, Math.min(p, old[1] - 1)), old[1]]
  else if (d.mode === '1' && old) range.value = [old[0], Math.min(d.total, Math.max(p, old[0] + 1))]
  else if (d.mode === 'pan' && old) { const shift = Math.max(-old[0], Math.min(d.total - old[1], p - d.anchor)); range.value = [old[0] + shift, old[1] + shift] }
  else range.value = clampTraceRange(d.anchor, p, d.total)
}
function pointerUp(e: PointerEvent) { const d = drag; if (!d || d.pointer !== e.pointerId) return; drag = null; if (d.active) commitRange(); else if (d.op) void selectOperation(d.op) }
function cancelDrag() { if (drag) { range.value = drag.before; if (!drag.before) frozenDomain.value = null; drag = null } }
function rangeKey(e: KeyboardEvent, index: number) { if (!range.value || !['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(e.key)) return; e.preventDefault(); const r = [...range.value] as [number, number], i = index === 0 ? 0 : 1, step = Math.max(1, domain.value * (e.shiftKey ? .1 : .01)); let v = e.key === 'Home' ? 0 : e.key === 'End' ? domain.value : r[i] + (e.key === 'ArrowRight' ? step : -step); v = i === 0 ? Math.max(0, Math.min(v, r[1] - 1)) : Math.min(domain.value, Math.max(v, r[0] + 1)); r[i] = v; range.value = r; commitRange() }
function handleKey(e: KeyboardEvent) {
  if (!props.active) return
  if (e.key === 'Escape') { if (selected.value) { e.preventDefault(); void closeDetail() } else if (range.value) { e.preventDefault(); clearRange() } }
  if (e.key === 'Tab' && mobile.value && selected.value && inspector.value) {
    const elements = Array.from(inspector.value.querySelectorAll<HTMLElement>('button,summary,a[href]')).filter(el => !el.hasAttribute('disabled'))
    const first = elements[0], last = elements[elements.length - 1]
    if (e.shiftKey && (document.activeElement === first || document.activeElement === detailTitle.value)) { e.preventDefault(); last?.focus() }
    else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first?.focus() }
  }
}
let observer: ResizeObserver | undefined
onMounted(() => { if (root.value) { width.value = root.value.clientWidth; observer = new ResizeObserver(entries => { width.value = entries[0]?.contentRect.width ?? width.value }); observer.observe(root.value) } })
onUnmounted(() => { observer?.disconnect(); drag = null })
</script>
<style scoped src="./executionTrace.css"></style>
