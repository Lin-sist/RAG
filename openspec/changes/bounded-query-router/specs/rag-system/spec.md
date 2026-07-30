# RAG System Delta: C16 Bounded Query Router

## ADDED Requirements

### Requirement: Default-Off Finite Query Router And Classifier

系统 SHALL 提供默认关闭的 bounded query router。`rag.router.enabled=false` 时，现有 REST、SSE、MCP read-only ask、cache 与 provider 选择语义 MUST 保持不变，系统 MUST NOT 因 Router 代码存在而声明 C16 已启用或已形成 strategy attribution。

显式启用时，每个问答请求 SHALL 先由版本化、确定性的 `fact-intent-v1` classifier 形成 immutable route plan。classifier MUST 只输出有限 `FACT / UNSUPPORTED / INVALID` intent 与稳定 reason taxonomy，MUST NOT 调用 embedding、rerank、generation、judge、LLM、数据库或外部服务。首版 strategy registry MUST 只包含 `fact-v1`；ambiguous、multi-hop、global、high-risk 或其他未支持输入 MUST fail closed 为 `UNSUPPORTED`，不得静默进入 legacy 或伪装成 fact。

客户端 query/header/body/MCP arguments/metadata 与评测 annotation MUST NOT 选择或覆盖 classifier、strategy、provider、model、retry、timeout、budget、tenant 或 collection scope。未知 classifier/policy/strategy、无效 budget 或 registry drift MUST 在 retrieval/provider 调用前阻止 Router 启动或当前请求执行。

#### Scenario: Router 默认关闭保持兼容

- GIVEN 使用默认配置启动应用
- WHEN REST、SSE 或 MCP read-only ask 执行现有问题
- THEN 请求沿用 C16 前的 legacy 路径、cache identity 与 provider defaults
- AND 不返回 `fact-v1` 已执行的虚假 attribution，也不产生额外 classifier/provider 调用

#### Scenario: Fact 请求形成唯一 Route Plan

- GIVEN Router 显式启用且输入满足 `fact-intent-v1` 的保守事实查询边界
- WHEN Router 分类请求
- THEN plan 固定 classifier version、`FACT`、effective strategy=`fact-v1`、reason、policy 与 server-owned budget identity
- AND classifier provider/model calls=0，plan 不包含 raw query、tenant/user/KB 或内容

#### Scenario: 未支持或含糊请求 Fail Closed

- GIVEN 输入属于 multi-hop、global、high-risk、开放式规划或无法可靠分类的范围
- WHEN Router 显式启用
- THEN final state 为 `UNSUPPORTED` 且 reason 使用有限 taxonomy
- AND retrieval、rerank、generation、judge 与 provider calls 均为 0，不回退 legacy 或未来策略

#### Scenario: 客户端尝试覆盖 Route 或 Budget

- GIVEN 客户端在 HTTP/MCP 输入或 metadata 中提交 strategy、classifier、budget、provider、model、retry、timeout、tenant 或 collection selector
- WHEN server 构造 route plan
- THEN unknown/reserved selector 被拒绝或忽略为无权输入，effective plan 只来自服务端配置与 identity
- AND 不扩大 tenant scope、不增加预算、不改变 provider 或执行未注册策略

### Requirement: Bounded Fact Strategy Execution And Usage Ledger

`fact-v1` SHALL 复用现有 server-derived tenant vector scope、hybrid retrieval、RRF、rerank、generation 与 citation contract，并通过 immutable `QueryExecutionBudget` 和单请求 `QueryBudgetUsage` 强制执行有限 query variants、最多一次 retrieval orchestration、最多一次 rerank outcome、最多一次 generation、context/output token ceiling 与 overall deadline。预算配置 SHALL 有硬上限并在启动时校验；客户端不得扩大。

每个 stage MUST 在调用前检查余额，在调用后记录实际 usage。预算不足或 deadline 已到时 MUST 停止后续 stage，返回稳定 `BUDGET_EXHAUSTED / DEADLINE_EXCEEDED`；不得自动重放整个 strategy、重复 generation，或把 partial/error 结果写入普通成功 cache/history。overall deadline 只保证 admission 与 stage 间 hard stop，并 SHALL 沿用底层 provider timeout；系统 MUST NOT 宣称能强制抢占所有已开始的阻塞 I/O。

Router enabled 的 `fact-v1` MUST NOT 执行现有 explanatory empty-result 的第二次 retrieval orchestration。Router disabled legacy path MAY 保留该既有行为，但不得把它计为 C16 fact success。

#### Scenario: Fact Strategy 在预算内完成

- GIVEN FACT plan 的 budget 完整且 tenant-scoped dependencies 健康
- WHEN `fact-v1` 完成 retrieval、generation 与 citation
- THEN usage 记录实际 query variants、retrieval/rerank/generation/provider calls、token estimate 与 deadline outcome
- AND retrieval passes<=1、rerank outcomes<=1、generation calls<=1，tenant/provider/prompt/rerank defaults 未被 C16 改写

#### Scenario: Query Variant 或 Stage 预算耗尽

- GIVEN query expansion、retrieval、rerank 或 generation 即将超过 plan ceiling
- WHEN ledger 在调用前检查余额
- THEN 不启动超额调用并返回机器可判定的 budget category
- AND 不继续 fallback、整链 replay、重复 provider call 或缓存 partial result

#### Scenario: Overall Deadline 到期

- GIVEN 请求在进入下一 stage 前已超过 monotonic deadline
- WHEN fact executor 检查 route plan
- THEN 后续 retrieval/rerank/generation/citation stage 不再启动
- AND final state 为 `ERROR/DEADLINE_EXCEEDED`，不伪装成 no-answer 或正常 success

#### Scenario: Legacy Explanatory Fallback 不进入 Fact Strategy

- GIVEN Router enabled 的事实策略首次 retrieval 为空
- WHEN `fact-v1` 评估下一动作
- THEN 直接进入统一 evidence/no-answer policy
- AND 不调用 explanation fallback query，不产生第二次 retrieval orchestration

### Requirement: Unified Evidence And No-Answer Policy

系统 SHALL 使用版本化 `evidence-no-answer-v1` 将 route intent、retrieval evidence、generation outcome、citation validation 与 final state 分开。no-answer MUST NOT 是纯前置 route 类别；classifier 不得因 query 形态直接判定知识库是否有答案。

pre-generation policy SHALL 将空 contexts 或没有 context 满足当前 tenant/minScore/evidence contract 的结果判为 `NO_ANSWER/INSUFFICIENT_EVIDENCE`，且 generation calls=0。retrieval/provider/dependency error SHALL 为 `ERROR`，不得映射为 no-answer。post-generation policy SHALL 将模型拒答标为 `NO_ANSWER/MODEL_REFUSAL`；fact answer 无 validated citation/evidence 时标为 `NO_ANSWER/UNVALIDATED_EVIDENCE`；只有 evidence 与 citation contract 通过时才为 `ANSWER`。

同步、SSE 与 MCP read-only ask SHALL 共享同一 classifier/strategy/policy identity 与 final state 语义。SSE 首版 MAY 不新增 wire metadata event，但 terminal signal、trace 与 integration/evaluation evidence MUST 保存同一低基数 final state/reason；`NO_ANSWER/ERROR/UNSUPPORTED` MUST NOT 被保存为正常成功 history。

#### Scenario: Evidence 不足时不调用 Generation

- GIVEN Router 已将请求路由到 `fact-v1`，但 retrieval 返回空或无合格 evidence
- WHEN pre-generation policy 执行
- THEN final state 为 `NO_ANSWER`、reason=`INSUFFICIENT_EVIDENCE`
- AND generation/provider calls=0，citation 为空，依赖错误数为 0

#### Scenario: Retrieval 依赖失败不是 No-Answer

- GIVEN vector/keyword 或其他必需 retrieval dependency 失败且不存在 accepted degradation evidence
- WHEN policy 形成 final state
- THEN final state 为 `ERROR` 并保留稳定 dependency category
- AND 不返回 `NO_ANSWER/INSUFFICIENT_EVIDENCE`，不调用 generation

#### Scenario: 模型拒答或 Citation 不足

- GIVEN pre-generation evidence 足够且 generation 已实际执行
- WHEN 模型输出拒答，或 fact answer 没有通过 citation validation 的 evidence
- THEN final state 分别为 `NO_ANSWER/MODEL_REFUSAL` 或 `NO_ANSWER/UNVALIDATED_EVIDENCE`
- AND usage 保留实际 generation/citation facts，不把该结果记为 evidence-backed answer

#### Scenario: Sync、SSE 与 MCP Final State 一致

- GIVEN 同一 tenant、KB、query、fixture、router/config identity 和 deterministic providers
- WHEN 通过同步、流式和 C15 read-only ask 执行
- THEN effective strategy、policy、final state、reason 与 bounded usage 语义一致
- AND MCP 仍不写 QA history/query count，SSE 非 ANSWER 结果不保存为正常成功历史

### Requirement: Router Attribution Cache And Telemetry Compatibility

Router enabled 的同步 response/debug/evaluation metadata SHALL 输出固定白名单的 classifier version、requested/effective strategy、route reason、policy version、final state、no-answer reason、budget profile/outcome 与实际 usage。unknown/missing attribution MUST NOT 被写成 `fact-v1` success 或 zero provider calls。

QA cache identity SHALL 绑定 router enabled mode、classifier version、effective strategy/legacy marker、policy version 与影响结果的 budget profile。不同 identity MUST NOT 互相命中；`UNSUPPORTED / INVALID / ERROR / BUDGET_EXHAUSTED` MUST NOT 写入普通成功 cache。tenant-scoped cache namespace 与 C15 read-only side-effect contract SHALL 保持不变。

trace/metrics/logs SHALL 只使用 bounded classifier/strategy/state/reason/budget outcome。query、规则命中片段、tenant/user/KB ID、context、answer、citation snippet、token/credential、provider body、异常 message/stack 与自由文本 strategy/model MUST NOT 成为普通日志或 metric labels。

#### Scenario: Router Metadata 与实际执行一致

- GIVEN `fact-v1` 实际完成或在某个 stage 失败
- WHEN response/debug/eval 构造 attribution
- THEN effective strategy、final state、usage、provider calls 与实际 diagnostics 一致
- AND missing/unknown facts 显式为 unknown/error，不伪造为成功或零调用

#### Scenario: Cache Identity 防止跨策略污染

- GIVEN query、tenant、KB 与普通参数相同，但 router mode、classifier、policy、strategy 或 budget profile 不同
- WHEN 读取或写入 QA cache
- THEN 不同 identity 不命中同一 entry
- AND legacy、fact、unsupported/error 结果不会相互覆盖或复用

#### Scenario: Telemetry 只使用低基数事实

- GIVEN Router success、unsupported、no-answer、budget exhaustion 与 dependency error 请求
- WHEN 写入 trace、metrics 与普通日志
- THEN 只包含 bounded classifier/strategy/state/reason/budget outcome 与允许的计数
- AND 不包含 query、identity、content、provider raw facts、credential、异常 message/stack 或高基数 budget 值

### Requirement: C16 Capability Claim And Advanced-Strategy Boundary

C16 acceptance SHALL 只证明固定 `fact-intent-v1`、`fact-v1`、`evidence-no-answer-v1`、approved budget profile 和版本化 deterministic evaluation 下的 bounded Router contract。C16 completion MUST NOT 自动把 `rag.router.enabled` 改为生产默认开启，也 MUST NOT 宣称 multi-hop、global summary、high-risk/artificial confirmation、tool loop、memory、reflection、所有问题分类准确、真实 provider 质量收益、生产 SLA 或 Agentic RAG 已完成。

未来 `router-multihop`、`router-global`、`router-high-risk`、client-visible SSE route metadata 或 production default activation SHALL 分别经过独立 Type C change、spec/evaluation gate 与所需外部调用授权。C16 MUST NOT 以 placeholder、legacy passthrough、未注册 strategy 或 mock-only evidence 代替这些能力。

#### Scenario: Fact Router 通过后描述能力

- GIVEN C16 deterministic required profile 完整通过并经用户验收
- WHEN 项目描述当前 Router 能力
- THEN 只声明 default-off bounded `fact-v1` 与固定 evidence/no-answer policy 已形成指定证据
- AND 明确列出覆盖率、真实 provider、生产默认、高级策略与 Agentic 能力的未完成边界

#### Scenario: 请求自动开启高级策略或生产默认

- GIVEN C16 fact evidence 已通过
- WHEN 请求启用 multi-hop/global/high-risk、Agent loop 或把 Router 改为生产默认
- THEN 必须建立独立 change 与对应评测/授权
- AND 不得复用 C16 fact PASS、legacy fallback 或空 strategy stub 声称已完成
