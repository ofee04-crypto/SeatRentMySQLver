<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import axios from 'axios'
import Swal from 'sweetalert2'

const API_URL = 'http://localhost:8080/rec-rent'

const rentList = ref([])
const searchCriteria = reactive({
  recSeqId: '',

  recId: '',
  memId: '',
  memName: '',
  recStatus: '',
  spotId: '',
  spotName: '',
  startDate: '',
  endDate: '',
  recPayment: '',
  recNote: '',
  serialNumber: '',
})

// 分頁相關狀態
const currentPage = ref(1)
const pageSize = ref(25)

// 計算總頁數
const totalPages = computed(() => {
  return Math.ceil(rentList.value.length / pageSize.value) || 1
})

// 計算當前頁面顯示的資料
const paginatedList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return rentList.value.slice(start, end)
})

// 監聽列表變更，重置回第一頁
watch(rentList, () => {
  currentPage.value = 1
})

// 監聽每頁筆數變更，重置回第一頁
watch(pageSize, () => {
  currentPage.value = 1
})

const emit = defineEmits(['edit-rent', 'delete-rent'])

const loadRents = async () => {
  try {
    const params = new URLSearchParams()
    if (searchCriteria.recSeqId) params.append('recSeqId', searchCriteria.recSeqId)
    if (searchCriteria.recId) params.append('recId', searchCriteria.recId)
    if (searchCriteria.memId) params.append('memId', searchCriteria.memId)
    if (searchCriteria.memName) params.append('memName', searchCriteria.memName)
    if (searchCriteria.recStatus) params.append('recStatus', searchCriteria.recStatus)
    if (searchCriteria.spotId) params.append('spotId', searchCriteria.spotId)
    if (searchCriteria.spotName) params.append('spotName', searchCriteria.spotName)
    if (searchCriteria.startDate) params.append('startDate', searchCriteria.startDate)
    if (searchCriteria.endDate) params.append('endDate', searchCriteria.endDate)
    if (searchCriteria.recVio) params.append('recPayment', searchCriteria.recPayment)
    if (searchCriteria.recPayment) params.append('recPayment', searchCriteria.recPayment)
    if (searchCriteria.recNote) params.append('recNote', searchCriteria.recNote)
    if (searchCriteria.serialNumber) params.append('serialNumber', searchCriteria.serialNumber)

    const queryString = params.toString()
    const requestUrl = queryString ? `${API_URL}?${queryString}` : API_URL

    const res = await axios.get(requestUrl)
    // 將搜尋結果依照 recId 由大到小排序 (最新到最舊)
    rentList.value = res.data
  } catch (err) {
    console.error('載入失敗:', err)
    alert('無法載入資料，請確認後端伺服器是否已啟動.\n錯誤: ' + err.message)
  }
}

const clearSearch = () => {
  for (const key in searchCriteria) {
    searchCriteria[key] = ''
  }
  loadRents()
}

const editRent = (rent) => {
  emit('edit-rent', rent)
}

const deleteRent = (id) => {
  emit('delete-rent', id)
}

// 切換頁面
const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
  }
}

onMounted(loadRents)

// 匯出 CSV 格式
const exportToCsv = () => {
  if (rentList.value.length === 0) {
    alert('沒有可匯出的資料。')
    return
  }

  // 定義 CSV 檔案的表頭
  const headers = [
    '訂單狀態',    '訂單編號',    '會員編號',    '會員姓名',
    '座椅編號',    '租借點編號',    '租借點名稱',    '歸還點編號',
    '歸還點名稱',    '租借時間',    '歸還時間',    '費用',    '備註',
  ]
  // 將每一筆訂單資料轉換為 CSV 的一列
  const rows = rentList.value.map((rent) =>
    [
      `"${rent.recStatus || ''}"`,
      `"${rent.recId || ''}"`,
      `"${rent.memId || ''}"`,
      `"${rent.memName || ''}"`,
      `"${rent.seatsId || ''}"`,
      `"${rent.spotIdRent || ''}"`,
      `"${rent.rentSpotName || ''}"`,
      `"${rent.spotIdReturn || ''}"`,
      `"${rent.returnSpotName || ''}"`,
      `"${rent.recRentDT2 ? rent.recRentDT2.replace('T', ' ') : ''}"`,
      `"${rent.recReturnDT2 ? rent.recReturnDT2.replace('T', ' ') : ''}"`,
      `"${rent.recPayment || ''}"`,
      `"${rent.recNote || ''}"`,
    ].join(','),
  )

  // 組合表頭和所有列，並添加 BOM 以確保 Excel 能正確讀取 UTF-8
  const csvContent = '\uFEFF' + [headers.join(','), ...rows].join('\n')
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.setAttribute('download', 'rent_data.csv')
  document.body.appendChild(link)
  // 模擬點擊以下載檔案
  link.click()
  // 釋放記憶體
  document.body.removeChild(link)
  URL.revokeObjectURL(link.href)
}

// 匯出 JSON 格式
const exportToJson = () => {
  if (rentList.value.length === 0) {
    alert('沒有可匯出的資料。')
    return
  }

  // 將資料轉換為格式化的 JSON 字串
  const jsonContent = JSON.stringify(rentList.value, null, 2)
  const blob = new Blob([jsonContent], { type: 'application/json;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = 'rent_data.json'
  // 模擬點擊以下載檔案
  link.click()
  // 釋放記憶體
  URL.revokeObjectURL(link.href)
}

// Expose the loadRents method to the parent component
defineExpose({
  loadRents,
})

// [新增] 顯示完整備註的彈窗函式
const showFullNote = (note) => {
  Swal.fire({
    title: '備註-詳細內容',
    text: note,
    confirmButtonText: '點擊任意處或點此關閉',
    confirmButtonColor: '#409eff',
    allowOutsideClick: true, // 允許點擊遮罩層(外部)關閉
  })
}
</script>

<template>
  <div class="rec-rent-search-wrapper">
    <!-- 搜尋表單 -->
    <div class="search-form-container">
      <div class="search-form">
        <div class="form-group-search">
          <label>訂單編號:</label>
          <input
            v-model="searchCriteria.recId"
            type="text"
            placeholder="依訂單編號"
            @keyup.enter="loadRents"
          />
        </div>
        <div class="form-group-search">
          <label>會員編號:</label>
          <input
            v-model="searchCriteria.memId"
            type="text"
            placeholder="依會員編號"
            @keyup.enter="loadRents"
          />
        </div>
        <div class="form-group-search">
          <label>會員姓名:</label>
          <input
            v-model="searchCriteria.memName"
            type="text"
            placeholder="依會員姓名(模糊)"
            @keyup.enter="loadRents"
          />
        </div>
        <div class="form-group-search">
          <label>訂單狀態:</label>
          <select v-model="searchCriteria.recStatus" @keyup.enter="loadRents">
            <option value="">所有狀態</option>
            <option value="租借中">租借中</option>
            <option value="已完成">已完成</option>
            <option value="未歸還">未歸還</option>
            <option value="已取消">已取消</option>
          </select>
        </div>
        <div class="form-group-search">
          <label>站點編號:</label>
          <input
            v-model="searchCriteria.spotId"
            type="text"
            placeholder="依站點編號"
            @keyup.enter="loadRents"
          />
        </div>
        <div class="form-group-search">
          <label>站點名稱:</label>
          <input
            v-model="searchCriteria.spotName"
            type="text"
            placeholder="依站點名稱(模糊)"
            @keyup.enter="loadRents"
          />
        </div>
        <div class="form-group-search">
        <label>開始日期:</label>
        <input v-model="searchCriteria.startDate" type="date" />
      </div>
      <div class="form-group-search">
        <label>結束日期:</label>
        <input v-model="searchCriteria.endDate" type="date" />
      </div>
      </div>
    </div>
    <div class="view-section active">
      <div class="search-actions">
        <button class="btn-primary" @click="loadRents" style="min-width: 135px; margin: 0 0 0 10px">
          搜尋
        </button>
        <button class="btn-secondary" @click="clearSearch">清除</button>
        <button class="btn-success" @click="loadRents">更新</button>
        <select v-model="pageSize" class="page-size-select">
          <option :value="25">每頁顯示筆數</option>
          <option :value="25">25 筆/頁</option>
          <option :value="50">50 筆/頁</option>
          <option :value="100">100 筆/頁</option>
          <option :value="200">200 筆/頁</option>
        </select>
        <!-- 匯出按鈕 -->
        <button class="btn-info" @click="exportToCsv" style="margin-left: auto">匯出 CSV</button>
        <button class="btn-info" @click="exportToJson">匯出 JSON</button>
      </div>
      <table>
        <thead>
          <tr>
            <th>訂單狀態</th>
            <th>訂單編號</th>
            <th>會員編號</th>
            <th>會員姓名</th>
            <th>座椅編號</th>
            <th>租借點名稱</th>
            <th>-編號</th>
            <th>租借時間</th>
            <th>歸還點名稱</th>
            <th>-編號</th>
            <th>歸還時間</th>
            <th>費用</th>
            <th>備註</th>
            <th width="150">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(rent, index) in paginatedList" :key="rent.recId || index">
            <td>{{ rent.recStatus }}</td>
            <td>
              <span v-if="rent.recId">{{ rent.recId }}</span>
              <span v-else style="color: gray">處理中...</span>
            </td>
            <td>{{ rent.memId }}</td>
            <td>{{ rent.memName }}</td>
            <td>SN-20260{{ rent.seatsId ? String(rent.seatsId).padStart(4, '0') : '' }}</td>
            <td>{{ rent.rentSpotName }}</td>
            <td>{{ rent.spotIdRent }}</td>
            <td>{{ rent.recRentDT2 ? rent.recRentDT2.replace('T', ' ') : '' }}</td>
            <td>{{ rent.returnSpotName }}</td>
            <td>{{ rent.spotIdReturn }}</td>
            <td>{{ rent.recReturnDT2 ? rent.recReturnDT2.replace('T', ' ') : '' }}</td>
            <td>{{ rent.recPayment }}</td>
            <td>
              <!-- [修改] 判斷備註長度，若超過 5 字元則截斷並顯示可點擊的 ... -->
              <span v-if="rent.recNote && rent.recNote.length > 5">
                {{ rent.recNote.substring(0, 5)
                }}<span class="note-more" @click="showFullNote(rent.recNote)" title="點擊查看完整內容"
                  >...</span
                >
              </span>
              <span v-else>{{ rent.recNote }}</span>
            </td>
            <td>
              <button class="btn-warning" @click="editRent(rent)">編輯</button><span>/</span>
              <button class="btn-danger ml-1" @click="deleteRent(rent.recId)">刪除</button>
            </td>
          </tr>
          <tr v-if="rentList.length === 0">
            <td colspan="12" class="text-center">暫無資料或查無結果</td>
          </tr>
        </tbody>
      </table>

      <!-- 分頁資訊列 (保留在底部) -->
      <div class="pagination-info-bar" v-if="rentList.length > 0">
        <span>第</span>
        <select v-model="currentPage" class="page-select">
          <option v-for="p in totalPages" :key="p" :value="p">{{ p }}</option>
        </select>
        <span>頁 / 共 {{ totalPages }} 頁 (共 {{ rentList.length }} 筆)</span>
      </div>

      <!-- 懸浮導航按鈕 (固定在視窗兩側) -->
      <Teleport to="body">
        <div class="floating-nav left" v-if="rentList.length > 0">
          <button class="btn-float" :disabled="currentPage === 1" @click="goToPage(1)">首頁</button>
          <button class="btn-float" :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">
            上頁
          </button>
        </div>

        <div class="floating-nav right" v-if="rentList.length > 0">
          <button
            class="btn-float"
            :disabled="currentPage === totalPages"
            @click="goToPage(currentPage + 1)"
          >
            下頁
          </button>
          <button
            class="btn-float"
            :disabled="currentPage === totalPages"
            @click="goToPage(totalPages)"
          >
            末頁
          </button>
        </div>
      </Teleport>
    </div>
  </div>
</template>

<style scoped>
/* ========== 搜尋表單區 - 淺色風格 ========== */
.search-form-container {
  background: #f5f7fa;
  padding: 8px 10px;
  border-radius: 10px;
  margin-bottom: 5px;
}

.search-form {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 8px;
  align-items: end;
}

.form-group-search {
  display: flex;
  flex-direction: column;
}

.form-group-search label {
  font-weight: 500;
  margin-bottom: 3px;
  font-size: 14px;
  color: #606266;
}

.form-group-search input,
.form-group-search select {
  padding: 3px 10px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  font-size: 14px;
  background: #fff;
  transition: all 0.3s ease;
}

.form-group-search input:focus,
.form-group-search select:focus {
  border-color: #c0c4cc;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.08);
  outline: none;
}

.form-group-search input::placeholder {
  color: #c0c4cc;
}

.search-actions {
  display: flex;
  padding: 0px;
  justify-content: flex-start;
  gap: 8px;
  margin-bottom: 10px;
}

/* [新增] 設定操作區按鈕的最小寬度，讓按鈕整齊一致 */
.search-actions button {
  min-width: 65px;
}

.page-size-select {
  padding: 5px 8px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
}

/* ========== 表格 - 淺色風格 ========== */
table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  margin-top: 8px;
  background: rgb(255, 255, 255);
  font-size: 15px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border-right: 1px solid #000000;
}

th,
td {
  border-bottom: 1px solid #ffffff;
  border-left: 1px solid #000000;
  padding: 4px 2px;
  text-align: center;
  white-space: nowrap;
}

th {
  background: #f5f7fa;
  color: #606266;
  font-weight: 600;
  font-size: 14px;
}

tbody tr {
  transition: background-color 0.2s ease;
}

tbody tr:hover {
  background-color: #f1f1f1;
}

tbody tr:nth-child(even) {
  background-color: #e4e4e4;
}

tbody tr:nth-child(even):hover {
  background-color: #cccccc;
}

.text-center {
  text-align: center;
  color: #909399;
  padding: 40px !important;
}

.view-section.active {
  color: #000000;
  padding: 0;
  display: block;
}

/* ========== 按鈕樣式 ========== */
button {
  padding: 5px 8px;
  cursor: pointer;
  border: none;
  border-radius: 8px;
  font-weight: 500;
  font-size: 17px;
  transition: all 0.3s ease;
}

button:hover {
  transform: translateY(-1px);
}

.btn-primary {
  background: #409eff;
  color: white;
}

.btn-primary:hover {
  background: #66b1ff;
}

.btn-secondary {
  background: #909399;
  color: white;
}

.btn-secondary:hover {
  background: #a6a9ad;
}

/* [新增] 補上 btn-success (綠色按鈕) 的樣式 */
.btn-success {
  background: #67c23a;
  color: white;
}

.btn-success:hover {
  background: #85ce61;
}

.btn-info {
  background: #17a2b8;
  color: white;
}

.btn-info:hover {
  background: #1ab5cf;
}

/* ========== 圓形操作按鈕 ========== */
.btn-danger {
  width: auto;
  height: auto;
  padding: 2px 8px;
  background: #f56c6c;
  color: white;
  border-radius: 4px;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-danger:hover {
  background: #f89898;
  transform: scale(1.1);
}

.btn-warning {
  width: auto;
  height: auto;
  padding: 2px 8px;
  background: #409eff;
  color: #ffffff;
  border-radius: 4px;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-warning:hover {
  background: #66b1ff;
  transform: scale(1.1);
}

.ml-1 {
  margin-left: 1px;
}

/* ========== 標題樣式 ========== */
h2 {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 1.3rem;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

h2 button {
  padding: 6px 12px;
  font-size: 12px;
}

/* ========== 分頁樣式 ========== */
.pagination-info-bar {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 20px;
  padding: 10px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  font-size: 14px;
  color: #606266;
  gap: 8px;
}

.page-select {
  padding: 4px 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: white;
}

/* 懸浮按鈕容器 */
.floating-nav {
  position: fixed;
  top: 50%;
  transform: translateY(-50%);
  z-index: 3000;
  display: flex;
  flex-direction: column;
  gap: 15px;
  transition: left 0.3s ease-in-out;
  /* 配合側邊欄動畫平滑移動 */
}

.floating-nav.left {
  left: 253px;
}

.floating-nav.right {
  right: 2px;
}

/* 針對 AdminLTE 側邊欄的響應式調整 (Desktop) */
@media (min-width: 992px) {
  /* 當側邊欄展開時 (預設) - 避開 250px 的側邊欄 */
  :global(body:not(.sidebar-collapse)) .floating-nav.left {
    left: 255px;
  }

  /* 當側邊欄收合時 (sidebar-collapse) - 避開約 73px 的小側邊欄 */
  :global(body.sidebar-collapse) .floating-nav.left {
    left: 80px;
  }
}

/* 懸浮按鈕本體 */
.btn-float {
  padding: 3px 7px;
  background: rgba(64, 158, 255, 0.9);
  color: white;
  border: none;
  border-radius: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  font-weight: bold;
  transition: all 0.3s ease;
}

.btn-float:hover:not(:disabled) {
  background: #66b1ff;
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

.btn-float:disabled {
  background: #c0c4cc;
  color: #c0c4cc;
  color: #fff;
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

/* [新增] 點擊查看更多的樣式 */
.note-more {
  color: #409eff;
  cursor: pointer;
  font-weight: bold;
  margin-left: 2px;
}
.note-more:hover {
  text-decoration: underline;
}
</style>
