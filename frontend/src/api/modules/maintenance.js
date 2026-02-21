/**
 * 維修模組 API
 * 封裝所有維修相關的 API 呼叫
 */
import http from '../http'

// ============ 維護人員 (Staff) API ============

/**
 * 取得所有在職維護人員
 */
export const getAllStaff = () => {
  return http.get('/maintenance/staff')
}

/**
 * ★ Bug2 修復：取得啟用中的維護人員（建立工單用）
 */
export const getActiveStaff = () => {
  return http.get('/maintenance/staff/active')
}

/**
 * 取得單一維護人員
 * @param {number} id - 人員 ID
 */
export const getStaffById = (id) => {
  return http.get(`/maintenance/staff/${id}`)
}

/**
 * 取得已停用的維護人員 (歷史紀錄)
 */
export const getInactiveStaff = () => {
  return http.get('/maintenance/staff/inactive')
}

/**
 * 新增維護人員
 * @param {Object} staff - 人員資料
 */
export const createStaff = (staff) => {
  return http.post('/maintenance/staff', staff)
}

/**
 * 更新維護人員
 * @param {number} id - 人員 ID
 * @param {Object} staff - 人員資料
 */
export const updateStaff = (id, staff) => {
  return http.put(`/maintenance/staff/${id}`, staff)
}

/**
 * 刪除維護人員 (軟刪除)
 * @param {number} id - 人員 ID
 */
export const deleteStaff = (id) => {
  return http.delete(`/maintenance/staff/${id}`)
}

/**
 * 轉移工單並刪除人員
 * @param {number} targetStaffId - 接手人員 ID
 * @param {number} deleteStaffId - 要停用的人員 ID
 */
export const transferAndDelete = (targetStaffId, deleteStaffId) => {
  return http.post('/maintenance/staff/transfer-and-delete', {
    targetStaffId,
    deleteStaffId,
  })
}

// ============ 工單 (Ticket) API ============

/**
 * 取得全部工單
 */
export const getAllTickets = () => {
  return http.get('/maintenance/tickets')
}

/**
 * 取得待處理工單 (Active)
 */
export const getActiveTickets = () => {
  return http.get('/maintenance/tickets/active')
}

/**
 * 取得歷史工單 (History)
 */
export const getHistoryTickets = () => {
  return http.get('/maintenance/tickets/history')
}

/**
 * 取得單一工單
 * @param {number} id - 工單 ID
 */
export const getTicketById = (id) => {
  return http.get(`/maintenance/tickets/${id}`)
}

/**
 * ★ C) 新增：取得工單歷程記錄
 * @param {number} id - 工單 ID
 */
export const getTicketLogs = (id) => {
  return http.get(`/maintenance/tickets/${id}/logs`)
}

/**
 * 依據點 ID 查詢工單
 * @param {number} spotId - 據點 ID
 */
export const getTicketsBySpot = (spotId) => {
  return http.get(`/maintenance/tickets/spot/${spotId}`)
}

/**
 * 新增工單
 * @param {Object} ticket - 工單資料
 */
export const createTicket = (ticket) => {
  return http.post('/maintenance/tickets', ticket)
}

/**
 * 更新工單
 * @param {number} id - 工單 ID
 * @param {Object} ticket - 工單資料
 */
export const updateTicket = (id, ticket) => {
  return http.put(`/maintenance/tickets/${id}`, ticket)
}

// ============ 工單狀態流程 API ============

/**
 * 指派維修人員
 * @param {number} ticketId - 工單 ID
 * @param {number|null} staffId - 人員 ID (null 表示取消指派)
 */
export const assignStaff = (ticketId, staffId) => {
  return http.post(`/maintenance/tickets/${ticketId}/assign`, { staffId })
}

/**
 * 開始維修
 * @param {number} ticketId - 工單 ID
 */
export const startTicket = (ticketId) => {
  return http.post(`/maintenance/tickets/${ticketId}/start`)
}

/**
 * 結案
 * @param {number} ticketId - 工單 ID
 * @param {string} resultType - 結果類型 (FIXED, NOT_FIXED, NO_ISSUE, NOT_FIXABLE, OTHER)
 * @param {string} resolveNote - 備註
 */
export const resolveTicket = (ticketId, resultType, resolveNote) => {
  return http.post(`/maintenance/tickets/${ticketId}/resolve`, { resultType, resolveNote })
}

/**
 * 取消工單
 * @param {number} ticketId - 工單 ID
 * @param {string} reason - 取消原因
 */
export const cancelTicket = (ticketId, reason) => {
  return http.post(`/maintenance/tickets/${ticketId}/cancel`, { reason })
}

// ============ 據點 API (供工單表單使用) ============

/**
 * 取得所有據點 (供下拉選單使用)
 */
export const getAllSpots = () => {
  return http.get('/maintenance/spots')
}

// ============ 椅子 API (供工單表單使用) ============

/**
 * 取得所有椅子 (供下拉選單使用)
 */
export const getAllSeats = () => {
  return http.get('/maintenance/seats')
}

/**
 * 依據點 ID 取得椅子
 * @param {number} spotId - 據點 ID
 */
export const getSeatsBySpot = (spotId) => {
  return http.get(`/maintenance/seats/spot/${spotId}`)
}

// ============ 排程 (Schedule) API ============

/**
 * 取得所有排程
 */
export const getAllSchedules = () => {
  return http.get('/maintenance/schedule')
}

/**
 * 取得單一排程
 * @param {number} id - 排程 ID
 */
export const getScheduleById = (id) => {
  return http.get(`/maintenance/schedule/${id}`)
}

/**
 * 取得啟用中的排程
 */
export const getActiveSchedules = () => {
  return http.get('/maintenance/schedule/active')
}

/**
 * 建立排程
 * @param {Object} schedule - 排程資料
 */
export const createSchedule = (schedule) => {
  return http.post('/maintenance/schedule', schedule)
}

/**
 * 更新排程
 * @param {number} id - 排程 ID
 * @param {Object} schedule - 排程資料
 */
export const updateSchedule = (id, schedule) => {
  return http.put(`/maintenance/schedule/${id}`, schedule)
}

/**
 * 刪除排程
 * @param {number} id - 排程 ID
 */
export const deleteSchedule = (id) => {
  return http.delete(`/maintenance/schedule/${id}`)
}

/**
 * 切換排程啟用狀態
 * @param {number} id - 排程 ID
 */
export const toggleSchedule = (id) => {
  return http.patch(`/maintenance/schedule/${id}/toggle`)
}

// ============ 資產健康度統計 (Stats) API ============

/**
 * 取得資產健康度統計
 * @param {string} assetType - 資產類型：'SPOT' 或 'SEAT'
 */
export const getAssetStats = (assetType) => {
  return http.get('/maintenance/stats/assets', { params: { assetType } })
}

// ============ 工單附件 (Attachment) API ============

/**
 * 上傳工單附件（圖片）
 * @param {number} ticketId - 工單 ID
 * @param {File[]} files - 圖片檔案陣列
 * @param {string} note - 備註（可選）
 */
export const uploadTicketAttachments = (ticketId, files, note = null) => {
  const formData = new FormData()
  
  // 加入多個檔案
  files.forEach(file => {
    formData.append('files', file)
  })
  
  // 加入備註（可選）
  if (note) {
    formData.append('note', note)
  }
  
  return http.post(`/maintenance/tickets/${ticketId}/attachments`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 取得工單的所有附件
 * @param {number} ticketId - 工單 ID
 */
export const getTicketAttachments = (ticketId) => {
  return http.get(`/maintenance/tickets/${ticketId}/attachments`)
}

/**
 * 刪除附件（軟刪除）
 * @param {number} attachmentId - 附件 ID
 */
export const deleteTicketAttachment = (attachmentId) => {
  return http.delete(`/maintenance/attachments/${attachmentId}`)
}

// 匯出所有 API 作為預設物件 (方便一次 import)
export default {
  // Staff
  getAllStaff,
  getActiveStaff, // ★ 修復：加入 getActiveStaff
  getStaffById,
  getInactiveStaff,
  createStaff,
  updateStaff,
  deleteStaff,
  transferAndDelete,
  // Ticket
  getAllTickets,
  getActiveTickets,
  getHistoryTickets,
  getTicketById,
  getTicketLogs, // ★ (1A) 修復：加入 getTicketLogs
  getTicketsBySpot,
  createTicket,
  updateTicket,
  // Workflow
  assignStaff,
  startTicket,
  resolveTicket,
  cancelTicket,
  // Spot
  getAllSpots,
  // Seat
  getAllSeats,
  getSeatsBySpot,
  // Schedule
  getAllSchedules,
  getScheduleById,
  getActiveSchedules,
  createSchedule,
  updateSchedule,
  deleteSchedule,
  toggleSchedule,
  // Stats
  getAssetStats,
  // Attachments (圖片附件)
  uploadTicketAttachments,
  getTicketAttachments,
  deleteTicketAttachment,
  // Task 2: 歷史工單查詢支援
  getTicketsByStaff: (staffId, statuses = null) => {
    const params = statuses ? { statuses: statuses.join(',') } : {}
    return api.get(`/api/maintenance/staff/${staffId}/tickets`, { params })
  },
}
