import { defineStore } from "pinia";
import { ref } from "vue";
import { getUserLogin } from "@/api/userController.ts"
import { message } from "ant-design-vue";

/**
 * 登录用户信息存储
 *
 * 维护人：normal <normalcode@qq.com>
 * 创建时间：2025-7-1
 */
export const useLoginUserStore = defineStore("loginUser", () => {
  // 当前登录用户信息
  const loginUser = ref<API.UserVO>({
    id: undefined,
    userName: "未登录",
  });

  // 用户信息加载状态
  const loading = ref(false);

  /**
   * 获取当前登录用户信息
   * 通常用于页面初始化或登录后刷新用户状态
   */
  async function fetchLoginUser() {
    loading.value = true;
    try {
      const res = await getUserLogin();
      if (res.data.code === 0 && res.data.data) {
        // 确保 userName 始终为 string
        loginUser.value = {
          ...res.data.data,
          userName: res.data.data.userName || "未登录"
        };
      } else {
        // 这里可以根据实际业务做错误提示
        loginUser.value = { id: undefined, userName: "未登录" };
        message.error(res.data.message);
      }
    } catch (e) {
      // 网络异常等处理
      loginUser.value = { id: undefined, userName: "未登录" };
      message.error("网络异常");
    } finally {
      loading.value = false;
    }
  }

  /**
   * 手动设置当前登录用户
   * @param newLoginUser 新的用户信息对象
   */
  function setLoginUser(newLoginUser: API.UserVO) {
    loginUser.value = {
      ...newLoginUser,
      userName: newLoginUser.userName || "未登录"
    };
  }

  /**
   * 重置用户状态
   */
  function resetLoginUser() {
    loginUser.value = { id: undefined, userName: "未登录" };
  }

  // 暴露属性和方法
  return { loginUser, setLoginUser, fetchLoginUser, loading, resetLoginUser };
}, {
  persist: {
    key: "loginUser",
    storage: localStorage
  }
});

