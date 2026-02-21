<template>
  <div class="seat-list-container">
    <!-- ========== Header 區域 ========== -->
    <section class="content-header">
      <div class="container-fluid">
        <div class="row align-items-center">
          <div class="col-sm-6">
            <div class="page-title-box">
              <div class="title-icon view-mode">
                <i class="fas fa-chair"></i>
              </div>
              <div class="title-content">
                <h1>座位管理列表</h1>
                <p class="subtitle">管理所有座位設備資訊</p>
              </div>
            </div>
          </div>
          <div class="col-sm-6 text-right">
            <el-button type="primary" @click="goToAdd" class="add-btn">
              <i class="fas fa-plus mr-1"></i> 新增座位
            </el-button>
          </div>
        </div>
      </div>
    </section>

    <section class="content">
      <div class="container-fluid">
        <transition name="fade-slide" appear>
          <div>
            <!-- ========== 主要表格卡片 ========== -->
            <el-card shadow="hover" class="table-card">
              <!-- 搜尋區塊 -->
              <!-- [修改] 參考 SpotList.vue 的搜尋介面 -->
              <div class="toolbar">
                <div class="search-controls">
                  <el-input
                    v-model="searchKeyword"
                    placeholder="搜尋名稱、類型或序號"
                    clearable
                    prefix-icon="Search"
                    class="search-input"
                  />
                  <el-input
                    v-model.number="searchSpotId"
                    placeholder="搜尋據點ID"
                    type="number"
                    :min="1"
                    clearable
                    class="search-input-sm"
                    @input="sanitizeSpotId"
                  />
                  <el-select v-model="searchStatus" placeholder="全部狀態" clearable class="search-select">
                    <el-option label="啟用" value="啟用" />
                    <el-option label="停用" value="停用" />
                    <el-option label="維修中" value="維修中" />
                  </el-select>
                </div>
              </div>

              <!-- 表格 -->
              <el-table
                :data="paginatedSeats"
                v-loading="loading"
                stripe
                style="width: 100%"
                :header-cell-style="{ background: '#f5f7fa', fontWeight: 'bold', color: '#303133' }"
                class="modern-table"
              >
                <el-table-column prop="seatsId" label="ID" width="70" align="center" />
                <el-table-column prop="seatsName" label="名稱" min-width="120">
                  <template #default="{ row }">
                    <div class="seat-name-cell">
                      <i class="fas fa-chair text-primary mr-2"></i>
                      <span>{{ row.seatsName }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="seatsType" label="類型" width="120" />
                <el-table-column prop="serialNumber" label="序號" width="140" />
                <!-- 建議後端 DTO 回傳 spotName，若無則顯示 ID -->
                <el-table-column label="所屬據點 ID" width="120" align="center">
                  <template #default="{ row }">
                    <el-tag type="info" size="small" effect="plain">
                      {{ row.spotName || row.spotId }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="seatsStatus" label="設備狀態" width="100" align="center">
                  <!-- 增加對後端可能回傳 1/0 或 ACTIVE/INACTIVE 的相容性判斷 -->
                  <template #default="{ row }">
                    <el-tag
                      :type="(row.seatsStatus === '啟用' || row.seatsStatus === 1 || row.seatsStatus === 'ACTIVE') ? 'success' : (row.seatsStatus === '維修中' ? 'warning' : 'danger')"
                      size="small"
                      effect="light"
                    >
                      {{ row.seatsStatus }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="180" align="center" fixed="right">
                  <template #default="{ row }">
                    <el-button-group>
                      <!-- [新增] 詳細按鈕 -->
                      <el-tooltip content="檢視" placement="top">
                        <el-button size="small" type="info" @click="goToView(row.seatsId)">
                          <i class="fas fa-eye"></i>
                        </el-button>
                      </el-tooltip>
                      <!-- [新增] 編輯按鈕 -->
                      <el-tooltip content="編輯" placement="top">
                        <el-button size="small" type="primary" @click="goToEdit(row.seatsId)">
                          <i class="fas fa-edit"></i>
                        </el-button>
                      </el-tooltip>
                      <el-tooltip content="刪除" placement="top">
                        <el-button size="small" type="danger" @click="deleteSeat(row.seatsId)">
                          <i class="fas fa-trash-alt"></i>
                        </el-button>
                      </el-tooltip>
                    </el-button-group>
                  </template>
                </el-table-column>
              </el-table>

              <el-empty v-if="filteredSeats.length === 0 && !loading" description="目前沒有符合條件的座位資料" />

              <!-- 分頁元件 -->
              <div class="pagination-wrapper" v-if="total > 0">
                <el-pagination
                  v-model:current-page="currentPage"
                  v-model:page-size="pageSize"
                  :page-sizes="[10, 20, 50, 100]"
                  :total="total"
                  layout="total, sizes, prev, pager, next, jumper"
                  background
                  @size-change="handleSizeChange"
                  @current-change="handlePageChange"
                />
              </div>
            </el-card>
          </div>
        </transition>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router' // [新增] 引入 useRouter
import axios from 'axios'
import Swal from 'sweetalert2' // 假設您有安裝，若無可改用 alert

// 明確定義元件名稱
defineOptions({
  name: 'SeatList'
})

const router = useRouter() // [新增] 初始化 router
const seats = ref([])
const loading = ref(false)

// 分頁狀態
const currentPage = ref(1)
const pageSize = ref(10)

// [新增] 參考 SpotList.vue 的搜尋狀態
const searchKeyword = ref('')
const searchSpotId = ref('')
const searchStatus = ref('')

// [新增] 參考 SpotList.vue 的數字輸入處理函式，並調整變數名稱
const sanitizeSpotId = () => {
  const v = searchSpotId.value
  if (v === '' || v === null || v === undefined) return

  if (!Number.isFinite(v) || v < 1) {
    searchSpotId.value = 1
  }
}

// [新增] 跳轉到詳細頁 (使用具名路由，更安全)
const goToView = (id) => {
  router.push({ name: 'seat-view', params: { id } })
}

// [新增] 跳轉到編輯頁
const goToEdit = (id) => {
  router.push({ name: 'seat-edit', params: { id } })
}

// [新增] 跳轉到新增頁
const goToAdd = () => {
  router.push({ name: 'seat-insert' })
}

// 查詢座位
const fetchSeats = async () => {
  loading.value = true
  try {
    // [修改] 為了實現前端過濾，移除請求參數，一次性獲取所有資料
    const res = await axios.get('/api/seats/search')
    seats.value = res.data
  } catch (err) {
    console.error('載入失敗:', err)
    Swal.fire({
      title: '錯誤',
      html: '<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg><p style="margin-top:12px;">無法載入座位列表</p></div>',
      confirmButtonColor: '#409eff',
      confirmButtonText: '確定'
    })
  } finally {
    loading.value = false
  }
}

// [新增] 參考 SpotList.vue 的計算屬性，並根據 Seat 的欄位調整過濾邏輯
const filteredSeats = computed(() => {
  let results = seats.value

  // 1. 關鍵字過濾 (對應 Seat 的名稱、類型、序號)
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.toLowerCase().trim()
    results = results.filter(
      (seat) =>
        seat.seatsName?.toLowerCase().includes(keyword) ||
        seat.seatsType?.toLowerCase().includes(keyword) ||
        seat.serialNumber?.toLowerCase().includes(keyword)
    )
  }

  // 2. 據點 ID 過濾 (對應 Seat 的 spotId)
  if (searchSpotId.value !== '' && searchSpotId.value !== null) {
    const sId = Number(searchSpotId.value)
    results = results.filter((seat) => Number(seat.spotId) === sId)
  }

  // 3. 狀態過濾 (對應 Seat 的 seatsStatus)
  if (searchStatus.value) {
    results = results.filter((seat) => seat.seatsStatus === searchStatus.value)
  }

  return results
})

// 分頁後的資料
const paginatedSeats = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredSeats.value.slice(start, end)
})

// 總筆數
const total = computed(() => filteredSeats.value.length)

// 分頁切換處理
const handlePageChange = (page) => {
  currentPage.value = page
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
}

// 刪除座位
const deleteSeat = async (id) => {
  const result = await Swal.fire({
    title: '確定刪除?',
    html: '<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#e6a23c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg><p style="margin-top:12px;">刪除後將無法復原！</p></div>',
    showCancelButton: true,
    confirmButtonColor: '#f56c6c',
    cancelButtonColor: '#909399',
    confirmButtonText: '是的，刪除',
    cancelButtonText: '取消'
  })

  if (result.isConfirmed) {
    try {
      await axios.delete(`/api/seats/${id}`)
      Swal.fire({
        title: '已刪除',
        html: '<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#67c23a" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg><p style="margin-top:12px;">該座位已被移除</p></div>',
        confirmButtonColor: '#409eff',
        confirmButtonText: '確定'
      })
      fetchSeats() // 重新整理列表
    } catch (err) {
      Swal.fire({
        title: '失敗',
        html: '<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg><p style="margin-top:12px;">刪除失敗，可能尚有相關聯的訂單</p></div>',
        confirmButtonColor: '#409eff',
        confirmButtonText: '確定'
      })
    }
  }
}

onMounted(() => {
  fetchSeats()
})
</script>

<style scoped>
/* ========== 頁面容器 ========== */
.seat-list-container {
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
  min-height: 100vh;
}

/* ========== 頁面標題區 ========== */
.page-title-box {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.title-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  transition: transform 0.3s ease;
}

.title-icon:hover {
  transform: scale(1.1) rotate(5deg);
}

.view-mode {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
}

.title-content h1 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
  color: #303133;
}

.title-content .subtitle {
  margin: 4px 0 0;
  font-size: 0.875rem;
  color: #909399;
}

/* ========== 新增按鈕 ========== */
.add-btn {
  border-radius: 10px;
  font-weight: 600;
  padding: 12px 24px;
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
  border: none;
  transition: all 0.3s ease;
}

.add-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(103, 194, 58, 0.4);
}

/* ========== 表格卡片 ========== */
.table-card {
  border-radius: 12px;
  overflow: hidden;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.search-controls {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input {
  width: 220px;
}

.search-input-sm {
  width: 120px;
}

.search-select {
  width: 140px;
}

/* ========== 表格樣式 ========== */
.modern-table {
  border-radius: 8px;
  overflow: hidden;
}

.seat-name-cell {
  display: flex;
  align-items: center;
}

.text-primary {
  color: #409eff;
}

/* ========== 動畫效果 ========== */
.fade-slide-enter-active {
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.fade-slide-leave-active {
  transition: all 0.3s ease-in;
}

.fade-slide-enter-from {
  transform: translateY(20px);
  opacity: 0;
}

.fade-slide-leave-to {
  transform: translateY(-10px);
  opacity: 0;
}

/* ========== 間距工具類 ========== */
.mr-1 { margin-right: 4px; }
.mr-2 { margin-right: 8px; }

/* ========== 分頁樣式 ========== */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

/* ========== 響應式設計 ========== */
@media (max-width: 768px) {
  .page-title-box {
    flex-direction: column;
    text-align: center;
    gap: 12px;
  }
  
  .toolbar {
    flex-direction: column;
    gap: 12px;
  }
  
  .search-controls {
    flex-wrap: wrap;
    width: 100%;
  }
  
  .search-input,
  .search-input-sm,
  .search-select {
    width: 100%;
  }
}
</style>