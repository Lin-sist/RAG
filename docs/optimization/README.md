# RAG 优化文档索引

> 状态日期：2026-07-14

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

## 历史材料

`history/` 保存 v3 正式计划形成前的 hybrid、reranker abstraction 和 token chunker 演进记录。它们可以解释代码为何形成当前结构，但不得单独用于判断当前阶段、指标或待办。

## 使用规则

1. 先读当前代码，再读本索引。
2. 指标结论必须同时核对报告状态、error count、metadata 和 Git HEAD。
3. 新优化不得继续在本目录新增无版本号的 `stage1.md / stage2.md`。
4. 未完成的新 change 以 OpenSpec 为执行源；本目录只保存阶段完成后的长期结论。
