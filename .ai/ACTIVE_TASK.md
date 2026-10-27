# Active Task

## Status

`ACTIVE · REVIEW_READY`

正式轨迹组件及同步/流式数据接线已实现，输入区模板/CSS 指纹一致；本地验收见[接入记录](../openspec/changes/rag-execution-trace-view/implementation-review.md)。真实业务联调与接受/归档仍待授权（WIP = 1/1）。

## Active (WIP Limit = 1)

- **rag-execution-trace-view**：[设计](../openspec/changes/rag-execution-trace-view/design.md) / [任务](../openspec/changes/rag-execution-trace-view/tasks.md)。2026-10-27 用户认可第二版并授权正式接入，明确输入框不变；真实业务调用预算 0，Agent 提交及接受/归档未授权。

## Parked & Remaining Evidence Debt

| ID / 债务项 | 当前状态 | 停放原因 / 唤醒条件 | 详情入口 |
| :--- | :--- | :--- | :--- |
| **login-particle-story** | `USER_ACCEPTED · ARCHIVE_PENDING` | 用户已确认验收并提交；本轮切换至轨迹规划，baseline 同步/正式归档另行处理 | [原任务与证据](../openspec/changes/login-particle-story/tasks.md) |
| **W1-E1 (C19 Live)** | `PARKED` | 离线工具链已归档；完整 24×3 Judge Live 校准待独立新 run 与外部调用授权 | [技术债 W1-E1](../docs/roadmap/technical-debt.md#2-judge-真实校准) / [C19 归档](../openspec/changes/archive/2026-10-21-judge-live-calibration-evidence/acceptance.md) |
| **W1-E2/E3 (C20 Live)** | `PARKED` | DRAFT profile 与三轮 intake 编译器已归档；待三轮独立 full150 参考证据与阈值批准后激活 | [技术债 W1-E2/E3](../docs/roadmap/technical-debt.md#3-objective--judge-quality-profile) / [C20 归档](../openspec/changes/archive/2026-10-21-objective-judge-quality-gate-profile/acceptance.md) |
| **Frontend Live (F4–F6)** | `PARKED` | 客户端工程与手工主路径已收口；完整任务链回读、拖放、清理与长稳待独立联调授权 | [前端收口索引](../docs/roadmap/frontend-demo-closeout-2026-10-18.md) |

## Last Completed

- **2026-10-26**：用户确认登录页验收并提交；此前 Wave 1 与前端接受范围见 [迭代蓝图](../docs/roadmap/iteration-blueprint.md)，未闭环证据见 [技术债](../docs/roadmap/technical-debt.md)。
