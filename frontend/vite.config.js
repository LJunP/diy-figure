import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// Vite 配置
// 前端开发服务器运行在 5173 端口,后端运行在 8080 端口
// 通过 proxy 代理解决跨域问题
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        // 后端默认 8080；端口被占用时可通过 VITE_BACKEND_TARGET 覆盖
        target: process.env.VITE_BACKEND_TARGET || 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
