# Active Task

## Status

`ACTIVE`

## Active Change

- Change：`tenant-isolation-adversarial-evaluation`
- 阶段：C14 实现中
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

- 已完成 adversarial v1 release/schema/manifest、纯标准库 validator、`--plan-only`、四通道 evaluator 与 no-overwrite 输出契约。
- 已建立 `c14-isolation-eval` profile 和首个真实双 tenant HTTP/Milvus/Redis 对抗切片；专用 profile 当前 5 tests / 0 failures / 0 errors / 0 skipped。
- 当前仍未形成 26/26 case-level 正式 evidence：task/history/feedback、reserved filter、cache/idempotency、sync/SSE 与 durable input 的统一 driver/report 映射继续实现中，因此不得宣称 C14 PASS 或进入归档。
- 全仓 `mvn -q test` 仍仅命中既有 OTel collector 时序波动；失败用例独立复跑通过，但全仓状态按规则保持非 GREEN。

## Readiness Basis

- 启动 HEAD：`d5e07b2`；`main` 工作区、暂存区均干净。
- C13b archive files=4、unchecked tasks=0、未归档 active change=0，delta body 是 `rag-system` baseline exact suffix。
- C13b 已验收归档但真实 Milvus maintenance 仍 `SKIPPED`；全仓 Maven 的既有 OTel collector 时序波动仍是独立债务。

## Emergency Rule

如果本文件指向的 change 不存在、已归档、未获批准却进入实现，或与用户当前请求冲突，停止写操作并先修正活动任务指针。
