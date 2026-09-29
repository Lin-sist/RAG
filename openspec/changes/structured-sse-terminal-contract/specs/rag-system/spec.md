## ADDED Requirements

### Requirement: Versioned Structured SSE Terminal

系统 SHALL 在现有 `POST /api/qa/ask/stream` 提供显式版本协商的结构化 SSE 契约，同时使未选择新版本的客户端继续接收既有纯文本格式。未知版本 MUST 在检索或 generation 前拒绝。新契约 SHALL 输出文本增量与最多一个具名 terminal event；terminal SHALL 包含协议版本、`ANSWER / NO_ANSWER / UNSUPPORTED / ERROR / CANCELLED` 可区分的 final state，并保留 C16 已有的 `INVALID` 无效输入状态，以及稳定 reason、该次有效 citations、必要安全 metadata、classifier/strategy/policy identity、budget outcome 和实际 usage。无法观测的 usage MUST 显式标记不可用，不得伪造为零。未收到 terminal 的客户端 MUST 将传输视为 incomplete，不得推断业务成功。

新版本通过 `X-RAG-Stream-Contract: structured-v1` 精确协商，缺失/空白值维持 legacy；其他值 SHALL 在 query count、retrieval 与 generation 前以 `UNSUPPORTED_STREAM_CONTRACT`/HTTP 400 拒绝。新版本使用具名 `text` 与 `terminal` event，不发送 `[DONE]`/`[ERROR]` 文本标记。terminal JSON 的字段为 `schemaVersion/finalState/reason/citations/metadata/classifierVersion/effectiveStrategy/policyVersion/routeReason/budgetOutcome/usage`。Router 关闭的 identity 为 `legacy`，`routeReason=LEGACY`；无法观测的 budget outcome/usage 为 JSON null。usage 中的估计 token 字段 MUST 保留 estimated 命名，不得声明为 provider 实耗 token。metadata MUST 使用安全白名单，不得包含 provider body、原始异常、凭据或任意检索上下文。

#### Scenario: 旧客户端兼容
- GIVEN 客户端未请求结构化版本
- WHEN 发起流式问答
- THEN 继续接收既有文本、`[ERROR]` 和 `[DONE]` 传输格式
- AND 不接收会被当成回答文本的 terminal JSON

#### Scenario: 完整回答
- GIVEN 单次流执行获得完整且通过现有 citation 校验的回答
- WHEN 结构化客户端持续连接到终态
- THEN 收到唯一 `ANSWER` terminal，包含该次实际有效 citations 与 usage
- AND 不通过另一次同步 ask 补齐数据

#### Scenario: 无答案、不支持与错误
- GIVEN 检索无证据、Router 不支持、或检索/generation 失败
- WHEN 结构化流达到可交付终态
- THEN 分别报告 `NO_ANSWER`、`UNSUPPORTED` 或 `ERROR` 和稳定 reason
- AND 不把已发送的部分文本或 `[DONE]` 视为 `ANSWER`

#### Scenario: 断连和超时
- GIVEN 客户端断开、服务端取消订阅、或流超时
- WHEN terminal 无法可靠送达
- THEN 服务端记录实际终态及 usage，客户端仅能判定 incomplete
- AND 不声称客户端已收到 `CANCELLED` 或 provider 一定未产生费用

### Requirement: Stream Final Result And History Integrity

系统 SHALL 从一次流执行形成最终状态、有效 citations、路由与预算事实，并用该结果决定 SSE terminal 与正常成功历史。只有完整 `ANSWER` 且满足交付条件才可保存正常 QA history，并保存同一次执行的有效 citations；`NO_ANSWER / UNSUPPORTED / ERROR / CANCELLED`、timeout、部分输出、断连或发送失败 MUST NOT 保存为正常成功历史。已接受请求的 query count 至多增加一次，retry 不得重复计数；同步、SSE 与 MCP 的 final state/reason 语义 SHALL 对齐，同时保留 MCP 无 history/query-count 副作用的边界。

#### Scenario: 最后一块之后断连
- GIVEN 最后一块文本已生成但 terminal 尚未送达
- WHEN 客户端断开或发送失败
- THEN 不将该次部分传输保存为正常成功历史
- AND 服务端取消或结束上游并记录可观测的实际 usage

#### Scenario: 完整交付后保存
- GIVEN 一次流执行得到 `ANSWER`、有效 citations，且完成协议所要求的交付条件
- WHEN 保存 QA history
- THEN 只保存一次，并包含本次回答与有效 citations
- AND query count 对该请求至多增加一次

#### Scenario: 三入口语义一致
- GIVEN 同一租户、知识库、问题、配置及确定性 provider
- WHEN 同步、SSE 和 MCP read-only ask 分别执行
- THEN final state、reason、strategy/policy 与可比 usage 语义一致
- AND MCP 不写 QA history 或 query count
