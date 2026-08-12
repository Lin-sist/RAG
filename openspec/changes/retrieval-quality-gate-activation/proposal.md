# Proposal: C17 Retrieval Quality Gate Activation

## Summary

把首个 `rag-eval-dev-v2` retrieval-only profile 从 `DRAFT / PENDING_REFERENCE_EVIDENCE` 推进到有完整、可追溯 reference evidence、经用户审阅的具体阈值和 `ACTIVE / APPROVED` 状态。

本 change 不通过“直接填数”激活门禁。它先补齐 C10 尚未交付的中间闭环：固定 C17 reference manifest，严格校验三次 full v2/150 retrieval details，生成只含身份、完整性和聚合指标的脱敏 evidence pack；完整 evidence 形成后，再由用户单独批准 hard floors 与 regression tolerances，最后生成绑定 ACTIVE profile hash 的锁定 reference summary。

## 用户故事：改前坏事 → 改后不同

- **改前坏事**：C10 已有离线 evaluator，但首个 profile 的 12 个 target 全为空，只能返回 `NOT_EVALUABLE`；三次 retrieval report 需要人工拼接，缺失样本、Git/config/KB 漂移、provider fallback 或成功子集筛选可能被遗漏，因此不能安全地把历史数字写成发布门禁。
- **改后不同**：固定 manifest 和 compiler 会在任何阈值决策前验证三次 reference 的身份、150×3 observation 完整性、错误/重试、heuristic attribution 与调用预算；用户看到每条规则的三次 observed 值及 min/median/max/spread 后再批准数值，ACTIVE profile 与 reference summary 绑定同一 profile/dataset/run/metric identity，后续 candidate 才可能得到可信 `PASS/FAIL`。

## Why Now

- C8a/C8b 已冻结 `rag-eval-dev-v2` 150 条 release、三份 fixture、配额、grounding 与 review identity。
- C9a/C9b 已把 retrieval、objective、judge 与 global status 分离；C17 可以保持 retrieval-only，不借 judge/generation 结果填补缺口。
- C10 已验收 `rag-quality-gate-profile-v1`、固定 slices、hard/reference AND、fail-closed completeness 与稳定退出码，但 reference calls 未授权，profile 仍为 DRAFT。
- `run_reproducible_rag_eval.py` 已支持既有 KB、repeat/run-index、独立 report/details/metadata、plan-only/preflight 与 no-overwrite；当前缺口是 C17 专用执行身份、全量调用预算、三次 details 的严格合并和 human approval 到 reference lock 的桥接。
- 蓝图 v6 把 C17 置于 W1 首位；后续 C18 generation evidence、C19 judge calibration 与 C20 profiles 都依赖先证明这一套 evidence-to-policy 流程可审计。

## Readiness And Capability Classification

- `confirmed`：启动时 HEAD=`701ade3`，工作树干净，`main...origin/main [ahead 1]`；`.ai/ACTIVE_TASK.md=IDLE`；无其他 active change；C1–C16 均已在批准范围内归档。
- `confirmed`：accepted `evaluation` spec 已要求只有 ACTIVE profile、VALID dataset 和兼容完整 evidence 才能 PASS；DRAFT/identity-incomplete evidence 不得被追认。
- `confirmed`：tracked config 默认 embedding 使用 OpenAI-compatible adapter、`nvidia/llama-nemotron-embed-1b-v2`、60s timeout、fallback disabled；rerank 默认 heuristic。环境变量可覆盖这些值，因此它们不是尚未核实的 runtime identity。
- `confirmed`：当前 profile 固定 full v2/150、retrieval-only、`topK=5`、`minScore=0.3`、rerank enabled、12 条 required rules，状态仍为 `DRAFT / PENDING_REFERENCE_EVIDENCE` 且 target 全为空。
- `partial`：runner 能产生三次独立 details/metadata，evaluator 能消费一个锁定 reference；但非 C7 模式的 plan 尚未完整展示 query embedding 上限，也没有 C17 reference manifest、三次 evidence compiler、阈值审阅包或 active-profile reference lock。
- `partial`：W0 的 OTel unavailable-collector 聚焦测试本轮 9/9 通过，但当前债务仍明确禁止用独立复跑把全仓状态改写为 GREEN；C17 live canary/full 前必须有单独 W0 closeout 事实。
- `planned`：C17 reference manifest/schema、调用预算 fail-fast、三次 strict-identity compiler、脱敏 evidence pack、人工阈值闸门、ACTIVE profile 与 median reference lock。
- `out_of_scope`：修改 dataset/fixture、retrieval/rerank/embedding 公式或生产默认；切换默认 reranker；generation/citation/no-answer answer quality、judge calibration/profile；CI 平台配置；生产 SLA；KB 重建或文档重新索引。
- `unknown`：实际 runtime embedding provider/model/endpoint/account、费用或零费用依据、限流/配额、现有固定 KB readiness、全仓 W0 closeout 状态、正式 reference 指标和波动；这些事实不得由 tracked default 或旧报告替代。

## Goals

1. 定义版本化 `c17-retrieval-reference-v1` manifest，固定 dataset/profile target、full 150 selection、3 measured repeats、retrieval config、expected heuristic attribution、error/retry policy、调用上限和安全输出边界。
2. 让 plan-only 在 backend/provider 调用前精确展示 canary/full 的 debug retrieval、query embedding、external rerank、ask、generation 与 judge 上限。
3. 提供纯本地 reference compiler，严格校验 3×150 observation、repeat/sample identity、Git/config/fixture/KB/provider attribution、status/error/retry 与 details hash，禁止成功子集聚合。
4. 生成脱敏 evidence pack：保留每条 gate rule 的三次 observed、denominator、min/median/max/spread 和完整性状态，但不复制 question、expected answer/context、retrieved context、provider body、secret 或绝对路径。
5. 把 exact threshold/tolerance 决策留给完整 evidence 后的用户中间闸门；工具不得自动学习、自动写入或自动批准数值。
6. 用户批准后把 canonical profile 从 `v1-draft` 提升为 `v1 / ACTIVE / APPROVED`，并生成绑定最终 profile SHA-256 的 locked reference summary；未来 profile 数值变化必须新建 version。
7. 验证三个 reference repeat 在最终 ACTIVE profile 和 locked reference 下均可离线重放，且 required rules 不出现 `NOT_EVALUABLE/INVALID`。
8. 保持生产 QA、默认 embedding/rerank、dataset、指标公式与业务 API 不变。

## Non-Goals

- 不在本次规划或未获授权的 offline implementation 中调用 backend、embedding、rerank、ask、generation、judge 或其他 provider。
- 不用 C7 的 30 条 A/B、C10 synthetic fixture、单次 v2 结果或任何旧 report 追认 C17 reference。
- 不把 canary 当质量 evidence；canary 只发现身份、认证、KB readiness、provider 可达性或输出契约错误。
- 不在 `--preflight-only` 失败时隐式创建 KB、上传 fixture 或触发文档 embedding；这些 mutation/calls 需要单独预算和授权。
- 不从 reference 自动拟合最容易 PASS 的阈值，不为门禁修改 question、annotation、expected context、chunking、retrieval、rerank 或 metric formula。
- 不把 retrieval gate 描述为 generation、citation、no-answer answer quality、judge、生产多租户、Router 或 Agentic RAG 已达标。
- 不新增/升级依赖，不修改 Java、API/DTO、数据库、frontend、runtime secrets 或生产默认配置。

## Proposed Scope

### 1. C17 Reference Execution Contract

- 新增 tracked manifest/schema，固定 `rag-eval-dev-v2` manifest hash、profile id、150 条完整 selection、`repeat=3`、`topK=5`、`minScore=0.3`、rerank enabled、expected requested/effective provider=`heuristic`、fallback/model rerank calls=0、retry=0。
- runner 在 plan-only 阶段验证 manifest 与参数，并总是报告 query embedding upper bound；正式 full 为 450 次 debug retrieval、最多 450 次 query embedding，external rerank/ask/generation/judge=0。
- preflight 必须使用 `--keep-existing`，只验证登录、既有固定 KB、三份 fixture、文档状态与 identity，不创建/上传/删除资源，也不运行 retrieval。

### 2. Canary And Full Evidence Gates

- 建议 canary 固定 5 条、每类 1 条：`fact-001`、`definition-001`、`reasoning-001`、`multi-hop-001`、`no-answer-001`，1 repeat。
- canary 上限为 5 次 debug retrieval、最多 5 次 query embedding；它不进入 reference aggregate。任何 auth/429/timeout/retrieve error、identity/provider drift、fallback/model rerank call 均停止，不自动重试或删样本。
- full 只有在 canary 的执行契约完整且用户再次确认预算后才运行。full 固定 150×3，全部 observation 保留；missing/unexpected/error/retry/fallback/model-call 任一非零均阻止 evidence COMPLETE。

### 3. Reference Compiler And Evidence Pack

- compiler 读取三个 local details/metadata，不访问 backend/provider；按 `runIndex + sampleId` 校验 exact 450 observations、顺序与 hashes。
- strict repeat identity 包含 dataset/selection、fixture/document/KB、tracked config snapshot、Git HEAD、retrieval config、repeat total/index、metric contract 与 observed rerank attribution。
- evidence status 区分 `COMPLETE / INCOMPLETE / NOT_COMPARABLE / INVALID`；任何失败都保留 expected counts 和 safe reason，不对成功子集计算 reference。
- 对每条 profile rule 输出三次 observed 与 `min/median/max/spread`。median 只作为后续 locked reference 的 central observed；hard floor 与 tolerance 仍为空，直到用户批准。

### 4. Threshold Review And Profile Activation

- 完整 evidence 后生成一次性审阅包，逐条展示 12 条 rules 的 denominator、三次 observed、min/median/max/spread、候选风险与适用边界。
- 用户逐项或整体批准 exact hard floor 与 `maxAbsoluteRegression`；未批准前 profile 保持 DRAFT，reference pack 不可供 candidate gate 产生 PASS。
- 激活时将 canonical profile 的 `profileVersion` 从 `v1-draft` 升到 `v1`，状态改为 `ACTIVE / APPROVED`，填入用户批准值并重新计算 profile hash。
- 用最终 ACTIVE profile 重新验证原始三次 details，生成 `reference.rules[].observed=median` 的 locked reference summary；三个 reference repeat 都必须通过 hard floor 与批准 tolerance，才允许进入 acceptance。

### 5. Documentation And Evidence Boundary

- raw details/metadata/report 写入 `tmp/eval/c17/`，必须 `--no-overwrite`，不进入 tracked files。
- tracked reference 只保存 allowlisted identity、hash、counts、aggregates、rule values、safe status/reason 与批准记录，不保存凭据、绝对路径、numeric KB id/vector collection、question/answer/context 或 provider raw response。
- 更新 eval guide、OpenSpec tasks 和 AGENT_LOG；accepted baseline、archive 与 `ACTIVE_TASK=IDLE` 只在用户最终验收后处理。

## External Call And Data Boundary

| 阶段 | Debug retrieval | Query embedding upper bound | External rerank | Ask / generation / judge | 数据出站与授权 |
|---|---:|---:|---:|---:|---|
| planning / offline implementation / compiler tests | 0 | 0 | 0 | 0 | 无业务数据出站；当前仅授权此阶段 |
| plan-only | 0 | 0 | 0 | 0 | 纯本地 manifest/命令形状检查 |
| preflight-only + keep-existing | 0 | 0 | 0 | 0 | 只访问用户本机 backend；不创建/上传/删除 KB |
| fixed 5-case canary | 5 | 5 | 0 | 0 | 5 条 tracked question 可能发送到实际 embedding provider；需单独授权 |
| full reference 150×3 | 450 | 450 | 0 | 0 | 150 条 tracked question 重复 3 次可能发送到实际 embedding provider；需 canary 后再次授权 |
| KB rebuild / fixture upload | 0（本 change 预算） | 0（本 change 预算） | 0 | 0 | 明确不授权；若 preflight 不 READY，停止并另立 mutation/indexing 预算 |

tracked config 的默认 embedding 指向 NVIDIA OpenAI-compatible endpoint/model，但 runtime 可被环境覆盖。canary 前必须以不含 secret 的 runtime fingerprint 明确 provider、model、endpoint host/path、timeout、fallback、账户费用或零费用依据、限流/配额与 retry=0；不能凭默认配置或历史账号状态推断。

## Risks And Mitigations

- **把当前实现固化成过低阈值**：只输出分布，不自动写数；hard floor/tolerance 由用户在完整 evidence 后审阅。
- **一次幸运结果掩盖波动**：固定三次 full repeats，保留每次值与 spread；locked reference 使用 median，同时要求三次 reference 都通过最终 profile。
- **KB 或配置漂移制造伪基线**：strict repeat identity 任一漂移即 `NOT_COMPARABLE`，不从成功子集继续。
- **provider fallback 被误当 heuristic baseline**：450 observations 必须 requested/effective heuristic、fallback=0、model rerank calls=0。
- **预检失败触发隐式建库/索引费用**：正式路径强制 `--keep-existing`；缺 KB/fixture 时 fail closed，mutation 另行授权。
- **raw evidence 泄露问题或上下文**：raw artifacts 只在 ignored tmp；tracked summary 使用 allowlist 和敏感信息扫描。
- **W0 非 GREEN 污染阶段结论**：live canary/full 前要求独立 W0 closeout；聚焦 9/9 不能代替全仓稳定性事实。
- **profile 激活后静默改数**：`v1-draft -> v1` 显式 version transition；ACTIVE profile/reference 被接受后任何数值变化必须新 version。

## Approval Gates

1. **规划审阅**：用户审阅本 proposal、design 决策、tasks 与 4 requirements / 12 scenarios delta。
2. **Offline implementation 授权**：只允许 Python/schema/profile shape/docs/TDD，provider calls=0、business data outbound=false。
3. **W0 + runtime readiness**：独立确认 W0 closeout、plan-only、preflight、runtime fingerprint 与已有 KB readiness。
4. **Canary 外调授权**：最多 5 retrieval + 5 query embedding；失败即停。
5. **Full reference 外调授权**：canary 后最多 450 retrieval + 450 query embedding；无自动 retry。
6. **阈值中间闸门**：完整 evidence 后用户批准 12 条 hard floors/tolerances；此前不改 ACTIVE。
7. **最终验收与收口**：用户确认 ACTIVE profile/reference/验证边界后，才允许 baseline acceptance、archive 和 `ACTIVE_TASK=IDLE`。

## Anticipated Files

- `openspec/changes/retrieval-quality-gate-activation/**`
- `.ai/ACTIVE_TASK.md`
- `.ai/AGENT_LOG.md`
- `docs/eval/schema/c17-retrieval-reference-v1.json`
- `docs/eval/config/c17-retrieval-reference-v1.json`
- `docs/eval/gates/rag-eval-dev-v2-retrieval-regression-v1.json`
- `docs/eval/references/` 下的脱敏 reference artifacts（仅 evidence/threshold 闸门通过后）
- `scripts/run_reproducible_rag_eval.py`
- `scripts/compile_retrieval_reference.py`
- 对应 Python tests 与 `docs/eval/RAG_EVAL_GUIDE.md`

具体文件可在实现时按 TDD 最小化，但不得越过本 change 的 retrieval-only 边界。

## Commit Responsibility

- 提交责任：`用户手动提交`。
- Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- 规划建议提交信息：`docs(openspec): 启动C17检索质量门禁规划`。
