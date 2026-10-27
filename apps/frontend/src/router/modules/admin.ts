import type { RouteRecordRaw } from 'vue-router'
import ACCESS_ENUM from '@/enums/AccessEnum'

/**
 * 管理员模块路由配置
 * 
 * 维护人：normal <normalcode@qq.com>
 * 创建时间：2025-7-1
 * 
 * 维护建议：
 * 1. 如需新增管理员相关页面，请在本文件添加路由。
 * 2. 路由 path、name、meta 字段请统一英文命名。
 * 3. meta 字段建议包含 title、access、icon、keepAlive 等。
 */
const adminRoutes: RouteRecordRaw[] = [
    {
        path: '/admin/user',
        name: 'adminUser',
        component: () => import('@/pages/admin/UserManagePage.vue'),
        meta: { title: '用户管理', access: ACCESS_ENUM.ADMIN }
    },
    {
        path: '/admin/picture_manage',
        name: 'adminPictureManage',
        component: () => import('@/pages/admin/PictureManagePage.vue'),
        meta: { title: '图片管理', access: ACCESS_ENUM.ADMIN }
    },
    {
        path: '/admin/spaceManage',
        name: 'adminSpaceManage',
        component: () => import('@/pages/admin/SpaceManagePage.vue'),
        meta: { title: '空间管理', access: ACCESS_ENUM.ADMIN }
    },
    {
        path: '/spaceUserManage/:id',
        name: 'spaceUserManage',
        component: () => import('@/pages/space/SpaceUserManagePage.vue'),
        meta: { title: '空间成员管理', access: ACCESS_ENUM.USER },
        props: true
    }
]

export default adminRoutes
