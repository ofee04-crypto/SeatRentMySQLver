# Coze Web Chat SDK 整合完成報告

## 📋 整合摘要

**完成時間**：2026-01-31  
**整合範圍**：前後端完整整合 Coze Web Chat SDK，提供 FAQ 系統與 AI 客服泡泡  
**功能狀態**：✅ 已完成並測試通過

---

## 🎯 實作功能

### 1. FAQ 系統

- ✅ 14 個常見問題（4 大分類）
  - 會員相關（4 題）
  - 場地相關（4 題）
  - 訂位相關（4 題）
  - 其他問題（2 題）
- ✅ 關鍵字搜尋（標題、內容、標籤、關鍵字）
- ✅ 分類篩選切換
- ✅ 手風琴展開/收合
- ✅ 響應式設計（Bootstrap 5）

### 2. Coze AI 客服泡泡

- ✅ 自動初始化（頁面載入後 1 秒）
- ✅ 動態載入 SDK Script
- ✅ 會員 ID 辨識機制
  - 已登入會員：`member_{memId}`
  - 匿名訪客：UUID（localStorage 持久化）
- ✅ Token 自動刷新機制
- ✅ 防止重複初始化（全域旗標）
- ✅ 錯誤處理（不影響 FAQ 功能）

### 3. 問題回報表單

- ✅ 結構化表單（問題類型、描述）
- ✅ 圖片上傳功能（預覽與刪除）
- ✅ 表單驗證
- ✅ 完善的錯誤處理

### 4. Backend Bootstrap API

- ✅ 提供前端初始化所需配置
- ✅ 環境變數注入（@Value）
- ✅ 配置驗證（缺少時回傳 500）
- ✅ Token 安全性（不在 log 印出）
- ✅ DTO toString() 自動遮蔽 Token

### 5. 路由與入口

- ✅ `/support` - 客服支援中心（FAQ + Coze）
- ✅ `/support/report` - 問題回報
- ✅ MainLayout 側邊欄客服入口（電話圖示）

---

## 📁 檔案清單

### 🆕 新增檔案

#### Backend（後端）

```
backend/src/main/java/com/example/backend/
├── controller/support/
│   └── SupportCozeController.java                  ✅ Bootstrap API 端點
└── dto/support/
    └── CozeBootstrapResponseDto.java              ✅ Response DTO（Token 遮蔽）
```

#### Frontend（前端）

```
frontend/
├── src/
│   ├── api/modules/
│   │   └── support.js                             ✅ Support API 模組
│   ├── data/
│   │   └── supportFaq.js                          ✅ FAQ 資料（14 項）
│   └── views/support/
│       ├── SupportCenterView.vue                  ✅ FAQ 中心 + Coze 初始化
│       └── SupportReportView.vue                  ✅ 問題回報表單
├── docs/
│   ├── COZE_README.md                             📚 整合總覽與快速開始
│   ├── COZE_TESTING_GUIDE.md                      📚 完整測試指南
│   └── COZE_TROUBLESHOOTING.md                    📚 疑難排解指南
└── COZE_README.md                                 📚 主要說明文件（根目錄）
```

#### Scripts（腳本）

```
專案根目錄/
├── diagnose-coze.ps1                              🔧 自動診斷腳本
└── setup-coze.ps1                                 🔧 快速設定腳本
```

---

### 🔧 修改檔案

#### Backend（後端）

```
backend/src/main/
├── java/com/example/backend/exception/
│   └── GlobalExceptionHandler.java                🔧 新增 NoResourceFoundException 處理
└── resources/
    └── application.yml                            🔧 新增 coze 配置區塊
```

#### Frontend（前端）

```
frontend/src/
├── router/
│   └── index.js                                   🔧 新增 /support, /support/report 路由
└── layouts/
    └── MainLayout.vue                             🔧 新增客服入口（已有）
```

---

## 🔍 核心技術細節

### 路徑配置（重要！）

```javascript
// ✅ 正確配置
// http.js
const http = axios.create({
  baseURL: "/api", // ← 必須是 '/api'
});

// support.js
export const getCozeBootstrap = () => {
  return http.get("/support/coze/bootstrap"); // ← 不加 /api 前綴
};

// 最終路徑拼接：
// baseURL('/api') + path('/support/coze/bootstrap')
// = '/api/support/coze/bootstrap'
// → Vite Proxy 轉發
// → http://localhost:8080/api/support/coze/bootstrap
```

### Coze 初始化流程

```javascript
// SupportCenterView.vue
initCozeChat() {
  1. 檢查是否已初始化（window.__coze_inited）
  2. 呼叫 supportApi.getCozeBootstrap()
  3. 驗證回應（botId, token）
  4. 決定 SDK 來源（後端優先，否則環境變數）
  5. 動態載入 SDK Script（防止重複插入）
  6. 建立 WebChatClient 實例
  7. 設定使用者 ID（member_X / UUID）
  8. 設定 Token 刷新機制（onRefreshToken）
  9. 設定全域旗標（window.__coze_inited = true）
}
```

### Token 安全性

```java
// CozeBootstrapResponseDto.java
@Override
public String toString() {
    return "CozeBootstrapResponseDto{" +
            "botId='" + botId + '\'' +
            ", token='" + (token != null && token.length() > 10
                ? token.substring(0, 10) + "..."
                : "[MASKED]") + '\'' +  // ← 自動遮蔽
            ", sdkSrc='" + sdkSrc + '\'' +
            ", expiresIn=" + expiresIn +
            ", serverTime=" + serverTime +
            ", note='" + note + '\'' +
            '}';
}
```

---

## 🚀 快速啟動

### Step 1：設定環境變數

```powershell
# 方式 1：使用快速設定腳本（推薦）
.\setup-coze.ps1

# 方式 2：手動設定
$env:COZE_BOT_ID="你的Bot ID"
$env:COZE_PAT="你的Personal Access Token"
$env:COZE_CHAT_SDK_SRC="https://lf-cdn.coze.cn/obj/unpkg/flow-platform/chat-app-sdk/1.0.0-beta.4/libs/oversea/index.js"
```

### Step 2：啟動服務

```bash
# 終端機 1：後端
cd backend
mvn spring-boot:run

# 終端機 2：前端
cd frontend
npm run dev
```

### Step 3：驗證整合

```bash
# 測試後端 API
curl http://localhost:8080/api/support/coze/bootstrap

# 瀏覽器測試
# 前往 http://localhost:5173/support
# 檢查 Console 是否顯示：[Coze] 初始化完成 ✅
```

### Step 4：自動診斷

```powershell
# 執行診斷腳本
.\diagnose-coze.ps1
```

---

## 📊 測試報告

### ✅ 後端測試

- ✅ Bootstrap API 回傳 200 OK
- ✅ 回應包含完整欄位（botId, token, sdkSrc, expiresIn, serverTime, note）
- ✅ 缺少環境變數時回傳 500 並提示錯誤
- ✅ Token 不在 log 印出完整內容
- ✅ CORS 配置正確

### ✅ 前端測試

- ✅ FAQ 系統正常顯示（14 個問題）
- ✅ 搜尋功能正常（標題、內容、標籤、關鍵字）
- ✅ 分類篩選正常
- ✅ 手風琴展開/收合正常
- ✅ Bootstrap API 呼叫成功（200 OK）
- ✅ Coze SDK 載入成功
- ✅ 泡泡出現在右下角
- ✅ 可正常傳送訊息並收到回應
- ✅ Token 刷新機制正常
- ✅ 會員 ID 辨識正常

### ✅ 整合測試

- ✅ 路徑無 api/api 重複問題
- ✅ Vite Proxy 轉發正常
- ✅ 匿名訪客 UUID 正常產生與持久化
- ✅ 會員登入後 ID 正確使用 member\_{memId}
- ✅ 泡泡初始化不影響 FAQ 功能
- ✅ 錯誤處理完善（500/404 不崩潰）

---

## 🛠️ 工具與腳本

### 1. diagnose-coze.ps1（診斷腳本）

**功能**：

- ✅ 檢查環境變數設定
- ✅ 測試後端 Bootstrap API
- ✅ 檢查前端配置檔案（http.js, support.js, vite.config.js）
- ✅ 檢查路由配置
- ✅ 檢查 MainLayout 入口
- ✅ 提供修正建議

**使用方式**：

```powershell
.\diagnose-coze.ps1
```

### 2. setup-coze.ps1（快速設定腳本）

**功能**：

- ✅ 互動式設定環境變數
- ✅ 驗證後端 API
- ✅ 產生可重複使用的設定檔（coze-env.ps1）
- ✅ 提供下一步操作指引

**使用方式**：

```powershell
# 互動式
.\setup-coze.ps1

# 命令列參數
.\setup-coze.ps1 -BotId "你的Bot ID" -Pat "你的PAT Token"
```

---

## 📚 說明文件

### 1. COZE_README.md

- 📌 整合總覽
- 📌 5 分鐘快速開始
- 📌 專案結構說明
- 📌 環境變數說明
- 📌 功能特色
- 📌 資料流程圖
- 📌 常用指令
- 📌 檢查清單

### 2. COZE_TESTING_GUIDE.md

- 📌 系統架構總覽
- 📌 完整配置檢查清單
- 📌 測試流程（Step by Step）
- 📌 瀏覽器 Console/Network 檢查
- 📌 視覺驗證清單
- 📌 疑難排解
- 📌 測試報告範例
- 📌 維護建議

### 3. COZE_TROUBLESHOOTING.md

- 📌 系統架構檢查清單
- 📌 診斷腳本使用方式
- 📌 常見錯誤與解決方式
- 📌 FAQ（常見問題）
- 📌 快速啟動流程
- 📌 維護建議

---

## 🔒 安全性措施

### Backend（後端）

- ✅ 敏感資訊來自環境變數（不寫死在程式碼）
- ✅ Token 不在 log 印出完整內容
- ✅ DTO 的 `toString()` 自動遮蔽 Token（只顯示前 10 字元）
- ✅ 缺少配置時回傳明確錯誤訊息（不洩漏敏感資訊）
- ✅ GlobalExceptionHandler 統一處理 404（不回傳 500）

### Frontend（前端）

- ✅ Token 不在 Console 印出
- ✅ 錯誤處理不洩漏敏感資訊
- ✅ 匿名訪客使用 UUID（crypto.randomUUID）
- ✅ 會員使用 `member_{memId}`（辨識度高，不洩漏個資）
- ✅ Token 刷新機制（onRefreshToken 自動處理）

---

## 📝 維護建議

### 定期檢查

- [ ] **每 90 天**：更換 Coze PAT Token
- [ ] **每月**：檢查 Coze SDK 版本更新
- [ ] **每週**：監控 Bootstrap API 呼叫頻率
- [ ] **每日**：檢查後端 log 是否有異常

### 監控指標

- Bootstrap API 成功率（目標：>99%）
- 初始化失敗率（目標：<1%）
- Token 刷新頻率
- 使用者滿意度（Chat 使用率）

### 備份計畫

- [ ] 定期備份 FAQ 資料
- [ ] 保留舊版 PAT Token（直到確認新 Token 正常）
- [ ] 記錄 SDK 版本變更歷史
- [ ] 定期檢查 Coze 官方文件更新

---

## 🎓 使用指南

### 對於開發者

1. 閱讀 [COZE_README.md](COZE_README.md) 了解整合架構
2. 執行 `setup-coze.ps1` 快速設定環境
3. 閱讀 [COZE_TESTING_GUIDE.md](COZE_TESTING_GUIDE.md) 學習測試流程
4. 遇到問題時查閱 [COZE_TROUBLESHOOTING.md](COZE_TROUBLESHOOTING.md)

### 對於測試人員

1. 執行 `diagnose-coze.ps1` 檢查配置
2. 依照 [COZE_TESTING_GUIDE.md](COZE_TESTING_GUIDE.md) 執行測試
3. 記錄測試結果（參考文件中的測試報告範例）

### 對於維運人員

1. 定期執行 `diagnose-coze.ps1` 檢查系統狀態
2. 監控 Token 過期時間
3. 定期更新 FAQ 內容
4. 檢查使用者回饋並優化 FAQ

---

## 📞 支援資源

### 官方文件

- [Coze 平台](https://www.coze.com/)
- [Coze Web Chat SDK 文件](https://www.coze.com/docs/developer_guides/web_chat_sdk)

### 專案文件

- [整合總覽](frontend/COZE_README.md)
- [測試指南](frontend/COZE_TESTING_GUIDE.md)
- [疑難排解](frontend/COZE_TROUBLESHOOTING.md)

### 聯絡資訊

- 開發團隊：Take@Seat 開發團隊
- 最後更新：2026-01-31

---

## ✅ 驗收標準

### 功能驗收

- [x] FAQ 系統完整顯示（14 個問題）
- [x] 搜尋功能正常運作
- [x] 分類篩選正常運作
- [x] Coze 泡泡成功出現
- [x] 可正常傳送訊息並收到回應
- [x] 問題回報表單正常運作
- [x] MainLayout 有客服入口

### 技術驗收

- [x] Backend Bootstrap API 正常運作
- [x] 路徑配置正確（無 api/api 重複）
- [x] Token 安全性措施完善
- [x] 錯誤處理完善
- [x] 會員 ID 辨識正常
- [x] Token 刷新機制正常

### 文件驗收

- [x] 完整的設定指南
- [x] 完整的測試指南
- [x] 完整的疑難排解指南
- [x] 自動診斷腳本
- [x] 快速設定腳本

---

## 🎉 總結

✅ **Coze Web Chat SDK 整合完成！**

所有核心功能已實作並測試通過：

- ✅ FAQ 系統（14 個問題，4 大分類）
- ✅ Coze AI 客服泡泡（自動初始化、Token 刷新）
- ✅ 問題回報表單（圖片上傳、表單驗證）
- ✅ Backend Bootstrap API（環境變數、安全性）
- ✅ 完整的測試與診斷工具
- ✅ 完善的說明文件

**下一步**：

1. 執行 `setup-coze.ps1` 設定環境變數
2. 啟動前後端服務
3. 執行 `diagnose-coze.ps1` 驗證配置
4. 前往 `http://localhost:5173/support` 測試功能

**遇到問題？**

- 查閱 [COZE_TROUBLESHOOTING.md](frontend/COZE_TROUBLESHOOTING.md)
- 執行 `.\diagnose-coze.ps1` 自動診斷

---

**維護者**：Take@Seat 開發團隊  
**完成時間**：2026-01-31  
**版本**：1.0.0
