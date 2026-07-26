# Active Task

## Status

`ACTIVE`

## Change

- ID：`2026-07-26-tenant-model-context-and-migration`
- 阶段：C13a
- 位置：`openspec/changes/2026-07-26-tenant-model-context-and-migration/`
- 类型：Type C（租户模型、认证上下文与持久化迁移）
- 当前状态：proposal/design/tasks/spec delta 已起草，等待用户事前闸门审阅；尚未进入实现。

## Scope

- 建立最小 tenant 领域模型，并为现有用户与知识库设计向前兼容迁移。
- tenant identity 只从数据库认证事实与服务端签发的身份上下文推导，不接受客户端 header、query、body 或 metadata 指定。
- 本 change 只做 C13a 暗铺设，不声明跨租户隔离已成立，不启用第二租户，不修改 SQL/Milvus/cache/task/history 的强制过滤。

## Execution Entry

1. 先审阅 proposal、design 的决策记录、tasks 与 `rag-system` spec delta。
2. 未经用户批准，不修改 migration、Java、测试、配置或运行行为。
3. 提交责任为 `用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
4. 实现阶段不得把 C13b/C14/C15/C16 范围并入本 change。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
