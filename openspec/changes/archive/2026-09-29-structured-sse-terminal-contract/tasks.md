# C21 任务

## 事前契约闸门

- [x] 审查 proposal、design、spec delta；锁定 wire 字段/nullability、reason 枚举、Router-off identity、历史写入与 terminal 发送时序。
- [x] 明确 legacy/Router enabled 状态到 `ANSWER / NO_ANSWER / UNSUPPORTED / ERROR / CANCELLED` 的映射，以及 timeout/无 terminal 断流；保留 C16 `INVALID`。

## 后端契约与验证

- [x] 首切片：单次流执行形成内部结构化结果及有效 citations/已观测的预算 usage；legacy 未建预算账本时保留 `null`。聚焦测试证明不重发问答、不伪造零值。此项尚不输出新 SSE wire，也不修正 Controller 历史写入。
- [x] 次切片：显式版本协商、具名 text/terminal event、legacy wire 兼容与安全载荷；MockMvc 覆盖完整回答、无答案、部分文本后错误及唯一终态。
- [x] 第三切片：补服务端 timeout、最后一块后断连与 terminal 发送失败的确定性测试；断连或发送失败时无 terminal 可交付，客户端按协议判为 incomplete，不保存成功历史。
- [x] 收尾切片：完成 history/cache/query-count 与租户作用域矩阵；覆盖文本发送失败、断连、timeout、拒绝访问、认证租户作用域及流式 cache 无读写。
- [x] 对齐同步/SSE/MCP 终态语义；同步非 `ANSWER` 不写历史，MCP `UNSUPPORTED/INVALID` 不标 `ok`；Java 聚焦测试及全仓 `mvn test` 通过。真实 provider 情形未验证。
- [x] 验收证据写入 `acceptance.md`；两项 delta requirement 接受进 baseline spec，C21 已按用户授权归档并将 `ACTIVE_TASK` 置为 `IDLE`。

## 后续顺序（不属于本 change 的勾选）

1. 前端 R5 接入已接受的结构化协议，展示终态、来源与 incomplete；运行正式 build 和合成测试。
2. 前后端联调：披露测试 KB、账号、provider/model、最大调用量、数据出站、费用/限流、timeout/retry、history/query-count 副作用，取得真实调用授权。
3. C19 独立校准：canary 4 次、full 最多 72 次 judge 调用，分阶段授权；缺 case/repeat 时不得生成 agreement 结论。
4. C20 独立门禁：在 C18/C19 充分证据后建 objective/judge profile，阈值经用户审阅；judge 可先 advisory，不自动升为 required。
