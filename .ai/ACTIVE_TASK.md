# Active Task

## Status

`IDLE`

## Active Change

- 当前无 active change。

## Last Completed

- Change：`2026-07-18-nvidia-reranker-adapter-and-attribution`
- 位置：`openspec/changes/archive/2026-07-18-nvidia-reranker-adapter-and-attribution/`
- 结果：实现独立 NVIDIA `/v1/ranking` adapter、typed rerank outcome、稳定 fallback taxonomy，以及同步 QA、debug retrieval 和评测 runner 的逐样本/聚合归因。
- 验收：用户已确认决策 13；4 个 requirements / 11 个 scenarios 已接受进 `rag-system` baseline，change 已归档。归档后用户另行授权 1 次纯合成 NVIDIA hosted rerank smoke，真实 endpoint/auth/schema 与 adapter 解析均通过；默认 provider 仍保持 heuristic，收益 A/B 留给 C7。

## Start A New Material Change

1. 在 `openspec/changes/<change-id>/` 创建 `proposal.md`、`design.md`、`tasks.md` 和需要的 spec delta。
2. 将本文件 `Status` 改为 `ACTIVE`，并填写 change id、目标、范围、非目标和验收入口。
3. 按 `AGENTS.md` 执行并持续更新 tasks 与 `.ai/AGENT_LOG.md`。
4. 完成且验证后，将本文件恢复为 `IDLE`；经用户确认后把 change 移入 `openspec/changes/archive/`。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
