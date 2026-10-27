# 目录与开发规范

## 仓库边界

| 目录 | 职责 |
| --- | --- |
| `apps/frontend` | 前端源码、依赖锁文件、Vite / TypeScript / ESLint 配置 |
| `apps/backend` | 后端源码、测试、资源文件、Maven 配置 |
| `apps/ai-service` | FastAPI 内部 AI 服务、Python 配置、uv 依赖锁文件与测试 |
| `database` | 数据库初始化与后续数据库变更脚本 |
| `deploy` | Docker Compose 等运行基础设施配置 |
| `docs` | 架构、开发维护说明与历史分析 |

根目录保留项目入口和仓库级规范，应用专用配置随应用维护。各应用独立安装、构建和部署，不额外引入工作区管理工具。

## AI 服务边界

前端通过 Spring Boot 访问 AI 能力，Spring Boot 通过 HTTP 调用内部 FastAPI 服务。
Java 负责认证授权、空间与图片业务、额度和业务结果入库；Python 负责 AI 执行及模型适配。
Python 不直接修改 Java 业务表。服务间使用 `X-API-Key` 认证、`X-Request-ID` 追踪请求。
图片分析由 Java 校验编辑权限并签发 MinIO 临时 GET 地址，Python 下载并预处理后调用 DeepSeek，结果作为建议返回，不自动入库。
未配置模型密钥时能力列表为空、分析返回明确配置错误；具体接口与运行方式见 [AI 服务说明](../apps/ai-service/README.md)。

## 前端文件归属

- 路由入口放在 `pages/<业务>/XxxPage.vue`，基础页面可直接放在 `pages/`。
- 可复用业务组件放在 `components/<业务>/`，无具体业务归属的组件放在 `components/common/`。
- 导航、侧栏等框架组件放在 `layouts/components/`。
- 模块路由放在 `router/modules/`，权限守卫在 `access/`，状态在 `stores/`。
- `api/` 用于生成代码，手工补充的类型放在 `types/`；避免将业务组件放入 `utils/`。
- Vue 组件使用 PascalCase，页面使用 `Page.vue` 后缀；跨目录导入使用 `@/`。

## 后端文件归属

- `interfaces`：HTTP 接口、输入输出模型、转换器。
- `application`：用例编排与应用服务。
- `domain`：按 picture / space / user 组织实体、业务规则及仓储接口。
- `infrastructure`：仓储实现、Mapper、外部服务、安全、技术配置和工具。
- `shared`：现有跨模块授权及实时协作功能；新增代码优先归属具体业务或技术模块。
- 测试镜像对应生产代码包；MyBatis XML 放在 `resources/mapper/`。

这次调整统一物理目录与命名，保留现有依赖关系，不宣称实现严格的 DDD 依赖隔离。

## 本次迁移映射

| 原位置 | 新位置 |
| --- | --- |
| `smart-team-frontend/` | `apps/frontend/` |
| `smart-teamwork-platform/` | `apps/backend/` |
| 根目录两份分析文档 | `docs/maintenance.md`、`docs/roadmap.md` |
| 根目录 MinIO Compose | `deploy/docker-compose.infra.yml`（统一基础服务） |
| 后端 `sql/create_table_sql.sql` | `database/schema.sql` |
| 前端 `src/page/`、`src/views/` | 统一为 `src/pages/` |
| `PicutreSearchForm.vue` | `components/picture/PictureSearchForm.vue` |
| `UserInfoCenter.vue`、`MySpace.vue`、`AboutView.vue` | `UserInfoPage.vue`、`MySpacePage.vue`、`AboutPage.vue` |
| `pages/admin/SpaceUserManagePage.vue` | `pages/space/SpaceUserManagePage.vue` |
| 后端 `infrastucture` 包 | `infrastructure` 包（包括测试、扫描配置及 XML） |

IDE、外部 CI 或个人启动脚本如果保存了旧工作目录，需要更新到 `apps/frontend` / `apps/backend`。
接口地址、前端路由 URL、数据库表名与 Maven 产物坐标保持原有值。
Compose 显式保留 `smart-picutre` 项目名；曾使用自定义 `-p` 的环境继续使用原值，保持原数据卷归属。

## 提交前验证

在仓库根目录执行：

```powershell
mvn -f apps/backend/pom.xml clean test
pnpm --dir apps/frontend build
uv run --project apps/ai-service --locked pytest apps/ai-service/tests
uv run --project apps/ai-service --locked ruff check apps/ai-service
git diff --check
```

移动 Java 包后必须使用 clean，避免旧 class 文件掩盖引用问题。依赖与构建产物、本地配置及日志均不提交。
