# Tasks: C9b Judge Calibration And Status Semantics

## 0. Approval And Boundary

- [x] 用户要求开始 C9b 规划。
- [x] readiness 复核：HEAD=`033ee01`，启动前工作区干净，`ACTIVE_TASK=IDLE`，C9a 已接受 baseline 并归档，无其他 active change。
- [x] 提交责任为 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- [x] 规划范围锁定为 judge contract/calibration、objective/judge/global status 与 comparison safety；C10/生产行为/默认 provider 继续串行等待。
- [x] 规划阶段 embedding/rerank/debug retrieval/ask/generation/judge/LLM/provider 调用量和数据出站均为 0。
- [x] 创建 proposal、design、tasks 与 `evaluation` spec delta；baseline、runner、tests、dataset、calibration artifacts 与 guide 暂不修改。
- [ ] 用户批准 proposal 的 24-case 四象限、3-repeat、status matrix、external-call gate 与 non-goals。
- [ ] 用户批准 design 的决策记录与 `evaluation` delta 的 4 requirements / 12 scenarios。
- [ ] 用户明确授权进入 TDD offline implementation；该授权默认不包含 live judge calibration。

## 1. Judge Contract And Parser

- [ ] RED：judge prompt/parser/threshold/max-context/model config 缺少或漂移时产生稳定 identity error。
- [ ] RED：score 缺失、非数值、越界、非法 JSON 和 schema 缺口统一 fail closed 为 `invalid_judge_payload`，不得 clamp 或进入分母。
- [ ] RED：provider 自报 pass 与 score-derived pass 冲突时保留诊断并以 tracked score threshold 为规范结果。
- [ ] GREEN：提取 direct/calibration 共用的 `rag-judge-v1` prompt、strict parser、identity 与安全诊断。
- [ ] GREEN：direct/reproducible plan、run metadata、report/details 固定完整 `judgeContractConfig`，不包含 key。

## 2. Calibration Corpus And Validator

- [ ] RED：manifest path/hash/count/order、case ID、四象限 exact quota、fixture grounding、gold consistency 与 review completeness 漂移均 fail closed。
- [ ] RED：calibration validator 在任何 judge/provider 调用前失败，错误只含稳定 code、repo-relative artifact 和 case ID/field。
- [ ] GREEN：新增 `judge-calibration-v1` manifest、24 cases 和 review/gold contract，四个 faithful×relevant 象限各 6 条。
- [ ] GREEN：context 只通过 tracked fixture source + exact contains 解析，不依赖 backend、embedding、generation 或外部知识。

## 3. Calibration Runner And Metrics

- [ ] RED：canary 固定 4 cases×1 repeat，full 固定 24 cases×3 repeats；case/order/repeat/contract/model identity 漂移不得比较。
- [ ] RED：任一调用/parse/repeat 缺口使 calibration status `PARTIAL/NOT_COMPARABLE`，不能删除失败 observation。
- [ ] RED：per-dimension confusion/agreement、joint pass agreement、provider-pass disagreement、parse coverage 与 per-case repeat consistency 公式稳定。
- [ ] GREEN：实现独立 calibration runner、plan-only、no-overwrite、脱敏 aggregate report 与本地 raw details。
- [ ] GREEN：不自动搜索或修改 threshold，不输出 production go/no-go。

## 4. Objective/Judge/Global Status

- [ ] RED：objective complete + judge all-error => objective `COMPLETE`、judge `PARTIAL`、global `PARTIAL`。
- [ ] RED：judge off => `SKIPPED` 且不阻止 objective-complete run 为 `CLEAN`；no-answer-only => `NOT_APPLICABLE`。
- [ ] RED：retrieval-only、ask/retrieval partial、login/retrieval failed 与 claim partial 的状态矩阵稳定。
- [ ] RED：structured comparison safety 保留干净 objective 指标，不把 judge partial 误报为“仅 retrieval 可比较”。
- [ ] GREEN：direct runner 成为 objective/judge/global status 与 comparison safety 的唯一实现，质量 pass/fail 不影响 completeness status。

## 5. Compatibility And Documentation

- [ ] 历史 details/report 缺 C9b 字段时仍可读取，解释为 unavailable，不回填为 0/SKIPPED/PARTIAL。
- [ ] reproducible runner 只传递/校验同一 contract 和 status metadata，不复制算法、不传 key 到 child command。
- [ ] 更新 `docs/eval/RAG_EVAL_GUIDE.md` 的 calibration、status、comparison、raw artifact 与 external-call 边界。
- [ ] 同步 `openspec/project.md`、architecture、technical debt、optimization index 与 `.ai/AGENT_LOG.md`。
- [ ] 明确 C9b 不改变 production QA、C9a objective formula、dataset release、默认 judge 或 C10 gate。

## 6. Offline Verification

- [ ] 按纵向 TDD 切片记录 RED→GREEN 证据，不批量先写完全部测试。
- [ ] 运行 direct/reproducible/calibration 聚焦 tests。
- [ ] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`。
- [ ] 验证 v1/v2 dataset 与 calibration v1 manifest，运行三个 runner 的 plan-only；实际业务调用必须为 0。
- [ ] 运行 SensitiveLogs、changed Markdown 链接、secret/absolute-path、受保护路径、历史 report 非覆盖与 `git diff --check`。
- [ ] Java/POM/前端无改动时，Maven、frontend build、Docker/live backend 记为 `SKIPPED`。

## 7. Live Calibration Gate

- [ ] 执行前单独披露并取得用户授权：canary 最多 4 judge calls；full 最多 72 judge calls；合计最多 76，其他业务调用均为 0。
- [ ] 固定 provider/model/endpoint/temperature/timeout/retry、Git HEAD、manifest、prompt/parser/threshold/max-context identity、费用/限流与 raw artifact no-overwrite。
- [ ] canary 四象限各 1 条通过 identity/schema/coverage 检查后，才允许 full 24×3；失败不得通过换样本或删除 observation 继续。
- [ ] 记录 parse errors、HTTP/rate-limit/timeout、attempt count、per-case repeat consistency、confusion/agreement 与 provider-pass mismatch。
- [ ] 用户审阅 live calibration evidence 后决定接受当前 contract、版本化调整或拒绝；Agent 不自动调 threshold。

## 8. Acceptance And Closeout

- [ ] 用户验收 calibration corpus、judge contract、live evidence、status/comparison semantics、兼容性与结论边界。
- [ ] 用户验收后将 approved delta 原文接受进 `openspec/specs/evaluation/spec.md`。
- [ ] 恢复 `.ai/ACTIVE_TASK.md=IDLE` 并归档 change。
- [ ] C9b 收口不确认 production faithfulness、通用 judge 可靠性、默认开启 judge 或 C10 quality gate。
