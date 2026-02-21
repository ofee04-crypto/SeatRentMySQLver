
<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import axios from 'axios';
import Swal from 'sweetalert2';

const route = useRoute();
const isLoading = ref(false);
const recId = ref('');
const returnSpotId=ref('');
const totalFee = ref(0); // [新增] 用於儲存總金額
onMounted(() => {
  // [修改] 從 route.query 獲取訂單ID與金額
  recId.value = route.query.recId;
  totalFee.value = route.query.total;
  returnSpotId.value = route.query.returnSpotId;
  if (!recId.value) {
    Swal.fire('錯誤', '無效的訂單編號', 'error');
  }
});

const handleCheckout = async () => {
  if (!recId.value) return;

  isLoading.value = true;
  try {
    // 1. 準備要送到後端的資料 (Content-Type: application/x-www-form-urlencoded)
    const params = new URLSearchParams();
    params.append('recId', recId.value);
    params.append('amount', totalFee.value);
    params.append('returnSpotId',returnSpotId);
    params.append('baseUrl', window.APP_CONFIG.API_URL);
    // 2. 呼叫後端 API
    const response = await axios.post('http://localhost:8080/api/payment/checkout', params);
    //const response = await axios.post(`${window.APP_CONFIG.API_URL}/api/payment/checkout`, params);
    // 3. 處理後端回傳的 HTML 字串
    const payHtml = response.data;
    if (!payHtml || (typeof payHtml === 'string' && payHtml.includes('Error'))) {
      throw new Error('訂單資訊有誤，無法付款');
    }

    // 3. 建立一個臨時容器但不立即插入頁面
    const div = document.createElement('div');
    div.innerHTML = payHtml;

    // 4. 從容器中尋找表單
    const form = div.querySelector('form');
    if (form) {
      
      div.style.display = 'none'; 
      document.body.appendChild(div);
      form.submit();
    } else {
      // 若回傳的 HTML 中找不到表單，則視為錯誤
      throw new Error('無法解析付款表單，請聯絡客服。');
    }
  } catch (error) {
    console.error('付款啟動失敗', error);
    Swal.fire({
      icon: 'error',
      title: '啟動支付失敗',
      text: error.message || '請檢查網路連線或聯絡客服。',
    });
    // 發生錯誤時，確保 loading 狀態被重置
    isLoading.value = false;
  } finally {
    // 導轉成功時，此組件可能已被銷毀，但為防萬一，短時間後重置按鈕
    setTimeout(() => { 
      // 檢查 isLoading 是否仍為 true，避免覆蓋 catch 中的 false
      if(isLoading.value) isLoading.value = false; 
    }, 3000);
  }
};
</script>

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
        <!-- [新增] 顯示費用總計 -->
        <div class="info-item">
          <span>費用總計：</span>
          <strong>{{ totalFee }} NTD</strong>
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