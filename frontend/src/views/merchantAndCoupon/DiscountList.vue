<template>
  <div class="discount-list-container">
    <section class="content-header">
      <div class="container-fluid">
        <div class="row align-items-center">
          <div class="col-12">
            <div class="page-title-box">
              <div class="title-icon">
                <i class="fas fa-ticket-alt"></i>
              </div>
              <div class="title-content">
                <h1>優惠券管理系統</h1>
                <p class="subtitle">管理合作商家優惠券活動</p>
              </div>
              <div class="title-actions">
                <el-button type="success" class="action-btn add-btn" @click="openEditModal()">
                  <i class="fas fa-plus mr-1"></i> 新增優惠券
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="content">
      <div class="container-fluid">
        <transition name="fade-slide" appear>
          <div>
            <el-row :gutter="20" class="mb-4">
              <el-col :xs="12" :sm="6">
                <div class="stat-card total-card">
                  <div class="stat-icon"><i class="fas fa-ticket-alt"></i></div>
                  <div class="stat-info">
                    <h3>{{ discounts.length }}</h3>
                    <span>總優惠券</span>
                  </div>
                </div>
              </el-col>
              <el-col :xs="12" :sm="6">
                <div class="stat-card active-card">
                  <div class="stat-icon"><i class="fas fa-play-circle"></i></div>
                  <div class="stat-info">
                    <h3>{{ discounts.filter((d) => d.couponStatus === 1).length }}</h3>
                    <span>進行中</span>
                  </div>
                </div>
              </el-col>
              <el-col :xs="12" :sm="6">
                <div class="stat-card pending-card">
                  <div class="stat-icon"><i class="fas fa-clock"></i></div>
                  <div class="stat-info">
                    <h3>{{ discounts.filter((d) => d.couponStatus === 0).length }}</h3>
                    <span>尚未開始</span>
                  </div>
                </div>
              </el-col>
              <el-col :xs="12" :sm="6">
                <div class="stat-card ended-card">
                  <div class="stat-icon"><i class="fas fa-stop-circle"></i></div>
                  <div class="stat-info">
                    <h3>
                      {{
                        discounts.filter((d) => d.couponStatus === 2 || d.couponStatus === 3).length
                      }}
                    </h3>
                    <span>已結束/下架</span>
                  </div>
                </div>
              </el-col>
            </el-row>

            <el-card shadow="hover" class="table-card">
              <template #header>
                <div class="card-header-content">
                  <div class="header-left">
                    <span class="header-icon"><i class="fas fa-list"></i></span>
                    <span class="header-text">優惠券列表</span>
                    <el-tag type="warning" effect="light" size="small" round
                      >{{ discounts.length }} 筆</el-tag
                    >
                  </div>
                </div>
              </template>

              <div class="filter-bar">
                <el-input
                  v-model="keyword"
                  placeholder="搜尋優惠券名稱..."
                  prefix-icon="Search"
                  clearable
                  class="filter-input"
                  @keyup.enter="fetchDiscounts"
                />
                <el-button type="warning" @click="fetchDiscounts" class="search-btn">
                  <i class="fas fa-search mr-1"></i> 搜尋
                </el-button>
              </div>

              <el-table
                :data="paginatedDiscounts"
                v-loading="loading"
                stripe
                highlight-current-row
                style="width: 100%"
                :header-cell-style="{ background: '#f5f7fa', fontWeight: 'bold', color: '#303133' }"
                class="modern-table"
              >
                <el-table-column prop="couponId" label="ID" width="70" align="center">
                  <template #default="{ row }">
                    <span class="id-tag">#{{ row.couponId }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="圖片" width="90" align="center">
                  <template #default="{ row }">
                    <div class="img-cell" @click.stop>
                      <el-image
                        v-if="row.couponImg"
                        class="coupon-img"
                        :src="`http://localhost:8080/images/${row.couponImg}`"
                        :preview-src-list="[`http://localhost:8080/images/${row.couponImg}`]"
                        :preview-teleported="true"
                        :z-index="4000"
                        fit="cover"
                        @click.stop
                      />
                      <span v-else class="img-empty">—</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="優惠名稱 / 內容" min-width="200">
                  <template #default="{ row }">
                    <div class="coupon-info-cell">
                      <div class="coupon-name">{{ row.couponName }}</div>
                      <div class="coupon-desc" :title="row.couponDescription">
                        {{ row.couponDescription }}
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="隸屬商家" width="140">
                  <template #default="{ row }">
                    <el-tag type="info" effect="plain" size="small" class="merchant-tag">
                      <i class="fas fa-store mr-1"></i>
                      {{
                        row.merchantName || (row.merchant ? row.merchant.merchantName : '未指定')
                      }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="所需點數" width="100" align="center">
                  <template #default="{ row }">
                    <span class="points-text">{{ row.pointsRequired }} <small>Pts</small></span>
                  </template>
                </el-table-column>
                <el-table-column label="狀態" width="110" align="center">
                  <template #default="{ row }">
                    <el-tag
                      :type="getStatusType(row.couponStatus)"
                      effect="light"
                      size="small"
                      class="status-tag"
                    >
                      <i :class="getStatusIcon(row.couponStatus)" class="mr-1"></i>
                      {{ getStatusText(row.couponStatus) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="活動期限" width="180">
                  <template #default="{ row }">
                    <div class="date-cell">
                      <i class="fas fa-calendar-alt text-primary mr-1"></i>
                      <span>{{ row.startDate }} ~ {{ row.endDate }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="150" align="center" fixed="right">
                  <template #default="{ row }">
                    <el-button-group>
                      <el-tooltip content="編輯" placement="top">
                        <el-button size="small" type="primary" @click="openEditModal(row)">
                          <i class="fas fa-edit"></i>
                        </el-button>
                      </el-tooltip>
                      <el-tooltip
                        v-if="row.couponStatus === 0 || row.couponStatus === 1"
                        content="下架"
                        placement="top"
                      >
                        <el-button
                          size="small"
                          type="warning"
                          @click="handleStatusChange(row.couponId, 'disable')"
                        >
                          <i class="fas fa-pause"></i>
                        </el-button>
                      </el-tooltip>
                      <el-tooltip v-if="row.couponStatus === 3" content="上架" placement="top">
                        <el-button
                          size="small"
                          type="success"
                          @click="handleStatusChange(row.couponId, 'relist')"
                        >
                          <i class="fas fa-play"></i>
                        </el-button>
                      </el-tooltip>
                      <el-tooltip content="刪除" placement="top">
                        <el-button size="small" type="danger" @click="deleteDiscount(row.couponId)">
                          <i class="fas fa-trash-alt"></i>
                        </el-button>
                      </el-tooltip>
                    </el-button-group>
                  </template>
                </el-table-column>
              </el-table>

              <div style="margin-top: 20px; display: flex; justify-content: center;">
                <el-pagination
                  v-model:current-page="currentPage"
                  v-model:page-size="pageSize"
                  :page-sizes="[5, 10, 15, 20]"
                  layout="total, sizes, prev, pager, next, jumper"
                  :total="filteredDiscounts.length"
                  background
                />
              </div>

              <el-empty
                v-if="discounts.length === 0 && !loading"
                description="目前沒有優惠券資料"
              />
            </el-card>
          </div>
        </transition>
      </div>
    </section>

    <el-dialog
      v-model="isEditModalOpen"
      :title="editingDiscount.couponId ? `編輯優惠券 #${editingDiscount.couponId}` : '新增優惠券'"
      width="600px"
      top="5vh"
      class="modern-dialog"
      :close-on-click-modal="false"
    >
      <div style="display: flex; justify-content: flex-end; margin-bottom: 15px;">
        <el-button 
          type="warning" 
          plain 
          size="small" 
          @click="handleQuickFill"
          style="border-style: dashed;"
        >
          <i class="fas fa-magic mr-1"></i> 一鍵填入測試資料
        </el-button>
      </div>

      <el-form
        :model="editingDiscount"
        label-position="top"
        class="modern-form"
      >
        <el-form-item label="優惠名稱" required>
          <el-input v-model="editingDiscount.couponName" placeholder="請輸入優惠名稱">
            <template #prefix><i class="fas fa-tag"></i></template>
          </el-input>
        </el-form-item>

        <el-form-item label="優惠描述">
          <el-input
            v-model="editingDiscount.couponDescription"
            type="textarea"
            :rows="2"
            placeholder="請輸入優惠描述"
          />
        </el-form-item>

        <el-form-item label="隸屬商家" required>
          <el-select
            v-model="editingDiscount.merchantId"
            placeholder="請選擇商家"
            style="width: 100%"
          >
            <el-option
              v-for="m in merchants"
              :key="m.merchantId"
              :value="m.merchantId"
              :label="m.merchantName"
            >
              <i class="fas fa-store mr-2 text-primary"></i>{{ m.merchantName }}
            </el-option>
          </el-select>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="開始日期">
              <el-date-picker
                v-model="editingDiscount.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="選擇日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="結束日期">
              <el-date-picker
                v-model="editingDiscount.endDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="選擇日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所需點數">
              <el-input-number
                v-model="editingDiscount.pointsRequired"
                :min="0"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          
          <el-col :span="12">
            <el-form-item label="優惠圖片">
              <div class="upload-container">
                <el-upload
                  action="#"
                  :auto-upload="false"
                  :show-file-list="false"
                  :on-change="onFileChange"
                  accept="image/*"
                >
                  <el-button type="info" plain style="width: 100%;">
                    <i class="fas fa-upload mr-1"></i>選擇圖片
                  </el-button>
                </el-upload>

                <div v-if="imagePreview" class="preview-box">
                  <el-image 
                    :src="imagePreview" 
                    fit="cover" 
                    class="preview-img-fill"
                  />
                  <div class="preview-overlay" @click="imagePreview = null; selectedFile = null">
                    <i class="fas fa-times"></i>
                  </div>
                </div>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="isEditModalOpen = false">取消</el-button>
          <el-button type="primary" @click="handleSave">儲存資料</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue' // 修正：補上 computed
import axios from 'axios'
import Swal from 'sweetalert2'

// 基本狀態
const discounts = ref([])
const merchants = ref([])
const keyword = ref('')
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(15)

// 搜尋過濾邏輯 
const filteredDiscounts = computed(() => {
  if (!keyword.value) return discounts.value;
  return discounts.value.filter(discount => 
    discount.couponName && discount.couponName.toLowerCase().includes(keyword.value.toLowerCase())
  );
});

// 分頁邏輯 
const paginatedDiscounts = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return filteredDiscounts.value.slice(start, end);
});

// Modal 與圖片控制
const isEditModalOpen = ref(false)
const selectedFile = ref(null)
const imagePreview = ref(null)

const editingDiscount = ref({
  couponId: null,
  couponName: '',
  couponDescription: '',
  merchantId: null,
  couponStatus: 1,
  couponImg: '',
  startDate: '',
  endDate: '',
  pointsRequired: 0,
})

// 狀態輔助函數
const getStatusType = (status) => {
  const map = { 0: 'info', 1: 'success', 2: 'danger', 3: 'warning' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { 0: '尚未開始', 1: '進行中', 2: '已結束', 3: '已下架' }
  return map[status] || '未知'
}

const getStatusIcon = (status) => {
  const map = {
    0: 'fas fa-clock',
    1: 'fas fa-play-circle',
    2: 'fas fa-stop-circle',
    3: 'fas fa-pause-circle',
  }
  return map[status] || 'fas fa-question-circle'
}

// 1. 取得列表
const fetchDiscounts = async () => {
  loading.value = true
  try {
    const res = await axios.get('http://localhost:8080/api/discounts', {
      params: { keyword: keyword.value },
    })
    discounts.value = res.data.data || []
  } catch (err) {
    console.error(err)
    Swal.fire({
      title: '錯誤',
      html: '<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg><p style="margin-top:12px;">取得清單失敗</p></div>',
      confirmButtonColor: '#409eff',
      confirmButtonText: '確定',
    })
  } finally {
    loading.value = false
  }
}

// 2. 取得商家選單
const fetchMerchants = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/merchants')
    merchants.value = res.data.data || []
  } catch (err) {
    console.error('載入商家失敗', err)
  }
}

// 3. 上、下架手動切換
const handleStatusChange = async (id, action) => {
  const actionText = action === 'relist' ? '上架' : '下架'

  const result = await Swal.fire({
    title: `確定要${actionText}嗎？`,
    html: `<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#e6a23c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg><p style="margin-top:12px;">${action === 'disable' ? '下架後消費者將無法在商城兌換此券' : '上架將根據活動日期自動判定狀態'}</p></div>`,
    showCancelButton: true,
    confirmButtonColor: '#409eff',
    cancelButtonColor: '#909399',
    confirmButtonText: `確定${actionText}`,
    cancelButtonText: '取消',
  })

  if (!result.isConfirmed) return

  try {
    const res = await axios.put(`http://localhost:8080/api/discounts/${id}/status`, null, {
      params: { action: action },
    })

    if (res.data.code === 200) {
      Swal.fire({
        title: '成功',
        html: `<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#67c23a" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg><p style="margin-top:12px;">${actionText}成功</p></div>`,
        confirmButtonColor: '#409eff',
        confirmButtonText: '確定',
      })
      fetchDiscounts()
    } else {
      Swal.fire({
        title: '失敗',
        html: `<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg><p style="margin-top:12px;">${res.data.message || '操作失敗'}</p></div>`,
        confirmButtonColor: '#409eff',
        confirmButtonText: '確定',
      })
    }
  } catch (err) {
    console.error(err)
    Swal.fire({
      title: '錯誤',
      html: '<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg><p style="margin-top:12px;">伺服器連線異常</p></div>',
      confirmButtonColor: '#409eff',
      confirmButtonText: '確定',
    })
  }
}

// 4. 圖片處理
const onFileChange = (file) => {
  if (file && file.raw) {
    selectedFile.value = file.raw
    imagePreview.value = URL.createObjectURL(file.raw)
  }
}

// 5. 開啟 Modal
const openEditModal = (item = null) => {
  if (item) {
    editingDiscount.value = { ...item }
    imagePreview.value = item.couponImg ? `http://localhost:8080/images/${item.couponImg}` : null
  } else {
    editingDiscount.value = {
      couponId: null,
      couponName: '',
      couponDescription: '',
      merchantId: null,
      couponStatus: 1,
      couponImg: '',
      startDate: '',
      endDate: '',
      pointsRequired: 0,
    }
    imagePreview.value = null
  }
  selectedFile.value = null
  isEditModalOpen.value = true
}

// 6. 儲存
const handleSave = async () => {
  try {
    const formData = new FormData()
    const jsonBlob = new Blob([JSON.stringify(editingDiscount.value)], { type: 'application/json' })
    formData.append('discount', jsonBlob)

    if (selectedFile.value) {
      formData.append('image', selectedFile.value)
    }

    const res = await axios.post('http://localhost:8080/api/discounts', formData)

    if (res.data.code === 200) {
      Swal.fire({
        title: '成功',
        html: '<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#67c23a" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg><p style="margin-top:12px;">資料已儲存</p></div>',
        confirmButtonColor: '#409eff',
        confirmButtonText: '確定',
      })
      isEditModalOpen.value = false
      fetchDiscounts()
    } else {
      Swal.fire({
        title: '失敗',
        html: `<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg><p style="margin-top:12px;">${res.data.message}</p></div>`,
        confirmButtonColor: '#409eff',
        confirmButtonText: '確定',
      })
    }
  } catch (err) {
    Swal.fire({
      title: '儲存失敗',
      html: '<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg><p style="margin-top:12px;">請檢查資料完整性</p></div>',
      confirmButtonColor: '#409eff',
      confirmButtonText: '確定',
    })
  }
}

// 7. 刪除
const deleteDiscount = (id) => {
  Swal.fire({
    title: '確定刪除？',
    html: '<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#e6a23c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg><p style="margin-top:12px;">刪除後無法還原！</p></div>',
    showCancelButton: true,
    confirmButtonColor: '#f56c6c',
    cancelButtonColor: '#909399',
    confirmButtonText: '確定刪除',
    cancelButtonText: '取消',
  }).then(async (result) => {
    if (result.isConfirmed) {
      try {
        await axios.delete(`http://localhost:8080/api/discounts/${id}`)
        Swal.fire({
          title: '已刪除',
          html: '<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#67c23a" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg><p style="margin-top:12px;">優惠券已移除</p></div>',
          confirmButtonColor: '#409eff',
          confirmButtonText: '確定',
        })
        fetchDiscounts()
      } catch {
        Swal.fire({
          title: '錯誤',
          html: '<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg><p style="margin-top:12px;">刪除失敗</p></div>',
          confirmButtonColor: '#409eff',
          confirmButtonText: '確定',
        })
      }
    }
  })
}
// 一鍵填入測試資料
const handleQuickFill = () => {
  const templates = [
    { name: '新春限時 8 折券', desc: '全店消費不限金額即可享有 8 折優惠', points: 50 },
    { name: '拿鐵咖啡買一送一', desc: '限中杯以上拿鐵，每人限領一次', points: 30 },
    { name: '百元抵用券', desc: '消費滿 500 元即可折抵 100 元', points: 100 },
    { name: '下午茶套餐組合優惠', desc: '指定蛋糕加飲料現折 50 元', points: 20 },
    { name: 'VIP 會員獨享禮', desc: '憑此券兌換精美好禮一份', points: 150 }
  ];

  const randomTpl = templates[Math.floor(Math.random() * templates.length)];
  const now = new Date();
  const future = new Date();
  future.setDate(now.getDate() + 60);
  const formatDate = (date) => date.toISOString().split('T')[0];

  editingDiscount.value = {
    ...editingDiscount.value,
    couponName: randomTpl.name,
    couponDescription: randomTpl.desc,
    pointsRequired: randomTpl.points,
    startDate: formatDate(now),
    endDate: formatDate(future),
    couponStatus: 1,
    merchantId: merchants.value.length > 0 ? merchants.value[merchants.value.length - 1].merchantId : null
  };
};

onMounted(() => {
  fetchDiscounts()
  fetchMerchants()
})
</script>

<style scoped>
/* ========== 頁面容器 ========== */
.discount-list-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  padding: 20px;
}

/* ========== 頁面標題區 ========== */
.page-title-box {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  flex-wrap: wrap;
}
/*========= 標題圖示 ========== */
.title-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
  background: linear-gradient(135deg, #e6a23c 0%, #f3d19e 100%);
  transition: all 0.4s ease;
  box-shadow: 0 4px 15px rgba(230, 162, 60, 0.3);
}

/*========= 標題內容 ========== */
.title-icon:hover {
  transform: scale(1.1) rotate(10deg);
}

.title-content {
  flex: 1;
  min-width: 200px;
}

.title-content h1 {
  margin: 0;
  font-size: 1.7rem;
  font-weight: 700;
  color: #303133;
}

.title-content .subtitle {
  margin: 6px 0 0;
  font-size: 0.9rem;
  color: #909399;
}

.title-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  border-radius: 12px;
  font-weight: 600;
  padding: 12px 24px;
  transition: all 0.3s ease;
}

.add-btn {
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
  border: none;
  box-shadow: 0 4px 15px rgba(103, 194, 58, 0.3);
}

.add-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(103, 194, 58, 0.4);
}

/* ========== 統計卡片 ========== */
.stat-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
  background: white;
  border-radius: 14px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.06);
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  overflow: hidden;
  margin-bottom: 16px;
}

.stat-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.12);
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  border-radius: 4px 0 0 4px;
}

.total-card::before {
  background: linear-gradient(135deg, #e6a23c 0%, #f3d19e 100%);
}
.active-card::before {
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
}
.pending-card::before {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
}
.ended-card::before {
  background: linear-gradient(135deg, #909399 0%, #c0c4cc 100%);
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
}

.total-card .stat-icon {
  background: linear-gradient(135deg, #e6a23c 0%, #f3d19e 100%);
}
.active-card .stat-icon {
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
}
.pending-card .stat-icon {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
}
.ended-card .stat-icon {
  background: linear-gradient(135deg, #909399 0%, #c0c4cc 100%);
}

.stat-info h3 {
  margin: 0;
  font-size: 1.8rem;
  font-weight: 700;
  color: #303133;
}

.stat-info span {
  font-size: 0.85rem;
  color: #909399;
}

/* ========== 表格卡片 ========== */
.table-card {
  border-radius: 16px;
  overflow: hidden;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.card-header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #e6a23c 0%, #f3d19e 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
}

.header-text {
  font-weight: 600;
  font-size: 1.1rem;
  color: #303133;
}

/* ========== 篩選區 ========== */
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 12px;
}

.filter-input {
  width: 300px;
}

.filter-input :deep(.el-input__wrapper) {
  border-radius: 10px;
}

.search-btn {
  border-radius: 10px;
  font-weight: 500;
  background: linear-gradient(135deg, #e6a23c 0%, #f3d19e 100%);
  border: none;
}

.search-btn:hover {
  background: linear-gradient(135deg, #d48806 0%, #e6a23c 100%);
}

/* ========== 表格樣式 ========== */
.modern-table {
  border-radius: 12px;
  overflow: hidden;
}

.id-tag {
  font-size: 12px;
  color: #909399;
  font-weight: 600;
}

.coupon-img {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.coupon-img:hover {
  transform: scale(1.1);
}

.no-img {
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 18px;
}

.coupon-info-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.coupon-name {
  font-weight: 600;
  color: #303133;
}

.coupon-desc {
  font-size: 12px;
  color: #909399;
  max-width: 200px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.merchant-tag {
  border-radius: 20px;
}

.points-text {
  font-weight: 700;
  color: #e6a23c;
  font-size: 16px;
}

.points-text small {
  font-weight: 400;
  font-size: 12px;
}

.status-tag {
  border-radius: 20px;
  padding: 4px 12px;
}

.date-cell {
  font-size: 12px;
  color: #606266;
  display: flex;
  align-items: center;
}

/* ========== 彈窗樣式 ========== */
.modern-dialog :deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

.modern-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, #e6a23c 0%, #f3d19e 100%);
  color: white;
  padding: 16px 20px;
}

.modern-dialog :deep(.el-dialog__title) {
  color: white;
  font-weight: 600;
}

.modern-dialog :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: white;
}

.modern-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #303133;
}

.modern-form :deep(.el-input__wrapper),
.modern-form :deep(.el-textarea__inner),
.modern-form :deep(.el-select .el-input__wrapper) {
  border-radius: 10px;
}

.image-preview {
  display: flex;
  justify-content: center;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 12px;
  margin-top: 16px;
}

.preview-img {
  max-height: 150px;
  border-radius: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.save-btn {
  background: linear-gradient(135deg, #e6a23c 0%, #f3d19e 100%);
  border: none;
  border-radius: 10px;
  padding: 10px 24px;
}

.save-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(230, 162, 60, 0.4);
}

/* ========== 動畫效果 ========== */
.fade-slide-enter-active {
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.fade-slide-leave-active {
  transition: all 0.3s ease-in;
}

.fade-slide-enter-from {
  transform: translateY(30px);
  opacity: 0;
}

.fade-slide-leave-to {
  transform: translateY(-20px);
  opacity: 0;
}

/* ========== 間距工具類 ========== */
.mb-4 {
  margin-bottom: 1.5rem;
}
.mr-1 {
  margin-right: 4px;
}
.mr-2 {
  margin-right: 8px;
}

/* ========== 顏色工具類 ========== */
.text-primary {
  color: #409eff;
}
.text-success {
  color: #67c23a;
}
.text-warning {
  color: #e6a23c;
}
.text-danger {
  color: #f56c6c;
}

/* ========== 響應式設計 ========== */
@media (max-width: 768px) {
  .page-title-box {
    flex-direction: column;
    text-align: center;
    gap: 16px;
  }

  .title-actions {
    width: 100%;
    justify-content: center;
  }

  .filter-bar {
    flex-direction: column;
  }

  .filter-input {
    width: 100%;
  }
 .upload-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.preview-box {
  position: relative;
  width: 100%;
  height: 120px; /* 固定預覽高度 */
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
}

.preview-img-fill {
  width: 100%;
  height: 100%;
}

.preview-overlay {
  position: absolute;
  top: 4px;
  right: 4px;
  background: rgba(0,0,0,0.5);
  color: white;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  font-size: 12px;
}
}
</style>
