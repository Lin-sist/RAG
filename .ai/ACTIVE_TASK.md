# Active Task

## Status

`ACTIVE · REVIEW_READY`

已修复用户反馈的头像保存 500 与复杂核对界面，本地验证通过待审阅；MySQL/真实账号仍缺证据（WIP = 1/1）。

## Active (WIP Limit = 1)

- **account-profile-editing**：[设计](../openspec/changes/account-profile-editing/design.md) / [任务](../openspec/changes/account-profile-editing/tasks.md)。2026-10-27 用户“确认方案”，授权实现及本地验证；实际账号/数据库写入与业务出站预算 0，无提交/部署/接受/归档授权。

## Parked & Remaining Evidence Debt

| ID / 债务项 | 当前状态 | 停放原因 / 唤醒条件 | 详情入口 |
| :--- | :--- | :--- | :--- |
| **settings-modal-preferences** | `REVIEW_READY · ACCEPTANCE_PENDING` | 用户转向个人资料编辑；原设置未接受/归档 | [任务与证据](../openspec/changes/settings-modal-preferences/tasks.md) |
| **workspace-context-visibility** | `REVIEW_READY · ACCEPTANCE_PENDING` | 用户转向设置实现；前项未接受/归档 | [实施记录](../openspec/changes/workspace-context-visibility/implementation-review.md) |
| **rag-execution-trace-view** | `USER_ACCEPTED · ARCHIVE_PENDING` | 用户已确认验收；真实 provider/实体设备未验，baseline/归档待处理 | [接受说明](../openspec/changes/rag-execution-trace-view/implementation-review.md) |
| **login-particle-story** | `USER_ACCEPTED · ARCHIVE_PENDING` | 用户已确认验收并提交；本轮切换至轨迹规划，baseline 同步/正式归档另行处理 | [原任务与证据](../openspec/changes/login-particle-story/tasks.md) |
| **W1-E1 (C19 Live)** | `PARKED` | 离线工具链已归档；完整 24×3 Judge Live 校准待独立新 run 与外部调用授权 | [技术债 W1-E1](../docs/roadmap/technical-debt.md#2-judge-真实校准) / [C19 归档](../openspec/changes/archive/2026-10-21-judge-live-calibration-evidence/acceptance.md) |
| **W1-E2/E3 (C20 Live)** | `PARKED` | DRAFT profile 与三轮 intake 编译器已归档；待三轮独立 full150 参考证据与阈值批准后激活 | [技术债 W1-E2/E3](../docs/roadmap/technical-debt.md#3-objective--judge-quality-profile) / [C20 归档](../openspec/changes/archive/2026-10-21-objective-judge-quality-gate-profile/acceptance.md) |
| **Frontend Live (F4–F6)** | `PARKED` | 客户端工程与手工主路径已收口；完整任务链回读、拖放、清理与长稳待独立联调授权 | [前端收口索引](../docs/roadmap/frontend-demo-closeout-2026-10-18.md) |

执行轨迹接受范围见[接受记录](../openspec/changes/rag-execution-trace-view/implementation-review.md)，长期规划见[迭代蓝图](../docs/roadmap/iteration-blueprint.md)。
