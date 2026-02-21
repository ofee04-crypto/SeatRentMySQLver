/**
 * 維護人員狀態計算邏輯 (統一使用，避免重複實作)
 * 
 * 用途：
 * - 根據 maintenanceInformation (tickets) 動態計算人員即時狀態
 * - 提供統一的狀態判斷邏輯給：統計卡、人員列表、人員詳情彈窗
 * 
 * 修改原因：
 * - 原本人員狀態是靜態欄位，沒有根據工單動態計算
 * - 導致「維修中」、「已指派」統計不準確
 * - 人員列表 badge 與實際工單狀態不一致
 */

/**
 * 判斷工單狀態是否為已完成 (RESOLVED / CANCELLED)
 * @param {string} status - 工單狀態
 * @returns {boolean}
 */
export function isCompletedStatus(status) {
  return status === 'RESOLVED' || status === 'CANCELLED'
}

/**
 * 判斷是否為保養任務 (根據問題類型或描述關鍵字)
 * @param {string} issueType - 問題類型
 * @param {string} issueDesc - 問題描述
 * @returns {boolean}
 */
export function isMaintenanceTask(issueType, issueDesc) {
  const type = String(issueType || '').trim()
  const desc = String(issueDesc || '').trim()

  if (!type && !desc) return false

  // 保養相關關鍵字 (涵蓋排程表單的 issueTypeOptions)
  const keywords = ['保養', '維護', '例行', '檢查', '清潔', '校正', '安全', '耗材', '更換']

  // 排程自動產生的保養任務標記
  const scheduleMarker = '【排程自動保養】'

  return keywords.some((k) => type.includes(k) || desc.includes(k)) || desc.includes(scheduleMarker)
}

/**
 * 計算人員即時狀態 (根據工單動態判斷)
 * 
 * 判斷優先級 (嚴格遵守)：
 * 1. 有 UNDER_MAINTENANCE → 維修中
 * 2. 無上者但有 ASSIGNED → 已指派  
 * 3. 其餘 → 閒置中
 * 
 * @param {number} staffId - 人員 ID
 * @param {Array} tickets - 所有工單列表 (maintenanceInformation)
 * @returns {Object} 狀態物件 { key, text, tagType, icon }
 */
export function computeStaffStatus(staffId, tickets) {
  // 篩選此人員的工單 (排除已完成的)
  const staffTickets = tickets.filter(
    (t) => t.assignedStaffId === staffId && !isCompletedStatus(t.issueStatus)
  )

  // 判斷 1: 有正在維修中的工單
  const hasUnderMaintenance = staffTickets.some((t) => t.issueStatus === 'UNDER_MAINTENANCE')
  if (hasUnderMaintenance) {
    return {
      key: 'UNDER_MAINTENANCE',
      text: '維修中',
      tagType: 'warning',
      icon: 'fas fa-wrench'
    }
  }

  // 判斷 2: 有已指派但尚未開始的工單
  const hasAssigned = staffTickets.some((t) => t.issueStatus === 'ASSIGNED')
  if (hasAssigned) {
    return {
      key: 'ASSIGNED',
      text: '已指派',
      tagType: 'primary',
      icon: 'fas fa-user-check'
    }
  }

  // 判斷 3: 閒置中 (無未完成工單)
  return {
    key: 'IDLE',
    text: '閒置中',
    tagType: 'success',
    icon: 'fas fa-mug-hot'
  }
}

/**
 * 計算人員任務統計 (區分維修/保養、未完成/已完成)
 * 
 * @param {number} staffId - 人員 ID
 * @param {Array} tickets - 所有工單列表
 * @returns {Object} 統計物件
 *   {
 *     repairCurrent: 維修中數量,
 *     maintainCurrent: 保養中數量,
 *     repairAssigned: 待修數量,
 *     maintainAssigned: 待保養數量,
 *     repairDone: 維修完成數量,
 *     maintainDone: 保養完成數量
 *   }
 */
export function computeStaffTaskStats(staffId, tickets) {
  const stats = {
    repairCurrent: 0,
    maintainCurrent: 0,
    repairAssigned: 0,
    maintainAssigned: 0,
    repairDone: 0,
    maintainDone: 0
  }

  // 篩選此人員的所有工單
  const staffTickets = tickets.filter((t) => t.assignedStaffId === staffId)

  staffTickets.forEach((t) => {
    const maintenance = isMaintenanceTask(t.issueType, t.issueDesc)
    const status = t.issueStatus
    const done = isCompletedStatus(status)
    const assigned = status === 'ASSIGNED'
    const inProgress = status === 'UNDER_MAINTENANCE'

    if (maintenance) {
      if (inProgress) stats.maintainCurrent++
      else if (assigned) stats.maintainAssigned++
      else if (done) stats.maintainDone++
    } else {
      if (inProgress) stats.repairCurrent++
      else if (assigned) stats.repairAssigned++
      else if (done) stats.repairDone++
    }
  })

  return stats
}

/**
 * 計算整體人員狀態統計 (用於統計卡)
 * 
 * @param {Array} staffList - 人員列表
 * @param {Array} tickets - 所有工單列表
 * @returns {Object} 統計物件
 *   {
 *     underMaintenance: 維修中人數,
 *     assigned: 已指派人數,
 *     idle: 閒置中人數
 *   }
 */
export function computeOverallStaffStats(staffList, tickets) {
  const stats = {
    underMaintenance: 0,
    assigned: 0,
    idle: 0
  }

  staffList.forEach((staff) => {
    const status = computeStaffStatus(staff.staffId, tickets)
    
    if (status.key === 'UNDER_MAINTENANCE') {
      stats.underMaintenance++
    } else if (status.key === 'ASSIGNED') {
      stats.assigned++
    } else {
      stats.idle++
    }
  })

  return stats
}
