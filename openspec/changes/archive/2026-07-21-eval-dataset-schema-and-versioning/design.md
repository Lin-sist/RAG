# Design: C8a Eval Dataset Schema And Versioning

## Context

当前评测链路已经具备一部分身份事实：

- `run_reproducible_rag_eval.py` 对 eval-set、fixture、tracked config 计算 SHA-256，并记录 observed KB/document identity 与 Git HEAD；
- C7 comparator 会用 eval-set hash、sample order、fixture/KB/config/Git HEAD 判断两个 reranker arms 是否可比较；
- `docs/eval/rag_eval_set.jsonl` 当前为 30 条开发样本，固定依赖 3 份 `test-data/*.md` fixture。

但这些事实仍属于“一次 run 的 metadata”，不是可演进的数据集 release contract：

- eval-set 没有 release/schema/annotation version；
- fixture 文件没有与 question set 共同组成一个不可变 corpus release；
- direct runner 只解析 JSON，reproducible runner 只额外检查 object；
- required/allowed fields、enum、unique ID、answerability 条件和 source 引用没有统一校验；
- custom eval-set 可以绕过正式版本身份，报告无法稳定区分 formal 与 ad-hoc evidence。

C8a 的设计目标是增加一个本地、无外调、fail-closed 的 dataset contract 层，同时保持当前指标公式、runtime API、默认 provider 和历史报告不变。

## Design Principles

1. 数据身份先于评测执行；无有效 release identity 不形成正式 baseline。
2. 静态 release 与运行时 observation 分离，避免 Git commit 自引用和环境数字 ID 假稳定。
3. schema 是可执行契约，不只是说明文档；两个 runner 复用同一 validator。
4. 版本变化必须显式，不能让同一 version 静默对应不同 bytes 或标注。
5. formal 与 ad-hoc 能力分层；兼容旧调试不等于允许未版本化结果冒充正式 evidence。
6. 当前 30 条数据与 fixture 原样纳入首个 release，不为通过 schema 而定制问题或标注。
7. 全部校验在本地完成，invalid/drift 输入在认证、preflight、mutation 或 provider 调用前失败。

## Proposed Flow

```text
tracked dataset manifest + sample schema contract
  -> local path safety + manifest/schema parse
  -> eval-set bytes/hash/count/order validation
  -> per-sample field/type/enum/conditional validation
  -> fixture bytes/hash/source-reference validation
  -> release identity VALID ?
       no  -> stable local error + non-zero exit + zero backend/provider calls
       yes -> plan/preflight/run
              -> observed KB/document identity
              -> tracked config snapshot + Git HEAD
              -> complete runtime evaluation identity
```

`--preflight-only` 仍可联系 backend 检查已有 KB，但必须先通过全部本地 release 校验；`--keep-existing` 仍只复用，不能因 manifest 引入隐式建库或上传。

## Components

### 1. Dataset release manifest

新增 `docs/eval/dataset-manifest.json`，建议结构：

```text
manifestSchemaVersion
releaseVersion
questionSetVersion
sampleSchemaVersion
annotationVersion
fixtureCorpusVersion
questionSet { path, sha256, bytes, sampleCount, orderedSampleIdsSha256 }
sampleSchema { path, sha256 }
fixtures[] { path, sha256, bytes }
logicalKnowledgeBase { name, marker, expectedDocumentNames }
distribution { type, difficulty, shouldAnswer }
```

manifest 使用 UTF-8 JSON、稳定 key/order 格式，并计算自身 SHA-256 写入 run metadata；manifest 不把自己的 hash 写回自身，也不写 Git HEAD、numeric KB ID、vector collection、绝对路径或 secret。

所有 path 使用 `/` 分隔的 repo-relative path，拒绝绝对路径、`..`、空片段和解析到 repo root 之外的目标。validator 按 repo root 解析，避免 manifest 跟随当前 shell working directory 漂移。

### 2. Sample schema contract

新增 `docs/eval/schema/rag-eval-sample-v1.json`。它是项目级的有限 schema vocabulary，由共享 validator 直接解释；不声明为完整 JSON Schema Draft 实现，因此无需引入 `jsonschema` 第三方依赖。

v1 建议固定：

- allowed/required fields：`id`、`question`、`type`、`difficulty`、`expected_sources`、`expected_keywords`、`expected_answer_points`、`expected_contexts`、`should_answer`、`notes`；
- `type` enum：`fact|definition|reasoning|multi_hop|no_answer`；
- `difficulty` enum：`easy|medium|hard`；
- `id`、`question`、`notes` 为非空 string，`id` 在整个 release 内唯一；
- expected sources/keywords/answer points 为 string arrays，context item 只允许 `source` 与 `contains` 两个非空 string；
- answerable 样本必须有 source、keyword、answer point 和 context；
- no-answer 样本必须使用 `type=no_answer`、`should_answer=false`，sources/keywords/contexts 为空，answer points 非空；
- answerable 样本的 source/context source 必须解析到 manifest fixture basename；
- unknown fields fail closed，未来新增字段必须 bump schema version。

schema contract 同时定义 stable error codes，例如：

```text
manifest_invalid
unsafe_artifact_path
artifact_hash_mismatch
sample_not_object
missing_field
unknown_field
invalid_field_type
invalid_enum
duplicate_sample_id
answerability_conflict
unknown_fixture_source
release_identity_mismatch
```

错误输出包含 artifact repo-relative path、line/sample ID、field 和 stable code，不回显 question、notes、expected content 或 fixture 正文。

### 3. Version axes and bump matrix

建议首个 release 使用可读、非日期猜测的稳定标识，例如：

```text
releaseVersion = rag-eval-dev-v1
questionSetVersion = questions-v1
sampleSchemaVersion = rag-eval-sample-v1
annotationVersion = annotations-v1
fixtureCorpusVersion = fixtures-v1
```

bump matrix：

| 变化 | 必须变化 |
|---|---|
| 样本增删、顺序、ID、question text | questionSetVersion + releaseVersion |
| expected fields、should_answer、type/difficulty、notes 等标注 | annotationVersion + releaseVersion |
| 字段集合、类型、enum、条件规则 | sampleSchemaVersion + releaseVersion |
| fixture 集合或任一 fixture bytes | fixtureCorpusVersion + releaseVersion |
| 仅 runner 代码或运行配置变化 | 不改 dataset release；由 runtime Git/config identity 区分 |

同一 `releaseVersion` 一旦提交，manifest、question set、schema、annotation 和 fixture hashes 必须不可变。修正 typo 也属于内容变化，不能覆盖旧 release。

### 4. Static and runtime identity composition

静态 dataset release identity：

- manifest hash 与各 version；
- question set bytes/hash/count/order；
- schema bytes/hash；
- fixture corpus bytes/hash；
- logical KB name/marker/expected document names。

运行时 identity：

- 当前 Git HEAD；
- tracked config snapshot hashes；
- actual KB ID/name/marker/vector collection；
- actual documents title/status/chunkCount/contentHash；
- sample selection/order、runner mode 和 provider-specific facts。

正式报告必须同时包含两层。manifest 不固定 Git HEAD，因为 manifest 文件本身被 Git 跟踪，写入“包含自身后的 commit”会产生循环；numeric KB ID/vector collection 也只作为 observed runtime fact，不能当跨环境 release version。

### 5. Shared validator

新增 `scripts/eval_dataset_contract.py`，提供：

```text
load_schema_contract
load_release_manifest
resolve_safe_repo_path
validate_release_artifacts
validate_samples
build_release_identity
```

返回 typed dict/facts 或抛出带 stable code 的专用异常。两个 runner 只负责调用和把安全 facts 写入 plan/metadata/report，不各自复制字段规则。

validator 只使用标准库：`json`、`hashlib`、`pathlib`、`re`、`dataclasses`/`typing`。不访问网络、不读取环境变量全集、不联系 backend。

### 6. Runner integration and call boundary

`run_rag_eval.py`：

- 默认 eval-set 使用默认 tracked manifest；
- 在 selection/call planning 前验证完整 release；
- details/report 写入 `datasetReleaseIdentity` 与 `datasetValidation=VALID`；
- custom eval-set 不匹配 manifest 时默认失败。

`run_reproducible_rag_eval.py`：

- plan-only、preflight-only、keep-existing 和实际 run 共用同一 validated release facts；
- 本地 validation 发生在 login/preflight/KB create/upload/warm-up/run 之前；
- build metadata 复用 release identity，不再分别散落计算相互独立的 eval/fixture identity；保留既有字段一段兼容期，新增字段 additive。

显式 `--allow-unversioned-eval-set` 只用于 ad-hoc/local diagnosis：

- metadata/report 标记 `datasetValidation=UNVERSIONED`；
- 不得输出 formal baseline/comparison status；
- 不可与 tracked release evidence 直接比较；
- 仍执行最小 JSON object、ID uniqueness 和安全输出检查。

该开关是否保留为 C8a 首版能力待用户在事前闸门确认；若拒绝，则 custom eval-set 必须同时提供独立 manifest。

### 7. Historical evidence compatibility

C7 和更早报告保留原样。它们已经记录 eval-set hash、fixture/config/KB/Git facts时，仍作为历史 evidence；C8a 不回写新 `releaseVersion` 到旧文件，也不把旧报告伪装成通过未来 validator。

首个 C8a release 指向当前完全相同的 JSONL/fixture bytes。后续新 run 可以说明“release artifact hashes 与 C7 捕获 hash 相同”，但正式 version 语义从 C8a 生效时开始。

### 8. Documentation and governance

`docs/eval/RAG_EVAL_GUIDE.md` 增加：

- formal dataset release 的组成；
- 如何本地 validate/plan；
- version bump 决策表；
- 如何新增 custom manifest；
- drift 错误恢复；
- `UNVERSIONED` 不得当 baseline 的边界；
- C8a 与 C8b/C9/C10/C14 的职责分界。

实现和验收完成后再同步 project/architecture/technical-debt/optimization index；规划阶段只建立 active change，不提前声称能力完成。

## Verification Strategy

1. RED：manifest 缺 version/hash/path 或包含绝对/越界 path 时 fail。
2. RED：question set bytes、count、order hash 或 fixture hash 漂移时 fail。
3. RED：sample 非 object、缺字段、未知字段、非法类型/enum、duplicate ID 时 fail。
4. RED：answerable/no-answer 条件冲突或 expected/context source 不在 fixture corpus 时 fail。
5. RED：同一 release version 对应不同 artifact identity 时得到 stable `release_identity_mismatch`。
6. RED：invalid release 运行 direct/reproducible runner 时，stub backend/login/provider call count 均为 0。
7. GREEN：最小标准库 validator、manifest/schema 和 runner integration。
8. 回归：当前 30 条/3 fixtures validation=`VALID`，bytes/SHA-256 与 C7 一致。
9. 回归：既有 runner CLI 默认用法、`--plan-only`、`--preflight-only`、`--keep-existing`、C7 arm manifest 测试保持通过。
10. 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`、SensitiveLogs、`git diff --check`、断链/旧字段/受保护路径/tracked secret 扫描。
11. 不运行 backend/Docker/live provider；Java/POM/前端无改动时记录 Maven/frontend build `SKIPPED`。

## Rollout And Compatibility

- 首个实现以 additive manifest/schema/metadata 为主；不改 backend API 或数据库。
- 默认 tracked eval-set 会自动绑定 tracked manifest；现有标准命令不需要新增 secret 或外部环境变量。
- legacy report 文件不改写；旧字段在一段兼容期内保留，新 consumer 优先读取 `datasetReleaseIdentity`。
- validation failure 只停止评测，不修改 KB、文件或业务数据。
- 回滚移除 runner integration 后，manifest/schema/validator仍可作为离线工具保留；不需要数据迁移。

## Implementation Outcome（已验收）

- 16 条决策均按批准方案实现：sidecar manifest、有限 schema vocabulary、四轴加 question-set version、严格 unknown-field、safe repo path、静态/运行时 identity 分层和显式 `UNVERSIONED` 诊断模式均已落地。
- 两个 runner 共享 `eval_dataset_contract.py`；validation 顺序位于 credentials、login、preflight、KB create/upload、warm-up 和 measured run 之前。既有 C7 arm manifest、metadata 字段、plan/preflight/keep-existing 行为与指标公式保持兼容。
- 正式 metadata/report 记录 dataset release identity 与 validation status；direct runner 会拒绝 wrapper metadata 与本地 validation 的同 version/identity mismatch。`UNVERSIONED` 报告固定不安全用于 comparison。
- 当前实现与离线验证已获用户验收，baseline delta 已接受，change 已归档且活动任务恢复 `IDLE`。

## 决策记录

> 审批状态：用户已于 2026-07-21 批准本节 16 条建议并授权进入 TDD 实现。下文原始“待用户在事前闸门确认”措辞作为规划审阅痕迹保留，不再表示未决。

### 决策 1：C8a 与 C8b 保持两个串行 change
- **面临的选择**：一次完成 schema/versioning 和 100～300 条扩充；先扩数据再补版本；C8a 只建治理后由 C8b 扩充。
- **选了哪个 + 为什么**：建议只做 C8a；先锁定 schema、version 和验收流程，C8b 才有稳定标注入口，待用户在事前闸门确认。
- **放弃的代价**：合并会把工具治理与大量内容审核混成一个不可审阅 change；先扩数据会让新增标注继续处于无版本状态。

### 决策 2：采用 sidecar release manifest，不把版本字段复制到每条样本
- **面临的选择**：每条 JSONL 都重复版本字段；仅靠目录/文件名表达版本；用一个 sidecar manifest 统一固定 question set、schema 和 fixtures。
- **选了哪个 + 为什么**：建议 sidecar manifest；release 事实只有一份，样本保持简洁且当前 30 条 bytes 不变，待用户在事前闸门确认。
- **放弃的代价**：逐行复制容易出现同文件多版本冲突并改变 C7 hash；目录命名没有内容 hash 和机器校验能力。

### 决策 3：使用项目级有限 schema vocabulary，不新增通用 JSON Schema 依赖
- **面临的选择**：引入 `jsonschema` 第三方包；手写散落在两个 runner 的 if 判断；定义一个机器可读的项目 schema contract，由共享标准库 validator 解释。
- **选了哪个 + 为什么**：建议第三种；它保持 Python 零第三方依赖，又让规则只有一个机器可读来源，待用户在事前闸门确认。
- **放弃的代价**：新依赖增加安装/版本/网络成本；两套手写判断会快速漂移，无法证明 direct 与 reproducible runner 契约一致。

### 决策 4：使用 release/question/schema/annotation/corpus 多轴版本
- **面临的选择**：只设一个整体版本；只靠 Git commit；为 question、schema、annotation、corpus 分轴并由 release 聚合。
- **选了哪个 + 为什么**：建议多轴 + release；能明确“题变了、标注变了、规则变了、材料变了”各自含义，同时给报告一个完整 release 名称，待用户在事前闸门确认。
- **放弃的代价**：单一版本看不出变化来源；只靠 Git commit 无法表达数据兼容语义，也不便跨分支审计。

### 决策 5：raw byte SHA-256 是 artifact 身份主口径
- **面临的选择**：只比较文件大小/mtime；把 JSON canonicalize 后只比较语义 hash；记录原始 bytes SHA-256，并额外记录 sample order identity。
- **选了哪个 + 为什么**：建议 raw SHA-256 + order identity；它能捕获任何未声明改动且与 C7 现有证据兼容，待用户在事前闸门确认。
- **放弃的代价**：大小/mtime 可碰撞且不可移植；只用 canonical hash 会忽略格式/编码漂移并与现有 C7 hash 口径分叉。

### 决策 6：静态 manifest 不写 Git HEAD，运行时再组合完整 identity
- **面临的选择**：把 Git HEAD 固定进 manifest；完全不记录 Git HEAD；manifest 固定数据 release，run metadata 固定 Git/config/observed KB。
- **选了哪个 + 为什么**：建议静态 + 运行时组合；manifest 被 Git 跟踪，写入包含自身后的 commit 会自引用，而 run metadata 可以准确记录实际执行 HEAD，待用户在事前闸门确认。
- **放弃的代价**：静态写 HEAD 无法稳定生成；不记录 HEAD 会让相同数据在不同代码/配置下的结果被误比较。

### 决策 7：manifest 固定逻辑 KB contract，不把 numeric KB ID 当 release 身份
- **面临的选择**：固定某台环境的 KB ID/vector collection；只记录 fixture 文件；固定逻辑 name/marker/expected documents，运行时再校验 numeric ID、collection 和 document facts。
- **选了哪个 + 为什么**：建议逻辑 contract + observed facts；既能跨环境复建，又不会把同名但内容不同的 KB 当成相同，待用户在事前闸门确认。
- **放弃的代价**：数字 ID 跨环境不稳定；只看 fixture 文件会漏掉实际索引 KB 与本地文件不一致。

### 决策 8：v1 unknown fields fail closed
- **面临的选择**：静默忽略未知字段；保留任意 extension map；schema v1 只允许明确字段，新增字段必须 bump schema。
- **选了哪个 + 为什么**：建议严格 allowed fields；评测标注字段直接影响指标，静默接纳会让 runner 与标注工具对字段含义不一致，待用户在事前闸门确认。
- **放弃的代价**：忽略字段可能把拼写错误当有效标注；自由 extension 会削弱 v1 的可比较性并让敏感内容混入报告。

### 决策 9：answerable/no-answer 使用条件语义，不只校验字段类型
- **面临的选择**：只确认 JSON 类型；允许 should_answer 与 type/expected fields 任意组合；显式锁定 answerable 与 no-answer 的条件规则。
- **选了哪个 + 为什么**：建议条件规则；否则结构合法但语义矛盾的样本会同时污染 retrieval、generation 和 no-answer 通道，待用户在事前闸门确认。
- **放弃的代价**：只做类型校验无法阻止 `no_answer + should_answer=true` 或有答案样本缺 source/context；任意组合会让指标分母不稳定。

### 决策 10：artifact path 只允许 repo-relative safe path
- **面临的选择**：允许绝对本机路径；相对当前 shell cwd；相对 repo root 并拒绝绝对路径与 `..` 越界。
- **选了哪个 + 为什么**：建议 repo-relative safe path；manifest 才能跨机器复用，也不会读取仓库外文件或把用户名路径写进 evidence，待用户在事前闸门确认。
- **放弃的代价**：绝对路径不可移植且泄露环境信息；cwd-relative 会因启动目录不同解析到错误 artifact。

### 决策 11：两个 runner 复用一个 validator，并在任何 I/O 外调前失败
- **面临的选择**：只在 reproducible runner 校验；两个 runner 各写一套；共享 validator 且 direct/reproducible 都先本地验证。
- **选了哪个 + 为什么**：建议共享、前置校验；formal evidence 无论从哪个入口运行都遵守同一 contract，并确保坏数据零外调，待用户在事前闸门确认。
- **放弃的代价**：只校验一个入口会留下绕过路径；复制实现会产生不一致错误和兼容行为。

### 决策 12：保留显式 UNVERSIONED 诊断模式，但禁止形成正式 baseline
- **面临的选择**：C8a 后彻底禁止任何 custom eval-set；默认接受所有 custom 文件；提供显式 `--allow-unversioned-eval-set` 并强制标记不可作为正式 evidence。
- **选了哪个 + 为什么**：建议显式降级模式；保留开发调试能力，同时让报告状态无法与版本化 baseline 混淆，待用户在事前闸门确认。
- **放弃的代价**：完全禁止会破坏合理的本地实验；默认接受会让未审数据继续冒充可比较 release。

### 决策 13：首个 release 保持现有 30 条和 3 份 fixture bytes 不变
- **面临的选择**：借 C8a 顺便修题/改标注；重新格式化 JSONL；原样固定当前 hash，发现语义问题则另起显式新 release。
- **选了哪个 + 为什么**：建议原样固定；它保持 C7 evidence 可追溯，也防止为通过新 schema 定制评测集，待用户在事前闸门确认。
- **放弃的代价**：顺手修订会同时改变治理和数据质量两个变量；格式化也会改变 raw hash并模糊 C7/C8a 边界。

### 决策 14：历史报告不回写 releaseVersion
- **面临的选择**：批量修改旧报告并声明通过新 schema；废弃全部旧报告；保留旧 hash/metadata事实，新 version 只对 C8a 后运行生效。
- **选了哪个 + 为什么**：建议保留历史原貌；旧 evidence 没有真正经过未来 validator，不能追认，但其原始 hash 仍有审计价值，待用户在事前闸门确认。
- **放弃的代价**：回写会制造“当时已验证”的假历史；全部废弃又丢失 C7 已验收的真实 A/B 证据。

### 决策 15：C8a 不做 C9 claim/judge、C10 quality gate 或默认 provider切换
- **面临的选择**：versioning 后顺便加 claim/judge 和阈值；根据 C7 结果切默认 reranker；只完成数据 release/schema/validator。
- **选了哪个 + 为什么**：建议只做 C8a；数据身份是后续指标和门禁的前提，但不等于那些能力已经设计或验证，待用户在事前闸门确认。
- **放弃的代价**：吞并 C9/C10 会混合数据、指标和 CI 退出语义；切 provider 还缺更大数据集、成本和 SLA 决策。

### 决策 16：C8a 不构造权限隔离/恶意文档样本，也不运行真实 provider
- **面临的选择**：提前加入 tenant/恶意样本并跑真实链路；只用 mock 宣称业务收益；保持本 change 本地数据治理且真实外调为 0。
- **选了哪个 + 为什么**：建议本地治理、零外调；tenant model 尚未建立，C14 才负责隔离评测，C8a 验收也不需要模型结果，待用户在事前闸门确认。
- **放弃的代价**：提前造隔离样本缺少领域契约；用 mock 宣称收益不可信；真实 provider只增加费用、出站和限流风险而不验证 versioning 核心。
