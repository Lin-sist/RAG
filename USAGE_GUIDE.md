# RAG 项目功能测试清单（可勾选）

> 目标：按模块快速验收「能不能用、是否符合预期、出问题如何定位」。
> 适用环境：Ubuntu/Linux。

---

## 0. 测试前准备（必须通过）

- [ ] 安装基础工具

```bash
sudo apt update
sudo apt install -y curl jq
```

- [ ] 核心服务状态检查

```bash
docker compose ps
curl -s http://localhost:8080/actuator/health | jq
```

预期：
- `docker compose ps` 中 MySQL/Redis/Milvus/MinIO 为 `running`。
- 健康检查返回 `{"status":"UP"}`。

- [ ] Swagger 可访问

访问：`http://localhost:8080/swagger-ui.html`

---

## 1. 测试变量初始化（后续命令复用）

- [ ] 登录并提取 Token

```bash
BASE_URL="http://localhost:8080"

TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.accessToken')

REFRESH_TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.refreshToken')

echo "TOKEN长度: ${#TOKEN}"
echo "REFRESH_TOKEN长度: ${#REFRESH_TOKEN}"
```

预期：
- `TOKEN长度` 和 `REFRESH_TOKEN长度` 大于 20。

---

## 2. Admin / 基础管理功能（优先验收）

### 2.1 知识库管理 CRUD

- [ ] 创建知识库

```bash
KB_NAME="kb-test-$(date +%s)"

KB_ID=$(curl -s -X POST "$BASE_URL/api/knowledge-bases" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"$KB_NAME\",\"description\":\"功能测试知识库\",\"isPublic\":false}" \
  | jq -r '.data.id')

echo "KB_ID=$KB_ID"
```

预期：`KB_ID` 为数字且不为空。

- [ ] 查询知识库列表

```bash
curl -s -X GET "$BASE_URL/api/knowledge-bases" \
  -H "Authorization: Bearer $TOKEN" | jq '.code, .data | length'
```

预期：返回 `code = 200`，并能看到列表长度。

- [ ] 查询知识库详情

```bash
curl -s -X GET "$BASE_URL/api/knowledge-bases/$KB_ID" \
  -H "Authorization: Bearer $TOKEN" | jq '.code, .data.id, .data.name'
```

预期：`data.id == KB_ID`。

- [ ] 更新知识库

```bash
curl -s -X PUT "$BASE_URL/api/knowledge-bases/$KB_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"kb-updated","description":"更新后描述","isPublic":false}' \
  | jq '.code, .message'
```

预期：`code = 200`。

### 2.2 文档上传与任务追踪

- [ ] 上传文档并获取任务 ID

```bash
UPLOAD_RES=$(curl -s -X POST "$BASE_URL/api/knowledge-bases/$KB_ID/documents" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@./test-data/springboot-basics.md")

echo "$UPLOAD_RES" | jq

TASK_ID=$(echo "$UPLOAD_RES" | jq -r '.data.taskId // .data.id // empty')
echo "TASK_ID=$TASK_ID"
```

预期：返回成功，且能拿到 `TASK_ID`。

- [ ] 轮询任务状态直到完成

```bash
for i in {1..20}; do
  STATUS=$(curl -s -X GET "$BASE_URL/api/tasks/$TASK_ID" \
    -H "Authorization: Bearer $TOKEN" | jq -r '.data.status // .data.taskStatus // "UNKNOWN"')
  echo "第${i}次轮询: $STATUS"
  [[ "$STATUS" == "COMPLETED" ]] && break
  sleep 3
done
```

预期：状态最终为 `COMPLETED`。

- [ ] 查看文档列表

```bash
curl -s -X GET "$BASE_URL/api/knowledge-bases/$KB_ID/documents" \
  -H "Authorization: Bearer $TOKEN" | jq '.code, (.data | length)'
```

预期：列表长度大于 0。

---

## 3. Core / 问答主链路

### 3.1 同步问答

- [ ] 调用同步问答接口

```bash
ASK_RES=$(curl -s -X POST "$BASE_URL/api/qa/ask" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"kbId\":$KB_ID,\"question\":\"Spring Boot 自动配置是什么\",\"topK\":5}")

echo "$ASK_RES" | jq '.code, .message, .data.answer // .data.content'
```

预期：`code = 200`，返回非空答案字段。

### 3.2 流式问答（SSE）

- [ ] 调用流式问答

```bash
curl -N -X POST "$BASE_URL/api/qa/ask/stream" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"kbId\":$KB_ID,\"question\":\"总结这份知识库重点\",\"topK\":3}"
```

预期：终端持续输出 `data:` 事件流，不是一次性 JSON。

---

## 4. Auth / 安全功能

### 4.1 未认证拦截

- [ ] 无 Token 访问受保护接口

```bash
curl -i -s -X GET "$BASE_URL/api/knowledge-bases" | head -n 1
```

预期：HTTP 状态码 `401`。

### 4.2 Token 刷新

- [ ] 刷新 Access Token

```bash
curl -s -X POST "$BASE_URL/auth/refresh" \
  -H "Authorization: Bearer $REFRESH_TOKEN" | jq '.code, .data.accessToken'
```

预期：`code = 200`，返回新的 `accessToken`。

### 4.3 登出与失效验证

- [ ] 调用登出

```bash
curl -s -X POST "$BASE_URL/auth/logout" \
  -H "Authorization: Bearer $TOKEN" | jq '.code, .message'
```

- [ ] 使用旧 Token 再访问

```bash
curl -i -s -X GET "$BASE_URL/api/knowledge-bases" \
  -H "Authorization: Bearer $TOKEN" | head -n 1
```

预期：旧 Token 访问失败（通常 401）。

---

## 5. 历史与反馈功能

- [ ] 查询历史列表

```bash
HISTORY_RES=$(curl -s -X GET "$BASE_URL/api/history?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN")
echo "$HISTORY_RES" | jq '.code, (.data.records | length // .data | length)'
```

预期：`code = 200`。

- [ ] 提交反馈

```bash
HISTORY_ID=$(echo "$HISTORY_RES" | jq -r '.data.records[0].id // .data[0].id // empty')
echo "HISTORY_ID=$HISTORY_ID"

if [ -n "$HISTORY_ID" ]; then
  curl -s -X POST "$BASE_URL/api/history/$HISTORY_ID/feedback" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"rating":5,"comment":"功能测试通过"}' | jq '.code, .message'
else
  echo "暂无历史记录，先执行一次 /api/qa/ask 再测试反馈接口"
fi
```

预期：有历史时反馈提交成功。

---

## 6. 清理测试数据（建议执行）

- [ ] 删除测试知识库

```bash
curl -s -X DELETE "$BASE_URL/api/knowledge-bases/$KB_ID" \
  -H "Authorization: Bearer $TOKEN" | jq '.code, .message'
```

预期：删除成功。

---

## 7. 一次性快速回归（已有脚本）

- [ ] 执行基础 API 冒烟

```bash
chmod +x ./test_api.sh
./test_api.sh
```

说明：该脚本覆盖登录、鉴权接口、健康检查，适合快速确认服务是否可用。

---

## 8. 失败定位速查（Linux）

1. 应用日志排查

```bash
# 如果你是用 mvn spring-boot:run 启动，直接看当前终端日志
# 若后端在后台运行，可先定位 Java 进程
ps -ef | grep -E "rag-admin|RagQaApplication|spring-boot" | grep -v grep
```

2. 端口冲突排查

```bash
ss -lntp | grep -E ':8080|:3306|:6379|:19530|:9001'
```

3. Docker 服务排查

```bash
docker compose ps
docker compose logs --tail=100 mysql redis milvus minio
```

4. 数据库用户排查

```bash
docker exec -it rag-mysql mysql -uroot -proot rag_qa -e "SELECT id, username, enabled FROM user;"
```

---

## 9. 验收结论模板（复制填写）

```text
[ ] 环境检查通过
[ ] 知识库 CRUD 正常
[ ] 文档上传与任务处理正常
[ ] 同步问答正常
[ ] 流式问答正常
[ ] 认证/刷新/登出正常
[ ] 历史与反馈正常
[ ] 清理数据完成

结论：
- 阻塞问题：
- 次要问题：
- 是否可进入下一阶段联调：是 / 否
```
