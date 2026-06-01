# Enterprise RAG QA System 项目审计报告

审计日期：2026-06-01  
审计范围：当前仓库全量源码、配置、测试、脚本、前端联调入口。  
审计原则：第一轮只做理解和记录，不修改业务代码。

## 一、项目真实结构

### 1. 模块与职责

| 模块 | 职责 | 主链路优先级 | 学习建议 |
|---|---|---:|---|
| `rag-admin` | Spring Boot 启动模块；REST API；知识库、文档、任务、问答历史、反馈、权限编排 | 必须优先理解 | 从 Controller 到 Service 的业务主链路从这里进入 |
| `rag-auth` | JWT 登录、刷新、登出、黑名单、用户认证过滤器 | 必须理解 | 先理解 JWT 流程，再补 DB 用户体系 |
| `rag-common` | 通用异常、API 响应、异步任务、限流、幂等、trace、配置 | 必须理解其中主链路部分 | 异步任务、trace、异常处理优先；限流/幂等可后置 |
| `rag-document` | 文档解析、hash、去重、分块、处理流水线 | 必须优先理解 | RAG 数据进入系统的第一段核心逻辑 |
| `rag-core` | Embedding、VectorStore、检索、query variants、rerank、prompt、LLM 生成 | 必须优先理解 | RAG 智能链路核心 |
| `rag-frontend` | Vue 3 前端；登录、知识库、上传、聊天、SSE 消费 | 主链路需要理解 | 先看聊天和上传，设置页和遗留组件可后置 |

### 2. 模块关键 package、类与配置

#### `rag-admin`

关键 package：

- `com.enterprise.rag.admin.controller`
- `com.enterprise.rag.admin.kb.*`
- `com.enterprise.rag.admin.qa.*`
- `com.enterprise.rag.admin.security`
- `com.enterprise.rag.admin.config`

关键类：

- `rag-admin/src/main/java/com/enterprise/rag/RagQaApplication.java`：后端启动入口，`@SpringBootApplication`，`@EnableAsync`。
- `rag-admin/src/main/java/com/enterprise/rag/admin/controller/KnowledgeBaseController.java`：知识库 CRUD、文档上传、文档列表、文档删除、任务查询入口。
- `rag-admin/src/main/java/com/enterprise/rag/admin/controller/QAController.java`：同步问答、SSE 问答、检索调试、历史、反馈入口。
- `rag-admin/src/main/java/com/enterprise/rag/admin/kb/service/impl/DocumentIndexingServiceImpl.java`：文档上传后的索引编排核心。
- `rag-admin/src/main/java/com/enterprise/rag/admin/kb/service/impl/KnowledgeBaseServiceImpl.java`：知识库创建、向量集合创建、统计更新。
- `rag-admin/src/main/java/com/enterprise/rag/admin/kb/service/impl/DocumentServiceImpl.java`：文档与 chunk 持久化、状态更新、删除。
- `rag-admin/src/main/java/com/enterprise/rag/admin/security/AuthorizationService.java`：知识库读写管理权限判断。
- `rag-admin/src/main/java/com/enterprise/rag/admin/security/CurrentUserService.java`：从 Spring Security 上下文提取当前用户。
- `rag-admin/src/main/java/com/enterprise/rag/admin/qa/service/impl/QAHistoryServiceImpl.java`：问答历史保存和分页。
- `rag-admin/src/main/java/com/enterprise/rag/admin/qa/service/impl/QAFeedbackServiceImpl.java`：反馈提交和查询。

关键配置：

- `rag-admin/src/main/resources/application.yml`：DB、Redis、JWT、LLM、Embedding、VectorStore、CORS、日志、代理配置。
- `rag-admin/src/main/resources/db/migration/*.sql`：Flyway 表结构。
- `rag-admin/src/test/resources/application-test.yml`：测试配置，H2、禁用 Flyway。

主链路必须理解：`KnowledgeBaseController`、`QAController`、`DocumentIndexingServiceImpl`、`DocumentServiceImpl`、`AuthorizationService`。  
可后置学习：部分统计接口、OpenAPI 配置、管理端细节。

#### `rag-auth`

关键 package：

- `com.enterprise.rag.auth.config`
- `com.enterprise.rag.auth.filter`
- `com.enterprise.rag.auth.provider`
- `com.enterprise.rag.auth.service`
- `com.enterprise.rag.auth.service.impl`
- `com.enterprise.rag.auth.model`

关键类：

- `rag-auth/src/main/java/com/enterprise/rag/auth/config/SecurityConfig.java`：Spring Security 配置，JWT filter，CORS，权限规则。
- `rag-auth/src/main/java/com/enterprise/rag/auth/filter/JwtAuthenticationFilter.java`：Bearer token 解析、校验、写入 SecurityContext。
- `rag-auth/src/main/java/com/enterprise/rag/auth/provider/JwtTokenProvider.java`：JWT 生成、解析、刷新 token、token hash。
- `rag-auth/src/main/java/com/enterprise/rag/auth/service/impl/AuthServiceImpl.java`：登录、刷新、登出、Redis session。
- `rag-auth/src/main/java/com/enterprise/rag/auth/service/impl/UserDetailsServiceImpl.java`：当前为内存用户初始化。
- `rag-auth/src/main/java/com/enterprise/rag/auth/service/impl/RedisTokenBlacklistService.java`：Redis token 黑名单。

主链路必须理解：`SecurityConfig`、`JwtAuthenticationFilter`、`AuthServiceImpl`、`UserDetailsServiceImpl`。  
可后置学习：自定义异常 handler 的细节。

#### `rag-common`

关键 package：

- `com.enterprise.rag.common.async`
- `com.enterprise.rag.common.async.mq`
- `com.enterprise.rag.common.exception`
- `com.enterprise.rag.common.model`
- `com.enterprise.rag.common.trace`
- `com.enterprise.rag.common.ratelimit`
- `com.enterprise.rag.common.idempotency`
- `com.enterprise.rag.common.config`

关键类：

- `rag-common/src/main/java/com/enterprise/rag/common/async/AsyncTaskManager.java`：异步任务抽象。
- `rag-common/src/main/java/com/enterprise/rag/common/async/RedisAsyncTaskManager.java`：Redis 任务状态实现。
- `rag-common/src/main/java/com/enterprise/rag/common/async/AsyncTask.java`：任务模型。
- `rag-common/src/main/java/com/enterprise/rag/common/async/TaskStatus.java`：任务状态枚举。
- `rag-common/src/main/java/com/enterprise/rag/common/async/mq/LocalDocumentIndexProducer.java`：本地文档索引消息生产者。
- `rag-common/src/main/java/com/enterprise/rag/common/async/mq/DefaultDocumentIndexConsumer.java`：默认文档索引消费者。
- `rag-common/src/main/java/com/enterprise/rag/common/exception/BusinessException.java`：业务异常。
- `rag-common/src/main/java/com/enterprise/rag/common/model/ApiResponse.java`：统一响应。
- `rag-common/src/main/java/com/enterprise/rag/common/trace/TraceFilter.java`：traceId 过滤器。
- `rag-common/src/main/java/com/enterprise/rag/common/trace/RequestObservationFilter.java`：请求观测日志。

主链路必须理解：`RedisAsyncTaskManager`、异常模型、trace。  
可后置学习：幂等、限流、MQ 抽象。

#### `rag-document`

关键 package：

- `com.enterprise.rag.document.processor`
- `com.enterprise.rag.document.parser`
- `com.enterprise.rag.document.chunker`

关键类：

- `rag-document/src/main/java/com/enterprise/rag/document/processor/DocumentProcessor.java`：文档处理抽象。
- `rag-document/src/main/java/com/enterprise/rag/document/processor/DocumentProcessorImpl.java`：解析、hash、内存去重、分块。
- `rag-document/src/main/java/com/enterprise/rag/document/processor/DocumentInput.java`：文档输入模型。
- `rag-document/src/main/java/com/enterprise/rag/document/processor/ProcessResult.java`：处理结果。
- `rag-document/src/main/java/com/enterprise/rag/document/chunker/DocumentChunker.java`：固定、语义、代码分块。
- `rag-document/src/main/java/com/enterprise/rag/document/chunker/ChunkConfig.java`：chunk size、overlap、策略。
- `rag-document/src/main/java/com/enterprise/rag/document/parser/DocumentParserFactory.java`：解析器选择。
- `rag-document/src/main/java/com/enterprise/rag/document/parser/PlainTextParser.java`：纯文本解析。
- `rag-document/src/main/java/com/enterprise/rag/document/parser/MarkdownParser.java`：Markdown 解析。
- `rag-document/src/main/java/com/enterprise/rag/document/parser/PdfParser.java`：PDF 解析。
- `rag-document/src/main/java/com/enterprise/rag/document/parser/WordParser.java`：DOCX 解析。
- `rag-document/src/main/java/com/enterprise/rag/document/parser/CodeParser.java`：代码文件解析。

主链路必须理解：`DocumentProcessorImpl`、`DocumentChunker`、各 Parser。  
可后置学习：代码分块策略优化。

#### `rag-core`

关键 package：

- `com.enterprise.rag.core.embedding`
- `com.enterprise.rag.core.embedding.config`
- `com.enterprise.rag.core.vectorstore`
- `com.enterprise.rag.core.vectorstore.milvus`
- `com.enterprise.rag.core.vectorstore.qdrant`
- `com.enterprise.rag.core.vectorstore.elasticsearch`
- `com.enterprise.rag.core.rag.service`
- `com.enterprise.rag.core.rag.query`
- `com.enterprise.rag.core.rag.generator`
- `com.enterprise.rag.core.rag.prompt`
- `com.enterprise.rag.core.config`

关键类：

- `rag-core/src/main/java/com/enterprise/rag/core/embedding/EmbeddingService.java`：Embedding 抽象。
- `rag-core/src/main/java/com/enterprise/rag/core/embedding/EmbeddingServiceImpl.java`：Embedding 调用、缓存、fallback。
- `rag-core/src/main/java/com/enterprise/rag/core/vectorstore/VectorStore.java`：向量库抽象。
- `rag-core/src/main/java/com/enterprise/rag/core/vectorstore/VectorDocument.java`：向量文档模型。
- `rag-core/src/main/java/com/enterprise/rag/core/vectorstore/SearchResult.java`：检索结果。
- `rag-core/src/main/java/com/enterprise/rag/core/vectorstore/milvus/MilvusVectorStore.java`：Milvus 实现。
- `rag-core/src/main/java/com/enterprise/rag/core/vectorstore/qdrant/QdrantVectorStore.java`：Qdrant 实现。
- `rag-core/src/main/java/com/enterprise/rag/core/vectorstore/elasticsearch/ElasticsearchVectorStore.java`：Elasticsearch 实现。
- `rag-core/src/main/java/com/enterprise/rag/core/rag/service/RAGServiceImpl.java`：同步和流式 RAG 编排。
- `rag-core/src/main/java/com/enterprise/rag/core/rag/query/QueryEngineImpl.java`：query variants、向量检索、启发式 rerank。
- `rag-core/src/main/java/com/enterprise/rag/core/rag/generator/AnswerGeneratorImpl.java`：LLM 调用、流式生成、citation 抽取。
- `rag-core/src/main/java/com/enterprise/rag/core/rag/prompt/PromptBuilder.java`：prompt 构造和上下文预算。

主链路必须理解：`RAGServiceImpl`、`QueryEngineImpl`、`AnswerGeneratorImpl`、`EmbeddingServiceImpl`、`VectorStore`。  
可后置学习：Qdrant、Elasticsearch 的完整生产适配。

#### `rag-frontend`

关键目录：

- `rag-frontend/src/api`
- `rag-frontend/src/components/chat`
- `rag-frontend/src/components/document`
- `rag-frontend/src/components/knowledge-base`
- `rag-frontend/src/composables`
- `rag-frontend/src/router`
- `rag-frontend/src/stores`
- `rag-frontend/src/views`

关键文件：

- `rag-frontend/src/router/index.ts`：路由，当前 `/chat` 使用 `components/chat/ChatPanel.vue`。
- `rag-frontend/src/components/chat/ChatPanel.vue`：主聊天界面，知识库选择，SSE 问答。
- `rag-frontend/src/composables/useSSE.ts`：POST SSE 流读取和 `data:` 行解析。
- `rag-frontend/src/api/qa.ts`：问答、历史、反馈 API。
- `rag-frontend/src/api/knowledge-base.ts`：知识库 API。
- `rag-frontend/src/components/document/DocumentUpload.vue`：文档上传 UI。
- `rag-frontend/src/stores/auth.ts`：认证状态。

主链路必须理解：`ChatPanel.vue`、`useSSE.ts`、上传组件、API 封装。  
可后置学习：`views/chat/ChatView.vue`、`components/chat/RagChatInterface.vue` 等遗留或备用组件。

## 二、端到端主链路审计

### 1. 文档上传链路

入口：

- `rag-admin/src/main/java/com/enterprise/rag/admin/controller/KnowledgeBaseController.java`
- 类：`KnowledgeBaseController`
- 方法：`uploadDocument(Long id, MultipartFile file, String title)`

链路说明：

1. 用户上传文档进入 `KnowledgeBaseController.uploadDocument`。
2. `uploadDocument` 通过 `CurrentUserService.requireUserId` 获取当前用户。
3. `uploadDocument` 通过 `AuthorizationService.requireKnowledgeBaseWriteAccess` 校验知识库写权限。
4. `uploadDocument` 做空文件、空文件名校验。
5. `uploadDocument` 调用 `DocumentIndexingService.submitIndexing(id, uploaderId, file, title)`。
6. `rag-admin/src/main/java/com/enterprise/rag/admin/kb/service/impl/DocumentIndexingServiceImpl.java` 的 `submitIndexing` 负责上传编排：
   - `DocumentInput.extractFileType(originalFilename)` 提取文件类型。
   - `DocumentParserFactory.isSupported(fileType)` 校验解析能力。
   - `createTempFile(file)` 将上传文件保存为临时文件。
   - `DocumentService.create(...)` 创建 `PENDING` 状态文档记录。
   - `AsyncTaskManager.submit("DOCUMENT_INDEX", uploaderId, progress -> doIndex(...))` 创建异步索引任务。
7. 异步任务实现位于：
   - `rag-common/src/main/java/com/enterprise/rag/common/async/RedisAsyncTaskManager.java`
   - 类：`RedisAsyncTaskManager`
   - 方法：`submit`、`executeAsync`
8. 文档正式处理发生在：
   - `rag-admin/src/main/java/com/enterprise/rag/admin/kb/service/impl/DocumentIndexingServiceImpl.java`
   - 类：`DocumentIndexingServiceImpl`
   - 方法：`doIndex`
9. `doIndex` 打开临时文件输入流，构造 `DocumentInput.of(inputStream, fileName, Map.of("kbId", kbId, "documentId", documentId))`。
10. `doIndex` 调用：
    - `rag-document/src/main/java/com/enterprise/rag/document/processor/DocumentProcessorImpl.java`
    - 类：`DocumentProcessorImpl`
    - 方法：`process(DocumentInput input)`
11. `DocumentProcessorImpl.process` 的处理步骤：
    - `DocumentParserFactory.getParser(fileType)` 选择解析器。
    - `parser.parse(input.inputStream())` 解析原始内容。
    - `calculateHash(rawContent)` 对解析后的文本计算 SHA-256 hash。
    - `processedHashes` 按 `kbId` 做内存级去重。
    - `DocumentChunker.chunk(rawContent, ChunkConfig.DEFAULT)` 分块。
12. 解析器位置：
    - `PlainTextParser.parse`
    - `MarkdownParser.parse`
    - `PdfParser.parse`
    - `WordParser.parse`
    - `CodeParser.parse`
13. 分块位置：
    - `rag-document/src/main/java/com/enterprise/rag/document/chunker/DocumentChunker.java`
    - 类：`DocumentChunker`
    - 方法：`chunk`、`semanticChunk`、`fixedSizeChunk`、`codeChunk`
14. 数据库级去重发生在：
    - `DocumentIndexingServiceImpl.doIndex`
    - 方法调用：`DocumentService.getByKnowledgeBaseAndContentHash(kbId, result.contentHash())`
    - 如果同知识库已有完成文档且 chunk 数大于 0，则当前文档直接更新为 `COMPLETED`，复用 chunkCount 和 contentHash。
15. 如果不是重复文档，`doIndex` 调用：
    - `rag-core/src/main/java/com/enterprise/rag/core/embedding/EmbeddingServiceImpl.java`
    - 类：`EmbeddingServiceImpl`
    - 方法：`embedBatch(chunkTexts)`
16. `EmbeddingServiceImpl` 当前支持：
    - OpenAI-compatible provider。
    - Qwen provider。
    - BGE provider。
    - 缓存。
    - fallback 配置，但默认配置中 `rag.embedding.fallback.enabled=false`。
17. 向量写入发生在：
    - `rag-admin/src/main/java/com/enterprise/rag/admin/kb/service/impl/DocumentIndexingServiceImpl.java`
    - 方法：`doIndex`
    - 调用：`vectorStore.upsert(collectionName, vectorDocs)`
18. `VectorStore` 抽象位于：
    - `rag-core/src/main/java/com/enterprise/rag/core/vectorstore/VectorStore.java`
19. Milvus 默认实现位于：
    - `rag-core/src/main/java/com/enterprise/rag/core/vectorstore/milvus/MilvusVectorStore.java`
    - 类：`MilvusVectorStore`
    - 方法：`upsert`
20. 文档 chunk 写数据库：
    - `rag-admin/src/main/java/com/enterprise/rag/admin/kb/service/impl/DocumentServiceImpl.java`
    - 类：`DocumentServiceImpl`
    - 方法：`saveChunks`
21. 任务状态和文档状态更新：
    - `DocumentIndexingServiceImpl.doIndex` 成功时调用 `documentService.updateContentHash`、`documentService.updateChunkCount`、`documentService.updateStatus(documentId, COMPLETED)`。
    - 失败时调用 `documentService.updateStatus(documentId, FAILED)`。
    - `RedisAsyncTaskManager.updateTask` 更新任务 `RUNNING`、`COMPLETED`、`FAILED`、`CANCELLED` 状态。

关键判断：

- 去重不只是内存中存在。`DocumentProcessorImpl` 有内存去重，`DocumentIndexingServiceImpl` 还做了同知识库 DB 级 contentHash 去重。
- 但 `DocumentProcessorImpl` 的内存去重仍有状态漂移风险，服务重启后丢失，并且和 DB 去重职责重复。
- 文档原始文件只保存为处理过程中的临时文件，当前没有看到面向用户下载/回溯的持久化原文件存储。

### 2. 问答链路

入口：

- `rag-admin/src/main/java/com/enterprise/rag/admin/controller/QAController.java`
- 类：`QAController`
- 方法：`ask(AskRequest request)`

链路说明：

1. 用户提问进入 `QAController.ask`。
2. `AskRequest` 字段：
   - `kbId`
   - `question`
   - `topK`
   - `minScore`
   - `filter`
   - `enableCache`
3. `QAController.ask` 通过 `CurrentUserService.requireUserId` 获取当前用户。
4. `QAController.ask` 通过 `AuthorizationService.requireKnowledgeBaseReadAccess` 校验知识库读权限。
5. `QAController.ask` 查询知识库：
   - `KnowledgeBaseService.getById(request.kbId())`
6. `QAController.ask` 封装 core 层请求：
   - `rag-core/src/main/java/com/enterprise/rag/core/rag/model/QARequest.java`
   - 构造字段包括 question、collectionName、topK、minScore、filter、enableCache、stream。
7. `QAController.ask` 调用：
   - `rag-core/src/main/java/com/enterprise/rag/core/rag/service/RAGServiceImpl.java`
   - 类：`RAGServiceImpl`
   - 方法：`ask(QARequest request)`
8. `RAGServiceImpl.ask` 步骤：
   - 校验 question 和 collectionName。
   - 构造 cache key。
   - 如果启用缓存则尝试返回缓存答案。
   - 调用 `QueryEngine.retrieve(...)` 检索上下文。
   - 如果上下文为空且是解释类问题，调用 `retryExplanatoryRetrieval` 放宽阈值重试。
   - 如果仍为空，返回 `noResult`。
   - 调用 `AnswerGenerator.generate(...)` 生成答案。
   - 写入缓存。
9. 检索实现：
   - `rag-core/src/main/java/com/enterprise/rag/core/rag/query/QueryEngineImpl.java`
   - 类：`QueryEngineImpl`
   - 方法：`retrieve`
10. query variants 生成：
    - `QueryEngineImpl.buildQueryVariants`
    - 规则包括原始 query、规范化 query、去除口语噪声、解释型问题变体、同义词扩展。
    - 同义词当前偏工程固定表，如 `jwt`、`oauth`、`sso`、`rbac`、`csrf`、`xss`。
11. 向量检索：
    - `QueryEngineImpl.retrieve` 对每个 variant 调用 `embeddingService.embed(variant.query())`。
    - 再调用 `vectorStore.search(collectionName, embedding, searchOptions)`。
    - 当前核心检索是 dense vector，没有看到 BM25、关键词索引或 hybrid retrieval 主链路。
12. 检索结果合并：
    - `QueryEngineImpl.mergeResults`
    - 优先用 metadata 中 `source` 作为 key，否则用 content。
    - variant score 按权重融合。
13. rerank 当前实现：
    - `QueryEngineImpl.rerank`
    - 启发式 keyword overlap。
    - 最终分数约为 `originalScore * 0.7 + keywordMatchScore * 0.3`。
    - 支持 CJK 2-gram 和 Latin token。
    - 没有 cross-encoder reranker 或 LLM reranker。
14. prompt 构造：
    - `rag-core/src/main/java/com/enterprise/rag/core/rag/prompt/PromptBuilder.java`
    - 类：`PromptBuilder`
    - 方法：`buildOptimized`
    - 默认策略：`PromptStrategy.STRUCTURED`
    - 包含“只基于上下文回答、证据不足就说明无法确定、不要编造、保持同语言”等约束。
15. 答案生成：
    - `rag-core/src/main/java/com/enterprise/rag/core/rag/generator/AnswerGeneratorImpl.java`
    - 类：`AnswerGeneratorImpl`
    - 方法：`generate`
    - 调用 OpenAI-compatible 或 Qwen 接口。
    - 对 429、5xx、timeout、io/connect 异常有 Reactor retry。
16. citations 生成：
    - `AnswerGeneratorImpl.extractCitations`
    - 当前是基于答案词与 context 句子的启发式匹配。
    - citation 不是模型显式输出的结构化证据，也没有二次校验。
17. QA history 保存：
    - `rag-admin/src/main/java/com/enterprise/rag/admin/controller/QAController.java`
    - 类：`QAController`
    - 方法：`ask`
    - 调用 `QAHistoryService.save(SaveQAHistoryRequest.builder()...)`
    - 实现：`rag-admin/src/main/java/com/enterprise/rag/admin/qa/service/impl/QAHistoryServiceImpl.java`
18. feedback 保存：
    - 入口：`QAController.submitFeedback`
    - 请求：`SubmitFeedbackRequest`
    - 实现：`rag-admin/src/main/java/com/enterprise/rag/admin/qa/service/impl/QAFeedbackServiceImpl.java`
    - 方法：`submit`
    - 支持 1-5 分，防重复反馈。

关键判断：

- 同步问答链路已完整打通。
- 检索质量主要依赖 dense vector、简单 query variants、启发式 rerank。
- citations 能展示，但严格来说还不能证明“答案中的每一句都有可靠证据”。

### 3. SSE 流式问答链路

后端入口：

- `rag-admin/src/main/java/com/enterprise/rag/admin/controller/QAController.java`
- 类：`QAController`
- 方法：`askStream(AskRequest request)`
- 路径：`POST /api/qa/ask/stream`
- produces：`text/event-stream`

后端实现方式：

1. Controller 使用 `SseEmitter`，不是直接返回 `Flux<ServerSentEvent<?>>`。
2. `QAController.askStream` 注释说明这样做是为了避免 Spring Security async dispatch 上下文问题。
3. `askStream` 同样进行用户提取、知识库读权限校验、知识库查询。
4. 封装 `QARequest` 时 `stream=true`。
5. 调用：
   - `rag-core/src/main/java/com/enterprise/rag/core/rag/service/RAGServiceImpl.java`
   - 类：`RAGServiceImpl`
   - 方法：`askStream`
6. `RAGServiceImpl.askStream` 先调用 `queryEngine.retrieve` 获取上下文。
7. 如果没有上下文，返回 `Flux.just("抱歉，没有找到...")`。
8. 如果有上下文，调用：
   - `rag-core/src/main/java/com/enterprise/rag/core/rag/generator/AnswerGeneratorImpl.java`
   - 类：`AnswerGeneratorImpl`
   - 方法：`generateStream`
9. `AnswerGeneratorImpl.generateStream` 通过 WebClient 消费外部模型 stream：
   - `generateOpenAIStream`
   - `generateQwenStream`
10. OpenAI-compatible stream 解析 `data:` 行中的 JSON，取 `choices[0].delta.content`。
11. Qwen stream 解析 `data:` 行中的 JSON，取 `output.text`。
12. Controller subscribe 后逐 chunk 调用 `emitter.send(chunk, MediaType.TEXT_PLAIN)`。
13. 正常完成时发送 `[DONE]`。

异常返回方式：

- `QAController.askStream` 的 subscribe error 回调中：
  - 将异常转为客户端消息 `toClientErrorMessage(error)`。
  - 发送 `[ERROR] <message>`。
  - 再发送 `[DONE]`。
  - 调用 `emitter.complete()`。

前端消费：

- `rag-frontend/src/components/chat/ChatPanel.vue`
- 类/组件：`ChatPanel`
- 方法：`simulateStreamResponse`
- 调用：`useSSE().connect('/api/qa/ask/stream', body, onChunk)`

SSE composable：

- `rag-frontend/src/composables/useSSE.ts`
- 方法：`connect`
- 实现细节：
  - 使用 `fetch` POST。
  - 设置 `Accept: text/event-stream`。
  - 自动附带 `Authorization: Bearer <token>`。
  - 读取 `response.body.getReader()`。
  - 使用 `TextDecoder` 逐块解码。
  - 按 `\r?\n` 切行。
  - 只处理 `data:` 开头的行。
  - 收到 `[DONE]` 后结束。
  - 其他内容传给 `onMessage` 追加到 assistant 消息。

关键判断：

- SSE 主链路可用。
- 当前流式答案历史保存时 citations 为空。
- 前端流式回答展示没有 citation 增量或完成后的 citation 回填。
- 错误以 `[ERROR]` 文本块返回，前端目前主要按普通文本/异常处理，错误 UI 还可以更明确。

## 三、当前实现成熟度判断

| 能力 | 当前是否实现 | 关键文件 | 成熟度 1-5 | 问题 | 优先级 |
|---|---|---|---:|---|---|
| 登录认证 | 是 | `rag-auth/.../SecurityConfig.java`、`AuthServiceImpl.java`、`JwtAuthenticationFilter.java` | 3 | JWT/Redis 流程完整，但用户来自内存初始化，不是 DB 用户体系；token hash 偏弱 | P0 |
| 知识库 CRUD | 是 | `KnowledgeBaseController.java`、`KnowledgeBaseServiceImpl.java` | 3 | 创建依赖向量库可用，失败恢复和降级不足 | P1 |
| 文档上传 | 是 | `KnowledgeBaseController.java`、`DocumentIndexingServiceImpl.java` | 3 | 临时文件处理可用，但原文件未持久化；失败后缺少用户可操作重试 | P0 |
| 异步任务 | 是 | `RedisAsyncTaskManager.java` | 3 | 状态在 Redis，取消 future 在内存；内部又使用 `CompletableFuture.supplyAsync`，线程池控制不够清晰 | P0 |
| 文档解析 | 是 | `PlainTextParser.java`、`MarkdownParser.java`、`PdfParser.java`、`WordParser.java`、`CodeParser.java` | 3 | 常见格式支持不错，但解析质量和异常分类还可增强 | P1 |
| 文档分块 | 是 | `DocumentChunker.java`、`ChunkConfig.java` | 3 | 语义分块仍是启发式/字符级，不是 token-aware；缺少父子块和标题层级 | P0 |
| 文档去重 | 部分实现 | `DocumentProcessorImpl.java`、`DocumentIndexingServiceImpl.java`、`DocumentServiceImpl.java` | 3 | 同时存在内存去重和 DB 去重；职责重复；内存状态重启丢失 | P0 |
| Embedding 缓存 | 是 | `EmbeddingServiceImpl.java` | 3 | 有缓存思路，但缺少命中率指标、容量策略和跨实例一致性说明 | P1 |
| Embedding fallback | 部分实现 | `EmbeddingServiceImpl.java`、`application.yml` | 3 | 代码支持 fallback，但默认关闭；provider 健康检查和降级策略不足 | P1 |
| 向量库抽象 | 是 | `VectorStore.java`、`VectorDocument.java`、`SearchOptions.java` | 4 | 抽象清晰，但 filter 语义在不同实现中不完全一致 | P1 |
| Milvus 实现 | 是 | `MilvusVectorStore.java` | 3 | 主实现可用；score 语义、collection load、filter 表达式和错误恢复还需打磨 | P1 |
| Qdrant 实现 | 部分实现 | `QdrantVectorStore.java` | 2 | metadata 以 JSON 字符串存储，但 filter 使用 `metadata.<key>`，过滤可能不生效 | P2 |
| Elasticsearch 实现 | 部分实现 | `ElasticsearchVectorStore.java` | 2 | 主要是 dense vector kNN；自动建集合、mapping、混合检索和测试不足 | P2 |
| 检索 | 是 | `QueryEngineImpl.java` | 3 | dense vector 检索为主，缺少 hybrid retrieval 和可观测指标 | P0 |
| query rewrite / query variants | 是 | `QueryEngineImpl.java` | 3 | 规则型变体可解释，但领域词表硬编码，无法自学习 | P1 |
| rerank | 部分实现 | `QueryEngineImpl.java` | 2 | 目前是关键词启发式，不是 cross-encoder 或学习型 rerank | P0 |
| prompt 构造 | 是 | `PromptBuilder.java` | 3 | 有防幻觉指令和上下文预算，但缺少结构化输出和证据校验闭环 | P0 |
| 同步问答 | 是 | `QAController.java`、`RAGServiceImpl.java` | 3 | 链路完整，异常/空结果可用；质量依赖检索和 citation | P0 |
| SSE 问答 | 是 | `QAController.java`、`AnswerGeneratorImpl.java`、`useSSE.ts` | 3 | 可流式输出，但流式 citations 为空，错误 UI 和中断控制不足 | P1 |
| 引用 citations | 部分实现 | `AnswerGeneratorImpl.java`、`QAHistoryServiceImpl.java` | 2 | 基于启发式匹配，不够可验证；流式历史 citations 为空 | P0 |
| 问答历史 | 是 | `QAHistoryServiceImpl.java`、`QAController.java` | 3 | 可保存和分页；与流式 citation、反馈状态联动还可增强 | P1 |
| feedback | 是 | `QAFeedbackServiceImpl.java`、`QAController.java` | 3 | 支持评分和防重复；未形成评测闭环和质量分析 | P1 |
| 权限隔离 | 部分实现 | `AuthorizationService.java`、`KBPermissionServiceImpl.java` | 3 | 知识库级权限有实现；认证用户不落库使权限体系不够真实 | P0 |
| 多租户隔离 | 部分实现 | `KnowledgeBase.java`、`AuthorizationService.java`、`DocumentIndexingServiceImpl.java` | 2 | 有 owner/public/permission，缺少组织/租户模型、强制 tenant filter 和隔离测试 | P1 |
| 测试覆盖 | 部分实现 | 各模块 `src/test/java` | 3 | 单元/性质测试不少；真实 MySQL/Redis/Milvus/LLM 集成测试缺失 | P0 |
| 可观测性 | 部分实现 | `TraceFilter.java`、`RequestObservationFilter.java`、日志配置 | 2 | 有 traceId 和请求日志，缺少模型/向量库耗时、指标、链路追踪和告警 | P0 |
| 部署脚本 | 部分实现 | `docker-compose.yml`、`start-backend.ps1`、`start_backend.sh` | 3 | docker compose 覆盖基础依赖；本机 Docker 未启动时无法验证；默认密码和 secret 风险 | P1 |
| 前端联调 | 部分实现 | `ChatPanel.vue`、`useSSE.ts`、`DocumentUpload.vue` | 3 | chat/upload 基本联通；部分遗留组件仍是 mock，feedback 和 citations 体验不足 | P1 |

## 四、测试现状

### 1. 所有 test 目录测试类

| 测试类 | 模块 | 测试内容 |
|---|---|---|
| `rag-admin/src/test/java/com/enterprise/rag/RagQaApplicationTests.java` | `rag-admin` | Spring context load，使用 H2、禁用 Flyway；不是完整外部依赖集成测试 |
| `rag-admin/src/test/java/com/enterprise/rag/admin/controller/QAControllerTest.java` | `rag-admin` | 同步问答 query count、SSE query count、debug retrieve 无副作用、文档标题隔离、文档查询失败 fallback、minScore 归一化 |
| `rag-admin/src/test/java/com/enterprise/rag/admin/controller/TaskControllerTest.java` | `rag-admin` | 任务归属校验、取消任务 |
| `rag-admin/src/test/java/com/enterprise/rag/admin/kb/DocumentIndexingServiceImplTest.java` | `rag-admin` | 拒绝 `.doc`，接受 `.docx` 并创建任务 |
| `rag-admin/src/test/java/com/enterprise/rag/admin/kb/KnowledgeBaseServiceImplTest.java` | `rag-admin` | 向量集合创建失败时知识库创建失败 |
| `rag-admin/src/test/java/com/enterprise/rag/admin/kb/KnowledgeBasePropertyTest.java` | `rag-admin` | 知识库 CRUD、文档删除级联向量、私有知识库隔离、统计一致性 |
| `rag-admin/src/test/java/com/enterprise/rag/admin/qa/QAFeedbackServiceImplTest.java` | `rag-admin` | 反馈评分校验、防重复提交 |
| `rag-admin/src/test/java/com/enterprise/rag/admin/qa/QAHistoryPropertyTest.java` | `rag-admin` | 历史字段保存、分页、通过 qaId 查反馈 |
| `rag-admin/src/test/java/com/enterprise/rag/admin/security/AuthorizationServiceTest.java` | `rag-admin` | KB read/write/admin 和 history owner 权限 |
| `rag-admin/src/test/java/com/enterprise/rag/admin/security/CurrentUserServiceTest.java` | `rag-admin` | 当前用户提取、非法 principal 处理 |
| `rag-auth/src/test/java/com/enterprise/rag/auth/provider/JwtTokenProviderPropertyTest.java` | `rag-auth` | JWT roundtrip、access/refresh claims、无效 token |
| `rag-auth/src/test/java/com/enterprise/rag/auth/service/AuthServiceImplTest.java` | `rag-auth` | 登录、刷新、session 校验、登出 |
| `rag-auth/src/test/java/com/enterprise/rag/auth/service/TokenBlacklistServicePropertyTest.java` | `rag-auth` | token blacklist 行为 |
| `rag-common/src/test/java/com/enterprise/rag/common/async/AsyncTaskManagerPropertyTest.java` | `rag-common` | 异步任务状态性质测试，使用 fake manager |
| `rag-common/src/test/java/com/enterprise/rag/common/idempotency/IdempotencyAspectTest.java` | `rag-common` | 幂等切面行为 |
| `rag-common/src/test/java/com/enterprise/rag/common/idempotency/IdempotencyHandlerPropertyTest.java` | `rag-common` | 幂等 handler，Redis 不可用时跳过部分测试 |
| `rag-common/src/test/java/com/enterprise/rag/common/ratelimit/RateLimiterPropertyTest.java` | `rag-common` | 限流性质测试 |
| `rag-common/src/test/java/com/enterprise/rag/common/trace/RequestObservationFilterTest.java` | `rag-common` | 请求观测 filter |
| `rag-common/src/test/java/com/enterprise/rag/common/trace/TraceIdGeneratorPropertyTest.java` | `rag-common` | traceId 生成性质测试 |
| `rag-core/src/test/java/com/enterprise/rag/core/embedding/EmbeddingServicePropertyTest.java` | `rag-core` | Embedding 缓存、fallback、批量处理性质 |
| `rag-core/src/test/java/com/enterprise/rag/core/rag/RAGServiceCacheKeyTest.java` | `rag-core` | RAG cache key |
| `rag-core/src/test/java/com/enterprise/rag/core/rag/RAGServiceImplTest.java` | `rag-core` | RAG no-result、cache、fallback 等 |
| `rag-core/src/test/java/com/enterprise/rag/core/rag/RAGServicePropertyTest.java` | `rag-core` | RAG 服务性质测试 |
| `rag-core/src/test/java/com/enterprise/rag/core/rag/generator/AnswerGeneratorImplSanitizerTest.java` | `rag-core` | answer sanitizer |
| `rag-core/src/test/java/com/enterprise/rag/core/rag/prompt/PromptBuilderTest.java` | `rag-core` | prompt 预算、去重、防幻觉指令 |
| `rag-core/src/test/java/com/enterprise/rag/core/rag/query/QueryEngineImplTest.java` | `rag-core` | query variants、检索、rerank |
| `rag-core/src/test/java/com/enterprise/rag/core/vectorstore/InMemoryVectorStore.java` | `rag-core` | 测试辅助向量库 |
| `rag-core/src/test/java/com/enterprise/rag/core/vectorstore/VectorStorePropertyTest.java` | `rag-core` | VectorStore 行为性质，基于内存实现 |
| `rag-document/src/test/java/com/enterprise/rag/document/DocumentProcessorPropertyTest.java` | `rag-document` | 文档处理、hash、chunk、去重性质 |
| `rag-document/src/test/java/com/enterprise/rag/document/DocumentProcessorScopeTest.java` | `rag-document` | 按 KB scope 去重 |
| `rag-document/src/test/java/com/enterprise/rag/document/MarkdownProcessingRegressionTest.java` | `rag-document` | Markdown 解析回归 |
| `rag-document/src/test/java/com/enterprise/rag/document/chunker/DocumentChunkerRegressionTest.java` | `rag-document` | chunk 边界和 oversized 文本回归 |
| `rag-document/src/test/java/com/enterprise/rag/document/parser/WordParserTest.java` | `rag-document` | DOCX 支持和非法 DOCX 处理 |

### 2. 是否存在集成测试

存在弱集成测试：

- `RagQaApplicationTests` 可以加载 Spring context。

但缺少真正的端到端集成测试：

- 没有实际启动 MySQL。
- 没有实际启动 Redis。
- 没有实际启动 Milvus。
- 没有通过 Testcontainers 覆盖数据库、Redis、向量库。
- 没有真实 LLM/Embedding mock server 集成。

`pom.xml` 中引入了 Testcontainers 依赖，但当前源码中没有看到真正使用 Testcontainers 的测试类。

### 3. 是否存在 RAG 评测集

存在占位评测文件：

- `docs/eval/rag_eval_set.jsonl`
- `docs/eval/RAG_EVAL_GUIDE.md`

当前 `rag_eval_set.jsonl` 包含 10 条占位记录，字段包括：

- `id`
- `question`
- `type`
- `difficulty`
- `expected_keywords`
- `expected_sources`
- `notes`
- `status`

这些记录仍是模板性质，文本中明确提示需要替换为真实问题。

### 4. `test-data` 目录用途

`test-data` 当前包含：

- `test-data/java-interview-guide.md`
- `test-data/rag-technology-guide.md`
- `test-data/springboot-basics.md`
- `test-data/qa-phase0-questions.txt`

判断用途：

- 三个 Markdown 文件适合作为手工上传到知识库的样本文档。
- `qa-phase0-questions.txt` 适合放手工验证问题。
- 这些文件目前没有被自动化测试或评测脚本消费。

### 5. 是否有 questions + expected answers + expected contexts 评测结构

当前没有完整结构。

已有 `question`、`expected_keywords`、`expected_sources`，但缺少：

- `expected_answer`
- `expected_answer_points`
- `expected_contexts`
- `expected_citations`
- `should_refuse` 或 `no_answer_expected`
- 自动评测脚本和指标输出。

建议最小评测集结构：

```jsonl
{"id":"spring-001","question":"Spring Boot 自动配置的核心机制是什么？","expected_answer_points":["条件装配","starter","AutoConfiguration","配置属性绑定"],"expected_contexts":[{"source":"springboot-basics.md","contains":"自动配置"}],"expected_citations":["springboot-basics.md"],"type":"fact","difficulty":"easy","no_answer_expected":false}
{"id":"rag-001","question":"RAG 为什么需要 rerank？","expected_answer_points":["召回结果排序","提高相关性","降低噪声上下文"],"expected_contexts":[{"source":"rag-technology-guide.md","contains":"rerank"}],"expected_citations":["rag-technology-guide.md"],"type":"reasoning","difficulty":"medium","no_answer_expected":false}
{"id":"guard-001","question":"这个知识库里有没有 Python GIL 的详细解释？","expected_answer_points":["知识库没有足够信息","无法确定"],"expected_contexts":[],"expected_citations":[],"type":"no_answer","difficulty":"easy","no_answer_expected":true}
```

最小可落地方案：

1. 先从 `test-data` 三个 Markdown 中各抽 5 个问题，共 15 条。
2. 每条写 `expected_answer_points` 和 `expected_contexts.contains`。
3. 写一个简单评测脚本调用 `/api/qa/debug/retrieve` 和 `/api/qa/ask`。
4. 输出 recall@k、citation hit rate、answer keyword hit rate、no-answer accuracy。

## 五、运行状态检查

### 1. 命令检查结果

| 命令 | 是否成功 | 实际结果 | 失败原因 | 修复建议 |
|---|---:|---|---|---|
| `mvn test` | 成功 | Maven 3.9.9、Java 17.0.12 下通过，reactor 全模块 SUCCESS，总耗时约 51 秒 | 首次在沙箱内因访问 Maven Central 失败，需要网络权限；放开后成功 | CI 中缓存 Maven 依赖；保留当前测试并补真实集成测试 |
| `mvn -pl rag-admin -am test` | 成功 | `rag-admin` 及依赖模块测试通过，总耗时约 1 分 15 秒 | 同样需要 Maven Central 网络访问；Redis 不可用测试会跳过部分场景 | 增加 Testcontainers，避免“本地 Redis 不可用就跳过”的盲区 |
| `mvn -pl rag-admin -am install -DskipTests` | 成功 | 构建成功，总耗时约 12 秒；生成 `rag-admin/target/rag-admin-1.0.0-SNAPSHOT.jar` | 无最终失败；编译中有 Elasticsearch unchecked 警告 | 清理泛型 warning；CI 中单独串行构建，避免并发写本地仓库或 target |
| `docker compose up -d` | 失败 | Docker CLI 可用，但启动失败：`open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified` | Docker Desktop Linux engine 未运行或当前用户无法访问 Docker daemon；还出现 Docker config access denied warning | 启动 Docker Desktop，切到 Linux engine；修复 `C:\Users\Lin\.docker\config.json` 权限；再执行 compose |
| `RagQaApplication.main()` | 部分成功 | JAR 可启动到 Tomcat 初始化，但 Flyway/Hikari 连接 MySQL 失败：`Communications link failure`、`Connection refused localhost:3306` | MySQL 未运行；Docker compose 未成功；应用启动强依赖 DB | 先启动 compose 中 MySQL/Redis/Milvus；确认 `DB_PASSWORD`、`REDIS_PASSWORD` 与 compose 一致 |
| 前端 `npm install` | 未能直接运行 | 当前环境 `npm` 不在 PATH；系统 Node shim 也有 access denied | 不是项目源码错误，是本机 Node/npm 环境问题 | 安装 Node.js/npm 或修复 PATH；也可继续使用已有 `node_modules` |
| 前端 `npm run dev` | 直接 npm 方式不可运行；等价 Vite 可运行 | 使用已有 `node_modules` 和 bundled Node 直接运行 `vite`，Vite 6.4.1 可启动，输出 `http://127.0.0.1:5173/` | `npm` 不可用，但依赖目录已经存在 | 修复 npm 后再用标准命令；当前可用 `node_modules/.bin/vite.cmd --host 127.0.0.1 --port 5173` 验证前端 |

### 2. 运行状态判断

后端代码可以编译和通过测试，但本机完整启动依赖外部基础设施：

- MySQL
- Redis
- Milvus
- 外部 Embedding/LLM API key

当前最大运行阻塞不是 Java 编译错误，而是 Docker daemon 未启动导致 MySQL/Redis/Milvus 没有起来。

### 3. 配置风险

发现敏感信息风险：

- `.env.local` 中存在真实格式的 `NVIDIA_API_KEY`。
- `application.yml` 中存在默认 JWT secret：`change-me-in-production-please-use-at-least-32-bytes`。
- `docker-compose.yml` 中 MySQL/Redis 默认密码为 `123456`。
- 启动脚本会默认使用 `123456` 作为 DB/Redis 密码。

建议：

- 立即将真实 API key 从仓库移除，并轮换密钥。
- `.env.local` 加入 `.gitignore` 并只保留 `.env.example`。
- dev/test/prod profile 分离 secret。
- README 明确本地启动所需 env。

## 六、最重要的技术债

排序原则：优先考虑暑期实习面试可讲性和学习价值，其次考虑生产成熟度。

| 排名 | 问题描述 | 涉及文件 | 为什么重要 | 修复难度 | 学习价值 | 面试价值 | 建议阶段 |
|---:|---|---|---|---|---|---|---|
| 1 | 认证用户来自内存初始化，未接入 DB 用户表 | `UserDetailsServiceImpl.java`、`AuthServiceImpl.java`、`db/migration/*.sql` | 面试官很容易追问“用户和权限是否真实落库”；当前会削弱企业级可信度 | 中 | 高 | 高 | 第 1 阶段 |
| 2 | 敏感配置和默认 secret 风险 | `.env.local`、`application.yml`、`docker-compose.yml`、`start-backend.ps1` | API key、JWT secret、默认密码是安全红线 | 低 | 中 | 高 | 第 1 阶段 |
| 3 | 缺少真实集成测试 | `pom.xml`、各模块 `src/test/java` | 当前单测不少，但没有证明 MySQL/Redis/Milvus/应用链路能一起跑 | 中 | 高 | 高 | 第 1 阶段 |
| 4 | RAG 评测集仍是占位模板 | `docs/eval/rag_eval_set.jsonl`、`RAG_EVAL_GUIDE.md`、`QAController.debugRetrieve` | 没有评测就无法证明优化有效，Agentic RAG 更无从谈起 | 中 | 高 | 高 | 第 1 阶段 |
| 5 | 异步文档任务失败后缺少可重试和断点恢复 | `DocumentIndexingServiceImpl.java`、`RedisAsyncTaskManager.java` | 文档索引是 RAG 数据入口，失败不可恢复会影响用户体验和数据一致性 | 中 | 高 | 高 | 第 1 阶段 |
| 6 | 异步执行器控制不清晰，取消只在内存 future 生效 | `RedisAsyncTaskManager.java`、`AsyncConfig.java` | 多实例部署时取消、恢复、线程隔离都会出问题 | 中 | 高 | 中 | 第 1 阶段 |
| 7 | 检索基本只有 dense vector，没有 hybrid retrieval | `QueryEngineImpl.java`、`VectorStore.java`、各 VectorStore 实现 | RAG 项目面试常问召回策略，只有 dense 对关键词、编号、专有名词不稳 | 中 | 高 | 高 | 第 2 阶段 |
| 8 | rerank 是启发式关键词打分 | `QueryEngineImpl.java` | 可解释但效果有限；面试官问“rerank 用什么模型”时会暴露短板 | 中 | 高 | 高 | 第 2 阶段 |
| 9 | 分块策略过于简单，非 token-aware | `DocumentChunker.java`、`ChunkConfig.java`、`PromptBuilder.java` | chunk 质量直接决定召回质量；字符级分块容易截断语义 | 中 | 高 | 高 | 第 2 阶段 |
| 10 | citations 不是强证据链，流式历史 citations 为空 | `AnswerGeneratorImpl.java`、`QAController.java`、`ChatPanel.vue` | 引用可靠性是 RAG 面试核心问题；当前 citations 偏展示型 | 中 | 高 | 高 | 第 2 阶段 |
| 11 | prompt 有防幻觉指令，但缺少 evidence verifier 和 answer self-check | `PromptBuilder.java`、`AnswerGeneratorImpl.java`、`RAGServiceImpl.java` | 仅靠 prompt 约束不够，无法系统性降低幻觉 | 中 | 高 | 高 | 第 2 阶段 |
| 12 | 外部模型超时、限流、重试有基础，但缺少熔断和 provider 健康状态 | `AnswerGeneratorImpl.java`、`EmbeddingServiceImpl.java`、`application.yml` | 真实系统一定会遇到 429、超时、供应商波动 | 中 | 高 | 中 | 第 1-2 阶段 |
| 13 | 向量库不可用时缺少优雅降级和健康检查 | `KnowledgeBaseServiceImpl.java`、`MilvusVectorStore.java`、`RAGServiceImpl.java` | 当前创建知识库或问答可能直接失败；需要用户可理解的状态 | 中 | 中 | 中 | 第 1-2 阶段 |
| 14 | 可观测性不足，缺少模型/向量库/任务级指标 | `TraceFilter.java`、`RequestObservationFilter.java`、`DocumentIndexingServiceImpl.java`、`RAGServiceImpl.java` | 没有指标就很难定位慢查询、低召回、模型失败 | 中 | 高 | 高 | 第 1 阶段 |
| 15 | 前端存在主链路和遗留 mock 组件并存，feedback/citation 联动不足 | `ChatPanel.vue`、`RagChatInterface.vue`、`ChatView.vue`、`useSSE.ts` | 面试演示时容易出现“界面能聊但质量证据不明显”的问题 | 中 | 中 | 中 | 第 2 阶段 |

特别关注项判断：

- 文档去重：不是只存在内存中；同时有 DB contentHash 去重。但内存去重仍然存在，建议统一为 DB/持久化策略。
- 分块策略：当前相对简单，语义分块是启发式，不是 token-aware，也没有 parent-child chunk。
- 检索：核心链路是 dense vector，没有 hybrid retrieval。
- rerank：当前是启发式，不是模型 reranker。
- prompt：已有防幻觉指令，但没有验证闭环。
- citations：目前不够真实可靠，属于启发式匹配。
- 外部模型：有 retry，但缺少熔断、限流预算、provider health、全链路指标。
- 向量库不可用：缺少优雅降级。
- 任务失败后：状态会失败，但缺少用户级 retry 和幂等恢复。
- traceId/requestId：有基础 trace，但模型、向量库、任务、embedding 级指标不足。
- 权限隔离：知识库级权限有基础，但用户体系未 DB 化，多租户模型不足。
- 硬编码/敏感信息：存在真实 API key 风险、默认 secret、默认弱密码。

## 七、Agentic RAG 升级可行性

结论：当前项目适合渐进式升级到 Agentic RAG，但不建议直接大重构。先把 Baseline RAG 稳住，再建立评测闭环，最后在 `rag-core` 中增量加入 agent 编排层。

### 1. Baseline RAG 稳定化阶段

目标：先让“上传、索引、检索、回答、引用、历史、权限、运行”可靠。

建议任务：

- 将认证用户从内存切到数据库。
- 移除敏感配置和默认生产 secret。
- 补 MySQL/Redis/Milvus 集成测试。
- 为文档索引任务增加 retry、失败原因、重试入口。
- 为 embedding、vector search、LLM call 增加耗时、错误码、traceId。
- 修复 citations 基础可靠性，至少保证 citation source 来自被检索 context。
- 统一文档去重职责，减少内存状态和 DB 状态不一致。

对应核心文件：

- `UserDetailsServiceImpl.java`
- `AuthServiceImpl.java`
- `DocumentIndexingServiceImpl.java`
- `RedisAsyncTaskManager.java`
- `RAGServiceImpl.java`
- `QueryEngineImpl.java`
- `AnswerGeneratorImpl.java`
- `PromptBuilder.java`

### 2. Evaluated RAG 阶段

目标：每次优化都能用数据说明效果，而不是凭感觉。

建议任务：

- 将 `docs/eval/rag_eval_set.jsonl` 替换为真实评测集。
- 增加 eval runner，调用 `/api/qa/debug/retrieve` 和 `/api/qa/ask`。
- 输出 retrieval 指标：
  - recall@k
  - MRR
  - context hit rate
  - no-answer accuracy
- 输出 answer 指标：
  - expected keyword hit rate
  - citation hit rate
  - answer groundedness 人工检查字段
- 做 chunk size、topK、minScore、rerank 参数实验。
- 引入 hybrid retrieval 或 keyword fallback。
- 抽象 reranker，为 cross-encoder 或 LLM rerank 留接口。

对应核心文件：

- `docs/eval/rag_eval_set.jsonl`
- `docs/eval/RAG_EVAL_GUIDE.md`
- `QAController.java`
- `QueryEngineImpl.java`
- `DocumentChunker.java`
- `VectorStore.java`

### 3. Agentic RAG 阶段

目标：在已有 RAGService 旁边增加 agent 编排，而不是推翻现有系统。

建议新增能力：

- `QueryAnalyzer`：判断问题类型、是否需要多步、是否需要历史上下文。
- `Planner`：把复杂问题拆成 retrieval steps。
- `Tool / Retriever abstraction`：把 dense retriever、hybrid retriever、history retriever、metadata filter retriever 封装为工具。
- `Multi-step retrieval`：按计划多次检索。
- `Query decomposition`：复杂问题拆成子问题。
- `Evidence verifier`：检查 retrieved evidence 是否支持候选答案。
- `Answer self-check`：回答后自检是否超出证据。
- `Citation validator`：验证每个 citation 是否真的支持对应句子。
- `Memory / history-aware retrieval`：基于历史问答补全指代。
- `Evaluation loop`：Agentic 策略必须进入评测集对比。

建议包结构：

- `rag-core/src/main/java/com/enterprise/rag/core/rag/agent`
- `rag-core/src/main/java/com/enterprise/rag/core/rag/tool`
- `rag-core/src/main/java/com/enterprise/rag/core/rag/eval`

渐进落地顺序：

1. 先把 `QueryEngine` 包装成 `RetrieverTool`。
2. 增加 `QueryAnalyzer`，只做规则型分类。
3. 增加 `Planner`，先支持单步和两步检索。
4. 增加 `EvidenceVerifier`，先用规则校验 citation source 和 answer keywords。
5. 最后再考虑 LLM planner 和更复杂的 tool calling。

## 八、4 周优化学习计划

### 第 1 周：把项目变成“能稳定演示”的工程项目

| 任务 | 涉及源码文件 | 验收标准 | 你能学到什么 |
|---|---|---|---|
| 敏感配置清理与 profile 梳理 | `.env.local`、`.env.example`、`application.yml`、`start-backend.ps1` | 仓库不再保存真实 API key；本地启动只依赖 `.env.local`；README 能说明 env | Java 项目配置分层、secret 管理、实习项目安全意识 |
| 认证用户接入 DB | `UserDetailsServiceImpl.java`、`AuthServiceImpl.java`、`db/migration/*.sql` | 登录用户来自数据库；默认用户由 migration 或初始化脚本创建；原有 auth 测试通过 | Spring Security、JWT、用户表设计 |
| Docker 启动链路修复 | `docker-compose.yml`、`start-backend.ps1`、`README.md` | `docker compose up -d` 后 MySQL/Redis/Milvus 健康；后端能启动到 ready | 后端本地开发环境、容器化依赖 |
| 文档索引失败可重试 | `DocumentIndexingServiceImpl.java`、`KnowledgeBaseController.java`、`RedisAsyncTaskManager.java` | 失败文档可以点击重试；重试不会重复插入脏 chunk | 异步任务、幂等、失败恢复 |

### 第 2 周：建立 RAG 评测闭环

| 任务 | 涉及源码文件 | 验收标准 | 你能学到什么 |
|---|---|---|---|
| 建真实 eval set | `docs/eval/rag_eval_set.jsonl`、`test-data/*.md` | 至少 15 条真实问题，包含 expected answer points 和 expected contexts | RAG 评测设计、问题分类 |
| 增加检索调试指标 | `QAController.java`、`QueryEngineImpl.java` | `/api/qa/debug/retrieve` 返回 variant、score、source、rank、hit 信息 | 检索可解释性、调参方法 |
| 写 eval runner | 新增 `scripts` 或 `docs/eval` 下脚本 | 一条命令输出 recall@k、context hit、keyword hit、citation hit | 自动化评测、工程化实验 |
| 补集成测试 smoke | `rag-admin/src/test/java`、`pom.xml` | 至少覆盖：创建 KB、上传小文档、查询 task、debug retrieve | Testcontainers、Spring Boot 集成测试 |

### 第 3 周：提升检索和引用质量

| 任务 | 涉及源码文件 | 验收标准 | 你能学到什么 |
|---|---|---|---|
| chunk 策略升级 | `DocumentChunker.java`、`ChunkConfig.java`、`DocumentProcessorPropertyTest.java` | 支持按标题/段落保留 metadata；eval recall 不下降 | 文档结构化、chunk 设计 |
| 增加 hybrid/keyword fallback | `QueryEngineImpl.java`、`VectorStore.java` | 对专有名词、编号、英文缩写问题召回提升；评测有对比 | 混合检索、召回策略 |
| 抽象 reranker | `QueryEngineImpl.java`，新增 `Reranker` 接口 | 现有 heuristic 变成一种实现；后续可替换模型 reranker | 策略模式、RAG ranking |
| citation validator | `AnswerGeneratorImpl.java`、`PromptBuilder.java`、`QAHistoryServiceImpl.java` | citation 必须来自实际 context；无法匹配时不生成假引用 | RAG groundedness、证据链 |

### 第 4 周：做轻量 Agentic RAG 雏形

| 任务 | 涉及源码文件 | 验收标准 | 你能学到什么 |
|---|---|---|---|
| Query Analyzer | 新增 `rag-core/.../rag/agent/QueryAnalyzer.java`，接入 `RAGServiceImpl.java` | 能分类 fact、comparison、multi-hop、no-answer | Agentic RAG 的第一步：理解问题 |
| RetrieverTool 抽象 | 新增 `rag-core/.../rag/tool/RetrieverTool.java`，包装 `QueryEngineImpl.java` | 原同步问答结果不变；agent 层能调用 retriever tool | Tool abstraction、可组合检索 |
| 简单 Planner | 新增 `Planner.java`、`PlanStep.java` | 对 comparison/multi-hop 问题生成 2 个检索步骤 | Query decomposition、multi-step retrieval |
| Evidence verifier 与 self-check | 新增 `EvidenceVerifier.java`，接入 `AnswerGeneratorImpl.java` 或 `RAGServiceImpl.java` | 评测集中 no-answer 和 citation hit 提升；日志可看 verifier 结果 | 幻觉防控、答案校验 |
| 前端展示 evidence | `ChatPanel.vue`、`qa.ts`、`useSSE.ts` | 聊天结果能展开查看 sources、scores、验证状态 | RAG 产品化表达、面试演示能力 |

## 九、最后结论

### 1. 这个项目现在能不能作为暑期实习项目写进简历？

可以写，但要谨慎包装。

现在它已经具备多模块 Spring Boot、JWT、知识库、文档上传、异步索引、Embedding、向量库抽象、Milvus、RAG 问答、SSE 流式输出、历史和反馈这些完整骨架。作为“大二学生的暑期实习项目”，基础已经不错。

但不要现在就写成“生产级 Agentic RAG 系统”。更合理的简历说法是：

> 基于 Spring Boot + Vue3 构建企业知识库 RAG 问答系统，实现文档解析分块、异步向量化索引、Milvus 向量检索、SSE 流式问答、JWT 权限控制，并正在建设评测集与 Agentic RAG 扩展。

### 2. 当前最怕面试官追问什么？

最怕这些问题：

1. 用户和权限是不是数据库真实落库？当前答案会暴露“用户是内存初始化”。
2. citations 怎么保证真的支持答案？当前只是启发式匹配。
3. rerank 用的什么模型？当前不是模型 rerank。
4. 你怎么证明优化后 RAG 效果变好了？当前评测集是占位。
5. Milvus、Redis、MySQL 挂了怎么办？当前降级和恢复还不完整。

### 3. 哪 5 个地方必须优先补？

1. DB 用户认证和权限体系真实落库。
2. 移除真实 API key、默认 JWT secret、默认弱密码。
3. 建真实 RAG eval set 和自动评测 runner。
4. 修复 citations 可靠性，至少做到 citation 必须来自真实检索 context。
5. 文档索引任务增加重试、幂等恢复和可观测指标。

### 4. 哪 3 个地方最适合包装成亮点？

1. 多模块 Java 17 + Spring Boot RAG 后端：上传、解析、分块、向量化、检索、生成、历史完整链路。
2. 向量库抽象：统一 `VectorStore`，支持 Milvus、Qdrant、Elasticsearch 多实现。
3. SSE 流式问答 + 异步索引任务：前后端能展示真实 RAG 产品体验。

### 5. 如果只剩 2 周，应该改什么？

优先改这些：

1. 第 1-2 天：清理 secret、修复 Docker/后端启动说明，保证能本机演示。
2. 第 3-5 天：把用户认证接 DB，解决最大面试风险。
3. 第 6-8 天：做 15 条真实 RAG eval set 和 eval runner。
4. 第 9-11 天：修 citation validator，让引用可信。
5. 第 12-14 天：补文档索引重试和一个端到端 smoke integration test。

直接结论：这个项目底子是能打的，已经不是空壳 demo；但要成为好讲、抗追问的实习项目，最缺的不是再堆功能，而是“真实运行、真实权限、真实评测、真实证据链”这四件事。
