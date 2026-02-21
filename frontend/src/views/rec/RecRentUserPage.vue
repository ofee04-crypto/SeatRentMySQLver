<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMemberAuthStore } from '@/stores/memberAuth' // 引入 Store
import { useAdminAuthStore } from '@/stores/adminAuth' // 引入 Store

// --- Component Imports ---
import RecRentUserOrder from '@/components/rec/RecRentUserOrder.vue'
import RecRentUserComplete from '@/components/rec/RecRentUserComplete.vue'
import RecRentUserRecord from '@/components/rec/RecRentUserRecord.vue'

const route = useRoute()
const router = useRouter()
const memberAuthStore = useMemberAuthStore() // 初始化 Store
const adminAuthStore = useAdminAuthStore() // 初始化 Store
// --- 1. 狀態定義 (State Definitions) ---
// [修正] 根據路由參數初始化 activeView，避免預設 'order' 導致 RecRentUserOrder 在歸還模式下被錯誤掛載
// 這樣可以防止進入歸還頁面時，因為先渲染了租借頁面而觸發「有未歸還訂單」的防呆檢查
const activeView = ref(
  ['order', 'complete', 'record'].includes(route.params.action) ? route.params.action : 'order',
)

// --- 2. 視圖切換邏輯 (View Switching Logic) ---
const setActiveView = (view) => {
  activeView.value = view
}

// --- 3. 路由監聽 (Route Listener from UserPage) ---
onMounted(() => {
  // [新增] 進入頁面時，若 URL 有 spotId，同步到 Pinia；若 URL 沒有但 Pinia 有，則保留 Pinia 的值
  if (route.query.spotId) {
    memberAuthStore.setSpotId(route.query.spotId)
  }
})

watch(
  () => route.params.action,
  (newAction) => {
    if (newAction === 'order' || newAction === 'complete' || newAction === 'record') {
      setActiveView(newAction)
    }
  },
)

// [新增] 監聽 URL 的 spotId 變化，隨時同步回 Pinia (例如使用者手動改網址)
watch(
  () => route.query.spotId,
  (newId) => {
    if (newId) {
      memberAuthStore.setSpotId(newId)
    }
  },
)

// [新增] 統一取得目前的 SpotId (優先看 URL，沒有則看 Pinia)
const currentSpotId = computed(() => {
  return route.query.spotId || memberAuthStore.selectedSpotId
})

// --- 4. Computed for router-link ---
const orderRoute = computed(() => {
  const r = { name: 'rec-rent-user', params: { action: 'order' } }
  // [修改] 使用 currentSpotId 確保切換時帶上 ID
  if (currentSpotId.value) {
    r.query = { spotId: currentSpotId.value }
  }
  return r
})

// [新增] 歸還頁面的路由物件，確保切換時也帶上 spotId
const completeRoute = computed(() => {
  const r = { name: 'rec-rent-user', params: { action: 'complete' } }
  if (currentSpotId.value) {
    r.query = { spotId: currentSpotId.value }
  }
  return r
})

// [新增] 紀錄頁面的路由物件
const recordRoute = computed(() => {
  const r = { name: 'rec-rent-user', params: { action: 'record' } }
  if (currentSpotId.value) {
    r.query = { spotId: currentSpotId.value }
  }
  return r
})
</script>

<template>
  <div class="top-nav">
    <!-- 還原為 router-link，用於頁面內部切換 -->
    <router-link :to="orderRoute" custom v-slot="{ navigate }">
      <button
        @click="navigate"
        :class="{ active: activeView === 'order' }"
        :disabled="activeView === 'order'"
      >
        租借＠Seat
      </button>
    </router-link>
    <router-link :to="completeRoute" custom v-slot="{ navigate }">
      <button
        @click="navigate"
        :class="{ active: activeView === 'complete' }"
        :disabled="activeView === 'complete'"
      >
        歸還＠Seat
      </button>
    </router-link>
    <router-link :to="recordRoute" custom v-slot="{ navigate }">
      <button
        @click="navigate"
        :class="{ active: activeView === 'record' }"
        :disabled="activeView === 'record'"
      >
        我的租借紀錄
      </button>
    </router-link>
  </div>

  <div class="rec-rent-container">
    <div class="main-content">
      <h1>＠Seat 租借服務</h1>

      <!-- Views from UserPage -->
      <div v-if="activeView === 'order'" class="view-section">
        <!-- 傳遞 spotId 給子元件，會員資訊由子元件自行從 store 獲取 -->
        <!-- [修改] 改用 currentSpotId，這樣即使 URL 暫時沒有 query，只要 Pinia 有值也能顯示 -->
        <rec-rent-user-order v-if="currentSpotId" :spot-id="String(currentSpotId)" />
        <div v-else class="alert alert-info">請先至「站點地圖」選擇一個租借站點。</div>
        <router-link to="/SearchSpot" class="btn btn-primary">前往地圖</router-link>
      </div>
      <div v-if="activeView === 'complete'" class="view-section">
        <rec-rent-user-complete />
      </div>
      <div v-if="activeView === 'record'" class="view-section">
        <rec-rent-user-record />
      </div>
    </div>
  </div>
</template>

<style scoped>
/* General container and navigation styles */
.rec-rent-container {
  display: flex;
  flex-direction: column;
  /* height: 100%; */ /* 移除，讓內容自然撐高 */
  width: 100%;
  font-family: 'Microsoft JhengHei', Arial, sans-serif;
  background-color: #f9f9f9;
}

.top-nav {
  width: 100%;
  background-color: #acacac;
  color: white;
  display: flex;
  padding-top: 0px;
  flex-shrink: 0;
}

.top-nav button {
  background-color: #01e68e;
  color: #2b2b2b;
  font-size: 25px;
  font-weight: 500;
  display: flex;
  margin: 10px;
  border: none;
  cursor: pointer;
}

.top-nav button:disabled,
.top-nav button.active {
  background-color: #00ff9d;
  color: black;
  font-weight: bold;
  cursor: not-allowed;
}

.top-nav button:not(:disabled):not(.active):hover {
  background-color: #5dffc4;
}

.main-content {
  flex: 1;
  padding: 20px;
  /* overflow-y: auto; */ /* 移除，交由 MainLayout 滾動 */
}

.view-section {
  display: block;
}
</style>
