# 本地基础服务

## 一键启动全部服务（Windows）

双击 `deploy/start-services.cmd`，或在仓库根目录的本地命令窗口执行：

```powershell
.\deploy\start-services.cmd
# 无暂停模式，适合从已有命令窗口调用
.\deploy\start-services.cmd --no-pause
```

需要已启动 Docker Desktop，并安装 Java 17+、Maven、uv，将命令加入 PATH。
脚本依次启动 MySQL / Redis / MinIO、Python AI（8334）和 Spring Boot（8333），等待健康检查通过。
它会自动使用 `deploy/infra.env`（如果存在），根据 Compose 实际配置给新启动的后端注入数据库、Redis、MinIO 连接参数。
首次缺少配置时会创建被 Git 忽略的 `application-local.yml` 和包含随机 AI 密钥的 `.env`，已有文件不覆盖；两个应用使用同一密钥。
进程环境中的 `AI_SERVICE_API_KEY` 优先于 `.env`。已有数据卷的账号密码不会被 Compose 初始化变量更新，需保持配置与实际数据一致。

后端通过独立本地 `cmd.exe` 窗口在后台运行，关闭启动器不影响服务；日志位于根目录 `logs/`，按服务名和启动时间区分。
运行正常的应用会跳过，端口被其他程序占用或 AI 密钥不匹配时会明确失败，不会结束已有进程。
跳过的进程保持原有配置；修改连接参数或密钥后需先停止对应应用再运行脚本。
脚本启动检查验证 Python 认证接口和 Java 未登录鉴权响应，不创建管理员，也不替代登录后的端到端业务测试。
部分启动失败时已启动的服务会保留，修复后可重新运行；本脚本不启动前端。

入口 CMD 调用 `start-services.py` 完成配置读取和启动检查，Python 运行环境由 AI 服务的 uv 锁文件提供。

`docker-compose.infra.yml` 统一管理 MySQL、Redis 和 MinIO。需要 Docker Engine / Docker Desktop（Linux 容器）与支持 `--wait` 的 Docker Compose。
所有命令从仓库根目录执行；配置用于本地开发，端口仅绑定 `127.0.0.1`。

## 启动

```powershell
docker compose -f deploy/docker-compose.infra.yml up -d --wait --wait-timeout 180
docker compose -f deploy/docker-compose.infra.yml ps
```

首次启动需要下载镜像。`--wait` 等待健康检查通过；超时或失败时查看日志，容器不会自动回滚。
三个服务互不依赖，可并行启动。

| 服务 | 宿主机地址 | 默认账号 / 密码 | 持久化卷 |
| --- | --- | --- | --- |
| MySQL 8.4 | `127.0.0.1:3307` | `smart_picture` / `smart_picture_dev`；root 密码 `root` | `mysql-data` |
| Redis 7.4 | `127.0.0.1:6379` | 本地开发不设置密码 | `redis-data`（AOF） |
| MinIO API | `http://127.0.0.1:9000` | `minioadmin` / `minioadmin` | `minio-data` |
| MinIO 控制台 | `http://127.0.0.1:9001` | 同上 | 同上 |

MySQL 默认使用 `3307` 避免与已有本机 `3306` 冲突。默认数据库为 `normal_picture_server`，与后端模板一致。
新 MySQL 数据卷首次启动时自动执行 `database/schema.sql`；已有数据卷不会重复执行，也不会因更改初始化环境变量而修改既有账号、密码或数据库。
该 SQL 包含不可重复执行的 ALTER 语句，不要在已有数据库中直接再次运行。后续结构变更应使用迁移脚本。
MinIO bucket 由后端按 `auto-create-bucket` 配置创建，基础服务启动本身不创建 bucket。

## Spring Boot 连接

使用后端 `application-local.example.yml` 模板，在启动后端的本地 PowerShell 窗口设置：

```powershell
$env:DB_HOST = '127.0.0.1'
$env:DB_PORT = '3307'
$env:DB_NAME = 'normal_picture_server'
$env:DB_USERNAME = 'smart_picture'
$env:DB_PASSWORD = 'smart_picture_dev'
$env:REDIS_HOST = '127.0.0.1'
$env:REDIS_PORT = '6379'
$env:REDIS_PASSWORD = ''
$env:MINIO_ENDPOINT = 'http://127.0.0.1:9000'
$env:MINIO_ACCESS_KEY = 'minioadmin'
$env:MINIO_SECRET_KEY = 'minioadmin'
mvn -f apps/backend/pom.xml spring-boot:run "-Dspring-boot.run.profiles=local"
```

这些地址供宿主机上运行的应用使用。容器内应用应使用服务名和容器端口，如 `mysql:3306`、`redis:6379`、`minio:9000`。

## 自定义端口和凭证

首次复制模板，已有 `infra.env` 不要覆盖：

```powershell
Copy-Item deploy/infra.env.example deploy/infra.env
# 编辑 infra.env 后执行
docker compose --env-file deploy/infra.env -f deploy/docker-compose.infra.yml up -d --wait --wait-timeout 180
```

`infra.env` 已被 Git 忽略。使用自定义文件后，后续 Compose 命令均加上相同的 `--env-file deploy/infra.env`。
该文件不会自动加载到 Spring Boot，后端环境变量需同步调整。Redis 当前固定为无密码的本地开发配置；模板中的 `REDIS_PORT` 只改变宿主机映射端口。

## 常用操作

```powershell
# 日志（限制最近 100 行，持续跟踪）
docker compose -f deploy/docker-compose.infra.yml logs --tail 100 -f

# 仅启动部分基础服务
docker compose -f deploy/docker-compose.infra.yml up -d --wait redis minio

# 停止，保留容器和数据
docker compose -f deploy/docker-compose.infra.yml stop

# 删除容器和网络，保留持久化数据卷
docker compose -f deploy/docker-compose.infra.yml down

# 检查配置语法，不输出凭证
docker compose -f deploy/docker-compose.infra.yml config --quiet
```

不要在需要保留数据时使用 `down -v`，它会删除数据卷。

## 兼容已有 MinIO

三个服务统一定义在 `docker-compose.infra.yml` 中，保留项目名 `smart-picutre`、容器名 `smart-image-minio` 与卷键 `minio-data`。
默认实际卷名仍是 `smart-picutre_minio-data`。以前使用自定义 `-p` 项目名的环境必须继续使用同一个项目名。
如需仅启动 MinIO，执行 `docker compose -f deploy/docker-compose.infra.yml up -d --wait minio`。
MinIO 端口现在仅绑定本机；如需局域网访问，应显式调整绑定和访问控制。
