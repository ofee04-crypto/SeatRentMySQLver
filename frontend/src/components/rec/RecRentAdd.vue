<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const memberList = ref([])
const spotList = ref([])
const seatList = ref([]) // [新增] 座位列表

// [新增] 下拉選單顯示狀態
const showMemId = ref(false)
const showMemName = ref(false)
const showSpotRentId = ref(false)
const showSpotRentName = ref(false)
const showSpotReturnId = ref(false)
const showSpotReturnName = ref(false)
const showSeatId = ref(false)

const fetchMembers = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/members')
    memberList.value = res.data
  } catch (err) {
    console.error('無法載入會員列表:', err)
  }
}

const fetchSpots = async () => {
  try {
    const res = await axios.get('http://localhost:8080/spot/list')
    spotList.value = res.data
  } catch (err) {
    console.error('無法載入據點列表:', err)
  }
}

// [新增] 載入座位列表
const fetchSeats = async () => {
  try {
    const res = await axios.get('http://localhost:8080/seats')
    seatList.value = res.data
  } catch (err) {
    console.error('無法載入座位列表:', err)
  }
}

// 1. 定義 emit 事件
const emit = defineEmits(['save-rent', 'cancel'])

// 2. 創建一個空的初始表單物件的函式
const createInitialForm = () => ({
  recSeqId: null,
  // recId: '',
  memId: '',
  memName: '',
  seatsId: '',
  recStatus: '租借中',
  recStatus: '租借中',
  spotIdRent: '',
  spotRentName: '', // [新增] 用於顯示/輸入
  spotIdReturn: '',
  spotReturnName: '', // [新增] 用於顯示/輸入
  recRentDT2: '',
  recReturnDT2: '',
  recViolatInt: 0,
  recNote: '', // [修正] 補上備註欄位的初始化
})

// 3. 使用 ref 來儲存表單資料
const form = ref(createInitialForm())
const formTitle = '新增訂單' // 標題是固定的

// 連動邏輯

// [新增] 過濾邏輯 (Computed)
import { computed } from 'vue'

const filteredMemIds = computed(() => {
  if (!form.value.memId) return memberList.value
  return memberList.value.filter((m) => String(m.memId).includes(String(form.value.memId)))
})

const filteredMemNames = computed(() => {
  if (!form.value.memName) return memberList.value
  return memberList.value.filter((m) => m.memName.includes(form.value.memName))
})

const filteredSpotRentIds = computed(() => {
  if (!form.value.spotIdRent) return spotList.value
  return spotList.value.filter((s) => String(s.spotId).includes(String(form.value.spotIdRent)))
})

const filteredSpotRentNames = computed(() => {
  if (!form.value.spotRentName) return spotList.value
  return spotList.value.filter((s) => s.spotName.includes(form.value.spotRentName))
})

const filteredSpotReturnIds = computed(() => {
  if (!form.value.spotIdReturn) return spotList.value
  return spotList.value.filter((s) => String(s.spotId).includes(String(form.value.spotIdReturn)))
})

const filteredSpotReturnNames = computed(() => {
  if (!form.value.spotReturnName) return spotList.value
  return spotList.value.filter((s) => s.spotName.includes(form.value.spotReturnName))
})

// [新增] 可用座位過濾：需符合「所在站點 = 租借站點」且「狀態 = 可使用(或類似狀態)」
// 若後端座位狀態 definition: '可用', '使用中', '維修中'
const filteredSeats = computed(() => {
  let list = seatList.value

  // 1. 先篩選出屬於「目前租借站點」的座位
  // if (form.value.spotIdRent) {
  //   list = list.filter(s => s.spotId == form.value.spotIdRent)
  // }

  // 2. 再根據輸入的 seatsId 進行模糊搜尋 (包含 ID, 名稱, 序號)
  if (form.value.seatsId) {
    const term = String(form.value.seatsId).toLowerCase()
    list = list.filter((s) => {
      const id = String(s.seatsId).toLowerCase()
      const name = (s.seatsName || '').toLowerCase()
      const serial = (s.serialNumber || '').toLowerCase()
      return id.includes(term) || name.includes(term) || serial.includes(term)
    })
  }

  // 3. 過濾掉非「可租借」狀態的座位 (可選)
  list = list.filter((s) => s.seatsStatus === '啟用')

  return list
})

// [新增] 選擇處理函式
const selectMember = (member) => {
  form.value.memId = member.memId
  form.value.memName = member.memName
  showMemId.value = false
  showMemName.value = false
}

const selectSpotRent = (spot) => {
  form.value.spotIdRent = spot.spotId
  form.value.spotRentName = spot.spotName
  showSpotRentId.value = false
  showSpotRentName.value = false
}

const selectSpotReturn = (spot) => {
  form.value.spotIdReturn = spot.spotId
  form.value.spotReturnName = spot.spotName
  showSpotReturnId.value = false
  showSpotReturnName.value = false
}

const selectSeat = (seat) => {
  form.value.seatsId = seat.seatsId // 注意型別轉換若需要
  showSeatId.value = false
}

// 延遲隱藏 (避免點擊選項前 input blur 導致選單消失)
const delayHide = (refVar) => {
  setTimeout(() => {
    refVar.value = false
  }, 200)
}

// [新增] 手動切換下拉選單顯示
const toggleDropdown = (refVar) => {
  refVar.value = !refVar.value
}

onMounted(() => {
  fetchMembers()
  fetchSpots()
  fetchSeats()
})

// 4. 清空/重設表單的函式
const resetForm = () => {
  form.value = createInitialForm()
}

// 5. 提交表單的處理函式
const saveRent = () => {
  const dataToSend = {
    ...form.value,
    recRentDT2: form.value.recRentDT2 || null, // 保持 ISO 格式 (含 'T')，後端才能正確解析
    recReturnDT2: form.value.recReturnDT2 || null, // 保持 ISO 格式 (含 'T')
  }
  emit('save-rent', dataToSend)
  // 成功提交後通常會切換視圖，但如果需要留在原頁面，可以取消註解下一行
  // resetForm();
}

// 6. 取消操作 (返回列表)
const handleCancel = () => {
  emit('cancel')
}

// [新增] 一鍵輸入預設值函式
const fillDefaultValues = () => {
  const now = new Date()

  // 封裝格式化邏輯：將 Date 物件轉換為 datetime-local 所需的 YYYY-MM-DDTHH:mm:ss 格式
  const toLocalISOString = (date) => {
    const offset = date.getTimezoneOffset() * 60000
    return new Date(date.getTime() - offset).toISOString().slice(0, 19)
  }

  // 計算昨天 (語意更清楚的寫法)
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)

  form.value = {
    recSeqId: null,
    // recId: 'TEST-' + Math.floor(Math.random() * 10000), // 隨機產生訂單編號
    memId: 12,
    memName: '李宗翰',
    seatsId: '100',
    recStatus: '租借中',
    spotIdRent: 1,
    spotRentName: '桃園高鐵站',
    spotIdReturn: 2,
    spotReturnName: '中壢火車站',
    recRentDT2: toLocalISOString(yesterday),
    recReturnDT2: toLocalISOString(now),
    recViolatInt: 2,
    recNote: '測試資料-系統自動帶入',
  }
}
</script>

<template>
  <div class="view-section form-section active">
    <!-- [修改] 標題區塊加入一鍵輸入按鈕 -->
    <div class="form-header">
      <h2>{{ formTitle }}</h2>
      <button class="btn-warning btn-sm" @click="fillDefaultValues">一鍵輸入</button>
    </div>

    <!-- <div class="form-group">
      <label>訂單編號(recId):</label>
      <input v-model="form.recId" type="text" placeholder="必須..." />
    </div> -->
    <div class="form-group relative">
      <label>會員編號:</label>
      <input
        v-model="form.memId"
        type="number"
        placeholder="必須、數字..."
        required
        @focus="showMemId = true"
        @blur="delayHide(showMemId)"
        @input="showMemId = true"
        @keydown.esc="showMemId = false"
      />
      <span class="toggle-icon" @mousedown.prevent="toggleDropdown(showMemId)">▼</span>
      <ul v-if="showMemId && filteredMemIds.length" class="custom-dropdown">
        <li v-for="m in filteredMemIds" :key="m.memId" @mousedown="selectMember(m)">
          {{ m.memId }} - {{ m.memName }}
        </li>
      </ul>
    </div>
    <div class="form-group relative">
      <label>會員姓名:</label>
      <input
        v-model="form.memName"
        type="text"
        placeholder="或輸入姓名快速搜尋..."
        @focus="showMemName = true"
        @blur="delayHide(showMemName)"
        @input="showMemName = true"
        @keydown.esc="showMemName = false"
      />
      <span class="toggle-icon" @mousedown.prevent="toggleDropdown(showMemName)">▼</span>
      <ul v-if="showMemName && filteredMemNames.length" class="custom-dropdown">
        <li v-for="m in filteredMemNames" :key="m.memId" @mousedown="selectMember(m)">
          {{ m.memId }} - {{ m.memName }}
        </li>
      </ul>
    </div>

    <div class="form-group relative">
      <label>座椅編號:</label>
      <input
        v-model="form.seatsId"
        type="text"
        placeholder="必須..."
        required
        @focus="showSeatId = true"
        @blur="delayHide(showSeatId)"
        @input="showSeatId = true"
        @keydown.esc="showSeatId = false"
      />
      <span class="toggle-icon" @mousedown.prevent="toggleDropdown(showSeatId)">▼</span>
      <ul v-if="showSeatId && filteredSeats.length" class="custom-dropdown">
        <li v-for="s in filteredSeats" :key="s.seatsId" @mousedown="selectSeat(s)">
          {{ s.seatsName }} ( {{ s.serialNumber }} )
        </li>
      </ul>
    </div>

    <div class="form-group">
      <label>訂單狀態:</label>
      <select v-model="form.recStatus">
        <option value="租借中">租借中</option>
        <option value="已完成">已完成</option>
        <option value="未歸還">未歸還</option>
        <option value="已取消">已取消</option>
      </select>
    </div>

    <div class="form-group relative">
      <label>租借站點編號:</label>
      <input
        v-model="form.spotIdRent"
        type="number"
        placeholder="必須、數字..."
        required
        @focus="showSpotRentId = true"
        @blur="delayHide(showSpotRentId)"
        @input="showSpotRentId = true"
        @keydown.esc="showSpotRentId = false"
      />
      <span class="toggle-icon" @mousedown.prevent="toggleDropdown(showSpotRentId)">▼</span>
      <ul v-if="showSpotRentId && filteredSpotRentIds.length" class="custom-dropdown">
        <li v-for="s in filteredSpotRentIds" :key="s.spotId" @mousedown="selectSpotRent(s)">
          {{ s.spotId }} - {{ s.spotName }}
        </li>
      </ul>
    </div>
    <div class="form-group relative">
      <label>租借站點名稱:</label>
      <input
        v-model="form.spotRentName"
        type="text"
        placeholder="或輸入名稱快速搜尋..."
        @focus="showSpotRentName = true"
        @blur="delayHide(showSpotRentName)"
        @input="showSpotRentName = true"
        @keydown.esc="showSpotRentName = false"
      />
      <span class="toggle-icon" @mousedown.prevent="toggleDropdown(showSpotRentName)">▼</span>
      <ul v-if="showSpotRentName && filteredSpotRentNames.length" class="custom-dropdown">
        <li v-for="s in filteredSpotRentNames" :key="s.spotId" @mousedown="selectSpotRent(s)">
          {{ s.spotName }} ({{ s.spotId }})
        </li>
      </ul>
    </div>

    <div class="form-group relative">
      <label>歸還站點編號:</label>
      <input
        v-model="form.spotIdReturn"
        type="number"
        placeholder="數字..."
        @focus="showSpotReturnId = true"
        @blur="delayHide(showSpotReturnId)"
        @input="showSpotReturnId = true"
        @keydown.esc="showSpotReturnId = false"
      />
      <span class="toggle-icon" @mousedown.prevent="toggleDropdown(showSpotReturnId)">▼</span>
      <ul v-if="showSpotReturnId && filteredSpotReturnIds.length" class="custom-dropdown">
        <li v-for="s in filteredSpotReturnIds" :key="s.spotId" @mousedown="selectSpotReturn(s)">
          {{ s.spotId }} - {{ s.spotName }}
        </li>
      </ul>
    </div>
    <div class="form-group relative">
      <label>歸還站點名稱:</label>
      <input
        v-model="form.spotReturnName"
        type="text"
        placeholder="或輸入名稱快速搜尋..."
        @focus="showSpotReturnName = true"
        @blur="delayHide(showSpotReturnName)"
        @input="showSpotReturnName = true"
        @keydown.esc="showSpotReturnName = false"
      />
      <span class="toggle-icon" @mousedown.prevent="toggleDropdown(showSpotReturnName)">▼</span>
      <ul v-if="showSpotReturnName && filteredSpotReturnNames.length" class="custom-dropdown">
        <li v-for="s in filteredSpotReturnNames" :key="s.spotId" @mousedown="selectSpotReturn(s)">
          {{ s.spotName }} ({{ s.spotId }})
        </li>
      </ul>
    </div>
    <div class="form-group">
      <label>租借時間:</label>
      <input v-model="form.recRentDT2" type="datetime-local" step="1" placeholder="必須" required />
    </div>
    <div class="form-group">
      <label>歸還時間:</label>
      <input v-model="form.recReturnDT2" type="datetime-local" step="1" />
    </div>
    <!-- <div class="form-group">
      <label>違規記點:</label>
      <input v-model="form.recViolatInt" type="number" value="0" placeholder="數字..." required />
    </div> -->
    <div class="form-group">
      <label>備註:</label>
      <!-- [修正] 修正綁定錯誤，原本誤綁定到 memName -->
      <input v-model="form.recNote" type="text" placeholder="選填..." />
    </div>

    <div style="text-align: right">
      <button class="btn-info" @click="resetForm">重設欄位</button>
      <button class="btn-secondary" @click="handleCancel">取消</button>
      <button class="btn-primary" @click="saveRent">儲存訂單</button>
    </div>
  </div>
</template>

<style scoped>
/* ========== 表單區塊 - 淺色風格 ========== */
.form-section {
  background: #f5f7fa;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 20px;
}

/* ========== 表單標題區塊 ========== */
.form-header {
  display: flex;
  align-items: center;
  justify-content: space-between; /* 標題在左，按鈕在右，若要按鈕緊鄰標題可改為 flex-start 並加 gap */
  border-bottom: 2px solid #e4e7ed;
  margin-bottom: 20px;
  padding-bottom: 10px;
}

.form-header h2 {
  font-size: 1.3rem;
  font-weight: 600;
  color: #303133;
  margin: 0; /* 移除預設邊距，由 header 控制 */
}

/* ========== 表單組 ========== */
.form-group {
  margin-bottom: 14px;
  display: flex;
  align-items: center;
}

.form-group label {
  width: 180px;
  font-weight: 500;
  font-size: 14px;
  color: #606266;
}

.form-group input,
.form-group select {
  padding: 10px 12px;
  flex: 1;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  font-size: 14px;
  background: #fff;
  transition: all 0.3s ease;
}

.form-group input:focus,
.form-group select:focus {
  border-color: #c0c4cc;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.08);
  outline: none;
}

.form-group input::placeholder {
  color: #c0c4cc;
}

.view-section.active {
  display: block;
}

/* ========== 按鈕樣式 ========== */
button {
  margin-left: 10px;
  padding: 10px 18px;
  cursor: pointer;
  border: none;
  border-radius: 8px;
  font-weight: 500;
  font-size: 14px;
  transition: all 0.3s ease;
}

button:hover {
  transform: translateY(-1px);
}

.btn-primary {
  background: #67c23a;
  color: white;
}

.btn-primary:hover {
  background: #85ce61;
}

.btn-secondary {
  background: #909399;
  color: white;
}

.btn-secondary:hover {
  background: #a6a9ad;
}

.btn-info {
  background: #409eff;
  color: white;
}

.btn-info:hover {
  background: #66b1ff;
}

/* [新增] 小按鈕樣式 */
/* [新增] 自訂下拉選單樣式 */
.relative {
  position: relative;
}

.custom-dropdown {
  position: absolute;
  top: 100%;
  left: 180px; /* 配合 label 寬度 */
  right: 0;
  background: white;
  border: 1px solid #c0c4cc;
  border-radius: 4px;
  max-height: 200px; /* 約顯示 6 筆 */
  overflow-y: auto;
  z-index: 1000;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 0;
  margin: 0;
  list-style: none;
}

.custom-dropdown li {
  padding: 8px 12px;
  cursor: pointer;
  color: #606266;
  font-size: 14px;
}

.custom-dropdown li:hover {
  background-color: #f5f7fa;
  color: #409eff;
}

/* [新增] 下拉選單切換圖示 */
.toggle-icon {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  color: #c0c4cc;
  font-size: 12px;
  transition: color 0.3s;
  padding: 5px; /* 增加點擊範圍 */
}

.toggle-icon:hover {
  color: #409eff;
}
</style>
