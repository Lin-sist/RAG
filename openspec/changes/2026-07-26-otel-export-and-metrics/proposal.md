# Proposal: C12 OTel Export And Metrics

## Summary

在 C11 已接受的进程内 GenAI tracing contract 之上，增加默认关闭、fail-open 的 OTLP trace/metric export，建立低基数阶段 metrics，并交付一个只在本机运行的自托管参考观测栈：OpenTelemetry Collector 接收应用遥测，Tempo 保存 trace，Prometheus 保存 metrics，Grafana提供受认证的 dashboard。

本 change 的目标是让现有 RAG 原型能够在明确开启时完成“产生遥测 → 本机导出 → 查询 trace/metrics → 触发本地规则”的闭环，不把它包装成生产观测平台。SaaS backend、跨主机外发、通知渠道、生产 SLA、多租户观测权限和长期容量规划不在 C12 范围内。

## Why Now

- C11 `genai-tracing-core` 已验收归档，4 requirements / 12 scenarios 已接受进 `rag-system` baseline；当前 `ACTIVE_TASK=IDLE`，无未归档 change。
- C11 runtime 只有 OTel API/SDK 与 in-memory verification，没有 network exporter、metrics、dashboard、alerts、sampling、retention、权限或部署配置。
- 冻结蓝图的下一顺序项明确为 C12 `otel-export-and-metrics`；C13 租户模型不依赖先修改 C11 trace contract。
- C11 已固定 span names、attributes、lineage 与隐私白名单，C12 现在可以在不重新定义业务 trace 的前提下设计传输与聚合边界。

## User Story

改之前，即使启用 C11 tracing，span 也只存在于进程内，开发者无法在服务外查询一次 ask/ingest 的阶段耗时、错误或 fallback，也没有低基数聚合指标。改之后，显式启动本机参考观测栈并开启 export，即可在 Grafana 中查看受采样 trace、阶段 metrics 与本地告警规则；关闭相关开关时，应用不建立外部连接且业务行为保持不变。

## Readiness And Capability Classification

- `confirmed`：规划启动前 HEAD=`5cdddd2`，`main...origin/main`，工作区干净，`.ai/ACTIVE_TASK.md=IDLE`，无其他未归档 change。
- `confirmed`：C11 已提供 OTel 1.31 API/SDK、固定 `rag.ask`/`rag.ingest` topology、stable lineage、W3C/custom context bridge、同步/流式终态与 telemetry allowlist。
- `confirmed`：当前 runtime 不含 network exporter；`GenAiTracingConfiguration` 使用 `parentBased(alwaysOn)` 且只创建 tracer provider；仓库没有 metrics provider、Collector/Tempo/Prometheus/Grafana 配置。
- `confirmed`：当前安全配置只公开 `/actuator/health`，没有可被匿名抓取的 metrics endpoint；C12 可避免新增公共 Actuator 暴露面，改用 OTLP push 到本机 Collector。
- `partial`：C11 spans 已含 stage/outcome/provider/fallback/token 等安全事实，但尚未形成独立、低基数、与 trace sampling 解耦的 metrics contract。
- `planned`：OTLP gRPC trace/metric export、bounded batching、低基数 metrics、Collector tail sampling、本机 Tempo/Prometheus/Grafana reference deployment、dashboard、Prometheus rules、retention/access boundary 与 integration smoke。
- `out_of_scope`：SaaS/云观测 backend、跨主机或公网 OTLP、Alertmanager/邮件/Slack 等通知、生产 SLA/SLO、HA、长期容量、租户级 dashboard 权限、Java agent、全栈自动 instrumentation、日志聚合。
- `unknown`：真实生产流量、长期存储量、组织合规保留期、生产告警阈值和多用户权限模型；这些不得由本机 smoke 推断。

## Goals

1. 在保持 C11 `rag.observability.tracing.enabled=false` 默认值的前提下，新增独立的 metrics/export 开关；所有开关默认关闭时不建立网络连接。
2. 使用 Spring Boot 3.2.1 已管理的 OTel 1.31 依赖线，通过 OTLP gRPC 将 trace 与 metrics 发送到显式配置的 Collector；export failure 不改变 QA/indexing 业务结果。
3. 建立固定名称、固定单位、低基数 label 的 ask/ingest/stage/provider/fallback/token/in-flight metrics；metrics 不依赖 trace 是否被保留。
4. 禁止 question、prompt、answer、context、snippet、document/user/lineage id、score、model 自由文本、credential、endpoint 和异常原文进入 metrics labels。
5. 交付本机自托管 reference deployment：Collector → Tempo/Prometheus → Grafana；只有 OTLP 与 Grafana 端口按 localhost 绑定，内部 backend 不直接暴露。
6. Collector 对 error/timeout/cancel/fallback trace 全保留，对普通成功 trace 使用固定比例 tail sampling；metrics 不采样。
7. 固定 reference retention：trace 72 小时、metrics 7 天；这只是本机开发默认，不代表生产合规策略。
8. 提供最小 dashboard 与本地 alert rules，覆盖请求量、错误率、fallback、阶段耗时、token coverage 和 collector/export failure；规则是诊断默认值，不宣称 SLA。
9. 通过 fake/in-memory、Collector config validation 与本机 synthetic smoke 验证闭环；不调用真实 embedding/rerank/ask/generation/judge/LLM provider，不发送知识库或用户数据。

## Non-Goals

- 不接入 Grafana Cloud、Datadog、New Relic、云厂商或其他 SaaS，不配置公网 endpoint、API key 或付费账号。
- 不提供生产 Kubernetes/云部署、HA、远程对象存储、备份、跨区域、容量模型或生产成本结论。
- 不引入日志 backend，不采集应用普通日志或容器日志。
- 不开放公共 `/actuator/prometheus`；metrics 由应用 OTLP push 到本机 Collector，再由 Collector 暴露内部 Prometheus scrape endpoint。
- 不加入 Alertmanager、邮件、短信、Slack/Teams webhook 等通知渠道，不在 tracked files 保存 Grafana/OTLP credential。
- 不修改 C11 span topology、lineage 持久化、问答 API/DTO、数据库 schema、认证语义、评测指标、retrieval/chunking/rerank/prompt/citation/no-answer 或默认 provider。
- 不把本机 dashboard/smoke 描述为 production readiness、SLA、容量或费用证明。

## Proposed Scope

### 1. Exporter And Runtime Configuration

- 保留 C11 tracing 开关，新增 `rag.observability.metrics.enabled` 与 `rag.observability.export.enabled`，三者默认均为 `false`。
- export 采用 OTLP gRPC；endpoint、timeout、batch/queue、metric interval 通过显式安全配置注入，tracked 默认 endpoint 只指向 `127.0.0.1`。
- `rag-admin` 负责 tracer/meter provider、batch processor/periodic reader 与 shutdown flush；业务模块继续只依赖 common facade，不直接依赖 exporter。
- exporter 初始化、queue pressure、timeout 或 backend unavailable 均 fail-open；只产生固定安全状态，不记录 endpoint、header、credential 或异常正文。

### 2. Low-Cardinality Metric Contract

- 固定 instruments 覆盖 operation count/duration/in-flight、stage duration、provider calls、fallback、actual token usage 与 token-usage coverage。
- label 只允许 bounded `operation/stage/outcome/provider/retrieval_route/fallback_reason/token_direction`；枚举外值归一为 `other/unknown`。
- lineage id、document/chunk/task/user id、rank/score、question/context、model 自由文本、error message/code、endpoint 均不得成为 label。
- metric 记录独立于 span sampling；trace 未保留时，计数、时延与错误/fallback 仍完整聚合。

### 3. Local Reference Deployment

- 新增独立 observability compose overlay/profile 与配置目录，不改变默认 `docker-compose.yml` 的启动行为。
- app 经 localhost OTLP 发送到 Collector；Collector tail-sample trace 后写入 Tempo，并把 metrics 暴露给内部 Prometheus；Grafana 预置 Tempo/Prometheus datasource 与最小 dashboard。
- 只将 Collector OTLP 和 Grafana 映射到 `127.0.0.1`；Tempo、Prometheus 与 Collector diagnostics 保持 compose network 内部可见。
- Grafana 禁止匿名访问，管理员密码只从未跟踪环境变量/secret 注入；仓库只提供 placeholder 与启动前检查。

### 4. Sampling Retention And Access

- 应用在 export-enabled reference mode 下把可采样 spans 交给本机 Collector，并尊重合法 remote parent 的 sampled decision。
- Collector tail sampling 保留全部 `ERROR/TIMEOUT/CANCELLED` 和发生 fallback 的 trace，对其余成功 trace 默认保留 10%；规则和比例可配置但须有界。
- Tempo retention 固定 72 小时，Prometheus retention 固定 7 天；清楚标注仅用于本机开发验证。
- 高基数 lineage 只存在于受采样 trace event；metrics、dashboard variables 和 alert labels 不使用 lineage id。

### 5. Dashboard Alerts And Verification

- dashboard 至少呈现 ask/ingest traffic、success/error/no-result/cache-hit、stage latency、provider/fallback、actual token coverage 和 trace drill-down。
- Prometheus rules 至少覆盖有最小流量门槛的 ask error ratio、provider fallback ratio，以及 Collector receive/export failure；不从当前开发数据猜测生产 latency SLA。
- notification delivery out of scope；规则只在 Prometheus/Grafana 中展示 pending/firing 状态。
- integration smoke 仅使用 synthetic metric/span，不走业务 provider；验证 trace 可查、metric 可查、隐私 sentinel 不泄漏、retention/access/port boundary 与停栈后业务 fail-open。

## Risks And Mitigations

- **默认开启导致意外出站**：tracing、metrics、export 三个开关默认关闭；reference endpoint 仅为 localhost，远程 endpoint 不进入本 change。
- **metrics label 爆炸**：只允许固定枚举 labels，未知值归一；lineage/model/id/score/error code 永不作为 label，并用 cardinality contract tests 锁定。
- **tail sampling 占用 Collector 内存**：固定 decision wait、expected new traces/sec 与 memory limiter；synthetic load smoke 验证 bounded behavior，不声称生产容量。
- **exporter 故障扩大业务故障域**：bounded queue、timeout、drop 而非阻塞业务；初始化/运行/关闭全部 fail-open，并有安全自诊断 metric/health 状态。
- **本机端口或 dashboard 泄漏 trace**：只绑定 localhost、禁匿名访问、不暴露 Tempo/Prometheus；凭据只从未跟踪环境注入。
- **把诊断阈值误当 SLA**：alert rule 明确标为 local diagnostic defaults，latency 只展示不告警，生产阈值留给真实容量/基线 change。
- **基础设施版本漂移**：实施前从官方发行源选择相互兼容版本并 pin version/digest；禁止 `latest`。版本选择与镜像下载须在用户批准 implementation 后进行。

## Acceptance Criteria

1. 默认配置下没有 exporter/metric reader、network connection 或新增公共 management endpoint，C11 legacy/default-off 行为不变。
2. 显式启用后，trace 与 metrics 通过 OTLP gRPC 发往配置的本机 Collector；backend 不可用、queue 满、timeout 与 shutdown flush failure 不改变业务结果。
3. 固定 instruments、units、buckets 和 bounded labels 被测试锁定；未知值归一且高基数/敏感字段无法成为 metric labels。
4. metrics 与 trace sampling 解耦；普通成功 trace 被 tail-sample 丢弃时，operation/stage/error/fallback/token metrics 仍保持正确计数。
5. reference compose 可从干净环境启动，只有 localhost OTLP/Grafana 对宿主开放，Grafana 无匿名访问，tracked files 无 secret。
6. synthetic smoke 能在 Tempo 查询 trace、在 Prometheus/Grafana 查询 metrics，并验证 error/fallback trace 全保留与普通成功 trace 比例采样。
7. retention 分别为 trace 72 小时、metrics 7 天；dashboard 与 alert rules 可加载，alert labels 不含高基数或用户内容。
8. 运行聚焦 Java tests、`mvn -q test`、Python 全量、SensitiveLogs、compose/config validation、dashboard/rules syntax、Markdown links、受保护路径与 `git diff --check`；前端无改动时 build 记为 `SKIPPED`。
9. 业务 provider 调用为 0；只允许在用户批准 implementation 后下载新增 Maven 依赖/固定 Docker images，并执行本机 synthetic observability smoke。

## External Calls Dependency And Cost Gate

- 规划阶段：真实 embedding/rerank/ask/generation/judge/LLM/provider/exporter 调用、镜像下载、遥测出站和费用均为 0。
- 进入实现前需用户明确批准：新增 OTel OTLP exporter/metrics SDK 依赖；下载固定版本的 Collector/Tempo/Prometheus/Grafana images；启动本机 synthetic smoke。
- 即使批准实现，也不授权真实业务 provider 调用、SaaS backend、跨主机/公网 telemetry、付费服务、push、PR 或部署。
- 本机参考栈无 SaaS 调用费，但会使用本机 CPU、内存、磁盘和首次镜像下载流量；实施记录必须给出实际镜像、端口、数据出站与 smoke 次数。

## Submission Responsibility

- `用户手动提交`。
- Agent 不暂存、不提交、不 push、不创建 PR、不部署。

