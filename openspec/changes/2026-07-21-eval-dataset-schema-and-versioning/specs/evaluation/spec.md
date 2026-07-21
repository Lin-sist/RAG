# Evaluation Spec Delta: C8a Eval Dataset Schema And Versioning

## ADDED Requirements

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

