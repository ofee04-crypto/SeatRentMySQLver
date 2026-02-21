<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { faqData } from '@/data/supportFaq'
import { useCozeChat } from '@/composables/maintenance/useCozeChat'

const router = useRouter()

// ==================== BEGIN: Coze OpenAPI  ====================
const {
  loading: cozeLoading, // 是否正在載入
  initialized: cozeInitialized, // 是否已初始化完成
  error: cozeError, // 初始化錯誤訊息
  degraded: cozeDegraded, // 是否處於降級模式
  status: cozeStatus, // 目前狀態
  statusText: cozeStatusText, // 目前狀態說明
  retryCount, // 重試次數
  messages, // 對話訊息列表
  sending, // 正在發送訊息
  initCozeChat, // 初始化函式
  sendMessage, // 發送訊息
  manualRetry, // 手動重試
  clearMessages, // 清除訊息
} = useCozeChat()

// 聊天視窗狀態
const chatOpen = ref(false)
const chatInput = ref('')
const messagesContainer = ref(null)

//導頁使用
const pendingIntent = ref(null)

// 開啟聊天視窗
const openChatWindow = () => {
  chatOpen.value = true
  nextTick(() => scrollToBottom())
}

// 關閉聊天視窗
const closeChatWindow = () => {
  chatOpen.value = false
}

// 發送訊息
const handleSendMessage = async () => {
  if (!chatInput.value.trim() || sending.value) return

  const message = chatInput.value.trim()
  chatInput.value = ''

  await sendMessage(message)
  nextTick(() => scrollToBottom())
}

// Enter 送出
const handleKeydown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSendMessage()
  }
}

// 捲動到最新訊息
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 清除對話
const handleClearChat = () => {
  clearMessages()
}

// 格式化時間
const formatTime = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return d.toLocaleTimeString('zh-TW', { hour: '2-digit', minute: '2-digit' })
}

// Quick Questions 統一走這個方法（避免 template 多行語句爆炸）
const sendQuickQuestion = async (text) => {
  if (sending.value) return
  chatInput.value = text
  await handleSendMessage()
}
// ==================== END: Coze OpenAPI 整合 ====================

// ==================== FAQ 相關狀態 ====================
const searchKeyword = ref('')
const activeCategory = ref(faqData[0]?.category || '')
const activeFaqItems = ref([])

/**
 * 【核心功能】搜尋過濾：同時搜尋標題、內容、標籤、關鍵字
 */
const filteredFaqData = computed(() => {
  const keyword = searchKeyword.value.toLowerCase().trim()

  if (!keyword) {
    const currentCategory = faqData.find((cat) => cat.category === activeCategory.value)
    return currentCategory ? [currentCategory] : []
  }

  return faqData
    .map((category) => ({
      ...category,
      items: category.items.filter((item) => {
        const qMatch = item.q.toLowerCase().includes(keyword)
        const aMatch = item.a.toLowerCase().includes(keyword)
        const tagMatch = item.tags?.some((tag) => tag.toLowerCase().includes(keyword))
        const keywordMatch = item.keywords?.some((kw) => kw.toLowerCase().includes(keyword))
        return qMatch || aMatch || tagMatch || keywordMatch
      }),
    }))
    .filter((cat) => cat.items.length > 0)
})

/**
 * 切換分類
 */
const handleCategoryChange = (category) => {
  activeCategory.value = category
  activeFaqItems.value = []
  searchKeyword.value = ''
}

/**
 * 【導航】前往問題回報頁面
 */
const goToReport = () => {
  router.push('/support/report')
}

/**
 * 【統計】總 FAQ 數量
 */
const totalFaqCount = computed(() => {
  return faqData.reduce((sum, cat) => sum + cat.items.length, 0)
})

// ==================== BEGIN: Coze 狀態標籤配置 ====================
const cozeTagConfig = computed(() => {
  const configs = {
    loading: { type: 'warning', icon: 'fa-spinner fa-spin', text: '初始化中' },
    ready: { type: 'success', icon: 'fa-check-circle', text: '已就緒' },
    degraded: { type: 'danger', icon: 'fa-exclamation-triangle', text: '服務暫時不可用' },
    error: { type: 'danger', icon: 'fa-times-circle', text: '初始化失敗' },
    idle: { type: 'info', icon: 'fa-clock', text: '未啟動' },
  }
  return configs[cozeStatus.value] || configs.idle
})
// ==================== END: Coze 狀態標籤配置 ====================

// ==================== BEGIN: 降級模式按鈕處理 ====================
const handleRetryCoze = async () => {
  await manualRetry()
}

const handleOpenChat = () => {
  if (cozeDegraded.value) {
    // 降級模式下，不執行開啟（UI 會引導到人工客服）
    return
  }
  openChatWindow()
}
// ==================== END: 降級模式按鈕處理 ====================

// ==================== BEGIN: 生命週期 ====================

let navigateHandler = null

onMounted(() => {
  // 延遲 500ms 後初始化，避免阻塞頁面渲染
  setTimeout(() => {
    initCozeChat()
  }, 500)

  // 2) 接收 useCozeChat dispatch 的「導頁事件」（C 方案）
  navigateHandler = (e) => {
    const intent = e?.detail || null
    pendingIntent.value = intent

    // 把 intent 存起來，讓 /support/report 頁面拿來預填
    try {
      sessionStorage.setItem('support_report_intent', JSON.stringify(intent))
    } catch (err) {
      // 失敗也不影響導頁
      console.warn('store intent failed', err)
    }

    //導頁
    router.push('/support/report')
  }

  window.addEventListener('support:navigate-report', navigateHandler)
})

onUnmounted(() => {
  // 關閉聊天視窗
  chatOpen.value = false

  // 移除事件監聽（避免記憶體洩漏 / 重複導頁）
  if (navigateHandler) {
    window.removeEventListener('support:navigate-report', navigateHandler)
    navigateHandler = null
  }
})

// ==================== END: 生命週期 ====================
</script>

<template>
  <div class="support-center-container">
    <!-- Hero 區域 -->
    <section class="hero-section">
      <div class="hero-content">
        <div class="hero-icon">
          <i class="fas fa-life-ring"></i>
        </div>
        <h1 class="hero-title">客服支援中心</h1>
        <p class="hero-subtitle">我們隨時為您提供協助</p>

        <!-- 搜尋框 -->
        <div class="search-wrapper">
          <el-input
            v-model="searchKeyword"
            placeholder="搜尋常見問題、關鍵字..."
            size="large"
            clearable
            class="search-input"
          >
            <template #prefix>
              <i class="fas fa-search"></i>
            </template>
          </el-input>
        </div>

        <div class="hero-stats">
          <span><i class="fas fa-book-open"></i> {{ totalFaqCount }} 個常見問題</span>
          <span><i class="fas fa-clock"></i> 24/7 全天候服務</span>
        </div>
      </div>
    </section>

    <!-- 主要內容區 -->
    <section class="content-section">
      <div class="container-fluid">
        <!-- 分類 Tabs -->
        <div class="category-tabs" v-if="!searchKeyword">
          <div
            v-for="category in faqData"
            :key="category.category"
            class="category-tab"
            :class="{ active: activeCategory === category.category }"
            @click="handleCategoryChange(category.category)"
          >
            <i :class="category.icon" :style="{ color: category.color }"></i>
            <span>{{ category.category }}</span>
            <el-badge :value="category.items.length" type="info" />
          </div>
        </div>

        <!-- 搜尋結果提示 -->
        <div v-if="searchKeyword" class="search-result-hint">
          <i class="fas fa-filter"></i>
          搜尋「<b>{{ searchKeyword }}</b
          >」共找到
          <span class="result-count">{{
            filteredFaqData.reduce((sum, cat) => sum + cat.items.length, 0)
          }}</span>
          筆結果
          <el-button text type="primary" size="small" @click="searchKeyword = ''">
            <i class="fas fa-times"></i> 清除搜尋
          </el-button>
        </div>

        <!-- FAQ 列表 -->
        <div class="faq-list">
          <div
            v-for="category in filteredFaqData"
            :key="category.category"
            class="faq-category-block"
          >
            <!-- 分類標題（僅在搜尋模式顯示） -->
            <div v-if="searchKeyword" class="category-title">
              <i :class="category.icon" :style="{ color: category.color }"></i>
              {{ category.category }}
            </div>

            <!-- FAQ Accordion -->
            <el-collapse v-model="activeFaqItems" accordion>
              <el-collapse-item
                v-for="(item, index) in category.items"
                :key="`${category.category}-${index}`"
                :name="`${category.category}-${index}`"
                class="faq-item"
              >
                <template #title>
                  <div class="faq-question">
                    <i class="fas fa-question-circle"></i>
                    <span v-html="item.q"></span>
                  </div>
                </template>
                <div class="faq-answer" v-html="item.a"></div>
                <div class="faq-tags" v-if="item.tags && item.tags.length">
                  <el-tag
                    v-for="tag in item.tags"
                    :key="tag"
                    size="small"
                    type="info"
                    effect="plain"
                  >
                    {{ tag }}
                  </el-tag>
                </div>
              </el-collapse-item>
            </el-collapse>
          </div>

          <!-- 無結果提示 -->
          <el-empty
            v-if="filteredFaqData.length === 0"
            description="找不到相關問題"
            :image-size="120"
          >
            <template #image>
              <i class="fas fa-search" style="font-size: 80px; color: #dcdfe6"></i>
            </template>
            <el-button type="primary" @click="goToReport">
              <i class="fas fa-exclamation-circle mr-1"></i> 直接回報問題
            </el-button>
          </el-empty>
        </div>

        <!-- CTA 按鈕區 -->
        <div class="cta-section">
          <!-- AI 客服卡片 -->
          <div class="cta-card cta-card-ai" :class="{ 'cta-card-degraded': cozeDegraded }">
            <div class="cta-icon">
              <i class="fas fa-robot"></i>
            </div>
            <div class="cta-content">
              <h3>
                試試 AI 智能客服
                <el-tag :type="cozeTagConfig.type" size="small" effect="dark">
                  <i :class="['fas', cozeTagConfig.icon]"></i> {{ cozeTagConfig.text }}
                </el-tag>
              </h3>

              <!-- 正常狀態提示 -->
              <p v-if="!cozeDegraded && !cozeError">
                即時解答您的疑問，24/7 全天候服務，請點擊右下角聊天圖示開始對話
              </p>

              <!-- 降級狀態提示 -->
              <div v-if="cozeDegraded" class="degraded-notice">
                <p class="degraded-text">
                  <i class="fas fa-exclamation-triangle"></i>
                  Coze 服務暫時不可用（可能是網路問題或服務端維護）
                </p>
                <p class="degraded-hint">您可以嘗試重新連線，或使用下方人工客服管道</p>
                <div class="degraded-actions">
                  <el-button
                    type="warning"
                    size="small"
                    :loading="cozeLoading"
                    @click="handleRetryCoze"
                  >
                    <i class="fas fa-redo mr-1" v-if="!cozeLoading"></i>
                    {{ cozeLoading ? '重試中...' : '重新連線' }}
                    <span v-if="retryCount > 0" class="retry-count">({{ retryCount }}/3)</span>
                  </el-button>
                </div>
              </div>

              <!-- 錯誤狀態提示 -->
              <div v-else-if="cozeError" class="error-notice">
                <p class="error-text">
                  <i class="fas fa-times-circle"></i>
                  初始化失敗：{{ cozeError }}
                </p>
                <el-button
                  type="primary"
                  size="small"
                  :loading="cozeLoading"
                  @click="handleRetryCoze"
                >
                  <i class="fas fa-redo mr-1" v-if="!cozeLoading"></i>
                  重試
                </el-button>
              </div>
            </div>

            <!-- 正常按鈕 -->
            <el-button
              v-if="!cozeDegraded && !cozeError"
              type="success"
              size="large"
              :disabled="!cozeInitialized"
              :loading="cozeLoading"
              @click="handleOpenChat"
            >
              <i class="fas fa-comments mr-2" v-if="!cozeLoading"></i>
              {{ cozeInitialized ? '開始對話' : cozeLoading ? '載入中...' : '初始化中' }}
            </el-button>
          </div>

          <!-- 問題回報卡片（人工客服入口） -->
          <div class="cta-card cta-card-report">
            <div class="cta-icon">
              <i class="fas fa-headset"></i>
            </div>
            <div class="cta-content">
              <h3>
                人工客服協助
                <!-- 降級時強調此入口 -->
                <el-tag v-if="cozeDegraded" type="success" size="small" effect="dark">
                  <i class="fas fa-star"></i> 推薦
                </el-tag>
              </h3>
              <p>
                {{
                  cozeDegraded
                    ? 'AI 客服暫時不可用，請透過此管道聯繫我們，我們會盡快為您處理'
                    : '若以上內容無法解決您的問題，請填寫問題回報表單，我們會盡快為您處理'
                }}
              </p>
            </div>
            <el-button
              :type="cozeDegraded ? 'success' : 'primary'"
              size="large"
              @click="goToReport"
            >
              <i class="fas fa-paper-plane mr-2"></i> 我要回報問題
            </el-button>
          </div>
        </div>

        <!-- 聯繫方式 -->
        <div class="contact-info">
          <div class="contact-item">
            <i class="fas fa-phone"></i>
            <span>客服專線：0968-179-091</span>
          </div>
          <div class="contact-item">
            <i class="fas fa-envelope"></i>
            <span>Email：support@seatrentsys.com</span>
          </div>
          <div class="contact-item">
            <i class="fas fa-clock"></i>
            <span>服務時間：24/7 全天候</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ==================== BEGIN: 自製聊天泡泡 UI（OpenAPI 模式） ==================== -->
    <!-- 右下角浮動聊天按鈕 -->
    <div
      v-if="cozeInitialized && !cozeDegraded && !chatOpen"
      class="chat-fab"
      @click="openChatWindow"
    >
      <i class="fas fa-comments"></i>
      <span class="fab-tooltip">AI 智能客服</span>
    </div>

    <!-- 聊天視窗 -->
    <transition name="chat-slide">
      <div v-if="chatOpen" class="chat-window">
        <!-- 聊天視窗標題列 -->
        <div class="chat-header">
          <div class="chat-header-info">
            <i class="fas fa-robot"></i>
            <span>Take@Seat 智能客服</span>
            <el-tag size="small" type="success" effect="plain">
              <i class="fas fa-circle" style="font-size: 6px; margin-right: 4px"></i>
              線上
            </el-tag>
          </div>
          <div class="chat-header-actions">
            <el-button text circle size="small" @click="handleClearChat" title="清除對話">
              <i class="fas fa-trash-alt"></i>
            </el-button>
            <el-button text circle size="small" @click="closeChatWindow" title="關閉">
              <i class="fas fa-times"></i>
            </el-button>
          </div>
        </div>

        <!-- 聊天訊息區 -->
        <div class="chat-messages" ref="messagesContainer">
          <!-- 歡迎訊息 -->
          <div v-if="messages.length === 0" class="chat-welcome">
            <div class="welcome-icon">
              <i class="fas fa-hand-sparkles"></i>
            </div>
            <h4>您好！我是 AI 智能客服</h4>
            <p>有任何關於座位租賃的問題，歡迎詢問我！</p>
            <div class="quick-questions">
              <el-button size="small" @click="sendQuickQuestion('付款方式有哪些？')">
                付款方式有哪些？
              </el-button>

              <el-button size="small" @click="sendQuickQuestion('如何使用租借功能？')">
                如何使用租借功能？
              </el-button>

              <el-button size="small" @click="sendQuickQuestion('如何換取點數？')">
                如何換取點數？
              </el-button>
            </div>
          </div>

          <!-- 訊息列表 -->
          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="chat-message"
            :class="{
              'message-user': msg.role === 'user',
              'message-assistant': msg.role === 'assistant',
              'message-error': msg.isError,
            }"
          >
            <div class="message-avatar">
              <i :class="msg.role === 'user' ? 'fas fa-user' : 'fas fa-robot'"></i>
            </div>
            <div class="message-content">
              <div class="message-bubble">{{ msg.content }}</div>
              <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
            </div>
          </div>

          <!-- 正在輸入指示器 -->
          <div v-if="sending" class="chat-message message-assistant">
            <div class="message-avatar">
              <i class="fas fa-robot"></i>
            </div>
            <div class="message-content">
              <div class="message-bubble typing-indicator">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>

        <!-- 聊天輸入區 -->
        <div class="chat-input-area">
          <el-input
            v-model="chatInput"
            placeholder="輸入訊息..."
            :disabled="sending"
            @keydown="handleKeydown"
            clearable
          >
            <template #append>
              <el-button
                type="primary"
                :loading="sending"
                :disabled="!chatInput.trim()"
                @click="handleSendMessage"
              >
                <i class="fas fa-paper-plane" v-if="!sending"></i>
              </el-button>
            </template>
          </el-input>
          <div class="chat-input-hint">
            按 Enter 送出 ·
            <a href="#" @click.prevent="goToReport">轉人工客服</a>
          </div>
        </div>
      </div>
    </transition>
    <!-- ==================== END: 自製聊天泡泡 UI ==================== -->
  </div>
</template>

<style scoped>
/* ========== 頁面容器 ========== */
.support-center-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);

  /*藍色漸層系列（更淡）*/
  --support-brand-1: #d4e3ee;
  --support-brand-2: #c8d9e6;

  /*常用的藍色點綴*/
  --support-accent-1: #60a5fa;
  --support-accent-2: #3b82f6;
}

/* ========== Hero 區域 ========== */
.hero-section {
  background: linear-gradient(135deg, var(--support-brand-1) 0%, var(--support-brand-2) 100%);
  padding: 80px 20px 60px;
  color: white;
  position: relative;
  overflow: hidden;
}

.hero-section::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -10%;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 70%);
  border-radius: 50%;
}

.hero-content {
  max-width: 800px;
  margin: 0 auto;
  text-align: center;
  position: relative;
  z-index: 1;
}

.hero-icon {
  font-size: 64px;
  margin-bottom: 20px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.hero-title {
  font-size: 2.5rem;
  font-weight: 800;
  margin: 0 0 10px;
  color: #2c3e50;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.15), 0 1px 3px rgba(255, 255, 255, 0.5);
}

.hero-subtitle {
  font-size: 1.1rem;
  font-weight: 600;
  opacity: 1;
  margin-bottom: 30px;
  color: #34495e;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.1), 0 1px 2px rgba(255, 255, 255, 0.3);
}

.search-wrapper {
  max-width: 600px;
  margin: 0 auto 20px;
}

.search-input {
  border-radius: 50px;
  overflow: hidden;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 50px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  padding: 0 20px;
}

.hero-stats {
  display: flex;
  justify-content: center;
  gap: 30px;
  font-size: 0.9rem;
  font-weight: 600;
  opacity: 1;
  color: #34495e;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.1), 0 1px 2px rgba(255, 255, 255, 0.3);
}

.hero-stats span {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ========== 內容區域 ========== */
.content-section {
  padding: 40px 20px;
}

/* ========== 分類 Tabs ========== */
.category-tabs {
  display: flex;
  gap: 15px;
  justify-content: center;
  max-width: 900px;
  margin: 0 auto 30px;
  overflow-x: auto;
  padding: 0 8px 10px;
  -webkit-overflow-scrollong: touch;
}

.category-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: white;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  white-space: nowrap;
  font-weight: 600;
}

.category-tab:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.category-tab.active {
  background: linear-gradient(135deg, rgba(212, 227, 238, 0.3) 0%, rgba(200, 217, 230, 0.3) 100%);
  color: #2c5282;
  border: 2px solid var(--support-brand-2);
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.2);
  font-weight: 700;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.5);
}

.category-tab.active i {
  color: #2c5282 !important;
}

/* ========== 搜尋結果提示 ========== */
.search-result-hint {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 15px 20px;
  background: #ecf5ff;
  border-radius: 12px;
  margin-bottom: 20px;
  color: #409eff;
  font-size: 14px;
}

.result-count {
  font-weight: 700;
  color: #409eff;
}

/* ========== FAQ 列表 ========== */
.faq-list {
  max-width: 900px;
  margin: 0 auto;
}

.faq-category-block {
  margin-bottom: 30px;
}

.category-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.2rem;
  font-weight: 600;
  margin-bottom: 15px;
  color: #303133;
}

.faq-item {
  margin-bottom: 10px;
}

.faq-item :deep(.el-collapse-item__header) {
  background: white;
  border-radius: 12px;
  padding: 20px;
  font-size: 15px;
  transition: all 0.3s ease;
}

.faq-item :deep(.el-collapse-item__header:hover) {
  background: #f5f7fa;
  transform: translateX(5px);
}

.faq-item :deep(.el-collapse-item__wrap) {
  border: none;
  background: white;
  border-radius: 0 0 12px 12px;
}

.faq-item :deep(.el-collapse-item__content) {
  padding: 20px;
  border-top: 1px solid #ebeef5;
}

.faq-question {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #303133;
  font-weight: 600;
}

.faq-question i {
  color: #409eff;
  font-size: 18px;
}

.faq-answer {
  color: #606266;
  line-height: 1.8;
}

.faq-answer :deep(ol),
.faq-answer :deep(ul) {
  margin: 10px 0;
}

.faq-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px dashed #ebeef5;
}

/* ========== CTA 區域 ========== */
.cta-section {
  margin: 60px 0 40px;
  max-width: 900px;
  margin-left: auto;
  margin-right: auto;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.cta-card {
  padding: 40px;
  border-radius: 20px;
  color: white;
  text-align: center;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  transition: transform 0.3s ease;
}

.cta-card:hover {
  transform: translateY(-5px);
}

.cta-card-ai {
  background: linear-gradient(135deg, #a8dba8 0%, #7ec8e3 100%);
}

.cta-card-ai .cta-content h3 {
  color: #1a4d2e;
  text-shadow:
    0 2px 8px rgba(0, 0, 0, 0.25),
    0 1px 3px rgba(255, 255, 255, 0.3);
}

.cta-card-ai .cta-content p {
  color: #2d5f4f;
  font-weight: 600;
  text-shadow:
    0 1px 4px rgba(0, 0, 0, 0.2),
    0 1px 2px rgba(255, 255, 255, 0.2);
}

/* 降級模式樣式 */
.cta-card-degraded {
  background: linear-gradient(135deg, #909399 0%, #606266 100%);
}

.cta-card-report {
  background: linear-gradient(135deg, var(--support-brand-1) 0%, var(--support-brand-2) 100%);
}

.cta-card-report .cta-content h3 {
  color: #1a3a52;
  text-shadow:
    0 2px 8px rgba(0, 0, 0, 0.25),
    0 1px 3px rgba(255, 255, 255, 0.3);
}

.cta-card-report .cta-content p {
  color: #2c5282;
  font-weight: 600;
  text-shadow:
    0 1px 4px rgba(0, 0, 0, 0.2),
    0 1px 2px rgba(255, 255, 255, 0.2);
}

.cta-icon {
  font-size: 48px;
  margin-bottom: 20px;
}

.cta-content h3 {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0 0 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.cta-content p {
  font-weight: 500;
  opacity: 0.95;
  margin-bottom: 25px;
  font-size: 0.95rem;
  line-height: 1.6;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.cta-card .el-button {
  background: white;
  border: none;
  font-weight: 700;
  padding: 15px 40px;
  border-radius: 50px;
  transition: all 0.3s ease;
}

.cta-card-ai .el-button {
  color: #67c23a;
}

.cta-card-degraded .el-button {
  color: #606266;
}

.cta-card-report .el-button {
  color: #2c5282;
  font-weight: 700;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.cta-card .el-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

/* 降級提示樣式 */
.degraded-notice,
.error-notice {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  padding: 15px;
  margin-bottom: 20px;
  text-align: left;
}

.degraded-text,
.error-text {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 10px;
  font-weight: 600;
}

.degraded-hint {
  font-size: 0.85rem;
  opacity: 0.9;
  margin: 0 0 15px;
}

.degraded-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-start;
}

.retry-count {
  margin-left: 5px;
  font-size: 0.8em;
  opacity: 0.8;
}

/* ========== 聯繫方式 ========== */
.contact-info {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 30px;
  padding: 30px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  max-width: 900px;
  margin: 0 auto;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #606266;
  font-size: 14px;
}

.contact-item i {
  color: #409eff;
  font-size: 18px;
}

/* ========== 響應式設計 ========== */
@media (max-width: 768px) {
  .hero-title {
    font-size: 1.8rem;
  }

  .category-tabs {
    justify-content: flex-start;
  }

  .cta-section {
    grid-template-columns: 1fr;
  }

  .cta-card {
    padding: 30px 20px;
  }

  .contact-info {
    flex-direction: column;
    align-items: flex-start;
  }
}

/* ========== 輔助類 ========== */
.mr-1 {
  margin-right: 4px;
}
.mr-2 {
  margin-right: 8px;
}

/* ==================== BEGIN: 聊天泡泡 UI 樣式（OpenAPI 模式） ==================== */

/* 右下角浮動按鈕 */
.chat-fab {
  position: fixed;
  bottom: 30px;
  right: 30px;
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #67c23a 0%, #409eff 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(64, 158, 255, 0.4);
  transition: all 0.3s ease;
  z-index: 1000;
}

.chat-fab:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 28px rgba(64, 158, 255, 0.5);
}

.chat-fab i {
  font-size: 24px;
  color: white;
}

.chat-fab .fab-tooltip {
  position: absolute;
  right: 70px;
  background: #303133;
  color: white;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 13px;
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.3s ease;
}

.chat-fab:hover .fab-tooltip {
  opacity: 1;
}

/* 聊天視窗 */
.chat-window {
  position: fixed;
  bottom: 30px;
  right: 30px;
  width: 380px;
  height: 550px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  z-index: 1001;
}

/* 聊天視窗動畫 */
.chat-slide-enter-active,
.chat-slide-leave-active {
  transition: all 0.3s ease;
}

.chat-slide-enter-from,
.chat-slide-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}

/* 聊天標題列 */
.chat-header {
  background: linear-gradient(135deg, var(--support-brand-1) 0%, var(--support-brand-2) 100%);
  color: white;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chat-header-info {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  color: #2c3e50;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.15), 0 1px 2px rgba(255, 255, 255, 0.3);
}

.chat-header-info i {
  font-size: 20px;
}

.chat-header-actions .el-button {
  color: white;
}

.chat-header-actions .el-button:hover {
  background: rgba(255, 255, 255, 0.2);
}

/* 聊天訊息區 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f7fa;
}

/* 歡迎訊息 */
.chat-welcome {
  text-align: center;
  padding: 30px 20px;
}

.chat-welcome .welcome-icon {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, var(--support-brand-1) 0%, var(--support-brand-2) 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 15px;
}

.chat-welcome .welcome-icon i {
  font-size: 28px;
  color: white;
}

.chat-welcome h4 {
  margin: 0 0 8px;
  color: #2c3e50;
  font-weight: 700;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.chat-welcome p {
  margin: 0 0 20px;
  color: #606266;
  font-size: 14px;
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.chat-welcome .quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.chat-welcome .quick-questions .el-button {
  font-size: 12px;
}

/* 訊息氣泡 */
.chat-message {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.chat-message.message-user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message-user .message-avatar {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: white;
}

.message-assistant .message-avatar {
  background: linear-gradient(135deg, var(--support-brand-1) 0%, var(--support-brand-2) 100%);
  color: white;
}

.message-content {
  max-width: 70%;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

.message-user .message-bubble {
  background: #409eff;
  color: white;
  border-bottom-right-radius: 4px;
}

.message-assistant .message-bubble {
  background: white;
  color: #303133;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.message-error .message-bubble {
  background: #fef0f0;
  color: #f56c6c;
}

.message-time {
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
  padding: 0 4px;
}

.message-user .message-time {
  text-align: right;
}

/* 正在輸入指示器 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #c0c4cc;
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) {
  animation-delay: 0s;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%,
  60%,
  100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-8px);
  }
}

/* 聊天輸入區 */
.chat-input-area {
  padding: 12px 16px;
  background: white;
  border-top: 1px solid #ebeef5;
}

.chat-input-area .el-input {
  border-radius: 24px;
}

.chat-input-area .el-input :deep(.el-input__wrapper) {
  border-radius: 24px 0 0 24px;
}

.chat-input-area .el-input :deep(.el-input-group__append) {
  border-radius: 0 24px 24px 0;
  padding: 0;
}

.chat-input-area .el-input :deep(.el-input-group__append .el-button) {
  border-radius: 0 24px 24px 0;
  margin: 0;
  height: 100%;
}

.chat-input-hint {
  font-size: 11px;
  color: #909399;
  text-align: center;
  margin-top: 8px;
}

.chat-input-hint a {
  color: #409eff;
  text-decoration: none;
}

.chat-input-hint a:hover {
  text-decoration: underline;
}

/* 響應式 - 手機版 */
@media (max-width: 480px) {
  .chat-window {
    bottom: 0;
    right: 0;
    width: 100%;
    height: 100%;
    border-radius: 0;
  }

  .chat-fab {
    bottom: 20px;
    right: 20px;
  }
}

/* ==================== END: 聊天泡泡 UI 樣式 ==================== */
</style>
