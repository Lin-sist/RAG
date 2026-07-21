# Active Task

## Status

`ACTIVE`

## Active Change

- Change：`2026-07-21-eval-dataset-schema-and-versioning`
- 位置：`openspec/changes/2026-07-21-eval-dataset-schema-and-versioning/`
- 类型：Type C 重大变更。
- 当前阶段：C8a proposal/design/tasks/`evaluation` spec delta 已建立，等待用户事前闸门审阅；未进入实现。
- 目标：把当前 question set、fixture corpus、sample schema 与 annotation 建成版本化 release identity，并让评测 runner 在任何 backend/provider 调用前统一 fail-fast 校验。
- 范围：dataset release manifest、项目级样本 schema contract、共享本地 validator、runner metadata/compatibility、version bump 规则、单元测试与评测指南。
- 非目标：不扩充或重新标注当前 30 条样本；不进入 C8b/C9/C10/C14；不修改 Java/API、指标公式、默认 provider、数据库、前端或部署；不执行真实业务外调。
- 提交责任：`用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- 验收入口：先批准 proposal scope/non-goals、design 16 条决策与 4 requirements / 13 scenarios；获批后才进入 TDD 实现，最终经用户验收后接受 delta、归档并恢复 `IDLE`。

## Last Completed

- Change：`2026-07-20-reranker-ab-evaluation`
- 位置：`openspec/changes/archive/2026-07-20-reranker-ab-evaluation/`
- 结果：完成严格 identity、provider coverage、sample pairing 与 latency 分口径的 retrieval-only reranker A/B；full `R=3,W=3` 六个 measured runs comparison=`COMPARABLE`。
- 验收：用户已验收 C7 full evidence 与结论边界；4 个 requirements / 11 个 scenarios 已接受进 `evaluation` baseline，change 已归档。当前默认 provider 继续保持 heuristic；如需切换必须另立 Type C change。

## Start A New Material Change

1. 在 `openspec/changes/<change-id>/` 创建 `proposal.md`、`design.md`、`tasks.md` 和需要的 spec delta。
2. 将本文件 `Status` 改为 `ACTIVE`，并填写 change id、目标、范围、非目标和验收入口。
3. 按 `AGENTS.md` 执行并持续更新 tasks 与 `.ai/AGENT_LOG.md`。
4. 完成且验证后，将本文件恢复为 `IDLE`；经用户确认后把 change 移入 `openspec/changes/archive/`。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
