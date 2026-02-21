<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import Swal from 'sweetalert2'
import { useAdminAuthStore } from '@/stores/adminAuth'

import { Bar, Line } from 'vue-chartjs'
import { 
  Chart as ChartJS, Title, Tooltip, Legend, BarElement, 
  CategoryScale, LinearScale, PointElement, LineElement 
} from 'chart.js'

ChartJS.register(Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale, PointElement, LineElement)

const router = useRouter()
const members = ref([])
const keyword = ref('')
const loading = ref(false)
const authStore = useAdminAuthStore()

// --- 分頁控制 ---
const currentPage = ref(1)
const pageSize = 6 

// --- 彈窗控制 ---
const showModal = ref(false)
const selectedMember = ref(null)

// --- 狀態切換與停權彈窗 ---
const filterStatus = ref(1) // 1: 啟用, 0: 停用
const showBanModal = ref(false)
const banReason = ref('')
const memberToBan = ref(null)

// 取得會員
const fetchMembers = async () => {
  loading.value = true
  try {
    const res = await axios.get('http://localhost:8080/api/members')
    members.value = res.data
    currentPage.value = 1 
  } catch (error) {
    Swal.fire({
      icon: 'error',
      title: '載入失敗',
      text: '取得會員資料失敗',
      confirmButtonColor: '#f56c6c'
    })
  } finally {
    loading.value = false
  }
}

// 過濾與分頁
const filteredMembers = computed(() => {
  return members.value.filter(m => m.memStatus === filterStatus.value)
})

const paginatedMembers = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredMembers.value.slice(start, start + pageSize)
})

const totalPages = computed(() => {
  return Math.ceil(filteredMembers.value.length / pageSize) || 1
})

const openMemberDetail = (member) => {
  selectedMember.value = member
  showModal.value = true
}

// 模糊搜尋 (完全保留原邏輯與樣式)
const searchMembers = async () => {
  // 如果關鍵字為空，顯示全部資料
  if (!keyword.value.trim()) {
    fetchMembers()
    return
  }

  loading.value = true
  try {
    const res = await axios.get('http://localhost:8080/api/members/search', {
      params: { keyword: keyword.value.trim() }
    })

    members.value = res.data
    currentPage.value = 1 // 搜尋後回到第一頁

    // --- 完整還原：顯示搜尋結果提示 ---
    if (res.data.length === 0) {
      Swal.fire({
        icon: 'warning',
        title: '查無結果',
        text: `找不到包含「${keyword.value.trim()}」的會員資料`,
        confirmButtonText: '確定',
        confirmButtonColor: '#e6a23c'
      })
    } else {
      Swal.fire({
        icon: 'success',
        title: '搜尋成功',
        text: `找到 ${res.data.length} 筆符合的會員資料`,
        timer: 1500,
        showConfirmButton: false
      })
    }
  } catch (error) {
    // --- 完整還原：搜尋失敗提示 ---
    Swal.fire({
      icon: 'error',
      title: '搜尋失敗',
      text: error.response?.data?.message || '搜尋過程發生錯誤，請稍後再試',
      confirmButtonText: '確定',
      confirmButtonColor: '#f56c6c'
    })
  } finally {
    loading.value = false
  }
}

// 停權邏輯
const openBanModal = (member) => {
  memberToBan.value = member
  banReason.value = ''
  showBanModal.value = true
}

const confirmBanMember = async () => {
  if (!banReason.value.trim()) {
    Swal.fire({ icon: 'warning', title: '請輸入原因', confirmButtonColor: '#e6a23c' })
    return
  }
  try {
    await axios.post('http://localhost:8080/api/members/ban', {
      memId: memberToBan.value.memId,
      reason: banReason.value
    })
    showBanModal.value = false
    Swal.fire({ icon: 'success', title: '該會員已移至停權區', timer: 1500, showConfirmButton: false })
    fetchMembers()
  } catch (error) {
    Swal.fire({ icon: 'error', title: '操作失敗' })
  }
}

// 恢復啟用 (加回 SweetAlert 確認)
const activateMember = async (member) => {
  const result = await Swal.fire({
    title: '確定要重新啟用嗎？',
    text: `即將恢復會員：${member.memName} 的權限`,
    icon: 'question',
    showCancelButton: true,
    confirmButtonColor: '#67c23a',
    cancelButtonColor: '#909399',
    confirmButtonText: '確定啟用',
    cancelButtonText: '取消'
  })

  if (result.isConfirmed) {
    try {
      await axios.post('http://localhost:8080/api/members/activate', { memId: member.memId })
      Swal.fire({ icon: 'success', title: '已重新啟用', timer: 1500, showConfirmButton: false })
      fetchMembers()
    } catch (error) {
      Swal.fire({ icon: 'error', title: '恢復失敗' })
    }
  }
}

const formatDateOnly = (dt) => {
  if (!dt) return ''
  return dt.substring(0, 10).replace(/-/g, '/')
}

const clearSearch = () => {
  keyword.value = ''
  fetchMembers()
}

const checkPermission = (actionCallback) => {
  const role = authStore.admin?.role; 
  if (authStore.isLogin && Number(role) === 9) {
    actionCallback();
  } else {
    Swal.fire({
      icon: 'lock',
      title: '權限不足',
      text: '您目前的身分是一般管理員，無法執行此操作。',
      confirmButtonColor: '#f56c6c'
    });
  }
};

// --- 統計邏輯：點數級距 ---
const pointStats = computed(() => {
  const dist = { '0-100': 0, '101-500': 0, '501-1000': 0, '1000+': 0 }
  members.value.forEach(m => {
    if (m.memPoints <= 100) dist['0-100']++
    else if (m.memPoints <= 500) dist['101-500']++
    else if (m.memPoints <= 1000) dist['501-1000']++
    else dist['1000+']++
  })
  return dist
})

// --- 統計邏輯：註冊趨勢 ---
const registrationStats = computed(() => {
  const counts = {}
  members.value.forEach(m => {
    if (m.createdAt) {
      const date = m.createdAt.substring(0, 10)
      counts[date] = (counts[date] || 0) + 1
    }
  })
  const sortedDates = Object.keys(counts).sort()
  return {
    labels: sortedDates,
    data: sortedDates.map(d => counts[d])
  }
})

// --- 圖表數據配置 ---
const lineChartData = computed(() => ({
  labels: registrationStats.value.labels,
  datasets: [{
    label: '每日新註冊人數',
    data: registrationStats.value.data,
    borderColor: '#409eff',
    backgroundColor: 'rgba(64, 158, 255, 0.1)',
    tension: 0.4,
    fill: true
  }]
}))

const barChartData = computed(() => ({
  labels: Object.keys(pointStats.value),
  datasets: [{
    label: '會員人數',
    data: Object.values(pointStats.value),
    backgroundColor: ['#95d475', '#409eff', '#f89898', '#eebe77']
  }]
}))

// --- CSV 匯出邏輯 ---
const exportToCSV = () => {
  if (members.value.length === 0) {
    Swal.fire('提示', '目前無資料可導出', 'info');
    return;
  }

  // 定義標題列
  const headers = ['會員ID', '帳號', '姓名', 'Email', '手機', '點數', '註冊日期'];

  // 處理資料內容
  const rows = members.value.map(m => [
    `\t${m.memId}`,
    `\t${m.memUsername}`,
    `\t${m.memName}`,
    m.memEmail,
    `\t${m.memPhone}`, // 加 \t 解決 9.1E+08 (科學記號)
    m.memPoints,
    `\t${m.createdAt?.substring(0, 10)}` // 加 \t 解決 #### 並格式化日期
  ]);

  // 轉成 CSV 字串，並用逗號分隔
  const csvContent = [
    headers.join(','),
    ...rows.map(row => row.join(','))
  ].join('\n');

  // 加上 BOM (\uFEFF) 解決 Excel 開啟中文亂碼問題
  const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `會員名單_${new Date().toLocaleDateString()}.csv`;
  link.click();
  URL.revokeObjectURL(url);
};

onMounted(fetchMembers)
</script>

<template>
  <div class="member-page">
    <h2 class="title"><i class="fas fa-users"></i> 會員列表</h2>

    <div class="dashboard-grid">
      <div class="chart-card">
        <div class="chart-header">
          <span class="chart-title"><i class="fas fa-chart-line"></i> 註冊成長趨勢</span>
          <button class="btn-csv" @click="exportToCSV">
            <i class="fas fa-file-download"></i> 匯出報表 (CSV)
          </button>
        </div>
        <div class="chart-body">
          <Line :data="lineChartData" :options="chartOptions" />
        </div>
      </div>

      <div class="chart-card">
        <div class="chart-header">
          <span class="chart-title"><i class="fas fa-chart-pie"></i> 會員點數級距</span>
        </div>
        <div class="chart-body">
          <Bar :data="barChartData" :options="chartOptions" />
        </div>
      </div>
    </div>

    <div class="toolbar">
      <div class="search-bar">
        <div class="search-input-wrapper">
          <i class="fas fa-search search-icon"></i>
          <input
            v-model="keyword"
            type="text"
            placeholder="搜尋：帳號 / 姓名 / Email / 手機"
            @keyup.enter="searchMembers"
            :disabled="loading"
          />
          <button v-if="keyword" class="clear-btn" @click="clearSearch">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <button class="btn-search" @click="searchMembers" :disabled="loading">
          <i class="fas fa-search"></i> 搜尋
        </button>
        <button class="btn-refresh" @click="fetchMembers" :disabled="loading">
          <i class="fas fa-sync-alt"></i> 顯示全部
        </button>
      </div>
      <div class="create-bar">
        <div class="status-toggle-group">
          <button 
            class="toggle-btn" 
            :class="{ active: filterStatus === 1 }" 
            @click="filterStatus = 1"
          >啟用會員</button>
          <button 
            class="toggle-btn" 
            :class="{ active: filterStatus === 0 }" 
            @click="filterStatus = 0"
          >停用會員</button>
        </div>
        <button class="btn-create" @click="checkPermission(() => router.push('/admin/members/create'))">
          <i class="fas fa-user-plus"></i> 新增會員
        </button>
      </div>
    </div>

    <div class="table-container">
      <table>
        <thead>
          <tr>
            <th class="col-id">ID</th>
            <th class="col-info">會員資訊</th>
            <th class="col-points">點數</th>
            <th class="col-date">{{ filterStatus === 1 ? '註冊日期' : '停用日期' }}</th>
            <th class="col-status">狀態</th>
            <th class="col-action">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr 
            v-for="m in paginatedMembers" 
            :key="m.memId" 
            :class="{ 'row-active': selectedMember?.memId === m.memId && showModal }"
          >
            <td class="col-id id-link-cell" @click="openMemberDetail(m)">{{ m.memId }}</td>
            <td class="col-info">
              <div class="info-cell">
                <div class="list-avatar-wrap">
                  <img :src="m.memImage ? '/members/' + m.memImage : '/members/default.png'" />
                </div>
                <div class="user-text">
                  <span class="user-name">{{ m.memName }}</span>
                  <span class="user-phone">{{ m.memPhone }}</span>
                </div>
              </div>
            </td>
            <td class="col-points"><span class="points-val">{{ m.memPoints }}</span></td>
            <td class="col-date">
              {{ filterStatus === 1 ? formatDateOnly(m.createdAt) : formatDateOnly(m.updatedAt) }}
            </td>
            <td class="col-status">
              <span class="status-badge" :class="m.memStatus === 1 ? 'status-active' : 'status-inactive'">
                {{ m.memStatus === 1 ? '啟用' : '停用' }}
              </span>
            </td>
            <td class="col-action">
              <div class="action-btns">
                <button 
                  v-if="filterStatus === 1" 
                  class="btn-box-edit" 
                  @click="checkPermission(() => router.push(`/admin/members/edit/${m.memId}`))"
                >修改</button>
                
                <button 
                  v-if="m.memStatus === 1"
                  class="btn-box-del" 
                  @click="checkPermission(() => openBanModal(m))"
                >停權</button>
                
                <button 
                  v-else
                  class="btn-box-active" 
                  @click="checkPermission(() => activateMember(m))"
                >啟用</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pagination-bar" v-if="filteredMembers.length > pageSize">
      <button class="btn-page" :disabled="currentPage === 1" @click="currentPage--">上一頁</button>
      <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 頁</span>
      <button class="btn-page" :disabled="currentPage === totalPages" @click="currentPage++">下一頁</button>
    </div>

    <div v-if="showBanModal" class="modal-overlay" @click.self="showBanModal = false">
      <div class="ban-card">
        <div class="ban-header">
          <span>停權會員</span>
          <button class="close-x" @click="showBanModal = false">&times;</button>
        </div>
        <div class="ban-body">
          <label>停權原因 <span class="required">*</span></label>
          <textarea v-model="banReason" placeholder="請輸入停權原因..."></textarea>
          <div class="ban-warning">
            <i class="fas fa-exclamation-triangle"></i>
            注意：停權後該會員將無法使用服務，直到重新啟用為止。
          </div>
        </div>
        <div class="ban-footer">
          <button class="btn-close-modal" @click="showBanModal = false">取消</button>
          <button class="btn-confirm-ban" @click="confirmBanMember">確認停權</button>
        </div>
      </div>
    </div>

    <div v-if="showModal && selectedMember" class="modal-overlay" @click.self="showModal = false">
      <div class="compact-card">
        <div class="card-header">
          <h3><i class="fas fa-id-card"></i> 會員詳細資訊</h3>
          <button class="close-x" @click="showModal = false">&times;</button>
        </div>
        <div class="card-body">
          <div class="compact-sidebar">
            <div class="mini-avatar">
              <img :src="selectedMember.memImage ? `/members/${selectedMember.memImage}` : '/members/default.png'" />
            </div>
            <h4 class="sidebar-name">{{ selectedMember.memName }}</h4>
            <span class="sidebar-user">@{{ selectedMember.memUsername }}</span>
          </div>
          <div class="compact-main">
            <div class="data-section">
              <h5 class="section-title">個人資訊</h5>
              <div class="mini-grid">
                <div class="grid-item">
                  <label>會員 ID</label>
                  <span>{{ selectedMember.memId }}</span>
                </div>
                <div class="grid-item">
                  <label>聯絡電話</label>
                  <span>{{ selectedMember.memPhone || '未提供' }}</span>
                </div>
                <div class="grid-item">
                  <label>電子信箱</label>
                  <span>{{ selectedMember.memEmail }}</span>
                </div>
                <div class="grid-item">
                  <label>發票載具</label>
                  <span class="text-blue">{{ selectedMember.memInvoice || '未提供' }}</span>
                </div>
                <div class="grid-item">
                  <label>目前點數</label>
                  <span class="text-green">{{ selectedMember.memPoints }}</span>
                </div>
              </div>
            </div>
            <div class="data-section">
              <h5 class="section-title">系統資訊</h5>
              <div class="mini-grid">
                <div class="grid-item">
                  <label>註冊日期</label>
                  <span>{{ formatDateOnly(selectedMember.createdAt) }}</span>
                </div>
                <div class="grid-item">
                  <label>帳號狀態</label>
                  <span :class="selectedMember.memStatus === 1 ? 'status-text-active' : 'status-text-inactive'">
                    {{ selectedMember.memStatus === 1 ? '啟用' : '停用' }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="card-footer">
          <button class="btn-close-modal" @click="showModal = false">關閉視窗</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.member-page {
  min-height: 100vh;
  background-color: #f0f5fa;
  padding: 20px;
}

.title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #303133;
  margin-bottom: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.search-bar {
  display: flex;
  gap: 10px;
}

.search-input-wrapper {
  position: relative;
  width: 350px;
}

.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #c0c4cc;
}

.search-input-wrapper input {
  width: 100%;
  padding: 10px 40px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #f5f7fa;
  transition: border-color 0.3s;
}

.search-input-wrapper input:focus {
  border-color: #409eff;
  outline: none;
}

.clear-btn {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: #909399;
  cursor: pointer;
}

.btn-search {
  padding: 10px 18px;
  background: #409eff;
  color: white;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-search:hover {
  background: #66b1ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(64, 158, 255, 0.3);
}

.btn-refresh {
  padding: 10px 18px;
  background: #67c23a;
  color: white;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-refresh:hover {
  background: #85ce61;
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(103, 194, 58, 0.3);
}

.create-bar {
  display: flex;
  align-items: center;
  gap: 15px;
}

.status-toggle-group {
  display: flex;
  border: 1px solid #409eff;
  border-radius: 6px;
  overflow: hidden;
}

.toggle-btn {
  padding: 8px 16px;
  border: none;
  background: white;
  color: #409eff;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.toggle-btn.active {
  background: #409eff;
  color: white;
}

.btn-create {
  padding: 10px 20px;
  background: #67c23a;
  color: white;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-create:hover {
  background: #85ce61;
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(103, 194, 58, 0.3);
}

.table-container {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

th, td {
  padding: 16px 12px;
  text-align: center;
  border-bottom: 1px solid #ebeef5;
}

th {
  background: #f5f7fa;
  color: #606266;
  font-weight: 600;
}

.col-id {
  width: 70px;
}

.col-info {
  width: 280px;
  text-align: left !important;
  padding-left: 20px;
}

.col-points {
  width: 100px;
}

.col-date {
  width: 150px;
}

.col-status {
  width: 100px;
}

.col-action {
  width: 160px;
}

.info-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.list-avatar-wrap img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #eee;
}

.user-text {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-weight: 600;
  color: #303133;
}

.user-phone {
  font-size: 12px;
  color: #909399;
}

.id-link-cell {
  color: #409eff;
  font-weight: 600;
  cursor: pointer;
  text-decoration: underline;
}

.points-val {
  color: #f59e0b;
  font-weight: bold;
}

.status-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
}

.status-active {
  background: #f0f9ff;
  color: #409eff;
  border: 1px solid #b3d8ff;
}

.status-inactive {
  background: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fbc4c4;
}

.action-btns {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.btn-box-edit {
  background-color: #409eff;
  color: white;
  padding: 6px 12px;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-box-edit:hover {
  background-color: #66b1ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(64, 158, 255, 0.3);
}

.btn-box-del {
  background-color: #f56c6c;
  color: white;
  padding: 6px 12px;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-box-del:hover {
  background-color: #f78989;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(245, 108, 108, 0.3);
}

.btn-box-active {
  background-color: #67c23a;
  color: white;
  padding: 6px 12px;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-box-active:hover {
  background-color: #85ce61;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(103, 194, 58, 0.3);
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 20px;
  margin-top: 25px;
}

.btn-page {
  padding: 8px 16px;
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-page:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(64, 158, 255, 0.2);
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
  box-shadow: 0 10px 25px rgba(0,0,0,0.2);
}

.ban-header {
  background: #ffc107;
  padding: 15px 20px;
  display: flex;
  justify-content: space-between;
  font-weight: bold;
  align-items: center;
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

.compact-card {
  background: white;
  width: 600px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 10px 25px rgba(0,0,0,0.2);
}

.card-header {
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-body {
  display: flex;
}

.compact-sidebar {
  width: 180px;
  background: #f9f9f9;
  padding: 25px 15px;
  display: flex;
  flex-direction: column;
  align-items: center;
  border-right: 1px solid #eee;
}

.mini-avatar img {
  width: 70px;
  height: 70px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #409eff;
}

.sidebar-name {
  margin-top: 10px;
  font-size: 1.1rem;
}

.sidebar-user {
  font-size: 12px;
  color: #999;
}

.compact-main {
  flex: 1;
  padding: 20px;
}

.data-section {
  margin-bottom: 20px;
}

.section-title {
  border-left: 3px solid #409eff;
  padding-left: 8px;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
}

.mini-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}

.grid-item {
  display: flex;
  flex-direction: column;
}

.grid-item label {
  font-size: 11px;
  color: #bbb;
  margin-bottom: 4px;
}

.text-green {
  color: #67c23a;
  font-weight: bold;
}

.text-blue {
  color: #409eff;
  font-weight: bold;
}

.status-text-active {
  color: #409eff;
}

.status-text-inactive {
  color: #f56c6c;
}

.card-footer {
  padding: 15px 20px;
  text-align: right;
  border-top: 1px solid #eee;
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
  position: absolute;
  top: 15px;         /* 距離頂部高度 */
  right: 20px;
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

.row-active {
  background-color: #ecf5ff !important; /* 淡淡的藍色選中感 */
  transition: background-color 0.3s;
}

.row-active .id-link-cell {
  color: #66b1ff;
  text-shadow: 0 0 2px rgba(64, 158, 255, 0.2);
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr; /* 左寬右窄 */
  gap: 20px;
  margin-bottom: 20px;
}

.chart-card {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.chart-title {
  font-weight: 600;
  color: #606266;
  font-size: 14px;
}

.chart-body {
  height: 230px; /* 固定高度，避免表格被推太遠 */
}

.btn-csv {
  padding: 6px 12px;
  background-color: #f0f9ff;
  color: #409eff;
  border: 1px solid #d9ecff;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-csv:hover {
  background-color: #409eff;
  color: white;
}

/* 讓圖表變垂直排列 */
@media (max-width: 1024px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>