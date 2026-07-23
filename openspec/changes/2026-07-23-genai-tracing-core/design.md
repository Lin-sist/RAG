# Design: C11 GenAI Tracing Core

## Context

仓库已有两个容易被误认为“tracing 已完成”的局部能力：一是 `TraceFilter/TraceContext` 生成自定义 trace/span id 并放入 MDC/响应头；二是 QA、retrieval、rerank、LLM resilience 和 index task ledger 已输出若干 diagnostics。这些能力缺少 OTel context、span topology、异步传播和机器化隐私契约，不能形成 GenAI trace。

C11 的设计目标不是接入某个观测平台，而是先把进程内事实模型固定下来。C12 才决定 exporter、metrics、部署、采样和告警。因此 C11 必须让所有验证在 in-memory exporter 下完成，且 production/default path 不产生外部网络调用。

## Proposed Architecture

### Dependency And Module Boundary

- 使用 Spring Boot 3.2.1 已管理的 OpenTelemetry BOM `1.31.0`，不另建漂移版本。
- `rag-common` 依赖 `opentelemetry-api`，承载固定 attribute keys、safe span helper、MDC bridge 和 context carrier。
- `rag-core` 通过 common facade instrument ask/retrieval/generation；不依赖 SDK/exporter。
- `rag-admin` 依赖 `opentelemetry-sdk` 并负责 `OpenTelemetry/Tracer` bean、enabled/no-op wiring、request/async task integration。
- `opentelemetry-sdk-testing` 仅 test scope；C11 runtime 不含 exporter dependency。

### Runtime Switch

- `rag.observability.tracing.enabled=false` 为默认值。
- disabled：返回 no-op `OpenTelemetry/Tracer`，legacy MDC/header 行为保持不变。
- enabled：构建本地 SDK provider 与 fixed resource/instrumentation scope，但不注册 network exporter。测试配置注册 in-memory span processor。
- tracing 初始化或记录失败不得改变业务返回、重试、fallback 或持久化语义。

### Request Context And MDC Bridge

1. `TraceFilter` 优先使用配置的 W3C propagator 提取 `traceparent`。
2. 若无有效 W3C parent，再接受严格校验的 32-hex `X-Trace-Id` + 16-hex `X-Span-Id` 作为兼容 remote parent；不完整或非法 pair 被忽略。
3. 创建 request/server context 后，把当前 OTel trace/span id 写入 MDC 与响应头；disabled 时继续使用 legacy generator。
4. servlet async request 使用 async completion/error/timeout callback 结束 request scope；普通请求在 filter finally 结束。
5. scope close 后恢复先前 context 并清理 MDC，不使用 `MDC.clear()` 擦除其他组件 key。

### Trace Topology

#### Ask

```text
rag.ask
├─ rag.cache.lookup                    (only when cache enabled)
├─ rag.retrieval
│  ├─ rag.embedding.query              (per query variant; bounded)
│  ├─ rag.vector.search                (per query variant; bounded)
│  ├─ rag.keyword.search               (only when enabled)
│  ├─ rag.retrieval.fusion             (only when both routes participate)
│  └─ rag.rerank                       (only when enabled and candidates exist)
└─ rag.generation                      (only when contexts exist and no cache hit)
   ├─ rag.prompt.build
   ├─ rag.llm.request
   └─ rag.citation.validate
```

- `rag.ask` outcome 使用固定枚举：`SUCCESS | CACHE_HIT | NO_RESULT | ERROR | CANCELLED | TIMEOUT`。
- cache hit 不创建 retrieval/generation span；no-result 不创建 generation span。
- rerank/LLM fallback、retry、requested/effective provider 沿用既有 diagnostics 枚举，不发明第二套结论。
- streaming span 在 reactive publisher/SSE 的 complete/error/cancel/timeout 终态结束；不逐 token 建 span/event。

#### Ingest

```text
rag.ingest                              (independent root)
├─ rag.ingest.input.open
├─ rag.ingest.parse-and-chunk
├─ rag.embedding.document               (one batch span, not per chunk)
├─ rag.vector.upsert
├─ rag.keyword.upsert                    (best-effort, only when attempted)
└─ rag.index.finalize
```

- upload request accepted context 只作为 optional `Span Link`；async ingest root 不继承其 parent id。
- retry 不复制整棵 ingest trace；同一 durable task 的当前执行建立一个 root，attempt/phase 作为安全属性或有界 event。
- coordinator/restart resume 没有 submission context 时照常创建 root，标记 `rag.ingest.resume=true` 和安全 phase。

### Lineage Model

- 写入 vector/keyword metadata 的稳定字段：`ingestTaskId`、既有 `documentId`、确定性 chunk id（vector document id）。
- ingest root 记录 `taskId/documentId`；不为每个 chunk 建 span。必要时用 bounded summary event 记录 chunk count。
- ask 在最终 selected contexts 上最多按最终 `topK` 产生 `rag.lineage.context` events：`ingestTaskId/documentId/chunkId/rank/score`。
- 旧索引无 `ingestTaskId`：仍记录可用的 document/chunk identity，`rag.lineage.status=PARTIAL`；没有任何 lineage 时为 `MISSING`，但 QA 继续。
- 不保存 ingest trace id/span id。跨时间查询以 stable business lineage 查找对应 ingest/ask spans，避免 telemetry retention 成为业务依赖。

### Attribute And Event Contract

固定低基数 attributes：

- `rag.operation`、`rag.stage`、`rag.outcome`；
- `rag.cache.enabled/hit`；
- `rag.retrieval.route`、`rag.candidate.count`、`rag.selected.count`、`rag.top_k`；
- `rag.provider.requested/effective`、`rag.model`、`rag.protocol`；
- `rag.fallback.count/reason`、`rag.attempt.count/retry.count`；
- `rag.error.type/category/code`；
- `rag.ingest.phase/resume/chunk_count`；
- `rag.token.input/output` 仅在 provider 返回真实 usage 时存在；`rag.prompt.estimated_tokens` 单独表达估算。

高基数 lineage 只放在 bounded `rag.lineage.context` event。span name 不拼接 provider/model/id。C11 不创建 metrics，因此不存在把这些值误用为 metrics tags 的实现入口。

禁止字段/行为：

- raw question、prompt、answer、context、snippet、document content、file name/title、collection name；
- username、user id、JWT/API key/Authorization/password、provider request/response；
- raw exception message、stack trace、`Span.recordException`；
- 动态 span names、逐 token event、逐 chunk embedding span、无限 candidate events。

### Error And Retry Semantics

- span error 使用 `StatusCode.ERROR` + allowlisted type/category/code；business `NO_RESULT` 不是 ERROR。
- provider retry 在单个 stage span 上记录 bounded `rag.retry` events 和 aggregate counts；不记录 raw message/body。
- fallback 记录 requested/effective provider、fallback count/reason；successful fallback 的 stage outcome 可以为 `FALLBACK_SUCCESS`，ask outcome 仍按最终业务结果。
- telemetry helper 捕获自身异常并 fail-open；不得吞掉业务异常或改变已有 retry/fallback。

## Verification Strategy

- Foundation tests：enabled/no-op、Boot-managed dependency、W3C/custom extraction、invalid header、MDC restore/cleanup。
- Topology tests：固定 names、parent/link、only-if-executed、status/outcome、retry/fallback、安全错误。
- Async tests：task submit context capture、independent ingest root、resume without parent、executor thread reuse cleanup。
- Streaming tests：complete/error/cancel/timeout 各只结束一次，span duration 覆盖真实 publisher 生命周期。
- Lineage tests：ingest metadata 写入、retrieval round-trip、bounded final-context events、legacy partial/missing。
- Privacy tests：给 raw question/prompt/answer/context/file/error 注入唯一 sentinel，遍历 exported spans 的 name/attributes/events/status，断言零命中。
- 回归：聚焦 modules 后运行 `mvn -q test`；Python 全量用于确认评测工具无旁路回归；SensitiveLogs、Markdown links、protected paths 和 `git diff --check`。

## 决策记录

### 决策 1：直接使用 OTel API/SDK，还是先引入 Micrometer Tracing 或 Java agent

1. **面临的选择**：直接 OTel API/SDK 手工埋点；Micrometer Tracing bridge；Java agent 自动 instrumentation。
2. **选了哪个 + 为什么**：选择直接 OTel API/SDK。冻结蓝图已指定 OTel，且 C11 需要精确控制 GenAI stage、lineage、隐私和 in-memory span contract。
3. **放弃的代价**：Micrometer bridge 会增加一层命名/依赖转换；Java agent 擅长通用 HTTP/DB，却无法替代业务级 ingest/ask/lineage 语义，并会扩大本 change 范围。

### 决策 2：API、SDK 与测试 exporter 放在哪些模块

1. **面临的选择**：全部放 `rag-common`；全部放 `rag-admin`；API/contract 下沉 common，SDK wiring 留 admin，testing exporter 仅 test scope。
2. **选了哪个 + 为什么**：选择第三种。core/document 可以使用稳定 facade，又不会把 SDK/exporter 强制传递到所有模块。
3. **放弃的代价**：全部 common 会污染依赖边界；全部 admin 会迫使 core 反向依赖应用层或复制 instrumentation。

### 决策 3：tracing 默认启用还是默认关闭

1. **面临的选择**：默认启用但无 exporter；默认关闭、显式开启；强制启用并同时接 exporter。
2. **选了哪个 + 为什么**：选择默认关闭、显式开启，保持当前部署行为和性能边界；测试配置启用并用 in-memory exporter 验证。
3. **放弃的代价**：默认启用会产生尚未量化的记录开销却无外部收益；强制 exporter 越过 C12 的部署、隐私和网络授权边界。

### 决策 4：ask 与 ingest 使用一棵长 trace 还是分离 trace

1. **面临的选择**：把 ingest 当 ask 的祖先；把 ask 当 ingest 的子任务；二者独立，以稳定 lineage 关联。
2. **选了哪个 + 为什么**：选择独立 trace。ingest 是异步持久任务，ask 可能在数小时或数天后发生，生命周期和 retention 完全不同。
3. **放弃的代价**：强行父子关系会制造超长/跨重启 trace，且把 telemetry retention 误变成业务依赖。

### 决策 5：upload request 与 async ingest 如何关联

1. **面临的选择**：async ingest 继承 upload parent；完全无关联；ingest 新 root + 可用时添加 submission link。
2. **选了哪个 + 为什么**：选择新 root + optional link，既保留提交来源，又不让短 HTTP span 成为 durable task 的生命周期父节点。
3. **放弃的代价**：直接继承会形成跨线程/长时间 child；完全无关联会丢失一次有价值的接受链路证据。

### 决策 6：跨时间 lineage 保存 trace id 还是业务 id

1. **面临的选择**：在 DB/vector metadata 保存 ingest trace/span id；只保存 `taskId/documentId/chunkId`；只靠文件名或 collection 名匹配。
2. **选了哪个 + 为什么**：选择稳定业务 id。它们已存在或可低风险补入 metadata，不依赖观测 backend 的 retention，也能支持重启和重建。
3. **放弃的代价**：保存 trace id 会把一次执行身份固化进业务数据；文件名/collection 不稳定且可能泄露内容或产生误关联。

### 决策 7：是否为每个 chunk、candidate 或 token 建 span

1. **面临的选择**：全量细粒度 span；阶段 span + bounded final-context events；只保留一个总 span。
2. **选了哪个 + 为什么**：选择阶段 span + 最终 topK bounded lineage events，在可诊断性、成本和高基数之间保持边界。
3. **放弃的代价**：全量 span 会爆炸并泄露更多标识；只有总 span 无法定位 embedding/vector/rerank/LLM 阶段问题。

### 决策 8：流式 ask 的 span 在哪里结束

1. **面临的选择**：controller 返回 `SseEmitter` 时结束；收到首 chunk 时结束；publisher/SSE 的 complete/error/cancel/timeout 终态结束。
2. **选了哪个 + 为什么**：选择真实终态结束，才能覆盖生成与交付生命周期并正确表达取消/超时。
3. **放弃的代价**：返回或首 chunk 就结束会系统性低估时长，且把后续错误伪装成成功。

### 决策 9：retry/fallback 用多 span 还是有界 event

1. **面临的选择**：每次 attempt 新建 child span；单 stage span + bounded retry events/aggregate；只记录最终结果。
2. **选了哪个 + 为什么**：选择单 stage span + bounded events/aggregate，复用现有 attempt/retry/fallback diagnostics，又避免 retry storm 造成 span 爆炸。
3. **放弃的代价**：逐 attempt span 成本高且难控；只看最终结果会丢失降级与重试事实。

### 决策 10：W3C 与现有自定义 header 的优先级

1. **面临的选择**：只保留自定义 header；只接受 W3C 并破坏兼容；W3C 优先、合法 custom pair fallback。
2. **选了哪个 + 为什么**：选择 W3C 优先 + custom fallback，给 C12 标准传播留入口，同时保住现有客户端和历史 traceId 字段。
3. **放弃的代价**：只用 custom 难接标准生态；立即移除 custom 会破坏前端、历史和排障习惯。

### 决策 11：错误详情是否调用 `recordException`

1. **面临的选择**：默认 `recordException`；记录完整异常 message；只记录 allowlisted type/category/code 与 ERROR status。
2. **选了哪个 + 为什么**：选择安全字段。默认 exception event 常包含 message/stack，可能携带 provider body、路径、问题或凭据。
3. **放弃的代价**：完整异常更易单点排障但泄露风险不可控；安全字段会牺牲部分细节，需由受控本地调试补充。

### 决策 12：token usage 缺失时如何表达

1. **面临的选择**：填 0；用 prompt 估算冒充实际 usage；实际值与估算值分开，缺失即省略。
2. **选了哪个 + 为什么**：选择分开表达，避免把 unknown 当 zero，也不把估算 token 包装成 provider 账单事实。
3. **放弃的代价**：填 0 会污染后续 metrics；混用估算会误导成本分析；省略意味着 C12 前 token coverage 可能不完整。

### 决策 13：telemetry 失败是否影响业务

1. **面临的选择**：tracing 初始化/记录失败时 fail closed；静默吞掉所有问题；fail-open 业务并记录固定安全诊断。
2. **选了哪个 + 为什么**：选择 fail-open + 安全诊断。observability 不能改变既有 QA/indexing 成败，但配置/实现问题仍应可定位。
3. **放弃的代价**：fail closed 会扩大故障域；完全静默会让 instrumentation 回归长期不可见。

### 决策 14：本 change 是否同时交付 exporter、metrics 与告警

1. **面临的选择**：C11 一次做完整观测栈；只做 exporter；只做 core contract/in-process verification，把其余留 C12。
2. **选了哪个 + 为什么**：选择只做 core。先稳定 span/隐私/lineage，C12 再基于真实 contract 决定外部传输和 metrics。
3. **放弃的代价**：一次做完会混入网络、部署、费用、采样和高基数风险；只做 exporter 仍缺少可信业务语义。

