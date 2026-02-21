<template>
  <div class="spot-one-container">
    <!-- ========== Header 區域 ========== -->
    <section class="content-header">
      <div class="container-fluid">
        <div class="row align-items-center">
          <div class="col-sm-6">
            <div class="page-title-box">
              <div class="title-icon detail-mode">
                <i class="fas fa-map-marker-alt"></i>
              </div>
              <div class="title-content">
                <h1>景點詳細資料</h1>
                <p class="subtitle">查看據點完整資訊</p>
              </div>
            </div>
          </div>
          <div class="col-sm-6 text-right">
            <ol class="breadcrumb-modern">
              <li><router-link to="/admin/spot/list"><i class="fas fa-home"></i> Home</router-link></li>
              <li class="active">Spot Detail</li>
            </ol>
          </div>
        </div>
      </div>
    </section>

    <section class="content">
      <div class="container-fluid">
        <transition name="zoom-fade" appear>
          <div>
            <el-card v-if="loading" shadow="hover" class="detail-card">
              <div class="loading-state">
                <i class="fas fa-spinner fa-spin"></i>
                <span>載入中...</span>
              </div>
            </el-card>

            <el-card v-else-if="!spot" shadow="hover" class="detail-card">
              <el-empty description="找不到資料" />
            </el-card>

            <el-card v-else shadow="hover" class="detail-card">
              <div class="detail-content">
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="ID">
                    <el-tag type="info">{{ spot.spotId }}</el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="代碼 (Code)">
                    <span class="code-text">{{ spot.spotCode }}</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="名稱 (Name)">
                    <span class="name-text">{{ spot.spotName }}</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="狀態 (Status)">
                    <el-tag :type="spot.spotStatus === '營運中' ? 'success' : (spot.spotStatus === '維修中' ? 'warning' : 'danger')">
                      {{ spot.spotStatus }}
                    </el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="地址 (Address)" :span="2">
                    <i class="fas fa-map-marker-alt text-primary mr-2"></i>{{ spot.spotAddress }}
                  </el-descriptions-item>
                  <el-descriptions-item label="Merchant ID">
                    <el-tag v-if="spot.merchantId" type="info">{{ spot.merchantId }}</el-tag>
                    <span v-else class="text-muted">-</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="緯度 (Latitude)">
                    {{ spot.latitude || '-' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="經度 (Longitude)">
                    {{ spot.longitude || '-' }}
                  </el-descriptions-item>
                </el-descriptions>
              </div>

              <div class="card-actions">
                <router-link to="/admin/spot/list">
                  <el-button>
                    <i class="fas fa-arrow-left mr-1"></i> 回列表
                  </el-button>
                </router-link>
                <router-link :to="`/admin/spot/edit/${spot.spotId}`">
                  <el-button type="primary">
                    <i class="fas fa-edit mr-1"></i> 編輯
                  </el-button>
                </router-link>
              </div>
            </el-card>
          </div>
        </transition>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import axios from 'axios';

// 明確定義元件名稱，方便識別與除錯
defineOptions({
  name: 'SpotOne'
});

const route = useRoute();
const spot = ref(null);
const loading = ref(true);

onMounted(async () => {
  const id = route.params.id;
  if (id) {
    try {
      const response = await axios.get(`/api/spot/${id}`);
      spot.value = response.data;
    } catch (error) {
      console.error('Error fetching spot:', error);
    }
  }
  loading.value = false;
});
</script>

<style scoped>
/* ========== 頁面容器 ========== */
.spot-one-container {
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

.detail-mode {
  background: linear-gradient(135deg, #909399 0%, #c0c4cc 100%);
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

/* ========== 面包屑 ========== */
.breadcrumb-modern {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
}

.breadcrumb-modern li a {
  color: #409eff;
  text-decoration: none;
}

.breadcrumb-modern li.active {
  color: #909399;
}

.breadcrumb-modern li:not(:last-child)::after {
  content: '/';
  margin-left: 8px;
  color: #c0c4cc;
}

/* ========== 詳情卡片 ========== */
.detail-card {
  border-radius: 12px;
  max-width: 900px;
  margin: 0 auto;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px;
  color: #409eff;
  font-size: 16px;
}

.detail-content {
  margin-bottom: 24px;
}

.code-text {
  font-family: monospace;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
}

.name-text {
  font-weight: 600;
  color: #303133;
}

/* ========== 按鈕區 ========== */
.card-actions {
  display: flex;
  justify-content: space-between;
  padding-top: 24px;
  border-top: 1px solid #ebeef5;
}

/* ========== 動畫效果 ========== */
.zoom-fade-enter-active {
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.zoom-fade-leave-active {
  transition: all 0.3s ease-in;
}

.zoom-fade-enter-from {
  transform: scale(0.9);
  opacity: 0;
}

.zoom-fade-leave-to {
  transform: scale(0.95);
  opacity: 0;
}

/* ========== 工具類 ========== */
.mr-1 { margin-right: 4px; }
.mr-2 { margin-right: 8px; }
.text-primary { color: #409eff; }
.text-muted { color: #909399; }
</style>