# C12 本机可观测性参考栈

该目录提供默认不启动、可整体删除的本机参考栈：应用通过 OTLP/gRPC 向 Collector 发送 trace 与 metrics，Collector 将 trace 写入 Tempo、将应用 metrics 暴露给 Prometheus，Grafana 只通过已认证的本机入口展示两类数据。

## 固定版本与发行源

- [OpenTelemetry Collector Contrib `0.157.0`](https://github.com/open-telemetry/opentelemetry-collector-releases/releases/tag/v0.157.0)
- [Tempo `2.10.7`](https://github.com/grafana/tempo/releases/tag/v2.10.7)
- [Prometheus `3.13.1`](https://github.com/prometheus/prometheus/releases/tag/v3.13.1)
- [Grafana OSS `13.1.1`](https://github.com/grafana/grafana/releases/tag/v13.1.1)

Tempo 保持在最新的 2.x 修订版，避免把 Tempo 3.0 的 Kafka 架构和迁移要求带入这个单机参考栈。所有镜像均固定版本，禁止 `latest`。

## 网络与数据边界

- 宿主只开放 `127.0.0.1:4317`（OTLP/gRPC）和 `127.0.0.1:3000`（Grafana）。
- Tempo、Prometheus、Collector 的 Prometheus/self-metrics 端口只在 compose network 内可见。
- Grafana 禁止 anonymous；密码从未跟踪的 `.env` 注入。
- Grafana 禁用默认插件预装、插件管理和公网 public-key retrieval，避免启动时产生隐式下载。
- trace 本地保留 72 小时，metrics 本地保留 7 天；这些值只用于开发，不是生产合规或 SLA 承诺。
- Collector 不配置 logging/debug exporter，不把业务 telemetry 复制到容器日志。
- 本目录不包含 Alertmanager、外部通知、SaaS endpoint 或公网 telemetry 出站。

## 启动

在本目录复制 `.env.example` 为 `.env`，把 placeholder 替换为仅用于本机的强密码。`.env` 已由仓库根 `.gitignore` 排除。

```powershell
docker compose --env-file .env -f docker-compose.observability.yml config
docker compose --env-file .env -f docker-compose.observability.yml up -d
```

本机应用按需显式开启：

```powershell
$env:RAG_OBSERVABILITY_TRACING_ENABLED='true'
$env:RAG_OBSERVABILITY_METRICS_ENABLED='true'
$env:RAG_OBSERVABILITY_EXPORT_ENABLED='true'
$env:RAG_OBSERVABILITY_OTLP_ENDPOINT='http://127.0.0.1:4317'
```

三个开关默认都是 `false`。metrics 可独立于 tracing 开启；export 开启但 signal 关闭时不会为该 signal 创建 exporter。C12 只允许 loopback OTLP endpoint，非法或远程地址会安全禁用 exporter，不阻断业务启动。

## 本机查询

- Grafana：`http://127.0.0.1:3000`
- Dashboard：`Enterprise RAG / Enterprise RAG Observability`
- Tempo datasource：使用固定 `service.name=enterprise-rag-qa` 的 TraceQL 入口。
- Prometheus datasource：展示 operation、stage、provider、fallback、actual token coverage 与 Collector 自身状态。

告警规则是 `non_sla_reference`：ask 10 分钟错误率大于 5% 且至少 20 次操作；provider 15 分钟 fallback 比率大于 10% 且至少 20 次实际调用；Collector 出现 receive/export/refuse failure。当前没有可信生产基线，因此不设置 latency SLA 告警。

## 验证与停止

```powershell
python -B -m unittest scripts.test_observability_reference
docker compose --env-file .env -f docker-compose.observability.yml exec -T otel-collector /otelcol-contrib validate --config=/etc/otelcol-contrib/config.yaml
docker compose --env-file .env -f docker-compose.observability.yml exec -T prometheus promtool check config /etc/prometheus/prometheus.yml
docker compose --env-file .env -f docker-compose.observability.yml exec -T prometheus promtool check rules /etc/prometheus/alerts.yml
docker compose --env-file .env -f docker-compose.observability.yml down
```

停止命令不带 `-v`，本地 volumes 会保留。若要清除 volume 数据，应由用户另行明确授权。
