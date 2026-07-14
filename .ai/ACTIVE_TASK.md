# Active Task

## Status

`ACTIVE`

## Active Change

- Change：`2026-07-14-database-backed-authentication`
- 位置：`openspec/changes/2026-07-14-database-backed-authentication/`
- 目标：将登录与 refresh 的用户事实源切换到数据库，增加默认关闭的一次性管理员 bootstrap，并通过前向 migration 隔离已知默认管理员凭据。
- 范围：认证 user/role 持久层、数据库 `UserDetailsService`、refresh 状态重载、known seed 安全迁移、bootstrap、固定凭据入口清理及相应测试。
- 非目标：注册/用户管理/密码修改 API、实时 access token 撤权、租户模型、C3 联合集成测试及任何 RAG 指标行为调整。
- 当前阶段：Phase 1→6 技术实现与验证均已完成；MySQL 8.0.36 Testcontainers、完整 Maven、Python、正式前端 build、敏感日志和范围门禁全部通过，等待用户明确确认验收后接受 delta、恢复 `IDLE` 并归档 C2。
- 验收入口：`openspec/changes/2026-07-14-database-backed-authentication/tasks.md` 的 Phase 6。
- 提交责任：`Agent 提交`；用户已明确授权本地中文 commit，不授权 push、PR、发布或部署。

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
