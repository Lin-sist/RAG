# Tasks: C9a Claim Evidence Objective Metrics

## 0. Approval And Boundary

- [x] 用户要求检查 readiness；闸门通过后直接启动 C9a 规划。
- [x] readiness 复核：C8b 已验收归档，HEAD=`1577aab`，启动前工作区干净，`ACTIVE_TASK=IDLE`，没有未归档 change。
- [x] C9a 单 change 边界锁定为 deterministic claim extraction、validated citation evidence、objective lexical alignment、per-sample/aggregate metrics；C9b/C10 继续串行等待。
- [x] 提交责任为 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- [x] 规划阶段真实 embedding/rerank/ask/judge/LLM/provider 调用量和数据出站均为 0。
- [x] 创建 proposal、design、tasks 与 `evaluation` spec delta；baseline spec、runner、tests、dataset 与 guide 暂不修改。
- [ ] 用户批准 proposal 的 scope、non-goals、initial threshold、denominator、status 与 external-call gate。
- [ ] 用户批准 design 的 12 条决策记录。
- [ ] 用户批准 `evaluation` spec delta 的 4 requirements / 12 scenarios。
- [ ] 用户明确授权进入 TDD 实现；该授权不包含真实 generation/judge/provider run。

## 1. Claim Splitter Contract

- [ ] RED：中英文句末、分号、段落和常见列表项按稳定顺序拆分。
- [ ] RED：空白、纯编号、纯标点、Markdown heading marker 和孤立 citation marker 不形成 claim。
- [ ] RED：非空 eligible answer 抽取 0 claim 时产生稳定 `empty_claim_set`，不能报告 COMPLETE。
- [ ] RED：no-answer、retrieval-only、ask error、empty answer 分别得到 NOT_APPLICABLE/SKIPPED/PARTIAL 语义。
- [ ] GREEN：实现纯本地 deterministic splitter、claimIndex/hash 与版本 identity。

## 2. Eligible Evidence And Matcher

- [ ] RED：未通过 citation identity/snippet provenance 的 citation 不进入 eligible evidence。
- [ ] RED：无 eligible citation 的 claim 进入分母并记 `no_eligible_evidence`。
- [ ] RED：normalized exact、0.70 token boundary、少于 2 token、below-threshold 与中英文 tokenizer 行为被锁定。
- [ ] RED：多 evidence 使用 method、coverage、citationIndex 的稳定 tie-break。
- [ ] GREEN：复用既有 citation provenance 与 tokenizer，实现 exact/token_overlap/unsupported attribution。
- [ ] GREEN：算法 config 固定为 `claim-lexical-v1`，不允许环境变量静默改 threshold。

## 3. Per-sample And Aggregate Metrics

- [ ] RED：per-sample 输出 claim total、supported/unsupported、exact/token、eligible evidence、claims[] 与稳定 reason。
- [ ] RED：aggregate 分母包含所有抽取 claim，partial sample 不会被静默删除后得到 COMPLETE。
- [ ] RED：SKIPPED、NOT_APPLICABLE、PARTIAL、COMPLETE 在混合 answerable/no-answer 选集下稳定。
- [ ] RED：旧 details/report 缺 C9a 字段时仍可读取，不被解释为 0。
- [ ] GREEN：扩展 `SampleResult`、details JSON、Markdown report、console summary 与 aggregate。
- [ ] GREEN：保持现有 keyword/citation/no-answer/judge 公式和全局 Report status 不变。

## 4. Runner Reuse And Documentation

- [ ] direct runner 成为唯一 C9a 计算实现；reproducible runner 只复用 child command/metadata。
- [ ] direct/reproducible plan 继续准确披露 ask/judge 调用量，不因 C9a 自动开启 ask。
- [ ] 更新 `docs/eval/RAG_EVAL_GUIDE.md`：指标定义、算法 identity、状态、raw details 与结论边界。
- [ ] 明确 lexical alignment 不等于 entailment、完整事实正确性、faithfulness 或 judge calibration。
- [ ] 明确 C9a 不改变 v1/v2 dataset release，不覆盖历史报告，不建立 C10 quality gate。

## 5. Offline Verification

- [ ] 运行 C9a splitter/matcher/aggregate/report 聚焦 tests。
- [ ] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`。
- [ ] 分别验证 v1/v2 dataset release，并运行 direct/reproducible plan-only；实际业务调用为 0。
- [ ] 运行 SensitiveLogs 与定向 answer/claim/snippet/secret/absolute-path 普通输出扫描。
- [ ] 运行 `git diff --check`、断链、受保护路径、tracked secret 与历史 report 非覆盖检查。
- [ ] Java/POM/前端无改动时，将 Maven、frontend build、Docker/live provider 记为 `SKIPPED`。
- [ ] 实现阶段真实 embedding/rerank/ask/judge/LLM/provider 调用量和数据出站保持 0。

## 6. Optional Real Evidence Gate

- [ ] 在任何真实 run 前固定 v2 release、KB/doc identity、Git HEAD、tracked config、LLM provider/model 与 claimMetricConfig。
- [ ] 单独披露并取得用户授权：一次 150 条上限为 150 debug retrieval、150 ask、至多 300 query embedding、至多 150 generation、judge=0；默认 heuristic 时外部 rerank model=0。
- [ ] 记录 timeout、retry、rate limit、error categories、raw artifact no-overwrite 与数据出站；异常不得通过删除样本形成 CLEAN evidence。
- [ ] 未获授权时本节保持 `SKIPPED`，不阻塞离线 C9a 实现验收。

## 7. Acceptance And Closeout

- [ ] 更新 proposal/design/tasks 的实际实现、验证、跳过项、风险与 external call facts。
- [ ] 同步 `openspec/project.md`、architecture、technical debt、optimization index 与 `.ai/AGENT_LOG.md`。
- [ ] 用户验收 claim contract、evidence attribution、aggregate/status、兼容性与结论边界。
- [ ] 用户验收后将 approved delta 原文接受进 `openspec/specs/evaluation/spec.md`。
- [ ] 恢复 `.ai/ACTIVE_TASK.md=IDLE` 并归档 change。
- [ ] C9a 收口只确认 objective lexical alignment，不确认 C9b judge calibration、faithfulness 或 C10 quality gate。
