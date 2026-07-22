# Proposal: C8b Eval Dataset Expansion And Annotation

## Status

- Change type：Type C 重大变更。
- 当前阶段：规划草案，等待用户批准 proposal、design 决策与 `evaluation` spec delta；未进入样本实现。
- 提交责任：`用户手动提交`。Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- 外部调用：规划阶段真实 embedding、rerank、ask、judge、LLM/provider 调用量为 0，数据出站为 0。

## Summary

C8a 已把当前 30 条开发样本、3 份 fixture、sample schema 与 annotation identity 固定为 `rag-eval-dev-v1`，并建立本地 fail-fast validator。C8b 在这个治理基础上扩充评测问题与标注，建议形成 150 条的 `rag-eval-dev-v2`：原 30 条作为不可变 seed 保留，新增 120 条，覆盖 fact、definition、reasoning、multi_hop、no_answer 五类和 easy、medium、hard 三档。

C8b 只扩大当前固定知识域下的数据覆盖，不修改 retrieval、chunking、rerank、prompt、citation、no-answer、judge 或生产默认行为，不运行真实评测来声称质量提升。数据冻结、版本 bump、配额、标注复核和去重规则必须先获批准，再进入实现。

## Why

当前 30 条数据已足以支持开发期可重复 A/B，但覆盖明显偏窄：

- type 分布为 fact 10、definition 8、reasoning 6、multi_hop 3、no_answer 3；
- difficulty 分布为 easy 15、medium 12、hard 3；
- answerable/no-answer 为 27/3；
- fact 几乎全部是 easy，reasoning 全部是 medium，multi_hop 全部是 hard，类型与难度高度耦合；
- expected source 引用为 Java 12、RAG 11、Spring Boot 7，三份 fixture 覆盖不完全均衡；
- 当前没有重复 ID 或完全重复 question，但样本量不足以稳定观察细分类失败模式。

若直接继续用 30 条数据建立 C9/C10 指标与门禁，个别样本会对类别指标产生过大影响，也容易把针对小样本的调整误当成通用收益。C8b 需要先建立更宽、可审计、可冻结的数据 release。

## Current State Classification

### confirmed

- C8a 已验收归档，4 requirements / 13 scenarios 已接受进 `evaluation` baseline。
- `.ai/ACTIVE_TASK.md=IDLE`，启动前无未归档 change，工作区干净。
- `rag-eval-dev-v1` 当前 validation=`VALID`，固定 30 条 question set、3 份 fixture 和 schema v1。
- 当前 30 条无 duplicate ID 或 duplicate question，全部满足 C8a field、enum、answerability、source 与 distribution 校验。
- 冻结路线图把 C8b 定义为 `eval-dataset-expansion-and-annotation`，位于 C8a 之后、C9/C10 之前。

### partial

- 当前已有五类 type、三档 difficulty 和 answerability 标注，但交叉覆盖不平衡。
- expected source/context/keyword/answer point 已存在，但只在 30 条 seed 上经过验证。
- C8a 定义了 version bump 与 immutable release 规则，但尚未形成同时保存 v1/v2 manifest 的 release 目录约定。
- 当前 validator 能验证 schema 与 distribution，但尚未验证 C8b quota、seed immutability、question near-duplicate 或 annotation review evidence。

### planned

- 建立 150 条 expanded release，保留原 30 条并新增 120 条。
- 固定 type、difficulty、answerability 和 fixture coverage 配额。
- 增加离线 quota、seed immutability、duplicate/near-duplicate、annotation grounding 与 review evidence 检查。
- 保存 v1 release manifest，创建 v2 question/annotation identity，并让默认 manifest 指向验收后的 v2。
- 更新评测指南、长期项目说明、tasks 和执行证据。

### out_of_scope

- 不构造 tenant isolation、越权或恶意文档样本；留给 C14。
- 不实现 claim-evidence 指标或 judge 校准；留给 C9a/C9b。
- 不建立质量阈值、退出码或 CI gate；留给 C10。
- 不修改 Java/API、数据库、前端、依赖、provider、embedding、retrieval、chunking、rerank、prompt、citation 或 no-answer 运行时行为。
- 不因扩样本宣称 retrieval、generation、citation、no-answer 或 judge 指标改善。

### unknown

- 150 条是否作为最终批准总量，还是选择 100、120、200 或 300。
- C8b 是否只使用现有 3 份 fixture，还是同时扩充 fixture corpus。
- quota 是否采用本文建议值，以及是否需要对 type×difficulty 做每格硬下限。
- annotation review evidence 使用独立 sidecar、Markdown checklist，还是扩展 manifest contract。
- 是否允许任何外部 LLM 辅助生成或复核；默认按 0 外调规划。

## 用户故事（大白话）

改之前，30 条题里 easy fact 很多、hard 几乎只有 multi-hop；某个类型只错一两条，百分比就会大幅波动。改之后，团队拿到一套有明确总量、分类配额、来源平衡、标注复核和版本身份的数据 release，能知道每类问题到底覆盖了多少，也能证明扩充没有偷改旧题、没有为了当前算法表现定制答案。

## Goals

1. 把开发评测集从 30 条扩充到获批总量，建议为 150 条。
2. 保留 C8a 的 30 条 seed 样本内容、顺序和标注不变，并为 expanded release 生成新的 question/annotation/release version。
3. 明确五类 type、三档 difficulty、answerability 与 fixture coverage 配额。
4. 保证 answerable 样本的 source/context/keyword/answer point 可由固定 fixture 支持，no-answer 样本在固定 corpus 中确实无答案。
5. 用离线、可重复的检查锁定 quota、duplicate、seed immutability、source grounding 与 review evidence。
6. 为 C9/C10 提供更稳定的数据基础，但不提前实现它们的指标或门禁。

## Proposed Scope

### Proposed target and quotas

建议目标总量为 150，包含原 30 条与新增 120 条：

| 维度 | 建议配额 |
|---|---|
| type | fact 35、definition 30、reasoning 40、multi_hop 25、no_answer 20 |
| difficulty | easy 50、medium 65、hard 35 |
| answerability | answerable 130、no-answer 20 |

type×difficulty 需要在 design 中进一步锁定，避免继续出现 fact≈easy、reasoning≈medium、multi_hop≈hard 的强耦合。配额是 release acceptance contract，不用于为了某个算法分数回填样本。

### Corpus boundary

首选方案是 C8b 继续使用现有 3 份 fixture，只扩 question/annotation，不改变 corpus，以便把“数据覆盖扩大”和“知识域扩大”分开。如果用户选择扩充 fixture，则必须同时 bump `fixtureCorpusVersion`、更新逻辑 KB contract、重新建立固定 KB，并把额外风险写入 design；不得静默混入新文档。

### Release artifacts

- 保存 `rag-eval-dev-v1` 的 manifest 与 question bytes，保证当前 checkout 仍可显式验证旧 release。
- 新增 v2 question set，不覆盖 v1 JSONL；默认 `dataset-manifest.json` 只有在 v2 完整验收后才切换到新 release。
- 若 schema 字段不变，继续使用 `rag-eval-sample-v1`；若新增 review/split 字段，必须创建 schema v2，而不是放宽 unknown-field。
- 新增机器可读 coverage/review evidence 或等价 sidecar；具体格式等待 design 决策批准。

### Authoring and review

- 采用 source-first：先定位 fixture 中的可支持事实/关系，再写 question 与 expected annotation。
- 原 30 条只读取、复制和比对，不修正文案、不重排、不重新标注。
- 新样本至少经过结构校验、grounding 校验、duplicate/near-duplicate 检查和第二遍语义复核。
- 数据冻结后才允许运行 baseline；不得根据某个 provider/算法的错误样本反向定制 question、keyword、context 或 no-answer。

## External Call Gate

C8b 规划默认全部本地完成：

- embedding：0；
- rerank：0；
- ask：0；
- judge：0；
- LLM/provider：0；
- 数据出站：0。

实现阶段也优先只读取 tracked fixture 并离线编写/校验。若后续希望使用外部 LLM 批量生成或复核，必须在调用前单独说明样本量、预计调用次数、出站文本、模型、费用/零费用依据、限流风险和 raw output 处置，并取得用户授权；该授权不由本 proposal 自动包含。

## Acceptance Criteria

### Planning gate

- proposal、design、tasks 和 `evaluation` spec delta 齐全且范围一致。
- design 记录总量、配额、fixture、release layout、review、duplicate、外调与 out_of_scope 的真实取舍。
- 用户批准总量、quota、corpus boundary、release/version 方案、review evidence 和实现授权。
- baseline spec、数据文件、manifest/schema 与 runner 在规划阶段保持不变。

### Implementation gate

- 先用测试证明 quota drift、seed drift、duplicate/near-duplicate、无效 source/context 和 review 缺失会失败。
- expanded question set 达到获批总量和 exact quota，原 30 条内容/顺序/hash 关系可审计。
- v1 仍可显式验证；v2 使用新的 question/annotation/release version，fixture/schema 是否 bump 与实际变化一致。
- 所有新 answerable 样本通过 fixture grounding；所有 no-answer 样本满足固定 corpus 下的拒答语义。
- Python 全量、SensitiveLogs、secret/path/link、`git diff --check` 和 current/v1/v2 release validation 通过。
- 实现阶段真实外部调用量与数据出站按授权记录；默认应为 0。

### Acceptance and closeout gate

- 用户验收 expanded data、quota、review evidence、release identity 与结论边界。
- 已批准 delta 原文接受进 `openspec/specs/evaluation/spec.md`。
- change 归档并恢复 `.ai/ACTIVE_TASK.md=IDLE`。
- C8b 归档只表示评测数据扩充完成，不代表 C9/C10/C14 或生产质量结论完成。

## Risks

- 仅用 3 份 fixture 扩到 150 条可能产生题意重复或过细切分；通过 quota、near-duplicate warning、source coverage 与语义复核控制，并在事前闸门确认是否扩大 corpus。
- 人工或 Agent 编写 annotation 仍可能把常识当作 fixture 证据；必须以 source/context grounding 为准，不接受“模型知道”作为依据。
- 为填 quota 硬造低质量问题会污染 baseline；允许在 review 阶段拒绝样本并重新选材，但不得通过降低规则凑数。
- 同时改变 question、annotation、fixture 和 schema 会难以定位漂移；默认建议只 bump question/annotation/release，其他轴按真实变化决定。
- tracked eval data 对开发者可见，不是隐藏 benchmark；C8b 只提高开发评测覆盖，不解决训练污染或独立盲测问题。

## Rollback

- v1 release 文件与 manifest 保留，v2 artifacts 可独立移除或停止设为默认，不需要恢复业务数据库或 KB 数据。
- C8b 不修改后端运行时行为，rollback 不涉及 Java/API/数据库迁移。
- 若 expanded release 语义验收失败，保持默认 manifest 指向 v1，不以 `UNVERSIONED` 结果替代正式 release。
