import './assets/main.css'

//main.ts（必须导入样式） 颜色选择器样式
import '@aurouscia/au-color-picker/style.css'

// 全局权限校验
import '@/access/index.ts'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate' //引入pinal持久化插件

// ant-design
import Antd from "ant-design-vue";
import "ant-design-vue/dist/reset.css";
// ICON组件注入
import * as Icons from '@ant-design/icons-vue'


import App from './App.vue'
import Lazyload from 'vue3-lazyload';
import router from './router'

import VueCropper from 'vue-cropper';
import 'vue-cropper/dist/index.css'



const app = createApp(App)
const pinia = createPinia();
pinia.use(piniaPluginPersistedstate)



const icons: any = Icons;
for (const i in icons) {
  app.component(i, icons[i])
}
app.use(Antd)
app.use(pinia)
app.use(router)
app.use(VueCropper)

// 懒加载配置
app.use(Lazyload, {
  loading: '@/assets/images/default.png',
  error: '@/assets/images/err.png',
});
app.mount('#app')
