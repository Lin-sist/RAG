# RAG 项目当前架构

> 状态日期：2026-07-12  
> 本文只描述当前代码中已确认的结构与能力。阶段指标以 `docs/optimization/` 和 `docs/eval/reports/` 中的当前文件为准。

## 1. 项目定位

本项目是一个模块化企业知识库 RAG 问答系统，覆盖认证、知识库管理、文档异步索引、混合检索、模型生成、引用校验、历史与反馈，并具备可复现评测脚本。

当前适合作为可运行、可评测的工程原型继续演进；尚不能表述为生产级多租户系统或 Agentic RAG 平台。

## 2. 模块职责

| 模块 | 当前职责 | 主要入口 |
|---|---|---|
| `rag-admin` | Spring Boot 启动、REST API、知识库/文档/任务/问答/历史/反馈编排 | `RagQaApplication`、各 Controller、`DocumentIndexingServiceImpl` |
| `rag-auth` | Spring Security、JWT、刷新与注销、Redis token 黑名单 | `SecurityConfig`、`AuthServiceImpl`、`JwtTokenProvider` |
| `rag-common` | 响应与异常、Redis、限流、幂等、异步任务、Trace | `GlobalExceptionHandler`、`RedisAsyncTaskManager`、`RequestObservationFilter` |
| `rag-document` | TXT/Markdown/PDF/Word/代码解析，token 估算和分块 | `DocumentParserFactory`、`DocumentProcessorImpl`、`DocumentChunker` |
| `rag-core` | Embedding、向量库、BM25、RRF、rerank、prompt、生成与引用 | `QueryEngineImpl`、`AnswerGeneratorImpl`、`CitationValidator` |
| `rag-frontend` | Vue 3 管理与问答界面 | `router/index.ts`、`ChatPanel.vue`、知识库与历史页面 |

## 3. 端到端链路

### 3.1 文档索引

```text
上传文档
  -> 创建文档与异步任务
  -> 解析正文
  -> 按运行时配置分块
  -> 生成 Embedding
  -> 写入向量库与 document_chunk
  -> 更新 BM25 关键词索引
  -> 更新任务和文档状态
```

当前默认分块配置为 `chunk-size=420`、`chunk-overlap=80`。默认向量库是 Milvus；代码另有 Qdrant 与 Elasticsearch 适配，但当前正式评测对象是 Milvus。

### 3.2 问答

```text
用户问题
  -> 规则化 query variants
  -> dense vector + BM25 双路召回
  -> RRF 融合
  -> heuristic/model reranker 抽象
  -> prompt 构造与 LLM 生成
  -> citation extraction/fallback/validation
  -> 返回答案、contexts、citations、metadata
  -> 保存历史与反馈
```

当前 hybrid 与 keyword route 默认开启，RRF 参数为 `rrf-k=60`。真实 `ModelReranker` 已有 HTTP adapter、健康检查、超时和 heuristic 降级，但默认仍使用 heuristic，真实 provider 收益尚未验证。

同步问答返回完整答案、contexts、citations 和 metadata。SSE 路径当前使用 `SseEmitter` 输出文本流；流式历史保存的 citations 仍为空，这是已确认的能力边界。

## 4. 质量工程现状

- v3 已完成固定评测 KB、分块矩阵和 reranker adapter 工程接入。
- 当前可靠 retrieval 指标：Recall@5 `68.63%`、MRR `0.7346`、Top1 source accuracy `96.30%`。
- v4 Stage 1 已完成两轮 30 条 CLEAN objective baseline。
- 当前生成侧客观指标覆盖 answer keyword、citation source/snippet、unsupported citation 和 no-answer。
- LLM judge 默认关闭，因此不能宣称已经验证逐 claim faithfulness/relevance。

## 5. 当前边界

- `UserDetailsServiceImpl` 从数据库加载未删除用户及其有效角色；运行时不再初始化固定默认账号。
- 真实 model reranker 尚未完成 A/B。
- 30 条评测集是开发基线，不是生产数据集或论文级基准。
- 标题感知、长代码块和长段落专项仍待验证。
- 当前只有请求日志与诊断字段，尚未形成完整 GenAI trace、指标和告警体系。

## 6. 文档真相源

治理与事实源优先级：

1. `AGENTS.md`：协作和安全规则。
2. `.ai/ACTIVE_TASK.md` 与 active OpenSpec change：当前范围、契约和验收。
3. `openspec/specs/`：已接受的长期能力契约。
4. 当前代码与配置：已实现事实；与 spec 冲突时记录为 gap。
5. `docs/architecture/overview.md`。
6. `docs/optimization/README.md` 及其 v3/v4 子目录。
7. `docs/eval/RAG_EVAL_GUIDE.md` 与当前正式报告。
8. `docs/roadmap/technical-debt.md`。

`docs/optimization/history/` 只保存演进证据，不负责约束当前阶段。
