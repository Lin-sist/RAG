# Active Task

## Status

`ACTIVE`

## Active Change

- Change：`2026-07-15-llm-provider-resilience`
- 位置：`openspec/changes/2026-07-15-llm-provider-resilience/`
- 阶段：C4b 规格已批准，按 TDD 进入实现。

## Objective

锁定 LLM 429、5xx、timeout、network 和非重试型故障的有界重试、同步/SSE 失败结果、诊断安全与 cache/history/query count 副作用；使用本地合成 HTTP server 验证，真实 provider 调用量为 0。

## Scope

- LLM 公共故障契约矩阵、`1+N` 尝试上限和稳定错误分类。
- 同步 HTTP 200 外层兼容 + `metadata.status=error`。
- SSE 首 chunk 前可重试、首 chunk 后不重放。
- generation failure 不写 cache/history，query count 只计一次。
- 安全 diagnostics 与本地故障注入测试。

## Non-goals

- 不处理 Redis/Milvus、索引恢复、跨 provider fallback、熔断器或结构化 SSE。
- 不默认开启 retry，不新增依赖，不修改 DTO/schema/评测指标。
- 未获用户明确批准前不修改生产 Java、配置、测试或评测脚本。

## Approval Gate

用户已明确批准 proposal、design、决策记录与 spec delta，允许进入 TDD 实现。

## Commit Responsibility

`Agent 提交`。用户已授权本 change 计划内文件的本地暂存与中文提交；不包含 push、PR、部署或发布。

## Last Completed

- Change：`2026-07-15-integration-test-happy-path`
- 位置：`openspec/changes/archive/2026-07-15-integration-test-happy-path/`
- 结果：新增独立 C3 Maven/Failsafe 入口，使用隔离 MySQL、Redis、etcd、MinIO、Milvus 和 test-scope 确定性 embedding，真实验证登录、上传、索引、retrieval 与删除主链路。
- 实现提交：`7f94538`。
- 验收：用户已确认通过；本 change 不修改生产契约，因此无 spec delta、未修改 baseline。

## Start A New Material Change

1. 在 `openspec/changes/<change-id>/` 创建 `proposal.md`、`design.md`、`tasks.md` 和需要的 spec delta。
2. 将本文件 `Status` 改为 `ACTIVE`，并填写 change id、目标、范围、非目标和验收入口。
3. 按 `AGENTS.md` 执行并持续更新 tasks 与 `.ai/AGENT_LOG.md`。
4. 完成且验证后，将本文件恢复为 `IDLE`；经用户确认后把 change 移入 `openspec/changes/archive/`。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
