# 部署指南

## 前置条件

### 1. 环境要求
- JDK 1.8 或更高版本
- Maven 3.6+
- Redis 5.0+
- 一个可以从外网访问的服务器（用于接收企业微信回调）

### 2. 企业微信配置

#### 2.1 获取企业ID
1. 登录[企业微信管理后台](https://work.weixin.qq.com/)
2. 进入"我的企业" -> "企业信息"
3. 复制"企业ID"

#### 2.2 获取客户联系Secret
1. 进入"应用管理" -> "客户联系"
2. 查看或创建"客户联系"应用
3. 复制"Secret"

#### 2.3 配置回调URL
1. 在"客户联系"应用中，找到"接收消息"设置
2. 配置回调URL：`https://your-domain.com/api/wechat/callback`
3. 设置Token和EncodingAESKey（随机生成即可）
4. 保存配置

## 配置步骤

### 1. 修改配置文件

复制 `application-example.yml` 为 `application.yml`：

```bash
cd src/main/resources
cp application-example.yml application.yml
```

编辑 `application.yml`，填入实际配置：

```yaml
wechat:
  work:
    corp-id: 你的企业ID
    contact-secret: 你的客户联系Secret
    token: 你设置的Token
    encoding-aes-key: 你设置的EncodingAESKey
    h5-base-url: 你的H5页面地址
```

### 2. 启动Redis

确保Redis服务已启动：

```bash
# macOS
brew services start redis

# Linux
systemctl start redis

# 或直接运行
redis-server
```

### 3. 编译项目

```bash
mvn clean package
```

### 4. 运行应用

#### 开发环境运行
```bash
mvn spring-boot:run
```

#### 生产环境运行
```bash
java -jar target/wechat-work-callback-1.0.0.jar
```

#### 后台运行
```bash
nohup java -jar target/wechat-work-callback-1.0.0.jar > output.log 2>&1 &
```

## 验证部署

### 1. 检查服务状态

访问健康检查接口：
```bash
curl http://localhost:8080/api/wechat/health
```

应该返回：`OK`

### 2. 测试回调URL

在企业微信管理后台保存回调URL配置时，系统会自动发送验证请求。如果配置正确，应该能够保存成功。

### 3. 测试客户添加

1. 让一个外部用户添加企业成员的企业微信
2. 查看应用日志，应该能看到：
   - 收到回调消息
   - 解析客户添加事件
   - 发送H5链接

## 生产环境建议

### 1. 使用HTTPS

企业微信要求回调URL必须使用HTTPS。可以使用Nginx做反向代理：

```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location /api/wechat/ {
        proxy_pass http://localhost:8080/api/wechat/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 2. 配置日志

修改 `application.yml` 中的日志配置：

```yaml
logging:
  level:
    com.company.wechat: info  # 生产环境建议使用info级别
    root: warn
  file:
    name: logs/wechat-callback.log
    max-size: 100MB
    max-history: 30
```

### 3. 配置Redis持久化

编辑 `redis.conf`：

```
# 开启AOF持久化
appendonly yes
appendfsync everysec

# 开启RDB持久化
save 900 1
save 300 10
save 60 10000
```

### 4. 使用进程管理工具

推荐使用 systemd 管理应用进程：

创建 `/etc/systemd/system/wechat-callback.service`：

```ini
[Unit]
Description=WeChat Work Callback Service
After=network.target redis.service

[Service]
Type=simple
User=your-user
WorkingDirectory=/path/to/app
ExecStart=/usr/bin/java -jar /path/to/app/wechat-work-callback-1.0.0.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动服务：

```bash
systemctl daemon-reload
systemctl start wechat-callback
systemctl enable wechat-callback
```

## 监控和维护

### 1. 查看日志

```bash
# 实时查看日志
tail -f logs/wechat-callback.log

# 查看最近100行
tail -n 100 logs/wechat-callback.log

# 查看错误日志
grep ERROR logs/wechat-callback.log
```

### 2. 监控Redis

```bash
# 连接Redis
redis-cli

# 查看Access Token
GET wechat:access_token

# 查看所有key
KEYS *
```

### 3. 性能监控

建议使用以下工具进行监控：
- Spring Boot Actuator（应用监控）
- Prometheus + Grafana（指标监控）
- ELK Stack（日志分析）

## 常见问题

### 1. 回调URL验证失败

**可能原因：**
- Token或EncodingAESKey配置错误
- 服务未启动或无法从外网访问
- HTTPS证书问题

**解决方法：**
- 检查配置文件中的Token和EncodingAESKey是否正确
- 确保服务器防火墙允许外网访问
- 检查HTTPS证书是否有效

### 2. 收不到回调消息

**可能原因：**
- 回调URL未正确配置
- 应用未开启"接收消息"功能
- 网络问题

**解决方法：**
- 检查企业微信后台的回调URL配置
- 确保"客户联系"应用已开启
- 查看应用日志是否有错误信息

### 3. 发送消息失败

**可能原因：**
- Access Token过期或无效
- 客户ID错误
- 应用权限不足

**解决方法：**
- 检查日志中的错误信息
- 确认Contact Secret配置正确
- 确保应用有"客户联系"权限

## 扩展功能

### 添加数据库支持

如果需要持久化客户信息，可以添加数据库依赖：

1. 在 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>
```

2. 在 `application.yml` 中配置数据源：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/wechat_work?useUnicode=true&characterEncoding=utf8
    username: root
    password: your_password
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

3. 实现 `CustomerEventService` 中的数据库操作逻辑

## 技术支持

如有问题，请查看日志文件或联系开发团队。

