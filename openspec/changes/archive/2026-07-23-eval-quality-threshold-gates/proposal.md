# Proposal: C10 Eval Quality Threshold Gates

## Summary

为版本化 RAG 评测结果建立确定性的质量门禁：用 tracked profile 声明适用的数据 release、指标通道、切片、阈值、容差、缺失值与错误策略；由独立离线 evaluator 读取已生成的 details JSON，输出结构化 gate result 和稳定退出码。

本 change 先交付零外调的 contract、schema、evaluator、测试与 CLI；首个可激活的数值 profile 限于 `rag-eval-dev-v2` 的 retrieval-only development regression gate，具体阈值必须基于后续单独授权、固定身份的 v2 全量 reference evidence，并经用户审阅后写入。Generation/objective 与 judge profile 只建立兼容边界，不在缺少真实 evidence 时激活。

## Why Now

- C8a/C8b 已固定并验收 `rag-eval-dev-v1/v2` 的 release identity、schema、annotation、review 与 fail-fast validation。
- C9a 已提供 objective lexical claim metric、完整分母与独立局部状态；C9b 已提供 objective/judge/global status、per-channel comparison safety 与 judge contract identity。
- 当前 direct runner 只有 `--fail-on-ask-errors`、`--fail-on-judge-errors` 这类执行错误开关；低于质量目标仍返回 0，也没有版本化 profile、per-type/per-difficulty gate、容差或稳定 gate result。
- 路线图明确 C10 是 C9b 之后的下一阶段，目标是按 profile/类别/指标通道定义阈值并以非零退出码形成门禁。

## Readiness And Capability Classification

- `confirmed`：工作区干净，`main` 相对 `origin/main` ahead 7；`.ai/ACTIVE_TASK.md=IDLE`；C9a/C9b delta 已接受进 `evaluation` baseline 并归档；默认 dataset 是 150 条 v2。
- `confirmed`：details JSON 已包含 dataset identity、run metadata、global/objective/judge status、comparison safety、aggregate metrics 与逐样本 metric calculation details，可作为离线 evaluator 的输入。
- `partial`：已有错误型退出码与 aggregate metrics，但没有质量 profile、slice aggregates、阈值判定或可区分的 gate exit code。
- `planned`：tracked profile schema、profile identity、type/difficulty/answerability slices、hard threshold 与 reference regression tolerance、fail-closed completeness、脱敏 gate summary、稳定退出码。
- `out_of_scope`：修改 dataset/fixture、prompt、chunking、retrieval/rerank/citation/no-answer/judge 公式、生产默认值、自动调参、CI 平台配置、C11+、C14。
- `unknown`：v2 全量 retrieval reference 的实际指标与波动、真实 query embedding provider/费用/限流；在 evidence 授权与运行前不得编造阈值。

## Goals

1. 定义 `rag-quality-gate-profile-v1`，固定 profile id/version/status、dataset release、sample selection、run/metric identity、required channel、slice、threshold、tolerance、missing/error policy 与 profile hash。
2. 提供独立离线 evaluator，对 details JSON 做 identity/completeness validation，按 `all/type/difficulty/answerability` 确定性重算门禁指标并输出逐规则结果。
3. 区分 `PASS`、`FAIL`、`NOT_EVALUABLE` 与 `INVALID`；低质量与证据不完整不得混为一类，CLI 使用稳定退出码。
4. 支持 hard floors/ceilings 与锁定 reference evidence 的绝对回退容差；任何 tolerance 不得豁免 error、status、identity 或 denominator completeness。
5. 保持 objective 与 judge 通道隔离；judge 未启用或未校准不得阻塞 retrieval-only profile，也不得被解释为 judge quality 通过。
6. 用合成 details fixture 完成 offline TDD；实际 embedding/rerank/debug retrieval/ask/generation/judge/provider 调用与数据出站保持 0。
7. 将首个 active retrieval profile 的 evidence 生成、阈值选择与激活置于后续独立授权/审阅闸门。

## Non-Goals

- 不在 planning/offline implementation 阶段运行 backend、创建/修改 KB 或调用任何 provider。
- 不使用 C7 旧 30 条报告追认 v2 质量门禁，也不把 C9a 的词法指标或 C9b 静态 calibration corpus 当成真实 generation/judge baseline。
- 不自动从当前 candidate run 学习阈值，不为了通过门禁修改评测集、prompt、分块、检索、rerank、citation、no-answer 或 judge 行为。
- 不启用默认 judge，不切换默认 reranker，不宣称 production SLA、production quality 或多租户隔离已完成。
- 不新增/升级依赖，不修改 Java/API/DTO/数据库/前端/生产配置。

## Proposed Scope

### 1. Versioned Gate Profile

- 新增 JSON Schema 与 tracked profiles 目录。
- profile 只允许固定 axes/operator，禁止任意表达式或可执行代码。
- `DRAFT` profile 可校验但不可返回 `PASS`；只有 evidence 与用户审阅完成后才能切为 `ACTIVE`。

### 2. Offline Gate Evaluator

- 输入：一个 tracked profile、一个 local details JSON，可选锁定 reference gate summary。
- 输出：脱敏 JSON/Markdown summary；只含 identity、slice、metric、denominator、observed/target/tolerance/result/reason，不复制 question、answer、claim、citation、context、凭据或绝对路径。
- 结果与退出码：`PASS=0`、`FAIL=3`、`NOT_EVALUABLE=4`、profile/input contract invalid=`2`；未捕获运行异常保留 `1`。

### 3. Slice And Threshold Semantics

- 支持 overall、`type`、`difficulty`、`answerability` 固定切片。
- profile 为每条规则声明 channel、metric、operator、target、minimum denominator 与 required/optional。
- hard threshold 与 reference regression rule 同时存在时必须全部通过；missing/partial/incompatible 不得填 0 或缩小分母。

### 4. Initial Profile Activation Gate

- offline implementation 只提交 schema/evaluator/fixtures/tests 与 `DRAFT` retrieval profile shape。
- 若后续要激活首个 v2 retrieval profile，先 plan/preflight 并单独披露、授权最多 3×150=450 次 debug retrieval；可能最多 450 次 query embedding，heuristic reranker 下 external rerank/ask/generation/judge 为 0。实际 provider/model、数据出站、费用/零费用依据、限流、timeout/retry 和 raw artifact 策略须在执行前重新确认。
- reference evidence 必须 `VALID`、full v2/150、固定 config/KB/Git identity、无 retrieval error、无 sample 缺口；任何 identity drift 或外调异常立即停止，不从成功子集定阈值。
- hard floor、overall/category tolerance 的具体数值在 evidence 产生后由用户审阅决定；未确认前 profile 保持 `DRAFT`，C10 不得声称 active quality gate 已完成。

## Risks And Mitigations

- **小切片波动导致误报**：每条 category rule 必须声明 minimum denominator；不足时 `NOT_EVALUABLE`，不能当 0 或 PASS。
- **把执行失败误判为质量差**：status/error/identity 不完整产生 `NOT_EVALUABLE`；只有完整可比较 evidence 低于阈值才是 `FAIL`。
- **阈值过拟合当前 run**：阈值不自动学习；reference identity、三次 repeat、hard floor 与 regression tolerance 分离，并由用户审阅。
- **judge 污染 objective/retrieval**：profile 显式声明 required channel；未要求 judge 的 profile 不读取 judge 质量结论。
- **历史 evidence 被追认**：未版本化、缺 C8/C9 identity 或 selection 不完整的输入一律不可用于 active profile。
- **raw evidence 泄露**：evaluator 只读本地 details，普通输出做 allowlist，不复制原文与 secret-bearing 字段。

## Acceptance Criteria

1. profile schema/validator 对 identity、dataset、selection、slice、operator、threshold、tolerance、status 与 hash 漂移 fail closed。
2. evaluator 对相同输入重复产生相同 gate result；overall/type/difficulty/answerability 的分母和指标与手算 fixture 一致。
3. `PASS/FAIL/NOT_EVALUABLE/INVALID` 语义与 `0/3/4/2` 退出码被单元测试锁定。
4. required metric missing、channel partial、dataset `UNVERSIONED`、identity mismatch、denominator 不足均不能返回 PASS；完整 evidence 低于阈值才返回 FAIL。
5. gate output 不含 raw question/answer/context/claim/citation、凭据、Authorization 或绝对本地路径。
6. Python 全量、SensitiveLogs、Markdown 链接、受保护路径与 `git diff --check` 通过；Java/POM/前端无改动时对应验证明确 `SKIPPED`。
7. 首个 profile 只有在 reference evidence 单独授权、完整通过并由用户确认具体阈值后才可从 `DRAFT` 切换为 `ACTIVE`。

## Submission Responsibility

- `用户手动提交`。
- Agent 不暂存、不提交、不 push、不创建 PR、不部署。

