# Coze Web Chat SDK 完整測試指南

## 📦 系統架構總覽

```
┌─────────────────────────────────────────────────────────────────┐
│                     用戶瀏覽器 (http://localhost:5173)             │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  SupportCenterView.vue                                      │ │
│  │  ┌───────────────┐        ┌──────────────────────────────┐ │ │
│  │  │ FAQ 系統      │        │ Coze Web Chat SDK            │ │ │
│  │  │ - 搜尋        │        │ 1. initCozeChat()            │ │ │
│  │  │ - 分類篩選    │        │ 2. getCozeBootstrap()        │ │ │
│  │  │ - 手風琴展開  │        │ 3. loadCozeSDK()             │ │ │
│  │  └───────────────┘        │ 4. new WebChatClient()       │ │ │
│  │                            └──────────────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                    │                              │
│                          supportApi.getCozeBootstrap()           │
│                                    ▼                              │
└─────────────────────────────────────────────────────────────────┘
                                     │
                    GET /api/support/coze/bootstrap
                                     │
                    (透過 Vite Proxy 轉發)
                                     ▼
┌─────────────────────────────────────────────────────────────────┐
│          後端 Spring Boot (http://localhost:8080)                │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  SupportCozeController.java                                 │ │
│  │  GET /api/support/coze/bootstrap                            │ │
│  │  ┌────────────────────────────────────────────────────────┐ │ │
│  │  │ 1. 驗證環境變數（botId, pat）                            │ │ │
│  │  │ 2. 回傳 CozeBootstrapResponseDto                        │ │ │
│  │  │    - botId                                              │ │ │
│  │  │    - token (PAT)                                        │ │ │
│  │  │    - sdkSrc                                             │ │ │
│  │  │    - expiresIn                                          │ │ │
│  │  │    - serverTime                                         │ │ │
│  │  │    - note                                               │ │ │
│  │  └────────────────────────────────────────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                    │                              │
│                    讀取 application.yml 配置                      │
│                                    ▼                              │
│                   ${COZE_BOT_ID}, ${COZE_PAT}                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔧 完整配置檢查清單

### ✅ Backend（後端）配置

#### 1. 環境變數設定（必須）
```powershell
# Windows PowerShell
$env:COZE_BOT_ID="你的Bot ID"
$env:COZE_PAT="你的Personal Access Token"
$env:COZE_CHAT_SDK_SRC="https://lf-cdn.coze.cn/obj/unpkg/flow-platform/chat-app-sdk/1.0.0-beta.4/libs/oversea/index.js"
```

驗證：
```powershell
echo $env:COZE_BOT_ID
echo $env:COZE_PAT
echo $env:COZE_CHAT_SDK_SRC
```

#### 2. application.yml 配置
檔案位置：`backend/src/main/resources/application.yml`

```yaml
coze:
  bot-id: ${COZE_BOT_ID:}
  pat: ${COZE_PAT:}
  chat-sdk-src: ${COZE_CHAT_SDK_SRC:}
```

#### 3. 後端檔案清單
```
backend/src/main/java/com/example/backend/
├── controller/
│   └── support/
│       └── SupportCozeController.java        ✅ GET /api/support/coze/bootstrap
├── dto/
│   └── support/
│       └── CozeBootstrapResponseDto.java    ✅ Response DTO
└── exception/
    └── GlobalExceptionHandler.java          ✅ 404 處理
```

#### 4. 測試後端 API
```bash
# 啟動後端
cd backend
mvn spring-boot:run

# 在新終端機測試
curl http://localhost:8080/api/support/coze/bootstrap
```

✅ **預期回應**（200 OK）：
```json
{
  "botId": "7469370888888888888",
  "token": "pat_xxxxxxxxxxxx",
  "sdkSrc": "https://lf-cdn.coze.cn/obj/unpkg/flow-platform/chat-app-sdk/1.0.0-beta.4/libs/oversea/index.js",
  "expiresIn": 0,
  "serverTime": "2026-01-31T12:00:00",
  "note": "⚠️ PAT Token 提醒：此 Token 有過期時間，過期後需要重新建立"
}
```

---

### ✅ Frontend（前端）配置

#### 1. 前端檔案清單
```
frontend/
├── .env                                       ✅ VITE_COZE_CHAT_SDK_SRC fallback
├── vite.config.js                            ✅ Proxy 配置
├── src/
│   ├── api/
│   │   ├── http.js                           ✅ baseURL: '/api'
│   │   └── modules/
│   │       └── support.js                    ✅ getCozeBootstrap()
│   ├── data/
│   │   └── supportFaq.js                     ✅ FAQ 資料（14 項）
│   ├── views/
│   │   └── support/
│   │       ├── SupportCenterView.vue         ✅ FAQ + Coze 初始化
│   │       └── SupportReportView.vue         ✅ 問題回報表單
│   ├── layouts/
│   │   └── MainLayout.vue                    ✅ 客服入口連結
│   └── router/
│       └── index.js                          ✅ /support, /support/report 路由
```

#### 2. http.js 配置（重要）
檔案：`frontend/src/api/http.js`

```javascript
const http = axios.create({
  baseURL: '/api', // ✅ 必須是 '/api'
  timeout: 10000,
})
```

#### 3. support.js 路徑（重要）
檔案：`frontend/src/api/modules/support.js`

```javascript
export const getCozeBootstrap = () => {
  return http.get('/support/coze/bootstrap') // ✅ 不加 /api 前綴
}
```

**路徑拼接邏輯**：
```
baseURL: '/api' + '/support/coze/bootstrap'
= '/api/support/coze/bootstrap'
→ Vite Proxy 轉發
→ http://localhost:8080/api/support/coze/bootstrap
```

#### 4. vite.config.js Proxy 配置
檔案：`frontend/vite.config.js`

```javascript
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // ✅ 不需要 rewrite，直接轉發
      },
    },
  },
})
```

#### 5. .env 環境變數（Fallback）
檔案：`frontend/.env`

```env
VITE_COZE_CHAT_SDK_SRC=https://lf-cdn.coze.cn/obj/unpkg/flow-platform/chat-app-sdk/1.0.0-beta.4/libs/oversea/index.js
```

---

## 🧪 完整測試流程

### Step 1：啟動後端
```bash
cd backend

# 設定環境變數（Windows PowerShell）
$env:COZE_BOT_ID="你的Bot ID"
$env:COZE_PAT="你的Personal Access Token"
$env:COZE_CHAT_SDK_SRC="https://lf-cdn.coze.cn/obj/unpkg/flow-platform/chat-app-sdk/1.0.0-beta.4/libs/oversea/index.js"

# 啟動後端
mvn spring-boot:run
```

**✅ 預期輸出**：
```
2026-01-31 12:00:00.000  INFO 12345 --- [  restartedMain] c.e.b.BackendApplication: Started BackendApplication in 5.123 seconds
```

**❌ 常見錯誤**：
```
❌ Coze Bot ID 未設定！請在 application.yml 設定 coze.bot-id 或環境變數 COZE_BOT_ID
```
→ 解決：確認環境變數已設定

---

### Step 2：測試後端 API
```bash
# 在新終端機執行
curl http://localhost:8080/api/support/coze/bootstrap
```

**✅ 預期回應**（200 OK）：
```json
{
  "botId": "7469370888888888888",
  "token": "pat_xxxxxxxxxxxx",
  "sdkSrc": "https://...",
  "expiresIn": 0,
  "serverTime": "2026-01-31T12:00:00",
  "note": "⚠️ PAT Token 提醒：此 Token 有過期時間，過期後需要重新建立"
}
```

**❌ 常見錯誤**：
- **404 Not Found** → Controller 路徑錯誤或未啟動
- **500 Internal Server Error** → 環境變數未設定
- **Connection Refused** → 後端未啟動

---

### Step 3：啟動前端
```bash
cd frontend
npm run dev
```

**✅ 預期輸出**：
```
VITE v5.x.x  ready in 500 ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
  ➜  press h + enter to show help
```

---

### Step 4：瀏覽器測試

#### 4.1 開啟瀏覽器
前往：`http://localhost:5173/support`

#### 4.2 開啟 Chrome DevTools（F12）

**Console Tab 檢查**：

✅ **成功初始化**：
```
[Coze] 開始載入 Bootstrap 配置...
[Coze] Bootstrap 配置載入成功 { botId: "7469370888888888888", sdkSrc: "https://...", expiresIn: 0 }
[Coze] 使用 SDK 來源: https://lf-cdn.coze.cn/obj/unpkg/flow-platform/chat-app-sdk/1.0.0-beta.4/libs/oversea/index.js
[Coze] SDK 載入成功
[Coze] 使用者 ID: member_123 或 [匿名訪客]
[Coze] 初始化完成 ✅
```

❌ **常見錯誤**：

| 錯誤訊息 | 原因 | 解決方式 |
|---------|------|---------|
| `GET http://localhost:5173/api/api/support/coze/bootstrap 404` | 路徑重複（api/api） | 確認 `support.js` 路徑為 `/support/...` |
| `[Coze] Bootstrap 配置不完整（缺少 botId 或 token）` | 後端環境變數未設定 | 檢查 `$env:COZE_BOT_ID` 和 `$env:COZE_PAT` |
| `Coze SDK 載入失敗` | SDK 來源 URL 錯誤 | 檢查 `COZE_CHAT_SDK_SRC` |
| `Access to XMLHttpRequest blocked by CORS` | CORS 未配置 | 檢查 `SecurityConfig.java` |

**Network Tab 檢查**：

✅ **成功請求**：
- **Request URL**: `http://localhost:5173/api/support/coze/bootstrap`
- **Request Method**: `GET`
- **Status Code**: `200 OK`
- **Response**: 包含 `botId`, `token`, `sdkSrc`

❌ **失敗請求**：
- **404 Not Found** → 後端 Controller 路徑錯誤
- **500 Internal Server Error** → 後端環境變數未設定
- **CORS Error** → CORS 未正確配置

---

### Step 5：視覺驗證

#### 5.1 FAQ 系統
- [ ] 搜尋框可輸入關鍵字
- [ ] 分類切換（會員相關、場地相關、訂位相關、其他問題）
- [ ] 手風琴展開/收合
- [ ] 關鍵字高亮顯示
- [ ] 顯示總 FAQ 數量（14 個問題）

#### 5.2 Coze 聊天泡泡
- [ ] 頁面右下角出現圓形泡泡圖示
- [ ] 點擊泡泡開啟聊天視窗
- [ ] 聊天視窗標題顯示「Take@Seat 智能客服」
- [ ] 可輸入訊息並收到回應
- [ ] 關閉後泡泡仍存在

#### 5.3 MainLayout 入口
- [ ] 側邊欄有「客服支援」選項（電話圖示）
- [ ] 點擊後正確導航至 `/support`

---

## 🐛 疑難排解

### 問題 1：路徑變成 `/api/api/support/...`

**原因**：`support.js` 的路徑寫成 `/api/support/...`

**解決方式**：
```javascript
// ❌ 錯誤
return http.get('/api/support/coze/bootstrap')

// ✅ 正確
return http.get('/support/coze/bootstrap')
```

---

### 問題 2：泡泡沒有出現

**診斷步驟**：
1. 檢查 Console 是否顯示 `[Coze] 初始化完成 ✅`
2. 在 Console 執行：`window.__cozeClient`（應該有值）
3. 在 Console 執行：`window.CozeWebSDK`（應該有值）
4. 嘗試手動開啟：`window.__cozeClient.open()`

**可能原因**：
- SDK 版本不相容
- SDK 來源 URL 錯誤
- Bot ID 或 Token 無效

---

### 問題 3：Token 過期

**症狀**：
- Console 顯示 `[Coze] Token 即將過期，自動刷新...`
- 聊天功能無法使用

**解決方式**：
1. 登入 Coze 平台
2. 重新建立 Personal Access Token
3. 更新環境變數：`$env:COZE_PAT="新的Token"`
4. 重新啟動後端

---

### 問題 4：CORS 錯誤

**症狀**：
```
Access to XMLHttpRequest at 'http://localhost:8080/api/support/coze/bootstrap' 
from origin 'http://localhost:5173' has been blocked by CORS policy
```

**解決方式**：
確認 `SecurityConfig.java` 有以下配置：
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.addAllowedOrigin("http://localhost:5173");
    configuration.addAllowedMethod("*");
    configuration.addAllowedHeader("*");
    configuration.setAllowCredentials(true);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

---

## 🚀 快速診斷腳本

執行診斷腳本：
```powershell
.\diagnose-coze.ps1
```

這個腳本會自動檢查：
- ✅ 環境變數設定
- ✅ 後端 API 狀態
- ✅ 前端配置檔案
- ✅ 路由配置
- ✅ MainLayout 入口

---

## 📊 測試報告範例

### ✅ 成功案例

```
========================================
測試報告
========================================
測試時間：2026-01-31 12:00:00
測試人員：開發者

【後端測試】
✅ 環境變數設定正確
✅ Bootstrap API 回傳 200 OK
✅ 回應包含完整欄位（botId, token, sdkSrc）

【前端測試】
✅ FAQ 系統正常顯示
✅ 搜尋功能正常
✅ Bootstrap API 呼叫成功（200 OK）
✅ Coze SDK 載入成功
✅ 泡泡出現在右下角
✅ 可正常傳送訊息並收到回應

【整合測試】
✅ MainLayout 客服入口正常
✅ 路由導航正常
✅ Token 刷新機制正常
✅ 匿名訪客 ID 正常產生
✅ 會員登入後 ID 正確使用 member_{memId}

【結論】
✅ Coze Web Chat SDK 整合成功
```

---

## 📝 維護建議

### 定期檢查
- [ ] 每 90 天更換 Coze PAT Token
- [ ] 每月檢查 Coze SDK 版本更新
- [ ] 每週監控 Bootstrap API 呼叫頻率
- [ ] 每日檢查後端 log 是否有異常

### 監控指標
- Bootstrap API 成功率（目標：>99%）
- 初始化失敗率（目標：<1%）
- Token 刷新頻率
- 使用者滿意度（Chat 使用率）

### 備份計畫
- [ ] 定期備份 FAQ 資料
- [ ] 保留舊版 PAT Token（直到確認新 Token 正常）
- [ ] 記錄 SDK 版本變更歷史

---

**最後更新**：2026-01-31  
**維護者**：Take@Seat 開發團隊
