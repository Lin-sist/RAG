# Active Task

## Status

`ACTIVE · REVIEW_READY`

设置浮层首版已接入，等待审阅；前项工作空间保留待验收（WIP = 1/1）。

## Active (WIP Limit = 1)

- **settings-modal-preferences**：[设计](../openspec/changes/settings-modal-preferences/design.md) / [任务](../openspec/changes/settings-modal-preferences/tasks.md)。2026-10-27 用户授权按规划实现首版，真实业务预算 0；无提交/接受/归档授权。

## Parked & Remaining Evidence Debt

| ID / 债务项 | 当前状态 | 停放原因 / 唤醒条件 | 详情入口 |
| :--- | :--- | :--- | :--- |
| **workspace-context-visibility** | `REVIEW_READY · ACCEPTANCE_PENDING` | 用户转向设置实现；前项未接受/归档 | [实施记录](../openspec/changes/workspace-context-visibility/implementation-review.md) |
| **rag-execution-trace-view** | `USER_ACCEPTED · ARCHIVE_PENDING` | 用户已确认验收；真实 provider/实体设备未验，baseline/归档待处理 | [接受说明](../openspec/changes/rag-execution-trace-view/implementation-review.md) |
| **login-particle-story** | `USER_ACCEPTED · ARCHIVE_PENDING` | 用户已确认验收并提交；本轮切换至轨迹规划，baseline 同步/正式归档另行处理 | [原任务与证据](../openspec/changes/login-particle-story/tasks.md) |
| **W1-E1 (C19 Live)** | `PARKED` | 离线工具链已归档；完整 24×3 Judge Live 校准待独立新 run 与外部调用授权 | [技术债 W1-E1](../docs/roadmap/technical-debt.md#2-judge-真实校准) / [C19 归档](../openspec/changes/archive/2026-10-21-judge-live-calibration-evidence/acceptance.md) |
| **W1-E2/E3 (C20 Live)** | `PARKED` | DRAFT profile 与三轮 intake 编译器已归档；待三轮独立 full150 参考证据与阈值批准后激活 | [技术债 W1-E2/E3](../docs/roadmap/technical-debt.md#3-objective--judge-quality-profile) / [C20 归档](../openspec/changes/archive/2026-10-21-objective-judge-quality-gate-profile/acceptance.md) |
| **Frontend Live (F4–F6)** | `PARKED` | 客户端工程与手工主路径已收口；完整任务链回读、拖放、清理与长稳待独立联调授权 | [前端收口索引](../docs/roadmap/frontend-demo-closeout-2026-10-18.md) |

执行轨迹接受范围见[接受记录](../openspec/changes/rag-execution-trace-view/implementation-review.md)，长期规划见[迭代蓝图](../docs/roadmap/iteration-blueprint.md)。
