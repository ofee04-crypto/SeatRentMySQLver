<script setup>
import { ref, computed } from 'vue'
import { useMemberAuthStore } from '@/stores/memberAuth'
import { useAdminAuthStore } from '@/stores/adminAuth'
import { useRouter } from 'vue-router'

const router = useRouter()

// --- 版面狀態 ---
const isSidebarCollapsed = ref(false)

//切換側邊欄收合狀態
const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}

const memberAuthStore = useMemberAuthStore()
const adminAuthStore = useAdminAuthStore()

/**
 * UID 顯示邏輯：
 * - 管理員優先
 * - 再來會員
 * - 都沒有就尚未登入
 */
const displayUID = computed(() => {
  if (adminAuthStore.isLogin) {
    return adminAuthStore.admin?.username || ''
  }
  if (memberAuthStore.isLogin) {
    return memberAuthStore.member?.memUsername || ''
  }
  return null
})

/**
 * 產品化：點擊 UID 的導頁策略
 * - 未登入：不動作（也可改成導去 /login）
 * - 管理員登入：導去 /admin
 * - 會員登入：導去 /profile
 */
const handleUidClick = () => {
  if (!displayUID.value) {
    router.push('/login')
    return
  }

  if (adminAuthStore.isLogin) {
    router.push('/admin')
    return
  }

  // member
  router.push('/profile')
}

/**
 *  產品化：讓游標跟可點性一致
 */
const uidClickable = computed(() => true)
const uidCursor = computed(() => (uidClickable.value ? 'pointer' : 'default'))

/**
 * 登出：
 * - 清空 Pinia（會員 / 管理員）
 * - 清空 localStorage
 * - 停留在首頁
 */
const logout = () => {
  // 清空 Pinia
  memberAuthStore.clearMemberLogin()
  adminAuthStore.clearAdmin()

  // 清空 localStorage
  localStorage.removeItem('member_user')
  localStorage.removeItem('admin')
  localStorage.removeItem('token')

  // 留在首頁（刷新一次確保畫面同步）
  window.location.href = '/'
}
</script>

<template>
  <div class="app-layout">
    <div class="page-wrapper" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
      <!-- 側邊欄 -->
      <aside class="sidebar">
        <!-- 收合功能選單 -->
        <ul class="menu-list">
          <li class="menu-item" @click="router.push('/')">
            <div class="member-info">
              <span class="icon-wrapper">
                <el-icon><House /></el-icon>
              </span>
              <span class="menu-text">Take@Seat</span>
            </div>
          </li>

          <!--未登入才顯示「會員登入」 -->
          <li class="menu-item" v-if="!memberAuthStore.isLogin && !adminAuthStore.isLogin">
            <router-link to="/login" class="member-info">
              <span class="icon-wrapper">
                <el-icon><Avatar /></el-icon>
              </span>
              <span class="menu-text">請先登入</span>
            </router-link>
          </li>
          <li class="menu-item">
            <div
              class="member-info"
              @click="handleUidClick"
              :style="{
                cursor: uidCursor,
                width: '100%',
              }"
              :aria-disabled="!uidClickable"
            >
              <span class="icon-wrapper" style="font-size: 130%; font-weight: 400">UID: </span>
              <span class="menu-text">
                <span v-if="displayUID">{{ displayUID }}</span>
                <span v-else> 歡迎使用 </span>
              </span>
            </div>
          </li>

          <li class="menu-item" @click="router.push('/SearchSpot')">
            <span class="icon-wrapper">
              <el-icon><Pointer /></el-icon>
            </span>
            <span class="menu-text">@Seat借還</span>
          </li>

          <li class="menu-item" @click="router.push('/mall')">
            <span class="icon-wrapper">
              <el-icon><Ticket /></el-icon>
            </span>
            <span class="menu-text">商家優惠</span>
          </li>

          <li class="menu-item" @click="router.push('/snake')">
            <span class="icon-wrapper">
              <el-icon><SwitchFilled /></el-icon>
            </span>
            <span class="menu-text">小遊戲</span>
          </li>
          <li class="menu-item" v-if="memberAuthStore.isLogin">
            <router-link to="/redemption-history" class="member-info">
              <span class="icon-wrapper">
                <el-icon><List /></el-icon>
              </span>
              <span class="menu-text">兌換紀錄</span>
            </router-link>
          </li>

          <li class="menu-item" @click="router.push('/VisitSiteReommand')">
            <span class="icon-wrapper">
              <el-icon><MapLocation /></el-icon>
            </span>
            <span class="menu-text">美食景點</span>
          </li>
          <li class="menu-item" @click="router.push('/support')">
            <span class="icon-wrapper">
              <el-icon><Phone /></el-icon>
            </span>
            <span class="menu-text">客服支援</span>
          </li>

          <li class="menu-item">
            <router-link to="/sponsor" class="member-info">
              <span class="icon-wrapper">
                <el-icon><StarFilled /></el-icon>
              </span>
              <span class="menu-text">支持我們</span>
            </router-link>
          </li>
          <li class="menu-item" @click="logout">
            <span class="icon-wrapper">
              <el-icon><TopLeft /></el-icon>
            </span>
            <span class="menu-text">登出</span>
          </li>
        </ul>
        <!--管理員快捷入口-->
        <div class="menu-admin" v-if="adminAuthStore.isLogin">
          <router-link to="/admin" class="member-info">
            <span class="icon-wrapper">
              <el-icon><Tools /></el-icon>
            </span>
            <span class="menu-text">後台管理</span>
          </router-link>
        </div>

        <!--收合按鈕 -->
        <div class="sidebar-footer">
          <button
            @click="toggleSidebar"
            class="toggle-btn"
            :title="isSidebarCollapsed ? '展開' : '收合'"
          >
            <el-icon>
              <DArrowLeft v-if="!isSidebarCollapsed" />
              <DArrowRight v-if="isSidebarCollapsed" />
            </el-icon>
          </button>
        </div>
      </aside>

      <!-- 右側主內容容器 -->
      <main class="main-content-area">
        <router-view />
      </main>
    </div>

    <!-- Footer -->
    <footer class="main-footer">
      <div class="footer-links">
        <a href="/claims">隱私權政策</a> | <a href="#">個資告知書</a> | <a href="#">使用條款</a> |
        <a href="#">服務條款</a> | © 2026 Have@Seat及其關係企業版權所有。
      </div>
      <div class="footer-copyright">
        Have@Seat其等之標誌以及本網站中其他Have@Seat產品及服務名稱及標誌，皆為Have@Seat Inc.
        之商標或註冊商標。本網站中提及之其他公司名稱、產品名稱、服務名稱及標誌，分別為其所有權人之商標。
      </div>
    </footer>
  </div>
</template>

<style scoped>
/* --- 0. App Layout --- */
.app-layout {
  display: flex;
  flex-direction:column;
  height:100vh;
  width: 100%;
  background-color: #ffffff;
}

/* --- 1. CSS 變數 --- */
:root {
  --sidebar-width-expanded: 200px;
  --sidebar-width-collapsed: 80px;
}
/* --- 4. 主內容容器 --- */
.main-content-area {
  flex-grow: 1;
  width: 100%;
  position: relative;
  display: flex;
  flex-direction: column;
  overflow-y: auto; /* 當內容超出時，顯示滾動條，避免內容溢出覆蓋頁尾 */
}
/* --- 2. 主佈局 --- */
.page-wrapper {
  display: flex;
  flex: 1; /* 取代 height: 100vh，使其能自動伸展 */
  width: 100%;
  overflow: hidden; /* 確保子元素滾動，而不是此容器滾動 */
}

/* --- 3. 側邊欄 --- */
.sidebar {
  width: var(--sidebar-width-expanded);
  background-color: #c4f7c4;
  border-right: 1px solid #dee2e6;
  transition: width 0.2s ease;
  flex-shrink: 0;
  display: flex;
  overflow-y: auto;
  flex-direction: column;
  height: auto;
}

.page-wrapper.sidebar-collapsed .sidebar {
  width: var(--sidebar-width-collapsed);
  overflow-y: auto;
}

/* 通用圖示容器樣式 */
.icon-wrapper {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 15px;
  height: 10px;
  font-size: 30px; /* for SVG size */
  color: #2e2e2e;
  flex-shrink: 0;
}

.member-info {
  display: flex;
  align-items: center;
  gap: 20px;
  text-decoration: none;
  color: inherit;
}

/* 功能選單 (可隱藏) */
.menu-list {
  list-style: none;
  padding: 0;
  margin: 3px 0;
  flex-grow: 1;
  overflow-y: auto;
  overflow-x: hidden; /* 新增：防止水平滾動條 */
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  gap: 17px;
  cursor: pointer;
  white-space: nowrap;
  transition: background-color 0.2s;
}

.menu-item:hover,
.menu-admin:hover {
  background-color: #f5f7fa;
}

.menu-admin {
  display: flex;
  align-items: center;
  padding: 12px 15px;
  gap: 20px;
  cursor: pointer;
  white-space: nowrap;
  transition: background-color 0.2s;
}
.menu-text {
  font-size: 20px;
  font-family: fantasy;
  opacity: 1;
  transition:
    opacity 0.2s ease,
    width 0.2s ease;
  user-select: none;
  pointer-events: none;
}

.member-info:active {
  background-color: transparent !important;
}

.page-wrapper.sidebar-collapsed .menu-text {
  opacity: 0;
  width: 0;
}

/* 側邊欄頁腳 (收合按鈕) */
.sidebar-footer {
  padding: 3px;
  margin-top: auto; /* 將按鈕推到底部 */
  border-top: 1px solid #e9ecef;
}

.toggle-btn {
  width: 90%;
  background-color: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 12px;
  cursor: pointer;
  padding: 5px;
  font-size: 28px;
  line-height: 1;
  color: #404142;
  display: flex;
  justify-content: center;
  align-items: center;
  transition:
    background-color 0.2s,
    color 0.2s;
}

.toggle-btn:hover {
  background-color: #ecf5ff;
  color: #409eff;
}

/* --- 5. Footer --- */
.main-footer {
  flex-shrink: 0;
  background-color: #ffffff;
  padding: 0;
  border-top: 1px solid #ffffff;
  color: #6c757d;
  font-size: 0.8rem;
  text-align: center;
   flex-direction: column;
  margin:0%;
}

.footer-links {
  margin-bottom: 1px;
}

.footer-links a {
  color: #6c757d;
  text-decoration: none;
  margin: 0 1px;
}

.footer-links a:hover {
  text-decoration: underline;
}

.footer-copyright {
  line-height: 1.5;
}
</style>
