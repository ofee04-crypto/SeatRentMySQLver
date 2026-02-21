<script setup>
import { RouterView } from 'vue-router'

// 粒子特效設定
const particlesOptions = {
  fullScreen: { enable: false },
  background: {
    color: { value: 'transparent' },
  },
  particles: {
    color: { value: '#ffffff' },
    links: {
      enable: true,
      color: '#ffffff',
      distance: 150,
      opacity: 0.3,
      width: 1,
    },
    move: {
      enable: true,
      speed: 0.6,
      direction: 'none',
      random: true,
      straight: false,
      outMode: {
        default: 'bounce',
      },
    },
    number: {
      value: 60,
      density: {
        enable: true,
        area: 800,
      },
    },
    opacity: {
      value: 0.5,
    },
    shape: {
      type: 'circle',
    },
    size: {
      value: { min: 1, max: 3 },
    },
  },
  interactivity: {
    events: {
      onHover: {
        enable: true,
        mode: 'grab',
      },
      onClick: {
        enable: true,
        mode: 'push',
      },
    },
    modes: {
      grab: {
        distance: 140,
        links: {
          opacity: 0.8,
        },
      },
    },
  },
}
</script>

<template>
  <div class="auth-layout">
    <div class="auth-background"></div>

    <vue-particles id="tsparticles" :options="particlesOptions" class="particles-layer" />

    <div class="auth-container">
      <header class="auth-header animate-fade-down">
        <h1 class="logo-text">SeatRentSys</h1>
      </header>

      <main class="auth-content glass-card-wrapper animate-pop-in">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>

      <footer class="auth-footer">
        <p>&copy;</p>
      </footer>
    </div>
  </div>
</template>

<style scoped>
/* 版面容器*/
.auth-layout {
  position: relative;
  /* 修正：改用 min-height 並移除溢出隱藏/自動設定，讓外層自然捲動 */
  min-height: 100vh; 
  width: 100%;
  
  display: flex;
  justify-content: center;
  /* 修正：使用 flex-start 配合 padding，確保長表單頂部不被切掉 */
  align-items: flex-start; 
  
  /* 針對各種裝置的呼吸空間 */
  padding: 60px 15px; 
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  
  /* 確保背景層不會因為 padding 而跑位 */
  box-sizing: border-box;
}

/* ========== 背景圖設定區 ========== */
.auth-background {
  position: absolute;
  inset: 0;
  z-index: -2;
  background-image: url('@/assets/bg-login.jpg'); /* 載入照片的區塊 */
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;

  /* 疊加一層深色，讓文字更清楚 */
  background-color: rgba(15, 23, 42, 0.4);
  background-blend-mode: multiply;

  /* === 新增：模糊特效 === */
  /* blur(8px) 數值越大越模糊，您可以根據喜好調整 */
  filter: blur(8px);

  /* === 新增：放大背景 === */
  /* 因為模糊會導致邊緣內縮產生白邊，所以要稍微放大一點點來填補 */
  transform: scale(1.1);
}

/* 粒子層定位 */
.particles-layer {
  position: absolute;
  inset: 0;
  z-index: -1;
}

/* 內容容器*/
.auth-container {
  position: relative;
  z-index: 1;
  width: 100%;
  /* 修正：限制最大寬度，但在手機上自動撐滿 */
  max-width: 480px; 
  
  /* 關鍵：利用 margin 達成垂直與水平的靈活置中 */
  margin: auto; 

  display: flex;
  flex-direction: column;
  align-items: center;
  
  /* 確保在極端窄的螢幕下不會爆版 */
  word-wrap: break-word;
}

/* Header & Logo */
.auth-header {
  margin-bottom: 1rem; /* 減少間距 (原本2rem) */
  text-align: center;
  flex-shrink: 0; /* 防止 Logo 被壓扁 */
}

.logo-text {
  font-size: 2rem; /* 縮小 Logo 字體 (原本3rem)，節省空間 */
  font-weight: 800;
  color: #ffffff;
  letter-spacing: 1.5px;
  text-shadow:
    0 0 10px rgba(14, 165, 233, 0.6),
    0 0 20px rgba(14, 165, 233, 0.4);
  margin: 0;
}

/* ========== 重點修改：3D 立體卡片容器 ========== */
.glass-card-wrapper {
  width: 100%;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px; /* 稍微調小圓角 */

  /* 【修復點 1】移除 padding，解決邊框折疊感 */
  padding: 0;

  /* 【修復點 2】加上 overflow: hidden，讓內部內容不會跑出圓角外 */
  overflow: hidden;

  box-shadow:
    0 20px 50px -12px rgba(0, 0, 0, 0.5),
    0 0 0 1px rgba(255, 255, 255, 0.2) inset;

  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);

  /* 讓卡片本身可以收縮，不要硬撐大 */
  display: flex;
  flex-direction: column;
}

/* Footer */
.auth-footer {
  margin-top: 1rem; /* 減少間距 (原本2rem) */
  font-size: 0.75rem; /* 字體縮小一點 */
  color: #cbd5e1;
  text-align: center;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
  flex-shrink: 0;
}

/* Vue Transition 動畫 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition:
    opacity 0.3s ease,
    transform 0.3s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.animate-fade-down {
  animation: fadeDown 0.8s ease-out;
}

.animate-pop-in {
  animation: popIn 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes fadeDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes popIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}
</style>
