# Proposal: C8a Eval Dataset Schema And Versioning

## Status

- Change type：Type C 重大变更。
- 当前阶段：用户已批准 proposal、16 条 design 决策和 4 requirements / 13 scenarios；TDD 实现与本地验证已完成，正在等待用户实现验收。
- 提交责任：`用户手动提交`。Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- 外部调用：规划与实现阶段真实 embedding、rerank、ask、judge、LLM/provider 调用量均为 0，数据出站为 0。

## Summary

C7 已用 eval-set SHA-256、fixture hash、KB observation、配置快照和 Git HEAD 完成一次严格的 reranker A/B，但当前 30 条 JSONL 仍没有独立的数据集 release manifest、样本 schema version 或 annotation version。两个 runner 目前主要完成 JSON 解析；字段缺失、未知字段、重复 ID、枚举漂移、answerable/no-answer 条件冲突和 fixture 来源漂移没有统一的 fail-fast 契约。

C8a 将把当前 30 条开发样本和 3 份 fixture 固定为首个版本化评测 release，增加一个机器可读的 release manifest、一个项目级样本 schema contract 和共享本地 validator。正式评测在接触 backend/provider 之前验证数据集、标注和 fixture 身份；任何未声明漂移都非零退出，不得形成新的正式 baseline。

C8a 只建立治理和执行门禁，不扩充、重写或重新标注样本。100～300 条扩充与分类配额留给 C8b。

## Why

当前已确认事实：

- `docs/eval/rag_eval_set.jsonl` 有 30 条样本，类型分布为 fact 10、definition 8、reasoning 6、multi_hop 3、no_answer 3；当前 SHA-256 为 `d17bde69db58848fe79069709a7b7c3c927da916661faa8caf1bd71efcd6d7fe`。
- 3 份 fixture 已由可复现 runner 记录 path/name/SHA-256/bytes，C7 也记录了 KB document contentHash、配置 snapshot 和 Git HEAD。
- `run_rag_eval.py` 只逐行 `json.loads`；`run_reproducible_rag_eval.py` 额外确认每行是 object，但两者没有共享 schema/annotation validator。
- accepted `evaluation` spec 只要求正式 baseline 固定评测集、fixture、KB、配置和 Git HEAD；尚未定义 release/version 演进、样本字段、条件语义和漂移失败规则。
- C7 证明当前 hash 下的 30 条数据可以支持一次可比较 A/B，但明确不能把它外推成生产数据集或论文级 benchmark。

缺少 C8a 时，同一个文件路径可能在字段、标注、样本顺序或 fixture 内容变化后继续被误称为同一 baseline；错误还可能直到已经联系 backend/provider 后才暴露，浪费调用并污染报告身份。

## Current State Classification

### confirmed

- `main` 与 `origin/main` 同步，规划启动前工作区干净。
- `.ai/ACTIVE_TASK.md` 为 `IDLE`，没有未归档 change。
- C7 已验收归档，`evaluation` baseline 已接受 A/B identity/provider coverage 契约。
- 当前 30 条样本 ID/问题无重复，10 个既有字段全部存在，expected source 均落在 3 份 fixture 文件名集合内。
- 当前 runner 已具备 SHA-256、fixture、KB observation、config snapshot 和 Git HEAD 元数据基础。

### partial

- eval-set 有原始文件 hash，但没有 dataset release version、schema version、annotation version 和 release manifest hash。
- fixture 有逐文件 hash，但没有作为版本化 corpus 与 question set 一起签名的 release 身份。
- runner 能解析 JSON/object，但不验证 required fields、allowed fields、enum、唯一 ID、条件语义或跨 artifact 引用。
- C7 metadata 能固定一次运行身份，但这些规则尚未形成所有正式评测共享的数据集契约。

### planned

- 创建 versioned dataset release manifest 和项目级样本 schema contract。
- 创建共享、仅标准库的 validator，并由两个 runner 在任何 backend/provider 联系前调用。
- 定义 dataset/schema/annotation/corpus version 的演进矩阵与不可变 release 语义。
- 在 run metadata/report 中记录 release identity 和 validation result。
- 用 TDD 覆盖字段、枚举、重复 ID、条件语义、路径安全、hash 漂移与无调用 fail-fast。

### out_of_scope

- 不新增、删除、改写或重新标注当前 30 条样本；C8b 才扩充至 100～300 条并定义分类配额。
- 不构造权限隔离或恶意文档样本；它们依赖 tenant model，留给 C14。
- 不修改 retrieval、embedding、chunking、rerank、prompt、citation、no-answer 或 judge 指标公式和生产默认行为。
- 不实现 claim-level 指标、judge 校准或通用质量退出码门禁；分别留给 C9/C10。
- 不新增 Python 第三方依赖，不修改 Java/API、数据库、前端、部署或 provider 配置。
- 不执行真实 embedding、rerank、ask、judge、LLM/provider 调用。

### unknown

- 本 change 实现完成后，真实生产数据分布与 C8b 分类配额仍未知，不在 C8a 推断。
- C8a 只验证本地 release/schema/versioning 行为，不通过真实 provider run 推断质量收益或生产 SLA。

## 用户故事（大白话）

改之前，团队看到两份报告都写着 `rag_eval_set.jsonl`，可能就以为它们用了同一套题；但其中一份题目、标注、顺序或 fixture 可能已经悄悄变化，甚至到请求发给 backend 或外部模型后才发现，比较结果因此不可信。改之后，每次正式评测先核对“这是哪个数据 release、用哪个 schema 和标注版本、题目和 fixture 的 hash 是否完全匹配”，任何漂移都会在本地、零外调地停止，只有身份完整的结果才能进入正式 baseline。

## Goals

1. 为当前 question set + fixture corpus 建立不可变、可审查的首个 dataset release identity。
2. 定义样本 allowed/required fields、类型、枚举、唯一 ID、answerable/no-answer 条件语义和 source 引用规则。
3. 明确 dataset、schema、annotation、fixture corpus 变化分别如何 bump version，禁止同 version 指向不同内容。
4. 让两个 runner 共享同一 validator，并在认证、preflight、KB mutation 或 provider 调用前 fail-fast。
5. 在正式 run metadata/report 中记录 release manifest hash、各版本、question/fixture identity、validation status、配置快照和 Git HEAD。
6. 保持 C7 历史证据及当前 30 条 JSONL/3 份 fixture 的原始字节不变。

## Proposed Scope

### Versioned artifacts

- 新增 `docs/eval/dataset-manifest.json`：记录 release version、schema version、annotation version、corpus version、question set path/hash/bytes/count/order identity、fixture path/hash/bytes、逻辑 KB 契约和类型分布。
- 新增 `docs/eval/schema/rag-eval-sample-v1.json`：项目级、机器可读的样本 contract；不伪称完整 JSON Schema 标准实现。
- manifest 和 schema 只保存 repo-relative safe paths、hash、版本与非敏感事实，不包含凭据、绝对本机路径、原始运行时响应或用户/生产数据。

### Shared validation

- 新增 `scripts/eval_dataset_contract.py`，只使用 Python 标准库加载 schema/manifest、验证 JSONL、计算 hash、检查 fixture 引用和生成稳定错误类别。
- `scripts/run_rag_eval.py` 与 `scripts/run_reproducible_rag_eval.py` 复用同一 validator；不复制两套规则。
- 正式默认 eval-set 自动绑定 tracked manifest；custom eval-set 若没有匹配 manifest，只能通过显式 `--allow-unversioned-eval-set` 作为非正式诊断运行，并在 metadata/report 中标为 `UNVERSIONED`，不得形成正式 baseline 或可比较结论。

### Identity composition

- 静态 manifest 固定 question set、schema、annotation、fixture corpus 和逻辑 KB contract。
- 运行时 metadata 再绑定 observed KB/document identity、tracked config snapshot 和当前 Git HEAD。
- Git HEAD 不写死在被同一 commit 跟踪的 manifest 中，避免 manifest→commit→manifest 的自引用；完整 identity 由静态 release + runtime observation 组合形成。

### Tests and documentation

- 新增 validator 单元测试并扩展两个 runner 测试。
- 更新 `docs/eval/RAG_EVAL_GUIDE.md`，说明 release 创建、校验、bump、formal/unversioned 边界和恢复步骤。
- 实现完成后同步 `openspec/project.md`、架构/技术债/优化索引与 `.ai/AGENT_LOG.md`；规划阶段不提前改这些长期完成态说明。

## Version Semantics

已批准采用四个显式 version 轴：

- `releaseVersion`：对外引用的完整数据 release；任一组成 identity 变化都生成新 release。
- `sampleSchemaVersion`：字段集合、类型、枚举或条件语义发生不兼容变化时 bump。
- `annotationVersion`：题目不变但 expected sources/keywords/answer points/contexts/should_answer/notes 的审核标注变化时 bump。
- `fixtureCorpusVersion`：fixture 文件集合或内容变化时 bump。

question membership、order、ID 或 question text 改变必须生成新 release；是否另设 `questionSetVersion` 作为独立轴在 design 决策中确认。无论版本名称如何，同一 release version 绝不能解析为不同 hash。

## External Call Gate

C8a 的规划、实现和验收均应可在本地完成：

- embedding：0；
- rerank：0；
- ask：0；
- judge：0；
- LLM/provider：0；
- 数据出站：0。

验证不得启动 backend 或 Docker 作为必要前提。若后续用户额外要求真实 run smoke，必须另行披露调用量、数据出站、模型、费用/零费用依据与限流风险，并获得独立授权；该 smoke 不属于 C8a 默认 acceptance gate。

## Acceptance Criteria

### Planning gate

- proposal、design、tasks、`evaluation` spec delta 齐全且范围一致。
- design 包含所有真实技术岔路和 out_of_scope 的三行决策记录。
- `.ai/ACTIVE_TASK.md` 只指向本 change。
- baseline spec 不在规划阶段修改。
- 用户明确批准 scope、non-goals、决策记录、spec delta 与实现授权后，才能修改 runner/schema/dataset artifacts。

### Implementation gate

- 先用 RED 测试证明缺字段、未知字段、重复 ID、非法 enum、answerability 冲突、source 漂移、unsafe path、hash 漂移和同版本不同内容未被拒绝。
- 最小 GREEN 实现共享 validator、manifest/schema、runner 集成和版本 metadata。
- 当前 30 条 JSONL 和 3 份 fixture 的 bytes/SHA-256 保持不变；若发现不能满足已批准 schema 的真实冲突，停止并回到事前闸门，不静默改标注。
- invalid/drift 输入在任何 backend/provider 联系前非零退出，测试证明真实外部调用为 0。
- Python 全量、SensitiveLogs、`git diff --check`、断链/受保护路径/tracked secret 检查通过。
- 无 Java/POM/前端改动时，Maven 与 frontend build 可按范围跳过并说明原因。

### Acceptance and closeout gate

- 用户验收 schema、manifest、version bump 规则、validator 行为和兼容边界。
- 已批准 delta 原文接受进 `openspec/specs/evaluation/spec.md`。
- change 归档并将 `.ai/ACTIVE_TASK.md` 恢复为 `IDLE`。
- C8a 归档后只表示版本治理完成，不表示 C8b 样本扩充、C9 claim/judge 或 C10 quality gate 已完成。

## Implementation Result（待用户验收）

- 新增 `docs/eval/dataset-manifest.json` 与 `docs/eval/schema/rag-eval-sample-v1.json`，固定 `rag-eval-dev-v1`、30 条 question set、3 份 fixture、逻辑 KB contract 与 distribution；现有 JSONL/fixture bytes 和 C7 hash 未改变。
- 新增标准库 validator，覆盖 safe repo path、artifact bytes/hash、sample count/order、allowed/required/type/enum/ID、answerable/no-answer、fixture source 与 distribution；错误只输出稳定 code 和安全定位字段。
- direct/reproducible runner 在 login、preflight、KB mutation 与 provider 前共用 validation；正式路径输出 `VALID` release identity，custom 输入默认拒绝，显式诊断标为 `UNVERSIONED` 且不可比较。
- 聚焦测试、Python 全量 86 tests、两条 current-release plan、SensitiveLogs、secret/protected-path/断链扫描和 `git diff --check` 均通过；Maven、frontend、Docker/live provider 因无对应改动且 acceptance 为纯本地而跳过。
- 尚未完成用户验收、delta 接受、归档和 `ACTIVE_TASK=IDLE`；这些 closeout 动作不在本轮提前执行。

## Risks

- 过度严格的 schema 可能阻碍后续字段扩展；通过显式 schema bump 和 formal/unversioned 分层控制，而不是静默接纳未知字段。
- schema contract 与 Python validator 可能漂移；设计要求 schema document 是唯一机器可读规则源，validator 从中加载规则并以测试锁定。
- 把 runtime Git HEAD 写进静态 manifest 会形成自引用；完整身份必须拆成静态 release 与运行时 observation。
- 把 numeric KB ID 写成跨环境稳定 identity 会降低可移植性；manifest 只固定逻辑 KB contract，运行时仍记录并校验实际 KB/document facts。
- C8a 若顺手修正文案或标注，会破坏 C7 hash 与阶段边界；当前数据只能原样纳入首个 release。

## Rollback

- manifest/schema/validator 和 runner additive metadata 可独立回退，不影响 backend、KB 或业务数据。
- 当前 JSONL 与 fixture 不在本 change 中重写，因此 rollback 不需要恢复评测内容。
- 若 validator 集成影响 legacy ad-hoc 命令，可回退 runner 接线并保留离线校验工具；不得通过放宽正式 baseline 契约来掩盖问题。
