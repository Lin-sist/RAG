# RAG 项目快速入门 - 问题解决版

## 🔥 当前状态

你的应用**已经启动**，但是遇到了一些配置问题。让我们一步步解决。

---

## ✅ 第一步：验证数据库中的用户

```bash
docker exec -it rag-mysql mysql -uroot -proot rag_qa -e "SELECT username, email, enabled FROM user;"
```

**预期结果**：应该看到 `admin` 用户

---

## ✅ 第二步：访问 Swagger UI

### 方法 1：浏览器访问
打开浏览器，输入以下任意一个地址：

- **推荐**: http://localhost:8080/swagger-ui/index.html
- **或者**: http://localhost:8080/v3/api-docs

如果看到 JSON 数据或者 Swagger 页面，说明成功！

### 方法 2：命令行测试
```bash
# 测试 OpenAPI 文档
curl http://localhost:8080/v3/api-docs

# 应该返回 JSON 格式的 API 文档
```

---

## ✅ 第三步：测试登录接口

### 使用 curl 命令
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }' \
  | python3 -m json.tool
```

**预期成功响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOi...(很长的字符串)",
    "refreshToken": "eyJhbGciOi...(很长的字符串)",
    "expiresIn": 3600
  }
}
```

**如果返回错误**：查看下方的[常见错误](#常见错误)部分

---

## 📖 Swagger UI 使用说明

### 什么是 "展开 POST /auth/login" ？

Swagger UI 是一个**网页界面**，用来可视化测试 API。操作步骤：

1. **在浏览器打开**: http://localhost:8080/swagger-ui/index.html

2. **找到认证接口**:
   - 页面上会看到不同的分组（Tags）
   - 找到 **"认证管理"** 或 **"auth-controller"** 这一组
   - 里面有 `POST /auth/login` 这一行

3. **点击展开**:
   - 点击 `POST /auth/login` 这一行
   - 会展开显示详细信息

4. **点击 "Try it out"** 按钮（右上角）

5. **填写参数**:
   ```json
   {
     "username": "admin",
     "password": "admin123"
   }
   ```

6. **点击 "Execute"** 按钮

7. **查看响应**:
   - 向下滚动可以看到"Responses"部分
   - 如果成功，会看到 `accessToken`

8. **配置认证**（用于测试其他接口）:
   - 复制 `accessToken` 的值
   - 点击页面右上角的 **"Authorize"** 🔓 按钮
   - 在弹窗中输入: `Bearer <你的token>`
   - 点击 "Authorize" 然后 "Close"

9. **现在可以测试其他需要登录的接口了！**

---

## 🔧 常见错误

### 错误 1: 502 Bad Gateway
**原因**: 应用内部错误，通常是配置问题

**解决方案**: 检查应用启动日志
```bash
# 查看最近的错误日志
docker logs $(docker ps -q --filter "name=rag-mysql") --tail 50
```

### 错误 2: Connection refused
**原因**: 应用没有启动

**解决方案**: 重新启动
```bash
cd /home/lin/Projects/My_Java/RAG/rag-admin
mvn spring-boot:run
```

### 错误 3: 端口已被占用
**原因**: 8080 端口被其他程序使用

**解决方案**: 杀掉旧进程
```bash
# 查找占用端口的进程
lsof -i :8080 | grep LISTEN

# 杀掉进程（替换 PID 为实际进程号）
kill -9 <PID>

# 重新启动
cd /home/lin/Projects/My_Java/RAG/rag-admin
mvn spring-boot:run
```

### 错误 4: 用户名或密码错误
**原因**: 数据库中没有用户或密码不匹配

**解决方案**: 检查并重建数据
```bash
# 1. 连接数据库
docker exec -it rag-mysql mysql -uroot -proot rag_qa

# 2. 检查用户
SELECT * FROM user;

# 3. 如果没有数据，检查 Flyway 迁移
SELECT * FROM flyway_schema_history;

# 4. 退出
exit;
```

---

## 🎯 完整测试流程示例

```bash
# 1. 确保所有 Docker 容器运行正常
docker-compose ps

# 2. 确保应用正在运行
ps aux | grep RagQaApplication

# 3. 测试 API 文档
curl http://localhost:8080/v3/api-docs | head -20

# 4. 测试登录
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | grep -o '"accessToken":"[^"]*"' \
  | cut -d'"' -f4)

# 5. 显示 Token
echo "Token: $TOKEN"

# 6. 使用 Token 访问受保护的接口
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/knowledge-bases
```

---

## 📞 仍然有问题？

### 步骤 1: 检查应用日志
```bash
# 如果通过 IDE 启动，查看控制台输出
# 如果通过 Maven 启动，查看终端输出

# 关键词搜索:
ERROR
Exception  
Failed to
```

### 步骤 2: 检查数据库连接
```bash
docker exec -it rag-mysql mysql -uroot -proot -e "SHOW DATABASES;"
```

### 步骤 3: 重新启动所有服务
```bash
# 1. 停止应用（Ctrl+C）

# 2. 重启 Docker 服务
docker-compose restart

# 3. 等待30秒
sleep 30

# 4. 重新启动应用
cd /home/lin/Projects/My_Java/RAG/rag-admin
mvn spring-boot:run
```

---

## 🎓 面试官会问的问题

### Q: 为什么需要两个 Token (accessToken 和 refreshToken)？

**A**: 
- **accessToken**: 短期有效（1小时），用于日常 API 调用
- **refreshToken**: 长期有效（7天），用于获取新的 accessToken
- **好处**: 
  - 即使 accessToken 泄露，危害期限短
  - 不需要频繁输入密码
  - refreshToken 只在 /auth/refresh 接口使用，更安全

### Q: Swagger UI 为什么不需要登录就能访问？

**A**: 
- 在 `SecurityConfig` 中，`/swagger-ui/**` 被配置为 `permitAll()`
- 这是为了**开发便利性**，生产环境建议添加认证限制
- 代码位置: `SecurityConfig.java` 的 `authorizeHttpRequests` 方法

---

**祝学习愉快！🎉**
