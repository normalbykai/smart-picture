import { createRouter, createWebHistory } from 'vue-router'
import userRoutes from './modules/user'
import adminRoutes from './modules/admin'
import pictureRoutes from './modules/picture'
import spaceRoutes from './modules/space'
import commonRoutes from './modules/common'

const routes = [
  ...commonRoutes,
  ...userRoutes,
  ...adminRoutes,
  ...pictureRoutes,
  ...spaceRoutes
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router
