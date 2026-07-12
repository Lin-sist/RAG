# RAG 项目技术债清单

> 状态日期：2026-07-12  
> 本文是从旧维护计划和交接材料中提炼、并按当前代码复核后的待办库存。它不是活动任务计划；每次重大改动应进入独立 OpenSpec change，再从本文移除或标记完成。

## P0：进入下一轮功能迭代前

### 1. 修复前端正式构建

- 现状：`npm run build` 执行 `vue-tsc -b` 时因 `ignoreDeprecations: "6.0"` 与 TypeScript 5.7.3 不兼容报 `TS5103`。
- 验收：标准 build 命令通过，不能只运行 `vite build` 绕过类型检查。
- 证据：`rag-frontend/package.json`、`rag-frontend/tsconfig.json`。

### 2. 认证用户与默认凭据治理

- 现状：`UserDetailsServiceImpl` 仍在内存中初始化 `admin/admin123` 与 `user/user123`；`application.yml` 仍有开发态 JWT fallback。
- 目标：数据库用户体系、环境级 bootstrap 管理员、生产启动时拒绝弱默认 secret。
- 证据：`rag-auth/.../UserDetailsServiceImpl.java`、`rag-admin/src/main/resources/application.yml`。

### 3. 补真实依赖集成测试

- 现状：单元和性质测试较完整，但 Redis 不可用时部分性质测试会在测试内部跳过；尚缺 MySQL/Redis/Milvus 联合链路证据。
- 目标：用 Testcontainers 或受控集成环境覆盖登录、上传、索引、检索、删除和故障恢复。

## P1：下一轮 RAG 质量工程

### 1. 真实 reranker A/B

- 适配真实 provider 协议。
- 固定 KB、fixture、配置和 Git HEAD，对比 heuristic/model 的 Recall@5、MRR、Top1、延迟和降级行为。
- 没有 provider/凭据时继续保持默认 heuristic，不用 mock 宣称业务收益。

### 2. 分块结构专项

- 验证标题感知、长代码块、长段落和父子块策略。
- 保持 `420/80` 为稳定基线，只做可回滚的单变量实验。

### 3. Claim-level 引用质量

- 当前 citation snippet 可回连到 retrieved contexts，但还不能证明答案每个 claim 都被证据支持。
- 后续补 claim support rate、人工抽样与可选 LLM judge；judge 失败时必须显式降级。

### 4. SSE 结构化结果

- 当前流式路径只输出文本 chunk，历史保存 citations 为空。
- 需要设计兼容的结构化完成事件，明确 citations、contexts、metadata 和中断语义。

### 5. 可观测性与故障演练

- 为 embedding、vector search、BM25、RRF、rerank、LLM 和 citation validation 建立统一 trace/metrics。
- 演练 LLM 429/503/timeout、Redis/Milvus 不可用、索引任务中断恢复。

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
