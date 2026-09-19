# 项目结构图

## 分层调用链

```
HTTP 请求
   ↓
Controller        —— 接参数、@Valid 校验、调 Service、返回 ApiResponse（不写 SQL）
   ↓
Service           —— 业务规则（是否存在、归属校验、BCrypt、@Transactional）
   ↓
Repository        —— 纯数据读写（MyBatis @Mapper）
   ↓
MySQL
```

异常统一由 `common/GlobalExceptionHandler`（`@RestControllerAdvice`）转成 `ApiResponse`。

## 包结构

```
com.learn.learnbackend
├─ LearnBackendApplication        @SpringBootApplication + @MapperScan
├─ common                         （跨模块）
│  ├─ ApiResponse                 统一响应
│  ├─ ErrorCode                   错误码
│  ├─ BusinessException           业务异常
│  └─ GlobalExceptionHandler      全局异常处理
├─ todo                           （阶段 1）
│  ├─ controller/TodoController
│  ├─ service/TodoService + impl/TodoServiceImpl
│  ├─ repository/TodoRepository
│  ├─ entity/Todo
│  └─ dto/ CreateTodoRequest / UpdateTodoRequest / TodoResponse
├─ user                           （阶段 2）
│  ├─ controller/UserController
│  ├─ service/UserService + impl/UserServiceImpl   （BCrypt）
│  ├─ repository/UserRepository
│  ├─ entity/User
│  └─ dto/ RegisterRequest / LoginRequest / UserResponse / UpdateProfileRequest / ChangePasswordRequest
└─ auth                           （阶段 2/3）
   ├─ CurrentUser                 当前登录身份（Todo 模块唯一依赖）
   ├─ CurrentUserResolver         阶段 2 临时：X-User-Id 头解析
   ├─ AuthWebConfig               注册参数解析器
   ├─ AuthController              /api/auth/register|login|logout
   ├─ cookie/                     阶段 3 版本一：签名 Cookie
   ├─ session/                    阶段 3 版本二：Cookie + Session
   └─ token/                      阶段 3 版本三：Token（JWT）
```

## 模块边界

- Todo 只依赖 `CurrentUser`（id、username），不 import 用户模块的 `UserRepository`/`User` 内部表。
- 用户模块不反向依赖 Todo 模块。
- 删除 Todo 模块后，用户域与其他模块仍能编译运行。
