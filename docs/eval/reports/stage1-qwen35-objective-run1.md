# RAG Eval Report

- Generated at: 2026-07-12T06:50:48.481657+00:00
- Report status: `CLEAN`
- askErrors count: `0`
- retrieveErrors count: `0`
- skippedAsk count: `0`
- judgeErrors count: `0`
- skippedJudge count: `30`
- rateLimitErrors count: `0`
- retry count: `2`
- Metrics safe for comparison: `yes`
- Base URL: `http://localhost:8080`
- Knowledge base ID: `11`
- Eval set: `docs\eval\rag_eval_set.jsonl`
- sampleIds: ``
- sampleLimit: `0`
- topK: `5`
- minScore: `0.3`
- enableRerank: `True`
- skipAsk: `False`
- judgeMode: `off`
- judgeModel: ``
- judgeBaseUrl: `https://integrate.api.nvidia.com/v1`
- judgeTemperature: `0.0`
- Eval KB name: `codex-stage1-repro-eval`
- Eval KB vector collection: `kb_2addbb37622c42cb`
- Eval KB document count: `3`
- Eval KB chunk count: `50`
- Fixture files: `springboot-basics.md sha256=c51df5761d510aa4c8a5fd610c90454b12973e2999138c3b57ba83768a296521; java-interview-guide.md sha256=a33f16e91025e9a8d92274c4221d1c172bb4f68c03790db53b8e20157ef4faa0; rag-technology-guide.md sha256=59ad5d66a59be2ce4e517ca81e37fde06b7673f3da7f798a4c76b01cc6f348a9`
- Config snapshot: `rag-admin\src\main\resources\application.yml sha256=3cc64fb29060a7df30c21be51e0867b3be2a6c2e112241d4c395eb710fe8c712; rag-core\src\main\java\com\enterprise\rag\core\rag\query\RetrievalProperties.java sha256=0303394e8d11a2a88e39cdcb970323437145a6c3468f51d1f424d169399f4636; rag-document\src\main\java\com\enterprise\rag\document\chunker\ChunkConfig.java sha256=01b17712c8974055d15a30034e396d5074ba37ad8ea637fa7bca895a0c8a8554`
- Git HEAD: `5293c53869b544814bd608d684b64f0c364b4cf6`
- askTimeout: `130.0`
- askDelaySeconds: `1.0`
- maxAskRetries: `1`
- retryBackoffSeconds: `5.0`
- retryAskTimeouts: `True`
- Duration: `220.58s`

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
| Ask successful samples | 30 |
| Answerable ask successful samples | 27 |
| No-answer ask successful samples | 3 |
| Answer keyword hit rate on successful ask samples | 72.12% (75/104) |
| Citation hit rate on successful ask samples | 83.33% (25/30) |
| Citation source hit rate on successful ask samples | 83.33% (25/30) |
| Citation snippet hit rate on successful ask samples | 100.00% (67/67) |
| Unsupported citation count | 0 |
| No-answer citation violation count | 0 |
| No-answer accuracy on successful ask samples | 100.00% (3/3) |
| Judge evaluable samples | 0 |
| LLM judge pass rate | skipped (0/0) |
| Faithfulness average | skipped |
| Relevance average | skipped |

## Sample Results

| ID | Type | Retrieve | First Match | Ask | Keyword Hit | Citation Source | Citation Snippet | Unsupported Citations | No-answer OK | Judge | Errors |
|---|---|---:|---:|---|---:|---:|---:|---:|---:|---|---|
| fact-001 | fact | 2/2 | 1 | ok | 4/4 | 1/1 | 2/2 | 0 | - | skipped |  |
| fact-002 | fact | 2/2 | 1 | ok | 3/3 | 1/2 | 2/2 | 0 | - | skipped |  |
| fact-003 | fact | 0/1 | - | ok | 3/3 | 1/1 | 3/3 | 0 | - | skipped |  |
| fact-004 | fact | 2/2 | 1 | ok | 2/2 | 1/1 | 1/1 | 0 | - | skipped |  |
| fact-005 | fact | 0/2 | - | ok | 4/4 | 1/1 | 3/3 | 0 | - | skipped |  |
| fact-006 | fact | 2/2 | 1 | ok | 3/4 | 1/1 | 3/3 | 0 | - | skipped |  |
| fact-007 | fact | 1/2 | 1 | ok | 5/5 | 1/1 | 3/3 | 0 | - | skipped |  |
| fact-008 | fact | 1/2 | 3 | ok | 4/4 | 1/1 | 3/3 | 0 | - | skipped |  |
| fact-009 | fact | 2/2 | 1 | ok | 4/4 | 1/1 | 3/3 | 0 | - | skipped |  |
| fact-010 | fact | 0/1 | - | ok | 0/3 | 0/1 | - | 0 | - | skipped |  |
| definition-001 | definition | 1/2 | 1 | ok | 4/4 | 1/1 | 2/2 | 0 | - | skipped |  |
| definition-002 | definition | 2/2 | 1 | ok | 3/3 | 1/1 | 3/3 | 0 | - | skipped |  |
| definition-003 | definition | 2/2 | 2 | ok | 0/3 | 0/1 | - | 0 | - | skipped |  |
| definition-004 | definition | 2/2 | 1 | ok | 4/4 | 1/1 | 3/3 | 0 | - | skipped |  |
| definition-005 | definition | 2/2 | 1 | ok | 1/3 | 1/1 | 3/3 | 0 | - | skipped |  |
| definition-006 | definition | 2/2 | 1 | ok | 4/4 | 1/1 | 3/3 | 0 | - | skipped |  |
| definition-007 | definition | 1/2 | 1 | ok | 2/4 | 1/1 | 3/3 | 0 | - | skipped |  |
| definition-008 | definition | 2/2 | 1 | ok | 3/4 | 1/1 | 3/3 | 0 | - | skipped |  |
| reasoning-001 | reasoning | 2/2 | 1 | ok | 2/4 | 1/1 | 3/3 | 0 | - | skipped |  |
| reasoning-002 | reasoning | 1/2 | 1 | ok | 1/4 | 1/1 | 3/3 | 0 | - | skipped |  |
| reasoning-003 | reasoning | 0/2 | - | ok | 2/4 | 1/1 | 3/3 | 0 | - | skipped |  |
| reasoning-004 | reasoning | 2/2 | 1 | ok | 3/4 | 2/2 | 3/3 | 0 | - | skipped |  |
| reasoning-005 | reasoning | 0/1 | - | ok | 3/3 | 1/1 | 3/3 | 0 | - | skipped |  |
| reasoning-006 | reasoning | 0/2 | - | ok | 0/3 | 0/1 | - | 0 | - | skipped |  |
| multi-hop-001 | multi_hop | 2/2 | 1 | ok | 7/7 | 1/1 | 3/3 | 0 | - | skipped |  |
| multi-hop-002 | multi_hop | 1/2 | 1 | ok | 2/5 | 1/1 | 3/3 | 0 | - | skipped |  |
| multi-hop-003 | multi_hop | 1/2 | 1 | ok | 2/5 | 1/2 | 3/3 | 0 | - | skipped |  |
| no-answer-001 | no_answer | - | - | ok | - | - | - | 0 | yes | skipped |  |
| no-answer-002 | no_answer | - | - | ok | - | - | - | 0 | yes | skipped |  |
| no-answer-003 | no_answer | - | - | ok | - | - | - | 0 | yes | skipped |  |

## Field Coverage

- `debug/retrieve` is used for Recall@3, Recall@5, MRR, and Top1 source accuracy.
- `ask` is used for answer keyword hit rate, citation hit rate, and no-answer accuracy.
- `LLM judge` is optional and only runs when `--judge-mode llm` is explicitly enabled with judge credentials.
- When `--skip-ask` is enabled, generation/citation/no-answer metrics are marked as skipped instead of being counted as zero.
- When ask errors occur, generation/citation/no-answer metrics are calculated only on successful ask samples and the report status becomes PARTIAL.
- When judge is disabled or unavailable, faithfulness/relevance metrics are marked as skipped or partial; objective citation/no-answer metrics remain reportable.
- `queryVariants`, `rank`, `score`, `source`, `documentId`, `chunkId`, `contentPreview`, and `metadata` are expected in debug output.

## Current Limitations

- `contentPreview` is a preview, not the full chunk, so long expected snippets may undercount recall.
- Citation source hit rate checks expected source names in returned citations.
- Citation snippet hit rate verifies each returned citation against the `contexts` returned by `/api/qa/ask` using exact match or token overlap.
- Answer keyword scoring is lexical; optional LLM judge metrics are reported separately when explicitly enabled.
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
- answer: @RestController 等价于 @Controller 和 @ResponseBody 这两个注解的组合。
- expected_keywords: `["@Controller", "@ResponseBody", "@RestController"]`
- keyword_hit: `3/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "0",
    "contentPreview": "核心注解 2.1 启动类注解 @SpringBootApplication public class Application { public static void main(String[] args) { SpringApplication.run(Application.class, args); } } @SpringBootApplication 是组合注解，包含： @Configuration ：标记为配置类 @EnableAutoConfiguration ：启用自动配置 @ComponentScan ：组件扫描 2.2 常用注解 @RestController ：= @Con...",
    "metadata": {
      "chunkIndex": 1.0,
      "tokenCount": 420.0,
      "headingPath": "",
      "sourceFileName": "springboot-basics.md",
      "documentId": 35.0,
      "kbId": 11.0,
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
    "source": "dc28958f-0290-4f35-b857-3714067493df",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "dc28958f-0290-4f35-b857-3714067493df",
    "contentPreview": "核心注解 2.1 启动类注解 @SpringBootApplication public class Application { public static void main(String[] args) { SpringApplication.run(Application.class, args); } } @SpringBootApplication 是组合注解，包含： @Configuration ：标记为配置类 @EnableAutoConfiguration ：启用自动配置 @ComponentScan ：组件扫描 2.2 常用注解 @RestController ：= @Con...",
    "metadata": {
      "kbId": 11,
      "fileName": "springboot-basics.md",
      "chunkIndex": 1,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "springboot-basics.md",
      "documentId": 35,
      "endIndex": 659,
      "startIndex": 239,
      "title": "springboot-basics.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "cb5551dd-5702-48d4-8669-fbc9bc0a4577",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "cb5551dd-5702-48d4-8669-fbc9bc0a4577",
    "contentPreview": "ationPolicy(SessionCreationPolicy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max...",
    "metadata": {
      "kbId": 11,
      "fileName": "springboot-basics.md",
      "chunkIndex": 13,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "springboot-basics.md",
      "documentId": 35,
      "endIndex": 3440,
      "startIndex": 3068,
      "title": "springboot-basics.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "476434c8-561b-489c-8296-1aad6d714562",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "476434c8-561b-489c-8296-1aad6d714562",
    "contentPreview": "1.1 @Scheduled 注解 @Component public class ScheduledTasks { // 每 5 秒执行一次 @Scheduled(fixedRate = 5000) public void reportCurrentTime() { log.info(\"当前时间：{}\", LocalDateTime.now()); }",
    "metadata": {
      "kbId": 11,
      "fileName": "springboot-basics.md",
      "chunkIndex": 20,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "springboot-basics.md",
      "documentId": 35,
      "endIndex": 4958,
      "startIndex": 4759,
      "title": "springboot-basics.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "3152a551-d10b-4aaa-8717-3e79c89987e4",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "3152a551-d10b-4aaa-8717-3e79c89987e4",
    "contentPreview": "rn executor; } } @Service public class EmailService { @Async public CompletableFuture<String> sendEmail(String to, String subject) { // 异步发送邮件 return CompletableFuture.completedFuture(\"SUCCESS\"); } } 11. 定时任务 11.1 @Scheduled 注解 @Component public class ScheduledTasks {",
    "metadata": {
      "kbId": 11,
      "fileName": "springboot-basics.md",
      "chunkIndex": 19,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "springboot-basics.md",
      "documentId": 35,
      "endIndex": 4817,
      "startIndex": 4519,
      "title": "springboot-basics.md",
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
[
  {
    "source": "0",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "0",
    "score": 0.016393441706895828,
    "snippet": "2 常用注解 @RestController ：= @Controller + @ResponseBody @RequestMapping ：映射 HTTP 请求 @GetMapping 、 @PostMapping ：简化的请求映射 @PathVariable ："
  },
  {
    "source": "dc28958f-0290-4f35-b857-3714067493df",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "dc28958f-0290-4f35-b857-3714067493df",
    "score": 0.016393441706895828,
    "snippet": "2 常用注解 @RestController ：= @Controller + @ResponseBody @RequestMapping ：映射 HTTP 请求 @GetMapping 、 @PostMapping ：简化的请求映射 @PathVariable ："
  },
  {
    "source": "cb5551dd-5702-48d4-8669-fbc9bc0a4577",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "cb5551dd-5702-48d4-8669-fbc9bc0a4577",
    "score": 0.016129031777381897,
    "snippet": "2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8."
  }
]
```
- citation validation metadata:
```json
{
  "validCitations": 3,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
- answer: 在 JWT 认证中，accessToken 是短期令牌，有效期为 1 小时，用于访问接口时携带；refreshToken 是长期令牌，有效期为 7 天，用于在 accessToken 过期时刷新获取新的 accessToken。
- expected_keywords: `["accessToken", "refreshToken", "访问接口", "刷新"]`
- keyword_hit: `4/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "0",
    "contentPreview": "ationPolicy(SessionCreationPolicy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max...",
    "metadata": {
      "chunkIndex": 13.0,
      "tokenCount": 372.0,
      "headingPath": "",
      "sourceFileName": "springboot-basics.md",
      "documentId": 35.0,
      "kbId": 11.0,
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
    "source": "cb5551dd-5702-48d4-8669-fbc9bc0a4577",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "cb5551dd-5702-48d4-8669-fbc9bc0a4577",
    "contentPreview": "ationPolicy(SessionCreationPolicy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max...",
    "metadata": {
      "kbId": 11,
      "fileName": "springboot-basics.md",
      "chunkIndex": 13,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "springboot-basics.md",
      "documentId": 35,
      "endIndex": 3440,
      "startIndex": 3068,
      "title": "springboot-basics.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.01515151560306549,
    "source": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 5,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1504,
      "startIndex": 1121,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015151515151515152
    }
  },
  {
    "rank": 4,
    "score": 0.016129031777381897,
    "source": "37bb6c85-b083-42a4-b849-890dbf989082",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "37bb6c85-b083-42a4-b849-890dbf989082",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 12,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2959,
      "startIndex": 2640,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 5,
    "score": 0.01587301678955555,
    "source": "901d738b-2d63-4b11-9c91-d9aa39fb8860",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "901d738b-2d63-4b11-9c91-d9aa39fb8860",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 0,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 270,
      "startIndex": 0,
      "title": "rag-technology-guide.md",
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
[
  {
    "source": "0",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "0",
    "score": 0.016393441706895828,
    "snippet": "2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8."
  },
  {
    "source": "cb5551dd-5702-48d4-8669-fbc9bc0a4577",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "cb5551dd-5702-48d4-8669-fbc9bc0a4577",
    "score": 0.016393441706895828,
    "snippet": "2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8."
  },
  {
    "source": "37bb6c85-b083-42a4-b849-890dbf989082",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "37bb6c85-b083-42a4-b849-890dbf989082",
    "score": 0.016129031777381897,
    "snippet": "面试常见问题 Q1: 为什么需要向量化？"
  }
]
```
- citation validation metadata:
```json
{
  "validCitations": 3,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
- answer: Redis 常见数据类型包括： - String：用于缓存、计数器和分布式锁 - Hash：用于对象存储，例如用户信息 - List：用于消息队列和列表分页 - Set：用于去重以及交集、并集、差集操作 - ZSet：用于排行榜和延迟队列
- expected_keywords: `["String", "Hash", "List", "Set", "ZSet"]`
- keyword_hit: `5/5`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "contentPreview": "） 锁机制 ： 行锁：锁定某一行 表锁：锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好...",
    "metadata": {
      "chunkIndex": 7.0,
      "tokenCount": 303.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 36.0,
      "kbId": 11.0,
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
    "source": "41240cb9-807f-4839-907a-c4385036f132",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "41240cb9-807f-4839-907a-c4385036f132",
    "contentPreview": "） 锁机制 ： 行锁：锁定某一行 表锁：锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 7,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 2187,
      "startIndex": 1884,
      "title": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "b72b7089-fdb9-4b82-8740-d6b5028717ca",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "b72b7089-fdb9-4b82-8740-d6b5028717ca",
    "contentPreview": "GC 收集器 ： Serial、Parallel、CMS、G1、ZGC --- 面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 3161,
      "startIndex": 2764,
      "title": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "e2b974e9-c87e-45b3-879b-ffb23565e898",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "e2b974e9-c87e-45b3-879b-ffb23565e898",
    "contentPreview": "(\"当前时间：{}\", LocalDateTime.now()); } // 每天凌晨 2 点执行 @Scheduled(cron = \"0 0 2 * * ?\") public void cleanupExpiredData() { log.info(\"清理过期数据\"); } } 12. 缓存 12.1 Spring Cache @Configuration @EnableCaching public class CacheConfig { // 配置 Redis 缓存 } @Service public class UserService { @Cacheable(value = \"use...",
    "metadata": {
      "kbId": 11,
      "fileName": "springboot-basics.md",
      "chunkIndex": 21,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "springboot-basics.md",
      "documentId": 35,
      "endIndex": 5336,
      "startIndex": 4919,
      "title": "springboot-basics.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "0569c9c9-aa04-49fc-add5-f3913b7a5717",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0569c9c9-aa04-49fc-add5-f3913b7a5717",
    "contentPreview": ". MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNCOMMITTED（读未提交） READ COMMITTED（读已提交） REPEATABLE READ（可重复读，MySQL ...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 6,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 1921,
      "startIndex": 1564,
      "title": "java-interview-guide.md",
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
[
  {
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "score": 0.016393441706895828,
    "snippet": "1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6."
  },
  {
    "source": "41240cb9-807f-4839-907a-c4385036f132",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "41240cb9-807f-4839-907a-c4385036f132",
    "score": 0.016393441706895828,
    "snippet": "1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6."
  },
  {
    "source": "b72b7089-fdb9-4b82-8740-d6b5028717ca",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "b72b7089-fdb9-4b82-8740-d6b5028717ca",
    "score": 0.016129031777381897,
    "snippet": "使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4."
  }
]
```
- citation validation metadata:
```json
{
  "validCitations": 3,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
- answer: 根据提供的文档，RAG 中列出的常用向量数据库包括： - **Milvus**：开源，性能强，本项目使用。
- **Pinecone**：托管服务。
- **Weaviate**：支持混合搜索。
- **Qdrant**：基于 Rust 实现。
- expected_keywords: `["Milvus", "Pinecone", "Weaviate", "Qdrant"]`
- keyword_hit: `4/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "77017a1a-4e1b-4092-bc23-8433fb9447f4",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "77017a1a-4e1b-4092-bc23-8433fb9447f4",
    "contentPreview": "提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召回相关文档 → 构建提示词 → LLM 生成答案 → 返回结果 2.1 详细步骤 Step 1: 文档处理（离线） 文档上传 ：PDF、Markdown、Word、代码文件等 文档解析 ：提取文本内容 文本分块 ：将长文档切分为小块（Chunk），通常 500-2000 字符 向量化 ：使用 Embedding 模型将文本转换为向量 存储 ：向量存入向量数据库（Milvus、Pinecone 等） Step 2: 问答处理（在线） 问题向量化 ：用户问题转换为向量 相似度检索 ...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 1,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 631,
      "startIndex": 241,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 2,
    "score": 0.015625,
    "source": "926499a3-c300-4daf-a598-e80534fee04c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "926499a3-c300-4daf-a598-e80534fee04c",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2333,
      "startIndex": 2000,
      "title": "rag-technology-guide.md",
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
    "documentId": 37,
    "chunkId": "0",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "chunkIndex": 4.0,
      "tokenCount": 232.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 37.0,
      "kbId": 11.0,
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
    "source": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 4,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1193,
      "startIndex": 961,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 5,
    "score": 0.01587301678955555,
    "source": "d770ae16-5ca7-4ad8-b9f9-c42a8a31d225",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "d770ae16-5ca7-4ad8-b9f9-c42a8a31d225",
    "contentPreview": "Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.52, ..., 0.91] (相似度高) \"今天天气很好\" → [-0.78,...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 3,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1022,
      "startIndex": 641,
      "title": "rag-technology-guide.md",
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
[
  {
    "source": "77017a1a-4e1b-4092-bc23-8433fb9447f4",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "77017a1a-4e1b-4092-bc23-8433fb9447f4",
    "score": 0.016393441706895828,
    "snippet": "1 详细步骤 Step 1: 文档处理（离线） 文档上传 ：PDF、Markdown、Word、代码文件等 文档解析 ：提取文本内容 文本分块 ：将长文档切分为小块（Chunk），通常 500-2000 字符 向量化 ：使用 Embedding 模型将文本转换为向量 存储 ：向量存入向量数据库（Milvus、Pinecone 等） Step 2: 问答处理（"
  },
  {
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "0",
    "score": 0.016393441706895828,
    "snippet": "常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3."
  },
  {
    "source": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "score": 0.016129031777381897,
    "snippet": "常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3."
  }
]
```
- citation validation metadata:
```json
{
  "validCitations": 3,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
- keyword_hit: `0/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "contentPreview": ". MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNCOMMITTED（读未提交） READ COMMITTED（读已提交） REPEATABLE READ（可重复读，MySQL ...",
    "metadata": {
      "chunkIndex": 6.0,
      "tokenCount": 357.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 36.0,
      "kbId": 11.0,
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
    "source": "0569c9c9-aa04-49fc-add5-f3913b7a5717",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0569c9c9-aa04-49fc-add5-f3913b7a5717",
    "contentPreview": ". MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNCOMMITTED（读未提交） READ COMMITTED（读已提交） REPEATABLE READ（可重复读，MySQL ...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 6,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 1921,
      "startIndex": 1564,
      "title": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 4,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1193,
      "startIndex": 961,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.015625,
    "source": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 5,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1504,
      "startIndex": 1121,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 5,
    "score": 0.01515151560306549,
    "source": "b72b7089-fdb9-4b82-8740-d6b5028717ca",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "b72b7089-fdb9-4b82-8740-d6b5028717ca",
    "contentPreview": "GC 收集器 ： Serial、Parallel、CMS、G1、ZGC --- 面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 3161,
      "startIndex": 2764,
      "title": "java-interview-guide.md",
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
  "validCitations": 0,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
- answer: RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了信息检索和文本生成的 AI 技术架构。其核心思想是解决传统大语言模型（LLM）存在的知识截止、幻觉问题以及缺乏专业知识等不足。具体做法是：先从知识库中检索相关信息，将检索到的信息作为上下文提供给 LLM，然后由 LLM 基于该上下文生成准确的答案。
- expected_keywords: `["Retrieval-Augmented Generation", "检索增强生成", "信息检索", "文本生成"]`
- keyword_hit: `4/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "0",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "chunkIndex": 0.0,
      "tokenCount": 269.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 37.0,
      "kbId": 11.0,
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
    "source": "901d738b-2d63-4b11-9c91-d9aa39fb8860",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "901d738b-2d63-4b11-9c91-d9aa39fb8860",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 0,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 270,
      "startIndex": 0,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 5,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1504,
      "startIndex": 1121,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "926499a3-c300-4daf-a598-e80534fee04c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "926499a3-c300-4daf-a598-e80534fee04c",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2333,
      "startIndex": 2000,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "2fc312fe-bd9e-45a6-bfdf-29d93b357ea5",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "2fc312fe-bd9e-45a6-bfdf-29d93b357ea5",
    "contentPreview": "：总结 4. 提示词工程（Prompt Engineering） RAG 系统需要精心设计提示词，将检索到的上下文和问题结合。 4.1 典型 Prompt 模板 你是一个专业的技术助手。请根据以下文档内容回答用户的问题。",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 6,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1550,
      "startIndex": 1441,
      "title": "rag-technology-guide.md",
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
[
  {
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "0",
    "score": 0.016393441706895828,
    "snippet": "RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构"
  },
  {
    "source": "901d738b-2d63-4b11-9c91-d9aa39fb8860",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "901d738b-2d63-4b11-9c91-d9aa39fb8860",
    "score": 0.016393441706895828,
    "snippet": "RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构"
  }
]
```
- citation validation metadata:
```json
{
  "validCitations": 2,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
- answer: 缓存穿透是指查询不存在的数据，导致缓存和数据库中都没有该数据的情况。
- expected_keywords: `["缓存穿透", "不存在的数据", "布隆过滤器", "缓存空值"]`
- keyword_hit: `2/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "contentPreview": "） 锁机制 ： 行锁：锁定某一行 表锁：锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好...",
    "metadata": {
      "chunkIndex": 7.0,
      "tokenCount": 303.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 36.0,
      "kbId": 11.0,
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
    "source": "41240cb9-807f-4839-907a-c4385036f132",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "41240cb9-807f-4839-907a-c4385036f132",
    "contentPreview": "） 锁机制 ： 行锁：锁定某一行 表锁：锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 7,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 2187,
      "startIndex": 1884,
      "title": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.01587301678955555,
    "source": "e2b974e9-c87e-45b3-879b-ffb23565e898",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "e2b974e9-c87e-45b3-879b-ffb23565e898",
    "contentPreview": "(\"当前时间：{}\", LocalDateTime.now()); } // 每天凌晨 2 点执行 @Scheduled(cron = \"0 0 2 * * ?\") public void cleanupExpiredData() { log.info(\"清理过期数据\"); } } 12. 缓存 12.1 Spring Cache @Configuration @EnableCaching public class CacheConfig { // 配置 Redis 缓存 } @Service public class UserService { @Cacheable(value = \"use...",
    "metadata": {
      "kbId": 11,
      "fileName": "springboot-basics.md",
      "chunkIndex": 21,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "springboot-basics.md",
      "documentId": 35,
      "endIndex": 5336,
      "startIndex": 4919,
      "title": "springboot-basics.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 4,
    "score": 0.015625,
    "source": "fceb5000-22c6-4337-bf1f-2d83c5e7ff33",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "fceb5000-22c6-4337-bf1f-2d83c5e7ff33",
    "contentPreview": "数据库，之后从缓存读取 } @CacheEvict(value = \"users\", key = \"#id\") public void deleteById(Long id) { // 删除缓存 } @CachePut(value = \"users\", key = \"#user.id\") public User update(User user) { // 更新缓存 } } 13. 单元测试 13.1 测试示例 @SpringBootTest @AutoConfigureMockMvc class UserControllerTest { @Autowired private MockMvc ...",
    "metadata": {
      "kbId": 11,
      "fileName": "springboot-basics.md",
      "chunkIndex": 22,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "springboot-basics.md",
      "documentId": 35,
      "endIndex": 5718,
      "startIndex": 5319,
      "title": "springboot-basics.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 5,
    "score": 0.015384615398943424,
    "source": "4c61d28b-7f57-4679-bcc4-3d9aca5dbe61",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "4c61d28b-7f57-4679-bcc4-3d9aca5dbe61",
    "contentPreview": "l 通过 spring.profiles.active 切换 4. MyBatis / MyBatis-Plus 4.1 MyBatis 核心 #{} vs ${} ： #{} 预编译，防止 SQL 注入 ${} 字符串替换，存在注入风险 一级缓存 vs 二级缓存 ： 一级缓存：SqlSession 级别，默认开启 二级缓存：Mapper 级别，需要手动开启 4.2 MyBatis-Plus 优势 ： 内置通用 CRUD 方法 代码生成器 分页插件 乐观锁、逻辑删除支持 5.",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 5,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 1565,
      "startIndex": 1324,
      "title": "java-interview-guide.md",
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
[
  {
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "score": 0.016393441706895828,
    "snippet": "2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6."
  },
  {
    "source": "41240cb9-807f-4839-907a-c4385036f132",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "41240cb9-807f-4839-907a-c4385036f132",
    "score": 0.016393441706895828,
    "snippet": "2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6."
  },
  {
    "source": "e2b974e9-c87e-45b3-879b-ffb23565e898",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "e2b974e9-c87e-45b3-879b-ffb23565e898",
    "score": 0.01587301678955555,
    "snippet": "// 第一次查询数据库，之后从缓存读取"
  }
]
```
- citation validation metadata:
```json
{
  "validCitations": 3,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
- answer: RAG（检索增强生成）在知识更新和可解释性方面通常比微调更适合企业知识库，原因如下： 1. **知识更新**：RAG 只需实时更新文档即可反映最新知识，无需重新训练模型；而微调则需要重新训练，成本高且耗时。
2. **可解释性**：RAG 的回答可以追溯到具体的文档来源，具有更高的可解释性；而微调后的模型生成内容往往难以追溯其依据，可解释性较低。 因此，对于需要频繁更新知识且要求回答可追溯的企业知识库场景，RAG 是更优选择。
- expected_keywords: `["实时更新文档", "成本低", "可追溯来源", "可解释性高"]`
- keyword_hit: `1/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "0",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "chunkIndex": 10.0,
      "tokenCount": 333.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 37.0,
      "kbId": 11.0,
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
    "source": "926499a3-c300-4daf-a598-e80534fee04c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "926499a3-c300-4daf-a598-e80534fee04c",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2333,
      "startIndex": 2000,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "901d738b-2d63-4b11-9c91-d9aa39fb8860",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "901d738b-2d63-4b11-9c91-d9aa39fb8860",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 0,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 270,
      "startIndex": 0,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.015384615398943424,
    "source": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 5,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1504,
      "startIndex": 1121,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015384615384615385
    }
  },
  {
    "rank": 5,
    "score": 0.01587301678955555,
    "source": "8b5df374-b365-41d3-8322-579cc48c0757",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "8b5df374-b365-41d3-8322-579cc48c0757",
    "contentPreview": "3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文档、技术博客 优势 ：快速查找代码示例、技术方案 7.4 法律文书检索 场景 ：...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 11,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2648,
      "startIndex": 2320,
      "title": "rag-technology-guide.md",
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
[
  {
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "0",
    "score": 0.016393441706895828,
    "snippet": "| 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。"
  },
  {
    "source": "926499a3-c300-4daf-a598-e80534fee04c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "926499a3-c300-4daf-a598-e80534fee04c",
    "score": 0.016393441706895828,
    "snippet": "| 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。"
  },
  {
    "source": "901d738b-2d63-4b11-9c91-d9aa39fb8860",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "901d738b-2d63-4b11-9c91-d9aa39fb8860",
    "score": 0.016129031777381897,
    "snippet": "1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2."
  }
]
```
- citation validation metadata:
```json
{
  "validCitations": 3,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
- answer: 根据提供的上下文，RAG 检索后使用 Rerank（重排序）是为了**二次精排**，从而提高 RAG 系统的准确率。 在提升 RAG 准确率的最佳实践中，除了优化分块策略、提升 Embedding 质量和增加 Top-K（检索更多候选）外，引入 Rerank 步骤可以对初步检索到的候选结果进行更精细的排序，确保最相关的信息被优先提供给大语言模型（LLM）作为上下文，从而生成更准确的答案。
- expected_keywords: `["重排序", "二次精排", "提高准确率", "检索优化"]`
- keyword_hit: `2/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016129031777381897,
    "source": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 5,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1504,
      "startIndex": 1121,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 2,
    "score": 0.01587301678955555,
    "source": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 4,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1193,
      "startIndex": 961,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 3,
    "score": 0.015384615398943424,
    "source": "37bb6c85-b083-42a4-b849-890dbf989082",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "37bb6c85-b083-42a4-b849-890dbf989082",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 12,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2959,
      "startIndex": 2640,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015384615384615385
    }
  },
  {
    "rank": 4,
    "score": 0.016393441706895828,
    "source": "926499a3-c300-4daf-a598-e80534fee04c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "926499a3-c300-4daf-a598-e80534fee04c",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2333,
      "startIndex": 2000,
      "title": "rag-technology-guide.md",
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
    "documentId": 37,
    "chunkId": "0",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "chunkIndex": 0.0,
      "tokenCount": 269.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 37.0,
      "kbId": 11.0,
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
[
  {
    "source": "926499a3-c300-4daf-a598-e80534fee04c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "926499a3-c300-4daf-a598-e80534fee04c",
    "score": 0.016393441706895828,
    "snippet": "优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？"
  },
  {
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "0",
    "score": 0.016393441706895828,
    "snippet": "1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2."
  },
  {
    "source": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "score": 0.016129031777381897,
    "snippet": "提示词工程（Prompt Engineering） RAG 系统需要精心设计提示词，将检索到的上下文和问题结合。"
  }
]
```
- citation validation metadata:
```json
{
  "validCitations": 3,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
- answer: 在多线程环境下不应该直接使用 HashMap，因为 HashMap 本身是线程不安全的。在并发场景下，直接使用 HashMap 可能导致数据不一致、覆盖或死循环等问题。根据提供的上下文，多线程环境下应使用线程安全的替代方案，例如 ConcurrentHashMap。
- expected_keywords: `["线程不安全", "ConcurrentHashMap", "多线程"]`
- keyword_hit: `3/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "chunkIndex": 1.0,
      "tokenCount": 420.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 36.0,
      "kbId": 11.0,
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
    "source": "bba6e34a-4290-4aba-ab90-676e863fd5e8",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "bba6e34a-4290-4aba-ab90-676e863fd5e8",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 1,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 438,
      "startIndex": 18,
      "title": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 4,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1193,
      "startIndex": 961,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 5,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1504,
      "startIndex": 1121,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015384615398943424,
    "source": "37bb6c85-b083-42a4-b849-890dbf989082",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "37bb6c85-b083-42a4-b849-890dbf989082",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 12,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2959,
      "startIndex": 2640,
      "title": "rag-technology-guide.md",
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
[
  {
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "score": 0.016393441706895828,
    "snippet": "75，容量翻倍 线程不安全，多线程使用 ConcurrentHashMap 1."
  },
  {
    "source": "bba6e34a-4290-4aba-ab90-676e863fd5e8",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "bba6e34a-4290-4aba-ab90-676e863fd5e8",
    "score": 0.016393441706895828,
    "snippet": "75，容量翻倍 线程不安全，多线程使用 ConcurrentHashMap 1."
  },
  {
    "source": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "score": 0.016129031777381897,
    "snippet": "常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3."
  }
]
```
- citation validation metadata:
```json
{
  "validCitations": 3,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
- answer: 知识库中没有足够信息回答该问题。
- expected_keywords: `["预编译", "SQL 注入", "字符串替换"]`
- keyword_hit: `0/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 4,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1193,
      "startIndex": 961,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 2,
    "score": 0.01587301678955555,
    "source": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 5,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1504,
      "startIndex": 1121,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 3,
    "score": 0.015625,
    "source": "37bb6c85-b083-42a4-b849-890dbf989082",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "37bb6c85-b083-42a4-b849-890dbf989082",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 12,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2959,
      "startIndex": 2640,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 4,
    "score": 0.01515151560306549,
    "source": "926499a3-c300-4daf-a598-e80534fee04c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "926499a3-c300-4daf-a598-e80534fee04c",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2333,
      "startIndex": 2000,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015151515151515152
    }
  },
  {
    "rank": 5,
    "score": 0.014492753893136978,
    "source": "996590f5-757b-4c51-a02c-1d1f91faed6e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "996590f5-757b-4c51-a02c-1d1f91faed6e",
    "contentPreview": "【相关文档】 {retrieved_context} 【用户提问】 {user_question} 【回答要求】 1. 基于文档内容回答，不要编造信息 2. 如果文档中没有相关信息，明确说明 3. 引用具体的文档片段作为依据 4. 回答要简洁清晰",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 7,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1679,
      "startIndex": 1550,
      "title": "rag-technology-guide.md",
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
  "validCitations": 0,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
- answer: 要提高 RAG 准确率，可以从以下三个方面进行优化： 1. **检索优化**： - 采用混合检索，结合向量检索与关键词检索（如 BM25）； - 使用重排序（Rerank）技术进行二次精排，提升检索结果的相关性； - 通过 Query 改写扩展或改写用户问题，提高召回率； - 增加 Top-K 值，检索更多候选文档。 2. **分块策略优化**： - 合理设置 Chunk 大小，推荐在 500–1000 字符之间，根据文档类型调整； - 技术文档可按段落或章节分块，代码文档可按函数或类分块； - 避免 Chunk 过小（<200 字符）导致语义不完整，或过大（>2000 字符）导致检索不精准和 Token 消耗过多； - 可结合固定长度、句子/段落、滑动窗口或语义分块等方法。 3. **Prompt 优化**： - 明确回答要求，定义 AI 的角色（如技术助手、客服等）； - 提供约束条件，如禁止编造、要求引用来源； - 使用 Few-Shot 提供示例问答； - 引入 Chain of Thought，要求模型逐步推理。 此外，提升 Embedding 质量（选择合适的模型）也是提高准确率的重要环节。
- expected_keywords: `["优化分块策略", "提升 Embedding 质量", "增加 Top-K", "使用 Rerank", "优化 Prompt"]`
- keyword_hit: `2/5`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "0",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "chunkIndex": 10.0,
      "tokenCount": 333.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 37.0,
      "kbId": 11.0,
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
    "source": "926499a3-c300-4daf-a598-e80534fee04c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "926499a3-c300-4daf-a598-e80534fee04c",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2333,
      "startIndex": 2000,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "64ea28ee-c330-4d25-ac22-39c926d02747",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "64ea28ee-c330-4d25-ac22-39c926d02747",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 9,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2021,
      "startIndex": 1679,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "d76c3fc0-ef54-4e0e-84b3-ef38b25607f1",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "d76c3fc0-ef54-4e0e-84b3-ef38b25607f1",
    "contentPreview": "文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈 关键指标 ： 检索召回率：Top-K 包含正确答案的比例 答案准确率：生成答案的正确性 用户满意度：...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 13,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 3227,
      "startIndex": 2880,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 5,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1504,
      "startIndex": 1121,
      "title": "rag-technology-guide.md",
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
[
  {
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "0",
    "score": 0.016393441706895828,
    "snippet": "优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？"
  },
  {
    "source": "926499a3-c300-4daf-a598-e80534fee04c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "926499a3-c300-4daf-a598-e80534fee04c",
    "score": 0.016393441706895828,
    "snippet": "优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？"
  },
  {
    "source": "64ea28ee-c330-4d25-ac22-39c926d02747",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "64ea28ee-c330-4d25-ac22-39c926d02747",
    "score": 0.016129031777381897,
    "snippet": "2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5."
  }
]
```
- citation validation metadata:
```json
{
  "validCitations": 3,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
- answer: 根据提供的上下文，在接口性能优化手段中，关于使用缓存（Redis）的内容仅出现在《java-interview-guide.md》（Java 面试文档）中，具体手段为“使用缓存（Redis）”。 提供的上下文中**没有包含**名为"Spring Boot 文档”的独立内容，且《java-interview-guide.md》中关于接口性能优化的部分并未区分或列出专门针对"Spring Boot 文档”的额外缓存手段，仅统一列出了“使用缓存（Redis）”这一项。 因此，基于现有上下文：
1. **Java 面试文档**（java-interview-guide.md）：明确列出的手段是“使用缓存（Redis）”。
2. **Spring Boot 文档**：上下文中未提供独立的 Spring Boot 文档内容，也未列出区别于 Java 面试文档的特定缓存手段。
- expected_keywords: `["Redis", "使用缓存", "@Cacheable", "@CacheEvict", "@CachePut"]`
- keyword_hit: `2/5`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "contentPreview": "GC 收集器 ： Serial、Parallel、CMS、G1、ZGC --- 面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID...",
    "metadata": {
      "chunkIndex": 10.0,
      "tokenCount": 398.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 36.0,
      "kbId": 11.0,
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
    "source": "b72b7089-fdb9-4b82-8740-d6b5028717ca",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "b72b7089-fdb9-4b82-8740-d6b5028717ca",
    "contentPreview": "GC 收集器 ： Serial、Parallel、CMS、G1、ZGC --- 面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 3161,
      "startIndex": 2764,
      "title": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "37bb6c85-b083-42a4-b849-890dbf989082",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "37bb6c85-b083-42a4-b849-890dbf989082",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 12,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2959,
      "startIndex": 2640,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "64ea28ee-c330-4d25-ac22-39c926d02747",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "64ea28ee-c330-4d25-ac22-39c926d02747",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 9,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2021,
      "startIndex": 1679,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.014705882407724857,
    "source": "2f315107-6b6f-4291-82f9-06bfe39d7dae",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "2f315107-6b6f-4291-82f9-06bfe39d7dae",
    "contentPreview": "信 流量削峰：秒杀系统 系统解耦：订单服务和库存服务 消息可靠性 ： 生产者确认机制 消息持久化 消费者手动 ACK 9. 设计模式 9.1 常用设计模式 单例模式 ：Spring Bean 默认单例 工厂模式 ：BeanFactory 代理模式 ：AOP 模板方法模式 ：JdbcTemplate、RestTemplate 观察者模式 ：Spring 事件监听 策略模式 ：支付方式选择 10. JVM 10.1 内存模型 堆 ：对象实例，GC 主要区域 栈 ：方法调用，局部变量 方法区 ：类信息、常量池 程序计数器 ：当前线程执行的字节码行号 10.2 垃圾回收 GC 算法 ： 标记-清除 复...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 9,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 2831,
      "startIndex": 2444,
      "title": "java-interview-guide.md",
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
[
  {
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "score": 0.016393441706895828,
    "snippet": "接口性能优化手段？"
  },
  {
    "source": "b72b7089-fdb9-4b82-8740-d6b5028717ca",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "b72b7089-fdb9-4b82-8740-d6b5028717ca",
    "score": 0.016393441706895828,
    "snippet": "接口性能优化手段？"
  },
  {
    "source": "37bb6c85-b083-42a4-b849-890dbf989082",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "37bb6c85-b083-42a4-b849-890dbf989082",
    "score": 0.016129031777381897,
    "snippet": "\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？"
  }
]
```
- citation validation metadata:
```json
{
  "validCitations": 3,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
  "unsupportedCitationCount": 0
}
```

## Failed Citation Cases

### fact-002 (fact)

- id: `fact-002`
- type: `fact`
- question: @SpringBootApplication 由哪些注解组合而成？
- expected_sources: `["springboot-basics.md", "java-interview-guide.md"]`
- expected_contexts.contains: `["@SpringBootApplication 是组合注解", "@SpringBootApplication = @Configuration"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `1`
- answer: @SpringBootApplication 是一个组合注解，由以下三个注解组成： - @Configuration：标记为配置类 - @EnableAutoConfiguration：启用自动配置 - @ComponentScan：组件扫描
- expected_keywords: `["@Configuration", "@EnableAutoConfiguration", "@ComponentScan"]`
- keyword_hit: `3/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "0",
    "contentPreview": "核心注解 2.1 启动类注解 @SpringBootApplication public class Application { public static void main(String[] args) { SpringApplication.run(Application.class, args); } } @SpringBootApplication 是组合注解，包含： @Configuration ：标记为配置类 @EnableAutoConfiguration ：启用自动配置 @ComponentScan ：组件扫描 2.2 常用注解 @RestController ：= @Con...",
    "metadata": {
      "chunkIndex": 1.0,
      "tokenCount": 420.0,
      "headingPath": "",
      "sourceFileName": "springboot-basics.md",
      "documentId": 35.0,
      "kbId": 11.0,
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
    "source": "dc28958f-0290-4f35-b857-3714067493df",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "dc28958f-0290-4f35-b857-3714067493df",
    "contentPreview": "核心注解 2.1 启动类注解 @SpringBootApplication public class Application { public static void main(String[] args) { SpringApplication.run(Application.class, args); } } @SpringBootApplication 是组合注解，包含： @Configuration ：标记为配置类 @EnableAutoConfiguration ：启用自动配置 @ComponentScan ：组件扫描 2.2 常用注解 @RestController ：= @Con...",
    "metadata": {
      "kbId": 11,
      "fileName": "springboot-basics.md",
      "chunkIndex": 1,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "springboot-basics.md",
      "documentId": 35,
      "endIndex": 659,
      "startIndex": 239,
      "title": "springboot-basics.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "0a027708-629e-408e-87ff-e2a96c1fccd8",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0a027708-629e-408e-87ff-e2a96c1fccd8",
    "contentPreview": "基于接口 CGLIB 代理：基于继承 3. Spring Boot 3.1 自动配置原理 @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan 条件注解 ： @ConditionalOnClass：类路径存在某个类 @ConditionalOnMissingBean：容器中不存在某个 Bean @ConditionalOnProperty：配置文件中存在某个属性 3.2 配置文件 application.yml vs application.properties ： yml 支持层...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 4,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 1357,
      "startIndex": 924,
      "title": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "476434c8-561b-489c-8296-1aad6d714562",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "476434c8-561b-489c-8296-1aad6d714562",
    "contentPreview": "1.1 @Scheduled 注解 @Component public class ScheduledTasks { // 每 5 秒执行一次 @Scheduled(fixedRate = 5000) public void reportCurrentTime() { log.info(\"当前时间：{}\", LocalDateTime.now()); }",
    "metadata": {
      "kbId": 11,
      "fileName": "springboot-basics.md",
      "chunkIndex": 20,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "springboot-basics.md",
      "documentId": 35,
      "endIndex": 4958,
      "startIndex": 4759,
      "title": "springboot-basics.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "3152a551-d10b-4aaa-8717-3e79c89987e4",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "3152a551-d10b-4aaa-8717-3e79c89987e4",
    "contentPreview": "rn executor; } } @Service public class EmailService { @Async public CompletableFuture<String> sendEmail(String to, String subject) { // 异步发送邮件 return CompletableFuture.completedFuture(\"SUCCESS\"); } } 11. 定时任务 11.1 @Scheduled 注解 @Component public class ScheduledTasks {",
    "metadata": {
      "kbId": 11,
      "fileName": "springboot-basics.md",
      "chunkIndex": 19,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "springboot-basics.md",
      "documentId": 35,
      "endIndex": 4817,
      "startIndex": 4519,
      "title": "springboot-basics.md",
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
[
  {
    "source": "0",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "0",
    "score": 0.016393441706895828,
    "snippet": "class, args);\n    }\n}\n@SpringBootApplication  是组合注解，包含： @Configuration ：标记为配置类 @EnableAutoConfiguration ：启用自动配置 @ComponentScan ：组件扫描 2"
  },
  {
    "source": "dc28958f-0290-4f35-b857-3714067493df",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "dc28958f-0290-4f35-b857-3714067493df",
    "score": 0.016393441706895828,
    "snippet": "class, args);\n    }\n}\n@SpringBootApplication  是组合注解，包含： @Configuration ：标记为配置类 @EnableAutoConfiguration ：启用自动配置 @ComponentScan ：组件扫描 2"
  }
]
```
- citation validation metadata:
```json
{
  "validCitations": 2,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
- keyword_hit: `0/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "contentPreview": ". MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNCOMMITTED（读未提交） READ COMMITTED（读已提交） REPEATABLE READ（可重复读，MySQL ...",
    "metadata": {
      "chunkIndex": 6.0,
      "tokenCount": 357.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 36.0,
      "kbId": 11.0,
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
    "source": "0569c9c9-aa04-49fc-add5-f3913b7a5717",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0569c9c9-aa04-49fc-add5-f3913b7a5717",
    "contentPreview": ". MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNCOMMITTED（读未提交） READ COMMITTED（读已提交） REPEATABLE READ（可重复读，MySQL ...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 6,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 1921,
      "startIndex": 1564,
      "title": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 4,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1193,
      "startIndex": 961,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.015625,
    "source": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 5,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1504,
      "startIndex": 1121,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 5,
    "score": 0.01515151560306549,
    "source": "b72b7089-fdb9-4b82-8740-d6b5028717ca",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "b72b7089-fdb9-4b82-8740-d6b5028717ca",
    "contentPreview": "GC 收集器 ： Serial、Parallel、CMS、G1、ZGC --- 面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 3161,
      "startIndex": 2764,
      "title": "java-interview-guide.md",
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
  "validCitations": 0,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
  "unsupportedCitationCount": 0
}
```

### definition-003 (definition)

- id: `definition-003`
- type: `definition`
- question: 什么是向量数据库？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["专门存储和检索向量的数据库", "支持高效的相似度搜索"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `2`
- answer:
- expected_keywords: `["存储", "检索向量", "相似度搜索"]`
- keyword_hit: `0/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "0",
    "contentPreview": "3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文档、技术博客 优势 ：快速查找代码示例、技术方案 7.4 法律文书检索 场景 ：...",
    "metadata": {
      "chunkIndex": 11.0,
      "tokenCount": 328.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 37.0,
      "kbId": 11.0,
      "startIndex": 2320.0,
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
    "source": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 4,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1193,
      "startIndex": 961,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "d770ae16-5ca7-4ad8-b9f9-c42a8a31d225",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "d770ae16-5ca7-4ad8-b9f9-c42a8a31d225",
    "contentPreview": "Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.52, ..., 0.91] (相似度高) \"今天天气很好\" → [-0.78,...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 3,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1022,
      "startIndex": 641,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "37bb6c85-b083-42a4-b849-890dbf989082",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "37bb6c85-b083-42a4-b849-890dbf989082",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 12,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2959,
      "startIndex": 2640,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "77017a1a-4e1b-4092-bc23-8433fb9447f4",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "77017a1a-4e1b-4092-bc23-8433fb9447f4",
    "contentPreview": "提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召回相关文档 → 构建提示词 → LLM 生成答案 → 返回结果 2.1 详细步骤 Step 1: 文档处理（离线） 文档上传 ：PDF、Markdown、Word、代码文件等 文档解析 ：提取文本内容 文本分块 ：将长文档切分为小块（Chunk），通常 500-2000 字符 向量化 ：使用 Embedding 模型将文本转换为向量 存储 ：向量存入向量数据库（Milvus、Pinecone 等） Step 2: 问答处理（在线） 问题向量化 ：用户问题转换为向量 相似度检索 ...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 1,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 631,
      "startIndex": 241,
      "title": "rag-technology-guide.md",
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
  "validCitations": 0,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
- answer: 知识库中没有足够信息回答该问题。
- expected_keywords: `["预编译", "SQL 注入", "字符串替换"]`
- keyword_hit: `0/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 4,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1193,
      "startIndex": 961,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 2,
    "score": 0.01587301678955555,
    "source": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 5,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1504,
      "startIndex": 1121,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 3,
    "score": 0.015625,
    "source": "37bb6c85-b083-42a4-b849-890dbf989082",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "37bb6c85-b083-42a4-b849-890dbf989082",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 12,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2959,
      "startIndex": 2640,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 4,
    "score": 0.01515151560306549,
    "source": "926499a3-c300-4daf-a598-e80534fee04c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "926499a3-c300-4daf-a598-e80534fee04c",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2333,
      "startIndex": 2000,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015151515151515152
    }
  },
  {
    "rank": 5,
    "score": 0.014492753893136978,
    "source": "996590f5-757b-4c51-a02c-1d1f91faed6e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "996590f5-757b-4c51-a02c-1d1f91faed6e",
    "contentPreview": "【相关文档】 {retrieved_context} 【用户提问】 {user_question} 【回答要求】 1. 基于文档内容回答，不要编造信息 2. 如果文档中没有相关信息，明确说明 3. 引用具体的文档片段作为依据 4. 回答要简洁清晰",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 7,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1679,
      "startIndex": 1550,
      "title": "rag-technology-guide.md",
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
  "validCitations": 0,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
- answer: 根据提供的上下文，在接口性能优化手段中，关于使用缓存（Redis）的内容仅出现在《java-interview-guide.md》（Java 面试文档）中，具体手段为“使用缓存（Redis）”。 提供的上下文中**没有包含**名为"Spring Boot 文档”的独立内容，且《java-interview-guide.md》中关于接口性能优化的部分并未区分或列出专门针对"Spring Boot 文档”的额外缓存手段，仅统一列出了“使用缓存（Redis）”这一项。 因此，基于现有上下文：
1. **Java 面试文档**（java-interview-guide.md）：明确列出的手段是“使用缓存（Redis）”。
2. **Spring Boot 文档**：上下文中未提供独立的 Spring Boot 文档内容，也未列出区别于 Java 面试文档的特定缓存手段。
- expected_keywords: `["Redis", "使用缓存", "@Cacheable", "@CacheEvict", "@CachePut"]`
- keyword_hit: `2/5`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "contentPreview": "GC 收集器 ： Serial、Parallel、CMS、G1、ZGC --- 面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID...",
    "metadata": {
      "chunkIndex": 10.0,
      "tokenCount": 398.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 36.0,
      "kbId": 11.0,
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
    "source": "b72b7089-fdb9-4b82-8740-d6b5028717ca",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "b72b7089-fdb9-4b82-8740-d6b5028717ca",
    "contentPreview": "GC 收集器 ： Serial、Parallel、CMS、G1、ZGC --- 面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 3161,
      "startIndex": 2764,
      "title": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "37bb6c85-b083-42a4-b849-890dbf989082",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "37bb6c85-b083-42a4-b849-890dbf989082",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 12,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2959,
      "startIndex": 2640,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "64ea28ee-c330-4d25-ac22-39c926d02747",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "64ea28ee-c330-4d25-ac22-39c926d02747",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 9,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2021,
      "startIndex": 1679,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.014705882407724857,
    "source": "2f315107-6b6f-4291-82f9-06bfe39d7dae",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "2f315107-6b6f-4291-82f9-06bfe39d7dae",
    "contentPreview": "信 流量削峰：秒杀系统 系统解耦：订单服务和库存服务 消息可靠性 ： 生产者确认机制 消息持久化 消费者手动 ACK 9. 设计模式 9.1 常用设计模式 单例模式 ：Spring Bean 默认单例 工厂模式 ：BeanFactory 代理模式 ：AOP 模板方法模式 ：JdbcTemplate、RestTemplate 观察者模式 ：Spring 事件监听 策略模式 ：支付方式选择 10. JVM 10.1 内存模型 堆 ：对象实例，GC 主要区域 栈 ：方法调用，局部变量 方法区 ：类信息、常量池 程序计数器 ：当前线程执行的字节码行号 10.2 垃圾回收 GC 算法 ： 标记-清除 复...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 9,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 2831,
      "startIndex": 2444,
      "title": "java-interview-guide.md",
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
[
  {
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "score": 0.016393441706895828,
    "snippet": "接口性能优化手段？"
  },
  {
    "source": "b72b7089-fdb9-4b82-8740-d6b5028717ca",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "b72b7089-fdb9-4b82-8740-d6b5028717ca",
    "score": 0.016393441706895828,
    "snippet": "接口性能优化手段？"
  },
  {
    "source": "37bb6c85-b083-42a4-b849-890dbf989082",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "37bb6c85-b083-42a4-b849-890dbf989082",
    "score": 0.016129031777381897,
    "snippet": "\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？"
  }
]
```
- citation validation metadata:
```json
{
  "validCitations": 3,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
  "unsupportedCitationCount": 0
}
```

## Low Answer Keyword Hit Cases

### fact-006 (fact)

- id: `fact-006`
- type: `fact`
- question: JDK 8 中 HashMap 的底层结构是什么？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["JDK 8：数组 + 链表 + 红黑树", "链表长度 > 8"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `1`
- answer: 在 JDK 8 中，HashMap 的底层结构是数组 + 链表 + 红黑树。当链表长度大于 8 时，链表会转换为红黑树。
- expected_keywords: `["数组", "链表", "红黑树", "链表长度 > 8"]`
- keyword_hit: `3/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "chunkIndex": 1.0,
      "tokenCount": 420.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 36.0,
      "kbId": 11.0,
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
    "source": "bba6e34a-4290-4aba-ab90-676e863fd5e8",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "bba6e34a-4290-4aba-ab90-676e863fd5e8",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 1,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 438,
      "startIndex": 18,
      "title": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "0a027708-629e-408e-87ff-e2a96c1fccd8",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0a027708-629e-408e-87ff-e2a96c1fccd8",
    "contentPreview": "基于接口 CGLIB 代理：基于继承 3. Spring Boot 3.1 自动配置原理 @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan 条件注解 ： @ConditionalOnClass：类路径存在某个类 @ConditionalOnMissingBean：容器中不存在某个 Bean @ConditionalOnProperty：配置文件中存在某个属性 3.2 配置文件 application.yml vs application.properties ： yml 支持层...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 4,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 1357,
      "startIndex": 924,
      "title": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "68aa7ca8-7a12-4410-b8f5-94d664c6e0e0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "68aa7ca8-7a12-4410-b8f5-94d664c6e0e0",
    "contentPreview": "Spring 框架 2.1 Spring IOC（控制反转） 依赖注入的三种方式 ： 构造器注入（推荐，保证不可变性） Setter 注入（可选依赖） 字段注入（不推荐，难以测试） Bean 的生命周期 ： 实例化 属性赋值 初始化（@PostConstruct） 使用 销毁（@PreDestroy） 2.2 Spring AOP（面向切面编程） 应用场景 ：事务管理、日志记录、权限校验、性能监控 核心概念 ： Aspect（切面）：横切关注点的模块化 Join Point（连接点）：方法执行点 Pointcut（切入点）：匹配规则 Advice（通知）：@Before、@After、@Aro...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 3,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 945,
      "startIndex": 603,
      "title": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "0569c9c9-aa04-49fc-add5-f3913b7a5717",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0569c9c9-aa04-49fc-add5-f3913b7a5717",
    "contentPreview": ". MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNCOMMITTED（读未提交） READ COMMITTED（读已提交） REPEATABLE READ（可重复读，MySQL ...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 6,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 1921,
      "startIndex": 1564,
      "title": "java-interview-guide.md",
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
[
  {
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "score": 0.016393441706895828,
    "snippet": "2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0."
  },
  {
    "source": "bba6e34a-4290-4aba-ab90-676e863fd5e8",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "bba6e34a-4290-4aba-ab90-676e863fd5e8",
    "score": 0.016393441706895828,
    "snippet": "2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0."
  },
  {
    "source": "0a027708-629e-408e-87ff-e2a96c1fccd8",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0a027708-629e-408e-87ff-e2a96c1fccd8",
    "score": 0.016129031777381897,
    "snippet": "properties ： yml 支持层级结构，可读性更好 properties 更简单，兼容性更好 多环境配置 ： application-dev."
  }
]
```
- citation validation metadata:
```json
{
  "validCitations": 3,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
- keyword_hit: `0/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "contentPreview": ". MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNCOMMITTED（读未提交） READ COMMITTED（读已提交） REPEATABLE READ（可重复读，MySQL ...",
    "metadata": {
      "chunkIndex": 6.0,
      "tokenCount": 357.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 36.0,
      "kbId": 11.0,
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
    "source": "0569c9c9-aa04-49fc-add5-f3913b7a5717",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0569c9c9-aa04-49fc-add5-f3913b7a5717",
    "contentPreview": ". MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNCOMMITTED（读未提交） READ COMMITTED（读已提交） REPEATABLE READ（可重复读，MySQL ...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 6,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 1921,
      "startIndex": 1564,
      "title": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 4,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1193,
      "startIndex": 961,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.015625,
    "source": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 5,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1504,
      "startIndex": 1121,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 5,
    "score": 0.01515151560306549,
    "source": "b72b7089-fdb9-4b82-8740-d6b5028717ca",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "b72b7089-fdb9-4b82-8740-d6b5028717ca",
    "contentPreview": "GC 收集器 ： Serial、Parallel、CMS、G1、ZGC --- 面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 3161,
      "startIndex": 2764,
      "title": "java-interview-guide.md",
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
  "validCitations": 0,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
  "unsupportedCitationCount": 0
}
```

### definition-003 (definition)

- id: `definition-003`
- type: `definition`
- question: 什么是向量数据库？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["专门存储和检索向量的数据库", "支持高效的相似度搜索"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `2`
- answer:
- expected_keywords: `["存储", "检索向量", "相似度搜索"]`
- keyword_hit: `0/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "0",
    "contentPreview": "3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文档、技术博客 优势 ：快速查找代码示例、技术方案 7.4 法律文书检索 场景 ：...",
    "metadata": {
      "chunkIndex": 11.0,
      "tokenCount": 328.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 37.0,
      "kbId": 11.0,
      "startIndex": 2320.0,
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
    "source": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 4,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1193,
      "startIndex": 961,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "d770ae16-5ca7-4ad8-b9f9-c42a8a31d225",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "d770ae16-5ca7-4ad8-b9f9-c42a8a31d225",
    "contentPreview": "Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.52, ..., 0.91] (相似度高) \"今天天气很好\" → [-0.78,...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 3,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1022,
      "startIndex": 641,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "37bb6c85-b083-42a4-b849-890dbf989082",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "37bb6c85-b083-42a4-b849-890dbf989082",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 12,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2959,
      "startIndex": 2640,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "77017a1a-4e1b-4092-bc23-8433fb9447f4",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "77017a1a-4e1b-4092-bc23-8433fb9447f4",
    "contentPreview": "提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召回相关文档 → 构建提示词 → LLM 生成答案 → 返回结果 2.1 详细步骤 Step 1: 文档处理（离线） 文档上传 ：PDF、Markdown、Word、代码文件等 文档解析 ：提取文本内容 文本分块 ：将长文档切分为小块（Chunk），通常 500-2000 字符 向量化 ：使用 Embedding 模型将文本转换为向量 存储 ：向量存入向量数据库（Milvus、Pinecone 等） Step 2: 问答处理（在线） 问题向量化 ：用户问题转换为向量 相似度检索 ...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 1,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 631,
      "startIndex": 241,
      "title": "rag-technology-guide.md",
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
  "validCitations": 0,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
  "unsupportedCitationCount": 0
}
```

### definition-005 (definition)

- id: `definition-005`
- type: `definition`
- question: Spring IOC 指的是什么？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["Spring IOC（控制反转）", "依赖注入的三种方式"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `1`
- answer: Spring IOC 指的是控制反转（Inversion of Control）。
- expected_keywords: `["控制反转", "依赖注入", "构造器注入"]`
- keyword_hit: `1/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "contentPreview": "Spring 框架 2.1 Spring IOC（控制反转） 依赖注入的三种方式 ： 构造器注入（推荐，保证不可变性） Setter 注入（可选依赖） 字段注入（不推荐，难以测试） Bean 的生命周期 ： 实例化 属性赋值 初始化（@PostConstruct） 使用 销毁（@PreDestroy） 2.2 Spring AOP（面向切面编程） 应用场景 ：事务管理、日志记录、权限校验、性能监控 核心概念 ： Aspect（切面）：横切关注点的模块化 Join Point（连接点）：方法执行点 Pointcut（切入点）：匹配规则 Advice（通知）：@Before、@After、@Aro...",
    "metadata": {
      "chunkIndex": 3.0,
      "tokenCount": 341.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 36.0,
      "kbId": 11.0,
      "startIndex": 603.0,
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
    "source": "68aa7ca8-7a12-4410-b8f5-94d664c6e0e0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "68aa7ca8-7a12-4410-b8f5-94d664c6e0e0",
    "contentPreview": "Spring 框架 2.1 Spring IOC（控制反转） 依赖注入的三种方式 ： 构造器注入（推荐，保证不可变性） Setter 注入（可选依赖） 字段注入（不推荐，难以测试） Bean 的生命周期 ： 实例化 属性赋值 初始化（@PostConstruct） 使用 销毁（@PreDestroy） 2.2 Spring AOP（面向切面编程） 应用场景 ：事务管理、日志记录、权限校验、性能监控 核心概念 ： Aspect（切面）：横切关注点的模块化 Join Point（连接点）：方法执行点 Pointcut（切入点）：匹配规则 Advice（通知）：@Before、@After、@Aro...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 3,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 945,
      "startIndex": 603,
      "title": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "37bb6c85-b083-42a4-b849-890dbf989082",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "37bb6c85-b083-42a4-b849-890dbf989082",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 12,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2959,
      "startIndex": 2640,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "4ba6b2a6-5323-4fb4-8d04-d9e0c83479a6",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "4ba6b2a6-5323-4fb4-8d04-d9e0c83479a6",
    "contentPreview": "Spring Boot 核心知识点 1. Spring Boot 简介 Spring Boot 是基于 Spring 框架的快速开发脚手架，旨在简化 Spring 应用的初始搭建和开发过程。 1.1 核心特性 自动配置 ：根据 classpath 下的依赖自动配置 Spring 应用 起步依赖 ：一站式依赖管理，避免版本冲突 内嵌服务器 ：Tomcat、Jetty、Undertow 生产就绪 ：Actuator 健康检查、监控指标 无代码生成 ：不需要 XML 配置 2.",
    "metadata": {
      "kbId": 11,
      "fileName": "springboot-basics.md",
      "chunkIndex": 0,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "springboot-basics.md",
      "documentId": 35,
      "endIndex": 239,
      "startIndex": 0,
      "title": "springboot-basics.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "9eea1fbd-f499-452c-bbb0-de6ee1de6247",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "9eea1fbd-f499-452c-bbb0-de6ee1de6247",
    "contentPreview": ", \"admin\"); when(userService.getById(1L)).thenReturn(user); mockMvc.perform(get(\"/api/users/1\")) .andExpect(status().isOk()) .andExpect(jsonPath(\"$.data.username\").value(\"admin\")); } } --- 学习建议 ： 多看官方文档：https://spring.io/projects/spring-boot 多写代码，实践是最好的老师 阅读优秀开源项目源码 关注最新技术动态 祝你学习顺利！🚀",
    "metadata": {
      "kbId": 11,
      "fileName": "springboot-basics.md",
      "chunkIndex": 24,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "springboot-basics.md",
      "documentId": 35,
      "endIndex": 6311,
      "startIndex": 5799,
      "title": "springboot-basics.md",
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
[
  {
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "score": 0.016393441706895828,
    "snippet": "1 Spring IOC（控制反转） 依赖注入的三种方式 ： 构造器注入（推荐，保证不可变性） Setter 注入（可选依赖） 字段注入（不推荐，难以测试） Bean 的生命周期 ： 实例化 属性赋值 初始化（@PostConstruct） 使用 销毁（@PreDestroy） 2."
  },
  {
    "source": "68aa7ca8-7a12-4410-b8f5-94d664c6e0e0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "68aa7ca8-7a12-4410-b8f5-94d664c6e0e0",
    "score": 0.016393441706895828,
    "snippet": "1 Spring IOC（控制反转） 依赖注入的三种方式 ： 构造器注入（推荐，保证不可变性） Setter 注入（可选依赖） 字段注入（不推荐，难以测试） Bean 的生命周期 ： 实例化 属性赋值 初始化（@PostConstruct） 使用 销毁（@PreDestroy） 2."
  },
  {
    "source": "37bb6c85-b083-42a4-b849-890dbf989082",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "37bb6c85-b083-42a4-b849-890dbf989082",
    "score": 0.016129031777381897,
    "snippet": "Spring Boot 后端"
  }
]
```
- citation validation metadata:
```json
{
  "validCitations": 3,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
- answer: 缓存穿透是指查询不存在的数据，导致缓存和数据库中都没有该数据的情况。
- expected_keywords: `["缓存穿透", "不存在的数据", "布隆过滤器", "缓存空值"]`
- keyword_hit: `2/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "contentPreview": "） 锁机制 ： 行锁：锁定某一行 表锁：锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好...",
    "metadata": {
      "chunkIndex": 7.0,
      "tokenCount": 303.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 36.0,
      "kbId": 11.0,
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
    "source": "41240cb9-807f-4839-907a-c4385036f132",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "41240cb9-807f-4839-907a-c4385036f132",
    "contentPreview": "） 锁机制 ： 行锁：锁定某一行 表锁：锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 7,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 2187,
      "startIndex": 1884,
      "title": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.01587301678955555,
    "source": "e2b974e9-c87e-45b3-879b-ffb23565e898",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "e2b974e9-c87e-45b3-879b-ffb23565e898",
    "contentPreview": "(\"当前时间：{}\", LocalDateTime.now()); } // 每天凌晨 2 点执行 @Scheduled(cron = \"0 0 2 * * ?\") public void cleanupExpiredData() { log.info(\"清理过期数据\"); } } 12. 缓存 12.1 Spring Cache @Configuration @EnableCaching public class CacheConfig { // 配置 Redis 缓存 } @Service public class UserService { @Cacheable(value = \"use...",
    "metadata": {
      "kbId": 11,
      "fileName": "springboot-basics.md",
      "chunkIndex": 21,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "springboot-basics.md",
      "documentId": 35,
      "endIndex": 5336,
      "startIndex": 4919,
      "title": "springboot-basics.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 4,
    "score": 0.015625,
    "source": "fceb5000-22c6-4337-bf1f-2d83c5e7ff33",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "fceb5000-22c6-4337-bf1f-2d83c5e7ff33",
    "contentPreview": "数据库，之后从缓存读取 } @CacheEvict(value = \"users\", key = \"#id\") public void deleteById(Long id) { // 删除缓存 } @CachePut(value = \"users\", key = \"#user.id\") public User update(User user) { // 更新缓存 } } 13. 单元测试 13.1 测试示例 @SpringBootTest @AutoConfigureMockMvc class UserControllerTest { @Autowired private MockMvc ...",
    "metadata": {
      "kbId": 11,
      "fileName": "springboot-basics.md",
      "chunkIndex": 22,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "springboot-basics.md",
      "documentId": 35,
      "endIndex": 5718,
      "startIndex": 5319,
      "title": "springboot-basics.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 5,
    "score": 0.015384615398943424,
    "source": "4c61d28b-7f57-4679-bcc4-3d9aca5dbe61",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "4c61d28b-7f57-4679-bcc4-3d9aca5dbe61",
    "contentPreview": "l 通过 spring.profiles.active 切换 4. MyBatis / MyBatis-Plus 4.1 MyBatis 核心 #{} vs ${} ： #{} 预编译，防止 SQL 注入 ${} 字符串替换，存在注入风险 一级缓存 vs 二级缓存 ： 一级缓存：SqlSession 级别，默认开启 二级缓存：Mapper 级别，需要手动开启 4.2 MyBatis-Plus 优势 ： 内置通用 CRUD 方法 代码生成器 分页插件 乐观锁、逻辑删除支持 5.",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 5,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 1565,
      "startIndex": 1324,
      "title": "java-interview-guide.md",
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
[
  {
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "score": 0.016393441706895828,
    "snippet": "2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6."
  },
  {
    "source": "41240cb9-807f-4839-907a-c4385036f132",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "41240cb9-807f-4839-907a-c4385036f132",
    "score": 0.016393441706895828,
    "snippet": "2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6."
  },
  {
    "source": "e2b974e9-c87e-45b3-879b-ffb23565e898",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "e2b974e9-c87e-45b3-879b-ffb23565e898",
    "score": 0.01587301678955555,
    "snippet": "// 第一次查询数据库，之后从缓存读取"
  }
]
```
- citation validation metadata:
```json
{
  "validCitations": 3,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
  "unsupportedCitationCount": 0
}
```

### definition-008 (definition)

- id: `definition-008`
- type: `definition`
- question: 什么是分布式锁，文档中列出了哪些实现方式？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["分布式锁", "Redis SETNX + 过期时间"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `1`
- answer: 分布式锁用于在分布式系统中协调多个进程对共享资源的访问，防止并发冲突。根据文档内容，分布式锁的实现方式包括： - Redis 的 SETNX 命令结合过期时间
- Redisson（推荐）
- Zookeeper 此外，文档还提到使用分布式锁时需要注意防止死锁（通过设置过期时间）、防止误删（使用 UUID 标识）以及保证原子性（使用 Lua 脚本）。
- expected_keywords: `["Redis SETNX", "Redisson", "Zookeeper", "过期时间"]`
- keyword_hit: `3/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "contentPreview": "y 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好 7. 分布式 7.1 分布式锁 实现方式 ： Redis SETNX + 过期时间 Redisson（推荐） Zookeeper 注意事项 ： 防止死锁（设置过期时间） 防止误删（UUID 标识） 原子性（Lua 脚本） 7.2 分布式事务 2PC（两阶段提交） ： 准备阶段、提交阶段 缺点：阻塞、单点故障 TCC（Try-Confirm-Cancel） ： 业务侵入性强 性能较好 Seata ：阿里开源分布式事务框架 8. 消息队列 8.1 RabbitMQ / Kafk...",
    "metadata": {
      "chunkIndex": 8.0,
      "tokenCount": 381.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 36.0,
      "kbId": 11.0,
      "startIndex": 2124.0,
      "endIndex": 2505.0,
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
    "source": "2e8ca4e9-f04a-4893-af1f-80a0d7e3c7cf",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "2e8ca4e9-f04a-4893-af1f-80a0d7e3c7cf",
    "contentPreview": "y 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好 7. 分布式 7.1 分布式锁 实现方式 ： Redis SETNX + 过期时间 Redisson（推荐） Zookeeper 注意事项 ： 防止死锁（设置过期时间） 防止误删（UUID 标识） 原子性（Lua 脚本） 7.2 分布式事务 2PC（两阶段提交） ： 准备阶段、提交阶段 缺点：阻塞、单点故障 TCC（Try-Confirm-Cancel） ： 业务侵入性强 性能较好 Seata ：阿里开源分布式事务框架 8. 消息队列 8.1 RabbitMQ / Kafk...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 8,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 2505,
      "startIndex": 2124,
      "title": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.015625,
    "source": "41240cb9-807f-4839-907a-c4385036f132",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "41240cb9-807f-4839-907a-c4385036f132",
    "contentPreview": "） 锁机制 ： 行锁：锁定某一行 表锁：锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 7,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 2187,
      "startIndex": 1884,
      "title": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 4,
    "score": 0.016129031777381897,
    "source": "3a85e3c6-95a4-49c7-a925-0653d304b149",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "3a85e3c6-95a4-49c7-a925-0653d304b149",
    "contentPreview": "如果文档中没有相关信息，明确说明 3. 引用具体的文档片段作为依据 4. 回答要简洁清晰",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 8,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1679,
      "startIndex": 1634,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 5,
    "score": 0.01587301678955555,
    "source": "996590f5-757b-4c51-a02c-1d1f91faed6e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "996590f5-757b-4c51-a02c-1d1f91faed6e",
    "contentPreview": "【相关文档】 {retrieved_context} 【用户提问】 {user_question} 【回答要求】 1. 基于文档内容回答，不要编造信息 2. 如果文档中没有相关信息，明确说明 3. 引用具体的文档片段作为依据 4. 回答要简洁清晰",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 7,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1679,
      "startIndex": 1550,
      "title": "rag-technology-guide.md",
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
[
  {
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "score": 0.016393441706895828,
    "snippet": "1 分布式锁 实现方式 ： Redis SETNX + 过期时间 Redisson（推荐） Zookeeper 注意事项 ： 防止死锁（设置过期时间） 防止误删（UUID 标识） 原子性（Lua 脚本） 7."
  },
  {
    "source": "2e8ca4e9-f04a-4893-af1f-80a0d7e3c7cf",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "2e8ca4e9-f04a-4893-af1f-80a0d7e3c7cf",
    "score": 0.016393441706895828,
    "snippet": "1 分布式锁 实现方式 ： Redis SETNX + 过期时间 Redisson（推荐） Zookeeper 注意事项 ： 防止死锁（设置过期时间） 防止误删（UUID 标识） 原子性（Lua 脚本） 7."
  },
  {
    "source": "3a85e3c6-95a4-49c7-a925-0653d304b149",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "3a85e3c6-95a4-49c7-a925-0653d304b149",
    "score": 0.016129031777381897,
    "snippet": "如果文档中没有相关信息，明确说明"
  }
]
```
- citation validation metadata:
```json
{
  "validCitations": 3,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
  "unsupportedCitationCount": 0
}
```

### reasoning-001 (reasoning)

- id: `reasoning-001`
- type: `reasoning`
- question: 为什么 RAG 系统需要把文档分块？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["为什么需要分块？", "提高检索效率"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `1`
- answer: RAG 系统需要对文档进行分块，主要原因如下： 1. **LLM 的 Token 限制**：大语言模型（如 GPT-3.5 支持 4096 个 Token，GPT-4 支持 8192 或 32768 个 Token）无法一次性处理过长的文本，因此需要将长文档切分为更小的块以适应输入限制。
2. **提高检索精准度**：小块内容更聚焦，有助于向量检索系统更准确地匹配用户问题与相关文档片段。
3. **提升检索效率**：分块后数据规模更小，向量数据库在搜索时能更快定位到最相关的片段，从而加快整体响应速度。 常见的分块方法包括固定长度分块、按句子或段落分块、滑动窗口分块以及基于语义的分块等，具体选择需根据文档类型和应用场景调整。
- expected_keywords: `["Token 限制", "检索更精准", "检索效率", "语义割裂"]`
- keyword_hit: `2/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 5,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1504,
      "startIndex": 1121,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 2,
    "score": 0.01587301678955555,
    "source": "37bb6c85-b083-42a4-b849-890dbf989082",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "37bb6c85-b083-42a4-b849-890dbf989082",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 12,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2959,
      "startIndex": 2640,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "2fc312fe-bd9e-45a6-bfdf-29d93b357ea5",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "2fc312fe-bd9e-45a6-bfdf-29d93b357ea5",
    "contentPreview": "：总结 4. 提示词工程（Prompt Engineering） RAG 系统需要精心设计提示词，将检索到的上下文和问题结合。 4.1 典型 Prompt 模板 你是一个专业的技术助手。请根据以下文档内容回答用户的问题。",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 6,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1550,
      "startIndex": 1441,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.015625,
    "source": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 4,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1193,
      "startIndex": 961,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 5,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "0",
    "contentPreview": "文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈 关键指标 ： 检索召回率：Top-K 包含正确答案的比例 答案准确率：生成答案的正确性 用户满意度：...",
    "metadata": {
      "chunkIndex": 13.0,
      "tokenCount": 347.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 37.0,
      "kbId": 11.0,
      "startIndex": 2880.0,
      "endIndex": 3227.0,
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
[
  {
    "source": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "score": 0.016393441706895828,
    "snippet": "5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字）"
  },
  {
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "0",
    "score": 0.016393441706895828,
    "snippet": "A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？"
  },
  {
    "source": "2fc312fe-bd9e-45a6-bfdf-29d93b357ea5",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "2fc312fe-bd9e-45a6-bfdf-29d93b357ea5",
    "score": 0.016129031777381897,
    "snippet": "提示词工程（Prompt Engineering） RAG 系统需要精心设计提示词，将检索到的上下文和问题结合。"
  }
]
```
- citation validation metadata:
```json
{
  "validCitations": 3,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
- answer: RAG（检索增强生成）在知识更新和可解释性方面通常比微调更适合企业知识库，原因如下： 1. **知识更新**：RAG 只需实时更新文档即可反映最新知识，无需重新训练模型；而微调则需要重新训练，成本高且耗时。
2. **可解释性**：RAG 的回答可以追溯到具体的文档来源，具有更高的可解释性；而微调后的模型生成内容往往难以追溯其依据，可解释性较低。 因此，对于需要频繁更新知识且要求回答可追溯的企业知识库场景，RAG 是更优选择。
- expected_keywords: `["实时更新文档", "成本低", "可追溯来源", "可解释性高"]`
- keyword_hit: `1/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "0",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "chunkIndex": 10.0,
      "tokenCount": 333.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 37.0,
      "kbId": 11.0,
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
    "source": "926499a3-c300-4daf-a598-e80534fee04c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "926499a3-c300-4daf-a598-e80534fee04c",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2333,
      "startIndex": 2000,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "901d738b-2d63-4b11-9c91-d9aa39fb8860",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "901d738b-2d63-4b11-9c91-d9aa39fb8860",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 0,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 270,
      "startIndex": 0,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.015384615398943424,
    "source": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 5,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1504,
      "startIndex": 1121,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015384615384615385
    }
  },
  {
    "rank": 5,
    "score": 0.01587301678955555,
    "source": "8b5df374-b365-41d3-8322-579cc48c0757",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "8b5df374-b365-41d3-8322-579cc48c0757",
    "contentPreview": "3: 向量数据库如何选择？ 小规模 （< 100 万向量）：Qdrant、Weaviate 大规模 （> 100 万向量）：Milvus、Pinecone 本地部署 ：Milvus、Qdrant 云服务 ：Pinecone、Weaviate Cloud 7. 实际应用场景 7.1 企业知识库问答 场景 ：公司内部文档、规章制度、技术文档 优势 ：新员工快速了解公司知识 7.2 客服机器人 场景 ：产品手册、常见问题、售后政策 优势 ：24/7 自动回答，降低人工成本 7.3 智能编程助手 场景 ：代码库、API 文档、技术博客 优势 ：快速查找代码示例、技术方案 7.4 法律文书检索 场景 ：...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 11,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2648,
      "startIndex": 2320,
      "title": "rag-technology-guide.md",
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
[
  {
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "0",
    "score": 0.016393441706895828,
    "snippet": "| 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。"
  },
  {
    "source": "926499a3-c300-4daf-a598-e80534fee04c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "926499a3-c300-4daf-a598-e80534fee04c",
    "score": 0.016393441706895828,
    "snippet": "| 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。"
  },
  {
    "source": "901d738b-2d63-4b11-9c91-d9aa39fb8860",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "901d738b-2d63-4b11-9c91-d9aa39fb8860",
    "score": 0.016129031777381897,
    "snippet": "1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2."
  }
]
```
- citation validation metadata:
```json
{
  "validCitations": 3,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
- answer: 根据提供的上下文，RAG 检索后使用 Rerank（重排序）是为了**二次精排**，从而提高 RAG 系统的准确率。 在提升 RAG 准确率的最佳实践中，除了优化分块策略、提升 Embedding 质量和增加 Top-K（检索更多候选）外，引入 Rerank 步骤可以对初步检索到的候选结果进行更精细的排序，确保最相关的信息被优先提供给大语言模型（LLM）作为上下文，从而生成更准确的答案。
- expected_keywords: `["重排序", "二次精排", "提高准确率", "检索优化"]`
- keyword_hit: `2/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016129031777381897,
    "source": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 5,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1504,
      "startIndex": 1121,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 2,
    "score": 0.01587301678955555,
    "source": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 4,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1193,
      "startIndex": 961,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 3,
    "score": 0.015384615398943424,
    "source": "37bb6c85-b083-42a4-b849-890dbf989082",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "37bb6c85-b083-42a4-b849-890dbf989082",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 12,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2959,
      "startIndex": 2640,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015384615384615385
    }
  },
  {
    "rank": 4,
    "score": 0.016393441706895828,
    "source": "926499a3-c300-4daf-a598-e80534fee04c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "926499a3-c300-4daf-a598-e80534fee04c",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2333,
      "startIndex": 2000,
      "title": "rag-technology-guide.md",
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
    "documentId": 37,
    "chunkId": "0",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "chunkIndex": 0.0,
      "tokenCount": 269.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 37.0,
      "kbId": 11.0,
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
[
  {
    "source": "926499a3-c300-4daf-a598-e80534fee04c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "926499a3-c300-4daf-a598-e80534fee04c",
    "score": 0.016393441706895828,
    "snippet": "优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？"
  },
  {
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "0",
    "score": 0.016393441706895828,
    "snippet": "1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2."
  },
  {
    "source": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "score": 0.016129031777381897,
    "snippet": "提示词工程（Prompt Engineering） RAG 系统需要精心设计提示词，将检索到的上下文和问题结合。"
  }
]
```
- citation validation metadata:
```json
{
  "validCitations": 3,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
  "unsupportedCitationCount": 0
}
```

### reasoning-004 (reasoning)

- id: `reasoning-004`
- type: `reasoning`
- question: 为什么 Spring 中更推荐构造器注入而不推荐字段注入？
- expected_sources: `["springboot-basics.md", "java-interview-guide.md"]`
- expected_contexts.contains: `["构造器注入（推荐，保证不可变性）", "字段注入（不推荐）"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `1`
- answer: 在 Spring 中，更推荐构造器注入而不推荐字段注入，主要原因如下： 1. **保证不可变性**：构造器注入允许将依赖项声明为 `final`，从而确保对象一旦创建，其依赖关系就不会被修改，提高了代码的健壮性和线程安全性。
2. **便于测试**：使用构造器注入时，可以在单元测试中轻松传入模拟对象（Mock），而字段注入由于依赖 Spring 容器自动注入，难以在脱离容器环境下进行独立测试。
3. **依赖关系明确**：构造器注入在类定义时就能清晰看出该类所依赖的组件，而字段注入隐藏了依赖关系，降低了代码的可读性和可维护性。 因此，构造器注入被广泛认为是更优的依赖注入方式。
- expected_keywords: `["构造器注入", "保证不可变性", "字段注入", "难以测试"]`
- keyword_hit: `3/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "contentPreview": "Spring 框架 2.1 Spring IOC（控制反转） 依赖注入的三种方式 ： 构造器注入（推荐，保证不可变性） Setter 注入（可选依赖） 字段注入（不推荐，难以测试） Bean 的生命周期 ： 实例化 属性赋值 初始化（@PostConstruct） 使用 销毁（@PreDestroy） 2.2 Spring AOP（面向切面编程） 应用场景 ：事务管理、日志记录、权限校验、性能监控 核心概念 ： Aspect（切面）：横切关注点的模块化 Join Point（连接点）：方法执行点 Pointcut（切入点）：匹配规则 Advice（通知）：@Before、@After、@Aro...",
    "metadata": {
      "chunkIndex": 3.0,
      "tokenCount": 341.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 36.0,
      "kbId": 11.0,
      "startIndex": 603.0,
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
    "source": "aec9e5d0-9648-4cd0-a7ae-ccdfa22fbd1e",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "aec9e5d0-9648-4cd0-a7ae-ccdfa22fbd1e",
    "contentPreview": "logging: level: root: INFO com.example: DEBUG 3.2 配置优先级 命令行参数 JNDI 属性 Java 系统属性 环境变量 application.properties/yml 4. 依赖注入 4.1 构造器注入（推荐） @Service @RequiredArgsConstructor public class UserService { private final UserRepository userRepository; // Lombok 自动生成构造器，Spring 自动注入 } 4.2 字段注入（不推荐） @Service publi...",
    "metadata": {
      "kbId": 11,
      "fileName": "springboot-basics.md",
      "chunkIndex": 6,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "springboot-basics.md",
      "documentId": 35,
      "endIndex": 1393,
      "startIndex": 973,
      "title": "springboot-basics.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "68aa7ca8-7a12-4410-b8f5-94d664c6e0e0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "68aa7ca8-7a12-4410-b8f5-94d664c6e0e0",
    "contentPreview": "Spring 框架 2.1 Spring IOC（控制反转） 依赖注入的三种方式 ： 构造器注入（推荐，保证不可变性） Setter 注入（可选依赖） 字段注入（不推荐，难以测试） Bean 的生命周期 ： 实例化 属性赋值 初始化（@PostConstruct） 使用 销毁（@PreDestroy） 2.2 Spring AOP（面向切面编程） 应用场景 ：事务管理、日志记录、权限校验、性能监控 核心概念 ： Aspect（切面）：横切关注点的模块化 Join Point（连接点）：方法执行点 Pointcut（切入点）：匹配规则 Advice（通知）：@Before、@After、@Aro...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 3,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 945,
      "startIndex": 603,
      "title": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.015625,
    "source": "37bb6c85-b083-42a4-b849-890dbf989082",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "37bb6c85-b083-42a4-b849-890dbf989082",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 12,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2959,
      "startIndex": 2640,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 5,
    "score": 0.015384615398943424,
    "source": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 4,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1193,
      "startIndex": 961,
      "title": "rag-technology-guide.md",
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
[
  {
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "score": 0.016393441706895828,
    "snippet": "1 Spring IOC（控制反转） 依赖注入的三种方式 ： 构造器注入（推荐，保证不可变性） Setter 注入（可选依赖） 字段注入（不推荐，难以测试） Bean 的生命周期 ： 实例化 属性赋值 初始化（@PostConstruct） 使用 销毁（@PreDestroy） 2."
  },
  {
    "source": "aec9e5d0-9648-4cd0-a7ae-ccdfa22fbd1e",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "aec9e5d0-9648-4cd0-a7ae-ccdfa22fbd1e",
    "score": 0.016393441706895828,
    "snippet": "// Lombok 自动生成构造器，Spring 自动注入"
  },
  {
    "source": "68aa7ca8-7a12-4410-b8f5-94d664c6e0e0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "68aa7ca8-7a12-4410-b8f5-94d664c6e0e0",
    "score": 0.016129031777381897,
    "snippet": "1 Spring IOC（控制反转） 依赖注入的三种方式 ： 构造器注入（推荐，保证不可变性） Setter 注入（可选依赖） 字段注入（不推荐，难以测试） Bean 的生命周期 ： 实例化 属性赋值 初始化（@PostConstruct） 使用 销毁（@PreDestroy） 2."
  }
]
```
- citation validation metadata:
```json
{
  "validCitations": 3,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
- answer: 知识库中没有足够信息回答该问题。
- expected_keywords: `["预编译", "SQL 注入", "字符串替换"]`
- keyword_hit: `0/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 4,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1193,
      "startIndex": 961,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 2,
    "score": 0.01587301678955555,
    "source": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 5,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1504,
      "startIndex": 1121,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 3,
    "score": 0.015625,
    "source": "37bb6c85-b083-42a4-b849-890dbf989082",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "37bb6c85-b083-42a4-b849-890dbf989082",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 12,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2959,
      "startIndex": 2640,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015625
    }
  },
  {
    "rank": 4,
    "score": 0.01515151560306549,
    "source": "926499a3-c300-4daf-a598-e80534fee04c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "926499a3-c300-4daf-a598-e80534fee04c",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2333,
      "startIndex": 2000,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015151515151515152
    }
  },
  {
    "rank": 5,
    "score": 0.014492753893136978,
    "source": "996590f5-757b-4c51-a02c-1d1f91faed6e",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "996590f5-757b-4c51-a02c-1d1f91faed6e",
    "contentPreview": "【相关文档】 {retrieved_context} 【用户提问】 {user_question} 【回答要求】 1. 基于文档内容回答，不要编造信息 2. 如果文档中没有相关信息，明确说明 3. 引用具体的文档片段作为依据 4. 回答要简洁清晰",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 7,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1679,
      "startIndex": 1550,
      "title": "rag-technology-guide.md",
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
  "validCitations": 0,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
- answer: 要提高 RAG 准确率，可以从以下三个方面进行优化： 1. **检索优化**： - 采用混合检索，结合向量检索与关键词检索（如 BM25）； - 使用重排序（Rerank）技术进行二次精排，提升检索结果的相关性； - 通过 Query 改写扩展或改写用户问题，提高召回率； - 增加 Top-K 值，检索更多候选文档。 2. **分块策略优化**： - 合理设置 Chunk 大小，推荐在 500–1000 字符之间，根据文档类型调整； - 技术文档可按段落或章节分块，代码文档可按函数或类分块； - 避免 Chunk 过小（<200 字符）导致语义不完整，或过大（>2000 字符）导致检索不精准和 Token 消耗过多； - 可结合固定长度、句子/段落、滑动窗口或语义分块等方法。 3. **Prompt 优化**： - 明确回答要求，定义 AI 的角色（如技术助手、客服等）； - 提供约束条件，如禁止编造、要求引用来源； - 使用 Few-Shot 提供示例问答； - 引入 Chain of Thought，要求模型逐步推理。 此外，提升 Embedding 质量（选择合适的模型）也是提高准确率的重要环节。
- expected_keywords: `["优化分块策略", "提升 Embedding 质量", "增加 Top-K", "使用 Rerank", "优化 Prompt"]`
- keyword_hit: `2/5`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "0",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "chunkIndex": 10.0,
      "tokenCount": 333.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 37.0,
      "kbId": 11.0,
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
    "source": "926499a3-c300-4daf-a598-e80534fee04c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "926499a3-c300-4daf-a598-e80534fee04c",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2333,
      "startIndex": 2000,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "64ea28ee-c330-4d25-ac22-39c926d02747",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "64ea28ee-c330-4d25-ac22-39c926d02747",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 9,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2021,
      "startIndex": 1679,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "d76c3fc0-ef54-4e0e-84b3-ef38b25607f1",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "d76c3fc0-ef54-4e0e-84b3-ef38b25607f1",
    "contentPreview": "文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈 关键指标 ： 检索召回率：Top-K 包含正确答案的比例 答案准确率：生成答案的正确性 用户满意度：...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 13,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 3227,
      "startIndex": 2880,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.015625,
    "source": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 5,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1504,
      "startIndex": 1121,
      "title": "rag-technology-guide.md",
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
[
  {
    "source": "0",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "0",
    "score": 0.016393441706895828,
    "snippet": "优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？"
  },
  {
    "source": "926499a3-c300-4daf-a598-e80534fee04c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "926499a3-c300-4daf-a598-e80534fee04c",
    "score": 0.016393441706895828,
    "snippet": "优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排 优化 Prompt ：明确回答要求 Q3: 向量数据库如何选择？"
  },
  {
    "source": "64ea28ee-c330-4d25-ac22-39c926d02747",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "64ea28ee-c330-4d25-ac22-39c926d02747",
    "score": 0.016129031777381897,
    "snippet": "2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5."
  }
]
```
- citation validation metadata:
```json
{
  "validCitations": 3,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
- answer: 根据提供的上下文，在接口性能优化手段中，关于使用缓存（Redis）的内容仅出现在《java-interview-guide.md》（Java 面试文档）中，具体手段为“使用缓存（Redis）”。 提供的上下文中**没有包含**名为"Spring Boot 文档”的独立内容，且《java-interview-guide.md》中关于接口性能优化的部分并未区分或列出专门针对"Spring Boot 文档”的额外缓存手段，仅统一列出了“使用缓存（Redis）”这一项。 因此，基于现有上下文：
1. **Java 面试文档**（java-interview-guide.md）：明确列出的手段是“使用缓存（Redis）”。
2. **Spring Boot 文档**：上下文中未提供独立的 Spring Boot 文档内容，也未列出区别于 Java 面试文档的特定缓存手段。
- expected_keywords: `["Redis", "使用缓存", "@Cacheable", "@CacheEvict", "@CachePut"]`
- keyword_hit: `2/5`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.016393441706895828,
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "contentPreview": "GC 收集器 ： Serial、Parallel、CMS、G1、ZGC --- 面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID...",
    "metadata": {
      "chunkIndex": 10.0,
      "tokenCount": 398.0,
      "headingPath": "",
      "sourceFileName": "java-interview-guide.md",
      "documentId": 36.0,
      "kbId": 11.0,
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
    "source": "b72b7089-fdb9-4b82-8740-d6b5028717ca",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "b72b7089-fdb9-4b82-8740-d6b5028717ca",
    "contentPreview": "GC 收集器 ： Serial、Parallel、CMS、G1、ZGC --- 面试常见问题 1. Spring Boot 启动流程？ 创建 SpringApplication 对象 运行 run 方法 准备环境（Environment） 创建 ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 3161,
      "startIndex": 2764,
      "title": "java-interview-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.01639344262295082
    }
  },
  {
    "rank": 3,
    "score": 0.016129031777381897,
    "source": "37bb6c85-b083-42a4-b849-890dbf989082",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "37bb6c85-b083-42a4-b849-890dbf989082",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 12,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2959,
      "startIndex": 2640,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.016129032258064516
    }
  },
  {
    "rank": 4,
    "score": 0.01587301678955555,
    "source": "64ea28ee-c330-4d25-ac22-39c926d02747",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "64ea28ee-c330-4d25-ac22-39c926d02747",
    "contentPreview": "请回答： 4.2 优化技巧 明确角色 ：定义 AI 的身份（技术助手、客服等） 提供约束 ：禁止编造、要求引用来源 Few-Shot ：提供示例问答 Chain of Thought ：要求逐步推理 5. 性能优化 5.1 检索优化 混合检索 ：向量检索 + 关键词检索（BM25） 重排序（Rerank） ：二次精排，提高准确率 Query 改写 ：扩展或改写用户问题，提高召回 5.2 缓存策略 问题缓存 ：相同问题直接返回缓存结果 向量缓存 ：缓存常见问题的向量 5.3 评估指标 检索准确率 ：Top-K 是否包含正确答案 答案质量 ：人工评分或自动评估（ROUGE、BLEU） 延迟 ：端到...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 9,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2021,
      "startIndex": 1679,
      "title": "rag-technology-guide.md",
      "retrievalRoutes": [
        "keyword"
      ],
      "rrfScore": 0.015873015873015872
    }
  },
  {
    "rank": 5,
    "score": 0.014705882407724857,
    "source": "2f315107-6b6f-4291-82f9-06bfe39d7dae",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "2f315107-6b6f-4291-82f9-06bfe39d7dae",
    "contentPreview": "信 流量削峰：秒杀系统 系统解耦：订单服务和库存服务 消息可靠性 ： 生产者确认机制 消息持久化 消费者手动 ACK 9. 设计模式 9.1 常用设计模式 单例模式 ：Spring Bean 默认单例 工厂模式 ：BeanFactory 代理模式 ：AOP 模板方法模式 ：JdbcTemplate、RestTemplate 观察者模式 ：Spring 事件监听 策略模式 ：支付方式选择 10. JVM 10.1 内存模型 堆 ：对象实例，GC 主要区域 栈 ：方法调用，局部变量 方法区 ：类信息、常量池 程序计数器 ：当前线程执行的字节码行号 10.2 垃圾回收 GC 算法 ： 标记-清除 复...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 9,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 2831,
      "startIndex": 2444,
      "title": "java-interview-guide.md",
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
[
  {
    "source": "0",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "0",
    "score": 0.016393441706895828,
    "snippet": "接口性能优化手段？"
  },
  {
    "source": "b72b7089-fdb9-4b82-8740-d6b5028717ca",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "b72b7089-fdb9-4b82-8740-d6b5028717ca",
    "score": 0.016393441706895828,
    "snippet": "接口性能优化手段？"
  },
  {
    "source": "37bb6c85-b083-42a4-b849-890dbf989082",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "37bb6c85-b083-42a4-b849-890dbf989082",
    "score": 0.016129031777381897,
    "snippet": "\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？"
  }
]
```
- citation validation metadata:
```json
{
  "validCitations": 3,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
  "unsupportedCitationCount": 0
}
```

## No-answer Cases

### no-answer-001 (no_answer)

- id: `no-answer-001`
- type: `no_answer`
- question: Kubernetes 的 livenessProbe 和 readinessProbe 有什么区别？
- expected_sources: `[]`
- expected_contexts.contains: `[]`
- retrieve hit ratio: `0/0`
- first_match_rank: `-`
- answer: 知识库中没有足够信息回答该问题。
- expected_keywords: `[]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 1.0,
    "source": "901d738b-2d63-4b11-9c91-d9aa39fb8860",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "901d738b-2d63-4b11-9c91-d9aa39fb8860",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 0,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 270,
      "startIndex": 0,
      "title": "rag-technology-guide.md"
    }
  },
  {
    "rank": 2,
    "score": 0.7920469045639038,
    "source": "37bb6c85-b083-42a4-b849-890dbf989082",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "37bb6c85-b083-42a4-b849-890dbf989082",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 12,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2959,
      "startIndex": 2640,
      "title": "rag-technology-guide.md"
    }
  },
  {
    "rank": 3,
    "score": 0.4335298240184784,
    "source": "d76c3fc0-ef54-4e0e-84b3-ef38b25607f1",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "d76c3fc0-ef54-4e0e-84b3-ef38b25607f1",
    "contentPreview": "文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈 关键指标 ： 检索召回率：Top-K 包含正确答案的比例 答案准确率：生成答案的正确性 用户满意度：...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 13,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 3227,
      "startIndex": 2880,
      "title": "rag-technology-guide.md"
    }
  },
  {
    "rank": 4,
    "score": 0.33610081672668457,
    "source": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 4,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1193,
      "startIndex": 961,
      "title": "rag-technology-guide.md"
    }
  },
  {
    "rank": 5,
    "score": 0.2595656216144562,
    "source": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "8d9e62d7-8f5e-4284-bf6e-c50a1fab03aa",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 5,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1504,
      "startIndex": 1121,
      "title": "rag-technology-guide.md"
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
  "validCitations": 0,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
    "source": "bba6e34a-4290-4aba-ab90-676e863fd5e8",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "bba6e34a-4290-4aba-ab90-676e863fd5e8",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 1,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 438,
      "startIndex": 18,
      "title": "java-interview-guide.md"
    }
  },
  {
    "rank": 2,
    "score": 0.655103325843811,
    "source": "926499a3-c300-4daf-a598-e80534fee04c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "926499a3-c300-4daf-a598-e80534fee04c",
    "contentPreview": "G vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 Top-K ：检索更多候选 使用 Rerank ：二次精排...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 10,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 2333,
      "startIndex": 2000,
      "title": "rag-technology-guide.md"
    }
  },
  {
    "rank": 3,
    "score": 0.6072593927383423,
    "source": "8652a10b-44b4-49ce-b978-21f27c87b785",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "8652a10b-44b4-49ce-b978-21f27c87b785",
    "contentPreview": "线程池 ThreadPoolExecutor ： 核心线程数 corePoolSize 最大线程数 maximumPoolSize 任务队列 workQueue 拒绝策略 RejectedExecutionHandler synchronized vs Lock ： synchronized 自动释放锁，Lock 需要手动释放 Lock 支持更灵活的锁机制（可中断、可超时） volatile 关键字 ： 保证可见性：一个线程修改，其他线程立即可见 禁止指令重排序 不保证原子性 2.",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 2,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 602,
      "startIndex": 358,
      "title": "java-interview-guide.md"
    }
  },
  {
    "rank": 4,
    "score": 0.34844863414764404,
    "source": "d770ae16-5ca7-4ad8-b9f9-c42a8a31d225",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "d770ae16-5ca7-4ad8-b9f9-c42a8a31d225",
    "contentPreview": "Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.52, ..., 0.91] (相似度高) \"今天天气很好\" → [-0.78,...",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 3,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1022,
      "startIndex": 641,
      "title": "rag-technology-guide.md"
    }
  },
  {
    "rank": 5,
    "score": 0.24985453486442566,
    "source": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "4b007ce2-dd75-47c1-acd3-e154ef86c7fc",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 4,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 1193,
      "startIndex": 961,
      "title": "rag-technology-guide.md"
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
  "validCitations": 0,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
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
    "source": "4ba6b2a6-5323-4fb4-8d04-d9e0c83479a6",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "4ba6b2a6-5323-4fb4-8d04-d9e0c83479a6",
    "contentPreview": "Spring Boot 核心知识点 1. Spring Boot 简介 Spring Boot 是基于 Spring 框架的快速开发脚手架，旨在简化 Spring 应用的初始搭建和开发过程。 1.1 核心特性 自动配置 ：根据 classpath 下的依赖自动配置 Spring 应用 起步依赖 ：一站式依赖管理，避免版本冲突 内嵌服务器 ：Tomcat、Jetty、Undertow 生产就绪 ：Actuator 健康检查、监控指标 无代码生成 ：不需要 XML 配置 2.",
    "metadata": {
      "kbId": 11,
      "fileName": "springboot-basics.md",
      "chunkIndex": 0,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "springboot-basics.md",
      "documentId": 35,
      "endIndex": 239,
      "startIndex": 0,
      "title": "springboot-basics.md"
    }
  },
  {
    "rank": 2,
    "score": 0.8596554398536682,
    "source": "bba6e34a-4290-4aba-ab90-676e863fd5e8",
    "sourceFileName": "java-interview-guide.md",
    "documentTitle": "java-interview-guide.md",
    "documentId": 36,
    "chunkId": "bba6e34a-4290-4aba-ab90-676e863fd5e8",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "kbId": 11,
      "fileName": "java-interview-guide.md",
      "chunkIndex": 1,
      "sourceFileName": "java-interview-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "java-interview-guide.md",
      "documentId": 36,
      "endIndex": 438,
      "startIndex": 18,
      "title": "java-interview-guide.md"
    }
  },
  {
    "rank": 3,
    "score": 0.5597094297409058,
    "source": "cb5551dd-5702-48d4-8669-fbc9bc0a4577",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "cb5551dd-5702-48d4-8669-fbc9bc0a4577",
    "contentPreview": "ationPolicy(SessionCreationPolicy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3, max...",
    "metadata": {
      "kbId": 11,
      "fileName": "springboot-basics.md",
      "chunkIndex": 13,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "springboot-basics.md",
      "documentId": 35,
      "endIndex": 3440,
      "startIndex": 3068,
      "title": "springboot-basics.md"
    }
  },
  {
    "rank": 4,
    "score": 0.5052065849304199,
    "source": "901d738b-2d63-4b11-9c91-d9aa39fb8860",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "901d738b-2d63-4b11-9c91-d9aa39fb8860",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "kbId": 11,
      "fileName": "rag-technology-guide.md",
      "chunkIndex": 0,
      "sourceFileName": "rag-technology-guide.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 37,
      "endIndex": 270,
      "startIndex": 0,
      "title": "rag-technology-guide.md"
    }
  },
  {
    "rank": 5,
    "score": 0.5152035355567932,
    "source": "77fe6cff-1436-4309-86ec-befeb8d0bcab",
    "sourceFileName": "springboot-basics.md",
    "documentTitle": "springboot-basics.md",
    "documentId": 35,
    "chunkId": "77fe6cff-1436-4309-86ec-befeb8d0bcab",
    "contentPreview": "@Transactional(rollbackFor = Exception.class) public void createOrder(Order order) { orderRepository.save(order); // 如果发生异常，自动回滚 stockService.deduct(order.getProductId(), order.getQuantity()); } } 9.2 事务传播行为 REQUIRED （默认）：加入当前事务，没有则新建 REQUIRES_NEW ：总是新建事务 SUPPORTS ：有事务就加入，没有就非事务执行 NOT_SUPPORTED ：非事务...",
    "metadata": {
      "kbId": 11,
      "fileName": "springboot-basics.md",
      "chunkIndex": 16,
      "sourceFileName": "springboot-basics.md",
      "vectorCollection": "kb_2addbb37622c42cb",
      "documentTitle": "springboot-basics.md",
      "documentId": 35,
      "endIndex": 4121,
      "startIndex": 3754,
      "title": "springboot-basics.md"
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
  "validCitations": 0,
  "droppedCitations": 0,
  "citationCoverage": 1.0,
  "unsupportedCitationCount": 0
}
```

## Source Normalization Diagnostics

| ID | Expected normalized | Retrieved candidate normalized | Citation candidate normalized |
|---|---|---|---|
| fact-001 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: 4ba6b2a6 5323 4fb4 8d04 d9e0c83479a6, 4ba6b2a653234fb48d04d9e0c83479a6, springboot basics, springbootbasics; r3: 9eea1fbd f499 452c bbb0 de6ee1de6247, 9eea1fbdf499452cbbb0de6ee1de6247, springboot basics, springbootbasics; r4: 0a027708 629e 408e 87ff e2a96c1fccd8, 0a027708629e408e87ffe2a96c1fccd8, java interview guide, javainterviewguide; r5: 4c61d28b 7f57 4679 bcc4 3d9aca5dbe61, 4c61d28b7f574679bcc43d9aca5dbe61, java interview guide, javainterviewguide | 0, springboot basics, springbootbasics; 4ba6b2a6 5323 4fb4 8d04 d9e0c83479a6, 4ba6b2a653234fb48d04d9e0c83479a6, springboot basics, springbootbasics |
| fact-002 | springboot-basics.md => springboot basics, springbootbasics; java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, springboot basics, springbootbasics; r2: dc28958f 0290 4f35 b857 3714067493df, dc28958f02904f35b8573714067493df, springboot basics, springbootbasics; r3: 0a027708 629e 408e 87ff e2a96c1fccd8, 0a027708629e408e87ffe2a96c1fccd8, java interview guide, javainterviewguide; r4: 476434c8 561b 489c 8296 1aad6d714562, 476434c8561b489c82961aad6d714562, springboot basics, springbootbasics; r5: 3152a551 d10b 4aaa 8717 3e79c89987e4, 3152a551d10b4aaa87173e79c89987e4, springboot basics, springbootbasics | 0, springboot basics, springbootbasics; dc28958f 0290 4f35 b857 3714067493df, dc28958f02904f35b8573714067493df, springboot basics, springbootbasics |
| fact-003 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: dc28958f 0290 4f35 b857 3714067493df, dc28958f02904f35b8573714067493df, springboot basics, springbootbasics; r3: cb5551dd 5702 48d4 8669 fbc9bc0a4577, cb5551dd570248d48669fbc9bc0a4577, springboot basics, springbootbasics; r4: 476434c8 561b 489c 8296 1aad6d714562, 476434c8561b489c82961aad6d714562, springboot basics, springbootbasics; r5: 3152a551 d10b 4aaa 8717 3e79c89987e4, 3152a551d10b4aaa87173e79c89987e4, springboot basics, springbootbasics | 0, springboot basics, springbootbasics; dc28958f 0290 4f35 b857 3714067493df, dc28958f02904f35b8573714067493df, springboot basics, springbootbasics; cb5551dd 5702 48d4 8669 fbc9bc0a4577, cb5551dd570248d48669fbc9bc0a4577, springboot basics, springbootbasics |
| fact-004 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: aec9e5d0 9648 4cd0 a7ae ccdfa22fbd1e, aec9e5d096484cd0a7aeccdfa22fbd1e, springboot basics, springbootbasics; r3: 4ba6b2a6 5323 4fb4 8d04 d9e0c83479a6, 4ba6b2a653234fb48d04d9e0c83479a6, springboot basics, springbootbasics; r4: 0a027708 629e 408e 87ff e2a96c1fccd8, 0a027708629e408e87ffe2a96c1fccd8, java interview guide, javainterviewguide; r5: 37bb6c85 b083 42a4 b849 890dbf989082, 37bb6c85b08342a4b849890dbf989082, rag technology guide, ragtechnologyguide | 4ba6b2a6 5323 4fb4 8d04 d9e0c83479a6, 4ba6b2a653234fb48d04d9e0c83479a6, springboot basics, springbootbasics |
| fact-005 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: cb5551dd 5702 48d4 8669 fbc9bc0a4577, cb5551dd570248d48669fbc9bc0a4577, springboot basics, springbootbasics; r3: 8d9e62d7 8f5e 4284 bf6e c50a1fab03aa, 8d9e62d78f5e4284bf6ec50a1fab03aa, rag technology guide, ragtechnologyguide; r4: 37bb6c85 b083 42a4 b849 890dbf989082, 37bb6c85b08342a4b849890dbf989082, rag technology guide, ragtechnologyguide; r5: 901d738b 2d63 4b11 9c91 d9aa39fb8860, 901d738b2d634b119c91d9aa39fb8860, rag technology guide, ragtechnologyguide | 0, springboot basics, springbootbasics; cb5551dd 5702 48d4 8669 fbc9bc0a4577, cb5551dd570248d48669fbc9bc0a4577, springboot basics, springbootbasics; 37bb6c85 b083 42a4 b849 890dbf989082, 37bb6c85b08342a4b849890dbf989082, rag technology guide, ragtechnologyguide |
| fact-006 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: bba6e34a 4290 4aba ab90 676e863fd5e8, bba6e34a42904abaab90676e863fd5e8, java interview guide, javainterviewguide; r3: 0a027708 629e 408e 87ff e2a96c1fccd8, 0a027708629e408e87ffe2a96c1fccd8, java interview guide, javainterviewguide; r4: 68aa7ca8 7a12 4410 b8f5 94d664c6e0e0, 68aa7ca87a124410b8f594d664c6e0e0, java interview guide, javainterviewguide; r5: 0569c9c9 aa04 49fc add5 f3913b7a5717, 0569c9c9aa0449fcadd5f3913b7a5717, java interview guide, javainterviewguide | 0, java interview guide, javainterviewguide; bba6e34a 4290 4aba ab90 676e863fd5e8, bba6e34a42904abaab90676e863fd5e8, java interview guide, javainterviewguide; 0a027708 629e 408e 87ff e2a96c1fccd8, 0a027708629e408e87ffe2a96c1fccd8, java interview guide, javainterviewguide |
| fact-007 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 41240cb9 807f 4839 907a c4385036f132, 41240cb9807f4839907ac4385036f132, java interview guide, javainterviewguide; r3: b72b7089 fdb9 4b82 8740 d6b5028717ca, b72b7089fdb94b828740d6b5028717ca, java interview guide, javainterviewguide; r4: e2b974e9 c87e 45b3 879b ffb23565e898, e2b974e9c87e45b3879bffb23565e898, springboot basics, springbootbasics; r5: 0569c9c9 aa04 49fc add5 f3913b7a5717, 0569c9c9aa0449fcadd5f3913b7a5717, java interview guide, javainterviewguide | 0, java interview guide, javainterviewguide; 41240cb9 807f 4839 907a c4385036f132, 41240cb9807f4839907ac4385036f132, java interview guide, javainterviewguide; b72b7089 fdb9 4b82 8740 d6b5028717ca, b72b7089fdb94b828740d6b5028717ca, java interview guide, javainterviewguide |
| fact-008 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 77017a1a 4e1b 4092 bc23 8433fb9447f4, 77017a1a4e1b4092bc238433fb9447f4, rag technology guide, ragtechnologyguide; r2: 926499a3 c300 4daf a598 e80534fee04c, 926499a3c3004dafa598e80534fee04c, rag technology guide, ragtechnologyguide; r3: 0, rag technology guide, ragtechnologyguide; r4: 4b007ce2 dd75 47c1 acd3 e154ef86c7fc, 4b007ce2dd7547c1acd3e154ef86c7fc, rag technology guide, ragtechnologyguide; r5: d770ae16 5ca7 4ad8 b9f9 c42a8a31d225, d770ae165ca74ad8b9f9c42a8a31d225, rag technology guide, ragtechnologyguide | 77017a1a 4e1b 4092 bc23 8433fb9447f4, 77017a1a4e1b4092bc238433fb9447f4, rag technology guide, ragtechnologyguide; 0, rag technology guide, ragtechnologyguide; 4b007ce2 dd75 47c1 acd3 e154ef86c7fc, 4b007ce2dd7547c1acd3e154ef86c7fc, rag technology guide, ragtechnologyguide |
| fact-009 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 77017a1a 4e1b 4092 bc23 8433fb9447f4, 77017a1a4e1b4092bc238433fb9447f4, rag technology guide, ragtechnologyguide; r3: d76c3fc0 ef54 4e0e 84b3 ef38b25607f1, d76c3fc0ef544e0e84b3ef38b25607f1, rag technology guide, ragtechnologyguide; r4: 64ea28ee c330 4d25 ac22 39c926d02747, 64ea28eec3304d25ac2239c926d02747, rag technology guide, ragtechnologyguide; r5: 5693a68b 544e 49e1 8f08 22964ecb0e84, 5693a68b544e49e18f0822964ecb0e84, springboot basics, springbootbasics | 0, rag technology guide, ragtechnologyguide; 77017a1a 4e1b 4092 bc23 8433fb9447f4, 77017a1a4e1b4092bc238433fb9447f4, rag technology guide, ragtechnologyguide; d76c3fc0 ef54 4e0e 84b3 ef38b25607f1, d76c3fc0ef544e0e84b3ef38b25607f1, rag technology guide, ragtechnologyguide |
| fact-010 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 0569c9c9 aa04 49fc add5 f3913b7a5717, 0569c9c9aa0449fcadd5f3913b7a5717, java interview guide, javainterviewguide; r3: 4b007ce2 dd75 47c1 acd3 e154ef86c7fc, 4b007ce2dd7547c1acd3e154ef86c7fc, rag technology guide, ragtechnologyguide; r4: 8d9e62d7 8f5e 4284 bf6e c50a1fab03aa, 8d9e62d78f5e4284bf6ec50a1fab03aa, rag technology guide, ragtechnologyguide; r5: b72b7089 fdb9 4b82 8740 d6b5028717ca, b72b7089fdb94b828740d6b5028717ca, java interview guide, javainterviewguide |  |
| definition-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 901d738b 2d63 4b11 9c91 d9aa39fb8860, 901d738b2d634b119c91d9aa39fb8860, rag technology guide, ragtechnologyguide; r3: 8d9e62d7 8f5e 4284 bf6e c50a1fab03aa, 8d9e62d78f5e4284bf6ec50a1fab03aa, rag technology guide, ragtechnologyguide; r4: 926499a3 c300 4daf a598 e80534fee04c, 926499a3c3004dafa598e80534fee04c, rag technology guide, ragtechnologyguide; r5: 2fc312fe bd9e 45a6 bfdf 29d93b357ea5, 2fc312febd9e45a6bfdf29d93b357ea5, rag technology guide, ragtechnologyguide | 0, rag technology guide, ragtechnologyguide; 901d738b 2d63 4b11 9c91 d9aa39fb8860, 901d738b2d634b119c91d9aa39fb8860, rag technology guide, ragtechnologyguide |
| definition-002 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: d770ae16 5ca7 4ad8 b9f9 c42a8a31d225, d770ae165ca74ad8b9f9c42a8a31d225, rag technology guide, ragtechnologyguide; r3: 37bb6c85 b083 42a4 b849 890dbf989082, 37bb6c85b08342a4b849890dbf989082, rag technology guide, ragtechnologyguide; r4: aa8385cf 16ff 4fdd a71f 289acf0244e6, aa8385cf16ff4fdda71f289acf0244e6, rag technology guide, ragtechnologyguide; r5: 926499a3 c300 4daf a598 e80534fee04c, 926499a3c3004dafa598e80534fee04c, rag technology guide, ragtechnologyguide | 0, rag technology guide, ragtechnologyguide; d770ae16 5ca7 4ad8 b9f9 c42a8a31d225, d770ae165ca74ad8b9f9c42a8a31d225, rag technology guide, ragtechnologyguide; 37bb6c85 b083 42a4 b849 890dbf989082, 37bb6c85b08342a4b849890dbf989082, rag technology guide, ragtechnologyguide |
| definition-003 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 4b007ce2 dd75 47c1 acd3 e154ef86c7fc, 4b007ce2dd7547c1acd3e154ef86c7fc, rag technology guide, ragtechnologyguide; r3: d770ae16 5ca7 4ad8 b9f9 c42a8a31d225, d770ae165ca74ad8b9f9c42a8a31d225, rag technology guide, ragtechnologyguide; r4: 37bb6c85 b083 42a4 b849 890dbf989082, 37bb6c85b08342a4b849890dbf989082, rag technology guide, ragtechnologyguide; r5: 77017a1a 4e1b 4092 bc23 8433fb9447f4, 77017a1a4e1b4092bc238433fb9447f4, rag technology guide, ragtechnologyguide |  |
| definition-004 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 8d9e62d7 8f5e 4284 bf6e c50a1fab03aa, 8d9e62d78f5e4284bf6ec50a1fab03aa, rag technology guide, ragtechnologyguide; r2: 4b007ce2 dd75 47c1 acd3 e154ef86c7fc, 4b007ce2dd7547c1acd3e154ef86c7fc, rag technology guide, ragtechnologyguide; r3: 77017a1a 4e1b 4092 bc23 8433fb9447f4, 77017a1a4e1b4092bc238433fb9447f4, rag technology guide, ragtechnologyguide; r4: 37bb6c85 b083 42a4 b849 890dbf989082, 37bb6c85b08342a4b849890dbf989082, rag technology guide, ragtechnologyguide; r5: 0, rag technology guide, ragtechnologyguide | 8d9e62d7 8f5e 4284 bf6e c50a1fab03aa, 8d9e62d78f5e4284bf6ec50a1fab03aa, rag technology guide, ragtechnologyguide; 0, rag technology guide, ragtechnologyguide; 4b007ce2 dd75 47c1 acd3 e154ef86c7fc, 4b007ce2dd7547c1acd3e154ef86c7fc, rag technology guide, ragtechnologyguide |
| definition-005 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 68aa7ca8 7a12 4410 b8f5 94d664c6e0e0, 68aa7ca87a124410b8f594d664c6e0e0, java interview guide, javainterviewguide; r3: 37bb6c85 b083 42a4 b849 890dbf989082, 37bb6c85b08342a4b849890dbf989082, rag technology guide, ragtechnologyguide; r4: 4ba6b2a6 5323 4fb4 8d04 d9e0c83479a6, 4ba6b2a653234fb48d04d9e0c83479a6, springboot basics, springbootbasics; r5: 9eea1fbd f499 452c bbb0 de6ee1de6247, 9eea1fbdf499452cbbb0de6ee1de6247, springboot basics, springbootbasics | 0, java interview guide, javainterviewguide; 68aa7ca8 7a12 4410 b8f5 94d664c6e0e0, 68aa7ca87a124410b8f594d664c6e0e0, java interview guide, javainterviewguide; 37bb6c85 b083 42a4 b849 890dbf989082, 37bb6c85b08342a4b849890dbf989082, rag technology guide, ragtechnologyguide |
| definition-006 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 68aa7ca8 7a12 4410 b8f5 94d664c6e0e0, 68aa7ca87a124410b8f594d664c6e0e0, java interview guide, javainterviewguide; r3: 8d9e62d7 8f5e 4284 bf6e c50a1fab03aa, 8d9e62d78f5e4284bf6ec50a1fab03aa, rag technology guide, ragtechnologyguide; r4: 4b007ce2 dd75 47c1 acd3 e154ef86c7fc, 4b007ce2dd7547c1acd3e154ef86c7fc, rag technology guide, ragtechnologyguide; r5: 2f315107 6b6f 4291 82f9 06bfe39d7dae, 2f3151076b6f429182f906bfe39d7dae, java interview guide, javainterviewguide | 0, java interview guide, javainterviewguide; 68aa7ca8 7a12 4410 b8f5 94d664c6e0e0, 68aa7ca87a124410b8f594d664c6e0e0, java interview guide, javainterviewguide; 8d9e62d7 8f5e 4284 bf6e c50a1fab03aa, 8d9e62d78f5e4284bf6ec50a1fab03aa, rag technology guide, ragtechnologyguide |
| definition-007 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 41240cb9 807f 4839 907a c4385036f132, 41240cb9807f4839907ac4385036f132, java interview guide, javainterviewguide; r3: e2b974e9 c87e 45b3 879b ffb23565e898, e2b974e9c87e45b3879bffb23565e898, springboot basics, springbootbasics; r4: fceb5000 22c6 4337 bf1f 2d83c5e7ff33, fceb500022c64337bf1f2d83c5e7ff33, springboot basics, springbootbasics; r5: 4c61d28b 7f57 4679 bcc4 3d9aca5dbe61, 4c61d28b7f574679bcc43d9aca5dbe61, java interview guide, javainterviewguide | 0, java interview guide, javainterviewguide; 41240cb9 807f 4839 907a c4385036f132, 41240cb9807f4839907ac4385036f132, java interview guide, javainterviewguide; e2b974e9 c87e 45b3 879b ffb23565e898, e2b974e9c87e45b3879bffb23565e898, springboot basics, springbootbasics |
| definition-008 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: 2e8ca4e9 f04a 4893 af1f 80a0d7e3c7cf, 2e8ca4e9f04a4893af1f80a0d7e3c7cf, java interview guide, javainterviewguide; r3: 41240cb9 807f 4839 907a c4385036f132, 41240cb9807f4839907ac4385036f132, java interview guide, javainterviewguide; r4: 3a85e3c6 95a4 49c7 a925 0653d304b149, 3a85e3c695a449c7a9250653d304b149, rag technology guide, ragtechnologyguide; r5: 996590f5 757b 4c51 a02c 1d1f91faed6e, 996590f5757b4c51a02c1d1f91faed6e, rag technology guide, ragtechnologyguide | 0, java interview guide, javainterviewguide; 2e8ca4e9 f04a 4893 af1f 80a0d7e3c7cf, 2e8ca4e9f04a4893af1f80a0d7e3c7cf, java interview guide, javainterviewguide; 3a85e3c6 95a4 49c7 a925 0653d304b149, 3a85e3c695a449c7a9250653d304b149, rag technology guide, ragtechnologyguide |
| reasoning-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 8d9e62d7 8f5e 4284 bf6e c50a1fab03aa, 8d9e62d78f5e4284bf6ec50a1fab03aa, rag technology guide, ragtechnologyguide; r2: 37bb6c85 b083 42a4 b849 890dbf989082, 37bb6c85b08342a4b849890dbf989082, rag technology guide, ragtechnologyguide; r3: 2fc312fe bd9e 45a6 bfdf 29d93b357ea5, 2fc312febd9e45a6bfdf29d93b357ea5, rag technology guide, ragtechnologyguide; r4: 4b007ce2 dd75 47c1 acd3 e154ef86c7fc, 4b007ce2dd7547c1acd3e154ef86c7fc, rag technology guide, ragtechnologyguide; r5: 0, rag technology guide, ragtechnologyguide | 8d9e62d7 8f5e 4284 bf6e c50a1fab03aa, 8d9e62d78f5e4284bf6ec50a1fab03aa, rag technology guide, ragtechnologyguide; 0, rag technology guide, ragtechnologyguide; 2fc312fe bd9e 45a6 bfdf 29d93b357ea5, 2fc312febd9e45a6bfdf29d93b357ea5, rag technology guide, ragtechnologyguide |
| reasoning-002 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 926499a3 c300 4daf a598 e80534fee04c, 926499a3c3004dafa598e80534fee04c, rag technology guide, ragtechnologyguide; r3: 901d738b 2d63 4b11 9c91 d9aa39fb8860, 901d738b2d634b119c91d9aa39fb8860, rag technology guide, ragtechnologyguide; r4: 8d9e62d7 8f5e 4284 bf6e c50a1fab03aa, 8d9e62d78f5e4284bf6ec50a1fab03aa, rag technology guide, ragtechnologyguide; r5: 8b5df374 b365 41d3 8322 579cc48c0757, 8b5df374b36541d38322579cc48c0757, rag technology guide, ragtechnologyguide | 0, rag technology guide, ragtechnologyguide; 926499a3 c300 4daf a598 e80534fee04c, 926499a3c3004dafa598e80534fee04c, rag technology guide, ragtechnologyguide; 901d738b 2d63 4b11 9c91 d9aa39fb8860, 901d738b2d634b119c91d9aa39fb8860, rag technology guide, ragtechnologyguide |
| reasoning-003 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 8d9e62d7 8f5e 4284 bf6e c50a1fab03aa, 8d9e62d78f5e4284bf6ec50a1fab03aa, rag technology guide, ragtechnologyguide; r2: 4b007ce2 dd75 47c1 acd3 e154ef86c7fc, 4b007ce2dd7547c1acd3e154ef86c7fc, rag technology guide, ragtechnologyguide; r3: 37bb6c85 b083 42a4 b849 890dbf989082, 37bb6c85b08342a4b849890dbf989082, rag technology guide, ragtechnologyguide; r4: 926499a3 c300 4daf a598 e80534fee04c, 926499a3c3004dafa598e80534fee04c, rag technology guide, ragtechnologyguide; r5: 0, rag technology guide, ragtechnologyguide | 926499a3 c300 4daf a598 e80534fee04c, 926499a3c3004dafa598e80534fee04c, rag technology guide, ragtechnologyguide; 0, rag technology guide, ragtechnologyguide; 8d9e62d7 8f5e 4284 bf6e c50a1fab03aa, 8d9e62d78f5e4284bf6ec50a1fab03aa, rag technology guide, ragtechnologyguide |
| reasoning-004 | springboot-basics.md => springboot basics, springbootbasics; java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: aec9e5d0 9648 4cd0 a7ae ccdfa22fbd1e, aec9e5d096484cd0a7aeccdfa22fbd1e, springboot basics, springbootbasics; r3: 68aa7ca8 7a12 4410 b8f5 94d664c6e0e0, 68aa7ca87a124410b8f594d664c6e0e0, java interview guide, javainterviewguide; r4: 37bb6c85 b083 42a4 b849 890dbf989082, 37bb6c85b08342a4b849890dbf989082, rag technology guide, ragtechnologyguide; r5: 4b007ce2 dd75 47c1 acd3 e154ef86c7fc, 4b007ce2dd7547c1acd3e154ef86c7fc, rag technology guide, ragtechnologyguide | 0, java interview guide, javainterviewguide; aec9e5d0 9648 4cd0 a7ae ccdfa22fbd1e, aec9e5d096484cd0a7aeccdfa22fbd1e, springboot basics, springbootbasics; 68aa7ca8 7a12 4410 b8f5 94d664c6e0e0, 68aa7ca87a124410b8f594d664c6e0e0, java interview guide, javainterviewguide |
| reasoning-005 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide; r2: bba6e34a 4290 4aba ab90 676e863fd5e8, bba6e34a42904abaab90676e863fd5e8, java interview guide, javainterviewguide; r3: 4b007ce2 dd75 47c1 acd3 e154ef86c7fc, 4b007ce2dd7547c1acd3e154ef86c7fc, rag technology guide, ragtechnologyguide; r4: 8d9e62d7 8f5e 4284 bf6e c50a1fab03aa, 8d9e62d78f5e4284bf6ec50a1fab03aa, rag technology guide, ragtechnologyguide; r5: 37bb6c85 b083 42a4 b849 890dbf989082, 37bb6c85b08342a4b849890dbf989082, rag technology guide, ragtechnologyguide | 0, java interview guide, javainterviewguide; bba6e34a 4290 4aba ab90 676e863fd5e8, bba6e34a42904abaab90676e863fd5e8, java interview guide, javainterviewguide; 4b007ce2 dd75 47c1 acd3 e154ef86c7fc, 4b007ce2dd7547c1acd3e154ef86c7fc, rag technology guide, ragtechnologyguide |
| reasoning-006 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 4b007ce2 dd75 47c1 acd3 e154ef86c7fc, 4b007ce2dd7547c1acd3e154ef86c7fc, rag technology guide, ragtechnologyguide; r2: 8d9e62d7 8f5e 4284 bf6e c50a1fab03aa, 8d9e62d78f5e4284bf6ec50a1fab03aa, rag technology guide, ragtechnologyguide; r3: 37bb6c85 b083 42a4 b849 890dbf989082, 37bb6c85b08342a4b849890dbf989082, rag technology guide, ragtechnologyguide; r4: 926499a3 c300 4daf a598 e80534fee04c, 926499a3c3004dafa598e80534fee04c, rag technology guide, ragtechnologyguide; r5: 996590f5 757b 4c51 a02c 1d1f91faed6e, 996590f5757b4c51a02c1d1f91faed6e, rag technology guide, ragtechnologyguide |  |
| multi-hop-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 77017a1a 4e1b 4092 bc23 8433fb9447f4, 77017a1a4e1b4092bc238433fb9447f4, rag technology guide, ragtechnologyguide; r3: d76c3fc0 ef54 4e0e 84b3 ef38b25607f1, d76c3fc0ef544e0e84b3ef38b25607f1, rag technology guide, ragtechnologyguide; r4: 37bb6c85 b083 42a4 b849 890dbf989082, 37bb6c85b08342a4b849890dbf989082, rag technology guide, ragtechnologyguide; r5: 8b5df374 b365 41d3 8322 579cc48c0757, 8b5df374b36541d38322579cc48c0757, rag technology guide, ragtechnologyguide | 0, rag technology guide, ragtechnologyguide; 77017a1a 4e1b 4092 bc23 8433fb9447f4, 77017a1a4e1b4092bc238433fb9447f4, rag technology guide, ragtechnologyguide; d76c3fc0 ef54 4e0e 84b3 ef38b25607f1, d76c3fc0ef544e0e84b3ef38b25607f1, rag technology guide, ragtechnologyguide |
| multi-hop-002 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 926499a3 c300 4daf a598 e80534fee04c, 926499a3c3004dafa598e80534fee04c, rag technology guide, ragtechnologyguide; r3: 64ea28ee c330 4d25 ac22 39c926d02747, 64ea28eec3304d25ac2239c926d02747, rag technology guide, ragtechnologyguide; r4: d76c3fc0 ef54 4e0e 84b3 ef38b25607f1, d76c3fc0ef544e0e84b3ef38b25607f1, rag technology guide, ragtechnologyguide; r5: 8d9e62d7 8f5e 4284 bf6e c50a1fab03aa, 8d9e62d78f5e4284bf6ec50a1fab03aa, rag technology guide, ragtechnologyguide | 0, rag technology guide, ragtechnologyguide; 926499a3 c300 4daf a598 e80534fee04c, 926499a3c3004dafa598e80534fee04c, rag technology guide, ragtechnologyguide; 64ea28ee c330 4d25 ac22 39c926d02747, 64ea28eec3304d25ac2239c926d02747, rag technology guide, ragtechnologyguide |
| multi-hop-003 | java-interview-guide.md => java interview guide, javainterviewguide; springboot-basics.md => springboot basics, springbootbasics | r1: 0, java interview guide, javainterviewguide; r2: b72b7089 fdb9 4b82 8740 d6b5028717ca, b72b7089fdb94b828740d6b5028717ca, java interview guide, javainterviewguide; r3: 37bb6c85 b083 42a4 b849 890dbf989082, 37bb6c85b08342a4b849890dbf989082, rag technology guide, ragtechnologyguide; r4: 64ea28ee c330 4d25 ac22 39c926d02747, 64ea28eec3304d25ac2239c926d02747, rag technology guide, ragtechnologyguide; r5: 2f315107 6b6f 4291 82f9 06bfe39d7dae, 2f3151076b6f429182f906bfe39d7dae, java interview guide, javainterviewguide | 0, java interview guide, javainterviewguide; b72b7089 fdb9 4b82 8740 d6b5028717ca, b72b7089fdb94b828740d6b5028717ca, java interview guide, javainterviewguide; 37bb6c85 b083 42a4 b849 890dbf989082, 37bb6c85b08342a4b849890dbf989082, rag technology guide, ragtechnologyguide |
| no-answer-001 |  | r1: 901d738b 2d63 4b11 9c91 d9aa39fb8860, 901d738b2d634b119c91d9aa39fb8860, rag technology guide, ragtechnologyguide; r2: 37bb6c85 b083 42a4 b849 890dbf989082, 37bb6c85b08342a4b849890dbf989082, rag technology guide, ragtechnologyguide; r3: d76c3fc0 ef54 4e0e 84b3 ef38b25607f1, d76c3fc0ef544e0e84b3ef38b25607f1, rag technology guide, ragtechnologyguide; r4: 4b007ce2 dd75 47c1 acd3 e154ef86c7fc, 4b007ce2dd7547c1acd3e154ef86c7fc, rag technology guide, ragtechnologyguide; r5: 8d9e62d7 8f5e 4284 bf6e c50a1fab03aa, 8d9e62d78f5e4284bf6ec50a1fab03aa, rag technology guide, ragtechnologyguide |  |
| no-answer-002 |  | r1: bba6e34a 4290 4aba ab90 676e863fd5e8, bba6e34a42904abaab90676e863fd5e8, java interview guide, javainterviewguide; r2: 926499a3 c300 4daf a598 e80534fee04c, 926499a3c3004dafa598e80534fee04c, rag technology guide, ragtechnologyguide; r3: 8652a10b 44b4 49ce b978 21f27c87b785, 8652a10b44b449ceb97821f27c87b785, java interview guide, javainterviewguide; r4: d770ae16 5ca7 4ad8 b9f9 c42a8a31d225, d770ae165ca74ad8b9f9c42a8a31d225, rag technology guide, ragtechnologyguide; r5: 4b007ce2 dd75 47c1 acd3 e154ef86c7fc, 4b007ce2dd7547c1acd3e154ef86c7fc, rag technology guide, ragtechnologyguide |  |
| no-answer-003 |  | r1: 4ba6b2a6 5323 4fb4 8d04 d9e0c83479a6, 4ba6b2a653234fb48d04d9e0c83479a6, springboot basics, springbootbasics; r2: bba6e34a 4290 4aba ab90 676e863fd5e8, bba6e34a42904abaab90676e863fd5e8, java interview guide, javainterviewguide; r3: cb5551dd 5702 48d4 8669 fbc9bc0a4577, cb5551dd570248d48669fbc9bc0a4577, springboot basics, springbootbasics; r4: 901d738b 2d63 4b11 9c91 d9aa39fb8860, 901d738b2d634b119c91d9aa39fb8860, rag technology guide, ragtechnologyguide; r5: 77fe6cff 1436 4309 86ec befeb8d0bcab, 77fe6cff1436430986ecbefeb8d0bcab, springboot basics, springbootbasics |  |

## Citation Diagnostics

| ID | Returned | Source Hits | Snippet Hits | Validation | Unsupported |
|---|---:|---:|---:|---|---:|
| fact-001 | 2 | 1/1 | 2/2 | valid=2, dropped=0, coverage=1.0 | 0 |
| fact-002 | 2 | 1/2 | 2/2 | valid=2, dropped=0, coverage=1.0 | 0 |
| fact-003 | 3 | 1/1 | 3/3 | valid=3, dropped=0, coverage=1.0 | 0 |
| fact-004 | 1 | 1/1 | 1/1 | valid=1, dropped=0, coverage=1.0 | 0 |
| fact-005 | 3 | 1/1 | 3/3 | valid=3, dropped=0, coverage=1.0 | 0 |
| fact-006 | 3 | 1/1 | 3/3 | valid=3, dropped=0, coverage=1.0 | 0 |
| fact-007 | 3 | 1/1 | 3/3 | valid=3, dropped=0, coverage=1.0 | 0 |
| fact-008 | 3 | 1/1 | 3/3 | valid=3, dropped=0, coverage=1.0 | 0 |
| fact-009 | 3 | 1/1 | 3/3 | valid=3, dropped=0, coverage=1.0 | 0 |
| fact-010 | 0 | 0/1 | - | valid=0, dropped=0, coverage=1.0 | 0 |
| definition-001 | 2 | 1/1 | 2/2 | valid=2, dropped=0, coverage=1.0 | 0 |
| definition-002 | 3 | 1/1 | 3/3 | valid=3, dropped=0, coverage=1.0 | 0 |
| definition-003 | 0 | 0/1 | - | valid=0, dropped=0, coverage=1.0 | 0 |
| definition-004 | 3 | 1/1 | 3/3 | valid=3, dropped=0, coverage=1.0 | 0 |
| definition-005 | 3 | 1/1 | 3/3 | valid=3, dropped=0, coverage=1.0 | 0 |
| definition-006 | 3 | 1/1 | 3/3 | valid=3, dropped=0, coverage=1.0 | 0 |
| definition-007 | 3 | 1/1 | 3/3 | valid=3, dropped=0, coverage=1.0 | 0 |
| definition-008 | 3 | 1/1 | 3/3 | valid=3, dropped=0, coverage=1.0 | 0 |
| reasoning-001 | 3 | 1/1 | 3/3 | valid=3, dropped=0, coverage=1.0 | 0 |
| reasoning-002 | 3 | 1/1 | 3/3 | valid=3, dropped=0, coverage=1.0 | 0 |
| reasoning-003 | 3 | 1/1 | 3/3 | valid=3, dropped=0, coverage=1.0 | 0 |
| reasoning-004 | 3 | 2/2 | 3/3 | valid=3, dropped=0, coverage=1.0 | 0 |
| reasoning-005 | 3 | 1/1 | 3/3 | valid=3, dropped=0, coverage=1.0 | 0 |
| reasoning-006 | 0 | 0/1 | - | valid=0, dropped=0, coverage=1.0 | 0 |
| multi-hop-001 | 3 | 1/1 | 3/3 | valid=3, dropped=0, coverage=1.0 | 0 |
| multi-hop-002 | 3 | 1/1 | 3/3 | valid=3, dropped=0, coverage=1.0 | 0 |
| multi-hop-003 | 3 | 1/2 | 3/3 | valid=3, dropped=0, coverage=1.0 | 0 |
| no-answer-001 | 0 | - | - | valid=0, dropped=0, coverage=1.0 | 0 |
| no-answer-002 | 0 | - | - | valid=0, dropped=0, coverage=1.0 | 0 |
| no-answer-003 | 0 | - | - | valid=0, dropped=0, coverage=1.0 | 0 |
