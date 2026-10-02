# Active Task

## Status

`IDLE`

- 当前没有活动 change。F4本次接受范围已完成并归档；未自动启动后续阶段。
- 推进顺序：C21 后端契约与验证 → 前端接入和联调 → C19 校准 → C20 门禁。后续阶段分别验收。

## Last Completed Change

- Change ID：`frontend-kb-upload-alignment`（F4客户端阶段）。
- 归档路径：`openspec/changes/archive/2026-10-02-frontend-kb-upload-alignment/`。
- 阶段：2026-10-02用户要求归档；按已交付客户端契约接受四项delta，工程66/66及正式build、36张矩阵和上传恢复合成证据完成。拖放与真实闭环未验证，移交技术债；不称完整真实F4业务闭环。工程提交`da4bc681`、证据提交`e238ee9`。
- 入口：[验收](../openspec/changes/archive/2026-10-02-frontend-kb-upload-alignment/acceptance.md)、[tasks](../openspec/changes/archive/2026-10-02-frontend-kb-upload-alignment/tasks.md)、[技术债](../docs/roadmap/technical-debt.md#f4-客户端归档后的证据债务2026-10-02)。F3三项、首批五区域、R5与C21已分别接受归档。

## Current Boundary

- 本轮用户实施、Agent提交及F4归档授权已执行；归档接受仅覆盖客户端展示与状态契约，不包含push/PR/发布/部署。
- 真实backend/provider/upload/ask/delete预算仍0，专用闭环与后续清理按披露授权另行执行；拖放动作与真实链路证据待技术债收口。任务取消、跨刷新恢复、原文预览/下载、F1 logout、F5、后端/API/DTO/模型/评测与C19/C20不在本次接受范围。
- 前端 R5 使用已接受的后端 `structured-v1` wire；真实成功证据仅覆盖 KB17 合成问题的 `ANSWER`，非答案和断连仍由合成测试约束。C19/C20 仍需各自契约与外调授权。客户端断流仍只可判定 incomplete，不能宣称服务端取消或无 provider 费用。
