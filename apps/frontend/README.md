# smart-picture 前端

Vue 3 + TypeScript + Vite + Ant Design Vue，使用 Pinia 和 Vue Router。

## 目录职责

```text
src/
├── api/                    # OpenAPI 生成的接口及类型
├── assets/                 # 样式、图片等静态资源
├── access/                 # 权限守卫与路由权限规则
├── components/
│   ├── common/             # 通用展示、分享、裁剪组件
│   ├── picture/            # 图片上传、列表、搜索、编辑
│   ├── space/analyze/      # 空间分析图表
│   └── user/               # 用户信息编辑
├── constants/              # 业务常量与映射
├── enums/                  # 枚举
├── layouts/
│   ├── components/         # 导航栏、侧边栏
│   └── BasicLayout.vue
├── pages/                  # 路由页面，统一以 Page.vue 结尾
│   ├── admin/
│   ├── picture/
│   ├── space/              # 空间及成员管理
│   ├── user/
│   └── test/               # 开发示例（未启用路由）
├── router/modules/         # 按业务划分的路由
├── stores/                 # Pinia 状态
├── types/                  # 手工维护的类型声明
├── utils/                  # 请求封装、WebSocket、工具函数
├── App.vue
└── main.ts
```

## 常用命令

在本目录执行：

```powershell
pnpm install --frozen-lockfile
pnpm dev
pnpm build          # 类型检查 + 生产构建
pnpm type-check
pnpm exec eslint .  # 只检查
pnpm lint           # 检查并自动修复
pnpm format
pnpm openapi        # 后端启动后更新 API 客户端
```

环境配置见 `.env.development`、`.env.production`，个人覆盖配置使用 `.env.local` 或 `.env.<mode>.local`。
OpenAPI 配置位于 `openapi.config.js`，生成代码输出至 `src/api/`。

## 图片 AI 分析

图片上传成功或进入图片编辑页后，点击“AI 智能分析”调用 `POST /picture/analyze`。
弹窗预览建议名称和标签；“取消”保留表单，“确认填入”替换名称和标签，但不会保存。
确认后可继续修改，点击“保存”才通过现有图片更新接口提交。简介和分类不受影响。
分析期间防止重复请求；重新上传、裁剪或离开页面后，旧分析结果失效。
模型未配置或调用失败时展示错误提示并保留表单。模型密钥只在 Python 服务配置，前端不接触密钥。

## 代码约定

- Vue 文件使用 PascalCase；路由页面统一使用 `XxxPage.vue`。
- 页面放在 `pages/`，可复用组件按业务放入 `components/`，布局组件放入 `layouts/`。
- 跨目录导入优先使用 `@/`（指向 `src/`），同目录模块可以使用相对路径。
- 路由 URL 与页面文件名独立维护；移动文件时保留已有 URL 和权限规则。
- 自动生成接口放在 `api/`，请求拦截和公共错误处理放在 `utils/request.ts`。

完整启动流程见 [根目录说明](../../README.md)，仓库规范见 [目录与开发规范](../../docs/architecture.md)。
