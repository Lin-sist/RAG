# RAG Eval Report

- Generated at: 2026-07-12T06:34:40.314115+00:00
- Report status: `CLEAN`
- askErrors count: `0`
- retrieveErrors count: `0`
- skippedAsk count: `0`
- judgeErrors count: `0`
- skippedJudge count: `3`
- rateLimitErrors count: `0`
- retry count: `0`
- Metrics safe for comparison: `yes`
- Base URL: `http://localhost:8080`
- Knowledge base ID: `11`
- Eval set: `docs\eval\rag_eval_set.jsonl`
- sampleIds: `no-answer-001,no-answer-002,no-answer-003`
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
- maxAskRetries: `0`
- retryBackoffSeconds: `0.0`
- retryAskTimeouts: `False`
- Duration: `12.60s`

## Summary Metrics

| Metric | Value |
|---|---:|
| Samples | 3 |
| Answerable samples | 0 |
| No-answer samples | 3 |
| Recall@3 | 0.00% |
| Recall@5 | 0.00% |
| MRR | 0.0000 |
| Top1 source accuracy | 0.00% |
| Ask successful samples | 3 |
| Answerable ask successful samples | 0 |
| No-answer ask successful samples | 3 |
| Answer keyword hit rate on successful ask samples | N/A (0/0) |
| Citation hit rate on successful ask samples | N/A (0/0) |
| Citation source hit rate on successful ask samples | N/A (0/0) |
| Citation snippet hit rate on successful ask samples | N/A (0/0) |
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

No cases.

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
- answer: 知识库中没有足够信息回答该问题。
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
- answer: 知识库中没有足够信息回答该问题。
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
| no-answer-001 |  | r1: 901d738b 2d63 4b11 9c91 d9aa39fb8860, 901d738b2d634b119c91d9aa39fb8860, rag technology guide, ragtechnologyguide; r2: 37bb6c85 b083 42a4 b849 890dbf989082, 37bb6c85b08342a4b849890dbf989082, rag technology guide, ragtechnologyguide; r3: d76c3fc0 ef54 4e0e 84b3 ef38b25607f1, d76c3fc0ef544e0e84b3ef38b25607f1, rag technology guide, ragtechnologyguide; r4: 4b007ce2 dd75 47c1 acd3 e154ef86c7fc, 4b007ce2dd7547c1acd3e154ef86c7fc, rag technology guide, ragtechnologyguide; r5: 8d9e62d7 8f5e 4284 bf6e c50a1fab03aa, 8d9e62d78f5e4284bf6ec50a1fab03aa, rag technology guide, ragtechnologyguide |  |
| no-answer-002 |  | r1: bba6e34a 4290 4aba ab90 676e863fd5e8, bba6e34a42904abaab90676e863fd5e8, java interview guide, javainterviewguide; r2: 926499a3 c300 4daf a598 e80534fee04c, 926499a3c3004dafa598e80534fee04c, rag technology guide, ragtechnologyguide; r3: 8652a10b 44b4 49ce b978 21f27c87b785, 8652a10b44b449ceb97821f27c87b785, java interview guide, javainterviewguide; r4: d770ae16 5ca7 4ad8 b9f9 c42a8a31d225, d770ae165ca74ad8b9f9c42a8a31d225, rag technology guide, ragtechnologyguide; r5: 4b007ce2 dd75 47c1 acd3 e154ef86c7fc, 4b007ce2dd7547c1acd3e154ef86c7fc, rag technology guide, ragtechnologyguide |  |
| no-answer-003 |  | r1: 4ba6b2a6 5323 4fb4 8d04 d9e0c83479a6, 4ba6b2a653234fb48d04d9e0c83479a6, springboot basics, springbootbasics; r2: bba6e34a 4290 4aba ab90 676e863fd5e8, bba6e34a42904abaab90676e863fd5e8, java interview guide, javainterviewguide; r3: cb5551dd 5702 48d4 8669 fbc9bc0a4577, cb5551dd570248d48669fbc9bc0a4577, springboot basics, springbootbasics; r4: 901d738b 2d63 4b11 9c91 d9aa39fb8860, 901d738b2d634b119c91d9aa39fb8860, rag technology guide, ragtechnologyguide; r5: 77fe6cff 1436 4309 86ec befeb8d0bcab, 77fe6cff1436430986ecbefeb8d0bcab, springboot basics, springbootbasics |  |

## Citation Diagnostics

| ID | Returned | Source Hits | Snippet Hits | Validation | Unsupported |
|---|---:|---:|---:|---|---:|
| no-answer-001 | 0 | - | - | valid=0, dropped=0, coverage=1.0 | 0 |
| no-answer-002 | 0 | - | - | valid=0, dropped=0, coverage=1.0 | 0 |
| no-answer-003 | 0 | - | - | valid=0, dropped=0, coverage=1.0 | 0 |
