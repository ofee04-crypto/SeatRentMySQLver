<template>
  <div class="seat-result-container">
    <!-- ========== Header 區域 ========== -->
    <section class="content-header">
      <div class="container-fluid">
        <div class="row align-items-center">
          <div class="col-sm-6">
            <div class="page-title-box">
              <div class="title-icon result-mode">
                <i class="fas fa-list-alt"></i>
              </div>
              <div class="title-content">
                <h1>Seat 查詢結果</h1>
                <p class="subtitle">顯示符合條件的座位資料</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="content">
      <div class="container-fluid">
        <transition name="fade-slide" appear>
          <el-card shadow="hover" class="result-card">
            <el-empty v-if="seatList.length === 0" description="沒有找到符合條件的資料。" />

            <el-table
              v-else
              :data="seatList"
              stripe
              style="width: 100%"
              :header-cell-style="{ background: '#f5f7fa', fontWeight: 'bold', color: '#303133' }"
              class="modern-table"
            >
              <el-table-column prop="seatsId" label="ID" width="70" align="center" />
              <el-table-column prop="seatsName" label="名稱" min-width="120">
                <template #default="{ row }">
                  <div class="seat-name-cell">
                    <i class="fas fa-chair text-primary mr-2"></i>
                    <span>{{ row.seatsName }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="seatsType" label="類型" width="100" />
              <el-table-column prop="seatsStatus" label="狀態" width="100" align="center">
                <template #default="{ row }">
                  <el-tag
                    :type="row.seatsStatus === '可用' ? 'success' : (row.seatsStatus === '維修' || row.seatsStatus === '故障' ? 'warning' : 'danger')"
                    size="small"
                    effect="light"
                  >
                    {{ row.seatsStatus }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="spotId" label="SpotId" width="90" align="center">
                <template #default="{ row }">
                  <el-tag type="info" size="small">{{ row.spotId }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="serialNumber" label="序號" width="120" />
              <el-table-column label="UpdatedAt" min-width="160">
                <template #default="{ row }">
                  {{ row.updatedAt?.replace('T', ' ').substring(0, 19) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button-group>
                    <el-tooltip content="檢視" placement="top">
                      <router-link :to="`/admin/seat/view/${row.seatsId}`">
                        <el-button size="small" type="info">
                          <i class="fas fa-eye"></i>
                        </el-button>
                      </router-link>
                    </el-tooltip>
                    <el-tooltip content="修改" placement="top">
                      <router-link :to="`/admin/seat/edit/${row.seatsId}`">
                        <el-button size="small" type="primary">
                          <i class="fas fa-edit"></i>
                        </el-button>
                      </router-link>
                    </el-tooltip>
                  </el-button-group>
                </template>
              </el-table-column>
            </el-table>

            <div class="card-actions">
              <router-link to="/admin/seat/search">
                <el-button type="primary">
                  <i class="fas fa-search mr-1"></i> 回查詢
                </el-button>
              </router-link>
              <router-link to="/admin/seat/list">
                <el-button>
                  <i class="fas fa-list mr-1"></i> 回清單
                </el-button>
              </router-link>
            </div>
          </el-card>
        </transition>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const seatList = ref([])

// 明確定義元件名稱，方便識別與除錯
defineOptions({
  name: 'SeatResult'
})

onMounted(async () => {
  const query = route.query
  console.log('Search query:', query)

  try {
    // 改用 RESTful API: GET /api/seats/search (搭配查詢參數)
    const response = await axios.get('/api/seats/search', { params: query })
    seatList.value = response.data
  } catch (error) {
    console.error('Error fetching seats:', error)
  }
})

// [新增] 簡單的狀態顏色輔助函式
const getStatusClass = (status) => {
  if (status === '可用') return 'badge badge-success'
  if (status === '維修' || status === '故障') return 'badge badge-warning'
  if (status === '停用') return 'badge badge-danger'
  return 'badge badge-secondary'
}
</script>

<style scoped>
/* ========== 頁面容器 ========== */
.seat-result-container {
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
  min-height: 100vh;
}

/* ========== 頁面標題區 ========== */
.page-title-box {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.title-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  transition: transform 0.3s ease;
}

.title-icon:hover {
  transform: scale(1.1) rotate(5deg);
}

.result-mode {
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
}

.title-content h1 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
  color: #303133;
}

.title-content .subtitle {
  margin: 4px 0 0;
  font-size: 0.875rem;
  color: #909399;
}

/* ========== 結果卡片 ========== */
.result-card {
  border-radius: 12px;
}

.modern-table {
  border-radius: 8px;
  overflow: hidden;
}

.seat-name-cell {
  display: flex;
  align-items: center;
}

/* ========== 按鈕區 ========== */
.card-actions {
  display: flex;
  gap: 12px;
  padding-top: 24px;
  margin-top: 24px;
  border-top: 1px solid #ebeef5;
}

/* ========== 動畫效果 ========== */
.fade-slide-enter-active {
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.fade-slide-leave-active {
  transition: all 0.3s ease-in;
}

.fade-slide-enter-from {
  transform: translateY(20px);
  opacity: 0;
}

.fade-slide-leave-to {
  transform: translateY(-10px);
  opacity: 0;
}

/* ========== 工具類 ========== */
.mr-1 { margin-right: 4px; }
.mr-2 { margin-right: 8px; }
.text-primary { color: #409eff; }
</style>
