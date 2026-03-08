# RAG 项目 Docker 快速启动

## 🚀 一键启动

```bash
# 1. 启动所有服务
docker-compose up -d

# 2. 等待服务初始化（约30秒）
sleep 30

# 3. 验证所有服务
chmod +x verify_services.sh
./verify_services.sh
```

## 📦 启动后需要的额外配置

### 创建 MinIO Buckets
```bash
# 安装 mc 客户端
docker run --rm --network rag-net minio/mc alias set myminio http://minio:9000 minioadmin minioadmin

# 或者通过 Web 界面创建
# 访问 http://localhost:9001 -> Buckets -> Create Bucket
# 创建两个 bucket：
# - rag-documents (应用文档存储)
# - milvus-bucket (Milvus 向量数据)
```

## 🛠️ 常用命令

```bash
# 查看所有容器状态
docker-compose ps

# 查看实时日志
docker-compose logs -f

# 重启某个服务
docker-compose restart mysql

# 停止所有服务
docker-compose down

# 完全清理（删除数据）
docker-compose down -v && sudo rm -rf ./data
```

## ✅ 验证清单

- [ ] MySQL 可连接 (localhost:3307)
- [ ] Redis 可连接 (localhost:6380)
- [ ] MinIO 控制台可访问 (http://localhost:9001)
- [ ] Milvus 健康检查通过 (curl http://localhost:9091/healthz)
- [ ] Attu 可视化界面可访问 (http://localhost:3001)

## 🎯 接下来

1. 启动 Spring Boot 应用: `mvn spring-boot:run -pl rag-admin`
2. 访问 Swagger: http://localhost:8080/swagger-ui.html
3. 开始测试接口

详细文档请查看：[DOCKER_GUIDE.md](DOCKER_GUIDE.md)
