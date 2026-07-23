# Tasks: C10 Eval Quality Threshold Gates

## 0. Approval And Boundary

- [x] 用户要求检查 C10 readiness，允许则开始规划。
- [x] readiness：启动前 HEAD=`c246929`，工作区干净，`main...origin/main [ahead 7]`，`ACTIVE_TASK=IDLE`，C9a/C9b 已接受 baseline 并归档，无其他 active change。
- [x] 提交责任为 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- [x] 创建 proposal、design、tasks 与 `evaluation` spec delta，并激活 `.ai/ACTIVE_TASK.md`。
- [x] 规划阶段真实 embedding/rerank/debug retrieval/ask/generation/judge/LLM/provider 调用与数据出站为 0。
- [x] 用户批准 proposal 的两道闸、profile/status/exit code 语义、initial retrieval-only 边界与 non-goals。
- [x] 用户批准 design 的 15 条决策记录及 `evaluation` delta 的 4 requirements / 12 scenarios。
- [x] 用户明确授权进入 offline TDD implementation；该授权不包含 v2 reference evidence 或任何 live/provider 调用。

## 1. Profile Contract And Validation

- [x] RED：schema version、profile id/version/status、dataset/selection/channel/slice/rule/operator/tolerance/hash 缺失或漂移均产生稳定 invalid code。
- [x] RED：DRAFT、UNVERSIONED dataset、selection 不完整、identity 不匹配不得返回 PASS。
- [x] GREEN：新增 `rag-quality-gate-profile-v1` schema、loader、canonical identity 与 DRAFT retrieval profile shape。
- [x] GREEN：profile 仅允许 `all/type/difficulty/answerability` 与受支持 operator，不执行任意表达式。

## 2. Deterministic Slice Metrics

- [x] RED：overall、type、difficulty、answerability 的 selection/denominator 与手算 fixture 一致。
- [x] RED：required metric missing、denominator 不足、partial channel、error 超预算均为 `NOT_EVALUABLE`，不得填 0 或删除失败样本。
- [x] GREEN：按 sample id 连接 profile 固定的 versioned dataset annotation，并对逐样本 calculation details 确定性聚合。
- [x] GREEN：支持 retrieval/objective/judge channel，但只评估 profile 明确声明且 identity/status 完整的规则。

## 3. Thresholds Tolerance And Results

- [x] RED：`minInclusive/maxInclusive` hard rule、reference `maxAbsoluteRegression`、hard+reference AND 语义稳定。
- [x] RED：tolerance 不能豁免 identity、status、error、missing、denominator 或 zero-tolerance safety count。
- [x] RED：`PASS/FAIL/NOT_EVALUABLE/INVALID` 分别返回 `0/3/4/2`，未分类 runtime error 保持 1。
- [x] GREEN：实现 rule evaluator、safe reason codes、gate summary JSON/Markdown 与 no-overwrite。

## 4. Compatibility And Safety

- [x] 历史 details 缺 C8/C9/C10 identity 时仍可读取，但只得到明确 `NOT_EVALUABLE/INVALID`，不得回填或追认为 PASS。
- [x] aggregate/普通输出不复制 question、answer、claim、citation、context、provider body、secret、Authorization 或绝对路径。
- [x] 更新 `docs/eval/RAG_EVAL_GUIDE.md` 的两步运行、profile versioning、退出码、CI 示例、raw artifact 与 external-call boundary。
- [x] 明确 C10 不改变 dataset、指标公式、production QA、默认 judge/reranker、prompt/citation/no-answer 或 C11+。

## 5. Offline Verification

- [x] 按纵向 TDD 切片记录 RED→GREEN 证据。
- [x] 运行 `scripts/test_evaluate_quality_gate.py` 与 direct/reproducible 相关聚焦 tests。
- [x] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`。
- [x] 用合成 details fixture 覆盖 PASS、FAIL、NOT_EVALUABLE、INVALID；实际业务调用为 0。
- [x] 运行 SensitiveLogs、Markdown 链接、secret/absolute-path、受保护路径、历史 report 非覆盖与 `git diff --check`。
- [x] Java/POM/前端无改动时，Maven、frontend build、Docker/live backend 记为 `SKIPPED`。

## 6. Reference Evidence And Profile Activation Gate

- [x] `SKIPPED`：用户未授权 reference calls，因此未选择 provider/model、固定 KB/config/Git live identity，也未产生数据出站、费用、限流、timeout/retry 或 raw reference artifact。
- [x] `SKIPPED`：未授权 full v2 150 条 × 3 repeats；实际 debug retrieval、query embedding、external rerank、ask、generation、judge/provider 调用均为 0。
- [x] `SKIPPED`：仅完成 direct/reproducible plan-only 的本地 VALID 检查；未进入 reference runtime preflight，也未从任何成功子集推断阈值。
- [x] `SKIPPED`：未生成锁定 reference summary，未提出或自动学习 hard floors / regression tolerances。
- [x] `SKIPPED`：没有具体阈值、容差或 reference identity 可供审阅，retrieval profile 保持 `DRAFT / PENDING_REFERENCE_EVIDENCE`。
- [x] Generation/objective 与 judge profile 保持未激活；本 change 只接受通用离线 contract/evaluator，不产生 retrieval、generation、citation 或 judge 质量结论。

## 7. Acceptance And Closeout

- [x] 用户验收 offline evaluator、profile contract、slice/threshold/status/exit code、安全与兼容性，并要求检查后归档。
- [x] `SKIPPED`：首个 ACTIVE profile 的 reference evidence、具体阈值与容差未授权、未产生；本次只以“offline gate framework + DRAFT profile”归档，不声称 active quality gate 完成。
- [x] 将 approved delta 原文接受进 `openspec/specs/evaluation/spec.md`。
- [x] 同步 project/architecture/technical debt/optimization index 与 `.ai/AGENT_LOG.md`。
- [x] 恢复 `.ai/ACTIVE_TASK.md=IDLE` 并归档 change。
