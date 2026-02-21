<script setup>
import { useRouter, useRoute } from "vue-router";
import { ElMessageBox } from "element-plus";

const router = useRouter();
const route = useRoute();

const logout = async () => {
  try {
    // 只有按「登出」才會 resolve
    await ElMessageBox.confirm("確定要登出嗎？", "登出確認", {
      confirmButtonText: "登出",
      cancelButtonText: "取消",
      type: "warning",
    });

    // 使用者真的確認後，才跳轉
    localStorage.removeItem("token");
    router.push("/login");
  } catch (err) {
    // 使用者按取消，什麼都不做
  }
};

const go = (path) => {
  router.push(path);
};
</script>

<template>
  <el-container class="layout">
    <!-- Header -->
    <el-header class="header">
        <router-link to="/">
          <div class="logo">SeatRentSys</div>
        </router-link>
    </el-header>

    <el-container>
      <!-- Sidebar -->
      <el-aside class="aside" width="220px">
        <el-menu class="menu" :default-active="route.name">
          <el-menu-item index="member-profile" @click="go('/member/profile')">
            個人資料
          </el-menu-item>
          <el-menu-item
            index="rec-rent-user-order"
            :class="{
              'is-active':
                route.name === 'rec-rent-user' &&
                (route.params.action === 'order' || !route.params.action),
            }"
            @click="router.push({ name: 'rec-rent-user', params: { action: 'order' } })"
          >
            租借＠Seat
          </el-menu-item>
          <el-menu-item
            index="rec-rent-user-complete"
            :class="{
              'is-active':
                route.name === 'rec-rent-user' && route.params.action === 'complete',
            }"
            @click="
              router.push({ name: 'rec-rent-user', params: { action: 'complete' } })
            "
          >
            歸還＠Seat
          </el-menu-item>

          <div class="divider"></div>

          <!-- 不要 index + 不要 router -->
          <el-menu-item index="logout" class="logout" @click="logout">
            登出
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- Content -->
      <el-main class="main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100vh;
  background: #f8f9fa;
}

.header {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  padding: 0 20px;
}

.logo {
  font-size: 20px;
  font-weight: 700;
}

.aside {
  background: #f7f8fa;
  border-right: 1px solid #e5e7eb;
  padding: 12px 10px;
}

.menu {
  border-right: none;
  background: transparent;
}

.divider {
  height: 1px;
  background: #e5e7eb;
  margin: 12px 8px;
}

.main {
  padding: 30px;
}

/* 登出樣式：Element Plus menu-item 需要用 class 覆蓋 */
:deep(.logout) {
  color: #b91c1c;
}
:deep(.logout:hover) {
  background: #fef2f2 !important;
}
</style>
