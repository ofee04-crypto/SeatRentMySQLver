import { ref } from 'vue'
import Swal from 'sweetalert2'

/**
 * [核心] Google Maps 相關邏輯的組合式函數 (Composable)
 * @param {Ref} formData - 表單資料的 ref 物件
 * @param {Object} manualOverride - 手動覆蓋座標的 reactive 狀態
 */
export function useGoogleMaps(formData, manualOverride) {
  // ========== 狀態定義 ==========
  const geoLoading = ref(false)
  const geoError = ref('')
  const geoPrecision = ref('') // 儲存 Google 回傳的精確度類型
  
  // Template Refs (需在元件 setup 中 return 給 template 綁定)
  const gmapAutoRef = ref(null)
  const addrElInputRef = ref(null)

  // ========== 輔助函式 ==========
  
  // 精確度顯示文字轉換
  const formatPrecision = (type) => {
    const map = { 
      'ROOFTOP': '精確 (Rooftop)', 
      'RANGE_INTERPOLATED': '街道插值 (Interpolated)', 
      'GEOMETRIC_CENTER': '區域中心 (Center)', 
      'APPROXIMATE': '粗略 (Approximate)', 
      'PLACES_API': 'Google 地點 (Place)', 
      'MANUAL': '手動修正 (Manual)' 
    }
    return map[type] || type
  }

  const getPrecisionTagType = (type) => {
    return (type === 'ROOFTOP' || type === 'PLACES_API' || type === 'MANUAL') ? 'success' : (type === 'RANGE_INTERPOLATED' ? 'warning' : 'info')
  }

  /**
  清理 Google 回傳的地址格式
   */
  const formatGoogleAddress = (rawAddress) => {
    if (!rawAddress) return ''
    // Regex: 移除開頭的 "數字+空白(可選)" 以及 "台灣"
    return rawAddress.replace(/^(\d+\s?)?(台灣)?/, '').trim()
  }

  /**
   * [關鍵] 將 formData 中的 spotAddress「強制同步」回 ElementPlus 的原生 input 元素。
   * 這是為了解決 Google Maps Autocomplete 腳本會覆蓋 Vue 響應式更新的問題。
   * 透過多次延遲執行，確保在 Google 初始化完成後，我們的值能成功寫入。
   * @returns {void}
   */
  const syncAddressToNativeInput = () => {
    const updateValue = () => {
      const comp = addrElInputRef.value
      let nativeInput = null

      // 1. 嘗試從 Element Plus 元件實例獲取 input
      if (comp) {
        // Element Plus 暴露的 input 屬性 (HTMLInputElement)
        if (comp.input && comp.input instanceof HTMLInputElement) {
          nativeInput = comp.input
        } 
        // 或者透過 DOM 查找
        else if (comp.$el) {
          nativeInput = comp.$el.querySelector('input')
        }
      }

      // 2. 最後手段：透過 placeholder 查找
      if (!nativeInput) {
        nativeInput = document.querySelector('input[placeholder="請輸入完整地址（可用建議清單）"]')
      }

      if (nativeInput) {
        const newValue = formData.value.spotAddress || ''
        // 只有當值不一致時才寫入
        if (nativeInput.value !== newValue) {
          nativeInput.value = newValue
          // 重要：觸發 input 事件，確保 Vue v-model 也能收到通知
          nativeInput.dispatchEvent(new Event('input', { bubbles: true }))
        }
      }
    }
    
    // 增加多次嘗試，確保在 Google Maps 初始化或 DOM 更新後能正確寫入
    setTimeout(updateValue, 100)
    setTimeout(updateValue, 300)
    setTimeout(updateValue, 800)
  }

  // ========== 核心邏輯 ==========
  /**
   * [初始化] 將 Google Places Autocomplete 功能綁定到地址輸入框上。
   * 這個函式必須在元件掛載 (mounted) 後呼叫，確保能抓取到 DOM 元素。
   * @returns {Promise<void>}
   */
const initPlacesAutocomplete = async () => {
  // 注意：要在元件 mounted 後、DOM 出來後才抓得到 input
  const comp = addrElInputRef.value
  const nativeInput =
    comp?.input instanceof HTMLInputElement
      ? comp.input
      : comp?.$el?.querySelector?.('input')

  if (!nativeInput) return

  // [新增] 等待機制：如果 Google Maps API 還沒載入，就稍微等一下 (最多等 3 秒)
  if (!window.google?.maps?.places?.Autocomplete) {
    await new Promise((resolve) => {
      const checkInterval = setInterval(() => {
        if (window.google?.maps?.places?.Autocomplete) {
          clearInterval(checkInterval)
          resolve()
        }
      }, 100) // 每 0.1 秒檢查一次
      setTimeout(() => { clearInterval(checkInterval); resolve() }, 3000) // 3秒後超時放棄
    })
  }

  if (!window.google?.maps?.places?.Autocomplete) {
    geoError.value = 'Google Places 載入失敗或逾時（請確認網路狀況或 API Key 設定）'
    return
  }

  const ac = new window.google.maps.places.Autocomplete(nativeInput, {
    fields: ['name', 'geometry', 'formatted_address', 'place_id'],
    componentRestrictions: { country: 'tw' }
  })

  ac.addListener('place_changed', () => {
    const place = ac.getPlace()
    onPlaceChangedForForm(place)
  })
}



  /**
   * [途徑 A-1] 當使用者從 Google Autocomplete 下拉選單中選取一個地點時觸發。
   * @param {google.maps.places.PlaceResult} placeFromEvent - Google API 回傳的地點物件。
   */
  const onPlaceChangedForForm = (placeFromEvent) => {
    const place = placeFromEvent || gmapAutoRef.value?.getPlace?.()
    if (!place?.geometry?.location) return

    // 如果 Google Place 有名稱，且我們的「據點名稱」欄位是空的，就自動帶入
    // 這是為了提升使用者體驗，選了「台北101」，名稱欄位就自動填上。
    if (place.name && !formData.value.spotName) {
      formData.value.spotName = place.name
    }

    if (place.formatted_address) {
      formData.value.spotAddress = formatGoogleAddress(place.formatted_address)
      syncAddressToNativeInput()
    }

    const loc = place.geometry.location
    formData.value.latitude = Number(loc.lat())
    formData.value.longitude = Number(loc.lng())

    // 標記精確度，並重置手動鎖定
    geoPrecision.value = 'PLACES_API'
    manualOverride.lat = false
    manualOverride.lng = false
    geoError.value = ''
  }

  /**
   * [途徑 A-2] 將文字地址透過 Geocoder API 轉換為經緯度。
   * @param {Object} options - 選項物件
   * @param {boolean} options.force - 是否強制執行，無視 `manualOverride` 鎖定。
   */
  const geocodeAddress = ({ force } = { force: false }) => {
    const address = (formData.value.spotAddress || '').trim()
    if (!address) return

    // [關鍵] 如果使用者手動修改過座標 (manualOverride 為 true)，
    // 且不是強制執行 (force 為 false)，則直接返回，以保護使用者的手動輸入。
    if (!force && (manualOverride.lat || manualOverride.lng)) return

    if (!window.google?.maps?.Geocoder) {
      geoError.value = 'Google Maps 尚未載入（請確認已載入 places library）'
      return
    }

    geoLoading.value = true
    geoError.value = ''

    const geocoder = new window.google.maps.Geocoder()
    geocoder.geocode({ address }, (results, status) => {
      geoLoading.value = false

      if (status === 'OK' && results?.[0]) {
        const location = results[0].geometry.location
        const lat = Number(location.lat())
        const lng = Number(location.lng())

        geoPrecision.value = results[0].geometry.location_type
        if (force || !manualOverride.lat) formData.value.latitude = lat
        if (force || !manualOverride.lng) formData.value.longitude = lng
        return
      }

      geoError.value = `找不到座標，請輸入更完整地址（狀態：${status}）`
    })
  }

  /**
   * [途徑 B] 依據「據點名稱」搜尋經緯度與地址。
   * 當使用者點擊名稱欄位旁的搜尋按鈕時觸發。
   */
  const geocodeByName = () => {
    const name = (formData.value.spotName || '').trim()
    if (!name) {
      Swal.fire({ icon: 'warning', title: '請先輸入據點名稱', timer: 1500, showConfirmButton: false })
      return
    }
    
    geoLoading.value = true
    geoError.value = ''
    
    const geocoder = new window.google.maps.Geocoder()
    geocoder.geocode({ address: name, componentRestrictions: { country: 'TW' } }, (results, status) => {
      geoLoading.value = false
      if (status === 'OK' && results?.[0]) {
        const location = results[0].geometry.location
        formData.value.latitude = Number(location.lat())
        formData.value.longitude = Number(location.lng())
        formData.value.spotAddress = formatGoogleAddress(results[0].formatted_address)
        syncAddressToNativeInput()
        
        geoPrecision.value = results[0].geometry.location_type
        manualOverride.lat = false
        manualOverride.lng = false
      } else {
        geoError.value = `找不到該名稱的位置（狀態：${status}）`
      }
    })
  }

  /**
   * [途徑 C] 當使用者在地圖上拖曳標記 (Marker) 後觸發。
   * 這是最高優先權的操作，會直接更新座標並啟用「手動鎖定」。
   * @param {google.maps.MapMouseEvent} event - 拖曳事件物件，包含新的經緯度。
   */
  const onMarkerDragEnd = (event) => {
    const lat = event.latLng.lat()
    const lng = event.latLng.lng()
    formData.value.latitude = Number(lat.toFixed(6))
    formData.value.longitude = Number(lng.toFixed(6))
    manualOverride.lat = true
    manualOverride.lng = true
    geoPrecision.value = 'MANUAL'
  }

  // 將狀態和方法導出，供 Vue 元件使用
  return {
    // State
    geoLoading,
    geoError,
    geoPrecision,
    gmapAutoRef,
    addrElInputRef,
    
    // Methods
     initPlacesAutocomplete,
    syncAddressToNativeInput,
    onPlaceChangedForForm,
    geocodeAddress,
    geocodeByName,
    onMarkerDragEnd,
    formatPrecision,
    getPrecisionTagType
  }
}
