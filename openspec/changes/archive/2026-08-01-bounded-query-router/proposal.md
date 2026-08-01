# Proposal: C16 Bounded Query Router

## Why

项目当前已有 query normalization、有限 query variants、hybrid retrieval、rerank、explanatory empty-result fallback、generation、citation 与 no-answer 处理，但这些行为仍由 `QueryEngineImpl`、`RAGServiceImpl` 和 `AnswerGeneratorImpl` 分散决定。调用方看不到一次请求为何使用某条执行路径，也没有一个统一对象锁定路由分类、最大检索次数、query variant 数、generation 次数、token 与 deadline 预算。

C13/C14 已完成服务端 tenant enforcement 与固定 adversarial matrix，C15 也已独立验收归档，因此 C16 的外部能力前置门禁已经满足。路线图将 C16 定义为独立 Type C change：首版只建立受约束的 fact 策略和统一 evidence/no-answer policy，不与 MCP 捆绑，也不提前实现 multi-hop、global summary、high-risk 人工确认或 Agentic RAG。

当前 no-answer 存在两个主要入口：检索为空时 `RAGServiceImpl` 直接返回 `QAResponse.noResult`，模型输出特定拒答文本时 `AnswerGeneratorImpl` 再通过字符串判断写入 `status=no_result`。同步与流式路径的可观察事实也不同。C16 需要先把“是否有足够证据继续生成”与“执行发生错误”分开，再让 fact 策略、同步问答、流式问答、MCP read-only ask 和评测使用同一版本化 policy identity；不能把 no-answer 当作纯前置意图类别。

## 用户故事：改前坏事 → 改后不同

- 改前坏事：同一个问题可能经过 query variants、explanatory fallback、rerank 与 generation，但调用方只能看到零散 diagnostics，无法确认请求到底按什么策略执行、用了几次检索或是否超出预算。
- 改后不同：显式启用 C16 后，每次请求先形成 immutable route plan，并返回版本化 classifier、requested/effective strategy、reason、budget 与实际使用量；事实型问题只由 `fact-v1` 执行。
- 改前坏事：若为了支持未来 multi-hop 而直接让 LLM 自由规划，模型可以不受控地重复检索、扩大 prompt、增加外调和费用。
- 改后不同：C16 首版分类器不调用模型，只有固定 `fact-v1` registry；query variants、retrieval pass、rerank、generation、token 与 deadline 都有 server-owned hard bound，客户端不能覆盖。
- 改前坏事：检索为空、证据不足、模型拒答和依赖错误可能被混成同一个“没有答案”，导致评测和运维无法区分业务拒答与系统失败。
- 改后不同：统一 policy 输出 `ANSWER / NO_ANSWER / ERROR` 与有限 reason taxonomy；证据不足不调用 generation，依赖错误也不会伪装成 no-answer。
- 改前坏事：把 route accuracy、retrieval、generation、citation 和 no-answer 混成一个总分，可能用某个成功通道掩盖路由误判或预算越界。
- 改后不同：C16 使用独立、版本化的 router evaluation profile，分别报告 classification、strategy execution、budget、retrieval、generation/citation 与 no-answer 通道，并以 fail-closed 状态聚合。

## Goals

1. 新增默认关闭的 `bounded-query-router` capability；关闭时现有 REST、SSE、MCP 和评测行为保持不变，不能仅因代码存在就宣称 Router 已启用。
2. 建立纯本地、确定性、版本化的 `fact-intent-v1` classifier，只输出 `FACT / UNSUPPORTED / INVALID`，不调用 embedding、rerank、LLM、judge 或其他 provider。
3. 建立 immutable `QueryRoutePlan`，固定 classifier identity、requested/effective strategy、reason、server-owned budget、tenant-safe request facts 和 contract version；客户端不能提交 strategy、provider、model、retry、timeout 或 budget selector。
4. 首版 registry 只注册 `fact-v1`；`UNSUPPORTED` 不偷跑 legacy/multi-hop/global/high-risk 路径，不产生 provider 调用，并返回稳定、可解释的 unsupported 结果。
5. 将现有单轮 retrieval → generation → citation 主链路收敛为 fact strategy executor；不改变默认 hybrid/BM25/RRF、heuristic reranker、provider、prompt、citation 算法或 tenant scope。
6. 对 fact strategy 强制 bounded execution：最多一次 strategy execution、一次 retrieval orchestration、有限 query variants、最多一次 rerank outcome、最多一次 generation、固定 context/output token ceiling、单请求 deadline 与零自动重放。
7. 建立版本化 `evidence-no-answer-v1`：no-answer 是 retrieval 后证据决策及最终回答状态，不是前置 route 类别；`ANSWER / NO_ANSWER / ERROR` 与 reason taxonomy 分离。
8. 让同步、流式和 C15 read-only ask 使用同一 route/evidence policy；同步 metadata 与 debug/eval 输出完整 bounded attribution，流式至少在 terminal signal、trace 与安全日志中保留同一最终状态。
9. 将 router/classifier/policy/budget identity 纳入 QA cache key 或 compatibility guard，禁止 legacy、不同策略或不同 policy 的结果互相污染。
10. 新增独立 C16 router evaluation release/profile，固定 route expectation sidecar、budget profile、Git/config identity、逐策略指标与 fail-closed status；主证据使用 synthetic/deterministic provider，真实 provider 调用为 0。

## Non-Goals

- 不实现 `router-multihop`、`router-global`、`router-high-risk`、query decomposition、循环 planner、tool loop、memory、reflection 或 Agentic RAG 平台。
- 不把 no-answer 建成前置 intent；不允许 classifier 因关键词“无法回答”直接跳过 retrieval 后的 evidence policy。
- 不使用 LLM、embedding、reranker 或 judge 做 route classification；不新增或升级外部依赖、模型或付费服务。
- 不修改默认 embedding/rerank/LLM provider、prompt template、chunking、RRF、citation 算法、C10 ACTIVE/DRAFT quality gate 或 C7 默认 heuristic 结论。
- 不为当前 eval set 定制生产 classifier 规则；评测 expected route 只作为独立 evidence，不进入运行时分类代码。
- 不开放客户端 strategy/budget/provider/model/timeout/retry/filter selector；现有 tenant scope 与 reserved-filter 防护保持 server-derived/fail-closed。
- 不修改数据库 schema、tenant model、MCP protocol/schema、前端交互或公开 REST request DTO；若实现证明必须修改这些边界，暂停并返回事前闸门。
- 不把 C16 PASS 描述为 multi-hop/global/high-risk 已支持、生产级 Agentic RAG、所有问题分类准确、真实 provider 质量提升或生产 SLA。
- 不执行真实 provider/model、live ask/judge、业务数据出站、真实 Milvus maintenance、部署、push 或 PR。
- 不修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/` 或历史报告。

## Capability Classification

- `confirmed`：C14 隔离门禁已通过；C15 已归档；现有 `QueryEngineImpl` 有 normalization/query variants/hybrid/RRF/rerank diagnostics，`RAGServiceImpl` 有 sync/SSE 与 explanatory empty-result fallback，`AnswerGeneratorImpl` 有 context token budget/citation/no-result metadata，OTel 已有 bounded `retrieval.route` label，评测集 v2 已含 fact/definition/reasoning/multi_hop/no_answer 类型。
- `partial`：已有路径与 provider 归因但没有统一 route plan；已有 topK/minScore/provider timeout/context token limit，但没有跨阶段总预算与有限 strategy registry；no-answer 分散在 empty retrieval 和生成文本判断；sync metadata、SSE terminal 与 eval details 的 attribution 不一致。
- `planned`：`fact-intent-v1`、`QueryRoutePlan`、`fact-v1` executor、`evidence-no-answer-v1`、budget usage ledger、cache compatibility、sync/SSE/MCP attribution、versioned router evaluation profile 与 per-strategy gate。
- `out_of_scope`：multi-hop/global/high-risk、人工确认状态机、Agent loop、LLM classifier、生产默认切换、真实 provider evidence、MCP 协议扩展、数据库/前端变更、tenant rollout 和未验证 vector adapters。
- `unknown`：fact allowlist 在真实业务问题上的覆盖率、合理的生产 deadline/token ceiling、SSE 客户端是否需要新的 terminal metadata wire contract、真实 provider 下的 latency/cost 分布，以及未来 multi-hop/global/high-risk 的分类边界。

## Proposed Contract

1. `rag.router.enabled` 默认 MUST 为 `false`。关闭时不得改变现有 query execution/cache/API 语义，也不得返回“Router 已启用”的能力声明。
2. 启用时每个问答请求 MUST 先由 `fact-intent-v1` 形成 `QueryRoutePlan`；分类器只使用 bounded normalized query shape 与有限规则，provider/model calls=0，并输出 `FACT / UNSUPPORTED / INVALID` 和稳定 reason。
3. 客户端输入、HTTP header/query/body、MCP arguments/metadata 与 eval annotation MUST NOT 选择或覆盖 strategy、budget、provider、model、retry、timeout、classifier 或 no-answer policy。
4. 首版 effective strategy 只有 `fact-v1`。`UNSUPPORTED` 请求不得静默执行 legacy、multi-hop、global 或 high-risk 路径；默认关闭模式仍保留现有 legacy 行为。
5. `fact-v1` MUST 复用现有 tenant-scoped hybrid retrieval、rerank、generation 与 citation contract，不得关闭 tenant filter、改变 provider 默认值或为评测样本特调 prompt/retrieval。
6. route plan MUST 固定 max retrieval passes、query variants、rerank/model calls、generation calls、context/output token ceiling 与 deadline。所有实际 usage MUST 汇总，预算耗尽返回稳定 `BUDGET_EXHAUSTED/DEADLINE_EXCEEDED`，不自动重放整个 strategy。
7. `evidence-no-answer-v1` MUST 在 retrieval 后判断证据是否足以进入 generation；空或不满足既有 minScore 的 evidence 返回 `NO_ANSWER/INSUFFICIENT_EVIDENCE` 且 generation calls=0。依赖异常返回 `ERROR`，不得伪装成 no-answer。
8. generation 后若模型给出拒答或 citation validation 无法形成 fact answer 的最低证据，最终状态 MUST 为 `NO_ANSWER` 并保留实际 generation/citation usage；`ANSWER` 必须与 validated citation/evidence facts 一致。
9. sync、SSE 与 C15 read-only ask MUST 共享同一 route/policy identity。任何 surface 不支持完整 wire metadata 时，也必须在内部 terminal signal、trace/eval evidence 中保留相同 effective strategy、final state、reason 与 usage。
10. QA cache MUST 绑定 router enabled state、classifier version、effective strategy、policy version 与影响结果的 budget profile；不同 identity 不得命中同一缓存结果。
11. 普通 metadata/trace/metrics 只允许低基数 strategy/state/reason/budget-outcome；不得把 query、tenant/user/KB ID、raw classifier feature、token、context、answer、provider body、异常 message/stack 作为 label 或普通日志内容。
12. C16 evaluation MUST 使用独立 versioned manifest/expectation sidecar 和 deterministic execution profile，分别报告 classification、unsupported leakage、budget、retrieval、generation/citation、no-answer 与错误通道；任一 required channel 缺失、error、unexpected strategy 或 budget violation 都不能被总平均掩盖。
13. C16 acceptance 主证据 MUST 为真实 provider/model calls=0、business data outbound=false；任何 live router ask/eval 仍需另行披露样本、provider/model、最大调用/尝试、数据出站、费用/零费用依据与限流风险并取得授权。
14. C16 完成只证明 `fact-v1` 和固定 evaluation profile；不得自动开启 Router production default，也不得宣称 multi-hop/global/high-risk 或 Agentic RAG 已完成。

## Impact

- 预计 production 代码主要位于 `rag-core`：新增 router/strategy/evidence/budget value objects 与服务，并对 `RAGServiceImpl`、`QueryEngineImpl`、`AnswerGeneratorImpl` 做 contract-preserving 接入。
- `rag-common` 预计只扩展低基数 route/strategy/policy telemetry key；不得记录 query 或高基数 identity。
- `rag-admin` 预计仅让 REST debug、SSE terminal diagnostics 和 C15 read-only ask 透传/白名单映射 route facts；公开 request DTO、MCP Tool schema 与 controller side-effect 语义默认不变。
- Python `scripts/` 预计新增 router evaluation validator/evaluator，并在现有 runner details 中加入 additive strategy attribution；不得改写既有 retrieval/generation/citation/no-answer/judge 公式或历史报告。
- 无数据库 migration、前端、依赖升级或 provider 默认值变更；若实现需要这些变更，必须暂停并重新审查 scope。
- 规划阶段只新增 OpenSpec artifacts、更新 `.ai/ACTIVE_TASK.md` 并追加 `.ai/AGENT_LOG.md`；accepted baseline 在用户验收前不修改。

## Risks And Mitigations

- 风险：只有 fact 策略时，分类失败会让原本可回答的问题变成 unsupported。缓解：Router 默认关闭；启用时采用保守 allowlist，正式报告同时给 fact precision/recall/coverage，不把低覆盖包装成安全或质量提升。
- 风险：`legacy passthrough` 被偷渡成第二策略。缓解：enabled 模式 registry 只允许 `fact-v1`；非 fact 稳定 unsupported，legacy 仅存在于 router disabled compatibility path。
- 风险：预算只写在 metadata 中，没有真正限制 query variants 或重试。缓解：budget 由 immutable ledger 在分类、retrieval、rerank、generation 前后扣减；query variant builder 接受 server-owned upper bound，测试验证超限不会继续调用。
- 风险：总 deadline 无法中断已经进入的阻塞 provider call。缓解：沿用现有 provider timeout，并在设计中把 overall deadline 定义为 admission/inter-stage hard stop；不得宣称能强制取消所有底层 I/O，后续需要 cooperative cancellation 时另立 change。
- 风险：统一 no-answer 改变既有评测口径。缓解：保留 retrieval/generation/citation/no-answer 独立通道；C16 新增 policy version 与 route gate，不回写历史 baseline，不用 C16 总状态替代既有 Report status。
- 风险：stream 已经发送 chunk 后才发现最终 no-answer。缓解：允许流式输出模型拒答文本，但 terminal signal 必须给出统一 final state；不把已发送内容重标为成功答案，controller 只保存符合既有完整成功条件的 history。
- 风险：cache 在 router 开关或 policy 变化后复用旧结果。缓解：cache compatibility identity 显式进入 key/entry guard，legacy 与 C16 结果隔离。
- 风险：评测 label 泄漏到生产 classifier。缓解：运行时分类器不读取 eval manifest/sidecar；测试只通过公共 classifier API 比较 observed 与 expected。
- 风险：C16 被描述成 Agentic RAG。缓解：spec、文档与 acceptance 只允许 `fact-v1 bounded router`，高级策略、tool loop、memory 与人工确认全部列为未来独立 change。

## Acceptance Evidence

- proposal、design 的全部决策、tasks 以及 `rag-system` / `evaluation` 双 spec delta 先经用户批准。
- classifier unit/property tests 覆盖 fact allowlist、unsupported multi-hop/global/high-risk/ambiguous inputs、Unicode/长度边界、空输入、determinism 与 zero-provider calls。
- router registry/plan tests 证明 enabled/disabled、客户端 override rejection、只有 `fact-v1`、immutable budget 与 stable sanitized reason。
- fact executor tests 证明 retrieval/query-variant/rerank/generation/token/deadline budget 在成功、no-answer、dependency failure、timeout/cancel 下均不越界、不整链自动重放。
- sync/SSE/MCP integration tests 证明同一问题的 classifier/policy/effective strategy/final state 一致，tenant scope 不变，MCP read-only side-effect contract 不回归。
- cache tests 证明 legacy/router、classifier/policy/budget identity 不串用，tenant cache namespace 继续隔离。
- versioned router evaluation plan-only validation 在 backend/provider 前 fail fast；正式 deterministic profile 固定 manifest、dataset/sidecar/config/Git identity并输出 per-strategy/per-channel counts。
- C16 required cases 的 missing/unexpected/failed/errors/skipped/unsupported leakage/budget violations 全部显式；真实 provider/model calls=0、business data outbound=false。
- 运行 focused Java tests、风险相称的 Maven suite、Python unit tests、SensitiveLogs、Markdown links、protected paths 与 `git diff --check`；前端无改动时正式 build 记为 `SKIPPED`。

## Approval Gate

本轮只批准启动 C16 规划，不代表批准 Java/Python 实现、修改 runtime config、运行 Docker/Testcontainers、启用 Router、执行 live eval/provider 调用、接受 baseline 或归档。用户需重点确认：

1. Router 默认关闭；启用模式只有 `fact-v1`，非 fact 不走 legacy passthrough，而是稳定 `UNSUPPORTED`。
2. classifier 使用确定性保守 allowlist，不调用任何模型；低覆盖必须如实报告。
3. total deadline 是 admission/inter-stage hard stop，并复用底层 provider timeout，不夸大为可强制取消所有阻塞 I/O。
4. `evidence-no-answer-v1` 将 empty/insufficient evidence、模型拒答、citation 不足与 dependency error 分开；no-answer 不是前置 route 类别。
5. C16 主证据只用 deterministic provider；任何 live router ask/eval 继续单独披露和授权。

提交责任保持 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
