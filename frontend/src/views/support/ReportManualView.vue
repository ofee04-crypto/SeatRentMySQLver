<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

// 根據 query.type 顯示不同內容
const problemType = computed(() => route.query.type || 'other')

// 標題和說明
const pageInfo = computed(() => {
  const info = {
    borrow: {
      title: '借還問題',
      icon: '📋',
      color: '#e6a23c',
      desc: '關於租借訂單、歸還異常等問題'
    },
    other: {
      title: '其他問題',
      icon: '❓',
      color: '#909399',
      desc: '其他類型的問題諮詢'
    }
  }
  return info[problemType.value] || info.other
})

// 返回客服支援中心
const goToSupport = () => {
  router.push('/support')
}

// 返回類型選擇
const goBack = () => {
  router.push('/support/report')
}
</script>

<template>
  <div class="manual-support-container">
    <!-- 頁面標題 -->
    <section class="page-header">
      <div class="container-fluid">
        <div class="header-content">
          <div class="header-icon" :style="{ background: pageInfo.color }">
            {{ pageInfo.icon }}
          </div>
          <div class="header-text">
            <h1>{{ pageInfo.title }}</h1>
            <p>{{ pageInfo.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 內容區域 -->
    <section class="content">
      <div class="container-fluid">
        <div class="info-card">
          <!-- 主要訊息 -->
          <div class="main-message">
            <i class="fas fa-info-circle message-icon"></i>
            <h2>需要人工協助</h2>
            <p>此類型問題需要客服人員協助處理，請透過以下方式與我們聯繫：</p>
          </div>

          <!-- 聯絡方式 -->
          <div class="contact-methods">
            <div class="contact-item">
              <div class="contact-icon phone">
                <i class="fas fa-phone"></i>
              </div>
              <div class="contact-info">
                <h3>電話聯繫</h3>
                <p class="contact-value">0800-123-456</p>
                <p class="contact-note">服務時間：週一至週五 09:00-18:00</p>
              </div>
            </div>

            <div class="contact-item">
              <div class="contact-icon email">
                <i class="fas fa-envelope"></i>
              </div>
              <div class="contact-info">
                <h3>Email 聯繫</h3>
                <p class="contact-value">support@seatrentsys.com</p>
                <p class="contact-note">我們會在 24 小時內回覆您</p>
              </div>
            </div>

            <div class="contact-item">
              <div class="contact-icon chat">
                <i class="fas fa-comments"></i>
              </div>
              <div class="contact-info">
                <h3>線上客服</h3>
                <p class="contact-value">返回客服支援中心使用 AI 智能客服</p>
                <p class="contact-note">即時線上諮詢服務</p>
              </div>
            </div>
          </div>

          <!-- 注意事項 -->
          <div class="notice-box">
            <h4><i class="fas fa-lightbulb"></i> 溫馨提醒</h4>
            <ul>
              <li>請準備好您的訂單編號或會員帳號，以便快速查詢</li>
              <li>詳細描述問題狀況，可加快處理速度</li>
              <li>如有相關照片或截圖，請一併提供</li>
            </ul>
          </div>

          <!-- 按鈕組 -->
          <div class="action-buttons">
            <el-button size="large" @click="goBack">
              <i class="fas fa-arrow-left"></i> 返回類型選擇
            </el-button>
            <el-button type="primary" size="large" @click="goToSupport">
              <i class="fas fa-home"></i> 返回客服支援中心
            </el-button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* ========== 頁面容器 ========== */
.manual-support-container {
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
  transition: all 0.3s ease;
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

.info-card {
  max-width: 900px;
  margin: 0 auto;
  background: white;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

/* ========== 主要訊息 ========== */
.main-message {
  text-align: center;
  margin-bottom: 40px;
  padding-bottom: 30px;
  border-bottom: 2px solid #f0f2f5;
}

.message-icon {
  font-size: 64px;
  color: #409eff;
  margin-bottom: 20px;
}

.main-message h2 {
  font-size: 1.8rem;
  font-weight: 700;
  color: #2c3e50;
  margin: 0 0 12px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.main-message p {
  font-size: 1rem;
  color: #606266;
  margin: 0;
  font-weight: 500;
}

/* ========== 聯絡方式 ========== */
.contact-methods {
  display: grid;
  gap: 24px;
  margin-bottom: 30px;
}

.contact-item {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  padding: 24px;
  background: #f8f9fa;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.contact-item:hover {
  background: #e8ecf1;
  transform: translateX(5px);
}

.contact-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  flex-shrink: 0;
}

.contact-icon.phone {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.contact-icon.email {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
}

.contact-icon.chat {
  background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
}

.contact-info {
  flex: 1;
}

.contact-info h3 {
  margin: 0 0 8px;
  font-size: 1.1rem;
  font-weight: 700;
  color: #2c3e50;
}

.contact-value {
  margin: 0 0 6px;
  font-size: 1.1rem;
  font-weight: 600;
  color: #409eff;
}

.contact-note {
  margin: 0;
  font-size: 0.85rem;
  color: #909399;
  font-weight: 500;
}

/* ========== 注意事項 ========== */
.notice-box {
  background: #fff9e6;
  border-left: 4px solid #e6a23c;
  padding: 20px 24px;
  border-radius: 8px;
  margin-bottom: 30px;
}

.notice-box h4 {
  margin: 0 0 12px;
  font-size: 1.05rem;
  font-weight: 700;
  color: #2c3e50;
  display: flex;
  align-items: center;
  gap: 8px;
}

.notice-box h4 i {
  color: #e6a23c;
}

.notice-box ul {
  margin: 0;
  padding-left: 20px;
  list-style: none;
}

.notice-box li {
  margin-bottom: 8px;
  font-size: 0.95rem;
  color: #606266;
  font-weight: 500;
  position: relative;
  padding-left: 16px;
}

.notice-box li:last-child {
  margin-bottom: 0;
}

.notice-box li::before {
  content: '•';
  position: absolute;
  left: 0;
  color: #e6a23c;
  font-weight: bold;
}

/* ========== 按鈕組 ========== */
.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.action-buttons .el-button {
  min-width: 180px;
}

.action-buttons i {
  margin-right: 6px;
}

/* ========== 響應式設計 ========== */
@media (max-width: 768px) {
  .info-card {
    padding: 24px;
  }

  .header-text h1 {
    font-size: 1.5rem;
  }

  .main-message h2 {
    font-size: 1.4rem;
  }

  .contact-item {
    flex-direction: column;
    text-align: center;
  }

  .action-buttons {
    flex-direction: column;
  }

  .action-buttons .el-button {
    width: 100%;
  }
}
</style>
