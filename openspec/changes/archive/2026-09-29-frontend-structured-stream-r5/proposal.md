# R5：前端接入结构化流式终态

## 目标

正式聊天页显式请求 C21 已接受的 `structured-v1`，从同一次流展示文本、终态与有效来源。没有 terminal 时只报告传输不完整。同步问答仍为默认方式。

## 范围

- 在现有 `useSSE` 路径解析具名 `text` 与唯一 `terminal`；保持旧文本解析器供既有调用使用。
- 将 `ANSWER / NO_ANSWER / UNSUPPORTED / INVALID / ERROR / CANCELLED` 映射为明确的前端显示状态。仅 `ANSWER` 展示 terminal 中的 citations。
- 使用合成流验证分帧、非法或重复 terminal、断流、客户端 abort 和失败后的部分文本；运行正式前端 build。

## 边界

初始离线切片不改后端、provider、数据模型或评测逻辑，不新增依赖，也不做真实 ask/SSE、登录、上传或 history 写入联调。后续真实 SSE 联调在另行披露并获授权后执行，结果见 `acceptance.md`；离线验证本身不证明该联调通过。客户端主动停止只说明本地停止接收，不证明服务端取消或无费用。
