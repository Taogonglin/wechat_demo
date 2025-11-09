# 批量发送使用示例

## 快速开始

### 1. 准备工作

确保已完成以下配置：
- ✅ 企业微信配置已填写
- ✅ H5页面已部署
- ✅ 可信域名已配置
- ✅ 服务已启动

### 2. 获取员工UserID

在企业微信管理后台获取员工的UserID：
```
管理后台 -> 通讯录 -> 找到员工 -> 查看详情 -> UserID
```

### 3. 调用批量发送接口

```bash
# 方式1：使用curl
curl -X POST "http://localhost:8080/api/customer/batch-send?staffUserId=ZhangSan"

# 方式2：使用Postman
POST http://localhost:8080/api/customer/batch-send
参数：staffUserId=ZhangSan
```

### 4. 查看结果

```bash
# 查询群发结果
curl "http://localhost:8080/api/customer/batch-send-result?msgid=返回的msgid"
```

## 完整示例代码

### Java示例

```java
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.HashMap;
import java.util.Map;

public class BatchSendExample {
    
    public static void main(String[] args) {
        String apiUrl = "http://localhost:8080/api/customer/batch-send";
        String staffUserId = "ZhangSan";
        
        RestTemplate restTemplate = new RestTemplate();
        String url = apiUrl + "?staffUserId=" + staffUserId;
        
        ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);
        Map<String, Object> result = response.getBody();
        
        if ((Boolean) result.get("success")) {
            String msgid = (String) result.get("msgid");
            System.out.println("群发任务创建成功，msgid: " + msgid);
        } else {
            System.out.println("群发任务创建失败");
        }
    }
}
```

### Python示例

```python
import requests

def batch_send(staff_user_id):
    api_url = "http://localhost:8080/api/customer/batch-send"
    params = {"staffUserId": staff_user_id}
    
    response = requests.post(api_url, params=params)
    result = response.json()
    
    if result.get('success'):
        msgid = result.get('msgid')
        print(f"群发任务创建成功，msgid: {msgid}")
        return msgid
    else:
        print(f"群发任务创建失败: {result.get('message')}")
        return None

def check_result(msgid):
    api_url = "http://localhost:8080/api/customer/batch-send-result"
    params = {"msgid": msgid}
    
    response = requests.get(api_url, params=params)
    result = response.json()
    
    print(f"群发结果: {result}")
    return result

# 使用示例
if __name__ == "__main__":
    # 批量发送
    msgid = batch_send("ZhangSan")
    
    # 查询结果
    if msgid:
        import time
        time.sleep(5)  # 等待5秒
        check_result(msgid)
```

### JavaScript示例

```javascript
// 批量发送
async function batchSend(staffUserId) {
    const apiUrl = 'http://localhost:8080/api/customer/batch-send';
    const params = new URLSearchParams({ staffUserId });
    
    try {
        const response = await fetch(`${apiUrl}?${params}`, {
            method: 'POST'
        });
        const result = await response.json();
        
        if (result.success) {
            console.log('群发任务创建成功，msgid:', result.msgid);
            return result.msgid;
        } else {
            console.log('群发任务创建失败:', result.message);
            return null;
        }
    } catch (error) {
        console.error('请求失败:', error);
        return null;
    }
}

// 查询结果
async function checkResult(msgid) {
    const apiUrl = 'http://localhost:8080/api/customer/batch-send-result';
    const params = new URLSearchParams({ msgid });
    
    try {
        const response = await fetch(`${apiUrl}?${params}`);
        const result = await response.json();
        console.log('群发结果:', result);
        return result;
    } catch (error) {
        console.error('查询失败:', error);
        return null;
    }
}

// 使用示例
(async () => {
    const msgid = await batchSend('ZhangSan');
    if (msgid) {
        // 等待5秒后查询结果
        await new Promise(resolve => setTimeout(resolve, 5000));
        await checkResult(msgid);
    }
})();
```

## 实际场景示例

### 场景1：定期给所有客户发送问候

```bash
#!/bin/bash
# daily_greeting.sh

# 配置
API_URL="http://localhost:8080/api/customer/batch-send"
STAFF_USER_ID="ZhangSan"

# 发送
echo "开始发送每日问候..."
RESULT=$(curl -s -X POST "${API_URL}?staffUserId=${STAFF_USER_ID}")

# 解析结果
MSGID=$(echo $RESULT | jq -r '.msgid')

if [ "$MSGID" != "null" ]; then
    echo "发送成功，msgid: $MSGID"
    
    # 5秒后查询结果
    sleep 5
    QUERY_RESULT=$(curl -s "http://localhost:8080/api/customer/batch-send-result?msgid=$MSGID")
    echo "群发结果: $QUERY_RESULT"
else
    echo "发送失败"
fi
```

### 场景2：给新客户标签的客户发送欢迎消息

```bash
#!/bin/bash
# welcome_new_customers.sh

# 配置
API_URL="http://localhost:8080/api/customer/batch-send-by-tag"
STAFF_USER_ID="ZhangSan"
TAG_ID="新客户标签ID"

# 发送
curl -X POST "${API_URL}?staffUserId=${STAFF_USER_ID}&tagIds=${TAG_ID}"
```

### 场景3：批量发送前先查看客户数量

```bash
#!/bin/bash
# check_and_send.sh

STAFF_USER_ID="ZhangSan"

# 1. 先查看客户数量
LIST_RESULT=$(curl -s "http://localhost:8080/api/customer/list?staffUserId=${STAFF_USER_ID}")
COUNT=$(echo $LIST_RESULT | jq -r '.count')

echo "该员工共有 $COUNT 个客户"

# 2. 确认后发送
read -p "确认发送？(y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    curl -X POST "http://localhost:8080/api/customer/batch-send?staffUserId=${STAFF_USER_ID}"
fi
```

## 常见使用场景

### 1. 活动通知

```
场景：通知所有客户参加活动
操作：批量发送 -> 客户点击链接 -> 自动识别身份 -> 填写报名信息
```

### 2. 客户信息收集

```
场景：收集客户的详细信息
操作：批量发送 -> 客户点击链接 -> OAuth识别 -> 填写表单 -> 后端保存
```

### 3. 满意度调查

```
场景：定期发送满意度调查
操作：定时任务 -> 批量发送 -> 客户填写 -> 统计分析
```

### 4. 新品推广

```
场景：向目标客户推广新产品
操作：按标签筛选 -> 批量发送 -> 客户查看 -> 意向收集
```

## 注意事项

### 1. 频率限制

- 每个员工每天最多给同一客户发送20条消息
- 群发功能每月有次数限制（具体看企业权限）

### 2. 消息内容

- 避免敏感词汇
- 内容不宜过长（建议不超过500字）
- 链接必须是https

### 3. 最佳实践

- 选择合适的时间发送（工作时间，避免打扰）
- 消息内容要有价值，避免频繁骚扰
- 定期清理无效客户
- 监控发送结果，及时调整策略

### 4. 错误处理

常见错误码：
- `40003`: 不合法的UserID
- `84061`: 不合法的外部联系人
- `90207`: 群发限制（超过频率）

## 监控和统计

### 实时监控脚本

```bash
#!/bin/bash
# monitor.sh

MSGID=$1

if [ -z "$MSGID" ]; then
    echo "用法: ./monitor.sh <msgid>"
    exit 1
fi

echo "开始监控群发结果..."

while true; do
    RESULT=$(curl -s "http://localhost:8080/api/customer/batch-send-result?msgid=$MSGID")
    
    # 解析统计
    echo "========================================"
    echo "时间: $(date)"
    echo $RESULT | jq '.'
    
    sleep 10  # 每10秒查询一次
done
```

## 更多示例

查看 `OAUTH-GUIDE.md` 了解完整的OAuth认证流程。

---

如有问题，请联系技术支持或查看日志。

