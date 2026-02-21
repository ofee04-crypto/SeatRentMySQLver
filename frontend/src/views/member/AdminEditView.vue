<script setup>
/**
 * AdminEditView.vue：編輯管理員 (完整驗證與 SweetAlert 版)
 */
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from 'axios'
import Swal from 'sweetalert2'

const router = useRouter()
const route = useRoute()

const admin = ref(null)
const newPassword = ref('')
const errorMsg = ref('')
const loading = ref(false)

// 載入資料
const fetchAdmin = () => {
  loading.value = true
  const id = route.params.id
  axios
    .get(`http://localhost:8080/admins/find?admId=${id}`)
    .then((res) => {
      // res.data 應包含 adminImage 欄位
      admin.value = res.data
    })
    .catch(() => {
      errorMsg.value = '載入管理員資料失敗'
    })
    .finally(() => {
      loading.value = false
    })
}

// 驗證邏輯
const validateForm = () => {
  // 1. 信箱格式驗證：需包含 @ 且結尾為 .com
  const emailRegex = /^[^\s@]+@[^\s@]+\.com$/
  if (!emailRegex.test(admin.value.admEmail)) {
    Swal.fire({
      icon: 'warning',
      title: '信箱格式錯誤',
      text: '電子郵件必須包含 @ 且以 .com 結尾',
      confirmButtonColor: '#409eff'
    })
    return false
  }

  // 2. 密碼格式驗證：如果有填寫新密碼，則需至少 6 碼 + 1 英文字
  if (newPassword.value && newPassword.value.trim() !== '') {
    const passwordRegex = /^(?=.*[a-zA-Z]).{6,}$/
    if (!passwordRegex.test(newPassword.value)) {
      Swal.fire({
        icon: 'warning',
        title: '新密碼格式不符',
        text: '新密碼需至少 6 個字元，且包含至少 1 個英文字母',
        confirmButtonColor: '#409eff'
      })
      return false
    }
  }

  return true
}

// 提交修改
const submitEdit = () => {
  if (!validateForm()) return

  const payload = {
    ...admin.value,
    admPassword: newPassword.value || '', // 如果沒填就傳空字串，後端會判斷不更新密碼
  }

  axios
    .post('http://localhost:8080/admins/update', payload)
    .then(() => {
      Swal.fire({
        icon: 'success',
        title: '修改成功',
        text: '管理員資料已成功更新',
        timer: 1500,
        showConfirmButton: false
      }).then(() => {
        router.push('/admin/admins')
      })
    })
    .catch((err) => {
      console.error('Update Error:', err)
      Swal.fire({
        icon: 'error',
        title: '修改失敗',
        text: '請檢查輸入資料或連線狀況',
        confirmButtonColor: '#409eff'
      })
    })
}

const goBack = () => {
  router.push('/admin/admins')
}

onMounted(() => {
  fetchAdmin()
})
</script>

<template>
  <div class="edit-page">
    <div class="form-card">
      <div class="card-header">
        <div class="header-icon">
          <i class="fas fa-user-edit"></i>
        </div>
        <h2>修改管理員資料</h2>
        <p>編輯現有管理員的帳號資訊</p>
      </div>

      <el-alert 
        v-if="errorMsg" 
        :title="errorMsg" 
        type="error" 
        show-icon 
        :closable="false" 
        class="error-alert" 
      />
      
      <div v-if="loading" class="loading-state">
        <i class="fas fa-spinner fa-spin loading-icon"></i>
        <p>資料載入中...</p>
      </div>

      <el-form v-else-if="admin" @submit.prevent="submitEdit" label-position="top" class="admin-form">
        <input type="hidden" v-model="admin.admId" />

        <el-form-item label="帳號" required>
          <el-input
            v-model="admin.admUsername"
            placeholder="請輸入帳號"
            prefix-icon="User"
          />
        </el-form-item>

        <el-form-item label="新密碼">
          <el-input
            type="password"
            v-model="newPassword"
            placeholder="若不修改請留空"
            prefix-icon="Lock"
            show-password
          />
          <div class="form-hint">
            <i class="fas fa-info-circle"></i>
            需至少 6 碼並含 1 個英文字，不修改請保持空白
          </div>
        </el-form-item>

        <el-form-item label="姓名" required>
          <el-input
            v-model="admin.admName"
            placeholder="請輸入姓名"
            prefix-icon="UserFilled"
          />
        </el-form-item>

        <el-form-item label="信箱" required>
          <el-input
            v-model="admin.admEmail"
            placeholder="example@mail.com"
            prefix-icon="Message"
          />
        </el-form-item>

        <el-form-item label="角色">
          <el-select v-model="admin.admRole" placeholder="選擇角色" style="width: 100%">
            <el-option :value="1" label="一般管理員">
              <span class="role-option">
                <i class="fas fa-user-tie mr-2"></i> 一般管理員
              </span>
            </el-option>
            <el-option :value="9" label="超級管理員">
              <span class="role-option">
                <i class="fas fa-crown mr-2"></i> 超級管理員
              </span>
            </el-option>
          </el-select>
        </el-form-item>

        <div class="form-actions">
          <el-button 
            type="primary" 
            size="large" 
            native-type="submit" 
            class="submit-btn"
          >
            <i class="fas fa-save mr-2"></i> 確認修改
          </el-button>
          <el-button 
            size="large" 
            @click="goBack" 
            class="back-btn"
            style="margin-left: 0"
          >
            <i class="fas fa-arrow-left mr-2"></i> 回管理員列表
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
/* ========== 頁面容器 ========== */
.edit-page {
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
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  animation: slideUp 0.5s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ========== 標題區域 ========== */
.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.header-icon {
  width: 72px;
  height: 72px;
  background: linear-gradient(135deg, #e8f4fc 0%, #d4e8f7 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #5b9bd5;
  margin: 0 auto 16px;
  box-shadow: 0 4px 15px rgba(91, 155, 213, 0.15);
}

.card-header h2 {
  margin: 0;
  font-size: 1.6rem;
  font-weight: 700;
  color: #303133;
}

.card-header p {
  margin: 8px 0 0;
  color: #909399;
  font-size: 0.95rem;
}

/* ========== 載入狀態 ========== */
.loading-state {
  text-align: center;
  padding: 40px;
}

.loading-icon {
  font-size: 36px;
  color: #409eff;
  margin-bottom: 16px;
}

.loading-state p {
  color: #909399;
  font-size: 15px;
}

/* ========== 錯誤提示 ========== */
.error-alert {
  margin-bottom: 20px;
  border-radius: 10px;
}

/* ========== 表單樣式 ========== */
.admin-form {
  margin-top: 24px;
}

.admin-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #606266;
}

.admin-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 4px 12px;
}

.admin-form :deep(.el-select .el-input__wrapper) {
  border-radius: 10px;
}

.form-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 6px;
}

.form-hint i {
  color: #409eff;
}

.role-option {
  display: flex;
  align-items: center;
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
  background: linear-gradient(135deg, #5b9bd5 0%, #7cb9e8 100%);
  border: none;
  transition: all 0.3s ease;
  color: white;
  cursor: pointer;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(91, 155, 213, 0.35);
}

.submit-btn:active {
  transform: scale(0.95);
}

.back-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  border-radius: 10px;
  border: 2px solid #e8ecf1;
  background: transparent;
  color: #606266;
  transition: all 0.3s ease;
  cursor: pointer;
}

.back-btn:hover {
  border-color: #5b9bd5;
  color: #5b9bd5;
  background: rgba(91, 155, 213, 0.05);
}

.back-btn:active {
  transform: scale(0.95);
}

/* ========== 工具類 ========== */
.mr-2 { 
  margin-right: 8px; 
}

/* ========== 響應式設計 ========== */
@media (max-width: 520px) {
  .form-card {
    padding: 24px;
    margin: 16px;
  }
  
  .header-icon {
    width: 60px;
    height: 60px;
    font-size: 24px;
  }
}
</style>