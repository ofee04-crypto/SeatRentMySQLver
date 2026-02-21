/**
 * 排程配置 Composable
 * 集中管理排程相關的配置與格式化方法
 */
import { computed } from 'vue'

export function useScheduleConfig() {
  // ====== 排程類型配置 ======
  const scheduleTypeConfig = {
    DAILY: {
      text: '每日',
      icon: 'fas fa-sun',
      color: '#67c23a',
      tagType: 'success',
      bgColor: 'rgba(103, 194, 58, 0.1)',
    },
    WEEKLY: {
      text: '每週',
      icon: 'fas fa-calendar-week',
      color: '#409eff',
      tagType: '',
      bgColor: 'rgba(64, 158, 255, 0.1)',
    },
    MONTHLY: {
      text: '每月',
      icon: 'fas fa-calendar-alt',
      color: '#e6a23c',
      tagType: 'warning',
      bgColor: 'rgba(230, 162, 60, 0.1)',
    },
  }

  // ====== 星期配置 ======
  const dayOfWeekConfig = {
    1: { text: '週一', short: '一', color: '#409eff' },
    2: { text: '週二', short: '二', color: '#67c23a' },
    3: { text: '週三', short: '三', color: '#e6a23c' },
    4: { text: '週四', short: '四', color: '#f56c6c' },
    5: { text: '週五', short: '五', color: '#909399' },
    6: { text: '週六', short: '六', color: '#b88230' },
    7: { text: '週日', short: '日', color: '#f56c6c' },
  }

  // ====== 優先級配置 ======
  const priorityConfig = {
    LOW: { text: '低', color: '#909399', tagType: 'info' },
    NORMAL: { text: '一般', color: '#409eff', tagType: '' },
    HIGH: { text: '高', color: '#e6a23c', tagType: 'warning' },
    URGENT: { text: '緊急', color: '#f56c6c', tagType: 'danger' },
  }

  // ====== 格式化方法 ======
  const formatDateTime = (dateStr) => {
    if (!dateStr) return '-'
    const d = new Date(dateStr)
    const pad = (n) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  }

  const formatDate = (dateStr) => {
    if (!dateStr) return '-'
    const d = new Date(dateStr)
    const pad = (n) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  }

  const formatTime = (timeStr) => {
    if (!timeStr) return '-'
    return timeStr.substring(0, 5)
  }

  const formatScheduleDetail = (schedule) => {
    const time = formatTime(schedule.executeTime)
    switch (schedule.scheduleType) {
      case 'DAILY':
        return `每天 ${time}`
      case 'WEEKLY':
        return `每${dayOfWeekConfig[schedule.dayOfWeek]?.text || '?'} ${time}`
      case 'MONTHLY':
        return `每月 ${schedule.dayOfMonth} 號 ${time}`
      default:
        return '-'
    }
  }

  // ====== 計算下次執行的相對時間 ======
  const getRelativeTime = (dateStr) => {
    if (!dateStr) return { text: '-', isOverdue: false }
    const now = new Date()
    const target = new Date(dateStr)
    const diff = target - now

    if (diff < 0) {
      return { text: '已過期', isOverdue: true }
    }

    const minutes = Math.floor(diff / 60000)
    const hours = Math.floor(minutes / 60)
    const days = Math.floor(hours / 24)

    if (days > 0) {
      return { text: `${days} 天後`, isOverdue: false }
    } else if (hours > 0) {
      return { text: `${hours} 小時後`, isOverdue: false }
    } else {
      return { text: `${minutes} 分鐘後`, isOverdue: false, isSoon: minutes < 30 }
    }
  }

  // ====== 取得排程的標籤樣式 ======
  const getScheduleTypeTag = (type) => scheduleTypeConfig[type] || scheduleTypeConfig.DAILY
  const getPriorityTag = (priority) => priorityConfig[priority] || priorityConfig.NORMAL

  return {
    scheduleTypeConfig,
    dayOfWeekConfig,
    priorityConfig,
    formatDateTime,
    formatDate,
    formatTime,
    formatScheduleDetail,
    getRelativeTime,
    getScheduleTypeTag,
    getPriorityTag,
  }
}
