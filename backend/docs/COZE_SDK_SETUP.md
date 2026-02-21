# Coze SDK 設定指南

## 問題背景

`@coze/chat-sdk` npm 套件內含 Taro 跨平台程式碼（`dist/lib-source`），導致 Vite 預打包時嘗試解析 `@tarojs/*` 依賴而失敗：

```
Could not resolve "@tarojs/components"
504 Outdated Optimize Dep
NS_ERROR_CORRUPTED_CONTENT
```

## 解決方案：雙模式載入

本專案實作了 npm + script loader 雙模式載入機制，透過環境變數 `VITE_COZE_SDK_MODE` 控制。

### 模式說明

| 模式 | 說明 | 適用場景 |
|------|------|----------|
| `auto` | 先嘗試 npm import，失敗則 fallback script | **推薦**，兼顧性能與穩定性 |
| `npm` | 強制 npm import | 僅測試用，可能失敗 |
| `script` | 強制 script loader | 最穩定，適合生產環境 |

### 環境變數配置

在 `frontend/.env` 中：

```env
# SDK 載入模式
VITE_COZE_SDK_MODE="auto"

# Script loader 來源（本地或 CDN）
VITE_COZE_CHAT_SDK_SRC="/vendor/coze/index.js"
```

## 設定步驟

### 1. Vite 配置（已完成）

`frontend/vite.config.js` 已設定排除預打包：

```js
optimizeDeps: {
  exclude: ['@coze/chat-sdk']
}
```

### 2. 本地 SDK 託管（可選）

若使用 script 模式，建議本地託管 SDK 以避免 CDN 問題：

```bash
cd backend/src/main/resources/static/vendor/coze

# 下載 SDK
curl -o index.js "https://lf-cdn.coze.com/obj/unpkg/flow-platform/chat-app-sdk/1.0.0-beta.4/index.js"
```

### 3. 驗證

1. 啟動後端：`cd backend && mvn spring-boot:run`
2. 啟動前端：`cd frontend && npm run dev`
3. 訪問客服中心頁面
4. 開啟瀏覽器 Console，查看 `[Coze]` 日誌：
   - `SDK 載入模式: auto`
   - `npm import 成功 ✅` 或 `script loader 成功 ✅`

## 故障排除

### 問題：npm import 持續失敗

**原因**：Vite 快取未清除

**解決**：
```bash
cd frontend
rm -rf node_modules/.vite
npm run dev
```

### 問題：script loader 404

**原因**：本地 SDK 檔案不存在

**解決**：
1. 確認 `backend/src/main/resources/static/vendor/coze/index.js` 存在
2. 或改用 CDN：`VITE_COZE_CHAT_SDK_SRC="https://lf-cdn.coze.com/obj/unpkg/flow-platform/chat-app-sdk/1.0.0-beta.4/index.js"`

### 問題：502 Bad Gateway / TLB

**原因**：Coze API 端點問題（非 SDK 載入問題）

**解決**：
1. 檢查 `coze.api.host` 設定
2. 確認網路能連線到 `api.coze.com`
3. 使用診斷腳本：`powershell -ExecutionPolicy Bypass -File backend/docs/diagnose-coze.ps1`

## 相關檔案

- [useCozeChat.js](../frontend/src/composables/useCozeChat.js) - SDK 載入邏輯
- [vite.config.js](../frontend/vite.config.js) - Vite 配置
- [.env](../frontend/.env) - 環境變數
- [SupportCenterView.vue](../frontend/src/views/support/SupportCenterView.vue) - 前端頁面
