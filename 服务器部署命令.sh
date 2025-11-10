#!/bin/bash
# 一键部署到服务器 47.108.150.198
# 使用方法：直接复制下面的命令到终端执行

ssh root@47.108.150.198 << 'ENDSSH'
echo "========================================"
echo "开始部署企业微信回调服务..."
echo "========================================"

cd /root/wechat_demo || exit 1
echo "✓ 进入项目目录"

echo "拉取最新代码..."
git pull origin main
echo "✓ 代码更新完成"

echo "停止旧服务..."
OLD_PID=$(ps aux | grep "wechat-work-callback" | grep -v grep | awk '{print $2}')
if [ -n "$OLD_PID" ]; then
    kill -9 $OLD_PID
    echo "✓ 已停止旧服务 (PID: $OLD_PID)"
else
    echo "⚠ 没有运行中的服务"
fi

echo "检查 Redis..."
if ! pgrep -x "redis-server" > /dev/null; then
    redis-server --daemonize yes
    sleep 2
    echo "✓ Redis 已启动"
else
    echo "✓ Redis 运行正常"
fi

echo "编译项目（约需 20-30 秒）..."
mvn clean package -DskipTests
echo "✓ 编译完成"

echo "启动新服务..."
nohup java -jar target/wechat-work-callback-1.0.0.jar > /root/wechat-app.log 2>&1 &
NEW_PID=$!
echo "✓ 服务已启动 (PID: $NEW_PID)"

sleep 5

echo ""
echo "========================================"
echo "部署完成！正在验证..."
echo "========================================"

# 测试健康检查
HEALTH_CHECK=$(curl -s http://localhost:8080/api/wechat/health)
if [ "$HEALTH_CHECK" = "WeChat Work Callback Service is running" ]; then
    echo "✓ 健康检查通过"
    echo "✓ 服务运行正常"
else
    echo "⚠ 健康检查失败，查看日志："
    tail -30 /root/wechat-app.log
fi

echo ""
echo "========================================"
echo "服务信息："
echo "========================================"
echo "进程ID: $NEW_PID"
echo "日志文件: /root/wechat-app.log"
echo "外网访问: http://47.108.150.198:8080/api/wechat/health"
echo ""
echo "查看实时日志命令："
echo "  tail -f /root/wechat-app.log"
echo ""
echo "查看进程状态命令："
echo "  ps aux | grep wechat-work-callback"
echo "========================================"
ENDSSH

