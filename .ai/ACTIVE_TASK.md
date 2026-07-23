# Active Task

## Status

`IDLE`

当前没有活动 change。新重大变更必须重新完成 readiness 与 OpenSpec 事前闸门。

## Last Completed

- Change：`2026-07-23-eval-quality-threshold-gates`
- 位置：`openspec/changes/archive/2026-07-23-eval-quality-threshold-gates/`
- 结果：完成 `rag-quality-gate-profile-v1`、独立离线 evaluator、固定切片、阈值/容差、fail-closed 状态、脱敏 summary 与稳定退出码。
- 验收：用户已验收 offline framework；4 requirements / 12 scenarios 已接受进 `evaluation` baseline。Reference calls 未授权并按 `SKIPPED` 收口，首个 retrieval profile 保持 `DRAFT`，因此不确认 ACTIVE quality gate 或项目质量达标。

## Start A New Material Change

1. 冻结蓝图的下一主线候选是 C11 `genai-tracing-core`；先等待用户手动提交本次 C10 归档，使工作区恢复干净。
2. 在 `openspec/changes/<change-id>/` 创建 `proposal.md`、`design.md`、`tasks.md` 和需要的 spec delta。
3. 将本文件 `Status` 改为 `ACTIVE`，并填写 change id、目标、范围、非目标和验收入口。
4. 按 `AGENTS.md` 执行并持续更新 tasks 与 `.ai/AGENT_LOG.md`；完成且验证后，经用户确认再归档并恢复 `IDLE`。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
