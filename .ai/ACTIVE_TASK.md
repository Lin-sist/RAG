# Active Task

## Status

`ACTIVE`

当前活动 change：`2026-07-23-genai-tracing-core`

- 位置：`openspec/changes/2026-07-23-genai-tracing-core/`
- 目标：建立 OTel API/SDK、分离的 ingest/ask trace、稳定 task/document/chunk lineage、上下文/MDC 兼容与进程内验证。
- 范围：proposal/design/tasks/`rag-system` spec delta 事前闸门；获用户批准后再按 Java TDD 实现 tracing core。
- 非目标：C12 exporter/metrics/alerts/deployment/sampling、Java agent、生产 SLA、raw GenAI content、租户隔离、质量门禁激活。
- 提交责任：`用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- 外调边界：真实 embedding/rerank/ask/generation/judge/LLM/provider/exporter 调用与数据出站均未授权；规划阶段保持 0。
- 当前闸门：等待用户审阅 proposal、14 条 design decisions、4 requirements / 12 scenarios，并明确授权新增 OTel 依赖与进入 Java TDD。

## Previous Completed

- Change：`2026-07-23-eval-quality-threshold-gates`
- 位置：`openspec/changes/archive/2026-07-23-eval-quality-threshold-gates/`
- 结果：完成 `rag-quality-gate-profile-v1`、独立离线 evaluator、固定切片、阈值/容差、fail-closed 状态、脱敏 summary 与稳定退出码。
- 验收：用户已验收 offline framework；4 requirements / 12 scenarios 已接受进 `evaluation` baseline。Reference calls 未授权并按 `SKIPPED` 收口，首个 retrieval profile 保持 `DRAFT`，因此不确认 ACTIVE quality gate 或项目质量达标。

## Execution Entry

1. 先审阅并批准 active change 的 proposal、design、tasks 与 spec delta；未批准前不修改 POM、Java、配置或测试。
2. 获得实现授权后，按 `tasks.md` 从 OTel foundation/context/privacy 的 RED 测试开始，一次推进一个可验证切片。
3. C11 不接 network exporter、不新增 metrics/alerts/deployment，不触发真实 provider 或数据出站。
4. 实现完成并经用户验收后，才接受 delta、归档 change 并恢复 `IDLE`。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
