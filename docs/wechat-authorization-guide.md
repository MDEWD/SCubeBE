# 微信网页授权获取用户信息指南

## 功能说明

实现微信网页授权功能，让用户在扫码后可以选择授权，授权后可以获取用户的昵称和头像，无需用户再次设置。

## 背景知识

根据微信官方文档和实际使用经验：

1. **未授权情况**：如果用户没有点击授权，从微信扫码只能获得用户的 `OpenId`，其他信息都获取不到
2. **授权后**：通过用户的授权，我们可以直接获取到用户的昵称和头像，不用用户再次设置
3. **手机号**：用户的手机号无法通过网页授权获取到

## 实现流程

### 1. 生成授权链接

前端调用接口获取授权链接，引导用户点击授权：

**接口地址：** `GET /api/user/authorize-url`

**请求参数：**
- `redirectUri`：授权后重定向的URI（需要URL编码）
- `state`：状态参数，用于保持请求和回调的状态（可选，可以传递ticket）

**响应数据：**
```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "authorizationUrl": "https://open.weixin.qq.com/connect/oauth2/authorize?appid=xxx&redirect_uri=xxx&response_type=code&scope=snsapi_userinfo&state=xxx#wechat_redirect"
  }
}
```

### 2. 用户授权

用户点击授权链接后，微信会跳转到授权页面，用户确认授权后，微信会重定向到我们配置的回调地址。

### 3. 授权回调

微信重定向到：`GET /api/wechat/oauth/callback?code=xxx&state=xxx`

后端会：
1. 使用 `code` 换取 `access_token` 和 `openId`
2. 使用 `access_token` 和 `openId` 获取用户信息（昵称、头像）
3. 将用户信息存储到Redis
4. 重定向到前端页面，带上用户信息

### 4. 更新登录流程

在扫码后，如果用户授权了，`checkTicket` 接口会返回用户的昵称和头像信息。

## 完整登录流程（带授权）

### 方案一：扫码后引导用户授权

1. 用户扫码关注公众号
2. 公众号回复验证码
3. 前端检测到已扫描，提示用户"是否授权获取昵称和头像？"
4. 用户点击"授权"，跳转到授权链接
5. 用户授权后，回调接口获取用户信息并存储
6. 用户输入验证码完成登录

### 方案二：在二维码中嵌入授权链接

1. 生成二维码时，同时生成授权链接
2. 用户扫码后，直接跳转到授权页面
3. 用户授权后，获取用户信息
4. 然后引导用户关注公众号获取验证码
5. 用户输入验证码完成登录

## 接口说明

### 1. 生成授权链接

**接口地址：** `GET /api/user/authorize-url`

**请求示例：**
```
GET /api/user/authorize-url?redirectUri=http%3A%2F%2Fyour-domain.com%2Fapi%2Fwechat%2Foauth%2Fcallback&state=ticket123
```

**响应示例：**
```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "authorizationUrl": "https://open.weixin.qq.com/connect/oauth2/authorize?appid=wxf10bccf1a861aeb7&redirect_uri=http%3A%2F%2Fyour-domain.com%2Fapi%2Fwechat%2Foauth%2Fcallback&response_type=code&scope=snsapi_userinfo&state=ticket123&connect_redirect=1#wechat_redirect"
  }
}
```

### 2. 授权回调接口

**接口地址：** `GET /api/wechat/oauth/callback`

**请求参数：**
- `code`：授权code（微信自动传递）
- `state`：状态参数（微信自动传递）

**处理流程：**
1. 使用 `code` 换取 `access_token` 和 `openId`
2. 使用 `access_token` 和 `openId` 获取用户信息
3. 将用户信息存储到Redis
4. 重定向到前端页面

### 3. 检查Ticket（已更新）

**接口地址：** `GET /api/user/check-ticket?ticket=xxx`

**响应数据（已授权）：**
```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "scanned": true,
    "userInfo": {
      "openId": "oUpF8uMuAJO_M2pxb1Q9zNjWeS6o",
      "nickname": "用户昵称",
      "avatar": "https://thirdwx.qlogo.cn/xxx"
    }
  }
}
```

**响应数据（未授权）：**
```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "scanned": true,
    "userInfo": {
      "openId": "oUpF8uMuAJO_M2pxb1Q9zNjWeS6o",
      "nickname": null,
      "avatar": null
    }
  }
}
```

## 配置说明

### 1. 微信公众号后台配置

1. 登录微信公众平台测试号：https://mp.weixin.qq.com/debug/cgi-bin/sandboxinfo
2. 在"体验接口权限表"中找到"网页服务" -> "网页账号" -> "网页授权获取用户基本信息"
3. 点击"修改"，添加授权回调域名（不需要带 `http://` 或 `https://`）
   - 例如：`your-domain.com` 或 `localhost:8077`（本地开发）

### 2. 本地开发配置

如果使用内网穿透（如 ngrok、cpolar），需要：
1. 配置内网穿透，将本地端口映射到公网
2. 在微信后台配置授权回调域名为内网穿透的域名
3. 确保回调地址使用公网地址

### 3. application.yml 配置

确保已配置微信公众号的 AppID 和 Secret：

```yaml
wechat:
  mp:
    app-id: wxf10bccf1a861aeb7
    secret: 832b070364fdd4a6c83da1fbd8a0299f
    token: 123456
```

## 注意事项

1. **授权域名**：必须在微信公众平台后台配置授权回调域名
2. **HTTPS**：生产环境必须使用HTTPS（测试号可以使用HTTP）
3. **state参数**：建议使用ticket作为state，方便关联扫码和授权
4. **用户拒绝授权**：如果用户拒绝授权，只能获取到openId，昵称和头像为null
5. **授权有效期**：授权后获取的access_token有时效性，建议缓存用户信息

## 参考文档

- [微信网页授权文档](https://developers.weixin.qq.com/doc/offiaccount/OA_Web_Apps/Wechat_webpage_authorization.html)
- [参考文章](https://blog.csdn.net/Go_ahead_forever/article/details/149275627)

