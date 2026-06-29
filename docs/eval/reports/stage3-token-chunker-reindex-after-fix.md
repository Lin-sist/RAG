# RAG Eval Report

- Generated at: 2026-06-29T10:49:23.961365+00:00
- Report status: `RETRIEVAL_ONLY`
- askErrors count: `0`
- retrieveErrors count: `0`
- skippedAsk count: `30`
- rateLimitErrors count: `0`
- retry count: `0`
- Metrics safe for comparison: `retrieval metrics only`
- Base URL: `http://localhost:8080`
- Knowledge base ID: `8`
- Eval set: `docs\eval\rag_eval_set.jsonl`
- topK: `5`
- minScore: `0.3`
- enableRerank: `True`
- skipAsk: `True`
- askDelaySeconds: `0.0`
- maxAskRetries: `0`
- retryBackoffSeconds: `0.0`
- Duration: `33.43s`

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
    "documentId": 29,
    "chunkId": "0",
    "contentPreview": "ctuator 健康检查、监控指标 无代码生成 ：不需要 XML 配置 2. 核心注解 2.1 启动类注解 @SpringBootApplication public class Application { public static void main(String[] args) { SpringApplication.run(Application.class, args); } } @SpringBootApplication 是组合注解，包含： @Configuration ：标记为配置类 @EnableAutoConfiguration ：启用自动配置 @ComponentScan...",
    "metadata": {
      "chunkIndex": 1.0,
      "tokenCount": 528.0,
      "headingPath": "",
      "sourceFileName": "springboot-basics.md",
      "documentId": 29.0,
      "kbId": 8.0,
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
    "source": "acec5be0-6186-4f5d-82de-fa3ae907d2f2",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 29,
    "chunkId": "acec5be0-6186-4f5d-82de-fa3ae907d2f2",
    "contentPreview": "ctuator 健康检查、监控指标 无代码生成 ：不需要 XML 配置 2. 核心注解 2.1 启动类注解 @SpringBootApplication public class Application { public static void main(String[] args) { SpringApplication.run(Application.class, args); } } @SpringBootApplication 是组合注解，包含： @Configuration ：标记为配置类 @EnableAutoConfiguration ：启用自动配置 @ComponentScan...",
    "metadata": {
      "fileName": "springboot-basics.md",
      "chunkIndex": 1,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "springboot-basics.md",
      "documentId": 29,
      "endIndex": 729,
      "startIndex": 201,
      "title": "springboot-basics.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "2a8877a8-9790-41fd-b5fa-191056171dc6",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 29,
    "chunkId": "2a8877a8-9790-41fd-b5fa-191056171dc6",
    "contentPreview": "icy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max = 20, message = \"用户名长度 3-20\")...",
    "metadata": {
      "fileName": "springboot-basics.md",
      "chunkIndex": 11,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "springboot-basics.md",
      "documentId": 29,
      "endIndex": 3563,
      "startIndex": 3098,
      "title": "springboot-basics.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "497fc809-30f6-48c6-a2d2-a72201f46543",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 29,
    "chunkId": "497fc809-30f6-48c6-a2d2-a72201f46543",
    "contentPreview": "e public class EmailService { @Async public CompletableFuture<String> sendEmail(String to, String subject) { // 异步发送邮件 return CompletableFuture.completedFuture(\"SUCCESS\"); } } 11. 定时任务 11.1 @Scheduled 注解 @Component public class ScheduledTasks { // 每 5 秒执行一次 @Scheduled(fixedRate = 5000) public void r...",
    "metadata": {
      "fileName": "springboot-basics.md",
      "chunkIndex": 15,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "springboot-basics.md",
      "documentId": 29,
      "endIndex": 4958,
      "startIndex": 4548,
      "title": "springboot-basics.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "7f0fccc7-0962-4795-ad72-4f7cf92c3784",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 29,
    "chunkId": "7f0fccc7-0962-4795-ad72-4f7cf92c3784",
    "contentPreview": "vice { @Transactional(rollbackFor = Exception.class) public void createOrder(Order order) { orderRepository.save(order); // 如果发生异常，自动回滚 stockService.deduct(order.getProductId(), order.getQuantity()); } } 9.2 事务传播行为 REQUIRED （默认）：加入当前事务，没有则新建 REQUIRES_NEW ：总是新建事务 SUPPORTS ：有事务就加入，没有就非事务执行 NOT_SUPPORT...",
    "metadata": {
      "fileName": "springboot-basics.md",
      "chunkIndex": 13,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "springboot-basics.md",
      "documentId": 29,
      "endIndex": 4197,
      "startIndex": 3748,
      "title": "springboot-basics.md",
      "kbId": 8,
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
    "documentId": 29,
    "chunkId": "0",
    "contentPreview": "icy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max = 20, message = \"用户名长度 3-20\")...",
    "metadata": {
      "chunkIndex": 11.0,
      "tokenCount": 465.0,
      "headingPath": "",
      "sourceFileName": "springboot-basics.md",
      "documentId": 29.0,
      "kbId": 8.0,
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
    "source": "2a8877a8-9790-41fd-b5fa-191056171dc6",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 29,
    "chunkId": "2a8877a8-9790-41fd-b5fa-191056171dc6",
    "contentPreview": "icy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max = 20, message = \"用户名长度 3-20\")...",
    "metadata": {
      "fileName": "springboot-basics.md",
      "chunkIndex": 11,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "springboot-basics.md",
      "documentId": 29,
      "endIndex": 3563,
      "startIndex": 3098,
      "title": "springboot-basics.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.015384615398943424,
    "source": "ffac4de1-10a7-47c6-911d-11f31d611867",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "ffac4de1-10a7-47c6-911d-11f31d611867",
    "contentPreview": "和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 3,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 1447,
      "startIndex": 1001,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015384615384615385
    }
  },
  {
    "rank": 4,
    "score": 0.016129031777381897,
    "source": "a61f5990-2fff-4bff-ae1b-fefa6f5c25b5",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "a61f5990-2fff-4bff-ae1b-fefa6f5c25b5",
    "contentPreview": ": 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 3227,
      "startIndex": 2830,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 5,
    "score": 0.01587301678955555,
    "source": "c671ccb2-583d-4b53-9372-7a0e75372265",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "c671ccb2-583d-4b53-9372-7a0e75372265",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 0,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 270,
      "startIndex": 0,
      "title": "rag-technology-guide.md",
      "kbId": 8,
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
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好 7. 分布式 7.1 分布式锁 实现方...",
    "metadata": {
      "chunkIndex": 7.0,
      "tokenCount": 495.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 30.0,
      "kbId": 8.0,
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
    "source": "03b40cc4-327b-424d-b69b-814a9f92bee8",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 30,
    "chunkId": "03b40cc4-327b-424d-b69b-814a9f92bee8",
    "contentPreview": "锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好 7. 分布式 7.1 分布式锁 实现方...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "chunkIndex": 7,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "java-interview-guide.md",
      "documentId": 30,
      "endIndex": 2399,
      "startIndex": 1904,
      "title": "java-interview-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "e5195a54-3b10-48fd-8fde-7eea7d0984f2",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 30,
    "chunkId": "e5195a54-3b10-48fd-8fde-7eea7d0984f2",
    "contentPreview": "面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID + Redis Token 机制 乐观锁（版本号） 状态机（订单状态） 5. ...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "chunkIndex": 9,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "java-interview-guide.md",
      "documentId": 30,
      "endIndex": 3161,
      "startIndex": 2804,
      "title": "java-interview-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "57dfec95-3dc0-4847-9d1c-d5cb0dac6194",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 29,
    "chunkId": "57dfec95-3dc0-4847-9d1c-d5cb0dac6194",
    "contentPreview": "()); } // 每天凌晨 2 点执行 @Scheduled(cron = \"0 0 2 * * ?\") public void cleanupExpiredData() { log.info(\"清理过期数据\"); } } 12. 缓存 12.1 Spring Cache @Configuration @EnableCaching public class CacheConfig { // 配置 Redis 缓存 } @Service public class UserService { @Cacheable(value = \"users\", key = \"#id\") public User...",
    "metadata": {
      "fileName": "springboot-basics.md",
      "chunkIndex": 16,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "springboot-basics.md",
      "documentId": 29,
      "endIndex": 5439,
      "startIndex": 4948,
      "title": "springboot-basics.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015384615398943424,
    "source": "c193a52d-cacb-42db-a104-dccc395a8dd2",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 30,
    "chunkId": "c193a52d-cacb-42db-a104-dccc395a8dd2",
    "contentPreview": "锁、逻辑删除支持 5. MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNCOMMITTED（读未提交） READ COMMITTED（读已提交） REPEATABLE READ（可...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "chunkIndex": 6,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "java-interview-guide.md",
      "documentId": 30,
      "endIndex": 1921,
      "startIndex": 1554,
      "title": "java-interview-guide.md",
      "kbId": 8,
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
    "documentId": 31,
    "chunkId": "0",
    "contentPreview": "端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索...",
    "metadata": {
      "chunkIndex": 8.0,
      "tokenCount": 469.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 31.0,
      "kbId": 8.0,
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
    "source": "1611f65d-0c32-4ac3-826b-5488a154914b",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "1611f65d-0c32-4ac3-826b-5488a154914b",
    "contentPreview": "LM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召回相关文档 → 构建提示词 → LLM 生成答案 → 返回结果 2.1 详细步骤 Step 1: 文档处理（离线） 文档上传 ：PDF、Markdown、Word、代码文件等 文档解析 ：提取文本内容 文本分块 ：将长文档切分为小块（Chunk），通常 500-2000 字符 向量化 ：使用 Embedding 模型将文本转换为向量 存储 ：向量存入向量数据库（Milvus、Pinecone 等） Step 2: 问答处理（在线） 问题向量化 ：用户问题转换为向量 相似度检索 ：在向量数据库中搜...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 1,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 684,
      "startIndex": 251,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.01587301678955555,
    "source": "524644ce-3237-4fdf-8b48-ef1aca4578af",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "524644ce-3237-4fdf-8b48-ef1aca4578af",
    "contentPreview": "端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 8,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 2449,
      "startIndex": 1980,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 4,
    "score": 0.016129031777381897,
    "source": "226d4a6c-7e59-43af-b961-77960cb3650f",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "226d4a6c-7e59-43af-b961-77960cb3650f",
    "contentPreview": "嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.52, ..., 0.91] (相似度高) \"今天天气很好\" → [-0.78, 0.23, -0....",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 2,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 1022,
      "startIndex": 651,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "5505ce35-75df-46c8-bfcb-e58c791676ef",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "5505ce35-75df-46c8-bfcb-e58c791676ef",
    "contentPreview": "e、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文档、技术博客 优势 ：快速查找代码示例、技术方案 7.4 法律文书检索 场景 ：法律条文、判例、合同模板 优势 ：精准检索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 9,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 2841,
      "startIndex": 2430,
      "title": "rag-technology-guide.md",
      "kbId": 8,
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
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "锁、逻辑删除支持 5. MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNCOMMITTED（读未提交） READ COMMITTED（读已提交） REPEATABLE READ（可...",
    "metadata": {
      "chunkIndex": 6.0,
      "tokenCount": 367.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 30.0,
      "kbId": 8.0,
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
    "source": "c193a52d-cacb-42db-a104-dccc395a8dd2",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 30,
    "chunkId": "c193a52d-cacb-42db-a104-dccc395a8dd2",
    "contentPreview": "锁、逻辑删除支持 5. MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNCOMMITTED（读未提交） READ COMMITTED（读已提交） REPEATABLE READ（可...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "chunkIndex": 6,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "java-interview-guide.md",
      "documentId": 30,
      "endIndex": 1921,
      "startIndex": 1554,
      "title": "java-interview-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "ffac4de1-10a7-47c6-911d-11f31d611867",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "ffac4de1-10a7-47c6-911d-11f31d611867",
    "contentPreview": "和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 3,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 1447,
      "startIndex": 1001,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.015625,
    "source": "e5195a54-3b10-48fd-8fde-7eea7d0984f2",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 30,
    "chunkId": "e5195a54-3b10-48fd-8fde-7eea7d0984f2",
    "contentPreview": "面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID + Redis Token 机制 乐观锁（版本号） 状态机（订单状态） 5. ...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "chunkIndex": 9,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "java-interview-guide.md",
      "documentId": 30,
      "endIndex": 3161,
      "startIndex": 2804,
      "title": "java-interview-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 5,
    "score": 0.014925372786819935,
    "source": "d5a74410-0e4a-439d-90ab-e2693a0989b6",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 30,
    "chunkId": "d5a74410-0e4a-439d-90ab-e2693a0989b6",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "chunkIndex": 1,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "java-interview-guide.md",
      "documentId": 30,
      "endIndex": 518,
      "startIndex": 18,
      "title": "java-interview-guide.md",
      "kbId": 8,
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
    "documentId": 31,
    "chunkId": "0",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "chunkIndex": 0.0,
      "tokenCount": 269.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 31.0,
      "kbId": 8.0,
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
    "source": "c671ccb2-583d-4b53-9372-7a0e75372265",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "c671ccb2-583d-4b53-9372-7a0e75372265",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 0,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 270,
      "startIndex": 0,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "a61f5990-2fff-4bff-ae1b-fefa6f5c25b5",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "a61f5990-2fff-4bff-ae1b-fefa6f5c25b5",
    "contentPreview": ": 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 3227,
      "startIndex": 2830,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "524644ce-3237-4fdf-8b48-ef1aca4578af",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "524644ce-3237-4fdf-8b48-ef1aca4578af",
    "contentPreview": "端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 8,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 2449,
      "startIndex": 1980,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "23b246a1-f049-4275-a4d3-4103dc69ed24",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "23b246a1-f049-4275-a4d3-4103dc69ed24",
    "contentPreview": "：技术原理 Chunk 3（500 字）：应用场景 Chunk 4（500 字）：总结 4. 提示词工程（Prompt Engineering） RAG 系统需要精心设计提示词，将检索到的上下文和问题结合。 4.1 典型 Prompt 模板 你是一个专业的技术助手。请根据以下文档内容回答用户的问题。",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 4,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 1550,
      "startIndex": 1401,
      "title": "rag-technology-guide.md",
      "kbId": 8,
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
    "documentId": 31,
    "chunkId": "0",
    "contentPreview": "嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.52, ..., 0.91] (相似度高) \"今天天气很好\" → [-0.78, 0.23, -0....",
    "metadata": {
      "chunkIndex": 2.0,
      "tokenCount": 371.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 31.0,
      "kbId": 8.0,
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
    "source": "226d4a6c-7e59-43af-b961-77960cb3650f",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "226d4a6c-7e59-43af-b961-77960cb3650f",
    "contentPreview": "嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.52, ..., 0.91] (相似度高) \"今天天气很好\" → [-0.78, 0.23, -0....",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 2,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 1022,
      "startIndex": 651,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "1611f65d-0c32-4ac3-826b-5488a154914b",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "1611f65d-0c32-4ac3-826b-5488a154914b",
    "contentPreview": "LM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召回相关文档 → 构建提示词 → LLM 生成答案 → 返回结果 2.1 详细步骤 Step 1: 文档处理（离线） 文档上传 ：PDF、Markdown、Word、代码文件等 文档解析 ：提取文本内容 文本分块 ：将长文档切分为小块（Chunk），通常 500-2000 字符 向量化 ：使用 Embedding 模型将文本转换为向量 存储 ：向量存入向量数据库（Milvus、Pinecone 等） Step 2: 问答处理（在线） 问题向量化 ：用户问题转换为向量 相似度检索 ：在向量数据库中搜...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 1,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 684,
      "startIndex": 251,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "5505ce35-75df-46c8-bfcb-e58c791676ef",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "5505ce35-75df-46c8-bfcb-e58c791676ef",
    "contentPreview": "e、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文档、技术博客 优势 ：快速查找代码示例、技术方案 7.4 法律文书检索 场景 ：法律条文、判例、合同模板 优势 ：精准检索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 9,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 2841,
      "startIndex": 2430,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "524644ce-3237-4fdf-8b48-ef1aca4578af",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "524644ce-3237-4fdf-8b48-ef1aca4578af",
    "contentPreview": "端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 8,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 2449,
      "startIndex": 1980,
      "title": "rag-technology-guide.md",
      "kbId": 8,
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
    "source": "03b40cc4-327b-424d-b69b-814a9f92bee8",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 30,
    "chunkId": "03b40cc4-327b-424d-b69b-814a9f92bee8",
    "contentPreview": "锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好 7. 分布式 7.1 分布式锁 实现方...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "chunkIndex": 7,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "java-interview-guide.md",
      "documentId": 30,
      "endIndex": 2399,
      "startIndex": 1904,
      "title": "java-interview-guide.md",
      "kbId": 8
    }
  },
  {
    "rank": 2,
    "score": 0.5222118496894836,
    "source": "57dfec95-3dc0-4847-9d1c-d5cb0dac6194",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 29,
    "chunkId": "57dfec95-3dc0-4847-9d1c-d5cb0dac6194",
    "contentPreview": "()); } // 每天凌晨 2 点执行 @Scheduled(cron = \"0 0 2 * * ?\") public void cleanupExpiredData() { log.info(\"清理过期数据\"); } } 12. 缓存 12.1 Spring Cache @Configuration @EnableCaching public class CacheConfig { // 配置 Redis 缓存 } @Service public class UserService { @Cacheable(value = \"users\", key = \"#id\") public User...",
    "metadata": {
      "fileName": "springboot-basics.md",
      "chunkIndex": 16,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "springboot-basics.md",
      "documentId": 29,
      "endIndex": 5439,
      "startIndex": 4948,
      "title": "springboot-basics.md",
      "kbId": 8
    }
  },
  {
    "rank": 3,
    "score": 0.5978318452835083,
    "source": "c671ccb2-583d-4b53-9372-7a0e75372265",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "c671ccb2-583d-4b53-9372-7a0e75372265",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 0,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 270,
      "startIndex": 0,
      "title": "rag-technology-guide.md",
      "kbId": 8
    }
  },
  {
    "rank": 4,
    "score": 0.4740881025791168,
    "source": "2ed239f6-2452-44ad-93f9-6789ec770b06",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 30,
    "chunkId": "2ed239f6-2452-44ad-93f9-6789ec770b06",
    "contentPreview": "4. MyBatis / MyBatis-Plus 4.1 MyBatis 核心 #{} vs ${} ： #{} 预编译，防止 SQL 注入 ${} 字符串替换，存在注入风险 一级缓存 vs 二级缓存 ： 一级缓存：SqlSession 级别，默认开启 二级缓存：Mapper 级别，需要手动开启 4.2 MyBatis-Plus 优势 ： 内置通用 CRUD 方法 代码生成器 分页插件 乐观锁、逻辑删除支持 5.",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "chunkIndex": 5,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "java-interview-guide.md",
      "documentId": 30,
      "endIndex": 1565,
      "startIndex": 1354,
      "title": "java-interview-guide.md",
      "kbId": 8
    }
  },
  {
    "rank": 5,
    "score": 0.42600470781326294,
    "source": "c9a275d0-1518-4319-a734-82e36c288a49",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "c9a275d0-1518-4319-a734-82e36c288a49",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 7,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 2021,
      "startIndex": 1679,
      "title": "rag-technology-guide.md",
      "kbId": 8
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
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好 7. 分布式 7.1 分布式锁 实现方...",
    "metadata": {
      "chunkIndex": 7.0,
      "tokenCount": 495.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 30.0,
      "kbId": 8.0,
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
    "source": "03b40cc4-327b-424d-b69b-814a9f92bee8",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 30,
    "chunkId": "03b40cc4-327b-424d-b69b-814a9f92bee8",
    "contentPreview": "锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好 7. 分布式 7.1 分布式锁 实现方...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "chunkIndex": 7,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "java-interview-guide.md",
      "documentId": 30,
      "endIndex": 2399,
      "startIndex": 1904,
      "title": "java-interview-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.01587301678955555,
    "source": "b6b4b8ac-efe5-4714-b616-4102ec2b791f",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 30,
    "chunkId": "b6b4b8ac-efe5-4714-b616-4102ec2b791f",
    "contentPreview": "m-Cancel） ： 业务侵入性强 性能较好 Seata ：阿里开源分布式事务框架 8. 消息队列 8.1 RabbitMQ / Kafka 应用场景 ： 异步处理：发送邮件、短信 流量削峰：秒杀系统 系统解耦：订单服务和库存服务 消息可靠性 ： 生产者确认机制 消息持久化 消费者手动 ACK 9. 设计模式 9.1 常用设计模式 单例模式 ：Spring Bean 默认单例 工厂模式 ：BeanFactory 代理模式 ：AOP 模板方法模式 ：JdbcTemplate、RestTemplate 观察者模式 ：Spring 事件监听 策略模式 ：支付方式选择 10. JVM 10.1 内存...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "chunkIndex": 8,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "java-interview-guide.md",
      "documentId": 30,
      "endIndex": 2831,
      "startIndex": 2354,
      "title": "java-interview-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 4,
    "score": 0.015625,
    "source": "ffac4de1-10a7-47c6-911d-11f31d611867",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "ffac4de1-10a7-47c6-911d-11f31d611867",
    "contentPreview": "和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 3,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 1447,
      "startIndex": 1001,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 5,
    "score": 0.016129031777381897,
    "source": "a6625763-d39e-448d-9671-b9e2af5d582c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "a6625763-d39e-448d-9671-b9e2af5d582c",
    "contentPreview": "【相关文档】 {retrieved_context} 【用户提问】 {user_question} 【回答要求】 1. 基于文档内容回答，不要编造信息 2. 如果文档中没有相关信息，明确说明 3. 引用具体的文档片段作为依据 4. 回答要简洁清晰",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 5,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 1679,
      "startIndex": 1550,
      "title": "rag-technology-guide.md",
      "kbId": 8,
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
    "source": "a61f5990-2fff-4bff-ae1b-fefa6f5c25b5",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "a61f5990-2fff-4bff-ae1b-fefa6f5c25b5",
    "contentPreview": ": 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 3227,
      "startIndex": 2830,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 2,
    "score": 0.015625,
    "source": "ffac4de1-10a7-47c6-911d-11f31d611867",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "ffac4de1-10a7-47c6-911d-11f31d611867",
    "contentPreview": "和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 3,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 1447,
      "startIndex": 1001,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 3,
    "score": 0.015384615398943424,
    "source": "5505ce35-75df-46c8-bfcb-e58c791676ef",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "5505ce35-75df-46c8-bfcb-e58c791676ef",
    "contentPreview": "e、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文档、技术博客 优势 ：快速查找代码示例、技术方案 7.4 法律文书检索 场景 ：法律条文、判例、合同模板 优势 ：精准检索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 9,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 2841,
      "startIndex": 2430,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015384615384615385
    }
  },
  {
    "rank": 4,
    "score": 0.016393441706895828,
    "source": "524644ce-3237-4fdf-8b48-ef1aca4578af",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "524644ce-3237-4fdf-8b48-ef1aca4578af",
    "contentPreview": "端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 8,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 2449,
      "startIndex": 1980,
      "title": "rag-technology-guide.md",
      "kbId": 8,
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
    "documentId": 31,
    "chunkId": "0",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "chunkIndex": 0.0,
      "tokenCount": 269.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 31.0,
      "kbId": 8.0,
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
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "chunkIndex": 1.0,
      "tokenCount": 500.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 30.0,
      "kbId": 8.0,
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
    "source": "d5a74410-0e4a-439d-90ab-e2693a0989b6",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 30,
    "chunkId": "d5a74410-0e4a-439d-90ab-e2693a0989b6",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "chunkIndex": 1,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "java-interview-guide.md",
      "documentId": 30,
      "endIndex": 518,
      "startIndex": 18,
      "title": "java-interview-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "ffac4de1-10a7-47c6-911d-11f31d611867",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "ffac4de1-10a7-47c6-911d-11f31d611867",
    "contentPreview": "和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 3,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 1447,
      "startIndex": 1001,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.015625,
    "source": "e5195a54-3b10-48fd-8fde-7eea7d0984f2",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 30,
    "chunkId": "e5195a54-3b10-48fd-8fde-7eea7d0984f2",
    "contentPreview": "面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID + Redis Token 机制 乐观锁（版本号） 状态机（订单状态） 5. ...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "chunkIndex": 9,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "java-interview-guide.md",
      "documentId": 30,
      "endIndex": 3161,
      "startIndex": 2804,
      "title": "java-interview-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 5,
    "score": 0.015384615398943424,
    "source": "5505ce35-75df-46c8-bfcb-e58c791676ef",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "5505ce35-75df-46c8-bfcb-e58c791676ef",
    "contentPreview": "e、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文档、技术博客 优势 ：快速查找代码示例、技术方案 7.4 法律文书检索 场景 ：法律条文、判例、合同模板 优势 ：精准检索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 9,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 2841,
      "startIndex": 2430,
      "title": "rag-technology-guide.md",
      "kbId": 8,
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
    "source": "a61f5990-2fff-4bff-ae1b-fefa6f5c25b5",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "a61f5990-2fff-4bff-ae1b-fefa6f5c25b5",
    "contentPreview": ": 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 3227,
      "startIndex": 2830,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 2,
    "score": 0.016129031777381897,
    "source": "ffac4de1-10a7-47c6-911d-11f31d611867",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "ffac4de1-10a7-47c6-911d-11f31d611867",
    "contentPreview": "和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 3,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 1447,
      "startIndex": 1001,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 3,
    "score": 0.015625,
    "source": "5505ce35-75df-46c8-bfcb-e58c791676ef",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "5505ce35-75df-46c8-bfcb-e58c791676ef",
    "contentPreview": "e、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文档、技术博客 优势 ：快速查找代码示例、技术方案 7.4 法律文书检索 场景 ：法律条文、判例、合同模板 优势 ：精准检索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 9,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 2841,
      "startIndex": 2430,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 4,
    "score": 0.01515151560306549,
    "source": "524644ce-3237-4fdf-8b48-ef1aca4578af",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "524644ce-3237-4fdf-8b48-ef1aca4578af",
    "contentPreview": "端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 8,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 2449,
      "startIndex": 1980,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015151515151515152
    }
  },
  {
    "rank": 5,
    "score": 0.014705882407724857,
    "source": "a6625763-d39e-448d-9671-b9e2af5d582c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "a6625763-d39e-448d-9671-b9e2af5d582c",
    "contentPreview": "【相关文档】 {retrieved_context} 【用户提问】 {user_question} 【回答要求】 1. 基于文档内容回答，不要编造信息 2. 如果文档中没有相关信息，明确说明 3. 引用具体的文档片段作为依据 4. 回答要简洁清晰",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 5,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 1679,
      "startIndex": 1550,
      "title": "rag-technology-guide.md",
      "kbId": 8,
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
    "documentId": 31,
    "chunkId": "0",
    "contentPreview": "端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索...",
    "metadata": {
      "chunkIndex": 8.0,
      "tokenCount": 469.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 31.0,
      "kbId": 8.0,
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
    "source": "524644ce-3237-4fdf-8b48-ef1aca4578af",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "524644ce-3237-4fdf-8b48-ef1aca4578af",
    "contentPreview": "端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 8,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 2449,
      "startIndex": 1980,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "c9a275d0-1518-4319-a734-82e36c288a49",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "c9a275d0-1518-4319-a734-82e36c288a49",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 7,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 2021,
      "startIndex": 1679,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "a61f5990-2fff-4bff-ae1b-fefa6f5c25b5",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "a61f5990-2fff-4bff-ae1b-fefa6f5c25b5",
    "contentPreview": ": 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 3227,
      "startIndex": 2830,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.014925372786819935,
    "source": "1611f65d-0c32-4ac3-826b-5488a154914b",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "1611f65d-0c32-4ac3-826b-5488a154914b",
    "contentPreview": "LM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召回相关文档 → 构建提示词 → LLM 生成答案 → 返回结果 2.1 详细步骤 Step 1: 文档处理（离线） 文档上传 ：PDF、Markdown、Word、代码文件等 文档解析 ：提取文本内容 文本分块 ：将长文档切分为小块（Chunk），通常 500-2000 字符 向量化 ：使用 Embedding 模型将文本转换为向量 存储 ：向量存入向量数据库（Milvus、Pinecone 等） Step 2: 问答处理（在线） 问题向量化 ：用户问题转换为向量 相似度检索 ：在向量数据库中搜...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 1,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 684,
      "startIndex": 251,
      "title": "rag-technology-guide.md",
      "kbId": 8,
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
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID + Redis Token 机制 乐观锁（版本号） 状态机（订单状态） 5. ...",
    "metadata": {
      "chunkIndex": 9.0,
      "tokenCount": 358.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 30.0,
      "kbId": 8.0,
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
    "source": "e5195a54-3b10-48fd-8fde-7eea7d0984f2",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 30,
    "chunkId": "e5195a54-3b10-48fd-8fde-7eea7d0984f2",
    "contentPreview": "面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID + Redis Token 机制 乐观锁（版本号） 状态机（订单状态） 5. ...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "chunkIndex": 9,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "java-interview-guide.md",
      "documentId": 30,
      "endIndex": 3161,
      "startIndex": 2804,
      "title": "java-interview-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.015625,
    "source": "5505ce35-75df-46c8-bfcb-e58c791676ef",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "5505ce35-75df-46c8-bfcb-e58c791676ef",
    "contentPreview": "e、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文档、技术博客 优势 ：快速查找代码示例、技术方案 7.4 法律文书检索 场景 ：法律条文、判例、合同模板 优势 ：精准检索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 9,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 2841,
      "startIndex": 2430,
      "title": "rag-technology-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 4,
    "score": 0.014925372786819935,
    "source": "b6b4b8ac-efe5-4714-b616-4102ec2b791f",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 30,
    "chunkId": "b6b4b8ac-efe5-4714-b616-4102ec2b791f",
    "contentPreview": "m-Cancel） ： 业务侵入性强 性能较好 Seata ：阿里开源分布式事务框架 8. 消息队列 8.1 RabbitMQ / Kafka 应用场景 ： 异步处理：发送邮件、短信 流量削峰：秒杀系统 系统解耦：订单服务和库存服务 消息可靠性 ： 生产者确认机制 消息持久化 消费者手动 ACK 9. 设计模式 9.1 常用设计模式 单例模式 ：Spring Bean 默认单例 工厂模式 ：BeanFactory 代理模式 ：AOP 模板方法模式 ：JdbcTemplate、RestTemplate 观察者模式 ：Spring 事件监听 策略模式 ：支付方式选择 10. JVM 10.1 内存...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "chunkIndex": 8,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "java-interview-guide.md",
      "documentId": 30,
      "endIndex": 2831,
      "startIndex": 2354,
      "title": "java-interview-guide.md",
      "kbId": 8,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.014925373134328358
    }
  },
  {
    "rank": 5,
    "score": 0.01587301678955555,
    "source": "c9a275d0-1518-4319-a734-82e36c288a49",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "c9a275d0-1518-4319-a734-82e36c288a49",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 7,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 2021,
      "startIndex": 1679,
      "title": "rag-technology-guide.md",
      "kbId": 8,
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
    "source": "c671ccb2-583d-4b53-9372-7a0e75372265",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "c671ccb2-583d-4b53-9372-7a0e75372265",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 0,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 270,
      "startIndex": 0,
      "title": "rag-technology-guide.md",
      "kbId": 8
    }
  },
  {
    "rank": 2,
    "score": 0.6496142745018005,
    "source": "a61f5990-2fff-4bff-ae1b-fefa6f5c25b5",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "a61f5990-2fff-4bff-ae1b-fefa6f5c25b5",
    "contentPreview": ": 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 3227,
      "startIndex": 2830,
      "title": "rag-technology-guide.md",
      "kbId": 8
    }
  },
  {
    "rank": 3,
    "score": 0.22468489408493042,
    "source": "ffac4de1-10a7-47c6-911d-11f31d611867",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "ffac4de1-10a7-47c6-911d-11f31d611867",
    "contentPreview": "和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 3,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 1447,
      "startIndex": 1001,
      "title": "rag-technology-guide.md",
      "kbId": 8
    }
  },
  {
    "rank": 4,
    "score": 0.22910824418067932,
    "source": "5505ce35-75df-46c8-bfcb-e58c791676ef",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "5505ce35-75df-46c8-bfcb-e58c791676ef",
    "contentPreview": "e、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文档、技术博客 优势 ：快速查找代码示例、技术方案 7.4 法律文书检索 场景 ：法律条文、判例、合同模板 优势 ：精准检索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ ...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 9,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 2841,
      "startIndex": 2430,
      "title": "rag-technology-guide.md",
      "kbId": 8
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
    "source": "d5a74410-0e4a-439d-90ab-e2693a0989b6",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 30,
    "chunkId": "d5a74410-0e4a-439d-90ab-e2693a0989b6",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "chunkIndex": 1,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "java-interview-guide.md",
      "documentId": 30,
      "endIndex": 518,
      "startIndex": 18,
      "title": "java-interview-guide.md",
      "kbId": 8
    }
  },
  {
    "rank": 2,
    "score": 0.6140515208244324,
    "source": "524644ce-3237-4fdf-8b48-ef1aca4578af",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "524644ce-3237-4fdf-8b48-ef1aca4578af",
    "contentPreview": "端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索...",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 8,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 2449,
      "startIndex": 1980,
      "title": "rag-technology-guide.md",
      "kbId": 8
    }
  },
  {
    "rank": 3,
    "score": 0.49650880694389343,
    "source": "37b62d9c-f7f1-4533-95ad-62ad38d7498c",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 30,
    "chunkId": "37b62d9c-f7f1-4533-95ad-62ad38d7498c",
    "contentPreview": "r synchronized vs Lock ： synchronized 自动释放锁，Lock 需要手动释放 Lock 支持更灵活的锁机制（可中断、可超时） volatile 关键字 ： 保证可见性：一个线程修改，其他线程立即可见 禁止指令重排序 不保证原子性 2.",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "chunkIndex": 2,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "java-interview-guide.md",
      "documentId": 30,
      "endIndex": 602,
      "startIndex": 468,
      "title": "java-interview-guide.md",
      "kbId": 8
    }
  },
  {
    "rank": 4,
    "score": 0.3733314573764801,
    "source": "226d4a6c-7e59-43af-b961-77960cb3650f",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "226d4a6c-7e59-43af-b961-77960cb3650f",
    "contentPreview": "嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.52, ..., 0.91] (相似度高) \"今天天气很好\" → [-0.78, 0.23, -0....",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 2,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 1022,
      "startIndex": 651,
      "title": "rag-technology-guide.md",
      "kbId": 8
    }
  },
  {
    "rank": 5,
    "score": 0.19701653718948364,
    "source": "b6b4b8ac-efe5-4714-b616-4102ec2b791f",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 30,
    "chunkId": "b6b4b8ac-efe5-4714-b616-4102ec2b791f",
    "contentPreview": "m-Cancel） ： 业务侵入性强 性能较好 Seata ：阿里开源分布式事务框架 8. 消息队列 8.1 RabbitMQ / Kafka 应用场景 ： 异步处理：发送邮件、短信 流量削峰：秒杀系统 系统解耦：订单服务和库存服务 消息可靠性 ： 生产者确认机制 消息持久化 消费者手动 ACK 9. 设计模式 9.1 常用设计模式 单例模式 ：Spring Bean 默认单例 工厂模式 ：BeanFactory 代理模式 ：AOP 模板方法模式 ：JdbcTemplate、RestTemplate 观察者模式 ：Spring 事件监听 策略模式 ：支付方式选择 10. JVM 10.1 内存...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "chunkIndex": 8,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "java-interview-guide.md",
      "documentId": 30,
      "endIndex": 2831,
      "startIndex": 2354,
      "title": "java-interview-guide.md",
      "kbId": 8
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
    "source": "798cb6d8-e70e-48a5-9123-a25e99de8e89",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 29,
    "chunkId": "798cb6d8-e70e-48a5-9123-a25e99de8e89",
    "contentPreview": "Spring Boot 核心知识点 1. Spring Boot 简介 Spring Boot 是基于 Spring 框架的快速开发脚手架，旨在简化 Spring 应用的初始搭建和开发过程。 1.1 核心特性 自动配置 ：根据 classpath 下的依赖自动配置 Spring 应用 起步依赖 ：一站式依赖管理，避免版本冲突 内嵌服务器 ：Tomcat、Jetty、Undertow 生产就绪 ：Actuator 健康检查、监控指标 无代码生成 ：不需要 XML 配置 2.",
    "metadata": {
      "fileName": "springboot-basics.md",
      "chunkIndex": 0,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "springboot-basics.md",
      "documentId": 29,
      "endIndex": 239,
      "startIndex": 0,
      "title": "springboot-basics.md",
      "kbId": 8
    }
  },
  {
    "rank": 2,
    "score": 0.8828594088554382,
    "source": "d5a74410-0e4a-439d-90ab-e2693a0989b6",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 30,
    "chunkId": "d5a74410-0e4a-439d-90ab-e2693a0989b6",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "fileName": "java-interview-guide.md",
      "chunkIndex": 1,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "java-interview-guide.md",
      "documentId": 30,
      "endIndex": 518,
      "startIndex": 18,
      "title": "java-interview-guide.md",
      "kbId": 8
    }
  },
  {
    "rank": 3,
    "score": 0.5149325728416443,
    "source": "c671ccb2-583d-4b53-9372-7a0e75372265",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 31,
    "chunkId": "c671ccb2-583d-4b53-9372-7a0e75372265",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 0,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 31,
      "endIndex": 270,
      "startIndex": 0,
      "title": "rag-technology-guide.md",
      "kbId": 8
    }
  },
  {
    "rank": 4,
    "score": 0.5358321666717529,
    "source": "2a8877a8-9790-41fd-b5fa-191056171dc6",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 29,
    "chunkId": "2a8877a8-9790-41fd-b5fa-191056171dc6",
    "contentPreview": "icy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max = 20, message = \"用户名长度 3-20\")...",
    "metadata": {
      "fileName": "springboot-basics.md",
      "chunkIndex": 11,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "springboot-basics.md",
      "documentId": 29,
      "endIndex": 3563,
      "startIndex": 3098,
      "title": "springboot-basics.md",
      "kbId": 8
    }
  },
  {
    "rank": 5,
    "score": 0.522793173789978,
    "source": "7f0fccc7-0962-4795-ad72-4f7cf92c3784",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 29,
    "chunkId": "7f0fccc7-0962-4795-ad72-4f7cf92c3784",
    "contentPreview": "vice { @Transactional(rollbackFor = Exception.class) public void createOrder(Order order) { orderRepository.save(order); // 如果发生异常，自动回滚 stockService.deduct(order.getProductId(), order.getQuantity()); } } 9.2 事务传播行为 REQUIRED （默认）：加入当前事务，没有则新建 REQUIRES_NEW ：总是新建事务 SUPPORTS ：有事务就加入，没有就非事务执行 NOT_SUPPORT...",
    "metadata": {
      "fileName": "springboot-basics.md",
      "chunkIndex": 13,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2259f65b65014dfa",
      "documentTitle": "springboot-basics.md",
      "documentId": 29,
      "endIndex": 4197,
      "startIndex": 3748,
      "title": "springboot-basics.md",
      "kbId": 8
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
| fact-001 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: 798cb6d8 e70e 48a5 9123 a25e99de8e89, 798cb6d8e70e48a59123a25e99de8e89, springboot basics, springbootbasics; r3: ed26e77e caf5 401c ba5e 875ae76ee6e8, ed26e77ecaf5401cba5e875ae76ee6e8, springboot basics, springbootbasics; r4: 33cf5e59 b73b 4864 b82a 31cc90e7732b, 33cf5e59b73b4864b82a31cc90e7732b, java interview guide, javainterviewguide; r5: f8994802 2a63 4d2a b094 cfd9f0a35763, f89948022a634d2ab094cfd9f0a35763, java interview guide, javainterviewguide |  |
| fact-002 | springboot-basics.md => springboot basics, springbootbasics; java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, springboot basics, springbootbasics; r2: acec5be0 6186 4f5d 82de fa3ae907d2f2, acec5be061864f5d82defa3ae907d2f2, springboot basics, springbootbasics; r3: 33cf5e59 b73b 4864 b82a 31cc90e7732b, 33cf5e59b73b4864b82a31cc90e7732b, java interview guide, javainterviewguide; r4: 497fc809 30f6 48c6 a2d2 a72201f46543, 497fc80930f648c6a2d2a72201f46543, springboot basics, springbootbasics; r5: 7f0fccc7 0962 4795 ad72 4f7cf92c3784, 7f0fccc709624795ad724f7cf92c3784, springboot basics, springbootbasics |  |
| fact-003 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: acec5be0 6186 4f5d 82de fa3ae907d2f2, acec5be061864f5d82defa3ae907d2f2, springboot basics, springbootbasics; r3: 2a8877a8 9790 41fd b5fa 191056171dc6, 2a8877a8979041fdb5fa191056171dc6, springboot basics, springbootbasics; r4: 497fc809 30f6 48c6 a2d2 a72201f46543, 497fc80930f648c6a2d2a72201f46543, springboot basics, springbootbasics; r5: 7f0fccc7 0962 4795 ad72 4f7cf92c3784, 7f0fccc709624795ad724f7cf92c3784, springboot basics, springbootbasics |  |
| fact-004 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: 7482bce6 b84b 4b4b aef9 633cd72a6d70, 7482bce6b84b4b4baef9633cd72a6d70, springboot basics, springbootbasics; r3: 798cb6d8 e70e 48a5 9123 a25e99de8e89, 798cb6d8e70e48a59123a25e99de8e89, springboot basics, springbootbasics; r4: 33cf5e59 b73b 4864 b82a 31cc90e7732b, 33cf5e59b73b4864b82a31cc90e7732b, java interview guide, javainterviewguide; r5: 5505ce35 75df 46c8 bfcb e58c791676ef, 5505ce3575df46c8bfcbe58c791676ef, rag technology guide, ragtechnologyguide |  |
| fact-005 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: 2a8877a8 9790 41fd b5fa 191056171dc6, 2a8877a8979041fdb5fa191056171dc6, springboot basics, springbootbasics; r3: ffac4de1 10a7 47c6 911d 11f31d611867, ffac4de110a747c6911d11f31d611867, rag technology guide, ragtechnologyguide; r4: a61f5990 2fff 4bff ae1b fefa6f5c25b5, a61f59902fff4bffae1bfefa6f5c25b5, rag technology guide, ragtechnologyguide; r5: c671ccb2 583d 4b53 9372 7a0e75372265, c671ccb2583d4b5393727a0e75372265, rag technology guide, ragtechnologyguide |  |
| fact-006 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 33cf5e59 b73b 4864 b82a 31cc90e7732b, 33cf5e59b73b4864b82a31cc90e7732b, java interview guide, javainterviewguide; r2: 0, java interview guide, javainterviewguide; r3: d5a74410 0e4a 439d 90ab e2693a0989b6, d5a744100e4a439d90abe2693a0989b6, java interview guide, javainterviewguide; r4: c193a52d cacb 42db a104 dccc395a8dd2, c193a52dcacb42dba104dccc395a8dd2, java interview guide, javainterviewguide; r5: f8994802 2a63 4d2a b094 cfd9f0a35763, f89948022a634d2ab094cfd9f0a35763, java interview guide, javainterviewguide |  |
| fact-007 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 03b40cc4 327b 424d b69b 814a9f92bee8, 03b40cc4327b424db69b814a9f92bee8, java interview guide, javainterviewguide; r3: e5195a54 3b10 48fd 8fde 7eea7d0984f2, e5195a543b1048fd8fde7eea7d0984f2, java interview guide, javainterviewguide; r4: 57dfec95 3dc0 4847 9d1c d5cb0dac6194, 57dfec953dc048479d1cd5cb0dac6194, springboot basics, springbootbasics; r5: c193a52d cacb 42db a104 dccc395a8dd2, c193a52dcacb42dba104dccc395a8dd2, java interview guide, javainterviewguide |  |
| fact-008 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 1611f65d 0c32 4ac3 826b 5488a154914b, 1611f65d0c324ac3826b5488a154914b, rag technology guide, ragtechnologyguide; r3: 524644ce 3237 4fdf 8b48 ef1aca4578af, 524644ce32374fdf8b48ef1aca4578af, rag technology guide, ragtechnologyguide; r4: 226d4a6c 7e59 43af b961 77960cb3650f, 226d4a6c7e5943afb96177960cb3650f, rag technology guide, ragtechnologyguide; r5: 5505ce35 75df 46c8 bfcb e58c791676ef, 5505ce3575df46c8bfcbe58c791676ef, rag technology guide, ragtechnologyguide |  |
| fact-009 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 1611f65d 0c32 4ac3 826b 5488a154914b, 1611f65d0c324ac3826b5488a154914b, rag technology guide, ragtechnologyguide; r3: a61f5990 2fff 4bff ae1b fefa6f5c25b5, a61f59902fff4bffae1bfefa6f5c25b5, rag technology guide, ragtechnologyguide; r4: c9a275d0 1518 4319 a734 82e36c288a49, c9a275d015184319a73482e36c288a49, rag technology guide, ragtechnologyguide; r5: 5505ce35 75df 46c8 bfcb e58c791676ef, 5505ce3575df46c8bfcbe58c791676ef, rag technology guide, ragtechnologyguide |  |
| fact-010 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: c193a52d cacb 42db a104 dccc395a8dd2, c193a52dcacb42dba104dccc395a8dd2, java interview guide, javainterviewguide; r3: ffac4de1 10a7 47c6 911d 11f31d611867, ffac4de110a747c6911d11f31d611867, rag technology guide, ragtechnologyguide; r4: e5195a54 3b10 48fd 8fde 7eea7d0984f2, e5195a543b1048fd8fde7eea7d0984f2, java interview guide, javainterviewguide; r5: d5a74410 0e4a 439d 90ab e2693a0989b6, d5a744100e4a439d90abe2693a0989b6, java interview guide, javainterviewguide |  |
| definition-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: c671ccb2 583d 4b53 9372 7a0e75372265, c671ccb2583d4b5393727a0e75372265, rag technology guide, ragtechnologyguide; r3: a61f5990 2fff 4bff ae1b fefa6f5c25b5, a61f59902fff4bffae1bfefa6f5c25b5, rag technology guide, ragtechnologyguide; r4: 524644ce 3237 4fdf 8b48 ef1aca4578af, 524644ce32374fdf8b48ef1aca4578af, rag technology guide, ragtechnologyguide; r5: 23b246a1 f049 4275 a4d3 4103dc69ed24, 23b246a1f0494275a4d34103dc69ed24, rag technology guide, ragtechnologyguide |  |
| definition-002 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 226d4a6c 7e59 43af b961 77960cb3650f, 226d4a6c7e5943afb96177960cb3650f, rag technology guide, ragtechnologyguide; r3: 5505ce35 75df 46c8 bfcb e58c791676ef, 5505ce3575df46c8bfcbe58c791676ef, rag technology guide, ragtechnologyguide; r4: 1611f65d 0c32 4ac3 826b 5488a154914b, 1611f65d0c324ac3826b5488a154914b, rag technology guide, ragtechnologyguide; r5: 524644ce 3237 4fdf 8b48 ef1aca4578af, 524644ce32374fdf8b48ef1aca4578af, rag technology guide, ragtechnologyguide |  |
| definition-003 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 226d4a6c 7e59 43af b961 77960cb3650f, 226d4a6c7e5943afb96177960cb3650f, rag technology guide, ragtechnologyguide; r3: 1611f65d 0c32 4ac3 826b 5488a154914b, 1611f65d0c324ac3826b5488a154914b, rag technology guide, ragtechnologyguide; r4: 5505ce35 75df 46c8 bfcb e58c791676ef, 5505ce3575df46c8bfcbe58c791676ef, rag technology guide, ragtechnologyguide; r5: 524644ce 3237 4fdf 8b48 ef1aca4578af, 524644ce32374fdf8b48ef1aca4578af, rag technology guide, ragtechnologyguide |  |
| definition-004 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: ffac4de1 10a7 47c6 911d 11f31d611867, ffac4de110a747c6911d11f31d611867, rag technology guide, ragtechnologyguide; r2: 0, rag technology guide, ragtechnologyguide; r3: 1611f65d 0c32 4ac3 826b 5488a154914b, 1611f65d0c324ac3826b5488a154914b, rag technology guide, ragtechnologyguide; r4: a61f5990 2fff 4bff ae1b fefa6f5c25b5, a61f59902fff4bffae1bfefa6f5c25b5, rag technology guide, ragtechnologyguide; r5: 5505ce35 75df 46c8 bfcb e58c791676ef, 5505ce3575df46c8bfcbe58c791676ef, rag technology guide, ragtechnologyguide |  |
| definition-005 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: f8994802 2a63 4d2a b094 cfd9f0a35763, f89948022a634d2ab094cfd9f0a35763, java interview guide, javainterviewguide; r3: 798cb6d8 e70e 48a5 9123 a25e99de8e89, 798cb6d8e70e48a59123a25e99de8e89, springboot basics, springbootbasics; r4: 5505ce35 75df 46c8 bfcb e58c791676ef, 5505ce3575df46c8bfcbe58c791676ef, rag technology guide, ragtechnologyguide; r5: ed26e77e caf5 401c ba5e 875ae76ee6e8, ed26e77ecaf5401cba5e875ae76ee6e8, springboot basics, springbootbasics |  |
| definition-006 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 5505ce35 75df 46c8 bfcb e58c791676ef, 5505ce3575df46c8bfcbe58c791676ef, rag technology guide, ragtechnologyguide; r3: f8994802 2a63 4d2a b094 cfd9f0a35763, f89948022a634d2ab094cfd9f0a35763, java interview guide, javainterviewguide; r4: b6b4b8ac efe5 4714 b616 4102ec2b791f, b6b4b8acefe54714b6164102ec2b791f, java interview guide, javainterviewguide; r5: ffac4de1 10a7 47c6 911d 11f31d611867, ffac4de110a747c6911d11f31d611867, rag technology guide, ragtechnologyguide |  |
| definition-007 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 03b40cc4 327b 424d b69b 814a9f92bee8, 03b40cc4327b424db69b814a9f92bee8, java interview guide, javainterviewguide; r2: 57dfec95 3dc0 4847 9d1c d5cb0dac6194, 57dfec953dc048479d1cd5cb0dac6194, springboot basics, springbootbasics; r3: c671ccb2 583d 4b53 9372 7a0e75372265, c671ccb2583d4b5393727a0e75372265, rag technology guide, ragtechnologyguide; r4: 2ed239f6 2452 44ad 93f9 6789ec770b06, 2ed239f6245244ad93f96789ec770b06, java interview guide, javainterviewguide; r5: c9a275d0 1518 4319 a734 82e36c288a49, c9a275d015184319a73482e36c288a49, rag technology guide, ragtechnologyguide |  |
| definition-008 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 03b40cc4 327b 424d b69b 814a9f92bee8, 03b40cc4327b424db69b814a9f92bee8, java interview guide, javainterviewguide; r3: b6b4b8ac efe5 4714 b616 4102ec2b791f, b6b4b8acefe54714b6164102ec2b791f, java interview guide, javainterviewguide; r4: ffac4de1 10a7 47c6 911d 11f31d611867, ffac4de110a747c6911d11f31d611867, rag technology guide, ragtechnologyguide; r5: a6625763 d39e 448d 9671 b9e2af5d582c, a6625763d39e448d9671b9e2af5d582c, rag technology guide, ragtechnologyguide |  |
| reasoning-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: a61f5990 2fff 4bff ae1b fefa6f5c25b5, a61f59902fff4bffae1bfefa6f5c25b5, rag technology guide, ragtechnologyguide; r3: ffac4de1 10a7 47c6 911d 11f31d611867, ffac4de110a747c6911d11f31d611867, rag technology guide, ragtechnologyguide; r4: 5505ce35 75df 46c8 bfcb e58c791676ef, 5505ce3575df46c8bfcbe58c791676ef, rag technology guide, ragtechnologyguide; r5: 23b246a1 f049 4275 a4d3 4103dc69ed24, 23b246a1f0494275a4d34103dc69ed24, rag technology guide, ragtechnologyguide |  |
| reasoning-002 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 524644ce 3237 4fdf 8b48 ef1aca4578af, 524644ce32374fdf8b48ef1aca4578af, rag technology guide, ragtechnologyguide; r3: 5505ce35 75df 46c8 bfcb e58c791676ef, 5505ce3575df46c8bfcbe58c791676ef, rag technology guide, ragtechnologyguide; r4: c671ccb2 583d 4b53 9372 7a0e75372265, c671ccb2583d4b5393727a0e75372265, rag technology guide, ragtechnologyguide; r5: ffac4de1 10a7 47c6 911d 11f31d611867, ffac4de110a747c6911d11f31d611867, rag technology guide, ragtechnologyguide |  |
| reasoning-003 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: a61f5990 2fff 4bff ae1b fefa6f5c25b5, a61f59902fff4bffae1bfefa6f5c25b5, rag technology guide, ragtechnologyguide; r2: ffac4de1 10a7 47c6 911d 11f31d611867, ffac4de110a747c6911d11f31d611867, rag technology guide, ragtechnologyguide; r3: 5505ce35 75df 46c8 bfcb e58c791676ef, 5505ce3575df46c8bfcbe58c791676ef, rag technology guide, ragtechnologyguide; r4: 524644ce 3237 4fdf 8b48 ef1aca4578af, 524644ce32374fdf8b48ef1aca4578af, rag technology guide, ragtechnologyguide; r5: 0, rag technology guide, ragtechnologyguide |  |
| reasoning-004 | springboot-basics.md => springboot basics, springbootbasics; java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 7482bce6 b84b 4b4b aef9 633cd72a6d70, 7482bce6b84b4b4baef9633cd72a6d70, springboot basics, springbootbasics; r3: f8994802 2a63 4d2a b094 cfd9f0a35763, f89948022a634d2ab094cfd9f0a35763, java interview guide, javainterviewguide; r4: a61f5990 2fff 4bff ae1b fefa6f5c25b5, a61f59902fff4bffae1bfefa6f5c25b5, rag technology guide, ragtechnologyguide; r5: 5505ce35 75df 46c8 bfcb e58c791676ef, 5505ce3575df46c8bfcbe58c791676ef, rag technology guide, ragtechnologyguide |  |
| reasoning-005 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: d5a74410 0e4a 439d 90ab e2693a0989b6, d5a744100e4a439d90abe2693a0989b6, java interview guide, javainterviewguide; r3: ffac4de1 10a7 47c6 911d 11f31d611867, ffac4de110a747c6911d11f31d611867, rag technology guide, ragtechnologyguide; r4: e5195a54 3b10 48fd 8fde 7eea7d0984f2, e5195a543b1048fd8fde7eea7d0984f2, java interview guide, javainterviewguide; r5: 5505ce35 75df 46c8 bfcb e58c791676ef, 5505ce3575df46c8bfcbe58c791676ef, rag technology guide, ragtechnologyguide |  |
| reasoning-006 | java-interview-guide.md => java interview guide, javainterviewguide | r1: a61f5990 2fff 4bff ae1b fefa6f5c25b5, a61f59902fff4bffae1bfefa6f5c25b5, rag technology guide, ragtechnologyguide; r2: ffac4de1 10a7 47c6 911d 11f31d611867, ffac4de110a747c6911d11f31d611867, rag technology guide, ragtechnologyguide; r3: 5505ce35 75df 46c8 bfcb e58c791676ef, 5505ce3575df46c8bfcbe58c791676ef, rag technology guide, ragtechnologyguide; r4: 524644ce 3237 4fdf 8b48 ef1aca4578af, 524644ce32374fdf8b48ef1aca4578af, rag technology guide, ragtechnologyguide; r5: a6625763 d39e 448d 9671 b9e2af5d582c, a6625763d39e448d9671b9e2af5d582c, rag technology guide, ragtechnologyguide |  |
| multi-hop-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 1611f65d 0c32 4ac3 826b 5488a154914b, 1611f65d0c324ac3826b5488a154914b, rag technology guide, ragtechnologyguide; r3: a61f5990 2fff 4bff ae1b fefa6f5c25b5, a61f59902fff4bffae1bfefa6f5c25b5, rag technology guide, ragtechnologyguide; r4: 5505ce35 75df 46c8 bfcb e58c791676ef, 5505ce3575df46c8bfcbe58c791676ef, rag technology guide, ragtechnologyguide; r5: 524644ce 3237 4fdf 8b48 ef1aca4578af, 524644ce32374fdf8b48ef1aca4578af, rag technology guide, ragtechnologyguide |  |
| multi-hop-002 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 524644ce 3237 4fdf 8b48 ef1aca4578af, 524644ce32374fdf8b48ef1aca4578af, rag technology guide, ragtechnologyguide; r3: c9a275d0 1518 4319 a734 82e36c288a49, c9a275d015184319a73482e36c288a49, rag technology guide, ragtechnologyguide; r4: a61f5990 2fff 4bff ae1b fefa6f5c25b5, a61f59902fff4bffae1bfefa6f5c25b5, rag technology guide, ragtechnologyguide; r5: 1611f65d 0c32 4ac3 826b 5488a154914b, 1611f65d0c324ac3826b5488a154914b, rag technology guide, ragtechnologyguide |  |
| multi-hop-003 | java-interview-guide.md => java interview guide, javainterviewguide; springboot-basics.md => springboot basics, springbootbasics | r1: 0, java interview guide, javainterviewguide; r2: e5195a54 3b10 48fd 8fde 7eea7d0984f2, e5195a543b1048fd8fde7eea7d0984f2, java interview guide, javainterviewguide; r3: 5505ce35 75df 46c8 bfcb e58c791676ef, 5505ce3575df46c8bfcbe58c791676ef, rag technology guide, ragtechnologyguide; r4: b6b4b8ac efe5 4714 b616 4102ec2b791f, b6b4b8acefe54714b6164102ec2b791f, java interview guide, javainterviewguide; r5: c9a275d0 1518 4319 a734 82e36c288a49, c9a275d015184319a73482e36c288a49, rag technology guide, ragtechnologyguide |  |
| no-answer-001 |  | r1: c671ccb2 583d 4b53 9372 7a0e75372265, c671ccb2583d4b5393727a0e75372265, rag technology guide, ragtechnologyguide; r2: a61f5990 2fff 4bff ae1b fefa6f5c25b5, a61f59902fff4bffae1bfefa6f5c25b5, rag technology guide, ragtechnologyguide; r3: ffac4de1 10a7 47c6 911d 11f31d611867, ffac4de110a747c6911d11f31d611867, rag technology guide, ragtechnologyguide; r4: 5505ce35 75df 46c8 bfcb e58c791676ef, 5505ce3575df46c8bfcbe58c791676ef, rag technology guide, ragtechnologyguide |  |
| no-answer-002 |  | r1: d5a74410 0e4a 439d 90ab e2693a0989b6, d5a744100e4a439d90abe2693a0989b6, java interview guide, javainterviewguide; r2: 524644ce 3237 4fdf 8b48 ef1aca4578af, 524644ce32374fdf8b48ef1aca4578af, rag technology guide, ragtechnologyguide; r3: 37b62d9c f7f1 4533 95ad 62ad38d7498c, 37b62d9cf7f1453395ad62ad38d7498c, java interview guide, javainterviewguide; r4: 226d4a6c 7e59 43af b961 77960cb3650f, 226d4a6c7e5943afb96177960cb3650f, rag technology guide, ragtechnologyguide; r5: b6b4b8ac efe5 4714 b616 4102ec2b791f, b6b4b8acefe54714b6164102ec2b791f, java interview guide, javainterviewguide |  |
| no-answer-003 |  | r1: 798cb6d8 e70e 48a5 9123 a25e99de8e89, 798cb6d8e70e48a59123a25e99de8e89, springboot basics, springbootbasics; r2: d5a74410 0e4a 439d 90ab e2693a0989b6, d5a744100e4a439d90abe2693a0989b6, java interview guide, javainterviewguide; r3: c671ccb2 583d 4b53 9372 7a0e75372265, c671ccb2583d4b5393727a0e75372265, rag technology guide, ragtechnologyguide; r4: 2a8877a8 9790 41fd b5fa 191056171dc6, 2a8877a8979041fdb5fa191056171dc6, springboot basics, springbootbasics; r5: 7f0fccc7 0962 4795 ad72 4f7cf92c3784, 7f0fccc709624795ad724f7cf92c3784, springboot basics, springbootbasics |  |

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
