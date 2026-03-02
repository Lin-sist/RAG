# 新手入门指南

本指南将帮助你快速了解并上手 Enterprise RAG QA System 项目。

## 📑 目录

1. [学习路径](#学习路径)
2. [核心概念](#核心概念)
3. [本地开发环境搭建](#本地开发环境搭建)
4. [第一个问答请求](#第一个问答请求)
5. [代码阅读指南](#代码阅读指南)
6. [动手实践任务](#动手实践任务)
7. [进阶学习资源](#进阶学习资源)

---

## 🛤️ 学习路径

建议按以下顺序学习本项目：

```
Week 1: 基础知识
├── 了解 RAG（检索增强生成）概念
├── 熟悉 Spring Boot 3.x 基础
└── 了解 JWT 认证机制

Week 2: 环境搭建与运行
├── 搭建本地开发环境
├── 运行项目并测试 API
└── 理解项目模块结构

Week 3: 核心功能理解
├── 阅读认证模块代码（rag-auth）
├── 阅读文档处理模块代码（rag-document）
└── 阅读 RAG 核心模块代码（rag-core）

Week 4: 深入学习
├── 理解向量存储与检索原理
├── 学习限流、幂等性等基础设施
└── 尝试添加新功能
```

---

## 🧠 核心概念

### 什么是 RAG？

**RAG（Retrieval-Augmented Generation）** 是一种结合检索和生成的 AI 技术：

1. **检索（Retrieval）**：从知识库中查找与问题相关的文档片段
2. **增强（Augmented）**：将检索到的信息作为上下文
3. **生成（Generation）**：基于上下文让 LLM 生成答案

```
传统 LLM：问题 → LLM → 答案（可能产生幻觉）

RAG 流程：问题 → 检索相关文档 → 文档 + 问题 → LLM → 答案（更准确，有来源）
```

### 向量嵌入（Embedding）

将文本转换为数值向量，使得语义相似的文本在向量空间中距离更近。

```
"如何配置 Spring Security" → [0.12, -0.34, 0.56, ...]
"Spring Security 配置方法" → [0.11, -0.35, 0.57, ...]  // 向量相似
"今天天气怎么样"          → [-0.45, 0.23, -0.78, ...] // 向量不相似
```

### 向量数据库

专门用于存储和检索向量数据的数据库。支持快速的相似度搜索。

本项目支持：
- **Milvus**：高性能开源向量数据库
- **Qdrant**：Rust 编写的向量搜索引擎
- **Elasticsearch**：支持向量检索的搜索引擎

### JWT（JSON Web Token）

一种用于身份验证的开放标准：

```
Header.Payload.Signature

eyJhbGciOiJIUzI1NiJ9.     // Header: 算法信息
eyJ1c2VySWQiOjEsLi4ufQ.   // Payload: 用户信息
dBjftJeZ4CVP-mB92K27uhbU  // Signature: 签名验证
```

---

## 💻 本地开发环境搭建

### 1. 安装必要软件

#### JDK 17

**Windows**：
1. 下载 [OpenJDK 17](https://adoptium.net/)
2. 设置环境变量 `JAVA_HOME`
3. 验证：`java -version`

#### Maven

1. 下载 [Maven](https://maven.apache.org/download.cgi)
2. 解压并配置环境变量
3. 验证：`mvn -version`

#### MySQL 8.x

1. 下载 [MySQL Community Server](https://dev.mysql.com/downloads/mysql/)
2. 安装并创建数据库：
   ```sql
   CREATE DATABASE rag_qa CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

#### Redis

**Windows**：
1. 下载 [Redis for Windows](https://github.com/microsoftarchive/redis/releases) 或使用 WSL
2. 启动 Redis 服务

### 2. IDE 配置（IntelliJ IDEA）

1. 打开项目：`File → Open → 选择 RAG 目录`
2. 等待 Maven 自动导入依赖
3. 设置 JDK：`File → Project Structure → SDK → 17`
4. 安装插件（推荐）：
   - Lombok
   - MyBatisX
   - Spring Boot Assistant

### 3. 配置文件修改

编辑 `rag-admin/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rag_qa
    username: root          # 修改为你的用户名
    password: your_password # 修改为你的密码
  data:
    redis:
      host: localhost
      port: 6379
```

### 4. 编译并运行

```bash
# 编译项目
mvn clean install -DskipTests

# 运行
cd rag-admin
mvn spring-boot:run
```

看到以下日志说明启动成功：

```
Started RagQaApplication in X.XXX seconds
```

---

## 🎯 第一个问答请求

### Step 1: 用户登录

```bash
# 使用 curl 或 Postman
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

响应：
```json
{
  "code": 200,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "...",
    "expiresIn": 3600
  }
}
```

保存 `accessToken` 用于后续请求。

### Step 2: 创建知识库

```bash
curl -X POST http://localhost:8080/api/knowledge-bases \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-access-token>" \
  -d '{"name":"技术文档库","description":"内部技术文档知识库"}'
```

### Step 3: 上传文档

```bash
curl -X POST http://localhost:8080/api/knowledge-bases/1/documents \
  -H "Authorization: Bearer <your-access-token>" \
  -F "file=@/path/to/your/document.pdf"
```

### Step 4: 发起问答

```bash
curl -X POST http://localhost:8080/api/qa/ask \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-access-token>" \
  -d '{"kbId":1,"question":"如何配置 Spring Security?","topK":5}'
```

响应：
```json
{
  "code": 200,
  "data": {
    "question": "如何配置 Spring Security?",
    "answer": "根据文档，配置 Spring Security 需要以下步骤...",
    "citations": [...],
    "contexts": [...]
  }
}
```

### 使用 Swagger UI

更便捷的方式是使用 Swagger UI：

1. 打开浏览器访问：http://localhost:8080/swagger-ui.html
2. 在 Swagger 界面中测试各个 API

---

## 📖 代码阅读指南

### 推荐阅读顺序

```
1. 入口文件
   └── rag-admin/src/main/java/.../RagQaApplication.java

2. 认证流程
   ├── AuthController.java      # 认证接口
   ├── JwtTokenProvider.java    # Token 生成/验证
   ├── SecurityConfig.java      # Security 配置
   └── JwtAuthenticationFilter  # 认证过滤器

3. RAG 核心流程
   ├── QAController.java        # 问答接口入口
   ├── RAGService.java          # RAG 服务协调
   ├── QueryEngine.java         # 检索引擎
   ├── EmbeddingService.java    # 向量嵌入
   ├── VectorStore.java         # 向量存储
   └── AnswerGenerator.java     # 答案生成

4. 文档处理
   ├── DocumentProcessor.java   # 文档处理入口
   ├── DocumentParser           # 文档解析
   └── DocumentChunker          # 文档分块

5. 基础设施
   ├── TraceFilter.java         # 链路追踪
   ├── RateLimiter.java         # 限流器
   └── IdempotencyHandler.java  # 幂等性处理
```

### 关键代码解读

#### 1. JWT Token 生成

```java
// JwtTokenProvider.java
public String generateAccessToken(UserPrincipal user) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expirationMs);
    
    return Jwts.builder()
        .claims(claims)           // 用户信息
        .subject(user.getUsername())
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(secretKey)      // 签名
        .compact();
}
```

#### 2. RAG 问答核心逻辑

```java
// RAGServiceImpl.java
public QAResponse ask(QARequest request) {
    // 1. 检查缓存
    QAResponse cached = getFromCache(question);
    if (cached != null) return cached;
    
    // 2. 检索相关文档
    List<RetrievedContext> contexts = queryEngine.retrieve(question, options);
    
    // 3. 处理无结果
    if (contexts.isEmpty()) {
        return QAResponse.noResult(question);
    }
    
    // 4. 生成答案
    GeneratedAnswer answer = answerGenerator.generate(question, contexts);
    
    // 5. 缓存并返回
    saveToCache(question, response);
    return response;
}
```

#### 3. 向量相似度检索

```java
// VectorStore 接口
void upsert(String collection, List<VectorDocument> documents);  // 存储
List<SearchResult> search(String collection, float[] vector, int topK);  // 检索
```

---

## 🏋️ 动手实践任务

### 初级任务

1. **任务1：添加健康检查接口**
   - 在 `rag-admin` 模块创建 `HealthController`
   - 实现 `GET /health` 接口，返回服务状态

2. **任务2：添加日志统计**
   - 统计每天的问答请求数量
   - 创建新的 Mapper 查询

### 中级任务

3. **任务3：新增文档类型支持**
   - 在 `rag-document` 模块添加新的解析器
   - 例如：支持 `.txt` 文件解析

4. **任务4：添加问答结果导出**
   - 实现将问答历史导出为 CSV/Excel

### 高级任务

5. **任务5：实现多轮对话**
   - 保存对话上下文
   - 在新问题中引用之前的对话

6. **任务6：添加新的嵌入模型**
   - 实现新的 `EmbeddingProvider`
   - 例如：接入本地 Ollama 服务

---

## 📚 进阶学习资源

### RAG 相关

- [LangChain RAG 教程](https://python.langchain.com/docs/tutorials/rag/)
- [RAG 原理详解](https://www.pinecone.io/learn/retrieval-augmented-generation/)
- [向量数据库对比](https://benchmark.vectorview.ai/)

### 技术栈相关

- [Spring Boot 3 官方文档](https://docs.spring.io/spring-boot/docs/3.2.x/reference/html/)
- [Spring Security 参考](https://docs.spring.io/spring-security/reference/)
- [MyBatis-Plus 文档](https://baomidou.com/)
- [Milvus 官方文档](https://milvus.io/docs)

### 视频教程

- B站搜索 "RAG 教程"
- YouTube: "Building RAG Applications"

### 推荐书籍

- 《Spring Boot 实战》
- 《深入理解 Spring Cloud》
- 《大规模语言模型：从理论到实践》

---

## 🎓 学习检查点

完成以下检查点，确认你已掌握项目基础：

- [ ] 能够独立搭建本地开发环境
- [ ] 理解 RAG 的基本工作原理
- [ ] 能够运行项目并使用 Swagger 测试 API
- [ ] 理解项目的模块划分和依赖关系
- [ ] 能够阅读并理解核心代码逻辑
- [ ] 完成至少一个初级动手任务
- [ ] 能够添加简单的新功能

---

## 🆘 寻求帮助

遇到问题时：

1. 先查看日志（`target/logs` 或控制台输出）
2. 检查配置文件是否正确
3. 查阅本文档的常见问题部分
4. 搜索相关技术文档
5. 向团队成员请教

**祝你学习愉快！** 🎉

