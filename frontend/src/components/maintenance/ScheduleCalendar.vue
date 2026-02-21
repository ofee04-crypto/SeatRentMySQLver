<script setup>
import { ref, computed } from 'vue'
import { useScheduleConfig } from '@/composables/maintenance/useScheduleConfig'

const props = defineProps({
  schedules: { type: Array, default: () => [] },
})

const emit = defineEmits(['select-date', 'click-schedule'])
const { scheduleTypeConfig, formatTime } = useScheduleConfig()

const calendarValue = ref(new Date())

// ====== 中文日曆 Header（取代 el-calendar 預設英文） ======
const weekDaysZh = ['日', '一', '二', '三', '四', '五', '六']
const monthNameZh = (date) => `${date.getFullYear()} 年 ${date.getMonth() + 1} 月`
const currentMonthText = computed(() => monthNameZh(calendarValue.value))

const goPrevMonth = () => {
  const d = new Date(calendarValue.value)
  d.setMonth(d.getMonth() - 1)
  calendarValue.value = d
}
const goNextMonth = () => {
  const d = new Date(calendarValue.value)
  d.setMonth(d.getMonth() + 1)
  calendarValue.value = d
}
const goToday = () => {
  calendarValue.value = new Date()
}

// 格式化日期為 key
const formatDateKey = (date) => {
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

// 取得某日期的排程
const getSchedulesForDate = (date) => {
  const dateStr = formatDateKey(date)
  return props.schedules.filter((s) => {
    if (!s.isActive) return false
    const nextDate = formatDateKey(new Date(s.nextExecuteAt))
    return nextDate === dateStr
  })
}

const handleDateClick = (date) => emit('select-date', date)
const handleScheduleClick = (schedule) => emit('click-schedule', schedule)
</script>

<template>
  <div class="schedule-calendar">
    <!-- ✅ 自訂中文 Header -->
    <div class="calendar-header-zh">
      <div class="title">{{ currentMonthText }}</div>
      <div class="actions">
        <el-button size="small" @click="goPrevMonth">上個月</el-button>
        <el-button size="small" @click="goToday">今天</el-button>
        <el-button size="small" @click="goNextMonth">下個月</el-button>
      </div>
    </div>

    <!-- ✅ 中文星期列 -->
    <div class="week-header-zh">
      <div v-for="d in weekDaysZh" :key="d" class="week-day">{{ d }}</div>
    </div>

    <el-calendar v-model="calendarValue" class="calendar-body">
      <template #date-cell="{ data }">
        <div
          class="calendar-cell"
          :class="{ 'has-schedule': getSchedulesForDate(data.date).length > 0 }"
          @click="handleDateClick(data.date)"
        >
          <div class="date-number">
            {{ data.date.getDate() }}
          </div>

          <!-- 排程標記 -->
          <div class="schedule-dots" v-if="getSchedulesForDate(data.date).length > 0">
            <template
              v-for="schedule in getSchedulesForDate(data.date).slice(0, 3)"
              :key="schedule.scheduleId"
            >
              <el-tooltip
                :content="`${schedule.title} - ${formatTime(schedule.executeTime)}`"
                placement="top"
              >
                <div
                  class="schedule-dot"
                  :style="{ backgroundColor: scheduleTypeConfig[schedule.scheduleType]?.color }"
                  @click.stop="handleScheduleClick(schedule)"
                ></div>
              </el-tooltip>
            </template>

            <span v-if="getSchedulesForDate(data.date).length > 3" class="more-count">
              +{{ getSchedulesForDate(data.date).length - 3 }}
            </span>
          </div>
        </div>
      </template>
    </el-calendar>

    <!-- 圖例 -->
    <div class="calendar-legend">
      <span v-for="(config, key) in scheduleTypeConfig" :key="key" class="legend-item">
        <span class="legend-dot" :style="{ backgroundColor: config.color }"></span>
        {{ config.text }}
      </span>
    </div>
  </div>
</template>

<style scoped>
.schedule-calendar {
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
}

/* ✅ 中文 header */
.calendar-header-zh {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
}
.calendar-header-zh .title {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
}
.calendar-header-zh .actions {
  display: flex;
  gap: 8px;
}

/* ✅ 中文星期列 */
.week-header-zh {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  padding: 8px 10px;
  background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}
.week-day {
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: #606266;
}

/* cell */
.calendar-cell {
  height: 100%;
  padding: 4px;
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 6px;
}
.calendar-cell:hover {
  background: rgba(64, 158, 255, 0.1);
}
.calendar-cell.has-schedule {
  background: rgba(64, 158, 255, 0.05);
}

.date-number {
  font-size: 14px;
  font-weight: 600;
  text-align: center;
  margin-bottom: 4px;
}

.schedule-dots {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 3px;
  flex-wrap: wrap;
}

.schedule-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  cursor: pointer;
  transition: transform 0.2s;
}
.schedule-dot:hover {
  transform: scale(1.3);
}

.more-count {
  font-size: 10px;
  color: #909399;
}

.calendar-legend {
  display: flex;
  justify-content: center;
  gap: 20px;
  padding: 12px;
  background: #f5f7fa;
  border-top: 1px solid #ebeef5;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #606266;
}
.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

/* ✅ 關鍵：把 el-calendar 原本英文 header + 英文星期列隱藏 */
:deep(.el-calendar__header) {
  display: none !important;
}
:deep(.el-calendar-table thead) {
  display: none !important;
}

/* day cell height */
:deep(.el-calendar-table .el-calendar-day) {
  height: 70px;
  padding: 0;
}
</style>
