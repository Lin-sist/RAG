# Evaluation Delta: C16 Bounded Query Router

## ADDED Requirements

### Requirement: Versioned Router Evaluation Release And Expectation Identity

系统 SHALL 为 C16 维护独立、版本化的 `bounded-query-router-eval-v1` release。manifest SHALL 固定已接受 dataset release identity、ordered selected sample IDs、route expectation sidecar schema/hash/bytes、classifier/strategy/policy/budget profile、evaluator version、required channels、status 与 exit code。route sidecar MUST 只按 sample ID 保存 expected intent/required status，不复制 question、answer、context 或 fixture 内容。

validator MUST 只使用本地 artifact，并在启动 backend/container、读取 credential 或调用 embedding/rerank/generation/judge/provider 前 fail fast。runtime classifier MUST NOT 读取 route expectation sidecar、sample type、expected answer/source/context 或报告；不得为通过评测把生产规则绑定到具体 sample ID、题目或 fixture 文本。

绝对路径、父目录逃逸、duplicate/missing/unexpected sample ID、unknown intent/strategy/policy/channel、hash/order/count drift、dataset identity mismatch、无 sidecar coverage 或 schema 外字段 MUST 使 release 为 `INVALID`，不得复用旧 report 或降级成正式 unversioned evidence。

#### Scenario: 本地 Plan-Only 验证通过

- GIVEN manifest、accepted dataset identity、route sidecar、budget profile 与 evaluator identity完整
- WHEN 运行 plan-only/validate-only
- THEN artifact path/hash/bytes、ordered sample IDs、coverage 与 version identity exact match
- AND backend/container/provider calls=0、business data outbound=false

#### Scenario: Dataset 或 Expectation 漂移

- GIVEN dataset bytes、sample order、sidecar expected route、classifier/policy/budget identity任一漂移
- WHEN 尝试形成正式 C16 evidence
- THEN release 在业务执行前标记为 `INVALID`
- AND 不复用旧 details/summary 或成功子集继续运行

#### Scenario: Evaluation Label 进入 Runtime Classifier

- GIVEN classifier 实现尝试读取 sample ID/type、route sidecar、expected source/answer/context 或 fixture-specific canary
- WHEN 运行 isolation/static/behavior gate
- THEN C16 evaluation 失败
- AND 该规则不得作为生产 classifier 或 acceptance evidence

### Requirement: Independent Classification Strategy And Budget Metrics

C16 report SHALL 独立报告 classification、strategy execution 与 budget 三个通道。classification 至少 SHALL 包含 expected/observed matrix、FACT precision、FACT recall、FACT coverage、unsupported leakage、invalid 与 unexpected count；strategy execution SHALL 包含 expected FACT 中 effective `fact-v1` 数、unsupported/invalid 中 provider calls、unknown/unregistered strategy 与 plan error；budget SHALL 包含每项 ceiling usage、violation、deadline outcome 和实际 provider/model call count。

低 coverage MUST 与高 precision 分开解释；successful fact subset MUST NOT 掩盖大量 ambiguous/unsupported、错误 fact 路由或非 fact provider leakage。任一 required unsupported/invalid case 产生 retrieval/rerank/generation/provider call、任一 unknown strategy、attribution missing 或 budget violation MUST 使对应 channel 非 PASS。

指标与普通报告 MUST 只保存 sample ID、expected/observed enum、bounded reason/status、计数和延迟/usage 数值；MUST NOT 输出 question、answer、context、citation snippet、token、credential、provider body、异常 message/stack 或用户绝对路径。

#### Scenario: Fact Classification 指标完整

- GIVEN 全部 required sample 都有 expected/observed intent 与 stable reason
- WHEN evaluator 聚合 classification
- THEN 输出 confusion counts、FACT precision/recall/coverage 与 unsupported leakage
- AND denominator、missing/unexpected/invalid 数量显式，不以单一 accuracy 掩盖覆盖率

#### Scenario: Unsupported 请求发生 Provider Leakage

- GIVEN expected/observed 为 unsupported 或 invalid 的请求
- WHEN 任一 retrieval/rerank/generation/provider call count 大于 0
- THEN strategy execution channel 为 `FAIL`
- AND 不得以最终响应是 unsupported、no-answer 或 error 来掩盖该外调

#### Scenario: Budget Violation 不能被成功结果掩盖

- GIVEN 请求最终返回 answer，但实际 query variants、retrieval/rerank/generation/provider calls、token 或 deadline 超过 profile ceiling
- WHEN 聚合 budget channel
- THEN 该 case 为 failed 且 channel/global Router status 不得为 PASS
- AND answer quality、retrieval hit 或 citation success 不抵消 budget violation

### Requirement: Per-Strategy Quality And Unified No-Answer Channels

C16 evaluation SHALL 在 effective strategy 维度切分既有 retrieval、generation、citation 与 no-answer metrics，但 MUST NOT 修改它们的公式、denominator、dataset annotation、Report status 或历史 baseline。`fact-v1` 的 routing metrics、retrieval quality、generation/citation quality与 no-answer policy MUST 分开解释。

no-answer SHALL 只在 strategy execution 后按 `evidence-no-answer-v1` final state/reason 评估，MUST NOT 作为 route classifier 的 expected intent。pre-generation insufficient evidence case SHALL 证明 generation calls=0；dependency error MUST 计入 error 而非 no-answer accuracy；post-generation model refusal/citation insufficiency SHALL 保留实际调用与独立 reason。

deterministic provider profile MAY 验证 contract、attribution、budget 与状态语义，但 MUST NOT 被描述为真实生成质量、faithfulness、provider latency/cost 或生产业务收益。`RETRIEVAL_ONLY` 仍不能证明 generation/citation/no-answer 质量，`PARTIAL` 仍不能作为干净 baseline。

#### Scenario: 按 Fact Strategy 切分既有指标

- GIVEN 样本实际 effective strategy=`fact-v1`
- WHEN 计算 retrieval/generation/citation/no-answer 指标
- THEN 沿用既有公式并额外按 strategy 汇总 numerator/denominator/status
- AND 不改写原始 sample annotation、全局 Report status 或历史报告

#### Scenario: Insufficient Evidence 不调用 Generation

- GIVEN `fact-v1` retrieval 后 policy 判定 evidence 不足
- WHEN evaluator 检查 no-answer case
- THEN final state/reason 与 generation calls=0 一致
- AND 该 case 不进入 generation/citation成功 denominator，也不被记为 dependency error

#### Scenario: Dependency Error 与 No-Answer 分离

- GIVEN retrieval/provider dependency 失败
- WHEN evaluator 汇总 final state
- THEN case 进入 error channel且 no-answer accuracy不将其算作正确拒答
- AND 报告保留稳定 error category，不输出 raw exception/message/stack

### Requirement: Fail-Closed Router Status And External-Call Boundary

C16 SHALL 使用独立 Router status `PASS / FAIL / NOT_EVALUABLE / INVALID`，并与既有 `CLEAN / PARTIAL / RETRIEVAL_ONLY / FAILED` Report status并列。只有 release identity、全部 required cases、classification、strategy execution、budget、attribution、适用 quality/no-answer与 error completeness 全部满足 approved profile 时，Router status才可为 `PASS`。

missing、unexpected、failed、errors、required skipped、unknown strategy、attribution missing、unsupported provider leakage 或 budget violation任一非零 MUST 阻止 PASS。基础设施不可用、样本不足或 required channel无法执行 SHALL 为 `NOT_EVALUABLE`；artifact/schema/identity drift SHALL 为 `INVALID`。summary/details/metadata 的 identity、counts、status 与 exit code MUST 一致，正式输出 SHALL no-overwrite。

规划、validator、classifier 与 deterministic主证据 MUST 为真实 embedding/rerank/generation/judge/LLM/provider calls=0、business data outbound=false。任何 live router eval MUST 先披露样本/strategy分布、provider/model、最大调用/尝试、query/context 出站、费用/零费用依据、限流、timeout/retry与 raw artifact 边界，并取得用户授权；未授权时 SHALL 记为 `SKIPPED`，不得以 mock/deterministic结果声称真实质量或成本已验证。

#### Scenario: 全部 Required Channels 通过

- GIVEN versioned release identity完整，全部 required cases已执行且各通道满足 profile
- WHEN evaluator 形成 summary/details/metadata
- THEN Router status=`PASS`、exit code=0，missing/unexpected/failed/errors/skipped/violations均为 0
- AND identity、counts、status、provider calls与数据出站事实三份输出一致

#### Scenario: Required Case 缺失或 Channel 不完整

- GIVEN 存在 missing/unexpected/failed/error/required skip、unknown strategy、attribution missing、provider leakage或budget violation
- WHEN 聚合 global Router status
- THEN status=`FAIL` 或按 identity/infrastructure语义为 `INVALID/NOT_EVALUABLE`
- AND 成功 fact subset、平均质量或旧 report不得掩盖缺口

#### Scenario: Live Evaluation 未获授权

- GIVEN 当前配置可能把 query/context 发送到外部 embedding/rerank/generation/judge provider
- WHEN 尚未完成调用量、模型、出站、费用、限流与重试披露并取得授权
- THEN live C16 evaluation=`SKIPPED` 且不产生真实 provider调用
- AND deterministic contract evidence 不得描述为 hosted endpoint、真实生成质量、成本或生产 SLA 已验证
