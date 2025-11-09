# 🖥️ 服务器配置信息

## 服务器信息

- **服务器IP**: `47.108.150.198`
- **SSH端口**: `22`
- **SSH用户**: `root`

---

## 🔗 项目访问地址

### 公网访问地址

- **健康检查**: http://47.108.150.198:8080/api/wechat/health
- **回调地址**: http://47.108.150.198:8080/api/wechat/callback
- **H5页面**: http://47.108.150.198:8080/h5/oauth.html

---

## 📝 企业微信回调配置

在企业微信管理后台配置以下信息：

```yaml
回调URL: http://47.108.150.198:8080/api/wechat/callback
Token: JKBYwA8yEwhjKEyKWRQe
EncodingAESKey: 3b3NP2JJACgzakSh3Enh1vGsWsWVcAbsXjlVeFEKLRi
```

---

## 🚀 连接服务器

### 方式1：直接SSH连接

```bash
ssh root@47.108.150.198
```

### 方式2：使用连接脚本

```bash
./connect-server.sh
```

### 方式3：配置免密登录

```bash
# 复制SSH公钥到服务器
ssh-copy-id root@47.108.150.198

# 之后直接连接，无需密码
ssh root@47.108.150.198
```

---

## 📦 部署项目到服务器

### 方式1：使用自动部署脚本（推荐）

```bash
./deploy.sh
```

脚本会自动：
- 上传代码到服务器
- 编译项目
- 启动服务

### 方式2：手动部署

```bash
# 1. SSH连接到服务器
ssh root@47.108.150.198

# 2. 克隆项目
cd /root
git clone https://github.com/Taogonglin/wechat_demo.git
cd wechat_demo

# 3. 安装环境
yum install java-1.8.0-openjdk java-1.8.0-openjdk-devel maven git redis -y
systemctl start redis
systemctl enable redis

# 4. 编译启动
mvn clean package -DskipTests
nohup mvn spring-boot:run > app.log 2>&1 &

# 5. 配置防火墙
firewall-cmd --permanent --add-port=8080/tcp
firewall-cmd --reload

# 6. 验证
curl http://localhost:8080/api/wechat/health
```

---

## 🔧 常用操作命令

### 查看服务状态

```bash
# 查看日志
ssh root@47.108.150.198 'tail -f /root/wechat_demo/app.log'

# 查看进程
ssh root@47.108.150.198 'ps aux | grep java'

# 测试服务
curl http://47.108.150.198:8080/api/wechat/health
```

### 重启服务

```bash
ssh root@47.108.150.198 'pkill -f "spring-boot:run" && cd /root/wechat_demo && nohup mvn spring-boot:run > app.log 2>&1 &'
```

### 停止服务

```bash
ssh root@47.108.150.198 'pkill -f "spring-boot:run"'
```

---

## 🔐 安全配置

### 开放端口（已配置）

- **8080**: Spring Boot应用端口
- **22**: SSH端口
- **6379**: Redis端口（仅localhost访问）

### 防火墙规则

```bash
# 查看防火墙状态
firewall-cmd --list-all

# 开放8080端口
firewall-cmd --permanent --add-port=8080/tcp
firewall-cmd --reload
```

---

## 📊 监控和日志

### 查看应用日志

```bash
tail -f /root/wechat_demo/app.log
```

### 查看系统资源

```bash
# CPU和内存使用情况
top

# 磁盘使用情况
df -h

# 网络连接
netstat -tulpn | grep 8080
```

---

## ⚠️ 重要提示

1. **安全组配置**: 确保云服务器安全组已开放8080端口
2. **域名配置**: 如需绑定域名，请配置DNS解析指向 47.108.150.198
3. **HTTPS配置**: 生产环境建议配置SSL证书
4. **备份**: 定期备份代码和数据

---

最后更新时间: 2025-11-09

