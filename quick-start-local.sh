#!/bin/bash

echo "======================================"
echo "  企业微信本地测试快速启动脚本"
echo "======================================"

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 1. 检查Redis
echo -e "\n${YELLOW}1. 检查Redis...${NC}"
if redis-cli ping > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Redis 运行正常${NC}"
else
    echo -e "${RED}✗ Redis 未运行${NC}"
    echo "启动Redis："
    echo "  macOS: brew services start redis"
    echo "  或运行: redis-server"
    echo "  或Docker: docker run -d -p 6379:6379 redis:5.0"
    exit 1
fi

# 2. 检查Java
echo -e "\n${YELLOW}2. 检查Java...${NC}"
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
    echo -e "${GREEN}✓ Java 版本: $JAVA_VERSION${NC}"
else
    echo -e "${RED}✗ Java 未安装${NC}"
    exit 1
fi

# 3. 检查配置文件
echo -e "\n${YELLOW}3. 检查配置文件...${NC}"
if [ -f "src/main/resources/application.yml" ]; then
    echo -e "${GREEN}✓ 配置文件存在${NC}"
    
    # 检查是否配置了企业ID
    if grep -q "your_corp_id" src/main/resources/application.yml; then
        echo -e "${YELLOW}⚠ 警告: 请先配置企业微信信息！${NC}"
        echo "编辑文件: src/main/resources/application.yml"
        echo "配置: corp-id, contact-secret, token, encoding-aes-key"
        read -p "是否已配置完成？(y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
else
    echo -e "${RED}✗ 配置文件不存在${NC}"
    exit 1
fi

# 4. 检查是否已编译
echo -e "\n${YELLOW}4. 检查项目状态...${NC}"
if [ ! -d "target/classes" ]; then
    echo -e "${YELLOW}项目未编译，开始编译...${NC}"
    if command -v mvn &> /dev/null; then
        mvn clean compile -DskipTests
    else
        echo -e "${RED}✗ Maven 未安装，请使用IDEA打开项目${NC}"
        exit 1
    fi
fi

# 5. 启动提示
echo -e "\n${GREEN}======================================"
echo "  准备就绪！"
echo "======================================${NC}"
echo ""
echo "接下来的步骤："
echo ""
echo "${YELLOW}1. 启动Spring Boot应用：${NC}"
echo "   方式1（推荐）: 用IDEA打开项目，运行 WechatWorkCallbackApplication"
echo "   方式2: mvn spring-boot:run"
echo "   方式3: java -jar target/wechat-work-callback-1.0.0.jar"
echo ""
echo "${YELLOW}2. 启动ngrok（新终端窗口）：${NC}"
echo "   ngrok http 8080"
echo "   然后复制 https://xxxx.ngrok.io 这个URL"
echo ""
echo "${YELLOW}3. 更新配置：${NC}"
echo "   在application.yml中更新:"
echo "   h5-base-url: https://xxxx.ngrok.io/h5/oauth.html"
echo "   然后重启应用"
echo ""
echo "${YELLOW}4. 配置企业微信：${NC}"
echo "   URL: https://xxxx.ngrok.io/api/wechat/callback"
echo "   查看详细步骤: cat LOCAL-TEST-GUIDE.md"
echo ""
echo "${GREEN}启动后访问: http://localhost:8080/api/wechat/health${NC}"
echo "应该返回: OK"
echo ""
echo "======================================"

# 6. 询问是否立即启动
read -p "是否立即启动Spring Boot？(y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if command -v mvn &> /dev/null; then
        echo -e "\n${GREEN}启动Spring Boot...${NC}"
        mvn spring-boot:run
    else
        echo -e "${YELLOW}请使用IDEA启动项目${NC}"
    fi
fi

