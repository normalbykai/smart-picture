import type { RouteRecordRaw } from 'vue-router'
import ACCESS_ENUM from '@/enums/AccessEnum'

/**
 * 空间模块路由配置
 * 
 * 维护人：normal <normalcode@qq.com>
 * 创建时间：2025-7-1
 * 
 * 维护建议：
 * 1. 如需新增空间相关页面，请在本文件添加路由。
 * 2. 路由 path、name、meta 字段请统一英文命名。
 * 3. meta 字段建议包含 title、access、icon、keepAlive 等。
 * 4. 如有统一布局，可在 component 字段指定。
 */
const spaceRoutes: RouteRecordRaw[] = [
  {
    path: '/add_space',
    name: 'addSpace',
    component: () => import('@/pages/space/AddSpacePage.vue'),
    meta: { title: '创建空间', access: ACCESS_ENUM.USER }
  },
  {
    path: '/my_space',
    name: 'mySpace',
    component: () => import('@/pages/space/MySpacePage.vue'),
    meta: { title: '我的空间', access: ACCESS_ENUM.USER }
  },
  {
    path: '/space/:id',
    name: 'spaceDetail',
    component: () => import('@/pages/space/SpaceDetailPage.vue'),
    meta: { title: '空间详情页', access: ACCESS_ENUM.USER },
    props: true
  },
  {
    path: '/space_analyze',
    name: 'spaceAnalyze',
    component: () => import('@/pages/space/SpaceAnalyzePage.vue'),
    meta: { title: '分析空间', access: ACCESS_ENUM.USER }
  }
]

export default spaceRoutes
