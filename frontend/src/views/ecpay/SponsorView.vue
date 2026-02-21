<template>
  <div class="payment-container">
    <div v-if="isSuccess" class="payment-card success-card animated fadeIn">
      <div class="hero-header">
        <el-icon class="success-icon" style="font-size: 60px; color: #2a9d8f;"><SuccessFilled /></el-icon>
        <h2 style="color: #2a9d8f;">贊助成功！</h2>
        <p>您的支持是我們前進的最大動力。</p>
      </div>
      <button @click="router.push('/')" class="checkout-btn">回到首頁</button>
    </div>

    <div v-else class="payment-card sponsor-card">
      <div class="hero-header">
        <el-icon class="star-icon"><StarFilled /></el-icon>
        <h2>支持我們的願景</h2>
        <p>您的支持是我們進步的最大動力</p>
      </div>
      <hr />
      <div class="amount-selector">
        <p class="section-title">選擇贊助金額：</p>
        <div class="price-tags">
          <button v-for="p in [100, 500, 1000]" :key="p" :class="{ active: amount === p }" @click="amount = p">
            ${{ p }}
          </button>
        </div>
        <div class="points-hint" style="margin: 10px 0; color: #e67e22; font-weight: bold;">
          <i class="bi bi-gift-fill"></i>
          感謝支持！本次贊助您將獲得 
          <span style="font-size: 1.2rem;">{{ amount }}</span> 
          點數回饋
        </div>
        <el-input-number v-model="amount" :min="10" :step="50" class="custom-input" />
      </div>
      <div class="payment-methods">
        <p class="section-title">支付平台：</p>
        <label class="method-option">
          <input type="radio" checked /> 
          <span class="radio-label">綠界科技 ECPay 安全支付</span>
        </label>
      </div>
      <div class="comment-area" style="margin-top: 20px;">
  <p class="section-title">給我們的鼓勵 (留言)：</p>
  <el-input
    v-model="comment"
    type="textarea"
    :rows="3"
    placeholder="寫下您想說的話..."
    maxlength="500"
    show-word-limit
    class="custom-textarea"
  />
</div>
<hr>
      <button @click="handleSponsorSubmit" :disabled="isLoading" class="checkout-btn sponsor-btn">
        <span v-if="isLoading" class="loader"></span>
        {{ isLoading ? '準備導向支付頁面...' : '立即贊助' }}
      </button>
      <p class="note">※ 點擊後將跳轉至綠界金流頁面，過程受加密保護</p>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import axios from 'axios';
import { StarFilled, SuccessFilled } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
// 💡 導入你的會員 Store
import { useMemberAuthStore } from '@/stores/memberAuth'; 

const isLoading = ref(false);
const isSuccess = ref(false);
const amount = ref(100);
const comment = ref(""); // 💡 留言板變數
const router = useRouter();
const memberAuthStore = useMemberAuthStore();

// 💡 統一使用 Store 的資料 (與蛇蛇遊戲一致)
const memberId = computed(() => memberAuthStore.member?.memId);

const messageHandler = (event) => {
  if (event.data === 'PAYMENT_SUCCESS') {
    isSuccess.value = true;
    isLoading.value = false;
    // 成功後順便刷新一下全站點數 (如果有需要的話)
    if (typeof memberAuthStore.refreshPoints === 'function') {
        memberAuthStore.refreshPoints();
    }
  }
};

onMounted(() => {
  window.addEventListener('message', messageHandler);
});

onUnmounted(() => {
  window.removeEventListener('message', messageHandler);
});

const handleSponsorSubmit = async () => {
  // 1. 檢查登入狀態
  if (!memberId.value) {
    alert("請先登入後再進行贊助，讓我們能記錄您的貢獻！");
    router.push({ path: '/login', query: { redirect: '/sponsor' } });
    return;
  }

  isLoading.value = true;
  const windowName = "ecpayPaymentWindow";
  const paymentWindow = window.open("", windowName);
  
  if (!paymentWindow) {
    alert("彈出視窗被瀏覽器封鎖，請允許本站開啟彈出視窗。");
    isLoading.value = false;
    return;
  }

  paymentWindow.document.write("<html><body style='display:flex;justify-content:center;align-items:center;height:100vh;font-family:sans-serif;'><div><h2>正在導向綠界支付...</h2></div></body></html>");

  try {
    const apiUrl = window.APP_CONFIG?.API_URL || 'http://localhost:8080';
    const frontendUrl = window.location.origin;

    // 💡 修正：將參數完整傳遞給整合後的後端
    const response = await axios.post(`${apiUrl}/api/payment/sponsor`, null, {
      params: { 
        memberId: memberId.value, // 從 computed 拿值
        amount: amount.value,
        comment: comment.value,   // 傳送留言板內容
        baseUrl: frontendUrl 
      }
    });

    const tempDiv = document.createElement('div');
    tempDiv.style.display = 'none';
    tempDiv.innerHTML = response.data;
    document.body.appendChild(tempDiv);

    const form = tempDiv.querySelector('form');
    if (form) {
      form.target = windowName; 
      form.submit();
      setTimeout(() => {
        if (document.body.contains(tempDiv)) document.body.removeChild(tempDiv);
      }, 500);
    }
  } catch (error) {
    console.error("導向失敗", error);
    if (paymentWindow) paymentWindow.close();
    alert("系統忙碌中，請稍後再試。");
    isLoading.value = false;
  }
};
</script>

<style scoped>
.payment-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 85vh;
  background-color: #f0f2f5;
}

.payment-card {
  background: white;
  padding: 2.5rem;
  border-radius: 16px;
  box-shadow: 0 10px 25px rgba(0,0,0,0.08);
  width: 100%;
  max-width: 420px;
}

.hero-header { text-align: center; margin-bottom: 1.5rem; }
.star-icon { font-size: 40px; color: #f4a261; margin-bottom: 10px; }
.section-title { font-weight: bold; margin-bottom: 10px; color: #333; }

.price-tags { display: flex; gap: 10px; margin-bottom: 15px; }
.price-tags button {
  flex: 1; padding: 12px; border: 1px solid #ddd; border-radius: 8px;
  background: white; cursor: pointer; transition: all 0.3s; font-weight: bold;
}
.price-tags button.active {
  background: #2a9d8f; color: white; border-color: #2a9d8f;
}

.custom-input { width: 100%; margin-top: 10px; }

.payment-methods { margin: 1.5rem 0; }
.method-option {
  display: flex; align-items: center; gap: 10px; padding: 12px;
  border: 1px solid #e0e0e0; border-radius: 8px; background: #fafafa;
}

.checkout-btn {
  width: 100%; padding: 1.2rem; background-color: #2a9d8f; color: white;
  border: none; border-radius: 8px; font-size: 1.2rem; font-weight: 600;
  cursor: pointer; transition: all 0.3s; display: flex;
  justify-content: center; align-items: center; gap: 10px;
}

.sponsor-btn { background-color: #e76f51; }
.sponsor-btn:hover { background-color: #d65d41; transform: translateY(-2px); }

.loader {
  border: 3px solid #f3f3f3; border-top: 3px solid #ffffff;
  border-radius: 50%; width: 18px; height: 18px; animation: spin 1s linear infinite;
}

.section-title {
  font-weight: bold;
  margin-bottom: 8px;
  color: #333;
  display: block;
  text-align: left;
}

.custom-textarea :deep(.el-textarea__inner) {
  border-radius: 8px;
  border: 1px solid #ddd;
  transition: border-color 0.3s;
}

.custom-textarea :deep(.el-textarea__inner:focus) {
  border-color: #2a9d8f;
}
@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
.note { font-size: 0.8rem; color: #888; text-align: center; margin-top: 1.2rem; }
</style>