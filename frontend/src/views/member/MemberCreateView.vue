<script setup>
/**
 * MemberCreateView.vue：新增會員（換裝美化版）
 */
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import Swal from 'sweetalert2'

const router = useRouter()

const member = reactive({
  memUsername: '',
  memPassword: '',
  memName: '',
  memEmail: '',
  memPhone: '',
  memInvoice: '',
  memStatus: 1,
  memLevel: 1,
})

// --- 格式驗證邏輯 (保留原樣) ---
const validateForm = () => {
  if (!member.memUsername || !member.memPassword || !member.memName || !member.memEmail || !member.memPhone) {
    return '除了發票載具，其餘欄位皆為必填'
  }

  const pwdRegex = /^(?=.*[a-zA-Z]).{6,}$/
  if (!pwdRegex.test(member.memPassword)) {
    return '密碼需至少 6 個字元，並包含至少一個英文字母'
  }

  const phoneRegex = /^09\d{8}$/
  if (!phoneRegex.test(member.memPhone)) {
    return '手機格式錯誤，請輸入 09 開頭的 10 位數字'
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.com$/
  if (!emailRegex.test(member.memEmail)) {
    return '信箱格式錯誤，必須包含 @ 且結尾為 .com'
  }

  return null
}

const submitCreate = async () => {
  const error = validateForm()
  if (error) {
    await Swal.fire({
      icon: 'warning',
      title: '格式錯誤',
      text: error,
      confirmButtonColor: '#5b9bd5',
    })
    return
  }

  try {
    const res = await axios.post('http://localhost:8080/api/members', {
      ...member,
      memStatus: 1,
      memLevel: 1,
    })

    await Swal.fire({
      icon: 'success',
      title: '新增成功',
      text: res.data?.message || '會員已新增',
      timer: 1500,
      showConfirmButton: false,
    })

    router.push('/admin/members')
  } catch (err) {
    const msg = err.response?.data?.message || '新增失敗'
    await Swal.fire({
      icon: 'error',
      title: '新增失敗',
      text: msg,
      confirmButtonColor: '#f56c6c',
    })
  }
}

const goBack = () => {
  router.push('/admin/members')
}
</script>

<template>
  <div class="create-page">
    <div class="form-card">
      <div class="card-header">
        <div class="header-icon">
          <i class="fas fa-user-plus"></i>
        </div>
        <h2>新增會員</h2>
        <p>建立新的 Take@Seat 使用者帳號</p>
      </div>

      <el-form @submit.prevent="submitCreate" label-position="top" class="admin-form">
        <el-form-item label="帳號" required>
          <el-input
            v-model="member.memUsername"
            placeholder="請輸入帳號"
            prefix-icon="User"
            autocomplete="new-username"
          />
        </el-form-item>

        <el-form-item label="密碼" required>
          <el-input
            type="password"
            v-model="member.memPassword"
            placeholder="至少 6 碼且包含英文字母"
            prefix-icon="Lock"
            autocomplete="new-password"
            show-password
          />
        </el-form-item>

        <el-form-item label="姓名" required>
          <el-input
            v-model="member.memName"
            placeholder="請輸入真實姓名"
            prefix-icon="UserFilled"
          />
        </el-form-item>

        <el-form-item label="信箱" required>
          <el-input
            v-model="member.memEmail"
            placeholder="example@mail.com"
            prefix-icon="Message"
          />
        </el-form-item>

        <el-form-item label="手機" required>
          <el-input
            v-model="member.memPhone"
            placeholder="09xxxxxxxx"
            maxlength="10"
            prefix-icon="Iphone"
          />
        </el-form-item>

        <el-form-item label="發票載具 (選填)">
          <el-input
            v-model="member.memInvoice"
            placeholder="例：/ABC1234"
            prefix-icon="Tickets"
          />
        </el-form-item>

        <div class="form-actions">
          <el-button 
            type="primary" 
            size="large" 
            native-type="submit" 
            class="submit-btn"
          >
            <i class="fas fa-check mr-2"></i> 確認新增
          </el-button>
          
          <el-button 
            size="large" 
            @click="goBack" 
            class="back-btn"
            style="margin-left: 0" 
          >
            <i class="fas fa-arrow-left mr-2"></i> 回會員列表
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
/* ========== 頁面容器 ========== */
.create-page {
  min-height: 100vh;
  padding: 40px 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ========== 表單卡片 ========== */
.form-card {
  width: 100%;
  max-width: 480px;
  background: white;
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  animation: slideUp 0.5s ease;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ========== 標題區域 ========== */
.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.header-icon {
  width: 72px;
  height: 72px;
  background: linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #3b82f6;
  margin: 0 auto 16px;
}

.card-header h2 {
  margin: 0;
  font-size: 1.6rem;
  font-weight: 700;
  color: #1e3a5f;
}

.card-header p {
  margin: 8px 0 0;
  color: #94a3b8;
  font-size: 0.95rem;
}

/* ========== 表單樣式 ========== */
.admin-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #475569;
}

.admin-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 4px 12px;
  background-color: #f8fafc;
}

/* ========== 按鈕區域 ========== */
.form-actions {
  margin-top: 32px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  color: white;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.35);
}

.back-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: white;
  color: #64748b;
}

.back-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
  background: #f0f7ff;
}

.mr-2 { margin-right: 8px; }

@media (max-width: 520px) {
  .form-card { padding: 24px; }
}
</style>