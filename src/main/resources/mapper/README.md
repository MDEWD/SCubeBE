# Mapper XML 文件说明

## 为什么 Mapper 接口看起来是"空的"？

### MyBatis Plus 的优势

项目使用了 **MyBatis Plus**，它是对 MyBatis 的增强工具，提供了以下功能：

1. **自动生成基础CRUD操作**：继承 `BaseMapper<T>` 后，自动提供以下方法：
   - `insert(T entity)` - 插入
   - `deleteById(Serializable id)` - 根据ID删除
   - `updateById(T entity)` - 根据ID更新
   - `selectById(Serializable id)` - 根据ID查询
   - `selectList(Wrapper<T> queryWrapper)` - 条件查询列表
   - `selectPage(IPage<T> page, Wrapper<T> queryWrapper)` - 分页查询
   - 等等...

2. **无需编写基础SQL**：对于简单的增删改查，不需要在XML中写SQL语句

3. **强大的条件构造器**：使用 `LambdaQueryWrapper` 可以类型安全地构建查询条件

### 什么时候需要写 XML？

虽然 MyBatis Plus 提供了很多便利，但在以下情况下仍需要在 XML 中编写 SQL：

1. **复杂的关联查询**：需要 JOIN 多个表
2. **自定义SQL逻辑**：如计算平均评分、统计等
3. **性能优化**：需要手写SQL来优化查询性能
4. **特殊数据库函数**：使用数据库特定的函数

### 当前项目的 Mapper XML 文件

本项目为每个 Mapper 都创建了对应的 XML 文件，包含：

1. **ResultMap 映射**：定义实体类与数据库字段的映射关系
2. **Base_Column_List**：定义基础字段列表，方便复用
3. **自定义SQL方法**：如 `incrementViewCount`、`calculateAverageRating` 等

### 使用示例

#### 使用 MyBatis Plus 的基础方法（无需XML）

```java
// 查询单个用户
User user = userMapper.selectById(1L);

// 条件查询
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(User::getUserRole, "ADMIN");
List<User> users = userMapper.selectList(wrapper);

// 分页查询
Page<User> page = new Page<>(1, 10);
Page<User> result = userMapper.selectPage(page, wrapper);
```

#### 使用自定义方法（需要XML）

```java
// 根据openId查询用户（自定义方法）
User user = userMapper.selectByOpenId("openId123");

// 增加浏览量（自定义方法）
productMapper.incrementViewCount(productId);

// 计算平均评分（自定义方法）
BigDecimal avgRating = commentMapper.calculateAverageRating(productId);
```

## 总结

- **Mapper 接口看起来"空"是正常的**，因为 MyBatis Plus 已经提供了基础CRUD方法
- **XML 文件提供了自定义SQL**，用于复杂查询和特殊业务逻辑
- **两者结合使用**，既享受 MyBatis Plus 的便利，又保留了 MyBatis 的灵活性

