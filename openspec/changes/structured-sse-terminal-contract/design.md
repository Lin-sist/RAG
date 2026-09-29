# C21 设计：后端契约与验证

## 协议草案

- 同一 `POST /api/qa/ask/stream` 默认保持 legacy 文本格式；客户端显式请求 `X-RAG-Stream-Contract: structured-v1` 才接收具名 `text` 增量与唯一具名 `terminal` event。未知版本在问答执行前拒绝。
- terminal 包含协议版本、`finalState`、稳定 `reason`、`citations[]`、必要安全 metadata、classifier/strategy/policy identity、budget outcome 与实际 usage。缺失观测显式标记不可用，不伪填零。新协议不以 `[DONE]` 表示业务成功。
- `ANSWER / NO_ANSWER / UNSUPPORTED / ERROR / CANCELLED` 可区分；服务端 timeout 归入 `ERROR` 并带稳定 timeout reason。网络断流而无 terminal 时，客户端只判定 incomplete。连接已断时不能保证送达 `CANCELLED`。
- 字段 nullability、Router-off identity、稳定 reason 枚举及最后一块后断连时的线性化次序，在首个实现切片前以契约测试锁定并同步更新 spec delta。

## 单次执行和副作用

当前 `Flux<String>` 不能交付校验后的引用与最终状态。核心层应形成一次流执行的结构化结果，供文本发送、terminal 和历史保存使用；不得为补引用再调用一次 `/ask`。只有完整 `ANSWER` 且满足交付条件才保存正常 QA history，引用来自同次执行。`NO_ANSWER / UNSUPPORTED / ERROR / CANCELLED`、timeout、部分输出、断连或发送失败不写正常成功历史。已接受请求的 query count 至多增加一次；MCP 仍不写 history/query count。

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
