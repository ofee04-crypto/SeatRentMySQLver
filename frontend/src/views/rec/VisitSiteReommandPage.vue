<script setup>
import { ref, onMounted, computed, watch } from 'vue'

const allData = ref([])
const displayData = ref([]) // 用於顯示和隨機排序的資料
const isLoading = ref(true)
const error = ref(null)

const currentPage = ref(1)
const itemsPerPage = 15

// --- 篩選用的狀態 ---
const searchQuery = ref('') // 綁定輸入框
const appliedSearchQuery = ref('') // 實際應用的搜尋關鍵字
const selectedRegion = ref('') // 空字串代表 '所有地區'
const selectedCategory = ref('all') // [新增] 'all', '景點', '餐廳'

// 從指定的 URL 獲取資料
async function fetchDataFromUrl(url) {
  const response = await fetch(url)
  if (!response.ok) {
    throw new Error(`無法從 ${url} 獲取資料: ${response.status} ${response.statusText}`)
  }
  return await response.json()
}

// 獲取所有景點和餐廳資料
async function fetchAllData() {
  isLoading.value = true
  error.value = null

  try {
    const restaurantUrl =
      'https://tdx.transportdata.tw/api/basic/v2/Tourism/Restaurant?%24top=9999&%24format=JSON'
    const scenicSpotUrl =
      'https://tdx.transportdata.tw/api/basic/v2/Tourism/ScenicSpot?%24top=9999&%24format=JSON'

    const [restaurants, scenicSpots] = await Promise.all([
      fetchDataFromUrl(restaurantUrl),
      fetchDataFromUrl(scenicSpotUrl),
    ])

    const formattedRestaurants = restaurants.map((item) => ({
      id: `res_${item.RestaurantID}`,
      name: item.RestaurantName,
      description: item.Description || '暫無描述',
      pictureUrl: item.Picture?.PictureUrl1,
      address: item.Address || '',
      type: '餐廳',
    }))

    const formattedScenicSpots = scenicSpots.map((item) => ({
      id: `spot_${item.ScenicSpotID}`,
      name: item.ScenicSpotName,
      description: item.DescriptionDetail || '暫無描述',
      pictureUrl: item.Picture?.PictureUrl1,
      address: item.Address || '',
      type: '景點',
    }))

    // 恢復為按名稱排序，作為預設順序
    allData.value = [...formattedRestaurants, ...formattedScenicSpots].sort((a, b) =>
      a.name.localeCompare(b.name),
    )
  } catch (e) {
    error.value = e.message
    console.error(e)
  } finally {
    isLoading.value = false
  }
}

// --- 過濾與排序邏輯 ---

// 固定的台灣縣市列表
const uniqueRegions = [
  '臺北市',
  '新北市',
  '桃園市',
  '臺中市',
  '臺南市',
  '高雄市',
  '基隆市',
  '宜蘭縣',
  '花蓮縣',
  '臺東縣',
  '新竹市',
  '新竹縣',
  '苗栗縣',
  '彰化縣',
  '南投縣',
  '雲林縣',
  '嘉義市',
  '嘉義縣',
  '屏東縣',
  '澎湖縣',
  '金門縣',
  '連江縣',
]

// 根據`appliedSearchQuery`和地區篩選資料
const filteredData = computed(() => {
  let data = allData.value

  if (selectedRegion.value) {
    data = data.filter((item) => item.address.startsWith(selectedRegion.value))
  }

  // [新增] 根據類別篩選
  if (selectedCategory.value !== 'all') {
    data = data.filter((item) => item.type === selectedCategory.value)
  }

  if (appliedSearchQuery.value) {
    const query = appliedSearchQuery.value.toLowerCase()
    data = data.filter(
      (item) =>
        item.name.toLowerCase().includes(query) || item.address.toLowerCase().includes(query),
    )
  }
  return data
})

// 監聽篩選結果，並將其同步到 displayData
watch(
  filteredData,
  (newData) => {
    displayData.value = newData
    currentPage.value = 1 // 篩選變更時，重置頁碼
  },
  { immediate: true },
)

// --- 使用者操作 ---

// 應用搜尋
function applySearch() {
  appliedSearchQuery.value = searchQuery.value
}

// 隨機排序當前顯示的列表
function randomizeOrder() {
  const dataToShuffle = [...displayData.value] // 複製以避免直接修改
  // Fisher-Yates 演算法
  for (let i = dataToShuffle.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[dataToShuffle[i], dataToShuffle[j]] = [dataToShuffle[j], dataToShuffle[i]]
  }
  displayData.value = dataToShuffle
  currentPage.value = 1 // 隨機排序後，回到第一頁
}

// UX 優化: 當使用者清空輸入框時，也自動清空篩選結果
watch(searchQuery, (newValue) => {
  if (newValue === '') {
    applySearch()
  }
})

// --- 分頁邏輯 (依賴 displayData) ---

const totalPages = computed(() => {
  return Math.ceil(displayData.value.length / itemsPerPage)
})

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  const end = start + itemsPerPage
  return displayData.value.slice(start, end)
})

function nextPage() {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
  }
}

function prevPage() {
  if (currentPage.value > 1) {
    currentPage.value--
  }
}

onMounted(() => {
  fetchAllData()
})
</script>

<template>
  <div class="container-fluid py-4">
    <!-- Page Header -->
    <div class="text-center mb-4">
      <h1 class="display-5">在地特色景點與美食餐廳</h1>
      <!-- Data Source Information -->
      <small class="text-muted">資料來源：交通部運輸資料流通服務平臺 (TDX)</small>
    </div>

    <!-- Main Content Card -->
    <div class="card shadow-sm">
      <div class="card-header bg-light">
        <div class="d-flex flex-wrap justify-content-between align-items-center gap-3">
          <!-- Left Controls: Filter and Randomizer -->
          <div class="d-flex flex-wrap gap-2 align-items-center">
            <select class="form-select form-select-sm" v-model="selectedRegion" style="width: auto">
              <option value="">所有地區</option>
              <option v-for="region in uniqueRegions" :key="region" :value="region">
                {{ region }}
              </option></select
            >&nbsp;&nbsp;&nbsp;&nbsp;

            <!-- [新增] 類別篩選按鈕 -->
            <div class="btn-group btn-group-sm" role="group">
              <button
                type="button"
                class="btn"
                :class="selectedCategory === 'all' ? 'btn-primary' : 'btn-outline-primary'"
                @click="selectedCategory = 'all'"
              >
                全部
              </button>
              <button
                type="button"
                class="btn"
                :class="selectedCategory === '景點' ? 'btn-primary' : 'btn-outline-primary'"
                @click="selectedCategory = '景點'"
              >
                景點
              </button>
              <button
                type="button"
                class="btn"
                :class="selectedCategory === '餐廳' ? 'btn-primary' : 'btn-outline-primary'"
                @click="selectedCategory = '餐廳'"
              >
                餐廳
              </button>
            </div>

            <button class="btn btn-success btn-sm" style="margin: 0 12px" @click="randomizeOrder">
              <i class="fas fa-random me-1"></i> 隨機推薦
            </button>
          </div>

          <!-- Right Controls: Search -->
          <div class="flex-grow-1" style="max-width: 300px">
            <form @submit.prevent="applySearch" class="w-100">
              <div class="input-group input-group-sm">
                <input
                  type="text"
                  class="form-control"
                  placeholder="搜尋名稱或地址..."
                  v-model="searchQuery"
                />
                <button class="btn btn-primary" type="submit" style="margin: 0 12px">
                  <i class="fas fa-search"></i>
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
      <div class="card-body">
        <!-- Loading State -->
        <div v-if="isLoading" class="text-center py-5">
          <div class="spinner-border text-primary" role="status" style="width: 3rem; height: 3rem">
            <span class="visually-hidden">Loading...</span>
          </div>
          <p class="mt-3 text-muted">資料載入中，請稍候...</p>
        </div>

        <!-- Error State -->
        <div v-else-if="error" class="alert alert-danger text-center">
          <h4><i class="fas fa-exclamation-triangle me-2"></i>資料載入失敗</h4>
          <p>{{ error }}</p>
          <p class="mb-0">請確認您的網路連線是否正常，或稍後再試。</p>
        </div>

        <!-- No Data State -->
        <div v-else-if="!isLoading && paginatedData.length === 0" class="text-center py-5">
          <i class="fas fa-search-minus fa-3x text-muted mb-3"></i>
          <h4>找不到符合條件的資料</h4>
          <p class="text-muted">請嘗試調整或放寬您的篩選條件。</p>
        </div>

        <!-- Data Grid -->
        <div v-else>
          <div class="row g-4">
            <div v-for="item in paginatedData" :key="item.id" class="col-12 col-md-4 g-4">
              <a
                :href="'https://www.google.com/search?q=' + encodeURIComponent(item.name)"
                target="_blank"
                rel="noopener noreferrer"
                class="text-decoration-none"
              >
                <div class="card h-100 recommendation-card">
                  <img
                    :src="item.pictureUrl || 'https://picsum.photos/320?random=' + item.id"
                    class="card-img-top"
                    :alt="item.name"
                    loading="lazy"
                  />
                  <div class="card-body d-flex flex-column">
                    <h5 class="card-title text-dark">
                      {{ item.name }}
                      <div
                        class="badge ms-2"
                        :class="
                          item.type === '景點' ? 'bg-primary text-white' : 'bg-warning text-dark'
                        "
                      >
                        {{ item.type }}
                      </div>
                    </h5>
                    <p class="card-text text-muted small flex-grow-1">
                      {{ item.description.substring(0, 80)
                      }}{{ item.description.length > 80 ? '...' : '' }}
                    </p>
                    <p class="card-text small text-muted">
                      <i class="fas fa-map-marker-alt me-2"></i>{{ item.address }}
                    </p>
                  </div>
                </div>
              </a>
            </div>
          </div>
        </div>
      </div>
      <div class="card-footer bg-light" v-if="!isLoading && paginatedData.length > 0">
        <div class="d-flex justify-content-between align-items-center flex-wrap gap-2">
          <!-- Pagination Controls -->
          <nav aria-label="Page navigation">
            <ul class="pagination pagination-sm m-0">
              <li class="page-item" :class="{ disabled: currentPage === 1 }">
                <a class="page-link" href="#" @click.prevent="prevPage">« 上一頁</a>
              </li>
              <li class="page-item disabled">
                <span class="page-link text-dark">第 {{ currentPage }} / {{ totalPages }} 頁</span>
              </li>
              <li class="page-item" :class="{ disabled: currentPage === totalPages }">
                <a class="page-link" href="#" @click.prevent="nextPage">下一頁 »</a>
              </li>
            </ul>
          </nav>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.recommendation-card {
  transition:
    transform 0.2s ease-in-out,
    box-shadow 0.2s ease-in-out;
  border: 1px solid #e9ecef;
}

.recommendation-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.1);
}

.card-title .badge {
  font-size: 0.8rem;
  vertical-align: middle;
}

.card-img-top {
  height: 250px;
  object-fit: cover;
  border-bottom: 1px solid #dee2e6;
}

/* Font Awesome Icons (if you are using them) */
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css');
</style>
