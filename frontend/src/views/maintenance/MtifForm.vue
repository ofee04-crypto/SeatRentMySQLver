<script setup>
import { ref, onMounted, reactive, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import maintenanceApi from '@/api/modules/maintenance'
import Swal from 'sweetalert2'
import { useTicketConfig } from '@/composables/maintenance/useTicketConfig'
import TicketTimeline from '@/components/maintenance/TicketTimeline.vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const ticketId = computed(() => Number(route.params.id))
const isEdit = computed(() => !isNaN(ticketId.value) && ticketId.value > 0)

const formRef = ref(null)
const timelineRef = ref(null)
const loading = ref(false)
const submitting = ref(false)
const formVisible = ref(false)
const activeStep = ref(0)

// ============ 圖片附件相關 ============
const uploadRef = ref(null) // el-upload ref
const attachments = ref([]) // 已上傳的附件清單
const fileList = ref([]) // 待上傳的檔案清單（新增模式暫存）
const attachmentNote = ref('') // 附件備註
const uploadingAttachments = ref(false) // 上傳中狀態
const showAttachmentSection = ref(false) // 是否顯示附件區塊（編輯模式立即顯示，新增模式在建單後顯示）

// 允許的圖片類型
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp']
const MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB
const MAX_FILE_COUNT = 5

// 維修目標類型：'spot' (機台) 或 'seat' (椅子)
const targetType = ref('spot')

const form = reactive({
  spotId: null,
  seatsId: null, // 新增：椅子 ID
  issueType: '',
  issueDesc: '',
  issuePriority: 'NORMAL',
  assignedStaffId: null,
})

const staffOptions = ref([])
const spotOptions = ref([])
const seatOptions = ref([]) // 新增：椅子選項

const STAFF_UI_STATUS = Object.freeze({
  IDLE: 'IDLE',
  ASSIGNED: 'ASSIGNED',
  MAINTENANCE: 'MAINTENANCE',
  MAINTAINING: 'MAINTAINING',
})

const staffUiStatusMap = ref({}) // { [staffId]: STAFF_UI_STATUS.* }

const getStaffUiTag = (staff) => {
  const st = staffUiStatusMap.value?.[staff.staffId] || STAFF_UI_STATUS.IDLE
  if (st === STAFF_UI_STATUS.MAINTAINING) return { type: 'danger', text: '保養中' }
  if (st === STAFF_UI_STATUS.MAINTENANCE) return { type: 'warning', text: '維修中' }
  if (st === STAFF_UI_STATUS.ASSIGNED) return { type: 'primary', text: '已指派' }
  return { type: 'success', text: '空閒中' }
}

// 取得進行中工單：優先用 active 端點；若只有 all，就前端過濾
const fetchActiveTicketsForStaffStatus = async () => {
  const tryCalls = [
    () => maintenanceApi.getActiveTickets?.(),
    () => maintenanceApi.getTicketsActive?.(),
    () => maintenanceApi.getAllTickets?.(),
    () => maintenanceApi.getTicketsAll?.(),
  ]

  for (const call of tryCalls) {
    try {
      const res = await call()
      if (Array.isArray(res?.data)) return res.data
    } catch (e) {
      // ignore
    }
  }
  return []
}

const refreshStaffStatusTags = async () => {
  if (!Array.isArray(staffOptions.value) || staffOptions.value.length === 0) return

  const tickets = await fetchActiveTicketsForStaffStatus()

  // 只看會影響人員狀態的「進行中」狀態
  const active = (tickets || []).filter((t) => {
    const s = String(t?.issueStatus || '')
      .trim()
      .toUpperCase()
    return s === 'ASSIGNED' || s === 'UNDER_MAINTENANCE'
  })

  // 優先級：保養中 > 維修中 > 已指派 > 空閒中
  const map = {}
  const priority = {
    [STAFF_UI_STATUS.IDLE]: 0,
    [STAFF_UI_STATUS.ASSIGNED]: 1,
    [STAFF_UI_STATUS.MAINTENANCE]: 2,
    [STAFF_UI_STATUS.MAINTAINING]: 3,
  }
  const setIfHigher = (staffId, next) => {
    if (!staffId) return
    const cur = map[staffId] || STAFF_UI_STATUS.IDLE
    if (priority[next] > priority[cur]) map[staffId] = next
  }

  // default：啟用人員先當空閒
  for (const s of staffOptions.value) {
    if (s?.staffId != null && s.isActive === true) map[s.staffId] = STAFF_UI_STATUS.IDLE
  }

  for (const t of active) {
    const staffId = t.assignedStaffId
    if (!staffId) continue

    const issueStatus = String(t.issueStatus || '')
      .trim()
      .toUpperCase()
    const issueType = String(t.issueType || '').trim()

    if (issueType === '保養') {
      // 保養中：只要是保養工單且已指派/進行中
      setIfHigher(staffId, STAFF_UI_STATUS.MAINTAINING)
    } else if (issueStatus === 'UNDER_MAINTENANCE') {
      setIfHigher(staffId, STAFF_UI_STATUS.MAINTENANCE)
    } else if (issueStatus === 'ASSIGNED') {
      setIfHigher(staffId, STAFF_UI_STATUS.ASSIGNED)
    }
  }

  staffUiStatusMap.value = map
}
// ========= [新增結束] =========

// ★ Bug3 修復：記錄編輯時原始的 assignedStaffId，用於判斷是否有變更
const originalAssignedStaffId = ref(null)

// ★ Bug3 修復：定義可編輯的狀態
const EDITABLE_STATUSES = ['REPORTED', 'ASSIGNED']

// 原來定義的 issueTypeOptions
// const { issueTypeOptions: sharedIssueTypes } = useTicketConfig()
// const issueTypeOptions = sharedIssueTypes

//修改：使用 computed 過濾掉 "保養"
const { issueTypeOptions: sharedIssueTypes } = useTicketConfig()

const issueTypeOptions = computed(() => {
  // 假設 sharedIssueTypes 是一個 Ref 陣列
  // 過濾條件：value 不等於 '保養' (或是看您的 config 裡是用 key 還是 value，通常是 value)
  return (sharedIssueTypes.value || sharedIssueTypes).filter((t) => t.value !== '保養')
})

// 優先級配置（擴展版，含描述）
const priorityConfig = {
  LOW: { color: '#909399', bgColor: '#f4f4f5', icon: '🔵', text: '低優先', desc: '可稍後處理' },
  NORMAL: { color: '#409eff', bgColor: '#ecf5ff', icon: '🟢', text: '普通', desc: '正常排程處理' },
  HIGH: { color: '#e6a23c', bgColor: '#fdf6ec', icon: '🟠', text: '高優先', desc: '優先安排處理' },
  URGENT: { color: '#f56c6c', bgColor: '#fef0f0', icon: '🔴', text: '緊急', desc: '立即處理' },
}

// 驗證規則
const rules = computed(() => ({
  spotId:
    targetType.value === 'spot'
      ? [{ required: true, message: '請選擇一個機台', trigger: 'change' }]
      : [],
  seatsId:
    targetType.value === 'seat'
      ? [{ required: true, message: '請選擇一張椅子', trigger: 'change' }]
      : [],
  issueType: [{ required: true, message: '請輸入或選擇問題類型', trigger: 'blur' }],
  issuePriority: [{ required: true, message: '請選擇優先級', trigger: 'change' }],
}))

// 計算表單完成度
const formProgress = computed(() => {
  let filled = 0
  if (targetType.value === 'spot' ? form.spotId : form.seatsId) filled++
  if (form.issueType) filled++
  if (form.issueDesc) filled++
  if (form.assignedStaffId) filled++
  return Math.round((filled / 4) * 100)
})

// 監聽表單變化，自動更新步驟指示
watch(
  () => (targetType.value === 'spot' ? form.spotId : form.seatsId),
  (val) => {
    if (val && activeStep.value === 0) activeStep.value = 1
  },
)
watch(
  () => form.issueType,
  (val) => {
    if (val && activeStep.value === 1) activeStep.value = 2
  },
)

// 切換維修類型時，清空已選擇的目標
watch(targetType, (newType) => {
  if (newType === 'spot') {
    form.seatsId = null
  } else {
    form.spotId = null
  }
})

onMounted(async () => {
  setTimeout(() => (formVisible.value = true), 100)

  loading.value = true
  try {
    // ★ Bug3 修復：先讀取工單資料，檢查狀態是否可編輯
    if (isEdit.value) {
      const ticketRes = await maintenanceApi.getTicketById(ticketId.value)
      const ticketData = ticketRes.data

      // ★ 問題A修復：使用正確的欄位名稱 issueStatus
      if (!EDITABLE_STATUSES.includes(ticketData.issueStatus)) {
        await Swal.fire({
          icon: 'warning', // ★ (2B) 改為 warning，不是系統錯誤
          title: '無法編輯',
          html: `
            <p style="color: #909399;">此工單狀態為「<b>${ticketData.issueStatus}</b>」，不允許編輯</p>
            <p style="color: #f56c6c; font-size: 13px; margin-top: 10px;">可編輯狀態：REPORTED, ASSIGNED</p>
          `,
          confirmButtonText: '返回列表',
        })
        router.push({ name: 'mtif-list' })
        return
      }

      // ★ Bug3 修復：記錄原始 assignedStaffId
      originalAssignedStaffId.value = ticketData.assignedStaffId

      // 載入其他資料
      const [spotRes, staffRes, seatRes] = await Promise.all([
        maintenanceApi.getAllSpots().catch(() => ({ data: [] })),
        maintenanceApi.getAllStaff().catch(() => ({ data: [] })), // 編輯時用 getAllStaff
        maintenanceApi.getAllSeats().catch(() => ({ data: [] })),
      ])

      spotOptions.value = Array.isArray(spotRes.data) ? spotRes.data : []
      staffOptions.value = staffRes.data || []
      await refreshStaffStatusTags()
      seatOptions.value = seatRes.data || []

      // ★ 如果原始人員已停用，要保留並顯示為 disabled
      if (ticketData.assignedStaffId) {
        const assignedStaff = staffOptions.value.find(
          (s) => s.staffId === ticketData.assignedStaffId,
        )
        if (assignedStaff && !assignedStaff.isActive) {
          assignedStaff.staffName = assignedStaff.staffName + ' (已停用)'
          assignedStaff.disabled = true
        }
      }

      // 賦值表單
      Object.assign(form, ticketData)
      // 根據資料判斷維修類型
      if (ticketData.seatsId) {
        targetType.value = 'seat'
      } else {
        targetType.value = 'spot'
      }
      activeStep.value = 3

      // === [編輯模式] 載入附件清單 ===
      showAttachmentSection.value = true
      await loadAttachments(ticketId.value)
    } else {
      // ★ Bug2 修復：建立時只載入啟用人員
      const [spotRes, staffRes, seatRes] = await Promise.all([
        maintenanceApi.getAllSpots().catch(() => ({ data: [] })),
        maintenanceApi.getActiveStaff().catch(() => ({ data: [] })), // ★ 改用 getActiveStaff
        maintenanceApi.getAllSeats().catch(() => ({ data: [] })),
      ])

      spotOptions.value = Array.isArray(spotRes.data) ? spotRes.data : []
      staffOptions.value = staffRes.data || []
      await refreshStaffStatusTags()
      seatOptions.value = seatRes.data || []

      if (spotOptions.value.length > 0) form.spotId = spotOptions.value[0].spotId
    }
  } catch (error) {
    console.error('Failed to load form data:', error)
    Swal.fire('錯誤', '載入失敗，請稍後再試', 'error')
    router.push({ name: 'mtif-list' })
  } finally {
    loading.value = false
  }
})

const selectIssueType = (type) => {
  form.issueType = type.value
}

// ============ 圖片附件功能 ============

/**
 * 載入工單附件清單
 */
const loadAttachments = async (ticketIdValue) => {
  try {
    const res = await maintenanceApi.getTicketAttachments(ticketIdValue)
    attachments.value = res.data || []
  } catch (error) {
    console.error('載入附件失敗:', error)
  }
}

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
    ElMessage.error(`檔案過大：${file.name}（最大 5MB）`)
    return false
  }

  // 檢查數量限制
  const currentCount = isEdit.value ? attachments.value.length : fileList.value.length
  if (currentCount >= MAX_FILE_COUNT) {
    ElMessage.error(`最多只能上傳 ${MAX_FILE_COUNT} 張圖片`)
    return false
  }

  return true
}

/**
 * 檔案選擇變更（新增模式：暫存到 fileList）
 */
const handleFileChange = (file, uploadFileList) => {
  if (isEdit.value) {
    // 編輯模式：直接上傳
    uploadAttachmentsToServer([file.raw])
  } else {
    // 新增模式：暫存到 fileList
    fileList.value = uploadFileList
  }
}

/**
 * 移除檔案（新增模式：從 fileList 移除）
 */
const handleRemoveFile = (file) => {
  const index = fileList.value.findIndex(f => f.uid === file.uid)
  if (index > -1) {
    fileList.value.splice(index, 1)
  }
}

/**
 * 上傳附件到伺服器
 */
const uploadAttachmentsToServer = async (files) => {
  if (!files || files.length === 0) return

  uploadingAttachments.value = true
  try {
    const res = await maintenanceApi.uploadTicketAttachments(
      ticketId.value,
      files,
      attachmentNote.value || null
    )

    ElMessage.success(`成功上傳 ${res.data.length} 張圖片`)
    
    // 重新載入附件清單
    await loadAttachments(ticketId.value)
    
    // 清空備註與 fileList
    attachmentNote.value = ''
    fileList.value = []
    
    // 清空 el-upload 的 fileList
    if (uploadRef.value) {
      uploadRef.value.clearFiles()
    }
  } catch (error) {
    console.error('上傳附件失敗:', error)
    const errorMsg = error?.response?.data?.message || '上傳失敗，請稍後再試'
    ElMessage.error(errorMsg)
  } finally {
    uploadingAttachments.value = false
  }
}

/**
 * 刪除附件
 */
const deleteAttachment = async (attachment) => {
  const result = await Swal.fire({
    title: '確認刪除圖片？',
    text: `將刪除圖片：${attachment.originalName}`,
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#f56c6c',
    cancelButtonColor: '#909399',
    confirmButtonText: '<i class="fas fa-trash-alt mr-1"></i> 確認刪除',
    cancelButtonText: '取消',
  })

  if (!result.isConfirmed) return

  try {
    await maintenanceApi.deleteTicketAttachment(attachment.attachmentId)
    ElMessage.success('圖片已刪除')
    
    // 重新載入附件清單
    await loadAttachments(ticketId.value)
  } catch (error) {
    console.error('刪除附件失敗:', error)
    const errorMsg = error?.response?.data?.message || '刪除失敗，請稍後再試'
    Swal.fire('錯誤', errorMsg, 'error')
  }
}

/**
 * 預覽圖片
 */
const previewImage = (attachment) => {
  Swal.fire({
    imageUrl: `http://localhost:8080${attachment.publicUrl}`,
    imageAlt: attachment.originalName,
    showCloseButton: true,
    showConfirmButton: false,
    width: 'auto',
  })
}

const submit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      // 1. 取得選中的人員名稱 (為了顯示確認窗)
      const selectedStaff = staffOptions.value.find((s) => s.staffId === form.assignedStaffId)

      // 2. 顯示確認視窗
      const confirmResult = await Swal.fire({
        title: isEdit.value ? '確認更新工單？' : '確認建立工單？',
        html: `
          <div style="text-align: left; padding: 10px 0;">
            <div style="display: grid; gap: 12px;">
              <div style="padding: 12px; background: #f5f7fa; border-radius: 10px;">
                <p style="margin: 0 0 8px; color: #909399; font-size: 12px;">問題類型</p>
                <p style="margin: 0; font-size: 16px; font-weight: 600;">${form.issueType}</p>
              </div>
              <div style="padding: 12px; background: ${priorityConfig[form.issuePriority].bgColor}; border-radius: 10px; border-left: 4px solid ${priorityConfig[form.issuePriority].color};">
                <p style="margin: 0 0 8px; color: #909399; font-size: 12px;">優先級</p>
                <p style="margin: 0; font-size: 16px; font-weight: 600; color: ${priorityConfig[form.issuePriority].color};">
                   ${priorityConfig[form.issuePriority].icon} ${priorityConfig[form.issuePriority].text}
                </p>
              </div>
              ${
                selectedStaff
                  ? `
              <div style="padding: 12px; background: #f0f9eb; border-radius: 10px;">
                <p style="margin: 0 0 8px; color: #909399; font-size: 12px;">指派人員</p>
                <p style="margin: 0; font-size: 16px; font-weight: 600; color: #67c23a;">
                  <i class="fas fa-user-check mr-1"></i> ${selectedStaff.staffName}
                </p>
              </div>
              `
                  : ''
              }
            </div>
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
        // ★ 關鍵修正：資料清洗（完整版）
        // 建立一個乾淨的物件，只包含後端需要的欄位
        const submitData = {
          spotId: form.spotId,
          seatsId: form.seatsId,
          issueType: form.issueType,
          issueDesc: form.issueDesc,
          issuePriority: form.issuePriority,
          assignedStaffId: form.assignedStaffId,
        }

        // 根據維修類型清除不需要的欄位
        if (targetType.value === 'spot') {
          submitData.seatsId = null
        } else if (targetType.value === 'seat') {
          submitData.spotId = null
        }

        if (isEdit.value) {
          // === [更新模式] ===
          // ★ 問題3修復：後端 updateTicket 已經處理 assignedStaffId 的更新與 Log 紀錄
          // 不需要額外呼叫 assignStaff API，完全依賴 updateTicket 即可
          await maintenanceApi.updateTicket(ticketId.value, submitData)

          await Swal.fire({
            icon: 'success',
            title: '更新成功！',
            text: '工單資料已更新',
            timer: 1200,
            showConfirmButton: false,
          })

          // 編輯模式不跳轉，留在當前頁面（可繼續管理附件）
        } else {
          // === [新增模式] ===
          const createRes = await maintenanceApi.createTicket(submitData)
          const newTicketId = createRes.data?.ticketId

          await Swal.fire({
            icon: 'success',
            title: '建立成功！',
            text: '新工單已建立',
            timer: 1500,
            showConfirmButton: false,
          })

          // 如果有選擇檔案，上傳附件
          if (fileList.value.length > 0 && newTicketId) {
            try {
              const files = fileList.value.map(f => f.raw)
              await maintenanceApi.uploadTicketAttachments(
                newTicketId,
                files,
                attachmentNote.value || null
              )
              ElMessage.success(`成功上傳 ${files.length} 張圖片`)
            } catch (error) {
              console.error('上傳附件失敗:', error)
              ElMessage.warning('工單已建立，但附件上傳失敗')
            }
          }

          // 跳轉回列表頁
          router.push({ name: 'mtif-list' })
        }
      } catch (error) {
        console.error('Submit failed:', error)
        // ★ Bug3 修復：顯示後端回傳的錯誤訊息
        const errorMsg = error?.response?.data?.message || '操作失敗，請稍後再試'
        Swal.fire('錯誤', errorMsg, 'error')
      } finally {
        submitting.value = false
      }
    }
  })
}

// ✅ DEMO 專用：一鍵帶入故障工單
const handleDemoFill = () => {
  // 1. 設定為機台模式
  targetType.value = 'spot'

  // 2. 2. 自動選取第一個 "營運中" 的機台 (修正邏輯：跳過維修中機台)
  if (spotOptions.value && spotOptions.value.length > 0) {
    // 取得第一個營運中機台
    const availableSpot = spotOptions.value.find((s) => s.spotStatus === '營運中')

    // 如果找到，帶入；否則顯示提示
    if (availableSpot) {
      form.spotId = availableSpot.spotId
    } else {
      // 如果全部都在維修中，顯示提示
      Swal.fire({
        icon: 'warning',
        title: '無可選擇的機台',
        text: '目前所有機台皆在維修中',
        toast: true,
        position: 'top-end',
        timer: 900,
      })
      return // 終止
    }
  }

  // 3. 填寫故障情境
  form.issueType = '機台故障異常' // 確保這個文字跟按鈕上的 value 一樣
  form.issueDesc = '機台運作時發出異音，且螢幕畫面閃爍，目前已先暫停使用，請盡快派人員來做檢查。'
  form.issuePriority = 'HIGH' // 設定為高優先

  // 4. (選填) 指派給第一個啟用的人員
  const activeStaff = staffOptions.value.find((s) => s.isActive)
  if (activeStaff) {
    form.assignedStaffId = activeStaff.staffId
  }

  // 5. 提示
  Swal.fire({
    icon: 'success',
    title: '新增資料成功',
    text: '資料已帶入',
    timer: 1500,
    showConfirmButton: false,
    toast: true,
    position: 'top-end',
  })
}

const handleCancel = async () => {
  if (form.issueType || form.issueDesc) {
    const result = await Swal.fire({
      title: '確定要離開嗎？',
      text: '您填寫的工單資料將不會被保存',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#e6a23c',
      cancelButtonColor: '#909399',
      confirmButtonText: '離開',
      cancelButtonText: '繼續編輯',
      showClass: { popup: 'animate__animated animate__fadeIn animate__faster' },
    })
    if (!result.isConfirmed) return
  }
  router.push('/admin/mtif-list')
}
</script>

<template>
  <div class="ticket-form-container">
    <!-- 頁面標題區 -->
    <section class="content-header">
      <div class="container-fluid">
        <transition name="slide-fade" appear>
          <div class="page-title-box">
            <div class="title-icon" :class="isEdit ? 'edit-mode' : 'add-mode'">
              <i :class="isEdit ? 'fas fa-ticket-alt' : 'fas fa-plus-circle'"></i>
            </div>
            <div class="title-content">
              <h1>{{ isEdit ? '編輯維修工單' : '建立新工單' }}</h1>
              <p class="subtitle">
                {{ isEdit ? '修改現有工單資訊' : '填寫問題詳情以建立維修工單' }}
              </p>
            </div>
            <div class="title-progress" v-if="!isEdit">
              <div class="progress-ring">
                <el-progress
                  type="circle"
                  :percentage="formProgress"
                  :width="60"
                  :stroke-width="6"
                  :color="formProgress === 100 ? '#67c23a' : '#409eff'"
                />
              </div>
              <span class="progress-text">完成度</span>
            </div>
          </div>
        </transition>
      </div>
    </section>

    <!-- 表單主體 -->
    <section class="content">
      <div class="container-fluid d-flex justify-content-center">
        <transition name="zoom-fade" appear>
          <el-card
            v-show="formVisible"
            shadow="hover"
            class="form-card"
            v-loading="loading"
            element-loading-text="載入中..."
          >
            <template #header>
              <div class="card-header-content">
                <div class="header-left">
                  <span class="header-icon">
                    <i class="fas fa-clipboard-list"></i>
                  </span>
                  <span class="header-text">工單資訊</span>
                  <el-tag v-if="isEdit" type="warning" effect="plain" size="small" class="ml-2">
                    #{{ ticketId }}
                  </el-tag>
                </div>
                <el-button class="cancel-btn" text type="info" @click="handleCancel">
                  <i class="fas fa-times mr-1"></i> 取消
                </el-button>
              </div>
            </template>

            <!-- 步驟指示器 -->
            <div class="steps-indicator" v-if="!isEdit">
              <el-steps :active="activeStep" finish-status="success" simple>
                <el-step title="選擇場地" icon="Location" />
                <el-step title="問題描述" icon="Edit" />
                <el-step title="設定優先級" icon="Flag" />
                <el-step title="指派人員" icon="User" />
              </el-steps>
            </div>

            <el-form
              ref="formRef"
              :model="form"
              :rules="rules"
              label-position="top"
              status-icon
              class="ticket-form"
            >
              <!-- 維修目標類型切換 -->
              <el-form-item class="form-item-animated">
                <template #label>
                  <span class="custom-label">
                    <i class="fas fa-wrench label-icon"></i> 維修目標
                    <span class="required-star">*</span>
                  </span>
                </template>
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

              <!-- 機台選擇 (當 targetType === 'spot') -->
              <el-form-item
                v-if="targetType === 'spot'"
                label="場地選擇"
                prop="spotId"
                class="form-item-animated"
              >
                <template #label>
                  <span class="custom-label">
                    <i class="fas fa-desktop label-icon"></i> 選擇機台
                    <span class="required-star">*</span>
                  </span>
                </template>
                <el-select
                  v-model="form.spotId"
                  placeholder="請選擇或搜尋機台..."
                  class="w-100"
                  filterable
                  :disabled="isEdit"
                  size="large"
                  popper-class="mt-spot-select-popper"
                >
                  <template #prefix>
                    <i class="fas fa-search"></i>
                  </template>

                  <el-option
                    v-for="spot in spotOptions"
                    :key="spot.spotId"
                    :label="`${spot.spotCode || spot.spotId} - ${spot.spotName} (${spot.spotStatus || '未知'})`"
                    :value="spot.spotId"
                    :disabled="spot.spotStatus && spot.spotStatus !== '營運中'"
                    style="height: auto; padding: 2px 8px"
                  >
                    <div class="spot-option" style="padding: 2px 0">
                      <span class="spot-code">{{ spot.spotCode || spot.spotId }}</span>
                      <span class="spot-name">{{ spot.spotName }}</span>

                      <el-tag
                        :type="
                          !spot.spotStatus || spot.spotStatus === '營運中' ? 'success' : 'danger'
                        "
                        size="small"
                        effect="plain"
                        style="margin-left: 8px"
                      >
                        {{ spot.spotStatus || '未知' }}
                      </el-tag>
                    </div>
                  </el-option>
                </el-select>
                <small v-if="spotOptions.length === 0" class="text-warning">
                  <i class="fas fa-exclamation-triangle mr-1"></i> 無可用機台資料
                </small>
              </el-form-item>

              <!-- 椅子選擇 (當 targetType === 'seat') -->
              <el-form-item
                v-if="targetType === 'seat'"
                label="椅子選擇"
                prop="seatsId"
                class="form-item-animated"
              >
                <template #label>
                  <span class="custom-label">
                    <i class="fas fa-chair label-icon"></i> 選擇椅子
                    <span class="required-star">*</span>
                  </span>
                </template>
                <el-select
                  v-model="form.seatsId"
                  placeholder="請選擇或搜尋椅子..."
                  class="w-100"
                  filterable
                  :disabled="isEdit"
                  size="large"
                >
                  <template #prefix>
                    <i class="fas fa-search"></i>
                  </template>
                  <el-option
                    v-for="seat in seatOptions"
                    :key="seat.seatsId"
                    :label="`${seat.seatsName || seat.seatsId} (${seat.seatsType || '一般'})`"
                    :value="seat.seatsId"
                  >
                    <div class="seat-option">
                      <span class="seat-icon">🪑</span>
                      <div class="seat-info">
                        <span class="seat-name">{{
                          seat.seatsName || `椅子 #${seat.seatsId}`
                        }}</span>
                        <span class="seat-type"
                          >{{ seat.seatsType || '一般座椅' }} ·
                          {{ seat.seatsStatus || '正常' }}</span
                        >
                      </div>
                    </div>
                  </el-option>
                </el-select>
                <small v-if="seatOptions.length === 0" class="text-warning">
                  <i class="fas fa-exclamation-triangle mr-1"></i> 無可用椅子資料
                </small>
              </el-form-item>

              <!-- 問題類型 -->
              <el-form-item label="問題類型" prop="issueType" class="form-item-animated">
                <template #label>
                  <span class="custom-label">
                    <i class="fas fa-exclamation-circle label-icon"></i> 問題類型
                    <span class="required-star">*</span>
                  </span>
                </template>

                <!-- 快速選擇區 -->
                <div class="quick-select-grid">
                  <div
                    v-for="type in issueTypeOptions"
                    :key="type.value"
                    class="quick-select-item"
                    :class="{ active: form.issueType === type.value }"
                    @click="selectIssueType(type)"
                  >
                    <span class="item-icon">{{ type.icon }}</span>
                    <span class="item-text">{{ type.value }}</span>
                  </div>
                </div>

                <el-input
                  v-model="form.issueType"
                  placeholder="或自行輸入問題類型..."
                  size="large"
                  class="mt-2"
                  clearable
                />
              </el-form-item>

              <!-- 詳細描述 -->
              <el-form-item label="詳細描述" prop="issueDesc" class="form-item-animated">
                <template #label>
                  <span class="custom-label">
                    <i class="fas fa-align-left label-icon"></i> 詳細描述
                  </span>
                </template>
                <el-input
                  v-model="form.issueDesc"
                  type="textarea"
                  :rows="4"
                  placeholder="請詳細描述問題狀況，例如：故障位置、發生時間、嚴重程度等..."
                  show-word-limit
                  maxlength="1000"
                  class="custom-textarea"
                />
              </el-form-item>

              <!-- 優先級選擇 -->
              <el-form-item label="優先級" prop="issuePriority" class="form-item-animated">
                <template #label>
                  <span class="custom-label">
                    <i class="fas fa-flag label-icon"></i> 優先級
                    <span class="required-star">*</span>
                  </span>
                </template>
                <div class="priority-cards">
                  <div
                    v-for="(config, key) in priorityConfig"
                    :key="key"
                    class="priority-card"
                    :class="{ active: form.issuePriority === key }"
                    :style="{
                      '--card-color': config.color,
                      '--card-bg': config.bgColor,
                    }"
                    @click="form.issuePriority = key"
                  >
                    <span class="priority-icon">{{ config.icon }}</span>
                    <span class="priority-text">{{ config.text }}</span>
                    <span class="priority-desc">{{ config.desc }}</span>
                  </div>
                </div>
              </el-form-item>

              <!-- 指派維修員 -->
              <el-form-item label="指派維修員" prop="assignedStaffId" class="form-item-animated">
                <template #label>
                  <span class="custom-label">
                    <i class="fas fa-user-cog label-icon"></i> 指派維修員
                    <el-tag type="info" size="small" class="ml-2">選填</el-tag>
                  </span>
                </template>
                <el-select
                  popper-class="mt-staff-select-popper"
                  v-model="form.assignedStaffId"
                  placeholder="暫不指派，稍後可編輯"
                  class="w-100"
                  filterable
                  clearable
                  size="large"
                >
                  <!-- ★ 問題2修復：過濾只顯示啟用人員，或當前工單已指派的人員（即使已停用） -->
                  <el-option
                    v-for="s in staffOptions.filter(
                      (staff) =>
                        staff.isActive === true || staff.staffId === originalAssignedStaffId,
                    )"
                    :key="s.staffId"
                    :label="`${s.staffName}${s.isActive === false ? ' (已停用)' : ''} (${s.staffCompany || '外部'})`"
                    :value="s.staffId"
                    :disabled="s.isActive === false && s.staffId !== form.assignedStaffId"
                  >
                    <!-- ✅【修正#3】改成左右兩欄：左邊姓名公司、右邊顯示狀態 -->
                    <div class="staff-option">
                      <div class="staff-left">
                        <div
                          class="staff-avatar"
                          :style="{ opacity: s.isActive === false ? 0.5 : 1 }"
                        >
                          {{ s.staffName?.charAt(0) }}
                        </div>

                        <div class="staff-info">
                          <span
                            class="staff-name"
                            :style="{ color: s.isActive === false ? '#909399' : '' }"
                          >
                            {{ s.staffName }}
                            <el-tag
                              v-if="s.isActive === false"
                              type="info"
                              size="small"
                              class="ml-1"
                              >已停用</el-tag
                            >
                          </span>
                          <span class="staff-company">{{ s.staffCompany || '外部人員' }}</span>
                        </div>
                      </div>

                      <!-- 【修正#3】顯示：已指派 / 維修中 / 保養中 / 空閒中 -->
                      <div class="staff-right">
                        <el-tag
                          :type="getStaffUiTag(s).type"
                          size="small"
                          effect="plain"
                          :disable-transitions="true"
                        >
                          {{ getStaffUiTag(s).text }}
                        </el-tag>
                      </div>
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>

              <!-- ============ 圖片附件區塊 ============ -->
              <el-form-item 
                v-if="isEdit" 
                label="圖片附件" 
                class="form-item-animated"
              >
                <template #label>
                  <span class="custom-label">
                    <i class="fas fa-images label-icon"></i> 圖片附件
                    <el-tag type="info" size="small" class="ml-2">選填</el-tag>
                  </span>
                </template>

                <!-- 上傳區 -->
                <div class="attachment-upload-area">
                  <el-upload
                    ref="uploadRef"
                    :auto-upload="false"
                    :on-change="handleFileChange"
                    :on-remove="handleRemoveFile"
                    :before-upload="beforeUpload"
                    :file-list="fileList"
                    accept="image/jpeg,image/png,image/webp"
                    list-type="picture-card"
                    :limit="MAX_FILE_COUNT"
                    :disabled="uploadingAttachments"
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
                        支援 JPG、PNG、WEBP 格式，單張最大 5MB，最多 {{ MAX_FILE_COUNT }} 張
                      </div>
                    </template>
                  </el-upload>

                  <!-- 備註輸入 -->
                  <el-input
                    v-if="fileList.length > 0"
                    v-model="attachmentNote"
                    placeholder="選填：為這批圖片加上備註..."
                    class="mt-2"
                    clearable
                    maxlength="200"
                    show-word-limit
                  />

                  <!-- 立即上傳按鈕（編輯模式） -->
                  <el-button
                    v-if="isEdit && fileList.length > 0"
                    type="primary"
                    :loading="uploadingAttachments"
                    @click="uploadAttachmentsToServer(fileList.map(f => f.raw))"
                    class="mt-2"
                  >
                    <i class="fas fa-upload mr-1"></i>
                    {{ uploadingAttachments ? '上傳中...' : '立即上傳' }}
                  </el-button>
                </div>

                <!-- 已上傳的附件清單 -->
                <div v-if="attachments.length > 0" class="attachments-list mt-3">
                  <el-divider content-position="left">
                    <span style="color: #909399; font-size: 13px;">
                      <i class="fas fa-paperclip mr-1"></i>
                      已上傳附件 ({{ attachments.length }})
                    </span>
                  </el-divider>

                  <div class="attachments-grid">
                    <div 
                      v-for="att in attachments" 
                      :key="att.attachmentId" 
                      class="attachment-item"
                    >
                      <!-- 圖片預覽 -->
                      <div class="attachment-preview" @click="previewImage(att)">
                        <img 
                          :src="`http://localhost:8080${att.publicUrl}`" 
                          :alt="att.originalName"
                          @error="$event.target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgZmlsbD0iI2YwZjBmMCIvPjx0ZXh0IHg9IjUwIiB5PSI1MCIgZm9udC1zaXplPSIxNCIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iIGZpbGw9IiM5OTkiPu+/ve+/ve+/vTwvdGV4dD48L3N2Zz4='"
                        />
                        <div class="preview-overlay">
                          <i class="fas fa-search-plus"></i>
                        </div>
                      </div>

                      <!-- 附件資訊 -->
                      <div class="attachment-info">
                        <div class="attachment-name" :title="att.originalName">
                          {{ att.originalName }}
                        </div>
                        <div class="attachment-meta">
                          <span class="meta-item">
                            <i class="fas fa-clock"></i>
                            {{ att.createdAt }}
                          </span>
                          <span class="meta-item">
                            <i class="fas fa-hdd"></i>
                            {{ (att.fileSize / 1024).toFixed(1) }} KB
                          </span>
                        </div>
                        <div v-if="att.note" class="attachment-note">
                          <i class="fas fa-comment-dots"></i>
                          {{ att.note }}
                        </div>
                      </div>

                      <!-- 刪除按鈕 -->
                      <el-button
                        type="danger"
                        size="small"
                        circle
                        class="delete-btn"
                        @click="deleteAttachment(att)"
                        :title="`刪除 ${att.originalName}`"
                      >
                        <i class="fas fa-trash-alt"></i>
                      </el-button>
                    </div>
                  </div>
                </div>
              </el-form-item>

              <!-- 新增模式：提示可在建立後上傳附件 -->
              <el-form-item 
                v-else 
                label="圖片附件" 
                class="form-item-animated"
              >
                <template #label>
                  <span class="custom-label">
                    <i class="fas fa-images label-icon"></i> 圖片附件
                    <el-tag type="info" size="small" class="ml-2">選填</el-tag>
                  </span>
                </template>

                <!-- 新增模式的上傳區 -->
                <div class="attachment-upload-area">
                  <el-upload
                    ref="uploadRef"
                    :auto-upload="false"
                    :on-change="handleFileChange"
                    :on-remove="handleRemoveFile"
                    :before-upload="beforeUpload"
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
                        建立工單後將自動上傳所選圖片（支援 JPG、PNG、WEBP，單張最大 5MB）
                      </div>
                    </template>
                  </el-upload>

                  <!-- 備註輸入 -->
                  <el-input
                    v-if="fileList.length > 0"
                    v-model="attachmentNote"
                    placeholder="選填：為這批圖片加上備註..."
                    class="mt-2"
                    clearable
                    maxlength="200"
                    show-word-limit
                  />
                </div>
              </el-form-item>

              <!-- 分隔線 -->
              <el-divider>
                <i class="fas fa-paper-plane"></i>
              </el-divider>

              <!-- 按鈕區 -->
              <el-form-item class="form-actions">
                <div
                  style="
                    display: flex;
                    justify-content: space-between;
                    width: 100%;
                    align-items: center;
                  "
                >
                  <div class="left-buttons">
                    <el-button
                      type="primary"
                      @click="submit"
                      :loading="submitting"
                      size="large"
                      class="submit-btn"
                    >
                      <i class="fas fa-paper-plane mr-2" v-if="!submitting"></i>
                      <span>{{ submitting ? '處理中...' : isEdit ? '更新工單' : '建立工單' }}</span>
                    </el-button>

                    <el-button @click="handleCancel" size="large" class="back-btn ml-3">
                      <i class="fas fa-arrow-left mr-2"></i> 返回列表
                    </el-button>
                  </div>

                  <div class="right-buttons">
                    <el-button
                      type="warning"
                      link
                      @click="handleDemoFill"
                      style="opacity: 0.5; font-weight: normal"
                      onmouseover="this.style.opacity=1"
                      onmouseout="this.style.opacity=0.5"
                    >
                      <i class="fas fa-magic mr-1"></i> 一鍵帶入
                    </el-button>
                  </div>
                </div>
              </el-form-item>
            </el-form>
          </el-card>
        </transition>
      </div>
    </section>
  </div>
  <!-- 維修歷程紀錄 -->
  <div class="page-container">
    <el-card v-if="ticketId" class="mt-4" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>維修歷程紀錄</span>
        </div>
      </template>
      <TicketTimeline ref="timelineRef" :ticketId="ticketId" />
    </el-card>
  </div>
</template>

<style scoped>
.ticket-form-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
  padding-bottom: 40px;
}

/* ============ 圖片附件樣式 ============ */
.attachment-upload-area {
  width: 100%;
}

.upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #8c939d;
  font-size: 14px;
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
  margin-top: 8px;
  line-height: 1.5;
}

.attachments-list {
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.attachments-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.attachment-item {
  position: relative;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
  transition: all 0.3s ease;
}

.attachment-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.attachment-preview {
  position: relative;
  width: 100%;
  height: 180px;
  cursor: pointer;
  overflow: hidden;
  background: #f5f7fa;
}

.attachment-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.attachment-item:hover .attachment-preview img {
  transform: scale(1.05);
}

.preview-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.attachment-preview:hover .preview-overlay {
  opacity: 1;
}

.preview-overlay i {
  font-size: 32px;
  color: white;
}

.attachment-info {
  padding: 12px;
}

.attachment-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.attachment-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.attachment-note {
  font-size: 12px;
  color: #606266;
  background: #f0f9ff;
  padding: 6px 8px;
  border-radius: 4px;
  border-left: 3px solid #409eff;
  margin-top: 8px;
  line-height: 1.4;
}

.attachment-note i {
  color: #409eff;
  margin-right: 4px;
}

.delete-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.attachment-item:hover .delete-btn {
  opacity: 1;
}

.content-header {
  padding: 20px 1rem;
}

.page-title-box {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
}

.title-icon {
  width: 60px;
  height: 60px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  color: white;
  transition: transform 0.3s ease;
}

.title-icon:hover {
  transform: scale(1.1) rotate(5deg);
}

.title-icon.add-mode {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
}

.title-icon.edit-mode {
  background: linear-gradient(135deg, #e6a23c 0%, #f3d19e 100%);
}

.title-content {
  flex: 1;
}

.title-content h1 {
  margin: 0;
  font-size: 1.6rem;
  font-weight: 700;
  color: #303133;
}

.title-content .subtitle {
  margin: 6px 0 0;
  font-size: 0.9rem;
  color: #909399;
}

.title-progress {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.progress-text {
  font-size: 12px;
  color: #909399;
}

/* 表單卡片 */
.form-card {
  width: 100%;
  max-width: 800px;
  border-radius: 16px;
  overflow: hidden;
  border: none;
  transition: all 0.3s ease;
}

.form-card:hover {
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
}

.card-header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
}

.header-text {
  font-weight: 600;
  font-size: 1.1rem;
  color: #303133;
}

/* 步驟指示器 */
.steps-indicator {
  padding: 16px 0 24px;
  border-bottom: 1px dashed #ebeef5;
  margin-bottom: 20px;
}

/* 表單樣式 */
.ticket-form {
  padding: 10px 20px;
}

.custom-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  color: #606266;
}

.label-icon {
  color: #409eff;
  font-size: 14px;
}

.required-star {
  color: #f56c6c;
  margin-left: 2px;
}

.form-item-animated {
  animation: fadeInUp 0.5s ease forwards;
  opacity: 0;
}

.form-item-animated:nth-child(1) {
  animation-delay: 0.1s;
}
.form-item-animated:nth-child(2) {
  animation-delay: 0.15s;
}
.form-item-animated:nth-child(3) {
  animation-delay: 0.2s;
}
.form-item-animated:nth-child(4) {
  animation-delay: 0.25s;
}
.form-item-animated:nth-child(5) {
  animation-delay: 0.3s;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 場地選項樣式 */
.spot-option {
  display: flex;
  align-items: center;
  gap: 10px;
}

.spot-code {
  background: #409eff;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.spot-name {
  color: #606266;
}

/* 維修目標類型切換 */
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
  padding: 20px;
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
  background: linear-gradient(135deg, #ecf5ff 0%, #e6f4ff 100%);
  border-color: #409eff;
  box-shadow: 0 4px 15px rgba(64, 158, 255, 0.2);
}

.target-type-option i {
  font-size: 28px;
  color: #909399;
  transition: all 0.3s ease;
}

.target-type-option.active i {
  color: #409eff;
  transform: scale(1.1);
}

.target-type-option span {
  font-weight: 600;
  font-size: 14px;
  color: #606266;
}

.target-type-option.active span {
  color: #409eff;
}

/* 椅子選項樣式 */
.seat-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0;
}

.seat-icon {
  font-size: 20px;
}

.seat-info {
  display: flex;
  flex-direction: column;
}

.seat-name {
  font-weight: 500;
  color: #303133;
}

.seat-type {
  font-size: 12px;
  color: #909399;
}

/* 快速選擇區 - 橫排設計 */
.quick-select-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.quick-select-item {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: #f5f7fa;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
  flex-shrink: 0;
}

.quick-select-item:hover {
  background: #ecf5ff;
  transform: translateY(-2px);
}

.quick-select-item.active {
  background: #ecf5ff;
  border-color: #409eff;
}

.item-icon {
  font-size: 22px;
}

.item-text {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
  white-space: nowrap;
}

/* 優先級卡片 */
.priority-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

@media (max-width: 768px) {
  .priority-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

.priority-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 12px;
  background: var(--card-bg);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: 2px solid transparent;
}

.priority-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.priority-card.active {
  border-color: var(--card-color);
  transform: scale(1.05);
}

.priority-icon {
  font-size: 28px;
  margin-bottom: 8px;
}

.priority-text {
  font-weight: 600;
  color: var(--card-color);
  margin-bottom: 4px;
}

.priority-desc {
  font-size: 11px;
  color: #909399;
  text-align: center;
}

/* 維修員選項 */
/* 維修員選項：左右佈局 + 足夠高度，避免公司文字被蓋 */
.staff-option {
  display: flex;
  align-items: center;
  justify-content: space-between; /* 右側留給狀態 tag */
  gap: 12px;
  padding: 8px 6px;
  height: auto !important;
  line-height: 1.4;
}

/* 左側群組（頭像 + 文字） */
.staff-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0; /* ✅ 允許文字省略 */
  flex: 1;
}

.staff-avatar {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(103, 194, 58, 0.2);
}

.staff-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0; /*  允許省略 */
  overflow: hidden;
}

.staff-name {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  line-height: 1.25;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.staff-company {
  font-size: 12px;
  color: #909399;
  line-height: 1.2;
  margin-top: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 右側狀態 tag */
.staff-right {
  flex-shrink: 0;
  margin-left: 10px;
}

/* 按鈕區 */
.form-actions {
  margin-top: 20px;
}

.submit-btn {
  min-width: 160px;
  border-radius: 12px;
  font-weight: 600;
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
  border: none;
  transition: all 0.3s ease;
}

.submit-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(64, 158, 255, 0.4);
}

.back-btn {
  min-width: 120px;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.back-btn:hover {
  transform: translateX(-3px);
}

/* 過渡動畫 */
.slide-fade-enter-active {
  transition: all 0.4s ease-out;
}
.slide-fade-leave-active {
  transition: all 0.3s ease-in;
}
.slide-fade-enter-from {
  transform: translateX(-20px);
  opacity: 0;
}
.slide-fade-leave-to {
  transform: translateX(20px);
  opacity: 0;
}

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

/* 輔助類 */
.w-100 {
  width: 100%;
}
.mt-2 {
  margin-top: 8px;
}
.ml-2 {
  margin-left: 8px;
}
.mr-1 {
  margin-right: 4px;
}
.mr-2 {
  margin-right: 8px;
}
.text-warning {
  color: #e6a23c;
}

:deep(.el-divider__text) {
  background: white;
  color: #c0c4cc;
}

.custom-textarea :deep(.el-textarea__inner) {
  border-radius: 10px;
}

/* 【修正#2】機台選單：縮小上下 padding，讓間隔不要太大 */
:deep(.mt-spot-select-popper .el-select-dropdown__item) {
  height: auto !important;
  line-height: 1.2 !important;
  padding-top: 4px !important;
  padding-bottom: 4px !important;
}

/* 【修正#3】維修員選單：保留較舒適的高度，避免兩行文字被蓋到 */
:deep(.mt-staff-select-popper .el-select-dropdown__item) {
  height: auto !important;
  line-height: 1.4 !important;
  padding-top: 10px !important;
  padding-bottom: 10px !important;
}
</style>
