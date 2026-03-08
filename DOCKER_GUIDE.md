# Docker 环境部署指南

## 📋 目录结构

启动后会在项目根目录自动创建以下数据目录：
```
RAG/
├── docker-compose.yml
└── data/
    ├── mysql/      # MySQL 数据文件
    ├── redis/      # Redis 持久化数据
    ├── minio/      # MinIO 对象存储
    ├── etcd/       # etcd 元数据
    └── milvus/     # Milvus 向量数据
```

## 🚀 启动服务

### 1. 启动所有服务
```bash
# 在项目根目录执行
docker-compose up -d

# 查看所有容器状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 2. 启动特定服务
```bash
# 只启动 MySQL 和 Redis
docker-compose up -d mysql redis

# 只启动 Milvus 相关服务
docker-compose up -d etcd minio milvus
```

### 3. 停止服务
```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷（危险操作！）
docker-compose down -v
```

## ✅ 验证服务状态

### 1️⃣ MySQL 验证
```bash
# 方法1：使用 docker exec 连接
docker exec -it rag-mysql mysql -uroot -proot

# 执行 SQL 验证
SHOW DATABASES;
USE rag_qa;
SHOW TABLES;

# 方法2：使用主机 MySQL 客户端（如果已安装）
mysql -h 127.0.0.1 -P 3307 -uroot -proot
```

**预期结果**：能成功连接并看到 `rag_qa` 数据库

---

### 2️⃣ Redis 验证
```bash
# 使用 docker exec 连接
docker exec -it rag-redis redis-cli -a root

# 执行命令验证
PING
# 应返回 PONG

SET test "hello"
GET test
# 应返回 "hello"
```

**预期结果**：PING 返回 PONG

---

### 3️⃣ MinIO 验证

**Web 控制台验证**：
1. 浏览器访问：http://localhost:9001
2. 登录账号：`minioadmin` / `minioadmin`
3. 应该能看到 MinIO 管理界面

**创建应用需要的 Bucket**：
```bash
# 安装 mc 客户端（MinIO Client）
wget https://dl.min.io/client/mc/release/linux-amd64/mc
chmod +x mc
sudo mv mc /usr/local/bin/

# 配置别名
mc alias set local http://localhost:9000 minioadmin minioadmin

# 创建 bucket
mc mb local/rag-documents      # 存放上传的文档
mc mb local/milvus-bucket      # Milvus 使用的 bucket

# 查看 bucket 列表
mc ls local
```

**预期结果**：能访问控制台，能创建 bucket

---

### 4️⃣ Milvus 验证

**方法1：使用 Attu 可视化工具**
1. 浏览器访问：http://localhost:3001
2. 连接地址填写：`milvus`，端口：`19530`
3. 应该能看到 Milvus 管理界面

**方法2：使用 Python 测试**
```bash
# 安装 pymilvus
pip install pymilvus

# 创建测试脚本 test_milvus.py
python3 << 'EOF'
from pymilvus import connections, utility

# 连接 Milvus
connections.connect(
    alias="default",
    host='localhost',
    port='19530'
)

# 检查连接
print(f"Milvus version: {utility.get_server_version()}")
print("✅ Milvus 连接成功！")

# 列出所有 collection
collections = utility.list_collections()
print(f"当前 Collections: {collections}")
EOF
```

**方法3：使用 curl 检查健康状态**
```bash
# 检查 Milvus 健康状态
curl http://localhost:9091/healthz

# 应返回空响应且 HTTP 状态码为 200
```

**预期结果**：能成功连接并返回 Milvus 版本号

---

## 📊 服务端口总览

| 服务 | 端口映射 | 访问地址 | 账号密码 |
|------|---------|---------|---------|
| MySQL | 3307 -> 3306 | localhost:3307 | root / root |
| Redis | 6380 -> 6379 | localhost:6380 | password: root |
| MinIO API | 9000 -> 9000 | http://localhost:9000 | minioadmin / minioadmin |
| MinIO Console | 9001 -> 9001 | http://localhost:9001 | minioadmin / minioadmin |
| Milvus gRPC | 19530 -> 19530 | localhost:19530 | 无需认证 |
| Attu (Milvus GUI) | 3001 -> 3000 | http://localhost:3001 | - |

---

## 🔧 常见问题

### Q1: 容器启动失败？
```bash
# 查看具体错误日志
docker-compose logs <service-name>

# 例如查看 Milvus 日志
docker-compose logs milvus
```

### Q2: 端口被占用？
```bash
# 检查端口占用
sudo lsof -i :3307
sudo lsof -i :6380

# 修改 docker-compose.yml 中的端口映射
```

### Q3: 数据持久化在哪里？
- 所有数据存储在 `./data/` 目录下
- 删除容器不会丢失数据，除非执行 `docker-compose down -v`

### Q4: Milvus 连接 MinIO 失败？
```bash
# 确保 MinIO 先启动
docker-compose up -d minio
sleep 10

# 再启动 Milvus
docker-compose up -d milvus

# 查看 Milvus 日志确认连接状态
docker-compose logs milvus | grep minio
```

### Q5: 如何重置所有数据？
```bash
# ⚠️ 危险操作：删除所有容器和数据
docker-compose down -v
sudo rm -rf ./data

# 重新启动
docker-compose up -d
```

---

## 🎯 快速验证脚本

创建 `verify_all.sh` 一键验证所有服务：

```bash
#!/bin/bash

echo "🔍 验证 Docker 服务状态..."

# 1. MySQL
echo -n "MySQL: "
docker exec rag-mysql mysqladmin ping -h localhost -uroot -proot &> /dev/null && echo "✅" || echo "❌"

# 2. Redis
echo -n "Redis: "
docker exec rag-redis redis-cli -a root PING &> /dev/null && echo "✅" || echo "❌"

# 3. MinIO
echo -n "MinIO: "
curl -s http://localhost:9000/minio/health/live &> /dev/null && echo "✅" || echo "❌"

# 4. Milvus
echo -n "Milvus: "
curl -s http://localhost:9091/healthz &> /dev/null && echo "✅" || echo "❌"

echo "✅ 验证完成！"
```

使用方法：
```bash
chmod +x verify_all.sh
./verify_all.sh
```

---

## 📝 下一步

服务启动成功后：
1. ✅ 确保 `application.yml` 配置正确（已自动调整）
2. ✅ 启动 Spring Boot 应用
3. ✅ 访问 Swagger UI：http://localhost:8080/swagger-ui.html
4. ✅ 开始测试接口

祝开发顺利！🎉
