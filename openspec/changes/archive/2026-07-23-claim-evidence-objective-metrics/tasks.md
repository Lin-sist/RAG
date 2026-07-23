# Tasks: C9a Claim Evidence Objective Metrics

## 0. Approval And Boundary

- [x] 用户要求检查 readiness；闸门通过后直接启动 C9a 规划。
- [x] readiness 复核：C8b 已验收归档，HEAD=`1577aab`，启动前工作区干净，`ACTIVE_TASK=IDLE`，没有未归档 change。
- [x] C9a 单 change 边界锁定为 deterministic claim extraction、validated citation evidence、objective lexical alignment、per-sample/aggregate metrics；C9b/C10 继续串行等待。
- [x] 提交责任为 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- [x] 规划阶段真实 embedding/rerank/ask/judge/LLM/provider 调用量和数据出站均为 0。
- [x] 创建 proposal、design、tasks 与 `evaluation` spec delta；baseline spec、runner、tests、dataset 与 guide 暂不修改。
- [x] 用户批准 proposal 的 scope、non-goals、initial threshold、denominator、status 与 external-call gate。
- [x] 用户批准 design 的 12 条决策记录。
- [x] 用户批准 `evaluation` spec delta 的 4 requirements / 12 scenarios。
- [x] 用户明确授权进入 TDD 实现；该授权不包含真实 generation/judge/provider run。

## 1. Claim Splitter Contract

- [x] RED：中英文句末、分号、段落和常见列表项按稳定顺序拆分。
- [x] RED：空白、纯编号、纯标点、Markdown heading marker 和孤立 citation marker 不形成 claim。
- [x] RED：非空 eligible answer 抽取 0 claim 时产生稳定 `empty_claim_set`，不能报告 COMPLETE。
- [x] RED：no-answer、retrieval-only、ask error、empty answer 分别得到 NOT_APPLICABLE/SKIPPED/PARTIAL 语义。
- [x] GREEN：实现纯本地 deterministic splitter、claimIndex/hash 与版本 identity。

## 2. Eligible Evidence And Matcher

- [x] RED：未通过 citation identity/snippet provenance 的 citation 不进入 eligible evidence。
- [x] RED：无 eligible citation 的 claim 进入分母并记 `no_eligible_evidence`。
- [x] RED：normalized exact、0.70 token boundary、少于 2 token、below-threshold 与中英文 tokenizer 行为被锁定。
- [x] RED：多 evidence 使用 method、coverage、citationIndex 的稳定 tie-break。
- [x] GREEN：复用既有 citation provenance 与 tokenizer，实现 exact/token_overlap/unsupported attribution。
- [x] GREEN：算法 config 固定为 `claim-lexical-v1`，不允许环境变量静默改 threshold。

## 3. Per-sample And Aggregate Metrics

- [x] RED：per-sample 输出 claim total、supported/unsupported、exact/token、eligible evidence、claims[] 与稳定 reason。
- [x] RED：aggregate 分母包含所有抽取 claim，partial sample 不会被静默删除后得到 COMPLETE。
- [x] RED：SKIPPED、NOT_APPLICABLE、PARTIAL、COMPLETE 在混合 answerable/no-answer 选集下稳定。
- [x] RED：旧 details/report 缺 C9a 字段时仍可读取，不被解释为 0。
- [x] GREEN：扩展 `SampleResult`、details JSON、Markdown report、console summary 与 aggregate。
- [x] GREEN：保持现有 keyword/citation/no-answer/judge 公式和全局 Report status 不变。

## 4. Runner Reuse And Documentation

- [x] direct runner 成为唯一 C9a 计算实现；reproducible runner 只复用 child command/metadata。
- [x] direct/reproducible plan 继续准确披露 ask/judge 调用量，不因 C9a 自动开启 ask。
- [x] 更新 `docs/eval/RAG_EVAL_GUIDE.md`：指标定义、算法 identity、状态、raw details 与结论边界。
- [x] 明确 lexical alignment 不等于 entailment、完整事实正确性、faithfulness 或 judge calibration。
- [x] 明确 C9a 不改变 v1/v2 dataset release，不覆盖历史报告，不建立 C10 quality gate。

## 5. Offline Verification

- [x] 运行 C9a splitter/matcher/aggregate/report 聚焦 tests：direct 36 tests / OK，reproducible 27 tests / OK。
- [x] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`：114 tests / OK。
- [x] 分别验证 v1/v2 dataset release，并运行 direct/reproducible plan-only；均为 `VALID`，实际业务调用为 0。
- [x] 运行 SensitiveLogs 与定向 answer/claim/snippet/secret/absolute-path 普通输出扫描：308 source files / PASS，定向 secret 无命中。
- [x] 运行 `git diff --check`、断链、受保护路径、tracked secret 与历史 report 非覆盖检查：全部通过。
- [x] Java/POM/前端无改动，Maven、frontend build、Docker/live provider 记为 `SKIPPED`。
- [x] 实现阶段真实 embedding/rerank/ask/judge/LLM/provider 调用量和数据出站保持 0。

## 6. Optional Real Evidence Gate

- [x] `SKIPPED`：未获真实 run 授权，因此本 change 未固定运行时 KB/doc、provider/model 等 live identity；执行前置要求保留给后续独立 evidence 任务。
- [x] `SKIPPED`：未发起 150 条真实 run，调用量、出站、模型、费用与限流披露继续作为执行前授权闸门。
- [x] `SKIPPED`：未产生 live artifact，timeout、retry、rate limit、error category、no-overwrite 与出站记录不适用；规则继续保留。
- [x] 未获授权时本节保持 `SKIPPED`，不阻塞离线 C9a 实现验收；本轮真实 evidence 明确 `SKIPPED`。

## 7. Acceptance And Closeout

- [x] 更新 proposal/design/tasks 的实际实现、验证、跳过项、风险与 external call facts。
- [x] 同步 `openspec/project.md`、architecture、technical debt、optimization index 与 `.ai/AGENT_LOG.md`。
- [x] 用户验收 claim contract、evidence attribution、aggregate/status、兼容性与结论边界。
- [x] 用户验收后将 approved delta 原文接受进 `openspec/specs/evaluation/spec.md`。
- [x] 恢复 `.ai/ACTIVE_TASK.md=IDLE` 并归档 change。
- [x] C9a 收口只确认 objective lexical alignment，不确认 C9b judge calibration、faithfulness 或 C10 quality gate。
