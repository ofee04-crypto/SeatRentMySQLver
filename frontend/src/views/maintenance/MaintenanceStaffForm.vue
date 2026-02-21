<script setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus' // 改用 Element Plus 元件
import maintenanceApi from '@/api/modules/maintenance'
import MaintenancePageHeader from '@/components/maintenance/MaintenancePageHeader.vue'

const route = useRoute()
const router = useRouter()
const staffId = Number(route.params.id)
const isEdit = computed(() => !isNaN(staffId) && staffId > 0)

const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)
// 移除 formVisible 與 setTimeout，改用 CSS 動畫原生的 appear

const form = reactive({
  staffName: '',
  staffCompany: '',
  staffPhone: '',
  staffEmail: '',
  staffNote: '',
  isActive: true,
})

const rules = {
  staffName: [{ required: true, message: '請輸入姓名', trigger: 'blur' }],
  staffEmail: [{ type: 'email', message: 'Email 格式不正確', trigger: 'blur' }],
}

onMounted(async () => {
  if (isEdit.value) {
    loading.value = true
    try {
      const res = await maintenanceApi.getStaffById(staffId)
      // 確保 API 回傳的資料能正確映射
      Object.assign(form, res.data || res)
    } catch (error) {
      console.error('獲取資料失敗:', error)
      ElMessage.error('無法載入人員資料，請稍後再試')
      router.push('/admin/staff-list')
    } finally {
      loading.value = false
    }
  }
})

const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      // 1. 確認視窗 (使用 Element Plus)
      try {
        await ElMessageBox.confirm(
          isEdit.value ? '即將更新此人員資料，確定嗎？' : '即將新增維護人員，確定嗎？',
          isEdit.value ? '確認更新' : '確認新增',
          {
            confirmButtonText: '確認送出',
            cancelButtonText: '再想想',
            type: 'warning',
            draggable: true,
          },
        )
      } catch {
        // 使用者點擊取消
        return
      }

      // 2. 執行送出
      submitting.value = true
      try {
        if (isEdit.value) {
          await maintenanceApi.updateStaff(staffId, form)
          ElMessage.success({
            message: `更新成功！人員 ${form.staffName} 資料已更新`,
            duration: 2000,
          })
        } else {
          await maintenanceApi.createStaff(form)
          ElMessage.success({
            message: `新增成功！歡迎 ${form.staffName} 加入團隊`,
            duration: 2000,
          })
        }
        router.push('/admin/staff-list')
      } catch (error) {
        // 錯誤通常由 http.js 攔截器處理，這裡做備用
        console.error(error)
      } finally {
        submitting.value = false
      }
    } else {
      // 驗證失敗提示
      ElMessage.warning('表單驗證失敗，請檢查必填欄位')
    }
  })
}

//一鍵帶入(Demo用)
const handleDemoFill = () => {
  form.staffName = '江小魚'
  form.staffCompany = '小魚科技股份有限公司'
  form.staffPhone = '0988-123-456'
  form.staffEmail = 'swagsnail860701@gmail.com'
  form.staffNote = '資深維護工程師，負責北區機台維修，週一至週五 09:00~18:00 可聯繫。'
  form.isActive = true

  // 提示一下使用者
  ElMessage.success('已帶入測試資料！')
}

const handleCancel = async () => {
  // 如果有填寫內容，離開前確認
  if (form.staffName || form.staffCompany || form.staffPhone) {
    try {
      await ElMessageBox.confirm('您填寫的資料將不會被保存，確定要離開嗎？', '確認離開', {
        confirmButtonText: '離開',
        cancelButtonText: '繼續編輯',
        type: 'warning',
      })
    } catch {
      return
    }
  }
  router.push('/admin/staff-list')
}
</script>

<template>
  <div class="staff-form-container">
    <section class="content-header">
      <div class="container-fluid">
        <transition name="slide-fade" appear>
          <MaintenancePageHeader
            :title="isEdit ? '編輯人員資料' : '新增維護人員'"
            :subtitle="isEdit ? '修改現有人員的詳細資訊' : '建立新的維護人員檔案'"
            :icon="isEdit ? 'fas fa-user-edit' : 'fas fa-user-plus'"
            :mode="isEdit ? 'edit' : 'add'"
          />
        </transition>
      </div>
    </section>

    <section class="content">
      <div class="container-fluid d-flex justify-content-center">
        <transition name="zoom-fade" appear>
          <el-card
            shadow="hover"
            class="form-card"
            v-loading="loading"
            element-loading-text="載入中..."
          >
            <template #header>
              <div class="card-header-content">
                <div class="header-left">
                  <span class="header-icon">
                    <i class="fas fa-id-card"></i>
                  </span>
                  <span class="header-text">基本資料</span>
                  <el-tag v-if="isEdit" type="warning" effect="plain" size="small" class="ml-2">
                    編輯模式
                  </el-tag>
                  <el-tag v-else type="success" effect="plain" size="small" class="ml-2">
                    新增模式
                  </el-tag>
                </div>

                <el-button class="cancel-btn" text type="info" @click="handleCancel">
                  <i class="fas fa-times mr-1"></i> 取消
                </el-button>
              </div>
            </template>

            <el-form
              ref="formRef"
              :model="form"
              :rules="rules"
              label-width="100px"
              label-position="top"
              status-icon
              class="staff-form"
            >
              <el-form-item label="姓名" prop="staffName" class="form-item-animated">
                <template #label>
                  <span class="custom-label">
                    <i class="fas fa-user label-icon"></i> 姓名
                    <span class="required-star">*</span>
                  </span>
                </template>
                <el-input
                  v-model="form.staffName"
                  placeholder="請輸入維護人員姓名"
                  prefix-icon="User"
                  clearable
                  size="large"
                  class="custom-input"
                />
              </el-form-item>

              <el-form-item label="公司" prop="staffCompany" class="form-item-animated">
                <template #label>
                  <span class="custom-label">
                    <i class="fas fa-building label-icon"></i> 所屬公司
                  </span>
                </template>
                <el-input
                  v-model="form.staffCompany"
                  placeholder="請輸入所屬公司名稱"
                  prefix-icon="OfficeBuilding"
                  clearable
                  size="large"
                  class="custom-input"
                />
              </el-form-item>

              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="電話" prop="staffPhone" class="form-item-animated">
                    <template #label>
                      <span class="custom-label">
                        <i class="fas fa-phone label-icon"></i> 聯絡電話
                      </span>
                    </template>
                    <el-input
                      v-model="form.staffPhone"
                      placeholder="09xx-xxx-xxx"
                      prefix-icon="Phone"
                      clearable
                      size="large"
                      class="custom-input"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="Email" prop="staffEmail" class="form-item-animated">
                    <template #label>
                      <span class="custom-label">
                        <i class="fas fa-envelope label-icon"></i> 電子郵件
                      </span>
                    </template>
                    <el-input
                      v-model="form.staffEmail"
                      placeholder="example@mail.com"
                      prefix-icon="Message"
                      clearable
                      size="large"
                      class="custom-input"
                    />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item label="備註" prop="staffNote" class="form-item-animated">
                <template #label>
                  <span class="custom-label">
                    <i class="fas fa-sticky-note label-icon"></i> 備註說明
                  </span>
                </template>
                <el-input
                  v-model="form.staffNote"
                  type="textarea"
                  :rows="4"
                  placeholder="可填寫專長、負責區域等補充資訊..."
                  show-word-limit
                  maxlength="500"
                  class="custom-textarea"
                />
              </el-form-item>

              <transition name="fade-slide">
                <el-form-item v-if="isEdit" label="狀態" prop="isActive" class="status-switch-item">
                  <template #label>
                    <span class="custom-label">
                      <i class="fas fa-toggle-on label-icon"></i> 人員狀態
                    </span>
                  </template>
                  <div class="status-switch-wrapper">
                    <el-switch
                      v-model="form.isActive"
                      active-text="在職"
                      inactive-text="停用"
                      :active-icon="''"
                      :inactive-icon="''"
                      inline-prompt
                      style="--el-switch-on-color: #67c23a; --el-switch-off-color: #f56c6c"
                      size="large"
                    />
                    <el-tag
                      :type="form.isActive ? 'success' : 'danger'"
                      effect="light"
                      class="status-tag ml-3"
                    >
                      <i :class="form.isActive ? 'fas fa-check-circle' : 'fas fa-ban'"></i>
                      {{ form.isActive ? '目前為在職狀態' : '目前為停用狀態' }}
                    </el-tag>
                  </div>
                </el-form-item>
              </transition>

              <el-divider>
                <i class="fas fa-paper-plane"></i>
              </el-divider>

              <el-form-item class="form-actions">
                <div
                  style="
                    display: flex;
                    justify-content: space-between;
                    width: 100%;
                    align-items: center;
                  "
                >
                  <div class="left-actions">
                    <el-button
                      type="primary"
                      @click="submitForm"
                      :loading="submitting"
                      size="large"
                      class="submit-btn"
                    >
                      <i class="fas fa-save mr-2" v-if="!submitting"></i>
                      <span>{{ submitting ? '處理中...' : isEdit ? '更新資料' : '確認新增' }}</span>
                    </el-button>

                    <el-button @click="handleCancel" size="large" class="back-btn ml-3">
                      <i class="fas fa-arrow-left mr-2"></i> 返回列表
                    </el-button>
                  </div>

                  <div class="right-actions">
                    <el-button
                      type="warning"
                      link
                      @click="handleDemoFill"
                      style="opacity: 0.6; transition: opacity 0.3s"
                      onmouseover="this.style.opacity=1"
                      onmouseout="this.style.opacity=0.6"
                    >
                      <i class="fas fa-magic mr-1"></i> 一鍵帶入
                    </el-button>
                  </div>
                </div>
              </el-form-item>
            </el-form>
          </el-card>
        </transition>
      </div>
    </section>
  </div>
</template>

<style scoped>
.staff-form-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
  padding-bottom: 40px;
}

.content-header {
  padding: 20px 1rem;
}

/* PageHeader 的樣式已移至元件中，此處刪除 */

/* 表單卡片 */
.form-card {
  width: 100%;
  max-width: 700px;
  border-radius: 16px;
  overflow: hidden;
  border: none;
  transition: all 0.3s ease;
}

.form-card:hover {
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
}

.card-header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
}

.header-text {
  font-weight: 600;
  font-size: 1rem;
  color: #303133;
}

.cancel-btn {
  transition: all 0.3s ease;
}

.cancel-btn:hover {
  transform: translateX(3px);
}

/* 表單樣式 */
.staff-form {
  padding: 10px 20px;
}

.custom-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  color: #606266;
}

.label-icon {
  color: #409eff;
  font-size: 14px;
}

.required-star {
  color: #f56c6c;
  margin-left: 2px;
}

.form-item-animated {
  animation: fadeInUp 0.5s ease forwards;
  opacity: 0;
}

.form-item-animated:nth-child(1) {
  animation-delay: 0.1s;
}
.form-item-animated:nth-child(2) {
  animation-delay: 0.15s;
}
.form-item-animated:nth-child(3) {
  animation-delay: 0.2s;
}
.form-item-animated:nth-child(4) {
  animation-delay: 0.25s;
}
.form-item-animated:nth-child(5) {
  animation-delay: 0.3s;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.custom-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  transition: all 0.3s ease;
}

.custom-input :deep(.el-input__wrapper):hover {
  box-shadow: 0 0 0 1px #409eff inset;
}

.custom-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.3) inset;
}

.custom-textarea :deep(.el-textarea__inner) {
  border-radius: 10px;
  transition: all 0.3s ease;
}

.custom-textarea :deep(.el-textarea__inner):hover {
  box-shadow: 0 0 0 1px #409eff inset;
}

/* 狀態切換 */
.status-switch-wrapper {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 10px;
}

.status-tag {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 按鈕區 */
.form-actions {
  margin-top: 20px;
}

.submit-btn {
  min-width: 140px;
  border-radius: 10px;
  font-weight: 600;
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
  border: none;
  transition: all 0.3s ease;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.4);
}

.back-btn {
  min-width: 120px;
  border-radius: 10px;
  transition: all 0.3s ease;
}

.back-btn:hover {
  transform: translateX(-3px);
}

/* 過渡動畫 */
.slide-fade-enter-active {
  transition: all 0.4s ease-out;
}
.slide-fade-leave-active {
  transition: all 0.3s ease-in;
}
.slide-fade-enter-from {
  transform: translateX(-20px);
  opacity: 0;
}
.slide-fade-leave-to {
  transform: translateX(20px);
  opacity: 0;
}

.zoom-fade-enter-active {
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.zoom-fade-leave-active {
  transition: all 0.3s ease-in;
}
.zoom-fade-enter-from {
  transform: scale(0.9);
  opacity: 0;
}
.zoom-fade-leave-to {
  transform: scale(0.95);
  opacity: 0;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}
.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* Element Plus 分隔線 */
:deep(.el-divider__text) {
  background: white;
  color: #c0c4cc;
}
</style>
