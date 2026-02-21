<template>
  <div class="redirect-container">
    <el-result icon="success" title="支付處理中" sub-title="正在同步贊助狀態，請勿關閉視窗...">
      <template #extra>
        <el-icon class="is-loading" style="font-size: 30px;"><Loading /></el-icon>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { Loading } from '@element-plus/icons-vue';

onMounted(() => {
  console.log("💳 支付完成中轉頁已啟動");

  const notifyAndClose = () => {
    // 💡 核心邏輯：向開啟這個視窗的父視窗發送訊息
    if (window.opener) {
      window.opener.postMessage('PAYMENT_SUCCESS', '*');
      console.log("已通知主視窗");
    }
    
    // 如果是在 iframe 內執行
    if (window.parent && window.parent !== window) {
      window.parent.postMessage('PAYMENT_SUCCESS', '*');
      console.log("已通知父層 iframe");
    }

    // 延遲關閉視窗，讓使用者稍微看到成功狀態
    setTimeout(() => {
      if (window.opener) {
        window.close();
      }
    }, 2500);
  };

  notifyAndClose();
});
</script>

<style scoped>
.redirect-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 60vh; /* 在 MainLayout 中居中 */
}
.is-loading {
  color: #2a9d8f;
}
</style>