# RAG System Delta: C12 OTel Export And Metrics

## ADDED Requirements

### Requirement: Default-Off OTLP Signal Export And Fail-Open Lifecycle

系统 SHALL 对 tracing、metrics 与 network export 提供相互独立的显式开关，且 tracked 默认值全部为关闭。默认启动 MUST NOT 创建 OTLP network connection 或新增公共 management endpoint。显式启用 export 时，系统 SHALL 使用 OTLP gRPC 将已启用 signals 发送到配置的本机 Collector。

exporter SHALL 使用 bounded queue、batch、interval 与 timeout。初始化失败、backend unavailable、queue pressure、export timeout 或 shutdown flush failure MUST NOT 改变 ask/indexing 的成功、失败、retry、fallback、持久化或响应语义。export diagnostics MUST NOT 包含 endpoint、headers、credential、异常 message 或 stack trace。

#### Scenario: 默认启动不外发遥测

- GIVEN tracked 默认配置未被覆盖
- WHEN 应用启动并执行 ask 或 ingest
- THEN 不注册 network exporter 或 periodic metric reader
- AND 不连接 OTLP endpoint
- AND C11 disabled/legacy 行为保持不变

#### Scenario: 仅启用 Metrics Export

- GIVEN metrics 与 export 显式开启而 tracing 保持关闭
- WHEN ask 或 ingest 执行
- THEN metrics 通过 OTLP 发送到配置的本机 Collector
- AND 不创建或导出 C11 spans
- AND metrics completeness 不依赖 trace context

#### Scenario: Collector 不可用

- GIVEN export 已启用且 Collector unreachable、queue full 或 export timeout
- WHEN 业务请求或索引任务完成
- THEN 业务结果与 exporter 可用时一致
- AND telemetry failure 只形成 bounded safe diagnostics/drop facts
- AND 不记录 endpoint、credential 或 raw exception content

### Requirement: Low-Cardinality GenAI Stage Metrics

系统 SHALL 为 ask/ingest operation count、duration、in-flight、固定 stage duration、actual provider calls、fallback、actual token usage 与 token-usage coverage 提供固定 instrument name、type、unit 和 histogram boundaries。metrics SHALL 在真实 lifecycle 上记录且独立于 trace sampling；未执行阶段、retry 或 fallback MUST NOT 造成 operation 分母重复计数。

metric labels MUST 限于 bounded operation、stage、outcome、provider、retrieval route、fallback reason 与 token direction taxonomy；未知值 SHALL 归一为 `unknown/other`。task/document/chunk/user/KB/QA/trace/span id、rank、score、topK、question、prompt、answer、context、snippet、file/title/collection、model 自由文本、endpoint、credential 与 error detail MUST NOT 成为 metric label。

#### Scenario: 普通成功 Trace 被采样丢弃

- GIVEN 一次成功 ask trace 未被 Collector 保留
- WHEN metric points 被聚合
- THEN operation count/duration、实际 stage、provider call 与 applicable token coverage 仍各自正确记录
- AND 分母不因 trace sampling 缩小
- AND metric 不伪造到不存在的 trace 关联

#### Scenario: 高基数输入不能扩张 Series

- GIVEN 多次请求携带不同 document/chunk/task/user id、model 文本、score 与 error code
- WHEN 系统记录同一 bounded operation/stage/outcome
- THEN metric label set 不包含这些输入
- AND series 数量只由批准的 bounded taxonomy 决定
- AND 非法或未知枚举归一为 `unknown/other`

#### Scenario: Token Usage 缺失

- GIVEN provider 未返回 actual token usage
- WHEN generation stage 完成
- THEN `rag.token.usage` 不以 0 或估算值填充 actual usage
- AND coverage 记录 usage missing
- AND prompt estimated token 如保留仍不冒充 provider actual usage

### Requirement: Local Reference Backend Sampling Retention And Access Boundary

C12 SHALL 提供独立、可选的本机 reference deployment，以 Collector 接收 OTLP、Tempo 保存 trace、Prometheus 保存 metrics、Grafana 查询两类信号。reference deployment MUST NOT 隐式加入默认 compose 启动；只有 OTLP 与 Grafana 可绑定宿主 localhost，Tempo、Prometheus 与 Collector internal endpoints MUST 保持内部可见。

Collector SHALL 全量保留 error、timeout、cancel 与 fallback traces，并按固定可配置比例保留其他成功 traces；metrics MUST NOT 被采样。reference trace retention SHALL 为 72 小时、metric retention SHALL 为 7 天。Grafana anonymous access MUST 关闭，credential MUST 由未跟踪 secret/env 注入；这些值 SHALL 被描述为本机开发默认而非生产 SLA、合规或容量承诺。

#### Scenario: Reference Stack 未启动

- GIVEN 默认 compose 或应用单独启动
- WHEN C12 reference profile 未被显式选择
- THEN Collector、Tempo、Prometheus 与 Grafana 不启动
- AND 应用不因 reference stack 缺失而失败
- AND 不产生公网或 SaaS telemetry 出站

#### Scenario: 本机 Trace Sampling

- GIVEN synthetic traces 同时包含 error/fallback 与普通 success outcomes
- WHEN Collector tail sampling 完成
- THEN error、timeout、cancel 与 fallback traces 全部进入 Tempo
- AND 普通 success 只按配置比例进入 Tempo
- AND 对应 metrics 全部进入 Prometheus

#### Scenario: 未授权访问 Backend

- GIVEN 用户未提供 Grafana credential 或尝试直接访问 backend internal port
- WHEN 请求 dashboard、Tempo 或 Prometheus
- THEN Grafana 不允许 anonymous access
- AND Tempo、Prometheus 与 Collector internal endpoints 不通过宿主端口暴露
- AND tracked files 不包含可用 credential

### Requirement: Queryable Dashboard Local Rules And Synthetic Verification

reference deployment SHALL provision 可查询的 Tempo/Prometheus datasources、RAG overview dashboard 与本地 Prometheus alert rules。dashboard SHALL 覆盖 ask/ingest traffic、outcome、in-flight、stage latency、provider/fallback、actual token coverage 和 Collector/export state。alert rules SHALL 至少覆盖带最小流量门槛的 ask error ratio、provider fallback ratio 与 Collector receive/export/drop failure，并标明其为 non-SLA reference defaults。

C12 acceptance SHALL 使用 synthetic spans/metrics 验证 export、query、sampling、retention、access、privacy 与 fail-open，不得为了观测验收调用真实 embedding、rerank、ask、generation、judge、LLM 或其他业务 provider。notification delivery、production latency SLA、SaaS backend 与跨主机 telemetry SHALL 保持 out of scope。

#### Scenario: Dashboard 和 Rules 可加载

- GIVEN reference stack 使用固定版本配置启动
- WHEN Grafana provisioning 与 Prometheus rules 被加载
- THEN dashboard 可查询规定的 trace/metric panels
- AND rule expressions 可解析并展示 pending/firing 状态
- AND variables、legend、annotations 与 alert labels 不含 lineage 或用户高基数内容

#### Scenario: 敏感 Sentinel 进入导出路径

- GIVEN synthetic question、prompt、context、file、user、credential 与 error 各含唯一 sentinel
- WHEN trace 与 metrics 经 Collector 导出并被查询
- THEN span allowlist 继续阻止 C11 禁止内容
- AND metric names、labels、resource、dashboard 与 alert output 均不含 sentinel
- AND Collector 不用 logging/debug exporter 把业务 telemetry 复制到普通日志

#### Scenario: Synthetic 验收不触发业务外调

- GIVEN C12 integration verification 被执行
- WHEN 生成测试 traces、metrics、failure 与 recovery evidence
- THEN 不调用真实 embedding、rerank、debug retrieval、ask、generation、judge、LLM 或 provider endpoint
- AND 不发送知识库、用户内容或 telemetry 到公网/SaaS
- AND 结果只证明本机 reference observability 闭环，不证明 production readiness 或 SLA

