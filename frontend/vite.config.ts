import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// The dev server proxies every /api call to the Spring Boot backend, so the frontend can use
// relative URLs and no CORS configuration is needed during development.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
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
