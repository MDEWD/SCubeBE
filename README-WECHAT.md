# 微信测试号快速配置指南

## 快速开始

### 1. 获取测试号信息

访问：https://mp.weixin.qq.com/debug/cgi-bin/sandboxinfo?action=showinfo&t=sandbox/index

获取：
- **appID**：测试号的AppID
- **appsecret**：测试号的AppSecret

### 2. 配置 application.yml

```yaml
wechat:
  mp:
    app-id: 你的AppID
    secret: 你的AppSecret
    token: 123456  # 自定义Token，建议使用随机字符串
    aes-key:       # 测试环境可以不填
```

### 3. 配置接口配置信息（本地开发）

#### 步骤1：启动内网穿透

**使用 ngrok（推荐新手）：**
```bash
# 下载 ngrok: https://ngrok.com/download
ngrok http 8077
```

启动后会显示：
```
Forwarding   https://abc123.ngrok.io -> http://localhost:8077
```

**使用 natapp（国内服务，更稳定）：**
```bash
# 1. 注册: https://natapp.cn/
# 2. 创建免费隧道（本地端口8077）
# 3. 下载客户端并启动
natapp
```

#### 步骤2：在测试号管理页面配置

在 **"接口配置信息"** 部分：

- **URL**：
  - ngrok: `https://abc123.ngrok.io/api/wechat/callback`
  - natapp: `http://abc123.natapp1.cc/api/wechat/callback`
  - ⚠️ 替换为你实际的内网穿透地址

- **Token**：`123456`（与 `application.yml` 中的 `token` 一致）

- **EncodingAESKey**：留空（选择"明文模式"）

点击"提交"，如果配置成功会显示"配置成功"。

**详细步骤请参考：** `docs/local-development-guide.md`

### 4. 配置JS接口安全域名（可选）

如果需要使用微信JS-SDK，在 **"JS接口安全域名"** 部分：
- 点击"设置"
- 填写域名（不带http://和端口号）
- 点击"确定"

### 5. 测试

1. 启动项目：`mvn spring-boot:run`
2. 测试二维码生成：`curl http://localhost:8077/api/user/qr-code`
3. 使用微信扫描二维码并关注
4. 公众号会自动回复6位验证码

## 详细文档

- **完整配置指南**：`docs/wechat-test-account-setup.md`
- **功能使用指南**：`docs/wechat-login-guide.md`

## 常见问题

**Q: 配置失败，提示token验证失败？**  
A: 检查 `application.yml` 中的 `token` 是否与测试号后台配置一致。

**Q: URL无法访问？**  
A: 确保服务器正在运行，且端口已开放。本地开发需要使用内网穿透。

**Q: 内网穿透域名每次变化？**  
A: 免费版内网穿透每次启动域名会变化，需要重新配置。建议使用付费版或云服务器。

