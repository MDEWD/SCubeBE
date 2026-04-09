# 数据库初始化说明

## 文件说明

- `init.sql`: 完整的数据库建表脚本，包含所有表的创建语句

## 使用方法

### 方式一：使用MySQL命令行

```bash
mysql -u root -p < sql/init.sql
```

### 方式二：使用MySQL客户端工具

1. 打开MySQL客户端（如Navicat、DBeaver、MySQL Workbench等）
2. 连接到MySQL服务器
3. 打开 `sql/init.sql` 文件
4. 执行整个脚本

### 方式三：在Spring Boot启动时自动执行

如果需要自动执行SQL脚本，可以在 `application.yml` 中添加：

```yaml
spring:
  sql:
    init:
      mode: always
      schema-locations: classpath:sql/init.sql
```

## 表结构说明

### 核心表

1. **user** - 用户表
2. **product** - 商品表
3. **product_tag** - 商品标签表
4. **product_image** - 商品图片表
5. **product_application_scene** - 商品应用场景表
6. **comment** - 评论表
7. **order** - 订单表
8. **post** - 帖子表
9. **post_tag** - 帖子标签表
10. **answer** - 回答表
11. **requirement** - 需求表
12. **carousel** - 轮播图表
13. **service** - 服务保障表

## 注意事项

1. 执行脚本前请确保MySQL版本 >= 8.0
2. 脚本会先删除已存在的表，请谨慎操作
3. 建议在测试环境先执行，确认无误后再在生产环境执行
4. 所有表使用 `utf8mb4` 字符集，支持emoji等特殊字符
5. 所有表都包含逻辑删除字段 `is_delete`，默认值为0（未删除）

## 索引说明

- 所有主键都有自增ID
- 外键字段都建立了索引
- 常用查询字段（如status、type、region等）都建立了索引
- post表的title和content字段建立了全文索引

