# Tasks: C9b Judge Calibration And Status Semantics

## 0. Approval And Boundary

- [x] 用户要求开始 C9b 规划。
- [x] readiness 复核：HEAD=`033ee01`，启动前工作区干净，`ACTIVE_TASK=IDLE`，C9a 已接受 baseline 并归档，无其他 active change。
- [x] 提交责任为 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- [x] 规划范围锁定为 judge contract/calibration、objective/judge/global status 与 comparison safety；C10/生产行为/默认 provider 继续串行等待。
- [x] 规划阶段 embedding/rerank/debug retrieval/ask/generation/judge/LLM/provider 调用量和数据出站均为 0。
- [x] 创建 proposal、design、tasks 与 `evaluation` spec delta；baseline、runner、tests、dataset、calibration artifacts 与 guide 暂不修改。
- [x] 用户批准 proposal 的 24-case 四象限、3-repeat、status matrix、external-call gate 与 non-goals。
- [x] 用户批准 design 的 15 条决策记录与 `evaluation` delta 的 4 requirements / 12 scenarios。
- [x] 用户明确授权进入 TDD offline implementation；该授权不包含 live judge calibration。

## 1. Judge Contract And Parser

- [x] RED：judge prompt/parser/threshold/max-context/model config 缺少或漂移时产生稳定 identity error。
- [x] RED：score 缺失、非数值、越界、非法 JSON 和 schema 缺口统一 fail closed 为 `invalid_judge_payload`，不得 clamp 或进入分母。
- [x] RED：provider 自报 pass 与 score-derived pass 冲突时保留诊断并以 tracked score threshold 为规范结果。
- [x] GREEN：提取 direct/calibration 共用的 `rag-judge-v1` prompt、strict parser、identity 与安全诊断。
- [x] GREEN：direct/reproducible plan、run metadata、report/details 固定完整 `judgeContractConfig`，不包含 key。

## 2. Calibration Corpus And Validator

- [x] RED：manifest path/hash/count/order、case ID、四象限 exact quota、fixture grounding、gold consistency 与 review completeness 漂移均 fail closed。
- [x] RED：calibration validator 在任何 judge/provider 调用前失败，错误只含稳定 code、repo-relative artifact 和 case ID/field。
- [x] GREEN：新增 `judge-calibration-v1` manifest、24 cases 和 review/gold contract，四个 faithful×relevant 象限各 6 条。
- [x] GREEN：context 只通过 tracked fixture source + exact contains 解析，不依赖 backend、embedding、generation 或外部知识。

## 3. Calibration Runner And Metrics

- [x] RED：canary 固定 4 cases×1 repeat，full 固定 24 cases×3 repeats；case/order/repeat/contract/model identity 漂移不得比较。
- [x] RED：任一调用/parse/repeat 缺口使 calibration status `PARTIAL/NOT_COMPARABLE`，不能删除失败 observation。
- [x] RED：per-dimension confusion/agreement、joint pass agreement、provider-pass disagreement、parse coverage 与 per-case repeat consistency 公式稳定。
- [x] GREEN：实现独立 calibration runner、plan-only、no-overwrite、脱敏 aggregate report 与本地 raw details。
- [x] GREEN：不自动搜索或修改 threshold，不输出 production go/no-go。

## 4. Objective/Judge/Global Status

- [x] RED：objective complete + judge all-error => objective `COMPLETE`、judge `PARTIAL`、global `PARTIAL`。
- [x] RED：judge off => `SKIPPED` 且不阻止 objective-complete run 为 `CLEAN`；no-answer-only => `NOT_APPLICABLE`。
- [x] RED：retrieval-only、ask/retrieval partial、login/retrieval failed 与 claim partial 的状态矩阵稳定。
- [x] RED：structured comparison safety 保留干净 objective 指标，不把 judge partial 误报为“仅 retrieval 可比较”。
- [x] GREEN：direct runner 成为 objective/judge/global status 与 comparison safety 的唯一实现，质量 pass/fail 不影响 completeness status。

## 5. Compatibility And Documentation

- [x] 历史 details/report 缺 C9b 字段时仍可读取，解释为 unavailable，不回填为 0/SKIPPED/PARTIAL。
- [x] reproducible runner 只传递/校验同一 contract 和 status metadata，不复制算法、不传 key 到 child command。
- [x] 更新 `docs/eval/RAG_EVAL_GUIDE.md` 的 calibration、status、comparison、raw artifact 与 external-call 边界。
- [x] 同步 `openspec/project.md`、architecture、technical debt、optimization index 与 `.ai/AGENT_LOG.md`。
- [x] 明确 C9b 不改变 production QA、C9a objective formula、dataset release、默认 judge 或 C10 gate。

## 6. Offline Verification

- [x] 按纵向 TDD 切片记录 RED→GREEN 证据，不批量先写完全部测试。
- [x] 运行 direct/reproducible/calibration 聚焦 tests。
- [x] 运行 `python -B -m unittest discover -s scripts -p 'test_*.py'`。
- [x] 验证 v1/v2 dataset 与 calibration v1 manifest，运行三个 runner 的 plan-only；实际业务调用必须为 0。
- [x] 运行 SensitiveLogs、changed Markdown 链接、secret/absolute-path、受保护路径、历史 report 非覆盖与 `git diff --check`。
- [x] Java/POM/前端无改动时，Maven、frontend build、Docker/live backend 记为 `SKIPPED`。

## 7. Live Calibration Gate

- [x] `SKIPPED`：用户本次验收并授权归档的是 C9b offline implementation；未单独授权 canary 4 calls、full 72 calls 或总计最多 76 calls。
- [x] `SKIPPED`：未选择 live provider/model/endpoint，也未产生费用、限流、timeout/retry 或 raw provider artifact；tracked identity 和 `--no-overwrite` 仅完成离线实现与测试。
- [x] `SKIPPED`：未执行 canary 或 full，不存在通过换样本、删除 observation 或只保留成功子集形成的 live evidence。
- [x] `SKIPPED`：没有 live parse/HTTP/rate-limit/timeout、repeat consistency、confusion/agreement 或 provider-pass mismatch 观测；实际 judge/provider 调用和数据出站为 0。
- [x] 用户明确验收 offline corpus/contract/status/comparison 实现并要求归档；当前 contract 作为离线能力接受，但无 live agreement 结论，后续真实校准必须另立授权与 evidence 流程。

## 8. Acceptance And Closeout

- [x] 用户验收 calibration corpus、judge contract、status/comparison semantics、兼容性与结论边界；live evidence 明确未执行并按 `SKIPPED` 收口。
- [x] 用户验收后将 approved delta 原文接受进 `openspec/specs/evaluation/spec.md`。
- [x] 恢复 `.ai/ACTIVE_TASK.md=IDLE` 并归档 change。
- [x] C9b 收口不确认 production faithfulness、通用 judge 可靠性、默认开启 judge 或 C10 quality gate。
