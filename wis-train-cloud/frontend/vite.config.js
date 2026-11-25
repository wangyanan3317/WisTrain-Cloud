import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// /api 代理到后端 8080；智能体会话 SSE 也走此代理
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
