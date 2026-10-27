import type { RouteRecordRaw } from 'vue-router'
import ACCESS_ENUM from '@/enums/AccessEnum'

/**
 * 用户模块路由配置
 *
 * 说明：本文件负责管理所有与用户相关的路由配置，建议仅维护本模块相关页面。
 * 维护人：normal
 * 创建时间：2025-7-1
 *
 * 维护建议：
 * 1. 如需新增用户相关页面，请在本文件添加路由。
 * 2. 路由 path、name、meta 字段请统一英文命名。
 * 3. meta 字段建议包含 title、access、icon、keepAlive 等。
 * 4. 如有统一布局，可在 component 字段指定。
 */
const userRoutes: RouteRecordRaw[] = [
  {
    path: '/user',
    name: 'user',
    // component: () => import('@/layouts/UserLayout.vue'), // 可选：用户相关统一布局
    meta: { title: '用户', access: ACCESS_ENUM.USER },
    children: [
      {
        path: 'login',
        name: 'userLogin',
        component: () => import('@/pages/user/UserLoginPage.vue'),
        meta: { title: '登录', access: ACCESS_ENUM.NO_LOGIN },
      },
      {
        path: 'register',
        name: 'userRegister',
        component: () => import('@/pages/user/UserRegisterPage.vue'),
        meta: { title: '注册', access: ACCESS_ENUM.NO_LOGIN },
      },
      {
        path: 'center',
        name: 'userCenter',
        component: () => import('@/pages/user/UserInfoPage.vue'),
        meta: { title: '个人中心', access: ACCESS_ENUM.USER },
      },
    ],
  },
]

export default userRoutes
