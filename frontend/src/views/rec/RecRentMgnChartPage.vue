<script setup>
// --- 0. Import ---
import { ref, onMounted, shallowRef } from 'vue'
import * as echarts from 'echarts'
import {
  getDailyOrderStats,
  getHourlyOrderStats,
  getOrderStatusStats,
  getRentalDurationStats,
} from '@/api/modules/rec'
import { ElMessage } from 'element-plus'

// --- 1. 狀態定義 ---

// 圖表實例，使用 shallowRef 避免不必要的深度響應
const dailyChartInstance = shallowRef(null)
const pieChartInstance = shallowRef(null)
const durationChartInstance = shallowRef(null)
const hourlyChartInstance = shallowRef(null)

// 圖表容器的 DOM 引用
const dailyChartContainer = ref(null)
const pieChartContainer = ref(null)
const durationChartContainer = ref(null)
const hourlyChartContainer = ref(null)

// 預設日期範圍 (最近六個月)
const defaultEndDate = new Date()
const defaultStartDate = new Date()
defaultStartDate.setMonth(defaultStartDate.getMonth() - 6)

// 用於日期選擇器的響應式變數
const startDate = ref(defaultStartDate)
const endDate = ref(defaultEndDate)

// --- 2. 核心邏輯 ---

/**
 * 格式化日期為 YYYY-MM-DD
 */
const formatDate = (date) => {
  if (!date) return ''
  const year = date.getFullYear()
  const month = (date.getMonth() + 1).toString().padStart(2, '0')
  const day = date.getDate().toString().padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * 繪製每日訂單與累計趨勢圖
 */
const drawDailyOrdersChart = async (start, end) => {
  const response = await getDailyOrderStats(start, end)
  const chartData = response.data

  // 準備每日數據
  const dailyLabels = chartData.map((item) => item.date)
  const dailyValues = chartData.map((item) => item.count)

  // 計算累計數據
  const cumulativeValues = dailyValues.reduce((acc, val) => {
    acc.push((acc.length > 0 ? acc[acc.length - 1] : 0) + val)
    return acc
  }, [])

  const options = {
    title: { text: '每日訂單數量與累計趨勢', left: 'center', top: '1', padding: '' },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'line' },
    },
    grid: { left: '1%', right: '1%', bottom: '9%', containLabel: true },
    legend: { data: ['日訂單', '累計訂單'], top: 'bottom' },
    xAxis: [{ type: 'category', data: dailyLabels, axisTick: { alignWithLabel: true } }],
    yAxis: [
      {
        type: 'value',
        name: '每日',
        axisLabel: { color: '#E0583A', formatter: '{value} 筆' },
        max: '50',
        nameTextStyle: { align: 'right', color: '#E0583A' },
      },
      {
        type: 'value',
        name: '累計',
        axisLabel: { color: '#188df0', formatter: '{value} 筆' },
        nameTextStyle: { align: 'left', color: '#188df0' },
      },
    ],
    series: [
      {
        name: '日訂單',
        type: 'line',
        color: '#E0583A',
        yAxisIndex: 0,
        data: dailyValues,
        smooth: false,
        // 直接在圖上顯示標籤
        label: {
          show: true,
          position: 'top',
        },
      },
      {
        name: '累計訂單',
        type: 'bar',
        yAxisIndex: 1,
        data: cumulativeValues,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 1, color: '#83bff6' },
            { offset: 0, color: '#188df0' },
          ]),
        },

        // 直接在圖上顯示標籤
        label: {
          show: true,
          position: 'top',
        },
      },
    ],
    dataZoom: [{ show: 'true', type: 'inside', start: 0, end: 100 }],
  }

  if (!dailyChartInstance.value) {
    dailyChartInstance.value = echarts.init(dailyChartContainer.value)
  }
  dailyChartInstance.value.setOption(options)
}

/**
 * 繪製每小時訂單分佈圖
 */
const drawHourlyOrdersChart = async (start, end) => {
  const response = await getHourlyOrderStats(start, end)
  const chartData = response.data

  // 初始化24小時數據
  const hourlyData = Array(24).fill(0)
  chartData.forEach((item) => {
    hourlyData[item.hour] = item.count
  })
  const hourlyLabels = Array.from({ length: 24 }, (_, i) => `${i}:00`)

  const options = {
    title: { text: '訂單時段分佈 (0-23點)', left: 'center', top: '0' },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: [{ type: 'category', data: hourlyLabels, name: '小時' }],
    yAxis: [{ type: 'value', name: '訂單數量' }],
    series: [
      {
        name: '訂單數',
        type: 'bar',
        data: hourlyData,
        itemStyle: {
          color: '#3ba272',
        },
        // 直接在圖上顯示標籤
        label: {
          show: true,
          position: 'top',
        },
      },
    ],
    dataZoom: [{ type: 'inside', start: 0, end: 100 }],
  }

  if (!hourlyChartInstance.value) {
    hourlyChartInstance.value = echarts.init(hourlyChartContainer.value)
  }
  hourlyChartInstance.value.setOption(options)
}

/**
 * 繪製訂單狀態圓餅圖
 */
const drawOrderStatusPieChart = async (start, end) => {
  const response = await getOrderStatusStats(start, end)
  const chartData = response.data.map((item) => ({
    name: item.status,
    value: item.count,
  }))

  const options = {
    title: { text: '訂單狀態比例', left: 'center', top: '0' },
    tooltip: { trigger: 'item', formatter: '{a} <br/>{b} : {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left', data: chartData.map((item) => item.name) },
    series: [
      {
        name: '訂單狀態',
        type: 'pie',
        radius: '75%',
        center: ['50%', '55%'],
        data: chartData,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)',
          },
        },
        // 直接在圖上顯示標籤
        label: {
          show: true,
          formatter: '{b} {c}筆: {d}% ', // 格式: 名稱: 百分比%
          position: 'outside',
        },
        labelLine: {
          show: true,
        },
      },
    ],
  }

  if (!pieChartInstance.value) {
    pieChartInstance.value = echarts.init(pieChartContainer.value)
  }
  pieChartInstance.value.setOption(options)
}

/**
 * 繪製租借時長分佈長條圖
 */
const drawRentalDurationChart = async (start, end) => {
  const response = await getRentalDurationStats(start, end)
  const chartData = response.data

  // 處理數據
  const durationLabels = chartData.map((item) => {
    const lower = item.intervalGroup * 0.5
    const upper = (item.intervalGroup + 1) * 0.5
    return `${lower}~${upper} hr`
  })
  const durationValues = chartData.map((item) => item.count)

  const options = {
    title: { text: '訂單租借時長分佈 (半小時區間)', left: 'center', top: '0' },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'line' },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
      splitLine: { interval: 'auto' },
    },
    xAxis: { type: 'category', data: durationLabels, name: '租借時長' },
    yAxis: { type: 'value', name: '訂單數量' },
    series: [
      {
        name: '訂單數',
        data: durationValues,
        type: 'bar',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 1, color: '#83bff6' },
            { offset: 0, color: '#188df0' },
          ]),
        },
        // 直接在圖上顯示標籤
        label: {
          show: true,
          position: 'top',
        },
      },
    ],
    dataZoom: [{ type: 'inside', start: 0, end: 100 }],
  }

  if (!durationChartInstance.value) {
    durationChartInstance.value = echarts.init(durationChartContainer.value)
  }
  durationChartInstance.value.setOption(options)
}

/**
 * 獲取後端數據並繪製所有圖表
 */
const fetchAllCharts = async () => {
  if (!startDate.value || !endDate.value) {
    ElMessage.warning('請選擇開始與結束日期')
    return
  }

  const loading = ElMessage({ message: '正在載入圖表數據...', type: 'info', duration: 0 })
  const formattedStart = formatDate(startDate.value)
  const formattedEnd = formatDate(endDate.value)

  try {
    // 並行獲取所有圖表數據
    await Promise.all([
      drawOrderStatusPieChart(formattedStart, formattedEnd),
      drawRentalDurationChart(formattedStart, formattedEnd),
      drawDailyOrdersChart(formattedStart, formattedEnd),
      drawHourlyOrdersChart(formattedStart, formattedEnd),
    ])
  } catch (error) {
    console.error('獲取圖表數據失敗:', error)
    ElMessage.error('載入圖表數據失敗，請稍後再試')
  } finally {
    loading.close()
  }
}

/**
 * 快速設定日期範圍並自動查詢
 * @param {number} months - 往前推算的月份數
 */
const setQuickDateRange = (months) => {
  const end = new Date()
  const start = new Date()
  start.setMonth(start.getMonth() - months)
  startDate.value = start
  endDate.value = end
  fetchAllCharts()
}

// --- 3. 生命週期 & 事件監聽 ---

// 組件掛載後，自動載入預設範圍的圖表
onMounted(() => {
  fetchAllCharts()
})

// 監聽窗口大小變化，重置圖表
window.addEventListener('resize', () => {
  ;[dailyChartInstance, hourlyChartInstance, pieChartInstance, durationChartInstance].forEach(
    (instance) => {
      if (instance.value) {
        instance.value.resize()
      }
    },
  )
})
</script>

<template>
  <div class="chart-page">
    <h3 style="text-align: center">訂單統計圖表</h3>
    <!-- 頂部控制項 -->
    <el-card class="box-card">
      <div class="controls-container">
        <el-form :inline="true" :model="{ startDate, endDate }">
          <el-form-item>
            <el-button-group>
              <el-button @click="setQuickDateRange(12)">近1年</el-button>
              <el-button @click="setQuickDateRange(36)">近3年</el-button>
              <el-button @click="setQuickDateRange(60)">近5年</el-button>
            </el-button-group>
            
          </el-form-item>
          <el-form-item label="選擇開始日期">
            <el-date-picker
              v-model="startDate"
              type="date"
              placeholder="選擇開始日期"
              style="width: 150px"
            />
          </el-form-item>
          <el-form-item label="選擇結束日期">
            <el-date-picker
              v-model="endDate"
              type="date"
              placeholder="選擇結束日期"
              style="width: 150px"
            /> </el-form-item
          ><el-form-item>
            <el-button type="primary" @click="fetchAllCharts" >查詢</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <!-- 圖表容器 -->
    <el-row :gutter="20">
      <!-- 每日訂單與累計趨勢圖 -->
      <el-col :span="12">
        <el-card class="box-card chart-container-card">
          <div ref="dailyChartContainer" style="width: 100%; height: 300px"></div>
        </el-card>
      </el-col>
      <!-- 每小時訂單分佈圖 -->
      <el-col :span="12">
        <el-card class="box-card chart-container-card">
          <div ref="hourlyChartContainer" style="width: 100%; height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <!-- 訂單狀態比例圖 -->
      <el-col :span="12">
        <el-card class="box-card chart-container-card">
          <div ref="pieChartContainer" style="width: 100%; height: 300px"></div>
        </el-card>
      </el-col>
      <!-- 租借時長分佈圖 -->
      <el-col :span="12">
        <el-card class="box-card chart-container-card">
          <div ref="durationChartContainer" style="width: 100%; height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.chart-page {
  padding: 10px;
}
.box-card {
  margin-bottom: 10px;
}
.controls-container {
  /* 使用 Flexbox 排版 */
  display: flex;
  /* 水平置中表單 */
  justify-content: center;
  /* 允許內容換行 */
  flex-wrap: wrap;padding-top: 10px;
}
</style>
