# Active Task

## Status

`IDLE`

当前无活动 change。开始新的 Type C 工作前，必须先创建新的 OpenSpec change 并更新本文件。

## Previous Completed

- Change：`2026-07-23-genai-tracing-core`
- 位置：`openspec/changes/archive/2026-07-23-genai-tracing-core/`
- 结果：完成默认关闭、fail-open 的 OTel 1.31 GenAI tracing core，建立分离 ingest/ask trace、稳定 lineage、固定阶段 topology、W3C/custom context、MDC bridge、同步/流式终态和隐私白名单。
- 验收：用户已验收实现与验证证据；4 requirements / 12 scenarios 已原文接受进 `rag-system` baseline。C12 exporter、metrics、alerts、sampling、retention、权限与部署仍未完成。

## Execution Entry

1. 当前无活动任务，不从已归档 change 继续实现。
2. 下一项重大变更必须先建立 proposal、design、tasks 和 spec delta，并明确提交责任。
3. 若进入 C12，必须重新确认 exporter/backend、metrics、数据出站、费用、采样、retention、权限与部署边界。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
