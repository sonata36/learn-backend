# 工作室订单管理系统

一个用于练习 Java、JDBC、MySQL 和事务的控制台记账程序。项目使用 Spring Boot 管理启动与数据源，但商品和订单的数据读写由项目内的 JDBC DAO 完成，没有使用 ORM 或 `SELECT *`。

## 环境

- JDK 21
- IntelliJ IDEA
- Maven Wrapper（项目自带）
- MySQL 8.x

## 数据库初始化

1. 启动 MySQL 服务。
2. 用 MySQL Workbench 或命令行执行 `src/main/resources/db/schema-mysql.sql`。脚本会创建 `studio_order` 数据库及 `products`、`orders`、`order_items` 三张表。
3. 配置连接信息。默认连接为 `localhost:3306`、数据库 `studio_order`、用户 `root`、空密码。推荐用环境变量设置账号密码，不要把本机凭据提交到 Git：

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的 MySQL 密码"
```

也可以设置 `DB_URL` 覆盖默认 JDBC URL。

## 启动

在 IDEA 中运行 `LearnBackendApplication`，或在 PowerShell 项目目录执行：

```powershell
.\mvnw.cmd spring-boot:run
```

启动后出现控制台菜单。创建订单时，按提示输入 `商品编号:数量`，多个商品用逗号分隔，例如 `1:2,3:1`。订单列表可按下单时间或总价升降序排列。

## 功能

- 商品新增、查询、更新、排序和停用。
- 订单新增、查询、更新商品明细、删除和排序。
- 订单创建时检查商品存在且启用、数量大于零；订单金额从数据库中的商品价格计算，不接受客户端传入总价。
- 订单与明细在同一 JDBC 事务内写入；发生异常时回滚。
- 订单明细保存商品名称和成交单价快照，商品后续改名或调价不会改写历史账目。
- 商品被订单引用后采用停用而非物理删除，保留外键关系和历史数据。
- 输入值使用 `PreparedStatement` 参数绑定；排序选项使用枚举白名单；SQL 显式列出查询字段。
- `JdbcUtil` 管理连接和事务；DAO 通过 try-with-resources 释放连接、语句和结果集。

## 测试

测试使用 H2 的 MySQL 兼容模式，不需要运行 MySQL 服务：

```powershell
.\mvnw.cmd test
```

覆盖商品 CRUD 和排序、特殊字符输入、价格校验、订单 CRUD、金额计算、历史快照、下单时间和价格排序、无效/停用商品拒绝、更新失败后的原订单保持，以及订单总价落库失败时主表和明细一起回滚。

## 结构

- `model`：商品、订单、明细和排序选项。
- `util/JdbcUtil`：获取 JDBC 连接及事务提交、回滚。
- `dao`：使用 JDBC 执行 SQL。
- `service`：输入校验、订单金额计算和业务事务。
- `app/ConsoleMenu`：控制台交互入口。
- `src/test`：基于 H2 的集成测试。

## 学习记录

本项目把订单拆成订单主表和订单明细表，避免把多个商品编号塞进一个字段。订单明细保存成交时的商品快照，使账单不依赖当前商品价格。创建或修改订单时，订单主表、明细和总价必须一起成功，因此需要事务；仅靠 Java 层检查不够，数据库外键和检查约束也能保护数据一致性。对于 SQL 注入，值使用占位符绑定；列名和排序方向无法当作普通参数绑定，因此通过固定枚举映射到 SQL 片段。
