# 企业微信回调API测试指南

## 一、本地开发测试

### 1.1 使用内网穿透工具

由于企业微信需要访问公网URL，本地开发时需要使用内网穿透工具。

#### 推荐工具：
- **ngrok**：https://ngrok.com/
- **natapp**：https://natapp.cn/
- **花生壳**：https://hsk.oray.com/

#### ngrok使用示例：

```bash
# 安装ngrok
# 下载：https://ngrok.com/download

# 启动内网穿透（假设本地服务运行在8080端口）
ngrok http 8080

# 会得到一个公网URL，例如：
# https://abc123.ngrok.io
```

### 1.2 配置企业微信测试回调

1. 使用ngrok生成的HTTPS URL配置回调地址：
   ```
   https://abc123.ngrok.io/api/wechat/callback
   ```

2. 在企业微信后台保存配置，验证URL是否可访问

## 二、接口测试

### 2.1 健康检查接口

```bash
# 测试服务是否正常运行
curl https://your-domain.com/api/wechat/health

# 预期响应
OK
```

### 2.2 模拟URL验证（GET请求）

企业微信在配置回调URL时会发送GET请求验证：

```bash
curl -X GET "https://your-domain.com/api/wechat/callback?msg_signature=SIGNATURE&timestamp=1234567890&nonce=NONCE&echostr=ENCRYPTED_ECHOSTR"
```

**参数说明：**
- `msg_signature`: 消息签名
- `timestamp`: 时间戳
- `nonce`: 随机数
- `echostr`: 加密的随机字符串

**预期响应：**
返回解密后的随机字符串

### 2.3 模拟客户添加事件（POST请求）

企业微信会发送POST请求推送事件：

```bash
curl -X POST "https://your-domain.com/api/wechat/callback?msg_signature=SIGNATURE&timestamp=1234567890&nonce=NONCE" \
  -H "Content-Type: text/xml" \
  -d '<xml>
    <ToUserName><![CDATA[ww1234567890abcdef]]></ToUserName>
    <Encrypt><![CDATA[ENCRYPTED_CONTENT]]></Encrypt>
  </xml>'
```

**预期响应：**
```
success
```

## 三、完整测试流程

### 3.1 准备工作

1. **启动Redis服务**
```bash
redis-server
# 或
systemctl start redis
```

2. **启动Spring Boot应用**
```bash
mvn spring-boot:run
# 或
java -jar target/wechat-work-callback-1.0.0.jar
```

3. **启动内网穿透（本地开发）**
```bash
ngrok http 8080
```

4. **记录生成的公网URL**
```
例如：https://abc123.ngrok.io
```

### 3.2 配置企业微信

1. 登录企业微信管理后台：https://work.weixin.qq.com/

2. 进入「应用管理」-「客户联系」

3. 配置「接收事件服务器」：
   - URL: `https://abc123.ngrok.io/api/wechat/callback`
   - Token: 与application.yml中的token一致
   - EncodingAESKey: 与application.yml中的encoding-aes-key一致

4. 点击保存，系统会自动验证URL

5. 配置「企业客户事件」，勾选：
   - ✅ 添加企业客户事件

### 3.3 实际测试

#### 方式1：通过企业微信客户端测试

1. **生成联系方式二维码**
   - 在企业微信管理后台
   - 进入「客户联系」-「联系我」
   - 创建一个二维码

2. **扫码添加**
   - 使用个人微信扫描二维码
   - 添加企业微信好友

3. **查看结果**
   - 检查是否收到包含H5链接的欢迎消息
   - 点击链接，查看是否正确传递了external_userid参数

4. **查看日志**
```bash
# 查看应用日志
tail -f logs/wechat-work-callback.log

# 应该看到类似日志：
# 收到回调消息 - Signature: xxx, Timestamp: xxx
# 解密后的消息内容: <xml>...</xml>
# 处理消息 - 类型: event, 事件: change_external_contact
# 检测到客户添加事件
# 收到客户添加事件 - External UserID: wmxxxxxxxxxxxxxx
# 生成H5链接: https://your-domain.com/h5/customer?external_userid=wmxxxxxxxxxxxxxx
# 发送消息给客户: wmxxxxxxxxxxxxxx
# 成功发送H5链接给客户: wmxxxxxxxxxxxxxx
```

#### 方式2：查看Redis缓存

```bash
# 连接Redis
redis-cli

# 查看Access Token
GET wechat:work:access_token

# 查看Token过期时间（秒）
TTL wechat:work:access_token
```

### 3.4 验证H5页面

1. 复制收到的H5链接
2. 在浏览器中打开
3. 检查页面是否正确显示external_userid
4. 测试表单提交功能

示例链接：
```
https://your-domain.com/h5/customer?external_userid=wmxxxxxxxxxxxxxx
```

## 四、常见测试场景

### 4.1 测试Access Token获取

查看日志确认Access Token是否成功获取和缓存：

```bash
# 应该看到：
# 获取Access Token成功，已缓存，过期时间: 7000秒
```

### 4.2 测试签名验证

故意修改Token配置，测试签名验证是否生效：

```yaml
wechat:
  work:
    token: wrong_token  # 错误的token
```

保存配置后，企业微信推送事件应该验证失败，日志显示：
```
回调消息签名验证失败
```

### 4.3 测试消息发送失败

故意配置错误的Secret，测试错误处理：

```yaml
wechat:
  work:
    contact-secret: wrong_secret
```

日志应该显示：
```
获取Access Token失败: invalid secret
```

### 4.4 测试Redis连接失败

停止Redis服务，测试应用的容错能力：

```bash
systemctl stop redis
# 或
redis-cli shutdown
```

应用应该抛出Redis连接异常，但不会崩溃。

## 五、调试技巧

### 5.1 开启详细日志

修改application.yml：

```yaml
logging:
  level:
    com.company.wechat: debug  # 开启DEBUG日志
    root: debug
```

### 5.2 使用Postman测试

由于企业微信的消息是加密的，直接用Postman测试比较困难。但可以测试健康检查接口：

```
GET https://your-domain.com/api/wechat/health
```

### 5.3 查看企业微信推送日志

在企业微信管理后台可以查看事件推送记录：
1. 进入「应用管理」-「客户联系」
2. 找到「接收事件服务器」配置
3. 点击「查看推送记录」

### 5.4 模拟企业微信推送

如果需要模拟企业微信的事件推送，需要：
1. 了解企业微信的加密算法
2. 构造符合格式的XML消息
3. 使用正确的Token和EncodingAESKey进行加密
4. 计算正确的签名

这个过程比较复杂，建议直接使用真实的企业微信环境测试。

## 六、性能测试

### 6.1 并发测试

使用Apache Bench测试并发性能：

```bash
# 测试健康检查接口（1000个请求，100并发）
ab -n 1000 -c 100 https://your-domain.com/api/wechat/health
```

### 6.2 压力测试

使用JMeter或Gatling进行压力测试，模拟大量客户同时添加的场景。

## 七、问题排查清单

### 7.1 URL验证失败

- [ ] 检查服务是否正常运行
- [ ] 检查Token和EncodingAESKey配置是否正确
- [ ] 检查CorpId配置是否正确
- [ ] 检查网络是否可达（内网穿透是否正常）
- [ ] 查看应用日志，确认错误原因

### 7.2 收不到事件推送

- [ ] 确认URL验证已通过
- [ ] 确认事件订阅已配置
- [ ] 检查企业微信管理后台的推送记录
- [ ] 查看应用日志，确认是否收到请求
- [ ] 检查防火墙规则

### 7.3 消息发送失败

- [ ] 确认CorpId和ContactSecret配置正确
- [ ] 确认Access Token获取成功
- [ ] 检查企业微信API返回的错误码
- [ ] 确认客户确实添加了企业微信
- [ ] 查看发送消息的日志

### 7.4 Redis连接失败

- [ ] 确认Redis服务正在运行
- [ ] 检查Redis连接配置（host、port、password）
- [ ] 测试Redis连接：`redis-cli ping`
- [ ] 检查防火墙规则

## 八、企业微信API错误码参考

| 错误码 | 说明 | 解决方案 |
|--------|------|----------|
| 0 | 成功 | - |
| 40001 | 不合法的secret | 检查secret配置 |
| 40013 | 不合法的corpid | 检查corpid配置 |
| 40014 | 不合法的access_token | 重新获取access_token |
| 42001 | access_token已过期 | 清除缓存，重新获取 |
| 84061 | 不合法的外部联系人userid | 检查external_userid |
| 90001 | 未认证摇一摇周边 | - |
| 90002 | 未开启摇一摇周边 | - |

完整错误码列表：https://work.weixin.qq.com/api/doc/90000/90139/90313

## 九、测试报告模板

测试完成后，建议记录以下信息：

```markdown
# 测试报告

## 测试环境
- 服务器：xxx
- Java版本：1.8
- Redis版本：5.0
- 测试时间：2024-xx-xx

## 测试结果

### 1. URL验证
- 状态：✅ 通过
- 响应时间：100ms

### 2. 事件接收
- 状态：✅ 通过
- 测试次数：10次
- 成功次数：10次

### 3. 消息发送
- 状态：✅ 通过
- 测试次数：10次
- 成功次数：10次
- 平均响应时间：500ms

### 4. H5页面
- 状态：✅ 通过
- external_userid传递：正常
- 页面加载：正常

## 发现的问题
无

## 建议
无
```

---

如有其他测试需求，请参考企业微信官方文档或联系技术支持。

