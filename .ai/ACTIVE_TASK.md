# Active Task

## Status

`ACTIVE`

- 当前唯一Type C：`judge-live-calibration-evidence`（C19）。用户于2026-10-18明确选择“暂不继续外部调用”，当前外调暂停。零外调实现与独立120秒canary已完成，原full仍不完整；新的120秒full72未授权/未执行。入口：[proposal](../openspec/changes/judge-live-calibration-evidence/proposal.md)、[design](../openspec/changes/judge-live-calibration-evidence/design.md)、[tasks](../openspec/changes/judge-live-calibration-evidence/tasks.md)、[readiness](../openspec/changes/judge-live-calibration-evidence/readiness.md)。
- 前端Demo于2026-10-18按用户确认的客户端/手工使用范围完成整体接受收口；各Type C已归档，F1/F6为Type B，无新delta待归档。见[收口索引](../docs/roadmap/frontend-demo-closeout-2026-10-18.md)。真实权限/持久化/拖放/清理/设备/流式稳定性债务保留。
- 原60秒full41有效/1timeout/30未调用，正式率N/A，见[旧full review](../docs/eval/reports/c19-full-review-v1.json)。独立120秒canary在clean HEAD193e46c完成4/4、HTTP200/解析100%、错误/重试0，见[新canary review](../docs/eval/reports/c19-canary-timeout120-review-v1.json)；仅单轮4条，不证明原超时根因已解决。默认60秒及旧证据保留，下一步[120秒full计划](../docs/eval/reports/c19-full-timeout120-plan-v1.json)待授权。
- 推进顺序：C21/R5 → 前端Demo（已接受范围收口）→ C19校准 → C20门禁；分别验收。

## Last Completed Stage

- 前端Demo：2026-10-18整体接受，见[收口索引](../docs/roadmap/frontend-demo-closeout-2026-10-18.md)；F4/F5真实链条partial，F6单次成功不外推长期稳定性。最后完成的Type C为下列登录动效。

- Change ID：`frontend-auth-motion-alignment`（F2剩余正式登录双翼动效）。
- 归档路径：`openspec/changes/archive/2026-10-16-frontend-auth-motion-alignment/`。
- 2026-10-16用户确认接受并要求归档；2项delta/6场景逐字进入baseline；实现62961a2，102/102、含vue-tsc正式build、41/41浏览器结果复用，本轮26图/4报告hash一致。
- 入口：[验收](../openspec/changes/archive/2026-10-16-frontend-auth-motion-alignment/acceptance.md)、[tasks](../openspec/changes/archive/2026-10-16-frontend-auth-motion-alignment/tasks.md)。原始采证元数据保留。

## Current Boundary

- 2026-10-18用户明确要求“为我提交commit，继续下一步”，授权Agent暂存并本地提交本轮前端收口/C19离线实现及随后C19准备文件；push/PR/发布/部署均未授权。
- 用户暂不继续外部调用，不启动新full或其他provider请求；恢复须用户重新明确授权。独立120秒canary授权4已用4；原full剩余30不可续跑/拼接。保留新full72审阅计划及既有证据，授权0/实际0。accepted spec/C17/C18 evidence、生产行为不改，C19仍ACTIVE但外调暂停。
- 累计50次尝试/49个HTTP200/1个旧timeout，账单/限流未独立核验，旧timeout远端结果/费用未知。费用依据用户免费额度声明；凭据源只读且未变，值不进入命令/日志。C19仍未完成，不归档或启动C20，未push/PR/部署。
- 全量搜索、多轮、账号/工作台、C20 profile/阈值、生产默认prompt/parser/retrieval/SSE不在本change范围。
- 前端[F4/F5执行包](../docs/roadmap/frontend-live-integration-execution.md)尚未执行；[F6](../docs/roadmap/frontend-f6-client-acceptance.md)和[诊断](../docs/roadmap/frontend-stream-failure-diagnosis-2026-10-18.md)原证据保留。前次STREAM_FAILED根因unknown，不关闭真实债务，不承诺服务端取消或零provider费用。
