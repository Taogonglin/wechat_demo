#!/bin/bash
# 企业微信回调服务 - 服务器部署脚本
# 在服务器上直接执行此脚本
# 使用方法: chmod +x deploy-on-server.sh && ./deploy-on-server.sh

set -e  # 遇到错误立即退出

echo "========================================"
echo "企业微信回调服务 - 一键部署"
echo "========================================"
echo ""

# 项目目录
PROJECT_DIR="/root/wechat_demo"

# 进入项目目录
echo "[1/7] 进入项目目录..."
cd $PROJECT_DIR || { echo "❌ 项目目录不存在"; exit 1; }
echo "✓ 当前目录: $(pwd)"
echo ""

# 拉取最新代码
echo "[2/7] 拉取最新代码..."
git pull origin main || { echo "❌ 代码拉取失败"; exit 1; }
echo "✓ 代码更新完成"
echo ""

# 停止旧服务
echo "[3/7] 停止旧服务..."
OLD_PID=$(ps aux | grep "wechat-work-callback" | grep -v grep | awk '{print $2}')
if [ -n "$OLD_PID" ]; then
    kill -9 $OLD_PID
    echo "✓ 已停止旧服务 (PID: $OLD_PID)"
else
    echo "⚠ 没有运行中的服务"
fi
echo ""

# 检查 Redis
echo "[4/7] 检查 Redis..."
if ! pgrep -x "redis-server" > /dev/null; then
    echo "⚠ Redis 未运行，正在启动..."
    redis-server --daemonize yes
    sleep 2
    if pgrep -x "redis-server" > /dev/null; then
        echo "✓ Redis 启动成功"
    else
        echo "❌ Redis 启动失败"
        exit 1
    fi
else
    echo "✓ Redis 运行正常"
fi
echo ""

# 编译项目
echo "[5/7] 编译项目（约需 20-30 秒）..."
mvn clean package -DskipTests || { echo "❌ 编译失败"; exit 1; }
echo "✓ 编译完成"
echo ""

# 启动新服务
echo "[6/7] 启动新服务..."
nohup java -jar target/wechat-work-callback-1.0.0.jar > /root/wechat-app.log 2>&1 &
NEW_PID=$!
echo "✓ 服务已启动 (PID: $NEW_PID)"
echo ""

# 等待服务启动
echo "[7/7] 验证服务状态..."
sleep 5

# 检查进程是否存在
if ps -p $NEW_PID > /dev/null 2>&1; then
    echo "✓ 进程运行正常"
    
    # 测试健康检查
    HEALTH_CHECK=$(curl -s http://localhost:8080/api/wechat/health 2>/dev/null)
    if [ "$HEALTH_CHECK" = "WeChat Work Callback Service is running" ]; then
        echo "✓ 健康检查通过"
        echo ""
        echo "========================================"
        echo "✅ 部署成功！"
        echo "========================================"
        echo "服务信息："
        echo "  - 进程ID: $NEW_PID"
        echo "  - 日志文件: /root/wechat-app.log"
        echo "  - 本地访问: http://localhost:8080/api/wechat/health"
        echo "  - 外网访问: http://47.108.150.198:8080/api/wechat/health"
        echo ""
        echo "常用命令："
        echo "  查看日志: tail -f /root/wechat-app.log"
        echo "  查看进程: ps aux | grep wechat-work-callback"
        echo "  停止服务: kill -9 $NEW_PID"
        echo "========================================"
    else
        echo "⚠ 健康检查失败，查看日志："
        tail -20 /root/wechat-app.log
        echo ""
        echo "========================================"
        echo "⚠️ 服务可能未正常启动"
        echo "========================================"
        echo "请检查日志: tail -f /root/wechat-app.log"
    fi
else
    echo "❌ 进程启动失败，查看日志："
    tail -30 /root/wechat-app.log
    exit 1
fi

