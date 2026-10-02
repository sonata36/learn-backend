# 工作室订单管理系统

当前版本：**2.1.0**。在 2.0.0 的 MyBatis、Druid、分页和 HTTP API 基础上，加入前端可视化页面及 Docker Compose 部署配置。

Java 21 + Spring Boot 4 的商品与订单记账项目。当前业务读写使用 **MyBatis**，数据库连接由 **Druid** 管理，提供 REST API 和可选的控制台菜单。MySQL 持久化数据；测试使用 H2 的 MySQL 兼容模式。原有 `dao/` 与 `util/JdbcUtil` 保留为 1.0.0 阶段的 JDBC 学习代码，当前 Service 不再调用它们。

## 准备数据库

1. 在 IDEA 的数据库控制台或 MySQL 客户端执行 [建表脚本](src/main/resources/db/schema-mysql.sql)，创建 `studio_order` 和三张表。
2. 在 IDEA 的运行配置中设置 `DB_USERNAME`、`DB_PASSWORD`；可选 `DB_URL`。不要将密码写入仓库。
3. 使用 JDK 21 运行 `LearnBackendApplication`，或在项目目录执行：

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的 MySQL 密码"
.\mvnw.cmd spring-boot:run
```

默认启动 HTTP 服务，地址为 `http://localhost:8080`。若还想使用旧控制台菜单，在运行配置中加入 `APP_CONSOLE_ENABLED=true`，或在命令行加 `--app.console.enabled=true`。控制台菜单会占用标准输入。

## 网页界面

启动后在浏览器打开 `http://localhost:8080/`。页面提供总览、商品新增/修改/停用、商品排序分页、订单创建/修改/删除、订单排序分页和明细查看。页面使用浏览器原生 HTML、CSS、JavaScript，无需安装 Node.js 或单独构建前端。

前端文件位于 `src/main/resources/static/`，构建时会进入 Spring Boot JAR。页面用相对于当前站点的 `api/` 地址访问后端；之后放入 Docker 时可让前后端通过同一个服务端口访问，无需修改浏览器端 API 地址。数据库连接需用容器可访问的 `DB_URL`，不能在容器里把 `localhost` 当作宿主机 MySQL。

## Docker Desktop 部署

项目已提供 `Dockerfile`、`compose.yaml` 和 `.env.example`，可一起启动应用与 MySQL。PowerShell 下的配置、启动、日志、数据持久化和故障排查步骤见 [Docker 部署指南](DOCKER_DEPLOY.md)。

## HTTP 接口

请求和响应均为 JSON。列表默认第一页 `page=0`、每页 `size=20`；`size` 范围为 1～100。返回 `items`、`page`、`size`、`total`、`totalPages`。列表在数据库中使用 `LIMIT/OFFSET` 分页，订单列表只返回摘要；详情包含商品明细。

| 操作 | 路由 | 说明 |
| --- | --- | --- |
| 商品分页 | `GET /api/products?page=0&size=20&sort=idAsc` | 可用 `idAsc`、`priceAsc`、`priceDesc`、`nameAsc`、`nameDesc`；`includeInactive=true` 包含停用商品 |
| 商品详情 | `GET /api/products/{id}` | 包含停用状态 |
| 新增商品 | `POST /api/products` | JSON：`{"name":"工作灯","price":35.50}` |
| 修改商品 | `PUT /api/products/{id}` | 请求体同新增 |
| 停用商品 | `DELETE /api/products/{id}` | 成功返回 204，历史订单仍可引用 |
| 订单分页 | `GET /api/orders?page=0&size=20&sort=timeDesc` | 可用 `timeAsc`、`timeDesc`、`priceAsc`、`priceDesc` |
| 订单详情 | `GET /api/orders/{id}` | 返回完整明细 |
| 创建订单 | `POST /api/orders` | JSON：`{"items":{"1":2,"3":1}}`；键是商品 ID，值是数量 |
| 修改订单 | `PUT /api/orders/{id}` | 用新明细替换旧明细，请求体同创建 |
| 删除订单 | `DELETE /api/orders/{id}` | 成功返回 204 |

例如使用 PowerShell：

```powershell
$base = "http://localhost:8080"
$product = Invoke-RestMethod -Method Post -Uri "$base/api/products" -ContentType "application/json" -Body '{"name":"工作灯","price":35.50}'
$orderBody = @{ items = @{ "$($product.id)" = 2 } } | ConvertTo-Json -Depth 3
Invoke-RestMethod -Method Post -Uri "$base/api/orders" -ContentType "application/json" -Body $orderBody
Invoke-RestMethod -Uri "$base/api/orders?page=0&size=10&sort=timeDesc"
```

无效商品名、价格、数量、页码和排序返回 400 及 `{"error":"具体原因"}`；详情不存在返回 404。金额由服务端根据数据库商品价格计算，调用者不传总价。

## 数据与实现

- `products` 存商品当前名称、价格和启用状态；`orders` 存下单时间与总价；`order_items` 存商品编号、数量，以及成交时名称和单价的快照。
- 商品停用采用软删除，避免破坏历史订单外键。旧订单价格不会随商品调价而变化。
- `mapper/` 中的 MyBatis SQL 显式列出字段；输入值由 `#{...}` 绑定。排序字段只由枚举白名单构造，不拼接原始用户输入。
- `OrderService` 用 Spring `@Transactional` 保证订单头、明细、总价同时成功或回滚；下单时锁定并验证商品。
- `config/DruidConfig` 创建连接池，可通过 `app.datasource.initial-size`、`min-idle`、`max-active`、`max-wait` 调整。
- `web/` 暴露路由并统一返回输入错误；`service/` 负责校验与业务规则；`model/` 放领域对象和分页结果。

## 测试

```powershell
.\mvnw.cmd test
```

集成测试覆盖商品和订单 CRUD、排序、分页、Druid 数据源、HTTP 请求、商品快照、无效输入、SQL 注入防护及订单事务回滚，不需要启动本机 MySQL。

更多概念说明见 [学习指南](学习指南.md)。1.0.0 标签保留了最初仅用 JDBC 的实现，方便与进阶版对照学习。
