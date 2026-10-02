# 使用 Docker Desktop 部署工作室订单系统

本项目使用 Docker Compose 启动两个服务：`app`（Spring Boot + 前端页面）和 `db`（MySQL 8.4）。浏览器只访问 `app`；`db` 默认不向宿主机开放 3306 端口，避免与电脑上已有 MySQL 冲突。MySQL 数据保存在命名卷 `studio_mysql_data` 中。

## 1. 准备

- 启动 Docker Desktop，确认左下角显示 **Engine running**，并使用 Linux containers。
- 在 PowerShell 中执行 `docker version` 和 `docker compose version`。如果提示找不到 `docker`，关闭并重新打开终端；仍找不到时检查 Docker Desktop 的 CLI 安装或 PATH。
- 如果 IDEA 中的 `LearnBackendApplication` 正占用 8080，请先停止它；也可以在下面的 `.env` 中把 `APP_PORT` 改为 `8081`。
- 首次构建需要下载 MySQL、Maven、JRE 镜像和 Maven 依赖，等待时间取决于网络。

## 2. 配置密码和端口

在 **项目根目录** 打开 PowerShell：

```powershell
cd C:\Users\Asus\Desktop\Java\code\learn-backend
Copy-Item .env.example .env
notepad .env
```

为 `MYSQL_ROOT_PASSWORD` 和 `MYSQL_APP_PASSWORD` 填入两个不同的强密码（建议使用较长的随机字母和数字）。`MYSQL_APP_PASSWORD` 是应用连接数据库使用的密码；留空会在配置检查时直接报错。`APP_PORT=8080` 表示浏览器使用电脑的 8080 端口；如该端口已被 IDEA 占用，设为 `8081`。

`.env` 已加入 `.gitignore` 和 `.dockerignore`，不要把真实密码提交到 Git。Compose 会从项目根目录的 `.env` 读取这些变量；`.env.example` 只作模板。

## 3. 启动

仍在项目根目录执行：

```powershell
docker compose config --quiet
docker compose up --build -d
docker compose ps
```

第一条检查 Compose 配置而不打印解析后的密码。第二条构建并启动服务，`-d` 表示后台运行。第三条应显示 `db` 为 **healthy**，`app` 为 **running**。数据库首次启动时会执行 `src/main/resources/db/schema-mysql.sql` 创建表。

在 Docker Desktop 的 **Containers** 页面也能看到这个 Compose 项目下的 `app` 和 `db` 两个容器。

## 4. 打开页面并确认数据

- `APP_PORT=8080`：打开 [http://localhost:8080/](http://localhost:8080/)
- `APP_PORT=8081`：打开 [http://localhost:8081/](http://localhost:8081/)

第一次打开时数据为空。在页面中新增一件商品、创建一笔订单，再查看订单详情。如果页面显示“连接失败”，先看日志：

```powershell
docker compose logs --tail=100 app
docker compose logs --tail=100 db
```

要持续跟踪应用日志：

```powershell
docker compose logs -f app
```

按 `Ctrl+C` 退出日志跟踪，容器仍继续运行。

## 5. 日常使用

```powershell
docker compose stop             # 暂停两个服务
docker compose start            # 重新启动
docker compose up -d --build    # 修改 Java/前端代码后重新构建并启动
docker compose down             # 停止并删除容器及网络，保留数据库卷
```

`docker compose down` 后再次执行 `docker compose up -d`，商品和订单仍在卷中。**不要对已有数据执行 `docker compose down --volumes`**：该命令会删除数据库卷和其中的数据。

## 6. 与本机 MySQL 的区别

容器内的 MySQL 是独立数据库。你原先安装在 Windows 上的 `studio_order` 数据不会自动出现在容器中；本配置会创建一套空表。若要迁移旧数据，请先备份本机数据库，再制定导入步骤，不要通过删除 Docker 卷来“重置”已有订单。

数据库初始化脚本只在 **数据库卷首次为空** 时运行。以后修改建表脚本或 `.env` 中的 MySQL 密码，不会自动改造已存在的数据库或用户；要升级数据库结构应使用迁移脚本。

## 7. 常见问题

| 现象 | 处理 |
| --- | --- |
| `docker` 不是命令 | 确认 Docker Desktop 已安装并运行，重开 PowerShell，检查 `docker version`。 |
| 拉取镜像时访问某个 `registry mirror` 并报 `EOF` | 镜像源连接中断。先查看 `docker info` 的 `Registry Mirrors`；在 Docker Desktop → Settings → Docker Engine 中移除失效的 `registry-mirrors` 项（或设为空数组），Apply & restart，再运行 `docker pull mysql:8.4`。若直连 Docker Hub 也失败，检查 Docker Desktop 的 Settings → Resources → Proxies。 |
| 8080 端口被占用 | 停止 IDEA 中的应用，或把 `.env` 的 `APP_PORT` 改为 `8081`，再执行 `docker compose up -d`。 |
| `db` 一直不是 healthy | 查看 `docker compose logs db`；确认 `.env` 中两个密码不为空。 |
| `app` 反复重启 | 查看 `docker compose logs app`，重点检查数据库连接错误。 |
| `Access denied` | 检查应用密码与数据库初始化时的密码一致；数据库卷已存在时，修改 `.env` 不会重设 MySQL 用户密码。 |
| 改了代码却看不到变化 | 执行 `docker compose up -d --build`，然后刷新浏览器。 |

配置所用文件：[Dockerfile](Dockerfile)、[compose.yaml](compose.yaml)、[.env.example](.env.example)。
