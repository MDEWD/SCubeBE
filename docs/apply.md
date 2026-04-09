# 合作伙伴申请 API 文档

> 适用范围：合作伙伴“个人/企业”资质申请（/alliance/apply）

## 1. 基础信息

- **Base URL**：`/api`
- **认证**：需要登录（Header：`Authorization: Bearer <token>`）
- **Content-Type**：`application/json`
- **统一返回结构（建议）**

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

- `code`: `0` 或 `200` 表示成功（与前端现有约定一致）

---

## 2. 提交申请（用户端）

### 2.1 接口

- **Method**：`POST`
- **Path**：`/alliance/applications`

### 2.2 请求参数（JSON）

公共字段：

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| kind | string | 是 | `person` 或 `company` |

#### kind=person（个人）

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| realName | string | 是 | 真实姓名 |
| phone | string | 是 | 联系电话 |
| idNumber | string | 是 | 身份证号 |
| idFrontImage | string | 是 | 身份证人像面图片（base64 字符串） |
| idBackImage | string | 是 | 身份证国徽面图片（base64 字符串） |

#### kind=company（企业/学校）

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| orgName | string | 是 | 企业/学校名称 |
| creditCode | string | 是 | 统一社会信用代码 |
| licenseImage | string | 是 | 营业执照图片（base64 字符串，不包含 `data:image/png;base64,` 前缀） |
| job | string | 是 | 申请人职务：`owner` / `biz` / `tech` / `other` |
| mainBusiness | string | 是 | 公司主营业务 |
| contactName | string | 是 | 联系人姓名 |
| contactMethod | string | 是 | 联系方式（手机号/微信/邮箱等） |

### 2.3 关键约束（后端实现要求）

- 后端从 token 获取当前用户：
  - 入库 `user_id`（内部用户主键）
  - **同时入库 `display_id`（用户表的 display_id 字段）**
- 新提交单据默认状态：`pending`
- 审核通过后，需要把 `user.user_role` 更新为：`PARTNER`

### 2.4 成功响应

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 123,
    "status": "pending"
  }
}
```

- `status`：`pending`（待审核）/ `approved`（通过）/ `rejected`（驳回）

---

## 3. 查询申请（用户端）

### 3.1 查询我的申请列表

- **Method**：`GET`
- **Path**：`/alliance/applications/mine`
- **Query**（可选）：`status`、`page`、`pageSize`

响应 data（示例）：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "total": 1,
    "items": [
      { "id": 123, "kind": "person", "status": "pending", "createdAt": "2026-03-07T12:00:00Z" }
    ]
  }
}
```

### 3.2 查询申请详情（我的）

- **Method**：`GET`
- **Path**：`/alliance/applications/{id}`

> 用户端只能查看自己的申请单。

---

## 4. 更新申请（用户端）

- **Method**：`PUT`
- **Path**：`/alliance/applications/{id}`
- **Body**：与提交申请一致

约束建议：
- 仅允许更新 `pending` 状态的申请
- 仅允许更新自己的申请

响应示例：

```json
{ "code": 0, "message": "ok", "data": { "id": 123, "status": "pending" } }
```

---

## 5. 删除申请（用户端）

- **Method**：`DELETE`
- **Path**：`/alliance/applications/{id}`

约束建议：
- 仅允许删除自己的申请
- 建议仅允许删除 `pending` / `rejected`

响应示例：

```json
{ "code": 0, "message": "ok", "data": true }
```

---

## 6. 审核（管理员端）

### 6.1 管理端分页查询申请

- **Method**：`GET`
- **Path**：`/alliance/admin/applications`
- **Query**（可选）：`status`、`kind`、`keyword`、`page`、`pageSize`

> `keyword` 可按 realName/orgName/phone/displayId 等字段模糊查询（按实现选取）。

### 6.2 审核通过/驳回

- **Method**：`POST`
- **Path**：`/alliance/admin/applications/{id}/audit`
- **Body**：

```json
{
  "action": "approved",
  "reason": ""
}
```

- `action`：`approved` / `rejected`
- `reason`：驳回原因（可选，驳回时建议必填）

审核通过的联动要求：
- 将申请单状态更新为 `approved`
- **同一事务内**将 `user` 表 `user_role` 更新为 `PARTNER`

响应示例：

```json
{
  "code": 0,
  "message": "ok",
  "data": { "id": 123, "status": "approved" }
}
```

---

## 7. 参数校验失败（示例）

手机号/身份证号等字段格式不合法时，建议后端直接返回 `400`：

```json
{
  "code": 400,
  "message": "手机号格式不正确",
  "data": null
}
```

---

## 8. 备注（实现建议）

- 建议后端保存：
  - 申请信息（字段 + kind）
  - 上传图片的存储地址（OSS/本地）
  - 申请状态与审核信息
  - 关联用户ID（来自 token），并存储 display_id
- 若需支持重复提交：
  - 可按用户维度限制“同一时间仅允许一个 pending 申请”，或允许覆盖更新。
