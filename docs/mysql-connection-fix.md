# MySQL连接问题解决方案

## 错误信息

```
Caused by: java.sql.SQLNonTransientConnectionException: Public Key Retrieval is not allowed
```

## 问题原因

这是MySQL 8.0+版本的一个常见问题。MySQL 8.0默认使用`caching_sha2_password`认证插件，该插件在首次连接时需要从服务器获取公钥。如果连接URL中没有允许公钥检索，就会出现这个错误。

## 解决方案

### 方案一：在连接URL中添加参数（推荐）

在 `application.yml` 中的数据库连接URL添加 `allowPublicKeyRetrieval=true` 参数：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/scube?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
```

### 方案二：修改MySQL用户认证方式

如果不想使用 `allowPublicKeyRetrieval=true`，可以修改MySQL用户的认证方式：

```sql
-- 查看当前用户认证方式
SELECT user, host, plugin FROM mysql.user WHERE user='root';

-- 修改为 mysql_native_password（如果使用的是MySQL 8.0.11之前的版本）
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'your_password';
FLUSH PRIVILEGES;
```

**注意：** MySQL 8.0.11之后，`mysql_native_password`已被弃用，推荐使用方案一。

## 其他可能的问题

### 1. 数据库不存在

如果数据库不存在，需要先创建：

```sql
CREATE DATABASE scube CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 数据库表不存在

执行建表脚本：

```bash
mysql -u root -p scube < sql/init.sql
```

### 3. MySQL服务未启动

**Windows:**
```bash
# 检查MySQL服务状态
net start | findstr MySQL

# 启动MySQL服务
net start MySQL
```

**Mac/Linux:**
```bash
# 检查MySQL服务状态
sudo systemctl status mysql

# 启动MySQL服务
sudo systemctl start mysql
```

### 4. 用户名或密码错误

检查 `application.yml` 中的数据库用户名和密码是否正确。

### 5. 端口错误

默认MySQL端口是3306，如果使用其他端口，需要在URL中指定：

```yaml
url: jdbc:mysql://localhost:3307/scube?...
```

## 完整的数据库连接URL参数说明

```yaml
jdbc:mysql://localhost:3306/scube?
  useUnicode=true                    # 使用Unicode编码
  &characterEncoding=utf8            # 字符编码
  &useSSL=false                      # 禁用SSL（开发环境）
  &serverTimezone=Asia/Shanghai      # 服务器时区
  &allowPublicKeyRetrieval=true      # 允许公钥检索（解决MySQL 8.0连接问题）
  &rewriteBatchedStatements=true     # 批量插入优化（可选）
```

## 验证连接

配置完成后，重启项目，如果连接成功，应该能看到：

```
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
```

如果还有问题，检查控制台的完整错误信息。

