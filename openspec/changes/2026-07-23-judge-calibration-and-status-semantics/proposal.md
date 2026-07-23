# Proposal: C9b Judge Calibration And Status Semantics

## Why

当前 eval runner 已支持可选 LLM judge，并输出 faithfulness、relevance、pass 与 judge error，但这条通道尚未形成可审计的校准契约：prompt、parser、阈值和模型身份没有作为一个完整版本绑定；现有 parser 会接受 provider 自报的 `pass`，也会把越界 score clamp 到 `[0,1]`；报告没有独立 judge 完整性状态，更关键的是全量 judge 失败只要 retrieval/ask 成功，当前 `Report status` 仍可能是 `CLEAN`。

C9a 已提供独立 objective lexical claim status，C9b 需要在不污染该客观通道的前提下补齐两件事：第一，用独立、人工复核的静态校准集衡量 judge 对 faithfulness/relevance gold label 的一致性与重复稳定性；第二，把 objective 完整性、judge 完整性和全局执行状态分别表达。C9b 不是让 judge 替代客观指标，也不是提前建立 C10 质量门禁。

## Readiness Gate

- 启动前 `git status --short --branch` 为干净 `main...origin/main [ahead 3]`，HEAD=`033ee01`。
- `.ai/ACTIVE_TASK.md=IDLE`，当前没有未归档 change。
- C9a 的 4 requirements / 12 scenarios 已接受进 `evaluation` baseline，change 已归档；objective claim channel 已有独立 `COMPLETE / PARTIAL / SKIPPED / NOT_APPLICABLE` 状态和固定算法身份。
- 当前代码事实确认可选 judge、call estimate、score/pass 聚合与 judge error count 已存在，但 judge contract 未版本化、无 calibration corpus/evidence，`report_status` 只检查 retrieve/ask error。
- 提交责任：`用户手动提交`。Agent 不暂存、不提交、不 push、不创建 PR、不部署。

## User Story

改之前，用户打开一份 `CLEAN` 报告，可能看到 objective retrieval/citation 指标完整，却没有意识到 judge 其实 100% 调用失败；即使 judge 返回分数，也无法判断这个模型、prompt、parser 和阈值是否在一组人工复核样本上表现稳定。

改之后，报告会同时写明 objective status、judge status 和全局 Report status。judge 明确开启但部分或全部失败时，全局状态降为 `PARTIAL`，objective 通道仍可独立标为完整和可比较；judge 分数只有在固定身份下完成校准、覆盖完整且无解析缺口时，才能作为本项目开发评测中的独立主观证据解释。

## Capability Classification

| 分类 | 当前事实 |
|---|---|
| `confirmed` | `judge-mode=off/llm`、OpenAI-compatible judge 调用、faithfulness/relevance/pass 解析、answerable-only eligibility、plan-only 调用估算、judge error count、`--fail-on-judge-errors`、C9a objective claim status 已存在 |
| `partial` | judge config 只零散进入 details；inline prompt、parser 和 `0.70` 派生阈值未形成完整 identity；聚合会静默忽略 judge error 样本；全局状态与 comparison safety 混淆 objective 和 judge 完整性 |
| `planned` | 版本化 judge contract、24 条独立人工 gold calibration cases、确定性 manifest validator、校准 runner、agreement/confusion/repeat stability、objective/judge 状态矩阵、结构化 comparison safety 与兼容读取 |
| `out_of_scope` | C10 质量阈值/退出码门禁、生产 prompt/citation/重答、默认开启 judge、自动模型选择、judge fallback、修改 v2 dataset、真实 generation baseline、no-answer judge、逐 claim LLM entailment |
| `unknown` | 具体 judge provider/model、费用和限流；当前 `0.70` 候选阈值在 live calibration 上的 agreement；24 条开发校准集能否覆盖未来模型偏差；这些必须由独立授权的真实 evidence 回答 |

## Scope

### 1. Versioned judge contract

- 把 judge rubric/prompt、parser、score threshold、joint pass 规则和输入裁剪策略作为固定 `judgeContractConfig` 写入 plan、run metadata、Markdown report 与 details JSON。
- 初始 contract 使用 `rag-judge-v1`、严格 JSON parser、faithfulness/relevance 双分数以及现有 `0.70` 候选阈值；阈值只是 calibration candidate，不是 C10 quality gate。
- provider 自报 `pass` 只作为诊断字段，规范 `judgePass` 由两个已验证 score 和 tracked threshold 确定性派生；缺失、非数值或越界 score 均为 `invalid_judge_payload`，不再静默 clamp。
- 不同 prompt/parser/threshold/model/temperature/max-context identity 的 judge 结果不得直接比较，也不得借共享 dataset 名称推断一致性。

### 2. Independent calibration corpus and evidence

- 新增独立于 `rag-eval-dev-v2` 的 `judge-calibration-v1` manifest 和 24 条静态 calibration cases；不修改 v1/v2 question、annotation、fixture、review 或默认 manifest。
- 24 条 case 按人工 gold 的 `faithful × relevant` 四象限各 6 条，覆盖中英文、单/多 claim、直接回答、支持但答非所问、相关但含无证据断言、无关且无证据等可解释边界。
- case 的 context 必须从 tracked fixture 通过 repo-relative source + exact contains 确定性解析；question、synthetic answer、gold labels、review status 和 evidence refs 进入 tracked calibration artifact，不依赖 backend 或 generation provider。
- validator 在任何 judge 调用前校验 manifest path/hash/count/order、四象限 quota、fixture grounding、ID uniqueness、gold consistency 与 review completeness。
- 新增独立 calibration runner；canary 固定 4 条各象限 1 条、1 repeat，full 固定 24 条、3 repeats。报告 parse coverage、per-dimension confusion/agreement、joint pass agreement、provider-pass disagreement 与 per-case repeat consistency；不自动调 threshold，也不输出生产 go/no-go。

### 3. Objective, judge and global status semantics

- 新增 `objectiveMetricStatus=COMPLETE / PARTIAL / RETRIEVAL_ONLY / FAILED`，只由 login、retrieval、ask、generation/citation/no-answer 和 C9a objective completeness 决定，不受 judge error 或 judge score 高低影响。
- 新增 `judgeMetricStatus=COMPLETE / PARTIAL / SKIPPED / NOT_APPLICABLE`：judge 关闭为 `SKIPPED`；没有 answerable eligible samples 为 `NOT_APPLICABLE`；显式开启且覆盖全部 eligible samples、payload 均有效为 `COMPLETE`；任何调用、解析或 coverage 缺口为 `PARTIAL`。
- 保留全局 `Report status=CLEAN / PARTIAL / RETRIEVAL_ONLY / FAILED`：objective failed/retrieval-only/partial 沿用原语义；judge 显式开启且 `PARTIAL` 时全局为 `PARTIAL`；judge `SKIPPED/NOT_APPLICABLE` 不阻止 objective-complete run 为 `CLEAN`。
- 新增结构化 per-channel comparison safety。judge failure 不能把干净 objective metrics 降成“只剩 retrieval 可比较”，judge pass/fail 的质量结果也不能改变通道完整性状态。

### 4. Compatibility, documentation and implementation boundary

- direct runner 作为正常评测 status/aggregation 的唯一实现；reproducible runner 只传递同一 judge contract identity，不复制状态算法。
- calibration runner 与 direct runner 复用同一 judge prompt/parser/config；不得维护两份隐式 rubric。
- 历史 report/details 缺少 C9b 字段时继续可读，解释为 `judge contract/status unavailable`，不能回填为 `SKIPPED`、`PARTIAL` 或 0。
- 更新 `docs/eval/RAG_EVAL_GUIDE.md`、project/architecture/technical debt/optimization index，明确 objective、judge、calibration 和 C10 gate 的边界。
- 实现按 TDD 纵向切片推进；planning 和 offline implementation 均不触发真实 judge/provider。

## Non-goals

- 不把 LLM judge 设为默认开启，不在凭据缺失时自动换模型、fallback 或 mock。
- 不修改 Java API、生产 QA prompt、citation validator、retrieval、chunking、rerank、no-answer、数据库、前端或 SSE。
- 不修改 C9a claim splitter/tokenizer/`0.70` lexical threshold，也不使用 judge 覆盖 objective claim 结论。
- 不修改 `rag-eval-dev-v1/v2` release identity，不把 calibration cases 混入正式 generation/retrieval 分母。
- 不定义 judge agreement 最低通过线、profile gate、CI 阻断阈值或通用非零质量退出码；这些属于 C10。
- 不做 no-answer judge、逐 claim NLI/entailment judge、自动 prompt 搜索、模型排名或生产 SLA 外推。

## External Call Gate

规划和 offline implementation 的真实业务外调预算为 0：embedding=0、rerank=0、debug retrieval=0、ask/generation=0、judge=0、其他 provider=0、数据出站=0。

完整 C9b live calibration 必须另行授权：

- canary：4 cases × 1 repeat，最多 4 次 judge 调用；
- full：24 cases × 3 repeats，最多 72 次 judge 调用；
- 若 canary 与 full 都执行，合计上限 76 次 judge 调用；
- embedding、rerank、backend debug retrieval、ask 和 generation 均为 0；
- 出站内容包括 tracked calibration question、synthetic answer 与从三份 tracked fixture 确定性解析的 context excerpt；不包含用户知识库、凭据或生产数据；
- 执行前必须固定 provider/model、endpoint、temperature、timeout、无重试或明确重试上限、prompt/parser/threshold identity、费用或零费用依据、rate limit、raw response 保存边界和 no-overwrite 路径，并再次取得用户授权。

## Planning Approval Outcome

- 用户于 2026-07-23 完成审阅并批准 24-case 四象限校准集、3 repeats、strict parser、score-derived pass、objective/judge/global status matrix、15 条决策记录、4 requirements / 12 scenarios、external-call gate 与 non-goals。
- 当前批准只完成 planning gate；offline TDD implementation 仍等待用户明确授权。
- Live canary/full judge calibration 继续保持未授权，不能由 planning 或 implementation approval 自动放开。

## Acceptance Criteria

### Planning gate

- proposal、design、tasks 与 `evaluation` spec delta 齐全且一致。
- design 明确 calibration corpus、gold labels、judge contract identity、严格解析、repeat、agreement、状态矩阵、兼容性、安全与 C10 边界。
- 用户批准 24-case/四象限/3-repeat 方案、决策记录、delta 与实现授权前，不修改 runner、tests、calibration artifacts、guide 或 baseline spec。

### Offline implementation gate

- RED tests 先覆盖 invalid payload、provider pass 冲突、identity drift、四象限/quota/grounding/review drift、judge all-error、partial coverage、judge off、no-answer-only、legacy report 与 comparison safety。
- direct/reproducible/calibration runner 复用唯一 judge contract；normal eval 的 objective/judge/global statuses 与 calibration metrics 都进入报告和 details。
- Python 全量、calibration validator、direct/reproducible/calibration plan-only、SensitiveLogs、链接、受保护路径、secret/绝对路径与 `git diff --check` 通过。
- 未获 live authorization 时真实 judge/provider 调用和数据出站保持 0，change 只能声称 offline implementation ready，不能声称 judge 已校准。

### Live calibration gate

- 用户单独批准 provider/model、4-call canary 与最多 72-call full 预算后才能执行。
- canary/full 固定 calibration manifest、case order、Git HEAD、judge contract/model config；任何 case 缺失、payload error、identity drift 或 repeat 缺口都使 calibration status `PARTIAL/NOT_COMPARABLE`，不得只比较成功子集。
- 报告完整呈现 confusion/agreement/repeat stability 与误判 case IDs，由用户审阅后决定是否接受当前 contract；C9b 不自动调 threshold 或建立 C10 gate。

### Acceptance and closeout gate

- 用户验收 calibration corpus、judge contract、live evidence、status/comparison semantics 与结论边界。
- 已批准 delta 原文接受进 `openspec/specs/evaluation/spec.md`。
- change 归档并恢复 `.ai/ACTIVE_TASK.md=IDLE`。
- C9b 验收不表示 production faithfulness、通用 judge 可靠性、C10 quality gate 或默认开启 judge 已完成。

## Risks

- 24 条静态 case 只能校准本项目开发 rubric，不能代表生产问题分布；通过独立 identity、四象限和明确外推边界控制误用。
- LLM 在 temperature=0 时仍可能存在 provider 侧非确定性；通过 3 repeats 与 per-case consistency 显式暴露，而不是假设确定性。
- `0.70` 可能不是最佳阈值；C9b 报告 agreement 而不自动调参，避免在小校准集上过拟合并越界进入 C10。
- prompt、model 或 parser 任一变化都会破坏可比较性；通过完整 judge contract identity 和 fail-fast mismatch 控制。
- calibration artifact 包含 synthetic answer 与 fixture excerpt 定位信息；aggregate 不复制正文，raw response/details 继续按本地敏感 evidence 管理。

## Rollback

- C9b 只计划 additive Python evaluation/calibration artifacts、状态字段与文档；回滚可恢复旧 report status 计算并保留 C9a objective metrics。
- 不涉及数据库迁移、Java API、生产配置或 dataset v2 变更，不需要业务数据恢复。
- 已生成的 calibration evidence 不回写；judge contract 变化必须创建新 version，不能覆盖旧报告后继续比较。
