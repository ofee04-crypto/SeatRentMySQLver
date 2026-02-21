<script setup>
import { ref } from 'vue'
import axios from 'axios'
import RecRentSearch from '@/components/rec/RecRentSearch.vue'
import RecRentAdd from '@/components/rec/RecRentAdd.vue'
import RecRentEdit from '@/components/rec/RecRentEdit.vue' // 1. 引入 Edit 組件
import Swal from 'sweetalert2'

// --- 1. 狀態定義 ---
const activeView = ref('list') // 'list', 'add', 'edit'
const editingRent = ref(null) // Holds the data for the rent being edited
const searchComponent = ref(null) // Ref to access the search component instance
const API_URL = 'http://localhost:8080/rec-rent'

// --- 2. 核心邏輯 ---

// 新增或更新 (Create / Update)
const handleSaveRent = async (formData) => {
  try {
    // [修正] 依據當前視圖模式決定 HTTP 方法與 URL
    // 避免因為新增時填寫了 recId 而誤判為 PUT 請求
    let method = 'post'
    let url = `${API_URL}/new` // 對應 Controller 的 @PostMapping("/new")

    if (activeView.value === 'edit') {
      method = 'put'
      url = `${API_URL}/${formData.recId}`
    }

    const res = await axios[method](url, formData)
    if (res.status === 200 || res.status === 201) {
      alert(activeView.value === 'edit' ? '更新成功！' : '新增成功！')
      activeView.value = 'list'
      // Use the ref to call the child's method
      if (searchComponent.value) {
        await searchComponent.value.loadRents()
      }
    } else {
      alert('儲存失敗，請檢查輸入資料。')
    }
  } catch (err) {
    console.error('儲存操作失敗:', err)
    alert('儲存失敗，請檢查輸入資料。')
  }
}

// 刪除 (Delete)
const handleDeleteRent = async (id) => {
  // 使用 SweetAlert2 彈出確認對話框，提供更好的使用者體驗
  const result = await Swal.fire({
    title: `確定要刪除訂單 #${id} 嗎?`,
    text: '此操作無法復原！',
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#d33',
    cancelButtonColor: '#3085d6',
    confirmButtonText: '是的，刪除它！',
    cancelButtonText: '取消',
  })

  // 如果使用者沒有確認，則中止操作
  if (!result.isConfirmed) {
    return
  }

  try {
    // 呼叫後端 API 執行刪除，使用標準的 RESTful 風格 URL
    const res = await axios.delete(`${API_URL}/${id}`)
    
    // 檢查回應狀態碼，200 (OK) 或 204 (No Content) 都代表成功
    if (res.status === 200 || res.status === 204) {
      await Swal.fire('已刪除！', `訂單 #${id} 已成功刪除。`, 'success')
      // 如果 searchComponent 存在，則呼叫其方法重新載入訂單列表
      if (searchComponent.value) {
        await searchComponent.value.loadRents()
      }
    } else {
      // 若狀態碼不為成功，顯示錯誤
      Swal.fire('刪除失敗', `發生未預期的錯誤，狀態碼: ${res.status}`, 'error')
    }
  } catch (err) {
    console.error('刪除操作失敗:', err)
    // 處理 API 呼叫的錯誤
    const errorMessage = err.response?.data?.message || err.message || '未知錯誤'
    Swal.fire('刪除失敗', `錯誤: ${errorMessage}`, 'error')
  }
}

// --- 3. 視圖切換邏輯 ---

// 準備編輯資料
const handleEditRent = (rent) => {
  editingRent.value = { ...rent }
  activeView.value = 'edit' // 切換到編輯視圖

  // Scroll to top for better user experience
  setTimeout(() => {
    const mainContent = document.querySelector('.main-content')
    if (mainContent) {
      mainContent.scrollTo({ top: 0, behavior: 'smooth' })
    }
  }, 50)
}

// 切換到新增畫面
const goToAddView = () => {
  editingRent.value = null // Clear any editing data
  activeView.value = 'add' // 切換到新增視圖
}

// 取消並返回列表
const backToList = () => {
  editingRent.value = null // Clear any editing data
  activeView.value = 'list'
}
</script>

<template>
  <div class="rec-rent-container">
    <div class="top-nav">
      <button
        @click="backToList"
        :class="{ active: activeView === 'list' }"
        :disabled="activeView === 'list'"
      >
        訂單查詢
      </button>

      <button
        @click="goToAddView"
        :class="{ active: activeView === 'add' }"
        :disabled="activeView === 'add'"
      >
        新增訂單
      </button>
    </div>

    <div class="main-content">
      <h1>訂單管理系統</h1>

      <div v-if="activeView === 'add'" class="view-section">
        <rec-rent-add @save-rent="handleSaveRent" @cancel="backToList" />
      </div>

      <!--  組件的區塊 -->
      <div v-if="activeView === 'edit'" class="view-section">
        <rec-rent-edit
          :initial-data="editingRent"
          @save-rent="handleSaveRent"
          @cancel="backToList"
        />
      </div>

      <div v-if="activeView === 'list'" class="view-section">
        <rec-rent-search
          ref="searchComponent"
          @edit-rent="handleEditRent"
          @delete-rent="handleDeleteRent"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ========== 主容器 - 淺色背景 ========== */
.rec-rent-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  width: 100%;
  font-family: 'Microsoft JhengHei', Arial, sans-serif;
  background: linear-gradient(180deg, #f0f5fa 0%, #e8eef5 100%);
}

/* ========== 頂部導航 - 淺色風格 ========== */
.top-nav {
  width: 100%;
  background: white;
  display: flex;
  padding: 12px 20px;
  gap: 10px;
  flex-shrink: 0;
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.04);
}

.top-nav button {
  background: #409eff;
  color: #ffffff;
  font-size: 14px;
  font-weight: 500;
  padding: 10px 22px;
  margin: 0;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.top-nav button:disabled,
.top-nav button.active {
  background: #67c23a;
  color: #fff;
  font-weight: 600;
  cursor: default;
}

.top-nav button:not(:disabled):hover {
  background: #66b1ff;
  transform: translateY(-1px);
}

/* ========== 主內容區 ========== */
.main-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

/* ========== 標題區 ========== */
.main-content h1 {
  font-size: 1.5rem;
  font-weight: 700;
  color: #303133;
  margin-bottom: 20px;
}

/* ========== 視圖區塊 ========== */
.view-section {
  display: block;
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.view-section:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}
</style>
