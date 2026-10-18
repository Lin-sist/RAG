# Active Task

## Status

`ACTIVE`

- 当前唯一Type C：`judge-live-calibration-evidence`（C19）。用户明确批准具体设计，零外调实现已完成；已分阶段授权并执行canary/full，full不完整，恢复选择待确认。入口：[proposal](../openspec/changes/judge-live-calibration-evidence/proposal.md)、[design](../openspec/changes/judge-live-calibration-evidence/design.md)、[tasks](../openspec/changes/judge-live-calibration-evidence/tasks.md)、[readiness](../openspec/changes/judge-live-calibration-evidence/readiness.md)。
- 前端Demo于2026-10-18按用户确认的客户端/手工使用范围完成整体接受收口；各Type C已归档，F1/F6为Type B，无新delta待归档。见[收口索引](../docs/roadmap/frontend-demo-closeout-2026-10-18.md)。真实权限/持久化/拖放/清理/设备/流式稳定性债务保留。
- C19 full在clean HEAD14efadc完成42次尝试后首错停止：41有效/HTTP200、1次60265ms timeout、余30未调用、retry0；compiler INCOMPLETE，agreement/repeat率N/A，见[full安全review](../docs/eval/reports/c19-full-review-v1.json)。先前canary4/4仅小样本证据，不能补齐full。新增[恢复审阅包](../openspec/changes/judge-live-calibration-evidence/recovery-plan.md)与独立120秒候选，未改默认60秒，待用户选择及新预算授权。
- 推进顺序：C21/R5 → 前端Demo（已接受范围收口）→ C19校准 → C20门禁；分别验收。

## Last Completed Stage

- 前端Demo：2026-10-18整体接受，见[收口索引](../docs/roadmap/frontend-demo-closeout-2026-10-18.md)；F4/F5真实链条partial，F6单次成功不外推长期稳定性。最后完成的Type C为下列登录动效。

- Change ID：`frontend-auth-motion-alignment`（F2剩余正式登录双翼动效）。
- 归档路径：`openspec/changes/archive/2026-10-16-frontend-auth-motion-alignment/`。
- 2026-10-16用户确认接受并要求归档；2项delta/6场景逐字进入baseline；实现62961a2，102/102、含vue-tsc正式build、41/41浏览器结果复用，本轮26图/4报告hash一致。
- 入口：[验收](../openspec/changes/archive/2026-10-16-frontend-auth-motion-alignment/acceptance.md)、[tasks](../openspec/changes/archive/2026-10-16-frontend-auth-motion-alignment/tasks.md)。原始采证元数据保留。

## Current Boundary

- 2026-10-18用户明确要求“为我提交commit，继续下一步”，授权Agent暂存并本地提交本轮前端收口/C19离线实现及随后C19准备文件；push/PR/发布/部署均未授权。
- 已批准C19离线实现及分阶段真实canary/full；本次full因timeout按首错停止，当前仅保存证据和准备恢复提案，不继续provider调用。accepted spec/C17/C18 evidence、生产行为不改；具体验证见readiness/AGENT_LOG。
- canary已用4，原full72上限下已尝试42、余30未用；因首错停止不可续跑/拼接，当前新run授权0。费用依据用户免费额度声明，timeout上游完成/扣费未知；不重放或自动补跑。凭据仅live时本地只读映射子进程，源文件未变且值不进入命令/日志。C19未完成、不归档，C20不启动。
- 全量搜索、多轮、账号/工作台、C20 profile/阈值、生产默认prompt/parser/retrieval/SSE不在本change范围。
- 前端[F4/F5执行包](../docs/roadmap/frontend-live-integration-execution.md)尚未执行；[F6](../docs/roadmap/frontend-f6-client-acceptance.md)和[诊断](../docs/roadmap/frontend-stream-failure-diagnosis-2026-10-18.md)原证据保留。前次STREAM_FAILED根因unknown，不关闭真实债务，不承诺服务端取消或零provider费用。
