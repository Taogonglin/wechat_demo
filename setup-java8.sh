#!/bin/bash

echo "======================================"
echo "  配置Java 8环境"
echo "======================================"

# 方法1: 使用Homebrew安装（如果网络正常）
echo ""
echo "方法1: 使用Homebrew安装Java 8"
echo "执行: brew install openjdk@8"
echo ""

# 方法2: 使用SDKMAN（推荐，更简单）
echo "方法2: 使用SDKMAN安装（推荐）"
echo "1. 安装SDKMAN:"
echo "   curl -s \"https://get.sdkman.io\" | bash"
echo "   source \"\$HOME/.sdkman/bin/sdkman-init.sh\""
echo ""
echo "2. 安装Java 8:"
echo "   sdk install java 8.0.392-amzn"
echo "   sdk use java 8.0.392-amzn"
echo ""

# 方法3: 手动下载安装
echo "方法3: 手动下载安装"
echo "1. 访问: https://adoptium.net/temurin/releases/?version=8"
echo "2. 下载macOS版本（.pkg文件）"
echo "3. 安装后配置JAVA_HOME"
echo ""

# 检查当前Java版本
echo "当前Java版本:"
java -version 2>&1 | head -1

echo ""
echo "======================================"
echo "  安装Java 8后，执行以下命令："
echo "======================================"
echo ""
echo "1. 设置JAVA_HOME（临时，当前终端）:"
echo "   export JAVA_HOME=\$(/usr/libexec/java_home -v 1.8)"
echo "   export PATH=\$JAVA_HOME/bin:\$PATH"
echo ""
echo "2. 验证Java版本:"
echo "   java -version"
echo ""
echo "3. 编译项目:"
echo "   cd /Users/taogonglin/projects/wechat_demo"
echo "   mvn clean compile -DskipTests"
echo ""
echo "======================================"

