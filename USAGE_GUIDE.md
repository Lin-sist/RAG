# RAG 项目使用指南

## 🎯 快速开始

**你看到的"未认证"错误是正常的！** 因为根路径需要登录。请按以下步骤操作：

---

## 📝 方式一：使用 Swagger UI（推荐）

### 1. 访问 Swagger UI
```
http://localhost:8080/swagger-ui.html
```

### 2. 登录获取 Token
1. 找到 **auth-controller** 分组
2. 展开 `POST /auth/login`
3. 点击 **Try it out**
4. 输入默认账号：
   ```json
   {
     "username": "admin",
     "password": "admin123"
   }
   ```
5. 点击 **Execute**
6. 从响应中复制 `accessToken`

### 3. 配置认证
1. 点击页面右上角的 **Authorize** 按钮
2. 在弹窗中输入：
   ```
   Bearer <你的accessToken>
   ```
   例如：
   ```
   Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWI...
   ```
3. 点击 **Authorize**

### 4. 开始测试接口
现在你可以测试所有需要认证的接口了！

---

## 📝 方式二：使用 curl 命令行

### 1. 登录获取 Token
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
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

### 2. 使用 Token 访问接口
```bash
# 设置 Token 变量
export TOKEN="your_access_token_here"

# 获取知识库列表
curl -X GET http://localhost:8080/api/knowledge-bases \
  -H "Authorization: Bearer $TOKEN"

# 创建知识库
curl -X POST http://localhost:8080/api/knowledge-bases \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Java 技术文档",
    "description": "Java 相关的技术文档知识库",
    "embeddingModel": "qwen",
    "vectorStore": "milvus"
  }'
```

---

## 📝 方式三：使用自动化测试脚本

我已经为你准备好了测试脚本：

```bash
chmod +x test_api.sh
./test_api.sh
```

**注意**：需要先安装 `jq`：
```bash
sudo apt install jq -y
```

---

## 🔐 默认账号信息

| 字段 | 值 |
|------|-----|
| 用户名 | admin |
| 密码 | admin123 |
| 角色 | ADMIN（超级管理员） |
| 权限 | 所有权限 |

---

## 🌐 可访问的 URL 汇总

| 服务 | URL | 说明 |
|------|-----|------|
| **Swagger UI** | http://localhost:8080/swagger-ui.html | 🔥 API 文档和测试界面 |
| **OpenAPI JSON** | http://localhost:8080/v3/api-docs | API 规范 JSON |
| **健康检查** | http://localhost:8080/actuator/health | 无需认证 |
| **MinIO 控制台** | http://localhost:9001 | minioadmin/minioadmin |
| **Attu (Milvus)** | http://localhost:3001 | Milvus 管理界面 |

---

## 🚀 完整的使用流程示例

### 场景：上传文档 → 建立知识库 → 进行问答

#### 1️⃣ 登录
```bash
# 获取 Token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.accessToken')

echo "Token: $TOKEN"
```

#### 2️⃣ 创建知识库
```bash
KB_ID=$(curl -s -X POST http://localhost:8080/api/knowledge-bases \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Spring Boot 文档",
    "description": "Spring Boot 技术文档",
    "embeddingModel": "qwen",
    "vectorStore": "milvus"
  }' | jq -r '.data.id')

echo "知识库 ID: $KB_ID"
```

#### 3️⃣ 上传文档
```bash
curl -X POST http://localhost:8080/api/knowledge-bases/$KB_ID/documents \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@./your-document.pdf" \
  -F "metadata={\"source\":\"官方文档\",\"version\":\"3.2.0\"}"
```

#### 4️⃣ 查询任务状态
从上传响应中获取 `taskId`，然后：
```bash
TASK_ID="your-task-id"
curl -X GET http://localhost:8080/api/tasks/$TASK_ID \
  -H "Authorization: Bearer $TOKEN"
```

#### 5️⃣ 进行问答
```bash
curl -X POST http://localhost:8080/api/qa/ask \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "kbId": '$KB_ID',
    "question": "如何配置 Spring Security？",
    "topK": 5,
    "enableCache": true
  }'
```

#### 6️⃣ 流式问答（Server-Sent Events）
```bash
curl -N -X POST http://localhost:8080/api/qa/ask/stream \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "kbId": '$KB_ID',
    "question": "SpringBoot 启动流程是什么？",
    "topK": 3
  }'
```

---

## 🔧 常见问题

### Q1: 为什么访问 http://localhost:8080 返回 401？

**答**：这是正常的！根路径需要认证。请访问：
- Swagger UI: http://localhost:8080/swagger-ui.html （**推荐**）
- 健康检查: http://localhost:8080/actuator/health （公开接口）

### Q2: 登录失败怎么办？

**答**：检查以下几点：
1. 数据库是否启动：`docker ps | grep mysql`
2. Flyway 迁移是否执行：查看应用启动日志
3. 用户名密码是否正确：`admin` / `admin123`

如果还是不行，手动检查数据库：
```bash
docker exec -it rag-mysql mysql -uroot -proot rag_qa
```
```sql
SELECT * FROM user;
```

### Q3: Token 过期了怎么办？

**答**：使用刷新接口：
```bash
curl -X POST http://localhost:8080/auth/refresh \
  -H "Authorization: Bearer $REFRESH_TOKEN"
```

### Q4: 如何查看详细日志？

**答**：应用启动日志在终端可以看到，每个请求都有 TraceId 方便追踪。

---

## 🎓 面试官可能会问的问题

### 1. **为什么访问根路径会返回 401？**
**回答**：因为 Spring Security 配置中，只有部分路径是公开的（如 `/auth/login`, `/swagger-ui/**`），其他路径默认需要认证。这是 `SecurityFilterChain` 的 `.anyRequest().authenticated()` 配置决定的。

### 2. **JWT Token 是怎么传递的？**
**回答**：通过 HTTP Header 的 `Authorization` 字段，格式为 `Bearer <token>`。在 `JwtAuthenticationFilter` 中会提取并验证这个 Token。

### 3. **如果 Token 被泄露怎么办？**
**回答**：
- 登出时 Token 会被加入黑名单（存储在 Redis）
- Token 有过期时间（默认 1 小时）
- 可以强制刷新 Token 使旧 Token 失效

---

## 💡 下一步

1. ✅ 确保 Docker 服务都正常运行
2. ✅ 使用 Swagger UI 测试登录
3. ✅ 创建知识库
4. ✅ 上传测试文档
5. ✅ 进行问答测试

**祝学习愉快！** 🎉
