# RAG 检索评测与调试指南

## 1. 这个 eval set 的目的

这个评测集不是为了马上衡量“答案好不好”，而是为了固定一个可对比的检索基线。你后面调整 chunk、rerank、query rewrite、minScore、topK 的时候，可以直接看召回结果有没有变化。

核心目标是看清楚三件事：

- 某个问题到底召回了哪些 chunks
- 每个 chunk 的 score、source、snippet、metadata 是什么
- 参数变化后，检索结果有没有更接近你的预期

## 2. 文件字段含义

`docs/eval/rag_eval_set.jsonl` 里每一行都是一个 JSON 对象，建议字段如下：

- `id`：评测样本编号，方便你在笔记里引用
- `question`：真实问题，后续会拿它调试检索
- `type`：问题类型，例如 fact、explanation、summary、procedure、comparison、multi_hop、no_answer
- `difficulty`：主观难度，便于分层统计
- `expected_keywords`：你希望检索结果里出现的关键词
- `expected_sources`：你希望召回的来源文档或文档名
- `notes`：为什么选这个问题
- `status`：当前状态，模板阶段建议先保持 todo

## 3. 如何选择 10 到 20 个真实问题

建议优先选这几类问题：

- 事实型：某个概念、配置、参数、接口名
- 解释型：为什么这么设计、原理是什么
- 流程型：某个功能的步骤或链路
- 对比型：A 和 B 的差异、优缺点
- 组合型：需要从多个片段拼起来回答的问题
- 无答案型：知识库里本来就不该有答案的问题

选择时尽量覆盖不同文档、不同长度、不同粒度的问题。不要只挑最简单的问题，否则你看不出检索的边界。

## 4. 如何调用 debug 接口

示例：

```bash
curl -X POST "http://localhost:8080/api/qa/debug/retrieve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{
    "kbId": 1,
    "question": "请替换为你的真实问题",
    "topK": 5,
    "minScore": 0.2,
    "enableRerank": true
  }'
```

你也可以把 `topK`、`minScore`、`enableRerank` 改成不同组合，观察召回结果是否变化。

## 5. 怎么判断问题出在检索还是生成

先只看 debug 接口返回的检索结果，再决定下一步看哪里。

- 如果 top results 完全不相关，优先看 chunk、embedding、query rewrite、minScore
- 如果 top results 相关但最终答案差，优先看 prompt、上下文组织、生成器
- 如果无答案问题仍然召回高分内容，优先检查 minScore 和 no-answer 策略
- 如果解释型问题只召回零散片段，考虑更大 chunk 或 parent-child chunk

## 6. 你应该怎么做基线

建议先固定一组参数：

- `topK = 5`
- `minScore = 0.3`
- `enableRerank = true`

然后只改一个变量，记录结果变化。例如：

- 只改 topK
- 只改 minScore
- 只关 rerank
- 只看某类问题

这样后面你才知道“结果变好了”到底是哪个参数带来的。

## 7. 实战建议

- 先用真实业务里常见的问题，不要只用课本式问题
- 每个样本记录你期望的 source 和关键词
- 先看召回，再看答案，不要反过来
- 如果要做对比，尽量一次只改一个变量

## 8. 当前版本的边界

这个接口只做检索，不调用 LLM，不保存历史，不增加知识库 queryCount。它的作用是给你一个可重复、可解释的检索观察窗口。
