#!/bin/bash
# 服务器端部署脚本
# 在服务器上执行此脚本以更新和重启服务

echo "========================================="
echo "企业微信回调服务 - 服务器部署脚本"
echo "========================================="

# 设置颜色
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 项目目录
PROJECT_DIR="/root/wechat_demo"
APP_NAME="wechat-work-callback"

# 步骤 1: 进入项目目录
echo -e "${YELLOW}[1/6] 进入项目目录...${NC}"
cd $PROJECT_DIR || { echo -e "${RED}错误: 项目目录不存在${NC}"; exit 1; }
echo -e "${GREEN}✓ 当前目录: $(pwd)${NC}"

# 步骤 2: 拉取最新代码
echo -e "\n${YELLOW}[2/6] 拉取最新代码...${NC}"
git pull origin main || { echo -e "${RED}错误: 代码拉取失败${NC}"; exit 1; }
echo -e "${GREEN}✓ 代码更新成功${NC}"

# 步骤 3: 停止旧服务
echo -e "\n${YELLOW}[3/6] 停止旧服务...${NC}"
OLD_PID=$(ps aux | grep "wechat-work-callback" | grep -v grep | awk '{print $2}')
if [ -n "$OLD_PID" ]; then
    kill -9 $OLD_PID
    echo -e "${GREEN}✓ 已停止旧服务 (PID: $OLD_PID)${NC}"
else
    echo -e "${YELLOW}⚠ 没有运行中的服务${NC}"
fi

# 检查 Redis 是否运行
echo -e "\n${YELLOW}[4/6] 检查 Redis 状态...${NC}"
if ! pgrep -x "redis-server" > /dev/null; then
    echo -e "${YELLOW}⚠ Redis 未运行，尝试启动...${NC}"
    redis-server --daemonize yes
    sleep 2
fi
if pgrep -x "redis-server" > /dev/null; then
    echo -e "${GREEN}✓ Redis 运行正常${NC}"
else
    echo -e "${RED}错误: Redis 启动失败${NC}"
    exit 1
fi

# 步骤 4: 编译项目
echo -e "\n${YELLOW}[5/6] 编译项目...${NC}"
mvn clean package -DskipTests || { echo -e "${RED}错误: 编译失败${NC}"; exit 1; }
echo -e "${GREEN}✓ 编译成功${NC}"

# 步骤 5: 启动新服务
echo -e "\n${YELLOW}[6/6] 启动新服务...${NC}"
nohup java -jar target/${APP_NAME}-1.0.0.jar > /root/wechat-app.log 2>&1 &
NEW_PID=$!
echo -e "${GREEN}✓ 服务已启动 (PID: $NEW_PID)${NC}"

# 等待几秒让服务启动
sleep 5

# 检查服务是否正常运行
if ps -p $NEW_PID > /dev/null; then
    echo -e "\n${GREEN}========================================="
    echo -e "✓ 部署成功！"
    echo -e "=========================================${NC}"
    echo -e "服务状态: ${GREEN}运行中${NC}"
    echo -e "进程 ID: ${GREEN}$NEW_PID${NC}"
    echo -e "日志文件: /root/wechat-app.log"
    echo -e "\n查看日志命令:"
    echo -e "  tail -f /root/wechat-app.log"
    echo -e "\n测试健康检查:"
    echo -e "  curl http://localhost:8080/api/wechat/health"
else
    echo -e "\n${RED}========================================="
    echo -e "✗ 部署失败！"
    echo -e "=========================================${NC}"
    echo -e "请查看日志文件: /root/wechat-app.log"
    exit 1
fi

