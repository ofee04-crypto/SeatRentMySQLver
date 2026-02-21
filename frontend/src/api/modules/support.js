import http from '../http'

/**
 * Support 模組 API 封裝
 * 
 * 【重構說明】2026-02-01
 * - 移除 WebSDK 相關 API
 * - 改用 Coze OpenAPI Proxy
 */

// ==================== BEGIN: OpenAPI 整合 ====================

/**
 * 取得 Coze 配置資訊（OpenAPI 模式）
 * GET /support/coze/bootstrap
 *
 * @returns {Promise} { botId, mode, serverTime, available }
 */
export const getCozeBootstrap = () => {
  return http.get('/support/coze/bootstrap')
}

/**
 * 檢查 Coze OpenAPI 狀態
 * GET /support/coze/status
 * 
 * @returns {Promise<{status: string, available: boolean, message: string}>}
 */
export const checkCozeStatus = () => {
  return http.get('/support/coze/status')
}

/**
 * 發送聊天訊息（透過後端 Proxy）
 * POST /support/coze/chat
 * 
 * @param {Object} params
 * @param {string} params.message - 使用者訊息
 * @param {string} [params.userId] - 使用者 ID（可選）
 * @param {string} [params.conversationId] - 對話 ID（可選，用於多輪對話）
 * @returns {Promise<{success: boolean, replyText?: string, conversationId?: string, error?: string}>}
 */
export const sendChatMessage = (params) => {
  return http.post('/support/coze/chat', params)
}

// ==================== END: OpenAPI 整合 ====================

// 匯出所有 API
export default {
  getCozeBootstrap,
  checkCozeStatus,
  sendChatMessage
}
