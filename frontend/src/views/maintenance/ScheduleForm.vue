<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import maintenanceApi from '@/api/modules/maintenance'
import Swal from 'sweetalert2'
import { useScheduleConfig } from '@/composables/maintenance/useScheduleConfig'
import { Setting, Aim, Clock, User, Check } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const { scheduleTypeConfig, priorityConfig, formatScheduleDetail } = useScheduleConfig()

// ====== 編輯模式判斷 ======
const isEdit = computed(() => !!route.params.id)
const pageTitle = computed(() => (isEdit.value ? '編輯排程' : '新增排程'))

// FIX: 驗證 route param id 是否為有效數字（防止字串 ID 如 DEMO_xxx 進入編輯頁）
const isValidRouteId = computed(() => {
  if (!route.params.id) return true // 新增模式不需驗證
  const id = route.params.id
  return /^\d+$/.test(id) // 必須是純數字
})

// ====== 步驟控制 ======
const currentStep = ref(0)
const steps = [
  { title: '基本資訊', icon: Setting },
  { title: '目標設定', icon: Aim },
  { title: '排程設定', icon: Clock },
  { title: '指派人員', icon: User },
  { title: '確認送出', icon: Check },
]

// ====== 表單資料 ======
const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)

const form = reactive({
  title: '',
  targetType: 'SPOT',
  targetId: null,
  scheduleType: 'DAILY',
  dayOfWeek: null,
  dayOfMonth: null,
  executeTime: '09:00:00',
  issueType: '',
  issuePriority: 'NORMAL',
  assignedStaffId: null,
  isActive: true,
})

// ====== 下拉選項資料 ======
const spotOptions = ref([])
const seatOptions = ref([])
const staffOptions = ref([])

// ====== 配置選項 ======
const scheduleTypeOptions = [
  { label: '每日', value: 'DAILY', desc: '每天固定時間執行', icon: 'fas fa-sun' },
  { label: '每週', value: 'WEEKLY', desc: '每週固定星期執行', icon: 'fas fa-calendar-week' },
  { label: '每月', value: 'MONTHLY', desc: '每月固定日期執行', icon: 'fas fa-calendar-alt' },
]

const dayOfWeekOptions = [
  { label: '週一', value: 1 },
  { label: '週二', value: 2 },
  { label: '週三', value: 3 },
  { label: '週四', value: 4 },
  { label: '週五', value: 5 },
  { label: '週六', value: 6 },
  { label: '週日', value: 7 },
]

const priorityOptions = [
  { label: '低', value: 'LOW' },
  { label: '一般', value: 'NORMAL' },
  { label: '高', value: 'HIGH' },
  { label: '緊急', value: 'URGENT' },
]

const issueTypeOptions = ['例行保養', '清潔維護', '零件檢查', '系統校正', '安全檢測', '耗材更換']

// ====== 表單驗證規則 ======
const rules = reactive({
  title: [
    { required: true, message: '請輸入排程標題', trigger: 'blur' },
    { min: 2, max: 100, message: '標題長度為 2-100 字', trigger: 'blur' },
  ],
  targetType: [{ required: true, message: '請選擇目標類型', trigger: 'change' }],
  targetId: [{ required: true, message: '請選擇目標', trigger: 'change' }],
  scheduleType: [{ required: true, message: '請選擇排程類型', trigger: 'change' }],
  dayOfWeek: [{ required: true, message: '請選擇星期幾', trigger: 'change' }],
  dayOfMonth: [{ required: true, message: '請輸入日期', trigger: 'blur' }],
  executeTime: [{ required: true, message: '請選擇執行時間', trigger: 'change' }],
  issueType: [{ required: true, message: '請選擇問題類型', trigger: 'change' }],
  issuePriority: [{ required: true, message: '請選擇優先級', trigger: 'change' }],
})

// ====== 動態驗證規則 ======
const dynamicRules = computed(() => {
  const baseRules = { ...rules }
  if (form.scheduleType === 'DAILY') {
    delete baseRules.dayOfWeek
    delete baseRules.dayOfMonth
  } else if (form.scheduleType === 'WEEKLY') {
    delete baseRules.dayOfMonth
  } else if (form.scheduleType === 'MONTHLY') {
    delete baseRules.dayOfWeek
  }
  return baseRules
})

// ====== 載入選項資料 ======
const loadOptions = async () => {
  try {
    loading.value = true
    const [spotsRes, seatsRes, staffRes] = await Promise.all([
      maintenanceApi.getAllSpots(),
      maintenanceApi.getAllSeats(),
      maintenanceApi.getAllStaff(),
    ])
    spotOptions.value = spotsRes.data || []
    seatOptions.value = seatsRes.data || []
    staffOptions.value = (staffRes.data || []).filter((s) => s.isActive)
  } catch {
    // handled by http.js
  } finally {
    loading.value = false
  }
}

// ====== 載入編輯資料 ======
const loadSchedule = async () => {
  if (!isEdit.value) return
  try {
    loading.value = true
    const res = await maintenanceApi.getScheduleById(route.params.id)
    const data = res.data
    Object.assign(form, {
      title: data.title,
      targetType: data.targetType,
      targetId: data.targetId,
      scheduleType: data.scheduleType,
      dayOfWeek: data.dayOfWeek,
      dayOfMonth: data.dayOfMonth,
      executeTime: data.executeTime,
      issueType: data.issueType,
      issuePriority: data.issuePriority,
      assignedStaffId: data.assignedStaffId,
      isActive: data.isActive,
    })
  } catch {
    router.push('/admin/maintenance/schedule')
  } finally {
    loading.value = false
  }
}

// ====== 監聽變更 ======
watch(
  () => form.targetType,
  () => {
    form.targetId = null
  },
)
watch(
  () => form.scheduleType,
  (newType) => {
    if (newType === 'DAILY') {
      form.dayOfWeek = null
      form.dayOfMonth = null
    } else if (newType === 'WEEKLY') {
      form.dayOfMonth = null
    } else if (newType === 'MONTHLY') {
      form.dayOfWeek = null
    }
  },
)

// ====== 步驟驗證 ======
const validateCurrentStep = async () => {
  const fieldsMap = {
    0: ['title', 'issueType', 'issuePriority'],
    1: ['targetType', 'targetId'],
    2: [
      'scheduleType',
      'executeTime',
      ...(form.scheduleType === 'WEEKLY' ? ['dayOfWeek'] : []),
      ...(form.scheduleType === 'MONTHLY' ? ['dayOfMonth'] : []),
    ],
    3: [],
    4: [],
  }
  const fields = fieldsMap[currentStep.value]
  if (!fields.length) return true

  try {
    await formRef.value.validateField(fields)
    return true
  } catch {
    return false
  }
}

const nextStep = async () => {
  if (await validateCurrentStep()) {
    currentStep.value = Math.min(currentStep.value + 1, steps.length - 1)
  }
}
const prevStep = () => {
  currentStep.value = Math.max(currentStep.value - 1, 0)
}

// ====== 提交表單 ======
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch {
    Swal.fire({
      icon: 'warning',
      title: '請檢查表單',
      text: '有必填欄位尚未填寫',
      confirmButtonColor: '#409eff',
    })
    return
  }

  submitting.value = true
  try {
    const payload = { ...form }
    if (isEdit.value) {
      await maintenanceApi.updateSchedule(route.params.id, payload)
      await Swal.fire({
        icon: 'success',
        title: '更新成功！',
        timer: 1500,
        showConfirmButton: false,
      })
    } else {
      await maintenanceApi.createSchedule(payload)
      await Swal.fire({
        icon: 'success',
        title: '建立成功！',
        timer: 1500,
        showConfirmButton: false,
      })
    }
    router.push('/admin/maintenance/schedule')
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => router.push('/admin/maintenance/schedule')

// ====== 取得目標名稱 ======
const getTargetName = computed(() => {
  if (!form.targetId) return '-'
  if (form.targetType === 'SPOT') {
    const spot = spotOptions.value.find((s) => s.spotId === form.targetId)
    return spot?.spotName || spot?.spotCode || `機台 #${form.targetId}`
  } else {
    const seat = seatOptions.value.find((s) => s.seatsId === form.targetId)
    return seat?.seatsName || seat?.serialNumber || `椅子 #${form.targetId}`
  }
})

const getStaffName = computed(() => {
  if (!form.assignedStaffId) return '(不指派)'
  const staff = staffOptions.value.find((s) => s.staffId === form.assignedStaffId)
  return staff?.staffName || `人員 #${form.assignedStaffId}`
})

onMounted(async () => {
  // FIX: 編輯模式下，檢查 route.params.id 是否為有效數字
  // 防止使用者手動輸入 URL 帶入非數字 ID（如 DEMO_xxx）導致後端 400/500 錯誤
  if (isEdit.value && !isValidRouteId.value) {
    await Swal.fire({
      icon: 'error',
      title: '排程 ID 異常',
      html: '<p>無法讀取此排程資料。</p><p style="color:#909399;font-size:13px;">請返回列表頁面。</p>',
      confirmButtonColor: '#f56c6c',
    })
    router.replace('/admin/maintenance/schedule')
    return
  }

  await loadOptions()
  await loadSchedule()
})
</script>

<template>
  <div class="schedule-form-container">
    <section class="content-header">
      <div class="container-fluid">
        <div class="row align-items-center">
          <div class="col-sm-6">
            <h1 class="page-title">
              <i
                :class="isEdit ? 'fas fa-edit' : 'fas fa-plus-circle'"
                class="mr-2"
                style="color: #409eff"
              ></i>
              {{ pageTitle }}
            </h1>
          </div>
          <div class="col-sm-6 text-right">
            <el-button @click="handleCancel">
              <i class="fas fa-arrow-left mr-1"></i> 返回列表
            </el-button>
          </div>
        </div>
      </div>
    </section>

    <section class="content">
      <div class="container-fluid">
        <el-card shadow="hover" class="form-card" v-loading="loading">
          <el-steps :active="currentStep" finish-status="success" align-center class="mb-6">
            <el-step v-for="(step, idx) in steps" :key="idx" :title="step.title">
              <template #icon
                ><el-icon><component :is="step.icon" /></el-icon
              ></template>
            </el-step>
          </el-steps>

          <el-form
            ref="formRef"
            :model="form"
            :rules="dynamicRules"
            label-width="100px"
            label-position="top"
          >
            <!-- Step 0 -->
            <div v-show="currentStep === 0" class="step-content">
              <div class="step-header">
                <i class="fas fa-info-circle"></i>
                <span>填寫排程基本資訊</span>
              </div>

              <el-form-item label="排程標題" prop="title">
                <el-input
                  v-model="form.title"
                  placeholder="例如：每日機台清潔"
                  maxlength="100"
                  show-word-limit
                  clearable
                >
                  <template #prefix><i class="fas fa-tag"></i></template>
                </el-input>
              </el-form-item>

              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="問題類型" prop="issueType">
                    <el-select
                      v-model="form.issueType"
                      placeholder="選擇或輸入"
                      filterable
                      allow-create
                      style="width: 100%"
                    >
                      <el-option
                        v-for="type in issueTypeOptions"
                        :key="type"
                        :label="type"
                        :value="type"
                      />
                    </el-select>
                  </el-form-item>
                </el-col>

                <el-col :span="12">
                  <el-form-item label="優先級" prop="issuePriority">
                    <el-radio-group v-model="form.issuePriority" class="priority-radio">
                      <el-radio-button
                        v-for="opt in priorityOptions"
                        :key="opt.value"
                        :value="opt.value"
                        :data-priority="opt.value"
                      >
                        {{ opt.label }}
                      </el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item label="啟用狀態">
                <el-switch
                  v-model="form.isActive"
                  active-text="啟用"
                  inactive-text="停用"
                  active-color="#67c23a"
                  inline-prompt
                />
              </el-form-item>
            </div>

            <!-- Step 1 -->
            <div v-show="currentStep === 1" class="step-content">
              <div class="step-header">
                <i class="fas fa-crosshairs"></i>
                <span>選擇維護目標</span>
              </div>

              <el-form-item label="目標類型" prop="targetType">
                <div class="target-type-cards">
                  <div
                    class="target-card"
                    :class="{ active: form.targetType === 'SPOT' }"
                    @click="form.targetType = 'SPOT'"
                  >
                    <i class="fas fa-desktop"></i>
                    <span>機台</span>
                    <small>維護據點設備</small>
                  </div>
                  <div
                    class="target-card"
                    :class="{ active: form.targetType === 'SEAT' }"
                    @click="form.targetType = 'SEAT'"
                  >
                    <i class="fas fa-chair"></i>
                    <span>椅子</span>
                    <small>維護座椅設備</small>
                  </div>
                </div>
              </el-form-item>

              <el-form-item label="選擇目標" prop="targetId">
                <el-select
                  v-if="form.targetType === 'SPOT'"
                  v-model="form.targetId"
                  placeholder="搜尋並選擇機台"
                  filterable
                  style="width: 100%"
                >
                  <el-option
                    v-for="spot in spotOptions"
                    :key="spot.spotId"
                    :label="`${spot.spotName || spot.spotCode || '機台'} (ID: ${spot.spotId})`"
                    :value="spot.spotId"
                  >
                    <div class="option-item">
                      <i class="fas fa-desktop mr-2"></i
                      >{{ spot.spotName || spot.spotCode || '機台' }}
                      <el-tag size="small" class="ml-2">ID: {{ spot.spotId }}</el-tag>
                    </div>
                  </el-option>
                </el-select>

                <el-select
                  v-else
                  v-model="form.targetId"
                  placeholder="搜尋並選擇椅子"
                  filterable
                  style="width: 100%"
                >
                  <el-option
                    v-for="seat in seatOptions"
                    :key="seat.seatsId"
                    :label="`${seat.seatsName || seat.serialNumber || '椅子'} (ID: ${seat.seatsId})`"
                    :value="seat.seatsId"
                  >
                    <div class="option-item">
                      <i class="fas fa-chair mr-2"></i
                      >{{ seat.seatsName || seat.serialNumber || '椅子' }}
                      <el-tag size="small" class="ml-2">ID: {{ seat.seatsId }}</el-tag>
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>
            </div>

            <!-- Step 2 -->
            <div v-show="currentStep === 2" class="step-content">
              <div class="step-header">
                <i class="fas fa-clock"></i>
                <span>設定執行頻率</span>
              </div>

              <el-form-item label="排程類型" prop="scheduleType">
                <div class="schedule-type-cards">
                  <div
                    v-for="opt in scheduleTypeOptions"
                    :key="opt.value"
                    class="schedule-card"
                    :class="{ active: form.scheduleType === opt.value }"
                    @click="form.scheduleType = opt.value"
                  >
                    <i :class="opt.icon"></i>
                    <span>{{ opt.label }}</span>
                    <small>{{ opt.desc }}</small>
                  </div>
                </div>
              </el-form-item>

              <el-row :gutter="20">
                <el-col :span="12" v-if="form.scheduleType === 'WEEKLY'">
                  <el-form-item label="星期幾" prop="dayOfWeek">
                    <el-select v-model="form.dayOfWeek" placeholder="選擇星期" style="width: 100%">
                      <el-option
                        v-for="opt in dayOfWeekOptions"
                        :key="opt.value"
                        :label="opt.label"
                        :value="opt.value"
                      />
                    </el-select>
                  </el-form-item>
                </el-col>

                <el-col :span="12" v-if="form.scheduleType === 'MONTHLY'">
                  <el-form-item label="每月幾號" prop="dayOfMonth">
                    <el-input-number
                      v-model="form.dayOfMonth"
                      :min="1"
                      :max="31"
                      style="width: 100%"
                    />
                  </el-form-item>
                </el-col>

                <el-col :span="12">
                  <el-form-item label="執行時間" prop="executeTime">
                    <el-time-picker
                      v-model="form.executeTime"
                      format="HH:mm"
                      value-format="HH:mm:ss"
                      placeholder="選擇時間"
                      style="width: 100%"
                    />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-alert type="info" :closable="false" show-icon class="mt-4">
                <template #title>
                  <span v-if="form.executeTime">預覽：{{ formatScheduleDetail(form) }}</span>
                  <span v-else>請選擇執行時間</span>
                </template>
              </el-alert>
            </div>

            <!-- Step 3 -->
            <div v-show="currentStep === 3" class="step-content">
              <div class="step-header">
                <i class="fas fa-user-cog"></i>
                <span>指派維護人員 (選填)</span>
              </div>

              <el-form-item label="維護人員">
                <el-select
                  v-model="form.assignedStaffId"
                  placeholder="選擇人員或留空"
                  clearable
                  filterable
                  style="width: 100%"
                >
                  <el-option
                    v-for="staff in staffOptions"
                    :key="staff.staffId"
                    :label="`${staff.staffName} (${staff.specialization || '未設定專長'})`"
                    :value="staff.staffId"
                  >
                    <div class="option-item">
                      <el-avatar :size="24" class="mr-2">{{
                        staff.staffName?.charAt(0)
                      }}</el-avatar>
                      {{ staff.staffName }}
                      <el-tag size="small" type="info" class="ml-2">{{
                        staff.specialization || '未設定'
                      }}</el-tag>
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>

              <el-alert type="warning" :closable="false" show-icon>
                <template #title>提示</template>
                <template #default
                  >若不指派，工單建立後狀態為「已回報」；若指派則為「已指派」</template
                >
              </el-alert>
            </div>

            <!-- Step 4 -->
            <div v-show="currentStep === 4" class="step-content">
              <div class="step-header">
                <i class="fas fa-check-circle"></i>
                <span>確認排程資訊</span>
              </div>

              <el-descriptions :column="2" border class="summary-desc">
                <el-descriptions-item label="排程標題" :span="2">
                  <strong>{{ form.title || '-' }}</strong>
                </el-descriptions-item>

                <el-descriptions-item label="問題類型">{{
                  form.issueType || '-'
                }}</el-descriptions-item>
                <el-descriptions-item label="優先級">
                  <el-tag :type="priorityConfig[form.issuePriority]?.tagType" size="small">
                    {{ priorityConfig[form.issuePriority]?.text }}
                  </el-tag>
                </el-descriptions-item>

                <el-descriptions-item label="目標類型">
                  <el-tag :type="form.targetType === 'SPOT' ? 'primary' : 'warning'" size="small">
                    {{ form.targetType === 'SPOT' ? '機台' : '椅子' }}
                  </el-tag>
                </el-descriptions-item>

                <el-descriptions-item label="目標名稱">{{ getTargetName }}</el-descriptions-item>

                <el-descriptions-item label="執行頻率" :span="2">
                  <el-tag :type="scheduleTypeConfig[form.scheduleType]?.tagType" effect="plain">
                    <i :class="scheduleTypeConfig[form.scheduleType]?.icon" class="mr-1"></i>
                    {{ formatScheduleDetail(form) }}
                  </el-tag>
                </el-descriptions-item>

                <el-descriptions-item label="指派人員">{{ getStaffName }}</el-descriptions-item>
                <el-descriptions-item label="啟用狀態">
                  <el-tag :type="form.isActive ? 'success' : 'info'" size="small">
                    {{ form.isActive ? '啟用' : '停用' }}
                  </el-tag>
                </el-descriptions-item>
              </el-descriptions>
            </div>

            <!-- Buttons -->
            <div class="form-actions">
              <el-button @click="handleCancel">取消</el-button>
              <div class="action-right">
                <el-button v-if="currentStep > 0" @click="prevStep">
                  <i class="fas fa-chevron-left mr-1"></i> 上一步
                </el-button>

                <el-button v-if="currentStep < steps.length - 1" type="primary" @click="nextStep">
                  下一步 <i class="fas fa-chevron-right ml-1"></i>
                </el-button>

                <el-button v-else type="success" :loading="submitting" @click="handleSubmit">
                  <i class="fas fa-save mr-1"></i> {{ isEdit ? '儲存變更' : '建立排程' }}
                </el-button>
              </div>
            </div>
          </el-form>
        </el-card>
      </div>
    </section>
  </div>
</template>

<style scoped>
.schedule-form-container {
  padding: 20px;
}

.page-title {
  font-size: 1.6rem;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  margin: 0;
}

.form-card {
  border-radius: 12px;
  max-width: 800px;
  margin: 0 auto;
}

.mb-6 {
  margin-bottom: 2rem;
}
.step-content {
  min-height: 300px;
}

.step-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.1rem;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 2px solid #409eff;
}

/* 目標/排程卡片 */
.target-type-cards,
.schedule-type-cards {
  display: flex;
  gap: 16px;
}
.target-card,
.schedule-card {
  flex: 1;
  padding: 20px;
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}
.target-card:hover,
.schedule-card:hover {
  border-color: #409eff;
  background: rgba(64, 158, 255, 0.05);
}
.target-card.active,
.schedule-card.active {
  border-color: #409eff;
  background: rgba(64, 158, 255, 0.1);
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.2);
}
.target-card i,
.schedule-card i {
  font-size: 2rem;
  color: #409eff;
  display: block;
  margin-bottom: 8px;
}
.target-card span,
.schedule-card span {
  font-size: 1rem;
  font-weight: 600;
  display: block;
}
.target-card small,
.schedule-card small {
  font-size: 12px;
  color: #909399;
  display: block;
  margin-top: 4px;
}

.option-item {
  display: flex;
  align-items: center;
}

/* ✅ 優先級 Radio：修正文字被吃掉 + 不同顏色 */
.priority-radio {
  width: 100%;
}
.priority-radio :deep(.el-radio-button) {
  flex: 1;
}
.priority-radio :deep(.el-radio-button__inner) {
  width: 100%;
}

.priority-radio :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  color: #fff !important;
  border-color: transparent !important;
}

/* ✅ 依不同優先級上色 */
.priority-radio :deep([data-priority='LOW'].is-active .el-radio-button__inner) {
  background: #909399 !important;
}
.priority-radio :deep([data-priority='NORMAL'].is-active .el-radio-button__inner) {
  background: #409eff !important;
}
.priority-radio :deep([data-priority='HIGH'].is-active .el-radio-button__inner) {
  background: #e6a23c !important;
}
.priority-radio :deep([data-priority='URGENT'].is-active .el-radio-button__inner) {
  background: #f56c6c !important;
}

.summary-desc {
  margin-top: 16px;
}
.summary-desc :deep(.el-descriptions__label) {
  width: 100px;
  font-weight: 500;
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 24px;
  margin-top: 24px;
  border-top: 1px solid #ebeef5;
}
.action-right {
  display: flex;
  gap: 12px;
}

.mr-1 {
  margin-right: 4px;
}
.mr-2 {
  margin-right: 8px;
}
.ml-1 {
  margin-left: 4px;
}
.ml-2 {
  margin-left: 8px;
}
.mt-4 {
  margin-top: 1rem;
}
</style>
