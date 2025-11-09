# 🚀 SSH连接快速开始

## 1. 首次连接

### 基本连接

```bash
ssh root@你的服务器IP
```

**示例：**
```bash
ssh root@123.456.789.0
```

### 如果使用非标准端口

```bash
ssh -p 2222 root@123.456.789.0
```

---

## 2. 配置免密登录（推荐）

### 步骤1：生成SSH密钥（如果还没有）

```bash
ssh-keygen -t rsa -b 4096
```

直接回车使用默认设置。

### 步骤2：复制公钥到服务器

```bash
ssh-copy-id root@你的服务器IP
```

输入一次密码后，以后就不需要密码了。

### 步骤3：测试免密登录

```bash
ssh root@你的服务器IP
```

如果直接登录成功，说明配置完成！

---

## 3. 上传项目到服务器

### 方法1：使用Git（推荐）

```bash
# SSH登录服务器
ssh root@你的服务器IP

# 安装Git
yum install git -y  # CentOS
# 或
apt-get install git -y  # Ubuntu

# 克隆项目
cd /root
git clone https://github.com/Taogonglin/wechat_demo.git
cd wechat_demo
```

### 方法2：使用scp上传

```bash
cd /Users/taogonglin/projects
scp -r wechat_demo root@你的服务器IP:/root/
```

### 方法3：使用rsync（推荐，支持增量同步）

```bash
rsync -avz --exclude 'target' --exclude '.git' \
  /Users/taogonglin/projects/wechat_demo/ \
  root@你的服务器IP:/root/wechat_demo/
```

---

## 4. 在服务器上安装环境

```bash
# SSH登录服务器
ssh root@你的服务器IP

# 安装Java 8
yum install java-1.8.0-openjdk java-1.8.0-openjdk-devel -y  # CentOS
# 或
apt-get install openjdk-8-jdk -y  # Ubuntu

# 安装Maven
yum install maven -y  # CentOS
# 或
apt-get install maven -y  # Ubuntu

# 安装Redis（可选）
yum install redis -y  # CentOS
systemctl start redis
systemctl enable redis
```

---

## 5. 配置和启动项目

```bash
# 编辑配置文件
cd /root/wechat_demo
vi src/main/resources/application.yml

# 修改以下配置：
# - contact-secret: 填入真实的Secret
# - h5-base-url: http://你的服务器IP:8080/h5/oauth.html

# 编译项目
mvn clean package -DskipTests

# 启动项目（后台运行）
nohup mvn spring-boot:run > app.log 2>&1 &

# 查看日志
tail -f app.log
```

---

## 6. 配置防火墙

```bash
# CentOS (firewalld)
firewall-cmd --permanent --add-port=8080/tcp
firewall-cmd --reload

# Ubuntu (ufw)
ufw allow 8080/tcp
ufw reload
```

**云服务器还需要在控制台配置安全组：**
- 阿里云：ECS控制台 → 安全组 → 添加规则（端口8080）
- 腾讯云：CVM控制台 → 安全组 → 入站规则（端口8080）

---

## 7. 使用自动部署脚本

### 配置脚本

编辑 `deploy.sh`，修改以下变量：

```bash
SERVER_USER="root"
SERVER_IP="你的服务器IP"
SERVER_PORT="22"
PROJECT_DIR="/root/wechat_demo"
```

### 执行部署

```bash
./deploy.sh
```

脚本会自动：
1. 检查SSH连接
2. 上传代码到服务器
3. 编译项目
4. 启动服务

---

## 8. 验证部署

```bash
# 在服务器上测试
curl http://localhost:8080/api/wechat/health

# 在本地浏览器访问
http://你的服务器IP:8080/api/wechat/health
```

应该返回：`OK`

---

## 📚 更多信息

详细说明请查看：`SSH-DEPLOY-GUIDE.md`

---

## 🔧 常用命令

```bash
# 查看服务日志
ssh root@服务器IP 'tail -f /root/wechat_demo/app.log'

# 重启服务
ssh root@服务器IP 'pkill -f "spring-boot:run" && cd /root/wechat_demo && nohup mvn spring-boot:run > app.log 2>&1 &'

# 查看Java进程
ssh root@服务器IP 'ps aux | grep java'

# 停止服务
ssh root@服务器IP 'pkill -f "spring-boot:run"'
```

---

**祝你部署顺利！** 🎉

