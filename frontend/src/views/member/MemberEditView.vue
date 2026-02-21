<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from 'axios'
import Swal from 'sweetalert2'

const router = useRouter()
const route = useRoute()

// 保留你原本的初始結構與回填邏輯
const member = ref({
  memId: '',
  memUsername: '',
  memName: '',
  memEmail: '',
  memPhone: '',
  memPoints: 0,
  memInvoice: ''
})
const newPassword = ref('')
const errorMsg = ref('')
const isSubmitting = ref(false)
const isLoading = ref(true)

const fetchMember = async () => {
  const id = route.params.id
  isLoading.value = true
  try {
    const res = await axios.get(`http://localhost:8080/api/members/find`, {
      params: { memId: id },
    })
    
    const data = res.data
    member.value = {
      ...data,
      memEmail: data.memEmail || data.email || '', 
      memPhone: data.memPhone || data.phone || ''
    }
  } catch (err) {
    errorMsg.value = '載入會員資料失敗'
    Swal.fire({ icon: 'error', title: '載入失敗', text: errorMsg.value })
  } finally {
    isLoading.value = false
  }
}

// 保留原本的驗證邏輯
const validateEditForm = () => {
  const m = member.value
  if (!m.memUsername || !m.memName || !m.memEmail || !m.memPhone) {
    return '基本資料欄位皆為必填'
  }
  if (newPassword.value) {
    const pwdRegex = /^(?=.*[a-zA-Z]).{6,}$/
    if (!pwdRegex.test(newPassword.value)) {
      return '新密碼需至少 6 個字元，並包含至少一個英文字母'
    }
  }
  const phoneRegex = /^09\d{8}$/
  if (!phoneRegex.test(m.memPhone)) {
    return '手機格式錯誤，請輸入 09 開頭的 10 位數字'
  }
  const emailRegex = /^[^\s@]+@[^\s@]+\.com$/
  if (!emailRegex.test(m.memEmail)) {
    return '信箱格式錯誤，必須包含 @ 且結尾為 .com'
  }
  if (m.memInvoice) {
    const invoiceRegex = /^\/[A-Z0-9]{7}$/
    if (!invoiceRegex.test(m.memInvoice)) {
      return '載具格式錯誤！請輸入 / 開頭加上 7 碼大寫英數組合'
    }
  }
  return null
}

const submitEdit = async () => {
  if (!member.value) return
  const error = validateEditForm()
  if (error) {
    await Swal.fire({
      icon: 'warning',
      title: '格式錯誤',
      text: error,
      confirmButtonColor: '#5b9bd5',
    })
    return
  }

  // 保留二次確認視窗
  const confirmResult = await Swal.fire({
    title: '確定要修改嗎？',
    text: "修改後的資料將會立即生效！",
    icon: 'question',
    showCancelButton: true,
    confirmButtonColor: '#5b9bd5',
    cancelButtonColor: '#909399',
    confirmButtonText: '修改',
    cancelButtonText: '取消'
  })

  if (!confirmResult.isConfirmed) return

  errorMsg.value = ''
  isSubmitting.value = true

  try {
    const payload = {
      ...member.value,
      ...(newPassword.value ? { memPassword: newPassword.value } : {}),
    }
    await axios.post('http://localhost:8080/api/members/update', payload)
    await Swal.fire({
      icon: 'success',
      title: '修改成功',
      text: '會員資料已更新',
      timer: 1500,
      showConfirmButton: false
    })
    router.push('/admin/members')
  } catch (err) {
    const msg = err?.response?.data?.message || '修改失敗'
    await Swal.fire({
      icon: 'error',
      title: '修改失敗',
      text: msg,
      confirmButtonColor: '#f56c6c',
    })
  } finally {
    isSubmitting.value = false
  }
}

const goBack = () => router.push('/admin/members')
onMounted(fetchMember)
</script>

<template>
  <div class="edit-page">
    <div class="form-card">
      <div class="card-header">
        <div class="header-icon">
          <i class="fas fa-user-edit"></i>
        </div>
        <h2>修改會員資料</h2>
        <p>編輯 Take@Seat 會員帳號與點數資訊</p>
      </div>

      <el-alert 
        v-if="errorMsg" 
        :title="errorMsg" 
        type="error" 
        show-icon 
        :closable="false" 
        class="error-alert" 
      />
      
      <div v-if="isLoading" class="loading-state">
        <i class="fas fa-spinner fa-spin loading-icon"></i>
        <p>資料載入中...</p>
      </div>

      <el-form v-else @submit.prevent="submitEdit" label-position="top" class="admin-form">
        <input type="hidden" v-model="member.memId" />

        <el-form-item label="帳號" required>
          <el-input v-model="member.memUsername" prefix-icon="User" />
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
            需至少 6 碼並含英文字，不修改請保持空白
          </div>
        </el-form-item>

        <el-form-item label="姓名" required>
          <el-input v-model="member.memName" prefix-icon="UserFilled" />
        </el-form-item>

        <el-form-item label="信箱" required>
          <el-input v-model="member.memEmail" prefix-icon="Message" />
        </el-form-item>

        <el-form-item label="電話" required>
          <el-input v-model="member.memPhone" maxlength="10" prefix-icon="Iphone" />
        </el-form-item>

        <el-form-item label="點數">
          <el-input v-model.number="member.memPoints" type="number" prefix-icon="Coin" />
        </el-form-item>

        <el-form-item label="發票載具">
          <el-input v-model="member.memInvoice" placeholder="例：/ABC1234" prefix-icon="Tickets" />
        </el-form-item>

        <div class="form-actions">
          <el-button 
            type="primary" 
            size="large" 
            native-type="submit" 
            class="submit-btn"
            :loading="isSubmitting"
          >
            <i class="fas fa-save mr-2"></i> {{ isSubmitting ? '修改中...' : '確認修改' }}
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
/* 套用管理員修改頁面的高級樣式 */
.edit-page {
  min-height: 100vh;
  padding: 40px 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

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

.loading-state {
  text-align: center;
  padding: 40px;
}

.loading-icon {
  font-size: 36px;
  color: #409eff;
  margin-bottom: 16px;
}

.error-alert {
  margin-bottom: 20px;
  border-radius: 10px;
}

.admin-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #606266;
}

.admin-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 4px 12px;
}

.form-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 6px;
}

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
  color: white;
  transition: all 0.3s ease;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(91, 155, 213, 0.35);
}

.back-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
  border-radius: 12px;
  border: 1px solid #e8ecf1;
  background: transparent;
  color: #606266;
}

.back-btn:hover {
  border-color: #5b9bd5;
  color: #5b9bd5;
  background: rgba(91, 155, 213, 0.05);
}

.mr-2 { margin-right: 8px; }
</style>