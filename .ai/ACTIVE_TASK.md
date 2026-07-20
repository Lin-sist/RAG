# Active Task

## Status

`ACTIVE`

## Active Change

- Change：`2026-07-20-reranker-ab-evaluation`
- 位置：`openspec/changes/2026-07-20-reranker-ab-evaluation/`
- 类型：Type C 重大变更；当前仅完成事前规划，等待用户审阅与实现授权。
- 目标：固定 heuristic/model 两个 retrieval-only arm 的评测身份、provider 覆盖与样本配对，离线比较 Recall@5、MRR、Top1、rerank/retrieval P50/P95，并对 fallback 或身份漂移明确判为不可比较。
- 范围：Python eval runner、可复现 metadata、sanitized arm manifest、离线 comparator、单元测试、评测指南与经授权后的 C7 evidence。
- 非目标：不修改默认 heuristic，不新增按请求切换 provider 的 API，不修改 Java provider/检索/分块/prompt/citation/no-answer/judge，不扩充评测集，不建立 C10 通用门禁，不调用 ask/judge/LLM。
- 外部调用：规划与离线实现预算为 0；真实 canary/full A/B 必须分别披露调用量、模型、数据出站、限流和费用并取得用户单独授权。
- 提交责任：`用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。
- 验收入口：`proposal.md`、`design.md` 的 15 条决策记录、`tasks.md`、`specs/evaluation/spec.md` 的 4 个 requirements / 11 个 scenarios。

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
