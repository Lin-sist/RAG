# Tasks: C10 Eval Quality Threshold Gates

## 0. Approval And Boundary

- [x] 用户要求检查 C10 readiness，允许则开始规划。
- [x] readiness：启动前 HEAD=`c246929`，工作区干净，`main...origin/main [ahead 7]`，`ACTIVE_TASK=IDLE`，C9a/C9b 已接受 baseline 并归档，无其他 active change。
- [x] 提交责任为 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- [x] 创建 proposal、design、tasks 与 `evaluation` spec delta，并激活 `.ai/ACTIVE_TASK.md`。
- [x] 规划阶段真实 embedding/rerank/debug retrieval/ask/generation/judge/LLM/provider 调用与数据出站为 0。
- [ ] 用户批准 proposal 的两道闸、profile/status/exit code 语义、initial retrieval-only 边界与 non-goals。
- [ ] 用户批准 design 的 15 条决策记录及 `evaluation` delta 的 4 requirements / 12 scenarios。
- [ ] 用户明确授权进入 offline TDD implementation；该授权默认不包含 v2 reference evidence 或任何 live/provider 调用。

## 1. Profile Contract And Validation

- [ ] RED：schema version、profile id/version/status、dataset/selection/channel/slice/rule/operator/tolerance/hash 缺失或漂移均产生稳定 invalid code。
- [ ] RED：DRAFT、UNVERSIONED dataset、selection 不完整、identity 不匹配不得返回 PASS。
- [ ] GREEN：新增 `rag-quality-gate-profile-v1` schema、loader、canonical identity 与 DRAFT retrieval profile shape。
- [ ] GREEN：profile 仅允许 `all/type/difficulty/answerability` 与受支持 operator，不执行任意表达式。

## 2. Deterministic Slice Metrics

- [ ] RED：overall、type、difficulty、answerability 的 selection/denominator 与手算 fixture 一致。
- [ ] RED：required metric missing、denominator 不足、partial channel、error 超预算均为 `NOT_EVALUABLE`，不得填 0 或删除失败样本。
- [ ] GREEN：按 sample id 连接 profile 固定的 versioned dataset annotation，并对逐样本 calculation details 确定性聚合。
- [ ] GREEN：支持 retrieval/objective/judge channel，但只评估 profile 明确声明且 identity/status 完整的规则。

## 3. Thresholds Tolerance And Results

- [ ] RED：`minInclusive/maxInclusive` hard rule、reference `maxAbsoluteRegression`、hard+reference AND 语义稳定。
- [ ] RED：tolerance 不能豁免 identity、status、error、missing、denominator 或 zero-tolerance safety count。
- [ ] RED：`PASS/FAIL/NOT_EVALUABLE/INVALID` 分别返回 `0/3/4/2`，未分类 runtime error 保持 1。
- [ ] GREEN：实现 rule evaluator、safe reason codes、gate summary JSON/Markdown 与 no-overwrite。

## 4. Compatibility And Safety

- [ ] 历史 details 缺 C8/C9/C10 identity 时仍可读取，但只得到明确 `NOT_EVALUABLE/INVALID`，不得回填或追认为 PASS。
- [ ] aggregate/普通输出不复制 question、answer、claim、citation、context、provider body、secret、Authorization 或绝对路径。
- [ ] 更新 `docs/eval/RAG_EVAL_GUIDE.md` 的两步运行、profile versioning、退出码、CI 示例、raw artifact 与 external-call boundary。
- [ ] 明确 C10 不改变 dataset、指标公式、production QA、默认 judge/reranker、prompt/citation/no-answer 或 C11+。

## 5. Offline Verification

- [ ] 按纵向 TDD 切片记录 RED→GREEN 证据。
- [ ] 运行 `scripts/test_evaluate_quality_gate.py` 与 direct/reproducible 相关聚焦 tests。
- [ ] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`。
- [ ] 用合成 details fixture 覆盖 PASS、FAIL、NOT_EVALUABLE、INVALID；实际业务调用为 0。
- [ ] 运行 SensitiveLogs、Markdown 链接、secret/absolute-path、受保护路径、历史 report 非覆盖与 `git diff --check`。
- [ ] Java/POM/前端无改动时，Maven、frontend build、Docker/live backend 记为 `SKIPPED`。

## 6. Reference Evidence And Profile Activation Gate

- [ ] `PENDING USER AUTHORIZATION`：执行前确认 provider/model、固定 KB/config/Git/dataset identity、数据出站、费用/零费用依据、限流、timeout/retry 与 raw artifact 策略。
- [ ] 推荐预算待确认：full v2 150 条 × 3 repeats = 最多 450 debug retrieval；可能最多 450 query embedding；heuristic reranker 下 external rerank/ask/generation/judge=0。
- [ ] 先运行 plan/preflight；任何 dataset/KB/config/Git drift、retrieve error、rate limit、fallback 或 excess calls 立即停止，不保留成功子集定阈值。
- [ ] 生成锁定 reference summary，提出 overall/type/difficulty/answerability hard floors 与 regression tolerances；不得自动修改 profile 为 ACTIVE。
- [ ] 用户审阅并明确确认具体阈值、容差与 profile identity 后，才将 retrieval profile 从 `DRAFT` 切为 `ACTIVE`。
- [ ] Generation/objective 与 judge profile 在各自真实 evidence 不足时保持未激活，不阻塞 retrieval profile，也不产生相应质量结论。

## 7. Acceptance And Closeout

- [ ] 用户验收 offline evaluator、profile contract、slice/threshold/status/exit code、安全与兼容性。
- [ ] 用户验收首个 ACTIVE profile 的 reference evidence、具体阈值与容差；若 reference gate 未授权或未完成，C10 不得以“active quality gate 完成”归档。
- [ ] 将 approved delta 原文接受进 `openspec/specs/evaluation/spec.md`。
- [ ] 同步 project/architecture/technical debt/optimization index 与 `.ai/AGENT_LOG.md`。
- [ ] 恢复 `.ai/ACTIVE_TASK.md=IDLE` 并归档 change。

