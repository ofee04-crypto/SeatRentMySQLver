<script setup>
import { ref, onMounted, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import maintenanceApi from '@/api/modules/maintenance'
import Swal from 'sweetalert2'
import { useRouter } from 'vue-router'
import { usePagination } from '@/composables/maintenance/usePagination'
import { InfoFilled, Right, Delete, Check } from '@element-plus/icons-vue'
// ✅ 【修正】引入統一的人員狀態計算邏輯 (避免重複實作、確保統計卡與列表一致)
import {
  computeStaffStatus,
  computeStaffTaskStats,
  computeOverallStaffStats,
  isCompletedStatus,
} from '@/composables/maintenance/useStaffStatus'

const router = useRouter()
const staffList = ref([])
const searchText = ref('')
const loading = ref(true)
const pageVisible = ref(false)
const sortConfig = ref({ prop: 'staffId', order: 'ascending' })

// ✅ 修正：狀態中文化映射表（不改後端，僅前端顯示轉換）
const STATUS_LABEL = {
  REPORTED: '已回報',
  ASSIGNED: '已指派',
  UNDER_MAINTENANCE: '維修中',
  UNDER_MAINT: '維修中',
  UNDER_MAINTAIN: '保養中',
  IN_PROGRESS: '處理中',
  RESOLVED: '已結案',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  PENDING: '待處理',
  ON_HOLD: '暫停',
}

// ✅ 修正：安全轉換狀態為中文（找不到則顯示原值或 '-'）
const statusText = (status) => STATUS_LABEL[status] ?? status ?? '-'

// ====== el-table 跑版修正：強制重新計算欄寬 ======
const tableRef = ref(null)

const doLayoutSafe = async () => {
  await nextTick()
  tableRef.value?.doLayout?.()
}

const onResize = () => doLayoutSafe()
window.addEventListener('resize', onResize)
onBeforeUnmount(() => window.removeEventListener('resize', onResize))

// ====== 工單快取（用一次 API 建立 staff 狀態）======
const allTickets = ref([])
const ticketLoading = ref(false)

// ====== 機台與椅子資料快取（用於顯示目標名稱）======
const allSpots = ref([])
const allSeats = ref([])

// ====== 狀態篩選（全部 / 維護中 / 維修中 / 閒置中）======
const statusFilter = ref('ALL') // 'ALL' | 'UNDER_MAINTENANCE' | 'ASSIGNED' | 'IDLE'

// ====== ✅ 【新增】人員詳情視窗狀態 ======
const detailDialogVisible = ref(false)
const detailCurrentView = ref('profile') // 'profile' | 'tasks'
const detailCurrentStaff = ref(null)
const detailCurrentTaskType = ref('') // 'repair-pending' | 'maintenance-pending' | ...

// ====== 轉移工單 Dialog 狀態 ======
const showTransferDialog = ref(false)
const transferForm = ref({
  deleteStaffId: null,
  deleteStaffName: '',
  targetStaffId: null,
})
const transferLoading = ref(false)

// ★ 計算可選接手人員：排除要刪除的人 + 必須啟用中
const availableTargetStaff = computed(() => {
  return staffList.value.filter(
    (s) => s.staffId !== transferForm.value.deleteStaffId && s.isActive === true,
  )
})

// ✅ 【修正】API 資料讀取
const fetchStaff = async () => {
  try {
    loading.value = true
    const res = await maintenanceApi.getAllStaff()
    staffList.value = res.data || []
  } catch {
    // 錯誤已由 http.js 攔截器處理
  } finally {
    loading.value = false
  }
}

const fetchTickets = async () => {
  try {
    ticketLoading.value = true
    const res = await maintenanceApi.getAllTickets()
    allTickets.value = res.data || []
  } catch {
    allTickets.value = []
  } finally {
    ticketLoading.value = false
  }
}

// ✅ 【修正】使用統一的狀態計算函式 (取代原本的重複邏輯)
// 人員任務統計 Map (staffId -> 任務統計)
const staffTicketStatMap = computed(() => {
  const map = new Map()
  staffList.value.forEach((staff) => {
    const stats = computeStaffTaskStats(staff.staffId, allTickets.value)
    map.set(staff.staffId, stats)
  })
  return map
})

// ✅ 【修正】人員即時狀態計算 (統一呼叫 composable)
const getStaffWorkStatus = (staffId) => {
  return computeStaffStatus(staffId, allTickets.value)
}

/** ====== 刪除人員（含防呆轉移邏輯）====== */
const handleDelete = async (row) => {
  const result = await Swal.fire({
    title: '確定要停用此人員嗎？',
    html: `
      <div style="text-align: center;">
        <div style="width: 80px; height: 80px; margin: 0 auto 16px; background: linear-gradient(135deg, #f56c6c 0%, #f89898 100%); border-radius: 50%; display: flex; align-items: center; justify-content: center;">
          <i class="fas fa-user-slash" style="font-size: 36px; color: white;"></i>
        </div>
        <p style="font-size: 16px; margin-bottom: 20px;">即將停用 <b style="color: #f56c6c;">${row.staffName}</b></p>
        
        <div style="text-align: left; margin-bottom: 12px;">
            <label style="font-size: 13px; color: #606266; font-weight: bold; margin-bottom: 4px; display: block;">停用原因 (必選)</label>
            <select id="swal-reason-select" class="swal2-select" style="display: flex; width: 100%; margin: 0; padding: 0.5em; border: 1px solid #d9d9d9; border-radius: 4px;">
                <option value="" disabled selected>請選擇...</option>
                <option value="已終止合作">已終止合作</option>
                <option value="其他">其他原因</option>
            </select>
        </div>

        <div style="text-align: left;">
            <label style="font-size: 13px; color: #606266; font-weight: bold; margin-bottom: 4px; display: block;">詳細備註 (選填)</label>
            <textarea id="swal-reason-note" class="swal2-textarea" placeholder="可在此補充詳細說明..." style="margin: 0; width: 100%; height: 80px; font-size: 14px; border: 1px solid #d9d9d9; box-sizing: border-box;"></textarea>
        </div>
      </div>
    `,
    showCancelButton: true,
    confirmButtonColor: '#f56c6c',
    cancelButtonColor: '#909399',
    confirmButtonText: '<i class="fas fa-user-slash mr-1"></i> 確認並停用',
    cancelButtonText: '取消',
    showClass: { popup: 'animate__animated animate__fadeInDown animate__faster' },
    hideClass: { popup: 'animate__animated animate__fadeOutUp animate__faster' },
    customClass: { popup: 'custom-swal-popup' },
    // 透過 preConfirm 抓取兩個欄位的值
    preConfirm: () => {
      const selectVal = document.getElementById('swal-reason-select').value
      const noteVal = document.getElementById('swal-reason-note').value

      if (!selectVal) {
        Swal.showValidationMessage('請選擇停用原因')
        return false
      }

      // 回傳組合後的物件
      return { selectVal, noteVal }
    },
  })

  if (result.isConfirmed) {
    const { selectVal, noteVal } = result.value

    // 組合最終顯示的字串，例如："已終止合作 (詳細說明...)"
    // 如果沒有備註，就只存選單的值
    const finalReason = noteVal ? `${selectVal}：${noteVal}` : selectVal

    //取得當天日期(格式 YYYY/MM/DD )
    const today = new Date().toLocaleDateString('zh-TW', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    })

    try {
      loading.value = true

      // --- 組合新的備註字串 (保持與歷史紀錄頁面的格式相容) ---
      const originalNote = row.staffNote || ''
      const newNote = `[停用原因：${finalReason}] [停用日期：${today}] \n${originalNote}`

      // 1. 先更新備註
      await maintenanceApi.updateStaff(row.staffId, {
        ...row,
        staffNote: newNote,
      })

      // 2. 再執行停用
      await maintenanceApi.deleteStaff(row.staffId)

      await fetchStaff()

      await Swal.fire({
        icon: 'success',
        title: '已停用',
        html: `
          <div style="text-align:center;">
            <span><b>${row.staffName}</b> 已移至歷史紀錄</span><br/>
            <span style="color: #909399; font-size: 13px; margin-top: 8px; display: inline-block; background: #f4f4f5; padding: 4px 8px; border-radius: 4px;">
              原因：${selectVal}
            </span>
          </div>
        `,
        timer: 900,
        timerProgressBar: true,
        showConfirmButton: false,
      })
    } catch (error) {
      const errorMsg = error?.response?.data?.message || error?.message || ''

      if (
        errorMsg.includes('未完成') ||
        errorMsg.includes('工單') ||
        error?.response?.status === 400
      ) {
        // 發生轉移需求時
        transferForm.value = {
          deleteStaffId: row.staffId,
          deleteStaffName: row.staffName,
          targetStaffId: null,
        }
        showTransferDialog.value = true

        // 額外提示：剛剛選的原因沒有存成功，轉移後可能需要重新操作(視後端邏輯而定)
      } else {
        Swal.fire('錯誤', '停用失敗，請稍後再試', 'error')
      }
    } finally {
      loading.value = false
    }
  }
}

/** ====== 移除不必要的 showHistoryModal (已改用 el-dialog 內部導覽) ====== */
// 此函式已整合到 handleViewTasks，不再需要

// ✅ 【修正】人員詳情視窗 (改用 el-dialog + 內部導覽)
const viewDetail = async (row) => {
  // 確保 tickets 有資料
  if (!allTickets.value.length && !ticketLoading.value) {
    await fetchTickets()
  }

  detailCurrentStaff.value = row
  detailCurrentView.value = 'profile'
  detailDialogVisible.value = true
}

// 點擊任務卡片 (切換到任務列表 view)
const handleViewTasks = (cardType) => {
  detailCurrentTaskType.value = cardType
  detailCurrentView.value = 'tasks'
}

// 計算任務列表資料 (根據 taskType 篩選)
// ✅【修正#1】任務列表分類也改成嚴格判定，確保「點卡片進去的清單」跟圖卡一致
const detailTaskList = computed(() => {
  if (!detailCurrentStaff.value || !detailCurrentTaskType.value) return []

  const staffId = detailCurrentStaff.value.staffId
  const cardType = detailCurrentTaskType.value
  const matchFn = (t) => t?.assignedStaffId === staffId

  const isDone = (t) => isCompletedStatus(t?.issueStatus)
  const isMaint = (t) => isMaintenanceTicketStrict(t)

  let filtered = []
  switch (cardType) {
    case 'repair-pending':
      filtered = (allTickets.value || []).filter((t) => matchFn(t) && !isMaint(t) && !isDone(t))
      break
    case 'maintenance-pending':
      filtered = (allTickets.value || []).filter((t) => matchFn(t) && isMaint(t) && !isDone(t))
      break
    case 'repair-completed':
      filtered = (allTickets.value || []).filter((t) => matchFn(t) && !isMaint(t) && isDone(t))
      break
    case 'maintenance-completed':
      filtered = (allTickets.value || []).filter((t) => matchFn(t) && isMaint(t) && isDone(t))
      break
    default:
      filtered = (allTickets.value || []).filter((t) => matchFn(t))
  }

  // 排序：最新在上
  const getTime = (t) => new Date(t?.reportedAt || t?.startAt || t?.resolvedAt || 0).getTime()
  filtered.sort((a, b) => getTime(b) - getTime(a))

  return filtered
})

// 任務類型標題對應
const taskTypeTitle = computed(() => {
  const map = {
    'repair-pending': '待修任務',
    'maintenance-pending': '待保養任務',
    'repair-completed': '維修完成',
    'maintenance-completed': '保養完成',
  }
  return map[detailCurrentTaskType.value] || '工單列表'
})

// 任務類型圖示對應
const taskTypeIcon = computed(() => {
  const map = {
    'repair-pending': 'fas fa-wrench',
    'maintenance-pending': 'fas fa-clipboard-list',
    'repair-completed': 'fas fa-check-circle',
    'maintenance-completed': 'fas fa-flag-checkered',
  }
  return map[detailCurrentTaskType.value] || 'fas fa-ticket-alt'
})

// 返回人員詳情
const backToProfile = () => {
  detailCurrentView.value = 'profile'
}

// 關閉詳情視窗
const closeDetailDialog = () => {
  detailDialogVisible.value = false
  detailCurrentView.value = 'profile'
  detailCurrentStaff.value = null
  detailCurrentTaskType.value = ''
}

// 格式化目標標籤
const getTargetLabel = (t) => {
  if (t.seatsId) {
    const seat = allSeats.value.find((s) => s.seatsId === t.seatsId)
    return seat ? `椅子: ${seat.seatsName}` : `椅子 #${t.seatsId}`
  }
  if (t.spotId) {
    const spot = allSpots.value.find((s) => s.spotId === t.spotId)
    return spot ? `機台: ${spot.spotName}` : `機台 #${t.spotId}`
  }
  return '未指定'
}

// ✅【修正#1】嚴格判定保養單：只認 issueType === '保養'
// 避免「檢查 / 例行 / 盤點」之類字眼被誤判成保養
const isMaintenanceTicketStrict = (t) => String(t?.issueType || '').trim() === '保養'

// ✅【修正#1】人員詳情四卡：在 Dialog 內用「嚴格判定」重新算一次
// 這樣就算 composable 判定有問題，詳情圖卡也不會顯示錯
const detailStaffStats = computed(() => {
  const staffId = detailCurrentStaff.value?.staffId
  if (!staffId) {
    return { repairCurrent: 0, maintainCurrent: 0, repairDone: 0, maintainDone: 0 }
  }

  const mine = (allTickets.value || []).filter((t) => t?.assignedStaffId === staffId)

  const current = mine.filter((t) => !isCompletedStatus(t?.issueStatus))
  const done = mine.filter((t) => isCompletedStatus(t?.issueStatus))

  const repairCurrent = current.filter((t) => !isMaintenanceTicketStrict(t)).length
  const maintainCurrent = current.filter((t) => isMaintenanceTicketStrict(t)).length
  const repairDone = done.filter((t) => !isMaintenanceTicketStrict(t)).length
  const maintainDone = done.filter((t) => isMaintenanceTicketStrict(t)).length

  return { repairCurrent, maintainCurrent, repairDone, maintainDone }
})

// ✅ 【移除】舊的 Swal 程式碼片段已清除，改用 el-dialog (見 template 區塊)

const handleAddNew = () => {
  Swal.fire({
    title: '新增維護人員',
    text: '即將前往新增人員表單',
    icon: 'info',
    timer: 600,
    timerProgressBar: true,
    showConfirmButton: false,
    showClass: { popup: 'animate__animated animate__fadeInRight animate__faster' },
  }).then(() => {
    router.push('/admin/staff-form')
  })
}

// ====== 執行轉移並刪除 ======
const handleTransferAndDelete = async () => {
  if (!transferForm.value.targetStaffId) {
    await Swal.fire({
      icon: 'warning',
      title: '請選擇接手人員',
      text: '必須指定一位接手人員來接收未完成的工單',
      confirmButtonColor: '#409eff',
    })
    return
  }

  transferLoading.value = true
  try {
    await maintenanceApi.transferAndDelete(
      transferForm.value.targetStaffId,
      transferForm.value.deleteStaffId,
    )

    showTransferDialog.value = false
    await fetchStaff()

    const targetStaff = staffList.value.find((s) => s.staffId === transferForm.value.targetStaffId)

    await Swal.fire({
      icon: 'success',
      title: '轉移成功！',
      html: `
        <div style="text-align: center;">
          <p>工單已轉移給 <b style="color: #67c23a;">${targetStaff?.staffName || '接手人員'}</b></p>
          <p style="color: #909399; font-size: 13px;"><b>${transferForm.value.deleteStaffName}</b> 已停用</p>
        </div>
      `,
      timer: 2000,
      timerProgressBar: true,
      showConfirmButton: false,
      showClass: { popup: 'animate__animated animate__bounceIn' },
    })
  } catch {
    // 錯誤已由 http.js 攔截器處理
  } finally {
    transferLoading.value = false
  }
}

// 關閉 Dialog
const closeTransferDialog = () => {
  showTransferDialog.value = false
  transferForm.value = {
    deleteStaffId: null,
    deleteStaffName: '',
    targetStaffId: null,
  }
}

/** 先過濾 active，再過濾狀態，再做搜尋 */
const filteredList = computed(() => {
  const key = searchText.value.trim().toLowerCase()

  const activeStaff = staffList.value.filter((s) => s.isActive === true)

  // ✅ 【修正】狀態篩選邏輯 (使用統一的 computeStaffStatus)
  const statusFiltered =
    statusFilter.value === 'ALL'
      ? activeStaff
      : activeStaff.filter((s) => getStaffWorkStatus(s.staffId).key === statusFilter.value)

  if (!key) return statusFiltered
  return statusFiltered.filter(
    (s) =>
      (s.staffName || '').toLowerCase().includes(key) ||
      (s.staffCompany || '').toLowerCase().includes(key) ||
      (s.staffPhone || '').includes(key),
  )
})

// 使用 usePagination composable
const {
  currentPage,
  pageSize,
  paginatedList,
  total: paginationTotal,
  showPagination,
  resetPagination,
} = usePagination(filteredList, { defaultPageSize: 10 })

watch([searchText, statusFilter], () => {
  resetPagination()
  doLayoutSafe()
})

watch([currentPage, pageSize], () => {
  doLayoutSafe()
})

const formatDate = (row, column, cellValue) => {
  if (!cellValue) return '-'
  return new Date(cellValue).toLocaleDateString('zh-TW')
}

// ✅ 【修正】統計卡片數字 (使用統一的狀態計算，確保與列表一致)
const activeCount = computed(() => staffList.value.filter((s) => s.isActive).length)

// 使用統一的 computeOverallStaffStats 計算整體狀態統計
const overallStats = computed(() => {
  const activeStaff = staffList.value.filter((s) => s.isActive)
  return computeOverallStaffStats(activeStaff, allTickets.value)
})

const maintainingCount = computed(() => overallStats.value.underMaintenance)
const assignedCount = computed(() => overallStats.value.assigned)
const idleCount = computed(() => overallStats.value.idle)

// ✅ 讓排程頁建立工單後，人員頁能即時同步圖卡
const onTicketsChanged = async () => {
  await fetchTickets()
}

// ============================================================================
// ✅ 【任務1修正】onMounted 使用 Promise.all 並行載入，提升效能與資料連動性
// ============================================================================
onMounted(async () => {
  try {
    // 【效能優化】同時呼叫四個 API，避免序列等待
    const [staffRes, ticketsRes, spotsRes, seatsRes] = await Promise.all([
      maintenanceApi.getAllStaff(),
      maintenanceApi.getAllTickets(),
      maintenanceApi.getAllSpots(),
      maintenanceApi.getAllSeats(),
    ])
    
    // 【資料連動】同步更新 staffList、allTickets、allSpots、allSeats
    staffList.value = staffRes.data || []
    allTickets.value = ticketsRes.data || []
    allSpots.value = spotsRes.data || []
    allSeats.value = seatsRes.data || []
    
    loading.value = false
    ticketLoading.value = false
  } catch (err) {
    console.error('載入人員或工單資料失敗:', err)
    loading.value = false
    ticketLoading.value = false
  }

  // ✅ 監聽跨頁事件：排程建立工單後會觸發
  window.addEventListener('maintenance:tickets-changed', onTicketsChanged)

  setTimeout(() => {
    pageVisible.value = true
    setTimeout(() => doLayoutSafe(), 150)
  }, 100)
})

onBeforeUnmount(() => {
  window.removeEventListener('maintenance:tickets-changed', onTicketsChanged)
})
</script>

<template>
  <div class="staff-list-container">
    <!-- 頁面標題區 -->
    <section class="content-header">
      <div class="container-fluid">
        <transition name="slide-down" appear>
          <div class="page-title-box">
            <div class="title-icon">
              <i class="fas fa-users-cog"></i>
            </div>
            <div class="title-content">
              <h1>維護人員管理</h1>
              <p class="subtitle">管理系統內的維護人員資料</p>
            </div>
            <div class="title-actions">
              <el-button-group>
                <el-button type="success" @click="handleAddNew" class="action-btn add-btn">
                  <i class="fas fa-user-plus mr-2"></i> 新增人員
                </el-button>
                <el-button
                  type="info"
                  plain
                  @click="router.push('/admin/staff-history')"
                  class="action-btn"
                >
                  <i class="fas fa-history mr-2"></i> 歷史紀錄
                </el-button>
              </el-button-group>
            </div>
          </div>
        </transition>
      </div>
    </section>

    <!-- 主內容區 -->
    <section class="content">
      <div class="container-fluid">
        <transition name="zoom-fade" appear>
          <div v-show="pageVisible">
            <!-- 統計卡片列：新增 3 張狀態卡 -->
            <el-row :gutter="20" class="mb-4">
              <el-col :xs="12" :sm="8" :md="6" :lg="4">
                <div class="stat-card active-card" @click="statusFilter = 'ALL'">
                  <div class="stat-icon pulse-animation">
                    <i class="fas fa-user-check"></i>
                  </div>
                  <div class="stat-info">
                    <h3>{{ activeCount }}</h3>
                    <span>在職人員</span>
                  </div>
                  <div class="stat-bg-icon"><i class="fas fa-users"></i></div>
                </div>
              </el-col>

              <el-col :xs="12" :sm="8" :md="6" :lg="4">
                <div class="stat-card filter-card" @click="statusFilter = 'UNDER_MAINTENANCE'">
                  <div
                    class="stat-icon"
                    style="background: linear-gradient(135deg, #e6a23c 0%, #f3d19e 100%)"
                  >
                    <i class="fas fa-wrench"></i>
                  </div>
                  <div class="stat-info">
                    <h3>{{ maintainingCount }}</h3>
                    <span>維修中</span>
                  </div>
                </div>
              </el-col>

              <el-col :xs="12" :sm="8" :md="6" :lg="4">
                <div class="stat-card filter-card" @click="statusFilter = 'ASSIGNED'">
                  <div
                    class="stat-icon"
                    style="background: linear-gradient(135deg, #409eff 0%, #a0cfff 100%)"
                  >
                    <i class="fas fa-user-check"></i>
                  </div>
                  <div class="stat-info">
                    <h3>{{ assignedCount }}</h3>
                    <span>已指派</span>
                  </div>
                </div>
              </el-col>

              <el-col :xs="12" :sm="8" :md="6" :lg="4">
                <div class="stat-card filter-card" @click="statusFilter = 'IDLE'">
                  <div
                    class="stat-icon"
                    style="background: linear-gradient(135deg, #67c23a 0%, #95d475 100%)"
                  >
                    <i class="fas fa-mug-hot"></i>
                  </div>
                  <div class="stat-info">
                    <h3>{{ idleCount }}</h3>
                    <span>閒置中</span>
                  </div>
                </div>
              </el-col>

              <el-col :xs="12" :sm="8" :md="6" :lg="4">
                <div class="stat-card filter-card">
                  <div
                    class="stat-icon"
                    style="background: linear-gradient(135deg, #909399 0%, #c0c4cc 100%)"
                  >
                    <i class="fas fa-search"></i>
                  </div>
                  <div class="stat-info">
                    <h3>{{ filteredList.length }}</h3>
                    <span>搜尋結果</span>
                  </div>
                </div>
              </el-col>
            </el-row>

            <!-- 資料表格卡片 -->
            <el-card shadow="hover" class="table-card">
              <template #header>
                <div class="card-header-content">
                  <div class="header-left">
                    <span class="header-icon">
                      <i class="fas fa-list-ul"></i>
                    </span>
                    <span class="header-text">人員名單</span>

                    <!-- 狀態 filter pills -->
                    <div class="ml-2" style="display: flex; gap: 6px; flex-wrap: wrap">
                      <el-tag
                        :effect="statusFilter === 'ALL' ? 'dark' : 'plain'"
                        type="info"
                        round
                        @click="statusFilter = 'ALL'"
                        style="cursor: pointer"
                      >
                        全部
                      </el-tag>
                      <el-tag
                        :effect="statusFilter === 'UNDER_MAINTENANCE' ? 'dark' : 'plain'"
                        type="warning"
                        round
                        @click="statusFilter = 'UNDER_MAINTENANCE'"
                        style="cursor: pointer"
                      >
                        維修中
                      </el-tag>

                      <el-tag
                        :effect="statusFilter === 'ASSIGNED' ? 'dark' : 'plain'"
                        type="primary"
                        round
                        @click="statusFilter = 'ASSIGNED'"
                        style="cursor: pointer"
                      >
                        已指派
                      </el-tag>

                      <el-tag
                        :effect="statusFilter === 'IDLE' ? 'dark' : 'plain'"
                        type="success"
                        round
                        @click="statusFilter = 'IDLE'"
                        style="cursor: pointer"
                      >
                        閒置中
                      </el-tag>
                    </div>

                    <el-tag type="success" effect="light" size="small" class="ml-2" round>
                      <i class="fas fa-circle" style="font-size: 6px; margin-right: 4px"></i>
                      {{ filteredList.length }} 位
                    </el-tag>
                  </div>

                  <div class="header-right">
                    <el-input
                      v-model="searchText"
                      placeholder="搜尋姓名、公司、電話..."
                      prefix-icon="Search"
                      clearable
                      class="search-input"
                    >
                      <template #append>
                        <el-button
                          icon="Refresh"
                          @click="
                            () => {
                              fetchStaff()
                              fetchTickets()
                            }
                          "
                        />
                      </template>
                    </el-input>
                  </div>
                </div>
              </template>

              <!-- 骨架屏載入 -->
              <div v-if="loading" class="loading-skeleton">
                <el-skeleton :rows="6" animated />
              </div>

              <!-- 資料表格 -->
              <el-table
                ref="tableRef"
                v-else
                :data="paginatedList"
                stripe
                highlight-current-row
                class="custom-table"
                style="width: 100%"
                table-layout="fixed"
                :header-cell-style="{ boxSizing: 'border-box' }"
                :cell-style="{ boxSizing: 'border-box' }"
                @row-dblclick="viewDetail"
              >
                <el-table-column prop="staffId" label="ID" width="70" align="center" sortable>
                  <template #default="{ row }">
                    <el-tag effect="plain" size="small">#{{ row.staffId }}</el-tag>
                  </template>
                </el-table-column>

                <el-table-column prop="staffName" label="姓名" width="160" sortable>
                  <template #default="{ row }">
                    <div class="name-cell" @click="viewDetail(row)">
                      <div class="name-avatar">
                        {{ row.staffName?.charAt(0) || '?' }}
                      </div>
                      <div class="name-info">
                        <span class="name-text">{{ row.staffName }}</span>
                        <span class="status-dot active"></span>
                      </div>
                    </div>
                  </template>
                </el-table-column>

                <!-- ✅ 新增欄位：目前狀態 -->
                <el-table-column label="目前狀態" width="120" align="center">
                  <template #default="{ row }">
                    <el-tag :type="getStaffWorkStatus(row.staffId).tagType" effect="light" round>
                      <i
                        :class="getStaffWorkStatus(row.staffId).icon"
                        style="margin-right: 6px"
                      ></i>
                      {{ getStaffWorkStatus(row.staffId).text }}
                    </el-tag>
                  </template>
                </el-table-column>

                <el-table-column prop="staffCompany" label="所屬公司" min-width="160">
                  <template #default="{ row }">
                    <div v-if="row.staffCompany" class="company-cell">
                      <i class="fas fa-building company-icon"></i>
                      <span>{{ row.staffCompany }}</span>
                    </div>
                    <span v-else class="text-muted">未填寫</span>
                  </template>
                </el-table-column>

                <el-table-column prop="staffPhone" label="聯絡電話" width="150">
                  <template #default="{ row }">
                    <div v-if="row.staffPhone" class="phone-cell">
                      <i class="fas fa-phone phone-icon"></i>
                      <span>{{ row.staffPhone }}</span>
                    </div>
                    <span v-else class="text-muted">-</span>
                  </template>
                </el-table-column>

                <el-table-column prop="staffEmail" label="Email" min-width="200">
                  <template #default="{ row }">
                    <el-tooltip v-if="row.staffEmail" :content="row.staffEmail" placement="top">
                      <div class="email-cell">
                        <i class="fas fa-envelope email-icon"></i>
                        <span>{{ row.staffEmail }}</span>
                      </div>
                    </el-tooltip>
                    <span v-else class="text-muted">-</span>
                  </template>
                </el-table-column>

                <el-table-column
                  prop="staffNote"
                  label="備註"
                  min-width="120"
                  show-overflow-tooltip
                >
                  <template #default="{ row }">
                    <span v-if="row.staffNote" class="note-cell">{{ row.staffNote }}</span>
                    <span v-else class="text-muted">-</span>
                  </template>
                </el-table-column>

                <el-table-column
                  prop="createdAt"
                  label="建立日期"
                  width="120"
                  :formatter="formatDate"
                />

                <el-table-column label="操作" width="180" align="center">
                  <template #default="{ row }">
                    <div class="action-buttons">
                      <el-tooltip content="查看詳情" placement="top">
                        <el-button
                          type="info"
                          size="small"
                          circle
                          @click="viewDetail(row)"
                          class="action-btn-item"
                        >
                          <i class="fas fa-eye"></i>
                        </el-button>
                      </el-tooltip>
                      <el-tooltip content="編輯資料" placement="top">
                        <el-button
                          type="primary"
                          size="small"
                          circle
                          @click="router.push(`/admin/staff-form/${row.staffId}`)"
                          class="action-btn-item"
                        >
                          <i class="fas fa-edit"></i>
                        </el-button>
                      </el-tooltip>
                      <el-tooltip content="停用人員" placement="top">
                        <el-button
                          type="danger"
                          size="small"
                          circle
                          @click="handleDelete(row)"
                          class="action-btn-item"
                        >
                          <i class="fas fa-user-slash"></i>
                        </el-button>
                      </el-tooltip>
                    </div>
                  </template>
                </el-table-column>

                <template #empty>
                  <el-empty
                    :description="
                      statusFilter === 'ALL' && !searchText
                        ? '目前沒有人員資料'
                        : '沒有符合條件的人員'
                    "
                  >
                    <template #image>
                      <div class="empty-icon">
                        <i class="fas fa-users-slash"></i>
                      </div>
                    </template>

                    <!-- ✅ 只有在「全部」且「完全沒資料」才顯示新增 -->
                    <el-button
                      v-if="statusFilter === 'ALL' && !searchText"
                      type="primary"
                      @click="handleAddNew"
                    >
                      <i class="fas fa-plus mr-1"></i> 新增第一位人員
                    </el-button>
                  </el-empty>
                </template>
              </el-table>

              <div class="pagination-wrapper" v-if="showPagination">
                <el-pagination
                  v-model:current-page="currentPage"
                  v-model:page-size="pageSize"
                  :page-sizes="[5, 10, 20, 50]"
                  :total="paginationTotal"
                  layout="total, sizes, prev, pager, next, jumper"
                  background
                />
              </div>
            </el-card>

            <div class="tips-bar mt-3">
              <el-alert type="info" :closable="false" show-icon>
                <template #title>
                  <span>💡 小提示：雙擊表格列可快速查看人員詳情；詳情四張卡可點進工單歷史</span>
                </template>
              </el-alert>
            </div>
          </div>
        </transition>
      </div>
    </section>

    <!-- ========== 轉移工單並刪除 Dialog ========== -->
    <!-- ✅ 修正：Dialog teleported 防裁切 -->
    <el-dialog
      v-model="showTransferDialog"
      title="轉移工單並刪除人員"
      width="500px"
      :close-on-click-modal="false"
      :close-on-press-escape="!transferLoading"
      @close="closeTransferDialog"
      :teleported="true"
      append-to-body
      :modal-append-to-body="true"
      :align-center="true"
      class="mt-transfer-dialog"
    >
      <div class="transfer-dialog-content">
        <el-alert type="warning" :closable="false" show-icon class="mb-4">
          <template #title>
            <strong>{{ transferForm.deleteStaffName }}</strong> 有未完成的工單
          </template>
          <template #default> 請選擇要將工單轉移給哪位人員後，才能刪除此人員。 </template>
        </el-alert>

        <el-form label-position="top">
          <el-form-item label="選擇接手人員" required>
            <el-select
              v-model="transferForm.targetStaffId"
              placeholder="請選擇接手人員"
              filterable
              style="width: 100%"
              :disabled="transferLoading"
            >
              <el-option
                v-for="staff in availableTargetStaff"
                :key="staff.staffId"
                :label="`${staff.staffName} (${staff.specialization || '未設定專長'})`"
                :value="staff.staffId"
              >
                <div class="transfer-option">
                  <span class="name">{{ staff.staffName }}</span>
                  <el-tag size="small" type="info">{{
                    staff.specialization || '未設定專長'
                  }}</el-tag>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
        </el-form>

        <div class="transfer-preview" v-if="transferForm.targetStaffId">
          <el-divider content-position="left">
            <el-icon><InfoFilled /></el-icon> 操作預覽
          </el-divider>
          <div class="preview-content">
            <p>
              <el-icon color="#E6A23C"><Right /></el-icon>
              <strong>{{ transferForm.deleteStaffName }}</strong> 的所有未完成工單 將轉移給
              <strong>{{
                availableTargetStaff.find((s) => s.staffId === transferForm.targetStaffId)
                  ?.staffName
              }}</strong>
            </p>
            <p>
              <el-icon color="#F56C6C"><Delete /></el-icon>
              然後刪除人員 <strong>{{ transferForm.deleteStaffName }}</strong>
            </p>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="closeTransferDialog" :disabled="transferLoading"> 取消 </el-button>
        <el-button
          type="danger"
          :loading="transferLoading"
          :disabled="!transferForm.targetStaffId"
          @click="handleTransferAndDelete"
        >
          <el-icon v-if="!transferLoading"><Check /></el-icon>
          確認轉移並刪除
        </el-button>
      </template>
    </el-dialog>

    <!-- ========== ✅ 【新增】人員詳情 Dialog (支援內部導覽) ========== -->
    <!-- ✅ 修正：Dialog teleported 防裁切 + 位置居中 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="detailCurrentView === 'profile' ? '人員詳情' : taskTypeTitle"
      width="720px"
      :close-on-click-modal="false"
      @close="closeDetailDialog"
      :teleported="true"
      append-to-body
      :modal-append-to-body="true"
      :align-center="true"
      top="6vh"
      class="mt-staffDetail-dialog"
    >
      <!-- Profile View -->
      <div v-if="detailCurrentView === 'profile' && detailCurrentStaff" class="mt-staffDetail">
        <!-- ✅ UI 修復：人員基本資訊頭部 - 改用專用 class 避免撞名 -->
        <div class="mt-staffDetail__header">
          <div class="mt-staffDetail__avatar">
            {{ detailCurrentStaff.staffName?.charAt(0) || '?' }}
          </div>
          <div class="mt-staffDetail__info">
            <h3 class="mt-staffDetail__name">{{ detailCurrentStaff.staffName }}</h3>
            <el-tag :type="getStaffWorkStatus(detailCurrentStaff.staffId).tagType" size="small">
              <i :class="getStaffWorkStatus(detailCurrentStaff.staffId).icon" class="mr-1"></i>
              {{ getStaffWorkStatus(detailCurrentStaff.staffId).text }}
            </el-tag>
          </div>
        </div>

        <!-- ✅ UI 修復：任務統計四卡 - 使用 el-row/el-col 確保穩定佈局，避免 class 撞名 -->
        <el-row :gutter="12" class="mt-staffDetail__statGrid">
          <el-col :xs="12" :sm="12" :md="12">
            <div class="mt-statCard mt-statCard--repair" @click="handleViewTasks('repair-pending')">
              <div class="mt-statCard__left">
                <div class="mt-statCard__icon mt-statCard__icon--repair">
                  <i class="fas fa-wrench"></i>
                </div>
                <span class="mt-statCard__label">待修任務</span>
              </div>
              <div class="mt-statCard__value">
                {{ detailStaffStats.repairCurrent }}
              </div>
            </div>
          </el-col>

          <el-col :xs="12" :sm="12" :md="12">
            <div
              class="mt-statCard mt-statCard--maintenance"
              @click="handleViewTasks('maintenance-pending')"
            >
              <div class="mt-statCard__left">
                <div class="mt-statCard__icon mt-statCard__icon--maintenance">
                  <i class="fas fa-clipboard-list"></i>
                </div>
                <span class="mt-statCard__label">待保養</span>
              </div>
              <div class="mt-statCard__value">
                {{ detailStaffStats.maintainCurrent }}
              </div>
            </div>
          </el-col>

          <el-col :xs="12" :sm="12" :md="12">
            <div
              class="mt-statCard mt-statCard--completed"
              @click="handleViewTasks('repair-completed')"
            >
              <div class="mt-statCard__left">
                <div class="mt-statCard__icon mt-statCard__icon--completed">
                  <i class="fas fa-check-circle"></i>
                </div>
                <span class="mt-statCard__label">維修完成</span>
              </div>
              <div class="mt-statCard__value">
                {{ detailStaffStats.repairDone }}
              </div>
            </div>
          </el-col>

          <el-col :xs="12" :sm="12" :md="12">
            <div
              class="mt-statCard mt-statCard--done"
              @click="handleViewTasks('maintenance-completed')"
            >
              <div class="mt-statCard__left">
                <div class="mt-statCard__icon mt-statCard__icon--done">
                  <i class="fas fa-flag-checkered"></i>
                </div>
                <span class="mt-statCard__label">保養完成</span>
              </div>
              <div class="mt-statCard__value">
                {{ detailStaffStats.maintainDone }}
              </div>
            </div>
          </el-col>
        </el-row>

        <!-- ✅ UI 修復：人員資料區 - 優化 descriptions 樣式 -->
        <div class="mt-staffDetail__infoSection">
          <el-descriptions :column="1" border class="mt-staffDetail__descriptions">
            <el-descriptions-item
              label="所屬公司"
              label-align="right"
              label-class-name="mt-desc-label"
            >
              <i class="fas fa-building mr-2" style="color: #409eff"></i>
              {{ detailCurrentStaff.staffCompany || '未填寫' }}
            </el-descriptions-item>
            <el-descriptions-item
              label="聯絡電話"
              label-align="right"
              label-class-name="mt-desc-label"
            >
              <i class="fas fa-phone mr-2" style="color: #67c23a"></i>
              {{ detailCurrentStaff.staffPhone || '-' }}
            </el-descriptions-item>
            <el-descriptions-item
              label="電子郵件"
              label-align="right"
              label-class-name="mt-desc-label"
            >
              <i class="fas fa-envelope mr-2" style="color: #e6a23c"></i>
              {{ detailCurrentStaff.staffEmail || '-' }}
            </el-descriptions-item>
            <el-descriptions-item
              label="備註說明"
              label-align="right"
              label-class-name="mt-desc-label"
            >
              {{ detailCurrentStaff.staffNote || '無備註' }}
            </el-descriptions-item>
            <el-descriptions-item
              label="建立日期"
              label-align="right"
              label-class-name="mt-desc-label"
            >
              {{
                detailCurrentStaff.createdAt
                  ? new Date(detailCurrentStaff.createdAt).toLocaleDateString('zh-TW')
                  : '-'
              }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>

      <!-- Tasks View -->
      <div v-else-if="detailCurrentView === 'tasks' && detailCurrentStaff" class="mt-taskView">
        <!-- ✅ UI 修復：返回按鈕區 -->
        <div class="mt-taskView__backBtn">
          <el-button @click="backToProfile" type="primary" plain size="small">
            <i class="fas fa-arrow-left mr-1"></i> 返回人員詳情
          </el-button>
        </div>

        <!-- ✅ UI 修復：加分項 - 任務列表標頭 -->
        <div class="mt-taskView__listHeader">
          <div class="mt-taskView__listTitle">
            <i :class="taskTypeIcon" class="mr-2"></i>
            <span>{{ taskTypeTitle }}</span>
          </div>
          <el-tag type="info" size="small" effect="plain">
            共 {{ detailTaskList.length }} 筆
          </el-tag>
        </div>

        <!-- 任務列表 -->
        <div v-if="detailTaskList.length > 0" class="mt-taskView__list">
          <el-table :data="detailTaskList" stripe max-height="400">
            <el-table-column prop="ticketId" label="工單" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small">#{{ row.ticketId }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="目標" width="100">
              <template #default="{ row }">
                {{ getTargetLabel(row) }}
              </template>
            </el-table-column>
            <el-table-column prop="issueDesc" label="描述" min-width="150" show-overflow-tooltip />
            <el-table-column label="狀態" width="100" align="center">
              <template #default="{ row }">
                <!-- ✅ 修正：狀態顯示中文，保留原本的 tag 樣式邏輯 -->
                <el-tag :type="row.issueStatus === 'RESOLVED' ? 'success' : 'info'" size="small">
                  {{ statusText(row.issueStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="時間" width="110">
              <template #default="{ row }">
                {{ row.reportedAt ? new Date(row.reportedAt).toLocaleDateString('zh-TW') : '-' }}
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-empty v-else description="暫無任務記錄" />
      </div>

      <template #footer v-if="detailCurrentView === 'profile'">
        <el-button @click="closeDetailDialog">關閉</el-button>
        <el-button
          type="primary"
          @click="router.push(`/admin/staff-form/${detailCurrentStaff?.staffId}`)"
        >
          <i class="fas fa-edit mr-1"></i> 編輯資料
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.staff-list-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  padding-bottom: 40px;
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
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  flex-wrap: wrap;
}

.title-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
  transition: all 0.4s ease;
  box-shadow: 0 4px 15px rgba(64, 158, 255, 0.3);
}

.title-icon:hover {
  transform: scale(1.1) rotate(10deg);
}

.title-content {
  flex: 1;
  min-width: 200px;
}

.title-content h1 {
  margin: 0;
  font-size: 1.7rem;
  font-weight: 700;
  color: #303133;
  background: linear-gradient(135deg, #303133 0%, #606266 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.title-content .subtitle {
  margin: 6px 0 0;
  font-size: 0.9rem;
  color: #909399;
}

.title-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  border-radius: 10px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.add-btn {
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
  border: none;
  box-shadow: 0 4px 15px rgba(103, 194, 58, 0.3);
}

.add-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(103, 194, 58, 0.4);
}

/* 統計卡片 */
.stat-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.06);
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  overflow: hidden;
  cursor: pointer;
  margin-bottom: 16px;
}

.stat-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.12);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  z-index: 1;
}

.active-card .stat-icon {
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
}

.filter-card .stat-icon {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
}

.stat-info {
  z-index: 1;
}

.stat-info h3 {
  margin: 0;
  font-size: 2rem;
  font-weight: 700;
  color: #303133;
}

.stat-info span {
  font-size: 0.85rem;
  color: #909399;
}

.stat-bg-icon {
  position: absolute;
  right: -10px;
  bottom: -10px;
  font-size: 80px;
  color: rgba(0, 0, 0, 0.03);
}

.pulse-animation {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

/* 表格卡片 */
.table-card {
  border-radius: 16px;
  overflow: hidden;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.card-header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  width: 40px;
  height: 40px;
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

.search-input {
  width: 300px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 10px 0 0 10px;
}

.search-input :deep(.el-input-group__append) {
  border-radius: 0 10px 10px 0;
}

/* 表格樣式 */
.custom-table {
  --el-table-header-bg-color: #f8f9fa;
}

/* 避免 scrollbar 有時出現有時消失造成欄寬抖動 */
.custom-table :deep(.el-table__body-wrapper) {
  overflow-y: scroll;
}

/* 保險：讓 header/body 計算一致 */
.custom-table :deep(.el-table__header),
.custom-table :deep(.el-table__body),
.custom-table :deep(th),
.custom-table :deep(td) {
  box-sizing: border-box;
}

.name-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 4px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.name-cell:hover {
  background: #f0f9eb;
}

.name-avatar {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
}

.name-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.name-text {
  font-weight: 500;
  color: #303133;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.active {
  background: #67c23a;
  box-shadow: 0 0 6px rgba(103, 194, 58, 0.6);
  animation: blink 2s infinite;
}

@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.company-cell,
.phone-cell,
.email-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.company-icon {
  color: #409eff;
}
.phone-icon {
  color: #67c23a;
}
.email-icon {
  color: #e6a23c;
}

.email-cell {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-cell {
  color: #606266;
  font-size: 13px;
}

/* 操作按鈕 */
.action-buttons {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.action-btn-item {
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.action-btn-item:hover {
  transform: scale(1.2);
}

/* 空狀態 */
.empty-icon {
  font-size: 64px;
  color: #dcdfe6;
  margin-bottom: 16px;
}

/* 分頁器 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
  margin-top: 20px;
}

/* 提示欄 */
.tips-bar :deep(.el-alert) {
  border-radius: 12px;
}

/* 過渡動畫 */
.slide-down-enter-active {
  transition: all 0.5s ease-out;
}
.slide-down-leave-active {
  transition: all 0.3s ease-in;
}
.slide-down-enter-from {
  transform: translateY(-30px);
  opacity: 0;
}
.slide-down-leave-to {
  transform: translateY(-20px);
  opacity: 0;
}

.zoom-fade-enter-active {
  transition: all 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.zoom-fade-leave-active {
  transition: all 0.3s ease-in;
}
.zoom-fade-enter-from {
  transform: scale(0.95);
  opacity: 0;
}
.zoom-fade-leave-to {
  transform: scale(0.98);
  opacity: 0;
}

/* 輔助類 */
.text-muted {
  color: #c0c4cc;
}
.mr-1 {
  margin-right: 4px;
}
.mr-2 {
  margin-right: 8px;
}
.ml-2 {
  margin-left: 8px;
}
.mb-4 {
  margin-bottom: 1.5rem;
}
.mt-3 {
  margin-top: 1rem;
}

/* ========== 轉移工單 Dialog 樣式 ========== */
.transfer-dialog-content {
  padding: 0 10px;
}

.transfer-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.transfer-option .name {
  font-weight: 500;
}

.transfer-preview {
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eef3 100%);
  border-radius: 8px;
  padding: 12px 16px;
  margin-top: 10px;
}

.transfer-preview .preview-content {
  font-size: 14px;
}

.transfer-preview .preview-content p {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 8px 0;
  color: #606266;
}

.transfer-preview .preview-content p strong {
  color: #303133;
}

:deep(.el-dialog__header) {
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 15px;
}

:deep(.el-dialog__footer) {
  border-top: 1px solid #ebeef5;
  padding-top: 15px;
}

/* ========== ✅ 【新增】人員詳情 Dialog 樣式 ========== */
/* ✅ UI 修復：使用 mt- 前綴避免與 AdminLTE 全域 CSS 撞名 */

/* 詳情容器 */
.mt-staffDetail {
  padding: 0;
}

/* 人員頭部資訊卡 */
.mt-staffDetail__header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  border-radius: 12px;
  margin-bottom: 20px;
}

.mt-staffDetail__avatar {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 26px;
  font-weight: 800;
  box-shadow: 0 4px 15px rgba(64, 158, 255, 0.3);
  flex-shrink: 0;
}

.mt-staffDetail__info {
  flex: 1;
}

.mt-staffDetail__name {
  margin: 0 0 10px 0;
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

/* ✅ UI 修復：統計卡片 Grid - 使用 BEM 命名避免撞名 */
.mt-staffDetail__statGrid {
  margin-bottom: 24px;
}

/* 單張統計卡 */
.mt-statCard {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.04);
  height: 100%;
  min-height: 88px;
}

.mt-statCard:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

.mt-statCard:active {
  transform: translateY(-1px);
}

/* 左側：icon + 標籤 */
.mt-statCard__left {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.mt-statCard__icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: white;
  flex-shrink: 0;
}

.mt-statCard__icon--repair {
  background: linear-gradient(135deg, #f6b26b 0%, #f9cf9d 100%);
}

.mt-statCard__icon--maintenance {
  background: linear-gradient(135deg, #6aa8ff 0%, #91bfff 100%);
}

.mt-statCard__icon--completed {
  background: linear-gradient(135deg, #79d7a7 0%, #a3e4c1 100%);
}

.mt-statCard__icon--done {
  background: linear-gradient(135deg, #a0a6ad 0%, #c4c8cd 100%);
}

.mt-statCard__label {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
  line-height: 1.2;
}

/* 右側：大數字 */
.mt-statCard__value {
  font-size: 32px;
  font-weight: 900;
  color: #303133;
  line-height: 1;
  margin-left: 12px;
}

/* 不同類型卡片背景色 */
.mt-statCard--repair {
  background: linear-gradient(135deg, #fff9f0 0%, #fef6ed 100%);
}

.mt-statCard--maintenance {
  background: linear-gradient(135deg, #f0f7ff 0%, #e6f2ff 100%);
}

.mt-statCard--completed {
  background: linear-gradient(135deg, #f0fdf4 0%, #e6faf0 100%);
}

.mt-statCard--done {
  background: linear-gradient(135deg, #f5f7fa 0%, #eef1f5 100%);
}

/* ✅ UI 修復：人員資料區 - 優化 descriptions 樣式 */
.mt-staffDetail__infoSection {
  margin-top: 0;
}

.mt-staffDetail__descriptions :deep(.el-descriptions__label) {
  width: 100px;
  font-weight: 600;
  color: #606266;
  background-color: #fafafa;
}

.mt-staffDetail__descriptions :deep(.el-descriptions__content) {
  color: #303133;
}

.mt-staffDetail__descriptions :deep(.el-descriptions__cell) {
  padding: 12px 16px;
}

/* ✅ UI 修復：Tasks View 樣式 */
.mt-taskView {
  padding: 0;
}

.mt-taskView__backBtn {
  margin-bottom: 16px;
}

.mt-taskView__listHeader {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  background: linear-gradient(135deg, #f8f9fa 0%, #f0f2f5 100%);
  border-radius: 10px;
  margin-bottom: 16px;
  border: 1px solid #e4e7ed;
}

.mt-taskView__listTitle {
  display: flex;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.mt-taskView__list {
  margin-top: 0;
}

/* ✅ UI 修復：響應式設計 - 手機單欄或兩欄 */
@media (max-width: 576px) {
  .mt-staffDetail__header {
    flex-direction: column;
    text-align: center;
    padding: 16px;
  }

  .mt-staffDetail__avatar {
    width: 56px;
    height: 56px;
    font-size: 22px;
  }

  .mt-staffDetail__name {
    font-size: 18px;
  }

  .mt-statCard {
    padding: 14px 16px;
    min-height: 80px;
  }

  .mt-statCard__icon {
    width: 36px;
    height: 36px;
    font-size: 16px;
  }

  .mt-statCard__label {
    font-size: 12px;
  }

  .mt-statCard__value {
    font-size: 26px;
  }

  .mt-taskView__listHeader {
    padding: 12px 14px;
  }

  .mt-taskView__listTitle {
    font-size: 14px;
  }
}

/* ✅ 修正：Dialog 內部可滾動防超出視窗被裁切 */
:deep(.mt-staffDetail-dialog .el-dialog__body) {
  max-height: calc(100vh - 220px);
  overflow-y: auto;
  overflow-x: hidden;
}

:deep(.mt-transfer-dialog .el-dialog__body) {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
  overflow-x: hidden;
}

/* ========== 舊的詳情樣式（已棄用，保留以防回溯需要）========== */
.detail-profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  border-radius: 12px;
  margin-bottom: 20px;
}

.detail-profile-header .avatar {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  font-weight: 800;
  box-shadow: 0 4px 15px rgba(64, 158, 255, 0.3);
}

.detail-profile-header .info h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.detail-task-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 20px;
}

.detail-task-cards .task-card {
  padding: 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.7);
}

.detail-task-cards .task-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.detail-task-cards .task-card.repair-pending {
  background: linear-gradient(135deg, #f6b26b 0%, #f9e0c7 100%);
  color: #1f2d3d;
}

.detail-task-cards .task-card.maintenance-pending {
  background: linear-gradient(135deg, #6aa8ff 0%, #d6e9ff 100%);
  color: #1f2d3d;
}

.detail-task-cards .task-card.repair-completed {
  background: linear-gradient(135deg, #79d7a7 0%, #e4f7ea 100%);
  color: #1f2d3d;
}

.detail-task-cards .task-card.maintenance-completed {
  background: linear-gradient(135deg, #aeb4bb 0%, #eef0f2 100%);
  color: #1f2d3d;
}

.detail-task-cards .task-card .card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  opacity: 0.85;
  margin-bottom: 8px;
}

.detail-task-cards .task-card .card-value {
  font-size: 28px;
  font-weight: 900;
}

.detail-info-section {
  margin-top: 16px;
}

.detail-back-btn {
  margin-bottom: 16px;
}

.detail-task-list {
  margin-top: 12px;
}
</style>
