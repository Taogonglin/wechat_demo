# 企业微信客户添加事件监听服务 - 部署指南

## 一、前置准备

### 1.1 服务器要求
- Java 8 或更高版本
- Maven 3.6+
- Redis 5.0+
- 公网可访问的域名（必须支持HTTPS）

### 1.2 企业微信配置准备

#### 步骤1：获取企业信息
1. 登录企业微信管理后台：https://work.weixin.qq.com/
2. 进入「我的企业」-「企业信息」，获取**企业ID**（CorpID）

#### 步骤2：配置客户联系Secret
1. 进入「应用管理」-「客户联系」
2. 点击「客户联系」应用
3. 获取**Secret**（ContactSecret）
4. 记录应用的**AgentId**（可选）

#### 步骤3：配置接收事件服务器
1. 在「客户联系」应用页面，找到「接收事件服务器」配置
2. 点击「设置接收事件服务器」
3. 生成以下信息：
   - **Token**：随机字符串，用于验证签名
   - **EncodingAESKey**：点击「随机生成」按钮生成
4. **暂时不要保存**，等部署完服务后再保存

## 二、配置文件

### 2.1 修改application.yml

编辑 `src/main/resources/application.yml`：

```yaml
server:
  port: 8080

spring:
  application:
    name: wechat-work-callback
  redis:
    host: localhost          # Redis服务器地址
    port: 6379               # Redis端口
    database: 0
    timeout: 3000ms

wechat:
  work:
    corp-id: ww1234567890abcdef              # 替换为你的企业ID
    contact-secret: xxxxxxxxxxxxxxxxxxxx      # 替换为客户联系Secret
    token: YourRandomToken123                 # 替换为你生成的Token
    encoding-aes-key: YourEncodingAESKey43CharactersLong  # 替换为EncodingAESKey
    h5-base-url: https://your-domain.com/h5/customer      # 替换为你的H5页面地址
    token-expire-time: 7000

logging:
  level:
    com.company.wechat: info
    root: info
```

### 2.2 生产环境配置（可选）

创建 `application-prod.yml` 用于生产环境：

```yaml
server:
  port: 8080

spring:
  redis:
    host: your-redis-server
    port: 6379
    password: your-redis-password
    database: 0

wechat:
  work:
    corp-id: ${WECHAT_CORP_ID}
    contact-secret: ${WECHAT_CONTACT_SECRET}
    token: ${WECHAT_TOKEN}
    encoding-aes-key: ${WECHAT_ENCODING_AES_KEY}
    h5-base-url: ${WECHAT_H5_BASE_URL}

logging:
  level:
    com.company.wechat: info
    root: warn
  file:
    name: /var/log/wechat-work-callback/application.log
```

## 三、编译和部署

### 3.1 本地编译

```bash
# 克隆代码（如果从仓库获取）
git clone <your-repository-url>
cd wechat_demo

# 编译打包
mvn clean package -DskipTests

# 编译后的jar文件位于
# target/wechat-work-callback-1.0.0.jar
```

### 3.2 部署到服务器

#### 方式1：直接运行JAR

```bash
# 上传jar文件到服务器
scp target/wechat-work-callback-1.0.0.jar user@server:/opt/apps/

# SSH登录服务器
ssh user@server

# 启动Redis（如果未启动）
systemctl start redis

# 运行应用
cd /opt/apps
java -jar wechat-work-callback-1.0.0.jar

# 后台运行
nohup java -jar wechat-work-callback-1.0.0.jar > app.log 2>&1 &
```

#### 方式2：使用systemd服务（推荐）

创建服务文件 `/etc/systemd/system/wechat-callback.service`：

```ini
[Unit]
Description=WeChat Work Callback Service
After=network.target redis.service

[Service]
Type=simple
User=appuser
WorkingDirectory=/opt/apps
ExecStart=/usr/bin/java -jar /opt/apps/wechat-work-callback-1.0.0.jar
Restart=on-failure
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
systemctl start wechat-callback

# 设置开机自启
systemctl enable wechat-callback

# 查看状态
systemctl status wechat-callback

# 查看日志
journalctl -u wechat-callback -f
```

### 3.3 配置Nginx反向代理（HTTPS）

创建Nginx配置文件 `/etc/nginx/sites-available/wechat-callback`：

```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    # 重定向到HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    # SSL证书配置
    ssl_certificate /path/to/your/certificate.crt;
    ssl_certificate_key /path/to/your/private.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # 日志配置
    access_log /var/log/nginx/wechat-callback-access.log;
    error_log /var/log/nginx/wechat-callback-error.log;

    # 代理配置
    location /api/wechat/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # H5页面（如果也部署在这里）
    location /h5/ {
        # 配置H5页面路径
        root /var/www/html;
        try_files $uri $uri/ /h5/index.html;
    }
}
```

启用配置：

```bash
# 创建软链接
ln -s /etc/nginx/sites-available/wechat-callback /etc/nginx/sites-enabled/

# 测试配置
nginx -t

# 重载Nginx
systemctl reload nginx
```

## 四、配置企业微信回调URL

### 4.1 验证服务是否正常

```bash
# 测试健康检查接口
curl https://your-domain.com/api/wechat/health
# 应该返回: OK
```

### 4.2 在企业微信后台配置回调URL

1. 登录企业微信管理后台
2. 进入「应用管理」-「客户联系」
3. 找到「接收事件服务器」配置
4. 填写以下信息：
   - **URL**: `https://your-domain.com/api/wechat/callback`
   - **Token**: 与application.yml中配置的token一致
   - **EncodingAESKey**: 与application.yml中配置的encoding-aes-key一致
5. 点击「保存」

**注意**：保存时企业微信会发送验证请求，确保服务已正常运行。

### 4.3 配置事件订阅

在「接收事件服务器」下方的「企业客户事件」中，勾选以下事件：
- ✅ 添加企业客户事件
- ✅ 编辑企业客户事件
- ✅ 删除企业客户事件（可选）

点击「保存」。

## 五、测试验证

### 5.1 添加测试客户

1. 使用企业成员的企业微信
2. 生成联系方式二维码
3. 使用个人微信扫码添加
4. 查看是否收到包含H5链接的欢迎消息

### 5.2 查看日志

```bash
# 如果使用systemd
journalctl -u wechat-callback -f

# 如果使用nohup
tail -f app.log

# 应该看到类似以下日志：
# 收到客户添加事件 - External UserID: wmxxxxxxxxxxxxxx
# 生成H5链接: https://your-domain.com/h5/customer?external_userid=wmxxxxxxxxxxxxxx
# 成功发送H5链接给客户
```

### 5.3 验证H5链接

访问生成的H5链接，确认能正确获取到external_userid参数。

## 六、监控和维护

### 6.1 日志轮转

配置logrotate `/etc/logrotate.d/wechat-callback`：

```
/var/log/wechat-work-callback/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 0644 appuser appuser
}
```

### 6.2 监控检查项

- 服务进程是否运行
- Redis连接是否正常
- 企业微信API调用是否成功
- Access Token是否正常获取和缓存

### 6.3 常见问题排查

#### 问题1：回调URL验证失败
- 检查Token和EncodingAESKey配置是否正确
- 检查服务是否正常运行
- 检查Nginx配置是否正确转发请求
- 查看应用日志排查具体错误

#### 问题2：收不到事件推送
- 确认事件订阅已正确配置
- 确认回调URL已验证通过
- 查看日志确认是否收到推送

#### 问题3：发送消息失败
- 检查CorpId和ContactSecret配置是否正确
- 确认Access Token获取成功
- 检查企业微信API返回的错误码和错误信息

## 七、安全建议

1. **使用HTTPS**：企业微信要求回调URL必须使用HTTPS
2. **定期更新密钥**：定期更换Token和EncodingAESKey
3. **限制访问**：在防火墙配置中限制只允许企业微信服务器IP访问
4. **日志脱敏**：生产环境日志不要记录敏感信息
5. **监控告警**：配置监控系统，及时发现异常

## 八、扩展功能

### 8.1 添加数据库存储
可以添加数据库（如MySQL）来存储客户信息和事件记录。

### 8.2 添加消息队列
对于高并发场景，可以使用消息队列（如RabbitMQ、Kafka）异步处理事件。

### 8.3 完善欢迎语
可以使用企业微信的欢迎语API发送更丰富的消息格式（图片、链接、小程序等）。

---

如有问题，请查看日志或联系技术支持。

