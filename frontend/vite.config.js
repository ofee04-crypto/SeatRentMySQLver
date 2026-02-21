import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig({
  plugins: [
    vue({
      template: {
        compilerOptions: {
          //所有以 gmpx- 開頭的標籤都是 Web Components
          isCustomElement: (tag) => tag.startsWith('gmpx-'),
        },
      },
    }),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  // ==================== 2026-02-01: 已改用 Coze OpenAPI，不再需要 WebSDK ====================
  // 保留 exclude/external 以防任何殘留 import 造成編譯錯誤
  optimizeDeps: {
    exclude: ['@coze/chat-sdk'],
  },
  build: {
    rollupOptions: {
      external: ['@coze/chat-sdk'],
    },
  },
  // ==================== 結束 ====================
  server: {
    //允許cloudflare和localhost進入
    host: true,
    allowedHosts: ['.trycloudflare.com', 'localhost'],
    proxy: {
      // ⚠️ 重點修正 1：針對「據點 (Spot)」的特殊處理
      // 前端呼叫 /api/spot -> Vite 幫忙去掉 /api -> 後端收到 /spot
      '/api/spot': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },

      // ⚠️ 重點修正 2：針對「座位 (Seat)」的特殊處理
      // 前端呼叫 /api/seat -> Vite 幫忙去掉 /api -> 後端收到 /seat
      '/api/seat': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },

      // 3. 維修模組 (保留 /api)
      // 因為你的 MaintenanceController 是真的有寫 @RequestMapping("/api/maintenance")
      // 所以這個規則要放在後面，讓上面兩個特定規則先被抓到
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },

      // 4. 圖片資源
      '/images': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },

      // 5. 座位與據點的備用規則 (預防他們前端改用 /seat 開頭)
      '/seat': { target: 'http://localhost:8080', changeOrigin: true },
      '/spot': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
