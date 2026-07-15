# Design: C4b LLM Provider Resilience

## 1. Context

C4b 是故障语义系列的第一个独立 change。它只接受并实现 LLM provider 相关契约，不提前接受 Redis、Milvus 或索引恢复语义。公共故障契约矩阵在本 design 中作为模板落地，后续 C4c/C4d 复用相同维度，但各自只修改自身依赖的 baseline。

当前主路径：

```text
QAController
  -> RAGServiceImpl.retrieve
  -> AnswerGeneratorImpl
       -> OpenAI-compatible / Qwen WebClient
       -> timeout + optional Reactor retry
  -> GeneratedAnswer or LLMException
  -> QAResponse.success / QAResponse.error
```

主要缺口：同步与流式对同一故障的结果不同；流式 retry 对完整 Flux 重订阅；原始异常 message 可能进入异常链；controller 无条件增加查询次数并保存历史；现有测试只 mock `LLMException`，没有验证真实 HTTP status、尝试次数和输出序列。

## 2. Goals

- 为 LLM 429、5xx、timeout、network 和非重试型故障建立确定性分类与有界行为。
- 维持现有同步 API 的 HTTP 兼容，同时让 generation failure 机器可判定且不伪装成功内容。
- 保证流式首 chunk 后绝不自动重放请求，消除重复内容风险。
- 明确 cache、history、query count 的失败副作用。
- 只用本地合成服务验证，不产生真实 provider 调用或费用。

## 3. Non-goals

- 不做跨 provider failover、熔断、全局限流或 provider 健康路由。
- 不改成结构化 SSE，不改前端展示协议。
- 不改变 generation/citation/no-answer 质量指标。
- 不覆盖 Redis/Milvus、索引任务或 embedding provider 故障。
- 不默认开启 provider retry。

## 4. 公共故障契约矩阵模板

后续依赖故障 change 统一使用以下列：

| 维度 | 含义 |
|---|---|
| dependency / operation | 失败依赖与调用动作 |
| failure category | 稳定错误类别，不依赖原始 message 猜测 |
| retry eligibility | 是否允许重试及前置条件 |
| attempt budget | 首次尝试加重试的硬上限 |
| client outcome | 同步/SSE 的可观察结果 |
| side effects | cache、history、counter、task state 等写入 |
| diagnostics | 可记录/返回的安全字段 |
| verification | 确定性故障注入与断言 |

### 4.1 C4b LLM 矩阵

| operation / failure | category | retry | attempt budget | client outcome | side effects |
|---|---|---|---:|---|---|
| sync 429 | `rate_limit` | 仅 `maxRetries>0` | `1+N` | 耗尽后稳定 generation error | query count 1；无 cache/history |
| sync 500–599 | `provider_5xx` | 仅 `maxRetries>0` | `1+N` | 同上 | 同上 |
| sync timeout / I/O / connect | `timeout` / `network` | 仅 `maxRetries>0` | `1+N` | 同上 | 同上 |
| sync 400/401/403/404 等 | `provider_http_error` | 否 | 1 | 立即稳定 generation error | 同上 |
| sync 2xx malformed/empty | `invalid_response` | 否 | 1 | 立即稳定 generation error | 同上 |
| stream 首个可见 chunk 前的 transient failure | 同上 | 仅 `maxRetries>0` | `1+N` | 成功则正常流；耗尽则 `[ERROR]` + `[DONE]` | query count 1；无 history |
| stream 已输出可见 chunk 后任意 provider failure | 对应稳定类别 | 否 | 当前订阅终止 | 保留已发 chunk，追加 `[ERROR]` + `[DONE]`，不重放 | query count 1；不保存部分历史 |
| client disconnect / emitter send failure | `client_disconnect` | 否 | 不追加 provider 尝试 | 取消订阅并结束 | 不保存部分历史 |

`N` 是非负 `max-retries`。负值按 0 处理或在配置绑定时拒绝，最终实现选择必须通过测试固定；无论哪种实现，都不得形成无限重试。

## 5. Contract Decisions

### 5.1 Retry 是显式 opt-in

tracked `application.yml` 继续使用 `RAG_LLM_MAX_RETRIES:0`。这意味着默认每个请求只调用一次 provider，然后走稳定降级。运维显式配置 `N>0` 时：

- `max-retries` 表示首次请求之外的最大重试数；
- 总尝试上限为 `1+N`；
- 只重试 429、5xx、timeout、I/O 和 connect；
- 非重试型 4xx、解析/配置错误不重试；
- 使用现有有界 exponential backoff，不承诺解释 provider-specific body；
- 当前 change 不承诺按 `Retry-After` 精确调度，header 可作为安全诊断但不得突破 `1+N` 上限。

不引入 provider fallback：requested provider 与 effective provider 相同。C4b 证明的是“失败可控”，不是“总能生成答案”。

### 5.2 同步问答兼容语义

同步 `/api/qa/ask` 暂时保留现有 HTTP 200 + `ApiResponse.success(QAResponse)` 外层，以避免本 change 同时重写前端和评测客户端。generation failure 必须满足：

- `QAResponse.metadata.status=error`；
- `answer` 只包含稳定用户提示，不包含 provider 原始 message/body；
- `citations=[]`、`contexts=[]`；
- metadata 保留安全诊断字段；
- `QAResponse.isSuccess()` 为 false；
- 不写 QA 成功 cache；
- query count 对一次被接受的用户 ask 只增加 1；
- controller 不保存失败问答历史。

如果未来要把 provider outage 映射为 HTTP 503，需要同时审查前端、评测脚本和外部客户端，另起 API 契约 change；C4b 不偷带该 breaking change。

### 5.3 流式问答语义

流式生成分为两个阶段：

1. `BEFORE_FIRST_CONTENT`：尚未向用户发送非空内容，可按配置预算重订阅 provider；
2. `AFTER_FIRST_CONTENT`：已发送至少一个非空内容 chunk，任何 provider 故障都不得重订阅。

失败终止仍沿用现有文本协议：发送稳定 `[ERROR] <message>`，随后发送 `[DONE]` 并 complete。禁止把 provider body、原始 exception message 或 prompt/context 放入错误 chunk。

首 chunk 后失败时已发送的内容不撤回，但 controller 不保存这段部分答案历史，因为当前 history schema 无法表达 incomplete。query count 在进入 ask endpoint 时增加一次，不因 provider retry 重复增加。

### 5.4 Timeout 语义

- 同步 timeout：从 provider 请求订阅到完整响应 body 的 configured deadline。
- 流式 timeout：首包或相邻 provider 数据之间的 inactivity deadline；SSE emitter 自身的 120 秒上限仍是 transport timeout，不等同于 provider timeout。
- client disconnect/emitter send failure 触发取消，不得被误分类为 provider retry。

### 5.5 稳定诊断字段

同步失败的 `metadata` 和内部 `LLMException.diagnostics` 计划统一包含：

- `provider` / flattened `llmProvider`；
- `endpoint` / `llmEndpoint`；
- `model` / `llmModel`；
- `timeoutSeconds` / `llmTimeoutSeconds`；
- `maxRetries` / `llmMaxRetries`；
- `attemptCount` / `llmAttemptCount`；
- `retryCount` / `llmRetryCount`；
- `retryExhausted` / `llmRetryExhausted`；
- `errorCategory` / `llmErrorCategory`；
- 可选 `httpStatus` / `llmHttpStatus`。

允许普通日志记录 provider、固定 endpoint path、model、timeout、attempt/retry count、HTTP status、error category 和 traceId。禁止记录：API key、Authorization header、provider response body、原始异常 message、prompt、question、context、snippet 或用户文件信息。

客户端提示从稳定 category 映射，不再通过 `message.contains("429")` 等字符串猜测 LLM 故障。向量索引等非 LLM 错误仍保留现有处理，避免 C4b 越界。

### 5.6 Cache、History 与 Counter

| 行为 | generation 成功 | generation 失败 | stream 部分输出后失败 |
|---|---|---|---|
| query count | +1 | +1 | +1 |
| QA cache | 可按现有配置写 | 不写 | 不写 |
| QA history | 写完整答案 | 不写 | 不写 |
| citations | 校验后返回 | 空 | 当前 SSE 不返回结构化 citations |

该设计不新增 history 状态字段或数据库 migration。以后若需要保存失败审计或 incomplete 历史，应单独设计非成功记录模型。

## 6. Implementation Shape

### 6.1 `rag-core`

- 在 `AnswerGeneratorImpl` 中把 retry eligibility、attempt tracking、stream first-content gate 和安全异常构造收敛为可测试逻辑。
- 保持 OpenAI-compatible 与 Qwen 请求/解析协议不变。
- `LLMException` 继续承载安全 diagnostics，不让原始 provider body 成为客户端 message。
- `RAGServiceImpl` 优先按 diagnostics category 生成稳定提示，补齐 attempts/retries 字段；不修改 retrieval/no-answer/citation 成功路径。

### 6.2 `rag-admin`

- `QAController` 在同步 response 非成功时跳过 history 保存，但 query count 仍只增加一次。
- SSE error、send failure、timeout 路径取消订阅并跳过 history；成功 complete 才保存完整 answer。
- 保持当前 HTTP 200 外层和文本 SSE 兼容。

### 6.3 测试边界

复用 JDK `com.sun.net.httpserver.HttpServer` 绑定 `127.0.0.1` 随机端口，不新增依赖、不访问网络。合成服务支持：

- 第一次 429、第二次成功；
- 连续 503 直至预算耗尽；
- 延迟响应触发 timeout；
- 401/400 证明不重试；
- 200 malformed body 证明不重试；
- 流式首 chunk 前失败后成功；
- 流式发出 `alpha` 后失败，断言订阅次数为 1 且 `alpha` 不重复；
- error body 中放入合成 secret/prompt marker，断言响应、metadata 和捕获日志均不包含 marker。

## 7. Planned File Boundary

预计实现只涉及：

- `rag-core/src/main/java/com/enterprise/rag/core/rag/generator/AnswerGeneratorImpl.java`；
- 必要时小改 `LLMException.java`、`LLMProperties.java`；
- `rag-core/src/main/java/com/enterprise/rag/core/rag/service/RAGServiceImpl.java`；
- `rag-core/src/test/java/.../AnswerGeneratorImplResilienceTest.java`；
- `rag-core/src/test/java/.../RAGServiceImplTest.java`；
- `rag-admin/src/main/java/.../QAController.java`；
- `rag-admin/src/test/java/.../QAControllerTest.java`；
- 本 change artifacts、`.ai/ACTIVE_TASK.md` 与 `.ai/AGENT_LOG.md`。

不计划修改前端、数据库 migration、API DTO shape、评测指标、embedding/rerank/vector store、受保护本地配置或依赖版本。若必须修改 `scripts/run_rag_eval.py` 或公开 DTO，先停下解释原因并重新审批范围。

## 8. TDD And Verification

实现按 RED → GREEN → REFACTOR：

1. 先添加本地 HTTP 故障测试，证明当前代码缺少 attempt/stream safety 契约；
2. 最小修复同步 retry/classification/diagnostics；
3. 添加流式首 chunk 前后测试并修复重订阅边界；
4. 添加 controller side-effect 测试并修复 history 保存条件；
5. 运行聚焦和完整门禁。

计划命令：

```powershell
mvn -q -pl rag-core -am test
mvn -q -pl rag-admin -am test
mvn -q test
python -B -m unittest discover -s scripts -p 'test_*.py'
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run_local_quality_gates.ps1 -Mode SensitiveLogs
git diff --check
```

无前端改动时不重复前端 build；若实现意外触及前端，必须运行包含 `vue-tsc` 的正式 build。

## 9. External Calls And Cost

全部故障测试访问 JVM 内 `127.0.0.1` 合成 server。真实 embedding/rerank/judge/ask/LLM 调用量均为 0，无业务数据出站、真实模型、provider 限流或费用。Maven 只解析仓库已声明依赖。

## 10. Alternatives

### A. 默认把 `max-retries` 改为 2 或 3

拒绝。它会在未显式选择的情况下放大每次真实 ask 的调用量、延迟和潜在费用。C4b 先锁定 opt-in 上限和安全降级。

### B. 所有 provider failure 都返回 HTTP 503

暂不采用。语义更纯粹，但会同时影响前端、评测脚本和外部客户端。本 change 保持 HTTP 兼容；未来可独立迁移。

### C. 流式错误后从头重试并让客户端去重

拒绝。客户端无法可靠识别模型重新生成的语义重复，且会造成用户可见内容倒退或重复。

### D. 自动切换另一个 provider

拒绝。两种 provider 的模型、费用、数据出站和答案行为不同，静默切换会破坏 attribution，也超出 C4b。

### E. 引入 Resilience4j/WireMock

暂不采用。现有 Reactor 与 JDK `HttpServer` 足以覆盖本 change；新增依赖需要额外维护和审批。

## 11. Rollback

回滚必须同时撤销 retry/stream gate、controller side-effect 变更、相关测试和尚未接受的 spec delta。不得只恢复默认配置或删除测试来掩盖实现与 baseline 不一致。因为本 change 不修改 schema，回滚不需要数据迁移。

## 12. Approved Review Decisions

用户已在事前闸门确认：

1. 接受 tracked 默认 `max-retries=0`，重试只由运维显式 opt-in，最大尝试数为 `1+N`；
2. 接受同步 generation failure 暂时保持 HTTP 200 外层兼容，以 `metadata.status=error` 表达失败；
3. 接受 query count 仍计 1 次，但同步失败、SSE 失败和部分答案都不保存 history、不写 cache；
4. 接受 SSE 只在首个可见 chunk 前重试，首 chunk 后任何故障均不重放；
5. 接受本 change 不做跨 provider failover、熔断器、`Retry-After` 精确调度或结构化 SSE；
6. 接受所有验证使用本地合成 HTTP server，真实 provider 调用量为 0。

## 13. Commit Responsibility

`Agent 提交`。用户已授权 Agent 对本 change 计划内文件执行本地暂存和中文提交；不 push。

## 决策记录

### 决策 1：故障矩阵随各依赖 change 落地，不单建 C4a
- **面临的选择**：单独创建 C4a 只写公共矩阵；一次性把 LLM、Redis、Milvus 全部写进同一 change；在 C4b/C4c/C4d 的 design 中复用同一模板并分别接受自身契约。
- **选了哪个 + 为什么**：用户已在事前闸门确认，选择第三种；它既保留统一分析维度，又保证 baseline 只接受已经随代码实现的依赖契约，不会出现规格领先实现或唯一 active change 被矩阵文档长期占用。
- **放弃的代价**：单建 C4a 会让已归档 baseline 提前承诺尚未实现的 Redis/Milvus 行为，或因不归档而阻塞后续 change；一次做完三类依赖会把范围放大成难以审查和回滚的跨模块改造。

### 决策 2：C4b 只处理 LLM，不顺带处理其他依赖故障
- **面临的选择**：只处理 LLM provider；同时纳入 Redis/Milvus；再把 embedding、索引恢复也一并纳入。
- **选了哪个 + 为什么**：用户已在事前闸门确认，选择只处理 LLM；同步/SSE、重试和问答副作用已经构成完整可验证切片，Redis/Milvus 与索引任务拥有不同状态机和降级边界，应分别由 C4c/C4d/C5 负责。
- **放弃的代价**：把其他依赖混入会让测试拓扑、失败状态和回滚面同时膨胀，任何一处未决都可能拖住整个 C4；继续扩到 embedding/索引恢复还会越过冻结蓝图的 change 顺序。

### 决策 3：重试保持默认关闭，由运维显式开启
- **面临的选择**：默认 `max-retries=0`、显式配置才重试；默认改成 2 或 3 次；彻底删除重试只保留立即失败。
- **选了哪个 + 为什么**：用户已在事前闸门确认，选择默认 0、显式配置 `N` 时最多尝试 `1+N` 次；这样不会在未授权情况下放大真实 provider 调用、延迟和费用，同时仍提供可测试的韧性开关。
- **放弃的代价**：默认开启会把一次 ask 静默放大成多次外调并加剧限流；完全删除重试则浪费现有能力，也无法让运维针对短暂 429/503 做受控恢复。

### 决策 4：同步失败暂时保留 HTTP 200 外层兼容
- **面临的选择**：改为 HTTP 503；保留 HTTP 200 并用 `metadata.status=error`；返回检索 contexts 作为 context-only 的 200 降级结果。
- **选了哪个 + 为什么**：用户已在事前闸门确认，选择保留 HTTP 200 外层并强化机器可判定 metadata；它能在不同时改造前端、评测脚本和外部客户端的前提下收紧故障语义。
- **放弃的代价**：直接改 503 会形成额外 breaking change，并可能让现有客户端丢失诊断；context-only 200 容易被误当成生成成功，还会引入“谁负责展示原始检索片段”的新产品契约。

### 决策 5：SSE 只允许在首个可见内容前重试
- **面临的选择**：整个 Flux 任意时点都可重试；完全禁止流式重试；首个可见 chunk 前可重试、之后立即终止且不重放。
- **选了哪个 + 为什么**：用户已在事前闸门确认，选择按首个可见内容划界；首包前客户端尚未观察到结果，可以安全重订阅，首包后则优先保证用户看到的内容不重复、不倒退。
- **放弃的代价**：任意时点重试会从头生成并造成重复或语义漂移；完全禁止会让首包前的短暂 429/503 失去低风险恢复机会。

### 决策 6：失败请求计数一次，但不写成功缓存和正常历史
- **面临的选择**：失败也照常写 cache/history；失败完全不计数也不落任何业务写入；query count 计一次，但失败或部分答案不写 cache/history。
- **选了哪个 + 为什么**：用户已在事前闸门确认，选择第三种；用户请求确实进入了问答入口，计数一次保持流量事实，但当前 cache/history 都表达“可复用或完整答案”，不应写入失败和半截内容。
- **放弃的代价**：照常保存会把错误提示或部分答案伪装成成功记录并可能被缓存复用；完全不计数会低估真实请求量和 provider 故障对用户的影响。

### 决策 7：诊断依赖稳定类别和结构化字段，不依赖原始异常文本
- **面临的选择**：把 provider message/body 原样返回；只给完全不透明的通用错误；返回允许清单内的结构化字段并由稳定 category 映射用户提示。
- **选了哪个 + 为什么**：用户已在事前闸门确认，选择结构化安全诊断；它兼顾排障和评测归因，同时守住 secret、prompt、context 与 provider body 不外泄的 accepted 安全边界。
- **放弃的代价**：原样透传可能泄露认证信息或用户内容，也会让客户端依赖供应商文案；完全不透明则无法区分限流、超时、网络和 provider 5xx，故障报告失去可操作性。

### 决策 8：同步 deadline、流式 inactivity、SSE transport timeout 分开定义
- **面临的选择**：所有路径共用一个总超时；同步使用完整响应 deadline、流式使用相邻数据 inactivity deadline；只依赖 `SseEmitter` 的 120 秒 transport timeout。
- **选了哪个 + 为什么**：用户已在事前闸门确认，选择按调用形态拆分；同步要等待完整 body，流式长连接则需要允许持续输出，而 emitter timeout 只负责客户端传输生命周期，三者职责不同。
- **放弃的代价**：统一总超时可能截断仍在正常输出的长回答；只依赖 emitter 会让 provider 首包或中途卡死长时间占用连接，并模糊 provider 与 transport 故障归因。

### 决策 9：用 JDK 本地 HTTP server 做故障注入
- **面临的选择**：调用真实 provider 制造 429/503；新增 WireMock/MockWebServer/Resilience4j 等依赖；复用 JDK `HttpServer` 在随机本地端口返回合成故障。
- **选了哪个 + 为什么**：用户已在事前闸门确认，选择 JDK `HttpServer`；仓库已有同类测试先例，它能确定性控制状态码、延迟、body 和订阅次数，且不新增依赖、不产生业务数据出站或费用。
- **放弃的代价**：真实 provider 故障不可重复且涉及凭据、限流和费用授权；引入新测试/韧性框架会扩大依赖面，并把本 change 从行为收口变成框架选型。

### 决策 10：不做跨 provider 自动切换
- **面临的选择**：OpenAI-compatible 失败后自动切 Qwen；按健康状态动态路由多个 provider；requested provider 失败后按本 provider 契约稳定结束。
- **选了哪个 + 为什么**：用户已在事前闸门确认，选择不自动切换；不同 provider 的模型、数据出站、费用和答案行为不同，静默切换会破坏 attribution，也超出单 provider 故障语义的职责边界。
- **放弃的代价**：自动切换或健康路由虽然可能提高可用性，却会引入凭据治理、模型等价性、费用授权和观测归因的新契约，失败时还难以证明最终实际用了哪个 provider。

### 决策 11：本 change 不引入熔断器或精确 `Retry-After` 调度
- **面临的选择**：同时加入熔断、半开探测和 `Retry-After` 精确调度；只实现现有 exponential backoff 下的有界重试；完全不保留后续扩展点。
- **选了哪个 + 为什么**：用户已在事前闸门确认，选择只锁定有界重试，并把熔断与 provider-specific 调度留给后续独立 change；C4b 当前要先解决可观察失败、重复流和副作用，避免一次引入新的全局状态机。
- **放弃的代价**：本轮全做会新增跨请求状态、时钟测试和 provider header 兼容复杂度，显著放大回滚面；完全封死扩展则会阻碍以后在真实流量证据下补熔断或调度。

### 决策 12：本 change 不升级结构化 SSE 或前端协议
- **面临的选择**：同步完成 C4b 的同时设计结构化 error/done 事件并改前端；继续沿用 `[ERROR]` + `[DONE]` 文本终态；取消流式错误提示只直接断开。
- **选了哪个 + 为什么**：用户已在事前闸门确认，选择沿用现有文本终态；它能先修复 provider 故障和重放风险，又不把独立的 SSE 结构化结果技术债和前端兼容迁入 C4b。
- **放弃的代价**：同步升级结构化协议会扩大到 DTO、前端和历史保存，并增加本 change 的审查面；直接断开则让用户无法区分正常结束、provider 故障和客户端网络问题。
