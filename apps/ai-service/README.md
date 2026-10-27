# Smart Picture AI Service

Python 3.11+ / FastAPI 内部服务，独立安装、运行和部署。当前仅提供服务基础设施，尚未接入模型，`capabilities` 返回空列表。

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

后续 AI 接口加入受认证保护的 `internal_router`；按功能增加 schema，将用例编排放入 `app/services/`、模型厂商适配放入 `app/providers/`，有实际功能时再创建这些模块。模型 SDK 按选定厂商添加，模型密钥与服务共享密钥分开管理。

Spring Boot 负责用户、空间权限、额度和业务结果入库；Python 负责 AI 执行，不直接修改现有业务表。图片通过受控对象地址传递。长任务应使用持久化任务状态和独立 worker/队列，避免把需要可靠执行的任务仅放在进程内后台任务中。第一版没有任务队列、数据库连接或模型推理端点。

## 验证

在本目录执行（TestClient 进程内测试，不启动监听服务）：

```powershell
uv run --locked pytest
uv run --locked ruff check .
uv run --locked ruff format --check .
```

基础实现参考 [FastAPI 配置文档](https://fastapi.tiangolo.com/advanced/settings/) 与 [测试文档](https://fastapi.tiangolo.com/tutorial/testing/)。
