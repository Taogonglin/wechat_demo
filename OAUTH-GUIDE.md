# 企业微信OAuth身份识别方案使用指南

## 方案概述

本方案实现了**批量发送通用链接 + OAuth自动识别客户身份**的功能，无需在链接中暴露客户的external_userid。

## 工作流程

```
1. 管理员调用批量发送接口
   ↓
2. 所有客户收到相同的H5链接（不含external_id）
   ↓
3. 客户点击链接，打开H5页面
   ↓
4. H5页面自动跳转到企业微信OAuth授权
   ↓
5. 企业微信验证客户身份，返回code
   ↓
6. 后端通过code获取客户的external_userid
   ↓
7. 客户填写表单，提交信息
```

## 一、后端接口说明

### 1.1 批量发送接口

**接口地址：** `POST /api/customer/batch-send`

**请求参数：**
```json
{
  "staffUserId": "员工UserID"
}
```

**响应示例：**
```json
{
  "success": true,
  "msgid": "msgGCAAAXtWy...",
  "message": "群发任务创建成功"
}
```

**使用示例：**
```bash
curl -X POST "http://localhost:8080/api/customer/batch-send?staffUserId=ZhangSan"
```

### 1.2 按标签批量发送

**接口地址：** `POST /api/customer/batch-send-by-tag`

**请求参数：**
```json
{
  "staffUserId": "员工UserID",
  "tagIds": "标签ID1,标签ID2"
}
```

### 1.3 查询群发结果

**接口地址：** `GET /api/customer/batch-send-result`

**请求参数：**
```
msgid: 群发任务ID
```

**响应示例：**
```json
{
  "errcode": 0,
  "errmsg": "ok",
  "detail_list": [
    {
      "external_userid": "wmxxxx",
      "userid": "zhangsan",
      "status": 1
    }
  ]
}
```

### 1.4 获取客户列表

**接口地址：** `GET /api/customer/list`

**请求参数：**
```
staffUserId: 员工UserID
```

**响应示例：**
```json
{
  "success": true,
  "count": 10,
  "customers": ["wmxxx1", "wmxxx2", ...]
}
```

### 1.5 OAuth回调接口

**接口地址：** `GET /api/oauth/callback`

**说明：** 此接口由企业微信自动调用，无需手动请求。

**流程：**
1. 客户点击H5链接
2. H5跳转到企业微信OAuth授权页
3. 企业微信验证后携带code回调此接口
4. 后端获取客户身份，重定向到H5页面并携带external_userid

### 1.6 获取用户信息API

**接口地址：** `GET /api/oauth/userinfo`

**请求参数：**
```
code: OAuth授权码
```

**响应示例：**
```json
{
  "success": true,
  "userId": null,
  "externalUserId": "wmxxxx",
  "openId": null,
  "isExternalContact": true
}
```

## 二、前端集成说明

### 2.1 H5页面

已提供两个H5页面：

1. **oauth.html** - OAuth自动识别版本（推荐）
   - 自动跳转OAuth授权
   - 自动识别客户身份
   - 无需手动输入external_userid

2. **index.html** - 传统版本（兼容）
   - 需要URL中携带external_userid参数
   - 适用于直接推送个性化链接的场景

### 2.2 OAuth流程配置

在 `oauth.html` 中需要配置企业ID：

```javascript
const corpId = 'YOUR_CORP_ID'; // 替换为实际的企业ID
```

**或者** 调用后端API获取OAuth URL：

```javascript
const response = await fetch(`${API_BASE_URL}/api/oauth/auth-url?redirectUri=${currentUrl}`);
const data = await response.json();
window.location.href = data.authUrl;
```

### 2.3 配置回调域名

在企业微信管理后台配置可信域名：

1. 进入「应用管理」-「客户联系」
2. 找到「网页授权及JS-SDK」
3. 配置「可信域名」，例如：`your-domain.com`

## 三、部署配置

### 3.1 修改application.yml

```yaml
wechat:
  work:
    corp-id: ww1234567890abcdef
    contact-secret: YOUR_SECRET
    token: YOUR_TOKEN
    encoding-aes-key: YOUR_AES_KEY
    h5-base-url: https://your-domain.com/h5/oauth.html  # 使用oauth.html
```

### 3.2 Nginx配置

```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;

    # 后端API
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
    }

    # H5页面
    location /h5/ {
        root /var/www/html;
        try_files $uri $uri/ /h5/oauth.html;
    }
}
```

## 四、完整使用示例

### 场景1：给所有存量客户发送H5链接

```bash
# 1. 调用批量发送接口
curl -X POST "https://your-domain.com/api/customer/batch-send?staffUserId=ZhangSan"

# 响应
{
  "success": true,
  "msgid": "msgGCAAAXtWy...",
  "message": "群发任务创建成功"
}

# 2. 所有客户收到消息（包含H5链接）
# 链接格式：https://your-domain.com/h5/oauth.html?source=batch

# 3. 客户点击链接
# - 自动跳转到企业微信OAuth授权
# - 验证通过后自动获取external_userid
# - 显示表单让客户填写信息

# 4. 查询群发结果
curl "https://your-domain.com/api/customer/batch-send-result?msgid=msgGCAAAXtWy..."
```

### 场景2：给特定标签的客户发送

```bash
# 1. 先给客户打标签（在企业微信后台或通过API）

# 2. 按标签批量发送
curl -X POST "https://your-domain.com/api/customer/batch-send-by-tag?staffUserId=ZhangSan&tagIds=TAG_001,TAG_002"
```

### 场景3：查看某员工的客户列表

```bash
curl "https://your-domain.com/api/customer/list?staffUserId=ZhangSan"

# 响应
{
  "success": true,
  "count": 50,
  "customers": ["wmxxxx1", "wmxxxx2", ...]
}
```

## 五、OAuth授权说明

### 5.1 授权URL格式

```
https://open.weixin.qq.com/connect/oauth2/authorize?
  appid=CORPID
  &redirect_uri=REDIRECT_URI
  &response_type=code
  &scope=snsapi_privateinfo
  &state=STATE
  #wechat_redirect
```

**参数说明：**
- `appid`: 企业的CorpID
- `redirect_uri`: 授权后重定向的地址（需URL编码）
- `scope`: 
  - `snsapi_base`: 静默授权，只能获取成员UserID
  - `snsapi_privateinfo`: 手动授权，可获取成员详细信息和external_userid
- `state`: 状态参数（可选）

### 5.2 授权范围对比

| scope | 是否弹出授权页 | 能否获取external_userid | 适用场景 |
|-------|---------------|------------------------|----------|
| snsapi_base | 否 | 否 | 企业内部应用 |
| snsapi_privateinfo | 是 | 是 | 客户联系场景 |

### 5.3 获取用户信息

授权后，企业微信会携带code重定向到指定URL：

```
https://your-domain.com/h5/oauth.html?code=CODE&state=STATE
```

后端通过code获取用户信息：

```
GET https://qyapi.weixin.qq.com/cgi-bin/user/getuserinfo?
  access_token=ACCESS_TOKEN
  &code=CODE
```

**响应示例：**

```json
{
  "errcode": 0,
  "errmsg": "ok",
  "userid": "zhangsan",           // 企业成员
  "external_userid": "wmxxxx",     // 外部联系人
  "openid": "oxxxx"                // 非企业成员
}
```

## 六、常见问题

### Q1: 客户点击链接后没有跳转到OAuth授权页？

**原因：**
- 链接必须在企业微信内打开
- 可信域名未配置

**解决：**
1. 确认客户是通过企业微信打开链接
2. 在企业微信后台配置可信域名

### Q2: 获取不到external_userid？

**原因：**
- OAuth scope设置错误
- 客户未授权

**解决：**
1. 确保scope使用`snsapi_privateinfo`
2. 客户需要点击授权按钮

### Q3: 批量发送失败？

**原因：**
- 员工UserID不存在
- 没有客户联系权限
- Secret配置错误

**解决：**
1. 检查员工UserID是否正确
2. 确认应用有客户联系权限
3. 验证ContactSecret配置

### Q4: 群发消息客户收不到？

**原因：**
- 客户已删除员工
- 群发频率限制
- 消息内容违规

**解决：**
1. 确认客户关系正常
2. 注意群发频率限制（每月有限额）
3. 避免敏感内容

## 七、安全建议

1. **HTTPS**: 必须使用HTTPS，企业微信要求
2. **Token安全**: 不要在前端暴露AccessToken
3. **参数验证**: 后端要验证所有输入参数
4. **频率限制**: 添加接口调用频率限制
5. **日志审计**: 记录所有OAuth操作日志

## 八、性能优化

1. **Redis缓存**: Access Token已使用Redis缓存
2. **批量操作**: 使用群发API而不是逐个发送
3. **异步处理**: 大批量操作使用异步任务
4. **CDN加速**: H5页面使用CDN加速

## 九、监控指标

建议监控以下指标：

- 群发任务创建成功率
- OAuth授权成功率
- 客户信息提交成功率
- API响应时间
- Access Token获取失败次数

---

## 参考文档

- 企业微信OAuth文档: https://work.weixin.qq.com/api/doc/90000/90135/91020
- 客户联系API: https://work.weixin.qq.com/api/doc/90000/90135/92148
- 群发消息API: https://work.weixin.qq.com/api/doc/90000/90135/92135

如有问题，请查看日志或联系技术支持。

