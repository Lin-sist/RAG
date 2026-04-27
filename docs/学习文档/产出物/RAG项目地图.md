# RAG 项目地图

> 用法：先写粗链路，不追求完整。每周补一次即可。

## 问答主链路

```text
QAController -> RAGServiceImpl -> QueryEngineImpl -> PromptBuilder -> AnswerGeneratorImpl
```

## 文档处理链路

```text
上传 -> 记录文档 -> 异步任务 -> 解析 -> 分块 -> 向量化 -> 写入向量库
```

## 当前最不确定的点

-

