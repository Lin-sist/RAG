# Active Task

## Status

`IDLE`

当前无活动 change。开始新的 Type C 工作前，必须先创建新的 OpenSpec change 并更新本文件。

## Previous Completed

- Change：`2026-07-26-otel-export-and-metrics`
- 位置：`openspec/changes/archive/2026-07-26-otel-export-and-metrics/`
- 结果：完成默认关闭、fail-open 的 OTLP gRPC trace/metric export、低基数 GenAI metrics 与本机 Collector/Tempo/Prometheus/Grafana reference 闭环，固定关键 trace 全保留、普通成功 trace 10% tail sampling、72h/7d retention、localhost 端口和 Grafana 认证边界。
- 验收：用户已验收实现与 synthetic evidence；4 requirements / 12 scenarios 已原文接受进 `rag-system` baseline。生产 HA、容量、合规 retention、租户权限、跨主机传输、通知与 SLA 仍不在已完成范围。

## Execution Entry

1. 当前无活动任务，不从已归档 change 继续实现。
2. 下一项重大变更必须先建立 proposal、design、tasks 和 spec delta，并明确提交责任。
3. 后续生产观测能力必须重新确认数据出站、费用、容量、保留期、权限、通知与部署边界。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
