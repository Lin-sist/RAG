# RAG QA System — API 调用教程

## 一、环境准备

确保以下服务已启动：

| 服务 | 启动方式 | 验证 |
|------|----------|------|
| MySQL + Redis + Milvus | `docker compose up -d` | `docker compose ps` 全部 running |
| Spring Boot 应用 | 运行 `RagQaApplication.main()` | 浏览器打开 http://localhost:8080/swagger-ui.html |

---

## 二、Swagger UI 使用流程

### Step 1：登录获取 Token

打开 Swagger UI → 找到 **认证管理** → `POST /auth/login`，点击 **Try it out**，输入：

```json
{
  "username": "admin",
  "password": "admin123"
}
```

点击 **Execute**，响应中会返回：

```json
{
  "code": 200,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 7200
  }
}
```

> 复制 `accessToken` 的值（不需要复制引号）。

### Step 2：填入 Token（Authorize）

点击页面右上角的 🔓 **Authorize** 按钮，在弹窗中输入你刚复制的 token（**不需要**加 `Bearer ` 前缀），点击 **Authorize** → **Close**。

> 之后所有接口的请求头都会自动带上 `Authorization: Bearer <token>`。

### Step 3：开始调用接口

现在你可以自由调用所有需要认证的接口了。

---

## 三、核心业务调用顺序

RAG 系统的正常使用流程：

```
登录 → 创建知识库 → 上传文档 → 等待处理完成 → 提问 → 查看历史
```

### 3.1 创建知识库

**`POST /api/knowledge-bases`**

```json
{
  "name": "Java技术文档库",
  "description": "存放 Spring Boot、MyBatis 等技术文档",
  "isPublic": false
}
```

记住返回的 `id`（比如 `1`）。

### 3.2 上传文档

**`POST /api/knowledge-bases/{id}/documents`**

- 将 `{id}` 替换为上一步的知识库 ID
- Content-Type: `multipart/form-data`
- 参数 `file`：选择一个 PDF / Markdown / Word / 代码文件

> ⚠️ 文档上传后是**异步处理**的（解析 → 分块 → 向量化 → 存入 Milvus），会返回一个 `taskId`。

### 3.3 查看任务状态

**`GET /api/tasks/{taskId}`**

轮询查看文档处理进度，直到 `status` 变为 `COMPLETED`。

### 3.4 提问

**`POST /api/qa/ask`**

```json
{
  "kbId": 1,
  "question": "Spring Boot 的自动配置原理是什么？",
  "topK": 5
}
```

> `topK` 表示从向量库中检索最相似的前 5 个文档片段作为上下文。

### 3.5 流式问答（可选）

**`POST /api/qa/ask/stream`**

和上面参数一样，但返回 SSE（Server-Sent Events）流式响应，像 ChatGPT 一样逐字输出。

> ⚠️ Swagger UI 不能很好地展示 SSE 流式响应，建议用 curl 或 Postman 测试。

### 3.6 查看问答历史

**`GET /api/history?page=0&size=10`**

### 3.7 提交反馈

**`POST /api/history/{id}/feedback`**

```json
{
  "rating": 5,
  "comment": "回答很准确"
}
```

---

## 四、用 curl 调用（命令行方式）

如果你更喜欢用命令行：

```powershell
# 1. 登录
$response = Invoke-RestMethod -Uri "http://localhost:8080/auth/login" `
  -Method POST -ContentType "application/json" `
  -Body '{"username":"admin","password":"admin123"}'

$token = $response.data.accessToken
Write-Host "Token: $token"

# 2. 创建知识库
Invoke-RestMethod -Uri "http://localhost:8080/api/knowledge-bases" `
  -Method POST -ContentType "application/json" `
  -Headers @{Authorization = "Bearer $token"} `
  -Body '{"name":"测试知识库","description":"测试用","isPublic":false}'

# 3. 提问
Invoke-RestMethod -Uri "http://localhost:8080/api/qa/ask" `
  -Method POST -ContentType "application/json" `
  -Headers @{Authorization = "Bearer $token"} `
  -Body '{"kbId":1,"question":"你好","topK":5}'
```

---

## 五、完整 API 端点速查表

### 认证管理 `/auth`

| 方法 | 路径 | 说明 | 需要认证 |
|------|------|------|----------|
| POST | `/auth/login` | 登录 | ❌ |
| POST | `/auth/logout` | 登出 | ✅ |
| POST | `/auth/refresh` | 刷新 Token | ❌ |

### 知识库管理 `/api/knowledge-bases`

| 方法 | 路径 | 说明 | 需要认证 |
|------|------|------|----------|
| POST | `/api/knowledge-bases` | 创建知识库 | ✅ |
| GET | `/api/knowledge-bases` | 知识库列表 | ✅ |
| GET | `/api/knowledge-bases/{id}` | 知识库详情 | ✅ |
| PUT | `/api/knowledge-bases/{id}` | 更新知识库 | ✅ |
| DELETE | `/api/knowledge-bases/{id}` | 删除知识库 | ✅ |
| GET | `/api/knowledge-bases/{id}/statistics` | 知识库统计 | ✅ |
| POST | `/api/knowledge-bases/{id}/documents` | 上传文档 | ✅ |
| GET | `/api/knowledge-bases/{id}/documents` | 文档列表 | ✅ |
| DELETE | `/api/knowledge-bases/{kbId}/documents/{docId}` | 删除文档 | ✅ |

### 问答服务 `/api/qa`

| 方法 | 路径 | 说明 | 需要认证 |
|------|------|------|----------|
| POST | `/api/qa/ask` | 同步问答 | ✅ |
| POST | `/api/qa/ask/stream` | 流式问答 (SSE) | ✅ |
| GET | `/api/qa/ask` | 简单问答 (GET) | ✅ |

### 问答历史 `/api/history`

| 方法 | 路径 | 说明 | 需要认证 |
|------|------|------|----------|
| GET | `/api/history` | 历史列表 (分页) | ✅ |
| GET | `/api/history/{id}` | 历史详情 | ✅ |
| DELETE | `/api/history/{id}` | 删除历史 | ✅ |
| POST | `/api/history/{id}/feedback` | 提交反馈 | ✅ |
| GET | `/api/history/{id}/feedback` | 查看反馈 | ✅ |
| GET | `/api/history/feedback/my` | 我的反馈 | ✅ |

### 任务管理 `/api/tasks`

| 方法 | 路径 | 说明 | 需要认证 |
|------|------|------|----------|
| GET | `/api/tasks/{taskId}` | 任务状态 | ✅ |
| GET | `/api/tasks/{taskId}/result` | 任务结果 | ✅ |
| POST | `/api/tasks/{taskId}/cancel` | 取消任务 | ✅ |
| GET | `/api/tasks/{taskId}/exists` | 任务是否存在 | ✅ |
| GET | `/api/tasks/{taskId}/completed` | 任务是否完成 | ✅ |

---

## 六、常见问题

### Q: Token 过期了怎么办？

调用 `POST /auth/refresh`，Body 中传入 `refreshToken` 即可换取新的 `accessToken`。

### Q: 上传文档后状态一直是 PENDING？

检查 Milvus 是否正常运行（`docker compose ps`）和通义千问 API Key 是否有效。文档处理需要调用 Embedding 接口把文本转成向量。

### Q: 问答返回空结果？

确保：1) 知识库中有已处理完成的文档 2) Milvus 向量数据库正常 3) LLM API Key 有效

