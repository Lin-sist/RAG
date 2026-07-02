# Stage 1：生成与引用质量评测闭环

## 当前状态

本阶段已完成 `scripts/run_rag_eval.py` 的 generation / citation 评测通道增强：

- 保留既有 retrieval-only 指标口径，`--skip-ask` 路径不变。
- 保留并显式报告既有客观生成指标：answer keyword hit、citation source hit、citation snippet hit、no-answer accuracy、askErrors、rateLimitErrors、retry count。
- 新增可选 LLM-as-judge 通道，默认 `--judge-mode off`，不会自动触发外部模型调用。
- 仅当显式传入 `--judge-mode llm` 且提供 judge model / api key 时，才调用 OpenAI-compatible `/chat/completions` 做 faithfulness / relevance 判定。
- details JSON 保留每题 judge 原始判定、judgeError、judgeSkipped，便于后续人工抽样复核。

## 外部调用边界

本次提交未运行批量 `/api/qa/ask` 或 LLM judge live eval。原因是 v4 计划要求批量 ask、LLM-judge、真实 provider 调用前先说明预计调用量、模型、费用/限流风险并获得用户确认。

因此当前还没有宣称首份生成/引用质量 baseline 已完成；Stage 1 的 live baseline 仍待用户确认后执行。

## 安全闸预检结果

无网络 plan-only 预检已完成，当前评测集共有 30 条样本：27 条 answerable、3 条 no-answer。

完整 Stage 1 baseline 如保持 `--judge-mode off`：

- 预计 `/api/qa/debug/retrieve` 调用：30 次。
- 预计 `/api/qa/ask` 调用：30 次。
- 预计外部 LLM judge 调用：0 次。
- 预计报告：`docs/eval/reports/stage1-genquality-objective.md`。
- 预计明细：`docs/eval/reports/stage1-genquality-objective-details.json`。

如显式开启 `--judge-mode llm`，同一批样本预计额外产生 27 次 OpenAI-compatible judge 调用；模型、base URL、API key 必须由用户确认后再执行。当前未启用 judge，因此不会产生外部 judge 成本。

## 2026-07-02 小样本 smoke 状态

用户已确认先跑 `fact-001 + no-answer-001` 小样本 smoke，计划调用量为 2 次 debug retrieve、2 次 `/api/qa/ask`、0 次 LLM judge；随后用户明确允许将本地评测样本和 `test-data` 文档内容发送到当前配置的外部 LLM/Embedding provider。

执行记录：

- 首次执行真实 smoke 时，后端未启动，登录阶段失败：`Cannot connect to http://localhost:8080/auth/login`。
- 随后启动 Docker Desktop，并执行 `docker compose --env-file .env.local up -d`；`rag-mysql`、`rag-redis`、`rag-milvus`、`rag-etcd`、`rag-minio` 均为 healthy。
- 后端通过 `start-backend.ps1` 启动，日志 `logs/stage1-v4-smoke-backend.out.log` 显示 Tomcat started on port 8080，且 `/auth/login` 探测成功。
- 用户明确批准外部数据出站后，执行真实 smoke；索引完成并生成 `docs/eval/reports/stage1-reproducible-eval-metadata.json`，本次评测 KB 为 id=14，vector collection=`kb_3dab7e9b88ea4888`，3 个文档、50 个 chunk。
- smoke 报告已生成：`docs/eval/reports/stage1-genquality-smoke.md` 与 `docs/eval/reports/stage1-genquality-smoke-details.json`。报告状态为 `PARTIAL`，`askErrors=2`、`retrieveErrors=0`、`retry count=4`、`rateLimitErrors=0`、`skippedJudge=2`。
- 小样本 retrieval 指标可比较：Recall@3=100.00%、Recall@5=100.00%、MRR=1.0000、Top1 source accuracy=100.00%。
- generation/citation 指标仍不可比较：两个样本 `/api/qa/ask` 均为 `timed out`，ask successful samples=0；后端日志显示当前 OpenAI-compatible provider 调用 `/chat/completions` 多次 timeout，并出现一次 NVIDIA endpoint `503 Service Unavailable`。

因此当前小样本 smoke 已验证检索链路与可复现 KB 准备链路，但未产出可用的生成/引用质量基线；剩余问题是当前外部 LLM provider 不稳定或超时，需要先处理 provider 可用性/超时策略，再继续完整 Stage 1 baseline。

## 已验证项

```powershell
python -B -m py_compile scripts\run_rag_eval.py scripts\test_run_rag_eval.py
python -B scripts\run_rag_eval.py --help
python -B scripts\test_run_rag_eval.py
```

结果：

- Python 编译通过。
- `run_rag_eval.py --help` 已展示 `--judge-mode`、`--judge-base-url`、`--judge-model`、`--judge-temperature`、`--fail-on-judge-errors` 等参数。
- `scripts/test_run_rag_eval.py` 通过，覆盖 judge JSON 解析、分数裁剪、judge 开关条件。
- `run_rag_eval.py --plan-only` 已验证完整基线预计调用量：30 次 debug retrieve、30 次 ask、judge off 时 0 次 LLM judge、judge llm 时 27 次 LLM judge。
- `run_reproducible_rag_eval.py --plan-only --include-ask` 已验证可在不登录、不建库、不上传、不调用后端的情况下输出完整执行计划，并直接列出 selectedSampleCount / estimatedLiveCalls。
- `run_reproducible_rag_eval.py` 已前置校验样本选择；若 `--sample-id`/`--sample-limit` 导致 0 条样本，会在登录、建库、上传前失败。

## 后续 live baseline 建议

在用户确认外部调用后，建议先跑小样本 smoke，再跑完整 Stage 1 baseline。

小样本 smoke 示例：

先做无网络 plan 预检：

```powershell
python -B scripts\run_reproducible_rag_eval.py `
  --plan-only `
  --include-ask `
  --sample-id fact-001 `
  --sample-id no-answer-001 `
  --report docs\eval\reports\stage1-genquality-smoke.md `
  --details-json docs\eval\reports\stage1-genquality-smoke-details.json
```

用户确认后再执行实际 smoke：

```powershell
python -B scripts\run_reproducible_rag_eval.py `
  --include-ask `
  --sample-id fact-001 `
  --sample-id no-answer-001 `
  --ask-delay-seconds 2 `
  --max-ask-retries 2 `
  --retry-backoff-seconds 10 `
  --report docs\eval\reports\stage1-genquality-smoke.md `
  --details-json docs\eval\reports\stage1-genquality-smoke-details.json
```

完整 Stage 1 baseline 示例：

```powershell
python -B scripts\run_reproducible_rag_eval.py `
  --include-ask `
  --ask-delay-seconds 2 `
  --max-ask-retries 2 `
  --retry-backoff-seconds 10 `
  --report docs\eval\reports\stage1-genquality-objective.md `
  --details-json docs\eval\reports\stage1-genquality-objective-details.json
```

如启用 LLM judge，需要额外显式配置：

```powershell
$env:RAG_EVAL_JUDGE_MODE="llm"
$env:RAG_EVAL_JUDGE_MODEL="<judge-model>"
$env:RAG_EVAL_JUDGE_API_KEY="<judge-api-key>"
$env:RAG_EVAL_JUDGE_BASE_URL="<openai-compatible-base-url>"
```

正式报告必须记录 `askErrors`、`rateLimitErrors`、`judgeErrors`、retry 次数和 judge 配置。
