#!/bin/bash

# ============================================
# 企业微信项目快速部署脚本
# ============================================

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 配置信息（请根据实际情况修改）
SERVER_USER="root"
SERVER_IP="47.108.150.198"
SERVER_PORT="22"
PROJECT_DIR="/root/wechat_demo"
LOCAL_DIR="/Users/taogonglin/projects/wechat_demo"

# 检查配置
if [ -z "$SERVER_IP" ]; then
    echo -e "${RED}❌ 错误: 请先配置服务器IP${NC}"
    echo ""
    echo "编辑 deploy.sh，修改以下变量："
    echo "  SERVER_USER=\"root\"          # 服务器用户名"
    echo "  SERVER_IP=\"你的服务器IP\"     # 服务器IP地址"
    echo "  SERVER_PORT=\"22\"            # SSH端口（默认22）"
    echo "  PROJECT_DIR=\"/root/wechat_demo\"  # 服务器上的项目目录"
    exit 1
fi

SERVER="${SERVER_USER}@${SERVER_IP}"

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}  企业微信项目部署脚本${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""
echo "服务器: $SERVER"
echo "项目目录: $PROJECT_DIR"
echo ""

# 检查SSH连接
echo -e "${YELLOW}1. 检查SSH连接...${NC}"
if ssh -p $SERVER_PORT -o ConnectTimeout=5 $SERVER "echo '连接成功'" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ SSH连接正常${NC}"
else
    echo -e "${RED}❌ SSH连接失败，请检查：${NC}"
    echo "  - 服务器IP是否正确: $SERVER_IP"
    echo "  - SSH端口是否正确: $SERVER_PORT"
    echo "  - 是否已配置SSH密钥"
    echo "  - 服务器防火墙是否开放SSH端口"
    exit 1
fi

# 检查本地项目目录
echo ""
echo -e "${YELLOW}2. 检查本地项目...${NC}"
if [ ! -d "$LOCAL_DIR" ]; then
    echo -e "${RED}❌ 本地项目目录不存在: $LOCAL_DIR${NC}"
    exit 1
fi
echo -e "${GREEN}✓ 本地项目目录正常${NC}"

# 上传代码
echo ""
echo -e "${YELLOW}3. 上传代码到服务器...${NC}"
rsync -avz --delete \
    --exclude 'target' \
    --exclude '.git' \
    --exclude '*.log' \
    --exclude '.idea' \
    --exclude '*.iml' \
    -e "ssh -p $SERVER_PORT" \
    $LOCAL_DIR/ $SERVER:$PROJECT_DIR/

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ 代码上传成功${NC}"
else
    echo -e "${RED}❌ 代码上传失败${NC}"
    exit 1
fi

# 在服务器上执行部署
echo ""
echo -e "${YELLOW}4. 在服务器上部署...${NC}"

ssh -p $SERVER_PORT $SERVER << 'ENDSSH'
    cd /root/wechat_demo
    
    # 检查Java
    if ! command -v java &> /dev/null; then
        echo "❌ Java未安装，请先安装Java 8"
        exit 1
    fi
    
    # 检查Maven
    if ! command -v mvn &> /dev/null; then
        echo "❌ Maven未安装，请先安装Maven"
        exit 1
    fi
    
    # 编译项目
    echo "🔨 编译项目..."
    mvn clean package -DskipTests
    
    if [ $? -ne 0 ]; then
        echo "❌ 编译失败"
        exit 1
    fi
    
    echo "✓ 编译成功"
    
    # 停止旧服务（如果存在）
    if systemctl is-active --quiet wechat-demo 2>/dev/null; then
        echo "🛑 停止旧服务..."
        systemctl stop wechat-demo
    fi
    
    # 启动服务
    echo "🚀 启动服务..."
    nohup mvn spring-boot:run > app.log 2>&1 &
    
    sleep 5
    
    # 检查服务是否启动
    if curl -s http://localhost:8080/api/wechat/health > /dev/null; then
        echo "✓ 服务启动成功"
    else
        echo "⚠️  服务可能未正常启动，请查看日志: tail -f app.log"
    fi
ENDSSH

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  ✓ 部署完成！${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo "📋 后续操作："
    echo ""
    echo "1. 查看服务日志:"
    echo "   ssh -p $SERVER_PORT $SERVER 'tail -f $PROJECT_DIR/app.log'"
    echo ""
    echo "2. 测试服务:"
    echo "   curl http://$SERVER_IP:8080/api/wechat/health"
    echo ""
    echo "3. 配置企业微信回调URL:"
    echo "   http://$SERVER_IP:8080/api/wechat/callback"
    echo ""
else
    echo -e "${RED}❌ 部署失败${NC}"
    exit 1
fi

