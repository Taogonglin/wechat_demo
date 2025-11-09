#!/bin/bash

echo "========================================"
echo "  启动企业微信服务 + ngrok 内网穿透"
echo "========================================"

# 设置颜色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

cd /Users/taogonglin/projects/wechat_demo

# 1. 启动 Spring Boot
echo ""
echo "${YELLOW}1. 启动 Spring Boot 应用...${NC}"
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
export PATH=$JAVA_HOME/bin:$PATH

# 停止旧进程
ps aux | grep "WechatWorkCallbackApplication" | grep -v grep | awk '{print $2}' | xargs kill -9 2>/dev/null
sleep 1

# 启动应用
mvn spring-boot:run > /tmp/wechat-app.log 2>&1 &
SPRING_PID=$!
echo "Spring Boot 进程ID: $SPRING_PID"

# 等待启动
echo "等待服务启动..."
for i in {1..15}; do
    if curl -s http://localhost:8080/api/wechat/health > /dev/null 2>&1; then
        echo "${GREEN}✓ 服务启动成功${NC}"
        break
    fi
    echo -n "."
    sleep 1
done

# 2. 启动 ngrok
echo ""
echo "${YELLOW}2. 启动 ngrok 内网穿透...${NC}"

# 停止旧的 ngrok
pkill -f "ngrok http" 2>/dev/null
sleep 1

# 启动 ngrok
/opt/homebrew/bin/ngrok http 8080 > /tmp/ngrok.log 2>&1 &
NGROK_PID=$!
echo "ngrok 进程ID: $NGROK_PID"

# 等待 ngrok 启动
echo "等待 ngrok 启动..."
sleep 3

# 3. 获取公网 URL
echo ""
echo "${YELLOW}3. 获取 ngrok 公网地址...${NC}"
NGROK_URL=""
for i in {1..10}; do
    NGROK_URL=$(curl -s http://localhost:4040/api/tunnels 2>/dev/null | grep -o '"public_url":"https://[^"]*"' | head -1 | cut -d'"' -f4)
    if [ ! -z "$NGROK_URL" ]; then
        break
    fi
    sleep 1
done

if [ -z "$NGROK_URL" ]; then
    echo "❌ 无法获取 ngrok URL"
    echo "请手动执行: /opt/homebrew/bin/ngrok http 8080"
    exit 1
fi

echo "${GREEN}✓ ngrok URL: $NGROK_URL${NC}"

# 4. 更新配置
echo ""
echo "${YELLOW}4. 更新配置文件...${NC}"
H5_URL="${NGROK_URL}/h5/oauth.html"

# 备份配置文件
cp src/main/resources/application.yml src/main/resources/application.yml.bak

# 更新 h5-base-url
sed -i.tmp "s|h5-base-url:.*|h5-base-url: ${H5_URL}|g" src/main/resources/application.yml
rm -f src/main/resources/application.yml.tmp

echo "${GREEN}✓ h5-base-url 已更新为: ${H5_URL}${NC}"

# 5. 重启应用
echo ""
echo "${YELLOW}5. 重启应用以应用新配置...${NC}"
kill -9 $SPRING_PID 2>/dev/null
sleep 2

mvn spring-boot:run > /tmp/wechat-app.log 2>&1 &
NEW_SPRING_PID=$!
echo "新的 Spring Boot 进程ID: $NEW_SPRING_PID"

# 等待重启
echo "等待服务重启..."
for i in {1..15}; do
    if curl -s http://localhost:8080/api/wechat/health > /dev/null 2>&1; then
        echo "${GREEN}✓ 服务重启成功${NC}"
        break
    fi
    echo -n "."
    sleep 1
done

# 6. 显示总结
echo ""
echo "========================================"
echo "${GREEN}  ✓ 启动完成！${NC}"
echo "========================================"
echo ""
echo "📋 重要信息："
echo ""
echo "1️⃣  本地服务: http://localhost:8080"
echo "2️⃣  公网地址: ${NGROK_URL}"
echo "3️⃣  H5页面: ${H5_URL}"
echo "4️⃣  回调地址: ${NGROK_URL}/api/wechat/callback"
echo ""
echo "📝 下一步操作："
echo ""
echo "1. 在企业微信管理后台配置回调URL:"
echo "   ${NGROK_URL}/api/wechat/callback"
echo ""
echo "2. 查看 ngrok 控制面板: http://localhost:4040"
echo ""
echo "3. 查看应用日志: tail -f /tmp/wechat-app.log"
echo ""
echo "⚠️  注意: ngrok 免费版每次启动URL都会变化，需要重新配置企业微信回调地址"
echo ""
echo "========================================"

