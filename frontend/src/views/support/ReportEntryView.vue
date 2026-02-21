<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

// 問題類型選項
const reportTypes = [
  {
    type: 'damage',
    icon: '🔧',
    title: '機台/椅子毀損問題',
    desc: '回報設備損壞、故障、清潔等問題',
    color: '#f56c6c',
    action: () => router.push('/support/report/damage')
  },
  {
    type: 'borrow',
    icon: '📋',
    title: '借還問題',
    desc: '租借訂單、歸還異常等問題',
    color: '#e6a23c',
    action: () => router.push({ path: '/support/report/manual', query: { type: 'borrow' } })
  },
  {
    type: 'other',
    icon: '❓',
    title: '其他',
    desc: '其他類型的問題諮詢',
    color: '#909399',
    action: () => router.push({ path: '/support/report/manual', query: { type: 'other' } })
  }
]

// 返回客服支援中心
const goBack = () => {
  router.push('/support')
}
</script>

<template>
  <div class="report-entry-container">
    <!-- 頁面標題 -->
    <section class="page-header">
      <div class="container-fluid">
        <div class="header-content">
          <div class="header-icon">
            <i class="fas fa-exclamation-circle"></i>
          </div>
          <div class="header-text">
            <h1>問題回報</h1>
            <p>請選擇您要回報的問題類型</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 選項卡片 -->
    <section class="content">
      <div class="container-fluid">
        <div class="type-cards">
          <div
            v-for="item in reportTypes"
            :key="item.type"
            class="type-card"
            @click="item.action"
          >
            <div class="card-icon" :style="{ background: item.color }">
              {{ item.icon }}
            </div>
            <div class="type-text">
              <div class="type-title">{{ item.title }}</div>
              <div class="type-desc">{{ item.desc }}</div>
            </div>
            <div class="card-arrow">
              <i class="fas fa-arrow-right"></i>
            </div>
          </div>
        </div>

        <!-- 返回按鈕 -->
        <div class="back-section">
          <el-button size="large" @click="goBack">
            <i class="fas fa-arrow-left mr-2"></i> 返回客服支援中心
          </el-button>
        </div>

        <!-- 提示資訊 -->
        <div class="tips-card">
          <div class="tip-item">
            <i class="fas fa-info-circle"></i>
            <span>選擇正確的問題類型能幫助我們更快為您處理</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* ========== 頁面容器 ========== */
.report-entry-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  padding-bottom: 40px;
}

/* ========== 頁面標題 ========== */
.page-header {
  background: linear-gradient(135deg, #d4e3ee 0%, #c8d9e6 100%);
  padding: 40px 20px;
  color: white;
}

.header-content {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-icon {
  width: 64px;
  height: 64px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
}

.header-text h1 {
  margin: 0 0 5px;
  font-size: 1.8rem;
  font-weight: 800;
  color: #2c3e50;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.15), 0 1px 3px rgba(255, 255, 255, 0.5);
}

.header-text p {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: #34495e;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.1), 0 1px 2px rgba(255, 255, 255, 0.3);
}

/* ========== 內容區域 ========== */
.content {
  padding: 40px 20px;
}

/* ========== 類型卡片 ========== */
.type-cards {
  max-width: 900px;
  margin: 0 auto 30px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
}

.type-card {
  background: white;
  border-radius: 16px;
  padding: 40px 30px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  position: relative;
  overflow: hidden;
}

.type-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, transparent, var(--card-color, #409eff), transparent);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.type-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.type-card:hover::before {
  opacity: 1;
}

.card-icon {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  margin: 0 auto 20px;
  transition: all 0.3s ease;
}

.type-card:hover .card-icon {
  transform: scale(1.1) rotate(5deg);
}

.type-text {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}

.type-title {
  font-size: 1.3rem;
  font-weight: 700;
  color: #2c3e50;
  margin: 0;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  text-align: center;
}

.type-desc {
  font-size: 0.95rem;
  color: #606266;
  margin: 0;
  line-height: 1.6;
  font-weight: 500;
  text-align: center;
}

.card-arrow {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
  transition: all 0.3s ease;
  color: #909399;
}

.type-card:hover .card-arrow {
  background: #409eff;
  color: white;
  transform: translateX(5px);
}

/* ========== 返回區域 ========== */
.back-section {
  max-width: 900px;
  margin: 0 auto 30px;
  text-align: center;
}

/* ========== 提示卡片 ========== */
.tips-card {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #606266;
  font-size: 14px;
  font-weight: 500;
}

.tip-item i {
  color: #409eff;
  font-size: 18px;
}

/* ========== 響應式設計 ========== */
@media (max-width: 768px) {
  .type-cards {
    grid-template-columns: 1fr;
  }

  .header-text h1 {
    font-size: 1.5rem;
  }
}

/* ========== 輔助類 ========== */
.mr-2 {
  margin-right: 8px;
}
</style>
