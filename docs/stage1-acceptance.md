# 第一阶段验收记录

验收日期：2026-09-19。结论：第一阶段 TodoList、分层与 MySQL 持久化验收通过。
里程碑标签：`stage1-todo`。

## 范围与环境

- 范围：Todo HTTP CRUD、服务端参数校验、统一错误响应、分层、数据库持久化、接口文档。
- 实测环境：Windows 11、Java 21.0.10；项目编译目标为 Java 17，本次未单独在 Java 17 运行时复验。
- Maven 3.9.16、Spring Boot 4.0.8、MyBatis starter 4.0.1、MySQL 8.0。
- 使用现有本地 MySQL（3307），Flyway 校验 V1、V2 迁移成功。未删除数据库或数据卷。
- 用户模块的已有实现保留；登录认证、Todo 用户归属及三种认证实验不计入本阶段完成范围。

## 本次修复

1. 更新 Todo 时，标题可以省略或为 null；传入的标题不允许为空字符串或全空白。
2. Windows Maven Wrapper 在普通目录没有链接目标时，不再直接索引空数组。
3. Todo 与用户测试使用当前 Spring Boot 配置的 Jackson 3 ObjectMapper。
4. 原有用户测试使用独立的随机用户名和邮箱，先断言注册成功，避免与已有开发数据冲突。

## 自动化测试

执行 `.\mvnw.cmd verify`，构建及打包成功。

| 测试集 | 结果 |
|---|---|
| TodoApiTest | 23 例通过 |
| UserAuthTest（原有用户模块回归） | 4 例通过 |
| LearnBackendApplicationTests | 1 例通过 |
| CookieAuthTest / SessionAuthTest / TokenAuthTest | 3 个占位测试跳过，属于后续阶段 |
| 合计 | 31 例，28 例通过，3 例跳过，0 失败，0 错误 |

Todo 测试覆盖创建、列表、详情、修改标题和内容、切换完成状态、删除、时间字段、部分更新、
空白及超长字段、缺少标题、非法 JSON、错误字段类型、错误路径参数、不支持的方法和不存在的记录。
非法更新后再次查询，确认原数据未被修改。接口测试通过事务回滚隔离写入。

## 真实 HTTP 与重启验收

执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-stage1.ps1
```

脚本验证以下流程，全部通过：

1. 从打包后的 JAR 启动真实应用，等待健康检查成功。
2. 经 HTTP 创建带唯一标记的 Todo，确认默认完成状态及时间字段。
3. 停止该应用进程并启动新的应用进程。
4. 经 HTTP 查询同一 ID 和列表，确认 MySQL 中已提交的数据仍存在。
5. 修改标题、内容及完成状态，验证非法输入返回 400。
6. 删除 Todo，确认后续查询、修改和删除均返回 404。
7. 清理本次创建的数据并停止测试进程。

这个流程验证的是应用进程重启后的持久化，没有重启 MySQL 容器，也没有删除数据卷。
验收脚本拒绝使用已占用的端口，只操作自身创建的记录和进程。

## 复验方式

```powershell
docker compose up -d
.\mvnw.cmd verify
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-stage1.ps1
```

自动化报告在 `target/surefire-reports/`；重启验收日志在 `target/stage1-*.log`。
`target/` 为生成目录，不提交到 Git。脚本默认端口 18080，可用 `-Port 18081` 调整。

本次只验收第一阶段。接下来进入第二阶段：真实登录凭据、个人信息保护、Todo 用户归属与越权测试。
