<script setup>
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { useMemberAuthStore } from '@/stores/memberAuth'
import { useRouter } from 'vue-router' // 【新增】引入 router

// --- Pinia Store ---
const memberAuthStore = useMemberAuthStore()

// --- Router ---
const router = useRouter() // 【新增】初始化 router

// --- 狀態定義 ---
const rents = ref([])
const isLoading = ref(false)
const errorMessage = ref('')

// --- 分頁狀態 ---
const currentPage = ref(1)
const itemsPerPage = 10 // 每頁顯示10筆

// --- Computed Properties ---
const totalPages = computed(() => {
  if (!rents.value) return 1
  return Math.ceil(rents.value.length / itemsPerPage)
})

const paginatedRents = computed(() => {
  if (!rents.value) return []
  const startIndex = (currentPage.value - 1) * itemsPerPage
  const endIndex = startIndex + itemsPerPage
  return rents.value.slice(startIndex, endIndex)
})

// --- 核心邏輯 ---
const loadUserRents = async () => {
  const memId = memberAuthStore.member?.memId

  // 防呆：未登入不執行查詢
  if (!memId) {
    errorMessage.value = '請先登入會員以查看紀錄'
    return
  }

  isLoading.value = true
  errorMessage.value = ''

  try {
    // 呼叫後端 API 查詢該會員的訂單 (假設後端支援 memId 參數過濾)
    const response = await axios.get(`http://localhost:8080/rec-rent?memId=${memId}`)
    let data = response.data

    // --- 排序邏輯 ---
    // 1. recStatus = "租借中" 的紀錄排在最上面
    // 2. 其餘紀錄依照租借時間 (recRentDT2) 倒序排列 (新的在前)
    data.sort((a, b) => {
      const isActiveA = a.recStatus === '租借中'
      const isActiveB = b.recStatus === '租借中'

      if (isActiveA && !isActiveB) return -1 // a 排前
      if (!isActiveA && isActiveB) return 1 // b 排前

      // 若狀態權重相同，比較時間 (倒序)
      const dateA = new Date(a.recRentDT2).getTime()
      const dateB = new Date(b.recRentDT2).getTime()
      return dateB - dateA
    })

    rents.value = data
    currentPage.value = 1 // 載入新資料時，重置回第一頁
  } catch (error) {
    console.error('載入租借紀錄失敗:', error)
    errorMessage.value = '無法載入租借紀錄，請確認網路連線或稍後再試。'
  } finally {
    isLoading.value = false
  }
}

// --- 【修改】問題回報處理：導向 /support/report 並帶入 query 參數 ---
const handleReport = (rent) => {
  const query = {}
  if (rent.recSeqId) query.recId = rent.recSeqId // 訂單 ID
  if (rent.spotIdRent) query.spotId = rent.spotIdRent // 站點 ID
  if (rent.seatsId) query.seatId = rent.seatsId // 座位 ID

  router.push({ path: '/support/report', query })
}

// --- 分頁方法 ---
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
  }
}

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
  }
}

const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
  }
}

// --- Helper Functions ---
const getStatusClass = (status) => {
  // 根據訂單狀態回傳對應的 BS class
  if (status === '租借中') {
    return 'bg-success'
  }
  if (status === '未付款') {
    return 'bg-warning'
  }
  return 'bg-secondary'
}

// --- (翌帆2026-1-31新增 客服回報用)【新增】處理問題回報：導向 /support/report 並帶入 query 參數 ---
const handleReportIssue = () => {
  const spotId = null //
  const seatId = null // 地圖頁目前沒有特定 seatId，可依需求擴充
  const recId = null // 地圖頁目前沒有 recId

  const query = {}
  if (spotId) query.spotId = spotId
  if (seatId) query.seatId = seatId
  if (recId) query.recId = recId

  router.push({ path: '/support/report', query })
}

// --- Lifecycle ---
onMounted(() => {
  if (memberAuthStore.isLogin) {
    loadUserRents()
  }
})
</script>

<template>
  <div class="record-container">
    <h2 class="section-title"><i class="fas fa-history"></i> 我的租借紀錄</h2>

    <!-- 載入中 -->
    <div v-if="isLoading" class="text-center my-5">
      <div class="spinner-border text-primary" role="status"></div>
      <p class="mt-2">載入中...</p>
    </div>

    <!-- 錯誤訊息 -->
    <div v-else-if="errorMessage" class="alert alert-danger">
      {{ errorMessage }}
    </div>

    <!-- 無資料 -->
    <div v-else-if="rents.length === 0" class="alert alert-info">目前沒有租借紀錄。</div>

    <!-- 列表顯示 -->
    <div v-else>
      <div class="list-group">
        <div
          v-for="rent in paginatedRents"
          :key="rent.recSeqId"
          class="list-group-item mb-3 shadow-sm"
          :class="{ 'border-active': rent.recStatus === '租借中' || rent.recStatus === '未付款' }"
        >
          <div class="d-flex w-100 justify-content-between align-items-center header-row">
            <h5 class="mb-1">
              <span class="badge" :class="getStatusClass(rent.recStatus)">
                {{ rent.recStatus }}
              </span>
              <span class="ms-2 title-text">訂單編號: {{ rent.recId || rent.recSeqId }}</span>
            </h5>
            <span style="margin: 0 60px"> </span>
           
            <span></span> <span style="margin: 0 40px"> </span>
            <h5 class="mb-0 text-primary fw-bold">
              <button class="btn btn-sm btn-outline-warning" @click="handleReportIssue">
                <i class="fas fa-exclamation-circle"></i> 問題回報
              </button>
            </h5>
          </div>

          <div class="row mt-2">
             <span class="ms-2 title-text"> </span>
            <div class="col-md-6">
              <p class="mb-1">
                <strong>發票號碼:</strong> {{ rent.recInvoice  }}
              </p>
              <p class="mb-1">
                <strong>租借站點:</strong> {{ rent.rentSpotName || rent.spotIdRent }}
              </p>
              <p class="mb-1">
                <strong>歸還站點:</strong> {{ rent.returnSpotName || rent.spotIdReturn || '-' }}
              </p>
            </div>
            <div class="col-md-6">
              <p class="mb-1">
                <strong>座椅編號:</strong> SN-2026{{ rent.seatsId || '-' }}
              </p>
              <p class="mb-1">
                <strong>租借時間:</strong> {{ rent.recRentDT2?.replace('T', ' ') || '-' }}
              </p>
              <p class="mb-1">
                <strong>歸還時間:</strong> {{ rent.recReturnDT2?.replace('T', ' ') || '-' }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Pagination Controls -->
      <nav v-if="totalPages > 1" aria-label="Page navigation" class="mt-4">
        <ul class="pagination justify-content-center flex-wrap">
          <li class="page-item" :class="{ disabled: currentPage === 1 }">
            <a class="page-link" href="#" @click.prevent="prevPage">&laquo;</a>
          </li>
          <li
            v-for="page in totalPages"
            :key="page"
            class="page-item"
            :class="{ active: currentPage === page }"
          >
            <a class="page-link" href="#" @click.prevent="goToPage(page)">{{ page }}</a>
          </li>
          <li class="page-item" :class="{ disabled: currentPage === totalPages }">
            <a class="page-link" href="#" @click.prevent="nextPage">&raquo;</a>
          </li>
        </ul>
      </nav>
    </div>
  </div>
</template>

<style scoped>
.record-container {
  padding: 10px;
  max-width: max-content;
  margin: 0 auto;
}
.section-title {
  color: #333;
  border-bottom: 2px solid #007bff;
  padding-bottom: 10px;
  margin-bottom: 20px;
}
.list-group-item {
  border: 1px solid #ddd;
  border-radius: 8px !important;
  transition: transform 0.2s;
}
.list-group-item:hover {
  transform: translateY(-2px);
}
/* 租借中狀態的特殊樣式 (左側綠色邊條) */
.border-active {
  border-left: 5px solid #28a745 !important;
  background-color: #f9fff9;
}
.badge {
  padding: 0.5em 0.7em;
  border-radius: 0.25rem;
  color: white;
}
.bg-success {
  background-color: #28a745;
}
.bg-secondary {
  background-color: #6c757d;
}
.bg-warning {
  background-color: #ffc107;
  color: #212529; /* 深色文字以確保可讀性 */
}
.text-primary {
  color: #007bff !important;
}
.text-muted {
  color: #6c757d !important;
}

/* Pagination Styles */
.page-item .page-link {
  color: #007bff;
  margin: 0 2px; /* 增加按鈕間距 */
  border-radius: 4px; /* 圓角 */
}
.page-item.active .page-link {
  background-color: #007bff;
  border-color: #007bff;
  color: white;
  z-index: 2;
}
.page-item.disabled .page-link {
  color: #6c757d;
  background-color: #fff;
  border-color: #dee2e6;
}
.pagination {
  padding-left: 0;
  list-style: none;
}
</style>
