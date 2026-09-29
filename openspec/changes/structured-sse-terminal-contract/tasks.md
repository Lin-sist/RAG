# C21 任务

## 事前契约闸门

- [ ] 审查 proposal、design、spec delta；锁定 wire 字段/nullability、reason 枚举、Router-off identity、历史写入与 terminal 发送时序。
- [ ] 明确 legacy/Router enabled 状态到 `ANSWER / NO_ANSWER / UNSUPPORTED / ERROR / CANCELLED` 的映射，以及 timeout/无 terminal 断流。

## 后端契约与验证

- [x] 首切片：单次流执行形成内部结构化结果及有效 citations/已观测的预算 usage；legacy 未建预算账本时保留 `null`。聚焦测试证明不重发问答、不伪造零值。此项尚不输出新 SSE wire，也不修正 Controller 历史写入。
- [x] 次切片：显式版本协商、具名 text/terminal event、legacy wire 兼容与安全载荷；MockMvc 覆盖完整回答、无答案、部分文本后错误及唯一终态。
- [ ] 后续验证：补服务端 timeout、断连与最后一块后发送失败的竞态测试；确认不可送达 terminal 时客户端为 incomplete。
- [ ] 后续切片：按最终结果和交付状态约束 history/cache/query-count；当前已将正常历史限定为可发送的 `ANSWER` 并保存同次 citations，仍需覆盖最后一块后断连、发送失败及租户隔离。
- [ ] 对齐同步/SSE/MCP 语义，运行 Java 聚焦测试和风险相称的全仓测试；记录未验证的真实 provider 情形。
- [ ] 汇总后端验收证据；整个 C21 完成后再按授权归档并置 `ACTIVE_TASK` 为 `IDLE`。

## 后续顺序（不属于本 change 的勾选）

1. 前端 R5 接入已接受的结构化协议，展示终态、来源与 incomplete；运行正式 build 和合成测试。
2. 前后端联调：披露测试 KB、账号、provider/model、最大调用量、数据出站、费用/限流、timeout/retry、history/query-count 副作用，取得真实调用授权。
3. C19 独立校准：canary 4 次、full 最多 72 次 judge 调用，分阶段授权；缺 case/repeat 时不得生成 agreement 结论。
4. C20 独立门禁：在 C18/C19 充分证据后建 objective/judge profile，阈值经用户审阅；judge 可先 advisory，不自动升为 required。
