# RAG Eval Report

- Generated at: 2026-06-03T09:08:52.623826+00:00
- Base URL: `http://localhost:8080`
- Knowledge base ID: `6`
- Eval set: `docs\eval\rag_eval_set.jsonl`
- topK: `5`
- minScore: `0.3`
- enableRerank: `True`
- Duration: `222.98s`

## Summary Metrics

| Metric | Value |
|---|---:|
| Samples | 30 |
| Answerable samples | 27 |
| No-answer samples | 3 |
| Recall@3 | 54.90% |
| Recall@5 | 54.90% |
| MRR | 0.6667 |
| Top1 source accuracy | 100.00% |
| Answer keyword hit rate | 48.08% |
| Citation hit rate | 0.00% |
| Citation source hit rate | 0.00% |
| Citation snippet hit rate | 0.00% |
| Unsupported citation count | 0 |
| No-answer citation violation count | 0 |
| No-answer accuracy | 100.00% |

## Sample Results

| ID | Type | Retrieve | First Match | Keyword Hit | Citation Source | Citation Snippet | Unsupported Citations | No-answer OK | Errors |
|---|---|---:|---:|---:|---:|---:|---:|---:|---|
| fact-001 | fact | 2/2 | 1 | 0/4 | 0/1 | - | 0 | - | HTTP 500 http://localhost:8080/api/qa/ask: {"status":500,"errorCode":"SYS_500","message":"系统内部错误，请稍后重试","traceId":"0019e8cbac84e544e36726da6776d0d5","timestamp":"2026-06-03T09:05:21.543473400Z","path":"/api/qa/ask"} |
| fact-002 | fact | 1/2 | 1 | 3/3 | 0/2 | - | 0 | - |  |
| fact-003 | fact | 0/1 | - | 3/3 | 0/1 | - | 0 | - |  |
| fact-004 | fact | 2/2 | 1 | 2/2 | 0/1 | - | 0 | - |  |
| fact-005 | fact | 0/2 | - | 4/4 | 0/1 | - | 0 | - |  |
| fact-006 | fact | 2/2 | 1 | 3/4 | 0/1 | - | 0 | - |  |
| fact-007 | fact | 1/2 | 1 | 5/5 | 0/1 | - | 0 | - |  |
| fact-008 | fact | 0/2 | - | 4/4 | 0/1 | - | 0 | - |  |
| fact-009 | fact | 1/2 | 1 | 4/4 | 0/1 | - | 0 | - |  |
| fact-010 | fact | 0/1 | - | 2/3 | 0/1 | - | 0 | - |  |
| definition-001 | definition | 1/2 | 1 | 0/4 | 0/1 | - | 0 | - | HTTP 500 http://localhost:8080/api/qa/ask: {"status":500,"errorCode":"SYS_500","message":"系统内部错误，请稍后重试","traceId":"0019e8cbb7528061ee682959ffec94a8","timestamp":"2026-06-03T09:05:59.007589400Z","path":"/api/qa/ask"} |
| definition-002 | definition | 2/2 | 1 | 3/3 | 0/1 | - | 0 | - |  |
| definition-003 | definition | 0/2 | - | 0/3 | 0/1 | - | 0 | - | HTTP 500 http://localhost:8080/api/qa/ask: {"status":500,"errorCode":"SYS_500","message":"系统内部错误，请稍后重试","traceId":"0019e8cbb9bce5dabd62a659631dfe6e","timestamp":"2026-06-03T09:06:07.318746800Z","path":"/api/qa/ask"} |
| definition-004 | definition | 0/2 | - | 2/4 | 0/1 | - | 0 | - |  |
| definition-005 | definition | 2/2 | 1 | 0/3 | 0/1 | - | 0 | - | QA returned error status: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试 |
| definition-006 | definition | 2/2 | 1 | 0/4 | 0/1 | - | 0 | - | QA returned error status: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试 |
| definition-007 | definition | 1/2 | 1 | 0/4 | 0/1 | - | 0 | - | QA returned error status: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试 |
| definition-008 | definition | 2/2 | 1 | 0/4 | 0/1 | - | 0 | - | QA returned error status: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试 |
| reasoning-001 | reasoning | 0/2 | - | 0/4 | 0/1 | - | 0 | - | QA returned error status: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试 |
| reasoning-002 | reasoning | 2/2 | 1 | 1/4 | 0/1 | - | 0 | - |  |
| reasoning-003 | reasoning | 0/2 | - | 0/4 | 0/1 | - | 0 | - |  |
| reasoning-004 | reasoning | 1/2 | 1 | 0/4 | 0/2 | - | 0 | - | QA returned error status: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试 |
| reasoning-005 | reasoning | 0/1 | - | 3/3 | 0/1 | - | 0 | - |  |
| reasoning-006 | reasoning | 2/2 | 1 | 3/3 | 0/1 | - | 0 | - |  |
| multi-hop-001 | multi_hop | 2/2 | 1 | 2/7 | 0/1 | - | 0 | - |  |
| multi-hop-002 | multi_hop | 1/2 | 1 | 4/5 | 0/1 | - | 0 | - |  |
| multi-hop-003 | multi_hop | 1/2 | 1 | 2/5 | 0/2 | - | 0 | - |  |
| no-answer-001 | no_answer | - | - | - | - | - | 0 | yes |  |
| no-answer-002 | no_answer | - | - | - | - | - | 0 | yes |  |
| no-answer-003 | no_answer | - | - | - | - | - | 0 | yes |  |

## Field Coverage

- `debug/retrieve` is used for Recall@3, Recall@5, MRR, and Top1 source accuracy.
- `ask` is used for answer keyword hit rate, citation hit rate, and no-answer accuracy.
- `queryVariants`, `rank`, `score`, `source`, `documentId`, `chunkId`, `contentPreview`, and `metadata` are expected in debug output.

## Current Limitations

- `contentPreview` is a preview, not the full chunk, so long expected snippets may undercount recall.
- Citation source hit rate checks expected source names in returned citations.
- Citation snippet hit rate verifies each returned citation against the `contexts` returned by `/api/qa/ask` using exact match or token overlap.
- Answer scoring is keyword based and does not use an LLM judge.
- Metrics assume the three `test-data/*.md` files were uploaded with recognizable file names or document titles.

## Failed Retrieval Cases

### fact-002 (fact)

- id: `fact-002`
- type: `fact`
- question: @SpringBootApplication 由哪些注解组合而成？
- expected_sources: `["springboot-basics.md", "java-interview-guide.md"]`
- expected_contexts.contains: `["@SpringBootApplication 是组合注解", "@SpringBootApplication = @Configuration"]`
- retrieve hit ratio: `1/2`
- first_match_rank: `1`
- answer: @SpringBootApplication 由 @Configuration、@EnableAutoConfiguration 和 @ComponentScan 三个注解组合而成。
- expected_keywords: `["@Configuration", "@EnableAutoConfiguration", "@ComponentScan"]`
- keyword_hit: `3/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.6092399954795837,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "springboot-basics.md",
    "documentId": 31,
    "chunkId": "0",
    "contentPreview": "rtow 生产就绪 ：Actuator 健康检查、监控指标 无代码生成 ：不需要 XML 配置 2. 核心注解 2.1 启动类注解 @SpringBootApplication public class Application { public static void main(String[] args) { SpringApplication.run(Application.class, args); } } @SpringBootApplication 是组合注解，包含： @Configuration ：标记为配置类 @EnableAutoConfiguration ：启用自动配置 @C...",
    "metadata": {
      "chunkIndex": 1.0,
      "kbId": 6.0,
      "startIndex": 189.0,
      "endIndex": 729.0,
      "documentId": 31.0
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

### fact-003 (fact)

- id: `fact-003`
- type: `fact`
- question: @RestController 等价于哪两个注解的组合？
- expected_sources: `["springboot-basics.md"]`
- expected_contexts.contains: `["@RestController：= @Controller + @ResponseBody"]`
- retrieve hit ratio: `0/1`
- first_match_rank: `-`
- answer: @RestController 等价于 @Controller 和 @ResponseBody 的组合。
- expected_keywords: `["@Controller", "@ResponseBody", "@RestController"]`
- keyword_hit: `3/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.49326619505882263,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "springboot-basics.md",
    "documentId": 31,
    "chunkId": "0",
    "contentPreview": "rtow 生产就绪 ：Actuator 健康检查、监控指标 无代码生成 ：不需要 XML 配置 2. 核心注解 2.1 启动类注解 @SpringBootApplication public class Application { public static void main(String[] args) { SpringApplication.run(Application.class, args); } } @SpringBootApplication 是组合注解，包含： @Configuration ：标记为配置类 @EnableAutoConfiguration ：启用自动配置 @C...",
    "metadata": {
      "chunkIndex": 1.0,
      "kbId": 6.0,
      "startIndex": 189.0,
      "endIndex": 729.0,
      "documentId": 31.0
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

### fact-005 (fact)

- id: `fact-005`
- type: `fact`
- question: JWT 认证中 accessToken 和 refreshToken 分别有什么作用？
- expected_sources: `["springboot-basics.md"]`
- expected_contexts.contains: `["accessToken：短期令牌", "refreshToken：长期令牌"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer: 在JWT认证中，accessToken和refreshToken分别有不同的作用。accessToken是一个短期令牌，有效期为1小时，用于访问接口。refreshToken是一个长期令牌，有效期为7天，当accessToken过期时，可以用refreshToken刷新获取新的accessToken。
- expected_keywords: `["accessToken", "refreshToken", "访问接口", "刷新"]`
- keyword_hit: `4/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.5016002058982849,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "springboot-basics.md",
    "documentId": 31,
    "chunkId": "0",
    "contentPreview": "onCreationPolicy(SessionCreationPolicy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3...",
    "metadata": {
      "chunkIndex": 11.0,
      "kbId": 6.0,
      "startIndex": 3063.0,
      "endIndex": 3563.0,
      "documentId": 31.0
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

### fact-007 (fact)

- id: `fact-007`
- type: `fact`
- question: Redis 常见数据类型包括哪些？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["数据类型", "ZSet：排行榜"]`
- retrieve hit ratio: `1/2`
- first_match_rank: `1`
- answer: Redis 常见数据类型包括 String、Hash、List、Set 和 ZSet。
- expected_keywords: `["String", "Hash", "List", "Set", "ZSet"]`
- keyword_hit: `5/5`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.503896951675415,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "IALIZABLE（串行化） 锁机制 ： 行锁：锁定某一行 表锁：锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AO...",
    "metadata": {
      "chunkIndex": 7.0,
      "kbId": 6.0,
      "startIndex": 1871.0,
      "endIndex": 2187.0,
      "documentId": 32.0
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

### fact-008 (fact)

- id: `fact-008`
- type: `fact`
- question: RAG 文档中列出的常用向量数据库有哪些？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["常用数据库", "Qdrant：Rust 实现"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer: RAG 文档中列出的常用向量数据库包括 Qdrant、Weaviate、Milvus 和 Pinecone。其中，小规模向量数据库（< 100 万向量）可选择 Qdrant 或 Weaviate，大规模向量数据库（> 100 万向量）可选择 Milvus 或 Pinecone。此外，Milvus 和 Qdrant 支持本地部署，而 Pinecone 和 Weaviate Cloud 支持云服务。
- expected_keywords: `["Milvus", "Pinecone", "Weaviate", "Qdrant"]`
- keyword_hit: `4/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.4407072067260742,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "U） 延迟 ：端到端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 ...",
    "metadata": {
      "chunkIndex": 8.0,
      "kbId": 6.0,
      "startIndex": 1971.0,
      "endIndex": 2449.0,
      "documentId": 30.0
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

### fact-009 (fact)

- id: `fact-009`
- type: `fact`
- question: RAG 在线问答处理的步骤有哪些？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["问答处理（在线）", "问题向量化"]`
- retrieve hit ratio: `1/2`
- first_match_rank: `1`
- answer: RAG 在线问答处理的步骤包括：问题向量化、相似度检索、构建提示词、LLM 生成和返回结果。
- expected_keywords: `["问题向量化", "相似度检索", "构建提示词", "LLM 生成"]`
- keyword_hit: `4/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.41188666224479675,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召回相关文档 → 构建提示词 → LLM 生成答案 → 返回结果 2.1 详细步骤 Step 1: 文档处理（离线） 文档上传 ：PDF、Markdown、Word、代码文件等 文档解析 ：提取文本内容 文本分块 ：将长文档切分为小块（Chunk），通常 500-2000 字符 向量化 ：使用 Embedding 模型将文本转换为向量 存储 ：向量存入向量数据库（Milvus、Pinecone 等） Step 2: 问答处理（在线） 问...",
    "metadata": {
      "chunkIndex": 1.0,
      "kbId": 6.0,
      "startIndex": 220.0,
      "endIndex": 684.0,
      "documentId": 30.0
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

### fact-010 (fact)

- id: `fact-010`
- type: `fact`
- question: MySQL InnoDB 默认使用什么索引结构？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["B+ 树索引：MySQL InnoDB 默认索引结构"]`
- retrieve hit ratio: `0/1`
- first_match_rank: `-`
- answer: MySQL InnoDB 默认使用 B+ 树索引结构。
- expected_keywords: `["B+ 树索引", "InnoDB", "默认索引结构"]`
- keyword_hit: `2/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.5808570384979248,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "is-Plus 优势 ： 内置通用 CRUD 方法 代码生成器 分页插件 乐观锁、逻辑删除支持 5. MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNCOMMITTED（读未提交）...",
    "metadata": {
      "chunkIndex": 6.0,
      "kbId": 6.0,
      "startIndex": 1515.0,
      "endIndex": 1921.0,
      "documentId": 32.0
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
- answer:
- expected_keywords: `["Retrieval-Augmented Generation", "检索增强生成", "信息检索", "文本生成"]`
- keyword_hit: `0/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.44440945982933044,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "chunkIndex": 0.0,
      "kbId": 6.0,
      "startIndex": 0.0,
      "endIndex": 270.0,
      "documentId": 30.0
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
- errors: `HTTP 500 http://localhost:8080/api/qa/ask: {"status":500,"errorCode":"SYS_500","message":"系统内部错误，请稍后重试","traceId":"0019e8cbb7528061ee682959ffec94a8","timestamp":"2026-06-03T09:05:59.007589400Z","path":"/api/qa/ask"}`

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
- keyword_hit: `0/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.42033588886260986,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "技术 3.1 Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.52, ..., 0.91] (相似度高) \"今天天气很好\" → ...",
    "metadata": {
      "chunkIndex": 2.0,
      "kbId": 6.0,
      "startIndex": 634.0,
      "endIndex": 1022.0,
      "documentId": 30.0
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
- errors: `HTTP 500 http://localhost:8080/api/qa/ask: {"status":500,"errorCode":"SYS_500","message":"系统内部错误，请稍后重试","traceId":"0019e8cbb9bce5dabd62a659631dfe6e","timestamp":"2026-06-03T09:06:07.318746800Z","path":"/api/qa/ask"}`

### definition-004 (definition)

- id: `definition-004`
- type: `definition`
- question: 什么是文本分块，为什么 RAG 文档会提到 Chunk？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["文本分块策略", "小块检索更精准"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer: 根据提供的上下文，文本分块（Chunk）是指将一段较长的文本分割成较小的、更易于管理的部分。在 RAG 文档中， Chunk 被提及为一个特定的文本单位，例如 Chunk 3（500 字）和 Chunk 4（500 字），这表明 RAG 文档将内容分割成这些较小的部分，以便更好地组织和呈现信息。然而，上下文并没有提供有关文本分块的详细定义或其背后的技术原理，只是提到了它在 RAG 文档中的应用。因此，关于文本分块的详细信息和技术细节，提供的上下文并不完整。
- expected_keywords: `["分块", "Chunk", "Token 限制", "检索更精准"]`
- keyword_hit: `2/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.4159863293170929,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "0 字）：技术原理 Chunk 3（500 字）：应用场景 Chunk 4（500 字）：总结 4. 提示词工程（Prompt Engineering） RAG 系统需要精心设计提示词，将检索到的上下文和问题结合。 4.1 典型 Prompt 模板 你是一个专业的技术助手。请根据以下文档内容回答用户的问题。",
    "metadata": {
      "chunkIndex": 4.0,
      "kbId": 6.0,
      "startIndex": 1397.0,
      "endIndex": 1550.0,
      "documentId": 30.0
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

### definition-007 (definition)

- id: `definition-007`
- type: `definition`
- question: 什么是缓存穿透？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["缓存穿透：查询不存在的数据", "布隆过滤器、缓存空值"]`
- retrieve hit ratio: `1/2`
- first_match_rank: `1`
- answer: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试
- expected_keywords: `["缓存穿透", "不存在的数据", "布隆过滤器", "缓存空值"]`
- keyword_hit: `0/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.341765820980072,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "IALIZABLE（串行化） 锁机制 ： 行锁：锁定某一行 表锁：锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AO...",
    "metadata": {
      "chunkIndex": 7.0,
      "kbId": 6.0,
      "startIndex": 1871.0,
      "endIndex": 2187.0,
      "documentId": 32.0
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
- errors: `QA returned error status: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试`

### reasoning-001 (reasoning)

- id: `reasoning-001`
- type: `reasoning`
- question: 为什么 RAG 系统需要把文档分块？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["为什么需要分块？", "提高检索效率"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试
- expected_keywords: `["Token 限制", "检索更精准", "检索效率", "语义割裂"]`
- keyword_hit: `0/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.4213184416294098,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "n-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估...",
    "metadata": {
      "chunkIndex": 10.0,
      "kbId": 6.0,
      "startIndex": 2791.0,
      "endIndex": 3227.0,
      "documentId": 30.0
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
- errors: `QA returned error status: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试`

### reasoning-003 (reasoning)

- id: `reasoning-003`
- type: `reasoning`
- question: 为什么 RAG 检索后还需要 Rerank？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["重排序（Rerank）", "二次精排，提高准确率"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer: 根据提供的上下文，RAG（检索增强生成）技术结合了信息检索和文本生成。它先从知识库中检索相关信息，然后将这些信息作为上下文提供给大语言模型（LLM），最后LLM基于上下文生成准确的答案。然而，文中并没有明确解释为什么RAG检索后还需要Rerank（重新排名）。可能的原因是，检索出的信息可能有多个相关结果，需要通过重新排名来确定哪些信息最相关、最准确，以便提供给LLM生成最准确的答案。但是，由于上下文中没有直接提到Rerank的必要性和作用，因此这个问题的答案在提供的上下文中不完全明确。
- expected_keywords: `["重排序", "二次精排", "提高准确率", "检索优化"]`
- keyword_hit: `0/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.4292636215686798,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "chunkIndex": 0.0,
      "kbId": 6.0,
      "startIndex": 0.0,
      "endIndex": 270.0,
      "documentId": 30.0
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

### reasoning-004 (reasoning)

- id: `reasoning-004`
- type: `reasoning`
- question: 为什么 Spring 中更推荐构造器注入而不推荐字段注入？
- expected_sources: `["springboot-basics.md", "java-interview-guide.md"]`
- expected_contexts.contains: `["构造器注入（推荐，保证不可变性）", "字段注入（不推荐）"]`
- retrieve hit ratio: `1/2`
- first_match_rank: `1`
- answer: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试
- expected_keywords: `["构造器注入", "保证不可变性", "字段注入", "难以测试"]`
- keyword_hit: `0/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.5260632634162903,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "Spring 框架 2.1 Spring IOC（控制反转） 依赖注入的三种方式 ： 构造器注入（推荐，保证不可变性） Setter 注入（可选依赖） 字段注入（不推荐，难以测试） Bean 的生命周期 ： 实例化 属性赋值 初始化（@PostConstruct） 使用 销毁（@PreDestroy） 2.2 Spring AOP（面向切面编程） 应用场景 ：事务管理、日志记录、权限校验、性能监控 核心概念 ： Aspect（切面）：横切关注点的模块化 Join Point（连接点）：方法执行点 Pointcut（切入点）：匹配规则 Advice（通知）：@Before、@After、@Aro...",
    "metadata": {
      "chunkIndex": 3.0,
      "kbId": 6.0,
      "startIndex": 603.0,
      "endIndex": 945.0,
      "documentId": 32.0
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
- errors: `QA returned error status: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试`

### reasoning-005 (reasoning)

- id: `reasoning-005`
- type: `reasoning`
- question: 为什么多线程环境下不应该直接使用 HashMap？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["线程不安全，多线程使用 ConcurrentHashMap"]`
- retrieve hit ratio: `0/1`
- first_match_rank: `-`
- answer: 因为HashMap在多线程环境下是线程不安全的。它的扩容机制和put、get等操作都可能导致线程安全问题。如果多个线程同时访问和修改HashMap，可能会导致数据不一致、丢失更新等问题。因此，在多线程环境下，应该使用线程安全的ConcurrentHashMap代替HashMap。
- expected_keywords: `["线程不安全", "ConcurrentHashMap", "多线程"]`
- keyword_hit: `3/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.39487847685813904,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "chunkIndex": 1.0,
      "kbId": 6.0,
      "startIndex": 18.0,
      "endIndex": 518.0,
      "documentId": 32.0
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
- answer: 为了提高 RAG 准确率，可以从检索、分块和 Prompt 三方面进行优化。具体来说，可以通过增加 Top-K 来检索更多候选，使用 Rerank 进行二次精排；优化分块策略，合理设置 Chunk 大小；以及优化 Prompt，明确回答要求。这些方法可以帮助提高 RAG 的准确率和性能。
- expected_keywords: `["优化分块策略", "提升 Embedding 质量", "增加 Top-K", "使用 Rerank", "优化 Prompt"]`
- keyword_hit: `4/5`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.4106127917766571,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "U） 延迟 ：端到端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 ...",
    "metadata": {
      "chunkIndex": 8.0,
      "kbId": 6.0,
      "startIndex": 1971.0,
      "endIndex": 2449.0,
      "documentId": 30.0
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
- answer: 根据提供的上下文，Java 面试文档和 Spring Boot 文档中关于接口性能优化的缓存相关手段是使用缓存（Redis）。
- expected_keywords: `["Redis", "使用缓存", "@Cacheable", "@CacheEvict", "@CachePut"]`
- keyword_hit: `2/5`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.5170324444770813,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID + Redis Token 机制 乐观锁（版本号） 状态机（订单状态） 5. 如何实现限流？ 计数器 滑动窗口 令牌桶（Guava RateLimiter） 漏桶算法 --- 祝你面试顺利！🎉",
    "metadata": {
      "chunkIndex": 10.0,
      "kbId": 6.0,
      "startIndex": 2887.0,
      "endIndex": 3161.0,
      "documentId": 32.0
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

## Failed Citation Cases

### fact-001 (fact)

- id: `fact-001`
- type: `fact`
- question: Spring Boot 的核心特性有哪些？
- expected_sources: `["springboot-basics.md"]`
- expected_contexts.contains: `["核心特性", "自动配置"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `1`
- answer:
- expected_keywords: `["自动配置", "起步依赖", "内嵌服务器", "生产就绪"]`
- keyword_hit: `0/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.6728367209434509,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "springboot-basics.md",
    "documentId": 31,
    "chunkId": "0",
    "contentPreview": "Spring Boot 核心知识点 1. Spring Boot 简介 Spring Boot 是基于 Spring 框架的快速开发脚手架，旨在简化 Spring 应用的初始搭建和开发过程。 1.1 核心特性 自动配置 ：根据 classpath 下的依赖自动配置 Spring 应用 起步依赖 ：一站式依赖管理，避免版本冲突 内嵌服务器 ：Tomcat、Jetty、Undertow 生产就绪 ：Actuator 健康检查、监控指标 无代码生成 ：不需要 XML 配置 2.",
    "metadata": {
      "chunkIndex": 0.0,
      "kbId": 6.0,
      "startIndex": 0.0,
      "endIndex": 239.0,
      "documentId": 31.0
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
- errors: `HTTP 500 http://localhost:8080/api/qa/ask: {"status":500,"errorCode":"SYS_500","message":"系统内部错误，请稍后重试","traceId":"0019e8cbac84e544e36726da6776d0d5","timestamp":"2026-06-03T09:05:21.543473400Z","path":"/api/qa/ask"}`

### fact-002 (fact)

- id: `fact-002`
- type: `fact`
- question: @SpringBootApplication 由哪些注解组合而成？
- expected_sources: `["springboot-basics.md", "java-interview-guide.md"]`
- expected_contexts.contains: `["@SpringBootApplication 是组合注解", "@SpringBootApplication = @Configuration"]`
- retrieve hit ratio: `1/2`
- first_match_rank: `1`
- answer: @SpringBootApplication 由 @Configuration、@EnableAutoConfiguration 和 @ComponentScan 三个注解组合而成。
- expected_keywords: `["@Configuration", "@EnableAutoConfiguration", "@ComponentScan"]`
- keyword_hit: `3/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.6092399954795837,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "springboot-basics.md",
    "documentId": 31,
    "chunkId": "0",
    "contentPreview": "rtow 生产就绪 ：Actuator 健康检查、监控指标 无代码生成 ：不需要 XML 配置 2. 核心注解 2.1 启动类注解 @SpringBootApplication public class Application { public static void main(String[] args) { SpringApplication.run(Application.class, args); } } @SpringBootApplication 是组合注解，包含： @Configuration ：标记为配置类 @EnableAutoConfiguration ：启用自动配置 @C...",
    "metadata": {
      "chunkIndex": 1.0,
      "kbId": 6.0,
      "startIndex": 189.0,
      "endIndex": 729.0,
      "documentId": 31.0
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

### fact-003 (fact)

- id: `fact-003`
- type: `fact`
- question: @RestController 等价于哪两个注解的组合？
- expected_sources: `["springboot-basics.md"]`
- expected_contexts.contains: `["@RestController：= @Controller + @ResponseBody"]`
- retrieve hit ratio: `0/1`
- first_match_rank: `-`
- answer: @RestController 等价于 @Controller 和 @ResponseBody 的组合。
- expected_keywords: `["@Controller", "@ResponseBody", "@RestController"]`
- keyword_hit: `3/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.49326619505882263,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "springboot-basics.md",
    "documentId": 31,
    "chunkId": "0",
    "contentPreview": "rtow 生产就绪 ：Actuator 健康检查、监控指标 无代码生成 ：不需要 XML 配置 2. 核心注解 2.1 启动类注解 @SpringBootApplication public class Application { public static void main(String[] args) { SpringApplication.run(Application.class, args); } } @SpringBootApplication 是组合注解，包含： @Configuration ：标记为配置类 @EnableAutoConfiguration ：启用自动配置 @C...",
    "metadata": {
      "chunkIndex": 1.0,
      "kbId": 6.0,
      "startIndex": 189.0,
      "endIndex": 729.0,
      "documentId": 31.0
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

### fact-004 (fact)

- id: `fact-004`
- type: `fact`
- question: Spring Boot 配置优先级最高的来源是什么？
- expected_sources: `["springboot-basics.md"]`
- expected_contexts.contains: `["配置优先级", "命令行参数"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `1`
- answer: 命令行参数是Spring Boot配置优先级最高的来源。
- expected_keywords: `["命令行参数", "配置优先级"]`
- keyword_hit: `2/2`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.48946133255958557,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "springboot-basics.md",
    "documentId": 31,
    "chunkId": "0",
    "contentPreview": "logging: level: root: INFO com.example: DEBUG 3.2 配置优先级 命令行参数 JNDI 属性 Java 系统属性 环境变量 application.properties/yml 4. 依赖注入 4.1 构造器注入（推荐） @Service @RequiredArgsConstructor public class UserService { private final UserRepository userRepository; // Lombok 自动生成构造器，Spring 自动注入 } 4.2 字段注入（不推荐） @Service publi...",
    "metadata": {
      "chunkIndex": 5.0,
      "kbId": 6.0,
      "startIndex": 973.0,
      "endIndex": 1393.0,
      "documentId": 31.0
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

### fact-005 (fact)

- id: `fact-005`
- type: `fact`
- question: JWT 认证中 accessToken 和 refreshToken 分别有什么作用？
- expected_sources: `["springboot-basics.md"]`
- expected_contexts.contains: `["accessToken：短期令牌", "refreshToken：长期令牌"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer: 在JWT认证中，accessToken和refreshToken分别有不同的作用。accessToken是一个短期令牌，有效期为1小时，用于访问接口。refreshToken是一个长期令牌，有效期为7天，当accessToken过期时，可以用refreshToken刷新获取新的accessToken。
- expected_keywords: `["accessToken", "refreshToken", "访问接口", "刷新"]`
- keyword_hit: `4/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.5016002058982849,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "springboot-basics.md",
    "documentId": 31,
    "chunkId": "0",
    "contentPreview": "onCreationPolicy(SessionCreationPolicy.STATELESS); return http.build(); } } 7.2 JWT 认证 accessToken ：短期令牌（1小时） refreshToken ：长期令牌（7天） 流程 ： 登录成功，返回两个 Token 访问接口携带 accessToken accessToken 过期，用 refreshToken 刷新 8. 参数校验 8.1 常用注解 public class CreateUserRequest { @NotBlank(message = \"用户名不能为空\") @Size(min = 3...",
    "metadata": {
      "chunkIndex": 11.0,
      "kbId": 6.0,
      "startIndex": 3063.0,
      "endIndex": 3563.0,
      "documentId": 31.0
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

### fact-006 (fact)

- id: `fact-006`
- type: `fact`
- question: JDK 8 中 HashMap 的底层结构是什么？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["JDK 8：数组 + 链表 + 红黑树", "链表长度 > 8"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `1`
- answer: JDK 8 中 HashMap 的底层结构是数组 + 链表 + 红黑树，当链表长度大于 8 时，会转换为红黑树。
- expected_keywords: `["数组", "链表", "红黑树", "链表长度 > 8"]`
- keyword_hit: `3/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.473936527967453,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "chunkIndex": 1.0,
      "kbId": 6.0,
      "startIndex": 18.0,
      "endIndex": 518.0,
      "documentId": 32.0
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

### fact-007 (fact)

- id: `fact-007`
- type: `fact`
- question: Redis 常见数据类型包括哪些？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["数据类型", "ZSet：排行榜"]`
- retrieve hit ratio: `1/2`
- first_match_rank: `1`
- answer: Redis 常见数据类型包括 String、Hash、List、Set 和 ZSet。
- expected_keywords: `["String", "Hash", "List", "Set", "ZSet"]`
- keyword_hit: `5/5`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.503896951675415,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "IALIZABLE（串行化） 锁机制 ： 行锁：锁定某一行 表锁：锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AO...",
    "metadata": {
      "chunkIndex": 7.0,
      "kbId": 6.0,
      "startIndex": 1871.0,
      "endIndex": 2187.0,
      "documentId": 32.0
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

### fact-008 (fact)

- id: `fact-008`
- type: `fact`
- question: RAG 文档中列出的常用向量数据库有哪些？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["常用数据库", "Qdrant：Rust 实现"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer: RAG 文档中列出的常用向量数据库包括 Qdrant、Weaviate、Milvus 和 Pinecone。其中，小规模向量数据库（< 100 万向量）可选择 Qdrant 或 Weaviate，大规模向量数据库（> 100 万向量）可选择 Milvus 或 Pinecone。此外，Milvus 和 Qdrant 支持本地部署，而 Pinecone 和 Weaviate Cloud 支持云服务。
- expected_keywords: `["Milvus", "Pinecone", "Weaviate", "Qdrant"]`
- keyword_hit: `4/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.4407072067260742,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "U） 延迟 ：端到端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 ...",
    "metadata": {
      "chunkIndex": 8.0,
      "kbId": 6.0,
      "startIndex": 1971.0,
      "endIndex": 2449.0,
      "documentId": 30.0
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

### fact-009 (fact)

- id: `fact-009`
- type: `fact`
- question: RAG 在线问答处理的步骤有哪些？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["问答处理（在线）", "问题向量化"]`
- retrieve hit ratio: `1/2`
- first_match_rank: `1`
- answer: RAG 在线问答处理的步骤包括：问题向量化、相似度检索、构建提示词、LLM 生成和返回结果。
- expected_keywords: `["问题向量化", "相似度检索", "构建提示词", "LLM 生成"]`
- keyword_hit: `4/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.41188666224479675,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召回相关文档 → 构建提示词 → LLM 生成答案 → 返回结果 2.1 详细步骤 Step 1: 文档处理（离线） 文档上传 ：PDF、Markdown、Word、代码文件等 文档解析 ：提取文本内容 文本分块 ：将长文档切分为小块（Chunk），通常 500-2000 字符 向量化 ：使用 Embedding 模型将文本转换为向量 存储 ：向量存入向量数据库（Milvus、Pinecone 等） Step 2: 问答处理（在线） 问...",
    "metadata": {
      "chunkIndex": 1.0,
      "kbId": 6.0,
      "startIndex": 220.0,
      "endIndex": 684.0,
      "documentId": 30.0
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

### fact-010 (fact)

- id: `fact-010`
- type: `fact`
- question: MySQL InnoDB 默认使用什么索引结构？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["B+ 树索引：MySQL InnoDB 默认索引结构"]`
- retrieve hit ratio: `0/1`
- first_match_rank: `-`
- answer: MySQL InnoDB 默认使用 B+ 树索引结构。
- expected_keywords: `["B+ 树索引", "InnoDB", "默认索引结构"]`
- keyword_hit: `2/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.5808570384979248,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "is-Plus 优势 ： 内置通用 CRUD 方法 代码生成器 分页插件 乐观锁、逻辑删除支持 5. MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNCOMMITTED（读未提交）...",
    "metadata": {
      "chunkIndex": 6.0,
      "kbId": 6.0,
      "startIndex": 1515.0,
      "endIndex": 1921.0,
      "documentId": 32.0
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
- answer:
- expected_keywords: `["Retrieval-Augmented Generation", "检索增强生成", "信息检索", "文本生成"]`
- keyword_hit: `0/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.44440945982933044,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "chunkIndex": 0.0,
      "kbId": 6.0,
      "startIndex": 0.0,
      "endIndex": 270.0,
      "documentId": 30.0
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
- errors: `HTTP 500 http://localhost:8080/api/qa/ask: {"status":500,"errorCode":"SYS_500","message":"系统内部错误，请稍后重试","traceId":"0019e8cbb7528061ee682959ffec94a8","timestamp":"2026-06-03T09:05:59.007589400Z","path":"/api/qa/ask"}`

### definition-002 (definition)

- id: `definition-002`
- type: `definition`
- question: 什么是 Embedding？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["将文本转换为高维向量", "语义相似的文本向量距离更近"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `1`
- answer: Embedding 是一种技术，将文本转换为高维向量，语义相似的文本向量距离更近。
- expected_keywords: `["高维向量", "语义相似", "距离更近"]`
- keyword_hit: `3/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.3202931582927704,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "技术 3.1 Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.52, ..., 0.91] (相似度高) \"今天天气很好\" → ...",
    "metadata": {
      "chunkIndex": 2.0,
      "kbId": 6.0,
      "startIndex": 634.0,
      "endIndex": 1022.0,
      "documentId": 30.0
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
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer:
- expected_keywords: `["存储", "检索向量", "相似度搜索"]`
- keyword_hit: `0/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.42033588886260986,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "技术 3.1 Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.52, ..., 0.91] (相似度高) \"今天天气很好\" → ...",
    "metadata": {
      "chunkIndex": 2.0,
      "kbId": 6.0,
      "startIndex": 634.0,
      "endIndex": 1022.0,
      "documentId": 30.0
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
- errors: `HTTP 500 http://localhost:8080/api/qa/ask: {"status":500,"errorCode":"SYS_500","message":"系统内部错误，请稍后重试","traceId":"0019e8cbb9bce5dabd62a659631dfe6e","timestamp":"2026-06-03T09:06:07.318746800Z","path":"/api/qa/ask"}`

### definition-004 (definition)

- id: `definition-004`
- type: `definition`
- question: 什么是文本分块，为什么 RAG 文档会提到 Chunk？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["文本分块策略", "小块检索更精准"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer: 根据提供的上下文，文本分块（Chunk）是指将一段较长的文本分割成较小的、更易于管理的部分。在 RAG 文档中， Chunk 被提及为一个特定的文本单位，例如 Chunk 3（500 字）和 Chunk 4（500 字），这表明 RAG 文档将内容分割成这些较小的部分，以便更好地组织和呈现信息。然而，上下文并没有提供有关文本分块的详细定义或其背后的技术原理，只是提到了它在 RAG 文档中的应用。因此，关于文本分块的详细信息和技术细节，提供的上下文并不完整。
- expected_keywords: `["分块", "Chunk", "Token 限制", "检索更精准"]`
- keyword_hit: `2/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.4159863293170929,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "0 字）：技术原理 Chunk 3（500 字）：应用场景 Chunk 4（500 字）：总结 4. 提示词工程（Prompt Engineering） RAG 系统需要精心设计提示词，将检索到的上下文和问题结合。 4.1 典型 Prompt 模板 你是一个专业的技术助手。请根据以下文档内容回答用户的问题。",
    "metadata": {
      "chunkIndex": 4.0,
      "kbId": 6.0,
      "startIndex": 1397.0,
      "endIndex": 1550.0,
      "documentId": 30.0
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
- answer: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试
- expected_keywords: `["控制反转", "依赖注入", "构造器注入"]`
- keyword_hit: `0/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.5036572813987732,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "Spring 框架 2.1 Spring IOC（控制反转） 依赖注入的三种方式 ： 构造器注入（推荐，保证不可变性） Setter 注入（可选依赖） 字段注入（不推荐，难以测试） Bean 的生命周期 ： 实例化 属性赋值 初始化（@PostConstruct） 使用 销毁（@PreDestroy） 2.2 Spring AOP（面向切面编程） 应用场景 ：事务管理、日志记录、权限校验、性能监控 核心概念 ： Aspect（切面）：横切关注点的模块化 Join Point（连接点）：方法执行点 Pointcut（切入点）：匹配规则 Advice（通知）：@Before、@After、@Aro...",
    "metadata": {
      "chunkIndex": 3.0,
      "kbId": 6.0,
      "startIndex": 603.0,
      "endIndex": 945.0,
      "documentId": 32.0
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
- errors: `QA returned error status: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试`

### definition-006 (definition)

- id: `definition-006`
- type: `definition`
- question: Spring AOP 是什么，适合用在哪些场景？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["Spring AOP（面向切面编程）", "应用场景"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `1`
- answer: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试
- expected_keywords: `["面向切面编程", "事务管理", "日志记录", "权限校验"]`
- keyword_hit: `0/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.4810759127140045,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "Spring 框架 2.1 Spring IOC（控制反转） 依赖注入的三种方式 ： 构造器注入（推荐，保证不可变性） Setter 注入（可选依赖） 字段注入（不推荐，难以测试） Bean 的生命周期 ： 实例化 属性赋值 初始化（@PostConstruct） 使用 销毁（@PreDestroy） 2.2 Spring AOP（面向切面编程） 应用场景 ：事务管理、日志记录、权限校验、性能监控 核心概念 ： Aspect（切面）：横切关注点的模块化 Join Point（连接点）：方法执行点 Pointcut（切入点）：匹配规则 Advice（通知）：@Before、@After、@Aro...",
    "metadata": {
      "chunkIndex": 3.0,
      "kbId": 6.0,
      "startIndex": 603.0,
      "endIndex": 945.0,
      "documentId": 32.0
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
- errors: `QA returned error status: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试`

### definition-007 (definition)

- id: `definition-007`
- type: `definition`
- question: 什么是缓存穿透？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["缓存穿透：查询不存在的数据", "布隆过滤器、缓存空值"]`
- retrieve hit ratio: `1/2`
- first_match_rank: `1`
- answer: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试
- expected_keywords: `["缓存穿透", "不存在的数据", "布隆过滤器", "缓存空值"]`
- keyword_hit: `0/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.341765820980072,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "IALIZABLE（串行化） 锁机制 ： 行锁：锁定某一行 表锁：锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AO...",
    "metadata": {
      "chunkIndex": 7.0,
      "kbId": 6.0,
      "startIndex": 1871.0,
      "endIndex": 2187.0,
      "documentId": 32.0
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
- errors: `QA returned error status: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试`

### definition-008 (definition)

- id: `definition-008`
- type: `definition`
- question: 什么是分布式锁，文档中列出了哪些实现方式？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["分布式锁", "Redis SETNX + 过期时间"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `1`
- answer: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试
- expected_keywords: `["Redis SETNX", "Redisson", "Zookeeper", "过期时间"]`
- keyword_hit: `0/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.40275147557258606,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好 7. 分布式 7.1 分布式锁 实现方式 ： Redis SETNX + 过期时间 Redisson（推荐） Zookeeper 注意事项 ： 防止死锁（设置过期时间） 防止误删（UUID 标识） 原子性（Lua 脚本） 7.2 分布式事务 2PC（两阶段提交） ： 准备阶段、提交阶段 缺点：阻塞、单点故障 TCC（Try-Confirm-Cancel） ： 业务侵入性强 性能较好 Seata ：阿里开源分布式事务框架 8. 消息队列 8.1 RabbitMQ / Kafka 应用场景 ： 异步处理...",
    "metadata": {
      "chunkIndex": 8.0,
      "kbId": 6.0,
      "startIndex": 2137.0,
      "endIndex": 2505.0,
      "documentId": 32.0
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
- errors: `QA returned error status: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试`

### reasoning-001 (reasoning)

- id: `reasoning-001`
- type: `reasoning`
- question: 为什么 RAG 系统需要把文档分块？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["为什么需要分块？", "提高检索效率"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试
- expected_keywords: `["Token 限制", "检索更精准", "检索效率", "语义割裂"]`
- keyword_hit: `0/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.4213184416294098,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "n-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估...",
    "metadata": {
      "chunkIndex": 10.0,
      "kbId": 6.0,
      "startIndex": 2791.0,
      "endIndex": 3227.0,
      "documentId": 30.0
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
- errors: `QA returned error status: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试`

### reasoning-002 (reasoning)

- id: `reasoning-002`
- type: `reasoning`
- question: 为什么 RAG 在知识更新和可解释性上通常比微调更适合企业知识库？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["RAG vs 微调", "知识更新"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `1`
- answer: RAG 在知识更新和可解释性上通常比微调更适合企业知识库，因为 RAG 支持实时更新文档，且具有高可解释性，能够追溯来源。相比之下，微调需要重新训练模型，并且其可解释性较低。这使得 RAG 更适合需要频繁更新知识和追溯信息来源的企业知识库。同时，RAG 的低成本（只需 API 调用）也是一个重要的优势。因此，结合 RAG 和微调可以发挥两者的优势，实现最佳实践。
- expected_keywords: `["实时更新文档", "成本低", "可追溯来源", "可解释性高"]`
- keyword_hit: `1/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.490170419216156,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "U） 延迟 ：端到端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 ...",
    "metadata": {
      "chunkIndex": 8.0,
      "kbId": 6.0,
      "startIndex": 1971.0,
      "endIndex": 2449.0,
      "documentId": 30.0
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

### reasoning-003 (reasoning)

- id: `reasoning-003`
- type: `reasoning`
- question: 为什么 RAG 检索后还需要 Rerank？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["重排序（Rerank）", "二次精排，提高准确率"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer: 根据提供的上下文，RAG（检索增强生成）技术结合了信息检索和文本生成。它先从知识库中检索相关信息，然后将这些信息作为上下文提供给大语言模型（LLM），最后LLM基于上下文生成准确的答案。然而，文中并没有明确解释为什么RAG检索后还需要Rerank（重新排名）。可能的原因是，检索出的信息可能有多个相关结果，需要通过重新排名来确定哪些信息最相关、最准确，以便提供给LLM生成最准确的答案。但是，由于上下文中没有直接提到Rerank的必要性和作用，因此这个问题的答案在提供的上下文中不完全明确。
- expected_keywords: `["重排序", "二次精排", "提高准确率", "检索优化"]`
- keyword_hit: `0/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.4292636215686798,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "chunkIndex": 0.0,
      "kbId": 6.0,
      "startIndex": 0.0,
      "endIndex": 270.0,
      "documentId": 30.0
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

### reasoning-004 (reasoning)

- id: `reasoning-004`
- type: `reasoning`
- question: 为什么 Spring 中更推荐构造器注入而不推荐字段注入？
- expected_sources: `["springboot-basics.md", "java-interview-guide.md"]`
- expected_contexts.contains: `["构造器注入（推荐，保证不可变性）", "字段注入（不推荐）"]`
- retrieve hit ratio: `1/2`
- first_match_rank: `1`
- answer: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试
- expected_keywords: `["构造器注入", "保证不可变性", "字段注入", "难以测试"]`
- keyword_hit: `0/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.5260632634162903,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "Spring 框架 2.1 Spring IOC（控制反转） 依赖注入的三种方式 ： 构造器注入（推荐，保证不可变性） Setter 注入（可选依赖） 字段注入（不推荐，难以测试） Bean 的生命周期 ： 实例化 属性赋值 初始化（@PostConstruct） 使用 销毁（@PreDestroy） 2.2 Spring AOP（面向切面编程） 应用场景 ：事务管理、日志记录、权限校验、性能监控 核心概念 ： Aspect（切面）：横切关注点的模块化 Join Point（连接点）：方法执行点 Pointcut（切入点）：匹配规则 Advice（通知）：@Before、@After、@Aro...",
    "metadata": {
      "chunkIndex": 3.0,
      "kbId": 6.0,
      "startIndex": 603.0,
      "endIndex": 945.0,
      "documentId": 32.0
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
- errors: `QA returned error status: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试`

### reasoning-005 (reasoning)

- id: `reasoning-005`
- type: `reasoning`
- question: 为什么多线程环境下不应该直接使用 HashMap？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["线程不安全，多线程使用 ConcurrentHashMap"]`
- retrieve hit ratio: `0/1`
- first_match_rank: `-`
- answer: 因为HashMap在多线程环境下是线程不安全的。它的扩容机制和put、get等操作都可能导致线程安全问题。如果多个线程同时访问和修改HashMap，可能会导致数据不一致、丢失更新等问题。因此，在多线程环境下，应该使用线程安全的ConcurrentHashMap代替HashMap。
- expected_keywords: `["线程不安全", "ConcurrentHashMap", "多线程"]`
- keyword_hit: `3/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.39487847685813904,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "chunkIndex": 1.0,
      "kbId": 6.0,
      "startIndex": 18.0,
      "endIndex": 518.0,
      "documentId": 32.0
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
- retrieve hit ratio: `2/2`
- first_match_rank: `1`
- answer: MyBatis 中 #{} 比 ${} 更适合接收用户输入，因为 #{} 预编译，可以防止 SQL 注入，而 ${} 是字符串替换，存在 SQL 注入风险。
- expected_keywords: `["预编译", "SQL 注入", "字符串替换"]`
- keyword_hit: `3/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.44793784618377686,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "plication-prod.yml 通过 spring.profiles.active 切换 4. MyBatis / MyBatis-Plus 4.1 MyBatis 核心 #{} vs ${} ： #{} 预编译，防止 SQL 注入 ${} 字符串替换，存在注入风险 一级缓存 vs 二级缓存 ： 一级缓存：SqlSession 级别，默认开启 二级缓存：Mapper 级别，需要手动开启 4.2 MyBatis-Plus 优势 ： 内置通用 CRUD 方法 代码生成器 分页插件 乐观锁、逻辑删除支持 5.",
    "metadata": {
      "chunkIndex": 5.0,
      "kbId": 6.0,
      "startIndex": 1307.0,
      "endIndex": 1565.0,
      "documentId": 32.0
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

### multi-hop-001 (multi_hop)

- id: `multi-hop-001`
- type: `multi_hop`
- question: 请把 RAG 的离线文档处理和在线问答处理串成一条完整链路。
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["文档处理（离线）", "问答处理（在线）"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `1`
- answer: RAG 的完整链路包括离线文档处理和在线问答处理。首先，在离线文档处理阶段，文档上传后会被解析提取文本内容，然后分块成小块（通常500-2000字符），接着使用Embedding模型将这些文本转换为向量，并存储到向量数据库中。在线问答处理阶段，用户的问题会被转换为向量，然后在向量数据库中搜索最相似的文本块，构建提示词，将检索到的内容和问题组合成Prompt，最后调用大模型生成答案并返回结果。
- expected_keywords: `["文档上传", "文档解析", "文本分块", "向量化", "相似度检索", "Prompt", "LLM"]`
- keyword_hit: `2/7`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.40575239062309265,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召回相关文档 → 构建提示词 → LLM 生成答案 → 返回结果 2.1 详细步骤 Step 1: 文档处理（离线） 文档上传 ：PDF、Markdown、Word、代码文件等 文档解析 ：提取文本内容 文本分块 ：将长文档切分为小块（Chunk），通常 500-2000 字符 向量化 ：使用 Embedding 模型将文本转换为向量 存储 ：向量存入向量数据库（Milvus、Pinecone 等） Step 2: 问答处理（在线） 问...",
    "metadata": {
      "chunkIndex": 1.0,
      "kbId": 6.0,
      "startIndex": 220.0,
      "endIndex": 684.0,
      "documentId": 30.0
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
- answer: 为了提高 RAG 准确率，可以从检索、分块和 Prompt 三方面进行优化。具体来说，可以通过增加 Top-K 来检索更多候选，使用 Rerank 进行二次精排；优化分块策略，合理设置 Chunk 大小；以及优化 Prompt，明确回答要求。这些方法可以帮助提高 RAG 的准确率和性能。
- expected_keywords: `["优化分块策略", "提升 Embedding 质量", "增加 Top-K", "使用 Rerank", "优化 Prompt"]`
- keyword_hit: `4/5`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.4106127917766571,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "U） 延迟 ：端到端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 ...",
    "metadata": {
      "chunkIndex": 8.0,
      "kbId": 6.0,
      "startIndex": 1971.0,
      "endIndex": 2449.0,
      "documentId": 30.0
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
- answer: 根据提供的上下文，Java 面试文档和 Spring Boot 文档中关于接口性能优化的缓存相关手段是使用缓存（Redis）。
- expected_keywords: `["Redis", "使用缓存", "@Cacheable", "@CacheEvict", "@CachePut"]`
- keyword_hit: `2/5`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.5170324444770813,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID + Redis Token 机制 乐观锁（版本号） 状态机（订单状态） 5. 如何实现限流？ 计数器 滑动窗口 令牌桶（Guava RateLimiter） 漏桶算法 --- 祝你面试顺利！🎉",
    "metadata": {
      "chunkIndex": 10.0,
      "kbId": 6.0,
      "startIndex": 2887.0,
      "endIndex": 3161.0,
      "documentId": 32.0
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

## Low Answer Keyword Hit Cases

### fact-001 (fact)

- id: `fact-001`
- type: `fact`
- question: Spring Boot 的核心特性有哪些？
- expected_sources: `["springboot-basics.md"]`
- expected_contexts.contains: `["核心特性", "自动配置"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `1`
- answer:
- expected_keywords: `["自动配置", "起步依赖", "内嵌服务器", "生产就绪"]`
- keyword_hit: `0/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.6728367209434509,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "springboot-basics.md",
    "documentId": 31,
    "chunkId": "0",
    "contentPreview": "Spring Boot 核心知识点 1. Spring Boot 简介 Spring Boot 是基于 Spring 框架的快速开发脚手架，旨在简化 Spring 应用的初始搭建和开发过程。 1.1 核心特性 自动配置 ：根据 classpath 下的依赖自动配置 Spring 应用 起步依赖 ：一站式依赖管理，避免版本冲突 内嵌服务器 ：Tomcat、Jetty、Undertow 生产就绪 ：Actuator 健康检查、监控指标 无代码生成 ：不需要 XML 配置 2.",
    "metadata": {
      "chunkIndex": 0.0,
      "kbId": 6.0,
      "startIndex": 0.0,
      "endIndex": 239.0,
      "documentId": 31.0
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
- errors: `HTTP 500 http://localhost:8080/api/qa/ask: {"status":500,"errorCode":"SYS_500","message":"系统内部错误，请稍后重试","traceId":"0019e8cbac84e544e36726da6776d0d5","timestamp":"2026-06-03T09:05:21.543473400Z","path":"/api/qa/ask"}`

### fact-006 (fact)

- id: `fact-006`
- type: `fact`
- question: JDK 8 中 HashMap 的底层结构是什么？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["JDK 8：数组 + 链表 + 红黑树", "链表长度 > 8"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `1`
- answer: JDK 8 中 HashMap 的底层结构是数组 + 链表 + 红黑树，当链表长度大于 8 时，会转换为红黑树。
- expected_keywords: `["数组", "链表", "红黑树", "链表长度 > 8"]`
- keyword_hit: `3/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.473936527967453,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "Java 基础 1.1 面向对象编程 封装、继承、多态 ：面向对象的三大特性 抽象类 vs 接口 ： 抽象类可以有构造方法，接口不能 一个类只能继承一个抽象类，但可以实现多个接口 Java 8 后接口可以有默认方法和静态方法 1.2 集合框架 ArrayList vs LinkedList ： ArrayList 基于数组，查询快 O(1)，插入删除慢 O(n) LinkedList 基于链表，插入删除快 O(1)，查询慢 O(n) HashMap 原理 ： JDK 7：数组 + 链表 JDK 8：数组 + 链表 + 红黑树（链表长度 > 8 时转换） 扩容机制：负载因子 0.75，容量翻倍 ...",
    "metadata": {
      "chunkIndex": 1.0,
      "kbId": 6.0,
      "startIndex": 18.0,
      "endIndex": 518.0,
      "documentId": 32.0
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

### fact-010 (fact)

- id: `fact-010`
- type: `fact`
- question: MySQL InnoDB 默认使用什么索引结构？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["B+ 树索引：MySQL InnoDB 默认索引结构"]`
- retrieve hit ratio: `0/1`
- first_match_rank: `-`
- answer: MySQL InnoDB 默认使用 B+ 树索引结构。
- expected_keywords: `["B+ 树索引", "InnoDB", "默认索引结构"]`
- keyword_hit: `2/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.5808570384979248,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "is-Plus 优势 ： 内置通用 CRUD 方法 代码生成器 分页插件 乐观锁、逻辑删除支持 5. MySQL 数据库 5.1 索引 B+ 树索引 ：MySQL InnoDB 默认索引结构 聚簇索引 vs 非聚簇索引 ： 聚簇索引：主键索引，叶子节点存储完整数据 非聚簇索引：辅助索引，叶子节点存储主键值 索引失效场景 ： 使用函数或表达式 类型转换 最左前缀原则失效 使用 OR 连接 5.2 事务 ACID 特性 ： 原子性 Atomicity 一致性 Consistency 隔离性 Isolation 持久性 Durability 隔离级别 ： READ UNCOMMITTED（读未提交）...",
    "metadata": {
      "chunkIndex": 6.0,
      "kbId": 6.0,
      "startIndex": 1515.0,
      "endIndex": 1921.0,
      "documentId": 32.0
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
- answer:
- expected_keywords: `["Retrieval-Augmented Generation", "检索增强生成", "信息检索", "文本生成"]`
- keyword_hit: `0/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.44440945982933044,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "chunkIndex": 0.0,
      "kbId": 6.0,
      "startIndex": 0.0,
      "endIndex": 270.0,
      "documentId": 30.0
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
- errors: `HTTP 500 http://localhost:8080/api/qa/ask: {"status":500,"errorCode":"SYS_500","message":"系统内部错误，请稍后重试","traceId":"0019e8cbb7528061ee682959ffec94a8","timestamp":"2026-06-03T09:05:59.007589400Z","path":"/api/qa/ask"}`

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
- keyword_hit: `0/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.42033588886260986,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "技术 3.1 Embedding（嵌入/向量化） 将文本转换为高维向量，语义相似的文本向量距离更近。 常用模型 ： OpenAI text-embedding-ada-002 ：1536 维 BGE 系列 （中文优化）：bge-large-zh、bge-small-zh 通义千问 Embedding ：text-embedding-v1 示例 ： \"Java 是一种编程语言\" → [0.12, -0.34, 0.56, ..., 0.89] (1536 维向量) \"Python 是编程语言\" → [0.15, -0.31, 0.52, ..., 0.91] (相似度高) \"今天天气很好\" → ...",
    "metadata": {
      "chunkIndex": 2.0,
      "kbId": 6.0,
      "startIndex": 634.0,
      "endIndex": 1022.0,
      "documentId": 30.0
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
- errors: `HTTP 500 http://localhost:8080/api/qa/ask: {"status":500,"errorCode":"SYS_500","message":"系统内部错误，请稍后重试","traceId":"0019e8cbb9bce5dabd62a659631dfe6e","timestamp":"2026-06-03T09:06:07.318746800Z","path":"/api/qa/ask"}`

### definition-004 (definition)

- id: `definition-004`
- type: `definition`
- question: 什么是文本分块，为什么 RAG 文档会提到 Chunk？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["文本分块策略", "小块检索更精准"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer: 根据提供的上下文，文本分块（Chunk）是指将一段较长的文本分割成较小的、更易于管理的部分。在 RAG 文档中， Chunk 被提及为一个特定的文本单位，例如 Chunk 3（500 字）和 Chunk 4（500 字），这表明 RAG 文档将内容分割成这些较小的部分，以便更好地组织和呈现信息。然而，上下文并没有提供有关文本分块的详细定义或其背后的技术原理，只是提到了它在 RAG 文档中的应用。因此，关于文本分块的详细信息和技术细节，提供的上下文并不完整。
- expected_keywords: `["分块", "Chunk", "Token 限制", "检索更精准"]`
- keyword_hit: `2/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.4159863293170929,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "0 字）：技术原理 Chunk 3（500 字）：应用场景 Chunk 4（500 字）：总结 4. 提示词工程（Prompt Engineering） RAG 系统需要精心设计提示词，将检索到的上下文和问题结合。 4.1 典型 Prompt 模板 你是一个专业的技术助手。请根据以下文档内容回答用户的问题。",
    "metadata": {
      "chunkIndex": 4.0,
      "kbId": 6.0,
      "startIndex": 1397.0,
      "endIndex": 1550.0,
      "documentId": 30.0
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
- answer: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试
- expected_keywords: `["控制反转", "依赖注入", "构造器注入"]`
- keyword_hit: `0/3`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.5036572813987732,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "Spring 框架 2.1 Spring IOC（控制反转） 依赖注入的三种方式 ： 构造器注入（推荐，保证不可变性） Setter 注入（可选依赖） 字段注入（不推荐，难以测试） Bean 的生命周期 ： 实例化 属性赋值 初始化（@PostConstruct） 使用 销毁（@PreDestroy） 2.2 Spring AOP（面向切面编程） 应用场景 ：事务管理、日志记录、权限校验、性能监控 核心概念 ： Aspect（切面）：横切关注点的模块化 Join Point（连接点）：方法执行点 Pointcut（切入点）：匹配规则 Advice（通知）：@Before、@After、@Aro...",
    "metadata": {
      "chunkIndex": 3.0,
      "kbId": 6.0,
      "startIndex": 603.0,
      "endIndex": 945.0,
      "documentId": 32.0
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
- errors: `QA returned error status: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试`

### definition-006 (definition)

- id: `definition-006`
- type: `definition`
- question: Spring AOP 是什么，适合用在哪些场景？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["Spring AOP（面向切面编程）", "应用场景"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `1`
- answer: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试
- expected_keywords: `["面向切面编程", "事务管理", "日志记录", "权限校验"]`
- keyword_hit: `0/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.4810759127140045,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "Spring 框架 2.1 Spring IOC（控制反转） 依赖注入的三种方式 ： 构造器注入（推荐，保证不可变性） Setter 注入（可选依赖） 字段注入（不推荐，难以测试） Bean 的生命周期 ： 实例化 属性赋值 初始化（@PostConstruct） 使用 销毁（@PreDestroy） 2.2 Spring AOP（面向切面编程） 应用场景 ：事务管理、日志记录、权限校验、性能监控 核心概念 ： Aspect（切面）：横切关注点的模块化 Join Point（连接点）：方法执行点 Pointcut（切入点）：匹配规则 Advice（通知）：@Before、@After、@Aro...",
    "metadata": {
      "chunkIndex": 3.0,
      "kbId": 6.0,
      "startIndex": 603.0,
      "endIndex": 945.0,
      "documentId": 32.0
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
- errors: `QA returned error status: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试`

### definition-007 (definition)

- id: `definition-007`
- type: `definition`
- question: 什么是缓存穿透？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["缓存穿透：查询不存在的数据", "布隆过滤器、缓存空值"]`
- retrieve hit ratio: `1/2`
- first_match_rank: `1`
- answer: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试
- expected_keywords: `["缓存穿透", "不存在的数据", "布隆过滤器", "缓存空值"]`
- keyword_hit: `0/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.341765820980072,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "IALIZABLE（串行化） 锁机制 ： 行锁：锁定某一行 表锁：锁定整个表 间隙锁：防止幻读 6. Redis 6.1 数据类型 String ：缓存、计数器、分布式锁 Hash ：对象存储（用户信息） List ：消息队列、列表分页 Set ：去重、交集并集差集 ZSet ：排行榜、延迟队列 6.2 缓存问题 缓存穿透 ：查询不存在的数据，缓存和数据库都没有 解决：布隆过滤器、缓存空值 缓存击穿 ：热点 Key 过期，大量请求直达数据库 解决：热点 Key 永不过期、互斥锁 缓存雪崩 ：大量 Key 同时过期 解决：过期时间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AO...",
    "metadata": {
      "chunkIndex": 7.0,
      "kbId": 6.0,
      "startIndex": 1871.0,
      "endIndex": 2187.0,
      "documentId": 32.0
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
- errors: `QA returned error status: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试`

### definition-008 (definition)

- id: `definition-008`
- type: `definition`
- question: 什么是分布式锁，文档中列出了哪些实现方式？
- expected_sources: `["java-interview-guide.md"]`
- expected_contexts.contains: `["分布式锁", "Redis SETNX + 过期时间"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `1`
- answer: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试
- expected_keywords: `["Redis SETNX", "Redisson", "Zookeeper", "过期时间"]`
- keyword_hit: `0/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.40275147557258606,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "间加随机值、多级缓存 6.3 持久化 RDB ：快照，定时备份 AOF ：命令日志，实时性更好 7. 分布式 7.1 分布式锁 实现方式 ： Redis SETNX + 过期时间 Redisson（推荐） Zookeeper 注意事项 ： 防止死锁（设置过期时间） 防止误删（UUID 标识） 原子性（Lua 脚本） 7.2 分布式事务 2PC（两阶段提交） ： 准备阶段、提交阶段 缺点：阻塞、单点故障 TCC（Try-Confirm-Cancel） ： 业务侵入性强 性能较好 Seata ：阿里开源分布式事务框架 8. 消息队列 8.1 RabbitMQ / Kafka 应用场景 ： 异步处理...",
    "metadata": {
      "chunkIndex": 8.0,
      "kbId": 6.0,
      "startIndex": 2137.0,
      "endIndex": 2505.0,
      "documentId": 32.0
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
- errors: `QA returned error status: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试`

### reasoning-001 (reasoning)

- id: `reasoning-001`
- type: `reasoning`
- question: 为什么 RAG 系统需要把文档分块？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["为什么需要分块？", "提高检索效率"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试
- expected_keywords: `["Token 限制", "检索更精准", "检索效率", "语义割裂"]`
- keyword_hit: `0/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.4213184416294098,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "n-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估...",
    "metadata": {
      "chunkIndex": 10.0,
      "kbId": 6.0,
      "startIndex": 2791.0,
      "endIndex": 3227.0,
      "documentId": 30.0
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
- errors: `QA returned error status: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试`

### reasoning-002 (reasoning)

- id: `reasoning-002`
- type: `reasoning`
- question: 为什么 RAG 在知识更新和可解释性上通常比微调更适合企业知识库？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["RAG vs 微调", "知识更新"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `1`
- answer: RAG 在知识更新和可解释性上通常比微调更适合企业知识库，因为 RAG 支持实时更新文档，且具有高可解释性，能够追溯来源。相比之下，微调需要重新训练模型，并且其可解释性较低。这使得 RAG 更适合需要频繁更新知识和追溯信息来源的企业知识库。同时，RAG 的低成本（只需 API 调用）也是一个重要的优势。因此，结合 RAG 和微调可以发挥两者的优势，实现最佳实践。
- expected_keywords: `["实时更新文档", "成本低", "可追溯来源", "可解释性高"]`
- keyword_hit: `1/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.490170419216156,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "U） 延迟 ：端到端响应时间 6. 常见问题 Q1: RAG vs 微调（Fine-tuning）？ | 对比项 | RAG | 微调 | |--------|-----|------| | 知识更新 | 实时更新文档即可 | 需要重新训练 | | 成本 | 低（只需 API 调用） | 高（需要 GPU 训练） | | 可解释性 | 高（可追溯来源） | 低 | | 专业能力 | 适合知识密集型 | 适合特定任务 | 最佳实践 ：RAG + 微调结合使用。 Q2: 如何提高 RAG 准确率？ 优化分块策略 ：合理设置 Chunk 大小 提升 Embedding 质量 ：选择合适的模型 增加 ...",
    "metadata": {
      "chunkIndex": 8.0,
      "kbId": 6.0,
      "startIndex": 1971.0,
      "endIndex": 2449.0,
      "documentId": 30.0
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

### reasoning-003 (reasoning)

- id: `reasoning-003`
- type: `reasoning`
- question: 为什么 RAG 检索后还需要 Rerank？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["重排序（Rerank）", "二次精排，提高准确率"]`
- retrieve hit ratio: `0/2`
- first_match_rank: `-`
- answer: 根据提供的上下文，RAG（检索增强生成）技术结合了信息检索和文本生成。它先从知识库中检索相关信息，然后将这些信息作为上下文提供给大语言模型（LLM），最后LLM基于上下文生成准确的答案。然而，文中并没有明确解释为什么RAG检索后还需要Rerank（重新排名）。可能的原因是，检索出的信息可能有多个相关结果，需要通过重新排名来确定哪些信息最相关、最准确，以便提供给LLM生成最准确的答案。但是，由于上下文中没有直接提到Rerank的必要性和作用，因此这个问题的答案在提供的上下文中不完全明确。
- expected_keywords: `["重排序", "二次精排", "提高准确率", "检索优化"]`
- keyword_hit: `0/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.4292636215686798,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "chunkIndex": 0.0,
      "kbId": 6.0,
      "startIndex": 0.0,
      "endIndex": 270.0,
      "documentId": 30.0
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

### reasoning-004 (reasoning)

- id: `reasoning-004`
- type: `reasoning`
- question: 为什么 Spring 中更推荐构造器注入而不推荐字段注入？
- expected_sources: `["springboot-basics.md", "java-interview-guide.md"]`
- expected_contexts.contains: `["构造器注入（推荐，保证不可变性）", "字段注入（不推荐）"]`
- retrieve hit ratio: `1/2`
- first_match_rank: `1`
- answer: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试
- expected_keywords: `["构造器注入", "保证不可变性", "字段注入", "难以测试"]`
- keyword_hit: `0/4`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.5260632634162903,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "Spring 框架 2.1 Spring IOC（控制反转） 依赖注入的三种方式 ： 构造器注入（推荐，保证不可变性） Setter 注入（可选依赖） 字段注入（不推荐，难以测试） Bean 的生命周期 ： 实例化 属性赋值 初始化（@PostConstruct） 使用 销毁（@PreDestroy） 2.2 Spring AOP（面向切面编程） 应用场景 ：事务管理、日志记录、权限校验、性能监控 核心概念 ： Aspect（切面）：横切关注点的模块化 Join Point（连接点）：方法执行点 Pointcut（切入点）：匹配规则 Advice（通知）：@Before、@After、@Aro...",
    "metadata": {
      "chunkIndex": 3.0,
      "kbId": 6.0,
      "startIndex": 603.0,
      "endIndex": 945.0,
      "documentId": 32.0
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
- errors: `QA returned error status: 抱歉，处理您的问题时发生错误：模型服务当前不稳定（重试耗尽），请稍后重试`

### multi-hop-001 (multi_hop)

- id: `multi-hop-001`
- type: `multi_hop`
- question: 请把 RAG 的离线文档处理和在线问答处理串成一条完整链路。
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["文档处理（离线）", "问答处理（在线）"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `1`
- answer: RAG 的完整链路包括离线文档处理和在线问答处理。首先，在离线文档处理阶段，文档上传后会被解析提取文本内容，然后分块成小块（通常500-2000字符），接着使用Embedding模型将这些文本转换为向量，并存储到向量数据库中。在线问答处理阶段，用户的问题会被转换为向量，然后在向量数据库中搜索最相似的文本块，构建提示词，将检索到的内容和问题组合成Prompt，最后调用大模型生成答案并返回结果。
- expected_keywords: `["文档上传", "文档解析", "文本分块", "向量化", "相似度检索", "Prompt", "LLM"]`
- keyword_hit: `2/7`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.40575239062309265,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "rag-technology-guide.md",
    "documentId": 30,
    "chunkId": "0",
    "contentPreview": "检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召回相关文档 → 构建提示词 → LLM 生成答案 → 返回结果 2.1 详细步骤 Step 1: 文档处理（离线） 文档上传 ：PDF、Markdown、Word、代码文件等 文档解析 ：提取文本内容 文本分块 ：将长文档切分为小块（Chunk），通常 500-2000 字符 向量化 ：使用 Embedding 模型将文本转换为向量 存储 ：向量存入向量数据库（Milvus、Pinecone 等） Step 2: 问答处理（在线） 问...",
    "metadata": {
      "chunkIndex": 1.0,
      "kbId": 6.0,
      "startIndex": 220.0,
      "endIndex": 684.0,
      "documentId": 30.0
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
- answer: 根据提供的上下文，Java 面试文档和 Spring Boot 文档中关于接口性能优化的缓存相关手段是使用缓存（Redis）。
- expected_keywords: `["Redis", "使用缓存", "@Cacheable", "@CacheEvict", "@CachePut"]`
- keyword_hit: `2/5`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 0.5170324444770813,
    "source": "0",
    "sourceFileName": null,
    "documentTitle": "java-interview-guide.md",
    "documentId": 32,
    "chunkId": "0",
    "contentPreview": "ApplicationContext 刷新容器（refresh） 启动完成 2. 如何解决跨域问题？ @CrossOrigin 注解 CORS 全局配置 Nginx 反向代理 3. 接口性能优化手段？ 使用缓存（Redis） 数据库索引优化 异步处理（消息队列） 分页查询 SQL 优化（避免全表扫描） 连接池复用 4. 如何保证接口幂等性？ 唯一 ID + Redis Token 机制 乐观锁（版本号） 状态机（订单状态） 5. 如何实现限流？ 计数器 滑动窗口 令牌桶（Guava RateLimiter） 漏桶算法 --- 祝你面试顺利！🎉",
    "metadata": {
      "chunkIndex": 10.0,
      "kbId": 6.0,
      "startIndex": 2887.0,
      "endIndex": 3161.0,
      "documentId": 32.0
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

## No-answer Cases

### no-answer-001 (no_answer)

- id: `no-answer-001`
- type: `no_answer`
- question: Kubernetes 的 livenessProbe 和 readinessProbe 有什么区别？
- expected_sources: `[]`
- expected_contexts.contains: `[]`
- retrieve hit ratio: `0/0`
- first_match_rank: `-`
- answer: 抱歉，未能找到与您问题相关的信息。请尝试换一种方式提问或提供更多细节。
- expected_keywords: `[]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[]
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
- answer: 抱歉，未能找到与您问题相关的信息。请尝试换一种方式提问或提供更多细节。
- expected_keywords: `[]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[]
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
- answer: 抱歉，未能找到与您问题相关的信息。请尝试换一种方式提问或提供更多细节。
- expected_keywords: `[]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[]
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
| fact-001 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics |  |
| fact-002 | springboot-basics.md => springboot basics, springbootbasics; java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, springboot basics, springbootbasics |  |
| fact-003 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics |  |
| fact-004 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics |  |
| fact-005 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics |  |
| fact-006 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide |  |
| fact-007 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide |  |
| fact-008 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide |  |
| fact-009 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide |  |
| fact-010 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide |  |
| definition-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide |  |
| definition-002 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide |  |
| definition-003 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide |  |
| definition-004 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide |  |
| definition-005 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide |  |
| definition-006 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide |  |
| definition-007 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide |  |
| definition-008 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide |  |
| reasoning-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide |  |
| reasoning-002 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide |  |
| reasoning-003 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide |  |
| reasoning-004 | springboot-basics.md => springboot basics, springbootbasics; java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide |  |
| reasoning-005 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide |  |
| reasoning-006 | java-interview-guide.md => java interview guide, javainterviewguide | r1: 0, java interview guide, javainterviewguide |  |
| multi-hop-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide |  |
| multi-hop-002 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide |  |
| multi-hop-003 | java-interview-guide.md => java interview guide, javainterviewguide; springboot-basics.md => springboot basics, springbootbasics | r1: 0, java interview guide, javainterviewguide |  |
| no-answer-001 |  |  |  |
| no-answer-002 |  |  |  |
| no-answer-003 |  |  |  |

## Citation Diagnostics

| ID | Returned | Source Hits | Snippet Hits | Validation | Unsupported |
|---|---:|---:|---:|---|---:|
| fact-001 | 0 | 0/1 | - | valid=None, dropped=None, coverage=None | 0 |
| fact-002 | 0 | 0/2 | - | valid=0, dropped=0, coverage=1.0 | 0 |
| fact-003 | 0 | 0/1 | - | valid=0, dropped=0, coverage=1.0 | 0 |
| fact-004 | 0 | 0/1 | - | valid=0, dropped=0, coverage=1.0 | 0 |
| fact-005 | 0 | 0/1 | - | valid=0, dropped=0, coverage=1.0 | 0 |
| fact-006 | 0 | 0/1 | - | valid=0, dropped=0, coverage=1.0 | 0 |
| fact-007 | 0 | 0/1 | - | valid=0, dropped=0, coverage=1.0 | 0 |
| fact-008 | 0 | 0/1 | - | valid=0, dropped=0, coverage=1.0 | 0 |
| fact-009 | 0 | 0/1 | - | valid=0, dropped=0, coverage=1.0 | 0 |
| fact-010 | 0 | 0/1 | - | valid=0, dropped=0, coverage=1.0 | 0 |
| definition-001 | 0 | 0/1 | - | valid=None, dropped=None, coverage=None | 0 |
| definition-002 | 0 | 0/1 | - | valid=0, dropped=0, coverage=1.0 | 0 |
| definition-003 | 0 | 0/1 | - | valid=None, dropped=None, coverage=None | 0 |
| definition-004 | 0 | 0/1 | - | valid=0, dropped=0, coverage=1.0 | 0 |
| definition-005 | 0 | 0/1 | - | valid=None, dropped=None, coverage=None | 0 |
| definition-006 | 0 | 0/1 | - | valid=None, dropped=None, coverage=None | 0 |
| definition-007 | 0 | 0/1 | - | valid=None, dropped=None, coverage=None | 0 |
| definition-008 | 0 | 0/1 | - | valid=None, dropped=None, coverage=None | 0 |
| reasoning-001 | 0 | 0/1 | - | valid=None, dropped=None, coverage=None | 0 |
| reasoning-002 | 0 | 0/1 | - | valid=0, dropped=0, coverage=1.0 | 0 |
| reasoning-003 | 0 | 0/1 | - | valid=0, dropped=0, coverage=1.0 | 0 |
| reasoning-004 | 0 | 0/2 | - | valid=None, dropped=None, coverage=None | 0 |
| reasoning-005 | 0 | 0/1 | - | valid=0, dropped=0, coverage=1.0 | 0 |
| reasoning-006 | 0 | 0/1 | - | valid=0, dropped=0, coverage=1.0 | 0 |
| multi-hop-001 | 0 | 0/1 | - | valid=0, dropped=0, coverage=1.0 | 0 |
| multi-hop-002 | 0 | 0/1 | - | valid=0, dropped=0, coverage=1.0 | 0 |
| multi-hop-003 | 0 | 0/2 | - | valid=0, dropped=0, coverage=1.0 | 0 |
| no-answer-001 | 0 | - | - | valid=0, dropped=0, coverage=1.0 | 0 |
| no-answer-002 | 0 | - | - | valid=0, dropped=0, coverage=1.0 | 0 |
| no-answer-003 | 0 | - | - | valid=0, dropped=0, coverage=1.0 | 0 |
