# 算酷博 (LiFang) 个人中心后端 API 接口文档

**版本**: v1.0.0
**更新日期**: 2026-02-22
**协议**: HTTP/1.1
**格式**: JSON
**基础路径 (Base URL)**: `/api`

---

## 1. 技术规范与约定

### 1.1 鉴权机制
所有接口需在 Request Header 中携带 JWT Token 或 Session Cookie 进行认证。
- **Header Key**: `Authorization`
- **Header Value**: `Bearer <your_token_here>`

### 1.2 通用响应结构
所有 API 返回必须遵循以下 JSON 结构：

**成功响应 (HTTP 200)**:
```json
{
  "code": 200,
  "message": "success",
  "data": { ... } // 具体业务数据对象或数组
}
```

**失败响应 (HTTP 4xx/5xx)**:
```json
{
  "code": 401, // 业务错误码
  "message": "Token expired", // 错误提示信息
  "data": null
}
```

### 1.3 业务错误码对照表
| 状态码 (Code) | 说明 |
| :--- | :--- |
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 401 | 未授权（未登录或 Token 失效） |
| 403 | 权限不足（当前角色无法执行此操作） |
| 404 | 请求资源不存在 |
| 500 | 服务器内部错误 |

---

## 2. 接口详情

### 2.1 核心用户模块 (User Core)

此模块用于页面初始化鉴权，决定前端渲染哪个角色的视图。

#### 2.1.1 获取当前登录用户信息
*   **接口名称**: Get Current User Profile
*   **接口路径**: `GET /user/me`
*   **接口描述**: 获取当前用户的基本资料、角色及权限状态。前端根据 `role` 字段判断显示 UserView, PartnerView 或 AdminView。
*   **请求参数**: 无

*   **响应参数 (`data` 对象)**:

| 字段名 | 类型 | 说明 | 示例 |
| :--- | :--- | :--- | :--- |
| id | string | 用户ID | "u_1001" |
| name | string | 用户昵称/显示名 | "张三" |
| email | string | 邮箱 | "zhangsan@lifang.com" |
| role | string | 用户角色 | "user" \| "partner" \| "admin" |
| avatar | string | 头像链接 | "https://cdn.../avatar.png" |
| joinDate | string | 注册时间 (ISO8601) | "2024-01-15T10:00:00Z" |

*   **响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "u_1001",
    "name": "张三",
    "email": "zhangsan@lifang.com",
    "role": "admin",
    "avatar": "https://example.com/a.jpg",
    "joinDate": "2024-01-15"
  }
}
```

---

### 2.2 普通用户业务模块 (Role: User)

仅当 `role = 'user'` 时调用。

#### 2.2.1 获取客户台账（我的订单）
*   **接口名称**: Get Customer Orders
*   **接口路径**: `GET /user/orders`
*   **接口描述**: 获取当前登录用户（作为客户）的所有采购合同订单列表。
*   **请求参数**:
    *   `page` (int, optional): 页码，默认1
    *   `pageSize` (int, optional): 每页数量，默认20
    *   `status` (string, optional): 状态筛选 (`active`, `expired`)

*   **响应参数 (`data` 数组 - CustomerOrder)**:

| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | string | 订单唯一ID |
| customerName | string | 客户名称（用户所属企业） |
| productName | string | 合作产品名称 |
| productQuantity | number | 产品数量 |
| startTime | string | 合同开始时间 (YYYY-MM-DD) |
| endTime | string | 合同结束时间 (YYYY-MM-DD/文本) |
| amount | number | 单笔/周期金额 |
| totalAmount | number | 订单总金额 |
| paymentMethod | string | 付款方式 (如: "月付", "季付") |
| contact | string | 我方对接人 |
| remarks | string | 备注/特殊需求 |
| status | string | 状态 (`active` \| `pending` \| `expired`) |

*   **响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "ord_001",
      "customerName": "武汉理工大学",
      "productName": "4090*8服务器",
      "productQuantity": 1,
      "startTime": "2025/05/01",
      "endTime": "无固定期限",
      "amount": 8400.00,
      "totalAmount": 100800.00,
      "paymentMethod": "月付",
      "contact": "Spring",
      "remarks": "每月27日左右提醒付款",
      "status": "active"
    }
  ]
}
```

---

### 2.3 合作伙伴业务模块 (Role: Partner)

仅当 `role = 'partner'` 时调用。

#### 2.3.1 获取供应商台账及统计
*   **接口名称**: Get Partner Orders & Stats
*   **接口路径**: `GET /partner/orders`
*   **接口描述**: 获取供应商视角的订单列表及顶部的统计概览数据。

*   **响应参数 (`data` 对象)**:

| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| stats | object | 统计概览对象 |
| └ monthSettlement | string | 本月结算金额 (格式化金额) |
| └ activeContracts | number | 活跃供应合同数 |
| list | array | 订单列表详情 (PartnerOrder) |
| └ id | string | 订单ID |
| └ supplierName | string | 供应商名称 |
| └ dateStart | string | 开始日期 |
| └ dateEnd | string | 结束日期 |
| └ settlementAmount | string | 结算金额 |
| └ paymentMethod | string | 付款方式 |
| └ contact | string | 对接人 |

*   **响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "stats": {
      "monthSettlement": "45,000.00",
      "activeContracts": 12
    },
    "list": [
      {
        "id": "p_ord_001",
        "supplierName": "英伟达核心代理",
        "dateStart": "2024-01-01",
        "dateEnd": "2025-01-01",
        "settlementAmount": "12,000.00",
        "paymentMethod": "季付",
        "contact": "李四"
      }
    ]
  }
}
```

---

### 2.4 管理员后台模块 (Role: Admin)

仅当 `role = 'admin'` 时调用，需严格鉴权 (403 Forbidden)。

#### 2.4.1 获取所有用户列表
*   **接口路径**: `GET /admin/users`
*   **响应参数**: (`data` 数组 - AdminUser)
    *   `id`, `name`, `email`, `role`, `joinDate`

#### 2.4.2 获取全平台订单（双边台账）
*   **接口路径**: `GET /admin/orders`
*   **接口描述**: 获取用于管理员“订单管理”Tab 的数据，需包含“客户视角”和“供应商视角”的双向信息。
*   **响应参数 (`data` 数组 - AdminOrder)**:

| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | string | 订单ID |
| customerName | string | 客户名称 |
| customerProduct | string | 客户侧-购买产品 |
| customerStartDate | string | 客户侧-开始时间 |
| customerEndDate | string | 客户侧-结束时间 |
| customerPaymentMethod | string | 客户侧-付款方式 |
| customerContact | string | 客户侧-对接人 |
| supplierName | string | 供应方名称 |
| supplierStartDate | string | 供应方-开始时间 |
| supplierEndDate | string | 供应方-结束时间 |
| supplierPaymentMethod | string | 供应方-付款方式 |
| supplierContact | string | 供应方-对接人 |

*   **响应示例**:
```json
{
  "code": 200,
  "data": [
    {
      "id": "adm_01",
      "customerName": "北京航空航天大学",
      "customerProduct": "A100集群",
      "customerContact": "张经理",
      "supplierName": "UCloud",
      "supplierStartDate": "2024-02-01",
      "supplierContact": "王技术"
    }
  ]
}
```

#### 2.4.3 获取审核列表
*   **接口路径**: `GET /admin/audits`
*   **描述**: 获取待审核的申请。
*   **响应参数**: (`data` 数组 - AuditRequest)
    *   `id`, `userName`, `applyTime`, `status`, `reason`

#### 2.4.4 处理审核 (通过/驳回)
*   **接口路径**: `POST /admin/audits/{auditId}/decision`
*   **请求参数**:
    ```json
    {
      "status": "approved", // or "rejected"
      "rejectReason": "资料不全" // optional
    }
    ```
