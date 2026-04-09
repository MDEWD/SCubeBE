# JWT Token 持久化解决方案

## 问题描述

登录后刷新页面，需要重新登录。这是因为 JWT token 只存储在内存中，刷新页面后丢失。

## 解决方案

### 后端（已实现 ✅）

后端已经正确实现，无需修改：

1. **JWT Token 生成**：登录成功后返回 token（有效期7天）
2. **Token 验证**：从 `Authorization` header 中读取 token 并验证
3. **用户信息获取**：`GET /api/user/get/login` 接口需要 token 验证

### 前端需要实现

前端需要做以下修改：

#### 1. 存储 Token 到 localStorage

登录成功后，将 token 存储到 `localStorage`：

```javascript
// 登录成功后
const loginResponse = await login(loginData);
if (loginResponse.code === 0) {
  const token = loginResponse.data.token;
  
  // 存储到 localStorage
  localStorage.setItem('token', token);
  
  // 同时存储用户信息（可选）
  localStorage.setItem('user', JSON.stringify(loginResponse.data.user));
}
```

#### 2. 应用启动时恢复 Token

在应用启动时（如 `App.js` 或 `main.js`），从 `localStorage` 读取 token 并设置到请求拦截器中：

```javascript
// 使用 axios 的示例
import axios from 'axios';

// 从 localStorage 读取 token
const token = localStorage.getItem('token');

// 如果存在 token，设置到请求头
if (token) {
  axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
}
```

#### 3. 请求拦截器自动添加 Token

设置 axios 请求拦截器，自动为每个请求添加 token：

```javascript
import axios from 'axios';

// 请求拦截器
axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);
```

#### 4. 响应拦截器处理 Token 过期

设置响应拦截器，处理 token 过期的情况：

```javascript
import axios from 'axios';

// 响应拦截器
axios.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    // 如果 token 过期或无效（401错误）
    if (error.response && error.response.status === 401) {
      // 清除本地存储的 token
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      
      // 跳转到登录页
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

#### 5. 退出登录时清除 Token

退出登录时，清除 localStorage 中的 token：

```javascript
const logout = () => {
  // 清除 token 和用户信息
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  
  // 清除 axios 的默认 header
  delete axios.defaults.headers.common['Authorization'];
  
  // 跳转到登录页
  window.location.href = '/login';
};
```

## 完整的前端实现示例

### React + Axios 示例

```javascript
// utils/request.js
import axios from 'axios';

const request = axios.create({
  baseURL: 'http://localhost:8077/api',
  timeout: 10000,
});

// 请求拦截器：自动添加 token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器：处理 token 过期
request.interceptors.response.use(
  (response) => {
    return response.data; // 直接返回 data
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      // Token 过期或无效
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default request;
```

```javascript
// services/auth.js
import request from '../utils/request';

export const login = (loginData) => {
  return request.post('/user/login', loginData);
};

export const getCurrentUser = () => {
  return request.get('/user/get/login');
};
```

```javascript
// App.js
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { login, getCurrentUser } from './services/auth';

function App() {
  const navigate = useNavigate();
  
  useEffect(() => {
    // 应用启动时检查是否有 token
    const token = localStorage.getItem('token');
    
    if (token) {
      // 验证 token 是否有效（可选，也可以直接使用）
      getCurrentUser()
        .then((user) => {
          // Token 有效，用户已登录
          console.log('用户已登录:', user);
        })
        .catch((error) => {
          // Token 无效，清除并跳转登录
          localStorage.removeItem('token');
          navigate('/login');
        });
    } else {
      // 没有 token，跳转登录
      navigate('/login');
    }
  }, [navigate]);
  
  // ... 其他代码
}
```

```javascript
// pages/Login.js
import { useState } from 'react';
import { login } from '../services/auth';

function Login() {
  const [code, setCode] = useState('');
  const [openId, setOpenId] = useState('');
  
  const handleLogin = async () => {
    try {
      const response = await login({
        code: code,
        openId: openId,
      });
      
      if (response.code === 0) {
        // 存储 token 和用户信息
        localStorage.setItem('token', response.data.token);
        localStorage.setItem('user', JSON.stringify(response.data.user));
        
        // 跳转到首页
        window.location.href = '/';
      }
    } catch (error) {
      console.error('登录失败:', error);
    }
  };
  
  // ... 其他代码
}
```

## 后端接口说明

### 1. 登录接口

**接口：** `POST /api/user/login`

**请求体：**
```json
{
  "code": "123456",
  "openId": "oUpF8uMuAJO_M2pxb1Q9zNjWeS6o",
  "nickname": "用户昵称（可选）",
  "avatar": "头像URL（可选）"
}
```

**响应：**
```json
{
  "code": 0,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "openId": "oUpF8uMuAJO_M2pxb1Q9zNjWeS6o",
      "nickname": "用户昵称",
      "avatar": "头像URL",
      "userRole": "USER"
    }
  }
}
```

### 2. 获取当前用户信息

**接口：** `GET /api/user/get/login`

**请求头：**
```
Authorization: Bearer {token}
```

**响应：**
```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "id": 1,
    "openId": "oUpF8uMuAJO_M2pxb1Q9zNjWeS6o",
    "nickname": "用户昵称",
    "avatar": "头像URL",
    "userRole": "USER"
  }
}
```

**错误响应（token 无效）：**
```json
{
  "code": 40100,
  "message": "未登录",
  "data": null
}
```

## 测试步骤

1. **登录测试**：
   - 调用登录接口
   - 检查 `localStorage` 中是否有 `token`
   - 检查请求头中是否包含 `Authorization: Bearer {token}`

2. **刷新页面测试**：
   - 登录后刷新页面
   - 检查是否自动从 `localStorage` 读取 token
   - 检查是否自动设置到请求头
   - 检查用户是否仍然处于登录状态

3. **Token 过期测试**：
   - 修改 token 使其无效
   - 调用需要认证的接口
   - 检查是否返回 401 错误
   - 检查是否自动清除 token 并跳转登录

## 注意事项

1. **安全性**：
   - `localStorage` 中的 token 可能被 XSS 攻击窃取
   - 生产环境建议使用 `httpOnly` cookie（需要后端支持）
   - 或者使用更安全的存储方式

2. **Token 有效期**：
   - 当前配置为 7 天（604800000 毫秒）
   - 可以在 `application.yml` 中修改 `jwt.expiration`

3. **自动刷新 Token**：
   - 如果需要在 token 快过期时自动刷新，需要实现 refresh token 机制
   - 当前实现中，token 过期后需要重新登录

## 总结

- ✅ **后端**：已正确实现，无需修改
- ⚠️ **前端**：需要实现 token 持久化（存储到 localStorage）和自动恢复（应用启动时读取）

按照上述步骤实现后，刷新页面就不会丢失登录状态了。

