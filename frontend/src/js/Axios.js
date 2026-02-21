import { createApp } from 'vue'
import App from './App.vue'
import axios from 'axios'
import Swal from 'sweetalert2'

const app = createApp(App)

// --- Axios 全域設定 ---

// 1. 設定基礎路徑，之後寫 API 只要寫 /api/merchants 即可
axios.defaults.baseURL = 'http://localhost:8080'

// 2. 設定請求超時時間 (例如 10 秒)
axios.defaults.timeout = 10000

// 3. 請求攔截器 (Request Interceptor)
axios.interceptors.request.use(
  (config) => {
    // 這裡可以統一加入 Token，例如：
    // config.headers['Authorization'] = 'Bearer ' + localStorage.getItem('token')
    console.log('發送請求:', config.url)
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

// 4. 回應攔截器 (Response Interceptor)
// 這裡可以統一處理後端 Result<T> 的錯誤訊息
axios.interceptors.response.use(
  (response) => {
    const res = response.data

    // 如果後端回傳的 code 不是 200，直接在這裡噴出 SweetAlert 提示
    if (res.code && res.code !== 200) {
      Swal.fire({
        icon: 'error',
        title: '操作失敗',
        text: res.message || '系統發生錯誤',
      })
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return response
  },
  (error) => {
    // 處理 HTTP 狀態碼錯誤 (如 404, 500)
    Swal.fire({
      icon: 'error',
      title: '網路錯誤',
      text: '無法連線至伺服器，請檢查後端是否啟動',
    })
    return Promise.reject(error)
  },
)

// 5. 將 axios 掛載到全域變數 (選用，Vue 3 建議直接 import)
app.config.globalProperties.$http = axios

app.mount('#app')
