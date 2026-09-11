# Active Task

## Status

`ACTIVE`

## Active Change

- Change ID：`generation-objective-evidence-baseline`
- 路径：`openspec/changes/generation-objective-evidence-baseline/`
- 阶段：`C18_PLANNED_PENDING_REVIEW`
- 目标：judge关闭下建立v2/150条一次完整真实generation/citation/objective claim/no-answer基线。

## Current Boundary

- C17文件验收归档已完成：4 requirements/12 scenarios已接受进baseline，ACTIVE profile/locked reference、原始三轮各12/12 PASS、Python244 tests通过。归档路径：`openspec/changes/archive/2026-09-10-retrieval-quality-gate-activation/`。
- C17已于2026-09-11本地提交：`9276d114058664a3cf33f26fecca046e68e566a4`。用户明确授权Agent本地提交并保持Git干净，原提交审批阻断已解除；C18规划与交接文档随后独立提交。
- 用户原请求授权readiness后直接规划下一阶段；C18仅proposal/design/tasks/spec delta，8条决策记录、4 requirements/12 scenarios；未开始W0/W1实现或任何真实调用。
- 已确认旧蓝图300 embedding上限不足；canary/full初始组成项22/902仍未覆盖ask解释回退，W0须冻结最终精确整数及代码hash后才能live。
- REST ask会写query count/history；后续外调披露必须包含该本地副作用。当前backend/provider/KB/SQL/Milvus调用或mutation=0，业务数据出站=false。
- 本轮C18规划提交责任：`Agent 提交`（2026-09-11用户明确授权本地提交并保持Git干净）。用户将另行开始C18实现；本轮只完成提交交接，未执行W0/W1或任何live调用。后续canary/full及验收/归档仍按对应范围授权；无push/PR/deploy授权。
