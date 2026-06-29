# RAG Eval Report

- Generated at: 2026-06-03T15:23:06.556861+00:00
- Report status: `FAILED`
- askErrors count: `0`
- retrieveErrors count: `0`
- skippedAsk count: `0`
- rateLimitErrors count: `0`
- retry count: `0`
- Metrics safe for comparison: `no`
- Base URL: `http://localhost:8080`
- Knowledge base ID: `6`
- Eval set: `docs\eval\rag_eval_set.jsonl`
- topK: `5`
- minScore: `0.3`
- enableRerank: `True`
- skipAsk: `False`
- askDelaySeconds: `12.0`
- maxAskRetries: `4`
- retryBackoffSeconds: `20.0`
- Duration: `4.14s`

## Run Status

Backend login failed, so no live metrics were collected.

```text
Cannot connect to http://localhost:8080/auth/login: [WinError 10061] 由于目标计算机积极拒绝，无法连接。
```

Diagnosis: 后端未启动或端口不对。请先启动 MySQL/Redis/Milvus，再启动 RagQaApplication.main()。

The eval set and runner are still ready to use after the backend, database, Redis, Milvus, and model credentials are available.

## Failed Retrieval Cases

No cases.

## Failed Citation Cases

No cases.

## Low Answer Keyword Hit Cases

No cases.

## No-answer Cases

No cases.

## Source Normalization Diagnostics

| ID | Expected normalized | Retrieved candidate normalized | Citation candidate normalized |
|---|---|---|---|

## Citation Diagnostics

| ID | Returned | Source Hits | Snippet Hits | Validation | Unsupported |
|---|---:|---:|---:|---|---:|
