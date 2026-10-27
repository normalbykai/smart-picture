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
