#!/bin/bash

###########################################################
# 企业微信回调服务停止脚本
###########################################################

APP_NAME="wechat-work-callback"
APP_HOME=$(cd "$(dirname "$0")/.." && pwd)
PID_FILE="${APP_HOME}/logs/${APP_NAME}.pid"

echo "=========================================="
echo "停止 ${APP_NAME}"
echo "=========================================="

# 检查PID文件是否存在
if [ ! -f ${PID_FILE} ]; then
    echo "应用未运行（找不到PID文件）"
    exit 0
fi

# 读取PID
PID=$(cat ${PID_FILE})

# 检查进程是否存在
if ! ps -p ${PID} > /dev/null 2>&1; then
    echo "应用未运行（进程不存在）"
    rm -f ${PID_FILE}
    exit 0
fi

# 优雅停止应用
echo "正在停止应用，PID: ${PID}"
kill ${PID}

# 等待应用停止
TIMEOUT=30
COUNT=0
while ps -p ${PID} > /dev/null 2>&1; do
    if [ ${COUNT} -ge ${TIMEOUT} ]; then
        echo "应用未在${TIMEOUT}秒内停止，强制终止"
        kill -9 ${PID}
        break
    fi
    echo -n "."
    sleep 1
    COUNT=$((COUNT + 1))
done

echo ""

# 再次检查进程
if ps -p ${PID} > /dev/null 2>&1; then
    echo "❌ 应用停止失败"
    exit 1
else
    echo "✅ 应用已停止"
    rm -f ${PID_FILE}
    exit 0
fi

