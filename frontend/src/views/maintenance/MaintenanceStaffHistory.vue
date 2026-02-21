<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import maintenanceApi from '@/api/modules/maintenance'
import Swal from 'sweetalert2'
import { useRouter } from 'vue-router'
import { usePagination } from '@/composables/maintenance/usePagination'

const router = useRouter()
const staffList = ref([])
const searchText = ref('')
const loading = ref(false)
const pageVisible = ref(false)

// 新增：日期格式化小工具
const formatDate = (dateVal, row = {}) => {
  // 優先順序：傳入的值 > updateTime > updated_at > createTime > createdAt
  // 這裡假設如果沒有更新時間，可能要看建立時間 (例如這筆歷史資料的建立時間)
  const targetDate =
    dateVal || row.updatedAt || row.updateTime || row.updated_at || row.createTime || row.createdAt

  if (!targetDate) return '-'

  const date = new Date(targetDate)
  // 檢查是否為有效日期
  if (isNaN(date.getTime())) return '-'

  return date.toLocaleDateString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
}

// 專門從備註裡抓出 [停用日期：...] 的工具
const extractDateFromNote = (noteStr) => {
  if (!noteStr) return '-'

  // 使用正則表達式抓取 [停用日期：xxxx/xx/xx]
  const match = noteStr.match(/\[停用日期：(.*?)\]/)

  if (match && match[1]) {
    return match[1] // 回傳抓到的日期，例如 "2026/01/29"
  }

  return '-' // 沒抓到就顯示槓槓
}

// ✅ [新增] 解析備註中的歷史紀錄 (將備註字串轉為時間軸陣列)
const parseHistoryFromNote = (note) => {
  if (!note) return []

  const history = []

  // 使用正則表達式抓取所有中括號內的內容，例如 [停用日期：2026/01/29]
  // flag 'g' 代表抓取全部，不只抓第一個
  const regex = /\[(.*?)\]/g
  let match

  // 迴圈找出所有的標籤
  while ((match = regex.exec(note)) !== null) {
    const content = match[1] // 取得括號內的文字

    // 預設樣式 (灰色)
    let color = '#909399'
    let iconColor = '#fff'
    let borderColor = '#909399'

    // 根據關鍵字決定顏色
    if (content.includes('停用')) {
      color = '#f56c6c' // 紅色文字
      borderColor = '#f56c6c' // 紅色圓點
    } else if (content.includes('復職')) {
      color = '#67c23a' // 綠色文字
      borderColor = '#67c23a' // 綠色圓點
    }

    history.push({
      content: content,
      color: color,
      borderColor: borderColor,
    })
  }

  // 如果希望最新的紀錄顯示在最上面，可以使用 history.reverse()
  return history
}

// 取得已停用 (Inactive) 的資料
const fetchHistory = async () => {
  try {
    loading.value = true
    const res = await maintenanceApi.getInactiveStaff()
    console.log('API回傳的第一筆資料:', res.data[0])
    staffList.value = res.data
  } catch {
    // 錯誤已由 http.js 攔截器處理
  } finally {
    loading.value = false
  }
}

// 前端搜尋
const filteredList = computed(() => {
  const key = searchText.value.trim().toLowerCase()
  if (!key) return staffList.value
  return staffList.value.filter(
    (s) =>
      (s.staffName || '').toLowerCase().includes(key) ||
      (s.staffCompany || '').toLowerCase().includes(key),
  )
})

// 使用 usePagination composable
const {
  currentPage,
  pageSize,
  paginatedList,
  total: paginationTotal,
  showPagination,
  resetPagination,
} = usePagination(filteredList, { defaultPageSize: 10 })

// 搜尋時重置分頁
watch(searchText, () => {
  resetPagination()
})

// 查看詳情彈窗
// ✅ [修改] 查看詳情彈窗 (包含時間軸顯示)
const viewDetail = (row) => {
  // 1. 先抓出備註原始字串
  const noteStr = row.staffNote || ''

  // 2. 利用剛剛寫的工具，解析出歷史紀錄陣列
  const historyList = parseHistoryFromNote(noteStr)

  // 3. 準備原始備註 (移除掉所有 [xxx] 標籤，只留純文字備註)
  // replace 語法會把所有中括號標籤都洗掉
  const originalNote = noteStr.replace(/\[.*?\]/g, '').trim() || '無其他詳細備註'

  // 4. 建構時間軸的 HTML (因為 Swal 只能吃 HTML 字串)
  let timelineHtml = ''

  if (historyList.length > 0) {
    // 如果有紀錄，開始組裝 HTML
    timelineHtml = '<div style="text-align: left; padding: 0 10px; margin-bottom: 20px;">'
    timelineHtml +=
      '<p style="font-size: 13px; color: #606266; font-weight: bold; margin-bottom: 12px;"><i class="fas fa-stream mr-1"></i> 異動歷程</p>'
    timelineHtml +=
      '<ul style="list-style: none; padding: 0; margin: 0; border-left: 2px solid #e4e7ed; margin-left: 8px;">'

    historyList.forEach((item) => {
      timelineHtml += `
        <li style="margin-bottom: 20px; padding-left: 24px; position: relative;">
          <div style="position: absolute; left: -7px; top: 2px; width: 12px; height: 12px; border-radius: 50%; background: #fff; border: 3px solid ${item.borderColor};"></div>
          <div style="font-size: 14px; color: #303133; font-weight: 500;">
            <span style="color: ${item.color}">${item.content}</span>
          </div>
        </li>
      `
    })

    timelineHtml += '</ul></div>'
  } else {
    // 如果沒有紀錄
    timelineHtml = `
      <div style="margin-bottom: 20px; padding: 12px; background: #f4f4f5; border-radius: 8px; color: #909399; font-size: 13px; border: 1px dashed #dcdfe6; text-align: center;">
        <i class="fas fa-info-circle mr-1"></i> 尚無異動紀錄
      </div>
    `
  }

  // 5. 顯示彈窗
  Swal.fire({
    // 標題區：姓名 + 公司
    title: `<div style="display:flex; flex-direction:column; align-items:center; padding-bottom: 10px; border-bottom: 1px solid #ebeef5;">
              <span style="font-size: 1.6rem; color: #303133;">${row.staffName}</span>
              <span style="font-size: 0.9rem; color: #909399; font-weight: normal; margin-top: 6px;">
                <i class="fas fa-building mr-1"></i>${row.staffCompany || '無公司資訊'}
              </span>
            </div>`,
    html: `
      <div style="text-align: left; padding: 15px 5px 0;">
        
        ${timelineHtml}

        <div style="margin-bottom: 16px;">
           <p style="margin: 0 0 8px 0; color: #606266; font-size: 13px; font-weight: bold;">基本資料</p>
           <div style="padding: 12px; background: #f5f7fa; border-radius: 8px;">
            <p style="margin: 6px 0; font-size: 14px; color: #606266;">
              <i class="fas fa-phone mr-2 text-success" style="width:20px; text-align:center;"></i>
              ${row.staffPhone || '未填寫'}
            </p>
            <p style="margin: 6px 0; font-size: 14px; color: #606266;">
              <i class="fas fa-envelope mr-2 text-warning" style="width:20px; text-align:center;"></i>
              ${row.staffEmail || '未填寫'}
            </p>
          </div>
        </div>

        <div>
          <p style="margin: 0 0 8px 0; color: #606266; font-size: 13px; font-weight: bold;">
            <i class="fas fa-sticky-note mr-1"></i> 其他備註
          </p>
          <div style="padding: 12px; background: #fff; border: 1px solid #dcdfe6; border-radius: 8px; color: #606266; font-size: 14px; line-height: 1.6; white-space: pre-line;">
            ${originalNote}
          </div>
        </div>

      </div>
    `,
    confirmButtonText: '關閉',
    confirmButtonColor: '#909399',
    width: 480, // 稍微加寬一點讓時間軸好看
    showClass: { popup: 'animate__animated animate__fadeInDown animate__faster' },
    hideClass: { popup: 'animate__animated animate__fadeOutUp animate__faster' },
  })
}

// 恢復人員功能
// 修改 handleRestore 函式
const handleRestore = async (row) => {
  const result = await Swal.fire({
    title: '確認恢復人員？',
    html: `<p>即將恢復 <b class="text-primary">${row.staffName}</b> 的在職狀態</p>`,
    icon: 'question',
    showCancelButton: true,
    confirmButtonColor: '#67c23a',
    cancelButtonColor: '#909399',
    confirmButtonText: '<i class="fas fa-undo mr-1"></i> 確認恢復',
    cancelButtonText: '取消',
    showClass: { popup: 'animate__animated animate__bounceIn' },
  })

  if (result.isConfirmed) {
    try {
      // --- 修正開始：清洗備註 ---
      let cleanNote = row.staffNote || ''
      // 1. 移除 [停用原因：...] (使用正則表達式 global 替換)
      cleanNote = cleanNote.replace(/\[停用原因：.*?\]/g, '')

      // 2. 移除 [停用日期：...] (使用正則表達式 global 替換)
      cleanNote = cleanNote.replace(/\[停用日期：.*?\]/g, '')

      // 3. 移除多餘空白行與前後空白
      cleanNote = cleanNote.trim()

      // 傳送清洗過的備註 (cleanNote) 回後端
      await maintenanceApi.updateStaff(row.staffId, {
        ...row,
        isActive: true,
        staffNote: cleanNote,
      })

      await Swal.fire({
        icon: 'success',
        title: '恢復成功！',
        html: `<span class="text-success"><b>${row.staffName}</b> 已恢復在職狀態</span>`,
        timer: 1000,
        timerProgressBar: true,
        showConfirmButton: false,
        showClass: { popup: 'animate__animated animate__tada' },
      })
      fetchHistory()
    } catch {
      // 錯誤已由攔截器處理
    }
  }
}

const handleBack = () => {
  router.push('/admin/staff-list')
}

onMounted(() => {
  fetchHistory()
  setTimeout(() => (pageVisible.value = true), 100)
})
</script>

<template>
  <div class="history-container">
    <!-- 頁面標題區 -->
    <section class="content-header">
      <div class="container-fluid">
        <transition name="slide-down" appear>
          <div class="page-title-box">
            <div class="title-icon">
              <i class="fas fa-archive"></i>
            </div>
            <div class="title-content">
              <h1>維護人員歷史紀錄</h1>
              <p class="subtitle">檢視已離職或停用的人員資料</p>
            </div>
            <div class="title-actions">
              <el-button type="primary" plain @click="handleBack" class="back-btn">
                <i class="fas fa-arrow-left mr-2"></i> 返回列表
              </el-button>
            </div>
          </div>
        </transition>
      </div>
    </section>

    <!-- 主內容區 -->
    <section class="content">
      <div class="container-fluid">
        <transition name="zoom-fade" appear>
          <div v-show="pageVisible">
            <!-- 統計卡片 -->
            <el-row :gutter="20" class="mb-4">
              <el-col :xs="24" :sm="8" :md="6">
                <div class="stat-card archive-card">
                  <div class="stat-icon">
                    <i class="fas fa-user-slash"></i>
                  </div>
                  <div class="stat-info">
                    <h3>{{ staffList.length }}</h3>
                    <span>已封存人員</span>
                  </div>
                </div>
              </el-col>
              <el-col :xs="24" :sm="8" :md="6">
                <div class="stat-card filter-card">
                  <div class="stat-icon">
                    <i class="fas fa-filter"></i>
                  </div>
                  <div class="stat-info">
                    <h3>{{ filteredList.length }}</h3>
                    <span>篩選結果</span>
                  </div>
                </div>
              </el-col>
            </el-row>

            <!-- 資料表格卡片 -->
            <el-card shadow="hover" class="table-card">
              <template #header>
                <div class="card-header-content">
                  <div class="header-left">
                    <span class="header-icon">
                      <i class="fas fa-history"></i>
                    </span>
                    <span class="header-text">已封存人員清單</span>
                    <el-tag type="info" effect="plain" size="small" class="ml-2">
                      共 {{ filteredList.length }} 筆
                    </el-tag>
                  </div>
                  <div class="header-right">
                    <el-input
                      v-model="searchText"
                      placeholder="搜尋姓名或公司..."
                      prefix-icon="Search"
                      clearable
                      class="search-input"
                    />
                  </div>
                </div>
              </template>

              <!-- 骨架屏 -->
              <el-skeleton :rows="6" animated v-if="loading" />

              <!-- 資料表格 -->
              <el-table
                v-else
                :data="paginatedList"
                stripe
                highlight-current-row
                style="width: 100%"
                class="custom-table"
                :row-class-name="tableRowClassName"
              >
                <el-table-column prop="staffId" label="ID" width="70" align="center" sortable>
                  <template #default="{ row }">
                    <el-tag effect="plain" size="small" type="info">{{ row.staffId }}</el-tag>
                  </template>
                </el-table-column>

                <el-table-column prop="staffName" label="姓名" width="140" sortable>
                  <template #default="{ row }">
                    <div class="name-cell">
                      <el-avatar :size="32" class="name-avatar">
                        {{ row.staffName?.charAt(0) || '?' }}
                      </el-avatar>
                      <span class="name-text">{{ row.staffName }}</span>
                    </div>
                  </template>
                </el-table-column>

                <el-table-column prop="staffCompany" label="公司" min-width="160" sortable>
                  <template #default="{ row }">
                    <span v-if="row.staffCompany">
                      <i class="fas fa-building mr-1 text-primary"></i>
                      {{ row.staffCompany }}
                    </span>
                    <span v-else class="text-muted">-</span>
                  </template>
                </el-table-column>

                <el-table-column prop="staffPhone" label="電話" min-width="150">
                  <template #default="{ row }">
                    <span v-if="row.staffPhone">
                      <i class="fas fa-phone mr-1 text-success"></i>
                      {{ row.staffPhone }}
                    </span>
                    <span v-else class="text-muted">-</span>
                  </template>
                </el-table-column>

                <el-table-column
                  prop="staffEmail"
                  label="Email"
                  min-width="200"
                  show-overflow-tooltip
                >
                  <template #default="{ row }">
                    <span class="email-cell">
                      <i class="fas fa-envelope mr-1 text-warning"></i>
                      {{ row.staffEmail }}
                    </span>
                  </template>
                </el-table-column>

                <el-table-column label="停用日期" width="120" sortable prop="updatedAt">
                  <template #default="{ row }">
                    <div style="display: flex; flex-direction: column; align-items: flex-start">
                      <span style="font-size: 13px; color: #606266">
                        <i class="far fa-calendar-alt mr-1 text-muted"></i>
                        {{ extractDateFromNote(row.staffNote) }}
                      </span>
                    </div>
                  </template>
                </el-table-column>

                <el-table-column label="狀態" width="110" align="center">
                  <template #default>
                    <el-tag type="info" effect="dark" class="status-tag">
                      <i class="fas fa-ban mr-1"></i> 已離職
                    </el-tag>
                  </template>
                </el-table-column>

                <el-table-column label="操作" width="160" align="center" fixed="right">
                  <template #default="{ row }">
                    <el-button-group>
                      <el-tooltip content="查看詳情" placement="top">
                        <el-button
                          type="info"
                          size="small"
                          circle
                          @click="viewDetail(row)"
                          class="action-btn"
                        >
                          <i class="fas fa-eye"></i>
                        </el-button>
                      </el-tooltip>
                      <el-tooltip content="恢復在職" placement="top">
                        <el-button
                          type="success"
                          size="small"
                          circle
                          @click="handleRestore(row)"
                          class="action-btn"
                        >
                          <i class="fas fa-undo"></i>
                        </el-button>
                      </el-tooltip>
                    </el-button-group>
                  </template>
                </el-table-column>

                <!-- 空狀態 -->
                <template #empty>
                  <el-empty description="暫無歷史資料">
                    <template #image>
                      <div class="empty-icon">
                        <i class="fas fa-inbox"></i>
                      </div>
                    </template>
                    <el-button type="primary" plain @click="handleBack"> 返回人員列表 </el-button>
                  </el-empty>
                </template>
              </el-table>

              <!-- 分頁器 -->
              <div class="pagination-wrapper" v-if="showPagination">
                <el-pagination
                  v-model:current-page="currentPage"
                  v-model:page-size="pageSize"
                  :page-sizes="[5, 10, 20, 50]"
                  :total="paginationTotal"
                  layout="total, sizes, prev, pager, next, jumper"
                  background
                />
              </div>
            </el-card>
          </div>
        </transition>
      </div>
    </section>
  </div>
</template>

<style scoped>
.history-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f0f2f5 0%, #e2e6ea 100%);
  padding-bottom: 40px;
}

.content-header {
  padding: 20px 1rem;
}

.page-title-box {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
}

.title-icon {
  width: 60px;
  height: 60px;
  border-radius: 14px;
  background: linear-gradient(135deg, #909399 0%, #c0c4cc 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  color: white;
  transition: all 0.3s ease;
}

.title-icon:hover {
  transform: scale(1.1) rotate(-5deg);
}

.title-content {
  flex: 1;
}

.title-content h1 {
  margin: 0;
  font-size: 1.6rem;
  font-weight: 700;
  color: #303133;
}

.title-content .subtitle {
  margin: 6px 0 0;
  font-size: 0.9rem;
  color: #909399;
}

.title-actions .back-btn {
  border-radius: 10px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.title-actions .back-btn:hover {
  transform: translateX(-5px);
}

/* 統計卡片 */
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: white;
  border-radius: 14px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  margin-bottom: 16px;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
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

.archive-card .stat-icon {
  background: linear-gradient(135deg, #909399 0%, #b1b3b8 100%);
}

.filter-card .stat-icon {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
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

/* 表格卡片 */
.table-card {
  border-radius: 16px;
  overflow: hidden;
  border: none;
}

.card-header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #909399 0%, #c0c4cc 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
}

.header-text {
  font-weight: 600;
  font-size: 1.05rem;
  color: #303133;
}

.search-input {
  width: 260px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 10px;
}

/* 表格樣式 */
.custom-table {
  --el-table-header-bg-color: #f8f9fa;
}

.name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.name-avatar {
  background: linear-gradient(135deg, #909399 0%, #b1b3b8 100%);
  color: white;
  font-weight: 600;
  flex-shrink: 0;
}

.name-text {
  font-weight: 500;
}

.email-cell {
  display: flex;
  align-items: center;
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-tag {
  border-radius: 6px;
}

.action-btn {
  transition: all 0.3s ease;
}

.action-btn:hover {
  transform: scale(1.15);
}

/* 空狀態 */
.empty-icon {
  font-size: 60px;
  color: #dcdfe6;
  margin-bottom: 16px;
}

/* 分頁 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
  margin-top: 20px;
}

/* 過渡動畫 */
.slide-down-enter-active {
  transition: all 0.5s ease-out;
}
.slide-down-leave-active {
  transition: all 0.3s ease-in;
}
.slide-down-enter-from {
  transform: translateY(-30px);
  opacity: 0;
}
.slide-down-leave-to {
  transform: translateY(-20px);
  opacity: 0;
}

.zoom-fade-enter-active {
  transition: all 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.zoom-fade-leave-active {
  transition: all 0.3s ease-in;
}
.zoom-fade-enter-from {
  transform: scale(0.95);
  opacity: 0;
}
.zoom-fade-leave-to {
  transform: scale(0.98);
  opacity: 0;
}

/* 輔助類 */
.text-muted {
  color: #c0c4cc;
}
.text-primary {
  color: #409eff;
}
.text-success {
  color: #67c23a;
}
.text-warning {
  color: #e6a23c;
}
.mr-1 {
  margin-right: 4px;
}
.mr-2 {
  margin-right: 8px;
}
.ml-2 {
  margin-left: 8px;
}
.mb-4 {
  margin-bottom: 1.5rem;
}
</style>
