### Requirement: Frontend Structured SSE Presentation

正式聊天页 SHALL 显式以 `X-RAG-Stream-Contract: structured-v1` 请求结构化流，且仍以同步问答为默认。前端 SHALL 只将具名 `text` 事件加入回答，并仅在收到唯一、有效的 `terminal` 后展示其 `finalState`、稳定 `reason` 与本次有效 citations。只有 `ANSWER` 可展示 citations 并通知历史刷新；非答案、无 terminal、损坏/重复 terminal、终态后文本、未知事件、读流失败或客户端中断 MUST NOT 被呈现为完整回答。客户端中断 MUST NOT 被解释为服务端已取消、未保存历史或没有 provider 费用。

#### Scenario: 完整回答
- GIVEN 用户显式选择结构化流
- WHEN 收到分帧文本和唯一有效 `ANSWER` terminal
- THEN 页面展示本次文本与 terminal 内有效 citations，并通知历史列表刷新
- AND 不额外调用同步 ask 补来源

#### Scenario: 非答案终态
- GIVEN 已收到部分文本
- WHEN terminal 为 `NO_ANSWER / UNSUPPORTED / INVALID / ERROR / CANCELLED`
- THEN 页面展示终态与 reason，来源为空，且不通知历史刷新

#### Scenario: 断流或协议异常
- GIVEN 未收到 terminal、terminal 无效或重复、或终态后又收到文本
- WHEN 流结束或用户停止接收
- THEN 页面标记 incomplete 或本地停止接收，保留已收到的部分文本
- AND 不宣称业务成功或服务端已取消
