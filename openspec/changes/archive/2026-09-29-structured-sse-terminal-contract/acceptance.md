# C21 后端契约验收记录

## 结论

`structured-sse-terminal-contract` 在本地确定性后端范围内接受。`structured-v1` 显式协商得到具名 `text` 和唯一 `terminal` SSE event；legacy wire 保持原样。终态来自同一次核心执行，包含有效 citations 与可观测 usage；缺少预算账本时 usage 保持 null。无 terminal 的客户端只可判定 incomplete。

## 验证证据

- 聚焦测试覆盖 `RAGServiceImplTest` 26、`QAControllerTest` 28、`StreamTerminalEventTest` 3、`C16McpRouterIntegrationTest` 1、`QueryEngineTelemetryTest` 2，合计 60 tests，零失败。
- `mvn -q test` 在本次代码状态下 exit 0；本次生成的 Surefire 报告合计 672 tests、0 failures、0 errors、21 skipped。原 `QueryEngineTelemetryTest` 两处过期 embedding mock 已按实际 `embedObserved` 调用修复。
- MockMvc 与可控 emitter 验证协商、legacy 兼容、唯一 terminal、无答案、部分输出后错误、timeout、最后一块后断连、text/terminal 发送失败及正常 history 条件。租户测试验证认证身份的 vector scope、query count 和 history 归属；拒绝访问时不计数也不检索。
- 同步非 `ANSWER` 不写正常历史；MCP `UNSUPPORTED/INVALID` 不再映射为 `ok`，沿用 `no_result` 加 `errorCategory`，保持 MCP 无 history/query-count 副作用。流式路径目前不读写 QA cache，即使请求 `enableCache=true`。
- `openspec/specs/rag-system/spec.md` 已接受本 change 的两项 requirement。验收测试运行时 Git HEAD 为 `183bfca` 加本轮尚未提交的 C21 文件；归档提交将在本轮创建。

## 边界与后续

- 未执行真实 provider、embedding、rerank、ask 或 judge 业务外调；真实前后端联调仍需按项目规则披露模型、样本、调用量、数据出站、费用和副作用并取得授权。21 个跳过用例不算已通过的集成证据。
- `SseEmitter.send` 成功不等于浏览器应用确认收到 terminal；网络断流无 terminal 时只可判 incomplete。断连后不能声称已送达 `CANCELLED`，也不能据此断言 provider 未产生调用或费用。
- 前端 R5、C19 校准及 C20 门禁是后续独立阶段，不由本次 C21 本地后端验收代替。
