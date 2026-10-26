# 正常图片管理系统后端

一个基于领域驱动设计(DDD)架构的图片管理后端系统，提供图片上传、管理、空间共享等功能。

## 项目简介

本项目是一个企业级的图片管理后端服务，采用现代化的技术栈和清晰的架构设计，支持用户管理、图片上传下载、空间共享、权限控制等核心功能。

## 技术架构

### 核心技术栈
- **框架**: Spring Boot 3.4.4
- **Java版本**: Java 17
- **构建工具**: Maven
- **数据库**: MySQL 8.0
- **缓存**: Redis + Caffeine
- **ORM**: MyBatis Plus 3.5.11
- **权限认证**: Sa-Token 1.44.0
- **文档**: Knife4j 4.5.0 (Swagger增强)

### 架构模式
项目采用领域驱动设计(DDD)的分层架构：

```
interfaces(接口层) → application(应用层) → domain(领域层) → infrastructure(基础设施层)
```

- **接口层**: REST API控制器、DTO、VO
- **应用层**: 应用服务、用例协调
- **领域层**: 核心业务实体、领域服务
- **基础设施层**: 数据持久化、外部服务集成
- **共享层**: 通用组件、工具类

## 核心功能

### 1. 用户管理
- 用户注册与登录
- 基于角色的权限控制(RBAC)
- 用户信息管理

### 2. 图片管理
- 图片上传与下载
- 图片元数据管理(尺寸、格式、大小等)
- 图片审核状态管理
- 批量图片操作

### 3. 空间管理
- 创建和管理图片空间
- 空间成员权限管理
- 空间图片共享

### 4. 高级功能
- 图片搜索API集成
- WebSocket实时通信
- 多级缓存架构
- 分布式会话管理
- 事件驱动架构

## 快速开始

### 环境要求
- Java 17+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.6+
- MinIO（本地开发，可选；也可以直接使用腾讯云 COS）

### 数据库初始化
1. 创建数据库
```sql
CREATE DATABASE `normal_picture` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行SQL脚本
```bash
# 在项目根目录执行
mysql -u your_username -p normal_picture < sql/create_table_sql.sql
```

### 配置文件

1. 复制配置模板：`cp src/main/resources/application-local.example.yml src/main/resources/application-local.yml`

2. **敏感信息一律通过环境变量注入**，不要写入配置文件。`application-local.yml` 已被 `.gitignore` 忽略。

```bash
# 数据库
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=normal_picture_server
export DB_USERNAME=root
export DB_PASSWORD=your_password

# Redis
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
export REDIS_PASSWORD=your_redis_password

# 本地 MinIO
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin

# 腾讯云 COS（仅在 storage.type=cos 或运行时切换时需要）
export COS_HOST=https://your-bucket.cos.ap-chongqing.myqcloud.com
export COS_SECRET_ID=your_secret_id
export COS_SECRET_KEY=your_secret_key
export COS_REGION=ap-chongqing
export COS_BUCKET=your-bucket

# 允许跨域的前端来源（生产环境改成真实域名，不要用 *）
export CORS_ALLOWED_ORIGINS=https://your-domain.com
```

> 安全提示：如果曾经把真实密钥写入过配置文件或提交到版本库，请立刻到云控制台轮换密钥。

### 对象存储：MinIO / COS 可切换

项目通过 `PictureObjectStorage` 抽象了对象存储能力，COS 与 MinIO 是两套独立实现，
上传业务只依赖抽象接口，因此可以随时切换，不需要修改上传代码。

#### 1. 启动本地 MinIO

在项目根目录（`smart-image`）执行：

```bash
docker compose -f docker-compose.minio.yml up -d
```

- S3 接口：`http://127.0.0.1:9000`
- 管理控制台：`http://127.0.0.1:9001`
- 默认账号 / 密码：`minioadmin / minioadmin`

后端首次上传时会自动创建 `storage.minio.bucket` 指定的桶，并设置公共读策略，
这样图片 URL 可以直接被浏览器访问。

#### 2. 通过配置切换

修改 `application-local.yml`：

```yaml
storage:
  type: cos   # 或 minio
```

重启后端后生效。`cos.client` 与 `storage.minio` 两套配置可以同时保留。

#### 3. 运行时切换（无需重启）

管理员登录后调用：

```http
GET  /api/storage/config
POST /api/storage/switch
Content-Type: application/json

{"type": "cos"}
```

- `GET /api/storage/config`：查看当前存储、全部存储及配置状态
- `POST /api/storage/switch`：切换当前存储，`type` 可选 `minio` / `cos`

切换只影响之后新上传的图片；已入库图片保存的是绝对 URL，历史图片仍可正常访问。
运行时切换是内存状态，重启后会恢复为 `storage.type` 的配置。

#### 4. 两种存储的差异

| 能力 | 腾讯云 COS | 本地 MinIO |
| --- | --- | --- |
| 原图存储 | 支持 | 支持 |
| 图片压缩 | 数据万象转 WebP | 保留原格式 |
| 缩略图 | 数据万象生成 | 后端 ImageIO 本地生成 512px 缩略图 |
| 宽高/格式/主色调 | 数据万象返回 | 后端读取图片计算 |
| 图片 URL | COS 域名 | MinIO 地址（或 `public-url` 配置的代理地址） |

> MinIO 没有数据万象，图片元数据和缩略图由后端在本地完成；JPEG/PNG 均支持，
> WebP 需要 JDK 额外安装 ImageIO WebP 解码器，未安装时仍可上传，只是不生成缩略图和宽高信息。

### 启动项目

```bash
# 开发环境启动
mvn spring-boot:run -Dspring.profiles.active=local

# 或者直接运行jar包
mvn clean package
java -jar target/normal-picture-backend-ddd-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### 访问API文档

项目启动后，可以通过以下地址访问API文档：
```
http://localhost:8333/api/doc.html
```

## 项目结构

```
normal-picture-backend-ddd/
├── src/main/java/com/normal/picturebackend/
│   ├── application/          # 应用层
│   │   ├── dto/             # 数据传输对象
│   │   ├── service/         # 应用服务
│   │   └── utils/           # 工具类
│   ├── domain/               # 领域层
│   │   ├── picture/         # 图片域
│   │   ├── space/           # 空间域
│   │   └── user/            # 用户域
│   ├── infrastructure/       # 基础设施层
│   │   ├── config/          # 配置类
│   │   ├── manager/
│   │   │   └── file/
│   │   │       └── storage/ # 对象存储抽象（COS / MinIO 实现与路由）
│   │   ├── mapper/          # MyBatis映射器
│   │   └── websocket/       # WebSocket配置
│   ├── interfaces/           # 接口层
│   │   ├── controller/      # REST控制器
│   │   ├── vo/              # 视图对象
│   │   └── interceptor/     # 拦截器
│   └── shared/              # 共享层
│       ├── common/          # 通用常量
│       ├── event/           # 事件处理
│       └── exception/       # 异常处理
├── src/main/resources/
│   ├── mapper/              # MyBatis XML映射文件
│   └── application*.yml     # 配置文件
└── sql/                     # 数据库脚本
```

## 核心模块详解

### 图片上传流程
1. 客户端发起上传请求
2. 系统校验用户权限和空间配额
3. 生成唯一文件名和对象键
4. 上传到当前对象存储（MinIO 或 COS）
5. 保存图片元数据到数据库
6. 更新缓存和索引

### 权限控制
- 采用 RBAC（基于角色的访问控制）模型
- 支持用户、角色、权限三级控制
- 空间级别的细粒度权限管理
- 使用 Sa-Token 实现空间权限认证；Session 中只保存用户 id
- 无空间上下文时只授予公共图库最小权限（查看、上传），不再默认管理员权限

### 缓存策略
- L1 缓存：Caffeine 本地缓存（应用级）
- L2 缓存：Redis 分布式缓存（服务级）
- 缓存雪崩防护：5–10 分钟随机过期时间
- 写操作后主动失效图片列表缓存（本地 + Redis）

## 部署说明

### 生产环境部署
1. 通过环境变量配置数据库、Redis、对象存储、CORS 来源（参考上文「配置文件」）
2. 设置 JVM 参数：
```bash
-Xms2g -Xmx4g -XX:+UseG1GC
```

3. 使用 Docker 部署：
```dockerfile
FROM eclipse-temurin:17-jre
COPY target/normal-picture-backend-ddd-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar","--spring.profiles.active=prod"]
```

### 监控与运维（规划中）
以下能力尚未接入，属于下一阶段工作：
- Spring Boot Actuator 健康检查
- Micrometer + Prometheus 指标监控
- 结构化日志与 traceId
- 分布式链路追踪

## 开发规范

### 代码规范
- 遵循阿里巴巴Java开发规范
- 使用Lombok简化代码
- 统一异常处理
- 接口返回统一包装

### 命名规范
- 包名：全小写，点分隔
- 类名：UpperCamelCase
- 方法名：lowerCamelCase
- 常量：UPPER_SNAKE_CASE

### Git提交规范
```
feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式调整
refactor: 代码重构
test: 测试相关
chore: 构建过程或辅助工具的变动
```

## 性能优化

### 数据库优化
- 使用MyBatis Plus分页插件
- 关键字段建立索引
- 读写分离支持
- 连接池参数优化

### 缓存优化
- 多级缓存架构
- 缓存预热机制
- 缓存更新策略
- 大Key监控

### 异步处理
- 使用@Async注解实现异步方法
- Disruptor高性能队列处理事件
- 线程池隔离

## 安全考虑

### 认证授权
- Session + Sa-Token 双登录态，Session / Sa-Token 中只保存用户 id
- 密码使用 BCrypt 存储，兼容旧版 MD5 并在登录后自动升级
- 空间权限注解 + 服务层兜底校验；空上下文 fail-closed
- 排序字段白名单，避免 ORDER BY 注入
- 登出时同时清理 Session 与 Sa-Token

### 数据与文件安全
- 响应对象脱敏，密码哈希永不出库
- CORS 来源白名单（`CORS_ALLOWED_ORIGINS`），不再允许任意来源携带 Cookie
- 上传文件校验扩展名、文件头魔数、可解码性与尺寸上限
- URL 上传做 SSRF 防护：禁止内网/回环/云元数据地址，限制重定向与文件大小
- 对象存储密钥、数据库密码全部通过环境变量注入
- 图片删除/替换/空间删除后异步清理对象存储文件

> 审计日志、接口限流、CSRF Token 属于下一阶段规划。

## 扩展功能

### 计划功能
- 图片 AI 识别标签 / 自然语言搜索
- CDN 加速与私有桶签名 URL
- 图片压缩、多规格缩略图
- 批量导入导出

### 集成方案
- 支持MinIO对象存储
- 支持阿里云OSS
- 支持华为云OBS
- 支持本地文件系统

## 问题反馈

如有问题或建议，欢迎提交Issue或Pull Request。

## 许可证

本项目采用MIT许可证，详见 [LICENSE](LICENSE) 文件。