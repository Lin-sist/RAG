# C21 设计：后端契约与验证

## 协议草案

- 同一 `POST /api/qa/ask/stream` 默认保持 legacy 文本格式；客户端显式请求 `X-RAG-Stream-Contract: structured-v1` 才接收具名 `text` 增量与唯一具名 `terminal` event。未知版本在问答执行前拒绝。
- terminal 包含协议版本、`finalState`、稳定 `reason`、`citations[]`、必要安全 metadata、classifier/strategy/policy identity、budget outcome 与实际 usage。缺失观测显式标记不可用，不伪填零。新协议不以 `[DONE]` 表示业务成功。
- `ANSWER / NO_ANSWER / UNSUPPORTED / ERROR / CANCELLED` 可区分；服务端 timeout 归入 `ERROR` 并带稳定 timeout reason。网络断流而无 terminal 时，客户端只判定 incomplete。连接已断时不能保证送达 `CANCELLED`。
- 字段 nullability、Router-off identity 与稳定 reason 已由第二切片锁定并同步更新 spec delta；第三切片用可控 emitter 验证最后一块后断连的服务端处理顺序。

## 已锁定的 wire v1 字段（第二切片）

- 请求头 `X-RAG-Stream-Contract` 缺失或空白时使用 legacy；精确值 `structured-v1` 启用新格式；其他非空值在 query count、retrieval 和 generation 前以 `UNSUPPORTED_STREAM_CONTRACT`/HTTP 400 拒绝。
- 新格式的增量事件名为 `text`，其 `data` 是原始文本片段；最后最多一个 `terminal`，其 `data` 是 JSON。新格式不发送 `[DONE]` 或 `[ERROR]` 文本标记。终态无法发送时，客户端只能看到 incomplete。
- terminal JSON 字段固定为 `schemaVersion`, `finalState`, `reason`, `citations`, `metadata`, `classifierVersion`, `effectiveStrategy`, `policyVersion`, `routeReason`, `budgetOutcome`, `usage`。`schemaVersion=structured-v1`。`citations` 在非 `ANSWER` 和错误时为空数组；`metadata` 只允许 citation 计数/coverage 与明确标名的 estimated token 字段。
- Router 关闭时三项 identity 为 `legacy`，`routeReason=LEGACY`。缺少预算账本时 `usage` 和 `budgetOutcome` 为 JSON null；不得写零。`usage` 有值时沿用 `QueryBudgetUsage` 的字段名，其中 token 值是估计值，不是 provider 返回的实耗 token。
- `reason` 对完整回答为 `NONE`；无证据或拒答使用现有 `NoAnswerReason`；不支持或无效输入使用既有 route reason，后者保持 C16 的 `INVALID/INVALID_INPUT`，不压成 `UNSUPPORTED`。流异常为 `STREAM_FAILED`，已识别超时为 `TIMEOUT`，核心结果缺失为 `RESULT_UNAVAILABLE`。这些错误码不包含异常原文。`CANCELLED` 仅保留为服务端可确认且连接仍可发送时的状态；当前断连不发送该 event。

## 单次执行和副作用

当前 `Flux<String>` 不能交付校验后的引用与最终状态。核心层应形成一次流执行的结构化结果，供文本发送、terminal 和历史保存使用；不得为补引用再调用一次 `/ask`。只有完整 `ANSWER` 且满足交付条件才保存正常 QA history，引用来自同次执行。`NO_ANSWER / UNSUPPORTED / ERROR / CANCELLED`、timeout、部分输出、断连或发送失败不写正常成功历史。已接受请求的 query count 至多增加一次；MCP 仍不写 history/query count。

第二切片已将 Controller 的保存条件收紧为：核心结果为完整 `ANSWER`，且 SseEmitter 发送 terminal（legacy 为 `[DONE]`）未抛错、未观测到连接关闭。保存的答案与 citations 来自同一次结果。发送调用成功仍不等于客户端应用确认收到；第三切片已测试最后一块后的断连与发送失败。

第三切片已将完成、超时、错误回调注册移到订阅之前，使同步完成或很快关闭的流也能及时标记交付关闭并取消上游。可控 emitter 测试在最后一块文本发送期间触发断连，以及在 terminal 发送时抛出 I/O 错误，均不得保存成功历史；超时测试确认取消上游并标记 timeout。上述测试只证明服务端已观察到的关闭/发送失败，不等于网络层客户端确认送达。

收尾验证进一步覆盖 text 发送失败、认证租户作用域、拒绝访问时零 query count，以及流式执行即使请求 `enableCache=true` 也不读写 QA cache 的现有行为。同步 Controller 只在 `ANSWER` 时写正常历史；MCP 继续复用同一个核心同步结果，`UNSUPPORTED/INVALID` 映射到现有 `no_result` 状态并通过 `errorCategory` 区分，不把它们包装成 `ok`。MCP 不写 history/query count，原有 diagnostics 白名单保持不变。

## 验证矩阵

| 场景 | 结果与验证重点 | 正常成功历史 |
| --- | --- | --- |
| 完整回答 | `ANSWER`、有效引用、实际 usage、唯一 terminal | 一次，含同次引用 |
| 无证据/拒答 | `NO_ANSWER`、稳定 reason、空引用 | 不写 |
| Router 不支持 | `UNSUPPORTED`、无 generation | 不写 |
| 检索/生成/发送错误 | `ERROR` 或 terminal 不可送达；保留实际 usage | 不写 |
| timeout/取消/断连 | 服务端真实终态；客户端无 terminal 则 incomplete | 不写 |
| legacy 客户端 | 旧文本和标记兼容，服务端历史条件修正 | 仅完整成功 |

聚焦测试覆盖协商、SSE 分帧、唯一终态、增量后错误、断流、取消/timeout 竞态、citation 租户边界、history/query-count/cache，以及确定性条件下同步/SSE/MCP 语义等价。Java 风险验证按项目规则执行；真实 provider 联调另获授权。

## 决策记录

### 决策 1：旧客户端如何兼容
- **面临的选择**：默认流直接插入 JSON terminal；新建第二端点；同端点显式版本协商。
- **选了哪个 + 为什么**：同端点显式协商，旧解析器不会把 terminal JSON 当答案，同时只维护一条问答入口。
- **放弃的代价**：直接插入污染旧 UI；第二端点复制鉴权、限流和流执行维护面。

### 决策 2：终态从哪里产生
- **面临的选择**：Controller 按文本和 `[DONE]` 推断；补一次同步 ask；核心单次执行形成结构化结果。
- **选了哪个 + 为什么**：单次执行结果使引用、路由、预算和 usage 对应本次流。
- **放弃的代价**：推断会把拒答当成功；补 ask 增加 provider 调用并可能生成不同答案和历史。

### 决策 3：如何解释客户端中断
- **面临的选择**：把浏览器 abort 当作已送达 `CANCELLED`；分别记录服务端取消和客户端是否收到 terminal。
- **选了哪个 + 为什么**：分别记录，已断连接无法可靠送事件，服务端副作用仍需验证。
- **放弃的代价**：直接视作已取消会误报 provider 停止和历史未写。

### 决策 4：为何将前端、C19、C20 留给后续切片
- **面临的选择**：一个 change 同时改后端、前端和门禁；后端契约验收后顺序推进独立切片。
- **选了哪个 + 为什么**：独立切片使前端以已接受 wire 为依据，judge 外调和门禁阈值保留各自证据与授权边界。
- **放弃的代价**：合并推进使协议缺陷、联调副作用和门禁失败难以归因，并可能扩散一次性外调授权。

### 决策 5：首切片如何传递流执行结果
- **面临的选择**：立即把 `Flux<String>` 全部改成新事件类型；沿用已有 Reactor context 中的 `StreamTerminalSignal` 传递同次执行结果。
- **选了哪个 + 为什么**：先沿用已有 signal，保持当前 Controller 文本消费接口稳定，让引用和预算从核心执行传出供下一切片使用。
- **放弃的代价**：立刻改流类型会同时改动 Controller、测试和 wire，首切片难以单独验证；沿用 signal 的代价是下一切片必须明确传输完成与结果快照的竞态。

### 决策 6：legacy 流没有预算账本时如何表示 usage
- **面临的选择**：根据文本长度或预设配置推算调用量；保持预算 usage 为不可用，并仅保存已有 finalization metadata。
- **选了哪个 + 为什么**：保持不可用，legacy 路径没有与 Router 相同的预算账本，推算值不能充当实际观测。
- **放弃的代价**：推算会把估计误报成真实 provider usage，影响后续 terminal 和评测判断。

### 决策 7：同步入口如何判定可保存的历史
- **面临的选择**：沿用 `QAResponse.isSuccess()`；全局修改其含义；在 Controller 的 history 边界检查业务 final state。
- **选了哪个 + 为什么**：在 Controller 检查 `ANSWER`，使 `NO_ANSWER/UNSUPPORTED/INVALID` 不写正常历史，同时保留其他既有调用对 `isSuccess()` 的解释。
- **放弃的代价**：只用 `isSuccess()` 会保存拒答和不支持提示；全局改动会改变其他消费方的行为，需要扩大本 change 的回归面。

### 决策 8：MCP 如何表达不支持与无效输入
- **面临的选择**：保留 `status=ok`；扩展 MCP output schema；复用现有 `no_result` 和 `errorCategory`。
- **选了哪个 + 为什么**：复用现有字段，避免把没有业务答案的结果标成 `ok`，也不改变 C15 MCP output schema 和 diagnostics 白名单。
- **放弃的代价**：`ok` 会误导调用方；扩展 schema 会增加 C15 契约与互操作验证范围。
