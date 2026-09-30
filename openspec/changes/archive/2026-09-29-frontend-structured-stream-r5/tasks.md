# R5 任务

- [x] 核对 C21 accepted spec、后端 wire 与现有 R4 前端代码；确定 R5 离线范围。
- [x] 现有 `useSSE` 加入版本化结构化解析和严格终态判定。
- [x] 正式聊天页展示本次终态及有效来源，保留同步默认。
- [x] 合成测试、正式前端 build、变更检查和执行日志。
- [x] 真实前后端/provider 联调：首次固定问题在后端重启窗口发生代理 `ECONNREFUSED`/HTTP 500，失败证据保留；用户随后授权继续测试。第二次从正式 Chrome `/chat` 页面发送，收到有效 `ANSWER`、文本 `CEDAR-47` 与文档 60 来源；后端完成 trace `001a0edbb414b4a9da4ef99e3d556ba6`，KB17 queryCount `3→4`、history 新增 id 648，详见 `acceptance.md`。
- [x] 用户明确授权后，将本 change 的 spec delta 逐字接受进 `openspec/specs/rag-system/spec.md` 并归档；接受内容已核对与 delta 一致。
