<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
import { useMemberAuthStore } from '@/stores/memberAuth'

// --- Computed properties from store ---
// --- Props ---
const props = defineProps({
  spotId: {
    type: String,
    required: true,
  },
})

// --- Pinia Store ---
const memberAuthStore = useMemberAuthStore()
const router = useRouter()

// --- Computed properties from store ---
const isLoggedIn = computed(() => memberAuthStore.isLogin && !!memberAuthStore.member?.memId)
const memberId = computed(() => memberAuthStore.member?.memId)
const memberName = computed(() => memberAuthStore.member?.memName || '訪客')
const memberUserName = computed(() => memberAuthStore.member?.memUsername || '訪客')
console.log(isLoggedIn.value)

// --- 狀態定義 ---
const spotInfo = ref(null) // 儲存傳入 spotId 的站點資訊
const seats = ref([])
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
// --- Lifecycle ---
onMounted(async () => {
  // 如果進入頁面時發現是登入狀態但沒有 ID，導回登入頁
  if (memberAuthStore.isLogin && !memberId.value) {
    console.warn('訂單頁面偵測到資料遺失，自動導向登入頁')
    memberAuthStore.clearMemberLogin() // 確保清除異常狀態
    router.push('/login')
    return
  }

  // 檢查是否有未完成的訂單 (recStatus === '租借中')
  if (memberAuthStore.isLogin && memberId.value) {
    try {
      const res = await axios.get(`http://localhost:8080/rec-rent?memId=${memberId.value}`)
      const hasActiveRent = res.data.some(
        (rent) => rent.recStatus === '租借中' || rent.recStatus === '未付款',
      )

      if (hasActiveRent) {
        alert('您尚有未完成的租借訂單。\n請先完成歸還或洽詢客服人員。')
        router.push({ name: 'rec-rent-user', params: { action: 'record' } })
      }
    } catch (error) {
      console.error('檢查租借狀態失敗:', error)
    }
  }
})
// --- API 呼叫 ---

const loadSpotInfo = async (spotId) => {
  if (!spotId) return
  isLoading.value.spot = true
  spotInfo.value = null // 重置舊資料
  try {
    // API 端點 `/spot/{id}`
    const response = await axios.get(`/spot/${spotId}`)
    spotInfo.value = response.data
  } catch (error) {
    console.error(`載入站點 ${spotId} 資訊失敗:`, error)
    errorMessage.value = '無法載入站點資料。'
  } finally {
    isLoading.value.spot = false
  }
}

const loadSeats = async (spotId) => {
  if (!spotId) return
  isLoading.value.seats = true
  seats.value = []
  selectedSeat.value = null
  try {
    const response = await axios.get(`http://localhost:8080/seats/search?spotId=${spotId}`)
    // 根據需求，只顯示狀態為"啟用"的座位
    seats.value = response.data.filter((seat) => seat.seatsStatus === '啟用')
  } catch (error) {
    console.error(`載入 ${spotId} 的座位失敗:`, error)
    errorMessage.value = '無法載入該站點的座位資訊。'
  } finally {
    isLoading.value.seats = false
  }
}

// --- 核心邏輯 ---

const openTermsModal = () => {
  if (isReadyToRent.value) {
    showTermsModal.value = true
  }
}

const closeModal = () => {
  showTermsModal.value = false
  agreedToTerms.value = false // 關閉時重置勾選
}

const proceedWithRent = async () => {
  // 檢查是否同意條款
  if (!agreedToTerms.value) {
    alert('請同意使用條款。')
    return
  }
  // 檢查所有步驟是否完成
  if (!isReadyToRent.value) {
    alert('請完成所有租借步驟。')
    return
  }

  // 防呆檢查：確保會員 ID 存在 (防止登入狀態異常導致 memId 為空)
  if (!memberAuthStore.member?.memId) {
    console.warn('無法獲取會員資訊(memId遺失)，自動導向登入頁')
    memberAuthStore.clearMemberLogin()
    router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }

  //Date()轉換LocalDateTime()
  const d = new Date()
  const localDate = new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, -1)
  // 根據使用者要求組合新的訂單資料
  const newOrderData = {
    memId: memberAuthStore.member.memId, // 直接從 Pinia Store 獲取 USERID
    seatsId: selectedSeat.value.seatsId,
    spotIdRent: parseInt(props.spotId), // 從 props 取得 spotId
    recRentDT2: localDate, // 紀錄當下時間 (ISO 8601 格式)
    recStatus: '租借中',
    recPrice: 0, // 價格或費率可由後端根據座位類型和站點決定
    recRequestPay: 0, // 因為無需確認付款，所以請求付款金額為 0
    recPayment: 0, // 同上，實際付款為 0
    recPayBy: '尚未付款', //
    recInvoice: '', // 歸還時生成發票號碼
    recCarrier: memberAuthStore.member.memInvoice,
    recViolatInt: 0, //
  }

  isLoading.value.rent = true
  errorMessage.value = '' // 清除舊的錯誤訊息

  try {
    const response = await axios.post('http://localhost:8080/rec-rent/new', newOrderData)

    if (response.status === 201 || response.status === 200) {
      console.log('訂單成功產生:', response.data)
      alert(
        `租借成功！\n訂單ID：${response.data.recSeqId}\n站點：${spotInfo.value.spotName}\n座位：${selectedSeat.value.seatsId}`,
      )
      // 重置流程，也許導航到使用者訂單頁面
      selectedSeat.value = null
      agreedToTerms.value = false
      router.push({ name: 'rec-rent-user', params: { action: 'record' } })
    } else {
      errorMessage.value = `租借失敗，伺服器回應狀態碼: ${response.status}`
    }
  } catch (error) {
    console.error('租借請求失敗:', error)
    const apiError = error.response?.data?.message || error.message
    errorMessage.value = `租借請求失敗: ${apiError}`
    alert(`租借失敗: ${apiError}`) // 直接彈出錯誤給使用者
  } finally {
    isLoading.value.rent = false
    closeModal() // 無論成功失敗都關閉對話框
  }
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

// --- Computed ---

const isStep1Completed = computed(() => !!selectedSeat.value)
const step1Class = computed(() => (isStep1Completed.value ? 'status-completed' : 'status-pending'))

const isReadyToRent = computed(() => isStep1Completed.value && isLoggedIn.value)

// --- Watchers & Lifecycle ---
watch(
  () => props.spotId,
  (newSpotId) => {
    if (newSpotId) {
      errorMessage.value = ''
      loadSpotInfo(newSpotId)
      loadSeats(newSpotId)
    }
  },
  { immediate: true },
)
</script>

<template>
  <div class="user-order-container">
    <div class="card">
      <h1 class="card-header">即時座位租借</h1>

      <!-- 會員資訊顯示區塊 -->
      <div class="card-body user-info-section">
        <div v-if="isLoggedIn">
          <h5><i class="fas fa-user-check"></i> 會員資訊</h5>
          <h5 class="mb-0">
            歡迎， <strong>{{ memberName }}</strong> (UID:
            {{ memberUserName }})，請確認您的租借資訊:<br /><br />
          </h5>
        </div>
        <div v-else class="alert alert-warning">
          <h5><i class="fas fa-exclamation-triangle"></i></h5>
          <p class="mb-0">請先登入以進行租借。</p>
        </div>
      </div>

      <div v-if="errorMessage" class="alert alert-danger m-3">{{ errorMessage }}</div>

      <!-- 站點資訊-->
      <div class="card-body">
        <h2><i class="fas fa-store"></i> 租借站點</h2>
        <div v-if="isLoading.spot" class="text-center">
          <span class="spinner-border spinner-border-sm"></span> 正在載入站點資訊...
        </div>
        <div v-else-if="spotInfo">
          <h4>{{ spotInfo.spotName }}</h4>
          <p class="text-muted">{{ spotInfo.spotAddress }}</p>
        </div>
      </div>

      <!-- 步驟一: 選擇座位 -->
      <div class="card-body" :class="step1Class">
        <fieldset :disabled="!isLoggedIn || !spotInfo">
          <h2><i class="fas fa-chair"></i> 請選擇座椅</h2>
          <div v-if="isLoading.seats" class="text-center">
            <span class="spinner-border spinner-border-sm"></span> 正在載入座位...
          </div>
          <div v-else-if="seats.length > 0" class="form-group">
            <label for="seat-select">請選擇座椅：</label>
            <select id="seat-select" class="form-control" v-model="selectedSeat">
              <option :value="null" disabled>-- 請選擇一個座椅 --</option>
              <option v-for="seat in seats" :key="seat.seatsId" :value="seat">
                座椅編號: {{ seat.seatsId }} &nbsp;&nbsp;&nbsp; 類型: {{ seat.seatsName }}
              </option>
            </select>
          </div>
          <div v-else-if="spotInfo" class="alert alert-warning">
            此站點目前無可用座位。
            <br />
            <router-link to="/SearchSpot" class="btn btn-primary mt-2"
              >返回地圖重新選擇</router-link
            >
          </div>
        </fieldset>
      </div>

      <!-- 步驟二: 確認租借 -->
      <div class="card-body">
        <fieldset :disabled="!isStep1Completed || !isLoggedIn">
          <h2><i class="fas fa-check-circle"></i> 確認使用條款後租借</h2>
          <p>請確認您的選擇，點擊下方按鈕以同意使用條款並完成租借。</p>
          <h4>
            費率說明:<br />
            前半小時 45 NTD，半小時後 30 NTD / 30 min
          </h4>
          <br />
          <div class="d-flex justify-content-between align-items-center">
            <button
              @click="openTermsModal"
              class="btn btn-success btn-lg"
              :disabled="!isReadyToRent"
            >
              {{ isReadyToRent ? '閱讀使用條款後租借' : '請先完成前置步驟' }}
            </button>
            <button class="btn btn-warning fw-bold" @click="handleReportIssue">
              <i class="fas fa-exclamation-circle"></i> 我有訂單問題!
            </button>
          </div>
        </fieldset>
      </div>
    </div>

    <!-- 使用條款 Modal -->
    <div v-if="showTermsModal" class="modal-backdrop fade show" style="width: 600px"></div>
    <div
      class="modal fade"
      :class="{ show: showTermsModal }"
      :style="{ display: showTermsModal ? 'block' : 'none' }"
      tabindex="-1"
      role="dialog"
    >
      <div class="modal-dialog modal-dialog-scrollable">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">【重要租借告知】</h5>
            <button type="button" class="btn-close" @click="closeModal"></button>
          </div>
          <div class="modal-body">
            <span>在您點擊租借前，請先確認以下租借及使用注意事項：</span><br /><br />
            <span style="font-weight: bolder">計費方式：</span>
            <span>前 30 分鐘 NT$45，之後每 30 分鐘 NT$30</span><br />
            <span>(不足 30 分鐘以 30 分鐘計)。 </span><br />
            <span style="font-weight: bolder">安全承諾：</span>
            <span style="color: red">座椅載重上限為 85kg，僅供椅坐，禁止危險動作或超載。</span
            ><br />
            <span>超過限制或不當使用導致之損害由使用者自負。</span><br />
            <span style="font-weight: bolder">檢查設備：</span>
            <span>使用前請檢查結構，如有異常請於 10分鐘內回報。</span><br />
            <span style="font-weight: bolder">正確歸還：</span>
            <span>務必確認App顯示「租借結束訂單」，否則將持續計費。</span><br />
            <span style="font-weight: bolder">賠償責任： </span>
            <span>若設備遺失或人為損毀，最高需負擔賠償金 NT$1500。</span><br />
            <span style="font-weight: bolder">服務細則： </span>
            <span>閱讀並同意上述條款視同接受我們的詳細服務細則。</span>
            <hr />
            <div class="form-check">
              <input
                class="form-check-input"
                type="checkbox"
                id="terms-agree"
                v-model="agreedToTerms"
              />
              <label class="form-check-label" for="terms-agree">
                我已閱讀並同意以上使用條款。
              </label>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="closeModal">取消</button>
            <button
              type="button"
              class="btn btn-primary"
              :disabled="!agreedToTerms || isLoading.rent"
              @click="proceedWithRent"
            >
              <span v-if="isLoading.rent" class="spinner-border spinner-border-sm"></span>
              確認租借
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css');

.user-order-container {
  padding: 0px;
  max-width: 600px;
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
  background-color: #007bff;
  color: rgb(0, 0, 0);
  padding: 15px 20px;
  margin: 0;
  text-align: center;
}
.card-body {
  padding: 20px;
  border-bottom: 1px solid #eee;
  transition: background-color 0.3s ease-in-out;
}
.card-body:last-child {
  border-bottom: none;
}
.user-info-section {
  background-color: #e9ecef;
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
  border-bottom: 2px solid #007bff;
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
