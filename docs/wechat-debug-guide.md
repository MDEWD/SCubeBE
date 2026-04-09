# 微信扫码登录调试指南

## 问题：扫码后 checkTicket 返回 scanned=false

### 排查步骤

#### 1. 检查微信回调是否触发

查看控制台日志，应该能看到：
```
========== 收到微信回调 ==========
XML数据: <xml>...</xml>
解析结果 - msgType: event, event: subscribe/SCAN, eventKey: ...
```

**如果没有看到这些日志：**
- 检查内网穿透是否正常运行
- 检查测试号后台的URL配置是否正确
- 检查 `/api/wechat/callback` 接口是否可访问

#### 2. 检查事件类型

**首次关注（subscribe事件）：**
- EventKey格式：`qrscene_12345`
- 需要从EventKey中提取sceneId：`12345`

**已关注用户扫码（SCAN事件）：**
- EventKey格式：`12345`（直接是sceneId）
- 直接使用EventKey作为sceneId

#### 3. 检查Redis存储

**生成二维码时：**
- 存储 `wechat:qr:scene:{sceneId}` -> `{ticket}`
- 存储 `wechat:qr:ticket:{ticket}` -> `pending`

**扫码后：**
- 应该更新 `wechat:qr:ticket:{ticket}` -> `{openId}`

**检查方法：**
```bash
# 连接Redis
redis-cli

# 查看所有相关key
KEYS wechat:qr:*

# 查看具体值
GET wechat:qr:scene:12345
GET wechat:qr:ticket:xxx
```

#### 4. 常见问题

**问题1：EventKey格式不对**

**现象：** 日志显示 `EventKey格式不正确或为空`

**原因：** 
- 不是通过扫码触发的（可能是直接关注）
- EventKey解析失败

**解决：**
- 确保是通过扫描二维码关注/扫码
- 检查EventKey的值

**问题2：找不到对应的Ticket**

**现象：** 日志显示 `警告: 未找到对应的Ticket，sceneId: xxx`

**原因：**
- sceneId和Ticket的映射已过期（5分钟）
- sceneId不匹配

**解决：**
- 检查Redis中是否存在 `wechat:qr:scene:{sceneId}`
- 确保在二维码有效期内扫码（5分钟）

**问题3：微信回调没有触发**

**现象：** 完全没有日志输出

**原因：**
- 内网穿透未配置或已断开
- 测试号URL配置错误
- 服务器未启动

**解决：**
1. 检查内网穿透是否运行
2. 在浏览器访问回调URL测试
3. 检查服务器日志

#### 5. 手动测试步骤

**步骤1：生成二维码**
```bash
curl http://localhost:8077/api/user/qr-code
```

**步骤2：检查Redis存储**
```bash
redis-cli
KEYS wechat:qr:*
```

**步骤3：扫码并查看日志**
- 使用微信扫描二维码
- 查看控制台日志输出
- 检查是否收到回调

**步骤4：检查Ticket状态**
```bash
curl "http://localhost:8077/api/user/check-ticket?ticket=你的ticket"
```

**步骤5：检查Redis中的Ticket**
```bash
redis-cli
GET wechat:qr:ticket:你的ticket
```

#### 6. 调试代码

如果问题仍然存在，可以在 `WeChatServiceImpl.handleWeChatCallback` 方法中添加更多日志：

```java
// 打印所有Redis中的scene映射
Set<String> sceneKeys = redisTemplate.keys("wechat:qr:scene:*");
System.out.println("当前Redis中的scene映射: " + sceneKeys);
for (String key : sceneKeys) {
    System.out.println(key + " -> " + redisTemplate.opsForValue().get(key));
}
```

#### 7. 验证流程

完整的验证流程应该是：

1. ✅ 生成二维码 -> 返回ticket和qrCodeUrl
2. ✅ Redis存储 `wechat:qr:scene:{sceneId}` -> `{ticket}`
3. ✅ Redis存储 `wechat:qr:ticket:{ticket}` -> `pending`
4. ✅ 用户扫码 -> 微信回调 `/api/wechat/callback`
5. ✅ 解析EventKey获取sceneId
6. ✅ 根据sceneId查找ticket
7. ✅ 更新 `wechat:qr:ticket:{ticket}` -> `{openId}`
8. ✅ 前端轮询 -> 返回 `scanned: true`

如果某个步骤失败，根据日志定位问题。

