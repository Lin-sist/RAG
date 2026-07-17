# RAG 项目技术债清单

> 状态日期：2026-07-16
> 本文是从旧维护计划和交接材料中提炼、并按当前代码复核后的待办库存。它不是活动任务计划；每次重大改动应进入独立 OpenSpec change，再从本文移除或标记完成。

## P0：进入下一轮功能迭代前

### 1. 前端正式构建（已完成：2026-07-14）

- 结果：`ignoreDeprecations` 已调整为与 TypeScript 5.7.3 兼容的 `"5.0"`，不再触发 `TS5103`。
- 验证：标准 build 中的 `vue-tsc -b` 与 `vite build` 均通过，并生成正式 `dist/`。
- 证据：`rag-frontend/package.json`、`rag-frontend/tsconfig.json`。

### 2. 认证用户与默认凭据治理（已完成：2026-07-15）

- 已实现：`UserDetailsServiceImpl` 改为数据库用户与角色查询，历史固定管理员种子由前向 migration 精确隔离，并提供默认关闭、外部凭据驱动的一次性管理员 bootstrap。
- 验证：MySQL 8.0.36 Testcontainers 覆盖全新 V1→V6、V5 exact seed、changed-admin、重复 migrate 与 Flyway validate；完整 Maven、Python、前端和敏感日志门禁通过。
- 独立剩余债务：`application.yml` 的开发态 JWT fallback 不属于 C2 登录/refresh 契约，后续需单独治理。
- 证据：`openspec/changes/archive/2026-07-14-database-backed-authentication/`、`rag-auth/.../UserDetailsServiceImpl.java`、`rag-admin/src/main/resources/db/migration/V6__quarantine_known_admin_seed.sql`。

### 3. 补真实依赖集成测试（主链路已完成：2026-07-15）

- 已实现：独立 `c3-integration` Maven/Failsafe 入口使用隔离 MySQL、Redis、etcd、MinIO、Milvus 和 test-scope 确定性 embedding，覆盖登录、上传、异步索引、retrieval、删除与资源清理。
- 验证：主链路重复运行通过；完整 Maven 203 tests、默认 Maven 202 tests、Python 33 tests 与 SensitiveLogs 门禁通过，均为 0 failures/errors/skipped。
- 后续进展：LLM、Redis、Milvus 故障语义和 C5a 索引输入持久化均已按独立 OpenSpec change 实现并接受进 baseline；当前剩余债务收敛为 C5b durable task ledger、孤儿协调与安全续跑。
- 证据：`openspec/changes/archive/2026-07-15-integration-test-happy-path/`、`openspec/changes/archive/2026-07-15-llm-provider-resilience/`、`openspec/changes/archive/2026-07-15-redis-failure-semantics/`、`openspec/changes/archive/2026-07-15-milvus-failure-semantics/`、`openspec/changes/archive/2026-07-16-durable-index-inputs/`。

## P1：下一轮 RAG 质量工程

### 1. 真实 reranker A/B

- 适配真实 provider 协议。
- 固定 KB、fixture、配置和 Git HEAD，对比 heuristic/model 的 Recall@5、MRR、Top1、延迟和降级行为。
- 没有 provider/凭据时继续保持默认 heuristic，不用 mock 宣称业务收益。

### 2. 分块结构专项

- 来源：承接已关闭 v4 计划中未执行的 Stage 3；后续须重新分级并独立立项，不从旧 v4 计划续跑。
- 验证标题感知、长代码块、长段落和父子块策略。
- 保持 `420/80` 为稳定基线，只做可回滚的单变量实验。

### 3. Claim-level 引用质量

- 当前 citation snippet 可回连到 retrieved contexts，但还不能证明答案每个 claim 都被证据支持。
- 后续补 claim support rate、人工抽样与可选 LLM judge；judge 失败时必须显式降级。

### 4. SSE 结构化结果

- 当前流式路径只输出文本 chunk，历史保存 citations 为空。
- 需要设计兼容的结构化完成事件，明确 citations、contexts、metadata 和中断语义。

### 5. 可观测性与恢复演练

- 为 embedding、vector search、BM25、RRF、rerank、LLM 和 citation validation 建立统一 trace/metrics。
- LLM 429/503/timeout、Redis/Milvus 不可用语义已完成；继续演练索引输入丢失、进程中断与恢复。

## P2：基线稳定后

- 组织/租户模型与强制 tenant filter。
- 前端统一设计 token、空态/错态/处理中态和可访问性。
- 生产数据评测集扩充与反馈闭环。
- 有界 Query Router：按 fact、multi-hop、global、no-answer 选择策略。
- MCP 只读知识资源和搜索/问答工具。
- Agentic RAG 仅在前述能力有评测门禁后进入。

## 不应重复立项

以下能力已经存在，不应继续以“从零接入”方式创建任务：

- BM25 + dense vector + RRF hybrid retrieval。
- Reranker 接口、heuristic 实现、HTTP model adapter 和失败降级。
- 固定评测 KB、`--preflight-only`、只读 `--keep-existing`。
- citation validation/fallback 与 no-answer 引用抑制。
- generation/citation/no-answer 客观指标通道。

## 已完成的治理基础

- 根目录 `AGENTS.md` 已建立统一协作规则。
- `.ai/ACTIVE_TASK.md` 已作为唯一活动任务指针。
- `.ai/AGENT_LOG.md` 已用于追加执行证据。
- `openspec/` 已包含 project context、baseline specs 和 change 生命周期。
