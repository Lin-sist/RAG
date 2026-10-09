# Active Task

## Status

`ACTIVE`

- 当前 change：`frontend-history-feedback-alignment`（F5 历史、反馈与查找）。
- 2026-10-03 用户明确“可以进入实现”，按已呈交F5方案实施并验证；默认用户手动提交，真实业务调用预算0。
- 入口：[proposal](../openspec/changes/frontend-history-feedback-alignment/proposal.md)、[design](../openspec/changes/frontend-history-feedback-alignment/design.md)、[tasks](../openspec/changes/frontend-history-feedback-alignment/tasks.md)、[spec delta](../openspec/changes/frontend-history-feedback-alignment/specs/rag-system/spec.md)。
- 推进顺序：C21 后端契约与验证 → 前端接入和联调 → C19 校准 → C20 门禁。后续阶段分别验收。

## Last Completed Change

- Change ID：`frontend-kb-upload-alignment`（F4客户端阶段）。
- 归档路径：`openspec/changes/archive/2026-10-02-frontend-kb-upload-alignment/`。
- 阶段：2026-10-02用户要求归档；按已交付客户端契约接受四项delta，工程66/66及正式build、36张矩阵和上传恢复合成证据完成。拖放与真实闭环未验证，移交技术债；不称完整真实F4业务闭环。工程提交`da4bc681`、证据提交`e238ee9`。
- 入口：[验收](../openspec/changes/archive/2026-10-02-frontend-kb-upload-alignment/acceptance.md)、[tasks](../openspec/changes/archive/2026-10-02-frontend-kb-upload-alignment/tasks.md)、[技术债](../docs/roadmap/technical-debt.md#f4-客户端归档后的证据债务2026-10-02)。F3三项、首批五区域、R5与C21已分别接受归档。

## Current Boundary

- F5客户端实施及验证完成：82/82、含vue-tsc的正式build、39项封闭HTTP检查、24张正式矩阵/18张Demo参考/31张补充图已复核，见[验收](../openspec/changes/frontend-history-feedback-alignment/acceptance.md)。等待用户接受，保持ACTIVE；默认用户手动提交，不借用F4授权，baseline接受/归档及push/PR/发布/部署未授权。
- 真实backend/provider/upload/ask/feedback/delete预算0，无真实业务数据写入；真实验收/删除清理按专用目标披露授权另行执行。全量关键词/Ctrl+K、多轮/重命名、反馈改写/删除、F1、后端/API/DTO/模型/评测及C19/C20在F5范围外。
- F4客户端接受保持不变，拖放和真实闭环/清理债务继续保留；任务取消、跨刷新恢复、原文预览/下载不纳入F5。
- 前端 R5 使用已接受的后端 `structured-v1` wire；真实成功证据仅覆盖 KB17 合成问题的 `ANSWER`，非答案和断连仍由合成测试约束。C19/C20 仍需各自契约与外调授权。客户端断流仍只可判定 incomplete，不能宣称服务端取消或无 provider 费用。
