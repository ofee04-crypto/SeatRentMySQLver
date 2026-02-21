<script setup>
import { ref, watch } from 'vue'
import axios from 'axios'
import Swal from 'sweetalert2'

const props = defineProps({
  show: Boolean,
  adminData: Object
})

const emit = defineEmits(['close', 'update-success'])

// 表單資料
const form = ref({
  password: '',
  confirmPassword: ''
})

// 格式化日期
const formatDate = (dt) => {
  if (!dt) return 'N/A'
  return dt.substring(0, 10).replace(/-/g, '/')
}

// 取得頭像邏輯 (比照你的管理員列表)
const getAvatar = (id) => {
  if (id >= 1 && id <= 10) {
    const fileName = String(id).padStart(2, '0')
    return `/admin/${fileName}.jpg`
  }
  return '/admin/default.png'
}

// 儲存變更
// 儲存變更
const handleSave = async () => {
  // 1. 如果沒輸入密碼，代表不改密碼，直接關閉
  if (!form.value.password) {
    emit('close')
    return
  }

  // 2. 檢查兩次密碼是否一致
  if (form.value.password !== form.value.confirmPassword) {
    Swal.fire({ icon: 'error', title: '密碼不一致', text: '請再次確認新密碼' })
    return
  }

  try {
    // 3. 準備要傳給 AdminController.java 的物件
    // 這裡的 key 必須對應到你 Admin.java 的欄位名稱
    const updateData = {
      admId: props.adminData.admId,       // 從 props 拿 ID
      admUsername: props.adminData.admUsername, 
      admName: props.adminData.admName,
      admEmail: props.adminData.admEmail,
      admRole: props.adminData.admRole,
      admPassword: form.value.password   // 這是你要更新的新密碼
    };

    // 4. 改呼叫你自己的 API：/admins/update
    // 這裡用 POST，因為你的 Controller 寫的是 @PostMapping("/update")
    const response = await axios.post('http://localhost:8080/admins/update', updateData);

    // 5. 判斷回傳字串 (因為你的 Controller 回傳的是 String "管理員修改成功")
    if (response.data === "管理員修改成功") {
      await Swal.fire({ 
        icon: 'success', 
        title: '密碼修改成功', 
        timer: 1500, 
        showConfirmButton: false 
      });
      emit('update-success');
      emit('close');
    } else {
      Swal.fire({ icon: 'error', title: '儲存失敗', text: response.data });
    }

  } catch (error) {
    console.error('更新失敗:', error);
    // 這裡報錯通常是連線問題，因為你走的是自己的路徑，應該不會再有 401 了
    Swal.fire({ icon: 'error', title: '連線失敗', text: '請檢查後端伺服器是否運行' });
  }
}

// 每次開啟時清空密碼欄位
watch(() => props.show, (newVal) => {
  if (newVal) {
    form.value.password = ''
    form.value.confirmPassword = ''
  }
})
</script>

<template>
  <div v-if="show" class="modal-overlay" @click.self="emit('close')">
    <div class="profile-card">
      <div class="profile-header">
        <div class="header-title">
          <i class="fas fa-address-card"></i>
          <span>管理員個人資訊</span>
        </div>
        <button class="close-x" @click="emit('close')">&times;</button>
      </div>

      <div class="profile-body">
        <div class="left-panel">
          <div class="avatar-wrapper">
            <img :src="getAvatar(adminData?.admId)" />
          </div>
          <h3 class="admin-name">{{ adminData?.admName }}</h3>
          <span class="admin-tag">@{{ adminData?.admUsername }}</span>
        </div>

        <div class="right-panel">
          <div class="info-section">
            <div class="section-divider">個人資訊</div>
            <div class="detail-grid">
              <div class="detail-item">
                <label>管理員 ID</label>
                <div class="value">{{ adminData?.admId }}</div>
              </div>
              <div class="detail-item">
                <label>電子信箱</label>
                <div class="value">{{ adminData?.admEmail }}</div>
              </div>
              <div class="detail-item">
                <label>目前權限</label>
                <div class="value">
                  <span class="role-badge" :class="adminData?.admRole === 9 ? 'super' : 'normal'">
                    {{ adminData?.admRole === 9 ? '超級管理員' : '一般管理員' }}
                  </span>
                </div>
              </div>
              <div class="detail-item">
                <label>到職日期</label>
                <div class="value">{{ formatDate(adminData?.createdAt) }}</div>
              </div>
            </div>
          </div>

          <div class="info-section">
            <div class="section-divider">安全性設定</div>
            <div class="password-fields">
              <div class="input-group">
                <label>修改密碼 (留空則不更動)</label>
                <input 
                  type="password" 
                  v-model="form.password" 
                  placeholder="請輸入新密碼" 
                />
              </div>
              <div class="input-group">
                <label>再次確認新密碼</label>
                <input 
                  type="password" 
                  v-model="form.confirmPassword" 
                  placeholder="請再次輸入新密碼" 
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="profile-footer">
        <button class="btn-cancel" @click="emit('close')">關閉視窗</button>
        <button class="btn-save" @click="handleSave">儲存變更</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(3px);
}

.profile-card {
  background: white;
  width: 750px;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.profile-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 20px;
  font-weight: 700;
  color: #333;
}

.header-title i {
  color: #1890ff;
}

.close-x {
  border: none;
  background: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
}

.profile-body {
  display: flex;
  min-height: 400px;
}

/* 左側樣式 */
.left-panel {
  flex: 1;
  background: #fcfcfc;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
}

.avatar-wrapper {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 4px solid #fff;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  overflow: hidden;
  margin-bottom: 16px;
}

.avatar-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.admin-name {
  font-size: 22px;
  margin: 0;
  color: #262626;
}

.admin-tag {
  color: #8c8c8c;
  font-size: 14px;
}

/* 右側樣式 */
.right-panel {
  flex: 2;
  padding: 30px;
}

.section-divider {
  font-weight: 600;
  color: #1890ff;
  border-left: 4px solid #1890ff;
  padding-left: 10px;
  margin-bottom: 20px;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 30px;
}

.detail-item label {
  display: block;
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.detail-item .value {
  font-size: 15px;
  color: #262626;
  font-weight: 500;
}

.role-badge {
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  color: white;
}

.role-badge.super { background: #f5222d; }
.role-badge.normal { background: #1890ff; }

.password-fields {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.input-group label {
  display: block;
  font-size: 13px;
  color: #595959;
  margin-bottom: 6px;
}

.input-group input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  transition: all 0.3s;
}

.input-group input:focus {
  border-color: #40a9ff;
  outline: none;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

/* Footer */
.profile-footer {
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
  text-align: right;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn-cancel {
  padding: 8px 20px;
  background: #f0f0f0;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  color: #595959;
}

.btn-save {
  padding: 8px 20px;
  background: #1890ff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  color: white;
  transition: background 0.3s;
}

.btn-save:hover {
  background: #40a9ff;
}
</style>