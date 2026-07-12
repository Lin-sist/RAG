# Proposal: Repository Governance Bootstrap

## Why

仓库经历多轮 AI 辅助迭代后，曾同时存在 Kiro spec、维护计划、阶段文档和会话交接稿。即使完成清理，如果没有统一活动任务、规格和执行日志入口，后续仍会重新产生平行真相源。

## Scope

- 建立根目录 `AGENTS.md`。
- 建立 `.ai/ACTIVE_TASK.md` 与 append-only `.ai/AGENT_LOG.md`。
- 初始化 OpenSpec project、baseline specs 和 change 生命周期说明。
- 收敛 Copilot 指令为读取统一治理入口。
- 更新长期文档中的治理状态与入口。

## Non-goals

- 不修改 Java、Vue 或数据库代码。
- 不实施技术债清单中的功能项。
- 不安装 OpenSpec CLI。
- 不自动 stage、commit 或 push。

## Acceptance

- 所有治理入口相互引用且无断链。
- `.ai/ACTIVE_TASK.md` 最终为 `IDLE`，并指向本归档 change。
- baseline specs 覆盖 RAG 主链、评测和 Agent 协作。
- 旧 Copilot 长规则不再与 `AGENTS.md` 并行维护。
- `git diff --check` 通过。
