# smart-picture 后端

Spring Boot 3、Java 17、MyBatis-Plus、Sa-Token，使用 MySQL、Redis 和 MinIO / COS。

## 目录职责

```text
src/main/java/com/normal/picturebackend/
├── interfaces/             # HTTP 控制器、DTO、VO、assembler
├── application/            # 应用服务与用例编排
├── domain/                 # picture、space、user 业务领域
├── infrastructure/         # 持久化、配置、外部 API、安全与技术工具
├── shared/                 # 跨模块认证与 WebSocket 协作
└── NormalPictureBackendApplication.java
src/main/resources/
├── mapper/                 # MyBatis XML，namespace 与 Java Mapper 对应
├── biz/                    # 业务权限配置
├── application.yml
└── application-local.example.yml
src/test/java/              # 与生产代码包结构保持一致
```

保留现有分层与业务逻辑；基础设施包名统一为 `infrastructure`。
新用例由接口层进入应用服务，业务规则放在相应领域，数据库和外部系统的实现放在基础设施层。
当前仍存在跨层引用，目录分层不代表已经完成严格的依赖隔离。

## 开发与验证

按 [启动指引](../../README.md) 初始化数据库、配置本地环境。
数据库初始化文件位于 [database/schema.sql](../../database/schema.sql)，部署配置位于 [deploy/](../../deploy/)。

在本目录执行：

```powershell
mvn clean test
mvn package
```

启动服务时，在本地命令窗口执行：

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

接口文档：`http://localhost:8333/api/doc.html`。
`application-local.yml`、`application-prod.yml` 不进入版本控制，敏感配置通过环境变量传入。

Java 类采用 PascalCase、包名采用小写。修改包路径时同步修改 import、MapperScan、XML namespace 与测试包。
仓库规范见 [目录与开发规范](../../docs/architecture.md)。

## AI 服务对接

`infrastructure/ai/AiServiceClient` 使用 Spring `RestClient` 调用独立 Python 服务的 `GET /api/v1/info`。
业务服务通过构造器注入 `AiServiceClient`，调用 `getInfo()` 或 `getInfo(requestId)`，返回 `AiServiceInfo`。
无需增加 HTTP 客户端依赖。请求自动携带 `X-API-Key` 与规范化的 `X-Request-ID`，禁止自动重定向，不做自动重试。

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `AI_SERVICE_BASE_URL` | `http://127.0.0.1:8334` | Python 服务源地址，不含 `/api/v1`、查询参数或凭证 |
| `AI_SERVICE_API_KEY` | 空 | 与 Python 完全相同的共享密钥，至少 32 个非空白 ASCII 字符 |
| `AI_SERVICE_CONNECT_TIMEOUT_MS` | `3000` | 正整数，连接超时 |
| `AI_SERVICE_READ_TIMEOUT_MS` | `100000` | 正整数，覆盖 Python 最长 90 秒分析流程 |
| `AI_SERVICE_MINIO_ENDPOINT` | 空 | 签名时使用的 MinIO 源地址；空值复用 `storage.minio.endpoint` |
| `AI_SERVICE_IMAGE_URL_EXPIRY_SECONDS` | `120` | 图片 GET 签名有效期，允许 30–600 秒 |

Python 的 `.env` 不会被 Java 自动读取。在启动 Spring Boot 的本地 PowerShell 窗口中设置上述环境变量，例如：

```powershell
$env:AI_SERVICE_BASE_URL = 'http://127.0.0.1:8334'
$env:AI_SERVICE_API_KEY = '<same-key-as-python>'
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

没有配置密钥时，原有业务可正常启动，调用 AI 接口会返回明确配置错误。
连接失败、超时、Python 非 2xx 状态、业务失败以及不合规响应统一转换为 `BusinessException(OPERATION_ERROR)`。
不向前端暴露 Python 原始错误体或密钥。沿用现有全局异常处理约定：业务失败的响应体 `code=50001`，HTTP 状态可能仍为 200。

完整业务服务运行并以管理员登录后，可访问 **`GET /api/ai/info`**。
它使用现有 `@AuthCheck` 管理员鉴权，与直接访问 Python 的共享密钥认证相互独立。
可传入 `X-Request-ID`，响应头会返回实际使用的请求 ID，两端日志使用同一 ID。
成功响应示例：

```json
{"code":0,"data":{"name":"smart-picture-ai-service","version":"0.1.0","capabilities":[]},"message":"ok"}
```

### 独立验证调用链

准备两个本地 PowerShell 命令窗口。窗口一从仓库根目录执行：

```powershell
Set-Location apps/ai-service
uv sync --locked
$env:AI_SERVICE_API_KEY = '<your-private-shared-key-at-least-32-characters>'
uv run --locked uvicorn app.main:create_app --factory --host 127.0.0.1 --port 18334 --no-access-log
```

窗口二从仓库根目录执行，密钥必须与窗口一完全相同：

```powershell
$env:AI_SERVICE_API_KEY = '<same-key-as-window-one>'
$env:AI_SERVICE_BASE_URL = 'http://127.0.0.1:18334'
$env:AI_SERVICE_LIVE_TEST = 'true'
mvn -f apps/backend/pom.xml "-Dtest=AiServiceLiveTest" test
Remove-Item Env:AI_SERVICE_LIVE_TEST
```

`AiServiceLiveTest` 以隔离的 Spring Boot 上下文加载生产客户端配置，通过真实 HTTP 调用 Python。
不需要 MySQL、Redis、MinIO，也不修改 `.env` 或 `application-local.yml`。测试完成后在窗口一按 Ctrl+C 停止临时服务。
两端日志中应出现 `springboot-live-info-001`，且 Maven 报告该测试通过。
该测试验证 Java 客户端到 Python 的链路，不覆盖完整业务 Web 服务或管理员登录链路。

普通 `mvn test` 默认跳过真实服务测试；需要对接已运行的 Python 时，设置以上服务配置和
`$env:AI_SERVICE_LIVE_TEST = 'true'`，再执行 `mvn "-Dtest=AiServiceLiveTest" test`。

## 图片标题与标签分析

登录后调用 `POST /api/picture/analyze`，请求体只需 `{"pictureId":"图片ID"}`，可传 `X-Request-ID`。
公共图库仅作者或系统管理员可分析；空间图片按数据库中的所属空间校验 `picture:edit` 权限，团队浏览者不能分析。
空间 ID、存储 URL 和角色均由服务端查询，不使用前端传入的同名字段决定权限。

Java 优先选择原图 `originUrl`，缺失时使用 `url`，仅接受配置的 MinIO 桶中的对象。
无论桶是否公共读，都重新签发短期 GET 地址；不会直接复用数据库中可能过期的签名。
目前不支持 COS 或任意外部图片 URL，无法识别的存储会明确拒绝，不回退到当前存储。
Python 直接从 MinIO 下载，Java 仅传签名 URL；签名不返回前端，也不写入分析结果。

成功响应示例：

```json
{"code":0,"data":{"title":"夕阳下的海边","tags":["夕阳","海边"]},"message":"ok"}
```

接口只返回建议，不自动写数据库。前端用户确认后再使用现有图片编辑接口，将 `title` 映射为图片 `name`，并保存 `tags`。
未配置 DeepSeek 密钥时返回业务错误 `50001` 和明确配置提示；密钥仅需填写 Python `.env` 中的 `AI_SERVICE_DEEPSEEK_API_KEY`。
分析为同步调用，前端或网关请求超时应大于 Java 配置的读取超时。模型费用、账户额度和识别效果须在填写真实密钥后验证。

真实 MinIO 下载验证（Windows、本地默认 MinIO 凭证、AI 服务 uv 环境已安装）：

```powershell
$env:AI_STORAGE_LIVE_TEST = 'true'
mvn -f apps/backend/pom.xml "-Dtest=AiStorageLiveTest" test
Remove-Item Env:AI_STORAGE_LIVE_TEST
```

该测试创建临时图片，通过生产签名器生成地址，再调用 Python 真实下载和预处理；完成后删除临时对象，不调用 DeepSeek。
