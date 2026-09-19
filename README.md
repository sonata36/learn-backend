# learn-backend

第二轮考核（Web 基础与用户域）项目：**Java + Spring Boot** 实现 TodoList 分层、用户注册/登录/权限，以及 Cookie / Session / Token 三种认证方案的渐进实验，最后用 MySQL 持久化。

## 技术栈

| 组件 | 版本/选型 | 说明 |
|------|-----------|------|
| Java | 17 (LTS) | |
| Spring Boot | 4.0.8 | 注意：Boot 4 把 `spring-boot-starter-web` 改名成了 `spring-boot-starter-webmvc` |
| 数据访问 | MyBatis（`mybatis-spring-boot-starter` 4.0.1） | 贴 SQL，能看清实际执行语句 |
| 数据库 | MySQL 8.0（Docker Compose） | |
| 迁移 | Flyway（`db/migration`） | |
| 密码散列 | `spring-security-crypto`（BCrypt） | 只取 BCrypt 工具，不启用 Spring Security 过滤链 |
| 参数校验 | `spring-boot-starter-validation` | `@Valid` / `@NotBlank` 等 |

## 快速开始

```bash
# 1. 启动 MySQL（首次会拉取镜像）
docker compose up -d

# 2. 启动应用（Flyway 自动建表）
./mvnw spring-boot:run        # macOS/Linux
mvnw.cmd spring-boot:run      # Windows
```

启动后：

- 应用：http://localhost:8080
- MySQL：`localhost:3307`，库 `learn_backend`，账号 `app/app123456`（仅本地开发默认值）
  - 说明：本机 3306 已被本地 MySQL 服务占用，因此 Docker 映射到 **3307**；`application.yml` 的 url 已同步为 3307。

## 项目结构

```
learn-backend/
├─ compose.yaml                          # MySQL 编排
├─ pom.xml
├─ README.md
├─ docs/                                 # api.md / db-design.md / structure.md / 学习记录.md
└─ src/main/
   ├─ java/com/learn/learnbackend/
   │  ├─ LearnBackendApplication.java    # 入口 + @MapperScan
   │  ├─ common/                         # ApiResponse / ErrorCode / BusinessException / GlobalExceptionHandler
   │  ├─ todo/                           # TodoList 模块（entity/dto/repository/service/controller）
   │  ├─ user/                           # 用户模块（entity/dto/repository/service/controller）
   │  └─ auth/                           # 认证模块（CurrentUser + 阶段 3 的 cookie/session/token）
   └─ resources/
      ├─ application.yml
      ├─ mapper/                         # 复杂 SQL 的 MyBatis XML
      └─ db/migration/                   # Flyway：V1__init_todo.sql / V2__init_user.sql
```

## 分层与解耦

调用链：`Controller → Service → Repository → MySQL`。

- **Controller**：只接参数、`@Valid` 校验、调 Service、返回 `ApiResponse`，绝不写 SQL。
- **Service**：只写业务规则，不依赖 `HttpServletRequest` / `HttpServletResponse`。
- **Repository**：只做数据读写（MyBatis `@Mapper`），不决定业务流程。
- **模块边界**：Todo 模块只依赖 `auth.CurrentUser`（仅 id、username 的身份对象）这一稳定输入，
  不碰用户模块的内部表/`UserRepository`；用户模块也不反向依赖 Todo 模块。

统一响应结构：

```json
{ "code": 0, "message": "ok", "data": { } }
```

## 数据访问选型：MyBatis（而不是 JPA / MyBatis-Plus）

- **是否手写 SQL**：简单 CRUD 用注解 SQL，复杂查询放 `resources/mapper/` 的 XML。
- **为何适合本项目**：更贴近 SQL，能看清实际执行语句，符合「掌握 SQL」的考核要求。
- **事务**：Service 层 `@Transactional`。
- **观察 SQL**：`mybatis.configuration.log-impl=StdOutImpl`（`application.yml` 已开启）。
- **放弃了什么**：相比 ORM（JPA）自动映射，需手写一些重复 SQL 与实体字段映射；
  相比 MyBatis-Plus，少了内置的通用 CRUD 封装。

## 认证演进路线（阶段 3）

Cookie、Session、Token 不是三个互斥名词，Cookie 是「浏览器自动携带的载体」，常用来装 Session ID 或 Token。

| 维度 | 签名 Cookie | Cookie + Session | Token（JWT） |
|------|------------|------------------|-------------|
| 状态位置 | 客户端 | 服务端 | 客户端 |
| 验证流程 | 验签 + 过期 | Session ID 查服务端 | 验签 + 过期 + 声明 |
| 退出 | 删 Cookie，已签发仍可重放 | 删服务端 Session，立即失效 | 无状态，无法真正吊销 |
| CSRF | 高 | 高 | 低（Header 携带免疫） |

实现细节见 `src/main/java/com/learn/learnbackend/auth/{cookie,session,token}/package-info.java`。

## 当前进度（如实标注）

- [x] 阶段 0：项目骨架 + 依赖 + MySQL 编排 + Flyway
- [x] 阶段 1：TodoList 分层 + MySQL 持久化（`/api/todos` CRUD）
- [x] 阶段 2a：用户注册 / 登录（仅校验账密）/ 资料 / 改密（BCrypt，`spring-security-crypto`）
- [ ] 阶段 2b：Todo 归属当前用户 + 越权防护（`CurrentUser` 已就位，待把 userId 绑定进 todo）
- [ ] 阶段 3：Cookie / Session / Token 三种认证方案实验（目录与测试占位已就位）
- [ ] 阶段 4：选定最终方案 + 全量测试 + 文档完善

> 阶段 2 的「当前用户」目前用 `X-User-Id` 请求头临时解析（`CurrentUserResolver`），
> 阶段 3 会替换为从 Cookie / Session / Token 解析真实凭据。

## 测试

运行前先 `docker compose up -d`：

```bash
./mvnw test          # macOS/Linux
mvnw.cmd test        # Windows
```

- `TodoApiTest`：Todo 增删改查 + 非法输入 + 不存在的 id
- `UserAuthTest`：注册 / 登录 / 重复用户名 / 密码错误 / 未登录 401
- `auth/{Cookie,Session,Token}AuthTest`：阶段 3 占位，暂 `@Disabled`

## 已知问题

- 阶段 2 的登录暂不签发凭据，`/api/users/me` 需手动带 `X-User-Id` 头（阶段 3 移除该临时方案）。
- 数据库密码 `app123456`、认证密钥占位仅为本地开发默认值，生产必须用环境变量覆盖。
- 进程内 Session（阶段 3 版本二）应用重启即失效，多实例不共享（本轮不引入 Redis）。
