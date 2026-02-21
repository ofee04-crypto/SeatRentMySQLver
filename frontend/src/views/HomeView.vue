<script setup>
import { ref, onMounted, computed } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import axios from 'axios';
import Swal from 'sweetalert2';
import { useMemberAuthStore } from '@/stores/memberAuth';

const router = useRouter();
const memberAuthStore = useMemberAuthStore();

// --- 元件狀態 ---
const searchKeyword = ref('');
const isMapVisible = ref(false); // 控制地圖 Modal 顯示
const selectedSpotForMap = ref(null); // 目前點選要導覽的點位
const mapCenter = ref({ lat: 25.033964, lng: 121.564472 }); // 預設台北 101
const mapZoom = ref(12);
const allSpots = ref([]); // 全台所有據點
const isSearchTriggered = ref(false); // 標記是否為搜尋框觸發的彈窗
const isSuggestionsVisible = ref(false); // 管理建議清單顯示

// 熱門標籤
const hotTags = ['台北車站', '信義區', '圖書館', '咖啡廳'];

// 流程步驟
const steps = [
  { icon: 'fas fa-map-marker-alt', title: '尋找座位', desc: '透過地圖快速找到附近的空位' },
  { icon: 'fas fa-calendar-alt', title: '線上預約', desc: '選取心儀座位並完成線上預約' },
  { icon: 'fas fa-coffee', title: '享受時光', desc: '專注工作或放鬆，按時計費' },
];

/**
 * -------------------------------------------
 * 1. 資料獲取邏輯 (Data Fetching)
 * -------------------------------------------
 */

// 熱門點位資料 (由 API 獲取)
const hotSpots = ref([]);

const fetchHotSpots = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/analyze/hot-spots');
    hotSpots.value = response.data.map(spot => ({
      id: spot.spotId,
      name: spot.spotName,
      status: spot.spotStatus,
      seats: spot.availableSeats,
      lat: spot.latitude,
      lng: spot.longitude,
      // 圖片處理邏輯
      image: spot.spotImage 
        ? (spot.spotImage.startsWith('http') ? spot.spotImage : `http://localhost:8080/${spot.spotImage}`) 
        : `https://images.unsplash.com/photo-1517502884422-41eaead166d4?q=80&w=600&auto=format&fit=crop`
    }));
  } catch (error) {
    console.error('無法取得熱門點位資料:', error);
    // 回退到模擬資料以確保介面不空白
    hotSpots.value = [
      { id: 1, name: '信義誠品閱讀區', status: '營運中', seats: 12, lat: 25.0392, lng: 121.5654, image: 'https://images.unsplash.com/photo-1507842217121-9e962835d771?q=80&w=600&auto=format&fit=crop' },
      { id: 2, name: '大安森林公園共享亭', status: '營運中', seats: 5, lat: 25.0297, lng: 121.5364, image: 'https://images.unsplash.com/photo-1493934558415-9d19f0b2b4d2?q=80&w=600&auto=format&fit=crop' }
    ];
  }
};

const fetchAllSpots = async () => {
  try {
    const response = await axios.get('http://localhost:8080/spot/list-with-seats');
    allSpots.value = response.data.map(spot => ({
      id: spot.spotId,
      name: spot.spotName,
      status: spot.spotStatus,
      lat: parseFloat(spot.latitude),
      lng: parseFloat(spot.longitude),
      seats: spot.availableSeats || 0
    }));
  } catch (error) {
    console.error('無法取得所有據點資料:', error);
  }
};

onMounted(() => {
  fetchHotSpots();
  fetchAllSpots();
});

// 模糊查詢過濾建議
const filteredSuggestions = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase();
  if (!kw) return [];
  return allSpots.value
    .filter(s => s.name.toLowerCase().includes(kw))
    .slice(0, 5);
});

const handleSearch = () => {
  if (!searchKeyword.value.trim()) return;
  const exactMatch = allSpots.value.find(s => s.name === searchKeyword.value.trim());
  if (exactMatch) {
    handleReserveClick(exactMatch);
  } else {
    // 若非精確匹配，可執行一般搜尋
    console.log('Search:', searchKeyword.value);
  }
};

const handleSelectSuggestion = (spot) => {
  searchKeyword.value = spot.name;
  isSuggestionsVisible.value = false;
  handleReserveClick(spot);
};

const hideSuggestions = () => {
  setTimeout(() => {
    isSuggestionsVisible.value = false;
  }, 200);
};

// 處理 Google Autocomplete 變化
const onPlaceChanged = (place) => {
  if (place && place.geometry && place.geometry.location) {
    const location = place.geometry.location;
    handleSearchLocation(location.lat(), location.lng());
  }
};

const handleSearchLocation = (lat, lng) => {
  isSearchTriggered.value = true;
  selectedSpotForMap.value = null;
  isMapVisible.value = true;
  mapCenter.value = { lat, lng };
  mapZoom.value = 13;
  setTimeout(() => { mapZoom.value = 15; }, 300);
};

/**
 * -------------------------------------------
 * 3. 地圖地圖與定位邏輯 (Map Modal & Zoom)
 * -------------------------------------------
 */

const handleReserveClick = (spot) => {
  isSearchTriggered.value = false;
  selectedSpotForMap.value = spot;
  isMapVisible.value = true;
  
  // 1. 設定該點位中心
  mapCenter.value = { lat: spot.lat, lng: spot.lng };
  mapZoom.value = 13;

  // 2. [WOW 效果] 平滑縮放至該點位
  setTimeout(() => {
    mapZoom.value = 17;
  }, 300);
};

const handleMarkerClick = (spot) => {
  selectedSpotForMap.value = spot;
};

// 前往租借操作頁面 (Rec 模組)
const confirmAndRent = () => {
  if (!selectedSpotForMap.value) return;

  // 檢查是否登入
  if (!memberAuthStore.isLogin) {
    Swal.fire({
      title: '請先登入',
      text: '預約座位需要先登入會員唷！',
      icon: 'info',
      showCancelButton: true,
      confirmButtonText: '前往登入',
      cancelButtonText: '先看看',
      confirmButtonColor: '#4CAF50'
    }).then((result) => {
      if (result.isConfirmed) {
        isMapVisible.value = false;
        router.push({ name: 'login', query: { redirect: `/rent/order?spotId=${selectedSpotForMap.value.id}` } });
      }
    });
    return;
  }

  isMapVisible.value = false;
  router.push({ 
    name: 'rec-rent-user', 
    params: { action: 'order' }, 
    query: { spotId: selectedSpotForMap.value.id } 
  });
};
</script>

<template>
  <div class="home-view-wrapper">
    <!-- 第一屏：Hero Section -->
    <div class="hero-container">
      <div class="hero-bg"></div>
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <h1 class="hero-title animate-up">Take@Seat</h1>
        <p class="hero-subtitle animate-up delay-1">隨時隨地，輕鬆入座</p>
        
        <div class="cta-buttons animate-up delay-2">
          <router-link to="/SearchSpot" class="cta-btn primary">
            開始尋找租借點 <i class="fas fa-arrow-right arrow-icon"></i>
          </router-link>
        </div>

        <div class="quick-search-card animate-up delay-3">
          <div class="search-input-wrapper-container">
            <div class="search-input-wrapper">
              <i class="fas fa-search search-icon"></i>
              <GMapAutocomplete
                @place_changed="onPlaceChanged"
                :options="{
                  fields: ['geometry', 'formatted_address', 'name'],
                  componentRestrictions: { country: 'tw' },
                }"
                style="flex: 1"
              >
                <input 
                  v-model="searchKeyword" 
                  type="text" 
                  placeholder="輸入據點名稱或地點..." 
                  @keyup.enter="handleSearch"
                  @focus="isSuggestionsVisible = true"
                  @blur="hideSuggestions"
                />
              </GMapAutocomplete>
              <button class="search-btn" @click="handleSearch">搜尋</button>
            </div>
            
            <!-- 自定義建議列表 -->
            <ul v-if="isSuggestionsVisible && filteredSuggestions.length > 0" class="search-suggestions">
              <li 
                v-for="spot in filteredSuggestions" 
                :key="spot.id"
                @mousedown="handleSelectSuggestion(spot)"
              >
                <i class="fas fa-chair"></i>
                <span class="spot-name">{{ spot.name }}</span>
                <span class="spot-status">({{ spot.status }})</span>
              </li>
            </ul>
          </div>
          <div class="hot-tags">
            <span>熱門：</span>
            <span v-for="tag in hotTags" :key="tag" class="tag" @click="searchKeyword = tag">{{ tag }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 第二部分：內容區塊 -->
    <div class="content-section">
      <!-- 簡單三步驟 -->
      <section class="how-it-works">
        <div class="section-header">
          <h2>簡單三步驟</h2>
          <p>輕鬆開啟您的共享空間體驗</p>
        </div>
        <div class="steps-grid">
          <div v-for="(step, index) in steps" :key="index" class="step-item">
            <div class="step-icon">
              <i :class="step.icon"></i>
            </div>
            <h3>{{ step.title }}</h3>
            <p>{{ step.desc }}</p>
          </div>
        </div>
      </section>

      <!-- 熱門點位 -->
      <section class="spot-section">
        <div class="section-header">
          <h2>熱門租借點</h2>
          <p>探索城市中最受歡迎的角落</p>
        </div>
        <div class="spots-grid">
          <div v-for="spot in hotSpots" :key="spot.id" class="spot-card">
            <div class="card-image" :style="{ backgroundImage: `url(${spot.image})` }">
              <span class="status-badge" :class="{ 'full': spot.seats === 0 }">{{ spot.status }}</span>
            </div>
            <div class="card-body">
              <h3>{{ spot.name }}</h3>
              <div class="card-meta">
                <span><i class="fas fa-chair"></i> 剩餘 {{ spot.seats }} 位</span>
                <!-- [修改] 觸發地圖預覽定位邏輯 -->
                <a href="javascript:void(0)" @click="handleReserveClick(spot)" class="card-link">
                  預約 <i class="fas fa-chevron-right"></i>
                </a>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <el-dialog
      v-model="isMapVisible"
      title="確認租借位址"
      width="850px"
      destroy-on-close
      class="map-preview-dialog"
      border-radius="16px"
    >
      <div class="map-preview-body">
        <div class="spot-selector-header">
          <div v-if="selectedSpotForMap" class="spot-info-mini animate-fade">
            <div class="spot-title-row">
              <span class="spot-icon-bg"><i class="fas fa-chair"></i></span>
              <div class="spot-text-content">
                <h4>{{ selectedSpotForMap.name }}</h4>
                <p><i class="fas fa-map-marker-alt"></i> 即將為您跳轉至此點位的租借頁面</p>
              </div>
              <div class="spot-stats-mini">
                <span class="stat-item"><i class="fas fa-couch"></i> {{ selectedSpotForMap.seats }} 位可用</span>
              </div>
            </div>
          </div>
          <div v-else class="spot-info-mini placeholder animate-fade">
            <div class="spot-title-row">
              <span class="spot-icon-bg gray"><i class="fas fa-search-location"></i></span>
              <div class="spot-text-content">
                <h4>請選擇附近的站點</h4>
                <p><i class="fas fa-info-circle"></i> 點擊下方地圖上的標記以選擇租借點</p>
              </div>
            </div>
          </div>
        </div>

        <div class="map-container-wrapper">
          <GMapMap
            :center="mapCenter"
            :zoom="mapZoom"
            :options="{ 
              disableDefaultUI: false, 
              zoomControl: true, 
              mapTypeControl: false,
              streetViewControl: false,
              fullscreenControl: true,
              styles: [
                { featureType: 'poi', elementType: 'labels', stylers: [{ visibility: 'off' }] }
              ]
            }"
            style="width: 100%; height: 450px; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.1);"
          >
            <GMapMarker
              v-for="spot in allSpots"
              :key="spot.id"
              :position="{ lat: spot.lat, lng: spot.lng }"
              :clickable="true"
              @click="handleMarkerClick(spot)"
              :icon="selectedSpotForMap && selectedSpotForMap.id === spot.id 
                ? 'http://maps.google.com/mapfiles/ms/icons/green-dot.png' 
                : 'http://maps.google.com/mapfiles/ms/icons/red-dot.png'"
            >
              <GMapInfoWindow :opened="selectedSpotForMap && selectedSpotForMap.id === spot.id">
                <div class="map-info-window">
                  <h6>{{ spot.name }}</h6>
                  <p>剩餘 {{ spot.seats }} 個座位</p>
                  <span class="badge" :class="spot.status === '營運中' ? 'badge-success' : 'badge-danger'">
                    {{ spot.status }}
                  </span>
                </div>
              </GMapInfoWindow>
            </GMapMarker>
          </GMapMap>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <div class="login-hint" v-if="!memberAuthStore.isLogin && selectedSpotForMap">
            <i class="fas fa-lightbulb"></i> 登入後可享受更快速的租借體驗
          </div>
          <el-button @click="isMapVisible = false" round>返回瀏覽</el-button>
          <el-button 
            type="success" 
            @click="confirmAndRent" 
            class="confirm-btn" 
            :disabled="!selectedSpotForMap"
            round 
            block
          >
            {{ selectedSpotForMap ? '確認並開始租借' : '請選擇站點' }} <i class="fas fa-chevron-right ml-2"></i>
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.home-view-wrapper { width: 100%; display: flex; flex-direction: column; }
.hero-container { position: relative; height: 100vh; width: 100%; overflow: hidden; display: flex; align-items: center; justify-content: center; background-color: #000; }
.hero-bg { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background-image: url('https://images.unsplash.com/photo-1497366216548-37526070297c?q=80&w=1920&auto=format&fit=crop'); background-size: cover; background-position: center; z-index: 0; animation: kenBurns 20s ease-in-out infinite alternate; }
.hero-overlay { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: linear-gradient(to bottom, rgba(0, 0, 0, 0.3) 0%, rgba(0, 0, 0, 0.6) 100%); z-index: 1; }
.hero-content { position: relative; z-index: 2; color: #ffffff; text-align: center; padding: 20px; }
.hero-title { font-size: 5rem; font-weight: 700; margin-bottom: 1rem; text-shadow: 2px 2px 15px rgba(0, 0, 0, 0.5); letter-spacing: 2px; }
.hero-subtitle { font-size: 1.5rem; font-weight: 300; margin-bottom: 2rem; text-shadow: 1px 1px 10px rgba(0, 0, 0, 0.5); letter-spacing: 1px; opacity: 0.9; }
.cta-buttons { display: flex; flex-wrap: wrap; gap: 1rem; justify-content: center; }
.cta-btn { display: inline-flex; align-items: center; justify-content: center; padding: 12px 24px; border-radius: 8px; text-decoration: none; font-size: 1.1rem; font-weight: 500; transition: transform 0.2s ease, box-shadow 0.2s ease; border: 1px solid rgba(255, 255, 255, 0.5); min-width: 160px; }
.cta-btn:hover { transform: translateY(-3px); box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2); }
.cta-btn.primary { background-color: #4CAF50; color: white; font-size: 1.25rem; padding: 16px 40px; border: none; font-weight: 600; box-shadow: 0 4px 15px rgba(76, 175, 80, 0.4); }
.cta-btn.primary:hover { background-color: #45a049; transform: translateY(-5px); box-shadow: 0 8px 25px rgba(76, 175, 80, 0.6); }
.quick-search-card { margin-top: 40px; background: rgba(255, 255, 255, 0.2); backdrop-filter: blur(10px); padding: 20px 30px; border-radius: 16px; border: 1px solid rgba(255, 255, 255, 0.3); width: 100%; max-width: 600px; margin-left: auto; margin-right: auto; box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2); }

.search-input-wrapper-container { position: relative; width: 100%; }
.search-input-wrapper { display: flex; align-items: center; background: white; border-radius: 50px; padding: 5px 5px 5px 20px; margin-bottom: 12px; }
.search-icon { color: #909399; margin-right: 10px; }
.search-input-wrapper input { border: none; outline: none; flex: 1; font-size: 1rem; color: #333; }
.search-btn { background: #4CAF50; color: white; border: none; padding: 10px 24px; border-radius: 50px; cursor: pointer; font-weight: 600; transition: background 0.2s; }
.search-btn:hover { background: #45a049; }

/* 搜尋建議列表 */
.search-suggestions { position: absolute; top: 105%; left: 0; right: 0; background: white; border-radius: 12px; box-shadow: 0 10px 25px rgba(0,0,0,0.1); list-style: none; padding: 8px 0; margin: 0; z-index: 1000; border: 1px solid #eee; text-align: left; }
.search-suggestions li { padding: 12px 20px; cursor: pointer; display: flex; align-items: center; gap: 12px; transition: background 0.2s; color: #333; }
.search-suggestions li:hover { background: #f5f7fa; }
.search-suggestions li i { color: #4CAF50; font-size: 0.9rem; }
.search-suggestions .spot-name { font-weight: 600; flex: 1; }
.search-suggestions .spot-status { font-size: 0.8rem; color: #909399; }

.hot-tags { display: flex; gap: 10px; font-size: 0.9rem; color: rgba(255, 255, 255, 0.9); justify-content: center; flex-wrap: wrap; }
.tag { cursor: pointer; text-decoration: underline; transition: color 0.2s; }
.tag:hover { color: #fff; text-shadow: 0 0 5px rgba(255, 255, 255, 0.5); }

.content-section { background-color: #f9f9f9; padding: 60px 20px; display: flex; flex-direction: column; gap: 80px; }
.section-header { text-align: center; margin-bottom: 40px; }
.section-header h2 { font-size: 2.5rem; color: #333; margin-bottom: 10px; font-weight: 700; }
.section-header p { color: #666; font-size: 1.1rem; }

.steps-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 30px; max-width: 1200px; margin: 0 auto; text-align: center; }
.step-item { padding: 20px; }
.step-icon { width: 80px; height: 80px; background: #e8f5e9; color: #4CAF50; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 32px; margin: 0 auto 20px; transition: transform 0.3s; }
.step-item:hover .step-icon { transform: scale(1.1) rotate(5deg); }
.step-item h3 { font-size: 1.5rem; margin-bottom: 10px; color: #333; }
.step-item p { color: #666; line-height: 1.6; }

.spots-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 30px; max-width: 1200px; margin: 0 auto; width: 100%; }
.spot-card { background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05); transition: transform 0.3s, box-shadow 0.3s; }
.spot-card:hover { transform: translateY(-5px); box-shadow: 0 12px 30px rgba(0, 0, 0, 0.1); }
.card-image { height: 200px; background-size: cover; background-position: center; position: relative; }
.status-badge { position: absolute; top: 10px; right: 10px; background: #4CAF50; color: white; padding: 4px 10px; border-radius: 4px; font-size: 0.8rem; font-weight: 600; }
.status-badge.full { background: #f56c6c; }
.card-body { padding: 20px; }
.card-body h3 { margin: 0 0 10px; font-size: 1.2rem; color: #333; }
.card-meta { display: flex; justify-content: space-between; align-items: center; color: #666; font-size: 0.95rem; }
.card-link { color: #4CAF50; text-decoration: none; font-weight: 600; display: flex; align-items: center; gap: 5px; transition: gap 0.2s; }
.card-link:hover { gap: 8px; }

/* 地圖預覽彈窗 */
.map-preview-dialog :deep(.el-dialog__header) { border-bottom: 1px solid #f0f2f5; margin-right: 0; padding-bottom: 20px; }
.map-preview-dialog :deep(.el-dialog__body) { padding: 0; }
.spot-selector-header { padding: 20px 25px; background: #fafafa; }
.spot-info-mini { background: white; padding: 15px 20px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
.spot-info-mini.placeholder { background: transparent; box-shadow: none; border: 2px dashed #e4e7ed; }
.spot-title-row { display: flex; align-items: center; gap: 15px; }
.spot-icon-bg { width: 45px; height: 45px; background: #e8f5e9; color: #4CAF50; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
.spot-icon-bg.gray { background: #f4f4f5; color: #909399; }
.spot-text-content { flex: 1; }
.spot-info-mini h4 { margin: 0 0 4px; font-size: 1.25rem; color: #2c3e50; font-weight: 700; }
.spot-info-mini p { margin: 0; color: #7f8c8d; font-size: 0.9rem; }
.spot-stats-mini { display: flex; gap: 10px; }
.stat-item { background: #f0f9eb; color: #67c23a; padding: 4px 12px; border-radius: 20px; font-size: 0.85rem; font-weight: 600; }

.map-container-wrapper { padding: 15px 25px 25px; }
.map-info-window { padding: 5px; min-width: 120px; }
.map-info-window h6 { margin: 0 0 5px; font-weight: 700; color: #333; }
.map-info-window p { margin: 0 0 8px; font-size: 0.85rem; color: #666; }
.badge { font-size: 0.75rem; padding: 2px 8px; border-radius: 4px; }
.badge-success { background: #67c23a; color: white; }
.badge-danger { background: #f56c6c; color: white; }

.dialog-footer { display: flex; align-items: center; justify-content: flex-end; gap: 15px; padding: 0 10px; }
.login-hint { font-size: 0.85rem; color: #e6a23c; margin-right: auto; display: flex; align-items: center; gap: 6px; }
.confirm-btn { padding: 12px 30px; font-size: 1.1rem; font-weight: 600; min-width: 200px; box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3); }
.confirm-btn:not(:disabled):hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(103, 194, 58, 0.4); }

.animate-fade { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(-5px); } to { opacity: 1; transform: translateY(0); } }

@keyframes kenBurns { 0% { transform: scale(1); } 100% { transform: scale(1.15); } }
.animate-up { opacity: 0; transform: translateY(30px); animation: fadeInUp 1s cubic-bezier(0.2, 0.8, 0.2, 1) forwards; }
.delay-1 { animation-delay: 0.2s; }
.delay-2 { animation-delay: 0.4s; }
.delay-3 { animation-delay: 0.6s; }
@keyframes fadeInUp { to { opacity: 1; transform: translateY(0); } }
</style>
