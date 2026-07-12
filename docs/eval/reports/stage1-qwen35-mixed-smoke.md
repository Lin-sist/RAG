# RAG Eval Report

- Generated at: 2026-07-12T06:37:07.161832+00:00
- Report status: `CLEAN`
- askErrors count: `0`
- retrieveErrors count: `0`
- skippedAsk count: `0`
- judgeErrors count: `0`
- skippedJudge count: `5`
- rateLimitErrors count: `0`
- retry count: `0`
- Metrics safe for comparison: `yes`
- Base URL: `http://localhost:8080`
- Knowledge base ID: `11`
- Eval set: `docs\eval\rag_eval_set.jsonl`
- sampleIds: `fact-001,definition-001,reasoning-001,multi-hop-001,no-answer-001`
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
- Duration: `140.06s`

## Summary Metrics

| Metric | Value |
|---|---:|
| Samples | 5 |
| Answerable samples | 4 |
| No-answer samples | 1 |
| Recall@3 | 87.50% |
| Recall@5 | 87.50% |
| MRR | 1.0000 |
| Top1 source accuracy | 100.00% |
| Ask successful samples | 5 |
| Answerable ask successful samples | 4 |
| No-answer ask successful samples | 1 |
| Answer keyword hit rate on successful ask samples | 68.42% (13/19) |
| Citation hit rate on successful ask samples | 100.00% (4/4) |
| Citation source hit rate on successful ask samples | 100.00% (4/4) |
| Citation snippet hit rate on successful ask samples | 100.00% (10/10) |
| Unsupported citation count | 0 |
| No-answer citation violation count | 0 |
| No-answer accuracy on successful ask samples | 100.00% (1/1) |
| Judge evaluable samples | 0 |
| LLM judge pass rate | skipped (0/0) |
| Faithfulness average | skipped |
| Relevance average | skipped |

## Sample Results

| ID | Type | Retrieve | First Match | Ask | Keyword Hit | Citation Source | Citation Snippet | Unsupported Citations | No-answer OK | Judge | Errors |
|---|---|---:|---:|---|---:|---:|---:|---:|---:|---|---|
| fact-001 | fact | 2/2 | 1 | ok | 4/4 | 1/1 | 2/2 | 0 | - | skipped |  |
| definition-001 | definition | 1/2 | 1 | ok | 4/4 | 1/1 | 2/2 | 0 | - | skipped |  |
| reasoning-001 | reasoning | 2/2 | 1 | ok | 2/4 | 1/1 | 3/3 | 0 | - | skipped |  |
| multi-hop-001 | multi_hop | 2/2 | 1 | ok | 3/7 | 1/1 | 3/3 | 0 | - | skipped |  |
| no-answer-001 | no_answer | - | - | ok | - | - | - | 0 | yes | skipped |  |

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

## Failed Citation Cases

No cases.

## Low Answer Keyword Hit Cases

### reasoning-001 (reasoning)

- id: `reasoning-001`
- type: `reasoning`
- question: 为什么 RAG 系统需要把文档分块？
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["为什么需要分块？", "提高检索效率"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `1`
- answer: RAG 系统需要对文档进行分块，主要原因如下： 1. **LLM 的 Token 限制**：大语言模型（如 GPT-3.5 和 GPT-4）对输入长度有限制（例如 GPT-3.5 为 4096 Token，GPT-4 为 8192 或 32768 Token），无法一次性处理过长的文档。
2. **提高检索精准度**：将文档切分为小块后，检索系统可以更精确地定位与问题相关的片段，避免无关信息干扰。
3. **提升检索效率**：小块数据更易于向量化和索引，从而加快检索速度，降低计算资源消耗。 常见的分块方法包括固定长度分块、按句子或段落分块、滑动窗口分块以及基于语义的分块等，可根据文档类型灵活选择。
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

### multi-hop-001 (multi_hop)

- id: `multi-hop-001`
- type: `multi_hop`
- question: 请把 RAG 的离线文档处理和在线问答处理串成一条完整链路。
- expected_sources: `["rag-technology-guide.md"]`
- expected_contexts.contains: `["文档处理（离线）", "问答处理（在线）"]`
- retrieve hit ratio: `2/2`
- first_match_rank: `1`
- answer: RAG 的完整链路如下： **离线文档处理阶段**：首先上传 PDF、Markdown、Word 或代码文件等文档，接着解析提取文本内容，然后将长文档切分为 500-2000 字符的小块（Chunk），使用 Embedding 模型将文本转换为向量，最后将向量存入 Milvus、Pinecone 等向量数据库中。 **在线问答处理阶段**：当用户提问时，先将问题转换为向量，在向量数据库中搜索最相似的文本块（Top-K），将检索到的内容与问题组合成 Prompt 构建提示词，调用大模型（LLM）生成答案，最后返回答案及引用来源。 整个流程概括为：用户提问 → 向量化 → 向量检索 → 召回相关文档 → 构建提示词 → LLM 生成答案 → 返回结果。
- expected_keywords: `["文档上传", "文档解析", "文本分块", "向量化", "相似度检索", "Prompt", "LLM"]`
- keyword_hit: `3/7`
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
    "contentPreview": "提供给 LLM LLM 基于上下文生成 准确的答案 2. RAG 架构流程 用户提问 → 向量化 → 向量检索 → 召回相关文档 → 构建提示词 → LLM 生成答案 → 返回结果 2.1 详细步骤 Step 1: 文档处理（离线） 文档上传 ：PDF、Markdown、Word、代码文件等 文档解析 ：提取文本内容 文本分块 ：将长文档切分为小块（Chunk），通常 500-2000 字符 向量化 ：使用 Embedding 模型将文本转换为向量 存储 ：向量存入向量数据库（Milvus、Pinecone 等） Step 2: 问答处理（在线） 问题向量化 ：用户问题转换为向量 相似度检索 ...",
    "metadata": {
      "chunkIndex": 1.0,
      "tokenCount": 389.0,
      "headingPath": "",
      "sourceFileName": "rag-technology-guide.md",
      "documentId": 37.0,
      "kbId": 11.0,
      "startIndex": 241.0,
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
    "rank": 3,
    "score": 0.016129031777381897,
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
    "snippet": "1 详细步骤 Step 1: 文档处理（离线） 文档上传 ：PDF、Markdown、Word、代码文件等 文档解析 ：提取文本内容 文本分块 ：将长文档切分为小块（Chunk），通常 500-2000 字符 向量化 ：使用 Embedding 模型将文本转换为向量 存储 ：向量存入向量数据库（Milvus、Pinecone 等） Step 2: 问答处理（"
  },
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
    "source": "d76c3fc0-ef54-4e0e-84b3-ef38b25607f1",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 37,
    "chunkId": "d76c3fc0-ef54-4e0e-84b3-ef38b25607f1",
    "score": 0.016129031777381897,
    "snippet": "A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈 关键指标 ： 检索召回率：Top-K 包含正确答案的比例 答案准确率：生成答案的正确性 用户满意度：点赞率、采纳率 --- RAG 是 AI 应用的核心技术之一，掌握它能大大提升你的竞争力！"
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

## Source Normalization Diagnostics

| ID | Expected normalized | Retrieved candidate normalized | Citation candidate normalized |
|---|---|---|---|
| fact-001 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: 4ba6b2a6 5323 4fb4 8d04 d9e0c83479a6, 4ba6b2a653234fb48d04d9e0c83479a6, springboot basics, springbootbasics; r3: 9eea1fbd f499 452c bbb0 de6ee1de6247, 9eea1fbdf499452cbbb0de6ee1de6247, springboot basics, springbootbasics; r4: 0a027708 629e 408e 87ff e2a96c1fccd8, 0a027708629e408e87ffe2a96c1fccd8, java interview guide, javainterviewguide; r5: 4c61d28b 7f57 4679 bcc4 3d9aca5dbe61, 4c61d28b7f574679bcc43d9aca5dbe61, java interview guide, javainterviewguide | 0, springboot basics, springbootbasics; 4ba6b2a6 5323 4fb4 8d04 d9e0c83479a6, 4ba6b2a653234fb48d04d9e0c83479a6, springboot basics, springbootbasics |
| definition-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 901d738b 2d63 4b11 9c91 d9aa39fb8860, 901d738b2d634b119c91d9aa39fb8860, rag technology guide, ragtechnologyguide; r3: 8d9e62d7 8f5e 4284 bf6e c50a1fab03aa, 8d9e62d78f5e4284bf6ec50a1fab03aa, rag technology guide, ragtechnologyguide; r4: 926499a3 c300 4daf a598 e80534fee04c, 926499a3c3004dafa598e80534fee04c, rag technology guide, ragtechnologyguide; r5: 2fc312fe bd9e 45a6 bfdf 29d93b357ea5, 2fc312febd9e45a6bfdf29d93b357ea5, rag technology guide, ragtechnologyguide | 0, rag technology guide, ragtechnologyguide; 901d738b 2d63 4b11 9c91 d9aa39fb8860, 901d738b2d634b119c91d9aa39fb8860, rag technology guide, ragtechnologyguide |
| reasoning-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 8d9e62d7 8f5e 4284 bf6e c50a1fab03aa, 8d9e62d78f5e4284bf6ec50a1fab03aa, rag technology guide, ragtechnologyguide; r2: 37bb6c85 b083 42a4 b849 890dbf989082, 37bb6c85b08342a4b849890dbf989082, rag technology guide, ragtechnologyguide; r3: 2fc312fe bd9e 45a6 bfdf 29d93b357ea5, 2fc312febd9e45a6bfdf29d93b357ea5, rag technology guide, ragtechnologyguide; r4: 4b007ce2 dd75 47c1 acd3 e154ef86c7fc, 4b007ce2dd7547c1acd3e154ef86c7fc, rag technology guide, ragtechnologyguide; r5: 0, rag technology guide, ragtechnologyguide | 8d9e62d7 8f5e 4284 bf6e c50a1fab03aa, 8d9e62d78f5e4284bf6ec50a1fab03aa, rag technology guide, ragtechnologyguide; 0, rag technology guide, ragtechnologyguide; 2fc312fe bd9e 45a6 bfdf 29d93b357ea5, 2fc312febd9e45a6bfdf29d93b357ea5, rag technology guide, ragtechnologyguide |
| multi-hop-001 | rag-technology-guide.md => rag technology guide, ragtechnologyguide | r1: 0, rag technology guide, ragtechnologyguide; r2: 77017a1a 4e1b 4092 bc23 8433fb9447f4, 77017a1a4e1b4092bc238433fb9447f4, rag technology guide, ragtechnologyguide; r3: d76c3fc0 ef54 4e0e 84b3 ef38b25607f1, d76c3fc0ef544e0e84b3ef38b25607f1, rag technology guide, ragtechnologyguide; r4: 37bb6c85 b083 42a4 b849 890dbf989082, 37bb6c85b08342a4b849890dbf989082, rag technology guide, ragtechnologyguide; r5: 8b5df374 b365 41d3 8322 579cc48c0757, 8b5df374b36541d38322579cc48c0757, rag technology guide, ragtechnologyguide | 0, rag technology guide, ragtechnologyguide; 77017a1a 4e1b 4092 bc23 8433fb9447f4, 77017a1a4e1b4092bc238433fb9447f4, rag technology guide, ragtechnologyguide; d76c3fc0 ef54 4e0e 84b3 ef38b25607f1, d76c3fc0ef544e0e84b3ef38b25607f1, rag technology guide, ragtechnologyguide |
| no-answer-001 |  | r1: 901d738b 2d63 4b11 9c91 d9aa39fb8860, 901d738b2d634b119c91d9aa39fb8860, rag technology guide, ragtechnologyguide; r2: 37bb6c85 b083 42a4 b849 890dbf989082, 37bb6c85b08342a4b849890dbf989082, rag technology guide, ragtechnologyguide; r3: d76c3fc0 ef54 4e0e 84b3 ef38b25607f1, d76c3fc0ef544e0e84b3ef38b25607f1, rag technology guide, ragtechnologyguide; r4: 4b007ce2 dd75 47c1 acd3 e154ef86c7fc, 4b007ce2dd7547c1acd3e154ef86c7fc, rag technology guide, ragtechnologyguide; r5: 8d9e62d7 8f5e 4284 bf6e c50a1fab03aa, 8d9e62d78f5e4284bf6ec50a1fab03aa, rag technology guide, ragtechnologyguide |  |

## Citation Diagnostics

| ID | Returned | Source Hits | Snippet Hits | Validation | Unsupported |
|---|---:|---:|---:|---|---:|
| fact-001 | 2 | 1/1 | 2/2 | valid=2, dropped=0, coverage=1.0 | 0 |
| definition-001 | 2 | 1/1 | 2/2 | valid=2, dropped=0, coverage=1.0 | 0 |
| reasoning-001 | 3 | 1/1 | 3/3 | valid=3, dropped=0, coverage=1.0 | 0 |
| multi-hop-001 | 3 | 1/1 | 3/3 | valid=3, dropped=0, coverage=1.0 | 0 |
| no-answer-001 | 0 | - | - | valid=0, dropped=0, coverage=1.0 | 0 |
