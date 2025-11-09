# 🚀 SSH连接远程服务器部署指南

## 📋 目录
1. [SSH连接基础](#1-ssh连接基础)
2. [配置SSH密钥（免密登录）](#2-配置ssh密钥免密登录)
3. [上传项目到服务器](#3-上传项目到服务器)
4. [在服务器上部署项目](#4-在服务器上部署项目)
5. [常用SSH命令](#5-常用ssh命令)

---

## 1. SSH连接基础

### 基本连接命令

```bash
ssh 用户名@服务器IP
```

**示例：**
```bash
ssh root@123.456.789.0
```

**如果服务器使用非标准端口（默认22）：**
```bash
ssh -p 端口号 用户名@服务器IP
```

**示例（端口2222）：**
```bash
ssh -p 2222 root@123.456.789.0
```

### 首次连接

首次连接时会提示确认服务器指纹，输入 `yes` 确认：

```
The authenticity of host '123.456.789.0 (123.456.789.0)' can't be established.
ECDSA key fingerprint is SHA256:xxxxx.
Are you sure you want to continue connecting (yes/no/[fingerprint])? yes
```

---

## 2. 配置SSH密钥（免密登录）

### 步骤1：检查本地是否已有SSH密钥

```bash
ls -la ~/.ssh
```

如果看到 `id_rsa` 和 `id_rsa.pub`，说明已有密钥，跳到步骤3。

### 步骤2：生成SSH密钥对

```bash
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"
```

按提示操作：
- 保存位置：直接回车（默认 `~/.ssh/id_rsa`）
- 密码：可以设置密码，也可以直接回车（不设置）

### 步骤3：复制公钥到服务器

**方法A：使用 ssh-copy-id（推荐）**

```bash
ssh-copy-id 用户名@服务器IP
```

**方法B：手动复制**

```bash
# 1. 查看公钥内容
cat ~/.ssh/id_rsa.pub

# 2. 复制输出的内容

# 3. SSH登录服务器
ssh 用户名@服务器IP

# 4. 在服务器上执行
mkdir -p ~/.ssh
chmod 700 ~/.ssh
echo "粘贴你的公钥内容" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

### 步骤4：测试免密登录

```bash
ssh 用户名@服务器IP
```

如果不需要输入密码就能登录，说明配置成功！

---

## 3. 上传项目到服务器

### 方法1：使用 scp 命令（推荐）

**上传整个项目目录：**

```bash
cd /Users/taogonglin/projects
scp -r wechat_demo 用户名@服务器IP:/root/
```

**上传单个文件：**

```bash
scp 文件路径 用户名@服务器IP:目标路径
```

**示例：**
```bash
scp pom.xml root@123.456.789.0:/root/wechat_demo/
```

### 方法2：使用 rsync（更高效，支持增量同步）

```bash
rsync -avz --exclude 'target' --exclude '.git' \
  /Users/taogonglin/projects/wechat_demo/ \
  用户名@服务器IP:/root/wechat_demo/
```

**参数说明：**
- `-a`: 归档模式，保持文件属性
- `-v`: 显示详细信息
- `-z`: 压缩传输
- `--exclude`: 排除不需要的文件

### 方法3：使用 Git（推荐用于生产环境）

**在服务器上直接克隆：**

```bash
# SSH登录服务器
ssh 用户名@服务器IP

# 安装Git（如果没有）
yum install git -y  # CentOS/RHEL
# 或
apt-get install git -y  # Ubuntu/Debian

# 克隆项目
cd /root
git clone https://github.com/Taogonglin/wechat_demo.git
cd wechat_demo
```

---

## 4. 在服务器上部署项目

### 步骤1：SSH登录服务器

```bash
ssh 用户名@服务器IP
```

### 步骤2：安装必要环境

**安装Java 8：**

```bash
# CentOS/RHEL
yum install java-1.8.0-openjdk java-1.8.0-openjdk-devel -y

# Ubuntu/Debian
apt-get update
apt-get install openjdk-8-jdk -y

# 验证安装
java -version
```

**安装Maven：**

```bash
# CentOS/RHEL
yum install maven -y

# Ubuntu/Debian
apt-get install maven -y

# 验证安装
mvn -version
```

**安装Redis（如果需要）：**

```bash
# CentOS/RHEL
yum install redis -y
systemctl start redis
systemctl enable redis

# Ubuntu/Debian
apt-get install redis-server -y
systemctl start redis
systemctl enable redis
```

### 步骤3：配置项目

```bash
cd /root/wechat_demo

# 编辑配置文件
vi src/main/resources/application.yml
```

**修改配置：**
```yaml
server:
  port: 8080

wechat:
  work:
    corp-id: wwd79126fde9eba684
    contact-secret: your_contact_secret  # 填入真实的Secret
    token: JKBYwA8yEwhjKEyKWRQe
    encoding-aes-key: 3b3NP2JJACgzakSh3Enh1vGsWsWVcAbsXjlVeFEKLRi
    h5-base-url: http://你的服务器IP:8080/h5/oauth.html  # 或使用域名
    token-expire-time: 7000
```

### 步骤4：编译项目

```bash
cd /root/wechat_demo
mvn clean package -DskipTests
```

### 步骤5：启动项目

**方式A：直接运行（测试用）**

```bash
cd /root/wechat_demo
mvn spring-boot:run
```

**方式B：后台运行（推荐）**

```bash
cd /root/wechat_demo
nohup mvn spring-boot:run > app.log 2>&1 &

# 查看日志
tail -f app.log
```

**方式C：使用systemd服务（生产环境推荐）**

创建服务文件：

```bash
sudo vi /etc/systemd/system/wechat-demo.service
```

内容：

```ini
[Unit]
Description=WeChat Work Callback Service
After=network.target redis.service

[Service]
Type=simple
User=root
WorkingDirectory=/root/wechat_demo
ExecStart=/usr/bin/mvn spring-boot:run
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

启动服务：

```bash
# 重载systemd配置
systemctl daemon-reload

# 启动服务
systemctl start wechat-demo

# 设置开机自启
systemctl enable wechat-demo

# 查看状态
systemctl status wechat-demo

# 查看日志
journalctl -u wechat-demo -f
```

### 步骤6：配置防火墙

**开放8080端口：**

```bash
# CentOS/RHEL (firewalld)
firewall-cmd --permanent --add-port=8080/tcp
firewall-cmd --reload

# CentOS/RHEL (iptables)
iptables -A INPUT -p tcp --dport 8080 -j ACCEPT
service iptables save

# Ubuntu/Debian (ufw)
ufw allow 8080/tcp
ufw reload
```

**云服务器还需要在控制台配置安全组：**
- 阿里云：ECS控制台 → 安全组 → 添加规则（端口8080）
- 腾讯云：CVM控制台 → 安全组 → 入站规则（端口8080）

### 步骤7：验证部署

```bash
# 在服务器上测试
curl http://localhost:8080/api/wechat/health

# 应该返回: OK
```

**在本地浏览器访问：**
```
http://你的服务器IP:8080/api/wechat/health
```

---

## 5. 常用SSH命令

### 基本操作

```bash
# 连接服务器
ssh 用户名@服务器IP

# 断开连接
exit
# 或按 Ctrl+D

# 在后台保持连接（使用screen或tmux）
screen -S wechat
# 或
tmux new -s wechat
```

### 文件传输

```bash
# 上传文件
scp 本地文件 用户名@服务器IP:远程路径

# 下载文件
scp 用户名@服务器IP:远程文件 本地路径

# 上传目录
scp -r 本地目录 用户名@服务器IP:远程路径

# 使用rsync同步（推荐）
rsync -avz 本地目录/ 用户名@服务器IP:远程目录/
```

### 端口转发

```bash
# 本地端口转发（将服务器8080端口映射到本地8080）
ssh -L 8080:localhost:8080 用户名@服务器IP

# 远程端口转发
ssh -R 8080:localhost:8080 用户名@服务器IP
```

### 执行远程命令

```bash
# 执行单个命令
ssh 用户名@服务器IP "命令"

# 示例：查看服务器Java版本
ssh root@123.456.789.0 "java -version"

# 示例：重启服务
ssh root@123.456.789.0 "systemctl restart wechat-demo"
```

---

## 📝 快速部署脚本

创建一个部署脚本 `deploy.sh`：

```bash
#!/bin/bash

# 配置信息
SERVER="用户名@服务器IP"
PROJECT_DIR="/root/wechat_demo"
LOCAL_DIR="/Users/taogonglin/projects/wechat_demo"

echo "🚀 开始部署..."

# 1. 上传代码
echo "📤 上传代码到服务器..."
rsync -avz --exclude 'target' --exclude '.git' \
  --exclude '*.log' \
  $LOCAL_DIR/ $SERVER:$PROJECT_DIR/

# 2. 在服务器上编译和重启
echo "🔨 在服务器上编译..."
ssh $SERVER "cd $PROJECT_DIR && mvn clean package -DskipTests"

echo "🔄 重启服务..."
ssh $SERVER "systemctl restart wechat-demo"

echo "✅ 部署完成！"
echo "📋 查看日志: ssh $SERVER 'journalctl -u wechat-demo -f'"
```

**使用方法：**

```bash
chmod +x deploy.sh
./deploy.sh
```

---

## 🔒 安全建议

1. **禁用密码登录，只使用密钥：**
   ```bash
   # 在服务器上编辑
   sudo vi /etc/ssh/sshd_config
   
   # 修改以下配置
   PasswordAuthentication no
   PubkeyAuthentication yes
   
   # 重启SSH服务
   sudo systemctl restart sshd
   ```

2. **修改SSH端口（可选）：**
   ```bash
   # 编辑配置文件
   sudo vi /etc/ssh/sshd_config
   
   # 修改端口（例如改为2222）
   Port 2222
   
   # 重启服务
   sudo systemctl restart sshd
   ```

3. **使用非root用户：**
   ```bash
   # 创建新用户
   adduser deploy
   usermod -aG sudo deploy
   
   # 配置密钥
   ssh-copy-id deploy@服务器IP
   ```

---

## 🐛 常见问题

### 问题1：连接超时

**原因：** 防火墙或安全组未开放22端口

**解决：**
- 检查服务器防火墙
- 检查云服务器安全组规则
- 确认服务器IP是否正确

### 问题2：Permission denied

**原因：** 密钥权限问题

**解决：**
```bash
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys
```

### 问题3：Host key verification failed

**原因：** 服务器密钥已更改

**解决：**
```bash
ssh-keygen -R 服务器IP
```

---

## 📚 参考资源

- [SSH官方文档](https://www.openssh.com/manual.html)
- [GitHub SSH设置指南](https://docs.github.com/en/authentication/connecting-to-github-with-ssh)
- [Linux系统管理基础](https://www.linux.org/)

---

**祝你部署顺利！** 🎉

