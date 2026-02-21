<script setup>
/**
 * AdminLayout.vue：AdminLTE 3 後台版型
 * [修正] 移除 lang="ts" 與型別標註，轉換為純 JS 寫法
 * [新增] 整合 SweetAlert2 登出確認
 * [新增] 下拉選單導航設計
 * [新增] Sidebar 收合/展開功能（Click Toggle + Hover Expand 模式）
 */
import { ref, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import { useRouter, useRoute, RouterLink, RouterView } from 'vue-router'
// 1. 引入 SweetAlert2
import Swal from 'sweetalert2'
import { useAdminAuthStore } from '@/stores/adminAuth'

import AdminInfoModal from '@/views/member/AdminInfoModal.vue'

const adminAuthStore = useAdminAuthStore()

const router = useRouter()
const route = useRoute()

// 展開的選單群組
const expandedGroups = ref([''])

// AdminLTE 3 必備的 Body Class
const bodyClasses = ['hold-transition', 'layout-fixed']

// 需要移除的 sidebar 收合 class（這些會導致 icon-only）
const sidebarCollapseClasses = ['sidebar-collapse', 'sidebar-closed', 'sidebar-mini']

// 側邊欄狀態（性能優化：避免重複操作DOM）
const sidebarInitialized = ref(false)

const showProfileModal = ref(false)

// ========== Sidebar 狀態管理 (Single Source of Truth) ==========
// 模式：'click' = 點擊切換模式, 'hover' = 懸停展開模式
const sidebarMode = ref('click')
// 是否收合（true = 只顯示 icon）
const isCollapsed = ref(true)
// 是否被固定展開（hover 模式下的 pin 功能）
const isPinned = ref(false)
// 是否正在 hover 中
const isHovering = ref(false)
// Hover 延遲計時器
let hoverEnterTimer = null
let hoverLeaveTimer = null
// 延遲時間配置
const HOVER_ENTER_DELAY = 80
const HOVER_LEAVE_DELAY = 180

/**
 * 計算 sidebar 是否應該展開
 * - click 模式：由 isCollapsed 控制
 * - hover 模式：isPinned 或 isHovering 時展開
 */
const shouldExpand = computed(() => {
  if (sidebarMode.value === 'click') {
    return !isCollapsed.value
  }
  // hover 模式
  return isPinned.value || isHovering.value
})

/**
 * 同步 body class 與狀態
 */
const syncBodyClass = () => {
  if (shouldExpand.value) {
    document.body.classList.remove('sidebar-collapse')
    document.body.classList.add('sidebar-open')
  } else {
    document.body.classList.add('sidebar-collapse')
    document.body.classList.remove('sidebar-open')
  }
}

// 監聽 shouldExpand 變化，同步 body class
watch(shouldExpand, () => {
  syncBodyClass()
})

/**
 * 切換 Sidebar 收合狀態（漢堡按鈕）
 */
const toggleSidebar = () => {
  if (sidebarMode.value === 'click') {
    isCollapsed.value = !isCollapsed.value
  } else {
    // hover 模式下，漢堡按鈕用於 pin/unpin
    isPinned.value = !isPinned.value
  }
}

/**
 * 切換 Sidebar 模式
 */
const toggleMode = () => {
  sidebarMode.value = sidebarMode.value === 'click' ? 'hover' : 'click'
  // 切換模式時重置狀態
  isPinned.value = false
  isHovering.value = false
  if (sidebarMode.value === 'click') {
    isCollapsed.value = true
  }
}

/**
 * Hover 進入處理
 */
const handleMouseEnter = () => {
  if (sidebarMode.value !== 'hover') return
  if (isPinned.value) return // 已固定，不處理

  clearTimeout(hoverLeaveTimer)
  hoverEnterTimer = setTimeout(() => {
    isHovering.value = true
  }, HOVER_ENTER_DELAY)
}

/**
 * Hover 離開處理
 */
const handleMouseLeave = () => {
  if (sidebarMode.value !== 'hover') return
  if (isPinned.value) return // 已固定，不處理

  clearTimeout(hoverEnterTimer)
  hoverLeaveTimer = setTimeout(() => {
    isHovering.value = false
  }, HOVER_LEAVE_DELAY)
}

const formattedAdminData = computed(() => {
  const raw = adminAuthStore.admin || {}
  return {
    // 這裡的 Key (左邊) 必須呼應 Modal 裡的變數
    admId: raw.admId || raw.id, // Modal 用 adminData.admId
    admName: raw.admName || raw.name, // Modal 用 adminData.admName
    admUsername: raw.admUsername || raw.username, // Modal 用 adminData.admUsername
    admEmail: raw.admEmail || raw.email, // Modal 用 adminData.admEmail
    admRole: raw.admRole || raw.role, // Modal 用 adminData.admRole
    createdAt: raw.createdAt || raw.admCreatedAt, // Modal 用 formatDate(adminData.createdAt)
  }
})

const currentAdminAvatar = computed(() => {
  const adminId = adminAuthStore.admin?.admId || adminAuthStore.admin?.id
  if (adminId >= 1 && adminId <= 10) {
    const fileName = String(adminId).padStart(2, '0')
    return `/admin/${fileName}.jpg`
  }
  return '/admin/default.png'
})

// 選單群組定義
const menuGroups = [
  {
    id: 'spot',
    title: '場地與座位',
    icon: 'fas fa-building',
    items: [
      {
        path: '/admin/spot/list',
        icon: 'fas fa-map-marker-alt',
        title: '據點管理',
        prefix: '/admin/spot/list',
      },
      { path: '/admin/seat/list', icon: 'fas fa-chair', title: '座位管理', prefix: '/admin/seat' },
      {
        path: '/admin/spot/analyze',
        icon: 'fas fa-chart-bar',
        title: '據點分析',
        prefix: '/admin/spot/analyze',
      },
      {
        path: '/admin/spot/monitor',
        icon: 'fas fa-broadcast-tower',
        title: '調度中心',
        prefix: '/admin/spot/monitor',
      },
    ],
  },
  {
    id: 'member',
    title: '會員與權限',
    icon: 'fas fa-users-cog',
    items: [
      { path: '/admin/members', icon: 'fas fa-users', title: '會員列表', prefix: '/admin/members' },
      {
        path: '/admin/admins',
        icon: 'fas fa-user-cog',
        title: '管理員列表',
        prefix: '/admin/admins',
      },
    ],
  },
  {
    id: 'merchant',
    title: '商家與優惠',
    icon: 'fas fa-store-alt',
    items: [
      {
        path: '/admin/merchants',
        icon: 'fas fa-store',
        title: '商家管理',
        prefix: '/admin/merchants',
      },
      {
        path: '/admin/discounts',
        icon: 'fas fa-ticket-alt',
        title: '優惠券管理',
        prefix: '/admin/discounts',
      },
      {
        path: '/admin/redemption-logs',
        icon: 'fas fa-chart-bar',
        title: '兌換紀錄報表',
        prefix: '/admin/redemption',
      },
      {
        path: '/admin/sponsors',
        icon: 'fas fa-chart-bar',
        title: '贊助紀錄報表',
        prefix: '/admin/sponsors',
      },
    ],
  },
  {
    id: 'order',
    title: '租借與訂單',
    icon: 'fas fa-clipboard-list',
    items: [
      {
        path: '/admin/rec-rent',
        icon: 'fas fa-file-invoice',
        title: '訂單管理',
        prefix: '/admin/rec-rent',
      },
      {
        path: '/admin/rec-chart',
        icon: 'fas fa-file-invoice',
        title: '統計圖表',
        prefix: '/admin/rec-chart',
      },
    ],
  },
  {
    id: 'maintenance',
    title: '維護與工單',
    icon: 'fas fa-tools',
    items: [
      {
        path: '/admin/staff-list',
        icon: 'fas fa-user-shield',
        title: '維護人員管理',
        prefix: '/admin/staff',
      },
      {
        path: '/admin/mtif-list',
        icon: 'fas fa-wrench',
        title: '維修工單管理',
        prefix: '/admin/mtif',
      },
      {
        path: '/admin/maintenance/schedule',
        icon: 'fas fa-calendar-check',
        title: '定期排程管理',
        prefix: '/admin/maintenance/schedule',
      },
    ],
  },
]

onMounted(() => {
  // 性能優化：避免重複DOM操作
  if (sidebarInitialized.value) return

  // 加入必要的 layout class
  document.body.classList.add(...bodyClasses)

  // ✅ 預設收合（關閉）- 同步初始狀態到 body class
  syncBodyClass()

  // 延遲移除 hold-transition（避免初始化閃爍）
  requestAnimationFrame(() => {
    document.body.classList.remove('hold-transition')
    sidebarInitialized.value = true
  })

  // 攔截 AdminLTE 原生的 pushmenu 點擊，改用自己的邏輯
  const pushmenuBtn = document.querySelector('[data-widget="pushmenu"]')
  if (pushmenuBtn) {
    pushmenuBtn.addEventListener('click', handlePushmenuClick)
  }

  const savedAdmin = localStorage.getItem('admin')
  if (savedAdmin) {
    adminAuthStore.setAdmin(JSON.parse(savedAdmin))
  }
})

/**
 * 攔截 AdminLTE pushmenu 點擊事件
 */
const handlePushmenuClick = (e) => {
  e.preventDefault()
  e.stopPropagation()
  toggleSidebar()
}

onBeforeUnmount(() => {
  // 性能優化：只在已初始化時清理
  if (!sidebarInitialized.value) return

  document.body.classList.remove(...bodyClasses)
  // 清除 sidebar 狀態 class，避免 SPA 殘留
  document.body.classList.remove('sidebar-open')
  sidebarCollapseClasses.forEach((cls) => {
    document.body.classList.remove(cls)
  })

  // 清理 hover 計時器
  clearTimeout(hoverEnterTimer)
  clearTimeout(hoverLeaveTimer)

  // 移除 pushmenu 事件監聽
  const pushmenuBtn = document.querySelector('[data-widget="pushmenu"]')
  if (pushmenuBtn) {
    pushmenuBtn.removeEventListener('click', handlePushmenuClick)
  }

  sidebarInitialized.value = false
})

/**
 * 判斷目前路由是否屬於某個功能組
 */
const isActiveGroup = (prefix) => {
  return route.path.startsWith(prefix)
}

/**
 * 判斷群組是否有活動項目
 */
const isGroupActive = (group) => {
  return group.items.some((item) => route.path.startsWith(item.prefix))
}

/**
 * 切換群組展開狀態
 */
const toggleGroup = (groupId) => {
  const index = expandedGroups.value.indexOf(groupId)
  if (index > -1) {
    expandedGroups.value.splice(index, 1)
  } else {
    expandedGroups.value.push(groupId)
  }
}

/**
 * 判斷群組是否展開
 */
const isGroupExpanded = (groupId) => {
  return expandedGroups.value.includes(groupId)
}

/**
 * 登出功能 (SweetAlert2 版)
 */
const logout = async () => {
  // 2. 跳出確認視窗
  const result = await Swal.fire({
    title: '確定要登出嗎？',
    text: '登出後將無法存取後台頁面',
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#d33', // 紅色按鈕表示警告
    cancelButtonColor: '#3085d6',
    confirmButtonText: '登出',
    cancelButtonText: '取消',
  })

  // 3. 使用者按了「確定」
  if (!result.isConfirmed) return

  // 統一清除 localStorage（跟 main.js / router 守衛一致）
  localStorage.removeItem('token')
  localStorage.removeItem('admin')

  // 清除 Pinia 管理員狀態（若有此方法）
  if (typeof adminAuthStore.clearAdmin === 'function') {
    adminAuthStore.clearAdmin()
  }

  // 顯示成功訊息並跳轉
  await Swal.fire({
    title: '已登出！',
    text: '登出成功!',
    icon: 'success',
    timer: 1500,
    showConfirmButton: false,
  })

  router.push('/login')
}
</script>

<template>
  <div class="wrapper">
    <!-- 頂部導航欄 -->
    <nav class="main-header navbar navbar-expand navbar-white navbar-light border-bottom">
      <ul class="navbar-nav">
        <li class="nav-item">
          <a
            class="nav-link"
            data-widget="pushmenu"
            href="#"
            role="button"
            :title="sidebarMode === 'hover' ? (isPinned ? '取消固定' : '固定展開') : '切換選單'"
          >
            <i
              class="fas"
              :class="sidebarMode === 'hover' && isPinned ? 'fa-thumbtack' : 'fa-bars'"
            ></i>
          </a>
        </li>
        <li class="nav-item d-none d-sm-inline-block">
          <RouterLink to="/admin" class="nav-link">
            <i class="fas fa-home me-1"></i> 後台首頁
          </RouterLink>
        </li>
        <li class="nav-item d-none d-sm-inline-block">
          <RouterLink to="/" class="nav-link">
            <i class="fas fa-external-link-alt me-1"></i> 前台首頁
          </RouterLink>
        </li>
        <!-- 模式切換按鈕 -->
        <li class="nav-item d-none d-sm-inline-block">
          <a
            class="nav-link"
            href="#"
            @click.prevent="toggleMode"
            :title="sidebarMode === 'click' ? '切換為 Hover 模式' : '切換為 Click 模式'"
          >
            <i
              class="fas"
              :class="sidebarMode === 'click' ? 'fa-mouse-pointer' : 'fa-hand-pointer'"
            ></i>
            <span class="ml-1">{{ sidebarMode === 'click' ? 'Click' : 'Hover' }}</span>
          </a>
        </li>
      </ul>

      <ul class="navbar-nav ml-auto">
        <li class="nav-item">
          <a class="nav-link text-danger fw-bold" href="#" @click.prevent="logout">
            <i class="fas fa-sign-out-alt"></i> 登出
          </a>
        </li>
      </ul>
    </nav>

    <!-- 側邊欄 -->
    <aside
      class="main-sidebar sidebar-dark-primary elevation-2"
      @mouseenter="handleMouseEnter"
      @mouseleave="handleMouseLeave"
    >
      <!-- 品牌 Logo -->
      <RouterLink to="/admin" class="brand-link">
        <div class="brand-icon">
          <i class="fas fa-chair"></i>
        </div>
        <span class="brand-text">Take@Seat</span>
      </RouterLink>

      <div class="sidebar">
        <!-- 管理員資訊卡片 -->
        <div class="user-card" @click="showProfileModal = true" style="cursor: pointer">
          <div class="user-avatar">
            <img :src="currentAdminAvatar" class="sidebar-avatar-img" />
          </div>
          <div class="user-info">
            <span class="user-name">{{ adminAuthStore.admin.name || '管理員' }}</span>
            <span class="user-role">
              <i class="fas fa-shield-alt"></i>
              {{ adminAuthStore.admin.role === 9 ? '超級管理員' : '一般管理員' }}
            </span>
          </div>
        </div>

        <!-- 導航選單 -->
        <nav class="sidebar-nav">
          <!-- 選單群組 -->
          <div class="nav-section">
            <div
              v-for="group in menuGroups"
              :key="group.id"
              class="menu-group"
              :class="{
                'is-active': isGroupActive(group),
                'is-expanded': isGroupExpanded(group.id),
              }"
            >
              <!-- 群組標題 -->
              <div class="group-header" @click="toggleGroup(group.id)">
                <div class="group-icon">
                  <i :class="group.icon"></i>
                </div>
                <span class="group-title">{{ group.title }}</span>
                <div class="group-arrow">
                  <i class="fas fa-chevron-down"></i>
                </div>
              </div>

              <!-- 群組項目 -->
              <transition name="slide">
                <div class="group-items" v-show="isGroupExpanded(group.id)">
                  <RouterLink
                    v-for="item in group.items"
                    :key="item.path"
                    :to="item.path"
                    class="menu-item"
                    :class="{ active: isActiveGroup(item.prefix) }"
                  >
                    <i :class="item.icon"></i>
                    <span>{{ item.title }}</span>
                  </RouterLink>
                </div>
              </transition>
            </div>
          </div>
        </nav>
      </div>
    </aside>

    <!-- 主要內容區 -->
    <div class="content-wrapper">
      <section class="content py-3">
        <div class="container-fluid">
          <RouterView />
        </div>
      </section>
    </div>
    <AdminInfoModal
      :show="showProfileModal"
      :adminData="formattedAdminData"
      @close="showProfileModal = false"
    />
  </div>
</template>

<style scoped>
/* ========== 品牌區域 ========== */
.brand-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px !important;
  background: linear-gradient(135deg, #9db8d4 0%, #8fa8c8 100%) !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.25);
  text-decoration: none;
  transition: background 0.2s ease;
  will-change: background;
}

.brand-link:hover {
  background: linear-gradient(135deg, #8fa8c8 0%, #8199bc 100%) !important;
}

.brand-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 19px;
  transition: transform 0.2s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  will-change: transform;
}

.brand-link:hover .brand-icon {
  transform: scale(1.1) rotate(5deg);
}

.brand-text {
  color: white !important;
  font-size: 1.1rem;
  font-weight: 600;
  letter-spacing: 0.5px;
}

/* ========== 側邊欄整體 ========== */
.main-sidebar {
  background: linear-gradient(180deg, #bdddff 0%, #96b5d4 100%) !important;
  width: 250px !important; /* 展開寬度 */
  min-width: 250px !important;
  overflow: hidden !important;
  transition:
    width 0.3s ease,
    min-width 0.3s ease !important;
  will-change: width, min-width;
}

.sidebar {
  padding: 0 !important;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 70px);
  overflow: visible;
  width: 250px; /* 固定內部寬度，避免文字換行 */
}

/* ========== 用戶卡片 ========== */
.user-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  margin: 12px 12px;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.35);
  transition:
    background 0.2s ease,
    box-shadow 0.2s ease;
  will-change: background, box-shadow;
}

.user-card:hover {
  background: rgba(255, 255, 255, 0.35);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.user-avatar {
  width: 42px;
  height: 42px;
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-name {
  color: #1e293b;
  font-weight: 600;
  font-size: 0.9rem;
}

.user-role {
  color: #475569;
  font-size: 0.75rem;
  margin-top: 2px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.user-role i {
  font-size: 10px;
  color: #60a5fa;
}

/* ========== 導航區域 ========== */
.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0 12px;
  scroll-behavior: smooth;
  /* 性能優化：固定高度避免計算 */
  contain: layout style paint;
}

.nav-section {
  margin-bottom: 12px;
}

/* ========== 選單群組 ========== */
.menu-group {
  margin-bottom: 2px;
  border-radius: 10px;
  overflow: hidden;
  /* 移除hover transform，避免layout shift */
}

.menu-group.is-active .group-header {
  background: rgba(76, 133, 248, 0.25);
}

.group-header {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 4px;
  cursor: pointer;
  transition: background 0.15s ease;
  border-radius: 12px;
  position: relative;
  will-change: background;
}

.group-header:hover {
  background: rgba(255, 255, 255, 0.15);
}

.group-icon {
  width: 32px;
  height: 32px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1e293b;
  font-size: 18px;
  transition: background 0.15s ease;
  will-change: background;
}

.group-header:hover .group-icon {
  background: rgba(255, 255, 255, 0.4);
  transform: scale(1.1) rotate(5deg);
}

.menu-group.is-active .group-icon {
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.4);
}

.group-title {
  flex: 1;
  color: #1e293b;
  font-size: 0.92rem;
  font-weight: 600;
}

.group-arrow {
  color: #64748b;
  font-size: 18px;
  transition: transform 0.3s ease;
}

.menu-group.is-expanded .group-arrow {
  transform: rotate(180deg);
}

/* ========== 群組項目 ========== */
.group-items {
  padding: 0px 0 0px 0;
  background: rgba(5, 0, 0, 0.1);
  border-radius: 0 0 10px 10px;
  overflow: hidden;
  will-change: max-height, opacity;
  transform-origin: top;
  /* 性能優化：避免submenu影響外層容器 */
  contain: layout;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 1px 5px 55px;
  color: #ffffff;
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.menu-item::before {
  content: '';
  position: absolute;
  left: 40px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #19365e;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.menu-item:hover {
  color: #000000;
  font-weight: 900;
}

.menu-item:hover::before {
  background: #1d4ed8;
  transform: scale(1.5);
  box-shadow: 0 0 10px rgba(29, 78, 216, 0.6);
}

.menu-item.active {
  color: #000000;
  background: rgba(55, 121, 255, 0.25);
  font-weight: 700;
}

.menu-item.active::before {
  background: #0173ff;
  box-shadow: 0 0 8px rgba(96, 165, 250, 0.5);
}

.menu-item i {
  width: 18px;
  text-align: center;
  font-size: 13px;
}

/* ========== 展開動畫（性能優化版） ========== */
.slide-enter-active {
  transition:
    max-height 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94),
    opacity 0.25s ease-out;
  overflow: hidden;
  will-change: max-height, opacity;
}

.slide-leave-active {
  transition:
    max-height 0.25s cubic-bezier(0.55, 0.06, 0.68, 0.19),
    opacity 0.2s ease-in;
  overflow: hidden;
  will-change: max-height, opacity;
}

.slide-enter-from {
  max-height: 0 !important;
  opacity: 0;
}

.slide-enter-to {
  max-height: 400px !important;
  opacity: 1;
}

.slide-leave-from {
  max-height: 400px !important;
  opacity: 1;
}

.slide-leave-to {
  max-height: 0 !important;
  opacity: 0;
}

/* ========== 頂部導航欄 ========== */
.main-header .navbar-nav .nav-link {
  padding: 8px 12px;
  font-size: 0.9rem;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 漢堡按鈕動態效果（簡化版） */
.main-header .navbar-nav .nav-link[data-widget='pushmenu'] {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: color 0.15s ease;
}

.main-header .navbar-nav .nav-link[data-widget='pushmenu']:active {
  color: #1d4ed8;
}

.main-header .navbar-nav .nav-link[data-widget='pushmenu']:hover {
  color: #3b82f6;
}

.main-header .navbar-text {
  color: #1e3a5f;
  font-weight: 500;
  font-size: 0.9rem;
}

/* ========== 滾動條美化 - 增強可見度 ========== */
.sidebar-nav::-webkit-scrollbar {
  width: 8px;
}

.sidebar-nav::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 4px;
}

.sidebar-nav::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.4);
  border-radius: 4px;
  border: 2px solid transparent;
  background-clip: padding-box;
}

.sidebar-nav::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.6);
  border: 2px solid transparent;
  background-clip: padding-box;
}

/* Firefox scrollbar */
.sidebar-nav {
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.4) rgba(255, 255, 255, 0.15);
}

/* AdminLTE 主內容區：統一由AdminLTE JS管理動畫 */
.content-wrapper {
  will-change: transform;
}

.sidebar-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 10px; /* 跟原本 .user-avatar 的圓角一致 */
}

/* 頂部導航欄微型頭像樣式 */
.nav-avatar-mini {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
  margin-right: 8px;
  border: 1px solid #ddd;
}

/* ========== Sidebar 收合狀態樣式 ========== */
/* 定義需要隱藏的文字元素的過渡動畫 */
.brand-text,
.user-info,
.group-title,
.group-arrow,
.menu-item span {
  transition:
    opacity 0.25s ease,
    transform 0.25s ease,
    width 0.25s ease;
  white-space: nowrap;
  overflow: hidden;
}

/* 用戶卡片收合過渡 */
.user-card {
  transition:
    background 0.2s ease,
    box-shadow 0.2s ease,
    padding 0.25s ease,
    margin 0.25s ease;
}
</style>

<!-- 非 scoped 樣式：處理 body.sidebar-collapse 狀態 -->
<style>
/* ========== Sidebar 收合狀態 (body.sidebar-collapse) ========== */
/* 收合寬度：72px */
body.sidebar-collapse .main-sidebar {
  width: 72px !important;
  min-width: 72px !important;
}

/* 隱藏品牌文字 */
body.sidebar-collapse .brand-text {
  opacity: 0;
  width: 0;
  overflow: hidden;
  display: none;
}

/* 品牌區域收合時置中 */
body.sidebar-collapse .brand-link {
  justify-content: center;
  padding: 16px 12px !important;
}

/* 隱藏用戶資訊 */
body.sidebar-collapse .user-info {
  opacity: 0;
  width: 0;
  overflow: hidden;
  display: none;
}

/* 用戶卡片收合時置中 */
body.sidebar-collapse .user-card {
  justify-content: center;
  padding: 8px;
  margin: 12px 8px;
}

/* 隱藏群組標題 */
body.sidebar-collapse .group-title {
  opacity: 0;
  width: 0;
  overflow: hidden;
  display: none;
}

/* 隱藏群組箭頭 */
body.sidebar-collapse .group-arrow {
  opacity: 0;
  width: 0;
  overflow: hidden;
  display: none;
}

/* 群組標題收合時置中 */
body.sidebar-collapse .group-header {
  justify-content: center;
  padding: 8px;
}

/* 隱藏選單項目文字 */
body.sidebar-collapse .menu-item span {
  opacity: 0;
  width: 0;
  overflow: hidden;
  display: none;
}

/* 選單項目收合時調整 */
body.sidebar-collapse .menu-item {
  justify-content: center;
  padding: 10px 8px;
}

/* 隱藏選單項目前的圓點 */
body.sidebar-collapse .menu-item::before {
  display: none;
}

/* 群組項目收合時隱藏（因為只顯示 icon，子項目不需要展開） */
body.sidebar-collapse .group-items {
  display: none !important;
}

/* 側邊欄內部固定寬度 */
body.sidebar-collapse .sidebar {
  width: 72px;
}

/* 導航區域收合時調整 padding */
body.sidebar-collapse .sidebar-nav {
  padding: 0 4px;
}

/* content-wrapper 收合時的 margin 調整 */
body.sidebar-collapse .content-wrapper {
  margin-left: 72px !important;
}

body.sidebar-open .menu-item::before {
  display: block;
}
</style>
