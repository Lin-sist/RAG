# RAG System Spec Delta: C4b LLM Provider Resilience

## ADDED Requirements

### Requirement: LLM Provider 有界重试与故障分类

系统 SHALL 将 LLM provider 的 429、5xx、请求 timeout、I/O 与连接失败分类为 transient failure，并且 MAY 仅在显式配置的非负 `max-retries` 预算内重试。`max-retries=N` 时单次同步问答或流式首个用户可见内容之前的 provider 总尝试数 MUST NOT 超过 `1+N`；`max-retries=0` MUST 只执行一次 provider 尝试。系统 MUST NOT 无限重试。

非重试型 4xx、配置错误以及 2xx malformed/empty response MUST NOT 自动重试。流式响应一旦已经输出至少一个用户可见内容 chunk，后续任何 provider failure MUST NOT 触发重新订阅或从头重放。

#### Scenario: 429 在显式预算内恢复

- GIVEN `max-retries=1`
- AND provider 第一次返回 429、第二次返回合法响应
- WHEN 发起同步问答或流式请求尚未输出用户可见内容
- THEN 系统总共执行 2 次 provider 尝试
- AND 返回第二次尝试的成功结果
- AND query count 只增加一次

#### Scenario: 503 重试耗尽

- GIVEN `max-retries=2`
- AND provider 每次均返回 503
- WHEN 发起问答
- THEN provider 总尝试数为 3
- AND 最终错误类别为 `provider_5xx`
- AND diagnostics 表明 `retryCount=2` 与 `retryExhausted=true`
- AND 系统不再追加第 4 次尝试

#### Scenario: 默认零重试

- GIVEN `max-retries=0`
- WHEN provider 返回 429、5xx、timeout 或 network failure
- THEN 系统只执行 1 次 provider 尝试
- AND 直接进入稳定 generation failure 语义

#### Scenario: 非重试型响应

- WHEN provider 返回 400、401、403、404 或合法 HTTP 2xx 但 body malformed/empty
- THEN 系统不自动重试
- AND 4xx 分类为 `provider_http_error`
- AND malformed/empty response 分类为 `invalid_response`

#### Scenario: 流式首个内容后失败

- GIVEN 流式 provider 已输出至少一个非空内容 chunk
- WHEN 后续发生 429、5xx、timeout、network 或解析失败
- THEN 系统不得重新订阅 provider
- AND 已输出内容不得从头重复
- AND 当前流进入稳定失败终态

### Requirement: LLM Generation 失败响应与副作用

LLM generation 最终失败时，系统 SHALL 返回稳定、机器可判定且不泄露 provider 或用户私密内容的失败结果。同步 `/api/qa/ask` SHALL 保持现有 HTTP 200 外层兼容，并在 `QAResponse.metadata.status` 中返回 `error`；answer MUST 仅包含稳定用户提示，citations 与 contexts MUST 为空。

失败 diagnostics MAY 包含 provider、固定 endpoint path、model、timeout、maxRetries、attemptCount、retryCount、retryExhausted、稳定 errorCategory 与可选 HTTP status，但 MUST NOT 包含 API key、Authorization header、provider response body、原始异常 message、question、prompt、context 或 snippet。

一次被接受的问答请求 SHALL 只增加一次 query count，不因 provider retry 重复计数。同步 generation failure、流式 failure 或部分输出后 failure MUST NOT 写入成功 QA cache，也 MUST NOT 保存为正常 QA history。

#### Scenario: 同步 generation 失败

- GIVEN retrieval 已返回 contexts
- WHEN LLM transient failure 在配置预算内仍未恢复
- THEN `/api/qa/ask` 返回 HTTP 200 外层兼容响应
- AND `metadata.status=error`
- AND answer 为稳定的暂不可用提示
- AND citations 与 contexts 为空
- AND QA cache 与 QA history 均不写入
- AND query count 只增加一次

#### Scenario: 流式首个内容前失败

- GIVEN 流式请求尚未输出用户可见内容
- WHEN provider failure 最终无法恢复
- THEN SSE 输出稳定 `[ERROR]` 提示后输出 `[DONE]` 并结束
- AND 不保存 QA history
- AND 错误 chunk 不包含 provider body、原始异常 message、prompt 或 context

#### Scenario: 流式部分输出后失败

- GIVEN 流式请求已输出部分答案
- WHEN provider 随后失败
- THEN 系统保留已发送内容但不得重放
- AND 追加稳定 `[ERROR]` 与 `[DONE]` 后结束
- AND 部分答案不得保存为正常 QA history

#### Scenario: 安全故障诊断

- GIVEN provider 错误 body 或异常链包含合成 secret、认证 header、prompt 或 context marker
- WHEN 系统构造客户端响应、diagnostics 和普通日志
- THEN 可观察输出只包含允许的稳定字段与错误类别
- AND 不包含上述敏感 marker 或原始 provider 内容
