<template>
  <div id="GlobalHeader">
    <a-row :wrap="false" style="width: 100%">
      <a-col flex="200px">
        <RouterLink to="/">
          <div class="title-bar">
            <img class="logo" src="@/assets/logo.jpg" alt="logo" />
            <div class="title">常态云图库</div>
          </div>
        </RouterLink>
      </a-col>

      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="current"
          mode="horizontal"
          :items="menu"
          @click="doMenuClick"
        />
      </a-col>

      <a-col>
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <a-space>
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
                {{ loginUserStore.loginUser.userName ?? '无名' }}
                <span style="color: red">
                  {{loginUserStore.loginUser.userRole === 'admin' ? "(管理员)" : ""}}
                </span>

              </a-space>

              <template #overlay>
                <a-menu class="user-logout">
                  <a-menu-item @click="router.push('/my_space')">我的空间 </a-menu-item>
                  <a-menu-item @click="doLogout"> 退出登录 </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
          <div v-else>
            <a-button type="primary" :href="`/user/login?redirect=${route.path}`">登录</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<style scoped>
#GlobalHeader {
  min-width: 400px;

  .title-bar {
    display: flex;
    align-items: center;
  }

  .logo {
    height: 48px;
  }

  .title {
    color: black;
    font-size: 18px;
    margin-left: 16px;
  }

  .user-logout {
    padding: 20px;
  }
}
</style>

<script lang="ts" setup>
import { h, ref, reactive, computed } from 'vue'
import { HomeOutlined } from '@ant-design/icons-vue'
import {useRoute, useRouter} from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { userLogout } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import { routes } from '@/access/routes.ts'
import {checkAuth} from "@/utils/checkAuth.ts";

const loginUserStore = useLoginUserStore()
const router = useRouter()
const route = useRoute()

const current = ref<string[]>(['/'])


// 过滤菜单
const filterItems = (items: any) => {
  return items.filter((item: any, index: number) => {
    const loginUser = loginUserStore.loginUser
    if (item.key.startsWith('/admin') && loginUser.userRole !== 'admin') {
      return false
    }
    return true
  })
}

const menuToRouteItem = (item: any) => {
  return {
    key: item.key,
    label: item.label,
    title: item.title,
    icon: item.icon ?? undefined,
    disabled: item.disabled,
  }
}

// 菜单动态渲染
const menu = computed(() => {
  const filterRoutes = routes.filter((item: any) => {
    // 导航拦是否开启显示菜单
    if (item.meta?.hideInMenu) {
      return false;
    }
    if (item.meta?.disabled) {
      item.disabled = true;
    }

    // 没权限不显示该菜单
    return checkAuth(loginUserStore.loginUser, item?.meta?.access as string);
  })
  return filterRoutes.map(menuToRouteItem);
})



// 退出登录
const doLogout = async () => {
  const logoutState = await userLogout()
  if (logoutState.data.code === 0) {
    loginUserStore.resetLoginUser();
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败,' + logoutState.data.message)
  }
}


// 路由跳转
const doMenuClick = ({ key }: { key: string }) => {
  router.push({
    path: key,
  })
}

// 监控路由地址使菜单栏改变
router.afterEach((to, from, next) => {
  current.value = [to.path]
})
</script>
