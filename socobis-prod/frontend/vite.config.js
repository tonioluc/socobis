// frontend/vite.config.js
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import devtools from 'vite-plugin-vue-devtools'

export default defineConfig({
  plugins: [vue(), devtools()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    proxy: {
      '/api': {
        // proxy API calls to the Java backend running on :8010
        target: 'http://localhost:8010/socobis',
        changeOrigin: true,
      }
    }
  }
})