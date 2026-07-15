# Active Task

## Status

`ACTIVE`

## Active Change

`2026-07-15-integration-test-happy-path`

## Goal

用一条独立 Maven 命令启动隔离的 MySQL、Redis 与 Milvus Testcontainers，使用 test-scope 确定性 embedding，真实验证登录 → 创建知识库 → 上传 → 索引 → retrieval → 删除主链路。

## Scope

- `rag-admin` test scope 的 C3 integration-test harness 与 Maven/Failsafe 入口。
- 真实 MySQL/Flyway、Redis、etcd、MinIO、Milvus 隔离容器和随机 host ports。
- JVM 内确定性 embedding provider，禁用真实 embedding/LLM/rerank model/judge 调用。
- HTTP happy-path、任务有界轮询、资源删除与重复运行验证。

## Non-goals

- 不验证 LLM generation、citation、no-answer、judge 或 SSE。
- 不实现 Redis/Milvus/LLM 故障语义，也不实现索引恢复。
- 不修改 API/DTO、数据库 schema、生产 provider 接口、检索/分块/prompt/评测行为。
- 不复用或停止本机常驻 `rag-*` Docker Compose 容器。

## Spec Delta

当前无长期 spec delta：本 change 只补 test-scope 联合证据，不修改生产契约。若实现必须触及 production seam，停止并补 `rag-system` delta 后重新审批。

## External Calls

embedding/rerank model/ask/LLM/judge 业务外部调用预计与授权量均为 0；只允许本地确定性 provider 与隔离 Docker network。首次实现或运行可能下载固定版本基础设施镜像，但不上传业务数据。

## Acceptance Entry

- `openspec/changes/2026-07-15-integration-test-happy-path/proposal.md`
- `openspec/changes/2026-07-15-integration-test-happy-path/design.md`
- `openspec/changes/2026-07-15-integration-test-happy-path/tasks.md`
- 专用命令：`mvn -q -pl rag-admin -am -Pc3-integration verify`

## Approval Gate

当前仅完成规格草案。用户明确批准 proposal、design、tasks 与“无长期 spec delta”决定前，不进入实现。

## Commit Responsibility

`用户手动提交`。Agent 不暂存、不提交、不 push。

## Last Completed

- Change：`2026-07-14-database-backed-authentication`
- 位置：`openspec/changes/archive/2026-07-14-database-backed-authentication/`
- 结果：登录与 refresh 改以数据库用户和角色为事实源；新增默认关闭的一次性管理员 bootstrap，并通过 V6 前向迁移精确隔离历史 known seed。
- 实现提交：`9c63051d8863786f04d8c0ccdb9fd34743d6311e`。
- 验收：用户已确认通过；delta 已接受进 `openspec/specs/rag-system/spec.md`。

## Start A New Material Change

1. 在 `openspec/changes/<change-id>/` 创建 `proposal.md`、`design.md`、`tasks.md` 和需要的 spec delta。
2. 将本文件 `Status` 改为 `ACTIVE`，并填写 change id、目标、范围、非目标和验收入口。
3. 按 `AGENTS.md` 执行并持续更新 tasks 与 `.ai/AGENT_LOG.md`。
4. 完成且验证后，将本文件恢复为 `IDLE`；经用户确认后把 change 移入 `openspec/changes/archive/`。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
