# RAG 项目学习路线图（项目实战版，8 周）

**更新日期**: 2026 年 3 月 22 日  
**当前基础**: 大二，Java / JavaWeb 基础一般，CRUD 经验有限  
**项目特点**: 多模块 Spring Boot RAG 项目，包含认证、文档处理、RAG 核心、通用中间层、前端联调  
**这版路线图的目标**: 不是“把所有源码都啃完”，而是 8 周内做到 `能跑通`、`能讲清`、`能定位`、`能小改`、`能投递实习`

---

## 🎯 先说结论：这 8 周最该追求什么

对你现在的阶段来说，最合理的目标不是“完全反向工程理解整个项目”，而是下面 5 件事：

1. 能在 Ubuntu 上独立把项目跑起来，知道依赖服务、后端、前端分别怎么启动
2. 能说清楚项目的两条核心链路
   - 文档上传链路：上传 -> 异步任务 -> 解析 -> 分块 -> 向量化 -> 入库
   - 问答链路：提问 -> 检索 -> 重排 -> Prompt 构建 -> LLM 生成 -> 返回答案
3. 能在 IDE 里快速找到一个功能对应的 Controller、Service、核心实现类
4. 能独立完成 1 个小改动，并讲清楚“为什么这样改、改了什么、怎么验证” 
5. 能把这个项目讲成一个适合暑期实习面试的项目，而不是“AI 生成代码我大概看过”

---

## 🧭 基于真实项目代码的学习顺序

这次路线图不是按抽象概念排的，而是按项目真实调用链排的。

### 项目实际模块分工

| 模块 | 作用 | 学习优先级 |
|------|------|-----------|
| `rag-admin` | 主应用入口，Controller、业务编排、知识库/问答/任务接口 | 最高 |
| `rag-document` | 文档解析、分块 | 最高 |
| `rag-core` | Embedding、向量检索、答案生成 | 最高 |
| `rag-common` | Redis、限流、幂等、异步任务、Trace、统一异常 | 高 |
| `rag-auth` | JWT 登录认证、刷新、黑名单 | 中高 |
| `rag-frontend` | 前端页面、联调、SSE 展示 | 中 |

### 正确学习主轴

推荐顺序：

`先跑通系统 -> 再看 admin 入口 -> 再看 document/core 主链 -> 再看 common 横切能力 -> 最后补 auth 和前端联调`

这比“先学 JWT，再学文档，再学 RAG”更适合你现在的目标。

---

## ⚠️ 项目阅读时要先记住的 5 个事实

这些都是我结合仓库代码整理出来的，提前知道会少走很多弯路：

1. 文档上传不是同步处理，而是通过 `DocumentIndexingServiceImpl` 提交异步任务，返回 `taskId`，再轮询任务状态。
2. 文档去重不是只靠 `DocumentProcessorImpl` 里的内存哈希，真正更关键的去重逻辑在 `DocumentIndexingServiceImpl` 里，会按 `kbId + contentHash` 查数据库。
3. 问答主链核心不是 Controller，而是 `QAController -> RAGServiceImpl -> QueryEngineImpl -> AnswerGeneratorImpl`。
4. 项目支持多种向量库和多种 Embedding Provider，但当前默认配置是 `Milvus + OpenAI 兼容 Embedding`。
5. 限流、幂等、Trace 这些能力并不在业务代码内部硬编码，而是放在 `rag-common` 里做横切处理，这正是值得你学习的“企业化写法”。

---

## ⏱️ 总体投入建议

如果你真的想在 8 周后拿这个项目去投暑期实习，建议按下面的学习强度执行：

- 每周 `12 ~ 15 小时`
- 每天学习 1.5 ~ 2.5 小时，尽量保证连续性
- 每周至少做 1 次“从运行到讲解”的复盘

低于这个投入，也能看懂一部分，但很难达到“面试能讲清”的程度。

---

## 📅 8 周学习计划（项目实战版）

### 第 1 周：先把系统跑起来，建立全局地图

**本周目标**: 先别急着啃细节，先把项目当成一个完整产品跑起来。

**先看这些文件**:
- `README.md`
- `DOCKER_QUICKSTART.md`
- `docker-compose.yml`
- `rag-admin/src/main/resources/application.yml`
- `rag-admin/src/main/java/com/enterprise/rag/RagQaApplication.java`

**本周必须理解**:
- 项目有哪些模块，各模块分别做什么
- MySQL、Redis、Milvus、MinIO、etcd 为什么都要启动
- 后端配置主要在哪里看
- 项目最重要的业务流程是什么

**动手任务**:
- 在 Ubuntu 上执行 `docker compose up -d`
- 启动后端 `rag-admin`
- 能打开 Swagger 页面
- 能调用登录接口
- 如果时间够，再启动 `rag-frontend`

**本周产出**:
- 一张你自己画的“模块图”
- 一份“本地启动命令清单”
- 一份“我现在还看不懂的 10 个名词”列表

---

### 第 2 周：补 Spring Boot 主链基础，看懂 API 是怎么落到业务层的

**本周目标**: 建立最基础的后端阅读能力，别再把项目当成一堆散乱的类。

**重点看这些文件**:
- `rag-admin/src/main/java/com/enterprise/rag/admin/controller/KnowledgeBaseController.java`
- `rag-admin/src/main/java/com/enterprise/rag/admin/controller/QAController.java`
- `rag-admin/src/main/java/com/enterprise/rag/admin/kb/service/impl/KnowledgeBaseServiceImpl.java`
- `rag-admin/src/main/java/com/enterprise/rag/admin/kb/service/impl/DocumentServiceImpl.java`
- `rag-common/src/main/java/com/enterprise/rag/common/model/ApiResponse.java`
- `rag-common/src/main/java/com/enterprise/rag/common/exception/GlobalExceptionHandler.java`

**本周必须理解**:
- Controller / Service / Mapper / Entity / DTO 分别是什么角色
- 一个 HTTP 请求进入 Spring Boot 后大概怎么流转
- 为什么这里用了事务、统一返回体、统一异常处理
- `rag-admin` 为什么适合作为项目入口模块

**动手任务**:
- 按接口顺序手动调用：登录 -> 创建知识库 -> 查询知识库列表
- 在 IDE 里用断点跟一遍 Controller 到 Service 的调用
- 记录 1 个接口的入参、返回值、数据库落点

**本周产出**:
- 一张“请求流转图”：`Controller -> Service -> Mapper -> DB`
- 一页“Spring Boot 主链笔记”

---

### 第 3 周：吃透文档上传和异步索引链路

**本周目标**: 这周是整个项目最重要的一周之一，因为它决定你是否真的理解 RAG 的“索引阶段”。

**重点看这些文件**:
- `rag-admin/src/main/java/com/enterprise/rag/admin/kb/service/impl/DocumentIndexingServiceImpl.java`
- `rag-admin/src/main/java/com/enterprise/rag/admin/controller/TaskController.java`
- `rag-document/src/main/java/com/enterprise/rag/document/processor/DocumentProcessorImpl.java`
- `rag-document/src/main/java/com/enterprise/rag/document/parser/DocumentParserFactory.java`
- `rag-document/src/main/java/com/enterprise/rag/document/chunker/DocumentChunker.java`
- `rag-common/src/main/java/com/enterprise/rag/common/async/AsyncTaskManager.java`

**本周必须理解**:
- 上传文档后为什么不直接同步处理
- `taskId` 是怎么来的，为什么要轮询任务状态
- 文档是如何从“文件”变成“纯文本 + 分块”的
- 为什么要做分块，块太大或太小会有什么问题
- 为什么这里会同时出现“幂等”和“去重”两个概念，它们不是一回事

**动手任务**:
- 上传 `test-small.txt` 和 `test-md.md`
- 看任务状态变化
- 跟踪一次 `submitIndexing -> doIndex -> process -> embedBatch -> upsert`
- 自己写出“上传一份文档后系统实际做了哪些步骤”

**本周产出**:
- 一张“文档上传时序图”
- 一页“分块策略与去重逻辑说明”

---

### 第 4 周：吃透 RAG 主链的检索阶段

**本周目标**: 从“索引阶段”切到“问答阶段”，重点理解向量化、检索、重排。

**重点看这些文件**:
- `rag-core/src/main/java/com/enterprise/rag/core/embedding/EmbeddingServiceImpl.java`
- `rag-core/src/main/java/com/enterprise/rag/core/rag/query/QueryEngineImpl.java`
- `rag-core/src/main/java/com/enterprise/rag/core/vectorstore/milvus/MilvusVectorStore.java`
- `rag-core/src/main/java/com/enterprise/rag/core/vectorstore/SearchOptions.java`
- `rag-core/src/main/java/com/enterprise/rag/core/rag/model/RetrieveOptions.java`

**本周必须理解**:
- 用户问题是怎么被转成向量的
- 为什么向量库可以做相似度检索
- `topK`、`minScore`、`filter` 分别在控制什么
- 当前项目里“重排”并不是大模型 reranker，而是一个简单关键词加权重排序逻辑
- 为什么项目支持 Milvus / Qdrant / Elasticsearch，但你现在只需要先把 Milvus 吃透

**动手任务**:
- 调问答接口，多试几个问题
- 观察不同问题下检索结果数量和答案效果
- 把 `QueryEngineImpl` 的检索流程逐步写成自然语言

**本周产出**:
- 一页“向量检索原理 + 本项目实现”笔记
- 一张“问题进入检索层后的调用链图”

---

### 第 5 周：吃透生成阶段、缓存和同步/流式问答

**本周目标**: 把完整问答链路真正串起来。

**重点看这些文件**:
- `rag-core/src/main/java/com/enterprise/rag/core/rag/service/RAGServiceImpl.java`
- `rag-core/src/main/java/com/enterprise/rag/core/rag/generator/AnswerGeneratorImpl.java`
- `rag-core/src/main/java/com/enterprise/rag/core/rag/prompt/PromptBuilder.java`
- `rag-admin/src/main/java/com/enterprise/rag/admin/controller/QAController.java`
- `rag-admin/src/main/java/com/enterprise/rag/admin/qa/service/impl/QAHistoryServiceImpl.java`

**本周必须理解**:
- 为什么问答要分成“检索”和“生成”两个步骤
- Prompt 是怎么拼出来的
- 为什么需要 citation / contextCount / cached 这些元数据
- 同步问答和流式问答的差异是什么
- 为什么 `QAController` 里的流式接口选择 `SseEmitter`，而不是直接返回 `Flux<String>`

**动手任务**:
- 调用 `/api/qa/ask`
- 调用 `/api/qa/ask/stream`
- 对比同步和流式接口的行为差异
- 自己写一段 3 分钟讲解词，讲清楚“用户提问后系统经历了什么”

**本周产出**:
- 一张“完整问答链路图”
- 一页“缓存、检索、生成三者关系总结”

---

### 第 6 周：学习横切能力，不再只盯着业务代码

**本周目标**: 开始理解这个项目为什么比“普通课程作业”更像工程项目。

**重点看这些文件**:
- `rag-auth/src/main/java/com/enterprise/rag/auth/config/SecurityConfig.java`
- `rag-auth/src/main/java/com/enterprise/rag/auth/filter/JwtAuthenticationFilter.java`
- `rag-auth/src/main/java/com/enterprise/rag/auth/service/impl/AuthServiceImpl.java`
- `rag-auth/src/main/java/com/enterprise/rag/auth/provider/JwtTokenProvider.java`
- `rag-auth/src/main/java/com/enterprise/rag/auth/service/TokenBlacklistService.java`
- `rag-common/src/main/java/com/enterprise/rag/common/ratelimit/RateLimitInterceptor.java`
- `rag-common/src/main/java/com/enterprise/rag/common/idempotency/IdempotencyAspect.java`
- `rag-common/src/main/java/com/enterprise/rag/common/trace/TraceFilter.java`

**本周必须理解**:
- JWT 登录、认证、刷新、登出的主流程
- 为什么登出后要做黑名单，而不是只在前端删除 Token
- 限流是在什么层做的，靠什么注解触发
- 幂等性是在什么层做的，为什么用 AOP
- TraceId 的价值是什么，为什么日志里要有链路信息

**动手任务**:
- 手动测试登录、刷新、登出
- 观察限流相关响应头
- 观察幂等 key 的作用
- 记录“这 3 个横切能力如何保护业务主链”

**本周产出**:
- 一页“认证 + 限流 + 幂等 + Trace 总结”
- 5 个适合面试回答的工程化问题答案

---

### 第 7 周：做一个真实的小改动，把“会看代码”升级成“会动代码”

**本周目标**: 从阅读者变成真正的项目参与者。

**这周只选 1 个改动，不要贪多**。

**推荐改动方向**:
- 方案 A: 调整文档分块参数，比较对检索结果和回答质量的影响
- 方案 B: 增强文档上传的校验或错误提示
- 方案 C: 为问答接口增加一个更明确的可调参数，并打通前后端
- 方案 D: 增强检索结果元数据展示，方便排查召回效果

**本周必须做到**:
- 说清楚你改的目标是什么
- 找到需要改的入口文件和依赖类
- 说明你为什么这样改
- 给出验证方式，而不是“我感觉能用”

**本周产出**:
- 一份你自己的“变更说明”
- 一份“测试/验证记录”
- 一段 2 分钟口头说明：你解决了什么问题

---

### 第 8 周：面试整理与投递准备

**本周目标**: 把前 7 周的学习真正转化成可以投递的能力表达。

**本周重点任务**:
- 写出项目 1 分钟版介绍
- 写出项目 3 分钟版介绍
- 准备 10 个高频面试问题答案
- 整理 3 个你做过的观察或改动点
- 在简历里把这个项目写成“我理解并实践过”，而不是“我运行过”

**建议你最终准备好这 4 份材料**:
- 一张项目总架构图
- 一张文档上传链路图
- 一张问答链路图
- 一份项目面试问答清单

**本周产出**:
- 最终版项目讲稿
- 最终版简历项目描述
- 最终版 Q&A 清单

---

## 📚 每周固定学习方法

每周都按这个顺序来，不容易迷路：

1. 先跑一遍功能，确认这个功能在系统里真实存在
2. 再从入口类开始看，不要一上来就钻底层工具类
3. 每看一个类，先回答这 3 个问题
   - 这个类解决什么问题？
   - 它依赖谁？被谁调用？
   - 如果没有它，系统会在哪一步出问题？
4. 每周至少打 2 次断点，跟踪真实数据流
5. 每周至少写 1 页自己的总结，禁止只收藏不输出

---

## 🧩 项目里哪些内容优先学，哪些先后置

### 必须优先学会的
- `rag-admin` 里的主要 Controller 和 Service
- 文档上传与异步索引链路
- RAG 问答主链路
- Redis 在缓存、限流、黑名单、任务状态里的用途
- Milvus 在向量检索里的角色
- 基础的 JWT 登录认证流程

### 可以后置的
- `Qdrant` 和 `Elasticsearch` 的适配实现
- 所有 Parser 的细节实现
- Property-Based Testing 的测试设计
- 过深的 AOP 底层原理
- 前端组件的细节样式实现
- 所有配置项的每个参数含义

---

## 🔥 这 10 个问题，你到第 8 周必须能答出来

1. 这个项目为什么拆成 `admin / auth / common / document / core` 五个后端模块？
2. 用户上传一份文档后，系统内部会经历哪些步骤？
3. 为什么文档处理要做异步化？
4. 文档为什么要分块？分块策略会影响什么？
5. 向量检索为什么能找到“语义相关”的内容？
6. 问答接口为什么不是直接把问题发给大模型？
7. Redis 在这个项目里承担了哪些职责？
8. 用户登出后，旧 Token 为什么还能被立即失效？
9. 限流和幂等分别解决什么问题？
10. 如果让你继续优化这个项目，你最想先改哪里？为什么？

---

## 🛠️ 建议你重点使用的命令

```bash
# 启动依赖
docker compose up -d

# 查看依赖状态
docker compose ps

# 查看后端日志
docker compose logs -f

# 启动后端（在项目根目录）
mvn -pl rag-admin -am spring-boot:run

# 启动前端
cd rag-frontend
npm install
npm run dev
```

如果你后面主要学后端，前端可以先只做到“能登录、能上传、能问答”，不用一开始就深挖 Vue 细节。

---

## ✅ 8 周结束时的达标标准

到第 8 周结束，你至少应该达到下面这些状态：

- [ ] 能在自己的 Ubuntu 环境中独立跑起整个项目
- [ ] 能无稿讲清文档上传链路和问答链路
- [ ] 能在 IDE 中快速定位上传、问答、认证的关键实现类
- [ ] 能解释 Redis、Milvus、JWT、异步任务各自在项目里的作用
- [ ] 能指出这个项目里 3 个“工程化设计”点
- [ ] 能完成并解释 1 个真实改动
- [ ] 能把这个项目写进简历并支撑 10 分钟面试追问

---

## 📝 最后给你的执行建议

这 8 周里，最容易犯的错误有两个：

1. 过早沉迷底层细节，结果主链没有真正跑通
2. 看了很多代码，但没有形成自己的图、笔记、讲稿和改动

所以这次请你牢牢记住一句话：

**你的目标不是“把代码都看过”，而是“把系统讲清楚，并且能改一点”。**

如果你愿意，下一步可以继续让我做两件事：
- 按这份路线图，继续给你拆出 `第 1 周详细任务清单`
- 直接基于项目代码，给你整理一份 `核心类阅读顺序表`
