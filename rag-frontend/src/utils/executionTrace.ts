/** Request-local, bounded trace projection. Never interpret arbitrary diagnostics as UI text. */
export type TraceStatus = 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED' | 'SKIPPED'
export type TraceType = 'tool' | 'assistant' | 'context' | 'other'
export interface TraceStage {
  stageId: string
  parentStageId: string | null
  stage: string
  status: TraceStatus
  startOffsetMs: number
  endOffsetMs: number | null
  facts: Record<string, string | number | boolean>
}
export interface ExecutionTrace {
  schemaVersion: 'trace-v1'
  runId: string
  seq: number
  traceId: string | null
  captureStatus: 'COMPLETE' | 'PARTIAL'
  elapsedMs: number
  stages: TraceStage[]
  reasonCode: string | null
  businessFinalState: string | null
}
export interface TraceOperation extends TraceStage {
  type: TraceType
  phase: string
  title: string
  description: string
}
const record = (v: unknown): v is Record<string, unknown> => !!v && typeof v === 'object' && !Array.isArray(v)
const id = (v: unknown): v is string => typeof v === 'string' && /^[A-Za-z0-9][A-Za-z0-9_-]{0,79}$/.test(v)
const time = (v: unknown): v is number => typeof v === 'number' && Number.isFinite(v) && v >= 0 && v <= 86400000
const statuses = new Set(['RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'SKIPPED'])
const states = new Set(['ANSWER', 'NO_ANSWER', 'UNSUPPORTED', 'INVALID', 'ERROR', 'CANCELLED'])
const reasons = new Set(['INVALID_TRACE', 'SEQUENCE_GAP', 'TRUNCATED', 'INCOMPLETE', 'UNKNOWN_STAGE'])
const scalarKeys = new Set(['attemptCount', 'retryCount', 'modelCallCount', 'fallbackCount', 'selectedContextCount', 'candidateCount', 'validCitations', 'droppedCitations', 'inputTokens', 'outputTokens', 'totalTokens', 'topK', 'minScore'])
const enumKeys = new Set(['requestedProvider', 'effectiveProvider', 'fallbackReason', 'errorCategory', 'retrievalMode'])
const safeEnums = new Set(['none', 'unknown', 'heuristic', 'nvidia', 'openai', 'ollama', 'anthropic', 'mock', 'local', 'hybrid', 'dense_only', 'keyword_only', 'disabled', 'other', 'timeout', 'network', 'unavailable', 'invalid_response', 'incomplete_rankings', 'retry_exhausted', 'provider_error', 'dependency_unavailable', 'rate_limited', 'error', 'cancelled', 'not_configured', 'provider_unavailable', 'execution_error', 'milvus', 'fallback'])
const taxonomy: Record<string, [TraceType, string, string, string]> = {
  'rag.cache.lookup': ['tool', 'prepare', '检查回答缓存', '确认是否已有可复用的回答'],
  'rag.embedding.query': ['tool', 'retrieve', '生成查询向量', '将问题转换成可用于相似检索的向量'],
  'rag.vector.search': ['tool', 'retrieve', '向量召回', '按语义相似度查找候选片段'],
  'rag.keyword.search': ['tool', 'retrieve', '关键词召回', '补充名称、术语和精确匹配的线索'],
  'rag.retrieval.fusion': ['tool', 'retrieve', '融合检索结果', '合并实际召回路线中的候选'],
  'rag.rerank': ['tool', 'retrieve', '重排候选片段', '按相关程度选择本次回答依据'],
  'rag.prompt.build': ['context', 'generate', '组织回答依据', '将已选资料整理为本次生成输入'],
  'rag.llm.request': ['assistant', 'generate', '生成回答', '结合本次回答依据生成文本'],
  'rag.citation.validate': ['tool', 'validate', '校验引用', '检查引用与已选资料的对应关系'],
}
const containers = new Set(['rag.ask', 'rag.retrieval', 'rag.generation'])
export const TRACE_LIMITS = { stages: 128, events: 256, incrementalBytes: 2048, snapshotBytes: 65536, totalBytes: 589824 }
const bytes = (v: unknown) => new TextEncoder().encode(typeof v === 'string' ? v : JSON.stringify(v)).length

function stageFrom(v: unknown): TraceStage | null {
  if (!record(v) || !id(v.stageId) || !(v.parentStageId === null || id(v.parentStageId))
    || typeof v.stage !== 'string' || !/^[a-z][a-z0-9.]{0,63}$/.test(v.stage)
    || !statuses.has(String(v.status)) || !time(v.startOffsetMs)
    || !(v.endOffsetMs === null || time(v.endOffsetMs))
    || (typeof v.endOffsetMs === 'number' && v.endOffsetMs < v.startOffsetMs)) return null
  if (v.status === 'RUNNING' && v.endOffsetMs !== null) return null
  if (v.status !== 'RUNNING' && v.endOffsetMs === null) return null
  const facts: TraceStage['facts'] = {}
  if (record(v.facts)) for (const [k, value] of Object.entries(v.facts)) {
    if (scalarKeys.has(k) && typeof value === 'number' && Number.isFinite(value) && value >= 0 && value <= 1e12) facts[k] = value
    else if (enumKeys.has(k) && typeof value === 'string' && safeEnums.has(value)) facts[k] = value
    else if (k === 'enabled' && typeof value === 'boolean') facts[k] = value
  }
  return { stageId: v.stageId, parentStageId: v.parentStageId, stage: v.stage,
    status: v.status as TraceStatus, startOffsetMs: v.startOffsetMs, endOffsetMs: v.endOffsetMs as number | null, facts }
}
export function parseExecutionTrace(v: unknown): ExecutionTrace | null {
  try {
    if (!record(v) || bytes(v) > TRACE_LIMITS.snapshotBytes || v.schemaVersion !== 'trace-v1'
      || !id(v.runId) || !Number.isSafeInteger(v.seq) || Number(v.seq) < 1
      || !['COMPLETE', 'PARTIAL'].includes(String(v.captureStatus)) || !time(v.elapsedMs)
      || !Array.isArray(v.stages) || v.stages.length > TRACE_LIMITS.stages) return null
    const stages = v.stages.map(stageFrom)
    if (stages.some(s => !s) || new Set(stages.map(s => s!.stageId)).size !== stages.length) return null
    if (stages.some(s => s!.startOffsetMs > (v.elapsedMs as number) || (s!.endOffsetMs ?? 0) > (v.elapsedMs as number))) return null
    const unknown = stages.some(s => !taxonomy[s!.stage] && !containers.has(s!.stage))
    return { schemaVersion: 'trace-v1', runId: v.runId, seq: Number(v.seq),
      traceId: typeof v.traceId === 'string' && /^[0-9a-f]{32}$/.test(v.traceId) ? v.traceId : null,
      captureStatus: unknown ? 'PARTIAL' : v.captureStatus as ExecutionTrace['captureStatus'], elapsedMs: v.elapsedMs,
      stages: stages as TraceStage[], reasonCode: unknown ? 'UNKNOWN_STAGE' : reasons.has(String(v.reasonCode)) ? String(v.reasonCode) : null,
      businessFinalState: states.has(String(v.businessFinalState)) ? String(v.businessFinalState) : null }
  } catch { return null }
}
export class TraceAccumulator {
  value: ExecutionTrace | undefined
  invalid = false
  private events = 0
  private size = 0
  ingest(raw: string): void {
    try {
      const n = bytes(raw)
      if (++this.events > TRACE_LIMITS.events + 1 || (this.size += n) > TRACE_LIMITS.totalBytes || n > TRACE_LIMITS.snapshotBytes + 512) return this.markInvalid('TRUNCATED')
      const e: unknown = JSON.parse(raw)
      if (!record(e) || e.schemaVersion !== 'trace-v1' || !id(e.runId) || !Number.isSafeInteger(e.seq) || Number(e.seq) < 1) return this.markInvalid()
      if (this.value && this.value.runId !== e.runId) return this.markInvalid()
      if (this.value && Number(e.seq) <= this.value.seq) return
      if (e.kind === 'execution.snapshot') {
        const snapshot = parseExecutionTrace(e.snapshot)
        if (!snapshot || snapshot.runId !== e.runId || snapshot.seq !== e.seq) return this.markInvalid()
        this.value = snapshot
        this.invalid = snapshot.captureStatus !== 'COMPLETE'
        return
      }
      if (n > TRACE_LIMITS.incrementalBytes) return this.markInvalid('TRUNCATED')
      const prior = this.value
      if (!prior && e.kind !== 'execution.start') return this.markInvalid('SEQUENCE_GAP')
      if (!prior) this.value = { schemaVersion: 'trace-v1', runId: e.runId, seq: 0, traceId: null,
        captureStatus: 'COMPLETE', elapsedMs: 0, stages: [], reasonCode: null, businessFinalState: null }
      const run = this.value!
      if (Number(e.seq) !== run.seq + 1) { this.invalid = true; run.captureStatus = 'PARTIAL'; run.reasonCode = 'SEQUENCE_GAP' }
      run.seq = Number(e.seq)
      if (e.kind === 'stage.upsert') {
        const stage = stageFrom(e.stage)
        if (!stage) return this.markInvalid()
        if (!taxonomy[stage.stage] && !containers.has(stage.stage)) { this.invalid = true; run.captureStatus = 'PARTIAL'; run.reasonCode = 'UNKNOWN_STAGE' }
        const i = run.stages.findIndex(s => s.stageId === stage.stageId)
        if (i < 0 && run.stages.length >= TRACE_LIMITS.stages) return this.markInvalid('TRUNCATED')
        if (i < 0) run.stages.push(stage)
        else {
          const old = run.stages[i]!
          if (old.stage !== stage.stage || old.startOffsetMs !== stage.startOffsetMs || (old.status !== 'RUNNING' && stage.status === 'RUNNING')) return this.markInvalid()
          run.stages[i] = stage
        }
        run.elapsedMs = Math.max(run.elapsedMs, stage.endOffsetMs ?? stage.startOffsetMs)
      } else if (e.kind !== 'execution.start') return this.markInvalid()
      if (time(e.elapsedMs)) run.elapsedMs = Math.max(run.elapsedMs, e.elapsedMs)
      this.value = { ...run, stages: [...run.stages] }
    } catch { this.markInvalid() }
  }
  markInvalid(reason = 'INVALID_TRACE'): void {
    this.invalid = true
    if (this.value) this.value = { ...this.value, captureStatus: 'PARTIAL', reasonCode: reason }
  }
}
export function traceOperations(run: ExecutionTrace): TraceOperation[] {
  return run.stages.filter(s => !containers.has(s.stage)).map(s => {
    const [type, phase, title, description] = taxonomy[s.stage] ?? ['other', 'other', '其他操作', '已记录一个尚未分类的实际阶段']
    return { ...s, type, phase, title, description }
  }).sort((a, b) => a.startOffsetMs - b.startOffsetMs || a.stageId.localeCompare(b.stageId))
}
export function intersectTrace(stage: TraceStage, range: [number, number] | null, elapsedMs: number): boolean {
  return !range || stage.startOffsetMs <= range[1] && (stage.endOffsetMs ?? elapsedMs) >= range[0]
}
export function clampTraceRange(a: number, b: number, total: number): [number, number] {
  const min = Math.max(1, total)
  const left = Math.max(0, Math.min(min - 1, Math.min(a, b)))
  return [left, Math.max(left + 1, Math.min(min, Math.max(a, b)))]
}
export function formatTraceTime(ms: number): string { return ms < 1000 ? `${Math.round(ms)} ms` : `${(ms / 1000).toFixed(2)} s` }
export function releaseOldTraces(messages: { loading?: boolean; executionTrace?: ExecutionTrace; traceReleased?: boolean }[]): void {
  const ended = messages.filter(m => m.executionTrace && !m.loading)
  for (const message of ended.slice(0, Math.max(0, ended.length - 20))) { message.executionTrace = undefined; message.traceReleased = true }
}
