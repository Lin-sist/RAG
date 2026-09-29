## ADDED Requirements

### Requirement: Frontend Text-Only SSE Presentation

正式聊天页 SHALL 保持同步问答为默认，并 MAY 显式使用现有 `POST /api/qa/ask/stream`。流式 UI MUST 只展示收到的文本和本地 transport 结果，不得从文本推断结构化业务 final state 或引用来源，不得为了补来源自动重发同步问答。

#### Scenario: 纯文本流完成
- GIVEN 用户选择纯文本流并发送问题
- WHEN 流返回文本且以 `[DONE]` 结束且无已知错误
- THEN UI 展示文本并标明纯文本流已结束
- AND 不展示伪造的 citations 或业务成功状态

#### Scenario: 流内错误及分帧
- GIVEN `data:`、`[ERROR]` 和 `[DONE]` 可被任意网络帧切开
- WHEN `useSSE` 解析事件
- THEN 文本保留前导空格与 UTF-8 字符，`[ERROR]` 不作为答案显示
- AND 即使其后有 `[DONE]` 仍标记流错误

#### Scenario: 客户端停止接收
- GIVEN 浏览器使用 AbortController 中断流
- WHEN 已收到部分文本或尚未收到文本
- THEN UI 标记客户端已停止接收
- AND 不宣称服务端已取消或未保存 history
