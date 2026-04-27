# RAG 30 天学习计划执行版（2026-04-27 重新开局）

> 版本：v0.2  
> 起点：2026-04-27  
> 结束：2026-05-22  
> 定位：这不是用来“读完”的计划，而是每天打开项目后直接执行的任务卡索引。  
> 核心原则：不补债，不重启，不追求每天完美。今天只负责把今天接上。

---

## 0. 这版计划解决什么问题

原来的计划内容足够完整，但启动成本偏高：每天打开后，还要自己判断看哪里、问 AI 什么、输出写到哪里。对完美主义者来说，只要前几天没完成，后面就容易变成“我已经失败了”。

这版计划改成更小的执行单元：

1. 每天只看少量指定文件。
2. 每天都有可直接复制给 AI 的提问。
3. 每天都明确输出到哪个文件。
4. 每天只保留一个明确的今日输出。
5. 漏一天不补，第二天照常从当天继续。

---

## 1. 每天固定工作流

每天打开项目后，只按这个顺序做。

1. 打开当天复盘文件：`docs/学习文档/复盘文档/日期.md`
2. 看“今天只打开”里的文件。
3. 复制“AI 逆向学习提问”，让 AI 只围绕当前仓库讲。
4. 自己写一点理解，不追求漂亮。
5. 把当天输出写回当天复盘文件。
6. 状态好时，再同步沉淀到 `docs/学习文档/产出物/`。

不允许先补昨天。昨天没做，就是历史数据，不是今天的债。

---

## 2. 输出位置

每日最小输出固定写到当天复盘文件：

- `docs/学习文档/复盘文档/4月27日.md`
- `docs/学习文档/复盘文档/4月28日.md`
- 以此类推

阶段性沉淀输出写到：

- `docs/学习文档/产出物/RAG术语表.md`
- `docs/学习文档/产出物/RAG项目地图.md`
- `docs/学习文档/产出物/面试问答卡.md`
- `docs/学习文档/产出物/2分钟项目介绍.md`
- `docs/学习文档/产出物/小改动记录.md`

如果状态差，只写当天复盘文件即可。

---

## 3. 完成标准

每天只有一个完成标准：写下当天的 `今日输出`。

输出可以很短，可以很粗糙，但必须是你自己写出来的东西。不要为了补完整表格而消耗启动力。

---

## 4. 断更恢复规则

如果某天没有完成：

1. 不补。
2. 不把昨天任务移到今天。
3. 不重做计划。
4. 今天只写今天的 `今日输出`。

当天复盘文件里写一句：

```text
今天不是补昨天，今天只是重新接上。
```

这句写了，就算已经重新启动。

---

## 5. 任务重新分配逻辑

4 月 22 日到 4 月 26 日已经过去，不再作为必须补完的内容。4 月 27 日开始重新进入项目：

- 4 月 27 日到 4 月 30 日：重新接上问答主链路
- 5 月 1 日到 5 月 4 日：假期保活，不排重任务
- 5 月 5 日到 5 月 10 日：吃透 RAG 问答链路和表达
- 5 月 11 日到 5 月 17 日：吃透文档处理、向量化、索引链路
- 5 月 18 日到 5 月 22 日：补后端基础、小改动、面试表达

---

# 6. 每日执行卡索引

## 第 1 段：重新接上问答主链路（4 月 27 日 - 4 月 30 日）

### 4 月 27 日（周一）：重新开局，不补债

今日主题：把计划重新接上，建立今天能启动的入口。

今天只打开：

- `README.md`
- `docs/学习文档/RAG-30天学习计划.md`
- `docs/学习文档/复盘文档/4月27日.md`

AI 逆向学习提问：

```text
请你基于当前仓库和 README，帮我重新接上 RAG 项目学习。
不要讲大理论。请只回答：
1. 这个项目解决什么问题
2. 用户最核心的两条路径是什么
3. 我今天重新开始时应该先看哪条路径
4. 给我 5 个检查问题，确认我真的接上了
```

必须输出到：`docs/学习文档/复盘文档/4月27日.md`

- 写 4 行重新开局日志。
- 写 1 句话：我今天从哪里重新接上。
- 写 3 个当前最不懂的问题。

---

### 4 月 28 日（周二）：QAController，找到问答入口

今日主题：用户点一次问答按钮，请求第一站到哪里。

今天只打开：

- `rag-admin/src/main/java/com/enterprise/rag/admin/controller/QAController.java`
- `rag-core/src/main/java/com/enterprise/rag/core/rag/model/QARequest.java`
- `rag-core/src/main/java/com/enterprise/rag/core/rag/service/RAGService.java`

AI 逆向学习提问：

```text
请基于 QAController 带我逆向学习问答入口。
不要泛讲 Spring MVC。请按顺序讲：
1. 这个 Controller 暴露了哪些问答接口
2. 同步问答和流式问答入口分别在哪里
3. QARequest 里有哪些关键字段
4. Controller 最后调用了哪个 Service
5. 给我 5 个检查题，等我回答后再纠错
```

必须输出到：`docs/学习文档/复盘文档/4月28日.md`

- 写出同步问答入口和流式问答入口。
- 写一条粗链路：`QAController -> RAGService -> ?`
- 写 1 句话面试表述：用户发起问答后，后端第一站是哪里。

---

### 4 月 29 日（周三）：RAGServiceImpl，串起问答主流程

今日主题：问答服务如何把“问题”变成“准备生成答案”。

今天只打开：

- `rag-core/src/main/java/com/enterprise/rag/core/rag/service/RAGServiceImpl.java`
- `rag-core/src/main/java/com/enterprise/rag/core/rag/query/QueryEngine.java`
- `rag-core/src/main/java/com/enterprise/rag/core/rag/generator/AnswerGenerator.java`

AI 逆向学习提问：

```text
请基于 RAGServiceImpl 带我逆向学习问答主流程。
请只围绕当前代码回答：
1. ask 方法的大步骤是什么
2. 参数校验、缓存、检索、生成分别在哪里发生
3. QueryEngine 和 AnswerGenerator 分别负责什么
4. 同步和流式流程有什么不同
5. 最后给我 5 个检查题
```

必须输出到：`docs/学习文档/复盘文档/4月29日.md`

- 写出 RAGServiceImpl 的 4 到 6 个主步骤。
- 写 2 个不懂点。
- 写 1 句话：RAGServiceImpl 在项目里的职责。

---

### 4 月 30 日（周四）：QueryEngineImpl，理解检索

今日主题：系统如何根据问题找到相关文档片段。

今天只打开：

- `rag-core/src/main/java/com/enterprise/rag/core/rag/query/QueryEngineImpl.java`
- `rag-core/src/main/java/com/enterprise/rag/core/embedding/EmbeddingService.java`
- `rag-core/src/main/java/com/enterprise/rag/core/vectorstore/VectorStore.java`

AI 逆向学习提问：

```text
请基于 QueryEngineImpl 带我逆向学习检索流程。
不要先讲 RAG 大理论。请按顺序讲：
1. 用户问题从哪里进入 QueryEngineImpl
2. embedding 在代码里如何被使用
3. vector store 如何参与检索
4. topK、score、rerank 在代码里分别体现在哪里
5. 给我 5 个检查题
```

必须输出到：`docs/学习文档/复盘文档/4月30日.md`

- 解释 4 个词：embedding、topK、rerank、relevance score。
- 写一条检索链路：`query -> embedding -> vector store -> results`
- 写 1 句面试表述：为什么提问前要先向量化。

---

## 第 2 段：假期保活（5 月 1 日 - 5 月 4 日）

### 5 月 1 日（周五）：只保活

今日主题：不追进度，只保持不断线。

今天只打开：

- `docs/学习文档/复盘文档/5月1日.md`
- `docs/学习文档/产出物/RAG术语表.md`

必须输出到：`docs/学习文档/复盘文档/5月1日.md`

- 写 2 个今天还能记得的词。
- 写 1 个最想问 AI 的问题。

---

### 5 月 2 日（周六）：只保活

今日主题：用 5 分钟维持连接。

今天只打开：

- `docs/学习文档/复盘文档/5月2日.md`

必须输出到：`docs/学习文档/复盘文档/5月2日.md`

- 写 1 句话：我现在记得的问答链路是怎样的。

---

### 5 月 3 日（周日）：只保活

今日主题：留一个问题给假期后的自己。

今天只打开：

- `docs/学习文档/复盘文档/5月3日.md`

必须输出到：`docs/学习文档/复盘文档/5月3日.md`

- 写 1 个你最想搞懂的问题。
- 写 1 个你现在最容易忘的概念。

---

### 5 月 4 日（周一）：复工预热

今日主题：不用学习新东西，只准备明天接上。

今天只打开：

- `docs/学习文档/复盘文档/5月4日.md`
- `docs/学习文档/复盘文档/4月30日.md`

必须输出到：`docs/学习文档/复盘文档/5月4日.md`

- 写一句：我明天从哪个类继续。
- 写 2 个明天要问 AI 的问题。

---

## 第 3 段：吃透问答生成链路（5 月 5 日 - 5 月 10 日）

### 5 月 5 日（周二）：PromptBuilder，理解上下文如何塞进提示词

今日主题：检索结果如何变成给大模型看的 prompt。

今天只打开：

- `rag-core/src/main/java/com/enterprise/rag/core/rag/prompt/PromptBuilder.java`
- `rag-core/src/main/java/com/enterprise/rag/core/rag/model/RetrievedContext.java`
- `rag-core/src/main/java/com/enterprise/rag/core/rag/model/QARequest.java`

AI 逆向学习提问：

```text
请基于 PromptBuilder 带我逆向学习。
请只围绕当前代码回答：
1. PromptBuilder 的输入是什么
2. 它如何组织检索到的上下文
3. token budget、去重、格式化分别在解决什么问题
4. 为什么不能把检索结果原样扔给模型
5. 给我 5 个检查题
```

必须输出到：`docs/学习文档/复盘文档/5月5日.md`

- 写 1 条链路：`RetrievedContext -> PromptBuilder -> prompt`
- 写 3 个概念解释：prompt、context、token budget。
- 写一句面试版表述：PromptBuilder 在项目里负责什么。

---

### 5 月 6 日（周三）：AnswerGeneratorImpl，理解模型调用

今日主题：大模型真正在哪里被调用。

今天只打开：

- `rag-core/src/main/java/com/enterprise/rag/core/rag/generator/AnswerGeneratorImpl.java`
- `rag-core/src/main/java/com/enterprise/rag/core/rag/generator/LLMProperties.java`
- `rag-core/src/main/java/com/enterprise/rag/core/rag/model/GeneratedAnswer.java`

AI 逆向学习提问：

```text
请基于 AnswerGeneratorImpl 带我逆向学习生成流程。
请按顺序讲：
1. prompt 如何进入 AnswerGeneratorImpl
2. 同步生成和流式生成分别怎么走
3. WebClient 或模型配置在哪里参与
4. citation 或来源信息如何进入最终回答
5. 给我 5 个检查题
```

必须输出到：`docs/学习文档/复盘文档/5月6日.md`

- 写出同步生成和流式生成的区别。
- 写 1 句话：大模型真正被调用的位置。
- 写 2 个仍然不懂的点。

---

### 5 月 7 日（周四）：RAG 核心概念，只围绕项目学

今日主题：把概念和代码连起来。

今天只打开：

- `docs/学习文档/复盘文档/4月30日.md`
- `docs/学习文档/复盘文档/5月5日.md`
- `docs/学习文档/复盘文档/5月6日.md`

AI 逆向学习提问：

```text
请只结合我这个 RAG 项目解释这些概念：
RAG、chunk、embedding、topK、rerank、citation、hallucination。
每个概念都要说明：
1. 在项目哪个环节出现
2. 它解决什么问题
3. 面试时一句话怎么讲
```

必须输出到：`docs/学习文档/复盘文档/5月7日.md`

- 补 5 个术语解释。
- 写 3 个面试问答卡。

---

### 5 月 8 日（周五）：问答链路图 v1

今日主题：把问答链路画出来。

今天只打开：

- `docs/学习文档/复盘文档/4月28日.md`
- `docs/学习文档/复盘文档/4月29日.md`
- `docs/学习文档/复盘文档/4月30日.md`
- `docs/学习文档/复盘文档/5月5日.md`
- `docs/学习文档/复盘文档/5月6日.md`

必须输出到：`docs/学习文档/复盘文档/5月8日.md`

- 写一版问答主链路图。
- 同步沉淀到：`docs/学习文档/产出物/RAG项目地图.md`
- 写 3 个链路里最虚的点。

---

### 5 月 9 日（周六）：第一次轻量模拟面

今日主题：用追问检查自己是不是真的能讲。

今天只打开：

- `docs/学习文档/产出物/RAG项目地图.md`
- `docs/学习文档/产出物/RAG术语表.md`

AI 逆向学习提问：

```text
你现在是 Java 后端实习面试官。
请基于我的 RAG 项目连续追问 8 个问题。
范围只包括：
1. 项目做什么
2. 问答链路怎么走
3. embedding / topK / rerank 是什么
4. PromptBuilder 和 AnswerGeneratorImpl 分别负责什么
我每回答一个，你再继续追问。回答空泛时请压问细节。
```

必须输出到：`docs/学习文档/复盘文档/5月9日.md`

- 记录 5 个被问到的问题。
- 写 3 个自己答得最虚的地方。

---

### 5 月 10 日（周日）：第一版 2 分钟项目介绍

今日主题：先有一版能讲的，不追求漂亮。

今天只打开：

- `docs/学习文档/产出物/RAG项目地图.md`
- `docs/学习文档/复盘文档/5月9日.md`
- `docs/学习文档/产出物/2分钟项目介绍.md`

必须输出到：`docs/学习文档/复盘文档/5月10日.md`

- 写 8 句话项目介绍。
- 同步沉淀到：`docs/学习文档/产出物/2分钟项目介绍.md`
- 写 1 个下周要补的最大短板。

---

## 第 4 段：吃透文档处理与索引链路（5 月 11 日 - 5 月 17 日）

### 5 月 11 日（周一）：DocumentIndexingServiceImpl，文档如何进入索引

今日主题：上传文档后，系统如何开始处理它。

今天只打开：

- `rag-admin/src/main/java/com/enterprise/rag/admin/kb/service/impl/DocumentIndexingServiceImpl.java`
- `rag-common/src/main/java/com/enterprise/rag/common/async/AsyncTaskManager.java`
- `rag-admin/src/main/java/com/enterprise/rag/admin/kb/entity/Document.java`

AI 逆向学习提问：

```text
请基于 DocumentIndexingServiceImpl 带我逆向学习文档索引流程。
请按顺序讲：
1. 文档记录什么时候创建
2. 异步任务什么时候提交
3. 解析、分块、向量化、入库分别在哪里发生
4. 文档状态如何变化
5. 给我 5 个检查题
```

必须输出到：`docs/学习文档/复盘文档/5月11日.md`

- 写一条文档索引主链路。
- 写 1 句话：为什么文档处理适合异步。
- 写 2 个不懂点。

---

### 5 月 12 日（周二）：DocumentProcessorImpl，解析、分块、去重

今日主题：一篇文档如何被拆成可检索的内容。

今天只打开：

- `rag-document/src/main/java/com/enterprise/rag/document/processor/DocumentProcessorImpl.java`
- `rag-document/src/main/java/com/enterprise/rag/document/parser/DocumentParserFactory.java`
- `rag-document/src/main/java/com/enterprise/rag/document/chunker/DocumentChunker.java`

AI 逆向学习提问：

```text
请基于 DocumentProcessorImpl 带我逆向学习。
请只围绕当前代码讲：
1. parser 负责什么
2. chunker 负责什么
3. content hash 在哪里出现，解决什么问题
4. 为什么要去重
5. 给我 5 个检查题
```

必须输出到：`docs/学习文档/复盘文档/5月12日.md`

- 解释 parser、chunker、content hash、dedup。
- 写一条链路：`file -> parser -> chunker -> chunks`
- 写 1 句面试表述：为什么不能直接把整篇文档喂给模型。

---

### 5 月 13 日（周三）：Chunk 专项

今日主题：分块大小和重叠为什么会影响效果。

今天只打开：

- `rag-document/src/main/java/com/enterprise/rag/document/chunker/DocumentChunker.java`
- `rag-document/src/main/java/com/enterprise/rag/document/chunker/ChunkConfig.java`
- `rag-document/src/main/java/com/enterprise/rag/document/chunker/DocumentChunk.java`

AI 逆向学习提问：

```text
请基于当前 chunker 相关代码解释 chunk 和 chunk overlap。
要求：
1. 不讲论文，只结合当前项目
2. 解释 chunk 太大和太小分别有什么问题
3. 解释 overlap 为什么有用
4. 给出面试时 3 句话版本
5. 给我 5 个检查题
```

必须输出到：`docs/学习文档/复盘文档/5月13日.md`

- 写 chunk / overlap 的类比解释。
- 写 3 个面试问答卡。

---

### 5 月 14 日（周四）：EmbeddingServiceImpl，向量化服务

今日主题：文本如何变成向量，provider 和 cache 是什么。

今天只打开：

- `rag-core/src/main/java/com/enterprise/rag/core/embedding/EmbeddingServiceImpl.java`
- `rag-core/src/main/java/com/enterprise/rag/core/embedding/EmbeddingProvider.java`
- `rag-core/src/main/java/com/enterprise/rag/core/embedding/config/EmbeddingProperties.java`

AI 逆向学习提问：

```text
请基于 EmbeddingServiceImpl 带我逆向学习。
请按顺序讲：
1. embedding provider 是什么
2. 项目为什么抽象出 provider
3. cache 在哪里参与
4. fallback 是什么意思
5. 给我 5 个检查题
```

必须输出到：`docs/学习文档/复盘文档/5月14日.md`

- 解释 provider、fallback、embedding cache。
- 写 1 句话：embedding 服务在问答和索引两条链路里分别做什么。

---

### 5 月 15 日（周五）：VectorStoreConfig 与向量库

今日主题：向量最终存在哪里，检索接口如何统一。

今天只打开：

- `rag-core/src/main/java/com/enterprise/rag/core/vectorstore/config/VectorStoreConfig.java`
- `rag-core/src/main/java/com/enterprise/rag/core/vectorstore/VectorStore.java`
- `rag-core/src/main/java/com/enterprise/rag/core/vectorstore/SearchOptions.java`
- `rag-core/src/main/java/com/enterprise/rag/core/vectorstore/SearchResult.java`

AI 逆向学习提问：

```text
请基于 VectorStoreConfig 和 VectorStore 接口带我逆向学习。
请讲清楚：
1. VectorStore 接口抽象了什么能力
2. SearchOptions 和 SearchResult 分别表示什么
3. 为什么项目要支持不同向量库实现
4. QueryEngineImpl 如何依赖这个接口
5. 给我 5 个检查题
```

必须输出到：`docs/学习文档/复盘文档/5月15日.md`

- 写向量库在项目里的职责。
- 写 2 个接口抽象的好处。
- 补 2 个术语：vector store、similarity search。

---

### 5 月 16 日（周六）：异步任务与状态

今日主题：为什么文档索引要用任务状态追踪。

今天只打开：

- `rag-common/src/main/java/com/enterprise/rag/common/async/AsyncTaskManager.java`
- `rag-common/src/main/java/com/enterprise/rag/common/async/TaskStatusService.java`
- `rag-admin/src/main/java/com/enterprise/rag/admin/controller/TaskController.java`

AI 逆向学习提问：

```text
请基于 async 相关代码和 TaskController 带我逆向学习。
请回答：
1. AsyncTaskManager 负责什么
2. TaskStatusService 负责什么
3. 前端为什么需要查询任务状态
4. 这个设计解决了文档上传里的什么体验问题
5. 给我 5 个检查题
```

必须输出到：`docs/学习文档/复盘文档/5月16日.md`

- 写异步任务状态流转。
- 写 1 句话：为什么不能让用户一直等上传接口返回。

---

### 5 月 17 日（周日）：文档处理链路图 v1

今日主题：把上传到索引这条链路画出来。

今天只打开：

- `docs/学习文档/复盘文档/5月11日.md`
- `docs/学习文档/复盘文档/5月12日.md`
- `docs/学习文档/复盘文档/5月14日.md`
- `docs/学习文档/复盘文档/5月15日.md`
- `docs/学习文档/产出物/RAG项目地图.md`

必须输出到：`docs/学习文档/复盘文档/5月17日.md`

- 写文档处理链路图。
- 同步补到：`docs/学习文档/产出物/RAG项目地图.md`
- 写当前最不懂的 5 个点。

---

## 第 5 段：后端基础、小改动、面试表达（5 月 18 日 - 5 月 22 日）

### 5 月 18 日（周一）：Spring 分层与项目表达

今日主题：Controller / Service / Config 为什么分层。

今天只打开：

- `rag-admin/src/main/java/com/enterprise/rag/admin/controller/QAController.java`
- `rag-core/src/main/java/com/enterprise/rag/core/rag/service/RAGServiceImpl.java`
- `rag-core/src/main/java/com/enterprise/rag/core/vectorstore/config/VectorStoreConfig.java`

AI 逆向学习提问：

```text
请结合当前仓库解释 Controller / Service / Config 分层。
不要泛讲 Spring。请回答：
1. QAController 为什么不直接写所有业务逻辑
2. RAGServiceImpl 为什么属于核心服务
3. Config 类解决什么问题
4. 面试时怎么讲这种分层
5. 给我 5 个检查题
```

必须输出到：`docs/学习文档/复盘文档/5月18日.md`

- 写 3 个分层解释。
- 写 1 句话：为什么不能把逻辑都堆在 Controller。

---

### 5 月 19 日（周二）：Redis、缓存、限流、幂等

今日主题：补项目里最容易被问的后端基础。

今天只打开：

- `rag-common/src/main/java/com/enterprise/rag/common/ratelimit/RateLimit.java`
- `rag-common/src/main/java/com/enterprise/rag/common/ratelimit/RateLimitInterceptor.java`
- `rag-common/src/main/java/com/enterprise/rag/common/idempotency/Idempotent.java`
- `rag-common/src/main/java/com/enterprise/rag/common/idempotency/IdempotencyAspect.java`
- `rag-common/src/main/java/com/enterprise/rag/common/util/RedisUtil.java`

AI 逆向学习提问：

```text
请结合当前仓库解释 Redis、限流、幂等和 AOP。
请回答：
1. 缓存命中和未命中是什么意思
2. 限流在项目里防什么
3. 幂等在项目里防什么
4. 为什么限流和幂等适合用注解或切面
5. 给我 5 个面试追问题
```

必须输出到：`docs/学习文档/复盘文档/5月19日.md`

- 写 5 个问答卡：缓存、Redis、限流、幂等、AOP。
- 同步到：`docs/学习文档/产出物/面试问答卡.md`

---

### 5 月 20 日（周三）：选择一个亲手小改动

今日主题：给项目表达增加一个真实抓手。

候选方向：

- 给问答链路补更清晰的日志。
- 给 PromptBuilder 或 QA 流程补一段必要注释。
- 给缓存命中 / 未命中增加更直观说明。
- 给某个核心流程补一个小测试。
- 做一个诊断记录模板。

今天只打开：

- `docs/学习文档/产出物/小改动记录.md`
- 候选改动涉及的 1 到 2 个代码文件

AI 逆向学习提问：

```text
请基于我当前 RAG 项目，帮我选择一个一两天能完成的小改动。
要求：
1. 范围小
2. 能提升我对项目的理解
3. 面试时能讲清楚
4. 不要大重构
请给我 3 个候选，并说明涉及文件、风险和验收方式。
```

必须输出到：`docs/学习文档/复盘文档/5月20日.md`

- 写选定的小改动。
- 写涉及文件。
- 写验收方式。
- 同步到：`docs/学习文档/产出物/小改动记录.md`

---

### 5 月 21 日（周四）：模拟项目面

今日主题：用面试追问找漏洞。

今天只打开：

- `docs/学习文档/产出物/RAG项目地图.md`
- `docs/学习文档/产出物/面试问答卡.md`
- `docs/学习文档/产出物/2分钟项目介绍.md`

AI 逆向学习提问：

```text
你现在是 Java 后端实习面试官。
请基于我的 RAG 项目连续追问 10 个问题。
重点关注：
1. 项目真实性
2. 问答链路
3. 文档处理链路
4. Redis / 限流 / 幂等 / 异步任务
5. 我亲手做过的小改动
我每答一个你再追问，回答空泛时请继续压问。
```

必须输出到：`docs/学习文档/复盘文档/5月21日.md`

- 记录 10 个问题中的至少 5 个。
- 写 3 个明显空泛的回答。
- 写 3 个需要回去看代码的问题。

---

### 5 月 22 日（周五）：阶段总复盘

今日主题：判断这轮学习带来了什么，下一轮补什么。

今天只打开：

- `docs/学习文档/复盘文档/`
- `docs/学习文档/产出物/RAG项目地图.md`
- `docs/学习文档/产出物/面试问答卡.md`
- `docs/学习文档/产出物/2分钟项目介绍.md`
- `docs/学习文档/产出物/小改动记录.md`

必须输出到：`docs/学习文档/复盘文档/5月22日.md`

- 写已经看过的模块。
- 写现在能讲什么。
- 写还不会什么。
- 写下一轮优先补什么。
- 写一句总判断：现在把项目写进简历，底气有几分，差在哪里。

---

## 7. 结束检查点

到 5 月 22 日，如果完成下面这些，就算这一轮成功：

- 至少有 12 天留下过有效输出。
- 问答主链路能讲出：`QAController -> RAGServiceImpl -> QueryEngineImpl -> PromptBuilder -> AnswerGeneratorImpl`
- 文档处理链路能讲出：`上传 -> 异步任务 -> 解析 -> 分块 -> 向量化 -> 入库`
- 至少写过 15 个术语或问答卡。
- 至少有一版 2 分钟项目介绍。
- 至少选定或完成一个小改动。

注意：成功标准不是 26 天全勤。成功标准是你重新把项目接上，并且开始能讲。
