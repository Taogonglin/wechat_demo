#!/bin/bash

###########################################################
# 企业微信回调服务状态检查脚本
###########################################################

APP_NAME="wechat-work-callback"
APP_HOME=$(cd "$(dirname "$0")/.." && pwd)
PID_FILE="${APP_HOME}/logs/${APP_NAME}.pid"

echo "=========================================="
echo "${APP_NAME} 状态检查"
echo "=========================================="

# 检查PID文件
if [ ! -f ${PID_FILE} ]; then
    echo "状态: ❌ 未运行（找不到PID文件）"
    exit 1
fi

# 读取PID
PID=$(cat ${PID_FILE})

# 检查进程
if ps -p ${PID} > /dev/null 2>&1; then
    echo "状态: ✅ 运行中"
    echo "PID: ${PID}"
    
    # 显示进程信息
    echo "=========================================="
    ps -p ${PID} -o pid,ppid,user,%cpu,%mem,vsz,rss,etime,cmd
    
    # 显示端口占用
    echo "=========================================="
    echo "端口占用:"
    netstat -tlnp 2>/dev/null | grep ${PID} || lsof -Pan -p ${PID} -i 2>/dev/null
    
    exit 0
else
    echo "状态: ❌ 未运行（进程不存在）"
    rm -f ${PID_FILE}
    exit 1
fi

