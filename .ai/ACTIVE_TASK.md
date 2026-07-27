# Active Task

## Status

`ACTIVE`

## Active Change

- Change：`tenant-isolation-adversarial-evaluation`
- 阶段：C14 实现与证据闭环完成，待用户最终验收/归档
- 位置：`openspec/changes/tenant-isolation-adversarial-evaluation/`
- 类型：Type C（新增版本化隔离/恶意样本评测能力与租户能力声明门禁）
- 提交责任：用户已于 2026-07-27 明确授权 Agent 提交当前 C14 计划内实现与治理文件；不 push、不创建 PR、不部署。

## Scope

1. 建立版本化 tenant isolation adversarial release、schema、validator、runner/report status 和 no-overwrite evidence。
2. 规划隔离 MySQL/Redis/Milvus + test-only 双 tenant fixture，覆盖 ID guessing、selector/reserved filter、public/permission、cache/task/recovery、vector/keyword、sync/SSE/history/feedback、error 与 coarse timing disclosure。
3. 使用 deterministic test stub，真实 embedding/rerank/generation/judge/provider calls=0；不执行真实 Milvus shadow maintenance。
4. C14 PASS 只允许声明 Milvus 支持配置和固定 synthetic attack matrix 下 evidence 通过，不自动开放第二业务 tenant、tenant management、C15/C16，也不构成生产级多租户或渗透测试结论。

## Current Gate

1. 2026-07-27 用户已验收并批准 proposal、design 的 16 条决策、tasks、`evaluation` 与 `rag-system` spec delta，授权进入 C14 实现。
2. 按 TDD 依次推进 adversarial release contract、隔离 harness、攻击矩阵、disclosure/timing evaluator 与完整门禁。
3. 用户已明确授权 Agent 提交当前 C14 计划内实现与治理文件；push、PR、部署或真实 provider 调用仍未授权。
4. 任何真实 Milvus collection 创建、复制、mapping/readiness 切换、重试或清理仍需另行披露并授权。
5. 若实现发现需要新 API/DTO/schema/权限语义、依赖升级或新增 adapter 支持，必须暂停并回到 OpenSpec 事前闸门。

## Current Implementation Checkpoint

- 已完成 adversarial v1 release/schema/manifest/evidence map、纯标准库 validator/assembler/evaluator、`--plan-only`、四通道聚合与 no-overwrite 输出契约。
- 正式 evidence 绑定 Git HEAD `dc9e3e6ed1434989a646b36389d6d9eeeea4ea83`：26/26 required cases，missing/unexpected/failed/errors/skipped 均为 0，functional/content/error/timing 四通道与 global `Report status` 均为 `PASS`。
- C14 映射 Surefire 71/0/0/0、C14 Failsafe 5/0/0/0、相邻 claim/session/global-security/durable-input/adapter 边界 58/0/0/0、Python 190 tests / OK。全仓 `mvn -q test` 仍只命中既有 OTel collector 时序波动，失败用例独立复跑通过；全仓状态按规则保持非 GREEN。
- provider/model calls=0、businessDataOutbound=false、真实 Milvus maintenance=`SKIPPED`；前端/DTO 无改动，正式 build=`SKIPPED`。
- 实现收口条件已满足；按项目规则仍需用户对最终 evidence 与受限结论明确验收，之后才可接受两个 delta、归档 change 并置 `IDLE`。

## Readiness Basis

- 启动 HEAD：`d5e07b2`；`main` 工作区、暂存区均干净。
- C13b archive files=4、unchecked tasks=0、未归档 active change=0，delta body 是 `rag-system` baseline exact suffix。
- C13b 已验收归档但真实 Milvus maintenance 仍 `SKIPPED`；全仓 Maven 的既有 OTel collector 时序波动仍是独立债务。

## Emergency Rule

如果本文件指向的 change 不存在、已归档、未获批准却进入实现，或与用户当前请求冲突，停止写操作并先修正活动任务指针。
