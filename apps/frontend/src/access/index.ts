import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import ACCESS_ENUM from '@/enums/AccessEnum.ts'
import router from '@/router/index.ts'
import { checkAuth } from '@/utils/checkAuth.ts'

/**
 * 全局统一进行登录校验
 */
router.beforeEach(async (to, from, next) => {
  const loginUserStore = useLoginUserStore()
  let loginUser = loginUserStore.loginUser

  // 1. 获取当前路径需要的权限
  const needAccess = (to.meta?.access as string) ?? ACCESS_ENUM.NO_LOGIN

  // 2. 只有需要登录的页面才自动拉去用户信息
  if (needAccess !== ACCESS_ENUM.NO_LOGIN) {
    // 未登录状态, 尝试通过cookie去拿到用户信息
    if (!loginUser || !loginUser.id) {
      // 发起请求获取当前登录用户信息 (session还存在会自动获取用户信息)
      await loginUserStore.fetchLoginUser();
      loginUser = loginUserStore.loginUser // 刷新用户登录状态
    }

    // 3. 未登录跳转登录页面
    if (!loginUser || !loginUser.id) {
      next({
        path: '/user/login', // 登录页面路由
        query: { redirect: window.location.pathname } // 记录当前访问的路径， 用于后续重定向跳转到该路径
      })
      return;
    }

    // 4. 校验当前路由访问权限
    if (!checkAuth(loginUser, needAccess)) {
      // 权限不足, 跳转到未授权页面
      next({
        path: 'noAuth'
      })
      return;
    }

  }

  // 满足访问权限放行路由
  next()
})
