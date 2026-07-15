# Proposal: C4b LLM Provider Resilience

## Why

C3 已用隔离基础设施验证登录、上传、索引、retrieval 与删除 happy-path，但 generation 路径在 LLM provider 抖动时仍缺少被长期规格和故障注入测试锁定的行为。当前 `AnswerGeneratorImpl` 已包含 timeout、429/5xx 重试判断和诊断字段，`RAGServiceImpl` 也会把异常转换为 `QAResponse.error`；然而这些能力仍是 `partial`：默认 `max-retries=0`，同步与流式路径没有真实 HTTP 故障测试，流式响应在已经输出 chunk 后仍可能整段重订阅并产生重复内容，失败问答还可能被当作正常历史保存。

如果不先把 C4b 契约锁定，后续 Redis/Milvus 故障语义和可观测性工作会继续面对“请求是否算成功、哪些故障可重试、失败是否写历史、客户端看到什么”的平行答案。

## 用户故事（大白话：改前坏事 → 改后不同）

改之前，模型服务遇到 429、503 或超时时，同一个问题可能直接失败、重试后重复输出一段内容，或者把错误提示保存成一条看似正常的问答历史；改之后，系统会按明确的预算决定是否重试，流式输出一旦开始就不再从头重放，失败只返回稳定且不泄露 provider 内容的提示，并且不会把失败或半截答案当成成功历史。

## Current Status

- `confirmed`：C3 已完成并归档，`main` 与 `origin/main` 一致，启动本 change 前工作区干净，C4b 顺序前置满足。
- `confirmed`：同步 OpenAI/Qwen 调用均使用 WebClient timeout；现有 retry filter 把 429、5xx、timeout、I/O 和连接错误视为可重试。
- `confirmed`：同步失败可向 `QAResponse.metadata` 透传 provider、endpoint、model、timeout、maxRetries、errorCategory 与可选 httpStatus。
- `partial`：tracked runtime 默认 `RAG_LLM_MAX_RETRIES=0`，现有重试逻辑默认不启用；显式启用后的调用上限与副作用未形成长期契约。
- `partial`：同步失败当前仍以 HTTP 200 + `metadata.status=error` 返回，查询次数与失败历史仍会写入；该兼容行为尚无完整 controller 测试。
- `partial`：流式路径对整个 Flux 使用 retry；首个可见 chunk 后失败时可能从头重新订阅并重复输出，且错误路径会保存部分历史。
- `planned`：建立本地 HTTP 故障服务，覆盖 429、503、timeout、非重试型 4xx、重试耗尽和流式首 chunk 前后失败。
- `out_of_scope`：Redis/Milvus 故障语义（C4c/C4d）、索引恢复（C5）、真实 provider 收益评测、跨 provider 自动切换、熔断器、结构化 SSE 完成事件与前端 UI 重做。
- `unknown`：不同真实 provider 对 `Retry-After`、流式心跳和错误 body 的具体实现差异；C4b 只接受协议无关的安全下界，不以单一 provider 特例定义通用契约。

## Scope

- 把公共故障契约矩阵模板应用到 LLM provider：故障类别、重试资格、尝试上限、同步/SSE 结果、副作用、诊断与验证证据。
- 对 429、5xx、timeout、I/O/连接错误提供可配置的有界重试；`max-retries=N` 表示最多 `1+N` 次 provider 尝试。
- 保持 tracked 默认 `max-retries=0`，避免未经运维显式选择就放大真实 provider 调用、限流和费用。
- 非重试型 4xx、2xx malformed response、配置/解析错误不得自动重试。
- 流式响应只允许在首个用户可见内容 chunk 之前重试；一旦已有内容输出，后续故障必须终止当前流，不得从头重放。
- 同步问答保持现有 HTTP 200 外层兼容，使用 `metadata.status=error` 和稳定诊断表达 generation 失败；不伪造答案或 citations。
- 问答请求只增加一次查询计数；generation 失败不得写成功缓存，不保存同步失败历史，也不保存流式失败或部分答案历史。
- 诊断与日志只记录稳定类别和安全字段，不回显 provider body、异常原始消息、prompt/context、API key 或 Authorization header。
- 使用 JVM 本地 HTTP server 和 mock service 进行故障注入，不连接真实 LLM provider。

## Non-goals

- 不自动从 OpenAI-compatible provider 切换到 Qwen，反之亦然；requested/effective provider 保持一致。
- 不默认开启重试，不改变 tracked `RAG_LLM_MAX_RETRIES:0`。
- 不新增 Resilience4j、WireMock、MockWebServer 等依赖；优先复用 JDK `HttpServer`、Reactor 与现有测试依赖。
- 不修改 retrieval、rerank、prompt、citation、no-answer 或 judge 指标口径。
- 不修改数据库 schema、认证、权限、租户、文档索引状态机或 vector store。
- 不把文本型 SSE 协议升级为结构化事件；该能力仍属于独立 SSE 技术债。
- 不通过真实 provider smoke 宣称 generation 质量或业务收益。
- 本规格阶段不修改生产 Java、配置、测试或评测脚本。

## External Calls And Authorization

本 change 的规格、实现和验证均不产生业务 provider 外调：

| 调用类型 | 规格/实现验证调用量 | 数据出站 | 模型 | 限流风险 | 费用与零费用依据 | 授权状态 |
|---|---:|---|---|---|---|---|
| embedding | 0 | 无 | 无 | 无 | 不发生调用，因此费用为 0 | 不适用 |
| rerank | 0 | 无 | 无 | 无 | 不发生调用，因此费用为 0 | 不适用 |
| judge | 0 | 无 | 无 | 无 | 不发生调用，因此费用为 0 | 不适用 |
| ask/LLM | 0 | 无；仅向 `127.0.0.1` 合成服务发送合成 prompt | 无真实模型 | 无 provider 限流 | 不发生真实调用，因此费用为 0；不依赖 NVIDIA 免费假设 | 不适用 |

部署后的理论调用放大边界：tracked 默认 `max-retries=0` 时每个 ask 最多 1 次 provider 尝试；运维显式配置 `N>0` 时，同步请求或流式首 chunk 前最多 `1+N` 次。流式首 chunk 后、非重试型 4xx 和 invalid response 均不追加尝试。任何后续真实批量 ask、provider smoke 或评测仍须另行说明调用量、数据出站、模型、限流和费用并取得授权。

## Acceptance Evidence

- 用户先审阅并明确批准本 proposal、design 与 spec delta，之后才允许修改业务代码。
- 本地 HTTP 故障测试证明：429/503/timeout 在显式预算内重试，非重试型 4xx 和 malformed 2xx 不重试。
- `max-retries=0` 时所有故障只产生 1 次本地 provider 尝试；`max-retries=N` 时总尝试不超过 `1+N`。
- 流式首 chunk 前可重试；首 chunk 后故障不重订阅、不重复已输出内容，并以稳定 `[ERROR]` + `[DONE]` 终止。
- 同步失败保持 HTTP 200 外层兼容但 `metadata.status=error`；citations/contexts 为空，不写成功缓存或历史，查询次数只增加一次。
- 失败响应、日志和诊断不包含合成 API key、Authorization header、provider body、prompt/context 或异常原始消息。
- 聚焦 `rag-core`、`rag-admin` 测试，完整 `mvn -q test`、Python unittest、SensitiveLogs 与 `git diff --check` 通过。
- 实际 embedding/rerank/judge/ask/LLM 业务调用量均为 0。

## Risks

- 保留 HTTP 200 外层兼容可能继续让只看 HTTP status 的客户端误判；本 change 通过稳定的 `metadata.status=error` 和测试锁定现有兼容语义，若未来改为 503 应另起 API 契约 change。
- 流式首 chunk 判断若放置错误，仍可能发生重复输出或误禁首包前重试；必须使用可观察订阅次数和完整 chunk 序列测试。
- 异常 cause/message 可能携带 provider body；实现不得把原始 message 拼入客户端错误或普通日志。
- 显式启用重试会放大单次用户请求的 provider 调用、延迟与费用；因此默认保持 0，并把 `1+N` 上限写入契约和诊断。
- 当前 QA history 无“不完整/失败”状态字段；C4b 选择不保存失败历史，而不是把半截答案伪装成成功记录。

## Commit Responsibility

`用户手动提交`。Agent 不执行 `git add`、`git commit`、push、PR、发布或部署；每个可验证切片结束后只提供中文 Conventional Commit 建议。
