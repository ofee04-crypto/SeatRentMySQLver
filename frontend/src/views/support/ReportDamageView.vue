<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import Swal from 'sweetalert2'
import maintenanceApi from '@/api/modules/maintenance'

const route = useRoute()
const router = useRouter()

// ============ 表單狀態 ============
const formRef = ref(null)
const uploadRef = ref(null)
const submitting = ref(false)
const loading = ref(false)

// 維修目標類型
const targetType = ref('spot') // 'spot' | 'seat'

// 表單資料
const form = reactive({
  spotId: null,
  seatsId: null,
  issueType: '',
  issueDesc: '',
  issuePriority: 'NORMAL',
})

// 問題類型選項（不含「保養」）
const issueTypeOptions = [
  { value: '機台故障異常', icon: '🖥️' },
  { value: '椅子損壞', icon: '🪑' },
  { value: '清潔問題', icon: '🧹' },
  { value: '網路異常', icon: '📡' },
  { value: '其他問題', icon: '❓' },
]

// 優先級配置
const priorityConfig = {
  LOW: { color: '#909399', icon: '🔵', text: '低', desc: '可稍後處理' },
  NORMAL: { color: '#409eff', icon: '🟢', text: '普通', desc: '一般問題' },
  HIGH: { color: '#e6a23c', icon: '🟠', text: '高', desc: '需盡快處理' },
  URGENT: { color: '#f56c6c', icon: '🔴', text: '緊急', desc: '立即處理' },
}

// 選項資料
const spotOptions = ref([])
const seatOptions = ref([])

// 圖片上傳
const fileList = ref([])
const MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB
const MAX_FILE_COUNT = 5
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp']

// ============ 驗證規則 ============
const rules = {
  issueType: [{ required: true, message: '請選擇或輸入問題類型', trigger: 'blur' }],
  issueDesc: [],
}

// ============ Computed ============
/**
 * 根據目標類型動態顯示可選擇的機台或椅子
 */
const currentTargetOptions = computed(() => {
  return targetType.value === 'spot' ? spotOptions.value : seatOptions.value
})

/**
 * 表單驗證狀態
 */
const isFormValid = computed(() => {
  const hasTarget = targetType.value === 'spot' ? !!form.spotId : !!form.seatsId
  const hasType = !!form.issueType
  const hasImages = fileList.value.length > 0
  return hasTarget && hasType && hasImages
})

// ============ 生命週期 ============
onMounted(async () => {
  loading.value = true
  try {
    // 載入機台與椅子選項
    const [spotRes, seatRes] = await Promise.all([
      maintenanceApi.getAllSpots().catch(() => ({ data: [] })),
      maintenanceApi.getAllSeats().catch(() => ({ data: [] })),
    ])

    spotOptions.value = Array.isArray(spotRes.data) ? spotRes.data : []
    seatOptions.value = Array.isArray(seatRes.data) ? seatRes.data : []

    // 從 query 預填資料
    const { spotId, seatId, recId } = route.query

    if (spotId) {
      targetType.value = 'spot'
      form.spotId = Number(spotId)
    } else if (seatId) {
      targetType.value = 'seat'
      form.seatsId = Number(seatId)
    }

    // 若有 recId，可在描述中提及（可選）
    if (recId) {
      form.issueDesc = `訂單編號：${recId}\n`
    }
  } catch (error) {
    console.error('載入資料失敗:', error)
    ElMessage.error('載入資料失敗，請重新整理頁面')
  } finally {
    loading.value = false
  }
})

// ============ 方法 ============
/**
 * 檔案上傳前檢查
 */
const beforeUpload = (file) => {
  // 檢查檔案類型
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    ElMessage.error(`不支援的檔案格式：${file.name}（僅允許 JPG, PNG, WEBP）`)
    return false
  }

  // 檢查檔案大小
  if (file.size > MAX_FILE_SIZE) {
    ElMessage.error(`${file.name} 超過 5MB 限制`)
    return false
  }

  // 檢查數量
  if (fileList.value.length >= MAX_FILE_COUNT) {
    ElMessage.error(`最多只能上傳 ${MAX_FILE_COUNT} 張圖片`)
    return false
  }

  return true
}

/**
 * 檔案變更處理
 */
const handleFileChange = (file) => {
  if (beforeUpload(file.raw)) {
    fileList.value.push(file)
  }
}

/**
 * 移除檔案
 */
const handleRemoveFile = (file) => {
  const index = fileList.value.findIndex((f) => f.uid === file.uid)
  if (index > -1) {
    fileList.value.splice(index, 1)
  }
}

/**
 * 選擇問題類型
 */
const selectIssueType = (type) => {
  form.issueType = type.value
}

/**
 * 送出表單
 */
const submit = async () => {
  // 1. 驗證表單
  await formRef.value.validate()

  // 2. 檢查是否有圖片
  if (fileList.value.length === 0) {
    ElMessage.warning('請至少上傳一張圖片')
    return
  }

  // 3. 確認彈窗
  const confirmResult = await Swal.fire({
    title: '確認送出問題回報？',
    html: `
      <div style="text-align: left; padding: 10px;">
        <p><b>回報目標：</b>${targetType.value === 'spot' ? '機台' : '椅子'} #${targetType.value === 'spot' ? form.spotId : form.seatsId}</p>
        <p><b>問題類型：</b>${form.issueType}</p>
        <p><b>優先級：</b>${priorityConfig[form.issuePriority].text}</p>
        <p><b>圖片數量：</b>${fileList.value.length} 張</p>
      </div>
    `,
    icon: 'question',
    showCancelButton: true,
    confirmButtonColor: '#409eff',
    cancelButtonColor: '#909399',
    confirmButtonText: '<i class="fas fa-paper-plane mr-1"></i> 確認送出',
    cancelButtonText: '再檢查一下',
  })

  if (!confirmResult.isConfirmed) return

  submitting.value = true
  try {
    // ============ 【關鍵】Step 1：建立工單（加上問題回報標記）============
    const ticketPayload = {
      spotId: targetType.value === 'spot' ? form.spotId : null,
      seatsId: targetType.value === 'seat' ? form.seatsId : null,
      issueType: `SUPPORT_${form.issueType}`, // ✅ 標記為問題回報
      issueDesc: `[REPORT] ${form.issueDesc}`, // ✅ 描述前加標記
      issuePriority: form.issuePriority,
      assignedStaffId: null, // 前台回報不指派人員
    }

    console.log('📤 建立工單:', ticketPayload)
    const createRes = await maintenanceApi.createTicket(ticketPayload)
    const ticketId = createRes.data?.ticketId

    if (!ticketId) {
      throw new Error('未取得工單 ID')
    }

    // ============ Step 2：上傳圖片 ============
    let attachmentSuccess = false
    try {
      const files = fileList.value.map((f) => f.raw)
      await maintenanceApi.uploadTicketAttachments(ticketId, files, '使用者回報')
      attachmentSuccess = true
    } catch (attachError) {
      console.error('圖片上傳失敗:', attachError)
      // ✅ 不拋出錯誤，工單已建立成功
    }

    // ============ Step 3：成功提示 ============
    if (attachmentSuccess) {
      await Swal.fire({
        icon: 'success',
        title: '回報成功！',
        html: `
          <div style="text-align: center;">
            <p>您的問題回報已送出</p>
            <p style="color: #67c23a; font-weight: 600; font-size: 18px;">工單編號：#${ticketId}</p>
            <p style="color: #909399; font-size: 13px;">我們會盡快為您處理</p>
          </div>
        `,
        confirmButtonText: '返回地圖',
        confirmButtonColor: '#409eff',
      })
      router.push('/SearchSpot')
    } else {
      // ✅ 工單建立成功但圖片上傳失敗
      await Swal.fire({
        icon: 'warning',
        title: '工單已建立',
        html: `
          <div style="text-align: center;">
            <p style="color: #e6a23c;">工單編號：<b>#${ticketId}</b></p>
            <p style="color: #f56c6c; font-size: 14px;">但圖片上傳失敗</p>
            <p style="color: #909399; font-size: 13px;">您可稍後在工單詳情中補上傳</p>
          </div>
        `,
        confirmButtonText: '我知道了',
        confirmButtonColor: '#e6a23c',
      })
      router.push('/SearchSpot')
    }
  } catch (error) {
    console.error('送出失敗:', error)
    const errorMsg = error?.response?.data?.message || '系統忙碌中，請稍後再試'
    Swal.fire('送出失敗', errorMsg, 'error')
  } finally {
    submitting.value = false
  }
}

/**
 * 返回上一頁
 */
const handleCancel = () => {
  router.push('/support/report')
}
</script>

<template>
  <div class="support-report-container">
    <!-- 頁面標題 -->
    <section class="page-header">
      <div class="container-fluid">
        <div class="header-content">
          <div class="header-icon">
            <i class="fas fa-exclamation-circle"></i>
          </div>
          <div class="header-text">
            <h1>機台/椅子毀損問題</h1>
            <p>請詳細描述您遇到的問題，我們會盡快為您處理</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 表單主體 -->
    <section class="content">
      <div class="container-fluid">
        <el-card shadow="hover" class="form-card" v-loading="loading">
          <template #header>
            <div class="card-header">
              <span class="header-icon">
                <i class="fas fa-clipboard-list"></i>
              </span>
              <span class="header-text">回報表單</span>
            </div>
          </template>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            class="report-form"
          >
            <!-- 回報目標類型 -->
            <el-form-item label="回報目標" required>
              <div class="target-type-switch">
                <div
                  class="target-type-option"
                  :class="{ active: targetType === 'spot' }"
                  @click="targetType = 'spot'"
                >
                  <i class="fas fa-desktop"></i>
                  <span>機台</span>
                </div>
                <div
                  class="target-type-option"
                  :class="{ active: targetType === 'seat' }"
                  @click="targetType = 'seat'"
                >
                  <i class="fas fa-chair"></i>
                  <span>椅子</span>
                </div>
              </div>
            </el-form-item>

            <!-- 選擇機台 / 椅子 -->
            <el-form-item :label="targetType === 'spot' ? '選擇機台' : '選擇椅子'" required>
              <el-select
                v-if="targetType === 'spot'"
                v-model="form.spotId"
                placeholder="請選擇機台"
                filterable
                size="large"
                class="w-100"
              >
                <el-option
                  v-for="spot in spotOptions"
                  :key="spot.spotId"
                  :label="`${spot.spotName || spot.spotId} (${spot.spotStatus})`"
                  :value="spot.spotId"
                  :disabled="spot.spotStatus === '停用'"
                >
                  <div class="option-item">
                    <i class="fas fa-desktop"></i>
                    <span>{{ spot.spotName || `機台 #${spot.spotId}` }}</span>
                    <el-tag
                      :type="spot.spotStatus === '營運中' ? 'success' : 'danger'"
                      size="small"
                    >
                      {{ spot.spotStatus }}
                    </el-tag>
                  </div>
                </el-option>
              </el-select>

              <el-select
                v-else
                v-model="form.seatsId"
                placeholder="請選擇椅子"
                filterable
                size="large"
                class="w-100"
              >
                <el-option
                  v-for="seat in seatOptions"
                  :key="seat.seatsId"
                  :label="`${seat.seatsName || seat.seatsId} (${seat.seatsStatus})`"
                  :value="seat.seatsId"
                  :disabled="seat.seatsStatus === '停用'"
                >
                  <div class="option-item">
                    <i class="fas fa-chair"></i>
                    <span>{{ seat.seatsName || `椅子 #${seat.seatsId}` }}</span>
                    <el-tag
                      :type="seat.seatsStatus === '可用' ? 'success' : 'warning'"
                      size="small"
                    >
                      {{ seat.seatsStatus }}
                    </el-tag>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>

            <!-- 問題類型 -->
            <el-form-item label="問題類型" prop="issueType" required>
              <div class="issue-type-grid">
                <div
                  v-for="type in issueTypeOptions"
                  :key="type.value"
                  class="issue-type-card"
                  :class="{ active: form.issueType === type.value }"
                  @click="selectIssueType(type)"
                >
                  <span class="type-icon">{{ type.icon }}</span>
                  <span class="type-text">{{ type.value }}</span>
                </div>
              </div>
              <el-input
                v-model="form.issueType"
                placeholder="或自行輸入問題類型..."
                size="large"
                clearable
                class="mt-2"
              />
            </el-form-item>

            <!-- 問題描述 -->
            <el-form-item label="問題描述" prop="issueDesc">
              <el-input
                v-model="form.issueDesc"
                type="textarea"
                :rows="6"
                placeholder="請詳細描述問題狀況，例如：故障位置、發生時間、嚴重程度等... "
                show-word-limit
                maxlength="1000"
              />
            </el-form-item>

            <!-- 優先級 -->
            <el-form-item label="優先級">
              <div class="priority-cards">
                <div
                  v-for="(config, key) in priorityConfig"
                  :key="key"
                  class="priority-card"
                  :class="{ active: form.issuePriority === key }"
                  :style="{ '--card-color': config.color }"
                  @click="form.issuePriority = key"
                >
                  <span class="priority-icon">{{ config.icon }}</span>
                  <span class="priority-text">{{ config.text }}</span>
                  <span class="priority-desc">{{ config.desc }}</span>
                </div>
              </div>
            </el-form-item>

            <!-- 圖片上傳 -->
            <el-form-item label="問題照片" required>
              <el-alert type="warning" :closable="false" show-icon class="mb-3">
                <template #title>
                  <span style="font-size: 13px">
                    <i class="fas fa-camera"></i> 必須上傳圖片（最多 5 張，單張最大 5MB，支援
                    JPG/PNG/WEBP）
                  </span>
                </template>
              </el-alert>

              <el-upload
                ref="uploadRef"
                :auto-upload="false"
                :on-change="handleFileChange"
                :on-remove="handleRemoveFile"
                :file-list="fileList"
                accept="image/jpeg,image/png,image/webp"
                list-type="picture-card"
                :limit="MAX_FILE_COUNT"
              >
                <template #default>
                  <div class="upload-trigger">
                    <i class="fas fa-plus"></i>
                    <div class="upload-text">選擇圖片</div>
                  </div>
                </template>
                <template #tip>
                  <div class="el-upload__tip">
                    <i class="fas fa-info-circle"></i>
                    送出後將自動上傳圖片
                  </div>
                </template>
              </el-upload>
            </el-form-item>

            <!-- 按鈕區 -->
            <el-form-item class="form-actions">
              <div class="button-group">
                <el-button
                  type="primary"
                  size="large"
                  @click="submit"
                  :loading="submitting"
                  :disabled="!isFormValid"
                >
                  <i class="fas fa-paper-plane mr-2" v-if="!submitting"></i>
                  {{ submitting ? '送出中...' : '送出回報' }}
                </el-button>
                <el-button size="large" @click="handleCancel">
                  <i class="fas fa-arrow-left mr-2"></i> 返回上一頁
                </el-button>
              </div>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 提示卡片 -->
        <div class="tips-card">
          <div class="tip-item">
            <i class="fas fa-lightbulb"></i>
            <span>詳細的描述與清晰的照片能幫助我們更快解決問題</span>
          </div>
          <div class="tip-item">
            <i class="fas fa-shield-alt"></i>
            <span>您的個人資料將受到保護，僅用於問題處理</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* ========== 頁面容器 ========== */
.support-report-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  padding-bottom: 40px;
}

/* ========== 頁面標題 ========== */
.page-header {
  background: linear-gradient(135deg, #d4e3ee 0%, #c8d9e6 100%);
  padding: 40px 20px;
  color: white;
}

.header-content {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-icon {
  width: 64px;
  height: 64px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
}

.header-text h1 {
  margin: 0 0 5px;
  font-size: 1.8rem;
  font-weight: 800;
  color: #2c3e50;
  text-shadow:
    0 2px 8px rgba(0, 0, 0, 0.15),
    0 1px 3px rgba(255, 255, 255, 0.5);
}

.header-text p {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: #34495e;
  text-shadow:
    0 1px 4px rgba(0, 0, 0, 0.1),
    0 1px 2px rgba(255, 255, 255, 0.3);
}

/* ========== 內容區域 ========== */
.content {
  padding: 30px 20px;
}

.form-card {
  max-width: 900px;
  margin: 0 auto 30px;
  border-radius: 16px;
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-header .header-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #d4e3ee 0%, #c8d9e6 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
}

.card-header .header-text {
  font-weight: 600;
  font-size: 1.1rem;
  color: #303133;
}

/* ========== 表單樣式 ========== */
.report-form {
  padding: 20px 0;
}

/* 回報目標類型切換 */
.target-type-switch {
  display: flex;
  gap: 16px;
  width: 100%;
}

.target-type-option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 24px;
  background: #f5f7fa;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.target-type-option:hover {
  background: #ecf5ff;
  transform: translateY(-2px);
}

.target-type-option.active {
  background: linear-gradient(135deg, #d4e3ee 0%, #c8d9e6 100%);
  color: #2c3e50;
  border-color: #409eff;
  transform: scale(1.05);
}

.target-type-option i {
  font-size: 32px;
}

.target-type-option span {
  font-weight: 600;
  font-size: 16px;
}

/* 選項項目 */
.option-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.option-item i {
  color: #409eff;
}

/* 問題類型卡片 */
.issue-type-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 12px;
  margin-bottom: 10px;
}

.issue-type-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.issue-type-card:hover {
  background: #ecf5ff;
  transform: translateY(-2px);
}

.issue-type-card.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.type-icon {
  font-size: 28px;
}

.type-text {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
  text-align: center;
}

/* 優先級卡片 */
.priority-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.priority-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 16px 12px;
  background: #f5f7fa;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.priority-card:hover {
  background: #ecf5ff;
  transform: translateY(-2px);
}

.priority-card.active {
  border-color: var(--card-color);
  background: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.priority-icon {
  font-size: 24px;
}

.priority-text {
  font-size: 14px;
  color: var(--card-color);
  font-weight: 600;
}

.priority-desc {
  font-size: 11px;
  color: #909399;
  text-align: center;
}

/* 圖片上傳 */
.upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #8c939d;
}

.upload-trigger i {
  font-size: 28px;
}

.upload-text {
  font-size: 12px;
}

:deep(.el-upload__tip) {
  color: #909399;
  font-size: 12px;
}

/* 按鈕區 */
.form-actions {
  margin-top: 30px;
}

.button-group {
  display: flex;
  gap: 12px;
  justify-content: center;
}

/* 提示卡片 */
.tips-card {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  color: #606266;
  font-size: 14px;
}

.tip-item i {
  color: #409eff;
  font-size: 18px;
}

/* ========== 響應式設計 ========== */
@media (max-width: 768px) {
  .priority-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .issue-type-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* ========== 輔助類 ========== */
.w-100 {
  width: 100%;
}
.mt-2 {
  margin-top: 8px;
}
.mb-3 {
  margin-bottom: 12px;
}
.mr-1 {
  margin-right: 4px;
}
.mr-2 {
  margin-right: 8px;
}
</style>
