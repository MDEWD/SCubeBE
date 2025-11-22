# 微信公众号扫码登录实现指南

## 功能说明

实现通过扫描微信公众号二维码，关注公众号后自动生成6位数字验证码，用户在前端输入验证码完成登录。

## 实现流程

### 1. 前端流程

1. 用户点击"微信登录"按钮
2. 前端调用 `GET /api/user/qr-code` 获取二维码
3. 前端展示二维码，并开始轮询 `GET /api/user/check-ticket?ticket=xxx` 检查是否已扫描
4. 用户使用微信扫描二维码并关注公众号
5. 公众号自动回复6位验证码
6. 前端检测到已扫描（返回openId），提示用户输入验证码
7. 用户输入验证码，调用 `POST /api/user/login` 完成登录

### 2. 后端流程

1. **生成二维码**：调用微信API生成临时二维码，将Ticket和sceneId存储到Redis
2. **接收回调**：微信服务器回调 `/api/wechat/callback`，处理关注事件
3. **生成验证码**：用户扫码关注后，生成6位验证码并存储到Redis（key: openId）
4. **绑定关系**：将Ticket和openId绑定，用于前端轮询
5. **验证登录**：用户输入验证码，验证通过后生成JWT Token

## 接口说明

### 1. 生成登录二维码

**接口地址：** `GET /api/user/qr-code`

**响应数据：**
```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "ticket": "gQH47joAAAAAAAAAASxodHRwOi8vd2VpeGluLnFxLmNvbS9xL2taZ2Z3TVRtNzJXV1Brb3ZhYmJJAAIEZ23sUwMEmm3sUw==",
    "qrCodeUrl": "https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=...",
    "expireSeconds": "300"
  }
}
```

### 2. 检查Ticket是否已扫描（轮询接口）

**接口地址：** `GET /api/user/check-ticket?ticket=xxx`

**响应数据（未扫描）：**
```json
{
  "code": 0,
  "data": {
    "scanned": "false"
  }
}
```

**响应数据（已扫描）：**
```json
{
  "code": 0,
  "data": {
    "scanned": "true",
    "openId": "oUpF8uMuAJO_M2pxb1Q9zNjWeS6o"
  }
}
```

### 3. 使用验证码登录

**接口地址：** `POST /api/user/login`

**请求参数：**
```json
{
  "code": "123456",
  "openId": "oUpF8uMuAJO_M2pxb1Q9zNjWeS6o"
}
```

**响应数据：**
```json
{
  "code": 0,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "user": {
      "id": 1,
      "openId": "oUpF8uMuAJO_M2pxb1Q9zNjWeS6o",
      "nickname": "用户昵称",
      "avatar": "https://...",
      "userRole": "USER",
      "createTime": "2024-01-15 10:00:00"
    }
  }
}
```

### 4. 微信回调接口（内部使用）

**接口地址：** `POST /api/wechat/callback`

此接口由微信服务器调用，用于接收用户扫码、关注等事件。

## 配置说明

### 1. 微信公众号配置

在 `application.yml` 中配置：

```yaml
wechat:
  mp:
    app-id: your-app-id          # 公众号AppID
    secret: your-secret          # 公众号Secret
    token: your-token            # 服务器配置Token
    aes-key: your-aes-key        # 消息加解密密钥（可选）
```

### 2. 微信公众号后台配置

1. **服务器配置**：
   - URL: `https://your-domain.com/api/wechat/callback`
   - Token: 与配置文件中的token一致
   - EncodingAESKey: 可选，如果配置了需要填写aes-key

2. **接口权限**：
   - 需要开通"生成带参数的二维码"接口权限

### 3. 测试环境

可以使用微信测试号进行开发测试：
- 测试号申请：https://mp.weixin.qq.com/debug/cgi-bin/sandboxinfo?action=showinfo&t=sandbox/index
- **详细配置指南请参考：** `docs/wechat-test-account-setup.md`

## 注意事项

1. **服务器要求**：
   - 必须是公网可访问的服务器
   - 端口必须是80或443（微信限制）
   - 如果使用内网穿透，需要支持80/443端口

2. **验证码有效期**：
   - 验证码有效期为3分钟
   - 验证码使用后立即删除（一次性使用）

3. **二维码有效期**：
   - 二维码有效期为5分钟
   - 过期后需要重新生成

4. **安全性**：
   - 验证码存储在Redis中，自动过期
   - 验证成功后立即删除验证码
   - 使用JWT Token进行后续认证

## 前端示例代码

```javascript
// 1. 获取二维码
async function getQrCode() {
  const response = await fetch('/api/user/qr-code');
  const result = await response.json();
  if (result.code === 0) {
    // 显示二维码
    document.getElementById('qrCode').src = result.data.qrCodeUrl;
    // 开始轮询
    startPolling(result.data.ticket);
  }
}

// 2. 轮询检查是否已扫描
function startPolling(ticket) {
  const interval = setInterval(async () => {
    const response = await fetch(`/api/user/check-ticket?ticket=${ticket}`);
    const result = await response.json();
    if (result.code === 0 && result.data.scanned === 'true') {
      clearInterval(interval);
      // 提示用户输入验证码
      const openId = result.data.openId;
      showVerifyCodeInput(openId);
    }
  }, 2000); // 每2秒轮询一次
  
  // 5分钟后停止轮询
  setTimeout(() => clearInterval(interval), 300000);
}

// 3. 用户输入验证码登录
async function login(openId, code) {
  const response = await fetch('/api/user/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      code: code,
      openId: openId
    })
  });
  const result = await response.json();
  if (result.code === 0) {
    // 保存Token
    localStorage.setItem('token', result.data.token);
    // 跳转到首页
    window.location.href = '/';
  }
}
```

## 参考文档

- [微信公众号开发文档](https://developers.weixin.qq.com/doc/offiaccount/Getting_Started/Overview.html)
- [生成带参数的二维码](https://developers.weixin.qq.com/doc/offiaccount/Account_Management/Generating_a_Parametric_QR_Code.html)
- [接收事件推送](https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Receiving_event_pushes.html)

