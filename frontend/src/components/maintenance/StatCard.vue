<script setup>
/**
 * 統計卡片元件
 * 用於顯示數據統計，可自訂圖示、數值、標籤和樣式
 */
defineProps({
  // 圖示 class (FontAwesome)
  icon: {
    type: String,
    default: 'fas fa-chart-bar',
  },
  // 顯示的數值
  value: {
    type: [String, Number],
    required: true,
  },
  // 標籤說明
  label: {
    type: String,
    required: true,
  },
  // 卡片變體樣式：'primary' | 'success' | 'warning' | 'danger' | 'info'
  variant: {
    type: String,
    default: 'primary',
  },
  // 是否啟用脈衝動畫
  pulse: {
    type: Boolean,
    default: false,
  },
  // 是否可點擊
  clickable: {
    type: Boolean,
    default: false,
  },
  // 背景裝飾圖示 (可選)
  bgIcon: {
    type: String,
    default: '',
  },
})

// 點擊事件
const emit = defineEmits(['click'])

const handleClick = () => {
  emit('click')
}
</script>

<template>
  <div 
    class="stat-card" 
    :class="[`${variant}-card`, { clickable }]"
    @click="handleClick"
  >
    <div class="stat-icon" :class="{ pulse }">
      <i :class="icon"></i>
    </div>
    <div class="stat-info">
      <h3>{{ value }}</h3>
      <span>{{ label }}</span>
    </div>
    <!-- 背景裝飾圖示 -->
    <div v-if="bgIcon" class="stat-bg-icon">
      <i :class="bgIcon"></i>
    </div>
  </div>
</template>

<style scoped>
.stat-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  background: white;
  border-radius: 14px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.06);
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  overflow: hidden;
}

.stat-card.clickable {
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.12);
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: white;
  z-index: 1;
  flex-shrink: 0;
}

.stat-icon.pulse {
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

/* 變體樣式 */
.primary-card .stat-icon {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
}

.success-card .stat-icon {
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
}

.warning-card .stat-icon {
  background: linear-gradient(135deg, #e6a23c 0%, #f3d19e 100%);
}

.danger-card .stat-icon {
  background: linear-gradient(135deg, #f56c6c 0%, #f89898 100%);
}

.info-card .stat-icon {
  background: linear-gradient(135deg, #909399 0%, #c0c4cc 100%);
}

.stat-info {
  z-index: 1;
  min-width: 0;
}

.stat-info h3 {
  margin: 0;
  font-size: 1.8rem;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-info span {
  font-size: 0.85rem;
  color: #909399;
  white-space: nowrap;
}

/* 背景裝飾圖示 */
.stat-bg-icon {
  position: absolute;
  right: -10px;
  bottom: -10px;
  font-size: 80px;
  color: rgba(0, 0, 0, 0.03);
  z-index: 0;
}
</style>
