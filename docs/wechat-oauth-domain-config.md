# 微信授权回调域名配置指南

## 错误信息

```
redirect_uri域名与后台配置不一致，错误码:10003
```

## 问题原因

微信网页授权需要先在微信公众平台后台配置授权回调域名，只有配置的域名才能作为 `redirect_uri` 使用。

## 解决步骤

### 1. 登录微信公众平台测试号

访问：https://mp.weixin.qq.com/debug/cgi-bin/sandboxinfo

### 2. 配置授权回调域名

1. 在"体验接口权限表"中找到"网页服务" -> "网页账号" -> "网页授权获取用户基本信息"
2. 点击"修改"或"设置"
3. 在"授权回调域名"中输入你的域名（**不需要带 `http://` 或 `https://`，也不需要带端口号**）

**重要说明：**
- 只需要填写域名，例如：`your-domain.com` 或 `localhost`（本地开发）
- **不要填写** `http://your-domain.com` 或 `https://your-domain.com`
- **不要填写** 端口号，例如：`localhost:8077`（只填写 `localhost`）
- 本地开发时，可以填写 `localhost`，但需要确保 `redirect_uri` 也使用 `localhost`

### 3. 检查 application.yml 配置

确保 `oauth-domain` 配置正确：

```yaml
wechat:
  mp:
    oauth-domain: http://localhost:8077  # 本地开发
    # 或者
    oauth-domain: https://your-domain.com  # 生产环境
```

### 4. 本地开发配置（使用内网穿透）

如果使用内网穿透（如 ngrok、cpolar），需要：

1. **启动内网穿透**，获取公网地址，例如：`https://abc123.ngrok.io`
2. **在微信后台配置授权回调域名**：只填写域名部分，例如：`abc123.ngrok.io`（不要带 `https://`）
3. **在 application.yml 中配置**：
   ```yaml
   wechat:
     mp:
       oauth-domain: https://abc123.ngrok.io
   ```

### 5. 验证配置

授权链接的格式应该是：
```
https://open.weixin.qq.com/connect/oauth2/authorize?
  appid=YOUR_APPID&
  redirect_uri=YOUR_OAUTH_DOMAIN/api/wechat/oauth/callback&
  response_type=code&
  scope=snsapi_userinfo&
  state=YOUR_STATE&
  connect_redirect=1#wechat_redirect
```

其中 `redirect_uri` 的域名部分必须与微信后台配置的授权回调域名一致。

## 常见错误

### 错误1：域名带协议前缀

**错误配置：**
- 微信后台：`http://localhost` ❌
- 微信后台：`https://your-domain.com` ❌

**正确配置：**
- 微信后台：`localhost` ✅
- 微信后台：`your-domain.com` ✅

### 错误2：域名带端口号

**错误配置：**
- 微信后台：`localhost:8077` ❌

**正确配置：**
- 微信后台：`localhost` ✅
- 注意：`redirect_uri` 可以带端口号，但后台配置的域名不能带端口号

### 错误3：本地开发未使用内网穿透

**问题：**
- 本地开发时，如果直接使用 `localhost:8077`，微信无法回调

**解决：**
- 使用内网穿透工具（ngrok、cpolar等）
- 在微信后台配置内网穿透的域名
- 在 `application.yml` 中配置内网穿透的地址

## 配置示例

### 本地开发（使用内网穿透）

1. **启动内网穿透**：
   ```bash
   # 使用 ngrok
   ngrok http 8077
   
   # 或使用 cpolar
   cpolar http 8077
   ```

2. **获取公网地址**：例如 `https://abc123.ngrok.io`

3. **在微信后台配置**：
   - 授权回调域名：`abc123.ngrok.io`

4. **在 application.yml 配置**：
   ```yaml
   wechat:
     mp:
       oauth-domain: https://abc123.ngrok.io
   ```

### 生产环境

1. **在微信后台配置**：
   - 授权回调域名：`your-domain.com`

2. **在 application.yml 配置**：
   ```yaml
   wechat:
     mp:
       oauth-domain: https://your-domain.com
   ```

## 验证步骤

1. 检查微信后台配置的授权回调域名
2. 检查 `application.yml` 中的 `oauth-domain` 配置
3. 检查生成的授权链接中的 `redirect_uri` 参数
4. 确保 `redirect_uri` 的域名部分与微信后台配置一致

## 注意事项

1. **域名必须一致**：`redirect_uri` 的域名必须与微信后台配置的授权回调域名完全一致
2. **协议要匹配**：如果微信后台配置的是 `https` 域名，`redirect_uri` 也必须使用 `https`
3. **本地开发**：本地开发建议使用内网穿透，否则微信无法回调本地地址
4. **配置生效时间**：配置后可能需要几分钟才能生效

