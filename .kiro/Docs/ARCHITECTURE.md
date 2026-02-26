# 架构设计文档

本文档详细说明 Enterprise RAG QA System 的架构设计、技术选型和设计决策。

---

## 📐 系统架构

### 整体架构

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                                    客户端                                        │
│                     (Web Browser / Mobile App / API Client)                     │
└────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        │ HTTPS
                                        ▼
┌────────────────────────────────────────────────────────────────────────────────┐
│                                 API 网关层                                       │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────────────┐   │
│  │  TraceFilter │ │  RateLimit   │ │  JwtFilter   │ │  GlobalExceptionHandler│   │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────────────┘   │
└────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌────────────────────────────────────────────────────────────────────────────────┐
│                               应用层 (rag-admin)                                 │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────────────┐   │
│  │AuthController│ │ QAController │ │  KBController│ │  HistoryController  │   │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────────────┘   │
└────────────────────────────────────────────────────────────────────────────────┘
                                        │
                    ┌───────────────────┼───────────────────┐
                    ▼                   ▼                   ▼
          ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
          │    rag-auth     │  │    rag-core     │  │  rag-document   │
          │                 │  │                 │  │                 │
          │ • JWT Provider  │  │ • RAGService    │  │ • DocProcessor  │
          │ • AuthService   │  │ • Embedding     │  │ • Parser        │
          │ • Security      │  │ • VectorStore   │  │ • Chunker       │
          └─────────────────┘  └─────────────────┘  └─────────────────┘
                    │                   │                   │
                    └───────────────────┴───────────────────┘
                                        │
                                        ▼
                              ┌─────────────────┐
                              │   rag-common    │
                              │                 │
                              │ • RateLimiter   │
                              │ • TraceContext  │
                              │ • Idempotency   │
                              │ • AsyncTask     │
                              │ • RedisUtil     │
                              └─────────────────┘
                                        │
                    ┌───────────────────┼───────────────────┐
                    ▼                   ▼                   ▼
          ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
          │     MySQL       │  │     Redis       │  │   VectorDB      │
          │                 │  │                 │  │                 │
          │ • 用户数据      │  │ • 会话缓存      │  │ • 向量存储      │
          │ • 知识库数据    │  │ • Token黑名单   │  │ • 相似度检索    │
          │ • 问答历史      │  │ • 限流计数      │  │ (Milvus/Qdrant) │
          └─────────────────┘  └─────────────────┘  └─────────────────┘
                                        │
                                        ▼
                              ┌─────────────────┐
                              │   外部服务       │
                              │                 │
                              │ • LLM API       │
                              │ • Embedding API │
                              └─────────────────┘
```

---

## 🧩 模块依赖关系

```
                    ┌─────────────┐
                    │  rag-admin  │  (主应用入口)
                    └─────────────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
         ▼                 ▼                 ▼
┌─────────────┐   ┌─────────────┐   ┌──────────────┐
│  rag-auth   │   │  rag-core   │   │ rag-document │
└─────────────┘   └─────────────┘   └──────────────┘
         │                 │                 │
         │                 ▼                 │
         │        ┌──────────────┐           │
         │        │ rag-document │           │
         │        └──────────────┘           │
         │                 │                 │
         └────────────────┬┴─────────────────┘
                          ▼
                  ┌─────────────┐
                  │ rag-common  │  (基础模块)
                  └─────────────┘
```

### 依赖说明

| 模块 | 依赖 | 职责 |
|------|------|------|
| rag-common | - | 公共工具、基础设施 |
| rag-auth | rag-common | 认证授权 |
| rag-document | rag-common | 文档处理 |
| rag-core | rag-common, rag-document | RAG 核心 |
| rag-admin | 所有模块 | 应用入口、API |

---

## 🔐 认证架构

### JWT 认证流程

```
┌─────────┐                                                    ┌─────────┐
│ Client  │                                                    │ Server  │
└────┬────┘                                                    └────┬────┘
     │                                                              │
     │  1. POST /auth/login {username, password}                    │
     │ ─────────────────────────────────────────────────────────────>
     │                                                              │
     │  2. Validate credentials                                     │
     │                                                              │
     │  3. {accessToken, refreshToken}                              │
     │ <─────────────────────────────────────────────────────────────
     │                                                              │
     │  4. GET /api/qa/ask (Authorization: Bearer <accessToken>)    │
     │ ─────────────────────────────────────────────────────────────>
     │                                                              │
     │  5. JwtFilter: Validate token                                │
     │     - 检查签名                                                │
     │     - 检查过期时间                                            │
     │     - 检查黑名单                                              │
     │                                                              │
     │  6. Response                                                 │
     │ <─────────────────────────────────────────────────────────────
     │                                                              │
     │  7. POST /auth/logout                                        │
     │ ─────────────────────────────────────────────────────────────>
     │                                                              │
     │  8. Add token to blacklist (Redis)                           │
     │                                                              │
     │  9. Success                                                  │
     │ <─────────────────────────────────────────────────────────────
```

### Token 黑名单机制

```
登出请求 → 提取 Token → 计算剩余过期时间 → 存入 Redis (TTL = 剩余过期时间)

验证请求 → 提取 Token → 验证签名 → 检查 Redis 黑名单 → 通过/拒绝
```

---

## 🔄 RAG 工作流程

### 问答流程

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                              RAG 问答流程                                        │
└────────────────────────────────────────────────────────────────────────────────┘

1. 问题处理
   ┌──────────┐      ┌────────────────┐      ┌─────────────┐
   │ Question │ ───> │ EmbeddingService│ ───> │ Query Vector│
   └──────────┘      └────────────────┘      └─────────────┘

2. 文档检索
   ┌─────────────┐      ┌─────────────┐      ┌───────────────────┐
   │ Query Vector│ ───> │ VectorStore │ ───> │ Top-K Documents   │
   └─────────────┘      │  .search()  │      │ (with similarity) │
                        └─────────────┘      └───────────────────┘

3. 上下文构建
   ┌───────────────────┐      ┌───────────────┐      ┌──────────────┐
   │ Top-K Documents   │ ───> │ PromptBuilder │ ───> │ Context Prompt│
   │ (sorted by score) │      │               │      │              │
   └───────────────────┘      └───────────────┘      └──────────────┘

4. 答案生成
   ┌──────────────┐      ┌────────────────┐      ┌────────────┐
   │Context Prompt│ ───> │ AnswerGenerator│ ───> │ LLM (GPT等)│
   │ + Question   │      │                │      │            │
   └──────────────┘      └────────────────┘      └────────────┘

5. 响应返回
   ┌────────────┐      ┌─────────────────┐      ┌────────────────┐
   │ LLM Answer │ ───> │ Response Builder│ ───> │ QAResponse     │
   │            │      │ + Citations     │      │ (answer+refs)  │
   └────────────┘      └─────────────────┘      └────────────────┘
```

### 文档索引流程

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                              文档索引流程                                        │
└────────────────────────────────────────────────────────────────────────────────┘

1. 文档上传
   ┌────────────┐      ┌────────────────┐      ┌───────────────┐
   │ Upload File│ ───> │ 幂等性检查     │ ───> │ 创建异步任务  │
   │ (PDF/MD/...) │      │ (contentHash)  │      │ (返回taskId) │
   └────────────┘      └────────────────┘      └───────────────┘

2. 文档解析 (异步)
   ┌───────────────┐      ┌──────────────────┐      ┌─────────────┐
   │ AsyncTask     │ ───> │ DocumentProcessor│ ───> │ Raw Content │
   │               │      │ (PDF/MD Parser)  │      │             │
   └───────────────┘      └──────────────────┘      └─────────────┘

3. 文档分块
   ┌─────────────┐      ┌────────────────┐      ┌─────────────────┐
   │ Raw Content │ ───> │ DocumentChunker│ ───> │ List<Chunk>     │
   │             │      │ (语义分块)     │      │ (500字/块)      │
   └─────────────┘      └────────────────┘      └─────────────────┘

4. 向量生成
   ┌─────────────────┐      ┌─────────────────┐      ┌──────────────┐
   │ List<Chunk>     │ ───> │ EmbeddingService│ ───> │ List<Vector> │
   │                 │      │ (批量处理)      │      │              │
   └─────────────────┘      └─────────────────┘      └──────────────┘

5. 向量存储
   ┌──────────────┐      ┌─────────────┐      ┌─────────────────┐
   │ List<Vector> │ ───> │ VectorStore │ ───> │ 索引完成        │
   │ + Metadata   │      │ .upsert()   │      │ 更新任务状态    │
   └──────────────┘      └─────────────┘      └─────────────────┘
```

---

## 📊 数据模型

### 数据库 ER 图

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│     user     │       │     role     │       │  permission  │
├──────────────┤       ├──────────────┤       ├──────────────┤
│ id           │       │ id           │       │ id           │
│ username     │       │ name         │       │ code         │
│ password_hash│       │ description  │       │ name         │
│ email        │       │ created_at   │       │ resource     │
│ enabled      │       │ updated_at   │       │ action       │
│ created_at   │       │ deleted      │       │ created_at   │
│ updated_at   │       │ version      │       │ updated_at   │
│ deleted      │       └──────────────┘       │ deleted      │
│ version      │                              │ version      │
└──────────────┘                              └──────────────┘
        │                     │                      │
        │    ┌────────────────┘                      │
        ▼    ▼                                       ▼
┌──────────────┐                            ┌──────────────────┐
│  user_role   │                            │ role_permission  │
├──────────────┤                            ├──────────────────┤
│ id           │                            │ id               │
│ user_id (FK) │                            │ role_id (FK)     │
│ role_id (FK) │                            │ permission_id (FK)│
│ created_at   │                            │ created_at       │
└──────────────┘                            └──────────────────┘


┌──────────────────┐
│  knowledge_base  │
├──────────────────┤
│ id               │
│ name             │
│ description      │
│ owner_id (FK)    │──────────────────────┐
│ vector_collection│                      │
│ document_count   │                      │
│ is_public        │                      │
│ created_at       │                      │
│ updated_at       │                      │
│ deleted          │                      │
│ version          │                      │
└──────────────────┘                      │
        │                                 │
        │ 1:N                             │
        ▼                                 │
┌──────────────────┐                      │
│    document      │                      │
├──────────────────┤                      │
│ id               │                      │
│ kb_id (FK)       │                      │
│ uploader_id (FK) │──────────────────────┤
│ title            │                      │
│ file_path        │                      │
│ file_type        │                      │
│ content_hash     │                      │
│ status           │                      │
│ chunk_count      │                      │
│ created_at       │                      │
│ updated_at       │                      │
│ deleted          │                      │
│ version          │                      │
└──────────────────┘                      │
        │                                 │
        │ 1:N                             │
        ▼                                 │
┌──────────────────┐                      │
│ document_chunk   │                      │
├──────────────────┤                      │
│ id               │                      │
│ document_id (FK) │                      │
│ vector_id        │ ──> VectorDB         │
│ content          │                      │
│ chunk_index      │                      │
│ start_pos        │                      │
│ end_pos          │                      │
│ metadata (JSON)  │                      │
│ created_at       │                      │
│ updated_at       │                      │
│ deleted          │                      │
│ version          │                      │
└──────────────────┘                      │
                                          │
                                          │
┌──────────────────┐                      │
│   qa_history     │                      │
├──────────────────┤                      │
│ id               │                      │
│ user_id (FK)     │──────────────────────┘
│ kb_id (FK)       │
│ question         │
│ answer           │
│ citations (JSON) │
│ trace_id         │
│ latency_ms       │
│ created_at       │
│ updated_at       │
│ deleted          │
│ version          │
└──────────────────┘
        │
        │ 1:1
        ▼
┌──────────────────┐
│   qa_feedback    │
├──────────────────┤
│ id               │
│ qa_id (FK)       │
│ user_id (FK)     │
│ rating           │
│ comment          │
│ created_at       │
│ updated_at       │
│ deleted          │
│ version          │
└──────────────────┘
```

---

## 🔌 接口设计

### API 设计原则

1. **RESTful 风格**：资源导向，使用 HTTP 动词
2. **版本控制**：通过 URL 路径（如 `/api/v1/`）
3. **统一响应格式**：`{code, message, data, traceId}`
4. **分页规范**：`{items, total, page, size, totalPages}`

### 响应格式

```java
public class ApiResponse<T> {
    private int code;           // 状态码
    private String message;     // 消息
    private T data;             // 数据
    private String traceId;     // 追踪 ID
    private LocalDateTime timestamp; // 时间戳
}
```

---

## ⚡ 性能优化

### 缓存策略

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                              多级缓存架构                                        │
└────────────────────────────────────────────────────────────────────────────────┘

L1: 本地缓存 (Caffeine)
    │
    │ Miss
    ▼
L2: 分布式缓存 (Redis)
    │
    │ Miss
    ▼
L3: 数据库 / 外部服务
```

### 缓存类型

| 缓存类型 | 存储位置 | TTL | 用途 |
|---------|---------|-----|------|
| Embedding 缓存 | Redis | 24h | 文本向量结果 |
| 问答缓存 | Redis | 1h | 相同问题的答案 |
| 会话缓存 | Redis | Token有效期 | 用户会话信息 |
| 限流计数 | Redis | 窗口时间 | 请求计数 |

### 异步处理

```java
// 使用 CompletableFuture 异步处理
@Async
public CompletableFuture<ProcessResult> processDocumentAsync(DocumentInput input) {
    // 文档处理逻辑
    return CompletableFuture.completedFuture(result);
}
```

---

## 🛡️ 安全设计

### 安全措施

| 措施 | 实现方式 | 说明 |
|------|---------|------|
| 认证 | JWT Token | 无状态、可扩展 |
| 授权 | Spring Security | 基于角色/权限 |
| 密码加密 | BCrypt | 单向哈希 |
| Token 安全 | HMAC-SHA256 签名 | 防篡改 |
| 传输安全 | HTTPS | 加密传输 |
| 限流 | Redis + Lua | 防止暴力攻击 |
| 幂等性 | 唯一 Key | 防重复提交 |

### 输入验证

```java
public record CreateKnowledgeBaseRequest(
    @NotBlank(message = "名称不能为空")
    @Size(min = 2, max = 100, message = "名称长度需在2-100之间")
    String name,
    
    @Size(max = 500, message = "描述不能超过500字符")
    String description
) {}
```

---

## 📈 可观测性

### 链路追踪

```
Request → TraceFilter (生成 TraceId)
    │
    ├── Controller [traceId=xxx]
    │
    ├── Service [traceId=xxx]
    │
    ├── External Call [X-Trace-Id: xxx]
    │
    └── Response [X-Trace-Id: xxx]
```

### 日志格式

```
2024-01-15 10:30:45.123 [http-nio-8080-exec-1] [abc123def456] INFO  QAController - 问答请求: question=xxx
```

### 监控指标

| 指标 | 说明 |
|------|------|
| 请求量 | QPS、日请求量 |
| 延迟 | P50、P95、P99 响应时间 |
| 错误率 | 4xx、5xx 错误占比 |
| 缓存命中率 | 缓存有效性 |
| 向量检索延迟 | 向量数据库性能 |
| LLM 调用延迟 | 答案生成性能 |

---

## 🔧 扩展点

### 扩展嵌入模型

实现 `EmbeddingProvider` 接口：

```java
public interface EmbeddingProvider {
    String getName();
    float[] embed(String text);
    List<float[]> embedBatch(List<String> texts);
    int getDimension();
    boolean isAvailable();
}
```

### 扩展向量存储

实现 `VectorStore` 接口：

```java
public interface VectorStore {
    void createCollection(String name, int dimension);
    void upsert(String collection, List<VectorDocument> documents);
    List<SearchResult> search(String collection, float[] vector, SearchOptions options);
    void delete(String collection, List<String> ids);
}
```

### 扩展文档解析

实现 `DocumentParser` 接口：

```java
public interface DocumentParser {
    boolean supports(String fileType);
    String parse(InputStream input, String filename);
}
```

---

## 📋 设计决策记录

### ADR-001: 选择 JWT 作为认证方案

**决策**：使用 JWT Token 进行认证

**原因**：
- 无状态，易于水平扩展
- 自包含用户信息，减少数据库查询
- 支持跨域认证

**后果**：
- 需要额外实现 Token 黑名单机制
- Token 刷新需要客户端配合

### ADR-002: 自主实现 RAG 流程

**决策**：不使用 LangChain 等框架，自主实现 RAG 流程

**原因**：
- 更好的控制和优化能力
- 减少外部依赖
- 便于定制化开发

**后果**：
- 开发工作量增加
- 需要自行处理各种边界情况

### ADR-003: 选择 Redis 作为缓存和限流存储

**决策**：使用 Redis 统一处理缓存、限流、幂等性

**原因**：
- 高性能
- 支持 Lua 脚本（原子操作）
- 丰富的数据结构

**后果**：
- 增加 Redis 运维成本
- 需要处理 Redis 故障场景

