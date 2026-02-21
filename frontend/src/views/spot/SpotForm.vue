<template>
  <div class="spot-form-container">
    <!-- ========== Header 區域 ========== -->
    <section class="content-header">
      <div class="container-fluid">
        <div class="row align-items-center">
          <div class="col-sm-6">
            <div class="page-title-box">
              <div class="title-icon" :class="isEditMode ? 'edit-mode' : 'add-mode'">
                <i :class="isEditMode ? 'fas fa-edit' : 'fas fa-plus-circle'"></i>
              </div>
              <div class="title-content">
                <h1>{{ isEditMode ? '編輯據點' : '新增據點' }}</h1>
                <p class="subtitle">{{ isEditMode ? '修改據點資訊' : '建立新的營業據點' }}</p>
              </div>
            </div>
          </div>
          <div class="col-sm-6 text-right">
            <el-button @click="goBack">
              <i class="fas fa-arrow-left mr-1"></i> 返回列表
            </el-button>
          </div>
        </div>
      </div>
    </section>

    <section class="content">
      <div class="container-fluid">
        <transition name="zoom-fade" appear>
          <el-card shadow="hover" class="form-card">
            <el-form
              v-if="isEditMode && spotDataForView"
              :model="spotDataForView"
              label-width="120px"
              label-position="top"
              class="view-section"
              disabled
            >
              <div class="form-section">
                <div class="section-header">
                  <i class="fas fa-eye"></i>
                  <span>目前資料預覽</span>
                </div>
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="據點代碼">
                      <el-input v-model="spotDataForView.spotCode" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="據點名稱">
                      <el-input v-model="spotDataForView.spotName" />
                    </el-form-item>
                  </el-col>
                </el-row>
              </div>
            </el-form>
            <!-- 1. 綁定 formRef (用於JS控制驗證) 與 rules (驗證規則物件) -->
            <el-form
              ref="formRef"
              :rules="rules"
              :model="formData"
              label-width="120px"
              label-position="top"
              class="modern-form"
              @submit.prevent="submitForm"
            >
              <!-- ========== 基本資料區 ========== -->
              <div class="form-section">
                <div class="section-header">
                  <i class="fas fa-info-circle"></i>
                  <span>編輯資料</span>
                </div>

                <el-row :gutter="20">
                  <el-col :span="12">
                    <!-- [修改說明] 2. 設定 prop="spotCode"，對應 rules 中的 key，驗證才會生效 -->
                    <el-form-item label="據點代碼 (Code)" prop="spotCode" required>
                      <el-input v-model="formData.spotCode" placeholder="例如: TP001" clearable>
                        <template #prefix><i class="fas fa-barcode"></i></template>
                      </el-input>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <!-- [修改說明] 2. 設定 prop="spotName" -->
                    <el-form-item label="據點名稱 (Name)" prop="spotName" required>
                      <el-input v-model="formData.spotName" placeholder="例如: 台北車站店" clearable>
                        <template #prefix><i class="fas fa-store"></i></template>
                        <template #append>
                          <el-button @click="geocodeByName" :loading="geoLoading" title="依名稱搜尋座標"><i class="fas fa-search-location"></i></el-button>
                        </template>
                      </el-input>
                    </el-form-item>
                  </el-col>
                </el-row>

                <!--地址（Autocomplete + Enter geocode） -->
                <el-form-item label="地址 (Address)" prop="spotAddress">
  <el-input
    ref="addrElInputRef"
    v-model="formData.spotAddress"
    placeholder="請輸入地址"
    @keyup.enter="geocodeAddress({ force: true })"
  />
</el-form-item>

                <!-- 經緯度 -->
                <el-row :gutter="12">
                  <el-col :span="12">
                    <el-form-item label="緯度 Latitude" prop="latitude">
                      <el-input-number
                        v-model="formData.latitude"
                        :precision="7"
                        :step="0.000001"
                        style="width: 100%"
                        @change="manualOverride.lat = true"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="經度 Longitude" prop="longitude">
                      <el-input-number
                        v-model="formData.longitude"
                        :precision="7"
                        :step="0.000001"
                        style="width: 100%"
                        @change="manualOverride.lng = true"
                      />
                    </el-form-item>
                  </el-col>
                </el-row>

                <div style="display:flex; gap:10px; margin-bottom: 12px;">
                  <el-button type="primary" :loading="geoLoading" @click="geocodeAddress({ force: true })">
                    依地址帶入經緯度
                  </el-button>
                  <el-button @click="manualOverride.lat=false; manualOverride.lng=false">
                    允許自動覆蓋
                  </el-button>
                </div>

                <!-- [新增] 地圖預覽與精確度提示 -->
                <div class="map-preview-container" v-if="formData.latitude && formData.longitude">
                  <div class="precision-info mb-2" v-if="geoPrecision">
                    <el-tag :type="getPrecisionTagType(geoPrecision)" effect="dark">
                      <i class="fas fa-crosshairs mr-1"></i>
                      定位精確度: {{ formatPrecision(geoPrecision) }}
                    </el-tag>
                    <span class="text-muted text-xs ml-2"><i class="fas fa-info-circle"></i> 若位置有偏差，可直接拖曳地圖上的紅色標記進行修正</span>
                  </div>
                  
                  <GMapMap
                    :center="{lat: formData.latitude, lng: formData.longitude}"
                    :zoom="16"
                    style="width: 100%; height: 350px; border-radius: 8px; border: 1px solid #dcdfe6;"
                  >
                    <GMapMarker
                      :position="{lat: formData.latitude, lng: formData.longitude}"
                      :draggable="true"
                      @dragend="onMarkerDragEnd"
                    />
                  </GMapMap>
                </div>

                <el-alert
                  v-if="geoError"
                  :title="geoError"
                  type="warning"
                  show-icon
                  :closable="false"
                  style="margin-bottom: 12px;"
                />

                <el-row :gutter="20">
                  <el-col :span="8">
                    <el-form-item label="所屬商家 (Merchant)">
                      <el-select
                        v-model="formData.merchantId"
                        placeholder="請選擇商家 (可留空)"
                        clearable
                        filterable
                        style="width: 100%"
                      >
                        <template #prefix><i class="fas fa-building"></i></template>
                        <el-option
                          v-for="item in merchantOptions"
                          :key="item.merchantId"
                          :label="item.merchantName"
                          :value="item.merchantId"
                        >
                          <span style="float: left">{{ item.merchantName }}</span>
                          <span style="float: right; color: #8492a6; font-size: 13px">ID: {{ item.merchantId }}</span>
                        </el-option>
                      </el-select>
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-form-item label="描述 (Description)">
                  <el-input
                    v-model="formData.spotDescription"
                    type="textarea"
                    :rows="3"
                    placeholder="請輸入據點描述..."
                  />  
                </el-form-item>

                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="狀態">
                      <el-select v-model="formData.spotStatus" style="width: 100%">
                        <el-option label="營運中" value="營運中">
                          <i class="fas fa-check-circle text-success mr-2"></i> 營運中
                        </el-option>
                        <el-option label="維修中" value="維修中">
                          <i class="fas fa-tools text-warning mr-2"></i> 維修中
                        </el-option>
                        <el-option label="停用" value="停用">
                          <i class="fas fa-ban text-danger mr-2"></i> 停用
                        </el-option>
                      </el-select>
                    </el-form-item>
                  </el-col>
                </el-row>
              </div>

              <!-- ========== 圖片上傳區 (整合原有功能) ========== -->
              <div class="form-section">
                <div class="section-header">
                  <i class="fas fa-image"></i>
                  <span>據點圖片</span>
                </div>

                <el-form-item>
                  <div v-if="previewUrl" class="preview-section">
                    <div class="preview-header">
                      <p class="preview-title">圖片預覽：</p>
                      <el-button type="danger" size="small" @click="deleteImage" :loading="isUploading">
                        <i class="fas fa-trash mr-1"></i> 刪除圖片
                      </el-button>
                    </div>
                    <img :src="previewUrl" alt="預覽圖片" class="preview-image" />
                  </div>

                  <div v-else class="image-upload-area">
                    <input
                      type="file"
                      @change="handleFileChange"
                      accept="image/jpeg,image/jpg,image/png,image/gif,image/webp"
                      class="file-input"
                      id="image-upload"
                    />
                    <label for="image-upload" class="upload-label">
                      <i class="fas fa-cloud-upload-alt"></i>
                      <span>點擊或拖曳上傳圖片</span>
                      <span class="upload-hint">支援 JPG, PNG, GIF, WebP (最大 5MB)</span>
                    </label>
                  </div>

                  <div v-if="isUploading" class="uploading-hint">
                    <i class="el-icon-loading"></i> 上傳中...
                  </div>
                </el-form-item>
              </div>

              <div class="form-actions">
                <el-button @click="goBack" class="back-btn">
                  <i class="fas fa-times mr-1"></i> 取消
                </el-button>
                <el-button type="primary" @click="submitForm" :loading="isSubmitting" class="submit-btn">
                  <i class="fas fa-save mr-1"></i> {{ isSubmitting ? '處理中...' : '儲存資料' }}
                </el-button>
              </div>
            </el-form>
          </el-card>
        </transition>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive, watch, nextTick, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import Swal from 'sweetalert2'
import { useGoogleMaps } from '@/composables/spot/useGoogleMaps'

defineOptions({ name: 'SpotForm' })

const route = useRoute()
const router = useRouter()

//  後端 Controller 是 /spot（沒有 /api）
const API_BASE = '/spot'
const MERCHANT_API_BASE = '/spot/merchants'

// 3. 定義 formRef 變數，對應 template 中的 ref="formRef"
const formRef = ref(null)

// 4. 定義驗證規則：必填 (required: true) 與 觸發時機 (trigger: 'blur' 失焦時)
const rules = {
  spotCode: [{ required: true, message: '請輸入據點代碼', trigger: 'blur' }],
  spotName: [{ required: true, message: '請輸入據點名稱', trigger: 'blur' }],
  spotAddress: [{ required: true, message: '請輸入地址', trigger: 'change' }],
}

// ========== Geo（地址 → 經緯度）狀態 ==========
// [關鍵] 手動鎖定旗標。當使用者手動修改經緯度或拖曳地圖標記時，
// 此旗標會設為 true，用來防止 `watch` 自動觸發的 geocodeAddress 覆蓋掉手動輸入。
const manualOverride = reactive({ lat: false, lng: false })

// ========== 狀態定義 ==========
const isEditMode = computed(() => !!route.params.id)
const isSubmitting = ref(false)
const selectedFile = ref(null)
const previewUrl = ref('')
const spotDataForView = ref(null) // [新增] 用於顯示的原始資料
const isUploading = ref(false)
const merchantOptions = ref([])

// ========== 表單資料模型 ==========
const formData = ref({
  spotCode: '',
  spotName: '',
  spotAddress: '',
  latitude: null,
  longitude: null,
  merchantId: '',
  spotDescription: '',
  spotStatus: '營運中',
  spotImage: '',
})

/**
 * [核心] 引入並使用 useGoogleMaps Composable。
 * 將所有地圖相關的狀態 (geoLoading) 和方法 (geocodeByName) 解構出來，直接在 template 和 script 中使用。
 */
const {
  geoLoading,
  geoError,
  geoPrecision,
  gmapAutoRef,
  addrElInputRef,
  initPlacesAutocomplete ,
  syncAddressToNativeInput,
  onPlaceChangedForForm,
  geocodeAddress,
  geocodeByName,
  onMarkerDragEnd,
  formatPrecision,
  getPrecisionTagType
} = useGoogleMaps(formData, manualOverride)

/**
 * [途徑 A-2 的觸發點] 監聽地址輸入框的變化。
 * 使用 setTimeout 實現防抖 (Debounce) 機制：
 * - 當使用者連續輸入時，會不斷清除舊的計時器。
 * - 直到使用者停止輸入 700 毫秒後，才會真正執行 geocodeAddress。
 * - 這樣可以避免在打字過程中發送大量不必要的 API 請求。
 */
let geoTimer = null
watch(
  () => formData.value.spotAddress,
  (v) => {
    geoError.value = ''
    if (!v || v.trim().length < 6) return
    clearTimeout(geoTimer)
    geoTimer = setTimeout(() => {
      geocodeAddress({ force: false })
    }, 700)
  },
)

// [優化] 元件銷毀時清除計時器，避免在頁面切換後，計時器依然執行，導致非預期的行為或記憶體洩漏。
onUnmounted(() => {
  if (geoTimer) clearTimeout(geoTimer)
})

// ========== 取得商家列表 ==========
const fetchMerchants = async () => {
  try {
    const res = await axios.get(MERCHANT_API_BASE)
    merchantOptions.value = res.data
  } catch (error) {
    console.error('無法取得商家列表:', error)
  }
}

// ========== 初始化：編輯模式載入資料 ==========
onMounted(async () => {
  // [初始化步驟 1] 等待 Vue 將模板渲染成真實的 DOM。
  // 這是必要的，因為 initPlacesAutocomplete 需要抓取 <el-input> 內部的 <input> 元素。
  await nextTick()
  // [初始化步驟 2] 呼叫 Composable 中的初始化函式，將 Google Autocomplete 綁定到地址輸入框。
  await initPlacesAutocomplete()

  // 載入商家選項
  await fetchMerchants()

  // 如果是編輯模式，則從後端載入既有資料。
  if (isEditMode.value) {
    try {
      const res = await axios.get(`${API_BASE}/${route.params.id}`)
      console.log('api回傳值 res.data =', res.data)
      console.log('api回傳 spotAddress =', res.data?.spotAddress)

      formData.value = { ...formData.value, ...res.data }
      spotDataForView.value = { ...res.data } // [新增] 將原始資料存入顯示用的 ref

      manualOverride.lat = false
      manualOverride.lng = false
      geoPrecision.value = '' // 既有資料不確定精確度，先留空

      if (formData.value.spotImage) {
        previewUrl.value = `http://localhost:8080/${formData.value.spotImage}`
      }

      // [關鍵] 載入資料後，呼叫 Composable 中的同步函式。
      // 確保從 API 取得的地址能夠正確顯示在被 Google 腳本控制的輸入框中。
      syncAddressToNativeInput()
    } catch (err) {
      console.error('載入失敗', err)
      Swal.fire({
        title: '載入失敗',
        html: '<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg><p style="margin-top:12px;">無法載入據點資料</p></div>',
        confirmButtonColor: '#409eff',
        confirmButtonText: '確定',
      })
    }
  }
})

// ========== 處理檔案選擇 ==========
const handleFileChange = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    Swal.fire({
      icon: 'error',
      title: '檔案類型錯誤',
      text: '只能上傳圖片檔案 (jpg, png, gif)',
      confirmButtonColor: '#409eff',
    })
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    Swal.fire({
      icon: 'error',
      title: '檔案過大',
      text: '圖片大小不能超過 5MB',
      confirmButtonColor: '#409eff',
    })
    return
  }

  selectedFile.value = file
  previewUrl.value = URL.createObjectURL(file)

  if (isEditMode.value) {
    await uploadImage()
  }
}

// ========== 上傳圖片到後端 ==========
const uploadImage = async () => {
  if (!selectedFile.value) return

  isUploading.value = true
  const formDataUpload = new FormData()
  formDataUpload.append('image', selectedFile.value)

  try {
    const res = await axios.post(`${API_BASE}/${route.params.id}/upload-image`, formDataUpload, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })

    formData.value.spotImage = res.data.filePath
    previewUrl.value = `http://localhost:8080${res.data.imageUrl}`

    Swal.fire({
      icon: 'success',
      title: '上傳成功',
      text: '圖片已成功上傳',
      timer: 1500,
      showConfirmButton: false,
    })
  } catch (error) {
    console.error('上傳失敗:', error)
    Swal.fire({
      icon: 'error',
      title: '上傳失敗',
      text: error.response?.data?.message || '圖片上傳失敗',
      confirmButtonColor: '#409eff',
    })
  } finally {
    isUploading.value = false
  }
}

// ========== 刪除圖片 ==========
const deleteImage = async () => {
  const result = await Swal.fire({
    title: '確定要刪除圖片嗎？',
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#f56c6c',
    cancelButtonColor: '#909399',
    confirmButtonText: '刪除',
    cancelButtonText: '取消',
  })

  if (!result.isConfirmed) return

  if (isEditMode.value && formData.value.spotImage) {
    try {
      await axios.delete(`${API_BASE}/${route.params.id}/image`)
      formData.value.spotImage = ''
      previewUrl.value = ''
      selectedFile.value = null

      Swal.fire({
        icon: 'success',
        title: '已刪除',
        text: '圖片已成功刪除',
        timer: 1500,
        showConfirmButton: false,
      })
    } catch (error) {
      Swal.fire({
        icon: 'error',
        title: '刪除失敗',
        text: error.response?.data?.message || '無法刪除圖片',
        confirmButtonColor: '#409eff',
      })
    }
  } else {
    previewUrl.value = ''
    selectedFile.value = null
    formData.value.spotImage = ''
  }
}

// ========== 提交表單 ==========
const submitForm = async () => {
  //  5. 確保 formRef 已掛載
  if (!formRef.value) return
  
  try {
    //  6. 執行表單驗證，若驗證失敗會拋出錯誤並跳到 catch 區塊，阻止後續 API 呼叫
    await formRef.value.validate()
  } catch (error) {
    Swal.fire({ icon: 'warning', title: '資料不完整', text: '請檢查必填欄位是否已填寫' })
    return
  }

  isSubmitting.value = true
  try {
    const payload = {
      spotCode: formData.value.spotCode,
      spotName: formData.value.spotName,
      spotAddress: formData.value.spotAddress,
      latitude:
        formData.value.latitude === '' || formData.value.latitude === null || formData.value.latitude === undefined
          ? null
          : Number(formData.value.latitude),
      longitude:
        formData.value.longitude === '' || formData.value.longitude === null || formData.value.longitude === undefined
          ? null
          : Number(formData.value.longitude),
      spotDescription: formData.value.spotDescription,
      spotStatus: formData.value.spotStatus,
      spotImage: formData.value.spotImage,
      merchantId:
        formData.value.merchantId === '' || formData.value.merchantId === null || formData.value.merchantId === undefined
          ? null
          : Number(formData.value.merchantId),
    }

    if (isEditMode.value) {
      payload.spotId = Number(route.params.id)
      await axios.put(`${API_BASE}/${route.params.id}`, payload)

      await Swal.fire({
        title: '更新成功',
        html: '<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#67c23a" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg><p style="margin-top:12px;">據點資料已成功更新</p></div>',
        confirmButtonColor: '#409eff',
        confirmButtonText: '確定',
      })
    } else {
      const res = await axios.post(`${API_BASE}`, payload)

      if (selectedFile.value && res.data?.spotId) {
        const spotId = res.data.spotId
        const formDataUpload = new FormData()
        formDataUpload.append('image', selectedFile.value)

        await axios.post(`${API_BASE}/${spotId}/upload-image`, formDataUpload, {
          headers: { 'Content-Type': 'multipart/form-data' },
        })
      }

      await Swal.fire({
        title: '新增成功',
        html: '<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#67c23a" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg><p style="margin-top:12px;">據點資料已成功新增</p></div>',
        confirmButtonColor: '#409eff',
        confirmButtonText: '確定',
      })
    }

    router.push({ name: 'spot-list' })
  } catch (error) {
    console.error('儲存錯誤:', error)
    const msg = error.response?.data?.message || error.message || '儲存失敗，請檢查輸入資料或後端連線'
    Swal.fire({
      title: '操作失敗',
      html: `<div style="display:flex;flex-direction:column;align-items:center;"><svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg><p style="margin-top:12px;">${msg}</p></div>`,
      confirmButtonColor: '#409eff',
      confirmButtonText: '確定',
    })
  } finally {
    isSubmitting.value = false
  }
}

const goBack = () => router.back()
</script>

<style scoped>
/* ========== 頁面容器 ========== */
.spot-form-container {
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
  min-height: 100vh;
}

.view-section {
  background-color: #f9fafb;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 30px;
}
/* ========== 頁面標題區 ========== */
.page-title-box {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.title-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  transition: transform 0.3s ease;
}

.title-icon:hover {
  transform: scale(1.1) rotate(5deg);
}

.add-mode {
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
}

.edit-mode {
  background: linear-gradient(135deg, #e6a23c 0%, #f3d19e 100%);
}

.title-content h1 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
  color: #303133;
}

.title-content .subtitle {
  margin: 4px 0 0;
  font-size: 0.875rem;
  color: #909399;
}

/* ========== 表單卡片 ========== */
.form-card {
  border-radius: 12px;
  max-width: 900px;
  margin: 0 auto;
}

.form-section {
  margin-bottom: 32px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.1rem;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #409eff;
}

.map-preview-container {
  margin-bottom: 20px;
}

/* ========== 圖片上傳 ========== */
.image-upload-area {
  border: 2px dashed #dcdfe6;
  border-radius: 12px;
  padding: 40px;
  text-align: center;
  transition: all 0.3s ease;
  cursor: pointer;
}

.image-upload-area:hover {
  border-color: #409eff;
  background: rgba(64, 158, 255, 0.05);
}

.file-input {
  display: none;
}

.upload-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  color: #909399;
}

.upload-label i {
  font-size: 48px;
  color: #c0c4cc;
}

.upload-hint {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
}

.preview-section {
  margin-top: 16px;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.preview-title {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

.preview-image {
  max-width: 100%;
  max-height: 300px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  display: block;
  margin: 0 auto;
}

.uploading-hint {
  text-align: center;
  color: #409eff;
  font-size: 14px;
  margin-top: 12px;
}

/* ========== 按鈕區 ========== */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 24px;
  margin-top: 24px;
  border-top: 1px solid #ebeef5;
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
  min-width: 100px;
  border-radius: 10px;
}

/* ========== 動畫效果 ========== */
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

/* ========== 工具類 ========== */
.mr-1 { margin-right: 4px; }
.mr-2 { margin-right: 8px; }
.text-success { color: #67c23a; }
.text-warning { color: #e6a23c; }
.text-danger { color: #f56c6c; }

@media (max-width: 768px) {
  .page-title-box {
    flex-direction: column;
    text-align: center;
    gap: 12px;
  }
}
</style>
