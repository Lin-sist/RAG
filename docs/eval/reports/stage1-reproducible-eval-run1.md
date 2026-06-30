# RAG Eval Report

- Generated at: 2026-06-30T00:58:05.003721+00:00
- Report status: `RETRIEVAL_ONLY`
- askErrors count: `0`
- retrieveErrors count: `0`
- skippedAsk count: `30`
- rateLimitErrors count: `0`
- retry count: `0`
- Metrics safe for comparison: `retrieval metrics only`
- Base URL: `http://localhost:8080`
- Knowledge base ID: `7`
- Eval set: `docs\eval\rag_eval_set.jsonl`
- topK: `5`
- minScore: `0.3`
- enableRerank: `True`
- skipAsk: `True`
- Eval KB name: `codex-stage1-repro-eval`
- Eval KB vector collection: `kb_b336dd162e314e30`
- Eval KB document count: `3`
- Eval KB chunk count: `40`
- Fixture files: `springboot-basics.md sha256=c51df5761d510aa4c8a5fd610c90454b12973e2999138c3b57ba83768a296521; java-interview-guide.md sha256=a33f16e91025e9a8d92274c4221d1c172bb4f68c03790db53b8e20157ef4faa0; rag-technology-guide.md sha256=59ad5d66a59be2ce4e517ca81e37fde06b7673f3da7f798a4c76b01cc6f348a9`
- Config snapshot: `rag-admin\src\main\resources\application.yml sha256=477188fb3a259adfe25805eb3cda5594a195b8af1038e77deff072c733c9fe52; rag-core\src\main\java\com\enterprise\rag\core\rag\query\RetrievalProperties.java sha256=f4d49e1ed6eeee0dc5c358df16bec0ec7424dd97b7dd03afeb1ca4f201e391e6; rag-document\src\main\java\com\enterprise\rag\document\chunker\ChunkConfig.java sha256=01b17712c8974055d15a30034e396d5074ba37ad8ea637fa7bca895a0c8a8554`
- Git HEAD: `89ae7fb99c3ce21eb3250eb5bf711f5fa8795659`
- askDelaySeconds: `0.0`
- maxAskRetries: `0`
- retryBackoffSeconds: `0.0`
- Duration: `72.94s`

## Summary Metrics

| Metric | Value |
|---|---:|
| Samples | 30 |
| Answerable samples | 27 |
| No-answer samples | 3 |
| Recall@3 | 62.75% |
| Recall@5 | 62.75% |
| MRR | 0.6605 |
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
| fact-006 | fact | 2/2 | 2 | skipped | skipped | skipped | skipped | 0 | - |  |
| fact-007 | fact | 1/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| fact-008 | fact | 0/2 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| fact-009 | fact | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| fact-010 | fact | 0/1 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-001 | definition | 1/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-002 | definition | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-003 | definition | 0/2 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-004 | definition | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-005 | definition | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-006 | definition | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-007 | definition | 1/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-008 | definition | 1/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| reasoning-001 | reasoning | 2/2 | 3 | skipped | skipped | skipped | skipped | 0 | - |  |
| reasoning-002 | reasoning | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| reasoning-003 | reasoning | 0/2 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| reasoning-004 | reasoning | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| reasoning-005 | reasoning | 0/1 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| reasoning-006 | reasoning | 0/2 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| multi-hop-001 | multi_hop | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| multi-hop-002 | multi_hop | 1/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| multi-hop-003 | multi_hop | 1/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
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
    "documentId": 33,
    "chunkId": "0",
    "contentPreview": "ctuator 健康检查、监控指标 无代码生成 ：不需要 XML 配置 2. 核心注解 2.1 启动类注解 @SpringBootApplication public class Application { public static void main(String[] args) { SpringApplication.run(Application.class, args); } } @SpringBootApplication 是组合注解，包含： @Configuration ：标记为配置类 @EnableAutoConfiguration ：启用自动配置 @ComponentScan...",
    "metadata": {
      "chunkIndex": 1.0,
      "tokenCount": 528.0,
      "headingPath": "",
      "sourceFileName": "springboot-basics.md",
      "documentId": 33.0,
      "kbId": 7.0,
      "startIndex": 201.0,
      "endIndex": 729.0,
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
    "source": "23c41b1a-f25c-48b9-a891-be280a9af481",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 33,
    "chunkId": "23c41b1a-f25c-48b9-a891-be280a9af481",
    "contentPreview": "ctuator 健康检查、监控指标 无代码生成 ：不需要 XML 配置 2. 核心注解 2.1 启动类注解 @SpringBootApplication public class Application { public static void main(String[] args) { SpringApplication.run(Application.class, args); } } @SpringBootApplication 是组合注解，包含： @Configuration ：标记为配置类 @EnableAutoConfiguration ：启用自动配置 @ComponentScan...",
    "metadata": {
      "fileName": "springboot-basics.md",
      "kbId": 7,
      "sourceFileName": "springboot-basics.md",
      "documentId": 33,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 729,
      "headingPath": "",
      "tokenCount": 528,
      "chunkIndex": 1,
      "title": "springboot-basics.md",
      "startIndex": 201,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "730d8a51-0238-4a0a-b186-429ce81aeded",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 33,
    "chunkId": "730d8a51-0238-4a0a-b186-429ce81aeded",
    "contentPreview": "icy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max = 20, message = \"用户名长度 3-20\")...",
    "metadata": {
      "fileName": "springboot-basics.md",
      "kbId": 7,
      "sourceFileName": "springboot-basics.md",
      "documentId": 33,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 3563,
      "headingPath": "",
      "tokenCount": 465,
      "chunkIndex": 11,
      "title": "springboot-basics.md",
      "startIndex": 3098,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "f5ce4456-d970-4949-b70f-6ec009b05c3f",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 33,
    "chunkId": "f5ce4456-d970-4949-b70f-6ec009b05c3f",
    "contentPreview": "e public class EmailService { @Async public CompletableFuture<String> sendEmail(String to, String subject) { // 异步发送邮件 return CompletableFuture.completedFuture(\"SUCCESS\"); } } 11. 定时任务 11.1 @Scheduled 注解 @Component public class ScheduledTasks { // 每 5 秒执行一次 @Scheduled(fixedRate = 5000) public void r...",
    "metadata": {
      "fileName": "springboot-basics.md",
      "kbId": 7,
      "sourceFileName": "springboot-basics.md",
      "documentId": 33,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 4958,
      "headingPath": "",
      "tokenCount": 410,
      "chunkIndex": 15,
      "title": "springboot-basics.md",
      "startIndex": 4548,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "5062a270-ddee-42d4-bb8e-77eafc15e0d3",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 33,
    "chunkId": "5062a270-ddee-42d4-bb8e-77eafc15e0d3",
    "contentPreview": "vice { @Transactional(rollbackFor = Exception.class) public void createOrder(Order order) { orderRepository.save(order); // 如果发生异常，自动回滚 stockService.deduct(order.getProductId(), order.getQuantity()); } } 9.2 事务传播行为 REQUIRED （默认）：加入当前事务，没有则新建 REQUIRES_NEW ：总是新建事务 SUPPORTS ：有事务就加入，没有就非事务执行 NOT_SUPPORT...",
    "metadata": {
      "fileName": "springboot-basics.md",
      "kbId": 7,
      "sourceFileName": "springboot-basics.md",
      "documentId": 33,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 4197,
      "headingPath": "",
      "tokenCount": 449,
      "chunkIndex": 13,
      "title": "springboot-basics.md",
      "startIndex": 3748,
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
    "documentId": 33,
    "chunkId": "0",
    "contentPreview": "icy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max = 20, message = \"用户名长度 3-20\")...",
    "metadata": {
      "chunkIndex": 11.0,
      "tokenCount": 465.0,
      "headingPath": "",
      "sourceFileName": "springboot-basics.md",
      "documentId": 33.0,
      "kbId": 7.0,
      "startIndex": 3098.0,
      "endIndex": 3563.0,
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
    "source": "730d8a51-0238-4a0a-b186-429ce81aeded",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 33,
    "chunkId": "730d8a51-0238-4a0a-b186-429ce81aeded",
    "contentPreview": "icy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max = 20, message = \"用户名长度 3-20\")...",
    "metadata": {
      "fileName": "springboot-basics.md",
      "kbId": 7,
      "sourceFileName": "springboot-basics.md",
      "documentId": 33,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 3563,
      "headingPath": "",
      "tokenCount": 465,
      "chunkIndex": 11,
      "title": "springboot-basics.md",
      "startIndex": 3098,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.015384615398943424,
    "source": "d3141a54-acc4-429e-8ee1-91ee14b2e148",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "d3141a54-acc4-429e-8ee1-91ee14b2e148",
    "contentPreview": "和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1447,
      "headingPath": "",
      "tokenCount": 446,
      "chunkIndex": 3,
      "title": "rag-technology-guide.md",
      "startIndex": 1001,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015384615384615385
    }
  },
  {
    "rank": 4,
    "score": 0.016129031777381897,
    "source": "638c0090-847c-4464-9474-346b26a4a2d6",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "638c0090-847c-4464-9474-346b26a4a2d6",
    "contentPreview": ": 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": "",
      "tokenCount": 398,
      "chunkIndex": 10,
      "title": "rag-technology-guide.md",
      "startIndex": 2830,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 5,
    "score": 0.01587301678955555,
    "source": "6907ad26-5def-4ec1-a9d7-1b421c5be86c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "6907ad26-5def-4ec1-a9d7-1b421c5be86c",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 270,
      "headingPath": "",
      "tokenCount": 269,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
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

### fact-007 (fact)

- id: `fact-007`
- type: `fact`
- question: Redis 常见数据类型包括哪些？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["数据类型", "ZSet：排行榜"]`
- retrieve hit ratio: `1/2`
- first_match_rank: `1`
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
    "documentId": 34,
    "chunkId": "0",
    "contentPreview": "锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好 7. 分布式 7.1 分布式锁 实现方...",
    "metadata": {
      "chunkIndex": 7.0,
      "tokenCount": 495.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 34.0,
      "kbId": 7.0,
      "startIndex": 1904.0,
      "endIndex": 2399.0,
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
    "source": "b53845a8-7b7e-4425-b022-a44130b1ae67",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 34,
    "chunkId": "b53845a8-7b7e-4425-b022-a44130b1ae67",
    "contentPreview": "锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好 7. 分布式 7.1 分布式锁 实现方...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "kbId": 7,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 34,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 2399,
      "headingPath": "",
      "tokenCount": 495,
      "chunkIndex": 7,
      "title": "java-interview-guide.md",
      "startIndex": 1904,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "47b1ccdb-6a87-49f8-b640-30f1b3fc8211",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 34,
    "chunkId": "47b1ccdb-6a87-49f8-b640-30f1b3fc8211",
    "contentPreview": "面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID + Redis Token 机制 乐观锁（版本号） 状态机（订单状态） 5. ...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "kbId": 7,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 34,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 3161,
      "headingPath": "",
      "tokenCount": 358,
      "chunkIndex": 9,
      "title": "java-interview-guide.md",
      "startIndex": 2804,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "5321266b-4613-4675-8953-219c9ec73464",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 33,
    "chunkId": "5321266b-4613-4675-8953-219c9ec73464",
    "contentPreview": "()); } // 每天凌晨 2 点执行 @Scheduled(cron = \"0 0 2 * * ?\") public void cleanupExpiredData() { log.info(\"清理过期数据\"); } } 12. 缓存 12.1 Spring Cache @Configuration @EnableCaching public class CacheConfig { // 配置 Redis 缓存 } @Service public class UserService { @Cacheable(value = \"users\", key = \"#id\") public User...",
    "metadata": {
      "fileName": "springboot-basics.md",
      "kbId": 7,
      "sourceFileName": "springboot-basics.md",
      "documentId": 33,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 5439,
      "headingPath": "",
      "tokenCount": 491,
      "chunkIndex": 16,
      "title": "springboot-basics.md",
      "startIndex": 4948,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015384615398943424,
    "source": "34a2b4f1-6bd9-4ede-aa5f-4231d328d409",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 34,
    "chunkId": "34a2b4f1-6bd9-4ede-aa5f-4231d328d409",
    "contentPreview": "锁、逻辑删除支持 5. MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNCOMMITTED（读未提交） READ COMMITTED（读已提交） REPEATABLE READ（可...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "kbId": 7,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 34,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 1921,
      "headingPath": "",
      "tokenCount": 367,
      "chunkIndex": 6,
      "title": "java-interview-guide.md",
      "startIndex": 1554,
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
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "0",
    "contentPreview": "端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索...",
    "metadata": {
      "chunkIndex": 8.0,
      "tokenCount": 469.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35.0,
      "kbId": 7.0,
      "startIndex": 1980.0,
      "endIndex": 2449.0,
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
    "source": "e3b0cf86-c288-4870-b3b6-6157ecdb6fd0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "e3b0cf86-c288-4870-b3b6-6157ecdb6fd0",
    "contentPreview": "LM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召回相关文档 → 构建提示词 → LLM 生成答案 → 返回结果 2.1 详细步骤 Step 1: 文档处理（离线） 文档上传 ：PDF、Markdown、Word、代码文件等 文档解析 ：提取文本内容 文本分块 ：将长文档切分为小块（Chunk），通常 500-2000 字符 向量化 ：使用 Embedding 模型将文本转换为向量 存储 ：向量存入向量数据库（Milvus、Pinecone 等） Step 2: 问答处理（在线） 问题向量化 ：用户问题转换为向量 相似度检索 ：在向量数据库中搜...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 684,
      "headingPath": "",
      "tokenCount": 433,
      "chunkIndex": 1,
      "title": "rag-technology-guide.md",
      "startIndex": 251,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.01587301678955555,
    "source": "1b4bf8bc-15b0-40ab-8b30-5eec811556d2",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "1b4bf8bc-15b0-40ab-8b30-5eec811556d2",
    "contentPreview": "端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2449,
      "headingPath": "",
      "tokenCount": 469,
      "chunkIndex": 8,
      "title": "rag-technology-guide.md",
      "startIndex": 1980,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 4,
    "score": 0.016129031777381897,
    "source": "24f06cc0-b32d-4d6b-a277-4ef3b86977d8",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "24f06cc0-b32d-4d6b-a277-4ef3b86977d8",
    "contentPreview": "嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.52, ..., 0.91] (相似度高) \"今天天气很好\" → [-0.78, 0.23, -0....",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1022,
      "headingPath": "",
      "tokenCount": 371,
      "chunkIndex": 2,
      "title": "rag-technology-guide.md",
      "startIndex": 651,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "f5056f44-f111-402e-a3b3-fb1b1c4e2d4c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "f5056f44-f111-402e-a3b3-fb1b1c4e2d4c",
    "contentPreview": "e、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文档、技术博客 优势 ：快速查找代码示例、技术方案 7.4 法律文书检索 场景 ：法律条文、判例、合同模板 优势 ：精准检索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2841,
      "headingPath": "",
      "tokenCount": 411,
      "chunkIndex": 9,
      "title": "rag-technology-guide.md",
      "startIndex": 2430,
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
    "documentId": 34,
    "chunkId": "0",
    "contentPreview": "锁、逻辑删除支持 5. MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNCOMMITTED（读未提交） READ COMMITTED（读已提交） REPEATABLE READ（可...",
    "metadata": {
      "chunkIndex": 6.0,
      "tokenCount": 367.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 34.0,
      "kbId": 7.0,
      "startIndex": 1554.0,
      "endIndex": 1921.0,
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
    "source": "34a2b4f1-6bd9-4ede-aa5f-4231d328d409",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 34,
    "chunkId": "34a2b4f1-6bd9-4ede-aa5f-4231d328d409",
    "contentPreview": "锁、逻辑删除支持 5. MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNCOMMITTED（读未提交） READ COMMITTED（读已提交） REPEATABLE READ（可...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "kbId": 7,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 34,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 1921,
      "headingPath": "",
      "tokenCount": 367,
      "chunkIndex": 6,
      "title": "java-interview-guide.md",
      "startIndex": 1554,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "d3141a54-acc4-429e-8ee1-91ee14b2e148",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "d3141a54-acc4-429e-8ee1-91ee14b2e148",
    "contentPreview": "和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1447,
      "headingPath": "",
      "tokenCount": 446,
      "chunkIndex": 3,
      "title": "rag-technology-guide.md",
      "startIndex": 1001,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.015625,
    "source": "47b1ccdb-6a87-49f8-b640-30f1b3fc8211",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 34,
    "chunkId": "47b1ccdb-6a87-49f8-b640-30f1b3fc8211",
    "contentPreview": "面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID + Redis Token 机制 乐观锁（版本号） 状态机（订单状态） 5. ...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "kbId": 7,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 34,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 3161,
      "headingPath": "",
      "tokenCount": 358,
      "chunkIndex": 9,
      "title": "java-interview-guide.md",
      "startIndex": 2804,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 5,
    "score": 0.014925372786819935,
    "source": "41e7dcc7-a481-4522-af0b-91d7894cab79",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 34,
    "chunkId": "41e7dcc7-a481-4522-af0b-91d7894cab79",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "kbId": 7,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 34,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 518,
      "headingPath": "",
      "tokenCount": 500,
      "chunkIndex": 1,
      "title": "java-interview-guide.md",
      "startIndex": 18,
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
    "documentId": 35,
    "chunkId": "0",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "chunkIndex": 0.0,
      "tokenCount": 269.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35.0,
      "kbId": 7.0,
      "startIndex": 0.0,
      "endIndex": 270.0,
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
    "source": "6907ad26-5def-4ec1-a9d7-1b421c5be86c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "6907ad26-5def-4ec1-a9d7-1b421c5be86c",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 270,
      "headingPath": "",
      "tokenCount": 269,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "638c0090-847c-4464-9474-346b26a4a2d6",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "638c0090-847c-4464-9474-346b26a4a2d6",
    "contentPreview": ": 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": "",
      "tokenCount": 398,
      "chunkIndex": 10,
      "title": "rag-technology-guide.md",
      "startIndex": 2830,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "1b4bf8bc-15b0-40ab-8b30-5eec811556d2",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "1b4bf8bc-15b0-40ab-8b30-5eec811556d2",
    "contentPreview": "端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2449,
      "headingPath": "",
      "tokenCount": 469,
      "chunkIndex": 8,
      "title": "rag-technology-guide.md",
      "startIndex": 1980,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "1373b31b-a798-4214-8149-abced23ca62d",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "1373b31b-a798-4214-8149-abced23ca62d",
    "contentPreview": "：技术原理 Chunk 3（500 字）：应用场景 Chunk 4（500 字）：总结 4. 提示词工程（Prompt Engineering） RAG 系统需要精心设计提示词，将检索到的上下文和问题结合。 4.1 典型 Prompt 模板 你是一个专业的技术助手。请根据以下文档内容回答用户的问题。",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1550,
      "headingPath": "",
      "tokenCount": 150,
      "chunkIndex": 4,
      "title": "rag-technology-guide.md",
      "startIndex": 1401,
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
    "documentId": 35,
    "chunkId": "0",
    "contentPreview": "嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.52, ..., 0.91] (相似度高) \"今天天气很好\" → [-0.78, 0.23, -0....",
    "metadata": {
      "chunkIndex": 2.0,
      "tokenCount": 371.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35.0,
      "kbId": 7.0,
      "startIndex": 651.0,
      "endIndex": 1022.0,
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
    "source": "24f06cc0-b32d-4d6b-a277-4ef3b86977d8",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "24f06cc0-b32d-4d6b-a277-4ef3b86977d8",
    "contentPreview": "嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.52, ..., 0.91] (相似度高) \"今天天气很好\" → [-0.78, 0.23, -0....",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1022,
      "headingPath": "",
      "tokenCount": 371,
      "chunkIndex": 2,
      "title": "rag-technology-guide.md",
      "startIndex": 651,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "e3b0cf86-c288-4870-b3b6-6157ecdb6fd0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "e3b0cf86-c288-4870-b3b6-6157ecdb6fd0",
    "contentPreview": "LM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召回相关文档 → 构建提示词 → LLM 生成答案 → 返回结果 2.1 详细步骤 Step 1: 文档处理（离线） 文档上传 ：PDF、Markdown、Word、代码文件等 文档解析 ：提取文本内容 文本分块 ：将长文档切分为小块（Chunk），通常 500-2000 字符 向量化 ：使用 Embedding 模型将文本转换为向量 存储 ：向量存入向量数据库（Milvus、Pinecone 等） Step 2: 问答处理（在线） 问题向量化 ：用户问题转换为向量 相似度检索 ：在向量数据库中搜...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 684,
      "headingPath": "",
      "tokenCount": 433,
      "chunkIndex": 1,
      "title": "rag-technology-guide.md",
      "startIndex": 251,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "f5056f44-f111-402e-a3b3-fb1b1c4e2d4c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "f5056f44-f111-402e-a3b3-fb1b1c4e2d4c",
    "contentPreview": "e、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文档、技术博客 优势 ：快速查找代码示例、技术方案 7.4 法律文书检索 场景 ：法律条文、判例、合同模板 优势 ：精准检索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2841,
      "headingPath": "",
      "tokenCount": 411,
      "chunkIndex": 9,
      "title": "rag-technology-guide.md",
      "startIndex": 2430,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "1b4bf8bc-15b0-40ab-8b30-5eec811556d2",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "1b4bf8bc-15b0-40ab-8b30-5eec811556d2",
    "contentPreview": "端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2449,
      "headingPath": "",
      "tokenCount": 469,
      "chunkIndex": 8,
      "title": "rag-technology-guide.md",
      "startIndex": 1980,
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
- retrieve hit ratio: `1/2`
- first_match_rank: `1`
- answer: 
- expected_keywords: `["缓存穿透", "不存在的数据", "布隆过滤器", "缓存空值"]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 1.0,
    "source": "b53845a8-7b7e-4425-b022-a44130b1ae67",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 34,
    "chunkId": "b53845a8-7b7e-4425-b022-a44130b1ae67",
    "contentPreview": "锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好 7. 分布式 7.1 分布式锁 实现方...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "kbId": 7,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 34,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 2399,
      "headingPath": "",
      "tokenCount": 495,
      "chunkIndex": 7,
      "title": "java-interview-guide.md",
      "startIndex": 1904
    }
  },
  {
    "rank": 2,
    "score": 0.5222118496894836,
    "source": "5321266b-4613-4675-8953-219c9ec73464",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 33,
    "chunkId": "5321266b-4613-4675-8953-219c9ec73464",
    "contentPreview": "()); } // 每天凌晨 2 点执行 @Scheduled(cron = \"0 0 2 * * ?\") public void cleanupExpiredData() { log.info(\"清理过期数据\"); } } 12. 缓存 12.1 Spring Cache @Configuration @EnableCaching public class CacheConfig { // 配置 Redis 缓存 } @Service public class UserService { @Cacheable(value = \"users\", key = \"#id\") public User...",
    "metadata": {
      "fileName": "springboot-basics.md",
      "kbId": 7,
      "sourceFileName": "springboot-basics.md",
      "documentId": 33,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 5439,
      "headingPath": "",
      "tokenCount": 491,
      "chunkIndex": 16,
      "title": "springboot-basics.md",
      "startIndex": 4948
    }
  },
  {
    "rank": 3,
    "score": 0.5978318452835083,
    "source": "6907ad26-5def-4ec1-a9d7-1b421c5be86c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "6907ad26-5def-4ec1-a9d7-1b421c5be86c",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 270,
      "headingPath": "",
      "tokenCount": 269,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0
    }
  },
  {
    "rank": 4,
    "score": 0.4740881025791168,
    "source": "18130159-6ab4-4bc1-ad95-c9105d5f1ece",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 34,
    "chunkId": "18130159-6ab4-4bc1-ad95-c9105d5f1ece",
    "contentPreview": "4. MyBatis / MyBatis-Plus 4.1 MyBatis 核心 #{} vs ${} ： #{} 预编译，防止 SQL 注入 ${} 字符串替换，存在注入风险 一级缓存 vs 二级缓存 ： 一级缓存：SqlSession 级别，默认开启 二级缓存：Mapper 级别，需要手动开启 4.2 MyBatis-Plus 优势 ： 内置通用 CRUD 方法 代码生成器 分页插件 乐观锁、逻辑删除支持 5.",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "kbId": 7,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 34,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 1565,
      "headingPath": "",
      "tokenCount": 210,
      "chunkIndex": 5,
      "title": "java-interview-guide.md",
      "startIndex": 1354
    }
  },
  {
    "rank": 5,
    "score": 0.42600470781326294,
    "source": "6ea20df0-3ff3-45a5-994e-7addd68f087e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "6ea20df0-3ff3-45a5-994e-7addd68f087e",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2021,
      "headingPath": "",
      "tokenCount": 341,
      "chunkIndex": 7,
      "title": "rag-technology-guide.md",
      "startIndex": 1679
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

### definition-008 (definition)

- id: `definition-008`
- type: `definition`
- question: 什么是分布式锁，文档中列出了哪些实现方式？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["分布式锁", "Redis SETNX + 过期时间"]`
- retrieve hit ratio: `1/2`
- first_match_rank: `1`
- answer: 
- expected_keywords: `["Redis SETNX", "Redisson", "Zookeeper", "过期时间"]`
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
    "documentId": 34,
    "chunkId": "0",
    "contentPreview": "锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好 7. 分布式 7.1 分布式锁 实现方...",
    "metadata": {
      "chunkIndex": 7.0,
      "tokenCount": 495.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 34.0,
      "kbId": 7.0,
      "startIndex": 1904.0,
      "endIndex": 2399.0,
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
    "source": "b53845a8-7b7e-4425-b022-a44130b1ae67",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 34,
    "chunkId": "b53845a8-7b7e-4425-b022-a44130b1ae67",
    "contentPreview": "锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好 7. 分布式 7.1 分布式锁 实现方...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "kbId": 7,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 34,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 2399,
      "headingPath": "",
      "tokenCount": 495,
      "chunkIndex": 7,
      "title": "java-interview-guide.md",
      "startIndex": 1904,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.01587301678955555,
    "source": "aa73fe07-deb6-4b8f-b9af-1efa7fae2f7b",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 34,
    "chunkId": "aa73fe07-deb6-4b8f-b9af-1efa7fae2f7b",
    "contentPreview": "m-Cancel） ： 业务侵入性强 性能较好 Seata ：阿里开源分布式事务框架 8. 消息队列 8.1 RabbitMQ / Kafka 应用场景 ： 异步处理：发送邮件、短信 流量削峰：秒杀系统 系统解耦：订单服务和库存服务 消息可靠性 ： 生产者确认机制 消息持久化 消费者手动 ACK 9. 设计模式 9.1 常用设计模式 单例模式 ：Spring Bean 默认单例 工厂模式 ：BeanFactory 代理模式 ：AOP 模板方法模式 ：JdbcTemplate、RestTemplate 观察者模式 ：Spring 事件监听 策略模式 ：支付方式选择 10. JVM 10.1 内存...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "kbId": 7,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 34,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 2831,
      "headingPath": "",
      "tokenCount": 477,
      "chunkIndex": 8,
      "title": "java-interview-guide.md",
      "startIndex": 2354,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 4,
    "score": 0.015625,
    "source": "d3141a54-acc4-429e-8ee1-91ee14b2e148",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "d3141a54-acc4-429e-8ee1-91ee14b2e148",
    "contentPreview": "和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1447,
      "headingPath": "",
      "tokenCount": 446,
      "chunkIndex": 3,
      "title": "rag-technology-guide.md",
      "startIndex": 1001,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 5,
    "score": 0.016129031777381897,
    "source": "fc4efcd8-b91b-404c-b5b4-b008a3221ddc",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "fc4efcd8-b91b-404c-b5b4-b008a3221ddc",
    "contentPreview": "【相关文档】 {retrieved_context} 【用户提问】 {user_question} 【回答要求】 1. 基于文档内容回答，不要编造信息 2. 如果文档中没有相关信息，明确说明 3. 引用具体的文档片段作为依据 4. 回答要简洁清晰",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1679,
      "headingPath": "",
      "tokenCount": 125,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1550,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
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

### reasoning-003 (reasoning)

- id: `reasoning-003`
- type: `reasoning`
- question: 为什么 RAG 检索后还需要 Rerank？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["重排序（Rerank）", "二次精排，提高准确率"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer: 
- expected_keywords: `["重排序", "二次精排", "提高准确率", "检索优化"]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.01587301678955555,
    "source": "638c0090-847c-4464-9474-346b26a4a2d6",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "638c0090-847c-4464-9474-346b26a4a2d6",
    "contentPreview": ": 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": "",
      "tokenCount": 398,
      "chunkIndex": 10,
      "title": "rag-technology-guide.md",
      "startIndex": 2830,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 2,
    "score": 0.015625,
    "source": "d3141a54-acc4-429e-8ee1-91ee14b2e148",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "d3141a54-acc4-429e-8ee1-91ee14b2e148",
    "contentPreview": "和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1447,
      "headingPath": "",
      "tokenCount": 446,
      "chunkIndex": 3,
      "title": "rag-technology-guide.md",
      "startIndex": 1001,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 3,
    "score": 0.015384615398943424,
    "source": "f5056f44-f111-402e-a3b3-fb1b1c4e2d4c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "f5056f44-f111-402e-a3b3-fb1b1c4e2d4c",
    "contentPreview": "e、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文档、技术博客 优势 ：快速查找代码示例、技术方案 7.4 法律文书检索 场景 ：法律条文、判例、合同模板 优势 ：精准检索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2841,
      "headingPath": "",
      "tokenCount": 411,
      "chunkIndex": 9,
      "title": "rag-technology-guide.md",
      "startIndex": 2430,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015384615384615385
    }
  },
  {
    "rank": 4,
    "score": 0.016393441706895828,
    "source": "1b4bf8bc-15b0-40ab-8b30-5eec811556d2",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "1b4bf8bc-15b0-40ab-8b30-5eec811556d2",
    "contentPreview": "端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2449,
      "headingPath": "",
      "tokenCount": 469,
      "chunkIndex": 8,
      "title": "rag-technology-guide.md",
      "startIndex": 1980,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 5,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "0",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "chunkIndex": 0.0,
      "tokenCount": 269.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35.0,
      "kbId": 7.0,
      "startIndex": 0.0,
      "endIndex": 270.0,
      "originalFilename": "rag-technology-guide.md",
      "fileName": "rag-technology-guide.md",
      "documentTitle": "rag-technology-guide.md",
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "vector"
      ],
      "rrfScore": 0.01639344262295082
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
    "documentId": 34,
    "chunkId": "0",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "chunkIndex": 1.0,
      "tokenCount": 500.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 34.0,
      "kbId": 7.0,
      "startIndex": 18.0,
      "endIndex": 518.0,
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
    "source": "41e7dcc7-a481-4522-af0b-91d7894cab79",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 34,
    "chunkId": "41e7dcc7-a481-4522-af0b-91d7894cab79",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "kbId": 7,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 34,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 518,
      "headingPath": "",
      "tokenCount": 500,
      "chunkIndex": 1,
      "title": "java-interview-guide.md",
      "startIndex": 18,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "d3141a54-acc4-429e-8ee1-91ee14b2e148",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "d3141a54-acc4-429e-8ee1-91ee14b2e148",
    "contentPreview": "和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1447,
      "headingPath": "",
      "tokenCount": 446,
      "chunkIndex": 3,
      "title": "rag-technology-guide.md",
      "startIndex": 1001,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.015625,
    "source": "47b1ccdb-6a87-49f8-b640-30f1b3fc8211",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 34,
    "chunkId": "47b1ccdb-6a87-49f8-b640-30f1b3fc8211",
    "contentPreview": "面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID + Redis Token 机制 乐观锁（版本号） 状态机（订单状态） 5. ...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "kbId": 7,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 34,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 3161,
      "headingPath": "",
      "tokenCount": 358,
      "chunkIndex": 9,
      "title": "java-interview-guide.md",
      "startIndex": 2804,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 5,
    "score": 0.015384615398943424,
    "source": "f5056f44-f111-402e-a3b3-fb1b1c4e2d4c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "f5056f44-f111-402e-a3b3-fb1b1c4e2d4c",
    "contentPreview": "e、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文档、技术博客 优势 ：快速查找代码示例、技术方案 7.4 法律文书检索 场景 ：法律条文、判例、合同模板 优势 ：精准检索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2841,
      "headingPath": "",
      "tokenCount": 411,
      "chunkIndex": 9,
      "title": "rag-technology-guide.md",
      "startIndex": 2430,
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
    "score": 0.01587301678955555,
    "source": "638c0090-847c-4464-9474-346b26a4a2d6",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "638c0090-847c-4464-9474-346b26a4a2d6",
    "contentPreview": ": 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": "",
      "tokenCount": 398,
      "chunkIndex": 10,
      "title": "rag-technology-guide.md",
      "startIndex": 2830,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 2,
    "score": 0.016129031777381897,
    "source": "d3141a54-acc4-429e-8ee1-91ee14b2e148",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "d3141a54-acc4-429e-8ee1-91ee14b2e148",
    "contentPreview": "和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1447,
      "headingPath": "",
      "tokenCount": 446,
      "chunkIndex": 3,
      "title": "rag-technology-guide.md",
      "startIndex": 1001,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 3,
    "score": 0.015625,
    "source": "f5056f44-f111-402e-a3b3-fb1b1c4e2d4c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "f5056f44-f111-402e-a3b3-fb1b1c4e2d4c",
    "contentPreview": "e、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文档、技术博客 优势 ：快速查找代码示例、技术方案 7.4 法律文书检索 场景 ：法律条文、判例、合同模板 优势 ：精准检索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2841,
      "headingPath": "",
      "tokenCount": 411,
      "chunkIndex": 9,
      "title": "rag-technology-guide.md",
      "startIndex": 2430,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 4,
    "score": 0.01515151560306549,
    "source": "1b4bf8bc-15b0-40ab-8b30-5eec811556d2",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "1b4bf8bc-15b0-40ab-8b30-5eec811556d2",
    "contentPreview": "端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2449,
      "headingPath": "",
      "tokenCount": 469,
      "chunkIndex": 8,
      "title": "rag-technology-guide.md",
      "startIndex": 1980,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015151515151515152
    }
  },
  {
    "rank": 5,
    "score": 0.014705882407724857,
    "source": "fc4efcd8-b91b-404c-b5b4-b008a3221ddc",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "fc4efcd8-b91b-404c-b5b4-b008a3221ddc",
    "contentPreview": "【相关文档】 {retrieved_context} 【用户提问】 {user_question} 【回答要求】 1. 基于文档内容回答，不要编造信息 2. 如果文档中没有相关信息，明确说明 3. 引用具体的文档片段作为依据 4. 回答要简洁清晰",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1679,
      "headingPath": "",
      "tokenCount": 125,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1550,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.014705882352941176
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
- retrieve hit ratio: `1/2`
- first_match_rank: `1`
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
    "documentId": 35,
    "chunkId": "0",
    "contentPreview": "端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索...",
    "metadata": {
      "chunkIndex": 8.0,
      "tokenCount": 469.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35.0,
      "kbId": 7.0,
      "startIndex": 1980.0,
      "endIndex": 2449.0,
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
    "source": "1b4bf8bc-15b0-40ab-8b30-5eec811556d2",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "1b4bf8bc-15b0-40ab-8b30-5eec811556d2",
    "contentPreview": "端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2449,
      "headingPath": "",
      "tokenCount": 469,
      "chunkIndex": 8,
      "title": "rag-technology-guide.md",
      "startIndex": 1980,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "6ea20df0-3ff3-45a5-994e-7addd68f087e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "6ea20df0-3ff3-45a5-994e-7addd68f087e",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2021,
      "headingPath": "",
      "tokenCount": 341,
      "chunkIndex": 7,
      "title": "rag-technology-guide.md",
      "startIndex": 1679,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "638c0090-847c-4464-9474-346b26a4a2d6",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "638c0090-847c-4464-9474-346b26a4a2d6",
    "contentPreview": ": 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": "",
      "tokenCount": 398,
      "chunkIndex": 10,
      "title": "rag-technology-guide.md",
      "startIndex": 2830,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.014925372786819935,
    "source": "e3b0cf86-c288-4870-b3b6-6157ecdb6fd0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "e3b0cf86-c288-4870-b3b6-6157ecdb6fd0",
    "contentPreview": "LM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召回相关文档 → 构建提示词 → LLM 生成答案 → 返回结果 2.1 详细步骤 Step 1: 文档处理（离线） 文档上传 ：PDF、Markdown、Word、代码文件等 文档解析 ：提取文本内容 文本分块 ：将长文档切分为小块（Chunk），通常 500-2000 字符 向量化 ：使用 Embedding 模型将文本转换为向量 存储 ：向量存入向量数据库（Milvus、Pinecone 等） Step 2: 问答处理（在线） 问题向量化 ：用户问题转换为向量 相似度检索 ：在向量数据库中搜...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 684,
      "headingPath": "",
      "tokenCount": 433,
      "chunkIndex": 1,
      "title": "rag-technology-guide.md",
      "startIndex": 251,
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

### multi-hop-003 (multi_hop)

- id: `multi-hop-003`
- type: `multi_hop`
- question: 如果接口性能优化选择缓存，Java 面试文档和 Spring Boot 文档分别给了哪些相关手段？
- expected_sources: `["java-interview-guide.md", "springboot-basics.md"]`
- expected_contexts.contains: `["接口性能优化手段", "Spring Cache"]`
- retrieve hit ratio: `1/2`
- first_match_rank: `1`
- answer: 
- expected_keywords: `["Redis", "使用缓存", "@Cacheable", "@CacheEvict", "@CachePut"]`
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
    "documentId": 34,
    "chunkId": "0",
    "contentPreview": "面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID + Redis Token 机制 乐观锁（版本号） 状态机（订单状态） 5. ...",
    "metadata": {
      "chunkIndex": 9.0,
      "tokenCount": 358.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 34.0,
      "kbId": 7.0,
      "startIndex": 2804.0,
      "endIndex": 3161.0,
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
    "source": "47b1ccdb-6a87-49f8-b640-30f1b3fc8211",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 34,
    "chunkId": "47b1ccdb-6a87-49f8-b640-30f1b3fc8211",
    "contentPreview": "面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID + Redis Token 机制 乐观锁（版本号） 状态机（订单状态） 5. ...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "kbId": 7,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 34,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 3161,
      "headingPath": "",
      "tokenCount": 358,
      "chunkIndex": 9,
      "title": "java-interview-guide.md",
      "startIndex": 2804,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.015625,
    "source": "f5056f44-f111-402e-a3b3-fb1b1c4e2d4c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "f5056f44-f111-402e-a3b3-fb1b1c4e2d4c",
    "contentPreview": "e、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文档、技术博客 优势 ：快速查找代码示例、技术方案 7.4 法律文书检索 场景 ：法律条文、判例、合同模板 优势 ：精准检索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2841,
      "headingPath": "",
      "tokenCount": 411,
      "chunkIndex": 9,
      "title": "rag-technology-guide.md",
      "startIndex": 2430,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 4,
    "score": 0.014925372786819935,
    "source": "aa73fe07-deb6-4b8f-b9af-1efa7fae2f7b",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 34,
    "chunkId": "aa73fe07-deb6-4b8f-b9af-1efa7fae2f7b",
    "contentPreview": "m-Cancel） ： 业务侵入性强 性能较好 Seata ：阿里开源分布式事务框架 8. 消息队列 8.1 RabbitMQ / Kafka 应用场景 ： 异步处理：发送邮件、短信 流量削峰：秒杀系统 系统解耦：订单服务和库存服务 消息可靠性 ： 生产者确认机制 消息持久化 消费者手动 ACK 9. 设计模式 9.1 常用设计模式 单例模式 ：Spring Bean 默认单例 工厂模式 ：BeanFactory 代理模式 ：AOP 模板方法模式 ：JdbcTemplate、RestTemplate 观察者模式 ：Spring 事件监听 策略模式 ：支付方式选择 10. JVM 10.1 内存...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "kbId": 7,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 34,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 2831,
      "headingPath": "",
      "tokenCount": 477,
      "chunkIndex": 8,
      "title": "java-interview-guide.md",
      "startIndex": 2354,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.014925373134328358
    }
  },
  {
    "rank": 5,
    "score": 0.01587301678955555,
    "source": "6ea20df0-3ff3-45a5-994e-7addd68f087e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "6ea20df0-3ff3-45a5-994e-7addd68f087e",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2021,
      "headingPath": "",
      "tokenCount": 341,
      "chunkIndex": 7,
      "title": "rag-technology-guide.md",
      "startIndex": 1679,
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
    "source": "6907ad26-5def-4ec1-a9d7-1b421c5be86c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "6907ad26-5def-4ec1-a9d7-1b421c5be86c",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 270,
      "headingPath": "",
      "tokenCount": 269,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0
    }
  },
  {
    "rank": 2,
    "score": 0.6496142745018005,
    "source": "638c0090-847c-4464-9474-346b26a4a2d6",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "638c0090-847c-4464-9474-346b26a4a2d6",
    "contentPreview": ": 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": "",
      "tokenCount": 398,
      "chunkIndex": 10,
      "title": "rag-technology-guide.md",
      "startIndex": 2830
    }
  },
  {
    "rank": 3,
    "score": 0.22468489408493042,
    "source": "d3141a54-acc4-429e-8ee1-91ee14b2e148",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "d3141a54-acc4-429e-8ee1-91ee14b2e148",
    "contentPreview": "和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1447,
      "headingPath": "",
      "tokenCount": 446,
      "chunkIndex": 3,
      "title": "rag-technology-guide.md",
      "startIndex": 1001
    }
  },
  {
    "rank": 4,
    "score": 0.22910824418067932,
    "source": "f5056f44-f111-402e-a3b3-fb1b1c4e2d4c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "f5056f44-f111-402e-a3b3-fb1b1c4e2d4c",
    "contentPreview": "e、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文档、技术博客 优势 ：快速查找代码示例、技术方案 7.4 法律文书检索 场景 ：法律条文、判例、合同模板 优势 ：精准检索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2841,
      "headingPath": "",
      "tokenCount": 411,
      "chunkIndex": 9,
      "title": "rag-technology-guide.md",
      "startIndex": 2430
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
    "source": "41e7dcc7-a481-4522-af0b-91d7894cab79",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 34,
    "chunkId": "41e7dcc7-a481-4522-af0b-91d7894cab79",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "kbId": 7,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 34,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 518,
      "headingPath": "",
      "tokenCount": 500,
      "chunkIndex": 1,
      "title": "java-interview-guide.md",
      "startIndex": 18
    }
  },
  {
    "rank": 2,
    "score": 0.6140515208244324,
    "source": "1b4bf8bc-15b0-40ab-8b30-5eec811556d2",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "1b4bf8bc-15b0-40ab-8b30-5eec811556d2",
    "contentPreview": "端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2449,
      "headingPath": "",
      "tokenCount": 469,
      "chunkIndex": 8,
      "title": "rag-technology-guide.md",
      "startIndex": 1980
    }
  },
  {
    "rank": 3,
    "score": 0.49650880694389343,
    "source": "7a92499a-5163-47df-a3f7-31373f37280b",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 34,
    "chunkId": "7a92499a-5163-47df-a3f7-31373f37280b",
    "contentPreview": "r synchronized vs Lock ： synchronized 自动释放锁，Lock 需要手动释放 Lock 支持更灵活的锁机制（可中断、可超时） volatile 关键字 ： 保证可见性：一个线程修改，其他线程立即可见 禁止指令重排序 不保证原子性 2.",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "kbId": 7,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 34,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 602,
      "headingPath": "",
      "tokenCount": 134,
      "chunkIndex": 2,
      "title": "java-interview-guide.md",
      "startIndex": 468
    }
  },
  {
    "rank": 4,
    "score": 0.3733314573764801,
    "source": "24f06cc0-b32d-4d6b-a277-4ef3b86977d8",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "24f06cc0-b32d-4d6b-a277-4ef3b86977d8",
    "contentPreview": "嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.52, ..., 0.91] (相似度高) \"今天天气很好\" → [-0.78, 0.23, -0....",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1022,
      "headingPath": "",
      "tokenCount": 371,
      "chunkIndex": 2,
      "title": "rag-technology-guide.md",
      "startIndex": 651
    }
  },
  {
    "rank": 5,
    "score": 0.19701653718948364,
    "source": "aa73fe07-deb6-4b8f-b9af-1efa7fae2f7b",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 34,
    "chunkId": "aa73fe07-deb6-4b8f-b9af-1efa7fae2f7b",
    "contentPreview": "m-Cancel） ： 业务侵入性强 性能较好 Seata ：阿里开源分布式事务框架 8. 消息队列 8.1 RabbitMQ / Kafka 应用场景 ： 异步处理：发送邮件、短信 流量削峰：秒杀系统 系统解耦：订单服务和库存服务 消息可靠性 ： 生产者确认机制 消息持久化 消费者手动 ACK 9. 设计模式 9.1 常用设计模式 单例模式 ：Spring Bean 默认单例 工厂模式 ：BeanFactory 代理模式 ：AOP 模板方法模式 ：JdbcTemplate、RestTemplate 观察者模式 ：Spring 事件监听 策略模式 ：支付方式选择 10. JVM 10.1 内存...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "kbId": 7,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 34,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 2831,
      "headingPath": "",
      "tokenCount": 477,
      "chunkIndex": 8,
      "title": "java-interview-guide.md",
      "startIndex": 2354
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
    "source": "3c2f8ea0-88ce-4f91-8458-8b2d04f66b77",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 33,
    "chunkId": "3c2f8ea0-88ce-4f91-8458-8b2d04f66b77",
    "contentPreview": "Spring Boot 核心知识点 1. Spring Boot 简介 Spring Boot 是基于 Spring 框架的快速开发脚手架，旨在简化 Spring 应用的初始搭建和开发过程。 1.1 核心特性 自动配置 ：根据 classpath 下的依赖自动配置 Spring 应用 起步依赖 ：一站式依赖管理，避免版本冲突 内嵌服务器 ：Tomcat、Jetty、Undertow 生产就绪 ：Actuator 健康检查、监控指标 无代码生成 ：不需要 XML 配置 2.",
    "metadata": {
      "fileName": "springboot-basics.md",
      "kbId": 7,
      "sourceFileName": "springboot-basics.md",
      "documentId": 33,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 239,
      "headingPath": "",
      "tokenCount": 238,
      "chunkIndex": 0,
      "title": "springboot-basics.md",
      "startIndex": 0
    }
  },
  {
    "rank": 2,
    "score": 0.8828594088554382,
    "source": "41e7dcc7-a481-4522-af0b-91d7894cab79",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 34,
    "chunkId": "41e7dcc7-a481-4522-af0b-91d7894cab79",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "kbId": 7,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 34,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 518,
      "headingPath": "",
      "tokenCount": 500,
      "chunkIndex": 1,
      "title": "java-interview-guide.md",
      "startIndex": 18
    }
  },
  {
    "rank": 3,
    "score": 0.5149325728416443,
    "source": "6907ad26-5def-4ec1-a9d7-1b421c5be86c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 35,
    "chunkId": "6907ad26-5def-4ec1-a9d7-1b421c5be86c",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "kbId": 7,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 35,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 270,
      "headingPath": "",
      "tokenCount": 269,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0
    }
  },
  {
    "rank": 4,
    "score": 0.5358321666717529,
    "source": "730d8a51-0238-4a0a-b186-429ce81aeded",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 33,
    "chunkId": "730d8a51-0238-4a0a-b186-429ce81aeded",
    "contentPreview": "icy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max = 20, message = \"用户名长度 3-20\")...",
    "metadata": {
      "fileName": "springboot-basics.md",
      "kbId": 7,
      "sourceFileName": "springboot-basics.md",
      "documentId": 33,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 3563,
      "headingPath": "",
      "tokenCount": 465,
      "chunkIndex": 11,
      "title": "springboot-basics.md",
      "startIndex": 3098
    }
  },
  {
    "rank": 5,
    "score": 0.522793173789978,
    "source": "5062a270-ddee-42d4-bb8e-77eafc15e0d3",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 33,
    "chunkId": "5062a270-ddee-42d4-bb8e-77eafc15e0d3",
    "contentPreview": "vice { @Transactional(rollbackFor = Exception.class) public void createOrder(Order order) { orderRepository.save(order); // 如果发生异常，自动回滚 stockService.deduct(order.getProductId(), order.getQuantity()); } } 9.2 事务传播行为 REQUIRED （默认）：加入当前事务，没有则新建 REQUIRES_NEW ：总是新建事务 SUPPORTS ：有事务就加入，没有就非事务执行 NOT_SUPPORT...",
    "metadata": {
      "fileName": "springboot-basics.md",
      "kbId": 7,
      "sourceFileName": "springboot-basics.md",
      "documentId": 33,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 4197,
      "headingPath": "",
      "tokenCount": 449,
      "chunkIndex": 13,
      "title": "springboot-basics.md",
      "startIndex": 3748
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
| fact-001 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: 3c2f8ea0 88ce 4f91 8458 8b2d04f66b77, 3c2f8ea088ce4f9184588b2d04f66b77, springboot basics, springbootbasics; r3: 0967d09b 836f 419c bf2a 3aef9e08db5d, 0967d09b836f419cbf2a3aef9e08db5d, springboot basics, springbootbasics; r4: e0f9ed49 d722 4e32 991f 64b96862301a, e0f9ed49d7224e32991f64b96862301a, java interview guide, javainterviewguide; r5: d9a07100 8421 4328 856a 10996868a285, d9a0710084214328856a10996868a285, java interview guide, javainterviewguide |  |
| fact-002 | springboot-basics.md => springboot basics, springbootbasics; java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, springboot basics, springbootbasics; r2: 23c41b1a f25c 48b9 a891 be280a9af481, 23c41b1af25c48b9a891be280a9af481, springboot basics, springbootbasics; r3: e0f9ed49 d722 4e32 991f 64b96862301a, e0f9ed49d7224e32991f64b96862301a, java interview guide, javainterviewguide; r4: f5ce4456 d970 4949 b70f 6ec009b05c3f, f5ce4456d9704949b70f6ec009b05c3f, springboot basics, springbootbasics; r5: 5062a270 ddee 42d4 bb8e 77eafc15e0d3, 5062a270ddee42d4bb8e77eafc15e0d3, springboot basics, springbootbasics |  |
| fact-003 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: 23c41b1a f25c 48b9 a891 be280a9af481, 23c41b1af25c48b9a891be280a9af481, springboot basics, springbootbasics; r3: 730d8a51 0238 4a0a b186 429ce81aeded, 730d8a5102384a0ab186429ce81aeded, springboot basics, springbootbasics; r4: f5ce4456 d970 4949 b70f 6ec009b05c3f, f5ce4456d9704949b70f6ec009b05c3f, springboot basics, springbootbasics; r5: 5062a270 ddee 42d4 bb8e 77eafc15e0d3, 5062a270ddee42d4bb8e77eafc15e0d3, springboot basics, springbootbasics |  |
| fact-004 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: 6d212d53 547e 49a2 98ee 955571ec3e2d, 6d212d53547e49a298ee955571ec3e2d, springboot basics, springbootbasics; r3: 3c2f8ea0 88ce 4f91 8458 8b2d04f66b77, 3c2f8ea088ce4f9184588b2d04f66b77, springboot basics, springbootbasics; r4: e0f9ed49 d722 4e32 991f 64b96862301a, e0f9ed49d7224e32991f64b96862301a, java interview guide, javainterviewguide; r5: f5056f44 f111 402e a3b3 fb1b1c4e2d4c, f5056f44f111402ea3b3fb1b1c4e2d4c, rag technology guide, ragtechnologyguide |  |
| fact-005 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: 730d8a51 0238 4a0a b186 429ce81aeded, 730d8a5102384a0ab186429ce81aeded, springboot basics, springbootbasics; r3: d3141a54 acc4 429e 8ee1 91ee14b2e148, d3141a54acc4429e8ee191ee14b2e148, rag technology guide, ragtechnologyguide; r4: 638c0090 847c 4464 9474 346b26a4a2d6, 638c0090847c44649474346b26a4a2d6, rag technology guide, ragtechnologyguide; r5: 6907ad26 5def 4ec1 a9d7 1b421c5be86c, 6907ad265def4ec1a9d71b421c5be86c, rag technology guide, ragtechnologyguide |  |
| fact-006 | java-interview-guide.md => java interview guide, javainterviewguide | r1: e0f9ed49 d722 4e32 991f 64b96862301a, e0f9ed49d7224e32991f64b96862301a, java interview guide, javainterviewguide; r2: 0, java interview guide, javainterviewguide; r3: 41e7dcc7 a481 4522 af0b 91d7894cab79, 41e7dcc7a4814522af0b91d7894cab79, java interview guide, javainterviewguide; r4: 34a2b4f1 6bd9 4ede aa5f 4231d328d409, 34a2b4f16bd94edeaa5f4231d328d409, java interview guide, javainterviewguide; r5: d9a07100 8421 4328 856a 10996868a285, d9a0710084214328856a10996868a285, java interview guide, javainterviewguide |  |
| fact-007 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: b53845a8 7b7e 4425 b022 a44130b1ae67, b53845a87b7e4425b022a44130b1ae67, java interview guide, javainterviewguide; r3: 47b1ccdb 6a87 49f8 b640 30f1b3fc8211, 47b1ccdb6a8749f8b64030f1b3fc8211, java interview guide, javainterviewguide; r4: 5321266b 4613 4675 8953 219c9ec73464, 5321266b461346758953219c9ec73464, springboot basics, springbootbasics; r5: 34a2b4f1 6bd9 4ede aa5f 4231d328d409, 34a2b4f16bd94edeaa5f4231d328d409, java interview guide, javainterviewguide |  |
| fact-008 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: e3b0cf86 c288 4870 b3b6 6157ecdb6fd0, e3b0cf86c2884870b3b66157ecdb6fd0, rag technology guide, ragtechnologyguide; r3: 1b4bf8bc 15b0 40ab 8b30 5eec811556d2, 1b4bf8bc15b040ab8b305eec811556d2, rag technology guide, ragtechnologyguide; r4: 24f06cc0 b32d 4d6b a277 4ef3b86977d8, 24f06cc0b32d4d6ba2774ef3b86977d8, rag technology guide, ragtechnologyguide; r5: f5056f44 f111 402e a3b3 fb1b1c4e2d4c, f5056f44f111402ea3b3fb1b1c4e2d4c, rag technology guide, ragtechnologyguide |  |
| fact-009 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: e3b0cf86 c288 4870 b3b6 6157ecdb6fd0, e3b0cf86c2884870b3b66157ecdb6fd0, rag technology guide, ragtechnologyguide; r3: 638c0090 847c 4464 9474 346b26a4a2d6, 638c0090847c44649474346b26a4a2d6, rag technology guide, ragtechnologyguide; r4: 6ea20df0 3ff3 45a5 994e 7addd68f087e, 6ea20df03ff345a5994e7addd68f087e, rag technology guide, ragtechnologyguide; r5: f5056f44 f111 402e a3b3 fb1b1c4e2d4c, f5056f44f111402ea3b3fb1b1c4e2d4c, rag technology guide, ragtechnologyguide |  |
| fact-010 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 34a2b4f1 6bd9 4ede aa5f 4231d328d409, 34a2b4f16bd94edeaa5f4231d328d409, java interview guide, javainterviewguide; r3: d3141a54 acc4 429e 8ee1 91ee14b2e148, d3141a54acc4429e8ee191ee14b2e148, rag technology guide, ragtechnologyguide; r4: 47b1ccdb 6a87 49f8 b640 30f1b3fc8211, 47b1ccdb6a8749f8b64030f1b3fc8211, java interview guide, javainterviewguide; r5: 41e7dcc7 a481 4522 af0b 91d7894cab79, 41e7dcc7a4814522af0b91d7894cab79, java interview guide, javainterviewguide |  |
| definition-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 6907ad26 5def 4ec1 a9d7 1b421c5be86c, 6907ad265def4ec1a9d71b421c5be86c, rag technology guide, ragtechnologyguide; r3: 638c0090 847c 4464 9474 346b26a4a2d6, 638c0090847c44649474346b26a4a2d6, rag technology guide, ragtechnologyguide; r4: 1b4bf8bc 15b0 40ab 8b30 5eec811556d2, 1b4bf8bc15b040ab8b305eec811556d2, rag technology guide, ragtechnologyguide; r5: 1373b31b a798 4214 8149 abced23ca62d, 1373b31ba79842148149abced23ca62d, rag technology guide, ragtechnologyguide |  |
| definition-002 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 24f06cc0 b32d 4d6b a277 4ef3b86977d8, 24f06cc0b32d4d6ba2774ef3b86977d8, rag technology guide, ragtechnologyguide; r3: f5056f44 f111 402e a3b3 fb1b1c4e2d4c, f5056f44f111402ea3b3fb1b1c4e2d4c, rag technology guide, ragtechnologyguide; r4: e3b0cf86 c288 4870 b3b6 6157ecdb6fd0, e3b0cf86c2884870b3b66157ecdb6fd0, rag technology guide, ragtechnologyguide; r5: 1b4bf8bc 15b0 40ab 8b30 5eec811556d2, 1b4bf8bc15b040ab8b305eec811556d2, rag technology guide, ragtechnologyguide |  |
| definition-003 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 24f06cc0 b32d 4d6b a277 4ef3b86977d8, 24f06cc0b32d4d6ba2774ef3b86977d8, rag technology guide, ragtechnologyguide; r3: e3b0cf86 c288 4870 b3b6 6157ecdb6fd0, e3b0cf86c2884870b3b66157ecdb6fd0, rag technology guide, ragtechnologyguide; r4: f5056f44 f111 402e a3b3 fb1b1c4e2d4c, f5056f44f111402ea3b3fb1b1c4e2d4c, rag technology guide, ragtechnologyguide; r5: 1b4bf8bc 15b0 40ab 8b30 5eec811556d2, 1b4bf8bc15b040ab8b305eec811556d2, rag technology guide, ragtechnologyguide |  |
| definition-004 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: d3141a54 acc4 429e 8ee1 91ee14b2e148, d3141a54acc4429e8ee191ee14b2e148, rag technology guide, ragtechnologyguide; r2: 0, rag technology guide, ragtechnologyguide; r3: e3b0cf86 c288 4870 b3b6 6157ecdb6fd0, e3b0cf86c2884870b3b66157ecdb6fd0, rag technology guide, ragtechnologyguide; r4: 638c0090 847c 4464 9474 346b26a4a2d6, 638c0090847c44649474346b26a4a2d6, rag technology guide, ragtechnologyguide; r5: f5056f44 f111 402e a3b3 fb1b1c4e2d4c, f5056f44f111402ea3b3fb1b1c4e2d4c, rag technology guide, ragtechnologyguide |  |
| definition-005 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: d9a07100 8421 4328 856a 10996868a285, d9a0710084214328856a10996868a285, java interview guide, javainterviewguide; r3: 3c2f8ea0 88ce 4f91 8458 8b2d04f66b77, 3c2f8ea088ce4f9184588b2d04f66b77, springboot basics, springbootbasics; r4: f5056f44 f111 402e a3b3 fb1b1c4e2d4c, f5056f44f111402ea3b3fb1b1c4e2d4c, rag technology guide, ragtechnologyguide; r5: 0967d09b 836f 419c bf2a 3aef9e08db5d, 0967d09b836f419cbf2a3aef9e08db5d, springboot basics, springbootbasics |  |
| definition-006 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: f5056f44 f111 402e a3b3 fb1b1c4e2d4c, f5056f44f111402ea3b3fb1b1c4e2d4c, rag technology guide, ragtechnologyguide; r3: d9a07100 8421 4328 856a 10996868a285, d9a0710084214328856a10996868a285, java interview guide, javainterviewguide; r4: aa73fe07 deb6 4b8f b9af 1efa7fae2f7b, aa73fe07deb64b8fb9af1efa7fae2f7b, java interview guide, javainterviewguide; r5: d3141a54 acc4 429e 8ee1 91ee14b2e148, d3141a54acc4429e8ee191ee14b2e148, rag technology guide, ragtechnologyguide |  |
| definition-007 | java-interview-guide.md => java interview guide, javainterviewguide | r1: b53845a8 7b7e 4425 b022 a44130b1ae67, b53845a87b7e4425b022a44130b1ae67, java interview guide, javainterviewguide; r2: 5321266b 4613 4675 8953 219c9ec73464, 5321266b461346758953219c9ec73464, springboot basics, springbootbasics; r3: 6907ad26 5def 4ec1 a9d7 1b421c5be86c, 6907ad265def4ec1a9d71b421c5be86c, rag technology guide, ragtechnologyguide; r4: 18130159 6ab4 4bc1 ad95 c9105d5f1ece, 181301596ab44bc1ad95c9105d5f1ece, java interview guide, javainterviewguide; r5: 6ea20df0 3ff3 45a5 994e 7addd68f087e, 6ea20df03ff345a5994e7addd68f087e, rag technology guide, ragtechnologyguide |  |
| definition-008 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: b53845a8 7b7e 4425 b022 a44130b1ae67, b53845a87b7e4425b022a44130b1ae67, java interview guide, javainterviewguide; r3: aa73fe07 deb6 4b8f b9af 1efa7fae2f7b, aa73fe07deb64b8fb9af1efa7fae2f7b, java interview guide, javainterviewguide; r4: d3141a54 acc4 429e 8ee1 91ee14b2e148, d3141a54acc4429e8ee191ee14b2e148, rag technology guide, ragtechnologyguide; r5: fc4efcd8 b91b 404c b5b4 b008a3221ddc, fc4efcd8b91b404cb5b4b008a3221ddc, rag technology guide, ragtechnologyguide |  |
| reasoning-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 638c0090 847c 4464 9474 346b26a4a2d6, 638c0090847c44649474346b26a4a2d6, rag technology guide, ragtechnologyguide; r3: d3141a54 acc4 429e 8ee1 91ee14b2e148, d3141a54acc4429e8ee191ee14b2e148, rag technology guide, ragtechnologyguide; r4: f5056f44 f111 402e a3b3 fb1b1c4e2d4c, f5056f44f111402ea3b3fb1b1c4e2d4c, rag technology guide, ragtechnologyguide; r5: 1373b31b a798 4214 8149 abced23ca62d, 1373b31ba79842148149abced23ca62d, rag technology guide, ragtechnologyguide |  |
| reasoning-002 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 1b4bf8bc 15b0 40ab 8b30 5eec811556d2, 1b4bf8bc15b040ab8b305eec811556d2, rag technology guide, ragtechnologyguide; r3: f5056f44 f111 402e a3b3 fb1b1c4e2d4c, f5056f44f111402ea3b3fb1b1c4e2d4c, rag technology guide, ragtechnologyguide; r4: 6907ad26 5def 4ec1 a9d7 1b421c5be86c, 6907ad265def4ec1a9d71b421c5be86c, rag technology guide, ragtechnologyguide; r5: d3141a54 acc4 429e 8ee1 91ee14b2e148, d3141a54acc4429e8ee191ee14b2e148, rag technology guide, ragtechnologyguide |  |
| reasoning-003 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 638c0090 847c 4464 9474 346b26a4a2d6, 638c0090847c44649474346b26a4a2d6, rag technology guide, ragtechnologyguide; r2: d3141a54 acc4 429e 8ee1 91ee14b2e148, d3141a54acc4429e8ee191ee14b2e148, rag technology guide, ragtechnologyguide; r3: f5056f44 f111 402e a3b3 fb1b1c4e2d4c, f5056f44f111402ea3b3fb1b1c4e2d4c, rag technology guide, ragtechnologyguide; r4: 1b4bf8bc 15b0 40ab 8b30 5eec811556d2, 1b4bf8bc15b040ab8b305eec811556d2, rag technology guide, ragtechnologyguide; r5: 0, rag technology guide, ragtechnologyguide |  |
| reasoning-004 | springboot-basics.md => springboot basics, springbootbasics; java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 6d212d53 547e 49a2 98ee 955571ec3e2d, 6d212d53547e49a298ee955571ec3e2d, springboot basics, springbootbasics; r3: d9a07100 8421 4328 856a 10996868a285, d9a0710084214328856a10996868a285, java interview guide, javainterviewguide; r4: 638c0090 847c 4464 9474 346b26a4a2d6, 638c0090847c44649474346b26a4a2d6, rag technology guide, ragtechnologyguide; r5: f5056f44 f111 402e a3b3 fb1b1c4e2d4c, f5056f44f111402ea3b3fb1b1c4e2d4c, rag technology guide, ragtechnologyguide |  |
| reasoning-005 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 41e7dcc7 a481 4522 af0b 91d7894cab79, 41e7dcc7a4814522af0b91d7894cab79, java interview guide, javainterviewguide; r3: d3141a54 acc4 429e 8ee1 91ee14b2e148, d3141a54acc4429e8ee191ee14b2e148, rag technology guide, ragtechnologyguide; r4: 47b1ccdb 6a87 49f8 b640 30f1b3fc8211, 47b1ccdb6a8749f8b64030f1b3fc8211, java interview guide, javainterviewguide; r5: f5056f44 f111 402e a3b3 fb1b1c4e2d4c, f5056f44f111402ea3b3fb1b1c4e2d4c, rag technology guide, ragtechnologyguide |  |
| reasoning-006 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 638c0090 847c 4464 9474 346b26a4a2d6, 638c0090847c44649474346b26a4a2d6, rag technology guide, ragtechnologyguide; r2: d3141a54 acc4 429e 8ee1 91ee14b2e148, d3141a54acc4429e8ee191ee14b2e148, rag technology guide, ragtechnologyguide; r3: f5056f44 f111 402e a3b3 fb1b1c4e2d4c, f5056f44f111402ea3b3fb1b1c4e2d4c, rag technology guide, ragtechnologyguide; r4: 1b4bf8bc 15b0 40ab 8b30 5eec811556d2, 1b4bf8bc15b040ab8b305eec811556d2, rag technology guide, ragtechnologyguide; r5: fc4efcd8 b91b 404c b5b4 b008a3221ddc, fc4efcd8b91b404cb5b4b008a3221ddc, rag technology guide, ragtechnologyguide |  |
| multi-hop-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: e3b0cf86 c288 4870 b3b6 6157ecdb6fd0, e3b0cf86c2884870b3b66157ecdb6fd0, rag technology guide, ragtechnologyguide; r3: 638c0090 847c 4464 9474 346b26a4a2d6, 638c0090847c44649474346b26a4a2d6, rag technology guide, ragtechnologyguide; r4: f5056f44 f111 402e a3b3 fb1b1c4e2d4c, f5056f44f111402ea3b3fb1b1c4e2d4c, rag technology guide, ragtechnologyguide; r5: 1b4bf8bc 15b0 40ab 8b30 5eec811556d2, 1b4bf8bc15b040ab8b305eec811556d2, rag technology guide, ragtechnologyguide |  |
| multi-hop-002 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 1b4bf8bc 15b0 40ab 8b30 5eec811556d2, 1b4bf8bc15b040ab8b305eec811556d2, rag technology guide, ragtechnologyguide; r3: 6ea20df0 3ff3 45a5 994e 7addd68f087e, 6ea20df03ff345a5994e7addd68f087e, rag technology guide, ragtechnologyguide; r4: 638c0090 847c 4464 9474 346b26a4a2d6, 638c0090847c44649474346b26a4a2d6, rag technology guide, ragtechnologyguide; r5: e3b0cf86 c288 4870 b3b6 6157ecdb6fd0, e3b0cf86c2884870b3b66157ecdb6fd0, rag technology guide, ragtechnologyguide |  |
| multi-hop-003 | java-interview-guide.md => java interview guide, javainterviewguide; springboot-basics.md => springboot basics, springbootbasics | r1: 0, java interview guide, javainterviewguide; r2: 47b1ccdb 6a87 49f8 b640 30f1b3fc8211, 47b1ccdb6a8749f8b64030f1b3fc8211, java interview guide, javainterviewguide; r3: f5056f44 f111 402e a3b3 fb1b1c4e2d4c, f5056f44f111402ea3b3fb1b1c4e2d4c, rag technology guide, ragtechnologyguide; r4: aa73fe07 deb6 4b8f b9af 1efa7fae2f7b, aa73fe07deb64b8fb9af1efa7fae2f7b, java interview guide, javainterviewguide; r5: 6ea20df0 3ff3 45a5 994e 7addd68f087e, 6ea20df03ff345a5994e7addd68f087e, rag technology guide, ragtechnologyguide |  |
| no-answer-001 |  | r1: 6907ad26 5def 4ec1 a9d7 1b421c5be86c, 6907ad265def4ec1a9d71b421c5be86c, rag technology guide, ragtechnologyguide; r2: 638c0090 847c 4464 9474 346b26a4a2d6, 638c0090847c44649474346b26a4a2d6, rag technology guide, ragtechnologyguide; r3: d3141a54 acc4 429e 8ee1 91ee14b2e148, d3141a54acc4429e8ee191ee14b2e148, rag technology guide, ragtechnologyguide; r4: f5056f44 f111 402e a3b3 fb1b1c4e2d4c, f5056f44f111402ea3b3fb1b1c4e2d4c, rag technology guide, ragtechnologyguide |  |
| no-answer-002 |  | r1: 41e7dcc7 a481 4522 af0b 91d7894cab79, 41e7dcc7a4814522af0b91d7894cab79, java interview guide, javainterviewguide; r2: 1b4bf8bc 15b0 40ab 8b30 5eec811556d2, 1b4bf8bc15b040ab8b305eec811556d2, rag technology guide, ragtechnologyguide; r3: 7a92499a 5163 47df a3f7 31373f37280b, 7a92499a516347dfa3f731373f37280b, java interview guide, javainterviewguide; r4: 24f06cc0 b32d 4d6b a277 4ef3b86977d8, 24f06cc0b32d4d6ba2774ef3b86977d8, rag technology guide, ragtechnologyguide; r5: aa73fe07 deb6 4b8f b9af 1efa7fae2f7b, aa73fe07deb64b8fb9af1efa7fae2f7b, java interview guide, javainterviewguide |  |
| no-answer-003 |  | r1: 3c2f8ea0 88ce 4f91 8458 8b2d04f66b77, 3c2f8ea088ce4f9184588b2d04f66b77, springboot basics, springbootbasics; r2: 41e7dcc7 a481 4522 af0b 91d7894cab79, 41e7dcc7a4814522af0b91d7894cab79, java interview guide, javainterviewguide; r3: 6907ad26 5def 4ec1 a9d7 1b421c5be86c, 6907ad265def4ec1a9d71b421c5be86c, rag technology guide, ragtechnologyguide; r4: 730d8a51 0238 4a0a b186 429ce81aeded, 730d8a5102384a0ab186429ce81aeded, springboot basics, springbootbasics; r5: 5062a270 ddee 42d4 bb8e 77eafc15e0d3, 5062a270ddee42d4bb8e77eafc15e0d3, springboot basics, springbootbasics |  |

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
