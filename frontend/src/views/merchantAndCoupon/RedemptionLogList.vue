<template>
  <div class="log-list-container">
    <section class="content-header mb-4">
      <div class="container-fluid">
        <div class="page-title-box">
          <div class="title-icon"><i class="fas fa-chart-line"></i></div>
          <div class="title-content">
            <h1>兌換數據分析中心</h1>
            <p class="subtitle">視覺化監控優惠券熱度與兌換趨勢</p>
          </div>
        </div>
      </div>
    </section>

    <section class="filter-section mb-4">
      <div class="container-fluid">
        <el-card shadow="never" class="filter-card">
          <div class="d-flex align-items-center flex-wrap">
            <span class="filter-label me-3">
              <i class="fas fa-calendar-alt me-2 text-primary"></i>分析區間：
            </span>
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              unlink-panels
              range-separator="至"
              start-placeholder="開始日期"
              end-placeholder="結束日期"
              value-format="YYYY-MM-DD"
              :shortcuts="dateShortcuts"
              class="me-3"
            />
            <el-button @click="resetFilter" plain size="small">重置</el-button>
            <div class="ms-auto">
              <el-button type="success" @click="exportToExcel">
                <i class="fas fa-file-excel me-1"></i> 匯出報表
              </el-button>
            </div>
          </div>
        </el-card>
      </div>
    </section>

    <section class="stat-section mb-4">
      <div class="container-fluid">
        <el-row :gutter="20">
          <el-col :xs="12" :md="6">
            <div class="stat-card total-card">
              <div class="stat-icon"><i class="fas fa-exchange-alt"></i></div>
              <div class="stat-info">
                <h3>{{ filteredLogs.length }}</h3>
                <span>區間兌換總次數</span>
              </div>
            </div>
          </el-col>
          <el-col :xs="12" :md="6">
            <div class="stat-card points-card">
              <div class="stat-icon"><i class="fas fa-coins"></i></div>
              <div class="stat-info">
                <h3>{{ totalPointsSpent }}</h3>
                <span>消耗點數總計</span>
              </div>
            </div>
          </el-col>
          <el-col :xs="12" :md="6">
            <div class="stat-card avg-card">
              <div class="stat-icon"><i class="fas fa-calculator"></i></div>
              <div class="stat-info">
                <h3>{{ avgPoints }}</h3>
                <span>平均每券點數</span>
              </div>
            </div>
          </el-col>
          <el-col :xs="12" :md="6">
            <div class="stat-card merchant-card">
              <div class="stat-icon"><i class="fas fa-ticket-alt"></i></div>
              <div class="stat-info">
                <h3>{{ uniqueCoupons }}</h3>
                <span>涉及優惠券種</span>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </section>

    <section class="chart-section mb-4">
      <div class="container-fluid">
        <el-row :gutter="20">
          <el-col :xs="24" :lg="16">
            <el-card shadow="hover" class="chart-card mb-4">
              <template #header>
                <div class="chart-header">
                  <i class="fas fa-chart-area me-2 text-primary"></i> 每日兌換趨勢
                </div>
              </template>
              <div ref="lineChartRef" style="height: 350px;"></div>
            </el-card>
          </el-col>
          <el-col :xs="24" :lg="8">
            <el-card shadow="hover" class="chart-card mb-4">
              <template #header>
                <div class="chart-header">
                  <i class="fas fa-chart-pie me-2 text-success"></i> 兌換佔比分析
                </div>
              </template>
              <div ref="pieChartRef" style="height: 350px;"></div>
            </el-card>
          </el-col>
        </el-row>

        <el-card shadow="hover" class="chart-card mb-4">
          <template #header>
            <div class="chart-header">
              <i class="fas fa-crown me-2 text-warning"></i> 熱門兌換排行榜 (Top 10)
            </div>
          </template>
          <div ref="barChartRef" style="height: 400px;"></div>
        </el-card>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'
import * as XLSX from 'xlsx'

// --- 狀態定義 ---
const allLogs = ref([])
const loading = ref(false)
const dateRange = ref([])
const lineChartRef = ref(null)
const pieChartRef = ref(null)
const barChartRef = ref(null)
let chartInstances = []

// 日期快捷選項
const dateShortcuts = [
  { text: '最近一週', value: () => {
    const end = new Date(); const start = new Date();
    start.setTime(start.getTime() - 3600 * 1000 * 24 * 7);
    return [start, end]
  }},
  { text: '最近一個月', value: () => {
    const end = new Date(); const start = new Date();
    start.setMonth(start.getMonth() - 1);
    return [start, end]
  }},
  { text: '最近三個月', value: () => {
    const end = new Date(); const start = new Date();
    start.setMonth(start.getMonth() - 3);
    return [start, end]
  }}
]

// --- 核心邏輯：數據篩選 ---
const filteredLogs = computed(() => {
  if (!dateRange.value || dateRange.value.length !== 2) return allLogs.value;
  
  const start = new Date(dateRange.value[0]).getTime();
  // 結束日期設為當日 23:59:59
  const end = new Date(dateRange.value[1]).setHours(23, 59, 59, 999);

  return allLogs.value.filter(log => {
    const time = new Date(log.redeemTime).getTime();
    return time >= start && time <= end;
  });
});

// --- 統計數據計算 ---
const totalPointsSpent = computed(() => filteredLogs.value.reduce((s, l) => s + (l.pointsSpent || 0), 0))
const uniqueCoupons = computed(() => new Set(filteredLogs.value.map(l => l.couponId)).size)
const avgPoints = computed(() => {
  return filteredLogs.value.length > 0 
    ? (totalPointsSpent.value / filteredLogs.value.length).toFixed(1) 
    : 0
})

// --- API 請求 ---
const fetchLogs = async () => {
  loading.value = true
  try {
    const res = await axios.get('http://localhost:8080/api/discounts/logs')
    allLogs.value = res.data.data || []
    await nextTick()
    renderAllCharts()
  } catch (err) {
    console.error('資料載入失敗', err)
  } finally {
    loading.value = false
  }
}

// --- 圖表渲染 ---
const renderAllCharts = () => {
  // 清除舊實例
  chartInstances.forEach(c => c.dispose())
  chartInstances = []

  if (filteredLogs.value.length === 0) return

  renderLineChart()
  renderPieChart()
  renderBarChart()
}

const renderLineChart = () => {
  const chart = echarts.init(lineChartRef.value)
  const dateMap = {}
  filteredLogs.value.forEach(l => {
    const d = new Date(l.redeemTime).toLocaleDateString()
    dateMap[d] = (dateMap[d] || 0) + 1
  })
  const sortedDates = Object.keys(dateMap).sort((a, b) => new Date(a) - new Date(b))
  
  chart.setOption({
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(255, 255, 255, 0.9)' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: sortedDates },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      name: '兌換次數',
      type: 'line',
      smooth: true,
      data: sortedDates.map(d => dateMap[d]),
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64, 158, 255, 0.5)' },
          { offset: 1, color: 'rgba(64, 158, 255, 0)' }
        ])
      },
      lineStyle: { width: 3, color: '#409eff' },
      itemStyle: { color: '#409eff' }
    }]
  })
  chartInstances.push(chart)
}

const renderPieChart = () => {
  const chart = echarts.init(pieChartRef.value)
  const nameMap = {}
  filteredLogs.value.forEach(l => {
    const name = l.couponName || '未命名'
    nameMap[name] = (nameMap[name] || 0) + 1
  })

  let pieData = Object.entries(nameMap)
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value)

  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}次 ({d}%)' },
    legend: { type: 'scroll', orient: 'vertical', right: 5, top: 20, bottom: 20 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['40%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      data: pieData
    }]
  })
  chartInstances.push(chart)
}

const renderBarChart = () => {
  const chart = echarts.init(barChartRef.value)
  const rankMap = {}
  filteredLogs.value.forEach(l => {
    rankMap[l.couponName] = (rankMap[l.couponName] || 0) + 1
  })

  const sorted = Object.entries(rankMap)
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => a.value - b.value)
    .slice(-10)

  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '8%', bottom: '3%', top: '2%', containLabel: true },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: { type: 'category', data: sorted.map(d => d.name) },
    series: [{
      name: '兌換總量',
      type: 'bar',
      data: sorted.map(d => d.value),
      itemStyle: {
        color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
          { offset: 0, color: '#f6d365' }, { offset: 1, color: '#fda085' }
        ]),
        borderRadius: [0, 5, 5, 0]
      },
      label: { show: true, position: 'right' }
    }]
  })
  chartInstances.push(chart)
}

// --- 監聽與生命週期 ---
watch(filteredLogs, () => {
  renderAllCharts()
}, { deep: true })

const handleResize = () => chartInstances.forEach(c => c.resize())
const resetFilter = () => { dateRange.value = [] }

// --- Excel 匯出 ---
const exportToExcel = () => {
  const summary = {}
  filteredLogs.value.forEach(log => {
    if (!summary[log.couponName]) {
      summary[log.couponName] = { "ID": log.couponId, "名稱": log.couponName, "次數": 0, "點數": 0 }
    }
    summary[log.couponName]["次數"] += 1
    summary[log.couponName]["點數"] += log.pointsSpent
  })

  const ws = XLSX.utils.json_to_sheet(Object.values(summary))
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, "統計結果")
  XLSX.writeFile(wb, `分析報告_${dateRange.value[0] || '全部'}.xlsx`)
}

onMounted(() => {
  fetchLogs()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstances.forEach(c => c.dispose())
})
</script>

<style scoped>
.log-list-container { padding: 20px; background-color: #f5f7fa; min-height: 100vh; }

/* 標題與篩選 */
.page-title-box { display: flex; align-items: center; background: white; padding: 20px; border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.05); }
.title-icon { font-size: 2rem; color: #409eff; margin-right: 15px; }
.subtitle { margin: 0; color: #909399; }
.filter-card { border-radius: 12px; border: none; }
.filter-label { font-weight: bold; color: #606266; }

/* 統計卡片 */
.stat-card { display: flex; align-items: center; padding: 20px; border-radius: 12px; color: white; margin-bottom: 20px; }
.total-card { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.points-card { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
.avg-card { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
.merchant-card { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }
.stat-icon { font-size: 2.5rem; opacity: 0.2; margin-right: 15px; }
.stat-info h3 { margin: 0; font-size: 1.8rem; }

/* 圖表 */
.chart-card { border-radius: 12px; border: none; }
.chart-header { font-weight: bold; display: flex; align-items: center; }
</style>