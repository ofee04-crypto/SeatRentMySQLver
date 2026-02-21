<script setup>
import { ref, onMounted, nextTick } from "vue";
import { LocationFilled } from "@element-plus/icons-vue";
import axios from "axios";
import MainLayout from "@/layouts/MainLayout.vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";

// --- 路由與狀態管理 ---
const router = useRouter();
const authStore = useAuthStore();
// --- 1. 狀態定義 ---

// --- 2. 核心邏輯 ---

// --- 3. 視圖切換邏輯 ---

</script>

<template>
<!-- 框架區塊 -->
<MainLayout></MainLayout>
<!-- 組件區塊 -->

</template>

<style scoped>

</style>