#!/bin/bash

###########################################################
# 企业微信回调服务启动脚本
###########################################################

# 设置Java环境
JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-8-openjdk}
export JAVA_HOME
export PATH=$JAVA_HOME/bin:$PATH

# 应用配置
APP_NAME="wechat-work-callback"
APP_VERSION="1.0.0"
JAR_NAME="${APP_NAME}-${APP_VERSION}.jar"
APP_HOME=$(cd "$(dirname "$0")/.." && pwd)
JAR_PATH="${APP_HOME}/target/${JAR_NAME}"
PID_FILE="${APP_HOME}/logs/${APP_NAME}.pid"
LOG_DIR="${APP_HOME}/logs"

# 创建日志目录
mkdir -p ${LOG_DIR}

# JVM参数
JAVA_OPTS="-server"
JAVA_OPTS="${JAVA_OPTS} -Xms512m -Xmx1024m"
JAVA_OPTS="${JAVA_OPTS} -XX:+UseG1GC"
JAVA_OPTS="${JAVA_OPTS} -XX:MaxGCPauseMillis=200"
JAVA_OPTS="${JAVA_OPTS} -XX:+HeapDumpOnOutOfMemoryError"
JAVA_OPTS="${JAVA_OPTS} -XX:HeapDumpPath=${LOG_DIR}/heap_dump.hprof"
JAVA_OPTS="${JAVA_OPTS} -Dfile.encoding=UTF-8"
JAVA_OPTS="${JAVA_OPTS} -Duser.timezone=Asia/Shanghai"

# Spring配置
SPRING_OPTS=""
# 如果需要指定配置文件，取消下面的注释
# SPRING_OPTS="${SPRING_OPTS} --spring.profiles.active=prod"
# SPRING_OPTS="${SPRING_OPTS} --spring.config.location=file:${APP_HOME}/config/application.yml"

# 检查应用是否已经运行
if [ -f ${PID_FILE} ]; then
    PID=$(cat ${PID_FILE})
    if ps -p ${PID} > /dev/null 2>&1; then
        echo "应用已经在运行中，PID: ${PID}"
        exit 1
    else
        echo "删除过期的PID文件"
        rm -f ${PID_FILE}
    fi
fi

# 检查JAR文件是否存在
if [ ! -f ${JAR_PATH} ]; then
    echo "错误：找不到JAR文件 ${JAR_PATH}"
    echo "请先执行 mvn clean package 编译项目"
    exit 1
fi

# 启动应用
echo "=========================================="
echo "启动 ${APP_NAME}"
echo "=========================================="
echo "JAR路径: ${JAR_PATH}"
echo "日志目录: ${LOG_DIR}"
echo "JVM参数: ${JAVA_OPTS}"
echo "=========================================="

nohup java ${JAVA_OPTS} -jar ${JAR_PATH} ${SPRING_OPTS} \
    > ${LOG_DIR}/console.log 2>&1 &

PID=$!
echo ${PID} > ${PID_FILE}

echo "应用启动成功，PID: ${PID}"
echo "查看日志: tail -f ${LOG_DIR}/console.log"
echo "=========================================="

# 等待几秒检查应用是否成功启动
sleep 3
if ps -p ${PID} > /dev/null 2>&1; then
    echo "✅ 应用运行正常"
else
    echo "❌ 应用启动失败，请查看日志"
    rm -f ${PID_FILE}
    exit 1
fi

