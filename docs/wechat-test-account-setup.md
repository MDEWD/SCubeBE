# 微信测试号配置指南

## 一、获取测试号信息

1. 访问微信测试号申请页面：https://mp.weixin.qq.com/debug/cgi-bin/sandboxinfo?action=showinfo&t=sandbox/index
2. 使用微信扫码登录
3. 获取以下信息：
   - **appID**：测试号的AppID
   - **appsecret**：测试号的AppSecret

## 二、配置 application.yml

在 `src/main/resources/application.yml` 中配置测试号信息：

```yaml
wechat:
  mp:
    app-id: wxf10bccf1a861aeb7          # 替换为你的测试号AppID
    secret: 832b070364fdd4a6c83da1fbd8a0299f  # 替换为你的测试号AppSecret
    token: 123456                        # 自定义Token（用于验证签名，建议使用随机字符串）
    aes-key:                            # 消息加解密密钥（可选，测试环境可以不填）
```

### Token 生成建议

Token 可以是任意字符串，建议使用随机生成的字符串，例如：
- 在线生成：https://www.random.org/strings/
- 或使用命令行：`openssl rand -hex 16`

## 三、配置接口配置信息

### 1. 准备服务器地址（本地开发）

**重要：** 微信服务器回调需要公网可访问的地址，且端口必须是 **80** 或 **443**

#### 本地开发方案：使用内网穿透（推荐）

本地开发必须使用内网穿透工具，将本地服务暴露到公网。推荐以下工具：

##### 方案一：使用 ngrok（简单易用）

1. **注册账号**：访问 https://ngrok.com/ 注册免费账号
2. **下载工具**：下载对应系统的ngrok客户端
3. **启动隧道**：
   ```bash
   # Windows
   ngrok.exe http 8077
   
   # Mac/Linux
   ./ngrok http 8077
   ```
4. **获取公网地址**：
   - 启动后会显示类似：`https://abc123.ngrok.io -> http://localhost:8077`
   - 复制 `https://abc123.ngrok.io` 这个地址

**注意：** 免费版每次启动域名会变化，需要重新配置

##### 方案二：使用 natapp（国内服务，推荐）

1. **注册账号**：访问 https://natapp.cn/ 注册账号
2. **购买免费隧道**：登录后创建免费隧道
   - 本地端口：`8077`
   - 隧道协议：选择 `http`
3. **下载客户端**：下载对应系统的natapp客户端
4. **配置authtoken**：
   ```bash
   natapp -authtoken=你的authtoken
   ```
5. **启动隧道**：
   ```bash
   natapp
   ```
6. **获取公网地址**：启动后会显示公网地址，例如：`http://abc123.natapp1.cc`

##### 方案三：使用 cpolar（国内服务）

1. **注册账号**：访问 https://www.cpolar.com/ 注册账号
2. **下载客户端**：下载对应系统的cpolar客户端
3. **启动隧道**：
   ```bash
   cpolar http 8077
   ```
4. **获取公网地址**：启动后会显示公网地址

##### 方案四：使用云服务器（生产环境推荐）

如果你有云服务器（如阿里云、腾讯云等）：
- 确保服务器有公网IP
- 确保80或443端口已开放
- 配置域名（可选，但推荐）

### 2. 配置回调URL

#### 步骤1：启动内网穿透

以 **ngrok** 为例：
```bash
ngrok http 8077
```

启动后会显示：
```
Forwarding   https://abc123.ngrok.io -> http://localhost:8077
```

#### 步骤2：在测试号管理页面配置

在测试号管理页面，找到 **"接口配置信息"** 部分：

1. **URL（服务器地址）**：
   
   **使用 ngrok（HTTPS）：**
   ```
   https://abc123.ngrok.io/api/wechat/callback
   ```
   
   **使用 natapp（HTTP）：**
   ```
   http://abc123.natapp1.cc/api/wechat/callback
   ```
   
   **使用云服务器：**
   ```
   http://your-domain.com/api/wechat/callback
   或
   https://your-domain.com/api/wechat/callback
   ```
   
   **重要提示：**
   - URL必须是公网可访问的完整地址
   - 必须包含协议（http:// 或 https://）
   - 必须包含 `/api/wechat/callback` 路径
   - 端口必须是80或443（内网穿透会自动处理）

2. **Token（令牌）**：
   - 填写与 `application.yml` 中 `wechat.mp.token` 相同的值
   - 例如：`123456`

3. **EncodingAESKey（消息加解密密钥）**：
   - 测试环境可以留空（选择"明文模式"）
   - 如果选择"安全模式"或"兼容模式"，需要填写AES密钥

4. 点击 **"提交"** 按钮
   - 如果配置正确，会显示"配置成功"
   - 如果失败，检查：
     - URL是否可访问
     - Token是否一致
     - 服务器是否返回正确的echostr

## 四、配置JS接口安全域名

如果需要在前端使用微信JS-SDK（如分享、定位等功能），需要配置JS接口安全域名：

1. 在测试号管理页面，找到 **"JS接口安全域名"** 部分
2. 点击 **"设置"** 按钮
3. 填写你的域名（不需要带 `http://` 或 `https://`）
   - 例如：`your-domain.com`
   - 或内网穿透域名：`abc123.ngrok.io`
4. 点击 **"确定"**

**注意：**
- 域名不需要备案
- 不需要带协议（http/https）
- 不需要带端口号
- 可以配置多个域名

## 五、测试配置

### 1. 启动项目

```bash
mvn spring-boot:run
```

### 2. 测试回调接口

访问回调接口（GET请求）：
```
http://your-domain.com/api/wechat/callback?signature=xxx&timestamp=xxx&nonce=xxx&echostr=xxx
```

如果配置正确，应该返回 `echostr` 的值。

### 3. 测试二维码生成

```bash
curl http://localhost:8077/api/user/qr-code
```

应该返回二维码信息：
```json
{
  "code": 0,
  "data": {
    "ticket": "...",
    "qrCodeUrl": "https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=...",
    "expireSeconds": "300"
  }
}
```

### 4. 测试完整流程

1. 调用 `/api/user/qr-code` 获取二维码
2. 使用微信扫描二维码并关注测试号
3. 公众号应该自动回复6位验证码
4. 前端轮询 `/api/user/check-ticket?ticket=xxx` 检查是否已扫描
5. 用户输入验证码，调用 `/api/user/login` 完成登录

## 六、常见问题

### 1. 配置失败：token验证失败

**原因：**
- Token不一致
- 签名验证失败

**解决：**
- 检查 `application.yml` 中的 `token` 是否与测试号后台配置一致
- 确保代码中的签名验证逻辑正确

### 2. 配置失败：URL无法访问

**原因：**
- 服务器未启动
- 端口未开放
- 内网穿透未配置

**解决：**
- 确保服务器正在运行
- 检查防火墙设置
- 使用 `curl` 或浏览器测试URL是否可访问

### 3. 回调接口返回错误

**原因：**
- 接口路径错误
- 请求方法错误（GET用于验证，POST用于接收消息）

**解决：**
- 确保URL路径正确：`/api/wechat/callback`
- 确保同时支持GET和POST请求

### 4. 二维码生成失败

**原因：**
- AppID或AppSecret错误
- 接口权限未开通

**解决：**
- 检查 `application.yml` 中的配置
- 测试号默认已开通所有接口权限

### 5. 内网穿透域名变化

**原因：**
- 免费版内网穿透每次启动域名会变化

**解决：**
- 每次启动后需要重新配置回调URL
- 或使用付费版内网穿透（支持固定域名）

## 七、生产环境配置

测试完成后，切换到正式公众号时：

1. 在 `application.yml` 中更新正式号的AppID和AppSecret
2. 在正式公众号后台配置回调URL
3. 配置JS接口安全域名
4. 建议使用HTTPS（443端口）
5. 建议配置消息加解密（使用EncodingAESKey）

## 八、参考文档

- [微信公众平台接口测试号申请](https://mp.weixin.qq.com/debug/cgi-bin/sandboxinfo?action=showinfo&t=sandbox/index)
- [微信公众号开发文档](https://developers.weixin.qq.com/doc/offiaccount/Getting_Started/Overview.html)
- [接口接入指南](https://developers.weixin.qq.com/doc/offiaccount/Basic_Information/Access_Overview.html)
- [生成带参数的二维码](https://developers.weixin.qq.com/doc/offiaccount/Account_Management/Generating_a_Parametric_QR_Code.html)

