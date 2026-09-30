# R5 前端结构化流式接入验收记录（2026-09-29）

## 结论与边界

- `confirmed`：正式 `/chat` 显式选择流式模式后，经 Vite 代理向现有后端请求 `structured-v1`，同一次流的文本、有效 `ANSWER` 终态与 1 条 citation 在浏览器页面展示；成功回答触发历史列表刷新。
- `confirmed`：合成测试覆盖非答案、断流、非法/重复 terminal、未知事件、终态后文本与本地 abort；这些场景只在合成流中验证，不冒充真实 provider 结果。
- `partial`：真实环境仅验证了一个完整 `ANSWER`；非答案、实际断连和客户端停止后的服务端行为仍没有真实链路证据。
- `out_of_scope`：服务端取消确认、provider 零费用承诺、C19/C20 指标与门禁、生产级多租户能力。

## 实现与确定性验证

- 正式前端复用现有 `useSSE` 网络入口，通过 `X-RAG-Stream-Contract: structured-v1` 协商；同步问答仍为默认。文本仅来自具名 `text` 事件，完整性只由唯一有效 `terminal` 决定，来源仅来自本次 `ANSWER` terminal。对应 design 决策 1、2、3。
- 本 change 的 39/39 前端合成测试、`npm run build`（含 `vue-tsc -b` 与 `vite build`）及 `git diff --check` 已通过；本次真实联调未改前端代码，复用同一代码/依赖的验证结果。既有大于 500 kB chunk 警告未消除。

## 真实联调

- 前置身份：测试账号、专用 KB 17 `frontend-r3b-smoke-20260926`、文档 60 `COMPLETED`、document/vectorCount `1/1`；配置的 chat 模型为 `nvidia/nemotron-3-super-120b-a12b`，embedding 为 `nvidia/nemotron-3-embed-1b`。合成问题为“北星令牌的代号是什么？”，前端 topK 默认 5、无自动业务重试；本轮没有上传、删除或清理。
- 首次页面发送遇到 Vite 至后端的 `ECONNREFUSED`/HTTP 500，原因是我误判监听后重新启动后端造成的短暂窗口；页面正确报告未交付有效终态。该次后 KB17 queryCount 仍为 3，未见新 history；没有证据表明到达后端或 NVIDIA。失败与原因保留在 AGENT_LOG。
- 用户随后授权继续测试且无需逐次请示业务调用。第二次从 Chrome 中的正式 `/chat` 页面发送同一问题，浏览器显示 `CEDAR-47`、“完整回答 · 来源来自本次流式问答”以及文档 60“Frontend R3b 合成联调资料”的一条引用。后端记录 `POST /api/qa/ask/stream` status=200、trace `001a0edbb414b4a9da4ef99e3d556ba6`、contextCount=1、chat model `nvidia/nemotron-3-super-120b-a12b`、`stream_delivery_complete`（chunkCount=2）及成功保存 history id 648。实际链路包含查询 embedding、检索/重排和流式 generation；未见重试或 judge 调用。后端完成日志的 totalLatencyMs=4263。
- 后置只读核对：KB17 queryCount `3→4`、document/vectorCount 仍为 `1/1`，全局 history `647→648`。GET `/api/history/648` 回读同一问题、答案 `CEDAR-47`、kbId 17 与 1 条 citation，citation 的 documentId=60、snippet 含同一代号。浏览器 UI、后端完成日志与持久化读回互相一致。

## 剩余风险与治理

- `SseEmitter.send` 无客户端确认语义；本次浏览器实际收到并展示完整回答，但不能由此推断其他断连场景的服务端取消或费用结果。引用检索相关度约 3%，仅为本次合成数据的展示值，不是 RAG 质量评测。
- 本轮不改后端、provider、模型配置、数据模型或评测逻辑；不修改 `.env.local`、用户未跟踪文档，不暂存、提交、push、PR、发布或部署。
- 用户随后明确授权本 change 的 baseline 接受与归档。`Frontend Structured SSE Presentation` requirement 已逐字接受进 `openspec/specs/rag-system/spec.md`，且核对为唯一一次出现；归档目录为 `openspec/changes/archive/2026-09-29-frontend-structured-stream-r5/`。
