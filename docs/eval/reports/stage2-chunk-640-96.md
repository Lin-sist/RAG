# RAG Eval Report

- Generated at: 2026-06-30T01:24:16.874939+00:00
- Report status: `RETRIEVAL_ONLY`
- askErrors count: `0`
- retrieveErrors count: `0`
- skippedAsk count: `30`
- rateLimitErrors count: `0`
- retry count: `0`
- Metrics safe for comparison: `retrieval metrics only`
- Base URL: `http://localhost:8080`
- Knowledge base ID: `12`
- Eval set: `docs\eval\rag_eval_set.jsonl`
- topK: `5`
- minScore: `0.3`
- enableRerank: `True`
- skipAsk: `True`
- Eval KB name: `codex-stage2-fill-chunk-640-96`
- Eval KB vector collection: `kb_f837fe44f0344e6a`
- Eval KB document count: `3`
- Eval KB chunk count: `31`
- Fixture files: `springboot-basics.md sha256=c51df5761d510aa4c8a5fd610c90454b12973e2999138c3b57ba83768a296521; java-interview-guide.md sha256=a33f16e91025e9a8d92274c4221d1c172bb4f68c03790db53b8e20157ef4faa0; rag-technology-guide.md sha256=59ad5d66a59be2ce4e517ca81e37fde06b7673f3da7f798a4c76b01cc6f348a9`
- Config snapshot: `rag-admin\src\main\resources\application.yml sha256=30ad69759a640af8a482da1dc734ddbf92abe8f59f351b83ed5ff297f153501e; rag-core\src\main\java\com\enterprise\rag\core\rag\query\RetrievalProperties.java sha256=f4d49e1ed6eeee0dc5c358df16bec0ec7424dd97b7dd03afeb1ca4f201e391e6; rag-document\src\main\java\com\enterprise\rag\document\chunker\ChunkConfig.java sha256=01b17712c8974055d15a30034e396d5074ba37ad8ea637fa7bca895a0c8a8554`
- Git HEAD: `2ba2970f9488998f69e03b73cd75ae5b55f80620`
- askDelaySeconds: `0.0`
- maxAskRetries: `0`
- retryBackoffSeconds: `0.0`
- Duration: `33.24s`

## Summary Metrics

| Metric | Value |
|---|---:|
| Samples | 30 |
| Answerable samples | 27 |
| No-answer samples | 3 |
| Recall@3 | 50.98% |
| Recall@5 | 54.90% |
| MRR | 0.5833 |
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
| fact-007 | fact | 1/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| fact-008 | fact | 0/2 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| fact-009 | fact | 0/2 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| fact-010 | fact | 0/1 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-001 | definition | 1/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-002 | definition | 2/2 | 2 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-003 | definition | 0/2 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-004 | definition | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-005 | definition | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-006 | definition | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-007 | definition | 1/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-008 | definition | 1/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| reasoning-001 | reasoning | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| reasoning-002 | reasoning | 0/2 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| reasoning-003 | reasoning | 2/2 | 4 | skipped | skipped | skipped | skipped | 0 | - |  |
| reasoning-004 | reasoning | 1/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| reasoning-005 | reasoning | 0/1 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| reasoning-006 | reasoning | 0/2 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| multi-hop-001 | multi_hop | 0/2 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| multi-hop-002 | multi_hop | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
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
    "documentId": 45,
    "chunkId": "0",
    "contentPreview": "生产就绪 ：Actuator 健康检查、监控指标 无代码生成 ：不需要 XML 配置 2. 核心注解 2.1 启动类注解 @SpringBootApplication public class Application { public static void main(String[] args) { SpringApplication.run(Application.class, args); } } @SpringBootApplication 是组合注解，包含： @Configuration ：标记为配置类 @EnableAutoConfiguration ：启用自动配置 @Compon...",
    "metadata": {
      "chunkIndex": 1.0,
      "tokenCount": 584.0,
      "headingPath": "",
      "sourceFileName": "springboot-basics.md",
      "documentId": 45.0,
      "kbId": 12.0,
      "startIndex": 193.0,
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
    "source": "012f2102-f3c6-4cfe-870a-c7c1d09984b8",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 45,
    "chunkId": "012f2102-f3c6-4cfe-870a-c7c1d09984b8",
    "contentPreview": "生产就绪 ：Actuator 健康检查、监控指标 无代码生成 ：不需要 XML 配置 2. 核心注解 2.1 启动类注解 @SpringBootApplication public class Application { public static void main(String[] args) { SpringApplication.run(Application.class, args); } } @SpringBootApplication 是组合注解，包含： @Configuration ：标记为配置类 @EnableAutoConfiguration ：启用自动配置 @Compon...",
    "metadata": {
      "sourceFileName": "springboot-basics.md",
      "documentId": 45,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 777,
      "headingPath": "",
      "tokenCount": 584,
      "chunkIndex": 1,
      "title": "springboot-basics.md",
      "startIndex": 193,
      "fileName": "springboot-basics.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "7fcad2f7-cf04-4d0f-bf87-b6ad641a83cd",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 45,
    "chunkId": "7fcad2f7-cf04-4d0f-bf87-b6ad641a83cd",
    "contentPreview": "onCreationPolicy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max = 20, message = ...",
    "metadata": {
      "sourceFileName": "springboot-basics.md",
      "documentId": 45,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 3563,
      "headingPath": "",
      "tokenCount": 478,
      "chunkIndex": 8,
      "title": "springboot-basics.md",
      "startIndex": 3085,
      "fileName": "springboot-basics.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "8dca3ee2-cbc6-40f9-b5ec-9dc436f660e9",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 45,
    "chunkId": "8dca3ee2-cbc6-40f9-b5ec-9dc436f660e9",
    "contentPreview": "cutor; } } @Service public class EmailService { @Async public CompletableFuture<String> sendEmail(String to, String subject) { // 异步发送邮件 return CompletableFuture.completedFuture(\"SUCCESS\"); } } 11. 定时任务 11.1 @Scheduled 注解 @Component public class ScheduledTasks { // 每 5 秒执行一次 @Scheduled(fixedRate = 5...",
    "metadata": {
      "sourceFileName": "springboot-basics.md",
      "documentId": 45,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 4958,
      "headingPath": "",
      "tokenCount": 433,
      "chunkIndex": 12,
      "title": "springboot-basics.md",
      "startIndex": 4525,
      "fileName": "springboot-basics.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "0ea2f2b0-7ec6-4769-8534-a39e23270497",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 45,
    "chunkId": "0ea2f2b0-7ec6-4769-8534-a39e23270497",
    "contentPreview": "equest request) { // @Valid 触发校验 } 9. 事务管理 9.1 声明式事务 @Service public class OrderService { @Transactional(rollbackFor = Exception.class) public void createOrder(Order order) { orderRepository.save(order); // 如果发生异常，自动回滚 stockService.deduct(order.getProductId(), order.getQuantity()); } } 9.2 事务传播行为 RE...",
    "metadata": {
      "sourceFileName": "springboot-basics.md",
      "documentId": 45,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 4197,
      "headingPath": "",
      "tokenCount": 536,
      "chunkIndex": 10,
      "title": "springboot-basics.md",
      "startIndex": 3661,
      "fileName": "springboot-basics.md",
      "kbId": 12,
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
    "documentId": 45,
    "chunkId": "0",
    "contentPreview": "onCreationPolicy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max = 20, message = ...",
    "metadata": {
      "chunkIndex": 8.0,
      "tokenCount": 478.0,
      "headingPath": "",
      "sourceFileName": "springboot-basics.md",
      "documentId": 45.0,
      "kbId": 12.0,
      "startIndex": 3085.0,
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
    "source": "7fcad2f7-cf04-4d0f-bf87-b6ad641a83cd",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 45,
    "chunkId": "7fcad2f7-cf04-4d0f-bf87-b6ad641a83cd",
    "contentPreview": "onCreationPolicy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max = 20, message = ...",
    "metadata": {
      "sourceFileName": "springboot-basics.md",
      "documentId": 45,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 3563,
      "headingPath": "",
      "tokenCount": 478,
      "chunkIndex": 8,
      "title": "springboot-basics.md",
      "startIndex": 3085,
      "fileName": "springboot-basics.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.015625,
    "source": "c4326107-94ce-446c-800b-1ea748308005",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "c4326107-94ce-446c-800b-1ea748308005",
    "contentPreview": "FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场景 Chunk 4（500 字）：总结 4. 提示词工程（Pro...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1550,
      "headingPath": "",
      "tokenCount": 398,
      "chunkIndex": 2,
      "title": "rag-technology-guide.md",
      "startIndex": 1153,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 4,
    "score": 0.016129031777381897,
    "source": "e1b72946-6f8b-4a9b-bfb2-149ba61bdaa4",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "e1b72946-6f8b-4a9b-bfb2-149ba61bdaa4",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : ...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": "",
      "tokenCount": 588,
      "chunkIndex": 7,
      "title": "rag-technology-guide.md",
      "startIndex": 2640,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 5,
    "score": 0.01587301678955555,
    "source": "c53e7361-b53b-4464-9bf8-6359cd82502e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "c53e7361-b53b-4464-9bf8-6359cd82502e",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 631,
      "headingPath": "",
      "tokenCount": 630,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
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
    "documentId": 46,
    "chunkId": "0",
    "contentPreview": "ITTED（读已提交） REPEATABLE READ（可重复读，MySQL 默认） SERIALIZABLE（串行化） 锁机制 ： 行锁：锁定某一行 表锁：锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key...",
    "metadata": {
      "chunkIndex": 4.0,
      "tokenCount": 574.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 46.0,
      "kbId": 12.0,
      "startIndex": 1825.0,
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
    "source": "680a11a2-0ee7-4925-8dd5-24b8061af6ed",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 46,
    "chunkId": "680a11a2-0ee7-4925-8dd5-24b8061af6ed",
    "contentPreview": "ITTED（读已提交） REPEATABLE READ（可重复读，MySQL 默认） SERIALIZABLE（串行化） 锁机制 ： 行锁：锁定某一行 表锁：锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key...",
    "metadata": {
      "sourceFileName": "java-interview-guide.md",
      "documentId": 46,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 2399,
      "headingPath": "",
      "tokenCount": 574,
      "chunkIndex": 4,
      "title": "java-interview-guide.md",
      "startIndex": 1825,
      "fileName": "java-interview-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.015625,
    "source": "e1b72946-6f8b-4a9b-bfb2-149ba61bdaa4",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "e1b72946-6f8b-4a9b-bfb2-149ba61bdaa4",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : ...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": "",
      "tokenCount": 588,
      "chunkIndex": 7,
      "title": "rag-technology-guide.md",
      "startIndex": 2640,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 4,
    "score": 0.016129031777381897,
    "source": "67eed153-0133-4a10-8027-e8be8b02121f",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 45,
    "chunkId": "67eed153-0133-4a10-8027-e8be8b02121f",
    "contentPreview": "log.info(\"当前时间：{}\", LocalDateTime.now()); } // 每天凌晨 2 点执行 @Scheduled(cron = \"0 0 2 * * ?\") public void cleanupExpiredData() { log.info(\"清理过期数据\"); } } 12. 缓存 12.1 Spring Cache @Configuration @EnableCaching public class CacheConfig { // 配置 Redis 缓存 } @Service public class UserService { @Cacheable(valu...",
    "metadata": {
      "sourceFileName": "springboot-basics.md",
      "documentId": 45,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 5439,
      "headingPath": "",
      "tokenCount": 528,
      "chunkIndex": 13,
      "title": "springboot-basics.md",
      "startIndex": 4909,
      "fileName": "springboot-basics.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 5,
    "score": 0.01587301678955555,
    "source": "e46ee62e-f2fa-4807-94e9-021a42963dad",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 46,
    "chunkId": "e46ee62e-f2fa-4807-94e9-021a42963dad",
    "contentPreview": "t） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID + Redis Token 机制 乐观锁（版本号） 状态机（订单状态） 5. 如何实现限流？ 计数器 滑动窗口 令牌桶（Guava RateLimiter） 漏桶算法 --- 祝你面试顺利！🎉",
    "metadata": {
      "sourceFileName": "java-interview-guide.md",
      "documentId": 46,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 3161,
      "headingPath": "",
      "tokenCount": 281,
      "chunkIndex": 6,
      "title": "java-interview-guide.md",
      "startIndex": 2881,
      "fileName": "java-interview-guide.md",
      "kbId": 12,
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
    "documentId": 47,
    "chunkId": "0",
    "contentPreview": "力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1...",
    "metadata": {
      "chunkIndex": 6.0,
      "tokenCount": 488.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47.0,
      "kbId": 12.0,
      "startIndex": 2160.0,
      "endIndex": 2648.0,
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
    "source": "c53e7361-b53b-4464-9bf8-6359cd82502e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "c53e7361-b53b-4464-9bf8-6359cd82502e",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 631,
      "headingPath": "",
      "tokenCount": 630,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.01587301678955555,
    "source": "d44ba779-3b9c-417d-b9ac-b83a1e475660",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "d44ba779-3b9c-417d-b9ac-b83a1e475660",
    "contentPreview": "力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2648,
      "headingPath": "",
      "tokenCount": 488,
      "chunkIndex": 6,
      "title": "rag-technology-guide.md",
      "startIndex": 2160,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 4,
    "score": 0.015625,
    "source": "e1b72946-6f8b-4a9b-bfb2-149ba61bdaa4",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "e1b72946-6f8b-4a9b-bfb2-149ba61bdaa4",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : ...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": "",
      "tokenCount": 588,
      "chunkIndex": 7,
      "title": "rag-technology-guide.md",
      "startIndex": 2640,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 5,
    "score": 0.016129031777381897,
    "source": "1ba1b3ad-0544-4929-b374-daab9ed1211b",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "1ba1b3ad-0544-4929-b374-daab9ed1211b",
    "contentPreview": "到的内容和问题组合成 Prompt LLM 生成 ：调用大模型生成答案 返回结果 ：返回答案和引用来源 3. 核心技术 3.1 Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1193,
      "headingPath": "",
      "tokenCount": 616,
      "chunkIndex": 1,
      "title": "rag-technology-guide.md",
      "startIndex": 577,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
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
    "documentId": 47,
    "chunkId": "0",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "chunkIndex": 0.0,
      "tokenCount": 630.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47.0,
      "kbId": 12.0,
      "startIndex": 0.0,
      "endIndex": 631.0,
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
    "source": "c53e7361-b53b-4464-9bf8-6359cd82502e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "c53e7361-b53b-4464-9bf8-6359cd82502e",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 631,
      "headingPath": "",
      "tokenCount": 630,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "e1b72946-6f8b-4a9b-bfb2-149ba61bdaa4",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "e1b72946-6f8b-4a9b-bfb2-149ba61bdaa4",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : ...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": "",
      "tokenCount": 588,
      "chunkIndex": 7,
      "title": "rag-technology-guide.md",
      "startIndex": 2640,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "df2365e1-f8f9-4dbf-88ff-92247c8fb52b",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "df2365e1-f8f9-4dbf-88ff-92247c8fb52b",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2220,
      "headingPath": "",
      "tokenCount": 540,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1679,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "d44ba779-3b9c-417d-b9ac-b83a1e475660",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "d44ba779-3b9c-417d-b9ac-b83a1e475660",
    "contentPreview": "力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2648,
      "headingPath": "",
      "tokenCount": 488,
      "chunkIndex": 6,
      "title": "rag-technology-guide.md",
      "startIndex": 2160,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
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
    "documentId": 46,
    "chunkId": "0",
    "contentPreview": "active 切换 4. MyBatis / MyBatis-Plus 4.1 MyBatis 核心 #{} vs ${} ： #{} 预编译，防止 SQL 注入 ${} 字符串替换，存在注入风险 一级缓存 vs 二级缓存 ： 一级缓存：SqlSession 级别，默认开启 二级缓存：Mapper 级别，需要手动开启 4.2 MyBatis-Plus 优势 ： 内置通用 CRUD 方法 代码生成器 分页插件 乐观锁、逻辑删除支持 5. MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数...",
    "metadata": {
      "chunkIndex": 3.0,
      "tokenCount": 576.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 46.0,
      "kbId": 12.0,
      "startIndex": 1345.0,
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
    "source": "3ea6ba23-8c03-4878-b31c-3d09c94f5c74",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 46,
    "chunkId": "3ea6ba23-8c03-4878-b31c-3d09c94f5c74",
    "contentPreview": "active 切换 4. MyBatis / MyBatis-Plus 4.1 MyBatis 核心 #{} vs ${} ： #{} 预编译，防止 SQL 注入 ${} 字符串替换，存在注入风险 一级缓存 vs 二级缓存 ： 一级缓存：SqlSession 级别，默认开启 二级缓存：Mapper 级别，需要手动开启 4.2 MyBatis-Plus 优势 ： 内置通用 CRUD 方法 代码生成器 分页插件 乐观锁、逻辑删除支持 5. MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数...",
    "metadata": {
      "sourceFileName": "java-interview-guide.md",
      "documentId": 46,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 1921,
      "headingPath": "",
      "tokenCount": 576,
      "chunkIndex": 3,
      "title": "java-interview-guide.md",
      "startIndex": 1345,
      "fileName": "java-interview-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "1ba1b3ad-0544-4929-b374-daab9ed1211b",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "1ba1b3ad-0544-4929-b374-daab9ed1211b",
    "contentPreview": "到的内容和问题组合成 Prompt LLM 生成 ：调用大模型生成答案 返回结果 ：返回答案和引用来源 3. 核心技术 3.1 Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1193,
      "headingPath": "",
      "tokenCount": 616,
      "chunkIndex": 1,
      "title": "rag-technology-guide.md",
      "startIndex": 577,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.015625,
    "source": "e46ee62e-f2fa-4807-94e9-021a42963dad",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 46,
    "chunkId": "e46ee62e-f2fa-4807-94e9-021a42963dad",
    "contentPreview": "t） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID + Redis Token 机制 乐观锁（版本号） 状态机（订单状态） 5. 如何实现限流？ 计数器 滑动窗口 令牌桶（Guava RateLimiter） 漏桶算法 --- 祝你面试顺利！🎉",
    "metadata": {
      "sourceFileName": "java-interview-guide.md",
      "documentId": 46,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 3161,
      "headingPath": "",
      "tokenCount": 281,
      "chunkIndex": 6,
      "title": "java-interview-guide.md",
      "startIndex": 2881,
      "fileName": "java-interview-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 5,
    "score": 0.015384615398943424,
    "source": "680a11a2-0ee7-4925-8dd5-24b8061af6ed",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 46,
    "chunkId": "680a11a2-0ee7-4925-8dd5-24b8061af6ed",
    "contentPreview": "ITTED（读已提交） REPEATABLE READ（可重复读，MySQL 默认） SERIALIZABLE（串行化） 锁机制 ： 行锁：锁定某一行 表锁：锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key...",
    "metadata": {
      "sourceFileName": "java-interview-guide.md",
      "documentId": 46,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 2399,
      "headingPath": "",
      "tokenCount": 574,
      "chunkIndex": 4,
      "title": "java-interview-guide.md",
      "startIndex": 1825,
      "fileName": "java-interview-guide.md",
      "kbId": 12,
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
    "documentId": 47,
    "chunkId": "0",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "chunkIndex": 0.0,
      "tokenCount": 630.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47.0,
      "kbId": 12.0,
      "startIndex": 0.0,
      "endIndex": 631.0,
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
    "source": "c53e7361-b53b-4464-9bf8-6359cd82502e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "c53e7361-b53b-4464-9bf8-6359cd82502e",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 631,
      "headingPath": "",
      "tokenCount": 630,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "c4326107-94ce-446c-800b-1ea748308005",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "c4326107-94ce-446c-800b-1ea748308005",
    "contentPreview": "FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场景 Chunk 4（500 字）：总结 4. 提示词工程（Pro...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1550,
      "headingPath": "",
      "tokenCount": 398,
      "chunkIndex": 2,
      "title": "rag-technology-guide.md",
      "startIndex": 1153,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "e1b72946-6f8b-4a9b-bfb2-149ba61bdaa4",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "e1b72946-6f8b-4a9b-bfb2-149ba61bdaa4",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : ...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": "",
      "tokenCount": 588,
      "chunkIndex": 7,
      "title": "rag-technology-guide.md",
      "startIndex": 2640,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "df2365e1-f8f9-4dbf-88ff-92247c8fb52b",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "df2365e1-f8f9-4dbf-88ff-92247c8fb52b",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2220,
      "headingPath": "",
      "tokenCount": 540,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1679,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
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
    "documentId": 47,
    "chunkId": "0",
    "contentPreview": "到的内容和问题组合成 Prompt LLM 生成 ：调用大模型生成答案 返回结果 ：返回答案和引用来源 3. 核心技术 3.1 Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是...",
    "metadata": {
      "chunkIndex": 1.0,
      "tokenCount": 616.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47.0,
      "kbId": 12.0,
      "startIndex": 577.0,
      "endIndex": 1193.0,
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
    "source": "c53e7361-b53b-4464-9bf8-6359cd82502e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "c53e7361-b53b-4464-9bf8-6359cd82502e",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 631,
      "headingPath": "",
      "tokenCount": 630,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "1ba1b3ad-0544-4929-b374-daab9ed1211b",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "1ba1b3ad-0544-4929-b374-daab9ed1211b",
    "contentPreview": "到的内容和问题组合成 Prompt LLM 生成 ：调用大模型生成答案 返回结果 ：返回答案和引用来源 3. 核心技术 3.1 Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1193,
      "headingPath": "",
      "tokenCount": 616,
      "chunkIndex": 1,
      "title": "rag-technology-guide.md",
      "startIndex": 577,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "e1b72946-6f8b-4a9b-bfb2-149ba61bdaa4",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "e1b72946-6f8b-4a9b-bfb2-149ba61bdaa4",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : ...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": "",
      "tokenCount": 588,
      "chunkIndex": 7,
      "title": "rag-technology-guide.md",
      "startIndex": 2640,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "d44ba779-3b9c-417d-b9ac-b83a1e475660",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "d44ba779-3b9c-417d-b9ac-b83a1e475660",
    "contentPreview": "力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2648,
      "headingPath": "",
      "tokenCount": 488,
      "chunkIndex": 6,
      "title": "rag-technology-guide.md",
      "startIndex": 2160,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
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
    "source": "680a11a2-0ee7-4925-8dd5-24b8061af6ed",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 46,
    "chunkId": "680a11a2-0ee7-4925-8dd5-24b8061af6ed",
    "contentPreview": "ITTED（读已提交） REPEATABLE READ（可重复读，MySQL 默认） SERIALIZABLE（串行化） 锁机制 ： 行锁：锁定某一行 表锁：锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key...",
    "metadata": {
      "sourceFileName": "java-interview-guide.md",
      "documentId": 46,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 2399,
      "headingPath": "",
      "tokenCount": 574,
      "chunkIndex": 4,
      "title": "java-interview-guide.md",
      "startIndex": 1825,
      "fileName": "java-interview-guide.md",
      "kbId": 12
    }
  },
  {
    "rank": 2,
    "score": 0.4724162220954895,
    "source": "67eed153-0133-4a10-8027-e8be8b02121f",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 45,
    "chunkId": "67eed153-0133-4a10-8027-e8be8b02121f",
    "contentPreview": "log.info(\"当前时间：{}\", LocalDateTime.now()); } // 每天凌晨 2 点执行 @Scheduled(cron = \"0 0 2 * * ?\") public void cleanupExpiredData() { log.info(\"清理过期数据\"); } } 12. 缓存 12.1 Spring Cache @Configuration @EnableCaching public class CacheConfig { // 配置 Redis 缓存 } @Service public class UserService { @Cacheable(valu...",
    "metadata": {
      "sourceFileName": "springboot-basics.md",
      "documentId": 45,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 5439,
      "headingPath": "",
      "tokenCount": 528,
      "chunkIndex": 13,
      "title": "springboot-basics.md",
      "startIndex": 4909,
      "fileName": "springboot-basics.md",
      "kbId": 12
    }
  },
  {
    "rank": 3,
    "score": 0.38898128271102905,
    "source": "379235d2-7ca2-4acd-9cb8-1d3e57b40bc3",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 45,
    "chunkId": "379235d2-7ca2-4acd-9cb8-1d3e57b40bc3",
    "contentPreview": "c void deleteById(Long id) { // 删除缓存 } @CachePut(value = \"users\", key = \"#user.id\") public User update(User user) { // 更新缓存 } } 13. 单元测试 13.1 测试示例 @SpringBootTest @AutoConfigureMockMvc class UserControllerTest { @Autowired private MockMvc mockMvc; @MockBean private UserService userService; @Test voi...",
    "metadata": {
      "sourceFileName": "springboot-basics.md",
      "documentId": 45,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 5866,
      "headingPath": "",
      "tokenCount": 477,
      "chunkIndex": 14,
      "title": "springboot-basics.md",
      "startIndex": 5389,
      "fileName": "springboot-basics.md",
      "kbId": 12
    }
  },
  {
    "rank": 4,
    "score": 0.36915963888168335,
    "source": "df2365e1-f8f9-4dbf-88ff-92247c8fb52b",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "df2365e1-f8f9-4dbf-88ff-92247c8fb52b",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2220,
      "headingPath": "",
      "tokenCount": 540,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1679,
      "fileName": "rag-technology-guide.md",
      "kbId": 12
    }
  },
  {
    "rank": 5,
    "score": 0.3484977185726166,
    "source": "3ea6ba23-8c03-4878-b31c-3d09c94f5c74",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 46,
    "chunkId": "3ea6ba23-8c03-4878-b31c-3d09c94f5c74",
    "contentPreview": "active 切换 4. MyBatis / MyBatis-Plus 4.1 MyBatis 核心 #{} vs ${} ： #{} 预编译，防止 SQL 注入 ${} 字符串替换，存在注入风险 一级缓存 vs 二级缓存 ： 一级缓存：SqlSession 级别，默认开启 二级缓存：Mapper 级别，需要手动开启 4.2 MyBatis-Plus 优势 ： 内置通用 CRUD 方法 代码生成器 分页插件 乐观锁、逻辑删除支持 5. MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数...",
    "metadata": {
      "sourceFileName": "java-interview-guide.md",
      "documentId": 46,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 1921,
      "headingPath": "",
      "tokenCount": 576,
      "chunkIndex": 3,
      "title": "java-interview-guide.md",
      "startIndex": 1345,
      "fileName": "java-interview-guide.md",
      "kbId": 12
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
    "documentId": 46,
    "chunkId": "0",
    "contentPreview": "ITTED（读已提交） REPEATABLE READ（可重复读，MySQL 默认） SERIALIZABLE（串行化） 锁机制 ： 行锁：锁定某一行 表锁：锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key...",
    "metadata": {
      "chunkIndex": 4.0,
      "tokenCount": 574.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 46.0,
      "kbId": 12.0,
      "startIndex": 1825.0,
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
    "source": "680a11a2-0ee7-4925-8dd5-24b8061af6ed",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 46,
    "chunkId": "680a11a2-0ee7-4925-8dd5-24b8061af6ed",
    "contentPreview": "ITTED（读已提交） REPEATABLE READ（可重复读，MySQL 默认） SERIALIZABLE（串行化） 锁机制 ： 行锁：锁定某一行 表锁：锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key...",
    "metadata": {
      "sourceFileName": "java-interview-guide.md",
      "documentId": 46,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 2399,
      "headingPath": "",
      "tokenCount": 574,
      "chunkIndex": 4,
      "title": "java-interview-guide.md",
      "startIndex": 1825,
      "fileName": "java-interview-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.015625,
    "source": "94a23fea-4dac-496f-88a1-ae15201f971b",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 46,
    "chunkId": "94a23fea-4dac-496f-88a1-ae15201f971b",
    "contentPreview": "2PC（两阶段提交） ： 准备阶段、提交阶段 缺点：阻塞、单点故障 TCC（Try-Confirm-Cancel） ： 业务侵入性强 性能较好 Seata ：阿里开源分布式事务框架 8. 消息队列 8.1 RabbitMQ / Kafka 应用场景 ： 异步处理：发送邮件、短信 流量削峰：秒杀系统 系统解耦：订单服务和库存服务 消息可靠性 ： 生产者确认机制 消息持久化 消费者手动 ACK 9. 设计模式 9.1 常用设计模式 单例模式 ：Spring Bean 默认单例 工厂模式 ：BeanFactory 代理模式 ：AOP 模板方法模式 ：JdbcTemplate、RestTemplate...",
    "metadata": {
      "sourceFileName": "java-interview-guide.md",
      "documentId": 46,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 2937,
      "headingPath": "",
      "tokenCount": 631,
      "chunkIndex": 5,
      "title": "java-interview-guide.md",
      "startIndex": 2305,
      "fileName": "java-interview-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 4,
    "score": 0.016129031777381897,
    "source": "2c6dcc13-9e6f-4e6b-a9fa-2288401e3bcb",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "2c6dcc13-9e6f-4e6b-a9fa-2288401e3bcb",
    "contentPreview": "【相关文档】 {retrieved_context} 【用户提问】 {user_question} 【回答要求】 1. 基于文档内容回答，不要编造信息 2. 如果文档中没有相关信息，明确说明 3. 引用具体的文档片段作为依据 4. 回答要简洁清晰",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1679,
      "headingPath": "",
      "tokenCount": 125,
      "chunkIndex": 3,
      "title": "rag-technology-guide.md",
      "startIndex": 1550,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 5,
    "score": 0.014925372786819935,
    "source": "c4326107-94ce-446c-800b-1ea748308005",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "c4326107-94ce-446c-800b-1ea748308005",
    "contentPreview": "FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场景 Chunk 4（500 字）：总结 4. 提示词工程（Pro...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1550,
      "headingPath": "",
      "tokenCount": 398,
      "chunkIndex": 2,
      "title": "rag-technology-guide.md",
      "startIndex": 1153,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
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
    "source": "df2365e1-f8f9-4dbf-88ff-92247c8fb52b",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "df2365e1-f8f9-4dbf-88ff-92247c8fb52b",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2220,
      "headingPath": "",
      "tokenCount": 540,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1679,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 2,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "0",
    "contentPreview": "力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1...",
    "metadata": {
      "chunkIndex": 6.0,
      "tokenCount": 488.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47.0,
      "kbId": 12.0,
      "startIndex": 2160.0,
      "endIndex": 2648.0,
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
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "d44ba779-3b9c-417d-b9ac-b83a1e475660",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "d44ba779-3b9c-417d-b9ac-b83a1e475660",
    "contentPreview": "力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2648,
      "headingPath": "",
      "tokenCount": 488,
      "chunkIndex": 6,
      "title": "rag-technology-guide.md",
      "startIndex": 2160,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "c53e7361-b53b-4464-9bf8-6359cd82502e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "c53e7361-b53b-4464-9bf8-6359cd82502e",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 631,
      "headingPath": "",
      "tokenCount": 630,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "c4326107-94ce-446c-800b-1ea748308005",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "c4326107-94ce-446c-800b-1ea748308005",
    "contentPreview": "FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场景 Chunk 4（500 字）：总结 4. 提示词工程（Pro...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1550,
      "headingPath": "",
      "tokenCount": 398,
      "chunkIndex": 2,
      "title": "rag-technology-guide.md",
      "startIndex": 1153,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
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
    "documentId": 46,
    "chunkId": "0",
    "contentPreview": "其他线程立即可见 禁止指令重排序 不保证原子性 2. Spring 框架 2.1 Spring IOC（控制反转） 依赖注入的三种方式 ： 构造器注入（推荐，保证不可变性） Setter 注入（可选依赖） 字段注入（不推荐，难以测试） Bean 的生命周期 ： 实例化 属性赋值 初始化（@PostConstruct） 使用 销毁（@PreDestroy） 2.2 Spring AOP（面向切面编程） 应用场景 ：事务管理、日志记录、权限校验、性能监控 核心概念 ： Aspect（切面）：横切关注点的模块化 Join Point（连接点）：方法执行点 Pointcut（切入点）：匹配规则 Adv...",
    "metadata": {
      "chunkIndex": 1.0,
      "tokenCount": 368.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 46.0,
      "kbId": 12.0,
      "startIndex": 577.0,
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
    "source": "f2ea3cf9-0ff2-4ed7-9f44-2f6c0bf8d759",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 45,
    "chunkId": "f2ea3cf9-0ff2-4ed7-9f44-2f6c0bf8d759",
    "contentPreview": "root driver-class-name: com.mysql.cj.jdbc.Driver redis: host: localhost port: 6379 logging: level: root: INFO com.example: DEBUG 3.2 配置优先级 命令行参数 JNDI 属性 Java 系统属性 环境变量 application.properties/yml 4. 依赖注入 4.1 构造器注入（推荐） @Service @RequiredArgsConstructor public class UserService { private final UserRepo...",
    "metadata": {
      "sourceFileName": "springboot-basics.md",
      "documentId": 45,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 1546,
      "headingPath": "",
      "tokenCount": 668,
      "chunkIndex": 3,
      "title": "springboot-basics.md",
      "startIndex": 877,
      "fileName": "springboot-basics.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "95112da1-3193-49c1-b060-33e06fffab54",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 46,
    "chunkId": "95112da1-3193-49c1-b060-33e06fffab54",
    "contentPreview": "其他线程立即可见 禁止指令重排序 不保证原子性 2. Spring 框架 2.1 Spring IOC（控制反转） 依赖注入的三种方式 ： 构造器注入（推荐，保证不可变性） Setter 注入（可选依赖） 字段注入（不推荐，难以测试） Bean 的生命周期 ： 实例化 属性赋值 初始化（@PostConstruct） 使用 销毁（@PreDestroy） 2.2 Spring AOP（面向切面编程） 应用场景 ：事务管理、日志记录、权限校验、性能监控 核心概念 ： Aspect（切面）：横切关注点的模块化 Join Point（连接点）：方法执行点 Pointcut（切入点）：匹配规则 Adv...",
    "metadata": {
      "sourceFileName": "java-interview-guide.md",
      "documentId": 46,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 945,
      "headingPath": "",
      "tokenCount": 368,
      "chunkIndex": 1,
      "title": "java-interview-guide.md",
      "startIndex": 577,
      "fileName": "java-interview-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "e1b72946-6f8b-4a9b-bfb2-149ba61bdaa4",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "e1b72946-6f8b-4a9b-bfb2-149ba61bdaa4",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : ...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": "",
      "tokenCount": 588,
      "chunkIndex": 7,
      "title": "rag-technology-guide.md",
      "startIndex": 2640,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "c4326107-94ce-446c-800b-1ea748308005",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "c4326107-94ce-446c-800b-1ea748308005",
    "contentPreview": "FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场景 Chunk 4（500 字）：总结 4. 提示词工程（Pro...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1550,
      "headingPath": "",
      "tokenCount": 398,
      "chunkIndex": 2,
      "title": "rag-technology-guide.md",
      "startIndex": 1153,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
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
    "documentId": 46,
    "chunkId": "0",
    "contentPreview": "Java 后端面试知识点总结 1. Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容...",
    "metadata": {
      "chunkIndex": 0.0,
      "tokenCount": 602.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 46.0,
      "kbId": 12.0,
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
    "source": "5891f5b6-a76e-4d8c-950d-89bdd019ca00",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 46,
    "chunkId": "5891f5b6-a76e-4d8c-950d-89bdd019ca00",
    "contentPreview": "Java 后端面试知识点总结 1. Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容...",
    "metadata": {
      "sourceFileName": "java-interview-guide.md",
      "documentId": 46,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 603,
      "headingPath": "",
      "tokenCount": 602,
      "chunkIndex": 0,
      "title": "java-interview-guide.md",
      "startIndex": 0,
      "fileName": "java-interview-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "c4326107-94ce-446c-800b-1ea748308005",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "c4326107-94ce-446c-800b-1ea748308005",
    "contentPreview": "FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场景 Chunk 4（500 字）：总结 4. 提示词工程（Pro...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1550,
      "headingPath": "",
      "tokenCount": 398,
      "chunkIndex": 2,
      "title": "rag-technology-guide.md",
      "startIndex": 1153,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "1ba1b3ad-0544-4929-b374-daab9ed1211b",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "1ba1b3ad-0544-4929-b374-daab9ed1211b",
    "contentPreview": "到的内容和问题组合成 Prompt LLM 生成 ：调用大模型生成答案 返回结果 ：返回答案和引用来源 3. 核心技术 3.1 Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1193,
      "headingPath": "",
      "tokenCount": 616,
      "chunkIndex": 1,
      "title": "rag-technology-guide.md",
      "startIndex": 577,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "95112da1-3193-49c1-b060-33e06fffab54",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 46,
    "chunkId": "95112da1-3193-49c1-b060-33e06fffab54",
    "contentPreview": "其他线程立即可见 禁止指令重排序 不保证原子性 2. Spring 框架 2.1 Spring IOC（控制反转） 依赖注入的三种方式 ： 构造器注入（推荐，保证不可变性） Setter 注入（可选依赖） 字段注入（不推荐，难以测试） Bean 的生命周期 ： 实例化 属性赋值 初始化（@PostConstruct） 使用 销毁（@PreDestroy） 2.2 Spring AOP（面向切面编程） 应用场景 ：事务管理、日志记录、权限校验、性能监控 核心概念 ： Aspect（切面）：横切关注点的模块化 Join Point（连接点）：方法执行点 Pointcut（切入点）：匹配规则 Adv...",
    "metadata": {
      "sourceFileName": "java-interview-guide.md",
      "documentId": 46,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 945,
      "headingPath": "",
      "tokenCount": 368,
      "chunkIndex": 1,
      "title": "java-interview-guide.md",
      "startIndex": 577,
      "fileName": "java-interview-guide.md",
      "kbId": 12,
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
    "score": 0.016129031777381897,
    "source": "1ba1b3ad-0544-4929-b374-daab9ed1211b",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "1ba1b3ad-0544-4929-b374-daab9ed1211b",
    "contentPreview": "到的内容和问题组合成 Prompt LLM 生成 ：调用大模型生成答案 返回结果 ：返回答案和引用来源 3. 核心技术 3.1 Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1193,
      "headingPath": "",
      "tokenCount": 616,
      "chunkIndex": 1,
      "title": "rag-technology-guide.md",
      "startIndex": 577,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 2,
    "score": 0.01587301678955555,
    "source": "e1b72946-6f8b-4a9b-bfb2-149ba61bdaa4",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "e1b72946-6f8b-4a9b-bfb2-149ba61bdaa4",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : ...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": "",
      "tokenCount": 588,
      "chunkIndex": 7,
      "title": "rag-technology-guide.md",
      "startIndex": 2640,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 3,
    "score": 0.016393441706895828,
    "source": "c4326107-94ce-446c-800b-1ea748308005",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "c4326107-94ce-446c-800b-1ea748308005",
    "contentPreview": "FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场景 Chunk 4（500 字）：总结 4. 提示词工程（Pro...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1550,
      "headingPath": "",
      "tokenCount": 398,
      "chunkIndex": 2,
      "title": "rag-technology-guide.md",
      "startIndex": 1153,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 4,
    "score": 0.015384615398943424,
    "source": "df2365e1-f8f9-4dbf-88ff-92247c8fb52b",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "df2365e1-f8f9-4dbf-88ff-92247c8fb52b",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2220,
      "headingPath": "",
      "tokenCount": 540,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1679,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015384615384615385
    }
  },
  {
    "rank": 5,
    "score": 0.014705882407724857,
    "source": "c53e7361-b53b-4464-9bf8-6359cd82502e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "c53e7361-b53b-4464-9bf8-6359cd82502e",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 631,
      "headingPath": "",
      "tokenCount": 630,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
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
    "documentId": 47,
    "chunkId": "0",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "chunkIndex": 0.0,
      "tokenCount": 630.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47.0,
      "kbId": 12.0,
      "startIndex": 0.0,
      "endIndex": 631.0,
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
    "source": "c53e7361-b53b-4464-9bf8-6359cd82502e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "c53e7361-b53b-4464-9bf8-6359cd82502e",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 631,
      "headingPath": "",
      "tokenCount": 630,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "e1b72946-6f8b-4a9b-bfb2-149ba61bdaa4",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "e1b72946-6f8b-4a9b-bfb2-149ba61bdaa4",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : ...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": "",
      "tokenCount": 588,
      "chunkIndex": 7,
      "title": "rag-technology-guide.md",
      "startIndex": 2640,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "d44ba779-3b9c-417d-b9ac-b83a1e475660",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "d44ba779-3b9c-417d-b9ac-b83a1e475660",
    "contentPreview": "力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2648,
      "headingPath": "",
      "tokenCount": 488,
      "chunkIndex": 6,
      "title": "rag-technology-guide.md",
      "startIndex": 2160,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "df2365e1-f8f9-4dbf-88ff-92247c8fb52b",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "df2365e1-f8f9-4dbf-88ff-92247c8fb52b",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2220,
      "headingPath": "",
      "tokenCount": 540,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1679,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
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
    "documentId": 46,
    "chunkId": "0",
    "contentPreview": "t） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID + Redis Token 机制 乐观锁（版本号） 状态机（订单状态） 5. 如何实现限流？ 计数器 滑动窗口 令牌桶（Guava RateLimiter） 漏桶算法 --- 祝你面试顺利！🎉",
    "metadata": {
      "chunkIndex": 6.0,
      "tokenCount": 281.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 46.0,
      "kbId": 12.0,
      "startIndex": 2881.0,
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
    "source": "e46ee62e-f2fa-4807-94e9-021a42963dad",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 46,
    "chunkId": "e46ee62e-f2fa-4807-94e9-021a42963dad",
    "contentPreview": "t） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID + Redis Token 机制 乐观锁（版本号） 状态机（订单状态） 5. 如何实现限流？ 计数器 滑动窗口 令牌桶（Guava RateLimiter） 漏桶算法 --- 祝你面试顺利！🎉",
    "metadata": {
      "sourceFileName": "java-interview-guide.md",
      "documentId": 46,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 3161,
      "headingPath": "",
      "tokenCount": 281,
      "chunkIndex": 6,
      "title": "java-interview-guide.md",
      "startIndex": 2881,
      "fileName": "java-interview-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "e1b72946-6f8b-4a9b-bfb2-149ba61bdaa4",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "e1b72946-6f8b-4a9b-bfb2-149ba61bdaa4",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : ...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": "",
      "tokenCount": 588,
      "chunkIndex": 7,
      "title": "rag-technology-guide.md",
      "startIndex": 2640,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.015625,
    "source": "df2365e1-f8f9-4dbf-88ff-92247c8fb52b",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "df2365e1-f8f9-4dbf-88ff-92247c8fb52b",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2220,
      "headingPath": "",
      "tokenCount": 540,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1679,
      "fileName": "rag-technology-guide.md",
      "kbId": 12,
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 5,
    "score": 0.014925372786819935,
    "source": "94a23fea-4dac-496f-88a1-ae15201f971b",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 46,
    "chunkId": "94a23fea-4dac-496f-88a1-ae15201f971b",
    "contentPreview": "2PC（两阶段提交） ： 准备阶段、提交阶段 缺点：阻塞、单点故障 TCC（Try-Confirm-Cancel） ： 业务侵入性强 性能较好 Seata ：阿里开源分布式事务框架 8. 消息队列 8.1 RabbitMQ / Kafka 应用场景 ： 异步处理：发送邮件、短信 流量削峰：秒杀系统 系统解耦：订单服务和库存服务 消息可靠性 ： 生产者确认机制 消息持久化 消费者手动 ACK 9. 设计模式 9.1 常用设计模式 单例模式 ：Spring Bean 默认单例 工厂模式 ：BeanFactory 代理模式 ：AOP 模板方法模式 ：JdbcTemplate、RestTemplate...",
    "metadata": {
      "sourceFileName": "java-interview-guide.md",
      "documentId": 46,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 2937,
      "headingPath": "",
      "tokenCount": 631,
      "chunkIndex": 5,
      "title": "java-interview-guide.md",
      "startIndex": 2305,
      "fileName": "java-interview-guide.md",
      "kbId": 12,
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
    "source": "c53e7361-b53b-4464-9bf8-6359cd82502e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "c53e7361-b53b-4464-9bf8-6359cd82502e",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 631,
      "headingPath": "",
      "tokenCount": 630,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "kbId": 12
    }
  },
  {
    "rank": 2,
    "score": 0.8778671026229858,
    "source": "e1b72946-6f8b-4a9b-bfb2-149ba61bdaa4",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "e1b72946-6f8b-4a9b-bfb2-149ba61bdaa4",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : ...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": "",
      "tokenCount": 588,
      "chunkIndex": 7,
      "title": "rag-technology-guide.md",
      "startIndex": 2640,
      "fileName": "rag-technology-guide.md",
      "kbId": 12
    }
  },
  {
    "rank": 3,
    "score": 0.37389102578163147,
    "source": "c4326107-94ce-446c-800b-1ea748308005",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "c4326107-94ce-446c-800b-1ea748308005",
    "contentPreview": "FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场景 Chunk 4（500 字）：总结 4. 提示词工程（Pro...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1550,
      "headingPath": "",
      "tokenCount": 398,
      "chunkIndex": 2,
      "title": "rag-technology-guide.md",
      "startIndex": 1153,
      "fileName": "rag-technology-guide.md",
      "kbId": 12
    }
  },
  {
    "rank": 4,
    "score": 0.3235825002193451,
    "source": "1ba1b3ad-0544-4929-b374-daab9ed1211b",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "1ba1b3ad-0544-4929-b374-daab9ed1211b",
    "contentPreview": "到的内容和问题组合成 Prompt LLM 生成 ：调用大模型生成答案 返回结果 ：返回答案和引用来源 3. 核心技术 3.1 Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1193,
      "headingPath": "",
      "tokenCount": 616,
      "chunkIndex": 1,
      "title": "rag-technology-guide.md",
      "startIndex": 577,
      "fileName": "rag-technology-guide.md",
      "kbId": 12
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
    "source": "5891f5b6-a76e-4d8c-950d-89bdd019ca00",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 46,
    "chunkId": "5891f5b6-a76e-4d8c-950d-89bdd019ca00",
    "contentPreview": "Java 后端面试知识点总结 1. Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容...",
    "metadata": {
      "sourceFileName": "java-interview-guide.md",
      "documentId": 46,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 603,
      "headingPath": "",
      "tokenCount": 602,
      "chunkIndex": 0,
      "title": "java-interview-guide.md",
      "startIndex": 0,
      "fileName": "java-interview-guide.md",
      "kbId": 12
    }
  },
  {
    "rank": 2,
    "score": 0.4736140966415405,
    "source": "df2365e1-f8f9-4dbf-88ff-92247c8fb52b",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "df2365e1-f8f9-4dbf-88ff-92247c8fb52b",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2220,
      "headingPath": "",
      "tokenCount": 540,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1679,
      "fileName": "rag-technology-guide.md",
      "kbId": 12
    }
  },
  {
    "rank": 3,
    "score": 0.49529388546943665,
    "source": "d44ba779-3b9c-417d-b9ac-b83a1e475660",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "d44ba779-3b9c-417d-b9ac-b83a1e475660",
    "contentPreview": "力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2648,
      "headingPath": "",
      "tokenCount": 488,
      "chunkIndex": 6,
      "title": "rag-technology-guide.md",
      "startIndex": 2160,
      "fileName": "rag-technology-guide.md",
      "kbId": 12
    }
  },
  {
    "rank": 4,
    "score": 0.4779723882675171,
    "source": "1ba1b3ad-0544-4929-b374-daab9ed1211b",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "1ba1b3ad-0544-4929-b374-daab9ed1211b",
    "contentPreview": "到的内容和问题组合成 Prompt LLM 生成 ：调用大模型生成答案 返回结果 ：返回答案和引用来源 3. 核心技术 3.1 Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1193,
      "headingPath": "",
      "tokenCount": 616,
      "chunkIndex": 1,
      "title": "rag-technology-guide.md",
      "startIndex": 577,
      "fileName": "rag-technology-guide.md",
      "kbId": 12
    }
  },
  {
    "rank": 5,
    "score": 0.2616786062717438,
    "source": "95112da1-3193-49c1-b060-33e06fffab54",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 46,
    "chunkId": "95112da1-3193-49c1-b060-33e06fffab54",
    "contentPreview": "其他线程立即可见 禁止指令重排序 不保证原子性 2. Spring 框架 2.1 Spring IOC（控制反转） 依赖注入的三种方式 ： 构造器注入（推荐，保证不可变性） Setter 注入（可选依赖） 字段注入（不推荐，难以测试） Bean 的生命周期 ： 实例化 属性赋值 初始化（@PostConstruct） 使用 销毁（@PreDestroy） 2.2 Spring AOP（面向切面编程） 应用场景 ：事务管理、日志记录、权限校验、性能监控 核心概念 ： Aspect（切面）：横切关注点的模块化 Join Point（连接点）：方法执行点 Pointcut（切入点）：匹配规则 Adv...",
    "metadata": {
      "sourceFileName": "java-interview-guide.md",
      "documentId": 46,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 945,
      "headingPath": "",
      "tokenCount": 368,
      "chunkIndex": 1,
      "title": "java-interview-guide.md",
      "startIndex": 577,
      "fileName": "java-interview-guide.md",
      "kbId": 12
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
    "source": "f1db76cf-2cf8-4bdf-b091-b5bf647edc1b",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 45,
    "chunkId": "f1db76cf-2cf8-4bdf-b091-b5bf647edc1b",
    "contentPreview": "Spring Boot 核心知识点 1. Spring Boot 简介 Spring Boot 是基于 Spring 框架的快速开发脚手架，旨在简化 Spring 应用的初始搭建和开发过程。 1.1 核心特性 自动配置 ：根据 classpath 下的依赖自动配置 Spring 应用 起步依赖 ：一站式依赖管理，避免版本冲突 内嵌服务器 ：Tomcat、Jetty、Undertow 生产就绪 ：Actuator 健康检查、监控指标 无代码生成 ：不需要 XML 配置 2.",
    "metadata": {
      "sourceFileName": "springboot-basics.md",
      "documentId": 45,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 239,
      "headingPath": "",
      "tokenCount": 238,
      "chunkIndex": 0,
      "title": "springboot-basics.md",
      "startIndex": 0,
      "fileName": "springboot-basics.md",
      "kbId": 12
    }
  },
  {
    "rank": 2,
    "score": 0.7905640602111816,
    "source": "5891f5b6-a76e-4d8c-950d-89bdd019ca00",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 46,
    "chunkId": "5891f5b6-a76e-4d8c-950d-89bdd019ca00",
    "contentPreview": "Java 后端面试知识点总结 1. Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容...",
    "metadata": {
      "sourceFileName": "java-interview-guide.md",
      "documentId": 46,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 603,
      "headingPath": "",
      "tokenCount": 602,
      "chunkIndex": 0,
      "title": "java-interview-guide.md",
      "startIndex": 0,
      "fileName": "java-interview-guide.md",
      "kbId": 12
    }
  },
  {
    "rank": 3,
    "score": 0.5385187864303589,
    "source": "7fcad2f7-cf04-4d0f-bf87-b6ad641a83cd",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 45,
    "chunkId": "7fcad2f7-cf04-4d0f-bf87-b6ad641a83cd",
    "contentPreview": "onCreationPolicy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max = 20, message = ...",
    "metadata": {
      "sourceFileName": "springboot-basics.md",
      "documentId": 45,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 3563,
      "headingPath": "",
      "tokenCount": 478,
      "chunkIndex": 8,
      "title": "springboot-basics.md",
      "startIndex": 3085,
      "fileName": "springboot-basics.md",
      "kbId": 12
    }
  },
  {
    "rank": 4,
    "score": 0.49507829546928406,
    "source": "0ea2f2b0-7ec6-4769-8534-a39e23270497",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 45,
    "chunkId": "0ea2f2b0-7ec6-4769-8534-a39e23270497",
    "contentPreview": "equest request) { // @Valid 触发校验 } 9. 事务管理 9.1 声明式事务 @Service public class OrderService { @Transactional(rollbackFor = Exception.class) public void createOrder(Order order) { orderRepository.save(order); // 如果发生异常，自动回滚 stockService.deduct(order.getProductId(), order.getQuantity()); } } 9.2 事务传播行为 RE...",
    "metadata": {
      "sourceFileName": "springboot-basics.md",
      "documentId": 45,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 4197,
      "headingPath": "",
      "tokenCount": 536,
      "chunkIndex": 10,
      "title": "springboot-basics.md",
      "startIndex": 3661,
      "fileName": "springboot-basics.md",
      "kbId": 12
    }
  },
  {
    "rank": 5,
    "score": 0.3351359963417053,
    "source": "c53e7361-b53b-4464-9bf8-6359cd82502e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 47,
    "chunkId": "c53e7361-b53b-4464-9bf8-6359cd82502e",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 47,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 631,
      "headingPath": "",
      "tokenCount": 630,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "kbId": 12
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
| fact-001 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: f1db76cf 2cf8 4bdf b091 b5bf647edc1b, f1db76cf2cf84bdfb091b5bf647edc1b, springboot basics, springbootbasics; r3: e1b72946 6f8b 4a9b bfb2 149ba61bdaa4, e1b729466f8b4a9bbfb2149ba61bdaa4, rag technology guide, ragtechnologyguide; r4: e8f8e607 b14d 475d af0e bb488a0ac0fc, e8f8e607b14d475daf0ebb488a0ac0fc, springboot basics, springbootbasics; r5: 3ea6ba23 8c03 4878 b31c 3d09c94f5c74, 3ea6ba238c034878b31c3d09c94f5c74, java interview guide, javainterviewguide |  |
| fact-002 | springboot-basics.md => springboot basics, springbootbasics; java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, springboot basics, springbootbasics; r2: 012f2102 f3c6 4cfe 870a c7c1d09984b8, 012f2102f3c64cfe870ac7c1d09984b8, springboot basics, springbootbasics; r3: dc89b72e 6112 4886 a320 af86621b8f01, dc89b72e61124886a320af86621b8f01, java interview guide, javainterviewguide; r4: 8dca3ee2 cbc6 40f9 b5ec 9dc436f660e9, 8dca3ee2cbc640f9b5ec9dc436f660e9, springboot basics, springbootbasics; r5: 0ea2f2b0 7ec6 4769 8534 a39e23270497, 0ea2f2b07ec647698534a39e23270497, springboot basics, springbootbasics |  |
| fact-003 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: 012f2102 f3c6 4cfe 870a c7c1d09984b8, 012f2102f3c64cfe870ac7c1d09984b8, springboot basics, springbootbasics; r3: 7fcad2f7 cf04 4d0f bf87 b6ad641a83cd, 7fcad2f7cf044d0fbf87b6ad641a83cd, springboot basics, springbootbasics; r4: 8dca3ee2 cbc6 40f9 b5ec 9dc436f660e9, 8dca3ee2cbc640f9b5ec9dc436f660e9, springboot basics, springbootbasics; r5: 0ea2f2b0 7ec6 4769 8534 a39e23270497, 0ea2f2b07ec647698534a39e23270497, springboot basics, springbootbasics |  |
| fact-004 | springboot-basics.md => springboot basics, springbootbasics | r1: f2ea3cf9 0ff2 4ed7 9f44 2f6c0bf8d759, f2ea3cf90ff24ed79f442f6c0bf8d759, springboot basics, springbootbasics; r2: 0, java interview guide, javainterviewguide; r3: f1db76cf 2cf8 4bdf b091 b5bf647edc1b, f1db76cf2cf84bdfb091b5bf647edc1b, springboot basics, springbootbasics; r4: dc89b72e 6112 4886 a320 af86621b8f01, dc89b72e61124886a320af86621b8f01, java interview guide, javainterviewguide; r5: e1b72946 6f8b 4a9b bfb2 149ba61bdaa4, e1b729466f8b4a9bbfb2149ba61bdaa4, rag technology guide, ragtechnologyguide |  |
| fact-005 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: 7fcad2f7 cf04 4d0f bf87 b6ad641a83cd, 7fcad2f7cf044d0fbf87b6ad641a83cd, springboot basics, springbootbasics; r3: c4326107 94ce 446c 800b 1ea748308005, c432610794ce446c800b1ea748308005, rag technology guide, ragtechnologyguide; r4: e1b72946 6f8b 4a9b bfb2 149ba61bdaa4, e1b729466f8b4a9bbfb2149ba61bdaa4, rag technology guide, ragtechnologyguide; r5: c53e7361 b53b 4464 9bf8 6359cd82502e, c53e7361b53b44649bf86359cd82502e, rag technology guide, ragtechnologyguide |  |
| fact-006 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: dc89b72e 6112 4886 a320 af86621b8f01, dc89b72e61124886a320af86621b8f01, java interview guide, javainterviewguide; r3: 5891f5b6 a76e 4d8c 950d 89bdd019ca00, 5891f5b6a76e4d8c950d89bdd019ca00, java interview guide, javainterviewguide; r4: 95112da1 3193 49c1 b060 33e06fffab54, 95112da1319349c1b06033e06fffab54, java interview guide, javainterviewguide; r5: 3ea6ba23 8c03 4878 b31c 3d09c94f5c74, 3ea6ba238c034878b31c3d09c94f5c74, java interview guide, javainterviewguide |  |
| fact-007 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 680a11a2 0ee7 4925 8dd5 24b8061af6ed, 680a11a20ee749258dd524b8061af6ed, java interview guide, javainterviewguide; r3: e1b72946 6f8b 4a9b bfb2 149ba61bdaa4, e1b729466f8b4a9bbfb2149ba61bdaa4, rag technology guide, ragtechnologyguide; r4: 67eed153 0133 4a10 8027 e8be8b02121f, 67eed15301334a108027e8be8b02121f, springboot basics, springbootbasics; r5: e46ee62e f2fa 4807 94e9 021a42963dad, e46ee62ef2fa480794e9021a42963dad, java interview guide, javainterviewguide |  |
| fact-008 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: c53e7361 b53b 4464 9bf8 6359cd82502e, c53e7361b53b44649bf86359cd82502e, rag technology guide, ragtechnologyguide; r3: d44ba779 3b9c 417d b9ac b83a1e475660, d44ba7793b9c417db9acb83a1e475660, rag technology guide, ragtechnologyguide; r4: e1b72946 6f8b 4a9b bfb2 149ba61bdaa4, e1b729466f8b4a9bbfb2149ba61bdaa4, rag technology guide, ragtechnologyguide; r5: 1ba1b3ad 0544 4929 b374 daab9ed1211b, 1ba1b3ad05444929b374daab9ed1211b, rag technology guide, ragtechnologyguide |  |
| fact-009 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: c53e7361 b53b 4464 9bf8 6359cd82502e, c53e7361b53b44649bf86359cd82502e, rag technology guide, ragtechnologyguide; r3: e1b72946 6f8b 4a9b bfb2 149ba61bdaa4, e1b729466f8b4a9bbfb2149ba61bdaa4, rag technology guide, ragtechnologyguide; r4: df2365e1 f8f9 4dbf 88ff 92247c8fb52b, df2365e1f8f94dbf88ff92247c8fb52b, rag technology guide, ragtechnologyguide; r5: d44ba779 3b9c 417d b9ac b83a1e475660, d44ba7793b9c417db9acb83a1e475660, rag technology guide, ragtechnologyguide |  |
| fact-010 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 3ea6ba23 8c03 4878 b31c 3d09c94f5c74, 3ea6ba238c034878b31c3d09c94f5c74, java interview guide, javainterviewguide; r3: 1ba1b3ad 0544 4929 b374 daab9ed1211b, 1ba1b3ad05444929b374daab9ed1211b, rag technology guide, ragtechnologyguide; r4: e46ee62e f2fa 4807 94e9 021a42963dad, e46ee62ef2fa480794e9021a42963dad, java interview guide, javainterviewguide; r5: 680a11a2 0ee7 4925 8dd5 24b8061af6ed, 680a11a20ee749258dd524b8061af6ed, java interview guide, javainterviewguide |  |
| definition-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: c53e7361 b53b 4464 9bf8 6359cd82502e, c53e7361b53b44649bf86359cd82502e, rag technology guide, ragtechnologyguide; r3: c4326107 94ce 446c 800b 1ea748308005, c432610794ce446c800b1ea748308005, rag technology guide, ragtechnologyguide; r4: e1b72946 6f8b 4a9b bfb2 149ba61bdaa4, e1b729466f8b4a9bbfb2149ba61bdaa4, rag technology guide, ragtechnologyguide; r5: df2365e1 f8f9 4dbf 88ff 92247c8fb52b, df2365e1f8f94dbf88ff92247c8fb52b, rag technology guide, ragtechnologyguide |  |
| definition-002 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: c53e7361 b53b 4464 9bf8 6359cd82502e, c53e7361b53b44649bf86359cd82502e, rag technology guide, ragtechnologyguide; r2: 1ba1b3ad 0544 4929 b374 daab9ed1211b, 1ba1b3ad05444929b374daab9ed1211b, rag technology guide, ragtechnologyguide; r3: e1b72946 6f8b 4a9b bfb2 149ba61bdaa4, e1b729466f8b4a9bbfb2149ba61bdaa4, rag technology guide, ragtechnologyguide; r4: d44ba779 3b9c 417d b9ac b83a1e475660, d44ba7793b9c417db9acb83a1e475660, rag technology guide, ragtechnologyguide; r5: c4326107 94ce 446c 800b 1ea748308005, c432610794ce446c800b1ea748308005, rag technology guide, ragtechnologyguide |  |
| definition-003 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: c53e7361 b53b 4464 9bf8 6359cd82502e, c53e7361b53b44649bf86359cd82502e, rag technology guide, ragtechnologyguide; r3: 1ba1b3ad 0544 4929 b374 daab9ed1211b, 1ba1b3ad05444929b374daab9ed1211b, rag technology guide, ragtechnologyguide; r4: e1b72946 6f8b 4a9b bfb2 149ba61bdaa4, e1b729466f8b4a9bbfb2149ba61bdaa4, rag technology guide, ragtechnologyguide; r5: d44ba779 3b9c 417d b9ac b83a1e475660, d44ba7793b9c417db9acb83a1e475660, rag technology guide, ragtechnologyguide |  |
| definition-004 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: c4326107 94ce 446c 800b 1ea748308005, c432610794ce446c800b1ea748308005, rag technology guide, ragtechnologyguide; r3: c53e7361 b53b 4464 9bf8 6359cd82502e, c53e7361b53b44649bf86359cd82502e, rag technology guide, ragtechnologyguide; r4: e1b72946 6f8b 4a9b bfb2 149ba61bdaa4, e1b729466f8b4a9bbfb2149ba61bdaa4, rag technology guide, ragtechnologyguide; r5: 1ba1b3ad 0544 4929 b374 daab9ed1211b, 1ba1b3ad05444929b374daab9ed1211b, rag technology guide, ragtechnologyguide |  |
| definition-005 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 95112da1 3193 49c1 b060 33e06fffab54, 95112da1319349c1b06033e06fffab54, java interview guide, javainterviewguide; r3: f1db76cf 2cf8 4bdf b091 b5bf647edc1b, f1db76cf2cf84bdfb091b5bf647edc1b, springboot basics, springbootbasics; r4: e1b72946 6f8b 4a9b bfb2 149ba61bdaa4, e1b729466f8b4a9bbfb2149ba61bdaa4, rag technology guide, ragtechnologyguide; r5: e8f8e607 b14d 475d af0e bb488a0ac0fc, e8f8e607b14d475daf0ebb488a0ac0fc, springboot basics, springbootbasics |  |
| definition-006 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 95112da1 3193 49c1 b060 33e06fffab54, 95112da1319349c1b06033e06fffab54, java interview guide, javainterviewguide; r3: 94a23fea 4dac 496f 88a1 ae15201f971b, 94a23fea4dac496f88a1ae15201f971b, java interview guide, javainterviewguide; r4: d44ba779 3b9c 417d b9ac b83a1e475660, d44ba7793b9c417db9acb83a1e475660, rag technology guide, ragtechnologyguide; r5: c4326107 94ce 446c 800b 1ea748308005, c432610794ce446c800b1ea748308005, rag technology guide, ragtechnologyguide |  |
| definition-007 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 680a11a2 0ee7 4925 8dd5 24b8061af6ed, 680a11a20ee749258dd524b8061af6ed, java interview guide, javainterviewguide; r2: 67eed153 0133 4a10 8027 e8be8b02121f, 67eed15301334a108027e8be8b02121f, springboot basics, springbootbasics; r3: 379235d2 7ca2 4acd 9cb8 1d3e57b40bc3, 379235d27ca24acd9cb81d3e57b40bc3, springboot basics, springbootbasics; r4: df2365e1 f8f9 4dbf 88ff 92247c8fb52b, df2365e1f8f94dbf88ff92247c8fb52b, rag technology guide, ragtechnologyguide; r5: 3ea6ba23 8c03 4878 b31c 3d09c94f5c74, 3ea6ba238c034878b31c3d09c94f5c74, java interview guide, javainterviewguide |  |
| definition-008 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 680a11a2 0ee7 4925 8dd5 24b8061af6ed, 680a11a20ee749258dd524b8061af6ed, java interview guide, javainterviewguide; r3: 94a23fea 4dac 496f 88a1 ae15201f971b, 94a23fea4dac496f88a1ae15201f971b, java interview guide, javainterviewguide; r4: 2c6dcc13 9e6f 4e6b a9fa 2288401e3bcb, 2c6dcc139e6f4e6ba9fa2288401e3bcb, rag technology guide, ragtechnologyguide; r5: c4326107 94ce 446c 800b 1ea748308005, c432610794ce446c800b1ea748308005, rag technology guide, ragtechnologyguide |  |
| reasoning-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: c4326107 94ce 446c 800b 1ea748308005, c432610794ce446c800b1ea748308005, rag technology guide, ragtechnologyguide; r2: e1b72946 6f8b 4a9b bfb2 149ba61bdaa4, e1b729466f8b4a9bbfb2149ba61bdaa4, rag technology guide, ragtechnologyguide; r3: 1ba1b3ad 0544 4929 b374 daab9ed1211b, 1ba1b3ad05444929b374daab9ed1211b, rag technology guide, ragtechnologyguide; r4: 0, rag technology guide, ragtechnologyguide; r5: c53e7361 b53b 4464 9bf8 6359cd82502e, c53e7361b53b44649bf86359cd82502e, rag technology guide, ragtechnologyguide |  |
| reasoning-002 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: df2365e1 f8f9 4dbf 88ff 92247c8fb52b, df2365e1f8f94dbf88ff92247c8fb52b, rag technology guide, ragtechnologyguide; r2: 0, rag technology guide, ragtechnologyguide; r3: d44ba779 3b9c 417d b9ac b83a1e475660, d44ba7793b9c417db9acb83a1e475660, rag technology guide, ragtechnologyguide; r4: c53e7361 b53b 4464 9bf8 6359cd82502e, c53e7361b53b44649bf86359cd82502e, rag technology guide, ragtechnologyguide; r5: c4326107 94ce 446c 800b 1ea748308005, c432610794ce446c800b1ea748308005, rag technology guide, ragtechnologyguide |  |
| reasoning-003 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: c4326107 94ce 446c 800b 1ea748308005, c432610794ce446c800b1ea748308005, rag technology guide, ragtechnologyguide; r2: e1b72946 6f8b 4a9b bfb2 149ba61bdaa4, e1b729466f8b4a9bbfb2149ba61bdaa4, rag technology guide, ragtechnologyguide; r3: 1ba1b3ad 0544 4929 b374 daab9ed1211b, 1ba1b3ad05444929b374daab9ed1211b, rag technology guide, ragtechnologyguide; r4: df2365e1 f8f9 4dbf 88ff 92247c8fb52b, df2365e1f8f94dbf88ff92247c8fb52b, rag technology guide, ragtechnologyguide; r5: 0, rag technology guide, ragtechnologyguide |  |
| reasoning-004 | springboot-basics.md => springboot basics, springbootbasics; java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: f2ea3cf9 0ff2 4ed7 9f44 2f6c0bf8d759, f2ea3cf90ff24ed79f442f6c0bf8d759, springboot basics, springbootbasics; r3: 95112da1 3193 49c1 b060 33e06fffab54, 95112da1319349c1b06033e06fffab54, java interview guide, javainterviewguide; r4: e1b72946 6f8b 4a9b bfb2 149ba61bdaa4, e1b729466f8b4a9bbfb2149ba61bdaa4, rag technology guide, ragtechnologyguide; r5: c4326107 94ce 446c 800b 1ea748308005, c432610794ce446c800b1ea748308005, rag technology guide, ragtechnologyguide |  |
| reasoning-005 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 5891f5b6 a76e 4d8c 950d 89bdd019ca00, 5891f5b6a76e4d8c950d89bdd019ca00, java interview guide, javainterviewguide; r3: c4326107 94ce 446c 800b 1ea748308005, c432610794ce446c800b1ea748308005, rag technology guide, ragtechnologyguide; r4: 1ba1b3ad 0544 4929 b374 daab9ed1211b, 1ba1b3ad05444929b374daab9ed1211b, rag technology guide, ragtechnologyguide; r5: 95112da1 3193 49c1 b060 33e06fffab54, 95112da1319349c1b06033e06fffab54, java interview guide, javainterviewguide |  |
| reasoning-006 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 1ba1b3ad 0544 4929 b374 daab9ed1211b, 1ba1b3ad05444929b374daab9ed1211b, rag technology guide, ragtechnologyguide; r2: e1b72946 6f8b 4a9b bfb2 149ba61bdaa4, e1b729466f8b4a9bbfb2149ba61bdaa4, rag technology guide, ragtechnologyguide; r3: c4326107 94ce 446c 800b 1ea748308005, c432610794ce446c800b1ea748308005, rag technology guide, ragtechnologyguide; r4: df2365e1 f8f9 4dbf 88ff 92247c8fb52b, df2365e1f8f94dbf88ff92247c8fb52b, rag technology guide, ragtechnologyguide; r5: c53e7361 b53b 4464 9bf8 6359cd82502e, c53e7361b53b44649bf86359cd82502e, rag technology guide, ragtechnologyguide |  |
| multi-hop-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: c53e7361 b53b 4464 9bf8 6359cd82502e, c53e7361b53b44649bf86359cd82502e, rag technology guide, ragtechnologyguide; r3: e1b72946 6f8b 4a9b bfb2 149ba61bdaa4, e1b729466f8b4a9bbfb2149ba61bdaa4, rag technology guide, ragtechnologyguide; r4: d44ba779 3b9c 417d b9ac b83a1e475660, d44ba7793b9c417db9acb83a1e475660, rag technology guide, ragtechnologyguide; r5: df2365e1 f8f9 4dbf 88ff 92247c8fb52b, df2365e1f8f94dbf88ff92247c8fb52b, rag technology guide, ragtechnologyguide |  |
| multi-hop-002 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: d44ba779 3b9c 417d b9ac b83a1e475660, d44ba7793b9c417db9acb83a1e475660, rag technology guide, ragtechnologyguide; r2: df2365e1 f8f9 4dbf 88ff 92247c8fb52b, df2365e1f8f94dbf88ff92247c8fb52b, rag technology guide, ragtechnologyguide; r3: e1b72946 6f8b 4a9b bfb2 149ba61bdaa4, e1b729466f8b4a9bbfb2149ba61bdaa4, rag technology guide, ragtechnologyguide; r4: 0, rag technology guide, ragtechnologyguide; r5: c4326107 94ce 446c 800b 1ea748308005, c432610794ce446c800b1ea748308005, rag technology guide, ragtechnologyguide |  |
| multi-hop-003 | java-interview-guide.md => java interview guide, javainterviewguide; springboot-basics.md => springboot basics, springbootbasics | r1: 0, java interview guide, javainterviewguide; r2: e46ee62e f2fa 4807 94e9 021a42963dad, e46ee62ef2fa480794e9021a42963dad, java interview guide, javainterviewguide; r3: e1b72946 6f8b 4a9b bfb2 149ba61bdaa4, e1b729466f8b4a9bbfb2149ba61bdaa4, rag technology guide, ragtechnologyguide; r4: df2365e1 f8f9 4dbf 88ff 92247c8fb52b, df2365e1f8f94dbf88ff92247c8fb52b, rag technology guide, ragtechnologyguide; r5: 94a23fea 4dac 496f 88a1 ae15201f971b, 94a23fea4dac496f88a1ae15201f971b, java interview guide, javainterviewguide |  |
| no-answer-001 |  | r1: c53e7361 b53b 4464 9bf8 6359cd82502e, c53e7361b53b44649bf86359cd82502e, rag technology guide, ragtechnologyguide; r2: e1b72946 6f8b 4a9b bfb2 149ba61bdaa4, e1b729466f8b4a9bbfb2149ba61bdaa4, rag technology guide, ragtechnologyguide; r3: c4326107 94ce 446c 800b 1ea748308005, c432610794ce446c800b1ea748308005, rag technology guide, ragtechnologyguide; r4: 1ba1b3ad 0544 4929 b374 daab9ed1211b, 1ba1b3ad05444929b374daab9ed1211b, rag technology guide, ragtechnologyguide |  |
| no-answer-002 |  | r1: 5891f5b6 a76e 4d8c 950d 89bdd019ca00, 5891f5b6a76e4d8c950d89bdd019ca00, java interview guide, javainterviewguide; r2: df2365e1 f8f9 4dbf 88ff 92247c8fb52b, df2365e1f8f94dbf88ff92247c8fb52b, rag technology guide, ragtechnologyguide; r3: d44ba779 3b9c 417d b9ac b83a1e475660, d44ba7793b9c417db9acb83a1e475660, rag technology guide, ragtechnologyguide; r4: 1ba1b3ad 0544 4929 b374 daab9ed1211b, 1ba1b3ad05444929b374daab9ed1211b, rag technology guide, ragtechnologyguide; r5: 95112da1 3193 49c1 b060 33e06fffab54, 95112da1319349c1b06033e06fffab54, java interview guide, javainterviewguide |  |
| no-answer-003 |  | r1: f1db76cf 2cf8 4bdf b091 b5bf647edc1b, f1db76cf2cf84bdfb091b5bf647edc1b, springboot basics, springbootbasics; r2: 5891f5b6 a76e 4d8c 950d 89bdd019ca00, 5891f5b6a76e4d8c950d89bdd019ca00, java interview guide, javainterviewguide; r3: 7fcad2f7 cf04 4d0f bf87 b6ad641a83cd, 7fcad2f7cf044d0fbf87b6ad641a83cd, springboot basics, springbootbasics; r4: 0ea2f2b0 7ec6 4769 8534 a39e23270497, 0ea2f2b07ec647698534a39e23270497, springboot basics, springbootbasics; r5: c53e7361 b53b 4464 9bf8 6359cd82502e, c53e7361b53b44649bf86359cd82502e, rag technology guide, ragtechnologyguide |  |

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
