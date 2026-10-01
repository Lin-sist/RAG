# Active Task

## Status

`IDLE`

- 当前无 active change。2026-10-01 用户授权提交并进行 F3 归档，本轮按已完成的验收范围接受其 baseline、归档并恢复 IDLE。
- 验收：[acceptance](../openspec/changes/archive/2026-10-01-frontend-chat-source-alignment/acceptance.md)。
- 入口：[proposal](../openspec/changes/archive/2026-10-01-frontend-chat-source-alignment/proposal.md)、[design](../openspec/changes/archive/2026-10-01-frontend-chat-source-alignment/design.md)、[tasks](../openspec/changes/archive/2026-10-01-frontend-chat-source-alignment/tasks.md)、[spec delta](../openspec/changes/archive/2026-10-01-frontend-chat-source-alignment/specs/rag-system/spec.md)。
- 推进顺序：C21 后端契约与验证 → 前端接入和联调 → C19 校准 → C20 门禁。后续阶段分别验收。

## Last Completed Change

- Change ID：`frontend-chat-source-alignment`（F3）。
- 归档路径：`openspec/changes/archive/2026-10-01-frontend-chat-source-alignment/`。
- 阶段：F3 工程、合成与指定视口视觉复核完成，用户授权提交及归档，三项 delta 接受进 baseline。首批五区域、R5 与 C21 后端契约已分别验收归档。

## Current Boundary

- F3 仅正文、Markdown/代码复制、独立来源展开和本地阅读滚动。用户已授权 Agent 提交本轮文件，不包含 push/PR/发布/部署或真实业务调用；F1 logout、F4/F5 整页迁移尚未完成，后续需各自规划与验收。不修改后端、评测或模型配置。
- 前端 R5 使用已接受的后端 `structured-v1` wire；真实成功证据仅覆盖 KB17 合成问题的 `ANSWER`，非答案和断连仍由合成测试约束。C19/C20 仍需各自契约与外调授权。客户端断流仍只可判定 incomplete，不能宣称服务端取消或无 provider 费用。
