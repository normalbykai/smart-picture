# smart-picture

智能云图库 / 团队图片协作平台。前后端在同一仓库维护，各自独立安装、构建和部署。

## 目录导航

```text
smart-picture/
├── apps/
│   ├── backend/            # Spring Boot 后端（Maven）
│   └── frontend/           # Vue 3 前端（pnpm + Vite）
├── database/
│   └── schema.sql          # 数据库初始化脚本
├── deploy/
│   └── docker-compose.minio.yml
├── docs/
│   ├── architecture.md     # 目录职责、规范与迁移说明
│   ├── maintenance.md      # 历史优化记录
│   └── roadmap.md          # 历史分析与后续方向
└── README.md
```

## 本地开发

准备 Java 17+、Maven、Node.js 22、pnpm、MySQL 和 Redis。以下命令从仓库根目录执行。

### 1. 数据库与对象存储

首次使用时，创建与后端配置一致的 `normal_picture` 数据库（utf8mb4），选中该数据库后，通过 MySQL 客户端执行 [database/schema.sql](database/schema.sql)。已有数据库不要重复初始化。

```powershell
docker compose -f deploy/docker-compose.minio.yml up -d
```

MinIO S3 接口为 `http://127.0.0.1:9000`，控制台为 `http://127.0.0.1:9001`；本地默认账号和密码为 `minioadmin`。
Compose 保留原项目名 `smart-picutre`，避免移动文件后创建新的数据卷。此前使用自定义 `-p` 项目名的环境，请继续传入原值。

### 2. 后端

首次配置时复制模板，已有本地配置无需覆盖：

```powershell
Copy-Item apps/backend/src/main/resources/application-local.example.yml apps/backend/src/main/resources/application-local.yml
```

根据模板设置数据库、Redis、对象存储环境变量，然后在本地命令窗口执行：

```powershell
mvn -f apps/backend/pom.xml spring-boot:run "-Dspring-boot.run.profiles=local"
```

接口文档：`http://localhost:8333/api/doc.html`。详情见 [后端说明](apps/backend/README.md)。

### 3. 前端

```powershell
pnpm --dir apps/frontend install --frozen-lockfile
pnpm --dir apps/frontend dev
```

详情见 [前端说明](apps/frontend/README.md)。

## 验证与构建

```powershell
mvn -f apps/backend/pom.xml clean test
mvn -f apps/backend/pom.xml package
pnpm --dir apps/frontend build
```

后端产物位于 `apps/backend/target/`，前端产物位于 `apps/frontend/dist/`。
本地配置、密钥、依赖和构建产物不提交到 Git。

新增文件前请阅读 [目录与开发规范](docs/architecture.md)。历史记录见 [优化说明](docs/maintenance.md) 和 [路线图](docs/roadmap.md)。
