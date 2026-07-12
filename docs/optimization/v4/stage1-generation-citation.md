# Stage 1：生成与引用质量评测闭环

## 阶段结论

**状态：已完成（2026-07-12）。**

本阶段在不改变既有 retrieval 指标定义、分块参数 `420/80` 和 RRF 融合逻辑的前提下，完成了 generation / citation / no-answer 的可复现评测闭环。最终代码与配置基线为 Git HEAD `5293c53869b544814bd608d684b64f0c364b4cf6`。

本阶段可以作为“v3 检索质量工程 + v4 Stage 1 生成/引用质量闭环”的合并 checkpoint；不代表 v4 Stage 2～4 已完成。

## 已落地能力

- `scripts/run_rag_eval.py` 保留 Recall@3/5、MRR、Top1 source accuracy，并增加 answer keyword hit、citation source/snippet hit、unsupported citation、no-answer accuracy/violation 等客观指标。
- 报告显式区分 `CLEAN`、`PARTIAL`、`RETRIEVAL_ONLY`、`FAILED`，同时记录 ask/retrieve/judge error、429、retry 和 skipped judge。
- `scripts/run_reproducible_rag_eval.py` 固定 KB、fixture、配置快照和 Git HEAD；`--keep-existing` 现在是只读复用，目标 KB 不存在时直接失败，不再隐式创建或上传。
- LLM-as-judge 仍为显式可选能力，默认 `judge-mode=off`，不会自动产生额外外部调用。
- 后端生成错误会输出脱敏 provider、endpoint、model、timeout、retry 与错误类别，runner 会把诊断带入明细。
- no-answer 协议固定为“知识库中没有足够信息回答该问题。”；拒答不返回 citations、不触发 citation fallback，并输出 `metadata.status=no_result`。
- Python 缓存已从版本控制移除，`.gitignore` 明确忽略 `__pycache__/` 与 `*.pyc`。

## Provider 与配置决策

当前通过 NVIDIA OpenAI-compatible `/chat/completions` 调用 Qwen：

- 默认模型：`qwen/qwen3.5-122b-a10b`
- 后端超时：`120s`
- 后端最大重试：`0`
- 正式 baseline runner：`ask-timeout=130s`、`ask-delay-seconds=1`、`max-ask-retries=1`、`retry-backoff-seconds=5`

选择依据：

1. `qwen/qwen3-next-80b-a3b-instruct` 在无项目数据、`max_tokens=4` 的最小请求中 35 秒仍超时；3 条 no-answer 在 60 秒后仍出现超时，不能作为稳定基线 provider。
2. 同一接口下，`qwen/qwen3.5-122b-a10b` 的 4-token 最小请求约 2.4 秒成功。
3. Qwen 3.5 的 multi-hop 请求存在明显长尾；`60s` 会误伤复杂题，`120s` 可覆盖混合 smoke。
4. 后端不自动重试，避免业务请求重复；baseline runner 仅允许一次显式重试并完整记账，用于吸收 provider 的偶发 5xx/timeout。

所有 API key 只来自本地环境变量；最终报告与仓库文件未发现明文凭据。

## 验收结果

### 代码与脚本测试

- `mvn -q test`：39 个 test suites，155 tests，0 failures，0 errors，0 skipped。
- `python -B -m unittest discover -s scripts -p 'test_*.py'`：25 tests，全部通过。
- `git diff --check main`：通过；历史生成报告中的行尾空格已做纯机械清理，没有改变报告内容。

### 3 条 no-answer

报告：

- `docs/eval/reports/stage1-qwen35-no-answer.md`
- `docs/eval/reports/stage1-qwen35-no-answer-details.json`
- `docs/eval/reports/stage1-qwen35-no-answer-metadata.json`

结果：`CLEAN`，3/3 ask 成功，0 retry，no-answer accuracy `100%`，citation violation `0`。三条响应均满足：

- `citations=[]`
- `citationFallbackUsed=false`
- `metadata.status=no_result`

### 混合 smoke

样本覆盖 fact、definition、reasoning、multi-hop、no-answer 各 1 条。

报告：

- `docs/eval/reports/stage1-qwen35-mixed-smoke.md`
- `docs/eval/reports/stage1-qwen35-mixed-smoke-details.json`
- `docs/eval/reports/stage1-qwen35-mixed-smoke-metadata.json`

结果：`CLEAN`，5/5 ask 成功，0 retry；Recall@5 `87.50%`、MRR `1.0000`、Top1 `100%`、citation hit `100%`、snippet hit `100%`、no-answer accuracy `100%`、citation violation `0`。正常回答未被拒答规则误伤。

### 两轮完整 30 条 objective baseline

固定条件：KB id `11`、collection `kb_2addbb37622c42cb`、30 条样本、topK `5`、minScore `0.3`、heuristic reranker、judge `off`。

| 指标 | Run 1 | Run 2 |
|---|---:|---:|
| Report status | CLEAN | CLEAN |
| ask / retrieve errors | 0 / 0 | 0 / 0 |
| rate limit errors | 0 | 0 |
| retry count | 2 | 2 |
| Recall@5 | 68.63% | 68.63% |
| MRR | 0.7346 | 0.7346 |
| Top1 source accuracy | 96.30% | 96.30% |
| Answer keyword hit | 72.12% | 72.12% |
| Citation source hit | 83.33% | 86.67% |
| Citation snippet hit | 100% | 100% |
| Unsupported citations | 0 | 0 |
| No-answer accuracy | 100% | 100% |
| No-answer citation violations | 0 | 0 |
| LLM judge | skipped | skipped |

报告：

- `docs/eval/reports/stage1-qwen35-objective-run1.md`
- `docs/eval/reports/stage1-qwen35-objective-details-run1.json`
- `docs/eval/reports/stage1-qwen35-objective-metadata-run1.json`
- `docs/eval/reports/stage1-qwen35-objective-run2.md`
- `docs/eval/reports/stage1-qwen35-objective-details-run2.json`
- `docs/eval/reports/stage1-qwen35-objective-metadata-run2.json`

两轮 retrieval 指标完全一致；citation hit 存在 `3.34` 个百分点的生成随机波动。Run 1 的 `fact-001/fact-003`、Run 2 的 `definition-006/reasoning-003` 各发生一次 provider timeout/5xx 后重试成功。报告仍为 CLEAN，但说明 provider 长尾与偶发 5xx 尚未消失。

## 指标解释边界

- `citation snippet hit=100%` 只证明引用片段能回连到本轮 retrieved contexts，不证明它在语义上支持答案每个 claim。
- `answer keyword hit` 是字符串覆盖率，不等价于完整答案正确率。
- 本轮 judge 为 `off`，因此不能宣称已完成独立 faithfulness/relevance 裁判。
- 30 条开发集和 3 份教学 fixture 只适合回归与单变量对比，不是生产级 benchmark。
- Reranker 默认仍是 heuristic；本阶段没有验证真实 model reranker 的收益。

## 阶段边界与下一步

- Stage 1 已完成，可以进入合并收口。
- Stage 2 触发条件当前不满足：没有配置真实 rerank provider/凭据，默认继续使用 heuristic；后续应正式记录为“跳过”或在取得 provider 后单独 A/B。
- Stage 3 的标题感知、长代码块/长段落分块专项尚未开始。
- Stage 4 文档真相源清理已于 2026-07-12 完成；当前索引为 `docs/optimization/README.md`。
- claim-level citation support、LLM judge、生产数据扩集与 provider 延迟/成本统计留待后续阶段。
