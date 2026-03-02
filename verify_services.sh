#!/bin/bash

# RAG 项目 Docker 服务验证脚本

echo "================================================"
echo "🔍 RAG 项目 Docker 服务状态验证"
echo "================================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 1. 检查容器运行状态
echo "📦 检查容器运行状态..."
docker-compose ps
echo ""

# 2. MySQL 验证
echo -n "🗄️  MySQL (3307): "
if docker exec rag-mysql mysqladmin ping -h localhost -uroot -proot &> /dev/null; then
    echo -e "${GREEN}✅ 运行正常${NC}"
else
    echo -e "${RED}❌ 连接失败${NC}"
fi

# 3. Redis 验证
echo -n "💾 Redis (6380): "
if docker exec rag-redis redis-cli -a root PING 2>/dev/null | grep -q PONG; then
    echo -e "${GREEN}✅ 运行正常${NC}"
else
    echo -e "${RED}❌ 连接失败${NC}"
fi

# 4. MinIO 验证
echo -n "📦 MinIO (9000/9001): "
if curl -s http://localhost:9000/minio/health/live &> /dev/null; then
    echo -e "${GREEN}✅ 运行正常${NC}"
    echo "   🌐 控制台: http://localhost:9001 (minioadmin/minioadmin)"
else
    echo -e "${RED}❌ 连接失败${NC}"
fi

# 5. etcd 验证
echo -n "🔧 etcd (2379): "
if docker exec rag-etcd etcdctl endpoint health 2>/dev/null | grep -q "is healthy"; then
    echo -e "${GREEN}✅ 运行正常${NC}"
else
    echo -e "${RED}❌ 连接失败${NC}"
fi

# 6. Milvus 验证
echo -n "🧠 Milvus (19530): "
if curl -s http://localhost:9091/healthz &> /dev/null; then
    echo -e "${GREEN}✅ 运行正常${NC}"
else
    echo -e "${RED}❌ 连接失败${NC}"
fi

# 7. Attu 验证
echo -n "🖥️  Attu 管理界面 (3001): "
if curl -s http://localhost:3001 &> /dev/null; then
    echo -e "${GREEN}✅ 运行正常${NC}"
    echo "   🌐 访问地址: http://localhost:3001"
else
    echo -e "${RED}❌ 连接失败${NC}"
fi

echo ""
echo "================================================"
echo "📊 端口映射总览"
echo "================================================"
echo "MySQL:        localhost:3307 (root/root)"
echo "Redis:        localhost:6380 (password: root)"
echo "MinIO API:    http://localhost:9000"
echo "MinIO Web:    http://localhost:9001"
echo "Milvus:       localhost:19530"
echo "Attu:         http://localhost:3001"
echo ""
echo "💡 提示: 如需查看详细日志，执行: docker-compose logs -f [service-name]"
echo ""
