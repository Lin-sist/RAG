# Active Task

## Status

`ACTIVE`

## Change

- Change：`2026-07-26-otel-export-and-metrics`
- 位置：`openspec/changes/2026-07-26-otel-export-and-metrics/`
- 阶段：proposal/design/tasks/spec delta 已起草，等待用户事前门禁审阅；尚未进入实现。
- 提交责任：`用户手动提交`。Agent 不暂存、不提交、不 push、不创建 PR、不部署。

## Scope

- 在 C11 tracing core 上增加默认关闭、fail-open 的 OTLP gRPC trace/metric export 与低基数 GenAI stage metrics。
- 交付仅本机使用的 Collector → Tempo/Prometheus → Grafana reference deployment、tail sampling、72h/7d retention、认证访问、dashboard 与本地 rules。
- SaaS/公网/跨主机 telemetry、Alertmanager/外部通知、生产 SLA/HA/容量/成本、多租户观测权限和日志聚合保持 out of scope。

## Approval Gate

1. 用户需批准 proposal 的 backend、default-off、metrics、sampling、retention、权限、dashboard/alerts 与 non-goals。
2. 用户需批准 design 的 15 条决策记录和 `rag-system` delta 的 4 requirements / 12 scenarios。
3. 进入实现前，用户需明确授权新增 OTel OTLP exporter/metrics SDK 依赖、下载固定 Collector/Tempo/Prometheus/Grafana images 并执行本机 synthetic smoke。
4. 上述实现授权不包含真实 embedding/rerank/ask/generation/judge/LLM/provider、SaaS、付费服务、公网数据出站、push、PR 或部署。

## Current External-Call State

- 规划阶段真实 provider/exporter 调用、Docker image 下载、telemetry 出站与费用均为 0。
- 不得把规划或后续本机 synthetic smoke 解释为 production observability readiness、SLA、容量或长期费用证据。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
