# 数据库设计

MySQL 8.0，字符集 `utf8mb4`，引擎 `InnoDB`。迁移脚本位于 `src/main/resources/db/migration/`。

## ER 关系

```
todo (N) ────── (1) user
  user_id 归属 user.id
```

> 阶段 2b 之前 todo 尚未有 user_id，届时通过新增迁移脚本 `ALTER TABLE todo ADD COLUMN user_id ...` 加入，
> 保持 Flyway 历史与开发阶段对应（已应用过的脚本不要回头改，校验失败时新增 `V3__...`）。

## todo

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| title | VARCHAR(100) | NOT NULL | 标题，有索引 |
| content | TEXT | NULL | 内容 |
| done | TINYINT(1) | NOT NULL DEFAULT 0 | 完成状态 |
| created_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | |
| updated_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | |

索引：`idx_todo_title(title)`；阶段 2b 增加 `idx_todo_user(user_id)`。

## user

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 唯一约束兜底并发重名 |
| email | VARCHAR(100) | NOT NULL, UNIQUE | 唯一约束兜底并发重复 |
| password_hash | VARCHAR(100) | NOT NULL | 只存 BCrypt 散列，绝不存明文 |
| status | TINYINT(1) | NOT NULL DEFAULT 1 | 1=启用 0=禁用 |
| created_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | |
| updated_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | |

唯一索引：`uk_user_username(username)`、`uk_user_email(email)`。

## 关键设计点

- **唯一性兜底**：username / email 的唯一性不能只靠「先查再插」，必须靠数据库唯一约束；
  并发下「先查再插」会漏，应用层捕获 `DuplicateKeyException` 转友好错误。
- **密码**：BCrypt（自带盐、抗彩虹表），不自研算法；响应与日志不含散列与明文。
- **字段映射**：开启 `map-underscore-to-camel-case`，`created_at` 自动映射到 `createdAt`。
