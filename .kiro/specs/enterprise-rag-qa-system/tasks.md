# Implementation Plan: Enterprise RAG QA System

## Overview

本实现计划将企业内部 RAG 问答系统的设计转化为可执行的编码任务。采用增量开发方式，从基础设施开始，逐步构建核心功能模块。

## Tasks

- [x] 1. 项目初始化与基础设施搭建
  - [x] 1.1 创建 Spring Boot 3.x 项目结构
    - 使用 Maven 多模块结构
    - 配置 Java 17、Spring Boot 3.x 依赖
    - 创建 common、auth、document、rag、admin 模块
    - _Requirements: 全局_

  - [x] 1.2 配置数据库连接和 MyBatis Plus
    - 配置 MySQL/PostgreSQL 数据源
    - 配置 MyBatis Plus 代码生成器
    - 创建数据库迁移脚本（Flyway）
    - _Requirements: 全局_

  - [x] 1.3 配置 Redis 连接
    - 配置 Redis 连接池
    - 创建 RedisTemplate 配置类
    - 实现 Redis 工具类
    - _Requirements: 10.1, 10.2_

- [x] 2. 链路追踪与日志模块
  - [x] 2.1 实现 TraceId 生成器和过滤器
    - 创建 TraceIdGenerator 类
    - 实现 TraceFilter（OncePerRequestFilter）
    - 配置 MDC 日志上下文
    - _Requirements: 8.1, 8.2_

  - [x] 2.2 编写 TraceId 唯一性属性测试
    - **Property 18: TraceId 唯一性**
    - **Validates: Requirements 8.1**

- [x] 3. 认证授权模块
  - [x] 3.1 实现 JWT Token 提供者
    - 创建 JwtTokenProvider 类
    - 实现 Token 生成、解析、验证方法
    - 配置 Token 过期时间
    - _Requirements: 1.1, 1.2, 1.3_

  - [x] 3.2 实现 Token 黑名单机制
    - 使用 Redis SET 存储黑名单
    - 实现 Token 加入黑名单方法
    - 在验证时检查黑名单
    - _Requirements: 1.5_

  - [x] 3.3 实现认证服务
    - 创建 AuthService 接口和实现
    - 实现 login、logout、refreshToken 方法
    - 集成 Spring Security
    - _Requirements: 1.1, 1.4, 1.5_

  - [x] 3.4 配置 Spring Security
    - 配置 SecurityFilterChain
    - 实现 JwtAuthenticationFilter
    - 配置 CORS 和 CSRF
    - _Requirements: 1.2, 1.3_

  - [x] 3.5 编写 JWT Token 属性测试
    - **Property 1: JWT Token 往返一致性**
    - **Property 2: 无效 Token 拒绝**
    - **Property 3: Token 刷新有效性**
    - **Property 4: Token 黑名单有效性**
    - **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5**

- [x] 4. 限流模块
  - [x] 4.1 实现 Redis + Lua 限流器
    - 创建滑动窗口限流 Lua 脚本
    - 实现 RateLimiter 接口
    - 支持多维度限流（用户/IP/接口）
    - _Requirements: 6.1, 6.3_

  - [x] 4.2 实现限流拦截器
    - 创建 RateLimitInterceptor
    - 配置限流响应头
    - 处理 429 响应
    - _Requirements: 6.2, 6.4_

  - [x] 4.3 编写限流属性测试
    - **Property 15: 限流阈值正确性**
    - **Property 16: 限流响应头完整性**
    - **Validates: Requirements 6.2, 6.4**

- [x] 5. 幂等性模块
  - [x] 5.1 实现幂等性处理器
    - 创建 IdempotencyHandler 接口和实现
    - 使用 Redis 存储幂等性 Key
    - 配置 Key 过期时间
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [x] 5.2 实现幂等性注解和切面
    - 创建 @Idempotent 注解
    - 实现 IdempotencyAspect
    - 从请求头提取幂等性 Key
    - _Requirements: 7.2_

  - [x] 5.3 编写幂等性属性测试
    - **Property 17: 幂等性处理正确性**
    - **Validates: Requirements 7.2, 7.3**

- [x] 6. Checkpoint - 基础设施验证
  - 确保所有测试通过，如有问题请询问用户

- [x] 7. 向量嵌入模块
  - [x] 7.1 定义 EmbeddingProvider 接口
    - 创建 EmbeddingProvider 接口
    - 定义 getEmbedding、getDimension 方法
    - _Requirements: 3.1_

  - [x] 7.2 实现 OpenAI Embedding Provider
    - 集成 OpenAI API
    - 实现 text-embedding-ada-002 调用
    - 处理 API 错误和重试
    - _Requirements: 3.1, 3.2_

  - [x] 7.3 实现通义 Embedding Provider
    - 集成通义千问 API
    - 实现 text-embedding-v1 调用
    - _Requirements: 3.1, 3.2_

  - [x] 7.4 实现 BGE 本地模型 Provider
    - 集成本地 BGE 模型服务
    - 实现 HTTP 调用本地推理服务
    - _Requirements: 3.1, 3.2_

  - [x] 7.5 实现 EmbeddingService 门面
    - 创建 EmbeddingService 接口和实现
    - 实现 Provider 选择和降级逻辑
    - 集成 Redis 缓存
    - _Requirements: 3.2, 3.3, 3.4_

  - [x] 7.6 编写嵌入服务属性测试
    - **Property 9: 嵌入向量有效性**
    - **Property 10: 嵌入缓存一致性**
    - **Validates: Requirements 3.2, 3.4**

- [x] 8. 向量存储模块
  - [x] 8.1 定义 VectorStore 接口
    - 创建 VectorStore 接口
    - 定义 upsert、search、delete 方法
    - 定义 VectorDocument、SearchResult 记录类
    - _Requirements: 4.1_

  - [x] 8.2 实现 Milvus VectorStore
    - 集成 Milvus Java SDK
    - 实现集合创建、向量存储、搜索
    - 实现元数据过滤
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

  - [x] 8.3 实现 Qdrant VectorStore（可选）
    - 集成 Qdrant Java Client
    - 实现相同接口
    - _Requirements: 4.1_

  - [x] 8.4 实现 Elasticsearch VectorStore（可选）
    - 集成 Elasticsearch Java Client
    - 使用 dense_vector 字段类型
    - 实现 kNN 搜索
    - _Requirements: 4.1_

  - [x] 8.5 编写向量存储属性测试
    - **Property 7: 向量存储完整性**
    - **Property 11: 向量搜索排序正确性**
    - **Property 12: 向量搜索过滤正确性**
    - **Validates: Requirements 2.4, 4.2, 4.3, 4.4**

- [x] 9. 文档处理模块
  - [x] 9.1 实现文档解析器
    - 创建 DocumentParser 接口
    - 实现 PdfParser（Apache PDFBox）
    - 实现 MarkdownParser
    - 实现 WordParser（Apache POI）
    - 实现 CodeParser（按语言分块）
    - _Requirements: 2.1_

  - [x] 9.2 实现文档分块器
    - 创建 DocumentChunker 类
    - 实现语义分块策略
    - 支持配置块大小和重叠
    - _Requirements: 2.2_

  - [x] 9.3 实现文档处理器门面
    - 创建 DocumentProcessor 接口和实现
    - 协调解析、分块、嵌入流程
    - 实现幂等性检查（content hash）
    - _Requirements: 2.1, 2.2, 2.6_

  - [x] 9.4 编写文档处理属性测试
    - **Property 5: 文档解析完整性**
    - **Property 6: 文档分块覆盖性**
    - **Property 8: 文档上传幂等性**
    - **Validates: Requirements 2.1, 2.2, 2.6**

- [x] 10. Checkpoint - 核心组件验证
  - 确保所有测试通过，如有问题请询问用户

- [x] 11. 异步任务模块
  - [x] 11.1 实现异步任务管理器
    - 创建 AsyncTaskManager 接口和实现
    - 使用 CompletableFuture 实现异步执行
    - 实现任务状态持久化
    - _Requirements: 9.1, 9.2_

  - [x] 11.2 实现任务状态查询
    - 创建 TaskStatus 记录类
    - 实现任务进度更新
    - 实现任务结果查询
    - _Requirements: 9.3_

  - [x] 11.3 实现消息队列集成（可选）
    - 集成 RabbitMQ 或 Kafka
    - 实现文档索引消息生产者
    - 实现文档索引消息消费者
    - _Requirements: 9.1_

  - [x] 11.4 编写异步任务属性测试
    - **Property 19: 异步任务提交即时性**
    - **Property 20: 任务状态查询完整性**
    - **Validates: Requirements 9.2, 9.3**

- [x] 12. RAG 核心模块
  - [x] 12.1 实现查询引擎
    - 创建 QueryEngine 接口和实现
    - 实现问题向量化
    - 实现相似度检索
    - 实现结果重排序
    - _Requirements: 5.1_

  - [x] 12.2 实现 Prompt 构建器
    - 创建 PromptBuilder 类
    - 实现上下文注入模板
    - 支持多种 Prompt 策略
    - _Requirements: 5.2_

  - [x] 12.3 实现答案生成器
    - 创建 AnswerGenerator 接口和实现
    - 集成 LLM API（OpenAI/通义）
    - 实现流式响应支持
    - 提取引用来源
    - _Requirements: 5.3, 5.4_

  - [x] 12.4 实现 RAG 服务门面
    - 创建 RAGService 接口和实现
    - 协调检索和生成流程
    - 集成缓存
    - 处理无结果情况
    - _Requirements: 5.1, 5.4, 5.5, 5.6_

  - [x] 12.5 编写 RAG 核心属性测试
    - **Property 13: Prompt 上下文包含性**
    - **Property 14: 问答响应完整性**
    - **Property 21: 查询缓存有效性**
    - **Validates: Requirements 5.2, 5.4, 10.1**

- [x] 13. 知识库管理模块
  - [x] 13.1 实现知识库 CRUD
    - 创建 KnowledgeBase 实体和 Repository
    - 实现 KnowledgeBaseService
    - 实现创建、查询、更新、删除
    - _Requirements: 11.1_

  - [x] 13.2 实现文档管理
    - 创建 Document 实体和 Repository
    - 实现文档上传、删除
    - 实现级联删除向量数据
    - _Requirements: 11.2_

  - [x] 13.3 实现知识库权限控制
    - 创建 KBPermission 实体
    - 实现权限检查服务
    - 集成 Spring Security 方法级安全
    - _Requirements: 11.3_

  - [x] 13.4 实现知识库统计
    - 实现文档数、向量数统计
    - 实现查询次数统计
    - _Requirements: 11.4_

  - [x] 13.5 编写知识库管理属性测试
    - **Property 22: 知识库 CRUD 一致性**
    - **Property 23: 文档删除级联性**
    - **Property 24: 知识库权限隔离性**
    - **Property 25: 统计信息一致性**
    - **Validates: Requirements 11.1, 11.2, 11.3, 11.4**

- [x] 14. Checkpoint - 业务功能验证
  - 确保所有测试通过，如有问题请询问用户

- [x] 15. 问答历史与反馈模块
  - [x] 15.1 实现问答历史记录
    - 创建 QAHistory 实体和 Repository
    - 实现历史保存服务
    - 集成到 RAG 服务
    - _Requirements: 12.1_

  - [x] 15.2 实现历史查询
    - 实现分页查询
    - 实现按时间倒序排列
    - _Requirements: 12.2_

  - [x] 15.3 实现反馈功能
    - 创建 QAFeedback 实体和 Repository
    - 实现反馈提交服务
    - _Requirements: 12.3_

  - [x] 15.4 编写问答历史属性测试
    - **Property 26: 问答历史记录完整性**
    - **Property 27: 分页查询正确性**
    - **Property 28: 反馈保存正确性**
    - **Validates: Requirements 12.1, 12.2, 12.3**

- [x] 16. REST API 层
  - [x] 16.1 实现认证 API
    - 创建 AuthController
    - 实现 /auth/login、/auth/logout、/auth/refresh 端点
    - 添加 Swagger 文档
    - _Requirements: 1.1, 1.4, 1.5_

  - [x] 16.2 实现知识库 API
    - 创建 KnowledgeBaseController
    - 实现 CRUD 端点
    - 实现文档上传端点
    - _Requirements: 11.1, 2.1_

  - [x] 16.3 实现问答 API
    - 创建 QAController
    - 实现 /qa/ask 端点
    - 实现流式响应端点
    - _Requirements: 5.1, 5.4_

  - [x] 16.4 实现历史与反馈 API
    - 创建 HistoryController
    - 实现历史查询端点
    - 实现反馈提交端点
    - _Requirements: 12.1, 12.2, 12.3_

  - [x] 16.5 实现任务状态 API
    - 创建 TaskController
    - 实现任务状态查询端点
    - _Requirements: 9.3_

- [x] 17. 全局异常处理与响应封装
  - [x] 17.1 实现全局异常处理器
    - 创建 GlobalExceptionHandler
    - 处理各类业务异常
    - 统一错误响应格式
    - _Requirements: 全局_

  - [x] 17.2 实现统一响应封装
    - 创建 ApiResponse 类
    - 实现 ResponseBodyAdvice
    - _Requirements: 全局_

- [x] 18. Final Checkpoint - 完整系统验证
  - 确保所有测试通过
  - 验证 API 文档完整性
  - 如有问题请询问用户

## Notes

- 所有任务都是必须完成的，包括属性测试
- 每个任务都引用了具体的需求条款以保证可追溯性
- Checkpoint 任务用于阶段性验证
- 属性测试验证核心逻辑的通用正确性
- 单元测试验证具体示例和边界情况
