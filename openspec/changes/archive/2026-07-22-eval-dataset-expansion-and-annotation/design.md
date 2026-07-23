# Design: C8b Eval Dataset Expansion And Annotation

## Context

C8a 已建立 `rag-eval-dev-v1` 的 release identity、sample schema、annotation/corpus version 和两个 runner 共用的本地 validator。当前数据结构可执行，但覆盖仍小且交叉分布失衡：30 条样本中 easy fact 占比高，reasoning 全是 medium，multi_hop 全是 hard，no_answer 只有 3 条。

C8b 需要扩大问题与标注覆盖，同时避免三类污染：一是改写旧 30 条导致 C7/C8a 身份失真；二是为了满足配额硬造缺少 fixture 支持的问题；三是先看当前算法错误再调整题目或标注。设计必须让 expanded release 可审计、可回退，并继续在任何 runtime/provider 调用前完成本地校验。

## Design Principles

1. 先冻结配额和审核规则，再写新样本。
2. v1 seed 不改写；v2 是新 release，不覆盖历史 bytes。
3. source-first authoring；fixture 证据优先于常识或模型记忆。
4. 配额描述覆盖，不描述收益；数据量增加不等于质量提高。
5. 自动检查负责结构、identity、exact grounding 和重复候选；语义正确性仍需第二遍复核。
6. 数据冻结后再评测，不按某个 provider/算法表现回填问题或答案。
7. 默认零外调；任何外部生成/审核必须单独授权。

## Proposed Flow

```text
C8b approved quota + authoring/review rules
  -> copy v1 30-sample seed unchanged
  -> source-first draft candidates from fixed fixtures
  -> schema + ID + exact grounding validation
  -> quota accounting + duplicate/near-duplicate scan
  -> second-pass annotation review evidence
  -> freeze v2 question set and release artifacts
  -> validate v1 and v2 identities locally
  -> only after freeze: optional separately authorized baseline run
```

## Proposed Data Contract

### 1. Target size and exact type×difficulty matrix

建议总量 150，交叉配额如下：

| type | easy | medium | hard | total |
|---|---:|---:|---:|---:|
| fact | 18 | 13 | 4 | 35 |
| definition | 14 | 12 | 4 | 30 |
| reasoning | 8 | 22 | 10 | 40 |
| multi_hop | 3 | 10 | 12 | 25 |
| no_answer | 7 | 8 | 5 | 20 |
| total | 50 | 65 | 35 | 150 |

answerable 固定为 130，no-answer 固定为 20。原 30 条计入上述配额；新增 120 条负责补齐缺口，不修改 seed 来迁就矩阵。

### 2. Fixture coverage

默认 fixture corpus 不变，仍为：

- `springboot-basics.md`
- `java-interview-guide.md`
- `rag-technology-guide.md`

对 130 条 answerable 样本，建议每份 fixture 至少被 35 条样本的 `expected_sources` 或 `expected_contexts.source` 引用，任何单份 fixture 不超过 answerable 样本的 45%。multi_hop 可以引用多份 fixture，因此 coverage count 不要求三份相加等于 130。

### 3. Seed immutability and ordering

v2 question set 的前 30 条必须与 v1 JSONL 按行解析后的对象和顺序完全一致；新增 120 条只允许追加。validator 同时比较 seed raw line hash、ordered IDs 和 canonical object identity，任一 seed 变化以 `seed_identity_mismatch` 失败。

新增 ID 延续 schema v1 pattern，并从各 type 当前最大序号之后递增；禁止重用、插队或因删除候选而回收已进入 review 的 ID。最终 release 不允许 ID gap 作为错误，但 review evidence 必须解释被废弃的 draft ID，避免悄悄替换。

### 4. Grounding semantics

- answerable：每个 `expected_contexts[].contains` 必须在对应 fixture 中 exact 或 whitespace-normalized exact 出现；`expected_sources` 与 context source 必须属于 manifest corpus。
- reasoning：answer point 必须能由一个或多个已标注 context 推导，不得依赖 fixture 外常识完成关键步骤。
- multi_hop：至少 2 个 expected contexts，且必须组合两个独立证据点；是否要求跨 fixture 由事前闸门确认。
- no_answer：固定 `type=no_answer`、`should_answer=false`、sources/keywords/contexts 为空，answer points 只描述期望拒答；review 必须确认三份 fixture 均不提供可回答证据。

### 5. Duplicate policy

自动检查分两层：

- Unicode NFKC、大小写、空白和常见标点归一化后完全相同：fail closed，error=`duplicate_normalized_question`。
- 中文字符 bigram / ASCII token Jaccard 达到建议阈值 0.82：输出 review candidate，不自动删除；review evidence 必须记录 `accepted_distinct` 或 `rejected_duplicate`。

不能通过自动改几个虚词绕过 near-duplicate；题意是否独立由复核结论决定。

### 6. Review evidence

建议新增机器可读 sidecar，例如 `docs/eval/review/rag-eval-dev-v2-review.jsonl`，每条只记录非敏感审计事实：

```text
sampleId
structureStatus
groundingStatus
duplicateStatus
semanticReviewStatus
reviewNotes
```

sidecar 不复制完整 question、expected content 或 fixture 正文；通过 sample ID 回连 question set。正式 v2 manifest 固定其 path/hash/bytes/reviewed count。为使 validator fail closed，建议将 manifest schema bump 为 v2；sample 字段不变时仍使用 `rag-eval-sample-v1`。

### 7. Release layout

建议布局：

```text
docs/eval/releases/rag-eval-dev-v1-manifest.json
docs/eval/releases/rag-eval-dev-v2.jsonl
docs/eval/releases/rag-eval-dev-v2-manifest.json
docs/eval/review/rag-eval-dev-v2-review.jsonl
docs/eval/dataset-manifest.json  # 验收后复制/指向 v2 的默认 manifest 内容
```

v1 manifest 以 C8a 已提交 bytes 保存；v2 使用新的 `releaseVersion`、`questionSetVersion` 和 `annotationVersion`。fixture 不变则保持 `fixtures-v1`，sample fields 不变则保持 `rag-eval-sample-v1`。默认 manifest 只有在 v2 全部门禁通过后才切换。

## Validation Components

### Expanded dataset validator

扩展 `scripts/eval_dataset_contract.py` 或新增窄职责模块，复用 C8a facts并增加：

- seed identity；
- exact type×difficulty quota；
- answerability 与 fixture coverage；
- normalized duplicate 与 near-duplicate candidates；
- context-to-fixture exact grounding；
- review sidecar completeness/hash；
- v1/v2 coexistence 与 version bump consistency。

错误只输出 code、repo-relative artifact、sample ID/field 或统计维度，不回显 question、context、answer point、fixture 正文或绝对路径。

### Runner compatibility

C8b 不修改指标公式。runner 继续从 manifest 得到 question set 和 release identity；若 manifest schema v2 增加 review descriptor，两个 runner 只消费 validator 返回的 additive facts。`--preflight-only` 与 `--keep-existing` 语义不变。

## Verification Strategy

1. RED：总量或 type×difficulty quota 漂移被拒绝。
2. RED：任一 seed object/order 变化被 `seed_identity_mismatch` 拒绝。
3. RED：normalized duplicate、无效 context/source、review 缺失或 sidecar hash drift 被拒绝。
4. RED：version bump 与实际 question/annotation/fixture/schema 变化不一致时失败。
5. GREEN：最小 coverage/review validator 与 manifest v2 支持。
6. 数据构建：按配额小批量追加，每批运行结构、grounding、duplicate 和 quota remaining 检查。
7. 冻结验证：v1 与 v2 均 `VALID`，v2=150、seed=30、新增=120，reviewed=150。
8. 回归：Python 全量、SensitiveLogs、secret/path/link、`git diff --check`；Java/POM/前端无改动时记录跳过。
9. 默认不运行 backend/provider；若用户另行批准 baseline，只能在数据冻结后执行并单独记录调用量和结论边界。

## Implemented Facts（已验收）

- release layout 按批准方案落地；用户验收后默认 manifest 已切换为与显式 v2 manifest byte-identical，显式 v1/v2 manifests 继续并存验证。
- sample schema 与 fixture corpus bytes 未变，故分别保持 `rag-eval-sample-v1`、`fixtures-v1`；question/annotation/release 升为 v2。
- expanded validator 实际输出 quota、seed、fixture coverage、grounding、duplicate、review 与 seed release compatibility facts；正式 `rag-eval-dev-v2` 的批准 quota 作为代码内 fail-closed contract 固定。
- 构建器只读取 tracked v1/fixture 并生成确定性 artifacts，不联网、不调用 provider；review sidecar 不复制完整 question、expected content 或 fixture 正文。

## Rollout And Compatibility

- v2 在验收前通过显式 manifest 路径验证，默认 manifest 继续保持 v1。
- v2 通过全部门禁并获用户验收后，默认 manifest 才切换；v1 manifest 和 JSONL 仍保留。
- C7/C8a 历史报告不回写 v2 version，不与 v2 指标直接比较，除非严格 identity 条件允许且比较目的明确。
- 回滚只需恢复默认 manifest 到 v1；不修改业务数据库、KB 或 runtime API。

## 决策记录

> 2026-07-22：用户已批准以下 18 条决策；实现按各条“选了哪个”执行，默认零外调，默认 manifest 延迟到验收后切换。

### 决策 1：expanded release 总量
- **面临的选择**：只做到路线图下限 100；采用中间规模 150；直接做到上限 300。
- **选了哪个 + 为什么**：采用 150；比 100 有更好的类别交叉覆盖，又能把新增量控制在 120 条，语义复核仍可执行，已由用户在事前闸门确认。
- **放弃的代价**：100 对 hard/multi-hop/no-answer 的每格样本仍少；300 在只有 3 份 fixture 时更易重复，审核成本和污染风险显著上升。

### 决策 2：原 30 条 seed 的处理
- **面临的选择**：允许顺手修订旧题；只保留 ID 但可重写标注；内容、标注与顺序全部不可变，只追加新样本。
- **选了哪个 + 为什么**：完整保留并只追加；这样 C7/C8a 身份可审计，C8b 只改变覆盖规模，已由用户在事前闸门确认。
- **放弃的代价**：修旧题会混合“数据修正”和“扩充”两个变量；只保留 ID 仍会破坏 annotation identity。

### 决策 3：fixture corpus 是否扩充
- **面临的选择**：继续只用现有 3 份 fixture；C8b 同时新增文档；完全换成更大外部 corpus。
- **选了哪个 + 为什么**：首版只用现有 3 份 fixture；控制变量并避免同时重建 KB/corpus，已由用户在事前闸门确认。
- **放弃的代价**：新增文档会扩大领域但显著增加 corpus/version/KB 变量；替换 corpus 会使旧 baseline 几乎不可比较。

### 决策 4：type 配额
- **面临的选择**：沿用当前比例；五类平均分配；按 fact 35 / definition 30 / reasoning 40 / multi_hop 25 / no_answer 20 定向补弱项。
- **选了哪个 + 为什么**：定向补弱项；保留事实/定义覆盖，同时显著增加 reasoning、multi-hop 和 no-answer，已由用户在事前闸门确认。
- **放弃的代价**：沿用比例会继续放大 easy fact 偏置；完全平均不反映当前主要能力和无答案风险的不同权重。

### 决策 5：difficulty 配额与交叉矩阵
- **面临的选择**：只约束 difficulty 总量；只约束 type 总量；同时固定 type×difficulty 15 格矩阵。
- **选了哪个 + 为什么**：固定交叉矩阵；它直接打破现有 type 与 difficulty 强耦合，已由用户在事前闸门确认。
- **放弃的代价**：只看边际总量仍可能让 fact 全是 easy、multi-hop 全是 hard；不设配额会使 release 难以复核。

### 决策 6：fixture coverage 门槛
- **面临的选择**：不约束来源分布；每份 fixture 精确等量；设置每份至少 35 条且不超过 answerable 45% 的区间。
- **选了哪个 + 为什么**：采用区间门槛；允许 multi-hop 多来源和文档内容差异，同时避免某一文档主导，已由用户在事前闸门确认。
- **放弃的代价**：无约束会重复当前不均衡；精确等量在 multi-source 样本下口径脆弱并可能逼迫低质量问题。

### 决策 7：是否新增 train/dev/test split 字段
- **面临的选择**：在 sample 中新增 split；用 sidecar 分区；C8b 仍作为单一开发 evaluation release，不建立训练/盲测分区。
- **选了哪个 + 为什么**：不新增 split；项目当前不训练模型，隐藏 benchmark 也无法由 tracked 数据实现，避免无真实用途的 schema bump，已由用户在事前闸门确认。
- **放弃的代价**：新增字段会带来 schema/runner 复杂度却不产生真正盲测；sidecar 分区同样容易被误称为隐藏测试集。

### 决策 8：sample schema 是否 bump
- **面临的选择**：把 review 字段写进每条 sample 并升 schema v2；静默允许未知字段；样本字段保持 v1，review 放 sidecar。
- **选了哪个 + 为什么**：sample schema 保持 v1、review 使用 sidecar；评测输入和治理证据职责分开，已由用户在事前闸门确认。
- **放弃的代价**：写入每条样本会污染 runner 输入；放宽未知字段会破坏 C8a fail-closed 契约。

### 决策 9：release 文件布局
- **面临的选择**：原地覆盖 `rag_eval_set.jsonl` 和默认 manifest；只保留 Git 历史；显式保存 v1/v2 artifacts并在验收后切默认 manifest。
- **选了哪个 + 为什么**：显式多版本并延迟切默认；当前 checkout 可验证旧 release，回滚也清晰，已由用户在事前闸门确认。
- **放弃的代价**：原地覆盖让旧 release 只能依赖 commit 恢复；只靠 Git 历史降低日常复现和审查可见性。

### 决策 10：review evidence 的形式
- **面临的选择**：只写 Markdown 自检；把 review 字段塞进 sample；使用按 sample ID 关联的机器可读 sidecar并由 manifest 固定。
- **选了哪个 + 为什么**：采用机器可读 sidecar；能检查 150 条是否全审、避免复制问题正文，也不改 sample schema，已由用户在事前闸门确认。
- **放弃的代价**：Markdown 难以自动证明覆盖完整；sample 内字段混合运行输入和治理状态。

### 决策 11：manifest schema 是否 bump
- **面临的选择**：review sidecar 不进 manifest；复用 v1 manifest 但增加未校验字段；升级 manifest schema v2 并验证 review descriptor。
- **选了哪个 + 为什么**：升级 manifest schema v2；正式 release 的 review evidence 也应有 path/hash/bytes/count 身份，已由用户在事前闸门确认。
- **放弃的代价**：不进 manifest 会让 review 与 release 漂移；添加未校验字段只是装饰性元数据。

### 决策 12：新增 ID 分配
- **面临的选择**：全局重新编号；随机 UUID；按现有 type prefix 从最大序号继续追加。
- **选了哪个 + 为什么**：延续 type prefix；人工审查可读并与 schema v1 兼容，已由用户在事前闸门确认。
- **放弃的代价**：重新编号破坏 seed identity；UUID 降低审计可读性且需要 schema 变化。

### 决策 13：context grounding 强度
- **面临的选择**：只验证 source 文件名；允许语义相似但不可定位；要求 exact 或 whitespace-normalized exact 命中 fixture。
- **选了哪个 + 为什么**：采用确定性 exact grounding；可离线复核，不依赖 embedding/LLM，已由用户在事前闸门确认。
- **放弃的代价**：只看文件名无法证明标注有证据；语义相似校验需要模型且结果不可稳定复现。

### 决策 14：multi-hop 的最低语义
- **面临的选择**：只要问题较长就算 multi-hop；至少两个 context 但可表达同一事实；至少两个独立证据点，跨 fixture 仅作为加分而非硬要求。
- **选了哪个 + 为什么**：要求两个独立证据点；在固定 3 文档 corpus 内可执行，又不会为了跨文档硬造问题，已由用户在事前闸门确认。
- **放弃的代价**：按长度分类没有语义；强制全部跨 fixture 会挤压自然问题并诱发低质量拼接。

### 决策 15：no-answer 构造与复核
- **面临的选择**：随机问 corpus 外主题；改写 answerable 题使关键词匹配失败；提出与当前知识域相邻但三份 fixture 都缺关键证据的问题，并逐份复核。
- **选了哪个 + 为什么**：采用相邻域 hard negative + 全 corpus 复核；更接近真实拒答压力且不依赖刻意乱码，已由用户在事前闸门确认。
- **放弃的代价**：随机外域题过于容易；故意破坏措辞会测试字符串而非知识边界。

### 决策 16：duplicate 与 near-duplicate 处理
- **面临的选择**：只查完全相同字符串；自动删除所有高相似候选；normalized exact fail + near-duplicate 进入人工/语义复核。
- **选了哪个 + 为什么**：采用两层处理；既阻断明显重复，又避免误删不同考点的相似技术问题，已由用户在事前闸门确认。
- **放弃的代价**：只查原文容易被标点/措辞绕过；自动删除会产生不可解释的 false positive。

### 决策 17：外部 LLM 辅助
- **面临的选择**：默认批量调用外部模型生成；允许隐式调用但不记账；默认零外调，确需使用时另行授权并保存非原始的审计事实。
- **选了哪个 + 为什么**：默认零外调；fixture 与标注不离开本机，成本和限流均为 0，已由用户在事前闸门确认。
- **放弃的代价**：默认外调会引入数据出站、费用和批量同质化风险；隐式调用违反仓库安全闸。

### 决策 18：冻结点与后续边界
- **面临的选择**：边跑评测边改题；根据失败样本优化 annotation；在任何 baseline 前冻结 v2，C9/C10/C14 另立 change。
- **选了哪个 + 为什么**：先冻结再评测；防止针对算法结果定制数据，并守住 C8b 只做扩充/标注的职责，已由用户在事前闸门确认。
- **放弃的代价**：边跑边改会让指标不可比较；把 claim/judge/gate/isolation 混入会让一个 change 跨越多套契约。
