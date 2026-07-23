# RAG 优化文档索引

> 状态日期：2026-07-23

本目录只保留三类内容：当前 v3 证据、已关闭的 v4 阶段证据、历史演进证据。阶段编号只在所属版本目录内解释。

## 当前真相源

### v3：检索质量工程

- `v3/summary.md`：v3 总结与指标入口。
- `v3/stage1-reproducible-eval.md`：固定评测 KB 与可复现 retrieval baseline。
- `v3/stage2-chunking.md`：分块矩阵与 `420/80` 决策。
- `v3/stage3-rerank-adapter.md`：HTTP model reranker adapter、健康检查与降级。

### v4：生成与引用质量（部分完成并关闭）

- `v4/plan.md`：已关闭的 v4 计划快照；Stage 3 已转入技术债 P1，不再从该文件继续执行。
- `v4/stage1-generation-citation.md`：两轮 CLEAN generation/citation/no-answer baseline。
- `v4/stage2-rerank-decision.md`：缺真实 provider 时的正式跳过结论。

### C7：当前 reranker A/B evidence

- `../eval/reports/c7-reranker-ab-full-2026-07-20.md`：固定 30 条开发样本、`R=3,W=3` 的 clean heuristic/NVIDIA 比较、provider coverage、质量 delta 与延迟边界；用户已验收，delta 已接受并归档。

### C8a：评测数据版本治理（已验收归档）

- `../eval/dataset-manifest.json`：首个 `rag-eval-dev-v1` release identity，固定 question/schema/annotation/corpus version 与当前 artifact hash。
- `../eval/schema/rag-eval-sample-v1.json`：allowed/required fields、类型、enum、ID pattern 与 answerable/no-answer 条件契约。
- `../eval/RAG_EVAL_GUIDE.md`：formal/UNVERSIONED 边界、零外调 plan、version bump matrix 与 drift recovery。4 个 requirements / 13 个 scenarios 已接受进长期 `evaluation` baseline，change 已归档。

### C8b：评测数据扩充与标注（已验收归档）

- `../eval/releases/rag-eval-dev-v2-manifest.json`：显式 v2 release，固定 150 条 exact quota、v1 seed identity、三份 fixture coverage、grounding/duplicate facts 和 review identity。
- `../eval/releases/rag-eval-dev-v2.jsonl`：前 30 条保持 v1 raw/object/order identity，追加 120 条 source-first 开发样本；默认 manifest 已切换为与显式 v2 manifest byte-identical。
- `../eval/review/rag-eval-dev-v2-review.jsonl`：150 条结构、grounding、duplicate 与语义复核事实，不复制完整 question、answer 或 fixture 正文。
- C8b 的 4 requirements / 12 scenarios 已接受进长期 baseline并归档；这不代表 C9/C10/C14 或任何质量收益完成。

### C9a：客观 Claim-Evidence 指标（已验收归档）

- archived change：`archive/2026-07-23-claim-evidence-objective-metrics`。
- direct runner 已新增固定 `claim-lexical-v1`：句子/列表 claim、provenance-valid returned citations、exact + `0.70` claim-token coverage、逐 claim attribution 与全 claim 分母。
- direct/reproducible plan、run metadata、Markdown 和 details JSON 固定 splitter/tokenizer/threshold/evidence policy identity；identity 漂移会在 backend/provider 调用前失败。
- 4 个 requirements / 12 个 scenarios 已接受进长期 `evaluation` baseline；当前只有合成离线测试和 plan-only 证据，真实 generation/provider 调用量为 0，仍不宣称 faithfulness、C9b judge calibration 或 C10 quality gate 完成。

### C9b：Judge 校准与状态语义（已验收归档）

- archived change：`../../openspec/changes/archive/2026-07-23-judge-calibration-and-status-semantics/`。
- `../eval/calibration/judge-calibration-v1-manifest.json` 固定 24 条 faithful×relevant 四象限人工 gold case，各象限 6 条；context 只解析 tracked fixture exact excerpt。
- normal/calibration runner 共用 `rag-judge-v1` prompt、strict score parser、score-derived pass 与脱敏 contract identity；normal report/details/console 分离 objective、judge、global status 和 comparison safety。
- canary 固定 4×1，full 固定 24×3；runner 无自动 retry，并要求显式 `--execute-live-judge`、本地 raw details 与 `--no-overwrite`。
- 4 requirements / 12 scenarios 已接受进长期 `evaluation` baseline；live calibration 未授权并按 `SKIPPED` 收口，真实 judge/provider 调用与数据出站为 0。尚无 agreement evidence，不能宣称 judge 已真实校准、默认开启或 C10 gate 已建立。

### C10：质量阈值门禁（离线框架已验收归档）

- archived change：`../../openspec/changes/archive/2026-07-23-eval-quality-threshold-gates/`。
- `../eval/schema/rag-quality-gate-profile-v1.json` 与独立 evaluator 固定 profile/dataset/run/channel identity、`all/type/difficulty/answerability` 切片、hard/reference AND、minimum denominator、缺失/错误 fail-closed 语义以及 `0/3/4/2` 退出码。
- 首个 `rag-eval-dev-v2-retrieval-regression-v1` profile 保持 `DRAFT / PENDING_REFERENCE_EVIDENCE`，12 个 target 均未填写；reference calls 未授权并按 `SKIPPED` 收口，实际 backend/provider 调用和数据出站为 0。
- 4 requirements / 12 scenarios 已接受进长期 `evaluation` baseline；本次只确认 offline gate framework，不确认 ACTIVE quality gate、retrieval/generation/citation/judge 质量达标或 production readiness。

## 历史材料

`history/` 保存 v3 正式计划形成前的 hybrid、reranker abstraction 和 token chunker 演进记录。它们可以解释代码为何形成当前结构，但不得单独用于判断当前阶段、指标或待办。

## 使用规则

1. 先读当前代码，再读本索引。
2. 指标结论必须同时核对报告状态、error count、metadata 和 Git HEAD。
3. 新优化不得继续在本目录新增无版本号的 `stage1.md / stage2.md`。
4. 未完成的新 change 以 OpenSpec 为执行源；本目录只保存阶段完成后的长期结论。
