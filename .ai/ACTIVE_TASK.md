# Active Task

## Status

`ACTIVE`

- 当前唯一Type C：`judge-live-calibration-evidence`（C19）。用户明确批准具体设计，零外调实现已完成；真实canary/full待分别披露授权。入口：[proposal](../openspec/changes/judge-live-calibration-evidence/proposal.md)、[design](../openspec/changes/judge-live-calibration-evidence/design.md)、[tasks](../openspec/changes/judge-live-calibration-evidence/tasks.md)、[readiness](../openspec/changes/judge-live-calibration-evidence/readiness.md)。
- 前端Demo于2026-10-18按用户确认的客户端/手工使用范围完成整体接受收口；各Type C已归档，F1/F6为Type B，无新delta待归档。见[收口索引](../docs/roadmap/frontend-demo-closeout-2026-10-18.md)。真实权限/持久化/拖放/清理/设备/流式稳定性债务保留。
- C19离线实现提交`e107b2c`；用户提供模型后独立冻结judge为`nvidia/nemotron-3-super-120b-a12b`。最新judge29/29通过，全Python279 tests仍为同一C18旧audit的1 failure/4 errors，不冒称GREEN。见[canary执行包](../openspec/changes/judge-live-calibration-evidence/canary-execution.md)，模型配置准备提交后须在clean HEAD复核本地preflight；真实预算0。
- 推进顺序：C21/R5 → 前端Demo（已接受范围收口）→ C19校准 → C20门禁；分别验收。

## Last Completed Stage

- 前端Demo：2026-10-18整体接受，见[收口索引](../docs/roadmap/frontend-demo-closeout-2026-10-18.md)；F4/F5真实链条partial，F6单次成功不外推长期稳定性。最后完成的Type C为下列登录动效。

- Change ID：`frontend-auth-motion-alignment`（F2剩余正式登录双翼动效）。
- 归档路径：`openspec/changes/archive/2026-10-16-frontend-auth-motion-alignment/`。
- 2026-10-16用户确认接受并要求归档；2项delta/6场景逐字进入baseline；实现62961a2，102/102、含vue-tsc正式build、41/41浏览器结果复用，本轮26图/4报告hash一致。
- 入口：[验收](../openspec/changes/archive/2026-10-16-frontend-auth-motion-alignment/acceptance.md)、[tasks](../openspec/changes/archive/2026-10-16-frontend-auth-motion-alignment/tasks.md)。原始采证元数据保留。

## Current Boundary

- 2026-10-18用户明确要求“为我提交commit，继续下一步”，授权Agent暂存并本地提交本轮前端收口/C19离线实现及随后C19准备文件；push/PR/发布/部署均未授权。
- 仅已批准的C19离线评测实现与下一步准备；不修改accepted spec/C17/C18 evidence或生产行为。实现提交身份以Git及后续日志补录为准，具体文件与验证见readiness及AGENT_LOG。
- canary最多4次、full最多72次judge，各阶段独立披露授权；provider/model/endpoint已固定，本次费用/配额及认证未核实，live预算0。用户提供模型或凭据不自动授予批量调用授权。
- 全量搜索、多轮、账号/工作台、C20 profile/阈值、生产默认prompt/parser/retrieval/SSE不在本change范围。
- 前端[F4/F5执行包](../docs/roadmap/frontend-live-integration-execution.md)尚未执行；[F6](../docs/roadmap/frontend-f6-client-acceptance.md)和[诊断](../docs/roadmap/frontend-stream-failure-diagnosis-2026-10-18.md)原证据保留。前次STREAM_FAILED根因unknown，不关闭真实债务，不承诺服务端取消或零provider费用。
