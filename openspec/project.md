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

## 当前边界

- 不是生产级多租户系统。
- 用户仍是内存初始化，不是数据库用户体系。
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
