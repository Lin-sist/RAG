#!/bin/bash

# RAG 项目 API 测试脚本

BASE_URL="http://localhost:8080"

echo "================================================"
echo "🧪 RAG 项目 API 测试"
echo "================================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. 测试登录接口
echo -e "${YELLOW}1️⃣ 测试登录接口${NC}"
echo "POST $BASE_URL/auth/login"
echo ""

LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }')

echo "响应："
echo "$LOGIN_RESPONSE" | jq '.'
echo ""

# 提取 Token
ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.accessToken // empty')

if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" == "null" ]; then
    echo -e "${RED}❌ 登录失败！请检查用户名密码${NC}"
    echo ""
    echo "💡 提示：如果数据库是新建的，可能还没有默认用户。"
    echo "   请先通过 Swagger UI 创建用户或查看数据库迁移脚本。"
    exit 1
fi

echo -e "${GREEN}✅ 登录成功！${NC}"
echo "Access Token: ${ACCESS_TOKEN:0:50}..."
echo ""

# 2. 测试认证接口（获取知识库列表）
echo -e "${YELLOW}2️⃣ 测试获取知识库列表（需要认证）${NC}"
echo "GET $BASE_URL/api/knowledge-bases"
echo ""

KB_RESPONSE=$(curl -s -X GET "$BASE_URL/api/knowledge-bases" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

echo "响应："
echo "$KB_RESPONSE" | jq '.'
echo ""

# 3. 测试健康检查接口（无需认证）
echo -e "${YELLOW}3️⃣ 测试健康检查接口（公开接口）${NC}"
echo "GET $BASE_URL/actuator/health"
echo ""

HEALTH_RESPONSE=$(curl -s -X GET "$BASE_URL/actuator/health")
echo "响应："
echo "$HEALTH_RESPONSE" | jq '.'
echo ""

# 4. 访问 Swagger UI
echo "================================================"
echo "📚 接口文档访问"
echo "================================================"
echo ""
echo -e "${GREEN}Swagger UI:${NC}   http://localhost:8080/swagger-ui.html"
echo -e "${GREEN}OpenAPI JSON:${NC} http://localhost:8080/v3/api-docs"
echo ""

echo "================================================"
echo "💡 使用提示"
echo "================================================"
echo ""
echo "1. 访问 Swagger UI 可视化测试所有接口"
echo "2. 先调用 /auth/login 获取 Token"
echo "3. 在 Swagger UI 右上角点击 'Authorize'，输入："
echo "   Bearer <your_access_token>"
echo "4. 然后就可以测试需要认证的接口了"
echo ""
