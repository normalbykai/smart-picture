<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { userLoginUsingPost } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import {useRoute, useRouter} from 'vue-router'

// 登录表单信息
const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})
const rememberMe = ref(false)

const router = useRouter();
const route = useRoute();

const loginUserStore = useLoginUserStore()

// 进入页面时只回填账号；密码不做本地存储（客户端可逆加密无法真正保护密码）
onMounted(() => {
  const savedAccount = localStorage.getItem('login_remember_account')
  if (savedAccount) {
    formState.userAccount = savedAccount
    rememberMe.value = true
  }
})

// 请求登录 通过校验才会触发
const handleCommit = async () => {
  const res = await userLoginUsingPost(formState)
  if (res.data.code == 0 && res.data.data) {
    // 记住账号（不保存密码）
    if (rememberMe.value) {
      localStorage.setItem('login_remember_account', formState.userAccount ?? '')
    } else {
      localStorage.removeItem('login_remember_account')
    }
    // 清理历史版本可能保存过的密码
    localStorage.removeItem('login_remember')
    // 获取当前登录用户的信息, 会存放在pinia 内存中
    await loginUserStore.fetchLoginUser()
    message.success('登录成功')
    //跳转到当前路由  如果有重定向就跳转到重定向位置
    const redirectPath = typeof route.query.redirect === 'string' ? route.query.redirect : "/";
    await router.push({
      path: redirectPath,
      replace: true,
    })
  } else {
    message.error(res.data.message)
  }
}
</script>

<template>
  <div id="UserLoginPage">
    <h2 class="title">常态云图库 - 用户登录</h2>
    <div class="desc">企业级智能协同图库</div>
    <a-form :model="formState" autocomplete="off" @finish="handleCommit">
      <a-form-item name="userAccount" :rules="{ required: true, message: '账号不能为空' }">
        <a-input v-model:value="formState.userAccount" placeholder="请输入账号"></a-input>
      </a-form-item>
      <a-form-item
        name="userPassword"
        :rules="[
          {
            required: true,
            message: '密码不能为空',
          },
          {
            min: 5,
            message: '密码长度不能小于5位',
          },
        ]"
      >
        <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码"></a-input-password>
      </a-form-item>
      <a-form-item class="form-options-row" style="margin-bottom: 0">
        <div class="form-options-flex">
          <a-checkbox v-model:checked="rememberMe">记住账号密码</a-checkbox>
          <div class="tips">
            没有账号？
            <RouterLink to="/user/register">去注册</RouterLink>
          </div>
        </div>
      </a-form-item>

      <a-form-item>
        <div style="margin-top: 10px;"></div>
        <a-button type="primary" style="width: 100%" html-type="submit">登录</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<style scoped>
#UserLoginPage {
  max-width: 400px;
  margin: 0 auto;
}

.desc {
  text-align: center;
  color: #bbb;
  margin-bottom: 16px;
}

.title {
  text-align: center;

  margin-bottom: 1rem;
}

.form-options-row {
  margin-bottom: 0 !important;
  margin-top: -8px;
  padding: 0;
}
.form-options-flex {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  width: 100%;
}
.form-options-flex .ant-checkbox-wrapper {
  font-size: 14px;
  color: #666;
  flex: 1 1 auto;
}
.tips {
  color: #bbb;
  font-size: 13px;
  margin-bottom: 0;
  text-align: right;
  white-space: nowrap;
  flex-shrink: 0;
}
.tips a {
  color: #1890ff;
  margin-left: 4px;
}
</style>
