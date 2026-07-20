# Evaluation Spec Delta: C7 Reranker A/B Evaluation

## ADDED Requirements

### Requirement: Reranker A/B 可比较身份

正式 reranker A/B comparison SHALL 使用两个独立 retrieval-only arms，并固定 eval-set 内容 hash、selected sample IDs 与顺序、fixture hashes、知识库与文档身份、retrieval 配置、tracked config snapshot、Git HEAD、measured repeat count 和 warm-up policy。comparison MUST 明确区分 strict identity fields 与唯一允许变化的 reranker arm fields；不得仅凭报告文件名、KB 名称或 `enableRerank=true` 判定可比较。

每个 arm SHALL 提供不含 secret 的 runtime rerank manifest，并用逐样本 observed requested/effective provider、model 与 protocol 交叉校验。manifest、metadata 与报告 MUST NOT 包含 API key、Authorization、密码、完整环境变量、raw provider response 或原始异常 message。

#### Scenario: 两个 arm 身份一致

- GIVEN heuristic 与 model arms 的 strict identity fields 完全相同
- AND run/sample pairs 完整且顺序一致
- AND 唯一差异属于批准的 reranker provider/model/protocol/config 白名单
- WHEN 离线 comparator 校验两个 arm
- THEN identity check 通过
- AND comparison 可以继续检查 provider coverage 与指标

#### Scenario: 评测身份发生漂移

- WHEN Git HEAD、eval-set hash、fixture、KB document identity、retrieval config、sample order 或 repeat count 任一不一致
- THEN comparison status 为 `NOT_COMPARABLE`
- AND 输出稳定 `identity_mismatch`、`run_count_mismatch` 或 `sample_pair_mismatch`
- AND 不计算或展示收益 delta

#### Scenario: Manifest 与实际 provider 观察不一致

- GIVEN arm manifest 声明预期 provider/model/protocol
- WHEN 任一样本的 observed attribution 与 manifest 不一致
- THEN comparison status 为 `NOT_COMPARABLE`
- AND 原因包含 `manifest_observation_mismatch`
- AND 报告不输出 secret 或 raw provider 内容

### Requirement: Reranker A/B Provider 覆盖有效性

Reranker A/B SHALL 按 `runIndex + sampleId` 对 observation 配对。`candidateCount>0` 的 observation 为 rerank-eligible；干净 heuristic arm MUST 在全部 eligible observations 使用 requested/effective heuristic、0 fallback 和 0 model calls。干净 model arm MUST 在全部 eligible observations 使用批准的 requested/effective model provider、0 fallback、1 model call 和完整 candidate coverage。

干净 model comparison MUST 达到 100% eligible effective-model coverage。系统 MUST NOT 删除失败样本、只比较成功交集或把 partial fallback 组表述为 model 组。`candidateCount=0` 的 observation SHALL 标为 not-applicable，不进入 model coverage 分母，但同一 pair 在两个 arms 中 MUST 一致。

#### Scenario: Heuristic 与 model arms 均干净

- GIVEN heuristic arm 的全部 eligible observations 均 effective heuristic 且没有 fallback/model call
- AND model arm 的全部 eligible observations 均 effective approved model provider、fallback=0、modelCallCount=1、candidateCoverage=100%
- WHEN comparator 计算 provider coverage
- THEN model coverage 为 100%
- AND comparison 可以进入 `COMPARABLE`

#### Scenario: Model arm 部分 fallback

- GIVEN model arm 至少一个 eligible observation effective provider 为 heuristic 或 fallbackCount 大于 0
- WHEN comparator 校验 coverage
- THEN comparison status 为 `NOT_COMPARABLE`
- AND 输出 fallback count/reason 与受影响 sample pairs
- AND 不对成功子集计算 model 收益

#### Scenario: Zero-candidate pair

- GIVEN 同一 `runIndex + sampleId` 在两个 arms 中 candidateCount 均为 0
- WHEN comparator 计算 model coverage
- THEN 该 pair 标为 not-applicable并保留在固定样本全集
- AND 不进入 eligible coverage 分母
- BUT 若只有一个 arm 为 zero-candidate，comparison 为 `NOT_COMPARABLE`

#### Scenario: 样本缺失或 retrieval 失败

- WHEN 任一 arm 缺少批准的 run/sample pair、存在 retrieve error 或 per-run status 不是 `RETRIEVAL_ONLY`
- THEN comparison status 为 `NOT_COMPARABLE` 或 `FAILED`
- AND 不通过删除该 pair 继续比较

### Requirement: Reranker A/B 指标与延迟口径

仅当 identity 与 provider coverage 均通过时，comparison SHALL 对同一固定样本全集报告 Recall@5、MRR 与 Top1 source accuracy 差异。C7 SHALL 分别记录 server-side rerank stage latency 与 client-observed debug retrieval wall-clock latency，并对每个 arm 报告 observation count、P50 和 P95；两种 latency MUST 分开命名和解释。

P50/P95 SHALL 使用固定、可复现的 percentile 算法并记录 measured repeats、warm-up count 与 observation count。warm-up observations MUST NOT 进入质量或 latency summary。总运行时间除以样本数 MUST NOT 被表述为 P50/P95。

#### Scenario: 生成可比较的质量与延迟结果

- GIVEN comparison status 为 `COMPARABLE`
- AND 两个 arms 的 measured observations 完整
- WHEN comparator 生成结果
- THEN 报告包含 Recall@5、MRR、Top1 的 arm values 与 deltas
- AND 分别包含 rerank latency 与 retrieval latency 的 count/P50/P95
- AND 保留逐样本配对事实以便审计

#### Scenario: Warm-up 与 measured observations 分离

- GIVEN arm 配置了 warm-up calls 和 measured repeats
- WHEN 计算质量与 latency summary
- THEN warm-up calls 单独计数且不进入指标
- AND measured observation count 与批准的 run/sample pairs 一致

### Requirement: Reranker A/B 结论边界

C7 reranker A/B SHALL 以 retrieval-only 方式运行，MUST NOT 因本 comparison 宣称 generation、citation、no-answer 或 judge 质量已经改善。per-run report status SHALL 继续使用既有 `RETRIEVAL_ONLY` 语义；A/B 可比性 SHALL 使用独立 `COMPARABLE / NOT_COMPARABLE / FAILED` 状态。

观察到的 model 收益只适用于报告固定的 eval-set、fixture、KB、配置、provider/model 与 Git HEAD。C7 MUST NOT 自动修改默认 reranker、自动建立通用质量退出码门禁，或把单次/partial/fallback evidence 外推为生产 SLA。

#### Scenario: 干净 retrieval-only comparison

- GIVEN 两个 arms 均为 `RETRIEVAL_ONLY`
- AND comparison status 为 `COMPARABLE`
- WHEN 报告展示观察结论
- THEN 结论限定为当前固定身份下的 retrieval 质量与 latency
- AND ask、judge、LLM generation 调用量为 0
- AND 不宣称 generation/citation/faithfulness 收益

#### Scenario: Model 指标优于 heuristic

- GIVEN comparison status 为 `COMPARABLE`
- AND model arm 的一个或多个 retrieval 指标优于 heuristic
- WHEN change 输出建议
- THEN 可以报告观察到的 delta 与适用身份
- BUT 默认 provider 保持不变
- AND 是否切换默认 provider或建立质量门禁须另行明确决策

