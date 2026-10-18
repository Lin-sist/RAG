# Active Task

## Status

`ACTIVE`

- 当前唯一活动Type C仍为C19。用户明确授权最后一次独立120秒full最多72次（同模型/约58KB/max_tokens1024/间隔至少2s/retry0/首错停止），当前实际0；如再中断则保留证据、暂停C19未完live任务，并转入C20零外调objective独立门禁/judge仅辅助切片，不将C19标完成或归档。
- 前端Demo于2026-10-18按用户确认的客户端/手工使用范围完成整体接受收口；各Type C已归档，F1/F6为Type B，无新delta待归档。见[收口索引](../docs/roadmap/frontend-demo-closeout-2026-10-18.md)。真实权限/持久化/拖放/清理/设备/流式稳定性债务保留。
- 最新120秒full见[安全review](../docs/eval/reports/c19-full-timeout120-review-v1.json)：clean HEAD91dc567，attempt9/cal-ft-rt-03/repeat3在5032ms报judge_network_error、无HTTP状态，未重试。同case前两次5266/17092ms成功，根因/远端完成及费用未知。原60秒full及两轮canary证据、默认配置均保留，不拼接；原plan为执行前授权快照。
- 推进顺序：C21/R5 → 前端Demo（已接受范围收口）→ C19校准 → C20门禁；分别验收。

## Last Completed Stage

- 前端Demo：2026-10-18整体接受，见[收口索引](../docs/roadmap/frontend-demo-closeout-2026-10-18.md)；F4/F5真实链条partial，F6单次成功不外推长期稳定性。最后完成的Type C为下列登录动效。

- Change ID：`frontend-auth-motion-alignment`（F2剩余正式登录双翼动效）。
- 归档路径：`openspec/changes/archive/2026-10-16-frontend-auth-motion-alignment/`。
- 2026-10-16用户确认接受并要求归档；2项delta/6场景逐字进入baseline；实现62961a2，102/102、含vue-tsc正式build、41/41浏览器结果复用，本轮26图/4报告hash一致。
- 入口：[验收](../openspec/changes/archive/2026-10-16-frontend-auth-motion-alignment/acceptance.md)、[tasks](../openspec/changes/archive/2026-10-16-frontend-auth-motion-alignment/tasks.md)。原始采证元数据保留。

## Current Boundary

- 2026-10-18用户明确要求“为我提交commit，继续下一步”，授权Agent暂存并本地提交本轮前端收口/C19离线实现及随后C19准备文件；push/PR/发布/部署均未授权。
- 用户恢复授权的新full72已尝试9次后首错停止，余63不可续跑/拼接；任何新run或诊断性网络请求须另行明确授权，当前只保全/核查本地证据。accepted spec/C17/C18 evidence及生产行为不改，不自动补跑、重试或启动其他业务调用。
- 累计59次尝试/57个HTTP200/1旧timeout/1本轮network error，账单/限流未独立核验，两次失败的远端结果/费用未知。费用依据用户免费额度声明；凭据源只读且未变，值不进入命令/日志。C19仍未完成，不归档或启动C20，未push/PR/部署。
- 全量搜索、多轮、账号/工作台、C20 profile/阈值、生产默认prompt/parser/retrieval/SSE不在本change范围。
- 前端[F4/F5执行包](../docs/roadmap/frontend-live-integration-execution.md)尚未执行；[F6](../docs/roadmap/frontend-f6-client-acceptance.md)和[诊断](../docs/roadmap/frontend-stream-failure-diagnosis-2026-10-18.md)原证据保留。前次STREAM_FAILED根因unknown，不关闭真实债务，不承诺服务端取消或零provider费用。
