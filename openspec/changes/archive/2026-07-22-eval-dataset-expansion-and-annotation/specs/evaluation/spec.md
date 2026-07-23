# Evaluation Spec Delta: C8b Eval Dataset Expansion And Annotation

## ADDED Requirements

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
