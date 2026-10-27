import type { RouteRecordRaw } from 'vue-router'
import ACCESS_ENUM from '@/enums/AccessEnum'

/**
 * 图片模块配置
 *
 * 说明：本文件负责管理所有与图片相关的路由配置，建议仅维护本模块相关页面。
 * 维护人：normal <normalcode@qq.com>
 * 创建时间：2025-7-1
 *
 * 维护建议：
 * 1. 如需新增图片相关页面，请在本文件添加路由。
 * 2. 路由 path、name、meta 字段请统一英文命名。
 */
const pictureRoutes: RouteRecordRaw[] = [
    {
        path: '/add_picture',
        name: 'addPicture',
        component: () => import('@/pages/picture/AddPicturePage.vue'),
        meta: { title: '创建图片', access: ACCESS_ENUM.USER }
    },
    {
        path: '/add_picture/batch',
        name: 'addPictureBatch',
        component: () => import('@/pages/picture/AddPictureBatchPage.vue'),
        meta: { title: '图片批量添加', access: ACCESS_ENUM.USER },
        props: true
    },
    {
        path: '/picture/:id',
        name: 'pictureDetail',
        component: () => import('@/pages/picture/PictureDetailPage.vue'),
        meta: { title: '图片详情', access: ACCESS_ENUM.NO_LOGIN },
        props: true
    },
    {
        path: '/picture_search',
        name: 'pictureSearch',
        component: () => import('@/pages/picture/PictureSearchPage.vue'),
        meta: { title: '图片识图搜索', access: ACCESS_ENUM.USER }
    }
]

export default pictureRoutes

