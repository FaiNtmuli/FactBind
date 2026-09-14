import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// The dev server proxies every /api call to the Spring Boot backend, so the frontend can use
// relative URLs and no CORS configuration is needed during development.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // 契约在仓库根（../contracts），Vite 默认只允许读项目目录内的文件
    fs: {
      allow: ['..'],
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    css: false,
    restoreMocks: true,
  },
})
