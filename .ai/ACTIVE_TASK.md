# Active Task

## Status

`PLANNING`

- 当前change：`frontend-auth-motion-alignment`（F2剩余：正式登录双翼动效），事前审查包已准备，等待批准后实施。
- 入口：[proposal](../openspec/changes/frontend-auth-motion-alignment/proposal.md)、[design](../openspec/changes/frontend-auth-motion-alignment/design.md)、[tasks](../openspec/changes/frontend-auth-motion-alignment/tasks.md)、[spec delta](../openspec/changes/frontend-auth-motion-alignment/specs/rag-system/spec.md)。
- 推进顺序：C21/R5 → 前端Demo接入和联调 → C19校准 → C20门禁；后续阶段分别验收。

## Last Completed Change

- Change ID：`frontend-history-feedback-alignment`（F5客户端）。
- 归档路径：`openspec/changes/archive/2026-10-14-frontend-history-feedback-alignment/`。
- 2026-10-14用户明确接受归档；四项delta/11场景逐字进入baseline。工程由现有31ad3f8保存，82/82、正式build、39项封闭HTTP及73图证据复用，本轮重验图像hash。真实历史/反馈/删除闭环未验证，移交技术债。
- 入口：[验收](../openspec/changes/archive/2026-10-14-frontend-history-feedback-alignment/acceptance.md)、[tasks](../openspec/changes/archive/2026-10-14-frontend-history-feedback-alignment/tasks.md)、[技术债](../docs/roadmap/technical-debt.md#f5-客户端归档后的证据债务2026-10-14)。

## Current Boundary

- 本轮用户已授权Agent本地提交并继续Demo接入；F5归档f595099、F1退出9061f1c、登录主题修复2b459da已提交，相关成功验证复用。push/PR/发布/部署仍未授权。
- 下一Type C仅迁入已确认双翼装饰、局部主题色和生命周期/降级；保留真实表单和零真实调用。当前只提交规划、不改动效业务源码；具体方案须事前批准，不因原型或本地commit授权而视为新change已验收。
- 真实backend/auth/provider/upload/ask/feedback/delete预算0；无真实业务写入。F5全量关键词/Ctrl+K、多轮/重命名、反馈改写/删除、F6整体验收、后端/API/DTO/模型/评测及C19/C20不纳入本change。
- F4客户端接受保持不变，拖放和真实闭环/清理债务继续保留；任务取消、跨刷新恢复、原文预览/下载独立。
- R5真实成功证据仅覆盖KB17合成问题的ANSWER；非答案/断连仍由合成约束。客户端断流只判incomplete，不宣称服务端取消或无provider费用。
