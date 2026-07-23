# RAG System Specification Delta

## ADDED Requirements

### Requirement: 分离的 Ingest 与 Ask Trace 身份

启用 GenAI tracing 时，系统 SHALL 使用 OpenTelemetry API/SDK 为 document ingest 与 ask 建立彼此分离的 trace。durable ingest task MUST 使用独立 root span；upload/request context 只可作为可选 span link，不得成为跨异步执行或重启恢复的强制 parent。ask trace MUST NOT 把历史 ingest span 当作 parent/child。

tracing 关闭或 telemetry 内部失败时，系统 MUST 保持既有 QA/indexing 业务行为；telemetry MUST fail open，但不得吞掉或改写业务异常、retry、fallback、持久化与返回语义。

#### Scenario: 上传接受后异步索引

- GIVEN 文档上传请求已创建稳定 taskId 并返回 202
- WHEN durable task 在线程池中开始执行
- THEN 系统创建新的 `rag.ingest` root span
- AND 若 submission context 仍可用则以 span link 关联
- AND ingest span 不作为 upload request span 的 child

#### Scenario: 重启后恢复索引任务

- GIVEN durable task 在进程重启后恢复且原 submission context 不可用
- WHEN reconciliation/resume 开始执行
- THEN 系统仍创建合法独立 `rag.ingest` root
- AND 使用稳定 task/phase/resume 属性表达恢复事实
- AND 不因缺少历史 trace context 拒绝恢复

#### Scenario: Ask 使用历史索引产物

- GIVEN ask 检索到此前 ingest 产生的 chunks
- WHEN 系统建立 `rag.ask` trace
- THEN ask 与 ingest 保持不同 trace identity
- AND 通过稳定 task/document/chunk lineage 关联产物
- AND 不持久化或依赖 ingest trace/span id

### Requirement: GenAI 阶段 Span 拓扑与生命周期

系统 SHALL 使用固定、非动态 span names 表达实际执行的 ask 与 ingest stages。未执行的 cache、retrieval route、rerank、generation、citation、keyword write 或 finalize 阶段 MUST NOT 产生伪 span。business no-result MUST NOT 被标为 telemetry error。

同步 ask SHALL 在返回或抛错时结束；流式 ask SHALL 在 complete、error、cancel 或 timeout 的真实终态结束且只结束一次。异步线程、Reactor/SSE 回调完成后 context 与 MDC MUST 被恢复或清理，不得泄漏到后续请求/任务。

#### Scenario: Cache Hit 跳过下游阶段

- GIVEN ask 启用 cache 且命中有效响应
- WHEN `rag.ask` 完成
- THEN trace 包含 cache lookup 与 `CACHE_HIT` outcome
- AND 不包含 retrieval、rerank、LLM 或 citation spans

#### Scenario: 流式问答被客户端取消

- GIVEN streaming ask 已开始并产生部分 chunks
- WHEN client disconnect 或 subscription cancel
- THEN `rag.ask` 以 `CANCELLED` 终态结束一次
- AND span duration 覆盖到取消时刻
- AND 不逐 token 创建 span/event

#### Scenario: Provider Retry 后成功降级

- GIVEN LLM 或 reranker 按既有契约发生 retry/fallback
- WHEN 最终业务请求成功
- THEN stage span 记录 bounded retry events、attempt/retry counts、requested/effective provider 与安全 fallback reason
- AND ask outcome 反映最终业务结果
- AND 不记录 raw provider response 或异常 message/stack

### Requirement: 稳定的 Ingest-to-Ask Lineage

ingest 产物 SHALL 使用稳定 `ingestTaskId`、`documentId` 与确定性 `chunkId` 建立 lineage。ask SHALL 只对最终选择的有界 contexts 记录 lineage events，其数量 MUST NOT 超过最终 selected topK。lineage id/rank/score MUST NOT 用于动态 span name、普通日志或 metrics labels。

旧索引缺少部分或全部 lineage 字段时，系统 SHALL 分别表达 `PARTIAL` 或 `MISSING`，但 MUST NOT 因 telemetry lineage 不完整改变检索、生成或响应结果。

#### Scenario: 完整 Lineage Round Trip

- GIVEN ingest 将 task/document/chunk identity 写入 vector/keyword metadata
- WHEN ask 最终选择对应 retrieved context
- THEN ask trace 产生有界 `rag.lineage.context` event
- AND event 可关联 ingest task、document 与 chunk
- AND lineage status 为 `COMPLETE`

#### Scenario: 旧索引缺少 Ingest Task Id

- GIVEN retrieved context 仍有 document/chunk identity 但没有 `ingestTaskId`
- WHEN ask 记录 lineage
- THEN lineage status 为 `PARTIAL`
- AND 系统保留可用 identity
- AND QA 结果与不启用 tracing 时一致

#### Scenario: 候选多于最终 TopK

- GIVEN retrieval/rerank 产生的候选数大于最终 selected topK
- WHEN ask 记录 lineage events
- THEN 只记录最终 selected contexts
- AND event 数量不超过 topK
- AND 未选择候选的 id/score 不进入普通 telemetry

### Requirement: OTel Context 兼容、隐私与进程内验证

启用 tracing 时，系统 SHALL 优先提取合法 W3C `traceparent`，仅在其不存在时兼容严格合法的 `X-Trace-Id/X-Span-Id` pair，并将当前 OTel trace/span id 同步到既有 MDC 与响应头。非法或不完整 header MUST NOT 成为 remote parent。

普通 telemetry MUST NOT 包含 raw question、prompt、answer、context、citation snippet、document content、file name/title、collection name、username/user id、credential、Authorization、provider raw body、异常 message 或 stack trace。系统 MUST NOT 使用默认 exception recording 绕过该限制。C11 SHALL 使用 in-memory exporter/fake dependencies 验证 topology、context、lineage、status 与隐私，不配置网络 exporter、metrics、告警或部署采样。

#### Scenario: W3C 与 Custom Header 同时存在

- GIVEN request 同时携带合法 W3C `traceparent` 与合法 custom trace/span headers
- WHEN tracing filter 提取 remote parent
- THEN W3C context 优先
- AND MDC/响应头使用当前 OTel context identity
- AND custom headers 不创建第二套 active context

#### Scenario: 敏感 Sentinel 贯穿失败路径

- GIVEN question/prompt/context/file/provider error 各包含唯一敏感 sentinel
- WHEN ask 或 ingest 失败并导出到 in-memory exporter
- THEN span name、attributes、events 与 status 中均不包含 sentinel
- AND 只保留 allowlisted error type/category/code
- AND 不记录异常 message 或 stack trace

#### Scenario: C11 无外部 Telemetry 传输

- GIVEN C11 runtime 或测试启用 tracing
- WHEN ask/ingest spans 被创建与验证
- THEN runtime 不注册 OTLP/Zipkin/Jaeger 等 network exporter
- AND metrics、alerts、dashboard、production sampling 与 deployment config 保持 out of scope
- AND 真实 provider/exporter 调用与数据出站为 0

