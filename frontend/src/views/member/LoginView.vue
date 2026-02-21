<script setup>
import { ref, reactive, watch } from 'vue' // 引入 reactive
import { useRouter, useRoute } from 'vue-router' // 引入 useRoute
import axios from 'axios'

//  SweetAlert2：統一用彈窗顯示成功/失敗訊息（取代 errorMsg/successMsg）
import Swal from 'sweetalert2'

//  Pinia：引入兩種 store
import { useAdminAuthStore } from '@/stores/adminAuth'
import { useMemberAuthStore } from '@/stores/memberAuth'

const adminAuthStore = useAdminAuthStore()
const memberAuthStore = useMemberAuthStore()
const router = useRouter()
const route = useRoute() // 建立 route 實例

/**
 * 登入類型：
 * - member：會員登入（導向 /member/profile）
 * - admin：管理員登入（導向 /admin）
 */
const loginType = ref('member') // member | admin

// --- 忘記密碼相關狀態 ---
const viewMode = ref('login') // login | forgot | reset
const forgotForm = reactive({
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

// 返回登入並清空資料
const backToLogin = () => {
  viewMode.value = 'login'
  forgotForm.email = ''
  forgotForm.code = ''
}
// -----------------------

// 共用輸入欄位（會員/管理員都用同一組欄位）
const account = ref('')
const password = ref('')

// 切換登入身分時，清空輸入，避免誤送上一種身分的帳密
watch(loginType, () => {
  account.value = ''
  password.value = ''
})

/**
 * 登入主流程（async/await）
 * 目的：
 * 1) 避免 Promise then/catch 的順序 race condition
 * 2) 確保「先寫入 localStorage」再導頁，路由守衛才會放行
 */
const login = async () => {
  try {
    // =========================
    // 會員登入
    // =========================
    if (loginType.value === 'member') {
      const res = await axios.post(
        'http://localhost:8080/login/member',
        {
          memUsername: account.value,
          memPassword: password.value,
        },
        {
          withCredentials: true,
        },
      )

      /**
       * token 儲存規則（統一 localStorage）
       * - main.js 的 axios interceptor 從 localStorage 取 token
       * - 所以這裡也必須存 localStorage
       * - 若後端沒回 token，暫用 'login-ok'（你原本 HEAD 的策略）避免流程卡死
       */
      const token = res.data?.token || 'login-ok'
      localStorage.setItem('token', token)

      adminAuthStore.clearAdmin()
      localStorage.removeItem('admin')

      //  [+] 將登入狀態存入 Pinia，讓整個 App 保持同步
      memberAuthStore.setMemberLogin({
        memId: res.data.memId,
        memUsername: res.data.memUsername,
        memName: res.data.memName,
        memPoints: res.data.memPoints,
        memInvoice: res.data.memInvoice,
      })

      //  [+] 將會員資料存入 localStorage，實現持久化登入
      localStorage.setItem('member_user', JSON.stringify(res.data))

      // 成功提示
      await Swal.fire({
        icon: 'success',
        title: '登入成功',
        text: '歡迎回來！',
        showConfirmButton: false,
        timer: 1200,
      })

      //  檢查是否有重定向路徑，若有則跳轉，否則導向會員頁
      if (route.query.redirect) {
        router.push(route.query.redirect)
      } else {
        router.push('/')
      }
      return
    }

    // =========================
    // 管理員登入 (最簡化修正版)
    // =========================
    const res = await axios.post(
      'http://localhost:8080/login/admin',
      {
        admUsername: account.value,
        admPassword: password.value,
      },
      {
        withCredentials: true,
      },
    )

    // 1. 只清掉「會員」的資料，不要用 clear()
    localStorage.removeItem('member_user')
    memberAuthStore.clearMemberLogin()

    // 2. 存入管理員 Token 與資料
    const token = res.data?.token || 'admin-login-ok'
    localStorage.setItem('token', token)

    const adminData = {
      username: res.data?.admUsername,
      name: res.data?.admName,
      role: res.data?.admRole,
      admId: res.data?.admId,          // 補上 ID
      admEmail: res.data?.admEmail,    // 補上 Email
      admCreatedAt: res.data?.admCreatedAt // 補上到職日
    }

    // 3. 同步寫入 Pinia 和 LocalStorage
    adminAuthStore.setAdmin(adminData)
    localStorage.setItem('admin', JSON.stringify(adminData))

    // 4. 成功提示
    await Swal.fire({
      icon: 'success',
      title: '管理員登入成功',
      text: '歡迎回來！',
      showConfirmButton: false,
      timer: 1000,
    })

    //  進後台（replace 避免回上一頁又回到登入頁）
    router.push('/admin')
  } catch (err) {
    console.error(err)

    // 錯誤訊息整理：後端可能回字串或物件
    let errorText = '登入失敗'
    if (err.response && err.response.data) {
      const msg = err.response.data
      errorText = typeof msg === 'object' ? JSON.stringify(msg) : String(msg)
    }

    // 失敗提示
    Swal.fire({
      icon: 'error',
      title: '登入失敗',
      text: errorText,
      confirmButtonText: '確認',
    })
  }
}

// 發送驗證碼邏輯
const handleSendCode = async () => {
  if (!forgotForm.email) {
    Swal.fire('錯誤', '請填寫電子郵件', 'warning')
    return
  }
  // 顯示讀取中，因為寄信需要 2~5 秒
  Swal.fire({
    title: '正在發送驗證碼...',
    allowOutsideClick: false,
    didOpen: () => { Swal.showLoading() }
  })
  try {
    // 根據目前的登入身分(member/admin)決定 API 路徑
    const url = loginType.value === 'admin' 
      ? 'http://localhost:8080/api/admin/forgot-password/send-code' 
      : 'http://localhost:8080/api/forgot-password/send-code';

    await axios.post(url, { email: forgotForm.email })
    
    Swal.close() // 關閉 Loading
    await Swal.fire('成功', '驗證碼已發送至您的信箱', 'success')
    viewMode.value = 'reset'
  } catch (err) {
    Swal.close()
    console.error(err)
    // 顯示後端傳回的錯誤訊息（例如：帳號不存在）
    const errorMsg = err.response?.data || '發送失敗，請檢查網路或信箱'
    Swal.fire('失敗', errorMsg, 'error')
  }
}

// 重設密碼邏輯
const handleResetPassword = async () => {
  // 基本檢查
  if (!forgotForm.code || !forgotForm.newPassword) {
    Swal.fire('錯誤', '請完整填寫驗證碼與新密碼', 'warning')
    return
  }

  if (forgotForm.newPassword !== forgotForm.confirmPassword) {
    Swal.fire('錯誤', '兩次密碼輸入不一致', 'error')
    return
  }

  try {
    // 根據目前的登入身分(member/admin)決定 API 路徑
    const url = loginType.value === 'admin' 
      ? 'http://localhost:8080/api/admin/forgot-password/reset' 
      : 'http://localhost:8080/api/forgot-password/reset';

    await axios.post(url, forgotForm)
    
    await Swal.fire('成功', '密碼重設成功，請重新登入', 'success')
    backToLogin()
  } catch (err) {
    console.error(err)
    const errorMsg = err.response?.data || '驗證碼錯誤或已過期'
    Swal.fire('重設失敗', errorMsg, 'error')
  }
}

/**
 * Particles 背景設定（只影響視覺，不影響登入邏輯）
 * 注意：main.js 需已 app.use(Particles) 且已載入 loadSlim
 */
const particlesOptions = {
  background: {
    color: { value: '#e9ecef' },
  },
  particles: {
    color: { value: '#6c757d' },
    links: {
      enable: true,
      color: '#6c757d',
      distance: 150,
      opacity: 0.3,
      width: 1,
    },
    move: {
      enable: true,
      speed: 1.2,
    },
    number: {
      value: 60,
    },
    size: {
      value: { min: 1, max: 3 },
    },
  },
  interactivity: {
    events: {
      onHover: {
        enable: true,
        mode: 'grab',
      },
    },
  },
}

const goRegister = () => {
  router.push('/register')
}

const loginWithGoogle = () => {
  // 加上時間戳記 t=${Date.now()} 確保每次請求都是新的，不被瀏覽器緩存網址
  const googleLoginUrl = `http://localhost:8080/oauth2/authorization/google?prompt=select_account&t=${Date.now()}`
  window.location.href = googleLoginUrl
}
</script>

<template>
  <div class="login-page">
    <vue-particles id="tsparticles" :options="particlesOptions" class="particles-bg" />

    <div class="login-box">
      <div class="card card-outline card-primary">
        <div class="card-header text-center">
          <h1 class="h1"><b>SeatRentSys</b></h1>
        </div>
        <div class="card-body">
          
          <div v-if="viewMode === 'login'">
            <div class="login-switch">
              <button
                :class="{ active: loginType === 'member' }"
                @click="loginType = 'member'"
                type="button"
              >
                會員登入
              </button>

              <button
                :class="{ active: loginType === 'admin' }"
                @click="loginType = 'admin'"
                type="button"
              >
                管理員登入
              </button>
            </div>

            <form @submit.prevent="login">
              <div class="input-group mb-3">
                <input
                  v-model="account"
                  type="text"
                  class="form-control"
                  :placeholder="loginType === 'member' ? '會員帳號' : '管理員帳號'"
                  required
                />
              </div>
              <div class="input-group mb-3">
                <input
                  v-model="password"
                  type="password"
                  class="form-control"
                  :placeholder="loginType === 'member' ? '會員密碼' : '管理員密碼'"
                  required
                />
              </div>

              <div class="helper-links">
                <span v-if="loginType === 'member'" @click="goRegister">註冊會員</span>
                
                <span v-else></span>

                <span @click="viewMode = 'forgot'">忘記密碼？</span>
              </div>

              <div class="google-login-section">
                <div class="divider">
                  <span>或使用第三方登入</span>
                </div>

                <button type="button" class="btn-google" @click="loginWithGoogle">
                  <img
                    src="https://developers.google.com/static/identity/images/g-logo.png"
                    alt="Google"
                  />
                  使用 Google 帳號登入
                </button>
              </div>

              <button type="submit" class="btn btn-primary btn-block">登入</button>
              <div class="back-to-home" @click="router.push('/')">
                <el-icon><ArrowLeft /></el-icon>
                <span>返回首頁</span>
              </div>
            </form>
          </div>

          <div v-else-if="viewMode === 'forgot'">
            <h2 class="sub-title">忘記密碼</h2>
            <p class="desc-text">請輸入您的電子郵件，我們將發送驗證碼至您的信箱</p>
            
            <div class="input-group mb-3">
              <input
                v-model="forgotForm.email"
                type="email"
                class="form-control"
                placeholder="請輸入註冊的 E-MAIL"
              />
            </div>
            
            <button @click="handleSendCode" class="btn btn-primary btn-block">發送驗證碼</button>
            
            <div class="back-link-center" @click="backToLogin">
              <span>返回登入頁面</span>
            </div>
          </div>

          <div v-else-if="viewMode === 'reset'">
            <h2 class="sub-title">重設密碼</h2>
            <div class="info-text">驗證碼已發送至：{{ forgotForm.email }}</div>
            
            <div class="input-group mb-3">
              <input v-model="forgotForm.code" type="text" class="form-control" placeholder="請輸入 6 位驗證碼" />
            </div>
            <div class="input-group mb-3">
              <input v-model="forgotForm.newPassword" type="password" class="form-control" placeholder="新密碼" />
            </div>
            <div class="input-group mb-3">
              <input v-model="forgotForm.confirmPassword" type="password" class="form-control" placeholder="再次確認新密碼" />
            </div>
            
            <button @click="handleResetPassword" class="btn btn-primary btn-block">確認重設</button>
            
            <div class="back-link-center" @click="viewMode = 'forgot'">
              <span>重新輸入電子郵件</span>
            </div>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ========== 登入頁面主容器 ========== */
.login-page {
  position: relative;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: linear-gradient(135deg, #9db8d4 0%, #8aa5c1 100%);
}

/* 粒子背景：最底層 */
.particles-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
}

/* ========== 登入框：Glassmorphism 風格 ========== */
.login-box {
  width: 400px;
  position: relative;
  z-index: 10;
}

.login-box .card {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  overflow: hidden;
}

.login-box .card-header {
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
  padding: 24px;
  border-bottom: none;
}

.login-box .card-header h1 {
  color: #ffffff;
  font-size: 1.8rem;
  font-weight: 700;
  margin: 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.login-box .card-body {
  padding: 28px;
}

/* ========== 登入切換按鈕 ========== */
.login-switch {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 24px;
}

.login-switch button {
  padding: 10px 24px;
  border: 2px solid #e2e8f0;
  background-color: #f8fafc;
  color: #475569;
  cursor: pointer;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    color 0.2s ease;
}

.login-switch button:hover {
  background-color: #e2e8f0;
  border-color: #cbd5e1;
}

.login-switch button.active {
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
  color: #ffffff;
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

/* ========== 輸入欄位 ========== */
.login-box .input-group {
  margin-bottom: 16px;
}

.login-box .form-control {
  padding: 12px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  font-size: 15px;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
  background: #f8fafc;
}

.login-box .form-control:focus {
  border-color: #60a5fa;
  box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.2);
  outline: none;
  background: #ffffff;
}

.login-box .form-control::placeholder {
  color: #94a3b8;
}

/* ========== 輔助連結 (註冊 + 忘記密碼) ========== */
.helper-links {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}

.helper-links span {
  font-size: 14px;
  color: #3b82f6;
  cursor: pointer;
  font-weight: 500;
  transition: color 0.2s ease;
}

.helper-links span:hover {
  color: #1d4ed8;
  text-decoration: underline;
}

/* ========== 登入按鈕 ========== */
.login-box .btn-primary {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  cursor: pointer;
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.login-box .btn-primary:hover {
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
}

.login-box .btn-primary:active {
  transform: translateY(1px);
}

/* ========== 忘記密碼專用樣式 ========== */
.sub-title {
  text-align: center;
  font-size: 20px;
  color: #333;
  margin-bottom: 15px;
}

.desc-text {
  font-size: 13px;
  color: #64748b;
  text-align: center;
  margin-bottom: 20px;
  line-height: 1.5;
}

.info-text {
  font-size: 14px;
  background-color: #f1f5f9;
  padding: 10px;
  border-radius: 8px;
  color: #475569;
  margin-bottom: 15px;
  text-align: center;
}

.back-link-center {
  margin-top: 20px;
  text-align: center;
  cursor: pointer;
}

.back-link-center span {
  font-size: 14px;
  color: #64748b;
  transition: color 0.2s;
}

.back-link-center:hover span {
  color: #3b82f6;
  text-decoration: underline;
}

/* ========== Google 登入與返回首頁  ========== */
.back-to-home {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 15px;
  /* 與登入按鈕拉開距離 */
  padding-top: 10px;
  border-top: 1px inset #f0f0f0; /* 加一條淡淡的分隔線 */
  color: #6c757d;
  /* 灰色，不搶走登入按鈕的焦點 */
  font-size: 14px;
  cursor: pointer;
  transition: color 0.2s;
  gap: 5px;
}

.back-to-home:hover {
  color: #007bff;
  /* 懸浮時變回藍色 */
  text-decoration: underline;
}

/* 確保 el-icon 在文字中間 */
.back-to-home .el-icon {
  font-size: 12px;
}

.google-login-section {
  margin-top: 20px;
}

.divider {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  color: #888;
  font-size: 12px;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #eee;
}

.divider span {
  padding: 0 10px;
}

.btn-google {
  width: 100%;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background-color: #ffffff;
  border: 1px solid #dadce0;
  border-radius: 4px;
  color: #3c4043;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-google img {
  width: 18px;
  height: 18px;
}

.btn-google:hover {
  background-color: #f8f9fa;
  box-shadow:
    0 1px 2px 0 rgba(60, 64, 67, 0.3),
    0 1px 3px 1px rgba(60, 64, 67, 0.15);
}
</style>