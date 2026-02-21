<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import axios from 'axios'
import { useRouter, useRoute } from 'vue-router'
import { useMemberAuthStore } from '@/stores/memberAuth'

// --- Pinia Store ---
const memberAuthStore = useMemberAuthStore()
const router = useRouter()
const route = useRoute()

// --- Computed properties from store ---
const isLoggedIn = computed(() => memberAuthStore.isLogin && !!memberAuthStore.member?.memId)
const memberId = computed(() => memberAuthStore.member?.memId)
const memberName = computed(() => memberAuthStore.member?.memName || '訪客')
const memberUserName = computed(() => memberAuthStore.member?.memUsername || '訪客')

// --- 狀態定義 ---
const seats = ref([])
const selectedSpot = ref(null)
const activeRent = ref(null) // [新增] 儲存目前進行中的訂單
const selectedSeat = ref(null)
const isLoading = ref({
  spot: false,
  seats: false,
  rent: false,
})
const errorMessage = ref('')

// 對話框狀態
const showTermsModal = ref(false)
const agreedToTerms = ref(false)

// --- API 呼叫 ---
const loadSpotInfo = async (spotId) => {
  if (!spotId) return
  isLoading.value.spot = true
  try {
    const response = await axios.get(`http://localhost:8080/spot/${spotId}`)
    selectedSpot.value = response.data
  } catch (error) {
    console.error(`載入站點 ${spotId} 失敗:`, error)
    errorMessage.value = '無法載入站點資料。'
  } finally {
    isLoading.value.spot = false
  }
}

// --- 核心邏輯 ---
const goToSearchSpot = () => {
  router.push('/SearchSpot')
}

// [步驟二] 導向至 ECpay 付款頁面
// Note: 這個方法應該在後端訂單資料更新後 (recStatus設為'未付款') 才被呼叫
const goToPayment = () => {
  if (!isReadyToRent.value) return

  const recId = activeRent.value?.recId
  if (!recId) {
    errorMessage.value = '無法找到訂單ID，無法進行付款。'
    console.error('訂單物件缺少 recId:', activeRent.value)
    return
  }

  router.push({
    name: 'payment-order',
    returnSpotId: selectedSpot.value?.spotId,
    query: {
      recId: recId,
      total: rentCalculation.value.totalFee,
    },
  })
}

const openTermsModal = () => {
  if (isReadyToRent.value) {
    showTermsModal.value = true
  }
}
const closeModal = () => {
  showTermsModal.value = false
  agreedToTerms.value = false // 關閉時重置勾選
}

// --- 問題回報處理 ---
const handleReport = () => {
  const rent = activeRent.value
  // 準備攜帶至回報頁面的資料
  const reportData = {
    orderId: rent?.recSeqId, // 訂單 ID
    memberId: memberAuthStore.member?.memId, // 使用者 ID
    spotId: selectedSpot.value?.spotId || rent?.spotIdRent, // 站點 ID
  }
  // TODO: REPORT - 暫時以 Alert 代替實際路由跳轉
  alert(
    `TODO: REPORT\n準備前往問題回報頁面\n訂單ID: ${reportData.orderId}\n會員ID: ${reportData.memberId}\n站點ID: ${reportData.spotId}`,
  )
}

// [步驟一] 點擊付款後，先更新後端訂單資料
// 將歸還站點(spotIdReturn)與費用(recPayment)寫入，並將狀態更新為'未付款'
// 成功後才會導向至付款頁面
const proceedWithRent = async () => {
  // 防呆檢查：確保會員 ID 存在
  if (!memberAuthStore.member?.memId) {
    console.warn('無法獲取會員資訊(memId遺失)，自動導向登入頁')
    memberAuthStore.clearMemberLogin()
    router.push({
      name: 'login',
      query: { redirect: router.currentRoute.value.fullPath },
    })
    return
  }

  // 準備更新資料：先將歸還資訊寫入，狀態設為「未付款」
  // 這樣後端 PaymentApiController 在付款成功後，才能讀取到 spotIdReturn 並執行座位更新
  const rentalData = {
    recStatus: '未付款', // 標記狀態，等待金流更新為「已完成」
    recInvoice: generateInvoiceNumber(), // [修正] 預設發票號碼
    spotIdReturn: selectedSpot.value?.spotId, // [修正] 使用 Optional Chaining 避免報錯
    recPayment: rentCalculation.value.totalFee,
  }

  // [新增] 防呆檢查：如果沒有抓到站點 ID，禁止送出並報錯
  console.log('準備送出歸還更新資料:', rentalData)
  if (!rentalData.spotIdReturn) {
    alert('系統錯誤：無法讀取歸還站點 ID (spotIdReturn)，請重新選擇站點或聯繫管理員。')
    console.error('錯誤：selectedSpot 物件內容:', selectedSpot.value)
    return
  }

  isLoading.value.rent = true
  try {
    // [修正] 使用 PUT 方法呼叫 /rec-rent/{recId} 進行更新
    const recId = activeRent.value.recId
    if (!recId) throw new Error('找不到訂單編號')

    // [修正] 將 spotIdReturn 同時放在 URL Query String 中，確保後端一定能收到
    const response = await axios.put(
      `http://localhost:8080/rec-rent/${recId}?spotIdReturn=${rentalData.spotIdReturn}`,
      rentalData,
    )

    if (response.status === 200) {
      // 資料更新成功後，呼叫 goToPayment 進入付款流程
      // 注意：這裡不需要再 router.push，因為 goToPayment 會處理
      activeRent.value = response.data // 更新本地資料
      goToPayment()
    } else {
      errorMessage.value = '歸還失敗，請稍後再試。'
    }
  } catch (error) {
    console.error('歸還請求失敗:', error)
    errorMessage.value = `歸還失敗: ${error.response?.data?.message || error.message}`
  } finally {
    isLoading.value.rent = false
    closeModal()
  }
}

// --- Computed ---

const isStep1Completed = computed(() => !!selectedSpot.value)

const step1Class = computed(() => (isStep1Completed.value ? 'status-completed' : 'status-pending'))

const isReadyToRent = computed(() => isStep1Completed.value && isLoggedIn.value)

// --- [新增] 費用與時間計算 ---
const rentCalculation = computed(() => {
  if (!activeRent.value) {
    return { rentTime: '-', returnTime: '-', duration: 0, totalFee: 0 }
  }

  // 1. 取得時間
  const rentDate = new Date(activeRent.value.recRentDT || activeRent.value.recRentDT2)
  const returnDate = new Date() // 當下時間

  // 2. 計算使用時間 (分鐘)
  const diffMs = returnDate - rentDate
  const durationMinutes = Math.floor(diffMs / (1000 * 60))

  // 3. 計算費用: 使用時間/30 取整後 + 20
  // 註：若您的費率是 "每30分鐘30元"，公式應為 Math.floor(durationMinutes / 30) * 30 + 20
  // 這裡依照您的指示 "使用時間/30 取整後+20" 實作
  const totalFee = Math.floor(durationMinutes / 30) * 30 + 45

  // 4. 格式化時間顯示 (YYYY/MM/DD HH:mm:ss)
  const formatTime = (date) => {
    return date.toLocaleString('zh-TW', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    })
  }

  return {
    rentTime: formatTime(rentDate),
    returnTime: formatTime(returnDate),
    duration: durationMinutes,
    totalFee: totalFee > 0 ? totalFee : 20, // 確保最低費用
  }
})
// 生成發票號碼 (格式: 兩位大寫英文 - 八位數字)
const generateInvoiceNumber = () => {
  // 產生兩個隨機大寫英文字母 (ASCII 65-90)
  const letters =
    String.fromCharCode(65 + Math.floor(Math.random() * 26)) +
    String.fromCharCode(65 + Math.floor(Math.random() * 26))
  // 產生八位隨機數字，不足補零
  const numbers = Math.floor(Math.random() * 100000000)
    .toString()
    .padStart(8, '0')
  return `${letters}-${numbers}`
}
// --- 測試功能：快速歸還 --- 在訂單問題前的BTN
// const handleQuickReturnTest = async () => {
//   if (!activeRent.value) return
//   if (!confirm(`[測試功能] 確定要強制歸還訂單 ${activeRent.value.recId} 嗎？`)) return
//   //Date()轉換LocalDateTime()
//   const d = new Date()
//   const localDate = new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, -1)
//   try {
//     const testReturnData = {
//       memId: memberId.value,
//       seatsId: activeRent.value.seatsId,
//       spotIdRent: activeRent.value.spotIdRent,
//       spotIdReturn: selectedSpot.value?.spotId, // 若未選站點則使用原站點
//       recRentDT2: activeRent.value.recRentDT2 || activeRent.value.recRentDT,
//       recReturnDT2: localDate, //轉換LocalDateTime()
//       recUsageDT2: rentCalculation.value.duration,
//       recStatus: '已完成',
//       recPrice: rentCalculation.value.totalFee, // 價格或費率可由後端根據座位類型和站點決定
//       recRequestPay: 0, // 因為無需確認付款，所以請求付款金額為 0
//       recPayment: 0, // 同上，實際付款為 0
//       recPayBy: '信用卡', //
//       recInvoice: generateInvoiceNumber(), // 歸還時生成發票號碼
//       recCarrier: memberAuthStore.member.memInvoice,
//       recViolatInt: 0, //
//     }
//     // [修改] 增加訂單ID的回退(fallback)機制與防呆，避免產生 undefined 的 API 請求
//     const updateId = activeRent.value.recId //|| activeRent.value.recId
//     if (!updateId) {
//       alert('錯誤：無法找到訂單 ID，無法完成測試歸還。')
//       console.error('訂單物件:', activeRent.value)
//       return
//     }
//     await axios.put(`http://localhost:8080/rec-rent/${updateId}`, testReturnData)
//     alert('測試歸還成功，將導向紀錄頁面。')
//     router.push({ name: 'rec-rent-user', params: { action: 'record' } })
//   } catch (error) {
//     console.error('測試歸還失敗:', error)
//     alert('測試歸還失敗，請檢查後端或網路。')
//   }
// }

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
// --- Watchers ---
watch(selectedSpot, (newSpot) => {
  if (newSpot) {
    // 歸還時可能不需要重載座位，或者需要載入正在使用的座位
    // loadSeats(newSpot.spotId);
  } else {
    seats.value = []
    selectedSeat.value = null
  }
})

var recId="";

onMounted(async () => {
  // Debug: 確認進入頁面時的狀態
  console.log('會員ID:', memberId.value, '登入狀態:', memberAuthStore.isLogin)

  // 如果進入頁面時發現是登入狀態但沒有 ID，導回登入頁
  if (memberAuthStore.isLogin && !memberId.value) {
    console.warn('歸還頁面偵測到資料遺失，自動導向登入頁')
    memberAuthStore.clearMemberLogin() // 確保清除異常狀態
    router.push({
      name: 'login',
      query: { redirect: router.currentRoute.value.fullPath },
    })
    return
  }

  // 檢查是否有進行中的訂單 (recStatus === '租借中')
  if (memberAuthStore.isLogin && memberId.value) {
    try {
      const res = await axios.get(`http://localhost:8080/rec-rent?memId=${memberId.value}`)
      // [修改] 找到該筆訂單並存入 activeRent
      const foundRent = res.data.find(
        (rent) => rent.recStatus === '租借中' || rent.recStatus === '未付款',
      )

      if (!foundRent) {
        alert('您目前沒有租借中的訂單，無法進行歸還。\n將為您導向至租借紀錄頁面。')
        router.push({ name: 'rec-rent-user', params: { action: 'record' } })
        return
      } else {
        // // --- DEBUG --- 印出找到的訂單物件，以確認 recSeqId 屬性是否存在及其值
        // console.log('訂單Id 為:', foundRent?.recId)
        // console.log('seatsId 為:', foundRent?.seatsId)
        // console.log('spotReturnId 為:', memberAuthStore.selectedSpotId)
        activeRent.value = foundRent
        recId=activeRent.value.recId;
  
      }
    } catch (error) {
      console.error('檢查租借狀態失敗:', error)
    }
  }

  // 載入指定的站點資訊 (從 Pinia 或 URL 參數)
  const spotId = memberAuthStore.selectedSpotId || route.query.spotId
  if (spotId) {
    loadSpotInfo(spotId)
  }
})
</script>

<template>
  <div class="user-order-container">
    <div class="card">
      <h2 class="card-header">歸還座位並結算</h2>

      <!-- 會員資訊顯示區塊 -->
      <div class="card-body user-info-section">
        <div v-if="isLoggedIn">
          <h5 class="mb-0">
            您好: <strong>{{ memberName }}</strong> (UID: {{ memberUserName }})，請確認您的歸還資訊:<br><br>
          <strong>訂單編號: {{ recId }}</strong>
          </h5>
        </div>
        <div v-else class="alert alert-warning">
          <h5><i class="fas fa-exclamation-triangle"></i> 訪客你好</h5>
          <p class="mb-0">請先登入以進行歸還結算。</p>
        </div>
      </div>

      <div v-if="errorMessage" class="alert alert-danger m-3">{{ errorMessage }}</div>

      <!-- 步驟一: 選擇歸還站點 -->
      <div class="card-body" :class="step1Class">
        <h2><i class="fas fa-store"></i> 步驟一：確認歸還站點</h2>
        <fieldset :disabled="!isLoggedIn">
          <div v-if="isLoading.spot" class="text-center">
            <span class="spinner-border spinner-border-sm"></span> 正在載入站點資訊...
          </div>
          <div v-else-if="selectedSpot" class="d-flex justify-content-between align-items-center">
            <div>
              <h4 class="mb-1">{{ selectedSpot.spotName }}</h4>
              <p class="text-muted mb-0">
                <i class="fas fa-map-marker-alt"></i> {{ selectedSpot.spotAddress }}
              </p>
            </div>
            <button class="btn btn-primary fw-bold" @click="goToSearchSpot">
              <i class="fas fa-exchange-alt"></i> 重新選擇站點
            </button>
          </div>
          <div v-else class="alert alert-warning d-flex justify-content-between align-items-center">
            <span>尚未選擇歸還站點，請先至地圖選擇。</span>
            <button class="btn btn-primary" @click="goToSearchSpot">前往地圖</button>
          </div>
        </fieldset>
      </div>

      <!-- 步驟2: 確認費用與付款 -->
      <div class="card-body">
        <fieldset :disabled="!isStep1Completed || !isLoggedIn">
          <div class="d-flex justify-content-between align-items-center mb-3">
            <h2 class="mb-0"><i class="fas fa-credit-card"></i> 步驟二：確認使用資訊</h2>
          </div>
          <h5>租借時間: {{ rentCalculation.rentTime }}</h5>
          <h5>歸還時間: {{ rentCalculation.returnTime }}</h5>
          <h5>使用時間: {{ rentCalculation.duration }} 分鐘</h5>
          <h5>費率:前半小時 45 NTD，半小時後 30 NTD / 30 min</h5>
          <hr />
          <h3>費用總計: {{ rentCalculation.totalFee }} NTD</h3>
          <div class="d-flex justify-content-between align-items-center">
            <button
              @click="proceedWithRent"
              class="btn btn-success btn-lg"
              :disabled="!isReadyToRent"
            >
              {{ isReadyToRent ? '前往付款' : '請完成歸還步驟' }}
            </button>
            <div>
              <!-- <button class="btn btn-outline-danger fw-bold me-2" @click="handleQuickReturnTest">
                <i class="fas fa-bug"></i> 測試歸還
              </button> -->
              <button class="btn btn-warning fw-bold" @click="handleReportIssue">
                <i class="fas fa-exclamation-circle"></i> 我有訂單問題!
              </button>
            </div>
          </div>
        </fieldset>
      </div>
    </div>
  </div>
</template>
<style scoped>
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css');

.user-order-container {
  padding: 20px;
  max-width: 800px;
  margin: auto;
  font-family: 'Microsoft JhengHei', sans-serif;
}
.card {
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transition: background-color 0.5s ease;
}
.card-header {
  background-color: #28a745;
  color: white;
  padding: 15px 20px;
  margin: 0;
  text-align: center;
}
.card-body {
  padding: 20px;
  border-bottom: 1px solid #eee;
  transition: background-color 0.3s ease-in-out;
}
.user-info-section {
  background-color: #e9ecef;
}
.card-body:last-child {
  border-bottom: none;
}

.status-pending {
  color: black;
  background-color: #ffffff;
}
.status-completed {
  background-color: #c1ffbe;
}

fieldset:disabled {
  opacity: 0.5;
  pointer-events: none;
}
h2 {
  color: #333;
  border-bottom: 2px solid #28a745;
  padding-bottom: 10px;
  margin-bottom: 20px;
}
.form-group {
  margin-bottom: 1rem;
}
.form-control {
  display: block;
  width: 100%;
  padding: 0.375rem 0.75rem;
  font-size: 1rem;
  line-height: 1.5;
  color: #495057;
  background-color: #fff;
  background-clip: padding-box;
  border: 1px solid #ced4da;
  border-radius: 0.25rem;
  transition:
    border-color 0.15s ease-in-out,
    box-shadow 0.15s ease-in-out;
}
.btn {
  font-size: 1rem;
  padding: 10px 15px;
  border-radius: 5px;
  cursor: pointer;
  text-align: center;
  text-decoration: none;
  display: inline-block;
  border: none;
}
.btn-primary {
  color: #fff;
  background-color: #007bff;
}
.btn-success {
  color: #fff;
  background-color: #28a745;
}
.btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}
.btn-lg {
  padding: 15px 25px;
  font-size: 1.25rem;
}
.alert {
  padding: 15px;
  margin-bottom: 20px;
  border: 1px solid transparent;
  border-radius: 4px;
}
.alert-danger {
  color: #721c24;
  background-color: #f8d7da;
  border-color: #f5c6cb;
}
.alert-info {
  color: #0c5460;
  background-color: #d1ecf1;
  border-color: #bee5eb;
}
.alert-warning {
  color: #856404;
  background-color: #fff3cd;
  border-color: #ffeeba;
}
.m-3 {
  margin: 1rem;
}
.mt-3 {
  margin-top: 1rem;
}
.text-muted {
  color: #6c757d !important;
}
.text-center {
  text-align: center;
}

.spinner-border-sm {
  width: 1rem;
  height: 1rem;
  border-width: 0.2em;
}
.spinner-border {
  display: inline-block;
  width: 2rem;
  height: 2rem;
  vertical-align: text-bottom;
  border: 0.25em solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  -webkit-animation: spinner-border 0.75s linear infinite;
  animation: spinner-border 0.75s linear infinite;
}
@keyframes spinner-border {
  to {
    transform: rotate(360deg);
  }
}
/* Modal styles */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 1050;
  width: 100vw;
  height: 100vh;
  background-color: #000;
  opacity: 0.5;
}
.modal.show {
  display: block;
}
.modal {
  z-index: 1055;
}
</style>
