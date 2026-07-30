# Design: C16 Bounded Query Router

## 1. 设计目标与边界

C16 在现有 tenant-scoped RAG 主链路之前增加一个显式、可观测、预算受限的路由层。首版不是开放式 planner：只识别可由当前单轮 RAG 处理的事实型问题，并只注册 `fact-v1`。Router 默认关闭，关闭时完全沿用当前 `RAGServiceImpl` 路径；启用后，非 fact 输入稳定返回 unsupported，不静默回退到 legacy 或未来策略。

设计必须同时守住五条边界：

1. route classification 不产生 provider 调用；
2. strategy 与 budget 只能由服务端配置和 registry 决定；
3. tenant identity、KB/vector scope 与 reserved filter contract 不改变；
4. no-answer 是 retrieval 后 evidence policy，不是意图分类；
5. C16 evidence 不改写既有 retrieval/generation/citation/no-answer/judge 指标，也不构成 Agentic RAG 证明。

## 2. 当前实现事实与缺口

当前 `QueryEngineImpl` 已执行 normalization、polite/explanation stripping、有限 synonym/explanation variants、dense + keyword + RRF、rerank 和 diagnostics。`RAGServiceImpl` 在检索为空时对 explanation 问题做额外 fallback query，并在仍为空时直接返回 `QAResponse.noResult`；同步链路随后调用 `AnswerGeneratorImpl`，后者以模型输出文本判断 `status=no_result`。现有 `PromptBuilder` 已有 context token budget，LLM/provider/reranker 各自也有 timeout 或调用归因，但没有一个跨阶段 immutable budget。

因此 C16 不重新发明 retrieval 或 generation，而是在它们外部建立：

- route decision 与 finite registry；
- fact strategy 的单次 orchestration 边界；
- 跨阶段 usage ledger；
- 统一 evidence/final-state policy；
- cache、sync/SSE/MCP、telemetry 与 evaluation 的一致 attribution。

## 3. 组件设计

### 3.1 Router 配置与值对象

新增 `rag.router.*` 配置，至少包含：

- `enabled=false`；
- `classifier-version=fact-intent-v1`；
- `policy-version=evidence-no-answer-v1`；
- `fact.max-query-variants`；
- `fact.max-retrieval-passes=1`；
- `fact.max-rerank-calls=1`；
- `fact.max-generation-calls=1`；
- `fact.max-context-tokens`、`fact.max-output-tokens`；
- `fact.deadline-millis`。

启动时配置必须通过 hard bounds 校验。无效、负数、零值、超出系统上限、budget 之间不一致或未知 classifier/policy/strategy 使 Router configuration fail startup；不得静默改成不受限值。

核心 immutable 值对象：

- `QueryIntent`：`FACT / UNSUPPORTED / INVALID`；
- `QueryRouteReason`：有限枚举，例如 `FACT_LOOKUP_CUE`、`DEFINITION_CUE`、`MULTI_HOP_CUE`、`GLOBAL_CUE`、`HIGH_RISK_CUE`、`AMBIGUOUS`、`INVALID_INPUT`；
- `QueryStrategyId`：首版只允许 `fact-v1`；
- `QueryExecutionBudget`：所有 server-owned ceilings 与 deadline；
- `QueryRoutePlan`：classifier/policy version、intent、requested/effective strategy、reason、budget profile identity；
- `QueryBudgetUsage`：query variants、retrieval/rerank/generation/provider calls、estimated context/output tokens、elapsed/deadline outcome；
- `QueryFinalState`：`ANSWER / NO_ANSWER / ERROR / UNSUPPORTED / INVALID`；
- `NoAnswerReason`：有限 evidence/final-state taxonomy。

这些对象不得存放 raw query、tenant/user/KB identity、context、answer、token、provider body 或异常文本。

### 3.2 `fact-intent-v1` 分类器

`QueryIntentClassifier` 是纯函数接口，输入只包含 bounded query text，输出 intent + reason + classifier version。`DeterministicFactIntentClassifier` 使用与 eval artifact 无关的有限规则：

1. 先做长度、空白、控制字符和基本 Unicode 边界校验；
2. 识别明确 multi-hop、global summary、比较综合、开放式规划和 high-risk cues，优先输出 `UNSUPPORTED`；
3. 识别单主体 fact/definition lookup cues，输出 `FACT`；
4. 冲突或无法可靠判断时输出 `UNSUPPORTED/AMBIGUOUS`；
5. 永不调用 embedding、rerank、LLM、judge、network、database 或 cache。

规则顺序、regex/cue identity 和 normalization identity 必须有稳定版本；实现不得读取 `docs/eval`、question set、expected route sidecar 或报告。

### 3.3 Router 与有限 Strategy Registry

`BoundedQueryRouter` 只负责：

- 检查 enabled/disabled mode；
- 调用 classifier；
- 从 immutable registry 解析 effective strategy；
- 绑定 server-owned budget 与 policy identity；
- 形成 route plan。

registry 首版只有 `fact-v1 -> FactQueryStrategyExecutor`。如果 classifier 输出 `UNSUPPORTED/INVALID`、registry 缺失、配置引用未知 strategy 或 plan 不一致，必须在 retrieval/provider 前 fail closed。Router 不接受客户端 strategy hint，也不提供动态 class name/reflection/plugin 加载。

Router disabled 时调用独立的 legacy compatibility branch；该 branch 不被标记成 `fact-v1`，也不进入 C16 strategy success 指标。

### 3.4 Fact Strategy Executor

`FactQueryStrategyExecutor` 从现有 `RAGServiceImpl` 抽取或编排单轮 fact 路径：

1. budget admission；
2. tenant-scoped retrieval orchestration；
3. pre-generation evidence decision；
4. generation；
5. citation validation 与 final-state decision；
6. metadata/telemetry/usage 汇总。

它复用现有 `QueryEngine`、`AnswerGenerator`、tenant vector scope、provider 与 citation contract。C16 不改变 hybrid、RRF、rerank provider 或 prompt。当前 explanation empty-result fallback 不进入 `fact-v1`：该 fallback 本质上是第二 retrieval orchestration，会与首版 `max-retrieval-passes=1` 冲突；Router disabled 时仍保留 legacy 行为。

`QueryEngineImpl` 的 query variant builder 增加 server-owned upper bound，并在返回 diagnostics 时给出实际 variant count。上限耗尽时只停止新增变体，不继续隐藏调用；如果原始 query 本身无法被纳入，返回稳定 budget error。

### 3.5 Budget Ledger 与 Deadline

`QueryBudgetLedger` 随 route plan 创建，在以下边界进行原子/单请求扣减：classifier、query variant 构造、retrieval orchestration、rerank outcome、generation/provider call、context/output token estimate。任何预算在调用前不足时立即停止并返回 `BUDGET_EXHAUSTED`；调用后若 provider diagnostics 显示实际次数超出 ceiling，则 final state 为 `ERROR/BUDGET_VIOLATION`，正式 C16 evidence 失败。

overall deadline 使用 monotonic clock，在 admission 和每个 stage 之间检查。已进入的底层调用继续依赖现有 provider timeout/cancel contract；C16 不宣称能抢占所有阻塞 I/O。deadline 到期后不得开始下一 stage、不得整链自动 replay，也不得把 partial output 写入正常 cache/history。

### 3.6 `evidence-no-answer-v1`

新增纯决策组件 `EvidenceNoAnswerPolicy`，分两次执行：

**Pre-generation：**

- retrieval dependency/error -> `ERROR`；
- contexts 为空，或没有 context 满足当前 server-owned minScore/tenant/canonical evidence contract -> `NO_ANSWER/INSUFFICIENT_EVIDENCE`，generation calls=0；
- 有合格 contexts -> 允许进入 generation。

**Post-generation：**

- model 输出既有拒答语义 -> `NO_ANSWER/MODEL_REFUSAL`；
- fact answer 没有 validated citation/evidence -> `NO_ANSWER/UNVALIDATED_EVIDENCE`；
- answer 与 citation policy 通过 -> `ANSWER`；
- provider/citation dependency 或预算错误 -> `ERROR`。

policy 不使用 expected answer、eval label 或 judge；no-answer 不参与 route classifier。现有 `QAResponse.status=no_result` 保留兼容语义，同时增加低基数 `routeFinalState` / `noAnswerReason` metadata。错误仍使用 error contract，不得映射成 no-result。

### 3.7 Sync、SSE 与 MCP 接入

同步 `RAGService.ask` 在 Router enabled 时返回 route plan、usage 与 final state 的白名单 metadata。REST controller 继续负责 query count/history；只有 `ANSWER` 且既有 success 条件满足时保存正常历史。

流式 `RAGService.askStream` 在 generation 前共享 route/evidence decision。它可以发送模型生成的拒答文本，但 completion 时必须从 bounded answer buffer/terminal signal 形成同一 post-generation final state；`NO_ANSWER/ERROR/UNSUPPORTED` 不得保存为正常成功历史。首版不新增 SSE wire event 或修改前端协议，route facts 记录在 terminal signal、trace 与 eval/integration evidence；若产品必须把 metadata 发给客户端，另立 API change。

C15 `rag.ask` 继续调用 read-only service boundary；Router enabled 时继承同一 route plan/policy，但 MCP schema 不新增 client selector，QA history/query count 仍保持不变。`rag.search` 只使用 retrieval，不自动成为 fact answer strategy；是否纳入未来 Router Tool contract另立 change。

### 3.8 Cache Compatibility

QA cache identity 增加：

- router enabled mode；
- classifier version；
- effective strategy 或 legacy marker；
- evidence/no-answer policy version；
- 影响结果的 budget profile hash/version。

禁止把 `UNSUPPORTED/INVALID/ERROR/BUDGET_EXHAUSTED` 存入普通成功 cache。`NO_ANSWER` 是否缓存沿用现有 contract；如现有实现仅缓存成功回答，则 C16 不扩大缓存范围。cache diagnostics 不输出 query 或 identity。

### 3.9 Telemetry 与安全输出

在既有低基数白名单中增加：

- classifier version；
- requested/effective strategy；
- route reason taxonomy；
- policy version；
- final state / no-answer reason；
- budget outcome。

普通 metric label 不记录 query、规则命中片段、tenant/user/KB ID、budget 数值、token 明细、deadline 毫秒、provider/model 自由文本或 error detail。详细 usage 只进入受控 response/eval metadata 的数值白名单；普通日志只记录低基数 category 与 count，不记录原问题。

## 4. Evaluation 设计

### 4.1 Release 身份

新增 `bounded-query-router-eval-v1` manifest，引用已接受的 `rag-eval-dev-v2` identity，但使用独立 route expectation sidecar 和 budget profile artifact。sidecar 按 sample ID 记录 expected intent/required status，不复制 question/answer/context；运行时 classifier 不能读取它。

manifest 固定：

- dataset release/hash/bytes；
- ordered selected sample IDs；
- sidecar schema/hash/bytes；
- classifier/strategy/policy/budget profile identity；
- evaluator version；
- required channel/status/exit code。

validator 使用 Python 标准库，并在 backend/container/provider 前 fail fast。C16 不修改 v2 question set、annotations、fixtures 或历史报告。

### 4.2 指标通道

正式报告至少拆分：

1. `classification`：FACT precision、recall、coverage、unsupported leakage、invalid/unexpected；
2. `strategy_execution`：expected FACT 中 effective `fact-v1` 数、unexpected strategy、registry/plan error；
3. `budget`：每项 ceiling violation、deadline exceeded、actual provider/model call count；
4. `retrieval`：沿用既有 Recall@K/MRR/Top1，按 effective strategy 切片但不改变公式；
5. `generation_citation`：只在授权/适用时沿用既有通道；deterministic 主证据不得外推真实质量；
6. `no_answer`：route 前不得预测 no-answer，运行后按 policy reason 统计 accuracy/violation；
7. `errors`：missing/unexpected/failed/errors/skipped 与 identity drift。

global Router status 使用独立 `PASS / FAIL / NOT_EVALUABLE / INVALID`，不替代现有 `CLEAN / PARTIAL / RETRIEVAL_ONLY / FAILED`。任一 required channel 不完整或 budget violation > 0 时不得为 `PASS`。

### 4.3 主证据与外调

规划、validator、classifier 和 unit/integration 主证据使用 deterministic embedding/generation 或纯 fixture，真实 provider/model calls=0、business data outbound=false。任何 live router evaluation 必须单独披露样本/策略分布、embedding/rerank/generation/judge provider/model、最大调用/重试、query/context 出站、费用与限流并取得授权；未授权时 `SKIPPED`，不阻塞 deterministic contract acceptance，也不能声称真实质量收益。

## 5. 实现切片

1. Router contract 与 default-off compatibility：值对象、配置、classifier、registry，纯 unit/property tests。
2. Budgeted fact retrieval：query variant cap、单 retrieval orchestration、diagnostics/usage，聚焦 QueryEngine tests。
3. Unified evidence/no-answer：sync fact executor、cache identity、generation/citation final state。
4. SSE/MCP/telemetry integration：terminal state、一致 attribution、read-only regression。
5. Versioned evaluation release：validator/evaluator/report、plan-only 与 deterministic profile。
6. Full gates 与用户验收：Maven/Python/security/static evidence，之后才接受双 delta、归档并恢复 `IDLE`。

每个切片必须先 RED 再最小 GREEN；不得在 Router change 内顺手重构 retrieval/provider、升级依赖或实现高级策略。

## 6. 回滚与兼容

- 运行回滚：保持 `rag.router.enabled=false` 即回到现有 legacy path；Router identity 不进入 disabled path 的成功声明。
- 数据回滚：无数据库 migration；cache key version 隔离，无需迁移旧 entry。
- API 兼容：同步 response 只增加 metadata keys；request DTO 与 SSE wire format不变。
- 代码回滚：router/strategy/policy 通过接口接入，不删除现有 legacy flow，直到未来独立 change 决定生产默认切换。
- evidence 回滚：C16 report/versioned artifacts不覆盖既有评测报告；失败或不完整 evidence 保留其原 status，不回写成 PASS。

## 7. 决策记录

### 决策 1：Router 首版如何上线
- **面临的选择**：直接替换现有问答路径；默认关闭、显式启用；默认开启但提供回退开关。
- **选了哪个 + 为什么**：选择默认关闭、显式启用；C16 会改变非 fact 请求语义，先保留现有默认行为最符合可回滚和 baseline 可比性。
- **放弃的代价**：直接替换会让未评测分类错误立即影响用户；默认开启再回退会让“默认行为是否已改变”难以审计。

### 决策 2：首版分类器使用什么能力
- **面临的选择**：LLM classifier；embedding/模型 classifier；确定性本地 allowlist classifier。
- **选了哪个 + 为什么**：选择确定性本地 `fact-intent-v1`；它零外调、可版本化、可重复，并能在 Router 执行前给出稳定预算。
- **放弃的代价**：LLM/embedding 分类会增加费用、延迟、数据出站和非确定性，也会把一次 ask 变成额外 provider 调用。

### 决策 3：无法确定是否为 fact 时如何处理
- **面临的选择**：默认当 fact；回退 legacy；返回 `UNSUPPORTED/AMBIGUOUS`。
- **选了哪个 + 为什么**：选择 `UNSUPPORTED/AMBIGUOUS`；enabled 模式必须保守，不能把未知请求伪装成首版已支持策略。
- **放弃的代价**：默认 fact 会扩大误路由；回退 legacy 会偷渡第二条无预算策略并污染 effective strategy 归因。

### 决策 4：首版 Strategy Registry 包含哪些策略
- **面临的选择**：fact + legacy passthrough；fact + multi-hop stub；只注册 `fact-v1`。
- **选了哪个 + 为什么**：只注册 `fact-v1`；路线图明确高级策略逐项独立推进，首版 registry 必须与完成声明一致。
- **放弃的代价**：legacy passthrough 会形成未定义策略；multi-hop stub 容易被误当成能力存在并让测试通过空实现。

### 决策 5：no-answer 放在哪里判断
- **面临的选择**：作为前置 route 类别；只依赖模型拒答文本；retrieval 后与 generation 后共享版本化 policy。
- **选了哪个 + 为什么**：选择共享 `evidence-no-answer-v1`，分别做 pre/post decision；拒答的依据是证据与最终回答，不是单看问题文本。
- **放弃的代价**：前置分类会在未检索前臆测无答案；只看模型文本会把 evidence 与 provider 行为混在一起，且无法保证 generation calls=0。

### 决策 6：fact answer 的最低 evidence 条件
- **面临的选择**：有任意 context 即成功；只看模型非拒答文本；要求 retrieval evidence 且最终至少一条 validated citation。
- **选了哪个 + 为什么**：选择 retrieval evidence + validated citation；fact 策略的成功必须能回连到本轮证据，与既有 citation contract 一致。
- **放弃的代价**：任意 context 可能相关性不足；只看模型文本会把无引用的流畅回答误记为有证据答案。

### 决策 7：explanatory empty-result fallback 是否进入 fact-v1
- **面临的选择**：保留所有 legacy fallback；允许一次额外 fallback；fact-v1 只做一次 retrieval orchestration。
- **选了哪个 + 为什么**：选择只做一次 retrieval orchestration；explanatory fallback 属于额外策略尝试，与首版 fact 的单轮预算和 why/how unsupported 边界冲突。
- **放弃的代价**：保留 fallback 会隐藏第二次检索并使预算归因不可信；允许特例会让策略定义随问题模式分叉。

### 决策 8：预算如何传递
- **面临的选择**：只用配置并事后记录；各组件自行读取配置；route plan 绑定 immutable budget + 单请求 usage ledger。
- **选了哪个 + 为什么**：选择 immutable budget 与 usage ledger；只有这样才能在调用前拒绝、跨阶段汇总实际使用并检测越界。
- **放弃的代价**：事后记录无法阻止调用；各组件自行读配置会产生不一致快照和难以复现的 budget identity。

### 决策 9：overall deadline 提供什么保证
- **面临的选择**：声称强制中断全部 I/O；只依赖各 provider timeout；在 admission/stage 间 hard-stop并复用底层 timeout。
- **选了哪个 + 为什么**：选择 stage 间 hard-stop + 底层 timeout；这是当前同步调用结构能够真实兑现的保证。
- **放弃的代价**：强制中断声明超出当前能力；只依赖 provider timeout 无法限制 stage 之间继续执行和整链总时长。

### 决策 10：SSE 如何携带 route/final state
- **面临的选择**：新增 SSE metadata event；缓冲全部答案后再发送；保持 wire 不变，在 terminal signal/trace/evidence 中记录。
- **选了哪个 + 为什么**：首版保持 wire 不变并扩展 terminal signal/trace；避免把 C16 扩成前端/API 协议变更，同时仍可验证最终状态与历史副作用。
- **放弃的代价**：新增 event 会要求前端与客户端兼容改造；全缓冲会失去流式价值并扩大内存/延迟。

### 决策 11：Router 结果如何进入 Cache
- **面临的选择**：继续使用旧 key；启用 Router 时关闭 cache；把 router/classifier/strategy/policy/budget identity 纳入兼容键。
- **选了哪个 + 为什么**：选择扩展兼容 identity；保留 cache 能力，同时防止 legacy 或不同 policy 的结果串用。
- **放弃的代价**：旧 key 会产生语义污染；完全关闭 cache 会改变性能/外调行为且无法验证未来安全复用。

### 决策 12：Router evaluation 是否修改现有 v2 数据集
- **面临的选择**：直接给 v2 question records 加 route label；新建一套完整复制数据集；引用 v2 identity并增加独立 expectation sidecar。
- **选了哪个 + 为什么**：选择独立 sidecar；保持 C8 已接受的 immutable dataset bytes，同时为 C16 固定 route expectation。
- **放弃的代价**：直接修改会破坏 release identity；复制整套数据会产生内容漂移与双份事实源。

### 决策 13：C16 global status 与既有 Report status 的关系
- **面临的选择**：替换现有 Report status；合成一个大总分；使用独立 Router status并保留各通道。
- **选了哪个 + 为什么**：选择独立 `PASS/FAIL/NOT_EVALUABLE/INVALID`；route/budget completeness 与既有 retrieval/generation 状态是不同事实。
- **放弃的代价**：替换会改写历史口径；大总分会让 route error 或 budget violation被其他高分掩盖。

### 决策 14：正式主证据使用真实还是确定性 Provider
- **面临的选择**：直接跑真实全量 provider；只做 unit mock；deterministic integration 为主、live evidence 单独授权。
- **选了哪个 + 为什么**：选择 deterministic integration 主证据；它能验证路由、预算、状态和副作用，且零数据出站、零费用、可重复。
- **放弃的代价**：真实 provider 未授权且会引入费用/非确定性；unit mock 不能证明 sync/SSE/MCP/cache 的集成边界。

### 决策 15：C16 是否改变默认 Provider、Prompt 或 Rerank
- **面临的选择**：为 fact 策略选择新 prompt/model；自动启用 C7 model reranker；完全复用当前 server-owned defaults。
- **选了哪个 + 为什么**：选择完全复用当前 defaults；C16 验证的是路由与预算，不把质量变量混入同一 change。
- **放弃的代价**：新 prompt/model/reranker 会使 baseline 不再可比较，也需要独立外调和质量收益证据。

### 决策 16：客户端能否提供 Route Hint 或 Budget
- **面临的选择**：允许高级客户端选择 strategy；允许有限 hint；完全由服务端决定。
- **选了哪个 + 为什么**：选择完全由服务端决定；这能保持 tenant/security/provider/budget 边界一致，避免 MCP/REST 输入成为绕过面。
- **放弃的代价**：允许 selector 会让客户端绕过 unsupported 和费用上限；有限 hint 仍需要新的权限与信任契约。

### 决策 17：何时宣称 C16 完成
- **面临的选择**：classifier unit tests 通过即完成；deterministic integrated profile 完整通过即完成；必须同时完成 live provider质量收益。
- **选了哪个 + 为什么**：选择 deterministic integrated profile 完整通过作为 contract acceptance；live provider 质量/成本另行授权和结论。
- **放弃的代价**：unit-only 缺集成证据；把 live provider 设为强制会把未授权外调与基础 contract 绑定，并诱导虚假完成。

### 决策 18：C16 与 Agentic RAG 的关系
- **面临的选择**：把 Router 作为 Agentic RAG 首版；同时加入 tool loop/memory；只称 bounded fact router。
- **选了哪个 + 为什么**：只称 bounded fact router；当前没有 multi-step planner、tool loop、memory、reflection 或人工确认，必须守住项目原型定位。
- **放弃的代价**：Agentic 命名会夸大能力；顺手加入 tool loop/memory 会跨越多个契约与评测门禁。
