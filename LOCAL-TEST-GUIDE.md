# 本地测试完整指南

## 准备工作

### 1. 环境检查

```bash
# 检查Java版本（需要Java 8+）
java -version

# 检查Maven（如果没有，可以用IDEA内置的Maven）
mvn -version

# 检查Redis是否运行
redis-cli ping
# 应该返回 PONG
```

### 2. 安装Redis（如果未安装）

**macOS:**
```bash
brew install redis
brew services start redis
```

**或使用Docker:**
```bash
docker run -d -p 6379:6379 redis:5.0
```

## 第一步：配置项目

### 1.1 配置企业微信信息

编辑 `src/main/resources/application.yml`：

```yaml
wechat:
  work:
    # 在企业微信管理后台获取
    corp-id: ww1234567890abcdef           # 你的企业ID
    contact-secret: YOUR_CONTACT_SECRET    # 客户联系Secret
    token: MyToken123                      # 自己设置（用于回调验证）
    encoding-aes-key: YOUR_43_CHAR_KEY     # 自己生成或企业微信生成
    h5-base-url: https://your-ngrok-url.ngrok.io/h5/oauth.html  # 先用临时的，后面会更新
```

### 1.2 如何获取企业微信配置信息

#### 获取企业ID（CorpID）
1. 登录：https://work.weixin.qq.com/
2. 进入「我的企业」→「企业信息」
3. 复制「企业ID」

#### 获取客户联系Secret
1. 进入「应用管理」→「客户联系」
2. 点击「客户联系」应用
3. 点击「查看Secret」，复制

#### 生成Token和EncodingAESKey（暂时先随便设置）
```
token: MyTestToken123
encoding-aes-key: 随便43个字符，例如：abcdefghijklmnopqrstuvwxyz01234567890ABC
```

## 第二步：启动项目

### 方式1：使用IDEA（推荐）

1. 用IDEA打开项目
2. 等待Maven依赖下载完成
3. 找到 `WechatWorkCallbackApplication.java`
4. 右键 → Run 'WechatWorkCallbackApplication'
5. 看到以下输出表示成功：
```
================================
企业微信回调服务启动成功！
================================
```

### 方式2：使用命令行

```bash
cd /Users/taogonglin/projects/wechat_demo

# 如果有Maven
mvn spring-boot:run

# 或者先编译再运行
mvn clean package -DskipTests
java -jar target/wechat-work-callback-1.0.0.jar
```

### 启动成功验证

打开浏览器访问：
```
http://localhost:8080/api/wechat/health
```
应该返回：`OK`

## 第三步：设置内网穿透

由于企业微信需要访问公网URL，我们需要使用内网穿透工具。

### 方式1：使用ngrok（推荐）

#### 3.1 安装ngrok

**macOS:**
```bash
brew install ngrok
```

**或直接下载：**
https://ngrok.com/download

#### 3.2 启动ngrok

```bash
# 在新终端窗口运行
ngrok http 8080
```

你会看到类似输出：
```
ngrok

Session Status                online
Account                       your-account (Plan: Free)
Version                       3.x.x
Region                        United States (us)
Latency                       -
Web Interface                 http://127.0.0.1:4040
Forwarding                    https://abc123.ngrok.io -> http://localhost:8080

Connections                   ttl     opn     rt1     rt5     p50     p90
                              0       0       0.00    0.00    0.00    0.00
```

**重要：复制 `https://abc123.ngrok.io` 这个URL，这是你的公网地址！**

#### 3.3 更新H5配置

编辑 `src/main/resources/application.yml`，更新：
```yaml
wechat:
  work:
    h5-base-url: https://abc123.ngrok.io/h5/oauth.html  # 使用ngrok的URL
```

重启Spring Boot应用。

### 方式2：使用natapp（国内推荐）

1. 注册：https://natapp.cn/
2. 下载客户端
3. 获取免费隧道的authtoken
4. 运行：
```bash
./natapp -authtoken=你的token
```

### 验证内网穿透

在浏览器访问：
```
https://abc123.ngrok.io/api/wechat/health
```
应该返回：`OK`

## 第四步：配置企业微信回调

### 4.1 配置接收事件服务器

1. 登录企业微信管理后台
2. 进入「应用管理」→「客户联系」
3. 找到「接收事件服务器」配置
4. 点击「设置接收事件服务器」

填写以下信息：
```
URL: https://abc123.ngrok.io/api/wechat/callback
Token: MyTestToken123  (与application.yml中一致)
EncodingAESKey: (点击"随机生成"，然后复制到application.yml中)
```

5. **重要：** 复制生成的EncodingAESKey到application.yml中
6. **重启Spring Boot应用**
7. 返回企业微信后台，点击「保存」

**如果验证失败：**
- 检查URL是否正确
- 检查Token和EncodingAESKey是否一致
- 查看Spring Boot日志
- 确认ngrok正在运行

### 4.2 配置事件订阅

在「企业客户事件」中勾选：
- ✅ 添加企业客户事件
- ✅ 编辑企业客户事件
- ✅ 删除企业客户事件（可选）

点击「保存」。

### 4.3 配置可信域名（用于OAuth）

1. 在「客户联系」应用页面
2. 找到「网页授权及JS-SDK」
3. 配置「可信域名」：`abc123.ngrok.io`（不要加https://）
4. 下载验证文件，放到H5目录下（ngrok会自动转发）

## 第五步：测试新客户添加功能

### 5.1 生成联系方式二维码

1. 在企业微信管理后台
2. 进入「客户联系」→「联系我」
3. 创建一个「联系我」，生成二维码

### 5.2 测试流程

```
1. 用个人微信扫描二维码
   ↓
2. 添加企业微信客服
   ↓
3. 查看Spring Boot日志，应该看到：
   - 收到回调消息
   - 解密成功
   - 检测到客户添加事件
   - 发送欢迎语成功
   ↓
4. 在企业微信中查看是否收到欢迎消息
   ↓
5. 点击H5链接，测试表单填写
```

### 5.3 查看日志

**Spring Boot日志：**
```
收到回调消息 - Signature: xxx, Timestamp: xxx
解密后的消息内容: <xml>...</xml>
处理消息 - 类型: event, 事件: change_external_contact
检测到客户添加事件
收到客户添加事件 - External UserID: wmxxxxxx
生成H5链接: https://abc123.ngrok.io/h5/oauth.html?external_userid=wmxxxxxx
发送欢迎语给客户: welcomeCode=xxx
成功发送H5链接给客户: wmxxxxxx
```

## 第六步：测试存量客户批量发送（OAuth方案）

### 6.1 获取员工UserID

在企业微信管理后台：
1. 进入「通讯录」
2. 找到员工
3. 查看详情，复制「账号」（UserID）

### 6.2 查看客户列表

```bash
curl "http://localhost:8080/api/customer/list?staffUserId=YourStaffUserId"
```

应该返回：
```json
{
  "success": true,
  "count": 5,
  "customers": ["wmxxx1", "wmxxx2", ...]
}
```

### 6.3 批量发送测试

```bash
curl -X POST "http://localhost:8080/api/customer/batch-send?staffUserId=YourStaffUserId"
```

应该返回：
```json
{
  "success": true,
  "msgid": "msgGCAAAXtWy...",
  "message": "群发任务创建成功"
}
```

### 6.4 客户端测试

1. 在企业微信中查看是否收到群发消息
2. 点击H5链接
3. 应该自动跳转到OAuth授权页面
4. 点击「确认」授权
5. 自动跳转回H5页面，显示表单
6. 页面应该自动显示你的external_userid（无需手动输入）

### 6.5 查看群发结果

```bash
# 使用返回的msgid查询
curl "http://localhost:8080/api/customer/batch-send-result?msgid=返回的msgid"
```

## 第七步：测试OAuth认证流程

### 7.1 直接访问H5页面

在企业微信中访问：
```
https://abc123.ngrok.io/h5/oauth.html
```

### 7.2 观察流程

1. 页面显示"身份验证中"
2. 自动跳转到企业微信OAuth授权页面
3. 点击"确认"授权
4. 返回H5页面，自动识别身份
5. 显示客户信息和表单

### 7.3 查看日志

```
通过code获取用户信息: code=xxx
获取用户信息成功: externalUserId=wmxxxx
识别为外部联系人，重定向到H5页面
```

## 常见问题排查

### 问题1：启动失败，端口占用

```bash
# 查看8080端口占用
lsof -i :8080

# 杀死进程
kill -9 <PID>

# 或修改端口
# 在application.yml中修改：
server:
  port: 8081
```

### 问题2：Redis连接失败

```bash
# 启动Redis
redis-server

# 或使用Docker
docker start redis
```

### 问题3：URL验证失败

**原因：**
- Token或EncodingAESKey不一致
- CorpId错误
- ngrok未运行

**解决：**
1. 检查配置是否一致
2. 查看Spring Boot日志
3. 确认ngrok正在运行

### 问题4：收不到事件推送

**检查清单：**
- [ ] URL验证是否通过
- [ ] 事件订阅是否勾选
- [ ] 客户是否真的添加成功
- [ ] 查看企业微信后台的推送记录

### 问题5：OAuth授权失败

**原因：**
- 可信域名未配置
- 链接不是在企业微信中打开
- CorpId错误

**解决：**
1. 配置可信域名：`abc123.ngrok.io`
2. 确保在企业微信中打开链接
3. 检查CorpId配置

## 完整测试流程总结

### 新客户自动欢迎测试
```
1. 启动Redis
2. 启动Spring Boot
3. 启动ngrok
4. 配置企业微信回调
5. 生成二维码
6. 用个人微信添加
7. 查看是否收到欢迎消息
8. 点击链接测试
```

### 存量客户批量发送测试
```
1. 确保服务运行正常
2. 配置OAuth可信域名
3. 调用批量发送API
4. 在企业微信查看消息
5. 点击链接测试OAuth
6. 查看是否自动识别身份
```

## 调试技巧

### 1. 查看实时日志

```bash
# 如果用java -jar运行
tail -f logs/wechat-work-callback.log

# 如果用IDEA运行
# 直接在IDEA控制台查看
```

### 2. 查看ngrok请求

访问：http://127.0.0.1:4040
可以看到所有通过ngrok的HTTP请求

### 3. 测试企业微信API

```bash
# 测试获取Access Token
curl "http://localhost:8080/api/customer/list?staffUserId=test"
# 查看日志中是否成功获取token
```

### 4. Redis调试

```bash
# 查看缓存的token
redis-cli
> GET wechat:work:access_token
> TTL wechat:work:access_token  # 查看过期时间
```

## 测试脚本

创建 `test.sh` 方便测试：

```bash
#!/bin/bash

# 配置
STAFF_USER_ID="YourStaffUserId"
API_BASE="http://localhost:8080"

echo "====== 企业微信本地测试 ======"

# 1. 健康检查
echo "\n1. 健康检查..."
curl -s "${API_BASE}/api/wechat/health"

# 2. 获取客户列表
echo "\n\n2. 获取客户列表..."
curl -s "${API_BASE}/api/customer/list?staffUserId=${STAFF_USER_ID}" | jq '.'

# 3. 批量发送
echo "\n\n3. 批量发送测试..."
RESULT=$(curl -s -X POST "${API_BASE}/api/customer/batch-send?staffUserId=${STAFF_USER_ID}")
echo $RESULT | jq '.'

# 提取msgid
MSGID=$(echo $RESULT | jq -r '.msgid')

if [ "$MSGID" != "null" ]; then
    echo "\n\n4. 等待5秒后查询结果..."
    sleep 5
    curl -s "${API_BASE}/api/customer/batch-send-result?msgid=${MSGID}" | jq '.'
fi

echo "\n\n====== 测试完成 ======"
```

使用：
```bash
chmod +x test.sh
./test.sh
```

## 下一步

测试成功后：
1. 查看 [OAUTH-GUIDE.md](OAUTH-GUIDE.md) 了解详细的OAuth流程
2. 查看 [BATCH-SEND-EXAMPLE.md](BATCH-SEND-EXAMPLE.md) 学习更多示例
3. 准备部署到生产环境（参考 [deployment-guide.md](deployment-guide.md)）

---

**祝测试顺利！** 🎉

如有问题，请查看日志或参考文档。

