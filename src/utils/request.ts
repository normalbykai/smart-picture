import axios from "axios";
import {message} from "ant-design-vue"
import router from "@/router/index"

const request = axios.create({
  baseURL: import.meta.env.VITE_BASE_API,
  timeout: import.meta.env.VITE_BASE_API_TIMEOUT,
  withCredentials: true // 跨域请求携带cookie
})



// 全局请求拦截器
request.interceptors.request.use(
  function (config) {
    // Do something before request is sent
    return config
  },
  function (error) {
    // Do something with request error
    return Promise.reject(error)
  },
)

// 全局响应拦截器
request.interceptors.response.use(
  function (response) {
    const { data } = response
    // 未登录
    if (data.code === 40100) {
      if (
        !response.request.responseURL.includes('user/get/login') &&
        !window.location.pathname.includes('/user/login')
      ) {
        message.warning('请先登录')
        const currentRedirect = window.location.pathname
        // 跳转路由
        router.push({
          path: '/user/login',
          query:{
            redirect : currentRedirect
          },
          replace: true
        })
      }
    }


    return response
  },
  async function (error) {
    // Any status codes that falls outside the range of 2xx cause this function to trigger
    // Do something with response error
    message.error("服务器异常")
    return Promise.reject(error)
  },
)

export default request;
