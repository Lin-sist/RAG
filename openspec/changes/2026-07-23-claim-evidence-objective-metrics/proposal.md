# Proposal: C9a Claim Evidence Objective Metrics

## Why

当前评测已能分别报告 answer keyword、citation source、citation snippet、unsupported citation 与 no-answer 指标，但这些指标回答的是“答案是否包含预期词”和“引用片段是否来自本轮 returned contexts”，不能回答“生成答案中的每个可审计 claim 是否能与已验证证据客观对齐”。

C8a/C8b 已完成正式 dataset release、schema、fixture、标注、review 与冻结治理，C9a 可以在不改评测数据身份的前提下，为 generation/citation 通道补充确定性的 claim-level 客观指标。该指标必须保持可复现、零 judge 依赖，并明确只是词法/结构对齐证据，不得冒充语义蕴含、完整事实正确性或独立 faithfulness judge。

## Readiness Gate

- `git status --short --branch` 为干净 `main...origin/main`，HEAD=`1577aab`。
- `.ai/ACTIVE_TASK.md` 在启动前为 `IDLE`，没有未归档 change。
- C8b `2026-07-22-eval-dataset-expansion-and-annotation` 已验收归档；默认 release 为已验证的 150 条 `rag-eval-dev-v2`，v1 仍可显式验证。
- C8b closeout 明确记录 C9a 数据治理前置已满足，剩余待决项正是 claim 单位、evidence 对齐、分母与状态语义。
- 提交责任：`用户手动提交`。Agent 不暂存、不提交、不 push、不创建 PR、不部署。

## User Story

改之前，一份报告即使 citation snippet 全部能回连 retrieved contexts，也可能仍有答案句子没有任何引用证据支持，报告无法给出 claim 级缺口。

改之后，评测 runner 会把成功的 answerable 答案确定性拆成可审计 claim，只使用通过既有 citation-context provenance 校验的引用证据做确定性词法对齐，并分别输出 supported、unsupported、unevaluable/partial 事实；用户可以定位“哪些 claim 没有客观证据对齐”，同时不会把该结果误称为语义 faithfulness。

## Capability Classification

| 分类 | 当前事实 |
|---|---|
| `confirmed` | direct/reproducible runner、正式 v1/v2 dataset validation、successful ask generation 指标、citation source/snippet provenance、unsupported citation、no-answer、可选 LLM judge 与 CLEAN/PARTIAL/RETRIEVAL_ONLY/FAILED 已存在 |
| `partial` | 答案与 returned citations/contexts 已进入 per-sample details，但没有 claim extraction、claim-to-evidence attribution、claim support denominator 或独立 claim metric status |
| `planned` | 确定性 claim 拆分、eligible evidence 构建、exact/token lexical alignment、逐 claim attribution、聚合 support rate 与 claim metric status |
| `out_of_scope` | judge 校准和 objective/judge 全局状态拆分（C9b）、质量阈值/退出码（C10）、prompt/生成重答、生产 citation 行为、SSE、默认 reranker、数据集改题或重标注 |
| `unknown` | 初始 `0.70` claim-token coverage 阈值在未来真实 150 条 generation evidence 上的分布；规划阶段只锁定可解释算法与审阅闸门，不宣称阈值已经完成经验校准 |

## Scope

### 1. Deterministic claim extraction

- 在 Python eval runner 中增加纯本地、确定性的 answer claim splitter。
- 先按段落和列表项切分，再按中英文句末标点切分；去除空白、纯序号、纯标题和孤立 citation marker，不改写剩余文本。
- 只对 `should_answer=true` 且 ask 成功、答案非空的样本计算；no-answer 继续使用既有独立指标，不进入 claim support 分母。
- 为每个 claim 记录稳定 `claimIndex`、局部 `claimHash`、字符数和安全的 per-sample attribution；不得仅输出聚合率而丢失审计入口。

### 2. Eligible evidence and objective alignment

- evidence 只能来自本轮 returned citations，且该 citation 必须先通过既有 citation identity + snippet-to-returned-context provenance 校验。
- 对 claim 与每条 eligible citation snippet 做两级确定性匹配：normalized exact containment；否则按既有 ASCII token/CJK bigram tokenizer 计算 claim-token coverage。
- 初始 lexical support 阈值设为 `0.70`，且 token 路径至少需要 2 个可比较 token；阈值与算法进入 tracked report metadata，禁止静默漂移。
- 输出 `exact`、`token_overlap`、`unsupported`；没有 eligible evidence 的 claim 必须计为 `unsupported`，不能从分母删除。
- 指标命名必须包含 `objective` 或 `lexical`，不得使用 `entailment`、`grounded truth` 或 `faithfulness` 作为客观结论名。

### 3. Per-sample and aggregate metrics

- per-sample details 增加 claims、best evidence attribution、support method、coverage 与未支持原因。
- aggregate 增加 claim total、supported count、unsupported count、exact/token counts、objective claim support rate、answerable sample coverage 与 claim metric status。
- claim support rate 分母为所有成功抽取的 answerable claims；ask 失败、空答案或 extractor 异常必须通过 count/status 显式呈现，不能静默剔除后仍称完整。
- claim metric status 使用 `COMPLETE / PARTIAL / SKIPPED / NOT_APPLICABLE`，只描述 claim 客观通道；本 change 不修改全局 `Report status` 和 judge status 语义。

### 4. Compatibility and documentation

- direct runner 负责唯一计算实现；reproducible runner 继续通过 child command 复用，不复制算法。
- 旧 details/report 缺少 C9a 字段时仍可读取；新增字段保持 additive，不覆盖历史报告。
- 更新 `docs/eval/RAG_EVAL_GUIDE.md`，明确 claim 指标与 keyword、citation provenance、judge 的边界。
- 用合成中英文/列表答案和 citation/context fixture 建立确定性单元测试，不通过真实 provider 反复试阈值。

## Non-goals

- 不调用 LLM 拆 claim，不做 NLI/embedding/语义相似度，不把 lexical alignment 表述为事实正确或蕴含。
- 不校准或改写现有 judge prompt、score、pass threshold、错误处理与全局状态；这些属于 C9b。
- 不定义 pass/fail threshold、profile gate、类别门禁或非零质量退出码；这些属于 C10。
- 不修改 `rag-eval-dev-v1/v2` question、annotation、review、fixture、manifest 或默认 release。
- 不修改 Java API、DTO、citation validation、prompt、LLM provider、retrieval、chunking、rerank、no-answer、数据库、前端或 SSE。
- 不运行新的 generation baseline，不根据 observed provider 输出修改算法、阈值或评测数据。

## External Call Gate

本规划与离线实现阶段的真实业务外调预算均为 0：

- embedding：0；
- rerank model：0；
- ask / generation：0；
- judge：0；
- 其他 LLM/provider：0；
- 数据出站：0。

若后续在 150 条 v2 上执行一次完整 C9a generation evidence run，保守上限预计为 150 次 debug retrieval、150 次 ask、至多 300 次 query embedding、至多 150 次 generation，judge=0；若保持默认 heuristic，则外部 rerank model=0。出站可能包含 150 条 tracked 开发问题以及传给 generation provider 的 retrieved fixture contexts。该运行不在本 proposal 的当前授权内，必须在执行前再次披露实际 provider/模型、凭据来源、timeout/retry、数据出站、费用或零费用依据、限流风险与 raw evidence 处置，并取得用户授权。

## Acceptance Criteria

### Planning gate

- proposal、design、tasks 与 `evaluation` spec delta 齐全且互相一致。
- design 明确 claim 单位、evidence eligibility、匹配算法、阈值、分母、状态、隐私与 C9b/C10 边界。
- 用户批准规划和实现授权前，不修改 runner、tests、guide 或 baseline spec。

### Implementation gate

- RED tests 先覆盖中英文句子、列表、纯标题/marker、无 citation、invalid citation、exact、token overlap、短 claim、ask failure、no-answer 与 partial aggregation。
- direct/reproducible runner 共享唯一 C9a 实现；历史 report/details 保持可读。
- claim 算法版本、threshold、tokenizer 与状态写入 details/report metadata，结果可复现。
- Python 全量测试、dataset v1/v2 validation、direct/reproducible plan-only、SensitiveLogs、受保护路径、secret/绝对路径扫描与 `git diff --check` 通过。
- Java/POM/前端无改动时，Maven、frontend build 与 Docker/live provider 明确记为 `SKIPPED`。
- 未获独立授权前，真实 embedding/rerank/ask/judge/LLM/provider 调用量和数据出站保持 0。

### Acceptance and closeout gate

- 用户验收 claim contract、per-sample evidence、aggregate/status、兼容性和结论边界。
- 已批准 delta 原文接受进 `openspec/specs/evaluation/spec.md`。
- change 归档并恢复 `.ai/ACTIVE_TASK.md=IDLE`。
- C9a 验收只表示 claim-level objective lexical alignment 能稳定计算，不表示 judge 已校准、faithfulness 已证明或 C10 quality gate 已完成。

## Risks

- 确定性句法切分不能保证语义原子性；通过保留原文本、稳定 claimIndex/hash、逐 claim attribution 与明确“客观词法指标”命名控制误解。
- lexical overlap 可能对同义改写产生 false negative，也可能因共享术语产生 false positive；C9a 不把它升级为 entailment，C9b 才负责校准独立 judge。
- `0.70` 初始阈值尚无 150 条真实 generation 分布支撑；实现先锁定算法与测试，正式 evidence run 后如需调整，必须新建可比较的版本/变更，不能原地改阈值覆盖旧结论。
- raw answer/claim/evidence 可包含用户内容；默认 evidence 输出沿用现有本地 report/details 边界，不进入普通日志，不新增 tracked raw report。
- 如果把 ask 失败样本静默排除，support rate 会虚高；通过 `claimMetricStatus=PARTIAL` 与失败计数阻止完整性误报。

## Implementation Outcome（待验收）

- `scripts/run_rag_eval.py` 已新增固定 `claim-lexical-v1`：确定性 sentence/list splitter、claim hash/index、provenance-valid citation evidence、exact / `0.70` claim-token coverage、stable best-evidence tie-break 与稳定 unsupported reason。
- per-sample details 已包含 raw claim attribution；aggregate、Markdown、details JSON 与 console 已包含局部 claim status、全 claim 分母、support/unsupported/exact/token counts、complete sample coverage 与算法 identity。摘要与普通输出不复制 raw claim/snippet。
- `scripts/run_reproducible_rag_eval.py` 直接复用 direct runner 的 `CLAIM_METRIC_CONFIG`，并将同一 identity 写入 plan 与 run metadata；direct runner 对 metadata identity drift 以 `claim_metric_identity_mismatch` 在 backend/provider 调用前失败。
- direct/reproducible runner 的原 retrieval、keyword、citation、no-answer、judge 公式与全局 Report status 保持不变；dataset v1/v2 identity 和历史报告未修改。
- TDD 聚焦行为覆盖中英文/列表拆分、结构 marker、invalid citation、exact/token threshold、短 claim、stable tie-break、no-answer/retrieval-only/partial、历史结果 unavailable、per-sample/aggregate/report 与 metadata drift。Python 全量为 `114 tests / OK`。
- v1 direct、v2 direct 与 v2 reproducible plan-only 均返回 `VALID`，并显示 `claim-lexical-v1` / `0.70`；实际 backend/provider 调用、数据出站与费用均为 0。真实 150 条 generation evidence 未授权、未执行。

## Rollback

- C9a 仅计划 additive Python report/details 字段和文档；回滚可移除 claim 计算并继续生成原有指标。
- 不涉及数据库迁移、Java API、生产配置或 dataset release，rollback 不需要数据恢复。
- 历史报告不回写；如算法版本后续变化，旧 evidence 继续按原 metadata 解释。
