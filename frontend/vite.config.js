import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: true,  // 允许局域网访问
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:4993',
        changeOrigin: true
      },
      // 本地上传文件
      '/uploads': {
        target: 'http://localhost:4993',
        changeOrigin: true
      },
      // MinIO 对象存储
      '/images': {
        target: 'http://localhost:9000',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/images/, '')
      }
    }
  }
})
