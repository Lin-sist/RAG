# Enterprise RAG QA System 项目文档

## 📋 目录

1. [项目概述](#项目概述)
2. [技术架构](#技术架构)
3. [模块说明](#模块说明)
4. [环境准备](#环境准备)
5. [快速开始](#快速开始)
6. [API 接口文档](#api-接口文档)
7. [核心功能详解](#核心功能详解)
8. [配置说明](#配置说明)
9. [开发指南](#开发指南)
10. [常见问题](#常见问题)

---

## 📖 项目概述

**Enterprise RAG QA System** 是一个企业内部 AI 驱动的技术文档与代码知识库问答系统。该系统为企业内部技术文档、代码仓库提供 AI 搜索与问答能力，通过 RAG（Retrieval-Augmented Generation，检索增强生成）技术实现精准的知识检索与智能问答。

### 核心特性

- 🔐 **安全认证**：基于 Spring Security + JWT 的用户认证授权
- 📄 **文档处理**：支持 PDF、Markdown、Word、代码文件的解析和分块
- 🔢 **向量嵌入**：支持 OpenAI、通义千问、BGE 本地模型等多种嵌入服务
- 🗄️ **向量存储**：支持 Milvus、Qdrant、Elasticsearch 作为向量数据库
- 💬 **智能问答**：完全自主实现的 RAG 流程，支持同步和流式响应
- ⚡ **高性能**：Redis 缓存、接口限流、异步任务处理
- 🔍 **可追踪**：完整的链路日志追踪（TraceId）

### 技术栈

| 类别 | 技术 |
|------|------|
| 框架 | Spring Boot 3.2.1, Spring Security |
| 语言 | Java 17 |
| 数据库 | MySQL 8.x, Redis |
| 向量数据库 | Milvus / Qdrant / Elasticsearch |
| ORM | MyBatis-Plus 3.5.5 |
| 数据库迁移 | Flyway 9.22.3 |
| 认证 | JWT (jjwt 0.12.3) |
| 文档解析 | Apache PDFBox 3.0.1, Apache POI 5.2.5, Flexmark |
| 测试 | JUnit 5, jqwik, Testcontainers |

---

## 🏗️ 技术架构

### 系统架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Client (Web/App)                               │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           API Gateway (rag-admin)                        │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │AuthController│  │QAController │  │KnowledgeBase │  │TaskController │  │
│  └─────────────┘  └─────────────┘  │  Controller  │  └───────────────┘  │
│                                    └──────────────┘                      │
└─────────────────────────────────────────────────────────────────────────┘
        │                    │                    │                    │
        ▼                    ▼                    ▼                    ▼
┌───────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ rag-auth  │  │    rag-core     │  │  rag-document   │  │   rag-common    │
│           │  │                 │  │                 │  │                 │
│ • JWT     │  │ • RAGService    │  │ • DocProcessor  │  │ • RateLimiter   │
│ • Security│  │ • Embedding     │  │ • Chunker       │  │ • Trace         │
│ • Token   │  │ • VectorStore   │  │ • Parser        │  │ • Idempotency   │
│ • Session │  │ • QueryEngine   │  │ • PDF/MD/Word   │  │ • AsyncTask     │
└───────────┘  └─────────────────┘  └─────────────────┘  └─────────────────┘
        │                    │                    │                    │
        └────────────────────┴────────────────────┴────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        ▼                           ▼                           ▼
┌─────────────┐            ┌─────────────┐            ┌─────────────┐
│   MySQL     │            │    Redis    │            │ VectorDB    │
│             │            │             │            │ (Milvus/    │
│ • 用户      │            │ • 缓存      │            │  Qdrant/    │
│ • 知识库    │            │ • 会话      │            │  ES)        │
│ • 文档      │            │ • 限流      │            │             │
│ • 问答历史  │            │ • 幂等性    │            │ • 向量存储  │
└─────────────┘            └─────────────┘            └─────────────┘
```

### 分层架构

```
┌────────────────────────────────────────────┐
│           Controller Layer                  │  API 接口层
├────────────────────────────────────────────┤
│             Service Layer                   │  业务逻辑层
├────────────────────────────────────────────┤
│          Repository/Mapper Layer            │  数据访问层
├────────────────────────────────────────────┤
│              Model Layer                    │  实体/DTO层
├────────────────────────────────────────────┤
│           Infrastructure Layer              │  基础设施层
│  (Redis, Vector Store, LLM, Embedding)      │
└────────────────────────────────────────────┘
```

---

## 📦 模块说明

项目采用 Maven 多模块结构，包含以下 5 个子模块：

### 1. rag-common（公共模块）

**职责**：提供通用工具类、配置和基础能力

**主要组件**：
- `trace/` - 链路追踪（TraceId 生成与传递）
- `ratelimit/` - 接口限流（基于 Redis + Lua 的滑动窗口限流）
- `idempotency/` - 幂等性控制（防止重复提交）
- `async/` - 异步任务管理（CompletableFuture 和消息队列）
- `exception/` - 全局异常处理
- `model/` - 通用响应模型
- `util/` - 工具类（RedisUtil 等）

**依赖关系**：被所有其他模块依赖

### 2. rag-auth（认证授权模块）

**职责**：处理用户认证和授权

**主要组件**：
- `config/` - Spring Security 配置、JWT 属性配置
- `provider/JwtTokenProvider.java` - JWT Token 生成、解析、验证
- `service/AuthService.java` - 登录、登出逻辑
- `service/TokenBlacklistService.java` - Token 黑名单管理
- `filter/` - JWT 认证过滤器
- `handler/` - 认证异常处理器

**依赖关系**：依赖 `rag-common`

### 3. rag-document（文档处理模块）

**职责**：文档解析、分块和预处理

**主要组件**：
- `parser/` - 文档解析器（PDF、Markdown、Word、代码文件）
- `chunker/` - 文档分块器（语义分块策略）
- `processor/DocumentProcessor.java` - 文档处理协调器

**支持的文档格式**：
- PDF（使用 Apache PDFBox）
- Word (.docx)（使用 Apache POI）
- Markdown（使用 Flexmark）
- 代码文件（Java、Python、JavaScript 等）

**依赖关系**：依赖 `rag-common`

### 4. rag-core（RAG 核心模块）

**职责**：RAG 核心功能实现

**主要组件**：

**embedding/**
- `EmbeddingService.java` - 嵌入服务接口
- `EmbeddingServiceImpl.java` - 嵌入服务实现（支持缓存和降级）
- `OpenAIEmbeddingProvider.java` - OpenAI 嵌入提供者
- `QwenEmbeddingProvider.java` - 通义千问嵌入提供者
- `BGEEmbeddingProvider.java` - BGE 本地模型嵌入提供者

**vectorstore/**
- `VectorStore.java` - 向量存储接口
- `milvus/` - Milvus 向量存储实现
- `qdrant/` - Qdrant 向量存储实现
- `elasticsearch/` - Elasticsearch 向量存储实现

**rag/**
- `query/QueryEngine.java` - 查询引擎（问题向量化 + 相似度检索）
- `generator/AnswerGenerator.java` - 答案生成器（LLM 调用）
- `prompt/` - Prompt 模板管理
- `service/RAGService.java` - RAG 服务（协调检索和生成）

**依赖关系**：依赖 `rag-common`, `rag-document`

### 5. rag-admin（管理模块）

**职责**：应用入口、API 控制器

**主要组件**：
- `RagQaApplication.java` - Spring Boot 启动类
- `controller/` - REST API 控制器
  - `AuthController.java` - 认证相关接口
  - `QAController.java` - 问答接口
  - `KnowledgeBaseController.java` - 知识库管理接口
  - `HistoryController.java` - 问答历史接口
  - `TaskController.java` - 异步任务状态查询
- `kb/` - 知识库相关实体、DTO、Service、Mapper
- `qa/` - 问答历史相关实体、DTO、Service、Mapper

**依赖关系**：依赖所有其他模块

---

## 🛠️ 环境准备

### 前置条件

1. **JDK 17+**
   ```bash
   java -version
   # 输出应包含 version "17" 或更高版本
   ```

2. **Maven 3.6+**
   ```bash
   mvn -version
   ```

3. **MySQL 8.x**
   ```bash
   mysql --version
   ```

4. **Redis 6.x+**
   ```bash
   redis-cli --version
   ```

5. **向量数据库**（任选其一）
   - Milvus 2.x
   - Qdrant 1.x
   - Elasticsearch 8.x

### 数据库初始化

1. 创建 MySQL 数据库：
   ```sql
   CREATE DATABASE rag_qa CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. 数据库表会通过 Flyway 自动迁移创建，无需手动执行 SQL

---

## 🚀 快速开始

### 1. 克隆项目

```bash
git clone <repository-url>
cd RAG
```

### 2. 配置文件

修改 `rag-admin/src/main/resources/application.yml`：

```yaml
# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rag_qa?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: your_username
    password: your_password

  # Redis 配置
  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password  # 如有密码

# JWT 配置（生产环境请修改密钥）
jwt:
  secret: your-256-bit-secret-key-for-jwt-token-generation-change-in-production
  access-token-expiration: 3600000   # 1 小时
  refresh-token-expiration: 604800000  # 7 天
```

### 3. 编译项目

```bash
mvn clean install -DskipTests
```

### 4. 运行应用

```bash
cd rag-admin
mvn spring-boot:run
```

或者直接运行 JAR：

```bash
java -jar rag-admin/target/rag-admin-1.0.0-SNAPSHOT.jar
```

### 5. 访问服务

- **应用地址**：http://localhost:8080
- **Swagger UI**：http://localhost:8080/swagger-ui.html
- **OpenAPI 文档**：http://localhost:8080/api-docs

---

## 📚 API 接口文档

### 认证接口 (`/auth`)

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/auth/login` | 用户登录，返回 JWT Token |
| POST | `/auth/logout` | 用户登出，Token 加入黑名单 |
| POST | `/auth/refresh` | 刷新 Access Token |

**登录请求示例**：
```json
POST /auth/login
{
  "username": "admin",
  "password": "password123"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 3600
  }
}
```

### 知识库管理接口 (`/api/knowledge-bases`)

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/knowledge-bases` | 获取知识库列表 |
| GET | `/api/knowledge-bases/{id}` | 获取知识库详情 |
| POST | `/api/knowledge-bases` | 创建知识库 |
| PUT | `/api/knowledge-bases/{id}` | 更新知识库 |
| DELETE | `/api/knowledge-bases/{id}` | 删除知识库 |
| POST | `/api/knowledge-bases/{id}/documents` | 上传文档 |
| GET | `/api/knowledge-bases/{id}/statistics` | 获取知识库统计 |

### 问答接口 (`/api/qa`)

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/qa/ask` | 同步问答 |
| POST | `/api/qa/ask/stream` | 流式问答（SSE） |

**问答请求示例**：
```json
POST /api/qa/ask
{
  "kbId": 1,
  "question": "如何配置 Spring Security?",
  "topK": 5,
  "enableCache": true
}
```

### 问答历史接口 (`/api/history`)

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/history` | 获取问答历史列表 |
| GET | `/api/history/{id}` | 获取问答详情 |
| POST | `/api/history/{id}/feedback` | 提交反馈 |

### 任务接口 (`/api/tasks`)

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/tasks/{taskId}` | 查询任务状态 |

---

## 🔧 核心功能详解

### 1. RAG 问答流程

```
用户提问 → 问题向量化 → 相似度检索 → 构建 Prompt → LLM 生成答案 → 返回答案+引用
```

**详细步骤**：

1. **问题向量化**：调用 `EmbeddingService` 将问题转换为向量
2. **相似度检索**：通过 `VectorStore` 检索 Top-K 最相似的文档块
3. **上下文构建**：将检索到的文档块作为上下文构建 Prompt
4. **答案生成**：调用 LLM（如 OpenAI、通义千问）生成答案
5. **引用来源**：返回答案时附带引用的原始文档信息

### 2. 文档处理流程

```
上传文档 → 内容解析 → 文本分块 → 向量嵌入 → 存储向量
```

**分块策略**：
- 按固定大小分块（默认 500 字符，重叠 100 字符）
- 支持语义分块（保持段落完整性）

### 3. 接口限流

使用 Redis + Lua 脚本实现滑动窗口限流：

```java
@RateLimit(
    permits = 100,        // 窗口内最大请求数
    window = 60,          // 窗口大小（秒）
    dimension = USER      // 限流维度：USER/IP/API
)
@GetMapping("/api/qa/ask")
public Response ask(...) { ... }
```

### 4. 幂等性控制

使用 `@Idempotent` 注解实现接口幂等性：

```java
@Idempotent(
    key = "#request.requestId",
    expireSeconds = 300
)
@PostMapping("/api/knowledge-bases")
public Response create(...) { ... }
```

### 5. 链路追踪

每个请求自动生成 TraceId，贯穿整个调用链：

- 请求进入时通过 `TraceFilter` 生成 TraceId
- TraceId 存储在 MDC 中，自动包含在所有日志中
- 调用外部服务时通过 Header 传递 TraceId

---

## ⚙️ 配置说明

### application.yml 配置项

```yaml
# 服务器配置
server:
  port: 8080

# Spring 配置
spring:
  application:
    name: rag-qa-system
  profiles:
    active: dev

  # 数据源配置
  datasource:
    url: jdbc:mysql://localhost:3306/rag_qa
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20

  # Flyway 数据库迁移
  flyway:
    enabled: true
    locations: classpath:db/migration

  # Redis 配置
  data:
    redis:
      host: localhost
      port: 6379

# MyBatis-Plus 配置
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true

# JWT 配置
jwt:
  secret: your-256-bit-secret-key
  access-token-expiration: 3600000
  refresh-token-expiration: 604800000

# 日志配置
logging:
  level:
    root: INFO
    com.enterprise.rag: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n"
```

---

## 💻 开发指南

### 项目结构约定

```
rag-{module}/
├── src/
│   ├── main/
│   │   ├── java/com/enterprise/rag/{module}/
│   │   │   ├── config/        # 配置类
│   │   │   ├── controller/    # 控制器
│   │   │   ├── service/       # 服务接口
│   │   │   │   └── impl/      # 服务实现
│   │   │   ├── entity/        # 实体类
│   │   │   ├── dto/           # 数据传输对象
│   │   │   ├── mapper/        # MyBatis Mapper
│   │   │   └── exception/     # 异常类
│   │   └── resources/
│   │       ├── mapper/        # XML Mapper 文件
│   │       └── application.yml
│   └── test/
│       └── java/
└── pom.xml
```

### 代码规范

1. **命名规范**
   - 类名：大驼峰（PascalCase）
   - 方法名/变量名：小驼峰（camelCase）
   - 常量：全大写下划线分隔（UPPER_SNAKE_CASE）

2. **注释规范**
   - 类和公共方法必须有 Javadoc 注释
   - 复杂业务逻辑需要行内注释说明

3. **日志规范**
   - 使用 Slf4j 的 `@Slf4j` 注解
   - 敏感信息不要打印到日志

### 新增功能开发流程

1. **定义接口**：在对应模块的 `service/` 目录创建接口
2. **实现业务**：在 `service/impl/` 目录实现具体逻辑
3. **创建控制器**：在 `controller/` 目录暴露 REST API
4. **添加测试**：编写单元测试和属性测试
5. **更新文档**：更新 Swagger 注解和 README

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行指定模块测试
mvn test -pl rag-auth

# 运行指定测试类
mvn test -Dtest=JwtTokenProviderPropertyTest
```

### 常用 Maven 命令

```bash
# 编译（跳过测试）
mvn clean install -DskipTests

# 打包
mvn package

# 查看依赖树
mvn dependency:tree

# 更新版本
mvn versions:set -DnewVersion=1.1.0-SNAPSHOT
```

---

## ❓ 常见问题

### Q1: 启动报错 "Cannot connect to MySQL"

**解决方案**：
1. 确认 MySQL 服务已启动
2. 检查 `application.yml` 中的数据库配置
3. 确认数据库 `rag_qa` 已创建

### Q2: Redis 连接失败

**解决方案**：
1. 确认 Redis 服务已启动
2. 检查 Redis 端口是否正确（默认 6379）
3. 如果 Redis 设置了密码，确保配置正确

### Q3: 向量数据库连接失败

**解决方案**：
1. 确认向量数据库服务已启动
2. 检查向量数据库的连接配置
3. 确认网络通畅

### Q4: JWT Token 验证失败

**解决方案**：
1. 检查 Token 是否过期
2. 确认请求头格式：`Authorization: Bearer <token>`
3. 验证 JWT secret 配置是否一致

### Q5: 文档上传后未被索引

**解决方案**：
1. 检查异步任务状态：`GET /api/tasks/{taskId}`
2. 查看日志中的错误信息
3. 确认向量数据库服务正常

---

## 📞 联系方式

如有问题，请联系项目维护团队或提交 Issue。

---

*文档版本：1.0.0*
*最后更新：2024年12月*

