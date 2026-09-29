# C21：结构化 SSE 终态契约

## 目标

在保留现有纯文本 SSE 客户端行为的同时，为显式选择新契约的客户端提供可验证的最终业务结果：状态、原因、有效 citations、必要 metadata、路由与预算归因及实际 usage。流式历史仅记录完整且符合成功条件的回答。

## 当前事实与范围

- R4 已验收：正式 `/chat` 默认同步问答，可显式使用纯文本流；前端只确认文本和本地传输状态。
- Controller 订阅 `Flux<String>`，发送文本及 `[DONE]`/`[ERROR]`；正常完成回调直接保存文本，citations 固定为空。核心层 `StreamTerminalSignal` 只有部分路由和预算字段，尚非完整响应。
- C16 baseline 要求同步、SSE、MCP final state 语义一致，非 `ANSWER` 不写正常成功历史。C21 需修正并验证当前流式路径。
- 定义版本化协商、文本事件、唯一结构化终态与断流判定；一次执行形成最终状态和有效引用，约束 history/cache/query-count；用确定性测试覆盖 legacy、Router enabled/disabled、租户和故障边界。

## 边界与后续顺序

本 change 先推进 C21 后端契约与验证。后端契约验收后，前端 R5 接入并联调；随后独立推进 C19 live judge 校准，再基于 C18/C19 证据建立 C20 objective/judge 门禁。不扩展 Router 策略或改变检索、分块、rerank、prompt、citation、no-answer、provider 默认行为；不引入 Agent task runtime。C21 不以 C19/C20 为前置，也不证明生成质量或 judge 达标。

本地确定性验证不调用真实 provider。真实 SSE 联调需另行披露模型、次数、数据出站、费用、限流和副作用并获授权。客户端断开时不能承诺其已收到 `CANCELLED` event，应验证服务端实际终态和历史结果。
