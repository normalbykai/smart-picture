import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  build: {
    // 将体积较大的依赖拆分为独立 chunk，避免单个入口包过大
    rollupOptions: {
      output: {
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'pinia', 'axios', 'dayjs'],
          'antd-vendor': ['ant-design-vue', '@ant-design/icons-vue'],
          'echarts-vendor': ['echarts', 'vue-echarts', 'echarts-wordcloud'],
        },
      },
    },
    // 部分图表 chunk 仍会超过 500KB，适当放宽告警阈值
    chunkSizeWarningLimit: 800,
  },
})
