# 企业微信客户添加事件监听服务

## 项目简介
本项目实现了企业微信客户联系功能，支持两种方案：
1. **新客户自动识别**：当客户首次添加企业成员时，自动发送欢迎消息
2. **存量客户批量触达**：通过OAuth自动识别客户身份，批量发送H5链接

## 功能特性

### 核心功能
- ✅ 监听企业微信客户添加事件
- ✅ 获取客户external_id
- ✅ 自动发送带参数的H5链接给新客户
- ✅ 企业微信回调签名验证
- ✅ Access Token自动管理和缓存
- ✅ 完整的日志记录

### ⭐ 新增功能（存量客户方案）
- ✅ 批量发送消息给所有存量客户
- ✅ OAuth自动识别客户身份（无需URL携带ID）
- ✅ 按标签筛选客户批量发送
- ✅ 群发结果查询和统计
- ✅ 安全的身份认证流程

## 技术栈
- Spring Boot 2.7.18
- Java 8
- Redis（用于token缓存）
- OkHttp（HTTP客户端）
- Gson（JSON处理）

## 快速开始

### 1. 配置企业微信
在 `src/main/resources/application.yml` 中配置：
```yaml
wechat:
  work:
    corp-id: 你的企业ID
    contact-secret: 客户联系Secret
    token: 回调Token
    encoding-aes-key: 回调EncodingAESKey
    h5-base-url: 你的H5页面地址
```

### 2. 启动Redis
```bash
redis-server
```

### 3. 运行项目
```bash
mvn spring-boot:run
```

### 4. 配置企业微信回调
在企业微信管理后台配置回调URL：
```
http://47.108.150.198:8080/api/wechat/callback
```

## 项目结构
```
src/main/java/com/company/wechat/
├── WechatWorkCallbackApplication.java    # 主启动类
├── config/                                # 配置类
│   ├── WechatWorkConfig.java             # 企业微信配置
│   └── RedisConfig.java                  # Redis配置
├── controller/                            # 控制器
│   └── WechatCallbackController.java     # 回调接口
├── service/                               # 服务层
│   ├── WechatCallbackService.java        # 回调处理服务
│   ├── WechatApiService.java             # 企业微信API调用
│   └── CustomerEventService.java         # 客户事件处理
├── util/                                  # 工具类
│   ├── WechatSignUtil.java               # 签名验证工具
│   ├── AesUtil.java                      # AES加解密工具
│   └── HttpUtil.java                     # HTTP请求工具
└── model/                                 # 数据模型
    ├── dto/                               # DTO对象
    │   ├── CallbackMessage.java          # 回调消息
    │   └── CustomerAddEvent.java         # 客户添加事件
    └── vo/                                # VO对象
        └── WechatResponse.java            # 企业微信API响应

```

## API说明

### 回调接口（新客户）
- **GET** `/api/wechat/callback` - 验证回调URL
- **POST** `/api/wechat/callback` - 接收事件回调

### 批量发送接口（存量客户）⭐
- **POST** `/api/customer/batch-send` - 批量发送给所有客户
- **POST** `/api/customer/batch-send-by-tag` - 按标签批量发送
- **GET** `/api/customer/batch-send-result` - 查询群发结果
- **GET** `/api/customer/list` - 获取客户列表

### OAuth认证接口⭐
- **GET** `/api/oauth/callback` - OAuth回调接口
- **GET** `/api/oauth/userinfo` - 获取用户信息
- **GET** `/api/oauth/auth-url` - 生成OAuth授权URL

### 事件类型
支持的客户事件类型：
- `change_external_contact` - 客户添加/删除事件
  - `add_external_contact` - 添加客户
  - `del_external_contact` - 删除客户

## 开发注意事项

1. **安全性**
   - 所有回调消息都经过签名验证
   - 消息内容使用AES加密传输

2. **Token管理**
   - Access Token自动获取和刷新
   - 使用Redis缓存，避免频繁请求

3. **错误处理**
   - 完整的异常处理机制
   - 详细的日志记录

## 部署建议

1. 生产环境请使用HTTPS
2. 配置合理的Redis持久化策略
3. 定期检查日志，监控系统运行状态
4. 建议使用Nginx做反向代理

## 两种方案对比

| 特性 | 新客户方案 | 存量客户方案 ⭐ |
|------|-----------|----------------|
| 触发方式 | 客户添加事件 | 管理员主动发送 |
| 发送方式 | 自动发送 | 批量群发 |
| 链接类型 | 带external_id | 通用链接 |
| 身份识别 | URL参数 | OAuth认证 |
| 适用场景 | 新客户欢迎 | 存量客户触达 |
| 效率 | 实时 | 批量高效 |

## 快速示例

### 场景1：新客户自动欢迎（自动触发）
```
客户添加企业微信 
  ↓
自动发送欢迎消息（含H5链接）
  ↓
客户点击填写信息
```

### 场景2：存量客户批量发送（手动触发）⭐
```bash
# 批量发送给所有客户
curl -X POST "http://localhost:8080/api/customer/batch-send?staffUserId=ZhangSan"

# 客户收到消息，点击链接
# OAuth自动识别身份
# 无需手动输入external_id
```

## 详细文档

- 📖 [QUICK-START.md](QUICK-START.md) - 快速开始指南
- 📖 [OAUTH-GUIDE.md](OAUTH-GUIDE.md) - OAuth身份识别完整指南 ⭐
- 📖 [BATCH-SEND-EXAMPLE.md](BATCH-SEND-EXAMPLE.md) - 批量发送示例代码 ⭐
- 📖 [OAUTH-IMPLEMENTATION-SUMMARY.md](OAUTH-IMPLEMENTATION-SUMMARY.md) - 实现总结 ⭐
- 📖 [deployment-guide.md](deployment-guide.md) - 部署指南
- 📖 [API-TEST-GUIDE.md](API-TEST-GUIDE.md) - 测试指南

## 联系方式
如有问题，请联系开发团队或查看详细文档。

