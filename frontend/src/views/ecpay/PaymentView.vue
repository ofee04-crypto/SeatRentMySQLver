<template>
  <div class="payment-container">
    <div class="payment-card">
      <h2>租借訂單結帳</h2>
      <hr />
      
      <div class="order-info">
        <div class="info-item">
          <span>訂單編號：</span>
          <strong>{{ recId }}</strong>
        </div>
        <div class="info-item">
          <span>服務項目：</span>
          <strong>機台/座位租借</strong>
        </div>
        <p class="warning-text">※ 請確認金額後再前往付款</p>
      </div>

      <div class="payment-methods">
        <p>支付平台：</p>
        <label class="method-option">
          <input type="radio" checked /> 
          <span class="radio-label">綠界科技 ECPay 安全支付</span>
        </label>
      </div>

      <button 
        @click="handleCheckout" 
        :disabled="isLoading" 
        class="checkout-btn"
      >
        <span v-if="isLoading" class="loader"></span>
        {{ isLoading ? '導向支付頁面中...' : '前往付款' }}
      </button>

      <p class="note">※ 點擊按鈕後將離開本站，導向至綠界金流加密頁面</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import axios from 'axios';
import Swal from 'sweetalert2';

const route = useRoute();
const isLoading = ref(false);
const recId = ref('');

onMounted(() => {
  recId.value = route.params.recId;
  if (!recId.value) {
    Swal.fire('錯誤', '無效的訂單編號', 'error');
  }
});

const handleCheckout = async () => {
  if (!recId.value) return;

  isLoading.value = true;
  try {
    // 1. 呼叫後端 API
    const response = await axios.post(`http://localhost:8080/api/payment/checkout?recId=${recId.value}`);
    
    // 2. 處理回應
    const payHtml = response.data;
    if (!payHtml || (typeof payHtml === 'string' && payHtml.includes('Error'))) {
      throw new Error('訂單資訊有誤，無法付款');
    }

    // 💡 修正點 A：先移除 HTML 裡的 <script> 標籤，避免塞入 DOM 時觸發 CSP 報錯
    const cleanHtml = payHtml.replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, "");

    // 3. 建立一個臨時容器並插入純表單 HTML
    const div = document.createElement('div');
    div.style.display = 'none'; 
    div.innerHTML = cleanHtml; 
    document.body.appendChild(div);

    // 💡 修正點 B：直接抓取 form 並提交
    const form = div.querySelector('form');
    if (form) {
      console.log("表單準備就緒，執行提交...");
      form.submit();
    } else {
      throw new Error('找不到支付表單，請稍後再試');
    }

  } catch (error) {
    console.error('付款啟動失敗', error);
    Swal.fire({
      icon: 'error',
      title: '啟動支付失敗',
      text: error.message || '請檢查網路連線或聯絡客服。',
    });
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
.warning-text {
  font-size: 0.85rem;
  color: #856404;
  background-color: #fff3cd;
  padding: 8px;
  border-radius: 4px;
  margin-top: 10px;
}
.order-info {
  margin: 2rem 0;
}
.info-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 1rem;
}
.checkout-btn {
  width: 100%;
  padding: 1.2rem;
  background-color: #2a9d8f;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1.2rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
}
.checkout-btn:hover {
  background-color: #21867a;
  transform: translateY(-2px);
}
.checkout-btn:disabled {
  background-color: #a8dadc;
  cursor: not-allowed;
}
.loader {
  border: 3px solid #f3f3f3;
  border-top: 3px solid #1d3557;
  border-radius: 50%;
  width: 18px;
  height: 18px;
  animation: spin 1s linear infinite;
}
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
.note {
  font-size: 0.8rem;
  color: #666;
  text-align: center;
  margin-top: 1rem;
}
</style>