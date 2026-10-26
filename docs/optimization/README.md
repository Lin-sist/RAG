# RAG 优化文档索引

> 整理日期：2026-10-26。本页只提供证据入口，不再复写各阶段的状态、指标和测试计数。

## 现行入口

- [项目上下文](../../openspec/project.md)：定位、能力与默认边界。
- [活动指针](../../.ai/ACTIVE_TASK.md)：当前任务。
- [技术债](../roadmap/technical-debt.md)：未闭环事项与唤醒条件。
- [评测指南](../eval/RAG_EVAL_GUIDE.md)：方法、身份与报告语义。
- [evaluation spec](../../openspec/specs/evaluation/spec.md)与[rag-system spec](../../openspec/specs/rag-system/spec.md)：已接受的长期契约。

## 阶段证据导航

以下结论只适用于各自版本与日期；不能跨数据集、模型、源码身份或运行策略直接比较历史数字。

| 范围 | 证据入口 |
| --- | --- |
| v3 检索工程 | [总结](v3/summary.md)、[可复现评测](v3/stage1-reproducible-eval.md)、[分块矩阵](v3/stage2-chunking.md)、[rerank adapter](v3/stage3-rerank-adapter.md) |
| v4 部分完成后关闭 | [关闭计划](v4/plan.md)、[生成与引用](v4/stage1-generation-citation.md)、[rerank 跳过结论](v4/stage2-rerank-decision.md) |
| C7 reranker A/B | [固定开发样本比较](../eval/reports/c7-reranker-ab-full-2026-07-20.md) |
| C8 数据集治理 | [当前 manifest](../eval/dataset-manifest.json)、[显式 v2 manifest](../eval/releases/rag-eval-dev-v2-manifest.json)、[样本 schema](../eval/schema/rag-eval-sample-v1.json) |
| C9～C16 工具与契约 | 从[评测指南](../eval/RAG_EVAL_GUIDE.md)及[归档目录](../../openspec/changes/archive/)查具体证据 |
| C17 检索门禁 | [正式验收](../../openspec/changes/archive/2026-09-10-retrieval-quality-gate-activation/acceptance.md) |
| C18 单轮生成基线 | [正式验收](../../openspec/changes/archive/2026-09-22-generation-objective-evidence-baseline/acceptance.md) |
| C19 离线校准工具链 | [接受范围与证据边界](../../openspec/changes/archive/2026-10-21-judge-live-calibration-evidence/acceptance.md) |
| C20 DRAFT 工具链 | [接受范围与证据边界](../../openspec/changes/archive/2026-10-21-objective-judge-quality-gate-profile/acceptance.md) |
| 早期演进 | [hybrid](history/hybrid-retrieval.md)、[reranker](history/reranker-abstraction.md)、[token chunker](history/token-chunker-hardening.md) |

## 维护规则

1. 新任务从活动指针与经确认的 change 启动，不在本目录另建进度表。
2. 结论须连同 Report status、error/retry、metadata 和 Git HEAD 阅读；文件名或归档状态不能证明真实质量达标。
3. 执行结果放专项验收或日志，本索引只链接；不回改冻结报告、audit、manifest 或 accepted archive 来清理历史差异。
4. 新阶段保留版本身份，不新增含义不明的 stage1.md、stage2.md。
