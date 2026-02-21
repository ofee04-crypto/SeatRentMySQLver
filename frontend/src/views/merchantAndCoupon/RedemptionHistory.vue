<template>
  <div class="history-page">
    <h2>我的兌換紀錄</h2>
    
    <el-table 
      v-loading="loading" 
      :data="logs" 
      border 
      stripe 
      style="width: 100%"
      empty-text="目前尚無兌換紀錄"
    >
      <el-table-column label="兌換時間" width="200" sortable>
        <template #default="scope">
          {{ formatDate(scope.row.redeemTime) }}
        </template>
      </el-table-column>

      <el-table-column prop="merchantName" label="合作商家" />
      <el-table-column prop="couponName" label="兌換項目" />
      
      <el-table-column label="消耗點數" width="120">
        <template #default="scope">
          <span style="color: #f56c6c; font-weight: bold;">
            - {{ scope.row.pointsSpent }} P
          </span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
<script setup>
import { ref, onMounted, computed } from 'vue';
import axios from 'axios';
// 💡 這裡路徑請根據你實際的檔案名稱（假設是 auth.js 或 memberAuth.js）
import { useMemberAuthStore } from '@/stores/memberAuth'; 

const authStore = useMemberAuthStore();
const logs = ref([]);
const loading = ref(false);

// 💡 修正：根據你的 Store 結構，從 member 物件中拿 memId
const memberId = computed(() => authStore.member?.memId);

const fetchLogs = async () => {
  // 防呆：確保 ID 存在且不是 null 字串
  if (!memberId.value) {
    console.warn("尚未登入或找不到 memId，取消請求");
    return;
  }

  loading.value = true;
  try {
    // 💡 發送請求到正確的路徑
    const url = `http://localhost:8080/api/discounts/member/${memberId.value}/logs`;
    console.log("正在請求會員紀錄，ID:", memberId.value);
    
    const res = await axios.get(url);
    logs.value = res.data;
    console.log("成功抓取紀錄，筆數:", logs.value.length);
  } catch (error) {
    console.error("抓取紀錄失敗:", error);
  } finally {
    loading.value = false;
  }
};

const formatDate = (dateStr) => {
  if (!dateStr) return '---';
  // 處理 ISO 8601 格式
  return dateStr.replace('T', ' ').substring(0, 19);
};

onMounted(fetchLogs);
</script>
<style scoped>
.history-page {
  padding: 20px;
}
</style>