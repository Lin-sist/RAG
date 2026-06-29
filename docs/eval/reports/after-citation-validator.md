# RAG Eval Baseline 002 Local

- Generated at: 2026-06-02T01:01:03.190249+00:00
- Base URL: `http://localhost:8080`
- Knowledge base ID: `6`
- Eval set: `docs\eval\rag_eval_set.jsonl`
- topK: `5`
- minScore: `0.3`
- enableRerank: `True`
- Duration: `180.43s`

## Summary Metrics

| Metric | Value |
|---|---:|
| Samples | 30 |
| Answerable samples | 27 |
| No-answer samples | 3 |
| Recall@3 | 54.90% |
| Recall@5 | 54.90% |
| MRR | 0.6667 |
| Top1 source accuracy | 100.00% |
| Answer keyword hit rate | 78.85% |
| Citation hit rate | 0.00% |
| Citation source hit rate | 0.00% |
| Citation snippet hit rate | 100.00% |
| Unsupported citation count | 0 |
| No-answer citation violation count | 0 |
| No-answer accuracy | 100.00% |

## Sample Results

| ID | Type | Retrieve | First Match | Keyword Hit | Citation Source | Citation Snippet | Unsupported Citations | No-answer OK | Errors |
|---|---|---:|---:|---:|---:|---:|---:|---:|---|
| fact-001 | fact | 2/2 | 1 | 4/4 | 0/1 | 1/1 | 0 | - |  |
| fact-002 | fact | 1/2 | 1 | 3/3 | 0/2 | - | 0 | - |  |
| fact-003 | fact | 0/1 | - | 3/3 | 0/1 | - | 0 | - |  |
| fact-004 | fact | 2/2 | 1 | 1/2 | 0/1 | - | 0 | - |  |
| fact-005 | fact | 0/2 | - | 4/4 | 0/1 | - | 0 | - |  |
| fact-006 | fact | 2/2 | 1 | 3/4 | 0/1 | - | 0 | - |  |
| fact-007 | fact | 1/2 | 1 | 5/5 | 0/1 | - | 0 | - |  |
| fact-008 | fact | 0/2 | - | 4/4 | 0/1 | - | 0 | - |  |
| fact-009 | fact | 1/2 | 1 | 4/4 | 0/1 | - | 0 | - |  |
| fact-010 | fact | 0/1 | - | 2/3 | 0/1 | - | 0 | - |  |
| definition-001 | definition | 1/2 | 1 | 4/4 | 0/1 | 1/1 | 0 | - |  |
| definition-002 | definition | 2/2 | 1 | 3/3 | 0/1 | - | 0 | - |  |
| definition-003 | definition | 0/2 | - | 3/3 | 0/1 | 1/1 | 0 | - |  |
| definition-004 | definition | 0/2 | - | 2/4 | 0/1 | 1/1 | 0 | - |  |
| definition-005 | definition | 2/2 | 1 | 3/3 | 0/1 | - | 0 | - |  |
| definition-006 | definition | 2/2 | 1 | 4/4 | 0/1 | - | 0 | - |  |
| definition-007 | definition | 1/2 | 1 | 2/4 | 0/1 | - | 0 | - |  |
| definition-008 | definition | 2/2 | 1 | 4/4 | 0/1 | - | 0 | - |  |
| reasoning-001 | reasoning | 0/2 | - | 0/4 | 0/1 | - | 0 | - |  |
| reasoning-002 | reasoning | 2/2 | 1 | 1/4 | 0/1 | - | 0 | - |  |
| reasoning-003 | reasoning | 0/2 | - | 0/4 | 0/1 | - | 0 | - |  |
| reasoning-004 | reasoning | 1/2 | 1 | 3/4 | 0/2 | - | 0 | - |  |
| reasoning-005 | reasoning | 0/1 | - | 3/3 | 0/1 | - | 0 | - |  |
| reasoning-006 | reasoning | 2/2 | 1 | 3/3 | 0/1 | - | 0 | - |  |
| multi-hop-001 | multi_hop | 2/2 | 1 | 7/7 | 0/1 | - | 0 | - |  |
| multi-hop-002 | multi_hop | 1/2 | 1 | 5/5 | 0/1 | - | 0 | - |  |
| multi-hop-003 | multi_hop | 1/2 | 1 | 2/5 | 0/2 | - | 0 | - |  |
| no-answer-001 | no_answer | - | - | - | - | - | 0 | yes |  |
| no-answer-002 | no_answer | - | - | - | - | - | 0 | yes |  |
| no-answer-003 | no_answer | - | - | - | - | - | 0 | yes |  |

## Field Coverage

- `debug/retrieve` is used for Recall@3, Recall@5, MRR, and Top1 source accuracy.
- `ask` is used for answer keyword hit rate, citation hit rate, and no-answer accuracy.
- `queryVariants`, `rank`, `score`, `source`, `documentId`, `chunkId`, `contentPreview`, and `metadata` are expected in debug output.

## Current Limitations

- `contentPreview` is a preview, not the full chunk, so long expected snippets may undercount recall.
- Citation source hit rate checks expected source names in returned citations.
- Citation snippet hit rate verifies each returned citation against the `contexts` returned by `/api/qa/ask` using exact match or token overlap.
- Answer scoring is keyword based and does not use an LLM judge.
- Metrics assume the three `test-data/*.md` files were uploaded with recognizable file names or document titles.
