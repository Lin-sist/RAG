# Active Task

## Status

`ACTIVE`

## Change

- Change：`2026-07-26-otel-export-and-metrics`
- 位置：`openspec/changes/2026-07-26-otel-export-and-metrics/`
- 阶段：Java/config/reference stack implementation 与 synthetic verification 已完成，等待用户验收；尚未接受 baseline 或归档。
- 提交责任：`用户手动提交`。Agent 不暂存、不提交、不 push、不创建 PR、不部署。

## Scope

- 在 C11 tracing core 上增加默认关闭、fail-open 的 OTLP gRPC trace/metric export 与低基数 GenAI stage metrics。
- 交付仅本机使用的 Collector → Tempo/Prometheus → Grafana reference deployment、tail sampling、72h/7d retention、认证访问、dashboard 与本地 rules。
- SaaS/公网/跨主机 telemetry、Alertmanager/外部通知、生产 SLA/HA/容量/成本、多租户观测权限和日志聚合保持 out of scope。

## Approval Gate

1. 用户已批准 proposal 的 backend、default-off、metrics、sampling、retention、权限、dashboard/alerts 与 non-goals。
2. 用户已批准 design 的 15 条决策记录和 `rag-system` delta 的 4 requirements / 12 scenarios。
3. 用户已授权新增 OTel OTLP exporter/metrics SDK 依赖、下载固定 Collector/Tempo/Prometheus/Grafana images，并执行仅含 synthetic telemetry 的本机 smoke。
4. 实现授权不包含真实 embedding/rerank/ask/generation/judge/LLM/provider、SaaS、付费服务、公网数据出站、push、PR 或部署。

## Current External-Call State

- 实现阶段真实 embedding/rerank/debug retrieval/ask/generation/judge/LLM/provider/SaaS 调用与业务数据出站均为 0；只发生固定镜像下载及本机 synthetic OTLP。
- 首次 Grafana 13.1.1 启动暴露默认插件预装下载，已通过 `GF_PLUGINS_PREINSTALL_DISABLED=true` 等配置关闭；全新 volume 重建后验证无插件下载或 provisioning error。
- 不得把本机 synthetic smoke 解释为 production observability readiness、SLA、容量或长期费用证据。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
