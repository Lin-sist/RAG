# Active Task

## Status

`ACTIVE`

## Active Change

- Change：`2026-07-14-jwt-secret-production-guard`
- 位置：`openspec/changes/2026-07-14-jwt-secret-production-guard/`
- 阶段：C1 实现与本地验证已完成，等待用户验收确认。

## Objective

在精确 active profile `prod` 下，对 JWT secret 的仓库已知默认值、空白与明确误配置执行启动 fail-fast；保留 JJWT 按 UTF-8 bytes 负责算法最小 key-length 的现有职责。

## Scope

- 已批准的 proposal、design、tasks 与 `rag-system` spec delta。
- 在 `rag-auth` 实现 prod secret guard 和聚焦测试。
- 错误与日志不得回显 secret 原文、长度、hash 或内容片段。

## Non-goals

- 不修改认证数据源、用户模型、token API/DTO 或过期策略。
- 不新增依赖、secret manager、部署、轮换或生产凭据。
- 不修改 RAG 检索、生成、引用、拒答、rerank 或评测指标。
- 不修改与 C1 无关的业务代码、配置或测试。

## External Calls

embedding、rerank、judge、ask 预计调用量均为 0；无业务数据出站、无模型、无限流风险、费用为 0。零费用依据是“不发生调用”，不依赖 NVIDIA 免费假设。

## Commit Responsibility

`用户手动提交`。Agent 不暂存、不提交、不 push。

## Acceptance Entry

1. 用户先审阅并批准 `proposal.md`、`design.md` 与 spec delta。
2. 实现阶段按 `tasks.md` 逐切片推进，不越过 C1 边界。
3. 聚焦测试、完整 Maven 测试、文档结构检查和 `git diff --check` 形成验证证据。
4. 完成且经用户确认后恢复为 `IDLE`；用户确认前不归档。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
