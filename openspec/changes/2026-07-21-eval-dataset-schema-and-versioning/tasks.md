# Tasks: C8a Eval Dataset Schema And Versioning

## 0. Approval And Boundary

- [x] 用户要求开启 C8a 规划。
- [x] readiness 复核：`main` 与 `origin/main` 同步、工作区干净、`ACTIVE_TASK=IDLE`、C7 delta 已接受归档、没有未归档 change。
- [x] C8a 单 change 边界锁定为 dataset schema/versioning；C8b 100～300 条扩充继续串行等待。
- [x] 提交责任为 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- [x] 规划阶段真实 embedding/rerank/ask/judge/LLM/provider 调用量为 0，数据出站为 0。
- [x] 规划 artifacts 结构验证：proposal/design/tasks/spec delta 共 4 文件，design 16 条决策，delta 4 requirements / 13 scenarios。
- [x] 规划范围验证：`ACTIVE_TASK=ACTIVE` 且只指向本 change；baseline spec 与受保护路径改动均为 0；`git diff --check` 和 SensitiveLogs 通过。
- [x] 用户批准 proposal 的 scope、non-goals、用户故事、version semantics 与 acceptance criteria。
- [x] 用户批准 design 的 16 条决策记录。
- [x] 用户批准 `evaluation` spec delta 的 4 个 requirements / 13 个 scenarios。
- [x] 用户明确授权进入 TDD 实现；真实业务外调仍保持为 0。

## 1. Versioned Release Contract

- [x] RED：缺少 release/question/schema/annotation/corpus version 的 manifest 被拒绝。
- [x] RED：绝对路径、`..` 越界、repo 外解析和缺失 artifact 被稳定拒绝。
- [x] RED：eval-set/schema/fixture bytes、SHA-256、count 或 ordered sample identity 漂移被拒绝。
- [x] 新增 `docs/eval/dataset-manifest.json`，固定当前 30 条 question set、3 份 fixture corpus、逻辑 KB contract 与类型分布。
- [x] 新增 `docs/eval/schema/rag-eval-sample-v1.json`，定义项目级机器可读 schema contract。
- [x] 验证首个 release 引用的 JSONL/fixture bytes 与 C7 捕获 hash 完全一致，禁止为通过校验静默改内容。

## 2. Shared Dataset Validator

- [x] RED：sample 非 object、缺字段、未知字段、非法类型/enum、duplicate ID 被稳定拒绝。
- [x] RED：answerable/no-answer 条件冲突、空必要标注和 unknown fixture source 被稳定拒绝。
- [x] RED：错误输出不回显 question、notes、expected content、fixture 正文或本机绝对路径。
- [x] 新增 `scripts/eval_dataset_contract.py`，只用标准库解释 schema/manifest并生成安全 release identity。
- [x] 实现 stable error taxonomy、repo-relative safe path、raw SHA-256、ordered sample identity 和 distribution 校验。
- [x] 为 validator 新增聚焦单元测试，覆盖合法 current release 与全部拒绝路径。

## 3. Runner Integration And Call Safety

- [x] RED：invalid/drift release 通过 direct runner 时，在 backend/provider stub 被联系前失败且 call count=0。
- [x] RED：invalid/drift release 通过 reproducible plan/preflight/run 入口时，在 login/KB/provider 前失败且 call count=0。
- [x] `run_rag_eval.py` 默认绑定 tracked manifest并记录 `datasetReleaseIdentity` / validation status。
- [x] `run_reproducible_rag_eval.py` 复用相同 validated facts，保留 plan-only/preflight-only/keep-existing 的既有语义。
- [x] 决定并实现 custom eval-set 策略：显式 `UNVERSIONED` 降级或强制独立 manifest，不允许未版本化结果形成正式 baseline。
- [x] 保持 C7 arm manifest、identity、comparison status 与既有 metadata 字段兼容；不修改指标公式。

## 4. Version Evolution And Documentation

- [x] 固化 release/question/schema/annotation/corpus bump matrix 和 immutable release 规则。
- [x] 明确同 version 不同 hash、schema 不兼容、annotation-only 变化和 fixture 变化的恢复路径。
- [x] 更新 `docs/eval/RAG_EVAL_GUIDE.md` 的 formal release、validate/plan、custom manifest、UNVERSIONED 与 drift recovery 说明。
- [x] 明确历史 C7/旧报告不回写 releaseVersion，只保留其原始 hash/metadata 边界。
- [x] 明确 C8a 不代表 C8b 数据扩充、C9 claim/judge、C10 quality gate 或 C14 isolation evaluation 完成。

## 5. Offline Verification

- [x] 运行 validator、direct runner、reproducible runner 聚焦 Python tests。
- [x] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`。
- [x] 运行 current release local validation/plan，确认 30 条、3 fixtures、版本/hash/distribution 全部匹配且真实调用为 0。
- [x] 运行 SensitiveLogs 与定向 secret/user-content/absolute-path 扫描。
- [x] 运行 `git diff --check`、规划/实现文件断链、受保护路径和 tracked secret 扫描。
- [x] Java/POM/前端无改动时记录 Maven、frontend build、Docker/live provider `SKIPPED` 及原因。
- [x] 记录实现阶段真实 embedding/rerank/ask/judge/LLM/provider 调用量为 0、数据出站为 0。

## 6. Acceptance And Closeout

- [x] 更新 proposal/design/tasks 的实际实现与验证结果。
- [x] 同步 `openspec/project.md`、架构、技术债、优化索引和 `.ai/AGENT_LOG.md`。
- [ ] 用户验收 schema/manifest、bump rules、validator、runner compatibility 与结论边界。
- [ ] 用户验收后将 4 requirements / 13 scenarios 原文接受进 `openspec/specs/evaluation/spec.md`。
- [ ] 恢复 `.ai/ACTIVE_TASK.md=IDLE` 并把 change 归档。
- [ ] C8a 收口后另行 readiness，再决定是否启动 C8b；不得在本 change 直接扩样本。
