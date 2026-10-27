# smart-image 项目分析与后续开发方向

> 分析对象：`apps/backend`（Spring Boot 后端）+ `apps/frontend`（Vue 3 前端）
> 分析时间：2026-10-21
> 依据：静态代码阅读 + 后端 clean compile / 前端 type-check 实测，未做运行期联调

> **更新（2026-10-21）**：本文 P0 问题（S1–S10）与架构收口已完成，前端类型检查已清零。
> 具体改动、行为变化与验证结果见 [优化说明](maintenance.md)。本文保留为优化前的基线分析。

---

## 一、结论摘要

**一句话判断：这是一个"功能面做到 80 分、工程质量停在 40 分"的团队云图库项目。**

它已经具备清晰的产品雏形：公共图库、私人空间、团队空间、成员角色、对象存储双实现（COS / MinIO）、实时协同编辑、空间分析、以图搜图。后端还尝试了 DDD 分层，前端页面覆盖也相当完整。作为课程项目、毕业设计或求职作品集，它的完成度明显高于平均水平。

但距离"可以给真实用户用"还有明显差距，且差距不在功能数量，而在三件事：

1. **权限边界不可靠**——多处权限判断写反、校验被注释、"无上下文即管理员"，越权风险是系统性的。
2. **数据一致性没有闭环**——删除图片/空间不清理对象存储，空间额度用非原子累加，编辑图片会把图片"移出"空间。
3. **工程约束缺失**——没有 Git、没有测试、没有 CI、没有部署编排；前端类型检查 20 个错误；云端密钥明文躺在配置里。

所以后续最该做的不是继续加功能，而是先把它变成"敢上线、能迭代"的系统，再把 AI 检索做成真正的差异化。

### 成熟度评分（主观评估，用于定位而非精确度量）

| 维度 | 评分 | 主要依据 |
| --- | --- | --- |
| 功能完整度 | 7.5 / 10 | 浏览、上传、裁剪、批量编辑、空间、成员、分析、协同都有闭环 |
| 架构设计 | 5.0 / 10 | 有 DDD 分层，但依赖倒置、领域服务继承 ORM、逻辑重复两份 |
| 安全与权限 | 2.5 / 10 | 越权读取、权限判断取反、密钥明文、SSRF、CORS 全开 |
| 代码质量 | 5.5 / 10 | 命名统一、注释充分；但死代码、拼写错误、异常吞噬并存 |
| 工程化 | 2.5 / 10 | 无 Git / 测试 / CI；构建脚本拼写错误；类型检查失败 |
| 可运维性 | 2.0 / 10 | 无 Actuator、无日志规范、无迁移工具、无部署编排 |
| 产品化潜力 | 6.5 / 10 | "团队图片资产 + AI 检索"方向成立，骨架已具备 |

---

## 二、项目全景

### 2.1 组成与规模

| 部分 | 技术 | 规模 |
| --- | --- | --- |
| 后端 | Spring Boot 3.4.4 / Java 17 / Maven | 163 个 Java 文件，约 410 KB |
| 前端 | Vue 3.5 / TypeScript 5.8 / Vite 6 / Ant Design Vue 4.2 | 71 个源文件，约 242 KB |
| 数据库 | MySQL 8（MyBatis-Plus 3.5.11） | 4 张表：user / picture / space / space_user |
| 缓存 | Redis + Caffeine 本地缓存 + Spring Session | L1/L2 两级 |
| 存储 | 腾讯云 COS / 本地 MinIO（S3 协议） | 抽象接口 + 运行时切换 |
| 鉴权 | Sa-Token 1.44 + 自定义 Session 登录态 | 两套登录态并存 |
| 实时 | Spring WebSocket + Disruptor | 单机内存会话 |
| 文档 | Knife4j 4.5 | `/api/doc.html` |
| 测试 | 无 | `src/test` 不存在；前端无 Vitest/Playwright |

### 2.2 模块地图

| 模块 | 关键类 | 能力 |
| --- | --- | --- |
| 用户 `domain/user` | `UserDomainServiceImpl`、`UserController` | 注册、登录、头像、个人资料、管理员 CRUD |
| 图片 `domain/picture` | `PictureDomainServiceImpl`、`PictureController` | 上传（文件/URL）、审核、编辑、批量改名、颜色检索、以图搜图 |
| 空间 `domain/space` | `SpaceDomainServiceImpl`、`SpaceController` | 私人/团队空间、等级配额、创建/删除/查询 |
| 成员 `space_user` | `SpaceUserDomainServiceImpl`、`SpaceUserController` | 成员增删改查、角色（viewer/editor/admin） |
| 分析 `space/analyze` | `SpaceAnalyzeApplicationServiceImpl` | 用量、分类、标签、大小、上传趋势、空间排行 |
| 存储 `manager/file` | `PictureStorageRouter`、`Cos/MinioPictureObjectStorage` | 上传、删除、缩略图、主色调、URL 解析 |
| 权限 `shared/auth` | `StpInterfaceImpl`、`SpaceUserAuthManager` | 从 `spaceUserAuthConfig.json` 加载角色→权限 |
| 协同 `shared/ws` | `PictureEditHandler`、`WsHandshakeInterceptor` | 进入/退出编辑、编辑动作广播 |

### 2.3 核心链路

```
浏览器 (Vue3 + AntDV)
   │  HTTP(S) + Cookie(Session) ／ WebSocket
   ▼
Controller ──► ApplicationService ──► DomainService ──► Repository / Mapper ──► MySQL
   │                  │                    │
   │                  │ 事务/编排           │ 业务规则（但继承 ServiceImpl）
   ▼                  ▼                    ▼
Sa-Token 注解鉴权   事务模板           MyBatis-Plus
   │
   └── PictureUploadTemplate（模板方法）──► StorageRouter ──► COS / MinIO
```

**上传链路**：Controller 取登录用户 → 校验空间与配额 → 选择 `PictureFileManager`（文件）或 `PictureUrlManager`（URL）→ 模板方法生成对象键、落临时文件 → `StorageRouter.current()` 上传 → 读取宽高/主色调/生成缩略图 → 入库 + 更新空间额度（事务）。

**鉴权链路**：`@SaCheckPermission(type=SPACE)` → `StpInterfaceImpl.getPermissionList()` → 从请求 body/param 反射解析 `spaceId` / `pictureId` → 查空间类型、成员角色 → 返回权限列表。权限配置在 `biz/spaceUserAuthConfig.json`，角色可扩展。

**协同链路**：WebSocket 握手校验图片与空间权限 → 会话属性带 user/pictureId → 消息进入 Disruptor 队列 → `PictureEditHandler` 在内存 Map 中维护"谁在编辑"并广播。**只支持单机、只支持团队空间。**

### 2.4 运行方式

```bash
# 1. 对象存储
docker compose -f deploy/docker-compose.minio.yml up -d

# 2. 后端（MySQL / Redis 需自行准备）
cd apps/backend
mvn spring-boot:run -Dspring.profiles.active=local

# 3. 前端
cd apps/frontend
pnpm install
pnpm dev
```

---

## 三、值得保留的设计

1. **对象存储抽象（全项目最有工程价值的部分）**
   `PictureObjectStorage` + `PictureStorageRouter` + COS/MinIO 两套实现 + `PictureUploadTemplate` 模板方法。上传业务只依赖接口，切换存储不改业务代码，还能按历史 URL 反查存储做删除。这个设计可以原样保留并继续扩展（直传、签名、生命周期）。

2. **权限配置外置**
   `spaceUserAuthConfig.json` 把"角色 → 权限"从代码里抽出来，前端根据 `permissions` 渲染按钮。新增角色只改配置，方向是对的。

3. **功能覆盖完整，且关键体验有闭环**
   瀑布流、图片裁剪、批量编辑、颜色检索、以图搜图、空间分析图表、协同编辑——不是"半成品菜单"，每块都能点进去用。

4. **统一骨架已具备**
   `BaseResponse` / `ResultUtils` / `BusinessException` / `GlobalExceptionHandler` / `PageRequest` / `@AuthCheck`，说明作者有意识地做了统一，只是还没落到每个接口。

5. **对双存储差异有思考**
   COS 走数据万象，MinIO 用 ImageIO 本地算宽高/主色调/缩略图，并在 README 里说明了 WebP 解码的限制，这是真正踩过坑才会写的细节。

---

## 四、问题清单（按优先级）

> 说明：下列问题均来自静态代码阅读，行号对应当前工作区文件。**P0 建议在对外演示或部署前全部修复。**

### P0：安全与数据风险（上线前必须修）

| 编号 | 问题 | 证据 | 影响 | 建议 |
| --- | --- | --- | --- | --- |
| S1 | 腾讯云 COS 密钥明文写在配置里，且未被 `.gitignore` 忽略 | `apps/backend/src/main/resources/application-local.yml:52-53`；后端 `.gitignore` 只忽略了 `application-prod.yml` | 密钥一旦进入版本库/截图/分享，可被直接盗用产生费用与数据泄露 | **立即到腾讯云轮换 AK/SK**；改为 `${COS_SECRET_ID}` 环境变量；把 `application-local.yml` 加入 `.gitignore`；生产用 KMS/Secrets Manager |
| S2 | `GET /user/get` 没有任何鉴权，返回完整 `User`（含密码哈希） | `interfaces/controller/UserController.java:110-114`（注释写着"仅管理员"） | 任意人可枚举 id 拿到账号、密码 MD5、角色、头像等 | 加 `@AuthCheck(mustRole=ADMIN)` 或直接删接口；对外只返回 `UserVO`；密码哈希永不出库 |
| S3 | 空间分析权限判断写反，多处校验被注释 | `application/service/impl/SpaceAnalyzeApplicationServiceImpl.java:76`（用量分析的 `!loginUser.isNotAdmin()` 导致管理员被拒、普通用户放行）；`:92-93`、`:111-113`、`:146-148`、`:189`、`:219` 服务层校验被注释 | 普通用户可以看"全空间/公共图库"的统计（空上下文给管理员权限 + 管理员判断反了）；具体私有空间目前只剩注解兜底，注解一旦失效即越权；管理员反而看不到全空间用量 | 恢复并统一 `checkSpaceAnalyzeAuth`；全空间/公共库仅管理员；服务层必须兜底，不能只靠注解 |
| S4 | "权限上下文为空 = 管理员权限" | `shared/auth/StpInterfaceImpl.java:83-85`、`:128-131`；`:212-221` 的 `case "space"` 缺少 `break`，会继续落到 `user` | 任何不带 `spaceId`/`pictureId` 的受保护接口，登录用户直接获得全部空间权限 | 改为 fail-closed（空上下文返回空权限）；公共图库用显式 PUBLIC 角色；补 `break`；用明确的 DTO 解析上下文而不是反射 body |
| S5 | 排序字段直拼 SQL，存在注入面 | `domain/picture/service/impl/PictureDomainServiceImpl.java:179`；`domain/user/service/impl/UserDomainServiceImpl.java:229`；`domain/space/service/impl/SpaceDomainServiceImpl.java:63` | `sortField` 来自请求 JSON，被直接当作列名拼进 `ORDER BY` | 字段白名单映射（`createTime` / `name` / `picSize` ...），非法字段直接拒绝 |
| S6 | URL 上传存在 SSRF，且下载无大小上限 | `infrastructure/manager/file/PictureUrlManager.java:58-84`（HEAD 失败即放行、无内网/重定向校验）、`:99-102`（`HttpUtil.downloadFile` 全量落盘） | 可探测内网服务、读取云元数据、拉取超大文件打满磁盘 | 解析 URL + DNS 校验私网/回环；限制重定向次数与目标；流式读取并截断；设置连接/读取超时 |
| S7 | 上传只校验后缀；MinIO 桶是公共读 | `infrastructure/manager/file/PictureFileManager.java:37-40`；`MinioPictureObjectStorage.java:237-239`、`:250-273` | 可上传伪装文件；图片 URL 泄露即公开，可被直接访问、盗链、刷流量 | 增加魔数 + `ImageIO.read` 解码校验；生产改私有桶 + 预签名 URL；Nginx 做 Referer 防盗链 |
| S8 | CORS 允许任意源且携带 Cookie | `infrastructure/config/CorsConfig.java:21-23` | 任意站点可带用户 Cookie 发起请求，配合无 CSRF 防护风险更高 | 显式域名白名单；Cookie 设 `SameSite=Lax/Strict`；关键写操作加 CSRF Token 或 Origin 校验 |
| S9 | 密码使用 MD5 + 固定盐；登录态存整个 User | `domain/user/service/impl/UserDomainServiceImpl.java:191-195`；`:103-107` 把 User 放进 Session 与 Sa-Token Session；`:141-151` 登出只清 Session，不清 Sa-Token | MD5 可被彩虹表/撞库；Session 里带密码哈希；登出后 Sa-Token 仍有效 | 改 BCrypt/Argon2；Session 只存 `userId`；登出时 `StpKit.SPACE.logout()` 双清 |
| S10 | 删除/编辑与对象存储、空间额度不一致 | `PictureApplicationServiceImpl.java:426-445`（只删库不删对象）；`:294-299`、`:432-441`（`setSql` 非原子累加）；`:224-235`（编辑时不传 spaceId 会把图片移出空间）；`SpaceController.java:109-121`（删空间只删库，且空空间会因 `remove()` 返回 false 回滚） | 存储成本持续泄漏；额度可被并发击穿；数据与配额长期漂移 | 用领域事件异步删对象；额度用 `UPDATE ... WHERE totalSize + ? <= maxSize` 原子扣减；抽出 `SpaceQuotaService`；删除进回收站 |

### P1：功能正确性（用户能直接感知）

| 编号 | 问题 | 证据 | 影响 |
| --- | --- | --- | --- |
| B1 | 按用户筛选图片时列名写错 | `PictureDomainServiceImpl.java:139` 使用 `"loginUserId"`，实际列是 `userId` | 带 `userId` 的查询直接 SQL 报错 |
| B2 | `nullSpaceId` 默认 true | `PictureDomainServiceImpl.java:173-177` | 只传 `spaceId`、不传 `nullSpaceId` 时，查询变成 `spaceId IS NULL AND spaceId = ?`，结果恒为空 |
| B3 | 公共/全部空间的用量统计反转 | `PictureDomainServiceImpl.java:700-727`（`if (!isPublic) isNull("spaceId")`） | "全部空间"统计到的是公共图库，"公共图库"统计到的是全部图片 |
| B4 | 使用率两个字段互换，且可能除零 | `SpaceDomainServiceImpl.java:114-115` | 前端"大小/数量"进度条显示反了；新建空空间可能 NaN/异常 |
| B5 | 空间名校验 NPE | `Space.java:133`（`isBlank(spaceName) && spaceName.length() > 36`） | 空间名为空时抛空指针，而不是参数错误；长度校验也永远不生效 |
| B6 | 删除空间成员的条件写反 | `SpaceUserController.java:73`（`deleteRequest.valid()` 为 true 时反而抛错） | 合法 id 必然被拒绝，删除成员功能不可用 |
| B7 | 图片详情接口把登录用户传成了 `null` | `PictureController.java:168-171` | 详情页 `permissions` 恒为空，编辑/删除按钮对所有人隐藏 |
| B8 | 编辑空间图片时空间 ID 丢失 | `PictureApplicationServiceImpl.java:224-235`（只有原图为空时才复用） | 从详情页编辑后，图片被移到公共库，空间额度不减 |
| B9 | 建立协同连接时空指针 | `PictureEditHandler.java:69-71`（无人编辑时 `getById(null)`） | 第一个进入协同的用户可能直接报错 |
| B10 | 私人空间无法协同编辑 | `WsHandshakeInterceptor.java:87-90` 强制要求团队空间 | 私人空间/公共图库的协同功能实际不可用 |
| B11 | 前端权限常量用错 | `SpaceDetailPage.vue:163` 删除用 `PICTURE_EDIT`；`:166` 批量编辑用 `SPACE_USER_MANAGE` | 按钮显隐与真实权限不一致 |
| B12 | 缓存清理前缀不一致 | `CacheController.java:55` 与 `PictureCacheConstant.java:12` | 管理端"清空首页缓存"实际清不掉 |
| B13 | 首页瀑布流加载原图 | `page/picture/components/ImageList.vue:29` 用 `item.url` | 首页带宽和首屏时间被放大数倍（列表组件已正确使用 `thumbnailUrl`） |
| B14 | 批量抓取同步阻塞、失败静默 | `PictureDomainServiceImpl.java:462-499` | 一次请求串行抓 20 张，超时/失败只打日志，用户只看到"成功 N 张" |

### P2：架构问题（影响迭代速度）

| 编号 | 问题 | 证据 |
| --- | --- | --- |
| A1 | 分层倒置：domain 依赖 application | `PictureDomainServiceImpl.java:40`、`:82` 注入 `SpaceApplicationService`；`SpaceUserAuthManager.java:13`、`StpInterfaceImpl.java:12` 同理 |
| A2 | 领域服务继承 `ServiceImpl`，领域层被 MyBatis-Plus 绑死 | `PictureDomainServiceImpl.java:71`、`UserDomainServiceImpl.java:39` |
| A3 | 仓储是空壳，实际全部绕过 | `PictureRepository.java` 仅 292 字节；业务里大量直接 `this.getById()` / `baseMapper` |
| A4 | 上传/编辑/批量编辑有两份实现 | `PictureApplicationServiceImpl.java:182-303`、`:373-407` 与 `PictureDomainServiceImpl.java:256-378`、`:597-633` |
| A5 | Controller 直接调分页、更新链、Mapper | `PictureController.java:184-187`、`SpaceController.java:115-117`、`SpaceAnalyzeApplicationServiceImpl.java:124` 等 |
| A6 | 事务边界混乱，自调用失效 | `TransactionTemplate` 与 `@Transactional` 混用；同类内调用 `fillReviewParams` / `editPictureBatch` |
| A7 | 缓存只写不失效，反序列化类型不安全 | `PictureDomainServiceImpl.java:660-691`：key 用全量请求 MD5、无版本、无主动失效；Redis 命中时 `JSONUtil.toBean(cache, Page.class)` 泛型已擦除，记录会退化为 `JSONObject` |
| A8 | 认证双栈并存 | Spring Session + 自定义 `USER_LOGIN_STATE` + Sa-Token；登出只清一套 |
| A9 | 权限上下文靠反射 body 解析 | `StpInterfaceImpl.java:186-223`，与接口参数强耦合、易被参数名变更破坏 |
| A10 | WebSocket 单机内存态 | `PictureEditHandler.java:37-40` 两个 `ConcurrentHashMap`，无法多实例；Disruptor 只在单 JVM |
| A11 | 无幂等、无去重、无审计、无限流 | 重复上传同一文件会重复占用存储与额度；关键操作无审计日志 |

### P3：工程化（决定能否长期开发）

| 编号 | 问题 | 证据/说明 |
| --- | --- | --- |
| E1 | 没有 Git 仓库 | `git status` 报 `not a git repository`；没有历史、无法回滚、无法协作 |
| E2 | 没有任何测试 | 后端无 `src/test`；前端无测试框架与脚本 |
| E3 | 无 CI/CD、无 Dockerfile、无数据库迁移 | 只有 `docker-compose.minio.yml`；SQL 用 `ALTER TABLE` 手工追加，不可重复执行 |
| E4 | 前端类型检查失败 | `pnpm type-check` 实测 **20 个错误**（`LocationQueryValue` 未收窄、`possibly null`、`uploadValid` 类型写成 boolean 等） |
| E5 | 脚本拼写与语义错误 | `package.json`：`noraml-build-production`、`noraml-build-dev`；`normal-production` 实际执行 `vite dev` |
| E6 | 生产环境配置缺失 | README 宣称有 `.env.production`，实际不存在且被 `.gitignore` 忽略；`VITE_SECRET_KEY` 把 AES 密钥放在客户端 |
| E7 | README 与实现漂移 | 声称 JWT、Actuator、Micrometer、布隆过滤器、读写分离、链路追踪，`pom.xml` 与代码中均无 |
| E8 | 死代码/测试接口 | `FileTestController`、`HomePage.fetchData2`、`tagsToOptionsObj`、`console.log` 若干 |
| E9 | 生产日志仍是 MyBatis 控制台输出 | `application.yml:43` 默认 `StdOutImpl`；无 logback 配置、无 traceId |
| E10 | 依赖版本管理不统一 | `pom.xml` 中 MyBatis-Plus BOM 为 3.5.9，starter 却又显式写 3.5.11 |

---

## 五、根因判断

这些问题不是"能力问题"，而是"工程约束缺失"的典型表现：

1. **教程式演进，架构是事后补的。**
   先有 Controller/Service 把功能跑通，再套 DDD 分层，结果就是"包名分层、依赖倒置、领域服务仍是 CRUD"。这不是白走的路——存储抽象、权限配置外置都是真收益——但需要一次"收口"。

2. **权限有两套事实源，没有单一入口。**
   Session 登录态、Sa-Token 登录态、`@AuthCheck`、`@SaCheckPermission`、服务层手写校验同时存在。任何一处判断写反，都不会被另一处发现，因为没有测试覆盖权限矩阵。

3. **没有测试与版本控制，回归靠手点。**
   这直接解释了为什么会出现"删除成员条件写反""统计口径反转""详情页权限传 null"这类一测就现的问题——它们从来没被自动化验证过。

4. **没有生产 checklist。**
   密钥、CORS、限流、对象清理、配额原子性、日志、迁移，这些不属于"功能"，但决定系统能不能上线。项目当前把它们全部排在了功能之后。

---

## 六、后续开发方向

### 6.1 先选产品方向（三选一，建议 A）

| 方向 | 目标用户 | 核心价值 | 功能取舍 | 风险 | 建议 |
| --- | --- | --- | --- | --- | --- |
| **A. AI 原生团队图片资产库** | 设计/运营/自媒体/电商小团队 | 找得到（自然语言/以图搜图/自动标签）、用得顺（直传、批量、分享）、管得住（成员权限、审核、用量） | 保留空间/成员/权限/存储/审核；砍掉公共图库爬虫、空间排行、复杂图表 | 需要 AI 成本控制与冷启动数据 | **推荐**，与现有骨架最匹配 |
| B. 个人云图库（ToC） | 个人用户 | 大容量、多端、相册整理 | 需要做客户端/同步/计费；团队能力浪费 | 红海、存储成本敏感、获客贵 | 不建议作为主线 |
| C. 作品集/毕设精修 | 面试官/答辩老师 | 架构叙事 + 3 个亮点 | 只做深权限、协同、存储抽象，不追求上线 | 天花板低，但投入最小 | 时间紧时选它 |

**推荐 A 的理由**：现有代码已经具备"空间 + 成员 + 角色 + 配额 + 存储抽象 + 审核"这套团队资产骨架，这是 ToC 图床没有的；而"AI 找图"恰好能把当前最脆弱的两块（百度识图爬虫、MySQL LIKE 搜索）替换成稳定、可解释、可收费的能力。

**MVP 范围建议（先做窄，再做深）**

- 保留：账号、团队空间、成员角色、拖拽/直传、AI 自动打标、自然语言搜索、以图搜图、分享页、审核、回收站、用量看板。
- 后置：公共图库批量抓取、空间排行榜、颜色搜索（降级为搜索筛选器）、复杂 ECharts、URL 批量导入。
- 明确不做：多端同步客户端、视频转码（除非客户明确需要）。

### 6.2 技术路线图（按"能上线"倒推）

#### Phase 0｜止血（1–2 周）

目标：把"会出事"的地方全部堵上，建立最低限度的安全网。

- 轮换 COS 密钥，配置改环境变量，`application-local.yml` 加入 `.gitignore`。
- 修复 P0 的 S1–S10；修复 P1 的 B1、B3、B6、B7、B8。
- `git init`，建立 `main/develop` 分支与 PR 模板，补全 `.gitignore`（target、dist、node_modules、本地配置）。
- 测试骨架：JUnit 5 + Testcontainers（MySQL/Redis/MinIO）+ MockMvc，先写 8 条：
  1) 未登录访问受保护接口 → 401；2) 普通用户读他人空间 → 403；3) 空上下文不再获得管理员权限；
  4) 上传超配额 → 拒绝；5) 删除图片后对象被清理；6) 删除空间成员成功；7) 空间分析越权 → 403；8) 编辑图片不改变 spaceId。
- 前端 `pnpm type-check` 清零，修 B11。

**验收**：安全清单全绿；CI 跑通 8 条集成测试；`pnpm type-check` 0 错误。

#### Phase 1｜可运维（2–4 周）

目标：一条命令起全栈，出问题能看见。

- 引入 Flyway，把 `create_table_sql.sql` 重写为可重复执行的版本化迁移（当前 `ALTER TABLE` 直接跑第二次会失败）。
- 配置分环境：`application-dev.yml` / `application-prod.yml`，敏感项全部走环境变量。
- Docker Compose 全栈：mysql、redis、minio、backend、nginx（前端静态资源 + 反向代理 + WebSocket upgrade）。
- 可观测：`spring-boot-starter-actuator` + Micrometer + Prometheus + Grafana；Logback JSON 日志 + traceId；关键指标（上传成功率、P95 时延、存储用量、配额拒绝数）。
- 契约：以 Knife4j/OpenAPI 为准，前端重新生成 API 与类型，去掉 `@ts-ignore`。
- 统一参数校验、分页默认值（`currentNum=1`、`pageSize=20`）、错误码表。

**验收**：`docker compose up` 一键起；`/actuator/health` 正常；有 Dashboard 与告警；PR 自动构建 + 测试。

#### Phase 2｜可规模化（4–8 周）

目标：扛住并发、控制成本、支持多实例。

- **上传直传**：后端签发 presigned PUT，浏览器直传对象存储，回调落库；后端不再中转文件。
- **图片处理服务**：独立 worker（libvips / ImageMagick / 云函数），生成 3 档缩略图 + WebP/AVIF + blurhash + EXIF；替换 `ImageIO` 同步处理。
- **CDN + 私有桶 + 签名 URL + 防盗链**；历史 URL 兼容。
- **删除异步化**：`PictureDeleted` 事件 → 队列 → 删对象 + 删缩略图；回收站 30 天生命周期。
- **缓存治理**：key 版本化、主动失效、空值缓存、真正实现布隆过滤器；修 A7 的反序列化类型问题。
- **分布式锁**：用 Redisson 替代 `String.intern()`（`SpaceApplicationServiceImpl.java:186-189`）。
- **WebSocket 多实例**：Redis Pub/Sub 或 STOMP relay 替换内存 Map；协同从"锁编辑"升级为"多人同时编辑"（Yjs/CRDT）。
- **数据库**：`picture(spaceId, createTime)`、`picture(userId, createTime)` 复合索引；统计改预聚合日表，替代 `selectObjs` 全量进内存。
- **限流与幂等**：Bucket4j/Redis 限流；上传按内容 hash 幂等/去重。

**验收**：100 并发上传不超额；首图 P75 < 1s；两实例水平扩容可用；删除后 24h 内存储回收；慢查询清零。

#### Phase 3｜AI 与产品化（2–3 月）

目标：形成差异化，能收费。

- 向量检索：CLIP/SigLIP 生成图文向量，pgvector（千万级以内够用）或 Milvus；混合检索（BM25 + 向量 + RRF）。
- 自动打标：离线批处理（多模态 LLM 或本地 Florence-2），写入 tag 表；人工可纠正。
- 以图搜图：替换百度识图爬虫，改为向量近邻检索。
- 相似去重：pHash + BK-tree，上传时提示"疑似重复"。
- 内容审核：云厂商图片审核 API，接入现有 `reviewStatus` 状态机。
- 增值：智能裁剪、去背景、超分、水印/版权信息。
- 商业化：分享链接（有效期/密码/下载权限）、嵌入代码、开放 API、套餐与用量计费、审计日志。

**验收**：搜索 Top5 命中率达到可接受线；单张图片 AI 处理成本可控；有付费套餐与账单。

### 6.3 架构改造建议（DDD 真正落地）

1. **依赖方向**：`domain` 不允许 import `application` / `infrastructure`。把 `SpaceApplicationService`、`SpaceUserApplicationService` 的调用改为领域端口（`SpaceQuotaPort`、`SpaceMembershipPort`），由基础设施实现。
2. **领域服务去 ORM**：`PictureDomainService` 不再 `extends ServiceImpl`；仓储接口保留并在 `infrastructure/repository` 真正实现；聚合根封装不变量（配额、审核状态机、空间类型）。
3. **应用服务只做编排**：事务、权限、DTO 转换、事件发布；业务规则下沉到领域。
4. **领域事件解耦**：`PictureUploaded`、`PictureDeleted`、`SpaceQuotaChanged` → 异步处理缩略图、AI 打标、存储清理、缓存失效、审计。
5. **认证收敛为单栈**：二选一（建议 Sa-Token + Redis），删除 Spring Session 双写；登出、改密、封号时统一踢下线。
6. **权限单一入口**：自定义 `@RequireSpacePermission(spaceIdParam = "spaceId")`，显式声明参数来源；空上下文 fail-closed；公共图库用 PUBLIC 角色而不是"管理员"。

### 6.4 前端演进

- **类型与契约**：OpenAPI 生成 + 响应泛型 + 严格模式；消灭 `any`、`@ts-ignore`、`console.log`。
- **数据层**：引入 `@tanstack/vue-query` 管理服务端状态（缓存、重试、失效），Pinia 只放 UI 状态；统一 loading/error/empty。
- **上传体验**：拖拽、粘贴、断点续传、并发队列、失败重试、上传前压缩；直传改造后进度更真实。
- **图片性能**：列表统一用 `thumbnailUrl`（修 B13）；`srcset` + AVIF/WebP；blurhash 占位；虚拟滚动已有，补骨架屏。
- **编辑器**：从单机裁剪升级为"非破坏性编辑 + 版本历史"；协同用 Yjs，替代当前"一人锁编辑"。
- **体验细节**：大图键盘浏览、批量选择、移动端适配、暗色模式、i18n（如需）。
- **可观测**：Sentry + 埋点（上传转化、搜索点击、分享转化）。

### 6.5 数据与成本

- **索引**：`picture(spaceId, createTime)`、`picture(userId, createTime)`、`picture(reviewStatus, createTime)`。
- **标签模型**：`tags` JSON 字段只适合展示；检索建议 `tag` + `picture_tag` 关联表，或引入 Meilisearch/ES。
- **统计**：用量/趋势走预聚合表（按天/空间），不要每次扫全表。
- **存储**：原图 + 3 档缩略图；生命周期规则；pHash 去重；配额与账单打通。
- **冷热分层**：90 天未访问的图片转低频存储，降低成本。

### 6.6 AI 能力落地顺序（按投入产出比）

| 顺序 | 能力 | 价值 | 实现要点 |
| --- | --- | --- | --- |
| 1 | 自动打标 | 解决"上传后没人填标签" | 离线批处理 + 多模态模型；标签写库；人工可纠正 |
| 2 | 自然语言搜索 | 核心差异化 | CLIP/SigLIP 向量 + pgvector；混合检索；中文查询改写 |
| 3 | 以图搜图 | 替换脆弱爬虫 | 复用同一套向量索引；相似度阈值可调 |
| 4 | 相似去重 | 直接降存储成本 | pHash + BK-tree；上传时提示 |
| 5 | 内容审核 | 合规刚需 | 云 API + 人工复核队列 |
| 6 | 智能编辑 | 增值付费点 | 裁剪/去背景/超分，按次计费 |

### 6.7 指标建议

**产品指标**：上传成功率 ≥ 99%、首图 P75 < 1s、搜索点击率、7 日留存、空间付费转化率、分享链接打开率。

**工程指标**：核心域测试覆盖率 ≥ 60%、部署频率、MTTR、P95 接口时延、配额拒绝率、存储成本/千张。

---

## 七、本周就能做的 5 件事

1. **`git init` + 轮换 COS 密钥 + 补 `.gitignore`**（半天，风险最高、收益最大）。
2. **修 S2/S3/S4/S5 + B1/B3/B6/B7/B8**（1 天，都是小改动但影响面大）。
3. **写 8 条集成测试**（2 天，把权限矩阵和配额锁死）。
4. **前端 `type-check` 清零 + 修 B11/B13**（1 天）。
5. **写 1 页 PRD，选定方向 A，冻结 MVP 范围**（半天，避免继续"想到哪做到哪"）。

---

## 附录 A：验证记录

| 项目 | 结果 |
| --- | --- |
| 后端 `mvn -B -DskipTests clean compile` | **BUILD SUCCESS**，163 个源文件，6.1s；仅 `SpaceAnalyzeApplicationServiceImpl` unchecked 警告 |
| 后端打包 | `mvn -B -DskipTests package` 成功，生成 `target/normal-picture-backend-ddd-0.0.1-SNAPSHOT.jar` |
| 前端 `pnpm type-check` | **失败，20 个错误**：`HomePage.vue`(2)、`AddPicturePage.vue`(6)、`ImageList.vue`(1)、`PictureDetailPage.vue`(1)、`PictureSearchPage.vue`(3)、`PictureSearchForm.vue`(1)、`AddSpacePage.vue`(3)、`SpaceDetailPage.vue`(2)、`utils/index.ts`(1) |
| 后端测试 | 无 `src/test`，未执行 |
| 版本控制 | `git status` → `fatal: not a git repository` |
| 代码规模 | 后端 163 个 Java 文件 / 410,041 字节；前端 71 个文件 / 242,195 字节 |
| 未执行 | 前端 `build`、运行期联调、压测、安全扫描 |

## 附录 B：关键文件索引

| 主题 | 文件 |
| --- | --- |
| 上传/编辑/删除 | `apps/backend/src/main/java/com/normal/picturebackend/application/service/impl/PictureApplicationServiceImpl.java` |
| 领域规则/缓存 | `apps/backend/src/main/java/com/normal/picturebackend/domain/picture/service/impl/PictureDomainServiceImpl.java` |
| 空间额度/分析 | `apps/backend/src/main/java/com/normal/picturebackend/domain/space/service/impl/SpaceDomainServiceImpl.java` |
| 动态权限 | `apps/backend/src/main/java/com/normal/picturebackend/shared/auth/StpInterfaceImpl.java` |
| 存储抽象 | `apps/backend/src/main/java/com/normal/picturebackend/infrastructure/manager/file/storage/` |
| 协同编辑 | `apps/backend/src/main/java/com/normal/picturebackend/shared/ws/` |
| 配置/密钥 | `apps/backend/src/main/resources/application-local.yml` |
| 建表 SQL | `database/schema.sql` |
| 前端请求层 | `apps/frontend/src/utils/request.ts` |
| 前端空间页 | `apps/frontend/src/pages/space/SpaceDetailPage.vue` |
| 前端首页 | `apps/frontend/src/pages/HomePage.vue` |
