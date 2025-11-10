#!/bin/bash
# 本地执行：上传部署脚本到服务器并执行

echo "========================================="
echo "准备部署到服务器 47.108.150.198"
echo "========================================="

SERVER="47.108.150.198"
USER="root"

echo ""
echo "步骤 1: 上传部署脚本到服务器..."
scp deploy-server.sh ${USER}@${SERVER}:/root/

echo ""
echo "步骤 2: 登录服务器并执行部署..."
ssh ${USER}@${SERVER} << 'ENDSSH'
cd /root
chmod +x deploy-server.sh
./deploy-server.sh
ENDSSH

echo ""
echo "========================================="
echo "部署完成！"
echo "========================================="

