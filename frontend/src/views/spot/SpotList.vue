<template>
  <div class="spot-list-container">
    <!-- ========== Header 區域 ========== -->
    <section class="content-header">
      <div class="container-fluid">
        <div class="row align-items-center">
          <div class="col-sm-6">
            <div class="page-title-box">
              <div class="title-icon view-mode">
                <i class="fas fa-map-marker-alt"></i>
              </div>
              <div class="title-content">
                <h1>據點管理列表</h1>
                <p class="subtitle">管理所有營業據點資訊</p>
              </div>
            </div>
          </div>
          <div class="col-sm-6 text-right">
            <!-- 點擊新增，跳轉到 SpotForm (無 ID) -->
            <el-button type="primary" @click="goToAdd" class="add-btn">
              <i class="fas fa-plus mr-1"></i> 新增據點
            </el-button>
          </div>
        </div>
      </div>
    </section>

    <section class="content">
      <div class="container-fluid">
        <!-- ========== 數據摘要區 ========== -->
        <div class="stats-overview">
          <div class="stat-box blue">
            <div class="stat-icon"><i class="fas fa-building"></i></div>
            <div class="stat-info">
              <span class="stat-label">據點總數</span>
              <span class="stat-value">{{ statsSummary.totalSpots }}</span>
            </div>
          </div>
          <div class="stat-box orange">
            <div class="stat-icon"><i class="fas fa-wrench"></i></div>
            <div class="stat-info">
              <span class="stat-label">維修中據點</span>
              <span class="stat-value">{{ statsSummary.maintenance }}</span>
            </div>
          </div>
          <div class="stat-box purple">
            <div class="stat-icon"><i class="fas fa-chair"></i></div>
            <div class="stat-info">
              <span class="stat-label">營運座位數</span>
              <span class="stat-value">{{ statsSummary.activeSeats }}</span>
            </div>
          </div>
          <div class="stat-box green">
            <div class="stat-icon"><i class="fas fa-clipboard-check"></i></div>
            <div class="stat-info">
              <span class="stat-label">今日累計租借</span>
              <span class="stat-value">{{ statsSummary.todayRents }}</span>
            </div>
          </div>
        </div>

        <transition name="fade-slide" appear>
          <div>
            <!-- ========== 主要表格卡片 ========== -->
            <el-card shadow="hover" class="table-card">
              <!-- 工具列 -->
              <div class="toolbar">
                <div class="search-controls">
                  <!-- [修復] 補上遺失的關鍵字搜尋框 -->
                  <el-input
                    v-model="searchKeyword"
                    placeholder="搜尋名稱、代碼或地址"
                    clearable
                    prefix-icon="Search"
                    class="search-input"
                  />
                  <!-- Merchant ID 搜尋框 ，說明：min="0" 讓點擊上下箭頭時不會變負數；oninput 則是防止使用者直接用鍵盤打 - 號。-->
                  <el-input
                    v-model.number="searchMerchantId"
                    placeholder="搜尋ID"
                    type="number"
                    :min="1"
                    clearable
                    class="search-input-sm"
                    @input="sanitizeMerchantId"
                  />
                  <!-- 狀態下拉選單 -->
                  <el-select v-model="searchStatus" placeholder="全部狀態" clearable class="search-select">
                    <el-option label="營運中" value="營運中" />
                    <el-option label="停用" value="停用" />
                    <el-option label="維修中" value="維修中" />
                  </el-select>
                </div>
              </div>

              <!-- 表格 -->
              <el-table
                :data="paginatedSpots"
                v-loading="loading"
                stripe
                style="width: 100%"
                :header-cell-style="{ background: '#f5f7fa', fontWeight: 'bold', color: '#303133' }"
                class="modern-table"
              >
                <el-table-column prop="spotId" label="ID" width="70" align="center" />
                <el-table-column prop="spotCode" label="代碼" width="100" />
                <el-table-column prop="spotName" label="名稱" min-width="150">
                  <template #default="{ row }">
                    <div class="spot-name-cell">
                      <i class="fas fa-store text-primary mr-2"></i>
                      <span>{{ row.spotName }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="spotAddress" label="地址" min-width="200" show-overflow-tooltip />
                <!-- 這裡新增了 Merchant ID 的標題欄位 -->
                <!-- 建議後端 DTO 補上 merchantName -->
                <el-table-column label="Merchant ID" width="120" align="center">
                  <template #default="{ row }">
                    <el-tag v-if="row.merchantId" type="info" size="small" effect="plain">
                      {{ row.merchantName || row.merchantId }}
                    </el-tag>
                    <span v-else class="text-muted">-</span>
                  </template>
                </el-table-column>
                <el-table-column prop="spotStatus" label="狀態" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag
                      :type="(row.spotStatus === '營運中' || row.spotStatus === '啟用' || row.spotStatus === 1) ? 'success' : (row.spotStatus === '維修中' ? 'warning' : 'danger')"
                      size="small"
                      effect="light"
                    >
                      {{ row.spotStatus }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="180" align="center" fixed="right">
                  <template #default="{ row }">
                    <el-button-group>
                      <!-- [修改] 改用 button 統一操作風格，並呼叫 goToView 函式 -->
                      <el-tooltip content="檢視" placement="top">
                        <el-button size="small" type="info" @click="goToView(row.spotId)">
                          <i class="fas fa-eye"></i>
                        </el-button>
                      </el-tooltip>
                      <!-- 點擊編輯，跳轉到 SpotForm (帶 ID) -->
                      <el-tooltip content="編輯" placement="top">
                        <el-button size="small" type="primary" @click="goToEdit(row.spotId)">
                          <i class="fas fa-edit"></i>
                        </el-button>
                      </el-tooltip>
                      <el-tooltip content="刪除" placement="top">
                        <el-button size="small" type="danger" @click="deleteSpot(row.spotId, row.spotName)">
                          <i class="fas fa-trash-alt"></i>
                        </el-button>
                      </el-tooltip>
                    </el-button-group>
                  </template>
                </el-table-column>
              </el-table>

              <!-- 空資料提示 -->
              <el-empty v-if="paginatedSpots.length === 0 && !loading" description="目前沒有資料" />

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
import { useRouter } from 'vue-router'
import axios from 'axios'
import Swal from 'sweetalert2'

defineOptions({
  name: 'SpotList'
})

const router = useRouter()
const spots = ref([])
const loading = ref(false)
const searchKeyword = ref('') // 搜尋關鍵字狀態
const searchMerchantId = ref('') // Merchant ID 搜尋狀態
const searchStatus = ref('') // 狀態搜尋狀態

// 統計數據摘要
const statsSummary = ref({
  totalSpots: 0,
  maintenance: 0,
  activeSeats: 0,
  todayRents: 0
})

// 分頁設定
const currentPage = ref(1)
const pageSize = ref(10)

// 防止輸入負數
const sanitizeMerchantId = () => {
  const v = searchMerchantId.value
  if (v === '' || v === null || v === undefined) return
  if (!Number.isFinite(v) || v < 1) {
    searchMerchantId.value = 1
  }
}

const loadSpots = async () => {
  loading.value = true
  try {
    // 1. 先讀取列表 (核心功能)
    const resList = await axios.get('/api/spot/list')
    spots.value = resList.data

    // 2. 再嘗試讀取統計數據 (非核心功能，失敗不應影響列表)
    try {
      const resStats = await axios.get('/api/analyze/stats')
      calculateStats(resList.data, resStats.data)
    } catch (statsErr) {
      console.warn('統計數據讀取失敗，僅顯示列表:', statsErr)
      // 傳入 null 讓 calculateStats 計算基礎數據即可
      calculateStats(resList.data, null)
    }

  } catch (err) {
    console.error('讀取失敗:', err)
    Swal.fire({
      title: '讀取失敗',
      text: '讀取資料失敗，請檢查後端連線',
      icon: 'error',
      confirmButtonText: '確定',
      confirmButtonColor: '#409eff'
    })
  } finally {
    loading.value = false
  }
}

const calculateStats = (listData, statsData) => {
  // A. 從列表計算的基本數據
  statsSummary.value.totalSpots = listData.length
  statsSummary.value.maintenance = listData.filter(s => s.spotStatus === '維修中').length

  // B. 從統計 API 取得的進階數據 (spotMonitor 陣列)
  const monitorList = statsData?.spotMonitor || []

  // 用 reduce 加總所有站點的數據
  statsSummary.value.activeSeats = monitorList.reduce((sum, item) => sum + (item.totalSeats || 0), 0)
  statsSummary.value.todayRents = monitorList.reduce((sum, item) => sum + (item.rentedCount || 0), 0)
}

const filteredSpots = computed(() => {
  let results = spots.value
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.toLowerCase().trim()
    results = results.filter(
      (spot) =>
        spot.spotName?.toLowerCase().includes(keyword) ||
        spot.spotCode?.toLowerCase().includes(keyword) ||
        spot.spotAddress?.toLowerCase().includes(keyword),
    )
  }
  if (searchMerchantId.value !== '' && searchMerchantId.value !== null) {
    const mId = Number(searchMerchantId.value)
    results = results.filter((spot) => Number(spot.merchantId) === mId)
  }
  if (searchStatus.value) {
    results = results.filter((spot) => spot.spotStatus === searchStatus.value)
  }
  return results
})

// 分頁後的資料
const paginatedSpots = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredSpots.value.slice(start, end)
})

// 總筆數
const total = computed(() => filteredSpots.value.length)

// 當過濾條件變化時，重置到第一頁
const resetPage = () => {
  currentPage.value = 1
}

// 分頁變化
const handlePageChange = (page) => {
  currentPage.value = page
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
}

// 跳轉到新增頁面
const goToAdd = () => {
  router.push({ name: 'spot-add' })
}

const goToMonitor = () => {
  // 請確保 router/index.js 中已設定 name: 'dispatch-monitor' 的路由
  router.push({ name: 'dispatch-monitor' })
}

const goToAnalyze = () => {
  router.push({ name: 'spot-analyze' })
}

const goToView = (id) => {
  router.push({ name: 'spot-view', params: { id } })
}

const goToEdit = (id) => {
  router.push({ name: 'spot-edit', params: { id } })
}

const deleteSpot = async (id, name) => {
  // 二次確認：顯示更詳細的資訊 (名稱 + ID)，並提示無法復原，增加安全性
  const result = await Swal.fire({
    title: '確定要刪除嗎？',
    html: `您即將刪除據點「<strong>${name}</strong>」(ID: ${id})<br><span style="color: #f56c6c;">此動作無法復原！</span>`,
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#f56c6c',
    cancelButtonColor: '#909399',
    confirmButtonText: '是的，刪除',
    cancelButtonText: '取消'
  })

  if (!result.isConfirmed) return

  try {
    await axios.delete(`/api/spot/${id}`)

    Swal.fire({
      title: '刪除成功',
      text: '據點已成功刪除',
      icon: 'success',
      confirmButtonText: '確定',
      confirmButtonColor: '#67c23a'
    })
    await loadSpots()
  } catch (err) {
    console.error('刪除失敗:', err)
    Swal.fire({
      title: '刪除失敗',
      text: '刪除失敗，可能尚有相關聯的座位或訂單',
      icon: 'error',
      confirmButtonText: '確定',
      confirmButtonColor: '#409eff'
    })
  }
}

onMounted(() => {
  loadSpots()
})
</script>

<style scoped>
/* ========== 頁面容器 ========== */
.spot-list-container {
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
  min-height: 100vh;
}

/* ========== 數據摘要區 ========== */
.stats-overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-box {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(8px);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.5);
  transition: transform 0.3s ease;
}

.stat-box:hover {
  transform: translateY(-5px);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: white;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 0.85rem;
  color: #64748b;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
}

.stat-box.blue .stat-icon { background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%); }
.stat-box.orange .stat-icon { background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%); }
.stat-box.purple .stat-icon { background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%); }
.stat-box.green .stat-icon { background: linear-gradient(135deg, #10b981 0%, #059669 100%); }

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

.spot-name-cell {
  display: flex;
  align-items: center;
}

.text-primary {
  color: #409eff;
}

.text-muted {
  color: #909399;
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
.mb-4 { margin-bottom: 1.5rem; }
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
