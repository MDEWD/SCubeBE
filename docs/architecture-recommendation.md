# 架构评审与建议 (Architecture Review & Recommendations)

## 1. 关于微服务架构 (Microservices)

**结论：** 目前阶段（单人开发、两个核心业务模块）**极为不推荐**使用微服务架构。

**理由：**
1.  **运维成本剧增**：微服务需要服务发现(Nacos/Eureka)、配置中心、网关(Gateway)、分布式事务(Seata)、分布式链路追踪(SkyWalking)等组件。对于单人开发者，维护这些基础设施的时间可能超过开发业务的时间。
2.  **开发效率降低**：跨服务调用（RPC/Feign）比本地方法调用复杂得多，且调试困难。
3.  **资源消耗**：每个服务都需要独立的JVM进程，对服务器内存要求高。

## 2. 现有架构分析 (Current Architecture)

目前项目采用的是标准的 **分层架构 (Layered Architecture)**：
- `controller/`
- `service/`
- `mapper/`

**存在的问题：**
虽然这种架构在起步阶段很简单，但随着“商品(Product)”和“社区(Community)”功能的增加，所有Service都在一个包里，所有Controller也在一个包里，会导致代码耦合度高，界限模糊。

## 3. 改进方案：模块化单体 (Modular Monolith)

**建议架构方向：**
将代码按照 **业务领域 (Domain/Feature)** 进行拆分，而不是按技术层级拆分。

**推荐的目录结构：**
```
com.scube.scubebackend
├── common             // 通用工具、全局异常、Swagger配置等
├── infrastructure     // 基础设施（Redis配置、第三方SDK等）
├── modules
│   ├── product        // 商品模块（包含独立的 Controller, Service, Mapper, Entity）
│   │   ├── controller
│   │   ├── service
│   │   ├── mapper
│   │   └── model
│   ├── community      // 社区模块（包含 Post, Comment, Answer 等）
│   │   ├── controller
│   │   └── ...
│   └── user           // 用户模块
│       ├── controller
│       └── ...
└── SCubeBackendApplication.java
```

**优势：**
1.  **逻辑清晰**：商品相关的代码都在 `modules/product` 下，修改商品功能时只需通过该文件夹。
2.  **易于拆分**：如果未来团队扩大或流量剧增，可以直接将 `modules/product` 文件夹独立出来作为一个微服务，改造成本极低。
3.  **高内聚低耦合**：这也是微服务的核心思想，但在单体中实现成本最低。

## 4. 已进行的修复 (Fixes Applied)

针对现有项目配置，我已修复了以下安全隐患：
- **`application.yml` 配置优化**：将数据库密码、微信密钥等敏感信息替换为环境变量占位符（如 `${DB_PASSWORD:123456}`）。这符合云原生应用（12-Factor App）的最佳实践，避免源码泄露敏感信息，同时也方便多环境部署。

