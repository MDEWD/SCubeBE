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

