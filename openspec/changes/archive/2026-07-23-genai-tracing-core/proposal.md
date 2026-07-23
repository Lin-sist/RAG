# Proposal: C11 GenAI Tracing Core

## Summary

为当前 RAG 原型建立可测试、可审计的 OpenTelemetry tracing core：引入由 Spring Boot 3.2.1 既有依赖管理约束的 OTel API/SDK，建立彼此分离的 document ingest trace 与 ask trace，覆盖同步/流式问答和异步索引关键阶段，并用稳定 `taskId/documentId/chunkId` lineage 将 ask 使用的检索产物关联回 ingest 产物。

本 change 只交付进程内 tracing contract、手工 instrumentation、上下文传播、隐私白名单和 in-memory exporter 测试。OTLP/Zipkin/Jaeger exporter、metrics、告警、部署配置、生产采样和 telemetry backend 留给 C12 `otel-export-and-metrics`。

## Why Now

- C10 离线质量门禁框架已验收归档，冻结路线图下一主线是 C11 `genai-tracing-core`。
- 当前只有自定义 `X-Trace-Id/X-Span-Id`、MDC 和请求耗时日志，没有 OTel `Span`、跨异步上下文、阶段拓扑或进程内 span contract 验证。
- ingest 已有稳定 task ledger、确定性 chunk id 和 vector metadata；ask 已有 retrieval/generation/citation/provider/fallback diagnostics，具备低侵入 tracing 的业务锚点。
- C12 需要一个先被测试锁定的 span/attribute/privacy contract；若先接 exporter，再决定 trace 模型，容易把不稳定名称、原文或高基数数据提前固化到外部系统。

## Readiness And Capability Classification

- `confirmed`：启动前 HEAD=`d85d85a`，工作区干净，`main...origin/main [ahead 10]`，`.ai/ACTIVE_TASK.md=IDLE`，C10 已接受进 `evaluation` baseline 并归档，当前无其他 active change。
- `confirmed`：Spring Boot 3.2.1 parent 已管理 OpenTelemetry BOM `1.31.0`；仓库当前没有 OTel/Micrometer tracing runtime dependency。
- `confirmed`：现有 `TraceFilter/TraceContext` 提供 32-hex trace id、16-hex span id、MDC 与响应头；`RedisAsyncTaskManager` 通过 `CompletableFuture.supplyAsync` 跨线程执行但未传播上下文。
- `confirmed`：ingest 具备稳定 `taskId/documentId`、确定性 `chunkId` 和 vector metadata；ask 的 retrieved contexts 可观察 `documentId/chunkId`、requested/effective provider、fallback 和 citation diagnostics。
- `partial`：现有请求日志和业务 diagnostics 能说明局部状态，但不是可组合的 trace；流式 SSE 生命周期、异步任务、retry/fallback 与 stage latency 无统一 span 语义。
- `planned`：OTel API/SDK 依赖分层、可开关 SDK、W3C/custom header 兼容、MDC bridge、ingest/ask span topology、稳定 lineage、安全属性 allowlist、sync/stream/error/cancel in-memory verification。
- `out_of_scope`：C12 exporter/metrics/alerts/dashboard/deployment/sampling policy；Java agent 或全栈自动 instrumentation；生产 SLA；保存 raw question/prompt/answer/context/snippet；租户模型；评测指标或质量 profile 激活。
- `unknown`：真实 exporter/backend 的吞吐、费用、retention、权限和 sampling 方案；这些在 C12 前不得编造或写入生产配置。

## Goals

1. 以 OTel API/SDK 建立固定的 instrumentation scope、span name、status、attribute/event allowlist 与错误语义。
2. ask trace 覆盖 cache、retrieval、query embedding、vector/keyword route、fusion/rerank、prompt、LLM、citation 等实际执行阶段；未执行阶段不得伪造 span。
3. ingest trace 以 durable async task 为独立 root，覆盖 parse/chunk、document embedding、vector upsert、keyword upsert 和 SQL finalize；不得作为上传 HTTP span 的长生命周期 child。
4. 通过稳定 `taskId/documentId/chunkId` 关联 ingest 与 ask；不持久化 OTel trace/span id，不依赖历史 span 仍被 backend 保留。
5. 同步与流式问答都在真实 success/error/cancel/timeout 终态结束 span；线程切换不得丢失 parent/context 或污染后续任务。
6. 兼容现有 MDC 与 `X-Trace-Id/X-Span-Id`，同时支持 W3C `traceparent`；启用 tracing 时 OTel context 为权威来源。
7. telemetry 默认不含用户原文、文件名、用户名、user id、secret、Authorization、provider raw body、异常 message 或 stack trace。
8. 仅用 fake provider/in-memory exporter 完成 TDD；真实 embedding/rerank/ask/generation/LLM/provider 调用和数据出站保持 0。

## Non-Goals

- 不配置 OTLP、Zipkin、Jaeger、Prometheus 或第三方观测平台，不开放 collector 端口。
- 不新增 metrics、dashboard、alert、SLO/SLA、retention、生产 sampling 或部署 manifest。
- 不采用 Java agent 做全应用自动 instrumentation，也不追踪所有 CRUD/SQL/Redis/HTTP 请求。
- 不修改问答 API/DTO、数据库 schema、认证/权限、评测 dataset/profile、retrieval/chunking/rerank/prompt/citation/no-answer 公式或默认 provider。
- 不把 ingest 伪装成 ask 的祖先/子 span；历史 lineage 使用稳定业务 id，而不是长期保存 trace id。
- 不在 span/log 中记录 raw question、prompt、answer、context、citation snippet、文档正文、文件名、用户名、用户 id、credential、raw response、异常 message 或 stack trace。
- 不因 tracing 失败改变 QA/indexing 成败；telemetry 必须 fail-open，业务错误仍按既有契约处理。

## Proposed Scope

### 1. OTel Foundation And Compatibility Bridge

- 依赖版本继续由 Spring Boot 3.2.1 管理的 OTel BOM 约束；`rag-common` 仅暴露 API/contract，`rag-admin` 负责 SDK wiring，in-memory exporter 仅 test scope。
- 新增 `rag.observability.tracing.enabled` 开关，默认关闭以保持当前部署行为；关闭时使用 no-op provider，不能影响业务或现有 MDC。
- 启用时优先提取 W3C `traceparent`；无有效 W3C context 时才兼容合法 `X-Trace-Id/X-Span-Id`。非法 header 不进入 span context。
- OTel span context 同步到现有 MDC/响应头；作用域退出时恢复/清理，线程复用不得泄漏。

### 2. Separate Ingest And Ask Models

- 上传 HTTP 请求只负责接受任务。durable task 真正开始执行时创建独立 `rag.ingest` root，并以 span link 关联可用的 submission context；进程重启恢复时没有原 context 也必须创建合法独立 root。
- ask 使用独立 `rag.ask` lifecycle。同步 ask 在方法返回/抛错时结束；流式 ask 在 publisher 完成、error、cancel 或 SSE timeout 的真实终态结束。
- 固定 stage name，不按 provider、model、collection、document 或用户动态拼 span name。

### 3. Stable Lineage

- ingest span 使用 stable task/document/chunk counts；vector/keyword metadata 增加稳定 `ingestTaskId`，沿用现有 `documentId` 与确定性 chunk id。
- ask 只对最终选中的有界 contexts 产生 lineage events，记录稳定 task/document/chunk id 与 rank/score；旧索引缺 `ingestTaskId` 时标记 `PARTIAL`，不得让问答失败。
- 不把 OTel trace/span id 写入数据库或 vector metadata；跨时间查询通过业务 lineage key 关联两个独立 trace。

### 4. Privacy And Cardinality Contract

- span attributes 只允许固定 operation/stage/outcome/provider/model/protocol、cache/fallback/retry、count、topK、status 和安全错误分类。
- 高基数 lineage id/score 只进入有界 span event，不进入 span name、普通日志或未来 metrics label。
- 禁止 `Span.recordException` 默认展开敏感 message/stack；错误只记录 safe error type/category/code，并设置 OTel status。
- 实际 token usage 只有 provider 明确返回时才记录；估算 token 必须单独命名，缺失不得填 0。

### 5. In-Process Verification

- 使用 `InMemorySpanExporter`/test SDK 验证 topology、parent/link、attribute allowlist、status、终态和 context cleanup。
- fake embedding/vector/keyword/rerank/LLM/cache 覆盖 cache hit、no-result、fallback、retry、sync success/error、stream complete/error/cancel、ingest success/failure/resume。
- SensitiveLogs 增加 telemetry source 扫描；测试必须断言 raw sentinel 不出现在 span name/attribute/event/status/log capture。

## Risks And Mitigations

- **现有 trace id 与 OTel context 形成双重事实源**：启用时由 OTel context 生成 MDC/header；关闭时保留 legacy 行为，并用契约测试锁定切换。
- **异步 ingest 被错误挂成长 HTTP child**：durable task 使用新 root + optional link；重启恢复不依赖原 request context。
- **SSE 提前结束 span**：span 生命周期绑定 reactive/SseEmitter 终态，不在返回 `SseEmitter` 时结束。
- **instrumentation 侵入业务逻辑**：封装小型 tracer facade/scope helper，telemetry 异常全部 fail-open，并用既有行为回归证明结果未变。
- **raw 内容或异常泄漏**：allowlist builder + 禁用默认 exception recording + sentinel 测试 + SensitiveLogs 扫描。
- **span 爆炸或高基数扩散**：固定 stage names、仅最终 topK lineage events、retry 使用受限事件/计数；不采集每 token、每 chunk embedding span。
- **C11/C12 边界漂移**：本 change 不含 exporter、metrics、部署或生产 sampling；所有外部传输保持 0。

## Acceptance Criteria

1. OTel dependency scope、enabled/no-op wiring 与 instrumentation scope 被聚焦测试锁定，未显式启用时业务行为保持不变。
2. ingest 与 ask 是分离 trace；ingest 不作为 ask 子 span，upload request 与 async ingest 仅以 optional link 关联。
3. ask/ingest 固定阶段仅在真实执行时出现，success/error/cancel/timeout 状态与 parent/child 关系正确。
4. sync 与 SSE streaming 均在真实终态结束 span；async executor/Reactor 线程切换后 context 可用且完成后无泄漏。
5. stable `ingestTaskId/documentId/chunkId` lineage 可从 ingest 产物进入 ask final-context event；旧数据缺 lineage 时降级为 `PARTIAL` 而非业务失败。
6. cache hit、no-result、rerank fallback、LLM retry/error 与 ingest resume/failure 使用稳定安全字段表达，不记录 raw exception。
7. raw question/prompt/answer/context/snippet/content/file name/user identity/credential/provider body/error message/stack trace 均不出现在 telemetry；lineage events 数量有界。
8. 聚焦 Java tests、`mvn -q test`、Python 全量（确认无回归）、SensitiveLogs、Markdown 链接、受保护路径与 `git diff --check` 通过；frontend 未改时 build 明确 `SKIPPED`。
9. 实现与验证期间真实 embedding/rerank/ask/generation/judge/LLM/provider 调用、外部 exporter 传输与数据出站均为 0。

## Submission Responsibility

- `用户手动提交`。
- Agent 不暂存、不提交、不 push、不创建 PR、不部署。

