# 接口文档

统一响应结构：

```json
{ "code": 0, "message": "ok", "data": { } }
```

- `code = 0`：成功
- `code != 0`：与 HTTP 状态码一致（400/401/403/404/409/500）

## Todo

| 方法 | 路径 | 说明 | 请求体 |
|------|------|------|--------|
| POST | `/api/todos` | 创建 Todo | `{ "title": "必填(≤100)", "content": "可选(≤5000)" }` |
| GET | `/api/todos` | 列表（按创建时间倒序） | - |
| GET | `/api/todos/{id}` | 详情 | - |
| PUT | `/api/todos/{id}` | 更新（字段可选，非 null 合并） | `{ "title": "?", "content": "?", "done": true }` |
| DELETE | `/api/todos/{id}` | 删除 | - |

错误：`400`（参数非法）、`404`（不存在）。

## 认证 / 用户

| 方法 | 路径 | 说明 | 请求体 |
|------|------|------|--------|
| POST | `/api/auth/register` | 注册 | `{ "username", "email", "password" }` |
| POST | `/api/auth/login` | 登录（用户名或邮箱） | `{ "login", "password" }` |
| POST | `/api/auth/logout` | 退出（阶段 3 清理凭据） | - |
| GET | `/api/users/me` | 查自己信息 | - |
| PUT | `/api/users/me` | 改邮箱 | `{ "email" }` |
| PUT | `/api/users/me/password` | 改密码 | `{ "oldPassword", "newPassword" }` |

> 阶段 2 的「当前用户」临时用 `X-User-Id` 请求头表示；阶段 3 替换为 Cookie / Session / Token。

### 注册校验规则

- `username`：必填，3–50 位，仅字母/数字/下划线，唯一
- `email`：必填，合法邮箱，≤100，唯一
- `password`：必填，8–72 位（BCrypt 只存散列）

### 错误码约定

| code | 含义 |
|------|------|
| 400 | 参数校验失败（含具体字段信息） |
| 401 | 未登录 / 用户名或密码错误 / 凭据无效 |
| 403 | 账号被禁用 / 无权限 |
| 404 | 资源不存在（越权访问他人资源也返回 404，不暴露存在性） |
| 409 | 用户名 / 邮箱已存在（唯一约束冲突） |
| 500 | 服务器内部错误（不泄露内部细节） |
