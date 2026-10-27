import { createServer as createHttpServer } from 'node:http'
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createServer } from 'vite'
import { fileURLToPath } from 'node:url'
import { traceFixture, traceEvents } from './trace-fixtures.mjs'

const server = await createServer({ configFile: false, optimizeDeps: { noDiscovery: true, include: [] }, server: { middlewareMode: true, hmr: { server: createHttpServer() } }, resolve: { alias: { '@': fileURLToPath(new URL('../src', import.meta.url)) } }, appType: 'custom' })
try {
  const { TraceAccumulator, parseExecutionTrace, intersectTrace, traceOperations, clampTraceRange, releaseOldTraces } = await server.ssrLoadModule('/src/utils/executionTrace.ts')
  const { createStructuredSSEParser } = await server.ssrLoadModule('/src/composables/sseParser.ts')
  const stage = (overrides = {}) => ({ stageId: 's1', parentStageId: null, stage: 'rag.vector.search', status: 'SUCCEEDED', startOffsetMs: 10, endOffsetMs: 30, facts: {}, ...overrides })
  const snapshot = (overrides = {}) => ({ schemaVersion: 'trace-v1', runId: 'run-1', seq: 4, traceId: null, captureStatus: 'COMPLETE', elapsedMs: 100, stages: [stage()], reasonCode: null, businessFinalState: 'ANSWER', ...overrides })
  const event = (seq, kind, extra = {}) => JSON.stringify({ schemaVersion: 'trace-v1', runId: 'run-1', seq, kind, ...extra })
  await test('facts exclude raw prompts, paths, arbitrary provider names and invalid timings', () => {
    const result = parseExecutionTrace(snapshot({ stages: [stage({ facts: { topK: 5, requestedProvider: 'openai', prompt: 'secret', effectiveProvider: 'private-provider', path: 'C:/private' } })] }))
    assert.deepEqual(result.stages[0].facts, { topK: 5, requestedProvider: 'openai' })
    assert.equal(parseExecutionTrace(snapshot({ stages: [stage({ endOffsetMs: 2 })] })), null)
    assert.equal(parseExecutionTrace(snapshot({ stages: [stage({ endOffsetMs: 101 })] })), null)
    assert.equal(parseExecutionTrace(snapshot({ stages: Array.from({ length: 129 }, (_, i) => stage({ stageId: 's' + i })) })), null)
  })
  await test('seq gaps mark partial, duplicates are ignored, final snapshot independently recovers', () => {
    const acc = new TraceAccumulator()
    acc.ingest(event(1, 'execution.start'))
    acc.ingest(event(3, 'stage.upsert', { stage: stage() }))
    assert.equal(acc.value.captureStatus, 'PARTIAL')
    acc.ingest(event(2, 'stage.upsert', { stage: stage({ status: 'RUNNING', endOffsetMs: null }) }))
    assert.equal(acc.value.stages[0].status, 'SUCCEEDED')
    acc.ingest(event(4, 'execution.snapshot', { snapshot: snapshot() }))
    assert.equal(acc.invalid, false)
    assert.equal(acc.value.captureStatus, 'COMPLETE')
    acc.ingest(JSON.stringify({ schemaVersion: 'trace-v1', runId: 'run-other', seq: 5, kind: 'execution.start' }))
    assert.equal(acc.invalid, true)
  })
  await test('unknown actual stages stay visible and containers do not pretend to be operations', () => {
    const run = parseExecutionTrace(snapshot({ stages: [stage({ stage: 'rag.ask' }), stage({ stageId: 's2', stage: 'rag.future.stage' })] }))
    assert.equal(run.captureStatus, 'PARTIAL')
    assert.deepEqual(traceOperations(run).map(o => o.type), ['other'])
    const live = new TraceAccumulator()
    live.ingest(event(1, 'execution.start'))
    live.ingest(event(2, 'stage.upsert', { stage: stage({ stage: 'rag.future.stage' }) }))
    assert.equal(live.value.captureStatus, 'PARTIAL')
    assert.equal(live.invalid, true)
    assert.equal(traceOperations(live.value)[0].type, 'other')
  })
  await test('range uses interval intersection including overlaps and point operations', () => {
    assert.equal(intersectTrace(stage(), [20, 25], 100), true)
    assert.equal(intersectTrace(stage({ startOffsetMs: 23, endOffsetMs: 23 }), [20, 25], 100), true)
    assert.equal(intersectTrace(stage(), [31, 40], 100), false)
    assert.deepEqual(clampTraceRange(70, 20, 100), [20, 70])
    assert.deepEqual(clampTraceRange(-10, 120, 100), [0, 100])
  })
  await test('retains latest twenty finished traces plus the running trace without removing answers', () => {
    const messages = Array.from({ length: 22 }, (_, i) => ({ content: 'answer-' + i, loading: i === 21, executionTrace: snapshot({ runId: 'run-' + i }) }))
    releaseOldTraces(messages)
    assert.equal(messages.filter(m => m.executionTrace).length, 21)
    assert.equal(messages[0].traceReleased, true)
    assert.equal(messages[0].content, 'answer-0')
    assert.ok(messages[21].executionTrace)
    messages[21].loading = false; releaseOldTraces(messages)
    assert.equal(messages.filter(m => m.executionTrace).length, 20)
  })
  await test('synthetic success/cache/empty/fallback/error contracts remain bounded without invented stages', () => {
    for (const scenario of ['trace-success', 'trace-cache', 'trace-none', 'trace-fallback', 'trace-error', 'trace-unsupported']) {
      const snapshot = traceFixture(scenario), events = traceEvents(snapshot), acc = new TraceAccumulator()
      for (const event of events) acc.ingest(JSON.stringify(event))
      assert.equal(acc.invalid, false, scenario)
      assert.equal(acc.value.businessFinalState, snapshot.businessFinalState)
      assert.ok(Buffer.byteLength(JSON.stringify(snapshot)) < 65536)
      assert.ok(events.length <= 256)
      if (scenario === 'trace-cache') assert.deepEqual(traceOperations(acc.value).map(s => s.stage), ['rag.cache.lookup'])
      if (scenario === 'trace-none') assert.ok(traceOperations(acc.value).every(s => s.type !== 'assistant'))
    }
  })
  await test('optional malformed trace never weakens business terminal validation', () => {
    const term = { schemaVersion: 'structured-v1', finalState: 'ANSWER', reason: 'NONE', citations: [], metadata: {}, classifierVersion: 'legacy', effectiveStrategy: 'legacy', policyVersion: 'legacy', routeReason: 'LEGACY', budgetOutcome: null, usage: null }
    const wire = `event:trace\ndata:bad trace\n\nevent:text\ndata:answer\n\nevent:terminal\ndata:${JSON.stringify(term)}\n\n`
    const acc = new TraceAccumulator(), text = []
    const parser = createStructuredSSEParser(chunk => text.push(chunk), raw => acc.ingest(raw))
    parser.push(wire); parser.finish()
    assert.equal(acc.invalid, true)
    assert.deepEqual(text, ['answer'])
    assert.equal(parser.protocolError, null)
    const old = createStructuredSSEParser(() => {})
    old.push(wire); old.finish(); assert.ok(old.protocolError)
    parser.push(`event:terminal\ndata:${JSON.stringify(term)}\n\n`); parser.finish()
    assert.ok(parser.protocolError)
  })
} finally { await server.close() }
