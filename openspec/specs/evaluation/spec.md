# Evaluation Specification

## Requirements

### Requirement: 可复现评测身份

正式 baseline SHALL 固定评测集、fixture、知识库身份、配置快照和 Git HEAD。

#### Scenario: 复跑 baseline

- GIVEN 相同 fixture、配置和代码
- WHEN 连续运行可复现评测
- THEN 报告记录相同的评测身份信息
- AND 客观 retrieval 指标应可直接比较

### Requirement: 只读复用语义

`--preflight-only` MUST 只检查现有资源；`--keep-existing` MUST 只复用现有知识库。目标不存在时 MUST 失败，不得隐式创建空知识库。

#### Scenario: 目标知识库缺失

- WHEN 使用 `--keep-existing` 指向不存在的固定知识库
- THEN runner 非零退出并给出恢复提示
- AND 不创建知识库或上传 fixture

### Requirement: 报告状态

评测报告 SHALL 区分 `CLEAN`、`PARTIAL`、`RETRIEVAL_ONLY` 和 `FAILED`，并记录 retrieve/ask/judge error、retry、rate limit 和 skipped 项。

#### Scenario: Ask 部分失败

- WHEN retrieval 完整但部分 ask 失败
- THEN 报告状态为 `PARTIAL`
- AND generation/citation 指标不得被表述为干净 baseline

### Requirement: 指标边界

Retrieval、generation、citation、no-answer 与 LLM judge 指标 MUST 分开解释。Citation snippet hit MUST NOT 被当作逐 claim faithfulness 的替代。

#### Scenario: Judge 关闭

- GIVEN `judge-mode=off`
- WHEN 评测完成
- THEN 客观指标可以报告
- BUT 不得宣称独立 faithfulness/relevance judge 已完成

### Requirement: 外部调用安全闸

批量 ask、judge、embedding 或 rerank 调用前 MUST 获得用户授权，并说明预计调用量、数据出站、模型、费用和限流风险。

#### Scenario: 未获授权

- WHEN 任务需要新的批量外部调用但尚未获得授权
- THEN 只允许 plan、静态检查、单元测试或不出站的预检

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

### Requirement: 版本化评测数据 Release 身份

正式 evaluation baseline SHALL 绑定一个 tracked dataset release manifest。manifest MUST 声明不可变 `releaseVersion`、question set version、sample schema version、annotation version 和 fixture corpus version，并固定 question set 的 repo-relative path、raw SHA-256、bytes、sample count 与有序 sample identity，以及 sample schema 和每份 fixture 的 repo-relative path、raw SHA-256 与 bytes。

manifest SHALL 声明逻辑知识库 name/marker/expected document names，但 MUST NOT 把绝对本机路径、secret、numeric KB ID、vector collection、运行时 provider 响应或 Git HEAD 写成静态 release 内容。正式 run metadata MUST 同时记录 manifest hash/version、实际 Git HEAD、tracked config snapshot 和 observed KB/document identity，组成完整 evaluation identity。

同一 `releaseVersion` MUST NOT 解析为不同 manifest、question set、schema、annotation 或 fixture identity。所有 manifest artifact path MUST 为受限 repo-relative path，MUST NOT 通过绝对路径或 `..` 解析到仓库外。

#### Scenario: 正式 release 身份完整且匹配

- GIVEN tracked manifest 的所有 version 与 artifact identity 完整
- AND question set、sample schema 与 fixture bytes/hash/count/order均匹配
- WHEN runner 构建正式 evaluation plan 或 run metadata
- THEN metadata 记录 dataset release version、manifest hash 和各组成 identity
- AND 再绑定实际 Git/config/KB/document observation
- AND 该 run 可以继续进入后续 preflight 或 evaluation

#### Scenario: Artifact 内容或路径发生漂移

- WHEN question set、sample schema 或任一 fixture 的 path、bytes、SHA-256、sample count 或 ordered sample identity 与 manifest 不一致
- OR manifest path 为绝对路径、包含越界 `..` 或解析到 repo root 外
- THEN 本地 validation 非零失败
- AND 输出稳定 drift/path error code
- AND 不形成正式 release identity

#### Scenario: 同一 Release Version 指向不同身份

- GIVEN 已声明的 `releaseVersion`
- WHEN其 manifest 或任一组成 artifact identity 与该 version 的 tracked 定义不同
- THEN validation 以 `release_identity_mismatch` 失败
- AND 不允许通过保留同一 version 覆盖旧 release

### Requirement: 样本 Schema 与标注语义校验

每个 versioned eval sample SHALL 遵循 manifest 指向的机器可读 sample schema contract。schema MUST 固定 allowed/required fields、field types、enum、ID uniqueness 和 conditional annotation rules；unknown field、missing field、invalid type/enum 或 duplicate ID MUST fail closed，而不得被静默忽略或通过默认值修复。

首版 contract SHALL 区分 answerable 与 no-answer 样本。answerable 样本 MUST 具有非空 expected source、keyword、answer point 和 context；no-answer 样本 MUST 同时满足 `type=no_answer`、`should_answer=false`、expected sources/keywords/contexts 为空且 refusal expectation 非空。expected source 和 context source MUST 解析到 release manifest 中的 fixture corpus，不得引用未版本化文件。

validation error MUST 只包含稳定 error code、repo-relative artifact、line/sample ID 与 field 等安全诊断；MUST NOT 回显 question、notes、expected content、fixture 正文、secret 或绝对本机路径。

#### Scenario: 当前 Answerable 样本通过校验

- GIVEN sample 包含 schema v1 的全部 required fields
- AND type/difficulty/field types/ID 均有效且唯一
- AND expected sources、keywords、answer points、contexts 非空并引用 manifest fixture
- WHEN validator 校验 sample
- THEN sample 被接受
- AND validation 不修改其内容或标注

#### Scenario: 当前 No-answer 样本通过校验

- GIVEN sample 的 `type=no_answer` 且 `should_answer=false`
- AND expected sources、keywords、contexts 为空
- AND refusal expectation 非空
- WHEN validator 校验 sample
- THEN sample 被接受为 no-answer annotation
- AND 不把空 source/context 当作缺失错误

#### Scenario: 字段或条件语义非法

- WHEN sample 缺 required field、包含 unknown field、field type/enum 非法或 ID 重复
- OR type、should_answer 与 expected annotation 的条件语义冲突
- THEN validation 非零失败
- AND 输出稳定 schema/annotation error code
- AND 不使用默认值或删除字段继续评测

#### Scenario: 标注引用未版本化 Fixture

- WHEN answerable sample 的 expected source 或 context source 不在 manifest fixture corpus
- THEN validation 以 `unknown_fixture_source` 失败
- AND 不把同名仓库外文件或 runtime KB 文档当作隐式替代

### Requirement: 数据校验先于 Backend 与外部调用

Direct eval runner、reproducible eval runner 的 plan、preflight 和实际 run SHALL 复用同一 dataset contract validator。完整本地 manifest/schema/question/fixture validation MUST 发生在 login、backend preflight、KB create/upload/mutation、embedding、rerank、ask、judge 或其他 provider 调用之前。

invalid 或 drifted release MUST 非零退出，并保证本次执行 backend/provider call count 为 0、数据出站为 0。`--preflight-only` 仍 MUST 只检查现有资源，`--keep-existing` 仍 MUST 只复用；dataset manifest MUST NOT 恢复隐式建库、上传或覆盖 report 的行为。

#### Scenario: Invalid Release 在本地 Fail-fast

- GIVEN manifest、schema、question set 或 fixture validation 失败
- WHEN从 direct 或 reproducible runner 启动 plan、preflight 或 run
- THEN runner 在任何 login/backend/provider 调用前非零退出
- AND backend/provider call count 为 0
- AND 数据出站为 0

#### Scenario: Plan-only 校验有效 Release

- GIVEN完整匹配的 versioned release
- WHEN运行不联系 backend 的 plan-only 入口
- THEN plan 报告 release version、sample/fixture count、hash 与预计业务调用量
- AND 实际业务调用量为 0

#### Scenario: 既有只读与复用语义保持不变

- GIVEN本地 dataset validation 通过
- WHEN运行 `--preflight-only` 或 `--keep-existing`
- THEN `--preflight-only` 不创建、上传、删除或重建资源
- AND `--keep-existing` 在目标不存在或 fixture 不匹配时失败
- AND 不因 manifest 存在而隐式建库或上传

### Requirement: 数据版本演进与历史证据边界

Dataset governance SHALL 分别表达 question set、sample schema、annotation 和 fixture corpus 的变化，并由新的 release version 聚合。样本 membership/order/ID/question text 变化 MUST 产生新的 question set version 与 release version；字段/类型/enum/条件规则变化 MUST 产生新的 sample schema version 与 release version；expected annotations 变化 MUST 产生新的 annotation version 与 release version；fixture 集合或 bytes 变化 MUST 产生新的 fixture corpus version 与 release version。

历史报告 MUST 保留其原始 hash、metadata 和当时的 validation 能力边界，MUST NOT 被回写或追认为通过后来引入的 schema/release validator。未版本化 custom eval-set 若被允许用于本地诊断，MUST 显式标记为 `UNVERSIONED`，MUST NOT 形成正式 baseline、可比较结论或质量门禁输入。

C8a 首个 release SHALL 固定当前已有 question set 与 fixture bytes，不得为了通过 schema 修改题目、标注、prompt、chunking、retrieval、rerank、citation、no-answer 或 judge 行为。数据扩充与分类配额 MUST 由后续独立 change 产生新的 release。

#### Scenario: 标注变化产生新 Version

- GIVEN question text/order 与 fixture corpus 不变
- WHEN expected source、keyword、answer point、context、should_answer、type/difficulty 或 notes 发生审核变更
- THEN annotation version 与 release version 必须变化
- AND旧 release 继续可按原 hash 复现

#### Scenario: 历史报告不被追认改写

- GIVEN C8a 之前的报告记录了原始 eval-set/fixture/config/Git identity
- WHEN C8a 引入新的 release/schema validator
- THEN历史报告保持原文件与原结论边界
- AND 不写入一个当时未实际验证的 releaseVersion 或 validation status

#### Scenario: 未版本化 Custom Eval 不得形成 Baseline

- GIVEN custom eval-set 没有匹配的 versioned manifest
- WHEN用户显式选择本地诊断模式
- THEN metadata/report 标记 `UNVERSIONED`
- AND 不输出正式 baseline/comparable/pass gate 结论
- BUT 默认正式运行路径仍应拒绝未版本化输入

### Requirement: Expanded Dataset Composition And Quotas

C8b expanded development release SHALL contain exactly 150 samples: the immutable 30-sample C8a seed followed by 120 new samples. Its type quota SHALL be fact 35、definition 30、reasoning 40、multi_hop 25、no_answer 20；difficulty quota SHALL be easy 50、medium 65、hard 35；answerability quota SHALL be answerable 130、no-answer 20。

The release SHALL satisfy the approved type×difficulty matrix rather than only its marginal totals. For answerable samples, each tracked fixture SHALL be referenced by at least 35 samples through expected source or expected context, and no single fixture SHALL be referenced by more than 45% of answerable samples. Multi-source samples MAY count toward multiple fixture coverage totals.

Quota SHALL be defined before authoring and MUST NOT be changed to improve the observed performance of a provider, retrieval configuration, prompt or metric implementation. Failure to meet any exact total or coverage boundary MUST fail local validation and MUST NOT form the expanded formal release identity.

#### Scenario: Expanded release 满足全部配额

- GIVEN v2 question set 含 150 条且前 30 条为已固定 seed
- AND type、difficulty、type×difficulty、answerability 与 fixture coverage 均匹配批准配额
- WHEN validator 构建 expanded release identity
- THEN quota validation 为 `VALID`
- AND identity 记录总量、seed/new count 与各配额事实

#### Scenario: 只满足边际总量但交叉矩阵漂移

- GIVEN type 总量和 difficulty 总量分别正确
- WHEN 任一 type×difficulty cell 与批准矩阵不一致
- THEN validation 以 `quota_mismatch` 非零失败
- AND 不通过移动其他 cell 的样本掩盖该漂移

#### Scenario: Fixture coverage 失衡

- WHEN 任一 fixture 的 answerable coverage 低于 35
- OR 任一 fixture 覆盖超过 answerable 样本的 45%
- THEN validation 以 `fixture_coverage_mismatch` 失败
- AND 不把 multi-source 引用静默改算为单一来源以通过门禁

### Requirement: Seed Immutability And Multi-release Compatibility

The C8a 30-sample seed SHALL remain identical in object content, annotation and order inside the C8b release. New samples MUST be appended after the seed and MUST use unique schema-valid IDs that do not replace or renumber seed IDs. Any seed object/order/ID/annotation drift MUST fail with `seed_identity_mismatch`.

C8b SHALL preserve an explicitly validatable `rag-eval-dev-v1` manifest and question set while creating a separate v2 question set and manifest. V2 MUST use new `releaseVersion`、`questionSetVersion` and `annotationVersion`. `sampleSchemaVersion` and `fixtureCorpusVersion` SHALL change only when their actual contract or corpus changes.

The default dataset manifest MUST remain on v1 until v2 passes all quota、grounding、review、identity and regression gates and receives user acceptance. Switching the default MUST NOT overwrite or make v1 unresolvable.

#### Scenario: Seed 原样保留并只追加新样本

- GIVEN v1 seed 的 30 个 ordered objects
- WHEN validator 检查 v2 前 30 条
- THEN object content、annotation、ID 与 order 全部相同
- AND新增样本只出现在 seed 之后

#### Scenario: Seed 被改写或重排

- WHEN v2 修改任一 seed question、annotation、ID 或 object order
- THEN validation 以 `seed_identity_mismatch` 失败
- AND 不把该变化计作普通 annotation bump 继续发布

#### Scenario: V1 与 V2 可同时验证

- GIVEN v2 已完成但尚未或已经切换为默认 release
- WHEN分别选择 v1 与 v2 manifest 做本地 validation
- THEN 两个 release 都能按自身 path/hash/version 独立得到 `VALID`
- AND v2 不复用 v1 release/question/annotation version 覆盖不同 identity

### Requirement: Grounded Annotation And Review Evidence

Every new answerable sample SHALL be authored source-first from the tracked fixture corpus. Each expected context `contains` value MUST resolve by exact or deterministic whitespace-normalized exact match within its declared fixture. Expected sources and context sources MUST belong to the release corpus; semantic correctness MUST NOT rely on provider memory or untracked external facts.

Every new multi_hop sample SHALL combine at least two independently reviewable evidence points. Every no_answer sample SHALL keep expected sources、keywords and contexts empty and SHALL have review evidence that the entire fixed fixture corpus lacks the key evidence required to answer.

The expanded release SHALL bind a machine-readable review artifact by repo-relative path、raw SHA-256、bytes and reviewed sample count. Review records SHALL cover every release sample by sample ID and SHALL record structure、grounding、duplicate and semantic review status without duplicating full question、expected content or fixture text.

Normalized exact duplicate questions MUST fail closed. Near-duplicate candidates above the approved deterministic threshold MUST have an explicit `accepted_distinct` or `rejected_duplicate` review result before release validation can pass.

#### Scenario: Answerable annotation 可回连 Fixture

- GIVEN新 answerable sample 的 source 和 context 均属于 manifest corpus
- AND每个 expected context 可在对应 fixture 中确定性定位
- WHEN grounding validator 检查样本
- THEN grounding status 为 `VALID`
- AND校验过程不调用 embedding、LLM 或其他 provider

#### Scenario: Context 无法定位或引用未跟踪来源

- WHEN expected context 无法在声明 fixture 中 exact/normalized-exact 命中
- OR source 不属于 release fixture corpus
- THEN validation 以稳定 grounding/source error code 失败
- AND错误不回显 context、question 或 fixture 正文

#### Scenario: Multi-hop 与 No-answer 语义不完整

- WHEN multi_hop 少于两个独立 evidence points
- OR no_answer 引用了 source/context 或缺少全 corpus 拒答复核
- THEN validation 非零失败
- AND不通过修改 type 或默认填充 annotation 继续发布

#### Scenario: Review 或重复处理不完整

- WHEN任一样本缺 review record、sidecar identity 漂移或 normalized exact question 重复
- OR near-duplicate candidate 没有明确复核结论
- THEN validation 非零失败
- AND不形成正式 v2 release identity

### Requirement: Dataset Freeze And Evaluation Boundary

C8b authoring、review and validation SHALL complete before any result is treated as an expanded baseline. Once v2 is frozen, question、annotation、quota、review or corpus corrections MUST create the corresponding new version and release; they MUST NOT be edited in place after observing provider or algorithm performance.

C8b MUST NOT change retrieval、chunking、rerank、prompt、citation、no-answer、judge metric formulas or production defaults. C8b acceptance SHALL mean only that expanded development data and annotations satisfy the approved governance contract; it MUST NOT imply improved retrieval/generation quality、production representativeness、a hidden benchmark、C9 claim/judge completion、C10 quality gates or C14 isolation evaluation.

External LLM/provider assistance for bulk authoring or review MUST remain disabled unless separately authorized with call volume、egress content、model、cost and rate-limit risk. Without that authorization, planning and implementation SHALL use zero embedding、rerank、ask、judge or external LLM/provider calls.

#### Scenario: 数据冻结前只做本地构建与校验

- GIVEN C8b 尚在 authoring 或 review 阶段
- WHEN执行规划、样本构建或 acceptance checks
- THEN只运行本地静态/单元/plan-only 验证
- AND真实 embedding、rerank、ask、judge、LLM/provider 调用量与数据出站均为 0

#### Scenario: 观察结果后试图原地改题或标注

- GIVEN v2 已冻结或已产生正式 run evidence
- WHEN question、annotation、quota、review 或 fixture 需要修订
- THEN必须按 C8a bump matrix 创建后续新 version/release
- AND不得覆盖 v2 后继续把前后结果表述为同一 baseline

### Requirement: Deterministic Answer Claim Extraction

C9a SHALL extract ordered claim units from successful, non-empty answerable generation outputs using a tracked deterministic splitter. The splitter SHALL use paragraph/list boundaries and Chinese/English sentence-ending punctuation without calling an embedding、LLM、judge or other external provider. It MUST preserve the visible claim text and order, assign stable per-output indexes, and record splitter/version identity in run metadata.

Claim extraction SHALL NOT use expected answer points as a substitute for generated claims and SHALL NOT semantically rewrite、merge or summarize the generated answer. No-answer samples MUST remain in the independent no-answer metric channel and MUST NOT enter the objective claim-support denominator.

#### Scenario: Answerable output is split reproducibly

- GIVEN a successful answerable ask contains paragraphs、sentences or list items
- WHEN the C9a splitter runs repeatedly with the same algorithm version
- THEN it returns the same ordered claim units、indexes and hashes
- AND no provider or backend call is made by claim extraction

#### Scenario: Empty and structural-only fragments

- GIVEN an answer contains blank text、pure numbering、punctuation、heading markers or isolated citation markers
- WHEN claim extraction runs
- THEN those structural-only fragments do not become claims
- BUT a non-empty eligible answer that yields zero claims records `empty_claim_set` and a partial claim metric status

#### Scenario: No-answer remains separate

- GIVEN a sample has `should_answer=false`
- WHEN generation/no-answer metrics are evaluated
- THEN its refusal remains governed by no-answer accuracy and citation-violation metrics
- AND refusal text does not enter the C9a claim-support numerator or denominator

### Requirement: Validated Citation Evidence And Objective Lexical Alignment

C9a claim evidence SHALL be limited to returned citations that first pass the existing citation identity and snippet-to-returned-context provenance checks. Unvalidated citations and retrieved contexts that were not explicitly represented by a validated returned citation MUST NOT be used to support a claim.

Each claim SHALL be classified as `exact`、`token_overlap` or `unsupported`. `exact` requires deterministic normalized containment. `token_overlap` SHALL use the tracked ASCII-token/CJK-bigram tokenizer with claim tokens as denominator、a fixed `0.70` minimum coverage and at least 2 claim tokens. A claim with no eligible evidence、insufficient comparable tokens or coverage below threshold MUST remain in the denominator as `unsupported` with a stable reason.

The report SHALL name this result objective/lexical alignment. It MUST NOT describe the metric as semantic entailment、ground truth、complete factual correctness or independent faithfulness.

#### Scenario: Claim matches validated evidence

- GIVEN a claim and at least one returned citation that passes provenance validation
- WHEN the claim is contained by the eligible evidence or reaches the tracked token coverage threshold
- THEN the claim is supported as `exact` or `token_overlap`
- AND details record the deterministic best evidence、method、coverage and algorithm identity

#### Scenario: Citation fails provenance validation

- GIVEN a returned citation cannot be matched to its returned context or its snippet lacks provenance support
- WHEN C9a builds eligible evidence
- THEN that citation is excluded from claim matching
- AND any claim without another eligible match is counted as `unsupported`

#### Scenario: Claim has no sufficient lexical support

- GIVEN a claim has no eligible citation、fewer than 2 comparable tokens or best coverage below `0.70`
- WHEN objective alignment is aggregated
- THEN the claim stays in the denominator as `unsupported`
- AND the result exposes a stable reason without raw exception or secret content

### Requirement: Claim Metric Denominator And Local Status

The objective claim support rate SHALL equal supported extracted answerable claims divided by all successfully extracted answerable claims. Claims without citations or matches MUST NOT be removed from the denominator. Per-sample and aggregate outputs SHALL expose claim total、supported/unsupported count、exact/token count、support rate、eligible evidence count and completeness/error counts.

C9a SHALL expose an independent `COMPLETE / PARTIAL / SKIPPED / NOT_APPLICABLE` claim metric status. Ask failures、empty eligible answers or extraction failures SHALL make the relevant claim channel partial rather than silently shrinking the sample set. Retrieval-only execution SHALL be skipped; an evaluated selection with no answerable samples SHALL be not applicable.

C9a MUST NOT change the existing global `CLEAN / PARTIAL / RETRIEVAL_ONLY / FAILED` Report status or judge status semantics. Objective/judge global status separation and judge calibration remain C9b scope.

#### Scenario: Complete claim metrics

- GIVEN every selected answerable sample has a successful non-empty ask and complete claim alignment
- WHEN results are aggregated
- THEN claim metric status is `COMPLETE`
- AND support rate uses every extracted claim including unsupported claims

#### Scenario: Ask or extraction is incomplete

- GIVEN at least one selected answerable sample has ask failure、empty answer or extraction failure
- WHEN claim results are aggregated
- THEN claim metric status is `PARTIAL`
- AND the report exposes affected sample counts without claiming a complete denominator

#### Scenario: Retrieval-only or no answerable samples

- GIVEN ask is skipped OR the evaluated selection contains no answerable samples
- WHEN C9a status is produced
- THEN status is respectively `SKIPPED` or `NOT_APPLICABLE`
- AND missing claim metrics are not reported as zero support

### Requirement: Algorithm Identity Compatibility And Safety Boundary

C9a reports and details SHALL record claim metric、splitter、tokenizer、threshold、minimum-token and evidence-policy identity. Results with differing C9a identities MUST NOT be presented as directly comparable. Historical reports without C9a fields MUST remain readable and SHALL be interpreted as “claim metric unavailable”, not as zero.

C9a changes SHALL be additive and MUST NOT modify dataset release/question/annotation/fixture identity、existing retrieval/generation/citation/no-answer/judge formulas、production prompt/citation behavior or default provider. Aggregate and ordinary output MUST NOT add raw question、answer、claim、citation snippet、context、secret、Authorization or absolute local path. Per-sample raw details MAY retain claim text only under the existing local raw-evidence boundary.

C9a planning and offline implementation SHALL use zero real embedding、rerank、ask、judge、LLM/provider calls and zero data egress. Any formal generation evidence run MUST receive separate authorization after disclosing call bounds、provider/model、egress、cost、rate limit、timeout/retry and raw artifact handling.

#### Scenario: Algorithm identity differs

- GIVEN two reports use different splitter、tokenizer、threshold or evidence-policy identity
- WHEN a consumer attempts to compare objective claim support
- THEN the reports are marked not directly comparable for C9a
- AND no delta is inferred from the shared dataset name alone

#### Scenario: Historical report lacks C9a fields

- GIVEN a pre-C9a report remains stored with its original content
- WHEN current tooling reads that report
- THEN existing metrics remain readable
- AND claim metrics are unavailable rather than backfilled or treated as zero

#### Scenario: No external-call authorization

- GIVEN planning or offline implementation is underway without separate live-run authorization
- WHEN C9a code and tests are executed
- THEN only local deterministic fixtures、static checks and unit tests are used
- AND real embedding、rerank、ask、judge、LLM/provider calls and data egress remain zero

### Requirement: Versioned Judge Contract And Calibration Corpus

C9b SHALL define a tracked judge contract identity covering rubric/prompt version and hash、strict parser version、faithfulness/relevance score thresholds、joint pass rule、context truncation policy、provider/model、temperature and protocol-relevant endpoint identity. API keys、Authorization、raw provider responses and secret-bearing configuration MUST NOT enter this identity or ordinary output.

Judge calibration SHALL use a versioned corpus independent from the normal evaluation dataset release. The initial corpus SHALL contain 24 human-reviewed static cases with exact quotas of 6 cases in each `faithful × relevant` boolean quadrant. Each case SHALL bind a synthetic question/answer to contexts resolved deterministically from tracked fixtures and SHALL record separate faithfulness/relevance gold labels、a derived joint label、review status and stable identity.

Calibration manifest/schema/case path、hash、bytes、count、order、quadrant quota、fixture grounding、gold consistency and review completeness MUST validate locally before any judge/provider call. Calibration cases MUST NOT modify or enter the denominators of `rag-eval-dev-v1/v2`.

#### Scenario: Calibration corpus is valid

- GIVEN the tracked calibration manifest、24 ordered cases、schema、fixture identities、quadrant quotas and review records all match
- WHEN the local calibration validator runs
- THEN validation status is `VALID`
- AND the plan binds the exact calibration and judge contract identities before estimating or making calls

#### Scenario: Calibration artifact drifts

- WHEN case content/order、manifest hash/count、quadrant quota、fixture grounding、gold relation or review status differs from the tracked contract
- THEN validation fails with a stable safe error code before any provider call
- AND the tool does not silently repair、drop or replace the case

#### Scenario: Normal dataset remains independent

- GIVEN calibration v1 is added or later versioned
- WHEN normal v1/v2 dataset identity is validated
- THEN question、annotation、review、fixture and default manifest identities remain unchanged
- AND calibration cases do not enter retrieval/generation/objective metric denominators

### Requirement: Strict Judge Parsing And Calibration Evidence

C9b SHALL accept a judge observation only when both `faithfulnessScore` and `relevanceScore` are present as finite JSON numbers within `[0,1]`. Missing、string、non-finite or out-of-range scores and invalid JSON/schema MUST fail closed as `invalid_judge_payload`; values MUST NOT be clamped、guessed from prose or included in quality denominators.

Normative `judgePass` SHALL be derived deterministically from both validated scores and the tracked thresholds. A provider-reported pass MAY be retained as a diagnostic boolean, but MUST NOT override the derived result; disagreement SHALL be counted explicitly.

The calibration runner SHALL support a fixed 4-case/1-repeat canary and a fixed 24-case/3-repeat full run. It SHALL preserve every expected case/repeat observation and report parse coverage、faithfulness/relevance/joint confusion and agreement、provider-pass disagreement and per-case repeat consistency. Missing or failed observations MUST make calibration evidence partial/not comparable and MUST NOT be removed to calculate a successful subset. C9b MUST NOT automatically optimize thresholds or define a production quality gate.

#### Scenario: Judge payload is valid

- GIVEN a response contains both numeric scores in range under the tracked parser contract
- WHEN the observation is evaluated
- THEN normative pass is derived from the tracked thresholds
- AND the observation enters dimension/joint agreement and repeat metrics

#### Scenario: Judge payload is invalid or incomplete

- WHEN JSON/schema is invalid OR either score is missing、non-numeric、non-finite or out of range
- THEN the observation records `invalid_judge_payload` without clamping or inference
- AND coverage/status reflects the failure while quality metrics exclude only the invalid value, not the expected observation

#### Scenario: Full calibration has a missing repeat

- GIVEN full calibration requires every approved case at each of three repeat indexes
- WHEN any call、parse result or case/repeat identity is missing
- THEN calibration status is `PARTIAL` or `NOT_COMPARABLE`
- AND no agreement conclusion is produced from a pruned successful subset

### Requirement: Objective Judge And Global Status Separation

Normal evaluation SHALL expose independent `objectiveMetricStatus` and `judgeMetricStatus` in addition to the existing global `Report status`. Objective status SHALL depend only on login、retrieval、ask、generation/citation/no-answer and applicable C9a objective completeness; judge errors or judge quality scores MUST NOT change objective values or objective completeness.

Judge status SHALL be `SKIPPED` when judge is disabled、`NOT_APPLICABLE` when enabled but no answerable ask result is eligible、`PARTIAL` when any eligible judge call/parse/coverage is incomplete, and `COMPLETE` only when every eligible sample has a valid judge observation. Judge pass rate or score magnitude MUST NOT determine completeness status.

Global status SHALL remain `CLEAN / PARTIAL / RETRIEVAL_ONLY / FAILED`. Objective failure/retrieval-only/partial SHALL retain precedence. When objective status is complete but explicitly enabled judge status is partial, global status MUST be `PARTIAL`; judge skipped/not-applicable MUST NOT prevent an otherwise complete objective run from being `CLEAN`.

#### Scenario: All judge calls fail after complete objective evaluation

- GIVEN retrieval、ask and applicable objective metrics are complete
- AND judge is explicitly enabled but every eligible judge observation fails
- WHEN statuses are aggregated
- THEN objective status is `COMPLETE`、judge status is `PARTIAL` and global status is `PARTIAL`
- AND objective metrics remain independently eligible for comparison under matching objective identity

#### Scenario: Judge is disabled

- GIVEN objective evaluation is complete and `judge-mode=off`
- WHEN statuses are aggregated
- THEN judge status is `SKIPPED` and global status may be `CLEAN`
- BUT the report does not claim calibrated faithfulness/relevance evidence

#### Scenario: Judge quality is low but coverage is complete

- GIVEN every eligible judge observation is schema-valid but pass/agreement scores are low
- WHEN statuses are aggregated
- THEN judge status is `COMPLETE`
- AND low quality remains a metric result rather than being mislabeled as incomplete execution

### Requirement: Per-channel Comparison Safety Compatibility And External-call Boundary

C9b reports/details SHALL expose structured per-channel comparison safety for objective and judge metrics. Judge partial/skipped/not-applicable MUST NOT downgrade a complete objective channel to retrieval-only. Judge metrics SHALL be comparison-eligible only when judge status is complete and compared reports match calibration、prompt/parser/threshold/provider/model/temperature/context identities. Historical reports lacking C9b fields MUST remain readable and SHALL treat C9b channel identity/status as unavailable rather than zero or inferred.

Direct、reproducible and calibration runners SHALL reuse one judge contract implementation. C9b MUST NOT change production QA、default judge mode、dataset release、C9a objective formulas、no-answer policy or C10 thresholds/exit gates. Aggregate and ordinary output MUST NOT add raw question、answer、context、reason、provider body、secret、Authorization or absolute local path.

Planning and offline implementation SHALL use zero real embedding、rerank、debug retrieval、ask、generation、judge or other provider calls and zero data egress. Live calibration MUST receive separate authorization after disclosing provider/model、a maximum of 4 canary plus 72 full judge calls、outbound tracked calibration content、cost、rate limit、timeout/retry and raw artifact handling.

#### Scenario: Judge partial but objective complete

- GIVEN global status is partial only because the judge channel is incomplete
- WHEN comparison safety is reported
- THEN objective channel remains eligible under matching objective identity
- AND judge channel is not eligible without mislabeling objective metrics as retrieval-only

#### Scenario: Historical report lacks C9b fields

- GIVEN a pre-C9b report remains stored with its original schema
- WHEN current tooling reads it
- THEN existing metrics remain readable
- AND judge contract、channel statuses and comparison safety are unavailable rather than backfilled

#### Scenario: Live calibration is not authorized

- GIVEN planning or offline implementation is underway without separate live-call authorization
- WHEN C9b tooling and tests run
- THEN only local fixtures、synthetic responses、static validation and plan-only paths execute
- AND real judge/provider calls、data egress and provider cost remain zero

### Requirement: Versioned Quality Gate Profile And Compatibility Identity

C10 SHALL define a tracked `rag-quality-gate-profile-v1` contract that binds profile id/version/status、dataset release and manifest identity、sample selection、run/metric identity、required metric channels、fixed slices、thresholds、tolerances、minimum denominators、missing/error policy and profile identity. Profile validation MUST complete before any quality result is produced.

Only an `ACTIVE` profile with a `VALID` versioned dataset and compatible evidence MAY produce `PASS`. A `DRAFT` profile MAY be validated and exercised with synthetic evidence but MUST NOT be represented as an active quality gate. Unversioned、historical identity-incomplete or selection-incomplete evidence MUST NOT be retroactively accepted by matching a filename or aggregate number.

#### Scenario: Active profile and evidence identities match

- GIVEN an ACTIVE profile、VALID dataset release、full declared selection and all required run/metric identities match
- WHEN the profile and details evidence are validated
- THEN profile compatibility is `VALID`
- AND gate evaluation may proceed without any backend/provider call

#### Scenario: Profile contract or identity drifts

- WHEN schema/version/profile hash、dataset、selection、channel、slice、operator、threshold、tolerance or required identity is missing or differs
- THEN validation fails closed with a stable safe error code
- AND no threshold result is inferred from partial fields

#### Scenario: Draft unversioned or historical evidence

- GIVEN the profile is DRAFT OR the evidence dataset is `UNVERSIONED` OR required C8/C9 identity is unavailable
- WHEN a consumer requests a gate result
- THEN the result is not `PASS`
- AND historical metrics remain readable without being backfilled or promoted into a C10 baseline

### Requirement: Deterministic Channel Slice And Threshold Evaluation

C10 SHALL evaluate only profile-declared metrics in their declared retrieval、objective or judge channel. Slice axes SHALL be limited to deterministic `all`、`type`、`difficulty` and `answerability` values resolved from the profile-bound versioned dataset by sample id. Arbitrary executable expressions MUST NOT be accepted.

Each rule SHALL expose channel、slice、metric、operator、target、observed value、denominator、minimum denominator、required flag、tolerance/reference if applicable and a safe result reason. Required missing values、insufficient denominators、failed observations or incomplete sample selection MUST NOT be filled with zero、dropped from the denominator or calculated from a successful subset.

Hard thresholds SHALL use inclusive minimum/maximum semantics. Reference regression tolerance MAY apply only when reference and candidate profile/dataset/run/metric identities match. When a hard threshold and reference rule both exist, both MUST pass; tolerance MUST NOT waive status、identity、error、missing or denominator requirements.

#### Scenario: Overall and category slices are reproducible

- GIVEN the same profile、versioned annotations and complete per-sample evidence
- WHEN overall、type、difficulty and answerability rules are evaluated repeatedly
- THEN each slice has the same sample membership、denominator、observed metric and result
- AND category degradation cannot be hidden by the overall aggregate

#### Scenario: Required metric or denominator is incomplete

- WHEN a required metric is unavailable OR its denominator is below the profile minimum OR any required sample observation is missing
- THEN that rule is `NOT_EVALUABLE`
- AND the evaluator does not substitute zero、remove failures or return PASS

#### Scenario: Hard floor and regression tolerance both apply

- GIVEN compatible complete candidate and locked reference evidence
- WHEN a rule declares both a hard threshold and maximum absolute regression
- THEN the rule passes only if both conditions pass
- AND tolerance cannot rescue a safety/error/status/identity failure

### Requirement: Gate Status And Stable Exit Code Semantics

C10 gate result SHALL be exactly one of `PASS`、`FAIL`、`NOT_EVALUABLE` or `INVALID`. `PASS` requires an ACTIVE compatible profile、complete required evidence and every required quality rule passing. `FAIL` SHALL mean complete compatible evidence exists but at least one required quality threshold fails. `NOT_EVALUABLE` SHALL mean policy-valid evidence cannot support a quality decision because of draft status、channel/status/error/missing/selection/denominator/comparison incompleteness. `INVALID` SHALL mean the profile or input contract is invalid.

The standalone evaluator CLI SHALL return `0` for PASS、`3` for FAIL、`4` for NOT_EVALUABLE and `2` for INVALID. An unclassified evaluator runtime failure SHALL remain exit code `1` and MUST NOT be relabeled as a quality failure. Gate summary SHALL preserve all expected rules and safe reason codes even when one rule already failed.

#### Scenario: Complete evidence passes every rule

- GIVEN an ACTIVE compatible profile and complete evidence
- AND every required threshold and regression rule passes
- WHEN gate evaluation completes
- THEN gate status is `PASS`
- AND CLI exit code is `0`

#### Scenario: Complete evidence is below a quality threshold

- GIVEN profile/evidence identities and required denominators are complete
- WHEN at least one required hard or regression threshold fails
- THEN gate status is `FAIL`
- AND CLI exit code is `3` without misreporting evidence as incomplete

#### Scenario: Evidence cannot support a quality decision

- WHEN a required channel is partial/skipped/ineligible OR errors exceed policy OR selection/metric/denominator is incomplete
- THEN gate status is `NOT_EVALUABLE`
- AND CLI exit code is `4` without presenting a successful-subset quality result

#### Scenario: Profile or input contract is invalid

- WHEN profile/input JSON、schema、version、hash、operator or finite numeric contract is invalid
- THEN gate status is `INVALID`
- AND CLI exit code is `2`; unexpected runtime failures remain `1`

### Requirement: Offline Gate Safety And Evidence Activation Boundary

C10 SHALL provide an offline evaluator that consumes local details evidence and writes allowlisted aggregate gate output. Ordinary JSON、Markdown and console output MUST NOT add raw question、answer、expected content、claim、citation、context、provider body、secret、Authorization or absolute local path. Evaluation of an existing details artifact MUST make zero backend、embedding、rerank、ask、generation、judge or other provider calls.

C10 planning and offline implementation SHALL use only synthetic/static evidence and zero data egress. Activation of any numeric profile from real evidence MUST receive separate authorization after disclosing dataset/selection、provider/model、maximum calls、egress、cost or zero-cost basis、rate limits、timeout/retry and raw artifact handling. C10 MUST NOT auto-learn thresholds、change metric formulas or alter dataset、production prompt、retrieval、rerank、citation、no-answer、default judge/provider or application behavior to make a gate pass.

#### Scenario: Offline evaluator replays existing evidence

- GIVEN a local details JSON and tracked profile
- WHEN the evaluator calculates and writes a gate summary
- THEN backend/provider call count and data egress are 0
- AND ordinary output contains only allowlisted identity、metric、denominator、threshold、status and safe reason fields

#### Scenario: Reference evidence is not separately authorized

- GIVEN offline implementation is approved but live/reference evidence is not
- WHEN C10 tests and documentation are completed
- THEN only synthetic fixtures、static validation and existing local artifacts are used
- AND any real-evidence profile remains DRAFT without a production or judge quality claim
