# Active Task

## Status

`IDLE`

当前无活动 change。开始新的 Type C 工作前，必须先创建新的 OpenSpec change 并更新本文件。

## Previous Completed

- Change：`2026-07-26-tenant-model-context-and-migration`
- 位置：`openspec/changes/archive/2026-07-26-tenant-model-context-and-migration/`
- 结果：完成唯一 legacy tenant、user/knowledge-base 非空归属、数据库认证与 access/refresh JWT tenant identity、refresh reload、旧 token fail-closed 及 immutable `RequestIdentity`。
- 验收：用户已验收 migration、auth/context evidence、旧 token 边界与 C13a 非隔离声明；4 requirements / 12 scenarios 已原文接受进 `rag-system` baseline。C13a 不证明跨租户隔离。

## Execution Entry

1. 当前无活动任务，不从已归档 change 继续实现。
2. 下一项重大变更 C13b 必须先建立 proposal、design、tasks 和 spec delta，并明确提交责任。
3. C13b 必须从服务端 `RequestIdentity` 覆盖 SQL/API/permission、所有启用 vector adapters、cache/task/history 强制隔离；不支持的 adapter 必须 fail closed。
4. C14 隔离与恶意样本评测通过前，不开放 C15 MCP 或 C16 Router，也不宣称租户隔离成立。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
