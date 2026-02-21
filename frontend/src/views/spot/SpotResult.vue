<template>
  <div class="spot-result-container">
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
                <h1>查詢結果列表</h1>
                <p class="subtitle">顯示符合條件的據點資料</p>
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
            <el-empty v-if="spotList.length === 0" description="沒有找到符合條件的資料。" />

            <el-table
              v-else
              :data="spotList"
              stripe
              style="width: 100%"
              :header-cell-style="{ background: '#f5f7fa', fontWeight: 'bold', color: '#303133' }"
              class="modern-table"
            >
              <el-table-column prop="spotId" label="ID" width="70" align="center" />
              <el-table-column prop="spotCode" label="代碼" width="100" />
              <el-table-column prop="spotName" label="名稱" min-width="150">
                <template #default="{ row }">
                  <div class="spot-name-cell">
                    <i class="fas fa-store text-primary mr-2"></i>
                    <span>{{ row.spotName }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="spotAddress" label="地址" min-width="200" show-overflow-tooltip />
              <el-table-column prop="spotStatus" label="狀態" width="100" align="center">
                <template #default="{ row }">
                  <el-tag
                    :type="row.spotStatus === '營運中' ? 'success' : (row.spotStatus === '維修中' ? 'warning' : 'danger')"
                    size="small"
                    effect="light"
                  >
                    {{ row.spotStatus }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="Merchant ID" width="110" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.merchantId" type="info" size="small">{{ row.merchantId }}</el-tag>
                  <span v-else class="text-muted">-</span>
                </template>
              </el-table-column>
              <el-table-column prop="latitude" label="緯度" width="100" />
              <el-table-column prop="longitude" label="經度" width="100" />
            </el-table>

            <div class="card-actions">
              <router-link to="/admin/spot/search">
                <el-button type="primary">
                  <i class="fas fa-search mr-1"></i> 回查詢
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
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import axios from 'axios';

const route = useRoute();
const spotList = ref([]);

// 明確定義元件名稱，方便識別與除錯
defineOptions({
  name: 'SpotResult'
})

onMounted(async () => {
  const query = route.query;
  try {
    // [AXIOS GET 請求原理]
    // 1. 動作：發送 GET 請求到 /spot/condition，並帶上查詢參數 (query)。
    const response = await axios.get('/api/spot/condition', { params: query });
    // 2. 接收：後端回傳的是一個 JSON 陣列 (List<RentalSpot>，即租借據點列表)。
    // 3. 更新：將資料存入 spotList，Vue 的 v-for 就會自動把表格畫出來。
    spotList.value = response.data;
  } catch (error) {
    console.error('Error fetching spots:', error);
  }
});
</script>

<style scoped>
/* ========== 頁面容器 ========== */
.spot-result-container {
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

.spot-name-cell {
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
.text-muted { color: #909399; }
</style>