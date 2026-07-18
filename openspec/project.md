# Enterprise RAG QA System｜项目上下文

## 项目身份

- 企业知识库 RAG 问答工程原型。
- 当前主链路：认证 → 知识库 → 文档异步索引 → 混合检索 → LLM 生成 → 引用校验 → 历史与反馈。
- 当前目标：先完成可信质量工程、生产化基础和个人掌握闭环，再增量进入 MCP 或 Agentic RAG。

## 当前代码事实

- 后端是 Java 17 + Spring Boot 3.2.1 Maven 多模块。
- 前端是 Vue 3 + TypeScript + Vite + Element Plus。
- 默认向量库是 Milvus；另有 Qdrant、Elasticsearch adapter。
- 默认检索是 dense vector + BM25 + RRF。
- 默认 reranker 是 heuristic；HTTP model adapter 已接线但真实收益未验证。
- 默认分块为 `420/80`。
- 同步问答返回答案、contexts、citations、metadata；SSE 当前主要输出文本 chunk。
- 评测集为 30 条开发样本，具备固定 KB、retrieval/generation/citation/no-answer 指标。
- `c3-integration` Maven/Failsafe 入口可用隔离 MySQL、Redis、etcd、MinIO、Milvus 和 test-scope 确定性 embedding 重复验证登录、上传、索引、retrieval 与删除主链路。
- LLM、Redis 与默认 Milvus 的故障语义已分别被测试锁定；Milvus dense route 仅在关键词证据可用时显式降级，mutation outcome unknown、删除和统计均不伪造成功。
- 文档索引已具备 C5a durable input、MySQL durable task ledger 与 C5 恢复债务收口实现：新任务使用稳定 taskId、phase checkpoint 与 deterministic chunk/vector IDs；Redis 是可重建状态投影。legacy 无 ledger 只隔离不合成任务；协调器使用有界 concurrency、持续 heartbeat、DB-time backoff 与 attempt 终态；SQL finalize 以 document row lock 和单一事务保证 chunks/document count/task completion 幂等。provider auto resume 默认继续关闭；本 change 尚待用户验收后接受进 baseline。

## 当前边界

- 不是生产级多租户系统。
- 登录与 refresh 已使用数据库用户、状态和角色；bootstrap 默认关闭，运行时不提供固定默认账号。
- LLM judge 默认关闭，未完成逐 claim faithfulness 结论。
- 真实 model reranker A/B、标题感知长块专项、完整 GenAI 可观测性尚未完成。

## 长期规格

- `openspec/specs/rag-system/spec.md`
- `openspec/specs/evaluation/spec.md`
- `openspec/specs/agent-collaboration/spec.md`

## 参考事实源

- `docs/architecture/overview.md`
- `docs/roadmap/technical-debt.md`
- `docs/optimization/README.md`
- `docs/eval/RAG_EVAL_GUIDE.md`

如果参考文档与当前代码或 accepted spec 冲突，先记录差异并请示，不得自行选择对自己实现最方便的版本。
