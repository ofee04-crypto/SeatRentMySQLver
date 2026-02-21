import './assets/custom.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import axios from 'axios'

// Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// --- [ADD] 引入 vue-google-maps ---
import VueGoogleMaps from '@fawmi/vue-google-maps'
// Particles.js（Vue 3 版）
import Particles from '@tsparticles/vue3'
import { loadSlim } from '@tsparticles/slim'

import VueApexCharts from 'vue3-apexcharts'

import App from './App.vue'
import router from './router'

import { ElMessageBox } from 'element-plus'

// Axios request interceptor
axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

let isAuthExpiredDialogShowing = false

axios.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const url = error.config?.url || ''
    const currentPath = window.location.hash || window.location.pathname // 取得當前路徑

    // 修改邏輯：
    // 1. 排除登入相關請求
    // 2. 只有在「非首頁」且「非登入頁」發生 401 時才強行踢出
    // 3. 增加對 /member/profile 的容錯，避免剛進首頁就噴錯
    if (
      status === 401 &&
      !url.includes('/login') &&
      !url.includes('/oauth2') &&
      !url.includes('/api/auth/me') &&
      !isAuthExpiredDialogShowing
    ) {
      // 如果是在首頁 (#/)，通常是靜態展示，我們就默默清理掉過期資訊就好，不跳彈窗
      if (currentPath === '#/' || currentPath === '/') {
        localStorage.removeItem('token')
        localStorage.removeItem('admin')
        localStorage.removeItem('member_user')
        return Promise.reject(error)
      }

      // 只有在試圖進入「管理後台」或「個人設定」等深層頁面時，才跳出警示
      isAuthExpiredDialogShowing = true

      ElMessageBox.alert('登入已過期或權限不足，請重新登入', '存取受限', {
        confirmButtonText: '確認',
        type: 'warning',
        showClose: false,
        closeOnClickModal: false,
      }).then(() => {
        isAuthExpiredDialogShowing = false
        // 清理所有身分緩存
        localStorage.clear()
        router.push('/login')
      })
    }

    return Promise.reject(error)
  },
)

const app = createApp(App)

app.use(createPinia())
app.use(router)

// Element Plus
app.use(ElementPlus)

// ApexCharts
app.use(VueApexCharts)

// --- 啟用 vue-google-maps 並設定 API 金鑰 ---
app.use(VueGoogleMaps, {
  load: {
    key: import.meta.env.VITE_GOOGLE_MAPS_API_KEY,
    libraries: ['marker', 'places'], // 載入 marker 和 places 函式庫
    v: 'quarterly', // 指定載入穩定的 API 版本
  },
})

// --- [NEW] 4. 自動註冊所有圖示 (解決方塊亂碼問題) ---
// Element Plus Icons
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 初始化 Particles（只做一次）
app.use(Particles, {
  init: async (engine) => {
    await loadSlim(engine)
  },
})

app.mount('#app')
