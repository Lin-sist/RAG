# Active Task

## Status

`ACTIVE`

## Active Change

- Change：`2026-07-22-eval-dataset-expansion-and-annotation`
- 位置：`openspec/changes/2026-07-22-eval-dataset-expansion-and-annotation/`
- 类型：Type C 重大变更。
- 当前阶段：C8b 规划草案，等待用户批准 proposal、18 条 design 决策和 `evaluation` delta 的 4 requirements / 12 scenarios；未进入样本实现。
- 目标：在 C8a 版本治理基础上扩充并复核开发评测 question/annotation，建立明确 quota、seed immutability、grounding/review evidence 与可回退的 v2 release。
- 范围：目标总量与 type×difficulty 配额、question/annotation 扩充、fixture coverage、duplicate/near-duplicate、review sidecar、v1/v2 release compatibility、离线 validator/tests 与评测指南。
- 非目标：不进入 C9/C10/C14；不修改 Java/API、指标公式、默认 provider、数据库、前端或部署；规划阶段不修改 eval JSONL、fixture、manifest/schema、runner，也不执行真实业务外调。
- 提交责任：`用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- 验收入口：用户先批准总量、quota、corpus boundary、release/review 方案、18 条决策、4/12 delta 与实现授权；涉及外部 LLM 辅助时还须另行授权调用量、出站、模型、费用和限流风险。

## Last Completed

- Change：`2026-07-21-eval-dataset-schema-and-versioning`
- 位置：`openspec/changes/archive/2026-07-21-eval-dataset-schema-and-versioning/`
- 结果：完成首个 `rag-eval-dev-v1` release manifest、sample schema contract、共享 fail-fast validator、runner identity metadata 与 version bump/drift recovery 规则。
- 验收：用户已验收 C8a schema/manifest、bump rules、validator、runner compatibility 与结论边界；4 个 requirements / 13 个 scenarios 已接受进 `evaluation` baseline，change 已归档。C8a 不代表 C8b 数据扩充或 C9/C10/C14 已完成。

## Start A New Material Change

1. 在 `openspec/changes/<change-id>/` 创建 `proposal.md`、`design.md`、`tasks.md` 和需要的 spec delta。
2. 将本文件 `Status` 改为 `ACTIVE`，并填写 change id、目标、范围、非目标和验收入口。
3. 按 `AGENTS.md` 执行并持续更新 tasks 与 `.ai/AGENT_LOG.md`。
4. 完成且验证后，将本文件恢复为 `IDLE`；经用户确认后把 change 移入 `openspec/changes/archive/`。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
