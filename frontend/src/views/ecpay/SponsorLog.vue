<template>
  <div class="admin-container">
    <section class="content-header mb-4">
      <div class="page-title-box">
        <div class="title-icon"><i class="fas fa-gem"></i></div>
        <div class="title-content">
          <h1>贊助與財務分析中心</h1>
          <p class="subtitle">追蹤月度營收增長與會員贊助分佈</p>
        </div>
      </div>
    </section>

    <el-card shadow="never" class="filter-card mb-4">
      <div class="d-flex justify-content-between align-items-center flex-wrap gap-3">
        <div class="d-flex gap-3 align-items-center">
          <div class="filter-item">
            <span class="label"><i class="fas fa-calendar-alt me-1"></i> 月份查詢：</span>
            <el-date-picker
              v-model="selectedMonth"
              type="month"
              placeholder="選擇月份"
              value-format="YYYY-MM"
              @change="handleFilterChange"
            />
          </div>
          <el-divider direction="vertical" />
          <el-input 
            v-model="searchQuery" 
            placeholder="搜尋 ID 或 單號" 
            style="width: 220px;" 
            clearable 
            @input="handleFilterChange"
          />
          <el-select v-model="statusFilter" placeholder="狀態" clearable style="width: 120px;" @change="handleFilterChange">
            <el-option label="待支付" :value="0" />
            <el-option label="成功" :value="1" />
            <el-option label="失敗" :value="2" />
          </el-select>
        </div>
        <div class="actions">
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="success" @click="exportToExcel">
            <i class="fas fa-file-excel me-1"></i> 匯出報表
          </el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20" class="mb-4">
      <el-col :xs="24" :sm="12" :md="8">
        <div class="stat-card revenue-card">
          <div class="stat-icon"><i class="fas fa-sack-dollar"></i></div>
          <div class="stat-info">
            <h3>$ {{ totalAmount.toLocaleString() }}</h3>
            <span>{{ selectedMonth || '歷年' }} 成功贊助總額</span>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8">
        <div class="stat-card count-card">
          <div class="stat-icon"><i class="fas fa-receipt"></i></div>
          <div class="stat-info">
            <h3>{{ filteredData.length }} <small>筆</small></h3>
            <span>{{ selectedMonth || '歷年' }} 交易總筆數</span>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8">
        <div class="stat-card user-card">
          <div class="stat-icon"><i class="fas fa-users"></i></div>
          <div class="stat-info">
            <h3>{{ uniqueDonors }} <small>人</small></h3>
            <span>本月贊助會員數</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="chart-card mb-4">
      <template #header>
        <div class="card-header">
          <i class="fas fa-chart-line me-2 text-primary"></i> 
          {{ selectedMonth || '年度' }} 贊助趨勢分析
        </div>
      </template>
      <div ref="chartRef" style="height: 300px; width: 100%;"></div>
    </el-card>

    <el-card shadow="hover" class="table-card">
      <el-table :data="paginatedData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="merchantTradeNo" label="訂單編號" min-width="180">
          <template #default="{ row }"><code class="order-code">{{ row.merchantTradeNo }}</code></template>
        </el-table-column>
        <el-table-column prop="memberId" label="會員 ID" width="100" />
        <el-table-column prop="amount" label="金額" width="120" sortable>
          <template #default="{ row }"><span class="amount-text">${{ row.amount }}</span></template>
        </el-table-column>
        <el-table-column label="狀態" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="paymentType" label="支付方式" width="120" />
        <el-table-column prop="createdAt" label="時間" width="180" sortable />
        <el-table-column prop="sponsorComment" label="留言" show-overflow-tooltip />
      </el-table>

      <div class="d-flex justify-content-end mt-4">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="filteredData.length"
          layout="total, prev, pager, next"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, onUnmounted } from 'vue';
import axios from 'axios';
import * as XLSX from 'xlsx';
import * as echarts from 'echarts';

// --- 響應式變數 ---
const loading = ref(false);
const allData = ref([]);
const selectedMonth = ref(''); // YYYY-MM
const searchQuery = ref('');
const statusFilter = ref(null);
const currentPage = ref(1);
const pageSize = ref(10);
const chartRef = ref(null);
let myChart = null;

// --- 數據獲取 ---
const fetchSponsors = async () => {
  loading.value = true;
  try {
    const apiUrl = window.APP_CONFIG?.API_URL || 'http://localhost:8080';
    const response = await axios.get(`${apiUrl}/api/payment/admin/sponsors`);
    allData.value = response.data;
    updateChart();
  } catch (error) {
    console.error("載入失敗", error);
  } finally {
    loading.value = false;
  }
};

// --- 核心過濾邏輯 ---
const filteredData = computed(() => {
  return allData.value.filter(item => {
    const matchesMonth = !selectedMonth.value || (item.createdAt && item.createdAt.startsWith(selectedMonth.value));
    const query = searchQuery.value.toLowerCase();
    const matchesSearch = item.merchantTradeNo.toLowerCase().includes(query) || String(item.memberId).includes(query);
    const matchesStatus = statusFilter.value === null || item.status === statusFilter.value;
    return matchesMonth && matchesSearch && matchesStatus;
  });
});

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredData.value.slice(start, start + pageSize.value);
});

// --- 統計計算 ---
const totalAmount = computed(() => 
  filteredData.value.filter(i => i.status === 1).reduce((s, i) => s + (i.amount || 0), 0)
);
const uniqueDonors = computed(() => new Set(filteredData.value.map(i => i.memberId)).size);

// --- 圖表邏輯 ---
const updateChart = () => {
  if (!chartRef.value) return;
  if (!myChart) myChart = echarts.init(chartRef.value);

  // 按日期統計成功金額
  const stats = {};
  filteredData.value.filter(i => i.status === 1).forEach(i => {
    const day = i.createdAt.split(' ')[0]; // 抓 YYYY-MM-DD
    stats[day] = (stats[day] || 0) + i.amount;
  });

  const sortedDays = Object.keys(stats).sort();
  
  myChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: sortedDays },
    yAxis: { type: 'value', name: '金額' },
    series: [{
      data: sortedDays.map(d => stats[d]),
      type: 'line',
      smooth: true,
      color: '#409eff',
      areaStyle: { opacity: 0.1 }
    }]
  });
};

// --- 功能函數 ---
const statusType = s => (s === 1 ? 'success' : s === 2 ? 'danger' : 'info');
const statusText = s => (s === 1 ? '成功' : s === 2 ? '失敗' : '待支付');
const resetFilters = () => { selectedMonth.value = ''; searchQuery.value = ''; statusFilter.value = null; };
const handleFilterChange = () => { currentPage.value = 1; updateChart(); };

const exportToExcel = () => {
  const ws = XLSX.utils.json_to_sheet(filteredData.value);
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, "Sponsors");
  XLSX.writeFile(wb, `贊助報表_${selectedMonth.value || '全部'}.xlsx`);
};

// --- 生命週期 ---
onMounted(() => {
  fetchSponsors();
  window.addEventListener('resize', () => myChart?.resize());
});
watch([selectedMonth, searchQuery, statusFilter], () => updateChart());
</script>

<style scoped>
.admin-container { padding: 20px; background: #f5f7fa; min-height: 100vh; }
.page-title-box { background: white; padding: 20px; border-radius: 12px; display: flex; align-items: center; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }
.title-icon { font-size: 2rem; color: #409eff; margin-right: 15px; }
.stat-card { padding: 20px; border-radius: 12px; color: white; display: flex; align-items: center; }
.revenue-card { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.count-card { background: linear-gradient(135deg, #2af598 0%, #009efd 100%); }
.user-card { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
.stat-icon { font-size: 2.5rem; opacity: 0.3; margin-right: 15px; }
.order-code { background: #f0f2f5; padding: 2px 6px; border-radius: 4px; font-family: monospace; }
.amount-text { color: #e6a23c; font-weight: bold; }
.chart-card { border-radius: 12px; }
</style>