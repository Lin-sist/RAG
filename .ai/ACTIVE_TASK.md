# Active Task

## Status

`IDLE`

- 无活动OpenSpec change；F5客户端范围已接受归档，F1退出修复按独立Type B推进。
- 推进顺序：C21/R5 → 前端Demo接入和联调 → C19校准 → C20门禁；后续阶段分别验收。

## Last Completed Change

- Change ID：`frontend-history-feedback-alignment`（F5客户端）。
- 归档路径：`openspec/changes/archive/2026-10-14-frontend-history-feedback-alignment/`。
- 2026-10-14用户明确接受归档；四项delta/11场景逐字进入baseline。工程由现有31ad3f8保存，82/82、正式build、39项封闭HTTP及73图证据复用，本轮重验图像hash。真实历史/反馈/删除闭环未验证，移交技术债。
- 入口：[验收](../openspec/changes/archive/2026-10-14-frontend-history-feedback-alignment/acceptance.md)、[tasks](../openspec/changes/archive/2026-10-14-frontend-history-feedback-alignment/tasks.md)、[技术债](../docs/roadmap/technical-debt.md#f5-客户端归档后的证据债务2026-10-14)。

## Current Boundary

- 本轮用户授权F1退出修复：只接已有/auth/logout，处理重复操作、退出失败降级与旧响应不清新会话。默认用户手动提交，无暂存/commit/push/PR/发布/部署授权。
- 真实backend/auth/provider/upload/ask/feedback/delete预算0；无真实业务写入。F5全量关键词/Ctrl+K、多轮/重命名、反馈改写/删除、F2登录视觉集成/F6、后端/API/DTO/模型/评测及C19/C20不纳入本轮。
- F4客户端接受保持不变，拖放和真实闭环/清理债务继续保留；任务取消、跨刷新恢复、原文预览/下载独立。
- R5真实成功证据仅覆盖KB17合成问题的ANSWER；非答案/断连仍由合成约束。客户端断流只判incomplete，不宣称服务端取消或无provider费用。
