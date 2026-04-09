# 本地开发配置指南

## 快速开始（5分钟配置）

### 1. 选择内网穿透工具

推荐使用 **ngrok**（最简单）或 **natapp**（国内服务，更稳定）

### 2. 使用 ngrok（推荐新手）

#### 步骤1：下载 ngrok

访问：https://ngrok.com/download
- Windows: 下载 `ngrok.exe`
- Mac: `brew install ngrok` 或下载zip包
- Linux: 下载对应版本

#### 步骤2：启动本地服务

```bash
# 确保项目已启动
mvn spring-boot:run
```

服务运行在：`http://localhost:8077`

#### 步骤3：启动 ngrok

```bash
# Windows
ngrok.exe http 8077

# Mac/Linux
ngrok http 8077
```

#### 步骤4：获取公网地址

启动后会显示类似：
```
Session Status                online
Account                       your-email@example.com
Version                       3.x.x
Region                        United States (us)
Latency                       45ms
Web Interface                 http://127.0.0.1:4040
Forwarding                    https://abc123.ngrok.io -> http://localhost:8077
```

**复制这个地址：** `https://abc123.ngrok.io`

#### 步骤5：配置测试号

在测试号管理页面的"接口配置信息"中：

- **URL**：`https://abc123.ngrok.io/api/wechat/callback`
- **Token**：`123456`（与application.yml中的token一致）
- **EncodingAESKey**：留空（选择"明文模式"）

点击"提交"，应该显示"配置成功"。

### 3. 使用 natapp（国内服务，更稳定）

#### 步骤1：注册账号

访问：https://natapp.cn/ 注册账号

#### 步骤2：创建免费隧道

1. 登录后，点击"购买隧道" -> "免费隧道"
2. 配置隧道：
   - **隧道名称**：随便填，如"微信测试"
   - **本地端口**：`8077`
   - **隧道协议**：选择 `http`
3. 点击"购买"（免费）

#### 步骤3：获取authtoken

在"我的隧道"页面，找到刚创建的隧道，复制 **authtoken**

#### 步骤4：下载客户端

访问：https://natapp.cn/download 下载对应版本

#### 步骤5：配置并启动

```bash
# 配置authtoken（只需配置一次）
natapp -authtoken=你的authtoken

# 启动隧道
natapp
```

启动后会显示：
```
Tunnel Status                 Online
Version                       2.x.x
Forwarding                    http://abc123.natapp1.cc -> 127.0.0.1:8077
```

**复制这个地址：** `http://abc123.natapp1.cc`

#### 步骤6：配置测试号

- **URL**：`http://abc123.natapp1.cc/api/wechat/callback`
- **Token**：`123456`
- **EncodingAESKey**：留空

## 完整配置流程

### 1. 启动本地服务

```bash
cd E:\promote\cloud\SCubeBE
mvn spring-boot:run
```

确保服务正常运行在 `http://localhost:8077`

### 2. 启动内网穿透

选择一种工具启动：

**ngrok:**
```bash
ngrok http 8077
```

**natapp:**
```bash
natapp
```

### 3. 测试回调接口

在浏览器访问内网穿透地址：
```
https://abc123.ngrok.io/api/wechat/callback
```

如果看到页面（即使报错），说明内网穿透配置成功。

### 4. 配置测试号

1. 打开测试号管理页面
2. 在"接口配置信息"中填写：
   - **URL**：`https://你的内网穿透地址/api/wechat/callback`
   - **Token**：`123456`（与application.yml一致）
   - **EncodingAESKey**：留空
3. 点击"提交"
4. 如果显示"配置成功"，说明配置正确

### 5. 测试完整流程

1. 调用接口生成二维码：
   ```bash
   curl http://localhost:8077/api/user/qr-code
   ```

2. 使用微信扫描二维码并关注测试号

3. 公众号应该自动回复6位验证码

4. 前端轮询检查是否已扫描：
   ```bash
   curl "http://localhost:8077/api/user/check-ticket?ticket=你的ticket"
   ```

5. 使用验证码登录：
   ```bash
   curl -X POST http://localhost:8077/api/user/login \
     -H "Content-Type: application/json" \
     -d '{"code":"123456","openId":"你的openId"}'
   ```

## 常见问题

### Q1: ngrok每次启动域名都变化？

**A:** 是的，免费版ngrok每次启动域名都会变化。解决方案：
- 每次启动后重新配置测试号URL
- 或使用付费版ngrok（支持固定域名）
- 或使用natapp（免费版也支持固定域名，但需要实名认证）

### Q2: 配置失败，提示"token验证失败"？

**A:** 检查以下几点：
1. `application.yml` 中的 `wechat.mp.token` 是否与测试号后台配置一致
2. 确保代码中的签名验证逻辑正确（已实现）
3. 重启本地服务后重试

### Q3: 配置失败，提示"URL无法访问"？

**A:** 检查以下几点：
1. 本地服务是否正在运行（`http://localhost:8077`）
2. 内网穿透是否正常启动
3. 在浏览器访问内网穿透地址，看是否能访问
4. 确保URL格式正确：`https://abc123.ngrok.io/api/wechat/callback`

### Q4: 内网穿透连接不稳定？

**A:** 
- 使用natapp（国内服务，更稳定）
- 或使用云服务器（最稳定）
- 检查网络连接

### Q5: 如何保持内网穿透一直运行？

**A:**
- Windows: 使用 `start /b ngrok http 8077` 后台运行
- Mac/Linux: 使用 `nohup ngrok http 8077 &` 后台运行
- 或使用screen/tmux保持会话

## 推荐配置

### 开发环境
- **工具**：ngrok（简单）或 natapp（稳定）
- **协议**：HTTP或HTTPS都可以
- **端口**：8077（本地），内网穿透会自动映射到80/443

### 生产环境
- **服务器**：云服务器（阿里云、腾讯云等）
- **协议**：HTTPS（推荐）
- **端口**：443
- **域名**：配置正式域名

## 内网穿透工具对比

| 工具 | 免费版 | 固定域名 | 国内访问 | 推荐度 |
|------|--------|----------|----------|--------|
| ngrok | ✅ | ❌ | 较慢 | ⭐⭐⭐ |
| natapp | ✅ | ✅（需实名） | 快 | ⭐⭐⭐⭐ |
| cpolar | ✅ | ❌ | 快 | ⭐⭐⭐ |
| 云服务器 | ❌ | ✅ | 快 | ⭐⭐⭐⭐⭐ |

## 下一步

配置完成后，参考：
- **功能使用指南**：`docs/wechat-login-guide.md`
- **完整配置文档**：`docs/wechat-test-account-setup.md`

