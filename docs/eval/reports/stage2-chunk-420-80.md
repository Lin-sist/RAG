# RAG Eval Report

- Generated at: 2026-06-30T01:26:27.820772+00:00
- Report status: `RETRIEVAL_ONLY`
- askErrors count: `0`
- retrieveErrors count: `0`
- skippedAsk count: `30`
- rateLimitErrors count: `0`
- retry count: `0`
- Metrics safe for comparison: `retrieval metrics only`
- Base URL: `http://localhost:8080`
- Knowledge base ID: `13`
- Eval set: `docs\eval\rag_eval_set.jsonl`
- topK: `5`
- minScore: `0.3`
- enableRerank: `True`
- skipAsk: `True`
- Eval KB name: `codex-stage2-fill-chunk-420-80`
- Eval KB vector collection: `kb_8625d9db53c04eeb`
- Eval KB document count: `3`
- Eval KB chunk count: `50`
- Fixture files: `springboot-basics.md sha256=c51df5761d510aa4c8a5fd610c90454b12973e2999138c3b57ba83768a296521; java-interview-guide.md sha256=a33f16e91025e9a8d92274c4221d1c172bb4f68c03790db53b8e20157ef4faa0; rag-technology-guide.md sha256=59ad5d66a59be2ce4e517ca81e37fde06b7673f3da7f798a4c76b01cc6f348a9`
- Config snapshot: `rag-admin\src\main\resources\application.yml sha256=30ad69759a640af8a482da1dc734ddbf92abe8f59f351b83ed5ff297f153501e; rag-core\src\main\java\com\enterprise\rag\core\rag\query\RetrievalProperties.java sha256=f4d49e1ed6eeee0dc5c358df16bec0ec7424dd97b7dd03afeb1ca4f201e391e6; rag-document\src\main\java\com\enterprise\rag\document\chunker\ChunkConfig.java sha256=01b17712c8974055d15a30034e396d5074ba37ad8ea637fa7bca895a0c8a8554`
- Git HEAD: `2ba2970f9488998f69e03b73cd75ae5b55f80620`
- askDelaySeconds: `0.0`
- maxAskRetries: `0`
- retryBackoffSeconds: `0.0`
- Duration: `34.26s`

## Summary Metrics

| Metric | Value |
|---|---:|
| Samples | 30 |
| Answerable samples | 27 |
| No-answer samples | 3 |
| Recall@3 | 68.63% |
| Recall@5 | 68.63% |
| MRR | 0.7346 |
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
| fact-008 | fact | 1/2 | 3 | skipped | skipped | skipped | skipped | 0 | - |  |
| fact-009 | fact | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| fact-010 | fact | 0/1 | - | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-001 | definition | 1/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-002 | definition | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-003 | definition | 2/2 | 2 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-004 | definition | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-005 | definition | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-006 | definition | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-007 | definition | 1/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| definition-008 | definition | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| reasoning-001 | reasoning | 2/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
| reasoning-002 | reasoning | 1/2 | 1 | skipped | skipped | skipped | skipped | 0 | - |  |
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
    "documentId": 48,
    "chunkId": "0",
    "contentPreview": "核心注解 2.1 启动类注解 @SpringBootApplication public class Application { public static void main(String[] args) { SpringApplication.run(Application.class, args); } } @SpringBootApplication 是组合注解，包含： @Configuration ：标记为配置类 @EnableAutoConfiguration ：启用自动配置 @ComponentScan ：组件扫描 2.2 常用注解 @RestController ：= @Con...",
    "metadata": {
      "chunkIndex": 1.0,
      "tokenCount": 420.0,
      "headingPath": "",
      "sourceFileName": "springboot-basics.md",
      "documentId": 48.0,
      "kbId": 13.0,
      "startIndex": 239.0,
      "endIndex": 659.0,
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
    "source": "48decea1-d138-47a5-a985-381194b56100",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 48,
    "chunkId": "48decea1-d138-47a5-a985-381194b56100",
    "contentPreview": "核心注解 2.1 启动类注解 @SpringBootApplication public class Application { public static void main(String[] args) { SpringApplication.run(Application.class, args); } } @SpringBootApplication 是组合注解，包含： @Configuration ：标记为配置类 @EnableAutoConfiguration ：启用自动配置 @ComponentScan ：组件扫描 2.2 常用注解 @RestController ：= @Con...",
    "metadata": {
      "tokenCount": 420,
      "chunkIndex": 1,
      "title": "springboot-basics.md",
      "startIndex": 239,
      "fileName": "springboot-basics.md",
      "kbId": 13,
      "sourceFileName": "springboot-basics.md",
      "documentId": 48,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 659,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "fa3ca785-dce4-4c51-83a0-4c6ac844915f",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 48,
    "chunkId": "fa3ca785-dce4-4c51-83a0-4c6ac844915f",
    "contentPreview": "ationPolicy(SessionCreationPolicy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max...",
    "metadata": {
      "tokenCount": 372,
      "chunkIndex": 13,
      "title": "springboot-basics.md",
      "startIndex": 3068,
      "fileName": "springboot-basics.md",
      "kbId": 13,
      "sourceFileName": "springboot-basics.md",
      "documentId": 48,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 3440,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "87a9428a-6fae-444b-8fcb-29586f9814bd",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 48,
    "chunkId": "87a9428a-6fae-444b-8fcb-29586f9814bd",
    "contentPreview": "1.1 @Scheduled 注解 @Component public class ScheduledTasks { // 每 5 秒执行一次 @Scheduled(fixedRate = 5000) public void reportCurrentTime() { log.info(\"当前时间：{}\", LocalDateTime.now()); }",
    "metadata": {
      "tokenCount": 199,
      "chunkIndex": 20,
      "title": "springboot-basics.md",
      "startIndex": 4759,
      "fileName": "springboot-basics.md",
      "kbId": 13,
      "sourceFileName": "springboot-basics.md",
      "documentId": 48,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 4958,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "8c38c830-07f4-4ebc-9303-2b59d08a90e6",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 48,
    "chunkId": "8c38c830-07f4-4ebc-9303-2b59d08a90e6",
    "contentPreview": "rn executor; } } @Service public class EmailService { @Async public CompletableFuture<String> sendEmail(String to, String subject) { // 异步发送邮件 return CompletableFuture.completedFuture(\"SUCCESS\"); } } 11. 定时任务 11.1 @Scheduled 注解 @Component public class ScheduledTasks {",
    "metadata": {
      "tokenCount": 298,
      "chunkIndex": 19,
      "title": "springboot-basics.md",
      "startIndex": 4519,
      "fileName": "springboot-basics.md",
      "kbId": 13,
      "sourceFileName": "springboot-basics.md",
      "documentId": 48,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 4817,
      "headingPath": "",
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
    "documentId": 48,
    "chunkId": "0",
    "contentPreview": "ationPolicy(SessionCreationPolicy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max...",
    "metadata": {
      "chunkIndex": 13.0,
      "tokenCount": 372.0,
      "headingPath": "",
      "sourceFileName": "springboot-basics.md",
      "documentId": 48.0,
      "kbId": 13.0,
      "startIndex": 3068.0,
      "endIndex": 3440.0,
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
    "source": "fa3ca785-dce4-4c51-83a0-4c6ac844915f",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 48,
    "chunkId": "fa3ca785-dce4-4c51-83a0-4c6ac844915f",
    "contentPreview": "ationPolicy(SessionCreationPolicy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max...",
    "metadata": {
      "tokenCount": 372,
      "chunkIndex": 13,
      "title": "springboot-basics.md",
      "startIndex": 3068,
      "fileName": "springboot-basics.md",
      "kbId": 13,
      "sourceFileName": "springboot-basics.md",
      "documentId": 48,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 3440,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.01515151560306549,
    "source": "f8ead395-9f1a-4854-a98f-7e7e0faa2796",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "f8ead395-9f1a-4854-a98f-7e7e0faa2796",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "tokenCount": 383,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1121,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1504,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015151515151515152
    }
  },
  {
    "rank": 4,
    "score": 0.016129031777381897,
    "source": "4e082e6a-345b-444a-a1c0-c8433fdb46ee",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "4e082e6a-345b-444a-a1c0-c8433fdb46ee",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？",
    "metadata": {
      "tokenCount": 319,
      "chunkIndex": 12,
      "title": "rag-technology-guide.md",
      "startIndex": 2640,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2959,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 5,
    "score": 0.01587301678955555,
    "source": "d824f101-acb5-4db8-9b12-f4fac562a6b2",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "d824f101-acb5-4db8-9b12-f4fac562a6b2",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "tokenCount": 269,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 270,
      "headingPath": "",
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
    "documentId": 49,
    "chunkId": "0",
    "contentPreview": "） 锁机制 ： 行锁：锁定某一行 表锁：锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好...",
    "metadata": {
      "chunkIndex": 7.0,
      "tokenCount": 303.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 49.0,
      "kbId": 13.0,
      "startIndex": 1884.0,
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
    "source": "0c41d7ac-c296-4270-aa6b-1153f0ca5ebe",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 49,
    "chunkId": "0c41d7ac-c296-4270-aa6b-1153f0ca5ebe",
    "contentPreview": "） 锁机制 ： 行锁：锁定某一行 表锁：锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好...",
    "metadata": {
      "tokenCount": 303,
      "chunkIndex": 7,
      "title": "java-interview-guide.md",
      "startIndex": 1884,
      "fileName": "java-interview-guide.md",
      "kbId": 13,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 49,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 2187,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "09b60cea-900d-440d-84f7-65fff4f05f05",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 49,
    "chunkId": "09b60cea-900d-440d-84f7-65fff4f05f05",
    "contentPreview": "GC 收集器 ： Serial、Parallel、CMS、G1、ZGC --- 面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID...",
    "metadata": {
      "tokenCount": 398,
      "chunkIndex": 10,
      "title": "java-interview-guide.md",
      "startIndex": 2764,
      "fileName": "java-interview-guide.md",
      "kbId": 13,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 49,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 3161,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "75aeaee2-5600-4448-9ec4-5f2ae8de3b8b",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 48,
    "chunkId": "75aeaee2-5600-4448-9ec4-5f2ae8de3b8b",
    "contentPreview": "(\"当前时间：{}\", LocalDateTime.now()); } // 每天凌晨 2 点执行 @Scheduled(cron = \"0 0 2 * * ?\") public void cleanupExpiredData() { log.info(\"清理过期数据\"); } } 12. 缓存 12.1 Spring Cache @Configuration @EnableCaching public class CacheConfig { // 配置 Redis 缓存 } @Service public class UserService { @Cacheable(value = \"use...",
    "metadata": {
      "tokenCount": 417,
      "chunkIndex": 21,
      "title": "springboot-basics.md",
      "startIndex": 4919,
      "fileName": "springboot-basics.md",
      "kbId": 13,
      "sourceFileName": "springboot-basics.md",
      "documentId": 48,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 5336,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "7d848fb5-1f0d-4be7-b2ba-64033419d6c5",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 49,
    "chunkId": "7d848fb5-1f0d-4be7-b2ba-64033419d6c5",
    "contentPreview": ". MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNCOMMITTED（读未提交） READ COMMITTED（读已提交） REPEATABLE READ（可重复读，MySQL ...",
    "metadata": {
      "tokenCount": 357,
      "chunkIndex": 6,
      "title": "java-interview-guide.md",
      "startIndex": 1564,
      "fileName": "java-interview-guide.md",
      "kbId": 13,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 49,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 1921,
      "headingPath": "",
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
- retrieve hit ratio: `1/2`
- first_match_rank: `3`
- answer:
- expected_keywords: `["Milvus", "Pinecone", "Weaviate", "Qdrant"]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "ba7ca1f6-6254-42a4-8491-376c2a5faf5a",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "ba7ca1f6-6254-42a4-8491-376c2a5faf5a",
    "contentPreview": "提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召回相关文档 → 构建提示词 → LLM 生成答案 → 返回结果 2.1 详细步骤 Step 1: 文档处理（离线） 文档上传 ：PDF、Markdown、Word、代码文件等 文档解析 ：提取文本内容 文本分块 ：将长文档切分为小块（Chunk），通常 500-2000 字符 向量化 ：使用 Embedding 模型将文本转换为向量 存储 ：向量存入向量数据库（Milvus、Pinecone 等） Step 2: 问答处理（在线） 问题向量化 ：用户问题转换为向量 相似度检索 ...",
    "metadata": {
      "tokenCount": 389,
      "chunkIndex": 1,
      "title": "rag-technology-guide.md",
      "startIndex": 241,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 631,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 2,
    "score": 0.015625,
    "source": "9ad45965-c8af-49bc-b2c6-1530cae47e03",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "9ad45965-c8af-49bc-b2c6-1530cae47e03",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "tokenCount": 333,
      "chunkIndex": 10,
      "title": "rag-technology-guide.md",
      "startIndex": 2000,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2333,
      "headingPath": "",
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
    "documentId": 50,
    "chunkId": "0",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "chunkIndex": 4.0,
      "tokenCount": 232.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50.0,
      "kbId": 13.0,
      "startIndex": 961.0,
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
    "rank": 4,
    "score": 0.016129031777381897,
    "source": "92ae6757-7953-42f2-9678-2eb8698114d9",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "92ae6757-7953-42f2-9678-2eb8698114d9",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "tokenCount": 232,
      "chunkIndex": 4,
      "title": "rag-technology-guide.md",
      "startIndex": 961,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1193,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 5,
    "score": 0.01587301678955555,
    "source": "785fdfa4-2e8f-4dbe-a7b9-d30e40bc720d",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "785fdfa4-2e8f-4dbe-a7b9-d30e40bc720d",
    "contentPreview": "Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.52, ..., 0.91] (相似度高) \"今天天气很好\" → [-0.78,...",
    "metadata": {
      "tokenCount": 381,
      "chunkIndex": 3,
      "title": "rag-technology-guide.md",
      "startIndex": 641,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1022,
      "headingPath": "",
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
    "documentId": 49,
    "chunkId": "0",
    "contentPreview": ". MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNCOMMITTED（读未提交） READ COMMITTED（读已提交） REPEATABLE READ（可重复读，MySQL ...",
    "metadata": {
      "chunkIndex": 6.0,
      "tokenCount": 357.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 49.0,
      "kbId": 13.0,
      "startIndex": 1564.0,
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
    "source": "7d848fb5-1f0d-4be7-b2ba-64033419d6c5",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 49,
    "chunkId": "7d848fb5-1f0d-4be7-b2ba-64033419d6c5",
    "contentPreview": ". MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNCOMMITTED（读未提交） READ COMMITTED（读已提交） REPEATABLE READ（可重复读，MySQL ...",
    "metadata": {
      "tokenCount": 357,
      "chunkIndex": 6,
      "title": "java-interview-guide.md",
      "startIndex": 1564,
      "fileName": "java-interview-guide.md",
      "kbId": 13,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 49,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 1921,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "92ae6757-7953-42f2-9678-2eb8698114d9",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "92ae6757-7953-42f2-9678-2eb8698114d9",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "tokenCount": 232,
      "chunkIndex": 4,
      "title": "rag-technology-guide.md",
      "startIndex": 961,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1193,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.015625,
    "source": "f8ead395-9f1a-4854-a98f-7e7e0faa2796",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "f8ead395-9f1a-4854-a98f-7e7e0faa2796",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "tokenCount": 383,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1121,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1504,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 5,
    "score": 0.01515151560306549,
    "source": "09b60cea-900d-440d-84f7-65fff4f05f05",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 49,
    "chunkId": "09b60cea-900d-440d-84f7-65fff4f05f05",
    "contentPreview": "GC 收集器 ： Serial、Parallel、CMS、G1、ZGC --- 面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID...",
    "metadata": {
      "tokenCount": 398,
      "chunkIndex": 10,
      "title": "java-interview-guide.md",
      "startIndex": 2764,
      "fileName": "java-interview-guide.md",
      "kbId": 13,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 49,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 3161,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015151515151515152
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
    "documentId": 50,
    "chunkId": "0",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "chunkIndex": 0.0,
      "tokenCount": 269.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50.0,
      "kbId": 13.0,
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
    "source": "d824f101-acb5-4db8-9b12-f4fac562a6b2",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "d824f101-acb5-4db8-9b12-f4fac562a6b2",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "tokenCount": 269,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 270,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "f8ead395-9f1a-4854-a98f-7e7e0faa2796",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "f8ead395-9f1a-4854-a98f-7e7e0faa2796",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "tokenCount": 383,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1121,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1504,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "9ad45965-c8af-49bc-b2c6-1530cae47e03",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "9ad45965-c8af-49bc-b2c6-1530cae47e03",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "tokenCount": 333,
      "chunkIndex": 10,
      "title": "rag-technology-guide.md",
      "startIndex": 2000,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2333,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "f55abc0e-18f4-47cf-8d5c-61ae6e0c67fc",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "f55abc0e-18f4-47cf-8d5c-61ae6e0c67fc",
    "contentPreview": "：总结 4. 提示词工程（Prompt Engineering） RAG 系统需要精心设计提示词，将检索到的上下文和问题结合。 4.1 典型 Prompt 模板 你是一个专业的技术助手。请根据以下文档内容回答用户的问题。",
    "metadata": {
      "tokenCount": 110,
      "chunkIndex": 6,
      "title": "rag-technology-guide.md",
      "startIndex": 1441,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1550,
      "headingPath": "",
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
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 49,
    "chunkId": "0",
    "contentPreview": "） 锁机制 ： 行锁：锁定某一行 表锁：锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好...",
    "metadata": {
      "chunkIndex": 7.0,
      "tokenCount": 303.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 49.0,
      "kbId": 13.0,
      "startIndex": 1884.0,
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
    "source": "0c41d7ac-c296-4270-aa6b-1153f0ca5ebe",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 49,
    "chunkId": "0c41d7ac-c296-4270-aa6b-1153f0ca5ebe",
    "contentPreview": "） 锁机制 ： 行锁：锁定某一行 表锁：锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好...",
    "metadata": {
      "tokenCount": 303,
      "chunkIndex": 7,
      "title": "java-interview-guide.md",
      "startIndex": 1884,
      "fileName": "java-interview-guide.md",
      "kbId": 13,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 49,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 2187,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.01587301678955555,
    "source": "75aeaee2-5600-4448-9ec4-5f2ae8de3b8b",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 48,
    "chunkId": "75aeaee2-5600-4448-9ec4-5f2ae8de3b8b",
    "contentPreview": "(\"当前时间：{}\", LocalDateTime.now()); } // 每天凌晨 2 点执行 @Scheduled(cron = \"0 0 2 * * ?\") public void cleanupExpiredData() { log.info(\"清理过期数据\"); } } 12. 缓存 12.1 Spring Cache @Configuration @EnableCaching public class CacheConfig { // 配置 Redis 缓存 } @Service public class UserService { @Cacheable(value = \"use...",
    "metadata": {
      "tokenCount": 417,
      "chunkIndex": 21,
      "title": "springboot-basics.md",
      "startIndex": 4919,
      "fileName": "springboot-basics.md",
      "kbId": 13,
      "sourceFileName": "springboot-basics.md",
      "documentId": 48,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 5336,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 4,
    "score": 0.015625,
    "source": "6514b6bb-7035-4f98-82e5-2158b00cc0d1",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 48,
    "chunkId": "6514b6bb-7035-4f98-82e5-2158b00cc0d1",
    "contentPreview": "数据库，之后从缓存读取 } @CacheEvict(value = \"users\", key = \"#id\") public void deleteById(Long id) { // 删除缓存 } @CachePut(value = \"users\", key = \"#user.id\") public User update(User user) { // 更新缓存 } } 13. 单元测试 13.1 测试示例 @SpringBootTest @AutoConfigureMockMvc class UserControllerTest { @Autowired private MockMvc ...",
    "metadata": {
      "tokenCount": 399,
      "chunkIndex": 22,
      "title": "springboot-basics.md",
      "startIndex": 5319,
      "fileName": "springboot-basics.md",
      "kbId": 13,
      "sourceFileName": "springboot-basics.md",
      "documentId": 48,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 5718,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 5,
    "score": 0.015384615398943424,
    "source": "18c1749c-6e32-46fb-93be-6c83881a9fd6",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 49,
    "chunkId": "18c1749c-6e32-46fb-93be-6c83881a9fd6",
    "contentPreview": "l 通过 spring.profiles.active 切换 4. MyBatis / MyBatis-Plus 4.1 MyBatis 核心 #{} vs ${} ： #{} 预编译，防止 SQL 注入 ${} 字符串替换，存在注入风险 一级缓存 vs 二级缓存 ： 一级缓存：SqlSession 级别，默认开启 二级缓存：Mapper 级别，需要手动开启 4.2 MyBatis-Plus 优势 ： 内置通用 CRUD 方法 代码生成器 分页插件 乐观锁、逻辑删除支持 5.",
    "metadata": {
      "tokenCount": 241,
      "chunkIndex": 5,
      "title": "java-interview-guide.md",
      "startIndex": 1324,
      "fileName": "java-interview-guide.md",
      "kbId": 13,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 49,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 1565,
      "headingPath": "",
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

### reasoning-002 (reasoning)

- id: `reasoning-002`
- type: `reasoning`
- question: 为什么 RAG 在知识更新和可解释性上通常比微调更适合企业知识库？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["RAG vs 微调", "知识更新"]`
- retrieve hit ratio: `1/2`
- first_match_rank: `1`
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
    "documentId": 50,
    "chunkId": "0",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "chunkIndex": 10.0,
      "tokenCount": 333.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50.0,
      "kbId": 13.0,
      "startIndex": 2000.0,
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
    "source": "9ad45965-c8af-49bc-b2c6-1530cae47e03",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "9ad45965-c8af-49bc-b2c6-1530cae47e03",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "tokenCount": 333,
      "chunkIndex": 10,
      "title": "rag-technology-guide.md",
      "startIndex": 2000,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2333,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "d824f101-acb5-4db8-9b12-f4fac562a6b2",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "d824f101-acb5-4db8-9b12-f4fac562a6b2",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "tokenCount": 269,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 270,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.015384615398943424,
    "source": "f8ead395-9f1a-4854-a98f-7e7e0faa2796",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "f8ead395-9f1a-4854-a98f-7e7e0faa2796",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "tokenCount": 383,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1121,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1504,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015384615384615385
    }
  },
  {
    "rank": 5,
    "score": 0.01587301678955555,
    "source": "c03f8f25-f695-4027-b5b8-e05800318e0c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "c03f8f25-f695-4027-b5b8-e05800318e0c",
    "contentPreview": "3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文档、技术博客 优势 ：快速查找代码示例、技术方案 7.4 法律文书检索 场景 ：...",
    "metadata": {
      "tokenCount": 328,
      "chunkIndex": 11,
      "title": "rag-technology-guide.md",
      "startIndex": 2320,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2648,
      "headingPath": "",
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
    "score": 0.016129031777381897,
    "source": "f8ead395-9f1a-4854-a98f-7e7e0faa2796",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "f8ead395-9f1a-4854-a98f-7e7e0faa2796",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "tokenCount": 383,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1121,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1504,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 2,
    "score": 0.01587301678955555,
    "source": "92ae6757-7953-42f2-9678-2eb8698114d9",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "92ae6757-7953-42f2-9678-2eb8698114d9",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "tokenCount": 232,
      "chunkIndex": 4,
      "title": "rag-technology-guide.md",
      "startIndex": 961,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1193,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 3,
    "score": 0.015384615398943424,
    "source": "4e082e6a-345b-444a-a1c0-c8433fdb46ee",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "4e082e6a-345b-444a-a1c0-c8433fdb46ee",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？",
    "metadata": {
      "tokenCount": 319,
      "chunkIndex": 12,
      "title": "rag-technology-guide.md",
      "startIndex": 2640,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2959,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015384615384615385
    }
  },
  {
    "rank": 4,
    "score": 0.016393441706895828,
    "source": "9ad45965-c8af-49bc-b2c6-1530cae47e03",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "9ad45965-c8af-49bc-b2c6-1530cae47e03",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "tokenCount": 333,
      "chunkIndex": 10,
      "title": "rag-technology-guide.md",
      "startIndex": 2000,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2333,
      "headingPath": "",
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
    "documentId": 50,
    "chunkId": "0",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "chunkIndex": 0.0,
      "tokenCount": 269.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50.0,
      "kbId": 13.0,
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
    "documentId": 49,
    "chunkId": "0",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "chunkIndex": 1.0,
      "tokenCount": 420.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 49.0,
      "kbId": 13.0,
      "startIndex": 18.0,
      "endIndex": 438.0,
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
    "source": "610d69f3-2a66-46b6-a6d0-456f3f0598f0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 49,
    "chunkId": "610d69f3-2a66-46b6-a6d0-456f3f0598f0",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "tokenCount": 420,
      "chunkIndex": 1,
      "title": "java-interview-guide.md",
      "startIndex": 18,
      "fileName": "java-interview-guide.md",
      "kbId": 13,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 49,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 438,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "92ae6757-7953-42f2-9678-2eb8698114d9",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "92ae6757-7953-42f2-9678-2eb8698114d9",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "tokenCount": 232,
      "chunkIndex": 4,
      "title": "rag-technology-guide.md",
      "startIndex": 961,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1193,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "f8ead395-9f1a-4854-a98f-7e7e0faa2796",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "f8ead395-9f1a-4854-a98f-7e7e0faa2796",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "tokenCount": 383,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1121,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1504,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015384615398943424,
    "source": "4e082e6a-345b-444a-a1c0-c8433fdb46ee",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "4e082e6a-345b-444a-a1c0-c8433fdb46ee",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？",
    "metadata": {
      "tokenCount": 319,
      "chunkIndex": 12,
      "title": "rag-technology-guide.md",
      "startIndex": 2640,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2959,
      "headingPath": "",
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
    "score": 0.016393441706895828,
    "source": "92ae6757-7953-42f2-9678-2eb8698114d9",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "92ae6757-7953-42f2-9678-2eb8698114d9",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "tokenCount": 232,
      "chunkIndex": 4,
      "title": "rag-technology-guide.md",
      "startIndex": 961,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1193,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 2,
    "score": 0.01587301678955555,
    "source": "f8ead395-9f1a-4854-a98f-7e7e0faa2796",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "f8ead395-9f1a-4854-a98f-7e7e0faa2796",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "tokenCount": 383,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1121,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1504,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 3,
    "score": 0.015625,
    "source": "4e082e6a-345b-444a-a1c0-c8433fdb46ee",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "4e082e6a-345b-444a-a1c0-c8433fdb46ee",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？",
    "metadata": {
      "tokenCount": 319,
      "chunkIndex": 12,
      "title": "rag-technology-guide.md",
      "startIndex": 2640,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2959,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 4,
    "score": 0.01515151560306549,
    "source": "9ad45965-c8af-49bc-b2c6-1530cae47e03",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "9ad45965-c8af-49bc-b2c6-1530cae47e03",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "tokenCount": 333,
      "chunkIndex": 10,
      "title": "rag-technology-guide.md",
      "startIndex": 2000,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2333,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015151515151515152
    }
  },
  {
    "rank": 5,
    "score": 0.014492753893136978,
    "source": "2cafb421-1c4d-42df-843e-024b316042c4",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "2cafb421-1c4d-42df-843e-024b316042c4",
    "contentPreview": "【相关文档】 {retrieved_context} 【用户提问】 {user_question} 【回答要求】 1. 基于文档内容回答，不要编造信息 2. 如果文档中没有相关信息，明确说明 3. 引用具体的文档片段作为依据 4. 回答要简洁清晰",
    "metadata": {
      "tokenCount": 125,
      "chunkIndex": 7,
      "title": "rag-technology-guide.md",
      "startIndex": 1550,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1679,
      "headingPath": "",
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
    "documentId": 50,
    "chunkId": "0",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "chunkIndex": 10.0,
      "tokenCount": 333.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50.0,
      "kbId": 13.0,
      "startIndex": 2000.0,
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
    "source": "9ad45965-c8af-49bc-b2c6-1530cae47e03",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "9ad45965-c8af-49bc-b2c6-1530cae47e03",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "tokenCount": 333,
      "chunkIndex": 10,
      "title": "rag-technology-guide.md",
      "startIndex": 2000,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2333,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "790357f9-c2f9-463b-aaa2-64b18b42b5b6",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "790357f9-c2f9-463b-aaa2-64b18b42b5b6",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "tokenCount": 341,
      "chunkIndex": 9,
      "title": "rag-technology-guide.md",
      "startIndex": 1679,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2021,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "965725e7-21d5-4380-ba19-376296b04580",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "965725e7-21d5-4380-ba19-376296b04580",
    "contentPreview": "文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈 关键指标 ： 检索召回率：Top-K 包含正确答案的比例 答案准确率：生成答案的正确性 用户满意度：...",
    "metadata": {
      "tokenCount": 347,
      "chunkIndex": 13,
      "title": "rag-technology-guide.md",
      "startIndex": 2880,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "f8ead395-9f1a-4854-a98f-7e7e0faa2796",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "f8ead395-9f1a-4854-a98f-7e7e0faa2796",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "tokenCount": 383,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1121,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1504,
      "headingPath": "",
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
    "documentId": 49,
    "chunkId": "0",
    "contentPreview": "GC 收集器 ： Serial、Parallel、CMS、G1、ZGC --- 面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID...",
    "metadata": {
      "chunkIndex": 10.0,
      "tokenCount": 398.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 49.0,
      "kbId": 13.0,
      "startIndex": 2764.0,
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
    "source": "09b60cea-900d-440d-84f7-65fff4f05f05",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 49,
    "chunkId": "09b60cea-900d-440d-84f7-65fff4f05f05",
    "contentPreview": "GC 收集器 ： Serial、Parallel、CMS、G1、ZGC --- 面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID...",
    "metadata": {
      "tokenCount": 398,
      "chunkIndex": 10,
      "title": "java-interview-guide.md",
      "startIndex": 2764,
      "fileName": "java-interview-guide.md",
      "kbId": 13,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 49,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 3161,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "4e082e6a-345b-444a-a1c0-c8433fdb46ee",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "4e082e6a-345b-444a-a1c0-c8433fdb46ee",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？",
    "metadata": {
      "tokenCount": 319,
      "chunkIndex": 12,
      "title": "rag-technology-guide.md",
      "startIndex": 2640,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2959,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "790357f9-c2f9-463b-aaa2-64b18b42b5b6",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "790357f9-c2f9-463b-aaa2-64b18b42b5b6",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "tokenCount": 341,
      "chunkIndex": 9,
      "title": "rag-technology-guide.md",
      "startIndex": 1679,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2021,
      "headingPath": "",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.014705882407724857,
    "source": "1846226c-4ca5-4ef5-84d7-db69224e51e4",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 49,
    "chunkId": "1846226c-4ca5-4ef5-84d7-db69224e51e4",
    "contentPreview": "信 流量削峰：秒杀系统 系统解耦：订单服务和库存服务 消息可靠性 ： 生产者确认机制 消息持久化 消费者手动 ACK 9. 设计模式 9.1 常用设计模式 单例模式 ：Spring Bean 默认单例 工厂模式 ：BeanFactory 代理模式 ：AOP 模板方法模式 ：JdbcTemplate、RestTemplate 观察者模式 ：Spring 事件监听 策略模式 ：支付方式选择 10. JVM 10.1 内存模型 堆 ：对象实例，GC 主要区域 栈 ：方法调用，局部变量 方法区 ：类信息、常量池 程序计数器 ：当前线程执行的字节码行号 10.2 垃圾回收 GC 算法 ： 标记-清除 复...",
    "metadata": {
      "tokenCount": 387,
      "chunkIndex": 9,
      "title": "java-interview-guide.md",
      "startIndex": 2444,
      "fileName": "java-interview-guide.md",
      "kbId": 13,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 49,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 2831,
      "headingPath": "",
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
    "source": "d824f101-acb5-4db8-9b12-f4fac562a6b2",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "d824f101-acb5-4db8-9b12-f4fac562a6b2",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "tokenCount": 269,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 270,
      "headingPath": ""
    }
  },
  {
    "rank": 2,
    "score": 0.7920469045639038,
    "source": "4e082e6a-345b-444a-a1c0-c8433fdb46ee",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "4e082e6a-345b-444a-a1c0-c8433fdb46ee",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？",
    "metadata": {
      "tokenCount": 319,
      "chunkIndex": 12,
      "title": "rag-technology-guide.md",
      "startIndex": 2640,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2959,
      "headingPath": ""
    }
  },
  {
    "rank": 3,
    "score": 0.4335298240184784,
    "source": "965725e7-21d5-4380-ba19-376296b04580",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "965725e7-21d5-4380-ba19-376296b04580",
    "contentPreview": "文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈 关键指标 ： 检索召回率：Top-K 包含正确答案的比例 答案准确率：生成答案的正确性 用户满意度：...",
    "metadata": {
      "tokenCount": 347,
      "chunkIndex": 13,
      "title": "rag-technology-guide.md",
      "startIndex": 2880,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 3227,
      "headingPath": ""
    }
  },
  {
    "rank": 4,
    "score": 0.33610081672668457,
    "source": "92ae6757-7953-42f2-9678-2eb8698114d9",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "92ae6757-7953-42f2-9678-2eb8698114d9",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "tokenCount": 232,
      "chunkIndex": 4,
      "title": "rag-technology-guide.md",
      "startIndex": 961,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1193,
      "headingPath": ""
    }
  },
  {
    "rank": 5,
    "score": 0.2595656216144562,
    "source": "f8ead395-9f1a-4854-a98f-7e7e0faa2796",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "f8ead395-9f1a-4854-a98f-7e7e0faa2796",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "tokenCount": 383,
      "chunkIndex": 5,
      "title": "rag-technology-guide.md",
      "startIndex": 1121,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1504,
      "headingPath": ""
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
    "source": "610d69f3-2a66-46b6-a6d0-456f3f0598f0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 49,
    "chunkId": "610d69f3-2a66-46b6-a6d0-456f3f0598f0",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "tokenCount": 420,
      "chunkIndex": 1,
      "title": "java-interview-guide.md",
      "startIndex": 18,
      "fileName": "java-interview-guide.md",
      "kbId": 13,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 49,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 438,
      "headingPath": ""
    }
  },
  {
    "rank": 2,
    "score": 0.655103325843811,
    "source": "9ad45965-c8af-49bc-b2c6-1530cae47e03",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "9ad45965-c8af-49bc-b2c6-1530cae47e03",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "tokenCount": 333,
      "chunkIndex": 10,
      "title": "rag-technology-guide.md",
      "startIndex": 2000,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 2333,
      "headingPath": ""
    }
  },
  {
    "rank": 3,
    "score": 0.6072593927383423,
    "source": "cfed8826-05cb-427d-bc5c-41948b1fb3f6",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 49,
    "chunkId": "cfed8826-05cb-427d-bc5c-41948b1fb3f6",
    "contentPreview": "线程池 ThreadPoolExecutor ： 核心线程数 corePoolSize 最大线程数 maximumPoolSize 任务队列 workQueue 拒绝策略 RejectedExecutionHandler synchronized vs Lock ： synchronized 自动释放锁，Lock 需要手动释放 Lock 支持更灵活的锁机制（可中断、可超时） volatile 关键字 ： 保证可见性：一个线程修改，其他线程立即可见 禁止指令重排序 不保证原子性 2.",
    "metadata": {
      "tokenCount": 244,
      "chunkIndex": 2,
      "title": "java-interview-guide.md",
      "startIndex": 358,
      "fileName": "java-interview-guide.md",
      "kbId": 13,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 49,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 602,
      "headingPath": ""
    }
  },
  {
    "rank": 4,
    "score": 0.34844863414764404,
    "source": "785fdfa4-2e8f-4dbe-a7b9-d30e40bc720d",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "785fdfa4-2e8f-4dbe-a7b9-d30e40bc720d",
    "contentPreview": "Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.52, ..., 0.91] (相似度高) \"今天天气很好\" → [-0.78,...",
    "metadata": {
      "tokenCount": 381,
      "chunkIndex": 3,
      "title": "rag-technology-guide.md",
      "startIndex": 641,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1022,
      "headingPath": ""
    }
  },
  {
    "rank": 5,
    "score": 0.24985453486442566,
    "source": "92ae6757-7953-42f2-9678-2eb8698114d9",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "92ae6757-7953-42f2-9678-2eb8698114d9",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "tokenCount": 232,
      "chunkIndex": 4,
      "title": "rag-technology-guide.md",
      "startIndex": 961,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 1193,
      "headingPath": ""
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
    "source": "5f95020a-ceea-4a3b-b694-577c62750111",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 48,
    "chunkId": "5f95020a-ceea-4a3b-b694-577c62750111",
    "contentPreview": "Spring Boot 核心知识点 1. Spring Boot 简介 Spring Boot 是基于 Spring 框架的快速开发脚手架，旨在简化 Spring 应用的初始搭建和开发过程。 1.1 核心特性 自动配置 ：根据 classpath 下的依赖自动配置 Spring 应用 起步依赖 ：一站式依赖管理，避免版本冲突 内嵌服务器 ：Tomcat、Jetty、Undertow 生产就绪 ：Actuator 健康检查、监控指标 无代码生成 ：不需要 XML 配置 2.",
    "metadata": {
      "tokenCount": 238,
      "chunkIndex": 0,
      "title": "springboot-basics.md",
      "startIndex": 0,
      "fileName": "springboot-basics.md",
      "kbId": 13,
      "sourceFileName": "springboot-basics.md",
      "documentId": 48,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 239,
      "headingPath": ""
    }
  },
  {
    "rank": 2,
    "score": 0.8596554398536682,
    "source": "610d69f3-2a66-46b6-a6d0-456f3f0598f0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 49,
    "chunkId": "610d69f3-2a66-46b6-a6d0-456f3f0598f0",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "tokenCount": 420,
      "chunkIndex": 1,
      "title": "java-interview-guide.md",
      "startIndex": 18,
      "fileName": "java-interview-guide.md",
      "kbId": 13,
      "sourceFileName": "java-interview-guide.md",
      "documentId": 49,
      "documentTitle": "java-interview-guide.md",
      "originalFilename": "java-interview-guide.md",
      "endIndex": 438,
      "headingPath": ""
    }
  },
  {
    "rank": 3,
    "score": 0.5597094297409058,
    "source": "fa3ca785-dce4-4c51-83a0-4c6ac844915f",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 48,
    "chunkId": "fa3ca785-dce4-4c51-83a0-4c6ac844915f",
    "contentPreview": "ationPolicy(SessionCreationPolicy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max...",
    "metadata": {
      "tokenCount": 372,
      "chunkIndex": 13,
      "title": "springboot-basics.md",
      "startIndex": 3068,
      "fileName": "springboot-basics.md",
      "kbId": 13,
      "sourceFileName": "springboot-basics.md",
      "documentId": 48,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 3440,
      "headingPath": ""
    }
  },
  {
    "rank": 4,
    "score": 0.5052065849304199,
    "source": "d824f101-acb5-4db8-9b12-f4fac562a6b2",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 50,
    "chunkId": "d824f101-acb5-4db8-9b12-f4fac562a6b2",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "tokenCount": 269,
      "chunkIndex": 0,
      "title": "rag-technology-guide.md",
      "startIndex": 0,
      "fileName": "rag-technology-guide.md",
      "kbId": 13,
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 50,
      "documentTitle": "rag-technology-guide.md",
      "originalFilename": "rag-technology-guide.md",
      "endIndex": 270,
      "headingPath": ""
    }
  },
  {
    "rank": 5,
    "score": 0.5152035355567932,
    "source": "3db369b1-7e1f-4ad2-b8a2-d43a221b2e26",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 48,
    "chunkId": "3db369b1-7e1f-4ad2-b8a2-d43a221b2e26",
    "contentPreview": "@Transactional(rollbackFor = Exception.class) public void createOrder(Order order) { orderRepository.save(order); // 如果发生异常，自动回滚 stockService.deduct(order.getProductId(), order.getQuantity()); } } 9.2 事务传播行为 REQUIRED （默认）：加入当前事务，没有则新建 REQUIRES_NEW ：总是新建事务 SUPPORTS ：有事务就加入，没有就非事务执行 NOT_SUPPORTED ：非事务...",
    "metadata": {
      "tokenCount": 366,
      "chunkIndex": 16,
      "title": "springboot-basics.md",
      "startIndex": 3754,
      "fileName": "springboot-basics.md",
      "kbId": 13,
      "sourceFileName": "springboot-basics.md",
      "documentId": 48,
      "documentTitle": "springboot-basics.md",
      "originalFilename": "springboot-basics.md",
      "endIndex": 4121,
      "headingPath": ""
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
| fact-001 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: 5f95020a ceea 4a3b b694 577c62750111, 5f95020aceea4a3bb694577c62750111, springboot basics, springbootbasics; r3: d9c00212 0e9a 45d2 acf5 ff87a5a8c999, d9c002120e9a45d2acf5ff87a5a8c999, springboot basics, springbootbasics; r4: 3c0cfe00 f88b 4534 9ed0 9f49ad21074a, 3c0cfe00f88b45349ed09f49ad21074a, java interview guide, javainterviewguide; r5: 18c1749c 6e32 46fb 93be 6c83881a9fd6, 18c1749c6e3246fb93be6c83881a9fd6, java interview guide, javainterviewguide |  |
| fact-002 | springboot-basics.md => springboot basics, springbootbasics; java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, springboot basics, springbootbasics; r2: 48decea1 d138 47a5 a985 381194b56100, 48decea1d13847a5a985381194b56100, springboot basics, springbootbasics; r3: 3c0cfe00 f88b 4534 9ed0 9f49ad21074a, 3c0cfe00f88b45349ed09f49ad21074a, java interview guide, javainterviewguide; r4: 87a9428a 6fae 444b 8fcb 29586f9814bd, 87a9428a6fae444b8fcb29586f9814bd, springboot basics, springbootbasics; r5: 8c38c830 07f4 4ebc 9303 2b59d08a90e6, 8c38c83007f44ebc93032b59d08a90e6, springboot basics, springbootbasics |  |
| fact-003 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: 48decea1 d138 47a5 a985 381194b56100, 48decea1d13847a5a985381194b56100, springboot basics, springbootbasics; r3: fa3ca785 dce4 4c51 83a0 4c6ac844915f, fa3ca785dce44c5183a04c6ac844915f, springboot basics, springbootbasics; r4: 87a9428a 6fae 444b 8fcb 29586f9814bd, 87a9428a6fae444b8fcb29586f9814bd, springboot basics, springbootbasics; r5: 8c38c830 07f4 4ebc 9303 2b59d08a90e6, 8c38c83007f44ebc93032b59d08a90e6, springboot basics, springbootbasics |  |
| fact-004 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: 0164cf00 cabc 466c 92a5 ef821dd89e2d, 0164cf00cabc466c92a5ef821dd89e2d, springboot basics, springbootbasics; r3: 5f95020a ceea 4a3b b694 577c62750111, 5f95020aceea4a3bb694577c62750111, springboot basics, springbootbasics; r4: 3c0cfe00 f88b 4534 9ed0 9f49ad21074a, 3c0cfe00f88b45349ed09f49ad21074a, java interview guide, javainterviewguide; r5: 4e082e6a 345b 444a a1c0 c8433fdb46ee, 4e082e6a345b444aa1c0c8433fdb46ee, rag technology guide, ragtechnologyguide |  |
| fact-005 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: fa3ca785 dce4 4c51 83a0 4c6ac844915f, fa3ca785dce44c5183a04c6ac844915f, springboot basics, springbootbasics; r3: f8ead395 9f1a 4854 a98f 7e7e0faa2796, f8ead3959f1a4854a98f7e7e0faa2796, rag technology guide, ragtechnologyguide; r4: 4e082e6a 345b 444a a1c0 c8433fdb46ee, 4e082e6a345b444aa1c0c8433fdb46ee, rag technology guide, ragtechnologyguide; r5: d824f101 acb5 4db8 9b12 f4fac562a6b2, d824f101acb54db89b12f4fac562a6b2, rag technology guide, ragtechnologyguide |  |
| fact-006 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 610d69f3 2a66 46b6 a6d0 456f3f0598f0, 610d69f32a6646b6a6d0456f3f0598f0, java interview guide, javainterviewguide; r3: 3c0cfe00 f88b 4534 9ed0 9f49ad21074a, 3c0cfe00f88b45349ed09f49ad21074a, java interview guide, javainterviewguide; r4: 40ece94c c27f 4044 958a ffc9bdf6e79a, 40ece94cc27f4044958affc9bdf6e79a, java interview guide, javainterviewguide; r5: 7d848fb5 1f0d 4be7 b2ba 64033419d6c5, 7d848fb51f0d4be7b2ba64033419d6c5, java interview guide, javainterviewguide |  |
| fact-007 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 0c41d7ac c296 4270 aa6b 1153f0ca5ebe, 0c41d7acc2964270aa6b1153f0ca5ebe, java interview guide, javainterviewguide; r3: 09b60cea 900d 440d 84f7 65fff4f05f05, 09b60cea900d440d84f765fff4f05f05, java interview guide, javainterviewguide; r4: 75aeaee2 5600 4448 9ec4 5f2ae8de3b8b, 75aeaee2560044489ec45f2ae8de3b8b, springboot basics, springbootbasics; r5: 7d848fb5 1f0d 4be7 b2ba 64033419d6c5, 7d848fb51f0d4be7b2ba64033419d6c5, java interview guide, javainterviewguide |  |
| fact-008 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: ba7ca1f6 6254 42a4 8491 376c2a5faf5a, ba7ca1f6625442a48491376c2a5faf5a, rag technology guide, ragtechnologyguide; r2: 9ad45965 c8af 49bc b2c6 1530cae47e03, 9ad45965c8af49bcb2c61530cae47e03, rag technology guide, ragtechnologyguide; r3: 0, rag technology guide, ragtechnologyguide; r4: 92ae6757 7953 42f2 9678 2eb8698114d9, 92ae6757795342f296782eb8698114d9, rag technology guide, ragtechnologyguide; r5: 785fdfa4 2e8f 4dbe a7b9 d30e40bc720d, 785fdfa42e8f4dbea7b9d30e40bc720d, rag technology guide, ragtechnologyguide |  |
| fact-009 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: ba7ca1f6 6254 42a4 8491 376c2a5faf5a, ba7ca1f6625442a48491376c2a5faf5a, rag technology guide, ragtechnologyguide; r3: 965725e7 21d5 4380 ba19 376296b04580, 965725e721d54380ba19376296b04580, rag technology guide, ragtechnologyguide; r4: 790357f9 c2f9 463b aaa2 64b18b42b5b6, 790357f9c2f9463baaa264b18b42b5b6, rag technology guide, ragtechnologyguide; r5: 224015c8 9692 489e a1a9 fcdbdc268f4c, 224015c89692489ea1a9fcdbdc268f4c, springboot basics, springbootbasics |  |
| fact-010 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 7d848fb5 1f0d 4be7 b2ba 64033419d6c5, 7d848fb51f0d4be7b2ba64033419d6c5, java interview guide, javainterviewguide; r3: 92ae6757 7953 42f2 9678 2eb8698114d9, 92ae6757795342f296782eb8698114d9, rag technology guide, ragtechnologyguide; r4: f8ead395 9f1a 4854 a98f 7e7e0faa2796, f8ead3959f1a4854a98f7e7e0faa2796, rag technology guide, ragtechnologyguide; r5: 09b60cea 900d 440d 84f7 65fff4f05f05, 09b60cea900d440d84f765fff4f05f05, java interview guide, javainterviewguide |  |
| definition-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: d824f101 acb5 4db8 9b12 f4fac562a6b2, d824f101acb54db89b12f4fac562a6b2, rag technology guide, ragtechnologyguide; r3: f8ead395 9f1a 4854 a98f 7e7e0faa2796, f8ead3959f1a4854a98f7e7e0faa2796, rag technology guide, ragtechnologyguide; r4: 9ad45965 c8af 49bc b2c6 1530cae47e03, 9ad45965c8af49bcb2c61530cae47e03, rag technology guide, ragtechnologyguide; r5: f55abc0e 18f4 47cf 8d5c 61ae6e0c67fc, f55abc0e18f447cf8d5c61ae6e0c67fc, rag technology guide, ragtechnologyguide |  |
| definition-002 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 785fdfa4 2e8f 4dbe a7b9 d30e40bc720d, 785fdfa42e8f4dbea7b9d30e40bc720d, rag technology guide, ragtechnologyguide; r3: 4e082e6a 345b 444a a1c0 c8433fdb46ee, 4e082e6a345b444aa1c0c8433fdb46ee, rag technology guide, ragtechnologyguide; r4: 4edd9020 2e19 4596 a29f 78bf3ff2acd3, 4edd90202e194596a29f78bf3ff2acd3, rag technology guide, ragtechnologyguide; r5: 9ad45965 c8af 49bc b2c6 1530cae47e03, 9ad45965c8af49bcb2c61530cae47e03, rag technology guide, ragtechnologyguide |  |
| definition-003 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 92ae6757 7953 42f2 9678 2eb8698114d9, 92ae6757795342f296782eb8698114d9, rag technology guide, ragtechnologyguide; r3: 785fdfa4 2e8f 4dbe a7b9 d30e40bc720d, 785fdfa42e8f4dbea7b9d30e40bc720d, rag technology guide, ragtechnologyguide; r4: 4e082e6a 345b 444a a1c0 c8433fdb46ee, 4e082e6a345b444aa1c0c8433fdb46ee, rag technology guide, ragtechnologyguide; r5: ba7ca1f6 6254 42a4 8491 376c2a5faf5a, ba7ca1f6625442a48491376c2a5faf5a, rag technology guide, ragtechnologyguide |  |
| definition-004 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: f8ead395 9f1a 4854 a98f 7e7e0faa2796, f8ead3959f1a4854a98f7e7e0faa2796, rag technology guide, ragtechnologyguide; r2: 92ae6757 7953 42f2 9678 2eb8698114d9, 92ae6757795342f296782eb8698114d9, rag technology guide, ragtechnologyguide; r3: ba7ca1f6 6254 42a4 8491 376c2a5faf5a, ba7ca1f6625442a48491376c2a5faf5a, rag technology guide, ragtechnologyguide; r4: 4e082e6a 345b 444a a1c0 c8433fdb46ee, 4e082e6a345b444aa1c0c8433fdb46ee, rag technology guide, ragtechnologyguide; r5: 0, rag technology guide, ragtechnologyguide |  |
| definition-005 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 40ece94c c27f 4044 958a ffc9bdf6e79a, 40ece94cc27f4044958affc9bdf6e79a, java interview guide, javainterviewguide; r3: 4e082e6a 345b 444a a1c0 c8433fdb46ee, 4e082e6a345b444aa1c0c8433fdb46ee, rag technology guide, ragtechnologyguide; r4: 5f95020a ceea 4a3b b694 577c62750111, 5f95020aceea4a3bb694577c62750111, springboot basics, springbootbasics; r5: d9c00212 0e9a 45d2 acf5 ff87a5a8c999, d9c002120e9a45d2acf5ff87a5a8c999, springboot basics, springbootbasics |  |
| definition-006 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 40ece94c c27f 4044 958a ffc9bdf6e79a, 40ece94cc27f4044958affc9bdf6e79a, java interview guide, javainterviewguide; r3: f8ead395 9f1a 4854 a98f 7e7e0faa2796, f8ead3959f1a4854a98f7e7e0faa2796, rag technology guide, ragtechnologyguide; r4: 92ae6757 7953 42f2 9678 2eb8698114d9, 92ae6757795342f296782eb8698114d9, rag technology guide, ragtechnologyguide; r5: 1846226c 4ca5 4ef5 84d7 db69224e51e4, 1846226c4ca54ef584d7db69224e51e4, java interview guide, javainterviewguide |  |
| definition-007 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 0c41d7ac c296 4270 aa6b 1153f0ca5ebe, 0c41d7acc2964270aa6b1153f0ca5ebe, java interview guide, javainterviewguide; r3: 75aeaee2 5600 4448 9ec4 5f2ae8de3b8b, 75aeaee2560044489ec45f2ae8de3b8b, springboot basics, springbootbasics; r4: 6514b6bb 7035 4f98 82e5 2158b00cc0d1, 6514b6bb70354f9882e52158b00cc0d1, springboot basics, springbootbasics; r5: 18c1749c 6e32 46fb 93be 6c83881a9fd6, 18c1749c6e3246fb93be6c83881a9fd6, java interview guide, javainterviewguide |  |
| definition-008 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: aa82addf f355 439b a13a e6d33e815dfc, aa82addff355439ba13ae6d33e815dfc, java interview guide, javainterviewguide; r3: 0c41d7ac c296 4270 aa6b 1153f0ca5ebe, 0c41d7acc2964270aa6b1153f0ca5ebe, java interview guide, javainterviewguide; r4: b6fcb148 2410 4ba0 8fff e0ff2f5552bb, b6fcb14824104ba08fffe0ff2f5552bb, rag technology guide, ragtechnologyguide; r5: 2cafb421 1c4d 42df 843e 024b316042c4, 2cafb4211c4d42df843e024b316042c4, rag technology guide, ragtechnologyguide |  |
| reasoning-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: f8ead395 9f1a 4854 a98f 7e7e0faa2796, f8ead3959f1a4854a98f7e7e0faa2796, rag technology guide, ragtechnologyguide; r2: 4e082e6a 345b 444a a1c0 c8433fdb46ee, 4e082e6a345b444aa1c0c8433fdb46ee, rag technology guide, ragtechnologyguide; r3: f55abc0e 18f4 47cf 8d5c 61ae6e0c67fc, f55abc0e18f447cf8d5c61ae6e0c67fc, rag technology guide, ragtechnologyguide; r4: 92ae6757 7953 42f2 9678 2eb8698114d9, 92ae6757795342f296782eb8698114d9, rag technology guide, ragtechnologyguide; r5: 0, rag technology guide, ragtechnologyguide |  |
| reasoning-002 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 9ad45965 c8af 49bc b2c6 1530cae47e03, 9ad45965c8af49bcb2c61530cae47e03, rag technology guide, ragtechnologyguide; r3: d824f101 acb5 4db8 9b12 f4fac562a6b2, d824f101acb54db89b12f4fac562a6b2, rag technology guide, ragtechnologyguide; r4: f8ead395 9f1a 4854 a98f 7e7e0faa2796, f8ead3959f1a4854a98f7e7e0faa2796, rag technology guide, ragtechnologyguide; r5: c03f8f25 f695 4027 b5b8 e05800318e0c, c03f8f25f6954027b5b8e05800318e0c, rag technology guide, ragtechnologyguide |  |
| reasoning-003 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: f8ead395 9f1a 4854 a98f 7e7e0faa2796, f8ead3959f1a4854a98f7e7e0faa2796, rag technology guide, ragtechnologyguide; r2: 92ae6757 7953 42f2 9678 2eb8698114d9, 92ae6757795342f296782eb8698114d9, rag technology guide, ragtechnologyguide; r3: 4e082e6a 345b 444a a1c0 c8433fdb46ee, 4e082e6a345b444aa1c0c8433fdb46ee, rag technology guide, ragtechnologyguide; r4: 9ad45965 c8af 49bc b2c6 1530cae47e03, 9ad45965c8af49bcb2c61530cae47e03, rag technology guide, ragtechnologyguide; r5: 0, rag technology guide, ragtechnologyguide |  |
| reasoning-004 | springboot-basics.md => springboot basics, springbootbasics; java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 0164cf00 cabc 466c 92a5 ef821dd89e2d, 0164cf00cabc466c92a5ef821dd89e2d, springboot basics, springbootbasics; r3: 40ece94c c27f 4044 958a ffc9bdf6e79a, 40ece94cc27f4044958affc9bdf6e79a, java interview guide, javainterviewguide; r4: 4e082e6a 345b 444a a1c0 c8433fdb46ee, 4e082e6a345b444aa1c0c8433fdb46ee, rag technology guide, ragtechnologyguide; r5: 92ae6757 7953 42f2 9678 2eb8698114d9, 92ae6757795342f296782eb8698114d9, rag technology guide, ragtechnologyguide |  |
| reasoning-005 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 610d69f3 2a66 46b6 a6d0 456f3f0598f0, 610d69f32a6646b6a6d0456f3f0598f0, java interview guide, javainterviewguide; r3: 92ae6757 7953 42f2 9678 2eb8698114d9, 92ae6757795342f296782eb8698114d9, rag technology guide, ragtechnologyguide; r4: f8ead395 9f1a 4854 a98f 7e7e0faa2796, f8ead3959f1a4854a98f7e7e0faa2796, rag technology guide, ragtechnologyguide; r5: 4e082e6a 345b 444a a1c0 c8433fdb46ee, 4e082e6a345b444aa1c0c8433fdb46ee, rag technology guide, ragtechnologyguide |  |
| reasoning-006 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 92ae6757 7953 42f2 9678 2eb8698114d9, 92ae6757795342f296782eb8698114d9, rag technology guide, ragtechnologyguide; r2: f8ead395 9f1a 4854 a98f 7e7e0faa2796, f8ead3959f1a4854a98f7e7e0faa2796, rag technology guide, ragtechnologyguide; r3: 4e082e6a 345b 444a a1c0 c8433fdb46ee, 4e082e6a345b444aa1c0c8433fdb46ee, rag technology guide, ragtechnologyguide; r4: 9ad45965 c8af 49bc b2c6 1530cae47e03, 9ad45965c8af49bcb2c61530cae47e03, rag technology guide, ragtechnologyguide; r5: 2cafb421 1c4d 42df 843e 024b316042c4, 2cafb4211c4d42df843e024b316042c4, rag technology guide, ragtechnologyguide |  |
| multi-hop-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: ba7ca1f6 6254 42a4 8491 376c2a5faf5a, ba7ca1f6625442a48491376c2a5faf5a, rag technology guide, ragtechnologyguide; r3: 965725e7 21d5 4380 ba19 376296b04580, 965725e721d54380ba19376296b04580, rag technology guide, ragtechnologyguide; r4: 4e082e6a 345b 444a a1c0 c8433fdb46ee, 4e082e6a345b444aa1c0c8433fdb46ee, rag technology guide, ragtechnologyguide; r5: c03f8f25 f695 4027 b5b8 e05800318e0c, c03f8f25f6954027b5b8e05800318e0c, rag technology guide, ragtechnologyguide |  |
| multi-hop-002 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 9ad45965 c8af 49bc b2c6 1530cae47e03, 9ad45965c8af49bcb2c61530cae47e03, rag technology guide, ragtechnologyguide; r3: 790357f9 c2f9 463b aaa2 64b18b42b5b6, 790357f9c2f9463baaa264b18b42b5b6, rag technology guide, ragtechnologyguide; r4: 965725e7 21d5 4380 ba19 376296b04580, 965725e721d54380ba19376296b04580, rag technology guide, ragtechnologyguide; r5: f8ead395 9f1a 4854 a98f 7e7e0faa2796, f8ead3959f1a4854a98f7e7e0faa2796, rag technology guide, ragtechnologyguide |  |
| multi-hop-003 | java-interview-guide.md => java interview guide, javainterviewguide; springboot-basics.md => springboot basics, springbootbasics | r1: 0, java interview guide, javainterviewguide; r2: 09b60cea 900d 440d 84f7 65fff4f05f05, 09b60cea900d440d84f765fff4f05f05, java interview guide, javainterviewguide; r3: 4e082e6a 345b 444a a1c0 c8433fdb46ee, 4e082e6a345b444aa1c0c8433fdb46ee, rag technology guide, ragtechnologyguide; r4: 790357f9 c2f9 463b aaa2 64b18b42b5b6, 790357f9c2f9463baaa264b18b42b5b6, rag technology guide, ragtechnologyguide; r5: 1846226c 4ca5 4ef5 84d7 db69224e51e4, 1846226c4ca54ef584d7db69224e51e4, java interview guide, javainterviewguide |  |
| no-answer-001 |  | r1: d824f101 acb5 4db8 9b12 f4fac562a6b2, d824f101acb54db89b12f4fac562a6b2, rag technology guide, ragtechnologyguide; r2: 4e082e6a 345b 444a a1c0 c8433fdb46ee, 4e082e6a345b444aa1c0c8433fdb46ee, rag technology guide, ragtechnologyguide; r3: 965725e7 21d5 4380 ba19 376296b04580, 965725e721d54380ba19376296b04580, rag technology guide, ragtechnologyguide; r4: 92ae6757 7953 42f2 9678 2eb8698114d9, 92ae6757795342f296782eb8698114d9, rag technology guide, ragtechnologyguide; r5: f8ead395 9f1a 4854 a98f 7e7e0faa2796, f8ead3959f1a4854a98f7e7e0faa2796, rag technology guide, ragtechnologyguide |  |
| no-answer-002 |  | r1: 610d69f3 2a66 46b6 a6d0 456f3f0598f0, 610d69f32a6646b6a6d0456f3f0598f0, java interview guide, javainterviewguide; r2: 9ad45965 c8af 49bc b2c6 1530cae47e03, 9ad45965c8af49bcb2c61530cae47e03, rag technology guide, ragtechnologyguide; r3: cfed8826 05cb 427d bc5c 41948b1fb3f6, cfed882605cb427dbc5c41948b1fb3f6, java interview guide, javainterviewguide; r4: 785fdfa4 2e8f 4dbe a7b9 d30e40bc720d, 785fdfa42e8f4dbea7b9d30e40bc720d, rag technology guide, ragtechnologyguide; r5: 92ae6757 7953 42f2 9678 2eb8698114d9, 92ae6757795342f296782eb8698114d9, rag technology guide, ragtechnologyguide |  |
| no-answer-003 |  | r1: 5f95020a ceea 4a3b b694 577c62750111, 5f95020aceea4a3bb694577c62750111, springboot basics, springbootbasics; r2: 610d69f3 2a66 46b6 a6d0 456f3f0598f0, 610d69f32a6646b6a6d0456f3f0598f0, java interview guide, javainterviewguide; r3: fa3ca785 dce4 4c51 83a0 4c6ac844915f, fa3ca785dce44c5183a04c6ac844915f, springboot basics, springbootbasics; r4: d824f101 acb5 4db8 9b12 f4fac562a6b2, d824f101acb54db89b12f4fac562a6b2, rag technology guide, ragtechnologyguide; r5: 3db369b1 7e1f 4ad2 b8a2 d43a221b2e26, 3db369b17e1f4ad2b8a2d43a221b2e26, springboot basics, springbootbasics |  |

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
