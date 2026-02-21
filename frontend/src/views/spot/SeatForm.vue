<template>
  <div class="seat-form-container">
    <!-- ========== Header 區域 ========== -->
    <section class="content-header">
      <div class="container-fluid">
        <div class="row align-items-center">
          <div class="col-sm-6">
            <div class="page-title-box">
              <div class="title-icon" :class="isEditMode ? 'edit-mode' : 'add-mode'">
                <i :class="isEditMode ? 'fas fa-edit' : 'fas fa-plus-circle'"></i>
              </div>
              <div class="title-content">
                <h1>{{ isEditMode ? '修改座位' : '新增座位' }}</h1>
                <p class="subtitle">{{ isEditMode ? '修改座位資訊' : '建立新的座位設備' }}</p>
              </div>
            </div>
          </div>
          <div class="col-sm-6 text-right">
            <el-button @click="goBack">
              <i class="fas fa-arrow-left mr-1"></i> 返回列表
            </el-button>
          </div>
        </div>
      </div>
    </section>

    <section class="content">
      <div class="container-fluid">
        <transition name="zoom-fade" appear>
          <el-card shadow="hover" class="form-card">
            <el-form
              :model="formData"
              label-width="120px"
              label-position="top"
              class="modern-form"
              @submit.prevent="handleSubmit"
            >
              <!-- ========== 基本資料區 ========== -->
              <div class="form-section">
                <div class="section-header">
                  <i class="fas fa-info-circle"></i>
                  <span>基本資料</span>
                </div>

                <!-- 所屬據點 (下拉選單) -->
                <el-form-item label="所屬據點" required>
                  <el-select v-model="formData.spotId" placeholder="請選擇據點" style="width: 100%">
                    <el-option
                      v-for="spot in spotList"
                      :key="spot.spotId"
                      :label="`${spot.spotName} (ID: ${spot.spotId})`"
                      :value="spot.spotId"
                    />
                  </el-select>
                </el-form-item>

                <el-row :gutter="20">
                  <el-col :span="12">
                    <!-- 座位名稱 -->
                    <el-form-item label="座位名稱" required>
                      <el-input v-model="formData.seatsName" placeholder="例如：A-01" clearable>
                        <template #prefix><i class="fas fa-chair"></i></template>
                      </el-input>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <!-- 序號 -->
                    <el-form-item label="序號">
                      <el-input v-model="formData.serialNumber" :placeholder="isEditMode ? '例如：SN-2023001' : '留空則自動產生 (例如：SN-2026001)'" clearable>
                        <template #prefix><i class="fas fa-barcode"></i></template>
                      </el-input>
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-row :gutter="20">
                  <el-col :span="12">
                    <!-- 類型 -->
                    <el-form-item label="類型" required>
                      <el-input v-model="formData.seatsType" placeholder="例如:高腳椅、塑膠椅..." clearable>
                        <template #prefix><i class="fas fa-couch"></i></template>
                      </el-input>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <!-- 狀態 -->
                    <el-form-item label="狀態">
                      <el-select v-model="formData.seatsStatus" style="width: 100%">
                        <el-option label="啟用" value="啟用">
                          <i class="fas fa-check-circle text-success mr-2"></i> 啟用
                        </el-option>
                        <el-option label="停用" value="停用">
                          <i class="fas fa-ban text-danger mr-2"></i> 停用
                        </el-option>
                        <el-option label="維修中" value="維修中">
                          <i class="fas fa-tools text-warning mr-2"></i> 維修中
                        </el-option>
                      </el-select>
                    </el-form-item>
                  </el-col>
                </el-row>
              </div>

              <!-- ========== 按鈕區 ========== -->
              <div class="form-actions">
                <el-button @click="goBack" class="back-btn">
                  <i class="fas fa-times mr-1"></i> 取消
                </el-button>
                <el-button type="primary" @click="handleSubmit" :loading="isSubmitting" class="submit-btn">
                  <i class="fas fa-save mr-1"></i> {{ isSubmitting ? '儲存中...' : '儲存' }}
                </el-button>
              </div>
            </el-form>
          </el-card>
        </transition>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import Swal from 'sweetalert2'

const route = useRoute()
const router = useRouter()

// 明確定義元件名稱，方便識別與除錯
defineOptions({
  name: 'SeatForm'
})

// 判斷是否為編輯模式 (網址有 id 參數)
const isEditMode = computed(() => !!route.params.id)

// 表單資料
const formData = ref({
  seatsName: '',
  seatsType: '',
  seatsStatus: '啟用',
  spotId: '',
  serialNumber: '',
})

// 防止重複提交的狀態
const isSubmitting = ref(false)

// 據點列表 (供下拉選單使用)
const spotList = ref([])

// 初始化
onMounted(async () => {
  await fetchSpots()
  
  if (isEditMode.value) {
    await fetchSeatData(route.params.id)
  }
})

// 取得所有據點 (用於下拉選單)
const fetchSpots = async () => {
  try {
    const res = await axios.get('/api/spot/list')
    spotList.value = res.data
  } catch (err) {
    console.error('無法取得據點列表:', err)
    Swal.fire({
      title: '載入失敗',
      html: '<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg><p style="margin-top:12px;">無法載入據點列表，請檢查後端連線</p></div>',
      confirmButtonColor: '#409eff',
      confirmButtonText: '確定'
    })
  }
}

// 取得單一座位資料 (編輯模式用)
const fetchSeatData = async (id) => {
  if (!id) {
    console.warn('未提供座位ID，無法取得資料')
    return
  }
  try {
    const res = await axios.get(`/api/seats/${id}`)
    // 將後端資料填入表單
    formData.value = {
      seatsId: res.data.seatsId, // 雖然表單不顯示，但更新時可能需要
      seatsName: res.data.seatsName,
      seatsType: res.data.seatsType,
      seatsStatus: res.data.seatsStatus,
      spotId: res.data.spotId,
      serialNumber: res.data.serialNumber,
    }
  } catch (err) {
    console.error('無法取得座位資料:', err)
    Swal.fire({
      title: '讀取失敗',
      html: '<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg><p style="margin-top:12px;">無法讀取座位資料</p></div>',
      confirmButtonColor: '#409eff',
      confirmButtonText: '確定'
    })
    router.push('/admin/seat/list')
  }
}

// 送出表單
const handleSubmit = async () => {
  if (isSubmitting.value) return
  isSubmitting.value = true

  // [優化] 建立一個乾淨的 payload，避免直接修改 ref
  const payload = { ...formData.value };

  // [修正] 如果序號為空字串，應轉為 null 送出，以符合資料庫的唯一索引篩選 (WHERE serialNumber IS NOT NULL)
  // 避免多筆空字串資料違反唯一約束
  if (payload.serialNumber === '') {
    payload.serialNumber = null;
  }

  try {
    if (isEditMode.value) {
      // 更新 (PUT)
      const seatId = route.params.id
      await axios.put(`/api/seats/${seatId}`, payload)
      await Swal.fire({
        title: '更新成功',
        html: '<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#67c23a" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg><p style="margin-top:12px;">座位資料已成功更新</p></div>',
        confirmButtonColor: '#409eff',
        confirmButtonText: '確定'
      })
    } else {
      // 新增 (POST)
      await axios.post('/api/seats', payload)
      await Swal.fire({
        title: '新增成功',
        html: '<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#67c23a" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg><p style="margin-top:12px;">座位資料已成功新增</p></div>',
        confirmButtonColor: '#409eff',
        confirmButtonText: '確定'
      })
    }
    // 成功後返回列表
    router.push('/admin/seat/list')
  } catch (err) {
    console.error('儲存失敗:', err)
    // [優化] 顯示更詳細的錯誤訊息
    const errorMsg = err.response?.data?.message || '儲存失敗，請檢查輸入資料或後端連線';
    Swal.fire({
      title: '儲存失敗',
      html: `<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg><p style="margin-top:12px;">${errorMsg}</p></div>`,
      confirmButtonColor: '#409eff',
      confirmButtonText: '確定'
    })
  } finally {
    isSubmitting.value = false
  }
}

const goBack = () => {
  router.back()
}
</script>

<style scoped>
/* ========== 頁面容器 ========== */
.seat-form-container {
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

.add-mode {
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
}

.edit-mode {
  background: linear-gradient(135deg, #e6a23c 0%, #f3d19e 100%);
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

/* ========== 表單卡片 ========== */
.form-card {
  border-radius: 12px;
  max-width: 800px;
  margin: 0 auto;
}

.form-section {
  margin-bottom: 32px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.1rem;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #409eff;
}

/* ========== 按鈕區 ========== */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 24px;
  margin-top: 24px;
  border-top: 1px solid #ebeef5;
}

.submit-btn {
  min-width: 120px;
  border-radius: 10px;
  font-weight: 600;
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
  border: none;
  transition: all 0.3s ease;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(103, 194, 58, 0.4);
}

.back-btn {
  min-width: 100px;
  border-radius: 10px;
}

/* ========== 動畫效果 ========== */
.zoom-fade-enter-active {
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.zoom-fade-leave-active {
  transition: all 0.3s ease-in;
}

.zoom-fade-enter-from {
  transform: scale(0.9);
  opacity: 0;
}

.zoom-fade-leave-to {
  transform: scale(0.95);
  opacity: 0;
}

/* ========== 工具類 ========== */
.mr-1 { margin-right: 4px; }
.mr-2 { margin-right: 8px; }
.text-success { color: #67c23a; }
.text-warning { color: #e6a23c; }
.text-danger { color: #f56c6c; }

/* ========== 響應式設計 ========== */
@media (max-width: 768px) {
  .page-title-box {
    flex-direction: column;
    text-align: center;
    gap: 12px;
  }
}
</style>