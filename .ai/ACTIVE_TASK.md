# Active Task

## Status

`IDLE`

## Active Change

无。

## Last Completed

- Change：`2026-07-14-jwt-secret-production-guard`
- 位置：`openspec/changes/archive/2026-07-14-jwt-secret-production-guard/`
- 结果：在精确 `prod` profile 下拒绝 JWT secret 的 blank、known-default、首尾空白和未解析占位符；保持 JJWT 按 UTF-8 bytes 负责算法 key-strength。
- 实现提交：`528a2cb16e11a54539c1ff602c62c74670026578`。
- 验收：用户已确认通过；delta 已接受进 `openspec/specs/rag-system/spec.md`。

## Start A New Material Change

1. 在 `openspec/changes/<change-id>/` 创建 `proposal.md`、`design.md`、`tasks.md` 和需要的 spec delta。
2. 将本文件 `Status` 改为 `ACTIVE`，并填写 change id、目标、范围、非目标和验收入口。
3. 按 `AGENTS.md` 执行并持续更新 tasks 与 `.ai/AGENT_LOG.md`。
4. 完成且验证后，将本文件恢复为 `IDLE`；经用户确认后把 change 移入 `openspec/changes/archive/`。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
