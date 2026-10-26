import type { RouteRecordRaw } from 'vue-router'
import ACCESS_ENUM from '@/enums/AccessEnum'

/**
 * 公共模块路由配置
 *
 * 维护人：normal <normalcode@qq.com>
 * 创建时间：2025-7-1
 *
 * 维护建议：
 * 1. 如需新增公共相关页面，请在本文件添加路由。
 * 2. 路由 path、name、meta 字段请统一英文命名。
 * 3. meta 字段建议包含 title、access、icon、keepAlive 等。
 */
const commonRoutes: RouteRecordRaw[] = [
    {
        path: '/',
        name: 'home',
        component: () => import('@/page/HomePage.vue'),
        meta: { title: '首页', access: ACCESS_ENUM.NO_LOGIN }
    },
    {
        path: '/about',
        name: 'about',
        component: () => import('@/views/AboutView.vue'),
        meta: { title: '关于', access: ACCESS_ENUM.NO_LOGIN }
    },
    {
        path: '/noAuth',
        name: 'noAuth',
        component: () => import('@/page/NoAuthPage.vue'),
        meta: { title: '未授权', access: ACCESS_ENUM.NO_LOGIN }
    }
    // {
    //     path: '/test',
    //     name: 'test',
    //     component: () => import('@/page/test/TestPage.vue'),
    //     meta: { title: '测试界面', access: ACCESS_ENUM.NO_LOGIN }
    // }
]

export default commonRoutes
