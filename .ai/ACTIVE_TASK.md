# Active Task

## Status

`ACTIVE`

## Change

- Change：`tenant-data-plane-enforcement`
- 阶段：C13b OpenSpec 规划与事前闸门
- 位置：`openspec/changes/tenant-data-plane-enforcement/`
- 类型：Type C（SQL/API/permission、vector、cache/task/history 跨数据面 tenant enforcement）
- 提交责任：`用户手动提交`；Agent 不暂存、不提交、不 push、不创建 PR、不部署。

## Planning Artifacts

- `proposal.md`：问题、范围、非目标、能力分类、契约、风险与验收证据。
- `design.md`：V11、显式 identity、权限、Milvus-first、legacy vector maintenance/readiness、Redis/task/recovery 与 15 条决策记录。
- `tasks.md`：事前闸门与 migration→SQL/API→task/cache→RAG/vector→maintenance→full gates 的可验证切片。
- `specs/rag-system/spec.md`：6 requirements / 18 scenarios 草案。

## Current Gate

1. 当前只完成规划草案，尚未批准 schema、Java、Redis、Milvus 或 maintenance 实现。
2. 用户需审阅 proposal、15 条 decisions、6/18 spec delta 与 tasks，尤其确认：
   - child business tables 冗余非空 tenantId；
   - 跨 tenant not-found、tenant-local public/permission；
   - Milvus-first，未通过 contract 的 Qdrant/Elasticsearch fail startup；
   - legacy vector 默认关闭的 maintenance audit/backfill 与 READY 门禁；
   - Redis v2 tenant key 冷启动、旧 session 重新登录；
   - C13b 完成仍不代替 C14，不开放第二业务 tenant/C15/C16。
3. 用户明确批准事前闸门后，才从 `tasks.md` 第 1 节 migration RED 开始实现。

## Execution Boundaries

- 不修改 V1-V10、accepted baseline、生产代码、测试、runtime config 或依赖，直到规划获批。
- 不执行真实 Milvus/Qdrant/Elasticsearch maintenance，不调用 embedding/rerank/ask/generation/judge/LLM/provider。
- 不修改 `.env.local`、`application-dev.yml`、`.agents/`、`docs/学习文档/`。
- 不宣称租户隔离成立；C14 通过前不开放第二业务 tenant、MCP 或 Router。

## Emergency Rule

如果本文件指向的 change 不存在、已归档或与用户当前请求冲突，停止写操作并先修正活动任务指针。
