<script setup>
import { computed } from 'vue'

// 定義外部傳入的參數
const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  subtitle: {
    type: String,
    default: '',
  },
  // 預設圖示
  icon: {
    type: String,
    default: 'fas fa-cube',
  },
  // 模式：'add' (新增-綠色) | 'edit' (編輯-橘色) | 'view' (檢視/列表-藍色)
  mode: {
    type: String,
    default: 'view',
  },
})

// 根據模式決定 Icon 的 CSS class
const iconClass = computed(() => {
  switch (props.mode) {
    case 'add':
      return 'add-mode'
    case 'edit':
      return 'edit-mode'
    default:
      return 'view-mode' // 預設藍色
  }
})
</script>

<template>
  <div class="page-title-box">
    <div class="title-icon" :class="iconClass">
      <i :class="icon"></i>
    </div>
    <div class="title-content">
      <h1>{{ title }}</h1>
      <p v-if="subtitle" class="subtitle">
        {{ subtitle }}
      </p>
    </div>
  </div>
</template>

<style scoped>
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

/* 顏色定義 */
.add-mode {
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
}

.edit-mode {
  background: linear-gradient(135deg, #e6a23c 0%, #f3d19e 100%);
}

.view-mode {
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
</style>
