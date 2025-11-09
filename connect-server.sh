#!/bin/bash

# 服务器配置
SERVER_IP="47.108.150.198"
SERVER_USER="root"
SERVER="$SERVER_USER@$SERVER_IP"

echo "========================================"
echo "  连接到服务器 $SERVER_IP"
echo "========================================"
echo ""

# 检查是否需要配置公钥
echo "正在检查SSH密钥配置..."
echo ""

# 尝试连接
ssh -o ConnectTimeout=10 -o BatchMode=yes $SERVER "echo 'SSH密钥已配置，连接成功'" 2>/dev/null

if [ $? -eq 0 ]; then
    echo "✓ SSH密钥已配置，可以免密登录"
    echo ""
    echo "直接连接："
    ssh $SERVER
else
    echo "⚠️  需要配置SSH密钥或输入密码"
    echo ""
    echo "选择操作："
    echo "1. 配置SSH密钥（免密登录，推荐）"
    echo "2. 直接使用密码连接"
    echo ""
    read -p "请选择 (1/2): " choice
    
    case $choice in
        1)
            echo ""
            echo "正在配置SSH密钥..."
            echo "请输入服务器密码："
            ssh-copy-id $SERVER
            
            if [ $? -eq 0 ]; then
                echo ""
                echo "✓ SSH密钥配置成功！"
                echo "现在可以免密登录了"
                echo ""
                read -p "是否现在连接到服务器？(y/n): " connect
                if [ "$connect" = "y" ] || [ "$connect" = "Y" ]; then
                    ssh $SERVER
                fi
            else
                echo ""
                echo "❌ 密钥配置失败"
            fi
            ;;
        2)
            echo ""
            echo "使用密码连接..."
            ssh $SERVER
            ;;
        *)
            echo "无效的选择"
            exit 1
            ;;
    esac
fi

