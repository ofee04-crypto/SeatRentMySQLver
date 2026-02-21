<script setup>
import { ref, onMounted, computed, reactive, watch } from 'vue'
import maintenanceApi from '@/api/modules/maintenance'
import Swal from 'sweetalert2'
import { useTicketConfig } from '@/composables/maintenance/useTicketConfig'
import { usePagination } from '@/composables/maintenance/usePagination'
import TicketCharts from '@/components/maintenance/TicketCharts.vue'
import TicketTimeline from '@/components/maintenance/TicketTimeline.vue'

// --- Props & Refs ---
const props = defineProps({ historyMode: Boolean })
const tickets = ref([])
const filters = reactive({ keyword: '', priority: '', status: '' })
const loading = ref(false)
const pageVisible = ref(false)
const allSpots = ref([])

// ====== 【新增】問題回報模式 ======
const viewMode = ref('all') // 'all' | 'support'

// 問題回報標記規則（可擴展）
const SUPPORT_TAG_PREFIX = 'SUPPORT_'
const SUPPORT_TAG_DESC = '[REPORT]'

/**
 * 【新增】判斷是否為問題回報工單
 * 規則：issueType 以 SUPPORT_ 開頭 或 issueDesc 包含 [REPORT]
 */
const isSupportTicket = (ticket) => {
  if (!ticket) return false
  const typeMatch = ticket.issueType?.startsWith(SUPPORT_TAG_PREFIX)
  const descMatch = ticket.issueDesc?.includes(SUPPORT_TAG_DESC)
  return typeMatch || descMatch
}

/**
 * 【新增】問題回報未處理數量（用於 Tab Badge）
 */
const supportPendingCount = computed(() => {
  return tickets.value.filter(t => 
    isSupportTicket(t) && t.issueStatus === 'REPORTED'
  ).length
})

// ====== 資產健康度統計 ======
const assetStatsTab = ref('SPOT')
const assetStats = ref([])
const assetStatsLoading = ref(false)

// 取得資產健康度統計
const fetchAssetStats = async () => {
  try {
    assetStatsLoading.value = true
    const res = await maintenanceApi.getAssetStats(assetStatsTab.value)
    assetStats.value = res.data || []
  } catch (err) {
    console.error('取得資產統計失敗:', err)
    assetStats.value = []
    Swal.fire({
      icon: 'error',
      title: '載入失敗',
      text: '無法取得資產健康度統計',
      timer: 2000,
      showConfirmButton: false,
    })
  } finally {
    assetStatsLoading.value = false
  }
}

// 監聽 tab 切換
watch(assetStatsTab, () => {
  fetchAssetStats()
})

// spotId -> spotName 對照表（讓椅子工單可顯示「機台真名」）
const spotNameMap = computed(() => {
  const map = new Map()
  for (const s of allSpots.value || []) {
    map.set(Number(s.spotId), s.spotName)
  }
  return map
})

// 取得顯示用的機台名稱：優先 row.rentalSpot，其次 allSpots，再 fallback 到「機台 #id」
const getSpotDisplayName = (spotId, rentalSpot) => {
  if (rentalSpot?.spotName) return rentalSpot.spotName
  const name = spotNameMap.value.get(Number(spotId))
  return name || `機台 #${spotId}`
}

// 格式化百分比
const formatPercent = (value) => {
  if (value == null || isNaN(value)) return '0.0%'
  return (value * 100).toFixed(1) + '%'
}

// 格式化故障率
const formatRate = (value) => {
  if (value == null || isNaN(value)) return '0.00'
  return value.toFixed(2)
}

// 取得妥善率狀態顏色
const getAvailabilityStatus = (value) => {
  if (value >= 0.95) return 'success'
  if (value >= 0.8) return 'warning'
  return 'exception'
}

// 控制 LOG 彈窗的變數
const logDialogVisible = ref(false)
const currentLogTicketId = ref(0)

const openLogDialog = (id) => {
  currentLogTicketId.value = id
  logDialogVisible.value = true
}

// 判斷工單是否可編輯
const EDITABLE_STATUSES = ['REPORTED', 'ASSIGNED']
const canEdit = (row) => EDITABLE_STATUSES.includes(row.issueStatus)

// 提示不可編輯原因
const getEditTooltip = (row) => {
  if (canEdit(row)) {
    return '編輯工單'
  }
  const statusName = getStatusText(row.issueStatus)
  return `狀態為「${statusName}」不可編輯（僅 REPORTED/ASSIGNED 可編輯）`
}

// 控制結案彈窗
const showResolveDialog = ref(false)
const resolveForm = reactive({ ticketId: 0, resultType: 'FIXED', resolveNote: '' })

// 使用共用 composables
const {
  priorityConfig,
  statusConfig,
  resultConfig,
  getPriorityTag,
  getStatusTag,
  getPriorityText,
  getStatusText,
  getPriorityIcon,
  getStatusIcon,
  getResultText,
  getResultIcon,
} = useTicketConfig()

// 向後相容：保留原有變數名稱 (供模板使用)
const priorityText = Object.fromEntries(Object.entries(priorityConfig).map(([k, v]) => [k, v.text]))
const priorityIcon = Object.fromEntries(Object.entries(priorityConfig).map(([k, v]) => [k, v.icon]))
const statusText = Object.fromEntries(Object.entries(statusConfig).map(([k, v]) => [k, v.text]))
const statusIcon = Object.fromEntries(Object.entries(statusConfig).map(([k, v]) => [k, v.icon]))

// --- API 資料讀取 ---
const fetchTickets = async () => {
  try {
    loading.value = true
    const res = props.historyMode
      ? await maintenanceApi.getHistoryTickets()
      : await maintenanceApi.getActiveTickets()
    tickets.value = res.data
  } catch {
    // 錯誤已由 http.js 攔截器處理
  } finally {
    loading.value = false
  }
}

// 統計卡片數據
const statsCards = computed(() => {
  const total = tickets.value.length
  const urgent = tickets.value.filter((t) => t.issuePriority === 'URGENT').length
  const inProgress = tickets.value.filter((t) => t.issueStatus === 'UNDER_MAINTENANCE').length
  const resolved = tickets.value.filter((t) => t.issueStatus === 'RESOLVED').length
  return { total, urgent, inProgress, resolved }
})

// 監聽篩選條件變更，重置分頁
watch(
  filters,
  () => {
    resetPagination()
  },
  { deep: true },
)

// ★ [Fix Issue 1] 根據模式動態產生狀態選項
// 如果是 historyMode，只顯示 已結案/已取消
// 如果是 activeMode，只顯示 已通報/已指派/維修中
const availableStatusOptions = computed(() => {
  const allStatuses = statusText
  const filtered = {}

  if (props.historyMode) {
    // 歷史模式：只顯示 RESOLVED, CANCELLED
    if (allStatuses['RESOLVED']) filtered['RESOLVED'] = allStatuses['RESOLVED']
    if (allStatuses['CANCELLED']) filtered['CANCELLED'] = allStatuses['CANCELLED']
  } else {
    // 現有模式：顯示 REPORTED, ASSIGNED, UNDER_MAINTENANCE
    if (allStatuses['REPORTED']) filtered['REPORTED'] = allStatuses['REPORTED']
    if (allStatuses['ASSIGNED']) filtered['ASSIGNED'] = allStatuses['ASSIGNED']
    if (allStatuses['UNDER_MAINTENANCE'])
      filtered['UNDER_MAINTENANCE'] = allStatuses['UNDER_MAINTENANCE']
  }
  return filtered
})

// --- 業務邏輯 & 排序邏輯 ---
const filteredTickets = computed(() => {
  // 1. 先進行篩選
  let list = tickets.value.filter((t) => {
    const k = filters.keyword.toLowerCase()
    const textMatch =
      !k ||
      String(t.ticketId).includes(k) ||
      (t.issueDesc || '').toLowerCase().includes(k) ||
      (t.issueType || '').toLowerCase().includes(k)
    const pMatch = !filters.priority || t.issuePriority === filters.priority
    const sMatch = !filters.status || t.issueStatus === filters.status
    return textMatch && pMatch && sMatch
  })

  // 【新增】2. 根據 viewMode 過濾問題回報
  if (viewMode.value === 'support') {
    list = list.filter(t => isSupportTicket(t))
  }

  // 3. 進行排序：緊急工單 (URGENT) 置頂
  return list.sort((a, b) => {
    // 如果 a 是緊急，b 不是，a 排前面 (-1)
    if (a.issuePriority === 'URGENT' && b.issuePriority !== 'URGENT') return -1
    // 如果 b 是緊急，a 不是，b 排前面 (1)
    if (b.issuePriority === 'URGENT' && a.issuePriority !== 'URGENT') return 1

    // 如果優先級相同，依照 ID 倒序 (新的在上面)
    return b.ticketId - a.ticketId
  })
})

// 使用 usePagination composable
const {
  currentPage,
  pageSize,
  paginatedList: paginatedTickets,
  total: paginationTotal,
  showPagination,
  resetPagination,
} = usePagination(filteredTickets, { defaultPageSize: 10 })

// 開始維修
const startTicket = async (row) => {
  const result = await Swal.fire({
    title: '開始維修？',
    html: `
      <div style="text-align: center; padding: 10px 0;">
        <div style="font-size: 48px; margin-bottom: 12px;"><i class="fas fa-wrench" style="color: #e6a23c;"></i></div>
        <p>工單 <b>#${row.ticketId}</b> 即將進入維修狀態</p>
        <p style="color: #909399; font-size: 13px;">問題類型：${row.issueType}</p>
      </div>
    `,
    icon: null,
    showCancelButton: true,
    confirmButtonColor: '#409eff',
    cancelButtonColor: '#909399',
    confirmButtonText: '<i class="fas fa-play mr-1"></i> 開始維修',
    cancelButtonText: '稍後再說',
    showClass: { popup: 'animate__animated animate__bounceIn' },
  })

  if (result.isConfirmed) {
    try {
      await maintenanceApi.startTicket(row.ticketId)
      await Swal.fire({
        icon: 'success',
        title: '維修開始！',
        html: '<span class="text-primary">工單狀態已更新為「維修中」</span>',
        timer: 1000,
        timerProgressBar: true,
        showConfirmButton: false,
        showClass: { popup: 'animate__animated animate__fadeInUp animate__faster' },
      })
      fetchTickets()
    } catch {
      // 錯誤已由攔截器處理
    }
  }
}

// 取消工單
const cancelTicket = async (row) => {
  const { value: reason } = await Swal.fire({
    title: '取消工單',
    html: `
      <div style="text-align: center; padding: 10px 0;">
        <div style="font-size: 48px; margin-bottom: 12px;"><i class="fas fa-exclamation-triangle" style="color: #f56c6c;"></i></div>
        <p style="margin-bottom: 16px;">工單 <b>#${row.ticketId}</b> - ${row.issueType}</p>
      </div>
    `,
    input: 'textarea',
    inputPlaceholder: '請輸入取消原因...',
    inputAttributes: { rows: 3 },
    showCancelButton: true,
    confirmButtonColor: '#f56c6c',
    cancelButtonColor: '#909399',
    confirmButtonText: '<i class="fas fa-times mr-1"></i> 確認取消',
    cancelButtonText: '返回',
    showClass: { popup: 'animate__animated animate__fadeInDown animate__faster' },
    inputValidator: (value) => {
      if (!value) return '請輸入取消原因！'
    },
  })

  if (reason) {
    try {
      await maintenanceApi.cancelTicket(row.ticketId, reason)
      await Swal.fire({
        icon: 'success',
        title: '工單已取消',
        timer: 1000,
        timerProgressBar: true,
        showConfirmButton: false,
        showClass: { popup: 'animate__animated animate__bounceIn' },
      })
      fetchTickets()
    } catch {
      // 錯誤已由攔截器處理
    }
  }
}

// 開啟結案彈窗
const openResolveDialog = (id) => {
  resolveForm.ticketId = id
  resolveForm.resultType = 'FIXED'
  resolveForm.resolveNote = ''
  showResolveDialog.value = true
}

// 送出結案
const submitResolve = async () => {
  try {
    // ✅ [除錯用] 在瀏覽器 Console 印出傳送的資料，請按 F12 查看
    console.log('📤 準備結案，傳送資料:', {
      ticketId: resolveForm.ticketId,
      resultType: resolveForm.resultType, // 必須是英文大寫 Key (FIXED, MAINTAINED, UNFIXABLE, OTHER)
      resolveNote: resolveForm.resolveNote,
    })

    await maintenanceApi.resolveTicket(
      resolveForm.ticketId,
      resolveForm.resultType, // ✅ 直接傳送英文 Key，由 resultConfig 保證正確性
      resolveForm.resolveNote,
    )
    showResolveDialog.value = false

    const config = resultConfig[resolveForm.resultType] || {
      text: '已結案',
      icon: '🎉',
      color: '#67c23a',
    }

    await Swal.fire({
      icon: 'success',
      title: '結案成功！',
      html: `
        <div style="text-align: center;">
          <div style="font-size: 48px; margin-bottom: 12px;">${config.icon}</div>
          <p>結案結果：<b style="color: ${config.color};">${config.text}</b></p>
        </div>
      `,
      timer: 1200,
      timerProgressBar: true,
      showConfirmButton: false,
      position: 'center',
      heightAuto: false,
      showClass: { popup: 'animate__animated animate__tada' },
    })
    fetchTickets()
  } catch (error) {
    console.error('❌ 結案失敗:', error)
    console.error('❌ 後端回傳:', error.response?.data)
    console.error('❌ HTTP 狀態:', error.response?.status)

    // ✅ 顯示更詳細的錯誤給使用者
    const errorMsg =
      error.response?.data?.message ||
      error.response?.data?.error ||
      '後端不接受此結果類型 (Enum不匹配)'
    await Swal.fire({
      icon: 'error',
      title: '結案失敗',
      html: `
        <div style="text-align: left; padding: 10px;">
          <p><b>錯誤訊息：</b>${errorMsg}</p>
          ${error.response?.status ? `<p><b>HTTP 狀態碼：</b>${error.response.status}</p>` : ''}
          <hr style="margin: 12px 0; border: none; border-top: 1px solid #eee;">
          <p style="color: #909399; font-size: 13px;">
            <i class="fas fa-info-circle"></i> 如果出現 IllegalArgumentException，表示後端不支援該結果類型。<br>
            請確認後端已重啟並支援：FIXED, MAINTAINED, UNFIXABLE, OTHER
          </p>
        </div>
      `,
      confirmButtonColor: '#409eff',
      position: 'center',
      heightAuto: false,
    })
  }
}

// 查看工單詳情 (UI 優化版)
const viewTicketDetail = (row) => {
  // 準備變數
  const staffName = row.assignedStaff ? row.assignedStaff.staffName : '未指派'
  const staffColor = row.assignedStaff ? '#409eff' : '#909399' // 藍色或灰色

  // 處理維修結果區塊
  let resultHtml = ''
  if (row.issueStatus === 'RESOLVED' && row.resultType) {
    const rConfig = resultConfig[row.resultType] || { text: row.resultType, icon: '' }
    // 簡單的灰色背景區塊
    resultHtml = `
      <div style="margin-top: 15px; padding: 12px; background: #f4f4f5; border-radius: 8px; border-left: 4px solid #909399;">
        <div style="font-weight: bold; color: #606266; margin-bottom: 4px;">維修結果：${rConfig.text}</div>
        <div style="font-size: 13px; color: #909399;">${row.resolveNote || '無備註'}</div>
      </div>
    `
  }

  Swal.fire({
    // 標題簡潔化
    title: `<div style="display:flex; align-items:center; gap:8px;">
              <span style="font-size: 20px; font-weight:700;">工單 #${row.ticketId}</span>
              <span style="font-size: 14px; font-weight:400; color:#909399;">${row.issueType}</span>
            </div>`,
    html: `
      <div style="text-align: left; padding: 0 10px;">
        <div style="margin-bottom: 16px;">
          <div style="font-size: 12px; color: #909399; margin-bottom: 4px;">問題描述</div>
          <div style="padding: 12px; background: #fff; border: 1px solid #e4e7ed; border-radius: 8px; color: #606266; min-height: 40px;">
            ${row.issueDesc || '無詳細描述'}
          </div>
        </div>

        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 16px;">
          <div>
            <div style="font-size: 12px; color: #909399; margin-bottom: 4px;">優先級</div>
            <div style="font-size: 16px; font-weight: 600;">
              ${priorityIcon[row.issuePriority]} ${priorityText[row.issuePriority]}
            </div>
          </div>
          <div>
            <div style="font-size: 12px; color: #909399; margin-bottom: 4px;">目前狀態</div>
            <div style="font-size: 16px; font-weight: 600;">
              ${statusIcon[row.issueStatus]} ${statusText[row.issueStatus]}
            </div>
          </div>
        </div>

        <div style="margin-bottom: 16px; padding-top: 12px; border-top: 1px solid #ebeef5;">
          <div style="font-size: 12px; color: #909399; margin-bottom: 4px;">負責人員</div>
          <div style="display: flex; align-items: center; gap: 8px;">
            <div style="width: 32px; height: 32px; background: ${staffColor}; color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center;">
              <i class="fas fa-user"></i>
            </div>
            <span style="font-size: 15px; font-weight: 500; color: #303133;">${staffName}</span>
          </div>
        </div>

        ${resultHtml}

        <div style="margin-top: 20px;">
           <button id="btn-view-log" style="width: 100%; padding: 10px; border: 1px dashed #dcdfe6; background: #fff; color: #606266; border-radius: 6px; cursor: pointer; transition: all 0.3s;">
             <i class="fas fa-history mr-1"></i> 查看完整歷程
           </button>
        </div>
      </div>
    `,
    showConfirmButton: false,
    showCloseButton: true,
    width: 450,
    didOpen: () => {
      // 綁定歷程按鈕
      document.getElementById('btn-view-log')?.addEventListener('click', () => {
        Swal.close()
        openLogDialog(row.ticketId)
      })
    },
  })
}

// 地圖小視窗功能
// ====== 修正後的開啟地圖函式 (修復椅子工單座標顯示) ======
const showLocationMap = async (row) => {
  let lat = null
  let lng = null
  let targetName = ''

  console.log('🗺️ 開啟地圖，工單資料:', row)

  // 1. 判斷是機台還是椅子，並取得座標
  if (row.spotId) {
    // 機台工單：優先從 row.rentalSpot 取座標（已經在工單資料裡）
    targetName = row.rentalSpot?.spotName || `機台 #${row.spotId}`
    lat = row.rentalSpot?.latitude
    lng = row.rentalSpot?.longitude

    console.log('📍 機台工單 - rentalSpot 座標:', { lat, lng, spotName: targetName })

    // 如果 rentalSpot 沒有座標，再從 allSpots 找（備援方案）
    if (!lat || !lng) {
      const foundSpot = allSpots.value.find((s) => Number(s.spotId) === Number(row.spotId))
      console.log('🔍 從 allSpots 找機台:', { foundSpot, allSpotsCount: allSpots.value.length })
      if (foundSpot) {
        //  修正原因：後端已修復座標傳輸，此處加入 Number 轉型與 null 檢查作為保險
        const spotLat = Number(foundSpot.latitude)
        const spotLng = Number(foundSpot.longitude)

        if (!isNaN(spotLat) && !isNaN(spotLng) && spotLat !== 0 && spotLng !== 0) {
          lat = spotLat
          lng = spotLng
          targetName = foundSpot.spotName || targetName
        }
      }
    }
  } else if (row.seatsId) {
    // 椅子工單：取得所屬機台的座標
    targetName = row.seat?.seatsName || `椅子 #${row.seatsId}`

    if (row.seat && row.seat.spotId) {
      console.log('🪑 椅子工單 - 所屬機台 ID:', row.seat.spotId)

      // ✅ 直接從 allSpots 找椅子所屬的機台（因為 seat.rentalSpot 通常沒有座標）
      const foundSpot = allSpots.value.find((s) => Number(s.spotId) === Number(row.seat.spotId))
      console.log('🔍 從 allSpots 找椅子所屬機台:', {
        foundSpot: foundSpot
          ? {
              spotId: foundSpot.spotId,
              spotName: foundSpot.spotName,
              lat: foundSpot.latitude,
              lng: foundSpot.longitude,
            }
          : null,
        searchSpotId: row.seat.spotId,
      })

      // ✅ 修正原因：後端已修復座標傳輸，此處加入 Number 轉型與 null 檢查作為保險
      if (foundSpot) {
        const spotLat = Number(foundSpot.latitude)
        const spotLng = Number(foundSpot.longitude)

        // ✅ 檢查座標是否為有效數字（不是 NaN、不是 0、不是 null）
        if (!isNaN(spotLat) && !isNaN(spotLng) && spotLat !== 0 && spotLng !== 0) {
          lat = spotLat
          lng = spotLng
          targetName = `${row.seat.seatsName || '椅子'} (位於 ${foundSpot.spotName || '機台'})`
          console.log('✅ 成功取得椅子所屬機台座標:', { lat, lng, targetName })
        } else {
          console.warn('⚠️ 機台座標無效:', { spotLat, spotLng })
          Swal.fire('無座標資訊', `椅子所屬的機台「${foundSpot.spotName}」未設定有效經緯度`, 'info')
          return
        }
      } else {
        console.warn('⚠️ 找不到椅子所屬機台')
        Swal.fire('無座標資訊', `找不到椅子所屬的機台（ID: ${row.seat.spotId}）`, 'info')
        return
      }
    } else {
      Swal.fire('無法定位', '這張椅子工單找不到所屬的機台資訊', 'warning')
      return
    }
  }

  // 2. 檢查是否有座標
  if (lat && lng) {
    const stationName = targetName

    // Google Maps Embed URL
    const mapUrl = `https://maps.google.com/maps?q=${lat},${lng}&z=15&output=embed`

    // 3. 彈出 Swal 視窗
    await Swal.fire({
      title: `<div style="display: flex; align-items: center; gap: 12px; justify-content: center;">
          <i class="fas fa-map-marker-alt" style="color: #e6a23c; font-size: 24px;"></i>
          <span>${stationName}</span>
        </div>`,
      html: `
          <div style="text-align: center;">
            <div style="margin-bottom: 16px; padding: 12px; background: #f0f9eb; border-radius: 8px; border-left: 4px solid #67c23a;">
              <p style="margin: 0; color: #606266; font-size: 13px;">
                <i class="fas fa-info-circle mr-1" style="color: #67c23a;"></i>
                經度：${lng}° | 緯度：${lat}°
              </p>
            </div>
            <iframe
              src="${mapUrl}"
              width="100%"
              height="300"
              style="border: none; border-radius: 10px; box-shadow: 0 4px 15px rgba(0,0,0,0.1);"
              allowfullscreen=""
              loading="lazy"
              referrerpolicy="no-referrer-when-downgrade">
            </iframe>
            <p style="margin: 12px 0 0; color: #909399; font-size: 11px;">
              <i class="fas fa-external-link-alt mr-1"></i>
              點擊地圖可在新視窗中開啟 Google Maps
            </p>
          </div>
        `,
      width: '600px',
      showConfirmButton: true,
      confirmButtonText: '<i class="fas fa-times mr-1"></i>關閉',
      confirmButtonColor: '#909399',
      customClass: {
        popup: 'custom-map-popup',
      },
      showClass: { popup: 'animate__animated animate__zoomIn animate__faster' },
      hideClass: { popup: 'animate__animated animate__zoomOut animate__faster' },
    })
  } else {
    console.error('❌ 最終檢查：沒有座標資訊', { lat, lng, targetName })
    Swal.fire('無座標資訊', `無法取得「${targetName}」的經緯度資訊`, 'info')
  }
}

// 切換模式時重新抓資料
watch(
  () => props.historyMode,
  () => {
    fetchTickets()
  },
)

// ====== (載入資料) ======
onMounted(async () => {
  try {
    loading.value = true

    //  同時載入「資產統計」和「機台資料」，工單則透過 fetchTickets 根據模式載入
    const [statsRes, spotsRes] = await Promise.all([
      maintenanceApi.getAssetStats(assetStatsTab.value),
      maintenanceApi.getAllSpots(), // 抓取機台資料供地圖使用
    ])

    // 存入變數
    assetStats.value = statsRes.data || []
    allSpots.value = spotsRes.data || []

    // ✅ 根據 historyMode 載入對應的工單資料
    await fetchTickets()
  } catch (err) {
    console.error('載入初始資料失敗:', err)
    Swal.fire('錯誤', '無法載入資料', 'error')
  } finally {
    loading.value = false
    // ✅ 【關鍵修復】設置頁面可見，否則 v-show="pageVisible" 會隱藏所有內容！
    pageVisible.value = true
  }
})
</script>

<template>
  <div class="ticket-list-container">
    <section class="content-header">
      <div class="container-fluid">
        <transition name="slide-down" appear>
          <div class="page-title-box">
            <div class="title-icon" :class="historyMode ? 'history-mode' : 'active-mode'">
              <i :class="historyMode ? 'fas fa-archive' : 'fas fa-clipboard-list'"></i>
            </div>
            <div class="title-content">
              <h1>{{ historyMode ? '維修歷史檔案' : '維修工單管理' }}</h1>
              <p class="subtitle">
                {{ historyMode ? '查看已完成或取消的工單紀錄' : '管理與追蹤所有維修工單' }}
              </p>
            </div>
            <div class="title-actions">
              <el-button-group>
                <router-link v-if="historyMode" to="/admin/mtif-list">
                  <el-button type="primary" plain class="action-btn">
                    <i class="fas fa-arrow-left mr-2"></i> 返回列表
                  </el-button>
                </router-link>
                <router-link v-if="!historyMode" to="/admin/mtif-history">
                  <el-button type="info" plain class="action-btn">
                    <i class="fas fa-history mr-2"></i> 歷史紀錄
                  </el-button>
                </router-link>
                <router-link v-if="!historyMode" to="/admin/mtif-form">
                  <el-button type="success" class="action-btn add-btn">
                    <i class="fas fa-plus mr-2"></i> 新增工單
                  </el-button>
                </router-link>
              </el-button-group>
            </div>
          </div>
        </transition>

        <!-- 【新增】檢視模式切換（僅在現有工單模式顯示） -->
        <transition name="fade" appear v-if="!historyMode">
          <div class="view-mode-switch mb-3">
            <el-segmented v-model="viewMode" size="large" block>
              <el-segmented-item value="all">
                <span class="segmented-label">
                  <i class="fas fa-list mr-2"></i>
                  全部工單
                </span>
              </el-segmented-item>
              <el-segmented-item value="support">
                <span class="segmented-label">
                  <i class="fas fa-life-ring mr-2"></i>
                  問題回報
                  <el-badge 
                    v-if="supportPendingCount > 0" 
                    :value="supportPendingCount" 
                    type="danger" 
                    class="ml-2"
                  />
                </span>
              </el-segmented-item>
            </el-segmented>
          </div>
        </transition>
      </div>
    </section>

    <section class="content">
      <div class="container-fluid">
        <transition name="zoom-fade" appear>
          <div v-show="pageVisible">
            <el-row :gutter="16" class="mb-4" v-if="!historyMode">
              <el-col :xs="12" :sm="6" :md="6">
                <div class="stat-card total-card">
                  <div class="stat-icon">
                    <i class="fas fa-clipboard-list"></i>
                  </div>
                  <div class="stat-info">
                    <h3>{{ statsCards.total }}</h3>
                    <span>全部工單</span>
                  </div>
                  <div class="stat-wave"></div>
                </div>
              </el-col>
              <el-col :xs="12" :sm="6" :md="6">
                <div class="stat-card urgent-card">
                  <div class="stat-icon pulse">
                    <i class="fas fa-exclamation-triangle"></i>
                  </div>
                  <div class="stat-info">
                    <h3>{{ statsCards.urgent }}</h3>
                    <span>緊急工單</span>
                  </div>
                </div>
              </el-col>
              <el-col :xs="12" :sm="6" :md="6">
                <div class="stat-card progress-card">
                  <div class="stat-icon">
                    <i class="fas fa-tools"></i>
                  </div>
                  <div class="stat-info">
                    <h3>{{ statsCards.inProgress }}</h3>
                    <span>維修中</span>
                  </div>
                </div>
              </el-col>
              <el-col :xs="12" :sm="6" :md="6">
                <div class="stat-card resolved-card">
                  <div class="stat-icon">
                    <i class="fas fa-check-circle"></i>
                  </div>
                  <div class="stat-info">
                    <h3>{{ statsCards.resolved }}</h3>
                    <span>已完成</span>
                  </div>
                </div>
              </el-col>
            </el-row>

            <TicketCharts :tickets="filteredTickets" class="mb-4" />

            <el-card shadow="hover" class="mb-4 asset-stats-card" v-if="!historyMode">
              <template #header>
                <div class="card-header-content">
                  <div class="header-left">
                    <span
                      class="header-icon"
                      style="background: linear-gradient(135deg, #67c23a, #95d475)"
                    >
                      <i class="fas fa-heartbeat"></i>
                    </span>
                    <span class="header-text">資產健康度統計</span>
                    <el-tag type="success" effect="light" size="small" class="ml-2" round
                      >最近 7 天</el-tag
                    >
                  </div>
                  <div class="header-right">
                    <el-radio-group v-model="assetStatsTab" size="small">
                      <el-radio-button value="SPOT"
                        ><i class="fas fa-desktop mr-1"></i> 機台</el-radio-button
                      >
                      <el-radio-button value="SEAT"
                        ><i class="fas fa-chair mr-1"></i> 椅子</el-radio-button
                      >
                    </el-radio-group>
                    <el-button type="info" plain size="small" @click="fetchAssetStats" class="ml-2">
                      <i class="fas fa-sync-alt"></i>
                    </el-button>
                  </div>
                </div>
              </template>

              <el-skeleton :rows="4" animated v-if="assetStatsLoading" />
              <el-empty v-else-if="assetStats.length === 0" description="暫無統計資料" />
              <el-table v-else :data="assetStats" stripe style="width: 100%" max-height="400">
                <el-table-column prop="assetName" label="資產名稱" min-width="150" fixed>
                  <template #default="{ row }">
                    <div style="display: flex; align-items: center; gap: 8px">
                      <i
                        :class="row.assetType === 'SPOT' ? 'fas fa-desktop' : 'fas fa-chair'"
                        :style="{ color: row.assetType === 'SPOT' ? '#409eff' : '#e6a23c' }"
                      ></i>
                      <span>{{ row.assetName || '未知資產#' + row.assetId }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="維修次數" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag
                      :type="row.repairCount > 0 ? 'danger' : 'info'"
                      effect="light"
                      size="small"
                      >{{ row.repairCount || 0 }}</el-tag
                    >
                  </template>
                </el-table-column>
                <el-table-column label="保養次數" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag type="primary" effect="light" size="small">{{
                      row.maintainCount || 0
                    }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="未結案" width="90" align="center">
                  <template #default="{ row }">
                    <el-tag
                      :type="row.openCount > 0 ? 'warning' : 'success'"
                      effect="plain"
                      size="small"
                      >{{ row.openCount || 0 }}</el-tag
                    >
                  </template>
                </el-table-column>
                <el-table-column label="妥善率" width="140" align="center">
                  <template #default="{ row }">
                    <el-progress
                      :percentage="Math.round((row.availability || 0) * 100)"
                      :status="getAvailabilityStatus(row.availability)"
                      :stroke-width="10"
                      style="width: 100px; display: inline-block"
                    />
                    <span style="margin-left: 8px; font-size: 12px; color: #606266">{{
                      formatPercent(row.availability)
                    }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="故障率(/天)" width="110" align="center">
                  <template #default="{ row }">
                    <span
                      :style="{
                        color: row.failureRatePerDay > 0.5 ? '#f56c6c' : '#67c23a',
                        fontWeight: 'bold',
                      }"
                      >{{ formatRate(row.failureRatePerDay) }}</span
                    >
                  </template>
                </el-table-column>
                <el-table-column label="維修率" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag
                      :type="
                        row.repairRate >= 1 ? 'success' : row.repairRate > 0 ? 'warning' : 'info'
                      "
                      effect="plain"
                      size="small"
                      >{{ formatPercent(row.repairRate) }}</el-tag
                    >
                  </template>
                </el-table-column>
                <el-table-column label="停機時間" width="100" align="center">
                  <template #default="{ row }">
                    <span style="color: #909399; font-size: 12px"
                      >{{ row.downtimeMinutes || 0 }} 分鐘</span
                    >
                  </template>
                </el-table-column>
              </el-table>
            </el-card>

            <el-card shadow="hover" class="table-card">
              <template #header>
                <div class="card-header-content">
                  <div class="header-left">
                    <span class="header-icon">
                      <i class="fas fa-table"></i>
                    </span>
                    <span class="header-text">工單列表</span>
                    <el-tag type="primary" effect="light" size="small" class="ml-2" round>
                      {{ filteredTickets.length }} 筆
                    </el-tag>
                  </div>
                </div>
              </template>

              <div class="filter-bar">
                <el-input
                  v-model="filters.keyword"
                  placeholder="搜尋 ID、描述、類型..."
                  prefix-icon="Search"
                  clearable
                  class="filter-input"
                />
                <el-select
                  v-model="filters.priority"
                  placeholder="優先級"
                  clearable
                  class="filter-select"
                >
                  <el-option label="🔵 低" value="LOW" />
                  <el-option label="🟢 普通" value="NORMAL" />
                  <el-option label="🟠 高" value="HIGH" />
                  <el-option label="🔴 緊急" value="URGENT" />
                </el-select>
                <el-select
                  v-model="filters.status"
                  placeholder="狀態"
                  clearable
                  class="filter-select"
                >
                  <el-option
                    v-for="(val, key) in availableStatusOptions"
                    :key="key"
                    :label="`${statusIcon[key]} ${val}`"
                    :value="key"
                  />
                </el-select>
                <el-button type="info" plain @click="fetchTickets" class="refresh-btn">
                  <i class="fas fa-sync-alt"></i>
                </el-button>
              </div>

              <el-skeleton :rows="8" animated v-if="loading" />

              <el-table
                v-else
                :data="paginatedTickets"
                stripe
                highlight-current-row
                style="width: 100%"
                class="custom-table"
                @row-dblclick="viewTicketDetail"
              >
                <el-table-column prop="ticketId" label="ID" width="80" sortable fixed>
                  <template #default="{ row }">
                    <el-tag effect="plain" size="small" class="id-tag">#{{ row.ticketId }}</el-tag>
                  </template>
                </el-table-column>

                <el-table-column label="維修目標" width="180" align="center">
                  <template #default="{ row }">
                    <div v-if="row.seatsId" class="target-cell seat-target">
                      <div class="target-main">
                        <i class="fas fa-chair" style="color: #e6a23c"></i>
                        <!--  改成椅子真名（優先 seatsName，沒有才 fallback #id） -->
                        <span>{{ row.seat?.seatsName || `椅子 #${row.seatsId}` }}</span>
                      </div>
                      <div v-if="row.seat && row.seat.spotId" class="target-station">
                        <span class="station-link" @click="showLocationMap(row)">
                          <i class="fas fa-map-marker-alt mr-1"></i>
                          <!--  改成機台真名：優先 row.rentalSpot.spotName，否則用 allSpots 查 spotName -->
                          {{ getSpotDisplayName(row.seat.spotId, row.rentalSpot) }}
                        </span>
                      </div>
                    </div>
                    <div v-else class="target-cell spot-target">
                      <div class="target-main">
                        <i class="fas fa-desktop" style="color: #409eff"></i>
                        <span>機台 #{{ row.spotId }}</span>
                      </div>
                      <div v-if="row.rentalSpot" class="target-station">
                        <span class="station-link" @click="showLocationMap(row)">
                          <i class="fas fa-map-marker-alt mr-1"></i>
                          {{ row.rentalSpot.spotName }}
                        </span>
                      </div>
                    </div>
                  </template>
                </el-table-column>

                <el-table-column prop="issueType" label="問題類型" width="160">
                  <template #default="{ row }">
                    <div class="type-cell" @click="viewTicketDetail(row)">
                      <i class="fas fa-exclamation-circle type-icon"></i>
                      <span>{{ row.issueType }}</span>
                      <!-- 【新增】問題回報標記 -->
                      <el-tag 
                        v-if="isSupportTicket(row)" 
                        type="warning" 
                        size="small" 
                        effect="plain"
                        style="margin-left: 4px;"
                      >
                        <i class="fas fa-life-ring" style="margin-right: 2px;"></i>
                        回報
                      </el-tag>
                    </div>
                  </template>
                </el-table-column>

                <el-table-column
                  prop="issueDesc"
                  label="描述"
                  min-width="200"
                  show-overflow-tooltip
                >
                  <template #default="{ row }">
                    <span class="desc-cell">{{ row.issueDesc || '-' }}</span>
                  </template>
                </el-table-column>

                <el-table-column prop="issuePriority" label="優先級" width="110" align="center">
                  <template #default="{ row }">
                    <!-- ✅ 【修正】優先級 badge 文字顏色對比度 (深色背景使用白字) -->
                    <el-tag
                      :type="getPriorityTag(row.issuePriority)"
                      effect="dark"
                      round
                      style="border: none; white-space: nowrap; color: white; font-weight: 600"
                    >
                      {{ priorityIcon[row.issuePriority] }} {{ priorityText[row.issuePriority] }}
                    </el-tag>
                  </template>
                </el-table-column>

                <el-table-column label="維修人員" width="120" align="center">
                  <template #default="{ row }">
                    <div v-if="row.assignedStaff">
                      <el-tag effect="plain" type="info" round size="small">
                        <i class="fas fa-user-check mr-1"></i>
                        {{ row.assignedStaff.staffName }}
                      </el-tag>
                    </div>
                    <div v-else>
                      <span style="color: #909399; font-size: 12px">- 未指派 -</span>
                    </div>
                  </template>
                </el-table-column>

                <el-table-column prop="issueStatus" label="狀態" width="130" align="center">
                  <template #default="{ row }">
                    <el-tag :type="getStatusTag(row.issueStatus)" effect="light" class="status-tag">
                      {{ statusIcon[row.issueStatus] }} {{ statusText[row.issueStatus] }}
                    </el-tag>
                  </template>
                </el-table-column>

                <el-table-column label="操作" width="240" align="center" fixed="right">
                  <template #default="{ row }">
                    <div class="action-buttons">
                      <el-tooltip content="查看詳情" placement="top">
                        <el-button
                          type="info"
                          size="small"
                          circle
                          @click="viewTicketDetail(row)"
                          class="action-btn-item"
                        >
                          <i class="fas fa-eye"></i>
                        </el-button>
                      </el-tooltip>

                      <el-tooltip content="查看歷程" placement="top">
                        <el-button
                          type="warning"
                          size="small"
                          circle
                          @click="openLogDialog(row.ticketId)"
                          class="action-btn-item"
                        >
                          <i class="fas fa-history"></i>
                        </el-button>
                      </el-tooltip>

                      <el-tooltip
                        v-if="!historyMode"
                        :content="getEditTooltip(row)"
                        placement="top"
                      >
                        <el-button
                          size="small"
                          circle
                          :disabled="!canEdit(row)"
                          @click="$router.push(`/admin/mtif-form/${row.ticketId}`)"
                          class="action-btn-item"
                        >
                          <i class="fas fa-edit"></i>
                        </el-button>
                      </el-tooltip>

                      <el-tooltip
                        v-if="!historyMode && row.issueStatus === 'ASSIGNED'"
                        content="開始維修"
                        placement="top"
                      >
                        <el-button
                          type="primary"
                          size="small"
                          circle
                          @click="startTicket(row)"
                          class="action-btn-item"
                        >
                          <i class="fas fa-play"></i>
                        </el-button>
                      </el-tooltip>

                      <el-tooltip
                        v-if="!historyMode && row.issueStatus === 'UNDER_MAINTENANCE'"
                        content="結案"
                        placement="top"
                      >
                        <el-button
                          type="success"
                          size="small"
                          circle
                          @click="openResolveDialog(row.ticketId)"
                          class="action-btn-item"
                        >
                          <i class="fas fa-check"></i>
                        </el-button>
                      </el-tooltip>

                      <el-tooltip
                        v-if="!historyMode && !['RESOLVED', 'CANCELLED'].includes(row.issueStatus)"
                        content="取消工單"
                        placement="top"
                      >
                        <el-button
                          type="danger"
                          size="small"
                          circle
                          @click="cancelTicket(row)"
                          class="action-btn-item"
                        >
                          <i class="fas fa-times"></i>
                        </el-button>
                      </el-tooltip>
                    </div>
                  </template>
                </el-table-column>

                <template #empty>
                  <el-empty description="目前沒有相關工單資料">
                    <template #image>
                      <div class="empty-icon"><i class="fas fa-clipboard"></i></div>
                    </template>
                    <router-link to="/admin/mtif-form" v-if="!historyMode">
                      <el-button type="primary"
                        ><i class="fas fa-plus mr-1"></i> 建立第一張工單</el-button
                      >
                    </router-link>
                  </el-empty>
                </template>
              </el-table>

              <div class="pagination-wrapper" v-show="paginationTotal > 0">
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
                  <span>💡 小提示：雙擊表格列可快速查看工單詳情 | 緊急工單會優先置頂顯示</span>
                </template>
              </el-alert>
            </div>
          </div>
        </transition>
      </div>
    </section>

    <el-dialog
      v-model="logDialogVisible"
      :title="`工單 #${currentLogTicketId}｜歷程`"
      width="760px"
      destroy-on-close
      append-to-body
      align-center
    >
      <TicketTimeline v-if="currentLogTicketId" :ticketId="currentLogTicketId" />
    </el-dialog>

    <el-dialog
      v-model="showResolveDialog"
      title=""
      width="500px"
      center
      destroy-on-close
      align-center
      append-to-body
      class="resolve-dialog"
    >
      <template #header>
        <div class="dialog-header">
          <span class="dialog-icon"
            ><i class="fas fa-check-circle" style="color: #67c23a; font-size: 24px"></i
          ></span>
          <span class="dialog-title">工單結案確認</span>
        </div>
      </template>
      <el-form label-position="top" class="resolve-form">
        <el-form-item label="維修結果">
          <div class="result-cards">
            <div
              v-for="(config, key) in resultConfig"
              :key="key"
              class="result-card"
              :class="{ active: resolveForm.resultType === key }"
              :style="{ '--card-color': config.color }"
              @click="resolveForm.resultType = key"
            >
              <span class="result-icon">{{ config.icon }}</span>
              <span class="result-text">{{ config.text }}</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="維修備註">
          <el-input
            v-model="resolveForm.resolveNote"
            type="textarea"
            :rows="4"
            placeholder="請填寫維修過程說明、更換零件、保養項目等資訊..."
            show-word-limit
            maxlength="500"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showResolveDialog = false" size="large">取消</el-button>
        <el-button type="primary" @click="submitResolve" size="large" class="confirm-btn"
          ><i class="fas fa-check mr-1"></i> 確認結案</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 頁面容器 */
.ticket-list-container {
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
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
  transition: all 0.4s ease;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.15);
}
.title-icon:hover {
  transform: scale(1.1) rotate(10deg);
}
.title-icon.active-mode {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
}
.title-icon.history-mode {
  background: linear-gradient(135deg, #909399 0%, #c0c4cc 100%);
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

/* 【新增】檢視模式切換樣式 */
.view-mode-switch {
  padding: 0 24px;
}

.segmented-label {
  display: flex;
  align-items: center;
  font-weight: 500;
}

:deep(.el-segmented) {
  border-radius: 12px;
  padding: 4px;
  background: white;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

:deep(.el-segmented-item) {
  border-radius: 10px;
  padding: 12px 24px;
  transition: all 0.3s ease;
}

:deep(.el-segmented-item--selected) {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%) !important;
  color: white !important;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

:deep(.el-badge__content) {
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}

/* 統計卡片 */
.stat-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  background: white;
  border-radius: 14px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.06);
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  overflow: hidden;
  margin-bottom: 16px;
}
.stat-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.12);
}
.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: white;
  z-index: 1;
}
.stat-icon.pulse {
  animation: pulse 1.5s infinite;
}
@keyframes pulse {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}
.total-card .stat-icon {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
}
.urgent-card .stat-icon {
  background: linear-gradient(135deg, #f56c6c 0%, #f89898 100%);
}
.progress-card .stat-icon {
  background: linear-gradient(135deg, #e6a23c 0%, #f3d19e 100%);
}
.resolved-card .stat-icon {
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
}
.stat-info h3 {
  margin: 0;
  font-size: 1.8rem;
  font-weight: 700;
  color: #303133;
}
.stat-info span {
  font-size: 0.85rem;
  color: #909399;
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

/* 篩選區 */
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 12px;
}
.filter-input {
  width: 280px;
}
.filter-input :deep(.el-input__wrapper) {
  border-radius: 10px;
}
.filter-select {
  width: 140px;
}
.filter-select :deep(.el-input__wrapper) {
  border-radius: 10px;
}
.refresh-btn {
  border-radius: 10px;
  transition: all 0.3s ease;
}
.refresh-btn:hover {
  transform: rotate(180deg);
}

/* 表格樣式 */
.custom-table {
  --el-table-header-bg-color: #f8f9fa;
}
.id-tag {
  font-weight: 600;
}

/* 維修目標欄位樣式 */
.target-cell {
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: 2px;
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.3s ease;
  text-align: center;
}
.seat-target {
  background: linear-gradient(135deg, #f0f9eb 0%, #e1f3d8 100%);
  color: #67c23a;
}
.seat-target:hover {
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.3);
}
.spot-target {
  background: linear-gradient(135deg, #ecf5ff 0%, #d9ecff 100%);
  color: #409eff;
}
.spot-target:hover {
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}
.target-main {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}
.target-station {
  font-size: 11px;
}
.station-link {
  color: #409eff;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
}
.station-link:hover {
  color: #66b1ff;
  background: #ecf5ff;
  transform: translateY(-1px);
}

.type-cell {
  cursor: pointer;
  transition: color 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 4px 8px;
  border-radius: 6px;
}
.type-cell:hover {
  color: #409eff;
  background: #ecf5ff;
}
.type-icon {
  margin-right: 6px;
  color: #f56c6c;
}
.desc-cell {
  color: #606266;
  font-size: 13px;
}

/* Tag 不換行 */
:deep(.el-tag) {
  white-space: nowrap;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 6px;
  flex-wrap: wrap;
}
.action-btn-item {
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.action-btn-item:hover {
  transform: scale(1.2);
}

.empty-icon {
  font-size: 64px;
  color: #dcdfe6;
  margin-bottom: 16px;
}
.pagination-wrapper {
  padding: 20px;
  text-align: center;
  border-top: 1px solid #ebeef5;
  background: #fafafa;
  margin-top: 20px;
}
.tips-bar :deep(.el-alert) {
  border-radius: 12px;
}

/* 結案彈窗 */
.resolve-dialog :deep(.el-dialog) {
  border-radius: 16px;
  margin: auto !important; /* ✅ 強制置中，避免偏高問題 */
}
.dialog-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}
.dialog-icon {
  font-size: 28px;
}
.dialog-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.resolve-form {
  padding: 10px 0;
}
.result-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
.result-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14px 10px;
  background: #f5f7fa;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}
.result-card:hover {
  background: #ecf5ff;
  transform: translateY(-2px);
}
.result-card.active {
  border-color: var(--card-color);
  background: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.result-icon {
  font-size: 24px;
  margin-bottom: 6px;
}
.result-text {
  font-size: 12px;
  color: #606266;
  font-weight: 500;
}
.confirm-btn {
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
  border: none;
  border-radius: 10px;
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
.w-100 {
  width: 100%;
}

:global(.custom-map-popup) {
  border-radius: 16px !important;
  overflow: hidden !important;
}
</style>
