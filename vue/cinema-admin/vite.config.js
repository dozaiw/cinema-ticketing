import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 9527,
    open: true,
    proxy: {
      // ⭐ 认证服务 - 通过代理访问 8000 端口
      '/auth': {
        target: 'http://127.0.0.1:8000',
        changeOrigin: true,
        secure: false,
        // 重写路径（可选）
        rewrite: (path) => path
      },
      // 电影服务
      '/movie': {
        target: 'http://127.0.0.1:8001',
        changeOrigin: true,
        secure: false
      },
      '/comment': {
        target: 'http://127.0.0.1:8001',
        changeOrigin: true,
        secure: false
      },
      // 影院/排片服务
      '/cinema': {
        target: 'http://127.0.0.1:8002',
        changeOrigin: true,
        secure: false
      },
      '/hall': {
        target: 'http://127.0.0.1:8002',
        changeOrigin: true,
        secure: false
      },
      '/schedule': {
        target: 'http://127.0.0.1:8002',
        changeOrigin: true,
        secure: false
      },
      '/seatSchedule': {
        target: 'http://127.0.0.1:8002',
        changeOrigin: true,
        secure: false
      },
      '/seat': {
        target: 'http://127.0.0.1:8002',
        changeOrigin: true,
        secure: false
      },
      // 订单服务
      '/order': {
        target: 'http://127.0.0.1:8003',
        changeOrigin: true,
        secure: false
      },
      // 演员和类型服务
      '/actor': {
        target: 'http://127.0.0.1:8001',
        changeOrigin: true,
        secure: false
      },
      '/genre': {
        target: 'http://127.0.0.1:8001',
        changeOrigin: true,
        secure: false
      },
      '/movie-staff': {
        target: 'http://127.0.0.1:8001',
        changeOrigin: true,
        secure: false
      },
      '/advertisement': {
        target: 'http://127.0.0.1:8001',
        changeOrigin: true,
        secure: false
      }
    }
  }
})
