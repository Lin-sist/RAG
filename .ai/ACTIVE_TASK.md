# Active Task

## Status

`ACTIVE`

- 当前 change：`frontend-demo-experience-alignment`。
- 阶段：2026-09-30 用户已授权按既定五区域规划实施；首批工程与交互验证完成，严格视觉收口待复核。详见 [acceptance](../openspec/changes/frontend-demo-experience-alignment/acceptance.md)。
- 入口：[proposal](../openspec/changes/frontend-demo-experience-alignment/proposal.md)、[design](../openspec/changes/frontend-demo-experience-alignment/design.md)、[tasks](../openspec/changes/frontend-demo-experience-alignment/tasks.md)、[spec delta](../openspec/changes/frontend-demo-experience-alignment/specs/rag-system/spec.md)。
- 推进顺序：C21 后端契约与验证 → 前端接入和联调 → C19 校准 → C20 门禁。后续阶段分别验收。

## Last Completed Change

- Change ID：`frontend-structured-stream-r5`（R5）
- 归档路径：`openspec/changes/archive/2026-09-29-frontend-structured-stream-r5/`
- 阶段：正式前端结构化 SSE 接入与单题真实 `ANSWER` 联调验收完成；详见归档 `acceptance.md`。C21 后端契约此前已单独验收归档。

## Current Boundary

- 首批仅侧栏、品牌、主题、首页和设置；当前 checkout 已包含 R5 本地提交 `0024e92`。本轮实现已授权，提交责任为用户手动提交；真实业务调用、baseline 接受与归档仍未授权。不修改后端、评测或模型配置。
- 前端 R5 使用已接受的后端 `structured-v1` wire；真实成功证据仅覆盖 KB17 合成问题的 `ANSWER`，非答案和断连仍由合成测试约束。C19/C20 仍需各自契约与外调授权。客户端断流仍只可判定 incomplete，不能宣称服务端取消或无 provider 费用。
