# Tasks: C8b Eval Dataset Expansion And Annotation

## 0. Approval And Boundary

- [x] 用户要求开启 C8b 规划。
- [x] readiness 复核：C8a 已验收归档，`83912d2` 存在，启动前工作区干净，`ACTIVE_TASK=IDLE`，没有未归档 change。
- [x] C8b 单 change 边界锁定为 question/annotation 扩充、quota、review evidence 与 v2 release；C9/C10/C14 继续串行等待。
- [x] 提交责任为 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- [x] 规划阶段真实 embedding/rerank/ask/judge/LLM/provider 调用量为 0，数据出站为 0。
- [x] 规划 artifacts 结构验证：proposal/design/tasks/spec delta 共 4 文件，design 18 条决策，delta 4 requirements / 12 scenarios。
- [x] 规划范围验证：`ACTIVE_TASK=ACTIVE` 且只指向本 change；baseline spec、eval JSONL、fixture、manifest/schema 与 runner 改动均为 0。
- [ ] 用户批准 proposal 的目标总量、scope、non-goals、quota、corpus boundary 与 acceptance criteria。
- [ ] 用户批准 design 的 18 条决策记录。
- [ ] 用户批准 `evaluation` spec delta 的 requirements / scenarios。
- [ ] 用户明确授权进入实现；若要求外部 LLM 辅助，须另行批准调用量、数据出站、模型、费用与限流风险。

## 1. Release Layout And Backward Compatibility

- [ ] RED：v1 manifest/question bytes 不可再验证或被 v2 覆盖时失败。
- [ ] RED：question/annotation/release version 未随 expanded identity 变化时失败。
- [ ] RED：fixture/schema 未变化却被无理由 bump，或实际变化却未 bump 时失败。
- [ ] 保存可显式验证的 `rag-eval-dev-v1` release artifacts。
- [ ] 新增 v2 question set 与 manifest；验收前默认 manifest 继续指向 v1。
- [ ] 若批准 review descriptor，新增 manifest schema v2 并保持 runner additive compatibility。

## 2. Quota And Seed Immutability

- [ ] RED：总量、type、difficulty、answerability 或 type×difficulty 任一 quota 漂移时失败。
- [ ] RED：原 30 条 seed 的 object、顺序、ID 或 annotation 发生变化时以 `seed_identity_mismatch` 失败。
- [ ] RED：fixture coverage 低于下限或单一 fixture 超过上限时失败。
- [ ] 实现 expanded release quota contract 与 remaining-quota 报告。
- [ ] 保持前 30 条 seed 不变，只追加获批数量的新样本。
- [ ] 新 ID 按 type prefix 从既有最大序号继续分配，不重用 seed ID。

## 3. Annotation Grounding And Review

- [ ] RED：answerable context 无法 exact/normalized-exact 命中 fixture 时失败。
- [ ] RED：multi-hop 少于两个独立 evidence points、no-answer 引用 source/context 或 review 不完整时失败。
- [ ] RED：normalized exact duplicate 被拒绝，near-duplicate candidate 缺复核结论时失败。
- [ ] 建立 source-first authoring 批次，逐批补齐 approved quota。
- [ ] 建立不复制 question/fixture 正文的机器可读 review evidence。
- [ ] 对全部 seed 与新增样本完成 structure、grounding、duplicate 和 semantic review 状态闭环。

## 4. Validator And Runner Compatibility

- [ ] 扩展共享 validator，输出安全 quota/seed/grounding/review/release identity facts。
- [ ] direct/reproducible runner 继续只消费 validated facts，不复制 C8b 规则。
- [ ] 保持 `--plan-only`、`--preflight-only`、`--keep-existing` 和 `UNVERSIONED` 既有语义。
- [ ] 保持 C7 comparator、旧 metadata/report 与 v1 release 可读取；不修改指标公式。
- [ ] 错误输出不回显 question、notes、expected content、review notes、fixture 正文、secret 或绝对路径。

## 5. Documentation And Freeze

- [ ] 更新 `docs/eval/RAG_EVAL_GUIDE.md` 的 v1/v2 选择、quota、authoring、review、freeze 与 rollback 流程。
- [ ] 明确 tracked expanded release 是开发评测集，不是隐藏 benchmark、生产分布或论文级数据集。
- [ ] 明确 C8b 不代表 C9 claim/judge、C10 quality gate 或 C14 isolation evaluation 完成。
- [ ] v2 冻结后禁止根据 provider/算法失败样本回改 question/annotation；修订必须生成后续新 version。
- [ ] 用户验收前不切换默认 manifest，不运行或发布新的正式 baseline 结论。

## 6. Offline Verification

- [ ] 运行 quota、seed、grounding、duplicate、review 与 release version 聚焦 tests。
- [ ] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`。
- [ ] 分别验证 v1 与 v2 release；确认 v2 总量、quota、seed、新增量、fixture/review identity 全部匹配。
- [ ] 运行 direct/reproducible plan-only，确认实际业务调用为 0。
- [ ] 运行 SensitiveLogs、定向 secret/user-content/absolute-path 与 review evidence 扫描。
- [ ] 运行 `git diff --check`、断链、受保护路径、tracked secret 和历史 artifact 非覆盖检查。
- [ ] Java/POM/前端无改动时记录 Maven、frontend build、Docker/live provider `SKIPPED` 及原因。
- [ ] 记录实现阶段真实 embedding/rerank/ask/judge/LLM/provider 调用量和数据出站；未获额外授权时必须为 0。

## 7. Acceptance And Closeout

- [ ] 更新 proposal/design/tasks 的实际数据构建、拒绝样本、review 与验证结果。
- [ ] 同步 `openspec/project.md`、架构、技术债、优化索引和 `.ai/AGENT_LOG.md`。
- [ ] 用户验收 expanded dataset、quota、review evidence、v1/v2 identity 与结论边界。
- [ ] 用户验收后将 approved delta 原文接受进 `openspec/specs/evaluation/spec.md`。
- [ ] 验收后切换默认 manifest 到 v2，恢复 `.ai/ACTIVE_TASK.md=IDLE` 并归档 change。
- [ ] C8b 收口后另行 readiness，再决定是否启动 C9a；不得在本 change 直接实现 claim metrics。
