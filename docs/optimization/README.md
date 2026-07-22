# RAG 优化文档索引

> 状态日期：2026-07-21

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

## 历史材料

`history/` 保存 v3 正式计划形成前的 hybrid、reranker abstraction 和 token chunker 演进记录。它们可以解释代码为何形成当前结构，但不得单独用于判断当前阶段、指标或待办。

## 使用规则

1. 先读当前代码，再读本索引。
2. 指标结论必须同时核对报告状态、error count、metadata 和 Git HEAD。
3. 新优化不得继续在本目录新增无版本号的 `stage1.md / stage2.md`。
4. 未完成的新 change 以 OpenSpec 为执行源；本目录只保存阶段完成后的长期结论。
