# Design: C12 OTel Export And Metrics

## Context

C11 已把业务级 trace 事实固定在 `GenAiTelemetry`：`rag.ask` 与 `rag.ingest` 是分离 trace，实际执行阶段使用固定 span name，lineage 只以有界 event 表达，敏感字段由 allowlist 阻断。当前 `GenAiTracingConfiguration` 只注册本地 `SdkTracerProvider`，使用 `parentBased(alwaysOn)`，没有 span processor、network exporter、meter provider 或外部 backend。

C12 不重做 C11 instrumentation，而是补齐“安全导出、低基数聚合、本机查询”的运行闭环。由于项目仍是模块化工程原型，且生产流量、合规保留期、租户权限和预算未知，本设计选择可关闭、可删除的本机自托管 reference deployment，不把未知生产约束伪装成已解决。

## Proposed Architecture

```text
RAG application
  OTel tracer + meter providers
          │ OTLP/gRPC (explicit opt-in, localhost by default)
          ▼
OpenTelemetry Collector
  ├─ memory limiter + batch + tail sampling ──► Tempo
  ├─ metric normalization ────────────────────► Prometheus exporter endpoint
  └─ internal/self metrics ───────────────────► Prometheus
                                                   │
                                                   ▼
                                                Grafana
```

### Module And Dependency Boundary

- `rag-common` 继续承载单一 safe telemetry facade；在既有 tracer facade 旁增加 meter instruments 与 fixed label normalization，不依赖 exporter SDK。
- `rag-core`、`rag-document` 与业务 service 只在已经明确的 operation/stage lifecycle 上调用 common facade，不直接创建任意 metric name/label。
- `rag-admin` 负责 OTel SDK wiring：`SdkTracerProvider`、`SdkMeterProvider`、`BatchSpanProcessor`、`PeriodicMetricReader`、OTLP gRPC exporters、resource、shutdown 与 configuration validation。
- OTel Java 版本继续服从 Spring Boot 3.2.1 的 dependency management；新增依赖限定在 exporter/metrics 所需 artifacts，不引入 Java agent、Micrometer bridge 或 runtime test exporter。
- reference backend 配置放在独立 `deploy/observability/`，默认 compose 不隐式启动它。

### Runtime Switch And Configuration Model

保留并扩展三层开关：

- `rag.observability.tracing.enabled=false`：是否创建 C11 spans。
- `rag.observability.metrics.enabled=false`：是否创建/记录 C12 instruments。
- `rag.observability.export.enabled=false`：是否注册 OTLP readers/processors 并建立网络连接。

规则：

1. 三个默认值均为 `false`；默认启动无 OTLP connection。
2. metrics 可在 tracing 关闭时独立记录并导出；指标不能依赖 span 是否 sampled。
3. export 开启但某个 signal 关闭时，只导出已启用 signal，不为关闭 signal 建 provider。
4. tracked 默认 OTLP endpoint 为 `http://127.0.0.1:4317`；C12 reference mode 不接受公网/SaaS endpoint 或 tracked headers。
5. endpoint、timeout、queue、batch、metric interval、resource environment 与 sampling ratio 都由 typed properties 校验；越界值归一到安全范围或禁用对应 exporter，不让应用启动失败。
6. exporter/runtime failure 更新低基数内部状态与安全日志，不包含 endpoint、headers、exception message/stack；业务线程不等待远端恢复。

### Trace Export Lifecycle

- 使用 OTLP gRPC span exporter + bounded `BatchSpanProcessor`，不在业务线程同步 export。
- queue、batch、schedule、export timeout 使用明确上限；queue 满时丢 telemetry 并计安全 drop fact，不阻塞或改写业务响应。
- provider shutdown 尝试有界 flush；flush timeout/failure 不把正常应用关闭改为异常业务退出。
- remote W3C parent 的 sampled decision 继续由 parent-based sampler 尊重；无 remote parent 的 reference mode 生成可交给 Collector 判定的 spans。
- C11 的 fixed topology/allowlist/lineage 不变，C12 不新增 raw exception recording 或动态 span/event 名称。

### Metric Instruments And Units

首版 instruments：

| Instrument | Type / unit | Purpose |
|---|---|---|
| `rag.operation.count` | Counter / `{operation}` | ask/ingest 终态计数 |
| `rag.operation.duration` | Histogram / `s` | ask/ingest 完整生命周期 |
| `rag.operation.inflight` | UpDownCounter / `{operation}` | 当前进行中的 ask/ingest |
| `rag.stage.duration` | Histogram / `s` | C11 固定 stage latency |
| `rag.provider.call.count` | Counter / `{call}` | 实际 provider call outcome |
| `rag.fallback.count` | Counter / `{fallback}` | bounded fallback reason |
| `rag.token.usage` | Counter / `{token}` | provider 返回的 actual input/output token |
| `rag.token.usage_coverage` | Counter / `{observation}` | usage present/missing coverage，不把 missing 当 0 |

允许 labels：

- `rag.operation`：`ask | ingest`；
- `rag.stage`：复用 C11 固定 stage allowlist；
- `rag.outcome`：固定 success/cache-hit/no-result/error/cancelled/timeout/fallback-success 等有限枚举；
- `rag.provider.requested/effective`：只接受 provider registry 的固定规范值，其他归一为 `other`；
- `rag.retrieval.route`、`rag.fallback.reason`、`rag.token.direction`：只接受既有 bounded taxonomy；
- 未知或非法枚举归一为 `unknown/other`，不透传原字符串。

明确禁止 labels：

- task/document/chunk/user/QA/KB/trace/span id、rank、score、topK 数值；
- question、prompt、answer、context、snippet、content、file/title、collection；
- model 自由文本、endpoint、host、credential、Authorization、异常 type/message/code/stack；
- 任何由请求、provider body、metadata map 或业务 id 直接提供的值。

Histogram boundaries 使用静态、可审查的秒级 bucket，覆盖毫秒级本地阶段到分钟级生成/索引；不依据本次开发样本自动调参。metrics 在 operation/stage 真实结束时写一次，retry 不重复计算整个 operation；provider actual calls 单独计数。

### Collector Pipeline And Tail Sampling

Collector 使用独立 trace/metric pipelines：

- 通用 processors：memory limiter、resource normalization、batch。
- trace pipeline：先按 C11 安全 attributes 做 tail sampling，再 OTLP 写入 Tempo。
- sampling policy：`ERROR/TIMEOUT/CANCELLED` 与 `fallback.count>0` trace 保留 100%；其余正常成功 trace 默认 deterministic 10%。
- metrics pipeline：不采样；向 Collector 内部 Prometheus exporter 暴露 app metrics，并合并 Collector 自身 receive/export/drop 指标。
- Collector config 不使用 logging/debug exporter 输出 span body，避免 lineage event 被普通容器日志复制。

Tail sampler 必须配置 decision wait、num_traces 与 expected_new_traces_per_sec 上限；synthetic load 只验证 bounded/config-valid，不形成生产容量结论。

### Reference Deployment And Network Boundary

建议目录：

```text
deploy/observability/
├─ docker-compose.observability.yml
├─ .env.example
├─ collector/config.yaml
├─ tempo/tempo.yaml
├─ prometheus/prometheus.yml
├─ prometheus/alerts.yml
└─ grafana/
   ├─ provisioning/datasources/
   ├─ provisioning/dashboards/
   └─ dashboards/rag-overview.json
```

- Collector、Tempo、Prometheus、Grafana images 必须 pin 明确 version；实施前按官方发行说明验证兼容性，禁止 `latest`。
- 宿主只映射 `127.0.0.1:4317` 与 `127.0.0.1:3000`。Tempo、Prometheus、Collector Prometheus/debug/health ports 只在 compose network 内开放。
- Grafana `auth.anonymous.enabled=false`；admin password 通过未跟踪环境变量/secret 注入，`.env.example` 只放 placeholder。
- Tempo 使用本地 volume 与 72h retention，Prometheus 使用本地 volume 与 `--storage.tsdb.retention.time=7d`。这两个值是开发 reference default，不是生产政策。
- 停止 reference stack 后，应用 exporter 失败必须 fail-open；重新启动后无需重启业务服务也能恢复后续 export，具体以 SDK 行为测试/smoke 为准。

### Dashboard And Alert Rules

Dashboard 固定展示：

- ask/ingest rate、终态分布与 in-flight；
- operation/stage P50/P95/P99；
- provider actual calls、effective provider、fallback rate/reason；
- actual token usage 与 usage coverage；
- Collector receive/export/drop 状态；
- 从 exemplar/trace id 可用处进入 trace，若当前 OTel 版本链路不支持 exemplar 则使用 Grafana trace 查询，不伪造关联。

初始 Prometheus rules：

- ask 10 分钟 error ratio 超过 5%，且窗口内至少 20 次 operation；
- provider 15 分钟 fallback ratio 超过 10%，且窗口内至少 20 次 calls；
- Collector 明确报告 receive refusal、export failure 或持续 queue/drop；
- 不设置 latency SLA alert：当前没有可信生产基线，latency 只进 dashboard。

这些阈值是本机诊断默认值，rule annotation 必须写明 `non_sla_reference`。C12 不部署 Alertmanager，不配置通知 webhook。

### Security Privacy And Cardinality Verification

- 复用 C11 sentinel corpus，遍历 exported trace 与 metric points，确认 raw question/prompt/answer/context/file/user/credential/error sentinel 零命中。
- 对 metric label set 做 allowlist 与最大 series 组合测试；输入大量随机 id/model/error string 时 series 数不得随输入线性增长。
- 对 compose resolved config 扫描：只允许 localhost host binding、无 anonymous Grafana、无 secret value、无 public Tempo/Prometheus port。
- 对 Collector config 扫描：无 logging/debug exporter 输出业务 telemetry，无未经 tail sampling 的 trace export path。
- trace 中的 lineage event 仍属高基数、潜在敏感 operational metadata；只有本机认证管理员可查询，retention 受 72h 限制。

## Verification Strategy

1. **Configuration RED→GREEN**：默认无 exporter、signal 独立开关、typed bounds、invalid/unreachable endpoint fail-open、shutdown flush bounded。
2. **Metric contract RED→GREEN**：instrument name/type/unit/bucket、一次性 lifecycle recording、actual-vs-missing token、unknown normalization、sampling independence。
3. **Privacy/cardinality**：sentinel export inspection、随机高基数 input 不扩 series、禁止字段扫描。
4. **Collector/static**：固定版本、compose render、Collector validate、Prometheus rule check、dashboard JSON/datasource provisioning parse、端口与凭据扫描。
5. **Synthetic integration**：只产生 synthetic ask/ingest spans/metrics，确认 Tempo/Prometheus 可查询、关键 trace sampling policy 与 metric completeness；不走登录、KB、retrieval 或 provider。
6. **Failure smoke**：停 Collector、制造 queue/export failure、恢复 Collector，确认应用业务 fake path 不失败且后续 telemetry 恢复。
7. **Regression**：聚焦 modules 后执行 `mvn -q test`、Python 全量、SensitiveLogs、Markdown links、protected paths、`git diff --check`；前端未改记 `SKIPPED`。

## 决策记录

### 决策 1：使用 SaaS backend 还是本机自托管参考栈

1. **面临的选择**：直接接 Grafana Cloud/商业平台；只交付 exporter 不提供 backend；交付本机 Collector + Tempo + Prometheus + Grafana reference stack。
2. **选了哪个 + 为什么**：选择本机自托管 reference stack，能验证完整闭环，又不引入账号、付费、跨主机出站和供应商锁定。
3. **放弃的代价**：SaaS 会把用户数据与费用授权带入 C12；只做 exporter 无法证明查询、sampling、retention、dashboard 和 alert contract 可工作。

### 决策 2：trace 与 metrics 使用什么传输协议

1. **面临的选择**：trace 用 OTLP、metrics 暴露 Actuator Prometheus；两种 signal 都走 OTLP gRPC；分别使用厂商专有协议。
2. **选了哪个 + 为什么**：选择 trace/metrics 都走 OTLP gRPC 到本机 Collector，保持单一出站边界，也避免新增公共 management endpoint。
3. **放弃的代价**：Prometheus scrape app 需要额外暴露/保护端口；厂商协议会耦合 backend，破坏本机参考栈的可替换性。

### 决策 3：metrics 使用 OTel Meter 还是 Micrometer bridge

1. **面临的选择**：直接 OTel Meter；引入 Micrometer/OTel bridge；同时维护两套 instruments。
2. **选了哪个 + 为什么**：选择直接 OTel Meter，与 C11 现有 API/SDK 和 OTLP pipeline 一致，并能用同一个 facade 强制 label allowlist。
3. **放弃的代价**：Micrometer bridge 会增加语义转换与依赖层；两套 instruments 会产生重复 series 和名称漂移。

### 决策 4：默认是否启用 export 与 metrics

1. **面临的选择**：默认全开；tracing 开启时自动 export；tracing/metrics/export 分别显式开启且默认关闭。
2. **选了哪个 + 为什么**：选择三个独立、默认关闭的开关，保证升级后无意外网络连接，并允许 metrics-only 验证。
3. **放弃的代价**：默认全开会产生未知开销/出站；隐式联动让运维无法只开本地 trace 或只开 metrics。

### 决策 5：在应用还是 Collector 做主要采样

1. **面临的选择**：应用固定 head ratio；Collector tail sampling；应用先低比例 head sampling、Collector 再 tail sampling。
2. **选了哪个 + 为什么**：选择 reference mode 将可采样 spans 送到本机 Collector，由 Collector tail sampling 全保留错误/fallback、比例保留普通成功，才能按最终 outcome 决策。
3. **放弃的代价**：纯 head sampling 会随机丢失错误/fallback；双重采样会在到达 Collector 前永久丢失关键 trace，并让比例难解释。

### 决策 6：metrics 是否携带 model、lineage 与 error code labels

1. **面临的选择**：完整复用 span attributes；只允许 bounded operational labels；对高基数值做 hash 后作为 label。
2. **选了哪个 + 为什么**：选择 bounded operational labels；model 自由文本、lineage、id、score、error code 全部排除，避免 series 爆炸和可逆关联风险。
3. **放弃的代价**：全量复用会让每个文档/模型/错误形成新 series；hash 仍保持高基数且可能被关联，不解决成本与隐私问题。

### 决策 7：metrics 与 trace sampling 是否耦合

1. **面临的选择**：只在 sampled span 上记 metrics；metrics 独立记录全部 operation；从 Tempo 离线聚合 metrics。
2. **选了哪个 + 为什么**：选择 metrics 独立记录全部 operation，才能让 error rate、fallback 与延迟分母不受 trace sampling 偏差影响。
3. **放弃的代价**：sampled-only metrics 会产生选择偏差；离线从 trace 聚合延迟高且在未采样请求上缺数据。

### 决策 8：是否暴露应用 `/actuator/prometheus`

1. **面临的选择**：公开主端口 endpoint；单独 management port 并加认证；不暴露应用 metrics endpoint，OTLP push 到 Collector。
2. **选了哪个 + 为什么**：选择 OTLP push，不增加当前 SecurityFilterChain 的公共/管理入口，也让 Prometheus 只访问 compose 内的 Collector。
3. **放弃的代价**：公开 endpoint 扩大攻击面；独立管理端口仍需额外网络、认证和部署语义，超出本机 reference 的必要范围。

### 决策 9：retention 取什么边界

1. **面临的选择**：无限保留；trace/metrics 都只保留 24h；本机 trace 72h、metrics 7d，生产值保持 unknown。
2. **选了哪个 + 为什么**：选择 72h/7d，在本机排障窗口与磁盘风险之间取明确边界，并避免冒充生产合规政策。
3. **放弃的代价**：无限保留会持续积累 lineage/磁盘；统一 24h 不利于观察一周趋势；直接写生产保留期没有业务/合规依据。

### 决策 10：Grafana 与 backend 如何暴露

1. **面临的选择**：所有端口对宿主/LAN 开放；只开放 Grafana；localhost 开放 Grafana 与 OTLP，其余服务仅内部可见。
2. **选了哪个 + 为什么**：选择 localhost Grafana + OTLP，应用能从宿主发 telemetry，用户能查看 dashboard，同时 Tempo/Prometheus 不形成额外入口。
3. **放弃的代价**：全部开放泄漏数据面；只开放 Grafana 会让宿主运行的应用无法连接容器内 Collector，除非改变主应用部署方式。

### 决策 11：Grafana 访问凭据如何处理

1. **面临的选择**：tracked 默认密码；anonymous read-only；未跟踪环境变量/secret + 启动前校验。
2. **选了哪个 + 为什么**：选择未跟踪 secret 注入并禁用 anonymous，符合仓库不写 credential 的规则，也保护含 lineage 的 trace。
3. **放弃的代价**：tracked 密码会形成已知凭据；anonymous 即使只读也会暴露 operational metadata。

### 决策 12：告警做到哪一层

1. **面临的选择**：只做 dashboard；加入 Prometheus rules 但不发通知；同时部署 Alertmanager 与外部通知渠道。
2. **选了哪个 + 为什么**：选择本地 rules、不配置通知，能验证 alert expression/status，又不引入 webhook、联系人、费用和外部副作用。
3. **放弃的代价**：只做 dashboard 不能关闭 C12 alert 可运行性缺口；外部通知需要新的隐私、credential、rate-limit 与所有权决策。

### 决策 13：是否现在定义 latency SLA 告警

1. **面临的选择**：按经验直接定 P95 阈值；从开发 smoke 自动推导；只展示 latency，等真实生产基线后单独定 SLA。
2. **选了哪个 + 为什么**：选择只展示不告警；当前开发 fixture、冷启动和 provider latency 不能代表生产负载。
3. **放弃的代价**：经验阈值容易误报或掩盖问题；从 smoke 推导会把不具代表性的样本伪装成服务目标。

### 决策 14：exporter/backend 故障是否阻断业务

1. **面临的选择**：配置或运行失败时 fail closed；无限阻塞/重试直到恢复；bounded queue/timeout 后 drop telemetry，业务 fail-open。
2. **选了哪个 + 为什么**：选择有界 fail-open，继承 C11 契约并限制观测故障域；丢失通过低基数内部事实呈现。
3. **放弃的代价**：fail closed 会让观测系统变成业务依赖；无限重试会耗尽线程/内存并放大 backend outage。

### 决策 15：是否在 C12 同时做生产部署与容量证明

1. **面临的选择**：直接交付 production manifests/SLA；只交付本机 reference deployment；把 deployment 全部留到未来。
2. **选了哪个 + 为什么**：选择本机 reference deployment，满足可运行闭环，同时把未知流量、HA、合规、租户权限和成本留在明确的后续决策中。
3. **放弃的代价**：直接生产化会编造未给出的基础设施事实；完全不部署则无法验证 exporter、sampling、retention、dashboard 与 rules 的组合行为。

