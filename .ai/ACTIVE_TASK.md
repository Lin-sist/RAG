# Active Task

## Status

`ACTIVE`

## Active Change

- Change ID：`generation-objective-evidence-baseline`
- 路径：`openspec/changes/generation-objective-evidence-baseline/`
- 阶段：`C18_W0_COMPLETE_W1_PENDING`
- 目标：judge关闭下建立v2/150条一次完整真实generation/citation/objective claim/no-answer基线。

## Current Boundary

- C17文件验收归档已完成：4 requirements/12 scenarios已接受进baseline，ACTIVE profile/locked reference、原始三轮各12/12 PASS、Python244 tests通过。归档路径：`openspec/changes/archive/2026-09-10-retrieval-quality-gate-activation/`。
- C17已于2026-09-11本地提交：`9276d114058664a3cf33f26fecca046e68e566a4`。用户明确授权Agent本地提交并保持Git干净，原提交审批阻断已解除；C18规划与交接文档随后独立提交。
- 用户于2026-09-11明确要求开始C18实现；W0调用图审计及budget plan-only已完成，W1完整manifest/runner护栏/compiler仍待实施；无任何真实业务调用。
- 当前已冻结canary/full query embedding上限34/1492，含解释回退12/590，ask/generation上限5/150；共享USER限流要求每请求至少2.2秒。33项Java聚焦与252项Python tests通过。执行前仍需runtime fingerprint、有限调用护栏、完整compiler与单独外调授权。
- REST ask会写query count/history；后续外调披露必须包含该本地副作用。当前backend/provider/KB/SQL/Milvus调用或mutation=0，业务数据出站=false。
- 本轮C18规划提交责任：`Agent 提交`（2026-09-11用户明确授权本地提交并保持Git干净）。用户已授权C18离线实现，本轮完成W0及budget plan-only；未执行任何live调用。后续canary/full及验收/归档仍按对应范围授权；无push/PR/deploy授权。
