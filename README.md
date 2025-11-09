# 企业微信客户添加事件监听服务

## 项目简介
本项目实现了企业微信客户联系功能，当客户首次添加企业成员为好友时，自动向客户发送包含客户标识的H5链接。

## 功能特性
- ✅ 监听企业微信客户添加事件
- ✅ 获取客户external_id
- ✅ 自动发送带参数的H5链接给客户
- ✅ 企业微信回调签名验证
- ✅ Access Token自动管理和缓存
- ✅ 完整的日志记录

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
http://your-domain.com/api/wechat/callback
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

### 回调接口
- **GET** `/api/wechat/callback` - 验证回调URL
- **POST** `/api/wechat/callback` - 接收事件回调

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

## 联系方式
如有问题，请联系开发团队。

