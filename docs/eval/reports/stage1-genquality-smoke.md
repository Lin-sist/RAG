# RAG Eval Report

- Generated at: 2026-07-02T03:04:27.113143+00:00
- Report status: `PARTIAL`
- askErrors count: `2`
- retrieveErrors count: `0`
- skippedAsk count: `0`
- judgeErrors count: `0`
- skippedJudge count: `2`
- rateLimitErrors count: `0`
- retry count: `0`
- Metrics safe for comparison: `retrieval metrics only; generation/citation metrics are partial`
- Base URL: `http://localhost:8080`
- Knowledge base ID: `15`
- Eval set: `docs\eval\rag_eval_set.jsonl`
- sampleIds: `fact-001,no-answer-001`
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
- Eval KB vector collection: `kb_ff06e2ea3de24fb4`
- Eval KB document count: `3`
- Eval KB chunk count: `50`
- Fixture files: `springboot-basics.md sha256=c51df5761d510aa4c8a5fd610c90454b12973e2999138c3b57ba83768a296521; java-interview-guide.md sha256=a33f16e91025e9a8d92274c4221d1c172bb4f68c03790db53b8e20157ef4faa0; rag-technology-guide.md sha256=59ad5d66a59be2ce4e517ca81e37fde06b7673f3da7f798a4c76b01cc6f348a9`
- Config snapshot: `rag-admin\src\main\resources\application.yml sha256=12c403bfe01b4746c8a816e5b1f58d5c295fc826db58eeabc2716062f57793bf; rag-core\src\main\java\com\enterprise\rag\core\rag\query\RetrievalProperties.java sha256=0303394e8d11a2a88e39cdcb970323437145a6c3468f51d1f424d169399f4636; rag-document\src\main\java\com\enterprise\rag\document\chunker\ChunkConfig.java sha256=01b17712c8974055d15a30034e396d5074ba37ad8ea637fa7bca895a0c8a8554`
- Git HEAD: `e28bc9a3568713b3f83033a522b3df8f9ee6746a`
- askTimeout: `20.0`
- askDelaySeconds: `2.0`
- maxAskRetries: `0`
- retryBackoffSeconds: `10.0`
- retryAskTimeouts: `False`
- Duration: `48.80s`

## Summary Metrics

| Metric | Value |
|---|---:|
| Samples | 2 |
| Answerable samples | 1 |
| No-answer samples | 1 |
| Recall@3 | 100.00% |
| Recall@5 | 100.00% |
| MRR | 1.0000 |
| Top1 source accuracy | 100.00% |
| Ask successful samples | 0 |
| Answerable ask successful samples | 0 |
| No-answer ask successful samples | 0 |
| Answer keyword hit rate on successful ask samples | N/A (0/0) |
| Citation hit rate on successful ask samples | N/A (0/0) |
| Citation source hit rate on successful ask samples | N/A (0/0) |
| Citation snippet hit rate on successful ask samples | N/A (0/0) |
| Unsupported citation count | 0 |
| No-answer citation violation count | 0 |
| No-answer accuracy on successful ask samples | N/A (0/0) |
| Judge evaluable samples | 0 |
| LLM judge pass rate | skipped (0/0) |
| Faithfulness average | skipped |
| Relevance average | skipped |

## Sample Results

| ID | Type | Retrieve | First Match | Ask | Keyword Hit | Citation Source | Citation Snippet | Unsupported Citations | No-answer OK | Judge | Errors |
|---|---|---:|---:|---|---:|---:|---:|---:|---:|---|---|
| fact-001 | fact | 2/2 | 1 | error | - | - | - | 0 | - | skipped | timed out |
| no-answer-001 | no_answer | - | - | error | - | - | - | 0 | - | skipped | timed out |

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
- answer:
- expected_keywords: `[]`
- keyword_hit: `0/0`
- top5 retrieved results:
```json
[
  {
    "rank": 1,
    "score": 1.0,
    "source": "7ae47b81-30e4-4a02-a17d-416ff33f1411",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 56,
    "chunkId": "7ae47b81-30e4-4a02-a17d-416ff33f1411",
    "contentPreview": "RAG（检索增强生成）技术详解 1. 什么是 RAG？ RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合了 信息检索 和 文本生成 的 AI 技术架构。 1.1 核心思想 传统的大语言模型（LLM）依赖于训练时学到的知识，但存在以下问题： 知识截止 ：只知道训练时的数据 幻觉问题 ：可能生成不准确的内容 缺乏专业知识 ：无法回答企业内部或专业领域问题 RAG 的解决方案 ： 先从知识库中 检索 相关信息 将检索到的信息作为 上下文 提供给 LLM LLM 基于上下文生成 准确的答案 2.",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "kbId": 15,
      "fileName": "rag-technology-guide.md",
      "startIndex": 0,
      "title": "rag-technology-guide.md",
      "chunkIndex": 0,
      "tokenCount": 269,
      "headingPath": "",
      "endIndex": 270,
      "originalFilename": "rag-technology-guide.md",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 56
    }
  },
  {
    "rank": 2,
    "score": 0.7920469045639038,
    "source": "0c09d312-344a-4746-9fde-9ebfdb145b0c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 56,
    "chunkId": "0c09d312-344a-4746-9fde-9ebfdb145b0c",
    "contentPreview": "索相关法条 8. 技术栈示例（本项目） 前端：用户提问 ↓ Spring Boot 后端 ↓ 文档处理层：解析、分块 ↓ Embedding 服务：向量化（通义千问 API） ↓ Milvus 向量数据库：存储和检索 ↓ LLM 服务：生成答案（通义千问 Qwen-Turbo） ↓ 返回答案 + 引用来源 9. 面试常见问题 Q1: 为什么需要向量化？ A : 传统的关键词匹配无法理解语义。例如： 问题：\"如何学习 Java？\" 文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "kbId": 15,
      "fileName": "rag-technology-guide.md",
      "startIndex": 2640,
      "title": "rag-technology-guide.md",
      "chunkIndex": 12,
      "tokenCount": 319,
      "headingPath": "",
      "endIndex": 2959,
      "originalFilename": "rag-technology-guide.md",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 56
    }
  },
  {
    "rank": 3,
    "score": 0.4335298240184784,
    "source": "8b9c2406-6e0b-4c21-bc1c-7c7e98abb4d4",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 56,
    "chunkId": "8b9c2406-6e0b-4c21-bc1c-7c7e98abb4d4",
    "contentPreview": "文档：\"Java 学习指南\" 关键词匹配：匹配到\"Java\"和\"学习\" 向量匹配：理解\"如何学习\"和\"学习指南\"语义相似 Q2: Chunk 大小如何选择？ A : 太小 （< 200 字符）：语义不完整 太大 （> 2000 字符）：检索不精准，耗费 Token 推荐 ：500-1000 字符，根据文档类型调整 技术文档 ：按段落或章节分块 代码文档 ：按函数或类分块 Q3: 如何评估 RAG 系统效果？ A : 离线评估 ：准备测试问答对，计算准确率 在线评估 ：A/B 测试，收集用户反馈 关键指标 ： 检索召回率：Top-K 包含正确答案的比例 答案准确率：生成答案的正确性 用户满意度：...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "kbId": 15,
      "fileName": "rag-technology-guide.md",
      "startIndex": 2880,
      "title": "rag-technology-guide.md",
      "chunkIndex": 13,
      "tokenCount": 347,
      "headingPath": "",
      "endIndex": 3227,
      "originalFilename": "rag-technology-guide.md",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 56
    }
  },
  {
    "rank": 4,
    "score": 0.33610081672668457,
    "source": "0da3b551-7182-4a29-9650-25c7da40f95f",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 56,
    "chunkId": "0da3b551-7182-4a29-9650-25c7da40f95f",
    "contentPreview": ".., 0.12] (相似度低) 3.2 向量数据库 专门存储和检索向量的数据库，支持高效的相似度搜索。 常用数据库 ： Milvus ：开源，性能强，本项目使用 Pinecone ：托管服务 Weaviate ：支持混合搜索 Qdrant ：Rust 实现 检索算法 ： HNSW （层次化可导航小世界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "kbId": 15,
      "fileName": "rag-technology-guide.md",
      "startIndex": 961,
      "title": "rag-technology-guide.md",
      "chunkIndex": 4,
      "tokenCount": 232,
      "headingPath": "",
      "endIndex": 1193,
      "originalFilename": "rag-technology-guide.md",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 56
    }
  },
  {
    "rank": 5,
    "score": 0.2595656216144562,
    "source": "969cdcde-c573-4278-b539-2d3dcbcc919c",
    "sourceFileName": "rag-technology-guide.md",
    "documentTitle": "rag-technology-guide.md",
    "documentId": 56,
    "chunkId": "969cdcde-c573-4278-b539-2d3dcbcc919c",
    "contentPreview": "界图）：召回率高，速度快 IVF （倒排索引）：适合大规模数据 FLAT （暴力搜索）：精度最高但速度慢 3.3 文本分块策略 为什么需要分块？ LLM 有 Token 限制（GPT-3.5 是 4096，GPT-4 是 8192/32768） 小块检索更精准 提高检索效率 分块方法 ： 固定长度分块 ：每 500 字符一块 句子/段落分块 ：自然语言边界 滑动窗口 ：重叠分块，避免语义割裂 语义分块 ：使用 NLP 技术识别语义边界 示例 ： 原文档（2000 字） ↓ 分块 Chunk 1（500 字）：介绍部分 Chunk 2（500 字）：技术原理 Chunk 3（500 字）：应用场...",
    "metadata": {
      "sourceFileName": "rag-technology-guide.md",
      "kbId": 15,
      "fileName": "rag-technology-guide.md",
      "startIndex": 1121,
      "title": "rag-technology-guide.md",
      "chunkIndex": 5,
      "tokenCount": 383,
      "headingPath": "",
      "endIndex": 1504,
      "originalFilename": "rag-technology-guide.md",
      "documentTitle": "rag-technology-guide.md",
      "documentId": 56
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
- errors: `timed out`

## Source Normalization Diagnostics

| ID | Expected normalized | Retrieved candidate normalized | Citation candidate normalized |
|---|---|---|---|
| fact-001 | springboot-basics.md => springboot basics, springbootbasics | r1: 0, springboot basics, springbootbasics; r2: 36eeee8e 993b 4938 8c50 e8d92533af60, 36eeee8e993b49388c50e8d92533af60, springboot basics, springbootbasics; r3: 9a1b6e57 69c8 4427 ba11 3ab5b43e682d, 9a1b6e5769c84427ba113ab5b43e682d, springboot basics, springbootbasics; r4: 9173c97a f3ff 4993 a556 471af90429e4, 9173c97af3ff4993a556471af90429e4, java interview guide, javainterviewguide; r5: 127cc8d8 1d8f 4846 903c 3322fd95ffed, 127cc8d81d8f4846903c3322fd95ffed, java interview guide, javainterviewguide |  |
| no-answer-001 |  | r1: 7ae47b81 30e4 4a02 a17d 416ff33f1411, 7ae47b8130e44a02a17d416ff33f1411, rag technology guide, ragtechnologyguide; r2: 0c09d312 344a 4746 9fde 9ebfdb145b0c, 0c09d312344a47469fde9ebfdb145b0c, rag technology guide, ragtechnologyguide; r3: 8b9c2406 6e0b 4c21 bc1c 7c7e98abb4d4, 8b9c24066e0b4c21bc1c7c7e98abb4d4, rag technology guide, ragtechnologyguide; r4: 0da3b551 7182 4a29 9650 25c7da40f95f, 0da3b55171824a29965025c7da40f95f, rag technology guide, ragtechnologyguide; r5: 969cdcde c573 4278 b539 2d3dcbcc919c, 969cdcdec5734278b5392d3dcbcc919c, rag technology guide, ragtechnologyguide |  |

## Citation Diagnostics

| ID | Returned | Source Hits | Snippet Hits | Validation | Unsupported |
|---|---:|---:|---:|---|---:|
| fact-001 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
| no-answer-001 | 0 | - | - | valid=None, dropped=None, coverage=None | 0 |
