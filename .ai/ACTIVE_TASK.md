# Active Task

## Status

`IDLE`

## Active Change

- 当前无 active change。

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
