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
