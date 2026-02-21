<template>
  <div class="seat-search-container">
    <!-- ========== Header 區域 ========== -->
    <section class="content-header">
      <div class="container-fluid">
        <div class="row align-items-center">
          <div class="col-sm-6">
            <div class="page-title-box">
              <div class="title-icon search-mode">
                <i class="fas fa-search"></i>
              </div>
              <div class="title-content">
                <h1>Seat 條件查詢</h1>
                <p class="subtitle">依條件搜尋座位資料</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="content">
      <div class="container-fluid">
        <transition name="zoom-fade" appear>
          <el-card shadow="hover" class="search-card">
            <el-form :model="searchCriteria" label-width="120px" label-position="top" @submit.prevent="handleSearch">
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="名稱 (模糊)">
                    <el-input v-model="searchCriteria.seatsName" placeholder="請輸入座位名稱" clearable>
                      <template #prefix><i class="fas fa-chair"></i></template>
                    </el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="類型 (模糊)">
                    <el-input v-model="searchCriteria.seatsType" placeholder="請輸入座位類型" clearable>
                      <template #prefix><i class="fas fa-couch"></i></template>
                    </el-input>
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="狀態">
                    <el-select v-model="searchCriteria.seatsStatus" placeholder="(不限制)" clearable style="width: 100%">
                      <el-option label="可用" value="可用" />
                      <el-option label="維修" value="維修" />
                      <el-option label="停用" value="停用" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="SpotId (精準)">
                    <el-input v-model="searchCriteria.spotId" type="number" placeholder="請輸入據點ID" clearable>
                      <template #prefix><i class="fas fa-map-marker-alt"></i></template>
                    </el-input>
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="序號 (模糊)">
                    <el-input v-model="searchCriteria.serialNumber" placeholder="請輸入序號" clearable>
                      <template #prefix><i class="fas fa-barcode"></i></template>
                    </el-input>
                  </el-form-item>
                </el-col>
              </el-row>

              <div class="form-actions">
                <el-button type="primary" @click="handleSearch" class="search-btn">
                  <i class="fas fa-search mr-1"></i> 查詢
                </el-button>
                <router-link to="/admin/seat/list">
                  <el-button>
                    <i class="fas fa-arrow-left mr-1"></i> 回清單
                  </el-button>
                </router-link>
              </div>
            </el-form>
          </el-card>
        </transition>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

// 明確定義元件名稱，方便識別與除錯
defineOptions({
  name: 'SeatSearch'
})

const router = useRouter()
const searchCriteria = ref({
  seatsName: '',
  seatsType: '',
  seatsStatus: '',
  spotId: '',
  serialNumber: '',
})

const handleSearch = () => {
  // [修正] 加上 /admin
  router.push({ path: '/admin/seat/result', query: { ...searchCriteria.value } })
}
</script>

<style scoped>
/* ========== 頁面容器 ========== */
.seat-search-container {
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

.search-mode {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
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

/* ========== 搜尋卡片 ========== */
.search-card {
  border-radius: 12px;
  max-width: 800px;
  margin: 0 auto;
}

/* ========== 按鈕區 ========== */
.form-actions {
  display: flex;
  gap: 12px;
  padding-top: 24px;
  margin-top: 24px;
  border-top: 1px solid #ebeef5;
}

.search-btn {
  min-width: 120px;
  border-radius: 10px;
  font-weight: 600;
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
</style>
