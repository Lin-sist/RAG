# Design: C7 Reranker A/B Evaluation

## Context

C6 已把 rerank outcome 通过 `RetrievalResult.diagnostics` 暴露到 debug retrieval、同步 QA 与 Python runner。当前 runner 对每个样本能得到：

- requested/effective provider；
- fallback count/reason；
- model call count；
- candidate/scored count 与 coverage；
- rerank stage latency；
- model 与 protocol。

当前缺口位于 evaluation orchestration 与 validity，而不是 provider adapter：

- `run_rag_eval.py` 只聚合 provider coverage，没有 latency percentile；
- `run_reproducible_rag_eval.py` 记录 KB/fixture/config/Git HEAD，但没有 eval-set hash、arm manifest 或 A/B identity diff；
- provider 是 backend 进程级配置，debug request 只有 `enableRerank`，没有也不应为 C7 新增任意 provider override；
- 旧报告可以单独成立，但不能自动证明两个 arm 可比较。

## Design Principles

1. 先证明身份一致，再比较指标。
2. 先证明 effective provider 覆盖，再陈述 model 收益。
3. retrieval、generation、citation、no-answer 与 judge 指标边界不混用。
4. A/B orchestration 不进入运行时 API；backend lifecycle 仍由明确的本地启动流程管理。
5. comparison 是纯离线步骤，不接触 backend、provider 或 secret。
6. 任何 invalid/partial evidence 都保留诊断，但不被清洗成“干净子集”。

## Proposed Flow

```text
approved planning artifacts
  -> offline runner/comparator implementation + tests
  -> user reviews implementation evidence
  -> separate live-call disclosure and authorization
  -> fixed-KB mutation-free preflight
  -> heuristic canary/full arm
  -> restart backend with approved model configuration
  -> model canary/full arm
  -> offline identity + coverage + pairing validation
  -> COMPARABLE ? quality/latency deltas : reasons-only diagnosis
```

Runner 不负责创建动态 provider API，也不把 backend restart 包进 Python。每个 arm 使用明确的启动配置与同一 tracked HEAD，输出 metadata/details；comparator 只消费这些文件。

## Components

### 1. Per-sample measurement

`run_rag_eval.py` 在调用 `/api/qa/debug/retrieve` 前后使用 monotonic clock 测量 `retrieveLatencyMillis`。该值表示客户端观察到的完整 debug retrieval wall time，包含 HTTP、embedding、hybrid retrieval、rerank 与序列化；不能与 server-side `rerankLatencyMillis` 混为同一指标。

每条 compact sample evidence 至少包含：

```text
runIndex
sampleId
retrieveOk
recall5 hits/total
firstMatchRank
top1SourceHit
requestedProvider
effectiveProvider
fallbackCount/reason
modelCallCount
candidateCount/scoredCount/coverage
rerankLatencyMillis
retrieveLatencyMillis
model
protocol
```

现有 raw details 仍可用于本地诊断，但 C7 的 tracked compact evidence 不复制 question、contexts、passages、raw provider response 或异常 message。

### 2. Arm manifest

每个 C7 arm 在执行前提供不含 secret 的 JSON manifest。建议字段：

```text
schemaVersion = c7-reranker-ab-v1
armId = heuristic | model
expectedRequestedProvider
expectedEffectiveProvider
model
protocol
topN
topK
truncate
timeoutMillis
healthCheckEnabled
endpointPath
measuredRepeats
warmupCalls
```

manifest 不包含 API key、Authorization、完整含 userinfo/query 的 base URL、密码或环境变量全集。metadata 保存 manifest 内容的安全白名单和 SHA-256。observed sample attribution 必须与 manifest 的 provider/model/protocol 一致；不一致时 comparison 为 `NOT_COMPARABLE`。

### 3. Strict identity

以下字段必须在两个 arm 完全一致：

- Git HEAD；
- eval-set path + SHA-256；
- selected sample IDs、顺序与数量；
- fixture path/name/SHA-256/bytes；
- KB name、marker、vector collection、document count、chunk count；
- 每份 KB document 的 title/status/chunkCount/contentHash；
- topK、minScore、enableRerank；
- tracked config snapshot hashes；
- measured repeats、warm-up policy；
- runner/comparator schema version。

允许差异只限：

- armId；
- expected/observed requested/effective rerank provider；
- provider-specific model、protocol、endpoint path、truncate、timeout、health；
- 运行时间戳与实际 latency；
- 因 rerank 顺序变化而产生的 retrieval metric/rank 结果。

base URL、KB 数字 ID 等部署实例字段继续记录，但是否纳入 strict identity 由同一 KB 的稳定业务身份决定。当前 C7 建议要求相同 KB ID 与 vector collection，避免误连到同名副本；跨环境可移植比较留给后续 change。

### 4. Provider coverage validity

定义 `rerankEligible = candidateCount > 0`。

heuristic arm 对每个 eligible observation 必须满足：

- requestedProvider=`heuristic`；
- effectiveProvider=`heuristic`；
- fallbackCount=0；
- modelCallCount=0。

model arm 对每个 eligible observation 必须满足：

- requestedProvider 等于批准的 model provider；
- effectiveProvider 等于批准的 model provider；
- fallbackCount=0；
- modelCallCount=1；
- candidateCoverage=1.0；
- model/protocol 与 manifest 一致。

model coverage 的分母是所有 eligible observations，干净比较要求 100%。任何 eligible observation fallback、partial coverage、0 model calls 或 provider mismatch 都令 comparison 为 `NOT_COMPARABLE`。

`candidateCount=0` 的样本标为 `notApplicable`，不计入 model coverage 分母；但两个 arm 必须在同一 `runIndex + sampleId` 都为 zero-candidate，否则说明 upstream retrieval 发生漂移，comparison 仍不可比较。

### 5. Report and comparison status

保留既有 per-run `Report status`：C7 每个 arm 应为 `RETRIEVAL_ONLY`。不把 `RETRIEVAL_ONLY` 改名为 `CLEAN`，也不把 comparison validity 塞进 generation status。

新增独立字段：

```text
comparisonStatus = COMPARABLE | NOT_COMPARABLE | FAILED
comparisonReasons = [stable reason codes]
```

建议稳定 reason：

- `identity_mismatch`
- `run_count_mismatch`
- `sample_pair_mismatch`
- `arm_report_not_retrieval_only`
- `retrieve_error`
- `heuristic_arm_contaminated`
- `model_provider_mismatch`
- `model_fallback_observed`
- `model_coverage_incomplete`
- `candidate_coverage_incomplete`
- `zero_candidate_mismatch`
- `manifest_observation_mismatch`
- `invalid_evidence_schema`

`FAILED` 仅用于文件不可读、schema 破损或无法完成 comparison；证据完整但不满足可比条件时使用 `NOT_COMPARABLE`。

### 6. Metrics and percentile semantics

质量指标保持现有公式：

- Recall@5；
- MRR；
- Top1 source accuracy。

C7 不修改 Recall@3、generation、citation、no-answer 或 judge 公式；可以继续显示 Recall@3，但它不作为 C7 主比较指标。

延迟分为：

- `rerankLatencyMillis`：server-side rerank stage；主 provider latency 口径；
- `retrieveLatencyMillis`：client-observed debug retrieval wall time；辅助端到端口径。

P50/P95 使用 nearest-rank：对 `n` 个非负有限 observations 升序排列，rank=`ceil(p*n)`，使用 1-based rank。每个 arm 分别报告 observation count、min、P50、P95、max；不得用总运行时间除以样本数冒充 percentile。

measured repeat 的每个 `runIndex + sampleId` 都保留。comparison 同时输出：

- 每个 repeat 的 arm metrics；
- 全部 measured observations 的 latency 分布；
- 每个 sample 跨 repeats 的 median latency，以及 model - heuristic 的 paired delta。

warm-up observations 只用于连接/JIT 预热，单独计数并从质量与 latency summary 排除。

### 7. Run ordering and cache boundary

建议正式 latency evidence 使用 3 个 measured repeats，并在两 arm 间交替顺序（A/B、B/A、A/B），每次 arm 启动后使用 3 个固定 warm-up samples。该方案降低固定“后跑 arm”受网络、进程和 cache 顺序影响的偏差，但会增加外调预算，待用户确认。

由于 query embedding cache、Redis、provider connection 和 JVM JIT 仍可能影响整体 retrieval wall time，C7 把 rerank stage P50/P95 作为主 latency 证据，完整 retrieval P50/P95 只作辅助诊断。C7 不清空共享 cache，不为实验破坏业务状态。

### 8. Offline comparator

`compare_reranker_ab.py` 输入两个 arm 的 metadata 与 compact details 集合，处理顺序固定：

1. 解析并验证 schema；
2. 比较 strict identity 与 allowed differences；
3. 检查 per-run report status、errors 与 sample pairing；
4. 验证 heuristic/model provider coverage；
5. 只有 status 为 `COMPARABLE` 才计算并展示收益 deltas；
6. 输出 compact JSON 与 Markdown。

即使 `NOT_COMPARABLE`，仍输出 provider counts、fallback histogram、missing pairs 与 stable reasons，便于诊断；但 quality delta 区域必须标为 unavailable，不能计算“成功子集收益”。

### 9. Evidence outputs

建议 tracked evidence：

- 一份 C7 Markdown summary；
- 一份 compact comparison JSON，含 identity、validity、arm aggregates、per-sample metric/latency facts 与 hashes；
- 两份 sanitized arm metadata。

raw debug/ask response details 与重复运行的完整大文件默认留在 `tmp/eval/`，不 tracked；compact JSON 记录其 SHA-256。若用户要求长期保存 raw details，必须先做敏感字段与体积审查。

### 10. External-call execution

离线实现、unit tests、plan-only、metadata/comparator fixtures 均使用合成 JSON，真实调用为 0。

获批后执行分两级：

1. canary：每 arm 使用相同少量 sample IDs，验证 actual backend identity 与 provider attribution；
2. full：只有 canary 无 fallback、配置与预算仍获批准时，执行完整 30 条与批准的 repeats/warm-up。

真实执行不启用 ask/judge，不上传或重建 fixture，不使用其他知识库。model arm 出站 candidate passages 只来自固定 3 份 fixture。

## Verification Strategy

1. RED：per-sample retrieval latency 与 nearest-rank percentile 测试。
2. GREEN：在 `run_rag_eval.py` 增加 monotonic timing 与安全聚合。
3. RED：metadata 缺 eval-set hash、sample order、run index、manifest hash 时不可形成 C7 identity。
4. GREEN：扩展 reproducible metadata 与 arm manifest validation。
5. RED：identity mismatch、sample/run mismatch、partial fallback、coverage incomplete、zero-candidate mismatch 必须得到 stable `NOT_COMPARABLE`。
6. GREEN：新增离线 comparator 与 compact evidence renderer。
7. RED/GREEN：确保 comparator 不输出 question、contexts、passages、raw response、secret marker 或异常 message。
8. 运行 Python 全量单测、SensitiveLogs、`git diff --check`、旧字段/断链/受保护路径扫描。
9. 运行 `mvn -q test` 做最终仓库回归；如 Docker 不可用，如实记录容器测试 skip，不把它描述为通过。
10. 不运行前端 build，除非 change 实际修改前端。
11. 未获单独授权时，真实 embedding/rerank/ask/judge/LLM/provider 调用量保持 0。

## Execution Outcome

- Full 按设计的 `H1/N1、N2/H2、H3/N3` 交替顺序完成；每 arm 只执行一次 3-call warm-up，measured run 各 3 次、每次固定 30 样本。
- 六个 measured details 的 strict identity、sample pairing 与 report status 均通过；heuristic 90/90 clean，NVIDIA 90/90 clean，model coverage=100%、fallback=0。
- Comparator 为 `COMPARABLE`；Recall@5 +7.84pp、MRR +0.0895、Top1 +3.70pp，三个 repeat 的质量指标一致。
- Server-side NVIDIA rerank P50/P95 为 363/688ms；overall retrieval P50 增加 188ms。H1 冷启动异常污染 overall P95，因此不把 aggregate P95 下降解释为模型性能收益。
- 设计中的安全边界保持成立：默认 provider 不变、ask/judge/generation=0、不剔除样本、不吞并 C8/C9/C10。
- 用户已于 2026-07-20 验收 full evidence 与结论边界；默认 provider 明确继续为 heuristic，evaluation delta 已接受并归档。

## Rollout And Compatibility

- 现有单 run CLI 与报告字段保持兼容；新增字段为 additive。
- comparator 是独立离线入口，不改变 backend API。
- 默认 heuristic 与 tracked production config 不变。
- 无数据库 migration、无新基础设施、无新依赖；Python 继续只使用标准库。
- A/B 执行失败只产生诊断文件，不修改 KB 或业务数据。

## 决策记录

> 审批状态：用户已于 2026-07-20 一次性批准本节 15 条建议，选择推荐的 `R=3,W=3`、arm 交替和 canary→full 方案。下文原始“待用户在事前闸门确认”措辞作为规划审阅痕迹保留，不再表示未决。

### 决策 1：采用两个独立 arm 加离线 comparator
- **面临的选择**：给 debug API 增加按请求 provider override；让 Python 自动重启 backend 并跑两个 arm；由明确启动流程产生两个独立 arm，再离线比较。
- **选了哪个 + 为什么**：建议两个独立 arm加离线 comparator；它不扩大运行时 API 权限，也让每组配置、日志和失败边界清楚，待用户在事前闸门确认。
- **放弃的代价**：请求级 override 会改变生产 provider 契约并可能被误用；自动重启会把进程、凭据和端口编排塞进评测脚本，增加平台耦合与隐式状态。

### 决策 2：严格身份字段全等，只对白名单字段放行差异
- **面临的选择**：只检查报告文件名；只检查 Git HEAD；对 eval-set、fixture、KB、配置、样本与 Git HEAD 做 strict identity，并显式列出 provider 差异白名单。
- **选了哪个 + 为什么**：建议 strict identity + allowed differences；只有这样才能证明结果差异主要来自 reranker，待用户在事前闸门确认。
- **放弃的代价**：文件名不提供任何身份保证；只看 Git HEAD 会漏掉 KB、配置、样本选择和运行时 override 漂移。

### 决策 3：C7 补最小 hash/manifest，不等待也不吞并 C8a
- **面临的选择**：等 C8a 完成后再做 C7；在 C7 完成整个 dataset schema/versioning；C7 只补 A/B 必需的 eval-set hash、sample order 和 sanitized arm manifest。
- **选了哪个 + 为什么**：建议最小 identity 补强；它满足 accepted evaluation spec 和 C7 可比性，同时不提前扩张 C8a，待用户在事前闸门确认。
- **放弃的代价**：等待 C8a 会打乱冻结路线图的 C6→C7顺序；吞并 C8a 会把一次 A/B 变成数据治理大项目。

### 决策 4：干净 model arm 要求 100% eligible observation 生效
- **面临的选择**：只要部分 model 成功就比较成功子集；设置低于 100% 的覆盖阈值；所有 rerank-eligible observations 均 effective-model 且 0 fallback。
- **选了哪个 + 为什么**：建议 100% 覆盖；30 条开发集规模有限，任何 fallback 都足以污染组间归因，待用户在事前闸门确认。
- **放弃的代价**：成功子集会产生选择偏差；低阈值会把 model 与 heuristic 混合组包装成单一 model 结果。

### 决策 5：zero-candidate 单列为 not-applicable
- **面临的选择**：把 zero-candidate 算 model failure；从报告中删除该样本；保留配对但不计入 model coverage 分母，并要求两 arm 一致。
- **选了哪个 + 为什么**：建议配对保留、coverage 不计、两 arm 一致；没有候选时不存在 rerank 顺序，但该样本仍是 retrieval 结果的一部分，待用户在事前闸门确认。
- **放弃的代价**：算 failure 会惩罚没有调用机会的 provider；删除样本会掩盖 retrieval 行为并破坏固定样本身份。

### 决策 6：comparison validity 不复用既有 Report status
- **面临的选择**：把干净 arm 改写成 `CLEAN`；把 partial fallback 塞进 `PARTIAL`；保留 per-run `RETRIEVAL_ONLY`，另设 `comparisonStatus`。
- **选了哪个 + 为什么**：建议独立 `COMPARABLE/NOT_COMPARABLE/FAILED`；它不会混淆 retrieval-only 与 generation status，待用户在事前闸门确认。
- **放弃的代价**：改成 `CLEAN` 会暗示 generation 也完成；复用 `PARTIAL` 无法区分 ask error 与 A/B identity/provider 污染。

### 决策 7：C7 只比较 retrieval，不调用 ask 或 judge
- **面临的选择**：同时比较生成与引用；只做 retrieval；把 judge 一并打开形成综合分。
- **选了哪个 + 为什么**：建议 retrieval-only；reranker 直接影响候选顺序，隔离变量最清楚，也避免 LLM 随机性和额外出站，待用户在事前闸门确认。
- **放弃的代价**：同时跑生成会混入 LLM、prompt、citation 和 no-answer 波动；综合分会掩盖不同指标通道的失败。

### 决策 8：同时记录 rerank stage 与 retrieval wall-clock，主次分明
- **面临的选择**：只用总运行时间；只用 rerank latency；同时记录两者并把 rerank P50/P95作为主口径、retrieval P50/P95作辅助。
- **选了哪个 + 为什么**：建议双口径；既能隔离 provider 成本，也能看用户可感知链路，但不把缓存/embedding 影响误算成 reranker，待用户在事前闸门确认。
- **放弃的代价**：总时间除样本数不是 percentile且混合太多阶段；只看 rerank latency又看不到整体链路代价。

### 决策 9：percentile 使用 nearest-rank 并保留 observation count
- **面临的选择**：依赖外部统计库插值；手写线性插值；使用标准库可实现的 nearest-rank。
- **选了哪个 + 为什么**：建议 nearest-rank；算法简单、可复现、无需新依赖，且对 30×R observations 的含义直观，待用户在事前闸门确认。
- **放弃的代价**：外部库会新增依赖；自定义插值容易与其他工具口径不一致且难审计。

### 决策 10：正式 latency 候选采用 3 repeats、3 warm-up并交替 arm 顺序
- **面临的选择**：单次 30 条直接下 latency 结论；大量并发压测；3 次串行 measured repeats、每 arm 3 次 warm-up并交替顺序。
- **选了哪个 + 为什么**：建议第三种作为待批准预算；它比单次更能暴露波动，又不把 C7扩大为 SLA/压测项目，待用户在事前闸门确认。
- **放弃的代价**：单次 P95 对偶发网络/JIT很敏感；并发压测会引入限流、吞吐与生产容量问题，远超当前目标。

### 决策 11：任何失败或 fallback 都不通过删样本“清洗”
- **面临的选择**：删除失败样本后比较；只比较两 arm 都成功的交集；保留固定全集并将 comparison 判为不可比较。
- **选了哪个 + 为什么**：建议固定全集 + fail validity；这避免选择偏差，并把 provider 稳定性纳入真实证据，待用户在事前闸门确认。
- **放弃的代价**：删除失败样本或取成功交集会系统性美化 model arm，最终指标不再代表批准的 30 条集合。

### 决策 12：tracked 只保存紧凑证据，raw details 默认留 tmp
- **面临的选择**：提交全部重复运行 raw response；完全不保存逐样本证据；提交 compact per-sample facts与hash，raw details 留本地临时目录。
- **选了哪个 + 为什么**：建议 compact evidence + raw hash；既可审计，又减少仓库体积和问题/context 暴露，待用户在事前闸门确认。
- **放弃的代价**：全部 raw 文件体积大且包含固定问题/contexts；只留汇总数字无法追查样本级差异。

### 决策 13：真实执行分 canary 与 full 两次授权闸门
- **面临的选择**：批准实现后直接跑全部外调；永远只做合成测试；先披露 canary，验证归因后再披露/确认 full budget。
- **选了哪个 + 为什么**：建议 canary→full；真实 endpoint 已 smoke 过，但批量数据与限流风险不同，分级闸门能在小成本下先发现配置/fallback问题，待用户在事前闸门确认。
- **放弃的代价**：直接 full 可能在错误配置下浪费调用并污染报告；只做合成测试永远无法得到真实收益证据。

### 决策 14：C7 不自动改默认 provider，也不建立通用 pass/fail gate
- **面临的选择**：model 指标更好就自动切默认；在 C7 同时定义 CI 质量阈值；只输出限定身份下的观察结论和建议。
- **选了哪个 + 为什么**：建议只输出证据与建议；默认行为变更和通用门禁都有独立风险与后续路线图归属，待用户在事前闸门确认。
- **放弃的代价**：自动切默认缺少生产稳定性与成本依据；把 C10 门禁塞入 C7 会混合实验与治理两个验收问题。

### 决策 15：不扩展 Java/API、不加 retry、不碰 C8/C9/C10
- **面临的选择**：顺便扩 debug API/config endpoint与 provider retry；把后续评测治理一次做完；把改动限制在 Python runner/comparator与评测文档。
- **选了哪个 + 为什么**：建议 Python/evaluation-only；C6 已提供足够 attribution，最小范围即可完成 C7，待用户在事前闸门确认。
- **放弃的代价**：扩 Java/API/retry 会改变调用量与生产契约；吞并后续阶段会扩大回归面并破坏串行路线图。
