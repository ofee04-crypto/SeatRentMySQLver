<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import Swal from 'sweetalert2'
import { Search } from '@element-plus/icons-vue'

const router = useRouter()

// 1. 狀態管理 - 確保初始化是空陣列 []，絕對不給 null
const allRawMerchants = ref([]) 
const loading = ref(false)
const searchQuery = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

// 2. 分頁狀態
const currentPage = ref(1)
const pageSize = ref(10)

// 3. 搜尋過濾邏輯 - 加入防錯機制 (?.)
const filteredMerchants = computed(() => {
  const list = allRawMerchants.value || [] // 如果是 undefined 就給空陣列
  if (!searchQuery.value) return list
  const q = searchQuery.value.toLowerCase()
  return list.filter(m => 
    (m.merchantName?.toLowerCase().includes(q)) ||
    (m.merchantAddress?.toLowerCase().includes(q))
  )
})

// 4. 強行分頁邏輯 - 你的 el-table :data 綁定這個
const merchantList = computed(() => {
  const list = filteredMerchants.value || []
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return list.slice(start, end)
})

// 5. 分頁器總筆數 - 加入防錯，確保不報 length 錯誤
const totalItems = computed(() => {
  return (filteredMerchants.value && filteredMerchants.value.length) ? filteredMerchants.value.length : 0
})

// 營運中商家統計 - 加入防錯
const activeMerchantsCount = computed(() => {
  const list = allRawMerchants.value || []
  return list.filter((m) => m.merchantStatus === 1).length
})

// 表單狀態
const form = ref({
  merchantId: null,
  merchantName: '',
  merchantPhone: '',
  merchantEmail: '',
  merchantAddress: '',
  merchantStatus: 1,
})

const rules = {
  merchantName: [{ required: true, message: '請輸入商家名稱', trigger: 'blur' }],
  merchantAddress: [{ required: true, message: '請輸入商家地址', trigger: 'blur' }]
}

// 獲取資料
const fetchMerchants = async () => {
  loading.value = true
  try {
    const res = await axios.get('http://localhost:8080/api/merchants')
    // 嚴格檢查回傳格式
    let data = []
    if (Array.isArray(res.data)) {
      data = res.data
    } else if (res.data && res.data.data) {
      data = res.data.data
    }
    allRawMerchants.value = data
  } catch (error) {
    console.error('獲取商家失敗:', error)
    Swal.fire('錯誤', '無法載入商家資料', 'error')
  } finally {
    loading.value = false
  }
}

// 搜尋與分頁處理
const handleSearch = () => {
  currentPage.value = 1
}

const resetSearch = () => {
  searchQuery.value = ''
  currentPage.value = 1
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
}

const handleCurrentChange = (val) => {
  currentPage.value = val
}

const goToMerchantCoupons = (merchantId) => {
  if (!merchantId) return
  router.push({ path: '/mall', query: { merchantId: merchantId } })
}

const openAddModal = () => {
  isEdit.value = false
  form.value = { merchantId: null, merchantName: '', merchantPhone: '', merchantEmail: '', merchantAddress: '', merchantStatus: 1 }
  dialogVisible.value = true
}

const openEditModal = (row) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    const isDuplicate = allRawMerchants.value.some(m => 
      (m.merchantName === form.value.merchantName || m.merchantAddress === form.value.merchantAddress) &&
      m.merchantId !== form.value.merchantId
    )
    if (isDuplicate) {
      Swal.fire('無法儲存', '店名或地址重複！', 'warning')
      return
    }
    try {
      if (isEdit.value) {
        await axios.put(`http://localhost:8080/api/merchants/${form.value.merchantId}`, form.value)
      } else {
        await axios.post('http://localhost:8080/api/merchants', form.value)
      }
      dialogVisible.value = false
      Swal.fire('成功', isEdit.value ? '資料已更新' : '商家已新增', 'success')
      fetchMerchants()
    } catch (error) {
      Swal.fire('錯誤', '操作失敗', 'error')
    }
  })
}

const deleteMerchant = (id, name) => {
  Swal.fire({
    title: '確定刪除？',
    text: `將刪除商家：${name}`,
    icon: 'warning',
    showCancelButton: true,
    confirmButtonText: '確定',
    cancelButtonText: '取消',
  }).then(async (result) => {
    if (result.isConfirmed) {
      try {
        await axios.delete(`http://localhost:8080/api/merchants/${id}`)
        Swal.fire('已刪除', '', 'success')
        fetchMerchants()
      } catch (error) {
        Swal.fire('錯誤', '刪除失敗', 'error')
      }
    }
  })
}

const handleQuickFill = () => {
  form.value = {
    merchantName: '開心咖啡館',
    merchantPhone: '02-23456789',
    merchantEmail: 'happy.coffee@example.com',
    merchantAddress: '台北市中正區忠孝西路一段 118 號',
    merchantStatus: 1,
  }
}

onMounted(fetchMerchants)
</script>
<template>
  <div class="merchant-list-container">
    <section class="content-header">
      <div class="container-fluid">
        <div class="row align-items-center">
          <div class="col-12">
            <div class="page-title-box">
              <div class="title-icon">
                <i class="fas fa-store"></i>
              </div>
              <div class="title-content">
                <h1>商家管理系統</h1>
                <p class="subtitle">管理合作商家資訊與狀態</p>
              </div>
              <div class="title-actions">
                <el-button type="success" class="action-btn add-btn" @click="openAddModal()">
                  <i class="fas fa-plus mr-1"></i> 新增商家
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
                  <div class="stat-icon"><i class="fas fa-store"></i></div>
                  <div class="stat-info">
                    <h3>{{ totalItems }}</h3>
                    <span>系統總筆數</span>
                  </div>
                </div>
              </el-col>
              <el-col :xs="12" :sm="6">
                <div class="stat-card active-card">
                  <div class="stat-icon"><i class="fas fa-check-circle"></i></div>
                  <div class="stat-info">
                    <h3>{{ activeMerchantsCount }}</h3>
                    <span>合作營運中</span>
                  </div>
                </div>
              </el-col>
              <el-col :xs="12" :sm="6">
                <div class="stat-card inactive-card">
                  <div class="stat-icon"><i class="fas fa-pause-circle"></i></div>
                  <div class="stat-info">
                    <h3>{{ totalItems - activeMerchantsCount }}</h3>
                    <span>停止合作</span>
                  </div>
                </div>
              </el-col>
            </el-row>

            <el-card shadow="hover" class="table-card">
              <template #header>
                <div class="card-header-content">
                  <div class="header-left">
                    <span class="header-icon"><i class="fas fa-list"></i></span>
                    <span class="header-text">商家列表</span>
                    <el-tag type="primary" effect="light" size="small" round
                      >{{ totalItems }} 筆</el-tag
                    >
                  </div>
                </div>
              </template>

              <div class="filter-bar">
                <el-input
                  v-model="searchQuery"
                  placeholder="搜尋名稱或地址..."
                  :prefix-icon="Search"
                  clearable
                  class="filter-input"
                  @keyup.enter="handleSearch"
                  @clear="handleSearch"
                />
                <el-button type="primary" @click="handleSearch" class="search-btn">
                  <i class="fas fa-search mr-1"></i> 搜尋
                </el-button>
                <el-button @click="resetSearch" class="reset-btn">
                  <i class="fas fa-redo mr-1"></i> 重置
                </el-button>
              </div>

              <el-table
                :data="merchantList"
                v-loading="loading"
                stripe
                highlight-current-row
                style="width: 100%"
                :header-cell-style="{ background: '#f5f7fa', fontWeight: 'bold', color: '#303133' }"
                class="modern-table"
              >
                <el-table-column prop="merchantId" label="ID" width="80" align="center">
                  <template #default="{ row }">
                    <span class="id-tag">#{{ row.merchantId }}</span>
                  </template>
                </el-table-column>

                <el-table-column prop="merchantName" label="商家名稱" min-width="150">
                  <template #default="{ row }">
                    <div
                      class="merchant-name-cell click-link"
                      @click="goToMerchantCoupons(row.merchantId)"
                      title="點擊前往商城查看優惠券"
                    >
                      <i class="fas fa-store text-primary mr-2"></i>
                      <span class="name-text">{{ row.merchantName }}</span>
                    </div>
                  </template>
                </el-table-column>

                <el-table-column prop="merchantAddress" label="地址" min-width="200">
                  <template #default="{ row }">
                    <div class="address-cell">
                      <i class="fas fa-map-marker-alt text-warning mr-2"></i>
                      <span>{{ row.merchantAddress || '-' }}</span>
                    </div>
                  </template>
                </el-table-column>
                
                <el-table-column prop="merchantPhone" label="電話" width="140">
                  <template #default="{ row }">
                    <span class="phone-text">
                      <i class="fas fa-phone mr-1"></i>{{ row.merchantPhone || '-' }}
                    </span>
                  </template>
                </el-table-column>

                <el-table-column prop="merchantStatus" label="狀態" width="120" align="center">
                  <template #default="{ row }">
                    <el-tag
                      :type="row.merchantStatus === 1 ? 'success' : 'danger'"
                      effect="light"
                      size="small"
                      class="status-tag"
                    >
                      <i :class="row.merchantStatus === 1 ? 'fas fa-check-circle' : 'fas fa-times-circle'" class="mr-1"></i>
                      {{ row.merchantStatus === 1 ? '營運中' : '停用中' }}
                    </el-tag>
                  </template>
                </el-table-column>

                <el-table-column label="操作" width="160" align="center" fixed="right">
                  <template #default="{ row }">
                    <el-button-group>
                      <el-button size="small" type="primary" @click="openEditModal(row)">
                        <i class="fas fa-edit"></i>
                      </el-button>
                      <el-button size="small" type="danger" @click="deleteMerchant(row.merchantId, row.merchantName)">
                        <i class="fas fa-trash-alt"></i>
                      </el-button>
                    </el-button-group>
                  </template>
                </el-table-column>
              </el-table>

              <div class="pagination-wrapper">
                <el-pagination
                  v-model:current-page="currentPage"
                  v-model:page-size="pageSize"
                  :page-sizes="[10, 20, 50]"
                  :total="totalItems"
                  layout="total, sizes, prev, pager, next, jumper"
                  @size-change="handleSizeChange"
                  @current-change="handleCurrentChange"
                  background
                />
              </div>
            </el-card>
          </div>
        </transition>
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? `編輯商家 #${form.merchantId}` : '新增商家'"
      width="600px"
      class="modern-dialog"
      :close-on-click-modal="false"
    >
      <el-form 
        ref="formRef" 
        :model="form" 
        :rules="rules" 
        label-width="80px" 
        label-position="top" 
        class="modern-form"
      >
        <div style="margin-bottom: 15px; text-align: right;">
          <el-button type="info" plain size="small" @click="handleQuickFill" style="border-style: dashed;">
            <i class="fas fa-magic mr-1"></i> 一鍵填入測試資料
          </el-button>
        </div>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商家名稱" prop="merchantName">
              <el-input v-model="form.merchantName" placeholder="請輸入商家名稱">
                <template #prefix><i class="fas fa-store"></i></template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="聯絡電話">
              <el-input v-model="form.merchantPhone" placeholder="請輸入聯絡電話">
                <template #prefix><i class="fas fa-phone"></i></template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="電子信箱">
          <el-input v-model="form.merchantEmail" placeholder="請輸入電子信箱">
            <template #prefix><i class="fas fa-envelope"></i></template>
          </el-input>
        </el-form-item>
        <el-form-item label="商家地址" prop="merchantAddress">
          <el-input
            v-model="form.merchantAddress"
            type="textarea"
            :rows="2"
            placeholder="請輸入商家地址"
          />
        </el-form-item>
        <el-form-item label="營運狀態">
          <el-select v-model="form.merchantStatus" placeholder="請選擇狀態" style="width: 100%">
            <el-option :value="1" label="營運中">營運中</el-option>
            <el-option :value="0" label="停用中">停用中</el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" class="save-btn">
            儲存資料
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* ========== 頁面容器 ========== */
.merchant-list-container {
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

.title-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
  transition: all 0.4s ease;
  box-shadow: 0 4px 15px rgba(64, 158, 255, 0.3);
}

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
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
}
.active-card::before {
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
}
.inactive-card::before {
  background: linear-gradient(135deg, #f56c6c 0%, #f89898 100%);
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
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
}
.active-card .stat-icon {
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
}
.inactive-card .stat-icon {
  background: linear-gradient(135deg, #f56c6c 0%, #f89898 100%);
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
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
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

.search-btn,
.reset-btn {
  border-radius: 10px;
  font-weight: 500;
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

.merchant-name-cell {
  display: flex;
  align-items: center;
}

/* [新增] 可點擊的樣式 */
.click-link {
  cursor: pointer;
  transition: all 0.2s;
  padding: 4px 0;
}

.click-link:hover .name-text {
  color: #409eff;
  text-decoration: underline;
}

.name-text {
  font-weight: 600;
  color: #303133;
  transition: color 0.2s;
}

.address-cell {
  display: flex;
  align-items: center;
  color: #606266;
}

.phone-text {
  color: #606266;
  font-size: 13px;
}

.email-text {
  color: #606266;
  font-size: 13px;
}

.status-tag {
  border-radius: 20px;
  padding: 4px 12px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  padding-bottom: 20px;
}

/* ========== 彈窗樣式 ========== */
.modern-dialog :deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

.modern-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
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

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.save-btn {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
  border: none;
  border-radius: 10px;
  padding: 10px 24px;
}

.save-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(64, 158, 255, 0.4);
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
}
</style>
