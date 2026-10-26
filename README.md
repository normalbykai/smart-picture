# smart-picture

智能云图库 / 团队图片协作平台，前后端放在同一个仓库中维护。

## 目录结构

```
smart-picture/
├── smart-teamwork-platform/   # 后端：Spring Boot 3 + MyBatis-Plus + Sa-Token + MySQL/Redis/MinIO/COS
├── smart-team-frontend/       # 前端：Vue 3 + TypeScript + Vite + Ant Design Vue
├── docker-compose.minio.yml   # 本地 MinIO 编排
├── 优化说明.md                 # P0 修复、架构收口与行为变化
└── 项目分析与后续开发方向.md     # 项目分析与后续路线图
```

## 快速开始

### 1. 启动本地 MinIO

```bash
docker compose -f docker-compose.minio.yml up -d
```

- S3 接口：`http://127.0.0.1:9000`
- 管理控制台：`http://127.0.0.1:9001`
- 默认账号 / 密码：`minioadmin / minioadmin`

### 2. 启动后端

```bash
cd smart-teamwork-platform
cp src/main/resources/application-local.example.yml src/main/resources/application-local.yml
# 按需配置 DB_PASSWORD、REDIS_PASSWORD 等环境变量
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

接口文档：`http://localhost:8333/api/doc.html`

### 3. 启动前端

```bash
cd smart-team-frontend
pnpm install
pnpm dev
```

## 安全提示

- 数据库密码、Redis 密码、COS/MinIO 密钥全部通过环境变量注入，不要写入配置文件。
- `application-local.yml` 已被 `.gitignore` 忽略，模板见 `application-local.example.yml`。
- 如果曾经把真实云密钥提交到任何仓库，请立即到云控制台轮换。

## 相关文档

- [优化说明](优化说明.md)：本次 P0 修复、架构收口、行为变化与验证记录
- [项目分析与后续开发方向](项目分析与后续开发方向.md)：项目现状、问题清单与路线图
