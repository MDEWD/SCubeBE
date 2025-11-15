# 算立方（SuanCube）后端系统

## 项目简介

算立方是一个算力租赁平台的后端系统，提供商品管理、用户认证、评论、社区等功能。

## 技术栈

- Spring Boot 3.5.7
- MyBatis Plus 3.5.3.1
- Spring Security
- JWT认证
- Redis缓存
- MySQL数据库
- 微信公众号SDK

## 项目结构

```
src/main/java/com/scube/scubebackend/
├── common/              # 通用类（错误码等）
├── config/              # 配置类（Redis、Security、MyBatis等）
├── controller/          # 控制器层
├── exception/           # 异常处理
├── filter/              # 过滤器（JWT认证）
├── mapper/              # MyBatis Mapper接口
├── model/
│   ├── dto/             # 数据传输对象（DTO/VO）
│   └── entity/          # 实体类
├── service/             # 服务层接口
│   └── impl/            # 服务层实现
└── util/                # 工具类
```

## 编译和运行

### 前置要求

1. JDK 21
2. Maven 3.6+
3. MySQL 8.0+
4. Redis 6.0+

### 配置数据库

1. 创建数据库：
```sql
CREATE DATABASE scube CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 修改 `src/main/resources/application.yml` 中的数据库配置：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/scube?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password
```

3. 修改Redis配置：
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your_password
```

4. 修改微信公众号配置（如果需要）：
```yaml
wechat:
  mp:
    app-id: your-app-id
    secret: your-secret
    token: your-token
    aes-key: your-aes-key
```

### 编译项目

```bash
mvn clean compile
```

### 打包项目

```bash
mvn clean package
```

### 运行项目

```bash
mvn spring-boot:run
```

或者运行打包后的jar文件：

```bash
java -jar target/SCubeBackend-0.0.1.jar
```

## API接口文档

### 用户认证接口

- `POST /api/user/login` - 微信公众号登录
- `GET /api/user/get/login` - 获取当前登录用户信息

### 首页数据接口

- `GET /api/home/carousel` - 获取轮播图
- `GET /api/home/hot-products` - 获取热门商品
- `GET /api/home/services` - 获取服务保障信息

### 商品管理接口

- `POST /api/product/publish` - 发布商品（需要ADMIN或PARTNER权限）
- `GET /api/product/list` - 获取商品列表
- `GET /api/product/{id}` - 获取商品详情
- `PUT /api/product/{id}` - 更新商品
- `DELETE /api/product/{id}` - 删除商品
- `GET /api/product/my` - 获取我的商品

### 评论接口

- `POST /api/product/{productId}/comment` - 发表评论
- `GET /api/product/{productId}/comments` - 获取商品评论列表
- `POST /api/comment/{commentId}/vote` - 点赞/点踩评论

### 社区接口

- `POST /api/post` - 发表帖子
- `GET /api/post/list` - 获取帖子列表
- `GET /api/post/{id}` - 获取帖子详情

### 文件上传接口

- `POST /api/file/upload` - 上传图片

## 数据库表结构

请参考技术文档中的数据库模型结构说明，创建相应的数据库表。

## 注意事项

1. 首次运行前需要创建数据库表
2. 确保Redis服务已启动
3. JWT Token默认有效期为7天
4. 商品列表和详情有5分钟的Redis缓存
5. 文件上传默认保存到 `./uploads` 目录

## 开发说明

- 使用MyBatis Plus进行数据库操作
- 使用Spring Security进行权限控制
- 使用JWT进行用户认证
- 使用Redis进行缓存
- 统一使用BaseResponse进行响应封装
- 使用@PreAuthorize注解进行方法级权限控制

