#!/bin/bash

# MinIO Bucket 初始化脚本

echo "================================================"
echo "📦 MinIO Bucket 初始化"
echo "================================================"
echo ""

# 方式一：使用 Docker 网络访问 MinIO
echo "🔧 方法 1：通过 Docker 网络创建 Bucket"
echo ""

# 找到正确的网络名称
NETWORK_NAME=$(docker network ls | grep rag | awk '{print $2}' | head -1)

if [ -z "$NETWORK_NAME" ]; then
    echo "❌ 未找到 rag 相关的 Docker 网络"
    echo "请确保 Docker Compose 已启动：docker-compose up -d"
    exit 1
fi

echo "✅ 找到网络: $NETWORK_NAME"
echo ""

# 创建 buckets
echo "创建 bucket: rag-documents"
docker run --rm --network $NETWORK_NAME minio/mc \
  alias set myminio http://minio:9000 minioadmin minioadmin

docker run --rm --network $NETWORK_NAME minio/mc \
  mb myminio/rag-documents 2>/dev/null || echo "  bucket 已存在"

echo ""
echo "创建 bucket: milvus-bucket"
docker run --rm --network $NETWORK_NAME minio/mc \
  mb myminio/milvus-bucket 2>/dev/null || echo "  bucket 已存在"

echo ""
echo "查看所有 buckets:"
docker run --rm --network $NETWORK_NAME minio/mc \
  ls myminio

echo ""
echo "================================================"
echo "🔧 方法 2：通过 Web 界面创建（更简单）"
echo "================================================"
echo ""
echo "1. 访问: http://localhost:9001"
echo "2. 登录: minioadmin / minioadmin"
echo "3. 左侧菜单点击 'Buckets'"
echo "4. 点击 'Create Bucket' 按钮"
echo "5. 创建以下两个 buckets:"
echo "   - rag-documents"
echo "   - milvus-bucket"
echo ""

echo "✅ 初始化完成！"
echo ""
