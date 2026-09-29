# R4 验收记录

## 结论

`frontend-text-stream-r4` 在纯文本流边界内接受：正式 `/chat` 默认同步问答，可显式选择纯文本流；UI 只报告文本及本地传输结果。流式来源、结构化业务终态和可靠服务端取消留待 C21。

## 证据

- `tasks.md` 四项均完成；`npm run test` 在当前隔离分支 HEAD 上为 33/33 PASS，其中 SSE 分帧、UTF-8、错误优先级和客户端 abort 用例通过。
- `npm run build` 已在本轮登录双主题改动后通过 `vue-tsc -b && vite build`；本次收口只改规格与治理文件，不改变前端代码。
- 用户此前授权的 KB17 一次纯文本流 UI 问答返回 `CEDAR-47`，页面显示流结束且无来源；查询次数 2→3，历史新增该问答。该结果仅验证当前环境的一次成功路径，不证明 generation/citation/no-answer 指标。
- 本轮收口没有新的 ask、embedding、rerank、generation 或 judge 业务外调；前端无自动重试。

## 范围与风险

- delta 的 1 项 requirement、3 个 scenarios 已按内容接受进 `openspec/specs/rag-system/spec.md`；后端 SSE wire、provider、检索策略和生产默认未改。
- C21 的 structured terminal、流式 citations 与可靠服务端取消仍未实现。没有重跑真实 provider 问答或故障注入；Java/Python 未改，未重跑相应测试。
