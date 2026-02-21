<template>
  <div class="dispatch-monitor container-fluid py-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2 class="fw-bold text-dark">
        <i class="fas fa-chart-line me-2"></i>站點調度監控中心
      </h2>
      <div class="controls d-flex gap-3 align-items-center">
        <span class="text-muted small">
          最後更新: {{ lastUpdateTime }}
        </span>
        <button 
          class="btn btn-primary" 
          @click="fetchData" 
          :disabled="loading"
        >
          <i class="fas fa-sync-alt" :class="{ 'fa-spin': loading }"></i> 
          {{ loading ? '更新中...' : '立即更新' }}
        </button>
      </div>
    </div>

    <!-- 警示摘要卡片 -->
    <div class="row mb-4">
      <div class="col-md-4">
        <div class="card bg-danger text-white shadow-sm h-100">
          <div class="card-body d-flex justify-content-between align-items-center">
            <div>
              <h6 class="card-title mb-0">急需補給 (可租用率過低)</h6>
              <h2 class="display-6 fw-bold my-2">{{ lowStockSpots.length }}</h2>
              <small>可租用率 &lt; 20%</small>
            </div>
            <i class="fas fa-battery-empty fa-3x opacity-50"></i>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="card bg-warning text-dark shadow-sm h-100">
          <div class="card-body d-flex justify-content-between align-items-center">
            <div>
              <h6 class="card-title mb-0">建議清運 (可租用率過高)</h6>
              <h2 class="display-6 fw-bold my-2">{{ overStockSpots.length }}</h2>
              <small>可租用率 &gt; 80%</small>
            </div>
            <i class="fas fa-battery-full fa-3x opacity-50"></i>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="card bg-success text-white shadow-sm h-100">
          <div class="card-body d-flex justify-content-between align-items-center">
            <div>
              <h6 class="card-title mb-0">營運正常</h6>
              <h2 class="display-6 fw-bold my-2">{{ normalSpots.length }}</h2>
              <small>可租用率健康</small>
            </div>
            <i class="fas fa-check-circle fa-3x opacity-50"></i>
          </div>
        </div>
      </div>
    </div>

    <!-- 監控列表 -->
    <div class="card shadow">
      <div class="card-header bg-white py-3">
        <h5 class="mb-0">即時站點狀態列表</h5>
      </div>
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th>ID</th>
              <th>租借據點名稱</th>
              <th>各據點容量上限</th>
              <th>可借數</th>
              <th>可租用率</th>
              <th>調度建議</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="spot in allSpots" :key="spot.spotId" :class="getRowClass(spot)">
              <td>{{ spot.spotId }}</td>
              <td class="fw-bold">{{ spot.spotName }}</td>
              <td>{{ spot.totalSeats }}</td>
              <td>{{ spot.availableSeats }}</td>
              <td style="width: 200px;">
                <div class="progress" style="height: 20px;">
                  <div 
                    class="progress-bar" 
                    role="progressbar" 
                    :style="{ width: getRentableRate(spot) + '%', backgroundColor: getProgressColor(spot) }"
                    :aria-valuenow="getRentableRate(spot)" 
                    aria-valuemin="0" 
                    aria-valuemax="100"
                  >
                    {{ getRentableRate(spot) }}%
                  </div>
                </div>
              </td>
              <td>
                <span class="badge rounded-pill" :class="getStatusBadge(spot).class">
                  {{ getStatusBadge(spot).text }}
                </span>
              </td>
              <td>
                <button class="btn btn-sm btn-outline-primary" @click="notifyDispatch(spot)">
                  <i class="fas fa-paper-plane"></i> 發送調度單
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import Swal from 'sweetalert2';

defineOptions({ name: 'DispatchMonitor' });

const allSpots = ref([]);
const loading = ref(false);
const lastUpdateTime = ref('-');

const API_URL = '/api/analyze/spot-monitor';

const getRentableRate = (spot) => {
  const total = Number(spot.totalSeats) || 0;
  const available = Number(spot.availableSeats) || 0;
  if (total <= 0) return 0;

  const rate = Math.round((available / total) * 100);
  return Math.min(100, Math.max(0, rate));
};


const getStatusType = (spot) => {
  const rate = getRentableRate(spot);
  if (rate < 20) return 'NEED_SUPPLY';
  if (rate > 80) return 'NEED_CLEAR';
  return 'NORMAL';
};

const lowStockSpots = computed(() => allSpots.value.filter(s => getStatusType(s) === 'NEED_SUPPLY'));
const overStockSpots = computed(() => allSpots.value.filter(s => getStatusType(s) === 'NEED_CLEAR'));
const normalSpots = computed(() => allSpots.value.filter(s => getStatusType(s) === 'NORMAL'));

const getRowClass = (spot) => {
  const type = getStatusType(spot);
  if (type === 'NEED_SUPPLY') return 'table-danger';
  if (type === 'NEED_CLEAR') return 'table-warning';
  return '';
};

const getProgressColor = (spot) => {
  const type = getStatusType(spot);
  if (type === 'NEED_SUPPLY') return '#dc3545'; // Red
  if (type === 'NEED_CLEAR') return '#ffc107'; // Yellow
  return '#198754'; // Green
};

const getStatusBadge = (spot) => {
  const type = getStatusType(spot);
  if (type === 'NEED_SUPPLY')  return { class: 'bg-danger', text: '急需補給' };
  if (type === 'NEED_CLEAR') return { class: 'bg-warning text-dark', text: '建議清運' };
  return { class: 'bg-success', text: '正常' };
};

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await axios.get(API_URL);
    allSpots.value = res.data;
    lastUpdateTime.value = new Date().toLocaleTimeString();
  } catch (err) {
    console.error('監控數據載入失敗', err);
    Swal.fire({
      icon: 'error',
      title: '載入失敗',
      text: '無法獲取站點監控數據，請稍後再試。'
    });
  } finally {
    loading.value = false;
  }
};

const notifyDispatch = (spot) => {
  const type = getStatusType(spot);
  let msg = '';
  if (type === 'NEED_SUPPLY') msg = `請派員前往 [${spot.spotName}] 補充設備！`;
  else if (type === 'NEED_CLEAR') msg = `請派員前往 [${spot.spotName}] 回收設備！`;
  else msg = `[${spot.spotName}] 目前狀態正常，確定要派單？`;

  Swal.fire({
    title: '發送調度通知?',
    text: msg,
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: '發送',
    cancelButtonText: '取消'
  }).then((result) => {
    if (result.isConfirmed) {
      Swal.fire('已發送', '管理員已收到通知', 'success');
    }
  });
};

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.dispatch-monitor {
  background-color: #f8f9fa;
  min-height: 100vh;
}
.card {
  border: none;
  border-radius: 10px;
}
.table-danger {
  background-color: #ffebe9 !important;
}
.table-warning {
  background-color: #fff8e1 !important;
}
</style>