# Active Task

## Status

`IDLE`

当前无活动 change。开始新的 Type C 工作前，必须先创建新的 OpenSpec change 并更新本文件。

## Previous Completed

- Change：`tenant-data-plane-enforcement`
- 位置：`openspec/changes/archive/2026-07-27-tenant-data-plane-enforcement/`
- 结果：完成 SQL/API/permission、task/cache/history/feedback、RAG/keyword、Milvus tenant adapter contract，以及默认关闭的 tenant-aware shadow collection/readiness 维护路径。
- 验收：用户已验收 C13b data-plane enforcement evidence；6 requirements / 18 scenarios 已原文接受进 `rag-system` baseline。真实 Milvus shadow copy/mapping/readiness switch 未执行，C13b 不证明租户隔离成立。

## Execution Entry

1. 当前无活动任务，不从已归档 change 继续实现。
2. 下一项重大变更 C14 必须先建立 proposal、design、tasks 和 spec delta，并明确提交责任。
3. C14 必须覆盖跨 tenant 隔离与恶意样本评测；C14 通过前不开放第二业务 tenant、tenant management、C15 MCP 或 C16 Router，也不宣称租户隔离成立。
4. 任何真实 Milvus collection 创建、vector 复制、mapping/readiness 切换、重试或清理仍需单独披露并取得授权。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
