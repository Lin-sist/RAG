# API 接口详细文档

本文档提供 Enterprise RAG QA System 所有 API 接口的详细说明。

---

## 🔑 认证说明

除登录接口外，所有接口都需要在请求头中携带 JWT Token：

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Token 说明

| Token 类型 | 用途 | 有效期 |
|-----------|------|--------|
| Access Token | API 访问凭证 | 1 小时（默认） |
| Refresh Token | 刷新 Access Token | 7 天（默认） |

---

## 📋 接口列表

### 1. 认证管理 `/auth`

#### 1.1 用户登录

```http
POST /auth/login
```

**请求体**：
```json
{
  "username": "admin",
  "password": "password123"
}
```

**响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 3600,
    "tokenType": "Bearer"
  },
  "traceId": "a1b2c3d4e5f6"
}
```

**响应码**：
| 状态码 | 说明 |
|-------|------|
| 200 | 登录成功 |
| 401 | 用户名或密码错误 |
| 400 | 请求参数错误 |

---

#### 1.2 用户登出

```http
POST /auth/logout
```

**请求头**：
```http
Authorization: Bearer <access_token>
```

**响应**：
```json
{
  "code": 200,
  "message": "登出成功",
  "data": null,
  "traceId": "a1b2c3d4e5f6"
}
```

---

#### 1.3 刷新 Token

```http
POST /auth/refresh
```

**请求体**：
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 3600,
    "tokenType": "Bearer"
  },
  "traceId": "a1b2c3d4e5f6"
}
```

---

### 2. 知识库管理 `/api/knowledge-bases`

#### 2.1 获取知识库列表

```http
GET /api/knowledge-bases
```

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页数量，默认 10 |
| keyword | string | 否 | 搜索关键词 |

**响应**：
```json
{
  "code": 200,
  "data": {
    "items": [
      {
        "id": 1,
        "name": "技术文档库",
        "description": "内部技术文档",
        "ownerId": 1,
        "documentCount": 15,
        "isPublic": false,
        "createdAt": "2024-01-01T10:00:00",
        "updatedAt": "2024-01-15T14:30:00"
      }
    ],
    "total": 5,
    "page": 1,
    "size": 10,
    "totalPages": 1
  },
  "traceId": "a1b2c3d4e5f6"
}
```

---

#### 2.2 获取知识库详情

```http
GET /api/knowledge-bases/{id}
```

**路径参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| id | long | 知识库 ID |

**响应**：
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "name": "技术文档库",
    "description": "内部技术文档",
    "ownerId": 1,
    "vectorCollection": "kb_1",
    "documentCount": 15,
    "isPublic": false,
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-15T14:30:00"
  },
  "traceId": "a1b2c3d4e5f6"
}
```

---

#### 2.3 创建知识库

```http
POST /api/knowledge-bases
```

**请求体**：
```json
{
  "name": "技术文档库",
  "description": "内部技术文档知识库",
  "isPublic": false
}
```

**字段说明**：
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 知识库名称（2-100字符） |
| description | string | 否 | 知识库描述 |
| isPublic | boolean | 否 | 是否公开，默认 false |

**响应**：
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "name": "技术文档库",
    "description": "内部技术文档知识库",
    "ownerId": 1,
    "vectorCollection": "kb_1",
    "documentCount": 0,
    "isPublic": false,
    "createdAt": "2024-01-01T10:00:00"
  },
  "traceId": "a1b2c3d4e5f6"
}
```

---

#### 2.4 更新知识库

```http
PUT /api/knowledge-bases/{id}
```

**请求体**：
```json
{
  "name": "更新后的名称",
  "description": "更新后的描述",
  "isPublic": true
}
```

---

#### 2.5 删除知识库

```http
DELETE /api/knowledge-bases/{id}
```

**响应**：
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null,
  "traceId": "a1b2c3d4e5f6"
}
```

> ⚠️ 删除知识库会同时删除所有关联的文档和向量数据

---

#### 2.6 上传文档

```http
POST /api/knowledge-bases/{id}/documents
Content-Type: multipart/form-data
```

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | file | 是 | 文档文件 |
| title | string | 否 | 文档标题（默认使用文件名） |

**支持的文件格式**：
- PDF (`.pdf`)
- Word (`.docx`)
- Markdown (`.md`)
- 代码文件 (`.java`, `.py`, `.js`, `.ts`, `.go` 等)

**响应**：
```json
{
  "code": 200,
  "data": {
    "documentId": 1,
    "taskId": "task_abc123",
    "status": "PROCESSING",
    "message": "文档已提交处理，请通过任务 ID 查询进度"
  },
  "traceId": "a1b2c3d4e5f6"
}
```

---

#### 2.7 获取知识库文档列表

```http
GET /api/knowledge-bases/{id}/documents
```

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码 |
| size | int | 否 | 每页数量 |
| status | string | 否 | 文档状态过滤 |

**文档状态**：
| 状态 | 说明 |
|------|------|
| PENDING | 待处理 |
| PROCESSING | 处理中 |
| COMPLETED | 已完成 |
| FAILED | 处理失败 |

---

#### 2.8 获取知识库统计

```http
GET /api/knowledge-bases/{id}/statistics
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "documentCount": 15,
    "chunkCount": 238,
    "vectorCount": 238,
    "totalQueryCount": 1024,
    "avgQueryLatencyMs": 156,
    "lastQueryTime": "2024-01-15T14:30:00"
  },
  "traceId": "a1b2c3d4e5f6"
}
```

---

### 3. 问答服务 `/api/qa`

#### 3.1 同步问答

```http
POST /api/qa/ask
```

**请求体**：
```json
{
  "kbId": 1,
  "question": "如何配置 Spring Security?",
  "topK": 5,
  "enableCache": true,
  "filter": {
    "documentType": "markdown"
  }
}
```

**字段说明**：
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| kbId | long | 是 | 知识库 ID |
| question | string | 是 | 问题内容 |
| topK | int | 否 | 检索文档数量，默认 5 |
| enableCache | boolean | 否 | 是否启用缓存，默认 true |
| filter | object | 否 | 元数据过滤条件 |

**响应**：
```json
{
  "code": 200,
  "data": {
    "question": "如何配置 Spring Security?",
    "answer": "根据您的知识库文档，配置 Spring Security 需要以下步骤：\n\n1. 添加依赖...\n2. 创建配置类...",
    "citations": [
      {
        "documentId": 1,
        "documentTitle": "Spring Security 指南.md",
        "chunkIndex": 3,
        "content": "配置 Spring Security 的第一步是添加 spring-boot-starter-security 依赖...",
        "score": 0.92
      },
      {
        "documentId": 2,
        "documentTitle": "安全配置最佳实践.pdf",
        "chunkIndex": 7,
        "content": "SecurityConfig 类需要继承 WebSecurityConfigurerAdapter...",
        "score": 0.87
      }
    ],
    "contexts": [...],
    "metadata": {
      "cached": false,
      "contextCount": 5,
      "latencyMs": 1234,
      "model": "gpt-3.5-turbo"
    }
  },
  "traceId": "a1b2c3d4e5f6"
}
```

---

#### 3.2 流式问答

```http
POST /api/qa/ask/stream
Content-Type: application/json
Accept: text/event-stream
```

**请求体**：与同步问答相同

**响应**（SSE 格式）：
```
data: 根据

data: 您的

data: 知识库

data: 文档，

data: 配置

data: Spring

data: Security

data: 需要

...

data: [DONE]
```

---

### 4. 问答历史 `/api/history`

#### 4.1 获取问答历史列表

```http
GET /api/history
```

**查询参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码 |
| size | int | 否 | 每页数量 |
| kbId | long | 否 | 知识库 ID 过滤 |
| startDate | string | 否 | 开始日期 (yyyy-MM-dd) |
| endDate | string | 否 | 结束日期 (yyyy-MM-dd) |

**响应**：
```json
{
  "code": 200,
  "data": {
    "items": [
      {
        "id": 1,
        "question": "如何配置 Spring Security?",
        "answer": "根据文档...",
        "kbId": 1,
        "kbName": "技术文档库",
        "latencyMs": 1234,
        "createdAt": "2024-01-15T10:30:00"
      }
    ],
    "total": 50,
    "page": 1,
    "size": 10
  },
  "traceId": "a1b2c3d4e5f6"
}
```

---

#### 4.2 获取问答详情

```http
GET /api/history/{id}
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "question": "如何配置 Spring Security?",
    "answer": "根据文档...",
    "citations": [...],
    "kbId": 1,
    "kbName": "技术文档库",
    "traceId": "original_trace_id",
    "latencyMs": 1234,
    "feedback": {
      "rating": 5,
      "comment": "非常有帮助"
    },
    "createdAt": "2024-01-15T10:30:00"
  },
  "traceId": "a1b2c3d4e5f6"
}
```

---

#### 4.3 提交问答反馈

```http
POST /api/history/{id}/feedback
```

**请求体**：
```json
{
  "rating": 5,
  "comment": "答案非常准确，帮助很大"
}
```

**字段说明**：
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| rating | int | 是 | 评分（1-5） |
| comment | string | 否 | 评论内容 |

---

### 5. 任务管理 `/api/tasks`

#### 5.1 查询任务状态

```http
GET /api/tasks/{taskId}
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "taskId": "task_abc123",
    "taskType": "DOCUMENT_INDEX",
    "status": "RUNNING",
    "progress": 65,
    "message": "正在处理第 13/20 个文档块",
    "result": null,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:31:30"
  },
  "traceId": "a1b2c3d4e5f6"
}
```

**任务状态**：
| 状态 | 说明 |
|------|------|
| PENDING | 待执行 |
| RUNNING | 执行中 |
| COMPLETED | 已完成 |
| FAILED | 执行失败 |
| CANCELLED | 已取消 |

---

## ⚠️ 错误码说明

### 通用错误

| 错误码 | HTTP 状态码 | 说明 |
|-------|------------|------|
| 400 | 400 | 请求参数错误 |
| 401 | 401 | 未认证或 Token 无效 |
| 403 | 403 | 权限不足 |
| 404 | 404 | 资源不存在 |
| 429 | 429 | 请求过于频繁 |
| 500 | 500 | 服务器内部错误 |

### 业务错误

| 错误码 | 说明 |
|-------|------|
| AUTH_001 | 用户名或密码错误 |
| AUTH_002 | Token 已过期 |
| AUTH_003 | Token 已被列入黑名单 |
| KB_001 | 知识库不存在 |
| KB_002 | 知识库名称已存在 |
| DOC_001 | 不支持的文件格式 |
| DOC_002 | 文件大小超出限制 |
| QA_001 | 问题不能为空 |
| QA_002 | 未找到相关文档 |
| RATE_001 | 请求频率超出限制 |

### 错误响应格式

```json
{
  "code": 401,
  "message": "Token 已过期，请重新登录",
  "error": "AUTH_002",
  "data": null,
  "traceId": "a1b2c3d4e5f6",
  "timestamp": "2024-01-15T10:30:00"
}
```

---

## 🔒 限流说明

### 默认限流配置

| 接口类型 | 限制 | 窗口 |
|---------|------|------|
| 登录接口 | 10 次/分钟 | 按 IP |
| 问答接口 | 60 次/分钟 | 按用户 |
| 文档上传 | 20 次/小时 | 按用户 |
| 其他接口 | 100 次/分钟 | 按用户 |

### 限流响应

当触发限流时，响应头会包含：

```http
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1705312200
Retry-After: 45
```

响应体：
```json
{
  "code": 429,
  "message": "请求过于频繁，请稍后再试",
  "error": "RATE_001",
  "data": {
    "retryAfter": 45
  },
  "traceId": "a1b2c3d4e5f6"
}
```

---

## 📌 附录

### 请求示例（cURL）

**登录**：
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

**问答**：
```bash
curl -X POST http://localhost:8080/api/qa/ask \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "kbId": 1,
    "question": "如何配置数据库连接池?",
    "topK": 5
  }'
```

**上传文档**：
```bash
curl -X POST http://localhost:8080/api/knowledge-bases/1/documents \
  -H "Authorization: Bearer <token>" \
  -F "file=@document.pdf" \
  -F "title=技术文档"
```

