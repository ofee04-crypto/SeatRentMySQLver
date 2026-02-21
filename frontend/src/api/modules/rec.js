/**
 * 租借記錄模組 API
 * 封裝所有租借記錄相關的 API 呼叫
 */
import http from '../http'

/**
 * 依時間區間取得每月訂單統計數據
 * @param {string} startDate - 開始日期 (格式: YYYY-MM-DD)
 * @param {string} endDate - 結束日期 (格式: YYYY-MM-DD)
 * @returns {Promise}
 */
export const getMonthlyOrderStats = (startDate, endDate) => {
  return http.get('/rent-details/stats/monthly-orders', {
    params: {
      startDate,
      endDate
    }
  })
}

/**
 * 依時間區間取得訂單狀態統計
 * @param {string} startDate - 開始日期 (格式: YYYY-MM-DD)
 * @param {string} endDate - 結束日期 (格式: YYYY-MM-DD)
 * @returns {Promise}
 */
export const getOrderStatusStats = (startDate, endDate) => {
  return http.get('/rent-details/stats/order-status', {
    params: {
      startDate,
      endDate
    }
  })
}

/**
 * 依時間區間取得訂單租借時長
 * @param {string} startDate - 開始日期 (格式: YYYY-MM-DD)
 * @param {string} endDate - 結束日期 (格式: YYYY-MM-DD)
 * @returns {Promise}
 */
export const getRentalDurations = (startDate, endDate) => {
  return http.get('/rent-details/stats/rental-duration', {
    params: {
      startDate,
      endDate
    }
  })
}

/**
 * 依時間區間取得每小時訂單統計
 * @param {string} startDate - 開始日期 (格式: YYYY-MM-DD)
 * @param {string} endDate - 結束日期 (格式: YYYY-MM-DD)
 * @returns {Promise}
 */
export const getHourlyOrderStats = (startDate, endDate) => {
  return http.get('/rent-details/stats/hourly-orders', {
    params: {
      startDate,
      endDate
    }
  })
}

/**
 * 依時間區間取得每日訂單統計
 * @param {string} startDate - 開始日期 (格式: YYYY-MM-DD)
 * @param {string} endDate - 結束日期 (格式: YYYY-MM-DD)
 * @returns {Promise}
 */
export const getDailyOrderStats = (startDate, endDate) => {
  return http.get('/rent-details/stats/daily-orders', {
    params: {
      startDate,
      endDate
    }
  })
}

/**
 * 依時間區間取得租借時長統計 (30分鐘間隔)
 * @param {string} startDate - 開始日期 (格式: YYYY-MM-DD)
 * @param {string} endDate - 結束日期 (格式: YYYY-MM-DD)
 * @returns {Promise}
 */
export const getRentalDurationStats = (startDate, endDate) => {
  return http.get('/rent-details/stats/rental-duration-intervals', {
    params: {
      startDate,
      endDate
    }
  })
}


// 匯出所有 API 作為預設物件 (方便一次 import)
export default {
  getMonthlyOrderStats,
  getOrderStatusStats,
  getRentalDurations,
  getHourlyOrderStats,
  getDailyOrderStats,
  getRentalDurationStats
}
