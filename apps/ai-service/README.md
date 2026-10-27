# Smart Picture AI Service

Python 3.11+ / FastAPI 内部服务，独立安装、运行和部署。提供 DeepSeek 图片标题与标签分析；未配置模型密钥时仍可启动，分析接口返回 503，`capabilities` 返回空列表。

## 本地安装和启动

安装 Python 3.11+ 和 [uv](https://docs.astral.sh/uv/getting-started/installation/)。在仓库根目录打开本地 PowerShell 命令窗口执行：

```powershell
Set-Location apps/ai-service
uv sync --locked
# 仅首次创建；已有 .env 不要覆盖。
Copy-Item .env.example .env
uv run python -c "import secrets; print(secrets.token_urlsafe(32))"
```

将生成的密钥填入 `.env` 的 `AI_SERVICE_API_KEY`。它是 Spring Boot 与本服务之间的共享凭证，不是模型厂商 API Key，不能放入前端。缺失、少于 32 字符、包含空白或非 ASCII 字符会导致启动失败。

然后在同一个本地命令窗口启动：

```powershell
uv run uvicorn app.main:create_app --factory --host 127.0.0.1 --port 8334 --reload --no-access-log
```

- 健康检查：`http://127.0.0.1:8334/health`
- Swagger：`http://127.0.0.1:8334/docs`，点击 Authorize 填入共享密钥。
- OpenAPI：`http://127.0.0.1:8334/openapi.json`

配置文件固定读取本应用根目录的 `.env`，环境变量优先。`AI_SERVICE_DOCS_ENABLED` 默认 `false`，开发模板设为 `true`；文档开启时无需鉴权，生产应关闭。服务没有配置浏览器 CORS，前端通过 Spring Boot 访问。

生产执行 `uv sync --locked --no-dev`，启动时移除 `--reload`；仅在需要容器/跨主机访问时绑定 `0.0.0.0`，由内部网络与 TLS 保护服务间传输。默认日志记录请求 ID、方法、状态码和耗时；关闭 Uvicorn access log 避免记录带查询参数的完整 URL。

## HTTP 对接约定

| 接口 | 认证 | 用途 |
| --- | --- | --- |
| `GET /health` | 无 | 检查 HTTP 服务存活，不表示模型或外部依赖可用 |
| `GET /api/v1/info` | `X-API-Key` | 获取服务版本与已实现能力，作为首次联调接口 |
| `POST /api/v1/images/analyze` | `X-API-Key` | 读取 MinIO 签名图片，调用 DeepSeek 并返回标题和标签建议 |

Spring Boot 使用独立配置保存 Base URL 与共享密钥，随每次内部请求发送 `X-API-Key`，可发送 `X-Request-ID` 串联日志。请求 ID 允许 1–64 位字母、数字、点、下划线和连字符；缺失或非法时服务生成新 ID。响应头和 JSON 中返回同一 ID。

Java 侧已封装 `AiServiceClient`，并提供管理员接口 `GET /api/ai/info`。
Java 不会自动读取本服务的 `.env`，启动时需要通过环境变量注入相同密钥。
配置、错误映射和真实 HTTP 联调步骤见 [Spring Boot AI 对接说明](../backend/README.md#ai-服务对接)。

本地联调示例（将占位符替换为 `.env` 中的密钥）：

```powershell
Invoke-RestMethod -Uri http://127.0.0.1:8334/api/v1/info -Headers @{
    'X-API-Key' = '<your-service-key>'
    'X-Request-ID' = 'springboot-check-001'
}
```

成功响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "name": "smart-picture-ai-service",
    "version": "0.1.0",
    "capabilities": []
  },
  "request_id": "springboot-check-001"
}
```

失败保留真实 HTTP 状态码（401、404、405、422、500），响应仍为以上结构，`code` 等于 HTTP 状态码、`data` 为 `null`。这是 AI 服务自己的协议，不复用 Java 业务错误码；Spring Boot 客户端应同时处理非 2xx 状态和响应体，将错误映射为业务异常。初次信息接口联调可设置连接超时 3 秒、读取超时 10 秒，后续模型接口按实际耗时单独配置。

## 目录与扩展

```text
app/
  main.py          # 应用工厂、请求追踪、异常处理
  core/config.py   # 环境配置
  core/security.py # 服务间认证
  api/routes.py    # 健康检查与版本化内部接口
  schemas.py       # 对外请求/响应模型
tests/             # 接口契约与认证测试
pyproject.toml     # 依赖与检查工具配置
uv.lock            # 可复现依赖版本
```

AI 接口加入受认证保护的 `internal_router`；图片下载、预处理及 DeepSeek HTTP 调用位于 `app/services/image_analysis.py`，对外模型位于 `app/schemas.py`。模型密钥与服务共享密钥分开管理。

Spring Boot 负责用户、空间权限和业务结果入库；Python 负责 AI 执行，不直接修改现有业务表。图片通过受控对象地址传递。当前为同步分析，没有任务队列、数据库连接、结果缓存或按用户计费额度。

## 图片智能分析配置

在本应用 `.env` 中填写 `AI_SERVICE_DEEPSEEK_API_KEY` 后重启 Python。不要填写到 Java 配置或前端：

```dotenv
AI_SERVICE_DEEPSEEK_API_KEY=
AI_SERVICE_DEEPSEEK_BASE_URL=https://api.deepseek.com
AI_SERVICE_DEEPSEEK_MODEL=deepseek-flash
AI_SERVICE_IMAGE_ALLOWED_BASE_URLS=["http://127.0.0.1:9000/smart-image/"]
AI_SERVICE_IMAGE_MAX_BYTES=10485760
AI_SERVICE_ANALYSIS_CONCURRENCY=2
```

接口请求为 `{"image_url":"Java生成的短期MinIO签名地址"}`，响应 `data` 为 `{"title":"夕阳下的海边","tags":["夕阳","海边"]}`。
仅允许配置中明确的存储源和桶路径，要求携带签名；拒绝重定向、任意外部 URL、目录穿越和非图片内容。
下载默认最多 10 MiB、总时限 20 秒。支持 JPEG、PNG、WebP，最多 2000 万像素、单边 8192 像素；按 EXIF 校正方向，缩放至最长边 1536，并转为 JPEG 去除元数据后通过 Base64 发送给模型。
模型请求使用 JSON 输出模式并关闭深度思考，读取超时 60 秒，整次分析上限 90 秒；单进程最多 2 个并发分析，超出返回 429。
不自动重试可能产生费用的生成请求。标题最多 60 字，标签 1–5 个、每个最多 12 字，去重并清理首尾空白；无效或截断输出返回 502，不伪造兜底结果。
未配置模型密钥返回 503，图片不合规返回 413/422，模型限流或繁忙返回 429，下载或模型调用超时返回 504。上游原始错误、图片内容及签名地址不写入客户端响应或常规请求日志。
`capabilities` 出现 `image-analysis` 表示已填写模型密钥，不表示已验证额度、模型权限或网络连通性。

Java 与 Python 必须能访问签名时使用的 MinIO 地址；签名生成后不得改写域名。容器部署需将签名源和允许下载的桶路径同步改为内部可达地址。
一键启动脚本会根据 Compose 端口及 `MINIO_BUCKET`（默认 `smart-image`）自动对齐这些配置；手动部署需自行配置。
模型协议参考 [DeepSeek 视觉文档](https://api-docs.deepseek.com/guides/vision/) 和 [JSON 输出文档](https://api-docs.deepseek.com/guides/json_mode/)。

## 验证

在本目录执行（TestClient 进程内测试，不启动监听服务）：

```powershell
uv run --locked pytest
uv run --locked ruff check .
uv run --locked ruff format --check .
```

基础实现参考 [FastAPI 配置文档](https://fastapi.tiangolo.com/advanced/settings/) 与 [测试文档](https://fastapi.tiangolo.com/tutorial/testing/)。
