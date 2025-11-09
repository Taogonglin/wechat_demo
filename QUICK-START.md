# 快速开始指南

## 一、5分钟快速部署

### 1. 环境准备

确保已安装：
- ✅ Java 8+
- ✅ Maven 3.6+
- ✅ Redis 5.0+

### 2. 克隆/下载项目

```bash
cd /path/to/your/projects
# 如果是从git仓库克隆
git clone <repository-url>
cd wechat_demo
```

### 3. 配置应用

复制配置示例文件并修改：

```bash
cp config/application-example.yml src/main/resources/application.yml
```

编辑 `src/main/resources/application.yml`，修改以下配置：

```yaml
wechat:
  work:
    corp-id: 你的企业ID                    # 必填
    contact-secret: 你的客户联系Secret      # 必填
    token: 你的回调Token                   # 必填
    encoding-aes-key: 你的EncodingAESKey  # 必填（43位字符）
    h5-base-url: 你的H5页面地址            # 必填
```

### 4. 启动Redis

```bash
# Linux/Mac
redis-server

# 或使用Docker
docker run -d -p 6379:6379 redis:5.0
```

### 5. 编译运行

```bash
# 编译
mvn clean package -DskipTests

# 运行
java -jar target/wechat-work-callback-1.0.0.jar

# 或直接运行
mvn spring-boot:run
```

看到以下日志表示启动成功：
```
===============================
企业微信回调服务启动成功！
===============================
```

### 6. 配置企业微信回调

1. 登录企业微信管理后台：https://work.weixin.qq.com/
2. 进入「应用管理」-「客户联系」
3. 配置「接收事件服务器」：
   - URL: `https://your-domain.com/api/wechat/callback`
   - Token: 与application.yml中的token一致
   - EncodingAESKey: 与application.yml中的encoding-aes-key一致
4. 点击「保存」（会自动验证URL）
5. 配置「企业客户事件」，勾选「添加企业客户事件」

### 7. 测试

1. 使用企业成员的企业微信生成联系方式二维码
2. 用个人微信扫码添加
3. 检查是否收到包含H5链接的欢迎消息

## 二、获取企业微信配置信息

### 2.1 获取企业ID（CorpID）

1. 登录企业微信管理后台
2. 进入「我的企业」-「企业信息」
3. 找到「企业ID」，复制

### 2.2 获取客户联系Secret

1. 进入「应用管理」-「客户联系」
2. 点击「客户联系」应用
3. 在「Secret」栏点击「查看」，复制Secret

### 2.3 生成Token和EncodingAESKey

1. 在「客户联系」应用页面
2. 找到「接收事件服务器」配置
3. 点击「设置接收事件服务器」
4. 填写URL（先填写一个临时地址，稍后修改）
5. 生成Token（随机字符串，建议32位）
6. 点击「随机生成」生成EncodingAESKey
7. **先不要保存**，等部署完服务后再保存

## 三、本地开发（使用内网穿透）

### 3.1 安装ngrok

```bash
# Mac
brew install ngrok

# 或下载：https://ngrok.com/download
```

### 3.2 启动内网穿透

```bash
# 启动本地服务（8080端口）
mvn spring-boot:run

# 新开终端，启动ngrok
ngrok http 8080
```

### 3.3 获取公网URL

ngrok会显示类似：
```
Forwarding  https://abc123.ngrok.io -> http://localhost:8080
```

### 3.4 配置企业微信

使用ngrok生成的HTTPS URL：
```
https://abc123.ngrok.io/api/wechat/callback
```

## 四、常见问题

### Q1: 启动失败，提示Redis连接失败

**解决方案：**
- 确认Redis服务已启动：`redis-cli ping` 应该返回 `PONG`
- 检查application.yml中的Redis配置

### Q2: URL验证失败

**解决方案：**
- 确认服务已启动并可以访问
- 检查Token和EncodingAESKey配置是否正确
- 检查CorpId配置是否正确
- 查看应用日志排查具体错误

### Q3: 收不到事件推送

**解决方案：**
- 确认URL验证已通过
- 确认事件订阅已配置（添加企业客户事件）
- 检查企业微信管理后台的推送记录
- 查看应用日志

### Q4: 发送消息失败

**解决方案：**
- 检查CorpId和ContactSecret配置
- 查看日志中的错误信息
- 确认Access Token获取成功

## 五、项目结构说明

```
wechat_demo/
├── src/main/java/com/company/wechat/
│   ├── WechatWorkCallbackApplication.java    # 主启动类
│   ├── config/                                # 配置类
│   ├── controller/                            # 控制器（回调接口）
│   ├── service/                               # 服务层
│   ├── util/                                  # 工具类
│   └── model/                                 # 数据模型
├── src/main/resources/
│   ├── application.yml                        # 应用配置
│   └── logback-spring.xml                     # 日志配置
├── scripts/                                   # 启动脚本
├── h5-example/                                # H5页面示例
├── pom.xml                                    # Maven配置
└── README.md                                  # 项目说明
```

## 六、下一步

- 📖 查看 [部署指南](deployment-guide.md) 了解生产环境部署
- 🧪 查看 [测试指南](API-TEST-GUIDE.md) 了解如何测试
- 📚 查看 [README.md](README.md) 了解项目详情

## 七、技术支持

如遇问题，请：
1. 查看日志文件：`logs/wechat-work-callback.log`
2. 查看错误日志：`logs/error.log`
3. 参考企业微信官方文档：https://work.weixin.qq.com/api/doc/

---

**祝使用愉快！** 🎉

