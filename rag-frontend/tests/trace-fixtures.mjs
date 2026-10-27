// Synthetic lifecycle only; imported exclusively by the fail-closed local preview server.
export function traceFixture(scenario = 'trace-success') {
  const names = ['rag.cache.lookup', 'rag.embedding.query', 'rag.vector.search', 'rag.keyword.search', 'rag.retrieval.fusion', 'rag.rerank', 'rag.prompt.build', 'rag.llm.request', 'rag.citation.validate']
  const starts = [0, 20, 170, 180, 470, 490, 660, 720, 2350], ends = [20, 170, 470, 300, 490, 660, 720, 2350, 2400]
  const finalState = { 'trace-none': 'NO_ANSWER', 'trace-error': 'ERROR', 'trace-unsupported': 'UNSUPPORTED' }[scenario] ?? 'ANSWER'
  let stages = names.map((stage, i) => ({ stageId: 's' + (i + 1), parentStageId: 'root', stage, status: 'SUCCEEDED', startOffsetMs: starts[i], endOffsetMs: ends[i], facts: i === 5 ? { requestedProvider: 'nvidia', effectiveProvider: scenario === 'trace-fallback' ? 'heuristic' : 'nvidia', fallbackCount: scenario === 'trace-fallback' ? 1 : 0, candidateCount: 12, selectedContextCount: 5 } : i === 7 ? { requestedProvider: 'openai', effectiveProvider: 'openai', attemptCount: 1 } : i === 6 ? { selectedContextCount: 5 } : i === 0 ? { enabled: true } : {} }))
  if (scenario === 'trace-cache') stages = stages.slice(0, 1)
  if (scenario === 'trace-none') stages = stages.slice(0, 6)
  if (scenario === 'trace-error') { stages = stages.slice(0, 8); stages[7].status = 'FAILED' }
  if (scenario === 'trace-unsupported') stages = []
  const elapsedMs = stages[stages.length - 1]?.endOffsetMs ?? 1
  return { schemaVersion: 'trace-v1', runId: 'synthetic-run-' + scenario, seq: stages.length * 2 + 4, traceId: null, captureStatus: 'COMPLETE', elapsedMs,
    stages: [{ stageId: 'root', parentStageId: null, stage: 'rag.ask', status: finalState === 'ERROR' ? 'FAILED' : 'SUCCEEDED', startOffsetMs: 0, endOffsetMs: elapsedMs, facts: {} }, ...stages], reasonCode: null, businessFinalState: finalState }
}
export function traceEvents(snapshot) {
  let seq = 1
  const base = { schemaVersion: 'trace-v1', runId: snapshot.runId }
  const events = [{ ...base, seq, kind: 'execution.start', elapsedMs: 0 }]
  const changes = snapshot.stages.flatMap(stage => [
    { stage: { ...stage, status: 'RUNNING', endOffsetMs: null }, time: stage.startOffsetMs },
    { stage, time: stage.endOffsetMs },
  ]).sort((a, b) => a.time - b.time)
  for (const change of changes) events.push({ ...base, seq: ++seq, kind: 'stage.upsert', stage: change.stage, elapsedMs: change.time })
  events.push({ ...base, seq: ++seq, kind: 'execution.snapshot', snapshot: { ...snapshot, seq } })
  return events
}
