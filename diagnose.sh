#!/bin/bash

echo "================================================"
echo "🔧 RAG 项目问题诊断与修复工具"
echo "================================================"
echo ""

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 1. 检查 Docker 服务
echo "1️⃣ 检查 Docker 服务状态..."
if ! docker ps | grep -q rag-mysql; then
    echo -e "${RED}❌ MySQL 容器未运行${NC}"
    echo "   执行: docker-compose up -d mysql"
    exit 1
fi
echo -e "${GREEN}✅ Docker 服务正常${NC}"
echo ""

# 2. 检查数据库用户
echo "2️⃣ 检查数据库用户..."
USER_COUNT=$(docker exec rag-mysql mysql -uroot -proot rag_qa -e "SELECT COUNT(*) as c FROM user;" 2>/dev/null | tail -1)
if [ "$USER_COUNT" = "0" ] || [ -z "$USER_COUNT" ]; then
    echo -e "${RED}❌ 数据库中没有用户数据${NC}"
    echo "   可能是 Flyway 迁移未执行"
else
    echo -e "${GREEN}✅ 数据库用户存在 (count: $USER_COUNT)${NC}"
fi
echo ""

# 3. 检查应用进程
echo "3️⃣ 检查应用进程..."
if ps aux | grep -q "[R]agQaApplication"; then
    PID=$(ps aux | grep "[R]agQaApplication" | awk '{print $2}' | head -1)
    echo -e "${YELLOW}⚠️  应用正在运行 (PID: $PID)${NC}"
    echo -n "   是否重启应用？ (y/n): "
    read answer
    if [ "$answer" = "y" ]; then
        echo "   正在停止应用..."
        kill -9 $PID 2>/dev/null
        sleep 2
        echo "   请手动启动: cd rag-admin && mvn spring-boot:run"
    fi
else
    echo -e "${RED}❌ 应用未运行${NC}"
    echo "   请启动: cd rag-admin && mvn spring-boot:run"
fi
echo ""

# 4. 检查端口占用
echo "4️⃣ 检查端口8080..."
if lsof -i :8080 | grep -q LISTEN; then
    PORT_PID=$(lsof -i :8080 | grep LISTEN | awk '{print $2}' | head -1)
    echo -e "${YELLOW}⚠️  端口8080被占用 (PID: $PORT_PID)${NC}"
else
    echo -e "${GREEN}✅ 端口8080空闲${NC}"
fi
echo ""

# 5. 测试接口
echo "5️⃣ 测试接口..."
echo "   测试 Swagger UI..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/swagger-ui/index.html)
if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✅ Swagger UI 可访问${NC}"
    echo "   浏览器访问: http://localhost:8080/swagger-ui/index.html"
elif [ "$HTTP_CODE" = "000" ]; then
    echo -e "${RED}❌ 无法连接到应用${NC}"
else
    echo -e "${YELLOW}⚠️  HTTP $HTTP_CODE${NC}"
fi
echo ""

echo "   测试登录接口..."
RESPONSE=$(curl -s -X POST http://localhost:8080/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin123"}')

if echo "$RESPONSE" | grep -q "accessToken"; then
    echo -e "${GREEN}✅ 登录接口正常${NC}"
    TOKEN=$(echo "$RESPONSE" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
    echo "   Token: ${TOKEN:0:30}..."
elif echo "$RESPONSE" | grep -q "code"; then
    CODE=$(echo "$RESPONSE" | grep -o '"code":[0-9]*' | cut -d':' -f2)
    MSG=$(echo "$RESPONSE" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)
    echo -e "${RED}❌ 登录失败${NC}"
    echo "   Code: $CODE"
    echo "   Message: $MSG"
else
    echo -e "${RED}❌ 接口无响应或返回异常${NC}"
    echo "   Response: $RESPONSE"
fi
echo ""

# 6. 总结
echo "================================================"
echo "📊 诊断完成"
echo "================================================"
echo ""
echo "💡 建议："
echo "1. 确保 Docker 服务都在运行: docker-compose ps"
echo "2. 重启应用: cd rag-admin && mvn spring-boot:run"
echo "3. 访问 Swagger UI: http://localhost:8080/swagger-ui/index.html"
echo "4. 查看本文件了解详细步骤: QUICK_START.md"
echo ""
