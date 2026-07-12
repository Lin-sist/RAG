# RAG Eval Report

- Generated at: 2026-06-30T01:19:03.029609+00:00
- Report status: `RETRIEVAL_ONLY`
- askErrors count: `0`
- retrieveErrors count: `0`
- skippedAsk count: `30`
- rateLimitErrors count: `0`
- retry count: `0`
- Metrics safe for comparison: `retrieval metrics only`
- Base URL: `http://localhost:8080`
- Knowledge base ID: `11`
- Eval set: `docs\eval\rag_eval_set.jsonl`
- topK: `5`
- minScore: `0.3`
- enableRerank: `True`
- skipAsk: `True`
- Eval KB name: `codex-stage2-chunk-700-100`
- Eval KB vector collection: `kb_13f94b6625134ddc`
- Eval KB document count: `3`
- Eval KB chunk count: `28`
- Fixture files: `springboot-basics.md sha256=c51df5761d510aa4c8a5fd610c90454b12973e2999138c3b57ba83768a296521; java-interview-guide.md sha256=a33f16e91025e9a8d92274c4221d1c172bb4f68c03790db53b8e20157ef4faa0; rag-technology-guide.md sha256=59ad5d66a59be2ce4e517ca81e37fde06b7673f3da7f798a4c76b01cc6f348a9`
- Config snapshot: `rag-admin\src\main\resources\application.yml sha256=30ad69759a640af8a482da1dc734ddbf92abe8f59f351b83ed5ff297f153501e; rag-core\src\main\java\com\enterprise\rag\core\rag\query\RetrievalProperties.java sha256=f4d49e1ed6eeee0dc5c358df16bec0ec7424dd97b7dd03afeb1ca4f201e391e6; rag-document\src\main\java\com\enterprise\rag\document\chunker\ChunkConfig.java sha256=01b17712c8974055d15a30034e396d5074ba37ad8ea637fa7bca895a0c8a8554`
- Git HEAD: `2ba2970f9488998f69e03b73cd75ae5b55f80620`
- askDelaySeconds: `0.0`
- maxAskRetries: `0`
- retryBackoffSeconds: `0.0`
- Duration: `34.46s`

## Summary Metrics

| Metric | Value |
|---|---:|
| Samples | 30 |
| Answerable samples | 27 |
| No-answer samples | 3 |
| Recall@3 | 43.14% |
| Recall@5 | 47.06% |
| MRR | 0.4352 |
| Top1 source accuracy | 96.30% |
| Ask successful samples | 0 |
| Answerable ask successful samples | 0 |
| No-answer ask successful samples | 0 |
| Answer keyword hit rate on successful ask samples | skipped (0/0) |
| Citation hit rate on successful ask samples | skipped (0/0) |
| Citation source hit rate on successful ask samples | skipped (0/0) |
| Citation snippet hit rate on successful ask samples | skipped (0/0) |
| Unsupported citation count | 0 |
| No-answer citation violation count | 0 |
| No-answer accuracy on successful ask samples | skipped (0/0) |

## Sample Results

| ID | Type | Retrieve | First Match | Ask | Keyword Hit | Citation Source | Citation Snippet | Unsupported Citations | No-answer OK | Errors |
|---|---|---:|---:|---|---:|---:|---:|---:|---:|---|
| fact-001 | fact | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| fact-002 | fact | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| fact-003 | fact | 0/1 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| fact-004 | fact | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| fact-005 | fact | 0/2 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| fact-006 | fact | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| fact-007 | fact | 0/2 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| fact-008 | fact | 0/2 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| fact-009 | fact | 0/2 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| fact-010 | fact | 0/1 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-001 | definition | 1/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-002 | definition | 2/2 | 2 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-003 | definition | 0/2 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-004 | definition | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-005 | definition | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-006 | definition | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-007 | definition | 0/2 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-008 | definition | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| reasoning-001 | reasoning | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| reasoning-002 | reasoning | 0/2 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| reasoning-003 | reasoning | 2/2 | 4 | skipped | skipped | skipped | skipped | 0 | - |  |
| reasoning-004 | reasoning | 1/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| reasoning-005 | reasoning | 0/1 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| reasoning-006 | reasoning | 0/2 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| multi-hop-001 | multi_hop | 0/2 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| multi-hop-002 | multi_hop | 0/2 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| multi-hop-003 | multi_hop | 0/2 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| no-answer-001 | no_answer | - | - | skipped | skipped | skipped | skipped | 0 | - |  |
| no-answer-002 | no_answer | - | - | skipped | skipped | skipped | skipped | 0 | - |  |
| no-answer-003 | no_answer | - | - | skipped | skipped | skipped | skipped | 0 | - |  |

## Field Coverage

- `debug/retrieve` is used for Recall@3, Recall@5, MRR, and Top1 source accuracy.
- `ask` is used for answer keyword hit rate, citation hit rate, and no-answer accuracy.
- When `--skip-ask` is enabled, generation/citation/no-answer metrics are marked as skipped instead of being counted as zero.
- When ask errors occur, generation/citation/no-answer metrics are calculated only on successful ask samples and the report status becomes PARTIAL.
- `queryVariants`, `rank`, `score`, `source`, `documentId`, `chunkId`, `contentPreview`, and `metadata` are expected in debug output.

## Current Limitations

- `contentPreview` is a preview, not the full chunk, so long expected snippets may undercount recall.
- Citation source hit rate checks expected source names in returned citations.
- Citation snippet hit rate verifies each returned citation against the `contexts` returned by `/api/qa/ask` using exact match or token overlap.
- Answer scoring is keyword based and does not use an LLM judge.
- Metrics assume the three `test-data/*.md` files were uploaded with recognizable file names or document titles.

## Failed Retrieval Cases

### fact-003 (fact)

- id: `fact-003`
- type: `fact`
- question: @RestController 等价于哪两个注解的组合？
- expected_sources: `["springboot-basics.md"]`
- expected_contexts.contains: `["@RestController：= @Controller + @ResponseBody"]`
- retrieve hit ratio: `0/1`
- first_match_rank: `-`
- answer:
- expected_keywords: `["@Controller", "@ResponseBody", "@RestController"]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 42,
    "chunkId": "0",
    "contentPreview": "ctuator 健康检查、监控指标 无代码生成 ：不需要 XML 配置 2. 核心注解 2.1 启动类注解 @SpringBootApplication public class Application { public static void main(String[] args) { SpringApplication.run(Application.class, args); } } @SpringBootApplication 是组合注解，包含： @Configuration ：标记为配置类 @EnableAutoConfiguration ：启用自动配置 @ComponentScan...",
    "metadata": {
      "chunkIndex": 1.0,
      "tokenCount": 577.0,
      "headingPath": "",
      "sourceFileName": "springboot-basics.md",
      "documentId": 42.0,
      "kbId": 11.0,
      "startIndex": 201.0,
      "endIndex": 777.0,
      "originalFilename": "springboot-basics.md",
      "fileName": "springboot-basics.md",
      "documentTitle": "springboot-basics.md",
      "title": "springboot-basics.md",
      "retrievalRoutes": [
        "vector"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 2,
    "score": 0.016393441706895828,
    "source": "7cf04af6-bda4-4c04-bb3a-f0c73f7e62d5",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 42,
    "chunkId": "7cf04af6-bda4-4c04-bb3a-f0c73f7e62d5",
    "contentPreview": "ctuator 健康检查、监控指标 无代码生成 ：不需要 XML 配置 2. 核心注解 2.1 启动类注解 @SpringBootApplication public class Application { public static void main(String[] args) { SpringApplication.run(Application.class, args); } } @SpringBootApplication 是组合注解，包含： @Configuration ：标记为配置类 @EnableAutoConfiguration ：启用自动配置 @ComponentScan...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "springboot-basics.md",
      "documentId": 42,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 777,
      "headingPath": "",
      "tokenCount": 577,
      "chunkIndex": 1,
      "title": "springboot-basics.md",
      "startIndex": 201,
      "fileName": "springboot-basics.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "2e66d5ed-9ca1-444d-8d68-8b34e03a26f3",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 42,
    "chunkId": "2e66d5ed-9ca1-444d-8d68-8b34e03a26f3",
    "contentPreview": "essionCreationPolicy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max = 20, messag...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "springboot-basics.md",
      "documentId": 42,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 3754,
      "headingPath": "",
      "tokenCount": 673,
      "chunkIndex": 8,
      "title": "springboot-basics.md",
      "startIndex": 3081,
      "fileName": "springboot-basics.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "98fd53bd-d1e2-4368-870e-577d1e511304",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 42,
    "chunkId": "98fd53bd-d1e2-4368-870e-577d1e511304",
    "contentPreview": "ss AsyncConfig { @Bean public Executor taskExecutor() { ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor(); executor.setCorePoolSize(5); executor.setMaxPoolSize(10); executor.setQueueCapacity(100); executor.setThreadNamePrefix(\"async-\"); executor.initialize(); return executor; } } @Servi...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "springboot-basics.md",
      "documentId": 42,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 4817,
      "headingPath": "",
      "tokenCount": 636,
      "chunkIndex": 10,
      "title": "springboot-basics.md",
      "startIndex": 4181,
      "fileName": "springboot-basics.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "3e4a14f8-51cb-4c6d-b1f5-498a8a950966",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 42,
    "chunkId": "3e4a14f8-51cb-4c6d-b1f5-498a8a950966",
    "contentPreview": "// @Valid 触发校验 } 9. 事务管理 9.1 声明式事务 @Service public class OrderService { @Transactional(rollbackFor = Exception.class) public void createOrder(Order order) { orderRepository.save(order); // 如果发生异常，自动回滚 stockService.deduct(order.getProductId(), order.getQuantity()); } } 9.2 事务传播行为 REQUIRED （默认）：加入当前事务...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "springboot-basics.md",
      "documentId": 42,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 4197,
      "headingPath": "",
      "tokenCount": 514,
      "chunkIndex": 9,
      "title": "springboot-basics.md",
      "startIndex": 3681,
      "fileName": "springboot-basics.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  }
]
```
- returned citations:
```json
[]
```
- citation validation metadata:
```json
{
  "validCitations": null,
  "droppedCitations": null,
  "citationCoverage": null,
  "unsupportedCitationCount": 0
}
```

### fact-005 (fact)

- id: `fact-005`
- type: `fact`
- question: JWT 认证中 accessToken 和 refreshToken 分别有什么作用？
- expected_sources: `["springboot-basics.md"]`
- expected_contexts.contains: `["accessToken：短期令牌", "refreshToken：长期令牌"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer:
- expected_keywords: `["accessToken", "refreshToken", "访问接口", "刷新"]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 42,
    "chunkId": "0",
    "contentPreview": "essionCreationPolicy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max = 20, messag...",
    "metadata": {
      "chunkIndex": 8.0,
      "tokenCount": 673.0,
      "headingPath": "",
      "sourceFileName": "springboot-basics.md",
      "documentId": 42.0,
      "kbId": 11.0,
      "startIndex": 3081.0,
      "endIndex": 3754.0,
      "originalFilename": "springboot-basics.md",
      "fileName": "springboot-basics.md",
      "documentTitle": "springboot-basics.md",
      "title": "springboot-basics.md",
      "retrievalRoutes": [
        "vector"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 2,
    "score": 0.016393441706895828,
    "source": "2e66d5ed-9ca1-444d-8d68-8b34e03a26f3",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 42,
    "chunkId": "2e66d5ed-9ca1-444d-8d68-8b34e03a26f3",
    "contentPreview": "essionCreationPolicy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max = 20, messag...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "springboot-basics.md",
      "documentId": 42,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 3754,
      "headingPath": "",
      "tokenCount": 673,
      "chunkIndex": 8,
      "title": "springboot-basics.md",
      "startIndex": 3081,
      "fileName": "springboot-basics.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.015384615398943424,
    "source": "b6daca3c-9562-4625-bb24-1e75c565b5be",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "b6daca3c-9562-4625-bb24-1e75c565b5be",
    "contentPreview": "索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1550,
      "headingPath": "",
      "tokenCount": 450,
      "chunkIndex": 2,
      "title": "rag-technology-guide.md",
      "startIndex": 1101,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015384615384615385
    }
  },
  {
    "rank": 4,
    "score": 0.016129031777381897,
    "source": "44ba0f47-a0b6-49d8-bab9-c71ea172753e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "44ba0f47-a0b6-49d8-bab9-c71ea172753e",
    "contentPreview": "更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2959,
      "headingPath": "",
      "tokenCount": 679,
      "chunkIndex": 6,
      "title": "rag-technology-guide.md",
      "startIndex": 2280,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "a9ef0078-c45e-48cd-86ad-763f0e314178",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "a9ef0078-c45e-48cd-86ad-763f0e314178",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 684,
      "headingPath": "",
      "tokenCount": 683,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  }
]
```
- returned citations:
```json
[]
```
- citation validation metadata:
```json
{
  "validCitations": null,
  "droppedCitations": null,
  "citationCoverage": null,
  "unsupportedCitationCount": 0
}
```

### fact-007 (fact)

- id: `fact-007`
- type: `fact`
- question: Redis 常见数据类型包括哪些？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["数据类型", "ZSet：排行榜"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer:
- expected_keywords: `["String", "Hash", "List", "Set", "ZSet"]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 43,
    "chunkId": "0",
    "contentPreview": "手动开启 4.2 MyBatis-Plus 优势 ： 内置通用 CRUD 方法 代码生成器 分页插件 乐观锁、逻辑删除支持 5. MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNC...",
    "metadata": {
      "chunkIndex": 3.0,
      "tokenCount": 686.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 43.0,
      "kbId": 11.0,
      "startIndex": 1501.0,
      "endIndex": 2187.0,
      "originalFilename": "java-interview-guide.md",
      "fileName": "java-interview-guide.md",
      "documentTitle": "java-interview-guide.md",
      "title": "java-interview-guide.md",
      "retrievalRoutes": [
        "vector"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 2,
    "score": 0.016393441706895828,
    "source": "62823eec-6888-4ae2-abef-18057c9f266f",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 43,
    "chunkId": "62823eec-6888-4ae2-abef-18057c9f266f",
    "contentPreview": "手动开启 4.2 MyBatis-Plus 优势 ： 内置通用 CRUD 方法 代码生成器 分页插件 乐观锁、逻辑删除支持 5. MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNC...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 43,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 2187,
      "headingPath": "",
      "tokenCount": 686,
      "chunkIndex": 3,
      "title": "java-interview-guide.md",
      "startIndex": 1501,
      "fileName": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "f0c2f64f-2410-4b10-b3f2-f292e39baf6b",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 43,
    "chunkId": "f0c2f64f-2410-4b10-b3f2-f292e39baf6b",
    "contentPreview": "plate 观察者模式 ：Spring 事件监听 策略模式 ：支付方式选择 10. JVM 10.1 内存模型 堆 ：对象实例，GC 主要区域 栈 ：方法调用，局部变量 方法区 ：类信息、常量池 程序计数器 ：当前线程执行的字节码行号 10.2 垃圾回收 GC 算法 ： 标记-清除 复制算法（新生代） 标记-整理（老年代） GC 收集器 ： Serial、Parallel、CMS、G1、ZGC --- 面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationCon...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 43,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 3161,
      "headingPath": "",
      "tokenCount": 561,
      "chunkIndex": 5,
      "title": "java-interview-guide.md",
      "startIndex": 2601,
      "fileName": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "934b1ed8-7db3-4a08-a1e3-859d1bf07692",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 42,
    "chunkId": "934b1ed8-7db3-4a08-a1e3-859d1bf07692",
    "contentPreview": "ponent public class ScheduledTasks { // 每 5 秒执行一次 @Scheduled(fixedRate = 5000) public void reportCurrentTime() { log.info(\"当前时间：{}\", LocalDateTime.now()); } // 每天凌晨 2 点执行 @Scheduled(cron = \"0 0 2 * * ?\") public void cleanupExpiredData() { log.info(\"清理过期数据\"); } } 12. 缓存 12.1 Spring Cache @Configurati...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "springboot-basics.md",
      "documentId": 42,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 5439,
      "headingPath": "",
      "tokenCount": 658,
      "chunkIndex": 11,
      "title": "springboot-basics.md",
      "startIndex": 4781,
      "fileName": "springboot-basics.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "44ba0f47-a0b6-49d8-bab9-c71ea172753e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "44ba0f47-a0b6-49d8-bab9-c71ea172753e",
    "contentPreview": "更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2959,
      "headingPath": "",
      "tokenCount": 679,
      "chunkIndex": 6,
      "title": "rag-technology-guide.md",
      "startIndex": 2280,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  }
]
```
- returned citations:
```json
[]
```
- citation validation metadata:
```json
{
  "validCitations": null,
  "droppedCitations": null,
  "citationCoverage": null,
  "unsupportedCitationCount": 0
}
```

### fact-008 (fact)

- id: `fact-008`
- type: `fact`
- question: RAG 文档中列出的常用向量数据库有哪些？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["常用数据库", "Qdrant：Rust 实现"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer:
- expected_keywords: `["Milvus", "Pinecone", "Weaviate", "Qdrant"]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "a9ef0078-c45e-48cd-86ad-763f0e314178",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "a9ef0078-c45e-48cd-86ad-763f0e314178",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 684,
      "headingPath": "",
      "tokenCount": 683,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 2,
    "score": 0.015625,
    "source": "1e423351-2df1-4829-8186-7344890ec4dd",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "1e423351-2df1-4829-8186-7344890ec4dd",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2333,
      "headingPath": "",
      "tokenCount": 653,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1679,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 3,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "0",
    "contentPreview": "更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文...",
    "metadata": {
      "chunkIndex": 6.0,
      "tokenCount": 679.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44.0,
      "kbId": 11.0,
      "startIndex": 2280.0,
      "endIndex": 2959.0,
      "originalFilename": "rag-technology-guide.md",
      "fileName": "rag-technology-guide.md",
      "documentTitle": "rag-technology-guide.md",
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "vector"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 4,
    "score": 0.016129031777381897,
    "source": "114a46c5-14ad-461e-a8b1-ef2b845dd6b1",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "114a46c5-14ad-461e-a8b1-ef2b845dd6b1",
    "contentPreview": "：调用大模型生成答案 返回结果 ：返回答案和引用来源 3. 核心技术 3.1 Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.5...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1193,
      "headingPath": "",
      "tokenCount": 591,
      "chunkIndex": 1,
      "title": "rag-technology-guide.md",
      "startIndex": 601,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 5,
    "score": 0.01587301678955555,
    "source": "44ba0f47-a0b6-49d8-bab9-c71ea172753e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "44ba0f47-a0b6-49d8-bab9-c71ea172753e",
    "contentPreview": "更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2959,
      "headingPath": "",
      "tokenCount": 679,
      "chunkIndex": 6,
      "title": "rag-technology-guide.md",
      "startIndex": 2280,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  }
]
```
- returned citations:
```json
[]
```
- citation validation metadata:
```json
{
  "validCitations": null,
  "droppedCitations": null,
  "citationCoverage": null,
  "unsupportedCitationCount": 0
}
```

### fact-009 (fact)

- id: `fact-009`
- type: `fact`
- question: RAG 在线问答处理的步骤有哪些？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["问答处理（在线）", "问题向量化"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer:
- expected_keywords: `["问题向量化", "相似度检索", "构建提示词", "LLM 生成"]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "0",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "chunkIndex": 0.0,
      "tokenCount": 683.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44.0,
      "kbId": 11.0,
      "startIndex": 0.0,
      "endIndex": 684.0,
      "originalFilename": "rag-technology-guide.md",
      "fileName": "rag-technology-guide.md",
      "documentTitle": "rag-technology-guide.md",
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "vector"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 2,
    "score": 0.016393441706895828,
    "source": "a9ef0078-c45e-48cd-86ad-763f0e314178",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "a9ef0078-c45e-48cd-86ad-763f0e314178",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 684,
      "headingPath": "",
      "tokenCount": 683,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "733bb127-bd08-4b45-8342-a8ffe61423b4",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "733bb127-bd08-4b45-8342-a8ffe61423b4",
    "contentPreview": "文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈 关键指标 ： 检索召回率：Top-K 包含正确答案的比例 答案准确率：生成答案的正确性 用户满意度：...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": "",
      "tokenCount": 347,
      "chunkIndex": 7,
      "title": "rag-technology-guide.md",
      "startIndex": 2880,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "1e423351-2df1-4829-8186-7344890ec4dd",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "1e423351-2df1-4829-8186-7344890ec4dd",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2333,
      "headingPath": "",
      "tokenCount": 653,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1679,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015384615398943424,
    "source": "44ba0f47-a0b6-49d8-bab9-c71ea172753e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "44ba0f47-a0b6-49d8-bab9-c71ea172753e",
    "contentPreview": "更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2959,
      "headingPath": "",
      "tokenCount": 679,
      "chunkIndex": 6,
      "title": "rag-technology-guide.md",
      "startIndex": 2280,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015384615384615385
    }
  }
]
```
- returned citations:
```json
[]
```
- citation validation metadata:
```json
{
  "validCitations": null,
  "droppedCitations": null,
  "citationCoverage": null,
  "unsupportedCitationCount": 0
}
```

### fact-010 (fact)

- id: `fact-010`
- type: `fact`
- question: MySQL InnoDB 默认使用什么索引结构？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["B+ 树索引：MySQL InnoDB 默认索引结构"]`
- retrieve hit ratio: `0/1`
- first_match_rank: `-`
- answer:
- expected_keywords: `["B+ 树索引", "InnoDB", "默认索引结构"]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 43,
    "chunkId": "0",
    "contentPreview": "手动开启 4.2 MyBatis-Plus 优势 ： 内置通用 CRUD 方法 代码生成器 分页插件 乐观锁、逻辑删除支持 5. MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNC...",
    "metadata": {
      "chunkIndex": 3.0,
      "tokenCount": 686.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 43.0,
      "kbId": 11.0,
      "startIndex": 1501.0,
      "endIndex": 2187.0,
      "originalFilename": "java-interview-guide.md",
      "fileName": "java-interview-guide.md",
      "documentTitle": "java-interview-guide.md",
      "title": "java-interview-guide.md",
      "retrievalRoutes": [
        "vector"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 2,
    "score": 0.016393441706895828,
    "source": "62823eec-6888-4ae2-abef-18057c9f266f",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 43,
    "chunkId": "62823eec-6888-4ae2-abef-18057c9f266f",
    "contentPreview": "手动开启 4.2 MyBatis-Plus 优势 ： 内置通用 CRUD 方法 代码生成器 分页插件 乐观锁、逻辑删除支持 5. MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNC...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 43,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 2187,
      "headingPath": "",
      "tokenCount": 686,
      "chunkIndex": 3,
      "title": "java-interview-guide.md",
      "startIndex": 1501,
      "fileName": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "b6daca3c-9562-4625-bb24-1e75c565b5be",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "b6daca3c-9562-4625-bb24-1e75c565b5be",
    "contentPreview": "索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1550,
      "headingPath": "",
      "tokenCount": 450,
      "chunkIndex": 2,
      "title": "rag-technology-guide.md",
      "startIndex": 1101,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.015625,
    "source": "114a46c5-14ad-461e-a8b1-ef2b845dd6b1",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "114a46c5-14ad-461e-a8b1-ef2b845dd6b1",
    "contentPreview": "：调用大模型生成答案 返回结果 ：返回答案和引用来源 3. 核心技术 3.1 Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.5...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1193,
      "headingPath": "",
      "tokenCount": 591,
      "chunkIndex": 1,
      "title": "rag-technology-guide.md",
      "startIndex": 601,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 5,
    "score": 0.015384615398943424,
    "source": "9bedf9de-67d7-44dd-80c7-0317df98252c",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 43,
    "chunkId": "9bedf9de-67d7-44dd-80c7-0317df98252c",
    "contentPreview": "Around 动态代理 ： JDK 动态代理：基于接口 CGLIB 代理：基于继承 3. Spring Boot 3.1 自动配置原理 @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan 条件注解 ： @ConditionalOnClass：类路径存在某个类 @ConditionalOnMissingBean：容器中不存在某个 Bean @ConditionalOnProperty：配置文件中存在某个属性 3.2 配置文件 application.yml vs applicati...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 43,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 1565,
      "headingPath": "",
      "tokenCount": 664,
      "chunkIndex": 2,
      "title": "java-interview-guide.md",
      "startIndex": 901,
      "fileName": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015384615384615385
    }
  }
]
```
- returned citations:
```json
[]
```
- citation validation metadata:
```json
{
  "validCitations": null,
  "droppedCitations": null,
  "citationCoverage": null,
  "unsupportedCitationCount": 0
}
```

### definition-001 (definition)

- id: `definition-001`
- type: `definition`
- question: 什么是 RAG？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["RAG（Retrieval-Augmented Generation，检索增强生成）", "结合了**信息检索**和**文本生成**"]`
- retrieve hit ratio: `1/2`
- first_match_rank: `1`
- answer:
- expected_keywords: `["Retrieval-Augmented Generation", "检索增强生成", "信息检索", "文本生成"]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "0",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "chunkIndex": 0.0,
      "tokenCount": 683.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44.0,
      "kbId": 11.0,
      "startIndex": 0.0,
      "endIndex": 684.0,
      "originalFilename": "rag-technology-guide.md",
      "fileName": "rag-technology-guide.md",
      "documentTitle": "rag-technology-guide.md",
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "vector"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 2,
    "score": 0.016393441706895828,
    "source": "a9ef0078-c45e-48cd-86ad-763f0e314178",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "a9ef0078-c45e-48cd-86ad-763f0e314178",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 684,
      "headingPath": "",
      "tokenCount": 683,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "b6daca3c-9562-4625-bb24-1e75c565b5be",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "b6daca3c-9562-4625-bb24-1e75c565b5be",
    "contentPreview": "索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1550,
      "headingPath": "",
      "tokenCount": 450,
      "chunkIndex": 2,
      "title": "rag-technology-guide.md",
      "startIndex": 1101,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "1e423351-2df1-4829-8186-7344890ec4dd",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "1e423351-2df1-4829-8186-7344890ec4dd",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2333,
      "headingPath": "",
      "tokenCount": 653,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1679,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "733bb127-bd08-4b45-8342-a8ffe61423b4",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "733bb127-bd08-4b45-8342-a8ffe61423b4",
    "contentPreview": "文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈 关键指标 ： 检索召回率：Top-K 包含正确答案的比例 答案准确率：生成答案的正确性 用户满意度：...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": "",
      "tokenCount": 347,
      "chunkIndex": 7,
      "title": "rag-technology-guide.md",
      "startIndex": 2880,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  }
]
```
- returned citations:
```json
[]
```
- citation validation metadata:
```json
{
  "validCitations": null,
  "droppedCitations": null,
  "citationCoverage": null,
  "unsupportedCitationCount": 0
}
```

### definition-003 (definition)

- id: `definition-003`
- type: `definition`
- question: 什么是向量数据库？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["专门存储和检索向量的数据库", "支持高效的相似度搜索"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer:
- expected_keywords: `["存储", "检索向量", "相似度搜索"]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "0",
    "contentPreview": "更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文...",
    "metadata": {
      "chunkIndex": 6.0,
      "tokenCount": 679.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44.0,
      "kbId": 11.0,
      "startIndex": 2280.0,
      "endIndex": 2959.0,
      "originalFilename": "rag-technology-guide.md",
      "fileName": "rag-technology-guide.md",
      "documentTitle": "rag-technology-guide.md",
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "vector"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 2,
    "score": 0.016393441706895828,
    "source": "a9ef0078-c45e-48cd-86ad-763f0e314178",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "a9ef0078-c45e-48cd-86ad-763f0e314178",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 684,
      "headingPath": "",
      "tokenCount": 683,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "114a46c5-14ad-461e-a8b1-ef2b845dd6b1",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "114a46c5-14ad-461e-a8b1-ef2b845dd6b1",
    "contentPreview": "：调用大模型生成答案 返回结果 ：返回答案和引用来源 3. 核心技术 3.1 Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.5...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1193,
      "headingPath": "",
      "tokenCount": 591,
      "chunkIndex": 1,
      "title": "rag-technology-guide.md",
      "startIndex": 601,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "44ba0f47-a0b6-49d8-bab9-c71ea172753e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "44ba0f47-a0b6-49d8-bab9-c71ea172753e",
    "contentPreview": "更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2959,
      "headingPath": "",
      "tokenCount": 679,
      "chunkIndex": 6,
      "title": "rag-technology-guide.md",
      "startIndex": 2280,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "1e423351-2df1-4829-8186-7344890ec4dd",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "1e423351-2df1-4829-8186-7344890ec4dd",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2333,
      "headingPath": "",
      "tokenCount": 653,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1679,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  }
]
```
- returned citations:
```json
[]
```
- citation validation metadata:
```json
{
  "validCitations": null,
  "droppedCitations": null,
  "citationCoverage": null,
  "unsupportedCitationCount": 0
}
```

### definition-007 (definition)

- id: `definition-007`
- type: `definition`
- question: 什么是缓存穿透？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["缓存穿透：查询不存在的数据", "布隆过滤器、缓存空值"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer:
- expected_keywords: `["缓存穿透", "不存在的数据", "布隆过滤器", "缓存空值"]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 1.0,
    "source": "62823eec-6888-4ae2-abef-18057c9f266f",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 43,
    "chunkId": "62823eec-6888-4ae2-abef-18057c9f266f",
    "contentPreview": "手动开启 4.2 MyBatis-Plus 优势 ： 内置通用 CRUD 方法 代码生成器 分页插件 乐观锁、逻辑删除支持 5. MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNC...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 43,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 2187,
      "headingPath": "",
      "tokenCount": 686,
      "chunkIndex": 3,
      "title": "java-interview-guide.md",
      "startIndex": 1501,
      "fileName": "java-interview-guide.md"
    }
  },
  {
    "rank": 2,
    "score": 0.448484867811203,
    "source": "934b1ed8-7db3-4a08-a1e3-859d1bf07692",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 42,
    "chunkId": "934b1ed8-7db3-4a08-a1e3-859d1bf07692",
    "contentPreview": "ponent public class ScheduledTasks { // 每 5 秒执行一次 @Scheduled(fixedRate = 5000) public void reportCurrentTime() { log.info(\"当前时间：{}\", LocalDateTime.now()); } // 每天凌晨 2 点执行 @Scheduled(cron = \"0 0 2 * * ?\") public void cleanupExpiredData() { log.info(\"清理过期数据\"); } } 12. 缓存 12.1 Spring Cache @Configurati...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "springboot-basics.md",
      "documentId": 42,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 5439,
      "headingPath": "",
      "tokenCount": 658,
      "chunkIndex": 11,
      "title": "springboot-basics.md",
      "startIndex": 4781,
      "fileName": "springboot-basics.md"
    }
  },
  {
    "rank": 3,
    "score": 0.3769789934158325,
    "source": "168826ac-26ad-4eaa-8206-90912ab5bf5e",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 42,
    "chunkId": "168826ac-26ad-4eaa-8206-90912ab5bf5e",
    "contentPreview": "public void deleteById(Long id) { // 删除缓存 } @CachePut(value = \"users\", key = \"#user.id\") public User update(User user) { // 更新缓存 } } 13. 单元测试 13.1 测试示例 @SpringBootTest @AutoConfigureMockMvc class UserControllerTest { @Autowired private MockMvc mockMvc; @MockBean private UserService userService; @Tes...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "springboot-basics.md",
      "documentId": 42,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 5866,
      "headingPath": "",
      "tokenCount": 482,
      "chunkIndex": 12,
      "title": "springboot-basics.md",
      "startIndex": 5381,
      "fileName": "springboot-basics.md"
    }
  },
  {
    "rank": 4,
    "score": 0.36498746275901794,
    "source": "9bedf9de-67d7-44dd-80c7-0317df98252c",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 43,
    "chunkId": "9bedf9de-67d7-44dd-80c7-0317df98252c",
    "contentPreview": "Around 动态代理 ： JDK 动态代理：基于接口 CGLIB 代理：基于继承 3. Spring Boot 3.1 自动配置原理 @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan 条件注解 ： @ConditionalOnClass：类路径存在某个类 @ConditionalOnMissingBean：容器中不存在某个 Bean @ConditionalOnProperty：配置文件中存在某个属性 3.2 配置文件 application.yml vs applicati...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 43,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 1565,
      "headingPath": "",
      "tokenCount": 664,
      "chunkIndex": 2,
      "title": "java-interview-guide.md",
      "startIndex": 901,
      "fileName": "java-interview-guide.md"
    }
  },
  {
    "rank": 5,
    "score": 0.3435048758983612,
    "source": "1e423351-2df1-4829-8186-7344890ec4dd",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "1e423351-2df1-4829-8186-7344890ec4dd",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2333,
      "headingPath": "",
      "tokenCount": 653,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1679,
      "fileName": "rag-technology-guide.md"
    }
  }
]
```
- returned citations:
```json
[]
```
- citation validation metadata:
```json
{
  "validCitations": null,
  "droppedCitations": null,
  "citationCoverage": null,
  "unsupportedCitationCount": 0
}
```

### reasoning-002 (reasoning)

- id: `reasoning-002`
- type: `reasoning`
- question: 为什么 RAG 在知识更新和可解释性上通常比微调更适合企业知识库？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["RAG vs 微调", "知识更新"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer:
- expected_keywords: `["实时更新文档", "成本低", "可追溯来源", "可解释性高"]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "0",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "chunkIndex": 5.0,
      "tokenCount": 653.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44.0,
      "kbId": 11.0,
      "startIndex": 1679.0,
      "endIndex": 2333.0,
      "originalFilename": "rag-technology-guide.md",
      "fileName": "rag-technology-guide.md",
      "documentTitle": "rag-technology-guide.md",
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "vector"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 2,
    "score": 0.016393441706895828,
    "source": "1e423351-2df1-4829-8186-7344890ec4dd",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "1e423351-2df1-4829-8186-7344890ec4dd",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2333,
      "headingPath": "",
      "tokenCount": 653,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1679,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "a9ef0078-c45e-48cd-86ad-763f0e314178",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "a9ef0078-c45e-48cd-86ad-763f0e314178",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 684,
      "headingPath": "",
      "tokenCount": 683,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "44ba0f47-a0b6-49d8-bab9-c71ea172753e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "44ba0f47-a0b6-49d8-bab9-c71ea172753e",
    "contentPreview": "更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2959,
      "headingPath": "",
      "tokenCount": 679,
      "chunkIndex": 6,
      "title": "rag-technology-guide.md",
      "startIndex": 2280,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "b6daca3c-9562-4625-bb24-1e75c565b5be",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "b6daca3c-9562-4625-bb24-1e75c565b5be",
    "contentPreview": "索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1550,
      "headingPath": "",
      "tokenCount": 450,
      "chunkIndex": 2,
      "title": "rag-technology-guide.md",
      "startIndex": 1101,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  }
]
```
- returned citations:
```json
[]
```
- citation validation metadata:
```json
{
  "validCitations": null,
  "droppedCitations": null,
  "citationCoverage": null,
  "unsupportedCitationCount": 0
}
```

### reasoning-004 (reasoning)

- id: `reasoning-004`
- type: `reasoning`
- question: 为什么 Spring 中更推荐构造器注入而不推荐字段注入？
- expected_sources: `["springboot-basics.md", "java-interview-guide.md"]`
- expected_contexts.contains: `["构造器注入（推荐，保证不可变性）", "字段注入（不推荐）"]`
- retrieve hit ratio: `1/2`
- first_match_rank: `1`
- answer:
- expected_keywords: `["构造器注入", "保证不可变性", "字段注入", "难以测试"]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 43,
    "chunkId": "0",
    "contentPreview": "2. Spring 框架 2.1 Spring IOC（控制反转） 依赖注入的三种方式 ： 构造器注入（推荐，保证不可变性） Setter 注入（可选依赖） 字段注入（不推荐，难以测试） Bean 的生命周期 ： 实例化 属性赋值 初始化（@PostConstruct） 使用 销毁（@PreDestroy） 2.2 Spring AOP（面向切面编程） 应用场景 ：事务管理、日志记录、权限校验、性能监控 核心概念 ： Aspect（切面）：横切关注点的模块化 Join Point（连接点）：方法执行点 Pointcut（切入点）：匹配规则 Advice（通知）：@Before、@After、@...",
    "metadata": {
      "chunkIndex": 1.0,
      "tokenCount": 344.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 43.0,
      "kbId": 11.0,
      "startIndex": 601.0,
      "endIndex": 945.0,
      "originalFilename": "java-interview-guide.md",
      "fileName": "java-interview-guide.md",
      "documentTitle": "java-interview-guide.md",
      "title": "java-interview-guide.md",
      "retrievalRoutes": [
        "vector"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 2,
    "score": 0.016393441706895828,
    "source": "c916fa3e-b47b-4456-9f20-125d01d40cd1",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 42,
    "chunkId": "c916fa3e-b47b-4456-9f20-125d01d40cd1",
    "contentPreview": "t driver-class-name: com.mysql.cj.jdbc.Driver redis: host: localhost port: 6379 logging: level: root: INFO com.example: DEBUG 3.2 配置优先级 命令行参数 JNDI 属性 Java 系统属性 环境变量 application.properties/yml 4. 依赖注入 4.1 构造器注入（推荐） @Service @RequiredArgsConstructor public class UserService { private final UserReposit...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "springboot-basics.md",
      "documentId": 42,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 1546,
      "headingPath": "",
      "tokenCount": 665,
      "chunkIndex": 3,
      "title": "springboot-basics.md",
      "startIndex": 881,
      "fileName": "springboot-basics.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "7ad3ab26-1f59-467c-8808-4a0159c1390c",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 43,
    "chunkId": "7ad3ab26-1f59-467c-8808-4a0159c1390c",
    "contentPreview": "2. Spring 框架 2.1 Spring IOC（控制反转） 依赖注入的三种方式 ： 构造器注入（推荐，保证不可变性） Setter 注入（可选依赖） 字段注入（不推荐，难以测试） Bean 的生命周期 ： 实例化 属性赋值 初始化（@PostConstruct） 使用 销毁（@PreDestroy） 2.2 Spring AOP（面向切面编程） 应用场景 ：事务管理、日志记录、权限校验、性能监控 核心概念 ： Aspect（切面）：横切关注点的模块化 Join Point（连接点）：方法执行点 Pointcut（切入点）：匹配规则 Advice（通知）：@Before、@After、@...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 43,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 945,
      "headingPath": "",
      "tokenCount": 344,
      "chunkIndex": 1,
      "title": "java-interview-guide.md",
      "startIndex": 601,
      "fileName": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01515151560306549,
    "source": "44ba0f47-a0b6-49d8-bab9-c71ea172753e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "44ba0f47-a0b6-49d8-bab9-c71ea172753e",
    "contentPreview": "更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2959,
      "headingPath": "",
      "tokenCount": 679,
      "chunkIndex": 6,
      "title": "rag-technology-guide.md",
      "startIndex": 2280,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015151515151515152
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "b6daca3c-9562-4625-bb24-1e75c565b5be",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "b6daca3c-9562-4625-bb24-1e75c565b5be",
    "contentPreview": "索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1550,
      "headingPath": "",
      "tokenCount": 450,
      "chunkIndex": 2,
      "title": "rag-technology-guide.md",
      "startIndex": 1101,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  }
]
```
- returned citations:
```json
[]
```
- citation validation metadata:
```json
{
  "validCitations": null,
  "droppedCitations": null,
  "citationCoverage": null,
  "unsupportedCitationCount": 0
}
```

### reasoning-005 (reasoning)

- id: `reasoning-005`
- type: `reasoning`
- question: 为什么多线程环境下不应该直接使用 HashMap？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["线程不安全，多线程使用 ConcurrentHashMap"]`
- retrieve hit ratio: `0/1`
- first_match_rank: `-`
- answer:
- expected_keywords: `["线程不安全", "ConcurrentHashMap", "多线程"]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 43,
    "chunkId": "0",
    "contentPreview": "Java 后端面试知识点总结 1. Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容...",
    "metadata": {
      "chunkIndex": 0.0,
      "tokenCount": 602.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 43.0,
      "kbId": 11.0,
      "startIndex": 0.0,
      "endIndex": 603.0,
      "originalFilename": "java-interview-guide.md",
      "fileName": "java-interview-guide.md",
      "documentTitle": "java-interview-guide.md",
      "title": "java-interview-guide.md",
      "retrievalRoutes": [
        "vector"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 2,
    "score": 0.016393441706895828,
    "source": "2f28e90e-179a-453b-8468-4b10d82d40f5",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 43,
    "chunkId": "2f28e90e-179a-453b-8468-4b10d82d40f5",
    "contentPreview": "Java 后端面试知识点总结 1. Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 43,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 603,
      "headingPath": "",
      "tokenCount": 602,
      "chunkIndex": 0,
      "title": "java-interview-guide.md",
      "startIndex": 0,
      "fileName": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "b6daca3c-9562-4625-bb24-1e75c565b5be",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "b6daca3c-9562-4625-bb24-1e75c565b5be",
    "contentPreview": "索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1550,
      "headingPath": "",
      "tokenCount": 450,
      "chunkIndex": 2,
      "title": "rag-technology-guide.md",
      "startIndex": 1101,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "f0c2f64f-2410-4b10-b3f2-f292e39baf6b",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 43,
    "chunkId": "f0c2f64f-2410-4b10-b3f2-f292e39baf6b",
    "contentPreview": "plate 观察者模式 ：Spring 事件监听 策略模式 ：支付方式选择 10. JVM 10.1 内存模型 堆 ：对象实例，GC 主要区域 栈 ：方法调用，局部变量 方法区 ：类信息、常量池 程序计数器 ：当前线程执行的字节码行号 10.2 垃圾回收 GC 算法 ： 标记-清除 复制算法（新生代） 标记-整理（老年代） GC 收集器 ： Serial、Parallel、CMS、G1、ZGC --- 面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationCon...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 43,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 3161,
      "headingPath": "",
      "tokenCount": 561,
      "chunkIndex": 5,
      "title": "java-interview-guide.md",
      "startIndex": 2601,
      "fileName": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "114a46c5-14ad-461e-a8b1-ef2b845dd6b1",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "114a46c5-14ad-461e-a8b1-ef2b845dd6b1",
    "contentPreview": "：调用大模型生成答案 返回结果 ：返回答案和引用来源 3. 核心技术 3.1 Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.5...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1193,
      "headingPath": "",
      "tokenCount": 591,
      "chunkIndex": 1,
      "title": "rag-technology-guide.md",
      "startIndex": 601,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  }
]
```
- returned citations:
```json
[]
```
- citation validation metadata:
```json
{
  "validCitations": null,
  "droppedCitations": null,
  "citationCoverage": null,
  "unsupportedCitationCount": 0
}
```

### reasoning-006 (reasoning)

- id: `reasoning-006`
- type: `reasoning`
- question: 为什么 MyBatis 中 #{} 比 ${} 更适合接收用户输入？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["#{} 预编译，防止 SQL 注入", "${} 字符串替换，存在注入风险"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer:
- expected_keywords: `["预编译", "SQL 注入", "字符串替换"]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "b6daca3c-9562-4625-bb24-1e75c565b5be",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "b6daca3c-9562-4625-bb24-1e75c565b5be",
    "contentPreview": "索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1550,
      "headingPath": "",
      "tokenCount": 450,
      "chunkIndex": 2,
      "title": "rag-technology-guide.md",
      "startIndex": 1101,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 2,
    "score": 0.016129031777381897,
    "source": "114a46c5-14ad-461e-a8b1-ef2b845dd6b1",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "114a46c5-14ad-461e-a8b1-ef2b845dd6b1",
    "contentPreview": "：调用大模型生成答案 返回结果 ：返回答案和引用来源 3. 核心技术 3.1 Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.5...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1193,
      "headingPath": "",
      "tokenCount": 591,
      "chunkIndex": 1,
      "title": "rag-technology-guide.md",
      "startIndex": 601,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 3,
    "score": 0.015625,
    "source": "44ba0f47-a0b6-49d8-bab9-c71ea172753e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "44ba0f47-a0b6-49d8-bab9-c71ea172753e",
    "contentPreview": "更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2959,
      "headingPath": "",
      "tokenCount": 679,
      "chunkIndex": 6,
      "title": "rag-technology-guide.md",
      "startIndex": 2280,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 4,
    "score": 0.015384615398943424,
    "source": "1e423351-2df1-4829-8186-7344890ec4dd",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "1e423351-2df1-4829-8186-7344890ec4dd",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2333,
      "headingPath": "",
      "tokenCount": 653,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1679,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015384615384615385
    }
  },
  {
    "rank": 5,
    "score": 0.014925372786819935,
    "source": "a9ef0078-c45e-48cd-86ad-763f0e314178",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "a9ef0078-c45e-48cd-86ad-763f0e314178",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 684,
      "headingPath": "",
      "tokenCount": 683,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.014925373134328358
    }
  }
]
```
- returned citations:
```json
[]
```
- citation validation metadata:
```json
{
  "validCitations": null,
  "droppedCitations": null,
  "citationCoverage": null,
  "unsupportedCitationCount": 0
}
```

### multi-hop-001 (multi_hop)

- id: `multi-hop-001`
- type: `multi_hop`
- question: 请把 RAG 的离线文档处理和在线问答处理串成一条完整链路。
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["文档处理（离线）", "问答处理（在线）"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer:
- expected_keywords: `["文档上传", "文档解析", "文本分块", "向量化", "相似度检索", "Prompt", "LLM"]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "0",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "chunkIndex": 0.0,
      "tokenCount": 683.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44.0,
      "kbId": 11.0,
      "startIndex": 0.0,
      "endIndex": 684.0,
      "originalFilename": "rag-technology-guide.md",
      "fileName": "rag-technology-guide.md",
      "documentTitle": "rag-technology-guide.md",
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "vector"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 2,
    "score": 0.016393441706895828,
    "source": "a9ef0078-c45e-48cd-86ad-763f0e314178",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "a9ef0078-c45e-48cd-86ad-763f0e314178",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 684,
      "headingPath": "",
      "tokenCount": 683,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "733bb127-bd08-4b45-8342-a8ffe61423b4",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "733bb127-bd08-4b45-8342-a8ffe61423b4",
    "contentPreview": "文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈 关键指标 ： 检索召回率：Top-K 包含正确答案的比例 答案准确率：生成答案的正确性 用户满意度：...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": "",
      "tokenCount": 347,
      "chunkIndex": 7,
      "title": "rag-technology-guide.md",
      "startIndex": 2880,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "44ba0f47-a0b6-49d8-bab9-c71ea172753e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "44ba0f47-a0b6-49d8-bab9-c71ea172753e",
    "contentPreview": "更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2959,
      "headingPath": "",
      "tokenCount": 679,
      "chunkIndex": 6,
      "title": "rag-technology-guide.md",
      "startIndex": 2280,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "1e423351-2df1-4829-8186-7344890ec4dd",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "1e423351-2df1-4829-8186-7344890ec4dd",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2333,
      "headingPath": "",
      "tokenCount": 653,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1679,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  }
]
```
- returned citations:
```json
[]
```
- citation validation metadata:
```json
{
  "validCitations": null,
  "droppedCitations": null,
  "citationCoverage": null,
  "unsupportedCitationCount": 0
}
```

### multi-hop-002 (multi_hop)

- id: `multi-hop-002`
- type: `multi_hop`
- question: 如果要提高 RAG 准确率，可以从检索、分块和 Prompt 三方面做哪些事？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["如何提高 RAG 准确率？", "优化 Prompt"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer:
- expected_keywords: `["优化分块策略", "提升 Embedding 质量", "增加 Top-K", "使用 Rerank", "优化 Prompt"]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "0",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "chunkIndex": 5.0,
      "tokenCount": 653.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44.0,
      "kbId": 11.0,
      "startIndex": 1679.0,
      "endIndex": 2333.0,
      "originalFilename": "rag-technology-guide.md",
      "fileName": "rag-technology-guide.md",
      "documentTitle": "rag-technology-guide.md",
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "vector"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 2,
    "score": 0.016393441706895828,
    "source": "1e423351-2df1-4829-8186-7344890ec4dd",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "1e423351-2df1-4829-8186-7344890ec4dd",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2333,
      "headingPath": "",
      "tokenCount": 653,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1679,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "733bb127-bd08-4b45-8342-a8ffe61423b4",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "733bb127-bd08-4b45-8342-a8ffe61423b4",
    "contentPreview": "文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈 关键指标 ： 检索召回率：Top-K 包含正确答案的比例 答案准确率：生成答案的正确性 用户满意度：...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": "",
      "tokenCount": 347,
      "chunkIndex": 7,
      "title": "rag-technology-guide.md",
      "startIndex": 2880,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "b6daca3c-9562-4625-bb24-1e75c565b5be",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "b6daca3c-9562-4625-bb24-1e75c565b5be",
    "contentPreview": "索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1550,
      "headingPath": "",
      "tokenCount": 450,
      "chunkIndex": 2,
      "title": "rag-technology-guide.md",
      "startIndex": 1101,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "a9ef0078-c45e-48cd-86ad-763f0e314178",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "a9ef0078-c45e-48cd-86ad-763f0e314178",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 684,
      "headingPath": "",
      "tokenCount": 683,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  }
]
```
- returned citations:
```json
[]
```
- citation validation metadata:
```json
{
  "validCitations": null,
  "droppedCitations": null,
  "citationCoverage": null,
  "unsupportedCitationCount": 0
}
```

### multi-hop-003 (multi_hop)

- id: `multi-hop-003`
- type: `multi_hop`
- question: 如果接口性能优化选择缓存，Java 面试文档和 Spring Boot 文档分别给了哪些相关手段？
- expected_sources: `["java-interview-guide.md", "springboot-basics.md"]`
- expected_contexts.contains: `["接口性能优化手段", "Spring Cache"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer:
- expected_keywords: `["Redis", "使用缓存", "@Cacheable", "@CacheEvict", "@CachePut"]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "f0c2f64f-2410-4b10-b3f2-f292e39baf6b",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 43,
    "chunkId": "f0c2f64f-2410-4b10-b3f2-f292e39baf6b",
    "contentPreview": "plate 观察者模式 ：Spring 事件监听 策略模式 ：支付方式选择 10. JVM 10.1 内存模型 堆 ：对象实例，GC 主要区域 栈 ：方法调用，局部变量 方法区 ：类信息、常量池 程序计数器 ：当前线程执行的字节码行号 10.2 垃圾回收 GC 算法 ： 标记-清除 复制算法（新生代） 标记-整理（老年代） GC 收集器 ： Serial、Parallel、CMS、G1、ZGC --- 面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationCon...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 43,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 3161,
      "headingPath": "",
      "tokenCount": 561,
      "chunkIndex": 5,
      "title": "java-interview-guide.md",
      "startIndex": 2601,
      "fileName": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 2,
    "score": 0.016129031777381897,
    "source": "44ba0f47-a0b6-49d8-bab9-c71ea172753e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "44ba0f47-a0b6-49d8-bab9-c71ea172753e",
    "contentPreview": "更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2959,
      "headingPath": "",
      "tokenCount": 679,
      "chunkIndex": 6,
      "title": "rag-technology-guide.md",
      "startIndex": 2280,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 3,
    "score": 0.01587301678955555,
    "source": "1e423351-2df1-4829-8186-7344890ec4dd",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "1e423351-2df1-4829-8186-7344890ec4dd",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2333,
      "headingPath": "",
      "tokenCount": 653,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1679,
      "fileName": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 4,
    "score": 0.01515151560306549,
    "source": "9bedf9de-67d7-44dd-80c7-0317df98252c",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 43,
    "chunkId": "9bedf9de-67d7-44dd-80c7-0317df98252c",
    "contentPreview": "Around 动态代理 ： JDK 动态代理：基于接口 CGLIB 代理：基于继承 3. Spring Boot 3.1 自动配置原理 @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan 条件注解 ： @ConditionalOnClass：类路径存在某个类 @ConditionalOnMissingBean：容器中不存在某个 Bean @ConditionalOnProperty：配置文件中存在某个属性 3.2 配置文件 application.yml vs applicati...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 43,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 1565,
      "headingPath": "",
      "tokenCount": 664,
      "chunkIndex": 2,
      "title": "java-interview-guide.md",
      "startIndex": 901,
      "fileName": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015151515151515152
    }
  },
  {
    "rank": 5,
    "score": 0.014492753893136978,
    "source": "992e271e-ea2e-4ecc-9fa5-1454f6e4561d",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 43,
    "chunkId": "992e271e-ea2e-4ecc-9fa5-1454f6e4561d",
    "contentPreview": "ey 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好 7. 分布式 7.1 分布式锁 实现方式 ： Redis SETNX + 过期时间 Redisson（推荐） Zookeeper 注意事项 ： 防止死锁（设置过期时间） 防止误删（UUID 标识） 原子性（Lua 脚本） 7.2 分布式事务 2PC（两阶段提交） ： 准备阶段、提交阶段 缺点：阻塞、单点故障 TCC（Try-Confirm-Cancel） ： 业务侵入性强 性能较好 Seata ：阿里开源分布式事务框架 8. 消...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 43,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 2642,
      "headingPath": "",
      "tokenCount": 541,
      "chunkIndex": 4,
      "title": "java-interview-guide.md",
      "startIndex": 2101,
      "fileName": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.014492753623188406
    }
  }
]
```
- returned citations:
```json
[]
```
- citation validation metadata:
```json
{
  "validCitations": null,
  "droppedCitations": null,
  "citationCoverage": null,
  "unsupportedCitationCount": 0
}
```

## Failed Citation Cases

No cases.

## Low Answer Keyword Hit Cases

No cases.

## No-answer Cases

### no-answer-001 (no_answer)

- id: `no-answer-001`
- type: `no_answer`
- question: Kubernetes 的 livenessProbe 和 readinessProbe 有什么区别？
- expected_sources: `[]`
- expected_contexts.contains: `[]`
- retrieve hit ratio: `0/0`
- first_match_rank: `-`
- answer:
- expected_keywords: `[]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 1.0,
    "source": "a9ef0078-c45e-48cd-86ad-763f0e314178",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "a9ef0078-c45e-48cd-86ad-763f0e314178",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 684,
      "headingPath": "",
      "tokenCount": 683,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md"
    }
  },
  {
    "rank": 2,
    "score": 0.8368229269981384,
    "source": "44ba0f47-a0b6-49d8-bab9-c71ea172753e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "44ba0f47-a0b6-49d8-bab9-c71ea172753e",
    "contentPreview": "更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2959,
      "headingPath": "",
      "tokenCount": 679,
      "chunkIndex": 6,
      "title": "rag-technology-guide.md",
      "startIndex": 2280,
      "fileName": "rag-technology-guide.md"
    }
  },
  {
    "rank": 3,
    "score": 0.6689239144325256,
    "source": "733bb127-bd08-4b45-8342-a8ffe61423b4",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "733bb127-bd08-4b45-8342-a8ffe61423b4",
    "contentPreview": "文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈 关键指标 ： 检索召回率：Top-K 包含正确答案的比例 答案准确率：生成答案的正确性 用户满意度：...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": "",
      "tokenCount": 347,
      "chunkIndex": 7,
      "title": "rag-technology-guide.md",
      "startIndex": 2880,
      "fileName": "rag-technology-guide.md"
    }
  },
  {
    "rank": 4,
    "score": 0.3730737268924713,
    "source": "b6daca3c-9562-4625-bb24-1e75c565b5be",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "b6daca3c-9562-4625-bb24-1e75c565b5be",
    "contentPreview": "索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1550,
      "headingPath": "",
      "tokenCount": 450,
      "chunkIndex": 2,
      "title": "rag-technology-guide.md",
      "startIndex": 1101,
      "fileName": "rag-technology-guide.md"
    }
  },
  {
    "rank": 5,
    "score": 0.36234381794929504,
    "source": "114a46c5-14ad-461e-a8b1-ef2b845dd6b1",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "114a46c5-14ad-461e-a8b1-ef2b845dd6b1",
    "contentPreview": "：调用大模型生成答案 返回结果 ：返回答案和引用来源 3. 核心技术 3.1 Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.5...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1193,
      "headingPath": "",
      "tokenCount": 591,
      "chunkIndex": 1,
      "title": "rag-technology-guide.md",
      "startIndex": 601,
      "fileName": "rag-technology-guide.md"
    }
  }
]
```
- returned citations:
```json
[]
```
- citation validation metadata:
```json
{
  "validCitations": null,
  "droppedCitations": null,
  "citationCoverage": null,
  "unsupportedCitationCount": 0
}
```

### no-answer-002 (no_answer)

- id: `no-answer-002`
- type: `no_answer`
- question: Python GIL 对 CPU 密集型多线程有什么影响？
- expected_sources: `[]`
- expected_contexts.contains: `[]`
- retrieve hit ratio: `0/0`
- first_match_rank: `-`
- answer:
- expected_keywords: `[]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 1.0,
    "source": "2f28e90e-179a-453b-8468-4b10d82d40f5",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 43,
    "chunkId": "2f28e90e-179a-453b-8468-4b10d82d40f5",
    "contentPreview": "Java 后端面试知识点总结 1. Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 43,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 603,
      "headingPath": "",
      "tokenCount": 602,
      "chunkIndex": 0,
      "title": "java-interview-guide.md",
      "startIndex": 0,
      "fileName": "java-interview-guide.md"
    }
  },
  {
    "rank": 2,
    "score": 0.4826071858406067,
    "source": "1e423351-2df1-4829-8186-7344890ec4dd",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "1e423351-2df1-4829-8186-7344890ec4dd",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2333,
      "headingPath": "",
      "tokenCount": 653,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1679,
      "fileName": "rag-technology-guide.md"
    }
  },
  {
    "rank": 3,
    "score": 0.4670254588127136,
    "source": "114a46c5-14ad-461e-a8b1-ef2b845dd6b1",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "114a46c5-14ad-461e-a8b1-ef2b845dd6b1",
    "contentPreview": "：调用大模型生成答案 返回结果 ：返回答案和引用来源 3. 核心技术 3.1 Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.5...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1193,
      "headingPath": "",
      "tokenCount": 591,
      "chunkIndex": 1,
      "title": "rag-technology-guide.md",
      "startIndex": 601,
      "fileName": "rag-technology-guide.md"
    }
  },
  {
    "rank": 4,
    "score": 0.22815172374248505,
    "source": "f0c2f64f-2410-4b10-b3f2-f292e39baf6b",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 43,
    "chunkId": "f0c2f64f-2410-4b10-b3f2-f292e39baf6b",
    "contentPreview": "plate 观察者模式 ：Spring 事件监听 策略模式 ：支付方式选择 10. JVM 10.1 内存模型 堆 ：对象实例，GC 主要区域 栈 ：方法调用，局部变量 方法区 ：类信息、常量池 程序计数器 ：当前线程执行的字节码行号 10.2 垃圾回收 GC 算法 ： 标记-清除 复制算法（新生代） 标记-整理（老年代） GC 收集器 ： Serial、Parallel、CMS、G1、ZGC --- 面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationCon...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 43,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 3161,
      "headingPath": "",
      "tokenCount": 561,
      "chunkIndex": 5,
      "title": "java-interview-guide.md",
      "startIndex": 2601,
      "fileName": "java-interview-guide.md"
    }
  },
  {
    "rank": 5,
    "score": 0.18568481504917145,
    "source": "b6daca3c-9562-4625-bb24-1e75c565b5be",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 44,
    "chunkId": "b6daca3c-9562-4625-bb24-1e75c565b5be",
    "contentPreview": "索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 44,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1550,
      "headingPath": "",
      "tokenCount": 450,
      "chunkIndex": 2,
      "title": "rag-technology-guide.md",
      "startIndex": 1101,
      "fileName": "rag-technology-guide.md"
    }
  }
]
```
- returned citations:
```json
[]
```
- citation validation metadata:
```json
{
  "validCitations": null,
  "droppedCitations": null,
  "citationCoverage": null,
  "unsupportedCitationCount": 0
}
```

### no-answer-003 (no_answer)

- id: `no-answer-003`
- type: `no_answer`
- question: React useEffect 的依赖数组为空时会发生什么？
- expected_sources: `[]`
- expected_contexts.contains: `[]`
- retrieve hit ratio: `0/0`
- first_match_rank: `-`
- answer:
- expected_keywords: `[]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 1.0,
    "source": "231a43cd-45b9-4117-bbf0-7f2f8643d970",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 42,
    "chunkId": "231a43cd-45b9-4117-bbf0-7f2f8643d970",
    "contentPreview": "Spring Boot 核心知识点 1. Spring Boot 简介 Spring Boot 是基于 Spring 框架的快速开发脚手架，旨在简化 Spring 应用的初始搭建和开发过程。 1.1 核心特性 自动配置 ：根据 classpath 下的依赖自动配置 Spring 应用 起步依赖 ：一站式依赖管理，避免版本冲突 内嵌服务器 ：Tomcat、Jetty、Undertow 生产就绪 ：Actuator 健康检查、监控指标 无代码生成 ：不需要 XML 配置 2.",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "springboot-basics.md",
      "documentId": 42,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 239,
      "headingPath": "",
      "tokenCount": 238,
      "chunkIndex": 0,
      "title": "springboot-basics.md",
      "startIndex": 0,
      "fileName": "springboot-basics.md"
    }
  },
  {
    "rank": 2,
    "score": 0.7977641224861145,
    "source": "2f28e90e-179a-453b-8468-4b10d82d40f5",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 43,
    "chunkId": "2f28e90e-179a-453b-8468-4b10d82d40f5",
    "contentPreview": "Java 后端面试知识点总结 1. Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 43,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 603,
      "headingPath": "",
      "tokenCount": 602,
      "chunkIndex": 0,
      "title": "java-interview-guide.md",
      "startIndex": 0,
      "fileName": "java-interview-guide.md"
    }
  },
  {
    "rank": 3,
    "score": 0.5102343559265137,
    "source": "3e4a14f8-51cb-4c6d-b1f5-498a8a950966",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 42,
    "chunkId": "3e4a14f8-51cb-4c6d-b1f5-498a8a950966",
    "contentPreview": "// @Valid 触发校验 } 9. 事务管理 9.1 声明式事务 @Service public class OrderService { @Transactional(rollbackFor = Exception.class) public void createOrder(Order order) { orderRepository.save(order); // 如果发生异常，自动回滚 stockService.deduct(order.getProductId(), order.getQuantity()); } } 9.2 事务传播行为 REQUIRED （默认）：加入当前事务...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "springboot-basics.md",
      "documentId": 42,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 4197,
      "headingPath": "",
      "tokenCount": 514,
      "chunkIndex": 9,
      "title": "springboot-basics.md",
      "startIndex": 3681,
      "fileName": "springboot-basics.md"
    }
  },
  {
    "rank": 4,
    "score": 0.5025732517242432,
    "source": "2e66d5ed-9ca1-444d-8d68-8b34e03a26f3",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 42,
    "chunkId": "2e66d5ed-9ca1-444d-8d68-8b34e03a26f3",
    "contentPreview": "essionCreationPolicy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max = 20, messag...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "springboot-basics.md",
      "documentId": 42,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 3754,
      "headingPath": "",
      "tokenCount": 673,
      "chunkIndex": 8,
      "title": "springboot-basics.md",
      "startIndex": 3081,
      "fileName": "springboot-basics.md"
    }
  },
  {
    "rank": 5,
    "score": 0.37372684478759766,
    "source": "7ad3ab26-1f59-467c-8808-4a0159c1390c",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 43,
    "chunkId": "7ad3ab26-1f59-467c-8808-4a0159c1390c",
    "contentPreview": "2. Spring 框架 2.1 Spring IOC（控制反转） 依赖注入的三种方式 ： 构造器注入（推荐，保证不可变性） Setter 注入（可选依赖） 字段注入（不推荐，难以测试） Bean 的生命周期 ： 实例化 属性赋值 初始化（@PostConstruct） 使用 销毁（@PreDestroy） 2.2 Spring AOP（面向切面编程） 应用场景 ：事务管理、日志记录、权限校验、性能监控 核心概念 ： Aspect（切面）：横切关注点的模块化 Join Point（连接点）：方法执行点 Pointcut（切入点）：匹配规则 Advice（通知）：@Before、@After、@...",
    "metadata": {
      "kbId": 11,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 43,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 945,
      "headingPath": "",
      "tokenCount": 344,
      "chunkIndex": 1,
      "title": "java-interview-guide.md",
      "startIndex": 601,
      "fileName": "java-interview-guide.md"
    }
  }
]
```
- returned citations:
```json
[]
```
- citation validation metadata:
```json
{
  "validCitations": null,
  "droppedCitations": null,
  "citationCoverage": null,
  "unsupportedCitationCount": 0
}
```

## Source Normalization Diagnostics

| ID | Expected normalized | Retrieved candidate normalized | Citation candidate normalized |
|---|---|---|---|
| fact-001 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: 231a43cd 45b9 4117 bbf0 7f2f8643d970, 231a43cd45b94117bbf07f2f8643d970, springboot basics, springbootbasics; r3: 9bedf9de 67d7 44dd 80c7 0317df98252c, 9bedf9de67d744dd80c70317df98252c, java interview guide, javainterviewguide; r4: 11d07305 9429 45be aa8f 785038a48715, 11d07305942945beaa8f785038a48715, springboot basics, springbootbasics; r5: 733bb127 bd08 4b45 8342 a8ffe61423b4, 733bb127bd084b458342a8ffe61423b4, rag technology guide, ragtechnologyguide |  |
| fact-002 | springboot-basics.md => springboot basics, springbootbasics; java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, springboot basics, springbootbasics; r2: 7cf04af6 bda4 4c04 bb3a f0c73f7e62d5, 7cf04af6bda44c04bb3af0c73f7e62d5, springboot basics, springbootbasics; r3: 9bedf9de 67d7 44dd 80c7 0317df98252c, 9bedf9de67d744dd80c70317df98252c, java interview guide, javainterviewguide; r4: 98fd53bd d1e2 4368 870e 577d1e511304, 98fd53bdd1e24368870e577d1e511304, springboot basics, springbootbasics; r5: 3e4a14f8 51cb 4c6d b1f5 498a8a950966, 3e4a14f851cb4c6db1f5498a8a950966, springboot basics, springbootbasics |  |
| fact-003 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: 7cf04af6 bda4 4c04 bb3a f0c73f7e62d5, 7cf04af6bda44c04bb3af0c73f7e62d5, springboot basics, springbootbasics; r3: 2e66d5ed 9ca1 444d 8d68 8b34e03a26f3, 2e66d5ed9ca1444d8d688b34e03a26f3, springboot basics, springbootbasics; r4: 98fd53bd d1e2 4368 870e 577d1e511304, 98fd53bdd1e24368870e577d1e511304, springboot basics, springbootbasics; r5: 3e4a14f8 51cb 4c6d b1f5 498a8a950966, 3e4a14f851cb4c6db1f5498a8a950966, springboot basics, springbootbasics |  |
| fact-004 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: c916fa3e b47b 4456 9f20 125d01d40cd1, c916fa3eb47b44569f20125d01d40cd1, springboot basics, springbootbasics; r3: 231a43cd 45b9 4117 bbf0 7f2f8643d970, 231a43cd45b94117bbf07f2f8643d970, springboot basics, springbootbasics; r4: 9bedf9de 67d7 44dd 80c7 0317df98252c, 9bedf9de67d744dd80c70317df98252c, java interview guide, javainterviewguide; r5: 44ba0f47 a0b6 49d8 bab9 c71ea172753e, 44ba0f47a0b649d8bab9c71ea172753e, rag technology guide, ragtechnologyguide |  |
| fact-005 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: 2e66d5ed 9ca1 444d 8d68 8b34e03a26f3, 2e66d5ed9ca1444d8d688b34e03a26f3, springboot basics, springbootbasics; r3: b6daca3c 9562 4625 bb24 1e75c565b5be, b6daca3c95624625bb241e75c565b5be, rag technology guide, ragtechnologyguide; r4: 44ba0f47 a0b6 49d8 bab9 c71ea172753e, 44ba0f47a0b649d8bab9c71ea172753e, rag technology guide, ragtechnologyguide; r5: a9ef0078 c45e 48cd 86ad 763f0e314178, a9ef0078c45e48cd86ad763f0e314178, rag technology guide, ragtechnologyguide |  |
| fact-006 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 2f28e90e 179a 453b 8468 4b10d82d40f5, 2f28e90e179a453b84684b10d82d40f5, java interview guide, javainterviewguide; r3: 9bedf9de 67d7 44dd 80c7 0317df98252c, 9bedf9de67d744dd80c70317df98252c, java interview guide, javainterviewguide; r4: 7ad3ab26 1f59 467c 8808 4a0159c1390c, 7ad3ab261f59467c88084a0159c1390c, java interview guide, javainterviewguide; r5: 62823eec 6888 4ae2 abef 18057c9f266f, 62823eec68884ae2abef18057c9f266f, java interview guide, javainterviewguide |  |
| fact-007 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 62823eec 6888 4ae2 abef 18057c9f266f, 62823eec68884ae2abef18057c9f266f, java interview guide, javainterviewguide; r3: f0c2f64f 2410 4b10 b3f2 f292e39baf6b, f0c2f64f24104b10b3f2f292e39baf6b, java interview guide, javainterviewguide; r4: 934b1ed8 7db3 4a08 a1e3 859d1bf07692, 934b1ed87db34a08a1e3859d1bf07692, springboot basics, springbootbasics; r5: 44ba0f47 a0b6 49d8 bab9 c71ea172753e, 44ba0f47a0b649d8bab9c71ea172753e, rag technology guide, ragtechnologyguide |  |
| fact-008 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: a9ef0078 c45e 48cd 86ad 763f0e314178, a9ef0078c45e48cd86ad763f0e314178, rag technology guide, ragtechnologyguide; r2: 1e423351 2df1 4829 8186 7344890ec4dd, 1e4233512df1482981867344890ec4dd, rag technology guide, ragtechnologyguide; r3: 0, rag technology guide, ragtechnologyguide; r4: 114a46c5 14ad 461e a8b1 ef2b845dd6b1, 114a46c514ad461ea8b1ef2b845dd6b1, rag technology guide, ragtechnologyguide; r5: 44ba0f47 a0b6 49d8 bab9 c71ea172753e, 44ba0f47a0b649d8bab9c71ea172753e, rag technology guide, ragtechnologyguide |  |
| fact-009 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: a9ef0078 c45e 48cd 86ad 763f0e314178, a9ef0078c45e48cd86ad763f0e314178, rag technology guide, ragtechnologyguide; r3: 733bb127 bd08 4b45 8342 a8ffe61423b4, 733bb127bd084b458342a8ffe61423b4, rag technology guide, ragtechnologyguide; r4: 1e423351 2df1 4829 8186 7344890ec4dd, 1e4233512df1482981867344890ec4dd, rag technology guide, ragtechnologyguide; r5: 44ba0f47 a0b6 49d8 bab9 c71ea172753e, 44ba0f47a0b649d8bab9c71ea172753e, rag technology guide, ragtechnologyguide |  |
| fact-010 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 62823eec 6888 4ae2 abef 18057c9f266f, 62823eec68884ae2abef18057c9f266f, java interview guide, javainterviewguide; r3: b6daca3c 9562 4625 bb24 1e75c565b5be, b6daca3c95624625bb241e75c565b5be, rag technology guide, ragtechnologyguide; r4: 114a46c5 14ad 461e a8b1 ef2b845dd6b1, 114a46c514ad461ea8b1ef2b845dd6b1, rag technology guide, ragtechnologyguide; r5: 9bedf9de 67d7 44dd 80c7 0317df98252c, 9bedf9de67d744dd80c70317df98252c, java interview guide, javainterviewguide |  |
| definition-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: a9ef0078 c45e 48cd 86ad 763f0e314178, a9ef0078c45e48cd86ad763f0e314178, rag technology guide, ragtechnologyguide; r3: b6daca3c 9562 4625 bb24 1e75c565b5be, b6daca3c95624625bb241e75c565b5be, rag technology guide, ragtechnologyguide; r4: 1e423351 2df1 4829 8186 7344890ec4dd, 1e4233512df1482981867344890ec4dd, rag technology guide, ragtechnologyguide; r5: 733bb127 bd08 4b45 8342 a8ffe61423b4, 733bb127bd084b458342a8ffe61423b4, rag technology guide, ragtechnologyguide |  |
| definition-002 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: a9ef0078 c45e 48cd 86ad 763f0e314178, a9ef0078c45e48cd86ad763f0e314178, rag technology guide, ragtechnologyguide; r2: 114a46c5 14ad 461e a8b1 ef2b845dd6b1, 114a46c514ad461ea8b1ef2b845dd6b1, rag technology guide, ragtechnologyguide; r3: 44ba0f47 a0b6 49d8 bab9 c71ea172753e, 44ba0f47a0b649d8bab9c71ea172753e, rag technology guide, ragtechnologyguide; r4: 1e423351 2df1 4829 8186 7344890ec4dd, 1e4233512df1482981867344890ec4dd, rag technology guide, ragtechnologyguide; r5: b6daca3c 9562 4625 bb24 1e75c565b5be, b6daca3c95624625bb241e75c565b5be, rag technology guide, ragtechnologyguide |  |
| definition-003 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: a9ef0078 c45e 48cd 86ad 763f0e314178, a9ef0078c45e48cd86ad763f0e314178, rag technology guide, ragtechnologyguide; r3: 114a46c5 14ad 461e a8b1 ef2b845dd6b1, 114a46c514ad461ea8b1ef2b845dd6b1, rag technology guide, ragtechnologyguide; r4: 44ba0f47 a0b6 49d8 bab9 c71ea172753e, 44ba0f47a0b649d8bab9c71ea172753e, rag technology guide, ragtechnologyguide; r5: 1e423351 2df1 4829 8186 7344890ec4dd, 1e4233512df1482981867344890ec4dd, rag technology guide, ragtechnologyguide |  |
| definition-004 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: b6daca3c 9562 4625 bb24 1e75c565b5be, b6daca3c95624625bb241e75c565b5be, rag technology guide, ragtechnologyguide; r2: a9ef0078 c45e 48cd 86ad 763f0e314178, a9ef0078c45e48cd86ad763f0e314178, rag technology guide, ragtechnologyguide; r3: 114a46c5 14ad 461e a8b1 ef2b845dd6b1, 114a46c514ad461ea8b1ef2b845dd6b1, rag technology guide, ragtechnologyguide; r4: 44ba0f47 a0b6 49d8 bab9 c71ea172753e, 44ba0f47a0b649d8bab9c71ea172753e, rag technology guide, ragtechnologyguide; r5: 0, rag technology guide, ragtechnologyguide |  |
| definition-005 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 7ad3ab26 1f59 467c 8808 4a0159c1390c, 7ad3ab261f59467c88084a0159c1390c, java interview guide, javainterviewguide; r3: 231a43cd 45b9 4117 bbf0 7f2f8643d970, 231a43cd45b94117bbf07f2f8643d970, springboot basics, springbootbasics; r4: 44ba0f47 a0b6 49d8 bab9 c71ea172753e, 44ba0f47a0b649d8bab9c71ea172753e, rag technology guide, ragtechnologyguide; r5: 11d07305 9429 45be aa8f 785038a48715, 11d07305942945beaa8f785038a48715, springboot basics, springbootbasics |  |
| definition-006 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 7ad3ab26 1f59 467c 8808 4a0159c1390c, 7ad3ab261f59467c88084a0159c1390c, java interview guide, javainterviewguide; r3: 44ba0f47 a0b6 49d8 bab9 c71ea172753e, 44ba0f47a0b649d8bab9c71ea172753e, rag technology guide, ragtechnologyguide; r4: b6daca3c 9562 4625 bb24 1e75c565b5be, b6daca3c95624625bb241e75c565b5be, rag technology guide, ragtechnologyguide; r5: 992e271e ea2e 4ecc 9fa5 1454f6e4561d, 992e271eea2e4ecc9fa51454f6e4561d, java interview guide, javainterviewguide |  |
| definition-007 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 62823eec 6888 4ae2 abef 18057c9f266f, 62823eec68884ae2abef18057c9f266f, java interview guide, javainterviewguide; r2: 934b1ed8 7db3 4a08 a1e3 859d1bf07692, 934b1ed87db34a08a1e3859d1bf07692, springboot basics, springbootbasics; r3: 168826ac 26ad 4eaa 8206 90912ab5bf5e, 168826ac26ad4eaa820690912ab5bf5e, springboot basics, springbootbasics; r4: 9bedf9de 67d7 44dd 80c7 0317df98252c, 9bedf9de67d744dd80c70317df98252c, java interview guide, javainterviewguide; r5: 1e423351 2df1 4829 8186 7344890ec4dd, 1e4233512df1482981867344890ec4dd, rag technology guide, ragtechnologyguide |  |
| definition-008 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 992e271e ea2e 4ecc 9fa5 1454f6e4561d, 992e271eea2e4ecc9fa51454f6e4561d, java interview guide, javainterviewguide; r3: 62823eec 6888 4ae2 abef 18057c9f266f, 62823eec68884ae2abef18057c9f266f, java interview guide, javainterviewguide; r4: f0c2f64f 2410 4b10 b3f2 f292e39baf6b, f0c2f64f24104b10b3f2f292e39baf6b, java interview guide, javainterviewguide; r5: 2d81d6bc f8f6 4c54 ab0e fe79e11f9955, 2d81d6bcf8f64c54ab0efe79e11f9955, rag technology guide, ragtechnologyguide |  |
| reasoning-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: b6daca3c 9562 4625 bb24 1e75c565b5be, b6daca3c95624625bb241e75c565b5be, rag technology guide, ragtechnologyguide; r2: 44ba0f47 a0b6 49d8 bab9 c71ea172753e, 44ba0f47a0b649d8bab9c71ea172753e, rag technology guide, ragtechnologyguide; r3: 114a46c5 14ad 461e a8b1 ef2b845dd6b1, 114a46c514ad461ea8b1ef2b845dd6b1, rag technology guide, ragtechnologyguide; r4: 0, rag technology guide, ragtechnologyguide; r5: 733bb127 bd08 4b45 8342 a8ffe61423b4, 733bb127bd084b458342a8ffe61423b4, rag technology guide, ragtechnologyguide |  |
| reasoning-002 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 1e423351 2df1 4829 8186 7344890ec4dd, 1e4233512df1482981867344890ec4dd, rag technology guide, ragtechnologyguide; r3: a9ef0078 c45e 48cd 86ad 763f0e314178, a9ef0078c45e48cd86ad763f0e314178, rag technology guide, ragtechnologyguide; r4: 44ba0f47 a0b6 49d8 bab9 c71ea172753e, 44ba0f47a0b649d8bab9c71ea172753e, rag technology guide, ragtechnologyguide; r5: b6daca3c 9562 4625 bb24 1e75c565b5be, b6daca3c95624625bb241e75c565b5be, rag technology guide, ragtechnologyguide |  |
| reasoning-003 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: b6daca3c 9562 4625 bb24 1e75c565b5be, b6daca3c95624625bb241e75c565b5be, rag technology guide, ragtechnologyguide; r2: 44ba0f47 a0b6 49d8 bab9 c71ea172753e, 44ba0f47a0b649d8bab9c71ea172753e, rag technology guide, ragtechnologyguide; r3: 114a46c5 14ad 461e a8b1 ef2b845dd6b1, 114a46c514ad461ea8b1ef2b845dd6b1, rag technology guide, ragtechnologyguide; r4: 1e423351 2df1 4829 8186 7344890ec4dd, 1e4233512df1482981867344890ec4dd, rag technology guide, ragtechnologyguide; r5: 0, rag technology guide, ragtechnologyguide |  |
| reasoning-004 | springboot-basics.md => springboot basics, springbootbasics; java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: c916fa3e b47b 4456 9f20 125d01d40cd1, c916fa3eb47b44569f20125d01d40cd1, springboot basics, springbootbasics; r3: 7ad3ab26 1f59 467c 8808 4a0159c1390c, 7ad3ab261f59467c88084a0159c1390c, java interview guide, javainterviewguide; r4: 44ba0f47 a0b6 49d8 bab9 c71ea172753e, 44ba0f47a0b649d8bab9c71ea172753e, rag technology guide, ragtechnologyguide; r5: b6daca3c 9562 4625 bb24 1e75c565b5be, b6daca3c95624625bb241e75c565b5be, rag technology guide, ragtechnologyguide |  |
| reasoning-005 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 2f28e90e 179a 453b 8468 4b10d82d40f5, 2f28e90e179a453b84684b10d82d40f5, java interview guide, javainterviewguide; r3: b6daca3c 9562 4625 bb24 1e75c565b5be, b6daca3c95624625bb241e75c565b5be, rag technology guide, ragtechnologyguide; r4: f0c2f64f 2410 4b10 b3f2 f292e39baf6b, f0c2f64f24104b10b3f2f292e39baf6b, java interview guide, javainterviewguide; r5: 114a46c5 14ad 461e a8b1 ef2b845dd6b1, 114a46c514ad461ea8b1ef2b845dd6b1, rag technology guide, ragtechnologyguide |  |
| reasoning-006 | java-interview-guide.md => java interview guide, javainterviewguide | r1: b6daca3c 9562 4625 bb24 1e75c565b5be, b6daca3c95624625bb241e75c565b5be, rag technology guide, ragtechnologyguide; r2: 114a46c5 14ad 461e a8b1 ef2b845dd6b1, 114a46c514ad461ea8b1ef2b845dd6b1, rag technology guide, ragtechnologyguide; r3: 44ba0f47 a0b6 49d8 bab9 c71ea172753e, 44ba0f47a0b649d8bab9c71ea172753e, rag technology guide, ragtechnologyguide; r4: 1e423351 2df1 4829 8186 7344890ec4dd, 1e4233512df1482981867344890ec4dd, rag technology guide, ragtechnologyguide; r5: a9ef0078 c45e 48cd 86ad 763f0e314178, a9ef0078c45e48cd86ad763f0e314178, rag technology guide, ragtechnologyguide |  |
| multi-hop-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: a9ef0078 c45e 48cd 86ad 763f0e314178, a9ef0078c45e48cd86ad763f0e314178, rag technology guide, ragtechnologyguide; r3: 733bb127 bd08 4b45 8342 a8ffe61423b4, 733bb127bd084b458342a8ffe61423b4, rag technology guide, ragtechnologyguide; r4: 44ba0f47 a0b6 49d8 bab9 c71ea172753e, 44ba0f47a0b649d8bab9c71ea172753e, rag technology guide, ragtechnologyguide; r5: 1e423351 2df1 4829 8186 7344890ec4dd, 1e4233512df1482981867344890ec4dd, rag technology guide, ragtechnologyguide |  |
| multi-hop-002 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 1e423351 2df1 4829 8186 7344890ec4dd, 1e4233512df1482981867344890ec4dd, rag technology guide, ragtechnologyguide; r3: 733bb127 bd08 4b45 8342 a8ffe61423b4, 733bb127bd084b458342a8ffe61423b4, rag technology guide, ragtechnologyguide; r4: b6daca3c 9562 4625 bb24 1e75c565b5be, b6daca3c95624625bb241e75c565b5be, rag technology guide, ragtechnologyguide; r5: a9ef0078 c45e 48cd 86ad 763f0e314178, a9ef0078c45e48cd86ad763f0e314178, rag technology guide, ragtechnologyguide |  |
| multi-hop-003 | java-interview-guide.md => java interview guide, javainterviewguide; springboot-basics.md => springboot basics, springbootbasics | r1: f0c2f64f 2410 4b10 b3f2 f292e39baf6b, f0c2f64f24104b10b3f2f292e39baf6b, java interview guide, javainterviewguide; r2: 44ba0f47 a0b6 49d8 bab9 c71ea172753e, 44ba0f47a0b649d8bab9c71ea172753e, rag technology guide, ragtechnologyguide; r3: 1e423351 2df1 4829 8186 7344890ec4dd, 1e4233512df1482981867344890ec4dd, rag technology guide, ragtechnologyguide; r4: 9bedf9de 67d7 44dd 80c7 0317df98252c, 9bedf9de67d744dd80c70317df98252c, java interview guide, javainterviewguide; r5: 992e271e ea2e 4ecc 9fa5 1454f6e4561d, 992e271eea2e4ecc9fa51454f6e4561d, java interview guide, javainterviewguide |  |
| no-answer-001 |  | r1: a9ef0078 c45e 48cd 86ad 763f0e314178, a9ef0078c45e48cd86ad763f0e314178, rag technology guide, ragtechnologyguide; r2: 44ba0f47 a0b6 49d8 bab9 c71ea172753e, 44ba0f47a0b649d8bab9c71ea172753e, rag technology guide, ragtechnologyguide; r3: 733bb127 bd08 4b45 8342 a8ffe61423b4, 733bb127bd084b458342a8ffe61423b4, rag technology guide, ragtechnologyguide; r4: b6daca3c 9562 4625 bb24 1e75c565b5be, b6daca3c95624625bb241e75c565b5be, rag technology guide, ragtechnologyguide; r5: 114a46c5 14ad 461e a8b1 ef2b845dd6b1, 114a46c514ad461ea8b1ef2b845dd6b1, rag technology guide, ragtechnologyguide |  |
| no-answer-002 |  | r1: 2f28e90e 179a 453b 8468 4b10d82d40f5, 2f28e90e179a453b84684b10d82d40f5, java interview guide, javainterviewguide; r2: 1e423351 2df1 4829 8186 7344890ec4dd, 1e4233512df1482981867344890ec4dd, rag technology guide, ragtechnologyguide; r3: 114a46c5 14ad 461e a8b1 ef2b845dd6b1, 114a46c514ad461ea8b1ef2b845dd6b1, rag technology guide, ragtechnologyguide; r4: f0c2f64f 2410 4b10 b3f2 f292e39baf6b, f0c2f64f24104b10b3f2f292e39baf6b, java interview guide, javainterviewguide; r5: b6daca3c 9562 4625 bb24 1e75c565b5be, b6daca3c95624625bb241e75c565b5be, rag technology guide, ragtechnologyguide |  |
| no-answer-003 |  | r1: 231a43cd 45b9 4117 bbf0 7f2f8643d970, 231a43cd45b94117bbf07f2f8643d970, springboot basics, springbootbasics; r2: 2f28e90e 179a 453b 8468 4b10d82d40f5, 2f28e90e179a453b84684b10d82d40f5, java interview guide, javainterviewguide; r3: 3e4a14f8 51cb 4c6d b1f5 498a8a950966, 3e4a14f851cb4c6db1f5498a8a950966, springboot basics, springbootbasics; r4: 2e66d5ed 9ca1 444d 8d68 8b34e03a26f3, 2e66d5ed9ca1444d8d688b34e03a26f3, springboot basics, springbootbasics; r5: 7ad3ab26 1f59 467c 8808 4a0159c1390c, 7ad3ab261f59467c88084a0159c1390c, java interview guide, javainterviewguide |  |

## Citation Diagnostics

| ID | Returned | Source Hits | Snippet Hits | Validation | Unsupported |
|---|---:|---:|---:|---|---:|
| fact-001 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| fact-002 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| fact-003 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| fact-004 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| fact-005 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| fact-006 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| fact-007 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| fact-008 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| fact-009 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| fact-010 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| definition-001 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| definition-002 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| definition-003 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| definition-004 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| definition-005 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| definition-006 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| definition-007 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| definition-008 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| reasoning-001 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| reasoning-002 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| reasoning-003 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| reasoning-004 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| reasoning-005 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| reasoning-006 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| multi-hop-001 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| multi-hop-002 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| multi-hop-003 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| no-answer-001 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| no-answer-002 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| no-answer-003 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
