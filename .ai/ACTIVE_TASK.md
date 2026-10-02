# Active Task

## Status

`ACTIVE`

- Change ID：`frontend-kb-upload-alignment`（F4）。阶段：`IMPLEMENTED / 浏览器上传与真实闭环待验收`。
- 2026-10-02 用户批准实施并授权 Agent 提交；客户端切片已实施，66/66 测试及 vue-tsc/Vite 正式 build 通过。视觉矩阵已完成五态六组，任务状态矩阵与浏览器上传仍待权限确认；真实闭环、baseline 接受/归档尚未执行。
- 入口：[proposal](../openspec/changes/frontend-kb-upload-alignment/proposal.md)、[design](../openspec/changes/frontend-kb-upload-alignment/design.md)、[tasks](../openspec/changes/frontend-kb-upload-alignment/tasks.md)、[阶段验收](../openspec/changes/frontend-kb-upload-alignment/acceptance.md)、[spec delta](../openspec/changes/frontend-kb-upload-alignment/specs/rag-system/spec.md)。
- 推进顺序：C21 后端契约与验证 → 前端接入和联调 → C19 校准 → C20 门禁。后续阶段分别验收。

## Last Completed Change

- Change ID：`frontend-chat-source-alignment`（F3）。
- 归档路径：`openspec/changes/archive/2026-10-01-frontend-chat-source-alignment/`。
- 阶段：F3 工程、合成与指定视口视觉复核完成，用户授权提交及归档，三项 delta 接受进 baseline。首批五区域、R5 与 C21 后端契约已分别验收归档。

## Current Boundary

- 2026-10-02 用户明确确认 F4 方案执行并授权本轮 Agent 提交。实施范围：正式 KB 列表/网格、详情/表单/统计、上传接受与失败恢复、查询恢复与删除后刷新、指定 KB 新问答导航；不包含 push/PR/发布/部署或 F4 baseline 接受/归档。
- 当前真实 backend/provider/upload/ask/delete 调用预算 0，专用闭环与清理后续分别按披露授权执行。任务取消、跨刷新任务恢复、原文预览/下载、F1 logout、F5、后端/API/DTO/模型/评测与 C19/C20 不在本 change 实施范围。baseline 接受/归档、push/PR/发布/部署未授权。
- 前端 R5 使用已接受的后端 `structured-v1` wire；真实成功证据仅覆盖 KB17 合成问题的 `ANSWER`，非答案和断连仍由合成测试约束。C19/C20 仍需各自契约与外调授权。客户端断流仍只可判定 incomplete，不能宣称服务端取消或无 provider 费用。
