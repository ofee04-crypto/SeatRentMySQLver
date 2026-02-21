<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { LocationFilled, Camera, Aim } from '@element-plus/icons-vue'
import { Html5Qrcode } from 'html5-qrcode'
import axios from 'axios'
import { useRouter } from 'vue-router'
import { useMemberAuthStore } from '@/stores/memberAuth'
import { useAdminAuthStore } from '@/stores/adminAuth'

// --- Computed properties from store ---
// --- Props ---
const props = defineProps({
  spotId: {
    type: String,
    required: false,
    default: '',
  },
})

// --- Pinia Store ---
const memberAuthStore = useMemberAuthStore()
const adminAuthStore = useAdminAuthStore()

// --- Computed properties from store ---
const isLoggedIn = computed(() => memberAuthStore.isLogin && !!memberAuthStore.member?.memId)
const memberId = computed(() => memberAuthStore.member?.memId)
const memberName = computed(() => memberAuthStore.member?.memName || '訪客')

// --- 路由與狀態管理 ---
const router = useRouter()

// --- 1. 組態設定 ---
const center = ref({ lat: 24.973875, lng: 121.382025 })
const zoom = ref(10)
const backendApiUrl = 'http://localhost:8080/spot/list'
// 地圖選項設定 (啟用 Google Maps 的完整 UI 控制項)
const mapOptions = {
  zoomControl: true,
  mapTypeControl: true,
  streetViewControl: true,
  fullscreenControl: true,
  rotateControl: true,
}

// --- 2. 狀態定義 ---
const spots = ref([])
const error = ref(null)
// 用於存放搜尋結果的地圖標記位置
const searchResultMarker = ref(null)
// 用於雙向綁定搜尋框的輸入文字
const searchQuery = ref('')
const infoWindow = ref({
  position: null,
  opened: false,
  isSearchResult: false,
  isLoadingCounts: false, // 新增：用於追蹤數量是否載入中
  spot: null,
  title: '',
})

// --- QR Code 掃描相關邏輯 ---
const isScanning = ref(false)
let html5QrCode = null

const startScan = () => {
  isScanning.value = true
  nextTick(() => {
    // 確保 DOM 元素已渲染
    html5QrCode = new Html5Qrcode('reader')
    const config = { fps: 10, qrbox: { width: 250, height: 250 } }

    // 優先使用後置鏡頭
    html5QrCode
      .start({ facingMode: 'environment' }, config, onScanSuccess, (errorMessage) => {
        // 掃描過程中的錯誤通常忽略，避免 log 洗版
      })
      .catch((err) => {
        console.error('啟動鏡頭失敗', err)
        alert('無法啟動鏡頭，請確認您使用的是 HTTPS 連線並已授權相機權限。')
        isScanning.value = false
      })
  })
}

const onScanSuccess = (decodedText, decodedResult) => {
  console.log(`掃描成功: ${decodedText}`, decodedResult)
  searchQuery.value = decodedText // 將結果填入搜尋框
  stopScan()
  performSearch() // 自動執行搜尋
}

const stopScan = () => {
  if (html5QrCode) {
    html5QrCode
      .stop()
      .then(() => {
        html5QrCode.clear()
        isScanning.value = false
      })
      .catch((err) => console.error('停止掃描失敗', err))
  } else {
    isScanning.value = false
  }
}

// --- 3. 核心邏輯 ---

// 根據站點狀態生成帶有顏色的地圖圖示
const getMarkerIcon = (status) => {
  const color = status === '營運中' ? 'green' : 'gray'
  const svg = `
<svg xmlns="http://www.w3.org/2000/svg" width="8" height="8" fill="${color}" class="bi bi-lightbulb-fill" viewBox="0 0 16 16">
  <path d="M2 6a6 6 0 1 1 10.174 4.31c-.203.196-.359.4-.453.619l-.762 1.769A.5.5 0 0 1 10.5 13h-5a.5.5 0 0 1-.46-.302l-.761-1.77a2 2 0 0 0-.453-.618A5.98 5.98 0 0 1 2 6m3 8.5a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1l-.224.447a1 1 0 0 1-.894.553H6.618a1 1 0 0 1-.894-.553L5.5 15a.5.5 0 0 1-.5-.5"/>
</svg>
 `
  return {
    url: `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`,
    scaledSize: { width: 32, height: 32 },
  }
}

// 統一的地圖更新函式，用於聚焦與縮放
const updateMapLocation = (location) => {
  if (!location) return
  center.value = {
    lat: location.lat(),
    lng: location.lng(),
  }
  zoom.value = 15
}

// // 定位使用者位置 (高精準度)
// const locateUser = () => {
//   if (navigator.geolocation) {
//     navigator.geolocation.getCurrentPosition(
//       (position) => {
//         center.value = {
//           lat: position.coords.latitude,
//           lng: position.coords.longitude,
//         }
//         zoom.value = 15 // 定位成功後放大
//       },
//       (err) => {
//         console.warn(`無使用者地理位置: ${err.message}`)
//         // 可以選擇顯示錯誤提示，例如使用 Element Plus 的 ElMessage
//         alert('無法取得您的位置，請確認瀏覽器權限或 GPS 設定。')
//       },
//       {
//         enableHighAccuracy: true, // 要求高精準度 (GPS)
//         timeout: 5000, // 超時時間
//         maximumAge: 0, // 不使用快取
//       },
//     )
//   } else {
//     alert('此瀏覽器不支援地理位置功能。')
//   }
// }

// 處理從 GMapAutocomplete 選擇一個地點
const onPlaceChanged = (place) => {
  if (place && place.geometry && place.geometry.location) {
    const location = place.geometry.location
    updateMapLocation(location)
    searchResultMarker.value = location.toJSON()
    searchQuery.value = place.formatted_address // 將選擇的地點地址同步回輸入框

    infoWindow.value.opened = false
    nextTick(() => {
      infoWindow.value = {
        position: location.toJSON(),
        opened: true,
        isSearchResult: true,
        spot: null,
        title: place.formatted_address,
      }
    })
  }
}

// 執行地理編碼搜尋 (將地址文字轉換為座標)
const performSearch = () => {
  const query = searchQuery.value // 直接從狀態獲取查詢字串
  if (!query) return

  const geocoder = new google.maps.Geocoder()
  geocoder.geocode({ address: query }, (results, status) => {
    if (status === 'OK' && results[0]) {
      const place = results[0]
      const location = place.geometry.location
      updateMapLocation(location)
      searchResultMarker.value = location.toJSON()

      infoWindow.value.opened = false
      nextTick(() => {
        infoWindow.value = {
          position: location.toJSON(),
          opened: true,
          isSearchResult: true,
          spot: null,
          title: place.formatted_address,
        }
      })
    } else {
      console.warn(`Geocode 失敗，原因: ${status}`)
      alert('找不到指定的地點，請嘗試輸入更詳細的地址。')
    }
  })
}

// 從後端 API 獲取所有站點資料
const fetchSpots = async () => {
  try {
    const response = await axios.get(backendApiUrl)
    // 將後端資料轉換為地圖標記所需的格式
    const spotsData = response.data.map((spot) => ({
      id: spot.spotId,
      name: spot.spotName,
      status: spot.spotStatus,
      position: {
        lat: parseFloat(spot.latitude),
        lng: parseFloat(spot.longitude),
      },
      // 注意：seatCount 和 returnCount 在這裡不再預先載入
    }))
    spots.value = spotsData
  } catch (err) {
    console.error('無法獲取租借站點資料:', err)
    error.value = '無法載入租借站點資料，請稍後再試。'
  }
}

// 開啟一個站點的資訊視窗，並動態獲取數量
const openInfoWindowForSpot = async (spot) => {
  searchResultMarker.value = null // 清除搜尋結果的標記
  infoWindow.value.opened = false

  // 立即顯示基本資訊
  await nextTick()
  infoWindow.value = {
    position: spot.position,
    opened: true,
    isSearchResult: false,
    spot: { ...spot, seatCount: null, returnCount: null }, // 初始設定為 null
    title: spot.name,
    isLoadingCounts: true, // 開始載入
  }

  // 如果站點不在營運中，則不查詢數量
  if (spot.status !== '營運中') {
    infoWindow.value.spot.seatCount = 0
    infoWindow.value.spot.returnCount = 0 // 假設非營運中就沒有可還
    infoWindow.value.isLoadingCounts = false
    return
  }

  // 動態獲取可租借數量
  try {
    const countResponse = await axios.get(
      `http://localhost:8080/seats/count-by-spot?spotId=${spot.id}`,
    )
    const seatCount = countResponse.data
    // 更新 infoWindow 中的 spot 物件
    if (infoWindow.value.spot && infoWindow.value.spot.id === spot.id) {
      infoWindow.value.spot.seatCount = seatCount
      infoWindow.value.spot.returnCount = 20 - seatCount // 假設總數為 20
    }
  } catch (err) {
    console.error(`無法獲取站點 ${spot.id} 的座位數:`, err)
    if (infoWindow.value.spot && infoWindow.value.spot.id === spot.id) {
      infoWindow.value.spot.seatCount = 'N/A' // 發生錯誤時顯示 N/A
      infoWindow.value.spot.returnCount = 'N/A'
    }
  } finally {
    if (infoWindow.value.spot && infoWindow.value.spot.id === spot.id) {
      infoWindow.value.isLoadingCounts = false // 載入完成
    }
  }
}

// 關閉資訊視窗
const closeInfoWindow = () => {
  infoWindow.value.opened = false
  searchResultMarker.value = null // 同時清除搜尋結果的標記
}

// 處理導航至租借或歸還頁面
const handleNavigation = (action) => {
  const spotId = infoWindow.value.spot?.id
  let routeParams = { name: 'rec-rent-user', params: { action } }

  if (action === 'order' && spotId) {
    routeParams.query = { spotId }
  }

  // --- [新增] 將站點 ID 寫入 Pinia，確保切換頁面時能讀取 ---
  if (spotId) {
    memberAuthStore.setSpotId(spotId)
  }

  // --- 統一導航邏輯 ---
  // 1. 檢查是否處於「假登入」狀態 (isLogin=true 但 memId 遺失)
  if (memberAuthStore.isLogin && !memberAuthStore.member?.memId) {
    console.warn('偵測到登入狀態異常(無會員ID)，執行強制登出並重導向')
    memberAuthStore.clearMemberLogin()
    const redirectPath = router.resolve(routeParams).path
    router.push({ name: 'login', query: { redirect: redirectPath } })
    return
  }

  // 2. 正常狀態判斷
  if (memberAuthStore.isLogin || adminAuthStore.isLogin) {
    console.log('準備導向 Store 中的會員ID:', memberAuthStore.member?.memId)
    router.push(routeParams)
  } else {
    const redirectPath = router.resolve(routeParams).path
    router.push({ name: 'login', query: { redirect: redirectPath } })
  }
}

// --- (翌帆2026-1-31新增 客服回報用)【新增】處理問題回報：導向 /support/report 並帶入 query 參數 ---
const handleReportIssue = () => {
  const spotId = infoWindow.value.spot?.id
  const seatId = null // 地圖頁目前沒有特定 seatId，可依需求擴充
  const recId = null // 地圖頁目前沒有 recId

  const query = {}
  if (spotId) query.spotId = spotId
  if (seatId) query.seatId = seatId
  if (recId) query.recId = recId

  router.push({ path: '/support/report', query })
}

// Vue 組件掛載時執行的初始化
onMounted(() => {
  fetchSpots()
  fetchSpots()
  //locateUser() // 初始化時嘗試定位
})
</script>

<template>
  <div class="map-container-wrapper">
    <!-- 地點搜尋列 -->
    <!-- <button class="search-button scan-btn" @click="startScan" title="掃描 QR Code">
      <el-icon :size="20"><Camera /></el-icon>
    </button> -->
    <div class="search-bar-container">
      <GMapAutocomplete
        @place_changed="onPlaceChanged"
        :options="{
          fields: ['geometry', 'formatted_address', 'name'],
          componentRestrictions: { country: 'tw' },
        }"
      >
        <input
          type="text"
          class="search-input"
          placeholder="搜尋地點..."
          v-model="searchQuery"
          @keyup.enter="performSearch"
        />
      </GMapAutocomplete>

      <button class="search-button" @click="performSearch" title="搜尋">
        <el-icon :size="20"><LocationFilled /></el-icon>
      </button>

      <!-- [新增] 定位按鈕
      <button class="search-button locate-btn" @click="locateUser" title="定位我的位置">
        <el-icon :size="20"><Aim /></el-icon>
      </button> -->
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>

    <GMapMap
      v-if="!error"
      :center="center"
      :zoom="zoom"
      :options="mapOptions"
      class="map"
      map-id="d2fc83863651fe9c90d73c8a"
    >
      <!-- 渲染站點標記 -->
      <GMapMarker
        v-for="spot in spots"
        :key="spot.id"
        :position="spot.position"
        :title="spot.name"
        :clickable="true"
        :icon="getMarkerIcon(spot.status)"
        @click="openInfoWindowForSpot(spot)"
      />
      <!-- 渲染搜尋結果標記 -->
      <GMapMarker v-if="searchResultMarker" :key="'search-result'" :position="searchResultMarker" />

      <GMapInfoWindow
        :opened="infoWindow.opened"
        :position="infoWindow.position"
        :options="{ pixelOffset: { width: 0, height: -35 }, headerDisabled: true }"
        @closeclick="closeInfoWindow"
      >
        <div class="info-window-content">
          <!-- 顯示站點資訊 -->
          <div v-if="infoWindow.spot">
            <span
              ><h5>{{ infoWindow.title }}</h5></span
            >
            <!-- <strong>ID:</strong> {{ infoWindow.spot.id }}-->
            <strong>狀態: {{ infoWindow.spot.status }}</strong>

            <!-- 當數量載入中時顯示讀取訊息 -->
            <div v-if="infoWindow.isLoadingCounts" class="loading-text">查詢中...</div>
            <!-- 載入完成後顯示數量與按鈕 -->
            <div v-else style="font-size: 15 px">
              <span style="color: darkgreen">
                <strong>可租借數量:</strong> {{ infoWindow.spot.seatCount }}</span
              >
              <span> | </span>
              <span style="color: darkblue"
                ><strong>可歸還數量:</strong> {{ infoWindow.spot.returnCount }}</span
              >
              <div class="button-group">
                <button
                  @click="handleNavigation('order')"
                  class="btn btn-success"
                  :disabled="infoWindow.spot.status !== '營運中' || infoWindow.spot.seatCount <= 0"
                  :class="{
                    'btn-disabled':
                      infoWindow.spot.status !== '營運中' || infoWindow.spot.seatCount <= 0,
                  }"
                >
                  租借
                </button>
                <button
                  @click="handleNavigation('complete')"
                  class="btn btn-primary"
                  :disabled="
                    infoWindow.spot.status !== '營運中' || infoWindow.spot.returnCount <= 0
                  "
                  :class="{
                    'btn-disabled':
                      infoWindow.spot.status !== '營運中' || infoWindow.spot.returnCount <= 0,
                  }"
                >
                  歸還
                </button>
                <button @click="handleReportIssue" class="btn btn-issue">
                  回報 <br />
                  問題
                </button>
              </div>
            </div>
          </div>
          <!-- 顯示搜尋結果 -->
          <div v-else-if="infoWindow.isSearchResult">
            <h4>{{ infoWindow.title }}</h4>
            <a
              v-if="infoWindow.position"
              :href="`https://www.google.com/maps/search/?api=1&query=${infoWindow.position.lat},${infoWindow.position.lng}`"
              target="_blank"
              rel="noopener noreferrer"
              class="map-link"
            >
              在 Google 地圖中查看
            </a>
          </div>
        </div>
      </GMapInfoWindow>
    </GMapMap>

    <!-- QR Code 掃描遮罩層 -->
    <div v-if="isScanning" class="scanner-overlay">
      <div class="scanner-container">
        <div id="reader" width="100%"></div>
        <button class="btn btn-primary close-scan-btn" @click="stopScan">關閉掃描</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.map-container-wrapper {
  width: 100%;
  height: 100%;
  position: relative;
}
.map {
  width: 100%;
  height: 100%;
}
.error-message {
  color: red;
  padding: 20px;
  text-align: center;
}
.info-window-content {
  padding: 0px;
  min-height: 50px;
  min-width: 150px;
  /* overflow: unset; */
}
.info-window-content h4,
.info-window-content p {
  margin: 1px 0;
}

/* 在 Google 地圖中查看的連結樣式 */
.info-window-content .map-link {
  display: block;
  margin-top: 3px;
  font-weight: 400;
  text-decoration: none;
  color: #007bff;
}
.info-window-content .map-link:hover {
  text-decoration: underline;
}
.loading-text {
  padding: 10px;
  text-align: center;
  color: #666;
}
.button-group {
  margin-top: 15px;
  display: flex;
  justify-content: space-around;
}
.btn {
  display: inline-block;
  font-weight: 500;
  text-align: center;
  white-space: nowrap;
  vertical-align: middle;
  user-select: none;
  border: 1px solid transparent;
  margin: 0.3rem;
  padding: 0.15rem 0.45rem;
  font-size: 1.5rem;
  line-height: 1.5;
  border-radius: 0.25rem;
  cursor: pointer;
  text-decoration: none; /* Add this to make router-link look like a button */
}
.btn-success {
  color: #fff;
  background-color: #28a745;
  border-color: #28a745;
}
.btn-primary {
  color: #fff;
  background-color: #007bff;
  border-color: #007bff;
}
.btn-issue {
  font-size: medium;
  color: #fff;
  background-color: #cf820e;
  border-color: #cf820e;
}

/* 當按鈕被禁用時的樣式 */
.btn-disabled {
  background-color: #6b6b6b;
  border-color: #6b6b6b;
  cursor: not-allowed;
}

/* --- 新增/修改的搜尋列樣式 --- */
.search-bar-container {
  position: absolute;
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
  width: 275px;
  height: 45px;
  z-index: 10;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  padding: 0 12px;
}

.search-bar-container :deep(.search-input) {
  flex-grow: 12;
  width: 130%;
  height: 68px;
  border: none;
  outline: none;
  font-size: 2.2rem;
  background-color: transparent;
}

/* 這是 GMapAutocomplete 元件的包裝器，我們讓它填滿空間 */
:deep(.pac-container) {
  z-index: 1051 !important; /* 確保建議清單顯示在其他元素之上 */
}

.search-button {
  height: 33px;
  margin-left: 8px;
  padding: 8px;
  border: none;
  background-color: #007bff;
  color: white;
  border-radius: 7px;
  cursor: pointer;
  font-size: 1.9rem;
  flex-shrink: 0;
  display: flex;
  align-items: center;
}
.scan-btn {
  background-color: #6c757d; /* 灰色區分 */
  margin-left: 5px;
}

/* [新增] 定位按鈕樣式 */
.locate-btn {
  background-color: #28a745; /* 綠色 */
}
.locate-btn:hover {
  background-color: #218838;
}

.search-button:hover {
  background-color: #0056b3;
}

/* 掃描介面樣式 */
.scanner-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.8);
  z-index: 9999;
  display: flex;
  justify-content: center;
  align-items: center;
}

.scanner-container {
  width: 90%;
  max-width: 500px;
  background: white;
  padding: 20px;
  border-radius: 10px;
  text-align: center;
}

.close-scan-btn {
  margin-top: 15px;
  width: 100%;
  font-size: 1.2rem;
}
</style>
