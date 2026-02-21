<template>
  <el-row :gutter="20" class="charts-container" v-show="hasData">
    <!-- 狀態分佈圖 -->
    <el-col :md="8" :sm="24">
      <el-card shadow="hover" class="chart-card">
        <template #header>
          <div class="chart-header">
            <span class="chart-icon status-icon">
              <i class="fas fa-chart-pie"></i>
            </span>
            <b>狀態分佈</b>
          </div>
        </template>
        <div ref="statusChartRef" class="chart-container"></div>
      </el-card>
    </el-col>

    <!-- 優先級佔比圖 -->
    <el-col :md="8" :sm="24">
      <el-card shadow="hover" class="chart-card">
        <template #header>
          <div class="chart-header">
            <span class="chart-icon priority-icon">
              <i class="fas fa-flag"></i>
            </span>
            <b>優先級佔比</b>
          </div>
        </template>
        <div ref="priorityChartRef" class="chart-container"></div>
      </el-card>
    </el-col>

    <!-- 故障類型統計圖 -->
    <el-col :md="8" :sm="24">
      <el-card shadow="hover" class="chart-card">
        <template #header>
          <div class="chart-header">
            <span class="chart-icon type-icon">
              <i class="fas fa-tools"></i>
            </span>
            <b>前五大故障類型</b>
          </div>
        </template>
        <div ref="typeChartRef" class="chart-container"></div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import ApexCharts from 'apexcharts'
import { useTicketConfig } from '@/composables/maintenance/useTicketConfig'

const props = defineProps({
  /** 工單資料陣列 */
  tickets: {
    type: Array,
    default: () => [],
  },
})

const { statusConfig, priorityConfig } = useTicketConfig()

// --- 圖表 DOM 參照 ---
const statusChartRef = ref(null)
const priorityChartRef = ref(null)
const typeChartRef = ref(null)

// --- 圖表實例 ---
let statusChart = null
let priorityChart = null
let typeChart = null

// --- 是否有資料 ---
const hasData = computed(() => props.tickets.length > 0)

// --- 計算圖表數據 ---
const chartData = computed(() => {
  const data = props.tickets

  // 狀態統計
  const statusCount = {}
  data.forEach((t) => {
    statusCount[t.issueStatus] = (statusCount[t.issueStatus] || 0) + 1
  })

  // 優先級統計
  const priorityCount = { LOW: 0, NORMAL: 0, HIGH: 0, URGENT: 0 }
  data.forEach((t) => {
    if (priorityCount[t.issuePriority] !== undefined) {
      priorityCount[t.issuePriority]++
    }
  })

  // 故障類型統計 (前5名)
  const typeCount = {}
  data.forEach((t) => {
    typeCount[t.issueType] = (typeCount[t.issueType] || 0) + 1
  })
  const sortedTypes = Object.entries(typeCount)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)

  return { statusCount, priorityCount, sortedTypes }
})

// --- 圖表配置工廠 ---
const createStatusChartOptions = (statusCount) => ({
  series: Object.values(statusCount),
  labels: Object.keys(statusCount).map((k) => statusConfig[k]?.text || k),
  chart: {
    type: 'donut',
    height: 280,
    toolbar: { show: false },
    animations: {
      enabled: true,
      easing: 'easeinout',
      speed: 800,
      animateGradually: { enabled: true, delay: 150 },
      dynamicAnimation: { enabled: true, speed: 350 },
    },
  },
  colors: ['#17a2b8', '#007bff', '#ffc107', '#28a745', '#6c757d'],
  dataLabels: { enabled: false },
  legend: { position: 'bottom', fontSize: '13px' },
  plotOptions: {
    pie: {
      donut: {
        size: '65%',
        labels: {
          show: true,
          total: {
            show: true,
            label: '總計',
            fontSize: '14px',
            fontWeight: 600,
            color: '#909399',
          },
        },
      },
    },
  },
  stroke: { width: 2, colors: ['#fff'] },
  tooltip: {
    y: { formatter: (val) => `${val} 件` },
  },
})

const createPriorityChartOptions = (priorityCount) => ({
  series: [priorityCount.LOW, priorityCount.NORMAL, priorityCount.HIGH, priorityCount.URGENT],
  labels: [
    `${priorityConfig.LOW.icon} 低`,
    `${priorityConfig.NORMAL.icon} 普通`,
    `${priorityConfig.HIGH.icon} 高`,
    `${priorityConfig.URGENT.icon} 緊急`,
  ],
  chart: {
    type: 'pie',
    height: 280,
    toolbar: { show: false },
    animations: {
      enabled: true,
      easing: 'easeinout',
      speed: 800,
    },
  },
  colors: ['#909399', '#67c23a', '#e6a23c', '#f56c6c'],
  legend: { position: 'bottom', fontSize: '13px' },
  stroke: { width: 2, colors: ['#fff'] },
  tooltip: {
    y: { formatter: (val) => `${val} 件` },
  },
})

const createTypeChartOptions = (sortedTypes) => ({
  series: [{ name: '件數', data: sortedTypes.map((i) => i[1]) }],
  chart: {
    type: 'bar',
    height: 280,
    toolbar: { show: false },
    animations: {
      enabled: true,
      easing: 'easeinout',
      speed: 800,
    },
  },
  xaxis: {
    categories: sortedTypes.map((i) => i[0]),
    labels: { style: { fontSize: '12px' } },
  },
  plotOptions: {
    bar: {
      borderRadius: 8,
      horizontal: true,
      distributed: true,
      dataLabels: { position: 'center' },
    },
  },
  colors: ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399'],
  dataLabels: {
    enabled: true,
    formatter: (val) => `${val} 件`,
    style: { fontSize: '12px', colors: ['#fff'] },
  },
  tooltip: {
    y: { formatter: (val) => `${val} 件` },
  },
})

// --- 渲染圖表 ---
const renderCharts = () => {
  if (!hasData.value) return

  if (!statusChartRef.value || !priorityChartRef.value || !typeChartRef.value) return

  const { statusCount, priorityCount, sortedTypes } = chartData.value

  const statusOptions = createStatusChartOptions(statusCount)
  const priorityOptions = createPriorityChartOptions(priorityCount)
  const typeOptions = createTypeChartOptions(sortedTypes)

  // 更新或建立圖表
  if (statusChart) {
    statusChart.updateOptions(statusOptions)
    priorityChart.updateOptions(priorityOptions)
    typeChart.updateOptions(typeOptions)
  } else {
    if (statusChartRef.value) {
      statusChart = new ApexCharts(statusChartRef.value, statusOptions)
      statusChart.render()
    }
    if (priorityChartRef.value) {
      priorityChart = new ApexCharts(priorityChartRef.value, priorityOptions)
      priorityChart.render()
    }
    if (typeChartRef.value) {
      typeChart = new ApexCharts(typeChartRef.value, typeOptions)
      typeChart.render()
    }
  }
}

// --- 銷毀圖表 ---
const destroyCharts = () => {
  if (statusChart) {
    statusChart.destroy()
    statusChart = null
  }
  if (priorityChart) {
    priorityChart.destroy()
    priorityChart = null
  }
  if (typeChart) {
    typeChart.destroy()
    typeChart = null
  }
}

// --- 監聽資料變化重新渲染 ---
watch(
  () => props.tickets,
  () => {
    setTimeout(() => {
      nextTick(() => renderCharts())
    }, 100)
  },
  { deep: true },
)

// --- 生命週期 ---
onMounted(() => {
  nextTick(() => renderCharts())
})

onBeforeUnmount(() => {
  destroyCharts()
})

// 暴露方法供外部調用
defineExpose({
  renderCharts,
  destroyCharts,
})
</script>

<style scoped>
.charts-container {
  margin-bottom: 20px;
}

.chart-card {
  border-radius: 16px;
  overflow: hidden;
  border: none;
  min-height: 360px;
  margin-bottom: 16px;
  transition: all 0.3s ease;
  background: linear-gradient(145deg, #ffffff 0%, #fafbfc 100%);
}

.chart-card:hover {
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.chart-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chart-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.status-icon {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
}

.priority-icon {
  background: linear-gradient(135deg, #e6a23c 0%, #f3d19e 100%);
}

.type-icon {
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
}

.chart-container {
  min-height: 280px;
}

/* 響應式調整 */
@media (max-width: 768px) {
  .chart-card {
    min-height: 320px;
  }

  .chart-container {
    min-height: 240px;
  }
}
</style>
