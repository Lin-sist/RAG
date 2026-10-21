# Active Task

## Status

`IDLE`

2026-10-21用户明确接受C19/C20已完成的零外调离线工具链与DRAFT契约，授权按该工程范围归档，将完整Live/参考/阈值激活留作P1证据债务。Wave 1（C17–C21）已按各自接受范围全部归档；当前无活动Type C，不自动启动W2。

## Last Completed Stage

| 阶段 | 已接受范围与归档入口 |
| --- | --- |
| C17 | [固定开发集检索门禁](../openspec/changes/archive/2026-09-10-retrieval-quality-gate-activation/acceptance.md)，ACTIVE retrieval profile，不代表生成/judge质量达标 |
| C18 | [单次full150生成/客观指标基线](../openspec/changes/archive/2026-09-22-generation-objective-evidence-baseline/acceptance.md)，judge SKIPPED |
| C19 | [离线校准执行工具链/契约](../openspec/changes/archive/2026-10-21-judge-live-calibration-evidence/acceptance.md)，完整Live未完成 |
| C20 | [DRAFT profile/review/三轮intake](../openspec/changes/archive/2026-10-21-objective-judge-quality-gate-profile/acceptance.md)，空阈值、judge仅辅助、未激活 |
| C21 | [structured-v1后端终态契约](../openspec/changes/archive/2026-09-29-structured-sse-terminal-contract/acceptance.md)，前端[R5](../openspec/changes/archive/2026-09-29-frontend-structured-stream-r5/acceptance.md)已接入 |

前端Demo于2026-10-18按客户端/手工使用范围接受收口，见[收口索引](../docs/roadmap/frontend-demo-closeout-2026-10-18.md)。F4/F5真实链条、权限/持久化/拖放/清理、设备与流式长期稳定性债务继续保留。两个无正文的旧前端空壳目录已按本次授权核实并删除，原archive不动。

## Remaining Evidence Debt

- [P1/W1-E1](../docs/roadmap/technical-debt.md#w1-e1)：完整24×3 judge Live校准及正式agreement/repeat证据。最后一轮24尝试/23有效/1network error/48未调用、retry0，累计83尝试/80 HTTP200/1timeout/2network errors；[最终review](../docs/eval/reports/c19-final-full-review-v1.json)状态与hash不变，不拼接或续跑旧run。
- [P1/W1-E2](../docs/roadmap/technical-debt.md#w1-e2)：三个独立、完整且身份/策略相容的full150真实参考；现有C18历史单轮intake仍INCOMPLETE，不构成三轮参考。
- [P1/W1-E3](../docs/roadmap/technical-debt.md#w1-e3)：逐规则target/tolerance批准、新profile版本/locked reference与激活验收；judge升级required须另有完整校准与批准。C20仍DRAFT/NOT_EVALUABLE。

## Current Boundary

- 本次用户授权验收归档、清理指定空目录及Agent本地提交；不包含push、PR、发布、部署、新provider调用、阈值批准或门禁激活。
- 本轮新增backend/provider/embedding/rerank/ask/judge调用0，业务数据出站0。历史partial/raw/安全报告、C17/C18 baseline与原审计不回写。
- 同一源码的全Python336 tests/OK（2026-10-20，0 failure/error/skip）可复用；该结果不证明真实业务或生产质量。当前源码不兼容旧C18冻结审计是有意保留的执行边界，新采集须独立审阅版本/预算。
- 后续Live补测或质量激活作为独立工作审阅/授权；W2–W6仍属长期规划，不因Wave 1工程归档而自动开工。
