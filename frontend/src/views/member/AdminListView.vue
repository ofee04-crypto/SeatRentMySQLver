<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import Swal from 'sweetalert2'
import { useAdminAuthStore } from '@/stores/adminAuth'

const router = useRouter()
const admins = ref([])
const keyword = ref('')
const loading = ref(false)
const authStore = useAdminAuthStore()

const showRoleModal = ref(false)
const adminToEditRole = ref(null)
const selectedRole = ref(1)

// --- 分頁控制 ---
const currentPage = ref(1)
const pageSize = 5

// --- 統計數據邏輯 --- 
const totalAdmins = computed(() => 
  admins.value.filter(a => a.admStatus === 1).length
)
const superAdmins = computed(() => 
  admins.value.filter(a => a.admRole === 9 && a.admStatus === 1).length
)
const normalAdmins = computed(() => 
  admins.value.filter(a => a.admRole === 1 && a.admStatus === 1).length
)

// --- 狀態切換與停權彈窗 ---
const filterStatus = ref(1) // 1: 啟用, 0: 停用
const showBanModal = ref(false)
const banReason = ref('')
const adminToBan = ref(null)

// 取得管理員
const fetchAdmins = async () => {
  loading.value = true
  try {
    const res = await axios.get('http://localhost:8080/admins')
    admins.value = res.data
    currentPage.value = 1
  } catch (error) {
    Swal.fire({
      icon: 'error',
      title: '載入失敗',
      text: '取得管理員資料失敗',
      confirmButtonColor: '#ff4d4f'
    })
  } finally {
    loading.value = false
  }
}

// 過濾與分頁 (狀態過濾)
const filteredAdmins = computed(() => {
  return admins.value.filter(a => a.admStatus === filterStatus.value)
})

const paginatedAdmins = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredAdmins.value.slice(start, start + pageSize)
})

const totalPages = computed(() => {
  return Math.ceil(filteredAdmins.value.length / pageSize) || 1
})

// 模糊搜尋
const searchAdmins = async () => {
  if (!keyword.value.trim()) {
    fetchAdmins()
    return
  }
  loading.value = true
  try {
    const res = await axios.get('http://localhost:8080/admins/search', {
      params: { keyword: keyword.value.trim() }
    })
    
    if (res.data.length === 0) {
      Swal.fire({
        icon: 'warning',
        title: '找不到資料',
        text: `找不到與「${keyword.value}」相關的管理員`,
        confirmButtonColor: '#f39c12'
      })
    } else {
      admins.value = res.data
      currentPage.value = 1
      Swal.fire({
        icon: 'success',
        title: '搜尋成功',
        text: `找到 ${res.data.length} 筆符合的管理員資料`,
        timer: 1500,
        showConfirmButton: false
      })
    }
  } catch (error) {
    Swal.fire({ 
      icon: 'error', 
      title: '搜尋發生錯誤' 
    })
  } finally {
    loading.value = false
  }
}


// --- 權限檢查邏輯 ---
const checkPermission = (actionCallback) => {
  const role = authStore.admin?.admRole || authStore.admin?.role;
  if (Number(role) === 9) {
    actionCallback();
  } else {
    Swal.fire({
      icon: 'lock',
      title: '權限不足',
      text: '您目前是一般管理員，無法執行此操作。',
      confirmButtonColor: '#ff4d4f'
    });
  }
};

// 停權操作
const openBanModal = (admin) => {
  adminToBan.value = admin
  banReason.value = ''
  showBanModal.value = true
}

const confirmBanAdmin = async () => {
  if (!banReason.value.trim()) {
    Swal.fire({ icon: 'warning', title: '請輸入原因', confirmButtonColor: '#f39c12' })
    return
  }
  try {
    // 假設後端 API 為 /admins/ban，並將狀態改為 0
    await axios.post('http://localhost:8080/admins/ban', {
      admId: adminToBan.value.admId,
      reason: banReason.value
    })
    showBanModal.value = false
    Swal.fire({ icon: 'success', title: '該管理員已停職', timer: 1500, showConfirmButton: false })
    fetchAdmins()
  } catch (error) {
    Swal.fire({ icon: 'error', title: '操作失敗' })
  }
}

// 恢復啟用
const activateAdmin = async (admin) => {
  const result = await Swal.fire({
    title: '確定要重新啟用嗎？',
    text: `將恢復管理員：${admin.admName} 的存取權限`,
    icon: 'question',
    showCancelButton: true,
    confirmButtonColor: '#52c41a',
    cancelButtonColor: '#8c8c8c',
    confirmButtonText: '確定啟用',
    cancelButtonText: '取消'
  })

  if (result.isConfirmed) {
    try {
      await axios.post('http://localhost:8080/admins/activate', { admId: admin.admId })
      Swal.fire({ icon: 'success', title: '已恢復啟用', timer: 1500, showConfirmButton: false })
      fetchAdmins()
    } catch (error) {
      Swal.fire({ icon: 'error', title: '恢復失敗' })
    }
  }
}

const formatDate = (dt) => {
  if (!dt) return ''
  return dt.substring(0, 10).replace(/-/g, '/')
}

const getAvatar = (id) => {
  if (id >= 1 && id <= 10) {
    const fileName = String(id).padStart(2, '0')
    return `/admin/${fileName}.jpg`
  }
  return '/admin/default.png'
}

// 開啟彈窗
const openRoleModal = (admin) => {
  adminToEditRole.value = admin
  selectedRole.value = admin.admRole
  showRoleModal.value = true
}

// 儲存變更並跳出 SweetAlert 確認
const handleUpdateRole = async () => {
  const result = await Swal.fire({
    title: '確定要變更權限嗎？',
    text: `將管理員「${adminToEditRole.value.admName}」的角色變更為：${selectedRole.value === 9 ? '超級管理員' : '一般管理員'}`,
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#1890ff',
    cancelButtonColor: '#8c8c8c',
    confirmButtonText: '確定更新',
    cancelButtonText: '取消'
  })

  if (result.isConfirmed) {
    try {
      await axios.post(`http://localhost:8080/admins/${adminToEditRole.value.admId}/role`, {
        admRole: selectedRole.value
      })
      showRoleModal.value = false
      Swal.fire({ icon: 'success', title: '權限已更新', timer: 1500, showConfirmButton: false })
      fetchAdmins() // 重新整理列表
    } catch (error) {
      Swal.fire({ icon: 'error', title: '更新失敗' })
    }
  }
}

onMounted(fetchAdmins)
</script>

<template>
  <div class="admin-main-container">
      <div class="header-info-card">
        <div class="header-icon">
          <i class="fas fa-user-shield"></i>
        </div>
        <div class="header-content">
          <h2>管理員列表</h2>
          <p>管理系統管理員帳號與權限</p>
        </div>
      </div>

      <div class="statistics-row">
        <div class="stat-item blue-left">
          <div class="stat-icon-box bg-blue-icon">
            <i class="fas fa-users"></i>
          </div>
          <div class="stat-data">
            <div class="stat-value">{{ totalAdmins }}</div>
            <div class="stat-label">管理員總數</div>
          </div>
        </div>
        <div class="stat-item red-left">
          <div class="stat-icon-box bg-red-icon">
            <i class="fas fa-crown"></i>
          </div>
          <div class="stat-data">
            <div class="stat-value">{{ superAdmins }}</div>
            <div class="stat-label">超級管理員</div>
          </div>
        </div>
        <div class="stat-item green-left">
          <div class="stat-icon-box bg-green-icon">
            <i class="fas fa-user-tie"></i>
          </div>
          <div class="stat-data">
            <div class="stat-value">{{ normalAdmins }}</div>
            <div class="stat-label">一般管理員</div>
          </div>
        </div>
      </div>

    <div class="search-filter-bar">
      <div class="search-input-group">
        <div class="icon-input-wrap">
          <i class="fas fa-search"></i>
          <input
            v-model="keyword"
            type="text"
            placeholder="搜尋帳號 / 姓名 / Email"
            @keyup.enter="searchAdmins"
          />
        </div>
        <button class="btn-action-search" @click="searchAdmins">
          <i class="fas fa-search"></i> 搜尋
        </button>
        <button class="btn-action-all" @click="fetchAdmins">
          <i class="fas fa-sync-alt"></i> 顯示全部
        </button>
      </div>

      <div class="action-right-group">
        <div class="status-toggle-group">
          <button 
            class="toggle-btn" 
            :class="{ active: filterStatus === 1 }" 
            @click="filterStatus = 1"
          >啟用員工</button>
          <button 
            class="toggle-btn" 
            :class="{ active: filterStatus === 0 }" 
            @click="filterStatus = 0"
          >停職員工</button>
        </div>
        <button class="btn-add-admin" @click="checkPermission(() => router.push('/admin/admins/create'))">
          <i class="fas fa-plus"></i> 新增管理員
        </button>
      </div>
    </div>

    <div class="table-wrapper-card">
      <table class="data-list-table">
        <thead>
          <tr>
            <th class="col-w-id">ID</th>
            <th class="col-w-info">管理員資訊</th>
            <th class="col-w-email">Email</th>
            <th class="col-w-role">權限</th>
            <th class="col-w-date">到職日</th>
            <th class="col-w-date">{{ filterStatus === 1 ? '更新時間' : '停職日' }}</th>
            <th class="col-w-btn">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in paginatedAdmins" :key="a.admId">
            <td>{{ a.admId }}</td>
            <td class="align-left">
              <div class="user-info-flex">
                <div class="avatar-box">
                  <img :src="getAvatar(a.admId)" />
                </div>
                <div class="name-account-stack">
                  <span class="full-name">{{ a.admName }}</span>
                  <span class="account-tag">@{{ a.admUsername }}</span>
                </div>
              </div>
            </td>
            <td class="align-left">{{ a.admEmail }}</td>
            <td>
              <span 
                class="role-badge" 
                :class="a.admRole === 9 ? 'role-super' : 'role-normal'"
                @click="checkPermission(() => openRoleModal(a))" 
                style="cursor: pointer;"
              >
                {{ a.admRole === 9 ? '超級管理員' : '一般管理員' }}
                <i class="fas fa-edit" style="font-size: 10px; margin-left: 4px;"></i>
              </span>
            </td>
            <td>{{ formatDate(a.createdAt) }}</td>
            <td>{{ formatDate(a.updatedAt) }}</td>
            <td>
              <div class="op-button-group">
                <template v-if="filterStatus === 1">
                  <button class="btn-op-edit" @click="checkPermission(() => router.push(`/admin/admins/edit/${a.admId}`))">修改</button>
                  <button class="btn-op-stop" @click="checkPermission(() => openBanModal(a))">停職</button>
                </template>
                <template v-else>
                  <button class="btn-box-active" @click="checkPermission(() => activateAdmin(a))">啟用</button>
                </template>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="page-control-footer" v-if="filteredAdmins.length > pageSize">
      <button class="page-nav-btn" :disabled="currentPage === 1" @click="currentPage--">上一頁</button>
      <span class="page-current-info">第 {{ currentPage }} / {{ totalPages }} 頁</span>
      <button class="page-nav-btn" :disabled="currentPage === totalPages" @click="currentPage++">下一頁</button>
    </div>

    <div v-if="showBanModal" class="modal-overlay" @click.self="showBanModal = false">
      <div class="ban-card">
        <div class="ban-header">
          <span>管理員停職處理</span>
          <button class="close-x" @click="showBanModal = false">&times;</button>
        </div>
        <div class="ban-body">
          <label>停職原因 <span class="required">*</span></label>
          <textarea v-model="banReason" placeholder="請詳細輸入停職原因..."></textarea>
          <div class="ban-warning">
            <i class="fas fa-exclamation-triangle"></i> 注意：停職後該帳號將無法登入後台系統。
          </div>
        </div>
        <div class="ban-footer">
          <button class="btn-close-modal" @click="showBanModal = false">取消</button>
          <button class="btn-confirm-ban" @click="confirmBanAdmin">確認停職</button>
        </div>
      </div>
    </div>
  </div>
  <div v-if="showRoleModal" class="modal-overlay" @click.self="showRoleModal = false">
    <div class="ban-card" style="width: 350px;"> <div class="ban-header" style="background: #1890ff;">
        <span>編輯 {{ adminToEditRole.admName }} 的職位</span>
        <button class="close-x" @click="showRoleModal = false">&times;</button>
      </div>
      <div class="ban-body">
        <p style="color: #909399; font-size: 14px; margin-bottom: 15px;">請勾選要指派給此員工的職位：</p>
        <div style="display: flex; flex-direction: column; gap: 12px;">
          <label style="display: flex; align-items: center; gap: 8px; cursor: pointer;">
            <input type="radio" v-model="selectedRole" :value="9"> 超級管理員 (ADMIN)
          </label>
          <label style="display: flex; align-items: center; gap: 8px; cursor: pointer;">
            <input type="radio" v-model="selectedRole" :value="1"> 一般管理員 (EMPLOYEE)
          </label>
        </div>
      </div>
      <div class="ban-footer">
        <button class="btn-close-modal" @click="showRoleModal = false">取消</button>
        <button class="btn-confirm-ban" style="background: #1890ff;" @click="handleUpdateRole">儲存變更</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-main-container {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.header-info-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background-color: #ffffff;
  padding: 20px 24px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  margin-bottom: 24px;
}

.header-icon {
  width: 56px;
  height: 56px;
  background-color: #e8f4ff;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #1890ff;
}

.header-content h2 {
  margin: 0;
  font-size: 22px;
  color: #303133;
}

.header-content p {
  margin: 4px 0 0 0;
  color: #909399;
  font-size: 14px;
}

/* 統計卡片容器：控制三欄等寬 */
.statistics-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

/* 單個統計項目 */
.stat-item {
  background-color: #ffffff;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border-left-width: 5px;
  border-left-style: solid;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.stat-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
}

/* 左側邊框顏色 */
.blue-left { 
  border-left-color: #1890ff; 
}

.red-left { 
  border-left-color: #ff4d4f; 
}

.green-left { 
  border-left-color: #52c41a; 
}

.stat-icon-box {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 20px;
}

.bg-blue-icon { 
  background-color: #1890ff; 
}

.bg-red-icon { 
  background-color: #ff4d4f; 
}

.bg-green-icon { 
  background-color: #52c41a; 
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #303133;
}

.search-filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #ffffff;
  padding: 16px 20px;
  border-radius: 12px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.search-input-group {
  display: flex;
  gap: 12px;
}

.icon-input-wrap {
  position: relative;
}

.icon-input-wrap i {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #c0c4cc;
}

.icon-input-wrap input {
  padding: 10px 12px 10px 36px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  width: 300px;
}

.action-right-group {
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-toggle-group {
  display: flex;
  border: 1px solid #1890ff;
  border-radius: 8px;
  overflow: hidden;
}

.toggle-btn {
  padding: 8px 16px;
  border: none;
  background: white;
  color: #1890ff;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.toggle-btn.active {
  background: #1890ff;
  color: white;
}

.btn-action-search,
.btn-action-all,
.btn-add-admin,
.btn-op-edit,
.btn-op-stop,
.btn-box-active,
.page-nav-btn {
  transition: all 0.2s ease;
  cursor: pointer;
  outline: none;
}

.btn-action-search:hover,
.btn-action-all:hover,
.btn-add-admin:hover,
.btn-op-edit:hover,
.btn-op-stop:hover,
.btn-box-active:hover,
.page-nav-btn:hover:not(:disabled), 
.role-badge:hover{
  filter: brightness(1.1);
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.btn-action-search:active,
.btn-add-admin:active,
.btn-op-stop:active {
  transform: scale(0.95);
}

.btn-add-admin {
  background-color: #52c41a;
  color: #ffffff;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
}

.btn-action-search {
  background-color: #1890ff;
  color: white;
  border: none;
  padding: 0 20px;
  border-radius: 8px;
}

.btn-action-all {
  background-color: #52c41a;
  color: white;
  border: none;
  padding: 0 20px;
  border-radius: 8px;
}

.table-wrapper-card {
  background-color: #ffffff;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.data-list-table {
  width: 100%;
  border-collapse: collapse;
}

.data-list-table th {
  background-color: #f8f9fb;
  padding: 16px;
  color: #606266;
  border-bottom: 1px solid #ebeef5;
}

.data-list-table td {
  padding: 16px;
  text-align: left;
  border-bottom: 1px solid #ebeef5;
}

.user-info-flex {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-box img {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
}

.name-account-stack {
  display: flex;
  flex-direction: column;
}

.full-name {
  font-weight: 600;
}

.account-tag {
  font-size: 12px;
  color: #909399;
}

.role-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: white;
  transition: all 0.2s ease;
  cursor: pointer;
}

.role-super {
  background: linear-gradient(135deg, #f56c6c 0%, #ff8e8e 100%);
}

.role-normal {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
}

.op-button-group {
  display: flex;
  gap: 8px;
}

.btn-op-edit {
  background-color: #1890ff;
  color: white;
  border: none;
  padding: 6px 14px;
  border-radius: 6px;
}

.btn-op-stop {
  background-color: #ff4d4f;
  color: white;
  border: none;
  padding: 6px 14px;
  border-radius: 6px;
}

.btn-box-active {
  background-color: #52c41a;
  color: white;
  border: none;
  padding: 6px 14px;
  border-radius: 6px;
}

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
  z-index: 2000;
}

.ban-card {
  background: white;
  width: 450px;
  border-radius: 12px;
  overflow: hidden;
}

.ban-header {
  background: #f39c12;
  padding: 15px 20px;
  display: flex;
  justify-content: space-between;
  color: white;
  font-weight: bold;
}

.ban-body {
  padding: 20px;
}

.ban-body textarea {
  width: 100%;
  height: 120px;
  margin-top: 10px;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  resize: none;
}

.ban-warning {
  margin-top: 15px;
  background: #fff9db;
  padding: 10px;
  border-radius: 4px;
  font-size: 13px;
  color: #856404;
}

.ban-footer {
  padding: 15px 20px;
  text-align: right;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn-confirm-ban {
  background: #f44336;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-confirm-ban:hover {
  background: #d32f2f;
  transform: translateY(-1px);
}

.btn-close-modal {
  padding: 8px 20px;
  background: #909399;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-close-modal:hover {
  background: #a6a9ad;
  transform: translateY(-1px);
}

.close-x {
  border: none;
  background: none;
  font-size: 24px;
  cursor: pointer;
  color: #333;
  transition: all 0.2s;
}

.close-x:hover {
  color: #f56c6c;
  transform: rotate(90deg);
}

.required {
  color: red;
}

.page-control-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
}

.page-nav-btn {
  padding: 8px 16px;
  background-color: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
}
</style>