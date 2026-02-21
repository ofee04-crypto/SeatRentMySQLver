<template>
  <div class="spot-analyze-container">
    <el-card class="mb-4">
      <template #header>
        <div class="flex justify-between items-center">
          <h2 class="text-xl font-bold m-0 flex items-center gap-2">
            <i class="fas fa-chart-line text-blue-500"></i>
            據點營運統計儀表板
          </h2>
          <div class="flex items-center gap-2" style="display: flex; gap: 10px; align-items: center;">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="開始日期"
              end-placeholder="結束日期"
              value-format="YYYY-MM-DD"
              :shortcuts="shortcuts"
              @change="fetchData"
            />
            <el-button type="primary" :loading="loading" @click="fetchData">
              <i class="fas fa-sync-alt mr-1"></i> 重新載入
            </el-button>
          </div>
        </div>
      </template>

      <!-- 恢復為純圖表模式 (移除方案 A 的 Tabs) -->
      <div v-loading="loading">
        <el-row :gutter="20" class="mb-4">
          <el-col :md="10" :sm="24" class="mb-4">
            <el-card shadow="hover" class="chart-card">
              <template #header><b>各縣市站點分佈</b></template>
              <div v-if="hasData" ref="cityChartRef" class="chart-container"></div>
              <el-empty v-else description="暫無數據" />
            </el-card>
          </el-col>
          <el-col :md="14" :sm="24" class="mb-4">
            <el-card shadow="hover" class="chart-card">
              <template #header><b>24小時租借熱度</b></template>
              <div v-if="hasData" ref="heatChartRef" class="chart-container"></div>
              <el-empty v-else description="暫無數據" />
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :md="10" :sm="24" class="mb-4">
            <el-card shadow="hover" class="chart-card">
              <template #header><b>租借時長分佈</b></template>
              <div v-if="hasData" ref="durationChartRef" class="chart-container"></div>
              <el-empty v-else description="暫無數據" />
            </el-card>
          </el-col>
          <el-col :md="14" :sm="24" class="mb-4">
            <el-card shadow="hover" class="chart-card">
              <template #header><b>站點即時分佈概覽</b></template>
              <el-table :data="statsData.spotMonitor" style="width: 100%" height="280">
                <el-table-column prop="spotName" label="據點" />
                <el-table-column prop="totalSeats" label="總位" width="80" align="center" />
                <el-table-column prop="availableSeats" label="可借" width="80" align="center" />
                <el-table-column label="可借率" width="120">
                  <template #default="scope">
                    <el-progress :percentage="Math.round((scope.row.availableSeats/scope.row.totalSeats)*100)" />
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import axios from 'axios'
import ApexCharts from 'apexcharts'
import Swal from 'sweetalert2'

const loading = ref(false)
const hasData = ref(false)
const statsData = ref({ cityDistribution: [], spotMonitor: [], hourlyHeatMap: [], durationStats: [] })
const dateRange = ref([])

const shortcuts = [
  { text: '最近一週', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 3600 * 1000 * 24 * 7); return [start, end] } },
  { text: '最近一個月', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 3600 * 1000 * 24 * 30); return [start, end] } },
  { text: '最近三個月', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 3600 * 1000 * 24 * 90); return [start, end] } },
]

const cityChartRef = ref(null); const heatChartRef = ref(null); const durationChartRef = ref(null)
let cityChart = null; let heatChart = null; let durationChart = null

const fetchData = async () => {
  loading.value = true
  try {
    const params = {}
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await axios.get('/api/analyze/stats', { params })
    if (res.data) { statsData.value = res.data; hasData.value = true; nextTick(() => renderCharts()) }
  } catch (err) { console.error(err) } finally { loading.value = false }
}

const renderCharts = () => {
  if (cityChartRef.value) {
    if (cityChart) cityChart.destroy()
    cityChart = new ApexCharts(cityChartRef.value, {
      series: statsData.value.cityDistribution.map(i => i.spotCount),
      labels: statsData.value.cityDistribution.map(i => i.city),
      chart: { type: 'donut', height: 280 }
    })
    cityChart.render()
  }
  if (heatChartRef.value) {
    if (heatChart) heatChart.destroy()
    heatChart = new ApexCharts(heatChartRef.value, {
      series: [{ name: '租借', data: Array.from({length:24}, (_,i) => statsData.value.hourlyHeatMap.find(d => d.hourofDay === i)?.rentedCount || 0) }],
      chart: { type: 'area', height: 280 },
      xaxis: { categories: Array.from({length:24}, (_,i) => `${i}:00`) }
    })
    heatChart.render()
  }
  if (durationChartRef.value) {
    if (durationChart) durationChart.destroy()
    durationChart = new ApexCharts(durationChartRef.value, {
      series: [{ name: '筆數', data: statsData.value.durationStats.map(i => i.count) }],
      chart: { type: 'bar', height: 280 },
      xaxis: { categories: statsData.value.durationStats.map(i => i.durationRange) }
    })
    durationChart.render()
  }
}

onMounted(() => fetchData())
onBeforeUnmount(() => { if(cityChart) cityChart.destroy(); if(heatChart) heatChart.destroy(); if(durationChart) durationChart.destroy() })
</script>

<style scoped>
.chart-card { border-radius: 12px; height: 100%; }
.chart-container { min-height: 280px; }
.mb-4 { margin-bottom: 20px; }
</style>
